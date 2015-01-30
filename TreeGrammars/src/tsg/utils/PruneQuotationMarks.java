package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class PruneQuotationMarks {

	public static String[] quotePos = new String[]{"``","''"};
	
	static {
		Arrays.sort(quotePos);
	}
	
	public static void main(String[] args) throws Exception {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			t.prunePos(quotePos);
			pw.println(t.toString());
		}
		pw.close();
	}
}
