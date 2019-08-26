// KHKsBwHdrCalculator_
// Bw = Black and White
// Hdr = high dynamic range
// copied and modified from zprojector	

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

/**
 * This plugin performs an average z-projection of the input stack. In contrast
 * to the original z-Projector plugin this version uses the value zero to mask
 * part of the images in some slices, but calculates an average from the
 * remaining slices. Intended use: high dynamic range images from black and
 * white stacks. The masked areas refer to saturation due to over exposure or
 * masking of the noise level with thresholded slices. Type of output image is
 * float.
 * 
 * @author Patrick Kelly <phkelly@ucsd.edu>
 * @author Karl-Heinz Kunzelmann <karl-heinz@kunzelmann.de>
 */
public class KHKsBwHdrCalculator_extended implements PlugIn {
	/** Image stack to project. */
	private ImagePlus imp = null;
	/** Projection starts from this slice. */
	private int startSlice = 1;
	/** Projection ends at this slice. */
	private int stopSlice = 1;

	/** general thresholds to reduze dark noise and white saturated pixels */
	private int upperThreshold = 253;
	private int lowerThreshold = 25;

	/** Array for weights of the slices */

	private int[] weight;

	private void setStartSlice(int slice) {
		if ((imp == null) || (slice < 1) || (slice > imp.getStackSize())) {
			return;
		}
		startSlice = slice;
	}

	private void setStopSlice(int slice) {
		if ((imp == null) || (slice < 1) || (slice > imp.getStackSize())) {
			return;
		}
		stopSlice = slice;
	}

	public void run(String arg) {
		imp = IJ.getImage();
		int stackSize = imp.getStackSize();
		if (imp == null) {
			IJ.noImage();
			return;
		}

		// Make sure input image is a stack.
		if (stackSize == 1) {
			IJ.error("KHKsBwHdrCalculator_", "Stack required");
			return;
		}

		// Check for inverting LUT.
		if ((imp.getProcessor().isInvertedLut()) && (!IJ.showMessageWithCancel("KHKsBwHdrCalculator_",
				"Stacks with inverter LUTs may not project correctly.\nTo create a standard LUT, invert the stack (Edit/Invert)\nand invert the LUT (Image/Lookup Tables/Invert LUT)."))) {
			return;
		}
		weight = new int[stackSize];
		for (int i = 0; i < stackSize; i++) {
			weight[i] = 1;
		}

		// Set default bounds.
		startSlice = 1;
		stopSlice = stackSize;

		// Build control dialog
		GenericDialog gd = buildControlDialog();
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		if (!imp.lock()) {
			return;
		} // exit if in use
		long tstart = System.currentTimeMillis();
		setStartSlice((int) gd.getNextNumber());
		setStopSlice((int) gd.getNextNumber());
		upperThreshold = (int) gd.getNextNumber();
		lowerThreshold = (int) gd.getNextNumber();
		for (int i = 0; i < stackSize; i++) {
			weight[i] = ((int) gd.getNextNumber());
			// System.out.println("weight-"+i+": "+weight[i]);
		}
		/** Image to hold z-projection. */
		ImagePlus projImage = doMaskedAverageProjection();
		if ((arg.equals("")) && (projImage != null)) {
			long tstop = System.currentTimeMillis();
			projImage.setCalibration(imp.getCalibration());

			projImage.show("KHKsBwHdrCalculator_: " + IJ.d2s((tstop - tstart) / 1000.0D, 2) + " seconds");
		}

		imp.unlock();
		IJ.register(KHKsBwHdrCalculator_extended.class);
	}

	/**
	 * Builds dialog to query users for projection parameters.
	 *
	 */
	private GenericDialog buildControlDialog() {
		GenericDialog gd = new GenericDialog("ZProjection", IJ.getInstance());
		gd.addNumericField("Start slice:", startSlice, 0);
		gd.addNumericField("Stop slice:", stopSlice, 0);
		gd.addNumericField("Upper threshold:", upperThreshold, 0);
		gd.addNumericField("Lower threshold:", lowerThreshold, 0);
		for (int i = 0; i < imp.getStackSize(); i++) {
			gd.addNumericField("weight slice: " + (i + 1), weight[i], 1);
		}
		return gd;
	}

	private String makeTitle() {
		String prefix = "KHKsBwHDR_";
		return WindowManager.makeUniqueName(prefix + imp.getTitle());
	}

	private ImagePlus doMaskedAverageProjection() {
		IJ.showStatus("Calculating median...");
		ImageStack stack_local = imp.getStack();
		int sliceCount = (stopSlice - startSlice + 1);
		ImageProcessor[] slices = new ImageProcessor[sliceCount];
		int index = 0;
		// System.out.println("startSlice: " + startSlice);
		// System.out.println("stopSlice: " + stopSlice);
		for (int slice = startSlice; slice <= stopSlice; slice++) {
			slices[(index++)] = stack_local.getProcessor(slice);
		}
		ImageProcessor ip2 = slices[0].duplicate();
		ip2 = ip2.convertToFloat();
		float[] valueArray = new float[sliceCount];
		int width = ip2.getWidth();
		int height = ip2.getHeight();
		int inc = Math.max(height / 30, 1);
		for (int y = 0; y < height; y++) {
			if (y % inc == 0) {
				IJ.showProgress(y, height - 1);
			}
			for (int x = 0; x < width; x++) {
				for (int i = 0; i < sliceCount; i++) {

					valueArray[i] = slices[i].getPixelValue(x, y);
				}
				ip2.putPixelValue(x, y, maskedAverage(valueArray));
			}
		}
		IJ.showProgress(1, 1);
		return new ImagePlus(makeTitle(), ip2);
	}

	private float maskedAverage(float[] a) {
		int divisor = a.length;
		float sum = 0.0F;

		for (int i = 0; i < a.length; i++) {

			// upperThreshold
			if (a[i] >= upperThreshold) {
				a[i] = 0.0F;
			}
			// lowerThreshold
			if (a[i] <= lowerThreshold) {
				a[i] = 0.0F;
			}

			// apply weight
			a[i] = weight[i] * a[i];

			// hdr average
			sum += a[i];
			if (a[i] == 0.0F) {
				divisor -= 1;
			}
		}

		return sum / divisor;
	}
}
