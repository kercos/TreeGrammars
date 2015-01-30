package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class CheckTreebank {

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		Scanner scan = FileUtil.getScanner(inputFile);
		int lineNumber = 0;
		while(scan.hasNextLine()) {
			lineNumber++;
			String line = scan.nextLine();
			try {
				TSNodeLabel tree = new TSNodeLabel(line);
			} catch (Exception e) {
				System.err.println(lineNumber + ": " + line);
			}			
		}
	}
	
	
}
