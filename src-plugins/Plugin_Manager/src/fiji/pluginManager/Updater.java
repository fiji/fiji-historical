package fiji.pluginManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/*
 * This class is responsible for writing updates to server, upon given the updated
 * plugin records (Map of plugins to all versions).
 * 
 * 1st Step: Generates the updated records (newPluginRecords & filesToUpload)
 * 2nd Step: Prepare print streams for writing XML and text files.
 * 3rd Step: Upload plugin file(s) to server
 * 4th Step: Write XML file using pluginRecords, save to server
 * 5th Step: Write text file using pluginRecords, save to server
 * 
 * Note: Plugins are uploaded differently
 * - Non-Fiji plugins & new versions of Fiji Plugins will have files AND details uploaded
 * - Uninstalled & up-to-date plugins will ONLY have their details uploaded (i.e.: XML file)
 * 
 */
public class Updater extends PluginDataObservable {
	private String xmlSavepath;
	private String txtSavepath;
	private PrintStream xmlPrintStream;
	private PrintStream txtPrintStream;
	private StreamResult streamResult;
	private SAXTransformerFactory tf;
	private TransformerHandler handler;
	private final String XALAN_INDENT_AMOUNT =
		"{http://xml.apache.org/xslt}" + "indent-amount";

	//accessible information after uploading tasks are done
	public List<PluginObject> changesList;
	private Map<String, List<PluginObject>> newPluginRecords;
	private List<PluginObject> filesToUpload; //list of plugins whose files has to be uploaded
	private DependencyAnalyzer dependencyAnalyzer;
	private XMLFileReader xmlFileReader;

	public Updater(PluginManager pluginManager) {
		//For purposes of uploading to server
		super(true);

		PluginCollection pluginCollection = (PluginCollection)pluginManager.pluginCollection;
		changesList = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_UPLOAD);
		((PluginCollection)changesList).resetChangeAndUploadStatuses();
		dependencyAnalyzer = new DependencyAnalyzer(pluginCollection);
		xmlFileReader = pluginManager.xmlFileReader;

		xmlSavepath = PluginManager.defaultServerPath + PluginManager.XML_FILENAME;
		txtSavepath = PluginManager.defaultServerPath + PluginManager.TXT_FILENAME;
	}

	public void generateNewPluginRecords() throws IOException {
		//Checking list for Fiji plugins - Either new versions or changes to existing ones
		filesToUpload = new PluginCollection();
		newPluginRecords = xmlFileReader.getXMLRecords();
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String name = pluginNamelist.next();
			List<PluginObject> versionList = newPluginRecords.get(name);
			for (PluginObject pluginToUpload : changesList) {
				if (pluginToUpload.getFilename().equals(name)) {
					PluginObject version = getPluginMatchingDigest(pluginToUpload.getmd5Sum(), versionList);
					if (version != null) {
						//edit the existing version's details, but no new file uploaded
						version.setDescription(pluginToUpload.getDescription());
					} else {
						//this version does not appear in existing records, therefore add it
						addPluginToVersionList(pluginToUpload, versionList);
					}
					break;
				}
			}
		}

		//Checking list for non-Fiji plugins to add to new records
		for (PluginObject pluginToUpload : changesList) {
			String name = pluginToUpload.getFilename();
			List<PluginObject> versionList = newPluginRecords.get(name);
			if (versionList == null) { //non-Fiji plugin, therefore add it
				versionList = new PluginCollection();
				addPluginToVersionList(pluginToUpload, versionList);
				newPluginRecords.put(name, versionList);
			}
		}
	}

	private void addPluginToVersionList(PluginObject pluginToUpload, List<PluginObject> versionList) throws IOException {
		pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(pluginToUpload.getFilename()));
		versionList.add(pluginToUpload);
		filesToUpload.add(pluginToUpload); //indicates plugin file itself has to be uploaded
	}

	private PluginObject getPluginMatchingDigest(String digest, List<PluginObject> pluginList) {
		for (PluginObject plugin : pluginList) {
			if (digest.equals(plugin.getmd5Sum())) {
				return plugin;
			}
		}
		return null;
	}

	public void uploadFilesToServer() throws SAXException, TransformerConfigurationException, IOException  {
		prepareFilesForWriting();
		writePlugins();
		writeXMLFile();
		System.out.println("XML file written to server");
		writeTxtFile();
		System.out.println("Text file written to server");
		convertUploadStatusesToModified();
		setStatusComplete(); //indicate to observer there's no more tasks
	}

	//Prepare the print streams to write plugin indexes to (XML file and current.txt)
	private void prepareFilesForWriting() throws TransformerConfigurationException, IOException {
		txtPrintStream = new PrintStream(txtSavepath); //current.txt
		xmlPrintStream = new PrintStream(xmlSavepath); //pluginRecords.xml
		streamResult = new StreamResult(xmlPrintStream);
		tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

		handler = tf.newTransformerHandler();
		Transformer serializer = handler.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PluginManager.XML_DIRECTORY + "/" + PluginManager.DTD_FILENAME);
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");
		serializer.setOutputProperty(XALAN_INDENT_AMOUNT, "4");
		handler.setResult(streamResult);
	}

	//Write plugin files (JAR) to the server
	private void writePlugins() {
		currentlyLoaded = 0;
		totalToLoad = filesToUpload.size();
		String remotePrefix = new File(xmlSavepath).getParent();
		for (PluginObject plugin : filesToUpload) {
			taskname = plugin.getFilename();
			String sourcePath = prefix(taskname);
			String targetPath = remotePrefix + File.separator + taskname + "-" + plugin.getTimestamp();
			try {
				copyFile(sourcePath, targetPath);
				plugin.setUploadStatusToFileUploaded();
				changeStatus(taskname, ++currentlyLoaded, totalToLoad);
			} catch (IOException e) {
				plugin.setUploadStatusToFail();
			}
		}
	}

	//pluginRecords consist of key of Plugin names, each maps to lists of different versions
	private void writeTxtFile() {
		changeStatus(PluginManager.TXT_FILENAME, 0, 1);

		//start writing
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String filename = pluginNamelist.next();
			List<PluginObject> versionList = newPluginRecords.get(filename);
			PluginObject latestPlugin = null;
			for (PluginObject plugin : versionList) {
				if (latestPlugin == null ||
					(latestPlugin != null &&
					latestPlugin.getTimestamp().compareTo(plugin.getTimestamp()) < 0)) {
					latestPlugin = plugin;
				}
			}
			txtPrintStream.println(latestPlugin.getFilename() + " " +
					latestPlugin.getTimestamp() + " " + latestPlugin.getmd5Sum());
		}

		changeStatus(PluginManager.TXT_FILENAME, 1, 1);
		txtPrintStream.close();
	}

	//pluginRecords consist of key of Plugin names, each maps to lists of different versions
	private void writeXMLFile() throws SAXException {
		changeStatus(PluginManager.XML_FILENAME, 0, 1);

		//Start writing
		handler.startDocument();
		AttributesImpl attrib = new AttributesImpl();

		handler.startElement("", "", "pluginRecords", attrib);
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String filenameAttribute = pluginNamelist.next();
			attrib.clear();
			attrib.addAttribute("", "", "filename", "CDATA", filenameAttribute);
			handler.startElement("", "", "plugin", attrib);
			List<PluginObject> versionList = newPluginRecords.get(filenameAttribute);
			for (PluginObject plugin : versionList) {
				attrib.clear();
				handler.startElement("", "", "version", attrib);
					writeSimpleTag("checksum", attrib, plugin.getmd5Sum());
					writeSimpleTag("timestamp", attrib, plugin.getTimestamp());
					String description = (plugin.getDescription() == null ? "" : plugin.getDescription());
					writeSimpleTag("description", attrib, description);
					String strFilesize = "" + plugin.getFilesize();
					writeSimpleTag("filesize", attrib, strFilesize);
					if (plugin.getDependencies() != null && plugin.getDependencies().size() > 0) {
						List<Dependency> dependencies = plugin.getDependencies();
						for (Dependency dependency : dependencies) {
							attrib.clear();
							handler.startElement("", "", "dependency", attrib);
								writeSimpleTag("filename", attrib, dependency.getFilename());
								writeSimpleTag("date", attrib, dependency.getTimestamp());
								//hd.characters(dependency.getRelation().toCharArray(), 0, dependency.getRelation().length());
								writeSimpleTag("relation", attrib, "at-least");
							handler.endElement("", "", "dependency");
						}
					}
				handler.endElement("", "", "version");
			}
			handler.endElement("", "", "plugin");
		}
		handler.endElement("", "", "pluginRecords");
		handler.endDocument();

		changeStatus(PluginManager.XML_FILENAME, 1, 1);
		xmlPrintStream.close();
	}

	private void writeSimpleTag(String tagName, AttributesImpl attrib, String value) throws SAXException {
		attrib.clear();
		handler.startElement("", "", tagName, attrib);
		handler.characters(value.toCharArray(), 0, value.length());
		handler.endElement("", "", tagName);
	}

	//Method assumes no exception occurred before:
	//Any plugins not given a status yet should be set to "Details modified only"
	private void convertUploadStatusesToModified() {
		for (PluginObject plugin : changesList) {
			if (!plugin.uploadPluginFileDone() && !plugin.uploadFailed()) {
				//plugin file upload done beforehand
				plugin.setUploadStatusToModified();
			}
		}
	}

	public Iterator<PluginObject> iterUploadSuccess() {
		return ((PluginCollection)changesList).getIterator(
				PluginCollection.FILTER_UPLOAD_SUCCESS);
	}

	public Iterator<PluginObject> iterUploadFail() {
		return ((PluginCollection)changesList).getIterator(
				PluginCollection.FILTER_UPLOAD_FAIL);
	}

	public int numberOfSuccessfulUploads() {
		return ((PluginCollection)changesList).getList(
				PluginCollection.FILTER_UPLOAD_SUCCESS).size();
	}

	public int numberOfFailedUploads() {
		return ((PluginCollection)changesList).getList(
				PluginCollection.FILTER_UPLOAD_FAIL).size();
	}

}
