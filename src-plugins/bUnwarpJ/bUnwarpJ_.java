/**
 * @(#)bUnwarpJ_.java	First version 09/15/2005
 *
 * Center for Machine Perception - Czech Technical University
 *
 * This work is an extension by Ignacio Arganda-Carreras and Jan Kybic 
 * of the previous UnwarpJ project by Carlos Oscar Sanchez Sorzano.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
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
 */

/**
 * ====================================================================
 *  | Version: May 17th, 2007
 * \===================================================================
 */

/**====================================================================
 * Ignacio Arganda-Carreras
 * Escuela Politecnica Superior
 * Laboratorio B-408     
 * Universidad Autonoma de Madrid
 * Ctra. de Colmenar Viejo, Km. 15
 * Madrid 28049,  Spain
 *
 * Phone: (+34) 91 497 2260
 * E-mail: Ignacio.Arganda@uam.es
 * Web: http://www.ii.uam.es/~iarganda
 *\===================================================================*/


/**
 * Old version information: 
 * http://bigwww.epfl.ch/thevenaz/UnwarpJ/
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;

/*====================================================================
|   bUnwarpJ_
\===================================================================*/

/**
 * Main class.
 * This class is a plugin for the ImageJ interface. It allows pairwise image
 * registration combining the ideas of elastic registration based on B-spline 
 * models and consistent registration.
 *
 * <p>
 * This work is an extension by Ignacio Arganda-Carreras and Jan Kybic 
 * of the previous UnwarpJ project by Carlos Oscar Sanchez Sorzano.
 * <p>
 * For more information visit the main site 
 * <a href="http://biocomp.cnb.uam.es/~iarganda/bUnwarpJ/">
 * http://biocomp.cnb.uam.es/~iarganda/bUnwarpJ/</a>
 *
 * @version 05/17/2007
 * @author Ignacio Arganda-Carreras
 * @author Jan Kybic
 */
public class bUnwarpJ_ implements PlugIn
{ /* begin class bUnwarpJ_ */

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Method to lunch the plugin.
     *
     * @param commandLine command to determine the action
     */
    public void run (final String commandLine) 
    {
       String options = Macro.getOptions();

       if (!commandLine.equals("")) options = commandLine;

       if (options == null) 
       {
          Runtime.getRuntime().gc();
          final ImagePlus[] imageList = createImageList();
          if (imageList.length < 2) 
          {
             IJ.error("At least two images are required (stack of color images disallowed)");
             return;
          }

          final bUnwarpJDialog dialog = new bUnwarpJDialog(IJ.getInstance(), imageList);
          GUI.center(dialog);
          dialog.setVisible(true);
       } 
       else 
       {
          final String[] args = getTokens(options);
          if (args.length<1) 
          {
             dumpSyntax();
             return;
          } 
          else 
          {              
              if      (args[0].equals("-help"))                 dumpSyntax();
              else if (args[0].equals("-align"))                alignImagesMacro(args);
              else if (args[0].equals("-elastic_transform"))    elasticTransformImageMacro(args);
              else if (args[0].equals("-raw_transform"))        rawTransformImageMacro(args);
              else if (args[0].equals("-compare_elastic"))      compareElasticTransformationsMacro(args);
              else if (args[0].equals("-compare_elastic_raw"))  compareElasticRawTransformationsMacro(args);
              else if (args[0].equals("-compare_raw"))          compareRawTransformationsMacro(args);
              else if (args[0].equals("-convert_to_raw"))       convertToRawTransformationMacro(args);
              else if (args[0].equals("-compose_elastic"))      composeElasticTransformationsMacro(args);
              else if (args[0].equals("-compose_raw"))          composeRawTransformationsMacro(args);
              else if (args[0].equals("-compose_raw_elastic"))  composeRawElasticTransformationsMacro(args);
          }
          return;
       }
    } /* end run */

    /*------------------------------------------------------------------*/
    /**
     * Main method for bUnwarpJ.
     *
     * @param args arguments to decide the action
     */
    public static void main(String args[]) 
    {
       if (args.length<1) 
       {
          dumpSyntax();
          System.exit(1);
       } 
       else 
       {
          if      (args[0].equals("-help"))                 dumpSyntax();
          else if (args[0].equals("-align"))                alignImagesMacro(args);
          else if (args[0].equals("-elastic_transform"))    elasticTransformImageMacro(args);
          else if (args[0].equals("-raw_transform"))        rawTransformImageMacro(args);
          else if (args[0].equals("-compare_elastic"))      compareElasticTransformationsMacro(args);
          else if (args[0].equals("-compare_elastic_raw"))  compareElasticRawTransformationsMacro(args);
          else if (args[0].equals("-compare_raw"))          compareRawTransformationsMacro(args);
          else if (args[0].equals("-convert_to_raw"))       convertToRawTransformationMacro(args);
          else if (args[0].equals("-compose_elastic"))      composeElasticTransformationsMacro(args);
          else if (args[0].equals("-compose_raw"))          composeRawTransformationsMacro(args);
          else if (args[0].equals("-compose_raw_elastic"))  composeRawElasticTransformationsMacro(args);
       }
       System.exit(0);
    }

    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Macro for images alignment with no graphical interface.
     *
     * @param args arguments for the program
     */
    private static void alignImagesMacro(String args[]) 
    {
       if (args.length < 13)
       {
           dumpSyntax();
           System.exit(0);
       }
       // Read input parameters
       String fn_target = args[1];
       String fn_target_mask = args[2];
       String fn_source = args[3];
       String fn_source_mask = args[4];
       int min_scale_deformation = ((Integer) new Integer(args[5])).intValue();
       int max_scale_deformation = ((Integer) new Integer(args[6])).intValue();
       double  divWeight = ((Double) new Double(args[7])).doubleValue();
       double  curlWeight = ((Double) new Double(args[8])).doubleValue();
       double  imageWeight = ((Double) new Double(args[9])).doubleValue();

       double  consistencyWeight = ((Double) new Double(args[10])).doubleValue();

       String fn_out_1 = args[11];
       String fn_out_2 = args[12];
       double  landmarkWeight = 0;
       String fn_landmark = "";
       if (args.length==15) 
       {
          landmarkWeight = ((Double) new Double(args[13])).doubleValue();
          fn_landmark = args[14];
       }

       // Show parameters
       IJ.write("Target image           : " + fn_target);
       IJ.write("Target mask            : " + fn_target_mask);
       IJ.write("Source image           : " + fn_source);
       IJ.write("Source mask            : " + fn_source_mask);
       IJ.write("Min. Scale Deformation : " + min_scale_deformation);
       IJ.write("Max. Scale Deformation : " + max_scale_deformation);
       IJ.write("Div. Weight            : " + divWeight);
       IJ.write("Curl Weight            : " + curlWeight);
       IJ.write("Image Weight           : " + imageWeight);
       IJ.write("Consistency Weight     : " + consistencyWeight);
       IJ.write("Output 1               : " + fn_out_1);
       IJ.write("Output 2               : " + fn_out_2);
       IJ.write("Landmark Weight        : " + landmarkWeight);
       IJ.write("Landmark file          : " + fn_landmark);

       // Produce side information
       int     imagePyramidDepth=max_scale_deformation-min_scale_deformation+1;
       int     min_scale_image = 0;
       double  stopThreshold = 1e-2;  // originaly -2
       int     outputLevel = -1;
       boolean showMarquardtOptim = false;
       int     accurate_mode = 1;
       boolean saveTransf = true;

       // First transformation file name.
       String fn_tnf_1 = "";
       int dot = fn_out_1.lastIndexOf('.');
       if (dot == -1) 
           fn_tnf_1 = fn_out_1 + "_transf.txt";
       else           
           fn_tnf_1 = fn_out_1.substring(0, dot) + "_transf.txt";
       
       // Second transformation file name.
       String fn_tnf_2 = "";
       dot = fn_out_2.lastIndexOf('.');
       if (dot == -1) 
           fn_tnf_2 = fn_out_2 + "_transf.txt";
       else           
           fn_tnf_2 = fn_out_2.substring(0, dot) + "_transf.txt";

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       
       bUnwarpJImageModel target =
          new bUnwarpJImageModel(targetImp.getProcessor(), true);
       
       target.setPyramidDepth(imagePyramidDepth+min_scale_image);
       target.getThread().start();
       
       bUnwarpJMask targetMsk = new bUnwarpJMask(targetImp.getProcessor(),false);
       
       if (fn_target_mask.equalsIgnoreCase(new String("NULL")) == false)
           targetMsk.readFile(fn_target_mask);
       
       bUnwarpJPointHandler targetPh=null;

       // Open source
       boolean bIsReverse = true;

       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);   

       bUnwarpJImageModel source =
          new bUnwarpJImageModel(sourceImp.getProcessor(), bIsReverse);

       source.setPyramidDepth(imagePyramidDepth + min_scale_image);
       source.getThread().start();

       bUnwarpJMask sourceMsk = new bUnwarpJMask(sourceImp.getProcessor(), false);

       if (fn_source_mask.equalsIgnoreCase(new String("NULL")) == false)
           sourceMsk.readFile(fn_source_mask);
       
       bUnwarpJPointHandler sourcePh=null;

       // Load landmarks
       if (fn_landmark!="") 
       {
          Stack sourceStack = new Stack();
          Stack targetStack = new Stack();
          bUnwarpJMiscTools.loadPoints(fn_landmark,sourceStack,targetStack);

          sourcePh  = new bUnwarpJPointHandler(sourceImp);
          targetPh  = new bUnwarpJPointHandler(targetImp);

          while ((!sourceStack.empty()) && (!targetStack.empty())) 
          {
             Point sourcePoint = (Point)sourceStack.pop();
             Point targetPoint = (Point)targetStack.pop();
             sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
             targetPh.addPoint(targetPoint.x, targetPoint.y);
          }
       }

       // Join threads
       try 
       {
           source.getThread().join();
           target.getThread().join();
       } 
       catch (InterruptedException e) 
       {
           IJ.error("Unexpected interruption exception " + e);
       }

       // Perform registration
       ImagePlus output_ip_1 = new ImagePlus();
       ImagePlus output_ip_2 = new ImagePlus();
       bUnwarpJDialog dialog = null;


       final bUnwarpJTransformation warp = new bUnwarpJTransformation(
         sourceImp, targetImp, source, target, sourcePh, targetPh,
         sourceMsk, targetMsk, min_scale_deformation, max_scale_deformation,
         min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
         consistencyWeight, stopThreshold, outputLevel, showMarquardtOptim, accurate_mode,
         saveTransf, fn_tnf_1, fn_tnf_2, output_ip_1, output_ip_2, dialog);

       IJ.write("\nRegistering...\n");
       
       warp.doRegistration();

       // Save results
       FileSaver fs = new FileSaver(output_ip_1);
       fs.saveAsTiff(fn_out_1);
       fs = new FileSaver(output_ip_2);
       fs.saveAsTiff(fn_out_2);
    }

    /*------------------------------------------------------------------*/
    /**
     * Create the image list.
     *
     * @return image list
     */
    private ImagePlus[] createImageList () 
    {
       final int[] windowList = WindowManager.getIDList();
       final Stack stack = new Stack();
       for (int k = 0; ((windowList != null) && (k < windowList.length)); k++) 
       {
          final ImagePlus imp = WindowManager.getImage(windowList[k]);
          final int inputType = imp.getType();

          if ((imp.getStackSize() == 1) || (inputType == imp.GRAY8) || (inputType == imp.GRAY16)
             || (inputType == imp.GRAY32)) 
          {
             stack.push(imp);
          }
       }
       final ImagePlus[] imageList = new ImagePlus[stack.size()];
       int k = 0;
       while (!stack.isEmpty()) {
          imageList[k++] = (ImagePlus)stack.pop();
       }
       return(imageList);
    } /* end createImageList */

    /*------------------------------------------------------------------*/
    /**
     * Method to write the syntaxis of the program in the command line.
     */
    private static void dumpSyntax () 
    {
       IJ.write("Purpose: Consistent and elastic registration of two images.");
       IJ.write(" ");
       IJ.write("Usage: bUnwarpj_ ");
       IJ.write("  -help                       : SHOW THIS MESSAGE");
       IJ.write("");
       IJ.write("  -align                      : ALIGN TWO IMAGES");
       IJ.write("          target_image        : In any image format");
       IJ.write("          target_mask         : In any image format");
       IJ.write("          source_image        : In any image format");
       IJ.write("          source_mask         : In any image format");
       IJ.write("          min_scale_def       : Scale of the coarsest deformation");
       IJ.write("                                0 is the coarsest possible");
       IJ.write("          max_scale_def       : Scale of the finest deformation");
       IJ.write("          Div_weight          : Weight of the divergence term");
       IJ.write("          Curl_weight         : Weight of the curl term");
       IJ.write("          Image_weight        : Weight of the image term");
       IJ.write("          Consistency_weight  : Weight of the deformation consistency");
       IJ.write("          Output image 1      : Output result 1 in TIFF");
       IJ.write("          Output image 2      : Output result 2 in TIFF");
       IJ.write("          Optional parameters :");
       IJ.write("             Landmark_weight  : Weight of the landmarks");
       IJ.write("             Landmark_file    : Landmark file");
       IJ.write("");
       IJ.write("  -elastic_transform          : TRANSFORM A SOURCE IMAGE WITH A GIVEN ELASTIC DEFORMATION");
       IJ.write("          target_image        : In any image format");
       IJ.write("          source_image        : In any image format");
       IJ.write("          transformation_file : As saved by bUnwarpJ in elastic format");
       IJ.write("          Output image        : Output result in TIFF");
       IJ.write("");
       IJ.write("  -raw_transform              : TRANSFORM A SOURCE IMAGE WITH A GIVEN RAW DEFORMATION");
       IJ.write("          target_image        : In any image format");
       IJ.write("          source_image        : In any image format");
       IJ.write("          transformation_file : As saved by bUnwarpJ in raw format");
       IJ.write("          Output image        : Output result in TIFF");
       IJ.write("");
       IJ.write("  -compare_elastic                   : COMPARE 2 OPPOSITE ELASTIC DEFORMATIONS (BY WARPING INDEX)");
       IJ.write("          target_image               : In any image format");
       IJ.write("          source_image               : In any image format");
       IJ.write("          target_transformation_file : As saved by bUnwarpJ");
       IJ.write("          source_transformation_file : As saved by bUnwarpJ");
       IJ.write("");
       IJ.write("  -compare_elastic_raw                : COMPARE AN ELASTIC DEFORMATION WITH A RAW DEFORMATION (BY WARPING INDEX)");
       IJ.write("          target_image                : In any image format");
       IJ.write("          source_image                : In any image format");
       IJ.write("          Elastic Transformation File : As saved by bUnwarpJ in elastic format");
       IJ.write("          Raw Transformation File     : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -compare_raw                       : COMPARE 2 ELASTIC DEFORMATIONS (BY WARPING INDEX)");
       IJ.write("          target_image               : In any image format");
       IJ.write("          source_image               : In any image format");
       IJ.write("          Raw Transformation File 1  : As saved by bUnwarpJ in raw format");
       IJ.write("          Raw Transformation File 2  : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -convert_to_raw                           : CONVERT AN ELASTIC DEFORMATION INTO RAW FORMAT");
       IJ.write("          target_image                      : In any image format");
       IJ.write("          source_image                      : In any image format");
       IJ.write("          Input Elastic Transformation File : As saved by bUnwarpJ in elastic format");
       IJ.write("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -compose_elastic                          : COMPOSE TWO ELASTIC DEFORMATIONS");
       IJ.write("          target_image                      : In any image format");
       IJ.write("          source_image                      : In any image format");
       IJ.write("          Elastic Transformation File 1     : As saved by bUnwarpJ in elastic format");
       IJ.write("          Elastic Transformation File 2     : As saved by bUnwarpJ in elastic format");
       IJ.write("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -compose_raw                              : COMPOSE TWO RAW DEFORMATIONS");
       IJ.write("          target_image                      : In any image format");
       IJ.write("          source_image                      : In any image format");
       IJ.write("          Raw Transformation File 1         : As saved by bUnwarpJ in raw format");
       IJ.write("          Raw Transformation File 2         : As saved by bUnwarpJ in raw format");
       IJ.write("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("  -compose_raw_elastic                      : COMPOSE A RAW DEFORMATION WITH AN ELASTIC DEFORMATION");
       IJ.write("          target_image                      : In any image format");
       IJ.write("          source_image                      : In any image format");
       IJ.write("          Elastic Transformation File       : As saved by bUnwarpJ in raw format");
       IJ.write("          Raw Transformation File           : As saved by bUnwarpJ in elastic format");
       IJ.write("          Output Raw Transformation File    : As saved by bUnwarpJ in raw format");
       IJ.write("");
       IJ.write("Examples:");
       IJ.write("Align two images without landmarks and without mask");
       IJ.write("   bUnwarpj_ -align target.jpg NULL source.jpg NULL 0 2 0.1 0.1 1 10 output_1.tif output_2.tif");
       IJ.write("Align two images with landmarks and mask");
       IJ.write("   bUnwarpj_ -align target.tif target_mask.tif source.tif source_mask.tif 0 2 0.1 0.1 1 10 output_1.tif output_2.tif 1 landmarks.txt");
       IJ.write("Align two images using only landmarks");
       IJ.write("   bUnwarpj_ -align target.jpg NULL source.jpg NULL 0 2 0.1 0.1 0 0 output.tif_1 output_2.tif 1 landmarks.txt");
       IJ.write("Transform the source image with a previously computed elastic transformation");
       IJ.write("   bUnwarpj_ -elastic_transform target.jpg source.jpg elastic_transformation.txt output.tif");       
       IJ.write("Transform the source image with a previously computed raw transformation");
       IJ.write("   bUnwarpj_ -raw_transform target.jpg source.jpg raw_transformation.txt output.tif");
       IJ.write("Calculate the warping index of two opposite elastic transformations");
       IJ.write("   bUnwarpj_ -compare_elastic target.jpg source.jpg source_transformation.txt target_transformation.txt");
       IJ.write("Calculate the warping index between an elastic transformation and a raw transformation");
       IJ.write("   bUnwarpj_ -compare_elastic_raw target.jpg source.jpg elastic_transformation.txt raw_transformation.txt");
       IJ.write("Calculate the warping index between two raw transformations");
       IJ.write("   bUnwarpj_ -compare_raw target.jpg source.jpg raw_transformation_1.txt raw_transformation_2.txt");
       IJ.write("Convert an elastic transformation into raw format");
       IJ.write("   bUnwarpj_ -convert_to_raw target.jpg source.jpg elastic_transformation.txt output_raw_transformation.txt");
       IJ.write("Compose two elastic transformations ");
       IJ.write("   bUnwarpj_ -compose_elastic target.jpg source.jpg elastic_transformation_1.txt elastic_transformation_2.txt output_raw_transformation.txt");
       IJ.write("Compose two raw transformations ");
       IJ.write("   bUnwarpj_ -compose_raw target.jpg source.jpg raw_transformation_1.txt raw_transformation_2.txt output_raw_transformation.txt");
       IJ.write("Compose a raw transformation with an elastic transformation ");
       IJ.write("   bUnwarpj_ -compose_raw_elastic target.jpg source.jpg raw_transformation.txt elastic_transformation.txt output_raw_transformation.txt");
    } /* end dumpSyntax */

    /*------------------------------------------------------------------*/
    /**
     * Get tokens.
     *
     * @param options options to get the tokens
     * @return tokens
     */
    private String[] getTokens (final String options) 
    {
        StringTokenizer t = new StringTokenizer(options);
        String[] token = new String[t.countTokens()];
        for (int k = 0; (k < token.length); k++) {
                token[k] = t.nextToken();
        }
        return(token);
    } /* end getTokens */

    /*------------------------------------------------------------------*/
    /**
     * Method to transform an image given an elastic deformation.
     *
     * @param args program arguments
     */
    private static void elasticTransformImageMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf    = args[3];
       String fn_out    = args[4];

       // Show parameters
       IJ.write("Target image           : " + fn_target);
       IJ.write("Source image           : " + fn_source);
       IJ.write("Transformation file    : " + fn_tnf);
       IJ.write("Output:                : " + fn_out);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       bUnwarpJImageModel source = new bUnwarpJImageModel(sourceImp.getProcessor(), false);
       source.setPyramidDepth(0);
       source.getThread().start();

       // Load transformation
       int intervals=bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);
       double [][]cx=new double[intervals+3][intervals+3];
       double [][]cy=new double[intervals+3][intervals+3];
       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx, cy);

       // Join threads
       try {
          source.getThread().join();
       } catch (InterruptedException e) {
          IJ.error("Unexpected interruption exception " + e);
       }

       // Apply transformation to source
       bUnwarpJMiscTools.applyTransformationToSource(
          sourceImp, targetImp, source, intervals, cx, cy);

       // Save results
       FileSaver fs = new FileSaver(sourceImp);
       fs.saveAsTiff(fn_out);
       
    } /* end elasticTransformIMageMacro */

    /*------------------------------------------------------------------*/
    /**
     * Method to transform an image given an elastic deformation.
     *
     * @param args program arguments
     */
    private static void rawTransformImageMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf    = args[3];
       String fn_out    = args[4];

       // Show parameters
       IJ.write("Target image           : " + fn_target);
       IJ.write("Source image           : " + fn_source);
       IJ.write("Transformation file    : " + fn_tnf);
       IJ.write("Output:                : " + fn_out);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       bUnwarpJImageModel source = new bUnwarpJImageModel(sourceImp.getProcessor(), false);
       source.setPyramidDepth(0);
       source.getThread().start();

       double [][]transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][]transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];

       bUnwarpJMiscTools.loadRawTransformation(fn_tnf, transformation_x, transformation_y);

       // Apply transformation
       bUnwarpJMiscTools.applyRawTransformationToSource(sourceImp, targetImp, source, transformation_x, transformation_y);

       // Save results
       FileSaver fs = new FileSaver(sourceImp);
       fs.saveAsTiff(fn_out);
       
    } /* end rawTransformImageMacro */    
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare two opposite elastic deformations through the 
     * warping index.
     *
     * @param args program arguments
     */
    private static void compareElasticTransformationsMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_1   = args[3];
       String fn_tnf_2   = args[4];

       // Show parameters
       IJ.write("Target image                  : " + fn_target);
       IJ.write("Source image                  : " + fn_source);
       IJ.write("Target Transformation file    : " + fn_tnf_1);
       IJ.write("Source Transformation file    : " + fn_tnf_2);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       // First deformation.
       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_2);

       double [][]cx_direct = new double[intervals+3][intervals+3];
       double [][]cy_direct = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_2, cx_direct, cy_direct);      

       intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_1);

       double [][]cx_inverse = new double[intervals+3][intervals+3];
       double [][]cy_inverse = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_1, cx_inverse, cy_inverse);
       
       double warpingIndex = bUnwarpJMiscTools.warpingIndex(sourceImp, targetImp, intervals, cx_direct, cy_direct, cx_inverse, cy_inverse);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!");             
       
    } /* end method compareElasticTransformationsMacro */
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare an elastic deformation with a raw deformation
     * through the warping index.
     *
     * @param args program arguments
     */
    private static void compareElasticRawTransformationsMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_elastic = args[3];
       String fn_tnf_raw     = args[4];

       // Show parameters
       IJ.write("Target image                  : " + fn_target);
       IJ.write("Source image                  : " + fn_source);
       IJ.write("Elastic Transformation file   : " + fn_tnf_elastic);
       IJ.write("Raw Transformation file       : " + fn_tnf_raw);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx_direct = new double[intervals+3][intervals+3];
       double [][]cy_direct = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic, cx_direct, cy_direct);

      
       // We load the transformation raw file.
       double[][] transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw, transformation_x, 
               transformation_y);
       
       double warpingIndex = bUnwarpJMiscTools.rawWarpingIndex(sourceImp, targetImp, 
               intervals, cx_direct, cy_direct, transformation_x, transformation_y);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!");             
       
    } /* end method compareElasticRawTransformationMacro */    
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compare two raw deformations through the warping index.
     *
     * @param args program arguments
     */
    private static void compareRawTransformationsMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_1   = args[3];
       String fn_tnf_2   = args[4];

       // Show parameters
       IJ.write("Target image                  : " + fn_target);
       IJ.write("Source image                  : " + fn_source);
       IJ.write("Target Transformation file    : " + fn_tnf_1);
       IJ.write("Source Transformation file    : " + fn_tnf_2);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       // We load the transformation raw file.
       double[][] transformation_x_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_1, transformation_x_1, transformation_y_1);
       
       // We load the transformation raw file.
       double[][] transformation_x_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_2, transformation_x_2, transformation_y_2);
       
       double warpingIndex = bUnwarpJMiscTools.rawWarpingIndex(sourceImp, targetImp, 
               transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!");            
       
    } /* end method compareRawTransformationsMacro */

    /*------------------------------------------------------------------*/
    /**
     * Method to convert an elastic deformations into raw format.
     *
     * @param args program arguments
     */
    private static void convertToRawTransformationMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_elastic = args[3];
       String fn_tnf_raw     = args[4];
       

       // Show parameters
       IJ.write("Target image                      : " + fn_target);
       IJ.write("Source image                      : " + fn_source);
       IJ.write("Input Elastic Transformation file : " + fn_tnf_elastic);
       IJ.write("Ouput Raw Transformation file     : " + fn_tnf_raw);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx = new double[intervals+3][intervals+3];
       double [][]cy = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic, cx, cy);
       
       
       // We load the transformation raw file.
       double[][] transformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
       
       bUnwarpJMiscTools.convertElasticTransformationToRaw(targetImp, intervals, cx, cy, transformation_x, transformation_y); 
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw, targetImp.getWidth(), targetImp.getHeight(), transformation_x, transformation_y);
       
    } /* end method convertToRawTransformationMacro */    

    /*------------------------------------------------------------------*/
    /**
     * Method to compose two raw deformations.
     *
     * @param args program arguments
     */
    private static void composeRawTransformationsMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_raw_1   = args[3];
       String fn_tnf_raw_2   = args[4];
       String fn_tnf_raw_out = args[5];
       

       // Show parameters
       IJ.write("Target image                      : " + fn_target);
       IJ.write("Source image                      : " + fn_source);
       IJ.write("Input Raw Transformation file 1   : " + fn_tnf_raw_1);
       IJ.write("Input Raw Transformation file 2   : " + fn_tnf_raw_2);
       IJ.write("Output Raw Transformation file    : " + fn_tnf_raw_out);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       // We load the first transformation raw file.
       double[][] transformation_x_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw_1, transformation_x_1, transformation_y_1);
              
       // We load the second transformation raw file.
       double[][] transformation_x_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_2 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw_2, transformation_x_2, transformation_y_2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeRawTransformations(targetImp.getWidth(), targetImp.getHeight(), 
               transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2, 
               outputTransformation_x, outputTransformation_y);
              
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw_out, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);
       
    } /* end method composeRawTransformationsMacro */     
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compose two elastic deformations.
     *
     * @param args program arguments
     */
    private static void composeElasticTransformationsMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_elastic_1   = args[3];
       String fn_tnf_elastic_2   = args[4];
       String fn_tnf_raw = args[5];
       

       // Show parameters
       IJ.write("Target image                        : " + fn_target);
       IJ.write("Source image                        : " + fn_source);
       IJ.write("Input Elastic Transformation file 1 : " + fn_tnf_elastic_1);
       IJ.write("Input Elastic Transformation file 2 : " + fn_tnf_elastic_2);
       IJ.write("Output Raw Transformation file      : " + fn_tnf_raw);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");
       
       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic_1);

       double [][]cx1 = new double[intervals+3][intervals+3];
       double [][]cy1 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic_1, cx1, cy1);

       intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic_2);

       double [][]cx2 = new double[intervals+3][intervals+3];
       double [][]cy2 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic_2, cx2, cy2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeElasticTransformations(targetImp, intervals, 
               cx1, cy1, cx2, cy2, outputTransformation_x, outputTransformation_y);
       
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);       
       
    } /* end method composeElasticTransformationsMacro */
    
    /*------------------------------------------------------------------*/
    /**
     * Method to compose a raw deformation with an elastic deformation.
     *
     * @param args program arguments
     */
    private static void composeRawElasticTransformationsMacro(String args[]) 
    {
       // Read input parameters
       String fn_target = args[1];
       String fn_source = args[2];
       String fn_tnf_raw_in = args[3];
       String fn_tnf_elastic = args[4];
       String fn_tnf_raw_out = args[5];
       

       // Show parameters
       IJ.write("Target image                      : " + fn_target);
       IJ.write("Source image                      : " + fn_source);
       IJ.write("Input Raw Transformation file     : " + fn_tnf_raw_in);
       IJ.write("Input Elastic Transformation file : " + fn_tnf_elastic);
       IJ.write("Output Raw Transformation file    : " + fn_tnf_raw_out);

       // Open target
       Opener opener=new Opener();
       ImagePlus targetImp;
       targetImp=opener.openImage(fn_target);
       if(targetImp == null)
           IJ.error("\nError: " + fn_target + " could not be opened\n");

       // Open source
       ImagePlus sourceImp;
       sourceImp = opener.openImage(fn_source);
       if(sourceImp == null)
           IJ.error("\nError: " + fn_source + " could not be opened\n");

       // We load the transformation raw file.
       double[][] transformation_x_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw_in, transformation_x_1, transformation_y_1);              

       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf_elastic);

       double [][]cx2 = new double[intervals+3][intervals+3];
       double [][]cy2 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf_elastic, cx2, cy2);
       
       double [][] outputTransformation_x = new double[targetImp.getHeight()][targetImp.getWidth()];
       double [][] outputTransformation_y = new double[targetImp.getHeight()][targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeRawElasticTransformations(targetImp, intervals, 
               transformation_x_1, transformation_y_1, cx2, cy2, outputTransformation_x, outputTransformation_y);
       
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw_out, targetImp.getWidth(), 
               targetImp.getHeight(), outputTransformation_x, outputTransformation_y);       
       
    } /* end method composeRawElasticTransformationsMacro */
    
} /* end class bUnwarpJ_ */

/*====================================================================
|   bUnwarpJClearAll
\===================================================================*/

/*------------------------------------------------------------------*/
/**
 * Class to clear all the processes and actions in bUnwarpJ.
 */
class bUnwarpJClearAll extends Dialog implements ActionListener
{ /* begin class bUnwarpJClearAll */

    /*....................................................................
       Private variables
    ....................................................................*/

    /** image plus for source image */
    private ImagePlus sourceImp;
    /** image plus for target image */
    private ImagePlus targetImp;
    /** point handler for source image */
    private bUnwarpJPointHandler sourcePh;
    /** point handler for target image */
    private bUnwarpJPointHandler targetPh;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Actions to take place with the dialog for clearing everything
     */
    public void actionPerformed (final ActionEvent ae) 
    {
       if (ae.getActionCommand().equals("Clear All")) {
          sourcePh.removePoints();
          targetPh.removePoints();
          setVisible(false);
       }
       else if (ae.getActionCommand().equals("Cancel")) {
          setVisible(false);
       }
    } /* end actionPerformed */

    /*------------------------------------------------------------------*/
    /**
     * Get the insets
     *
     * @return new insets
     */
    public Insets getInsets () 
    {
       return(new Insets(0, 20, 20, 20));
    } /* end getInsets */

    /*------------------------------------------------------------------*/
    /**
     * Create a new instance of bUnwarpJClearAll.
     *
     * @param parentWindow pointer to the parent window
     * @param sourceImp source image representation
     * @param targetImp target image representation
     * @param sourcePh point handler for the source image
     * @param targetPh point handler for the source image
     */
    bUnwarpJClearAll (
       final Frame parentWindow,
       final ImagePlus sourceImp,
       final ImagePlus targetImp,
       final bUnwarpJPointHandler sourcePh,
       final bUnwarpJPointHandler targetPh) 
    {
       super(parentWindow, "Removing Points", true);
       this.sourceImp = sourceImp;
       this.targetImp = targetImp;
       this.sourcePh = sourcePh;
       this.targetPh = targetPh;
       setLayout(new GridLayout(0, 1));
       final Button removeButton = new Button("Clear All");
       removeButton.addActionListener(this);
       final Button cancelButton = new Button("Cancel");
       cancelButton.addActionListener(this);
       final Label separation1 = new Label("");
       final Label separation2 = new Label("");
       add(separation1);
       add(removeButton);
       add(separation2);
       add(cancelButton);
       pack();
    } /* end bUnwarpJClearAll */

} /* end class bUnwarpJClearAll */

/*====================================================================
|   bUnwarpJCredits
\===================================================================*/

/*------------------------------------------------------------------*/
/**
 * Class to show the bUnwarpJ credits
 */
class bUnwarpJCredits extends Dialog
{ /* begin class bUnwarpJCredits */

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Get the insets.
     *
     * @return new insets
     */    
    public Insets getInsets () 
    {
       return(new Insets(0, 20, 20, 20));
    } /* end getInsets */

    /*------------------------------------------------------------------*/
    /**
     * Create a new instance of bUnwarpJCredits.
     *
     * @param parentWindow pointer to the parent window
     */
    public bUnwarpJCredits (final Frame parentWindow) 
    {
       super(parentWindow, "bUnwarpJ", true);
       setLayout(new BorderLayout(0, 20));
       final Label separation = new Label("");
       final Panel buttonPanel = new Panel();
       buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Button doneButton = new Button("Done");
       doneButton.addActionListener(
          new ActionListener (
          ) {
             public void actionPerformed (
                final ActionEvent ae
             ) {
                if (ae.getActionCommand().equals("Done")) {
                   dispose();
                }
             }
          }
       );
       buttonPanel.add(doneButton);
       final TextArea text = new TextArea(22, 72);
       text.setEditable(false);
       text.append("\n");
       text.append(" **************************************** bUnwarpJ ****************************************\n\n");
       text.append(" This work is an extension of the original UnwarpJ plugin. It has been modified in order\n" +
                   " to do bidirectional registration.\n");
       text.append("\n Its first version was developed by Ignacio Arganda-Carreras and Jan Kybic and finished\n" +
                   " in October 2005.\n");
       text.append("\n");
       text.append(" The work is based on the paper:\n");
       text.append("\n Ignacio Arganda-Carreras, Carlos O. S. Sorzano, Roberto Marabini, Jose M. Carazo,\n" +
                   " Carlos Ortiz de Solorzano, and Jan Kybic. 'Consistent and Elastic Registration of Histological\n" +
                   " Sections using Vector-Spline Regularization'. Lecture Notes in Computer Science, Springer\n" + 
                   " Berlin / Heidelberg, Volume 4241/2006, CVAMIA: Computer Vision Approaches to Medical\n" + 
                   " Image Analysis, Pages 85-95, 2006.\n");
       text.append("\n");
       text.append(" You'll be free to use this software for research purposes, but you should not redistribute it\n");
       text.append(" without our consent. In addition, we expect you to include a citation or acknowledgment\n");
       text.append(" whenever you present or publish results that are based on it.\n");
       add("North", separation);
       add("Center", text);
       add("South", buttonPanel);
       pack();
    } /* end bUnwarpJCredits */

} /* end class bUnwarpJCredits */

/*====================================================================
|   bUnwarpJCumulativeQueue
\===================================================================*/
/**
 * Class to create a cumulative queue in bUnwarpJ.
 */
class bUnwarpJCumulativeQueue extends Vector 
{
    /** front index of the queue */
    private int ridx;
    /** rear index of the queue */
    private int widx;
    /** current lenght of the queue */
    private int currentLength;
    /** queue sum */
    private double sum;

    /*------------------------------------------------------------------*/
    /**
     * Create a new instance of bUnwarpJCumulativeQueue.
     *
     * @param length length of the queue to be created
     */
    public bUnwarpJCumulativeQueue(int length)
    {
        currentLength=ridx=widx=0; setSize(length);
    }

    /*------------------------------------------------------------------*/
    /**
     * Get the current size of the queue.
     *
     * @return current size
     */
    public int currentSize(){return currentLength;}

    /*------------------------------------------------------------------*/
    /**
     * Get the sum of the queue.
     *
     * @return sum
     */
    public double getSum(){return sum;}

    /*------------------------------------------------------------------*/
    /**
     * Pop the value from the front of the queue.
     *
     * @return front value
     */
    public double pop_front() 
    {
       if (currentLength==0) 
           return 0.0;
       double x=((Double)elementAt(ridx)).doubleValue();
       currentLength--;
       sum-=x;
       ridx++; 
       if (ridx==size()) 
           ridx=0;
       return x;
    }

    /*------------------------------------------------------------------*/
    /**
     * Push a value at the end of the queue.
     */
    public void push_back(double x) 
    {
       if (currentLength==size()) 
           pop_front();
       setElementAt(new Double(x),widx);
       currentLength++;
       sum+=x;
       widx++; 
       if (widx==size()) 
           widx=0;
    }

} /* end class bUnwarpJCumulativeQueue */

/*====================================================================
|   bUnwarpJDialog
\===================================================================*/

/*------------------------------------------------------------------*/
/**
 * Class to create the dialog for bUnwarpJ.
 */
class bUnwarpJDialog extends Dialog implements ActionListener
{ /* begin class bUnwarpJDialog */

    /*....................................................................
       Private variables
    ....................................................................*/

    /** Advanced dialog */
    private Dialog advanced_dlg = null;

    /** List of available images in ImageJ */
    private ImagePlus[] imageList;

    // Image representations (canvas and ImagePlus) 
    /** Canvas of the source image */
    private ImageCanvas sourceIc;
    /** Canvas of the target image */
    private ImageCanvas targetIc;
    /** Image representation for source image */
    private ImagePlus sourceImp;
    /** Image representation for target image */
    private ImagePlus targetImp;

    // Image models
    /** Model for source image */
    private bUnwarpJImageModel source;
    /** Model for target image */
    private bUnwarpJImageModel target;

    // Image Masks
    /** Mask for source image */
    private bUnwarpJMask       sourceMsk;
    /** Mask for target image */
    private bUnwarpJMask       targetMsk;

    // Point handlers for the landmarks
    /** Point handlers for the landmarks in the source image */
    private bUnwarpJPointHandler sourcePh;
    /** Point handlers for the landmarks in the target image */
    private bUnwarpJPointHandler targetPh;


    /** Boolean for clearing mask */
    private boolean clearMask=false;
    /** Toolbar handler */
    private bUnwarpJPointToolbar tb
       = new bUnwarpJPointToolbar(Toolbar.getInstance(),this);

    // Final action
    /** flag to see if the finalAction was launched */
    private boolean finalActionLaunched=false;
    /** flag to stop the registration */
    private boolean stopRegistration=false;

    // Dialog related
    /** "done" button */
    private final Button DoneButton = new Button("Done");
    /** text field for the mininum scale deformation */
    private TextField min_scaleDeformationTextField;
    /** text field for the maximum scale deformation */
    private TextField max_scaleDeformationTextField;
    /** text field for divergency weight */
    private TextField divWeightTextField;
    /** text field for curl weight */
    private TextField curlWeightTextField;
    /** text field for landmark weight */
    private TextField landmarkWeightTextField;
    /** text field for image weight */
    private TextField imageWeightTextField;
    /** text field for the consistency weight */
    private TextField consistencyWeightTextField;
    /** text field for stopping threshold */
    private TextField stopThresholdTextField;

    /** index of the source choice */
    private int sourceChoiceIndex = 0;
    /** index of the target choice */
    private int targetChoiceIndex = 1;
    /** minimum scale deformation */
    private static int min_scale_deformation = 0;
    /** maximum scale deformation */
    private static int max_scale_deformation = 2;
    /** mode */
    private static int mode = 1;
    /** checkbox for rich output (verbose option) */
    private Checkbox ckRichOutput;
    /** checkbox for save transformation option */
    private Checkbox ckSaveTransformation;

    // Transformation parameters
    /** minium size */
    private static int     MIN_SIZE                   = 8;
    /** divergency weight */
    private static double  divWeight                  = 0;
    /** curl weight */
    private static double  curlWeight                 = 0;
    /** landmarks weight */
    private static double  landmarkWeight             = 0;
    /** image similarity weight */
    private static double  imageWeight                = 1;
    /** consistency weight */
    private static double  consistencyWeight          = 10; 
    /** flag for rich output (verbose option) */
    private static boolean richOutput                 = false;
    /** flag for save transformation option */
    private static boolean saveTransformation         = false;
    /** minimum image scale */
    private static int     min_scale_image            = 0;
    /** maximum depth for the image pyramid */
    private static int     imagePyramidDepth          = 3;
    /** stopping threshold */
    private static double  stopThreshold              = 1e-2;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Get source Mask.
     */
    public bUnwarpJMask getSourceMask () 
    {
        return this.sourceMsk;        
    } /* end getSourceMask */
    
    /*------------------------------------------------------------------*/
    /**
     * Get target Mask.
     */
    public bUnwarpJMask getTargetMask () 
    {
        return this.targetMsk;        
    }   /* end getTargetMask */
    
    /*------------------------------------------------------------------*/
    /**
     * Actions to be taken during the dialog.
     */
    public void actionPerformed (final ActionEvent ae) 
    {
       if (ae.getActionCommand().equals("Cancel")) 
       {
          dispose();
          restoreAll();
       }
       else if (ae.getActionCommand().equals("Done")) 
       {
          dispose();
          joinThreads();

          imagePyramidDepth = max_scale_deformation-min_scale_deformation+1;

          divWeight = Double.valueOf(divWeightTextField.getText()).doubleValue();

          curlWeight = Double.valueOf(curlWeightTextField.getText()).doubleValue();

          landmarkWeight = Double.valueOf(landmarkWeightTextField.getText()).doubleValue();

          imageWeight = Double.valueOf(imageWeightTextField.getText()).doubleValue();

          consistencyWeight = Double.valueOf(consistencyWeightTextField.getText()).doubleValue();

          richOutput = ckRichOutput.getState();

          saveTransformation = ckSaveTransformation.getState();

          int outputLevel=1;

          boolean showMarquardtOptim=false;

          if (richOutput) 
          {
             outputLevel++;
             showMarquardtOptim=true;
          }

          bUnwarpJFinalAction finalAction =
             new bUnwarpJFinalAction(this);

          finalAction.setup(sourceImp, targetImp,
             source, target, sourcePh, targetPh,
             sourceMsk, targetMsk, min_scale_deformation, max_scale_deformation,
             min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
             consistencyWeight, stopThreshold, outputLevel, showMarquardtOptim, mode);

          finalActionLaunched=true;
          tb.setAllUp(); 
          tb.repaint();
          finalAction.getThread().start();
       }
       else if (ae.getActionCommand().equals("Credits...")) 
       {
          final bUnwarpJCredits dialog = new bUnwarpJCredits(IJ.getInstance());
          GUI.center(dialog);
          dialog.setVisible(true);
       } 
       else if (ae.getActionCommand().equals("Advanced Options")) 
       {
           advanced_dlg.setVisible(true);
       } 
       else if (ae.getActionCommand().equals("Done")) 
       {
           advanced_dlg.setVisible(false);
       }
    } /* end actionPerformed */

    /*------------------------------------------------------------------*/
    /**
     * Apply the transformation defined by the spline coefficients to the source
     * image.
     * 
     * @param intervals intervals in the deformation
     * @param cx b-spline X- coefficients
     * @param cy b-spline Y- coefficients
     */
    public void applyTransformationToSource(
       int intervals,
       double [][]cx,
       double [][]cy) 
    {
       // Apply transformation
       bUnwarpJMiscTools.applyTransformationToSource(
          this.sourceImp, this.targetImp, this.source, intervals, cx, cy);

       // Restart the computation of the model
       cancelSource();
       this.targetPh.removePoints();

       createSourceImage(false);

       setSecondaryPointHandlers();
    }

    /*------------------------------------------------------------------*/
    /**
     * Apply a raw transformation yo the source image.
     * 
     * @param transformation_x X- mapping
     * @param transformation_y Y- mapping
     */
    public void applyRawTransformationToSource(
       double [][] transformation_x,
       double [][] transformation_y) 
    {
       // Apply transformation
       bUnwarpJMiscTools.applyRawTransformationToSource(this.sourceImp, this.targetImp, this.source, transformation_x, transformation_y);

       // Restart the computation of the model
       cancelSource();
       this.targetPh.removePoints();

       createSourceImage(false);

       setSecondaryPointHandlers();
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Create dialog for advance options.
     *
     * @param bIsReverse boolean flag to announce the use of consistency
     */
    public void createAdvancedOptions(final boolean bIsReverse) 
    {
       advanced_dlg = new Dialog(new Frame(), "Advanced Options", true);

       // Create min_scale_deformation, max_scale_deformation panel
       advanced_dlg.setLayout(new GridLayout(0, 1));
       final Choice min_scale_deformationChoice = new Choice();
       final Choice max_scale_deformationChoice = new Choice();
       final Panel min_scale_deformationPanel = new Panel();
       final Panel max_scale_deformationPanel = new Panel();
       min_scale_deformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       max_scale_deformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label min_scale_deformationLabel = new Label("Initial Deformation: ");
       final Label max_scale_deformationLabel = new Label("Final Deformation: ");
       min_scale_deformationChoice.add("Very Coarse");
       min_scale_deformationChoice.add("Coarse");
       min_scale_deformationChoice.add("Fine");
       min_scale_deformationChoice.add("Very Fine");
       max_scale_deformationChoice.add("Very Coarse");
       max_scale_deformationChoice.add("Coarse");
       max_scale_deformationChoice.add("Fine");
       max_scale_deformationChoice.add("Very Fine");
       max_scale_deformationChoice.add("Super Fine");
       min_scale_deformationChoice.select(min_scale_deformation);
       max_scale_deformationChoice.select(max_scale_deformation);
       min_scale_deformationChoice.addItemListener(
          new ItemListener (
          ) {
             public void itemStateChanged (
                final ItemEvent ie
             ) {
                final int new_min_scale_deformation = 
                   min_scale_deformationChoice.getSelectedIndex();
                int new_max_scale_deformation=max_scale_deformation;
                if (max_scale_deformation<new_min_scale_deformation)
                    new_max_scale_deformation=new_min_scale_deformation;
                if (new_min_scale_deformation!=min_scale_deformation ||
                    new_max_scale_deformation!=max_scale_deformation) {
                   min_scale_deformation=new_min_scale_deformation;
                   max_scale_deformation=new_max_scale_deformation;
                   computeImagePyramidDepth();
                   restartModelThreads(bIsReverse);
                }
                min_scale_deformationChoice.select(min_scale_deformation);
                max_scale_deformationChoice.select(max_scale_deformation);
             }
          }
       );
       max_scale_deformationChoice.addItemListener(
          new ItemListener (
          ) {
             public void itemStateChanged (
                final ItemEvent ie
             ) {
                final int new_max_scale_deformation = 
                   max_scale_deformationChoice.getSelectedIndex();
                int new_min_scale_deformation=min_scale_deformation;
                if (new_max_scale_deformation<min_scale_deformation)
                    new_min_scale_deformation=new_max_scale_deformation;
                if (new_max_scale_deformation!=max_scale_deformation ||
                    new_min_scale_deformation!=min_scale_deformation) {
                   min_scale_deformation=new_min_scale_deformation;
                   max_scale_deformation=new_max_scale_deformation;
                   computeImagePyramidDepth();
                   restartModelThreads(bIsReverse);
                }
                max_scale_deformationChoice.select(max_scale_deformation);
                min_scale_deformationChoice.select(min_scale_deformation);
             }
          }
       );
       min_scale_deformationPanel.add(min_scale_deformationLabel);
       max_scale_deformationPanel.add(max_scale_deformationLabel);
       min_scale_deformationPanel.add(min_scale_deformationChoice);
       max_scale_deformationPanel.add(max_scale_deformationChoice);

            // Create div and curl weight panels
       final Panel divWeightPanel= new Panel();
       divWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label label_divWeight = new Label();
       label_divWeight.setText("Divergence Weight:");
       divWeightTextField = new TextField("", 5);
       divWeightTextField.setText(""+divWeight);
       divWeightTextField.addTextListener(
          new TextListener (
          ) {
             public void textValueChanged (
                final TextEvent e
             ) {
                boolean validNumber =true;
                try {
                   divWeight = Double.valueOf(divWeightTextField.getText()).doubleValue();
                } catch (NumberFormatException n) {
                   validNumber = false;
                }
                DoneButton.setEnabled(validNumber);
             }
          }
       );
       divWeightPanel.add(label_divWeight);
       divWeightPanel.add(divWeightTextField);
            divWeightPanel.setVisible(true);

       final Panel curlWeightPanel= new Panel();
       curlWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label label_curlWeight = new Label();
       label_curlWeight.setText("Curl Weight:");
       curlWeightTextField = new TextField("", 5);
       curlWeightTextField.setText(""+curlWeight);
       curlWeightTextField.addTextListener(
          new TextListener (
          ) {
             public void textValueChanged (
                final TextEvent e
             ) {
                boolean validNumber =true;
                try {
                   curlWeight = Double.valueOf(curlWeightTextField.getText()).doubleValue();
                } catch (NumberFormatException n) {
                   validNumber = false;
                }
                DoneButton.setEnabled(validNumber);
             }
          }
       );
       curlWeightPanel.add(label_curlWeight);
       curlWeightPanel.add(curlWeightTextField);
            curlWeightPanel.setVisible(true);

       // Create landmark and image weight panels
       final Panel landmarkWeightPanel= new Panel();
       landmarkWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label label_landmarkWeight = new Label();
       label_landmarkWeight.setText("Landmark Weight:");
       landmarkWeightTextField = new TextField("", 5);
       landmarkWeightTextField.setText(""+landmarkWeight);
       landmarkWeightTextField.addTextListener(
          new TextListener (
          ) {
             public void textValueChanged (
                final TextEvent e
             ) {
                boolean validNumber =true;
                try {
                   landmarkWeight = Double.valueOf(landmarkWeightTextField.getText()).doubleValue();
                } catch (NumberFormatException n) {
                   validNumber = false;
                }
                DoneButton.setEnabled(validNumber);
             }
          }
       );
       landmarkWeightPanel.add(label_landmarkWeight);
       landmarkWeightPanel.add(landmarkWeightTextField);
       landmarkWeightPanel.setVisible(true);

       final Panel imageWeightPanel= new Panel();
       imageWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label label_imageWeight = new Label();
       label_imageWeight.setText("Image Weight:");
       imageWeightTextField = new TextField("", 5);
       imageWeightTextField.setText(""+imageWeight);
       imageWeightTextField.addTextListener(
          new TextListener (
          ) {
             public void textValueChanged (
                final TextEvent e
             ) {
                boolean validNumber =true;
                try {
                   imageWeight = Double.valueOf(imageWeightTextField.getText()).doubleValue();
                } catch (NumberFormatException n) {
                   validNumber = false;
                }
                DoneButton.setEnabled(validNumber);
             }
          }
       );
       imageWeightPanel.add(label_imageWeight);
       imageWeightPanel.add(imageWeightTextField);
       imageWeightPanel.setVisible(true);

       // Create consistency weight panel
       final Panel consistencyWeightPanel= new Panel();
       consistencyWeightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label label_consistencyWeight = new Label();
       label_consistencyWeight.setText("Consistency Weight:");
       consistencyWeightTextField = new TextField("", 5);
       consistencyWeightTextField.setText("" + consistencyWeight);
       consistencyWeightTextField.addTextListener(
          new TextListener (
          ) {
             public void textValueChanged (
                final TextEvent e
             ) {
                boolean validNumber =true;
                try {
                   consistencyWeight = Double.valueOf(consistencyWeightTextField.getText()).doubleValue();
                } catch (NumberFormatException n) {
                   validNumber = false;
                }
                DoneButton.setEnabled(validNumber);
             }
          }
       );
       consistencyWeightPanel.add(label_consistencyWeight);
       consistencyWeightPanel.add(consistencyWeightTextField);
       consistencyWeightPanel.setVisible(true);   


       // Create stopThreshold panel
       final Panel stopThresholdPanel= new Panel();
       stopThresholdPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label label_stopThreshold = new Label();
       label_stopThreshold.setText("Stop Threshold:");
       stopThresholdTextField = new TextField("", 5);
       stopThresholdTextField.setText(""+stopThreshold);
       stopThresholdTextField.addTextListener(
          new TextListener (
          ) {
             public void textValueChanged (
                final TextEvent e
             ) {
                boolean validNumber =true;
                try {
                   stopThreshold = Double.valueOf(stopThresholdTextField.getText()).doubleValue();
                } catch (NumberFormatException n) {
                   validNumber = false;
                }
                DoneButton.setEnabled(validNumber);
             }
          }
       );
       stopThresholdPanel.add(label_stopThreshold);
       stopThresholdPanel.add(stopThresholdTextField);
       stopThresholdPanel.setVisible(true);

       // Create checkbox for creating rich output
       final Panel richOutputPanel=new Panel();
       richOutputPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       ckRichOutput=new Checkbox("Verbose",richOutput);
       richOutputPanel.add(ckRichOutput);

       // Create checkbox for saving the transformation
       final Panel saveTransformationPanel=new Panel();
       saveTransformationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       ckSaveTransformation=new Checkbox("Save Transformation",saveTransformation);
       saveTransformationPanel.add(ckSaveTransformation);

       // Create close button
       final Panel buttonPanel = new Panel();
       buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Button CloseButton = new Button("Close");
       CloseButton.addActionListener(
          new ActionListener (
          ) {
             public void actionPerformed (
                final ActionEvent ae
             ) {
                if (ae.getActionCommand().equals("Close")) {
                   advanced_dlg.dispose();
                }
             }
          }
       );
       buttonPanel.add(CloseButton);

       // Build separations
       final Label separation1 = new Label("");

       // Create the dialog
       advanced_dlg.add(min_scale_deformationPanel);
       advanced_dlg.add(max_scale_deformationPanel);
       advanced_dlg.add(divWeightPanel);
       advanced_dlg.add(curlWeightPanel);
       advanced_dlg.add(landmarkWeightPanel);
       advanced_dlg.add(imageWeightPanel);
       advanced_dlg.add(consistencyWeightPanel);  
       advanced_dlg.add(stopThresholdPanel);
       advanced_dlg.add(richOutputPanel);
       advanced_dlg.add(saveTransformationPanel);
       advanced_dlg.add(separation1);
       advanced_dlg.add(buttonPanel);
       advanced_dlg.pack();

       advanced_dlg.setVisible(false);
    }

    /*------------------------------------------------------------------*/
    /**
     * Free the memory used in the program.
     */
    public void freeMemory() 
    {
       advanced_dlg = null;
       imageList    = null;
       sourceIc     = null;
       targetIc     = null;
       sourceImp    = null;
       targetImp    = null;
       source       = null;
       target       = null;
       sourcePh     = null;
       targetPh     = null;
       tb           = null;
       Runtime.getRuntime().gc();
    }

    /*------------------------------------------------------------------*/
    /**
     * Method to color the area of the mask.
     *
     * @param ph image point handler
     */
    public void grayImage(final bUnwarpJPointHandler ph) 
    {
       if (ph==sourcePh) 
       {
          int Xdim=source.getWidth();
          int Ydim=source.getHeight();
          FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
          int ij=0;
          double []source_data=source.getImage();
          for (int i=0; i<Ydim; i++)
             for (int j=0; j<Xdim; j++,ij++)
                if (sourceMsk.getValue(j,i))
                     fp.putPixelValue(j,i,    source_data[ij]);
                else fp.putPixelValue(j,i,0.5*source_data[ij]);
          fp.resetMinAndMax();
          sourceImp.setProcessor(sourceImp.getTitle(),fp);
          sourceImp.updateImage();
       } 
       else 
       {
          int Xdim=target.getWidth();
          int Ydim=target.getHeight();
          FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
          double []target_data=target.getImage();
          int ij=0;
          for (int i=0; i<Ydim; i++)
             for (int j=0; j<Xdim; j++,ij++)
                if (targetMsk.getValue(j,i))
                     fp.putPixelValue(j,i,    target_data[ij]);
                else fp.putPixelValue(j,i,0.5*target_data[ij]);
          fp.resetMinAndMax();
          targetImp.setProcessor(targetImp.getTitle(),fp);
          targetImp.updateImage();
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Get finalActionLaunched flag.
     */
    public boolean isFinalActionLaunched () {return finalActionLaunched;}

    /*------------------------------------------------------------------*/
    /**
     * Get clearMask flag.
     */
    public boolean isClearMaskSet () {return clearMask;}

    /*------------------------------------------------------------------*/
    /**
     * Get saveTransformation flag.
     */
    public boolean isSaveTransformationSet () {return saveTransformation;}

    /*------------------------------------------------------------------*/
    /**
     * Get stopRegistration flag.
     */
    public boolean isStopRegistrationSet () {return stopRegistration;}

    /*------------------------------------------------------------------*/
    /**
     * Get the insets.
     *
     * @return new insets
     */   
    public Insets getInsets () 
    {
       return(new Insets(0, 20, 20, 20));
    } /* end getInsets */

    /*------------------------------------------------------------------*/
    /**
     * Join the threads for the source and target images.
     */
    public void joinThreads () 
    {
       try 
       {
          source.getThread().join();
          target.getThread().join();
       } 
       catch (InterruptedException e) 
       {
          IJ.error("Unexpected interruption exception" + e);
       }
    } /* end joinSourceThread */

    /*------------------------------------------------------------------*/
    /**
     * Restore the initial conditions.
     */
    public void restoreAll () 
    {
       cancelSource();
       cancelTarget();
       tb.restorePreviousToolbar();
       Toolbar.getInstance().repaint();
       bUnwarpJProgressBar.resetProgressBar();
       Runtime.getRuntime().gc();
    } /* end restoreAll */

    /*------------------------------------------------------------------*/
    /**
     * Set the clearMask flag.
     */
    public void setClearMask (boolean val) {clearMask=val;}

    /*------------------------------------------------------------------*/
    /**
     * Set the stopRegistration flag to true.
     */
    public void setStopRegistration () {stopRegistration=true;}

    /*------------------------------------------------------------------*/
    /**
     * Create a new instance of bUnwarpJDialog.
     *
     * @param parentWindow pointer to the parent window
     * @param imageList list of images from ImageJ
     */
    public bUnwarpJDialog (
       final Frame parentWindow,
       final ImagePlus[] imageList) 
    {
       super(parentWindow, "bUnwarpJ", false);
       this.imageList = imageList;

       // Start concurrent image processing threads

       final boolean bIsReverse = true;

       createSourceImage(bIsReverse);
       createTargetImage();
       setSecondaryPointHandlers();

        // Create Source panel
       setLayout(new GridLayout(0, 1));
       final Choice sourceChoice = new Choice();
       final Choice targetChoice = new Choice();
       final Panel sourcePanel = new Panel();
       sourcePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label sourceLabel = new Label("Source: ");
       addImageList(sourceChoice);
       sourceChoice.select(sourceChoiceIndex);
       sourceChoice.addItemListener(
          new ItemListener (
          ) {
             public void itemStateChanged (
                final ItemEvent ie
             ) {
                final int newChoiceIndex = sourceChoice.getSelectedIndex();
                if (sourceChoiceIndex != newChoiceIndex) 
                {
                   stopSourceThread();
                   if (targetChoiceIndex != newChoiceIndex) 
                   {
                      sourceChoiceIndex = newChoiceIndex;
                      cancelSource();
                      targetPh.removePoints();
                      createSourceImage(bIsReverse);
                      setSecondaryPointHandlers();
                   }
                   else 
                   {
                      stopTargetThread();
                      targetChoiceIndex = sourceChoiceIndex;
                      sourceChoiceIndex = newChoiceIndex;
                      targetChoice.select(targetChoiceIndex);
                      permuteImages(bIsReverse);
                   }
                }
                repaint();
             }
          }
       );
       sourcePanel.add(sourceLabel);
       sourcePanel.add(sourceChoice);

       // Create target panel
       final Panel targetPanel = new Panel();
       targetPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label targetLabel = new Label("Target: ");
       addImageList(targetChoice);
       targetChoice.select(targetChoiceIndex);
       targetChoice.addItemListener(
          new ItemListener (
          ) {
             public void itemStateChanged (
                final ItemEvent ie
             ) 
             {
                final int newChoiceIndex = targetChoice.getSelectedIndex();
                if (targetChoiceIndex != newChoiceIndex) 
                {                    
                    stopTargetThread();
                    if (sourceChoiceIndex != newChoiceIndex) 
                    {
                      targetChoiceIndex = newChoiceIndex;
                      cancelTarget();
                      sourcePh.removePoints();
                      createTargetImage();
                      setSecondaryPointHandlers();
                    }
                    else 
                    {
                      stopSourceThread();
                      sourceChoiceIndex = targetChoiceIndex;
                      targetChoiceIndex = newChoiceIndex;
                      sourceChoice.select(sourceChoiceIndex);
                      permuteImages(bIsReverse);
                   }
                }
                repaint();
             }
          }
       );
       targetPanel.add(targetLabel);
       targetPanel.add(targetChoice);

        // Create mode panel
       setLayout(new GridLayout(0, 1));
       final Choice modeChoice = new Choice();
       final Panel modePanel = new Panel();
       modePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Label modeLabel = new Label("Registration Mode: ");
       modeChoice.add("Fast");
       modeChoice.add("Accurate");
       modeChoice.select(mode);
       modeChoice.addItemListener(
          new ItemListener (
          ) {
             public void itemStateChanged (
                final ItemEvent ie
             ) {
                final int mode = modeChoice.getSelectedIndex();
                if (mode==0) {
                   // Fast
                   min_scale_image=1;
                } else if (mode==1) {
                   // Accurate
                   min_scale_image=0;
                }
                    repaint();
             }
          }
       );
       modePanel.add(modeLabel);
       modePanel.add(modeChoice);

       // Build Advanced Options panel
       final Panel advancedOptionsPanel = new Panel();
       advancedOptionsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       final Button advancedOptionsButton = new Button("Advanced Options");
       advancedOptionsButton.addActionListener(this);
       advancedOptionsPanel.add(advancedOptionsButton);

       // Build Done Cancel Credits panel
       final Panel buttonPanel = new Panel();
       buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
       DoneButton.addActionListener(this);
       final Button cancelButton = new Button("Cancel");
       cancelButton.addActionListener(this);
       final Button creditsButton = new Button("Credits...");
       creditsButton.addActionListener(this);
       buttonPanel.add(cancelButton);
       buttonPanel.add(DoneButton);
       buttonPanel.add(creditsButton);

       // Build separations
       final Label separation1 = new Label("");
       final Label separation2 = new Label("");

       // Finally build dialog
       add(separation1);
       add(sourcePanel);
       add(targetPanel);
       add(modePanel);
       add(advancedOptionsPanel);
       add(separation2);
       add(buttonPanel);
       pack();

       createAdvancedOptions(bIsReverse);
    } /* end bUnwarpJDialog */

    /*------------------------------------------------------------------*/
    /**
     * Ungray image.
     *
     * @param pa
     */
    public void ungrayImage(final bUnwarpJPointAction pa) 
    {
       if (pa==sourcePh.getPointAction()) {
          int Xdim=source.getWidth();
          int Ydim=source.getHeight();
          FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
          int ij=0;
          double []source_data=source.getImage();
          for (int i=0; i<Ydim; i++)
             for (int j=0; j<Xdim; j++,ij++)
                 fp.putPixelValue(j,i,source_data[ij]);
          fp.resetMinAndMax();
          sourceImp.setProcessor(sourceImp.getTitle(),fp);
          sourceImp.updateImage();
       } else {
          int Xdim=target.getWidth();
          int Ydim=target.getHeight();
          FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
          double []target_data=target.getImage();
          int ij=0;
          for (int i=0; i<Ydim; i++)
             for (int j=0; j<Xdim; j++,ij++)
                 fp.putPixelValue(j,i,target_data[ij]);
          fp.resetMinAndMax();
          targetImp.setProcessor(targetImp.getTitle(),fp);
          targetImp.updateImage();
       }
    }

    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Add the image list to the list of choices.
     *
     * @param choice list of choices
     */
    private void addImageList (final Choice choice) 
    {
       for (int k = 0; (k < imageList.length); k++)
          choice.add(imageList[k].getTitle());
    } /* end addImageList */

    /*------------------------------------------------------------------*/
    /**
     * Close all the variables related to the source image.
     */
    private void cancelSource () 
    {
       sourcePh.killListeners();
       sourcePh  = null;
       sourceIc  = null;
       sourceImp.killRoi();
       sourceImp = null;
       source    = null;
       sourceMsk = null;
       Runtime.getRuntime().gc();
    } /* end cancelSource */

    /*------------------------------------------------------------------*/
    /**
     * Close all the variables related to the target image.
     */
    private void cancelTarget () 
    {
       targetPh.killListeners();
       targetPh  = null;
       targetIc  = null;
       targetImp.killRoi();
       targetImp = null;
       target    = null;
       targetMsk = null;
       Runtime.getRuntime().gc();
    } /* end cancelTarget */

    /*------------------------------------------------------------------*/
    /**
     * Compute the depth of the resolution pyramid.
     */
    private void computeImagePyramidDepth () 
    {
       imagePyramidDepth=max_scale_deformation-min_scale_deformation+1;
    }

    /*------------------------------------------------------------------*/
    /**
     * Create the source image.
     *
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     */
    private void createSourceImage (boolean bIsReverse) 
    {
       sourceImp = imageList[sourceChoiceIndex];
       sourceImp.setSlice(1);

       source    =
          new bUnwarpJImageModel(sourceImp.getProcessor(), bIsReverse);

       source.setPyramidDepth(imagePyramidDepth+min_scale_image);
       source.getThread().start();
       sourceIc  = sourceImp.getWindow().getCanvas();
       if (sourceImp.getStackSize()==1) { 
          // Create an empy mask
          sourceMsk = new bUnwarpJMask(sourceImp.getProcessor(),false);
       } else {
          // Take the mask from the second slice
          sourceImp.setSlice(2);
          sourceMsk = new bUnwarpJMask(sourceImp.getProcessor(),true);
          sourceImp.setSlice(1);
       }
       sourcePh  = new bUnwarpJPointHandler(sourceImp, tb, sourceMsk, this);
       tb.setSource(sourceImp, sourcePh);
    } /* end createSourceImage */

    /*------------------------------------------------------------------*/
    /**
     * Create target image.
     */
    private void createTargetImage () 
    {
       targetImp = imageList[targetChoiceIndex];
       targetImp.setSlice(1);
       target    =
          new bUnwarpJImageModel(targetImp.getProcessor(), true);
       target.setPyramidDepth(imagePyramidDepth+min_scale_image);
       target.getThread().start();
       targetIc  = targetImp.getWindow().getCanvas();
       if (targetImp.getStackSize()==1) { 
          // Create an empy mask
          targetMsk = new bUnwarpJMask(targetImp.getProcessor(),false);
       } else {
          // Take the mask from the second slice
          targetImp.setSlice(2);
          targetMsk = new bUnwarpJMask(targetImp.getProcessor(),true);
          targetImp.setSlice(1);
       }
       targetPh  = new bUnwarpJPointHandler(targetImp, tb, targetMsk, this);
       tb.setTarget(targetImp, targetPh);
    } /* end createTargetImage */

    /*------------------------------------------------------------------*/
    /**
     * Permute the pointer for the target and source images.
     *
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     */
    private void permuteImages (boolean bIsReverse) 
    {      
       // Swap image canvas
       final ImageCanvas swapIc = this.sourceIc;
       this.sourceIc = this.targetIc;
       this.targetIc = swapIc;

       // Swap ImagePlus
       final ImagePlus swapImp = this.sourceImp;
       this.sourceImp = this.targetImp;
       this.targetImp = swapImp;

       // Swap Mask
       final bUnwarpJMask swapMsk = this.sourceMsk;
       this.sourceMsk = this.targetMsk;
       this.targetMsk = swapMsk;

       // Swap Point Handlers
       final bUnwarpJPointHandler swapPh = this.sourcePh;
       this.sourcePh = this.targetPh;
       this.targetPh = swapPh;
       setSecondaryPointHandlers();

       // Inform the Toolbar about the change
       tb.setSource(this.sourceImp, this.sourcePh);
       tb.setTarget(this.targetImp, this.targetPh);

       // Restart the computation with each image
       this.source = new bUnwarpJImageModel(this.sourceImp.getProcessor(), bIsReverse);
       this.source.setPyramidDepth(imagePyramidDepth + min_scale_image);
       this.source.getThread().start();

       this.target = new bUnwarpJImageModel(this.targetImp.getProcessor(), true);
       this.target.setPyramidDepth(imagePyramidDepth + min_scale_image);
       this.target.getThread().start();
    } /* end permuteImages */

    /*------------------------------------------------------------------*/
    /**
     * Remove the points from the points handlers of the source and target image.
     */
    private void removePoints () 
    {
       sourcePh.removePoints();
       targetPh.removePoints();
    }

    /*------------------------------------------------------------------*/
    /**
     * Relaunch the threads for the image models of the source and target.
     *
     * @param bIsReverse boolean variable to indicate the use of consistency
     */
    private void restartModelThreads (boolean bIsReverse) 
    {
       // Stop threads
       stopSourceThread();
       stopTargetThread();

       // Remove the current image models
       source = null;
       target = null;
       Runtime.getRuntime().gc();

       // Now restart the threads
       source    =
          new bUnwarpJImageModel(sourceImp.getProcessor(), bIsReverse);
       source.setPyramidDepth(imagePyramidDepth+min_scale_image);
       source.getThread().start();

       target =
          new bUnwarpJImageModel(targetImp.getProcessor(), true);
       target.setPyramidDepth(imagePyramidDepth+min_scale_image);
       target.getThread().start();
    }

    /*------------------------------------------------------------------*/
    /**
     * Set the secondary point handlers.
     */
    private void setSecondaryPointHandlers () 
    {
       sourcePh.setSecondaryPointHandler(targetImp, targetPh);
       targetPh.setSecondaryPointHandler(sourceImp, sourcePh);
    } /* end setSecondaryPointHandler */

    /*------------------------------------------------------------------*/
    /**
     * Stop the thread of the source image.
     */
    private void stopSourceThread () 
    {
       // Stop the source image model
       while (source.getThread().isAlive()) {
          source.getThread().interrupt();
       }
       source.getThread().interrupted();
    } /* end stopSourceThread */

    /*------------------------------------------------------------------*/
    /**
     * Stop the thread of the target image.
     */
    private void stopTargetThread () 
    {
       // Stop the target image model
       while (target.getThread().isAlive()) {
          target.getThread().interrupt();
       }
       target.getThread().interrupted();
    } /* end stopTargetThread */

} /* end class bUnwarpJDialog */

/*====================================================================
|   bUnwarpJFile
\===================================================================*/

/*------------------------------------------------------------------*/
/**
 * Class to create a dialog to deal with the files to keep the information of bUnwarpJ.
 */
class bUnwarpJFile extends Dialog implements ActionListener
{ /* begin class bUnwarpJFile */

    /*....................................................................
       Private variables
    ....................................................................*/
    /** Checkbox for the list of choices */
    private final CheckboxGroup choice = new CheckboxGroup();
    /** Pointer to the source image representation */
    private ImagePlus sourceImp;
    /** Pointer to the target image representation */
    private ImagePlus targetImp;
    /** Point handler for the source image */
    private bUnwarpJPointHandler sourcePh;
    /** Point handler for the target image */
    private bUnwarpJPointHandler targetPh;
    /** Dialog for bUnwarpJ interface */
    private bUnwarpJDialog       dialog;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /** 
     * Actions to be taking during the dialog.
     */
    public void actionPerformed (final ActionEvent ae) 
    {
       this.setVisible(false);
       if (ae.getActionCommand().equals("Save Landmarks As...")) {
          savePoints();
       }
       else if (ae.getActionCommand().equals("Load Landmarks...")) {
          loadPoints();
       }
       else if (ae.getActionCommand().equals("Show Landmarks")) {
          showPoints();
       }
       else if (ae.getActionCommand().equals("Load Elastic Transformation")) {
           loadTransformation();       
       }
       else if (ae.getActionCommand().equals("Load Raw Transformation")) {
           loadRawTransformation();
       }
       else if (ae.getActionCommand().equals("Compare Opposite Elastic Transformations")) {
           compareOppositeElasticTransformations();
       }
       else if (ae.getActionCommand().equals("Compare Elastic/Raw Transformations")) {
           compareElasticWithRaw();
       }
       else if (ae.getActionCommand().equals("Compare Raw Transformations")) {
           compareRawTransformations();
       }
       else if (ae.getActionCommand().equals("Convert Transformation To Raw")) {
           saveTransformationInRaw();           
       }
       else if (ae.getActionCommand().equals("Compose Elastic Transformations")) {
           composeElasticTransformations();
       }
       else if (ae.getActionCommand().equals("Compose Raw Transformations")) {
           composeRawTransformations();
       }
       else if (ae.getActionCommand().equals("Compose Raw and Elastic Transformations")) {
           composeRawElasticTransformations();
       }
       else if (ae.getActionCommand().equals("Evaluate Image Similarity")) {
           evaluateSimilarity();
       }
       else if (ae.getActionCommand().equals("Cancel")) {
       }
    } /* end actionPerformed */

    /*------------------------------------------------------------------*//**
     * Get the insets.
     *
     * @return new insets
     */   
    public Insets getInsets () 
    {
       return(new Insets(0, 20, 20, 20));
    } /* end getInsets */

    /*------------------------------------------------------------------*/
    /**
     * Create a new instance of bUnwarpJFile.
     *
     * @param parentWindow pointer to the parent window 
     * @param sourceImp pointer to the source image represantation
     * @param targetImp pointer to the target image represantation
     * @param sourcePh point handler for the source image
     * @param targetPh point handler for the source image
     * @param dialog dialog for bUnwarpJ interface
     */
    bUnwarpJFile (
       final Frame parentWindow,
       final ImagePlus sourceImp,
       final ImagePlus targetImp,
       final bUnwarpJPointHandler sourcePh,
       final bUnwarpJPointHandler targetPh,
       final bUnwarpJDialog       dialog) 
    {
       super(parentWindow, "I/O Menu", true);
       this.sourceImp = sourceImp;
       this.targetImp = targetImp;
       this.sourcePh = sourcePh;
       this.targetPh = targetPh;
       this.dialog   = dialog;
       setLayout(new GridLayout(0, 1));
       
       final Button saveAsButton = new Button("Save Landmarks As...");
       final Button loadButton = new Button("Load Landmarks...");
       final Button show_PointsButton = new Button("Show Landmarks");
       final Button loadTransfButton = new Button("Load Elastic Transformation");
       final Button loadRawTransfButton = new Button("Load Raw Transformation");
       final Button compareOppositeTransfButton = new Button("Compare Opposite Elastic Transformations");
       final Button compareElasticRawTransfButton = new Button("Compare Elastic/Raw Transformations");       
       final Button compareRawButton = new Button("Compare Raw Transformations");       
       final Button convertToRawButton = new Button("Convert Transformation To Raw");
       final Button composeElasticButton = new Button("Compose Elastic Transformations");
       final Button composeRawButton = new Button("Compose Raw Transformations");
       final Button composeRawElasticButton = new Button("Compose Raw and Elastic Transformations");
       final Button evaluateSimilarityButton = new Button("Evaluate Image Similarity");
       final Button cancelButton = new Button("Cancel");
       
       saveAsButton.addActionListener(this);
       loadButton.addActionListener(this);
       show_PointsButton.addActionListener(this);
       loadTransfButton.addActionListener(this);
       loadRawTransfButton.addActionListener(this);
       cancelButton.addActionListener(this);
       compareOppositeTransfButton.addActionListener(this);
       compareElasticRawTransfButton.addActionListener(this);       
       compareRawButton.addActionListener(this);
       convertToRawButton.addActionListener(this);
       composeElasticButton.addActionListener(this);
       composeRawButton.addActionListener(this);
       composeRawElasticButton.addActionListener(this);
       evaluateSimilarityButton.addActionListener(this);
       
       final Label separation1 = new Label("");
       final Label separation2 = new Label("");
       add(separation1);
       add(loadButton);
       add(saveAsButton);
       add(show_PointsButton);
       add(loadTransfButton);
       add(loadRawTransfButton);
       add(compareOppositeTransfButton);
       add(compareElasticRawTransfButton);       
       add(compareRawButton);
       add(convertToRawButton);
       add(composeElasticButton);
       add(composeRawButton);
       add(composeRawElasticButton);
       add(evaluateSimilarityButton);
       add(separation2);
       add(cancelButton);
       pack();
    } /* end bUnwarpJFile */

    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Load the points from the point handlers.
     */
    private void loadPoints () 
    {
       final Frame f = new Frame();
       final FileDialog fd = new FileDialog(f, "Load Points", FileDialog.LOAD);
       fd.setVisible(true);
       final String path = fd.getDirectory();
       final String filename = fd.getFile();
       if ((path == null) || (filename == null)) return;

       Stack sourceStack = new Stack();
       Stack targetStack = new Stack();
       bUnwarpJMiscTools.loadPoints(path+filename,sourceStack,targetStack);

       sourcePh.removePoints();
       targetPh.removePoints();
       while ((!sourceStack.empty()) && (!targetStack.empty())) {
          Point sourcePoint = (Point)sourceStack.pop();
          Point targetPoint = (Point)targetStack.pop();
          sourcePh.addPoint(sourcePoint.x, sourcePoint.y);
          targetPh.addPoint(targetPoint.x, targetPoint.y);
       }
    } /* end loadPoints */

    /*------------------------------------------------------------------*/
    /**
     * Load a transformation and apply it to the source image.
     */
    private void loadTransformation () 
    {
       final Frame f = new Frame();
       final FileDialog fd = new FileDialog(f, "Load Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       final String path = fd.getDirectory();
       final String filename = fd.getFile();
       
       if ((path == null) || (filename == null)) 
           return;
       
       String fn_tnf = path+filename;

       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);

       double [][]cx = new double[intervals+3][intervals+3];
       double [][]cy = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx, cy);

       // Apply transformation
       dialog.applyTransformationToSource(intervals, cx, cy);
    }

    /*------------------------------------------------------------------*/
    /**
     * Load a raw transformation and apply it to the source image.
     */
    private void loadRawTransformation () 
    {
       final Frame f = new Frame();
       final FileDialog fd = new FileDialog(f, "Load Raw Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       final String path = fd.getDirectory();
       final String filename = fd.getFile();
       
       if ((path == null) || (filename == null)) 
           return;
       
       String fn_tnf = path+filename;

       double [][]transformation_x = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double [][]transformation_y = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];

       bUnwarpJMiscTools.loadRawTransformation(fn_tnf, transformation_x, transformation_y);

       // Apply transformation
       dialog.applyRawTransformationToSource(transformation_x, transformation_y);
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Save an elastic transformation in raw format
     */
    private void saveTransformationInRaw () 
    {
       // We ask the user for the elastic transformation file
       Frame f = new Frame();
       FileDialog fd = new FileDialog(f, "Load elastic transformation", FileDialog.LOAD);
       fd.setVisible(true);
       String path = fd.getDirectory();
       String filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       String fn_tnf = path+filename;

       int intervals=bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);

       double [][]cx = new double[intervals+3][intervals+3];
       double [][]cy = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx, cy);
       
             
        // We ask the user for the raw deformation file.
       Frame f_raw = new Frame();
       FileDialog fd_raw = new FileDialog(f_raw, "Saving in raw - select raw transformation file", FileDialog.SAVE);
       fd_raw.setVisible(true);
       String path_raw = fd_raw.getDirectory();
       String filename_raw = fd_raw.getFile();
       if ((path_raw == null) || (filename_raw == null))
          return;
       
       String fn_tnf_raw = path_raw + filename_raw;
       
       // We load the transformation raw file.
       double[][] transformation_x = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double[][] transformation_y = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       
       bUnwarpJMiscTools.convertElasticTransformationToRaw(this.targetImp, intervals, cx, cy, transformation_x, transformation_y); 
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw, this.targetImp.getWidth(), this.targetImp.getHeight(), transformation_x, transformation_y);
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Compare two opposite transformations (direct and inverse)
     * represented by elastic b-splines through the warping index.
     */
    private void compareOppositeElasticTransformations () 
    {
       // We ask the user for the direct transformation file
       Frame f = new Frame();
       FileDialog fd = new FileDialog(f, "Comparing - Load Direct Elastic Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       String path = fd.getDirectory();
       String filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       String fn_tnf = path+filename;

       int intervals=bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);

       double [][]cx_direct = new double[intervals+3][intervals+3];
       double [][]cy_direct = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx_direct, cy_direct);

       
       // We ask the user for the inverse transformation file
       fd = new FileDialog(f, "Comparing - Load Inverse Elastic Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       path = fd.getDirectory();
       filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       fn_tnf = path+filename;

       intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);

       double [][]cx_inverse = new double[intervals+3][intervals+3];
       double [][]cy_inverse = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx_inverse, cy_inverse);
       
             
       // Now we compare both transformations through the "warping index", which is 
       // a method equivalent to our consistency measure.
       
       double warpingIndex = bUnwarpJMiscTools.warpingIndex(this.sourceImp, this.targetImp, intervals, cx_direct, cy_direct, cx_inverse, cy_inverse);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!"); 
       
    }

    /*------------------------------------------------------------------*/
    /**
     * Compose two transformations represented by elastic b-splines 
     * into a raw mapping table (saved as usual).
     */
    private void composeElasticTransformations () 
    {
       // We ask the user for the first transformation file
       Frame f = new Frame();
       FileDialog fd = new FileDialog(f, "Composing - Load First Elastic Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       String path = fd.getDirectory();
       String filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       String fn_tnf = path+filename;

       int intervals=bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);

       double [][]cx1 = new double[intervals+3][intervals+3];
       double [][]cy1 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx1, cy1);

       
       // We ask the user for the second transformation file
       fd = new FileDialog(f, "Composing - Load Second Elastic Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       path = fd.getDirectory();
       filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       fn_tnf = path+filename;

       intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);

       double [][]cx2 = new double[intervals+3][intervals+3];
       double [][]cy2 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx2, cy2);
       
       double [][] outputTransformation_x = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double [][] outputTransformation_y = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeElasticTransformations(this.targetImp, intervals, 
               cx1, cy1, cx2, cy2, outputTransformation_x, outputTransformation_y);
       
       // We ask the user for the raw deformation file where we will save the mapping table.
       Frame f_raw = new Frame();
       FileDialog fd_raw = new FileDialog(f_raw, "Composing - Save Raw Transformation", FileDialog.SAVE);
       fd_raw.setVisible(true);
       String path_raw = fd_raw.getDirectory();
       String filename_raw = fd_raw.getFile();
       if ((path_raw == null) || (filename_raw == null)) {
          return;
       }
       String fn_tnf_raw = path_raw + filename_raw;
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw, this.targetImp.getWidth(), 
               this.targetImp.getHeight(), outputTransformation_x, outputTransformation_y);       
    }    

    /*------------------------------------------------------------------*/
    /**
     * Compose a raw transformation with an elastic transformation 
     * represented by elastic b-splines into a raw mapping table (saved as usual).
     */
    private void composeRawElasticTransformations () 
    {
       // We ask the user for the first transformation file
       Frame f = new Frame();
       FileDialog fd = new FileDialog(f, "Composing - Load First (Raw) Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       String path = fd.getDirectory();
       String filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       String fn_tnf = path+filename;

       double[][] transformation_x_1 = new double[targetImp.getHeight()][targetImp.getWidth()];
       double[][] transformation_y_1 = new double[targetImp.getHeight()][targetImp.getWidth()];

       bUnwarpJMiscTools.loadRawTransformation(fn_tnf, transformation_x_1, transformation_y_1);

       
       // We ask the user for the second transformation file
       fd = new FileDialog(f, "Composing - Load Second (Elastic) Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       path = fd.getDirectory();
       filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       fn_tnf = path+filename;

       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);

       double [][]cx2 = new double[intervals+3][intervals+3];
       double [][]cy2 = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx2, cy2);
       
       double [][] outputTransformation_x = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double [][] outputTransformation_y = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeRawElasticTransformations(this.targetImp, intervals, 
               transformation_x_1, transformation_y_1, cx2, cy2, outputTransformation_x, outputTransformation_y);
       
       // We ask the user for the raw deformation file where we will save the mapping table.
       Frame f_raw = new Frame();
       FileDialog fd_raw = new FileDialog(f_raw, "Composing - Save Raw Transformation", FileDialog.SAVE);
       fd_raw.setVisible(true);
       String path_raw = fd_raw.getDirectory();
       String filename_raw = fd_raw.getFile();
       if ((path_raw == null) || (filename_raw == null)) {
          return;
       }
       String fn_tnf_raw = path_raw + filename_raw;
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw, this.targetImp.getWidth(), 
               this.targetImp.getHeight(), outputTransformation_x, outputTransformation_y);       
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Compose two random (raw) deformations.
     */
    private void composeRawTransformations () 
    {
            
       // We ask the user for the first raw deformation file.
       Frame f_raw = new Frame();
       FileDialog fd_raw = new FileDialog(f_raw, "Composing - Load First Raw Transformation", FileDialog.LOAD);
       fd_raw.setVisible(true);
       String path_raw = fd_raw.getDirectory();
       String filename_raw = fd_raw.getFile();
       if ((path_raw == null) || (filename_raw == null)) {
          return;
       }
       String fn_tnf_raw = path_raw + filename_raw;
       
       // We load the transformation raw file.
       double[][] transformation_x_1 = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double[][] transformation_y_1 = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw, transformation_x_1, transformation_y_1);
       
       // We ask the user for the second raw deformation file.
       Frame f_raw_2 = new Frame();
       FileDialog fd_raw_2 = new FileDialog(f_raw_2, "Comparing - Load Second Raw Transformation", FileDialog.LOAD);
       fd_raw_2.setVisible(true);
       String path_raw_2 = fd_raw_2.getDirectory();
       String filename_raw_2 = fd_raw_2.getFile();
       if ((path_raw_2 == null) || (filename_raw_2 == null)) 
          return;
       
       String fn_tnf_raw_2 = path_raw_2 + filename_raw_2;
       
       // We load the transformation raw file.
       double[][] transformation_x_2 = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double[][] transformation_y_2 = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw_2, transformation_x_2, transformation_y_2);
       
       double [][] outputTransformation_x = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double [][] outputTransformation_y = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
             
       // Now we compose them and get as result a raw transformation mapping.
       bUnwarpJMiscTools.composeRawTransformations(this.targetImp.getWidth(), this.targetImp.getHeight(), 
               transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2, 
               outputTransformation_x, outputTransformation_y);
       
       // We ask the user for the raw deformation file where we will save the mapping table.
       Frame f_raw_out = new Frame();
       FileDialog fd_raw_out = new FileDialog(f_raw_out, "Composing - Save Raw Transformation", FileDialog.SAVE);
       fd_raw_out.setVisible(true);
       String path_raw_out = fd_raw_out.getDirectory();
       String filename_raw_out = fd_raw_out.getFile();
       if ((path_raw_out == null) || (filename_raw_out == null)) {
          return;
       }
       String fn_tnf_raw_out = path_raw_out + filename_raw_out;
       
       bUnwarpJMiscTools.saveRawTransformation(fn_tnf_raw_out, this.targetImp.getWidth(), 
               this.targetImp.getHeight(), outputTransformation_x, outputTransformation_y);
    }     
    
    /*------------------------------------------------------------------*/
    /**
     * Compare an elastic splines transformation with a random deformation
     * (in raw format) by the warping index.
     */
    private void compareElasticWithRaw () 
    {
       // We ask the user for the direct transformation file
       Frame f = new Frame();
       FileDialog fd = new FileDialog(f, "Comparing - Load Elastic Transformation", FileDialog.LOAD);
       fd.setVisible(true);
       String path = fd.getDirectory();
       String filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       String fn_tnf = path+filename;

       int intervals = bUnwarpJMiscTools.numberOfIntervalsOfTransformation(fn_tnf);

       double [][]cx_direct = new double[intervals+3][intervals+3];
       double [][]cy_direct = new double[intervals+3][intervals+3];

       bUnwarpJMiscTools.loadTransformation(fn_tnf, cx_direct, cy_direct);

       
       // We ask the user for the raw deformation file.
       Frame f_raw = new Frame();
       FileDialog fd_raw = new FileDialog(f_raw, "Comparing - Load Raw Transformation", FileDialog.LOAD);
       fd_raw.setVisible(true);
       String path_raw = fd_raw.getDirectory();
       String filename_raw = fd_raw.getFile();
       if ((path_raw == null) || (filename_raw == null)) {
          return;
       }
       String fn_tnf_raw = path_raw + filename_raw;
       
       // We load the transformation raw file.
       double[][] transformation_x = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double[][] transformation_y = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw, transformation_x, 
               transformation_y);
       
       double warpingIndex = bUnwarpJMiscTools.rawWarpingIndex(this.sourceImp, 
               this.targetImp, intervals, cx_direct, cy_direct, transformation_x, transformation_y);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!");
    }
  
    /*------------------------------------------------------------------*/
    /**
     * Compare two random (raw) deformations by the warping index.
     */
    private void compareRawTransformations () 
    {
            
       // We ask the user for the first raw deformation file.
       Frame f_raw = new Frame();
       FileDialog fd_raw = new FileDialog(f_raw, "Comparing - Load First Raw Transformation", FileDialog.LOAD);
       fd_raw.setVisible(true);
       String path_raw = fd_raw.getDirectory();
       String filename_raw = fd_raw.getFile();
       if ((path_raw == null) || (filename_raw == null)) {
          return;
       }
       String fn_tnf_raw = path_raw + filename_raw;
       
       // We load the transformation raw file.
       double[][] transformation_x_1 = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double[][] transformation_y_1 = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw, transformation_x_1, transformation_y_1);
       
       // We ask the user for the second raw deformation file.
       Frame f_raw_2 = new Frame();
       FileDialog fd_raw_2 = new FileDialog(f_raw_2, "Comparing - Load Second Raw Transformation", FileDialog.LOAD);
       fd_raw_2.setVisible(true);
       String path_raw_2 = fd_raw_2.getDirectory();
       String filename_raw_2 = fd_raw_2.getFile();
       if ((path_raw_2 == null) || (filename_raw_2 == null)) 
          return;
       
       String fn_tnf_raw_2 = path_raw_2 + filename_raw_2;
       
       // We load the transformation raw file.
       double[][] transformation_x_2 = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       double[][] transformation_y_2 = new double[this.targetImp.getHeight()][this.targetImp.getWidth()];
       bUnwarpJMiscTools.loadRawTransformation(fn_tnf_raw_2, transformation_x_2, transformation_y_2);
       
       double warpingIndex = bUnwarpJMiscTools.rawWarpingIndex(this.sourceImp, 
               this.targetImp, transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2);

       if(warpingIndex != -1)
           IJ.write(" Warping index = " + warpingIndex);             
       else
           IJ.write(" Warping index could not be evaluated because not a single pixel matched after the deformation!");
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Calculate the similarity error between two images.
     */
    private void evaluateSimilarity () 
    {
        
        double imageSimilarity = 0;
        int n = 0;
        
        bUnwarpJMask targetMsk = this.dialog.getTargetMask();
        
        for (int v=0; v < this.targetImp.getHeight(); v++) 
          {
              for (int u=0; u<this.targetImp.getWidth(); u++) 
              {
                  if (targetMsk.getValue(u, v)) 
                  {
                      // Compute image term .....................................................
                      double I2 = (double) (this.targetImp.getPixel(u, v)[0]);
                      double I1 = (double) (this.sourceImp.getPixel(u, v)[0]);


                      double error = I2 - I1;
                      double error2 = error*error;

                      imageSimilarity += error2;
                      n++;
                  }
              }
        }
                   
        if(n != 0)
            IJ.write(" Image similarity = " + (imageSimilarity / n) + ", n = " + n);             
        else
            IJ.write(" Error: not a single pixel was evaluated ");
               
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Save the points.
     */
    private void savePoints () 
    {
       final Frame f = new Frame();
       final FileDialog fd = new FileDialog(f, "Save Points", FileDialog.SAVE);
       String filename = targetImp.getTitle();
       int dot = filename.lastIndexOf('.');
       if (dot == -1) {
          fd.setFile(filename + ".txt");
       }
       else {
          filename = filename.substring(0, dot);
          fd.setFile(filename + ".txt");
       }
       fd.setVisible(true);
       final String path = fd.getDirectory();
       filename = fd.getFile();
       if ((path == null) || (filename == null)) {
          return;
       }
       try {
          final FileWriter fw = new FileWriter(path + filename);
          final Vector sourceList = sourcePh.getPoints();
          final Vector targetList = targetPh.getPoints();
          Point sourcePoint;
          Point targetPoint;
          String n;
          String xSource;
          String ySource;
          String xTarget;
          String yTarget;
          fw.write("Index\txSource\tySource\txTarget\tyTarget\n");
          for (int k = 0; (k < sourceList.size()); k++) {
             n = "" + k;
             while (n.length() < 5) {
                n = " " + n;
             }
             sourcePoint = (Point)sourceList.elementAt(k);
             xSource = "" + sourcePoint.x;
             while (xSource.length() < 7) {
                xSource = " " + xSource;
             }
             ySource = "" + sourcePoint.y;
             while (ySource.length() < 7) {
                ySource = " " + ySource;
             }
             targetPoint = (Point)targetList.elementAt(k);
             xTarget = "" + targetPoint.x;
             while (xTarget.length() < 7) {
                xTarget = " " + xTarget;
             }
             yTarget = "" + targetPoint.y;
             while (yTarget.length() < 7) {
                yTarget = " " + yTarget;
             }
             fw.write(n + "\t" + xSource + "\t" + ySource + "\t" + xTarget + "\t" + yTarget + "\n");
          }
          fw.close();
       } catch (IOException e) {
          IJ.error("IOException exception" + e);
       } catch (SecurityException e) {
          IJ.error("Security exception" + e);
       }
    } /* end savePoints */

    /*------------------------------------------------------------------*/
    /**
     * Display the points over the images.
     */
    private void showPoints () 
    {
       final Vector sourceList = sourcePh.getPoints();
       final Vector targetList = targetPh.getPoints();
       Point sourcePoint;
       Point targetPoint;
       String n;
       String xTarget;
       String yTarget;
       String xSource;
       String ySource;
       IJ.getTextPanel().setFont(new Font("Monospaced", Font.PLAIN, 12));
       IJ.setColumnHeadings("Index\txSource\tySource\txTarget\tyTarget");
       for (int k = 0; (k < sourceList.size()); k++) {
          n = "" + k;
          while (n.length() < 5) {
             n = " " + n;
          }
          sourcePoint = (Point)sourceList.elementAt(k);
          xTarget = "" + sourcePoint.x;
          while (xTarget.length() < 7) {
             xTarget = " " + xTarget;
          }
          yTarget = "" + sourcePoint.y;
          while (yTarget.length() < 7) {
             yTarget = " " + yTarget;
          }
          targetPoint = (Point)targetList.elementAt(k);
          xSource = "" + targetPoint.x;
          while (xSource.length() < 7) {
             xSource = " " + xSource;
          }
          ySource = "" + targetPoint.y;
          while (ySource.length() < 7) {
             ySource = " " + ySource;
          }
          IJ.write(n + "\t" + xSource + "\t" + ySource + "\t" + xTarget + "\t" + yTarget);
       }
    } /* end showPoints */

} /* end class bUnwarpJFile */


/*====================================================================
|   bUnwarpJFinalAction
\===================================================================*/

/*------------------------------------------------------------------*/
/**
 * Class to launch the registration in bUnwarpJ.
 */
class bUnwarpJFinalAction implements Runnable
{
    /*....................................................................
       Private variables
    ....................................................................*/

    /** thread to run the registration method */
    private Thread t;
    /** dialog for bUnwarpJ interface */
    private bUnwarpJDialog dialog;

    // Images
    /** image representation for the source */
    private ImagePlus                      sourceImp;
    /** image representation for the target */
    private ImagePlus                      targetImp;
    /** source image model */
    private bUnwarpJImageModel   source;
    /** target image model */
    private bUnwarpJImageModel   target;

    // Landmarks
    /** point handler for the landmarks in the source image*/
    private bUnwarpJPointHandler sourcePh;
    /** point handler for the landmarks in the target image*/
    private bUnwarpJPointHandler targetPh;

    // Masks for the images
    /** source image mask */
    private bUnwarpJMask sourceMsk;
    /** target image mask */
    private bUnwarpJMask targetMsk;

    // Transformation parameters
    /** minimum scale deformation */
    private int     min_scale_deformation;
    /** maximum scale deformation */
    private int     max_scale_deformation;
    /** minimum image scale */
    private int     min_scale_image;
    /** flag to specify the level of resolution in the output */
    private int     outputLevel;
    /** flag to show the optimizer */
    private boolean showMarquardtOptim;
    /** divergency weight */
    private double  divWeight;
    /** curl weight */
    private double  curlWeight;
    /** landmark weight */
    private double  landmarkWeight;
    /** weight for image similarity */
    private double  imageWeight;
    /** weight for the deformations consistency */
    private double  consistencyWeight;
    /** stopping threshold */
    private double  stopThreshold;
    /** level of accuracy */
    private int     accurate_mode;

    /*....................................................................
       Public methods
    ....................................................................*/
    /**     
     * Get the thread.
     *
     * @return the thread associated with this <code>bUnwarpJFinalAction</code>
     *         object
     */
    public Thread getThread () 
    {
       return(t);
    } /* end getThread */

    /**
     * Perform the registration
     */
    public void run () 
    {
        // Create output image (source-target)
        int Ydimt = target.getHeight();
        int Xdimt = target.getWidth();
        int Xdims = source.getWidth();

        final FloatProcessor fp = new FloatProcessor(Xdimt, Ydimt);

        for (int i=0; i<Ydimt; i++)
           for (int j=0; j<Xdimt; j++)
               if (sourceMsk.getValue(j, i) && targetMsk.getValue(j, i))
                  fp.putPixelValue(j, i, (target.getImage())[i*Xdimt+j]-
                                         (source.getImage())[i*Xdims+j]);
               else fp.putPixelValue(j, i, 0);
        fp.resetMinAndMax();
        final ImagePlus      ip1 = new ImagePlus("Output Source-Target", fp);
        final ImageWindow    iw1 = new ImageWindow(ip1);
        ip1.updateImage();

        // Create output image (target-source)
        int Ydims = source.getHeight();        

        final FloatProcessor fp2 = new FloatProcessor(Xdims, Ydims);

        for (int i=0; i<Ydims; i++)
           for (int j=0; j<Xdims; j++)
               if (targetMsk.getValue(j, i) && sourceMsk.getValue(j, i))
                  fp2.putPixelValue(j, i, (source.getImage())[i*Xdims+j]-
                                          (target.getImage())[i*Xdimt+j]);
               else fp2.putPixelValue(j, i, 0);
        fp2.resetMinAndMax();
        final ImagePlus      ip2 = new ImagePlus("Output Target-Source", fp2);
        final ImageWindow    iw2 = new ImageWindow(ip2);
        ip2.updateImage();

        // Perform the registration    
        final bUnwarpJTransformation warp = new bUnwarpJTransformation(
          sourceImp, targetImp, source, target, sourcePh, targetPh,
          sourceMsk, targetMsk, min_scale_deformation, max_scale_deformation,
          min_scale_image, divWeight, curlWeight, landmarkWeight, imageWeight,
          consistencyWeight, stopThreshold, outputLevel, showMarquardtOptim, accurate_mode,
          dialog.isSaveTransformationSet(), "", "", ip1, ip2, dialog);

        warp.doRegistration();

        dialog.ungrayImage(sourcePh.getPointAction());
        dialog.ungrayImage(targetPh.getPointAction());
        dialog.restoreAll();
        dialog.freeMemory();
    }

    /**
     * Pass parameter from <code>bUnwarpJDialog</code> to
     * <code>bUnwarpJFinalAction</code>.
     * 
     * @param sourceImp image representation for the source
     * @param targetImp image representation for the target
     * @param source source image model
     * @param target target image model
     * @param sourcePh point handler for the landmarks in the source image
     * @param targetPh point handler for the landmarks in the target image
     * @param sourceMsk source image mask
     * @param targetMsk target image mask
     * @param min_scale_deformation minimum scale deformation 
     * @param max_scale_deformation maximum scale deformation
     * @param min_scale_image minimum image scale
     * @param outputLevel flag to specify the level of resolution in the output
     * @param showMarquardtOptim flag to show the optimizer
     * @param divWeight divergency weight
     * @param curlWeight curl weight
     * @param landmarkWeight landmark weight
     * @param imageWeight weight for image similarity
     * @param consistencyWeight weight for the deformations consistency
     * @param stopThreshold stopping threshold 
     * @param accurate_mode level of accuracy 
     */
    public void setup (
       final ImagePlus sourceImp,
       final ImagePlus targetImp,
       final bUnwarpJImageModel source,
       final bUnwarpJImageModel target,
       final bUnwarpJPointHandler sourcePh,
       final bUnwarpJPointHandler targetPh,
       final bUnwarpJMask sourceMsk,
       final bUnwarpJMask targetMsk,
       final int min_scale_deformation,
       final int max_scale_deformation,
       final int min_scale_image,
       final double divWeight,
       final double curlWeight,
       final double landmarkWeight,
       final double imageWeight,
       final double consistencyWeight,
       final double stopThreshold,
       final int outputLevel,
       final boolean showMarquardtOptim,
       final int accurate_mode) 
    {
       this.sourceImp             = sourceImp;
       this.targetImp             = targetImp;
       this.source                = source;
       this.target                = target;
       this.sourcePh              = sourcePh;
       this.targetPh              = targetPh;
       this.sourceMsk             = sourceMsk;
       this.targetMsk             = targetMsk;
       this.min_scale_deformation = min_scale_deformation;
       this.max_scale_deformation = max_scale_deformation;
       this.min_scale_image       = min_scale_image;
       this.divWeight             = divWeight;
       this.curlWeight            = curlWeight;
       this.landmarkWeight        = landmarkWeight;
       this.imageWeight           = imageWeight;
       this.consistencyWeight     = consistencyWeight;
       this.stopThreshold         = stopThreshold;
       this.outputLevel           = outputLevel;
       this.showMarquardtOptim    = showMarquardtOptim;
       this.accurate_mode         = accurate_mode;
    } /* end setup */

    /**
     * Start a thread under the control of the main event loop. This thread
     * has access to the progress bar, while methods called directly from
     * within <code>bUnwarpJDialog</code> do not because they are
     * under the control of its own event loop.
     */
    public bUnwarpJFinalAction (final bUnwarpJDialog dialog) 
    {
       this.dialog = dialog;
       t = new Thread(this);
       t.setDaemon(true);
    }
}

/*====================================================================
|   bUnwarpJImageModel
\===================================================================*/

/*------------------------------------------------------------------*/
/**
 * Class for representing the images by cubic b-splines
 */
class bUnwarpJImageModel implements Runnable
{ /* begin class bUnwarpJImageModel */
    
    // Some constants
    /** minimum image size */
    private static int min_image_size = 4;


    /*....................................................................
       Private variables
    ....................................................................*/
    // Thread
    /** thread to create the model */
    private Thread t;

    // Stack for the pyramid of images/coefficients
    /** stack of coefficients pyramid */
    private final Stack cpyramid   = new Stack();
    /** stack of image pyramid */
    private final Stack imgpyramid = new Stack();

    // Original image, image spline coefficients, and gradient
    /** original image */
    private double[] image;
    /** image spline coefficients */
    private double[] coefficient;

    // Current image (the size might be different from the original)
    /** current image */
    private double[] currentImage;
    /** current image spline coefficients */
    private double[] currentCoefficient;
    /** current image width */
    private int      currentWidth;
    /** current image height */
    private int      currentHeight;
    /** twice current image width */
    private int      twiceCurrentWidth;
    /** twice current image height */
    private int      twiceCurrentHeight;

    // Size and other information
    /** image width */
    private int     width;
    /** image height */
    private int     height;
    /** twice image width */
    private int     twiceWidth;
    /** twice image height */
    private int     twiceHeight;
    /** pyramid depth */
    private int     pyramidDepth;
    /** current pyramid depth*/
    private int     currentDepth;
    /** smallest image width */
    private int     smallestWidth;
    /** smallest image height */
    private int     smallestHeight;
    /** flag to check target image */
    private boolean isTarget;
    /** flag to check if the coefficietns are mirrored */
    private boolean coefficientsAreMirrored;

    // Some variables to speedup interpolation
    // All these information is set through prepareForInterpolation()

    // Point to interpolate
    /** x component of the point to interpolate */
    private double   x;           
    /** y component of the point to interpolate */
    private double   y;   

    // Indexes related
    /** x index */
    public  int      xIndex[];
    /** y index */
    public  int      yIndex[];

    // Weights of the splines related
    /** x component of the weight of the spline */
    private double   xWeight[];   
    /** y component of the weight of the spline */
    private double   yWeight[];

    // Weights of the derivatives splines related
    /** x component of the weight of derivative spline */
    private double   dxWeight[];  
    /** y component of the weight of derivative spline */
    private double   dyWeight[];

    // Weights of the second derivatives splines related
    /** x component of the weight of second derivative spline */
    private double   d2xWeight[]; 
    /** y component of the weight of second derivative spline */
    private double   d2yWeight[];

    /** Interpolation source (current or original) */
    private boolean  fromCurrent;  

    // Size of the image used for the interpolation
    /** width of the image used for the interpolation */
    private int      widthToUse;  
    /** height of the image used for the interpolation */
    private int      heightToUse;

    // Some variables to speedup interpolation (precomputed)
    // All these information is set through prepareForInterpolation()
    // Indexes related
    /** precomputed x index */
    public  int      prec_xIndex[][];    
    /** precomputed y index */
    public  int      prec_yIndex[][];
    // Weights of the splines related
    /** precomputed x component of the weight of the spline */
    private double   prec_xWeight[][];   
    /** precomputed y component of the weight of the spline */
    private double   prec_yWeight[][];
    // Weights of the derivatives splines related
    /** precomputed x component of the weight of derivative spline */
    private double   prec_dxWeight[][];  
    /** precomputed y component of the weight of derivative spline */
    private double   prec_dyWeight[][];
    // Weights of the second derivatives splines related
    /** precomputed x component of the weight of second derivative spline */
    private double   prec_d2xWeight[][]; 
    /** precomputed y component of the weight of second derivative spline */
    private double   prec_d2yWeight[][];

    /*....................................................................
       Public methods
    ....................................................................*/

    /**
     * Clear the pyramid. 
     */
    public void clearPyramids ()
    {
       cpyramid.removeAllElements();
       imgpyramid.removeAllElements();
    } /* end clearPyramid */

    /*------------------------------------------------------------------*/
    /**
     * Get b-spline coefficients.
     *
     * @return the full-size B-spline coefficients 
     */
    public double[] getCoefficient () {return coefficient;}

    /*------------------------------------------------------------------*/
    /**
     * Get current height.
     *
     * @return the current height of the image/coefficients 
     */
    public int getCurrentHeight() {return currentHeight;}

    /*------------------------------------------------------------------*/
    /** 
     * Get current image.
     *
     * @return the current image of the image/coefficients 
     */
    public double[] getCurrentImage() {return currentImage;}

    /*------------------------------------------------------------------*/
    /**
     * Get current width. 
     *
     * @return the current width of the image/coefficients 
     */
    public int getCurrentWidth () {return currentWidth;}

    /*------------------------------------------------------------------*/
    /**
     * Get factor height.
     *
     * @return the relationship between the current size of the image
     *         and the original size 
     */
    public double getFactorHeight () {return (double)currentHeight/height;}

    /*------------------------------------------------------------------*/
    /** 
     * Get fact or width.
     * 
     * @return the relationship between the current size of the image
     *         and the original size. 
     */
    public double getFactorWidth () {return (double)currentWidth/width;}

    /*------------------------------------------------------------------*/
    /**
     * Get current depth.
     * 
     * @return the current depth of the image/coefficients 
     */
    public int getCurrentDepth() {return currentDepth;}

    /*------------------------------------------------------------------*/
    /** 
     * Get height.
     * 
     * @return the full-size image height. 
     */
    public int getHeight () {return(height);}

    /*------------------------------------------------------------------*/
    /** 
     * Get image.
     * 
     * @return the full-size image. 
     */
    public double[] getImage () {return image;}

    /*------------------------------------------------------------------*/
    /**
     * Get the pixel value from the image pyramid.
     *
     * @param x x-coordinate of the pixel
     * @param y y-coordinate of the pixel
     * @return pixel value
     */
    public double getPixelValFromPyramid(
       int x,   // Pixel location
       int y) 
    {
       return currentImage[y*currentWidth+x];
    }

    /*------------------------------------------------------------------*/
    /** 
     * Get pyramid depth.
     *
     * @return the depth of the image pyramid. A depth 1 means
     *         that one coarse resolution level is present in the stack. 
     *         The full-size level is not placed on the stack 
     */
    public int getPyramidDepth () {return(pyramidDepth);}

    /*------------------------------------------------------------------*/
    /** 
     * Get smallest height.
     *
     * @return the height of the smallest image in the pyramid 
     */
    public int getSmallestHeight () {return(smallestHeight);}

    /*------------------------------------------------------------------*/
    /** 
     * Get smallest width.
     *
     * @return the width of the smallest image in the pyramid 
     */
    public int getSmallestWidth () {return(smallestWidth);}

    /*------------------------------------------------------------------*/
    /**
     * Get thread.
     *
     * @return the thread associated
     */
    public Thread getThread () {return(t);}

    /*------------------------------------------------------------------*/
    /**
     * Get width.
     *
     * @return the full-size image width
     */
    public int getWidth () {return(width);}

    /*------------------------------------------------------------------*/
    /** 
     * Get weigth dx.
     *
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     *         image interpolation 
     */
    public double getWeightDx(int l, int m) {return yWeight[l]*dxWeight[m];}

    /*------------------------------------------------------------------*/
    /**
     * Get weidth dxdx.
     *
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     *         image interpolation
     */
    public double getWeightDxDx(int l, int m) {return yWeight[l]*d2xWeight[m];}

    /*------------------------------------------------------------------*/
    /**
     * Get weight dxdy.
     *
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     * image interpolation 
     */
    public double getWeightDxDy(int l, int m) {return dyWeight[l]*dxWeight[m];}

    /*------------------------------------------------------------------*/
    /**
     * Get weight dy.
     * 
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     *         image interpolation 
     */
    public double getWeightDy(int l, int m) {return dyWeight[l]*xWeight[m];}

    /*------------------------------------------------------------------*/
    /**
     * Get weight dydy.
     * 
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     *         image interpolation 
     */
    public double getWeightDyDy(int l, int m) {return d2yWeight[l]*xWeight[m];}

    /*------------------------------------------------------------------*/
    /**
     * Get image coeffecient weight.
     * 
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     *         image interpolation 
     */
    public double getWeightI(int l, int m) {return yWeight[l]*xWeight[m];}

    /*------------------------------------------------------------------*/
    /**
     * There are two types of interpolation routines. Those that use
     * precomputed weights and those that don't.
     * An example of use of the ones without precomputation is the
     * following:
     *    // Set of B-spline coefficients
     *    double [][]c;
     *
     *    // Set these coefficients to an interpolator
     *    bUnwarpJImageModel sw = new bUnwarpJImageModel(c);
     *
     *    // Compute the transformation mapping
     *    for (int v=0; v<ImageHeight; v++) {
     *       final double tv = (double)(v * intervals) / (double)(ImageHeight - 1) + 1.0F;
     *       for (int u = 0; u<ImageeWidth; u++) {
     *          final double tu = (double)(u * intervals) / (double)(ImageWidth - 1) + 1.0F;
     *          sw.prepareForInterpolation(tu, tv, ORIGINAL);
     *          interpolated_val[v][u] = sw.interpolateI();
     *       }
     */
    /*------------------------------------------------------------------*/
    /*------------------------------------------------------------------*/
    /**
     * Interpolate the X and Y derivatives of the image at a
     * given point. 
     *
     * @param D output, interpolation the X and Y derivatives of the image
     */
    public void interpolateD(double []D) 
    {
       // Only SplineDegree=3 is implemented
       D[0]=D[1]=0.0F;
       for (int j = 0; j<4; j++) {
           double sx=0.0F, sy=0.0F;
           int iy=yIndex[j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=xIndex[i];
                 if (ix!=-1) {
                    double c;
                    if (fromCurrent) c=currentCoefficient[p + ix];
                    else             c=coefficient[p + ix];
                    sx += dxWeight[i]*c;
                    sy +=  xWeight[i]*c;
                 }
              }
              D[0]+= yWeight[j] * sx;
              D[1]+=dyWeight[j] * sy;
           }
       }
    } /* end Interpolate D */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the XY, XX and YY derivatives of the image at a
     * given point.
     *
     * @param D2 output, interpolation of the XY, XX and YY derivatives of the image
     */
    public void interpolateD2 (double []D2) 
    {
       // Only SplineDegree=3 is implemented
       D2[0]=D2[1]=D2[2]=0.0F;
       for (int j = 0; j<4; j++) {
           double sxy=0.0F, sxx=0.0F, syy=0.0F;
           int iy=yIndex[j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=xIndex[i];
                 if (ix!=-1) {
                    double c;
                    if (fromCurrent) c=currentCoefficient[p + ix];
                    else             c=coefficient[p + ix];
                     sxy +=  dxWeight[i]*c;
                     sxx += d2xWeight[i]*c;
                     syy +=   xWeight[i]*c;
                 }
              }
              D2[0]+= dyWeight[j] * sxy;
              D2[1]+=  yWeight[j] * sxx;
              D2[2]+=d2yWeight[j] * syy;
           }
       }
    } /* end Interpolate dxdy, dxdx and dydy */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the X derivative of the image at a given point. 
     *
     * @return dx interpolation
     */
    public double interpolateDx () {
       // Only SplineDegree=3 is implemented
       double ival=0.0F;
       for (int j = 0; j<4; j++) {
           double s=0.0F;
           int iy=yIndex[j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=xIndex[i];
                 if (ix!=-1)
                    if (fromCurrent) s += dxWeight[i]*currentCoefficient[p + ix];
                    else             s += dxWeight[i]*coefficient[p + ix];
              }
              ival+=yWeight[j] * s;
           }
       }
       return ival;
    } /* end Interpolate Dx */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the X derivative of the image at a given point.
     *
     * @return dxdx interpolation
     */
    public double interpolateDxDx () 
    {
       // Only SplineDegree=3 is implemented
       double ival=0.0F;
       for (int j = 0; j<4; j++) {
           double s=0.0F;
           int iy=yIndex[j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=xIndex[i];
                 if (ix!=-1)
                    if (fromCurrent) s += d2xWeight[i]*currentCoefficient[p + ix];
                    else             s += d2xWeight[i]*coefficient[p + ix];
              }
              ival+=yWeight[j] * s;
           }
       }
       return ival;
    } /* end Interpolate DxDx */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the X derivative of the image at a given point. 
     *
     * @return dxdy interpolation
     */
    public double interpolateDxDy () {
       // Only SplineDegree=3 is implemented
       double ival=0.0F;
       for (int j = 0; j<4; j++) {
           double s=0.0F;
           int iy=yIndex[j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=xIndex[i];
                 if (ix!=-1)
                    if (fromCurrent) s += dxWeight[i]*currentCoefficient[p + ix];
                    else             s += dxWeight[i]*coefficient[p + ix];
              }
              ival+=dyWeight[j] * s;
           }
       }
       return ival;
    } /* end Interpolate DxDy */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the Y derivative of the image at a given point. 
     *
     * @return dy interpolation
     */
    public double interpolateDy () 
    {
       // Only SplineDegree=3 is implemented
       double ival=0.0F;
       for (int j = 0; j<4; j++) {
           double s=0.0F;
           int iy=yIndex[j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=xIndex[i];
                 if (ix!=-1)
                    if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
                    else             s += xWeight[i]*coefficient[p + ix];
              }
              ival+=dyWeight[j] * s;
           }
       }
       return ival;
    } /* end Interpolate Dy */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the X derivative of the image at a given point. 
     *
     * @return dydy interpolation
     */
    public double interpolateDyDy() 
    {
       // Only SplineDegree=3 is implemented
       double ival=0.0F;
       for (int j = 0; j<4; j++) {
           double s=0.0F;
           int iy=yIndex[j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=xIndex[i];
                 if (ix!=-1)
                    if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
                    else             s += xWeight[i]*coefficient[p + ix];
              }
              ival+=d2yWeight[j] * s;
           }
       }
       return ival;
    } /* end Interpolate DyDy */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the image at a given point. 
     *
     * @return image interpolation
     */
    public double interpolateI () 
    {
       // Only SplineDegree=3 is implemented
       double ival=0.0F;
       for (int j = 0; j<4; j++) {
           double s=0.0F;
           int iy=yIndex[j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=xIndex[i];
                 if (ix!=-1) 
                    if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
                    else             s += xWeight[i]*coefficient[p + ix];
              }
              ival+=yWeight[j] * s;
           }
       }
       return ival;
    } /* end Interpolate Image */

    /*------------------------------------------------------------------*/
    /**
     * Check if the coefficients pyramid is empty.
     *
     * @return true when the coefficients pyramid is empty
     *         false if not
     */
    public boolean isFinest() {return cpyramid.isEmpty();}

    /*------------------------------------------------------------------*/
    /**
     * Pop one element from the coefficients and image pyramids.
     */
    public void popFromPyramid()
    {
       // Pop coefficients
       if (cpyramid.isEmpty()) {
          currentWidth       = width;
          currentHeight      = height;
          currentCoefficient = coefficient;
       } else {
          currentWidth       = ((Integer)cpyramid.pop()).intValue();
          currentHeight      = ((Integer)cpyramid.pop()).intValue();
          currentCoefficient = (double [])cpyramid.pop();
       }
       twiceCurrentWidth     = 2*currentWidth;
       twiceCurrentHeight    = 2*currentHeight;
       if (currentDepth>0) currentDepth--;

       // Pop image
       if (isTarget && !imgpyramid.isEmpty()) {
          if (currentWidth != ((Integer)imgpyramid.pop()).intValue())
             System.out.println("I cannot understand");
          if (currentHeight != ((Integer)imgpyramid.pop()).intValue())
             System.out.println("I cannot understand");
          currentImage = (double [])imgpyramid.pop();
       } else currentImage = image;
    }

    /*------------------------------------------------------------------*/
    /**
     * fromCurrent=true  --> The interpolation is prepared to be done
     *                       from the current image in the pyramid.
     * fromCurrent=false --> The interpolation is prepared to be done
     *                       from the original image. 
     *
     * @param x x- point coordinate
     * @param y y- point coordinate
     * @param fromCurrent flag to determine the image to do the interpolation from
     */
    public void prepareForInterpolation(
       double x, 
       double y,
       boolean fromCurrent) 
    {
        // Remind this point for interpolation
        this.x = x;
        this.y = y;
        this.fromCurrent = fromCurrent;

        if (fromCurrent) 
        {
            widthToUse = currentWidth; 
            heightToUse = currentHeight;
        }
        else             
        { 
            widthToUse = width;        
            heightToUse = height;
        }
               
        int ix=(int)x;
        int iy=(int)y;

        int twiceWidthToUse =2*widthToUse;
        int twiceHeightToUse=2*heightToUse;

        // Set X indexes
        // p is the index of the rightmost influencing spline
        int p = (0.0 <= x) ? (ix + 2) : (ix + 1);
        for (int k = 0; k<4; p--, k++) {
           if (coefficientsAreMirrored) {
             int q = (p < 0) ? (-1 - p) : (p);
             if (twiceWidthToUse <= q) q -= twiceWidthToUse * (q / twiceWidthToUse);
             xIndex[k] = (widthToUse <= q) ? (twiceWidthToUse - 1 - q) : (q);
          } else
              xIndex[k] = (p<0 || p>=widthToUse) ? (-1):(p);
        }

        // Set Y indexes
        p = (0.0 <= y) ? (iy + 2) : (iy + 1);
        for (int k = 0; k<4; p--, k++) {
           if (coefficientsAreMirrored) {
               int q = (p < 0) ? (-1 - p) : (p);
               if (twiceHeightToUse <= q) q -= twiceHeightToUse * (q / twiceHeightToUse);
               yIndex[k] = (heightToUse <= q) ? (twiceHeightToUse - 1 - q) : (q);
          } else
               yIndex[k] = (p<0 || p>=heightToUse) ? (-1):(p);
        }

        // Compute how much the sample depart from an integer position
        double ex = x - ((0.0 <= x) ? (ix) : (ix - 1));
        double ey = y - ((0.0 <= y) ? (iy) : (iy - 1));

        // Set X weights for the image and derivative interpolation
        double s = 1.0F - ex;
        dxWeight[0] = 0.5F * ex * ex;
        xWeight[0]  = ex * dxWeight[0] / 3.0F; // Bspline03(x-ix-2)
        dxWeight[3] = -0.5F * s * s;
        xWeight[3]  = s * dxWeight[3] / -3.0F; // Bspline03(x-ix+1)
        dxWeight[1] = 1.0F - 2.0F * dxWeight[0] + dxWeight[3];
        //xWeight[1]  = 2.0F / 3.0F + (1.0F + ex) * dxWeight[3]; // Bspline03(x-ix-1);
        xWeight[1]  = bUnwarpJMathTools.Bspline03(x-ix-1);
        dxWeight[2] = 1.5F * ex * (ex - 4.0F/ 3.0F);
        xWeight[2]  = 2.0F / 3.0F - (2.0F - ex) * dxWeight[0]; // Bspline03(x-ix)

        d2xWeight[0] = ex;
        d2xWeight[1] = s-2*ex;
        d2xWeight[2] = ex-2*s;
        d2xWeight[3] = s;

       // Set Y weights for the image and derivative interpolation
       double t = 1.0F - ey;
       dyWeight[0] = 0.5F * ey * ey;
       yWeight[0]  = ey * dyWeight[0] / 3.0F;
       dyWeight[3] = -0.5F * t * t;
       yWeight[3]  = t * dyWeight[3] / -3.0F;
       dyWeight[1] = 1.0F - 2.0F * dyWeight[0] + dyWeight[3];
       yWeight[1]  = 2.0F / 3.0F + (1.0F + ey) * dyWeight[3];
       dyWeight[2] = 1.5F * ey * (ey - 4.0F/ 3.0F);
       yWeight[2]  = 2.0F / 3.0F - (2.0F - ey) * dyWeight[0];

       d2yWeight[0] = ey;
       d2yWeight[1] = t-2*ey;
       d2yWeight[2] = ey-2*t;
       d2yWeight[3] = t;
    } /* prepareForInterpolation */

    /*------------------------------------------------------------------*/
    /**
     * Get width of precomputed vectors.
     * 
     * @return the width of the precomputed vectors 
     */
    public int precomputed_getWidth() {return prec_yWeight.length;}

    /*------------------------------------------------------------------*/
    /** 
     * Get the weight of the coefficient dx.
     *
     * @param l
     * @param m
     * @param u
     * @param v
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     * image interpolation 
     */
    public double precomputed_getWeightDx(int l, int m, int u, int v)
       {return prec_yWeight[v][l]*prec_dxWeight[u][m];}

    /*------------------------------------------------------------------*/
    /**
     * @param l
     * @param m
     * @param u
     * @param v
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     * image interpolation 
     */
    public double precomputed_getWeightDxDx(int l, int m, int u, int v)
       {return prec_yWeight[v][l]*prec_d2xWeight[u][m];}

    /*------------------------------------------------------------------*/
    /**
     * @param l
     * @param m
     * @param u
     * @param v 
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     * image interpolation 
     */
    public double precomputed_getWeightDxDy(int l, int m, int u, int v)
       {return prec_dyWeight[v][l]*prec_dxWeight[u][m];}

    /*------------------------------------------------------------------*/
    /** 
     * @param l
     * @param m
     * @param u
     * @param v
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     * image interpolation 
     */
    public double precomputed_getWeightDy(int l, int m, int u, int v)
       {return prec_dyWeight[v][l]*prec_xWeight[u][m];}

    /*------------------------------------------------------------------*/
    /**
     * @param l
     * @param m
     * @param u
     * @param v
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     * image interpolation 
     */
    public double precomputed_getWeightDyDy(int l, int m, int u, int v)
       {return prec_d2yWeight[v][l]*prec_xWeight[u][m];}

    /*------------------------------------------------------------------*/
    /**
     * @param l
     * @param m
     * @param u
     * @param v
     * @return the weight of the coefficient l,m (yIndex, xIndex) in the
     * image interpolation 
     */
    public double precomputed_getWeightI(int l, int m, int u, int v)
       {return prec_yWeight[v][l]*prec_xWeight[u][m];}

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the X and Y derivatives of the image at a
     * given point. 
     *
     * @param D output, X and Y derivatives of the image
     * @param u x- point coordinate 
     * @param v y- point coordinate 
     */
    public void precomputed_interpolateD(double []D, int u, int v) 
    {
       // Only SplineDegree=3 is implemented
       D[0]=D[1]=0.0F;
       for (int j = 0; j<4; j++) {
           double sx=0.0F, sy=0.0F;
           int iy=prec_yIndex[v][j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=prec_xIndex[u][i];
                 if (ix!=-1) {
                    double c;
                    if (fromCurrent) c=currentCoefficient[p + ix];
                    else             c=coefficient[p + ix];
                    sx += prec_dxWeight[u][i]*c;
                    sy +=  prec_xWeight[u][i]*c;
                 }
              }
              D[0]+= prec_yWeight[v][j] * sx;
              D[1]+=prec_dyWeight[v][j] * sy;
           }
       }
    } /* end Interpolate D */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the XY, XX and YY derivatives of the image at a
     * given point. 
     *
     * @param D2 output, XY, XX and YY derivatives of the image
     * @param u x- point coordinate 
     * @param v y- point coordinate 
     */
    public void precomputed_interpolateD2 (double []D2, int u, int v) {
       // Only SplineDegree=3 is implemented
       D2[0]=D2[1]=D2[2]=0.0F;
       for (int j = 0; j<4; j++) {
           double sxy=0.0F, sxx=0.0F, syy=0.0F;
           int iy=prec_yIndex[v][j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=prec_xIndex[u][i];
                 if (ix!=-1) {
                    double c;
                    if (fromCurrent) c=currentCoefficient[p + ix];
                    else             c=coefficient[p + ix];
                     sxy +=  prec_dxWeight[u][i]*c;
                     sxx += prec_d2xWeight[u][i]*c;
                     syy +=   prec_xWeight[u][i]*c;
                 }
              }
              D2[0]+= prec_dyWeight[v][j] * sxy;
              D2[1]+=  prec_yWeight[v][j] * sxx;
              D2[2]+=prec_d2yWeight[v][j] * syy;
           }
       }
    } /* end Interpolate dxdy, dxdx and dydy */

    /*------------------------------------------------------------------*/
    /**
     * Interpolate the image at a given point. 
     *
     * @param u x- point coordinate 
     * @param v y- point coordinate 
     */
    public double precomputed_interpolateI (int u, int v) 
    {
       // Only SplineDegree=3 is implemented
       double ival=0.0F;
       for (int j = 0; j<4; j++) {
           double s=0.0F;
           int iy=prec_yIndex[v][j];
           if (iy!=-1) {
              int p=iy*widthToUse;
              for (int i=0; i<4; i++) {
                 int ix=prec_xIndex[u][i];
                 if (ix!=-1) 
                    if (fromCurrent) s += prec_xWeight[u][i]*currentCoefficient[p + ix];
                    else             s += prec_xWeight[u][i]*coefficient[p + ix];
              }
              ival+=prec_yWeight[v][j] * s;
           }
       }
       return ival;
    } /* end Interpolate Image */

    /*------------------------------------------------------------------*/
    /**
     * Prepare precomputations for a given image size. 
     *
     * @param Ydim y- image dimension
     * @param Xdim x- image dimension
     * @param intervals intervals in the deformation
     */ 
    public void precomputed_prepareForInterpolation(int Ydim, int Xdim, int intervals) 
    {
       // Ask for memory
       prec_xIndex   =new int   [Xdim][4];
       prec_yIndex   =new int   [Ydim][4];
       prec_xWeight  =new double[Xdim][4];
       prec_yWeight  =new double[Ydim][4];
       prec_dxWeight =new double[Xdim][4];
       prec_dyWeight =new double[Ydim][4];
       prec_d2xWeight=new double[Xdim][4];
       prec_d2yWeight=new double[Ydim][4];

        boolean ORIGINAL = false;
        // Fill the precomputed weights and indexes for the Y axis
        for (int v=0; v<Ydim; v++) {
            // Express the current point in Spline units
           final double tv = (double)(v * intervals) / (double)(Ydim - 1) + 1.0F;
           final double tu = 1.0F;

            // Compute all weights and indexes
            prepareForInterpolation(tu, tv, ORIGINAL);

            // Copy all values
            for (int k=0; k<4; k++) {
             prec_yIndex   [v][k]=  yIndex [k];
             prec_yWeight  [v][k]=  yWeight[k];
             prec_dyWeight [v][k]= dyWeight[k];
             prec_d2yWeight[v][k]=d2yWeight[k];
           }
        }

        // Fill the precomputed weights and indexes for the X axis
        for (int u=0; u<Xdim; u++) 
        {
            // Express the current point in Spline units
            final double tv = 1.0F;
            final double tu = (double)(u * intervals) / (double)(Xdim - 1) + 1.0F;

            // Compute all weights and indexes
            prepareForInterpolation(tu,tv,ORIGINAL);

            // Copy all values
            for (int k=0; k<4; k++) 
            {
             prec_xIndex   [u][k]=  xIndex [k];
             prec_xWeight  [u][k]=  xWeight[k];
             prec_dxWeight [u][k]= dxWeight[k];
             prec_d2xWeight[u][k]=d2xWeight[k];
           }
        }
    }

    /*------------------------------------------------------------------*/
    /**
     * Start the image precomputations. The computation of the B-spline
     * coefficients of the full-size image is not interruptible; all other
     * methods are.
     */
    public void run () 
    {
        coefficient = getBasicFromCardinal2D();
        buildCoefficientPyramid();
        if (isTarget) buildImagePyramid();
    } /* end run */

    /*------------------------------------------------------------------*/
    /**
     * Set spline coefficients.
     *
     * @param c Set of B-spline coefficients 
     * @param Ydim Y-dimension of the set of coefficients
     * @param Xdim X-dimension of the set of coefficients
     * @param offset Offset of the beginning of the array with respect to the origin of c
     */
    public void setCoefficients (
       final double []c,     
       final int Ydim,       
       final int Xdim,
       final int offset) 
    {
        // Copy the array of coefficients
        System.arraycopy(c, offset, coefficient, 0, Ydim*Xdim);
    }

    /*------------------------------------------------------------------*/
    /**
     * Sets the depth up to which the pyramids should be computed.
     *
     * @param pyramidDepth pyramid depth to be set
     */
    public void setPyramidDepth (final int pyramidDepth) 
    {
       int proposedPyramidDepth=pyramidDepth;

       // Check what is the maximum depth allowed by the image
       int currentWidth=width;
       int currentHeight=height;
       int scale=0;
       while (currentWidth>=min_image_size && currentHeight>=min_image_size) {
          currentWidth/=2;
          currentHeight/=2;
          scale++;
       }
       scale--;

       if (proposedPyramidDepth>scale) proposedPyramidDepth=scale;

       this.pyramidDepth = proposedPyramidDepth;
    } /* end setPyramidDepth */

    /*------------------------------------------------------------------*/
    /**
     * Converts the pixel array of the incoming ImageProcessor
     * object into a local double array. 
     * 
     * @param ip image in pixel array
     * @param isTarget enables the computation of the derivative or not
     */
    public bUnwarpJImageModel (
       final ImageProcessor ip,
       final boolean isTarget) 
    {
       // Initialize thread
       t = new Thread(this);
       t.setDaemon(true);

       // Get image information
       this.isTarget = isTarget;
       width         = ip.getWidth();
       height        = ip.getHeight();
       twiceWidth    = 2*width;
       twiceHeight   = 2*height;
       coefficientsAreMirrored = true;

       // Copy the pixel array
       int k = 0;
       image = new double[width * height];
       bUnwarpJMiscTools.extractImage(ip, image);

       // Resize the speedup arrays
       xIndex    = new int[4];
       yIndex    = new int[4];
       xWeight   = new double[4];
       yWeight   = new double[4];
       dxWeight  = new double[4];
       dyWeight  = new double[4];
       d2xWeight = new double[4];
       d2yWeight = new double[4];
    } /* end bUnwarpJImage */

    /**
     * The same as before, but take the image from an array.
     *
     * @param img image in a double array
     * @param isTarget enables the computation of the derivative or not
     */
    public bUnwarpJImageModel (
       final double [][]img,
       final boolean isTarget) 
    {
        // Initialize thread
       t = new Thread(this);
       t.setDaemon(true);

        // Get image information
       this.isTarget = isTarget;
       width         = img[0].length;
       height        = img.length;
       twiceWidth    = 2*width;
       twiceHeight   = 2*height;
       coefficientsAreMirrored = true;

       // Copy the pixel array
       int k = 0;
       image = new double[width * height];
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++, k++)
             image[k] = img[y][x];

       // Resize the speedup arrays
       xIndex    = new int[4];
       yIndex    = new int[4];
       xWeight   = new double[4];
       yWeight   = new double[4];
       dxWeight  = new double[4];
       dyWeight  = new double[4];
       d2xWeight = new double[4];
       d2yWeight = new double[4];
    } /* end bUnwarpJImage */

    /*------------------------------------------------------------------*/
    /**
     * Initialize the model from a set of coefficients.
     *
     * @param c Set of B-spline coefficients
     */
    public bUnwarpJImageModel (final double [][]c) 
    {
        // Get the size of the input array
        currentHeight      = height      = c.length;
        currentWidth       = width       = c[0].length;
        twiceCurrentHeight = twiceHeight = 2*height;
        twiceCurrentWidth  = twiceWidth  = 2*width;
        coefficientsAreMirrored = false;

        // Copy the array of coefficients
        coefficient=new double[height*width];
        int k=0;
       for (int y=0; y<height; y++, k+= width)
          System.arraycopy(c[y], 0, coefficient, k, width);

       // Resize the speedup arrays
       xIndex    = new int[4];
       yIndex    = new int[4];
       xWeight   = new double[4];
       yWeight   = new double[4];
       dxWeight  = new double[4];
       dyWeight  = new double[4];
       d2xWeight = new double[4];
       d2yWeight = new double[4];
    }

    /*------------------------------------------------------------------*/
    /**
     * Initialize the model from a set of coefficients.
     * The same as the previous function but now the coefficients
     * are in a single row. 
     *
     * @param c Set of B-spline coefficients 
     * @param Ydim Y-dimension of the set of coefficients
     * @param Xdim X-dimension of the set of coefficients
     * @param offset Offset of the beginning of the array with respect to the origin of c
     */
    public bUnwarpJImageModel (
       final double []c,     
       final int Ydim,       
       final int Xdim,
       final int offset) 
    {
        // Get the size of the input array
        currentHeight      = height      = Ydim;
        currentWidth       = width       = Xdim;
        twiceCurrentHeight = twiceHeight = 2*height;
        twiceCurrentWidth  = twiceWidth  = 2*width;
        coefficientsAreMirrored = false;

        // Copy the array of coefficients
        coefficient=new double[height*width];
        System.arraycopy(c, offset, coefficient, 0, height*width);

       // Resize the speedup arrays
       xIndex    = new int[4];
       yIndex    = new int[4];
       xWeight   = new double[4];
       yWeight   = new double[4];
       dxWeight  = new double[4];
       dyWeight  = new double[4];
       d2xWeight = new double[4];
       d2yWeight = new double[4];
    }
    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     *
     * @param h
     * @param c
     * @param s
     */
    private void antiSymmetricFirMirrorOffBounds1D (
       final double[] h,
       final double[] c,
       final double[] s) 
    {
       if (2 <= c.length) {
          s[0] = h[1] * (c[1] - c[0]);
          for (int i = 1; (i < (s.length - 1)); i++) {
             s[i] = h[1] * (c[i + 1] - c[i - 1]);
          }
          s[s.length - 1] = h[1] * (c[c.length - 1] - c[c.length - 2]);
       } else s[0] = 0.0;
    } /* end antiSymmetricFirMirrorOffBounds1D */

    /*------------------------------------------------------------------*/
    /**
     * Passes from basic to cardinal.
     *
     * @param basic
     * @param cardinal
     * @param width
     * @param height
     * @param degree
     */
    private void basicToCardinal2D (
       final double[] basic,
       final double[] cardinal,
       final int width,
       final int height,
       final int degree) 
    {
       final double[] hLine = new double[width];
       final double[] vLine = new double[height];
       final double[] hData = new double[width];
       final double[] vData = new double[height];
       double[] h = null;
       switch (degree) {
          case 3:
             h = new double[2];
             h[0] = 2.0 / 3.0;
             h[1] = 1.0 / 6.0;
             break;
          case 7:
             h = new double[4];
             h[0] = 151.0 / 315.0;
             h[1] = 397.0 / 1680.0;
             h[2] = 1.0 / 42.0;
             h[3] = 1.0 / 5040.0;
             break;
          default:
             h = new double[1];
             h[0] = 1.0;
       }
       for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
          extractRow(basic, y, hLine);
          symmetricFirMirrorOffBounds1D(h, hLine, hData);
          putRow(cardinal, y, hData);
       }
       for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
          extractColumn(cardinal, width, x, vLine);
          symmetricFirMirrorOffBounds1D(h, vLine, vData);
          putColumn(cardinal, width, x, vData);
       }
    } /* end basicToCardinal2D */

    /*------------------------------------------------------------------*/
    /**
     * Build the coefficients pyramid.
     */
    private void buildCoefficientPyramid () 
    {
       int fullWidth;
       int fullHeight;
       double[] fullDual = new double[width * height];
       int halfWidth = width;
       int halfHeight = height;
       basicToCardinal2D(coefficient, fullDual, width, height, 7);
       for (int depth = 1; ((depth <= pyramidDepth) && (!t.isInterrupted())); depth++) {
          fullWidth = halfWidth;
          fullHeight = halfHeight;
          halfWidth /= 2;
          halfHeight /= 2;
          final double[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
          final double[] halfCoefficient = getBasicFromCardinal2D(halfDual, halfWidth, halfHeight, 7);
          cpyramid.push(halfCoefficient);
          cpyramid.push(new Integer(halfHeight));
          cpyramid.push(new Integer(halfWidth));
          fullDual = halfDual;
       }
        smallestWidth  = halfWidth;
        smallestHeight = halfHeight;
        currentDepth=pyramidDepth+1;
    } /* end buildCoefficientPyramid */

    /*------------------------------------------------------------------*/
    /**
     * Build the image pyramid.
     */
    private void buildImagePyramid () 
    {
       int fullWidth;
       int fullHeight;
       double[] fullDual = new double[width * height];
       int halfWidth = width;
       int halfHeight = height;
       cardinalToDual2D(image, fullDual, width, height, 3);
       for (int depth = 1; ((depth <= pyramidDepth) && (!t.isInterrupted())); depth++) {
          fullWidth = halfWidth;
          fullHeight = halfHeight;
          halfWidth /= 2;
          halfHeight /= 2;
          final double[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
          final double[] halfImage = new double[halfWidth * halfHeight];
          dualToCardinal2D(halfDual, halfImage, halfWidth, halfHeight, 3);
          imgpyramid.push(halfImage);
          imgpyramid.push(new Integer(halfHeight));
          imgpyramid.push(new Integer(halfWidth));
          fullDual = halfDual;
       }
    } /* end buildImagePyramid */

    /*------------------------------------------------------------------*/
    /**
     * Passes from cardinal to dual (2D).
     *
     * @param cardinal
     * @param dual
     * @param width
     * @param height
     * @param degree
     */
    private void cardinalToDual2D (
       final double[] cardinal,
       final double[] dual,
       final int width,
       final int height,
       final int degree) 
    {
       basicToCardinal2D(getBasicFromCardinal2D(cardinal, width, height, degree),
          dual, width, height, 2 * degree + 1);
    } /* end cardinalToDual2D */

    /*------------------------------------------------------------------*/
    /**
     * Pass coeffients to gradient (1D).
     *
     * @param c coefficients
     */
    private void coefficientToGradient1D (final double[] c) 
    {
       final double[] h = {0.0, 1.0 / 2.0};
       final double[] s = new double[c.length];
       antiSymmetricFirMirrorOffBounds1D(h, c, s);
       System.arraycopy(s, 0, c, 0, s.length);
    } /* end coefficientToGradient1D */

    /*------------------------------------------------------------------*/
    /**
     * Pass coeffiecients to samples.
     * 
     * @param c coefficients
     */
    private void coefficientToSamples1D (final double[] c) 
    {
       final double[] h = {2.0 / 3.0, 1.0 / 6.0};
       final double[] s = new double[c.length];
       symmetricFirMirrorOffBounds1D(h, c, s);
       System.arraycopy(s, 0, c, 0, s.length);
    } /* end coefficientToSamples1D */

    /*------------------------------------------------------------------*/
    /**
     * 
     * @param basic
     * @param xGradient
     * @param yGradient
     * @param width
     * @param height
     */
    private void coefficientToXYGradient2D (
       final double[] basic,
       final double[] xGradient,
       final double[] yGradient,
       final int width,
       final int height) 
    {
       final double[] hLine = new double[width];
       final double[] hData = new double[width];
       final double[] vLine = new double[height];
       for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
          extractRow(basic, y, hLine);
          System.arraycopy(hLine, 0, hData, 0, width);
          coefficientToGradient1D(hLine);
          coefficientToSamples1D(hData);
          putRow(xGradient, y, hLine);
          putRow(yGradient, y, hData);
       }
       for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
          extractColumn(xGradient, width, x, vLine);
          coefficientToSamples1D(vLine);
          putColumn(xGradient, width, x, vLine);
          extractColumn(yGradient, width, x, vLine);
          coefficientToGradient1D(vLine);
          putColumn(yGradient, width, x, vLine);
       }
    } /* end coefficientToXYGradient2D */

    /*------------------------------------------------------------------*/
    /**
     * Pass from dual to cardinal (2D).
     *
     * @param dual
     * @param cardinal
     * @param width
     * @param height
     * @param degree
     */
    private void dualToCardinal2D (
       final double[] dual,
       final double[] cardinal,
       final int width,
       final int height,
       final int degree) 
    {
       basicToCardinal2D(getBasicFromCardinal2D(dual, width, height, 2 * degree + 1),
          cardinal, width, height, degree);
    } /* end dualToCardinal2D */

    /*------------------------------------------------------------------*/
    /**
     * Extract a column from the array.
     *
     * @param array
     * @param width of the position of the column in the array
     * @param x column position in the array
     * @param column output, extractred column
     */
    private void extractColumn (
       final double[] array,
       final int width,
       int x,
       final double[] column) 
    {
       for (int i = 0; (i < column.length); i++, x+=width)
          column[i] = (double)array[x];
    } /* end extractColumn */

    /*------------------------------------------------------------------*/
    /**
     * Extract a row from the array .
     *
     * @param array
     * @param y row position in the array
     * @param row output, extractred row
     */
    private void extractRow (
       final double[] array,
       int y,
       final double[] row) 
    {
       y *= row.length;
       for (int i = 0; (i < row.length); i++)
          row[i] = (double)array[y++];
    } /* end extractRow */

    /*------------------------------------------------------------------*/
    /**
     * Get basic from cardinal (2D).
     */
    private double[] getBasicFromCardinal2D () 
    {
       final double[] basic = new double[width * height];
       final double[] hLine = new double[width];
       final double[] vLine = new double[height];
       for (int y = 0; (y < height); y++) {
          extractRow(image, y, hLine);
          samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
          putRow(basic, y, hLine);
       }
       for (int x = 0; (x < width); x++) {
          extractColumn(basic, width, x, vLine);
          samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
          putColumn(basic, width, x, vLine);
       }
       return(basic);
    } /* end getBasicFromCardinal2D */

    /*------------------------------------------------------------------*/
    /**
     * Get basic from cardinal (2D).
     *
     * @param cardinal
     * @param width
     * @param height
     * @param degree
     */
    private double[] getBasicFromCardinal2D (
       final double[] cardinal,
       final int width,
       final int height,
       final int degree) 
    {
       final double[] basic = new double[width * height];
       final double[] hLine = new double[width];
       final double[] vLine = new double[height];
       for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
          extractRow(cardinal, y, hLine);
          samplesToInterpolationCoefficient1D(hLine, degree, 0.0);
          putRow(basic, y, hLine);
       }
       for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
          extractColumn(basic, width, x, vLine);
          samplesToInterpolationCoefficient1D(vLine, degree, 0.0);
          putColumn(basic, width, x, vLine);
       }
       return(basic);
    } /* end getBasicFromCardinal2D */

    /*------------------------------------------------------------------*/
    /**
     * Get half dual (2D).
     * 
     * @param fullDual
     * @param fullWidth
     * @param fullHeight
     */
    private double[] getHalfDual2D (
       final double[] fullDual,
       final int fullWidth,
       final int fullHeight) 
    {
       final int halfWidth = fullWidth / 2;
       final int halfHeight = fullHeight / 2;
       final double[] hLine = new double[fullWidth];
       final double[] hData = new double[halfWidth];
       final double[] vLine = new double[fullHeight];
       final double[] vData = new double[halfHeight];
       final double[] demiDual = new double[halfWidth * fullHeight];
       final double[] halfDual = new double[halfWidth * halfHeight];
       for (int y = 0; ((y < fullHeight) && (!t.isInterrupted())); y++) {
          extractRow(fullDual, y, hLine);
          reduceDual1D(hLine, hData);
          putRow(demiDual, y, hData);
       }
       for (int x = 0; ((x < halfWidth) && (!t.isInterrupted())); x++) {
          extractColumn(demiDual, halfWidth, x, vLine);
          reduceDual1D(vLine, vData);
          putColumn(halfDual, halfWidth, x, vData);
       }
       return(halfDual);
    } /* end getHalfDual2D */

    /*------------------------------------------------------------------*/
    /**
     * Get initial anti-causal coefficients mirror of bounds.
     *
     * @param c coefficients
     * @param z
     * @param tolerance
     */
    private double getInitialAntiCausalCoefficientMirrorOffBounds (
       final double[] c,
       final double z,
       final double tolerance) 
    {
       return(z * c[c.length - 1] / (z - 1.0));
    } /* end getInitialAntiCausalCoefficientMirrorOffBounds */

    /*------------------------------------------------------------------*/
    /**
     * Get initial causal coefficients mirror of bounds.
     *
     * @param c coefficients
     * @param z
     * @param tolerance
     */
    private double getInitialCausalCoefficientMirrorOffBounds (
       final double[] c,
       final double z,
       final double tolerance) 
    {
       double z1 = z;
       double zn = Math.pow(z, c.length);
       double sum = (1.0 + z) * (c[0] + zn * c[c.length - 1]);
       int horizon = c.length;
       if (0.0 < tolerance) {
          horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
          horizon = (horizon < c.length) ? (horizon) : (c.length);
       }
       zn = zn * zn;
       for (int n = 1; (n < (horizon - 1)); n++) {
          z1 = z1 * z;
          zn = zn / z;
          sum = sum + (z1 + zn) * c[n];
       }
       return(sum / (1.0 - Math.pow(z, 2 * c.length)));
    } /* end getInitialCausalCoefficientMirrorOffBounds */

    /*------------------------------------------------------------------*/
    /**
     * Put a column in the array.
     *
     * @param array
     * @param width of the position of the column in the array
     * @param x column position in the array
     * @param column column to be put
     */
    private void putColumn (
       final double[] array,
       final int width,
       int x,
       final double[] column) 
    {
       for (int i = 0; (i < column.length); i++, x+=width)
          array[x] = (double)column[i];
    } /* end putColumn */

    /*------------------------------------------------------------------*/
    /**
     * Put a row in the array.
     *
     * @param array
     * @param y row position in the array
     * @param row row to be put
     */
    private void putRow (
       final double[] array,
       int y,
       final double[] row) 
    {
       y *= row.length;
       for (int i = 0; (i < row.length); i++)
          array[y++] = (double)row[i];
    } /* end putRow */

    /*------------------------------------------------------------------*/
    /**
     * Reduce dual (1D).
     *
     * @param c
     * @param s
     */ 
    private void reduceDual1D (
       final double[] c,
       final double[] s)
    {
       final double h[] = {6.0 / 16.0, 4.0 / 16.0, 1.0 / 16.0};
       if (2 <= s.length) {
          s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
          for (int i = 2, j = 1; (j < (s.length - 1)); i += 2, j++) {
             s[j] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
                + h[2] * (c[i - 2] + c[i + 2]);
          }
          if (c.length == (2 * s.length)) {
             s[s.length - 1] = h[0] * c[c.length - 2] + h[1] * (c[c.length - 3] + c[c.length - 1])
                + h[2] * (c[c.length - 4] + c[c.length - 1]);
          }
          else {
             s[s.length - 1] = h[0] * c[c.length - 3] + h[1] * (c[c.length - 4] + c[c.length - 2])
                + h[2] * (c[c.length - 5] + c[c.length - 1]);
          }
       }
       else {
          switch (c.length) {
             case 3:
                s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
                break;
             case 2:
                s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + 2.0 * h[2] * c[1];
                break;
             default:
          }
       }
    } /* end reduceDual1D */

    /*------------------------------------------------------------------*/
    /**
     * Samples to interpolation coefficient (1D).
     *
     * @param c coefficients
     * @param degree
     * @param tolerance
     */
    private void samplesToInterpolationCoefficient1D (
       final double[] c,
       final int degree,
       final double tolerance)
    {
       double[] z = new double[0];
       double lambda = 1.0;
       switch (degree) {
          case 3:
             z = new double[1];
             z[0] = Math.sqrt(3.0) - 2.0;
             break;
          case 7:
             z = new double[3];
             z[0] = -0.5352804307964381655424037816816460718339231523426924148812;
             z[1] = -0.122554615192326690515272264359357343605486549427295558490763;
             z[2] = -0.0091486948096082769285930216516478534156925639545994482648003;
             break;
          default:
       }
       if (c.length == 1) {
          return;
       }
       for (int k = 0; (k < z.length); k++) {
          lambda *= (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
       }
       for (int n = 0; (n < c.length); n++) {
          c[n] = c[n] * lambda;
       }
       for (int k = 0; (k < z.length); k++) {
          c[0] = getInitialCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
          for (int n = 1; (n < c.length); n++) {
             c[n] = c[n] + z[k] * c[n - 1];
          }
          c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
          for (int n = c.length - 2; (0 <= n); n--) {
             c[n] = z[k] * (c[n+1] - c[n]);
          }
       }
    } /* end samplesToInterpolationCoefficient1D */

    /*------------------------------------------------------------------*/
    /**
     * Symmetric FIR filter with mirror off bounds (1D) conditions.
     *
     * @param h
     * @param c
     * @param s
     */
    private void symmetricFirMirrorOffBounds1D (
       final double[] h,
       final double[] c,
       final double[] s)
    {
       switch (h.length) {
          case 2:
             if (2 <= c.length) {
                s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]);
                for (int i = 1; (i < (s.length - 1)); i++) {
                   s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1]);
                }
                s[s.length - 1] = h[0] * c[c.length - 1]
                   + h[1] * (c[c.length - 2] + c[c.length - 1]);
             }
             else {
                s[0] = (h[0] + 2.0 * h[1]) * c[0];
             }
             break;
          case 4:
             if (6 <= c.length) {
                s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
                   + h[3] * (c[2] + c[3]);
                s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
                   + h[3] * (c[1] + c[4]);
                s[2] = h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[4])
                   + h[3] * (c[0] + c[5]);
                for (int i = 3; (i < (s.length - 3)); i++) {
                   s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
                      + h[2] * (c[i - 2] + c[i + 2]) + h[3] * (c[i - 3] + c[i + 3]);
                }
                s[s.length - 3] = h[0] * c[c.length - 3]
                   + h[1] * (c[c.length - 4] + c[c.length - 2])
                   + h[2] * (c[c.length - 5] + c[c.length - 1])
                   + h[3] * (c[c.length - 6] + c[c.length - 1]);
                s[s.length - 2] = h[0] * c[c.length - 2]
                   + h[1] * (c[c.length - 3] + c[c.length - 1])
                   + h[2] * (c[c.length - 4] + c[c.length - 1])
                   + h[3] * (c[c.length - 5] + c[c.length - 2]);
                s[s.length - 1] = h[0] * c[c.length - 1]
                   + h[1] * (c[c.length - 2] + c[c.length - 1])
                   + h[2] * (c[c.length - 3] + c[c.length - 2])
                   + h[3] * (c[c.length - 4] + c[c.length - 3]);
             }
             else {
                switch (c.length) {
                   case 5:
                      s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
                         + h[3] * (c[2] + c[3]);
                      s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
                         + h[3] * (c[1] + c[4]);
                      s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
                         + (h[2] + h[3]) * (c[0] + c[4]);
                      s[3] = h[0] * c[3] + h[1] * (c[2] + c[4]) + h[2] * (c[1] + c[4])
                         + h[3] * (c[0] + c[3]);
                      s[4] = h[0] * c[4] + h[1] * (c[3] + c[4]) + h[2] * (c[2] + c[3])
                         + h[3] * (c[1] + c[2]);
                      break;
                   case 4:
                      s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
                         + h[3] * (c[2] + c[3]);
                      s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
                         + h[3] * (c[1] + c[3]);
                      s[2] = h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[3])
                         + h[3] * (c[0] + c[2]);
                      s[3] = h[0] * c[3] + h[1] * (c[2] + c[3]) + h[2] * (c[1] + c[2])
                         + h[3] * (c[0] + c[1]);
                      break;
                   case 3:
                      s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
                         + 2.0 * h[3] * c[2];
                      s[1] = h[0] * c[1] + (h[1] + h[2]) * (c[0] + c[2])
                         + 2.0 * h[3] * c[1];
                      s[2] = h[0] * c[2] + h[1] * (c[1] + c[2]) + h[2] * (c[0] + c[1])
                         + 2.0 * h[3] * c[0];
                      break;
                   case 2:
                      s[0] = (h[0] + h[1] + h[3]) * c[0] + (h[1] + 2.0 * h[2] + h[3]) * c[1];
                      s[1] = (h[0] + h[1] + h[3]) * c[1] + (h[1] + 2.0 * h[2] + h[3]) * c[0];
                      break;
                   case 1:
                      s[0] = (h[0] + 2.0 * (h[1] + h[2] + h[3])) * c[0];
                      break;
                   default:
                } 
             }
             break;
          default:
       }
    } /* end symmetricFirMirrorOffBounds1D */

} /* end class bUnwarpJImageModel */

/*====================================================================
|   bUnwarpJMask
\===================================================================*/

/**
 * This class is responsible for the mask preprocessing that takes
 * place concurrently with user-interface events. It contains methods
 * to compute the mask pyramids.
 */
class bUnwarpJMask
{ /* begin class bUnwarpJMask */

    /*....................................................................
       Private variables
    ....................................................................*/
    // Mask related
    /** mask flags */
    private boolean[]     mask;
    /** mask width */
    private int           width;
    /** mask height */
    private int           height;
    /** polygon composing the flag */
    private Polygon       polygon=null;
    /** flag to check if the mask comes from the stack of images */
    private boolean       mask_from_the_stack;

    /*....................................................................
       Public methods
    ....................................................................*/

    /**
     * Bounding box for the mask.
     * An array is returned with the convention [x0,y0,xF,yF]. This array
     * is returned in corners. This vector should be already resized. 
     *
     * @param corners array of coordinates of the bounding box
     */
    public void BoundingBox(int [] corners) 
    {
       if (polygon.npoints!=0) {
          Rectangle boundingbox=polygon.getBounds();
          corners[0]=(int)boundingbox.x;
          corners[1]=(int)boundingbox.y;
          corners[2]=corners[0]+(int)boundingbox.width;
          corners[3]=corners[1]+(int)boundingbox.height;
       } else {
          corners[0]=0;
          corners[1]=0;
          corners[2]=width;
          corners[3]=height;
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Set to true every pixel of the full-size mask.
     */
    public void clearMask ()
    {
       int k = 0;
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++)
             mask[k++] = true;
       polygon=new Polygon();
    } /* end clearMask */

    /*------------------------------------------------------------------*/
    /**
     * Fill the mask associated to the mask points.
     *
     * @param tool option to invert or not the mask
     */
    public void fillMask (int tool) 
    {
       int k=0;
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++) {
             mask[k] = polygon.contains(x,y);
             if (tool==bUnwarpJPointAction.INVERTMASK) mask[k]=!mask[k];
             k++;
          }
    }

    /*------------------------------------------------------------------*/
    /**
     * Get the value of the mask at a certain pixel.
     * If the sample is not integer then the closest point is returned. 
     *
     * @param x x- coordinate of the pixel
     * @param y y- coordinate of the pixel
     * @return value of the mask at the pixel in (x,y)
     */
    public boolean getValue(double x, double y) 
    {
       int u=(int)Math.round(x);
       int v=(int)Math.round(y);
       if (u<0 || u>=width || v<0 || v>=height) return false;
       else                                     return mask[v*width+u];
    }

    /*------------------------------------------------------------------*/
    /**
     * Get a point from the mask.
     *
     * @param i index of the point in the polygong
     * @return corresponding point
     */
    public Point getPoint(int i) 
    {
       return new Point(polygon.xpoints[i],polygon.ypoints[i]);
    }

    /*------------------------------------------------------------------*/
    /**
     * Check if the mask was taken from the stack.
     *
     * @return True if the mask was taken from the stack
     */
    public boolean isFromStack() 
    {
       return mask_from_the_stack;
    }

    /*------------------------------------------------------------------*/
    /**
     * Get the number of points in the mask.
     *
     * @return number of point of the polygon that composes the mask
     */
    public int numberOfMaskPoints() {return polygon.npoints;}

    /*------------------------------------------------------------------*/
    /**
     * Read mask from file.
     * An error is shown if the file read is not of the same size as the
     * previous mask.
     * 
     * @param filename name of the mask file
     */
    public void readFile(String filename) 
    {
       ImagePlus aux = new ImagePlus(filename);
       if (aux.getWidth()!=width || aux.getHeight()!=height)
          IJ.error("Mask in file is not of the expected size");
       ImageProcessor ip = aux.getProcessor();
       int k=0;
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++, k++)
             if (ip.getPixelValue(x,y)!=0) mask[k]=true;
             else                          mask[k]=false;
    }

    /*------------------------------------------------------------------*/
    /**
     * Show mask.
     */
    public void showMask () 
    {
        double [][]img=new double[height][width];
       int k = 0;
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++)
             if (mask[k++]) img[y][x]=1; else img[y][x]=0;
       bUnwarpJMiscTools.showImage("Mask",img);
    }

    /*------------------------------------------------------------------*/
    /**
     * Set the mask points.
     * 
     * @param listMaskPoints list of point composing the mask
     */
    public void setMaskPoints (final Vector listMaskPoints) 
    {
       int imax=listMaskPoints.size();
       for (int i=0; i<imax; i++) 
       {
          Point p=(Point)listMaskPoints.elementAt(i);
          polygon.addPoint(p.x,p.y);
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Sets the value of the mask at a certain pixel.
     *
     * @param u x- coordinate of the pixel
     * @param v y- coordinate of the pixel
     * @param value mask value to be set
     */
    public void setValue(int u, int v, boolean value) 
    {
       if (u>=0 && u<width && v>=0 && v<height) mask[v*width+u]=value;
    }

    /*------------------------------------------------------------------*/
    /**
     * Empty constructor, the input image is used only to take the
     * image size. 
     *
     * @param ip image
     * @param take_mask flag to take the mask from the stack of images
     */
    public bUnwarpJMask (
       final ImageProcessor ip, boolean take_mask
    ) 
    {
       width  = ip.getWidth();
       height = ip.getHeight();
       mask = new boolean[width * height];
       if (!take_mask) {
           mask_from_the_stack=false;
           clearMask();
       } else {
           mask_from_the_stack=true;
           int k=0;
          if (ip instanceof ByteProcessor) {
             final byte[] pixels = (byte[])ip.getPixels();
             for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                   mask[k] = (pixels[k] != 0);
                }
             }
          }
          else if (ip instanceof ShortProcessor) {
             final short[] pixels = (short[])ip.getPixels();
             for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                   mask[k] = (pixels[k] != 0);
                }
             }
          }
          else if (ip instanceof FloatProcessor) {
             final float[] pixels = (float[])ip.getPixels();
             for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                   mask[k] = (pixels[k] != 0.0F);
                }
             }
          }
       }
    } /* end bUnwarpJMask */

} /* end class bUnwarpJMask */

/*====================================================================
|   bUnwarpJMathTools
\===================================================================*/
/**
 * This class has the math methods to deal with b-splines.
 */
class bUnwarpJMathTools 
{
    /** float epsilon */
    private static final double FLT_EPSILON = (double)Float.intBitsToFloat((int)0x33FFFFFF);
    /** maximum number of iteration for the Singular Value Decomposition */
    private static final int MAX_SVD_ITERATIONS = 1000;

    /*------------------------------------------------------------------*/
    /**
     * B-spline 01.
     *
     * @param x 
     */
    public static double Bspline01 (double x)
    {
       x = Math.abs(x);
       if (x < 1.0F) {
          return(1.0F - x);
       }
       else {
          return(0.0F);
       }
    } /* Bspline01 */

    /*------------------------------------------------------------------*/
    /**
     * B-spline 02.
     *
     * @param x 
     */
    public static double Bspline02 (double x) 
    {
       x = Math.abs(x);
       if (x < 0.5F) {
          return(3.0F / 4.0F - x * x);
       }
       else if (x < 1.5F) {
          x -= 1.5F;
          return(0.5F * x * x);
       }
       else {
          return(0.0F);
       }
    } /* Bspline02 */

    /*------------------------------------------------------------------*/
    /**
     * B-spline 03.
     *
     * @param x 
     */
    public static double Bspline03 (double x) 
    {
       x = Math.abs(x);
       if (x < 1.0F) {
          return(0.5F * x * x * (x - 2.0F) + (2.0F / 3.0F));
       }
       else if (x < 2.0F) {
          x -= 2.0F;
          return(x * x * x / (-6.0F));
       }
       else {
          return(0.0F);
       }
    } /* Bspline03 */

    /*------------------------------------------------------------------*/
    /**
     * Euclidean Norm.
     *
     * @param a
     * @param b 
     */
    public static double EuclideanNorm (
       final double a,
       final double b) 
    {
       final double absa = Math.abs(a);
       final double absb = Math.abs(b);
       if (absb < absa) {
          return(absa * Math.sqrt(1.0 + (absb * absb / (absa * absa))));
       }
       else {
          return((absb == 0.0F) ? (0.0F)
             : (absb * Math.sqrt(1.0 + (absa * absa / (absb * absb)))));
       }
    } /* end EuclideanNorm */

    /*------------------------------------------------------------------*/
    /**
     * Invert a matrix by the Singular Value Decomposition method.
     *
     * @param Ydim input, Y-dimension
     * @param Xdim input, X-dimension
     * @param B input, matrix to invert
     * @param iB output, inverted matrix
     * @return under-constrained flag
     */
    public static boolean invertMatrixSVD(
             int   Ydim,   
             int   Xdim,   
       final double [][]B, 
       final double [][]iB) 
    {
       boolean underconstrained=false;

       final double[] W = new double[Xdim];
       final double[][] V = new double[Xdim][Xdim];
       // B=UWV^t (U is stored in B)
       singularValueDecomposition(B, W, V);

       // B^-1=VW^-1U^t

       // Compute W^-1
       int Nzeros=0;
       for (int k = 0; k<Xdim; k++) {
          if (Math.abs(W[k]) < FLT_EPSILON) {
             W[k] = 0.0F;
             Nzeros++;
          } else 
             W[k] = 1.0F / W[k];
       }
       if (Ydim-Nzeros<Xdim) underconstrained=true;

       // Compute VW^-1
       for (int i = 0; i<Xdim; i++)
          for (int j = 0; j<Xdim; j++)
             V[i][j] *= W[j];

       // Compute B^-1
       // iB should have been already resized
       for (int i = 0; i<Xdim; i++) {
          for (int j = 0; j<Ydim; j++) {
             iB[i][j] = 0.0F;
             for (int k = 0; k<Xdim; k++)
                iB[i][j] += V[i][k] * B[j][k];
          }
       }
       return underconstrained;
    } /* invertMatrixSVD */

    /*------------------------------------------------------------------*/
    /**
     * Gives the least-squares solution to (A * x = b) such that
     * (A^T * A)^-1 * A^T * b = x is a vector of size (column), where A is
     * a (line x column) matrix, and where b is a vector of size (line).
     * The result may differ from that obtained by a singular-value
     * decomposition in the cases where the least-squares solution is not
     * uniquely defined (SVD returns the solution of least norm, not QR).
     *
     * @param A An input matrix A[line][column] of size (line x column)
     * @param b An input vector b[line] of size (line)
     * @return An output vector x[column] of size (column)
     */
    public static double[] linearLeastSquares (
       final double[][] A,
       final double[] b)
    {
       final int lines = A.length;
       final int columns = A[0].length;
       final double[][] Q = new double[lines][columns];
       final double[][] R = new double[columns][columns];
       final double[] x = new double[columns];
       double s;
       for (int i = 0; (i < lines); i++) {
          for (int j = 0; (j < columns); j++) {
             Q[i][j] = A[i][j];
          }
       }
       QRdecomposition(Q, R);
       for (int i = 0; (i < columns); i++) {
          s = 0.0F;
          for (int j = 0; (j < lines); j++) {
             s += Q[j][i] * b[j];
          }
          x[i] = s;
       }
       for (int i = columns - 1; (0 <= i); i--) {
          s = R[i][i];
          if ((s * s) == 0.0F) {
             x[i] = 0.0F;
          }
          else {
             x[i] /= s;
          }
          for (int j = i - 1; (0 <= j); j--) {
             x[j] -= R[j][i] * x[i];
          }
       }
       return(x);
    } /* end linearLeastSquares */

    /*------------------------------------------------------------------*/
    /**
     * N choose K.
     *
     * @param n
     * @param k
     */
    public static double nchoosek(int n, int k) 
    {
       if (k>n)  return 0;
       if (k==0) return 1;
       if (k==1) return n;
       if (k>n/2) k=n-k;
       double prod=1;
       for (int i=1; i<=k; i++) prod*=(n-k+i)/i; 
       return prod;
    }

    /*------------------------------------------------------------------*/
    /**
     * Decomposes the (line x column) input matrix Q into an orthonormal
     * output matrix Q of same size (line x column) and an upper-diagonal
     * square matrix R of size (column x column), such that the matrix
     * product (Q * R) gives the input matrix, and such that the matrix
     * product (Q^T * Q) gives the identity.
     *
     * @param Q An in-place (line x column) matrix Q[line][column], which
     * expects as input the matrix to decompose, and which returns as
     * output an orthonormal matrix
     * @param R An output (column x column) square matrix R[column][column]
     */
    public static void QRdecomposition (
       final double[][] Q,
       final double[][] R)
    {
       final int lines = Q.length;
       final int columns = Q[0].length;
       final double[][] A = new double[lines][columns];
       double s;
       for (int j = 0; (j < columns); j++) {
          for (int i = 0; (i < lines); i++) {
             A[i][j] = Q[i][j];
          }
          for (int k = 0; (k < j); k++) {
             s = 0.0F;
             for (int i = 0; (i < lines); i++) {
                s += A[i][j] * Q[i][k];
             }
             for (int i = 0; (i < lines); i++) {
                Q[i][j] -= s * Q[i][k];
             }
          }
          s = 0.0F;
          for (int i = 0; (i < lines); i++) {
             s += Q[i][j] * Q[i][j];
          }
          if ((s * s) == 0.0F) {
             s = 0.0F;
          }
          else {
             s = 1.0F / Math.sqrt(s);
          }
          for (int i = 0; (i < lines); i++) {
             Q[i][j] *= s;
          }
       }
       for (int i = 0; (i < columns); i++) {
          for (int j = 0; (j < i); j++) {
             R[i][j] = 0.0F;
          }
          for (int j = i; (j < columns); j++) {
             R[i][j] = 0.0F;
             for (int k = 0; (k < lines); k++) {
                R[i][j] += Q[k][i] * A[k][j];
             }
          }
       }
    } /* end QRdecomposition */

    /* -----------------------------------------------------------------*/
    /**
     * Method to display the matrix in the command line.
     *
     * @param Ydim Y-dimension
     * @param Xdim X-dimension
     * @param A matrix to display
     */
    public static void showMatrix(
       int Ydim,
       int Xdim,
       final double [][]A)
    {
       for (int i=0; i<Ydim; i++) {
          for (int j=0; j<Xdim; j++)
             System.out.print(A[i][j]+" ");
          System.out.println();
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Singular Value Decomposition.
     *
     * @param U input matrix
     * @param W vector of singular values
     * @param V untransposed orthogonal matrix
     */
    public static void singularValueDecomposition (
       final double[][] U,
       final double[] W,
       final double[][] V)
    {
       final int lines = U.length;
       final int columns = U[0].length;
       final double[] rv1 = new double[columns];
       double norm, scale;
       double c, f, g, h, s;
       double x, y, z;
       int l = 0;
       int nm = 0;
       boolean   flag;
       g = scale = norm = 0.0F;
       for (int i = 0; (i < columns); i++) {
          l = i + 1;
          rv1[i] = scale * g;
          g = s = scale = 0.0F;
          if (i < lines) {
             for (int k = i; (k < lines); k++) {
                scale += Math.abs(U[k][i]);
             }
             if (scale != 0.0) {
                for (int k = i; (k < lines); k++) {
                   U[k][i] /= scale;
                   s += U[k][i] * U[k][i];
                }
                f = U[i][i];
                g = (0.0 <= f) ? (-Math.sqrt((double)s))
                   : (Math.sqrt((double)s));
                h = f * g - s;
                U[i][i] = f - g;
                for (int j = l; (j < columns); j++) {
                   s = 0.0F;
                   for (int k = i; (k < lines); k++) {
                      s += U[k][i] * U[k][j];
                   }
                   f = s / h;
                   for (int k = i; (k < lines); k++) {
                      U[k][j] += f * U[k][i];
                   }
                }
                for (int k = i; (k < lines); k++) {
                   U[k][i] *= scale;
                }
             }
          }
          W[i] = scale * g;
          g = s = scale = 0.0F;
          if ((i < lines) && (i != (columns - 1))) {
             for (int k = l; (k < columns); k++) {
                scale += Math.abs(U[i][k]);
             }
             if (scale != 0.0) {
                for (int k = l; (k < columns); k++) {
                   U[i][k] /= scale;
                   s += U[i][k] * U[i][k];
                }
                f = U[i][l];
                g = (0.0 <= f) ? (-Math.sqrt(s))
                   : (Math.sqrt(s));
                h = f * g - s;
                U[i][l] = f - g;
                for (int k = l; (k < columns); k++) {
                   rv1[k] = U[i][k] / h;
                }
                for (int j = l; (j < lines); j++) {
                   s = 0.0F;
                   for (int k = l; (k < columns); k++) {
                      s += U[j][k] * U[i][k];
                   }
                   for (int k = l; (k < columns); k++) {
                      U[j][k] += s * rv1[k];
                   }
                }
                for (int k = l; (k < columns); k++) {
                   U[i][k] *= scale;
                }
             }
          }
          norm = ((Math.abs(W[i]) + Math.abs(rv1[i])) < norm) ? (norm)
             : (Math.abs(W[i]) + Math.abs(rv1[i]));
       }
       for (int i = columns - 1; (0 <= i); i--) {
          if (i < (columns - 1)) {
             if (g != 0.0) {
                for (int j = l; (j < columns); j++) {
                   V[j][i] = U[i][j] / (U[i][l] * g);
                }
                for (int j = l; (j < columns); j++) {
                   s = 0.0F;
                   for (int k = l; (k < columns); k++) {
                      s += U[i][k] * V[k][j];
                   }
                   for (int k = l; (k < columns); k++) {
                      if (s != 0.0) {
                         V[k][j] += s * V[k][i];
                      }
                   }
                }
             }
             for (int j = l; (j < columns); j++) {
                V[i][j] = V[j][i] = 0.0F;
             }
          }
          V[i][i] = 1.0F;
          g = rv1[i];
          l = i;
       }
       for (int i = (lines < columns) ? (lines - 1) : (columns - 1); (0 <= i); i--) {
          l = i + 1;
          g = W[i];
          for (int j = l; (j < columns); j++) {
             U[i][j] = 0.0F;
          }
          if (g != 0.0) {
             g = 1.0F / g;
             for (int j = l; (j < columns); j++) {
                s = 0.0F;
                for (int k = l; (k < lines); k++) {
                   s += U[k][i] * U[k][j];
                }
                f = s * g / U[i][i];
                for (int k = i; (k < lines); k++) {
                   if (f != 0.0) {
                      U[k][j] += f * U[k][i];
                   }
                }
             }
             for (int j = i; (j < lines); j++) {
                U[j][i] *= g;
             }
          }
          else {
             for (int j = i; (j < lines); j++) {
                U[j][i] = 0.0F;
             }
          }
          U[i][i] += 1.0F;
       }
       for (int k = columns - 1; (0 <= k); k--) {
          for (int its = 1; (its <= MAX_SVD_ITERATIONS); its++) {
             flag = true;
             for (l = k; (0 <= l); l--) {
                nm = l - 1;
                if ((Math.abs(rv1[l]) + norm) == norm) {
                   flag = false;
                   break;
                }
                if ((Math.abs(W[nm]) + norm) == norm) {
                   break;
                }
             }
             if (flag) {
                c = 0.0F;
                s = 1.0F;
                for (int i = l; (i <= k); i++) {
                   f = s * rv1[i];
                   rv1[i] *= c;
                   if ((Math.abs(f) + norm) == norm) {
                      break;
                   }
                   g = W[i];
                   h = EuclideanNorm(f, g);
                   W[i] = h;
                   h = 1.0F / h;
                   c = g * h;
                   s = -f * h;
                   for (int j = 0; (j < lines); j++) {
                      y = U[j][nm];
                      z = U[j][i];
                      U[j][nm] = y * c + z * s;
                      U[j][i] = z * c - y * s;
                   }
                }
             }
             z = W[k];
             if (l == k) {
                if (z < 0.0) {
                   W[k] = -z;
                   for (int j = 0; (j < columns); j++) {
                      V[j][k] = -V[j][k];
                   }
                }
                break;
             }
             if (its == MAX_SVD_ITERATIONS) {
                return;
             }
             x = W[l];
             nm = k - 1;
             y = W[nm];
             g = rv1[nm];
             h = rv1[k];
             f = ((y - z) * (y + z) + (g - h) * (g + h)) / (2.0F * h * y);
             g = EuclideanNorm(f, 1.0F);
             f = ((x - z) * (x + z) + h * ((y / (f + ((0.0 <= f) ? (Math.abs(g))
                : (-Math.abs(g))))) - h)) / x;
             c = s = 1.0F;
             for (int j = l; (j <= nm); j++) {
                int i = j + 1;
                g = rv1[i];
                y = W[i];
                h = s * g;
                g = c * g;
                z = EuclideanNorm(f, h);
                rv1[j] = z;
                c = f / z;
                s = h / z;
                f = x * c + g * s;
                g = g * c - x * s;
                h = y * s;
                y *= c;
                for (int jj = 0; (jj < columns); jj++) {
                   x = V[jj][j];
                   z = V[jj][i];
                   V[jj][j] = x * c + z * s;
                   V[jj][i] = z * c - x * s;
                }
                z = EuclideanNorm(f, h);
                W[j] = z;
                if (z != 0.0F) {
                   z = 1.0F / z;
                   c = f * z;
                   s = h * z;
                }
                f = c * g + s * y;
                x = c * y - s * g;
                for (int jj = 0; (jj < lines); jj++) {
                   y = U[jj][j];
                   z = U[jj][i];
                   U[jj][j] = y * c + z * s;
                   U[jj][i] = z * c - y * s;
                }
             }
             rv1[l] = 0.0F;
             rv1[k] = f;
             W[k] = x;
          }
       }
    } /* end singularValueDecomposition */

    /**
     * solve (U.W.Transpose(V)).X == B in terms of X 
     * {U, W, V} are given by SingularValueDecomposition 
     * by convention, set w[i,j]=0 to get (1/w[i,j])=0 
     * the size of the input matrix U is (Lines x Columns) 
     * the size of the vector (1/W) of singular values is (Columns) 
     * the size of the untransposed orthogonal matrix V is (Columns x Columns) 
     * the size of the input vector B is (Lines) 
     * the size of the output vector X is (Columns) 
     *
     * @param U input matrix
     * @param W vector of singular values
     * @param V untransposed orthogonal matrix
     * @param B input vector 
     * @param X returned solution
     */
    public static void singularValueBackSubstitution (
       final double [][]U,      
       final double   []W,      
       final double [][]V,      
       final double   []B,    
       final double   []X)
    {

       final int Lines   = U.length;
       final int Columns = U[0].length;
       double []  aux     = new double [Columns];

       // A=UWV^t
       // A^-1=VW^-1U^t
       // X=A^-1*B

       // Perform aux=W^-1 U^t B
       for (int i=0; i<Columns; i++) {
           aux[i]=0.0F;
          if (Math.abs(W[i]) > FLT_EPSILON) {
              for (int j=0; j<Lines; j++) aux[i]+=U[j][i]*B[j]; // U^t B
              aux[i]/=W[i];                                     // W^-1 U^t B
           }
       }

       // Perform X=V aux
       for (int i=0; i<Columns; i++) {
           X[i]=0.0F;
           for (int j=0; j<Columns; j++) X[i]+=V[i][j]*aux[j];
       }
    }

} /* End MathTools */

/*====================================================================
|   bUnwarpJMiscTools
\===================================================================*/
/**
 * Different tools for the bUnwarpJ interface.
 */
class bUnwarpJMiscTools 
{
    /**
     * Apply a given splines transformation to the source image.
     * The source image is modified. The target image is used to know
     * the output size.
     *
     * @param sourceImp source image representation
     * @param targetImp target image representation
     * @param source source image
     * @param intervals intervals in the deformation
     * @param cx x- b-spline coefficients
     * @param cy y- b-spline coefficients
     */
    static public void applyTransformationToSource(
       ImagePlus sourceImp, 
       ImagePlus targetImp,
       bUnwarpJImageModel source,
       int intervals,
       double [][]cx,
       double [][]cy) 
    {
       int targetHeight = targetImp.getProcessor().getHeight();
       int targetWidth  = targetImp.getProcessor().getWidth ();
       int sourceHeight = sourceImp.getProcessor().getHeight();
       int sourceWidth  = sourceImp.getProcessor().getWidth ();

       // Ask for memory for the transformation
       double [][] transformation_x = new double [targetHeight][targetWidth];
       double [][] transformation_y = new double [targetHeight][targetWidth];

       // Compute the deformation
       // Set these coefficients to an interpolator
       bUnwarpJImageModel swx = new bUnwarpJImageModel(cx);
       bUnwarpJImageModel swy = new bUnwarpJImageModel(cy);

        // Compute the transformation mapping
        boolean ORIGINAL = false;
        for (int v=0; v<targetHeight; v++) 
        {
          final double tv = (double)(v * intervals) / (double)(targetHeight - 1) + 1.0F;
          for (int u = 0; u<targetWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(targetWidth - 1) + 1.0F;

             swx.prepareForInterpolation(tu, tv, ORIGINAL); 
             transformation_x[v][u] = swx.interpolateI();

             swy.prepareForInterpolation(tu, tv, ORIGINAL); 
             transformation_y[v][u] = swy.interpolateI();
          }
        }

        // Compute the warped image
        FloatProcessor fp = new FloatProcessor(targetWidth, targetHeight);
        for (int v=0; v<targetHeight; v++)
          for (int u=0; u<targetWidth; u++) 
          {
             final double x = transformation_x[v][u];
             final double y = transformation_y[v][u];

             if (x>=0 && x<sourceWidth && y>=0 && y<sourceHeight) 
             {
                 source.prepareForInterpolation(x, y, ORIGINAL);
                 fp.putPixelValue(u, v, source.interpolateI());
             } 
             else
                 fp.putPixelValue(u, v, 0);
          }
        fp.resetMinAndMax();
        sourceImp.setProcessor(sourceImp.getTitle(), fp);
        sourceImp.updateImage();
    }

    /**
     * Apply a given raw transformation to the source image.
     * The source image is modified. The target image is used to know
     * the output size.
     *
     * @param sourceImp source image representation
     * @param targetImp target image representation
     * @param source source image
     * @param transformation_x x- mapping coordinates
     * @param transformation_y y- mapping coordinates
     */
    static public void applyRawTransformationToSource(
       ImagePlus sourceImp,
       ImagePlus targetImp,
       bUnwarpJImageModel source,
       double [][] transformation_x,
       double [][] transformation_y) 
    {
       int targetHeight = targetImp.getProcessor().getHeight();
       int targetWidth  = targetImp.getProcessor().getWidth ();
       int sourceHeight = sourceImp.getProcessor().getHeight();
       int sourceWidth  = sourceImp.getProcessor().getWidth ();

       boolean ORIGINAL = false;

       // Compute the warped image
       FloatProcessor fp = new FloatProcessor(targetWidth, targetHeight);
       for (int v=0; v<targetHeight; v++)
          for (int u=0; u<targetWidth; u++) 
          {
             final double x = transformation_x[v][u];
             final double y = transformation_y[v][u];

             if (x>=0 && x<sourceWidth && y>=0 && y<sourceHeight) 
             {
                 source.prepareForInterpolation(x, y, ORIGINAL);
                 fp.putPixelValue(u, v, source.interpolateI());
             } 
             else
                 fp.putPixelValue(u, v, 0);
          }
       fp.resetMinAndMax();
       sourceImp.setProcessor(sourceImp.getTitle(), fp);
       sourceImp.updateImage();
    }
    
    
    /**
     * Warping index.
     *
     * @param sourceImp source image representation
     * @param targetImp target image representation
     * @param intervals intervals in the deformation
     * @param cx_direct direct transformation x- b-spline coefficients
     * @param cy_direct direct transformation y- b-spline coefficients
     * @param cx_inverse inverse transformation x- b-spline coefficients
     * @param cy_inverse inverse transformation y- b-spline coefficients
     */
    public static double warpingIndex(
       ImagePlus sourceImp, 
       ImagePlus targetImp,
       int intervals,
       double [][]cx_direct,
       double [][]cy_direct,
       double [][]cx_inverse,
       double [][]cy_inverse) 
    {
       // Ask for memory for the transformation
       int targetCurrentHeight = targetImp.getProcessor().getHeight();
       int targetCurrentWidth  = targetImp.getProcessor().getWidth ();
       int sourceCurrentHeight = sourceImp.getProcessor().getHeight();
       int sourceCurrentWidth  = sourceImp.getProcessor().getWidth ();
       
       double [][] transformation_x_direct = new double [targetCurrentHeight][targetCurrentWidth];
       double [][] transformation_y_direct = new double [targetCurrentHeight][targetCurrentWidth];

       double [][] transformation_x_inverse = new double [sourceCurrentHeight][sourceCurrentWidth];
       double [][] transformation_y_inverse = new double [sourceCurrentHeight][sourceCurrentWidth];

       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim * cXdim;
       int twiceNk = 2 * Nk;    
          
       // We pass the coffiecients to a one-dimension array
       // Direct coefficients.
       double c_direct[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c_direct[n     ] = cx_direct[i][j];
               c_direct[n + Nk] = cy_direct[i][j];
           }

       // Compute the deformation
       // Set these coefficients to an interpolator
       bUnwarpJImageModel swx_direct = new bUnwarpJImageModel(c_direct, cYdim, cXdim, 0); 
       bUnwarpJImageModel swy_direct = new bUnwarpJImageModel(c_direct, cYdim, cXdim, Nk);  

       // Inverse coefficients.
       double c_inverse[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c_inverse[n     ] = cx_inverse[i][j];
               c_inverse[n + Nk] = cy_inverse[i][j];
           }
       
       bUnwarpJImageModel swx_inverse = new bUnwarpJImageModel(c_inverse, cYdim, cXdim, 0);  
       bUnwarpJImageModel swy_inverse = new bUnwarpJImageModel(c_inverse, cYdim, cXdim, Nk); 

       
       swx_direct.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);
       swy_direct.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);

       swx_inverse.precomputed_prepareForInterpolation(
          sourceCurrentHeight, sourceCurrentWidth, intervals);
       swy_inverse.precomputed_prepareForInterpolation(
          sourceCurrentHeight, sourceCurrentWidth, intervals);

       // Compute the direct transformation mapping
       for (int v=0; v<targetCurrentHeight; v++) 
       {
          final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<targetCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

             swx_direct.prepareForInterpolation(tu, tv, false); 
             transformation_x_direct[v][u] = swx_direct.interpolateI();

             swy_direct.prepareForInterpolation(tu, tv, false); 
             transformation_y_direct[v][u] = swy_direct.interpolateI();
          }
       }

        // Compute the inverse transformation mapping    
        for (int v=0; v<sourceCurrentHeight; v++) 
        {
          final double tv = (double)(v * intervals) / (double)(sourceCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<sourceCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(sourceCurrentWidth - 1) + 1.0F;

             swx_inverse.prepareForInterpolation(tu, tv, false); 
             transformation_x_inverse[v][u] = swx_inverse.interpolateI();

             swy_inverse.prepareForInterpolation(tu, tv, false); 
             transformation_y_inverse[v][u] = swy_inverse.interpolateI();
          }
        }    

       
        // *********** Compute the geometrical error and gradient (DIRECT) ***********   
        double warpingIndex = 0;
        int n = 0;
        for (int v=0; v<targetCurrentHeight; v++)
            for (int u=0; u<targetCurrentWidth; u++) 
            {                
                 // Check if this point is in the target mask

                 final int x = (int) Math.round(transformation_x_direct[v][u]);
                 final int y = (int) Math.round(transformation_y_direct[v][u]);

                 if (x>=0 && x<sourceCurrentWidth && y>=0 && y<sourceCurrentHeight) 
                 {
                    final double x2 = transformation_x_inverse[y][x];
                    final double y2 = transformation_y_inverse[y][x];
                    double aux1 = u - x2;
                    double aux2 = v - y2;

                    warpingIndex += aux1 * aux1 + aux2 * aux2;

                    n++; // Another point has been successfully evaluated
                 }   
            }      

        if(n != 0)
        {            
            warpingIndex /= (double) n;
            warpingIndex = Math.sqrt(warpingIndex);
        }
        else
            warpingIndex = -1;
        return warpingIndex;
    }    
    /*------------------------------------------------------------------*/
    /**
     * Calculate the raw transformation mapping from b-spline 
     * coefficients.
     *
     * @param targetImp target image representation
     * @param intervals intervals in the deformation
     * @param cx transformation x- b-spline coefficients
     * @param cy transformation y- b-spline coefficients
     * @param transformation_x raw transformation in x- axis (output)
     * @param transformation_y raw transformation in y- axis (output)
     */
   public static void convertElasticTransformationToRaw( 
       ImagePlus targetImp,
       int intervals,
       double [][] cx,
       double [][] cy,
       double [][] transformation_x,
       double [][] transformation_y)        
    {
       
       if(cx == null || cy == null || transformation_x == null || transformation_y == null)
       {
           IJ.error("Error in transformations parameters!");
           return;
       }
       
       // Ask for memory for the transformation
       int targetCurrentHeight = targetImp.getProcessor().getHeight();
       int targetCurrentWidth  = targetImp.getProcessor().getWidth ();
                   
       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim * cXdim;
       int twiceNk = 2 * Nk;    
              
       // We pass the coffiecients to a one-dimension array
       // Direct coefficients.
       double c[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c[n     ] = cx[i][j];
               c[n + Nk] = cy[i][j];
           }

       // Compute the deformation
       // Set these coefficients to an interpolator
       bUnwarpJImageModel swx = new bUnwarpJImageModel(c, cYdim, cXdim, 0); 
       bUnwarpJImageModel swy = new bUnwarpJImageModel(c, cYdim, cXdim, Nk);  
                   
              
       swx.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);
       swy.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);

       
       // Compute the direct transformation mapping
       for (int v=0; v<targetCurrentHeight; v++) 
       {
          final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<targetCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

             swx.prepareForInterpolation(tu, tv, false); 
             transformation_x[v][u] = swx.interpolateI();

             swy.prepareForInterpolation(tu, tv, false); 
             transformation_y[v][u] = swy.interpolateI();
          }
       }
   }

    /*------------------------------------------------------------------*/
    /**
     * Warping index for comparing elastic deformations with any kind 
     * of deformation.
     *
     * @param sourceImp source image representation
     * @param targetImp target image representation
     * @param intervals intervals in the deformation
     * @param cx_direct direct transformation x- b-spline coefficients
     * @param cy_direct direct transformation y- b-spline coefficients
     * @param transformation_x raw direct transformation in x- axis
     * @param transformation_y raw direct transformation in y- axis
     */
   public static double rawWarpingIndex(
       ImagePlus sourceImp, 
       ImagePlus targetImp,
       int intervals,
       double [][] cx_direct,
       double [][] cy_direct,
       double [][] transformation_x,
       double [][] transformation_y)        
    {
       
       if(cx_direct == null || cy_direct == null || transformation_x == null || transformation_y == null)
       {
           IJ.error("Error in the raw warping index parameters!");
           return -1;
       }
       
       // Ask for memory for the transformation
       int targetCurrentHeight = targetImp.getProcessor().getHeight();
       int targetCurrentWidth  = targetImp.getProcessor().getWidth ();
       int sourceCurrentHeight = sourceImp.getProcessor().getHeight();
       int sourceCurrentWidth  = sourceImp.getProcessor().getWidth ();
              
       double [][] transformation_x_direct = new double [targetCurrentHeight][targetCurrentWidth];
       double [][] transformation_y_direct = new double [targetCurrentHeight][targetCurrentWidth];
       
       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim * cXdim;
       int twiceNk = 2 * Nk;    
              
       // We pass the coffiecients to a one-dimension array
       // Direct coefficients.
       double c_direct[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c_direct[n     ] = cx_direct[i][j];
               c_direct[n + Nk] = cy_direct[i][j];
           }

       // Compute the deformation
       // Set these coefficients to an interpolator
       bUnwarpJImageModel swx_direct = new bUnwarpJImageModel(c_direct, cYdim, cXdim, 0); 
       bUnwarpJImageModel swy_direct = new bUnwarpJImageModel(c_direct, cYdim, cXdim, Nk);  
                   
              
       swx_direct.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);
       swy_direct.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);

       
       // Compute the direct transformation mapping
       for (int v=0; v<targetCurrentHeight; v++) 
       {
          final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<targetCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

             swx_direct.prepareForInterpolation(tu, tv, false); 
             transformation_x_direct[v][u] = swx_direct.interpolateI();

             swy_direct.prepareForInterpolation(tu, tv, false); 
             transformation_y_direct[v][u] = swy_direct.interpolateI();
          }
       }
       
       
        // Compute the geometrical error between both transformations
        double warpingIndex = 0;
        int n = 0;
        for (int v=0; v<targetCurrentHeight; v++)
            for (int u=0; u<targetCurrentWidth; u++) 
            {                
                // Calculate the mapping through the elastic deformation                
                final double x_elastic = transformation_x_direct[v][u];
                final double y_elastic = transformation_y_direct[v][u];

                if (x_elastic>=0 && x_elastic<sourceCurrentWidth && y_elastic>=0 && y_elastic<sourceCurrentHeight) 
                {
                    double x_random = transformation_x[v][u];
                    double y_random = transformation_y[v][u];
                                        
                    double aux1 = x_elastic - x_random;
                    double aux2 = y_elastic - y_random;
                    
                    warpingIndex += aux1 * aux1 + aux2 * aux2;                
                    
                    n++; // Another point has been successfully evaluated
                }
                   
            }   
          
        if(n != 0)
        {
            warpingIndex /= (double) n;
            warpingIndex = Math.sqrt(warpingIndex);
        }
        else
            warpingIndex = -1;
        return warpingIndex;
    }    
    /*------------------------------------------------------------------*/
    /**
     * Warping index for comparing two raw deformations.
     *
     * @param sourceImp source image representation
     * @param targetImp target image representation
     * @param transformation_x_1 raw first tranformation in x- axis
     * @param transformation_y_1 raw first tranformation in y- axis
     * @param transformation_x_2 raw second tranformation in x- axis
     * @param transformation_y_2 raw second tranformation in y- axis
     */
   public static double rawWarpingIndex(
       ImagePlus sourceImp, 
       ImagePlus targetImp,
       double [][] transformation_x_1,
       double [][] transformation_y_1,
       double [][] transformation_x_2,
       double [][] transformation_y_2)        
    {
       
       if(transformation_x_1 == null || transformation_y_1 == null || transformation_x_2 == null || transformation_y_2 == null)
       {
           IJ.error("Error in the raw warping index parameters!");
           return -1;
       }
       
       // Ask for memory for the transformation
       int targetCurrentHeight = targetImp.getProcessor().getHeight();
       int targetCurrentWidth  = targetImp.getProcessor().getWidth ();
       int sourceCurrentHeight = sourceImp.getProcessor().getHeight();
       int sourceCurrentWidth  = sourceImp.getProcessor().getWidth ();
              
        // Compute the geometrical error between both transformations
        double warpingIndex = 0;
        int n = 0;
        for (int v=0; v<targetCurrentHeight; v++)
            for (int u=0; u<targetCurrentWidth; u++) 
            {                
                // Calculate the mapping through the elastic deformation                
                final double x1 = transformation_x_1[v][u];
                final double y1 = transformation_y_1[v][u];

                if (x1>=0 && x1<sourceCurrentWidth && y1>=0 && y1<sourceCurrentHeight) 
                {
                    double x2 = transformation_x_2[v][u];
                    double y2 = transformation_y_2[v][u];
                                        
                    double aux1 = x1 - x2;
                    double aux2 = y1 - y2;
                    
                    warpingIndex += aux1 * aux1 + aux2 * aux2;                
                    
                    n++; // Another point has been successfully evaluated
                }
                   
            }   
          
        if(n != 0)
        {
            warpingIndex /= (double) n;
            warpingIndex = Math.sqrt(warpingIndex);
        }
        else
            warpingIndex = -1;
        return warpingIndex;
    }        
    /*------------------------------------------------------------------*/    
        
    /**
     * Draw an arrow between two points.
     * The arrow head is in (x2,y2) 
     *
     * @param canvas canvas where we are painting
     * @param x1 x- coordinate for the arrow origin
     * @param y1 y- coordinate for the arrow origin
     * @param x2 x- coordinate for the arrow head
     * @param y2 y- coordinate for the arrow head
     * @param color arrow color
     * @param arrow_size arrow size
     */
    static public void drawArrow(double [][]canvas, int x1, int y1,
       int x2, int y2, double color, int arrow_size) 
    {
       drawLine(canvas,x1,y1,x2,y2,color);
       int arrow_size2 = 2 * arrow_size;

       // Do not draw the arrow_head if the arrow is very small
       if ((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)<arrow_size*arrow_size) return;

       // Vertical arrow
       if (x2 == x1) {
          if (y2 > y1) {
            drawLine(canvas,x2,y2,x2-arrow_size,y2-arrow_size2,color);
            drawLine(canvas,x2,y2,x2+arrow_size,y2-arrow_size2,color);
          } else {
            drawLine(canvas,x2,y2,x2-arrow_size,y2+arrow_size2,color);
            drawLine(canvas,x2,y2,x2+arrow_size,y2+arrow_size2,color);
          }
        }

        // Horizontal arrow
        else if (y2 == y1) {
          if (x2 > x1) {
            drawLine(canvas,x2,y2,x2-arrow_size2,y2-arrow_size,color);
            drawLine(canvas,x2,y2,x2-arrow_size2,y2+arrow_size,color);
          } else {
            drawLine(canvas,x2,y2,x2+arrow_size2,y2-arrow_size,color);
            drawLine(canvas,x2,y2,x2+arrow_size2,y2+arrow_size,color);
          }
        }

        // Now we need to rotate the arrow head about the origin
        else {
          // Calculate the angle of rotation and adjust for the quadrant
          double t1 = Math.abs(new Integer(y2 - y1).doubleValue());
          double t2 = Math.abs(new Integer(x2 - x1).doubleValue());
          double theta = Math.atan(t1 / t2);
          if (x2 < x1) {
            if (y2 < y1) theta = Math.PI + theta;
            else         theta = - (Math.PI + theta);
          } else if (x2 > x1 && y2 < y1)
            theta =  2*Math.PI - theta;
          double cosTheta = Math.cos(theta);
          double sinTheta = Math.sin(theta);

          // Create the other points and translate the arrow to the origin
          Point p2 = new Point(-arrow_size2,-arrow_size);
          Point p3 = new Point(-arrow_size2,+arrow_size);

          // Rotate the points (without using matrices!)
          int x = new Long(Math.round((cosTheta * p2.x) - (sinTheta * p2.y))).intValue();
          p2.y = new Long(Math.round((sinTheta * p2.x) + (cosTheta * p2.y))).intValue();
          p2.x = x;
          x = new Long(Math.round((cosTheta * p3.x) - (sinTheta * p3.y))).intValue();
          p3.y = new Long(Math.round((sinTheta * p3.x) + (cosTheta * p3.y))).intValue();
          p3.x = x;

          // Translate back to desired location and add to polygon
          p2.translate(x2,y2);
          p3.translate(x2,y2);
          drawLine(canvas,x2,y2,p2.x,p2.y,color);
          drawLine(canvas,x2,y2,p3.x,p3.y,color);
        }   
    }

    /*------------------------------------------------------------------*/
    /**
     * Draw a line between two points.
     * Bresenham's algorithm.
     *
     * @param canvas canvas where we are painting
     * @param x1 x- coordinate for first point
     * @param y1 y- coordinate for first point
     * @param x2 x- coordinate for second point
     * @param y2 y- coordinate for second point
     * @param color line color
     */
    static public void drawLine(double [][]canvas, int x1, int y1,
       int x2, int y2, double color) 
    {
       int temp;
       int dy_neg = 1;
       int dx_neg = 1;
       int switch_x_y = 0;
       int neg_slope = 0;
       int tempx, tempy;
       int dx = x2 - x1;
       if(dx == 0)
          if(y1 > y2) {
             for(int n = y2; n <= y1; n++) Point(canvas,n,x1,color);
             return;
          } else {
             for(int n = y1; n <= y2; n++) Point(canvas,n,x1,color);
             return;
          }

       int dy = y2 - y1;
       if(dy == 0)
          if(x1 > x2) {
             for(int n = x2; n <= x1; n++) Point(canvas,y1,n,color);
             return;
          } else {
             for(int n = x1; n <= x2; n++) Point(canvas,y1,n,color);
             return;
          }

       float m = (float) dy/dx;

       if(m > 1 || m < -1) {
          temp = x1;
          x1 = y1;
          y1 = temp;
          temp = x2;
          x2 = y2;
          y2 = temp;
          dx = x2 - x1;
          dy = y2 - y1;
          m = (float) dy/dx;
          switch_x_y = 1;
       }

       if(x1 > x2) {
          temp = x1;
          x1 = x2;
          x2 = temp;
          temp = y1;
          y1 = y2;
          y2 = temp;
          dx = x2 - x1;
          dy = y2 - y1;
          m = (float) dy/dx;
       }

       if(m < 0) {
          if(dy < 0) {
             dy_neg = -1;
             dx_neg = 1;
          } else {
             dy_neg = 1;
             dx_neg = -1;
          }
          neg_slope = 1;
       }

       int d = 2 * (dy * dy_neg) - (dx * dx_neg);
       int incrH = 2 * dy * dy_neg;
       int incrHV = 2 * ( (dy * dy_neg)  - (dx * dx_neg) );
       int x = x1;
       int y = y1;
       tempx = x;
       tempy = y;

       if(switch_x_y == 1) {
          temp = x;
          x = y;
          y = temp;
       }
       Point(canvas,y,x,color);
       x = tempx;
       y = tempy;

       while(x < x2) {
          if(d <= 0) {
             x++;
             d += incrH;
          } else {
             d += incrHV;
             x++;
             if(neg_slope == 0) y++;
             else               y--;
          }
          tempx = x;
          tempy = y;

          if (switch_x_y == 1) {
             temp = x;
             x = y;
             y = temp;
          }
          Point(canvas,y,x,color);
          x = tempx;
          y = tempy;
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Put the image from an ImageProcessor into a double array.
     *
     * @param ip input, origin of the image
     * @param image output, the image in a double array
     */
    static public void extractImage(final ImageProcessor ip, double image[]) 
    {
       int k=0;
       int height=ip.getHeight();
       int width =ip.getWidth ();
       if (ip instanceof ByteProcessor) {
          final byte[] pixels = (byte[])ip.getPixels();
          for (int y = 0; (y < height); y++)
             for (int x = 0; (x < width); x++, k++)
                image[k] = (double)(pixels[k] & 0xFF);
       } else if (ip instanceof ShortProcessor) {
          final short[] pixels = (short[])ip.getPixels();
          for (int y = 0; (y < height); y++)
             for (int x = 0; (x < width); x++, k++)
                if (pixels[k] < (short)0) image[k] = (double)pixels[k] + 65536.0F;
                else                      image[k] = (double)pixels[k];
       } else if (ip instanceof FloatProcessor) {
          final float[] pixels = (float[])ip.getPixels();
          for (int p = 0; p<height*width; p++)
             image[p]=pixels[p];
       }
    }
    /*------------------------------------------------------------------*/
    /**
     * Put the image from an ImageProcessor into a double[][].
     *
     * @param ip input, origin of the image
     * @param image output, the image in a double[][]
     */
    static public void extractImage(final ImageProcessor ip, double image[][]) 
    {
        int k=0;
        int height=ip.getHeight();
        int width =ip.getWidth ();
        if (ip instanceof ByteProcessor) {
          final byte[] pixels = (byte[])ip.getPixels();
          for (int y = 0; (y < height); y++)
             for (int x = 0; (x < width); x++, k++)
                image[y][x] = (double)(pixels[k] & 0xFF);
        } else if (ip instanceof ShortProcessor) {
          final short[] pixels = (short[])ip.getPixels();
          for (int y = 0; (y < height); y++)
             for (int x = 0; (x < width); x++, k++)
                if (pixels[k] < (short)0) image[y][x] = (double)pixels[k] + 65536.0F;
                else                      image[y][x] = (double)pixels[k];
        } else if (ip instanceof FloatProcessor) {
          final float[] pixels = (float[])ip.getPixels();
          for (int y = 0; (y < height); y++)
             for (int x = 0; (x < width); x++, k++)
                 image[y][x]=pixels[k];
        }
    }

    /*------------------------------------------------------------------*/
    /**
     * Load landmarks from file.
     *
     * @param filename landmarks file name
     * @param sourceStack stack of source related images
     * @param targetStack stack of target related images
     */
    static public void loadPoints(String filename,
       Stack sourceStack, Stack targetStack) 
    {
       Point sourcePoint;
       Point targetPoint;
       try {
          final FileReader fr = new FileReader(filename);
          final BufferedReader br = new BufferedReader(fr);
          String line;
          String index;
          String xSource;
          String ySource;
          String xTarget;
          String yTarget;
          int separatorIndex;
          int k = 1;
          if (!(line = br.readLine()).equals("Index\txSource\tySource\txTarget\tyTarget")) {
             fr.close();
             IJ.write("Line " + k + ": 'Index\txSource\tySource\txTarget\tyTarget'");
             return;
          }
          ++k;
          while ((line = br.readLine()) != null) {
             line = line.trim();
             separatorIndex = line.indexOf('\t');
             if (separatorIndex == -1) {
                fr.close();
                IJ.write("Line " + k
                   + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
                return;
             }
             index = line.substring(0, separatorIndex);
             index = index.trim();
             line = line.substring(separatorIndex);
             line = line.trim();
             separatorIndex = line.indexOf('\t');
             if (separatorIndex == -1) {
                fr.close();
                IJ.write("Line " + k
                   + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
                return;
             }
             xSource = line.substring(0, separatorIndex);
             xSource = xSource.trim();
             line = line.substring(separatorIndex);
             line = line.trim();
             separatorIndex = line.indexOf('\t');
             if (separatorIndex == -1) {
                fr.close();
                IJ.write("Line " + k
                   + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
                return;
             }
             ySource = line.substring(0, separatorIndex);
             ySource = ySource.trim();
             line = line.substring(separatorIndex);
             line = line.trim();
             separatorIndex = line.indexOf('\t');
             if (separatorIndex == -1) {
                fr.close();
                IJ.write("Line " + k
                   + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
                return;
             }
             xTarget = line.substring(0, separatorIndex);
             xTarget = xTarget.trim();
             yTarget = line.substring(separatorIndex);
             yTarget = yTarget.trim();
             sourcePoint = new Point(Integer.valueOf(xSource).intValue(),
                Integer.valueOf(ySource).intValue());
             sourceStack.push(sourcePoint);
             targetPoint = new Point(Integer.valueOf(xTarget).intValue(),
                Integer.valueOf(yTarget).intValue());
             targetStack.push(targetPoint);
          }
          fr.close();
       } catch (FileNotFoundException e) {
          IJ.error("File not found exception" + e);
          return;
       } catch (IOException e) {
          IJ.error("IOException exception" + e);
          return;
       } catch (NumberFormatException e) {
          IJ.error("Number format exception" + e);
          return;
       }
    }
    /*------------------------------------------------------------------*/
    /**
     * Load a transformation from a file.
     *
     * @param filename transformation file name
     * @param cx x- b-spline coefficients
     * @param cy y- b-spline coefficients
     */
    static public void loadTransformation(String filename,
       final double [][]cx, final double [][]cy) 
    {
       try {
          final FileReader fr = new FileReader(filename);
          final BufferedReader br = new BufferedReader(fr);
          String line;

          // Read number of intervals
          line = br.readLine();
          int lineN = 1;
          StringTokenizer st = new StringTokenizer(line,"=");
          if (st.countTokens()!=2) 
          {
             fr.close();
             IJ.write("Line "+lineN+"+: Cannot read number of intervals");
             return; 
          }
          st.nextToken();
          int intervals=Integer.valueOf(st.nextToken()).intValue();

          // Skip next 2 lines
          line = br.readLine();
          line = br.readLine();
          lineN+=2;

          // Read the cx coefficients
          for (int i= 0; i<intervals+3; i++) 
          {
              line = br.readLine(); lineN++;
              st=new StringTokenizer(line);
              if (st.countTokens()!=intervals+3) 
              {
                   fr.close();
                   IJ.write("Line "+lineN+": Cannot read enough coefficients");
                   return; 
              }
             for (int j=0; j<intervals+3; j++)
                cx[i][j]=Double.valueOf(st.nextToken()).doubleValue();
          }

          // Skip next 2 lines
          line = br.readLine();
          line = br.readLine();
          lineN+=2;

          // Read the cy coefficients
          for (int i= 0; i<intervals+3; i++) 
          {
              line = br.readLine(); lineN++;
              st = new StringTokenizer(line);
              if (st.countTokens()!=intervals+3) 
              {
                   fr.close();
                   IJ.write("Line "+lineN+": Cannot read enough coefficients");
                   return; 
              }
              for (int j=0; j<intervals+3; j++)
                 cy[i][j]=Double.valueOf(st.nextToken()).doubleValue();
          }
          fr.close();
       } catch (FileNotFoundException e) {
          IJ.error("File not found exception" + e);
          return;
       } catch (IOException e) {
          IJ.error("IOException exception" + e);
          return;
       } catch (NumberFormatException e) {
          IJ.error("Number format exception" + e);
          return;
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Load a raw transformation from a file.
     *
     * @param filename transformation file name
     * @param transformation_x x- transformation coordinates
     * @param transformation_y y- transformation coordinates
     */
    static public void loadRawTransformation(String filename,
       double [][]transformation_x, double [][]transformation_y) 
    {
        try
        {
            final FileReader fr = new FileReader(filename);
            final BufferedReader br = new BufferedReader(fr);
            String line;
            
            // Read width
            line = br.readLine();
            int lineN = 1;
            StringTokenizer st = new StringTokenizer(line,"=");
            if (st.countTokens() != 2) 
            {
               fr.close();
               IJ.write("Line "+lineN+"+: Cannot read transformation width");
               return; 
            }
            st.nextToken();
            int width = Integer.valueOf(st.nextToken()).intValue();

            // Read height
            line = br.readLine();
            lineN ++;
            st = new StringTokenizer(line,"=");
            if (st.countTokens() != 2) 
            {
               fr.close();
               IJ.write("Line " + lineN + "+: Cannot read transformation height");
               return; 
            }
            st.nextToken();
            int height = Integer.valueOf(st.nextToken()).intValue();

            // Skip next 2 lines
            line = br.readLine();
            line = br.readLine();
            lineN+=2;          
            
            // Read the X transformation coordinates
            for (int i= 0; i < height; i++) 
            {
                line = br.readLine(); lineN++;
                st = new StringTokenizer(line);
                if (st.countTokens() != width) 
                {
                     fr.close();
                     IJ.write("Line "+lineN+": Cannot read enough coordinates");
                     return; 
                }
               for (int j = 0; j < width; j++)
                  transformation_x[i][j]  = Double.valueOf(st.nextToken()).doubleValue();
            }

            // Skip next 2 lines
            line = br.readLine();
            line = br.readLine();
            lineN+=2;
            
            // Read the Y transformation coordinates
            for (int i= 0; i < height; i++) 
            {
                line = br.readLine(); lineN++;
                st = new StringTokenizer(line);
                if (st.countTokens() != width) 
                {
                     fr.close();
                     IJ.write("Line "+lineN+": Cannot read enough coordinates");
                     return; 
                }
                for (int j = 0; j < width; j++)
                  transformation_y[i][j]  = Double.valueOf(st.nextToken()).doubleValue();
            }
            fr.close();
        } 
        catch (FileNotFoundException e) 
        {
            IJ.error("File not found exception" + e);
            return;
        } 
        catch (IOException e) 
        {
          IJ.error("IOException exception" + e);
          return;
        } 
        catch (NumberFormatException e) 
        {
          IJ.error("Number format exception" + e);
          return;
        }
    }    

    /*------------------------------------------------------------------*/
    /**
    * Compose two elastic deformations into a raw deformation.
     *
     * @param targetImp target image representation
     * @param intervals intervals in the deformation
     * @param cx1 first transformation x- b-spline coefficients
     * @param cy1 first transformation y- b-spline coefficients
     * @param cx2 second transformation x- b-spline coefficients
     * @param cy2 second transformation y- b-spline coefficients
     * @param outputTransformation_x output transformation coordinates in x-axis
     * @param outputTransformation_y output transformation coordinates in y-axis
     */
    public static void composeElasticTransformations(
       ImagePlus targetImp,
       int intervals,
       double [][] cx1,
       double [][] cy1,
       double [][] cx2,
       double [][] cy2,
       double [][] outputTransformation_x,
       double [][] outputTransformation_y) 
    {
       // Ask for memory for the transformation
       int targetCurrentHeight = targetImp.getProcessor().getHeight();
       int targetCurrentWidth  = targetImp.getProcessor().getWidth ();              
       
       double [][] transformation_x_1 = new double [targetCurrentHeight][targetCurrentWidth];
       double [][] transformation_y_1 = new double [targetCurrentHeight][targetCurrentWidth];

       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim * cXdim;
       int twiceNk = 2 * Nk;    
          
       // We pass the coffiecients to a one-dimension array
       // Direct coefficients.
       double c1[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c1[n     ] = cx1[i][j];
               c1[n + Nk] = cy1[i][j];
           }

       // Compute the deformation
       // Set these coefficients to an interpolator
       bUnwarpJImageModel swx1 = new bUnwarpJImageModel(c1, cYdim, cXdim, 0); 
       bUnwarpJImageModel swy1 = new bUnwarpJImageModel(c1, cYdim, cXdim, Nk);  

       // Inverse coefficients.
       double c2[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c2[n     ] = cx2[i][j];
               c2[n + Nk] = cy2[i][j];
           }
       
       bUnwarpJImageModel swx2 = new bUnwarpJImageModel(c2, cYdim, cXdim, 0);  
       bUnwarpJImageModel swy2 = new bUnwarpJImageModel(c2, cYdim, cXdim, Nk); 

       
       swx1.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);
       swy1.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);

       swx2.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);
       swy2.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);

       // Compute the transformation mapping
       for (int v=0; v<targetCurrentHeight; v++) 
       {
          final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<targetCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;             
             
             swx1.prepareForInterpolation(tu, tv, false); 
             final double x2 = transformation_x_1[v][u] = swx1.interpolateI();

             swy1.prepareForInterpolation(tu, tv, false); 
             final double y2 = transformation_y_1[v][u] = swy1.interpolateI();
             
//             if(x2 >= 0 && x2 < targetCurrentWidth && y2 >= 0 && y2 < targetCurrentHeight)
//             {
                 final double tv2 = (double)(y2 * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;

                 final double tu2 = (double)(x2 * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

                 swx2.prepareForInterpolation(tu2, tv2, false); 
                 outputTransformation_x[v][u] = swx2.interpolateI();

                 swy2.prepareForInterpolation(tu2, tv2, false); 
                 outputTransformation_y[v][u] = swy2.interpolateI();
//             }
//             else
//             {
//                 outputTransformation_x[v][u] = -100;
//                 outputTransformation_y[v][u] = -100;
//             }
                       
          }
       }
       
    }  /* end method composeElasticTransformations */

    /*------------------------------------------------------------------*/
    /**
    * Compose a raw deformatiion and an elastic deformation into a raw deformation.
     *
     * @param targetImp target image representation
     * @param intervals intervals in the deformation
     * @param cx1 first transformation coordinates in x-axis
     * @param cy1 first transformation coordinates in y-axis
     * @param cx2 second transformation x- b-spline coefficients
     * @param cy2 second transformation y- b-spline coefficients
     * @param outputTransformation_x output transformation coordinates in x-axis
     * @param outputTransformation_y output transformation coordinates in y-axis
     */
    public static void composeRawElasticTransformations(
       ImagePlus targetImp,
       int intervals,
       double [][] transformation_x_1,
       double [][] transformation_y_1,
       double [][] cx2,
       double [][] cy2,
       double [][] outputTransformation_x,
       double [][] outputTransformation_y) 
    {
       // Ask for memory for the transformation
       int targetCurrentHeight = targetImp.getProcessor().getHeight();
       int targetCurrentWidth  = targetImp.getProcessor().getWidth ();              

       double [][] transformation_x_2 = new double [targetCurrentHeight][targetCurrentWidth];
       double [][] transformation_y_2 = new double [targetCurrentHeight][targetCurrentWidth];

       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim * cXdim;
       int twiceNk = 2 * Nk;    
          

       // Inverse coefficients.
       double c2[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c2[n     ] = cx2[i][j];
               c2[n + Nk] = cy2[i][j];
           }
       
       bUnwarpJImageModel swx2 = new bUnwarpJImageModel(c2, cYdim, cXdim, 0);  
       bUnwarpJImageModel swy2 = new bUnwarpJImageModel(c2, cYdim, cXdim, Nk);       

       swx2.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);
       swy2.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);

       // Compute the transformation mapping
       for (int v=0; v<targetCurrentHeight; v++) 
       {          
          for (int u = 0; u<targetCurrentWidth; u++) 
          {
             final double x2 = transformation_x_1[v][u];

             final double y2 = transformation_y_1[v][u];
             
//             if(x2 >= 0 && x2 < targetCurrentWidth && y2 >= 0 && y2 < targetCurrentHeight)
//             {
                 final double tv2 = (double)(y2 * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;

                 final double tu2 = (double)(x2 * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

                 swx2.prepareForInterpolation(tu2, tv2, false); 
                 outputTransformation_x[v][u] = swx2.interpolateI();

                 swy2.prepareForInterpolation(tu2, tv2, false); 
                 outputTransformation_y[v][u] = swy2.interpolateI();
//             }
//             else
//             {
//                 outputTransformation_x[v][u] = -100;                
//                 outputTransformation_y[v][u] = -100;
//             }
                       
          }
       }
       
    }  /* end method composeRawElasticTransformations */    
    
    /*------------------------------------------------------------------*/
    /**
     * Compose two elastic deformations into a raw deformation at pixel level.
     *
     * @param targetImp target image representation
     * @param intervals intervals in the deformation
     * @param cx1 first transformation x- b-spline coefficients
     * @param cy1 first transformation y- b-spline coefficients
     * @param cx2 second transformation x- b-spline coefficients
     * @param cy2 second transformation y- b-spline coefficients
     * @param outputTransformation_x output transformation coordinates in y-axis
     * @param outputTransformation_y output transformation coordinates in y-axis
     */
    public static void composeElasticTransformationsAtPixelLevel(
       ImagePlus targetImp,
       int intervals,
       double [][] cx1,
       double [][] cy1,
       double [][] cx2,
       double [][] cy2,
       double [][] outputTransformation_x,
       double [][] outputTransformation_y) 
    {
       // Ask for memory for the transformation
       int targetCurrentHeight = targetImp.getProcessor().getHeight();
       int targetCurrentWidth  = targetImp.getProcessor().getWidth ();
       
       double [][] transformation_x_1 = new double [targetCurrentHeight][targetCurrentWidth];
       double [][] transformation_y_1 = new double [targetCurrentHeight][targetCurrentWidth];

       double [][] transformation_x_2 = new double [targetCurrentHeight][targetCurrentWidth];
       double [][] transformation_y_2 = new double [targetCurrentHeight][targetCurrentWidth];

       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim * cXdim;
       int twiceNk = 2 * Nk;    
          
       // We pass the coffiecients to a one-dimension array
       // Direct coefficients.
       double c1[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c1[n     ] = cx1[i][j];
               c1[n + Nk] = cy1[i][j];
           }

       // Compute the deformation
       // Set these coefficients to an interpolator
       bUnwarpJImageModel swx1 = new bUnwarpJImageModel(c1, cYdim, cXdim, 0); 
       bUnwarpJImageModel swy1 = new bUnwarpJImageModel(c1, cYdim, cXdim, Nk);  

       // Inverse coefficients.
       double c2[] = new double[twiceNk];
       for(int n = 0, i = 0; i < cYdim; i++)
           for(int j = 0; j < cYdim; j++, n++)
           {
               c2[n     ] = cx2[i][j];
               c2[n + Nk] = cy2[i][j];
           }
       
       bUnwarpJImageModel swx2 = new bUnwarpJImageModel(c2, cYdim, cXdim, 0);  
       bUnwarpJImageModel swy2 = new bUnwarpJImageModel(c2, cYdim, cXdim, Nk); 

       
       swx1.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);
       swy1.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);

       swx2.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);
       swy2.precomputed_prepareForInterpolation(
          targetCurrentHeight, targetCurrentWidth, intervals);

       // Compute the first transformation mapping
       for (int v=0; v<targetCurrentHeight; v++) 
       {
          final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<targetCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

             swx1.prepareForInterpolation(tu, tv, false); 
             transformation_x_1[v][u] = swx1.interpolateI();

             swy1.prepareForInterpolation(tu, tv, false); 
             transformation_y_1[v][u] = swy1.interpolateI();
          }
       }
       
       // Compute the second transformation mapping
       for (int v=0; v<targetCurrentHeight; v++) 
       {
          final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<targetCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

             swx2.prepareForInterpolation(tu, tv, false); 
             transformation_x_2[v][u] = swx2.interpolateI();

             swy2.prepareForInterpolation(tu, tv, false); 
             transformation_y_2[v][u] = swy2.interpolateI();
          }
       }
       
       bUnwarpJMiscTools.composeRawTransformations(targetCurrentWidth, targetCurrentHeight, 
               transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2, 
               outputTransformation_x, outputTransformation_y);
    }        
    
    /*------------------------------------------------------------------*/
    /**
     * Compose two raw transformations
     *
     * @param width image width
     * @param height image height
     * @param transformation_x_1 first transformation coordinates in x-axis
     * @param transformation_y_1 first transformation coordinates in y-axis
     * @param transformation_x_2 second transformation coordinates in x-axis
     * @param transformation_y_2 second transformation coordinates in y-axis
     * @param outputTransformation_x output transformation coordinates in y-axis
     * @param outputTransformation_y output transformation coordinates in y-axis
     */
    public static void composeRawTransformations(
       int          width,
       int          height,
       double [][]  transformation_x_1,
       double [][]  transformation_y_1,
       double [][]  transformation_x_2,
       double [][]  transformation_y_2,
       double [][]  outputTransformation_x,
       double [][]  outputTransformation_y) 
    {        
        
        for (int i= 0; i < height; i++) 
            for (int j = 0; j < width; j++) 
            {
                // Second transformation.
                int x1 = (int) Math.round(transformation_x_2[i][j]);
                int y1 = (int) Math.round(transformation_y_2[i][j]);
                
                // First transformation.
                if(x1 >= 0 && x1 < width && y1 >= 0 && y1 < height)
                {
                    outputTransformation_x[i][j] = transformation_x_1[y1][x1];
                    outputTransformation_y[i][j] = transformation_y_1[y1][x1];
                }
                else
                {
                    outputTransformation_x[i][j] = x1;
                    outputTransformation_y[i][j] = y1;
                }
            }
         
    }    
    
    /*------------------------------------------------------------------*/
    /**
     * Save a raw transformation
     *
     * @param filename raw transformation file name
     * @param width image width
     * @param height image height
     * @param transformation_x transformation coordinates in x-axis
     * @param transformation_y transformation coordinates in y-axis
     */
    public static void saveRawTransformation(
       String       filename,
       int          width,
       int          height,
       double [][]  transformation_x,
       double [][]  transformation_y) 
    {
        if(filename == null || filename == "")
        {
            String path = "";
            String new_filename = "";

            final Frame f = new Frame();
            final FileDialog fd = new FileDialog(f, "Save Transformation", FileDialog.SAVE);
            fd.setFile(new_filename);
            fd.setVisible(true);
            path = fd.getDirectory();
            filename = fd.getFile();
            if ((path == null) || (filename == null)) return;
                 filename = path+filename;

        }

        // Save the file
        try 
        {
            final FileWriter fw = new FileWriter(filename);
            String aux;
            fw.write("Width=" + width +"\n");
            fw.write("Height=" + height +"\n\n");
            fw.write("X Trans -----------------------------------\n");
            for (int i= 0; i < height; i++) 
            {
                 for (int j = 0; j < width; j++) 
                 {
                    aux="" + transformation_x[i][j];
                    while (aux.length()<21) aux=" "+aux;
                    fw.write(aux+" ");
                 }
                 fw.write("\n");
            }
            fw.write("\n");
            fw.write("Y Trans -----------------------------------\n");
            for (int i= 0; i < height; i++) 
            {
                 for (int j = 0; j < width; j++) 
                 {
                    aux="" + transformation_y[i][j];
                    while (aux.length()<21) aux=" "+aux;
                    fw.write(aux+" ");
                 }
                 fw.write("\n");
            }
            fw.close();
        } 
        catch (IOException e) 
        {
              IJ.error("IOException exception" + e);
        } 
        catch (SecurityException e) 
        {
              IJ.error("Security exception" + e);
        }
    }     
    
    /*------------------------------------------------------------------*/
    /**
     * Read the number of intervals of a transformation from a file.
     *
     * @param filename transformation file name
     * @return number of intervals
     */
    static public int numberOfIntervalsOfTransformation(String filename) 
    {
       try {
          final FileReader fr = new FileReader(filename);
          final BufferedReader br = new BufferedReader(fr);
          String line;

          // Read number of intervals
          line = br.readLine();
          int lineN=1;
          StringTokenizer st=new StringTokenizer(line,"=");
          if (st.countTokens()!=2) {
             fr.close();
             IJ.write("Line "+lineN+"+: Cannot read number of intervals");
             return -1; 
          }
          st.nextToken();
          int intervals=Integer.valueOf(st.nextToken()).intValue();

          fr.close();
          return intervals;
       } catch (FileNotFoundException e) {
          IJ.error("File not found exception" + e);
          return -1;
       } catch (IOException e) {
          IJ.error("IOException exception" + e);
          return -1;
       } catch (NumberFormatException e) {
          IJ.error("Number format exception" + e);
          return -1;
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Plot a point in a canvas.
     *
     * @param canvas canvas where we are painting
     * @param x x- coordinate for the point
     * @param y y- coordinate for the point
     * @param color point color
     */
    static public void Point(double [][]canvas, int y, int x, double color) 
    {
       if (y<0 || y>=canvas.length)    return;
       if (x<0 || x>=canvas[0].length) return;
       canvas[y][x]=color;
    }

    /*------------------------------------------------------------------*/
    /**
     * Print a matrix in the command line.
     *
     * @param title matrix title
     * @param array matrix to be printed
     */
    public static void printMatrix(
       final String    title,
       final double [][]array) 
    {
        int Ydim=array.length;
        int Xdim=array[0].length;

        System.out.println(title);
        for (int i=0; i<Ydim; i++) {
            for (int j=0; j<Xdim; j++)
               System.out.print(array[i][j]+" ");
            System.out.println();
        }
    }

    /*------------------------------------------------------------------*/
    /**
     * Show an image in a new bUnwarpJ window.
     *
     * @param title image title
     * @param array image in a double array
     * @param Ydim image height
     * @param Xdim image width
     */
    public static void showImage(
       final String    title,
       final double []  array,
       final int       Ydim,
       final int       Xdim) 
    {
       final FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
       int ij=0;
       for (int i=0; i<Ydim; i++)
           for (int j=0; j<Xdim; j++, ij++)
              fp.putPixelValue(j,i,array[ij]); 
       fp.resetMinAndMax();
       final ImagePlus      ip=new ImagePlus(title, fp);
       final ImageWindow    iw=new ImageWindow(ip);
       ip.updateImage();
    }

    /*------------------------------------------------------------------*/
    /**
     * Show an image in a new bUnwarpJ window.
     *
     * @param title image title
     * @param array image in a double array
     */
    public static void showImage(
       final String    title,
       final double [][]array) 
    {
       int Ydim=array.length;
       int Xdim=array[0].length;

       final FloatProcessor fp=new FloatProcessor(Xdim,Ydim);
          for (int i=0; i<Ydim; i++)
              for (int j=0; j<Xdim; j++)
                 fp.putPixelValue(j,i,array[i][j]);
       fp.resetMinAndMax();
       final ImagePlus      ip=new ImagePlus(title, fp);
       final ImageWindow    iw=new ImageWindow(ip);
       ip.updateImage();
    }

} /* End of MiscTools class */

/*====================================================================
|   bUnwarpJPointAction
\===================================================================*/

/**
 * Class for point actions in the bUnwarpJ interface.
 */
class bUnwarpJPointAction extends ImageCanvas implements KeyListener, MouseListener,
      MouseMotionListener
{ /* begin class bUnwarpJPointAction */

    /*....................................................................
       Public variables
    ....................................................................*/

    public static final int ADD_CROSS    = 0;
    public static final int MOVE_CROSS   = 1;
    public static final int REMOVE_CROSS = 2;
    public static final int MASK         = 3;
    public static final int INVERTMASK   = 4;
    public static final int FILE         = 5;
    public static final int STOP         = 7;
    public static final int MAGNIFIER    = 11;

    /*....................................................................
       Private variables
    ....................................................................*/

    private ImagePlus                      mainImp;
    private ImagePlus                      secondaryImp;
    private bUnwarpJPointHandler mainPh;
    private bUnwarpJPointHandler secondaryPh;
    private bUnwarpJPointToolbar tb;
    private bUnwarpJDialog       dialog;
    private long                           mouseDownTime;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Method key pressed.
     * 
     * @param e key event
     */
    public void keyPressed (final KeyEvent e) 
    {
        if (tb.getCurrentTool()==MASK || tb.getCurrentTool()==INVERTMASK) return;
       final Point p = mainPh.getPoint();
       if (p == null) return;
       final int x = p.x;
       final int y = p.y;
       switch (e.getKeyCode()) {
          case KeyEvent.VK_DELETE:
          case KeyEvent.VK_BACK_SPACE:
             mainPh.removePoint();
             secondaryPh.removePoint();
             updateAndDraw();
             break;
          case KeyEvent.VK_DOWN:
             mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
                mainImp.getWindow().getCanvas().screenY(y
                + (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())));
             mainImp.setRoi(mainPh);
             break;
          case KeyEvent.VK_LEFT:
             mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x
                - (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())),
                mainImp.getWindow().getCanvas().screenY(y));
             mainImp.setRoi(mainPh);
             break;
          case KeyEvent.VK_RIGHT:
             mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x
                + (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())),
                mainImp.getWindow().getCanvas().screenY(y));
             mainImp.setRoi(mainPh);
             break;
          case KeyEvent.VK_TAB:
             mainPh.nextPoint();
             secondaryPh.nextPoint();
             updateAndDraw();
             break;
          case KeyEvent.VK_UP:
             mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
                mainImp.getWindow().getCanvas().screenY(y
                - (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())));
             mainImp.setRoi(mainPh);
             break;
       }
    } /* end keyPressed */

    /*------------------------------------------------------------------*/
    /**
     * Method key released.
     * 
     * @param e key event
     */
    public void keyReleased (final KeyEvent e){
    } /* end keyReleased */

    /*------------------------------------------------------------------*/
    /**
     * Method key typed.
     * 
     * @param e key event
     */
    public void keyTyped (final KeyEvent e) {
    } /* end keyTyped */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse clicked.
     * 
     * @param e mouse event
     */
    public void mouseClicked (final MouseEvent e) {
    } /* end mouseClicked */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse dragged, applied move the cross.
     * 
     * @param e mouse event
     */
    public void mouseDragged (final MouseEvent e) 
    {
       final int x = e.getX();
       final int y = e.getY();
       if (tb.getCurrentTool() == MOVE_CROSS) {
          mainPh.movePoint(x, y);
          updateAndDraw();
       }
       mouseMoved(e);
    } /* end mouseDragged */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse entered, applied to set the main window.
     * 
     * @param e mouse event
     */
    public void mouseEntered (final MouseEvent e) 
    {
       WindowManager.setCurrentWindow(mainImp.getWindow());
       mainImp.getWindow().toFront();
       updateAndDraw();
    } /* end mouseEntered */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse exited.
     * 
     * @param e mouse event
     */
    public void mouseExited (final MouseEvent e) 
    {
       IJ.showStatus("");
    } /* end mouseExited */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse moved, show the coordinates of the mouse pointer.
     * 
     * @param e mouse event
     */
    public void mouseMoved (final MouseEvent e) 
    {
       setControl();
       final int x = mainImp.getWindow().getCanvas().offScreenX(e.getX());
       final int y = mainImp.getWindow().getCanvas().offScreenY(e.getY());
       IJ.showStatus(mainImp.getLocationAsString(x, y) + getValueAsString(x, y));
    } /* end mouseMoved */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse pressed, allow all the different option over the images.
     * 
     * @param e mouse event
     */
    public void mousePressed (final MouseEvent e) 
    {
       if (dialog.isFinalActionLaunched()) return;
       int x = e.getX(),xp;
       int y = e.getY(),yp;
       int currentPoint;
       boolean doubleClick = (System.currentTimeMillis() - mouseDownTime) <= 250L;
       mouseDownTime = System.currentTimeMillis();
       switch (tb.getCurrentTool()) {
          case ADD_CROSS:
             xp=mainImp.getWindow().getCanvas().offScreenX(x);
             yp=mainImp.getWindow().getCanvas().offScreenY(y);
             mainPh.addPoint(xp, yp);

             xp = positionX(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenX(x));
             yp = positionY(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenY(y));
             secondaryPh.addPoint(xp, yp);

             updateAndDraw();
             break;
          case MOVE_CROSS:
             currentPoint = mainPh.findClosest(x, y);
             secondaryPh.setCurrentPoint(currentPoint);
             updateAndDraw();
             break;
          case REMOVE_CROSS:
             currentPoint = mainPh.findClosest(x, y);
             mainPh.removePoint(currentPoint);
             secondaryPh.removePoint(currentPoint);
             updateAndDraw();
             break;
          case MASK:
          case INVERTMASK:
              if (mainPh.canAddMaskPoints()) 
              {
                 if (!doubleClick) 
                 {
                    if (dialog.isClearMaskSet()) 
                    {
                       mainPh.clearMask();
                       dialog.setClearMask(false);
                       dialog.ungrayImage(this);
                    }
                    x = positionX(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenX(x));
                    y = positionY(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenY(y));
                    
                    mainPh.addMaskPoint(x, y);
                 } 
                 else 
                    mainPh.closeMask(tb.getCurrentTool());
                 updateAndDraw();
             } else {
                 IJ.error("A mask cannot be manually assigned since the mask was already in the stack");
             }
              break;
          case MAGNIFIER:
             final int flags = e.getModifiers();
             if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) {
                mainImp.getWindow().getCanvas().zoomOut(x, y);
             }
             else {
                mainImp.getWindow().getCanvas().zoomIn(x, y);
             }
             break;
       }
    } /* end mousePressed */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse released.
     * 
     * @param e mouse event
     */
    public void mouseReleased (final MouseEvent e) {
    } /* end mouseReleased */

    /*------------------------------------------------------------------*/
    /**
     * Set the secondary point handler.
     *
     * @param secondaryImp pointer to the secondary image
     * @param secondaryPh secondary point handler
     */
    public void setSecondaryPointHandler (
       final ImagePlus secondaryImp,
       final bUnwarpJPointHandler secondaryPh) 
    {
       this.secondaryImp = secondaryImp;
       this.secondaryPh = secondaryPh;
    } /* end setSecondaryPointHandler */

    /*------------------------------------------------------------------*/
    /**
     * Create an instance of bUnwarpJPointAction.
     */
    public bUnwarpJPointAction (
       final ImagePlus imp,
       final bUnwarpJPointHandler ph,
       final bUnwarpJPointToolbar tb,
       final bUnwarpJDialog       dialog) 
    {
       super(imp);
       this.mainImp = imp;
       this.mainPh = ph;
       this.tb = tb;
       this.dialog = dialog;
    } /* end bUnwarpJPointAction */

    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Get a pixel value as string.
     *
     * @param x x- coordinate of the pixel
     * @param y y- coordinate of the pixel
     * @return pixel value in string form
     */
    private String getValueAsString (
       final int x,
       final int y)
    {
       final Calibration cal = mainImp.getCalibration();
       final int[] v = mainImp.getPixel(x, y);
       final int mainImptype=mainImp.getType();
       if (mainImptype==mainImp.GRAY8 || mainImptype==mainImp.GRAY16) {
           final double cValue = cal.getCValue(v[0]);
           if (cValue==v[0]) {
              return(", value=" + v[0]);
           }
           else {
              return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
           }
       } else if (mainImptype==mainImp.GRAY32) {
                return(", value=" + Float.intBitsToFloat(v[0]));
       } else if (mainImptype==mainImp.COLOR_256) {
           return(", index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);
       } else if (mainImptype == mainImp.COLOR_RGB) {
           return(", value=" + v[0] + "," + v[1] + "," + v[2]);
       } else {
           return("");
       }
    } /* end getValueAsString */

    /*------------------------------------------------------------------*/
    /**
     * Get a common x- position between two images.
     *
     * @param imp1 first image
     * @param imp2 second image
     * @param x x-coordinate
     * @return common position
     */
    private int positionX (
       final ImagePlus imp1,
       final ImagePlus imp2,
       final int x) 
    {
       return((x * imp2.getWidth()) / imp1.getWidth());
    } /* end PositionX */

    /*------------------------------------------------------------------*/
    /**
     * Get a common y- position between two images.
     *
     * @param imp1 first image
     * @param imp2 second image
     * @param y y-coordinate
     * @return common position
     */
    private int positionY (
       final ImagePlus imp1,
       final ImagePlus imp2,
       final int y) 
    {
       return((y * imp2.getHeight()) / imp1.getHeight());
    } /* end PositionY */

    /*------------------------------------------------------------------*/
    /**
     * Set control.
     */
    private void setControl () 
    {
       switch (tb.getCurrentTool()) {
          case ADD_CROSS:
             mainImp.getWindow().getCanvas().setCursor(crosshairCursor);
             break;
          case FILE:
          case MAGNIFIER:
          case MOVE_CROSS:
          case REMOVE_CROSS:
          case MASK:
          case INVERTMASK:
          case STOP:
             mainImp.getWindow().getCanvas().setCursor(defaultCursor);
             break;
       }
    } /* end setControl */

    /*------------------------------------------------------------------*/
    /**
     * Update the region of interest of the main and scondary images.
     */
    private void updateAndDraw () 
    {
       mainImp.setRoi(mainPh);
       secondaryImp.setRoi(secondaryPh);
    } /* end updateAndDraw */

} /* end class bUnwarpJPointAction */

/*====================================================================
|   bUnwarpJPointHandler
\===================================================================*/

/**
 * Class to deal with point handler in bUnwarpJ.
 */
class bUnwarpJPointHandler extends Roi
{ /* begin class bUnwarpJPointHandler */

    /*....................................................................
       Private variables
    ....................................................................*/

    /** constant to keep half of the cross size */
    private static final int CROSS_HALFSIZE = 5;

    // Colors
    /** colors rank */
    private static final int GAMUT       = 1024;
    /** array of colors */
    private final Color   spectrum[]     = new Color[GAMUT];
    /** array with a flag for each color to determine if it is being used */
    private final boolean usedColor[]    = new boolean[GAMUT];
    /** list of colors */
    private final Vector  listColors     = new Vector(0, 16);
    /** current color */
    private int           currentColor   = 0;

    // List of crosses
    /** lsit of points */
    private final Vector  listPoints     = new Vector(0, 16);
    /** current point */
    private int           currentPoint   = -1;
    /** number of points */
    private int           numPoints      = 0;
    /** start flag */
    private boolean       started        = false;

    /** list of points in the mask */
    private final Vector  listMaskPoints = new Vector(0,16);
    /** flat to check if the mask is closed or not */
    private boolean       maskClosed     = false;

    // Some useful references
    /** pointer to the image representation */
    private ImagePlus                      imp;
    /** pointer to the point actions */
    private bUnwarpJPointAction  pa;
    /** pointer to the point toolbar */
    private bUnwarpJPointToolbar tb;
    /** pointer to the mask */
    private bUnwarpJMask         mask;
    /** pointer to the bUnwarpJ dialog */
    private bUnwarpJDialog       dialog;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Add a point to the mask.
     *
     * @param x x- point coordinate
     * @param y y- point coordinate
     */
    public void addMaskPoint (
       final int x,
       final int y) 
    {
       if (maskClosed) return;
       final Point p = new Point(x, y);
       listMaskPoints.addElement(p);
    }

    /*------------------------------------------------------------------*/
    /**
     * Add a point to the list of points.
     *
     * @param x x- point coordinate
     * @param y y- point coordinate
     */
    public void addPoint (
       final int x,
       final int y) 
    {
       if (numPoints < GAMUT) {
          final Point p = new Point(x, y);
          listPoints.addElement(p);
          if (!usedColor[currentColor]) {
             usedColor[currentColor] = true;
          }
          else {
             int k;
             for (k = 0; (k < GAMUT); k++) {
                currentColor++;
                currentColor &= GAMUT - 1;
                if (!usedColor[currentColor]) {
                   break;
                }
             }
             if (GAMUT <= k) {
                throw new IllegalStateException("Unexpected lack of available colors");
             }
          }
          int stirredColor = 0;
          int c = currentColor;
          for (int k = 0; (k < (int)Math.round(Math.log((double)GAMUT) / Math.log(2.0))); k++) {
             stirredColor <<= 1;
             stirredColor |= (c & 1);
             c >>= 1;
          }
          listColors.addElement(new Integer(stirredColor));
          currentColor++;
          currentColor &= GAMUT - 1;
          currentPoint = numPoints;
          numPoints++;
       }
       else {
          IJ.error("Maximum number of points reached");
       }
    } /* end addPoint */

    /*------------------------------------------------------------------*/
    /** 
     * Check if it is possible to add points to the mask.
     *
     * @return false if the image is coming from a stack 
     */
    public boolean canAddMaskPoints() 
    {
       return !mask.isFromStack();
    }

    /*------------------------------------------------------------------*/
    /**
     * Remove all the elements of the mask.
     */
    public void clearMask () 
    {
       // Clear mask information in this object
       listMaskPoints.removeAllElements();
       maskClosed=false;
       mask.clearMask();
    }

    /*------------------------------------------------------------------*/
    /**
     * Close mask.
     * 
     * @param tool option to invert or not the mask
     */
    public void closeMask (int tool) 
    {
       listMaskPoints.addElement(listMaskPoints.elementAt(0));
       maskClosed=true;
       mask.setMaskPoints(listMaskPoints);
       mask.fillMask(tool);
       dialog.grayImage(this);
    }

    /*------------------------------------------------------------------*/
    /**
     * Draw the mask.
     *
     * @param g graphic element
     */
    public void draw (final Graphics g)
    {
        // Draw landmarks
       if (started) {
          final double mag = (double)ic.getMagnification();
          final int dx = (int)(mag / 2.0);
          final int dy = (int)(mag / 2.0);
          for (int k = 0; (k < numPoints); k++) {
             final Point p = (Point)listPoints.elementAt(k);
             g.setColor(spectrum[((Integer)listColors.elementAt(k)).intValue()]);
             if (k == currentPoint) {
                if (WindowManager.getCurrentImage() == imp) {
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

       // Draw mask
       int numberMaskPoints=listMaskPoints.size();
       if (numberMaskPoints!=0) {
           final double mag = (double)ic.getMagnification();
           final int dx = (int)(mag / 2.0);
           final int dy = (int)(mag / 2.0);

           int CIRCLE_RADIUS=CROSS_HALFSIZE/2;
           int CIRCLE_DIAMETER=2*CIRCLE_RADIUS;
          for (int i=0; i<numberMaskPoints; i++) {
                  final Point p = (Point)listMaskPoints.elementAt(i);
                  g.setColor(Color.yellow);
                  g.drawOval(ic.screenX(p.x)-CIRCLE_RADIUS+dx, ic.screenY(p.y)-CIRCLE_RADIUS+dy,
                CIRCLE_DIAMETER, CIRCLE_DIAMETER);
                  if (i!=0) {
                Point previous_p=(Point)listMaskPoints.elementAt(i-1);
                g.drawLine(ic.screenX(p.x)+dx,ic.screenY(p.y)+dy,
                       ic.screenX(previous_p.x)+dx,ic.screenY(previous_p.y)+dy);
                  }
          }
       }
    } /* end draw */

    /*------------------------------------------------------------------*/
    /**
     * Find the the closest point to a certain point.
     *
     * @param x x- point coordinate
     * @param y y- point coordinate
     * @return point index in the list of points
     */
    public int findClosest (
       int x,
       int y) 
    {
       if (numPoints == 0) 
       {
          return(currentPoint);
       }

       x = ic.offScreenX(x);
       y = ic.offScreenY(y);

       Point p = new Point((Point)listPoints.elementAt(currentPoint));

       double distance = (double)(x - p.x) * (double)(x - p.x)
          + (double)(y - p.y) * (double)(y - p.y);

       for (int k = 0; (k < numPoints); k++) 
       {
          p = (Point)listPoints.elementAt(k);
          final double candidate = (double)(x - p.x) * (double)(x - p.x)
             + (double)(y - p.y) * (double)(y - p.y);
          if (candidate < distance) 
          {
             distance = candidate;
             currentPoint = k;
          }
       }
       return(currentPoint);
    } /* end findClosest */

    /*------------------------------------------------------------------*/
    /**
     * Get the current point in the list of points.
     *
     * @return current point
     */
    public Point getPoint () 
    {
       return((0 <= currentPoint) ? (Point)listPoints.elementAt(currentPoint) : (null));
    } /* end getPoint */

    /*------------------------------------------------------------------*/
    /**
     * Get point action.
     *
     * @return point action
     */
    public bUnwarpJPointAction getPointAction () {return pa;}

    /*------------------------------------------------------------------*/
    /**
     * Get current point index.
     *
     * @return index of current point
     */
    public int getCurrentPoint () 
    {
       return(currentPoint);
    } /* end getCurrentPoint */

    /*------------------------------------------------------------------*/
    /**
     * Get the list of points.
     *
     * @return list of points
     */
    public Vector getPoints () 
    {
       return(listPoints);
    } /* end getPoints */

    /*------------------------------------------------------------------*/
    /**
     * Kill listeners.
     */
    public void killListeners () 
    {
       final ImageWindow iw = imp.getWindow();
       final ImageCanvas ic = iw.getCanvas();
       ic.removeKeyListener(pa);
       ic.removeMouseListener(pa);
       ic.removeMouseMotionListener(pa);
       ic.addMouseMotionListener(ic);
       ic.addMouseListener(ic);
       ic.addKeyListener(IJ.getInstance());
    } /* end killListeners */

    /*------------------------------------------------------------------*/
    /**
     * Move the current point into a new position.
     *
     * @param x x-coordinate of the new position
     * @param y y-coordiante of the new position
     */
    public void movePoint (
       int x,
       int y) 
    {
       if (0 <= currentPoint) 
       {
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

    /*------------------------------------------------------------------*/
    /**
     * Increase the current point index one position in the list.
     */
    public void nextPoint () 
    {
       currentPoint = (currentPoint == (numPoints - 1)) ? (0) : (currentPoint + 1);
    } /* end nextPoint */

    /*------------------------------------------------------------------*/
    /**
     * Remove the current point.
     */
    public void removePoint () 
    {
       if (0 < numPoints) {
          listPoints.removeElementAt(currentPoint);
          usedColor[((Integer)listColors.elementAt(currentPoint)).intValue()] = false;
          listColors.removeElementAt(currentPoint);
          numPoints--;
       }
       currentPoint = numPoints - 1;
       if (currentPoint < 0) {
          tb.setTool(pa.ADD_CROSS);
       }
    } /* end removePoint */

    /*------------------------------------------------------------------*/
    /**
     * Remove one specific point.
     *
     * @param k index of the point to be removed
     */
    public void removePoint (final int k) 
    {
       if (0 < numPoints) {
          listPoints.removeElementAt(k);
          usedColor[((Integer)listColors.elementAt(k)).intValue()] = false;
          listColors.removeElementAt(k);
          numPoints--;
       }
       currentPoint = numPoints - 1;
       if (currentPoint < 0) {
          tb.setTool(pa.ADD_CROSS);
       }
    } /* end removePoint */

    /*------------------------------------------------------------------*/
    /**
     * Remove all points in the mask.
     */
    public void removePoints () 
    {
       listPoints.removeAllElements();
       listColors.removeAllElements();
       for (int k = 0; (k < GAMUT); k++) 
       {
          usedColor[k] = false;
       }
       currentColor = 0;
       numPoints = 0;
       currentPoint = -1;
       tb.setTool(pa.ADD_CROSS);
       imp.setRoi(this);
    } /* end removePoints */

    /*------------------------------------------------------------------*/
    /**
     * Set the current point.
     *
     * @param currentPoint new current point index
     */
    public void setCurrentPoint (final int currentPoint) 
    {
       this.currentPoint = currentPoint;
    } /* end setCurrentPoint */

    /*------------------------------------------------------------------*/
    /**
     * Set the set of test for the source.
     *
     * @param set number of source set
     */
    public void setTestSourceSet (final int set) 
    {
       removePoints();
       switch(set) {
          case 1: // Deformed_Lena 1
             addPoint(11,11);
             addPoint(200,6);
             addPoint(197,204);
             addPoint(121,111);
             break;
          case 2: // Deformed_Lena 1
             addPoint(6,6);
             addPoint(202,7);
             addPoint(196,210);
             addPoint(10,214);
             addPoint(120,112);
             addPoint(68,20);
             addPoint(63,163);
             addPoint(186,68);
             break;
       }
    } /* end setTestSourceSet */

    /*------------------------------------------------------------------*/
    /**
     * Set the set of test for the target.
     *
     * @param set number of target set
     */
    public void setTestTargetSet (final int set) 
    {
       removePoints();
       switch(set) {
          case 1:
             addPoint(11,11);
             addPoint(185,15);
             addPoint(154,200);
             addPoint(123,92);
             break;
          case 2: // Deformed_Lena 1
             addPoint(6,6);
             addPoint(185,14);
             addPoint(154,200);
             addPoint(3,178);
             addPoint(121,93);
             addPoint(67,14);
             addPoint(52,141);
             addPoint(178,68);
             break;
       }
    } /* end setTestTargetSet */

    /*------------------------------------------------------------------*/
    /**
     * Set the secondary point handler.
     *
     * @param secondaryImp pointer to the secondary image
     * @param secondaryPh secondary point handler
     */
    public void setSecondaryPointHandler (
       final ImagePlus secondaryImp,
       final bUnwarpJPointHandler secondaryPh)
    {
       pa.setSecondaryPointHandler(secondaryImp, secondaryPh);
    } /* end setSecondaryPointHandler */

    /*------------------------------------------------------------------*/
    /**
     * Constructor with graphical capabilities, create an instance of bUnwarpJPointHandler.
     * 
     * @param imp pointer to the image
     * @param tb pointer to the toolbar
     * @param mask pointer to the mask
     * @param dialog pointer to the bUnwarpJ dialog
     */
    public bUnwarpJPointHandler (
       final ImagePlus           imp,
       final bUnwarpJPointToolbar tb,
       final bUnwarpJMask         mask,
       final bUnwarpJDialog       dialog) 
    {
       super(0, 0, imp.getWidth(), imp.getHeight(), imp);
       this.imp = imp;
       this.tb = tb;
       this.dialog=dialog;
       pa = new bUnwarpJPointAction(imp, this, tb, dialog);
       final ImageWindow iw = imp.getWindow();
       final ImageCanvas ic = iw.getCanvas();
       iw.requestFocus();
       iw.removeKeyListener(IJ.getInstance());
       iw.addKeyListener(pa);
       ic.removeMouseMotionListener(ic);
       ic.removeMouseListener(ic);
       ic.removeKeyListener(IJ.getInstance());
       ic.addKeyListener(pa);
       ic.addMouseListener(pa);
       ic.addMouseMotionListener(pa);
       setSpectrum();
       started = true;

       this.mask=mask;
       clearMask();
    } /* end bUnwarpJPointHandler */

    /**
     * Constructor without graphical capabilities, create an instance of bUnwarpJPointHandler.
     *
     * @param imp image
     */
    public bUnwarpJPointHandler (final ImagePlus imp) 
    {
       super(0, 0, imp.getWidth(), imp.getHeight(), imp);
       this.imp = imp;
       tb = null;
       dialog=null;
       pa = null;
       started = true;
       mask=null;
    } /* end bUnwarpJPointHandler */

    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Set the spectrum of colors.
     */
    private void setSpectrum () 
    {
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
       int k = 0;
       do {
          spectrum[k] = new Color(1.0F, (float)k / gamutChunk1, 0.0F);
          usedColor[k] = false;
       } while (++k < bound1);
       do {
          spectrum[k] = new Color(1.0F - (float)(k - bound1) / gamutChunk2, 1.0F, 0.0F);
          usedColor[k] = false;
       } while (++k < bound2);
       do {
          spectrum[k] = new Color(0.0F, 1.0F, (float)(k - bound2) / gamutChunk3);
          usedColor[k] = false;
       } while (++k < bound3);
       do {
          spectrum[k] = new Color(0.0F, 1.0F - (float)(k - bound3) / gamutChunk4, 1.0F);
          usedColor[k] = false;
       } while (++k < bound4);
       do {
          spectrum[k] = new Color((float)(k - bound4) / gamutChunk5, 0.0F, 1.0F);
          usedColor[k] = false;
       } while (++k < bound5);
       do {
          spectrum[k] = new Color(1.0F, 0.0F, 1.0F - (float)(k - bound5) / gamutChunk6);
          usedColor[k] = false;
       } while (++k < bound6);
    } /* end setSpectrum */

} /* end class bUnwarpJPointHandler */

/*====================================================================
|   bUnwarpJPointToolbar
\===================================================================*/

/*------------------------------------------------------------------*/
/**
 * Class to deal with the point toolbar option in the bUnwarpJ interface.
 */
class bUnwarpJPointToolbar extends Canvas implements MouseListener
{ /* begin class bUnwarpJPointToolbar */

    /*....................................................................
       Private variables
    ....................................................................*/

    /** number of tools */
    private static final int NUM_TOOLS = 19;
    /** size of toolbar */
    private static final int SIZE      = 22;
    /** offset */
    private static final int OFFSET    = 3;

    /** grey color */
    private static final Color gray       = Color.lightGray;
    /** bright grey color */
    private static final Color brighter   = gray.brighter();
    /** dark grey color */
    private static final Color darker     = gray.darker();
    /** very dark grey color */
    private static final Color evenDarker = darker.darker();

    /** flags for every tool */
    private final boolean[] down = new boolean[NUM_TOOLS];
    /** graphic pointer */
    private Graphics g;
    /** source image pointer */
    private ImagePlus sourceImp;
    /** target image pointer */
    private ImagePlus targetImp;
    /** previous toolbar instance */
    private Toolbar previousInstance;
    /** source point handler */
    private bUnwarpJPointHandler sourcePh;
    /** target point handler */
    private bUnwarpJPointHandler targetPh;
    /** toolbar instance */
    private bUnwarpJPointToolbar instance;
    /** mouse down time */
    private long mouseDownTime;
    /** current tool */
    private int currentTool = bUnwarpJPointAction.ADD_CROSS;
    /** x- coordinate */
    private int x;
    /** y- coordinate */
    private int y;
    /** x- offset */
    private int xOffset;
    /** y- offset */
    private int yOffset;
    /** pointer to the bUnwarpJ dialog */
    private bUnwarpJDialog dialog;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Get current tool.
     */
    public int getCurrentTool () 
    {
       return(currentTool);
    } /* getCurrentTool */

    /*------------------------------------------------------------------*/
    /**
     * Mouse clicked.
     * 
     * @param e mouse event
     */
    public void mouseClicked (final MouseEvent e) {
    } /* end mouseClicked */

    /*------------------------------------------------------------------*/
    /**
     * Mouse entered.
     * 
     * @param e mouse event
     */
    public void mouseEntered (final MouseEvent e) {
    } /* end mouseEntered */

    /*------------------------------------------------------------------*/
    /**
     * Mouse exited.
     * 
     * @param e mouse event
     */
    public void mouseExited (final MouseEvent e) {
    } /* end mouseExited */

    /*------------------------------------------------------------------*/
    /**
     * Mouse pressed, applied to select the tool.
     * 
     * @param e mouse event
     */
    public void mousePressed (final MouseEvent e) 
    {
       final int x = e.getX();
       final int y = e.getY();
       int newTool = 0;
       for (int i = 0; (i < NUM_TOOLS); i++) {
          if (((i * SIZE) < x) && (x < (i * SIZE + SIZE))) {
             newTool = i;
          }
       }
       boolean doubleClick = ((newTool == getCurrentTool())
          && ((System.currentTimeMillis() - mouseDownTime) <= 500L)
          && (newTool == bUnwarpJPointAction.REMOVE_CROSS));
       mouseDownTime = System.currentTimeMillis();
       if (newTool==bUnwarpJPointAction.STOP && !dialog.isFinalActionLaunched())
          return;
       if (newTool!=bUnwarpJPointAction.STOP &&  dialog.isFinalActionLaunched())
          return;
       setTool(newTool);
       if (doubleClick) {
          bUnwarpJClearAll clearAllDialog = new bUnwarpJClearAll(IJ.getInstance(),
             sourceImp, targetImp, sourcePh, targetPh);
          GUI.center(clearAllDialog);
          clearAllDialog.setVisible(true);
          setTool(bUnwarpJPointAction.ADD_CROSS);
          clearAllDialog.dispose();
       }
       switch (newTool) {
          case bUnwarpJPointAction.FILE:
             bUnwarpJFile fileDialog = new bUnwarpJFile(IJ.getInstance(),
                sourceImp, targetImp, sourcePh, targetPh,dialog);
             GUI.center(fileDialog);
             fileDialog.setVisible(true);
             setTool(bUnwarpJPointAction.ADD_CROSS);
             fileDialog.dispose();
             break;
          case bUnwarpJPointAction.MASK:
          case bUnwarpJPointAction.INVERTMASK:
              dialog.setClearMask(true);
              break;
          case bUnwarpJPointAction.STOP:
              dialog.setStopRegistration();
              break;
       }
    } /* mousePressed */

    /*------------------------------------------------------------------*/
    /**
     * Mouse released.
     * 
     * @param e mouse event
     */
    public void mouseReleased (final MouseEvent e) {
    } /* end mouseReleased */

    /*------------------------------------------------------------------*/
    /**
     * Paint the buttons of the toolbar.
     *
     * @param g graphic pointer
     */
    public void paint (final Graphics g) 
    {
       for (int i = 0; (i < NUM_TOOLS); i++) {
          drawButton(g, i);
       }
    } /* paint */

    /*------------------------------------------------------------------*/
    /**
     * Restore the previous toolbar.
     */
    public void restorePreviousToolbar ()
    {
       final Container container = instance.getParent();
       final Component[] component = container.getComponents();
       for (int i = 0; (i < component.length); i++) {
          if (component[i] == instance) {
             container.remove(instance);
             container.add(previousInstance, i);
             container.validate();
             break;
          }
       }
    } /* end restorePreviousToolbar */

    /*------------------------------------------------------------------*/
    /**
     * Enable the tool buttons.
     */
    public void setAllUp () 
    {
       for (int i=0; i<NUM_TOOLS; i++) down[i]=false;
    }

    /*------------------------------------------------------------------*/
    /**
     * Set the source image.
     *
     * @param sourceImp pointer to the source image representation
     * @param sourcePh source point handler
     */
    public void setSource (
       final ImagePlus sourceImp,
       final bUnwarpJPointHandler sourcePh) 
    {
       this.sourceImp = sourceImp;
       this.sourcePh = sourcePh;
    } /* end setSource */

    /*------------------------------------------------------------------*/
    /**
     * Set the target image.
     *
     * @param targetImp pointer to the target image representation
     * @param targetPh target point handler
     */
    public void setTarget (
       final ImagePlus targetImp,
       final bUnwarpJPointHandler targetPh) 
    {
       this.targetImp = targetImp;
       this.targetPh = targetPh;
    } /* end setTarget */

    /*------------------------------------------------------------------*/
    /**
     * Set the tool.
     *
     * @param tool tool index
     */
    public void setTool (final int tool) 
    {
       if (tool == currentTool) {
          return;
       }
       down[tool] = true;
       down[currentTool] = false;
       Graphics g = this.getGraphics();
       drawButton(g, currentTool);
       drawButton(g, tool);
       g.dispose();
       showMessage(tool);
       currentTool = tool;
    } /* end setTool */

    /*------------------------------------------------------------------*/
    /**
     * Create an instance of bUnwarpJPointToolbar.
     *
     * @param previousToolbar pointer to the previous toolbar in order to be able
     *                        to restore it
     * @param dialog pointer to the bUnwarpJ interface dialog
     */
    public bUnwarpJPointToolbar (
       final Toolbar previousToolbar,
       final bUnwarpJDialog dialog) 
    {
       previousInstance = previousToolbar;
       this.dialog      = dialog;
       instance = this;
       final Container container = previousToolbar.getParent();
       final Component[] component = container.getComponents();
       for (int i = 0; (i < component.length); i++) {
          if (component[i] == previousToolbar) {
             container.remove(previousToolbar);
             container.add(this, i);
             break;
          }
       }
       resetButtons();
       down[currentTool] = true;
       setTool(currentTool);
       setForeground(evenDarker);
       setBackground(gray);
       addMouseListener(this);
       container.validate();
    } /* end bUnwarpJPointToolbar */

    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Draw a line from the current coordinates to a destination point.
     *
     * @param x x-coordinate of the destination point
     * @param y y-coordinate of the destination point
     */
    private void d (
       int x,
       int y) 
    {
       x += xOffset;
       y += yOffset;
       g.drawLine(this.x, this.y, x, y);
       this.x = x;
       this.y = y;
    } /* end d */

    /*------------------------------------------------------------------*/
    /**
     * Draw button in the toolbar.
     *
     * @param g graphic pointer
     * @param tool specific tool button
     */
    private void drawButton (
       final Graphics g,
       final int tool) 
    {
       fill3DRect(g, tool * SIZE + 1, 1, SIZE, SIZE - 1, !down[tool]);
       if (tool==bUnwarpJPointAction.STOP && !dialog.isFinalActionLaunched())
          return;
       if (tool!=bUnwarpJPointAction.STOP &&  dialog.isFinalActionLaunched())
          return;
       g.setColor(Color.black);
       int x = tool * SIZE + OFFSET;
       int y = OFFSET;
       if (down[tool]) 
       {
          x++;
          y++;
       }
       this.g = g;

        // Plygon for the mask
       int px[]=new int[5]; px[0]=x+4;px[1]=x+ 4;px[2]=x+14;px[3]=x+ 9;px[4]=x+14;
       int py[]=new int[5]; py[0]=y+3;py[1]=y+13;py[2]=y+13;py[3]=y+ 8;py[4]=y+ 3;

       switch (tool) 
       {
          case bUnwarpJPointAction.ADD_CROSS:
             xOffset = x;
             yOffset = y;
             m(7, 0);
             d(7, 1);
             m(6, 2);
             d(6, 3);
             m(8, 2);
             d(8, 3);
             m(5, 4);
             d(5, 5);
             m(9, 4);
             d(9, 5);
             m(4, 6);
             d(4, 8);
             m(10, 6);
             d(10, 8);
             m(5, 9);
             d(5, 14);
             m(9, 9);
             d(9, 14);
             m(7, 4);
             d(7, 6);
             m(7, 8);
             d(7, 8);
             m(4, 11);
             d(10, 11);
             g.fillRect(x + 6, y + 12, 3, 3);
             m(11, 13);
             d(15, 13);
             m(13, 11);
             d(13, 15);
             break;
          case bUnwarpJPointAction.FILE:
             xOffset = x;
             yOffset = y;
             m(3, 1);
             d(9, 1);
             d(9, 4);
             d(12, 4);
             d(12, 14);
             d(3, 14);
             d(3, 1);
             m(10, 2);
             d(11, 3);
             m(5, 4);
             d(7, 4);
             m(5, 6);
             d(10, 6);
             m(5, 8);
             d(10, 8);
             m(5, 10);
             d(10, 10);
             m(5, 12);
             d(10, 12);
             break;
          case bUnwarpJPointAction.MAGNIFIER:
             xOffset = x + 2;
             yOffset = y + 2;
             m(3, 0);
             d(3, 0);
             d(5, 0);
             d(8, 3);
             d(8, 5);
             d(7, 6);
             d(7, 7);
             d(6, 7);
             d(5, 8);
             d(3, 8);
             d(0, 5);
             d(0, 3);
             d(3, 0);
             m(8, 8);
             d(9, 8);
             d(13, 12);
             d(13, 13);
             d(12, 13);
             d(8, 9);
             d(8, 8);
             break;
          case bUnwarpJPointAction.MOVE_CROSS:
             xOffset = x;
             yOffset = y;
             m(1, 1);
             d(1, 10);
             m(2, 2);
             d(2, 9);
             m(3, 3);
             d(3, 8);
             m(4, 4);
             d(4, 7);
             m(5, 5);
             d(5, 7);
             m(6, 6);
             d(6, 7);
             m(7, 7);
             d(7, 7);
             m(11, 5);
             d(11, 6);
             m(10, 7);
             d(10, 8);
             m(12, 7);
             d(12, 8);
             m(9, 9);
             d(9, 11);
             m(13, 9);
             d(13, 11);
             m(10, 12);
             d(10, 15);
             m(12, 12);
             d(12, 15);
             m(11, 9);
             d(11, 10);
             m(11, 13);
             d(11, 15);
             m(9, 13);
             d(13, 13);
             break;
          case bUnwarpJPointAction.REMOVE_CROSS:
             xOffset = x;
             yOffset = y;
             m(7, 0);
             d(7, 1);
             m(6, 2);
             d(6, 3);
             m(8, 2);
             d(8, 3);
             m(5, 4);
             d(5, 5);
             m(9, 4);
             d(9, 5);
             m(4, 6);
             d(4, 8);
             m(10, 6);
             d(10, 8);
             m(5, 9);
             d(5, 14);
             m(9, 9);
             d(9, 14);
             m(7, 4);
             d(7, 6);
             m(7, 8);
             d(7, 8);
             m(4, 11);
             d(10, 11);
             g.fillRect(x + 6, y + 12, 3, 3);
             m(11, 13);
             d(15, 13);
             break;
          case bUnwarpJPointAction.MASK:
             xOffset = x;
             yOffset = y;
             g.fillPolygon(px, py, 5);
             break;
          case bUnwarpJPointAction.INVERTMASK:
             xOffset = x;
             yOffset = y;
             g.fillRect(x + 1, y + 1, 15, 15);
                g.setColor(gray);
             g.fillPolygon(px,py,5);
             g.setColor(Color.black);
             break;
          case bUnwarpJPointAction.STOP:
             xOffset = x;
             yOffset = y;
                // Octogon
             m( 1,  5);
             d( 1, 11);
             d( 5, 15);
             d(11, 15);
             d(15, 11);
             d(15,  5);
             d(11,  1);
             d( 5,  1);
             d( 1,  5);
             // S
             m( 5,  6);
             d( 3,  6);
             d( 3,  8);
             d( 5,  8);
             d( 5, 10);
             d( 3, 10);
             // T
             m( 6,  6);
             d( 6,  8);
             m( 7,  6);
             d( 7, 10);
             // O
             m(11,  6);
             d( 9,  6);
             d( 9, 10);
             d(11, 10);
             d(11,  6);
             // P
             m(12, 10);
             d(12,  6);
             d(14,  6);
             d(14,  8);
             d(12,  8);
             break;
       }
    } /* end drawButton */

    /*------------------------------------------------------------------*/
    /**
     * Fill a 3D rect.
     * 
     * @param g graphic pointer
     * @param x x-coordinate
     * @param y y-coordinate
     * @param width rect width
     * @param height rect height
     * @param raised color flag
     */
    private void fill3DRect (
       final Graphics g,
       final int x,
       final int y,
       final int width,
       final int height,
       final boolean raised) 
    {
       if (raised) {
          g.setColor(gray);
       }
       else {
          g.setColor(darker);
       }
       g.fillRect(x + 1, y + 1, width - 2, height - 2);
       g.setColor((raised) ? (brighter) : (evenDarker));
       g.drawLine(x, y, x, y + height - 1);
       g.drawLine(x + 1, y, x + width - 2, y);
       g.setColor((raised) ? (evenDarker) : (brighter));
       g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
       g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
    } /* end fill3DRect */

    /*------------------------------------------------------------------*/
    /**
     * Add the offset to the current coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     */
    private void m (
       final int x,
       final int y) 
    {
       this.x = xOffset + x;
       this.y = yOffset + y;
    } /* end m */

    /*------------------------------------------------------------------*/
    /**
     * Reset tool buttons.
     */ 
    private void resetButtons () 
    {
       for (int i = 0; (i < NUM_TOOLS); i++) {
          down[i] = false;
       }
    } /* end resetButtons */

    /*------------------------------------------------------------------*/
    /**
     * Show a message for the corresponding tool.
     *
     * @param tool tool identifier
     */
    private void showMessage (final int tool) 
    {
       switch (tool) {
          case bUnwarpJPointAction.ADD_CROSS:
             IJ.showStatus("Add crosses");
             return;
          case bUnwarpJPointAction.FILE:
             IJ.showStatus("Input/Output menu");
             return;
          case bUnwarpJPointAction.MAGNIFIER:
             IJ.showStatus("Magnifying glass");
             return;
          case bUnwarpJPointAction.MOVE_CROSS:
             IJ.showStatus("Move crosses");
             return;
          case bUnwarpJPointAction.REMOVE_CROSS:
             IJ.showStatus("Remove crosses");
             return;
          case bUnwarpJPointAction.MASK:
             IJ.showStatus("Draw a mask");
             return;
          case bUnwarpJPointAction.STOP:
             IJ.showStatus("Stop registration");
             return;
          default:
             IJ.showStatus("Undefined operation");
             return;
       }
    } /* end showMessage */

} /* end class bUnwarpJPointToolbar */

/*====================================================================
|   bUnwarpJProgressBar
\===================================================================*/

/**
 * This class implements the interactions when dealing with ImageJ's
 * progress bar.
 */
class bUnwarpJProgressBar
{ /* begin class bUnwarpJProgressBar */

    /*....................................................................
       Private variables
    ....................................................................*/

    /**
     * Same time constant than in ImageJ version 1.22.
     */
    private static final long TIME_QUANTUM = 50L;

    private static volatile long lastTime = System.currentTimeMillis();
    private static volatile int completed = 0;
    private static volatile int workload = 0;

    /*....................................................................
       Public methods
    ....................................................................*/

    /**
     * Extend the amount of work to perform by <code>batch</code>.
     *
     * @param batch Additional amount of work that need be performed.
     */
    public static synchronized void addWorkload (final int batch) 
    {
       workload += batch;
    } /* end addWorkload */

    /**
     * Erase the progress bar and cancel pending operations.
     */
    public static synchronized void resetProgressBar () 
    {
       final long timeStamp = System.currentTimeMillis();
       if ((timeStamp - lastTime) < TIME_QUANTUM) {
          try {
             Thread.sleep(TIME_QUANTUM - timeStamp + lastTime);
          } catch (InterruptedException e) {
             IJ.error("Unexpected interruption exception" + e);
          }
       }
       lastTime = timeStamp;
       completed = 0;
       workload = 0;
       IJ.showProgress(1.0);
    } /* end resetProgressBar */

    /**
     * Perform <code>stride</code> operations at once.
     *
     * @param stride Amount of work that is skipped.
     */
    public static synchronized void skipProgressBar (final int stride) 
    {
       completed += stride - 1;
       stepProgressBar();
    } /* end skipProgressBar */

    /**
     * Perform <code>1</code> operation unit.
     */
    public static synchronized void stepProgressBar () 
    {
       final long timeStamp = System.currentTimeMillis();
       completed = completed + 1;
       if ((TIME_QUANTUM <= (timeStamp - lastTime)) | (completed == workload)) {
          lastTime = timeStamp;
          IJ.showProgress((double)completed / (double)workload);
       }
    } /* end stepProgressBar */

    /**
     * Acknowledge that <code>batch</code> work has been performed.
     *
     * @param batch Completed amount of work.
     */
    public static synchronized void workloadDone (final int batch) 
    {
       workload -= batch;
       completed -= batch;
    } /* end workloadDone */

} /* end class bUnwarpJProgressBar */

/*====================================================================
|   bUnwarpJTransformation
\===================================================================*/

/**
 * Class to perform the transformation for bUnwarpJ.
 */
class bUnwarpJTransformation
{ /* begin class bUnwarpJTransformation */

    /*....................................................................
       Private variables
    ....................................................................*/

    /** float epsilon */
    private final double FLT_EPSILON = (double)Float.intBitsToFloat((int)0x33FFFFFF);
    /** pyramid flag */
    private final boolean PYRAMID  = true;
    /** original flag */
    private final boolean ORIGINAL = false;
    /** degree of the b-splines involved in the transformation */
    private final int transformationSplineDegree=3;

    // Some useful references
    /** reference to the first output image */
    private ImagePlus           output_ip_1;
    /** reference to the second output image */
    private ImagePlus           output_ip_2;
    /** pointer to the dialog of the bUnwarpJ interface */
    private bUnwarpJDialog       dialog;

    // Images
    /** pointer to the source image representation */
    private ImagePlus           sourceImp;
    /** pointer to the target image representation */
    private ImagePlus           targetImp;
    /** pointer to the source image model */
    private bUnwarpJImageModel   source;
    /** pointer to the target image model */
    private bUnwarpJImageModel   target;

    // Landmarks
    /** pointer to the source point handler */
    private bUnwarpJPointHandler sourcePh;
    /** pointer to the target point handler */
    private bUnwarpJPointHandler targetPh;

    // Masks for the images
    /** pointer to the source mask */
    private bUnwarpJMask sourceMsk;
    /** pointer to the target mask */
    private bUnwarpJMask targetMsk;

    // Image size
    /** source image height */
    private int     sourceHeight;
    /** source image width */
    private int     sourceWidth;
    /** target image height */
    private int     targetHeight;
    /** source image width */
    private int     targetWidth;
    /** target image current height */
    private int     targetCurrentHeight;
    /** target image current width */
    private int     targetCurrentWidth;
    /** source image current height */
    private int     sourceCurrentHeight;
    /** source image current width */
    private int     sourceCurrentWidth;
    /** height factor in the target image*/
    private double  targetFactorHeight;
    /** width factor in the target image*/
    private double  targetFactorWidth;
    /** height factor in the source image*/
    private double  sourceFactorHeight;
    /** width factor in the source image*/
    private double  sourceFactorWidth;

    // Display variables
    /** direct similarity error for the current iteration */
    private double partialDirectSimilarityError;
    /** direct consistency error for the current iteration */
    private double partialDirectConsitencyError;
    /** direct similarity error at the end of the registration */
    private double finalDirectSimilarityError;
    /** direct consistency error at the end of the registration */
    private double finalDirectConsistencyError;
    /** inverse similarity error for the current iteration */
    private double partialInverseSimilarityError;
    /** inverse consistency error for the current iteration */
    private double partialInverseConsitencyError;
    /** inverse similarity error at the end of the registration */
    private double finalInverseSimilarityError;
    /** inverse consistency error at the end of the registration */
    private double finalInverseConsistencyError;
    
    // Transformation parameters
    /** minimum scale deformation */
    private int     min_scale_deformation;
    /** maximum scale deformation */
    private int     max_scale_deformation;
    /** minimum scale image */
    private int     min_scale_image;
    /** flag to specify the level of resolution in the output */
    private int     outputLevel;
    /** flag to show the optimizer */ 
    private boolean showMarquardtOptim;
    /** divergency weight */
    private double  divWeight;
    /** curl weight */
    private double  curlWeight;
    /** landmark weight */
    private double  landmarkWeight;
    /** weight for image similarity */ 
    private double  imageWeight;
    /** weight for the deformations consistency */
    private double  consistencyWeight;
    /** stopping threshold  */
    private double  stopThreshold;
    /** level of accuracy */
    private int     accurate_mode;
    /** flag to save the transformation */
    private boolean saveTransf;
    /** direct transformation file name */
    private String  fn_tnf_1;
    /** inverse transformation file name */
    private String  fn_tnf_2;



    // Transformation estimate
    /** number of intervals to place b-spline coefficients */
    private int     intervals;
    /** x- b-spline coefficients keeping the transformation from source to target */
    private double  [][]cxSourceToTarget;
    /** y- b-spline coefficients keeping the transformation from source to target */
    private double  [][]cySourceToTarget;
    /** x- b-spline coefficients keeping the transformation from target to source */
    private double  [][]cxTargetToSource;
    /** y- b-spline coefficients keeping the transformation from target to source */
    private double  [][]cyTargetToSource;

    /** x- transformation matrix */
    private double  [][]transformation_x;
    /** y- transformation matrix */
    private double  [][]transformation_y;

    /** image model to interpolate the cxSourceToTarget coefficients */
    private bUnwarpJImageModel swxSourceToTarget;
    /** image model to interpolate the cySourceToTarget coefficients */
    private bUnwarpJImageModel swySourceToTarget;
    /** image model to interpolate the cxTargetToSource coefficients */
    private bUnwarpJImageModel swxTargetToSource;
    /** image model to interpolate the cyTargetToSource coefficients */
    private bUnwarpJImageModel swyTargetToSource;

    // Regularization temporary variables
    /** regularization P11 (source to target) matrix */
    private double  [][]P11_SourceToTarget;
    /** regularization P22 (source to target) matrix */
    private double  [][]P22_SourceToTarget;
    /** regularization P12 (source to target) matrix */
    private double  [][]P12_SourceToTarget;

    /** regularization P11 (target to source) matrix */
    private double  [][]P11_TargetToSource;
    /** regularization P22 (target to source) matrix */
    private double  [][]P22_TargetToSource;
    /** regularization P12 (target to source) matrix */
    private double  [][]P12_TargetToSource;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Registration method.
     */
    public void doRegistration () 
    {    
       
        // This function can only be applied with splines of an odd order

        // Bring into consideration the image/coefficients at the smallest scale
        source.popFromPyramid();
        target.popFromPyramid();

        targetCurrentHeight = target.getCurrentHeight();
        targetCurrentWidth  = target.getCurrentWidth();
        
        targetFactorHeight  = target.getFactorHeight();
        targetFactorWidth   = target.getFactorWidth();

        sourceCurrentHeight = source.getCurrentHeight();
        sourceCurrentWidth  = source.getCurrentWidth();

        sourceFactorHeight  = source.getFactorHeight();
        sourceFactorWidth   = source.getFactorWidth();

        // Ask memory for the transformation coefficients
        intervals = (int)Math.pow(2, min_scale_deformation);

        cxTargetToSource = new double[intervals+3][intervals+3];
        cyTargetToSource = new double[intervals+3][intervals+3];    

        // Build matrices for computing the regularization
        buildRegularizationTemporary(intervals, false);
        buildRegularizationTemporary(intervals, true);

        // Ask for memory for the residues
        final int K;
        if (targetPh!=null) K = targetPh.getPoints().size();
        else                K = 0;
        double [] dxTargetToSource = new double[K];
        double [] dyTargetToSource = new double[K];
        computeInitialResidues(dxTargetToSource, dyTargetToSource, false);

        // Compute the affine transformation FROM THE TARGET TO THE SOURCE coordinates
        // Notice that this matrix is independent of the scale, but the residues are not
        double[][] affineMatrix = null;
        if (landmarkWeight==0) affineMatrix = computeAffineMatrix(false);
        else 
        {
           affineMatrix = new double[2][3];
           affineMatrix[0][0] = affineMatrix[1][1]=1;
           affineMatrix[0][1] = affineMatrix[0][2]=0;
           affineMatrix[1][0] = affineMatrix[1][2]=0;
        }

        // Incorporate the affine transformation into the spline coefficient
        for (int i= 0; i<intervals + 3; i++) 
        {
           final double v = (double)((i - 1) * (targetCurrentHeight - 1)) / (double)intervals;
           final double xv = affineMatrix[0][2] + affineMatrix[0][1] * v;
           final double yv = affineMatrix[1][2] + affineMatrix[1][1] * v;
           for (int j = 0; j < intervals + 3; j++) 
           {
              final double u = (double)((j - 1) * (targetCurrentWidth - 1)) / (double)intervals;
              cxTargetToSource[i][j] = xv + affineMatrix[0][0] * u;
              cyTargetToSource[i][j] = yv + affineMatrix[1][0] * u;
           }
        }

        // Compute the affine transformation FROM THE SOURCE TO THE TARGET coordinates
        // Notice again that this matrix is independent of the scale, but the residues are not    
        // Ask for memory for the residues
        final int K2;
        if (sourcePh!=null) K2 = sourcePh.getPoints().size();
        else                K2 = 0;
        double [] dxSourceToTarget = new double[K2];
        double [] dySourceToTarget = new double[K2];
        computeInitialResidues(dxSourceToTarget, dySourceToTarget, true);

        cxSourceToTarget = new double[intervals+3][intervals+3];
        cySourceToTarget = new double[intervals+3][intervals+3];

        if (landmarkWeight==0) affineMatrix = computeAffineMatrix(true);
        else 
        {
           affineMatrix = new double[2][3];
           affineMatrix[0][0] = affineMatrix[1][1]=1;
           affineMatrix[0][1] = affineMatrix[0][2]=0;
           affineMatrix[1][0] = affineMatrix[1][2]=0;
        }

        // Incorporate the affine transformation into the spline coefficient    
        for (int i= 0; i<intervals + 3; i++) 
        {
           final double v = (double)((i - 1) * (sourceCurrentHeight - 1)) / (double)intervals;
           final double xv = affineMatrix[0][2] + affineMatrix[0][1] * v;
           final double yv = affineMatrix[1][2] + affineMatrix[1][1] * v;
           for (int j = 0; j < intervals + 3; j++) 
           {
              final double u = (double)((j - 1) * (sourceCurrentWidth - 1)) / (double)intervals;
              cxSourceToTarget[i][j] = xv + affineMatrix[0][0] * u;
              cySourceToTarget[i][j] = yv + affineMatrix[1][0] * u;
           }
        }
        
        // Now refine with the different scales
        int state;   // state=-1 --> Finish
                     // state= 0 --> Increase deformation detail
                     // state= 1 --> Increase image detail
                     // state= 2 --> Do nothing until the finest image scale
        if (min_scale_deformation==max_scale_deformation) state=1;
        else                                              state=0;
        int s = min_scale_deformation;
        int step = 0;
        computeTotalWorkload();

        while (state != -1) 
        {
            /*
            bUnwarpJMiscTools.showImage("CURRENT SOURCE", source.getCurrentImage(), source.getCurrentHeight(), source.getCurrentWidth());
            bUnwarpJMiscTools.showImage("CURRENT TARGET", target.getCurrentImage(), target.getCurrentHeight(), target.getCurrentWidth());
        */
            int currentDepth = target.getCurrentDepth();

            // Update the deformation coefficients only in states 0 and 1
            if (state==0 || state==1) 
            {
               // Update the deformation coefficients with the error of the landmarks
               // The following conditional is now useless but it is there to allow
               // easy changes like applying the landmarks only in the coarsest deformation
               if (s>=min_scale_deformation) 
               {
                // Number of intervals at this scale and ask for memory
                intervals = (int) Math.pow(2,s);
                final double[][] newcxTargetToSource = new double[intervals+3][intervals+3];
                final double[][] newcyTargetToSource = new double[intervals+3][intervals+3];

                final double[][] newcxSourceToTarget = new double[intervals+3][intervals+3];
                final double[][] newcySourceToTarget = new double[intervals+3][intervals+3];

                // Compute the residues before correcting at this scale
                computeScaleResidues(intervals, cxTargetToSource, cyTargetToSource, dxTargetToSource, dyTargetToSource, false);
                computeScaleResidues(intervals, cxSourceToTarget, cySourceToTarget, dxSourceToTarget, dySourceToTarget, true);

                // Compute the coefficients at this scale
                boolean underconstrained = true;
                // FROM TARGET TO SOURCE.
                if (divWeight==0 && curlWeight==0)
                   underconstrained=
                      computeCoefficientsScale(intervals, dxTargetToSource, dyTargetToSource, newcxTargetToSource, newcyTargetToSource, false);
                else
                   underconstrained=
                      computeCoefficientsScaleWithRegularization(
                         intervals, dxTargetToSource, dyTargetToSource, newcxTargetToSource, newcyTargetToSource, false);

                // Incorporate information from the previous scale
                if (!underconstrained || (step==0 && landmarkWeight!=0)) 
                {
                     for (int i=0; i<intervals+3; i++)
                         for (int j=0; j<intervals+3; j++) {
                            cxTargetToSource[i][j]+=newcxTargetToSource[i][j];
                            cyTargetToSource[i][j]+=newcyTargetToSource[i][j];
                      }           
                }

                // FROM SOURCE TO TARGET.
                underconstrained = true;
                if (divWeight==0 && curlWeight==0)
                   underconstrained=
                      computeCoefficientsScale(intervals, dxSourceToTarget, dySourceToTarget, newcxSourceToTarget, newcySourceToTarget, true);
                else
                   underconstrained=
                      computeCoefficientsScaleWithRegularization(
                         intervals, dxSourceToTarget, dySourceToTarget, newcxSourceToTarget, newcySourceToTarget, true);

                // Incorporate information from the previous scale
                if (!underconstrained || (step==0 && landmarkWeight!=0)) 
                {
                     for (int i=0; i<intervals+3; i++)
                         for (int j=0; j<intervals+3; j++) 
                         {
                            cxSourceToTarget[i][j]+= newcxSourceToTarget[i][j];
                            cySourceToTarget[i][j]+= newcySourceToTarget[i][j];
                         }
                }
              }

               // Optimize deformation coefficients
               if (imageWeight!=0)
                  optimizeCoeffs(intervals, stopThreshold, cxTargetToSource, cyTargetToSource, cxSourceToTarget, cySourceToTarget);
           }

            // Prepare for next iteration
            step++;
            switch (state) 
            {
               case 0:
                  // Finer details in the deformation
                  if (s<max_scale_deformation) 
                  {
                    cxTargetToSource = propagateCoeffsToNextLevel(intervals, cxTargetToSource, 1);
                    cyTargetToSource = propagateCoeffsToNextLevel(intervals, cyTargetToSource, 1);
                    cxSourceToTarget = propagateCoeffsToNextLevel(intervals, cxSourceToTarget, 1);
                    cySourceToTarget = propagateCoeffsToNextLevel(intervals, cySourceToTarget, 1);
                    s++;
                    intervals*=2;

                    // Prepare matrices for the regularization term
                    buildRegularizationTemporary(intervals, false);
                    buildRegularizationTemporary(intervals, true);

                    if (currentDepth>min_scale_image) state=1;
                    else                              state=0;
                 } else
                    if (currentDepth>min_scale_image) state=1;
                    else                              state=2;
                 break;
               case 1: // Finer details in the image, go on  optimizing
               case 2: // Finer details in the image, do not optimize
                  // Compute next state
                  if (state==1) {
                     if      (s==max_scale_deformation && currentDepth==min_scale_image) state=2;
                     else if (s==max_scale_deformation)                                  state=1;
                     else                                                                state=0;
                  } else if (state==2) {
                     if (currentDepth==0) state=-1;
                     else                 state= 2;
                  }

                  // Pop another image and prepare the deformation
                  if (currentDepth!=0) 
                  {
                     double oldTargetCurrentHeight = targetCurrentHeight;
                     double oldTargetCurrentWidth  = targetCurrentWidth;
                     double oldSourceCurrentHeight = sourceCurrentHeight;
                     double oldSourceCurrentWidth  = sourceCurrentWidth;

                     source.popFromPyramid();
                     target.popFromPyramid();

                     targetCurrentHeight = target.getCurrentHeight();
                     targetCurrentWidth  = target.getCurrentWidth();
                     targetFactorHeight = target.getFactorHeight();                 
                     targetFactorWidth  = target.getFactorWidth();

                     sourceCurrentHeight = source.getCurrentHeight();
                     sourceCurrentWidth  = source.getCurrentWidth();
                     sourceFactorHeight = source.getFactorHeight();
                     sourceFactorWidth  = source.getFactorWidth();

                     // Adapt the transformation to the new image size
                     double targetFactorY = (targetCurrentHeight-1) / (oldTargetCurrentHeight-1);
                     double targetFactorX = (targetCurrentWidth -1) / (oldTargetCurrentWidth -1);                 
                     double sourceFactorY = (sourceCurrentHeight-1) / (oldSourceCurrentHeight-1);
                     double sourceFactorX = (sourceCurrentWidth -1) / (oldSourceCurrentWidth -1);

                     for (int i=0; i<intervals+3; i++)
                       for (int j=0; j<intervals+3; j++) 
                       {
                           cxTargetToSource[i][j] *= targetFactorX;
                           cyTargetToSource[i][j] *= targetFactorY;
                           cxSourceToTarget[i][j] *= sourceFactorX;
                           cySourceToTarget[i][j] *= sourceFactorY;
                       }                    
                     
                     // Prepare matrices for the regularization term
                     buildRegularizationTemporary(intervals, false);
                     buildRegularizationTemporary(intervals, true);
                  }
                  break;
            }

            // In accurate_mode reduce the stopping threshold for the last iteration
            if ((state==0 || state==1) && s==max_scale_deformation && 
                currentDepth==min_scale_image+1 && accurate_mode==1)
                stopThreshold /= 10;
            
       }// end while (state != -1).     
        
       // Show results.
       showTransformation(intervals, cxTargetToSource, cyTargetToSource, false);
       showTransformation(intervals, cxSourceToTarget, cySourceToTarget, true);
       
       // Display final errors.
       IJ.write(" Final direct similarity error = " + this.finalDirectSimilarityError);
       IJ.write(" Final inverse similarity error = " + this.finalInverseSimilarityError);
       IJ.write(" Final direct consistency error = " + this.finalDirectConsistencyError);
       IJ.write(" Final inverse consistency error = " + this.finalInverseConsistencyError);
       
        // Save transformations.
       if (saveTransf)
       {
           saveTransformation(intervals, cxTargetToSource, cyTargetToSource, false);
           saveTransformation(intervals, cxSourceToTarget, cySourceToTarget, true);
       }
    } /* end doRegistration */


    /*--------------------------------------------------------------------------*/
    /** 
     * Evaluate the similarity between the images.
     *
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     * @return image similarity
     */
    public double evaluateImageSimilarity(boolean bIsReverse) 
    {
       int int3 = intervals+3;
       int halfM = int3*int3;
       int M = halfM*2;

       double   []x            = new double   [M];
       double   []grad         = new double   [M];

       // Variables to allow registering in both directions.
       bUnwarpJImageModel auxTarget = target;
       bUnwarpJImageModel swx = swxTargetToSource;
       bUnwarpJImageModel swy = swyTargetToSource;
       double [][]cx = cxTargetToSource;
       double [][]cy = cyTargetToSource;

       if(bIsReverse)
       {
           auxTarget = source;
           swx = swxSourceToTarget;
           swy = swySourceToTarget;
           cx = cxSourceToTarget;
           cy = cySourceToTarget;
       }

       for (int i= 0, p=0; i<intervals + 3; i++)
           for (int j = 0; j < intervals + 3; j++, p++) 
           {
             x[      p] = cx[i][j];
             x[halfM+p] = cy[i][j];
           }

       if (swx==null) 
       {
          swx = new bUnwarpJImageModel(cx);
          swy = new bUnwarpJImageModel(cy);
          swx.precomputed_prepareForInterpolation(
             auxTarget.getCurrentHeight(), auxTarget.getCurrentWidth(), intervals);
          swy.precomputed_prepareForInterpolation(
             auxTarget.getCurrentHeight(), auxTarget.getCurrentWidth(), intervals);
       }

       if (swx.precomputed_getWidth() != auxTarget.getCurrentWidth()) 
       {
          swx.precomputed_prepareForInterpolation(
             auxTarget.getCurrentHeight(), auxTarget.getCurrentWidth(), intervals);
          swy.precomputed_prepareForInterpolation(
             auxTarget.getCurrentHeight(), auxTarget.getCurrentWidth(), intervals);
       }
       return evaluateSimilarity(x, intervals, grad, true, false, bIsReverse);
    }


    /*------------------------------------------------------------------*/
    /**
     * Get the deformation from the corresponding coefficients.
     *
     * @param transformation_x matrix to store the x- transformation
     * @param transformation_y matrix to store the y- transformation
     * @param bIsReverse flag to choose the deformation coefficients 
     *                   (source-target=TRUE or target-source=FALSE)
     */
    public void getDeformation(
        final double [][]transformation_x,
        final double [][]transformation_y, 
        boolean bIsReverse) 
    {  
       // Variables to allow registering in both directions. 
       double [][]cx = cxTargetToSource;
       double [][]cy = cyTargetToSource;
       if(bIsReverse)
       {
           cx = cxSourceToTarget;
           cy = cySourceToTarget;
       }
        computeDeformation(intervals, cx, cy,
           transformation_x,transformation_y, bIsReverse);
    }

    /*------------------------------------------------------------------*/
    /**
     * Create an instance of bUnwarpJTransformation.
     *
     * @param sourceImp image representation for the source
     * @param targetImp image representation for the target
     * @param source source image model
     * @param target target image model
     * @param sourcePh point handler for the landmarks in the source image
     * @param targetPh point handler for the landmarks in the target image
     * @param sourceMsk source image mask
     * @param targetMsk target image mask
     * @param min_scale_deformation minimum scale deformation 
     * @param max_scale_deformation maximum scale deformation
     * @param min_scale_image minimum image scale
     * @param divWeight divergency weight
     * @param curlWeight curl weight
     * @param landmarkWeight landmark weight
     * @param imageWeight weight for image similarity
     * @param consistencyWeight weight for the deformations consistency
     * @param stopThreshold stopping threshold 
     * @param outputLevel flag to specify the level of resolution in the output
     * @param showMarquardtOptim flag to show the optimizer
     * @param accurate_mode level of accuracy 
     * @param saveTransf flat to choose to save the transformation in a file
     * @param fn_tnf_1 direct transformation file name
     * @param fn_tnf_2 inverse transformation file name
     * @param output_ip_1 pointer to the first output image
     * @param output_ip_2 pointer to the second output image
     * @param dialog pointer to the dialog of the bUnwarpJ interface
     */
    public bUnwarpJTransformation (
       final ImagePlus                    sourceImp,
       final ImagePlus                    targetImp,
       final bUnwarpJImageModel source,
       final bUnwarpJImageModel target,
       final bUnwarpJPointHandler sourcePh,
       final bUnwarpJPointHandler targetPh,
       final bUnwarpJMask sourceMsk,
       final bUnwarpJMask targetMsk,
       final int min_scale_deformation,
       final int max_scale_deformation,
       final int min_scale_image,
       final double divWeight,
       final double curlWeight,
       final double landmarkWeight,
       final double imageWeight,
       final double consistencyWeight,       
       final double stopThreshold,
       final int outputLevel,
       final boolean showMarquardtOptim,
       final int accurate_mode,
       final boolean saveTransf,
       final String fn_tnf_1,
       final String fn_tnf_2,     
       final ImagePlus output_ip_1,
       final ImagePlus output_ip_2,
       final bUnwarpJDialog dialog) 
    {
       this.sourceImp	      = sourceImp;
       this.targetImp	      = targetImp;
       this.source                = source;
       this.target                = target;
       this.sourcePh              = sourcePh;
       this.targetPh              = targetPh;
       this.sourceMsk             = sourceMsk;
       this.targetMsk             = targetMsk;
       this.min_scale_deformation = min_scale_deformation;
       this.max_scale_deformation = max_scale_deformation;
       this.min_scale_image       = min_scale_image;
       this.divWeight             = divWeight;
       this.curlWeight            = curlWeight;
       this.landmarkWeight        = landmarkWeight;
       this.imageWeight           = imageWeight;
       this.consistencyWeight     = consistencyWeight;
       this.stopThreshold         = stopThreshold;
       this.outputLevel           = outputLevel;
       this.showMarquardtOptim    = showMarquardtOptim;
       this.accurate_mode         = accurate_mode;
       this.saveTransf            = saveTransf;
       this.fn_tnf_1              = fn_tnf_1;
       this.fn_tnf_2              = fn_tnf_2;
       this.output_ip_1           = output_ip_1;
       this.output_ip_2           = output_ip_2;
       this.dialog                = dialog;
       sourceWidth                = source.getWidth();
       sourceHeight               = source.getHeight();
       targetWidth                = target.getWidth();
       targetHeight               = target.getHeight();
    } /* end bUnwarpJTransformation */

    /*------------------------------------------------------------------*/
    /**
     * Apply the corresponding transformation to a given point.
     *
     * @param u input, x- point coordinate
     * @param v input, y- point coordinate
     * @param xyF ouput, transformed point
     * @param bIsReverse flag to decide the transformation direction (direct-inverse)
     *                   (source-target=TRUE or target-source=FALSE)
     */
    public void transform(double u, double v, double []xyF, boolean bIsReverse) 
    {
       // Variables to allow registering in both directions.
       bUnwarpJImageModel auxTarget = target;
       bUnwarpJImageModel swx = swxTargetToSource;
       bUnwarpJImageModel swy = swyTargetToSource;

       if(bIsReverse)
       {
           auxTarget = source;
           swx = swxSourceToTarget;
           swy = swySourceToTarget;
       }

       final double tu = (u * intervals) / (double)(auxTarget.getCurrentWidth()  - 1) + 1.0F;
       final double tv = (v * intervals) / (double)(auxTarget.getCurrentHeight() - 1) + 1.0F;

       final boolean ORIGINAL = false;
       swx.prepareForInterpolation(tu,tv,ORIGINAL); 
       xyF[0] = swx.interpolateI();
       swy.prepareForInterpolation(tu,tv,ORIGINAL); 
       xyF[1] = swy.interpolateI();
    }

    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Build the matrix for the landmark interpolation.
     *
     * @param intervals Intervals in the deformation
     * @param K Number of landmarks
     * @param B System matrix of the landmark interpolation
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     */
    private void build_Matrix_B(
        int intervals,    // Intervals in the deformation
        int K,            // Number of landmarks
        double [][]B,     // System matrix of the landmark interpolation
        boolean bIsReverse) 
    {

       // Auxiliar variables to calculate inverse transformation   
       bUnwarpJPointHandler auxTargetPh = this.targetPh;   
       double auxFactorWidth = this.targetFactorWidth;
       double auxFactorHeight = this.targetFactorHeight;

       if(bIsReverse)
       {
           auxTargetPh = this.sourcePh;              
           auxFactorWidth = this.sourceFactorWidth;
           auxFactorHeight = this.sourceFactorHeight;
       }   

       Vector targetVector = null;
       if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
       for (int k = 0; k<K; k++) 
       {
          final Point targetPoint = (Point)targetVector.elementAt(k);
          double x = auxFactorWidth * (double)targetPoint.x;
          double y = auxFactorHeight * (double)targetPoint.y;
          final double[] bx = xWeight(x, intervals, true, bIsReverse);
          final double[] by = yWeight(y, intervals, true, bIsReverse);
          for (int i=0; i<intervals+3; i++)
             for (int j=0; j<intervals+3; j++)
                B[k][(intervals+3)*i+j] = by[i] * bx[j];
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Build matrix Rq1q2.
     */
    private void build_Matrix_Rq1q2(
        int intervals,
        double weight,
        int q1, int q2,
        double [][]R,
        boolean bIsReverse)
    {build_Matrix_Rq1q2q3q4(intervals, weight, q1, q2, q1, q2, R, bIsReverse);}

    /*------------------------------------------------------------------*/
    /**
     * Build matrix Rq1q2q3q4.
     */
    private void build_Matrix_Rq1q2q3q4(
        int intervals,
        double weight,
        int q1, int q2, int q3, int q4,
        double [][]R,
        boolean bIsReverse)
    {
       /* Let's define alpha_q as the q-th derivative of a B-Spline

                         q   n
                       d    B (x)
          alpha_q(x)= --------------
                             q
                           dx

          eta_q1q2(x,s1,s2)=integral_0^Xdim alpha_q1(x/h-s1) alpha_q2(x/h-s2)

       */
       double [][]etaq1q3 = new double[16][16];
       int Ydim = target.getCurrentHeight();
       int Xdim = target.getCurrentWidth();

       if(bIsReverse)
       {
           Ydim = source.getCurrentHeight();
           Xdim = source.getCurrentWidth();
       }

       build_Matrix_R_geteta(etaq1q3, q1, q3, Xdim, intervals);

       double [][]etaq2q4=null;
       if (q2!=q1 || q4!=q3 || Ydim!=Xdim) 
       {
          etaq2q4=new double[16][16];
          build_Matrix_R_geteta(etaq2q4, q2, q4, Ydim, intervals);
       } 
       else etaq2q4 = etaq1q3;

       int M=intervals+1;
       int Mp=intervals+3;
       for (int l=-1; l<=M; l++)
          for (int k=-1; k<=M; k++)
             for (int n=-1; n<=M; n++)
                for (int m=-1; m<=M; m++) {
                   int []ip=new int[2];
                   int []jp=new int[2];
                   boolean valid_i = build_Matrix_R_getetaindex(l, n, intervals, ip);
                   boolean valid_j = build_Matrix_R_getetaindex(k, m, intervals, jp);
                   if (valid_i && valid_j) 
                   {
                      int mn=(n+1)*Mp+(m+1);
                      int kl=(l+1)*Mp+(k+1);
                      R[kl][mn]+=weight*etaq1q3[jp[0]][jp[1]]*etaq2q4[ip[0]][ip[1]];
                   }
                }
    }

    /*------------------------------------------------------------------*/
    /**
     * Compute the following integral
     *<P>
     *<PRE>
     *           xF d^q1      3  x        d^q2    3  x
     *  integral    -----   B  (--- - s1) ----- B  (--- - s2) dx
     *           x0 dx^q1        h        dx^q2      h
     *<PRE>
     */

    private double build_Matrix_R_computeIntegral_aa(
       double x0,
       double xF,
       double s1,
       double s2,
       double h,
       int    q1,
       int    q2) 
    {
    // Computes the following integral
    //
    //           xF d^q1      3  x        d^q2    3  x
    //  integral    -----   B  (--- - s1) ----- B  (--- - s2) dx
    //           x0 dx^q1        h        dx^q2      h

       // Form the spline coefficients
       double [][]C = new double [3][3];
       int    [][]d = new int    [3][3];
       double [][]s = new double [3][3];
       C[0][0]= 1  ; C[0][1]= 0  ; C[0][2]= 0  ;
       C[1][0]= 1  ; C[1][1]=-1  ; C[1][2]= 0  ;
       C[2][0]= 1  ; C[2][1]=-2  ; C[2][2]= 1  ; 
       d[0][0]= 3  ; d[0][1]= 0  ; d[0][2]= 0  ;
       d[1][0]= 2  ; d[1][1]= 2  ; d[1][2]= 0  ;
       d[2][0]= 1  ; d[2][1]= 1  ; d[2][2]= 1  ; 
       s[0][0]= 0  ; s[0][1]= 0  ; s[0][2]= 0  ;
       s[1][0]=-0.5; s[1][1]= 0.5; s[1][2]= 0  ;
       s[2][0]= 1  ; s[2][1]= 0  ; s[2][2]=-1  ; 

       // Compute the integral
       double integral=0;
       for (int k=0; k<3; k++) 
       {
           double ck=C[q1][k]; if (ck==0) continue;
           for (int l=0; l<3; l++) 
           {
               double cl=C[q2][l]; if (cl==0) continue;
               integral+=ck*cl*build_matrix_R_computeIntegral_BB(
                  x0,xF,s1+s[q1][k],s2+s[q2][l],h,d[q1][k],d[q2][l]);
           }
        }
        return integral;
    }

    /*------------------------------------------------------------------*/
    /**
     * Compute the following integral
     *<PRE>
     *           xF   n1  x          n2  x
     *  integral     B  (--- - s1)  B  (--- - s2) dx
     *           x0       h              h
     *</PRE>
     */
    private double build_matrix_R_computeIntegral_BB(
       double x0,
       double xF,
       double s1,
       double s2,
       double h,
       int    n1,
       int    n2) 
    {
    // Computes the following integral
    //
    //           xF   n1  x          n2  x
    //  integral     B  (--- - s1)  B  (--- - s2) dx
    //           x0       h              h

       // Change the variable so that the h disappears
       // X=x/h
       double xFp=xF/h;
       double x0p=x0/h;

       // Form the spline coefficients
       double []c1=new double [n1+2];
       double fact_n1=1; for (int k=2; k<=n1; k++) fact_n1*=k;
       double sign=1; 
       for (int k=0; k<=n1+1; k++, sign*=-1)
           c1[k]=sign*bUnwarpJMathTools.nchoosek(n1+1,k)/fact_n1;

       double []c2=new double [n2+2];
       double fact_n2=1; for (int k=2; k<=n2; k++) fact_n2*=k;
       sign=1; 
       for (int k=0; k<=n2+1; k++, sign*=-1)
           c2[k]=sign*bUnwarpJMathTools.nchoosek(n2+1,k)/fact_n2;

       // Compute the integral
       double n1_2=(double)((n1+1))/2.0;
       double n2_2=(double)((n2+1))/2.0;
       double integral=0;
       for (int k=0; k<=n1+1; k++)
           for (int l=0; l<=n2+1; l++) {
               integral+=
                  c1[k]*c2[l]*build_matrix_R_computeIntegral_xx(
                     x0p,xFp,s1+k-n1_2,s2+l-n2_2,n1,n2);
           }
       return integral*h;
    }

    /*------------------------------------------------------------------*/
    /**
     *<P>
     *<PRE>
     * Computation of the integral:
     *             xF          q1       q2
     *    integral       (x-s1)   (x-s2)     dx
     *             x0          +        +
     *</PRE>
     */
    private double build_matrix_R_computeIntegral_xx(
       double x0,
       double xF,
       double s1,
       double s2,
       int    q1,
       int    q2)
    {
    // Computation of the integral
    //             xF          q1       q2
    //    integral       (x-s1)   (x-s2)     dx
    //             x0          +        +

       // Change of variable so that s1 is 0
       // X=x-s1 => x-s2=X-(s2-s1)
       double s2p=s2-s1;
       double xFp=xF-s1;
       double x0p=x0-s1;

       // Now integrate
       if (xFp<0) return 0;

       // Move x0 to the first point where both integrands
       // are distinct from 0
       x0p=Math.max(x0p,Math.max(s2p,0));
       if (x0p>xFp) return 0;

       // There is something to integrate
       // Evaluate the primitive at xF and x0
       double IxFp=0;
       double Ix0p=0;
       for (int k=0; k<=q2; k++) {
           double aux=bUnwarpJMathTools.nchoosek(q2,k)/(q1+k+1)*
                      Math.pow(-s2p,q2-k);
           IxFp+=Math.pow(xFp,q1+k+1)*aux;
           Ix0p+=Math.pow(x0p,q1+k+1)*aux;
       }

       return IxFp-Ix0p;
    }

    /*------------------------------------------------------------------*/
    /**
     * Build matrix R, get eta.
     */
    private void build_Matrix_R_geteta(
       double [][]etaq1q2,
       int q1,
       int q2,
       int dim,
       int intervals) 
    {
       boolean [][]done = new boolean[16][16];
       // Clear
       for (int i=0; i<16; i++)
           for (int j=0; j<16; j++) 
           {
               etaq1q2[i][j]=0;
               done[i][j]=false;
           }

       // Compute each integral needed
       int M = intervals+1;
       double h = (double) dim/intervals;
       for (int ki1=-1; ki1<=M; ki1++)
          for (int ki2=-1; ki2<=M; ki2++) 
          {
              int []ip = new int[2];
              boolean valid_i = build_Matrix_R_getetaindex(ki1, ki2, intervals, ip);
              if (valid_i && !done[ip[0]][ip[1]]) 
              {
                 etaq1q2[ip[0]][ip[1]]=
                    build_Matrix_R_computeIntegral_aa(0,dim,ki1,ki2,h,q1,q2);
                 done[ip[0]][ip[1]]=true;
              }
          }
    }

    /*------------------------------------------------------------------*/
    /**
     * Build matrix R, get eta index.
     */
    private boolean build_Matrix_R_getetaindex(
       int ki1,
       int ki2,
       int intervals,
       int []ip) 
    {
       ip[0]=0;
       ip[1]=0;

       // Determine the clipped inner limits of the intersection
       int kir=Math.min(intervals,Math.min(ki1,ki2)+2);
       int kil=Math.max(0        ,Math.max(ki1,ki2)-2);

       if (kil>=kir) return false;

       // Determine which are the pieces of the
       // function that lie in the intersection
       int two_i=1;
       double ki;
       for (int i=0; i<=3; i++, two_i*=2) 
       {
           // First function
           ki=ki1+i-1.5; // Middle sample of the piece i
           if (kil<=ki && ki<=kir) ip[0]+=two_i;

           // Second function
           ki=ki2+i-1.5; // Middle sample of the piece i
           if (kil<=ki && ki<=kir) ip[1]+=two_i;
       }

       ip[0]--;
       ip[1]--;
       return true;   
    }

    /*------------------------------------------------------------------*/
    /**
     * Build regularization temporary.
     *
     * @param intervals intervals in the deformation
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     */
    private void buildRegularizationTemporary(int intervals, boolean bIsReverse) 
    {
        // M is the number of spline coefficients per row
        int M = intervals+3;
        int M2 = M * M;

       // P11
       double[][] P11 = new double[M2][M2];      
       if(bIsReverse) P11_SourceToTarget = P11;
       else           P11_TargetToSource = P11;

       for (int i=0; i<M2; i++)
          for (int j=0; j<M2; j++) P11[i][j]=0.0;
       build_Matrix_Rq1q2(intervals, divWeight           , 2, 0, P11, bIsReverse);
       build_Matrix_Rq1q2(intervals, divWeight+curlWeight, 1, 1, P11, bIsReverse);
       build_Matrix_Rq1q2(intervals,           curlWeight, 0, 2, P11, bIsReverse);

       // P22
       double[][] P22 = new double[M2][M2];
       if(bIsReverse) P22_SourceToTarget = P22;
       else           P22_TargetToSource = P22;

       for (int i=0; i<M2; i++)
          for (int j=0; j<M2; j++) P22[i][j]=0.0;
       build_Matrix_Rq1q2(intervals, divWeight           , 0, 2, P22, bIsReverse);
       build_Matrix_Rq1q2(intervals, divWeight+curlWeight, 1, 1, P22, bIsReverse);
       build_Matrix_Rq1q2(intervals,           curlWeight, 2, 0, P22, bIsReverse);

       // P12
       double[][] P12 = new double[M2][M2];
       if(bIsReverse) P12_SourceToTarget = P12;
       else           P12_TargetToSource = P12;

       for (int i=0; i<M2; i++)
          for (int j=0; j<M2; j++) P12[i][j]=0.0;
       build_Matrix_Rq1q2q3q4(intervals, 2*divWeight , 2, 0, 1, 1, P12, bIsReverse);
       build_Matrix_Rq1q2q3q4(intervals, 2*divWeight , 1, 1, 0, 2, P12, bIsReverse);
       build_Matrix_Rq1q2q3q4(intervals,-2*curlWeight, 0, 2, 1, 1, P12, bIsReverse);
       build_Matrix_Rq1q2q3q4(intervals,-2*curlWeight, 1, 1, 2, 0, P12, bIsReverse);      
    }

    /*------------------------------------------------------------------*/
    /**
     * Compute the affine matrix.
     *
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     */
    private double[][] computeAffineMatrix (boolean bIsReverse) 
    {    
       boolean adjust_size = false;

       final double[][] D = new double[3][3];
       final double[][] H = new double[3][3];
       final double[][] U = new double[3][3];
       final double[][] V = new double[3][3];
       final double[][] X = new double[2][3];
       final double[] W = new double[3];

       // Auxiliar variables to calculate inverse transformation
       bUnwarpJPointHandler auxSourcePh = sourcePh;
       bUnwarpJPointHandler auxTargetPh = targetPh;
       bUnwarpJMask auxSourceMsk = sourceMsk;
       bUnwarpJMask auxTargetMsk = targetMsk;
       bUnwarpJImageModel auxSource = source;
       bUnwarpJImageModel auxTarget = target;
       double auxFactorWidth = this.targetFactorWidth;
       double auxFactorHeight = this.targetFactorHeight;

       if(bIsReverse)
       {
           auxSourcePh = targetPh;
           auxTargetPh = sourcePh;
           auxSourceMsk = targetMsk;
           auxTargetMsk = sourceMsk;
           auxSource = target;
           auxTarget = source;
           auxFactorWidth = this.sourceFactorWidth;
           auxFactorHeight = this.sourceFactorHeight;
       }

       Vector sourceVector=null;
       if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
       else                   sourceVector = new Vector();

       Vector targetVector = null;
       if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
       else                   targetVector = new Vector();

       int removeLastPoint = 0;

        if (false) 
        {
            removeLastPoint = auxSourceMsk.numberOfMaskPoints();
            for (int i=0; i<removeLastPoint; i++) 
            {
               sourceVector.addElement(auxSourceMsk.getPoint(i));
               targetVector.addElement(auxTargetMsk.getPoint(i));
            }
        }

       int n = targetVector.size();
       switch (n) 
       {
          case 0:
             for (int i = 0; (i < 2); i++) 
                for (int j = 0; (j < 3); j++) X[i][j]=0.0;
              if (adjust_size) 
              {
                  // Make both images of the same size
                 X[0][0] = (double)auxSource.getCurrentWidth () / auxTarget.getCurrentWidth ();
                 X[1][1] = (double)auxSource.getCurrentHeight() / auxTarget.getCurrentHeight();
               } 
              else 
              {
                 // Make both images to be centered
                 X[0][0] = X[1][1] = 1;
                 X[0][2] = ((double)auxSource.getCurrentWidth () - auxTarget.getCurrentWidth ())/2;
                 X[1][2] = ((double)auxSource.getCurrentHeight() - auxTarget.getCurrentHeight())/2;
              }
              break;
          case 1:
             for (int i = 0; (i < 2); i++) {
                for (int j = 0; (j < 2); j++) {
                   X[i][j] = (i == j) ? (1.0F) : (0.0F);
                }
             }
             X[0][2] = auxFactorWidth * (double)(((Point)sourceVector.firstElement()).x
                - ((Point)targetVector.firstElement()).x);
             X[1][2] = auxFactorHeight * (double)(((Point)sourceVector.firstElement()).y
                - ((Point)targetVector.firstElement()).y);
             break;
          case 2:
             final double x0 = auxFactorWidth  * ((Point)sourceVector.elementAt(0)).x;
             final double y0 = auxFactorHeight * ((Point)sourceVector.elementAt(0)).y;
             final double x1 = auxFactorWidth  * ((Point)sourceVector.elementAt(1)).x;
             final double y1 = auxFactorHeight * ((Point)sourceVector.elementAt(1)).y;
             final double u0 = auxFactorWidth  * ((Point)targetVector.elementAt(0)).x;
             final double v0 = auxFactorHeight * ((Point)targetVector.elementAt(0)).y;
             final double u1 = auxFactorWidth  * ((Point)targetVector.elementAt(1)).x;
             final double v1 = auxFactorHeight * ((Point)targetVector.elementAt(1)).y;
             sourceVector.addElement(new Point((int)(x1 + y0 - y1), (int)(x1 + y1 - x0)));
             targetVector.addElement(new Point((int)(u1 + v0 - v1), (int)(u1 + v1 - u0)));
             removeLastPoint=1;
             n = 3;
          default:
             for (int i = 0; (i < 3); i++) 
             {
                for (int j = 0; (j < 3); j++) 
                {
                   H[i][j] = 0.0F;
                }
             }
             for (int k = 0; (k < n); k++) 
             {
                final Point sourcePoint = (Point)sourceVector.elementAt(k);
                final Point targetPoint = (Point)targetVector.elementAt(k);
                final double sx = auxFactorWidth * (double)sourcePoint.x;
                final double sy = auxFactorHeight* (double)sourcePoint.y;
                final double tx = auxFactorWidth * (double)targetPoint.x;
                final double ty = auxFactorHeight* (double)targetPoint.y;
                H[0][0] += tx * sx;
                H[0][1] += tx * sy;
                H[0][2] += tx;
                H[1][0] += ty * sx;
                H[1][1] += ty * sy;
                H[1][2] += ty;
                H[2][0] += sx;
                H[2][1] += sy;
                H[2][2] += 1.0F;
                D[0][0] += sx * sx;
                D[0][1] += sx * sy;
                D[0][2] += sx;
                D[1][0] += sy * sx;
                D[1][1] += sy * sy;
                D[1][2] += sy;
                D[2][0] += sx;
                D[2][1] += sy;
                D[2][2] += 1.0F;
             }
             bUnwarpJMathTools.singularValueDecomposition(H, W, V);
             if ((Math.abs(W[0]) < FLT_EPSILON) || (Math.abs(W[1]) < FLT_EPSILON)
                || (Math.abs(W[2]) < FLT_EPSILON)) {
                return(computeRotationMatrix(bIsReverse));
             }
             for (int i = 0; (i < 3); i++) {
                for (int j = 0; (j < 3); j++) {
                   V[i][j] /= W[j];
                }
             }
             for (int i = 0; (i < 3); i++) {
                for (int j = 0; (j < 3); j++) {
                   U[i][j] = 0.0F;
                   for (int k = 0; (k < 3); k++) {
                      U[i][j] += D[i][k] * V[k][j];
                   }
                }
             }
             for (int i = 0; (i < 2); i++) {
                for (int j = 0; (j < 3); j++) {
                   X[i][j] = 0.0F;
                   for (int k = 0; (k < 3); k++) {
                      X[i][j] += U[i][k] * H[j][k];
                   }
                }
             }
             break;
       }
       if (removeLastPoint!=0) {
          for (int i=1; i<=removeLastPoint; i++) {
             auxSourcePh.getPoints().removeElementAt(n-i);
             auxTargetPh.getPoints().removeElementAt(n-i);
          }
       }

       return(X);
    } /* end computeAffineMatrix */

    /*------------------------------------------------------------------*/
    /**
     * Compute the affine residues for the landmarks.
     * <p>
     * NOTE: The output vectors should be already resized
     *
     * @param affineMatrix Input
     * @param dx Output, difference in x for each landmark
     * @param dy Output, difference in y for each landmark
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     */
    private void computeAffineResidues(
       final double[][] affineMatrix,               // Input
       final double[] dx,                           // output, difference in x for each landmark
       final double[] dy,                           // output, difference in y for each landmark
                                                    // The output vectors should be already resized
       boolean bIsReverse) 
    {
       // Auxiliar variables to allow registering in both directions
       double auxFactorWidth = target.getFactorWidth();
       double auxFactorHeight = target.getFactorHeight();
       bUnwarpJPointHandler auxSourcePh = this.sourcePh;
       bUnwarpJPointHandler auxTargetPh = this.targetPh;
       if(bIsReverse)
       {
           auxFactorWidth = source.getFactorWidth();
           auxFactorHeight = source.getFactorHeight();
           auxSourcePh = this.targetPh;
           auxTargetPh = this.sourcePh;
       }

       Vector sourceVector=null;
       if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
       else                   sourceVector = new Vector();

       Vector targetVector = null;
       if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
       else                   targetVector = new Vector();

       final int K = auxTargetPh.getPoints().size();
       for (int k=0; k<K; k++) 
       {
          final Point sourcePoint = (Point)sourceVector.elementAt(k);
          final Point targetPoint = (Point)targetVector.elementAt(k);

          double u = auxFactorWidth  * (double)targetPoint.x;
          double v = auxFactorHeight * (double)targetPoint.y;

          final double x = affineMatrix[0][2]
             + affineMatrix[0][0] * u + affineMatrix[0][1] * v;

          final double y = affineMatrix[1][2]
             + affineMatrix[1][0] * u + affineMatrix[1][1] * v;

          dx[k] = auxFactorWidth  * (double)sourcePoint.x - x;
          dy[k] = auxFactorHeight * (double)sourcePoint.y - y;
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Compute the coefficients scale.
     *
     * @param intervals input, number of intervals at this scale
     * @param dx input, x residue so far
     * @param dy input, y residue so far
     * @param cx output, x coefficients for splines
     * @param cy output, y coefficients for splines
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     * @return under-constrained flag
     */
    private boolean computeCoefficientsScale(
       final int intervals,                       // input, number of intervals at this scale
       final double []dx,                         // input, x residue so far
       final double []dy,                         // input, y residue so far
       final double [][]cx,                       // output, x coefficients for splines
       final double [][]cy,                       // output, y coefficients for splines
       boolean bIsReverse) 
    {       

       bUnwarpJPointHandler auxTargetPh = (bIsReverse) ? this.sourcePh : this.targetPh; 

       int K=0;
       if (auxTargetPh!=null) K = auxTargetPh.getPoints().size();
       boolean underconstrained=false;

       if (0<K) {
          // Form the equation system Bc=d
          final double[][] B = new double[K][(intervals + 3) * (intervals + 3)];
          build_Matrix_B(intervals, K, B, bIsReverse);

          // "Invert" the matrix B
          int Nunk=(intervals+3)*(intervals+3);
          double [][] iB=new double[Nunk][K];
          underconstrained=bUnwarpJMathTools.invertMatrixSVD(K,Nunk,B,iB);

          // Now multiply iB times dx and dy respectively
          int ij=0;
          for (int i = 0; i<intervals+3; i++)
             for (int j = 0; j<intervals+3; j++) {
                cx[i][j] = cy[i][j] = 0.0F;
                for (int k = 0; k<K; k++) {
                   cx[i][j] += iB[ij][k] * dx[k];
                   cy[i][j] += iB[ij][k] * dy[k];
                }
                ij++;
             }
       }
       return underconstrained;
    }
    /*------------------------------------------------------------------*/
    /**
     * Compute the coefficients scale with regularization.
     *
     * @param intervals input, number of intervals at this scale
     * @param dx input, x residue so far
     * @param dy input, y residue so far
     * @param cx output, x coefficients for splines
     * @param cy output, y coefficients for splines
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     * @return under-constrained flag
     */
    private boolean computeCoefficientsScaleWithRegularization(
       final int intervals,                       // input, number of intervals at this scale
       final double []dx,                         // input, x residue so far
       final double []dy,                         // input, y residue so far
       final double [][]cx,                       // output, x coefficients for splines
       final double [][]cy,                       // output, y coefficients for splines
       boolean bIsReverse) 
    {

       double P11[][] = this.P11_TargetToSource;
       double P12[][] = this.P12_TargetToSource;
       double P22[][] = this.P22_TargetToSource;

       bUnwarpJPointHandler auxTargetPh = this.targetPh; 
       if(bIsReverse)
       {
           auxTargetPh = this.sourcePh;
           P11 = this.P11_SourceToTarget;
           P12 = this.P12_SourceToTarget;
           P22 = this.P22_SourceToTarget;
       }

       boolean underconstrained=true;
       int K = 0;
       if (auxTargetPh!=null) K = auxTargetPh.getPoints().size();

       if (0<K) {
          // M is the number of spline coefficients per row
          int M=intervals+3;
          int M2=M*M;

          // Create A and b for the system Ac=b
          final double[][] A = new double[2*M2][2*M2];
          final double[]   b = new double[2*M2];
          for (int i=0; i<2*M2; i++) {
             b[i]=0.0;
             for (int j=0; j<2*M2; j++) A[i][j]=0.0;
          }

          // Get the matrix related to the landmarks 
          final double[][] B = new double[K][M2];
          build_Matrix_B(intervals, K, B, bIsReverse);

          // Fill the part of the equation system related to the landmarks
          // Compute 2 * B^t * B
          for (int i=0; i<M2; i++) {
              for (int j=i; j<M2; j++) {
                 double bitbj=0; // bi^t * bj, i.e., column i x column j
                 for (int l=0; l<K; l++)
                     bitbj+=B[l][i]*B[l][j];
                 bitbj*=2;
                 int ij=i*M2+j;
                 A[M2+i][M2+j]=A[M2+j][M2+i]=A[i][j]=A[j][i]=bitbj;
              }
          }

          // Compute 2 * B^t * [dx dy]
          for (int i=0; i<M2; i++) {
             double bitdx=0;
             double bitdy=0;
             for (int l=0; l<K; l++) {
                bitdx+=B[l][i]*dx[l];
                bitdy+=B[l][i]*dy[l];
             }
             bitdx*=2;
             bitdy*=2;
             b[   i]=bitdx;
             b[M2+i]=bitdy;
          }

          // Get the matrices associated to the regularization
          // Copy P11 symmetrized to the equation system
          for (int i=0; i<M2; i++)
             for (int j=0; j<M2; j++) {
                double aux=P11[i][j];
                A[i][j]+=aux;
                A[j][i]+=aux;
             }

          // Copy P22 symmetrized to the equation system
          for (int i=0; i<M2; i++)
             for (int j=0; j<M2; j++) {
                double aux=P22[i][j];
                A[M2+i][M2+j]+=aux;
                A[M2+j][M2+i]+=aux;
             }

          // Copy P12 and P12^t to their respective places
          for (int i=0; i<M2; i++)
             for (int j=0; j<M2; j++) {
                A[   i][M2+j]=P12[i][j]; // P12
                A[M2+i][   j]=P12[j][i]; // P12^t
             }

          // Now solve the system
          // Invert the matrix A
          double [][] iA=new double[2*M2][2*M2];
          underconstrained=bUnwarpJMathTools.invertMatrixSVD(2*M2,2*M2,A,iA);

          // Now multiply iB times b and distribute in cx and cy
          int ij=0;
          for (int i = 0; i<intervals+3; i++)
             for (int j = 0; j<intervals+3; j++) {
                cx[i][j] = cy[i][j] = 0.0F;
                for (int l = 0; l<2*M2; l++) {
                   cx[i][j] += iA[   ij][l] * b[l];
                   cy[i][j] += iA[M2+ij][l] * b[l];
                }
                ij++;
             }
       }
       return underconstrained;
    }

    /*------------------------------------------------------------------*/
    /**
     * Compute the initial residues for the landmarks.
     * <p>
     * NOTE: The output vectors should be already resized
     *
     * @param dx output, difference in x for each landmark 
     * @param dy output, difference in y for each landmark
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     */
    private void computeInitialResidues(
       final double[] dx,                           // output, difference in x for each landmark
       final double[] dy,                           // output, difference in y for each landmark
                                                    // The output vectors should be already resized
       boolean bIsReverse) 
    {

       // Auxiliar variables for registering in both directions.
       double auxFactorWidth = target.getFactorWidth();
       double auxFactorHeight = target.getFactorHeight();
       bUnwarpJPointHandler auxSourcePh = this.sourcePh;
       bUnwarpJPointHandler auxTargetPh = this.targetPh;

       if(bIsReverse)
       {
           auxFactorWidth = source.getFactorWidth();
           auxFactorHeight = source.getFactorHeight();
           auxSourcePh = this.targetPh;
           auxTargetPh = this.sourcePh;
       } 


       Vector sourceVector=null;
       if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
       else                   sourceVector = new Vector();

       Vector targetVector = null;
       if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
       else                   targetVector = new Vector();
       int K = 0;

       if (auxTargetPh!=null) auxTargetPh.getPoints().size();

       for (int k=0; k<K; k++) 
       {
          final Point sourcePoint = (Point)sourceVector.elementAt(k);
          final Point targetPoint = (Point)targetVector.elementAt(k);
          dx[k] = auxFactorWidth  * (sourcePoint.x - targetPoint.x);
          dy[k] = auxFactorHeight * (sourcePoint.y - targetPoint.y);
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Compute the deformation.
     *
     * @param intervals input, number of intervals
     * @param cx input, X b-spline coefficients
     * @param cy input, Y b-spline coefficients
     * @param transformation_x output, X transformation map
     * @param transformation_y output, Y transformation map
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     */
    private void computeDeformation(
       final int intervals,
       final double [][]cx,
       final double [][]cy,
       final double [][]transformation_x,
       final double [][]transformation_y,
       boolean bIsReverse) 
    {

       int auxTargetCurrentHeight = this.targetCurrentHeight;
       int auxTargetCurrentWidth = this.targetCurrentWidth;

       if(bIsReverse)
       {
           auxTargetCurrentHeight = this.sourceCurrentHeight;
           auxTargetCurrentWidth  = this.sourceCurrentWidth;
       }


       // Set these coefficients to an interpolator
       bUnwarpJImageModel swx = new bUnwarpJImageModel(cx);
       bUnwarpJImageModel swy = new bUnwarpJImageModel(cy);

        // Compute the transformation mapping
       for (int v=0; v<auxTargetCurrentHeight; v++) {
          final double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<auxTargetCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth - 1) + 1.0F;
             swx.prepareForInterpolation(tu, tv, ORIGINAL); 
             transformation_x[v][u] = swx.interpolateI();
             swy.prepareForInterpolation(tu, tv, ORIGINAL); 
             transformation_y[v][u] = swy.interpolateI();
          }
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Compute the rotation matrix.
     *
     * @param bIsReverse determines the transformation direction (source-target=TRUE or target-source=FALSE)
     * @return rotation matrix
     */
    private double[][] computeRotationMatrix (boolean bIsReverse) 
    {
       final double[][] X = new double[2][3];
       final double[][] H = new double[2][2];
       final double[][] V = new double[2][2];
       final double[] W = new double[2];


       double auxFactorWidth = this.targetFactorWidth;
       double auxFactorHeight = this.targetFactorHeight;
       bUnwarpJPointHandler auxSourcePh = this.sourcePh;
       bUnwarpJPointHandler auxTargetPh = this.targetPh;
       if(bIsReverse)
       {
           auxFactorWidth = this.sourceFactorWidth;
           auxFactorHeight = this.sourceFactorHeight;
           auxSourcePh = this.targetPh;
           auxTargetPh = this.sourcePh;
       }

       Vector sourceVector = null;
       if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
       else                   sourceVector = new Vector();

       Vector targetVector = null;
       if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
       else                   targetVector = new Vector();

       final int n = targetVector.size();
       switch (n) {
          case 0:
             for (int i = 0; (i < 2); i++) {
                for (int j = 0; (j < 3); j++) {
                   X[i][j] = (i == j) ? (1.0F) : (0.0F);
                }
             }
             break;
          case 1:
             for (int i = 0; (i < 2); i++) {
                for (int j = 0; (j < 2); j++) {
                   X[i][j] = (i == j) ? (1.0F) : (0.0F);
                }
             }
             X[0][2] = auxFactorWidth  * (double)(((Point)sourceVector.firstElement()).x
                - ((Point)targetVector.firstElement()).x);
             X[1][2] = auxFactorHeight * (double)(((Point)sourceVector.firstElement()).y
                - ((Point)targetVector.firstElement()).y);
             break;
          default:
             double xTargetAverage = 0.0F;
             double yTargetAverage = 0.0F;

             for (int i = 0; (i < n); i++) 
             {
                final Point p = (Point)targetVector.elementAt(i);
                xTargetAverage += auxFactorWidth  * (double)p.x;
                yTargetAverage += auxFactorHeight * (double)p.y;
             }

             xTargetAverage /= (double)n;
             yTargetAverage /= (double)n;

             final double[] xCenteredTarget = new double[n];
             final double[] yCenteredTarget = new double[n];

             for (int i = 0; (i < n); i++) 
             {
                final Point p = (Point)targetVector.elementAt(i);
                xCenteredTarget[i] = auxFactorWidth *(double)p.x - xTargetAverage;
                yCenteredTarget[i] = auxFactorHeight*(double)p.y - yTargetAverage;
             }

             double xSourceAverage = 0.0F;
             double ySourceAverage = 0.0F;

             for (int i = 0; (i < n); i++) 
             {
                final Point p = (Point)sourceVector.elementAt(i);
                xSourceAverage += auxFactorWidth *(double)p.x;
                ySourceAverage += auxFactorHeight*(double)p.y;
             }

             xSourceAverage /= (double)n;
             ySourceAverage /= (double)n;

             final double[] xCenteredSource = new double[n];
             final double[] yCenteredSource = new double[n];

             for (int i = 0; (i < n); i++) 
             {
                final Point p = (Point)sourceVector.elementAt(i);
                xCenteredSource[i] = auxFactorWidth *(double)p.x - xSourceAverage;
                yCenteredSource[i] = auxFactorHeight*(double)p.y - ySourceAverage;
             }

             for (int i = 0; (i < 2); i++) {
                for (int j = 0; (j < 2); j++) {
                   H[i][j] = 0.0F;
                }
             }

             for (int k = 0; (k < n); k++) 
             {
                H[0][0] += xCenteredTarget[k] * xCenteredSource[k];
                H[0][1] += xCenteredTarget[k] * yCenteredSource[k];
                H[1][0] += yCenteredTarget[k] * xCenteredSource[k];
                H[1][1] += yCenteredTarget[k] * yCenteredSource[k];
             }
             // COSS: Watch out that this H is the transpose of the one
             // defined in the text. That is why X=V*U^t is the inverse 
             // of the rotation matrix.
             bUnwarpJMathTools.singularValueDecomposition(H, W, V);
             if (((H[0][0] * H[1][1] - H[0][1] * H[1][0])
                * (V[0][0] * V[1][1] - V[0][1] * V[1][0])) < 0.0F) {
                if (W[0] < W[1]) {
                   V[0][0] *= -1.0F;
                   V[1][0] *= -1.0F;
                }
                else {
                   V[0][1] *= -1.0F;
                   V[1][1] *= -1.0F;
                }
             }
             for (int i = 0; (i < 2); i++) {
                for (int j = 0; (j < 2); j++) {
                   X[i][j] = 0.0F;
                   for (int k = 0; (k < 2); k++) {
                      X[i][j] += V[i][k] * H[j][k];
                   }
                }
             }
             X[0][2] = xSourceAverage - X[0][0] * xTargetAverage - X[0][1] * yTargetAverage;
             X[1][2] = ySourceAverage - X[1][0] * xTargetAverage - X[1][1] * yTargetAverage;
             break;
       }
       return(X);
    } /* end computeRotationMatrix */

    /*------------------------------------------------------------------*/
    /**
     * Compute the scale residues.
     * <p>
     * NOTE: At the input dx and dy have the residues so far, at the output these
     * residues are modified to account for the model at the new scale
     *
     * @param intervals input, number of intervals 
     * @param cx input, X b-spline coefficients
     * @param cy input, Y b-spline coefficients
     * @param dx input/output, X residues
     * @param dy input/output, Y residues
     * @param bIsReverse determines the transformation direction (target-source=FALSE or source-target=TRUE)
     */
    private void computeScaleResidues(
       int intervals,                                  // input, number of intervals
       final double [][]cx,                            // Input, spline coefficients
       final double [][]cy,
       final double []dx,                              // Input/Output. At the input it has the
       final double []dy,                              // residue so far, at the output this
                                                       // residue is modified to account for
                                                       // the model at the new scale
       boolean bIsReverse) 
    {

       double auxFactorWidth = target.getFactorWidth();
       double auxFactorHeight = target.getFactorHeight();
       bUnwarpJPointHandler auxSourcePh = this.sourcePh;
       bUnwarpJPointHandler auxTargetPh = this.targetPh;
       int auxTargetCurrentWidth = this.targetCurrentWidth;
       int auxTargetCurrentHeight = this.targetCurrentHeight;

       if(bIsReverse)
       {
           auxFactorWidth = source.getFactorWidth();
           auxFactorHeight = source.getFactorHeight();
           auxSourcePh = this.targetPh;
           auxTargetPh = this.sourcePh;
           auxTargetCurrentWidth = this.sourceCurrentWidth;
           auxTargetCurrentHeight = this.sourceCurrentHeight;
       }    

       // Set these coefficients to an interpolator
       bUnwarpJImageModel swx = new bUnwarpJImageModel(cx);
       bUnwarpJImageModel swy = new bUnwarpJImageModel(cy);

        // Get the list of landmarks
       Vector sourceVector=null;
       if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
       else                   sourceVector = new Vector();

       Vector targetVector = null;
       if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
       else                   targetVector = new Vector();
       final int K = targetVector.size();

       for (int k=0; k<K; k++) 
       {
           // Get the landmark coordinate in the target image
          final Point sourcePoint = (Point)sourceVector.elementAt(k);
          final Point targetPoint = (Point)targetVector.elementAt(k);
          double u = auxFactorWidth  * (double)targetPoint.x;
          double v = auxFactorHeight * (double)targetPoint.y;

          // Express it in "spline" units
          double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth  - 1) + 1.0F;
          double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;

          // Transform this coordinate to the source image
          swx.prepareForInterpolation(tu, tv, false); double x=swx.interpolateI();
          swy.prepareForInterpolation(tu, tv, false); double y=swy.interpolateI();

          // Substract the result from the residual
          dx[k] = auxFactorWidth *(double)sourcePoint.x - x;
          dy[k] = auxFactorHeight*(double)sourcePoint.y - y;
       }
    }

    /*--------------------------------------------------------------------------*/
    /**
     * This code is an excerpt from doRegistration() to compute the exact
     * number of steps.
     */
    private void computeTotalWorkload() 
    {
        // This code is an excerpt from doRegistration() to compute the exact
        // number of steps

        // Now refine with the different scales
        int state;  // state=-1 --> Finish
                    // state= 0 --> Increase deformation detail
                    // state= 1 --> Increase image detail
                    // state= 2 --> Do nothing until the finest image scale
        if (min_scale_deformation==max_scale_deformation) state=1;
        else                                              state=0;
        int s=min_scale_deformation;
        int currentDepth = target.getCurrentDepth();
        int workload=0;
        while (state!=-1) {
            // Update the deformation coefficients only in states 0 and 1
            if (state==0 || state==1) {
               // Optimize deformation coefficients
               if (imageWeight!=0)
                  workload+=300*(currentDepth+1);
            }

            // Prepare for next iteration
            switch (state) {
               case 0:
                  // Finer details in the deformation
                  if (s<max_scale_deformation) {
                   s++;
                    if (currentDepth>min_scale_image) state=1;
                    else                              state=0;
                 } else
                    if (currentDepth>min_scale_image) state=1;
                    else                              state=2;
                 break;
              case 1: // Finer details in the image, go on  optimizing
              case 2: // Finer details in the image, do not optimize
                  // Compute next state
                  if (state==1) {
                     if      (s==max_scale_deformation && currentDepth==min_scale_image) state=2;
                     else if (s==max_scale_deformation)                                  state=1;
                     else                                                                state=0;
                  } else if (state==2) {
                     if (currentDepth==0) state=-1;
                     else                 state= 2;
                  }

                // Pop another image and prepare the deformation
                  if (currentDepth!=0) currentDepth--;
                  break;
            }
       }
       bUnwarpJProgressBar.resetProgressBar();
       bUnwarpJProgressBar.addWorkload(workload);
    }

    /*--------------------------------------------------------------------------*/
    /**
     * Calulate the geometrical error commited with source-target and target-source
     * deformations.
     *
     * @param c_direct Input: Direct deformation coefficients
     * @param c_inverse Input: Inverse deformation coefficients
     * @param intervals Input: Number of intervals for the deformation
     * @param grad Output: Gradient of the function
     * @return error function value
     */

    private double evaluateConsistency(
        final double []c_direct,        
        final double []c_inverse,       
        final int intervals,            
              double []grad) 
    {    

       // Ask for memory for the transformation
       double [][] transformation_x_direct = new double [this.targetCurrentHeight][this.targetCurrentWidth];
       double [][] transformation_y_direct = new double [this.targetCurrentHeight][this.targetCurrentWidth];

       double [][] transformation_x_inverse = new double [this.sourceCurrentHeight][this.sourceCurrentWidth];
       double [][] transformation_y_inverse = new double [this.sourceCurrentHeight][this.sourceCurrentWidth];

       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim * cXdim;
       int twiceNk = 2 * Nk;    

       // Initialize gradient
       for (int k=0; k<grad.length; k++) 
           grad[k]=0.0F;   

       // Compute the deformation
       // Set these coefficients to an interpolator   
       bUnwarpJImageModel swx_direct = new bUnwarpJImageModel(c_direct, cYdim, cXdim, 0); //this.swxTargetToSource; 
       bUnwarpJImageModel swy_direct = new bUnwarpJImageModel(c_direct, cYdim, cXdim, Nk); //this.swyTargetToSource; 

       bUnwarpJImageModel swx_inverse = new bUnwarpJImageModel(c_inverse, cYdim, cXdim, 0); //this.swxSourceToTarget; 
       bUnwarpJImageModel swy_inverse = new bUnwarpJImageModel(c_inverse, cYdim, cXdim, Nk); // this.swySourceToTarget;

       swx_direct.precomputed_prepareForInterpolation(
          target.getCurrentHeight(), target.getCurrentWidth(), intervals);
       swy_direct.precomputed_prepareForInterpolation(
          target.getCurrentHeight(), target.getCurrentWidth(), intervals);

       swx_inverse.precomputed_prepareForInterpolation(
          source.getCurrentHeight(), source.getCurrentWidth(), intervals);
       swy_inverse.precomputed_prepareForInterpolation(
          source.getCurrentHeight(), source.getCurrentWidth(), intervals);

       // Compute the direct transformation mapping
       for (int v=0; v<this.targetCurrentHeight; v++) 
       {
          final double tv = (double)(v * intervals) / (double)(this.targetCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<this.targetCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(this.targetCurrentWidth - 1) + 1.0F;

             swx_direct.prepareForInterpolation(tu, tv, false); 
             transformation_x_direct[v][u] = swx_direct.interpolateI();

             swy_direct.prepareForInterpolation(tu, tv, false); 
             transformation_y_direct[v][u] = swy_direct.interpolateI();
          }
       }

        // Compute the inverse transformation mapping    
        for (int v=0; v<this.sourceCurrentHeight; v++) 
        {
          final double tv = (double)(v * intervals) / (double)(this.sourceCurrentHeight - 1) + 1.0F;
          for (int u = 0; u<this.sourceCurrentWidth; u++) 
          {
             final double tu = (double)(u * intervals) / (double)(this.sourceCurrentWidth - 1) + 1.0F;

             swx_inverse.prepareForInterpolation(tu, tv, false); 
             transformation_x_inverse[v][u] = swx_inverse.interpolateI();

             swy_inverse.prepareForInterpolation(tu, tv, false); 
             transformation_y_inverse[v][u] = swy_inverse.interpolateI();
          }
        }    

        // *********** Compute the geometrical error and gradient (DIRECT) ***********   
        double f_direct = 0;
        int n_direct = 0;
        for (int v=0; v<this.targetCurrentHeight; v++)
            for (int u=0; u<this.targetCurrentWidth; u++) 
            {                
                 // Check if this point is in the target mask
                 if (this.targetMsk.getValue(u/this.targetFactorWidth, v/this.targetFactorHeight)) 
                 {        

                     final int x = (int) Math.round(transformation_x_direct[v][u]);
                     final int y = (int) Math.round(transformation_y_direct[v][u]);

                     if (x>=0 && x<this.sourceCurrentWidth && y>=0 && y<this.sourceCurrentHeight) 
                     {
                        final double x2 = transformation_x_inverse[y][x];
                        final double y2 = transformation_y_inverse[y][x];
                        double aux1 = u - x2;
                        double aux2 = v - y2;

                        f_direct += aux1 * aux1 + aux2 * aux2;

                        // Compute the derivative with respect to all the c coefficients
                       // Derivatives from direct coefficients.
                       for (int l=0; l<4; l++)
                           for (int m=0; m<4; m++)                                                 
                           {                                               
                               if (swx_direct.prec_yIndex[v][l]==-1 || swx_direct.prec_xIndex[u][m]==-1) 
                                   continue;

                               double dddx = swx_direct.precomputed_getWeightI(l, m, u, v);                       
                               double dixx = swx_inverse.precomputed_getWeightDx(l, m, x, y);                                                                                            
                               double diyy = swy_inverse.precomputed_getWeightDy(l, m, x, y);

                               double weightIx = (dixx + diyy) * dddx;

                               double dddy = swy_direct.precomputed_getWeightI(l, m, u, v);                                              
                               double dixy = swx_inverse.precomputed_getWeightDy(l, m, x, y);
                               double diyx = swy_inverse.precomputed_getWeightDx(l, m, x, y);

                               double weightIy = (diyx + dixy) * dddy;                                              

                               int k = swx_direct.prec_yIndex[v][l] * cYdim + swx_direct.prec_xIndex[u][m];

                               // Derivative related to X deformation
                               grad[k]        += -aux1 * weightIx;

                               // Derivative related to Y deformation
                               grad[k+twiceNk]+= -aux2 * weightIy;
                           }


                       // Derivatives from inverse coefficients.
                       for (int l=0; l<4; l++)
                           for (int m=0; m<4; m++)                                                 
                           { 
                               // d inverse(direct(x)) / d c_inverse
                               if (swx_inverse.prec_yIndex[y][l]==-1 || swx_inverse.prec_xIndex[x][m]==-1) 
                                   continue;

                               double weightI = swx_inverse.precomputed_getWeightI(l, m, x, y);

                               int k = swx_inverse.prec_yIndex[y][l] * cYdim + swx_inverse.prec_xIndex[x][m];

                               // Derivative related to X deformation
                               grad[k+Nk]        += -aux1 * weightI;

                               // Derivative related to Y deformation
                               grad[k+Nk+twiceNk]+= -aux2 * weightI;
                           }


                        n_direct++; // Another point has been successfully evaluated
                     }   
                 }// end if mask.
            }      
        if(n_direct != 0) 
            f_direct /= (double) n_direct;               

        // Average the image related terms
        if (n_direct != 0) 
        {
           double aux = consistencyWeight * 2.0 / n_direct;  // This is the 2 coming from the
                                                             // derivative that I would do later
           for (int k=0; k<grad.length; k++) 
               grad[k] *= aux;
        } 

        // Inverse gradient
        double []vgrad = new double[grad.length];

        // Initialize gradient
        for (int k=0; k<vgrad.length; k++) 
           vgrad[k] = 0.0F;        
        
        // *********** Compute the geometrical error and gradient (INVERSE) ***********    
        double f_inverse = 0;
        int n_inverse = 0;
        for (int v=0; v<this.sourceCurrentHeight; v++)
            for (int u=0; u<this.sourceCurrentWidth; u++) 
            {
                // Check if this point is in the target mask
                if (this.sourceMsk.getValue(u/this.sourceFactorWidth, v/this.sourceFactorHeight)) 
                {
                    final int x = (int) Math.round(transformation_x_inverse[v][u]);
                    final int y = (int) Math.round(transformation_y_inverse[v][u]);

                    if (x>=0 && x<this.targetCurrentWidth && y>=0 && y<this.targetCurrentHeight) 
                    {
                       final double x2 = transformation_x_direct[y][x];
                       final double y2 = transformation_y_direct[y][x];
                       double aux1 = u - x2;
                       double aux2 = v - y2;

                       f_inverse += aux1 * aux1 + aux2 * aux2;

                       // Compute the derivative with respect to all the c coefficients
                       // Derivatives from direct coefficients.
                       for (int l=0; l<4; l++)
                           for (int m=0; m<4; m++)                                                 
                           { 
                               // d direct(inverse(x)) / d c_direct
                               if (swx_direct.prec_yIndex[y][l]==-1 || swx_direct.prec_xIndex[x][m]==-1) 
                                   continue;

                               double weightI = swx_direct.precomputed_getWeightI(l, m, x, y);

                               int k = swx_direct.prec_yIndex[y][l] * cYdim + swx_direct.prec_xIndex[x][m];

                               // Derivative related to X deformation
                               vgrad[k]        += -aux1 * weightI;

                               // Derivative related to Y deformation
                               vgrad[k+twiceNk]+= -aux2 * weightI;
                           }
                       // Derivatives from inverse coefficients.
                       for (int l=0; l<4; l++)
                           for (int m=0; m<4; m++)                                                 
                           {                        
                               if (swx_inverse.prec_yIndex[v][l]==-1 || swx_inverse.prec_xIndex[u][m]==-1) 
                                   continue;

                               double diix = swx_inverse.precomputed_getWeightI(l, m, u, v);                       
                               double ddxx = swx_direct.precomputed_getWeightDx(l, m, x, y);                                                                                            
                               double ddyy = swy_direct.precomputed_getWeightDy(l, m, x, y);

                               double weightIx = (ddxx + ddyy) * diix;

                               double diiy = swy_inverse.precomputed_getWeightI(l, m, u, v);
                               double ddxy = swx_direct.precomputed_getWeightDy(l, m, x, y);
                               double ddyx = swy_direct.precomputed_getWeightDx(l, m, x, y);                                              

                               double weightIy = (ddyx + ddxy) * diiy;


                               int k = swx_inverse.prec_yIndex[v][l] * cYdim + swx_inverse.prec_xIndex[u][m];

                               // Derivative related to X deformation
                               vgrad[k+Nk]        += -aux1 * weightIx;

                               // Derivative related to Y deformation
                               vgrad[k+Nk+twiceNk]+= -aux2 * weightIy;
                           }

                       n_inverse++; // Another point has been successfully evaluated
                    }    
                } // end if mask
            }

        if(n_inverse != 0)
            f_inverse /= (double) n_inverse;       
        
        // Average the image related terms
        if (n_inverse != 0) 
        {
           double aux = consistencyWeight * 2.0 / n_inverse;  // This is the 2 coming from the
                                                              // derivative that I would do later
           for(int k=0; k<vgrad.length; k++) 
               vgrad[k] *= aux;
        }

        // Sum of both gradients (direct and inverse)
        for(int k=0; k<grad.length; k++) 
            grad[k] += vgrad[k];

        
        this.partialDirectConsitencyError = f_direct;
        this.partialInverseConsitencyError = f_inverse;        
        
        double consistencyDirectError = (n_direct == 0) ? 1.0/FLT_EPSILON : (this.consistencyWeight * f_direct);
        double consistencyInverseError = (n_inverse == 0) ? 1.0/FLT_EPSILON : (this.consistencyWeight * f_inverse);
        
        if (showMarquardtOptim) 
        {
            IJ.write("    Consistency Error (s-t): " + consistencyDirectError);
            IJ.write("    Consistency Error (t-s): " + consistencyInverseError);
        }
                
        
        if(n_direct == 0 || n_inverse == 0)
            return 1/FLT_EPSILON;
        return (this.consistencyWeight * (f_direct + f_inverse));
    }

    /*--------------------------------------------------------------------------*/
    /**
     * Energy function to be minimized by the optimizer.
     *
     * @param c Input: Deformation coefficients
     * @param intervals Input: Number of intervals for the deformation
     * @param grad Output: Gradient of the function
     * @param only_image Input: if true, only the image term is considered and not the regularization
     * @param show_error Input: if true, an image is shown with the error
     * @return value of the energy function for these deformation coefficients
     */
    private double energyFunction(
       final double []c,            
       final int      intervals,    
             double []grad,                                             
       final boolean  only_image,   
       final boolean  show_error) 
    {
        final int twiceM = c.length;
        final int M = c.length / 2;
        final int halfM = M / 2;
        double []x1 = new double [M];    
        double []auxGrad1 = new double [M];
        double []auxGrad2 = new double [M];

        for(int i = 0, p = 0; i<halfM; i++, p++)
        {
            x1[p] = c[i];
            x1[p + halfM] = c[i + M];        
        }

        // Source to Target evaluation
        double f = evaluateSimilarity(x1, intervals, auxGrad1, only_image, show_error, false);

        this.partialDirectSimilarityError = f;
        
        double []x2 = new double [M];
        for(int i = halfM, p = 0; i<M; i++, p++)
        {
            x2[p] = c[i];
            x2[p + halfM] = c[i + M];
        }    

        // Target to Source evaluation
        this.partialInverseSimilarityError = evaluateSimilarity(x2, intervals, auxGrad2, only_image, show_error, true);        

        f += this.partialInverseSimilarityError;
        
        // Gradient composition.
        for(int i = 0, p = 0; i<halfM; i++, p++)
        {
            grad[p] = auxGrad1[i];
            grad[p + halfM] = auxGrad2[i];
            grad[p + M] = auxGrad1[i + halfM];
            grad[p + M + halfM] = auxGrad2[i + halfM];       
        }        

        double f_consistency = 0;

//        if(this.consistencyWeight != 0)
//        {
            // Consistency gradient.
            double []vgradcons = new double[grad.length];                

            f_consistency = evaluateConsistency(x1, x2, intervals, vgradcons);                

            // Update gradient.
            for(int i = 0; i < grad.length; i++)
                grad[i] += vgradcons[i];
//        }

        return f + f_consistency;
    }

    /*--------------------------------------------------------------------------*/
    /**
     * Evaluate the similarity between the source and the target images.
     *
     * @param c Input: Deformation coefficients
     * @param intervals Input: Number of intervals for the deformation
     * @param grad Output: Gradient of the similarity
     * @param only_image Input: if true, only the image term is considered and not the regularization
     * @param show_error Input: if true, an image is shown with the error
     * @param bIsReverse Input: flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
     * @return images similarity value
     */
    private double evaluateSimilarity(
       final double []c,          
       final int      intervals,  
             double []grad,       
       final boolean  only_image,   
                                    
       final boolean  show_error,   
       boolean bIsReverse) 
    {

       // Auxiliar variables for changing from source to target and inversely
       bUnwarpJImageModel auxTarget = target;
       bUnwarpJImageModel auxSource = source;

       bUnwarpJMask auxTargetMsk = targetMsk;
       bUnwarpJMask auxSourceMsk = sourceMsk;

       bUnwarpJPointHandler auxTargetPh = targetPh;
       bUnwarpJPointHandler auxSourcePh = sourcePh;

       bUnwarpJImageModel swx = swxTargetToSource;
       bUnwarpJImageModel swy = swyTargetToSource;

       double auxFactorWidth = this.target.getFactorWidth();
       double auxFactorHeight = this.target.getFactorHeight();

       double P11[][] = this.P11_TargetToSource;
       double P12[][] = this.P12_TargetToSource;
       double P22[][] = this.P22_TargetToSource;

       int auxTargetCurrentWidth = this.targetCurrentWidth; 
       int auxTargetCurrentHeight = this.targetCurrentHeight; 

       // Change if necessary
       if(bIsReverse)
       {
           auxSource = target;
           auxTarget = source;

           auxSourceMsk = targetMsk;
           auxTargetMsk = sourceMsk;

           auxSourcePh = targetPh;
           auxTargetPh = sourcePh;

           swx = swxSourceToTarget;
           swy = swySourceToTarget;

           auxFactorWidth = this.sourceFactorWidth;
           auxFactorHeight = this.sourceFactorHeight;

           P11 = this.P11_SourceToTarget;
           P12 = this.P12_SourceToTarget;
           P22 = this.P22_SourceToTarget;

           auxTargetCurrentWidth = this.sourceCurrentWidth;
           auxTargetCurrentHeight = this.sourceCurrentHeight;
       }

       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim * cXdim;
       int twiceNk = 2 * Nk;
       double []vgradreg = new double[grad.length];
       double []vgradland = new double[grad.length];

       // Set the transformation coefficients to the interpolator
       swx.setCoefficients(c, cYdim, cXdim, 0);
       swy.setCoefficients(c, cYdim, cXdim, Nk);

       // Initialize gradient
       for (int k=0; k<twiceNk; k++) vgradreg[k]=vgradland[k]=grad[k]=0.0F;

       // Estimate the similarity and gradient between both images
       double imageSimilarity=0.0;
       int Ydim = auxTarget.getCurrentHeight();
       int Xdim = auxTarget.getCurrentWidth();

       // Prepare to show
       double [][]error_image=null;
       double [][]div_error_image=null;
       double [][]curl_error_image=null;
       double [][]laplacian_error_image=null;
       double [][]jacobian_error_image=null;
       if (show_error) 
       {
          error_image = new double[Ydim][Xdim];
          div_error_image = new double[Ydim][Xdim];
          curl_error_image = new double[Ydim][Xdim];
          laplacian_error_image = new double[Ydim][Xdim];
          jacobian_error_image = new double[Ydim][Xdim];
          for (int v=0; v<Ydim; v++)
             for (int u=0; u<Xdim; u++)
                error_image[v][u]=div_error_image[v][u]=curl_error_image[v][u]=
                   laplacian_error_image[v][u]=jacobian_error_image[v][u]=-1.0;
       }

       // Loop over all points in the source image
       int n=0;
       if (imageWeight!=0 || show_error) 
       {
          double []xD2 = new double[3]; // Some space for the second derivatives
          double []yD2 = new double[3]; // of the transformation
          double []xD  = new double[2]; // Some space for the second derivatives
          double []yD  = new double[2]; // of the transformation
          double []I1D = new double[2]; // Space for the first derivatives of I1
          double hx = (Xdim-1)/intervals;   // Scale in the X axis
          double hy = (Ydim-1)/intervals;   // Scale in the Y axis

          double []targetCurrentImage = auxTarget.getCurrentImage();


          int uv=0;
          for (int v=0; v<Ydim; v++) 
          {
              for (int u=0; u<Xdim; u++, uv++) 
              {
                   // Compute image term .....................................................

                   // Check if this point is in the target mask
                   if (auxTargetMsk.getValue(u/auxFactorWidth, v/auxFactorHeight)) 
                   {
                     // Compute value in the source image
                     double I2 = targetCurrentImage[uv];

                     // Compute the position of this point in the target
                     double x = swx.precomputed_interpolateI(u,v);
                     double y = swy.precomputed_interpolateI(u,v);

                     // Check if this point is in the source mask
                     if (auxSourceMsk.getValue(x/auxFactorWidth, y/auxFactorHeight)) 
                     {
                        // Compute the value of the target at that point
                        auxSource.prepareForInterpolation(x, y, PYRAMID); 
                        double I1 = auxSource.interpolateI();
                        auxSource.interpolateD(I1D); 
                        double I1dx = I1D[0], I1dy = I1D[1];

                        double error = I2 - I1;
                        double error2 = error*error;
                        if (show_error) error_image[v][u]=error;
                        imageSimilarity+=error2;

                        // Compute the derivative with respect to all the c coefficients
                        // Cost of the derivatives = 16*(3 mults + 2 sums)
                        // Current cost= 359 mults + 346 sums
                        for (int l=0; l<4; l++)
                          for (int m=0; m<4; m++) 
                          {
                             if (swx.prec_yIndex[v][l]==-1 || swx.prec_xIndex[u][m]==-1) continue;

                             double weightI = swx.precomputed_getWeightI(l,m,u,v);

                             int k = swx.prec_yIndex[v][l] * cYdim + swx.prec_xIndex[u][m];

                             // Compute partial result
                             // There's also a multiplication by 2 that I will
                             // do later
                             double aux = -error * weightI;

                             // Derivative related to X deformation
                             grad[k]   += aux*I1dx;

                             // Derivative related to Y deformation
                             grad[k+Nk]+= aux*I1dy;
                          }
                       n++; // Another point has been successfully evaluated
                    }
                 }

               // Show regularization images ...........................................
                if (show_error) 
                {
                  double gradcurlx=0.0, gradcurly=0.0;
                  double graddivx =0.0, graddivy =0.0;
                  double xdx  =0.0, xdy  =0.0,
                         ydx  =0.0, ydy  =0.0,
                         xdxdy=0.0, xdxdx=0.0, xdydy=0.0,
                         ydxdy=0.0, ydxdx=0.0, ydydy=0.0; 

                  // Compute the first derivative terms
                  swx.precomputed_interpolateD(xD,u,v); xdx=xD[0]/hx; xdy=xD[1]/hy; 
                  swy.precomputed_interpolateD(yD,u,v); ydx=yD[0]/hx; ydy=yD[1]/hy; 

                  // Compute the second derivative terms
                  swx.precomputed_interpolateD2(xD2,u,v);
                  xdxdy=xD2[0]; xdxdx=xD2[1]; xdydy=xD2[2]; 
                  swy.precomputed_interpolateD2(yD2,u,v);
                  ydxdy=yD2[0]; ydxdx=yD2[1]; ydydy=yD2[2]; 

                  // Error in the divergence
                  graddivx=xdxdx+ydxdy;
                  graddivy=xdxdy+ydydy;

                  double graddiv = graddivx*graddivx + graddivy*graddivy;
                  double errorgraddiv = divWeight*graddiv;

                  if (divWeight!=0) div_error_image [v][u]=errorgraddiv;
                  else              div_error_image [v][u]=graddiv;

                  // Compute error in the curl
                  gradcurlx = -xdxdy+ydxdx;
                  gradcurly = -xdydy+ydxdy;
                  double gradcurl = gradcurlx*gradcurlx + gradcurly*gradcurly;
                  double errorgradcurl = curlWeight*gradcurl;

                  if (curlWeight!=0) curl_error_image[v][u]=errorgradcurl;
                  else               curl_error_image[v][u]=gradcurl;

                  // Compute Laplacian error
                  laplacian_error_image[v][u] =xdxdx*xdxdx;
                  laplacian_error_image[v][u]+=xdxdy*xdxdy;
                  laplacian_error_image[v][u]+=xdydy*xdydy;
                  laplacian_error_image[v][u]+=ydxdx*ydxdx;
                  laplacian_error_image[v][u]+=ydxdy*ydxdy;
                  laplacian_error_image[v][u]+=ydydy*ydydy;

                  // Compute jacobian error
                  jacobian_error_image[v][u] =xdx*ydy-xdy*ydx;
                }
            }
           }
       }

        // Average the image related terms
        if (n!=0) 
        {
           imageSimilarity *= imageWeight/n;
           double aux = imageWeight * 2.0/n; // This is the 2 coming from the
                                         // derivative that I would do later
           for (int k=0; k<twiceNk; k++) grad[k]*=aux;
        } else 
           if (imageWeight==0) imageSimilarity = 0;
           else                imageSimilarity = 1/FLT_EPSILON;

        // Compute regularization term .............................................. 
        double regularization = 0.0;
        if (!only_image) 
        {
           for (int i=0; i<Nk; i++)
               for (int j=0; j<Nk; j++) {
                   regularization+=c[   i]*P11[i][j]*c[   j]+// c1^t P11 c1
                                   c[Nk+i]*P22[i][j]*c[Nk+j]+// c2^t P22 c2
                                   c[   i]*P12[i][j]*c[Nk+j];// c1^t P12 c2
                   vgradreg[   i]+=2*P11[i][j]*c[j];         // 2 P11 c1
                   vgradreg[Nk+i]+=2*P22[i][j]*c[Nk+j];      // 2 P22 c2
                   vgradreg[   i]+=  P12[i][j]*c[Nk+j];      //   P12 c2
                   vgradreg[Nk+i]+=  P12[j][i]*c[   j];      //   P12^t c1
               }
           regularization*=1.0/(Ydim*Xdim);
           for (int k=0; k<twiceNk; k++) vgradreg [k]*=1.0/(Ydim*Xdim);
        }

        // Compute landmark error and derivative ...............................
        // Get the list of landmarks
        double landmarkError=0.0;
        int K = 0;
        if (auxTargetPh!=null) K = auxTargetPh.getPoints().size();
        if (landmarkWeight!=0) 
        {
          Vector sourceVector=null;
          if (auxSourcePh!=null) sourceVector = auxSourcePh.getPoints();
          else                   sourceVector = new Vector();
          Vector targetVector = null;
          if (auxTargetPh!=null) targetVector = auxTargetPh.getPoints();
          else                   targetVector = new Vector();

          for (int kp=0; kp<K; kp++) 
          {
              // Get the landmark coordinate in the target image
             final Point sourcePoint = (Point)sourceVector.elementAt(kp);
             final Point targetPoint = (Point)targetVector.elementAt(kp);
             double u = auxFactorWidth *(double)targetPoint.x;
             double v = auxFactorHeight*(double)targetPoint.y;

             // Express it in "spline" units
             double tu = (double)(u * intervals) / (double)(auxTargetCurrentWidth  - 1) + 1.0F;
             double tv = (double)(v * intervals) / (double)(auxTargetCurrentHeight - 1) + 1.0F;

             // Transform this coordinate to the source image
             swx.prepareForInterpolation(tu, tv, false); double x=swx.interpolateI();
             swy.prepareForInterpolation(tu, tv, false); double y=swy.interpolateI();

             // Substract the result from the residual
             double dx=auxFactorWidth *(double)sourcePoint.x - x;
             double dy=auxFactorHeight*(double)sourcePoint.y - y;

             // Add to landmark error
             landmarkError += dx*dx + dy*dy;

             // Compute the derivative with respect to all the c coefficients
             for (int l=0; l<4; l++)
                for (int m=0; m<4; m++) 
                {
                   if (swx.yIndex[l]==-1 || swx.xIndex[m]==-1) continue;
                   int k=swx.yIndex[l]*cYdim+swx.xIndex[m];

                   // There's also a multiplication by 2 that I will do later
                   // Derivative related to X deformation
                   vgradland[k]   -=dx*swx.getWeightI(l,m);

                   // Derivative related to Y deformation
                   vgradland[k+Nk]-=dy*swy.getWeightI(l,m);
                }
          }
       }

       if (K!=0) 
       {
          landmarkError*=landmarkWeight/K;
          double aux=2.0*landmarkWeight/K;
                              // This is the 2 coming from the derivative
                              // computation that I would do at the end
          for (int k=0; k<twiceNk; k++) vgradland[k]*=aux;
       }
       if (only_image) landmarkError=0;

       // Finish computations .............................................................
       // Add all gradient terms
       for (int k=0; k<twiceNk; k++)
           grad[k]+=vgradreg[k]+vgradland[k];

       if (show_error) 
       {
          bUnwarpJMiscTools.showImage("Error",error_image);
          bUnwarpJMiscTools.showImage("Divergence Error",div_error_image);
          bUnwarpJMiscTools.showImage("Curl Error",curl_error_image);
          bUnwarpJMiscTools.showImage("Laplacian Error",laplacian_error_image);
          bUnwarpJMiscTools.showImage("Jacobian Error",jacobian_error_image);
       }

       if (showMarquardtOptim) 
       {
          String s = bIsReverse ? new String("(t-s)") : new String("(s-t)");
          if (imageWeight!=0)                IJ.write("    Image          error " + s + ": " + imageSimilarity);
          if (landmarkWeight!=0)             IJ.write("    Landmark       error " + s + ": " + landmarkError);
          if (divWeight!=0 || curlWeight!=0) IJ.write("    Regularization error " + s + ": " + regularization);
       }
       return imageSimilarity + landmarkError + regularization;
    }

    /*--------------------------------------------------------------------------*/
    /**
     * In this function the system (H+lambda*Diag(H))*update=gradient
     *     is solved for update.
     *     H is the hessian of the function f,
     *     gradient is the gradient of the function f,
     *     Diag(H) is a matrix with the diagonal of H.
     */
    private void Marquardt_it (
       double   []x,
       boolean  []optimize,
       double   []gradient,
       double   []Hessian,
       double     lambda)
    {    
        final double TINY  = FLT_EPSILON;
        final int   M      = x.length;       
        
        // Find the threshold for the most important components
        double []sortedgradient= new double [M];
        for (int i = 0; i < M; i++) 
            sortedgradient[i] = Math.abs(gradient[i]);
        Arrays.sort(sortedgradient);

        double largestGradient = sortedgradient[M-1];
        //System.out.println("largestGradient = " + largestGradient);
        
        // We set the threshold gradient at 9% of the largest value.
        double gradient_th = 0.09 * largestGradient;
        
        // We count the number of values over the threshold.
        int Mused = 0;
        for(int i = 0; i < M; i++)
            if(sortedgradient[i] >= gradient_th) 
                Mused++;
        
        //System.out.println("Mused = " + Mused);
        
        double [][] u         = new double  [Mused][Mused];
        double [][] v         = null; //new double  [Mused][Mused];
        double   [] w         = null; //new double  [Mused];
        double   [] g         = new double  [Mused];
        double   [] update    = new double  [Mused];
        boolean  [] optimizep = new boolean [M];

        System.arraycopy(optimize,0,optimizep,0,M);
        
        lambda+=1.0F;

  
        int m = 0, i;

        // Take the Mused components with big gradients
        for (i=0; i<M; i++)
           if  (optimizep[i] && Math.abs(gradient[i])>=gradient_th) {
               m++; 
               if (m==Mused) break;
           } 
           else 
               optimizep[i]=false;
        // Set the rest to 0
        for (i=i+1; i<M; i++) 
           optimizep[i]=false;
        

        // Gradient descent
        //for (int i=0; i<M; i++) if (optimizep[i]) x[i]-=0.01*gradient[i];
        //if (true) return;

        /* u will be a copy of the Hessian where we take only those
           components corresponding to variables being optimized */
        int kr=0, iw=0;
        for (int ir = 0; ir<M; kr=kr+M,ir++) 
        {
          if (optimizep[ir]) 
          {
             int jw=0;
             for (int jr = 0; jr<M; jr++)
                if (optimizep[jr]) u[iw][jw++] = Hessian[kr + jr];
             g[iw]=gradient[ir];
             u[iw][iw] *= lambda;
             iw++;
          }
        }

        // Solve he equation system
        /* SVD u=u*w*v^t */
        update=bUnwarpJMathTools.linearLeastSquares(u,g);

        /* x = x - update */
        kr=0;
        for (int kw = 0; kw<M; kw++)
          if (optimizep[kw]) x[kw] -=  update[kr++];        
        
    } /* end Marquardt_it */

    /*--------------------------------------------------------------------------*/
    /**
     * Optimize the b-spline coefficients.
     *
     * @param intervals number of intervals in the deformation
     * @param thChangef
     * @param cxTargetToSource x- b-spline coefficients storing the target to source deformation
     * @param cyTargetToSource y- b-spline coefficients storing the target to source deformation
     * @param cxSourceToTarget x- b-spline coefficients storing the source to target deformation
     * @param cySourceToTarget y- b-spline coefficients storing the source to target deformation
     */
    private double optimizeCoeffs(
       int intervals,
       double thChangef,
       double [][]cxTargetToSource,
       double [][]cyTargetToSource,       
       double [][]cxSourceToTarget,
       double [][]cySourceToTarget)
    {
       if (dialog!=null && dialog.isStopRegistrationSet()) 
           return 0.0;

       final double TINY               = FLT_EPSILON;
       final double EPS                = 3.0e-8F;
       final double FIRSTLAMBDA        = 1;
       final int    MAXITER_OPTIMCOEFF = 300;
       final int    CUMULATIVE_SIZE    = 5;

       int int3 = intervals + 3;
       int halfM = 2 * int3 * int3;
       int quarterM = halfM / 2;
       int threeQuarterM = quarterM * 3;
       int M = halfM * 2;

       double   rescuedf, f;
       double   []x            = new double   [M];
       double   []rescuedx     = new double   [M];
       double   []diffx        = new double   [M];
       double   []rescuedgrad  = new double   [M];
       double   []grad         = new double   [M];
       double   []diffgrad     = new double   [M];
       double   []Hdx          = new double   [M];
       double   []rescuedhess  = new double   [M*M];
       double   []hess         = new double   [M*M];
       double   []safehess     = new double   [M*M];
       double   []proposedHess = new double   [M*M];
       boolean  []optimize     = new boolean  [M];
       int        i, j, p, iter = 1;
       boolean    skip_update, ill_hessian;
       double     improvementx = (double)Math.sqrt(TINY),
                  lambda = FIRSTLAMBDA, max_normx, distx, aux, gmax;
       double     fac, fae, dgdx, dxHdx, sumdiffg, sumdiffx;

       bUnwarpJCumulativeQueue lastBest=
          new bUnwarpJCumulativeQueue(CUMULATIVE_SIZE);

       for (i=0; i<M; i++) optimize[i] = true;

       /* Form the vector with the current guess for the optimization */
       for (i= 0, p=0; i < intervals + 3; i++)
           for (j = 0; j < intervals + 3; j++, p++) 
           {
             x[      p] = cxTargetToSource[i][j];
             x[quarterM+p] = cxSourceToTarget[i][j];

             x[halfM+p] = cyTargetToSource[i][j];
             x[threeQuarterM+p] = cySourceToTarget[i][j];
           }

       /* Prepare the precomputed weights for interpolation */
       this.swxTargetToSource = new bUnwarpJImageModel(x, intervals+3, intervals+3, 0);
       this.swyTargetToSource = new bUnwarpJImageModel(x, intervals+3, intervals+3, halfM);
       this.swxTargetToSource.precomputed_prepareForInterpolation(
          target.getCurrentHeight(), target.getCurrentWidth(), intervals);
       this.swyTargetToSource.precomputed_prepareForInterpolation(
          target.getCurrentHeight(), target.getCurrentWidth(), intervals);

       this.swxSourceToTarget = new bUnwarpJImageModel(x, intervals+3, intervals+3, quarterM);
       this.swySourceToTarget = new bUnwarpJImageModel(x, intervals+3, intervals+3, threeQuarterM);
       this.swxSourceToTarget.precomputed_prepareForInterpolation(
          source.getCurrentHeight(), source.getCurrentWidth(), intervals);
       this.swySourceToTarget.precomputed_prepareForInterpolation(
          source.getCurrentHeight(), source.getCurrentWidth(), intervals);


       /* First computation of the energy */
       f = energyFunction(x, intervals, grad, false, false);  

       if (showMarquardtOptim) IJ.write("f(1)="+f);

       /* Initially the hessian is the identity matrix multiplied by
          the first function value */
       for (i=0,p=0; i<M; i++)
          for (j=0; j<M; j++,p++)
             if (i==j) hess[p]=1.0F; 
             else hess[p]=0.0F;

       rescuedf    = f;
       for (i=0,p=0; i<M; i++) {
          rescuedx[i]=x[i];
          rescuedgrad[i]=grad[i];
          for (j=0; j<M; j++,p++) 
              rescuedhess[p]=hess[p];
       }

       // Ignacio added "2*"
       int maxiter = MAXITER_OPTIMCOEFF * (source.getCurrentDepth() + 1);

       bUnwarpJProgressBar.stepProgressBar();

       int last_successful_iter=0;

       boolean stop = dialog != null && dialog.isStopRegistrationSet();

       while (iter < maxiter && !stop) 
       {
           /* Compute new x ------------------------------------------------- */
          Marquardt_it(x, optimize, grad, hess, lambda);

          /* Stopping criteria --------------------------------------------- */
          /* Compute difference with the previous iteration */
          max_normx=improvementx=0;
          for (i=0; i<M; i++) 
          {
             diffx[i] = x[i] - rescuedx[i];
             distx = Math.abs(diffx[i]);
             improvementx += distx*distx;
             aux = Math.abs(rescuedx[i]) < Math.abs(x[i]) ? x[i] : rescuedx[i];
             max_normx += aux*aux;
          }
          
          if (TINY < max_normx) improvementx = improvementx/max_normx;
          
          improvementx = (double) Math.sqrt(Math.sqrt(improvementx));

          /* If there is no change with respect to the old geometry then
             finish the iterations */
          if (improvementx < Math.sqrt(TINY)) break;

          /* Estimate the new function value -------------------------------- */
          f = energyFunction(x, intervals, grad, false, false);
          iter++;
          if (showMarquardtOptim) IJ.write("f("+iter+")="+f+" lambda="+lambda);
          bUnwarpJProgressBar.stepProgressBar();

          /* Update lambda -------------------------------------------------- */
          if (rescuedf > f) 
          {
              /* Check if the improvement is only residual */
              lastBest.push_back(rescuedf-f);
              if (lastBest.currentSize()==CUMULATIVE_SIZE && lastBest.getSum()/f<thChangef)
                 break;

              // We save the last energy terms values in order to be displayed.
              this.finalDirectConsistencyError = this.partialDirectConsitencyError;
              this.finalDirectSimilarityError = this.partialDirectSimilarityError;
              this.finalInverseConsistencyError = this.partialInverseConsitencyError;
              this.finalInverseSimilarityError = this.partialInverseSimilarityError;
              
              /* If we have improved then estimate the hessian, 
                 update the geometry, and decrease the lambda */
              /* Estimate the hessian ....................................... */
              if (showMarquardtOptim) IJ.write("  Accepted");
              if ((last_successful_iter++%10)==0 && outputLevel>-1)
                 update_outputs(x, intervals);

              /* Estimate the difference between gradients */
              for (i=0; i<M; i++) diffgrad[i]=grad[i]-rescuedgrad[i];

              /* Multiply this difference by the current inverse of the hessian */
              for (i=0, p=0; i<M; i++) {
                 Hdx[i]=0.0F;
                 for (j=0; j<M; j++, p++) Hdx[i]+=hess[p]*diffx[j];
              }

              /* Calculate dot products for the denominators ................ */
              dgdx=dxHdx=sumdiffg=sumdiffx=0.0F;
              skip_update=true;
              for (i=0; i<M; i++) {
                 dgdx     += diffgrad[i]*diffx[i];
                 dxHdx    += diffx[i]*Hdx[i];
                 sumdiffg += diffgrad[i]*diffgrad[i];
                 sumdiffx += diffx[i]*diffx[i];
                 if (Math.abs(grad[i])>=Math.abs(rescuedgrad[i])) gmax=Math.abs(grad[i]);
                 else                                             gmax=Math.abs(rescuedgrad[i]);
                 if (gmax!=0 && Math.abs(diffgrad[i]-Hdx[i])>Math.sqrt(EPS)*gmax)
                    skip_update=false;
              }

              /* Update hessian ............................................. */
              /* Skip if fac not sufficiently positive */
              if (dgdx>Math.sqrt(EPS*sumdiffg*sumdiffx) && !skip_update) {
                 fae=1.0F/dxHdx;
                 fac=1.0F/dgdx;

                 /* Update the hessian after BFGS formula */
                for (i=0, p=0; i<M; i++)
                   for (j=0; j<M; j++, p++) {
                      if (i<=j) proposedHess[p]=hess[p]+
                         fac*diffgrad[i]*diffgrad[j]
                        -fae*(Hdx[i]*Hdx[j]);
                      else proposedHess[p]=proposedHess[j*M+i];
                   }

                ill_hessian=false;
                if (!ill_hessian) {
                   for (i=0, p=0; i<M; i++)
                       for (j=0; j<M; j++,p++)
                           hess[p]= proposedHess[p];
                } else
                   if (showMarquardtOptim)
                          IJ.write("Hessian cannot be safely updated, ill-conditioned");

              } else
                if (showMarquardtOptim)
                    IJ.write("Hessian cannot be safely updated");

              /* Update geometry and lambda ................................. */
             rescuedf    = f;
             for (i=0,p=0; i<M; i++) {
                rescuedx[i]=x[i];
                rescuedgrad[i]=grad[i];
                for (j=0; j<M; j++,p++) rescuedhess[p]=hess[p];
             }
             if (1e-4 < lambda) lambda = lambda/10;
          } else {
          /* else, if it is worse, then recover the last geometry
             and increase lambda, saturate lambda with FIRSTLAMBDA */
             for (i=0,p=0; i<M; i++) {
                x[i]=rescuedx[i];
                grad[i]=rescuedgrad[i];
                for (j=0; j<M; j++,p++) hess[p]=rescuedhess[p];
             }
             if (lambda < 1.0/TINY) lambda*=10;
             else break;
             if (lambda < FIRSTLAMBDA) lambda = FIRSTLAMBDA;
          }

            stop=dialog!=null && dialog.isStopRegistrationSet();
        }

       // Copy the values back to the input arrays
       for (i= 0, p=0; i<intervals + 3; i++)
           for (j = 0; j < intervals + 3; j++, p++) 
           {
             cxTargetToSource[i][j] = x[      p];
             cxSourceToTarget[i][j] = x[quarterM+p];

             cyTargetToSource[i][j] = x[halfM+p];
             cySourceToTarget[i][j] = x[threeQuarterM+p];
           }

       bUnwarpJProgressBar.skipProgressBar(maxiter-iter);
       return f;
    }

    /*-----------------------------------------------------------------------------*/
    /**
     * Propagate deformation coefficients to the next level.
     *
     * @param intervals number of intervals in the deformation
     * @param c b-spline coefficients
     * @param expansionFactor due to the change of size in the represented image
     * @return propagated coefficients
     */
    private double [][] propagateCoeffsToNextLevel(
       int intervals,
       final double [][]c,
       double expansionFactor)
    {
       // Expand the coefficients for the next scale
       intervals*=2;
       double [][] cs_expand = new double[intervals+7][intervals+7];

       // Upsample
       for (int i=0; i<intervals+7; i++)
          for (int j=0; j<intervals+7; j++) {
              // If it is not in an even sample then set it to 0
              if (i%2 ==0 || j%2 ==0) cs_expand[i][j]=0.0F;
              else {
                 // Now look for this sample in the coarser level
                 int ipc=(i-1)/2;
                 int jpc=(j-1)/2;
                 cs_expand[i][j]=c[ipc][jpc];
              }
          }

       // Define the FIR filter
       double [][] u2n=new double [4][];
       u2n[0]=null;
       u2n[1]=new double[3]; u2n[1][0]=0.5F; u2n[1][1]=1.0F; u2n[1][2]=0.5F;
       u2n[2]=null;
       u2n[3]=new double[5]; u2n[3][0]=0.125F; u2n[3][1]=0.5F; u2n[3][2]=0.75F;
           u2n[3][3]=0.5F; u2n[3][4]=0.125F; 
       int [] half_length_u2n={0, 1, 0, 2};
       int kh=half_length_u2n[transformationSplineDegree];

       // Apply the u2n filter to rows
       double [][] cs_expand_aux = new double[intervals+7][intervals+7];

       for (int i=1; i<intervals+7; i+=2)
          for (int j=0; j<intervals+7; j++) {
              cs_expand_aux[i][j]=0.0F;
              for (int k=-kh; k<=kh; k++)
                  if (j+k>=0 && j+k<=intervals+6)
                     cs_expand_aux[i][j]+=u2n[transformationSplineDegree][k+kh]*cs_expand[i][j+k];
          }

       // Apply the u2n filter to columns
       for (int i=0; i<intervals+7; i++)
          for (int j=0; j<intervals+7; j++) {
              cs_expand[i][j]=0.0F;
              for (int k=-kh; k<=kh; k++)
                  if (i+k>=0 && i+k<=intervals+6)
                     cs_expand[i][j]+=u2n[transformationSplineDegree][k+kh]*cs_expand_aux[i+k][j];
          }

       // Copy the central coefficients to c
       double [][]newc=new double [intervals+3][intervals+3];
       for (int i= 0; i<intervals+3; i++)
          for (int j = 0; j <intervals + 3; j++)
              newc[i][j]=cs_expand[i+2][j+2]*expansionFactor;

       // Return the new set of coefficients
       return newc;
    }

    /*------------------------------------------------------------------*/
    /**
     * Save the transformation.
     *
     * @param intervals number of intervals in the deformation
     * @param cx x- deformation coefficients
     * @param cy y- deformation coefficients
     * @param bIsReverse flat to determine the transformation direction
     */ 
    private void saveTransformation(
       int intervals,
       double [][]cx,
       double [][]cy,
       boolean bIsReverse) 
    {   
       String filename = fn_tnf_1;
       
       if(bIsReverse) filename = fn_tnf_2;
       
       if (filename.equals("")) 
       {
          // Get the filename to save
          File dir=new File(".");
          String path="";
          try 
          {
             path=dir.getCanonicalPath()+"/";
          } 
          catch (Exception e) 
          {
             e.printStackTrace();
          }
          filename = (bIsReverse) ? targetImp.getTitle() : sourceImp.getTitle();
          String new_filename="";
          int dot = filename.lastIndexOf('.');
          if (dot == -1) new_filename=filename + "_transf.txt";
          else           new_filename=filename.substring(0, dot)+"_transf.txt";
          filename=path+filename;

          if (outputLevel > -1) 
          {
             final Frame f = new Frame();
             final FileDialog fd = new FileDialog(f, "Save Transformation", FileDialog.SAVE);
             fd.setFile(new_filename);
             fd.setVisible(true);
             path = fd.getDirectory();
             filename = fd.getFile();
             if ((path == null) || (filename == null)) return;
             filename=path+filename;
           } else
             filename=new_filename;
       }

       // Save the file
       try {
          final FileWriter fw = new FileWriter(filename);
          String aux;
          fw.write("Intervals="+intervals+"\n\n");
          fw.write("X Coeffs -----------------------------------\n");
          for (int i= 0; i<intervals + 3; i++) {
             for (int j = 0; j < intervals + 3; j++) {
                aux=""+cx[i][j];
                    while (aux.length()<21) aux=" "+aux;
                    fw.write(aux+" ");
             }
             fw.write("\n");
          }
          fw.write("\n");
          fw.write("Y Coeffs -----------------------------------\n");
          for (int i= 0; i<intervals + 3; i++) {
             for (int j = 0; j < intervals + 3; j++) {
                aux=""+cy[i][j];
                    while (aux.length()<21) aux=" "+aux;
                    fw.write(aux+" ");
             }
             fw.write("\n");
          }
          fw.close();
       } catch (IOException e) {
          IJ.error("IOException exception" + e);
       } catch (SecurityException e) {
          IJ.error("Security exception" + e);
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Show the deformation grid.
     *
     * @param intervals number of intervals in the deformation
     * @param cx x- deformation coefficients
     * @param cy y- deformation coefficients
     * @param is image stack where we want to show the deformation grid
     * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
     */ 
    private void showDeformationGrid(
       int intervals,
       double [][]cx,
       double [][]cy,
       ImageStack is,
       boolean bIsReverse)
    {

       int auxTargetCurrentHeight = this.targetCurrentHeight;
       int auxTargetCurrentWidth = this.targetCurrentWidth;

       if(bIsReverse)
       {
           auxTargetCurrentHeight = sourceCurrentHeight;
           auxTargetCurrentWidth = sourceCurrentWidth;
       }
        // Initialize output image
       int stepv = Math.min(Math.max(10, auxTargetCurrentHeight/15), 30);
       int stepu = Math.min(Math.max(10, auxTargetCurrentWidth/15), 30);
       final double transformedImage [][] = new double [auxTargetCurrentHeight][auxTargetCurrentWidth];
       for (int v=0; v<auxTargetCurrentHeight; v++)
          for (int u=0; u<auxTargetCurrentWidth; u++) 
              transformedImage[v][u] = 255;

       // Ask for memory for the transformation
       double [][] transformation_x=new double [auxTargetCurrentHeight][auxTargetCurrentWidth];
       double [][] transformation_y=new double [auxTargetCurrentHeight][auxTargetCurrentWidth];

       // Compute the deformation
       computeDeformation(intervals,cx,cy,transformation_x,transformation_y, bIsReverse);

       // Show deformed grid ........................................
       // Show deformation vectors
       for (int v=0; v<auxTargetCurrentHeight; v+=stepv)
          for (int u=0; u<auxTargetCurrentWidth; u+=stepu) {
             final double x = transformation_x[v][u];
             final double y = transformation_y[v][u];
               // Draw horizontal line
               int uh=u+stepu;
               if (uh<auxTargetCurrentWidth) {
                    final double xh = transformation_x[v][uh];
                  final double yh = transformation_y[v][uh];
                  bUnwarpJMiscTools.drawLine(
                     transformedImage,
                     (int)Math.round(x) ,(int)Math.round(y),
                     (int)Math.round(xh),(int)Math.round(yh),0);
              }

               // Draw vertical line
               int vv = v+stepv;
               if (vv<auxTargetCurrentHeight) {
                    final double xv = transformation_x[vv][u];
                  final double yv = transformation_y[vv][u];
                  bUnwarpJMiscTools.drawLine(
                     transformedImage,
                     (int)Math.round(x) ,(int)Math.round(y),
                     (int)Math.round(xv),(int)Math.round(yv),0);
              }
           }

       // Set it to the image stack
       FloatProcessor fp=new FloatProcessor(auxTargetCurrentWidth, auxTargetCurrentHeight);
       for (int v=0; v<auxTargetCurrentHeight; v++)
          for (int u=0; u<auxTargetCurrentWidth; u++)
             fp.putPixelValue(u, v, transformedImage[v][u]);
       is.addSlice("Deformation Grid",fp);
    }

    /*------------------------------------------------------------------*/
    /**
     * Show the deformation vectors.
     *
     * @param intervals number of intervals in the deformation
     * @param cx x- deformation coefficients
     * @param cy y- deformation coefficients
     * @param is image stack where we want to show the deformation vectors
     * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
     */ 
    private void showDeformationVectors(
       int intervals,
       double [][]cx,
       double [][]cy,
       ImageStack is,
       boolean bIsReverse) 
    {
       // Auxiliar variables for changing from source to target and inversely
       bUnwarpJMask auxTargetMsk = this.targetMsk;
       bUnwarpJMask auxSourceMsk = this.sourceMsk;
       int auxTargetCurrentHeight = this.targetCurrentHeight;
       int auxTargetCurrentWidth = this.targetCurrentWidth;

       // Change if necessary
       if(bIsReverse)
       {
           auxTargetMsk = this.sourceMsk;
           auxSourceMsk = this.targetMsk;
           auxTargetCurrentHeight = this.sourceCurrentHeight;
           auxTargetCurrentWidth = this.sourceCurrentWidth;                     
       }

        // Initialize output image
       int stepv = Math.min(Math.max(10, auxTargetCurrentHeight/15),30);
       int stepu = Math.min(Math.max(10, auxTargetCurrentWidth/15),30);

       final double transformedImage [][] = new double [auxTargetCurrentHeight][auxTargetCurrentWidth];

       for (int v=0; v<auxTargetCurrentHeight; v++)
          for (int u=0; u<auxTargetCurrentWidth; u++) 
              transformedImage[v][u]=255;

       // Ask for memory for the transformation
       double [][] transformation_x=new double [auxTargetCurrentHeight][auxTargetCurrentWidth];
       double [][] transformation_y=new double [auxTargetCurrentHeight][auxTargetCurrentWidth];

       // Compute the deformation
       computeDeformation(intervals, cx, cy, transformation_x, transformation_y, bIsReverse);

       // Show shift field ........................................
       // Show deformation vectors
       for (int v=0; v<auxTargetCurrentHeight; v+=stepv)
          for (int u=0; u<auxTargetCurrentWidth; u+=stepu)
              if (auxTargetMsk.getValue(u,v)) {
                final double x = transformation_x[v][u];
                final double y = transformation_y[v][u];
                 if (auxSourceMsk.getValue(x,y))
                    bUnwarpJMiscTools.drawArrow(
                       transformedImage,
                       u,v,(int)Math.round(x),(int)Math.round(y),0,2);
             }

       // Set it to the image stack
       FloatProcessor fp=new FloatProcessor(auxTargetCurrentWidth,auxTargetCurrentHeight);
       for (int v=0; v<auxTargetCurrentHeight; v++)
          for (int u=0; u<auxTargetCurrentWidth; u++)
             fp.putPixelValue(u, v, transformedImage[v][u]);
       is.addSlice("Deformation Field",fp);
    }

    /*-------------------------------------------------------------------*/
    /**
     * Show the transformation.
     *
     * @param intervals number of intervals in the deformation
     * @param cx x- deformation coefficients
     * @param cy y- deformation coefficients
     * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
     */ 
    private void showTransformation(
       final int   intervals,
       final double [][]cx,    // Input, spline coefficients
       final double [][]cy,
       boolean bIsReverse)
    {
       boolean show_deformation = false;

       bUnwarpJImageModel auxTarget = target;
       bUnwarpJImageModel auxSource = source;
       bUnwarpJMask auxTargetMsk = targetMsk;
       bUnwarpJMask auxSourceMsk = sourceMsk;   
       int auxTargetWidth = this.targetWidth;
       int auxTargetHeight = this.targetHeight;
       ImagePlus output_ip = this.output_ip_1;

       // Change if necessary
       if(bIsReverse)
       {   
           auxTarget = source;
           auxSource = target;              
           auxTargetMsk = sourceMsk; 
           auxSourceMsk = targetMsk;
           auxTargetWidth = this.sourceWidth;
           auxTargetHeight = this.sourceHeight;     
           output_ip = this.output_ip_2;
       }

       // Ask for memory for the transformation
       double [][] transformation_x = new double [auxTargetHeight][auxTargetWidth];
       double [][] transformation_y = new double [auxTargetHeight][auxTargetWidth];

       // Compute the deformation
       computeDeformation(intervals, cx, cy, transformation_x, transformation_y, bIsReverse);

       if (show_deformation) 
       {
           bUnwarpJMiscTools.showImage("Transf. X", transformation_x);
           bUnwarpJMiscTools.showImage("Transf. Y", transformation_y);
       }

        // Compute the warped image
        FloatProcessor fp = new FloatProcessor(auxTargetWidth, auxTargetHeight);
        FloatProcessor fp_mask = new FloatProcessor(auxTargetWidth, auxTargetHeight);
        FloatProcessor fp_target = new FloatProcessor(auxTargetWidth, auxTargetHeight);

        int uv = 0;

        for (int v=0; v<auxTargetHeight; v++)
          for (int u=0; u<auxTargetWidth; u++,uv++) 
          {
              fp_target.putPixelValue(u, v, auxTarget.getImage()[uv]);
              if (!auxTargetMsk.getValue(u,v)) 
              {
                 fp.putPixelValue(u,v,0);
                 fp_mask.putPixelValue(u,v,0);
              } 
              else 
              {
                final double x = transformation_x[v][u];
                final double y = transformation_y[v][u];
                if (auxSourceMsk.getValue(x,y)) 
                {
                   auxSource.prepareForInterpolation(x,y,ORIGINAL);
                   double sval = auxSource.interpolateI();
                   fp.putPixelValue(u,v,sval);
                   fp_mask.putPixelValue(u,v,255);
                } 
                else 
                {
                    fp.putPixelValue(u,v,0);
                    fp_mask.putPixelValue(u,v,0);
                }
             }
         }
       fp.resetMinAndMax();
       final ImageStack is = new ImageStack(auxTargetWidth, auxTargetHeight);       
              
       String s = bIsReverse ? new String("Target") : new String("Source");       
       is.addSlice("Registered " + s + " Image", fp);
       if (outputLevel > -1)
           is.addSlice("Target Image", fp_target);
       if (outputLevel > -1)
           is.addSlice("Warped Source Mask",fp_mask);   
       
       if (outputLevel == 2) 
       {
          showDeformationVectors(intervals, cx, cy, is, bIsReverse);
          showDeformationGrid(intervals, cx, cy, is, bIsReverse);
       }
       output_ip.setStack("Registered " + s + " Image", is);
       output_ip.setSlice(1); 
       output_ip.getProcessor().resetMinAndMax();
       if (outputLevel > -1) 
           output_ip.updateAndRepaintWindow();
    }

    /*------------------------------------------------------------------*/
    /*
     * Method to update both current outputs 
     * (source-target and target-source).
     *
     * @param c b-spline coefficients
     * @param intervals number of intervals in the deformation
     */
    private void update_outputs(
       final double []c,
       int intervals)
    {    
        final int M = c.length / 2;
        final int halfM = M / 2;
        double []x1 = new double [M];    

        for(int i = 0, p = 0; i<halfM; i++, p++)
        {
            x1[p] = c[i];
            x1[p + halfM] = c[i + M];
        }

        double []x2 = new double [M];
        for(int i = halfM, p = 0; i<M; i++, p++)
        {
            x2[p] = c[i];
            x2[p + halfM] = c[i + M];
        } 
        // Updates.
        update_current_output(x1, intervals, false);
        update_current_output(x2, intervals, true);
    }
    /*------------------------------------------------------------------*/
    /*
     * Method to update a current output.
     *
     * @param c b-spline coefficients
     * @param intervals number of intervals in the deformation
     * @param flag to decide the deformation direction (source-target, target-source)
     */
    private void update_current_output(
       final double []c,
       int intervals,
       boolean bIsReverse) 
    {
       // Set the coefficients to an interpolator
       int cYdim = intervals+3;
       int cXdim = cYdim;
       int Nk = cYdim*cXdim;

       bUnwarpJImageModel auxTarget = target;
       bUnwarpJImageModel auxSource = source;
       bUnwarpJMask auxTargetMsk = targetMsk;
       bUnwarpJMask auxSourceMsk = sourceMsk;
       bUnwarpJPointHandler auxTargetPh = targetPh;
       bUnwarpJPointHandler auxSourcePh = sourcePh;
       bUnwarpJImageModel swx = swxTargetToSource;
       bUnwarpJImageModel swy = swyTargetToSource;
       int auxTargetWidth = this.targetWidth;
       int auxTargetHeight = this.targetHeight;
       int auxTargetCurrentWidth = this.targetCurrentWidth;
       int auxTargetCurrentHeight = this.targetCurrentHeight;
       int auxSourceWidth = this.sourceWidth;
       int auxSourceHeight = this.sourceHeight;
       ImagePlus auxSourceImp = this.sourceImp;
       ImagePlus output_ip = this.output_ip_1;
       double auxFactorWidth = this.targetFactorWidth;
       double auxFactorHeight = this.targetFactorHeight;
       String sOutput = new String ("Output Source-Target");

       // Change if necessary
       if(bIsReverse)
       {
           auxTarget = source;
           auxSource = target;
           auxTargetMsk = sourceMsk;
           auxSourceMsk = targetMsk;
           auxTargetPh = sourcePh;
           auxSourcePh = targetPh;
           swx = swxSourceToTarget;
           swy = swySourceToTarget;
           auxTargetWidth = this.sourceWidth;
           auxTargetHeight = this.sourceHeight;
           auxTargetCurrentWidth = sourceCurrentWidth;
           auxTargetCurrentHeight = sourceCurrentHeight; 
           auxSourceWidth = this.targetWidth;
           auxSourceHeight = this.targetHeight;
           auxSourceImp = this.targetImp;
           output_ip = this.output_ip_2;                    
           auxFactorWidth = this.sourceFactorWidth;
           auxFactorHeight = this.sourceFactorHeight;
           sOutput = new String ("Output Target-Source");
       }

       swx.setCoefficients(c, cYdim, cXdim, 0);
       swy.setCoefficients(c, cYdim, cXdim, Nk);

       // Compute the deformed image
       FloatProcessor fp = new FloatProcessor(auxTargetWidth, auxTargetHeight);
       int uv = 0;
       for (int v=0; v<auxTargetHeight; v++)
           for (int u=0; u<auxTargetWidth; u++, uv++) {
             if (auxTargetMsk.getValue(u,v)) {
                 double down_u = u*auxFactorWidth;
                 double down_v = v*auxFactorHeight;
                 final double tv = (double)(down_v * intervals)/(double)(auxTargetCurrentHeight-1) + 1.0F;
                 final double tu = (double)(down_u * intervals)/(double)(auxTargetCurrentWidth -1) + 1.0F;
                 swx.prepareForInterpolation(tu,tv,ORIGINAL); 
                 double x=swx.interpolateI();
                 swy.prepareForInterpolation(tu,tv,ORIGINAL); 
                 double y=swy.interpolateI();
                 double up_x=x/auxFactorWidth;
                 double up_y=y/auxFactorHeight;
                 if (auxSourceMsk.getValue(up_x,up_y)) {
                     auxSource.prepareForInterpolation(up_x, up_y, ORIGINAL);
                     fp.putPixelValue(u, v, auxTarget.getImage()[uv]-
                       auxSource.interpolateI());
                 } else
                    fp.putPixelValue(u, v, 0);
              } else
                 fp.putPixelValue(u, v, 0);
           }

       double min_val = output_ip.getProcessor().getMin();
       double max_val = output_ip.getProcessor().getMax();
       fp.setMinAndMax(min_val, max_val);
       output_ip.setProcessor(sOutput, fp);
       output_ip.updateImage();

       // Draw the grid on the target image ...............................
       // Some initialization
       int stepv = Math.min(Math.max(10, auxTargetHeight/15), 30);
       int stepu = Math.min(Math.max(10, auxTargetWidth/15), 30);
       final double transformedImage [][]=new double [auxSourceHeight][auxSourceWidth];
       double grid_colour = -1e-10;
       uv = 0;
       for (int v=0; v<auxSourceHeight; v++)
          for (int u=0; u<auxSourceWidth; u++,uv++) 
          {
             transformedImage[v][u] = auxSource.getImage()[uv];
             if (transformedImage[v][u]>grid_colour) grid_colour = transformedImage[v][u];
          }

        // Draw grid
       for (int v=0; v<auxTargetHeight+stepv; v+=stepv)
          for (int u=0; u<auxTargetWidth+stepu; u+=stepu) 
          {
                double down_u = u * auxFactorWidth;
                double down_v = v * auxFactorHeight;
                final double tv = (double)(down_v * intervals) / (double)(auxTargetCurrentHeight-1) + 1.0F;
                final double tu = (double)(down_u * intervals) / (double)(auxTargetCurrentWidth -1) + 1.0F;
                swx.prepareForInterpolation(tu,tv,ORIGINAL); 
                double x = swx.interpolateI();
                swy.prepareForInterpolation(tu,tv,ORIGINAL); 
                double y = swy.interpolateI();
                double up_x = x / auxFactorWidth;
                double up_y = y / auxFactorHeight;

                // Draw horizontal line
                int uh=u+stepu;
                if (uh<auxTargetWidth+stepu) 
                {
                    double down_uh = uh * auxFactorWidth;
                    final double tuh = (double)(down_uh * intervals)/(double)(auxTargetCurrentWidth -1) + 1.0F;
                    swx.prepareForInterpolation(tuh,tv,ORIGINAL); 
                    double xh=swx.interpolateI();
                    swy.prepareForInterpolation(tuh,tv,ORIGINAL); 
                    double yh=swy.interpolateI();
                    double up_xh = xh / auxFactorWidth;
                    double up_yh = yh / auxFactorHeight;
                    bUnwarpJMiscTools.drawLine(
                      transformedImage,
                      (int)Math.round(up_x) ,(int)Math.round(up_y),
                      (int)Math.round(up_xh),(int)Math.round(up_yh),grid_colour);
                }

                // Draw vertical line
                int vv=v+stepv;
                if (vv<auxTargetHeight+stepv) {
                    double down_vv= vv * auxFactorHeight;
                    final double tvv = (double)(down_vv * intervals)/(double)(auxTargetCurrentHeight-1) + 1.0F;
                    swx.prepareForInterpolation(tu,tvv,ORIGINAL); double xv=swx.interpolateI();
                    swy.prepareForInterpolation(tu,tvv,ORIGINAL); double yv=swy.interpolateI();
                    double up_xv=xv / auxFactorWidth;
                    double up_yv=yv / auxFactorHeight;
                    bUnwarpJMiscTools.drawLine(
                      transformedImage,
                      (int)Math.round(up_x) , (int)Math.round(up_y),
                      (int)Math.round(up_xv), (int)Math.round(up_yv), grid_colour);
               }
           }

       // Update the target image plus
       FloatProcessor fpg=new FloatProcessor(auxSourceWidth, auxSourceHeight);
       for (int v=0; v<auxSourceHeight; v++)
           for (int u=0; u<auxSourceWidth; u++)
              fpg.putPixelValue(u,v,transformedImage[v][u]);
       min_val = auxSourceImp.getProcessor().getMin();
       max_val = auxSourceImp.getProcessor().getMax();
       fpg.setMinAndMax(min_val,max_val);
       auxSourceImp.setProcessor(auxSourceImp.getTitle(),fpg);
       auxSourceImp.updateImage();
    }

    /*------------------------------------------------------------------*/
    /**
     * Calculate the cubic b-spline x weight.
     *
     * @param x x- value
     * @param xIntervals x- number of intervals
     * @param extended extended flat
     * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
     * @return weights
     */
    private double[] xWeight(
       final double x,
       final int xIntervals,
       final boolean extended,
       boolean bIsReverse) 
    {    
       int auxTargetCurrentWidth = (bIsReverse) ? this.sourceCurrentWidth : this.targetCurrentWidth; 

       int length = xIntervals+1;
       int j0=0, jF = xIntervals;
       if (extended) 
       {
           length+=2; 
           j0--; 
           jF++;
       }
       final double[] b = new double[length];
       final double interX = (double)xIntervals / (double)(auxTargetCurrentWidth - 1);
       for (int j=j0; j<=jF; j++) {
          b[j-j0] = bUnwarpJMathTools.Bspline03(x * interX - (double)j);
       }
       return(b);
    } /* end xWeight */

    /*------------------------------------------------------------------*/
    /**
     * Calculate the cubic b-spline y weight.
     *
     * @param y y- value
     * @param yIntervals y- number of intervals
     * @param extended extended flat
     * @param bIsReverse flag to determine the transformation direction (target-source=FALSE or source-target=TRUE)
     * @return weights
     */
    private double[] yWeight(
       final double y,
       final int yIntervals,
       final boolean extended,
       boolean bIsReverse) 
    {

       int auxTargetCurrentHeight = (bIsReverse) ? this.sourceCurrentHeight : this.targetCurrentHeight; 

       int length=yIntervals+1;
       int i0=0, iF=yIntervals;
       if (extended) 
       {
           length+=2; 
           i0--; 
           iF++;
       }
       final double[] b = new double[length];
       final double interY = (double)yIntervals / (double)(auxTargetCurrentHeight - 1);
       for (int i = i0; i<=iF; i++) {
          b[i-i0] = bUnwarpJMathTools.Bspline03(y * interY - (double)i);
       }
       return(b);
    } /* end yWeight */

} /* end class bUnwarpJTransformation */
