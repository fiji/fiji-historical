package fiji.pluginManager.ui;

import ij.IJ;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

import com.jcraft.jsch.JSchException;

import fiji.pluginManager.logic.PluginManager;
import fiji.pluginManager.logic.UpdateSource;
import fiji.pluginManager.logic.Updater;
import fiji.pluginManager.logic.FileUploader.SourceFile;
import fiji.pluginManager.logic.FileUploader.UploadListener;

/*
 * The "interface" for uploading plugins (Actually, it mainly consists of IJ progress bar).
 */
public class Uploader implements UploadListener, Runnable {
	private volatile MainUserInterface mainUserInterface;
	private volatile PluginManager pluginManager;
	private volatile Updater updater;
	private Thread uploadThread;

	public Uploader(MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
	}

	public synchronized void setUploadInformationAndStart(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		uploadThread = new Thread(this);
		uploadThread.start();
	}

	public synchronized void update(SourceFile source, long bytesSoFar, long bytesTotal) {
		UpdateSource updateSource = (UpdateSource)source;
		IJ.showStatus("Uploading " + updateSource.getRelativePath() + "...");
		IJ.showProgress((int)bytesSoFar, (int)bytesTotal);
	}

	public synchronized void uploadFileComplete(SourceFile source) {
		UpdateSource updateSource = (UpdateSource)source;
		System.out.println("File " + updateSource.getRelativePath() + " uploaded.");
	}

	public synchronized void uploadProcessComplete() {
		System.out.println("Upload process was a success!");
	}

	public void run() {
		String error_message = null;
		try {
			updater = new Updater(pluginManager);
			updater.generateNewPluginRecords();
			updater.uploadFilesToServer(this);
		} catch (TransformerConfigurationException e1) {
			error_message = e1.getLocalizedMessage();
		} catch (IOException e2) {
			error_message = e2.getLocalizedMessage();
		} catch (SAXException e3) {
			error_message = e3.getLocalizedMessage();
		} catch (ParserConfigurationException e4) {
			error_message = e4.getLocalizedMessage();
		} catch (JSchException e5) {
			error_message = e5.getLocalizedMessage();
		} catch (Exception e6) {
			error_message = e6.getLocalizedMessage();
		} catch (Error e7) {
			error_message = e7.getLocalizedMessage();
		}

		//If there is an error message, show it
		if (error_message != null) {
			mainUserInterface.exitWithRestartMessage("Error",
					"Failed to upload changes to server: " + error_message + "\n\n" +
					"You need to restart Plugin Manager again."); //exit if failure
		} else {
			IJ.showStatus(""); //exit if successful
			mainUserInterface.exitWithRestartMessage("Updated",
					"Files successfully uploaded to server!\n\n"
					+ "You need to restart Plugin Manager for changes to take effect.");
		}
		//Doesn't need usual task of re-enabling MainUserInterface, as program always exit after this
	}
}
