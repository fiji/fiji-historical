package io;

/**
 * Open .mrc files from numerous sources, including Leginon (software for automated imaging in FEI electron microscopes, see http://ami.scripps.edu )
 * Copyright Albert Cardona. This work is in the public domain.
 */
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.plugin.ContrastEnhancer;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

public class Open_MRC_Leginon extends ImagePlus implements PlugIn {

	/** Expects path as argument, or will ask for it and then open the image.*/
	public void run(final String arg) {
		String path = arg;
		String directory = null;
		String filename = null;
		if (null == path || 0 == path.length()) {
			OpenDialog od = new OpenDialog("Choose .mrc file", null);
			directory = od.getDirectory();
			if (null == directory) return;
			filename = od.getFileName();
			path = directory + "/" + filename;
		} else {
			// the argument is the path
			File file = new File(path);
			directory = file.getParent(); // could be a URL
			filename = file.getName();
			if (directory.startsWith("http:/")) directory = "http://" + directory.substring(6); // the double '//' has been eliminated by the File object call to getParent()
		}

		if (!filename.toLowerCase().endsWith(".mrc")) {
			this.width = this.height = -1;
			return;
		}

		if (!directory.endsWith("/")) directory += "/"; // works in windows too

		InputStream is;
		byte[] buf = new byte[136];
		try {
			if (0 == path.indexOf("http://")) {
				is = new java.net.URL(path).openStream();
			} else {
				is = new FileInputStream(path);
			}
			is.read(buf, 0, 136);
			is.close();
		} catch (IOException e) {
			return;
		}
		int w = readIntLittleEndian(buf, 0);
		int h = readIntLittleEndian(buf, 4);
		int n = readIntLittleEndian(buf, 8);
		int dtype = getType(readIntLittleEndian(buf, 12));
		if (-1 == dtype) return;
		ImagePlus imp = openRaw(
					dtype,
					directory,
					filename,
					w,
					h,
					1024L,
					n,
					0,
					true, // little-endian
					false);

		// integrate, the HandleExtraFileTypes way
		ImageStack stack = imp.getStack();
		setStack(imp.getTitle(),stack);
		setCalibration(imp.getCalibration());
		Object obinfo = imp.getProperty("Info");
		if (null != obinfo) setProperty("Info", obinfo);
		setFileInfo(imp.getOriginalFileInfo());

		if (null == arg || 0 == arg.length()) {
			// was opened with a dialog
			this.show();
		}
	}

	private int getType(int datatype) {
		switch (datatype) {
			case 0: return FileInfo.GRAY8;
			case 1: return FileInfo.GRAY16_SIGNED;
			case 2: return FileInfo.GRAY32_FLOAT;
			case 6: return FileInfo.GRAY16_UNSIGNED;
		}
		// else, error:
		return -1;
	}

	private final int readIntLittleEndian(byte[] buf, int start) {
		return (buf[start]) + (buf[start+1]<<8) + (buf[start+2]<<12) + (buf[start+3]<<24);
	}

	/** Copied and modified from ij.io.ImportDialog. @param imageType must be a static field from FileInfo class. */
	static public ImagePlus openRaw(int imageType, String directory, String fileName, int width, int height, long offset, int nImages, int gapBetweenImages, boolean intelByteOrder, boolean whiteIsZero) {
		FileInfo fi = new FileInfo();
		fi.fileType = imageType;
		fi.fileFormat = fi.RAW;
		fi.fileName = fileName;
		if (0 == directory.indexOf("http://")) {
			fi.url = directory; // the ij.io.FileOpener will open a java.net.URL(fi.url).openStream() from it
		} else {
			fi.directory = directory;
		}
		fi.width = width;
		fi.height = height;
		if (offset>2147483647)
			fi.longOffset = offset;
		else
			fi.offset = (int)offset;
		fi.nImages = nImages;
		fi.gapBetweenImages = gapBetweenImages;
		fi.intelByteOrder = intelByteOrder;
		fi.whiteIsZero = whiteIsZero;
		FileOpener fo = new FileOpener(fi);
		try {
			return fo.open(false);
		} catch (Exception e) {
			return null;
		}
	}
}
