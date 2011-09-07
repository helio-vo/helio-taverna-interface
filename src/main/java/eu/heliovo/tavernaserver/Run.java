package eu.heliovo.tavernaserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.w3c.dom.Element;

import uk.org.taverna.ns._2010.port.InputPort;
import uk.org.taverna.ns._2010.xml.server.DirectoryEntry;
import uk.org.taverna.ns._2010.xml.server.DirectoryReference;
import uk.org.taverna.ns._2010.xml.server.FileReference;
import uk.org.taverna.ns._2010.xml.server.Status;
import uk.org.taverna.ns._2010.xml.server.Workflow;
import uk.org.taverna.ns._2010.xml.server.soap.BadPropertyValueException;
import uk.org.taverna.ns._2010.xml.server.soap.BadStateChangeException;
import uk.org.taverna.ns._2010.xml.server.soap.FilesystemAccessException;
import uk.org.taverna.ns._2010.xml.server.soap.NoCreateException;
import uk.org.taverna.ns._2010.xml.server.soap.NoDirectoryEntryException;
import uk.org.taverna.ns._2010.xml.server.soap.NoUpdateException;
import uk.org.taverna.ns._2010.xml.server.soap.TavernaService;
import uk.org.taverna.ns._2010.xml.server.soap.UnknownRunException;

/**
 * A particular run of a workflow on an instance of Taverna Server.
 * 
 * @author Donal Fellows
 */
public class Run {
	private static DatatypeFactory dtf;
	static {
		try {
			dtf = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	private TavernaService s;
	private String id;
	private int pollInterval = 1000;

	Run(TavernaService service, Element workflow) throws NoCreateException,
			NoUpdateException {
		this.s = service;
		Workflow w = new Workflow();
		w.getAny().add(workflow);
		this.id = s.submitWorkflow(w).getValue();
	}

	Run(TavernaService service, String id) {
		this.s = service;
		this.id = id;
	}

	/**
	 * @return The unique identifier for this workflow run. Only guaranteed to
	 *         be unique within a particular server.
	 *         <p>
	 *         <small><i>(Implementation note: these are UUIDs so they will
	 *         actually be probabilistically unique across all
	 *         servers.)</i></small>
	 */
	public String getId() {
		return id;
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	/**
	 * Get the current status of a workflow run. Workflow runs start in the
	 * initialized state, and need to be manually moved into the operating
	 * state. Once the workflow run finishes, they automatically move into the
	 * finished state; they can also be moved there manually to kill the run's
	 * processing. Access to the run's files and other resources is permitted
	 * until the run is destroyed (an independent state from the finished state;
	 * never reported since it produces an exception instead).
	 * <p>
	 * <small><i>(Implementation note: the stopped state is currently not
	 * supported.)</i></small>
	 * 
	 * @return The current status of the workflow run.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public Status getStatus() throws UnknownRunException {
		return s.getRunStatus(id);
	}

	/**
	 * @return whether this run has not yet started (i.e., is in the initial
	 *         state).
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public boolean isPending() throws UnknownRunException {
		return getStatus() == Status.INITIALIZED;
	}

	/**
	 * @return whether this run has finished (i.e., is in the final state).
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public boolean isFinished() throws UnknownRunException {
		return getStatus() == Status.FINISHED;
	}

	/**
	 * @return whether this run is running (i.e., is in the operating state).
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public boolean isRunning() throws UnknownRunException {
		return getStatus() == Status.OPERATING;
	}

	/**
	 * Set the state of the run. This is how to make the workflow run start
	 * processing or kill it off.
	 * 
	 * @param status
	 *            The state to set it to. Should be either operating or
	 *            finished, though setting a run to its current state is always
	 *            legal.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 * @throws BadStateChangeException
	 *             If the state change is illegal (e.g., going from finished
	 *             back to operating).
	 */
	public void setStatus(Status status) throws NoUpdateException,
			UnknownRunException, BadStateChangeException {
		s.setRunStatus(id, status);
	}

	/**
	 * Start this workflow run operating.
	 * 
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 * @throws BadStateChangeException
	 *             If the workflow run has finished.
	 */
	public void start() throws NoUpdateException, UnknownRunException,
			BadStateChangeException {
		setStatus(Status.OPERATING);
	}

	/**
	 * Stop this workflow run from running.
	 * 
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public void stop() throws NoUpdateException, UnknownRunException {
		try {
			if (!isFinished())
				setStatus(Status.FINISHED);
		} catch (BadStateChangeException e) {
			// do nothing; shouldn't happen
		}
	}

	/**
	 * Run the workflow and wait until it has finished.
	 * 
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 * @throws NoUpdateException
	 *             If the user does not have write access to this workflow run.
	 * @throws BadStateChangeException
	 *             If the state changes go wrong.
	 */
	public void runUntilFinished() throws UnknownRunException,
			NoUpdateException, BadStateChangeException {
		if (isPending())
			start();
		do {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				stop();
				break;
			}
		} while (isRunning());
	}

	/**
	 * Run the workflow and wait until it has finished.
	 * 
	 * @param statusCallback
	 *            A callback that receives regular calls. Makes it easy to
	 *            handle progress indicators
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 * @throws NoUpdateException
	 *             If the user does not have write access to this workflow run.
	 * @throws BadStateChangeException
	 *             If the state changes go wrong.
	 */
	public void runUntilFinished(Callback statusCallback)
			throws UnknownRunException, NoUpdateException,
			BadStateChangeException {
		if (isPending())
			start();
		do {
			try {
				Thread.sleep(pollInterval);
			} catch (InterruptedException e) {
				stop();
				break;
			}
			statusCallback.tick();
			if (Thread.interrupted()) {
				stop();
				break;
			}
		} while (isRunning());
	}

	/**
	 * The progress callback for {@link #runUntilFinished(Callback)
	 * runUntilFinished(...)}.
	 * <p>
	 * <small><i>(Implementation note: the built-in delay is currently one
	 * second, plus whatever is actually inherent in asking the server what its
	 * current state is.)</i></small>
	 * 
	 * @author Donal Fellows
	 */
	public interface Callback {
		/**
		 * Called periodically until the workflow run has finished. Should not
		 * throw <i>any</i> exceptions, even unchecked ones.
		 */
		void tick();
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	/**
	 * Describes the list of inputs to the workflow run.
	 * 
	 * @return The list of <i>expected</i>/supported inputs.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public List<String> getInputNames() throws UnknownRunException {
		ArrayList<String> result = new ArrayList<String>();
		for (InputPort i : s.getRunInputDescriptor(id).getInput()) {
			result.add(i.getName());
		}
		return result;
	}

	private int inputCtr;

	/**
	 * Sets the named input to be the given value.
	 * 
	 * @param name
	 *            The name of the input to set. Setting an unexpected input just
	 *            leads to wasted space.
	 * @param value
	 *            The literal bytes to use for the input value.
	 * @throws UnknownRunException
	 *             If this run has expired or been destroyed.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws BadStateChangeException
	 *             If the input is set when the run isn't initializing.
	 */
	public void setInput(String name, byte[] value) throws NoUpdateException,
			UnknownRunException, BadStateChangeException {
		try {
			String inName = "in" + (++inputCtr);
			FileReference de = mkfile("", inName);
			s.setRunFileContents(id, de, value);
			s.setRunInputPortFile(id, name, inName);
		} catch (BadPropertyValueException e) {
			// ignore; should not happen (by construction)
		} catch (FilesystemAccessException e) {
			// ignore; should not happen (by construction)
		} catch (NoDirectoryEntryException e) {
			// ignore; should not happen (by construction)
		}
	}

	/**
	 * Sets the named input to be the contents of the given file.
	 * 
	 * @param name
	 *            The name of the input to set. Setting an unexpected input just
	 *            leads to wasted space.
	 * @param localFile
	 *            The file on the client system holding the input value.
	 * @throws UnknownRunException
	 *             If this run has expired or been destroyed.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws BadStateChangeException
	 *             If the input is set when the run isn't initializing.
	 * @throws IOException
	 *             If a problem happens when accessing the local file.
	 */
	public void setInput(String name, File localFile) throws IOException,
			NoUpdateException, UnknownRunException, BadStateChangeException {
		InputStream is = new FileInputStream(localFile);
		byte[] buffer = new byte[(int) localFile.length()];
		try {
			is.read(buffer);
		} finally {
			is.close();
		}
		setInput(name, buffer);
	}

	/**
	 * Sets the named input to be the given value.
	 * 
	 * @param name
	 *            The name of the input to set. Setting an unexpected input just
	 *            leads to wasted space.
	 * @param value
	 *            The string to use for the input value.
	 * @param encoding
	 *            The character encoding to use for the string. Use
	 *            <tt>null</tt> to use the <i>client</i> platform's default.
	 * @throws UnknownRunException
	 *             If this run has expired or been destroyed.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws BadStateChangeException
	 *             If the input is set when the run isn't initializing.
	 */
	public void setInput(String name, String value, Charset encoding)
			throws NoUpdateException, UnknownRunException,
			BadStateChangeException {
		if (encoding == null)
			encoding = Charset.defaultCharset();
		setInput(name, value.getBytes(encoding));
	}

	/**
	 * Direct all inputs to come from the given Baclava file on the local
	 * system. This overrides any settings on inputs otherwise.
	 * 
	 * @param baclavaFileToRead
	 *            The <i>local</i> file to be read. Must be in Baclava format.
	 * @throws IOException
	 *             If the baclava file cannot be read.
	 * @throws UnknownRunException
	 *             If this run has expired or been destroyed.
	 * @throws NoUpdateException
	 *             If the user cannot update this run.
	 * @throws BadStateChangeException
	 *             If this run is not in the initial state.
	 */
	public void inputFromBaclava(File baclavaFileToRead)
			throws NoUpdateException, UnknownRunException, IOException,
			BadStateChangeException {
		byte[] b;
		InputStream is = new FileInputStream(baclavaFileToRead);
		try {
			b = new byte[(int) baclavaFileToRead.length()];
			is.read(b);
			String remoteBaclavaFile = "in" + (++inputCtr) + ".baclava";
			FileReference de = mkfile("", remoteBaclavaFile);
			s.setRunFileContents(id, de, b);
			s.setRunInputBaclavaFile(id, remoteBaclavaFile);
		} catch (FilesystemAccessException e) {
			// ignore; should not happen (by construction)
		} catch (NoDirectoryEntryException e) {
			// ignore; should not happen (by construction)
		} finally {
			is.close();
		}
	}

	/**
	 * Ask for the run to produce its output as baclava instead of a directory
	 * of files.
	 * 
	 * @see #retrieveOutputBaclava(File)
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 * @throws NoUpdateException
	 *             If the user is not allowed to write to this run.
	 * @throws BadStateChangeException
	 *             If the run is not in the initial state.
	 */
	public void outputToBaclava() throws NoUpdateException,
			UnknownRunException, BadStateChangeException {
		try {
			s.setRunOutputBaclavaFile(id, "out.baclava");
		} catch (FilesystemAccessException ex) {
			// ignore; should not happen (by construction)
		}
	}

	/**
	 * Retrieve the output of a workflow run as a baclava file. Should only be
	 * called in the final state, and only if the workflow run has been started.
	 * The output must have been requested as baclava during initialisation.
	 * 
	 * @see #outputToBaclava()
	 * @param baclavaFileToWrite
	 *            The file to have the baclava-format data written to it.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 * @throws NoDirectoryEntryException
	 *             If the output was not requested as baclava or if it hasn't
	 *             been written yet (because the run is still running).
	 * @throws IOException
	 *             If the file is unwritable.
	 */
	public void retrieveOutputBaclava(File baclavaFileToWrite)
			throws NoDirectoryEntryException, UnknownRunException, IOException {
		try {
			byte[] contents = s.getRunFileContents(id, mkfref("out.baclava"));
			OutputStream os = new FileOutputStream(baclavaFileToWrite);
			try {
				os.write(contents);
			} finally {
				os.close();
			}
		} catch (FilesystemAccessException e) {
			// ignore; should not happen (by construction)
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	/**
	 * @return the listeners attached to this run. There is always at least one,
	 *         called <tt>io</tt>, which provides access to miscellaneous
	 *         properties (such as the run's standard output).
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public Map<String, Listener> getListeners() throws UnknownRunException {
		HashMap<String, Listener> result = new HashMap<String, Listener>();
		for (String name : s.getRunListeners(id))
			result.put(name, new Listener(s, id, name));
		return result;
	}

	/**
	 * @param name
	 *            the name of the listener to look up.
	 * @return the named listener, or <tt>null</tt> if no such listener exists.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public Listener getListener(String name) throws UnknownRunException {
		return getListeners().get(name);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	private DirectoryReference mkdref(String dirName) {
		DirectoryReference dir = new DirectoryReference();
		dir.setValue(dirName);
		return dir;
	}

	private FileReference mkfref(String name) {
		FileReference file = new FileReference();
		file.setValue(name);
		return file;
	}

	private FileReference mkfile(String dir, String file)
			throws NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException, UnknownRunException {
		return (FileReference) s.makeRunFile(id, mkdref(dir), file);
	}

	/**
	 * Get a recursive listing of a directory. The directory separator is "
	 * <tt>/</tt>".
	 * 
	 * @param dirName
	 *            the name of the directory to list.
	 * @return the files and subdirectories in a directory, in arbitrary order.
	 * @throws FilesystemAccessException
	 *             If the directory name is malformed.
	 * @throws NoDirectoryEntryException
	 *             If the directory doesn't exist.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public Collection<String> listDirectory(String dirName)
			throws FilesystemAccessException, NoDirectoryEntryException,
			UnknownRunException {
		DirectoryReference out = mkdref(dirName);
		List<String> result = new ArrayList<String>();
		listDir(out, result);
		return result;
	}

	private void listDir(DirectoryReference dir, List<String> collector)
			throws FilesystemAccessException, NoDirectoryEntryException,
			UnknownRunException {
		for (DirectoryEntry de : s.getRunDirectoryContents(id, dir)) {
			if (de instanceof FileReference)
				collector.add(de.getValue());
			else
				listDir((DirectoryReference) de, collector);
		}
	}

	/**
	 * Get the content type of the named file.
	 * 
	 * @param fileName
	 *            The name of the file.
	 * @return The content type (typically in the form "<tt>type/subtype</tt>").
	 * @throws FilesystemAccessException
	 *             If the file name is illegal.
	 * @throws NoDirectoryEntryException
	 *             If the file doesn't exist.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public String getFileType(String fileName)
			throws FilesystemAccessException, NoDirectoryEntryException,
			UnknownRunException {
		return s.getRunFileType(id, mkfref(fileName));
	}

	/**
	 * Get the length of the named file.
	 * 
	 * @param fileName
	 *            The name of the file.
	 * @return The length of the file, in bytes.
	 * @throws FilesystemAccessException
	 *             If the file name is illegal.
	 * @throws NoDirectoryEntryException
	 *             If the file doesn't exist.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public long getFileLength(String fileName)
			throws FilesystemAccessException, NoDirectoryEntryException,
			UnknownRunException {
		return s.getRunFileLength(id, mkfref(fileName));
	}

	/**
	 * Get the content of the named file.
	 * 
	 * @param fileName
	 *            The name of the file.
	 * @return The contents, as uninterpreted bytes. Note that large files
	 *         should not be retrieved this way or memory problems will be
	 *         likely to happen.
	 * @throws FilesystemAccessException
	 *             If the file name is illegal.
	 * @throws NoDirectoryEntryException
	 *             If the file doesn't exist.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public byte[] getFile(String fileName) throws FilesystemAccessException,
			NoDirectoryEntryException, UnknownRunException {
		return s.getRunFileContents(id, mkfref(fileName));
	}

	/**
	 * Get a compressed ZIP archive of the output of the run.
	 * 
	 * @return the bytes of the archive.
	 * @throws NoDirectoryEntryException
	 *             If the output was not (yet) written to a directory in the
	 *             standard location. This can happen if output is requested as
	 *             a baclava file.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public byte[] getOutputZip() throws NoDirectoryEntryException,
			UnknownRunException {
		try {
			return getZip("out");
		} catch (FilesystemAccessException e) {
			// ignore; should not happen (by construction)
			return null;
		}
	}

	/**
	 * Get a compressed ZIP archive of a directory (and its subdirs).
	 * 
	 * @param dirName
	 *            The name of the directory to retrieve
	 * @return the bytes of the archive.
	 * @throws FilesystemAccessException
	 *             If the directory name is illegal.
	 * @throws NoDirectoryEntryException
	 *             If the directory doesn't exist.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public byte[] getZip(String dirName) throws FilesystemAccessException,
			NoDirectoryEntryException, UnknownRunException {
		return s.getRunDirectoryAsZip(id, mkdref(dirName));
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	/**
	 * Destroy this run. <i>Note that after calling this, no other method on
	 * this class should be called.</i>
	 * 
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 * @throws NoUpdateException
	 *             If the user does not have permission to delete the run.
	 */
	public void destroy() throws NoUpdateException, UnknownRunException {
		s.destroyRun(id);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	private static Calendar getcal(XMLGregorianCalendar instant) {
		if (instant == null)
			return null;
		return instant.toGregorianCalendar();
	}

	/**
	 * @return When this workflow run will be automatically deleted.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public Calendar getExpiry() throws UnknownRunException {
		return getcal(s.getRunExpiry(id));
	}

	/**
	 * @return When this workflow run was created.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public Calendar getCreation() throws UnknownRunException {
		return getcal(s.getRunCreationTime(id));
	}

	/**
	 * @return When this workflow run was started, or <tt>null</tt> if it has
	 *         not started yet.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public Calendar getStart() throws UnknownRunException {
		return getcal(s.getRunStartTime(id));
	}

	/**
	 * @return When this workflow run finished or was forcibly terminated, or
	 *         <tt>null</tt> if it has not finished yet.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public Calendar getFinish() throws UnknownRunException {
		return getcal(s.getRunFinishTime(id));
	}

	/**
	 * Update the time this workflow run will be automatically destroyed. All
	 * workflow runs always have a lifespan, but the (owning) user can set the
	 * time of expiry.
	 * 
	 * @param newExpiry
	 *            When the run should expire. It is unwise to set this in the
	 *            past.
	 * @throws NoUpdateException
	 *             If the user does not have permission to update the lifespan
	 *             of the run.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public void setExpiry(Calendar newExpiry) throws NoUpdateException,
			UnknownRunException {
		s.setRunExpiry(id,
				dtf.newXMLGregorianCalendar((GregorianCalendar) newExpiry));
	}

	/**
	 * Extend the lifespan of a workflow run by a given number of seconds.
	 * 
	 * @param numberOfSeconds
	 *            The number of <i>seconds to add</i> on to the lifespan.
	 * @throws NoUpdateException
	 *             If the user does not have permission to update the lifespan
	 *             of the run.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public void setExpiryToNowPlus(int numberOfSeconds)
			throws NoUpdateException, UnknownRunException {
		GregorianCalendar c = new GregorianCalendar();
		c.add(Calendar.SECOND, numberOfSeconds);
		setExpiry(c);
	}
}