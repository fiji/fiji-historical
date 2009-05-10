import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

import javax.swing.JButton;
import javax.swing.JLabel;

public class MyPlugin implements PlugIn {
	public void run(String arg) {
		//IJ.showMessage("My_Plugin","Hello world!");
		//plugin frame appears immediately after creation...
		MyPluginFrame mpf = new MyPluginFrame();
	}

}
