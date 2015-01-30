package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class MakeTreebankFreqUniform {

	public static void main(String[] args) throws Exception {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		double uniformWeight = Double.parseDouble(args[2]);
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) 
				continue;
			String[] split = line.split("\t");
			pw.println(split[0] + "\t" + uniformWeight);
		}
		pw.close();
	}	
		
	
}
