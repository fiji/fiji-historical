import edu.mines.jtk.dsp.FftComplex;

import ij.IJ;
import ij.ImagePlus;

import ij.plugin.filter.PlugInFilter;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Arrays;

public class FFT_Line implements PlugInFilter {
	protected ImagePlus image;

	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_32;
	}

	public void run(ImageProcessor ip) {
		int w = ip.getWidth();
		int h = ip.getHeight();
		float[] pixels = (float[])ip.getPixels();

		// FFT needs appropriate length
		int fftLength = FftComplex.nfftFast(w * h * 2);

		// make a pseudo-complex array, column by column
		float[] complex = new float[fftLength * 2];
		for (int i = 0; i < w; i++)
			for (int j = 0; j < h; j++)
				complex[(i * h + j) * 2] = pixels[i + j * w];

		// forward transform
		FftComplex fft = new FftComplex(fftLength);
		float[] transformed = new float[fftLength * 2];
		fft.complexToComplex(-1, complex, transformed);

		// truncate
		int cutoff = w * h / 10;
		Arrays.fill(transformed, cutoff * 2, transformed.length, 0);

		// inverse transform
		fft.complexToComplex(1, transformed, complex);

		// create new image
		pixels = new float[w * h];
		for (int i = 0; i < w; i++)
			for (int j = 0; j < h; j++)
				pixels[i + j * w] = complex[(i * h + j) * 2];
		ip = new FloatProcessor(w, h, pixels, null);
		new ImagePlus("filtered " + image.getTitle(), ip).show();
	}
}
