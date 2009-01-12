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
|	PointPicker_
\===================================================================*/

/*********************************************************************
 This class is the only one that is accessed directly by imageJ;
 it attaches listeners and dies. Note that it implements
 <code>PlugIn</code> rather than <code>PlugInFilter</code>.
 ********************************************************************/
public class PointPicker

{ /* begin class PointPicker_ */

	 public pointHandler[] ph;
	final boolean usedColor[] = new boolean[1024];
	 
	 public pointToolbar tb;
/*..................................................................*/
/* Public methods													*/
/*..................................................................*/

/*------------------------------------------------------------------*/
	public PointPicker () {
	final ImagePlus imp = WindowManager.getCurrentImage();
	if (imp == null) {
		IJ.noImage();
		return;
	}
	final ImageCanvas ic = imp.getWindow().getCanvas();
	
	tb = new pointToolbar(Toolbar.getInstance());
	final int stackSize = imp.getStackSize();
	 ph = new pointHandler[stackSize];
	 
	// int puntpartcol=0;//frosi
	 
	for (int s = 0; (s < stackSize); s++) {
		ph[s] = new pointHandler(imp, tb,  this);
//		puntpartcol=(puntpartcol+1)%4;//frosi
		
		
	}
	final pointAction pa = new pointAction(imp, ph, tb);
	for (int s = 0; (s < stackSize); s++) {
		ph[s].setPointAction(pa);
	}
} /* end run */

} /* end class PointPicker_ */

/*====================================================================
|	pointPickerClearAll
\===================================================================*/
