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
public class KHKsBwHdrCalculator_ implements PlugIn {

	public static final String lutMessage = "Stacks with inverter LUTs may not project correctly.\n"
			+ "To create a standard LUT, invert the stack (Edit/Invert)\n"
			+ "and invert the LUT (Image/Lookup Tables/Invert LUT).";

	/** Image to hold z-projection. */
	private ImagePlus projImage = null;
	/** Image stack to project. */
	private ImagePlus imp = null;
	/** Projection starts from this slice. */
	private int startSlice = 1;
	/** Projection ends at this slice. */
	private int stopSlice = 1;
	private int sliceCount;

	public KHKsBwHdrCalculator_() {
	}

	/** Construction of KHKsBwHdrCalculator_ with image to be projected. */
	public KHKsBwHdrCalculator_(ImagePlus imp) {
		setImage(imp);
	}

	/**
	 * Explicitly set image to be projected. This is useful if ZProjection_ object
	 * is to be used not as a plugin but as a stand alone processing object.
	 */
	private void setImage(ImagePlus imp) {
		this.imp = imp;
		startSlice = 1;
		stopSlice = imp.getStackSize();
	}

	public void setStartSlice(int slice) {
		if (imp == null || slice < 1 || slice > imp.getStackSize()) {
			return;
		}
		startSlice = slice;
	}

	public void setStopSlice(int slice) {
		if (imp == null || slice < 1 || slice > imp.getStackSize()) {
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
		if (imp.getProcessor().isInvertedLut()) {
			if (!IJ.showMessageWithCancel("KHKsBwHdrCalculator_", lutMessage)) {
				return;
			}
		}

		// Set default bounds.
		startSlice = 1;
		stopSlice = stackSize;

		// Build control dialog
		GenericDialog gd = buildControlDialog(startSlice, stopSlice);
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

		projImage = doMaskedAverageProjection();

		if (arg.equals("") && projImage != null) {
			long tstop = System.currentTimeMillis();
			projImage.setCalibration(imp.getCalibration());

			projImage.show("KHKsBwHdrCalculator_: " + IJ.d2s((tstop - tstart) / 1000.0, 2) + " seconds");
		}

		imp.unlock();
		IJ.register(KHKsBwHdrCalculator_.class);
	}

	/**
	 * Builds dialog to query users for projection parameters.
	 * 
	 * @param start
	 *            starting slice to display
	 * @param stop
	 *            last slice
	 */
	protected GenericDialog buildControlDialog(int start, int stop) {
		GenericDialog gd = new GenericDialog("ZProjection", IJ.getInstance());
		gd.addNumericField("Start slice:", startSlice, 0/* digits */);
		gd.addNumericField("Stop slice:", stopSlice, 0/* digits */);

		return gd;
	}

	String makeTitle() {
		String prefix = "KHKsBwHDR_";
		return WindowManager.makeUniqueName(prefix + imp.getTitle());
	}

	ImagePlus doMaskedAverageProjection() {
		IJ.showStatus("Calculating median...");
		ImageStack stack_local = imp.getStack();
		sliceCount = stopSlice - startSlice + 1;
		ImageProcessor[] slices = new ImageProcessor[sliceCount];
		int index = 0;
		System.out.println("startSlice: " + startSlice);
		System.out.println("stopSlice: " + stopSlice);
		for (int slice = startSlice; slice <= stopSlice; slice += 1) {
			slices[index++] = stack_local.getProcessor(slice);
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
					// System.out.println("i:" + i + "values-i" + values[i]);
				}
				ip2.putPixelValue(x, y, maskedAverage(valueArray));
			}
		}

		IJ.showProgress(1, 1);
		return new ImagePlus(makeTitle(), ip2);
	}

	float maskedAverage(float[] a) {

		int divisor = a.length;
		float sum = 0;

		for (int i = 0; i < a.length; i++) {
			sum += a[i];
			if (a[i] == 0) {
				divisor = divisor - 1;
			}
		}
		float value = sum / divisor;
		return value;

	}

} // end KHKsBwHdrCalculator_
