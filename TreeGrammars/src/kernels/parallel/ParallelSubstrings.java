package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import kernels.algo.AllOrderedStringExactSubsequences;
import kernels.algo.AllStringSubsequencesSet;
import kernels.algo.CommonSubstring;
import kernels.algo.StringsAlignment;
import settings.Parameters;
import util.ArgumentReader;
import util.IdentityArrayList;
import util.PrintProgress;
import util.PrintProgressPercentage;
import util.Utility;
import util.file.FileUtil;

public class ParallelSubstrings {
	
	protected static String getVersion() { return "1.05"; }
	
	static boolean onlyContiguous = true;
	static boolean convertToLowerCase = true;
	static char[] ignorePunctChar = new char[] { '.', ',', ':', ';', '?', '!', '"' };
	static String[] ignorePunctString = new String[] { "--", "..." };
	static int functionalWordThreshold = 100;
	
	static {		
		//Arrays.sort(ignoreStartChars);
		Arrays.sort(ignorePunctChar);
		for(int i=0; i<ignorePunctString.length; i++) {
			ignorePunctString[i] = ignorePunctString[i].intern();
		}
		Arrays.sort(ignorePunctString);
	}
	
	
	long reportEvery;
	ArrayList<String[]> sentencesSource, sentencesTarget;
	File sourceFile, targetFile, printTableFile;
	long linesSource, totalPairs;
	int minMatchSize;
	
	HashSet<String> functionalWordsSetSource = new HashSet<String>(); 
	HashSet<String> functionalWordsSetTarget = new HashSet<String>();
	
	
	//final table with source-target-indexes
	HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> finalTable;
	PrintProgressPercentage progress;

	public ParallelSubstrings(File sourceFile, File targetFile, File outputFile, int minMatchSize)  {		
		
		CommonSubstring.minMatchLength = minMatchSize; //min length of matchin substring

		this.minMatchSize = minMatchSize;
		this.printTableFile = outputFile;
		this.sourceFile = sourceFile;
		this.targetFile  = targetFile;
		File systemLogFile = FileUtil.changeExtension(printTableFile, "log");
		Parameters.openLogFile(systemLogFile);		
		
	}
	
	protected void init() throws FileNotFoundException {
		getSentences();
		getWordsFreq();
		
		linesSource = sentencesSource.size();
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
		Parameters.logStdOutPrintln("Number of sentences: " + linesSource);
		Parameters.logStdOutPrintln("Number of total pairs: " + totalPairs);
		Parameters.logStdOutPrintln("Only Contiguous: " + onlyContiguous);
		Parameters.flushLog();		
	}

	protected boolean getSentences() throws FileNotFoundException {
		Parameters.logStdOutPrintln("Reading Source Sentence... ");
		sentencesSource = getInternedSentences(sourceFile);
		Parameters.logStdOutPrintln("Reading Target Sentence... ");
		sentencesTarget = getInternedSentences(targetFile);		
		linesSource = sentencesSource.size();
		long linesTarget = sentencesTarget.size();
		if (linesSource!=linesTarget) {
			System.err.println("Source and target files have different number of lines");
			System.err.println(linesSource + " != " + linesTarget);
			return false;
		}
		Parameters.logStdOutPrintln("Total lines: " + linesSource);
		//removeBadSentences();
		//printCleanedSentencesToFile();
		removePunctuations();
		return true;
	}

	/*
	private void printCleanedSentencesToFile() throws FileNotFoundException {
		String dir = printTableFile.getParent() + "/";		
		File outputSource = new File(dir + "train_source.txt");
		File outputTarget = new File(dir + "train_target.txt");
		Parameters.logStdOutPrintln("Printing clean sentences source: " + outputSource);
		Parameters.logStdOutPrintln("Printing clean sentences target: " + outputTarget);
		PrintWriter pwSource = new PrintWriter(outputSource);
		PrintWriter pwTarget = new PrintWriter(outputTarget);
		Iterator<String[]> iterSource = sentencesSource.iterator();
		Iterator<String[]> iterTarget = sentencesTarget.iterator();
		while(iterSource.hasNext()) {
			pwSource.println(Utility.joinStringArrayToString(iterSource.next(), " "));
			pwTarget.println(Utility.joinStringArrayToString(iterTarget.next(), " "));
		}
		pwSource.close();
		pwTarget.close();		
	}
	*/

	/*
	private void removeBadSentences() {
		int ignored = 0;
		Iterator<String[]> iterSouce = sentencesSource.iterator();
		Iterator<String[]> iterTarget = sentencesTarget.iterator();
		while(iterSouce.hasNext()) {
			String[] source = iterSouce.next();
			String[] target = iterTarget.next();
			if (ignoreSentence(source[0]) || ignoreSentence(target[0])) {
				iterSouce.remove();
				iterTarget.remove();
			}
			ignored++;
		}		
		Parameters.logStdOutPrintln("Ignored Sentences: " + ignored);
		Parameters.logStdOutPrintln("Remaining Sentences: " + sentencesSource.size());
	}
	*/

	private void removePunctuations() {
		String[] cleanedSentence = null;
		ListIterator<String[]> iter = sentencesSource.listIterator();		
		while(iter.hasNext()) {			
			cleanedSentence = removePunctuation(iter.next());
			if (cleanedSentence!=null) {
				iter.remove();
				iter.add(cleanedSentence);
			}
		}
		
		iter = sentencesTarget.listIterator();
		while(iter.hasNext()) {
			cleanedSentence = removePunctuation(iter.next());
			if (cleanedSentence!=null) {
				iter.remove();
				iter.add(cleanedSentence);
			}
		}
		
	}

	private static String[] removePunctuation(String[] lineWords) {
		int i = 0;		
		for(String w : lineWords) {
			if (w.length()==1 && Arrays.binarySearch(ignorePunctChar, w.charAt(0))>=0)
				continue;
			if (Arrays.binarySearch(ignorePunctString, w)>=0)
				continue;
			lineWords[i++] = w;
		}
		if (i==lineWords.length)
			return null;
		return Arrays.copyOf(lineWords, i);
	}

	private void getWordsFreq() {
		HashMap<String,int[]> wordFreqSource = new HashMap<String,int[]>();
		HashMap<String,int[]> wordFreqTarget = new HashMap<String,int[]>();
		getWordsFreq(sentencesSource, wordFreqSource);
		getWordsFreq(sentencesTarget, wordFreqTarget);
		HashMap<String, Integer> wordFreqSourceInteger = Utility.convertHashMapIntArrayInteger(wordFreqSource);
		HashMap<String, Integer> wordFreqTargetInteger = Utility.convertHashMapIntArrayInteger(wordFreqTarget);
		TreeMap<Integer, HashSet<String>> reverseSource = Utility.reverseAndSortTable(wordFreqSourceInteger);
		TreeMap<Integer, HashSet<String>> reverseTarget = Utility.reverseAndSortTable(wordFreqTargetInteger);
		//Utility.printInvertedSortedTableInt(reverseSource, new File("/tmp/source.txt"));
		//Utility.printInvertedSortedTableInt(reverseTarget, new File("/tmp/target.txt"));	
		makeFunctionalSet(reverseSource, functionalWordsSetSource);
		makeFunctionalSet(reverseTarget, functionalWordsSetTarget);
		
	}

	private void makeFunctionalSet(
			TreeMap<Integer, HashSet<String>> reverseTable,
			HashSet<String> functionalWords) {
		int i=0;
		for(HashSet<String> freqSet : reverseTable.descendingMap().values()) {
			functionalWords.addAll(freqSet);
			i+= freqSet.size();
			if (i>=functionalWordThreshold)
				break;
		}
		
	}

	private static void getWordsFreq(ArrayList<String[]> sentences, 
			HashMap<String, int[]> table) {
		for(String[] s : sentences) {
			for(String w : s) {
				Utility.increaseInHashMap(table, w);
			}
		}
		
	}

	public static ArrayList<String[]> getInternedSentences(File inputFile) throws FileNotFoundException {
		ArrayList<String[]> result = new ArrayList<String[]>();
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(inputFile, "utf-8");
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
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

	
	
	/*
	private void printCleanedSentencesToFile() throws FileNotFoundException {
		String dir = printTableFile.getParent() + "/";		
		File outputSource = new File(dir + "train_source.txt");
		File outputTarget = new File(dir + "train_target.txt");
		Parameters.logStdOutPrintln("Printing clean sentences source: " + outputSource);
		Parameters.logStdOutPrintln("Printing clean sentences target: " + outputTarget);
		PrintWriter pwSource = new PrintWriter(outputSource);
		PrintWriter pwTarget = new PrintWriter(outputTarget);
		Iterator<String[]> iterSource = sentencesSource.iterator();
		Iterator<String[]> iterTarget = sentencesTarget.iterator();
		while(iterSource.hasNext()) {
			pwSource.println(Utility.joinStringArrayToString(iterSource.next(), " "));
			pwTarget.println(Utility.joinStringArrayToString(iterTarget.next(), " "));
		}
		pwSource.close();
		pwTarget.close();		
	}
	*/
	
	/*
	private void removeBadSentences() {
		int ignored = 0;
		Iterator<String[]> iterSouce = sentencesSource.iterator();
		Iterator<String[]> iterTarget = sentencesTarget.iterator();
		while(iterSouce.hasNext()) {
			String[] source = iterSouce.next();
			String[] target = iterTarget.next();
			if (ignoreSentence(source[0]) || ignoreSentence(target[0])) {
				iterSouce.remove();
				iterTarget.remove();
			}
			ignored++;
		}		
		Parameters.logStdOutPrintln("Ignored Sentences: " + ignored);
		Parameters.logStdOutPrintln("Remaining Sentences: " + sentencesSource.size());
	}
	*/
	
	protected void computePairwiseMatch()  {
				
		ListIterator<String[]> sourceIter1 = sentencesSource.listIterator();
		ListIterator<String[]> targetIter1 = sentencesTarget.listIterator();
		
		int index1 = -1, index2=0;
		long count = 0;
		while(sourceIter1.hasNext()) {			
			index1++;
			index2 = index1; //incremented immediately
			String[] source1 = sourceIter1.next();
			String[] target1 = targetIter1.next();		
			ListIterator<String[]> sourceIter2 = sentencesSource.listIterator(index2);
			ListIterator<String[]> targetIter2 = sentencesTarget.listIterator(index2);
			while (sourceIter2.hasNext()) {
				index2++;
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
			}										
		}
	}
	
	protected static boolean ignoreSentence(String[] s) {
		//return Arrays.binarySearch(ignoreStartChars, s.charAt(0))>=0;
		return s.length==0 || s[0].charAt(0)=='&';
	}

	static int maxMatchSize = 0;

	protected void computeMatch(int index1, int index2, String[] source1, String[] source2,
			String[] target1, String[] target2) {
		
		if (ignoreSentence(source1) || ignoreSentence(source2) || 
				ignoreSentence(target1) || ignoreSentence(target2))
			return;

		// GET ALL SUBSTRING MATCHES FROM SOURCE PAIR
		HashSet<IdentityArrayList<String>> resultMatchSource = 
				onlyContiguous ?
				CommonSubstring.getAllMaxCommonSubstringsIdentity(source1, source2):
				AllStringSubsequencesSet.getAllMaxCommonSubstringsIdentity(source1, source2, minMatchSize);
		
		// GET ALL SUBSTRING MATCHES FROM TARGET PAIR
		HashSet<IdentityArrayList<String>> resultMatchTarget =
				onlyContiguous ?
				CommonSubstring.getAllMaxCommonSubstringsIdentity(target1, target2) :
				AllStringSubsequencesSet.getAllMaxCommonSubstringsIdentity(target1, target2, minMatchSize);
		
		//Remove exact matches (e.b., proper nouns, etc...)
		cleanFunctionalWords(resultMatchSource, functionalWordsSetSource);
		cleanFunctionalWords(resultMatchTarget, functionalWordsSetTarget);
		cleanExactMatches(resultMatchSource, resultMatchTarget);
		
		if (!onlyContiguous) {
			removeMoreThanOneGap(resultMatchSource,source1);
			removeMoreThanOneGap(resultMatchSource,source2);
			removeMoreThanOneGap(resultMatchTarget,target1);
			removeMoreThanOneGap(resultMatchTarget,target2);
		}
		
		// both not empty
		if (!resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty()) {
			
			int totalMatch = resultMatchSource.size() * resultMatchTarget.size();
			if (totalMatch>maxMatchSize) {
				maxMatchSize = totalMatch; 
				System.out.println("New Max: " + maxMatchSize);
				System.out.println(Arrays.toString(source1));
				System.out.println(Arrays.toString(source2));
				System.out.println(resultMatchSource);				
				System.out.println(Arrays.toString(target1));
				System.out.println(Arrays.toString(target2));
				System.out.println(resultMatchTarget);
				System.out.println("------------------------------");
			}					
			
			addMatchesToFinalTable(index1, index2, resultMatchSource, resultMatchTarget);
		}
	}
	
	private static void removeMoreThanOneGap(
			HashSet<IdentityArrayList<String>> resultMatch, String[] sentece) {
		Iterator<IdentityArrayList<String>> iter = resultMatch.iterator();
		while(iter.hasNext()) {
			IdentityArrayList<String> seq = iter.next();
			if (StringsAlignment.getBestAlignemntGaps(seq.toArray(new String[]{}), sentece)>1)
				iter.remove();
		}
		
	}

	private void cleanFunctionalWords(
			HashSet<IdentityArrayList<String>> wordSequences,
			HashSet<String> functionalWords) {		
		Iterator<IdentityArrayList<String>> iter = wordSequences.iterator();
		while(iter.hasNext()) {
			IdentityArrayList<String> next = iter.next();
			if (functionalWords.containsAll(next))
				iter.remove();
		}		
	}

	/*
	private void printCleanedSentencesToFile() throws FileNotFoundException {
		String dir = printTableFile.getParent() + "/";		
		File outputSource = new File(dir + "train_source.txt");
		File outputTarget = new File(dir + "train_target.txt");
		Parameters.logStdOutPrintln("Printing clean sentences source: " + outputSource);
		Parameters.logStdOutPrintln("Printing clean sentences target: " + outputTarget);
		PrintWriter pwSource = new PrintWriter(outputSource);
		PrintWriter pwTarget = new PrintWriter(outputTarget);
		Iterator<String[]> iterSource = sentencesSource.iterator();
		Iterator<String[]> iterTarget = sentencesTarget.iterator();
		while(iterSource.hasNext()) {
			pwSource.println(Utility.joinStringArrayToString(iterSource.next(), " "));
			pwTarget.println(Utility.joinStringArrayToString(iterTarget.next(), " "));
		}
		pwSource.close();
		pwTarget.close();		
	}
	*/
	
	/*
	private void removeBadSentences() {
		int ignored = 0;
		Iterator<String[]> iterSouce = sentencesSource.iterator();
		Iterator<String[]> iterTarget = sentencesTarget.iterator();
		while(iterSouce.hasNext()) {
			String[] source = iterSouce.next();
			String[] target = iterTarget.next();
			if (ignoreSentence(source[0]) || ignoreSentence(target[0])) {
				iterSouce.remove();
				iterTarget.remove();
			}
			ignored++;
		}		
		Parameters.logStdOutPrintln("Ignored Sentences: " + ignored);
		Parameters.logStdOutPrintln("Remaining Sentences: " + sentencesSource.size());
	}
	*/
	


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
	 * @param sourceTargetIndex
	 * @param outputFileMain
	 */
	private void printFinalTableToFile() {
		
		Parameters.logStdOutPrintln("Printing final table to " + printTableFile);
		Parameters.logStdOutPrintln("Total size of table keys, subkey " + Arrays.toString(totalKeysAndPairs(finalTable)));
		Parameters.logStdOutPrintln(" " + printTableFile);
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
	
	public static HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> 
		readTableFromFile(File inputFile) {
		
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
	
	public static String[] getInternedArrya(String string, String splitExp) {
		String[] split = string.split(splitExp);
		for(int i=0; i<split.length; i++) {
			split[i] = split[i].trim().intern();
		}  
		return split;
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

	public static int getTotalUniqueIndexes(
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
		
		String dir = "/Users/fedja/Dropbox/ted_experiment/";		
		args = new String[]{
			dir + "corpus_en_it/train.tags.en-it.clean.tok.lc.en", //source
			dir + "corpus_en_it/train.tags.en-it.clean.tok.lc.it", //targe
			dir + "en_it/kernels/dummy.gz", //output
			"2", //minLength
			"false" //onlyContiguous	
		};

		
		String usage = "ParallelSubstrings v. " + getVersion() + "\n" + 
				"usage: java ParallelSubstringsMatch "
				+ "-sourceFile:file, -targetFile:file, -outputFile:file "
				+ "-minMatchSize:n -onlyContiguous:true";		
		
		if (args.length!=5) {
			System.err.println("Wrong number of arguments!");
			System.err.println(usage);
		}
				
		File sourceFile = ArgumentReader.readFileOption(args[0]);
		File targetFile = ArgumentReader.readFileOption(args[1]);
		File outputFile = ArgumentReader.readFileOption(args[2]);
		int minMatchSize = ArgumentReader.readIntOption(args[3]);
		onlyContiguous = ArgumentReader.readBooleanOption(args[4]);
		ParallelSubstrings PS = new ParallelSubstrings(
				sourceFile, targetFile, outputFile, minMatchSize);
		PS.run();

		
	}
	
}
