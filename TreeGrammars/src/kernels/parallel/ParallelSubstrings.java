package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;

import settings.Parameters;
import util.CommonSubstring;
import util.IdentityArrayList;
import util.PrintProgress;
import util.PrintProgressPercentage;
import util.file.FileUtil;

public class ParallelSubstrings {
	
	protected static String getVersion() { return "1.02"; }
	
	static boolean convertToLowerCase = true;
	static char[] ignoreStartChars = new char[]{'<','/'};
	
	static {		
		Arrays.sort(ignoreStartChars);
	}
	
	long reportEvery;
	ArrayList<String[]> sentencesSouce, sentencesTarget;
	File sourceFile, targetFile, printTableFile;
	long linesSource, totalPairs;
	
	//final table with source-target-indexes
	HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> finalTable;
	PrintProgressPercentage progress;

	public ParallelSubstrings(File sourceFile, File targetFile, File outputFile, int minMatchSize)  {		
		
		CommonSubstring.minMatchLength = minMatchSize; //min length of matchin substring
		
		this.printTableFile = outputFile;
		this.sourceFile = sourceFile;
		this.targetFile  = targetFile;
		File systemLogFile = FileUtil.changeExtension(printTableFile, "log");
		Parameters.openLogFile(systemLogFile);		
		
	}
	
	protected void init() throws FileNotFoundException {
		getSentences();
		
		totalPairs = ((long) linesSource) * (linesSource - 1) / 2;
		reportEvery = totalPairs/100;
		
		printParameters();
		
		finalTable = new HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>>();
		progress = new PrintProgressPercentage("Processing pair", 10000, 0, totalPairs);
	}

	protected void run() throws FileNotFoundException {
		
		init();
		
		long startTime = System.currentTimeMillis();		
		
		// compute the matches
		computePairwiseMatch();
		
		progress.end();
		
		// print the table to file
		printFinalTableToFile();
		
		long endTime = System.currentTimeMillis();
		Parameters.logStdOutPrintln("Took " + ((float) (endTime - startTime)) / 1000 + " s");
		Parameters.closeLogFile();
	}

	protected void printParameters() {
		Parameters.logStdOutPrintln(this.getClass().getName() + " v. " + getVersion());
		Parameters.logStdOutPrintln("");
		Parameters.logStdOutPrintln("Min length subsequence: " + CommonSubstring.minMatchLength);
		Parameters.logStdOutPrintln("Source File: " + sourceFile);
		Parameters.logStdOutPrintln("Target File: " + targetFile);		
		Parameters.logStdOutPrintln("Number of lines in each file: " + linesSource);
		Parameters.logStdOutPrintln("Number of total pairs: " + totalPairs);
		Parameters.flushLog();		
	}

	protected boolean getSentences() throws FileNotFoundException {
		sentencesSouce = getSentences(sourceFile);
		sentencesTarget = getSentences(targetFile);		
		linesSource = sentencesSouce.size();
		long linesTarget = sentencesTarget.size();
		if (linesSource!=linesTarget) {
			System.err.println("Source and target files have different number of lines");
			System.err.println(linesSource + " != " + linesTarget);
			return false;
		}
		return true;
	}

	public static ArrayList<String[]> getSentences(File inputFile) throws FileNotFoundException {
		ArrayList<String[]> result = new ArrayList<String[]>();
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(inputFile, "utf-8");
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (convertToLowerCase) 
				line = line.toLowerCase();
			String[] lineWords = getInternedWordArrayFromSentence(line);
			result.add(lineWords);
		}		
		return result;
	}
	
	public static ArrayList<String[]> getInternedSentences(File inputFile) throws FileNotFoundException {
		ArrayList<String[]> result = new ArrayList<String[]>();
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(inputFile, "utf-8");
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (convertToLowerCase) 
				line = line.toLowerCase();
			String[] lineWords = getInternedWordArrayFromSentence(line);
			result.add(lineWords);
		}		
		return result;
	}

	public static String[] getInternedWordArrayFromSentence(String line) {
		String[] lineWords = line.trim().split("\\s+");
		for(int i=0; i<lineWords.length; i++) {
			lineWords[i] = lineWords[i].intern();	
			// INTERNING ALL WORDS IN FILES
		}
		return lineWords;
	}

	protected void computePairwiseMatch()  {
				
		ListIterator<String[]> sourceIter1 = sentencesSouce.listIterator();
		ListIterator<String[]> targetIter1 = sentencesTarget.listIterator();
		
		int index1 = -1, index2=0;
		long count = 0;
		while(sourceIter1.hasNext()) {			
			index1++;
			index2 = index1 + 1;
			String[] source1 = sourceIter1.next();
			String[] target1 = targetIter1.next();			
			ListIterator<String[]> sourceIter2 = sentencesSouce.listIterator(index2);
			ListIterator<String[]> targetIter2 = sentencesTarget.listIterator(index2);
			while (sourceIter2.hasNext()) {
				progress.next();
				if (++count == reportEvery) {
					count = 0;
					progress.suspend();
					Parameters.logStdOutPrintln("Total size of table keys, subkey " + Arrays.toString(totalKeysAndPairs(finalTable)));
					if (CommonSubstring.minMatchLength==1)
						Parameters.logStdOutPrintln("Total size of table keys, subkey (of size 1) " + Arrays.toString(totalKeysLengthOneAndPairs()));
					progress.resume();
				}
				String[] source2 = sourceIter2.next();
				String[] target2 = targetIter2.next();
				computeMatch(index1, index2, source1, source2, target1, target2);
				index2++;
			}										
		}
		
		
	}

	protected void computeMatch(int index1, int index2, String[] source1, String[] source2,
			String[] target1, String[] target2) {
		
		if (source1[0].isEmpty() || source2[0].isEmpty())
			return;
		
		if (startWithStrangeChar(source1[0]) || startWithStrangeChar(source2[0])) 
			return;
		
		// GET ALL SUBSTRING MATCHES FROM SOURCE PAIR
		HashSet<IdentityArrayList<String>> resultMatchSource = 
				CommonSubstring.getAllMaxCommonSubstringsIdentity(source1, source2);
		
		// GET ALL SUBSTRING MATCHES FROM TARGET PAIR
		HashSet<IdentityArrayList<String>> resultMatchTarget = 
				CommonSubstring.getAllMaxCommonSubstringsIdentity(target1, target2);
		
		//Remove exact matches (e.b., proper nouns, etc...)
		cleanExactMatches(resultMatchSource, resultMatchTarget);
		
		// both not empty
		if (!resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty()) {
			addMatchesToFinalTable(index1, index2, resultMatchSource, resultMatchTarget);
		}
		
	}
	
	public static void testTwoSentences(String a, String b) {
		String[] source1 = getInternedWordArrayFromSentence(a);
		String[] source2 = getInternedWordArrayFromSentence(b);
		HashSet<IdentityArrayList<String>> resultMatchSource = 
				CommonSubstring.getAllMaxCommonSubstringsIdentity(source1, source2);
		if (resultMatchSource.isEmpty()) {
			System.out.println("--- No match found ---");
			return;
		}
		for(IdentityArrayList<String> m : resultMatchSource) {
			System.out.println(m);
		}
	}
	
	public static void testTwoSentencePairs(String sa, String sb, String ta, String tb) {
		System.out.println("SOURCE:");
		testTwoSentences(sa, sb);
		System.out.println("\nTARGET:");
		testTwoSentences(ta, tb);
		
	}


	protected static boolean startWithStrangeChar(String s) {
		return Arrays.binarySearch(ignoreStartChars, s.charAt(0))>=0;
	}

	/**
	 * Remove exact matches (e.b., proper nouns, etc...)
	 * @param resultMatchSource
	 * @param resultMatchTarget
	 */
	protected void cleanExactMatches(
			HashSet<IdentityArrayList<String>> resultMatchSource, 
			HashSet<IdentityArrayList<String>> resultMatchTarget) {
		
		Iterator<IdentityArrayList<String>> iter1 = resultMatchSource.iterator();
		
		while(iter1.hasNext()) {
			IdentityArrayList<String> key1 = iter1.next();						
			if (resultMatchTarget.remove(key1))
				iter1.remove();			
		}
	}

	protected void addMatchesToFinalTable(
			int index1, int index2, 
			HashSet<IdentityArrayList<String>> resultMatchSource, 
			HashSet<IdentityArrayList<String>> resultMatchTarget) {
		
		for(IdentityArrayList<String> sourceMatch : resultMatchSource) {
			int sourceSize = sourceMatch.size();
			for(IdentityArrayList<String> targetMatch : resultMatchTarget) {
				int targetSize = targetMatch.size();
				if (sourceSize!=1 || targetSize!=1)
					addToFinalTable(sourceMatch, targetMatch, index1, index2);
			}
		}

	}


	synchronized private void addToFinalTable(
			IdentityArrayList<String> sourceMatch,
			IdentityArrayList<String> targetMatch,
			int index1, int index2) {
		
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> subtable = finalTable.get(sourceMatch);
		
		if (subtable==null) {
			subtable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
			finalTable.put(sourceMatch, subtable);
			TreeSet<Integer> indexSet = new TreeSet<Integer>();
			indexSet.add(index1);
			indexSet.add(index2);			
			subtable.put(targetMatch, indexSet);
		}
		else {
			TreeSet<Integer> indexSet = subtable.get(targetMatch);
			if (indexSet==null) {
				indexSet = new TreeSet<Integer>();
				subtable.put(targetMatch, indexSet);
			}
			indexSet.add(index1);
			indexSet.add(index2);
		}
	}

	/**
	 * Print final table to file
	 * @param table
	 * @param outputFileMain
	 */
	private void printFinalTableToFile() {
		
		Parameters.logStdOutPrintln("Printing final table to " + printTableFile);
		Parameters.logStdOutPrintln("Total size of table keys, subkey " + Arrays.toString(totalKeysAndPairs(finalTable)));
		printTable(finalTable, printTableFile);
		
	}
	
	public static int[] totalKeysSubKeys(File inputFile) {
		
		PrintProgress pp = new PrintProgress("Reading table", 10000, 0);
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		//IdentityArrayList<String> key=null, value=null;
		//HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = null;
		int keys=0, values=0;
		//int[] indexes = null;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //new implementation
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] // new implementation only has these lines
				//key =  getIdentityArrayList(split[1]);
				keys++;
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			values++;
			//value = getIdentityArrayList(split[2]);
			//indexes = getIndexes(split[3]);			
		}
		pp.end();
		return new int[]{keys, values};
	}
	
	public static HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> readTableFromFile(File inputFile) {
		
		HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> result = 
				new HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> ();
		
		PrintProgress pp = new PrintProgress("Reading table", 10000, 0);
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		IdentityArrayList<String> key=null, value=null;
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = null;
		int[] indexes = null;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //new implementation
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] // new implementation only has these lines
				key =  getIdentityArrayListFromBracket(split[1]);
				subTable = result.get(key);
				if (subTable==null) {
					subTable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
					result.put(key, subTable);
				}
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			value = getIdentityArrayListFromBracket(split[2]);
			indexes = getIndexeArrayFromParenthesis(split[3]);			
			TreeSet<Integer> valueSet = new TreeSet<Integer>();					
			subTable.put(value, valueSet);
			for(int i : indexes) {
				valueSet.add(i);
			}			
		}
		pp.end();
		return result;
	}
	
	public static int[] totalKeysAndPairs(File inputFile) {
		
		int keys=0;
		int pairs=0;
		PrintProgress pp = new PrintProgress("Reading table", 10000, 0);
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //new implementation			
			if (split.length==2) {
				//        [check, for] // new implementation only has these lines
				keys++;				
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			pairs++;
		}
		pp.end();
		return new int[]{keys, pairs};
	}
	
	public static void reportKeysPairsCountPerLength(File inputFile) {		
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		String length = null;
		int countKeys = 0, countPairs = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) { //2:
				if (length!=null) {
					System.out.println(length + " " + Arrays.toString(new int[]{countKeys, countPairs}));
				}
				length = line;
				countKeys = 0;
				countPairs = 0;
				continue; 
			}
			if (split.length==2) { //	[check, for] // new implementation only has these lines				
				countKeys++;				
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			countPairs++;
		}
		if (length!=null)
			System.out.println(length + " " + Arrays.toString(new int[]{countKeys, countPairs}));	
	}
	
	public static IdentityArrayList<String> getIdentityArrayListFromBracket(String string) {
		string = string.substring(1, string.length()-1);		
		return getIdentityArrayList(string, "\\, ");
	}
	
	public static IdentityArrayList<String> getIdentityArrayList(String string, String splitExp) {
		String[] split = string.split(splitExp);
		for(int i=0; i<split.length; i++) {
			split[i] = split[i].trim().intern();
		}  
		return new IdentityArrayList<String>(split);
	}
	
	public static String[] getInternedStringArrayFromBracket(String string) {
		string = string.substring(1, string.length()-1);
		String[] split = string.split("\\, ");
		for(int i=0; i<split.length; i++) {
			split[i] = split[i].trim().intern();
		}  
		return split;
	}
	
	public static int[] getIndexeArrayFromParenthesis(String string) {
		string = string.substring(1, string.length()-1);
		String[] split = string.split("\\, ");
		int[] result = new int[split.length];
		for(int i=0; i<split.length; i++) {
			result[i] = Integer.parseInt(split[i]);
		}
		return result;
	}
	
	public static TreeSet<Integer> getIndexeSetFromParenthesis(String string) { //int addToAllIndexes
		TreeSet<Integer> result = new TreeSet<Integer>();
		string = string.substring(1, string.length()-1);
		String[] split = string.split("\\, ");
		for(String i : split) {
			result.add(Integer.parseInt(i)); //+ addToAllIndexes
		}
		return result;
	}
	
	public static int[] totalKeysAndPairs(
			HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> table) {
		int keys = table.size();
		int subKeys = 0;
		for(HashMap<IdentityArrayList<String>, TreeSet<Integer>> e : table.values()) {
			subKeys += e.size();
		}		
		return new int[]{keys, subKeys};
	}

	protected int[] totalKeysLengthOneAndPairs() {
		int keys = 0;
		int subkeys = 0;
		for(Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> e : finalTable.entrySet()) {
			IdentityArrayList<String> source = e.getKey();
			if (source.size()==1)
				keys++;
			for(IdentityArrayList<String> target : e.getValue().keySet()) {
				if (target.size()==1)
					subkeys++;
			}
		}		
		return new int[]{keys, subkeys};
	}

	public static int getTotalIndexes(
			HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> table) {
		TreeSet<Integer> allIndexes = new TreeSet<Integer>();
		for(HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable : table.values()) {
			allIndexes.addAll(getAllIndexes(subTable));
		}
		return allIndexes.size();
	}

	public static TreeSet<Integer> getAllIndexes(HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable) {
		TreeSet<Integer> result = new TreeSet<Integer>();
		for(TreeSet<Integer> set : subTable.values()) {
			result.addAll(set);
		}
		return result;
	}

	public static void printTable(
			HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> finalTable,
			File printTableFile) {
		PrintWriter pw = FileUtil.getGzipPrintWriter(printTableFile);
		int size = finalTable.size();
		int count = 0;
		for(int i=0; i<10000; i++) {	// order by the length of the source substring		
			boolean foundSize = false;
			for( Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> e : finalTable.entrySet()) {
				IdentityArrayList<String> source = e.getKey();
				int length = source.size();
				if (length==i) {
					count++;
					if (!foundSize) {
						pw.println(length + ":");
						foundSize = true;
					}
					pw.println("\t" + source);
					for( Entry<IdentityArrayList<String>, TreeSet<Integer>> f : e.getValue().entrySet()) {
						IdentityArrayList<String> target = f.getKey();
						pw.println("\t\t" + target + "\t" + f.getValue().toString());
					}
				}								
			}
			if (count==size)
				break;
		}
		pw.close();
	}

	public static void main(String args[]) throws FileNotFoundException {
		
		/*
		String dir = "/Volumes/HardDisk/Scratch/CORPORA/TED_Parallel/en-it/giza_align/";
		String sourceFile = dir + "train.tags.en-it.tok.en.head";
		String targetFile = dir + "train.tags.en-it.tok.it.head";
		String logFile = dir + "matchingSubstrings.en-it.tok.log";
		args = new String[]{sourceFile, targetFile, logFile};
		*/
		
		/*
		String usage = "ParallelSubstringsMatch v. " + getVersion() + "\n" + 
				"usage: java ParallelSubstringsMatch "
				+ "-sourceFile:file, -targetFile:file, -outputFile:file -minMatchSize:n";		
		
		if (args.length!=4) {
			System.err.println("Wrong number of arguments!");
			System.err.println(usage);
		}
		
		File sourceFile = ArgumentReader.readFileOption(args[0]);
		File targetFile = ArgumentReader.readFileOption(args[1]);
		File outputFile = ArgumentReader.readFileOption(args[2]);
		int minMatchSize = ArgumentReader.readIntOption(args[3]);
		ParallelSubstrings PS = new ParallelSubstrings(sourceFile, targetFile, outputFile, minMatchSize);
		PS.run();
		*/
		
		/*
		String sa = "now , if we consider a different ray of light , one going off like this , we now need to take into account what einstein predicted when he developed general relativity .";
		String sb = "as you saw a little bit earlier , when we were doing the phoenix one , we have to take into account the heat that we are going to be facing .";
		String ta = "se ora immaginiamo un raggio di luce che parte in questa direzione dobbiamo tenere in considerazione quello che einstein aveva predetto quando ha sviluppato la relatività generale .";
		String tb = "come avete visto poco fa , mentre facevamo la phoenix , dovevamo tenere in considerazione il calore che avremmo affrontato .";
		
		//testTwoSentences(s,t);
		testTwoSentencePairs(sa,sb,ta,tb);
		*/
		
		/*
		String a = "[vedete, qui, ,, che]";
		IdentityArrayList<String> ia = getIdentityArrayListFromBracket(a);
		for(String w : ia) {
			System.out.println("'" + w + "'");
		}
		*/
		
		//System.out.println(Arrays.toString(totalKeysAndPairs(new File(args[0]))));
		reportKeysPairsCountPerLength(new File(args[0]));
		
	}
	
}
