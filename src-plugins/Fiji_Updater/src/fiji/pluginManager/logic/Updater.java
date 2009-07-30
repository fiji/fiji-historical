package fiji.pluginManager.logic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.jcraft.jsch.JSchException;

import fiji.pluginManager.logic.FileUploader.SourceFile;
import fiji.pluginManager.logic.FileUploader.UploadListener;
import fiji.pluginManager.utilities.Compressor;
import fiji.pluginManager.utilities.PluginData;

/*
 * This class is responsible for writing updates to server, upon given the updated
 * plugin records (Map of plugins to all versions).
 * 
 * 1st Step: Generates the updated records (newPluginRecords & filesToUpload)
 * 2nd Step: Login details _must_ be authenticated before below steps can proceed
 * 3rd Step: Write and validate XML file, and write current.txt contents too
 * 4th Step: Upload XML, text and plugin file(s) to server
 * 
 * Note: Plugins are uploaded differently
 * - Non-Fiji plugins & new versions of Fiji Plugins will have files AND details uploaded
 * - Uninstalled & up-to-date plugins will ONLY have their details uploaded (i.e.: XML file)
 * - Updater's FileUploader does NOT upload the DTD file. It assumes the DTD file is up to date.
 */
public class Updater extends PluginData {
	private FileUploader fileUploader;
	private String[] relativePaths = { //@ Fiji root
			PluginManager.XML_LOCK,
			PluginManager.TXT_FILENAME
	};
	private String[] savePaths; //paths for getting the required files to upload
	private String backupXMLPath;
	private long xmlLastModified; //Use for lock conflict check
	private TransformerHandler handler; //tool for writing of XML contents

	//accessible information after uploading tasks are done
	private XMLFileReader xmlFileReader;
	public Map<String, List<PluginObject>> newPluginRecords;
	private ArrayList<SourceFile> filesToUpload; //list of plugin files to be uploaded
	public List<PluginObject> changesList;
	private DependencyAnalyzer dependencyAnalyzer;

	public Updater(PluginManager pluginManager) {
		PluginCollection pluginCollection = (PluginCollection)pluginManager.pluginCollection;
		changesList = pluginCollection.getList(PluginCollection.FILTER_ACTIONS_UPLOAD);
		((PluginCollection)changesList).resetChangeStatuses();
		dependencyAnalyzer = new DependencyAnalyzer(pluginCollection);
		xmlFileReader = pluginManager.xmlFileReader;

		xmlLastModified = pluginManager.getXMLLastModified();
		savePaths = new String[relativePaths.length];
		for (int i = 0; i < savePaths.length; i++)
			savePaths[i] = prefix(relativePaths[i]);
		backupXMLPath = prefix(PluginManager.XML_BACKUP);
	}

	public synchronized boolean setLogin(String username, String password) {
		try {
			fileUploader = new FileUploader(username, password);
			System.out.println("User " + username + " logged in successfully.");
			return true;
		} catch (JSchException e) {
			System.out.println("Failed to login. Username or password incorrect.");
			return false;
		}
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
					latest.setPluginDetails(pluginToUpload.getPluginDetails());
				} else {
					//Newer version which does not exist in records yet, thus requires upload
					pluginToUpload.setDependency(dependencyAnalyzer.getDependencyListFromFile(
							pluginToUpload.getFilename()));
					filesToUpload.add(new UpdateSource(prefix(pluginToUpload.getFilename()),
							pluginToUpload, "C0644"));
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
						pluginToUpload, "C0644"));
				//therefore add it as a Fiji Plugin
				PluginCollection newPluginRecord = new PluginCollection();
				newPluginRecord.add(pluginToUpload);
				newPluginRecords.put(name, newPluginRecord);
			}
		}
	}

	public synchronized void uploadFilesToServer(UploadListener uploadListener) throws Exception  {
		System.out.println("********** Upload Process begins **********");
		fileUploader.addListener(uploadListener);

		generateAndValidateXML();
		saveXMLFile();
		saveTextFile();
		List<SourceFile> information = new ArrayList<SourceFile>();
		//_Lock_ file, writable for none but current uploader
		information.add(0, new UpdateSource(savePaths[0], relativePaths[0], "C0644"));
		//Text file for old Fiji Updater, writable for all uploaders
		information.add(1, new UpdateSource(savePaths[1], relativePaths[1], "C0664"));
		fileUploader.beganUpload(xmlLastModified, information, filesToUpload);

		//No errors thrown, implies successful upload, so just remove temporary files
		new File(backupXMLPath).delete();
		for (String path : savePaths)
			new File(path).delete();

		System.out.println("********** Upload Process ended **********");
	}

	//assumed validation is done
	private void saveXMLFile() throws IOException {
		//Compress and save using given path
		FileOutputStream xmlOutputStream = new FileOutputStream(savePaths[0]); //to compress to
		FileInputStream xmlInputStream = new FileInputStream(backupXMLPath); //to get data from
		Compressor.compressAndSave(Compressor.readStream(xmlInputStream), xmlOutputStream);
		xmlOutputStream.close();
		xmlInputStream.close();
	}

	//pluginRecords consist of key of Plugin names, each maps to lists of different versions
	private void saveTextFile() throws FileNotFoundException {
		PrintStream txtPrintStream = new PrintStream(savePaths[1]); //Writing to current.txt

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
		//Prepare XML writing
		FileOutputStream xmlOutputStream = new FileOutputStream(backupXMLPath);
		XMLFileHandler xmlHandler = new XMLFileHandler(xmlOutputStream);
		handler = xmlHandler.getXMLHandler();

		//Start actual writing
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
				String description = (latest.getPluginDetails().getDescription() == null ?
						"" : latest.getPluginDetails().getDescription());
				writeSimpleTag("description", attrib, description);
				String strFilesize = "" + latest.getFilesize();
				writeSimpleTag("filesize", attrib, strFilesize);

				//Write dependencies if any
				if (latest.getDependencies() != null && latest.getDependencies().size() > 0) {
					List<Dependency> dependencies = latest.getDependencies();
					for (Dependency dependency : dependencies) {
						attrib.clear();
						handler.startElement("", "", "dependency", attrib);
						writeSimpleTag("filename", attrib, dependency.getFilename());
						writeSimpleTag("date", attrib, dependency.getTimestamp());
						if (dependency.getRelation() != null)
							writeSimpleTag("relation", attrib, dependency.getRelation());
						handler.endElement("", "", "dependency");
					}
				}
				//write <link> and <author> tags if any
				writeMultipleSimpleTags("link", attrib, latest.getPluginDetails().getLinks());
				writeMultipleSimpleTags("author", attrib, latest.getPluginDetails().getAuthors());
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
		xmlOutputStream.close();
		System.out.println("XML contents written, checking for validation");

		//Validate XML contents (Reading the saved XML file)
		FileInputStream xmlInputStream = new FileInputStream(backupXMLPath);
		xmlHandler.validateXMLContents(xmlInputStream);
		xmlInputStream.close();
		System.out.println("XML contents validated");
	}

	private void writeMultipleSimpleTags(String tagName, AttributesImpl attrib, List<String> values)
	throws SAXException {
		if (values != null && values.size() > 0)
			for (String value : values)
				writeSimpleTag(tagName, attrib, value);
	}

	private void writeSimpleTag(String tagName, AttributesImpl attrib, String value)
	throws SAXException {
		attrib.clear();
		handler.startElement("", "", tagName, attrib);
		handler.characters(value.toCharArray(), 0, value.length());
		handler.endElement("", "", tagName);
	}
}