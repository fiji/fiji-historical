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
|	pointToolbar
\===================================================================*/

/*********************************************************************
 This class deals with the toolbar that gets substituted to that of
 ImageJ.
 ********************************************************************/
public class pointToolbar
	extends
		Canvas
	implements
		AdjustmentListener,
		MouseListener

{ /* begin class pointToolbar */

/*....................................................................
	 variables
....................................................................*/
 static final int NUM_TOOLS = 19;
 static final int SIZE = 22;
 static final int OFFSET = 3;
 static final Color gray = Color.lightGray;
 static final Color brighter = gray.brighter();
 static final Color darker = gray.darker();
 static final Color evenDarker = darker.darker();
 final boolean[] down = new boolean[NUM_TOOLS];
 Graphics g;
	Scrollbar scrollbar;
 ImagePlus imp;
 Toolbar previousInstance;
 pointAction pa;
 pointHandler[] ph;
 pointToolbar instance;
 long mouseDownTime;
 int currentTool = pointAction.ADD_CROSS;
 int currentMode = pointAction.MONOSLICE;
 int x;
 int y;
 int xOffset;
 int yOffset;
final int previousTool=0;
/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Listen to <code>AdjustmentEvent</code> events.
 @param e Ignored.
 ********************************************************************/
public synchronized void adjustmentValueChanged (
	AdjustmentEvent e
) {
	imp.setRoi(ph[e.getValue() - 1]);
} /* adjustmentValueChanged */

/*********************************************************************
 Return the index of the mode that is currently activated.
 ********************************************************************/
public int getCurrentMode (
) {
	return(pointAction.MONOSLICE);
} /* getCurrentMode */

/*********************************************************************
 Return the index of the tool that is currently activated.
 ********************************************************************/
public int getCurrentTool (
) {
	return(currentTool);
} /* getCurrentTool */

/*********************************************************************
 Setup the various listeners.
 @param pa <code>pointAction</code> object.
 ********************************************************************/
public void installListeners (
	pointAction pa
) {
	this.pa = pa;
	final ImageWindow iw = imp.getWindow();
	final ImageCanvas ic = iw.getCanvas();
	iw.requestFocus();
	iw.removeKeyListener(IJ.getInstance());
	ic.removeMouseListener(ic);
	ic.removeMouseMotionListener(ic);
	ic.addMouseMotionListener(pa);
	ic.addMouseListener(pa);
	iw.addKeyListener(pa);
	if (imp.getWindow() instanceof StackWindow) {
		StackWindow sw = (StackWindow)imp.getWindow();
		final Component component[] = sw.getComponents();
		for (int i = 0; (i < component.length); i++) {
			if (component[i] instanceof Scrollbar) {
				scrollbar = (Scrollbar)component[i];
				scrollbar.addAdjustmentListener(this);
			}
		}
	}
	else {
		scrollbar = null;
	}
} /* end installListeners */

/*********************************************************************
 Listen to <code>mouseClicked</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseClicked (
	final MouseEvent e
) {
} /* end mouseClicked */

/*********************************************************************
 Listen to <code>mouseEntered</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseEntered (
	final MouseEvent e
) {
} /* end mouseEntered */

/*********************************************************************
 Listen to <code>mouseExited</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseExited (
	final MouseEvent e
) {
} /* end mouseExited */

/*********************************************************************
 Listen to <code>mousePressed</code> events. Test for single or double
 clicks and perform the relevant action.
 @param e Event.
 ********************************************************************/
public void mousePressed (
	final MouseEvent e
) {
	final int x = e.getX();
	final int y = e.getY();
	final int previousTool = currentTool;
	int newTool = 0;
	for (int i = 0; (i < NUM_TOOLS); i++) {
		if (((i * SIZE) < x) && (x < (i * SIZE + SIZE))) {
			newTool = i;
		}
	}
	final boolean doubleClick = ((newTool == getCurrentTool())
		&& ((System.currentTimeMillis() - mouseDownTime) <= 500L));
	mouseDownTime = System.currentTimeMillis();
	switch (newTool) {
		case pointAction.MONOSLICE:
		case pointAction.MULTISLICE:
			setMode(newTool);
			return;
		default:
			setTool(newTool);
	}
	if (doubleClick) {
		switch (newTool) {
			case pointAction.ADD_CROSS:
			case pointAction.MOVE_CROSS:
				pointPickerColorSettings colorDialog
					= new pointPickerColorSettings(IJ.getInstance(), ph, imp);
				GUI.center(colorDialog);
				colorDialog.setVisible(true);
				colorDialog.dispose();
				break;
			case pointAction.REMOVE_CROSS:
				pointPickerClearAll clearAllDialog
					= new pointPickerClearAll(IJ.getInstance(), ph, imp);
				GUI.center(clearAllDialog);
				clearAllDialog.setVisible(true);
				clearAllDialog.dispose();
				break;
		}
	}
	switch (newTool) {
		case pointAction.FILE:
			pointPickerFile fileDialog
				= new pointPickerFile(IJ.getInstance(), ph, imp);
			GUI.center(fileDialog);
			fileDialog.setVisible(true);
			setTool(previousTool);
			fileDialog.dispose();
			break;
		case pointAction.TERMINATE:
			pointPickerTerminate terminateDialog
				= new pointPickerTerminate(IJ.getInstance());
			GUI.center(terminateDialog);
			terminateDialog.setVisible(true);
			if (terminateDialog.choseCancel()) {
				setTool(previousTool);
			}
			else {
				for (int s = 0; (s < ph.length); s++) {
					ph[s].removePoints();
				}
				cleanUpListeners();
				restorePreviousToolbar();
				Toolbar.getInstance().repaint();
			}
			terminateDialog.dispose();
			break;
	}
} /* mousePressed */

/*********************************************************************
 Listen to <code>mouseReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseReleased (
	final MouseEvent e
) {
} /* end mouseReleased */

/*********************************************************************
 Draw the tools of the toolbar.
 @param g Graphics environment.
 ********************************************************************/
public void paint (
	final Graphics g
) {
	for (int i = 0; (i < NUM_TOOLS); i++) {
		drawButton(g, i);
	}
} /* paint */

/*********************************************************************
 This constructor substitutes ImageJ's toolbar by that of PointPicker_.
 @param previousToolbar ImageJ's toolbar.
 ********************************************************************/
public pointToolbar (
	final Toolbar previousToolbar
) {
	previousInstance = previousToolbar;
	instance = this;
	final Container container = previousToolbar.getParent();
//	final Container container = IJ.getInstance(); // Proposed by Maxime Pinchon
	final Component component[] = container.getComponents();
	for (int i = 0; (i < component.length); i++) {
		if (component[i] == previousToolbar) {
			container.remove(previousToolbar);
			container.add(this, i);
			break;
		}
	}
	resetButtons();
	down[currentTool] = true;
	//down[currentMode] = true;
	setTool(currentTool);
	setMode(pointAction.MONOSLICE);
	setForeground(evenDarker);
	setBackground(gray);
	addMouseListener(this);
	container.validate();
} /* end pointToolbar */

/*********************************************************************
 Setup the current mode. The selection of non-functional modes is
 honored but leads to a no-op action.
 @param mode Admissible modes belong to [<code>0</code>,
 <code>NUM_TOOLS - 1</code>]
 ********************************************************************/
public void setMode (
	final int mode
) {
	if (mode == currentMode) {
		return;
	}
	//down[mode] = true;
	//down[currentMode] = false;
	final Graphics g = this.getGraphics();
	drawButton(g, currentMode);
	drawButton(g, mode);
	g.dispose();
	showMessage(mode);
	currentMode = mode;
} /* end setMode */

/*********************************************************************
 Setup the current tool. The selection of non-functional tools is
 honored but leads to a no-op action.
 @param tool Admissible tools belong to [<code>0</code>,
 <code>NUM_TOOLS - 1</code>]
 ********************************************************************/
public void setTool (
	final int tool
) {
	if (tool == currentTool) {
		return;
	}
	if ((tool==0) || (tool==1) || (tool ==2) || (tool ==7) ||(tool ==8) ||(tool ==11))
	{down[tool] = true;
	down[currentTool] = false;
	final Graphics g = this.getGraphics();
	drawButton(g, currentTool);
	drawButton(g, tool);
	g.dispose();
	showMessage(tool);
	currentTool = tool;
	}
} /* end setTool */

/*********************************************************************
 Setup the point handler.
 @param ph <code>pointHandler</code> object that handles operations.
 @param imp <code>ImagePlus</code> object where points are being picked.
 ********************************************************************/
public void setWindow (
	final pointHandler[] ph,
	final ImagePlus imp
) {
	this.ph = ph;
	this.imp = imp;
} /* end setWindow */

public void cleanUpListeners (
) {
	if (scrollbar != null) {
		scrollbar.removeAdjustmentListener(this);
	}
	final ImageWindow iw = imp.getWindow();
	final ImageCanvas ic = iw.getCanvas();
	iw.removeKeyListener(pa);
	ic.removeMouseListener(pa);
	ic.removeMouseMotionListener(pa);
	ic.addMouseMotionListener(ic);
	ic.addMouseListener(ic);
	iw.addKeyListener(IJ.getInstance());
} /* end cleanUpListeners */

public void restorePreviousToolbar (
) {
	final Container container = instance.getParent();
	final Component component[] = container.getComponents();
	for (int i = 0; (i < component.length); i++) {
		if (component[i] == instance) {
			container.remove(instance);
			container.add(previousInstance, i);
			container.validate();
			break;
		}
	}
} /* end restorePreviousToolbar */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/

/*------------------------------------------------------------------*/
 void d (
	int x,
	int y
) {
	x += xOffset;
	y += yOffset;
	g.drawLine(this.x, this.y, x, y);
	this.x = x;
	this.y = y;
} /* end d */

/*------------------------------------------------------------------*/
 void drawButton (
	final Graphics g,
	final int tool
) {
	fill3DRect(g, tool * SIZE + 1, 1, SIZE, SIZE - 1, !down[tool]);
	g.setColor(Color.black);
	int x = tool * SIZE + OFFSET;
	int y = OFFSET;
	if (down[tool]) {
		x++;
		y++;
	}
	this.g = g;
	switch (tool) {
		case pointAction.ADD_CROSS:
			xOffset = x;
			yOffset = y;
			m(7, 0);
			d(7, 1);
			m(6, 2);
			d(6, 3);
			m(8, 2);
			d(8, 3);
			m(5, 4);
			d(5, 5);
			m(9, 4);
			d(9, 5);
			m(4, 6);
			d(4, 8);
			m(10, 6);
			d(10, 8);
			m(5, 9);
			d(5, 14);
			m(9, 9);
			d(9, 14);
			m(7, 4);
			d(7, 6);
			m(7, 8);
			d(7, 8);
			m(4, 11);
			d(10, 11);
			g.fillRect(x + 6, y + 12, 3, 3);
			m(11, 13);
			d(15, 13);
			m(13, 11);
			d(13, 15);
			break;
		case pointAction.MOVE_CROSS:
			xOffset = x;
			yOffset = y;
			m(1, 1);
			d(1, 10);
			m(2, 2);
			d(2, 9);
			m(3, 3);
			d(3, 8);
			m(4, 4);
			d(4, 7);
			m(5, 5);
			d(5, 7);
			m(6, 6);
			d(6, 7);
			m(7, 7);
			d(7, 7);
			m(11, 5);
			d(11, 6);
			m(10, 7);
			d(10, 8);
			m(12, 7);
			d(12, 8);
			m(9, 9);
			d(9, 11);
			m(13, 9);
			d(13, 11);
			m(10, 12);
			d(10, 15);
			m(12, 12);
			d(12, 15);
			m(11, 9);
			d(11, 10);
			m(11, 13);
			d(11, 15);
			m(9, 13);
			d(13, 13);
			break;
		case pointAction.REMOVE_CROSS:
			xOffset = x;
			yOffset = y;
			m(7, 0);
			d(7, 1);
			m(6, 2);
			d(6, 3);
			m(8, 2);
			d(8, 3);
			m(5, 4);
			d(5, 5);
			m(9, 4);
			d(9, 5);
			m(4, 6);
			d(4, 8);
			m(10, 6);
			d(10, 8);
			m(5, 9);
			d(5, 14);
			m(9, 9);
			d(9, 14);
			m(7, 4);
			d(7, 6);
			m(7, 8);
			d(7, 8);
			m(4, 11);
			d(10, 11);
			g.fillRect(x + 6, y + 12, 3, 3);
			m(11, 13);
			d(15, 13);
			break;
		case pointAction.MONOSLICE:
			/*xOffset = x;
			yOffset = y;
			m(2, 6);
			d(2, 6);
			m(3, 5);
			d(3, 5);
			m(5, 3);
			d(5, 3);
			m(6, 2);
			d(6, 2);
			m(9, 6);
			d(9, 6);
			m(10, 5);
			d(10, 5);
			m(12, 3);
			d(12, 3);
			m(13, 2);
			d(13, 2);
			m(9, 13);
			d(9, 13);
			m(10, 12);
			d(10, 12);
			m(12, 10);
			d(12, 10);
			m(13, 9);
			d(13, 9);
			m(2, 13);
			d(2, 13);
			m(3, 12);
			d(3, 12);
			m(4, 11);
			d(11, 11);
			d(11, 4);
			d(4, 4);
			d(4, 11);
			break;*/
		case pointAction.MULTISLICE:
			/*xOffset = x;
			yOffset = y;
			m(2, 13);
			d(9, 13);
			d(9, 6);
			d(2, 6);
			d(2, 13);
			m(3, 5);
			d(3, 5);
			m(4, 4);
			d(11, 4);
			d(11, 11);
			m(10, 12);
			d(10, 12);
			m(10, 5);
			d(10, 5);
			m(12, 3);
			d(12, 3);
			m(12, 10);
			d(12, 10);
			m(5, 3);
			d(5, 3);
			m(6, 2);
			d(13, 2);
			d(13, 9);*/
			break;
		case pointAction.FILE:
			xOffset = x;
			yOffset = y;
			m(3, 1);
			d(9, 1);
			d(9, 4);
			d(12, 4);
			d(12, 14);
			d(3, 14);
			d(3, 1);
			m(10, 2);
			d(11, 3);
			m(5, 4);
			d(7, 4);
			m(5, 6);
			d(10, 6);
			m(5, 8);
			d(10, 8);
			m(5, 10);
			d(10, 10);
			m(5, 12);
			d(10, 12);
			break;
		case pointAction.TERMINATE:
			xOffset = x;
			yOffset = y;
			m(5, 0);
			d(5, 8);
			m(4, 5);
			d(4, 7);
			m(3, 6);
			d(3, 7);
			m(2, 7);
			d(2, 9);
			m(1, 8);
			d(1, 9);
			m(2, 10);
			d(6, 10);
			m(3, 11);
			d(3, 13);
			m(1, 14);
			d(6, 14);
			m(0, 15);
			d(7, 15);
			m(2, 13);
			d(2, 13);
			m(5, 13);
			d(5, 13);
			m(7, 8);
			d(14, 8);
			m(8, 7);
			d(15, 7);
			m(8, 9);
			d(13, 9);
			m(9, 6);
			d(9, 10);
			m(15, 4);
			d(15, 6);
			d(14, 6);
			break;
		case pointAction.MAGNIFIER:
			xOffset = x + 2;
			yOffset = y + 2;
			m(3, 0);
			d(3, 0);
			d(5, 0);
			d(8, 3);
			d(8, 5);
			d(7, 6);
			d(7, 7);
			d(6, 7);
			d(5, 8);
			d(3, 8);
			d(0, 5);
			d(0, 3);
			d(3, 0);
			m(8, 8);
			d(9, 8);
			d(13, 12);
			d(13, 13);
			d(12, 13);
			d(8, 9);
			d(8, 8);
			break;
	}
} /* end drawButton */

/*------------------------------------------------------------------*/
 void fill3DRect (
	final Graphics g,
	final int x,
	final int y,
	final int width,
	final int height,
	final boolean raised
) {
	if (raised) {
		g.setColor(gray);
	}
	else {
		g.setColor(darker);
	}
	g.fillRect(x + 1, y + 1, width - 2, height - 2);
	g.setColor((raised) ? (brighter) : (evenDarker));
	g.drawLine(x, y, x, y + height - 1);
	g.drawLine(x + 1, y, x + width - 2, y);
	g.setColor((raised) ? (evenDarker) : (brighter));
	g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
	g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
} /* end fill3DRect */

/*------------------------------------------------------------------*/
 void m (
	final int x,
	final int y
) {
	this.x = xOffset + x;
	this.y = yOffset + y;
} /* end m */

/*------------------------------------------------------------------*/
 void resetButtons (
) {
	for (int i = 0; (i < NUM_TOOLS); i++) {
		down[i] = false;
	}
} /* end resetButtons */

/*------------------------------------------------------------------*/

/*------------------------------------------------------------------*/
 void showMessage (
	final int tool
) {
	switch (tool) {
		case pointAction.ADD_CROSS:
			IJ.showStatus("Add crosses");
			return;
		case pointAction.MOVE_CROSS:
			IJ.showStatus("Move crosses");
			return;
		case pointAction.REMOVE_CROSS:
			IJ.showStatus("Remove crosses");
			return;
		case pointAction.MONOSLICE:
			//IJ.showStatus("Apply to the current slice");
			return;
		case pointAction.MULTISLICE:
			//IJ.showStatus("Apply to all slices");
			return;
		case pointAction.FILE:
			IJ.showStatus("Export/Import list of points");
			return;
		case pointAction.TERMINATE:
			IJ.showStatus("Exit PointPicker");
			return;
		case pointAction.MAGNIFIER:
			IJ.showStatus("Magnifying glass");
			return;
		default:
			IJ.showStatus("Undefined operation");
			return;
	}
} /* end showMessage */

} /* end class pointToolbar */
