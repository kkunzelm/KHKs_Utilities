
// KH: this plugin is derived from ii/plugins/filter/filler.java

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * This plugin implements ImageJ's Fill, Clear, Clear Outside and Draw commands.
 */
// nanfill and clearNanOutside added by KH

public class FillNaN_ implements PlugInFilter, Measurements {

	private Roi roi;

	public int setup(String arg, ImagePlus imp) {

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

	private boolean isLineSelection() {
		return roi != null && roi.isLine();
	}

	private boolean isStraightLine() {
		return roi != null && roi.getType() == Roi.LINE;
	}

	// next method added by KH

	private void nanfill(ImageProcessor ip) {
		ip.setValue(Float.NaN);
		if (isLineSelection()) {
			if (isStraightLine() && Line.getWidth() > 1)
				ip.fillPolygon(roi.getPolygon());
			else
				roi.drawPixels(ip);
		} else
			ip.fill(); // fill with Float.NaN
	}

}
