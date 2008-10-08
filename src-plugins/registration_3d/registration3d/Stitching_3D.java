package registration3d;
/**
 * <p>Title: Stitching_3D</p>
 *
 * <p>Description: Fourier based PlugIn for stitching 3D microscopic slices which are only related by shift (not rotation). Has full support
 * for ImageJ macro language and can, of course, also be executed as a normal PlugIn. It depends only on the Fourier Package included in the 
 * edu.mines.jtk framework developed by Dave Hale (http://www.mines.edu/~dhale/jtk/) </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: The Hackathon 2007 @ Janelia Farm and previous work at the MPI-CBG in Dresden/Germany</p>
 *
 * <p>License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * @author Stephan Preibisch
 * @version 1.0
 */

import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.process.*;
import ij.gui.Roi;
import ij.gui.MultiLineLabel;
import ij.ImageStack;
import ij.plugin.BrowserLauncher;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;

import edu.mines.jtk.dsp.FftComplex;
import edu.mines.jtk.dsp.FftReal;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Cursor;

public class Stitching_3D implements PlugIn
{
	private String myURL = "http://fly.mpi-cbg.de/~preibisch/contact.html";
	private String imgStack1, imgStack2, handleRGB1, handleRGB2, method, fusedImageName;
	private ImagePlus imp1 = null, imp2 = null; 
	private boolean fuseImages, windowing, coregister, wasIndexed;
	private int checkPeaks, numberOfChannels;
	private ArrayList <String[]>coregStacks;
	private ArrayList <ImagePlus[]>coregStackIMPs;
	private ArrayList <Boolean> coregWasIndexed;
	
	// a macro can call to only fuse two images given a certain shift
	private Point3D translation = null;

	private static String[] methodList = {"Average", "Max. Intensity", "Min. Intensity", "Red-Cyan Overlay"};
	private static String[] colorList = {"Red", "Green", "Blue", "Red and Green", "Red and Blue", "Green and Blue", "Red, Green and Blue"};

	public void run(String args)
	{
		// get list of image stacks
		int[] idList = WindowManager.getIDList();

		if (idList == null)
		{
			IJ.error("You need two open image stacks.");
			return;
		}

		int stacks = 0;
		for (int i = 0; i < idList.length; i++)
			if (WindowManager.getImage(idList[i]).getStackSize() > 1)
				stacks++;

		if (stacks < 2)
		{
			IJ.error("You need two open image stacks.");
			return;
		}

		String[] stackList = new String[stacks];
		int[] stackIDs = new int[stacks];
		stacks = 0;

		for (int i = 0; i < idList.length; i++)
		{
			if (WindowManager.getImage(idList[i]).getStackSize() > 1)
			{
				stackList[stacks] = WindowManager.getImage(idList[i]).getTitle();
				stackIDs[stacks] = idList[i];
				++stacks;
			}
		}
		
		// create generic dialog
		GenericDialog gd = new GenericDialog("Stitching of 3D Images");
		gd.addChoice("First_Stack (reference)", stackList, stackList[0]);		
		gd.addChoice("Use_Channel_for_First", colorList, colorList[colorList.length - 1]);
		enableChannelChoice((Choice)gd.getChoices().get(0), (Choice)gd.getChoices().get(1), stackIDs);
		
		gd.addChoice("Second_Stack (to register)", stackList, stackList[1]);
		gd.addChoice("Use_Channel_for_Second", colorList, colorList[colorList.length - 1]);				
		enableChannelChoice((Choice)gd.getChoices().get(2), (Choice)gd.getChoices().get(3), stackIDs);
		
		gd.addCheckbox("Use_Windowing", true);
		gd.addNumericField("Peaks", 5, 0);
		gd.addCheckbox("Create_Fused_Image", true);
		gd.addChoice("Fusion_Method", methodList, methodList[0]);
		gd.addStringField("Fused_Image_Name: ", "Fused_" + stackList[0] + "_" + stackList[1]);
		gd.addCheckbox("Apply_to_other_Channels", false);
		gd.addNumericField("Number_of_other_Channels", 1, 0);
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n"+myURL);
		
		//Component c = gd.getM
		MultiLineLabel text = (MultiLineLabel)gd.getMessage();
		addHyperLinkListener(text);
		
		
		// get Checkboxes
		Component[] c1 = new Component[]{(Component)gd.getChoices().get(4),
				                         (Component)gd.getStringFields().get(0),
				                         (Component)gd.getCheckboxes().get(2), 
				                         (Component)gd.getNumericFields().get(1)};

		Component[] c2 = new Component[]{(Component)gd.getNumericFields().get(1)};		
		((Component)gd.getNumericFields().get(1)).setEnabled(false);

		addEnablerListener((Checkbox)gd.getCheckboxes().get(1), c1, null);
		addEnablerListener((Checkbox)gd.getCheckboxes().get(2), c2, null);
		
		gd.showDialog();

		if (gd.wasCanceled())
			return;

		this.imgStack1 = gd.getNextChoice();
		this.handleRGB1 = gd.getNextChoice();
		this.imgStack2 = gd.getNextChoice();
		this.handleRGB2 = gd.getNextChoice();
		this.imp1 = WindowManager.getImage(stackIDs[((Choice)gd.getChoices().get(0)).getSelectedIndex()]);
		this.imp2 = WindowManager.getImage(stackIDs[((Choice)gd.getChoices().get(2)).getSelectedIndex()]);		
		this.windowing = gd.getNextBoolean();
		this.checkPeaks = (int) gd.getNextNumber();
		this.fuseImages = gd.getNextBoolean();
		this.method = gd.getNextChoice();
		this.fusedImageName = gd.getNextString();
		this.coregister = gd.getNextBoolean();
		this.numberOfChannels = (int)gd.getNextNumber();				
		
		if (stackIDs[((Choice)gd.getChoices().get(0)).getSelectedIndex()] == stackIDs[((Choice)gd.getChoices().get(2)).getSelectedIndex()])
		{
			IJ.error("You selected the same stack twice. Stopping.");
			return;			
		}
		
		if (this.fuseImages )
		{
			if (imp1.getType() != imp2.getType())
			{
				IJ.error("The Image Stacks are of a different type, it is unclear how to fuse them. Stopping.");
				return;
			}
			
			if ((imp1.getType() == ImagePlus.COLOR_RGB || imp1.getType() == ImagePlus.COLOR_256) && this.method.equals(methodList[3]))
			{
				IJ.error("Red-Cyan Overlay is not possible for RGB images, we can do this only with Single Channel data.");
				return;				
			}			
		}
		
		
		if (!this.fuseImages)
			this.coregister = false;

		if (imp1.getType() == ImagePlus.COLOR_256)			
		{
			// convert to RGB
			new StackConverter(imp1).convertToRGB();
			new StackConverter(imp2).convertToRGB();
			
			wasIndexed = true;
		}
		else
		{
			wasIndexed = false;
		}
		
		if (this.coregister)
		{
			if (this.fuseImages == false)
			{
				YesNoCancelDialog error = new YesNoCancelDialog(null, "Error",
						"Co-Registration makes only sense if you want to fuse the image stacks, actually you only get the numbers. Do you want to continue?");

				if (!error.yesPressed())
					return;
				else
					this.coregister = false;

			}
			else if (stackList.length < 3)
			{
				YesNoCancelDialog error = new YesNoCancelDialog(null, "Error", "You have only two stacks open, there is nothing to co-register. Do you want to continue?");

				if (!error.yesPressed())
					return;
				else
					this.coregister = false;
			}
			else if (this.numberOfChannels < 1)
			{

				YesNoCancelDialog error = new YesNoCancelDialog(null, "Error", "You have selected less than 1 stack to co-register...that makes no sense to me. Do you want to continue?");

				if (!error.yesPressed())
					return;
				else
					this.coregister = false;
			}
			else
			{
				GenericDialog coreg = new GenericDialog("Co-Registration");
				for (int i = 0; i < this.numberOfChannels; i++)
				{
					coreg.addMessage("Co-Register Stack #" + (i + 1));
					coreg.addChoice("First_Image_Stack_" + (i + 1) + " (not moved)", stackList, stackList[2]);
					coreg.addChoice("Second_Image_Stack_" + (i + 1) + " (moved)", stackList, stackList[3]);
					coreg.addStringField("Fused_Image_Name_" + (i + 1), "Fused Channel " + (i+2));
					if (i+1 != this.numberOfChannels)
						coreg.addMessage("");
			   }

				coreg.showDialog();

				if (coreg.wasCanceled())
					return;

				this.coregStacks = new ArrayList<String[]>();
				this.coregStackIMPs = new ArrayList<ImagePlus[]>();
				this.coregWasIndexed= new ArrayList<Boolean>();

				for (int i = 0; i < this.numberOfChannels; i++)
				{
					String[] entry = new String[3];
					entry[0] = coreg.getNextChoice();
					entry[1] = coreg.getNextChoice();
					entry[2] = coreg.getNextString();
					
					ImagePlus[] entryIMPs = new ImagePlus[2];
					entryIMPs[0] = WindowManager.getImage(stackIDs[((Choice)coreg.getChoices().get(i*2)).getSelectedIndex()]);
					entryIMPs[1] = WindowManager.getImage(stackIDs[((Choice)coreg.getChoices().get(i*2+1)).getSelectedIndex()]);
					
					if (entryIMPs[0].getType() != entryIMPs[1].getType())
					{
						IJ.error("The Image Stacks (Coreg #"+(i+1)+") are of a different type, it is unclear how to fuse them. Stopping.");
						return;
					}

					if (stackIDs[((Choice)coreg.getChoices().get(i*2)).getSelectedIndex()] == stackIDs[((Choice)coreg.getChoices().get(i*2+1)).getSelectedIndex()])
					{
						IJ.error("You selected the same stack twice (Coreg #"+(i+1)+"). Stopping.");
						return;			
					}

					if (entryIMPs[0].getType() == ImagePlus.COLOR_256)			
					{
						// convert to RGB
						new StackConverter(entryIMPs[0]).convertToRGB();
						new StackConverter(entryIMPs[1]).convertToRGB();
						this.coregWasIndexed.add(true);
					}
					else
					{
						this.coregWasIndexed.add(false);
					}
					
					this.coregStacks.add(entry);
					this.coregStackIMPs.add( entryIMPs );
				}
			}
		}

		IJ.log("(" + new Date(System.currentTimeMillis()) + "):Starting");
		work();
		IJ.log("(" + new Date(System.currentTimeMillis()) + "):Finished");
	}
	
	private final void addHyperLinkListener(final MultiLineLabel text)
	{
		text.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				try
				{
					BrowserLauncher.openURL(myURL);
				}
				catch (Exception ex)
				{
					IJ.error("" + ex);
				}
			}
			public void mouseEntered(MouseEvent e)
			{
				text.setForeground(Color.BLUE);
				text.setCursor(new Cursor(Cursor.HAND_CURSOR));				
			}
			public void mouseExited(MouseEvent e)
			{
				text.setForeground(Color.BLACK);
				text.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}			 
		});
	}
	
	private final void enableChannelChoice(final Choice controller, final Choice target, final int[] stackIDs)	
	{
		setRGB(stackIDs[controller.getSelectedIndex()], target);
		
		controller.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{				
				setRGB(stackIDs[controller.getSelectedIndex()], target);
			}			
		});
	}
	private final void setRGB(int imagejID, final Choice target)
	{
		if (WindowManager.getImage(imagejID).getType() == ImagePlus.COLOR_RGB || WindowManager.getImage(imagejID).getType() == ImagePlus.COLOR_256)
			target.setEnabled(true);
		else
			target.setEnabled(false);			
	}

	private final void addEnablerListener(/*final GenericDialog gd, */final Checkbox master, final Component[] enable, final Component[] disable) 
	{
		master.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent ie) 
			{
				if (ie.getStateChange() == ItemEvent.SELECTED) 
				{
					process(enable, true);
					process(disable, false);
				} else {
					process(enable, false);
					process(disable, true);
				}
			}

			private void process(final Component[] c, final boolean state) 
			{				
				if (null == c)
					return;
				for (int i = 0; i < c.length; i++)
				{
					c[i].setEnabled(state);
					//c[i].setVisible(state);
				}
				//gd.pack();
			}
		});
	}
	
	private ImagePlus getImage(String imgStack)
	{
		ImagePlus imp = null;

		try
		{
			imp = WindowManager.getImage(imgStack);
		} catch (Exception e)
		{
			IJ.error("Could not find image stack: " + e);
		}
		;

		return imp;
	}

	public Stitching_3D()
	{
	}

	public Stitching_3D(boolean windowing, int checkPeaks, boolean fuseImages, String method, String fusedImageName)
	{
		this.windowing = windowing;
		this.fuseImages = fuseImages;
		this.method = method;
		this.fusedImageName = fusedImageName;
		this.checkPeaks = checkPeaks;
	}
	
	private void work()
	{
		work(null, null);
	}

	public void work(FloatArray3D inputImage1, FloatArray3D inputImage2)
	{
		Point3D shift = null;
		ImagePlus imp1, imp2;
		FloatArray3D img1 = null, img2 = null, fft1, fft2;
		CrossCorrelationResult[] result = null;
		
		Point3D img1Dim = new Point3D(0, 0, 0), img2Dim = new Point3D(0, 0, 0), // (incl. possible ext!!!)
				ext1Dim = new Point3D(0, 0, 0), ext2Dim = new Point3D(0, 0, 0),
				maxDim;

		// make it also executable as non-plugin/macro
		if (inputImage1 == null || inputImage2 == null)
		{
			// get images
			if (this.imp1 == null)
				imp1 = getImage(imgStack1);
			else
				imp1 = this.imp1;
			
			if (this.imp2 == null)
				imp2 = getImage(imgStack2);
			else
				imp2 = this.imp2;

			if (imp1 == null || imp2 == null)
			{
				IJ.error("Could not get the image stacks for some unknown reason.");
				return;
			}

			// check for ROIs in images and whether they are valid
			if (!checkRoi(imp1, imp2))
				return;

			// apply ROIs if they are there and save dimensions of original images and size increases
			if (this.translation == null)
			{
				img1 = applyROI(imp1, img1Dim, ext1Dim, this.handleRGB1, windowing);
				img2 = applyROI(imp2, img2Dim, ext2Dim, this.handleRGB2, windowing);				
			}
		}
		else
		{
			img1 = inputImage1;
			img2 = inputImage2;

			imp1 = FloatArrayToStack(img1, "Image1", 0, 0);
			imp2 = FloatArrayToStack(img2, "Image2", 0, 0);

			img1Dim.x = img1.width;
			img1Dim.y = img1.height;
			img1Dim.z = img1.depth;
			img2Dim.x = img2.width;
			img2Dim.y = img2.height;
			img2Dim.z = img2.depth;

		}

		if (this.translation == null)
		{
			// apply windowing
			if (windowing)
			{
				exponentialWindow(img1);
				exponentialWindow(img2);
			}
	
			// zero pad images to fft-able size
			FloatArray3D[] zeropadded = zeroPadImages(img1, img2);
			img1 = zeropadded[0];
			img2 = zeropadded[1];
	
			// save dimensions of zeropadded image
			maxDim = new Point3D(img1.width, img1.height, img1.depth);
	
			// compute FFT's
			fft1 = computeFFT(img1);
			fft2 = computeFFT(img2);
	
			// do the phase correlation
			FloatArray3D invPCM = computePhaseCorrelationMatrix(fft1, fft2, maxDim.x);
	
			// find the peaks
			ArrayList<Point3D> peaks = findPeaks(invPCM, img1Dim, img2Dim, ext1Dim, ext2Dim);
	
			// get the original images
			img1 = applyROI(imp1, img1Dim, ext1Dim, this.handleRGB1, false /*no windowing of course*/);
			img2 = applyROI(imp2, img2Dim, ext2Dim, this.handleRGB2, false /*no windowing of course*/);
	
			// test peaks
			result = testCrossCorrelation(invPCM, peaks, img1, img2);
	
			// delete images from memory
			img1.data = img2.data = null;
			img1 = img2 = null;
	
			// get shift of images relative to each other
			shift = getImageOffset(result[0], imp1, imp2);
		}
		else
		{
			shift = this.translation;			
		}
			

		if (this.fuseImages)
		{
			// merge if wanted
			ImagePlus fused = fuseImages(imp1, imp2, shift, this.method, this.fusedImageName);
			
			if (fused != null)
			{
				if (this.wasIndexed)
					new StackConverter(fused).convertToIndexedColor(256);
					
				fused.show();
			}

			// coregister other channels
			if (this.coregister)
				for (int i = 0; i < this.numberOfChannels; i++)
				{
					String[] stacks = this.coregStacks.get(i);
					ImagePlus[] imps = this.coregStackIMPs.get(i);
					
					// get images
					imp1 = imps[0];
					imp2 = imps[1];

					ImagePlus coregistered = fuseImages(imp1, imp2, shift, this.method, stacks[2]);
					
					if (coregistered != null)
					{
						if (this.coregWasIndexed.get(i))
							new StackConverter(fused).convertToIndexedColor(256);

						coregistered.show();
					}
				}


		}

		IJ.log("Translation Parameters:");
		IJ.log("(second stack relative to first stack)");
		IJ.log("x=" + shift.x + " y=" + shift.y + " z=" + shift.z + " R=" + result[0].R);
	}

	private Point3D getImageOffset(CrossCorrelationResult result, ImagePlus imp1, ImagePlus imp2)
	{
		// see "ROI shift to Image Shift.ppt" for details of nomenclature
		Point3D r1, r2, sr, sir, si;

		// relative shift between rois (all shifts relative to upper left front corner)
		sr = result.shift;

		if (imp1.getRoi() == null || imp2.getRoi() == null)
		{
			// there are no rois....so we already have the relative shift between the images
			si = sr;
		}
		else
		{
			Roi roi1 = imp1.getRoi();
			Roi roi2 = imp2.getRoi();

			int x1 = roi1.getBoundingRect().x;
			int y1 = roi1.getBoundingRect().y;
			int x2 = roi2.getBoundingRect().x;
			int y2 = roi2.getBoundingRect().y;

			r1 = new Point3D(x1, y1, 0);
			r2 = new Point3D(x2, y2, 0);

			sir = add(r1, sr);
			si = subtract(sir, r2);
		}

		return si;
	}

	private Point3D subtract(Point3D a, Point3D b)
	{
		return new Point3D(a.x - b.x, a.y - b.y, a.z - b.z);
	}

	private Point3D add(Point3D a, Point3D b)
	{
		return new Point3D(a.x + b.x, a.y + b.y, a.z + b.z);
	}

	final private float getPixelMin(final int imageType, final Object[] imageStack, final int width, final int height, final int depth, 
			final int x, final int y, final int z, final double min)
	{
		if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth)
			return (float) min;

		if (imageType == ImagePlus.GRAY8)
		{
			byte[] pixelTmp = (byte[]) imageStack[z];
			return (float) (pixelTmp[x + y * width] & 0xff);
		}
		else if (imageType == ImagePlus.GRAY16)
		{
			short[] pixelTmp = (short[]) imageStack[z];
			return (float) (pixelTmp[x + y * width] & 0xffff);
		}
		else // instance of float[]
		{
			float[] pixelTmp = (float[]) imageStack[z];
			return pixelTmp[x + y * width];
		}
	}

	final private int[] getPixelMinRGB(final Object[] imageStack, final int width, final int height, final int depth, 
			final int x, final int y, final int z, final double min)
	{
		final int[] rgb = new int[3];
		
		if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth)
		{
			rgb[0] = rgb[1] = rgb[2] = (int)min;
			return rgb;
		}

		int[] pixelTmp = (int[]) imageStack[z];
		int color = (pixelTmp[x + y * width] & 0xffffff);
		
		rgb[0] = (color&0xff0000)>>16;
        rgb[1] = (color&0xff00)>>8;
        rgb[2] = color&0xff;
        
        return rgb;
	}

	private ImagePlus fuseImages(ImagePlus imp1, ImagePlus imp2, Point3D shift, String method, String name)
	{
		Object[] imageStack1 = imp1.getStack().getImageArray();
		int w1 = imp1.getStack().getWidth();
		int h1 = imp1.getStack().getHeight();
		int d1 = imp1.getStack().getSize();

		Object[] imageStack2 = imp2.getStack().getImageArray();
		int w2 = imp2.getStack().getWidth();
		int h2 = imp2.getStack().getHeight();
		int d2 = imp2.getStack().getSize();

		int sx = shift.x;
		int sy = shift.y;
		int sz = shift.z;

		int imgW, imgH, imgD;

		if (sx >= 0)
			imgW = Math.max(w1, w2 + sx);
		else
			imgW = Math.max(w1 - sx, w2); // equals max(w1 + Math.abs(sx), w2);

		if (sy >= 0)
			imgH = Math.max(h1, h2 + sy);
		else
			imgH = Math.max(h1 - sy, h2);

		if (sz >= 0)
			imgD = Math.max(d1, d2 + sz);
		else
			imgD = Math.max(d1 - sz, d2);

		int offsetImg1X = Math.max(0, -sx); // + max(0, max(0, -sx) - max(0, (w1 - w2)/2));
		int offsetImg1Y = Math.max(0, -sy); // + max(0, max(0, -sy) - max(0, (h1 - h2)/2));
		int offsetImg1Z = Math.max(0, -sz); // + max(0, max(0, -sy) - max(0, (h1 - h2)/2));
		int offsetImg2X = Math.max(0, sx); // + max(0, max(0, sx) - max(0, (w2 - w1)/2));
		int offsetImg2Y = Math.max(0, sy); // + max(0, max(0, sy) - max(0, (h2 - h1)/2));
		int offsetImg2Z = Math.max(0, sz); // + max(0, max(0, sy) - max(0, (h2 - h1)/2));

		int type = 0;		

		if (method.equals("Average"))
			type = 0;
		else if (method.equals("Max. Intensity"))
			type = 1;
		else if (method.equals("Min. Intensity"))
			type = 2;
		else if (method.equals("Red-Cyan Overlay"))
			type = 3;
				
		if (imp1.getType() != imp2.getType())
		{
			IJ.error("Image Types changed, they are not the same. Cannot fuse them");
			return null;
		}

		int imageType = imp1.getType();

		//FloatArray3D fused = null;
		ImagePlus fusedImp = null;
		ImageStack fusedStack = null;
		ImageProcessor fusedIp = null;
		float pixel1 = 0, pixel2 = 0;
		int[] pixel1rgb = null, pixel2rgb = null;

		// get min and max intensity of both stacks
		double min1 = imp1.getStack().getProcessor(1).getMin();
		double max1 = imp1.getStack().getProcessor(1).getMax();

		double min2 = imp2.getStack().getProcessor(1).getMin();
		double max2 = imp2.getStack().getProcessor(1).getMax();

		for (int stack = 2; stack <= d1; stack++)
		{
			if (imp1.getStack().getProcessor(stack).getMin() < min1)
				min1 = imp1.getStack().getProcessor(stack).getMin();

			if (imp1.getStack().getProcessor(stack).getMax() < max1)
				max1 = imp1.getStack().getProcessor(stack).getMax();
		}

		for (int stack = 2; stack <= d2; stack++)
		{
			if (imp2.getStack().getProcessor(stack).getMin() < min2)
				min2 = imp2.getStack().getProcessor(stack).getMin();

			if (imp2.getStack().getProcessor(stack).getMax() < max2)
				max2 = imp2.getStack().getProcessor(stack).getMax();
		}

		if (type != 3)
		{
			if (imageType == ImagePlus.GRAY8)
				fusedImp = IJ.createImage(name, "8-bit black", imgW, imgH, imgD);
			else if (imageType == ImagePlus.GRAY16)
				fusedImp = IJ.createImage(name, "16-bit black", imgW, imgH, imgD);
			else if (imageType == ImagePlus.GRAY32)
				fusedImp = IJ.createImage(name, "32-bit black", imgW, imgH, imgD);
			else if (imageType == ImagePlus.COLOR_RGB)
				fusedImp = IJ.createImage(name, "rgb black", imgW, imgH, imgD);
			else
			{
				IJ.error("Unknown Image Type: " + imageType);
				return null;
			}
		}
		else
		{			
			fusedImp = IJ.createImage(name, "rgb black", imgW, imgH, imgD);
		}

		fusedStack = fusedImp.getStack();

		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		float pixel3 = 0;
		int[] pixel3rgb = new int[3];
		int[] iArray = new int[3];

		for (int z = 0; z < imgD; z++)
		{
			fusedIp = fusedStack.getProcessor(z + 1);

			for (int y = 0; y < imgH; y++)
				for (int x = 0; x < imgW; x++)
				{
					if (imageType == ImagePlus.COLOR_RGB)
					{
						pixel1rgb = getPixelMinRGB(imageStack1, w1, h1, d1, x - offsetImg1X, y - offsetImg1Y, z - offsetImg1Z, min1);
						pixel2rgb = getPixelMinRGB(imageStack2, w2, h2, d2, x - offsetImg2X, y - offsetImg2Y, z - offsetImg2Z, min2);						
					}
					else
					{
						pixel1 = getPixelMin(imageType, imageStack1, w1, h1, d1, x - offsetImg1X, y - offsetImg1Y, z - offsetImg1Z, min1);
						pixel2 = getPixelMin(imageType, imageStack2, w2, h2, d2, x - offsetImg2X, y - offsetImg2Y, z - offsetImg2Z, min2);
					}
					
					if (type != 3)
					{
						// combine images if overlapping
						if (x >= offsetImg1X && x >= offsetImg2X &&
							x < offsetImg1X + w1 && x < offsetImg2X + w2 &&
							y >= offsetImg1Y && y >= offsetImg2Y &&
							y < offsetImg1Y + h1 && y < offsetImg2Y + h2 &&
							z >= offsetImg1Z && z >= offsetImg2Z &&
							z < offsetImg1Z + d1 && z < offsetImg2Z + d2)
						{
							pixel3 = 0;
							pixel3rgb = new int[3];

							if (imageType == ImagePlus.COLOR_RGB)
							{
								if (type == 0)
								{
									pixel3rgb[0] = (int)((pixel1rgb[0] + pixel2rgb[0]) / 2.0 + 0.5);
									pixel3rgb[1] = (int)((pixel1rgb[1] + pixel2rgb[1]) / 2.0 + 0.5);
									pixel3rgb[2] = (int)((pixel1rgb[2] + pixel2rgb[2]) / 2.0 + 0.5);
								}
								else if (type == 1)
								{
									pixel3rgb[0] = Math.max(pixel1rgb[0], pixel2rgb[0]);
									pixel3rgb[1] = Math.max(pixel1rgb[1], pixel2rgb[1]);
									pixel3rgb[2] = Math.max(pixel1rgb[2], pixel2rgb[2]);
								}
								else if (type == 2)
								{
									pixel3rgb[0] = Math.min(pixel1rgb[0], pixel2rgb[0]);
									pixel3rgb[1] = Math.min(pixel1rgb[1], pixel2rgb[1]);
									pixel3rgb[2] = Math.min(pixel1rgb[2], pixel2rgb[2]);
								}
							}
							else
							{
								if (type == 0)
									pixel3 = (pixel1 + pixel2) / 2f;
								else if (type == 1)
									pixel3 = Math.max(pixel1, pixel2);
								else if (type == 2)
									pixel3 = Math.min(pixel1, pixel2);								
							}
						}
						else
						{
							if (imageType == ImagePlus.COLOR_RGB)
							{
								pixel3rgb[0] = Math.max(pixel1rgb[0], pixel2rgb[0]);
								pixel3rgb[1] = Math.max(pixel1rgb[1], pixel2rgb[1]);
								pixel3rgb[2] = Math.max(pixel1rgb[2], pixel2rgb[2]);								
							}	
							else
								pixel3 = Math.max(pixel1, pixel2);
						}

						if (imageType == ImagePlus.COLOR_RGB)
							fusedIp.putPixel(x, y, pixel3rgb);
						else if (imageType == ImagePlus.GRAY8 || imageType == ImagePlus.GRAY16)
							fusedIp.putPixel(x, y, (int)(pixel3+0.5));
						else
							fusedIp.putPixelValue(x, y, pixel3);

						if (pixel3 < min)
							min = pixel3;
						else if (pixel3 > max)
							max = pixel3;
					}
					else
					{
						iArray[0] = (int) (((pixel1 - min1) / (max1 - min1)) * 255D);
						iArray[1] = iArray[2] = (int) (((pixel2 - min2) / (max2 - min2)) * 255D);
						fusedIp.putPixel(x, y, iArray);
					}
				}
		}
		
		// adjust contrast for images with colordepth higher than 8 Bit
		if (imageType == ImagePlus.GRAY16 || imageType == ImagePlus.GRAY32)
			fusedImp.getProcessor().setMinAndMax(min, max);
		

		/*if (type != 3)
		{
			fusedImp = FloatArrayToStack(fused, name, min, max);
			fused.data = null;
			fused = null;			
		}*/

		return fusedImp;
	}

	private CrossCorrelationResult[] testCrossCorrelation(FloatArray3D invPCM, ArrayList <Point3D>peaks,
			final FloatArray3D img1, final FloatArray3D img2)
	{
		final int numBestHits = peaks.size();
		final int numCases = 8;
		final CrossCorrelationResult result[] = new CrossCorrelationResult[numBestHits * numCases];

		int count = 0;
		int w = invPCM.width;
		int h = invPCM.height;
		int d = invPCM.depth;

		final Point3D[] points = new Point3D[numCases];

		for (int hit = 0; count < numBestHits * numCases; hit++)
		{
			points[0] = peaks.get(hit);

			if (points[0].x < 0)
				points[1] = new Point3D(points[0].x + w, points[0].y, points[0].z);
			else
				points[1] = new Point3D(points[0].x - w, points[0].y, points[0].z);

			if (points[0].y < 0)
				points[2] = new Point3D(points[0].x, points[0].y + h, points[0].z);
			else
				points[2] = new Point3D(points[0].x, points[0].y - h, points[0].z);

			if (points[0].z < 0)
				points[3] = new Point3D(points[0].x, points[0].y, points[0].z + d);
			else
				points[3] = new Point3D(points[0].x, points[0].y, points[0].z - d);

			points[4] = new Point3D(points[1].x, points[2].y, points[0].z);
			points[5] = new Point3D(points[0].x, points[2].y, points[3].z);
			points[6] = new Point3D(points[1].x, points[0].y, points[3].z);
			points[7] = new Point3D(points[1].x, points[2].y, points[3].z);

			final AtomicInteger entry = new AtomicInteger(count);
			final AtomicInteger shift = new AtomicInteger(0);

			Runnable task = new Runnable()
			{
				public void run()
				{
					try
					{
						int myEntry = entry.getAndIncrement();
						int myShift = shift.getAndIncrement();
						result[myEntry] = testCrossCorrelation(points[myShift], img1, img2);
					} catch (Exception e)
					{
						e.printStackTrace();
						IJ.log(e.getMessage());
					}
				}
			};

			startTask(task, numCases);

			count += numCases;
		}

		quicksort(result, 0, result.length - 1);

		for (int i = 0; i < result.length; i++)
			IJ.log("x:" + result[i].shift.x + " y:" + result[i].shift.y + " z:" + result[i].shift.z + " overlap:" + result[i].overlappingPixels + " R:" + result[i].R + " Peak:" + result[i].PCMValue);

		return result;
	}

	private CrossCorrelationResult testCrossCorrelation(Point3D shift, FloatArray3D img1, FloatArray3D img2)
	{
		// init Result Datastructure
		CrossCorrelationResult result = new CrossCorrelationResult();

		// compute values that will not change during testing
		result.PCMValue = shift.value;
		result.shift = shift;

		int w1 = img1.width;
		int h1 = img1.height;
		int d1 = img1.depth;

		int w2 = img2.width;
		int h2 = img2.height;
		int d2 = img2.depth;

		int sx = shift.x;
		int sy = shift.y;
		int sz = shift.z;

		int offsetImg1X = Math.max(0, -sx); // + max(0, max(0, -sx) - max(0, (w1 - w2)/2));
		int offsetImg1Y = Math.max(0, -sy); // + max(0, max(0, -sy) - max(0, (h1 - h2)/2));
		int offsetImg1Z = Math.max(0, -sz); // + max(0, max(0, -sy) - max(0, (h1 - h2)/2));
		int offsetImg2X = Math.max(0, sx); // + max(0, max(0, sx) - max(0, (w2 - w1)/2));
		int offsetImg2Y = Math.max(0, sy); // + max(0, max(0, sy) - max(0, (h2 - h1)/2));
		int offsetImg2Z = Math.max(0, sz); // + max(0, max(0, sy) - max(0, (h2 - h1)/2));

		int count = 0;
		float pixel1, pixel2;

		// iterate over overlapping region
		// first the average

		double avg1 = 0, avg2 = 0;

		int startX = Math.max(offsetImg1X, offsetImg2X);
		int startY = Math.max(offsetImg1Y, offsetImg2Y);
		int startZ = Math.max(offsetImg1Z, offsetImg2Z);
		int endX = Math.min(offsetImg1X + w1, offsetImg2X + w2);
		int endY = Math.min(offsetImg1Y + h1, offsetImg2Y + h2);
		int endZ = Math.min(offsetImg1Z + d1, offsetImg2Z + d2);

		// for direct array addressing
		int arrayOffsetY1 = img1.getPos(0, 1, 0);
		int arrayOffsetY2 = img2.getPos(0, 1, 0);
		int arrayOffsetZ1 = img1.getPos(0, 0, 1);
		int arrayOffsetZ2 = img2.getPos(0, 0, 1);

		int off1 = img1.getPos(startX - offsetImg1X, startY - offsetImg1Y, startZ - offsetImg1Z);
		int off2 = img2.getPos(startX - offsetImg2X, startY - offsetImg2Y, startZ - offsetImg2Z);
		int oldZ1, oldZ2, oldY1, oldY2;

		for (int z = startZ; z < endZ; z++)
		{
			oldZ1 = off1;
			oldZ2 = off2;

			for (int y = startY; y < endY; y++)
			{
				oldY1 = off1;
				oldY2 = off2;

				for (int x = startX; x < endX; x++)
				{
					//pixel1 = img1.getZero(x - offsetImg1X, y - offsetImg1Y, z - offsetImg1Z);
					//pixel2 = img2.getZero(x - offsetImg2X, y - offsetImg2Y, z - offsetImg2Z);
					pixel1 = img1.data[off1++];
					pixel2 = img2.data[off2++];

					avg1 += pixel1;
					avg2 += pixel2;
					count++;
				}

				off1 = oldY1 + arrayOffsetY1;
				off2 = oldY2 + arrayOffsetY2;
			}

			off1 = oldZ1 + arrayOffsetZ1;
			off2 = oldZ2 + arrayOffsetZ2;
		}

		// if less than 1% is overlapping
		if (count <= (Math.min(w1 * h1 * d1, w2 * h2 * d2)) * 0.01)
		{
			//IJ.log("lower than 1%");
			result.R = 0;
			result.SSQ = Float.MAX_VALUE;
			result.overlappingPixels = count;
			return result;
		}

		avg1 /= (double) count;
		avg2 /= (double) count;

		double var1 = 0, var2 = 0;
		double coVar = 0;
		double dist1, dist2;
		double SSQ = 0;
		double pixelSSQ;

		count = 0;
		off1 = img1.getPos(startX - offsetImg1X, startY - offsetImg1Y, startZ - offsetImg1Z);
		off2 = img2.getPos(startX - offsetImg2X, startY - offsetImg2Y, startZ - offsetImg2Z);

		for (int z = startZ; z < endZ; z++)
		{
			oldZ1 = off1;
			oldZ2 = off2;

			for (int y = startY; y < endY; y++)
			{
				oldY1 = off1;
				oldY2 = off2;

				for (int x = startX; x < endX; x++)
				{
					//pixel1 = img1.getZero(x - offsetImg1X, y - offsetImg1Y, z - offsetImg1Z);
					//pixel2 = img2.getZero(x - offsetImg2X, y - offsetImg2Y, z - offsetImg2Z);
					pixel1 = img1.data[off1++];
					pixel2 = img2.data[off2++];

					pixelSSQ = Math.pow(pixel1 - pixel2, 2);
					SSQ += pixelSSQ;
					count++;

					dist1 = pixel1 - avg1;
					dist2 = pixel2 - avg2;

					coVar += dist1 * dist2;
					var1 += dist1 * dist1;
					var2 += dist2 * dist2;
				}

				off1 = oldY1 + arrayOffsetY1;
				off2 = oldY2 + arrayOffsetY2;
			}

			off1 = oldZ1 + arrayOffsetZ1;
			off2 = oldZ2 + arrayOffsetZ2;
		}

		SSQ /= (double) count;
		var1 /= (double) count;
		var2 /= (double) count;
		coVar /= (double) count;

		double stDev1 = Math.sqrt(var1);
		double stDev2 = Math.sqrt(var2);

		// all pixels had the same color....
		if (stDev1 == 0 || stDev2 == 0)
		{
			result.R = 0;
			result.SSQ = Float.MAX_VALUE;
			result.overlappingPixels = count;
			return result;
		}

		// compute correlation coeffienct
		result.R = coVar / (stDev1 * stDev2);
		result.SSQ = SSQ;
		result.overlappingPixels = count;
		result.shift = shift;

		//IJ.log("returning result");

		return result;
	}

	private ArrayList<Point3D> findPeaks(FloatArray3D invPCM, Point3D img1, Point3D img2, Point3D ext1, Point3D ext2)
	{
		int w = invPCM.width;
		int h = invPCM.height;
		int d = invPCM.depth;

		int xs, ys, zs, xt, yt, zt;
		float value;

		ArrayList<Point3D> peaks = new ArrayList<Point3D>();

		for (int j = 0; j < checkPeaks; j++)
			peaks.add(new Point3D(0, 0, 0, Float.MIN_VALUE));

		for (int z = 0; z < d; z++)
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					if (isLocalMaxima(invPCM, x, y, z))
					{
						value = invPCM.get(x, y, z);
						Point3D insert = null;
						int insertPos = -1;

						Iterator<Point3D> i = peaks.iterator();
						boolean wasBigger = true;

						while (i.hasNext() && wasBigger)
						{
							if (value > i.next().value)
							{
								if (insert == null)
									insert = new Point3D(0, 0, 0, value);

								insertPos++;
							}
							else
								wasBigger = false;
						}

						if (insertPos >= 0)
							peaks.add(insertPos + 1, insert);

						// remove lowest peak
						if (peaks.size() > checkPeaks)
							peaks.remove(0);

						if (insert != null)
						{

							// find relative to the left upper front corners of both images
							xt = x + (img1.x - img2.x) / 2 - (ext1.x - ext2.x) / 2;

							if (xt >= w / 2)
								xs = xt - w;
							else
								xs = xt;

							yt = y + (img1.y - img2.y) / 2 - (ext1.y - ext2.y) / 2;

							if (yt >= h / 2)
								ys = yt - h;
							else
								ys = yt;

							zt = z + (img1.z - img2.z) / 2 - (ext1.z - ext2.z) / 2;

							if (zt >= d / 2)
								zs = zt - d;
							else
								zs = zt;

							insert.x = xs;
							insert.y = ys;
							insert.z = zs;
						}
					}

		return peaks;
	}

	private boolean isLocalMaxima(FloatArray3D invPCM, int x, int y, int z)
	{
		int width = invPCM.width;
		int height = invPCM.height;
		int depth = invPCM.depth;

		boolean isMax = true;
		float value = invPCM.get(x, y, z);

		if (x > 0 && y > 0 && z > 0 && x < width - 1 && y < height - 1 && z < depth - 1)
		{
			for (int xs = x - 1; xs <= x + 1 && isMax; xs++)
				for (int ys = y - 1; ys <= y + 1 && isMax; ys++)
					for (int zs = z - 1; zs <= z + 1 && isMax; zs++)
						if (!(x == xs && y == ys && z == zs))
							if (invPCM.get(xs, ys, zs) > value)
								isMax = false;
		}
		else
		{
			int xt, yt, zt;

			for (int xs = x - 1; xs <= x + 1 && isMax; xs++)
				for (int ys = y - 1; ys <= y + 1 && isMax; ys++)
					for (int zs = z - 1; zs <= z + 1 && isMax; zs++)
						if (!(x == xs && y == ys && z == zs))
						{
							xt = xs;
							yt = ys;
							zt = zs;

							if (xt == -1) xt = width - 1;
							if (yt == -1) yt = height - 1;
							if (zt == -1) zt = depth - 1;

							if (xt == width) xt = 0;
							if (yt == height) yt = 0;
							if (zt == depth) zt = 0;

							if (invPCM.get(xt, yt, zt) > value)
								isMax = false;
						}
		}

		return isMax;
	}

	private FloatArray3D computePhaseCorrelationMatrix(FloatArray3D fft1, FloatArray3D fft2, int width)
	{
		//
		// Do Phase Correlation
		//

		FloatArray3D pcm = new FloatArray3D(computePhaseCorrelationMatrix(fft1.data, fft2.data, false), fft1.width, fft1.height, fft1.depth);

		fft1.data = fft2.data = null;
		fft1 = fft2 = null;

		FloatArray3D ipcm = pffftInv3DMT(pcm, width);

		pcm.data = null;
		pcm = null;

		return ipcm;
	}

	private FloatArray3D computeFFT(FloatArray3D img)
	{
		FloatArray3D fft = pffft3DMT(img, false);
		//img.data = null; img = null;

		return fft;
	}

	private FloatArray3D[] zeroPadImages(FloatArray3D img1, FloatArray3D img2)
	{
		int width = Math.max(img1.width, img2.width);
		int height = Math.max(img1.height, img2.height);
		int depth = Math.max(img1.depth, img2.depth);

		int widthFFT = FftReal.nfftFast(width);
		int heightFFT = FftComplex.nfftFast(height);
		int depthFFT = FftComplex.nfftFast(depth);

		FloatArray3D[] result = new FloatArray3D[2];

		result[0] = zeroPad(img1, widthFFT, heightFFT, depthFFT);
		img1.data = null;
		img1 = null;

		result[1] = zeroPad(img2, widthFFT, heightFFT, depthFFT);
		img2.data = null;
		img2 = null;

		return result;
	}

	private FloatArray3D applyROI(ImagePlus imp, Point3D imgDim, Point3D extDim, String handleRGB, boolean windowing)
	{
		FloatArray3D stack;

		if (imp.getRoi() == null)
		{
			// there are no rois....
			stack = StackToFloatArray(imp.getStack(), handleRGB);
		}
		else
		{
			Roi r = imp.getRoi();

			int x = r.getBoundingRect().x;
			int y = r.getBoundingRect().y;
			int w = r.getBoundingRect().width;
			int h = r.getBoundingRect().height;
			
			stack = StackToFloatArray(imp.getStack(), handleRGB, x, y, w, h);
		}

		imgDim.x = stack.width;
		imgDim.y = stack.height;
		imgDim.z = stack.depth;

		extDim.x = extDim.y = extDim.z = 0;

		if (windowing)
		{
			int imgW = stack.width;
			int imgH = stack.height;
			int imgD = stack.depth;
			int extW = imgW / 4;
			int extH = imgH / 4;
			int extD = imgD / 4;

			// add an even number so that both sides extend equally
			if (extW % 2 != 0) extW++;
			if (extH % 2 != 0) extH++;
			if (extD % 2 != 0) extD++;

			extDim.x = extW;
			extDim.y = extH;
			extDim.z = extD;

			imgDim.x += extDim.x;
			imgDim.y += extDim.y;
			imgDim.z += extDim.z;

			// extend images
			stack = extendImageMirror(stack, imgW + extW, imgH + extH, imgD + extD);
		}
		return stack;
	}

	private boolean checkRoi(ImagePlus imp1, ImagePlus imp2)
	{
		boolean img1HasROI, img2HasROI;

		if (imp1.getRoi() != null)
			img1HasROI = true;
		else
			img1HasROI = false;

		if (imp2.getRoi() != null)
			img2HasROI = true;
		else
			img2HasROI = false;

		if (img1HasROI && !img2HasROI || img2HasROI && !img1HasROI)
		{
			IJ.error("Either both images should have a ROI or none of them.");
			return false;
		}

		if (img1HasROI)
		{
			int type1 = imp1.getRoi().getType();
			int type2 = imp2.getRoi().getType();

			if (type1 != Roi.RECTANGLE)
			{
				IJ.error(imp1.getTitle() + " has a ROI which is no rectangle.");
			}

			if (type2 != Roi.RECTANGLE)
			{
				IJ.error(imp2.getTitle() + " has a ROI which is no rectangle.");
			}
		}

		return true;
	}


	/*
	  -----------------------------------------------------------------------------------------
	  Here are the general static methods needed by the plugin
	  Just to pronouce that they are not directly related, I put them in a seperate child class
	  -----------------------------------------------------------------------------------------
	 */

	private void exponentialWindow(FloatArray3D img)
	{
		double a = 1000;

		// create lookup table
		double weightsX[] = new double[img.width];
		double weightsY[] = new double[img.height];
		double weightsZ[] = new double[img.depth];

		for (int x = 0; x < img.width; x++)
		{
			double relPos = (double) x / (double) (img.width - 1);

			if (relPos <= 0.5)
				weightsX[x] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsX[x] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int y = 0; y < img.height; y++)
		{
			double relPos = (double) y / (double) (img.height - 1);

			if (relPos <= 0.5)
				weightsY[y] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsY[y] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int z = 0; z < img.depth; z++)
		{
			double relPos = (double) z / (double) (img.depth - 1);

			if (relPos <= 0.5)
				weightsZ[z] = 1.0 - (1.0 / (Math.pow(a, (relPos * 2))));
			else
				weightsZ[z] = 1.0 - (1.0 / (Math.pow(a, ((1 - relPos) * 2))));
		}

		for (int z = 0; z < img.depth; z++)
			for (int y = 0; y < img.height; y++)
				for (int x = 0; x < img.width; x++)
					img.set((float) (img.get(x, y, z) * weightsX[x] * weightsY[y] * weightsZ[z]), x, y, z);
	}

	private FloatArray3D extendImageMirror(FloatArray3D ip, int width, int height, int depth)
	{
		FloatArray3D image = new FloatArray3D(width, height, depth);

		int offsetX = (width - ip.width) / 2;
		int offsetY = (height - ip.height) / 2;
		int offsetZ = (depth - ip.depth) / 2;

		if (offsetX < 0)
		{
			IJ.error("Stitching_3D.extendImageMirror(): Extended size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			IJ.error("Stitching_3D.extendImageMirror(): Extended size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		if (offsetZ < 0)
		{
			IJ.error("Stitching_3D.extendImageMirror(): Extended size in Z smaller than image! " + depth + " < " + ip.depth);
			return null;
		}

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < depth; z++)
					image.set(ip.getMirror(x - offsetX, y - offsetY, z - offsetZ), x, y, z);

		return image;
	}

	private FloatArray3D StackToFloatArray(ImageStack stack, String handleRGB)
	{
		Object[] imageStack = stack.getImageArray();
		int width = stack.getWidth();
		int height = stack.getHeight();
		int nstacks = stack.getSize();
		
		int rgbType = -1;

		if (imageStack == null || imageStack.length == 0)
		{
			System.out.println("Image Stack is empty.");
			return null;
		}

		if (imageStack[0] instanceof int[])
		{
			if (handleRGB == null || handleRGB.trim().length() == 0)
				handleRGB = colorList[colorList.length - 1];
			
			for (int i = 0; i < colorList.length; i++)
			{
				if (handleRGB.toLowerCase().trim().equals(colorList[i].toLowerCase()))
					rgbType = i;
			}
			
			if (rgbType == -1)
			{
				System.err.println("Unrecognized command to handle RGB: " + handleRGB + ". Assuming Average of Red, Green and Blue.");
				rgbType = colorList.length - 1;
			}
		}

		FloatArray3D pixels = new FloatArray3D(width, height, nstacks);
		int count;


		if (imageStack[0] instanceof byte[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				byte[] pixelTmp = (byte[])imageStack[countSlice];
				count = 0;

				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
						pixels.data[pixels.getPos(x,y,countSlice)] = (float)(pixelTmp[count++] & 0xff);
			}
		else if (imageStack[0] instanceof short[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				short[] pixelTmp = (short[])imageStack[countSlice];
				count = 0;

				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
						pixels.data[pixels.getPos(x,y,countSlice)] = (float)(pixelTmp[count++] & 0xffff);
			}
		else if (imageStack[0] instanceof float[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				float[] pixelTmp = (float[])imageStack[countSlice];
				count = 0;

				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
						pixels.data[pixels.getPos(x,y,countSlice)] = pixelTmp[count++];
			}
		else if (imageStack[0] instanceof int[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				int[] pixelTmp = (int[])imageStack[countSlice];
				count = 0;
				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
						pixels.data[pixels.getPos(x,y,countSlice)] = getPixelValueRGB(pixelTmp[count++], rgbType);				
			}
		else
		{
			IJ.error("StackToFloatArray: Unknown image type.");
			return null;
		}


		return pixels;
	}
	
	private float getPixelValueRGB(int rgb, int rgbType)
	{
		int r = (rgb&0xff0000)>>16;
        int g = (rgb&0xff00)>>8;
        int b = rgb&0xff;
        
        //colorList = {"Red", "Green", "Blue", "Red and Green", "Red and Blue", "Green and Blue", "Red, Green and Blue"};

        if (rgbType == 0)
        	return r;
        else if (rgbType == 1)
        	return g;
        else if (rgbType == 2)
        	return b;
        else if (rgbType == 3)
        	return (r+g)/2.0f;
        else if (rgbType == 4)
        	return (r+b)/2.0f;
        else if (rgbType == 5)
        	return (g+b)/2.0f;
        else
        	return (r+g+b)/3.0f;
	}

	private FloatArray3D StackToFloatArray(ImageStack stack, String handleRGB, int xCrop, int yCrop, int wCrop, int hCrop)
	{
		Object[] imageStack = stack.getImageArray();
		int width = stack.getWidth();
		int nstacks = stack.getSize();
		
		int rgbType = -1;

		if (imageStack == null || imageStack.length == 0)
		{
			System.out.println("Image Stack is empty.");
			return null;
		}

		if (imageStack[0] instanceof int[])
		{
			if (handleRGB == null || handleRGB.trim().length() == 0)
				handleRGB = colorList[colorList.length - 1];
			
			for (int i = 0; i < colorList.length; i++)
			{
				if (handleRGB.toLowerCase().trim().equals(colorList[i].toLowerCase()))
					rgbType = i;
			}
			
			if (rgbType == -1)
			{
				System.err.println("Unrecognized command to handle RGB: " + handleRGB + ". Assuming Average of Red, Green and Blue.");
				rgbType = colorList.length - 1;
			}
		}

		FloatArray3D pixels = new FloatArray3D(wCrop, hCrop, nstacks);

		if (imageStack[0] instanceof byte[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				byte[] pixelTmp = (byte[])imageStack[countSlice];

				for (int y = yCrop; y < yCrop + hCrop; y++)
					for (int x = xCrop; x < xCrop + wCrop; x++)
						pixels.data[pixels.getPos(x - xCrop, y - yCrop, countSlice)] = (float) (pixelTmp[x + y * width] & 0xff);
			}
		else if (imageStack[0] instanceof short[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				short[] pixelTmp = (short[])imageStack[countSlice];

				for (int y = yCrop; y < yCrop + hCrop; y++)
					for (int x = xCrop; x < xCrop + wCrop; x++)
						pixels.data[pixels.getPos(x - xCrop, y - yCrop, countSlice)] = (float) (pixelTmp[x + y * width] & 0xffff);
			}
		else if (imageStack[0] instanceof float[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				float[] pixelTmp = (float[])imageStack[countSlice];

				for (int y = yCrop; y < yCrop + hCrop; y++)
					for (int x = xCrop; x < xCrop + wCrop; x++)
						pixels.data[pixels.getPos(x - xCrop, y - yCrop, countSlice)] = pixelTmp[x + y * width];
			}
		else if (imageStack[0] instanceof int[])
			for (int countSlice = 0; countSlice < nstacks; countSlice++)
			{
				int[] pixelTmp = (int[])imageStack[countSlice];

				for (int y = yCrop; y < yCrop + hCrop; y++)
					for (int x = xCrop; x < xCrop + wCrop; x++)
						pixels.data[pixels.getPos(x - xCrop, y - yCrop, countSlice)] = getPixelValueRGB(pixelTmp[x + y*width], rgbType);				
			}
		else
		{
			IJ.error("StackToFloatArray(Crop): Unknown image type.");
			return null;
		}


		return pixels;
	}

	private ImagePlus FloatArrayToStack(FloatArray3D image, String name, float min, float max)
	{
		int width = image.width;
		int height = image.height;
		int nstacks = image.depth;

		ImageStack stack = new ImageStack(width, height);

		for (int slice = 0; slice < nstacks; slice++)
		{
			ImagePlus impResult = IJ.createImage("Result", "32-Bit Black", width, height, 1);
			ImageProcessor ipResult = impResult.getProcessor();
			float[] sliceImg = new float[width * height];

			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++)
					sliceImg[y * width + x] = image.get(x, y, slice);

			ipResult.setPixels(sliceImg);

			if (min == max)
				ipResult.resetMinAndMax();
			else
				ipResult.setMinAndMax(min, max);

			stack.addSlice("Slice " + slice, ipResult);
		}

		return new ImagePlus(name, stack);
	}

	private FloatArray3D zeroPad(FloatArray3D ip, int width, int height, int depth)
	{
		FloatArray3D image = new FloatArray3D(width, height, depth);

		int offsetX = (width - ip.width) / 2;
		int offsetY = (height - ip.height) / 2;
		int offsetZ = (depth - ip.depth) / 2;

		if (offsetX < 0)
		{
			IJ.error("Stitching_3D.ZeroPad(): Zero-Padding size in X smaller than image! " + width + " < " + ip.width);
			return null;
		}

		if (offsetY < 0)
		{
			IJ.error("Stitching_3D.ZeroPad(): Zero-Padding size in Y smaller than image! " + height + " < " + ip.height);
			return null;
		}

		if (offsetZ < 0)
		{
			IJ.error("Stitching_3D.ZeroPad(): Zero-Padding size in Z smaller than image! " + depth + " < " + ip.depth);
			return null;
		}

		for (int z = 0; z < ip.depth; z++)
			for (int y = 0; y < ip.height; y++)
				for (int x = 0; x < ip.width; x++)
					image.set(ip.get(x, y, z), x + offsetX, y + offsetY, z + offsetZ);

		return image;
	}

	private FloatArray3D pffft3DMT(final FloatArray3D values, final boolean scale)
	{
		final int height = values.height;
		final int width = values.width;
		final int depth = values.depth;
		final int complexWidth = (width / 2 + 1) * 2;

		final FloatArray3D result = new FloatArray3D(complexWidth, height, depth);

		//do fft's in x direction
		final AtomicInteger ai = new AtomicInteger(0);
		Thread[] threads = newThreads();
		final int numThreads = threads.length;

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					int myNumber = ai.getAndIncrement();

					float[] tempIn = new float[width];
					float[] tempOut;
					FftReal fft = new FftReal(width);

					for (int z = 0; z < depth; z++)
						if (z % numThreads == myNumber)
							for (int y = 0; y < height; y++)
							{
								tempOut = new float[complexWidth];

								for (int x = 0; x < width; x++)
									tempIn[x] = values.get(x, y, z);

								fft.realToComplex( -1, tempIn, tempOut);

								if (scale)
									fft.scale(width, tempOut);

								for (int x = 0; x < complexWidth; x++)
									result.set(tempOut[x], x, y, z);
							}
				}
			});
		startAndJoin(threads);

		//do fft's in y direction
		ai.set(0);
		threads = newThreads();

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					float[] tempIn = new float[height * 2];
					float[] tempOut;
					FftComplex fftc = new FftComplex(height);

					int myNumber = ai.getAndIncrement();

					for (int z = 0; z < depth; z++)
						if (z % numThreads == myNumber)
							for (int x = 0; x < complexWidth / 2; x++)
							{
								tempOut = new float[height * 2];

								for (int y = 0; y < height; y++)
								{
									tempIn[y * 2] = result.get(x * 2, y, z);
									tempIn[y * 2 + 1] = result.get(x * 2 + 1, y, z);
								}

								fftc.complexToComplex( -1, tempIn, tempOut);

								for (int y = 0; y < height; y++)
								{
									result.set(tempOut[y * 2], x * 2, y, z);
									result.set(tempOut[y * 2 + 1], x * 2 + 1, y, z);
								}
							}
				}
			});

		startAndJoin(threads);

		//do fft's in z direction
		ai.set(0);
		threads = newThreads();

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					float[] tempIn = new float[depth * 2];
					float[] tempOut;
					FftComplex fftc = new FftComplex(depth);

					int myNumber = ai.getAndIncrement();

					for (int y = 0; y < height; y++)
						if (y % numThreads == myNumber)
							for (int x = 0; x < complexWidth / 2; x++)
							{
								tempOut = new float[depth * 2];

								for (int z = 0; z < depth; z++)
								{
									tempIn[z * 2] = result.get(x * 2, y, z);
									tempIn[z * 2 + 1] = result.get(x * 2 + 1, y, z);
								}

								fftc.complexToComplex( -1, tempIn, tempOut);

								for (int z = 0; z < depth; z++)
								{
									result.set(tempOut[z * 2], x * 2, y, z);
									result.set(tempOut[z * 2 + 1], x * 2 + 1, y, z);
								}
							}
				}
			});

		startAndJoin(threads);

		return result;
	}

	private float[] computePhaseCorrelationMatrix(final float[] fft1, final float[] fft2, boolean inPlace)
	{
		normalizeComplexVectorsToUnitVectors(fft1);
		normalizeComplexVectorsToUnitVectors(fft2);

		float[] fftTemp1 = fft1;
		float[] fftTemp2 = fft2;

		// do complex conjugate
		if (inPlace)
			complexConjugate(fft2);
		else
			complexConjugate(fftTemp2);

		// multiply both complex arrays elementwise
		if (inPlace)
			multiply(fft1, fft2, true);
		else
			fftTemp1 = multiply(fftTemp1, fftTemp2, false);

		if (inPlace)
			return null;
		else
			return fftTemp1;
	}

	private void normalizeComplexVectorsToUnitVectors(float[] complex)
	{
		int wComplex = complex.length / 2;

		double length;

		for (int pos = 0; pos < wComplex; pos++)
		{
			length = Math.sqrt(Math.pow(complex[pos * 2], 2) + Math.pow(complex[pos * 2 + 1], 2));

			if (length > 1E-5)
			{
				complex[pos * 2 + 1] /= length;
				complex[pos * 2] /= length;
			}
			else
			{
				complex[pos * 2 + 1] = complex[pos * 2] = 0;
			}
		}

	}

	private void complexConjugate(float[] complex)
	{
		int wComplex = complex.length / 2;

		for (int pos = 0; pos < wComplex; pos++)
			complex[pos * 2 + 1] = -complex[pos * 2 + 1];
	}

	private float multiplyComplexReal(float a, float b, float c, float d)
	{
		return a * c - b * d;
	}

	private float multiplyComplexImg(float a, float b, float c, float d)
	{
		return a * d + b * c;
	}

	private float[] multiply(float[] complexA, float[] complexB, boolean overwriteA)
	{
		if (complexA.length != complexB.length)
			return null;

		float[] complexResult = null;

		if (!overwriteA)
			complexResult = new float[complexA.length];

		// this is the amount of complex numbers
		// the actual array size is twice as high
		int wComplex = complexA.length / 2;

		// we compute: (a + bi) * (c + di)
		float a, b, c, d;

		if (!overwriteA)
			for (int pos = 0; pos < wComplex; pos++)
			{
				a = complexA[pos * 2];
				b = complexA[pos * 2 + 1];
				c = complexB[pos * 2];
				d = complexB[pos * 2 + 1];

				// compute new real part
				complexResult[pos * 2] = multiplyComplexReal(a, b, c, d);

				// compute new imaginary part
				complexResult[pos * 2 + 1] = multiplyComplexImg(a, b, c, d);
			}
		else
			for (int pos = 0; pos < wComplex; pos++)
			{
				a = complexA[pos * 2];
				b = complexA[pos * 2 + 1];
				c = complexB[pos * 2];
				d = complexB[pos * 2 + 1];

				// compute new real part
				complexA[pos * 2] = multiplyComplexReal(a, b, c, d);

				// compute new imaginary part
				complexA[pos * 2 + 1] = multiplyComplexImg(a, b, c, d);
			}

		if (overwriteA)
			return complexA;
		else
			return complexResult;
	}

	private FloatArray3D pffftInv3DMT(final FloatArray3D values, final int nfft)
	{
		final int depth = values.depth;
		final int height = values.height;
		final int width = nfft;
		final int complexWidth = (width / 2 + 1) * 2;

		final FloatArray3D result = new FloatArray3D(width, height, depth);

		// do inverse fft's in z-direction on the complex numbers
		final AtomicInteger ai = new AtomicInteger(0);
		Thread[] threads = newThreads();
		final int numThreads = threads.length;

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					int myNumber = ai.getAndIncrement();

					float[] tempIn = new float[depth * 2];
					float[] tempOut;
					FftComplex fftc = new FftComplex(depth);

					for (int y = 0; y < height; y++)
						if (y % numThreads == myNumber)
							for (int x = 0; x < complexWidth / 2; x++)
							{
								tempOut = new float[complexWidth];

								tempOut = new float[depth * 2];

								for (int z = 0; z < depth; z++)
								{
									tempIn[z * 2] = values.get(x * 2, y, z);
									tempIn[z * 2 + 1] = values.get(x * 2 + 1, y, z);
								}

								fftc.complexToComplex(1, tempIn, tempOut);

								for (int z = 0; z < depth; z++)
								{
									values.set(tempOut[z * 2], x * 2, y, z);
									values.set(tempOut[z * 2 + 1], x * 2 + 1, y, z);
								}
							}
				}
			});
		startAndJoin(threads);

		// do inverse fft's in y-direction on the complex numbers
		ai.set(0);
		threads = newThreads();

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					float[] tempIn = new float[height * 2];
					float[] tempOut;
					FftComplex fftc = new FftComplex(height);

					int myNumber = ai.getAndIncrement();

					for (int z = 0; z < depth; z++)
						if (z % numThreads == myNumber)
							for (int x = 0; x < complexWidth / 2; x++)
							{
								tempOut = new float[height * 2];

								for (int y = 0; y < height; y++)
								{
									tempIn[y * 2] = values.get(x * 2, y, z);
									tempIn[y * 2 + 1] = values.get(x * 2 + 1, y, z);
								}

								fftc.complexToComplex(1, tempIn, tempOut);

								for (int y = 0; y < height; y++)
								{
									values.set(tempOut[y * 2], x * 2, y, z);
									values.set(tempOut[y * 2 + 1], x * 2 + 1, y, z);
								}
							}
				}
			});
		startAndJoin(threads);

		//do inverse fft's in x direction
		ai.set(0);
		threads = newThreads();

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(new Runnable()
			{
				public void run()
				{
					float[] tempIn = new float[complexWidth];
					float[] tempOut;
					FftReal fft = new FftReal(width);

					int myNumber = ai.getAndIncrement();

					for (int z = 0; z < depth; z++)
						if (z % numThreads == myNumber)
							for (int y = 0; y < height; y++)
							{
								tempOut = new float[width];

								for (int x = 0; x < complexWidth; x++)
									tempIn[x] = values.get(x, y, z);

								fft.complexToReal(1, tempIn, tempOut);

								for (int i = 0; i < tempOut.length; i++)
									tempOut[i] /= (float) (width * height * depth);

								//fft.scale(width, tempOut);

								for (int x = 0; x < width; x++)
									result.set(tempOut[x], x, y, z);
							}
				}
			});

		startAndJoin(threads);

		return result;
	}

	private void quicksort(Quicksortable[] data, int left, int right)
	{
		if (data == null || data.length < 2)return;
		int i = left, j = right;

		double x = data[(left + right) / 2].getQuicksortValue();

		do
		{
			while (data[i].getQuicksortValue() < x) i++;
			while (x < data[j].getQuicksortValue()) j--;
			if (i <= j)
			{
				Quicksortable temp = data[i];
				data[i] = data[j];
				data[j] = temp;
				i++;
				j--;
			}
		}
		while (i <= j);
		if (left < j) quicksort(data, left, j);
		if (i < right) quicksort(data, i, right);
	}

	private void startTask(Runnable run, int numThreads)
	{
		Thread[] threads = newThreads(numThreads);

		for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread] = new Thread(run);

		startAndJoin(threads);
	}

	private Thread[] newThreads()
	{
		int nthread = Runtime.getRuntime().availableProcessors();
		return new Thread[nthread];
	}

	private Thread[] newThreads(int numThreads)
	{
		return new Thread[numThreads];
	}

	private void startAndJoin(Thread[] threads)
	{
		for (int ithread = 0; ithread < threads.length; ++ithread)
		{
			threads[ithread].setPriority(Thread.NORM_PRIORITY);
			threads[ithread].start();
		}

		try
		{
			for (int ithread = 0; ithread < threads.length; ++ithread)
				threads[ithread].join();
		} catch (InterruptedException ie)
		{
			throw new RuntimeException(ie);
		}
	}


	private interface Quicksortable
	{
		public double getQuicksortValue();
	}

	/**
	 * <p>Title: CrossCorrelationResult</p>
	 *
	 * <p>Description: Stores the Result of a Cross Correlation done based on a spike in the Phase Correlation Matrix.
	 *                 It implements Quicksortable so that an Array of CrossCorrelationResults can be sorted.</p>
	 *
	 * <p>Copyright: Copyright (c) 2008</p>
	 *
	 * <p>License: GPL
	 * 
	 * @author Stephan Preibisch
	 * @version 1.0
	 */
	private class CrossCorrelationResult implements Quicksortable
	{
		public Point3D shift;
		public int overlappingPixels;
		public double SSQ, R, PCMValue;
		public ImagePlus overlapImp, errorMapImp;

		public double getQuicksortValue()
		{
			return 1 - R;
		}
	}

	/**
	 * <p>Title: Point3D</p>
	 *
	 * <p>Description: Data Structure for Storing a Point in 3D and an associated float Value</p>
	 *
	 * <p>Copyright: Copyright (c) 2008</p>
	 *
	 * <p>License: GPL
	 * 
	 * @author Stephan Preibisch
	 * @version 1.0
	 */
	private class Point3D
	{
		public int x = 0, y = 0, z = 0;
		public float value;

		public Point3D(int x, int y, int z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public Point3D(int x, int y, int z, float value)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.value = value;
		}
	}

	/**
	 * <p>Title: FloatArray</p>
	 *
	 * <p>Description: Abstract Class for implementations of Single Precision Floating Point Datastructure for fast access and simple storage</p>
	 *
	 * <p>Copyright: Copyright (c) 2008</p>
	 *
	 * <p>License: GPL
	 * 
	 * This program is free software; you can redistribute it and/or
	 * modify it under the terms of the GNU General Public License 2
	 * as published by the Free Software Foundation.
	 *
	 * This program is distributed in the hope that it will be useful,
	 * but WITHOUT ANY WARRANTY; without even the implied warranty of
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 * GNU General Public License for more details.
	 *
	 * You should have received a copy of the GNU General Public License
	 * along with this program; if not, write to the Free Software
	 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
	 *
	 * @author Stephan Preibisch
	 * @version 1.0
	 */
	private abstract class FloatArray
	{
		public float data[] = null;
		public abstract FloatArray clone();
	}

	/**
	 * <p>Title: FloatArray3D</p>
	 *
	 * <p>Description: Implementation of Single Precision Floating Point 3D Datastructure for fast access and simple storage</p>
	 *
	 * <p>Copyright: Copyright (c) 2008</p>
	 *
	 * <p>License: GPL
	 *
	 * This program is free software; you can redistribute it and/or
	 * modify it under the terms of the GNU General Public License 2
	 * as published by the Free Software Foundation.
	 *
	 * This program is distributed in the hope that it will be useful,
	 * but WITHOUT ANY WARRANTY; without even the implied warranty of
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 * GNU General Public License for more details.
	 *
	 * You should have received a copy of the GNU General Public License
	 * along with this program; if not, write to the Free Software
	 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
	 *
	 * @author Stephan Preibisch
	 * @version 1.0
	 */
	private class FloatArray3D extends FloatArray
	{
		//public float data[] = null;
		public int width = 0;
		public int height = 0;
		public int depth = 0;

		public FloatArray3D(float[] data, int width, int height, int depth)
		{
			this.data = data;
			this.width = width;
			this.height = height;
			this.depth = depth;
		}

		public FloatArray3D(int width, int height, int depth)
		{
			data = new float[width * height * depth];
			this.width = width;
			this.height = height;
			this.depth = depth;
		}

		public FloatArray3D clone()
		{
			FloatArray3D clone = new FloatArray3D(width, height, depth);
			System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
			return clone;
		}

		public int getPos(int x, int y, int z)
		{
			return x + width * (y + z * height);
		}

		public float get(int x, int y, int z)
		{
			return data[getPos(x, y, z)];
		}

		public float getMirror(int x, int y, int z)
		{
			if (x >= width)
				x = width - (x - width + 2);

			if (y >= height)
				y = height - (y - height + 2);

			if (z >= depth)
				z = depth - (z - depth + 2);

			if (x < 0)
			{
				int tmp = 0;
				int dir = 1;

				while (x < 0)
				{
					tmp += dir;
					if (tmp == width - 1 || tmp == 0)
						dir *= -1;
					x++;
				}
				x = tmp;
			}

			if (y < 0)
			{
				int tmp = 0;
				int dir = 1;

				while (y < 0)
				{
					tmp += dir;
					if (tmp == height - 1 || tmp == 0)
						dir *= -1;
					y++;
				}
				y = tmp;
			}

			if (z < 0)
			{
				int tmp = 0;
				int dir = 1;

				while (z < 0)
				{
					tmp += dir;
					if (tmp == depth - 1 || tmp == 0)
						dir *= -1;
					z++;
				}
				z = tmp;
			}

			return data[getPos(x, y, z)];
		}

		public float getZero(int x, int y, int z)
		{
			if (x >= width)
				return 0;

			if (y >= height)
				return 0;

			if (z >= depth)
				return 0;

			if (x < 0)
				return 0;

			if (y < 0)
				return 0;

			if (z < 0)
				return 0;

			return data[getPos(x, y, z)];
		}

		public void set(float value, int x, int y, int z)
		{
			data[getPos(x, y, z)] = value;
		}
	}
}



