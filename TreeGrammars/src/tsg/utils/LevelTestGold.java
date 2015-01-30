package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class LevelTestGold {

	public static void main(String[] args) throws Exception {
		File goldFile = new File(args[0]);
		File testFile = new File(args[1]);
		File outputFile = new File(args[2]);		
		
		Scanner scanGold = FileUtil.getScanner(goldFile);
		Scanner scanTest = FileUtil.getScanner(testFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		String lastNonMetchinTestLine = null;
		
		while(scanGold.hasNextLine()) {
			
			String goldLine = scanGold.nextLine();
			TSNodeLabel goldTree = new TSNodeLabel(goldLine);
			String[] goldWords = goldTree.toFlatWordArray();
			
						
			TSNodeLabel testTree = null;
			if (lastNonMetchinTestLine!=null) {
				String testLine = lastNonMetchinTestLine;				
				testTree = new TSNodeLabel(testLine);
			}			
			else if (scanTest.hasNextLine()) {
				String testLine = scanTest.nextLine();
				testTree = new TSNodeLabel(testLine);
			}
			
			String[] testWords = testTree == null ? new String[]{} : testTree.toFlatWordArray();
			if (Arrays.equals(goldWords, testWords)) {
				lastNonMetchinTestLine = null;				
			}
			else {
				String[] goldPos = goldTree.toPosArray();
				testTree = TSNodeLabel.defaultWSJparse(goldWords, goldPos, "TOP");
			}
			
			pw.println(testTree.toString());
		}
		pw.close();
		
	}
	
	
}
