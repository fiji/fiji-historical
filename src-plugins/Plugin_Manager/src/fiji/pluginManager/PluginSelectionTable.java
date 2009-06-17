package fiji.pluginManager;

import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

public class PluginSelectionTable extends JTable  {
	private RecordsBuilderUI recordsBuilderUI;
	private PluginSelectionTableModel pluginSelectionTableModel;

	public PluginSelectionTable(RecordsBuilderUI recordsBuilderUI) {
		this.recordsBuilderUI = recordsBuilderUI;
		
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			//Called when a row is selected
			public void valueChanged(ListSelectionEvent event) {
				int viewRow = getSelectedRow();
				if (viewRow < 0) {
					//Selection got filtered away
				} else {
					int modelRow = convertRowIndexToModel(viewRow);
					PluginObject myPlugin = pluginSelectionTableModel.getEntry(modelRow);
					displayPluginDetails(myPlugin);
				}
			}

		});
	}

	//Set up table model, to be called each time display list is to be changed
	public void setupTableModel(List<PluginObject> myList) {
		//getModel().removeTableModelListener(recordsBuilderUI);
		setModel(pluginSelectionTableModel = new PluginSelectionTableModel(myList));
		//getModel().addTableModelListener(recordsBuilderUI); //listen for changes (tableChanged(TableModelEvent e))
		TableColumn col1 = getColumnModel().getColumn(0);
		TableColumn col2 = getColumnModel().getColumn(1);

		//Minimum width of 300 (270 + 30)
		col1.setPreferredWidth(270);
		col1.setMinWidth(270);
		col1.setResizable(false);
		col2.setPreferredWidth(30);
		col2.setMinWidth(30);
		col2.setResizable(false);

		pluginSelectionTableModel.fireTableChanged(new TableModelEvent(pluginSelectionTableModel));
	}

	private void displayPluginDetails(PluginObject myPlugin) {
		//recordsBuilderUI.displayPluginDetails(myPlugin);
	}

	class PluginSelectionTableModel extends AbstractTableModel {
		private List<PluginObject> entries;
		private List<PluginObject> selected;

		public PluginSelectionTableModel(List<PluginObject> myList) {
			super();
			entries = myList;
		}

		@Override
		public int getColumnCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getRowCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		public PluginObject getEntry(int rowIndex) {
			//in the future, you might need to convert this to actual TABLE row index
			return (PluginObject)entries.get(rowIndex);
		}

		
	}
}
