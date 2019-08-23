
/*
 * SetNaN2Zero_.java
 *
 * Created on 15. M�rz 2006, 17:11
 * Vollständig ersetzt durch: SetNaN_ToZero.java (KHK 21.08.2009)
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




public class SetNaN2Zero_ implements PlugInFilter {

		
	public int setup(String arg, ImagePlus imp) {
		return DOES_32;
	}

	public void run(ImageProcessor ip) {

		
		int width = ip.getWidth();
		int height = ip.getHeight();
		int length = width * height;



		// define an array which referes to the pixels of the image

		float[] arrayOfImagePixels = (float[])ip.getPixels();


		for (int a=0; a < length; a++) {
				
                    // if (arrayOfImagePixels[a] == 0.0)
                        if (Float.isNaN(arrayOfImagePixels[a]))
                        {arrayOfImagePixels[a] = 0;}
                 }  
        }

	void showAbout() {
		IJ.showMessage("About SetNaN2Zero...","This PlugIn does sets NaN to Zero !");
	}


}

