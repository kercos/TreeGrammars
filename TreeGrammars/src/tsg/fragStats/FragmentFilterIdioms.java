package tsg.fragStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class FragmentFilterIdioms {

	static boolean read2bytes = true;
	static int printProgressEvery = 100000;
	
	int minNumberWords = 8;
	int minFreq = 5;
	int minFronteerNonTerminal = 2;
	
	File fragmentFile;	
	
	public FragmentFilterIdioms(File fragmentFile, int minNumberWords, 
			int minFreq, int minFronteerNonTerminal) throws Exception {
		
		this.fragmentFile = fragmentFile;
		this.minNumberWords = minNumberWords;
		this.minFreq = minFreq;
		this.minFronteerNonTerminal = minFronteerNonTerminal;
		
		filterFragemntFile();
				
	}
	
	private void analyzeNextLine(String line) throws Exception {	
		String[] lineSplit = line.split("\t");
		int freq = Integer.parseInt(lineSplit[1]);
		if (freq>=minFreq) {
			String fragString = lineSplit[0];
			if (fragString.matches(".*[\\$,%].*")) return;
			TSNodeLabel frag = new TSNodeLabel(fragString,false);
			int words = frag.countLexicalNodes();			
			if (words>=minNumberWords) {
				int fronteerNonTerminal = frag.countTerminalNodes() - words;
				if (fronteerNonTerminal >= minFronteerNonTerminal) {
					System.out.println(freq + "\t" + frag.toStringQtree());
				}
			}
		}
	}

	private void filterFragemntFile() throws Exception {
						
		Scanner scan = FileUtil.getScanner(fragmentFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			analyzeNextLine(line);
		}		

		scan.close();		
				
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		File fragmentFile = new File(args[0]);		
		int minNumberWords = Integer.parseInt(args[1]);
		int minFreq = Integer.parseInt(args[2]);
		int minFronteerNonTerminal = Integer.parseInt(args[3]);
		
		new FragmentFilterIdioms(fragmentFile, minNumberWords, minFreq, minFronteerNonTerminal);
		//String a = "fdas%fdsa";
		//System.out.println(a.matches(".*[\\$,%].*"));
	}	

}
