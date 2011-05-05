package eu.heliovo.myexperiment;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Class describing a group in myExperiment.
 * 
 * @author Donal Fellows
 */
public class Group extends Resource {
	private final Repository repository;

	Group(Repository myExperiment) {
		repository = myExperiment;
	}

	private String description;
	private String owner;
	private boolean populated;

	private String get(Element wfdoc, String tag) {
		for (Element elem : repository.elems(wfdoc, tag))
			return elem.getTextContent();
		return "";
	}

	private void populate() {
		if (populated)
			return;
		try {
			Element wfdoc = repository.fetchXml(uri, "description", "owner");
			description = get(wfdoc, "description");
			owner = get(wfdoc, "owner");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} finally {
			populated = true;
		}
	}

	public String getDescription() {
		populate();
		return description;
	}

	public String getOwner() {
		populate();
		return owner;
	}

	/**
	 * List the workflows that have been shared with this group.
	 * 
	 * @return the handles to the workflows.
	 * @throws IOException
	 *             If the fetch fails.
	 * @throws SAXException
	 *             If the response isn't well-formed XML.
	 */
	public List<Workflow> getWorkflows() throws IOException, SAXException {
		List<Workflow> result = new ArrayList<Workflow>();
		for (Element shared : repository.elems(
				repository.fetchXml(uri, "shared-items"), "shared-items")) {
			for (Element elem : repository.elems(shared, "workflow")) {
				Workflow w = new Workflow(repository);
				w.uri = new URL(elem.getAttribute("uri"));
				w.version = Integer.parseInt(elem.getAttribute("version"));
				w.title = elem.getTextContent();
				result.add(w);
			}
		}
		return result;
	}
}