package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import tsg.TSNodeLabel;
import util.PrintProgress;
import util.Utility;

public class CheckDutchCoverage {

	
	static String corporaPath = "/gardner0/data/Corpora/MWE_Datasets/";
	
	static File cornettoMwe = new File (corporaPath + "/Cornetto/Cornetto_2.0/DATA/cdb2.0.lu.form-spelling-mwe.txt");
	static int cornettoMweIndex = 0;
	
	static File eLexMwe = new File (corporaPath + "elex1.1/lexdata/elex-mw_LengthPOSmwe.txt");
	static int eLexMweIndex = 2;
	
	static File duelmeMwe = new File(corporaPath + "Duelme2/Data/DuELME_UTF8_expressions.txt");
	static int duelmeMweIndex = 0;
	
	static String fragPath = "/home/sangati/Work/FBK/TSG_MWE/Dutch/";
	static File lassySmallFrags = new File(fragPath + "LassySmall/lassytrain-nomorph.mrg.frag.filtered.lexQuotes");
	static File lassySmallFragsPrunedCarved = new File(fragPath + "LassySmall/lassytrain-nomorph.mrg.frag.filtered.lexQuotes_pruned_0.1.frag");
	static File lassySmallLemmasFrags = new File(fragPath + "LassySmallLemma/lassytrain-lemma-1content.nopunct.mrg.frag.lexQuotes");
	static File lassySmallLemmasFragsPrunedCarved = new File(fragPath + "LassySmallLemma/lassytrain-lemma-1content.nopunct.mrg.frag.lexQuotes_pruned_0.1.frag");
	
	private static void checkCoverage(File fragFile, File mweFile, int mweFileTab, boolean lemmas) throws Exception {
		HashSet<String> mweList = readMweList(mweFile, mweFileTab);
		HashMap<Integer, int[]> mweLengthTable = new HashMap<Integer, int[]>();
		PrintProgress progress = new PrintProgress("Reading fragments", 100, 0);
		Scanner scan = new Scanner(fragFile);
		int totalFrags = 0, matchedFrag = 0;
		int index = 0;
		while(scan.hasNextLine()) {
			totalFrags++;
			String line = scan.nextLine();
			progress.next();
			String[] lineSplit = line.split("\t");
			TSNodeLabel frag = new TSNodeLabel(lineSplit[0], false);
			//int freq = Integer.parseInt(lineSplit[1]);			
			//if (freq<FragFreqThreshold)
			//	continue;
			int[] l = new int[1];
			String lexFrag = getLexStringFromFrag(frag, l, lemmas);			
			if (mweList.contains(lexFrag)) {
				matchedFrag++;
				Utility.increaseInHashMap(mweLengthTable, l[0]);
			}
		}
		progress.end();
		System.out.println("Total frags: " + totalFrags);
		System.out.println("Total mwe: " + mweList.size());
		System.out.println("Matched: " + matchedFrag);	
		
		System.out.println("Matched MWE length stats: ");
		TreeSet<Integer> lengthSet = new TreeSet<Integer>(mweLengthTable.keySet());
		for(int l : lengthSet) {
			System.out.println(l + "\t" + mweLengthTable.get(l)[0]);
		}
	}
	
	public static String getLexStringFromFrag(TSNodeLabel frag, int[] l, boolean lemmas) {
		StringBuilder sb = new StringBuilder();
		if (lemmas) {
			ArrayList<TSNodeLabel> termItems = frag.collectTerminalItems();
			int countLex = 0;
			Iterator<TSNodeLabel> iter = termItems.iterator();
			while(iter.hasNext()) {
				TSNodeLabel t = iter.next();
				String w = null;
				if (t.isLexical) {
					w = t.parent.label().toLowerCase();				
				}
				else if (t.label().matches("[a-z]")) {
					w = t.label().toLowerCase();
				}
				if (w!=null && !TSG_Frags_MWE_Sign_AM.isPunctWord(w)) {
					sb.append(w);
					countLex++;
					sb.append(' ');
				}
				
			}
			l[0] = countLex;
		}
		else {			
			ArrayList<TSNodeLabel> lexItems = frag.collectLexicalItems();
			Iterator<TSNodeLabel> iter = lexItems.iterator();
			while(iter.hasNext()) {
				String w = iter.next().label().toLowerCase();
				if (!TSG_Frags_MWE_Sign_AM.isPunctWord(w)) {
					sb.append(w);
					if (iter.hasNext())
						sb.append(' ');
				}				
			}
			l[0] = lexItems.size();			
		}		
		return sb.toString().trim();
	}
	
	private static HashSet<String> readMweList(File file, int tabIndex) throws FileNotFoundException {
		HashSet<String> result = new HashSet<String>();
		Scanner scan = new Scanner(file);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			String mwe = lineSplit[tabIndex];
			result.add(mwe.toLowerCase());
		}
		return result;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		//CORNETTO
		//boolean lemmas = false;
		//checkCoverage(lassySmallFrags, cornettoMwe, cornettoMweIndex, lemmas); // 77/782
		//checkCoverage(lassySmallFragsPrunedCarved, cornettoMwe, cornettoMweIndex, lemmas); // 69/782
		
		//DUELME
		//boolean lemmas = false;
		//checkCoverage(lassySmallFrags, duelmeMwe, duelmeMweIndex, lemmas); // 711/5003
		//checkCoverage(lassySmallFragsPrunedCarved, duelmeMwe, duelmeMweIndex, lemmas); // 529/5003
		
		boolean lemmas = true;
		checkCoverage(lassySmallLemmasFrags, duelmeMwe, duelmeMweIndex, lemmas);
		//checkCoverage(lassySmallLemmasFragsPrunedCarved, duelmeMwe, duelmeMweIndex, lemmas);
		
		//ELEX
		//boolean lemmas = false;
		//checkCoverage(lassySmallFrags, eLexMwe, eLexMweIndex, lemmas); // 2367/77201
		//checkCoverage(lassySmallFragsPrunedCarved, eLexMwe, eLexMweIndex, lemmas); // 2167/77201		

	}


	

}
