// from Mark Besonen
// http://osdir.com/ml/java.imagej/2005-06/msg00019.html


/** START "GarbageCollect_.java" code */

import ij.plugin.PlugIn;

public class GarbageCollect_ implements PlugIn {
    public void run(String arg) {
        System.gc();
    }
}
/** END "GarbageCollect_.java" code */