package eu.heliovo.tavernaserver;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;
import static javax.xml.ws.BindingProvider.PASSWORD_PROPERTY;
import static javax.xml.ws.BindingProvider.USERNAME_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.org.taverna.ns._2010.xml.server.TavernaRun;
import uk.org.taverna.ns._2010.xml.server.Workflow;
import uk.org.taverna.ns._2010.xml.server.soap.NoUpdateException;
import uk.org.taverna.ns._2010.xml.server.soap.TavernaServer;
import uk.org.taverna.ns._2010.xml.server.soap.TavernaService;

/**
 * Representation of a whole Taverna Server instance.
 * 
 * @author Donal Fellows
 */
public class Server {
	TavernaService s;

	/**
	 * Create a connection to the server at the default address with the default
	 * credentials.
	 */
	public Server() {
		s = new TavernaServer().getTavernaServerImplPort();
	}

	/**
	 * Create a connection to a server with the default credentials
	 * 
	 * @param serviceAddress
	 *            The address of the server endpoint to connect to.
	 */
	public Server(String serviceAddress) {
		s = new TavernaServer().getTavernaServerImplPort();
		((BindingProvider) s).getRequestContext().put(
				ENDPOINT_ADDRESS_PROPERTY, serviceAddress);
	}

	/**
	 * Create a connection to the server at the default address.
	 * 
	 * @param username
	 *            Who to connect to the server as.
	 * @param password
	 *            Password associated with the username.
	 */
	public Server(String username, String password) {
		s = new TavernaServer().getTavernaServerImplPort();
		Map<String, Object> requestContext = ((BindingProvider) s)
				.getRequestContext();
		requestContext.put(USERNAME_PROPERTY, username);
		requestContext.put(PASSWORD_PROPERTY, password);
	}

	/**
	 * Create a connection to a server.
	 * 
	 * @param serviceAddress
	 *            The address of the server endpoint to connect to.
	 * @param username
	 *            Who to connect to the server as.
	 * @param password
	 *            Password associated with the username.
	 */
	public Server(String serviceAddress, String username, String password) {
		s = new TavernaServer().getTavernaServerImplPort();
		Map<String, Object> requestContext = ((BindingProvider) s)
				.getRequestContext();
		requestContext.put(ENDPOINT_ADDRESS_PROPERTY, serviceAddress);
		requestContext.put(USERNAME_PROPERTY, username);
		requestContext.put(PASSWORD_PROPERTY, password);
	}

	/**
	 * Create a workflow run on the server.
	 * 
	 * @param workflowFile
	 *            A <i>local</i> t2flow file.
	 * @return A handle to the workflow run (which is not yet started).
	 * @throws SAXException
	 *             If the file doesn't at least hold a well-formed XML document.
	 * @throws IOException
	 *             If the file is not readable or some other IO error happens.
	 * @throws ParserConfigurationException
	 *             If the local Java installation is misconfigured.
	 * @throws NoUpdateException
	 *             If the server failed to build the workflow run.
	 */
	public Run createRun(File workflowFile) throws SAXException, IOException,
			ParserConfigurationException, NoUpdateException {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().parse(workflowFile);
		return createRun(doc.getDocumentElement());
	}

	/**
	 * Create a workflow run on the server.
	 * 
	 * @param workflow
	 *            A t2flow document's contents, already parsed.
	 * @return A handle to the workflow run (which is not yet started).
	 * @throws NoUpdateException
	 *             If the server failed to build the workflow run.
	 */
	public Run createRun(Element workflow) throws NoUpdateException {
		return new Run(s, workflow);
	}

	/**
	 * Returns the runs that the user is permitted to read on the server.
	 * 
	 * @return A collection of run handles; there is no point in manipulating
	 *         the collection.
	 */
	public Collection<Run> listRuns() {
		List<Run> r = new ArrayList<Run>();
		for (TavernaRun tr : s.listRuns()) {
			r.add(new Run(s, tr.getValue()));
		}
		return r;
	}

	/**
	 * Returns the protocols that may be used for subscribed notifications.
	 * Others may be used, but will result in no notification being sent.
	 * 
	 * @return A list of URI schemes.
	 */
	public List<String> getNotifierProtocols() {
		return s.getEnabledNotificationFabrics();
	}

	/**
	 * @return The maximum number of runs that the current user may have on this
	 *         server. Note that additional limits may be applied in practice
	 *         (e.g., a global limit across all users).
	 */
	public int getMaxRuns() {
		return s.getMaxSimultaneousRuns();
	}

	/**
	 * @return A list of all the types of listener that may be attached to a
	 *         workflow run. <i>May</i> be empty if there are currently no
	 *         workflow runs at all.
	 */
	public List<String> getListenerTypes() {
		return s.getPermittedListenerTypes();
	}

	/**
	 * Get a list of permitted workflows.
	 * 
	 * @return A list of the workflows that are permitted. If this is the empty
	 *         list, <i>all</i> workflows are permitted.
	 */
	public List<Element> getPermittedWorkflows() {
		ArrayList<Element> result = new ArrayList<Element>();
		for (Workflow w : s.getPermittedWorkflows())
			result.add((Element) w.getAny().get(0));
		return result;
	}
}