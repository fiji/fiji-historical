package fiji.pluginManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/*
 * Interface of a separate window, when downloading plugins.
 */
class DownloadUI extends JFrame {
	private PluginManager pluginManager;
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
		pack();
	}

	private void setUpUserInterface() {
		setTitle("Download");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		/* progress bar */
		progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(555, 30));
		progressBar.setStringPainted(true);
		progressBar.setString("100%");
		progressBar.setMinimum(0);
		progressBar.setMaximum(100); //percentage
		progressBar.setValue(100);

		/* Create textpane to hold the information */
		txtProgressDetails = new JTextPane();
		txtProgressDetails.setEditable(false);
		txtProgressDetails.setPreferredSize(new Dimension(555, 200));

		/* Create scrollpane to hold the textpane */
		JScrollPane txtScrollpane = new JScrollPane(txtProgressDetails);
		txtScrollpane.getViewport().setBackground(txtProgressDetails.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(555, 200));

		/* Button to cancel progressing task (Or press done when complete) */
		btnClose = new JButton(strCloseWhenFinished);
		btnClose.setToolTipText(toolTipWhenFinished);
		isProgressing = false;
		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.add(btnClose);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		panel.add(progressBar);
		panel.add(Box.createRigidArea(new Dimension(0,15)));
		panel.add(txtScrollpane);
		panel.add(Box.createRigidArea(new Dimension(0, 15)));
		panel.add(btnPanel);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);
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

				if (totalBytes == 0) {
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
						if (i != 0 && !strCurrentStatus.equals("")) strCurrentStatus += "\n";
						strCurrentStatus += myPlugin.getFilename() + " failed to download.";
					}
					if (currentlyDownloading != null) {
						strCurrentStatus += "\nNow downloading " + currentlyDownloading.getFilename();
					}
					txtProgressDetails.setText(strCurrentStatus);

					//check if download has finished (Whether 100% success or not)
					if (stillDownloading == false) {
						showProgressComplete();
						if (downloadedList.size() > 0) {
							int totalSize = downloadedList.size() + failedList.size();
							txtProgressDetails.setText(txtProgressDetails.getText() + "\n" +
									downloadedList.size() + " of " + totalSize +
									" download tasks completed.");
						} else {
							txtProgressDetails.setText(txtProgressDetails.getText() +
									"\nDownload(s) failed.");
						}
						timer.cancel();
					}
				}
			}
		}
	}

	private void backToPluginManager() {
		//plugin manager will deal with this
		if (!isProgressing)
			pluginManager.backToPluginManager();
		else {
			if (JOptionPane.showConfirmDialog(this,
					"Are you sure you want to cancel the ongoing download?",
					"Revert Download?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
				installer.stopDownload();
				pluginManager.backToPluginManager();
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
