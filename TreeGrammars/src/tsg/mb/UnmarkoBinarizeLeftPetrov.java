package tsg.mb;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.Label;
import tsg.TSNodeLabel;
import util.file.FileUtil;

public class UnmarkoBinarizeLeftPetrov {

	static Label topLabel = Label.getLabel("TOP");
	
	public static void main(String[] args) {	
		boolean separateOutput = args.length > 1;
		File inputFile = new File(args[0]);
		File outputFile = separateOutput ? new File(args[1]) : new File(args[0] + ".tmp");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		TreeMarkoBinarizationLeft_Petrov MB = new TreeMarkoBinarizationLeft_Petrov();
		TreeMarkoBinarization.markH = 1;
		TreeMarkoBinarization.markV = 2;
		
		int index = 0;
		while(scan.hasNextLine()) {
			index++;
			String line = scan.nextLine();
			if (line.isEmpty()) 
				continue;			
			if (!line.startsWith("(")) {
				System.err.println("Parenthesis not fund in line " + index + ": '" 
						+ line + "'" + " --> replacing with (TOP null)" );
				pw.println("(TOP null)");
				continue;
			}
			TSNodeLabel t;			
			try {
				t = new TSNodeLabel(line);
			} catch (Exception e) {
				System.err.println("Found error in tree in line " + index + ": '" + line + "'");
				pw.println("(TOP null)");				
				System.err.println("Aborting");
				e.printStackTrace();
				return;
			}
			t = MB.undoMarkovBinarization(t);			
			pw.println(t);
		}
		pw.close();
		if (!separateOutput)
			outputFile.renameTo(inputFile);
	}
	
	
}
