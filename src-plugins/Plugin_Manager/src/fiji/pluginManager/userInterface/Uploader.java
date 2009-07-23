package fiji.pluginManager.userInterface;

import fiji.pluginManager.logic.PluginManager;
import fiji.pluginManager.logic.UpdateSource;
import fiji.pluginManager.logic.Updater;
import fiji.pluginManager.logic.FileUploader.SourceFile;
import fiji.pluginManager.logic.FileUploader.UploadListener;
import ij.IJ;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import org.xml.sax.SAXException;
import com.jcraft.jsch.JSchException;

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
		IJ.showStatus("");
		System.out.println("Upload process complete!");
		mainUserInterface.exitWithRestartMessage("Updated",
				"Files successfully uploaded to server!\n\n"
				+ "You need to restart Plugin Manager for changes to take effect."); //exit if successful
	}

	public void run() {
		String message = null;
		try {
			updater = new Updater(pluginManager);
			updater.generateNewPluginRecords();
			updater.uploadFilesToServer(this);
		} catch (TransformerConfigurationException e1) {
			message = e1.getLocalizedMessage();
		} catch (IOException e2) {
			message = e2.getLocalizedMessage();
		} catch (SAXException e3) {
			message = e3.getLocalizedMessage();
		} catch (ParserConfigurationException e4) {
			message = e4.getLocalizedMessage();
		} catch (JSchException e5) {
			message = e5.getLocalizedMessage();
		} catch (Exception e6) {
			message = e6.getLocalizedMessage();
		} catch (Error e7) {
			message = e7.getLocalizedMessage();
		}

		if (message != null) {
			//TODO  Instructions to delete unclean db.xml.lock.gz and unlock db.xml.gz again?
			mainUserInterface.exitWithRestartMessage("Error",
					"Failed to upload changes to server: " + message + "\n\n" +
					"You need to restart Plugin Manager again."); //exit if failure
		}
		//Doesn't need usual task of re-enabling MainUserInterface, as program will always exit after this
	}
}
