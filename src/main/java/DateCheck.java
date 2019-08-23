/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



/**
 *
 * @author kkunzelm
 */


/* Ist nur eine Vorversion: Überholt durch DateAndMacAddrCheck.java (KHK: 21.08.2009)
 * Primitive Abfrage, ob das aktuelle Datum vor einem fest eincompilierten Datum liegt
 * Kann für einen sehr einfachen Kopierschutz verwendet werden
 */


import java.util.*;
import java.text.*;

/**
 *
 * @author Prof. Dr. Karl-Heinz Kunzelmann
 */
public class DateCheck {


        // public static void main(String[] args) {
        public void compareCurrentDateWithExpirationDate() {

        // dieses Datum muss angepasst werden
        String expiration_date="01-12-09";
        DateFormat formatter = new SimpleDateFormat("dd-MM-yy");
        Calendar currentDateAndTime=Calendar.getInstance();
        Calendar expirationDateAndTime=Calendar.getInstance();

        Date date;

        try {
            date = (Date)formatter.parse(expiration_date);
            expirationDateAndTime.setTime(date);
            //Calendar expirationDateAndTime=Calendar.getInstance();
        }
        catch (ParseException e){
            System.out.println("Exception :"+e);
        }

        // System.out.println("expiration date in ms: " + expirationDateAndTime.getTimeInMillis());
        // System.out.println("current date in ms: " + currentDateAndTime.getTimeInMillis());
        // System.out.println("aktuelle Zeit nach Exp Zeit: " + currentDateAndTime.after(expirationDateAndTime));

        boolean flag = currentDateAndTime.after(expirationDateAndTime);
        if (flag == true){
            System.out.println("Bitte verlängern Sie ihre Lizenz");
        }
        else {
            // hier kann der richtige Kode stehen oder den else Teil einfach weglassen
        }

        }
}


