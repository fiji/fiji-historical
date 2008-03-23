import ij.IJ;
import ij.Menus;
import ij.plugin.PlugIn;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Enumeration;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Get_Class_Versions implements PlugIn {
	public void run(String args) {
		if (args == null || args.equals(""))
			args = Menus.getPlugInsPath();
		getClassVersions(args);
	}

	void getClassVersions(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			if (!path.endsWith(File.separator))
				path += File.separator;
			String[] list = file.list();
			for (int i = 0; i < list.length; i++)
				getClassVersions(path + list[i]);
		} else if (path.endsWith(".jar")) {
			try {
				ZipFile jarFile = new ZipFile(file);
				Enumeration list = jarFile.entries();
				while (list.hasMoreElements()) {
					ZipEntry entry =
						(ZipEntry)list.nextElement();
					String name = entry.getName();
					if (!name.endsWith(".class"))
						continue;
					getClassVersion(path
						+ "(" + name + ")",
						jarFile.getInputStream(entry));
				}
			} catch (Exception e) {
				System.err.println("Invalid jar file: '"
					+ path + "'");
			}
		} else if (path.endsWith(".class")) {
			try {
				getClassVersion(path,
					new FileInputStream(file));
			} catch (Exception e) {
				System.err.println("Could not open file: '"
					+ path + "'");
			}
		}
	}

	void getClassVersion(String path, InputStream stream)
			throws IOException {
		DataInputStream data = new DataInputStream(stream);

		if (data.readInt() != 0xcafebabe)
			System.err.println("Invalid class: " + path);
		else {
			int minor = data.readShort();
			int major = data.readShort();
			System.out.println("Version " + major + "." + minor
				+ ", class: '" + path + "'");
		}
		data.close();
	}
}
