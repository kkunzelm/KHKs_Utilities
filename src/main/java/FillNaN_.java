
// KH: this plugin is derived from ii/plugins/filter/filler.java
// KH: this code is nearly the same as in ClearNaNOutside_.java
// KH: only the call to the class differs in Line 42

import java.awt.*;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.Measurements;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * This plugin implements ImageJ's Fill, Clear, Clear Outside and Draw commands.
 */
// nanfill and clearNanOutside added by KH

public class FillNaN_ implements PlugInFilter, Measurements {

	String arg;
	Roi roi;
	ImagePlus imp;
	int sliceCount;
	ImageProcessor mask;

	public int setup(String arg, ImagePlus imp) {

		this.imp = imp;

		if (imp != null)
			roi = imp.getRoi();
		// IJ.register(Filler.class);
		int baseCapabilities = DOES_ALL + ROI_REQUIRED;
		// das Supports_masking war sehr wichtig
		// sonst war immer nur ein bounding rectangle um die ROI gef�llt
		return IJ.setupDialog(imp, baseCapabilities + SUPPORTS_MASKING);

	}

	public void run(ImageProcessor ip) {

		nanfill(ip);

	}

	boolean isLineSelection() {
		return roi != null && roi.isLine();
	}

	boolean isStraightLine() {
		return roi != null && roi.getType() == Roi.LINE;
	}

	// next method added by KH

	public void nanfill(ImageProcessor ip) {
		ip.setValue(Float.NaN);
		if (isLineSelection()) {
			if (isStraightLine() && Line.getWidth() > 1)
				ip.fillPolygon(roi.getPolygon());
			else
				roi.drawPixels();
		} else
			ip.fill(); // fill with Float.NaN
	}

	// KH

	public synchronized void clearNanOutside(ImageProcessor ip) {
		if (isLineSelection()) {
			IJ.error("\"Clear Outside\" does not work with line selections.");
			return;
		}
		sliceCount++;
		Rectangle r = ip.getRoi();
		if (mask == null)
			makeMask(ip, r);
		// ip.setColor(Toolbar.getBackgroundColor());
		ip.setValue(Float.NaN);
		int stackSize = imp.getStackSize();
		if (stackSize > 1)
			ip.snapshot();
		ip.fill();
		ip.reset(mask);
		int width = ip.getWidth();
		int height = ip.getHeight();
		ip.setRoi(0, 0, r.x, height);
		ip.fill();
		ip.setRoi(r.x, 0, r.width, r.y);
		ip.fill();
		ip.setRoi(r.x, r.y + r.height, r.width, height - (r.y + r.height));
		ip.fill();
		ip.setRoi(r.x + r.width, 0, width - (r.x + r.width), height);
		ip.fill();
		ip.setRoi(r); // restore original ROI
		if (sliceCount == stackSize) {
			ip.setColor(Toolbar.getForegroundColor());
			Roi roi = imp.getRoi();
			imp.killRoi();
			imp.updateAndDraw();
			imp.setRoi(roi);
		}
	}

	// KH

	public void makeMask(ImageProcessor ip, Rectangle r) {
		mask = ip.getMask();
		if (mask == null) {
			mask = new ByteProcessor(r.width, r.height);
			mask.invert();
		} else {
			// duplicate mask (needed because getMask caches masks)
			mask = mask.duplicate();
		}
		mask.invert();
	}

}
