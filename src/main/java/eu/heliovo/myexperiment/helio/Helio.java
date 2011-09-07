package eu.heliovo.myexperiment.helio;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import eu.heliovo.myexperiment.Group;
import eu.heliovo.myexperiment.Repository;
import eu.heliovo.registryclient.AccessInterface;
import eu.heliovo.registryclient.AccessInterfaceType;
import eu.heliovo.registryclient.HelioServiceName;
import eu.heliovo.registryclient.ServiceRegistryClient;
import eu.heliovo.registryclient.impl.ServiceRegistryClientFactory;

/**
 * Access to HELIO-specific things on myExperiment.
 * 
 * @author Donal Fellows
 */
public class Helio {
	private Helio() {
	}

	private static Repository myExperiment;
	private static Group helio;
	public static final HelioServiceName REGISTRY_KEY = HelioServiceName
			.register("myExperiment", null);
	public static final AccessInterfaceType myExpAPI = AccessInterfaceType
			.register("myExperiment_REST", "http://rest");
	public static final String GROUP_NAME = "helio";

	static {
		try {
			ServiceRegistryClient registry = ServiceRegistryClientFactory
					.getInstance().getServiceRegistryClient();
			for (AccessInterface ai : registry.getAllEndpoints(
					registry.getServiceDescriptor(REGISTRY_KEY), null, null))
				if (myExpAPI.equals(ai.getInterfaceType())) {
					myExperiment = new Repository(ai.getUrl().toString());
					break;
				}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the HELIO group on myExperiment.
	 * 
	 * @return a handle to the group that allows the lookup of shared workflows.
	 * @throws IOException
	 *             If things are unaccessible.
	 * @throws SAXException
	 *             If myExperiment produces bad XML.
	 */
	public static Group group() throws IOException, SAXException {
		if (helio == null)
			helio = myExperiment.getGroup(GROUP_NAME);
		return helio;
	}
}
