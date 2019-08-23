/*
 * Stand: 21.8.09
 * @author Prof. Dr. Karl-Heinz Kunzelmann
 */


/*
 * Diese Klasse kann von meinen Plugins aufgerufen werden, um als einfacher Kopierschutz zu fungieren.
 * Das Prinzip ist wie folgt:
 * Es wird geprüft, ob ein File "KHKdata.txt" existiert.
 * Dieses enthält ein "Verfallsdatum" und die MacAdresse des Computers
 * Falls das File nicht existiert, wird es angelegt (mit 3 Monate Laufzeit = default als Wert)
 * Falls das File existiert, wird geprüft, ob das Verfallsdatum überschritten ist
 * und ob es sich um den passenden PC handelt (anhand der MacAdresse von eth0).
 *
 * Das Programm kann 
 * 1. im Terminal ausgeführt werden. Damit kann man dann ein neues "Wunsch"-KHKdata.txt-File anlegen.
 * 2. Das Programm kann auch von IJ Plugins aufgerufen werden
 *
 *
        // Durch die Möglichkeit auf der Commandozeile ein eigenes Datum zu übergeben (1. Parameter)
       



        // nebenbei: ich frage das Datum in dem ExpirationData File 2x ab. Einmal ist es als ASCII Text enthalten. Einmal als long in Millisekunden.
        // gewisser Manipulationsschutz

 *
 * TODO: im Moment geht das File nur bei PCs mit mind. eth0, Abfangen, falls es mal keine eth0 geben solltes
 *
 */


import java.util.*;
import java.text.*;
import java.io.*;

import ij.Prefs;
import java.net.*;


public class DateAndMacAddrCheck {

    static boolean debug = false;
    static boolean expirDataFileExists = false;
    static boolean valideExpirData  = false;        // Data = Datum UND Mac
    static boolean valideExpirDate  = false;        // Date = nur Datum
    static boolean valideMacAddress  = false;
    static boolean allowUseOfPlugin = false;
    static boolean manuallySetExpDateFlag = false;
    static boolean manuallySetMacAddressFlag = false;

    static String expirDateFromExpirDataFile = null;
    static String macAddressFromExpirDataFile = null;
    static String manuallySetExpirationDate = null;
    static String[] manuallySetMacAddress = new String[6];

    static long dateInDays;

    public static void main(String args[]){

        if (args.length == 1) {

            if (args[0].equals("-help")||args[0].equals("--help")||args[0].equals("/?")){

                 // im Verzeichnis dist von KHKs_Utilities aufrufen:
                 System.out.println("\nTo generate expiration date file, use:\n" +
                         "\njava -cp KHKs_Utilities.jar DateAndMacAddrCheck <dd-mm-yy> <MacAddresEth0-1.Byte> <... bis zum 6.Byte>");
                 System.out.println("\nResulting KHKdata.txt file is located in user-home-directory\n");
                 System.exit(1);
            }
            else {
                   manuallySetExpirationDate = args[0];  // overwrites the expirationDate with the first commandline argument
                   manuallySetExpDateFlag = true;
            }
        }
        else if (args.length > 1) {

           manuallySetExpirationDate = args[0];  // overwrites the expirationDate with the first commandline argument
           manuallySetExpDateFlag = true;


           // man könnte auch einen String als 2. Argument verwenden und dann in die Bytes zerlegen.
           // wäre eleganter, aber im Moment nicht nötig
           manuallySetMacAddress[0] = args[1];
           manuallySetMacAddress[1] = args[2];
           manuallySetMacAddress[2] = args[3];
           manuallySetMacAddress[3] = args[4];
           manuallySetMacAddress[4] = args[5];
           manuallySetMacAddress[5] = args[6];
           manuallySetMacAddressFlag = true;
        }

            // es existiert ein KHKdata.txt File, es soll aber verlängert werden
            if ((manuallySetExpDateFlag == true)||(manuallySetMacAddressFlag == true)){
            try {
                    System.out.println("Will generate a new ExpirationDataFile!");

                    generateNewExpirDataFile();

                    System.out.println("Existing expirDataFile sucessfully updated.");
                    System.exit(1);
                }
                catch (IOException e2 ) {
                    System.out.println("Could not update existing expirDataFile because of IOExecptionerror: "+e2);
                }
        }

        // Hier wird die Hauptroutine des Programms aufgerufen.
        // gleicher Aufruf gilt auch für die Plugins
        
        if (mainOfKHKsCopyProctection()){
            System.out.println("Heureka ... it works");
        }
        else {
            System.out.println("I am so sorry... I cannot allow you to use this plugin.");
        }
    }

    public static boolean mainOfKHKsCopyProctection (){

        /* Aufruf aus Plugin: siehe Test_DateAndMacAddrCheck.java
         *
         * if (DateAndMacAddrCheck.mainOfKHKsCopyProctection()){
         *   System.out.println("Heureka ...");
         *   // Code for plugin execution
         * }
         * else {
         *  
         *   System.out.println("I am so sorry...");
         * }
         *
         */

        // ich muss im Prinzip zwei Situationen unterscheiden:
           // das ExpirDataFile existiert, ist aber abgelaufen
           // das ExpirDataFile existiert nicht, muss also angelegt werden
        
        // Erstmal Prüfen: ist ExpirDataFile da?

        try {
            readExpirDataFile();
        }
        catch (IOException e ) {

            System.out.println("readExpirDataFile caused an IOExecptionerror: "+e);
  }

        // es existiert noch kein KHKdata.txt File
        // jetzt eines Anlegen
        if (expirDataFileExists == false){
            try {
                    generateNewExpirDataFile();
                    System.out.println("Have to generate a new ExpirationDataFile!");
                    // wenn das File neu angelegt wird, darf man auf jeden Fall schon mal weiterarbeiten
                    // beim nächsten Mal existiert dann das File schon.
                    // Problem: wenn man das File immer wieder löschen würde,
                    // könnte man so den Kopierschutz aushebeln
                    // TODO über dieses Problem muss ich noch mal nachdenken.
                    allowUseOfPlugin = true;
                }
                catch (IOException e2 ) {
                    System.out.println("Could not generate expirDataFile because of IOExecptionerror: "+e2);
                    allowUseOfPlugin = false;
                }
        }
        
 

        // ja: ExpirDataFile ist da...
        // Jetzt Prüfen: Datum/Mac vergleichen
            // ja: Flag valideExpirDate = true
            // ja: Flag valideMacAddress = true
            // nein: Flag valideExpirDate = false
            // nein: Flag valideMacAddress = false

        if (expirDataFileExists == true){
            compareCurrentDateWithExpirationDate();
            compareCurrentMacWithMacFromExpirDataFile();

            // Prüfen: Flag valideExpirData = true
                // ja: Plugin ausführen
                // nein: Plugin verweigern - Fehlermeldungs
            if (expirCheck()) {              // ist die Kurzschreibweise für expirCheck == true
                allowUseOfPlugin = true;
            }
            else{                            // ist eigentlich überflüssig, da Vorgabe
                allowUseOfPlugin = false;
            }
        }
        return allowUseOfPlugin;
    }


       private static boolean readExpirDataFile() throws IOException{
 
            // siehe hier: http://www.java2s.com/Code/Java/File-Input-Output/newDataInputStreamnewBufferedInputStreamnewFileInputStream.htm
            String expirDataPathAndFile = null;
            if (Prefs.getHomeDir()==null){

                // Einschub:
                // Wenn ich das File von der Console öffne, ohne classpath zu ImageJ, dann geht Prefs.getHomeDir nicht
                // Als Workaround wähle ich ein anderes Verzeichnis
                // Mit System.getProperty("String") kann man verschiedene Werte von Systemvariablen auslesen
                // gültige String Werte sind z. B.
                // "java.io.tmpdir" ist mit file.separator am Ende
                // "user.home" ist ohne file.separator am Ende
                // "file.separator" wird auch benötigt
                // Anwendungsbeispiel: System.out.println(System.getProperty("user.name"));

                expirDataPathAndFile=System.getProperty("user.home")+System.getProperty("file.separator")+"KHKdata.txt";
            }
            else {
                expirDataPathAndFile = Prefs.getHomeDir()+Prefs.getFileSeparator()+"KHKdata.txt";   // richtigen Path separator verwenden
            }

            if (debug) System.out.println("DataFile inkl. Pfad: " + expirDataPathAndFile);

            DataInputStream in = null;
            try {

                in = new DataInputStream(new BufferedInputStream(new FileInputStream(expirDataPathAndFile)));

                expirDateFromExpirDataFile = in.readUTF();
                dateInDays = in.readLong();

                // MacAdresse hat 6 Byte - hier wird gleich ein String angelegt

                macAddressFromExpirDataFile = Byte.toString(in.readByte());
                macAddressFromExpirDataFile += Byte.toString(in.readByte());
                macAddressFromExpirDataFile += Byte.toString(in.readByte());
                macAddressFromExpirDataFile += Byte.toString(in.readByte());
                macAddressFromExpirDataFile += Byte.toString(in.readByte());
                macAddressFromExpirDataFile += Byte.toString(in.readByte());

                in.close();

                //if (debug) System.out.println("readExpirDataFile: MacAddress: "+macAddressFromExpirDataFile);
                if (debug) {
                    System.out.println("readExpirDataFile: ExpirDate: " + expirDateFromExpirDataFile + " MacAdr.: " + macAddressFromExpirDataFile);
                }

                expirDataFileExists = true;
                
            }
            catch (IOException ex) {
                System.out.println("Could not open data input stream for expirDateFile: " + ex);
                expirDataFileExists = false;
            }
            
            return expirDataFileExists;
         }

       // falls das Daten-File nicht existiert, wird es angelegt
       private static void generateNewExpirDataFile() throws IOException{

           // siehe: http://www.java2s.com/Code/Java/File-Input-Output/newDataOutputStreamnewBufferedOutputStreamnewFileOutputStream.htm
           String expirDateString = null;
           String expirDataPathAndFile = Prefs.getHomeDir()+Prefs.getFileSeparator()+"KHKdata.txt";
           if (Prefs.getHomeDir()==null){
               expirDataPathAndFile=System.getProperty("user.home")+System.getProperty("file.separator")+"KHKdata.txt";
           }
          
           // diesen Teil nochmal überarbeiten und ein eine eigene Methode verschieben
           // Mache aus einem Kalenderdatum einen String
           
           DateFormat formatter = new SimpleDateFormat("dd-MM-yy");
           Calendar currentDateAndTime=Calendar.getInstance();
           currentDateAndTime.add( Calendar.MONTH, 4 );   // hier werden zum aktuellen Datum 4 Mon hinzuaddiert
           if (manuallySetExpDateFlag){
               expirDateString= manuallySetExpirationDate;
           }
           else {
                expirDateString= formatter.format(currentDateAndTime.getTime());
           }
           if (debug) System.out.println("Datum..Formatter: "+ formatter.format(currentDateAndTime.getTime()));

 
           // get MacAdress of eth0
           // TODO hierfür habe ich eine eine Methode geschrieben. Hier einbauen!

            NetworkInterface networkInterface =
                NetworkInterface.getByName("eth0");

           byte[] b1 = new byte[6];

           // Abfrage, ob MacAdress manuell eingegeben wurde.


           if (manuallySetMacAddressFlag){
               //b1 mit "manuallySetMacAddress" füllen;


              for (int n=0; n<6; n++){
               b1[n] = Byte.parseByte(manuallySetMacAddress[n]) ;
              }
           }
           else {
               b1 = networkInterface.getHardwareAddress();
          }

           if (debug) {
               System.out.print("generateNewExpirDataFile: Hardware Address = ");
                for (int i = 0; i < b1.length; i++) {
                    System.out.print(b1[i]+" ");
                }
                System.out.println(" ");

                 /*
                 * Extract a  array of mac address and convert it to hexa with the
                 * following format 08-00-27-DC-4A-9E.
                 */
                for (int i = 0; i < b1.length; i++) {
                    System.out.format("%02X%s", b1[i], (i < b1.length - 1) ? "-" : "");

                    // System.out.format("%02X",b1[i]); // wäre ohne Bindestrich
                    // %02 = 2 Stellen, X = Hexadezimal mit Grossbuchstaben, %s = String - hier mit Bedingung entweder - oder ""
                }
           }
            DataOutputStream out = null;
            try {
                out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(expirDataPathAndFile)));
                
                // Expiration date as UTF info
                out.writeUTF(expirDateString);
                if (debug) System.out.println ("generateNewExpirDataFile: writeUTF: "+expirDateString);

                // Expiration date as long in milliseconds
                // wird dann in days umgerechnet
                // z. B. long days = (System.currentTimeMillis() – ms) / (1000*60*60*24);
                // dürfte aber eigentlich egal sein, ob days oder millisec

                if (manuallySetExpDateFlag){

                    // TODO könnte man in eine eigene Methode auslagern, gleiches Muster 3x verwendet

                    Calendar manuallySetExpirationDateAndTime=Calendar.getInstance();
                    Date date;

                    try {
                        date = (Date)formatter.parse(manuallySetExpirationDate);
                        manuallySetExpirationDateAndTime.setTime(date);

                        //Calendar expirationDateAndTime=Calendar.getInstance();
                    }
                    catch (ParseException e){
                        System.out.println("Exception :"+e);
                    }

                    dateInDays = manuallySetExpirationDateAndTime.getTimeInMillis()/ (1000*60*60*24);
                }
                else { //
                    // hatte ich ursprünglich
                    // aber musste ich wegen Rundungsproblemen ersetzen
                    // dateInDays = currentDateAndTime.getTimeInMillis() / (1000*60*60*24);

                    // TODO könnte man in eine eigene Methode auslagern, gleiches Muster 3x verwendet
                    // Mache aus String mit Datum ein Kalenderdatum

                    Calendar automaticallySetExpirationDateAndTime=Calendar.getInstance();
                    Date date;

                    try {
                        date = (Date)formatter.parse(expirDateString);
                        automaticallySetExpirationDateAndTime.setTime(date);

                        //Calendar expirationDateAndTime=Calendar.getInstance();
                    }
                    catch (ParseException e){
                        System.out.println("Exception :"+e);
                    }

                    dateInDays = automaticallySetExpirationDateAndTime.getTimeInMillis()/ (1000*60*60*24);
                   
                }

                out.writeLong(dateInDays);
                if (debug) System.out.println ("generateNewExpirDataFile: date [days] als long: "+ Long.toString(dateInDays));


                // macAddress as String

                for (int i = 0; i < b1.length; i++) {
                    if (debug) System.out.println("i"+i+" <"+ b1[i]+">");
                    out.writeByte(b1[i]);
                }
            } finally {
                out.close();
            }

     }


        private static boolean compareCurrentDateWithExpirationDate() {

        // Mache aus String mit Datum ein Kalenderdatum 

        DateFormat formatter = new SimpleDateFormat("dd-MM-yy");

        Calendar currentDateAndTime=Calendar.getInstance();
        Calendar expirationDateAndTime=Calendar.getInstance();

        Date date;

        try {
            date = (Date)formatter.parse(expirDateFromExpirDataFile);
            expirationDateAndTime.setTime(date);

            //Calendar expirationDateAndTime=Calendar.getInstance();
        }
        catch (ParseException e){
            System.out.println("Exception :"+e);
        }

        long temp = expirationDateAndTime.getTimeInMillis()/ (1000*60*60*24);
        if (debug) System.out.println("temp [days]: "+temp+"\n"+"dateInDays: "+dateInDays);
        if (dateInDays != temp) {
            System.out.println("Expiration date has been manipulated. Licence invalid.");
            ij.IJ.showMessage("Expiration date has been manipulated. Licence invalid.");
            valideExpirDate=false;
            return valideExpirDate;
            //System.exit(1);   // ist ziemlich hart, geht aber auch
        }
        // System.out.println("expiration date in ms: " + expirationDateAndTime.getTimeInMillis());
        // System.out.println("current date in ms: " + currentDateAndTime.getTimeInMillis());
        // System.out.println("aktuelle Zeit nach Exp Zeit: " + currentDateAndTime.after(expirationDateAndTime));

        valideExpirDate = currentDateAndTime.before(expirationDateAndTime);
        if(debug) System.out.println("Wert von validExpireDate (nach Compare): "+valideExpirDate);

        return valideExpirDate;
        }

        private static boolean compareCurrentMacWithMacFromExpirDataFile (){
            if (debug) System.out.println("Compare: macAddressFromExpirDataFile: " + macAddressFromExpirDataFile);
            if (debug) System.out.println("Compare: Ergebnis von getMacAdressEth0: " + getMacAdressEth0());

            //Compare Strings: if (string1.equals(string2))
            //Compare Strings: if (string1.equalsIgnoreCase(string2))


            if (macAddressFromExpirDataFile.equals(getMacAdressEth0())){
                valideMacAddress = true;
            }

            if (debug) System.out.println("Compare: valideMacAddress: "+valideMacAddress);
            return valideMacAddress ;
        }

        // Aufruf und Abfrage der Lizenz
        private static boolean expirCheck () {



            if ((valideExpirDate)&& (valideMacAddress)) valideExpirData = true;

            // ... hier differenzierte Antwort, warum es nicht weitergehen soll
            // z. B. Datum
            // z. B. illegale Kopie

            if (!valideExpirData){
                
                if (valideMacAddress) System.out.println("Licence expired. Contact the author of this plugin to update the licence.");
                if (valideExpirDate) System.out.println("Unauthorized Copy. Contact the author of this plugin for an individual licence.");

                System.out.println("Send an email with the name of the plugin and this information to the author: "+getFormatedMacAdressEth0());
               
                if (valideMacAddress)ij.IJ.showMessage("Licence expired. Contact the author of this plugin to update the licence.\n" +
                                                       "Send an email with the name of the plugin and this information to the author:\n" +
                                                       "\""+getFormatedMacAdressEth0()+"\"");
                if (valideExpirDate)ij.IJ.showMessage("Unauthorized Copy. Contact the author of this plugin for an individual licence.\n" +
                                                       "Send an email with the name of the plugin and this information to the author:\n" +
                                                       "\""+getFormatedMacAdressEth0()+"\"");
           }
            else {
                // hier kann der richtige Kode stehen oder den else Teil einfach weglassen
                if (debug) System.out.println("ExpirCheck: alles ok!");
                // ij.IJ.showMessage("ExpirCheck: alles ok!");

            }

            return valideExpirData;
        }

        private static String getMacAdressEth0(){
            // to be completed --- a reminder only now
            // überlegen: ich könnte auch den IntegerByteArray b1[] als Rückgabe nutzen
            String macAddressFromEth0 = "";
            byte[] b1 = null;

            try {
            NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
            b1 = networkInterface.getHardwareAddress();
                /*
                if (debug) System.out.print("Hardware Address = ");
                for (int i = 0; i < b1.length; i++) {
                    System.out.print(b1[i]);
                }
                if (debug) System.out.println(" ");
                */
            }
            catch (IOException exNet){
                System.out.println ("Exception - Ethernetcard not found: "+exNet);

            }
             // MacAdresse hat 6 Byte
            // hierfür könnte ich eine eigenen Klasse verwenden (wird ja 2x verwendet)

            macAddressFromEth0 = Byte.toString(b1[0]);
            macAddressFromEth0 += Byte.toString(b1[1]);
            macAddressFromEth0 += Byte.toString(b1[2]);
            macAddressFromEth0 += Byte.toString(b1[3]);
            macAddressFromEth0 += Byte.toString(b1[4]);
            macAddressFromEth0 += Byte.toString(b1[5]);


            if (debug) System.out.println("getMacAdressEth0: MacAddress: "+ macAddressFromEth0);

            return macAddressFromEth0;
        }


        private static String getFormatedMacAdressEth0(){
            // to be completed --- a reminder only now
            // überlegen: ich könnte auch den IntegerByteArray b1[] als Rückgabe nutzen
            String macAddressFromEth0 = "";
            byte[] b1 = null;

            try {
            NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
            b1 = networkInterface.getHardwareAddress();
                /*
                if (debug) System.out.print("Hardware Address = ");
                for (int i = 0; i < b1.length; i++) {
                    System.out.print(b1[i]);
                }
                if (debug) System.out.println(" ");
                */
            }
            catch (IOException exNet){
                System.out.println ("Exception - Ethernetcard not found: "+exNet);

            }
             // MacAdresse hat 6 Byte
            // hierfür könnte ich eine eigenen Klasse verwenden (wird ja 2x verwendet)



            macAddressFromEth0 = Byte.toString(b1[0])+" ";
            macAddressFromEth0 += Byte.toString(b1[1])+" ";
            macAddressFromEth0 += Byte.toString(b1[2])+" ";
            macAddressFromEth0 += Byte.toString(b1[3])+" ";
            macAddressFromEth0 += Byte.toString(b1[4])+" ";
            macAddressFromEth0 += Byte.toString(b1[5])+" ";


            if (debug) System.out.println("getMacAdressEth0: MacAddress: "+ macAddressFromEth0);

            return macAddressFromEth0;
        }
}


/*
 *
 * Question: How do I compare two strings?

    Answer

    A common error that we all make from time to time is incorrect String comparison. Even once you learn how to compare strings correctly, it's extremely easy to make a mistake and use the == operator.
    When we compare primitive data types, such as two ints, two chars, two doubles, etc. we can use the == operator. We can also use the == operator to compare two objects. However, when used with an object, the == operator will only check to see if they are the same objects, not if they hold the same contents.
    This means that code like the following will not correctly compare to strings :

    if ( string1 == string2 )
        {
                System.out.println ("Match found");
        }

    This code will only evaluate to true if string1 and string2 are the same object, not if they hold the same contents. This is an important distinction to make. Checking, for example, to see if
    aString == "somevalue", will not evaluate to true even if aString holds the same contents.

    To correctly compare two strings, we must use the .equals method(). This method is inherited from java.lang.Object, and can be used to compare any two strings. Here's an example of how to correctly check a String's contents :

    if ( string1.equals("abcdef") )
        {
                System.out.println ("Match found");
        }

 * This is a simple, and easy to remember tip that will safe you considerable time debugging applications.
 * Remember - never use the == operator if you only want to compare the string's contents.
 *
 */