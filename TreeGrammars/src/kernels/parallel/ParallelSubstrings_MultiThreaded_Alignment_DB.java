package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import kernels.algo.CommonSubstring;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import settings.Parameters;
import util.ArgumentReader;
import util.IdentityArrayList;
import util.PrintProgressPercentage;
import util.Utility;
import util.file.FileUtil;

// mapDB version
public class ParallelSubstrings_MultiThreaded_Alignment_DB {

	static final String version = "1.6";
	
	final static DecimalFormat df = new DecimalFormat("#.##");

	static boolean convertToLowerCase = true;
	static boolean printLogVerbose = false;
	static char[] ignoreStartChars = new char[] { '<', '/' };
	static boolean useAlignment = false;

	static boolean cleanTable = false;
	static int cleanTableEvery = (int) 1E8; //1E7 for 1K sentences, 1E8 for 200K sentences
	static double cleanTableThreshold = 0.2;
	//static int cleanTableTypesThreshold = 10;

	static int maxIndexesPerMatch = 100;

	static {
		Arrays.sort(ignoreStartChars);
	}

	ArrayList<String[]> sentencesSouce, sentencesTarget;
	ArrayList<int[][]> sentencesAlignement;
	HTreeMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>>  matchTable;
	File sourceFile, targetFile, alignementFile;
	File logFileVerbose;
	File matchTableDBFile,matchTableOutputFile;
	long totalPairs, matchingPairs;
	int numerOfSentences;
	PrintWriter pwLogVerbose;
	int minSubstringLength, threads;
	PrintProgressPercentage progress;
	ListIterator<String[]> sourceIter1, targetIter1, sourceIter2, targetIter2;
	ListIterator<int[][]> alignementIter1, alignementIter2;
	int[][] alignement1, alignement2;
	int index1, index2;
	String[] source1, source2, target1, target2;
	DB db_handler;
	//int maxPhraseLength;

	public ParallelSubstrings_MultiThreaded_Alignment_DB(File sourceFile,
			File targetFile, String alignementFilePath, String logFileVerbosePath,
			File matchTableOutputFile, int minLength, int threads)
			throws IOException {

		long startTime = System.currentTimeMillis();
		
		File parentDir = matchTableOutputFile.getParentFile();
		if (!matchTableOutputFile.exists())
			parentDir.mkdirs();

		File systemLogFile = Parameters.getLogFile(matchTableOutputFile, "ParallelSubstrings_MultiThreaded_Alignment");
		Parameters.openLogFile(systemLogFile);
		
		if (alignementFilePath.toLowerCase().equals("null"))
			useAlignment = false;
		else {
			this.alignementFile = new File(alignementFilePath);
		}			

		if (logFileVerbosePath.toLowerCase().equals("null"))
			printLogVerbose = false;		
		else {
			this.logFileVerbose = new File(logFileVerbosePath);
			pwLogVerbose = getGzipLogWriter(logFileVerbose);
		}

		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
		this.matchTableOutputFile = matchTableOutputFile;
		this.matchTableDBFile = FileUtil.changeExtension(matchTableOutputFile, ".db");
		this.minSubstringLength = minLength;
		CommonSubstring.minMatchLength = minSubstringLength;
		this.threads = threads;
		
		AlignementInfo.maxIndexesPerMatch = maxIndexesPerMatch;
						
		sentencesSouce = getSentences(sourceFile);
		sentencesTarget = getSentences(targetFile);

		sentencesAlignement = useAlignment ? AlignementFunctions.getAlignements(alignementFile) : null;

		int linesSource = sentencesSouce.size();
		int linesTarget = sentencesTarget.size();
		int linesAlignements = useAlignment ? sentencesAlignement.size() : sentencesSouce.size();
		if (linesSource != linesTarget || linesTarget != linesAlignements) {
			System.err.println("Source|target|alignement files have different number of lines");
			System.err.println(linesSource + " != " + linesTarget + " != " + linesAlignements);
			return;
		}

		numerOfSentences = linesSource;
		totalPairs = ((long) linesSource) * (linesSource - 1) / 2;

		db_handler = DBMaker.newFileDB(matchTableDBFile).transactionDisable().closeOnJvmShutdown().make(); //mmapFileEnable(). //mmapFileEnablePartial()
		matchTable = db_handler.getHashMap("match_table"); 
			//new HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>>();
		
		printParameters();

		progress = new PrintProgressPercentage("Processing pair", 10000, 0, totalPairs);
		sourceIter1 = sentencesSouce.listIterator();
		targetIter1 = sentencesTarget.listIterator();
		source1 = sourceIter1.next();
		target1 = targetIter1.next();
		index1 = 0;
		index2 = 1;
		sourceIter2 = sentencesSouce.listIterator(index2);
		targetIter2 = sentencesTarget.listIterator(index2);
		if (useAlignment) {
			alignementIter1 = sentencesAlignement.listIterator();
			alignement1 = alignementIter1.next();
			alignementIter2 = sentencesAlignement.listIterator(index2);
		}

		ThreadRunner[] threadArray = new ThreadRunner[threads];
		for (int i = 0; i < threads; i++) {
			threadArray[i] = new ThreadRunner();
		}
		submitMultithreadJob(threadArray);

		progress.end();

		Parameters.logStdOutPrintln("Number of total pairs: " + totalPairs);
		float percMatchPairs = (float) (((double) matchingPairs) / totalPairs * 100);
		Parameters.logStdOutPrintln("Number of matching pairs: " + matchingPairs + " (" + df.format(percMatchPairs) + "%)");

		printTable(true);

		if (printLogVerbose) {
			pwLogVerbose.close();
		}

		long endTime = System.currentTimeMillis();
		Parameters.logStdOutPrintln("Took " + ((float) (endTime - startTime)) / 1000 + " s");
		Parameters.closeLogFile();
	}
	
	private void printParameters() {
		
		System.out.println();
		
		Parameters.logStdOutPrintln("ParallelSubstrings_MultiThreaded_Alignment v. " + version);

		Parameters.logStdOutPrintln("");
		Parameters.logStdOutPrintln("Source File: " + sourceFile);
		Parameters.logStdOutPrintln("Target File: " + targetFile);
		Parameters.logStdOutPrintln("Match Table Output File: " + matchTableOutputFile);
		Parameters.logStdOutPrintln("Match Table DB File: " + matchTableDBFile);
		Parameters.logStdOutPrintln("Min length subsequence: " + minSubstringLength);
		Parameters.logStdOutPrintln("Alignement File: " + alignementFile);
		Parameters.logStdOutPrintln("Cleaning tables: " + cleanTable);
		if (cleanTable) {
			Parameters.logStdOutPrintln("\tClean tables every: " + cleanTableEvery);
			Parameters.logStdOutPrintln("\tClean tables threshold: " + cleanTableThreshold);
		}
		Parameters.logStdOutPrintln("Number of lines in each file: " + numerOfSentences);
		Parameters.logStdOutPrintln("Number of total pairs: " + totalPairs);

	}

	private void submitMultithreadJob(ThreadRunner[] threadArray) {

		
		for (int i = 0; i < threads - 1; i++) {
			ThreadRunner t = threadArray[i];
			t.start();
		}
		threadArray[threads - 1].run();

		for (int i = 0; i < threads - 1; i++) {
			try {
				threadArray[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	private void printTable(boolean finalCall) {

		if (finalCall) { //final call
			if (!cleanTable) {
				computeProbabilities();
			}
			else {
				cleanTable(true);
				Parameters.logStdOutPrintln("Printing final table to: " + matchTableOutputFile);
			}			
		}
		else {
			System.out.println("Printing intermediate table to: " + matchTableOutputFile);
		}
					
		
		printTableToFile(matchTable, matchTableOutputFile);
		
	}
	
	boolean computedProb = false;

	private void computeProbabilities() {
		if (computedProb) {
			setAllTotTargetIndexToZero(matchTable);			
		}
		else {
			computedProb = true;
		}
		PrintProgressPercentage progressProb = new PrintProgressPercentage("Computing Probabilities", 1, 0, matchTable.size(), true);
		for(HashMap<IdentityArrayList<String>, AlignementInfo> subTable : matchTable.values()) {
			progressProb.next();
			int totalSourceIndexes = LocalUtility.getTotalIndexUnionCounts(subTable.values());			
			for(Entry<IdentityArrayList<String>, AlignementInfo> f : subTable.entrySet()) {				
				IdentityArrayList<String> target = f.getKey();	
				AlignementInfo ai = f.getValue();
				double probTS = ((double)ai.getNumberOfMatches())/totalSourceIndexes;
				ai.setProbTS(probTS);
				if (ai.totTargetIndexes==0) {
					ArrayList<AlignementInfo> allEqualTarget = LocalUtility.collectAllAlignmentTarget(target, matchTable);
					int totTargetIndexes = LocalUtility.getTotalIndexUnionCounts(allEqualTarget);
					for(AlignementInfo t : allEqualTarget) {
						t.setTotTargetIndexes(totTargetIndexes);
						double probST = ((double)t.getNumberOfMatches())/totTargetIndexes;
						t.setProbST(probST);
					}
				}
			}
		}
		progressProb.end();
	}

	private static void printTableToFile(
			HTreeMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> matchTable,
			File outputFile) {		

		PrintWriter pw = getGzipLogWriter(outputFile);
		for (Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> e : matchTable.entrySet()) {
			IdentityArrayList<String> source = e.getKey();
			Integer length = source.size();
			pw.println(length + ":");
			HashMap<IdentityArrayList<String>, AlignementInfo> subTable = e.getValue();				
			double entropy = Utility.entropy(LocalUtility.getTargetIndexCounts(subTable));
			int totalTargetTypes = subTable.size();
			int totalSourceIndexes = LocalUtility.getTotalIndexUnionCounts(subTable.values());
			pw.println("\t" + source + " " +
					"(TotTargetTypes " + totalTargetTypes + 
					", TotSourceIndexMatch: " + totalSourceIndexes + 
					", Entropy: " + entropy +
					")");
			printOrderedSubTable(subTable, pw, totalSourceIndexes);
			
		}	
		pw.close();
	}

	private static void printOrderedSubTable(
			HashMap<IdentityArrayList<String>, AlignementInfo> subTable,
			PrintWriter pw, int totalSourceIndexes) {

		
		TreeMap<Double, HashMap<IdentityArrayList<String>, AlignementInfo>> scoredTreeMap = 
			new TreeMap<Double, HashMap<IdentityArrayList<String>,AlignementInfo>>();
		
		for(Entry<IdentityArrayList<String>, AlignementInfo> e : subTable.entrySet()) {
			IdentityArrayList<String> match = e.getKey();
			AlignementInfo ai = e.getValue();
			double probTS_ST =  getScore(ai.probTS,ai.probST);
			Utility.putInMapDouble(scoredTreeMap, probTS_ST, match, ai);
		}
		
		int count = 1;
		for (HashMap<IdentityArrayList<String>, AlignementInfo> e : scoredTreeMap.descendingMap().values()) {
			for(Entry<IdentityArrayList<String>, AlignementInfo> f : e.entrySet()) {
				pw.println("\t\t" + count++ + ":" + f.getKey() + "\t" + f.getValue().toString());
			}			
		}
		
		/*
		int count = 1;
		for(Entry<IdentityArrayList<String>, AlignementInfo> e : subTable.entrySet()) {
			IdentityArrayList<String> match = e.getKey();
			AlignementInfo ai = e.getValue();
			//ArrayList<Integer> countValue = targetIndexCounts.get(match);
			//int totMatches = Utility.sum(countValue);
			double relFreq = ((double)ai.getNumberOfMatches())/totalSourceIndexes;
			pw.println("\t\t" + count++ + ":" + match + "\t" + ai.toString(relFreq));
		}
		*/

		
	}

	private static double getScore(double probTS, double probST) {
		return 2d * probTS * probST / (probTS + probST);
	}

	public static PrintWriter getGzipLogWriter(File outputFile) {
		PrintWriter writer = null;
		try {
			GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(
					outputFile));

			writer = new PrintWriter(new OutputStreamWriter(zip, "UTF-8"));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer;
	}

	private static ArrayList<String[]> getSentences(File inputFile)
			throws FileNotFoundException {
		ArrayList<String[]> result = new ArrayList<String[]>();
		Scanner scanner = new Scanner(inputFile, "utf-8");
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (convertToLowerCase)
				line = line.toLowerCase();
			String[] lineWords = getWordsFromLine(line);
			result.add(lineWords);
		}
		return result;
	}

	private static String[] getWordsFromLine(String line) {
		String[] lineWords = line.trim().split("\\s+");
		for (int i = 0; i < lineWords.length; i++) {
			lineWords[i] = lineWords[i].intern();
		}
		return lineWords;
	}

	private static boolean startWithStrangeChar(String s) {
		return Arrays.binarySearch(ignoreStartChars, s.charAt(0)) >= 0;
	}
	
	static int countThreradIteration;

	synchronized private PairInfo getNextPairs() {
		if (++countThreradIteration==10000)
			System.gc();		
		
		if (sourceIter2.hasNext()) {
			progress.next();
			source2 = sourceIter2.next();
			target2 = targetIter2.next();
			if (useAlignment)
				alignement2 = alignementIter2.next();
			PairInfo pi = new PairInfo(source1, source2, target1, target2,
					alignement1, alignement2, index1, index2);
			index2++;
			cleanTable(false);
			return pi;
		}
		if (sourceIter1.hasNext()) {
			source1 = sourceIter1.next();
			target1 = targetIter1.next();
			if (useAlignment)
				alignement1 = alignementIter1.next();
			index1++;
			index2 = index1 + 1;
			sourceIter2 = sentencesSouce.listIterator(index2);
			targetIter2 = sentencesTarget.listIterator(index2);
			if (useAlignment)
				alignementIter2 = sentencesAlignement.listIterator(index2);
			if (sourceIter2.hasNext()) {
				progress.next();
				source2 = sourceIter2.next();
				target2 = targetIter2.next();
				if (useAlignment)
					alignement2 = alignementIter2.next();
				PairInfo pi = new PairInfo(source1, source2, target1, target2,
						alignement1, alignement2, index1, index2);
				index2++;
				cleanTable(false);
				return pi;
			}
		}
		return null;
	}

	private void cleanTable(boolean finalClean) {
		
		if (cleanTable && (finalClean || progress.currentIndex() % cleanTableEvery == 0)) {
			if (!finalClean)
				progress.suspend();
			
			computeProbabilities();

			Parameters.logStdOutPrint("Cleaning table...");

			//HashMap<IdentityArrayList<String>, ArrayList<Integer>> targetIndexCounts = LocalUtility.getTargetIndexCounts(matchTable);			
			
			int[] ST_keys_subKeys_before = LocalUtility.totalKeysSubKeys(matchTable);
			cleanTable(matchTable);
			int[] ST_keys_subKeys_after = LocalUtility.totalKeysSubKeys(matchTable);
			Parameters.logStdOutPrintln("Original keys, subkeys " + Arrays.toString(ST_keys_subKeys_before) + 
					" remaining keys, subkeys: " + Arrays.toString(ST_keys_subKeys_after));

			System.gc();

			if (!finalClean) {
				printTable(false);
				progress.resume();
			}

		}
		
	}

	
	private static void setAllTotTargetIndexToZero(
			HTreeMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> matchTable) {
		for(HashMap<IdentityArrayList<String>, AlignementInfo> subTable : matchTable.values()) {
			for(AlignementInfo ai : subTable.values()) {
				ai.totTargetIndexes = 0;
			}
		}
		
	}

	private void cleanTable(
			HTreeMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> table) {
		
		//HashMap<IdentityArrayList<String>, ArrayList<Integer>> countMappings
		
		/*
		HashMap<IdentityArrayList<String>, int[]> removeItemsTable = new HashMap<IdentityArrayList<String>, int[]>();
		for (HashMap<IdentityArrayList<String>, AlignementInfo> subSubTable : table.values()) {
			for (Entry<IdentityArrayList<String>, AlignementInfo> e : subSubTable.entrySet()) {
				IdentityArrayList<String> subEntry = e.getKey();
				int alignIndexes = e.getValue().numberOfMatches();
				int[] value = Utility.increaseInHashMapIndex(removeItemsTable, subEntry, 1, 0, 2);
				if (value[1] < alignIndexes)
					value[1] = alignIndexes;
			}
		}
		

		Iterator<Entry<IdentityArrayList<String>, int[]>> removeItemsTableIter = removeItemsTable.entrySet().iterator();
		while (removeItemsTableIter.hasNext()) {
			int freq = removeItemsTableIter.next().getValue()[0];
			if (freq < cleanTableThreshold) {
				removeItemsTableIter.remove();
			}
		}
		*/

		Iterator<Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>>> iter = table.entrySet().iterator();
		while (iter.hasNext()) {
			HashMap<IdentityArrayList<String>, AlignementInfo> subTable = iter.next().getValue();
			Iterator<Entry<IdentityArrayList<String>, AlignementInfo>> subIter = subTable.entrySet().iterator();
			while (subIter.hasNext()) {
				Entry<IdentityArrayList<String>, AlignementInfo> subEntry = subIter.next();
				//IdentityArrayList<String> subItem = subEntry.getKey();
				AlignementInfo ai = subEntry.getValue();
				//ArrayList<Integer> countMappingValue = countMappings.get(subItem);				
				//int sum = Utility.sum(countMappingValue);
				//double freq = ai.getNumberOfMatches();
				//double relFreq = freq/sum;
				
				if (ai.probST < cleanTableThreshold || ai.probTS < cleanTableThreshold) {
					subIter.remove();						
				}
			}
			if (subTable.isEmpty())
				iter.remove();
		}

	}
	
	static class AlignementFunctions {

		public static ArrayList<int[][]> getAlignements(File alignementFile) throws FileNotFoundException {
			Scanner scan = new Scanner(alignementFile);
			ArrayList<int[][]> result = new ArrayList<int[][]>();
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String[] split = line.split("\\s");
				int[][] alignment = new int[split.length][];
				result.add(alignment);
				int i = 0;
				for (String s : split) {
					String[] twoIndexes = s.split("-");
					int[] indexes = new int[] { Integer.parseInt(twoIndexes[0]), Integer.parseInt(twoIndexes[1]) };
					alignment[i++] = indexes;
				}
			}
			return result;
		}

		public static void cleanAlignements(HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchSource, 
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchTarget,
				int[][] alignement1, int[][] alignement2) {

			HashMap<IdentityArrayList<String>, HashSet<Integer>> sourceIndexes1 = new HashMap<IdentityArrayList<String>, HashSet<Integer>>();
			HashMap<IdentityArrayList<String>, HashSet<Integer>> sourceIndexes2 = new HashMap<IdentityArrayList<String>, HashSet<Integer>>();
			buildSetIndexes(resultMatchSource, sourceIndexes1, sourceIndexes2);

			HashMap<IdentityArrayList<String>, HashSet<Integer>> targetIndexes1 = new HashMap<IdentityArrayList<String>, HashSet<Integer>>();
			HashMap<IdentityArrayList<String>, HashSet<Integer>> targetIndexes2 = new HashMap<IdentityArrayList<String>, HashSet<Integer>>();
			buildSetIndexes(resultMatchTarget, targetIndexes1, targetIndexes2);

			HashMap<Integer, HashSet<Integer>> alignTable1 = buildAlignemntTable(alignement1);
			HashMap<Integer, HashSet<Integer>> alignTable2 = buildAlignemntTable(alignement2);

			cleanAlignements(sourceIndexes1, targetIndexes1, alignTable1, resultMatchSource, resultMatchTarget);
			cleanAlignements(sourceIndexes2, targetIndexes2, alignTable2, resultMatchSource, resultMatchTarget);

		}

		private static void cleanAlignements(HashMap<IdentityArrayList<String>, HashSet<Integer>> sourceStartIndexes, 
				HashMap<IdentityArrayList<String>, HashSet<Integer>> targetStartIndexes,
				HashMap<Integer, HashSet<Integer>> alignTable, HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchSource,
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchTarget) {
		
			Iterator<Entry<IdentityArrayList<String>, HashSet<Integer>>> iterSource = sourceStartIndexes.entrySet().iterator();
			while (iterSource.hasNext()) {
				Entry<IdentityArrayList<String>, HashSet<Integer>> iS = iterSource.next();
				IdentityArrayList<String> keyS = iS.getKey();
				int lengthS = keyS.size();
				HashSet<Integer> setS = iS.getValue();
				Iterator<Entry<IdentityArrayList<String>, HashSet<Integer>>> iterTarget = targetStartIndexes.entrySet().iterator();
				while (iterTarget.hasNext()) {
					Entry<IdentityArrayList<String>, HashSet<Integer>> iT = iterTarget.next();
					IdentityArrayList<String> keyT = iT.getKey();
					int lengthT = keyT.size();
					HashSet<Integer> setT = iT.getValue();
					if (alignementMatch(setS, lengthS, setT, lengthT, alignTable)) {
						iterSource.remove();
						iterTarget.remove();
						resultMatchSource.remove(keyS);
						resultMatchTarget.remove(keyT);
						break;
					}
				}
			}
		}

		private static HashMap<Integer, HashSet<Integer>> buildAlignemntTable(int[][] alignement) {
			HashMap<Integer, HashSet<Integer>> result = new HashMap<Integer, HashSet<Integer>>();
			for (int[] p : alignement) {
				Utility.putInHashMap(result, p[0], p[1]);
			}
			return result;
		}

		private static boolean alignementMatch(HashSet<Integer> setS, int lengthS, HashSet<Integer> setT, 
				int lengthT, HashMap<Integer, HashSet<Integer>> alignTable) {

			for (int is : setS) {
				for (int it : setT) {
					if (alignementMatch(is, is + lengthS - 1, it, it + lengthT - 1, alignTable))
						return true;
				}
			}
			return false;
		}

		private static boolean alignementMatch(int indexSourceStart, int indexSourceEnd, 
				int indexTargetStart, int indexTargetEnd, HashMap<Integer, HashSet<Integer>> alignTable) {

			for (int i = indexSourceStart; i <= indexSourceEnd; i++) {
				if (!valueInRange(alignTable, i, indexTargetStart, indexTargetEnd)) {
					return false;
				}
			}
			return true;
		}

		private static boolean valueInRange(HashMap<Integer, HashSet<Integer>> alignTable, 
				int indexSource, int indexTargetStart, int indexTargetEnd) {

			HashSet<Integer> set = alignTable.get(indexSource);
			if (set == null)
				return false;
			boolean inRange = false;
			for (int i : set) {
				if (i >= indexTargetStart && i <= indexTargetEnd) {
					inRange = true;
					break;
				}
			}
			return inRange;
		}
		
	}
	

	static class LocalUtility {
		
		public static ArrayList<AlignementInfo> collectAllAlignmentTarget(IdentityArrayList<String> target, 
				HTreeMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> matchTable) {
			
			ArrayList<AlignementInfo> result = new ArrayList<AlignementInfo>();
			for(HashMap<IdentityArrayList<String>, AlignementInfo> subTable : matchTable.values()) {
				AlignementInfo ai = subTable.get(target);
				if (ai!=null) {
					result.add(ai);
				}
			}
			return result;
			
		}

		public static int[] totalTypesTokens(
				TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultMatch) {
			int tokens = 0;
			int types = 0;
			for (HashMap<IdentityArrayList<String>, ArrayList<int[]>> subTable : resultMatch
					.values()) {
				types += subTable.size();
				for (ArrayList<int[]> l : subTable.values()) {
					tokens += l.size();
				}
			}
			return new int[] { types, tokens };
		}
	
		public static int getTotalIndexUnionCounts(Collection<AlignementInfo> otherTargets) {
			BitSet totalIndexes = new BitSet();
			for(AlignementInfo a : otherTargets) {
				totalIndexes.or(a.indexes);
			}
			return totalIndexes.cardinality();
		}

		public static int[] totalTypesTokens(
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatch) {
			int tokens = 0;
			int types = resultMatch.size();
			for (ArrayList<int[]> subTable : resultMatch.values()) {
				tokens += subTable.size();
			}
			return new int[] { types, tokens };
		}
	
		public static int[] totalTypesTokensLengthOne(
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatch) {
			int tokens = 0;
			int types = 0;
			for (Entry<IdentityArrayList<String>, ArrayList<int[]>> e : resultMatch
					.entrySet()) {
				if (e.getKey().size() == 1) {
					types++;
					tokens += e.getValue().size();
				}
			}
			return new int[] { types, tokens };
		}
	
		public static int[] totalKeysSubKeys(
				HTreeMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> matchTable) {
			int totalKeys = matchTable.size();
			int totalSubKeys = 0;
			for (HashMap<IdentityArrayList<String>, AlignementInfo> subTable : matchTable
					.values()) {
				totalSubKeys += subTable.size();
			}
			return new int[] { totalKeys, totalSubKeys };
		}
	
		public static <T> HashMap<IdentityArrayList<String>, T> flatTable(TreeMap<Integer, HashMap<IdentityArrayList<String>, T>> resultMatch) {
			HashMap<IdentityArrayList<String>, T> result = new HashMap<IdentityArrayList<String>, T>();
			for (HashMap<IdentityArrayList<String>, T> subTable : resultMatch.values()) {
				result.putAll(subTable);
			}
			return result;
		}
	
		public static <T> TreeMap<Integer, HashMap<IdentityArrayList<String>, T>> unflatTable(
				HashMap<IdentityArrayList<String>, T> table) {
	
			TreeMap<Integer, HashMap<IdentityArrayList<String>, T>> result = new TreeMap<Integer, HashMap<IdentityArrayList<String>, T>>();
			for (Entry<IdentityArrayList<String>, T> e : table.entrySet()) {
				IdentityArrayList<String> key = e.getKey();
				Integer length = key.size();
				HashMap<IdentityArrayList<String>, T> subResult = result
						.get(length);
				if (subResult == null) {
					subResult = new HashMap<IdentityArrayList<String>, T>();
					result.put(length, subResult);
				}
				subResult.put(key, e.getValue());
			}
			return result;
		}

		public static HashMap<IdentityArrayList<String>, ArrayList<Integer>> getTargetIndexCountsTotal(
				HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, AlignementInfo>> table) {
		
			HashMap<IdentityArrayList<String>, ArrayList<Integer>> result = new HashMap<IdentityArrayList<String>, ArrayList<Integer>>();
		
			for (HashMap<IdentityArrayList<String>, AlignementInfo> subTable : table.values()) {
				for (Entry<IdentityArrayList<String>, AlignementInfo> e : subTable.entrySet()) {
					IdentityArrayList<String> match = e.getKey();
					ArrayList<Integer> resultValue = result.get(match);
					if (resultValue == null) {
						resultValue = new ArrayList<Integer>();
						result.put(match, resultValue);
					}
					resultValue.add(e.getValue().getNumberOfMatches());
				}
			}
		
			return result;
		}
		
		public static ArrayList<Integer> getTargetIndexCounts(HashMap<IdentityArrayList<String>, AlignementInfo> subTable) {
			ArrayList<Integer> result = new ArrayList<Integer>();
			for (AlignementInfo ai : subTable.values()) {
				result.add(ai.getNumberOfMatches());
			}
			return result;
		}
		
	}

	static protected class PairInfo {
		String[] source1, source2, target1, target2;
		int[][] alignement1, alignement2;
		int index1, index2;

		public PairInfo(String[] source1, String[] source2, String[] target1,
				String[] target2, int[][] alignement1, int[][] alignement2,
				int index1, int index2) {
			this.source1 = source1;
			this.source2 = source2;
			this.target1 = target1;
			this.target2 = target2;
			this.alignement1 = alignement1;
			this.alignement2 = alignement2;
			this.index1 = index1;
			this.index2 = index2;
		}
	}

	

	synchronized private void addToMatchTable(
			IdentityArrayList<String> sourceMatch,
			IdentityArrayList<String> targetMatch, int index1,
			int index2, HashMap<IdentityArrayList<String>, AlignementInfo> tableMatch) {
		
		/*
		int sizeSource = sourceMatch.size();
		int sizeTarget = targetMatch.size();
		int max = Math.max(sizeSource,sizeTarget);
		if (max > maxPhraseLength)
			maxPhraseLength = max;
		*/
		
		//HashMap<IdentityArrayList<String>, AlignementInfo> tableMatch = matchTable.get(sourceMatch);
		if (tableMatch == null) {
			tableMatch = new HashMap<IdentityArrayList<String>, AlignementInfo>();
			matchTable.put(sourceMatch, tableMatch);
		}
		AlignementInfo entryMatch = tableMatch.get(targetMatch);
		if (entryMatch == null) {
			entryMatch = new AlignementInfo(index1, index2);
			tableMatch.put(targetMatch, entryMatch);
		} else
			entryMatch.addIndexes(index1, index2);
	}

	synchronized private void logMatch(
			boolean sourceMatch, boolean targetMatch,
			int index1, int index2,
			String[] source1, String[] source2,
			String[] target1, String[] target2,
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchSource,
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchTarget) {

		pwLogVerbose.println("PAIRS " + index1 + "-" + index2);

		if (sourceMatch && targetMatch)
			pwLogVerbose.println("MATCHES BOTH SIDES");
		else if (sourceMatch)
			pwLogVerbose.println("MATCHES ONLY IN SOURCE");
		else
			pwLogVerbose.println("MATCHES ONLY IN TARGET");

		if (sourceMatch) {
			pwLogVerbose.println("Source1:");
			pwLogVerbose.println("\t" + Arrays.toString(source1));
			pwLogVerbose.println("Source2:");
			pwLogVerbose.println("\t" + Arrays.toString(source2));
			pwLogVerbose.println();
			printMatchToLog(resultMatchSource);
		}

		if (targetMatch) {
			pwLogVerbose.println("Target1:");
			pwLogVerbose.println("\t" + Arrays.toString(target1));
			pwLogVerbose.println("Target2:");
			pwLogVerbose.println("\t" + Arrays.toString(target2));
			pwLogVerbose.println();
			printMatchToLog(resultMatchTarget);
		}

		pwLogVerbose.println("-------------------------------------------------------");

	}

	private void printMatchToLog(
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultTable) {
		printMatchToOutput(pwLogVerbose, resultTable);
	}

	private static void printMatchToOutput(PrintWriter output,
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultTable) {
		for (Entry<IdentityArrayList<String>, ArrayList<int[]>> e : resultTable
				.entrySet()) {
			output.println("\t\t" + e.getKey().toString() + " @ "
					+ Arrays.deepToString(e.getValue().toArray(new int[][] {})));
		}
	}

	static private void cleanExactMatches(
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchSource,
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchTarget) {

		Iterator<Entry<IdentityArrayList<String>, ArrayList<int[]>>> iter1 = resultMatchSource.entrySet().iterator();
		while (iter1.hasNext()) {
			Entry<IdentityArrayList<String>, ArrayList<int[]>> f = iter1.next();
			IdentityArrayList<String> match1 = f.getKey();
			if (resultMatchTarget.keySet().remove(match1)) {
				iter1.remove();
			}
		}
	}

	private static boolean hasOnlyOneWords(Set<IdentityArrayList<String>> keySet) {
		for (IdentityArrayList<String> a : keySet) {
			if (a.size() > 1)
				return false;
		}
		return true;
	}

	private static void buildSetIndexes(
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> table,
			HashMap<IdentityArrayList<String>, HashSet<Integer>> setIndexes1,
			HashMap<IdentityArrayList<String>, HashSet<Integer>> setIndexes2) {
	
		for (Entry<IdentityArrayList<String>, ArrayList<int[]>> e : table.entrySet()) {
			IdentityArrayList<String> substring = e.getKey();
			HashSet<Integer> set1 = new HashSet<Integer>();
			HashSet<Integer> set2 = new HashSet<Integer>();
			setIndexes1.put(substring, set1);
			setIndexes2.put(substring, set2);
			for (int[] i : e.getValue()) {
				set1.add(i[0]);
				set2.add(i[1]);
			}

		}
	
	}
		

	protected class ThreadRunner extends Thread {
		
		HashMap<IdentityArrayList<String>, AlignementInfo> subTable;
		HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchSource, resultMatchTarget;

		public ThreadRunner() {

		}

		public void run() {
			PairInfo pi = null;
			while ((pi = getNextPairs()) != null) {				
				computeMatch(pi.index1, pi.index2, pi.source1, pi.source2,
						pi.target1, pi.target2, pi.alignement1, pi.alignement2);
			}

		}

		private void computeMatch(int index1, int index2, String[] source1,
				String[] source2, String[] target1, String[] target2,
				int[][] alignement1, int[][] alignement2) {

			if (source1[0].isEmpty() || source2[0].isEmpty())
				return;

			if (startWithStrangeChar(source1[0]) || startWithStrangeChar(source2[0]))
				return;

			resultMatchSource = CommonSubstring.getAllMaxCommonSubstringsIdentityIndexes(source1, source2);
			resultMatchTarget = CommonSubstring.getAllMaxCommonSubstringsIdentityIndexes(target1, target2);

			checkAllOneLengthSubstrings(resultMatchSource, resultMatchTarget);
			cleanExactMatches(resultMatchSource, resultMatchTarget);

			if (useAlignment)
				AlignementFunctions.cleanAlignements(resultMatchSource, resultMatchTarget, alignement1, alignement2);

			/*
			 * if (resultMatchSource.isEmpty() && resultMatchTarget.isEmpty()) {
			 * //no match return; }
			 * 
			 * if (!resultMatchSource.isEmpty() && resultMatchTarget.isEmpty())
			 * { //match source logMatch(true, false, index1, index2, source1,
			 * source2, target1, target2, resultMatchSource, resultMatchTarget);
			 * return; }
			 * 
			 * if (resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty())
			 * { //match target logMatch(false, true, index1, index2, source1,
			 * source2, target1, target2, resultMatchSource, resultMatchTarget);
			 * return; }
			 */

			// last case: !resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty()
			if (!resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty()) {
				if (printLogVerbose) {
					logMatch(true, true, index1, index2, source1, source2,
							target1, target2, resultMatchSource,
							resultMatchTarget);
				}
				addMatchesToFinalTable(index1, index2, source1, source2,
						target1, target2, resultMatchSource, resultMatchTarget);
			}

		}

		private void checkAllOneLengthSubstrings(
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchSource,
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchTarget) {
			if (minSubstringLength>1)
				return;
			if (hasOnlyOneWords(resultMatchSource.keySet()))
				resultMatchSource.clear();
			if (hasOnlyOneWords(resultMatchTarget.keySet()))
				resultMatchTarget.clear();
		
		}

		@SuppressWarnings("unused")
		private double computeWeight(int typeCombination, int sourceTokens, int targetTokens) {
			double tokensRatio = (sourceTokens > targetTokens) ? ((double) sourceTokens) / targetTokens : ((double) targetTokens) / sourceTokens;
			return 1d / typeCombination * tokensRatio;
		}

		private void addMatchesToFinalTable(int index1, int index2, 
				String[] source1, String[] source2, 
				String[] target1, String[] target2,
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchSourceFlat, 
				HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultMatchTargetFlat) {

			matchingPairs++;

			//int[] sourceTypesTokens = totalTypesTokens(resultMatchSourceFlat);
			//int[] sourceTypesTokensLengthOne = totalTypesTokensLengthOne(resultMatchSourceFlat);
			//int[] targetTypesTokens = totalTypesTokens(resultMatchTargetFlat);
			//int[] targetTypesTokensLengthOne = totalTypesTokensLengthOne(resultMatchTargetFlat);
			//int typeCombination = sourceTypesTokens[0] * targetTypesTokens[0] - sourceTypesTokensLengthOne[0] * targetTypesTokensLengthOne[0];			

			for (Entry<IdentityArrayList<String>, ArrayList<int[]>> e : resultMatchSourceFlat.entrySet()) {
				IdentityArrayList<String> sourceMatch = e.getKey();
				subTable = matchTable.get(sourceMatch);
				//int sourceTokens = e.getValue().size();
				for (Entry<IdentityArrayList<String>, ArrayList<int[]>> f : resultMatchTargetFlat.entrySet()) {
					IdentityArrayList<String> targetMatch = f.getKey();
					if (sourceMatch.size() == 1 && targetMatch.size() == 1)
						continue;
					//int targetTokens = f.getValue().size();
					//double weight = computeWeight(typeCombination, sourceTokens, targetTokens);
					//if (weight > 0) {
						//addToMatchTable(sourceMatch, targetMatch, weight, index1, index2);
					//}
					addToMatchTable(sourceMatch, targetMatch, index1, index2, subTable);
				}
			}
		}

	}

	public static void main(String args[]) throws NumberFormatException, IOException {

		/*
		String dir = "/gardner0/data/Corpora/TED_Parallel/en-it/giza_align/";
		String sourceFile = dir + "train.tags.en-it.tok.selection.head.en";
		String targetFile = dir + "train.tags.en-it.tok.selection.head.it";
		String alignementFile = "null"; //dir + "aligned.grow-diag-final-and.head";
		String logFile = "null";
		String out = dir + "ParalleSubstring_NoAlignment/matchingTableIndexes.en-it.selection.align.head.gz";
		//String out2 = dir + "ParalleSubstring_Alignment/matchingTableIndexes.it-en.selection.align.head.gz";
		args = new String[] { sourceFile, targetFile, alignementFile, logFile, out, "1" };
		*/ 
		
		
		String usage = "java ParallelSubstrings_MultiThreaded_Alignment " +
				"-sourceFile:[path_to_file] " + //0
				"-targetFile:[path_to_file] " + //1
				"-alignementFile:null " +  //2
				"-logFile:null " + //3
				"-matchTableFile:[outputPath] " + //4
				"-minSubstringLength:2 " + //5
				"-threads:1"; //6
		
		if (args.length != 7) {
			System.err.println("Wrong number of arguments!");
			System.err.println(usage);
			return;
		}

		File sourceFile = ArgumentReader.readFileOption(args[0]);
		File targetFile = ArgumentReader.readFileOption(args[1]);
		String alignementFilePath = ArgumentReader.readStringOption(args[2]);
		String logFilePath = ArgumentReader.readStringOption(args[3]);
		File matchTableDBFile = ArgumentReader.readFileOption(args[4]);
		int minSubstringLength = ArgumentReader.readIntOption(args[5]);
		int threads = ArgumentReader.readIntOption(args[6]);
				
		
		new ParallelSubstrings_MultiThreaded_Alignment_DB(sourceFile,
				targetFile, alignementFilePath, logFilePath,
				matchTableDBFile, minSubstringLength, threads);
		

	}

}
