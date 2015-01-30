package tsg.parsingExp;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Map.Entry;

import util.StringInteger;
import util.Utility;
import util.file.FileUtil;

public class ConvertBitParGrammarInMJFormat {
	
	static boolean removeRedundantRules = true;
	
	public static void makeConversion(File grammarFile, File lexiconFile,
			File outputFile, String startSymbol) {
		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner scan = FileUtil.getScanner(grammarFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\\s");
			String lhs = lineSplit[1];
			if (lhs.equals(startSymbol)) {
				int ruleLength = lineSplit.length;
				if (removeRedundantRules && ruleLength==3 && lhs.equals(lineSplit[2])) {
					continue;
				}
				String rule = lineSplit[0] + " " + lhs + " -->";				
				for(int i=2; i<ruleLength; i++) {
					rule += " " + lineSplit[i];
				}
				pw.println(rule);
			}
		}
		
		scan = FileUtil.getScanner(grammarFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\\s");
			String lhs = lineSplit[1];
			if (!lhs.equals(startSymbol)) {
				int ruleLength = lineSplit.length;
				if (removeRedundantRules && ruleLength==3 && lhs.equals(lineSplit[2])) {
					continue;
				}
				String rule = lineSplit[0] + " " + lhs + " -->";
				for(int i=2; i<ruleLength; i++) {
					rule += " " + lineSplit[i];
				}
				pw.println(rule);
			}
		}
		
		scan = FileUtil.getScanner(lexiconFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			String word = lineSplit[0];
			for(int i=1; i<lineSplit.length; i++) {
				String posProb = lineSplit[i];
				String[] posProbSplit = posProb.split("\\s");
				String pos = posProbSplit[0];
				String prob = posProbSplit[1];
				pw.println(prob + " " + pos + " --> " + word);
			}
		}
		pw.close();
	}
	

	public static void main(String[] args) {
		File grammarFile = new File(args[0]);
		File lexiconFile = new File(args[1]);
		File outputFile = new File(args[2]);
		String startSymbol = args[3];
		removeRedundantRules = true;
		makeConversion(grammarFile, lexiconFile, outputFile, startSymbol);		
	}

	
	
}
