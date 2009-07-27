package fiji.pluginManager.userInterface;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import fiji.pluginManager.logic.PluginObject;

public class FrameDetailsEditor extends JFrame {
	private DocumentListener changeListener;
	private MainUserInterface mainUserInterface;
	private PluginObject selectedPlugin;
	private JTextPane txtDescription;
	private JTextPane txtAuthors;
	private JTextPane txtLinks;
	private JButton btnSave;
	private JButton btnCancel;
	private boolean textChanged;

	public FrameDetailsEditor(MainUserInterface mainUserInterface, PluginObject selectedPlugin) {
		super("Description Editor: " + selectedPlugin.getFilename());
		this.mainUserInterface = mainUserInterface;
		this.selectedPlugin = selectedPlugin;
		setUpUserInterface();
		pack();
	}

	private void setUpUserInterface() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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

		JPanel panelTitle = getLabelPanel("For multiple authors or links, separate each using a new line.");
		JPanel panelAuthors = getLabelPanel("Authors(s):");
		txtAuthors = new JTextPane();
		JScrollPane authScrollpane = getTextScrollPane(txtAuthors, 450, 120,
				selectedPlugin.getPluginDetails().getAuthors());

		JPanel panelDescription = getLabelPanel("Description:");
		txtDescription = new JTextPane();
		JScrollPane descScrollpane = getTextScrollPane(txtDescription, 450, 200,
				selectedPlugin.getPluginDetails().getDescription());

		JPanel panelLinks = getLabelPanel("Link(s):");
		txtLinks = new JTextPane();
		JScrollPane linkScrollpane = getTextScrollPane(txtLinks, 450, 120,
				selectedPlugin.getPluginDetails().getLinks());

		textChanged = false;

		//Button to save description
		btnSave = new JButton();
		btnSave.setText("Save");
		btnSave.setToolTipText("Save Description");
		btnSave.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				saveText();
			}

		});

		//Button to cancel and return to Plugin Manager
		btnCancel = new JButton();
		btnCancel.setText("Close");
		btnCancel.setToolTipText("Exit Description Editor");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				backToPluginManager();
			}

		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(btnSave);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnCancel);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

		JPanel uiPanel = new JPanel();
		uiPanel.setLayout(new BoxLayout(uiPanel, BoxLayout.Y_AXIS));
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

	private JPanel getLabelPanel(String text) {
		JLabel label = new JLabel(text, SwingConstants.LEFT);
		JPanel lblPanel = new JPanel();
		lblPanel.setLayout(new BoxLayout(lblPanel, BoxLayout.X_AXIS));
		lblPanel.add(label);
		lblPanel.add(Box.createHorizontalGlue());
		return lblPanel;
	}

	private JScrollPane getTextScrollPane(JTextPane textPane, int width, int height, String contents) {
		JScrollPane scrollpane = getTextScrollPane(textPane, width, height);
		if (contents != null)
			textPane.setText(contents);
		textPane.setSelectionStart(0);
		textPane.setSelectionEnd(0);
		return scrollpane;
	}

	private JScrollPane getTextScrollPane(JTextPane textPane, int width, int height, List<String> contentList) {
		JScrollPane scrollpane = getTextScrollPane(textPane, width, height);
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

	private JScrollPane getTextScrollPane(JTextPane textPane, int width, int height) {
		textPane.setPreferredSize(new Dimension(width, height));
		textPane.getDocument().addDocumentListener(changeListener);

		JScrollPane scrollpane = new JScrollPane(textPane);
		scrollpane.getViewport().setBackground(textPane.getBackground());
		scrollpane.setPreferredSize(new Dimension(width, height));

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
		selectedPlugin.getPluginDetails().setDescription(txtDescription.getText().trim());
		selectedPlugin.getPluginDetails().setLinks(txtLinks.getText().trim().split("\n"));
		selectedPlugin.getPluginDetails().setAuthors(txtAuthors.getText().trim().split("\n"));
		mainUserInterface.displayPluginDetails(selectedPlugin);
		textChanged = false;
	}
}
