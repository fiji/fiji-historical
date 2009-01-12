package MeshMaker;

/*====================================================================
| Version: September 27, 2003
\===================================================================*/

/*====================================================================
| EPFL/STI/BIO-E/LIB
| Philippe Thevenaz
| Bldg. BM-Ecublens 4.137
| CH-1015 Lausanne VD
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.37.01
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

/*====================================================================
| Additional help available at http://bigwww.epfl.ch/thevenaz/pointpicker/
\===================================================================*/

import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.WindowManager;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;


/*====================================================================
|	pointPickerColorSettings
\===================================================================*/

/*********************************************************************
 This class creates a dialog to choose the color scheme.
 ********************************************************************/
class pointPickerColorSettings
	extends
		Dialog
	implements
		ActionListener

{ /* begin class pointPickerColorSettings */

/*....................................................................
	Private variables
....................................................................*/
 final CheckboxGroup choice = new CheckboxGroup();
 ImagePlus imp;
 pointHandler[] ph;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method processes the button actions.
 @param ae The expected actions are as follows:
 <ul><li><code>Rainbow</code>: Display points in many colors;</li>
 <li><code>Monochrome</code>: Display points in ImageJ's highlight color;</li>
 <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	
	
	if (ae.getActionCommand().equals("Rainbow")) {
		for (int s = 0; (s < ph.length); s++) {
			ph[s].setSpectrum(pointHandler.RAINBOW);
		}
		imp.setRoi(ph[imp.getCurrentSlice() - 1]);
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Monochrome")) {
		for (int s = 0; (s < ph.length); s++) {
			ph[s].setSpectrum(pointHandler.MONOCHROME);
		}
		imp.setRoi(ph[imp.getCurrentSlice() - 1]);
		setVisible(false);
	}
	else if (ae.getActionCommand().equals("Cancel")) {
		setVisible(false);
	}
} /* end actionPerformed */

/*********************************************************************
 Return some additional margin to the dialog, for aesthetic purposes.
 Necessary for the current MacOS X Java version, lest the first item
 disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*********************************************************************
 This constructor stores a local copy of its parameters and prepares
 the layout of the dialog.
 @param parentWindow Parent window.
 @param imp <code>ImagePlus</code> object where points are being picked.
 @param ph <code>pointHandler</code> object that handles operations.
 ********************************************************************/
public pointPickerColorSettings (
	final Frame parentWindow,
	final pointHandler[] ph,
	final ImagePlus imp
) {
	super(parentWindow, "Color Settings", true);
	this.ph = ph;
	this.imp = imp;
	setLayout(new GridLayout(0, 1));
	final Button rainbow = new Button("Rainbow");
	final Button monochrome = new Button("Monochrome");
	final Button cancelButton = new Button("Cancel");
	rainbow.addActionListener(this);
	monochrome.addActionListener(this);
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(rainbow);
	add(monochrome);
	add(separation2);
	add(cancelButton);
	pack();
} /* end pointPickerColorSettings */

} /* end class pointPickerColorSettings */
