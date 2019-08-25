/*
 * SetZero_ToNaN.java
 * Stand: 20.8.09
 * sowohl einfache Bilder, als auch Stacks können mit diesem Plugin bearbeitet werden
 *
 * The Khoros-Diff-Files use ZERO for the undefined areas
 * In ImageJ I decided to use NaN for this purpose
 * Therefore I need a conversion tool
 *
 */

/**
 *
 * @author kkunzelm
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

/* PlugIn: requires no image to be open to start a plugin.
 * PlugInFilter: the currently active image is passed to the plugin when started.
 */

public class SetZero_ToNaN implements PlugIn {

	public void run(String arg) {

		/** Image stack to project. */
		ImagePlus imp = WindowManager.getCurrentImage();
		int stackSize = imp.getStackSize();

		if (imp == null) {
			IJ.noImage();
			return;
		}

		// Make sure input image is a stack.
		ImageProcessor ip;
		if (stackSize == 1) {

			ip = imp.getProcessor();
			int width = ip.getWidth();
			int height = ip.getHeight();
			int length = width * height;

			// define an array which referes to the pixels of the image

			float[] arrayOfImagePixels = (float[]) ip.getPixels();

			for (int a = 0; a < length; a++) {

				if (arrayOfImagePixels[a] == 0.0) {
					arrayOfImagePixels[a] = Float.NaN;
				}
			}
		}

		if (stackSize > 1) {
			ImageStack stack = imp.getStack();

			for (int n = 1; n <= stackSize; n++) {
				ip = stack.getProcessor(n);

				int width = ip.getWidth();
				int height = ip.getHeight();
				int length = width * height;

				// define an array which referes to the pixels of the image

				float[] arrayOfImagePixels = (float[]) ip.getPixels();

				for (int a = 0; a < length; a++) {

					if (arrayOfImagePixels[a] == 0.0) {
						arrayOfImagePixels[a] = Float.NaN;
					}

				}
			}
		}
	}

	void showAbout() {
		IJ.showMessage("About SetNaN2Zero...", "This PlugIn does sets NaN to Zero !");
	}

}
