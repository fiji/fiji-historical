package fiji.pluginManager.logic;

import fiji.pluginManager.userInterface.MainUserInterface;
import ij.IJ;
import ij.plugin.PlugIn;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/*
 * Start up class of Plugin Manager Application:
 * Facade, Business logic, and overall-in-charge of providing the main user interface the
 * required list of PluginObjects that interface will use for display.
 */
public class PluginManager implements PlugIn, Observer {
	//TODO
	public static final String MAIN_URL = "http://pacific.mpi-cbg.de/uploads/incoming/";
	//public static final String MAIN_URL = "http://pacific.mpi-cbg.de/update/";
	public static final String TXT_FILENAME = "current.txt";
	public static final String XML_COMPRESSED_LOCK = "db.xml.gz.lock";
	public static final String XML_COMPRESSED_FILENAME = "db.xml.gz";
	public static final String XML_FILENAME = "db.xml";
	public static final String DTD_FILENAME = "plugins.dtd";
	public static final String UPDATE_DIRECTORY = "update";

	//PluginObjects for output at User Interface
	public List<PluginObject> pluginCollection;
	public List<PluginObject> readOnlyList;

	//Used for generating Plugin information
	private XMLFileDownloader xmlFileDownloader;
	private PluginListBuilder pluginListBuilder;
	public XMLFileReader xmlFileReader;

	private long xmlLastModified; //Track when was file modified (Lock conflict purposes)
	boolean isDeveloper = true;

	public void run(String arg) {
		try {
			System.out.println("********** Plugin Manager Startup **********");
			//First download the required information, which starts the program running
			xmlFileDownloader = new XMLFileDownloader();
			xmlFileDownloader.register(this);
			System.out.println("Attempting to download required information.");
			xmlFileDownloader.startDownload();
		} catch (Error e) {
			//Interface side: This should handle presentation side of exceptions
			IJ.showMessage("Error", "Failed to load Plugin Manager:\n" + e.getLocalizedMessage());
		} catch (IOException e) {
			//Scenario if decompression fails
			if (JOptionPane.showConfirmDialog(null,
					"Plugin Manager has failed to decompress plugin information db.xml.gz." +
					"\nAn upgrade may be necessary. Do you want to use an older version to help download " +
					"the latest version of Plugin Manager?", "Using UpdateFiji", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				UpdateFiji updateFiji = new UpdateFiji();
				updateFiji.hasGUI = true;
				updateFiji.exec(UpdateFiji.defaultURL);
			}
		}
	}

	public long getXMLLastModified() {
		return xmlLastModified;
	}

	//Show progress of startup at IJ bar, directs what actions to take after task is complete.
	public void refreshData(Observable subject) {
		try {	
			if (subject == xmlFileDownloader) {
				IJ.showStatus("Downloading " + xmlFileDownloader.getTaskname() + "...");
				IJ.showProgress(xmlFileDownloader.getCurrentlyLoaded(),
						xmlFileDownloader.getTotalToLoad());
				//After required files are downloaded, read and retrieve them
				if (xmlFileDownloader.allTasksComplete()) {
					System.out.println("Download complete.");
					xmlLastModified = xmlFileDownloader.getXMLLastModified();
					xmlFileReader = new XMLFileReader(PluginManager.XML_FILENAME);

					//Start to build a list from the information
					pluginListBuilder = new PluginListBuilder(xmlFileReader);
					pluginListBuilder.register(this);
					pluginListBuilder.buildFullPluginList();
				}

			} else if (subject == pluginListBuilder) {
				IJ.showStatus("Loading " + pluginListBuilder.getTaskname() + "...");
				IJ.showProgress(pluginListBuilder.getCurrentlyLoaded(),
						pluginListBuilder.getTotalToLoad());

				//After plugin list is built successfully, retrieve it and show main interface
				if (pluginListBuilder.allTasksComplete()) {
					System.out.println("Finished building up plugin data.");
					IJ.showStatus("");
					pluginCollection = pluginListBuilder.pluginCollection;
					readOnlyList = pluginListBuilder.readOnlyList;
					MainUserInterface mainUserInterface = new MainUserInterface(this, isDeveloper);
					mainUserInterface.setVisible(true);
					System.out.println("********** Startup Ended **********");
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