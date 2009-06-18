package fiji.pluginManager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/*
 * This class' role is to be in charge of how the Table should be displayed
 */
public class PluginTable extends JTable {
	private PluginTableModel pluginTableModel;
	private PluginManager pluginManager;
	private boolean isDeveloper;

	static String[] arrUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrInstalledOptions = { "Installed", "Remove it" };
	static String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };
	static String[] arrDevelUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrDevelInstalledOptions = { "Installed", "Remove it", "Upload" };
	static String[] arrDevelUpdateableOptions = { "Installed", "Remove it", "Update it", "Upload" };
	private TableCellEditor uninstalledOptions = new DefaultCellEditor(new JComboBox(arrUninstalledOptions));
	private TableCellEditor installedOptions = new DefaultCellEditor(new JComboBox(arrInstalledOptions));
	private TableCellEditor updateableOptions = new DefaultCellEditor(new JComboBox(arrUpdateableOptions));
	private TableCellEditor develUninstalledOptions = new DefaultCellEditor(new JComboBox(arrDevelUninstalledOptions));
	private TableCellEditor develInstalledOptions = new DefaultCellEditor(new JComboBox(arrDevelInstalledOptions));
	private TableCellEditor develUpdateableOptions = new DefaultCellEditor(new JComboBox(arrDevelUpdateableOptions));

	//NOTE: To be created only after related display components are created
	//Namely: lblPluginSummary, txtPluginDetails
	public PluginTable(List<PluginObject> pluginList, PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		isDeveloper = pluginManager.isDeveloper();
		setupTable(pluginList);
	}

	public void setupTable(List<PluginObject> pluginList) {
		//default display: All plugins shown
		setupTableModel(pluginList);
		
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			//Called when a row is selected
			public void valueChanged(ListSelectionEvent event) {
				int viewRow = getSelectedRow();
				if (viewRow < 0) {
					//Selection got filtered away
				} else {
					int modelRow = convertRowIndexToModel(viewRow);
					PluginObject myPlugin = pluginTableModel.getEntry(modelRow);
					displayPluginDetails(myPlugin);
				}
			}

		});

		//Set appearance of table
		setShowGrid(false);
		setIntercellSpacing(new Dimension(0,0));
		setAutoResizeMode(PluginTable.AUTO_RESIZE_ALL_COLUMNS);
		setRequestFocusEnabled(false);
		setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

			// method to over-ride - returns cell renderer component
			public Component getTableCellRendererComponent(JTable table, Object value, 
					boolean isSelected, boolean hasFocus, int row, int column) {

				// let the default renderer prepare the component for us
				Component comp = super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);
				int modelRow = table.convertRowIndexToModel(row);
				PluginObject myPlugin = pluginTableModel.getEntry(modelRow);

				if (!myPlugin.actionSpecified()) {
					//if there is no action
					comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
				} else {
					//if an action is specified by user, bold the field
					comp.setFont(comp.getFont().deriveFont(Font.BOLD));
				}

				return comp;
			}
		});

		//set up the table properties and other settings
		setCellSelectionEnabled(true);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		changeSelection(0, 0, false, false);
		requestFocusInWindow();
	}

	//Set up table model, to be called each time display list is to be changed
	public void setupTableModel(List<PluginObject> myList) {
		getModel().removeTableModelListener(pluginManager);
		setModel(pluginTableModel = new PluginTableModel(myList, isDeveloper));
		getModel().addTableModelListener(pluginManager); //listen for changes (tableChanged(TableModelEvent e))
		setColumnWidths(250, 120);
		pluginTableModel.fireTableChanged(new TableModelEvent(pluginTableModel));
	}

	private void setColumnWidths(int col1Width, int col2Width) {
		TableColumn col1 = getColumnModel().getColumn(0);
		TableColumn col2 = getColumnModel().getColumn(1);

		col1.setPreferredWidth(col1Width);
		col1.setMinWidth(col1Width);
		col1.setResizable(false);
		col2.setPreferredWidth(col2Width);
		col2.setMinWidth(col2Width);
		col2.setResizable(true);
	}

	public TableCellEditor getCellEditor(int row, int col) {
		PluginObject myPlugin = getPluginFromRow(row);

		//As we follow PluginTableModel, 1st column is filename
		if (col == 0) {
			return super.getCellEditor(row,col);
		} else if (col == 1) {
			if (isDeveloper) {
				if (myPlugin.isInstallable()) {
					return develUninstalledOptions;
				} else if (myPlugin.isRemovableOnly()) {
					if (myPlugin.isUploadable()) {
						return develInstalledOptions;
					} else {
						return installedOptions;
					}
				} else if (myPlugin.isUpdateable()) {
					if (myPlugin.isUploadable()) {
						//if timestamp is newer than the latest version, indicates local modification
						return develUpdateableOptions;
					} else {
						return updateableOptions;
					}
				}
			} else {
				if (myPlugin.isInstallable()) //if plugin is not installed
					return uninstalledOptions;
				else if (myPlugin.isRemovableOnly()) //if plugin is installed
					return installedOptions;
				else if (myPlugin.isUpdateable()) //if plugin is installed and may update
					return updateableOptions;
			}
			throw new Error("Error while assigning combo-boxes to data!");
		} else
			throw new Error("Unidentified Column number for Plugin Table");
	}

	public PluginObject getSelectedPlugin() {
		return getPluginFromRow(getSelectedRow());
	}

	public PluginObject getPluginFromRow(int viewRow) {
		int modelRow = convertRowIndexToModel(viewRow);
		return pluginTableModel.getEntry(modelRow);
	}

	private void displayPluginDetails(PluginObject myPlugin) {
		pluginManager.displayPluginDetails(myPlugin);
	}

	class PluginTableModel extends AbstractTableModel {
		private List<PluginObject> entries;
		private boolean isDeveloper;

		public PluginTableModel(List<PluginObject> myList, boolean isDeveloper) {
			super();
			this.isDeveloper = isDeveloper;
			entries = myList;
		}

		public int getColumnCount() {
			return 2; //Name of plugin, status
		}

		public Class getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0: return String.class; //filename
				case 1: return String.class; //status/action
				default: return Object.class;
			}
		}

		public String getColumnName(int column) {
			switch (column) {
				case 0: return "Name";
				case 1: return "Status/Action";
				default: throw new Error("Column out of range");
			}
		}

		public Iterator<PluginObject> getEntries() {
			return entries.iterator();
		}

		/*public boolean isMarked(int rowIndex) {
			PluginObject plugin = entries.get(rowIndex);
			return false;
		}*/

		public PluginObject getEntry(int rowIndex) {
			return entries.get(rowIndex);
		}

		public int getRowCount() {
			return entries.size();
		}

		private String[] getOptionsAccordingToState(PluginObject entry) {
			String[] optionsArray = null;
			if (isDeveloper) {
				if (entry.isInstallable()) {
					optionsArray = PluginTable.arrDevelUninstalledOptions;
				} else if (entry.isRemovableOnly()) {
					if (entry.isUploadable()) {
						optionsArray = PluginTable.arrDevelInstalledOptions;
					} else {
						optionsArray = PluginTable.arrInstalledOptions;
					}
				} else if (entry.isUpdateable()) {
					if (entry.isUploadable()) {
						//if timestamp is newer than the latest version, indicates local modification
						optionsArray = PluginTable.arrDevelUpdateableOptions;
					} else {
						optionsArray = PluginTable.arrUpdateableOptions;
					}
				}
			} else {
				if (entry.isInstallable()) {
					optionsArray = PluginTable.arrUninstalledOptions;
				} else if (entry.isRemovableOnly()) {
					optionsArray = PluginTable.arrInstalledOptions;
				} else if (entry.isUpdateable()) {
					optionsArray = PluginTable.arrUpdateableOptions;
				}
			}
			if (optionsArray == null)
				throw new Error("Failed to get available display options for Plugin's ComboBoxes.");
			else
				return optionsArray;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			PluginObject entry = (PluginObject)entries.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return entry.getFilename();
				case 1:
					return getValue(entry);
				default:
					throw new Error("Column out of range");
			}
		}
		
		private String getValue(PluginObject entry) {
			String[] optionsArray = getOptionsAccordingToState(entry);
			
			if (entry.isInstallable()) { //if not installed
				if (!entry.actionSpecified()) {
					return optionsArray[0]; //"Not installed"
				} else if (entry.toInstall()) {
					return optionsArray[1]; //"Install"
				} else {
					throw new Error("INVALID action value for Uninstalled Plugin");
				}
			} else if (entry.isRemovableOnly()) { //if installed and no updates
				if (!entry.actionSpecified()) {
					return optionsArray[0]; //"Installed"
				} else if (entry.toRemove()) {
					return optionsArray[1]; //"Remove"
				} else if (entry.toUpload()) {
					return optionsArray[2]; //"Upload"
				} else {
					throw new Error("INVALID action value for Installed Plugin");
				}
			} else if (entry.isUpdateable()) { //if installed and update-able
				if (!entry.actionSpecified()) {
					return optionsArray[0]; //"Installed"
				} else if (entry.toRemove()) {
					return optionsArray[1]; //"Remove"
				} else if (entry.toUpdate()) {
					return optionsArray[2]; //"Update"
				} else if (entry.toUpload()) {
					return optionsArray[3]; //"Upload"
				} else {
					throw new Error("INVALID action value for Update-able Plugin");
				}
			} else {
				throw new Error("...");
			}
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			PluginObject entry = (PluginObject)entries.get(rowIndex);
			if (columnIndex == 1) {
				String newValue = (String)value;
				setValue(newValue, entry);
				fireTableChanged(new TableModelEvent(this));
			}
		}

		private void setValue(String newValue, PluginObject entry) {
			//if current status of selected plugin is "not installed"
			String[] optionsArray = getOptionsAccordingToState(entry);
			if (entry.isInstallable()) {
				//if option chosen is "Not installed"
				if (newValue.equals(optionsArray[0])) {
					entry.setActionNone();
				}
				//if option chosen is "Install"
				else if (newValue.equals(optionsArray[1])) {
					entry.setActionToInstall();
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + (entry.isInstallable() ? "Installable" : "NOT Installable"));
				}
			} else if (entry.isRemovableOnly()) {
				//if option chosen is "Installed"
				if (newValue.equals(optionsArray[0])) {
					entry.setActionNone();
				}
				//if option chosen is "Remove" (Or uninstall it)
				else if (newValue.equals(optionsArray[1])) {
					entry.setActionToRemove();
				}
				//if option chosen is "Upload"
				else if (isDeveloper && newValue.equals(optionsArray[2])) {
					entry.setActionToUpload();
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + (entry.isRemovableOnly() ? "Uninstallable" : "NOT Uninstallable"));
				}
			} else if (entry.isUpdateable()) {
				//if option chosen is "Installed"
				if (newValue.equals(optionsArray[0])) {
					entry.setActionNone();
				}
				//if option chosen is "Remove" (Or uninstall it)
				else if (newValue.equals(optionsArray[1])) {
					entry.setActionToRemove();
				}
				//if option chosen is "Update"
				else if (newValue.equals(optionsArray[2])) {
					entry.setActionToUpdate();
				}
				//if option chosen is "Upload"
				else if (isDeveloper && newValue.equals(optionsArray[3])) {
					entry.setActionToUpload();
				}
				//otherwise...
				else {
					throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + (entry.isUpdateable() ? "Updateable" : "NOT Updateable"));
				}
			} else {
				throw new Error("Invalid status specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
			}
		}

		public void setSortType(int type) {
		}

		public void sort(int type) {
		}
	}
}