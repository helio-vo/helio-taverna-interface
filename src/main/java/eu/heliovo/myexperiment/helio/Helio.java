package eu.heliovo.myexperiment.helio;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import eu.heliovo.myexperiment.Group;
import eu.heliovo.myexperiment.Repository;


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

	static {
		try {
			myExperiment = new Repository();
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
			helio = myExperiment.getGroup("helio");
		return helio;
	}
}
