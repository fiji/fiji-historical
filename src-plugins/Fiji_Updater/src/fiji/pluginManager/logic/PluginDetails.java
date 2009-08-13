package fiji.pluginManager.logic;
import java.util.ArrayList;
import java.util.List;

/*
 * Text description of a plugin, namely its function, links and authors
 */
public class PluginDetails {
	private String description;
	private List<String> links;
	private List<String> authors;

	public PluginDetails() {
		this(null, null, null);
	}

	public PluginDetails(String description, List<String> links, List<String> authors) {
		this.description = description;
		this.links = links;
		this.authors = authors;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setLinks(String[] links) {
		this.links = convertToList(links); //validating
	}

	public void setAuthors(String[] authors) {
		this.authors = convertToList(authors); //validating
	}

	public List<String> getLinks() {
		return links;
	}

	public List<String> getAuthors() {
		return authors;
	}

	public String getDescription() {
		if (description != null && description.trim().length() == 0)
			return null;
		return description;
	}

	private List<String> convertToList(String[] array) {
		if (array == null || array.length == 0)
			return null;
		List<String> list = new ArrayList<String>();
		for (String value : array) {
			value = value.replace("\r", "").replace("\n", "").trim();
			if (!value.isEmpty())
				list.add(value);
		}
		return list;
	}

	public boolean matchesDetails(String searchText) {
		// TODO: move this to the caller, otherwise it is performed once for _every_ plugin!
		String needle = searchText.toLowerCase().trim();
		if (links != null)
			for (String link : links)
				if (link.toLowerCase().indexOf(needle) >= 0) return true;
		if (authors != null)
			for (String author : authors)
				if (author.toLowerCase().indexOf(needle) >= 0) return true;
		if (description != null &&
				description.toLowerCase().indexOf(needle) >= 0)
			return true;
		return false;
	}
}
