package fiji.pluginManager.logic;

import fiji.pluginManager.userInterface.MainUserInterface;
import ij.IJ;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/*
 * Start up class of Plugin Manager Application:
 * Facade, Business logic, and overall-in-charge of providing the main user interface the
 * required list of PluginObjects that interface will use for display.
 */
public class PluginManager implements PlugIn, Observer {
	//TODO: strange, why not the original "/var/www/update/"?
	//public static final String defaultServerPath = "var/www/update/"; //... obselete?
	public static final String MAIN_URL = "http://pacific.mpi-cbg.de/update/";
	public static final String TXT_FILENAME = "current.txt";
	public static final String XML_COMPRESSED_FILENAME = "db.xml.gz";
	public static final String XML_FILENAME = "db.xml";
	public static final String DTD_FILENAME = "plugins.dtd";
	public static final String READ_DIRECTORY = "plugininfo";
	public static final String WRITE_DIRECTORY = "var/www/update/"; //write to local, then uploaded
	public static final String UPDATE_DIRECTORY = "update";

	//PluginObjects for output at User Interface
	public List<PluginObject> pluginCollection;
	public List<PluginObject> readOnlyList;

	//Used for generating Plugin information
	private XMLFileDownloader xmlFileDownloader;
	private PluginListBuilder pluginListBuilder;
	public XMLFileReader xmlFileReader;

	public void run(String arg) {
		try {
			//Downloads files, convert info into PluginObjects useful for interface usage
			xmlFileDownloader = new XMLFileDownloader();
			xmlFileDownloader.register(this);
			xmlFileDownloader.startDownload();

			//Gets the PluginObject information
			pluginCollection = pluginListBuilder.pluginCollection;
			readOnlyList = pluginListBuilder.readOnlyList;

			MainUserInterface mainUserInterface = new MainUserInterface(this);
			mainUserInterface.setVisible(true);
		} catch (Error e) {
			//Interface side: This should handle presentation side of exceptions
			IJ.showMessage("Error", "Failed to load Plugin Manager:\n" + e.getLocalizedMessage());
		}
	}

	//Show progress of Plugin Manager startup at IJ bar
	public void refreshData(Observable subject) {
		try {
			if (subject == xmlFileDownloader) {
				IJ.showStatus("Downloading " + xmlFileDownloader.getTaskname() + "...");
				IJ.showProgress(xmlFileDownloader.getCurrentlyLoaded(),
						xmlFileDownloader.getTotalToLoad());
				if (xmlFileDownloader.allTasksComplete()) {
					//After downloading information successfully, read it
					xmlFileReader = new XMLFileReader(PluginManager.READ_DIRECTORY +
							File.separator + PluginManager.XML_FILENAME);
					//Build a list from the information
					pluginListBuilder = new PluginListBuilder(xmlFileReader);
					pluginListBuilder.register(this);
					pluginListBuilder.buildFullPluginList();
				}
			} else if (subject == pluginListBuilder) {
				IJ.showStatus("Downloading " + pluginListBuilder.getTaskname() + "...");
				IJ.showProgress(pluginListBuilder.getCurrentlyLoaded(),
						pluginListBuilder.getTotalToLoad());
				if (pluginListBuilder.allTasksComplete()) {
					IJ.showStatus("");
				}
			}
		} catch (ParserConfigurationException e1) {
			throw new Error(e1.getLocalizedMessage());
		} catch (IOException e2) {
			throw new Error(e2.getLocalizedMessage());
		} catch (SAXException e3) {
			throw new Error(e3.getLocalizedMessage());
		}
	}
}