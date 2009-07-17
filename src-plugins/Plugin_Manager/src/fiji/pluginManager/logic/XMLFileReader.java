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
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ErrorHandler;
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
	private List<Dependency> dependencyList;
	private String dependencyFilename;
	private String dependencyTimestamp;
	private String dependencyRelation;
	private String currentTag;

	//TODO
	/*
	 pluginRecords will consist of plugin names mapped to list of their respective versions.
	 Latest version can be retrieved using PluginCollection's method
	 */
	//private Map<String, List<PluginObject>> pluginRecords;
	private Map<String, PluginObject> pluginRecords;

	public XMLFileReader(String fileLocation) throws ParserConfigurationException, IOException, SAXException {
		//TODO
		//pluginRecords = new TreeMap<String, List<PluginObject>>();
		pluginRecords = new TreeMap<String, PluginObject>();
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		//factory.setValidating(true); //commented out per postel's law
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();

		XMLReader xr = parser.getXMLReader();
		xr.setContentHandler(this);
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(new InputSource(fileLocation));
	}

	//TODO: The role this method performs should be left unchanged, just refactor it
	public void getLatestDigestsAndDates(Map<String, String> latestDigests, Map<String, String> latestDates) {
		Iterator<PluginObject> iterPlugins = pluginRecords.values().iterator();
		while (iterPlugins.hasNext()) {
			PluginObject plugin = iterPlugins.next();
			latestDigests.put(plugin.getFilename(), plugin.getmd5Sum());
			latestDates.put(plugin.getFilename(), plugin.getTimestamp());
		}
	}

	//TODO
	public Map<String,PluginObject> getLatestFijiPlugins() {
		return pluginRecords;
	}

	private PluginObject getPluginMatching(String filename, String timestamp) {
		PluginObject plugin = pluginRecords.get(filename);
		if (plugin != null) {
			if (plugin.getTimestamp().equals(timestamp))
				return plugin;
		}
		throw new Error("Plugin " + filename + ", " + timestamp + " does not exist");
	}

	//Get filesize associated with specified version, assumed filename & timestamp are correct
	public int getFilesizeFrom(String filename, String timestamp) {
		return getPluginMatching(filename, timestamp).getFilesize();
	}

	//Get description associated with specified version, assumed filename & timestamp are correct
	public String getDescriptionFrom(String filename, String timestamp) {
		return getPluginMatching(filename, timestamp).getDescription();
	}

	//Get dependencies associated with specified version, assumed filename & timestamp are correct
	public List<Dependency> getDependenciesFrom(String filename, String timestamp) {
		return getPluginMatching(filename, timestamp).getDependencies();
	}

	public boolean matchesFilenameAndDigest(String outputFilename, String outputDigest) {
		if (getTimestamp(outputFilename, outputDigest) == null) {
			return false; //implies plugin of given filename and digest do not exist
		} else {
			return true;
		}
	}

	//Get timestamp associated with specified version, assumed filename & digest are correct
	public String getTimestamp(String outputFilename, String outputDigest) {
		PluginObject plugin = pluginRecords.get(outputFilename);
		if (plugin != null) {
			if (plugin.getmd5Sum().equals(outputDigest))
				return plugin.getTimestamp();
		}
		return null; //digest does not exist
	}

	public void startDocument () { }

	public void endDocument () {
		//no longer needed after parsing, set back to default values
		filename = null;
		timestamp = null;
		digest = null;
		description = null;
		filesize = null;
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
			digest = "";
			timestamp = "";
			description = "";
			filesize = "";
			dependencyList = new ArrayList<Dependency>();
		} else if (currentTag.equals("dependency")) {
			dependencyFilename = "";
			dependencyTimestamp = "";
			dependencyRelation = "";
		}
	}

	public void endElement (String uri, String name, String qName) {
		String tagName;
		if ("".equals (uri))
			tagName = qName;
		else
			tagName = name;
		
		if (tagName.equals("version")) {
			PluginObject plugin = new PluginObject(filename, digest, timestamp, PluginObject.STATUS_UNINSTALLED, true);
			plugin.setDescription(description);
			plugin.setFilesize(Integer.parseInt(filesize));
			if (dependencyList.size() > 0)
				plugin.setDependency(dependencyList);
			pluginRecords.put(plugin.getFilename(), plugin);

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
			}
		}
	}

}
class XMLFileErrorHandler implements ErrorHandler {
	public void error(SAXParseException exception) throws SAXException {
		throw new Error(exception.getLocalizedMessage());
	}

	public void fatalError(SAXParseException exception) throws SAXException {
		throw new Error(exception.getLocalizedMessage());
	}

	public void warning(SAXParseException exception) throws SAXException {
		System.out.println("XML File Warning: " + exception.getLocalizedMessage());
	}
}