package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

import tsg.Label;
import tsg.TSNodeLabel;
import util.Pair;
import util.PrintProgress;
import util.SetPartition;
import util.Utility;
import util.file.FileUtil;

public class TSG_Frags_MWE_Sign_AM {

	static int FragFreqThreshold = 5;
	static int SignFragCountThreshold = 100;
	static boolean attachPosToWords = true; // used for computing association measures
	static boolean countGapCateogiesInAssMeasures = true;		
	static boolean countOuterFronteersInSignature = true;
	static boolean ignoreFragsWithPunct = true;
	static boolean ignoreFragsWithNNP = true;
	static boolean encodePOSLabelsInSignature = true;
	static boolean addRootLabelsInSignature = true; 	
	
	public static String[] punctWords = new String[]{"#", "$", "''", ",", "-LCB-", "-LRB-", 
		"-RCB-", "-RRB-", ".", ":", "``", ";", "?", "!"};
	static {
		Arrays.sort(punctWords);
	}
	
	File inputFile, outputFileStats;
	String FileStatsName, FileFragPrefix;
	String outputDir;	
	int totalFrags, acquiredFrags;
	HashMap<Integer, HashMap<String, int[]>> lengthSignatureFragTypesCount = 
			new HashMap<Integer, HashMap<String,int[]>>();
	HashMap<Integer, HashMap<String, HashMap<TSNodeLabel, int[]>>> lengthSignatureFragFreq = 
			new HashMap<Integer, HashMap<String, HashMap<TSNodeLabel, int[]>>>();
	
	public TSG_Frags_MWE_Sign_AM(File inputFile, String outputDir) throws Exception {
		FileStatsName = "stats_fragMinFreq:" + FragFreqThreshold + "_signMinCount" + SignFragCountThreshold + ".txt";
		FileFragPrefix = "frags_fragMinFreq:" + FragFreqThreshold + "_signMinCount" + SignFragCountThreshold + "_"; // lexCount_signature
		this.inputFile = inputFile;
		this.outputDir = new File(outputDir) + "/";
		this.outputFileStats = new File(this.outputDir + FileStatsName);		
		readFragmentBank();
		printFileStats();
		printFragsMPI();		
	}
	
	private void readFragmentBank()  throws Exception {
		PrintProgress progress = new PrintProgress("Reading fragments", 100, 0);
		Scanner scan = new Scanner(inputFile);
		int index = 0;
		while(scan.hasNextLine()) {
			totalFrags++;
			String line = scan.nextLine();
			progress.next();
			String[] lineSplit = line.split("\t");
			TSNodeLabel frag = TSNodeLabel.newTSNodeLabelStd(lineSplit[0]);
			int freq = Integer.parseInt(lineSplit[1]);			
			if (freq<FragFreqThreshold || !passCheckPunctuation(frag) || !passCheckNNP(frag) || frag.countLexicalNodes()<2)
				continue;
			addFrag(frag, freq, index);
			index++;
		}
		progress.end();
		System.out.println("Total frags: " + totalFrags);
		System.out.println("Acquired frags: " + acquiredFrags);		
	}

	private boolean passCheckPunctuation(TSNodeLabel frag) {
		if (!ignoreFragsWithPunct)
			return true;
		for (String l : frag.collectLexicalWords()) {
			if (isPunctWord(l))
				return false;
		}
		return true;
	}
	
	public static boolean isPunctWord(String w) {
		return Arrays.binarySearch(punctWords, w)>=0;
	}
	
	private boolean passCheckNNP(TSNodeLabel frag) {
		if (!ignoreFragsWithNNP)
			return true;
		for (TSNodeLabel l : frag.collectLexicalItems()) {
			if (l.parent.label().startsWith("NNP")) // NNP, NNPS
				return false;
		}
		return true;
	}
	

	private void addFrag(TSNodeLabel frag, int freq, int index) {
		acquiredFrags++;
		ArrayList<TSNodeLabel> terms = frag.collectTerminalItems();
		int[] lexCount = new int[]{0};	
		int[] gapCount = new int[]{0};
		String signature = getSignature(terms, frag.label, lexCount, gapCount);
		Utility.increaseInHashMap(lengthSignatureFragTypesCount, lexCount[0], signature, 1);
		Utility.putInMapTriple(lengthSignatureFragFreq, lexCount[0], signature, frag, new int[]{freq});
	}
	
	static char catBinSymbol = '|';

	private String getSignature(ArrayList<TSNodeLabel> terms, Label root, int[] lexCount, int[] gapCount) {
		//int finalOuterNonLexTermCount = 0;
		StringBuilder signature = new StringBuilder();
		if (addRootLabelsInSignature) {
			String rootLabel = root.toString();
			if (rootLabel.indexOf(catBinSymbol)>=0)
				rootLabel = rootLabel.substring(0, rootLabel.indexOf(catBinSymbol));
			signature.append(rootLabel + "|");
		}
		if (countOuterFronteersInSignature) {			
			for(Iterator<TSNodeLabel> i = terms.iterator(); i.hasNext(); ) {
				TSNodeLabel term = i.next();				
				appendTermSignature(signature, term, lexCount, gapCount);
				if (i.hasNext())
					signature.append('_');
			}
		}
		else {
			int nonLexTermCount = 0;
			int initialOuterNonLexTermCount = 0;	
			ArrayList<Integer> lexIndexes = new ArrayList<Integer>();
			ArrayList<Integer> nonLexTermIndexes = new ArrayList<Integer>();
			int i=0;
			boolean firstLexReached = false;
			for(TSNodeLabel t : terms) {
				if (t.isLexical) {
					lexCount[0]++;
					lexIndexes.add(i);
					firstLexReached = true;
				}
				else {
					nonLexTermCount++;
					nonLexTermIndexes.add(i);
					if (!firstLexReached)
						initialOuterNonLexTermCount++;
				}
				i++;
			}		
			int firstLexIndex = lexIndexes.get(0);
			int lastLexIndex = lexIndexes.get(lexIndexes.size()-1);			
			for(i=firstLexIndex; i<=lastLexIndex; i++) {
				TSNodeLabel t = terms.get(i);
				appendTermSignature(signature, t, lexCount, gapCount);
				if (i!=lastLexIndex)
					signature.append('_');
			}
		}		
		//finalOuterNonLexTermCount = nonLexTermCount - gapCount - initialOuterNonLexTermCount;
		return signature.toString();
	}

	private void appendTermSignature(StringBuilder signature, TSNodeLabel t, int[] lexCount, int[] gapCount) {
		if (encodePOSLabelsInSignature) {
			if (t.isLexical) {
				lexCount[0]++;
				signature.append("L:" + t.parent.label());
			}
			else {
				gapCount[0]++;
				signature.append("X:" + t.label());				
			}
		}
		else {
			if (t.isLexical) {
				lexCount[0]++;
				signature.append('L');
			}
			else {
				gapCount[0]++;
				signature.append('X');				
			}
		}
		
	}

	private void printFileStats() throws Exception {		
		System.out.println("Output file stats: " + outputFileStats);		
		PrintWriter pw = new PrintWriter(outputFileStats);
		pw.println("Considering frags with freq >= " + FragFreqThreshold);
		pw.println("File Signature Fragment Count threshold: " + SignFragCountThreshold);
		pw.println("Ignore frags with punctuation: " + ignoreFragsWithPunct);
		pw.println("Ignore frags with NNP: " + ignoreFragsWithNNP);		
		pw.println("Attach pos to words: " + attachPosToWords);
		pw.println("Count gap categores in association measures: " + countGapCateogiesInAssMeasures);
		pw.println("Count outer fronteers non-lexical items in signature: " + countOuterFronteersInSignature);
		pw.println("Encode POS labels in signature: " + encodePOSLabelsInSignature);
		pw.println("Encode frag root label in signature: " + addRootLabelsInSignature);
		
		pw.println("Input file: " + inputFile);		
		pw.println("Total frags: " + totalFrags);
				
		pw.println("Acquired frags: " + acquiredFrags);		
		pw.println();		
		
		pw.println("Fragments Lexical Count Statistics");		
		pw.println("LexCount\tFragTypes");
		for(Entry<Integer, HashMap<String, int[]>> e : lengthSignatureFragTypesCount.entrySet()) {
			int lexCount = e.getKey();
			int fragTypes = Utility.totalSumValues(e.getValue());
			pw.println(lexCount + "\t" + fragTypes);
		}
		
		pw.println();
		
		pw.println("Signatures Statistics");
		pw.println("LexCount\tSignature\tFragTypes");
		for(Entry<Integer, HashMap<String, int[]>> e : lengthSignatureFragTypesCount.entrySet()) {
			int lexCount = e.getKey();
			TreeMap<Integer, HashSet<String>> subTab = Utility.reverseAndSortTable(Utility.convertHashMapIntArrayInteger(e.getValue()));
			Iterator<Integer> iter = subTab.descendingKeySet().iterator();
			int remainder = 0;
			while(iter.hasNext()) {
				Integer count = iter.next();
				if (count<SignFragCountThreshold) {
					remainder+=count;
					continue;
				}
				for(String sign : subTab.get(count)) {
					pw.println(lexCount + "\t" + sign + "\t" + count);
				}				
			}	
			if (remainder>0)
				pw.println(lexCount + "\t" + "others" + "\t" + remainder);
		}
		pw.close();
	}
	
	private void printFragsMPI() throws FileNotFoundException {
		System.out.println("Printing fragments to " + outputDir);
		String fields = "Fields are\n"
				+ "1) fronteers in pattern\n"
				+ "2) frags\n"				
				+ "3) freq\n"
				+ "4) logMPI (pair interaction)\n"
				+ "5) logMPI (total correlation)\n"				
				+ "6) logLikelihood (only contiguous span in denominator of formula)\n"
				+ "7) relFreq (with respect to the freq of unlexicalized frag)\n"
				+ "8) entropy (with respect to the group of frags sharing the same unlexicalized template)";
		System.out.println(fields);
		FileUtil.appendReturn(fields, new File(outputDir + "readme.txt"));
		
		for(Entry<Integer, HashMap<String, HashMap<TSNodeLabel, int[]>>> a : 
				lengthSignatureFragFreq.entrySet()) {
			int signatureLength = a.getKey();			
			for(Entry<String, HashMap<TSNodeLabel, int[]>> b : a.getValue().entrySet()) {
				String signLabel = b.getKey();
				HashMap<TSNodeLabel, int[]> table = b.getValue();
				int size = table.size(); 
				if (size<SignFragCountThreshold)
					continue;
				
				HashMap<TSNodeLabel,  HashMap<TSNodeLabel, Integer>> unlexFragTypesFreq =
					TSG_Frags_MWE_EntropyStats.buildUnlexFragTypesFreq(table);
				
				File outputFile = new File(outputDir + FileFragPrefix + signatureLength + "_" + signLabel);	
				PrintWriter pw = new PrintWriter(outputFile);
				
				System.out.println("Printing file " + outputFile);
				
				int totalFreq = Utility.totalSumValues(table);
				int sizeRelevantTerms = getRelevantTerms(table.keySet().iterator().next()).size();
				int[][] pairsIndex = Utility.n_air(sizeRelevantTerms, 2);
				int[][][] sizeGroups = getPartitionGroups(sizeRelevantTerms);  // group_size-1, group, indexes
				int[][][] partitions = getPartitionSetIndexes(sizeRelevantTerms); //  partition, group, indexes
				
				
				HashMap<String, int[]> termIndexFreqTable = getTermIndexFreqTable(table);
				HashMap<Pair<String>, int[]> termPairIndexFreqTable = getTermPairIndexFreqTable(table);
				HashMap<Integer, HashMap<ArrayList<String>, int[]>> termGropupPartitionFreqTable = 
					getGroupPartitionFreqTabel(table, sizeGroups); // size_group, groupLabel, freq per group index
				
				for(Entry<TSNodeLabel, int[]> f : table.entrySet()) {
					TSNodeLabel frag = f.getKey();
					int freq = f.getValue()[0];
					String fronteers = getFronteerPattern(frag);					
					double logMpi_pairInter = logMpi_pairInter(frag, freq, table, totalFreq, termIndexFreqTable, termPairIndexFreqTable, pairsIndex);
					double logMpi_totCorr = logMpi_totCorr(frag, freq, table, totalFreq, termIndexFreqTable);					
					double logLikelihood = logLikelihood(frag, freq, table, totalFreq, termGropupPartitionFreqTable, sizeGroups, partitions);
					double[] relFreqEntropy = TSG_Frags_MWE_EntropyStats.getRelFreqEntropy(frag, freq, unlexFragTypesFreq);
					pw.println(fronteers + "\t" + frag.toStringStandard() + "\t" + freq + "\t" + 
							logMpi_pairInter + "\t" + logMpi_totCorr + "\t" + logLikelihood + "\t" +
							relFreqEntropy[0] + "\t" + relFreqEntropy[1]);
				}
				pw.close();				
			}			
		}
		
	}

	public static String getFronteerPattern(TSNodeLabel frag) {
		ArrayList<TSNodeLabel> terms = getRelevantTerms(frag, true);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(TSNodeLabel t : terms) {
			if (first)
				first = false;
			else
				sb.append(' ');
			if (t.isLexical) 
				sb.append(t.parent.label() + "_" + t.label());
			else 
				sb.append(t.label());			
		}
		return sb.toString();
	}

	private HashMap<String, int[]> getTermIndexFreqTable(HashMap<TSNodeLabel, int[]> table) {
		HashMap<String, int[]> result = new HashMap<String, int[]>();
		for(Entry<TSNodeLabel, int[]> e : table.entrySet()) {			
			ArrayList<TSNodeLabel> terms = getRelevantTerms(e.getKey());
			int freq = e.getValue()[0];
			int size = terms.size();
			for(int i=0; i<size; i++) {
				String l = getTermLabel(terms.get(i));
				Utility.increaseInHashMapIndex(result, l, freq, i, size);
			}
		}
		return result;
	}
	
	private HashMap<Pair<String>, int[]> getTermPairIndexFreqTable(HashMap<TSNodeLabel, int[]> table) {
		HashMap<Pair<String>, int[]> result = new HashMap<Pair<String>, int[]>();
		for(Entry<TSNodeLabel, int[]> e : table.entrySet()) {			
			ArrayList<TSNodeLabel> terms = getRelevantTerms(e.getKey());
			int freq = e.getValue()[0];
			int size = terms.size();
			int[][] pairsIndex = Utility.n_air(size, 2);
			int numberOfPairs = pairsIndex.length;
			//e.g., for size = 4 : [[0, 1], [0, 2], [0, 3], [1, 2], [1, 3], [2, 3]]
			for(int p=0; p<numberOfPairs; p++) {
				int[] pair = pairsIndex[p];
				String labelX = getTermLabel(terms.get(pair[0]));
				String labelY = getTermLabel(terms.get(pair[1]));
				Pair<String> l = new Pair<String>(labelX,labelY);
				Utility.increaseInHashMapIndex(result, l, freq, p, numberOfPairs);
			}
		}
		return result;
	}
	
	private HashMap<Integer, HashMap<ArrayList<String>, int[]>> getGroupPartitionFreqTabel(
			HashMap<TSNodeLabel, int[]> table, int[][][] sizeGroups) {
		
		HashMap<Integer, HashMap<ArrayList<String>, int[]>> result = 
			new HashMap<Integer, HashMap<ArrayList<String>, int[]>>();
		for(Entry<TSNodeLabel, int[]> e : table.entrySet()) {			
			ArrayList<TSNodeLabel> terms = getRelevantTerms(e.getKey());
			int freq = e.getValue()[0];
			//int setSize = terms.size();
			//int[][][] sizeGroups = getPartitionGroups(setSize);  // group_size-1, group, indexes
			int groupSize = 0;
			for(int[][] groups : sizeGroups) {
				groupSize++; // start from 1				
				int numberOfGoupsOfSameSize = groups.length;
				for(int groupIndex = 0; groupIndex<numberOfGoupsOfSameSize; groupIndex++) {
					int[] g = groups[groupIndex];
					ArrayList<String> signLabels = getTermLabels(terms, g);
					Utility.increaseInHashMapIndex(result, groupSize, signLabels, freq, groupIndex, numberOfGoupsOfSameSize);
				}				
			}			
		}			
		return result;
	}
	


	private double logMpi_pairInter(TSNodeLabel frag, int fragFreq, HashMap<TSNodeLabel, int[]> table, 
			int totalFreq, HashMap<String, int[]> termIndexFreqTable, 
			HashMap<Pair<String>, int[]> termIndexPairFreqTable, int[][] pairsIndex) {
		
		double result = - Math.log( (double)fragFreq/(double)totalFreq); // joint prob. of all elements, e.g., p(x,y,z)
		ArrayList<TSNodeLabel> terms = getRelevantTerms(frag);
		int size = terms.size();		
		for(int i=0; i<size; i++) {
			String l = getTermLabel(terms.get(i));
			int[] termIndexFreq = termIndexFreqTable.get(l);
			result -= Math.log( (double)termIndexFreq[i]/(double)totalFreq);
		}	

		//int[][] pairsIndex = Utility.n_air(size, 2);
		int numberOfPairs = pairsIndex.length;
		//e.g., for size = 4 : [[0, 1], [0, 2], [0, 3], [1, 2], [1, 3], [2, 3]]
		for(int p=0; p<numberOfPairs; p++) {
			int[] pair = pairsIndex[p];
			String labelX = getTermLabel(terms.get(pair[0]));
			String labelY = getTermLabel(terms.get(pair[1]));
			Pair<String> l = new Pair<String>(labelX,labelY);
			int[] termIndexPairFreq = termIndexPairFreqTable.get(l);
			result += Math.log( (double)termIndexPairFreq[p]/(double)totalFreq);
		}
		return result;
	}
	
	private double logMpi_totCorr(TSNodeLabel frag, int fragFreq, HashMap<TSNodeLabel, int[]> table, 
			int totalFreq, HashMap<String, int[]> termIndexFreqTable) {
		
		double result = Math.log( (double)fragFreq/(double)totalFreq); //// joint prob. of all elements, e.g., p(x,y,z)
		ArrayList<TSNodeLabel> terms = getRelevantTerms(frag);
		int s = terms.size();		
		for(int i=0; i<s; i++) {
			String l = getTermLabel(terms.get(i));
			int[] termIndexFreq = termIndexFreqTable.get(l);
			result -= Math.log( (double)termIndexFreq[i]/(double)totalFreq);
		}		
		return result;
	}


	private double logLikelihood(TSNodeLabel frag, int fragFreq, HashMap<TSNodeLabel, int[]> table,
			int totalFreq, HashMap<Integer, HashMap<ArrayList<String>, int[]>> termGropupPartitionFreqTable, 
			int[][][] sizeGroups, int[][][] partitions) {
		
		double result = Math.log( (double)fragFreq/(double)totalFreq); //// joint prob. of all elements, e.g., p(x,y,z)
		ArrayList<TSNodeLabel> terms = getRelevantTerms(frag);
		//int setSize = terms.size();
		//int[][][] sizeGroups = getPartitionGroups(setSize);  // group_size-1, group, indexes
		//int[][][] partitions = getPartitionSetIndexes(setSize); //  partition, group, indexes
		HashMap<ArrayList<Integer>,Double> groupsProb = new HashMap<ArrayList<Integer>, Double>();

		int groupSize = 0;
		for(int[][] groups : sizeGroups) {
			groupSize++; // start from 1							
			int numberOfGoupsOfSameSize = groups.length;
			for(int groupIndex = 0; groupIndex<numberOfGoupsOfSameSize; groupIndex++) {
				int[] g = groups[groupIndex];
				ArrayList<String> signLabels = getTermLabels(terms, g);
				int groupFreq = termGropupPartitionFreqTable.get(groupSize).get(signLabels)[groupIndex];
				Double groupProb = (double)groupFreq/(double)totalFreq;
				groupsProb.put(Utility.convertToArrayList(g), groupProb);
			}										
		}

		double denominator = 0;
		for(int[][] p : partitions) {
			double partitionLogProb = 0;
			for(int[] g : p) {
				double logProb = Math.log(groupsProb.get(Utility.convertToArrayList(g)));
				partitionLogProb += logProb; // moltiply all probs of groups in partition
			}
			denominator += Math.exp(partitionLogProb); // add all partitions prob in denominator
		}
		
		result -= Math.log(denominator);
		return result;
		
	}

	private static ArrayList<TSNodeLabel> getRelevantTerms(TSNodeLabel frag) {
		return getRelevantTerms(frag, countGapCateogiesInAssMeasures);
	}

	private static ArrayList<TSNodeLabel> getRelevantTerms(TSNodeLabel frag, boolean withGaps) {
		ArrayList<TSNodeLabel> lex = frag.collectLexicalItems();
		if (withGaps) {
			int s = lex.size();
			ArrayList<TSNodeLabel> terms = frag.collectTerminalItems();
			TSNodeLabel firstLex = lex.get(0);
			TSNodeLabel lastLex = lex.get(s-1);
			Iterator<TSNodeLabel> iter = terms.iterator();
			boolean started = false;
			ArrayList<TSNodeLabel> lexAndGapCat = new ArrayList<TSNodeLabel>();
			while(iter.hasNext()) {
				TSNodeLabel t = iter.next();
				if (t==firstLex) {
					started = true;
					lexAndGapCat.add(t);
					continue;
				}
				if (started) {
					lexAndGapCat.add(t);
					if (t==lastLex)
						break;
				}				
			}	
			return lexAndGapCat;
		}
		return lex;
	}

	private String getTermLabel(TSNodeLabel termItem) {
		if (termItem.isLexical) {
			if (attachPosToWords)
				return termItem.label() + "_" + termItem.parent.label();
			return termItem.label();
		}
		return "|__" + termItem.label() + "__|";
	}

	private ArrayList<String> getTermLabels(ArrayList<TSNodeLabel> terms, int[] g) {
		ArrayList<String> result = new ArrayList<String>(g.length);
		for(int i: g) {
			String label = getTermLabel(terms.get(i));
			result.add(label);
		}
		return result;
	}

	private static int[][][][] sizePartitionSetIndexes = new int[20][][][]; // set_size, partition, group, indexes
	private static int[][][][] sizePartitionGroups = new int[20][][][]; // set_size, group_size-1, group, indexes

	private static int[][][] getPartitionSetIndexes(int size) {
		int[][][] result = sizePartitionSetIndexes[size];  
		if (result!=null)
			return result;
		result = SetPartition.getAllPartitions(size, true, true);
		sizePartitionSetIndexes[size] = result;
		return result;
	}

	private static int[][][] getPartitionGroups(int size) {
		int[][][] result = sizePartitionGroups[size];  
		if (result!=null)
			return result;
		
		HashMap<Integer, HashSet<ArrayList<Integer>>> groups = new HashMap<Integer, HashSet<ArrayList<Integer>>>();
		for(int[][] part : getPartitionSetIndexes(size)) {
			for(int[] grouping : part) {
				ArrayList<Integer> groupArray = Utility.convertToArrayList(grouping);
				Integer groupSize = groupArray.size();				
				Utility.putInHashMap(groups, groupSize, groupArray);
			}
		}
		result = new int[groups.size()][][];		
		for(Entry<Integer, HashSet<ArrayList<Integer>>> e : groups.entrySet()) {
			int i = e.getKey()-1; // index 0 containes groups with one element
			HashSet<ArrayList<Integer>> groupSet = e.getValue();
			int[][] subResult = new int[groupSet.size()][];
			int j=0;
			for(ArrayList<Integer> g : groupSet) {
				subResult[j++] = Utility.convertToArrayList(g);
			}
			result[i] = subResult;
		}
		sizePartitionGroups[size] = result;
		return result;
	}

	public static void main(String[] args) throws Exception {
		//String dir = "/home/sangati/Work/FBK/TSG_MWE/NYT/NYT_500K_All_Skip150/";
		//args = new String[]{dir + "test.frag",dir + "stats/frag_MPI_Entropy_Stats_LogLike/"};
		
		
		String usage = "java TSG_Frags_MWE_Extractor inputFile outputDir addRootLabelsInSignature encodePOSLabelsInSignature fragFreqThreshold SignFragThreshold";
		if (args.length!=6) {
			System.err.println("Wrong number of parameters");
			System.err.println(usage);
			return;
		}
		if (!new File(args[1]).exists() || !new File(args[1]).isDirectory()) {
			System.err.println("OutputDir does not exists or is not a directory");
			System.err.println(usage);
			return;
		}
		addRootLabelsInSignature = Boolean.parseBoolean(args[2]);
		encodePOSLabelsInSignature = Boolean.parseBoolean(args[3]);
		FragFreqThreshold = Integer.parseInt(args[4]);
		SignFragCountThreshold = Integer.parseInt(args[5]);
		new TSG_Frags_MWE_Sign_AM(new File(args[0]), args[1]);
		
		
		/*
		int[][][] part = getPartitionSetIndexes(3);
		System.out.println(Arrays.deepToString(part));
		*/
	}
	
}
