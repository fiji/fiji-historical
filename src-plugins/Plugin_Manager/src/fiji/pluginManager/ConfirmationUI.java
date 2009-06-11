package fiji.pluginManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;

public class ConfirmationUI extends JFrame {
	private PluginManager pluginManager; //Used if opened from Plugin Manager UI
	private JTextPane txtPluginList;
	private JTextPane txtAdditionalList;
	private JTextPane txtConflictsList;
	private JLabel lblStatus;
	private JButton btnDownload;
	private JButton btnCancel;
	private String msgConflictExists = "Conflicts exist. Please return to resolve them.";
	private String msgConflictNone = "No conflicts found. You may proceed.";

	public static void main(String args[]) {
		new ConfirmationUI(null).setVisible(true);
	}

	public ConfirmationUI(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		setupUserInterface();
		pack();
	}

	private void setupUserInterface() {
		setTitle("Dependency and Conflict check");
		//setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		/* Create textpane to hold the information and its scrollpane */
		txtPluginList = new JTextPane();
		txtPluginList.setEditable(false);
		txtPluginList.setPreferredSize(new Dimension(400,260));
		JScrollPane txtScrollpane = new JScrollPane(txtPluginList);
		txtScrollpane.getViewport().setBackground(txtPluginList.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(400,260));

		/* Tabbed pane of plugin list to hold the textpane (w/ scrollpane) */
		JTabbedPane tabbedPane = new JTabbedPane();
		JPanel panelPluginList = new JPanel();
		panelPluginList.setLayout(new BorderLayout());
		panelPluginList.add(txtScrollpane, BorderLayout.CENTER);
		tabbedPane.addTab("Selected Plugins", null, panelPluginList, "Your selection of plugins");
		tabbedPane.setPreferredSize(new Dimension(400,260));

		/* Create textpane to hold the information and its scrollpane */
		txtAdditionalList = new JTextPane();
		txtAdditionalList.setEditable(false);
		txtAdditionalList.setPreferredSize(new Dimension(260,260));
		JScrollPane txtScrollpane2 = new JScrollPane(txtAdditionalList);
		txtScrollpane.getViewport().setBackground(txtAdditionalList.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(260,260));

		/* Tabbed pane of additional plugin list to hold the textpane (w/ scrollpane) */
		JTabbedPane tabbedPaneAdditional = new JTabbedPane();
		JPanel panelPluginAdditional = new JPanel();
		panelPluginAdditional.setLayout(new BorderLayout());
		panelPluginAdditional.add(txtScrollpane2, BorderLayout.CENTER);
		tabbedPaneAdditional.addTab("Additional Changes", null, panelPluginAdditional, "Additional installations or removals to be made due to dependencies");
		tabbedPaneAdditional.setPreferredSize(new Dimension(260,260));

		JPanel listsPanel = new JPanel();
		listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.X_AXIS));
		listsPanel.add(tabbedPane);
		listsPanel.add(Box.createRigidArea(new Dimension(15,0)));
		listsPanel.add(tabbedPaneAdditional);
		listsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		/* Create textpane to hold the information and its scrollpane */
		txtConflictsList = new JTextPane();
		txtConflictsList.setEditable(false);
		txtConflictsList.setPreferredSize(new Dimension(675,120));
		JScrollPane txtScrollpane3 = new JScrollPane(txtConflictsList);
		txtScrollpane.getViewport().setBackground(txtConflictsList.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(675,120));

		JPanel conflictsPanel = new JPanel();
		conflictsPanel.setLayout(new BorderLayout());
		conflictsPanel.add(txtScrollpane3, BorderLayout.CENTER);
		conflictsPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

		lblStatus = new JLabel(msgConflictExists);

		//Buttons to start actions
		btnDownload = new JButton();
		btnDownload.setText("Confirm changes");
		btnDownload.setToolTipText("Start installing/uninstalling specified plugins");
		btnDownload.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				//
			}

		});

		btnCancel = new JButton();
		btnCancel.setText("Cancel");
		btnCancel.setToolTipText("Cancel and return to Plugin Manager");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				//
			}

		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(lblStatus);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnDownload);
		buttonPanel.add(Box.createRigidArea(new Dimension(15,0)));
		buttonPanel.add(btnCancel);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(listsPanel);
		getContentPane().add(conflictsPanel);
		getContentPane().add(buttonPanel);
	}
}
