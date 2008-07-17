import java.awt.*;
import java.util.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.util.*;
import mmorpho.*;

/**
 *     Granulometric filtering of images
 *     @version 1.3
 *     @date   26  March 2005
 *     - version 1.2
 *     - date   19  Jan 2004
 *
 *     @author	Dimiter Prodanov
 *     @author  University of Leiden
 *     @depends mmorpho
 *
 *      This plugin filter performs granulometric filtering of grayscale images by
 *      various types of Structuring Elements provided by the  StructuringElement class
 *
 *      Copyright (C) 2003 Dimiter Prodanov
 *
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *      Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */


public class GranFilter_ implements PlugInFilter, mmorpho.Constants {
    ImagePlus imp;

    //private static int options=0;
   // private static int showoptions=0;


    private static final String MINR="minR2", STEP="stepR2", SETYPE="SEtype";
    private  static float radius =(float)Prefs.getDouble(MINR,3);

    private static float step=(float)Prefs. getDouble(STEP,1);

    private static int eltype=Prefs.getInt(SETYPE,0);
    public final static String[] strelitems={"circle","diamond","square","hor line","ver line"};
    public final static int[] constitems={CIRCLE,DIAMOND,SQARE,HLINE,VLINE};
    private static boolean thresh=false;
    private ImagePlus result_image;
    /*------------------------------------------------------------------*/
    /**
     *  Overloaded method of PlugInFilter.
     *
     * @param  arg  Optional argument, not used by this plugin.
     * @param  imp  Optional argument, not used by this plugin.
     * @return   Flag word that specifies the filters capabilities.
     */
    public int setup(String arg, ImagePlus imp){
        this.imp=imp;
        IJ.register(GranFilter_.class);
        if (arg.equals("about")){
            showAbout();
            return DONE;
        }
        if(IJ.versionLessThan("1.23") || !showDialog(imp)) {
            return DONE;
        }
        else {
            return DOES_8G+NO_CHANGES+NO_UNDO;
        }
    } /* setup */
    
    public void run(ImageProcessor ip) {
        //imp.unlock();
        float minrad, maxrad;
        minrad=radius;
        maxrad=radius+step;
        //IJ.log(IJ.d2s(minrad));
        //IJ.log(IJ.d2s(maxrad));
       // ImageStatistics stats = imp.getStatistics();
        //double Vf=stats.mean/255;
        //  IJ.log(IJ.d2s(Vf));
        //IJ.setColumnHeadings("threshold"+"\t"+"distance");
       
        // tilte handling
        String title=imp.getTitle();
        int i2=0;
        if (title==null) title="empty";
        // int i1=title.length();
        if (title.indexOf(".")>-1) {
         i2=title.length()-4;
        }
        else  { i2=title.length();}
        //   IJ.log(i1+" "+ i2);
        title=title.substring(0, i2);
        title+="_";
        
       ImageProcessor ip1 = ip.duplicate(); // makes a copy of this image
        
        GrayOpen(ip1, eltype,minrad);
        ImageProcessor ip2 = ip.duplicate(); // makes a copy of this image
        
        GrayOpen(ip2,eltype,  maxrad);
        ip1.copyBits(ip2, 0, 0, Blitter.SUBTRACT);
        GrayOpen(ip1, eltype,minrad);
        
        // ImagePlus im=new ImagePlus("Filtered_"+imp.getTitle() , ip1);
        result_image=new ImagePlus("Filtered_"+title+minrad+"_"+maxrad , ip1);
       // stats=result_image.getStatistics();

        
 
        if (thresh) {
                    double area=ip.getHeight()*ip.getWidth();
                    IJ.setColumnHeadings("distance /pixel\t ");
                    double distance=calculateDistance(ip,result_image.getProcessor())/area;
                   IJ.write(IJ.d2s(distance,6));
            
    //        ip1.setThreshold(stats.mean/Vf, 255, 0);
            //IJ.write(IJ.d2s(throptions));
        }
        result_image.show();
        result_image.unlock();
        
    }
    
    public ImagePlus getResultImage(){
        return this.result_image;
    }
    
   public double calculateDistance(ImageProcessor ip1,ImageProcessor ip2) {
        int w=ip1.getWidth();
        int h=ip1.getHeight();
        double d=0;
        if ((w!=ip2.getWidth()) || (h!=ip2.getHeight()))
        return -1;
        else {
            for (int y=0;y<h;y++){
                for (int x=0;x<w ; x++) {
                    d+=(ip1.getPixel(x,y)-ip2.getPixel(x,y))*(ip1.getPixel(x,y)-ip2.getPixel(x,y));
                }
            }
            //d=1-d/w/h;
            d=Math.sqrt(d);
        }
        return d;
    }
        public int H(int x) {
        int h=0;
        if (x>=0) {
            h=1;
        }
        return h;
    }
    
    public int D(int x) {
        if (x==0) return 1;
        else return 0;
    }
    /*------------------------------------------------------------------*/
    /** Performs Graylevel opening of the current image
     * @param ip
     * @param radius radius of opening in pixels
     */
    public void GrayOpen(ImageProcessor ip, int type, float radius){

        //GrayMorphology_ gm=new GrayMorphology_();
        int seltype=constitems[type];
        StructureElement strel=new StructureElement(seltype, 0, radius, OFFSET0);
        MorphoProcessor mp=new MorphoProcessor(strel);
        mp.open(ip);
        //gm.open(ip, strel);

       // se=strel.getMask();
        
    } /* ImgGrayOpen */
    /*------------------------------------------------------------------*/
    boolean showDialog(ImagePlus imp)   {
        
        if (imp==null) return true;
        GenericDialog gd=new GenericDialog("Parameters");
        
        // Dialog box for user input
        gd.addMessage("This plugin performs granulometric filtering\n");
        // radius=size/2-1;
         gd.addChoice("Type of structure element", strelitems, strelitems[eltype]);
        gd.addNumericField("Radius of structure element (pixels):", radius, 1);
        
        gd.addNumericField("Step (pixels):", step, 1);
        gd.addCheckbox("Euclidean distance:", thresh);

        gd.showDialog();
        
        //eltype=constitems[gd.getNextChoiceIndex()];
        eltype=gd.getNextChoiceIndex();
        radius=(float)gd.getNextNumber();
        step=(float) gd.getNextNumber();
        thresh=gd.getNextBoolean();
        //      IJ.write(IJ.d2s(step));
        
        // IJ.write(MinRadius+"\t"+MaxRadius);
        if (gd.wasCanceled())
            return false;
        
        
        if (!validate(radius) || (radius<0)){
            IJ.showMessage("Invalid Numbers!\n" +
            "Enter Integers equal or greater than 1");
            return false;
        }
        
        if (!validate(step)){
            IJ.showMessage("Invalid Numbers!\n" +
            "Enter floats 0.5 or 1");
            return false;
        }
        
        return true;
    } /* showDialog */
    
    /*------------------------------------------------------------------*/
    void showAbout() {
        IJ.showMessage("About GranFilter...",
        "This plug-in filter performs granulometric filtering of images\n"+
        "the algorithm is described in Prodanov et al. J. Neurosci. Methods 2005\n"+
        "doi:10.1016/j.jneumeth.2005.07.011\n"+
        "more information at www.neuromorf.com"
        );
    }
    
    
    /* showAbout */
    /*------------------------------------------------------------------*/
    private static boolean validate( float var){
        float a=2*var;
        int b=(int) (2* var);
        // IJ.log(IJ.d2s(a-b));
        if ((a-b==0)||(var<0))
            return true;
        else return false;
    } /* validate */
    

        public static void savePreferences(Properties prefs) {
                    
            prefs.put(MINR, Double.toString(radius));
            prefs.put(STEP, Double.toString(step));
            prefs.put(SETYPE, Integer.toString(eltype));

   

    }
     
}
