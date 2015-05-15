package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import kernels.algo.CommonSubstring;
import util.IdentityArrayList;
import util.IdentityPair;
import util.PrintProgress;
import util.Utility;

public class ParallelSubstrings_MultiThreaded_IndexBoost {
	
	static boolean convertToLowerCase = true;
	static boolean printLog = false;
	static char[] ignoreStartChars = new char[]{'<','/'};
	static int sentenceBlockSize = 1000;
	
	static {
		Arrays.sort(ignoreStartChars);
	}
	
	Vector<String[]> sentencesSrc, sentencesTrg;
	Set<String> recurringLexSrc, recurringLexTrg;
	
	int totalMatchingPairs;
	//ArrayList<int[]> matchingPairList;
	//ListIterator<int[]> iterMatchinPairs;
	
	TreeMap<Integer, HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,AlignementInfo>>> final_ST_Table, final_TS_Table;
	File logFile, matchTableSTFile,matchTableTSFile;
	PrintWriter pwLog;	
	int threads;
	PrintProgress progress;
	int totalSentences;
	int sentenceIndexStartBlock;
	private Vector<BitSet> sentenceBgIdxVectorSrc, sentenceBgIdxVectorTrg;
	private Vector<BitSet> bigramSntIdxVectorSrc, bigramSntIdxVectorTrg;
	int sentenceIndex1, sentencePairIndex;
	int[] sentencePairs;
	

	public ParallelSubstrings_MultiThreaded_IndexBoost(File sourceFile, File targetFile, File logFile, 
			File matchTableSTFile, File matchTableTSFile, int threads) throws FileNotFoundException {
		
		if (printLog) {
			this.logFile = logFile;
			pwLog = getGzipLogWriter(logFile);
		}
		
		this.matchTableSTFile = matchTableSTFile;
		this.matchTableTSFile = matchTableTSFile;
		this.threads = threads;
		
		sentencesSrc = getSentences(sourceFile);
		sentencesTrg = getSentences(targetFile);
		int linesSource = sentencesSrc.size();
		int linesTarget = sentencesTrg.size();
		if (linesSource!=linesTarget) {
			System.err.println("Source and target files have different number of lines");
			System.err.println(linesSource + " != " + linesTarget);
			return;
		}
		
		totalSentences = linesSource;
		long numberOfPairs = linesSource*(linesSource-1)/2;
		System.out.println("Number of lines in each file: " + linesSource);	
		System.out.println("Total number of pairs: " + numberOfPairs);
		
		buildRecurringLex();
		
		int blocks = totalSentences / sentenceBlockSize + (totalSentences%sentenceBlockSize>0 ? 1 : 0);
		System.out.println("Using boost mode with blocks of sentences of size " + sentenceBlockSize);
		System.out.println("Number of block runs " + blocks);
		
		final_ST_Table = new TreeMap<Integer, HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,AlignementInfo>>>();
		final_TS_Table = new TreeMap<Integer, HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,AlignementInfo>>>();		
		
		ThreadRunner[] threadArray = new ThreadRunner[threads];		
				
		for(int b=0; b<blocks; b++) {
			
			System.out.println();
			
			sentenceIndexStartBlock = b * sentenceBlockSize;
			System.out.println("Runnicn sentence block " + (b+1) + "/" + blocks + " starting at sentence index " + sentenceIndexStartBlock);
			
			//matchingPairList = new ArrayList<int[]>();
			buildBigramCounts();			
			
			sentenceIndex1 = 0; 
			sentencePairs = getSetnencesMatch(sentenceIndex1);
			sentencePairIndex = 0;
			
			//int blockMatchingPairs =  matchingPairList.size();
			//System.out.println("Total of matching pairs in current block: " + blockMatchingPairs);
			//totalMatchingPairs += blockMatchingPairs;
			
			progress = new PrintProgress("Processing matching pairs with sentence", 1, 0);
			//iterMatchinPairs = matchingPairList.listIterator();
			for(int i=0; i<threads; i++) {
				threadArray[i] = new ThreadRunner();
			}
			submitMultithreadJob(threadArray);
			progress.end();
		}				
		
		System.out.println();
		System.out.print("Printing tables to files...");
		printMatchtablesToFile(final_ST_Table, matchTableSTFile);
		printMatchtablesToFile(final_TS_Table, matchTableTSFile);
		System.out.println("done");
		
		System.out.println();
		System.out.println("Total number of pairs: " + numberOfPairs);
		System.out.println("Total matched pairs (boost mode): " + totalMatchingPairs);
		float opt = ((float)numberOfPairs)/totalMatchingPairs;
		System.out.println("Boost optimazation: " + opt);
		
		if (printLog) {
			pwLog.close();
		}
		
	}
	
	private static Vector<String[]> getSentences(File inputFile) throws FileNotFoundException {
		PrintProgress pp = new PrintProgress("Reading sentences", 1, 0);
		Vector<String[]> result = new Vector<String[]>();
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(inputFile);
		int i = 0;
		while(scanner.hasNextLine()) {
			i++;
			pp.next();
			String line = scanner.nextLine();
			if (convertToLowerCase) 
				line = line.toLowerCase();
			String[] lineWords = getWordsFromLine(line);
			result.add(lineWords);			
		}		
		pp.end();
		return result;
	}

	private static String[] getWordsFromLine(String line) {
		String[] lineWords = line.trim().split("\\s+");
		for(int i=0; i<lineWords.length; i++) {
			lineWords[i] = lineWords[i].intern();			
		}
		return lineWords;
	}
	
	private void buildRecurringLex() {
		System.out.println("Building lexicon source");
		recurringLexSrc = getRecurringLex(sentencesSrc); 
		
		System.out.println("Building lexicon target");
		recurringLexTrg = getRecurringLex(sentencesTrg);
	}

	private Set<String> getRecurringLex(Vector<String[]> sentences) {
		IdentityHashMap<String, int[]> unigramTable = new IdentityHashMap<String, int[]>();
		for(String[] s : sentences) {
			for(String w : s) {
				Utility.increaseInHashMap(unigramTable, w);
			}
		}
		System.out.println("Total words: " + unigramTable.size());
		Iterator<Entry<String, int[]>> iter = unigramTable.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, int[]> e = iter.next();
			if (e.getValue()[0]==1) {
				iter.remove();
			}
		}
		System.out.println("Recurring words: " + unigramTable.size());
		return unigramTable.keySet();
		
	}

	private void buildBigramCounts() {
		// build recurring lexicons
		
		//for every recurring bitset all sentence indexes where it occurs
		HashMap<IdentityPair<String>, BitSet> bigramSntIdxTableSrc = new HashMap<IdentityPair<String>, BitSet>(); 
		HashMap<IdentityPair<String>, BitSet> bigramSntIdxTableTrg = new HashMap<IdentityPair<String>, BitSet>();		
		// builds recurring bigram indexes from current sentence block
		buildRecurringBigramTable(sentencesSrc, bigramSntIdxTableSrc, recurringLexSrc, "source"); 
		buildRecurringBigramTable(sentencesTrg, bigramSntIdxTableTrg, recurringLexTrg, "target");
				
		System.out.println("Number of recurring bigrams in sentence block source: " + bigramSntIdxTableSrc.size());
		System.out.println("Number of recurring bigrams in sentence block target: " + bigramSntIdxTableTrg.size());
		
		// for every sentence all recurring bigram indexes it contains
		sentenceBgIdxVectorSrc = new Vector<BitSet>(totalSentences); 
		sentenceBgIdxVectorTrg = new Vector<BitSet>(totalSentences);
		//for every indexed recurring bitset all sentence indexes where it occurs
		bigramSntIdxVectorSrc = new Vector<BitSet>();
		bigramSntIdxVectorTrg = new Vector<BitSet>();
		buildBigramIndex(sentenceBgIdxVectorSrc, bigramSntIdxTableSrc, bigramSntIdxVectorSrc);
		buildBigramIndex(sentenceBgIdxVectorTrg, bigramSntIdxTableTrg, bigramSntIdxVectorTrg);
		
	}

	private void buildRecurringBigramTable(Vector<String[]> sentences,
			HashMap<IdentityPair<String>, BitSet> bigramSntIdxTable, Set<String> recurringLex, String srctrg) {
		
		for(int si = sentenceIndexStartBlock; si<totalSentences && (si==sentenceIndexStartBlock || si%sentenceBlockSize!=0); si++ ) {
			String[] s = sentences.get(si);			
			int l = s.length;
			for(int i=0; i<l-1; i++) {
				String s1 = s[i];
				if (!recurringLex.contains(s1))
					continue;
				String s2 = s[i+1];
				if (!recurringLex.contains(s2))
					continue;
				IdentityPair<String> bigramId = new IdentityPair<String>(s1, s2);
				Utility.setBitSetInHashMap(bigramSntIdxTable, bigramId, si);				
			}
		}
		
		int nextBlockIndex = sentenceIndexStartBlock+sentenceBlockSize;
		PrintProgress pp = new PrintProgress("Building bigram table " + srctrg + " ", 100, 0);
		for(int si=0; si<totalSentences; si++) {						
			pp.next();
			if (si>=sentenceIndexStartBlock && si<nextBlockIndex)
				continue;
			String[] s = sentences.get(si);
			int l = s.length;
			for(int i=0; i<l-1; i++) {
				String s1 = s[i];
				if (!recurringLex.contains(s1))
					continue;
				String s2 = s[i+1];
				if (!recurringLex.contains(s2))
					continue;
				IdentityPair<String> bigramId = new IdentityPair<String>(s1, s2);
				if (!bigramSntIdxTable.containsKey(bigramId)) {
					continue;
				}
				Utility.setBitSetInHashMap(bigramSntIdxTable, bigramId, si);				
			}
		}
		pp.end();
		
		//remove entries in table with count==1 and set indexes to others
		Iterator<Entry<IdentityPair<String>, BitSet>> iter = bigramSntIdxTable.entrySet().iterator();
		while(iter.hasNext()) {
			BitSet bs = iter.next().getValue();
			if (bs.cardinality()==1) {
				iter.remove();
			}
		}		
	}


	private void buildBigramIndex(Vector<BitSet> sentenceBgIdxVector,
			HashMap<IdentityPair<String>, BitSet> bigramSntIdxTable, 
			Vector<BitSet> bigramSntIdxVector) {
		
		//int totalBigrams = bigramSntIdxTable.size();
		for(int i=0; i<totalSentences; i++) {
			sentenceBgIdxVector.add(new BitSet()); //optimize with null
		}
		
		Iterator<Entry<IdentityPair<String>, BitSet>> iter = bigramSntIdxTable.entrySet().iterator();
		int bgIndex = 0;
		while(iter.hasNext()) {			
			BitSet bs = iter.next().getValue();
			bigramSntIdxVector.add(bs);
			for(int sentenceIndex : Utility.makeArray(bs)) {
				sentenceBgIdxVector.get(sentenceIndex).set(bgIndex);
			}
			bgIndex++;
		}	
		
	}

	
	private void submitMultithreadJob(ThreadRunner[] threadArray) {		
		
		for(int i=0; i<threads-1; i++) {
			ThreadRunner t = threadArray[i];
			t.start();						
		}
		threadArray[threads-1].run();
		
		for(int i=0; i<threads-1; i++) {
			try {
				threadArray[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}	
	}
	

	private static void printMatchtablesToFile(
			TreeMap<Integer, HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>>> finalTable, 
			File outputFile) {
		
		PrintWriter pw = getGzipLogWriter(outputFile);
		for(Entry<Integer, HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>>> e : finalTable.entrySet()) {
			Integer length = e.getKey();
			pw.println(length + ":");
			for(Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> f : e.getValue().entrySet()) {
				IdentityArrayList<String> source = f.getKey();
				HashMap<IdentityArrayList<String>, AlignementInfo> subTable = f.getValue();
				int totalSentencesMatch = getUniqueIndexesSize(subTable);
				pw.println("\t" + source + "\t" + totalSentencesMatch);
				for(Entry<IdentityArrayList<String>, AlignementInfo> g : subTable.entrySet()) {
					AlignementInfo ai = g.getValue();
					pw.println("\t\t" + g.getKey() + "\t" + ai.weight + "\t" + ai.indexes.toString());
				}				
			}
		}
		pw.close();
	}


	private static int getUniqueIndexesSize(
			HashMap<IdentityArrayList<String>, AlignementInfo> subTable) {
		HashSet<Integer> indexSet = new HashSet<Integer>(); 
		for(Entry<IdentityArrayList<String>, AlignementInfo> g : subTable.entrySet()) {
			indexSet.addAll(g.getValue().indexes);
		}
		return indexSet.size();
	}

	static final int bufferSize = 512*64; //512 default
	public static PrintWriter getGzipLogWriter(File outputFile) {
		PrintWriter writer = null;
	    try
	    {
	        @SuppressWarnings("resource")
			GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(outputFile), bufferSize);

	        writer = new PrintWriter(new OutputStreamWriter(zip));

	    } catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return writer;
	}


	private static boolean startWithStrangeChar(String s) {
		return Arrays.binarySearch(ignoreStartChars, s.charAt(0))>=0;
	}

	synchronized private PairInfo getNextPairs() {
		
		if (sentencePairs==null)
			return null;
		
		while (sentencePairIndex==sentencePairs.length) {
			sentencePairs = getSetnencesMatch(++sentenceIndex1);
			progress.next();
			if (sentencePairs==null)
				return null;
			sentencePairIndex = 0;
		}
		
		totalMatchingPairs++;
		int i1 = sentenceIndex1;
		int i2 = sentencePairs[sentencePairIndex++];
		PairInfo pi = new PairInfo(sentencesSrc.get(i1), sentencesSrc.get(i2), sentencesTrg.get(i1), sentencesTrg.get(i2), i1, i2);
		return pi;
		
		/*
		if(iterMatchinPairs.hasNext()) {
			progress.next();
			int[] pair = iterMatchinPairs.next();
			int i1 = pair[0];
			int i2 = pair[1];
			if (i1==i2) {
				System.err.println("Reflexive pair");
				return null;
			}
			PairInfo pi = new PairInfo(sentencesSrc.get(i1), sentencesSrc.get(i2), sentencesTrg.get(i1), sentencesTrg.get(i2), i1, i2);
			return pi;
		}
		return null;
		*/
	}
	
	
	private int[] getSetnencesMatch(int sentenceIndex) {
		
		if (sentenceIndex==totalSentences)
			return null;
		
		//PrintProgress pp = new PrintProgress("Building final index match ", 100, 0);
		//for(int i=0; i<totalSentences; i++) {
			//pp.next();
			BitSet matchingSntBs = getUnionBsSentences(sentenceIndex, sentenceBgIdxVectorSrc, bigramSntIdxVectorSrc);
			BitSet matchingSntTrg = getUnionBsSentences(sentenceIndex, sentenceBgIdxVectorTrg, bigramSntIdxVectorTrg);
			matchingSntBs.and(matchingSntTrg);
			matchingSntBs.clear(0, sentenceIndex+1); // exclude previoius sentences and current one (to avoid redundant pairs (a,b) (b,a) and reflexive ones (a,a))
			return Utility.makeArray(matchingSntBs);			
			//for(int j : indexSet) {
				//matchingPairList.add(new int[]{i,j});
			//}			
		//}
		//pp.end();
	}

	private static BitSet getUnionBsSentences(int i,
			Vector<BitSet> sentenceBgIdxVector,
			Vector<BitSet> bigramSntIdxVector) {
		BitSet union = new BitSet();
		BitSet bgIdxBs = sentenceBgIdxVector.get(i); // all indexes of bigrams in sentence
		for(int bgIdx : Utility.makeArray(bgIdxBs)) {
			BitSet sntIdxBs = bigramSntIdxVector.get(bgIdx); //all indexes of senteces containing the bigram
			union.or(sntIdxBs);
		}
		return union;
	}

	static protected class PairInfo {
		String[] source1, source2, target1, target2;
		int index1, index2;
		
		public PairInfo(String[] source1, String[] source2, String[] target1, String[] target2, int index1, int index2) {
			this.source1 = source1;
			this.source2 = source2;
			this.target1 = target1;
			this.target2 = target2;
			this.index1 = index1;
			this.index2 = index2;
		}
	}
	
	static protected class AlignementInfo {
		
		double weight;
		TreeSet<Integer> indexes;
		
		public AlignementInfo(double weight, int index1, int index2) {
			this.weight = weight;
			this.indexes = new TreeSet<Integer>();
			this.indexes.add(index1);
			this.indexes.add(index2);
		}
		
		public void addWeightIndex(double w, int index1, int index2) {
			this.weight += w;
			this.indexes.add(index1);
			this.indexes.add(index2);
		}
		
	}

	/*
	private void computePairwiseMatch() {
				
		PrintProgress progress = new PrintProgress("Processing line", 100, 0);
		ListIterator<String[]> sourceIter1 = sentencesSouce.listIterator();
		ListIterator<String[]> targetIter1 = sentencesTarget.listIterator();
		
		int index1 = -1, index2=0;
		while(sourceIter1.hasNext()) {			
			index1++;
			index2 = index1 + 1;
			String[] source1 = sourceIter1.next();
			String[] target1 = targetIter1.next();			
			ListIterator<String[]> sourceIter2 = sentencesSouce.listIterator(index2);
			ListIterator<String[]> targetIter2 = sentencesTarget.listIterator(index2);
			while (sourceIter2.hasNext()) {
				progress.next();
				String[] source2 = sourceIter2.next();
				String[] target2 = targetIter2.next();
				computeMatch(index1, index2, source1, source2, target1, target2);
				index2++;
			}										
		}
		
		progress.end();		
		
	}
	*/
	


	synchronized private static void addToFinalTable(Integer length,
			IdentityArrayList<String> sourceMatch,
			IdentityArrayList<String> targetMatch, double weight, 
			int index1, int index2, 
			TreeMap<Integer, HashMap<IdentityArrayList<String>, 
			HashMap<IdentityArrayList<String>, AlignementInfo>>> finalTable) {
		
		HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> 
		finalSubTable = finalTable.get(length);
		if (finalSubTable==null) {
			finalSubTable = new HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>,AlignementInfo>>();
			finalTable.put(length, finalSubTable);
		}
		HashMap<IdentityArrayList<String>, AlignementInfo> tableMatch = finalSubTable.get(sourceMatch);
		if (tableMatch==null) {
			tableMatch = new HashMap<IdentityArrayList<String>, AlignementInfo>();
			finalSubTable.put(sourceMatch, tableMatch);
		}
		AlignementInfo entryMatch = tableMatch.get(targetMatch);
		if (entryMatch==null) {
			entryMatch = new AlignementInfo(weight,index1,index2);
			tableMatch.put(targetMatch, entryMatch);
		}
		else
			entryMatch.addWeightIndex(weight, index1, index2);
	}


	private static double computeWeight(int typeCombination, int sourceTokens,
			int targetTokens) {
		double tokensRatio =  (sourceTokens>targetTokens) ? 
				((double)sourceTokens)/targetTokens : ((double)targetTokens)/sourceTokens; 
		return 1d/typeCombination * tokensRatio;
	}


	private static int[] totalTypesTokens(
			TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultMatch) {
		int tokens = 0;
		int types = 0;
		for(HashMap<IdentityArrayList<String>, ArrayList<int[]>> subTable : resultMatch.values()) {
			types += subTable.size();
			for(ArrayList<int[]> l : subTable.values()) {
				tokens += l.size();
			}
		}		
		return new int[]{types, tokens};
	}


	private static HashMap<IdentityArrayList<String>, ArrayList<int[]>> flatTable(
			TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultMatch) {
		HashMap<IdentityArrayList<String>, ArrayList<int[]>> result =
				new HashMap<IdentityArrayList<String>, ArrayList<int[]>>();
		for(HashMap<IdentityArrayList<String>, ArrayList<int[]>> subTable : resultMatch.values()) {
			result.putAll(subTable);
		}
		return result;
	}


	private void logMatch(boolean sourceMatch, boolean targetMatch,
			int index1, int index2, String[] source1,
			String[] source2, String[] target1, String[] target2, 
			TreeMap<Integer, HashMap<IdentityArrayList<String>, 
			ArrayList<int[]>>> resultMatchSource, TreeMap<Integer, 
			HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultMatchTarget) {
		
		StringBuilder sb = new StringBuilder(5000);
		sb.append("PAIRS " + index1 + "-" + index2 + "\n");
		
		/*
		if (sourceMatch && targetMatch) 
			pwLog.println("MATCHES BOTH SIDES");
		else if (sourceMatch)
			pwLog.println("MATCHES ONLY IN SOURCE");
		else 
			pwLog.println("MATCHES ONLY IN TARGET");
		
		if (sourceMatch) {
			pwLog.println("Source1:");
			pwLog.println("\t" + Arrays.toString(source1));
			pwLog.println("Source2:");
			pwLog.println("\t" + Arrays.toString(source2));
			pwLog.println();			
			printMatchToLog(resultMatchSource);
		}
		*/
		
		//if (targetMatch) {
			sb.append("Target1:\n");
			sb.append("\t" + Arrays.toString(target1) + "\n");
			sb.append("Target2:\n");
			sb.append("\t" + Arrays.toString(target2) + "\n\n");
			printTableToSb(resultMatchTarget, sb);
		//}
		
		
		sb.append("-------------------------------------------------------\n");
		logMatch(sb.toString());
		
	}
	
	synchronized private void logMatch(String pairReport) {
		
		pwLog.println(pairReport);
		
	}


	
	private static void printTableToSb(
			TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultTable, StringBuilder sb) {
		for(Entry<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> e : resultTable.descendingMap().entrySet()) {
			int length = e.getKey();
			sb.append("\t\t" + length + ":\n");
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> subMap = e.getValue();
			for (Entry<IdentityArrayList<String>, ArrayList<int[]>> f : subMap.entrySet()) {
				sb.append("\t\t\t" + f.getKey().toString() + 
						" @ " + Arrays.deepToString(f.getValue().toArray(new int[][]{})) + "\n");
			}			
		}
	}
	
	

	static private void cleanExactMatches(
			TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> table1, 
			TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> table2) {
		
		Iterator<Entry<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>>> iter1 = table1.entrySet().iterator();
		
		while(iter1.hasNext()) {
			Entry<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> e1 = iter1.next();
			Integer key1 = e1.getKey();						
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> subTab2 = table2.get(key1);
			if (subTab2==null)
				continue;
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> subTab1 = e1.getValue();
			Set<IdentityArrayList<String>> subTab2Matches = subTab2.keySet();
			Iterator<Entry<IdentityArrayList<String>, ArrayList<int[]>>> subIter1 = subTab1.entrySet().iterator();
			while(subIter1.hasNext()) {
				Entry<IdentityArrayList<String>, ArrayList<int[]>> f = subIter1.next();
				IdentityArrayList<String> match1 = f.getKey();
				if (subTab2Matches.remove(match1)) {
					subIter1.remove();					
				}
			}
			if (subTab1.isEmpty())
				iter1.remove();
			if (subTab2.isEmpty())
				table2.remove(key1);			
		}
		
		/*
		if (table1.size()==1 && table2.size()==1) {
			HashMap<IdentityPair<String>, ArrayList<int[]>> subTab1 = table1.entrySet().iterator().next().getValue();
			HashMap<IdentityPair<String>, ArrayList<int[]>> subTab2 = table2.entrySet().iterator().next().getValue();
			if (subTab1.size()==1 && subTab2.size()==1) {
				IdentityPair<String> s1 = subTab1.entrySet().iterator().next().getKey();
				IdentityPair<String> s2 = subTab2.entrySet().iterator().next().getKey();
				if (s1.equals(s2)) {
					table1.clear();
					table2.clear();
					return;
				}
			}
		}
		*/
	}
	
	protected class ThreadRunner extends Thread {
								
		public ThreadRunner () {
			
		}

		public void run () {
			PairInfo pi = null;
			while( (pi = getNextPairs()) != null) {
				computeMatch(pi.index1, pi.index2, pi.source1, pi.source2, pi.target1, pi.target2);				
			}
			
		}
		
		/*
		private boolean hasDualMatch(int index1, int index2) {
			BitSet bsSrc1 = sentenceBgIdxVectorSrc.get(index1);
			BitSet bsSrc2 = sentenceBgIdxVectorSrc.get(index2);
			if (!bsSrc1.intersects(bsSrc2))
				return false;
			
			BitSet bsTrg1 = sentenceBgIdxVectorTrg.get(index1);
			BitSet bsTrg2 = sentenceBgIdxVectorTrg.get(index2);
			if (!bsTrg1.intersects(bsTrg2))
				return false;
			
			return true;
		}
		*/

		private void computeMatch(int index1, int index2, String[] source1, String[] source2,
				String[] target1, String[] target2) {
			/*
			if (!hasDualMatch(index1, index2))
				return;
			*/
			
			if (source1[0].isEmpty() || source2[0].isEmpty())
				return;
				
			if (startWithStrangeChar(source1[0]) || startWithStrangeChar(source2[0])) 
				return;
			
			TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultMatchSource = 
					CommonSubstring.getAllMaxCommonSubstringsIdentityLength(source1, source2);
			
			TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultMatchTarget = 
					CommonSubstring.getAllMaxCommonSubstringsIdentityLength(target1, target2);
			
			cleanExactMatches(resultMatchSource, resultMatchTarget);
			
			/*
			if (resultMatchSource.isEmpty() && resultMatchTarget.isEmpty()) {
				//no match
				return;
			}
			
			if (!resultMatchSource.isEmpty() && resultMatchTarget.isEmpty()) {
				//match source
				logMatch(true, false, index1, index2, source1, source2,
						target1, target2, resultMatchSource, resultMatchTarget);
				return;
			}
			
			if (resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty()) {
				//match target
				logMatch(false, true, index1, index2, source1, source2,
						target1, target2, resultMatchSource, resultMatchTarget);
				return;
			}
			*/
			
			//last case: !resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty()
			if (!resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty()) {
				if (printLog) {
					logMatch(true, true, index1, index2, source1, source2,
						target1, target2, resultMatchSource, resultMatchTarget);
				}
				addMatchesToFinalTable(index1, index2, source1, source2,
						target1, target2, resultMatchSource, resultMatchTarget);
			}
			
		}
		
		private void addMatchesToFinalTable(
				int index1, int index2, String[] source1, String[] source2,
				String[] target1, String[] target2,
				TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultMatchSource,
				TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultMatchTarget) {
			
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchTargetFlat = flatTable(resultMatchTarget);
			int[] sourceTypesTokens = totalTypesTokens(resultMatchSource);
			int[] targetTypesTokens = totalTypesTokens(resultMatchTarget);
			int typeCombination = sourceTypesTokens[0]*targetTypesTokens[0];
					
			for(Entry<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> e : resultMatchSource.entrySet()) {
				Integer length = e.getKey();			
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> subTable = e.getValue();
				for(Entry<IdentityArrayList<String>, ArrayList<int[]>> f1 : subTable.entrySet()) {
					IdentityArrayList<String> sourceMatch = f1.getKey();
					int sourceTokens = f1.getValue().size();
					for(Entry<IdentityArrayList<String>, ArrayList<int[]>> f2 : resultMatchTargetFlat.entrySet()) {
						IdentityArrayList<String> targetMatch = f2.getKey();
						int targetTokens = f2.getValue().size();
						double weight = computeWeight(typeCombination, sourceTokens, targetTokens);
						if (weight>0) {
							addToFinalTable(length, sourceMatch, targetMatch, weight, index1, index2, final_ST_Table);
							addToFinalTable(targetMatch.size(), targetMatch, sourceMatch, weight,  index1, index2, final_TS_Table);
						}
					}
				}			
			}
		}
		
	}

	public static void main(String args[]) throws FileNotFoundException {
		
		/*
		String dir = "/gardner0/data/Corpora/Europarl/it-en/";
		String sourceFile = dir + "europarl-v7.it-en.tok.clean.en.head";
		String targetFile = dir + "europarl-v7.it-en.tok.clean.it.head";
		String logFile = dir + "matching.head.boost.log";
		String tableSrc = dir + "matchingTableIndexes.head.en-it.head.boost.gz";
		String tableTrg = dir + "matchingTableIndexes.head.it-en.head.boost.gz";
		String threads = "1";
		args = new String[]{sourceFile, targetFile, logFile, tableSrc, tableTrg, threads};
		*/
		
		String usage = "java ParallelSubstrings_MultiThreaded_IndexBoost sourceFile targetFile "
				+ "logFile matchTable_ST_File matchTable_TS_File threads";
		if (args.length!=6) {
			System.err.println("Wrong number of arguments!");
			System.err.println(usage);
			return;
		}
		CommonSubstring.minMatchLength = 2;
		
		long startTime = System.currentTimeMillis();
		
		System.out.println("ParallelSubstringsIndexBoos v. 1.0");
		
		new ParallelSubstrings_MultiThreaded_IndexBoost(new File(args[0]),new File(args[1]), 
				new File(args[2]), new File(args[3]), new File(args[4]), Integer.parseInt(args[5]));
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("Took " + ((float)(endTime-startTime))/1000 + " s");
		
		/*
		String s = "Basil Jones : But actually we 're going to start this evolution with a hyena .";
		String t = "< description > \" Puppets always have to try to be alive , \" says Adrian Kohler of the Handspring Puppet Company , a gloriously ambitious troupe of human and wooden actors . Beginning with the tale of a hyena 's subtle paw , puppeteers Kohler and Basil Jones build to the story of their latest astonishment : the wonderfully life-like Joey , the War Horse , who trots convincingly onto the stage . < / description >";
		test2Sentences(s,t);
		*/
	}
	
}
