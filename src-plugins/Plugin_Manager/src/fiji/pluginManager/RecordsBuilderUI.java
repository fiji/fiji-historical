package fiji.pluginManager;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

public class RecordsBuilderUI extends JFrame {
	private PluginManager pluginManager;
	private JList listLocalPlugins;
	private JLabel lblFilename;
	private JLabel lblDigest; //not too sure if this is really needed
	private JLabel lblDate;
	private JLabel lblFilesize;
	private JList listDependencies;
	private JTextArea txtDescription;
	private JButton btnUpload;
	private JButton btnClose;

	public RecordsBuilderUI(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		setupUserInterface();
		pack();
	}

	private void setupUserInterface() {
		setTitle("Build Plugin information");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JLabel lblPluginList = new JLabel("Plugins on the local side");
		JPanel lblPanel = new JPanel();
		lblPanel.setLayout(new BoxLayout(lblPanel, BoxLayout.X_AXIS));
		lblPanel.add(lblPluginList);
		lblPanel.add(Box.createHorizontalGlue());

		listLocalPlugins = new JList();
		listLocalPlugins.setPreferredSize(new Dimension(270, 370));
		JScrollPane listScrollpane = new JScrollPane(listLocalPlugins);
		listScrollpane.getViewport().setBackground(listLocalPlugins.getBackground());
		listScrollpane.setPreferredSize(new Dimension(300, 370));

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(lblPanel);
		leftPanel.add(listScrollpane);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridBagLayout());
		GridBagConstraints c;

		JLabel lblTitle = new JLabel("Plugin Details");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 7;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(lblTitle, c);

		lblFilename = new JLabel("Filename:");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		c.ipady = 7;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(lblFilename, c);

		lblDigest = new JLabel("Md5 sum:");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 2;
		c.ipady = 7;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(lblDigest, c);

		lblDate = new JLabel("Timestamp:");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 3;
		c.ipady = 7;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(lblDate, c);

		lblFilesize = new JLabel("Filesize:");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 4;
		c.ipady = 7;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(lblFilesize, c);

		JLabel lblDependenciesTitle = new JLabel("Dependencies");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 5;
		c.ipady = 7;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(lblDependenciesTitle, c);

		listDependencies = new JList();
		listDependencies.setPreferredSize(new Dimension(400, 120));
		JScrollPane dependenciesScrollpane = new JScrollPane(listDependencies);
		dependenciesScrollpane.getViewport().setBackground(listDependencies.getBackground());
		dependenciesScrollpane.setPreferredSize(new Dimension(430, 120));
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 6;
		c.ipady = 7;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(dependenciesScrollpane, c);

		JLabel lblDescriptionTitle = new JLabel("Description:");
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 7;
		c.ipady = 15;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(lblDescriptionTitle, c);

		txtDescription = new JTextArea();
		txtDescription.setPreferredSize(new Dimension(400, 180));
		JScrollPane txtScrollpane = new JScrollPane(txtDescription);
		txtScrollpane.getViewport().setBackground(txtDescription.getBackground());
		txtScrollpane.setPreferredSize(new Dimension(430, 180));
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 8;
		c.ipady = 0;
		c.weightx = 0.5;
		c.weighty = 0.5;
		rightPanel.add(txtScrollpane, c);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(leftPanel);
		topPanel.add(Box.createRigidArea(new Dimension(15,0)));
		topPanel.add(rightPanel);
		topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		btnUpload = new JButton("Upload");
		btnUpload.setToolTipText("Do nothing as of now... Upload to server? Or what?");
		btnUpload.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				//nothing yet
			}

		});

		btnClose = new JButton("Close");
		btnClose.setToolTipText("Cancel and return to Plugin Manager");
		btnClose.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				backToPluginManager();
			}

		});

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(btnUpload);
		bottomPanel.add(Box.createRigidArea(new Dimension(15,0)));
		bottomPanel.add(btnClose);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(topPanel);
		getContentPane().add(bottomPanel);
	}

	private void backToPluginManager() {
		pluginManager.backToPluginManager();
	}

}
