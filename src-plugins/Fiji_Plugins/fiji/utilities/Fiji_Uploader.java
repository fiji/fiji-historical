package fiji.utilities;

/*
 * A plugin to facilitate uploading files to the Fiji dropbox.
 *
 * License: GPL v2
 * Author: Johannes E. Schindelin
 */

import fiji.utilities.FileUploader.UploadListener;
import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import java.io.File;

public class Fiji_Uploader implements PlugIn, UploadListener {
	FileUploader fileUploader;

	public void run(String arg) {
		OpenDialog od = new OpenDialog("File to upload", arg);
		if (od.getDirectory() == null)
			return; // cancelled

		File file = new File(od.getDirectory(), od.getFileName());
		try {
			fileUploader = new FileUploader("incoming");
			fileUploader.addListener(this);
			fileUploader.uploadFile(file);
			fileUploader.disconnectSession();
			System.out.println("Upload tasks complete.");
		} catch(Exception e){
			IJ.error(e.toString());
		}
	}

	public void uploadComplete(String filename) {
		IJ.showStatus("Upload successful!");
		IJ.showProgress(1, 1);
	}

	public void uploadFailed(String filename, Exception e) {
		IJ.error(e.toString());
	}

	public void update(String filename, int bytesSoFar, int bytesTotal) {
		IJ.showStatus("Uploading " + filename);
		IJ.showProgress(bytesSoFar, bytesTotal);
	}
}
