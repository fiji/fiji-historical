package fiji.pluginManager.ui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import fiji.pluginManager.logic.PluginObject;

public class DetailsEditor extends JFrame {
	private DocumentListener changeListener;
	private MainUserInterface mainUserInterface;
	private PluginObject selectedPlugin;
	private JTextPane[] txtEdits; //0: Authors, 1: Description, 2: Links
	private JButton btnSave;
	private JButton btnCancel;
	private boolean textChanged;

	public DetailsEditor(MainUserInterface mainUserInterface, PluginObject selectedPlugin) {
		super("Description Editor: " + selectedPlugin.getFilename());
		this.mainUserInterface = mainUserInterface;
		this.selectedPlugin = selectedPlugin;
		setUpUserInterface();
		pack();
	}

	private void setUpUserInterface() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		changeListener = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				textChanged = true;
			}

			public void insertUpdate(DocumentEvent e) {
				textChanged = true;
			}

			public void removeUpdate(DocumentEvent e) {
				textChanged = true;
			}
		};

		txtEdits = new JTextPane[3];
		for (int i = 0; i < txtEdits.length; i++)
			txtEdits[i] = new JTextPane();

		JPanel panelTitle = SwingTools.createLabelPanel(
				"For multiple authors or links, separate each using a new line.");
		JPanel panelAuthors = SwingTools.createLabelPanel("Authors(s):");
		JScrollPane authScrollpane = getTextScrollPane(txtEdits[0], 450, 120,
				selectedPlugin.getPluginDetails().getAuthors());

		JPanel panelDescription = SwingTools.createLabelPanel("Description:");
		JScrollPane descScrollpane = getTextScrollPane(txtEdits[1], 450, 200,
				selectedPlugin.getPluginDetails().getDescription());

		JPanel panelLinks = SwingTools.createLabelPanel("Link(s):");
		JScrollPane linkScrollpane = getTextScrollPane(txtEdits[2], 450, 120,
				selectedPlugin.getPluginDetails().getLinks());

		textChanged = false;

		//Button to save description
		btnSave = SwingTools.createButton("Save", "Save Description");
		btnSave.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				saveText();
			}
		});

		//Button to cancel and return to Plugin Manager
		btnCancel = SwingTools.createButton("Close", "Exit Description Editor");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				backToPluginManager();
			}
		});

		JPanel buttonPanel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		buttonPanel.add(btnSave);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnCancel);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

		JPanel uiPanel = SwingTools.createBoxLayoutPanel(BoxLayout.Y_AXIS);
		uiPanel.add(panelTitle);
		uiPanel.add(panelAuthors);
		uiPanel.add(authScrollpane);
		uiPanel.add(panelDescription);
		uiPanel.add(descScrollpane);
		uiPanel.add(panelLinks);
		uiPanel.add(linkScrollpane);
		uiPanel.add(buttonPanel);
		uiPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		getContentPane().add(uiPanel);
	}

	private JScrollPane getTextScrollPane(JTextPane textPane, int width, int height, String contents) {
		textPane.getDocument().addDocumentListener(changeListener);
		JScrollPane scrollpane = SwingTools.getTextScrollPane(textPane, width, height);
		if (contents != null)
			textPane.setText(contents);
		textPane.setSelectionStart(0);
		textPane.setSelectionEnd(0);
		return scrollpane;
	}

	private JScrollPane getTextScrollPane(JTextPane textPane, int width, int height, List<String> contentList) {
		JScrollPane scrollpane = SwingTools.getTextScrollPane(textPane, width, height);
		if (contentList != null && contentList.size() > 0) {
			String contents = "";
			for (String value : contentList)
				contents += value + "\n";
			textPane.setText(contents);
			textPane.setSelectionStart(0);
			textPane.setSelectionEnd(0);
		}
		return scrollpane;
	}

	private void backToPluginManager() {
		if (textChanged) {
			int option = JOptionPane.showConfirmDialog(this,
					"Description has changed.\n\nSave it before exiting Editor?",
					"Save?",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);
			if (option == JOptionPane.CANCEL_OPTION) {
				return;
			} else if (option == JOptionPane.YES_OPTION) {
				saveText();
			} //else ("No"), just exit
		}
		mainUserInterface.backToPluginManager();
	}

	private void saveText() {
		selectedPlugin.getPluginDetails().setDescription(txtEdits[1].getText().trim());
		selectedPlugin.getPluginDetails().setLinks(txtEdits[2].getText().trim().split("\n"));
		selectedPlugin.getPluginDetails().setAuthors(txtEdits[0].getText().trim().split("\n"));
		mainUserInterface.displayPluginDetails(selectedPlugin);
		textChanged = false;
	}
}
