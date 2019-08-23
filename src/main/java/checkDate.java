/*
 * Ist nur eine Vorversion.... (KHK 21.08.2009)s
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *
 * TODO brauche ich die Main Methode?
 * TODO Pfad übergeben zum homeDir (Aufruf von aufrufender Klasse)
 * Damit müsste ich eine Variable Pfad vergeben können und damit das File KHKdata.txt
 * immer an der gleichen Stelle finden können
 *
 *
 */


/*
 * Primitive Abfrage, ob das aktuelle Datum vor einem fest eincompilierten Datum liegt
 * Kann für einen sehr einfachen Kopierschutz verwendet werden
 *
 * Bisher ist die Klasse noch nicht fertig.
 * Es klappt alles, ausser, dass der Pfad zu der Datei mit dem ExpDatum noch nicht
 * exakt angegeben werden kann.
 * Muster wie das geht: ij/Prefs.java -> homeDir
 *
 */


import ij.Prefs;
import java.util.*;
import java.text.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Prof. Dr. Karl-Heinz Kunzelmann
 */
public class checkDate {


    public static void main(String args[]){


        String path = Prefs.getHomeDir();
        System.out.println("Pfad zum ImageJ Verzeichnis: "+path);

       // Method main sets the expiration date
       // in addition it tests the rest of the routine
       // call from other programs: expirationChecks()

       String expirationDate="01-12-09"; // Default for testing
       if (args.length != 1){
            System.err.println("Generate expiration date file. Usage: java -jar checkDate <dd-mm-yy>");
        }
        else {

           expirationDate = args[0];  // overwrites the expirationDate with the first commandline argument
           try {
              writeDateToFile(expirationDate);
            } catch (IOException ex) {
                Logger.getLogger(checkDate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //*************************************************
        boolean dateStatusFlag = false;
        String result = null;
        try {
            result = readDateFromFile();
            System.out.println("eingelesen: " + result);
        } catch (IOException ex) {
            Logger.getLogger(checkDate.class.getName()).log(Level.SEVERE, null, ex);
        }

        //*************************************************


        dateStatusFlag = compareDateFromFileWithCurrentDate(result);


        if (dateStatusFlag == true){
            System.out.println("Bitte verlängern Sie ihre Lizenz");
        }
        else {
            // hier kann der richtige Kode stehen
            System.out.println("Ihre Lizenz ist gültig");
        }
        //*************************************************

    }

    public static boolean compareDateFromFileWithCurrentDate(String expirationDate) {
        // dieses Datum muss angepasst werden

        DateFormat formatter = new SimpleDateFormat("dd-MM-yy");
        Calendar currentDateAndTime=Calendar.getInstance();
        Calendar expirationDateAndTime=Calendar.getInstance();

        Date date;

        try {
            date = (Date)formatter.parse(expirationDate);
            expirationDateAndTime.setTime(date);
            //Calendar expirationDateAndTime=Calendar.getInstance();
        }
        catch (ParseException e){
            System.out.println("Exception :"+e);
        }

        // System.out.println("expiration date in ms: " + expirationDateAndTime.getTimeInMillis());
        // System.out.println("current date in ms: " + currentDateAndTime.getTimeInMillis());
        // System.out.println("aktuelle Zeit nach Exp Zeit: " + currentDateAndTime.after(expirationDateAndTime));

        boolean dateStatus = currentDateAndTime.after(expirationDateAndTime);
        return dateStatus;
        }

    public static void writeDateToFile(String expDate) throws IOException{

        String dataFile = "KHKdata.txt";
        // String desc = "01-12-09";
        String desc = expDate;
        DataOutputStream out = null;

        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));
            out.writeUTF(desc);

        } finally {
            out.close();
        }

     }

    public static String readDateFromFile() throws IOException {
        String expirationDateFromFile ="";
        String dataFile = "KHKdata.txt";
        System.out.println(expirationDateFromFile);

        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(dataFile)));

        System.out.println(expirationDateFromFile);
        expirationDateFromFile = in.readUTF();
        System.out.println(expirationDateFromFile);
        return expirationDateFromFile;
        } finally {
            in.close();
        }
    }

    public static boolean expirationCheck(){
        //************************* copy from here ************************
        boolean dateStatusFlag = false;
        String result = null;
        try {
            result = readDateFromFile();
            System.out.println("eingelesen: " + result);
        } catch (IOException ex) {
            Logger.getLogger(checkDate.class.getName()).log(Level.SEVERE, null, ex);
        }

        //*************************************************


        dateStatusFlag = compareDateFromFileWithCurrentDate(result);


        if (dateStatusFlag == true){
            System.out.println("Bitte verlängern Sie ihre Lizenz");
            return dateStatusFlag;
        }
        else {
            // hier kann der richtige Kode stehen
            System.out.println("Ihre Lizenz ist gültig");
            return dateStatusFlag;
        }
    }

}


/*









		// Stream to read file
		FileInputStream fin;
                String dateToBeReturned = "";
		try
		{
		    // Open an input stream
		    fin = new FileInputStream ("myfile.txt");

		    // Read a line of text
		    System.out.println( new DataInputStream(fin).readLine() );

		    // Close our input stream
		    fin.close();
                    return dateToBeReturned;
		}
		// Catches any error conditions
		catch (IOException e)
		{
			System.err.println ("Unable to read from file");
			// System.exit(-1);
                        dateToBeReturned = "Error ask KHK";
                        return dateToBeReturned;
		}
	}

}
*/

