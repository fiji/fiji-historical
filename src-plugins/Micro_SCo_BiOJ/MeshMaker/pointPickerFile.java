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
|	pointPickerFile
\===================================================================*/

/*********************************************************************
 This class creates a dialog to store and retrieve points into and
 from a text file, respectively.
 ********************************************************************/
class pointPickerFile
	extends
		Dialog
	implements
		ActionListener

{ /* begin class pointPickerFile */

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
 <ul><li><code>Save as</code>: Save points into a text file;</li>
 <li><code>Show</code>: Display the coordinates in ImageJ's window;</li>
 <li><code>Open</code>: Retrieve points from a text file;</li>
 <li><code>Cancel</code>: Do nothing.</li></ul>
 ********************************************************************/
public void actionPerformed (
	final ActionEvent ae
) {
	this.setVisible(false);
	if (ae.getActionCommand().equals("Save as")) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(f, "Point list", FileDialog.SAVE);
		final String path;
		String filename = imp.getTitle();
		final int dot = filename.lastIndexOf('.');
		if (dot == -1) {
			fd.setFile(filename + ".txt");
		}
		else {
			filename = filename.substring(0, dot);
			fd.setFile(filename + ".txt");
		}
		fd.setVisible(true);
		path = fd.getDirectory();
		filename = fd.getFile();
		if ((path == null) || (filename == null)) {
			return;
		}
		try {
			final FileWriter fw = new FileWriter(path + filename);
			Point p;
			String n;
			String x;
			String y;
			String z;
			String c;
			fw.write("point     x     y slice color\n");
			for (int s = 0; (s < ph.length); s++) {
				Vector listPoints = ph[s].getPoints();
				Vector listColors = ph[s].getColors();
				for (int k = 0; (k < listPoints.size()); k++) {
					n = "" + k;
					while (n.length() < 5) {
						n = " " + n;
					}
					p = (Point)listPoints.elementAt(k);
					x = "" + p.x;
					while (x.length() < 5) {
						x = " " + x;
					}
					y = "" + p.y;
					while (y.length() < 5) {
						y = " " + y;
					}
					z = "" + (s + 1);
					while (z.length() < 5) {
						z = " " + z;
					}
					c = "" + ((Integer)listColors.elementAt(k)).intValue();
					while (c.length() < 5) {
						c = " " + c;
					}
					fw.write(n + " " + x + " " + y + " " + z + " " + c + "\n");
				}
			}
			fw.close();
		} catch (IOException e) {
			IJ.error("IOException exception");
		} catch (SecurityException e) {
			IJ.error("Security exception");
		}
	}
	else if (ae.getActionCommand().equals("Show")) {
		try{
		Point p;
		String n;
		String x;
		String y;
		String z;
		String c;
		IJ.getTextPanel().setFont(new Font("Monospaced", Font.PLAIN, 12));
		IJ.setColumnHeadings(" point\t      x\t      y\t slice\t color");
		for (int s = 0; (s < ph.length); s++) {
			Vector listPoints = ph[s].getPoints();
			Vector listColors = ph[s].getColors();
			for (int k = 0; (k < listPoints.size()); k++) {
				n = "" + k;
				while (n.length() < 6) {
					n = " " + n;
				}
				p = (Point)listPoints.elementAt(k);
				x = "" + p.x;
				while (x.length() < 7) {
					x = " " + x;
				}
				y = "" + p.y;
				while (y.length() < 7) {
					y = " " + y;
				}
				z = "" + (s + 1);
				while (z.length() < 6) {
					z = " " + z;
				}
				c = "" + ((Integer)listColors.elementAt(k)).intValue();
				while (c.length() < 6) {
					c = " " + c;
				}
				IJ.write(n + "\t" + x + "\t" + y + "\t" + z + "\t" + c);
			}
		}
	}catch(Exception e){IJ.showMessage("Errore " + e.getMessage());}
	}
	else if (ae.getActionCommand().equals("Open")) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(f, "Point list", FileDialog.LOAD);
		fd.setVisible(true);
		final String path = fd.getDirectory();
		final String filename = fd.getFile();
		if ((path == null) || (filename == null)) {
			return;
		}
		try {
			final FileReader fr = new FileReader(path + filename);
			final BufferedReader br = new BufferedReader(fr);
			for (int s = 0; (s < ph.length); s++) {
				ph[s].removePoints();
			}
			String line;
			String pString;
			String xString;
			String yString;
			String zString;
			String cString;
			int separatorIndex;
			int x;
			int y;
			int z;
			int c;
			if ((line = br.readLine()) == null) {
				fr.close();
				return;
			}
			while ((line = br.readLine()) != null) {
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					fr.close();
					IJ.error("Invalid file");
					return;
				}
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					fr.close();
					IJ.error("Invalid file");
					return;
				}
				xString = line.substring(0, separatorIndex);
				xString = xString.trim();
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					separatorIndex = line.length();
				}
				yString = line.substring(0, separatorIndex);
				yString = yString.trim();
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					separatorIndex = line.length();
				}
				zString = line.substring(0, separatorIndex);
				zString = zString.trim();
				line = line.substring(separatorIndex);
				line = line.trim();
				separatorIndex = line.indexOf(' ');
				if (separatorIndex == -1) {
					separatorIndex = line.length();
				}
				cString = line.substring(0, separatorIndex);
				cString = cString.trim();
				x = Integer.parseInt(xString);
				y = Integer.parseInt(yString);
				z = Integer.parseInt(zString) - 1;
				c = Integer.parseInt(cString);
				if (z < ph.length) {
					ph[z].addPoint(x, y, c);
				}
			}
			fr.close();
		} catch (FileNotFoundException e) {
			IJ.error("File not found exception");
		} catch (IOException e) {
			IJ.error("IOException exception");
		} catch (NumberFormatException e) {
			IJ.error("Number format exception");
		}
		imp.setRoi(ph[imp.getCurrentSlice() - 1]);
	}
	else if (ae.getActionCommand().equals("Cancel")) {
	}
} /* end actionPerformed */

public static void write(String str, pointHandler[] ph)
{
try {
			final FileWriter fw = new FileWriter(str);
			Point p;
			String n;
			String x;
			String y;
			String z;
			String c;
			fw.write("point     x     y slice color\n");
			for (int s = 0; (s < ph.length); s++) {
				Vector listPoints = ph[s].getPoints();
				Vector listColors = ph[s].getColors();
				for (int k = 0; (k < listPoints.size()); k++) {
					n = "" + k;
					while (n.length() < 5) {
						n = " " + n;
					}
					p = (Point)listPoints.elementAt(k);
					x = "" + p.x;
					while (x.length() < 5) {
						x = " " + x;
					}
					y = "" + p.y;
					while (y.length() < 5) {
						y = " " + y;
					}
					z = "" + (s + 1);
					while (z.length() < 5) {
						z = " " + z;
					}
					c = "" + ((Integer)listColors.elementAt(k)).intValue();
					while (c.length() < 5) {
						c = " " + c;
					}
					fw.write(n + " " + x + " " + y + " " + z + " " + c + "\n");
				}
			}
			fw.close();
		} catch (Exception e) {
			IJ.error("IOException exception");
		
	
}
}
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
 @param ph <code>pointHandler</code> object that handles operations.
 @param imp <code>ImagePlus</code> object where points are being picked.
 ********************************************************************/
public pointPickerFile (
	final Frame parentWindow,
	final pointHandler[] ph,
	final ImagePlus imp
) {
	super(parentWindow, "Point List", true);
	this.ph = ph;
	this.imp = imp;
	setLayout(new GridLayout(0, 1));
	final Button saveAsButton = new Button("Save as");
	final Button showButton = new Button("Show");
	final Button openButton = new Button("Open");
	final Button cancelButton = new Button("Cancel");
	saveAsButton.addActionListener(this);
	showButton.addActionListener(this);
	openButton.addActionListener(this);
	cancelButton.addActionListener(this);
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(saveAsButton);
	add(showButton);
	add(openButton);
	add(separation2);
	add(cancelButton);
	pack();
} /* end pointPickerFile */

} /* end class pointPickerFile */
