package fiji.pluginManager;

import java.awt.Color;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class TextPaneFormat {
	public static SimpleAttributeSet ITALIC_BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BOLD_BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BOLD_BLACK_TITLE = new SimpleAttributeSet();

	static {
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
	}

	public static void insertText(JTextPane textPane, String text, AttributeSet set)
	throws BadLocationException {
		textPane.getDocument().insertString(textPane.getDocument().getLength(), text, set);
	}

	public static void insertText(JTextPane textPane, String text)
	throws BadLocationException {
		textPane.getDocument().insertString(textPane.getDocument().getLength(), text, TextPaneFormat.BLACK);
	}

	public static void insertDependenciesList(JTextPane textPane, List<Dependency> dependencyList)
	throws BadLocationException {
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
		insertText(textPane, "\n" + strDependencies);
	}

	public static void insertDescription(JTextPane textPane, String description)
	throws BadLocationException {
		if (description == null || description.trim().equals("")) {
			insertText(textPane, "\nNo description available.");
		} else {
			insertText(textPane, "\n" + description);
		}
	}

	public static void insertPluginNamelist(JTextPane textPane, List<PluginObject> myList)
	throws BadLocationException {
		for (int i = 0; i < myList.size(); i++) {
			PluginObject myPlugin = myList.get(i);
			insertText(textPane, "\n" + myPlugin.getFilename());
		}
	}

	public static void insertBlankLine(JTextPane textPane) throws BadLocationException {
		insertText(textPane, "\n\n");
	}
}