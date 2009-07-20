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

public class Uploader implements UploadListener {
	private MainUserInterface mainUserInterface;
	private Updater updater;

	public Uploader(MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
	}

	public synchronized void setUploadInformationAndStart(PluginManager pluginManager) {
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
			mainUserInterface.exitWithRestartMessage("Error",
					"Failed to upload changes to server: " + message + "\n\n" +
					"You need to restart Plugin Manager again.");
		}
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
				+ "You need to restart Plugin Manager for changes to take effect.");
	}
}
