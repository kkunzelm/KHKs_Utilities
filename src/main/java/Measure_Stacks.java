import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.*;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.text.TextPanel;

import javax.swing.*;

/**
 * KH: obviously KHs Version - überarbeitet 21.8.2009
 *
 * Man kann mit diesem Plugin in einem Stack verschiedene ROI in
 * unterschiedlichen Slices markieren. Durch Interpolation zwischen den ROI
 * werden ähnliche ROIs in die nicht markierte Slices eingetragen
 * 
 * KHs Beitrag: Optionen für Clear und Fill mit verschiedenen Farben
 * (Background/Forground) und NaN
 * 
 * This plugin measures volumes in stacks. Bob Dougherty 6/11/2002 Version 0:
 * 6/11/2002 Version 1: 6/24/2002 Interpolated ROIs are red. Close box works.
 * Review Button added. Slice lebels added.
 * 
 * Custom version 11/10/2002. Performs the measurements on a separate stack.
 */
/*
 * License: Copyright (c) 2002, 2005, OptiNav, Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. Neither the name of OptiNav, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
public class Measure_Stacks implements PlugInFilter, Measurements {

	private ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL + SUPPORTS_MASKING;

	}

	public void run(ImageProcessor ip) {
		new MeasureStacks(imp);
	}

}

class MeasureStacks extends Dialog implements ActionListener, Runnable, WindowListener {
	private static final int NPOLY = 1000;
	private final Object resource = "resource";// For synchronization.
	private final ImagePlus imp;
	private final Roi[] roi;
	private final boolean[] userInput;
	private final int numSlices;
	private int oldSlice;
	private boolean done = false;
	private Button spacing;
	private Button review;
	private Button measure;
	private Button clearOutsideWithNan;
	private Button fillWithNan;
	private Button clearOutsideWithForegroundColor;
	private Button clearOutsideWithBackgroundColor;
	private Button fillWithForeground;
	private Button fillWithBackground;
	private Button quit;
	private int roiType = -1;
	private double sliceSpacing = 1;
	private ImageProcessor mask;

	public MeasureStacks(ImagePlus imp) {
		// super((Frame)WindowManager.getCurrentImage().getWindow(),"Measure
		// Stack",false);
		super(IJ.getInstance(), "Measure Stack", false);
		addWindowListener(this);
		this.imp = imp;
		numSlices = imp.getStackSize();
		roi = new Roi[numSlices];
		userInput = new boolean[numSlices];
		for (int i = 0; i < numSlices; i++)
			userInput[i] = false;
		oldSlice = imp.getCurrentSlice();
		Thread thread = new Thread(this, "MeasureStack");

		setup();
		thread.start();
	}
	private void setup() {
		setLayout(new GridLayout(2, 2, 5, 5));
		// setLayout(new BorderLayout(3,3));
		spacing = new Button("Slice Spacing");
		spacing.addActionListener(this);
		review = new Button("Review areas");
		review.addActionListener(this);
		measure = new Button("Measure");
		measure.addActionListener(this);
		clearOutsideWithNan = new Button("clearOutsideWithNan");
		clearOutsideWithNan.addActionListener(this);
		fillWithNan = new Button("fillWithNan");
		fillWithNan.addActionListener(this);
		clearOutsideWithForegroundColor = new Button("ClearOutsideWithFgColor");
		clearOutsideWithForegroundColor.addActionListener(this);
		fillWithForeground = new Button("FillWithFgColor");
		fillWithForeground.addActionListener(this);
		clearOutsideWithBackgroundColor = new Button("ClearOutsideWithBgColor");
		clearOutsideWithBackgroundColor.addActionListener(this);
		fillWithBackground = new Button("FillWithBgColor");
		fillWithBackground.addActionListener(this);

		quit = new Button("Quit");
		quit.addActionListener(this);
		add(measure);
		add(review);
		add(spacing);
		add(fillWithNan);
		add(clearOutsideWithNan);
		add(fillWithForeground);
		add(clearOutsideWithForegroundColor);
		add(fillWithBackground);
		add(clearOutsideWithBackgroundColor);
		add(quit);
		pack();
		setVisible(true);
	}

	public void run() {
		while (!done) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException ignored) {
			}
			synchronized (resource) {// Obtain a lock to aviod interfering with doMeasurements.
				checkSlice();
			}
		}
	}

	private void checkSlice() {
		int slice = imp.getCurrentSlice();
		if (slice != oldSlice) {
			// System.out.println("slice moved: "+oldSlice+" to "+slice);
			updateRoi(oldSlice, slice);
			oldSlice = slice;
		}
	}

	private void updateCurrentRoi() {
		// Update the stored ROI for the old slice if the user changed it.
		Roi newRoi = imp.getRoi();
		int slice = imp.getCurrentSlice();
		if (newRoi == null) {
			roi[slice - 1] = null;
			userInput[oldSlice - 1] = false;
		} else {
			int newType = newRoi.getType();
			if (newType == Roi.FREELINE)
				newType = Roi.POLYLINE;
			if (newType != roiType) {
				for (int i = 0; i < numSlices; i++) {
					roi[i] = null;
					userInput[i] = false;
				}
				roiType = newType;
			}
			if (roi[slice - 1] == null) {
				roi[slice - 1] = (Roi) newRoi.clone();
				userInput[slice - 1] = true;
			} else {
				if (differentRoi(newRoi, roi[slice - 1])) {
					roi[slice - 1] = (Roi) newRoi.clone();
					userInput[slice - 1] = true;
				}
			}
		}
	}

	private void updateRoi(int oldSlice, int slice) {
		// Update the stored ROI for the old slice if the user changed it.
		Roi newRoi = imp.getRoi();
		if (newRoi == null) {
			roi[oldSlice - 1] = null;
			userInput[oldSlice - 1] = false;
		} else {
			int newType = newRoi.getType();
			if (newType == Roi.FREELINE)
				newType = Roi.POLYLINE;
			if (newType != roiType) {
				for (int i = 0; i < numSlices; i++) {
					roi[i] = null;
					userInput[i] = false;
				}
				roiType = newType;
			}
			if (roi[oldSlice - 1] == null) {
				roi[oldSlice - 1] = (Roi) newRoi.clone();
				userInput[oldSlice - 1] = true;
			} else {
				if (differentRoi(newRoi, roi[oldSlice - 1])) {
					roi[oldSlice - 1] = (Roi) newRoi.clone();
					userInput[oldSlice - 1] = true;
				}
			}
		}
		showRoi(slice, imp);
	}

	private void showRoi(int slice, ImagePlus imp) {
		// Interpolate the roi for slice from leading and following slices
		// that have user input roi's.
		// First check to see of the slice itself has user input.
		if (userInput[slice - 1]) {
			Roi.setColor(Color.yellow);
			imp.setRoi((Roi) roi[slice - 1].clone());
			return;
		}

		// OK, now try to interploate. First find the user input slice to the left.
		int sMinus = slice;
		while ((sMinus > 1) && (!userInput[--sMinus - 1]));
		if (!userInput[sMinus - 1]) {
			imp.killRoi();
			return;
		}

		// Find the user input slice to the right.
		int sPlus = slice;
		while ((sPlus < numSlices) && (!userInput[++sPlus - 1]));
		if (!userInput[sPlus - 1]) {
			imp.killRoi();
			return;
		}

		// Get linear interpolation coefficients.
		double delta = sPlus - sMinus;
		double wMinus = (sPlus - slice) / delta;
		double wPlus = (slice - sMinus) / delta;
		roi[slice - 1] = (Roi) roiInterp(wMinus, wPlus, roi[sMinus - 1], roi[sPlus - 1]).clone();
		userInput[slice - 1] = false;
	}

	private boolean differentRoi(Roi roi1, Roi roi2) {
		int type1 = roi1.getType();
		if (type1 == Roi.FREELINE)
			type1 = Roi.POLYLINE;
		int type2 = roi2.getType();
		if (type2 == Roi.FREELINE)
			type2 = Roi.POLYLINE;
		if (type1 != type2)
			return true;

		Rectangle rect1 = roi1.getBounds();
		Rectangle rect2 = roi2.getBounds();
		if (rect1.x != rect2.x)
			return true;
		if (rect1.y != rect2.y)
			return true;
		if (rect1.width != rect2.width)
			return true;
		if (rect1.height != rect2.height)
			return true;

		if ((type1 == Roi.POLYGON) || (type1 == Roi.FREEROI) || (type1 == Roi.TRACED_ROI) || (type1 == Roi.POLYLINE)) {
			PolygonRoi r1 = (PolygonRoi) roi1;
			PolygonRoi r2 = (PolygonRoi) roi2;
			int n1 = r1.getNCoordinates();
			int n2 = r2.getNCoordinates();
			if (n1 != n2)
				return true;
			int[] x1 = r1.getXCoordinates();
			int[] y1 = r1.getYCoordinates();
			int[] x2 = r2.getXCoordinates();
			int[] y2 = r2.getYCoordinates();
			for (int i = 0; i < n1; i++) {
				if (x1[i] != x2[i])
					return true;
				if (y1[i] != y2[i])
					return true;
			}
		} else if (type1 == Roi.LINE) {
			Line l1 = (Line) roi1;
			Line l2 = (Line) roi2;
			if (l1.x1 != l2.x1)
				return true;
			if (l1.y1 != l2.y1)
				return true;
			if (l1.x2 != l2.x2)
				return true;
			return l1.y2 != l2.y2;
		}
		return false;
	}

	private Roi roiInterp(double w1, double w2, Roi roi1, Roi roi2) {
		imp.killRoi();
		Roi result = null;
		if ((roi1 != null) || (roi2 != null)) {
			if (roi1 == null) {
				result = (Roi) roi2.clone();
				Roi.setColor(Color.yellow);
				imp.setRoi(result);
			} else if (roi2 == null) {
				result = (Roi) roi1.clone();
				Roi.setColor(Color.yellow);
				imp.setRoi(result);
			} else {
				Roi.setColor(Color.red);
				int type1 = roi1.getType();
				int type2 = roi2.getType();
				if (type1 == Roi.FREELINE)
					type1 = Roi.POLYLINE;
				if (type2 == Roi.FREELINE)
					type2 = Roi.POLYLINE;
				Rectangle rect1 = roi1.getBounds();
				Rectangle rect2 = roi2.getBounds();
				if ((type1 == Roi.RECTANGLE) | (type1 == Roi.OVAL)) {
					int x = (int) Math.round(w1 * rect1.x + w2 * rect2.x);
					int y = (int) Math.round(w1 * rect1.y + w2 * rect2.y);
					int width = (int) Math.round(w1 * rect1.width + w2 * rect2.width);
					int height = (int) Math.round(w1 * rect1.height + w2 * rect2.height);
					if (type1 == Roi.OVAL) {
						result = new OvalRoi(x, y, width, height);
						result.setImage(imp);
					} else {
						result = new Roi(x, y, width, height);
						result.setImage(imp);
					}
					imp.setRoi(result);
				} else if (type1 == Roi.LINE) {
					Line r1 = (Line) roi1;
					Line r2 = (Line) roi2;
					int ox1 = (int) Math.round(w1 * r1.x1 + w2 * r2.x1);
					int oy1 = (int) Math.round(w1 * r1.y1 + w2 * r2.y1);
					int ox2 = (int) Math.round(w1 * r1.x2 + w2 * r2.x2);
					int oy2 = (int) Math.round(w1 * r1.y2 + w2 * r2.y2);
					Line rl = new Line(ox1, oy1, ox2, oy2);
					rl.setImage(imp);
					rl.x1 = ox1;// Strangely, this seems necessary.
					rl.y1 = oy1;
					rl.x2 = ox2;
					rl.y2 = oy2;
					imp.setRoi(rl);
					result = rl;
				} else if ((type1 == Roi.POLYGON) || (type1 == Roi.FREEROI) || (type1 == Roi.TRACED_ROI)
						|| (type1 == Roi.POLYLINE)) {
					PolygonRoi r1 = (PolygonRoi) roi1;
					PolygonRoi r2 = (PolygonRoi) roi2;
					int n1 = r1.getNCoordinates();
					int n2 = r2.getNCoordinates();
					int[] x1Data = r1.getXCoordinates();
					int[] y1Data = r1.getYCoordinates();
					int[] x1 = new int[n1];
					int[] y1 = new int[n1];
					for (int i = 0; i < n1; i++) {
						x1[i] = x1Data[i] + rect1.x;
						y1[i] = y1Data[i] + rect1.y;
					}
					int[] x2Data = r2.getXCoordinates();
					int[] y2Data = r2.getYCoordinates();
					int[] x2 = new int[n2];
					int[] y2 = new int[n2];
					for (int i = 0; i < n2; i++) {
						x2[i] = x2Data[i] + rect2.x;
						y2[i] = y2Data[i] + rect2.y;
					}
					double[] arc1 = arcLength(n1, x1, y1);
					double[] arc2 = arcLength(n2, x2, y2);
					double[] x1Interp = interp(n1, arc1, x1);
					double[] y1Interp = interp(n1, arc1, y1);
					double[] x2Interp = interp(n2, arc2, x2);
					double[] y2Interp = interp(n2, arc2, y2);
					int[] x = interpCoords(NPOLY, w1, w2, x1Interp, x2Interp);
					int[] y = interpCoords(NPOLY, w1, w2, y1Interp, y2Interp);
					int xLoc = locate(x);
					int yLoc = locate(y);
					result = new PolygonRoi(x, y, x.length, type1);
					result.setImage(imp);
					result.setLocation(xLoc, yLoc);
					imp.setRoi(result);
				}
			}
		}
		return result;
	}

	/*
	 * Interpolate f, given at n t values, at integer points 0,...,nOut-1. It can be
	 * assumed that t is increasing, t[0] = 0, and t[n-1] = nOut - 1.
	 */
	private double[] interp(int n, double[] t, int[] f) {
		double[] result = new double[MeasureStacks.NPOLY];
		result[0] = f[0];
		int jBeyond = 1;
		for (int i = 1; i < MeasureStacks.NPOLY; i++) {
			while (t[jBeyond] < i) {
				if (jBeyond == (n - 1))
					break;
				jBeyond++;
			}
			double delta = t[jBeyond] - t[jBeyond - 1];
			if (delta == 0) {
				result[i] = f[jBeyond];
			} else {
				result[i] = ((t[jBeyond] - i) * f[jBeyond - 1] + (i - t[jBeyond - 1]) * f[jBeyond]) / delta;
			}
		}
		return result;
	}

	/* Subtract the minimum */
	private int locate(int[] x) {
		int result = x[0];
		for (int i = 1; i < MeasureStacks.NPOLY; i++) {
			if (x[i] < result)
				result = x[i];
		}
		for (int i = 0; i < MeasureStacks.NPOLY; i++)
			x[i] -= result;
		return result;
	}

	private double[] arcLength(int n, int[] x, int[] y) {
		double[] result = new double[n];
		result[0] = 0;
		for (int i = 1; i < n; i++) {
			result[i] = result[i - 1]
					+ Math.sqrt((x[i] - x[i - 1]) * (x[i] - x[i - 1]) + (y[i] - y[i - 1]) * (y[i] - y[i - 1]));
		}
		double scale = result[n - 1] / NPOLY - 1;
		if (scale > 0) {
			for (int i = 1; i < n; i++) {
				result[i] /= scale;
			}
		}
		return result;
	}

	/* Interpolate two curves. */
    private int[] interpCoords(int n, double w1, double w2, double[] x1, double[] x2) {
		int[] result = new int[n];
		for (int i = 0; i < n; i++) {
			result[i] = (int) Math.round(w1 * x1[i] + w2 * x2[i]);
		}
		return result;
	}

	public void windowClosing(WindowEvent e) {
		shutDown();
	}
	public void windowClosed(WindowEvent e) {
	}
	public void windowActivated(WindowEvent e) {
	}
	public void windowDeactivated(WindowEvent e) {
	}
	public void windowDeiconified(WindowEvent e) {
	}
	public void windowIconified(WindowEvent e) {
	}
	public void windowOpened(WindowEvent e) {
	}

	private void shutDown() {
		done = true;
		Roi.setColor(Color.yellow);
		imp.killRoi();
		setVisible(false);
		dispose();

	}

	private void getSpacing() {
		GenericDialog gd = new GenericDialog("Slice Spacing...", IJ.getInstance());
		gd.addNumericField("Distance between slices (for volume) ", sliceSpacing, 5);
		gd.showDialog();
		sliceSpacing = gd.getNextNumber();
	}

	private ImagePlus chooseStack(ImagePlus imp) {
		int[] wList = WindowManager.getIDList();
		String[] titles = new String[wList.length];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus impi = WindowManager.getImage(wList[i]);
			titles[i] = impi != null ? impi.getTitle() : "";
		}

		GenericDialog gd = new GenericDialog("Stack to Measure");
		gd.addChoice("Stack to measure:", titles, titles[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return imp;
		int index = gd.getNextChoiceIndex();

		return WindowManager.getImage(wList[index]);
	}

	private void doMeasurements() {
		IJ.run("Clear Results");
		int measurements = Analyzer.getMeasurements();
		Analyzer.setMeasurements(measurements);
		Analyzer a = new Analyzer();
		ResultsTable rt = Analyzer.getResultsTable();
		synchronized (resource) {// Lock out checkSlice.

			ImagePlus impMeas = chooseStack(imp);// Get the stack to measure
			oldSlice = imp.getCurrentSlice();
			for (int slice = 1; slice <= numSlices; slice++) {
				impMeas.setSlice(slice);
				showRoi(slice, impMeas);
				Roi roi = impMeas.getRoi();
				if (roi != null) {
					ImageStatistics stats = impMeas.getStatistics(measurements);
					a.saveResults(stats, roi);
					// IJ.run("Measure");
					rt.addValue("Slice", slice);
					a.displayResults();
					a.updateHeadings();
				}
			}
			imp.setSlice(oldSlice);
			showRoi(oldSlice, imp);

			int cA;
			int cM;
			double sum;
			double total;

			IJ.write("");
			// Area measurements
			try {
				cA = rt.getColumnIndex("Area");
				if (cA != ResultsTable.COLUMN_NOT_FOUND) {
					sum = 0;
					for (int i = 0; i < rt.getCounter(); i++) {
						sum += rt.getValueAsDouble(cA, i);
					}
					IJ.write("Volume: " + IJ.d2s(sum * sliceSpacing, 3));

					try {
						if (sum > 0) {
							cM = rt.getColumnIndex("Mean");
							if (cM != ResultsTable.COLUMN_NOT_FOUND) {
								total = 0;
								for (int i = 0; i < rt.getCounter(); i++) {
									total += rt.getValueAsDouble(cM, i) * rt.getValueAsDouble(cA, i);
								}
								IJ.write("Overall Volume Mean: " + IJ.d2s(total / sum, 3));
							}
						}
					} catch (IllegalArgumentException ignored) {
					}
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object b = e.getSource();
		if (b == spacing) {
			getSpacing();
		} else if (b == measure) {
			updateCurrentRoi();
			doMeasurements();
		} else if (b == review) {
			new MeasureStackReviewer(imp);
		} else if (b == clearOutsideWithNan) {
			updateCurrentRoi();
			clearOutsideWithNan();
		} else if (b == fillWithNan) {
			updateCurrentRoi();
			fillWithNan();
		} else if (b == clearOutsideWithForegroundColor) {
			updateCurrentRoi();
			clearOutsideWithForegroundColor();
		} else if (b == fillWithForeground) {
			updateCurrentRoi();
			fillWithForegroundColor();
		} else if (b == clearOutsideWithBackgroundColor) {
			updateCurrentRoi();
			clearOutsideWithBackgroundColor();
		} else if (b == fillWithBackground) {
			updateCurrentRoi();
			fillWithBackgroundColor();
		} else if (b == quit) {
			shutDown();
		}
	}

	// KH

	private synchronized void clearOutsideWithNan() {
		synchronized (resource) {// Lock out checkSlice.

			ImagePlus impMeas = chooseStack(imp);// Get the stack to measure
			ImageProcessor ip = impMeas.getProcessor();
			oldSlice = imp.getCurrentSlice();
			for (int slice = 1; slice <= numSlices; slice++) {
				impMeas.setSlice(slice);
				showRoi(slice, impMeas);
				Roi roi = impMeas.getRoi();
				if (roi != null) {

					Rectangle r = ip.getRoi();
					mask = impMeas.getMask();
					if (mask == null)
						makeMask(ip, r);
					mask.invert();

					// ip.setColor(Toolbar.getForegroundColor());
					// ip.setColor(Toolbar.getBackgroundColor());
					ip.setValue(Float.NaN);
					int stackSize = imp.getStackSize();
					if (stackSize > 1)
						ip.snapshot();
					ip.fill();
					ip.reset(mask);

					// *****

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

					// *****
					ip.setRoi(r); // restore original ROI
				}
			}
			imp.setSlice(oldSlice);
			showRoi(oldSlice, imp);
		}
	}

	private void fillWithNan() {

		synchronized (resource) {// Lock out checkSlice.

			ImagePlus impMeas = chooseStack(imp);// Get the stack to measure
			ImageProcessor ip = impMeas.getProcessor();
			oldSlice = imp.getCurrentSlice();
			for (int slice = 1; slice <= numSlices; slice++) {
				impMeas.setSlice(slice);
				showRoi(slice, impMeas);
				Roi roi = impMeas.getRoi();
				if (roi != null) {

					Rectangle r = ip.getRoi();
					mask = impMeas.getMask();
					if (mask == null)
						makeMask(ip, r);

					// ip.setColor(Toolbar.getForegroundColor());
					// ip.setColor(Toolbar.getBackgroundColor());
					ip.setValue(Float.NaN);
					int stackSize = imp.getStackSize();
					if (stackSize > 1)
						ip.snapshot();
					ip.fill();
					ip.reset(mask);

					ip.setRoi(r); // restore original ROI
				}
			}
			imp.setSlice(oldSlice);
			showRoi(oldSlice, imp);
		}

	}

	private synchronized void clearOutsideWithForegroundColor() {

		synchronized (resource) {// Lock out checkSlice.

			ImagePlus impMeas = chooseStack(imp);// Get the stack to measure
			ImageProcessor ip = impMeas.getProcessor();
			oldSlice = imp.getCurrentSlice();
			for (int slice = 1; slice <= numSlices; slice++) {
				impMeas.setSlice(slice);
				showRoi(slice, impMeas);
				Roi roi = impMeas.getRoi();
				if (roi != null) {

					Rectangle r = ip.getRoi();
					mask = impMeas.getMask();
					if (mask == null)
						makeMask(ip, r);
					mask.invert();

					ip.setColor(Toolbar.getForegroundColor());
					// ip.setColor(Toolbar.getBackgroundColor());
					// ip.setValue(Float.NaN);
					int stackSize = imp.getStackSize();
					if (stackSize > 1)
						ip.snapshot();
					ip.fill();
					ip.reset(mask);

					// *****

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

					// *****
					ip.setRoi(r); // restore original ROI
				}
			}
			imp.setSlice(oldSlice);
			showRoi(oldSlice, imp);
		}

	}

	private void fillWithForegroundColor() {

		synchronized (resource) {// Lock out checkSlice.

			ImagePlus impMeas = chooseStack(imp);// Get the stack to measure
			ImageProcessor ip = impMeas.getProcessor();
			oldSlice = imp.getCurrentSlice();
			for (int slice = 1; slice <= numSlices; slice++) {
				impMeas.setSlice(slice);
				showRoi(slice, impMeas);
				Roi roi = impMeas.getRoi();
				if (roi != null) {

					Rectangle r = ip.getRoi();
					mask = impMeas.getMask();
					if (mask == null)
						makeMask(ip, r);

					ip.setColor(Toolbar.getForegroundColor());
					// ip.setColor(Toolbar.getBackgroundColor());
					// ip.setValue(Float.NaN);
					int stackSize = imp.getStackSize();
					if (stackSize > 1)
						ip.snapshot();
					ip.fill();
					ip.reset(mask);

					ip.setRoi(r); // restore original ROI
				}
			}
			imp.setSlice(oldSlice);
			showRoi(oldSlice, imp);
		}
	}

	private synchronized void clearOutsideWithBackgroundColor() {

		synchronized (resource) {// Lock out checkSlice.

			ImagePlus impMeas = chooseStack(imp);// Get the stack to measure
			ImageProcessor ip = impMeas.getProcessor();
			oldSlice = imp.getCurrentSlice();
			for (int slice = 1; slice <= numSlices; slice++) {
				impMeas.setSlice(slice);
				showRoi(slice, impMeas);
				Roi roi = impMeas.getRoi();
				if (roi != null) {

					Rectangle r = ip.getRoi();
					mask = impMeas.getMask();
					if (mask == null)
						makeMask(ip, r);
					mask.invert();

					// ip.setColor(Toolbar.getForegroundColor());
					ip.setColor(Toolbar.getBackgroundColor());
					// ip.setValue(Float.NaN);
					int stackSize = imp.getStackSize();
					if (stackSize > 1)
						ip.snapshot();
					ip.fill();
					ip.reset(mask);

					// *****

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

					// *****
					ip.setRoi(r); // restore original ROI
				}
			}
			imp.setSlice(oldSlice);
			showRoi(oldSlice, imp);
		}

	}

	private void fillWithBackgroundColor() {

		synchronized (resource) {// Lock out checkSlice.

			ImagePlus impMeas = chooseStack(imp);// Get the stack to measure
			ImageProcessor ip = impMeas.getProcessor();
			oldSlice = imp.getCurrentSlice();
			for (int slice = 1; slice <= numSlices; slice++) {
				impMeas.setSlice(slice);
				showRoi(slice, impMeas);
				Roi roi = impMeas.getRoi();
				if (roi != null) {

					Rectangle r = ip.getRoi();
					mask = impMeas.getMask();
					if (mask == null)
						makeMask(ip, r);

					// ip.setColor(Toolbar.getForegroundColor());
					ip.setColor(Toolbar.getBackgroundColor());
					// ip.setValue(Float.NaN);
					int stackSize = imp.getStackSize();
					if (stackSize > 1)
						ip.snapshot();
					ip.fill();
					ip.reset(mask);

					ip.setRoi(r); // restore original ROI
				}
			}
			imp.setSlice(oldSlice);
			showRoi(oldSlice, imp);
		}

	}

	private void makeMask(ImageProcessor ip, Rectangle r) {
		mask = ip.getMask();
		if (mask == null) {
			mask = new ByteProcessor(r.width, r.height);
			mask.invert();
		} else {
			// duplicate mask (needed because getMask caches masks)
			mask = mask.duplicate();
		}
		// KH 15.10.06: mask.invert();
	}

}

class MeasureStackReviewer implements Runnable {
	private final ImagePlus imp;

	public MeasureStackReviewer(ImagePlus imp) {
		this.imp = imp;
		Thread thread = new Thread(this, "Reviewer");
		thread.start();
	}
	public void run() {
		ImageWindow win = imp.getWindow();
		int numSlices = imp.getStackSize();
		if (!(win instanceof StackWindow))
			return;

		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		int oldSlice = imp.getCurrentSlice();
		int reviewTime = 50;
		for (int slice = 1; slice <= numSlices; slice++) {
			imp.setSlice(slice);
			try {
				Thread.sleep(reviewTime);
			} catch (InterruptedException ignored) {
			}
		}
		imp.setSlice(oldSlice);
	}
}
