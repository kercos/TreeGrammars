package tsg.parsingExp;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class GetTreebankYield {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File treebankFile = new File(args[0]);
		File outputFile = new File(args[1]);
		Scanner scan = FileUtil.getScanner(treebankFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			pw.println(tree.getYield());
		}
		pw.close();
	}

}
