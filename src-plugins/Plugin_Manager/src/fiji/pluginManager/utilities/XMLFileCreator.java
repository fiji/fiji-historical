package fiji.pluginManager.utilities;

import java.io.File;

public class XMLFileCreator {
	public XMLFileCreator() {
		
	}

	//first argument given should be path
	public static void main(String args[]) {
		String path = null;
		if (args.length == 0)
			path = "";
		else
			path = args[0];

		path = path.replace(File.separator, "/"); //standardize
		if (!path.endsWith("/"))
			path += "/";
	}
}
