
import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.RankFilters;


public class WAT_MicroSCoBiOJ implements PlugInFilter {
    ImagePlus imp;
	boolean canceled;
	double radius=3;
	int shift=0;
	int isodatath = 0;
	int baseth = 0;
	float isodataweight = (float)0.25;
	float baseweight = (float)0.25;
	float localweight = (float)0.5;

    public int setup(String arg, ImagePlus imp) {
        if (IJ.versionLessThan("1.30"))
            return DONE;
        this.imp = imp;
        return DOES_8G+NO_UNDO;
    }

    public void run(ImageProcessor ip) {
		
        ImageStack stack = imp.getStack();
        int w = stack.getWidth();
        int h = stack.getHeight();
        int nSlices = imp.getStackSize();
        getDetails();
        if (canceled) return;
		int mSize= (int)radius;
		radius = radius/2;

		ImageStack risTh = new ImageStack(w,h);

		ByteProcessor bpSlice;
		ByteProcessor bpTemp = new ByteProcessor(w,h);
		RankFilters rf = new RankFilters();

        for (int i = 0; i < nSlices; i++)
		{
            IJ.showStatus("a: "+i+"/"+nSlices);
            imp.setSlice(i+1);
			isodatath=stack.getProcessor(i+1).getAutoThreshold();
			IJ.log("Plane: "+(i+1)+" IsadataTH: "+isodatath+"");
			bpSlice = (ByteProcessor)stack.getProcessor(i+1);
			byte[] sliceArray = (byte[])bpSlice.getPixelsCopy();

			bpTemp.setPixels(bpSlice.getPixelsCopy());
			rf.rank(bpTemp,radius, 1);
			bpTemp=getNewProcessor(bpTemp,bpSlice);
			risTh.addSlice("Test",bpTemp);
		}

        createImagePlus(risTh, "Adaptive Threshold image");
    }

   	void getDetails() {
		GenericDialog gd = new GenericDialog("WAT_MicroSCoBiOJ");
		gd.addNumericField("Window size: ", 3, 0);
		gd.addNumericField("Base threshold: ", 127, 0);
		gd.addNumericField("Base threshold weight (0.00 -> 1.00): ", 0.25, 3);
		gd.addNumericField("IsoData threshold weight  (0.00 -> 1.00): ", 0.25, 3);
		gd.addNumericField("Dynamic threshold weight (0.00 -> 1.00): ", 0.5, 3);
		gd.addNumericField("Background: ", 0, 0);

		gd.showDialog();
		if (gd.wasCanceled()) {
			canceled = true;
			return;
		}
		radius= (int)gd.getNextNumber();
		baseth = (int)gd.getNextNumber();
		baseweight = (float)gd.getNextNumber();
		isodataweight = (float)gd.getNextNumber();
		localweight = (float)gd.getNextNumber();
		shift= (int)gd.getNextNumber();
	}

	ByteProcessor getNewProcessor(ByteProcessor bpTmp, ByteProcessor bpSlc){
		int ww = bpSlc.getWidth();
		int hh = bpSlc.getHeight();
		byte[] tmpArray = (byte[])bpTmp.getPixelsCopy();
		byte[] slcArray = (byte[])bpSlc.getPixelsCopy();
		int maxm;
		int minm;
		int val;
		int meanval;
		int tmpr;
		maxm = (int)bpTmp.getMax();
		minm = (int)bpTmp.getMin();
		if (shift >= 0)
		{
			for (int i = 0; i < tmpArray.length; i++)
			{
				val = 0xff & ((byte[])slcArray)[i];
				meanval = 0xff & ((byte[])tmpArray)[i];
				tmpr=(int)(baseweight*baseth+localweight*meanval+isodataweight*isodatath+shift);
				if (val >tmpr)
					tmpArray[i] = (byte)255;
				else
					tmpArray[i] = (byte)0;
			}
		}
		else
		{
			for (int i = 0; i < tmpArray.length; i++)
			{
				val = 0xff & ((byte[])slcArray)[i];
				meanval = 0xff & ((byte[])tmpArray)[i];
				tmpr = (int)(baseweight * baseth + localweight * meanval + isodataweight * isodatath + shift);
				if ((val >tmpr) && (val > -shift))
					tmpArray[i] = (byte)255;
				else
					tmpArray[i] = (byte)0;
			}
		}
		ByteProcessor tmp = new ByteProcessor(ww, hh, tmpArray, null);
		return tmp;
	}


	void createImagePlus(ImageStack imsTemp, String txt){
		ImagePlus impTemp = new ImagePlus(txt,imsTemp);
		impTemp.setStack(null,imsTemp);
		impTemp.show();
	}
}
