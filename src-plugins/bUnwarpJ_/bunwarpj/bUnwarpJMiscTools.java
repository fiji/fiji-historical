package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ(C).
 * Copyright (C) 2005,2006,2007,2008 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
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

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;
import java.util.StringTokenizer;
/**
 * Different tools for the bUnwarpJ interface.
 */
public class bUnwarpJMiscTools
{
	/**
	 * Apply a given splines transformation to the source (gray-scale) image.
	 * The source image is modified. The target image is used to know
	 * the output size.
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param source source image model
	 * @param intervals intervals in the deformation
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
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
		/* GRAY SCALE IMAGES */
		if(!(sourceImp.getProcessor() instanceof ColorProcessor))
		{
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
		else /* COLOR IMAGES */
		{        	
			// red
			bUnwarpJImageModel sourceR = new bUnwarpJImageModel( ((ColorProcessor) (sourceImp.getProcessor())).toFloat(0, null), false);
			sourceR.setPyramidDepth(0);
			sourceR.getThread().start();
			// green
			bUnwarpJImageModel sourceG = new bUnwarpJImageModel( ((ColorProcessor) (sourceImp.getProcessor())).toFloat(1, null), false);
			sourceG.setPyramidDepth(0);
			sourceG.getThread().start();
			//blue
			bUnwarpJImageModel sourceB = new bUnwarpJImageModel( ((ColorProcessor) (sourceImp.getProcessor())).toFloat(2, null), false);
			sourceB.setPyramidDepth(0);
			sourceB.getThread().start();

			// Join threads
			try {
				sourceR.getThread().join();
				sourceG.getThread().join();
				sourceB.getThread().join();
			} catch (InterruptedException e) {
				IJ.error("Unexpected interruption exception " + e);
			}

			// Calculate warped RGB image
			ColorProcessor cp = new ColorProcessor(targetWidth, targetHeight);
			FloatProcessor fpR = new FloatProcessor(targetWidth, targetHeight);
			FloatProcessor fpG = new FloatProcessor(targetWidth, targetHeight);
			FloatProcessor fpB = new FloatProcessor(targetWidth, targetHeight);
			for (int v=0; v<targetHeight; v++)
				for (int u=0; u<targetWidth; u++)
				{
					final double x = transformation_x[v][u];
					final double y = transformation_y[v][u];

					if (x>=0 && x<sourceWidth && y>=0 && y<sourceHeight)
					{                	 
						sourceR.prepareForInterpolation(x, y, ORIGINAL);
						fpR.putPixelValue(u, v, sourceR.interpolateI());

						sourceG.prepareForInterpolation(x, y, ORIGINAL);
						fpG.putPixelValue(u, v, sourceG.interpolateI());

						sourceB.prepareForInterpolation(x, y, ORIGINAL);
						fpB.putPixelValue(u, v, sourceB.interpolateI());                 
					}
					else
					{
						fpR.putPixelValue(u, v, 0);
						fpG.putPixelValue(u, v, 0);
						fpB.putPixelValue(u, v, 0);
					}
				}
			cp.setPixels(0, fpR);
			cp.setPixels(1, fpG);
			cp.setPixels(2, fpB);            
			cp.resetMinAndMax();

			sourceImp.setProcessor(sourceImp.getTitle(), cp);
			sourceImp.updateImage();
		}
	}

	/**
	 * Apply a given splines transformation to the source (RGB color) image.
	 * The source image is modified. The target image is used to know
	 * the output size.
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param sourceR image model of the source red channel 
	 * @param sourceG image model of the source green channel
	 * @param sourceB image model of the source blue channel
	 * @param intervals intervals in the deformation
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
	 */
	static public void applyTransformationToSource(
			ImagePlus sourceImp,
			ImagePlus targetImp,
			bUnwarpJImageModel sourceR,
			bUnwarpJImageModel sourceG,
			bUnwarpJImageModel sourceB,
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
		ColorProcessor cp = new ColorProcessor(targetWidth, targetHeight);
		FloatProcessor fpR = new FloatProcessor(targetWidth, targetHeight);
		FloatProcessor fpG = new FloatProcessor(targetWidth, targetHeight);
		FloatProcessor fpB = new FloatProcessor(targetWidth, targetHeight);
		for (int v=0; v<targetHeight; v++)
			for (int u=0; u<targetWidth; u++)
			{
				final double x = transformation_x[v][u];
				final double y = transformation_y[v][u];

				if (x>=0 && x<sourceWidth && y>=0 && y<sourceHeight)
				{                	 
					sourceR.prepareForInterpolation(x, y, ORIGINAL);
					fpR.putPixelValue(u, v, sourceR.interpolateI());

					sourceG.prepareForInterpolation(x, y, ORIGINAL);
					fpG.putPixelValue(u, v, sourceG.interpolateI());

					sourceB.prepareForInterpolation(x, y, ORIGINAL);
					fpB.putPixelValue(u, v, sourceB.interpolateI());                 
				}
				else
				{
					fpR.putPixelValue(u, v, 0);
					fpG.putPixelValue(u, v, 0);
					fpB.putPixelValue(u, v, 0);
				}
			}
		cp.setPixels(0, fpR);
		cp.setPixels(1, fpG);
		cp.setPixels(2, fpB);            
		cp.resetMinAndMax();

		sourceImp.setProcessor(sourceImp.getTitle(), cp);
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
		/* GRAY SCALE IMAGES */
		if(!(sourceImp.getProcessor() instanceof ColorProcessor))
		{
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
		else /* COLOR IMAGES */
		{        	
			// red
			bUnwarpJImageModel sourceR = new bUnwarpJImageModel( ((ColorProcessor) (sourceImp.getProcessor())).toFloat(0, null), false);
			sourceR.setPyramidDepth(0);
			sourceR.getThread().start();
			// green
			bUnwarpJImageModel sourceG = new bUnwarpJImageModel( ((ColorProcessor) (sourceImp.getProcessor())).toFloat(1, null), false);
			sourceG.setPyramidDepth(0);
			sourceG.getThread().start();
			//blue
			bUnwarpJImageModel sourceB = new bUnwarpJImageModel( ((ColorProcessor) (sourceImp.getProcessor())).toFloat(2, null), false);
			sourceB.setPyramidDepth(0);
			sourceB.getThread().start();

			// Join threads
			try {
				sourceR.getThread().join();
				sourceG.getThread().join();
				sourceB.getThread().join();
			} catch (InterruptedException e) {
				IJ.error("Unexpected interruption exception " + e);
			}

			// Calculate warped RGB image
			ColorProcessor cp = new ColorProcessor(targetWidth, targetHeight);
			FloatProcessor fpR = new FloatProcessor(targetWidth, targetHeight);
			FloatProcessor fpG = new FloatProcessor(targetWidth, targetHeight);
			FloatProcessor fpB = new FloatProcessor(targetWidth, targetHeight);
			for (int v=0; v<targetHeight; v++)
				for (int u=0; u<targetWidth; u++)
				{
					final double x = transformation_x[v][u];
					final double y = transformation_y[v][u];

					if (x>=0 && x<sourceWidth && y>=0 && y<sourceHeight)
					{                	 
						sourceR.prepareForInterpolation(x, y, ORIGINAL);
						fpR.putPixelValue(u, v, sourceR.interpolateI());

						sourceG.prepareForInterpolation(x, y, ORIGINAL);
						fpG.putPixelValue(u, v, sourceG.interpolateI());

						sourceB.prepareForInterpolation(x, y, ORIGINAL);
						fpB.putPixelValue(u, v, sourceB.interpolateI());                 
					}
					else
					{
						fpR.putPixelValue(u, v, 0);
						fpG.putPixelValue(u, v, 0);
						fpB.putPixelValue(u, v, 0);
					}
				}
			cp.setPixels(0, fpR);
			cp.setPixels(1, fpG);
			cp.setPixels(2, fpB);            
			cp.resetMinAndMax();

			sourceImp.setProcessor(sourceImp.getTitle(), cp);
			sourceImp.updateImage();
		} // end calculating warped color image

	}


	/**
	 * Calculate the warping index between two opposite elastic deformations.
	 * Note: the only difference between the warping index and the consistency 
	 * term formulae is a squared root: warping index = sqrt(consistency error).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx_direct direct transformation x- B-spline coefficients
	 * @param cy_direct direct transformation y- B-spline coefficients
	 * @param cx_inverse inverse transformation x- B-spline coefficients
	 * @param cy_inverse inverse transformation y- B-spline coefficients
	 * 
	 * @return geometric error (warping index) between both deformations.
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

		// We pass the coefficients to a one-dimension array
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


		// *********** Compute the geometric error ***********
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
			// Note: the only difference between the warping index and the 
			// consistency term is this squared root.
			warpingIndex = Math.sqrt(warpingIndex);            
		}
		else
			warpingIndex = -1;
		return warpingIndex;
	}
	/*------------------------------------------------------------------*/
	/**
	 * Calculate the raw transformation mapping from B-spline
	 * coefficients.
	 *
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx transformation x- B-spline coefficients
	 * @param cy transformation y- B-spline coefficients
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

		// We pass the coefficients to a one-dimension array
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
	 * of deformation (both transformations having same direction).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx_direct direct transformation x- B-spline coefficients
	 * @param cy_direct direct transformation y- B-spline coefficients
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

		// We pass the coefficients to a one-dimension array
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


		// Compute the geometric error between both transformations
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
	 * Warping index for comparing two raw deformations (both 
	 * transformations having same direction).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param transformation_x_1 raw first transformation in x- axis
	 * @param transformation_y_1 raw first transformation in y- axis
	 * @param transformation_x_2 raw second transformation in x- axis
	 * @param transformation_y_2 raw second transformation in y- axis
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
		if (ip instanceof ByteProcessor) 
		{
			final byte[] pixels = (byte[])ip.getPixels();
			for (int y = 0; (y < height); y++)
				for (int x = 0; (x < width); x++, k++)
					image[k] = (double)(pixels[k] & 0xFF);
		} 
		else if (ip instanceof ShortProcessor) 
		{
			final short[] pixels = (short[])ip.getPixels();
			for (int y = 0; (y < height); y++)
				for (int x = 0; (x < width); x++, k++)
					if (pixels[k] < (short)0) image[k] = (double)pixels[k] + 65536.0F;
					else                      image[k] = (double)pixels[k];
		} 
		else if (ip instanceof FloatProcessor) 
		{
			final float[] pixels = (float[])ip.getPixels();
			for (int p = 0; p<height*width; p++)
				image[p]=pixels[p];
		}
		else if (ip instanceof ColorProcessor)
		{
			ImageProcessor fp = ip.convertToFloat();
			final float[] pixels = (float[])fp.getPixels();
			for (int p = 0; p<height*width; p++)
				image[p] = pixels[p];    	  
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
	 * @param sourceStack stack of source related points
	 * @param targetStack stack of target related points
	 */
	static public void loadPoints(String filename,
			Stack <Point> sourceStack, Stack <Point> targetStack)
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
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
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
	 * @param transformation_x output x- transformation coordinates
	 * @param transformation_y output y- transformation coordinates
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
	 * Load an affine matrix from a file.
	 *
	 * @param filename matrix file name
	 * @param affineMatrix output affine matrix
	 */
	static public void loadAffineMatrix(String filename,
			double [][]affineMatrix)
	{
		try
		{
			final FileReader fr = new FileReader(filename);
			final BufferedReader br = new BufferedReader(fr);
			String line;

			// Read width
			line = br.readLine();
			StringTokenizer st = new StringTokenizer(line," ");
			if (st.countTokens() != 6)
			{
				fr.close();
				IJ.write("Cannot read affine transformation matrix");
				return;
			}

			affineMatrix[0][0] = Double.valueOf(st.nextToken()).doubleValue();
			affineMatrix[0][1] = Double.valueOf(st.nextToken()).doubleValue();
			affineMatrix[1][0] = Double.valueOf(st.nextToken()).doubleValue();
			affineMatrix[1][1] = Double.valueOf(st.nextToken()).doubleValue();
			affineMatrix[0][2] = Double.valueOf(st.nextToken()).doubleValue();
			affineMatrix[1][2] = Double.valueOf(st.nextToken()).doubleValue();

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
	}    /* end loadAffineMatrix */


	/*------------------------------------------------------------------*/
	/**
	 * Compose two elastic deformations into a raw deformation.
	 *
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx1 first transformation x- B-spline coefficients
	 * @param cy1 first transformation y- B-spline coefficients
	 * @param cx2 second transformation x- B-spline coefficients
	 * @param cy2 second transformation y- B-spline coefficients
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

		int cYdim = intervals+3;
		int cXdim = cYdim;
		int Nk = cYdim * cXdim;
		int twiceNk = 2 * Nk;

		// We pass the coefficients to a one-dimension array
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
		// Notice here that we apply first the second transformation
		// since we are actually filling the target image with
		// pixels of the source image.
		for (int v=0; v<targetCurrentHeight; v++)
		{
			final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
			for (int u = 0; u<targetCurrentWidth; u++)
			{
				final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

				swx2.prepareForInterpolation(tu, tv, false);
				final double x2 = swx2.interpolateI();

				swy2.prepareForInterpolation(tu, tv, false);
				final double y2 = swy2.interpolateI();

				final double tv2 = (double)(y2 * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;

				final double tu2 = (double)(x2 * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

				swx1.prepareForInterpolation(tu2, tv2, false);
				outputTransformation_x[v][u] = swx1.interpolateI();

				swy1.prepareForInterpolation(tu2, tv2, false);
				outputTransformation_y[v][u] = swy1.interpolateI();
			}
		}

	}  /* end method composeElasticTransformations */

	/*------------------------------------------------------------------*/
	/**
	 * Compose a raw deformation and an elastic deformation into a raw deformation.
	 *
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param transformation_x_1 first transformation coordinates in x-axis
	 * @param transformation_y_1 first transformation coordinates in y-axis
	 * @param cx2 second transformation x- B-spline coefficients
	 * @param cy2 second transformation y- B-spline coefficients
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
		// Notice here that we apply first the second (elastic) transformation
		// since we are actually filling the target image with
		// pixels of the source image.
		for (int v=0; v<targetCurrentHeight; v++)
		{
			final double tv = (double)(v * intervals) / (double)(targetCurrentHeight - 1) + 1.0F;
			for (int u = 0; u<targetCurrentWidth; u++)
			{
				final double tu = (double)(u * intervals) / (double)(targetCurrentWidth - 1) + 1.0F;

				// Second transformation
				swx2.prepareForInterpolation(tu, tv, false);
				final double x2 = swx2.interpolateI();

				swy2.prepareForInterpolation(tu, tv, false);
				final double y2 = swy2.interpolateI();

				int xbase = (int) x2;
				int ybase = (int) y2;
				double xFraction = x2 - xbase;
				double yFraction = y2 - ybase;

				// First transformation.
				if(x2 >= 0 && x2 < targetCurrentWidth && y2 >= 0 && y2 < targetCurrentHeight)
				{
					// We apply bilinear interpolation
					double lowerLeftX = transformation_x_1[ybase][xbase];
					double lowerLeftY = transformation_y_1[ybase][xbase];

					int xp1 = (xbase < (targetCurrentWidth -1)) ? xbase+1 : xbase;
					int yp1 = (ybase < (targetCurrentHeight-1)) ? ybase+1 : ybase;

					double lowerRightX = transformation_x_1[ybase][xp1];
					double lowerRightY = transformation_y_1[ybase][xp1];

					double upperRightX = transformation_x_1[yp1][xp1];
					double upperRightY = transformation_y_1[yp1][xp1];

					double upperLeftX = transformation_x_1[yp1][xbase];
					double upperLeftY = transformation_y_1[yp1][xbase];

					double upperAverageX = upperLeftX + xFraction * (upperRightX - upperLeftX);
					double upperAverageY = upperLeftY + xFraction * (upperRightY - upperLeftY);
					double lowerAverageX = lowerLeftX + xFraction * (lowerRightX - lowerLeftX);
					double lowerAverageY = lowerLeftY + xFraction * (lowerRightY - lowerLeftY);

					outputTransformation_x[v][u] = lowerAverageX + yFraction * (upperAverageX - lowerAverageX);
					outputTransformation_y[v][u] = lowerAverageY + yFraction * (upperAverageY - lowerAverageY);
				}
				else
				{
					outputTransformation_x[v][u] = x2;
					outputTransformation_y[v][u] = y2;
				}

			}
		}

	}  /* end method composeRawElasticTransformations */

	/*------------------------------------------------------------------*/
	/**
	 * Compose two elastic deformations into a raw deformation at pixel level.
	 *
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx1 first transformation x- B-spline coefficients
	 * @param cy1 first transformation y- B-spline coefficients
	 * @param cx2 second transformation x- B-spline coefficients
	 * @param cy2 second transformation y- B-spline coefficients
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

		// We pass the coefficients to a one-dimension array
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
	 * Compose two raw transformations (Bilinear interpolation)
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
		// Notice here that we apply first the second transformation
		// since we are actually filling the target image with
		// pixels of the source image.
		for (int i= 0; i < height; i++)
			for (int j = 0; j < width; j++)
			{
				// Second transformation.
				double dX = transformation_x_2[i][j];
				double dY = transformation_y_2[i][j];
				int xbase = (int) dX;
				int ybase = (int) dY;
				double xFraction = dX - xbase;
				double yFraction = dY - ybase;

				// First transformation.
				if(dX >= 0 && dX < width && dY >= 0 && dY < height)
				{
					double lowerLeftX = transformation_x_1[ybase][xbase];
					double lowerLeftY = transformation_y_1[ybase][xbase];

					int xp1 = (xbase < (width -1)) ? xbase+1 : xbase;
					int yp1 = (ybase < (height-1)) ? ybase+1 : ybase;

					double lowerRightX = transformation_x_1[ybase][xp1];
					double lowerRightY = transformation_y_1[ybase][xp1];

					double upperRightX = transformation_x_1[yp1][xp1];
					double upperRightY = transformation_y_1[yp1][xp1];

					double upperLeftX = transformation_x_1[yp1][xbase];
					double upperLeftY = transformation_y_1[yp1][xbase];

					double upperAverageX = upperLeftX + xFraction * (upperRightX - upperLeftX);
					double upperAverageY = upperLeftY + xFraction * (upperRightY - upperLeftY);
					double lowerAverageX = lowerLeftX + xFraction * (lowerRightX - lowerLeftX);
					double lowerAverageY = lowerLeftY + xFraction * (lowerRightY - lowerLeftY);

					outputTransformation_x[i][j] = lowerAverageX + yFraction * (upperAverageX - lowerAverageX);
					outputTransformation_y[i][j] = lowerAverageY + yFraction * (upperAverageY - lowerAverageY);
				}
				else
				{
					outputTransformation_x[i][j] = dX;
					outputTransformation_y[i][j] = dY;
				}
			}
	}

	/*------------------------------------------------------------------*/
	/**
	 * Save the elastic transformation.
	 *
	 * @param intervals number of intervals in the deformation
	 * @param cx x- deformation coefficients
	 * @param cy y- deformation coefficients
	 * @param filename transformation file name
	 */
	public static void saveElasticTransformation(
			int intervals,
			double [][]cx,
			double [][]cy,
			String filename)
	{

		// Save the file
		try {
			final FileWriter fw = new FileWriter(filename);
			String aux;
			fw.write("Intervals="+intervals+"\n\n");
			fw.write("X Coeffs -----------------------------------\n");
			for (int i= 0; i<intervals + 3; i++) 
			{
				for (int j = 0; j < intervals + 3; j++) 
				{
					aux=""+cx[i][j];
					while (aux.length()<21) 
						aux=" "+aux;
					fw.write(aux+" ");
				}
				fw.write("\n");
			}
			fw.write("\n");
			fw.write("Y Coeffs -----------------------------------\n");
			for (int i= 0; i<intervals + 3; i++) 
			{
				for (int j = 0; j < intervals + 3; j++) 
				{
					aux=""+cy[i][j];
					while (aux.length()<21) 
						aux=" "+aux;
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
		if(filename == null || filename.equals(""))
		{
			String path = "";

			final OpenDialog od = new OpenDialog("Save Transformation", "");
			path = od.getDirectory();
			filename = od.getFileName();
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
		ip.updateImage();
		ip.show();
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
		ip.updateImage();
		ip.show();
	}

} /* End of MiscTools class */
