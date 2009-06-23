package fiji.pluginManager;
import ij.IJ;
import ij.plugin.PlugIn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/*
 * Main User Interface
 */
public class PluginManager extends JFrame implements PlugIn, TableModelListener {
	private List<PluginObject> viewList;
	private PluginListBuilder pluginListBuilder;
	private String fileURL = "http://pacific.mpi-cbg.de/update/current.txt";//should be XML file actually
	private String saveFile = "current.txt";//should be XML file actually

	/* User Interface elements */
	private boolean isDeveloper = true; //temporarily activated by change of code
	private JFrame loadedFrame;
	private String[] arrViewingOptions = {
			"View all plugins",
			"View installed plugins only",
			"View uninstalled plugins only",
			"View up-to-date plugins only",
			"View update-able plugins only"
			};
	private JTextField txtSearch;
	private JComboBox comboBoxViewingOptions;
	private PluginTable table;
	private JLabel lblPluginSummary;
	private JTextPane txtPluginDetails;
	private PluginObject currentPlugin;
	private JButton btnStart;
	private JButton btnOK;

	//For developers
	private JButton btnUpload;
	private JButton btnEditDescriptions;

	public PluginManager() {
		super("Plugin Manager");
		try {
			pluginListBuilder = new PluginListBuilder(fileURL, saveFile);
			pluginListBuilder.downloadXMLFile(); //should be XML file actually
			pluginListBuilder.buildLocalPluginInformation(); //2nd step
			pluginListBuilder.buildFullPluginList(); //3rd step
			viewList = pluginListBuilder.getExistingPluginList(); //initial view: All plugins

			setUpUserInterface();
			setVisible(true);
			pack();
		} catch (Error e) {
			//Interface side: This should handle presentation side of exceptions
			//i.e.: pluginDataReader should throw _Error_ objects to here.
			IJ.showMessage("Error", "Failed to load Plugin Manager:\n" + e.getLocalizedMessage());
			dispose();
		}
	}

	private void setUpUserInterface() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		/* Create textpane to hold the information and its scrollpane */
		txtPluginDetails = new TextPaneDisplay();
		txtPluginDetails.setPreferredSize(new Dimension(335,315));
		JScrollPane txtScrollpane = new JScrollPane(txtPluginDetails);
		txtScrollpane.getViewport().setBackground(txtPluginDetails.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(335,315));

		/* Tabbed pane of plugin details to hold the textpane (w/ scrollpane) */
		JTabbedPane tabbedPane = new JTabbedPane();
		JPanel panelPluginDetails = new JPanel();
		panelPluginDetails.setLayout(new BorderLayout());
		panelPluginDetails.add(txtScrollpane, BorderLayout.CENTER);
		tabbedPane.addTab("Details", null, panelPluginDetails, "Individual Plugin information");
		tabbedPane.setPreferredSize(new Dimension(335,315));

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(Box.createVerticalGlue());
		rightPanel.add(tabbedPane);

		if (isDeveloper) {
			JPanel editButtonPanel = new JPanel();
			editButtonPanel.setLayout(new BoxLayout(editButtonPanel, BoxLayout.X_AXIS));
			btnEditDescriptions = new JButton("Edit Descriptions");
			btnEditDescriptions.setToolTipText("Edit the descriptions of selected plugin");
			btnEditDescriptions.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					clickToEditDescriptions();
				}

			});
			btnEditDescriptions.setEnabled(false);
			editButtonPanel.add(btnEditDescriptions);
			editButtonPanel.add(Box.createHorizontalGlue());
			rightPanel.add(editButtonPanel);
		} else {
			rightPanel.add(Box.createRigidArea(new Dimension(0,25)));
		}

		/* Create text search */
		JLabel lblSearch1 = new JLabel("Search:", SwingConstants.LEFT);
		txtSearch = new JTextField();
		txtSearch.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				changeListingListener();
			}

			public void removeUpdate(DocumentEvent e) {
				changeListingListener();
			}

			public void insertUpdate(DocumentEvent e) {
				changeListingListener();
			}

		});

		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
		searchPanel.add(lblSearch1);
		searchPanel.add(Box.createRigidArea(new Dimension(10,0)));
		searchPanel.add(txtSearch);

		/* Create combo box of options */
		JLabel lblSearch2 = new JLabel("View Options:");
		comboBoxViewingOptions = new JComboBox(arrViewingOptions);
		comboBoxViewingOptions.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				changeListingListener();
			}

		});
		JPanel comboBoxPanel = new JPanel();
		comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.X_AXIS));
		comboBoxPanel.add(lblSearch2);
		comboBoxPanel.add(Box.createRigidArea(new Dimension(10,0)));
		comboBoxPanel.add(comboBoxViewingOptions);

		/* Create labels to annotate table */
		JLabel lblTable = new JLabel("Please choose what you want to install/uninstall:");
		JPanel lblTablePanel = new JPanel();
		lblTablePanel.add(lblTable);
		lblTablePanel.add(Box.createHorizontalGlue());
		lblTablePanel.setLayout(new BoxLayout(lblTablePanel, BoxLayout.X_AXIS));

		/* Label text for plugin summaries */
		lblPluginSummary = new JLabel();
		JPanel lblSummaryPanel = new JPanel();
		lblSummaryPanel.add(lblPluginSummary);
		lblSummaryPanel.add(Box.createHorizontalGlue());
		lblSummaryPanel.setLayout(new BoxLayout(lblSummaryPanel, BoxLayout.X_AXIS));

		/* Create the plugin table and set up its scrollpane */
		table = new PluginTable(viewList, this);
		JScrollPane pluginListScrollpane = new JScrollPane(table);
		pluginListScrollpane.getViewport().setBackground(table.getBackground());

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(searchPanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,10)));
		leftPanel.add(comboBoxPanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,10)));
		leftPanel.add(lblTablePanel);
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));
		leftPanel.add(pluginListScrollpane);
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));
		leftPanel.add(lblSummaryPanel);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(leftPanel);
		topPanel.add(Box.createRigidArea(new Dimension(15,0)));
		topPanel.add(rightPanel);
		topPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 5, 15));

		//Button to start actions
		btnStart = new JButton();
		btnStart.setText("Apply changes");
		btnStart.setToolTipText("Start installing/uninstalling specified plugins");
		btnStart.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToBeginOperations();
			}

		});
		btnStart.setEnabled(false);

		if (isDeveloper) {
			//Button to start uploading to server
			btnUpload = new JButton();
			btnUpload.setText("Upload to server");
			btnUpload.setToolTipText("Upload the selected plugins to server");
			btnUpload.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					clickToUploadRecords();
				}

			});
			btnUpload.setEnabled(false);
		}

		//Button to quit Plugin Manager
		btnOK = new JButton();
		btnOK.setText("Cancel");
		btnOK.setToolTipText("Exit Plugin Manager without applying changes");
		btnOK.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToQuitPluginManager();
			}

		});

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(btnStart);
		if (isDeveloper) {
			bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
			bottomPanel.add(btnUpload);
		}
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(btnOK);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(topPanel);
		getContentPane().add(bottomPanel);
	}

	//Whenever search text or ComboBox has been changed
	private void changeListingListener() {
		int selectedIndex = comboBoxViewingOptions.getSelectedIndex();

		if (txtSearch.getText().trim().isEmpty()) {
			viewList = pluginListBuilder.getExistingPluginList();
		} else {
			viewList = ((PluginCollection)pluginListBuilder.getExistingPluginList()).getList(PluginCollection.getFilterForText(txtSearch.getText().trim()));
		}

		//if "View all plugins"
		if (selectedIndex == 0) {
			//do nothing
		}
		//if "View installed plugins"
		else if (selectedIndex == 1) {
			viewList = ((PluginCollection)viewList).getList(PluginCollection.FILTER_STATUS_ALREADYINSTALLED);
		}
		//if "View uninstalled plugins"
		else if (selectedIndex == 2) {
			viewList = ((PluginCollection)viewList).getList(PluginCollection.FILTER_STATUS_UNINSTALLED);
		}
		//if "View up-to-date plugins"
		else if (selectedIndex == 3) {
			viewList = ((PluginCollection)viewList).getList(PluginCollection.FILTER_STATUS_INSTALLED);
		}
		//if "View update-able plugins"
		else if (selectedIndex == 4) {
			viewList = ((PluginCollection)viewList).getList(PluginCollection.FILTER_STATUS_MAYUPDATE);
		} else {
			throw new Error("Viewing option specified does not exist!");
		}

		//Directly update the table for display
		table.setupTableModel(viewList);
	}

	private void clickToUploadRecords() {
		//After uploading, you might need to restart Fiji (?)
		System.out.println("TODO: Select plugins that are indicated to action UPLOAD. Then get their information... connect to server... Not sure how to implement yet.");
		//TODO: In the future, a UI might hold the below code in the form of
		//loadedFrame = new UploaderFrame(this);
		//...
		//uploaderFrame.setUploader(new Uploader(pluginDataReader));
		//inside of .setUploader()... start the actions (generateDocuments(), etc etc)
		Uploader uploader = new Uploader(pluginListBuilder);
		uploader.generateDocuments();
		uploader.uploadToServer();
	}

	private void clickToEditDescriptions() {
		loadedFrame = new DescriptionEditorUI(this, currentPlugin);
		loadedFrame.setVisible(true);
		setEnabled(false);
	}

	private void clickToBeginOperations() {
		loadedFrame = new ConfirmationUI(this);
		ConfirmationUI confirmationUI = (ConfirmationUI)loadedFrame;
		confirmationUI.displayInformation(new Controller(pluginListBuilder.getExistingPluginList()));
		loadedFrame.setVisible(true);
		setEnabled(false);
	}

	private void clickToQuitPluginManager() {
		//in later implementations, this should have some notifications
		dispose();
	}

	public void openDownloader() {
		backToPluginManager();
		loadedFrame = new DownloadUI(this);
		loadedFrame.setVisible(true);
		DownloadUI downloadUI = (DownloadUI)loadedFrame;
		downloadUI.setInstaller(new Installer(pluginListBuilder, fileURL));
		setEnabled(false);
	}

	public void backToPluginManager() {
		if (loadedFrame != null) {
			loadedFrame.setVisible(false);
			loadedFrame.dispose();
			loadedFrame = null;
		}
		setEnabled(true);
		setVisible(true);
	}

	public void displayPluginDetails(PluginObject currentPlugin) {
		this.currentPlugin = currentPlugin;

		try {
			TextPaneDisplay textPane = (TextPaneDisplay)txtPluginDetails;
			textPane.setText("");
			textPane.showPluginDetails(currentPlugin);
		} catch (BadLocationException e) {
			throw new Error("Problem with printing Plugin information: " + e.getMessage());
		}

		//if uploadable, then description is editable too
		if (isDeveloper) {
			if (currentPlugin.isUploadable())
				btnEditDescriptions.setEnabled(true);
			else
				btnEditDescriptions.setEnabled(false);
		}
	}

	public void tableChanged(TableModelEvent e) {
		//QUESTION: should we count objects in the view-table or objects in the entire list?
		int size = 0;
		int installCount = 0;
		int removeCount = 0;
		int updateCount = 0;

		size = viewList.size();
		for (PluginObject myPlugin : viewList) {
			if (myPlugin.toInstall()) {
				installCount += 1;
			} else if (myPlugin.toRemove()) {
				removeCount += 1;
			} else if (myPlugin.toUpdate()) {
				updateCount += 1;
			}
		}

		lblPluginSummary.setText("Total: " + size + ", To install: " + installCount +
				", To remove: " + removeCount + ", To update: " + updateCount);
		enablingButtonIfAnyActions(btnStart, PluginCollection.FILTER_ACTIONS_SPECIFIED_NOT_UPLOAD);
		enablingButtonIfAnyActions(btnUpload, PluginCollection.FILTER_ACTIONS_UPLOAD);
	}

	private void enablingButtonIfAnyActions(JButton button, PluginCollection.Filter filter) {
		PluginCollection pluginList = (PluginCollection)pluginListBuilder.getExistingPluginList();
		if (button != null) {
			List<PluginObject> myList = pluginList.getList(filter);
			if (myList.size() > 0)
				button.setEnabled(true);
			else
				button.setEnabled(false);
		}
	}

	public boolean isDeveloper() {
		return isDeveloper;
	}

	@Override
	public void run(String arg0) {
		// TODO Auto-generated method stub
		
	}
}