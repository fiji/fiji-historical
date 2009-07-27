package fiji.pluginManager.logic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * XML File Reader, as name implies, reads an already downloaded XML file of containing all
 * existing records of Fiji plugins (Upon calling a constructor). It would save the entire
 * information inside pluginRecordsList.
 * 
 * Upon specific requests (call methods) for certain information, they will be retrieved from
 * pluginRecordsList, not the XML file itself.
 * 
 */
public class XMLFileReader extends DefaultHandler {
	private String filename;
	private String timestamp;
	private String digest;
	private String description;
	private String filesize;
	private String link;
	private List<String> links;
	private String author;
	private List<String> authors;
	private List<Dependency> dependencyList;
	private String dependencyFilename;
	private String dependencyTimestamp;
	private String dependencyRelation;
	private String currentTag;

	//plugin names mapped to list of their respective versions
	private Map<String, List<PluginObject>> pluginRecords;

	public XMLFileReader(String fileLocation) throws ParserConfigurationException,
	IOException, SAXException {
		pluginRecords = new TreeMap<String, List<PluginObject>>();
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		//factory.setValidating(true); //commented out per postel's law
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();

		XMLReader xr = parser.getXMLReader();
		xr.setContentHandler(this);
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(new InputSource(fileLocation));
	}

	public void getLatestDigestsAndDates(Map<String, String> latestDigests, Map<String, String> latestDates) {
		//Iterator<PluginObject> iterPlugins = pluginRecords.values().iterator();
		Iterator<String> iterNamelist = pluginRecords.keySet().iterator();
		while (iterNamelist.hasNext()) {
			String pluginName = iterNamelist.next();
			PluginCollection versions = (PluginCollection)pluginRecords.get(pluginName);
			PluginObject plugin = versions.getLatestPlugin();
			latestDigests.put(plugin.getFilename(), plugin.getmd5Sum());
			latestDates.put(plugin.getFilename(), plugin.getTimestamp());
		}
	}

	public Map<String, List<PluginObject>> getAllPluginRecords() {
		return pluginRecords;
	}

	//Get the latest version, which _will_ have all the important details/information
	private PluginObject getPluginMatching(String filename) {
		PluginCollection versions = (PluginCollection)pluginRecords.get(filename);
		if (versions != null) {
			return versions.getLatestPlugin();
		}
		throw new Error("Plugin " + filename + " does not exist.");
	}

	//Get filesize associated with latest version, assumed filename is correct
	public int getFilesizeFrom(String filename) {
		return getPluginMatching(filename).getFilesize(); //only useful for latest
	}

	//Get description associated with latest version, assumed filename is correct
	public PluginDetails getPluginDetailsFrom(String filename) {
		return getPluginMatching(filename).getPluginDetails();
	}

	//Get dependencies associated with latest version, assumed filename is correct
	public List<Dependency> getDependenciesFrom(String filename) {
		return getPluginMatching(filename).getDependencies(); //only useful for latest
	}

	//Get timestamp associated with specified version, assumed filename & digest are correct
	public String getTimestampFromRecords(String filename, String digest) {
		PluginCollection versions = (PluginCollection)pluginRecords.get(filename);
		String timestamp = null;
		if (versions != null) {
			PluginObject match = versions.getPluginFromDigest(filename, digest);
			if (match != null)
				timestamp = match.getTimestamp();
		}
		return timestamp;
	}

	public void startDocument () { }

	public void endDocument () {
		//no longer needed after parsing, set back to default values
		filename = null;
		timestamp = null;
		digest = null;
		description = null;
		filesize = null;
		link = null;
		links = null;
		author = null;
		authors = null;
		dependencyList = null;
		dependencyFilename = null;
		dependencyTimestamp = null;
		dependencyRelation = null;
		currentTag = null;
	}

	public void startElement (String uri, String name, String qName, Attributes atts) {
		if ("".equals (uri))
			currentTag = qName;
		else
			currentTag = name;

		if (currentTag.equals("plugin")) {
			filename = atts.getValue("filename");
		} else if (currentTag.equals("version")) {
			resetPluginValues();
		} else if (currentTag.equals("dependency")) {
			dependencyFilename = "";
			dependencyTimestamp = "";
			dependencyRelation = "";
		} else if (currentTag.equals("link")) {
			link = "";
		} else if (currentTag.equals("author")) {
			author = "";
		} else if (currentTag.equals("previous-version")) {
			resetPluginValues();
			timestamp = atts.getValue("timestamp");
			digest = atts.getValue("checksum");
			filesize = "0";
		}
	}

	private void resetPluginValues() {
		digest = "";
		timestamp = "";
		description = "";
		filesize = "";
		dependencyList = new ArrayList<Dependency>();
		links = new ArrayList<String>();
		authors = new ArrayList<String>();
	}

	public void endElement (String uri, String name, String qName) {
		String tagName;
		if ("".equals (uri))
			tagName = qName;
		else
			tagName = name;

		if (tagName.equals("version") || tagName.equals("previous-version")) {
			PluginObject plugin = new PluginObject(filename, digest, timestamp, PluginObject.STATUS_UNINSTALLED, true);
			if (tagName.equals("version"))
				plugin.setPluginDetails(new PluginDetails(description, links, authors));
			plugin.setFilesize(Integer.parseInt(filesize));
			if (dependencyList.size() > 0)
				plugin.setDependency(dependencyList);

			List<PluginObject> versions;
			if (pluginRecords.containsKey(plugin.getFilename())) {
				versions = pluginRecords.get(plugin.getFilename());
			} else {
				versions = new PluginCollection();
				pluginRecords.put(plugin.getFilename(), versions);
			}
			versions.add(plugin);

		} else if (tagName.equals("dependency")) {
			if (dependencyRelation.toLowerCase().equals(Dependency.RELATION_AT_LEAST)) {
				dependencyRelation = Dependency.RELATION_AT_LEAST;
			} else if (dependencyRelation.toLowerCase().equals(Dependency.RELATION_AT_MOST)) {
				dependencyRelation = Dependency.RELATION_AT_MOST;
			} else if (dependencyRelation.toLowerCase().equals(Dependency.RELATION_EXACT)) {
				dependencyRelation = Dependency.RELATION_EXACT;
			} else {
				throw new Error("Dependency Relation " + dependencyRelation + " does not exist for " + dependencyFilename);
			}
			Dependency dependency = new Dependency(dependencyFilename, dependencyTimestamp, dependencyRelation);
			dependencyList.add(dependency);

		} else if (tagName.equals("link")) {
			links.add(link);
		} else if (tagName.equals("author")) {
			authors.add(author);
		}
	}

	public void characters(char ch[], int start, int length) {
		for (int i = start; i < start + length; i++) {
			if (currentTag.equals("checksum")) {
				digest += ch[i];
			} else if (currentTag.equals("timestamp")) {
				timestamp += ch[i];
			} else if (currentTag.equals("description")) {
				description += ch[i];
			} else if (currentTag.equals("filesize")) {
				filesize += ch[i];
			} else if (currentTag.equals("filename")) {
				dependencyFilename += ch[i];
			} else if (currentTag.equals("date")) {
				dependencyTimestamp += ch[i];
			} else if (currentTag.equals("relation")) {
				dependencyRelation += ch[i];
			} else if (currentTag.equals("link")) {
				link += ch[i];
			} else if (currentTag.equals("author")) {
				author += ch[i];
			}
		}
	}

}