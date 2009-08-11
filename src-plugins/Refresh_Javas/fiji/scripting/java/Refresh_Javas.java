package fiji.scripting.java;

import common.RefreshScripts;

import ij.IJ;
import ij.ImagePlus;
import ij.Menus;

import ij.io.PluginClassLoader;

import ij.text.TextWindow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



/**
 * This plugin looks for Java sources in plugins/ and turns them into
 * transparently-compiling plugins.
 *
 * That means that whenever the .java file is newer than the .class file,
 * it is compiled before it is called.
 */
public class Refresh_Javas extends RefreshScripts {

	boolean ifDebug=false;

	public Refresh_Javas() {
		super();
	}

	public Refresh_Javas(boolean debug) {
		super();
		ifDebug=debug;
	}

	public void run(String arg) {
		setLanguageProperties(".java", "Java");
		setVerbose(false);
		super.run(arg);
	}

	/** Compile and run an ImageJ plugin */
	public void runScript(String path) {
		String c = path;
		if (c.endsWith(".java")) {
			c = c.substring(0, c.length() - 5);
			try {
				if (!upToDate(path, c + ".class") &&
						!compile(path,ifDebug))
					return;
			} catch(Exception e) {
				IJ.error("Could not invoke javac compiler for "
					+ path + ": " + e);
				return;
			}
		}
		String pluginsPath = Menus.getPlugInsPath();
		if (!pluginsPath.endsWith(File.separator))
			pluginsPath += File.separator;
		if (c.startsWith(pluginsPath)) {
			c = c.substring(pluginsPath.length());
			while (c.startsWith(File.separator))
				c = c.substring(1);
		}
		runPlugin(c.replace('/', '.'));
	}

	boolean upToDate(String source, String target) {
		File sourceFile = new File(source);
		File targetFile = new File(target);
		if (!targetFile.exists())
			return false;
		if (!sourceFile.exists())
			return true;
		return sourceFile.lastModified() < targetFile.lastModified();
	}

	static Method javac;

	public void setIfDebug(boolean option) {
		ifDebug=option;
	}

	boolean compile(String path) throws ClassNotFoundException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {
		return(compile(path,false));
	}

	boolean compile(String path,boolean debug) throws ClassNotFoundException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {
		String[] arguments = { path };
		String classPath = getPluginsClasspath();
		if (!classPath.equals(""))
			arguments = new String[] {
				"-classpath", classPath, path
			};
		if(debug) {
			String[] newArgs = new String[arguments.length + 1];
			newArgs[0] = "-g";
			System.arraycopy(arguments, 0, newArgs, 1,
					arguments.length);
			arguments = newArgs;
		}
		if (javac == null) {
			String className = "com.sun.tools.javac.Main";
			ClassLoader loader = getClass().getClassLoader();
			Class main = loader.loadClass(className);
			Class[] argsType = new Class[] {
				arguments.getClass(),
				PrintWriter.class
			};
			javac = main.getMethod("compile", argsType);
		}

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(buffer);
		Object result = javac.invoke(null,
			new Object[] { arguments, out });

		if (result.equals(new Integer(0)))
			return true;

		new TextWindow("Could not compile " + path,
				buffer.toString(), 640, 480);
		return false;
	}

	void runPlugin(String className) {
		new PlugInExecuter(className);
	}
}
