package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import util.file.FileUtil;

public class MakeCharBased {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			tree.makeTreeCharBased();
			pw.println(tree);
		}
		pw.close();
	}

}
