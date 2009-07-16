package fiji.pluginManager.logic;
import java.io.File;
import fiji.pluginManager.logic.FileUploader.SourceFile;

public class UpdateSource implements SourceFile {
	private File file;
	private String relativePath;
	private long filesize;
	private PluginObject plugin = null; //if null, it means not a plugin file

	UpdateSource(PluginObject plugin, String absolutePath) {
		if (plugin == null)
			throw new Error("UpdateSource constructor parameters cannot be null");
		this.plugin = plugin;
		relativePath = plugin.getFilename();
		file = new File(absolutePath);
		filesize = plugin.getFilesize();
	}

	public UpdateSource(File file, String relativePath) {
		if (file == null)
			throw new Error("UpdateSource constructor parameters cannot be null");
		this.file = file;
		this.relativePath = relativePath;
		filesize = file.length();
	}

	public PluginObject getPlugin() {
		return plugin;
	}

	public File getFile() {
		return file;
	}

	public long getFilesize() {
		return filesize;
	}

	public String getRelativePath() {
		return relativePath;
	}

}
