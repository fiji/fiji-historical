package fiji.pluginManager;

public class DependencyAnalyzer {
	public DependencyAnalyzer() {
		System.out.println("DependencyAnalyzer CLASS: Started up");
	}

	//or should the argument be a list instead?
	public void generateDependencies(PluginObject plugin) {
		System.out.println("DependencyAnalyzer CLASS: " + plugin.getFilename() + ", TODO: Assign dependencies to PluginObject");
	}
}
