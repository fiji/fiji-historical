import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.plugin.ContrastEnhancer;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.util.Hashtable;
import java.util.Enumeration;

/** Automate task of adjusting .dat files:
 *
 * - open raw (headless) with predefined values
 * - resize to 50%
 * - run Enhance contrast
 * - convert to 8-bit
 * - cut the 6 px margin
 *
 *
 * */
public class Open_DAT_EMMENU extends ImagePlus implements PlugIn {


	public void run(String arg) {
		if (null == arg) return;
		while (-1 != arg.indexOf("  ")) {
			arg = arg.replaceAll("  ", " ");
		}
		Hashtable props = new Hashtable();
		String[] data = arg.split(" ");
		if (data.length<5) return;
		StringBuffer sb_info = new StringBuffer("Tecnai EMMENU .dat file info:\n");
		// start at 2, the datatype
		for (int i=2; i<data.length; i++) {
			int ieq = data[i].indexOf('=');
			String key = data[i].substring(0, ieq);
			String txt = data[i].substring(ieq+1);
			if (key.equals("comment")) txt = txt.replaceAll("QQQQ", " ");
			props.put(key, txt);
			sb_info.append(key).append(':').append(' ').append(txt).append('\n');
		}
		int w =  Integer.parseInt((String)props.get("width"));
		int h =  Integer.parseInt((String)props.get("height"));
		ImagePlus imp = openRaw(
					getType(Integer.parseInt((String)props.get("datatype"))),
					data[0].substring(data[0].indexOf('=') +1),
					data[1].substring(data[1].indexOf('=') + 1),
					w,
					h,
					512L,
					Integer.parseInt((String)props.get("n_images")),
					0,
					false,
					false);
		// gather info
		String info = (String)imp.getProperty("Info");
		if (null == info) info = sb_info.toString();
		else info += "\n" + sb_info.toString();

		// my convenience:

		/*
		// adjust contrast before converting to 8-bit for best results
		new ContrastEnhancer().stretchHistogram(imp, 0.5);
		// convert to 8-bit
		new ImageConverter(imp).convertToGray8();

		// cut off margin (ridiculous margin, why in the world ...)
		imp.setRoi(new Roi(6, 6, w -12, h -12));
		imp.setProcessor(imp.getTitle(), imp.getProcessor().crop());
		*/

		// integrate, the HandleExtraFileTypes way
		ImageStack stack = imp.getStack();
		setStack(imp.getTitle(),stack);
		setCalibration(imp.getCalibration());
		setProperty("Info", sb_info.toString());
		setFileInfo(imp.getOriginalFileInfo());
	}

	private int getType(int datatype) {
		switch (datatype) {
			case 1: return FileInfo.GRAY8;
			case 2: return FileInfo.GRAY16_SIGNED;
			case 4: return FileInfo.GRAY32_INT;
			case 5: return FileInfo.GRAY32_FLOAT;
			case 8: return FileInfo.GRAY64_FLOAT;
		}
		// else, error:
		return -1;
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
