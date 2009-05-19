package fiji.managerUI;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class DownloadUI extends JFrame {
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
		txtDownloadDetails.setText("");
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
	}

	private void backToPluginManager() {
		//plugin manager will deal with this
		if (!isDownloading)
			pluginManager.clickBackToPluginManager();
		else {
			if (JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel the download?", "Revert Download?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
				pluginManager.clickBackToPluginManager();
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
	
}
