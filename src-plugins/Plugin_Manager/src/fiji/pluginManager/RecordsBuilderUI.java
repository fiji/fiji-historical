package fiji.pluginManager;

import javax.swing.JFrame;

public class RecordsBuilderUI extends JFrame {
	private PluginManager pluginManager;

	public RecordsBuilderUI(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		setupUserInterface();
		//pack();
	}

	private void setupUserInterface() {
		setTitle("Build Plugin information");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}

	private void backToPluginManager() {
		pluginManager.fromRecordsBuilderToPluginManager();
	}

}
