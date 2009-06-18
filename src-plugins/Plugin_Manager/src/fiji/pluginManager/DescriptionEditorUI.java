package fiji.pluginManager;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DescriptionEditorUI extends JFrame {
	private PluginManager pluginManager;
	private PluginObject selectedPlugin;
	private JTextArea txtDescription;
	private JButton btnSave;
	private JButton btnCancel;
	private boolean textChanged;

	public DescriptionEditorUI(PluginManager pluginManager, PluginObject selectedPlugin) {
		super("Description Editor: " + selectedPlugin.getFilename());
		this.pluginManager = pluginManager;
		this.selectedPlugin = selectedPlugin;
		textChanged = false;
		setUpUserInterface();
		pack();
	}
	
	private void setUpUserInterface() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		txtDescription = new JTextArea();
		txtDescription.setPreferredSize(new Dimension(425, 200));
		txtDescription.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				textChanged = true;
			}

			public void removeUpdate(DocumentEvent e) {
				textChanged = true;
			}

			public void insertUpdate(DocumentEvent e) {
				textChanged = true;
			}

		});
		if (selectedPlugin.getDescription() != null)
			txtDescription.setText(selectedPlugin.getDescription());
		JScrollPane txtScrollpane = new JScrollPane(txtDescription);
		txtScrollpane.getViewport().setBackground(txtDescription.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(450, 200));

		//Button to save description
		btnSave = new JButton();
		btnSave.setText("Save");
		btnSave.setToolTipText("Save Description");
		btnSave.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				//
			}

		});

		//Button to cancel and return to Plugin Manager
		btnCancel = new JButton();
		btnCancel.setText("Cancel");
		btnCancel.setToolTipText("Exit Description Editor");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				//TODO: use the boolean value textChanged to verify first...
				backToPluginManager();
			}

		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(btnSave);
		buttonPanel.add(Box.createRigidArea(new Dimension(15, 0)));
		buttonPanel.add(btnCancel);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(txtScrollpane);
		getContentPane().add(buttonPanel);
	}

	private void backToPluginManager() {
		pluginManager.backToPluginManager();
	}
}
