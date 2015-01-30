package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class ConvertFreqClassRank {

	public static void main(String[] args) throws Exception {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) 
				continue;
			String[] split = line.split("\t");
			int freq = Integer.parseInt(split[0]);
			int freqClassMembers = Integer.parseInt(split[1]);
			for(int i=0; i<freqClassMembers; i++) {
				pw.println(split[0]);
			}			
		}
		pw.close();
	}	
	
	public static void main1(String[] args) throws Exception {
		main1(new String[]{
			"/Users/fedja/Work/Papers/LREC10/Results/freqCounts.txt",
			"/Users/fedja/Work/Papers/LREC10/Results/freqCountsRank.txt"
		});
	}
		
	
}
