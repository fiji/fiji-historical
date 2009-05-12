import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.frame.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class MyPluginFrame extends PlugInFrame implements ActionListener {
	private JButton myBtn = null;
	private JLabel myLbl = null;
	private JScrollPane scrollPane = null;
	private JEditorPane editorPane = null;
	private String filename = "\\test.html";

	public MyPluginFrame() {
		super("Plugin_Frame");
		this.setSize(640,480);
		this.setLayout(new BorderLayout());

		myLbl = new JLabel();
		myLbl.setText("Text");
		myLbl.setBounds(25,25,250,25);
		myBtn = new JButton();
		myBtn.setText("Load");
		myBtn.setBounds(50,50,100,30);
		myBtn.addActionListener(this);
		this.add(myBtn);
		this.add(myLbl);

		editorPane = new JEditorPane();
	    editorPane.setEditable(false);
	    editorPane.setEditorKit(new HTMLEditorKit());
	    scrollPane = new JScrollPane(editorPane);
	    this.add(scrollPane, BorderLayout.CENTER);
	    this.add(editorPane);

		//pack();
		GUI.center(this);
		this.show();
	}
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == myBtn) {
			//perform an action...
			//IJ.showMessage("My_Plugin","Hello world!");
		    FileReader reader = null;
		    try {
		      System.out.println("Loading");
		      reader = new FileReader(filename);

		      // Create empty HTMLDocument to read into
		      HTMLEditorKit htmlKit = new HTMLEditorKit();
		      HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
		      // Create parser (javax.swing.text.html.parser.ParserDelegator)
		      HTMLEditorKit.Parser parser = new ParserDelegator();
		      // Get parser callback from document
		      HTMLEditorKit.ParserCallback callback = htmlDoc.getReader(0);
		      // Load it (true means to ignore character set)
		      parser.parse(reader, callback, true);
		      // Replace document
		      editorPane.setDocument(htmlDoc);
		      System.out.println("Loaded");

		    } catch (IOException exception) {
		      System.out.println("Load oops");
		      exception.printStackTrace();
		    } finally {
		      if (reader != null) {
		        try {
		          reader.close();
		        } catch (IOException ignoredException) {
		        }
		      }
		    }
		}
	}
}
