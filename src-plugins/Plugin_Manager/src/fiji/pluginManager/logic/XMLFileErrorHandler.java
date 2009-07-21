package fiji.pluginManager.logic;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLFileErrorHandler implements ErrorHandler {
	public void error(SAXParseException exception) throws SAXException {
		throw new Error(exception.getLocalizedMessage());
	}

	public void fatalError(SAXParseException exception) throws SAXException {
		throw new Error(exception.getLocalizedMessage());
	}

	public void warning(SAXParseException exception) throws SAXException {
		System.out.println("XML File Warning: " + exception.getLocalizedMessage());
	}
}
