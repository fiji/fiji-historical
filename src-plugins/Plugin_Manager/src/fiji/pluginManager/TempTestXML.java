package fiji.pluginManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class TempTestXML {

	private static List<String> getDataFromQueryResult(Object queryResult) {
		NodeList nodes = (NodeList) queryResult;
		List<String> myList = new ArrayList<String>();
		for (int i = 0; i < nodes.getLength(); i++) {
			myList.add(nodes.item(i).getNodeValue());
		}
		return myList;
	}

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

		//Get the list of plugins' names
		expr = xpath.compile("//plugin/attribute::filename");
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		List<String> filenameList = getDataFromQueryResult(result);

		//Then get the plugins' individual information
		for (String filename : filenameList) {
			System.out.println("------------------------------------------------------------");
			System.out.println("Plugin name: " + filename);

			//Get the list of versions (timestamps) of iterated plugin
			String pluginQuery = "//plugin[@filename='" + filename + "']/";
			String timestampQuery = pluginQuery + "version/timestamp/text()";
			
			expr = xpath.compile(timestampQuery);
			result = expr.evaluate(doc, XPathConstants.NODESET);
			List<String> timestampList = getDataFromQueryResult(result);
			//... TODO: Might want to get input from existing plugins and thus get to
			//    loop through only corresponding along with new timestamps...

			for (String timestamp : timestampList) {
				System.out.println("-------------------------");

				//retrieve the information of an iterated version of an iterated plugin
				String pluginVersionQuery = pluginQuery + "version[timestamp='" + timestamp + "']/";
				String checksumQuery = pluginVersionQuery + "checksum/text()";
				String descriptionQuery = pluginVersionQuery + "description/text()";
				String filesizeQuery = pluginVersionQuery + "filesize/text()";
				expr = xpath.compile(checksumQuery + " | " +
						descriptionQuery + " | " +
						filesizeQuery);
				result = expr.evaluate(doc, XPathConstants.NODESET);
				List<String> arrayResults = getDataFromQueryResult(result);
				String strChecksum = arrayResults.get(0);
				String strDescription = arrayResults.get(1);
				int filesize = Integer.parseInt(arrayResults.get(2));

				System.out.println("Version (Timestamp): " + timestamp + "\nChecksum: " + strChecksum + "\nDescription: " + strDescription + "\nFilesize: " + filesize);

				//retrieve the dependencies of an iterated version of an iterated plugin
				String dependencyQuery = pluginVersionQuery + "dependency/";
				String dependencyFilenameQuery = dependencyQuery + "filename/text()";
				String dependencyDateQuery = dependencyQuery + "date/text()";
				String dependencyRelationQuery = dependencyQuery + "relation/text()";
				expr = xpath.compile(dependencyFilenameQuery + " | " +
						dependencyDateQuery + " | " +
						dependencyRelationQuery);
				result = expr.evaluate(doc, XPathConstants.NODESET);
				List<String> arrayDependencies = getDataFromQueryResult(result);
				List<Dependency> dependencyList = new ArrayList<Dependency>();
				//compiles a list of Dependency objects for each version
				for (int i = 0; i < arrayDependencies.size(); i += 3) {
					String dependencyFilename = arrayDependencies.get(i);
					String dependencyTimestamp = arrayDependencies.get(i+1);
					String dependencyRelation = arrayDependencies.get(i+2);
					Dependency iterDependency = new Dependency(dependencyFilename, dependencyTimestamp);
					dependencyList.add(iterDependency);
				}

				System.out.println("Dependencies:");
				if (dependencyList.size() > 0) {
					for (Dependency dependency : dependencyList) {
						System.out.println(dependency.getFilename() + "; " + dependency.getTimestamp());
					}
				} else {
					System.out.println("None.");
				}
				System.out.println("-------------------------");
			}

			System.out.println("------------------------------------------------------------");
		}
	}
}
