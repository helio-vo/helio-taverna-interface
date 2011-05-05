package eu.heliovo.myexperiment;

import java.net.URL;

abstract class Resource {
	String title;
	URL uri;

	public String getTitle() {
		return title;
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return getClass().isInstance(o) && uri.equals(((Resource) o).uri);
	}

	@Override
	public String toString() {
		return title + "<" + uri + ">";
	}
}
