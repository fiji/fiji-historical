// $Revision$, $Date$, $Author$

package levelsets.algorithm;

import ij.IJ;

import java.util.PriorityQueue;
import java.util.Vector;

import levelsets.filter.GreyValueErosion;
import levelsets.filter.MorphologicalOperator;
import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;

/**
 * Implementation of the Fast Marching algorithm. Uses tiles array data
 * structure.
 */
public class FastMarching implements StagedAlgorithm
{
   // Holds the state of the voxels
   private DeferredByteArray3D map = null;
   // Holds the arrival timeof the voxels
   private DeferredDoubleArray3D arrival = null;
   /* Holds the distance (shortest way over already visited voxels) from seed
    * point of the voxels
    */
   private DeferredDoubleArray3D distances = null;
   
   // Lookup table for fast location of the voxel element for given coordinate
   private DeferredObjectArray3D<BandElement> elementLUT = null;
   
   // The seed points - held for later initialization
   private Vector<Coordinate> seeds = null;
   // Mean of greyvalues around all seedpoints
   private int seed_greyvalue = 0;
   
   // The source image
   private ImageContainer source = null;
   // The working copy - will be filtered
   private ImageContainer img = null;
   // Grey value gradient of working copy image
   private double[][][] gradients = null;
   // The image where the progress is shown
   private ImageProgressContainer progress = null;
   
   // Heap data structure for sorting the trial set elements
   private PriorityQueue<BandElement> heap = null;
   
   // Constant for the exponent of the image term
   private static double ALPHA = 0.005d;
   
   // Arrival time of the last voxel added to the alive set
   private double lastFreezeTime = 0;
   // Maximum distance in pixels from seed point travelled so far
   private double max_distance = 0;
   
   // Use the halting conditions or run over the entire domain?
   private boolean halt = false;
   // Flag: Initialization needed? If true - will be flipped afterwards
   private boolean needInit = true;
   // Tag to signal if a problem was encountered which prevents more iterations
   private boolean invalid = false;
   
   // Cache for BandElement objects to avoid continuous reallocation
   private BandElementCache elem_cache = null;
   // Size of that cache (number of elements)
   private static int ELEMENT_CACHE_SIZE = 1000;
   
   // preallocate
   int [] pixel = new int[4];
   
   /**
    * Constant for Far elements
    */
   public static final byte FAR = 0;
   /**
    * Constant for Band elements
    */
   public static final byte BAND = 1;
   /**
    * Constant for ALIVE elements
    */
   public static final byte ALIVE = 2;
   
   // Threshold percentage for trial points that should be freezed
   private static double DISTANCE_THRESHOLD = 0.5; //1;
   // Maximum distance to be travelled (pixels on a path from seedpoint)
   private static final double DISTANCE_STOP = 1000; //1;
   // Penalty threshold around mean seed greyvalue
   private static int GREYVALUE_THRESHOLD = 50;
   // Growth threshold for immediate abort
   private static final double EXTREME_GROWTH = 1000;
   
   // green coloured pixel for alive set pixel visualization
   private static int[] ALIVE_PIXEL = new int[] {0, 255, 0, 0};
   // red coloured pixel for trial set (band) pixel visualization
   private static int[] BAND_PIXEL = new int[] {255, 0, 0, 0};
   
   // counter for completed step output
   private int steps = 0;
   
   /** Creates a new instance of FastMarching
    */
   public FastMarching(ImageContainer image, ImageProgressContainer img_progress, StateContainer seedContainer, boolean halt)
   {
      /* Just reference the input data. Actual initialization is done later
       * because when this construcor is called this object is queued for later
       * execution so this avoids allocation of much memory to early.
       */
      this.halt = halt;
      this.source = image;
      this.progress = img_progress;
      this.seeds = seedContainer.getForFastMarching();
      needInit = true;
   }
   
   
   /**
    * Sets the Grey value.
    * Works only before the initialization (i.e. the first iteration)
    */
   public void setGreyThreshold(int t) {
	   if ( needInit )
		   GREYVALUE_THRESHOLD = t;
   }

   /**
    * Returns the Grey value.
    * @return The AGrey value threshold
    */
   public static int getGreyThreshold() {
	   return GREYVALUE_THRESHOLD;
   }

   /**
    * Sets the distance threshold.
    * Works only before the initialization (i.e. the first iteration)
    */
   public void setDistanceThreshold(double w) {
	   if ( needInit )
		   DISTANCE_THRESHOLD = w;
   }
   
   /**
    * Returns the distance threshold.
    * @return The distance threshold
    */
   public static double getDistanceThreshold() {
	   return DISTANCE_THRESHOLD;
   }

   /* Initialization - called on first call to step(). Does basically things the
    * constructor would have done.
    */
   private boolean init()
   {
      // Initialize all the data structures
      map = new DeferredByteArray3D(source.getWidth(), source.getHeight(), source.getImageCount(), 5, FAR);
      arrival = new DeferredDoubleArray3D(source.getWidth(), source.getHeight(), source.getImageCount(), 5, 0);
      distances = new DeferredDoubleArray3D(source.getWidth(), source.getHeight(), source.getImageCount(), 5, 0d);
      elementLUT = new DeferredObjectArray3D<BandElement>(source.getWidth(), source.getHeight(), source.getImageCount(), 5, null);
      heap = new PriorityQueue<BandElement>(1000);
      
      elem_cache = new BandElementCache(ELEMENT_CACHE_SIZE);
      
      /* Create a working copy of the input image which then is filtered as
       * needed
       */
      this.img = source.deepCopy();
      
      // First make sure we actually got valid seed points
      if ( seeds == null ) {
    	  invalid = true;
      }
      else if ( seeds.size() == 0 ) {
    	  invalid = true;
      }
      if ( invalid ) {
    	  IJ.error("Fast Marching needs seed points but didn't find any! Did you specify an area?");
    	  return false;
      }
      
      /* Add all seed points to the heap. Also determine mean grey value
       * around seed points
       */       
      for (int i = 0; i< seeds.size(); i++)
      {
         Coordinate seed = seeds.elementAt(i);
         this.seed_greyvalue += probeSeedGreyValue(seed.getX(), seed.getY(), seed.getZ());
         
         BandElement start = elem_cache.getRecycledBandElement(seed.getX(), seed.getY(), seed.getZ(), 0);
         elementLUT.set(seed.getX(), seed.getY(), seed.getZ(), start);
         map.set(seed.getX(), seed.getY(), seed.getZ(), BAND);
         
         heap.add(start);
      }
      
      this.seed_greyvalue /= seeds.size();
      
      // release seed points - no longer needed
      IJ.log("Seed mean greyvalue = " + seed_greyvalue);
      seeds = null;
      
      gradients = this.source.calculateGradients();
      
      //this.img.applyFilter(new MedianFilter(2));
      
      if (img.getImageCount() > 100)
      {
         int partsize = img.getImageCount() / 2;
         int offset = 0;
         this.img.applyFilter(new GreyValueErosion(MorphologicalOperator.getTrueMask(5, 5)), 0, partsize - 1);
         this.img.applyFilter(new GreyValueErosion(MorphologicalOperator.getTrueMask(5, 5)), 0, partsize - 1);
         offset += partsize;
         this.img.applyFilter(new GreyValueErosion(MorphologicalOperator.getTrueMask(5, 5)), offset, offset + partsize - 1);
      }
      else
      {
//         this.img.applyFilter(new GreyValueErosion(MorphologicalOperator.getTrueMask(5, 5)));
         this.img.applyFilter(new GreyValueErosion(MorphologicalOperator.getTrueMask(5, 5)));
      }
      
      IJ.log("Fast Marching done init");
      
      visualize(true);
      
      return(true);
   }
   
   
   private void freeze(BandElement elem)
   {
      int freezeX = elem.getX();
      int freezeY = elem.getY();
      int freezeZ = elem.getZ();
      
      map.set(freezeX, freezeY, freezeZ, ALIVE);
      elementLUT.set(freezeX, freezeY, freezeZ, null);
      
      double dist = distances.get(freezeX, freezeY, freezeZ);
      
      if (dist > DISTANCE_STOP)
      {
         System.out.println("Stopped - max distance exceeded");
         heap.clear();
         return;
      }
      else if (dist < max_distance * DISTANCE_THRESHOLD || (dist < max_distance - 30))
                                                         
      {
         arrival.set(freezeX, freezeY, freezeZ, Double.MAX_VALUE);
         distances.set(freezeX, freezeY, freezeZ, Double.MAX_VALUE);
         //System.out.println("Sorted out voxel");
      }
      else
      {
         arrival.set(freezeX, freezeY, freezeZ, elem.getValue());
         
         if (max_distance < distances.get(freezeX, freezeY, freezeZ))
         {
            max_distance = distances.get(freezeX, freezeY, freezeZ);
         }
         
         if (freezeX > 0) update(freezeX - 1, freezeY, freezeZ);
         if (freezeX + 1 < map.getXLength()) update(freezeX + 1, freezeY, freezeZ);
         if (freezeY > 0) update(freezeX, freezeY - 1, freezeZ);
         if (freezeY + 1 < map.getYLength()) update(freezeX, freezeY + 1, freezeZ);
         if (freezeZ > 0) update(freezeX, freezeY, freezeZ - 1);
         if (freezeZ + 1 < map.getZLength()) update(freezeX, freezeY, freezeZ + 1);
         
         //   if (halt)
         //   {
         //      if (heap.size() > 100 || arrival[freezeX][freezeY][freezeZ] > ((lastFreezeDistance + 1) * 1.1))
         //         //                     if (heap.size() > 100)
         if (arrival.get(freezeX, freezeY, freezeZ) > (((lastFreezeTime + EXTREME_GROWTH))))
         {
            
            IJ.log("Fast marching stopped - extreme growth");
            IJ.log("Last -> " + lastFreezeTime);
            IJ.log("Now -> " + arrival.get(freezeX, freezeY, freezeZ));
            heap.clear();
            elementLUT = null;
         }
         else
         {
            lastFreezeTime = arrival.get(freezeX, freezeY, freezeZ);
         }
         //   }
      }
   }
   
   // Updates a voxel in the neighbourhood of voxel just moved to alive set
   private void update(int x, int y, int z)
   {
      byte cell_state = map.get(x, y, z);
      if (cell_state == ALIVE) return;
      
      double time = calculateArrivalTime(x, y, z);
      double dist = calculateDistance(x, y, z);
      
      // If this voxel is already in the trial update arrival time and distance
      if (cell_state == BAND)
      {
         BandElement elem = elementLUT.get(x, y, z);
         
         /* updated distance and arrival time is guaranteed to be <= old
          * distance so omit a time consuming check
          */
         
         heap.remove(elem);
         elem.setValue(time);
         heap.offer(elem);
         
         distances.set(x, y, z, dist);
      }
      // If this voxel is currently in the far set add it to the trial set
      else if (cell_state == FAR)
      {
         BandElement elem = elem_cache.getRecycledBandElement(x, y, z, time);
         heap.offer(elem);
         
         map.set(x, y, z, BAND);
         elementLUT.set(x, y, z, elem);
         distances.set(x, y, z, dist);
      }
   }
   
   // See StagedAlgorithm interface defintion for javadoc
   public boolean step(int granularity)
   {
	  // if something weird happened - don't even try to go any further
	  if (invalid) {
		  return false;
	  }
	   
	  // If this ist the first call - initialize
      if (needInit)
      {
         needInit = false;
         if ( ! init() ) {
        	 return false;
         }
      }
      
      if (heap.isEmpty())
      {
         postProcessStatemap();
         visualize(false);
         cleanup();
         // map.dumpToFile("D:\\temp\\fm_output.txt");
         //new Java3DVisualizer(this).setVisible(true);
         return false;
      }
      
      for (int i = 0; i < granularity; i++)
      {
         BandElement next = heap.poll();
         freeze(next);
         elem_cache.recycleBandElement(next);
         if (heap.isEmpty())
         {
            postProcessStatemap();
            visualize(false);
            cleanup();
            // map.dumpToFile("D:\\temp\\fm_output.txt");
            //new Java3DVisualizer(this).setVisible(true);
            return false;
         }
      }
      
      steps += granularity;
      IJ.log("Steps done : " + steps);
      visualize(false);
      
      return true;
   }
   
   /**
    * Returns the state map. State per voxel is one of the public constants, either
    * FAR, BAND or ALIVE
    * @return The state map
    */
   public DeferredByteArray3D getStateMap()
   {
      return map;
   }
   
   
   public StateContainer getStateContainer() {
	   // If there was a serious problem, return null (we don't have any valid data anyways)
	   if (invalid) {
		   return null;
	   }
	   
	   StateContainer sc_fm = new StateContainer();
	   sc_fm.setFastMarching(map, seed_greyvalue);
	   return sc_fm;
   }
   
   
   
   /**
    * Returns the mean of the seed point grey values
    * @return The grey value
    */
   public int getSeedGreyValue()
   {
      return this.seed_greyvalue;
   }
   
   private double calculateArrivalTime(int x, int y, int z)
   {
      // Get neighbour with minimal arrival time in every spatial direction
      double dist = Double.MAX_VALUE;
      
      double xB = (x > 0 && map.get(x - 1, y, z) == ALIVE) ?
         arrival.get(x - 1, y, z) : Double.MAX_VALUE;
      double xF = (x + 1 < map.getXLength() && map.get(x + 1, y, z) == ALIVE) ?
         arrival.get(x + 1, y, z) : Double.MAX_VALUE;
      double yB = (y > 0 && map.get(x, y - 1, z) == ALIVE) ?
         arrival.get(x, y - 1, z) : Double.MAX_VALUE;
      double yF = (y + 1 < map.getYLength() && map.get(x, y + 1, z) == ALIVE) ?
         arrival.get(x, y + 1, z) : Double.MAX_VALUE;
      double zB = (z > 0 && map.get(x, y, z - 1) == ALIVE) ?
         arrival.get(x, y , z - 1) : Double.MAX_VALUE;
      double zF = (z + 1 < map.getZLength() && map.get(x, y, z + 1) == ALIVE) ?
         arrival.get(x, y, z + 1) : Double.MAX_VALUE;
      
      
      double xVal = (xB < xF) ? xB : xF;
      double yVal = (yB < yF) ? yB : yF;
      double zVal = (zB < zF) ? zB : zF;
      
      // Determine quadratic cooefficient.
      int quadCoeff = 0;
      if (xVal < Double.MAX_VALUE) quadCoeff++;
      if (yVal < Double.MAX_VALUE) quadCoeff++;
      if (zVal < Double.MAX_VALUE) quadCoeff++;
      
      double speed = getSpeedTerm(x, y, z);
      
      /* If only one spatial  direction contributes to the quadratic
       * coefficient, than there ist a much more efficient solution - so this
       * is calculated and returned instead then. */
      if (quadCoeff == 1)
      {
         if (xVal < Double.MAX_VALUE)
         {
            return xVal + (1 / speed);
         }
         else if (yVal < Double.MAX_VALUE)
         {
            return yVal + (1 / speed);
         }
         else
         {
            return zVal + (1 / speed);
         }
      }
      
      int numSol = 0;
      double solution = 0;
      double linCoeff = 0;
      double abs = (-1 / (speed * speed));
      
      /* Calculate linear and absolute term contributions for every spatial
       * direction if alive.
       */
      if (xVal < Double.MAX_VALUE)
      {
         linCoeff -= 2 * xVal;
         abs += xVal * xVal;
      }
      if (yVal < Double.MAX_VALUE)
      {
         linCoeff -= 2 * yVal;
         abs += yVal * yVal;
      }
      if (zVal < Double.MAX_VALUE)
      {
         linCoeff -= 2 * zVal;
         abs += zVal * zVal;
      }
      
      // Discriminat of the general quadratic equation
      double discriminant = (linCoeff * linCoeff) - (4 * quadCoeff * abs);
      
      // Two solutions exist. Calculate the bigger one.
      if (discriminant > 0)
      {
         double rootDiscriminant = Math.sqrt(discriminant);
         solution = ((-linCoeff) + rootDiscriminant) / (2 * quadCoeff);
      }
      // No solution exists - read below
      else
      {
         /* Something went really wrong - no solution for the quadratic equation.
          * This should NEVER happen, so it clearly indicates a problem with the
          * speed calculation.
          */
         IJ.log("OUCH !!! # solutions = 0 (at " + x + ", " + y + ")");
         IJ.log("quad. coefficient = " + quadCoeff);
         IJ.log("lin. coefficient = " + linCoeff);
         IJ.log("absolute term = " + abs);
         IJ.log("xVal = " + xVal);
         IJ.log("yVal = " + yVal);
         IJ.log("Speedterm = " + speed);
         IJ.log("xB, xF, yB, yF = " + xB + ", " + xF + ", " + yB + ", " + yF);
         IJ.log("**********************************");
         System.exit(0);
      }
      
      return solution;
   }
   
   // Calculate the distance to the nearest seedpoint using only alive waypoints
   private double calculateDistance(int x, int y, int z)
   {
      // Get distances of all alive neighbours
      double xB = (x > 0 && map.get(x - 1, y, z) == ALIVE) ?
         distances.get(x - 1, y, z) : Double.MAX_VALUE;
      double xF = (x + 1 < map.getXLength() && map.get(x + 1, y, z) == ALIVE) ?
         distances.get(x + 1, y, z) : Double.MAX_VALUE;
      double yB = (y > 0 && map.get(x, y - 1, z) == ALIVE) ?
         distances.get(x, y - 1, z) : Double.MAX_VALUE;
      double yF = (y + 1 < map.getYLength() && map.get(x, y + 1, z) == ALIVE) ?
         distances.get(x, y + 1, z) : Double.MAX_VALUE;
      double zB = (z > 0 && map.get(x, y, z - 1) == ALIVE) ?
         distances.get(x, y, z - 1) : Double.MAX_VALUE;
      double zF = (z + 1 < map.getZLength() && map.get(x, y, z + 1) == ALIVE) ?
         distances.get(x, y, z + 1) : Double.MAX_VALUE;
      
      // Find minimum of the distances
      double xVal = (xB < xF) ? xB : xF;
      double yVal = (yB < yF) ? yB : yF;
      double zVal = (zB < zF) ? zB : zF;
      
      double dist = Math.min(Math.min(xVal, yVal), zVal);
      
      // Add 1 to the smallest way of its neighbours to reach this voxel
      return (dist + 1);
   }
   
   private double getSpeedTerm(int x, int y, int z)
   {
      //      int pixelval = inImg.getPixel(x, y, z, pixel)[0];
      //      if (pixelval < seed_greyvalue - GREYVALUE_THRESHOLD
      //              || pixelval > seed_greyvalue + GREYVALUE_THRESHOLD) return 0.0000001;
      //      return Math.pow(Math.E, (-alpha) * pixel[0]);
      //      return Math.pow(Math.E, (-alpha) * gradients[x][y][z]);
      int greyval = img.getPixel(x, y, z);
      int greyval_penalty = Math.abs(greyval - this.seed_greyvalue);
      if (greyval_penalty < GREYVALUE_THRESHOLD) greyval_penalty = 1;
      //greyval_penalty *= 100;
//            return (1d / (1 + gradients[x][y][z] * gradients[x][y][z] + greyval_penalty * greyval_penalty));
      return Math.pow(Math.E, (-ALPHA) * (gradients[x][y][z] * gradients[x][y][z] + greyval_penalty * greyval_penalty));
      //gradients[x][y][z]
   }
   
   // Visualize alive (green) and trial set (red)
   private void visualize(boolean set_output)
   {
	  // don't visualize if progress container is null
	  if ( progress == null ) {
		  return;
	  }
	  
	  int px_alive=0, px_band=0, px_far=0;
	  
      ImageProgressContainer output = progress;
      if ( set_output == true ) {
    	  progress.duplicateImages(img);
      }
      progress.showProgressStep();
      byte cell_state = 0;
      for (int z = 0; z < map.getZLength(); z++)
      {
         for (int x = 0; x < map.getXLength(); x++)
         {
            for (int y = 0; y < map.getYLength(); y++)
            {
               cell_state = map.get(x, y, z);
               if (cell_state == ALIVE)
               {
                  output.setPixel(x, y, z, ALIVE_PIXEL);
                  px_alive++;
               }
               else if (cell_state == BAND)
               {
                  output.setPixel(x, y, z, BAND_PIXEL);
                  px_band++;
               }
               else {
            	   px_far++;
               }
            }
         }
      }
      
      IJ.log("FastMarching iteration: Found pixels " +px_alive+" ALIVE, " +px_band + " BAND,"+px_far + " FAR");
      progress.showProgressStep();
   }
   
   // Derefrence large data structure to allow garbage collection
   private void cleanup()
   {
      arrival = null;
      this.gradients = null;
      elementLUT = null;
      img = source = null;
      heap = null;
      elem_cache = null;
   }
   
   // Determine mean grey value of the seed pixel and neighbourhood
   private int probeSeedGreyValue(int x, int y, int z)
   {
      int value = source.getPixel(x, y, z);
      int count = 1;
      
      if (x > 0)
      {  value += source.getPixel(x - 1, y, z); count++; }
      if (x < img.getWidth() - 1)
      { value += source.getPixel(x + 1, y, z); count++; }
      if (y > 0)
      { value += source.getPixel(x, y - 1, z); count++; }
      if (y < img.getHeight() - 1)
      { value += source.getPixel(x, y + 1, z); count++; }
      
      return value / count;
   }
   
   /**
    * Due to the freezing extension to the Fast Marching algorithm it no longer
    * maintains a closed contour of band elments around the alive set, Therefore
    * the contour is rebuild in this method.
    */
   private void postProcessStatemap()
   {
      IJ.log("Postprocessing Statemap...");
      
      DeferredByteArray3D processed_map = new DeferredByteArray3D(map.getXLength(), map.getYLength(), map.getZLength(), map.getTileSize(), FAR);
      
      for (int i = 0; i < map.getXLength(); i++)
      {
         for (int j = 0; j < map.getYLength(); j++)
         {
            for (int k = 0; k < map.getZLength(); k++)
            {
               if (map.get(i, j, k) == ALIVE)
               {
                  boolean inside = ((i > 0 && map.get(i - 1, j, k) == ALIVE) || i == 0);
                  inside = (inside && ((i + 1 < map.getXLength() && map.get(i + 1, j, k) == ALIVE) || (i + 1) == map.getXLength()));
                  inside = (inside && ((j > 0 && map.get(i, j - 1, k) == ALIVE) || j == 0));
                  inside = (inside && ((j + 1 < map.getYLength() && map.get(i, j + 1, k) == ALIVE) || (j + 1) == map.getYLength()));
                  inside = (inside && ((k > 0 && map.get(i, j, k - 1) == ALIVE) || k == 0));
                  inside = (inside && ((k + 1 < map.getZLength() && map.get(i, j, k + 1) == ALIVE) || (k + 1) == map.getZLength()));
                  
                  if (inside)
                  {
                     processed_map.set(i, j, k, ALIVE);
                  }
                  else
                  {
                     processed_map.set(i, j, k, BAND);
                  }
               }
            }
         }
      }
      
      map = processed_map;
   }
}
