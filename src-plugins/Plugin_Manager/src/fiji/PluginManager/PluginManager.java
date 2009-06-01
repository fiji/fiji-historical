package fiji.PluginManager;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;


public class PluginManager extends JFrame implements PlugIn, ActionListener, TableModelListener {
	private Controller controller;
	private PluginDataReader pluginDataReader;
	private Installer installer;
	private String updateURL = "http://pacific.mpi-cbg.de/update/current.txt";
	//private String ... //current.txt and database.txt address tentatively......

	/* User Interface elements */
	private DownloadUI frameDownloader;
	private String[] arrViewingOptions = {
			"View all plugins",
			"View installed plugins only",
			"View uninstalled plugins only",
			"View up-to-date plugins only",
			"View update-able plugins only"
			};
	private JTextField txtSearch;
	private JComboBox comboBoxViewingOptions;
	private PluginTableModel pluginTableModel;
	private PluginTable table;
	private JLabel lblPluginSummary;
	private JTextPane txtPluginDetails;
	private JButton btnStart;
	private JButton btnOK;

	public PluginManager() {
		super("Plugin Manager");
		setSize(750,540);

		//Firstly, get information from local, existing plugins
		pluginDataReader = new PluginDataReader();
		//Get information from server to build on information
		try {
			pluginDataReader.buildFullPluginList(new URL(updateURL));
		} catch (MalformedURLException e) {
			throw new Error(updateURL + " specifies an unknown protocol.");
		}

		//initialize the data...
		controller = new Controller(pluginDataReader.getExistingPluginList());

		setUpUserInterface();

		//Retrieves the data and displays them
		pluginTableModel.update(pluginDataReader.getExistingPluginList());

		setVisible(true);
	}

	private void setUpUserInterface() {
		/* Create text search */
		JLabel lblSearch1 = new JLabel("Search:", SwingConstants.LEFT);
		txtSearch = new JTextField();

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
				comboBoxViewListener();
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

		/* Create the plugin table */
		table = new PluginTable();
		setupTableModel(); //set pluginTableModel and table column widths
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent event) {
				int viewRow = table.getSelectedRow();
				if (viewRow < 0) {
					//Selection got filtered away
				} else {
					int modelRow = table.convertRowIndexToModel(viewRow);
					PluginObject myPlugin = pluginTableModel.getEntry(modelRow);
					txtPluginDetails.setText("");
					TextPaneFormat.insertText(txtPluginDetails, myPlugin.getFilename(), TextPaneFormat.BOLD_BLACK_TITLE);
					if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE)
						TextPaneFormat.insertText(txtPluginDetails, "\n(Update is available)", TextPaneFormat.ITALIC_BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "\n\nMd5 Sum", TextPaneFormat.BOLD_BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "\n" + myPlugin.getmd5Sum());
					TextPaneFormat.insertText(txtPluginDetails, "\n\nLast Modified: ", TextPaneFormat.BOLD_BLACK);
					TextPaneFormat.insertText(txtPluginDetails, "" + myPlugin.getTimestamp());
					TextPaneFormat.insertText(txtPluginDetails, "\n\nDependency", TextPaneFormat.BOLD_BLACK);
					ArrayList<Dependency> myDependencies = (ArrayList<Dependency>) myPlugin.getDependencies();
					String strDependencies = "";
					if (myDependencies != null) {
						int noOfDependencies = myDependencies.size();
						for (int i = 0; i < noOfDependencies; i++) {
							Dependency dependency = myDependencies.get(i);
							strDependencies +=  dependency.getFilename() + " (" + dependency.getTimestamp() + ")";
							if (i != noOfDependencies -1 && noOfDependencies != 1) //if last index
								strDependencies += ",\n";
						}
						if (strDependencies.equals("")) strDependencies = "None";
					} else {
						strDependencies = "None";
					}
					TextPaneFormat.insertText(txtPluginDetails, "\n" + strDependencies);
					TextPaneFormat.insertText(txtPluginDetails, "\n\nDescription", TextPaneFormat.BOLD_BLACK);
					String strDescription = "";
					if (myPlugin.getDescription() == null || myPlugin.getDescription().trim().equals("")) {
						strDescription = "None";
					} else
						strDescription = myPlugin.getDescription();
					TextPaneFormat.insertText(txtPluginDetails, "\n" + strDescription);
					if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) {
						TextPaneFormat.insertText(txtPluginDetails, "\n\nUpdate Details", TextPaneFormat.BOLD_BLACK_TITLE);
						TextPaneFormat.insertText(txtPluginDetails, "\n\nNew Md5 Sum", TextPaneFormat.BOLD_BLACK);
						TextPaneFormat.insertText(txtPluginDetails, "\n" + myPlugin.getNewMd5Sum());
						TextPaneFormat.insertText(txtPluginDetails, "\n\nReleased: ", TextPaneFormat.BOLD_BLACK);
						TextPaneFormat.insertText(txtPluginDetails, "" + myPlugin.getNewTimestamp());
					}
				}
			}

		});

		//Set appearance of table
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0,0));
		table.setAutoResizeMode(JTableX.AUTO_RESIZE_ALL_COLUMNS);
		table.setRequestFocusEnabled(false);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

			// method to over-ride - returns cell renderer component
			public Component getTableCellRendererComponent(JTable table, Object value, 
					boolean isSelected, boolean hasFocus, int row, int column) {

				// let the default renderer prepare the component for us
				Component comp = super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);
				int modelRow = table.convertRowIndexToModel(row);
				PluginObject myPlugin = pluginTableModel.getEntry(modelRow);

				if (myPlugin.getAction() == PluginObject.ACTION_NONE) {
					//if there is no action
					comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
				} else {
					//if an action is specified by user, bold the field
					comp.setFont(comp.getFont().deriveFont(Font.BOLD));
				}

				return comp;
			}
		});
		
		/* create the scrollpane that holds the table */
		JScrollPane pluginListScrollpane = new JScrollPane(table);
		pluginListScrollpane.getViewport().setBackground(table.getBackground());

		/* Label text for plugin summaries */
		lblPluginSummary = new JLabel();
		JPanel lblSummaryPanel = new JPanel();
		lblSummaryPanel.add(lblPluginSummary);
		lblSummaryPanel.add(Box.createHorizontalGlue());
		lblSummaryPanel.setLayout(new BoxLayout(lblSummaryPanel, BoxLayout.X_AXIS));

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

		/* Create textpane to hold the information */
		txtPluginDetails = new JTextPane();
		txtPluginDetails.setEditable(false);
		txtPluginDetails.setPreferredSize(new Dimension(335,315));

		/* Create scrollpane to hold the textpane */
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
		rightPanel.add(Box.createRigidArea(new Dimension(0,25)));

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(leftPanel);
		topPanel.add(Box.createRigidArea(new Dimension(15,0)));
		topPanel.add(rightPanel);
		topPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 5, 15));

		//Buttons to start actions
		btnStart = new JButton();
		btnStart.setText("Start");
		btnStart.setToolTipText("Start installing/uninstalling specified plugins");
		btnStart.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToBeginOperations();
			}

		});

		//Buttons to quit Plugin Manager
		btnOK = new JButton();
		btnOK.setText("OK");
		btnOK.setToolTipText("Exit Plugin Manager");
		btnOK.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				clickToQuitPluginManager();
			}

		});

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(btnStart);
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(btnOK);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(topPanel);
		getContentPane().add(bottomPanel);
	}

	//Set up the table model
	private void setupTableModel() {
		table.setModel(pluginTableModel = new PluginTableModel());
		table.getModel().addTableModelListener(this); //listen for changes (tableChanged(TableModelEvent e))
		TableColumn col1 = table.getColumnModel().getColumn(0);
		TableColumn col2 = table.getColumnModel().getColumn(1);

		//Minimum width of 370 (250 + 120)
		col1.setPreferredWidth(250);
		col1.setMinWidth(250);
		col1.setResizable(false);
		col2.setPreferredWidth(120);
		col2.setMinWidth(120);
		col2.setResizable(false);
	}

	//Whenever Viewing Options in the ComboBox has been changed
	private void comboBoxViewListener() {
		//Remove the old pluginList display, if any
		table.getModel().removeTableModelListener(this);
		//Preparing the new pluginList display
		setupTableModel();

		List<PluginObject> viewList = new ArrayList<PluginObject>();
		int selectedIndex = comboBoxViewingOptions.getSelectedIndex();

		if (selectedIndex == 0) {

			//if "View all plugins"
			viewList = pluginDataReader.getExistingPluginList();

		} else if (selectedIndex == 1) {

			//if "View installed plugins"
			viewList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getAlreadyInstalledList();

		} else if (selectedIndex == 2) {

			//if "View uninstalled plugins"
			viewList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getListWhereStatus(PluginObject.STATUS_UNINSTALLED);

		} else if (selectedIndex == 3) {

			//if "View up-to-date plugins"
			viewList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getListWhereStatus(PluginObject.STATUS_INSTALLED);

		} else if (selectedIndex == 4) {

			//if "View update-able plugins"
			viewList = ((PluginCollection)pluginDataReader.getExistingPluginList()).getListWhereStatus(PluginObject.STATUS_MAY_UPDATE);

		} else {
			throw new Error("Viewing option specified does not exist!");
		}

		//Directly update the table for display
		pluginTableModel.update(viewList);
	}

	private void clickToBeginOperations() {
		//in later implementations, this should liase with Controller
		//if status says there's a list to download...
		frameDownloader = new DownloadUI(this);
		//Installer installer = new Installer(((PluginCollection)pluginList).getListWhereActionIsSpecified(), updateURL);
		frameDownloader.setVisible(true);
		installer = new Installer(pluginDataReader.getPluginDataProcessor(),
				((PluginCollection)pluginDataReader.getExistingPluginList()).getListWhereActionIsSpecified(),
				updateURL);
		frameDownloader.setInstaller(installer);
		setEnabled(false);
	}

	private void clickToQuitPluginManager() {
		//in later implementations, this should have some notifications
		dispose();
	}

	public void clickBackToPluginManager() {
		//in later implementations, this should liase with Controller
		//e.g: controller.hasDownloadEnded()... Controller checks download complete or not...
		frameDownloader.setVisible(false);
		setEnabled(true);
	}

	public void actionPerformed(ActionEvent e) {
		
	}

	//When a value in the table has been modified by the user
	public void tableChanged(TableModelEvent e) {
		//QUESTION: should we count objects in the view-table or objects in the entire list?
		PluginTableModel model = (PluginTableModel)e.getSource();
		int size = model.getRowCount();
		int installCount = 0;
		int removeCount = 0;
		int updateCount = 0;
		for (int i = 0; i < size; i++) {
			PluginObject myPlugin = model.getEntry(i);
			if (myPlugin.getStatus() == PluginObject.STATUS_UNINSTALLED &&
				myPlugin.getAction() == PluginObject.ACTION_REVERSE) {
				installCount += 1;
			}
			else if ((myPlugin.getStatus() == PluginObject.STATUS_INSTALLED ||
				myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE) &&
				myPlugin.getAction() == PluginObject.ACTION_REVERSE) {
				removeCount += 1;
			} else if (myPlugin.getStatus() == PluginObject.STATUS_MAY_UPDATE &&
						myPlugin.getAction() == PluginObject.ACTION_UPDATE) {
				updateCount += 1;
			}
		}
		lblPluginSummary.setText("Total: " + size + ", To install: " + installCount +
				", To remove: " + removeCount + ", To update: " + updateCount);
	}

	/* Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = PluginManager.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	@Override
	public void run(String arg0) {
		// TODO Auto-generated method stub
		
	}
}

class TextPaneFormat {
	public static SimpleAttributeSet ITALIC_BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BOLD_BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BOLD_BLACK_TITLE = new SimpleAttributeSet();

	static {
		StyleConstants.setForeground(ITALIC_BLACK, Color.black);
		StyleConstants.setItalic(ITALIC_BLACK, true);
		StyleConstants.setFontFamily(ITALIC_BLACK, "Verdana");
		StyleConstants.setFontSize(ITALIC_BLACK, 12);

		StyleConstants.setForeground(BOLD_BLACK, Color.black);
		StyleConstants.setBold(BOLD_BLACK, true);
		StyleConstants.setFontFamily(BOLD_BLACK, "Verdana");
		StyleConstants.setFontSize(BOLD_BLACK, 12);

		StyleConstants.setForeground(BLACK, Color.black);
		StyleConstants.setFontFamily(BLACK, "Verdana");
		StyleConstants.setFontSize(BLACK, 12);

		StyleConstants.setForeground(BOLD_BLACK_TITLE, Color.black);
		//StyleConstants.setBold(BOLD_BLACK_TITLE, true);
		StyleConstants.setFontFamily(BOLD_BLACK_TITLE, "Impact");
		StyleConstants.setFontSize(BOLD_BLACK_TITLE, 18);
	}

	public static void insertText(JTextPane textPane, String text, AttributeSet set) {
		try {
			textPane.getDocument().insertString(textPane.getDocument().getLength(), text, set);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public static void insertText(JTextPane textPane, String text) {
		try {
			textPane.getDocument().insertString(textPane.getDocument().getLength(), text, TextPaneFormat.BLACK);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
}