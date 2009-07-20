package fiji.pluginManager.logic;
import java.io.File;
import fiji.pluginManager.logic.FileUploader.SourceFile;

public class UpdateSource implements SourceFile {
	private String absolutePath;
	private String permissions;
	private String filenameToWrite;
	private String directory;
	private long filesize;

	public UpdateSource(String absolutePath, String relativePath, String permissions) {
		if (!isValidPermissions(permissions))
			throw new Error("Permissions settings for " + relativePath + " not valid.");
		this.permissions = permissions;
		this.absolutePath = absolutePath;
		filesize = new File(absolutePath).length();
		getDirectoryAndFilename(relativePath);
	}

	public UpdateSource(String absolutePath, PluginObject plugin, String permissions) {
		if (plugin == null)
			throw new Error("PluginObject parameter cannot be null!");
		if (!isValidPermissions(permissions))
			throw new Error("Permissions settings for " + plugin.getFilename() + " not valid.");
		this.permissions = permissions;
		this.absolutePath = absolutePath;
		filesize = new File(absolutePath).length();
		getDirectoryAndFilename(plugin.getFilename());
		filenameToWrite += "-" + plugin.getTimestamp();
	}

	private boolean isValidPermissions(String permissions) {
		if (permissions.startsWith("C") && permissions.length() > 1 &&
				isValidNumber(permissions.substring(1))) {
			return true;
		} else
			return false;
	}

	private boolean isValidNumber(String number) {
		if (number.startsWith("0") && number.length() == 4) {
			try {
				Integer.parseInt(number);
				return true;
			} catch (NumberFormatException e) { }
		}
		return false;
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

	public String getRelativePath() {
		if (directory.equals(""))
			return filenameToWrite;
		return directory + "/" + filenameToWrite;
	}

	//********** Implemented methods for SourceFile **********
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

	public String getPermissions() {
		return permissions;
	}
}