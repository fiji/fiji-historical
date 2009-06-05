package fiji.PluginManager;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

class DownloadUI extends JFrame {
	private PluginManager pluginManager; //Used if opened from Plugin Manager UI
	private Installer installer; //To grab data from (Used if Plugin Manager UI not null)

	private Timer timer;
	private JButton btnClose;
	private JProgressBar progressBar;
	private JTextPane txtProgressDetails;
	private String strCloseWhileDownloading = "Cancel";
	private String toolTipWhileDownloading = "Stop downloads and return";
	private String strCloseWhenFinished = "Done";
	private String toolTipWhenFinished = "Close Window";
	private boolean isProgressing;

	//Download Window opened from Plugin Manager UI
	public DownloadUI(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		setUpUserInterface();
		setupButtonsAndListeners();
	}

	private void setUpUserInterface() {
		setLayout(null);
		//getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		setSize(600, 400);
		setTitle("Download");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		/* progress bar */
		progressBar = new JProgressBar();
		progressBar.setBounds(15, 30, 555, 30);
		progressBar.setStringPainted(true);
		progressBar.setString("100%");
		progressBar.setMinimum(0);
		progressBar.setMaximum(100); //percentage
		progressBar.setValue(100);

		/* Create textpane to hold the information */
		txtProgressDetails = new JTextPane();
		txtProgressDetails.setEditable(false);
		txtProgressDetails.setBounds(0, 0, 555, 200);

		/* Create scrollpane to hold the textpane */
		JScrollPane txtScrollpane = new JScrollPane(txtProgressDetails);
		txtScrollpane.getViewport().setBackground(txtProgressDetails.getBackground());
		txtScrollpane.setBounds(15, 90, 555, 200);

		/* Button to cancel progressing task (Or press done when complete) */
		btnClose = new JButton(strCloseWhenFinished);
		btnClose.setBounds(460, 315, 115, 30);
		btnClose.setToolTipText(toolTipWhenFinished);
		isProgressing = false;

		getContentPane().add(progressBar);
		getContentPane().add(txtScrollpane);
		getContentPane().add(btnClose);
	}

	private void setupButtonsAndListeners() {
		if (pluginManager != null) {
			//Close button listener
			btnClose.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	            	backToPluginManager();
	            }
	        });
			//Timer to check for download
			timer = new Timer();
			timer.schedule(new DownloadStatus(), 0, 100); //status refreshes every 100 ms
		}
	}

	private class DownloadStatus extends TimerTask {
		public void run() {
			if (installer == null) {
				//Remain at 0
				showProgressStart("Preparing to download...");
				setPercentageComplete(0);
			}
			else {
				//List<PluginObject> toUninstallList;
				List<PluginObject> downloadedList = installer.getListOfDownloaded();
				List<PluginObject> failedList = installer.getListOfFailedDownloads();
				PluginObject currentlyDownloading = installer.getCurrentDownload();
				int totalBytes = installer.getBytesTotal();
				int downloadedBytes = installer.getBytesDownloaded();
				boolean stillDownloading = installer.stillDownloading();
				String strCurrentStatus = "";

				if (totalBytes == 0 || downloadedBytes == 0) {
					//Remain at 0
					showProgressStart("Starting up download now...");
					setPercentageComplete(0);
				} else {
					//Able to display progress bar
					setPercentageComplete(downloadedBytes * 100 / totalBytes);

					for (int i=0; i < downloadedList.size(); i++) {
						PluginObject myPlugin = downloadedList.get(i);
						if (i != 0) strCurrentStatus += "\n";
						strCurrentStatus += "Finished downloading " + myPlugin.getFilename();
					}
					for (int i=0; i < failedList.size(); i++) {
						PluginObject myPlugin = failedList.get(i);
						strCurrentStatus += "\n" + myPlugin.getFilename() + " failed to download.";
					}
					if (currentlyDownloading != null) {
						strCurrentStatus += "\nNow downloading " + currentlyDownloading.getFilename();
					}
					txtProgressDetails.setText(strCurrentStatus);

					//check if download has finished (Whether 100% success or not)
					if (stillDownloading == false) {
						showProgressComplete();
						txtProgressDetails.setText(txtProgressDetails.getText() + "\nAll download tasks completed.");
						timer.cancel();
					}
				}
			}
		}
	}

	private void backToPluginManager() {
		//plugin manager will deal with this
		if (!isProgressing)
			pluginManager.clickBackToPluginManager();
		else {
			if (JOptionPane.showConfirmDialog(this,
					"Are you sure you want to cancel the ongoing download?",
					"Revert Download?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
				//below comment: loop through the waitingList
				/*
					String fullPath = pluginDataProcessor.prefix(updateDirectory +
					File.separator + name);
					if (name.startsWith("fiji-")) {
						boolean useMacPrefix = pluginDataProcessor.getUseMacPrefix();
						String macPrefix = pluginDataProcessor.getMacPrefix();
						fullPath = pluginDataProcessor.prefix((useMacPrefix ? macPrefix : "") + name);
					}
					try {
						new File(fullPath).delete();
					} catch (Exception e2) { }*/
					
				pluginManager.clickBackToPluginManager();
				//delete or just let those that are already downloaded remain?
			}
		}
	}

	public void showProgressStart(String startMessage) {
		btnClose.setText(strCloseWhileDownloading);
		btnClose.setToolTipText(toolTipWhileDownloading);
		txtProgressDetails.setText(startMessage);
		progressBar.setString("0%");
		progressBar.setValue(0);
		isProgressing = true;
	}

	public void showProgressComplete() {
		btnClose.setText(strCloseWhenFinished);
		btnClose.setToolTipText(toolTipWhenFinished);
		progressBar.setString("100%");
		progressBar.setValue(100);
		isProgressing = false;
	}

	public void setPercentageComplete(int percent) {
		progressBar.setString(percent + "%");
		progressBar.setValue(percent);
	}

	public void insertText(String text) {
		txtProgressDetails.setText(txtProgressDetails.getText() + text);
	}

	public void setInstaller(Installer installer) {
		if (this.installer != null) throw new Error("Installer object already exists.");
		else {
			this.installer = installer;
			installer.startDelete();
			installer.startDownload();
		}
	}

}
