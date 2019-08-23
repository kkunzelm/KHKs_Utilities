/*
 * ConvertBinaryStackTo2dHeightMap.java
 *  
 * Sometimes it would be nice to see the surface of a microct image.
 * 3D Viewer can do this really much bettern than this plugin
 * 
 * To use this plugin in a meaningful way, use Image > Adjust > Threshold
 * to make a binary stack which separates the foreground, which you wish to see,
 * from the background.
 *
 * Now you can apply this plugin.
 * 
 * It will iterate through all pixels from the first slice in the stack to the last one = nth slice. 
 * For each pixel from slice 0 to n the first slice it is checked whether 
 * a foreground or background pixel is present. > 0 = foreground, 0 = background
 * if a foreground pixel is found the slice number will be interpreted a height.
 * if no foreground pixel is found the value zero will be assigned to the pixel.
 * 
 * In a next step use KHKs_Utilities > Greyanimate to greycast the surface
 *
 * This plugin is heavily based on the code of ZProjector.java from @author Patrick Kelly <phkelly@ucsd.edu>
 *
 * Take care: there might arise a problem when ImageJ stores the last preference (i.e. the heightmap) in
 *            .imagej/IJ_Prefs.txt -> if the number is not within the limits of the zprojector array, we will get an error.
 * 
 *
 */

/*
 * 
 * @author kkunzelm
 * @date 15.8.2011
 */

import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.*;



/** This plugin performs a z-projection of the input stack. Type of
    output image is same as type of input image.

    @author Patrick Kelly <phkelly@ucsd.edu> */

public class ConvertBinaryStackTo2dHeightMap_ implements PlugIn {
   /* public static final int AVG_METHOD = 0;
    public static final int MAX_METHOD = 1;
    public static final int MIN_METHOD = 2;
    public static final int SUM_METHOD = 3;
	public static final int SD_METHOD = 4;
	public static final int MEDIAN_METHOD = 5;*/
    
        public static final int HEIGHT_MAP = 6;
	public static final String[] METHODS =
		{"2D-HeightMap"};

    private static final String METHOD_KEY = "zproject.method";

    private int method = (int)Prefs.get(METHOD_KEY, HEIGHT_MAP);



    public static final String lutMessage =
    	"Stacks with inverter LUTs may not project correctly.\n"
    	+"To create a standard LUT, invert the stack (Edit/Invert)\n"
    	+"and invert the LUT (Image/Lookup Tables/Invert LUT).";

    /** Image to hold z-projection. */
    private ImagePlus projImage = null;

    /** Image stack to project. */
    private ImagePlus imp = null;

    /** Projection starts from this slice. */
    private int startSlice = 1;
    /** Projection ends at this slice. */
    private int stopSlice;
    /** Project all time points? */
    private boolean allTimeFrames = true;
    private boolean invertedProjectionDirection = false;
    private int voxelSizeZ = 1;

    private boolean isHyperstack;
    private int increment = 1;
    private int sliceCount;

    public ConvertBinaryStackTo2dHeightMap_() {
    }

    /** Construction of ZProjector with image to be projected. */
    public ConvertBinaryStackTo2dHeightMap_(ImagePlus imp) {
		setImage(imp);
    }

    /** Explicitly set image to be projected. This is useful if
	ZProjection_ object is to be used not as a plugin but as a
	stand alone processing object.  */
    public void setImage(ImagePlus imp) {
    	this.imp = imp;
		startSlice = 1;
		stopSlice = imp.getStackSize();
                System.out.println("stack size: " + stopSlice);
    }

    public void setStartSlice(int slice) {
		if(imp==null || slice < 1 || slice > imp.getStackSize())
	    	return;
		startSlice = slice;
    }

    public void setStopSlice(int slice) {
		if(imp==null || slice < 1 || slice > imp.getStackSize())
	    	return;
		stopSlice = slice;
    }
   

    public void setMethod(int projMethod){
            method = projMethod;
    }

    /** Retrieve results of most recent projection operation.*/
    public ImagePlus getProjection() {
		return projImage;
    }

    public void run(String arg) {
		imp = WindowManager.getCurrentImage();
		if (imp==null) {
	    	IJ.noImage();
	    	return;
		}
		int stackSize = imp.getStackSize();
                
		//  Make sure input image is a stack.
		if(stackSize==1) {
	    	IJ.error("2dHeightMap_", "Stack required");
	    	return;
		}

		//  Check for inverting LUT.
		if (imp.getProcessor().isInvertedLut()) {
	    	if (!IJ.showMessageWithCancel("2dHeightMap_", lutMessage))
	    		return;
		}

		// Set default bounds.
		int frames = imp.getNFrames();
		int slices = imp.getNSlices();
		isHyperstack = imp.isHyperStack()||( ij.macro.Interpreter.isBatchMode()&&((frames>1&&frames<stackSize)||(slices>1&&slices<stackSize)));
		startSlice = 1;
		if (isHyperstack) {
			int nSlices = imp.getNSlices();
			if (nSlices>1)
				stopSlice = nSlices;
			else
				stopSlice = imp.getNFrames();
		} else
			stopSlice  = stackSize;

		// Build control dialog
		GenericDialog gd = buildControlDialog(startSlice,stopSlice);
		gd.showDialog();
		if(gd.wasCanceled()) return;

		if (!imp.lock()) return;   // exit if in use
		long tstart = System.currentTimeMillis();
		setStartSlice((int)gd.getNextNumber());
		setStopSlice((int)gd.getNextNumber());
                voxelSizeZ =(int)gd.getNextNumber();
                invertedProjectionDirection=gd.getNextBoolean();
		method = gd.getNextChoiceIndex();
		Prefs.set(METHOD_KEY, method);

			doProjection();

		if (arg.equals("") && projImage!=null) {
			long tstop = System.currentTimeMillis();
			projImage.setCalibration(imp.getCalibration());
	    	projImage.show("2dHeightMap_: " +IJ.d2s((tstop-tstart)/1000.0,2)+" seconds");
		}

		imp.unlock();
		IJ.register(ConvertBinaryStackTo2dHeightMap_.class);
		return;
    }

   

    /** Builds dialog to query users for projection parameters.
	@param start starting slice to display
	@param stop last slice
     * @return  */
    protected GenericDialog buildControlDialog(int start, int stop) {
		GenericDialog gd = new GenericDialog("ZProjection",IJ.getInstance());
		gd.addNumericField("Start slice:",startSlice,0/*digits*/);
		gd.addNumericField("Stop slice:",stopSlice,0/*digits*/);
                gd.addNumericField("Voxel size in z [µm]:", voxelSizeZ,0);
		gd.addCheckbox("Reverse Projection Direction: stop > start", invertedProjectionDirection);
		if (isHyperstack && imp.getNFrames()>1&& imp.getNSlices()>1)
			gd.addCheckbox("All Time Frames", allTimeFrames);
		return gd;
    }

    /** Performs actual projection using specified method. */
    public void doProjection() {
		if (imp==null)
			return;
                sliceCount = 0;
                for (int slice=startSlice; slice<=stopSlice; slice+=increment)
                sliceCount++; // KHK: total number of slices between start and stop slice
 
                projImage = doHeightMapProjection();
                
                ContrastEnhancer ce = new ContrastEnhancer();
                double saturated = 0.35;
                projImage.resetDisplayRange();
                ce.stretchHistogram(projImage, saturated);
		projImage.updateAndDraw();
		
	

    }


    String makeTitle() {
    	String prefix = "zProj_";
 		switch (method) {
                        case HEIGHT_MAP:  prefix = "2dMap_"; break;
	    }
    	return WindowManager.makeUniqueName(prefix+imp.getTitle());
    }


        ImagePlus doHeightMapProjection() {
		IJ.showStatus("Calculating 2d Height Map...");
		ImageStack stack = imp.getStack();


                ImageProcessor[] slices = new ImageProcessor[sliceCount];
		int index = 0;
		for (int slice=startSlice; slice<=stopSlice; slice+=increment)
			slices[index++] = stack.getProcessor(slice);
		ImageProcessor ip2 = slices[0].duplicate();
                ip2 = ip2.convertToFloat();
                ip2.multiply(0.0);
		float[] values = new float[sliceCount];
		int width = ip2.getWidth();
		int height = ip2.getHeight();
		int inc = Math.max(height/30, 1); // for ProgressBar
		for (int y=0; y<height; y++) {
			if (y%inc==0) IJ.showProgress(y, height-1);
			for (int x=0; x<width; x++) {
                            if (invertedProjectionDirection){
                                for (int i=sliceCount-1; i>=0; i--){
                                    if ((ip2.getPixelValue(x, y)==0) && (slices[i].getPixelValue(x, y)>0)){
                                            ip2.putPixelValue(x, y, i+1);
                                        }
                                }
                            }
                            else{
                                for (int i=0; i<sliceCount; i++)
                                    if ((ip2.getPixelValue(x, y)==0) && (slices[i].getPixelValue(x, y)>0)){
                                            ip2.putPixelValue(x, y, sliceCount - i);
                                        }
                            }
				

			}
		}
                ip2.multiply(voxelSizeZ*0.001);  // *0.001 -> unit is Micrometers
		return (new ImagePlus(makeTitle(),ip2));
	}

}  // end ZProjection


