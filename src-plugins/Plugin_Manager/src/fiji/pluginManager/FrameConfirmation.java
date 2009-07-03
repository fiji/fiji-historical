package fiji.pluginManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import javax.swing.text.BadLocationException;

public class FrameConfirmation extends JFrame {
	private PluginManager pluginManager;
	private JTextPane txtPluginList;
	private JTextPane txtAdditionalList;
	private JTextPane txtConflictsList;
	private JLabel lblStatus;
	private JButton btnDownload;
	private JButton btnCancel;
	private String msgConflictExists = "Conflicts exist. Please return to resolve them.";
	private String msgConflictNone = "No conflicts found. You may proceed.";
	private DependencyCompiler dependencyCompiler;

	public FrameConfirmation(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		setupUserInterface();
		pack();
	}

	private void setupUserInterface() {
		setTitle("Dependency and Conflict check");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		/* Create textpane to hold the information and its scrollpane */
		txtPluginList = new TextPaneDisplay();
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
		txtAdditionalList = new TextPaneDisplay();
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
		txtConflictsList = new TextPaneDisplay();
		txtConflictsList.setPreferredSize(new Dimension(675,120));
		JScrollPane txtScrollpane3 = new JScrollPane(txtConflictsList);
		txtScrollpane.getViewport().setBackground(txtConflictsList.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(675,120));

		JPanel conflictsPanel = new JPanel();
		conflictsPanel.setLayout(new BorderLayout());
		conflictsPanel.add(txtScrollpane3, BorderLayout.CENTER);
		conflictsPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

		lblStatus = new JLabel();

		//Buttons to start actions
		btnDownload = new JButton();
		btnDownload.setText("Confirm changes");
		btnDownload.setToolTipText("Start installing/uninstalling specified plugins");
		btnDownload.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				startActualChanges();
			}

		});
		btnDownload.setEnabled(false);

		btnCancel = new JButton();
		btnCancel.setText("Cancel");
		btnCancel.setToolTipText("Cancel and return to Plugin Manager");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				backToPluginManager();
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

	private void startActualChanges() {
		//indicate the actions as reference for Downloader (Installer) to refer to
		dependencyCompiler.setToInstall(dependencyCompiler.toInstallList);
		dependencyCompiler.setToUpdate(dependencyCompiler.toUpdateList);
		dependencyCompiler.setToRemove(dependencyCompiler.toRemoveList);
		dependencyCompiler = null;
		pluginManager.openDownloader();
	}

	private void backToPluginManager() {
		pluginManager.backToPluginManager();
	}

	public void displayInformation(DependencyCompiler dependencyCompiler) {
		this.dependencyCompiler = dependencyCompiler;

		//Gets the necessary information
		List<PluginObject> changeList = dependencyCompiler.changeList;
		Map<PluginObject,List<PluginObject>> installDependenciesMap = dependencyCompiler.installDependenciesMap;
		Map<PluginObject,List<PluginObject>> updateDependenciesMap = dependencyCompiler.updateDependenciesMap;
		Map<PluginObject,List<PluginObject>> uninstallDependentsMap = dependencyCompiler.uninstallDependentsMap;
		List<PluginObject> toInstallList = dependencyCompiler.toInstallList;
		List<PluginObject> toUpdateList = dependencyCompiler.toUpdateList;
		List<PluginObject> toRemoveList = dependencyCompiler.toRemoveList;

		//Compile a list of plugin names that conflicts with uninstalling (if any)
		List<String[]> installConflicts = new ArrayList<String[]>();
		List<String[]> updateConflicts = new ArrayList<String[]>();
		Iterator<PluginObject> iterInstall = installDependenciesMap.keySet().iterator();
		while (iterInstall.hasNext()) {
			PluginObject pluginAdd = iterInstall.next();
			List<PluginObject> pluginInstallList = installDependenciesMap.get(pluginAdd);
			List<PluginObject> pluginUpdateList = updateDependenciesMap.get(pluginAdd);
			Iterator<PluginObject> iterUninstall = uninstallDependentsMap.keySet().iterator();
			while (iterUninstall.hasNext()) {
				PluginObject pluginUninstall = iterUninstall.next();
				List<PluginObject> pluginUninstallList = uninstallDependentsMap.get(pluginUninstall);

				if (dependencyCompiler.conflicts(pluginInstallList, pluginUpdateList, pluginUninstallList)) {
					String installName = pluginAdd.getFilename();
					String uninstallName = pluginUninstall.getFilename();
					String[] arrNames = {installName, uninstallName};
					if (pluginAdd.isUpdateable()) {
						updateConflicts.add(arrNames);
					} else {
						installConflicts.add(arrNames);
					}
				}
			}
		}

		//Objective is to show user only information that was previously invisible
		toInstallList = ((PluginCollection)toInstallList).getList(PluginCollection.FILTER_UNLISTED_TO_INSTALL);
		toUpdateList = ((PluginCollection)toUpdateList).getList(PluginCollection.FILTER_UNLISTED_TO_UPDATE);
		toRemoveList = ((PluginCollection)toRemoveList).getList(PluginCollection.FILTER_UNLISTED_TO_UNINSTALL);

		//Actual display of information
		try {
			//textpane listing plugins explicitly set by user to take action
			TextPaneDisplay txtPluginList = (TextPaneDisplay)this.txtPluginList;
			for (int i = 0; i < changeList.size(); i++) {
				PluginObject myPlugin = changeList.get(i);
				String pluginName = myPlugin.getFilename();
				String pluginDescription = myPlugin.getDescription();

				String strAction = "";
				if (myPlugin.isRemovableOnly()) {
					//obviously, if its in "changes" list, then action is uninstall
					strAction = "To Uninstall";
				} else if (myPlugin.isInstallable()) {
					//obviously, if its in "changes" list, then action is install
					strAction = "To install";
				} else if (myPlugin.isUpdateable() && myPlugin.toRemove()) {
					strAction = "To Uninstall";
				} else if (myPlugin.toUpdate()) {
					strAction = "To Update";
				}

				txtPluginList.insertStyledText(pluginName, txtPluginList.BOLD_BLACK_TITLE);
				txtPluginList.insertDescription(pluginDescription);
				txtPluginList.insertBlankLine();
				txtPluginList.insertBoldText("Action: ");
				txtPluginList.insertText(strAction + "\n\n");
			}
			//ensure first line of text is always shown (i.e.: scrolled to top)
			txtPluginList.scrollToTop();

			//textpane listing additional plugins to add/remove
			TextPaneDisplay txtAdditionalList = (TextPaneDisplay)this.txtAdditionalList;
			if (toInstallList.size() > 0) {
				txtAdditionalList.insertStyledText("To Install", txtPluginList.BOLD_BLACK_TITLE);
				txtAdditionalList.insertPluginNamelist(toInstallList);
			}
			if (toUpdateList.size() > 0) {
				if (toInstallList.size() > 0)
					txtAdditionalList.insertBlankLine();
				txtAdditionalList.insertStyledText("To Update", txtPluginList.BOLD_BLACK_TITLE);
				txtAdditionalList.insertPluginNamelist(toUpdateList);
			}
			if (toRemoveList.size() > 0) {
				if (toInstallList.size() > 0 || toUpdateList.size() > 0)
					txtAdditionalList.insertBlankLine();
				txtAdditionalList.insertStyledText("To Remove", txtPluginList.BOLD_BLACK_TITLE);
				txtAdditionalList.insertPluginNamelist(toRemoveList);
			}
			if (toInstallList.size() == 0 && toUpdateList.size() == 0 && toRemoveList.size() == 0) {
				txtAdditionalList.setText("None.");
			}
			//ensure first line of text is always shown (i.e.: scrolled to top)
			txtAdditionalList.scrollToTop();

			//conflicts list textpane
			TextPaneDisplay txtConflictsList = (TextPaneDisplay)this.txtConflictsList;
			for (String[] names : installConflicts)
				txtConflictsList.insertText("Installing " + names[0] + " would conflict with uninstalling " + names[1] + "\n");
			for (String[] names : updateConflicts)
				txtConflictsList.insertText("Updating " + names[0] + " would conflict with uninstalling " + names[1] + "\n");

			//ensure first line of text is always shown (i.e.: scrolled to top)
			txtConflictsList.scrollToTop();

			//enable download button if no conflicts recorded
			if (installConflicts.size() == 0 && updateConflicts.size() == 0) {
				txtConflictsList.insertText("None.");
				btnDownload.setEnabled(true);
				lblStatus.setText(msgConflictNone);
				lblStatus.setForeground(Color.GREEN);
			} else {
				//otherwise, prevent user from clicking to download
				btnDownload.setEnabled(false);
				lblStatus.setText(msgConflictExists);
				lblStatus.setForeground(Color.RED);
			}

		} catch (BadLocationException e) {
			throw new Error("Problem with printing Plugin information: " + e.getMessage());
		}

	}
}
