package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.TreeSet;

import util.file.FileUtil;

public class CountUniqueIndexes {

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = new File(
			"/Users/fedja/Work/Code/TreeGrammars/Viewers/TDS/TDSvsCCG/TDS_origNPbrack_vs_CCG_Report_barend");
		Scanner scan = FileUtil.getScanner(inputFile);
		TreeSet<Integer> indexSet = new TreeSet<Integer>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			int colonIndex = line.indexOf(':');
			if (colonIndex!=-1 && Character.isDigit(line.charAt(0))) {
				String indexString = line.substring(0, colonIndex).trim();
				int index = Integer.parseInt(indexString);
				indexSet.add(index);
			}
		}
		System.out.println(indexSet.size());

	}

	

}
