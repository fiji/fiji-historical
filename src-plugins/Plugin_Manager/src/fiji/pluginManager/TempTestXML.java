package fiji.pluginManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class TempTestXML {
	public static void main(String[] args) 
		throws ParserConfigurationException, SAXException, 
			IOException, XPathExpressionException {

		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document doc = builder.parse("pluginRecords.xml");

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression expr;
		//XPathExpression expr = xpath.compile("/inventory/book[author='Neal Stephenson']/title/text()");
		//XPathExpression expr = xpath.compile("//book[@year>='2000']/title/text() | //author/text()");
		//XPathExpression expr = xpath.compile("//book[author='Neal Stephenson']/title/text()");
		//XPathExpression expr = xpath.compile("//book/*/text()");
		//XPathExpression expr = xpath.compile("/inventory/child::*/child::price/text()");
		//XPathExpression expr = xpath.compile("/inventory/child::*/attribute::year");

		//Note right now you only focus on getting out the output, not how the information
		//is to be integrated into a pluginObject.

		//First get the list of plugins' names
		List<String> filenameList = new ArrayList<String>();
		expr = xpath.compile("//plugin/attribute::filename");
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			filenameList.add(nodes.item(i).getNodeValue());
		}

		//Then get the plugins' individual information
		for (String filename : filenameList) {
			System.out.println("--------------------");
			System.out.println("Plugin name: " + filename);

			String versionQuery = "//plugin[@filename='" + filename + "']/version/";
			String checksumQuery = versionQuery + "checksum/text()";
			String timestampQuery = versionQuery + "timestamp/text()";
			String descriptionQuery = versionQuery + "description/text()";
			String filesizeQuery = versionQuery + "filesize/text()";

			expr = xpath.compile(checksumQuery + " | " + timestampQuery + " | " +
					descriptionQuery + " | " + filesizeQuery);
			result = expr.evaluate(doc, XPathConstants.NODESET);
			nodes = (NodeList)result;
			String versionDetails = "";
			for (int i = 0; i < nodes.getLength(); i++) {
				int index = (i+1) % 4;
				if (index == 1) {
					String checksum = nodes.item(i).getNodeValue();
					versionDetails += "This is " + filename + " version:\n\tchecksum=" + checksum + ";\n\t";
				} else if (index == 2) {
					String timestamp = nodes.item(i).getNodeValue();
					versionDetails += "timestamp=" + timestamp + ";\n\t";
				} else if (index == 3) {
					String description = nodes.item(i).getNodeValue();
					versionDetails += "description=" + description + ";\n\t";
				} else if (index == 0) {
					int filesize = Integer.parseInt(nodes.item(i).getNodeValue());
					versionDetails += "filesize=" + filesize + ";";
					if (i < nodes.getLength() -1) {
						versionDetails += "\n";
					}
				}
			}
			System.out.println(versionDetails);

			System.out.println("Plugin Dependencies List (Right now only list dependencies of ALL versions, doesn't really sort out...)");
			String dependencyQuery = versionQuery + "dependency/";
			String dependencyFilenameQuery = dependencyQuery + "filename/text()";
			String dependencyDateQuery = dependencyQuery + "date/text()";
			String dependencyRelationQuery = dependencyQuery + "relation/text()";
			expr = xpath.compile(dependencyFilenameQuery + " | " + dependencyDateQuery + " | " +
					dependencyRelationQuery);
			result = expr.evaluate(doc, XPathConstants.NODESET);
			nodes = (NodeList)result;
			String dependencyDetails = "";
			for (int i = 0; i < nodes.getLength(); i++) {
				int index = (i+1) % 3;
				if (index == 1) {
					String dependencyName = nodes.item(i).getNodeValue();
					dependencyDetails += "This is " + filename + " dependency " + ((i/3)+1) + " :\n\tfilename=" + dependencyName + ";\n\t";
				} else if (index == 2) {
					String date = nodes.item(i).getNodeValue();
					dependencyDetails += "date=" + date + ";\n\t";
				} else if (index == 0) {
					String relation = nodes.item(i).getNodeValue();
					if (relation.equals("at-least")) {
						dependencyDetails += "relation=" + relation + ";";
					}
					if (i < nodes.getLength() -1) {
						dependencyDetails += "\n";
					}
				}
			}
			System.out.println(dependencyDetails);

			System.out.println("--------------------");
		}
	}
}
