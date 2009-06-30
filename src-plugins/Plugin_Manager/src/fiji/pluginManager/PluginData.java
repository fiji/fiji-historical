package fiji.pluginManager;

import ij.Menus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class PluginData {
	private final String macPrefix = "Contents/MacOS/";
	private boolean useMacPrefix;
	private String fijiPath;
	private String platform;
	private boolean forServer;
	private final char[] hex = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	public PluginData() {
		initialize(false);
	}

	public PluginData(boolean forServer) {
		initialize(forServer);
	}

	private void initialize(boolean forServer) {
		this.forServer = forServer;
		fijiPath = (forServer ? getDefaultFijiPath() :
			stripSuffix(stripSuffix(Menus.getPlugInsPath(), File.separator),
		"plugins"));
		//fijiPath = (fijiPath == null ? getDefaultFijiPath() : fijiPath);

		//gets the platform string value
		boolean is64bit = System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux")) {
			platform = "linux" + (is64bit ? "64" : "");
		} else if (osName.equals("Mac OS X")) {
			platform = "macosx";
		} else if (osName.startsWith("Windows")) {
			platform = "win" + (is64bit ? "64" : "32") + ".exe";
		} else {
			System.err.println("Unknown platform: " + osName);
			platform = osName;
		}

		//useMacPrefix initially is false, set to true if macLauncher exist
		useMacPrefix = false;
		String macLauncher = macPrefix + "fiji-macosx";
		if (platform.equals("macosx") && new File(prefix(macLauncher)).exists())
			useMacPrefix = true;
	}

	protected String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	protected String getDefaultFijiPath() {
		String name = "/PluginManager.class";
		URL url = getClass().getResource(name);
		String path = URLDecoder.decode(url.toString());
		path = path.substring(0, path.length() - name.length());
		if (path.startsWith("jar:") && path.endsWith("!"))
			path = path.substring(4, path.length() - 5);
		if (path.startsWith("file:")) {
			path = path.substring(5);
			if (File.separator.equals("\\") && path.startsWith("/"))
				path = path.substring(1);
		}
		int slash = path.lastIndexOf('/');
		if (slash > 0) {
			slash = path.lastIndexOf('/', slash - 1);
			if (slash > 0)
				path = path.substring(0, slash);
		}
		return path;
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
		if (path.endsWith(".jar"))
			return getJarDigest(fullPath);
		MessageDigest digest = getDigest();
		digest.update(path.getBytes("ASCII"));
		updateDigest(new FileInputStream(fullPath), digest);
		return toHex(digest.digest());
	}

	private String getJarDigest(String path)
			throws FileNotFoundException, IOException {
		MessageDigest digest = null;
		try {
			digest = getDigest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		JarFile jar = new JarFile(path);
		List list = new ArrayList();
		Enumeration entries = jar.entries();
		while (entries.hasMoreElements())
			list.add(entries.nextElement());
		Collections.sort(list, new JarEntryComparator());

		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			JarEntry entry = (JarEntry)iter.next();
			digest.update(entry.getName().getBytes("ASCII"));
			updateDigest(jar.getInputStream(entry), digest);
		}
		return toHex(digest.digest());
	}

	private MessageDigest getDigest()
			throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-1");
	}

	private void updateDigest(InputStream input, MessageDigest digest)
			throws IOException {
		byte[] buffer = new byte[65536];
		DigestInputStream digestStream =
			new DigestInputStream(input, digest);
		while (digestStream.read(buffer) >= 0)
			; /* do nothing */
		digestStream.close();
	}

	private String toHex(byte[] bytes) {
		char[] buffer = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			buffer[i * 2] = hex[(bytes[i] & 0xf0) >> 4];
			buffer[i * 2 + 1] = hex[bytes[i] & 0xf];
		}
		return new String(buffer);
	}

	//Gets the location of specified file when inside of saveDirectory
	protected String getSaveToLocation(String saveDirectory, String filename) {
		String savePath = prefix(saveDirectory + File.separator + filename);
		if (filename.startsWith("fiji-")) {
			boolean useMacPrefix = getUseMacPrefix();
			String macPrefix = getMacPrefix();
			savePath = prefix((useMacPrefix ? macPrefix : "") + filename);
		}
		return savePath;
	}

	protected String getTimestampFromFile(String filename) {
		String fullPath = prefix(filename);
		long modified = new File(fullPath).lastModified();
		return timestamp(modified);
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
			if (e instanceof FileNotFoundException && filename.startsWith("fiji-"))
				return null;
			throw new Error("Could not get digest: " + prefix(filename) + " (" + e + ")");
		}
	}

	protected String prefix(String path) {
		return fijiPath + File.separator
			+ (forServer && path.startsWith("fiji-") ?
					"precompiled/" : "")
			+ path;
	}

	protected String initializeFilename(String filename) {
		if (getUseMacPrefix() && filename.startsWith(getMacPrefix()))
			filename = filename.substring(getMacPrefix().length());
		if (File.separator.equals("\\"))
			filename = filename.replace("\\", "/");
		return filename;
	}

	private String timestamp(long millis) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(millis);
		return timestamp(date);
	}

	private String timestamp(Calendar date) {
		DecimalFormat format = new DecimalFormat("00");
		int month = date.get(Calendar.MONTH) + 1;
		int day = date.get(Calendar.DAY_OF_MONTH);
		int hour = date.get(Calendar.HOUR_OF_DAY);
		int minute = date.get(Calendar.MINUTE);
		int second = date.get(Calendar.SECOND);
		return "" + date.get(Calendar.YEAR) +
			format.format(month) + format.format(day) +
			format.format(hour) + format.format(minute) +
			format.format(second);
	}

	private class JarEntryComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			String name1 = ((JarEntry)o1).getName();
			String name2 = ((JarEntry)o2).getName();
			return name1.compareTo(name2);
		}

		public boolean equals(Object o1, Object o2) {
			String name1 = ((JarEntry)o1).getName();
			String name2 = ((JarEntry)o2).getName();
			return name1.equals(name2);
		}
	}

}
