package tsg.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

import settings.Parameters;
import util.file.FileUtil;

public class CleanStanfordOuput {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		File inputFile = new File(Parameters.resultsPath + "TSG/PCFG_Stanford_1000best/wsj-22_PCFG_stanford_best1000.mrg");
		File outputFile = new File(Parameters.resultsPath + "TSG/PCFG_Stanford_1000best/wsj-22_PCFG_stanford_best1000_clean.mrg");
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		String currentTree = "";
		String line = null;
		String previousLine = null;
		boolean printNext = false;
		int treeNumber = 0;
		while(scan.hasNextLine()) {			
			previousLine = line;
			line = scan.nextLine();
			if (line.equals("")) {
				treeNumber=0;
				continue;
			}			
			if (line.startsWith("PCFG")) {
				if (printNext) {
					pw.println(currentTree.trim());					
					treeNumber ++;
					printNext = false;
				}
				continue;
			}
			if (line.equals("(ROOT")) {		
				if (printNext) {
					pw.println(currentTree.trim() + "\n");
					treeNumber ++;
				}
				if (previousLine!=null && previousLine.startsWith("PCFG")) {
					printNext = true;					
				}				
				else printNext = false;
				line = "(TOP";
				currentTree = "";				
			}
			currentTree += " " + line.trim();			
		}
		pw.println(currentTree.trim());
		pw.close();
	}

}
