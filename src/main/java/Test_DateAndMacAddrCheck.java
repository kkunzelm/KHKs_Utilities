/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author kkunzelm
 */

import ij.plugin.PlugIn;

public class Test_DateAndMacAddrCheck implements PlugIn {
	public void run(String arg) {
		if (DateAndMacAddrCheck.mainOfKHKsCopyProctection()) {
			System.out.println("Heureka ...");
		} else {
			System.out.println("I am so sorry...");
		}
	}
}
