package fiji.pluginManager;

import ij.Menus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DependencyAnalyzer {
	private PluginDataProcessor pluginDataProcessor;

	public DependencyAnalyzer(PluginDataProcessor pluginDataProcessor) {
		System.out.println("DependencyAnalyzer CLASS: Started up");
		this.pluginDataProcessor = pluginDataProcessor;
		
		//temporay placement...
		/*String path = stripSuffix(stripSuffix(Menus.getPlugInsPath(),
				File.separator),
				"plugins");*/
		List result = new ArrayList();
		File cwd = new File(".");
		String filenameProcessed = pluginDataProcessor.prefix("plugins/Jython_Interpreter.jar");

		try {
			JarFile jarfile = new JarFile(filenameProcessed);
			Enumeration<JarEntry> myEnum = jarfile.entries();
			//For each file in the selected jar file
			while (myEnum.hasMoreElements()) {
				JarEntry file = myEnum.nextElement();

				//defunct?
				/*System.out.println("Inside of " + filenameProcessed + " is " + file.getName());
				String java = "jar:file:" + jarfile.getName() + "!/" + file.getName();
				System.out.println("readFile() of makePath(cwd,java) is " + makePath(cwd, java));
				byte[] buffer = readFile(makePath(cwd, java));
				if (buffer == null) {
					System.err.println("Warning: " + java + " does not exist.  Skipping...");
					return;
				}*/
				
				
				//Read only class files inside the selected jar file
				if (file.getName().endsWith(".class")) {
					//Analyze each class file for dependent classes
					byte[] buffer = readStream(jarfile.getInputStream(file));
					ByteCodeAnalyzer analyzer = new ByteCodeAnalyzer(buffer);
					String fullClass = analyzer.getPathForClass() + ".class";

					/*if (!java.endsWith(fullClass))
						throw new RuntimeException("Huh? " + fullClass + " is not a suffix of " + java);
					java = java.substring(0, java.length() - fullClass.length());*/

					//For each dependent class
					Iterator iter = analyzer.getClassNames();
					while (iter.hasNext()) {
						String className = (String)iter.next();
						String path = className + ".class";
						System.out.println("Analyzed: className=" + className + ",path=" + path);
						System.out.println("makePath(cwd,path): " + makePath(cwd,path));
						System.out.println("For " + className + ", " + (!result.contains(path) ? "Not in list yet" : "Already in list"));
						if (!result.contains(path)) {
							result.add(path);
							//java2classFiles(path, cwd, result, all);
						}
					}
				}
			}
			System.out.println("As final results... of result...");
			for (int i = 0; i < result.size(); i++) {
				String strName = (String)result.get(i);
				System.out.println((i+1) + " - " + strName);
			}
		} catch (IOException e) {
			System.out.println("Error occurred while doing that JAR file stuff..." + e.getLocalizedMessage());
		}
		/*System.out.println("Going to print out " + filenameProcessed + " dependencies");
		java2classFiles(filenameProcessed, cwd, result, all);
		for (int i = 0; i < result.size(); i++) {
			String strName = (String)result.get(i);
			System.out.println((i+1) + " - " + strName);
		}*/
	}

	private String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	//or should the argument be a list instead?
	public void generateDependencies(PluginObject plugin) {
		System.out.println("DependencyAnalyzer CLASS: " + plugin.getFilename() + ", TODO: Assign dependencies to PluginObject");
	}
	
	public static void main(String args[]) {
		
	}

	/*
	 * This function inspects a .class file for a given .java file,
	 * infers the package name and all used classes, and adds to "all"
	 * the class file names of those classes used that have been found
	 * in the same class path.
	 */
	protected static void java2classFiles(String java, File cwd,
			List result, Set all) {
		if (java.endsWith(".java"))
			java = java.substring(0, java.length() - 5) + ".class";
		else if (!java.endsWith(".class")) {
			if (!all.contains(java)) {
				result.add(java);
				all.add(java);
			}
			return;
		}
		System.out.println("readFile() of makePath(cwd,java) is " + makePath(cwd, java));
		byte[] buffer = readFile(makePath(cwd, java));
		if (buffer == null) {
			System.err.println("Warning: " + java
					+ " does not exist.  Skipping...");
			return;
		}
		ByteCodeAnalyzer analyzer = new ByteCodeAnalyzer(buffer);
		String fullClass = analyzer.getPathForClass() + ".class";
		if (!java.endsWith(fullClass))
			throw new RuntimeException("Huh? " + fullClass
					+ " is not a suffix of " + java);
		java = java.substring(0, java.length() - fullClass.length());
		Iterator iter = analyzer.getClassNames();
		while (iter.hasNext()) {
			String className = (String)iter.next();
			String path = java + className + ".class";
			System.out.println("Analyzed: className=" + className + ",path=" + path);
			System.out.println("makePath(cwd,path): " + makePath(cwd,path));
			System.out.println("For " + className + ", " + (!all.contains(path) ? "Not in list yet" : "Already in list"));
			if (new File(makePath(cwd, path)).exists() &&
					!all.contains(path)) {
				result.add(path);
				all.add(path);
				java2classFiles(path, cwd, result, all);
			}
		}
	}

	public static String getPlatform() {
		boolean is64bit =
			System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "64" : "");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32");
		System.err.println("Unknown platform: " + osName);
		return osName;
	}
	
	public static boolean isAbsolutePath(String path) {
		boolean isWindows = getPlatform().startsWith("win");
		//above line should be = pluginDataProcessor.getPlatform().startsWith("win");
		return (isWindows && path.length() > 1 && path.charAt(1) == ':')
			|| (!isWindows && path.startsWith("/"));
	}

	public static String makePath(File cwd, String path) {
		String prefix = "", suffix = "";
		if (path.startsWith("jar:file:")) {
			prefix = "jar:file:";
			int exclamation = path.indexOf('!');
			suffix = path.substring(exclamation);
			path = path.substring(prefix.length(), exclamation);
		}
		if (isAbsolutePath(path))
			return prefix + path + suffix;
		if (path.equals("."))
			return prefix + cwd.toString() + suffix;
		if (cwd.toString().equals("."))
			return prefix + (path.equals("") ? "." : path) + suffix;
		return prefix + new File(cwd, path).toString() + suffix;
	}

	static byte[] readFile(String fileName) {
		try {
			if (fileName.startsWith("jar:file:")) {
				URL url = new URL(fileName);
				return readStream(url.openStream());
			}
			File file = new File(fileName);
			if (!file.exists())
				return null;
			InputStream in = new FileInputStream(file);
			byte[] buffer = new byte[(int)file.length()];
			in.read(buffer);
			in.close();
			return buffer;
		} catch (Exception e) { System.out.println("Hi, its an exception."); return null; }
	}

	static byte[] readStream(InputStream input) throws IOException {
		byte[] buffer = new byte[1024];
		int offset = 0, len = 0;
		for (;;) {
			if (offset == buffer.length)
				buffer = realloc(buffer,
						2 * buffer.length);
			len = input.read(buffer, offset,
					buffer.length - offset);
			if (len < 0)
				return realloc(buffer, offset);
			offset += len;
		}
	}

	static byte[] realloc(byte[] buffer, int newLength) {
		if (newLength == buffer.length)
			return buffer;
		byte[] newBuffer = new byte[newLength];
		System.arraycopy(buffer, 0, newBuffer, 0,
				Math.min(newLength, buffer.length));
		return newBuffer;
	}

	static class ByteCodeAnalyzer {
		byte[] buffer;
		int[] poolOffsets;
		int endOffset;

		public ByteCodeAnalyzer(byte[] buffer) {
			this.buffer = buffer;
			if ((int)getU4(0) != 0xcafebabe)
				throw new RuntimeException("No class");
			getConstantPoolOffsets();
		}

		public String getPathForClass() {
			int thisOffset = dereferenceOffset(endOffset + 2);
			if (getU1(thisOffset) != 7)
				throw new RuntimeException("Parse error");
			return getString(dereferenceOffset(thisOffset + 1));
		}

		int dereferenceOffset(int offset) {
			int index = getU2(offset);
			return poolOffsets[index - 1];
		}

		void getConstantPoolOffsets() {
			int poolCount = getU2(8) - 1;
			poolOffsets = new int[poolCount];
			int offset = 10;
			for (int i = 0; i < poolCount; i++) {
				poolOffsets[i] = offset;
				int tag = getU1(offset);
				if (tag == 7 || tag == 8)
					offset += 3;
				else if (tag == 9 || tag == 10 || tag == 11 ||
						tag == 3 || tag == 4 ||
						tag == 12)
					offset += 5;
				else if (tag == 5 || tag == 6) {
					poolOffsets[++i] = offset;
					offset += 9;
				}
				else if (tag == 1)
					offset += 3 + getU2(offset + 1);
				else
					throw new RuntimeException("Unknown tag"
						+ " " + tag);
			}
			endOffset = offset;
		}

		class ClassNameIterator implements Iterator {
			int index;

			ClassNameIterator() {
				index = -1;
				findNext();
			}

			void findNext() {
				while (++index < poolOffsets.length)
					if (getU1(poolOffsets[index]) == 7)
						break;
			}

			public boolean hasNext() {
				return index < poolOffsets.length;
			}

			public Object next() {
				int offset = poolOffsets[index];
				findNext();
				return getString(dereferenceOffset(offset + 1));
			}

			public void remove()
					throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
		}

		public Iterator getClassNames() {
			return new ClassNameIterator();
		}

		public String toString() {
			String result = "";
			for (int i = 0; i < poolOffsets.length; i++) {
				int offset = poolOffsets[i];
				result += "index #" + i + ": "
					+ format(offset) + "\n";
				int tag = getU1(offset);
				if (tag == 5 || tag == 6)
					i++;
			}
			return result;
		}

		int getU1(int offset) {
			return buffer[offset] & 0xff;
		}

		int getU2(int offset) {
			return getU1(offset) << 8 | getU1(offset + 1);
		}

		long getU4(int offset) {
			return ((long)getU2(offset)) << 16 | getU2(offset + 2);
		}

		String getString(int offset) {
			try {
				return new String(buffer, offset + 3,
						getU2(offset + 1), "UTF-8");
			} catch (Exception e) { return ""; }
		}

		String format(int offset) {
			int tag = getU1(offset);
			int u2 = getU2(offset + 1);
			String result = "offset: " + offset + "(" + tag + "), ";
			if (tag == 7)
				return result + "class " + u2;
			if (tag == 9)
				return result + "field " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 10)
				return result + "method " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 11)
				return result + "interface method " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 8)
				return result + "string #" + u2;
			if (tag == 3)
				return result + "integer " + getU4(offset + 1);
			if (tag == 4)
				return result + "float " + getU4(offset + 1);
			if (tag == 12)
				return result + "name and type " + u2 + ", "
					+ getU2(offset + 3);
			if (tag == 5)
				return result + "long "
					+ getU4(offset + 1) + ", "
					+ getU4(offset + 5);
			if (tag == 6)
				return result + "double "
					+ getU4(offset + 1) + ", "
					+ getU4(offset + 5);
			if (tag == 1)
				return result + "utf8 " + u2
					+ " " + getString(offset);
			return result + "unknown";
		}
	}
}
