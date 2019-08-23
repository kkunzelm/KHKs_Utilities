/*
 * SetNaN_ToZero.java
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

public class SetNaN_ToZero implements PlugIn {

	/** Image stack to project. */
	private ImagePlus imp = null;
	private ImageProcessor ip = null;
	private ImageStack stack = null;

	public void setup(String arg, ImagePlus imp) {

	}
	public void run(String arg) {

		imp = WindowManager.getCurrentImage();
		int stackSize = imp.getStackSize();
		if (imp == null) {
			IJ.noImage();
			return;
		}

		// Make sure input image is a stack.
		if (stackSize == 1) {

			ip = imp.getProcessor();
			int width = ip.getWidth();
			int height = ip.getHeight();
			int length = width * height;

			// define an array which referes to the pixels of the image

			float[] arrayOfImagePixels = (float[]) ip.getPixels();

			for (int a = 0; a < length; a++) {

				// if (arrayOfImagePixels[a] == 0.0)
				if (Float.isNaN(arrayOfImagePixels[a])) {
					arrayOfImagePixels[a] = 0;
				}
			}
		}

		if (stackSize > 1) {
			for (int n = 1; n <= stackSize; n++) {
				stack = imp.getStack();
				ip = stack.getProcessor(n);

				int width = ip.getWidth();
				int height = ip.getHeight();
				int length = width * height;

				// define an array which referes to the pixels of the image

				float[] arrayOfImagePixels = (float[]) ip.getPixels();

				for (int a = 0; a < length; a++) {
					if (Float.isNaN(arrayOfImagePixels[a])) {
						arrayOfImagePixels[a] = 0;
					}
				}
			}
		}
	}

	void showAbout() {
		IJ.showMessage("About SetNaN2Zero...", "This PlugIn does sets NaN to Zero !");
	}

}
