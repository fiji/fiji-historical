//$Id: DT3D_.java,v 1.7 2005/04/20 16:08:56 perchrh Exp $

/* Makes an inplace 3D discrete distance transform. Uses ImageJ.

 Written by Maria Axelsson, ported to ImageJ/Java by 
 Jens Bache-Wiig <jensbw%at%gmail.com> and 
 Per Christian Henden <perchrh%at%pvv.org> 

 Free Software in the Public Domain.

 */

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class DT3D_ implements PlugInFilter {

	private ImagePlus imRef;

	public int setup(String arg, ImagePlus imp) {
		imRef = imp;

		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		return DOES_8G;
	}

	public void run(ImageProcessor ip) {
		final int width = ip.getWidth();
		final int height = ip.getHeight();
		final int depth = imRef.getStackSize();

		// final int a = 3, b = 4, c = 3, d = 4, e = 5; //alternate weights
		final int a = 3, b = 4, c = 5, d = 3, e = 7;

		final int[] wf = new int[] { e, d, e, d, c, d, e, d, e, b, a, b, a,
				255, 255, 255, 255, 255, };
		final int[] wb = new int[] { 255, 255, 255, 255, 255, a, b, a, b, e, d,
				e, d, c, d, e, d, e, };
		int[] slask = new int[2 * 3 * 3];

		// Border pixels are ignored to simplify the convolutions below
		for (int z = 0; z < depth; z++) {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {

					if (z == 0 || x == 0 || y == 0 || z == depth - 1 || y == height - 1 || x == width - 1) 
                                        {
						((byte[]) imRef.getStack().getPixels(z + 1))[x + y * width] = (byte) 0x0;
					}
				}
			}
		}

		// Forward iteration
		for (int z = 1; z < depth - 1; z++) {
			IJ.showProgress(z, 2 * depth - 2);
			for (int x = 1; x < width - 1; x++) {
				for (int y = 1; y < height - 1; y++) {

					for (int k = -1; k < 1; k++) {
						for (int j = -1; j < 2; j++) {
							for (int i = -1; i < 2; i++) {
								int slaskindex = (i + 1) + (j + 1) * 3
										+ (k + 1) * 3 * 3;
								int pixel = 0xff & ((byte[]) imRef.getStack()
										.getPixels(z + k + 1))[(x + i)
										+ (y + j) * width];
								slask[slaskindex] = pixel + wf[slaskindex];
							}
						}
					}
					int minval = slask[0]; // the lowest value so far
					for (int i = 1; i < slask.length; i++) {
						if ((slask[i]) < minval)
							minval = (slask[i]);
					}

					int pixel = 0xff & ((byte[]) imRef.getStack().getPixels(
							z + 1))[(x) + (y) * width];
					if (pixel > minval)
						((byte[]) imRef.getStack().getPixels(z + 1))[x + y
								* width] = (byte) (minval & 0xff);
				}
			}
		}

		// Backward iteration
		for (int z = depth - 2; z > 0; z--) {
			IJ.showProgress(2 * depth - z, 2 * depth - 2);
			for (int x = width - 2; x > 0; x--) {
				for (int y = height - 2; y > 0; y--) {

					for (int k = 0; k < 2; k++) {
						for (int j = -1; j < 2; j++) {
							for (int i = -1; i < 2; i++) {
								int slaskindex = (i + 1) + (j + 1) * 3 + (k)
										* 3 * 3;
								int pixel = 0xff & ((byte[]) imRef.getStack()
										.getPixels(z + k + 1))[(x + i)
										+ (y + j) * width];
								slask[slaskindex] = pixel + wb[slaskindex];
							}
						}
					}

					int minval = slask[0]; // the lowest value so far
					for (int i = 1; i < slask.length; i++) {
						if ((slask[i]) < minval)
							minval = (slask[i]);
					}

					int pixel = 0xff & ((byte[]) imRef.getStack().getPixels(
							z + 1))[(x) + (y) * width];
					if (pixel > minval)
						((byte[]) imRef.getStack().getPixels(z + 1))[x + y
								* width] = (byte) (minval & 0xff);
				}
			}
		}

	} // run

	void showAbout() {
		IJ
				.showMessage(
						"About DT3D...",
						"This plug-in filter calculates the 3D distances transform of a binary image with "
								+ "white (255) as background and black (0) as foreground.\n");
	}

} // class
