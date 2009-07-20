package fiji.pluginManager.logic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import fiji.pluginManager.logic.FileUploader.SourceFile;
import fiji.pluginManager.logic.FileUploader.UploadListener;

/*
 * This class is responsible for writing updates to server, upon given the updated
 * plugin records (Map of plugins to all versions).
 * 
 * 1st Step: Generates the updated records (newPluginRecords & filesToUpload)
 * 2nd Step: Writes XML and current.txt contents and connect to server
 * 3rd Step: Upload plugin file(s) and/or other information to server
 * 4th Step: Write XML file using pluginRecords, save to server
 * 5th Step: Write text file using pluginRecords, save to server
 * 
 * Note: Plugins are uploaded differently
 * - Non-Fiji plugins & new versions of Fiji Plugins will have files AND details uploaded
 * - Uninstalled & up-to-date plugins will ONLY have their details uploaded (i.e.: XML file)
 */
public class Updater extends PluginData {
	private FileUploader fileUploader;
	private String xmlSavePath;
	private String xmlRelativePath;
	private String txtSavePath;
	private String txtRelativePath;
	private ByteArrayOutputStream xmlWriter; //writes to memory
	private StreamResult streamResult;
	private SAXTransformerFactory tf;
	private TransformerHandler handler;
	private final String XALAN_INDENT_AMOUNT =
		"{http://xml.apache.org/xslt}" + "indent-amount";

	//accessible information after uploading tasks are done
	public List<PluginObject> changesList;
	public Map<String, List<PluginObject>> newPluginRecords;
	private ArrayList<SourceFile> filesToUpload; //list of plugins whose files has to be uploaded
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

		xmlRelativePath = PluginManager.XML_COMPRESSED_LOCK;
		txtRelativePath = PluginManager.TXT_FILENAME;
		xmlSavePath = prefix(xmlRelativePath);
		txtSavePath = prefix(txtRelativePath);
	}

	public synchronized void generateNewPluginRecords() throws IOException {
		//Checking list for Fiji plugins - Either new versions or changes to existing ones
		filesToUpload = new ArrayList<SourceFile>();
		newPluginRecords = xmlFileReader.getAllPluginRecords();
		Iterator<String> filenames = newPluginRecords.keySet().iterator();
		while (filenames.hasNext()) {
			String filename = filenames.next();
			PluginObject pluginToUpload = ((PluginCollection)changesList).getPlugin(filename);
			if (pluginToUpload != null) {
				PluginCollection versions = (PluginCollection)newPluginRecords.get(filename);
				PluginObject latest = versions.getLatestPlugin();
				//if either an existing version, or timestamp is older than recorded version
				if (latest.getmd5Sum().equals(pluginToUpload.getmd5Sum()) ||
						latest.getTimestamp().compareTo(pluginToUpload.getTimestamp()) >= 0) {
					//Just update details
					latest.setDescription(pluginToUpload.getDescription());
				} else {
					//Newer version which does not exist in records yet, thus requires upload
					pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(
							pluginToUpload.getFilename()));
					filesToUpload.add(new UpdateSource(prefix(pluginToUpload.getFilename()),
							pluginToUpload, "C0444"));
					//Add to existing records
					versions.add(pluginToUpload);
					
				}
			}
		}
		//Checking list for non-Fiji plugins to add to new records
		for (PluginObject pluginToUpload : changesList) {
			String name = pluginToUpload.getFilename();
			List<PluginObject> pluginVersions = newPluginRecords.get(name);
			if (pluginVersions == null) { //non-Fiji plugin doesn't exist in records yet
				pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(name));
				filesToUpload.add(new UpdateSource(prefix(pluginToUpload.getFilename()),
						pluginToUpload, "C0444"));
				//therefore add it as a Fiji Plugin
				PluginCollection newPluginRecord = new PluginCollection();
				newPluginRecord.add(pluginToUpload);
				newPluginRecords.put(name, newPluginRecord);
			}
		}
	}

	public synchronized void uploadFilesToServer(UploadListener uploadListener) throws Exception  {
		fileUploader = new FileUploader();
		fileUploader.addListener(uploadListener);

		generateAndValidateXML();
		saveXMLFile();
		saveTextFile();
		//_Lock_ file, writable for none but current uploader
		SourceFile xmlSource = new UpdateSource(xmlSavePath, xmlRelativePath, "C0644");
		//Text file for old Fiji Updater, writable for all uploaders
		SourceFile txtSource = new UpdateSource(txtSavePath, txtRelativePath, "C0664");
		fileUploader.beganUpload(xmlSource, filesToUpload, txtSource);
		convertUploadStatusesToModified();
	}

	private void saveXMLFile() throws IOException { //assumed validation is done
		FileUtility fileUtility = new FileUtility();

		//Compress and save using given path
		FileOutputStream xmlOutputStream = new FileOutputStream(xmlSavePath);
		fileUtility.compressAndSave(xmlWriter.toByteArray(),
				xmlOutputStream);
		xmlOutputStream.close();
	}

	//pluginRecords consist of key of Plugin names, each maps to lists of different versions
	private void saveTextFile() throws FileNotFoundException {
		PrintStream txtPrintStream = new PrintStream(txtSavePath); //Writing to current.txt

		//start writing
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String filename = pluginNamelist.next();
			PluginCollection versions = (PluginCollection)newPluginRecords.get(filename);
			PluginObject latestPlugin = versions.getLatestPlugin();
			txtPrintStream.println(latestPlugin.getFilename() + " " +
				latestPlugin.getTimestamp() + " " + latestPlugin.getmd5Sum());
		}

		txtPrintStream.close();
	}

	//pluginRecords consist of key of Plugin names, each maps to lists of different versions
	private void generateAndValidateXML() throws SAXException,
	TransformerConfigurationException, IOException, ParserConfigurationException {
		//Prepare XML writing for later purposes of validation
		xmlWriter = new ByteArrayOutputStream();
		streamResult = new StreamResult(xmlWriter);
		tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

		handler = tf.newTransformerHandler();
		Transformer serializer = handler.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PluginManager.READ_DIRECTORY +
				"/" + PluginManager.DTD_FILENAME); //Relative path where it would be read
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");
		serializer.setOutputProperty(XALAN_INDENT_AMOUNT, "4");
		handler.setResult(streamResult);

		//Start writing to memory
		handler.startDocument();
		AttributesImpl attrib = new AttributesImpl();
		handler.startElement("", "", "pluginRecords", attrib);
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String filenameAttribute = pluginNamelist.next();

			//latest version have the tag "version", others given the tag "previous-version"
			PluginCollection versions = (PluginCollection)newPluginRecords.get(filenameAttribute);
			PluginObject latest = versions.getLatestPlugin();
			PluginCollection otherVersions = new PluginCollection();
			for (PluginObject version : versions)
				if (version != latest)
					otherVersions.add(version);

			attrib.clear();
			attrib.addAttribute("", "", "filename", "CDATA", filenameAttribute);
			handler.startElement("", "", "plugin", attrib);
				//tag "version" for the latest version
				attrib.clear();
				handler.startElement("", "", "version", attrib);
				writeSimpleTag("checksum", attrib, latest.getmd5Sum());
				writeSimpleTag("timestamp", attrib, latest.getTimestamp());
				String description = (latest.getDescription() == null ? "" : latest.getDescription());
				writeSimpleTag("description", attrib, description);
				String strFilesize = "" + latest.getFilesize();
				writeSimpleTag("filesize", attrib, strFilesize);
				if (latest.getDependencies() != null && latest.getDependencies().size() > 0) {
					List<Dependency> dependencies = latest.getDependencies();
					for (Dependency dependency : dependencies) {
						attrib.clear();
						handler.startElement("", "", "dependency", attrib);
						writeSimpleTag("filename", attrib, dependency.getFilename());
						writeSimpleTag("date", attrib, dependency.getTimestamp());
						writeSimpleTag("relation", attrib, "at-least");
						handler.endElement("", "", "dependency");
					}
				}
				handler.endElement("", "", "version");

				//As for the rest of the plugin's history record...
				for (PluginObject version : otherVersions) {
					//tag "previous-version"
					attrib.clear();
					attrib.addAttribute("", "", "timestamp", "CDATA", version.getTimestamp());
					attrib.addAttribute("", "", "checksum", "CDATA", version.getmd5Sum());
					handler.startElement("", "", "previous-version", attrib);
					handler.endElement("", "", "previous-version");
				}
			handler.endElement("", "", "plugin");
		}
		handler.endElement("", "", "pluginRecords");
		handler.endDocument();
		System.out.println("XML contents written to memory, checking for validation");

		//Validate XML contents
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();

		XMLReader xr = parser.getXMLReader();
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(new InputSource(new ByteArrayInputStream(xmlWriter.toByteArray())));
		System.out.println("XML contents validated");
	}

	private void writeSimpleTag(String tagName, AttributesImpl attrib, String value)
	throws SAXException {
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
