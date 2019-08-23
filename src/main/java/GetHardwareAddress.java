 /*
 * @ # GetHardwareAddress.java
 * A class repersenting use to GetHardwareAddress method 
 * of URL class in java.net package
 * version 23 June 2008
 * author Rose India 
 */

import java.net.*;

public class GetHardwareAddress {

    public static void main(String args[]) throws Exception {

        NetworkInterface networkInterface =
                NetworkInterface.getByName("eth0");

        byte[] b1 = networkInterface.getHardwareAddress();
        System.out.print("Hardware Address = ");
        for (int i = 0; i < b1.length; i++) {
            System.out.print(b1[i]);
        }
    }
}