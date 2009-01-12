package MeshMaker;

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
|	pointPickerTerminate
\===================================================================*/

/*********************************************************************
 This class creates a dialog to return to ImageJ.
 ********************************************************************/
public class pointPickerTerminate
	extends
		Dialog
	implements
		ActionListener

{ /* begin class pointPickerTerminate */

/*....................................................................
	Private variables
....................................................................*/
private final CheckboxGroup choice = new CheckboxGroup();
private boolean cancel = false;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method processes the button actions.
 @param ae The expected actions are as follows:
 <ul><li><code>Done</code>: Return to ImageJ;</li>
 <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	this.setVisible(false);
	if (ae.getActionCommand().equals("Done")) {
	}
	else if (ae.getActionCommand().equals("Cancel")) {
		cancel = true;
	}
} /* end actionPerformed */

/*********************************************************************
 Return <code>true</code> only if the user chose <code>Cancel</code>.
 ********************************************************************/
public boolean choseCancel (
) {
	return(cancel);
} /* end choseCancel */

/*********************************************************************
 Return some additional margin to the dialog, for aesthetic purposes.
 Necessary for the current MacOS X Java version, lest the first item
 disappears from the frame.
 ********************************************************************/
public Insets getInsets (
) {
	return(new Insets(0, 40, 20, 40));
} /* end getInsets */

/*********************************************************************
 This constructor prepares the layout of the dialog.
 @param parentWindow Parent window.
 ********************************************************************/
public pointPickerTerminate (
	final Frame parentWindow
) {
	super(parentWindow, "Back to ImageJ", true);
	setLayout(new GridLayout(0, 1));
	final Button doneButton = new Button("Done");
	final Button cancelButton = new Button("Cancel");
	doneButton.addActionListener(this);
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(doneButton);
	add(separation2);
	add(cancelButton);
	pack();
} /* end pointPickerTerminate */

} /* end class pointPickerTerminate */