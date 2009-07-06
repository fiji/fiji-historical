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
 * 1st Step: At constructor, prepare print streams for writing XML and text files.
 * 2nd Step: Upload plugin file(s) to server
 * 3rd Step: Write XML file using pluginRecords, save to server
 * 4th Step: Write text file using pluginRecords, save to server
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
	public List<PluginObject> successList;
	public List<PluginObject> failList;

	public Updater() throws IOException, TransformerConfigurationException {
		super(true); //For purposes of uploading to server
		xmlSavepath = PluginManager.defaultServerPath + PluginManager.XML_FILENAME;
		txtSavepath = PluginManager.defaultServerPath + PluginManager.TXT_FILENAME;

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

		successList = new PluginCollection();
		failList = new PluginCollection();
	}

	public void uploadFilesToServer(Map<String, List<PluginObject>> pluginRecords, List<PluginObject> filesUploadList) throws SAXException {
		writePlugins(filesUploadList);
		writeXMLFile(pluginRecords);
		System.out.println("XML file written to server");
		writeTxtFile(pluginRecords);
		System.out.println("Text file written to server");
		setStatusComplete(); //indicate to observer there's no more tasks
	}

	private void writePlugins(List<PluginObject> filesUploadList) {
		currentlyLoaded = 0;
		totalToLoad = filesUploadList.size();
		String remotePrefix = new File(xmlSavepath).getParent();
		for (PluginObject plugin : filesUploadList) {
			taskname = plugin.getFilename();
			String sourcePath = prefix(taskname);
			String targetPath = remotePrefix + File.separator + taskname + "-" + plugin.getTimestamp();
			try {
				copyFile(sourcePath, targetPath);
				successList.add(plugin);
				changeStatus(taskname, ++currentlyLoaded, totalToLoad);
			} catch (IOException e) {
				failList.add(plugin);
			}
		}
	}

	//pluginRecords consist of key of Plugin names, each maps to lists of different versions
	private void writeTxtFile(Map<String, List<PluginObject>> pluginRecords) throws SAXException {
		changeStatus(PluginManager.TXT_FILENAME, 0, 1);

		//start writing
		Iterator<String> pluginNamelist = pluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String filename = pluginNamelist.next();
			List<PluginObject> versionList = pluginRecords.get(filename);
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
	private void writeXMLFile(Map<String, List<PluginObject>> pluginRecords) throws SAXException {
		changeStatus(PluginManager.XML_FILENAME, 0, 1);

		//Start writing
		handler.startDocument();
		AttributesImpl attrib = new AttributesImpl();

		handler.startElement("", "", "pluginRecords", attrib);
		Iterator<String> pluginNamelist = pluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String filenameAttribute = pluginNamelist.next();
			attrib.clear();
			attrib.addAttribute("", "", "filename", "CDATA", filenameAttribute);
			handler.startElement("", "", "plugin", attrib);
			List<PluginObject> versionList = pluginRecords.get(filenameAttribute);
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
}
