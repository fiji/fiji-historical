package fiji.pluginManager.ui;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

//Specialized functions to ease repetitive creation of Swing components
public class SwingTools {
	//Created a tabbed pane with a single tab that contains a single component
	public static JTabbedPane getSingleTabbedPane(JComponent component, String title, String tooltip,
			int width, int height) {
		JPanel tabPane = new JPanel();
		tabPane.setLayout(new BorderLayout());
		tabPane.add(component, BorderLayout.CENTER);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab(title, null, tabPane, tooltip);
		tabbedPane.setPreferredSize(new Dimension(width,height));
		return tabbedPane;
	}

	//Creates a JScrollPane for the given textpane of specified width and height
	public static JScrollPane getTextScrollPane(JTextPane textPane, int width, int height) {
		textPane.setPreferredSize(new Dimension(width, height));
		JScrollPane scrollpane = new JScrollPane(textPane);
		scrollpane.getViewport().setBackground(textPane.getBackground());
		scrollpane.setPreferredSize(new Dimension(width, height));

		return scrollpane;
	}

	//Creates a JPanel with a label that sticks to the left
	public static JPanel createLabelPanel(String text) {
		JLabel label = new JLabel(text, SwingConstants.LEFT);
		JPanel lblPanel = createBoxLayoutPanel(BoxLayout.X_AXIS);
		lblPanel.add(label);
		lblPanel.add(Box.createHorizontalGlue());
		return lblPanel;
	}

	public static JPanel createBoxLayoutPanel(int alignment) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, alignment));
		return panel;
	}

	public static JButton createButton(String buttonTitle, String toolTipText) {
		JButton btn = new JButton(buttonTitle);
		btn.setToolTipText(toolTipText);
		return btn;
	}
}