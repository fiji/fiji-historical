package MeshMaker;

import ij.measure.Calibration;
import ij.*;
import ij.process.ImageProcessor;

/*
	Classe che contiene le informazioni dello stack image. Formati supportati: 8 bit, 16 bit, 32 bit
*/

class InputStack
{

	private int widthX;   				// larghezza dell'immagine
	private int heightY;  	 	   		// altezza dell'immagine
	private int depthZ;					// numero di slice dello stack
	private int[][][] matrice; 			// matrice che immagazzina i valori di tutti i pixel dello stack
	private double min, max;			// valore minimo e massimo dell'immagine
	double a = 0, b = 1;
	
	
	// Restituisce il valore  del pixel (l'ho chiamata getVoxel impropriamente....
	int getVoxel(int x, int y, int z)
	{
		return this.matrice[x][y][z];
	
	}

	// Restituisce la larghezza dell'immagine
	int getWidth()
	{
		return this.widthX;	
		
	}
	
	// Restituisce l'altezza dell'immagine
	int getHeight()
	{
		return this.heightY;	
		
	}
	
	// Restituisce il numero di slice dello stack
	int getDepth()
	{
		return this.depthZ;	
		
	}

	// Setta i valori minimi e massimi dell'immagine
	boolean getMinMax(ImagePlus imp, ImageProcessor ip) {
			min = ip.getMin();
			max = ip.getMax();
	
			Calibration cal = imp.getCalibration();
	
			if (cal != null) {
				if (cal.calibrated()) {
					
					min = cal.getCValue((int)min);
					max = cal.getCValue((int)max);
					
					double[] coef = cal.getCoefficients();
					if (coef != null) {		
						a = coef[0];
						b = coef[1];
					}
				}
			}
			return true;
	}		
	
	// Costruttore che data un'immagine e l'image processor associato immagazzina lo stack image
	InputStack(ImagePlus imp, ImageProcessor ip)
	{

			// prendo le dimensioni del cubo 3d che voglio rappresentare
			
			widthX = imp.getWidth();
			heightY = imp.getHeight();
			depthZ = imp.getStackSize();
			

			matrice = new int[widthX][heightY][depthZ]; //x y z
			
			getMinMax(imp, ip);
					
			ImageStack stack = imp.getStack();
			
			// acquisisco la nuvola di punti discernendo i casi di immagini a 8 bit
			
			int bitDepth = imp.getBitDepth();

			// caso 8 bit
			if (bitDepth == 8) {
				float scale = (float) (255f/(max-min));
				
				for (int z=0;z<depthZ;z++) {
				//for (int z=depthZ-1;z>=0;z--) {
					IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
					IJ.showProgress((double)3*z/(4*depthZ));
					
					byte[] pixels = (byte[]) stack.getPixels(z+1);
					
					int pos = 0;
					for (int y = 0; y < heightY; y++) {
						for (int x = 0; x <widthX; x++) {
							
							int val = 0xff & pixels[pos++];
							
							val = (int)((val-min)*scale);
							
							
							if (val<0f) val = 0;
							
							if (val>255) val = 255;
							
							
							matrice[x][y][z] = (int)(0xff & val); 
							
				
				

						}
					}
				}
				
			}
			/*
			// caso 16 bit
			if (bitDepth == 16) {
				float scale = (float) (255f/(max-min));
				
				for (int z=0;z<depthZ;z++) {
					IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
					IJ.showProgress((double)3*z/(4*depthZ));
					short[] pixels = (short[]) stack.getPixels(z+1);
					int pos = 0;
					for (int y = 0; y < heightY; y++) {
						
						for (int x = 0; x <widthX; x++) {
							
							int val = (int) ((int)(0xFFFF & pixels[pos++])*b + a - min);
							if (val<0f) val = 0;
							val = (int)(val*scale);
							
							if (val>255) val = 255;
							matrice[x][y][z] = (byte)(val);  
						}
					}
				}							
			}
			
			// caso 32 bit
			if (bitDepth == 32) {
				
				float scale = (float) (255f/(max-min));
				
				for (int z=0; z<depthZ; z++) {
					IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
					IJ.showProgress((double)3*z/(4*depthZ));
					float[] pixels = (float[]) stack.getPixels(z+1);
					
					int pos = 0;
					for (int y = 0; y < heightY; y++) {
						for (int x = 0; x <widthX; x++) {
							float value = (float) (pixels[pos++] - min);
							if (value<0f) value = 0f;
							int ivalue = (int)(value*scale);
							
							if (ivalue>255) ivalue = 255;
							matrice[x][y][z] = (byte)(ivalue);  
						}
					}
				}
			}
			

			IJ.showProgress(1.0);
			IJ.showStatus("");

*/




	}




}
