package fiji.pluginManager.userInterface;

import fiji.pluginManager.logic.Observable;
import fiji.pluginManager.logic.Observer;
import fiji.pluginManager.logic.PluginManager;
import fiji.pluginManager.logic.PluginObject;
import fiji.pluginManager.logic.Updater;
import ij.IJ;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
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
		} catch (ParserConfigurationException e4) {
			message = e4.getLocalizedMessage();
		} catch (Error e5) {
			message = e5.getLocalizedMessage();
		}

		if (message != null) {
			IJ.showMessage("Error", "Failed to upload changes to server:\n" + message);
		} else {
			Iterator<PluginObject> iterUploadFail = updater.iterUploadFail();
			int numFailedUploads = updater.numberOfFailedUploads();
			int numSuccessfulUploads = updater.numberOfSuccessfulUploads();
			if (numFailedUploads > 0) {
				String namelist = "";
				while (iterUploadFail.hasNext())
					namelist += "\n" + iterUploadFail.next().getFilename();
				IJ.showMessage("Failed Uploads", "The following files failed to upload:" + namelist);
			}
			if (numSuccessfulUploads > 0) {
				int successfulNum = updater.numberOfSuccessfulUploads();
				int totalSize = updater.numberOfFailedUploads() + successfulNum;
				mainUserInterface.exitWithRestartMessage("Updated",
						successfulNum + " out of " + totalSize +
						" plugins uploaded successfully\n\n"
						+ "You need to restart Plugin Manager for changes to take effect.");
			} //if there are zero successful uploads, don't need to auto-close Plugin Manager
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
