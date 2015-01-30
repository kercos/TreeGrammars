package wordModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Map.Entry;

import symbols.SymbolFreq;
import symbols.SymbolString;
import tsg.corpora.Wsj;
import util.file.FileUtil;

public class AffixSuffix {

	Hashtable<String, FixValue> affixes;
	Hashtable<String, FixValue> suffixes;
	
	public static int printExtractLength = 5;
	public static int maxLength = 5;
	public static int treshold = 100;
	
	public AffixSuffix(File flatSentencesFile) {		
		affixes = new Hashtable<String, FixValue>();
		suffixes = new Hashtable<String, FixValue>();
		Scanner scan = FileUtil.getScanner(flatSentencesFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] words = line.split("\\s+");
			for(String w : words) {
				loadAffixSuffix(w.toLowerCase());
			}
		}
		scan.close();		
	}
	
	class FixValue {
		int totalTokens;		
		Hashtable<SymbolString, int[]> wordTypes;
		
		public FixValue() {	
			wordTypes = new Hashtable<SymbolString, int[]>();
		}		
		
		public void addWord(SymbolString wordType) {
			totalTokens++;
			increaseSymbolIntArray(wordType);
		}
		
		public void increaseSymbolIntArray(SymbolString key) {
			int[] freq = wordTypes.get(key);
			if (freq==null) {
				freq = new int[]{1};
				wordTypes.put(key, freq);
			}
			else freq[0]++;		
		}
		
		public TreeSet<SymbolFreq> getSortedSet() {
			TreeSet<SymbolFreq> result = new TreeSet<SymbolFreq>();
			for(Entry<SymbolString, int[]> e : wordTypes.entrySet()) {
				result.add(new SymbolFreq(e.getKey(), e.getValue()[0]));
			}
			return result;
		}
		
		public int totalTypes() {
			return wordTypes.size();
		}
		
		public int totalTokens() {
			return totalTokens;
		}
		
		public String printExtract() {
			String result = "";
			int i = 0;
			TreeSet<SymbolFreq> sortedSet = getSortedSet();
			Iterator<SymbolFreq> iter = sortedSet.descendingIterator();			
			while(iter.hasNext() && i<printExtractLength) {
				result += iter.next().toString() + ", ";
				i++;
			}
			result += "...";
			return result;
		}
	}
	
	private void loadAffixSuffix(String w) {
		int length = w.length();
		int max = Math.min(maxLength, length-1);
		SymbolString wordSymbol = new SymbolString(w);
		for(int i=1; i<=max; i++) {
			String affix = w.substring(0, i);
			String suffix = w.substring(length-i);
			increaseStringIntArray(affixes, affix, wordSymbol);
			increaseStringIntArray(suffixes, suffix, wordSymbol);
		}		
	}

	private void increaseStringIntArray(Hashtable<String, FixValue> table,
			String fix, SymbolString word) {
		FixValue v = table.get(fix);
		if (v==null) {
			v = new FixValue();
			table.put(fix, v);
		}
		v.addWord(word);
	}

	public void printListAffixSuffix() {
		System.out.println("Affixes\tLength\tTokens\tTypes\tExamples");
		printTable(affixes, treshold);
		
		System.out.println("\n\n\nSuffixes\tLength\tTokens\tTypes\tExamples");
		printTable(suffixes, treshold);
	}
	
	private static void printTable(Hashtable<String, FixValue> table, int treshold) {
		for(Entry<String,FixValue> e : table.entrySet()) {
			FixValue v = e.getValue();
			int totalTokens = v.totalTokens();
			int totalTypes = v.totalTypes();
			if (totalTokens>treshold) {		
				String key = e.getKey();
				System.out.println(key + "\t" + key.length() + "\t" + 
						totalTokens + "\t" + totalTypes + "\t" + v.printExtract());
			}
		}
	}
	
	public void searchSuffix(String string) {
		searchFix(this.suffixes, string);		
	}

	public void searchAffix(String string) {
		searchFix(this.affixes, string);		
	}
	
	private void searchFix(Hashtable<String, FixValue> table, String string) {
		FixValue v = table.get(string);
		if (v==null) {
			System.out.println(string + ": no found");
			return;
		}
		int totalTokens = v.totalTokens();
		int totalTypes = v.totalTypes();
		System.out.println(string + "\t" + string.length() + "\t" + 
				totalTokens + "\t" + totalTypes + "\t" + v.printExtract());
	}
	
	
	public static void main(String[] args) throws IOException {
		printExtractLength = Integer.MAX_VALUE;
		maxLength = 5;
		treshold = 100;
		File flatFileTraining = new File(Wsj.WsjFlatNoTraces + "wsj-02-21.mrg");
		AffixSuffix as = new AffixSuffix(flatFileTraining);
		//as.printListAffixSuffix();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    String str = "";
	    while (str != null) {
	        System.out.print("<prompt> ");
	        str = in.readLine();
	        as.searchAffix(str);
	    }
		
	}

	
}
