package fiji.pluginManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
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

public class UpdatesWriter extends PluginDataObservable {
	private String xmlSavepath;
	private String txtSavepath;
	private PrintStream xmlPrintStream;
	private PrintStream txtPrintStream;
	private StreamResult streamResult;
	private SAXTransformerFactory tf;
	private TransformerHandler handler;

	//accessible information after uploading tasks are done
	public List<PluginObject> successList;
	public List<PluginObject> failList;

	public UpdatesWriter(Observer observer) throws IOException, TransformerConfigurationException {
		super(observer, true); //For purposes of uploading to server
		xmlSavepath = PluginManager.defaultServerPath + PluginManager.XML_FILENAME;
		txtSavepath = PluginManager.defaultServerPath + PluginManager.TXT_FILENAME;

		txtPrintStream = new PrintStream(txtSavepath); //current.txt
		xmlPrintStream = new PrintStream(xmlSavepath); //pluginRecords.xml
		streamResult = new StreamResult(xmlPrintStream);
		tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

		handler = tf.newTransformerHandler();
		Transformer serializer = handler.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
		serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PluginManager.XML_DIRECTORY + "/" + PluginManager.DTD_FILENAME);
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");
		handler.setResult(streamResult);
		
		successList = new PluginCollection();
		failList = new PluginCollection();
	}

	public void uploadFilesToServer(Map<String, List<PluginObject>> pluginRecords, List<PluginObject> filesUploadList) throws SAXException {
		writePlugins(filesUploadList);
		writeXMLFile(pluginRecords);
		xmlPrintStream.close();
		System.out.println("XML file written to server");
		writeTxtFile(pluginRecords);
		txtPrintStream.close();
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
	}

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
	}

	private void writeSimpleTag(String tagName, AttributesImpl attrib, String value) throws SAXException {
		attrib.clear();
		handler.startElement("", "", tagName, attrib);
		handler.characters(value.toCharArray(), 0, value.length());
		handler.endElement("", "", tagName);
	}
}
