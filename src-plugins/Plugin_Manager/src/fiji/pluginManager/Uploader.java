package fiji.pluginManager;

import ij.IJ;
import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;
import org.xml.sax.SAXException;

public class Uploader implements Observer {
	private MainUserInterface mainUserInterface;
	private Updater updater;

	public Uploader(MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
		System.out.println("Uploader CLASS: Started up");
	}

	public void setUploadInformationAndStart(PluginManager pluginManager) {
		String message = null;
		try {
			updater = new Updater(pluginManager);
			updater.generateNewPluginRecords();
			updater.register(this);
			updater.uploadFilesToServer();
		} catch (TransformerConfigurationException e1) {
			message = e1.getLocalizedMessage();
		} catch (IOException e2) {
			message = e2.getLocalizedMessage();
		} catch (SAXException e3) {
			message = e3.getLocalizedMessage();
		}

		if (message != null) {
			IJ.showMessage("Error", "Failed to upload changes to server:\n" + message);
		} else {
			int failedListSize = updater.failList.size();
			int successfulListSize = updater.successList.size();
			if (failedListSize == 0 && successfulListSize == 0) { //no plugin files to upload
				mainUserInterface.exitWithRestartMessage("Success",
						"Updated existing plugin records successfully.\n" +
						"You need to restart Plugin Manager for changes to take effect.");
			} else {
				if (failedListSize > 0) {
					String namelist = "";
					for (PluginObject plugin : updater.failList)
						namelist += "\n" + plugin.getFilename();
					IJ.showMessage("Failed Uploads", "The following files failed to upload:" + namelist);
				}
				if (successfulListSize > 0) {
					int totalSize = failedListSize + successfulListSize;
					mainUserInterface.exitWithRestartMessage("Updated",
							successfulListSize + " out of " + totalSize +
							" plugin files uploaded successfully\n\n"
							+ "You need to restart Plugin Manager for changes to take effect.");
				} //if there are zero successful uploads, don't need to auto-close Plugin Manager
			}
		}
	}

	//Updating the interface
	public void refreshData(Observable subject) {
		if (subject == updater) {
			if (updater.allTasksComplete()) {
				IJ.showStatus("");
			} else {
				//continue displaying statuses
				IJ.showStatus("Uploading " + updater.getTaskname() + "...");
				IJ.showProgress(updater.getCurrentlyLoaded(), updater.getTotalToLoad());
			}
		}
	}

}
