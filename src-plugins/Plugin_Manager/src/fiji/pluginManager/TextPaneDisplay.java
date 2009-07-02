package fiji.pluginManager;

import java.awt.Color;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class TextPaneDisplay extends JTextPane {
	public SimpleAttributeSet ITALIC_BLACK;
	public SimpleAttributeSet BOLD_BLACK;
	public SimpleAttributeSet BLACK;
	public SimpleAttributeSet BOLD_BLACK_TITLE;

	public TextPaneDisplay() {
		ITALIC_BLACK = new SimpleAttributeSet();
		BOLD_BLACK = new SimpleAttributeSet();
		BLACK = new SimpleAttributeSet();
		BOLD_BLACK_TITLE = new SimpleAttributeSet();

		StyleConstants.setForeground(ITALIC_BLACK, Color.black);
		StyleConstants.setItalic(ITALIC_BLACK, true);
		StyleConstants.setFontFamily(ITALIC_BLACK, "Verdana");
		StyleConstants.setFontSize(ITALIC_BLACK, 12);

		StyleConstants.setForeground(BOLD_BLACK, Color.black);
		StyleConstants.setBold(BOLD_BLACK, true);
		StyleConstants.setFontFamily(BOLD_BLACK, "Verdana");
		StyleConstants.setFontSize(BOLD_BLACK, 12);

		StyleConstants.setForeground(BLACK, Color.black);
		StyleConstants.setFontFamily(BLACK, "Verdana");
		StyleConstants.setFontSize(BLACK, 12);

		StyleConstants.setForeground(BOLD_BLACK_TITLE, Color.black);
		//StyleConstants.setBold(BOLD_BLACK_TITLE, true);
		StyleConstants.setFontFamily(BOLD_BLACK_TITLE, "Impact");
		StyleConstants.setFontSize(BOLD_BLACK_TITLE, 18);

		setEditable(false);
	}

	public void insertStyledText(String text, AttributeSet set) throws BadLocationException {
		getDocument().insertString(getDocument().getLength(), text, set);
	}

	public void insertBoldText(String text) throws BadLocationException {
		getDocument().insertString(getDocument().getLength(), text, BOLD_BLACK);
	}

	public void insertText(String text) throws BadLocationException {
		getDocument().insertString(getDocument().getLength(), text, BLACK);
	}

	//appends list of dependencies to existing text
	public void insertDependenciesList(List<Dependency> dependencyList) throws BadLocationException {
		String strDependencies = "";
		if (dependencyList != null) {
			int noOfDependencies = dependencyList.size();
			for (int i = 0; i < noOfDependencies; i++) {
				Dependency dependency = dependencyList.get(i);
				strDependencies +=  dependency.getFilename() + " (" + dependency.getTimestamp() + ")";
				if (i != noOfDependencies -1 && noOfDependencies != 1) //if last index
					strDependencies += ",\n";
			}
			if (strDependencies.equals("")) strDependencies = "None";
		} else {
			strDependencies = "None";
		}
		insertText("\n" + strDependencies);
	}

	//appends plugin description to existing text
	public void insertDescription(String description)
	throws BadLocationException {
		if (description == null || description.trim().equals("")) {
			insertText("\nNo description available.");
		} else {
			insertText("\n" + description);
		}
	}

	//appends list of plugin names to existing text
	public void insertPluginNamelist(List<PluginObject> myList)
	throws BadLocationException {
		for (int i = 0; i < myList.size(); i++) {
			PluginObject myPlugin = myList.get(i);
			insertText("\n" + myPlugin.getFilename());
		}
	}

	//inserts blank new line
	public void insertBlankLine() throws BadLocationException {
		insertText("\n\n");
	}

	//rewrite the entire textpane with details of a plugin
	public void showPluginDetails(PluginObject plugin) throws BadLocationException {
		setText("");
		//Display plugin data, text with different formatting
		insertStyledText(plugin.getFilename(), BOLD_BLACK_TITLE);
		if (plugin.isUpdateable())
			insertStyledText("\n(Update is available)", ITALIC_BLACK);
		insertBlankLine();
		insertBoldText("Md5 Sum");
		insertText("\n" + plugin.getmd5Sum());
		insertBlankLine();
		insertBoldText("Date: ");
		insertText(plugin.getTimestamp());
		insertBlankLine();
		insertBoldText("Dependency");
		insertDependenciesList(plugin.getDependencies());
		insertBlankLine();
		insertBoldText("Is Fiji Plugin: ");
		insertText(plugin.isFijiPlugin() ? "Yes" : "No");
		insertBlankLine();
		insertBoldText("Description");
		insertDescription(plugin.getDescription());
		if (plugin.isUpdateable()) {
			insertBlankLine();
			insertStyledText("Update Details", BOLD_BLACK_TITLE);
			insertBlankLine();
			insertBoldText("New Md5 Sum");
			insertText("\n" + plugin.getNewMd5Sum());
			insertBlankLine();
			insertBoldText("Released: ");
			insertText(plugin.getNewTimestamp());
			insertBlankLine();
			insertBoldText("Dependency");
			insertDependenciesList(plugin.getNewDependencies());
			insertBlankLine();
			insertBoldText("Description");
			insertDescription(plugin.getNewDescription());
		}

		//ensure first line of text is always shown (i.e.: scrolled to top)
		scrollToTop();
	}
	
	public void showDownloadProgress(Installer installer) {
		//List<PluginObject> toUninstallList;
		List<PluginObject> downloadedList = installer.getListOfDownloaded();
		List<PluginObject> failedList = installer.getListOfFailedDownloads();
		PluginObject currentlyDownloading = installer.getCurrentDownload();
		boolean stillDownloading = installer.stillDownloading();
		String strCurrentStatus = "";

		for (int i=0; i < downloadedList.size(); i++) {
			PluginObject myPlugin = downloadedList.get(i);
			if (i != 0) strCurrentStatus += "\n";
			strCurrentStatus += "Finished downloading " + myPlugin.getFilename();
		}
		for (int i=0; i < failedList.size(); i++) {
			PluginObject myPlugin = failedList.get(i);
			if (i != 0 && !strCurrentStatus.equals("")) strCurrentStatus += "\n";
			strCurrentStatus += myPlugin.getFilename() + " failed to download.";
		}
		if (currentlyDownloading != null)
			strCurrentStatus += "\nNow downloading " + currentlyDownloading.getFilename();

		//check if download has finished (Whether 100% success or not)
		if (stillDownloading == false) {
			if (downloadedList.size() > 0) {
				int totalSize = downloadedList.size() + failedList.size();
				strCurrentStatus += "\n" + downloadedList.size() + " of " + totalSize +
				" download tasks completed.";
			} else {
				strCurrentStatus += "\nDownload(s) failed.";
			}
		}
		setText(strCurrentStatus);
	}

	public void scrollToTop() {
		setSelectionStart(0);
		setSelectionEnd(0);
	}
}
