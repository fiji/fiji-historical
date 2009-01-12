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
|	pointAction
\===================================================================*/

/*********************************************************************
 This class is responsible for dealing with the mouse events relative
 to the image window.
 ********************************************************************/
public class pointAction
	extends
		ImageCanvas
	implements
		FocusListener,
		KeyListener,
		MouseListener,
		MouseMotionListener

{ /* begin class pointAction */

/*....................................................................
	Public variables
....................................................................*/
public static final int ADD_CROSS = 0;
public static final int MOVE_CROSS = 1;
public static final int REMOVE_CROSS = 2;
public static final int MONOSLICE = 4;
public static final int MULTISLICE = 5;
public static final int FILE = 7;
public static final int TERMINATE = 8;
public static final int MAGNIFIER = 11;

/*....................................................................
	Private variables
....................................................................*/
 ImagePlus imp;
 pointHandler[] ph;
 pointToolbar tb;
 boolean active = false;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 Listen to <code>focusGained</code> events.
 @param e Ignored.
 ********************************************************************/
public void focusGained (
	final FocusEvent e
) {
	active = true;
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
} /* end focusGained */

/*********************************************************************
 Listen to <code>focusGained</code> events.
 @param e Ignored.
 ********************************************************************/
public void focusLost (
	final FocusEvent e
) {
	active = false;
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
} /* end focusLost */

/*********************************************************************
 Return true if the window is active.
 ********************************************************************/
public boolean isActive (
) {
	return(active);
} /* end isActive */

/*********************************************************************
 Listen to <code>keyPressed</code> events.
 @param e The expected key codes are as follows:
 <ul><li><code>KeyEvent.VK_DELETE</code>: remove the current landmark;</li>
 <li><code>KeyEvent.VK_BACK_SPACE</code>: remove the current landmark;</li>
 <li><code>KeyEvent.VK_COMMA</code>: display the previous slice, if any;</li>
 <li><code>KeyEvent.VK_DOWN</code>: move down the current landmark;</li>
 <li><code>KeyEvent.VK_LEFT</code>: move the current landmark to the left;</li>
 <li><code>KeyEvent.VK_PERIOD</code>: display the next slice, if any;</li>
 <li><code>KeyEvent.VK_RIGHT</code>: move the current landmark to the right;</li>
 <li><code>KeyEvent.VK_SPACE</code>: select the current landmark;</li>
 <li><code>KeyEvent.VK_TAB</code>: activate the next landmark;</li>
 <li><code>KeyEvent.VK_UP</code>: move up the current landmark.</li></ul>
 ********************************************************************/
public void keyPressed (
	final KeyEvent e
) {
	active = true;
	switch (e.getKeyCode()) {
		case KeyEvent.VK_COMMA:
			if (1 < imp.getCurrentSlice()) {
				imp.setSlice(imp.getCurrentSlice() - 1);
				imp.setRoi(ph[imp.getCurrentSlice() - 1]);
				updateStatus();
			}
			return;
		case KeyEvent.VK_PERIOD:
			if (imp.getCurrentSlice() < imp.getStackSize()) {
				imp.setSlice(imp.getCurrentSlice() + 1);
				imp.setRoi(ph[imp.getCurrentSlice() - 1]);
				updateStatus();
			}
			return;
	}
	final Point p = ph[imp.getCurrentSlice() - 1].getPoint();
	if (p == null) {
		return;
	}
	final int x = p.x;
	final int y = p.y;
	int scaledX;
	int scaledY;
	int scaledShiftedX;
	int scaledShiftedY;
	switch (e.getKeyCode()) {
		case KeyEvent.VK_DELETE:
		case KeyEvent.VK_BACK_SPACE:
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].removePoint();
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].removePoint(commonColor);
					}
					break;
			}
			break;
		case KeyEvent.VK_DOWN:
			scaledX = imp.getWindow().getCanvas().screenX(x);
			scaledShiftedY = imp.getWindow().getCanvas().screenY(y
				+ (int)Math.ceil(1.0 / imp.getWindow().getCanvas().getMagnification()));
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].movePoint(scaledX, scaledShiftedY);
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].movePoint(scaledX, scaledShiftedY, commonColor);
					}
					break;
			}
			break;
		case KeyEvent.VK_LEFT:
			scaledShiftedX = imp.getWindow().getCanvas().screenX(x
				- (int)Math.ceil(1.0 / imp.getWindow().getCanvas().getMagnification()));
			scaledY = imp.getWindow().getCanvas().screenY(y);
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].movePoint(scaledShiftedX, scaledY);
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].movePoint(scaledShiftedX, scaledY, commonColor);
					}
					break;
			}
			break;
		case KeyEvent.VK_RIGHT:
			scaledShiftedX = imp.getWindow().getCanvas().screenX(x
				+ (int)Math.ceil(1.0 / imp.getWindow().getCanvas().getMagnification()));
			scaledY = imp.getWindow().getCanvas().screenY(y);
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].movePoint(scaledShiftedX, scaledY);
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].movePoint(scaledShiftedX, scaledY, commonColor);
					}
					break;
			}
			break;
		case KeyEvent.VK_TAB:
			ph[imp.getCurrentSlice() - 1].nextPoint();
			break;
		case KeyEvent.VK_SPACE:
			break;
		case KeyEvent.VK_UP:
			scaledX = imp.getWindow().getCanvas().screenX(x);
			scaledShiftedY = imp.getWindow().getCanvas().screenY(y
				- (int)Math.ceil(1.0 / imp.getWindow().getCanvas().getMagnification()));
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].movePoint(scaledX, scaledShiftedY);
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].movePoint(scaledX, scaledShiftedY, commonColor);
					}
					break;
			}
			break;
	}
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
	updateStatus();
} /* end keyPressed */

/*********************************************************************
 Listen to <code>keyReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void keyReleased (
	final KeyEvent e
) {
	active = true;
} /* end keyReleased */

/*********************************************************************
 Listen to <code>keyTyped</code> events.
 @param e Ignored.
 ********************************************************************/
public void keyTyped (
	final KeyEvent e
) {
	active = true;
} /* end keyTyped */

/*********************************************************************
 Listen to <code>mouseClicked</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseClicked (
	final MouseEvent e
) {
	active = true;
} /* end mouseClicked */

/*********************************************************************
 Listen to <code>mouseDragged</code> events. Move the current point
 and refresh the image window.
 @param e Event.
 ********************************************************************/
public void mouseDragged (
	final MouseEvent e
) {
	active = true;
	final int x = e.getX();
	final int y = e.getY();
	if (tb.getCurrentTool() == MOVE_CROSS) {
		switch (tb.getCurrentMode()) {
			case MONOSLICE:
				ph[imp.getCurrentSlice() - 1].movePoint(x, y);
				break;
			case MULTISLICE:
				final Integer commonColor =
					ph[imp.getCurrentSlice() - 1].getCurrentColor();
				for (int s = 0; (s < ph.length); s++) {
					ph[s].movePoint(x, y, commonColor);
				}
				break;
		}
		imp.setRoi(ph[imp.getCurrentSlice() - 1]);
	}
	mouseMoved(e);
} /* end mouseDragged */

/*********************************************************************
 Listen to <code>mouseEntered</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseEntered (
	final MouseEvent e
) {
	active = true;
	WindowManager.setCurrentWindow(imp.getWindow());
	imp.getWindow().toFront();
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
} /* end mouseEntered */

/*********************************************************************
 Listen to <code>mouseExited</code> events. Clear the ImageJ status
 bar.
 @param e Event.
 ********************************************************************/
public void mouseExited (
	final MouseEvent e
) {
	active = false;
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
	IJ.showStatus("");
} /* end mouseExited */

/*********************************************************************
 Listen to <code>mouseMoved</code> events. Update the ImageJ status
 bar.
 @param e Event.
 ********************************************************************/
public void mouseMoved (
	final MouseEvent e
) {
	active = true;
	setControl();
	final int x = imp.getWindow().getCanvas().offScreenX(e.getX());
	final int y = imp.getWindow().getCanvas().offScreenY(e.getY());
	IJ.showStatus(imp.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end mouseMoved */

/*********************************************************************
 Listen to <code>mousePressed</code> events. Perform the relevant
 action.
 @param e Event.
 ********************************************************************/
public void mousePressed (
	final MouseEvent e
) {
	active = true;
	final int x = e.getX();
	final int y = e.getY();
	int currentPoint;
	switch (tb.getCurrentTool()) {
		case ADD_CROSS:
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].addPoint(
						imp.getWindow().getCanvas().offScreenX(x),
						imp.getWindow().getCanvas().offScreenY(y));
					break;
				case MULTISLICE:
					final int commonFreeColor = sieveColors();
					if (0 <= commonFreeColor) {
						for (int s = 0; (s < ph.length); s++) {
							ph[s].addPoint(
								imp.getWindow().getCanvas().offScreenX(x),
								imp.getWindow().getCanvas().offScreenY(y),
								commonFreeColor);
						}
					}
					break;
			}
			break;
		case MAGNIFIER:
			final int flags = e.getModifiers();
			if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) {
				imp.getWindow().getCanvas().zoomOut(x, y);
			}
			else {
				imp.getWindow().getCanvas().zoomIn(x, y);
			}
			break;
		case MOVE_CROSS:
			ph[imp.getCurrentSlice() - 1].findClosest(x, y);
			break;
		case REMOVE_CROSS:
			ph[imp.getCurrentSlice() - 1].findClosest(x, y);
			switch (tb.getCurrentMode()) {
				case MONOSLICE:
					ph[imp.getCurrentSlice() - 1].removePoint();
					break;
				case MULTISLICE:
					final Integer commonColor =
						ph[imp.getCurrentSlice() - 1].getCurrentColor();
					for (int s = 0; (s < ph.length); s++) {
						ph[s].removePoint(commonColor);
					}
					break;
			}
			break;
	}
	imp.setRoi(ph[imp.getCurrentSlice() - 1]);
} /* end mousePressed */

/*********************************************************************
 Listen to <code>mouseReleased</code> events.
 @param e Ignored.
 ********************************************************************/
public void mouseReleased (
	final MouseEvent e
) {
	active = true;
} /* end mouseReleased */

/*********************************************************************
 This constructor stores a local copy of its parameters and initializes
 the current control.
 @param imp <code>ImagePlus</code> object where points are being picked.
 @param ph <code>pointHandler</code> object that handles operations.
 @param tb <code>pointToolbar</code> object that handles the toolbar.
 ********************************************************************/
public pointAction (
	final ImagePlus imp,
	final pointHandler[] ph,
	final pointToolbar tb
) {
	super(imp);
	this.imp = imp;
	this.ph = ph;
	this.tb = tb;
	tb.setWindow(ph, imp);
	tb.installListeners(this);
} /* end pointAction */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
 String getValueAsString (
	final int x,
	final int y
) {
	final Calibration cal = imp.getCalibration();
	final int[] v = imp.getPixel(x, y);
	switch (imp.getType()) {
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16:
			final double cValue = cal.getCValue(v[0]);
			if (cValue==v[0]) {
				return(", value=" + v[0]);
			}
			else {
				return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
			}
		case ImagePlus.GRAY32:
			return(", value=" + Float.intBitsToFloat(v[0]));
		case ImagePlus.COLOR_256:
			return(", index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);
		case ImagePlus.COLOR_RGB:
			return(", value=" + v[0] + "," + v[1] + "," + v[2]);
		default:
			return("");
	}
} /* end getValueAsString */

/*------------------------------------------------------------------*/
 void setControl (
) {
	switch (tb.getCurrentTool()) {
		case ADD_CROSS:
			imp.getWindow().getCanvas().setCursor(crosshairCursor);
			break;
		case FILE:
		case MAGNIFIER:
		case MOVE_CROSS:
		case REMOVE_CROSS:
			imp.getWindow().getCanvas().setCursor(defaultCursor);
			break;
	}
} /* end setControl */

/*------------------------------------------------------------------*/
 int sieveColors (
) {
	int attempt = 0;
	/*	boolean found;
	do {
		found = true;
		for (int s = 0; (s < ph.length); s++) {
			if (ph[s].isUsedColor(attempt)) {
				found = false;
				attempt++;
				break;
			}
		}
	} while((attempt < pointHandler.GAMUT) && !found);
	if (!found) {
		attempt = -1;
		IJ.error("No color could be found that would fit all slices");
	}*/
	return(attempt);
} /* end sieveColors */

/*------------------------------------------------------------------*/
 void updateStatus (
) {
	final Point p = ph[imp.getCurrentSlice() - 1].getPoint();
	if (p == null) {
		IJ.showStatus("");
		return;
	}
	final int x = p.x;
	final int y = p.y;
	IJ.showStatus(imp.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end updateStatus */

} /* end class pointAction */
