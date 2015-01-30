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
import tsg.corpora.Wsj;
import tsg.incremental.old.MultiFringe.TerminalMatrix;
import tsg.incremental.old.MultiFringe.TerminalsMultiSetTable;
import util.PrintProgress;
import util.Timer;
import util.Utility;
import util.file.FileUtil;

public class Incremental_TSG_Parser_new extends Thread {
		
	static final TermLabel TOPnode = TermLabel.getTermLabel(Label.getLabel("TOP"), false);
	static final String scratchDebugPath = "/disk/scratch/fsangati/Debug/";
	
	static boolean debug = true;	
	static int treeLenghtLimit = 10;		
	static long maxMsecPerWord = 60*1000;
	static long threadCheckingTime = 20000;
	static int threads = 1;
	
	static boolean printWordFringes = false;
	static boolean tryToRemoveDuplicates = true;
	static int oneTestSentenceIndex = -1;
	static boolean singleThreadWithoutInterruption = false;
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
	HashMap<TermLabel, HashSet<TermLabel>> posLexTable, lexPosTable;
		
	//used as sub-down-second fringes in all bust first word in sentence
	//and as scan/sub-down-first/sub-up-first fringes in first word in sentence
	HashMap<TermLabel, 
		HashMap<TermLabel, MultiFringe>> 
			firstLexFringes = 
				new HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>(); 
				//indexed on first lex and root
	
	//used for sub-up-second fringes in all but first word in sentence
	HashMap<TermLabel, 
		HashMap<TermLabel, 
			HashMap<TermLabel, MultiFringe>>> 			
				firstSubFringes = 
					new HashMap<TermLabel, HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>>(); 
					//indexed on first lex and first terminal (sub-site) and root

	HashMap<TermLabel, 
		HashMap<TermLabel, MultiFringe>> 
			firstLexFringesSmoothing = 
				new HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>(); 
	HashMap<TermLabel, 
		HashMap<TermLabel, 
			HashMap<TermLabel, MultiFringe>>> 			
				firstSubFringesSmoothing = 
					new HashMap<TermLabel, HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>>(); 
	
	
	
	public Incremental_TSG_Parser_new(FragmentExtractor FE, 
			File testFile, String outputPath) throws Exception {
		this(FE, Wsj.getTreebank(testFile), outputPath);		
	}
	
	public Incremental_TSG_Parser_new (FragmentExtractor FE, 
			ArrayList<TSNodeLabel> testTB, String outputPath) throws Exception {
		this.FE = FE;
		this.testTB = testTB;
		this.outputPath = outputPath;
		this.smoothing = FragmentExtractor.smoothingFromFrags || FragmentExtractor.smoothingFromMinSet;
		
		int smoothThresh = FragmentExtractor.minFragFreqForSmoothingFromMinSet;
		logFile = new File(outputPath + "log_parsing_" +
				treeLenghtLimit + 
				(smoothing ? "_smoothing(" + smoothThresh + ")" : "") + 
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
			
			if (singleThreadWithoutInterruption)
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
		Parameters.reportLineFlush("Max comb. when simplifying: " + MultiFringe.maxCombinationsToSimplify);
		Parameters.reportLineFlush("Remove frags not compatible with sentence: " + removeFringesNonCompatibleWithSentence);
		
	}
	
	private void printFinalSummary() {
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
		Parameters.reportLineFlush("Number of finished analysis: " + (testTB.size()-interruptedTrees));
		Parameters.reportLineFlush("Number of interrupted analysis: " + interruptedTrees);
		Parameters.reportLineFlush("Number of successfully parsed trees: " + parsedTrees);		
		Parameters.reportLineFlush("... of which reached top: " + reachedTopTrees);
	}

	private static HashMap<TermLabel, HashSet<TermLabel>> convertTable(
			HashMap<Label, HashSet<Label>> table, boolean keyIsLex, boolean valuesAreLex) {
		
		HashMap<TermLabel, HashSet<TermLabel>> result =
			new HashMap<TermLabel, HashSet<TermLabel>>(); 
		
		for(Entry<Label, HashSet<Label>>  e : table.entrySet()) {
			TermLabel key = TermLabel.getTermLabel(e.getKey(), keyIsLex);
			HashSet<TermLabel> value = new HashSet<TermLabel>();
			for(Label l : e.getValue()) {
				value.add(TermLabel.getTermLabel(l, valuesAreLex));
			}			
			result.put(key,value);
		}
		
		return result;
	}
	
	private void convertPosSet() {
		posLexTable = convertTable(FE.posLexFinal, false, true);
		lexPosTable = convertTable(FE.lexPosFinal, true, false);
		MultiFringe.posSet = posLexTable.keySet();
		MultiFringe.posSetLexTable = posLexTable;
	}

	private void convertLexFragmentsToFringes() {
		
		Parameters.reportLineFlush("Converting lex fragments into fringes...");
		
		int countFirstLexFrags = 0;
		int countFirstLexFringes = 0;
		int countFirstSubFrags = 0;
		int countFirstSubFringes = 0;
		
		for(Entry<Label, HashSet<TSNodeLabel>> e : FE.lexFragsTableFirstLex.entrySet()) {
			TermLabel firstLexLabel = TermLabel.getTermLabel(e.getKey(),true);
			HashSet<TSNodeLabel> fragSet = e.getValue();
			countFirstLexFrags += fragSet.size();
			for(TSNodeLabel firstLexFrag : fragSet) {
				if (addFirstLexFringe(firstLexFringes, firstLexLabel, firstLexFrag))
					countFirstLexFringes++;
			}	
		}
		
		for(Entry<Label, HashSet<TSNodeLabel>> e : FE.lexFragsTableFirstSub.entrySet()) {			
			TermLabel lexLabel = TermLabel.getTermLabel(e.getKey(), true);
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
			Label firstTermLabel = e.getKey();
			TermLabel firstPosLabel = TermLabel.getTermLabel(firstTermLabel,false);
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
			HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> table,
			TermLabel firstLexLabel, TSNodeLabel frag) {					
		
		TermLabel rootLabel = TermLabel.getTermLabel(frag);
		
		LinkedList<TermLabel> termLabels = new LinkedList<TermLabel>();		
		
		for(TSNodeLabel n : frag.collectTerminalItems()) {
			termLabels.add(TermLabel.getTermLabel(n));
		}
		
		termLabels.removeFirst();
		
		MultiFringe multiFringe = null;
		HashMap<TermLabel, MultiFringe> lexFringe = table.get(firstLexLabel);
		
		if (lexFringe==null) {
			lexFringe = new HashMap<TermLabel, MultiFringe>();
			table.put(firstLexLabel,lexFringe);						
		}
		else {
			multiFringe = lexFringe.get(rootLabel);
		}
		if (multiFringe == null) {
			multiFringe = new MultiFringe(rootLabel, firstLexLabel);
			lexFringe.put(rootLabel, multiFringe);
		}
		return multiFringe.addFringe(termLabels);
	}

	private static boolean addFirstSubFringe(
			HashMap<TermLabel, HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>> table,
			TermLabel lexLabel, TSNodeLabel frag) {
		
		TermLabel rootLabel = TermLabel.getTermLabel(frag);
		LinkedList<TermLabel> termLabels = new LinkedList<TermLabel>();
		for(TSNodeLabel n : frag.collectTerminalItems()) {
			termLabels.add(TermLabel.getTermLabel(n));
		}
		TermLabel firstTermLabel = termLabels.removeFirst();		
		
		HashMap<TermLabel, MultiFringe> multiFringeTable = null;
		HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> 
			lexFringe = table.get(lexLabel);
		
		if (lexFringe==null) {
			lexFringe = new HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>();
			table.put(lexLabel,lexFringe);						
		}
		else {
			multiFringeTable = lexFringe.get(firstTermLabel);
		}
		
		MultiFringe multiFringe = null;
		
		if (multiFringeTable==null) {
			multiFringeTable = new HashMap<TermLabel, MultiFringe>();
			lexFringe.put(firstTermLabel, multiFringeTable);
		}
		else {
			multiFringe = multiFringeTable.get(rootLabel);
		}		
		if (multiFringe == null) {
			multiFringe = new MultiFringe(rootLabel, firstTermLabel);
			multiFringeTable.put(rootLabel, multiFringe);
		}
		return multiFringe.addFringe(termLabels);
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
			Vector<HashMap<TermLabel, MultiFringe>> firstLexFringesWords,
			Vector<HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>> firstSubFringesWords) {
		int wIndex = -1;
		for(String w : words) {
			wIndex++;
			HashMap<TermLabel, MultiFringe>  rootMultiFringe = firstLexFringesWords.get(wIndex);
			if (rootMultiFringe!=null) {
				File outputFile = new File(scratchDebugPath + wIndex + "_" + w + "_firstLex");
				PrintWriter pw = FileUtil.getPrintWriter(outputFile);
				pw.println("FIRST LEX TABLE [" + w + "]");
				printTable(rootMultiFringe,pw, false);
				pw.close();
			}
			HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>  firstTermMultiFringe = 
				firstSubFringesWords.get(wIndex);
			if (firstTermMultiFringe!=null) {
				File outputFile = new File(scratchDebugPath + wIndex + "_" + w + "_secondLex");
				PrintWriter pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SECOND LEX TABLE [" + w + "]");
				printDoubleTable(firstTermMultiFringe,pw);
				pw.close();
			}
		}	
		
	}
	
	private static void printTable(HashMap<TermLabel, MultiFringe> table, PrintWriter pw, boolean indent) {
		if (table==null) return;
		if (indent) pw.print("\t");
		pw.println("TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		ArrayList<TermLabel> subIndexes = new ArrayList<TermLabel>();
		for(Entry<TermLabel, MultiFringe> e : table.entrySet()) {
			MultiFringe mf = e.getValue();
			int subTotal = mf.totalFringes();
			subTotalSizes.add(subTotal);
			subSizes.add(1);
			subIndexes.add(e.getKey());
		}
		if (indent) pw.print("\t");
		pw.println("SUB INDEXES: " + subIndexes);
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
		for(Entry<TermLabel, MultiFringe> e : table.entrySet()) {
			if (indent) pw.print("\t");
			pw.println("TABLE ENTRY KEY: " + e.getKey());			
			MultiFringe mf = e.getValue();
			if (indent) pw.print("\t");
			pw.println("SIZE (multifringes): " + 1);
			if (indent) pw.print("\t");
			pw.println("TOTAL SIZE (fringes): " + mf.totalFringes());
			pw.println(e.getValue().toString(1));
		}
	}
	
	private static void printTableArray(
			HashMap<TermLabel, ArrayList<MultiFringe>> table,
			PrintWriter pw, boolean indent) {
		if (table==null) return;
		if (indent) pw.print("\t");
		pw.println("TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		ArrayList<TermLabel> subIndexes = new ArrayList<TermLabel>();
		for(Entry<TermLabel, ArrayList<MultiFringe>> e : table.entrySet()) {
			int size = 0;
			int totalSize = 0;
			for(MultiFringe mf : e.getValue()) {
				totalSize += mf.totalFringes();				
				size ++;
			}
			subTotalSizes.add(totalSize);
			subSizes.add(size);
			subIndexes.add(e.getKey());
		}
		if (indent) pw.print("\t");
		pw.println("SUB INDEXES: " + subIndexes);
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
		for(Entry<TermLabel, ArrayList<MultiFringe>> e : table.entrySet()) {
			if (indent) pw.print("\t");
			pw.println("TABLE ENTRY KEY: " + e.getKey());
			subSizes = new ArrayList<Integer>();
			subTotalSizes = new ArrayList<Integer>();
			for(MultiFringe mf : e.getValue()) {
				subSizes.add(1);
				subTotalSizes.add(mf.totalFringes());
			}
			if (indent) pw.print("\t");
			pw.println("SIZES (multifringes): " + subSizes);			
			if (indent) pw.print("\t");
			pw.println("TOTAL SIZES (fringes): " + subTotalSizes);
			int index = -1;
			for(MultiFringe mf : e.getValue()) {
				index++;
				if (indent) pw.print("\t");
				pw.println("SUB-FRINGE #" + index + ":");
				pw.println(mf.toString(1));
			}
		}
		
	}

	private static void printDoubleTable(HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> table, 
			PrintWriter pw) {
		if (table==null) return;
		pw.println("DOUBLE TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();		
		ArrayList<TermLabel> subIndexes = new ArrayList<TermLabel>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		for(Entry<TermLabel, HashMap<TermLabel, MultiFringe>> e : table.entrySet()) {
			subSizes.add(e.getValue().size());
			subIndexes.add(e.getKey());
			subTotalSizes.add(countTotalFringes(e.getValue()));
		}
		pw.println("SUB INDEXES: " + subIndexes);
		pw.println("SUB SIZES (multifringes): " + subSizes);
		pw.println("SUB SIZES TOTAL (frings): " + subTotalSizes);
		pw.println("TOTAL MULTIFRINGES: " + Utility.sum(subSizes));
		pw.println("TOTAL FRINGES: " + Utility.sum(subTotalSizes));
		pw.println("------------------------------\n");
		for(Entry<TermLabel, HashMap<TermLabel, MultiFringe>> e : table.entrySet()) {
			pw.println("TABLE FIRST ENTRY KEY: " + e.getKey());;
			printTable(e.getValue(),pw, true);			
		}
	}	
	
	private static void printDoubleTableArray(
			HashMap<TermLabel, HashMap<TermLabel, ArrayList<MultiFringe>>> table, 
			PrintWriter pw) {
		if (table==null) return;
		pw.println("DOUBLE TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();		
		ArrayList<TermLabel> subIndexes = new ArrayList<TermLabel>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		for(Entry<TermLabel, HashMap<TermLabel, ArrayList<MultiFringe>>> e : table.entrySet()) {
			int[] sizes = countTotalFringesArray(e.getValue());
			subSizes.add(sizes[0]);
			subIndexes.add(e.getKey());
			subTotalSizes.add(sizes[1]);
		}
		pw.println("SUB INDEXES: " + subIndexes);
		pw.println("SUB SIZES (multifringes): " + subSizes);
		pw.println("SUB SIZES TOTAL (frings): " + subTotalSizes);
		pw.println("TOTAL MULTIFRINGES: " + Utility.sum(subSizes));
		pw.println("TOTAL FRINGES: " + Utility.sum(subTotalSizes));
		pw.println("------------------------------\n");
		for(Entry<TermLabel, HashMap<TermLabel, ArrayList<MultiFringe>>> e : table.entrySet()) {
			pw.println("TABLE FIRST ENTRY KEY: " + e.getKey());;
			printTableArray(e.getValue(),pw, true);			
		}
	}	
	

	
	private static int countTotalFringes(HashMap<TermLabel, MultiFringe> table) {		
		int result = 0;
		for(Entry<TermLabel, MultiFringe> e : table.entrySet()) {
			result += e.getValue().totalFringes();
		}
		return result;
	}
	
	private static int[] countTotalFringesArray(HashMap<TermLabel, ArrayList<MultiFringe>> table) {		
		int multiFringes = 0;
		int totalFringes = 0;
		for(ArrayList<MultiFringe> a : table.values()) {
			multiFringes += a.size();
			totalFringes += countTotalFringes(a);
		}
		return new int[]{multiFringes,totalFringes};
	}
	
	private static int countTotalFringes(ArrayList<MultiFringe> array) {
		int result = 0;
		for(MultiFringe mf : array) {
			result += mf.totalFringes();
		}
		return result;
	}
	

	private static boolean addInTableSimple(
			HashMap<TermLabel, MultiFringe> table,
			TermLabel rootLabel, MultiFringe smf) {
		
		MultiFringe multiFringe = table.get(rootLabel);
		if (multiFringe==null) {
			table.put(rootLabel, smf);
			return true;
		}
		return false;		
	}

	private static boolean addInTableSimpleClone(HashMap<TermLabel, MultiFringe> table,
			TermLabel firstKey, TermLabel rootLabel, TermLabel firstTermLabel, 
			TermLabel nextInLine, TerminalMatrix termMatrix) {
		
		MultiFringe multiFringe = table.get(firstKey);				
		if (multiFringe==null) {
			multiFringe = new MultiFringe(rootLabel, firstTermLabel);
			multiFringe.firstTerminalMultiSet.put(nextInLine, clone(termMatrix));				
			table.put(firstKey, multiFringe);
			return true;
		}
		else {
			TerminalsMultiSetTable firstTermTable = multiFringe.firstTerminalMultiSet;
			TerminalMatrix present = firstTermTable.get(nextInLine);
			if (present==null) {
				firstTermTable.put(nextInLine, clone(termMatrix));
				return true;
			}
			return present.addAll(termMatrix);
		}
	}

	
	
	private static boolean addInDoubleTableSimpleClone(
			HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> table,
			TermLabel firstKey, TermLabel rootLabel, TermLabel firstTermLabel, 
			TermLabel nextInLine, TerminalMatrix termMatrix) {
		
		HashMap<TermLabel, MultiFringe> multiFringeTable = table.get(firstKey);				
		if (multiFringeTable==null) {
			multiFringeTable = new HashMap<TermLabel, MultiFringe>();
			table.put(firstKey, multiFringeTable);						
		}
		return addInTableSimpleClone(multiFringeTable, rootLabel, rootLabel, firstTermLabel, 
				nextInLine, termMatrix);
	}


	
	
	private static void addInTableArray(
			HashMap<TermLabel, ArrayList<MultiFringe>> table,
			TermLabel firstKey, MultiFringe multiFringe) {
		
		ArrayList<MultiFringe> multiFringeArray = table.get(firstKey);
		if (multiFringeArray==null) {
			multiFringeArray = new ArrayList<MultiFringe>();
			table.put(firstKey, multiFringeArray);
		}
		multiFringeArray.add(multiFringe);
	}
	
	private static void addInTableArrayCheckDuplicates(
			HashMap<TermLabel, ArrayList<MultiFringe>> table,
			TermLabel firstKey, MultiFringe multiFringe) {
		
		ArrayList<MultiFringe> multiFringeArray = table.get(firstKey);
		if (multiFringeArray==null) {
			multiFringeArray = new ArrayList<MultiFringe>();
			table.put(firstKey, multiFringeArray);
			multiFringeArray.add(multiFringe);
		}
		else {
			boolean present = false;
			Iterator<MultiFringe> iter = multiFringeArray.iterator();
			while(iter.hasNext()) {
				MultiFringe mfPresent = iter.next();
				//int index = MultiFringe.subFringeEqualSize(multiFringe, mfPresent);
				int index = MultiFringe.subFringe(multiFringe, mfPresent);
				if (index==0) //not compatible
					continue;
				if (index==1) { // multiFringe contains mfPresent
					iter.remove();
					break;
				}
				//mfPresent contains multiFringe
				present = true;
				break;
			}
			if (!present)
				multiFringeArray.add(multiFringe);
		}
		
	}
	
	private static void addInTableComplex(
			HashMap<TermLabel, HashMap<TermLabel, ArrayList<MultiFringe>>> table,
			TermLabel firstKey, TermLabel secondKey, MultiFringe multiFringe) {
		
		HashMap<TermLabel, ArrayList<MultiFringe>> multiFringeArrayTable = table.get(firstKey);
		if (multiFringeArrayTable==null) {
			multiFringeArrayTable = new HashMap<TermLabel, ArrayList<MultiFringe>>();
			table.put(firstKey, multiFringeArrayTable);
			ArrayList<MultiFringe> multiFringeArray = new ArrayList<MultiFringe>();
			multiFringeArrayTable.put(secondKey, multiFringeArray);
			multiFringeArray.add(multiFringe);			
		}
		else {
			addInTableArray(multiFringeArrayTable, secondKey, multiFringe);
		}
			
	}
	
	private static void addInTableComplexCheckDuplicates(
			HashMap<TermLabel, HashMap<TermLabel, ArrayList<MultiFringe>>> table,
			TermLabel firstKey, TermLabel secondKey, MultiFringe multiFringe) {
		
		HashMap<TermLabel, ArrayList<MultiFringe>> multiFringeArrayTable = table.get(firstKey);
		if (multiFringeArrayTable==null) {
			multiFringeArrayTable = new HashMap<TermLabel, ArrayList<MultiFringe>>();
			table.put(firstKey, multiFringeArrayTable);
			ArrayList<MultiFringe> multiFringeArray = new ArrayList<MultiFringe>();
			multiFringeArrayTable.put(secondKey, multiFringeArray);
			multiFringeArray.add(multiFringe);			
		}
		else {
			addInTableArrayCheckDuplicates(multiFringeArrayTable, secondKey, multiFringe);
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
		Vector<TermLabel> wordLabels;
		LinkedList<TermLabel> restOfTheSentence;
		int length, lastWordIndex;
		boolean lastWord;
		boolean parsed, reachedTop, finished;
		int[][] countScan, countSubDownFirstSimple, 
			countSubDownFirstComplex, countSubDownSecond,
			countSubUpSecond, countSubUpFirst;
		int[] wordTime;
		TermLabel nextWordLabel;
		int totalInitLexFringes, totalInitLexFringesRemoved;

		//used as sub-down-second fringes in all bust first word in sentence
		//and as scan/sub-down-first/sub-up-first fringes in first word in sentence
		Vector<HashMap<TermLabel, MultiFringe>> 
			firstLexFringesWords; 
			//indexed on root
		
		//used for sub-up-second fringes in all but first word in sentence
		Vector<
			HashMap<TermLabel, 
				HashMap<TermLabel, MultiFringe>>> 			
					firstSubFringesWords ; 
					//indexed on first terminal (sub-site) and root
		
		HashMap<TermLabel, MultiFringe> scanFringesSimple,
			subUpFirstFringes, subDownSecondFringes;
		
		HashMap<TermLabel, 
			HashMap<TermLabel, ArrayList<MultiFringe>>> subDownFirstFringesComplex;
		
		HashMap<TermLabel, ArrayList<MultiFringe>>
			scanFringesComplex;
		
		HashMap<TermLabel, 
			HashMap<TermLabel, MultiFringe>> subDownFirstFringesSimple;
		
		HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> 
			subUpSecondFringes;
		
		Set<TermLabel> subDownMatchingLabel, subUpMatchingLabel;
			
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
			this.wordLabels = new Vector<TermLabel>(termNodes.size());
			for(TSNodeLabel n : termNodes) {
				this.wordLabels.add(TermLabel.getTermLabel(n));
			}
			this.restOfTheSentence = new LinkedList<TermLabel>(wordLabels);
			
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
						 
			firstLexFringesWords = new Vector<HashMap<TermLabel, MultiFringe>>(length);
			firstSubFringesWords = new Vector<HashMap<TermLabel, 
				HashMap<TermLabel, MultiFringe>>>(length); 
			
			
			LinkedList<TermLabel> restOfSentenceLexicon = new LinkedList<TermLabel>(wordLabels); 
			for(int i=0; i<length; i++) {
				buildFirstLexFringes(restOfSentenceLexicon);
				buildFirstSubFringes(restOfSentenceLexicon);
				restOfSentenceLexicon.removeFirst();
			};
		}
		
		private void buildFirstLexFringes(LinkedList<TermLabel> sentence) {
			
			LinkedList<TermLabel> restOfSentenceLexicon = new LinkedList<TermLabel>(sentence); 
			TermLabel word = restOfSentenceLexicon.removeFirst();
			HashMap<TermLabel, MultiFringe> subTable = getFreshFirstLexFringesWithSmoothing(word);

			if (removeFringesNonCompatibleWithSentence)
				filterFringes(restOfSentenceLexicon, subTable);
			
			firstLexFringesWords.add(subTable.isEmpty() ? null : subTable);
		}
		
		private HashMap<TermLabel, MultiFringe> getFreshFirstLexFringesWithSmoothing(
				TermLabel firstLex) {
						
			HashMap<TermLabel, MultiFringe> lexFringes = firstLexFringes.get(firstLex);
			HashMap<TermLabel, MultiFringe> result = (lexFringes==null) ? 
					new HashMap<TermLabel, MultiFringe>() : 
					addAllWithCloning(lexFringes);

			if (smoothing && lexPosTable.containsKey(firstLex)) {
				for(TermLabel pos : lexPosTable.get(firstLex)) {
					HashMap<TermLabel, MultiFringe> posFringes = firstLexFringesSmoothing.get(pos);
					if (posFringes!=null) {
						for(Entry<TermLabel, MultiFringe> e : posFringes.entrySet()) {
							TermLabel root = e.getKey();
							MultiFringe mf = e.getValue().clone();
							mf.firstTerminalLabel = firstLex;
							MultiFringe mfPresent = result.get(root);
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
		
		private HashMap<TermLabel, MultiFringe> addAllWithCloning(
				HashMap<TermLabel, MultiFringe> tableSource) {
			
			HashMap<TermLabel, MultiFringe> result = new HashMap<TermLabel, MultiFringe>();
			for(Entry<TermLabel, MultiFringe> e : tableSource.entrySet()) {
				TermLabel root = e.getKey();
				MultiFringe mf = e.getValue().clone();
				result.put(root, mf);
			}
			return result;
		}

		private HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> 
			getFreshFirstSubFringesWithSmoothing(TermLabel lex) {
			
			HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> result = 
				new HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>();
			
			HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> firsSubFringes =
				firstSubFringes.get(lex);
			if (firsSubFringes!=null) {
				for(Entry<TermLabel, HashMap<TermLabel, MultiFringe>> e : firsSubFringes.entrySet()) {
					TermLabel firstTerm = e.getKey();
					HashMap<TermLabel, MultiFringe> rootTable = e.getValue();
					HashMap<TermLabel, MultiFringe> rootTableCloned =
						addAllWithCloning(rootTable);
					result.put(firstTerm,rootTableCloned);					
				}
			}
			if (smoothing && lexPosTable.containsKey(lex)) {
				for(TermLabel pos : lexPosTable.get(lex)) {
					HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> 
						posFringes = firstSubFringesSmoothing.get(pos);
					if (posFringes!=null) {
						for(Entry<TermLabel, HashMap<TermLabel, MultiFringe>> e : posFringes.entrySet()) {
							TermLabel firstTerm = e.getKey();							
							HashMap<TermLabel, MultiFringe> rootTable = e.getValue();
							HashMap<TermLabel, MultiFringe> presentTable = result.get(firstTerm);
							boolean newlyCreated = false;
							if (presentTable==null) {
								presentTable = new HashMap<TermLabel, MultiFringe>();
								newlyCreated = true;
							}
							for(Entry<TermLabel, MultiFringe> f : rootTable.entrySet()) {
								TermLabel root = f.getKey();
								MultiFringe mf = f.getValue().clone();
								mf.changeNextInLine(lex);
								MultiFringe mfPresent = presentTable.get(root);
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

		private void buildFirstSubFringes(LinkedList<TermLabel> restOfSentenceLexicon) {
			
			TermLabel word = restOfSentenceLexicon.getFirst();
			HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> subTable = 
				getFreshFirstSubFringesWithSmoothing(word);
			
			if (removeFringesNonCompatibleWithSentence) {
				Iterator<Entry<TermLabel, HashMap<TermLabel, MultiFringe>>> iter =
					subTable.entrySet().iterator();
				while(iter.hasNext()) {
					Entry<TermLabel, HashMap<TermLabel, MultiFringe>> e = iter.next();
					HashMap<TermLabel, MultiFringe> subSubTable = e.getValue();					
					filterFringes(restOfSentenceLexicon, subSubTable);
					if (subSubTable.isEmpty())
						iter.remove();
				}
			}

			
			firstSubFringesWords.add(subTable.isEmpty() ? null : subTable);
				
		}

		private void filterFringes(
				LinkedList<TermLabel> restOfTheSentence,
				HashMap<TermLabel, MultiFringe> tableFringes) {
			
			Iterator<Entry<TermLabel, MultiFringe>> iter = tableFringes.entrySet().iterator();
			while(iter.hasNext()) {
				Entry<TermLabel, MultiFringe> e = iter.next();
				MultiFringe mf = e.getValue();
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
			HashMap<TermLabel, MultiFringe> wordStartFringes = 
				firstLexFringesWords.get(currentWordIndex);			
									
			initNewFringes();
			for(MultiFringe mf : wordStartFringes.values()) {
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
			HashMap<TermLabel, ArrayList<MultiFringe>> previousScan,
			HashMap<TermLabel, HashMap<TermLabel, ArrayList<MultiFringe>>> previousSubDownFirst,
			HashMap<TermLabel, MultiFringe> previousSubUpFirst) {
			
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
			
			TermLabel currentWordLabel = restOfTheSentence.removeFirst();
			
			initNewFringes();
			
			//SCAN
			scanTimer.start();
			if (!previousScan.isEmpty()) {				
				for(ArrayList<MultiFringe> fringeToScan : previousScan.values()) {					
					for(MultiFringe multiFringe : fringeToScan) {
						MultiFringe result = multiFringe.scan(currentWordLabel);
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
				for(Entry<TermLabel, MultiFringe> e : subDownSecondFringes.entrySet()) {
					MultiFringe multiFringeSecond = e.getValue();
					
					//filter subdownsecond based on future subdownsecond roots 
					TerminalsMultiSetTable ftms = multiFringeSecond.firstTerminalMultiSet;
					ftms.retainLexAnd(subDownMatchingLabel);
					if (ftms.isEmpty())
						continue;
					
					TermLabel subSite_Root = e.getKey();
					HashMap<TermLabel, ArrayList<MultiFringe>>
						subtableMaching = previousSubDownFirst.get(subSite_Root);
					for(ArrayList<MultiFringe> matchingFringeArrayFirst : subtableMaching.values()) {						
						for(MultiFringe multiFringeFirst : matchingFringeArrayFirst) {														
							MultiFringe result = multiFringeFirst.subDown(multiFringeSecond);
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
				for(Entry<TermLabel, HashMap<TermLabel, MultiFringe>> e : subUpSecondFringes.entrySet()) {
					TermLabel root_subSite = e.getKey();
					MultiFringe matchingFringeFirst = previousSubUpFirst.get(root_subSite);	
					for(MultiFringe multiFringeSecond : e.getValue().values()) {									
						MultiFringe result = matchingFringeFirst.subUp(multiFringeSecond);
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
			
			scanFringesComplex = new HashMap<TermLabel, ArrayList<MultiFringe>>(); 			
			scanFringesSimple = new HashMap<TermLabel, MultiFringe>();			
			subDownFirstFringesComplex = new HashMap<TermLabel, 
				HashMap<TermLabel, ArrayList<MultiFringe>>>(); 			
			subDownFirstFringesSimple = new HashMap<TermLabel, 
				HashMap<TermLabel, MultiFringe>>();			
			subUpFirstFringes = new HashMap<TermLabel, MultiFringe>();
			
			HashMap<TermLabel, MultiFringe> 
				subDownSecondFringesNextWord = 
					nextWordIndex==length ? null : firstLexFringesWords.get(nextWordIndex);
			HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> 				 
				subUpSecondFringesNextWord = 
					nextWordIndex==length ? null :firstSubFringesWords.get(nextWordIndex);
			
			subDownMatchingLabel = subDownSecondFringesNextWord==null ? 
					new HashSet<TermLabel>() : subDownSecondFringesNextWord.keySet();
			subUpMatchingLabel = subUpSecondFringesNextWord==null ? 
					new HashSet<TermLabel>() : subUpSecondFringesNextWord.keySet();
					
			nextWordLabel = nextWordIndex==length ? null : wordLabels.get(nextWordIndex);
	
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

		private void classifyFringe(MultiFringe multiFringe) {						
			if (multiFringe.isSimpleMultiFringe())
				classifySimpleFringe(multiFringe);
			else
				classifyComplexFringe(multiFringe);
		}

		private void classifySimpleFringe(MultiFringe smf) {
			
			for(Entry<TermLabel, TerminalMatrix> e : smf.firstTerminalMultiSet.entrySet()) {

				TermLabel nextInLine = e.getKey();

				if (nextInLine==MultiFringe.emptyTerminal && 
						(subUpMatchingLabel.contains(smf.rootLabel) ||  lastWord)) {
					//subUpFirst
					addInTableSimple(subUpFirstFringes, smf.rootLabel, smf);
					continue;
				}
				if (nextInLine.isLexical) {
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




		private void classifyComplexFringe(MultiFringe cmf) {
			
			// only scann or subDownFirst
			for(Entry<TermLabel, TerminalMatrix> e : cmf.firstTerminalMultiSet.entrySet()) {
				TermLabel nextInLine = e.getKey();
				if (nextInLine==MultiFringe.emptyTerminal) {
					LinkedList<TerminalsMultiSetTable> otherTerminalMultiSetCopy = 
							new LinkedList<TerminalsMultiSetTable>(cmf.otherTerminalMultiSet);
					MultiFringe shiftFringe = new MultiFringe(cmf.rootLabel, cmf.firstTerminalLabel,
							otherTerminalMultiSetCopy.removeFirst(), otherTerminalMultiSetCopy);
					classifyFringe(shiftFringe);
					continue;
				}
				TerminalMatrix tm = e.getValue();//.clone();
				MultiFringe multiFringe = new MultiFringe(cmf.rootLabel, cmf.firstTerminalLabel);
				multiFringe.firstTerminalMultiSet.put(nextInLine, tm);
				multiFringe.otherTerminalMultiSet = 
						new LinkedList<TerminalsMultiSetTable>(cmf.otherTerminalMultiSet);				
				if (multiFringe.tryToSimplify()) {
					classifySimpleFringe(multiFringe);
					continue;
				}
				if (nextInLine.isLexical) {
					if (nextInLine==nextWordLabel) {
						//scan
						if (tryToRemoveDuplicates)
							addInTableArrayCheckDuplicates(scanFringesComplex, 
								cmf.rootLabel, multiFringe);
						else
							addInTableArray(scanFringesComplex, 
									cmf.rootLabel, multiFringe);
					}
					continue;
				}					
				if (subDownMatchingLabel.contains(nextInLine)) {
					//subDown
					if (tryToRemoveDuplicates)
						addInTableComplexCheckDuplicates(subDownFirstFringesComplex, nextInLine, 
							cmf.rootLabel, multiFringe);
					else
						addInTableComplex(subDownFirstFringesComplex, nextInLine, 
								cmf.rootLabel, multiFringe);
				}		
			}

		}

		private void updateCounts() {
			
			if (nextWordIndex!=length) {
				countScan[0][nextWordIndex] = 0;
				//countScan[1][nextWordIndex] = 0;
				for(ArrayList<MultiFringe> fringeToScan : scanFringesComplex.values()) {
					countScan[0][nextWordIndex] += fringeToScan.size();
				}				
				
				countSubUpFirst[0][currentWordIndex] = subUpFirstFringes.size();
				//countSubUpFirst[1][currentWordIndex] = countTotalFringes(subUpFirstFringes);
				
				countSubDownFirstSimple[0][currentWordIndex]=0;
				//countSubDownFirstSimple[1][currentWordIndex]=0;
				countSubDownFirstComplex[0][currentWordIndex]=0;
				//countSubDownFirstComplex[1][currentWordIndex]=0;

				for(HashMap<TermLabel, ArrayList<MultiFringe>> subTable : subDownFirstFringesComplex.values()) {
					for(ArrayList<MultiFringe> fringeArrayFirst : subTable.values()) {
						for(MultiFringe multiFringeFirst : fringeArrayFirst) {
							if (multiFringeFirst.isSimpleMultiFringe()) {
								countSubDownFirstSimple[0][currentWordIndex]++;
								//countSubDownFirstSimple[1][currentWordIndex]+=multiFringeFirst.totalFringes();
							}
							else {
								countSubDownFirstComplex[0][currentWordIndex]++;
								//countSubDownFirstComplex[1][currentWordIndex]+=multiFringeFirst.totalFringes();
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
					for(HashMap<TermLabel, MultiFringe> subUpSecondSubTable : subUpSecondFringes.values()) {
						countSubUpSecond[0][currentWordIndex] += subUpSecondSubTable.size();
						//countSubUpSecond[1][nextWordIndex] += countTotalFringes(subUpSecondSubTable);
					}
				}
			}
			

			
		}
		
		private void printFringes() {
			
			PrintWriter pw = null;
			String currentWord = wordLabels.get(currentWordIndex).toString(false);
			
			if (!lastWord) {
				
				String nextWord = wordLabels.get(nextWordIndex).toString(false);
				
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
				HashMap<TermLabel, MultiFringe> tableSimple,
				HashMap<TermLabel, ArrayList<MultiFringe>> tableComplex) {
			
			for(Entry<TermLabel, MultiFringe> e : tableSimple.entrySet()) {
				TermLabel key = e.getKey();
				ArrayList<MultiFringe> array = tableComplex.get(key);
				if (array==null) {
					array = new ArrayList<MultiFringe>();
					tableComplex.put(key, array);
				}
				array.add(e.getValue());
			}			
		}
		
		private void mergeSimpleComplexCheckDuplicates(
				HashMap<TermLabel, MultiFringe> tableSimple,
				HashMap<TermLabel, ArrayList<MultiFringe>> tableComplex) {
			
			for(Entry<TermLabel, MultiFringe> e : tableSimple.entrySet()) {
				addInTableArrayCheckDuplicates(tableComplex, e.getKey(), e.getValue());
			}			
		}
		
		private void mergeSimpleComplexDouble(
				HashMap<TermLabel,HashMap<TermLabel,MultiFringe>> tableSimple,
				HashMap<TermLabel,HashMap<TermLabel,ArrayList<MultiFringe>>> tableComplex) {
			
			for(Entry<TermLabel,HashMap<TermLabel,MultiFringe>> e : tableSimple.entrySet()) {
				TermLabel firstKey = e.getKey();
				HashMap<TermLabel,MultiFringe> subTableSimple = e.getValue();
				HashMap<TermLabel,ArrayList<MultiFringe>> subTableComplex = tableComplex.get(firstKey);
				if (subTableComplex==null) {
					subTableComplex = new HashMap<TermLabel,ArrayList<MultiFringe>>();
					tableComplex.put(firstKey, subTableComplex);					
				}
				mergeSimpleComplex(subTableSimple, subTableComplex);		
			}			
		}
		
		private void mergeSimpleComplexDoubleCheckDuplicates(
				HashMap<TermLabel,HashMap<TermLabel,MultiFringe>> tableSimple,
				HashMap<TermLabel,HashMap<TermLabel,ArrayList<MultiFringe>>> tableComplex) {
			
			for(Entry<TermLabel,HashMap<TermLabel,MultiFringe>> e : tableSimple.entrySet()) {
				TermLabel firstKey = e.getKey();
				HashMap<TermLabel,MultiFringe> subTableSimple = e.getValue();
				HashMap<TermLabel,ArrayList<MultiFringe>> subTableComplex = tableComplex.get(firstKey);
				if (subTableComplex==null) {
					subTableComplex = new HashMap<TermLabel,ArrayList<MultiFringe>>();
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
				Parameters.logLine("                                                     " + Utility.removeBrackAndDoTabulation(wordLabels.toString()));		
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
