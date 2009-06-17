package fiji.pluginManager;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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

	private List<PluginObject> pluginRecords;

	public RecordsBuilderUI(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		setupUserInterface();
		pack();

		pluginRecords = new PluginCollection();
		Dependency dependencyA1 = new Dependency("PluginD.jar", "20090420190033");
		ArrayList<Dependency> Adependency = new ArrayList<Dependency>();
		Adependency.add(dependencyA1);
		PluginObject pluginA = new PluginObject("PluginA.jar", "65c3ecc1bbd7564f92545ffd2521f9d96509ca64", "20090429190842", null, Adependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyB1 = new Dependency("PluginD.jar", "20090420190033");
		Dependency dependencyB2 = new Dependency("PluginH.jar", "20090524666220");
		Dependency dependencyB3 = new Dependency("PluginC.jar", "20081011183621");
		ArrayList<Dependency> Bdependency = new ArrayList<Dependency>();
		Bdependency.add(dependencyB1);
		Bdependency.add(dependencyB2);
		Bdependency.add(dependencyB3);
		PluginObject pluginB = new PluginObject("PluginB.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", null, Bdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		PluginObject pluginC = new PluginObject("PluginC.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090425190854", null, null, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyD1 = new Dependency("PluginF.jar", "20090420191023");
		Dependency dependencyD3 = new Dependency("PluginE.jar", "20090311213621");
		ArrayList<Dependency> Ddependency = new ArrayList<Dependency>();
		Ddependency.add(dependencyD1);
		Ddependency.add(dependencyD3);
		PluginObject pluginD = new PluginObject("PluginD.jar", "61c3ecc1add7364f92545ffd2521e9d96508cb62", "20090420190033", null, Ddependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyE1 = new Dependency("PluginG.jar", "20090125190842");
		ArrayList<Dependency> Edependency = new ArrayList<Dependency>();
		Edependency.add(dependencyE1);
		PluginObject pluginE = new PluginObject("PluginE.jar", "8114fe93cbf7720c01c7ff97c28b007b79900dc7", "20090311213621", null, Edependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyF1 = new Dependency("PluginI.jar", "20090501190854");
		ArrayList<Dependency> Fdependency = new ArrayList<Dependency>();
		Fdependency.add(dependencyF1);
		PluginObject pluginF = new PluginObject("PluginF.jar", "1b992dbca07ef84020d44a980c7902ba6c82dfee", "20090420191023", null, Fdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);
		
		PluginObject pluginG = new PluginObject("PluginG.jar", "1a992dbc077ef84020d44a980c7992ba6c8edf3d", "20090415160854", null, null, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyH1 = new Dependency("PluginD.jar", "20090420190033");
		ArrayList<Dependency> HnewDependency = new ArrayList<Dependency>();
		HnewDependency.add(dependencyH1);
		PluginObject pluginH = new PluginObject("PluginH.jar", "33c88dc1fbd7564f92587ffdc521f9de6507ca65", "20081224666220", null, null, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyI1 = new Dependency("PluginF.jar", "20090420191023");
		Dependency dependencyI2 = new Dependency("PluginK.jar", "20081221866291");
		ArrayList<Dependency> Idependency = new ArrayList<Dependency>();
		Idependency.add(dependencyI1);
		Idependency.add(dependencyI2);
		PluginObject pluginI = new PluginObject("PluginI.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090429190854", null, Idependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyJ1 = new Dependency("PluginI.jar", "20090404090854");
		ArrayList<Dependency> Jdependency = new ArrayList<Dependency>();
		Jdependency.add(dependencyJ1);
		PluginObject pluginJ = new PluginObject("PluginJ.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20090521181954", null, Jdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		Dependency dependencyK1 = new Dependency("PluginJ.jar", "20090404090854");
		ArrayList<Dependency> Kdependency = new ArrayList<Dependency>();
		Kdependency.add(dependencyK1);
		PluginObject pluginK = new PluginObject("PluginK.jar", "9624fa93cbf7720c01c7ff97c28b00747b700de3", "20081221866291", null, Kdependency, PluginObject.STATUS_UNINSTALLED, PluginObject.ACTION_NONE);

		pluginRecords.add(pluginA);
		pluginRecords.add(pluginB);
		pluginRecords.add(pluginC);
		pluginRecords.add(pluginD);
		pluginRecords.add(pluginE);
		pluginRecords.add(pluginF);
		pluginRecords.add(pluginG);
		pluginRecords.add(pluginH);
		pluginRecords.add(pluginI);
		pluginRecords.add(pluginJ);
		pluginRecords.add(pluginK);

		DefaultListModel listModel = (DefaultListModel)listLocalPlugins.getModel();
		for (PluginObject plugin : pluginRecords) {
			listModel.addElement(plugin.getFilename());
		}
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
		listLocalPlugins.setModel(new DefaultListModel());
		listLocalPlugins.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listLocalPlugins.setPreferredSize(new Dimension(270, 370));
		JScrollPane listScrollpane = new JScrollPane(listLocalPlugins);
		listScrollpane.getViewport().setBackground(listLocalPlugins.getBackground());
		listScrollpane.setPreferredSize(new Dimension(300, 370));
		listLocalPlugins.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			//Called when a row is selected
			public void valueChanged(ListSelectionEvent event) {
				String selectedName = (String)listLocalPlugins.getSelectedValue();
				if (selectedName == null) {
					//Selection got filtered away
				} else {
					for (PluginObject plugin : pluginRecords) {
						if (plugin.getFilename().equals(selectedName)) {
							lblFilename.setText("Filename: " + plugin.getFilename());
							lblDigest.setText("Md5 sum: " + plugin.getmd5Sum());
							lblDate.setText("Timestamp: " + plugin.getTimestamp());
							lblFilesize.setText("Filesize: " + plugin.getFilesize());
							List<Dependency> dependencyList = plugin.getDependencies();
							DefaultListModel listModel = new DefaultListModel();
							if (dependencyList != null) {
								for (Dependency dependency : dependencyList) {
									listModel.addElement(dependency.getFilename() + "; " + dependency.getTimestamp());
								}
							} else if (dependencyList == null || dependencyList.size() == 0) {
								listModel.addElement("None.");
							}
							listDependencies.setModel(listModel);
							//ignore dependencies first
							if (plugin.getDescription() != null)
								txtDescription.setText(plugin.getDescription());
							break;
						}
					}
					//private JList listDependencies;
				}
			}

		});

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
		listDependencies.setPreferredSize(new Dimension(400, 80));
		JScrollPane dependenciesScrollpane = new JScrollPane(listDependencies);
		dependenciesScrollpane.getViewport().setBackground(listDependencies.getBackground());
		dependenciesScrollpane.setPreferredSize(new Dimension(430, 80));
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

		//Only editing of description is allowed, the rest are generated automatically
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
