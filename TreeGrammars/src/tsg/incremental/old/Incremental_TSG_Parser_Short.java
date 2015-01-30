package tsg.incremental.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.TermLabelShort;
import tsg.corpora.Wsj;
import tsg.incremental.old.MultiFringeShort.TerminalMatrix;
import tsg.incremental.old.MultiFringeShort.TerminalsMultiSetTable;
import util.PrintProgress;
import util.Timer;
import util.Utility;
import util.file.FileUtil;

public class Incremental_TSG_Parser_Short extends Thread {
		
	static final short TOPnode = TermLabelShort.getTermLabelId("TOP", false);
	static final String scratchDebugPath = "/disk/scratch/fsangati/Debug/";
	
	static boolean debug = true;	
	static int treeLenghtLimit = 10;		
	static long maxMsecPerWord = 60*1000;
	static long threadCheckingTime = 20000;
	static int threads = 1;
	
	static boolean printWordFringes = false;
	static boolean tryToRemoveDuplicates = true;
	static int oneTestSentenceIndex = -1;
	static boolean noInterruption = false;
	static boolean removeFringesNonCompatibleWithSentence = false;
	
	File logFile;	
	FragmentExtractor FE;
	String outputPath;
	ArrayList<TSNodeLabel> testTB;
	PrintProgress progress;
	int parsedTrees, reachedTopTrees, interruptedTrees;
	Iterator<TSNodeLabel> testTBIterator;
	int treebankIteratorIndex;
	boolean smoothing; 
	HashMap<Short, HashSet<Short>> posLexTable, lexPosTable;
		
	//used as sub-down-second fringes in all bust first word in sentence
	//and as scan/sub-down-first/sub-up-first fringes in first word in sentence
	HashMap<Short, 
		HashMap<Short, MultiFringeShort>> 
			firstLexFringes = 
				new HashMap<Short, HashMap<Short, MultiFringeShort>>(); 
				//indexed on first lex and root
	
	//used for sub-up-second fringes in all but first word in sentence
	HashMap<Short, 
		HashMap<Short, 
			HashMap<Short, MultiFringeShort>>> 			
				firstSubFringes = 
					new HashMap<Short, HashMap<Short, HashMap<Short, MultiFringeShort>>>(); 
					//indexed on first lex and first terminal (sub-site) and root

	HashMap<Short, 
		HashMap<Short, MultiFringeShort>> 
			firstLexFringesSmoothing = 
				new HashMap<Short, HashMap<Short, MultiFringeShort>>(); 
	HashMap<Short, 
		HashMap<Short, 
			HashMap<Short, MultiFringeShort>>> 			
				firstSubFringesSmoothing = 
					new HashMap<Short, HashMap<Short, HashMap<Short, MultiFringeShort>>>(); 
	
	
	
	public Incremental_TSG_Parser_Short(FragmentExtractor FE, 
			File testFile, String outputPath) throws Exception {
		this(FE, Wsj.getTreebank(testFile), outputPath);		
	}
	
	public Incremental_TSG_Parser_Short (FragmentExtractor FE, 
			ArrayList<TSNodeLabel> testTB, String outputPath) throws Exception {
		this.FE = FE;
		this.testTB = testTB;
		this.outputPath = outputPath;
		this.smoothing = FragmentExtractor.smoothingFromFrags || FragmentExtractor.smoothingFromMinSet;
		
		int smoothThresh = FragmentExtractor.minFragFreqForSmoothingFromMinSet;
		logFile = new File(outputPath + "log_parsing_" +
				treeLenghtLimit + 
				(smoothing ? "_smoothing(" + smoothThresh + ")" : "") + 
				(oneTestSentenceIndex!=-1 ? "_sent#" + oneTestSentenceIndex : "") +
				".txt");
		Parameters.openLogFile(logFile);
		FE.extractFragments(logFile);
	}
	
	public void run() {									
			printParameters();			
			convertPosSet();
			convertLexFragmentsToFringes();
			convertSmoothedFragmentsToFringes();
			printFringesToFile();
			filterTestTB();			
		
			testTBIterator = testTB.iterator();
			
			if (threads==1 && noInterruption)
				parseIncrementalOneThread();
			else
				parseIncremental();
			
			printFinalSummary();			
		Parameters.closeLogFile();
	}
	
	

	private void printParameters() {
		Parameters.reportLineFlush("\n\n");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("INCREMENTAL TSG SYMBOLIC PARSER");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("");
		Parameters.reportLineFlush("Tree Lenght Limit: " + treeLenghtLimit);
		Parameters.reportLineFlush("Max sec Per Word: " + maxMsecPerWord/1000);
		Parameters.reportLineFlush("Trying to remove duplicates: " + tryToRemoveDuplicates);		
		Parameters.reportLineFlush("Number of threads: " + threads);
		Parameters.reportLineFlush("Max comb. when simplifying: " + MultiFringeShort.maxCombinationsToSimplify);
		Parameters.reportLineFlush("Remove frags not compatible with sentence: " + removeFringesNonCompatibleWithSentence);
		
	}
	
	private void printFinalSummary() {
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
		Parameters.reportLineFlush("Number of finished analysis: " + (testTB.size()-interruptedTrees));
		Parameters.reportLineFlush("Number of interrupted analysis: " + interruptedTrees);
		Parameters.reportLineFlush("Number of successfully parsed trees: " + parsedTrees);		
		Parameters.reportLineFlush("... of which reached top: " + reachedTopTrees);
	}

	private static HashMap<Short, HashSet<Short>> convertTable(
			HashMap<Label, HashSet<Label>> table, boolean keyIsLex, boolean valuesAreLex) {
		
		HashMap<Short, HashSet<Short>> result =
			new HashMap<Short, HashSet<Short>>(); 
		
		for(Entry<Label, HashSet<Label>>  e : table.entrySet()) {
			short key = TermLabelShort.getTermLabelId(e.getKey(), keyIsLex);
			HashSet<Short> value = new HashSet<Short>();
			for(Label l : e.getValue()) {
				value.add(TermLabelShort.getTermLabelId(l, valuesAreLex));
			}			
			result.put(key,value);
		}
		
		return result;
	}
	
	private void convertPosSet() {
		posLexTable = convertTable(FE.posLexFinal, false, true);
		lexPosTable = convertTable(FE.lexPosFinal, true, false);
		MultiFringeShort.posSet = posLexTable.keySet();
		MultiFringeShort.posSetLexTable = posLexTable;
	}

	private void convertLexFragmentsToFringes() {
		
		Parameters.reportLineFlush("Converting lex fragments into fringes...");
		
		int countFirstLexFrags = 0;
		int countFirstLexFringes = 0;
		int countFirstSubFrags = 0;
		int countFirstSubFringes = 0;
		
		for(Entry<Label, HashSet<TSNodeLabel>> e : FE.lexFragsTableFirstLex.entrySet()) {
			short firstLexLabel = TermLabelShort.getTermLabelId(e.getKey(),true);
			HashSet<TSNodeLabel> fragSet = e.getValue();
			countFirstLexFrags += fragSet.size();
			for(TSNodeLabel firstLexFrag : fragSet) {
				if (addFirstLexFringe(firstLexFringes, firstLexLabel, firstLexFrag))
					countFirstLexFringes++;
			}	
		}
		
		for(Entry<Label, HashSet<TSNodeLabel>> e : FE.lexFragsTableFirstSub.entrySet()) {			
			short lexLabel = TermLabelShort.getTermLabelId(e.getKey(), true);
			HashSet<TSNodeLabel> fragSet = e.getValue();
			countFirstSubFrags += fragSet.size();
			for(TSNodeLabel frag : fragSet) {
				if (addFirstSubFringe(firstSubFringes, lexLabel,frag))
					countFirstSubFringes++;
			}
		}
		
		Parameters.reportLineFlush("Total number of lex fragments: " + (countFirstLexFrags+countFirstSubFrags));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexFrags);
		Parameters.reportLineFlush("\tSub first: " + countFirstSubFrags);
		Parameters.reportLineFlush("Total number of lex fringes: " + (countFirstLexFringes+countFirstSubFringes));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexFringes);
		Parameters.reportLineFlush("\tSub first: " + countFirstSubFringes);
	}

	private void convertSmoothedFragmentsToFringes() {
		
		Parameters.reportLineFlush("Converting smoothed fragments into fringes...");
		
		int[] countFirstLexSubFrags = new int[2];
		int[] countFirstLexSubFringes = new int[2];
		
		for(Entry<Label, HashSet<TSNodeLabel>> e : FE.posFragSmoothingMerged.entrySet()) {
			Label firstShort = e.getKey();
			short firstPosLabel = TermLabelShort.getTermLabelId(firstShort,false);
			HashSet<TSNodeLabel> fragSet = e.getValue();
			for(TSNodeLabel frag : fragSet) {
				frag = frag.clone();
				TSNodeLabel[] firstSecondTermNode = new TSNodeLabel[]{
						frag.getTerminalItemsAtPosition(0),
						frag.getTerminalItemsAtPosition(1)
				};
				int lexIndex = firstSecondTermNode[0].isLexical ? 0 : 1;
				firstSecondTermNode[lexIndex].parent.daughters = null;
				countFirstLexSubFrags[lexIndex]++;
				if (addFirstLexFringe(firstLexFringesSmoothing, firstPosLabel, frag))
					countFirstLexSubFringes[lexIndex]++;
			}
		}
		
		Parameters.reportLineFlush("Total number of smoothed fragments: " + (Utility.sum(countFirstLexSubFrags)));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexSubFrags[0]);
		Parameters.reportLineFlush("\tSub first: " + countFirstLexSubFrags[1]);
		Parameters.reportLineFlush("Total number of smoothed fringes: " + (Utility.sum(countFirstLexSubFringes)));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexSubFringes[0]);
		Parameters.reportLineFlush("\tSub first: " + countFirstLexSubFringes[1]);
		
	}

	private static boolean addFirstLexFringe(
			HashMap<Short, HashMap<Short, MultiFringeShort>> table,
			Short firstLexLabel, TSNodeLabel frag) {					
		
		short rootLabel = TermLabelShort.getTermLabelId(frag);
		
		LinkedList<Short> Shorts = new LinkedList<Short>();		
		
		for(TSNodeLabel n : frag.collectTerminalItems()) {
			Shorts.add(TermLabelShort.getTermLabelId(n));
		}
		
		Shorts.removeFirst();
		
		MultiFringeShort MultiFringeShort = null;
		HashMap<Short, MultiFringeShort> lexFringe = table.get(firstLexLabel);
		
		if (lexFringe==null) {
			lexFringe = new HashMap<Short, MultiFringeShort>();
			table.put(firstLexLabel,lexFringe);						
		}
		else {
			MultiFringeShort = lexFringe.get(rootLabel);
		}
		if (MultiFringeShort == null) {
			MultiFringeShort = new MultiFringeShort(rootLabel, firstLexLabel);
			lexFringe.put(rootLabel, MultiFringeShort);
		}
		return MultiFringeShort.addFringe(Shorts);
	}

	private static boolean addFirstSubFringe(
			HashMap<Short, HashMap<Short, HashMap<Short, MultiFringeShort>>> table,
			short lexLabel, TSNodeLabel frag) {
		
		short rootLabel = TermLabelShort.getTermLabelId(frag);
		LinkedList<Short> Shorts = new LinkedList<Short>();
		for(TSNodeLabel n : frag.collectTerminalItems()) {
			Shorts.add(TermLabelShort.getTermLabelId(n));
		}
		short firstShort = Shorts.removeFirst();		
		
		HashMap<Short, MultiFringeShort> MultiFringeShortTable = null;
		HashMap<Short, HashMap<Short, MultiFringeShort>> 
			lexFringe = table.get(lexLabel);
		
		if (lexFringe==null) {
			lexFringe = new HashMap<Short, HashMap<Short, MultiFringeShort>>();
			table.put(lexLabel,lexFringe);						
		}
		else {
			MultiFringeShortTable = lexFringe.get(firstShort);
		}
		
		MultiFringeShort MultiFringeShort = null;
		
		if (MultiFringeShortTable==null) {
			MultiFringeShortTable = new HashMap<Short, MultiFringeShort>();
			lexFringe.put(firstShort, MultiFringeShortTable);
		}
		else {
			MultiFringeShort = MultiFringeShortTable.get(rootLabel);
		}		
		if (MultiFringeShort == null) {
			MultiFringeShort = new MultiFringeShort(rootLabel, firstShort);
			MultiFringeShortTable.put(rootLabel, MultiFringeShort);
		}
		return MultiFringeShort.addFringe(Shorts);
	}

	private void printFringesToFile() {
		// TODO Auto-generated method stub
		
	}

	private void filterTestTB() {
		if (treeLenghtLimit>0) {
			testTBIterator = testTB.iterator();
			while(testTBIterator.hasNext()) {
				TSNodeLabel t = testTBIterator.next();
				if (t.countLexicalNodes()>treeLenghtLimit)
					testTBIterator.remove();
			}			
		}
		if (oneTestSentenceIndex!=-1) {
			Parameters.reportLineFlush("Restricting only to test tree # " + oneTestSentenceIndex);
			TSNodeLabel t = testTB.get(oneTestSentenceIndex);
			testTB.clear();
			testTB.add(t);
		}
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
	}
	
	
	private static void printFirstLexFirstSubFrags(
			List<String> words, 
			Vector<HashMap<Short, MultiFringeShort>> firstLexFringesWords,
			Vector<HashMap<Short, HashMap<Short, MultiFringeShort>>> firstSubFringesWords) {
		int wIndex = -1;
		for(String w : words) {
			wIndex++;
			HashMap<Short, MultiFringeShort>  rootMultiFringeShort = firstLexFringesWords.get(wIndex);
			if (rootMultiFringeShort!=null) {
				File outputFile = new File(scratchDebugPath + wIndex + "_" + w + "_firstLex");
				PrintWriter pw = FileUtil.getPrintWriter(outputFile);
				pw.println("FIRST LEX TABLE [" + w + "]");
				printTable(rootMultiFringeShort,pw, false);
				pw.close();
			}
			HashMap<Short, HashMap<Short, MultiFringeShort>>  firstTermMultiFringeShort = 
				firstSubFringesWords.get(wIndex);
			if (firstTermMultiFringeShort!=null) {
				File outputFile = new File(scratchDebugPath + wIndex + "_" + w + "_secondLex");
				PrintWriter pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SECOND LEX TABLE [" + w + "]");
				printDoubleTable(firstTermMultiFringeShort,pw);
				pw.close();
			}
		}	
		
	}
	
	private static void printTable(HashMap<Short, MultiFringeShort> table, PrintWriter pw, boolean indent) {
		if (table==null) return;
		if (indent) pw.print("\t");
		pw.println("TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		ArrayList<Short> subIndexes = new ArrayList<Short>();
		for(Entry<Short, MultiFringeShort> e : table.entrySet()) {
			MultiFringeShort mf = e.getValue();
			int subTotal = mf.totalFringes();
			subTotalSizes.add(subTotal);
			subSizes.add(1);
			subIndexes.add(e.getKey());
		}
		if (indent) pw.print("\t");
		pw.println("SUB INDEXES: " + TermLabelShort.toString(subIndexes));
		if (indent) pw.print("\t");
		pw.println("SUB SIZES (multifringes): " + subSizes);
		if (indent) pw.print("\t");
		pw.println("SUB SIZES TOTAL (frings): " + subTotalSizes);
		if (indent) pw.print("\t");
		pw.println("TOTAL MULTIFRINGES: " + Utility.sum(subSizes));
		if (indent) pw.print("\t");
		pw.println("TOTAL FRINGES: " + Utility.sum(subTotalSizes));
		if (indent) pw.print("\t");
		pw.println("---------");
		for(Entry<Short, MultiFringeShort> e : table.entrySet()) {
			if (indent) pw.print("\t");
			pw.println("TABLE ENTRY KEY: " + TermLabelShort.toString(e.getKey()));			
			MultiFringeShort mf = e.getValue();
			if (indent) pw.print("\t");
			pw.println("SIZE (multifringes): " + 1);
			if (indent) pw.print("\t");
			pw.println("TOTAL SIZE (fringes): " + mf.totalFringes());
			pw.println(e.getValue().toString(1));
		}
	}
	
	private static void printTableArray(
			HashMap<Short, ArrayList<MultiFringeShort>> table,
			PrintWriter pw, boolean indent) {
		if (table==null) return;
		if (indent) pw.print("\t");
		pw.println("TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		ArrayList<Short> subIndexes = new ArrayList<Short>();
		for(Entry<Short, ArrayList<MultiFringeShort>> e : table.entrySet()) {
			int size = 0;
			int totalSize = 0;
			for(MultiFringeShort mf : e.getValue()) {
				totalSize += mf.totalFringes();				
				size ++;
			}
			subTotalSizes.add(totalSize);
			subSizes.add(size);
			subIndexes.add(e.getKey());
		}
		if (indent) pw.print("\t");
		pw.println("SUB INDEXES: " + TermLabelShort.toString(subIndexes));
		if (indent) pw.print("\t");
		pw.println("SUB SIZES (multifringes): " + subSizes);
		if (indent) pw.print("\t");
		pw.println("SUB SIZES TOTAL (frings): " + subTotalSizes);
		if (indent) pw.print("\t");
		pw.println("TOTAL MULTIFRINGES: " + Utility.sum(subSizes));
		if (indent) pw.print("\t");
		pw.println("TOTAL FRINGES: " + Utility.sum(subTotalSizes));
		if (indent) pw.print("\t");
		pw.println("---------");
		for(Entry<Short, ArrayList<MultiFringeShort>> e : table.entrySet()) {
			if (indent) pw.print("\t");
			pw.println("TABLE ENTRY KEY: " + TermLabelShort.toString(e.getKey()));
			subSizes = new ArrayList<Integer>();
			subTotalSizes = new ArrayList<Integer>();
			for(MultiFringeShort mf : e.getValue()) {
				subSizes.add(1);
				subTotalSizes.add(mf.totalFringes());
			}
			if (indent) pw.print("\t");
			pw.println("SIZES (multifringes): " + subSizes);			
			if (indent) pw.print("\t");
			pw.println("TOTAL SIZES (fringes): " + subTotalSizes);
			int index = -1;
			for(MultiFringeShort mf : e.getValue()) {
				index++;
				if (indent) pw.print("\t");
				pw.println("SUB-FRINGE #" + index + ":");
				pw.println(mf.toString(1));
			}
		}
		
	}

	private static void printDoubleTable(HashMap<Short, HashMap<Short, MultiFringeShort>> table, 
			PrintWriter pw) {
		if (table==null) return;
		pw.println("DOUBLE TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();		
		ArrayList<Short> subIndexes = new ArrayList<Short>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		for(Entry<Short, HashMap<Short, MultiFringeShort>> e : table.entrySet()) {
			subSizes.add(e.getValue().size());
			subIndexes.add(e.getKey());
			subTotalSizes.add(countTotalFringes(e.getValue()));
		}
		pw.println("SUB INDEXES: " + TermLabelShort.toString(subIndexes));
		pw.println("SUB SIZES (multifringes): " + subSizes);
		pw.println("SUB SIZES TOTAL (frings): " + subTotalSizes);
		pw.println("TOTAL MULTIFRINGES: " + Utility.sum(subSizes));
		pw.println("TOTAL FRINGES: " + Utility.sum(subTotalSizes));
		pw.println("------------------------------\n");
		for(Entry<Short, HashMap<Short, MultiFringeShort>> e : table.entrySet()) {
			pw.println("TABLE FIRST ENTRY KEY: " + TermLabelShort.toString(e.getKey()));;
			printTable(e.getValue(),pw, true);			
		}
	}	
	
	private static void printDoubleTableArray(
			HashMap<Short, HashMap<Short, ArrayList<MultiFringeShort>>> table, 
			PrintWriter pw) {
		if (table==null) return;
		pw.println("DOUBLE TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();		
		ArrayList<Short> subIndexes = new ArrayList<Short>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		for(Entry<Short, HashMap<Short, ArrayList<MultiFringeShort>>> e : table.entrySet()) {
			int[] sizes = countTotalFringesArray(e.getValue());
			subSizes.add(sizes[0]);
			subIndexes.add(e.getKey());
			subTotalSizes.add(sizes[1]);
		}
		pw.println("SUB INDEXES: " + TermLabelShort.toString(subIndexes));
		pw.println("SUB SIZES (multifringes): " + subSizes);
		pw.println("SUB SIZES TOTAL (frings): " + subTotalSizes);
		pw.println("TOTAL MULTIFRINGES: " + Utility.sum(subSizes));
		pw.println("TOTAL FRINGES: " + Utility.sum(subTotalSizes));
		pw.println("------------------------------\n");
		for(Entry<Short, HashMap<Short, ArrayList<MultiFringeShort>>> e : table.entrySet()) {
			pw.println("TABLE FIRST ENTRY KEY: " + TermLabelShort.toString(e.getKey()));;
			printTableArray(e.getValue(),pw, true);			
		}
	}	
	

	
	private static int countTotalFringes(HashMap<Short, MultiFringeShort> table) {		
		int result = 0;
		for(Entry<Short, MultiFringeShort> e : table.entrySet()) {
			result += e.getValue().totalFringes();
		}
		return result;
	}
	
	private static int[] countTotalFringesArray(HashMap<Short, ArrayList<MultiFringeShort>> table) {		
		int MultiFringeShorts = 0;
		int totalFringes = 0;
		for(ArrayList<MultiFringeShort> a : table.values()) {
			MultiFringeShorts += a.size();
			totalFringes += countTotalFringes(a);
		}
		return new int[]{MultiFringeShorts,totalFringes};
	}
	
	private static int countTotalFringes(ArrayList<MultiFringeShort> array) {
		int result = 0;
		for(MultiFringeShort mf : array) {
			result += mf.totalFringes();
		}
		return result;
	}
	

	private static boolean addInTableSimple(
			HashMap<Short, MultiFringeShort> table,
			short rootLabel, MultiFringeShort smf) {
		
		MultiFringeShort MultiFringeShort = table.get(rootLabel);
		if (MultiFringeShort==null) {
			table.put(rootLabel, smf);
			return true;
		}
		return false;		
	}

	private static boolean addInTableSimpleClone(HashMap<Short, MultiFringeShort> table,
			short firstKey, short rootLabel, short firstShort, 
			short nextInLine, TerminalMatrix termMatrix) {
		
		MultiFringeShort MultiFringeShort = table.get(firstKey);				
		if (MultiFringeShort==null) {
			MultiFringeShort = new MultiFringeShort(rootLabel, firstShort);
			MultiFringeShort.firstTerminalMultiSet.put(nextInLine, clone(termMatrix));				
			table.put(firstKey, MultiFringeShort);
			return true;
		}
		else {
			TerminalsMultiSetTable firstTermTable = MultiFringeShort.firstTerminalMultiSet;
			TerminalMatrix present = firstTermTable.get(nextInLine);
			if (present==null) {
				firstTermTable.put(nextInLine, clone(termMatrix));
				return true;
			}
			return present.addAll(termMatrix);
		}
	}

	
	
	private static boolean addInDoubleTableSimpleClone(
			HashMap<Short, HashMap<Short, MultiFringeShort>> table,
			short firstKey, short rootLabel, short firstShort, 
			short nextInLine, TerminalMatrix termMatrix) {
		
		HashMap<Short, MultiFringeShort> MultiFringeShortTable = table.get(firstKey);				
		if (MultiFringeShortTable==null) {
			MultiFringeShortTable = new HashMap<Short, MultiFringeShort>();
			table.put(firstKey, MultiFringeShortTable);						
		}
		return addInTableSimpleClone(MultiFringeShortTable, rootLabel, rootLabel, firstShort, 
				nextInLine, termMatrix);
	}


	
	
	private static void addInTableArray(
			HashMap<Short, ArrayList<MultiFringeShort>> table,
			short firstKey, MultiFringeShort MultiFringeShort) {
		
		ArrayList<MultiFringeShort> MultiFringeShortArray = table.get(firstKey);
		if (MultiFringeShortArray==null) {
			MultiFringeShortArray = new ArrayList<MultiFringeShort>();
			table.put(firstKey, MultiFringeShortArray);
		}
		MultiFringeShortArray.add(MultiFringeShort);
	}
	
	private static void addInTableArrayCheckDuplicates(
			HashMap<Short, ArrayList<MultiFringeShort>> table,
			short firstKey, MultiFringeShort MultiFringeShort) {
		
		ArrayList<MultiFringeShort> MultiFringeShortArray = table.get(firstKey);
		if (MultiFringeShortArray==null) {
			MultiFringeShortArray = new ArrayList<MultiFringeShort>();
			table.put(firstKey, MultiFringeShortArray);
			MultiFringeShortArray.add(MultiFringeShort);
		}
		else {
			boolean present = false;
			Iterator<MultiFringeShort> iter = MultiFringeShortArray.iterator();
			while(iter.hasNext()) {
				MultiFringeShort mfPresent = iter.next();
				//int index = MultiFringeShort.subFringeEqualSize(MultiFringeShort, mfPresent);
				int index = MultiFringeShort.subFringe(MultiFringeShort, mfPresent);
				if (index==0) //not compatible
					continue;
				if (index==1) { // MultiFringeShort contains mfPresent
					iter.remove();
					break;
				}
				//mfPresent contains MultiFringeShort
				present = true;
				break;
			}
			if (!present)
				MultiFringeShortArray.add(MultiFringeShort);
		}
		
	}
	
	private static void addInTableComplex(
			HashMap<Short, HashMap<Short, ArrayList<MultiFringeShort>>> table,
			short firstKey, short secondKey, MultiFringeShort MultiFringeShort) {
		
		HashMap<Short, ArrayList<MultiFringeShort>> MultiFringeShortArrayTable = table.get(firstKey);
		if (MultiFringeShortArrayTable==null) {
			MultiFringeShortArrayTable = new HashMap<Short, ArrayList<MultiFringeShort>>();
			table.put(firstKey, MultiFringeShortArrayTable);
			ArrayList<MultiFringeShort> MultiFringeShortArray = new ArrayList<MultiFringeShort>();
			MultiFringeShortArrayTable.put(secondKey, MultiFringeShortArray);
			MultiFringeShortArray.add(MultiFringeShort);			
		}
		else {
			addInTableArray(MultiFringeShortArrayTable, secondKey, MultiFringeShort);
		}
			
	}
	
	private static void addInTableComplexCheckDuplicates(
			HashMap<Short, HashMap<Short, ArrayList<MultiFringeShort>>> table,
			short firstKey, short secondKey, MultiFringeShort MultiFringeShort) {
		
		HashMap<Short, ArrayList<MultiFringeShort>> MultiFringeShortArrayTable = table.get(firstKey);
		if (MultiFringeShortArrayTable==null) {
			MultiFringeShortArrayTable = new HashMap<Short, ArrayList<MultiFringeShort>>();
			table.put(firstKey, MultiFringeShortArrayTable);
			ArrayList<MultiFringeShort> MultiFringeShortArray = new ArrayList<MultiFringeShort>();
			MultiFringeShortArrayTable.put(secondKey, MultiFringeShortArray);
			MultiFringeShortArray.add(MultiFringeShort);			
		}
		else {
			addInTableArrayCheckDuplicates(MultiFringeShortArrayTable, secondKey, MultiFringeShort);
		}
	}
	
	private static TerminalMatrix clone(TerminalMatrix termMatrix) {
		if (termMatrix==null)
			return null;
		return termMatrix.clone();
	}

	
	private void parseIncrementalOneThread() {
		progress = new PrintProgress("Parsing incremental tree:", 1, 0);
		
		Parameters.logLineFlush("\n");
		ParsingThread thread = new ParsingThread();
		thread.run();
							
		progress.end();
	}
	
	private void parseIncremental() {			
		Parameters.reportLineFlush("Checking incremental coverage...");
		progress = new PrintProgress("Parsing tree:", 1, 0);
		
		Parameters.logLineFlush("\n");
		ParsingThread[] threadsArray = new ParsingThread[threads];
		for(int i=0; i<threads; i++) {
			ParsingThread t = new ParsingThread();
			threadsArray[i] = t;
			t.start();
		}
		
		if (noInterruption) {
			for(ParsingThread t : threadsArray) {
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}		
		else {
			if (threads==1)
				threadCheckingTime = 1000;
			
			try {
				Thread.sleep(1000);				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
			boolean someRunning = false;		
			do {						
				someRunning = false;
				for(int i=0; i<threads; i++) {
					ParsingThread t = threadsArray[i];
					if (t.isAlive()) {
						someRunning = true;					
						//long runningTime = t.treeTimer.getEllapsedTime();
						Timer wordTimer = t.currentWordTimer;
						if (wordTimer==null) continue;
						long runningTime = wordTimer.getEllapsedTime();
						if (runningTime > maxMsecPerWord) {
							t.interrupt();
							//t.outOfTime();												
							//t = new ParsingThread();
							//threadsArray[i] = t;
							//t.start();
						}
											
					}				
				}
				
				if (!someRunning)
					break;
				
				try {
					Thread.sleep(threadCheckingTime);				
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			} while(true);	
		}
		
	
		
		progress.end();		
	}

	
	private synchronized TSNodeLabel getNextTree(int[] index) {
		if (!testTBIterator.hasNext()) 
			return null;			
		index[0] = treebankIteratorIndex++;
		return testTBIterator.next();
	}
	
	
	protected class ParsingThread extends Thread {
		
		long numberOfDerivations;
		int[] bestDerivationIndexes;
		int sentenceIndex;
		int currentWordIndex, previousWordIndex, nextWordIndex;
		Timer treeTimer, scanTimer, subDownTimer, subUpTimer, currentWordTimer;
		TSNodeLabel tree;
		Vector<Short> wordLabels;
		LinkedList<Short> restOfTheSentence;
		int length, lastWordIndex;
		boolean lastWord;
		boolean parsed, reachedTop, finished;
		int[][] countScan, countSubDownFirstSimple, 
			countSubDownFirstComplex, countSubDownSecond,
			countSubUpSecond, countSubUpFirst;
		int[] wordTime;
		short nextWordLabel;
		int totalInitLexFringes, totalInitLexFringesRemoved;

		//used as sub-down-second fringes in all bust first word in sentence
		//and as scan/sub-down-first/sub-up-first fringes in first word in sentence
		Vector<HashMap<Short, MultiFringeShort>> 
			firstLexFringesWords; 
			//indexed on root
		
		//used for sub-up-second fringes in all but first word in sentence
		Vector<
			HashMap<Short, 
				HashMap<Short, MultiFringeShort>>> 			
					firstSubFringesWords ; 
					//indexed on first terminal (sub-site) and root
		
		HashMap<Short, MultiFringeShort> scanFringesSimple,
			subUpFirstFringes, subDownSecondFringes;
		
		HashMap<Short, 
			HashMap<Short, ArrayList<MultiFringeShort>>> subDownFirstFringesComplex;
		
		HashMap<Short, ArrayList<MultiFringeShort>>
			scanFringesComplex;
		
		HashMap<Short, 
			HashMap<Short, MultiFringeShort>> subDownFirstFringesSimple;
		
		HashMap<Short, HashMap<Short, MultiFringeShort>> 
			subUpSecondFringes;
		
		Set<Short> subDownMatchingLabel, subUpMatchingLabel;
			
		public void run() {
			TSNodeLabel t = null;
			int[] i = {0};
			while ( (t = getNextTree(i)) != null) {
				sentenceIndex = i[0];
				initTree(t);
				synchronized(progress) {
					initSenteceWordFringes();
				}
				parseIncrementally();
				if (!finished)
					outOfTime();
				else {
					synchronized(progress) {
						if (parsed) parsedTrees++;
						if (reachedTop) reachedTopTrees++;
						progress.next();
						if (debug) {
							printDebug();
						}
					}
				}
			}			
		}
		
		private void initTree(TSNodeLabel t) {
			treeTimer = new Timer(); 
			scanTimer = new Timer();
			subDownTimer = new Timer();
			subUpTimer = new Timer();			
			this.tree = t;
			
			currentWordIndex = 0;
			previousWordIndex = -1;
			nextWordIndex = 1;
			
			lastWord = false;
			
			ArrayList<TSNodeLabel> termNodes = tree.collectTerminalItems();
			this.wordLabels = new Vector<Short>(termNodes.size());
			for(TSNodeLabel n : termNodes) {
				this.wordLabels.add(TermLabelShort.getTermLabelId(n));
			}
			this.restOfTheSentence = new LinkedList<Short>(wordLabels);
			
			this.length = wordLabels.size();
			this.lastWordIndex = length-1;
			this.parsed = false;
			this.finished = false;
			this.reachedTop = false;
			countScan = new int[2][length];
			countSubDownFirstSimple = new int[2][length];
			countSubDownFirstComplex = new int[2][length];
			countSubUpFirst = new int[2][length];
			countSubDownSecond = new int[2][length];
			countSubUpSecond = new int[2][length];
			//avgLengthSubDown = new int[length];
			this.wordTime = new int[length];			
			initArray(countScan);
			initArray(countSubDownFirstSimple);
			initArray(countSubDownFirstComplex);
			initArray(countSubUpFirst);
			initArray(countSubDownSecond);
			initArray(countSubUpSecond);			
			//Arrays.fill(avgLengthSubDown,-1);			
			Arrays.fill(wordTime,-1);
						
		}
		
		private void initSenteceWordFringes() {
			totalInitLexFringes=0; 
			totalInitLexFringesRemoved=0;
						 
			firstLexFringesWords = new Vector<HashMap<Short, MultiFringeShort>>(length);
			firstSubFringesWords = new Vector<HashMap<Short, 
				HashMap<Short, MultiFringeShort>>>(length); 
			
			
			LinkedList<Short> restOfSentenceLexicon = new LinkedList<Short>(wordLabels); 
			for(int i=0; i<length; i++) {
				buildFirstLexFringes(restOfSentenceLexicon);
				buildFirstSubFringes(restOfSentenceLexicon);
				restOfSentenceLexicon.removeFirst();
			};
		}
		
		private void buildFirstLexFringes(LinkedList<Short> sentence) {
			
			LinkedList<Short> restOfSentenceLexicon = new LinkedList<Short>(sentence); 
			short word = restOfSentenceLexicon.removeFirst();
			HashMap<Short, MultiFringeShort> subTable = getFreshFirstLexFringesWithSmoothing(word);

			if (removeFringesNonCompatibleWithSentence)
				filterFringes(restOfSentenceLexicon, subTable);
			
			firstLexFringesWords.add(subTable.isEmpty() ? null : subTable);
		}
		
		private HashMap<Short, MultiFringeShort> getFreshFirstLexFringesWithSmoothing(
				short firstLex) {
						
			HashMap<Short, MultiFringeShort> lexFringes = firstLexFringes.get(firstLex);
			HashMap<Short, MultiFringeShort> result = (lexFringes==null) ? 
					new HashMap<Short, MultiFringeShort>() : 
					addAllWithCloning(lexFringes);

			if (smoothing && lexPosTable.containsKey(firstLex)) {
				for(short pos : lexPosTable.get(firstLex)) {
					HashMap<Short, MultiFringeShort> posFringes = firstLexFringesSmoothing.get(pos);
					if (posFringes!=null) {
						for(Entry<Short, MultiFringeShort> e : posFringes.entrySet()) {
							short root = e.getKey();
							MultiFringeShort mf = e.getValue().clone();
							mf.firstTerminalLabel = firstLex;
							MultiFringeShort mfPresent = result.get(root);
							if (mfPresent==null)
								result.put(root, mf);
							else 
								mfPresent.firstTerminalMultiSet.merge(mf.firstTerminalMultiSet);
						}
					}
				}
			}
			return result;
		}
		
		private HashMap<Short, MultiFringeShort> addAllWithCloning(
				HashMap<Short, MultiFringeShort> tableSource) {
			
			HashMap<Short, MultiFringeShort> result = new HashMap<Short, MultiFringeShort>();
			for(Entry<Short, MultiFringeShort> e : tableSource.entrySet()) {
				short root = e.getKey();
				MultiFringeShort mf = e.getValue().clone();
				result.put(root, mf);
			}
			return result;
		}

		private HashMap<Short, HashMap<Short, MultiFringeShort>> 
			getFreshFirstSubFringesWithSmoothing(short lex) {
			
			HashMap<Short, HashMap<Short, MultiFringeShort>> result = 
				new HashMap<Short, HashMap<Short, MultiFringeShort>>();
			
			HashMap<Short, HashMap<Short, MultiFringeShort>> firsSubFringes =
				firstSubFringes.get(lex);
			if (firsSubFringes!=null) {
				for(Entry<Short, HashMap<Short, MultiFringeShort>> e : firsSubFringes.entrySet()) {
					short firstTerm = e.getKey();
					HashMap<Short, MultiFringeShort> rootTable = e.getValue();
					HashMap<Short, MultiFringeShort> rootTableCloned =
						addAllWithCloning(rootTable);
					result.put(firstTerm,rootTableCloned);					
				}
			}
			if (smoothing && lexPosTable.containsKey(lex)) {
				for(short pos : lexPosTable.get(lex)) {
					HashMap<Short, HashMap<Short, MultiFringeShort>> 
						posFringes = firstSubFringesSmoothing.get(pos);
					if (posFringes!=null) {
						for(Entry<Short, HashMap<Short, MultiFringeShort>> e : posFringes.entrySet()) {
							short firstTerm = e.getKey();							
							HashMap<Short, MultiFringeShort> rootTable = e.getValue();
							HashMap<Short, MultiFringeShort> presentTable = result.get(firstTerm);
							boolean newlyCreated = false;
							if (presentTable==null) {
								presentTable = new HashMap<Short, MultiFringeShort>();
								newlyCreated = true;
							}
							for(Entry<Short, MultiFringeShort> f : rootTable.entrySet()) {
								short root = f.getKey();
								MultiFringeShort mf = f.getValue().clone();
								mf.changeNextInLine(lex);
								MultiFringeShort mfPresent = presentTable.get(root);
								if (mfPresent==null)
									presentTable.put(root, mf);
								else 
									mfPresent.firstTerminalMultiSet.merge(mf.firstTerminalMultiSet);
							}
							if (newlyCreated && !presentTable.isEmpty()) {
								result.put(firstTerm, presentTable);
							}
							return result;							
						}
					}					
				}
			}
			return result;
		}

		private void buildFirstSubFringes(LinkedList<Short> restOfSentenceLexicon) {
			
			short word = restOfSentenceLexicon.getFirst();
			HashMap<Short, HashMap<Short, MultiFringeShort>> subTable = 
				getFreshFirstSubFringesWithSmoothing(word);
			
			if (removeFringesNonCompatibleWithSentence) {
				Iterator<Entry<Short, HashMap<Short, MultiFringeShort>>> iter =
					subTable.entrySet().iterator();
				while(iter.hasNext()) {
					Entry<Short, HashMap<Short, MultiFringeShort>> e = iter.next();
					HashMap<Short, MultiFringeShort> subSubTable = e.getValue();					
					filterFringes(restOfSentenceLexicon, subSubTable);
					if (subSubTable.isEmpty())
						iter.remove();
				}
			}
			
			firstSubFringesWords.add(subTable.isEmpty() ? null : subTable);
				
		}

		private void filterFringes(
				LinkedList<Short> restOfTheSentence,
				HashMap<Short, MultiFringeShort> tableFringes) {
			
			Iterator<Entry<Short, MultiFringeShort>> iter = tableFringes.entrySet().iterator();
			while(iter.hasNext()) {
				Entry<Short, MultiFringeShort> e = iter.next();
				MultiFringeShort mf = e.getValue();
				int[] totRem = mf.firstTerminalMultiSet.removeNonCompatibleFringes(restOfTheSentence);
				totalInitLexFringes+= totRem[0];
				totalInitLexFringesRemoved += totRem[1];
				if (mf.firstTerminalMultiSet.isEmpty()) {
					iter.remove();
				}
			}
			
		}

		private void initArray(int[][] array) {
			for(int[] a : array) {
				Arrays.fill(a, -1);
			}
		}
		
		private void outOfTime() {
			//this.stop();
			wordTime[currentWordIndex] = (int)((currentWordTimer.getEllapsedTime())/1000);
			stopAllTimers();			
			synchronized(progress) {
				progress.next();
				interruptedTrees++;
			}
			if (debug)
				this.printDebug();
		}
		
		private void stopAllTimers() {
			treeTimer.stop(); 
			scanTimer.stop();
			subDownTimer.stop();
			subUpTimer.stop();
		}
		
		private void parseIncrementally() {	
			
			if (printWordFringes) {
				//printFirstLexFirstSubFrags(tree.collectTerminalStrings());
				printFirstLexFirstSubFrags(tree.collectTerminalStrings(), firstLexFringesWords,firstSubFringesWords);
			}
			
			treeTimer.start();
			currentWordTimer = new Timer(true);
			
			lastWord = length==1;
			
			restOfTheSentence.removeFirst();
			HashMap<Short, MultiFringeShort> wordStartFringes = 
				firstLexFringesWords.get(currentWordIndex);			
									
			initNewFringes();
			for(MultiFringeShort mf : wordStartFringes.values()) {
				classifyFringe(mf);
			}
				
			mergeSimpleComplexFringes();
			
			wordTime[0] = (int)((currentWordTimer.stop())/1000);
						
			if (debug) {
				countScan[0][currentWordIndex] = wordStartFringes.size();
				updateCounts();
			}
			
			if (printWordFringes)			
				printFringes(); 						
						
			if (lastWord) {
				finished = true;				
				if (!subUpFirstFringes.isEmpty()) {
					parsed = true;
					reachedTop = subUpFirstFringes.containsKey(TOPnode);
				}
				treeTimer.stop();
			}
			else {				
				parseIncrementallyNextWord(scanFringesComplex,subDownFirstFringesComplex,subUpFirstFringes);
			}
				
		}

		private void parseIncrementallyNextWord(
			HashMap<Short, ArrayList<MultiFringeShort>> previousScan,
			HashMap<Short, HashMap<Short, ArrayList<MultiFringeShort>>> previousSubDownFirst,
			HashMap<Short, MultiFringeShort> previousSubUpFirst) {
			
			if (previousScan.isEmpty() && previousSubDownFirst.isEmpty() && previousSubUpFirst.isEmpty()) {
				finished = true;				
				treeTimer.stop();
				return;
			}
			
			currentWordTimer = new Timer(true);
			
			currentWordIndex++;						
			previousWordIndex++;
			nextWordIndex++;
			
			lastWord = nextWordIndex==length;
			
			short currentWordLabel = restOfTheSentence.removeFirst();
			
			initNewFringes();
			
			//SCAN
			scanTimer.start();
			if (!previousScan.isEmpty()) {				
				for(ArrayList<MultiFringeShort> fringeToScan : previousScan.values()) {					
					for(MultiFringeShort MultiFringeShort : fringeToScan) {
						MultiFringeShort result = MultiFringeShort.scan(currentWordLabel);
						classifyFringe(result);
					}
					if (Thread.interrupted()) {
				        return;
				    }
				}
			}
			scanTimer.stop();
			
			subDownSecondFringes = firstLexFringesWords.get(currentWordIndex);
			subUpSecondFringes = firstSubFringesWords.get(currentWordIndex);						
			
			//SUBDOWN
			subDownTimer.start();
			if (subDownSecondFringes!=null && !previousSubDownFirst.isEmpty()) {			
				subDownSecondFringes.keySet().retainAll(previousSubDownFirst.keySet());				
				for(Entry<Short, MultiFringeShort> e : subDownSecondFringes.entrySet()) {
					MultiFringeShort MultiFringeShortSecond = e.getValue();
					
					//filter subdownsecond based on future subdownsecond roots 
					TerminalsMultiSetTable ftms = MultiFringeShortSecond.firstTerminalMultiSet;
					ftms.retainLexAnd(subDownMatchingLabel);
					if (ftms.isEmpty())
						continue;
					
					short subSite_Root = e.getKey();
					HashMap<Short, ArrayList<MultiFringeShort>>
						subtableMaching = previousSubDownFirst.get(subSite_Root);
					for(ArrayList<MultiFringeShort> matchingFringeArrayFirst : subtableMaching.values()) {						
						for(MultiFringeShort MultiFringeShortFirst : matchingFringeArrayFirst) {														
							MultiFringeShort result = MultiFringeShortFirst.subDown(MultiFringeShortSecond);
							classifyFringe(result);
						}
						if (Thread.interrupted()) {
					        return;
					    }
					}
				}
			}			
			subDownTimer.stop();
						
			
			//SUBUP
			subUpTimer.start();
			if (subUpSecondFringes!=null && !previousSubUpFirst.isEmpty()) {				
				subUpSecondFringes.keySet().retainAll(previousSubUpFirst.keySet());				
				for(Entry<Short, HashMap<Short, MultiFringeShort>> e : subUpSecondFringes.entrySet()) {
					short root_subSite = e.getKey();
					MultiFringeShort matchingFringeFirst = previousSubUpFirst.get(root_subSite);	
					for(MultiFringeShort MultiFringeShortSecond : e.getValue().values()) {									
						MultiFringeShort result = matchingFringeFirst.subUp(MultiFringeShortSecond);
						classifyFringe(result);
					}
					if (Thread.interrupted()) {
				        return;
				    }
				}
			}			
			subUpTimer.stop();
			
			mergeSimpleComplexFringes();
			
			wordTime[currentWordIndex] = (int)((currentWordTimer.stop())/1000);
			
			if (debug)
				updateCounts();				
			
			if (printWordFringes)
				printFringes();				
			
			if (lastWord) {		
				finished = true;
				if (!subUpFirstFringes.isEmpty()) {
					parsed = true;
					reachedTop = subUpFirstFringes.containsKey(TOPnode);
				}
				treeTimer.stop();				
			}
			else  {
				parseIncrementallyNextWord(scanFringesComplex,subDownFirstFringesComplex,subUpFirstFringes);
			}
			
		}


		
		private void initNewFringes() {
			
			scanFringesComplex = new HashMap<Short, ArrayList<MultiFringeShort>>(); 			
			scanFringesSimple = new HashMap<Short, MultiFringeShort>();			
			subDownFirstFringesComplex = new HashMap<Short, 
				HashMap<Short, ArrayList<MultiFringeShort>>>(); 			
			subDownFirstFringesSimple = new HashMap<Short, 
				HashMap<Short, MultiFringeShort>>();			
			subUpFirstFringes = new HashMap<Short, MultiFringeShort>();
			
			HashMap<Short, MultiFringeShort> 
				subDownSecondFringesNextWord = 
					nextWordIndex==length ? null : firstLexFringesWords.get(nextWordIndex);
			HashMap<Short, HashMap<Short, MultiFringeShort>> 				 
				subUpSecondFringesNextWord = 
					nextWordIndex==length ? null :firstSubFringesWords.get(nextWordIndex);
			
			subDownMatchingLabel = subDownSecondFringesNextWord==null ? 
					new HashSet<Short>() : subDownSecondFringesNextWord.keySet();
			subUpMatchingLabel = subUpSecondFringesNextWord==null ? 
					new HashSet<Short>() : subUpSecondFringesNextWord.keySet();
					
			nextWordLabel = nextWordIndex==length ? MultiFringeShort.nullTerminal : wordLabels.get(nextWordIndex);
	
		}
		
		private void mergeSimpleComplexFringes() {
			if (tryToRemoveDuplicates) {
				mergeSimpleComplexCheckDuplicates(scanFringesSimple, scanFringesComplex);			
				mergeSimpleComplexDoubleCheckDuplicates(subDownFirstFringesSimple, subDownFirstFringesComplex);
			}
			else {
				mergeSimpleComplex(scanFringesSimple, scanFringesComplex);			
				mergeSimpleComplexDouble(subDownFirstFringesSimple, subDownFirstFringesComplex);
			}
		}

		private void classifyFringe(MultiFringeShort MultiFringeShort) {						
			if (MultiFringeShort.isSimpleMultiFringe())
				classifySimpleFringe(MultiFringeShort);
			else
				classifyComplexFringe(MultiFringeShort);
		}

		private void classifySimpleFringe(MultiFringeShort smf) {
			
			for(Entry<Short, TerminalMatrix> e : smf.firstTerminalMultiSet.entrySet()) {

				short nextInLine = e.getKey();

				if (nextInLine==MultiFringeShort.emptyTerminal && 
						(subUpMatchingLabel.contains(smf.rootLabel) ||  lastWord)) {
					//subUpFirst
					addInTableSimple(subUpFirstFringes, smf.rootLabel, smf);
					continue;
				}
				if (TermLabelShort.isLexical(nextInLine)) {
					if (nextInLine==nextWordLabel) {
						//scan
						addInTableSimpleClone(scanFringesSimple, smf.rootLabel, smf.rootLabel,
								smf.firstTerminalLabel, nextInLine, e.getValue());				
					}					
					continue;
				}
				if (subDownMatchingLabel.contains(nextInLine)) {
					//subDown
					addInDoubleTableSimpleClone(subDownFirstFringesSimple, nextInLine, smf.rootLabel, 
							smf.firstTerminalLabel, nextInLine, e.getValue());
				}
				
				
			}
			
		}




		private void classifyComplexFringe(MultiFringeShort cmf) {
			
			// only scann or subDownFirst
			for(Entry<Short, TerminalMatrix> e : cmf.firstTerminalMultiSet.entrySet()) {
				short nextInLine = e.getKey();
				if (nextInLine==MultiFringeShort.emptyTerminal) {
					LinkedList<TerminalsMultiSetTable> otherTerminalMultiSetCopy = 
							new LinkedList<TerminalsMultiSetTable>(cmf.otherTerminalMultiSet);
					MultiFringeShort shiftFringe = new MultiFringeShort(cmf.rootLabel, cmf.firstTerminalLabel,
							otherTerminalMultiSetCopy.removeFirst(), otherTerminalMultiSetCopy);
					classifyFringe(shiftFringe);
					continue;
				}
				TerminalMatrix tm = e.getValue();//.clone();
				MultiFringeShort MultiFringeShort = new MultiFringeShort(cmf.rootLabel, cmf.firstTerminalLabel);
				MultiFringeShort.firstTerminalMultiSet.put(nextInLine, tm);
				MultiFringeShort.otherTerminalMultiSet = 
						new LinkedList<TerminalsMultiSetTable>(cmf.otherTerminalMultiSet);				
				if (MultiFringeShort.tryToSimplify()) {
					classifySimpleFringe(MultiFringeShort);
					continue;
				}
				if (TermLabelShort.isLexical(nextInLine)) {
					if (nextInLine==nextWordLabel) {
						//scan
						if (tryToRemoveDuplicates)
							addInTableArrayCheckDuplicates(scanFringesComplex, 
								cmf.rootLabel, MultiFringeShort);
						else
							addInTableArray(scanFringesComplex, 
									cmf.rootLabel, MultiFringeShort);
					}
					continue;
				}					
				if (subDownMatchingLabel.contains(nextInLine)) {
					//subDown
					if (tryToRemoveDuplicates)
						addInTableComplexCheckDuplicates(subDownFirstFringesComplex, nextInLine, 
							cmf.rootLabel, MultiFringeShort);
					else
						addInTableComplex(subDownFirstFringesComplex, nextInLine, 
								cmf.rootLabel, MultiFringeShort);
				}		
			}

		}

		private void updateCounts() {
			
			if (nextWordIndex!=length) {
				countScan[0][nextWordIndex] = 0;
				//countScan[1][nextWordIndex] = 0;
				for(ArrayList<MultiFringeShort> fringeToScan : scanFringesComplex.values()) {
					countScan[0][nextWordIndex] += fringeToScan.size();
				}				
				
				countSubUpFirst[0][currentWordIndex] = subUpFirstFringes.size();
				//countSubUpFirst[1][currentWordIndex] = countTotalFringes(subUpFirstFringes);
				
				countSubDownFirstSimple[0][currentWordIndex]=0;
				//countSubDownFirstSimple[1][currentWordIndex]=0;
				countSubDownFirstComplex[0][currentWordIndex]=0;
				//countSubDownFirstComplex[1][currentWordIndex]=0;

				for(HashMap<Short, ArrayList<MultiFringeShort>> subTable : subDownFirstFringesComplex.values()) {
					for(ArrayList<MultiFringeShort> fringeArrayFirst : subTable.values()) {
						for(MultiFringeShort MultiFringeShortFirst : fringeArrayFirst) {
							if (MultiFringeShortFirst.isSimpleMultiFringe()) {
								countSubDownFirstSimple[0][currentWordIndex]++;
								//countSubDownFirstSimple[1][currentWordIndex]+=MultiFringeShortFirst.totalFringes();
							}
							else {
								countSubDownFirstComplex[0][currentWordIndex]++;
								//countSubDownFirstComplex[1][currentWordIndex]+=MultiFringeShortFirst.totalFringes();
							}
						}
					}
				}

			}
			
			if (currentWordIndex!=0) { 
				countSubUpSecond[0][currentWordIndex] = 0;
				//countSubUpSecond[1][nextWordIndex] += 0;
				
				if (subDownSecondFringes!=null) {
					countSubDownSecond[0][currentWordIndex] = subDownSecondFringes.size();
					//countSubDownSecond[1][nextWordIndex] = countTotalFringes(subDownSecondFringes);
				}

				if (subUpSecondFringes!=null) {
					for(HashMap<Short, MultiFringeShort> subUpSecondSubTable : subUpSecondFringes.values()) {
						countSubUpSecond[0][currentWordIndex] += subUpSecondSubTable.size();
						//countSubUpSecond[1][nextWordIndex] += countTotalFringes(subUpSecondSubTable);
					}
				}
			}
			

			
		}
		
		private void printFringes() {
			
			PrintWriter pw = null;
			String currentWord = TermLabelShort.toString(wordLabels.get(currentWordIndex),false);
			
			if (!lastWord) {
				
				String nextWord = TermLabelShort.toString(wordLabels.get(nextWordIndex),false);
				
				File outputFile = new File(scratchDebugPath + (nextWordIndex) + "_" + nextWord + "_scan_filtered");
				pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SCAN TABLE [" + nextWord + "]");
				printTableArray(scanFringesComplex,pw,false);
				pw.close();
								
			}
			
			if (currentWordIndex!=0) {
				File outputFile = new File(scratchDebugPath + currentWordIndex + "_" + currentWord + "_subDownSecond_filtered");
				pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SUB-DOWN-SECOND TABLE FILTERED [" + currentWord + "]");
				printTable(subDownSecondFringes,pw,false);
				pw.close();
				
				outputFile = new File(scratchDebugPath + currentWordIndex + "_" + currentWord + "_subUpSecond_filtered");
				pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SUB-UP-SECOND TABLE FILTERED [" + currentWord + "]");
				printDoubleTable(subUpSecondFringes,pw);
				pw.close();
			}
			
			 		
			File outputFile = new File(scratchDebugPath + (currentWordIndex) + "_" + currentWord + "_subDownFirst_filtered");
			pw = FileUtil.getPrintWriter(outputFile);
			pw.println("SUB-DOWN-FIRST TABLE FILTERED [" + currentWord + "]");
			printDoubleTableArray(subDownFirstFringesComplex,pw);
			pw.close();
			
			outputFile = new File(scratchDebugPath + (currentWordIndex) + "_" + currentWord + "_subUpFirst_filtered");
			pw = FileUtil.getPrintWriter(outputFile);
			pw.println("SUB-UP-FIRST TABLE FILTERED [" + currentWord + "]");
			printTable(subUpFirstFringes,pw,false);
			pw.close();
			
		}

		private void mergeSimpleComplex(
				HashMap<Short, MultiFringeShort> tableSimple,
				HashMap<Short, ArrayList<MultiFringeShort>> tableComplex) {
			
			for(Entry<Short, MultiFringeShort> e : tableSimple.entrySet()) {
				short key = e.getKey();
				ArrayList<MultiFringeShort> array = tableComplex.get(key);
				if (array==null) {
					array = new ArrayList<MultiFringeShort>();
					tableComplex.put(key, array);
				}
				array.add(e.getValue());
			}			
		}
		
		private void mergeSimpleComplexCheckDuplicates(
				HashMap<Short, MultiFringeShort> tableSimple,
				HashMap<Short, ArrayList<MultiFringeShort>> tableComplex) {
			
			for(Entry<Short, MultiFringeShort> e : tableSimple.entrySet()) {
				addInTableArrayCheckDuplicates(tableComplex, e.getKey(), e.getValue());
			}			
		}
		
		private void mergeSimpleComplexDouble(
				HashMap<Short,HashMap<Short,MultiFringeShort>> tableSimple,
				HashMap<Short,HashMap<Short,ArrayList<MultiFringeShort>>> tableComplex) {
			
			for(Entry<Short,HashMap<Short,MultiFringeShort>> e : tableSimple.entrySet()) {
				short firstKey = e.getKey();
				HashMap<Short,MultiFringeShort> subTableSimple = e.getValue();
				HashMap<Short,ArrayList<MultiFringeShort>> subTableComplex = tableComplex.get(firstKey);
				if (subTableComplex==null) {
					subTableComplex = new HashMap<Short,ArrayList<MultiFringeShort>>();
					tableComplex.put(firstKey, subTableComplex);					
				}
				mergeSimpleComplex(subTableSimple, subTableComplex);		
			}			
		}
		
		private void mergeSimpleComplexDoubleCheckDuplicates(
				HashMap<Short,HashMap<Short,MultiFringeShort>> tableSimple,
				HashMap<Short,HashMap<Short,ArrayList<MultiFringeShort>>> tableComplex) {
			
			for(Entry<Short,HashMap<Short,MultiFringeShort>> e : tableSimple.entrySet()) {
				short firstKey = e.getKey();
				HashMap<Short,MultiFringeShort> subTableSimple = e.getValue();
				HashMap<Short,ArrayList<MultiFringeShort>> subTableComplex = tableComplex.get(firstKey);
				if (subTableComplex==null) {
					subTableComplex = new HashMap<Short,ArrayList<MultiFringeShort>>();
					tableComplex.put(firstKey, subTableComplex);					
				}
				mergeSimpleComplexCheckDuplicates(subTableSimple, subTableComplex);	
			}			
		}
		
		private void printDebug() {
			//synchronized(logFile) {
				Parameters.logLine("Sentence # " + sentenceIndex);
				Parameters.logLine(tree.toStringQtree());
				Parameters.logLine("Sentence Length: " + length);		
				Parameters.logLine("Total lex fringes for words in sentence: " + totalInitLexFringes);
				if (removeFringesNonCompatibleWithSentence)
					Parameters.logLine("... of which compatible to the sentence: " + (totalInitLexFringes-totalInitLexFringesRemoved));
				Parameters.logLine("                                                     " + Utility.removeBrackAndDoTabulation(TermLabelShort.toString(wordLabels).toString()));		
				Parameters.logLine("Ready for Scan (multifringes)                        " + formatIntArray(countScan[0]));
				//Parameters.logLine("Ready for Scan (fringes)                           " + formatIntArray(countScan[1]));
				Parameters.logLine("Ready for SubDownFirst simple (multifringes)         " + formatIntArray(countSubDownFirstSimple[0]));
				//Parameters.logLine("Ready for SubDownFirst simple (fringes)              " + formatIntArray(countSubDownFirstSimple[1]));				
				Parameters.logLine("Ready for SubDownFirst complex (multifringes)        " + formatIntArray(countSubDownFirstComplex[0]));				
				//Parameters.logLine("Ready for SubDownFirst complex (fringes)             " + formatIntArray(countSubDownFirstComplex[1]));
				Parameters.logLine("Ready for SubDownSecond (multifringes)               " + formatIntArray(countSubDownSecond[0]));
				//Parameters.logLine("Ready for SubDownSecond (fringes)                    " + formatIntArray(countSubDownSecond[1]));
				Parameters.logLine("Ready for SubUpFirst (multifringes)                  " + formatIntArray(countSubUpFirst[0]));
				//Parameters.logLine("Ready for SubUpFirst (fringes)                       " + formatIntArray(countSubUpFirst[1]));				
				Parameters.logLine("Ready for SubUpSecond (multifringes)                 " + formatIntArray(countSubUpSecond[0]));				
				//Parameters.logLine("Ready for SubUpSecond (fringes)                      " + formatIntArray(countSubUpSecond[1]));
				Parameters.logLine("Finished: " + finished);
				Parameters.logLine("Parsed: " + parsed);
				Parameters.logLine("Reached TOP: " + reachedTop);
				
				if (!finished)
					Parameters.logLine("Got stuck at word pair : " + wordLabels.get(previousWordIndex) + "-" +
							wordLabels.get(currentWordIndex) + " (" + previousWordIndex + "-" + currentWordIndex + ")");
				long runningTime = treeTimer.getTotalTime();
				long runningTimeSec = runningTime / 1000;
				long otherTime = runningTime - scanTimer.getTotalTime() - subDownTimer.getTotalTime() - 
						subUpTimer.getTotalTime();
				Parameters.logLine("Running time (sec): " + runningTimeSec);
				if (runningTimeSec>1) {
					Parameters.logLine("Word running times (sec):                            " + Utility.removeBrackAndDoTabulation(Arrays.toString(wordTime)));
					Parameters.logLine("\tof which scanning time: " + ((float)scanTimer.getTotalTime())/runningTime*100);
					Parameters.logLine("\tand combining subDown time: " + ((float)subDownTimer.getTotalTime())/runningTime*100);
					Parameters.logLine("\tand combining subUp time: " + ((float)subUpTimer.getTotalTime())/runningTime*100);				
					Parameters.logLine("\tand remaining time: " + ((float)otherTime)/runningTime*100);
				}
				Parameters.logLineFlush("");
			}
		//}
		
	}
		
	
	public static String formatIntArray(int[] a) {
		return Utility.removeBrackAndDoTabulation(Utility.formatArrayNumbersReadable(a));
	}
	
	
	

	public static void main(String[] args) throws Exception {
		
		
	}

	
}
