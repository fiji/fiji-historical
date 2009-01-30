import edu.mines.jtk.dsp.FftComplex;

import ij.IJ;
import ij.ImagePlus;

import ij.gui.PlotWindow;

import ij.plugin.filter.PlugInFilter;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;

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

		boolean normalizeIntensities = true;
		if (normalizeIntensities) {
			// normalize the pixel values
			float cumul = 0;
			for (int i = 0; i < w * h; i++)
				cumul += pixels[i];
			cumul /= w * h;
			for (int i = 0; i < w * h; i++)
				pixels[i] -= cumul;
		}

		// FFT needs appropriate length
		int fftLength = FftComplex.nfftFast(w * h * 2);

		// make a pseudo-complex array, column by column
		float[] complex = new float[fftLength * 2];
		float cumulative = 0;
		for (int i = 0; i < w; i++)
			for (int j = 0; j < h; j++) {
				complex[(i * h + j) * 2] = cumulative;
				cumulative += pixels[i + j * w];
			}

		// forward transform
		FftComplex fft = new FftComplex(fftLength);
		float[] transformed = new float[fftLength * 2];
		fft.complexToComplex(-1, complex, transformed);

		// truncate
		int leftCutoff = h / 2;
		int rightCutoff = h * 2;
IJ.log("cutoffs were chosen as " + leftCutoff + ", " + rightCutoff);
		Arrays.fill(transformed, 0, leftCutoff * 2, 0);
		Arrays.fill(transformed,
				rightCutoff * 2, transformed.length, 0);

//showPlot(complex, w * h / 2 - 10 * h, w * h / 2 + 10 * h);
		showPlot(transformed, leftCutoff, rightCutoff);

		// inverse transform
		fft.complexToComplex(1, transformed, complex);

		// transform back into intensity values
		pixels = new float[w * h];
		cumulative = 0;
		for (int i = 0; i < w; i++)
			for (int j = 0; j < h; j++) {
				pixels[i + j * w] = complex[(i * h + j) * 2]
					- cumulative;
				cumulative = complex[(i * h + j) * 2];
			}

		// create new image
		ip = new FloatProcessor(w, h, pixels, null);
		new ImagePlus("filtered " + image.getTitle(), ip).show();
	}

	protected PlotWindow plot, plot2;

	protected void showPlot(float[] array,
			int leftCutoff, int rightCutoff) {
		float[] real = new float[rightCutoff - leftCutoff];
		float[] imag = new float[real.length];
		float[] indexes = new float[real.length];

		for (int i = 0; i < real.length; i++) {
			int j = leftCutoff + i;
			indexes[i] = j;
			real[i] = array[j * 2];
			imag[i] = array[j * 2 + 1];
		}

		if (plot == null)
			plot = new PlotWindow("FFT", "i", "Fourier",
					indexes, real);
		else {
			plot.setColor(Color.red);
			plot.addPoints(indexes, real, PlotWindow.LINE);
		}

		boolean separatePlots = true;

		if (separatePlots) {
			if (plot2 == null)
				plot2 = new PlotWindow("FFT", "i",
						"Fourier-imag", indexes, real);
			else {
				plot2.setColor(Color.red);
				plot2.addPoints(indexes, real, PlotWindow.LINE);
			}
			plot2.draw();
		}
		else {
			plot.setColor(Color.blue);
			plot.addPoints(indexes, imag, PlotWindow.LINE);
		}
		//plot.setLimits(0, 20, -300, 300);
		plot.draw();
	}
}
