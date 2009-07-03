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
import java.util.List;

/*
 * Interface of a separate window, when downloading plugins.
 */
class FrameInstaller extends JFrame {
	private PluginManager pluginManager;
	private Timer timer;
	private JButton btnClose;
	private JProgressBar progressBar;
	private JTextPane txtProgressDetails;
	private Installer installer;

	//Download Window opened from Plugin Manager UI
	public FrameInstaller(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		setUpUserInterface();
		pack();
	}

	private void setUpUserInterface() {
		setTitle("Download");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		/* progress bar */
		progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(555, 30));
		progressBar.setStringPainted(true);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);

		/* Create textpane to hold the information */
		txtProgressDetails = new TextPaneDisplay();
		txtProgressDetails.setPreferredSize(new Dimension(555, 200));

		/* Create scrollpane to hold the textpane */
		JScrollPane txtScrollpane = new JScrollPane(txtProgressDetails);
		txtScrollpane.getViewport().setBackground(txtProgressDetails.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(555, 200));

		/* Button to cancel progressing task (Or press done when complete) */
		btnClose = new JButton();
		btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	closeFrameInstaller();
            }
        });

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

	public void setInstaller(Installer installer) {
		if (this.installer != null) throw new Error("Installer object already exists.");
		else {
			this.installer = installer;
			installer.beginOperations();
			if (installer.isDownloading()) {
				//Timer to check for download
				timer = new Timer();
				timer.schedule(new DownloadStatus(), 0, 100); //status refreshes every 100 ms
			} else {
				//no progressing tasks, then just display one-time
				setFrameDisplay();
			}
		}
	}

	private void setFrameDisplay() {
		int totalBytes = installer.getBytesTotal();
		int downloadedBytes = installer.getBytesDownloaded();
		if (installer.isDownloading()) {
			btnClose.setText("Cancel");
			btnClose.setToolTipText("Stop downloads and return");
		} else {
			btnClose.setText("Done");
			btnClose.setToolTipText("Close Window");
		}
		((TextPaneDisplay)txtProgressDetails).showDownloadProgress(installer);
		setPercentageComplete(downloadedBytes, totalBytes);
	}

	private class DownloadStatus extends TimerTask {
		public void run() {
			setFrameDisplay();
			if (installer.isDownloading() == false)
				timer.cancel(); //Not downloading anything, no progress to refresh
		}
	}

	private void closeFrameInstaller() {
		//plugin manager will deal with this
		if (installer.isDownloading()) {
			if (JOptionPane.showConfirmDialog(this,
					"Are you sure you want to cancel the ongoing download?",
					"Stop?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
				installer.stopDownload();
			} else
				return;
		}
		if (installer != null &&
			(installer.downloadedList.size() > 0 || installer.markedUninstallList.size() > 0))
			pluginManager.exitWithRestartFijiMessage();
		else
			pluginManager.backToPluginManager();
	}

	private void setPercentageComplete(int downloaded, int total) {
		int percent = (total > 0 ? downloaded*100/total : 0);
		progressBar.setString(percent + "%");
		progressBar.setValue(percent);
	}
}
