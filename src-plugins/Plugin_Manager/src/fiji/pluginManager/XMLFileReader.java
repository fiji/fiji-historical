package fiji.pluginManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

/*
 * XML File Reader, as name implies, reads an already downloaded XML file of containing all
 * existing records of Fiji plugins. Upon specific requests (call methods), it will retrieve
 * the associated information using an XPath expression.
 * 
 * XML document is parsed just once when constructor is called thus avoiding parsing each
 * time information is retrieved.
 */
public class XMLFileReader {
	private DocumentBuilderFactory domFactory;
	private DocumentBuilder builder;
	private Document doc;
	private XPathFactory factory;
	private XPath xpath;
	private XPathExpression expr;

	private List<String> filenameList;

	public XMLFileReader(String fileLocation) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
		domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true); // never forget this!
		builder = domFactory.newDocumentBuilder();
		doc = builder.parse(fileLocation);

		factory = XPathFactory.newInstance();
		xpath = factory.newXPath();

		//As a start, get list of filenames
		Object result = evaluateQueryAsList("//plugin/attribute::filename");
		filenameList = getListFromQueryResult(result);
	}

	private List<String> getListFromQueryResult(Object queryResult) {
		NodeList nodes = (NodeList) queryResult;
		List<String> myList = new ArrayList<String>();
		for (int i = 0; i < nodes.getLength(); i++) {
			myList.add(nodes.item(i).getNodeValue());
		}
		return myList;
	}

	private Object evaluateQueryAsList(String query) throws XPathExpressionException {
		expr = xpath.compile(query);
		return expr.evaluate(doc, XPathConstants.NODESET);
	}

	private Object evaluateQueryAsString(String query) throws XPathExpressionException {
		expr = xpath.compile(query);
		return expr.evaluate(doc, XPathConstants.STRING);
	}

	public boolean existsDigest(String outputFilename, String outputDigest) throws XPathExpressionException {
		if (!existsFilename(outputFilename)) {
			return false;
		} else {
			String pluginQuery = "//plugin[@filename='" + outputFilename + "']/";
			String digestsQuery = pluginQuery + "version/checksum[1]/text()";
			Object result = evaluateQueryAsList(digestsQuery);
			List<String> arrayResults = getListFromQueryResult(result);
			if (arrayResults.contains(outputDigest)) {
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean existsFilename(String outputFilename) {
		return (filenameList.contains(outputFilename));
	}

	public void getLatestDigestsAndDates(Map<String, String> latestDigests, Map<String, String> latestDates) throws XPathExpressionException {
		for (String filename : filenameList) {
			//Get the latest update details (timestamp and digest) of plugin
			String pluginQuery = "//plugin[@filename='" + filename + "']/";
			String latestVersionQuery = pluginQuery + "version[not(timestamp <= preceding-sibling::version/timestamp) and not(timestamp <=following-sibling::version/timestamp)]/";
			String timestampQuery = latestVersionQuery + "timestamp[1]/text()";
			String digestQuery = latestVersionQuery + "checksum[1]/text()";

			Object result = evaluateQueryAsList(digestQuery + " | " + timestampQuery);
			List<String> updates = getListFromQueryResult(result);

			latestDigests.put(filename, updates.get(0));
			latestDates.put(filename, updates.get(1));
		}
	}

	//Get description associated with specified version, assumed filename & timestamp are correct
	public String getDescriptionFrom(String filename, String timestamp) throws XPathExpressionException {
		String pluginQuery = "//plugin[@filename='" + filename + "']/";
		String versionQuery = pluginQuery + "version[timestamp='" + timestamp + "']/";
		String descriptionQuery = versionQuery + "description[1]/text()";
		return (String)evaluateQueryAsString(descriptionQuery);
	}

	//Get dependencies associated with specified version, assumed filename & timestamp are correct
	public List<Dependency> getDependenciesFrom(String filename, String timestamp) throws XPathExpressionException {
		String pluginQuery = "//plugin[@filename='" + filename + "']/";
		String versionQuery = pluginQuery + "version[timestamp='" + timestamp + "']/";
		String dependencyQuery = versionQuery + "dependency/";
		String dependencyFilenameQuery = dependencyQuery + "filename[1]/text()";
		String dependencyDateQuery = dependencyQuery + "date[1]/text()";
		String dependencyRelationQuery = dependencyQuery + "relation[1]/text()";

		Object result = evaluateQueryAsList(dependencyFilenameQuery + " | " +
				dependencyDateQuery + " | " +
				dependencyRelationQuery);
		List<String> arrayDependencies = getListFromQueryResult(result);

		//compiles a list of Dependency objects for each version
		List<Dependency> dependencyList = new ArrayList<Dependency>();
		for (int i = 0; i < arrayDependencies.size(); i += 3) {
			String dependencyFilename = arrayDependencies.get(i);
			String dependencyTimestamp = arrayDependencies.get(i+1);
			String dependencyRelation = arrayDependencies.get(i+2);
			if (dependencyRelation.equals("at-least")) {
				Dependency iterDependency = new Dependency(dependencyFilename, dependencyTimestamp);
				dependencyList.add(iterDependency);
			}
		}
		return dependencyList;
	}

	//Get timestamp associated with specified version, assumed filename & digest are correct
	public String getTimestamp(String outputFilename, String outputDigest) throws XPathExpressionException {
		String pluginQuery = "//plugin[@filename='" + outputFilename + "']/";
		String digestQuery = pluginQuery + "version[checksum='" + outputDigest + "']/";
		String timestampQuery = digestQuery + "timestamp[1]/text()";
		return (String)evaluateQueryAsString(timestampQuery);
	}

	//Unit Test (This scaffold is to be removed)
	public void printTest() throws XPathExpressionException {
		//XPathExpression expr = xpath.compile("/inventory/book[author='Neal Stephenson']/title/text()");
		//XPathExpression expr = xpath.compile("//book[@year>='2000']/title/text() | //author/text()");
		//XPathExpression expr = xpath.compile("//book[author='Neal Stephenson']/title/text()");
		//XPathExpression expr = xpath.compile("//book/*/text()");
		//XPathExpression expr = xpath.compile("/inventory/child::*/child::price/text()");
		//XPathExpression expr = xpath.compile("/inventory/child::*/attribute::year");

		//Get the list of plugins' names
		expr = xpath.compile("//plugin/attribute::filename");
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		List<String> filenameList = getListFromQueryResult(result);

		//Then get the plugins' individual information
		for (String filename : filenameList) {
			System.out.println("------------------------------------------------------------");
			System.out.println("Plugin name: " + filename);

			//Get the list of versions (timestamps) of iterated plugin
			String pluginQuery = "//plugin[@filename='" + filename + "']/";
			String timestampQuery = pluginQuery + "version[not(timestamp <= preceding-sibling::version/timestamp) and not(timestamp <=following-sibling::version/timestamp)]/timestamp[1]/text()";

			expr = xpath.compile(timestampQuery);
			result = expr.evaluate(doc, XPathConstants.NODESET);
			List<String> timestampList = getListFromQueryResult(result);
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
				List<String> arrayResults = getListFromQueryResult(result);
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
				List<String> arrayDependencies = getListFromQueryResult(result);
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

	//Unit Test (This scaffold is to be removed)
	public static void main(String args[]) {
		XMLFileReader xmlFileReader;
		try {
			xmlFileReader = new XMLFileReader("pluginRecords.xml");
		} catch (ParserConfigurationException e1) {
			throw new Error("Configuration error within XMLFileReader class: " + e1.getLocalizedMessage());
		} catch (IOException e2) {
			throw new Error("Failed to read given XML file.");
		} catch (SAXException e3) {
			throw new Error("SAXException: " + e3.getLocalizedMessage());
		} catch (XPathExpressionException e) {
			throw new Error("Please check if XPath expression is valid: " + e.getLocalizedMessage());
		}

		//reading/printing info
		try {
			xmlFileReader.printTest();
		} catch (XPathExpressionException e) {
			throw new Error("Please check if XPath expression is valid: " + e.getLocalizedMessage());
		}
	}

}
