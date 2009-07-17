package fiji.pluginManager.logic;
import java.io.File;
import fiji.pluginManager.logic.FileUploader.SourceFile;

public class UpdateSource implements SourceFile {
	private String absolutePath;
	private String filenameToWrite;
	private String directory;
	private long filesize;
	private PluginObject plugin = null; //if null, it means not a plugin file

	UpdateSource(PluginObject plugin, String absolutePath) {
		if (plugin == null)
			throw new Error("UpdateSource constructor parameters cannot be null");
		this.plugin = plugin;
		this.absolutePath = absolutePath;
		filesize = plugin.getFilesize();
		getDirectoryAndFilename(plugin.getFilename());
		filenameToWrite += "-" + plugin.getTimestamp();
	}

	public UpdateSource(File file, String relativePath) {
		if (file == null)
			throw new Error("UpdateSource constructor parameters cannot be null");
		absolutePath = file.getAbsolutePath();
		filesize = file.length();
		getDirectoryAndFilename(relativePath);
	}

	//format relative path to get relative directories and filename separately
	private void getDirectoryAndFilename(String relativePath) {
		directory = relativePath.replace(File.separator, "/");
		if (directory.startsWith("/"))
			directory = directory.substring(1);
		if (directory.endsWith("/"))
			directory = directory.substring(0, directory.length()-1);
		if (directory.indexOf("/") != -1) { //remove the file
			filenameToWrite = directory.substring(directory.lastIndexOf("/") +1);
			directory = directory.substring(0, directory.lastIndexOf("/"));
		} else {
			filenameToWrite = directory;
			directory = "";
		}
	}

	public boolean isPlugin() {
		return (plugin != null);
	}
	
	public String getRelativePath() {
		if (isPlugin())
			return plugin.getFilename();
		else
			return directory + "/" + filenameToWrite;
	}

	public long getFilesize() {
		return filesize;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getDirectory() {
		return directory;
	}

	public String getFilenameToWrite() {
		return filenameToWrite;
	}
}