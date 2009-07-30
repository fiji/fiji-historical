package fiji.pluginManager.utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/*
 * Class functionality:
 * Extend from it if you need to
 * - Calculate timestamps of files
 * - Calculate the Md5 sums of files
 * - Get the absolute path (prefix()) of Fiji main directory
 * - Copy a file over to a particular location
 * - Get details of the Operating System Fiji application is on
 */
public abstract class PluginData {
	private final String macPrefix = "Contents/MacOS/";
	private boolean useMacPrefix;
	private String fijiPath;
	private String platform;

	public PluginData() {
		fijiPath = UpdateFiji.getFijiRootPath();
		platform = UpdateFiji.getPlatform(); //gets the platform string value

		//useMacPrefix initially is false, set to true if macLauncher exist
		useMacPrefix = false;
		String macLauncher = macPrefix + "fiji-macosx";
		if (platform.equals("macosx") && new File(prefix(macLauncher)).exists())
			useMacPrefix = true;
	}

	protected String getMacPrefix() {
		return macPrefix;
	}

	protected String getPlatform() {
		return platform;
	}

	protected boolean getUseMacPrefix() {
		return useMacPrefix;
	}

	//get digest of the file as according to fullPath
	protected String getDigest(String path, String fullPath)
	throws NoSuchAlgorithmException, FileNotFoundException,
	IOException, UnsupportedEncodingException {
		return UpdateFiji.getDigest(path, fullPath);
	}

	//Gets the location of specified file when inside of saveDirectory
	protected String getSaveToLocation(String saveDirectory, String filename) {
		return prefix(saveDirectory + File.separator + filename);
	}

	protected String getTimestampFromFile(String filename) {
		String fullPath = prefix(filename);
		long modified = new File(fullPath).lastModified();
		return UpdateFiji.timestamp(modified);
	}

	protected int getFilesizeFromFile(String filename) {
		long filesize = new File(filename).length();
		int intValue = new Long(filesize).intValue();
		return intValue;
	}

	protected String getDigestFromFile(String filename) {
		try {
			String fullPath = prefix(filename);
			return getDigest(filename, fullPath);
		} catch (Exception e) {
			throw new Error("Could not get digest: " + prefix(filename) + " (" + e + ")");
		}
	}

	protected String prefix(String path) {
		return fijiPath + path;
	}

	protected String initializeFilename(String filename) {
		if (getUseMacPrefix() && filename.startsWith(getMacPrefix()))
			filename = filename.substring(getMacPrefix().length());
		if (File.separator.equals("\\"))
			filename = filename.replace("\\", "/");
		return filename;
	}

	protected boolean fileExists(String filename) {
		return new File(prefix(filename)).exists();
	}
}
