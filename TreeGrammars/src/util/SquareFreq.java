package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import util.file.FileUtil;

public class SquareFreq {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = new File("/disk/scratch/fsangati/PLTSG/" +
				"MB_ROARK_Right_H0_V1_UkM4_UkT4_notop/fragments_approxFreq.txt");
		File outputFile = new File("/disk/scratch/fsangati/PLTSG/" +
				"MB_ROARK_Right_H0_V1_UkM4_UkT4_notop_squared/fragments_approxFreq.txt");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			pw.print(lineSplit[0]);
			for(int i=1; i<lineSplit.length; i++) {
				int freq = Integer.parseInt(lineSplit[i]);
				freq = (int)Math.pow(freq, 2);
				pw.print("\t" + freq);
			}
			pw.println();
		}
		pw.close();

	}

}
