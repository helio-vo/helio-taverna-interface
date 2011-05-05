package eu.heliovo.myexperiment;

import java.io.IOException;
import java.net.URL;

import org.joda.time.DateTime;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import eu.heliovo.tavernaserver.Run;
import eu.heliovo.tavernaserver.Server;

import uk.org.taverna.ns._2010.xml.server.soap.NoUpdateException;

/**
 * Class describing a workflow published in myExperiment.
 * 
 * @author Donal Fellows
 */
public class Workflow extends Resource {
	private final Repository repository;

	Workflow(Repository myExperiment) {
		repository = myExperiment;
	}

	int version;
	private String id;
	private String description;
	private URL svg;
	private String contentType;
	private boolean populated = false;
	private DateTime date;
	private URL contentLocation;
	private String uploader;

	private String get(Element wfdoc, String tag) {
		for (Element elem : repository.elems(wfdoc, tag))
			return elem.getTextContent();
		return "";
	}

	private void populate() {
		try {
			Element wfdoc = repository.fetchXml(uri, "description", "svg",
					"uploader", "content-type", "updated-at", "content-uri",
					"id");
			id = get(wfdoc, "id");
			description = get(wfdoc, "description");
			contentType = get(wfdoc, "content-type");
			uploader = get(wfdoc, "uploader");
			svg = new URL(get(wfdoc, "svg"));
			contentLocation = new URL(get(wfdoc, "content-uri"));
			date = Repository.dtf.parseDateTime(get(wfdoc, "updated-at"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} finally {
			populated = true;
		}
	}

	public String getId() {
		if (!populated)
			populate();
		return id;
	}

	public int getVersion() {
		return version;
	}

	public URL getSVGLink() {
		if (!populated)
			populate();
		return svg;
	}

	public String getContentType() {
		if (!populated)
			populate();
		return contentType;
	}

	public String getDescription() {
		if (!populated)
			populate();
		return description;
	}

	public DateTime getDate() {
		if (!populated)
			populate();
		return date;
	}

	public URL getWorkflowLink() {
		if (!populated)
			populate();
		return contentLocation;
	}

	public String getUploader() {
		if (!populated)
			populate();
		return uploader;
	}

	public Run submit(Server tavernaServer) throws NoUpdateException,
			IOException, SAXException, BadWorkflowTypeException {
		if (!"application/vnd.taverna.t2flow+xml".equals(getContentType()))
			throw new BadWorkflowTypeException("workflow not in t2flow format");
		return tavernaServer.createRun(repository
				.fetchXmlDoc(getWorkflowLink()));
	}
}