/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *
 * TODO --- implements Plugin ...
 */



import ij.Prefs;
import ij.plugin.*;

/**
 *
 * @author kkunzelm
 */
public class SendSystemPrintLine_Message implements PlugIn {


    // A plugin of type PlugIn only has a run method
    public void run(String arg) {
        String path = Prefs.getHomeDir();
        ij.IJ.showMessage("Pfad zum ImageJ Verzeichnis: "+path);
    }
}
