package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.Label;
import tsg.TSNodeLabel;
import util.file.FileUtil;

public class ReplaceX {

	public static void main(String[] args) throws Exception {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Label topLabel = Label.getLabel("TOP");
		Label xLabel = Label.getLabel("X");
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) continue;
			TSNodeLabel t = new TSNodeLabel(line); 
			t.replaceAllNonTerminalLabels(xLabel, topLabel);
			pw.println(t);
		}
		pw.close();
	}
	
}
