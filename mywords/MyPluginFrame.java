import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.frame.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;

public class MyPluginFrame extends PlugInFrame implements ActionListener {
	private JButton myBtn = new JButton();
	private JLabel myLbl = new JLabel();
	public MyPluginFrame() {
		super("Plugin_Frame");
		this.setSize(300,200);
		this.setLayout(null);

		myLbl.setText("Test label...");
		myLbl.setBounds(25,25,250,25);
		myBtn.setText("Click me!");
		myBtn.setBounds(25,50,120,30);
		myBtn.addActionListener(this);
		this.add(myBtn);
		this.add(myLbl);

		//pack();
		GUI.center(this);
		this.show();
	}
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == myBtn) {
			IJ.showMessage("My_Plugin","Hello world!");
		}
	}
}
