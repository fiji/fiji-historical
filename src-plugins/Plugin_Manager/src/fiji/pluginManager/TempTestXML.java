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
			expr = xpath.compile("//plugin[@filename='" + filename + "']/description/text()");
			result = expr.evaluate(doc, XPathConstants.STRING);
			String strDescription = (String)result;
			System.out.println("--------------------");
			System.out.println("Plugin name: " + filename);
			System.out.println("Plugin description: " + strDescription);

			expr = xpath.compile("//plugin[@filename='" + filename + "']/version/checksum/text() |" +
					" //plugin[@filename='" + filename + "']/version/timestamp/text() |" +
					" //plugin[@filename='" + filename + "']/version/filesize/text()");
			result = expr.evaluate(doc, XPathConstants.NODESET);
			nodes = (NodeList)result;
			String versionDetails = "";
			for (int i = 0; i < nodes.getLength(); i++) {
				int index = (i+1) % 3;
				if (index == 1) {
					versionDetails += "This is " + filename + " version with sum=" + nodes.item(i).getNodeValue() + "; ";
				} else if (index == 2) {
					versionDetails += "timestamp=" + nodes.item(i).getNodeValue() + "; ";
				} else if (index == 0) {
					versionDetails += "filesize=" + nodes.item(i).getNodeValue() + ";";
					if (i < nodes.getLength() -1) {
						versionDetails += "\n";
					}
				}
			}
			System.out.println(versionDetails);
			System.out.println("--------------------");
		}
	}
}
