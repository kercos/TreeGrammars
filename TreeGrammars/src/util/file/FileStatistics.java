package util.file;

import java.io.*;
import java.util.*;

public class FileStatistics {
	
	public static void getFileStats(File inputFile) {
		Scanner scan = FileUtil.getScanner(inputFile);
		int xGT1=0, yGT1=0;
		int xE1=0, yE1=0;
		int values = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();			
			if (line.length()==0) continue;
			values++;
			float[] pair = getLinePairs(line);
			if (pair[0]>1) xGT1++;
			if (pair[1]>1) yGT1++;
			if (pair[0]==1) xE1++;
			if (pair[1]==1) yE1++;
		}
		System.out.println("Total values: " + values);
		System.out.println("X greater than 1: " + xGT1);
		System.out.println("Y greater than 1: " + yGT1);
		System.out.println("X equals to 1: " + xE1);
		System.out.println("Y equals to 1: " + yE1);
	}

	public static float[] getLinePairs(String line) {
		String[] numbers = line.split("\\s+");
		float[] result = new float[2];
		result[0] = Float.parseFloat(numbers[0]);
		result[1] = Float.parseFloat(numbers[1]);
		return result;
	}	

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		getFileStats(inputFile);
		
	}
}
