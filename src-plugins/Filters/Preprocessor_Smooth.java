import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.Blitter;
import ij.ImagePlus;
import ij.io.Opener;

public class Preprocessor_Smooth implements PlugInFilter {

	private ImagePlus imp_;
	static private ImageProcessor gradient32bit = null;
	static private ImageProcessor gradient16bit = null;

	/** Apply only to 16 and 32-bit images. */
	public int setup(String arg, ImagePlus imp_) {

		System.out.println("imp_ is " + imp_);

		this.imp_ = imp_;

		if (null == imp_ || imp_ instanceof ini.trakem2.display.FakeImagePlus) return DONE;

		// doesn't improve the contrast uneveness

		/*
		// remove unbalanced background: filter away frequencies over half the image size
		int freq = imp_.getWidth() > imp_.getHeight() ? imp_.getWidth() : imp_.getHeight();
		freq /= 3;
		final String[] names = new String[]{"imp", "filterLargeDia", "filterSmallDia", "choiceIndex", "choiceDia", "toleranceDia", "doScalingDia", "saturateDia", "displayFilter"};
		final Object[] values = new Object[]{imp_, new Double(freq), new Double(0), new Integer(0), "None", new Double(5), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE};
		PlugInFilter bandpass = new ij.plugin.filter.FFTFilter();
		fixFields(bandpass, names, values);
		//ij.Macro.setOptions("filter_large=" + freq + " filter_small=0 suppress=None tolerance=5"); // no autoscale and no saturate
		//bandpass.setup("", imp);
		bandpass.run(imp_.getProcessor());
		*/

		final String base_dir = ij.Prefs.getHomeDir() + "/plugins/Filters/";

		switch (imp_.getType()) {
			case ImagePlus.GRAY16:
				if (null == gradient16bit) gradient16bit = new Opener().openImage(base_dir + "median_gradient3-16bit.tif").getProcessor();
				return NO_UNDO | DOES_STACKS | DOES_ALL;
			case ImagePlus.GRAY32:
				if (null == gradient32bit) gradient32bit = new Opener().openImage(base_dir + "median_gradient3.tif").getProcessor();
				return NO_UNDO | DOES_STACKS | DOES_ALL;
			default:
				// ignore all other image types
				return DONE;
		}
	}

	public void run(ImageProcessor ip) {
		// subtract median-filtered image
		switch (imp_.getType()) {
			case ImagePlus.GRAY16:
				ip.copyBits(gradient16bit, 0, 0, Blitter.SUBTRACT);
				return;
			case ImagePlus.GRAY32:
				ip.copyBits(gradient32bit, 0, 0, Blitter.SUBTRACT);
				return;
			default:
				return;
		}
		//if (null == imp_) return;
		//ip.smooth();
	}

	private void fixFields(Object ob, String[] names, Object[] values) {
		/*
		try {
		java.lang.reflect.Field[] fff = ob.getClass().getDeclaredFields();
		for (int j=0; j<fff.length; j++) {
			System.out.println(fff[j].getName());
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		for (int i=0; i<names.length; i++) {
			try {
				java.lang.reflect.Field f = ob.getClass().getDeclaredField(names[i]);
				f.setAccessible(true);
				f.set(ob, values[i]);
			} catch (Exception e) {
				System.out.println(e.toString()); // short
				try {
					java.lang.reflect.Field f = ob.getClass().getField(names[i]);
					f.setAccessible(true);
					f.set(ob, values[i]);
				} catch (Exception ee) {
					System.out.println(ee.toString()); // short
				}
			}
		}
	}
}

