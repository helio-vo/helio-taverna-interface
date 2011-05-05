package eu.heliovo.tavernaserver;

import java.util.Collections;
import java.util.List;

import uk.org.taverna.ns._2010.xml.server.soap.NoListenerException;
import uk.org.taverna.ns._2010.xml.server.soap.NoUpdateException;
import uk.org.taverna.ns._2010.xml.server.soap.TavernaService;
import uk.org.taverna.ns._2010.xml.server.soap.UnknownRunException;

/**
 * Representation of a listener attached to a workflow run.
 * 
 * @author Donal Fellows
 */
public class Listener {
	private TavernaService s;
	private String runid;
	private String listener;

	Listener(TavernaService s, String runid, String listener) {
		this.s = s;
		this.runid = runid;
		this.listener = listener;
	}

	/**
	 * @return The name of the listener.
	 */
	public String getName() {
		return listener;
	}

	/**
	 * @return The configuration document for the listener. This is read-only.
	 * @throws NoListenerException
	 *             If the listener is unknown.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public String getConfiguration() throws NoListenerException,
			UnknownRunException {
		return s.getRunListenerConfiguration(runid, listener);
	}

	/**
	 * @return The collection of properties that the listener knows about.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public List<String> getProperties() throws UnknownRunException {
		try {
			return s.getRunListenerProperties(runid, listener);
		} catch (NoListenerException ex) {
			return Collections.emptyList();
		}
	}

	/**
	 * Read a listener property.
	 * 
	 * @param property
	 *            The name of the property to read.
	 * @return The value of the property, or <tt>null</tt> if the property
	 *         doesn't exist.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 */
	public String getProperty(String property) throws UnknownRunException {
		try {
			return s.getRunListenerProperty(runid, listener, property);
		} catch (NoListenerException ex) {
			return null;
		}
	}

	/**
	 * Update a listener property.
	 * 
	 * @param property
	 *            The name of the property to write.
	 * @param value
	 *            The value to write into the property.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., destroyed or expired).
	 * @throws NoUpdateException
	 *             If the user is not permitted to write to the property (e.g.,
	 *             because they only have read access to the workflow run).
	 */
	public void setProperty(String property, String value)
			throws UnknownRunException, NoUpdateException {
		try {
			s.setRunListenerProperty(runid, listener, property, value);
		} catch (NoListenerException ex) {
			// do nothing
		}
	}
}
