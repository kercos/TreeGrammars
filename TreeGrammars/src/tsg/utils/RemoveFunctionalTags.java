package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class RemoveFunctionalTags {

	public static void main(String[] args) throws Exception {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) continue;
			TSNodeLabel t = new TSNodeLabel(line);
			t.removeSemanticTags();
			pw.println(t);
		}
		pw.close();
	}
	
}
