package levelsets.ij;

import ij.IJ;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;

import java.awt.Rectangle;
import java.util.Vector;

import levelsets.algorithm.Coordinate;
import levelsets.algorithm.DeferredByteArray3D;
import levelsets.algorithm.DeferredObjectArray3D;
import levelsets.algorithm.FastMarching;
import levelsets.algorithm.SparseFieldLevelSet;

/**
 * @author erwin
 *
 */
public class StateContainer {
	
	
	public enum States { INSIDE, ZERO, OUTSIDE };
	
	// Manual seeds, FastMarching, SparseField use different state maps 
	// This class holds and (only if required) converts one state map to another
	// The purpose is primarily the decoupling of the FastMarching from the SparseField
	// so that a ROI can directly be used for SparseField and FastMarching can run on its own
	// as separate plugin. 
	// This has also the ability to create a binary image with the segmentation as output.
	
	protected static int TILE_SIZE = 5;
	protected int avg_grey = -1;
	protected int x_size, y_size, z_size;
	boolean insideout;
	
	protected ImageContainer ic = null;
	
	// it has 3 possible internal representations of the states:
	// DeferredByteArray3D (used by fast marching)
	DeferredObjectArray3D<States> d_map = null;
	// Full array containing the state map
	States [][][] s_map = null;
	// Vector holding bounding box coordinates
	Vector<Coordinate> c_map = null;
	// ROI
	Roi roi_map = null;
	
	
	public StateContainer() {
		
	}
	
	
	// sets the whole thing as region of interest
	// we need the image size in this case
	public void setROI(Roi roi, int x, int y, int z, boolean insideout) {
		roi_map = roi;
		x_size = x;
		y_size = y;
		z_size = z == 0 ? 1 : z; // make sure we have at least a z value of 1
		
		this.insideout = insideout;
	}
		
	// Assign with the output from FastMarching
	public void setFastMarching(DeferredByteArray3D statemap, int avg_grey) {
		
		d_map = new DeferredObjectArray3D<States>(statemap.getXLength(), statemap.getYLength(), statemap.getZLength(), statemap.getTileSize(), null);
			
		for (int x = 0; x < statemap.getXLength(); x++) {
			for (int y = 0; y < statemap.getYLength(); y++) {
				for (int z = 0; z < statemap.getZLength(); z++) {
					switch ( statemap.get(x, y, z) ) {
					case FastMarching.BAND:
						d_map.set(x, y, z, States.ZERO);
						break;
					case FastMarching.ALIVE:
						d_map.set(x, y, z, States.INSIDE);
						break;
					case FastMarching.FAR:
						d_map.set(x, y, z, States.OUTSIDE);
						break;
					}
				}
			}
		}
		
		this.avg_grey = avg_grey;
	}
	
	
	public void setSparseField(int [][][] state) {
		
		s_map = new States[state.length][state[0].length][state[0][0].length];
		
        for (int z = 0; z < state[0][0].length; z++) {
           for (int y = 0; y < state[0].length; y++) {
              for (int x = 0; x < state.length; x++) {
            	  if ( state[x][y][z] == SparseFieldLevelSet.STATE_ZERO ) {
            		  s_map[x][y][z] = States.ZERO;
            	  }
            	  else if ( state[x][y][z] < 0 ) {
            		  s_map[x][y][z] = States.INSIDE;
            	  }
            	  else {
            		  s_map[x][y][z] = States.OUTSIDE;
            	  }
              }
           }
        }
		
	}
	
	// Returns the d_map object -- assumes read only
	public DeferredObjectArray3D<States> getForSparseField() {
		
		if ( d_map != null ) {
			return d_map;
		}
		else if ( roi_map != null ) {
			return roi2dmap();			
		}
		else if ( c_map != null ) {
			// TODO
		}
		else if ( s_map != null ) {
			// TODO: create d_map 
		}
		
		return null;
	}

	
	public Vector<Coordinate> getForFastMarching() {
		
		return roi2points();
		
	}
	
	// TODO Make more robust so that it works with points too
	// Simplification but currently only used for SparseField anyway
	public int getZeroGreyValue() {
		return avg_grey;
	}
	
	protected Vector<Coordinate> roi2points() {
		
		if ( c_map != null ) {
			return c_map;
		}
		else if ( roi_map != null ) {
			
			c_map = new Vector<Coordinate>(10);
			
			if ( roi_map instanceof PolygonRoi ) {
				PolygonRoi roi_p = (PolygonRoi) roi_map;
				Rectangle roi_r = roi_p.getBounds();
				int [] xp = roi_p.getXCoordinates();
				int [] yp = roi_p.getYCoordinates();
				
				for ( int i = 0; i < roi_p.getNCoordinates(); i++ ) {
					c_map.add(new Coordinate(roi_r.x + xp[i], roi_r.y+yp[i], 0));
				}
			
				return c_map;
			}
			
		}
		
		return null;
	}
	
	
	protected DeferredObjectArray3D<States> roi2dmap() {
				
		if ( roi_map == null ) {
			return null;
		}
		
		States outside = States.OUTSIDE, inside = States.INSIDE;
		
		if ( insideout == true ) {
			outside = States.INSIDE;
			inside = States.OUTSIDE;
		}

		d_map = new DeferredObjectArray3D<States>(x_size, y_size, z_size, TILE_SIZE, outside);			
		
		ByteProcessor mask = (ByteProcessor) roi_map.getMask();
		Rectangle roi_r = roi_map.getBounds();
		int x_start = roi_r.x;
		int x_end = roi_r.x + roi_r.width;
		int y_start = roi_r.y;
		int y_end = roi_r.y + roi_r.height;
		int px_zero = 0, px_inside = 0;
		int grey_zero = 0, grey_inside = 0;
		// IJ.log("Got bounding rectangle " + roi_r.x + "/" + roi_r.y + "/" + roi_r.width + "/" + roi_r.height);
		// IJ.log("Got bounding rectangle with coordinates " + x_start + "/" + x_end + "/" + y_start + "/" + y_end);

		if ( mask == null ) {
			IJ.log("Rectangle, just parsing borders");
			int z = 0; // TODO z is not possible with roi
            for (int y = y_start; y < y_end; y++) {
               for (int x = x_start; x < x_end; x++) {
            	   if ( x == x_start || y == y_start || x == x_end - 1 || y == y_end - 1 ) {
            		   d_map.set(x, y, z, States.ZERO);
            		   px_zero++;
            	   } else {
            		   d_map.set(x, y, z, inside);
            		   px_inside++;
            	   }
               }
            }
            // IJ.log("Zero level= " + px_zero + ", Inside = " + px_inside );
		} else {
			IJ.log("Shape, parsing shape");
			int z = 0;
			for (int y = 0; y < roi_r.height; y++) {
				for (int x = 0; x < roi_r.width; x++) {
					
					int mask_pt = mask.get(x, y);
					
					if ( mask_pt != 0 ) {
						boolean border = false;

						if ( x == 0 || y == 0 || x == roi_r.width - 1 || y == roi_r.height - 1 ) {
							border = true;
						} else {
							// int [] mask_ba = { mask.get(x-1, y), mask.get(x+1, y), mask.get(x, y-1), mask.get(x, y+1) };
//							for ( int i = 0; i < mask_b.length; i++ ) {
//								if ( (mask_b[i] != 0 && mask_pt == 0) || (mask_b[i] == 0 && mask_pt != 0) ) {
//									border = true;
//									break;
//								}
//							}
							// lets try it the easy way and just sum up all the surrounding pixels
							// turns out the values are 255, have to make a logical and with 1 to get the number
							int mask_b = (mask.get(x-1, y) & 1) + (mask.get(x+1, y) & 1) + (mask.get(x, y-1) & 1) + (mask.get(x, y+1) & 1);
							if ( mask_b != 4 ) {
								border = true;
							}
						
						}						
						if ( border  ) {
							d_map.set(x + x_start, y + y_start, z, States.ZERO);
							px_zero++;
						}
						else {
							d_map.set(x + x_start, y + y_start, z, States.INSIDE);
							px_inside++;
						}
					}
				}
			}
	 			
		}
		
		return d_map;
	}
	

}
