package fiji.pluginManager.logic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import com.jcraft.jsch.JSchException;
import fiji.pluginManager.userInterface.Uploader;
import fiji.pluginManager.logic.FileUploader.SourceFile;

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
	public Map<String, PluginObject> newPluginRecords;
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

	public void generateNewPluginRecords() throws IOException {
		//Checking list for Fiji plugins - Either new versions or changes to existing ones
		filesToUpload = new ArrayList<SourceFile>();
		newPluginRecords = xmlFileReader.getLatestFijiPlugins();
		Iterator<String> filenames = newPluginRecords.keySet().iterator();
		while (filenames.hasNext()) {
			String filename = filenames.next();
			PluginObject plugin = newPluginRecords.get(filename);
			for (PluginObject pluginToUpload : changesList) {
				if (pluginToUpload.getFilename().equals(plugin.getFilename())) {
					pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(pluginToUpload.getFilename()));
					newPluginRecords.put(filename, pluginToUpload);
					//If it is not part of existing records, it has to be new upgrade
					if (!pluginToUpload.getmd5Sum().equals(plugin.getmd5Sum())) {
						filesToUpload.add(new UpdateSource(prefix(pluginToUpload.getFilename()),
								pluginToUpload, "C0444"));
					}
					break;
				}
			}
		}
		//Checking list for non-Fiji plugins to add to new records
		for (PluginObject pluginToUpload : changesList) {
			String name = pluginToUpload.getFilename();
			PluginObject plugin = newPluginRecords.get(name);
			if (plugin == null) { //non-Fiji plugin that doesn't exist in records yet
				//therefore add it as a new Fiji plugin
				pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(pluginToUpload.getFilename()));
				newPluginRecords.put(name, pluginToUpload);
				filesToUpload.add(new UpdateSource(prefix(pluginToUpload.getFilename()),
						pluginToUpload, "C0444"));
			}
		}
	}

	public void uploadFilesToServer(Uploader uploader) throws Exception  {
		fileUploader = new FileUploader();
		fileUploader.addListener(uploader);

		generateAndValidateXML();
		saveXMLFile();
		saveTextFile();
		//_LOCK_ file to write, writable for none but current uploader
		SourceFile xmlSource = new UpdateSource(xmlSavePath, xmlRelativePath, "C0644");
		//Text file for old Fiji Updater, writable for all uploaders
		SourceFile txtSource = new UpdateSource(txtSavePath, txtRelativePath, "C0664");
		fileUploader.beganUpload(xmlSource, filesToUpload, txtSource);
		convertUploadStatusesToModified();
	}

	private void saveXMLFile() throws IOException { //assumed validation is done
		CompressionUtility compressionUtility = new CompressionUtility();

		//Compress and save using given path
		FileOutputStream xmlOutputStream = new FileOutputStream(xmlSavePath);
		compressionUtility.compressAndSave(xmlWriter.toByteArray(),
				xmlOutputStream);
		xmlOutputStream.close();
	}

	//Write plugin files (JAR) to the server
	/*private void writePlugins() {
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
	}*/

	//pluginRecords consist of key of Plugin names, each maps to lists of different versions
	private void saveTextFile() throws FileNotFoundException {
		PrintStream txtPrintStream = new PrintStream(txtSavePath); //Writing to current.txt

		//start writing
		Iterator<String> pluginNamelist = newPluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String filename = pluginNamelist.next();
			PluginObject latestPlugin = newPluginRecords.get(filename);
			if (!latestPlugin.uploadFailed()) //do not add entry if indicated "failed upload"
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
			attrib.clear();
			attrib.addAttribute("", "", "filename", "CDATA", filenameAttribute);
			handler.startElement("", "", "plugin", attrib);
			PluginObject plugin = newPluginRecords.get(filenameAttribute);
			if (!plugin.uploadFailed()) { //do not add entry if indicated "failed upload"
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
