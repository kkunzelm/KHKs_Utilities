
/*
 * SetZero2NaN_stack.java
 *
 * Created on 09.June 2009, 09:00
 * Vollständig ersetzt durch: SetZero_ToNaN.java (KHK 21.08.2009)
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author kkunzelm
 */
/**
 * 
 * The Khoros-Diff-Files use ZERO for the undefined areas 
 * In ImageJ I decided to use NaN for this purpose
 * Therefore I need a conversion tool 
 *
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class SetZero2NaN_stack implements PlugInFilter {

	int stackSize;
	ImagePlus imp1;
	ImageStack img1;

	public int setup(String arg, ImagePlus imp) {
		imp1 = WindowManager.getCurrentImage();
		if (imp1 == null) {
			IJ.noImage();
			return DOES_32;
		}
		stackSize = imp1.getStackSize();
		if (stackSize < 2) {
			IJ.error("Reslicer", "Stack required");

		}
		return DOES_32;
	}

	public void run(ImageProcessor ip) {

		img1 = imp1.getStack();

		for (int n = 1; n <= stackSize; n++) {
			ip = img1.getProcessor(n);

			int width = ip.getWidth();
			int height = ip.getHeight();
			int length = width * height;

			// define an array which referes to the pixels of the image

			float[] arrayOfImagePixels = (float[]) ip.getPixels();

			for (int a = 0; a < length; a++) {
				if (arrayOfImagePixels[a] == 0.0)

				{
					arrayOfImagePixels[a] = Float.NaN;
				}
			}

		}

	}

	void showAbout() {
		IJ.showMessage("About SetZero2NaN...", "This PlugIn does sets ZERO to NaN !");
	}

}
