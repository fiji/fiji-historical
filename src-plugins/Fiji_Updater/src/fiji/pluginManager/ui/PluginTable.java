package fiji.pluginManager.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import fiji.pluginManager.logic.PluginCollection;
import fiji.pluginManager.logic.PluginObject;

/*
 * This class' role is to be in charge of how the Table should be displayed
 */
public class PluginTable extends JTable {
	private PluginTableModel pluginTableModel;
	private MainUserInterface mainUserInterface;

	private static final String[] arrUninstalledOptions = { "Not installed", "Install it" };
	private static final String[] arrInstalledOptions = { "Installed", "Remove it" };
	private static final String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };
	private static final String[] arrDevelUninstalledOptions = { "Not installed", "Install it", "Upload" };
	private static final String[] arrDevelInstalledOptions = { "Installed", "Remove it", "Upload" };
	private static final String[] arrDevelUpdateableOptions = { "Installed", "Remove it", "Update it", "Upload" };

	public PluginTable(PluginCollection pluginList, MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
		setupTable(pluginList);
	}

	public void setupTable(PluginCollection pluginList) {
		//default display: All plugins shown
		setupTableModel(pluginList);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			//Called when a row is selected
			public void valueChanged(ListSelectionEvent event) {
				int viewRow = getSelectedRow();
				if (viewRow >= 0) {
					int modelRow = convertRowIndexToModel(viewRow);
					PluginObject plugin = pluginTableModel.getEntry(modelRow);
					mainUserInterface.displayPluginDetails(plugin);
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
				PluginObject plugin = pluginTableModel.getEntry(modelRow);

				comp.setFont(comp.getFont().deriveFont(plugin.actionSpecified() ? Font.BOLD : Font.PLAIN));

				return comp;
			}
		});

		//set up the table properties and other settings
		setCellSelectionEnabled(true);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		requestFocusInWindow();
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

	//Set up table model, to be called each time display list is to be changed
	public void setupTableModel(PluginCollection plugins) {
		getModel().removeTableModelListener(this);
		getModel().removeTableModelListener(mainUserInterface);
		setModel(pluginTableModel = new PluginTableModel(plugins));
		getModel().addTableModelListener(this);
		getModel().addTableModelListener(mainUserInterface); //listen for changes (tableChanged(TableModelEvent e))
		setColumnWidths(250, 100);
		pluginTableModel.fireTableChanged(new TableModelEvent(pluginTableModel));
	}

	public TableCellEditor getCellEditor(int row, int col) {
		PluginObject plugin = getPluginFromRow(row);

		//As we follow PluginTableModel, 1st column is filename
		if (col == 0)
			return super.getCellEditor(row,col);
		String[] arrOptions = getOptions(plugin, mainUserInterface.isDeveloper());
		return new DefaultCellEditor(new JComboBox(arrOptions));
	}

	public static String[] getOptions(PluginObject plugin, boolean isDeveloper) {
		String[] optionsArray = null;
		if (plugin.isInstallable()) {
			if (isDeveloper)
				optionsArray = arrDevelUninstalledOptions;
			else
				optionsArray = arrUninstalledOptions;
		} else if (plugin.isRemovableOnly()) {
			if (isDeveloper)
				optionsArray = arrDevelInstalledOptions;
			else
				optionsArray = arrInstalledOptions;
		} else if (plugin.isUpdateable()) {
			if (isDeveloper)
				optionsArray = arrDevelUpdateableOptions;
			else
				optionsArray = arrUpdateableOptions;
		}
		if (optionsArray == null)
			throw new Error("Failed to get available display options for Plugin's ComboBoxes.");
		return optionsArray;
	}

	public PluginObject getPluginFromRow(int viewRow) {
		int modelRow = convertRowIndexToModel(viewRow);
		return pluginTableModel.getEntry(modelRow);
	}

	class PluginTableModel extends AbstractTableModel {
		private PluginCollection plugins;

		public PluginTableModel(PluginCollection plugins) {
			this.plugins = plugins;
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

		public PluginObject getEntry(int rowIndex) {
			return plugins.get(rowIndex);
		}

		public int getRowCount() {
			return plugins.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			PluginObject plugin = plugins.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return plugin.getFilename();
				case 1:
					return getValue(plugin);
			}
			throw new Error("Column out of range");
		}

		private String getValue(PluginObject entry) {
			String[] optionsArray = PluginTable.getOptions(entry, mainUserInterface.isDeveloper());

			if (entry.isInstallable()) { //if not installed
				if (!entry.actionSpecified()) {
					return optionsArray[0]; //"Not installed"
				} else if (entry.toInstall()) {
					return optionsArray[1]; //"Install"
				} else if (entry.toUpload()) {
					return optionsArray[2]; //"Upload"
				}
			} else if (entry.isRemovableOnly()) { //if installed and no updates
				if (!entry.actionSpecified()) {
					return optionsArray[0]; //"Installed"
				} else if (entry.toRemove()) {
					return optionsArray[1]; //"Remove"
				} else if (entry.toUpload()) {
					return optionsArray[2]; //"Upload"
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
				}
			}
			throw new Error("Invalid state or action for " + entry.getFilename());
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				setValue((String)value, plugins.get(rowIndex));
				fireTableChanged(new TableModelEvent(this));
			}
		}

		private void setValue(String value, PluginObject plugin) {
			String[] optionsArray = PluginTable.getOptions(plugin, mainUserInterface.isDeveloper());
			if (plugin.isInstallable()) {
				if (value.equals(optionsArray[0])) //status "Not installed"
					plugin.setActionNone();
				else if (value.equals(optionsArray[1])) //option "Install"
					plugin.setActionToInstall();
				else if (mainUserInterface.isDeveloper() && value.equals(optionsArray[2])) //option "Upload"
					plugin.setActionToUpload();
				else //otherwise...
					throw new Error("Invalid string value specified for " + plugin.getFilename() + "; String object: " + value + ", Plugin status: " + (plugin.isInstallable() ? "Installable" : "NOT Installable"));
			} else if (plugin.isRemovableOnly()) {
				if (value.equals(optionsArray[0])) //status "Installed"
					plugin.setActionNone();
				else if (value.equals(optionsArray[1])) //option "Remove/Uninstall"
					plugin.setActionToRemove();
				else if (mainUserInterface.isDeveloper() && value.equals(optionsArray[2])) //option "Upload"
					plugin.setActionToUpload();
				else //otherwise...
					throw new Error("Invalid string value specified for " + plugin.getFilename() + "; String object: " + value + ", Plugin status: " + (plugin.isRemovableOnly() ? "Uninstallable" : "NOT Uninstallable"));
			} else if (plugin.isUpdateable()) {
				if (value.equals(optionsArray[0])) //status "Installed"
					plugin.setActionNone();
				else if (value.equals(optionsArray[1])) //option "Remove/Uninstall"
					plugin.setActionToRemove();
				else if (value.equals(optionsArray[2])) //option "To Update"
					plugin.setActionToUpdate();
				else if (mainUserInterface.isDeveloper() && value.equals(optionsArray[3])) //option "Upload"
					plugin.setActionToUpload();
				else //otherwise
					throw new Error("Invalid string value specified for " + plugin.getFilename() + "; String object: " + value + ", Plugin status: " + (plugin.isUpdateable() ? "Updateable" : "NOT Updateable"));
			} else {
				throw new Error("Invalid status specified for " + plugin.getFilename() + "; String object: " + value + ", Plugin status: " + plugin.getStatus());
			}
		}

		public void setSortType(int type) {
		}

		public void sort(int type) {
		}
	}
}
