/*
 * SetZero2NaN_.java
 *
 * Created on 15. M�rz 2006, 17:11
 *
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
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/* PlugIn: requires no image to be open to start a plugin.
 * PlugInFilter: the currently active image is passed to the plugin when started.
 */

public class SetZero2NaN_ implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		return DOES_32;
	}

	public void run(ImageProcessor ip) {

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

	void showAbout() {
		IJ.showMessage("About SetZero2NaN...", "This PlugIn does sets ZERO to NaN !");
	}

}
