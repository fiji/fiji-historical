package fiji.managerUI;
import java.awt.Color;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

//Utility Class (To format textpane)
public class TextPaneFormat {
	public static SimpleAttributeSet ITALIC_GRAY = new SimpleAttributeSet();
	public static SimpleAttributeSet BOLD_BLACK = new SimpleAttributeSet();
	public static SimpleAttributeSet BLACK = new SimpleAttributeSet();

	static {
		StyleConstants.setForeground(ITALIC_GRAY, Color.gray);
		StyleConstants.setItalic(ITALIC_GRAY, true);
		StyleConstants.setFontFamily(ITALIC_GRAY, "Verdana");
		StyleConstants.setFontSize(ITALIC_GRAY, 12);

		StyleConstants.setForeground(BOLD_BLACK, Color.black);
		StyleConstants.setBold(BOLD_BLACK, true);
		StyleConstants.setFontFamily(BOLD_BLACK, "Verdana");
		StyleConstants.setFontSize(BOLD_BLACK, 12);

		StyleConstants.setForeground(BLACK, Color.black);
		StyleConstants.setFontFamily(BLACK, "Verdana");
		StyleConstants.setFontSize(BLACK, 12);
	}

	public static void insertText(JTextPane textPane, String text, AttributeSet set) {
		try {
			textPane.getDocument().insertString(textPane.getDocument().getLength(), text, set);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
}
