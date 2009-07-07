package fiji.pluginManager;
import ij.IJ;
import ij.plugin.PlugIn;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class PluginManager implements PlugIn, Observer {
	public static final String defaultServerPath =
		"var/www/update/"; //TODO: strange, why not the original "/var/www/update/"?
	public static final String MAIN_URL = "http://pacific.mpi-cbg.de/update/";
	public static final String TXT_FILENAME = "current.txt";
	public static final String XML_FILENAME = "db.xml";
	public static final String DTD_FILENAME = "plugins.dtd";
	public static final String XML_DIRECTORY = "plugininfo";
	public static final String UPDATE_DIRECTORY = "update";
	public List<PluginObject> pluginCollection;
	public List<PluginObject> readOnlyList;
	private XMLFileDownloader xmlFileDownloader;
	private PluginListBuilder pluginListBuilder;

	public PluginManager() {
		System.out.println("How many times is this run?");
	}

	public void run(String arg) {
		try {
			//Downloads files, convert information into PluginObjects useful for interface usage
			startup();
			pluginCollection = pluginListBuilder.pluginCollection;

			//Inform user of read-only plugins if any
			readOnlyList = pluginListBuilder.readOnlyList;
			if (readOnlyList.size() > 0) {
				String namelist = "";
				for (int i = 0; i < readOnlyList.size(); i++) {
					if (i != 0 && i % 3 == 0)
						namelist += "\n";
					namelist += readOnlyList.get(i).getFilename();
					if (i < readOnlyList.size() -1)
						namelist += ", ";
				}
				IJ.showMessage("Read-Only Plugins", "WARNING: The following plugin files are set to read-only, you are advised to quit Fiji and set to writable:\n" + namelist);
			}

		} catch (Error e) {
			//Interface side: This should handle presentation side of exceptions
			IJ.showMessage("Error", "Failed to load Plugin Manager:\n" + e.getLocalizedMessage());
		}
		FrameManager frameManager = new FrameManager(this);
		frameManager.setVisible(true);
	}

	//"Start the start-up" (Download file, convert information into useful format)
	private void startup() {
		xmlFileDownloader = new XMLFileDownloader();
		xmlFileDownloader.register(this);
		xmlFileDownloader.startDownload();
	}

	//Show progress of Plugin Manager startup at IJ bar
	public void refreshData(Observable subject) {
		try {
			if (subject == xmlFileDownloader) {
				IJ.showStatus("Downloading " + xmlFileDownloader.getTaskname() + "...");
				IJ.showProgress(xmlFileDownloader.getCurrentlyLoaded(), xmlFileDownloader.getTotalToLoad());
				if (xmlFileDownloader.allTasksComplete()) {
					pluginListBuilder = new PluginListBuilder();
					pluginListBuilder.register(this);
					pluginListBuilder.buildFullPluginList();
				}
			} else if (subject == pluginListBuilder) {
				IJ.showStatus("Downloading " + pluginListBuilder.getTaskname() + "...");
				IJ.showProgress(pluginListBuilder.getCurrentlyLoaded(), pluginListBuilder.getTotalToLoad());
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