package fiji.pluginManager.ui;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import java.awt.TextField;
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
 * The "interface" for uploading plugins (Consists of IJ progress bar & IJ GenericDialog).
 */
public class Uploader implements UploadListener, Runnable {
	private volatile MainUserInterface mainUserInterface;
	private volatile PluginManager pluginManager;
	private volatile Updater updater;
	private Thread uploadThread;

	public Uploader(MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
	}

	//Ask for login details and then began upload process
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
		boolean loginSuccess = false;
		try {
			String username = "";
			String password = "";
			updater = new Updater(pluginManager);
			do {
				//Dialog to enter username and password
				GenericDialog gd = new GenericDialog("Login");
				gd.addStringField("Username", Prefs.get(PluginManager.PREFS_USER, ""), 20);
				gd.addStringField("Password", "", 20);
				((TextField)gd.getStringFields().lastElement()).setEchoChar('*');
				gd.showDialog();
				if (gd.wasCanceled()) {
					break;
				}

				//Get the required login information
				username = gd.getNextString();
				password = gd.getNextString();
				Prefs.set(PluginManager.PREFS_USER, username);

			} while ((loginSuccess = updater.setLogin(username, password)) == false);

			if (loginSuccess) {
				mainUserInterface.setVisible(false); //this UI not needed for upload
				updater.generateNewPluginRecords();
				updater.uploadFilesToServer(this);
			}

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
			if (loginSuccess) {
				IJ.showStatus(""); //exit if successful
				mainUserInterface.exitWithRestartMessage("Updated",
					"Files successfully uploaded to server!\n\n"
					+ "You need to restart Plugin Manager for changes to take effect.");
			} else {
				//Implies user declines entering login details, thus return to main UI
				mainUserInterface.backToPluginManager();
			}
		}
	}

}
