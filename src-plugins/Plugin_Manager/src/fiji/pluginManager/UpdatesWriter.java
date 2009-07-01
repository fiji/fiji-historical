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

public class UpdatesWriter extends PluginData {
	PrintWriter xmlPrintWriter;
	PrintStream txtPrintStream;
	StreamResult streamResult;
	SAXTransformerFactory tf;
	TransformerHandler handler;

	public UpdatesWriter() throws IOException, TransformerConfigurationException {
		String xmlSavepath = PluginManager.defaultServerPath + PluginManager.XML_FILENAME;
		String txtSavepath = PluginManager.defaultServerPath + PluginManager.TXT_FILENAME;

		//XML file writer (pluginRecords.xml)
		xmlPrintWriter = new PrintWriter(new FileWriter(xmlSavepath));
		streamResult = new StreamResult(xmlPrintWriter);
		tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

		handler = tf.newTransformerHandler();
		Transformer serializer = handler.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
		serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PluginManager.XML_DIRECTORY + "/" + PluginManager.DTD_FILENAME);
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");
		handler.setResult(streamResult);

		//Text file writer (current.txt)
		txtPrintStream = new PrintStream(txtSavepath);
	}

	public void writeFilesForUploading(Map<String, List<PluginObject>> pluginRecords) throws SAXException {
		writeXMLFile(pluginRecords);
		xmlPrintWriter.close();
		System.out.println("XML file written to server");
		writeTxtFile(pluginRecords);
		txtPrintStream.close();
		System.out.println("Text file written to server");
	}

	private void writeTxtFile(Map<String, List<PluginObject>> pluginRecords) throws SAXException {
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
	}

	private void writeXMLFile(Map<String, List<PluginObject>> pluginRecords) throws SAXException {
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
	}

	private void writeSimpleTag(String tagName, AttributesImpl attrib, String value) throws SAXException {
		attrib.clear();
		handler.startElement("", "", tagName, attrib);
		handler.characters(value.toCharArray(), 0, value.length());
		handler.endElement("", "", tagName);
	}
}
