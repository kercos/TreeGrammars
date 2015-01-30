package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import util.file.FileUtil;

public class FixQuotes {


	private static String fixLine(String line, int index) {
		int quoteIndex = line.indexOf("\"", index); 
		if (quoteIndex==-1)
			return line;
		int spaceIndex = -1;
		for(int i=quoteIndex-1; i>=0; i--) {
			if (line.charAt(i)==' ') {
				spaceIndex = i;
				break;
			}
		}
		String sub = line.substring(spaceIndex, quoteIndex);		
		sub = sub.replaceFirst("``", "\"");
		String result = line.substring(0,spaceIndex) + sub + line.substring(quoteIndex);
		return fixLine(result, quoteIndex+1);
	}
	
	public static void mainT(String[] args) {
		String a = "(ADJP (`` ````\") (ADJP@ (JJ ``300-a-share\")))\t15\t0";
		System.out.println(fixLine(a,0));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String fixedLine = fixLine(line,0);
			pw.println(fixedLine);
		}
		pw.close();
	}


}
