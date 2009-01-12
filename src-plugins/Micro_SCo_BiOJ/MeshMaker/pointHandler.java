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
|	pointHandler
\===================================================================*/

/*********************************************************************
 This class is responsible for dealing with the list of point
 coordinates and for their visual appearance.
 ********************************************************************/
public class pointHandler
	extends
		Roi

{ /* begin class pointHandler */

/*....................................................................
	Public variables
....................................................................*/
public static final int RAINBOW = 1;
public static final int MONOCHROME = 2;
public static final int GAMUT = 1024;

/*....................................................................
	Private variables
....................................................................*/
static final int CROSS_HALFSIZE = 5;
 final Color spectrum[] = new Color[GAMUT];
 //final boolean pp.usedColor[] = new boolean[GAMUT];
 final Vector listColors = new Vector(0, 16);
 final Vector listPoints = new Vector(0, 16);
 ImagePlus imp;
 pointAction pa;
 pointToolbar tb;
 int nextColor = 1;
 int currentPoint = -1;
 int numPoints = 0;
 boolean started = false;
 
 PointPicker pp;

/*....................................................................
	Public methods
....................................................................*/

/*********************************************************************
 This method adds a new point to the list, with a color that is as
 different as possible from all those that are already in use. The
 points are stored in pixel units rather than canvas units to cope
 for different zooming factors.
 @param x Horizontal coordinate, in canvas units.
 @param y Vertical coordinate, in canvas units.
 ********************************************************************/
public void addPoint (
	final int x,
	final int y
) {
	if (numPoints < GAMUT) {
		if (pp.usedColor[nextColor]) {
			int k;
			boolean found = false;
			for (k = 0; (k < GAMUT); k++) {
				nextColor++;
				nextColor &= GAMUT - 1;
				if (!pp.usedColor[nextColor]) {
					found = true;
					break;
				}
			}
			if (!found) {
				throw new IllegalStateException("Unexpected lack of available colors");
			}
		}
		final Point p = new Point(x, y);
		listPoints.addElement(p);
		listColors.addElement(new Integer(nextColor));
		pp.usedColor[nextColor] = true;
		nextColor++;
		nextColor &= GAMUT - 1;
		currentPoint = numPoints;
		numPoints++;
	}
	else {
		IJ.error("Maximum number of points reached for this slice");
	}
} /* end addPoint */

/*********************************************************************
 This method adds a new point to the list, with a specific color.
 The points are stored in pixel units rather than canvas units to
 cope for different zooming factors.
 @param x Horizontal coordinate, in canvas units.
 @param y Vertical coordinate, in canvas units.
 @param color Specific color.
 ********************************************************************/
public void addPoint (
	final int x,
	final int y,
	final int color
) {
	if (pp.usedColor[color]) {
		throw new IllegalStateException("Illegal color request");
	}
	final Point p = new Point(x, y);
	listPoints.addElement(p);
	listColors.addElement(new Integer(color));
	pp.usedColor[color] = true;
	currentPoint = numPoints;
	numPoints++;
} /* end addPoint */

/*********************************************************************
 Draw the landmarks and outline the current point if there is one.
 @param g Graphics environment.
 ********************************************************************/
public void draw (
	final Graphics g
) {
	if (started) {
		final float mag = (float)ic.getMagnification();
		final int dx = (int)(mag / 2.0);
		final int dy = (int)(mag / 2.0);
		for (int k = 0; (k < numPoints); k++) {
			final Point p = (Point)listPoints.elementAt(k);
			g.setColor(spectrum[((Integer)listColors.elementAt(k)).intValue()]);
			if (k == currentPoint) {
				if (pa.isActive()) {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - 1) + dy);
					g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					g.drawLine(ic.screenX(p.x + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x - 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + 1) + dy);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + 1) + dy,
						ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - 1) + dy);
					if (1.0 < ic.getMagnification()) {
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy);
						g.drawLine(ic.screenX(p.x) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE) + dy,
							ic.screenX(p.x) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE) + dy);
					}
				}
				else {
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
					g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
						ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
						ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
						ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
				}
			}
			else {
				g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
					ic.screenY(p.y) + dy,
					ic.screenX(p.x + CROSS_HALFSIZE) + dx,
					ic.screenY(p.y) + dy);
				g.drawLine(ic.screenX(p.x) + dx,
					ic.screenY(p.y - CROSS_HALFSIZE) + dy,
					ic.screenX(p.x) + dx,
					ic.screenY(p.y + CROSS_HALFSIZE) + dy);
			}
		}
		if (updateFullWindow) {
			updateFullWindow = false;
			imp.draw();
		}
	}
} /* end draw */

/*********************************************************************
 Let the point that is closest to the given coordinates become the
 current landmark.
 @param x Horizontal coordinate, in canvas units.
 @param y Vertical coordinate, in canvas units.
 ********************************************************************/
public void findClosest (
	int x,
	int y
) {
	if (numPoints == 0) {
		return;
	}
	x = ic.offScreenX(x);
	y = ic.offScreenY(y);
	Point p = new Point((Point)listPoints.elementAt(currentPoint));
	float distance = (float)(x - p.x) * (float)(x - p.x)
		+ (float)(y - p.y) * (float)(y - p.y);
	for (int k = 0; (k < numPoints); k++) {
		p = (Point)listPoints.elementAt(k);
		final float candidate = (float)(x - p.x) * (float)(x - p.x)
			+ (float)(y - p.y) * (float)(y - p.y);
		if (candidate < distance) {
			distance = candidate;
			currentPoint = k;
		}
	}
} /* end findClosest */

/*********************************************************************
 Return the list of colors.
 ********************************************************************/
public Vector getColors (
) {
	return(listColors);
} /* end getColors */

/*********************************************************************
 Return the color of the current object. Return -1 if there is none.
 ********************************************************************/
public Integer getCurrentColor (
) {
	return((0 <= currentPoint) ? ((Integer)listColors.elementAt(currentPoint))
		: (new Integer(-1)));
} /* end getCurrentColor */

/*********************************************************************
 Return the current point as a <code>Point</code> object.
 ********************************************************************/
public Point getPoint (
) {
	return((0 <= currentPoint) ? ((Point)listPoints.elementAt(currentPoint))
		: (null));
} /* end getPoint */

/*********************************************************************
 Return the list of points.
 ********************************************************************/
public Vector getPoints (
) {
	return(listPoints);
} /* end getPoints */

/*********************************************************************
 Return <code>true</code> if color is free.
 Return <code>false</code> if color is in use.
 ********************************************************************/
public boolean isusedColor (
	final int color
) {
	return(pp.usedColor[color]);
} /* end ispp.usedColor */

/*********************************************************************
 Modify the location of the current point. Clip the admissible range
 to the image size.
 @param x Desired new horizontal coordinate in canvas units.
 @param y Desired new vertical coordinate in canvas units.
 ********************************************************************/
public void movePoint (
	int x,
	int y
) {
	if (0 <= currentPoint) {
		x = ic.offScreenX(x);
		y = ic.offScreenY(y);
		x = (x < 0) ? (0) : (x);
		x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
		y = (y < 0) ? (0) : (y);
		y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
		listPoints.removeElementAt(currentPoint);
		final Point p = new Point(x, y);
		listPoints.insertElementAt(p, currentPoint);
	}
} /* end movePoint */

/*********************************************************************
 Modify the location of the current point. Clip the admissible range
 to the image size.
 @param x Desired new horizontal coordinate in canvas units.
 @param y Desired new vertical coordinate in canvas units.
 @param color Color index of the point to move.
 ********************************************************************/
public void movePoint (
	int x,
	int y,
	final Integer color
) {
	final int index = listColors.indexOf(color);
	if (index == -1) {
		return;
	}
	currentPoint = index;
	movePoint(x, y);
} /* end movePoint */

/*********************************************************************
 Change the current point.
 ********************************************************************/
public void nextPoint (
) {
	currentPoint = (currentPoint == (numPoints - 1)) ? (0) : (currentPoint + 1);
} /* end nextPoint */

/*********************************************************************
 This constructor stores a local copy of its parameters and initializes
 the current spectrum. It also creates the object that takes care of
 the interactive work.
 @param imp <code>ImagePlus</code> object where points are being picked.
 @param tb <code>pointToolbar</code> object that handles the toolbar.
 ********************************************************************/
public pointHandler (
	final ImagePlus imp,
	final pointToolbar tb,
	//int part, 
	PointPicker pp
) {
	super(0, 0, imp.getWidth(), imp.getHeight(), imp);
	this.pp=pp;
	this.imp = imp;
	this.tb = tb;
	//this.nextColor= this.nextColor + part;//frosi
	setSpectrum(RAINBOW);
} /* end pointHandler */

/*********************************************************************
 Remove the current point. Make its color available again.
 ********************************************************************/
public void removePoint (
) {
	if (0 < numPoints) {
		listPoints.removeElementAt(currentPoint);
		pp.usedColor[((Integer)listColors.elementAt(currentPoint)).intValue()] = false;
		listColors.removeElementAt(currentPoint);
		numPoints--;
	}
	currentPoint = numPoints - 1;
	if (currentPoint < 0) {
		tb.setTool(pointAction.ADD_CROSS);
	}
} /* end removePoint */

/*********************************************************************
 Remove a point of a given index. Make its color available again.
 @param color Color index of the point to remove.
 ********************************************************************/
public void removePoint (
	final Integer color
) {
	final int index = listColors.indexOf(color);
	if (index == -1) {
		return;
	}
	currentPoint = index;
	removePoint();
} /* end removePoint */

/*********************************************************************
 Remove all points and make every color available.
 ********************************************************************/
public void removePoints (
) {
	listPoints.removeAllElements();
	listColors.removeAllElements();
	for (int k = 0; (k < GAMUT); k++) {
		pp.usedColor[k] = false;
	}
	nextColor = 0;
	numPoints = 0;
	currentPoint = -1;
	tb.setTool(pointAction.ADD_CROSS);
	imp.setRoi(this);
} /* end removePoints */

/*********************************************************************
 Stores a local copy of its parameter and allows the graphical
 operations to proceed. The present class is now fully initialized.
 @param pa <code>pointAction</code> object.
 ********************************************************************/
public void setPointAction (
	final pointAction pa
) {
	this.pa = pa;
	started = true;
} /* end setPointAction */

/*********************************************************************
 Setup the color scheme.
 @ param colorization Colorization code. Admissible values are
 {<code>RAINBOW</code>, <code>MONOCHROME</code>}.
 ********************************************************************/
public void setSpectrum (
	final int colorization
) {
	int k = 0;
	switch (colorization) {
		case RAINBOW:
			final int bound1 = GAMUT / 6;
			final int bound2 = GAMUT / 3;
			final int bound3 = GAMUT / 2;
			final int bound4 = (2 * GAMUT) / 3;
			final int bound5 = (5 * GAMUT) / 6;
			final int bound6 = GAMUT;
			final float gamutChunk1 = (float)bound1;
			final float gamutChunk2 = (float)(bound2 - bound1);
			final float gamutChunk3 = (float)(bound3 - bound2);
			final float gamutChunk4 = (float)(bound4 - bound3);
			final float gamutChunk5 = (float)(bound5 - bound4);
			final float gamutChunk6 = (float)(bound6 - bound5);
			do {
				spectrum[stirColor(k)] = new Color(1.0F, (float)k
					/ gamutChunk1, 0.0F);
				pp.usedColor[stirColor(k)] = false;
			} while (++k < bound1);
			do {
				spectrum[stirColor(k)] = new Color(1.0F - (float)(k - bound1)
					/ gamutChunk2, 1.0F, 0.0F);
				pp.usedColor[stirColor(k)] = false;
			} while (++k < bound2);
			do {
				spectrum[stirColor(k)] = new Color(0.0F, 1.0F, (float)(k - bound2)
					/ gamutChunk3);
				pp.usedColor[stirColor(k)] = false;
			} while (++k < bound3);
			do {
				spectrum[stirColor(k)] = new Color(0.0F, 1.0F - (float)(k - bound3)
					/ gamutChunk4, 1.0F);
				pp.usedColor[stirColor(k)] = false;
			} while (++k < bound4);
			do {
				spectrum[stirColor(k)] = new Color((float)(k - bound4)
					/ gamutChunk5, 0.0F, 1.0F);
				pp.usedColor[stirColor(k)] = false;
			} while (++k < bound5);
			do {
				spectrum[stirColor(k)] = new Color(1.0F, 0.0F, 1.0F - (float)(k - bound5)
					/ gamutChunk6);
				pp.usedColor[stirColor(k)] = false;
			} while (++k < bound6);
			break;
		case MONOCHROME:
			for (k = 0; (k < GAMUT); k++) {
				spectrum[k] = ROIColor;
				pp.usedColor[k] = false;
			}
			break;
	}
	imp.setRoi(this);
} /* end setSpectrum */

/*....................................................................
	Private methods
....................................................................*/

/*------------------------------------------------------------------*/
private int stirColor (
	int color
) {
	if (color < 0) {
		return(-1);
	}
	int stirredColor = 0;
	for (int k = 0; (k < (int)Math.round(Math.log((double)GAMUT) / Math.log(2.0))); k++) {
		stirredColor <<= 1;
		stirredColor |= (color & 1);
		color >>= 1;
	}
	return(stirredColor);
} /* end stirColor */

} /* end class pointHandler */
