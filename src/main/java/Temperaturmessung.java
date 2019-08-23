/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author kkunzelm
 */

import java.util.Random;

public class Temperaturmessung {

	Random random = new Random();
	private int[] temperatur;

	public Temperaturmessung() {
		int i;
		temperatur = new int[365];

		/*
		 * for(){ }
		 *
		 * for(xx;xx;xxx){ }
		 *
		 * for(beginn;begingung;schrittweite){ Anweisung }
		 *
		 * beginn= i=1; bedingung = i<300; schrittweite = i++ = i=i+1 schrittweite:
		 * i=i+10
		 *
		 */

		for (i = 0; i < 365; i++) {
			temperatur[i] = getmesswert();
			printmesswert(i, temperatur[i]);
		}
	}

	public int getmesswert() {
		int messwert;
		// didaktisch sinnvolleres type casting!
		messwert = random.nextInt(60) - 20; // von -20 bis 39
		// Kontrolle:
		// System.out.println("Erg. von random.nextInt(60)-20: "+messwert);
		return messwert;
	}

	public void printmesswert(int tag, int mw) {
		tag++;
		System.out.println("Die Temp. am " + tag + "ten Tag ist " + mw + " Grad Celsius");
	}

}
