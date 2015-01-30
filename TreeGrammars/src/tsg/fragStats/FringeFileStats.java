package tsg.fragStats;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;

import util.Utility;
import util.file.FileUtil;

public class FringeFileStats {
	
	static HashMap<Integer,int[]> fringeLex = new HashMap<Integer,int[]>();
	static int lines = 0;

	private static void computeFringeStats(File inputFile) {		
		Scanner scan = FileUtil.getScanner(inputFile);		
		
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty())
				continue;
			lines++;
			String fringe = line.split("\t")[0];
			int words = Utility.countCharInString(fringe, '"') / 2;
			Utility.increaseInHashMap(fringeLex, words);
		}		
		
	}
	
	private static void printCounts() {
		TreeSet<Integer> wordsNumber = new TreeSet<Integer>(fringeLex.keySet());
		System.out.println("words\tcount");
		for(Integer n : wordsNumber) {
			System.out.println(n + "\t" + fringeLex.get(n)[0]);
		}		
		System.out.println("total\t" + lines);
	}
	
	public static void main(String[] args) {
		for(String a : args) {
			File inputFile = new File(a);
			computeFringeStats(inputFile);
		}		
		printCounts();
	}

	
}
