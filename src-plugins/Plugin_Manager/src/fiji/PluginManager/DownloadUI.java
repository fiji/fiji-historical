package fiji.PluginManager;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

class DownloadUI extends JFrame {
	private Installer installer = null; //observable, to grab data from
	private Timer timer = null;
	private PluginManager pluginManager = null;
	private JButton btnCancel = null;
	private JProgressBar progressBar = null;
	private JTextPane txtDownloadDetails = null;
	private String strCloseWhileDownloading = "Cancel";
	private String toolTipWhileDownloading = "Revert installation and return";
	private String strCloseWhenFinished = "Done";
	private String toolTipWhenFinished = "";
	private boolean isDownloading = false;

	public DownloadUI(PluginManager pluginManager) {
		super();
		this.setLayout(null);
		this.setSize(600, 400);
		this.setTitle("Download");
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.pluginManager = pluginManager;

		/* progress bar */
		progressBar = new JProgressBar();
		progressBar.setBounds(15, 30, 555, 30);
		progressBar.setStringPainted(true);
		progressBar.setString("100%");
		progressBar.setMinimum(0);
		progressBar.setMaximum(100); //percentage
		progressBar.setValue(100);

		/* Create textpane to hold the information */
		txtDownloadDetails = new JTextPane();
		txtDownloadDetails.setEditable(false);
		txtDownloadDetails.setBounds(0, 0, 555, 200);

		/* Create scrollpane to hold the textpane */
		JScrollPane txtScrollpane = new JScrollPane(txtDownloadDetails);
		txtScrollpane.getViewport().setBackground(txtDownloadDetails.getBackground());
		txtScrollpane.setBounds(15, 90, 555, 200);

		/* Button to cancel download (Or press done when complete) */
		btnCancel = new JButton(strCloseWhenFinished);
		btnCancel.setBounds(460, 315, 115, 30);
		btnCancel.setToolTipText(toolTipWhenFinished);
		btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	backToPluginManager();
            }
        });
		isDownloading = false;

		this.getContentPane().add(progressBar);
		this.getContentPane().add(txtScrollpane);
		this.getContentPane().add(btnCancel);

		timer = new Timer();
		timer.schedule(new DownloadStatus(), 0, 100); //status refreshes every 100 ms
	}

	private class DownloadStatus extends TimerTask {
		public void run() {
			if (installer == null) {
				//Remain at 0
				showDownloadStart("Preparing to download...");
				setPercentageComplete(0);
			}
			else {
				//List<PluginObject> toUninstallList;
				List<PluginObject> downloadedList = installer.getListOfDownloaded();
				List<PluginObject> waitingList = installer.getListOfWaiting();
				PluginObject currentlyDownloading = installer.getCurrentDownload();
				int totalBytes = installer.getBytesTotal();
				int downloadedBytes = installer.getBytesDownloaded();
				String strCurrentStatus = "";

				if (totalBytes == 0 || downloadedBytes == 0) {
					//Remain at 0
					showDownloadStart("Starting up download now...");
					setPercentageComplete(0);
				} else {
					//Able to display progress bar
					setPercentageComplete(downloadedBytes * 100 / totalBytes);
				}

				for (int i=0; i < downloadedList.size(); i++) {
					PluginObject myPlugin = downloadedList.get(i);
					if (i != 0) strCurrentStatus += "\n";
					strCurrentStatus += "Finished downloading " + myPlugin.getFilename();
				}
				if (currentlyDownloading != null) {
					strCurrentStatus += "\nNow downloading " + currentlyDownloading.getFilename();
				}
				txtDownloadDetails.setText(strCurrentStatus);

				//check if download has finished (Whether 100% success or not)
				if (waitingList.size() == 0) {
					showDownloadEnded();
					txtDownloadDetails.setText(txtDownloadDetails.getText() + "\nAll download tasks completed.");
					timer.cancel();
				}
			}
		}
	}

	private void backToPluginManager() {
		//plugin manager will deal with this
		if (!isDownloading)
			pluginManager.clickBackToPluginManager();
		else {
			if (JOptionPane.showConfirmDialog(this,
					"Are you sure you want to cancel the ongoing download?",
					"Revert Download?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
				pluginManager.clickBackToPluginManager();
				//delete or just let those that are already downloaded remain?
			}
		}
	}

	public void showDownloadStart(String startMessage) {
		btnCancel.setText(strCloseWhileDownloading);
		btnCancel.setToolTipText(toolTipWhileDownloading);
		txtDownloadDetails.setText(startMessage);
		progressBar.setString("0%");
		progressBar.setValue(0);
		isDownloading = true;
	}

	public void showDownloadEnded() {
		btnCancel.setText(strCloseWhenFinished);
		btnCancel.setToolTipText(toolTipWhenFinished);
		progressBar.setString("100%");
		progressBar.setValue(100);
		isDownloading = false;
	}

	public void setPercentageComplete(int percent) {
		progressBar.setString(percent + "%");
		progressBar.setValue(percent);
	}

	public void insertText(String text) {
		txtDownloadDetails.setText(txtDownloadDetails.getText() + text);
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
