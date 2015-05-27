package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import settings.Parameters;
import tsg.Label;
import tsg.ParenthesesBlockPennStd.WrongParenthesesBlockException;
import tsg.TSNodeLabel;
import util.IdentitySet;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class FilterFragmentsByText {
	
	public static String[] punctWords = new String[]{"#", "$", "''", ",", "-LCB-", "-LRB-", 
		"-RCB-", "-RRB-", ".", ":", "``", ";", "?", "!"};
	public static IdentitySet<Label> punctWordsLabel;
	
	static {
		punctWordsLabel = new IdentitySet<Label>();
		punctWordsLabel.addAll(Arrays.asList(Label.getLabelArray(punctWords)));
	}
	
	IdentitySet<Label> dictionary = new IdentitySet<Label>();
	IdentityHashMap<Label, TreeMap<Integer, TreeSet<Integer>>> wordSentPos = new IdentityHashMap<Label, TreeMap<Integer, TreeSet<Integer>>>();  
	File outFragFile, outInsideFile;
	
	//HashMap<TSNodeLabel, double[]> fragmentTableLogFreq = new HashMap<TSNodeLabel, double[]>();
	//HashMap<Label, double[]> rootTableLogFreq = new HashMap<Label, double[]>();

	public FilterFragmentsByText(File textFile, File fragmentBank, File outputFile) throws FileNotFoundException, WrongParenthesesBlockException {
		outFragFile = FileUtil.changeExtension(outputFile, "frag.gz");
		File logFile = FileUtil.changeExtension(outputFile, "frag.log");		
		Parameters.openLogFile(logFile);
		
		readTextFile(textFile);
		filterFragmentBank(fragmentBank);						
		
		Parameters.closeLogFile();
	}
	
	private void readTextFile(File textFile) throws FileNotFoundException {
		Parameters.reportLine("Building dictionary from file " + textFile);
		Scanner scan = new Scanner(textFile);
		int sntIndex = -1;
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.isEmpty())
				continue;
			sntIndex++;
			String[] words = line.trim().split("\\s+");						
			Label[] wordLabels = Label.getLabelArray(words);
			for(int p=0; p<wordLabels.length; p++) {
				Label l = wordLabels[p];
				dictionary.add(l);
				Utility.putInIdentityHashMapDoubleTreeSet(wordSentPos, l, sntIndex, p);				
			}
		}
		scan.close();
		Parameters.reportLine("Dictionary size: " + dictionary.size());
	}
	
	private void filterFragmentBank(File fragmentBank) throws FileNotFoundException, WrongParenthesesBlockException {
		Parameters.reportLine("Reading fragments in " + fragmentBank);
		Parameters.reportLine("Filtered fragments to " + outFragFile);
		Scanner scan = FileUtil.getGzipScanner(fragmentBank);
		PrintWriter  pwFrag = FileUtil.getGzipPrintWriter(outFragFile);
		int totalFrags = 0;
		int selectedFrags = 0;
		int cfgRules = 0;
		int internalFrags = 0;
		PrintProgress pp = new PrintProgress("Processing fragments", 10000, 0);
		ArrayList<Label> fragLex = null;
		TSNodeLabel frag = null;
		while(scan.hasNextLine()) {			
			String line = scan.nextLine().trim();
			if (line.isEmpty())
				continue;
			pp.next();
			totalFrags++;
			String[] fragmentFreq = line.split("\t");
			String fragmentString = fragmentFreq[0];						
			frag = TSNodeLabel.newTSNodeLabelStd(fragmentString);			
			fragLex = frag.collectLexicalLabels();
			int termCounts = frag.countTerminalNodes();
			//if (!fragLex.isEmpty() && !punctWordsLabel.containsAll(fragLex) //not empty and not only punctuations
			//		&& dictionary.containsAll(fragLex) // all words in dictionary 
			//		&& rightOrder(fragLex)) { // exists a sentence where all words are in the right order
			if (termCounts<5 && !hasPunctuationMarks(fragLex) && 
					dictionary.containsAll(fragLex) && rightOrder(fragLex)) {
				selectedFrags++;
				pwFrag.println(line); //frag.toStringStandard());
				if (frag.maxDepth()==1)
					cfgRules++;
				if (fragLex.isEmpty())
					internalFrags++;
				//double freq = Double.parseDouble(fragmentFreq[1]);
				//double logFreq = Math.log(freq);
				//fragmentTableLogFreq.put(frag, new double[]{logFreq});
				//Utility.increaseInHashMap(rootTableLogFreq, frag.label, freq);
			}
		}
		pp.end();
		scan.close();
		pwFrag.close();
		Parameters.reportLine("Total frags: " + totalFrags);
		Parameters.reportLine("Selected frags: " + selectedFrags);
		Parameters.reportLine("\tCFG rules: " + cfgRules);
		Parameters.reportLine("\tInternal frags: " + internalFrags);
		
	}

	
	private boolean hasPunctuationMarks(ArrayList<Label> fragLex) {
		for(Label l : fragLex) {
			if (punctWordsLabel.contains(l))
				return true;
		}
		return false;
	}

	private boolean rightOrder(ArrayList<Label> fragLex) {
		if (fragLex.isEmpty())
			return true;
		TreeSet<Integer> presenceSentences = null;				
		for(Label l : fragLex) {
			Set<Integer> set = wordSentPos.get(l).keySet();
			if (presenceSentences==null)
				presenceSentences = new TreeSet<Integer>(set);
			else {
				presenceSentences.retainAll(set);
				if (presenceSentences.isEmpty())
					return false;
			}
		}
		// presenceSenteces contains the indexes of the sentences having all the words in fragLex
		// not necessarily in the right order
		outer:
		for(int sntIndex : presenceSentences) {
			Integer pos = -1;
			for(Label l : fragLex) {
				TreeSet<Integer> set = wordSentPos.get(l).get(sntIndex);
				pos = set.ceiling(pos+1);
				if (pos==null)
					continue outer;
			}
			return true;
		}
		return false;
	}

	public static void main(String[] args) throws FileNotFoundException, WrongParenthesesBlockException {
		String fragsBank = "/Volumes/HardDisk/Work/TSG_MWE/NYT/NYT_500K_All_Skip150/nyt_eng_all_skip150.frag.gz";
		String textFile = "/Users/fedja/Dropbox/ted_experiment/corpus_en_it/IWSLT14.TED.tst2010.en-it.tok.lc.en";
		String outputFile = "/Users/fedja/Dropbox/ted_experiment/en_it/tsg/IWSLT14.TED.tst2010.en-it.tok.lc.en";
		new FilterFragmentsByText(new File(textFile), new File(fragsBank), new File(outputFile));
	}
	
}
