package fiji.pluginManager.logic;
import java.io.File;

import fiji.pluginManager.logic.FileUploader.SourceFile;

public class PluginSource implements SourceFile {
	private PluginObject plugin;

	public PluginSource(PluginObject plugin) {
		if (plugin == null)
			throw new Error("PluginSource constructor parameters cannot be null");
		this.plugin = plugin;
	}

	public File getFile() {
		return new File("");
	}

	public int getFilesize() {
		return plugin.getFilesize();
	}

	public String getRelativePath() {
		return plugin.getFilename();
	}

}
