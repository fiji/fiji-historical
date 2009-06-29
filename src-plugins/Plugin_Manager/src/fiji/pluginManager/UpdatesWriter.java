package fiji.pluginManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
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
	StreamResult streamResult;
	SAXTransformerFactory tf;
	TransformerHandler hd;

	public UpdatesWriter() throws IOException, TransformerConfigurationException {
		xmlPrintWriter = new PrintWriter(new FileWriter(getSaveToLocation(PluginManager.XML_DIRECTORY, "pluginRecords.xml")));
		streamResult = new StreamResult(xmlPrintWriter);
		tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

		hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
		serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PluginManager.XML_DIRECTORY + File.separator + PluginManager.DTD_FILENAME);
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");
		hd.setResult(streamResult);
	}

	public void writeXMLFile(Map<String, List<PluginObject>> pluginRecords) throws SAXException {
		hd.startDocument();
		AttributesImpl attrib = new AttributesImpl();
		
		hd.startElement("", "", "pluginRecords", attrib);
		Iterator<String> pluginNamelist = pluginRecords.keySet().iterator();
		while (pluginNamelist.hasNext()) {
			String filenameAttribute = pluginNamelist.next();
			attrib.clear();
			attrib.addAttribute("", "", "filename", "CDATA", filenameAttribute);
			hd.startElement("", "", "plugin", attrib);
			System.out.println("attribute: "+filenameAttribute);
			List<PluginObject> versionList = pluginRecords.get(filenameAttribute);
			for (PluginObject plugin : versionList) {
				attrib.clear();
				hd.startElement("", "", "version", attrib);
					attrib.clear();
					hd.startElement("", "", "checksum", attrib);
					hd.characters(plugin.getmd5Sum().toCharArray(), 0, plugin.getmd5Sum().length());
					hd.endElement("", "", "checksum");
					System.out.println("for " + filenameAttribute + ", checksum: " + plugin.getmd5Sum());
					attrib.clear();
					hd.startElement("", "", "timestamp", attrib);
					hd.characters(plugin.getTimestamp().toCharArray(), 0, plugin.getTimestamp().length());
					hd.endElement("", "", "timestamp");
					attrib.clear();
					hd.startElement("", "", "description", attrib);
					String description = (plugin.getDescription() == null ? "" : plugin.getDescription());
					hd.characters(description.toCharArray(), 0, description.length());
					hd.endElement("", "", "description");
					attrib.clear();
					hd.startElement("", "", "filesize", attrib);
					String strFilesize = "" + plugin.getFilesize();
					hd.characters(strFilesize.toCharArray(), 0, strFilesize.length());
					hd.endElement("", "", "filesize");
					if (plugin.getDependencies() != null && plugin.getDependencies().size() > 0) {
						List<Dependency> dependencies = plugin.getDependencies();
						for (Dependency dependency : dependencies) {
							attrib.clear();
							hd.startElement("", "", "dependency", attrib);
								attrib.clear();
								hd.startElement("", "", "filename", attrib);
								hd.characters(dependency.getFilename().toCharArray(), 0, dependency.getFilename().length());
								hd.endElement("", "", "filename");
								attrib.clear();
								hd.startElement("", "", "date", attrib);
								hd.characters(dependency.getTimestamp().toCharArray(), 0, dependency.getTimestamp().length());
								hd.endElement("", "", "date");
								attrib.clear();
								hd.startElement("", "", "relation", attrib);
								//hd.characters(dependency.getRelation().toCharArray(), 0, dependency.getRelation().length());
								hd.characters("at-least".toCharArray(), 0, "at-least".length());
								hd.endElement("", "", "relation");
							hd.endElement("", "", "dependency");
						}
					}
				hd.endElement("", "", "version");
			}
			hd.endElement("", "", "plugin");
		}
		hd.endElement("", "", "pluginRecords");
		hd.endDocument();
	}

}
