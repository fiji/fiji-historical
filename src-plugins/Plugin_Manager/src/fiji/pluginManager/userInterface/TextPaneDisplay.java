package fiji.pluginManager.userInterface;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import fiji.pluginManager.logic.Dependency;
import fiji.pluginManager.logic.Installer;
import fiji.pluginManager.logic.PluginObject;

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
		insertText("\n");
		for (int i = 0; i < myList.size(); i++) {
			PluginObject myPlugin = myList.get(i);
			insertText(myPlugin.getFilename() + "\n");
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
		}

		//ensure first line of text is always shown (i.e.: scrolled to top)
		scrollToTop();
	}

	public void showDownloadProgress(Installer installer) {
		Iterator<PluginObject> iterDownloaded = installer.iterDownloaded();
		int downloadedSize = 0;
		Iterator<PluginObject> iterFailedDownloads = installer.iterFailedDownloads();
		int failedDownloads = 0;
		Iterator<PluginObject> iterMarkedUninstall = installer.iterMarkedUninstall();
		int markedUninstallSize = 0;
		Iterator<PluginObject> iterFailedUninstalls = installer.iterFailedUninstalls();
		int failedUninstalls = 0;

		PluginObject currentlyDownloading = installer.currentlyDownloading;
		boolean stillDownloading = installer.isDownloading();
		String strCurrentStatus = "";

		while (iterFailedUninstalls.hasNext()) {
			strCurrentStatus += "Failed to mark " + iterFailedUninstalls.next().getFilename() + " for uninstalling.\n";
			failedUninstalls++;
		}
		while (iterMarkedUninstall.hasNext()) {
			strCurrentStatus += "Marked " + iterMarkedUninstall.next().getFilename() + " for uninstalling.\n";
			markedUninstallSize++;
		}
		while (iterDownloaded.hasNext()) {
			strCurrentStatus += "Finished downloading " + iterDownloaded.next().getFilename() + "\n";
			downloadedSize++;
		}
		while (iterFailedDownloads.hasNext()) {
			strCurrentStatus += iterFailedDownloads.next().getFilename() + " failed to download.\n";
			failedDownloads++;
		}
		if (currentlyDownloading != null)
			strCurrentStatus += "Now downloading " + currentlyDownloading.getFilename() + "\n";

		//if no more download tasks, results can be displayed
		if (stillDownloading == false) {
			//Display overall results
			if (markedUninstallSize > 0) {
				int totalSize = markedUninstallSize + failedUninstalls;
				strCurrentStatus += markedUninstallSize + " of " + totalSize +
				" plugins successfully marked for removal.\n";
			} else if (failedUninstalls > 0) {
				strCurrentStatus += "Marking for deletion(s) failed.\n";
			} //else if uninstall lists' sizes are 0, ignore

			if (downloadedSize > 0) {
				int totalSize = downloadedSize + failedDownloads;
				strCurrentStatus += downloadedSize + " of " + totalSize +
				" download tasks completed.\n";
			} else if (failedDownloads > 0) {
				strCurrentStatus += "Download(s) failed.\n";
			} //else if download lists' sizes are 0, ignore
		}
		setText(strCurrentStatus);
	}

	public void scrollToTop() {
		setSelectionStart(0);
		setSelectionEnd(0);
	}
}
