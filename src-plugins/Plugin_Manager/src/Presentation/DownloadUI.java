package Presentation;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JProgressBar;
import javax.swing.JButton;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class DownloadUI extends JFrame {
	private PluginMgrUI pluginMgrUI = null;
	public DownloadUI(PluginMgrUI pluginMgrUI) {
		super();
		this.setLayout(null);
		this.setSize(600, 400);
		this.setTitle("Download");
		this.pluginMgrUI = pluginMgrUI;

		/* progress bar */
		JProgressBar progressBar = new JProgressBar();
		progressBar.setBounds(15, 30, 555, 30);
		progressBar.setStringPainted(true);
		progressBar.setString("25%");
		progressBar.setMinimum(0);
		progressBar.setMaximum(100); //percentage
		progressBar.setValue(25);

		/* Create textpane to hold the information */
		JTextPane txtDownloadDetails = new JTextPane();
		txtDownloadDetails.setText("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nhahaha!");
		txtDownloadDetails.setBounds(0, 0, 555, 200);

		/* Create scrollpane to hold the textpane */
		JScrollPane txtScrollpane = new JScrollPane(txtDownloadDetails);
		txtScrollpane.getViewport().setBackground(txtDownloadDetails.getBackground());
		txtScrollpane.setBounds(15, 90, 555, 200);

		/* Button to cancel download (Or press done when complete) */
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setBounds(460, 315, 115, 30);
		btnCancel.setToolTipText("Revert installation and return");
		btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //may want to check for what state it is in?
            	backToPluginManager();
            }
        });

		this.getContentPane().add(progressBar);
		this.getContentPane().add(txtScrollpane);
		this.getContentPane().add(btnCancel);
	}

	private void backToPluginManager() {
		pluginMgrUI.backToPluginManager();
	}
}
