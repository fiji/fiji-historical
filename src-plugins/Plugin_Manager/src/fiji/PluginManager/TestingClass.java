package fiji.PluginManager;
import java.awt.Component;
import java.util.Enumeration;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class TestingClass extends JFrame {
	private JTable table = null;
	static String[] arrUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrInstalledOptions = { "Installed", "Remove it" };
	static String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };

	public TestingClass() {
		super("Testing purposes...");
		setSize(600,400);

		// Create columns names
		String columnNames[] = { "Column 1", "Column 2"};

		// Create some data
		String dataValues[][] = {
			{"12", "Not installed"},
			{"-123", "Not installed"},
			{"93", "Not installed"},
			{"279", "Not installed"},
			{"10", "Not installed"},
			{"77", "Not installed"}
		};

		// Create a new table instance
		table = new JTable( dataValues, columnNames );
		TableColumnModel columnModel = table.getColumnModel();
		TableColumn column = columnModel.getColumn(1);
		//column.setCellEditor(cellEditor)
		column.setCellEditor(new MyTableCellEditor(dataValues));
		

		getContentPane().add(table);
		setVisible(true);
	}
	public static void main(String args[]) {
		new TestingClass();
	}
}
class MyTableColumnModel extends DefaultTableColumnModel {
	TableColumn nameColumn;
	public MyTableColumnModel() {
		super();
		nameColumn = new TableColumn();
		this.addColumn(new TableColumn());
	}

	@Override
	public void addColumn(TableColumn column) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addColumnModelListener(TableColumnModelListener x) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TableColumn getColumn(int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getColumnIndex(Object columnIdentifier) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getColumnIndexAtX(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getColumnMargin() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getColumnSelectionAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Enumeration<TableColumn> getColumns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSelectedColumnCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getSelectedColumns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListSelectionModel getSelectionModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTotalColumnWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void moveColumn(int columnIndex, int newIndex) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeColumn(TableColumn column) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeColumnModelListener(TableColumnModelListener x) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColumnMargin(int newMargin) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColumnSelectionAllowed(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSelectionModel(ListSelectionModel newModel) {
		// TODO Auto-generated method stub
		
	}
}
class MyTableColumn extends TableColumn {
	static String[] arrUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrInstalledOptions = { "Installed", "Remove it" };
	static String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };
	static JComboBox uninstalledOptions = new JComboBox(arrUninstalledOptions);
	static JComboBox installedOptions = new JComboBox(arrInstalledOptions);
	static JComboBox updateableOptions = new JComboBox(arrUpdateableOptions);

	public MyTableColumn(String[][] data) {
		//initial settings, give correct text string rep. settings to the column
		this.getCellEditor();
	}
	public TableCellEditor getCellEditor(int row, int col) {
		return this.getCellEditor();
	}
}



class MyTableCellEditor extends DefaultCellEditor {
	String[][] data;
	int rowState[] = {0, 1, 0, 2, 2, 1}; //states of the plugin object, not shown

	public MyTableCellEditor(String[][] data) {
		super(uninstalledOptions);
		this.data = data;
	}

	static String[] arrUninstalledOptions = { "Not installed", "Install it" };
	static String[] arrInstalledOptions = { "Installed", "Remove it" };
	static String[] arrUpdateableOptions = { "Installed", "Remove it", "Update it" };
	static JComboBox uninstalledOptions = new JComboBox(arrUninstalledOptions);
	static JComboBox installedOptions = new JComboBox(arrInstalledOptions);
	static JComboBox updateableOptions = new JComboBox(arrUpdateableOptions);

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int col) {
		// TODO Auto-generated method stub
		if (col == 1) {
			if (data[row][1].equals("0")) {
				return uninstalledOptions;
			} else if (data[row][1].equals("1")) {
				return installedOptions;
			} else { //2
				return updateableOptions;
			}
		}
		return super.getTableCellEditorComponent(table, value, isSelected, row, col);
	}

}
