package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import util.file.FileUtil;

public class FixIndexFiles {

	
	
	private static void fixLine(String line, PrintWriter pw) {		
		if (line.isEmpty() || line.charAt(0)=='\t') {
			pw.println(line);
			return;
		}
		int index = line.indexOf(':');
		String numbers = line.substring(0, index);
		String rest = line.substring(index);
		String[] numbersSplit = numbers.split(",\\s");
		int n1 = Integer.parseInt(numbersSplit[0])+1;
		int n2 = Integer.parseInt(numbersSplit[1])+1;
		pw.println(n1 + ", " + n2 + rest);		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = new File(
			"/Users/fedja/Work/Code/TreeGrammars/Viewers/TDS/TDSvsCCG/TDS_origNPbrack_vs_CCG.txt");
		File outputFile = new File(
			"/Users/fedja/Work/Code/TreeGrammars/Viewers/TDS/TDSvsCCG/TDS_origNPbrack_vs_CCG_indexFix.txt");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			fixLine(line,pw);
		}
		pw.close();
	}

	

}
