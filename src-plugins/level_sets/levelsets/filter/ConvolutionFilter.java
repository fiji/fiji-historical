// $Revision$, $Date$, $Author$

package levelsets.filter;

import java.awt.*;
import java.awt.image.*;

/**
 * Performs a convolution operation on the input image.
 */
public class ConvolutionFilter implements Filter
{
   public static float[] LAPLACE_EDGE_KERNEL = new float[]
   {
      1.0f, 1.0f, 1.0f,
              1.0f, -8.0f, 1.0f,
              1.0f, 1.0f, 1.0f
   };
   
   public static float[] GAUSSIAN_SMOOTH_KERNEL = new float[]
   {
      2f/159f, 4f/159f, 5f/159f, 4f/159f, 2f/159f,
              4f/159f, 9f/159f, 12f/159f, 4f/159f, 9f/159f,
              5f/159f, 12f/159f, 15f/159f, 12f/159f, 5f/159f,
              4f/159f, 9f/159f, 12f/159f, 4f/159f, 9f/159f,
              2f/159f, 4f/159f, 5f/159f, 4f/159f, 2f/159f,
   };
   
   private float[] kernel = null;
   
   /** Creates a new instance of SimpleEdgeDetect */
   public ConvolutionFilter(float[] kernel)
   {
      this.kernel = kernel;
   }
   
   public BufferedImage filter(BufferedImage input)
   {
      System.out.println("Convolution filter");
      int size = (int)Math.sqrt(kernel.length);
      
      BufferedImageOp edge = new ConvolveOp(new Kernel(size, size, kernel));
      
      ColorModel cm = input.getColorModel();
      
      BufferedImage srccpy = edge.createCompatibleDestImage(input, cm);
      input.copyData(srccpy.getRaster());
      BufferedImage result = edge.createCompatibleDestImage(input, cm);
      
      return edge.filter(srccpy, result);
   }
}
