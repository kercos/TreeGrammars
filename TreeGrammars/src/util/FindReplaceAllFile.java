package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import util.file.FileUtil;

public class FindReplaceAllFile {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		String regex = args[2];
		String replace = args[3];
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			line = line.replaceAll(regex, replace);
			pw.println(line);
		}
		pw.close();
	}

}
