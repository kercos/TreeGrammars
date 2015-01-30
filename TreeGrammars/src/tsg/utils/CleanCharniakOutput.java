package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import util.file.FileUtil;

public class CleanCharniakOutput {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith("(")) {
				line = line.replaceAll("S1", "TOP");
				pw.println(line);	
			}
			else if (line.equals("")) pw.println();			
		}
		pw.close();
		scan.close();
	}
	

}
