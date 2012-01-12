package eu.heliovo.myexperiment;

import static org.joda.time.format.DateTimeFormat.forPattern;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Simple class for looking up groups and workflows in myExperiment.
 * 
 * @author Donal Fellows
 */
public class Repository {
	public Repository() throws ParserConfigurationException {
		this("http://www.myexperiment.org/search.xml");
	}

	public Repository(String searchURL) throws ParserConfigurationException {
		this.searchURL = searchURL;
		this.db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	private String searchURL;
	private DocumentBuilder db;
	static final DateTimeFormatter dtf = forPattern("EEE MMM dd HH:mm:ss ZZZ yyyy");

	// http://www.myexperiment.org/search.xml?type=group&query=helio
	// Text value of 'group' child is name (helio)
	// Get uri attribute, add '&elements=shared-items'
	// http://www.myexperiment.org/group.xml?id=101&elements=shared-items
	// Text value of each 'shared-item' child is name
	// Get uri attribute, add '&elements=svg,content'
	// http://www.myexperiment.org/workflow.xml?id=940&elements=svg,content
	// Content of 'svg' element is link to SVG picture of WF
	// Content of 'content' element is base-64 encoded workflow doc

	private Element searchXml(String type, String term, String... elements)
			throws IOException, SAXException {
		String urlStr = searchURL + "?type=" + type + "&query=" + term;
		if (elements != null && elements.length > 0) {
			String sep = "&elements=";
			for (String el : elements) {
				urlStr += sep + el;
				sep = ",";
			}
		}
		return fetchXmlDoc(new URL(urlStr));
	}

	Element fetchXmlDoc(URL location) throws IOException, SAXException {
		// System.out.println("fetching " + location);
		InputStream is = location.openStream();
		try {
			return db.parse(is).getDocumentElement();
		} finally {
			is.close();
		}
	}

	Element fetchXml(URL entryPoint, String... elements) throws IOException,
			SAXException {
		String query = entryPoint.getQuery();
		if (query == null)
			query = "?";
		else if (!query.startsWith("?"))
			query = "?" + query;
		if (elements != null && elements.length > 0) {
			String sep = (query == null ? "elements=" : "&elements=");
			for (String el : elements) {
				query += sep + el;
				sep = ",";
			}
		}
		return fetchXmlDoc(new URL(entryPoint, entryPoint.getPath() + query));
	}

	List<Element> elems(Element container, String name) {
		List<Element> result = new ArrayList<Element>();
		NodeList nl = container.getElementsByTagName(name);
		for (int i = 0; i < nl.getLength(); i++)
			result.add((Element) nl.item(i));
		return result;
	}

	public Set<Group> getGroups(String namePart) throws IOException,
			SAXException {
		HashSet<Group> result = new HashSet<Group>();
		for (Element elem : elems(searchXml("group", namePart), "group")) {
			Group g = new Group(this);
			g.uri = new URL(elem.getAttribute("uri"));
			g.title = elem.getTextContent();
			result.add(g);
		}
		return result;
	}

	public Group getGroup(String name) throws IOException, SAXException {
		for (Group g : getGroups(name)) {
			if (name.equals(g.title))
				return g;
		}
		return null;
	}
}
