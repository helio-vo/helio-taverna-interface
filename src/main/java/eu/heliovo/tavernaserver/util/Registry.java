package eu.heliovo.tavernaserver.util;

import static eu.heliovo.registryclient.AccessInterfaceType.REST_SERVICE;
import static eu.heliovo.registryclient.AccessInterfaceType.SOAP_SERVICE;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import eu.heliovo.myexperiment.Group;
import eu.heliovo.myexperiment.Repository;
import eu.heliovo.registryclient.AccessInterfaceType;
import eu.heliovo.registryclient.HelioServiceName;
import eu.heliovo.registryclient.ServiceCapability;
import eu.heliovo.registryclient.ServiceRegistryClient;
import eu.heliovo.registryclient.impl.ServiceRegistryClientFactory;
import eu.heliovo.tavernaserver.Server;

/**
 * Simplified interface to the HELIO Registry.
 * 
 * @author Donal Fellows
 */
public class Registry {
	private static final ServiceRegistryClient registry = ServiceRegistryClientFactory
			.getInstance().getServiceRegistryClient();
	private static Group helio;

	/** Abstract name of myExperiment service */
	public static final HelioServiceName MYEXPERIMENT = HelioServiceName
			.register("myExperiment", "myExperiment");
	/** Abstract name of Taverna Server service */
	public static final HelioServiceName TAVSERV_INSTANCE = HelioServiceName
			.register("tavernaServer", "tavernaServer");
	/** Abstract capability of being a social workflow repository */
	public static final ServiceCapability WORKFLOW_REPOSITORY = ServiceCapability
			.register("WORKFLOW_REPOSITORY", "helio:workflows");
	/** Abstract capability of Taverna workflow execution */
	public static final ServiceCapability WORKFLOW_EXECUTION = ServiceCapability
			.register("TAVERNA_SERVER", "helio:tavserv");

	/**
	 * Do a lookup for an endpoint address in the registry.
	 * 
	 * @param name
	 *            The abstract name of the service
	 * @param capability
	 *            The capability that the service should have.
	 * @param type
	 *            The type of interface to look up.
	 * @return The address.
	 */
	public static URL getURL(HelioServiceName name,
			ServiceCapability capability, AccessInterfaceType type) {
		return registry.getBestEndpoint(name, capability, type).getUrl();
	}

	/**
	 * Retrieve the HELIO group from myExperiment.
	 * 
	 * @return The group search access interface.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static Group getHelioGroup() throws IOException, SAXException,
			ParserConfigurationException {
		if (helio == null)
			helio = new Repository(getURL(MYEXPERIMENT, WORKFLOW_REPOSITORY,
					REST_SERVICE).toString()).getGroup("helio");
		return helio;
	}

	/**
	 * Get a handle to a Taverna Server instance.
	 * 
	 * @param securityToken
	 *            The login token to use when communicating with the service
	 * @return The server handle. NB: an invalid security token will not be
	 *         detected at this stage.
	 */
	public static Server getTavernaServer(Object securityToken) {
		// FIXME Use the right type of tokens
		return new Server(getURL(TAVSERV_INSTANCE, WORKFLOW_EXECUTION,
				SOAP_SERVICE).toString(), securityToken);
	}
}
