package fiji.pluginManager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
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

	static String[] arrUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrInstalledOptions = { "Installed", "Remove it" };
	static String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };
	private TableCellEditor uninstalledOptions = new DefaultCellEditor(new JComboBox(arrUninstalledOptions));
	private TableCellEditor installedOptions = new DefaultCellEditor(new JComboBox(arrInstalledOptions));
	private TableCellEditor updateableOptions = new DefaultCellEditor(new JComboBox(arrUpdateableOptions));

	//NOTE: To be created only after related display components are created
	//Namely: lblPluginSummary, txtPluginDetails
	public PluginTable(List<PluginObject> pluginList, PluginManager pluginManager) {
		this.pluginManager = pluginManager;

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
		setModel(pluginTableModel = new PluginTableModel(myList));
		getModel().addTableModelListener(pluginManager); //listen for changes (tableChanged(TableModelEvent e))
		TableColumn col1 = getColumnModel().getColumn(0);
		TableColumn col2 = getColumnModel().getColumn(1);

		//Minimum width of 370 (250 + 120)
		col1.setPreferredWidth(250);
		col1.setMinWidth(250);
		col1.setResizable(false);
		col2.setPreferredWidth(120);
		col2.setMinWidth(120);
		col2.setResizable(false);

		pluginTableModel.fireTableChanged(new TableModelEvent(pluginTableModel));
	}

	public TableCellEditor getCellEditor(int row, int col) {
		PluginTableModel pluginTableModel = (PluginTableModel)this.getModel();
		PluginObject myPlugin = pluginTableModel.getEntry(row);

		//As we follow PluginTableModel, 1st column is filename
		if (col == 0) {
			return super.getCellEditor(row,col);
		} else if (col == 1) {
			if (myPlugin.isInstallable()) //if plugin is not installed
				return uninstalledOptions;
			else if (myPlugin.isRemovableOnly()) //if plugin is installed
				return installedOptions;
			else if (myPlugin.isUpdateable()) //if plugin is installed
				return updateableOptions;
			else
				throw new Error("Error while assigning combo-boxes to data!");
		} else
			throw new Error("Unidentified Column number for Plugin Table");
	}

	private void displayPluginDetails(PluginObject myPlugin) {
		pluginManager.displayPluginDetails(myPlugin);
	}

	class PluginTableModel extends AbstractTableModel {
		private List<PluginObject> entries;

		public PluginTableModel(List<PluginObject> myList) {
			super();
			//entries = new ArrayList<PluginObject>();
			entries = myList;
		}

		public int getColumnCount() {
			return 2; //Name of plugin, status
		}

		public Class getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0: return String.class; //filename
				case 1: return String.class; //status
				default: return Object.class;
			}
		}

		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "Name";
				case 1:
					return "Status/Action";
				default:
					throw new Error("Column out of range");
			}
		}

		public Iterator<PluginObject> getEntries() {
			return entries.iterator();
		}

		public PluginObject getEntry(int rowIndex) {
			//in the future, you might need to convert this to actual TABLE row index
			return (PluginObject)entries.get(rowIndex);
		}

		public int getRowCount() {
			return entries.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			PluginObject entry = (PluginObject)entries.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return entry.getFilename();
				case 1:
					if (entry.isInstallable()) { //if not installed
						if (!entry.actionSpecified()) {
							return PluginTable.arrUninstalledOptions[0]; //"Not installed"
						} else if (entry.toInstall()) {
							return PluginTable.arrUninstalledOptions[1]; //"Install"
						} else {
							throw new Error("INVALID action value for Uninstalled Plugin");
						}
					} else if (entry.isRemovableOnly()) { //if installed and no updates
						if (!entry.actionSpecified()) {
							return PluginTable.arrInstalledOptions[0]; //"Installed"
						} else if (entry.toRemove()) {
							return PluginTable.arrInstalledOptions[1]; //"Remove"
						} else {
							throw new Error("INVALID action value for Installed Plugin");
						}
					} else if (entry.isUpdateable()) { //if installed and update-able
						if (!entry.actionSpecified()) {
							return PluginTable.arrUpdateableOptions[0]; //"Installed"
						} else if (entry.toRemove()) {
							return PluginTable.arrUpdateableOptions[1]; //"Remove"
						} else if (entry.toUpdate()) {
							return PluginTable.arrUpdateableOptions[2]; //"Update"
						} else {
							throw new Error("INVALID action value for Update-able Plugin");
						}
					}
				default:
					throw new Error("Column out of range");
			}
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			PluginObject entry = (PluginObject)entries.get(rowIndex);
			if (columnIndex == 1) {
				String newValue = (String)value;
				//if current status of selected plugin is "not installed"
				if (entry.isInstallable()) {
					//if option chosen is "Not installed"
					if (newValue.equals(PluginTable.arrUninstalledOptions[0])) {
						entry.setActionNone();
					}
					//if option chosen is "Install"
					else if (newValue.equals(PluginTable.arrUninstalledOptions[1])) {
						entry.setActionToInstall();
					}
					//otherwise...
					else {
						throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + (entry.isInstallable() ? "Installable" : "NOT Installable"));
					}
				} else if (entry.isRemovableOnly()) {
					//if option chosen is "Installed"
					if (newValue.equals(PluginTable.arrInstalledOptions[0])) {
						entry.setActionNone();
					}
					//if option chosen is "Remove" (Or uninstall it)
					else if (newValue.equals(PluginTable.arrInstalledOptions[1])) {
						//TO IMPLEMENT: To check whether it is valid to remove w/o violating dependencies
						entry.setActionToRemove();
					}
					//otherwise...
					else {
						throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + (entry.isRemovableOnly() ? "Uninstallable" : "NOT Uninstallable"));
					}
				} else if (entry.isUpdateable()) {
					//if option chosen is "Installed"
					if (newValue.equals(PluginTable.arrUpdateableOptions[0])) {
						entry.setActionNone();
					}
					//if option chosen is "Remove" (Or uninstall it)
					else if (newValue.equals(PluginTable.arrUpdateableOptions[1])) {
						entry.setActionToRemove();
					}
					//if option chosen is "Update"
					else if (newValue.equals(PluginTable.arrUpdateableOptions[2])) {
						entry.setActionToUpdate();
					}
					//otherwise...
					else {
						throw new Error("Invalid string value specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + (entry.isUpdateable() ? "Updateable" : "NOT Updateable"));
					}
				} else {
					throw new Error("Invalid status specified for " + entry.getFilename() + "; String object: " + newValue + ", Plugin status: " + entry.getStatus());
				}
				fireTableChanged(new TableModelEvent(this));
			}
		}

		public void setSortType(int type) {
		}

		public void sort(int type) {
		}
	}
}