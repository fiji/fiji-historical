package fiji.pluginManager;
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

	//private List<PluginObject> pluginRecordsList;
	private Map<String, List<PluginObject>> pluginRecordsList;

	public XMLFileReader(String fileLocation) throws ParserConfigurationException, IOException, SAXException {
		pluginRecordsList = new TreeMap<String, List<PluginObject>>();
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();

		XMLReader xr = parser.getXMLReader();
		xr.setContentHandler(this);
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(new InputSource(fileLocation));
	}

	public void getLatestDigestsAndDates(Map<String, String> latestDigests, Map<String, String> latestDates) {
		Iterator<String> pluginNamelist = pluginRecordsList.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String name = pluginNamelist.next();
			List<PluginObject> versionList = pluginRecordsList.get(name);
			for (PluginObject plugin : versionList) {
				String filename = plugin.getFilename();
				String newMd5Sum = plugin.getmd5Sum();
				String newDate = plugin.getTimestamp();
				//if already exists in the lists
				if (latestDigests.containsKey(filename)) {
					String prevDate = latestDates.get(filename);
					if (newDate.compareTo(prevDate) <= 0) {
						continue;
					} else {
						//Replace with timestamp if it is newer
						latestDigests.remove(filename);
						latestDates.remove(filename);
					}
				}
				latestDigests.put(filename, newMd5Sum);
				latestDates.put(filename, newDate);
			}
		}
	}

	public Map<String,List<PluginObject>> getXMLRecords() {
		return pluginRecordsList;
	}

	private PluginObject getPluginMatching(String filename, String timestamp) {
		List<PluginObject> versionList = pluginRecordsList.get(filename);
		if (versionList != null) {
			for (PluginObject plugin : versionList) {
				if (plugin.getTimestamp().equals(timestamp)) {
					return plugin;
				}
			}
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
		List<PluginObject> versionList = pluginRecordsList.get(outputFilename);
		if (versionList != null) {
			for (PluginObject plugin : versionList) {
				if (plugin.getmd5Sum().equals(outputDigest))
					return plugin.getTimestamp();
			}
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

			List<PluginObject> versionList;
			if (pluginRecordsList.containsKey(plugin.getFilename())) {
				versionList = pluginRecordsList.get(plugin.getFilename());
			} else {
				versionList = new PluginCollection();
				pluginRecordsList.put(plugin.getFilename(), versionList);
			}
			versionList.add(plugin);
		} else if (tagName.equals("dependency")) {
			//TODO: dependencyRelation string would be involved too...
			Dependency dependency = new Dependency(dependencyFilename, dependencyTimestamp);
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