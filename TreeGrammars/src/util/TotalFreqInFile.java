package util;

import java.io.File;
import java.util.Scanner;

import util.file.FileUtil;

public class TotalFreqInFile {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		Scanner scan = FileUtil.getScanner(inputFile);
		double totalCount = 0;
		while(scan.hasNextLine()) {
			totalCount += Double.parseDouble(scan.nextLine().split("\t")[1]);
		}
		System.out.println("Total count: " + totalCount);

	}

}
