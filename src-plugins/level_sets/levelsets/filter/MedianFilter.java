// $Revision$, $Date$, $Author$

package levelsets.filter;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

/**
 * Performs a median filtering on the input image. This is a quite expensive 
 * operation with non linear growth when the mask size is increased! 
 */
public class MedianFilter implements Filter 
{
   private int maskradius = 0;
   private int[] pixel = new int[4];
   private ArrayList<Integer> sortlist = null;
   
   /**
    * Creates a new MedianFilter
    * @param maskradius Radius of the filter mask. The total length is (2 x radius + 1) so if you want
    * to have a 5x5 mask the radius would be 2.
    */
   public MedianFilter(int maskradius)
   {
      this.maskradius = maskradius;
      sortlist = new ArrayList<Integer>((int)Math.pow((maskradius * 2 + 1), 2));
   }
   
   /**
    * See Filter interface for javadoc
    */
   public BufferedImage filter(BufferedImage image) {
      BufferedImage result = new BufferedImage(
         image.getWidth(null), image.getHeight(null), image.getType());
      if (!(image instanceof WritableRenderedImage)) return null;
            
      WritableRaster in =  image.getRaster();
      WritableRaster out = result.getRaster();
      
      // Write median into output image
      int[] pixel = new int[4];
      for (int column = 0; column < in.getWidth(); column++) {
         for (int line = 0; line < in.getHeight(); line++) {
            int median = getMedianForPixel(column, line, in);
            pixel[0] = pixel[1] = pixel[2] = median;
            out.setPixel(column, line, pixel);
         }
      }
      
      return result;
   }
   
   /** Gets all pixels in the mask area around the center pixel designated by
    * the x/y coordinates. Trims the mask if near the image boundary. Then
    * sorts the pixel values and returns the median value.
    */
   private int getMedianForPixel(int x, int y, Raster raster)
   {
      // Trim mask if needed (at the image boundary)
      int radius = maskradius;
      if (x - radius  < 0) radius  = x;
      if (y - radius  < 0) radius  = y;
      if (x + radius  > raster.getWidth() - 1)
      {
         radius  = raster.getWidth() - 1 - x;
      }
      if (y + radius  > raster.getHeight() - 1)
      {
         radius = raster.getHeight() - 1 - y;
      }
      
      // Get pixels and add to the lists of elements to be sorted
      for (int i = x - radius ; i <= x + radius ; i++)
      {
         for (int j = y - radius ; j <= y + radius ; j++)
         {  
            pixel = raster.getPixel(i, j, pixel);
            sortlist.add(new Integer(pixel[0]));
         }
      }
      
      // Sort the list
      Collections.sort(sortlist);
      int index = (sortlist.size() - 1) / 2;
      Integer result = sortlist.get(index);      
      sortlist.clear();
      
      // Return median
      return result.intValue();
   }
}
