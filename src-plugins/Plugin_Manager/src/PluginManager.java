import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

import javax.swing.JButton;
import javax.swing.JLabel;
import Presentation.PluginMgrUI;

public class PluginManager implements PlugIn {
	public void run(String arg) {
		//IJ.showMessage("My_Plugin","Hello world!");
		//plugin frame appears immediately after creation...
		PluginMgrUI pluginMgrUI = PluginMgrUI.getInstance();
		//pluginMgrUI.setVisible(true);
		pluginMgrUI.show();
	}
}
