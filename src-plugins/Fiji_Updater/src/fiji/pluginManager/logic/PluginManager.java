package fiji.pluginManager.logic;
import fiji.pluginManager.ui.MainUserInterface;
import fiji.pluginManager.utilities.UpdateFiji;
import ij.IJ;
import ij.plugin.PlugIn;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;

/*
 * Start up class of Plugin Manager Application:
 * Facade, Business logic, and overall-in-charge of providing the main user interface the
 * required list of PluginObjects that interface will use for display.
 */
public class PluginManager implements PlugIn, Observer {
	//For downloading/upload files
	//TODO fix it!
	public static final String TEMP_DOWNLOADURL = "http://pacific.mpi-cbg.de/update/";
	public static final String MAIN_URL = "http://pacific.mpi-cbg.de/uploads/incoming/";
	//public static final String MAIN_URL = "http://pacific.mpi-cbg.de/update/";
	public static final String TXT_FILENAME = "current.txt";
	public static final String XML_LOCK = "db.xml.gz.lock";
	public static final String XML_COMPRESSED = "db.xml.gz";
	public static final String XML_FILENAME = "db.xml";
	public static final String DTD_FILENAME = "plugins.dtd";
	public static final String XML_BACKUP = "db.bak";
	public static final String UPDATE_DIRECTORY = "update";

	//Key names for ij.Prefs for saved values ("cookies")
	//Note: ij.Prefs only work after Fiji itself is closed (Does not work if you close the console)
	public static final String PREFS_XMLDATE = "fiji.updater.xmlDate";
	public static final String PREFS_USER = "fiji.updater.login";
	private long xmlLastModified; //Track when was file modified (Lock conflict purposes)

	//PluginObjects for output at User Interface
	public List<PluginObject> pluginCollection;

	//Used for generating Plugin information
	private XMLFileDownloader xmlFileDownloader;
	private PluginListBuilder pluginListBuilder;
	public XMLFileReader xmlFileReader;
	private boolean isDeveloper;

	public void run(String arg) {
		try {
			System.out.println("********** Plugin Manager Startup **********");
			isDeveloper = new File(UpdateFiji.getFijiRootPath() + "fiji.cxx").exists();

			//First download the required information, which starts the program running
			xmlFileDownloader = new XMLFileDownloader();
			xmlFileDownloader.register(this);

			IJ.showStatus("Starting up Plugin Manager...");
			System.out.println("Attempting to download required information.");
			xmlFileDownloader.startDownload();

		} catch (Error e) {
			//Interface side: This should handle presentation side of exceptions
			IJ.showMessage("Error", "Failed to load Plugin Manager:\n" + e.getLocalizedMessage());
		} catch (IOException e) {
			try {
				new File(UpdateFiji.getFijiRootPath() + PluginManager.XML_COMPRESSED).deleteOnExit();
				//Scenario if decompression fails
				JOptionPane.showMessageDialog(null,
						"Plugin Manager has failed to decompress plugin information db.xml.gz." +
						"\nEither db.xml.gz is corrupted, or Plugin Manager requires an upgrade." +
						"\nPlease restart Fiji application and then start up Plugin Manager to try again.",
						"Failed compression", JOptionPane.ERROR_MESSAGE);
				if (JOptionPane.showConfirmDialog(null,
						"If this problem persists, you may want to check your connection settings," +
						"\notherwise it may be that Plugin Manager requires an upgrade." +
						"\n\nIf so, do you want to use an older version to help download the " +
						"latest version?", "Using UpdateFiji", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					//Load the older version of Fiji Updater instead
					UpdateFiji updateFiji = new UpdateFiji();
					updateFiji.hasGUI = true;
					updateFiji.exec(UpdateFiji.defaultURL);
				}
			} catch (SecurityException se) {
				JOptionPane.showMessageDialog(null,
					"Plugin Manager has failed to decompress plugin information db.xml.gz," +
					"\nand deletion of db.xml.gz did not succeed as well." +
					"\n\nYou are advised to quit Fiji, remove db.xml.gz manually, and then " +
					"restart Fiji and Plugin Manager to try again.",
					"Delete access denied", JOptionPane.ERROR_MESSAGE);
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
					xmlFileReader = new XMLFileReader(
							new ByteArrayInputStream(xmlFileDownloader.getXMLFileData()));

					//Start to build a list from the information
					pluginListBuilder = new PluginListBuilder(xmlFileReader, isDeveloper);
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

					MainUserInterface mainUserInterface = new MainUserInterface(this, isDeveloper);
					mainUserInterface.setVisible(true);
					mainUserInterface.setLocationRelativeTo(null); //center of the screen
					System.out.println("********** Startup Ended **********");
				}

			}
		} catch (Throwable e) {
			throw new Error(e.getLocalizedMessage());
		}
	}

	public boolean isDeveloper() {
		return isDeveloper;
	}
}