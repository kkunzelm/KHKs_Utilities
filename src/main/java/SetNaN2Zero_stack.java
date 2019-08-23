
/*
 * SetNaN2Zero_stack.java
 *
 * Created on 09.June 2009, 09:00
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

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class SetNaN2Zero_stack implements PlugInFilter {

    int stackSize;
    ImagePlus imp;
    ImageStack img1;
		
	public int setup(String arg, ImagePlus imp) {
                this.imp = WindowManager.getCurrentImage();
        	if (this.imp==null) {
            		IJ.noImage();
            		return DOES_32;
        		}
        	stackSize = this.imp.getStackSize();
                if (stackSize<2) {
            		IJ.error("Reslicer", "Stack required");
            		
        		}
                      return DOES_32;
        }

	public void run(ImageProcessor ip) {
            
            img1 = imp.getStack();  
            
         
            
            for (int n=1; n<=stackSize; n++){
                ip = img1.getProcessor(n);

                    int width = ip.getWidth();
                    int height = ip.getHeight();
                    int length = width * height;



                    // define an array which referes to the pixels of the image

                    float[] arrayOfImagePixels = (float[])ip.getPixels();


                    for (int a=0; a < length; a++) {
                            if (Float.isNaN(arrayOfImagePixels[a])){
                                arrayOfImagePixels[a] = 0;}
                    }


            }
        }  

	void showAbout() {
		IJ.showMessage("About SetZero2NaN...","This PlugIn does sets NaN to Zero in a Stack !");
	}


}

