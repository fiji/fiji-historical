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

/** The file ends up opened 3 times: at HandleExtraFileTypes.java, here to read the header again, and at openRaw. */

public class Open_MRC_Leginon extends ImagePlus implements PlugIn {

	/** Expects path as argument, or will ask for it and then open the image.*/
	public void run(String arg) {
		String path = arg;
		String directory = null;
		String filename = null;
		File f = new File(arg);
		if (!f.exists()) {
			OpenDialog od = new OpenDialog("Choose .mrc file", null);
			directory = od.getDirectory();
			if (null == directory) return;
			filename = od.getFileName();
			path = directory + "/" + filename;
		} else {
			try {
				directory = f.getParentFile().getAbsolutePath();
				filename = f.getName();
			} catch (Exception e) { return; }
		}
		if (!filename.toLowerCase().endsWith(".mrc")) return;

		InputStream is;
		byte[] buf = new byte[136];
		try {
			is = new FileInputStream(path);
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

		if (!f.exists()) this.show();
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
		fi.directory = directory;
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
