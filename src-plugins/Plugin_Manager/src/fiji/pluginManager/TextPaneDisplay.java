package fiji.pluginManager;

import java.awt.Color;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class TextPaneDisplay extends JTextPane {
	private SimpleAttributeSet ITALIC_BLACK;
	private SimpleAttributeSet BOLD_BLACK;
	private SimpleAttributeSet BLACK;
	private SimpleAttributeSet BOLD_BLACK_TITLE;

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

	public SimpleAttributeSet styleItalicBlack() {
		return ITALIC_BLACK;
	}

	public SimpleAttributeSet styleBoldBlack() {
		return BOLD_BLACK;
	}

	public SimpleAttributeSet styleBlack() {
		return BLACK;
	}

	public SimpleAttributeSet styleBoldTitle() {
		return BOLD_BLACK_TITLE;
	}

	public void insertStyledText(String text, AttributeSet set) throws BadLocationException {
		getDocument().insertString(getDocument().getLength(), text, set);
	}

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

	public void insertBoldText(String text) throws BadLocationException {
		getDocument().insertString(getDocument().getLength(), text, BOLD_BLACK);
	}

	public void insertText(String text) throws BadLocationException {
		getDocument().insertString(getDocument().getLength(), text, BLACK);
	}

	public void insertDescription(String description)
	throws BadLocationException {
		if (description == null || description.trim().equals("")) {
			insertText("\nNo description available.");
		} else {
			insertText("\n" + description);
		}
	}

	public void insertPluginNamelist(List<PluginObject> myList)
	throws BadLocationException {
		for (int i = 0; i < myList.size(); i++) {
			PluginObject myPlugin = myList.get(i);
			insertText("\n" + myPlugin.getFilename());
		}
	}

	public void insertBlankLine() throws BadLocationException {
		insertText("\n\n");
	}

	public void scrollToTop() {
		setSelectionStart(0);
		setSelectionEnd(0);
	}
}
