package tsg.incremental.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.corpora.Wsj;
import tsg.incremental.old.MultiFringe.TerminalMatrix;
import tsg.incremental.old.MultiFringe.TerminalsMultiSetTable;
import util.ArgumentReader;
import util.PrintProgress;
import util.Timer;
import util.Utility;
import util.file.FileUtil;

public class Incremental_TSG_Parser_old extends Thread {
		
	static final TermLabel TOPnode = TermLabel.getTermLabel(Label.getLabel("TOP"), false); 
	static int treeLenghtLimit = 10;
	static boolean debug = true;	
	static long maxMsecPerWord = 20*1000;
	static long threadCheckingTime = 5000;
	static String basePath = "./";
	static File logFile;
	static File filteredFragmentsFile;
	static boolean printFilteredFragmetnsToFile = false;
	static boolean printWordFringes = false;
	//static boolean pruneFringes = true;
	//static int pruneFringeLength = 10;
		
	ArrayList<TSNodeLabel> testTB, fragList = new ArrayList<TSNodeLabel>();	
	Iterator<TSNodeLabel> treebankIterator;
	HashMap<TermLabel, HashSet<TSNodeLabel>> lexiconPosRule;
	int treebankIteratorIndex;
	File fragmentsFile, outputFile;
	
	//used only for first word in sentence
	IdentityHashMap<TermLabel, 
		IdentityHashMap<TermLabel, MultiFringe>> //indexed on first lex and
			subUpFirstFringes = new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>();
			// root
	//used only for first word in sentence
	IdentityHashMap<TermLabel, 
	IdentityHashMap<TermLabel, 
		IdentityHashMap<TermLabel, MultiFringe>>> //indexed on first lex and
		scanFringes = 
			new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>>(),  
			// nextInLine (which is lex) and root
		subDownFirstFringes = 
			new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>>(); 
			//nextInLine (which is non-lex) and root
			
	
	
	//used only for all bust first word in sentence
	IdentityHashMap<TermLabel, 
		IdentityHashMap<TermLabel, 
			IdentityHashMap<TermLabel, MultiFringe>>> //indexed on first lex and			
		subUpSecondFringes = 
			new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>>(); 
			//firstTerm and root
	//used only for all bust first word in sentence
	IdentityHashMap<TermLabel, 
	IdentityHashMap<TermLabel, MultiFringe>> //indexed on first lex and root
		subDownSecondFringes = 
			new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>(); 
			//root
	
	
	
	int threads;
	PrintProgress progress;	
	int parsedTrees, reachedTopTrees, interruptedTrees;
	
	public Incremental_TSG_Parser_old(File fragmentsFile, File testFile, int threads) throws Exception {
		this(fragmentsFile, Wsj.getTreebank(testFile), threads);		
	}
	
	public Incremental_TSG_Parser_old (File fragmentsFile, ArrayList<TSNodeLabel> testTB, int threads) {
		this.testTB = testTB;
		this.fragmentsFile = fragmentsFile;
		this.threads = threads;
		treebankIterator = testTB.iterator();		
		logFile = new File(basePath + "log_old.txt");
		filteredFragmentsFile = new File(basePath + "lexFrags.mrg");
		readTreeBankAndFragments();
	}
	
	private void readTreeBankAndFragments() {
		if (debug)
			Parameters.openLogFile(logFile);
		printParameters();
		filterTreebank();
		readLexiconFromTreebank();
		readFragmentsFile();
		addFragmentPos();
		printFragmentsToFile();
	}
	
	public void run() {		
		if (threads==1)
			parseIncrementalOneThread();
		else
			parseIncremental();
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
		Parameters.reportLineFlush("Number of completed trees: " + interruptedTrees);
		Parameters.reportLineFlush("Number of interrupted trees: " + interruptedTrees);
		Parameters.reportLineFlush("Number of successfully parsed trees: " + parsedTrees);
		Parameters.reportLineFlush("... of which reached top: " + reachedTopTrees);		
		if (debug)
			Parameters.closeLogFile();
	}
	
	private void printParameters() {
		Parameters.reportLineFlush("");
		Parameters.reportLineFlush("Tree Lenght Limit: " + treeLenghtLimit);
		Parameters.reportLineFlush("Max sec Per Word: " + maxMsecPerWord/1000);
		Parameters.reportLineFlush("Number of threads: " + threads);
		//Parameters.reportLineFlush("Prune Fringes: " + pruneFringes + 
		//		(pruneFringes ? " (max yield: " + pruneFringeLength + ")" : ""));
		Parameters.reportLineFlush("Number of threads: " + threads);
		
	}

	private void filterTreebank() {
		if (treeLenghtLimit<0)
			Parameters.reportLineFlush("No tree length limit");
		else {
			Parameters.reportLineFlush("Tree length limit: " + treeLenghtLimit);
			while(treebankIterator.hasNext()) {
				TSNodeLabel t = treebankIterator.next();
				if (t.countLexicalNodes()>treeLenghtLimit)
					treebankIterator.remove();
			}
			treebankIterator = testTB.iterator();
		}
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
	}

	private void readLexiconFromTreebank() {
		Parameters.reportLineFlush("Reading Lexicon from treebank...");
		lexiconPosRule = new HashMap<TermLabel, HashSet<TSNodeLabel>>();
		for(TSNodeLabel t : testTB) {
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			for(TSNodeLabel l : lexItems) {
				TermLabel word = TermLabel.getTermLabel(l);
				TSNodeLabel posRule = l.parent.clone();
				HashSet<TSNodeLabel> wordPosRuleSet = lexiconPosRule.get(word);
				if (wordPosRuleSet==null) {
					wordPosRuleSet = new HashSet<TSNodeLabel>();
					lexiconPosRule.put(word, wordPosRuleSet);
				}
				wordPosRuleSet.add(posRule);
			}
		}		
	}
	

	private void readFragmentsFile() {		
		Parameters.reportLineFlush("Reading Fragments File...");	
		Scanner fragmentScan = FileUtil.getScanner(fragmentsFile);
		int totalFragments = 0;
		int totalLexicalizedFragments = 0;
		int initalFrags = 0;
		int nextFrags = 0;
		int prunedFrags = 0;
		progress = new PrintProgress("Reading Fragment", 10000, 0);
		while(fragmentScan.hasNextLine()) {
			totalFragments++;
			progress.next();
			String line = fragmentScan.nextLine();
			String[] treeFreq = line.split("\t");			
			String fragmentString = treeFreq[0];
			try {
				TSNodeLabel fragment = new TSNodeLabel(fragmentString, false);
				TSNodeLabel lexNode = fragment.getLeftmostLexicalNode();
				if (lexNode!=null) {
					ArrayList<TSNodeLabel> terminalNodes = fragment.collectTerminalItems();
					/*if (pruneFringes) {
						if (terminalNodes.size()>pruneFringeLength) {
							prunedFrags++;
							continue;
						}
					}*/					
					TermLabel lexLabel = TermLabel.getTermLabel(lexNode);
					LinkedList<TermLabel> termLabels = new LinkedList<TermLabel>();
					for(TSNodeLabel n : terminalNodes) {
						termLabels.add(TermLabel.getTermLabel(n));
					}
					//if (!lexiconPosRule.keySet().containsAll(termLabels)) continue;					
					int lexNodeIndex = termLabels.indexOf(lexLabel);
					if (lexNodeIndex>1) continue; //more than 1 sub site to the left of leftmost lex
					
					totalLexicalizedFragments++;
					if (printFilteredFragmetnsToFile)
						fragList.add(fragment);
					
					
					TermLabel rootLabel = TermLabel.getTermLabel(fragment);
					TermLabel firstTerminal = termLabels.removeFirst();
					if (firstTerminal.isLexical) {
						initalFrags++;	
						addFirstLexFrag(firstTerminal, rootLabel, termLabels);
					}
					else {
						nextFrags++;
						addFirstSubFrag(firstTerminal, rootLabel, lexLabel, termLabels);
					}
				}
			} catch (Exception e) {				
				e.printStackTrace();
				Parameters.reportError("Problems in fragmentsFile: " + line);
				Parameters.reportError(e.getMessage());
				return;
			}
		}
		
		progress.end();
		Parameters.reportLineFlush("Total Fragments: " + totalFragments);
		Parameters.reportLineFlush("Pruned Fragments for Fringe length limit: " + prunedFrags);		
		Parameters.reportLineFlush("Total valid Lexicalized Fragments: " + totalLexicalizedFragments);
		Parameters.reportLineFlush("of which lex first: " + initalFrags);
		Parameters.reportLineFlush("and sub first: " + nextFrags);
		
	}
	
	static TermLabel thisLabel = TermLabel.getTermLabel("This", true);
	
	private void addFirstLexFrag(TermLabel firstLexical, TermLabel rootLabel, 
			LinkedList<TermLabel> termLabels) {
		
		LinkedList<TermLabel> termLabelsCopy = new LinkedList<TermLabel>(termLabels);
		addInTable(subDownSecondFringes, firstLexical, rootLabel, firstLexical, termLabelsCopy);		
		if (termLabels.isEmpty())
			addInTable(subUpFirstFringes, firstLexical, rootLabel, firstLexical, termLabels);
		else {
			TermLabel nextInLine = termLabels.getFirst(); 
			if (nextInLine.isLexical)
				addInDoubleTable(scanFringes, firstLexical, nextInLine, rootLabel, firstLexical, termLabels);
			else 
				addInDoubleTable(subDownFirstFringes, firstLexical, nextInLine, rootLabel, firstLexical, termLabels);
		}
		
	}
	
	private void addFirstSubFrag(TermLabel firstTerminal, TermLabel rootLabel, TermLabel firstLexical, 
			LinkedList<TermLabel> termLabels) {
		addInDoubleTable(subUpSecondFringes, firstLexical, firstTerminal, rootLabel, firstTerminal, termLabels);		
	}
	

	private static boolean addInTable(
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> table,
			TermLabel firstKey, TermLabel rootLabel, TermLabel firstTermLabel,
			LinkedList<TermLabel> otherTermLabels) {
		
		MultiFringe multiFringe = null;
		IdentityHashMap<TermLabel, MultiFringe> lexFringe = table.get(firstKey);
		
		if (lexFringe==null) {
			lexFringe = new IdentityHashMap<TermLabel, MultiFringe>();
			table.put(firstKey,lexFringe);						
		}
		else {
			multiFringe = lexFringe.get(rootLabel);
		}
		if (multiFringe == null) {
			multiFringe = new MultiFringe(rootLabel, firstTermLabel);
			lexFringe.put(rootLabel, multiFringe);
		}
		return multiFringe.addFringe(otherTermLabels);
	}
	
	private static boolean addInDoubleTable(
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>> table,
			TermLabel firstKey, TermLabel secondKey, TermLabel rootLabel, TermLabel firstTermLabel,
			LinkedList<TermLabel> otherTermLabels) {
		
		IdentityHashMap<TermLabel, MultiFringe> multiFringeTable = null;
		IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> lexFringe = table.get(firstKey);
		
		if (lexFringe==null) {
			lexFringe = new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>();
			table.put(firstKey,lexFringe);						
		}
		else {
			multiFringeTable = lexFringe.get(secondKey);
		}
		
		MultiFringe multiFringe = null;
		if (multiFringeTable==null) {
			multiFringeTable = new IdentityHashMap<TermLabel, MultiFringe>();
			lexFringe.put(secondKey, multiFringeTable);
		}
		else {
			multiFringe = multiFringeTable.get(rootLabel);
		}		
		if (multiFringe == null) {
			multiFringe = new MultiFringe(rootLabel, firstTermLabel);
			multiFringeTable.put(rootLabel, multiFringe);
		}
		return multiFringe.addFringe(otherTermLabels);
	}

	private void printLexFrags(List<String> list) {
		boolean first = true;
		int wIndex = -1;
		for(String w : list) {
			wIndex++;
			TermLabel termLabelWord = TermLabel.getTermLabel(Label.getLabel(w), true);
			IdentityHashMap<TermLabel, MultiFringe>  rootMultiFringe = subDownSecondFringes.get(termLabelWord);
			File outputFile = new File(basePath + wIndex + "_" + w + "_firstLex");
			PrintWriter pw = FileUtil.getPrintWriter(outputFile);
			pw.println("FIRST LEX TABLE [" + w + "]");
			printTable(rootMultiFringe,pw, false);
			pw.close();
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>  firstTermMultiFringe = 
				subUpSecondFringes.get(termLabelWord);
			outputFile = new File(basePath + wIndex + "_" + w + "_secondLex");
			pw = FileUtil.getPrintWriter(outputFile);
			pw.println("SECOND LEX TABLE [" + w + "]");
			printDoubleTable(firstTermMultiFringe,pw);
			pw.close();
			
			if (first) {
				outputFile = new File(basePath + wIndex + "_" + w + "_scan");
				pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SCAN TABLE [" + w + "]");
				printDoubleTable(scanFringes.get(termLabelWord),pw);
				pw.close();
				
				outputFile = new File(basePath + wIndex + "_" + w + "_subDownFirst");
				pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SUB-DOWN-FIRST TABLE [" + w + "]");
				printDoubleTable(subDownFirstFringes.get(termLabelWord),pw);
				pw.close();
				
				outputFile = new File(basePath + wIndex + "_" + w + "_subUpFirst");
				pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SUB-UP-FIRST TABLE [" + w + "]");
				printTable(subUpFirstFringes.get(termLabelWord),pw, false);
				pw.close();
				
				first = false;
			}
		}		
	}
	
	private static void printTable(IdentityHashMap<TermLabel, MultiFringe> table, PrintWriter pw, boolean indent) {
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
			IdentityHashMap<TermLabel, ArrayList<MultiFringe>> table,
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

	private static void printDoubleTable(IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> table, 
			PrintWriter pw) {
		if (table==null) return;
		pw.println("DOUBLE TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();		
		ArrayList<TermLabel> subIndexes = new ArrayList<TermLabel>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		for(Entry<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> e : table.entrySet()) {
			subSizes.add(e.getValue().size());
			subIndexes.add(e.getKey());
			subTotalSizes.add(computeTotalFringes(e.getValue()));
		}
		pw.println("SUB INDEXES: " + subIndexes);
		pw.println("SUB SIZES (multifringes): " + subSizes);
		pw.println("SUB SIZES TOTAL (frings): " + subTotalSizes);
		pw.println("TOTAL MULTIFRINGES: " + Utility.sum(subSizes));
		pw.println("TOTAL FRINGES: " + Utility.sum(subTotalSizes));
		pw.println("------------------------------\n");
		for(Entry<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> e : table.entrySet()) {
			pw.println("TABLE FIRST ENTRY KEY: " + e.getKey());;
			printTable(e.getValue(),pw, true);			
		}
	}	
	
	private static void printDoubleTableArray(
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> table, 
			PrintWriter pw) {
		if (table==null) return;
		pw.println("DOUBLE TABLE SIZE: " + table.size());
		ArrayList<Integer> subSizes = new ArrayList<Integer>();		
		ArrayList<TermLabel> subIndexes = new ArrayList<TermLabel>();
		ArrayList<Integer> subTotalSizes = new ArrayList<Integer>();
		for(Entry<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> e : table.entrySet()) {
			subSizes.add(e.getValue().size());
			subIndexes.add(e.getKey());
			subTotalSizes.add(computeTotalFringesArray(e.getValue()));
		}
		pw.println("SUB INDEXES: " + subIndexes);
		pw.println("SUB SIZES (multifringes): " + subSizes);
		pw.println("SUB SIZES TOTAL (frings): " + subTotalSizes);
		pw.println("TOTAL MULTIFRINGES: " + Utility.sum(subSizes));
		pw.println("TOTAL FRINGES: " + Utility.sum(subTotalSizes));
		pw.println("------------------------------\n");
		for(Entry<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> e : table.entrySet()) {
			pw.println("TABLE FIRST ENTRY KEY: " + e.getKey());;
			printTableArray(e.getValue(),pw, true);			
		}
	}	
	

	
	private static int computeTotalFringes(IdentityHashMap<TermLabel, MultiFringe> table) {		
		int result = 0;
		for(Entry<TermLabel, MultiFringe> e : table.entrySet()) {
			result += e.getValue().totalFringes();
		}
		return result;
	}
	
	private static int computeTotalFringesArray(IdentityHashMap<TermLabel, ArrayList<MultiFringe>> table) {		
		int result = 0;
		for(Entry<TermLabel, ArrayList<MultiFringe>> e : table.entrySet()) {
			for(MultiFringe mf : e.getValue()) {
				result += mf.totalFringes();
			}			
		}
		return result;
	}
	
	private static int countTotalFringes(ArrayList<MultiFringe> array) {
		int result = 0;
		for(MultiFringe mf : array) {
			result += mf.totalFringes();
		}
		return result;
	}

	private static boolean addInTableSimple(IdentityHashMap<TermLabel, MultiFringe> table,
			TermLabel rootLabel, TermLabel firstTermLabel, 
			TermLabel nextInLine, TerminalMatrix termMatrix) {
		
		MultiFringe multiFringe = table.get(rootLabel);				
		if (multiFringe==null) {
			multiFringe = new MultiFringe(rootLabel, firstTermLabel);
			multiFringe.firstTerminalMultiSet.put(nextInLine, clone(termMatrix));				
			table.put(rootLabel, multiFringe);
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
	
	private static boolean addInDoubleTableSimple(
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> table,
			TermLabel firstKey, TermLabel rootLabel, TermLabel firstTermLabel, 
			TermLabel nextInLine, TerminalMatrix termMatrix) {
		
		IdentityHashMap<TermLabel, MultiFringe> multiFringeTable = table.get(firstKey);				
		if (multiFringeTable==null) {
			multiFringeTable = new IdentityHashMap<TermLabel, MultiFringe>();
			table.put(firstKey, multiFringeTable);						
		}
		return addInTableSimple(multiFringeTable, rootLabel, firstTermLabel, 
				nextInLine, termMatrix);
	}
	
	private static TerminalMatrix clone(TerminalMatrix termMatrix) {
		if (termMatrix==null)
			return null;
		return termMatrix.clone();
	}
	
	private static void addInTableComplex(
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> table,
			TermLabel firstKey, MultiFringe multiFringe) {
		
		IdentityHashMap<TermLabel, ArrayList<MultiFringe>> multiFringeArrayTable = table.get(firstKey);
		if (multiFringeArrayTable==null) {
			multiFringeArrayTable = new IdentityHashMap<TermLabel, ArrayList<MultiFringe>>();
			table.put(firstKey, multiFringeArrayTable);
		}
		TermLabel rootLabel = multiFringe.rootLabel;
		ArrayList<MultiFringe> multiFringeArray = multiFringeArrayTable.get(rootLabel);
		if (multiFringeArray==null) {
			multiFringeArray = new ArrayList<MultiFringe>();
			multiFringeArrayTable.put(rootLabel, multiFringeArray);
		}
		multiFringeArray.add(multiFringe);
	}



	private void addFragmentPos() {
		int newWordPos = 0;
		for(Entry<TermLabel, HashSet<TSNodeLabel>> e : lexiconPosRule.entrySet()) {
			TermLabel word = e.getKey();
			HashSet<TSNodeLabel> posRuleSet = e.getValue();
			LinkedList<TermLabel> termLabels = new LinkedList<TermLabel>(); 
			for(TSNodeLabel posRule : posRuleSet) {
				TermLabel rootLabel = TermLabel.getTermLabel(posRule);
				if (addInTable(subUpFirstFringes, word, rootLabel, word, termLabels)) {
					addInTable(subDownSecondFringes, word, rootLabel, word, termLabels);
					newWordPos++;
					if (printFilteredFragmetnsToFile)
						fragList.add(posRule);
				}
			}
		}
		Parameters.reportLineFlush("Adding total unseen Pos-Word pairs in gold: " + newWordPos);
		
	}


	private void printFragmentsToFile() {
		if (printFilteredFragmetnsToFile) {
			Parameters.reportLineFlush("Printing Fragments...");
			PrintWriter pw = FileUtil.getPrintWriter(filteredFragmentsFile);
			for(TSNodeLabel frag : fragList) {
				pw.println(frag.toString(false, true));
			}			
			pw.close();
		}		
	}
	
	private void parseIncrementalOneThread() {
		Parameters.reportLineFlush("Checking incremental coverage...");
		progress = new PrintProgress("Extracting from tree:", 1, 0);
		
		Parameters.logLineFlush("\n");
		ParsingThread thread = new ParsingThread();
		thread.run();
							
		progress.end();
	}
	
	private void parseIncremental() {			
		Parameters.reportLineFlush("Checking incremental coverage...");
		progress = new PrintProgress("Extracting from tree:", 1, 0);
		
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
						t.outOfTime();												
						t = new ParsingThread();
						threadsArray[i] = t;
						t.start();
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
		if (!treebankIterator.hasNext()) 
			return null;			
		index[0] = treebankIteratorIndex++;
		return treebankIterator.next();
	}
	
	
	protected class ParsingThread extends Thread {
		
		long numberOfDerivations;
		int[] bestDerivationIndexes;
		int index;
		int currentWordIndex;
		Timer treeTimer, scanTimer, subDownTimer, subUpTimer, currentWordTimer;
		TSNodeLabel tree;
		Vector<TermLabel> wordLabels;
		int length, lastWordIndex;
		boolean parsed, reachedTop, finished;
		int[][] totalFringesScan, totalFringesSubDownFirstSimple, 
			totalFringesSubDownFirstComplex, totalFringesSubDownSecond,
			totalFringesSubUpSecond, totalFringesSubUpFirst;
		//int[] avgLengthSubDown;
		int[] wordTime;
		
		IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> 
			readyForScanComplex, // nextInLine (lex), root
			readyForSubDownFirstComplex;  // nextInLine (non-lex), root
		
		IdentityHashMap<TermLabel, ArrayList<MultiFringe>> 
			readyForSubUpFirstComplex;  // root
	
		IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> 
			readyForScanSimple, // nextInLine (lex), root 
			readyForSubDownFirstSimple; // nextInLine (non-lex), root
	
		IdentityHashMap<TermLabel, MultiFringe> 
			readyForSubUpFirstSimple; //root
		
		public void run() {
			TSNodeLabel t = null;
			int[] i = {0};
			while ( (t = getNextTree(i)) != null) {
				index = i[0];
				initTree(t);
				parseIncrementally();
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
		
		private void initTree(TSNodeLabel t) {
			treeTimer = new Timer(); 
			scanTimer = new Timer();
			subDownTimer = new Timer();
			subUpTimer = new Timer();			
			this.tree = t;
			
			ArrayList<TSNodeLabel> termNodes = tree.collectTerminalItems();
			this.wordLabels = new Vector<TermLabel>(termNodes.size());
			for(TSNodeLabel n : termNodes) {
				this.wordLabels.add(TermLabel.getTermLabel(n));
			}
			
			this.length = wordLabels.size();
			this.lastWordIndex = length-1;
			this.parsed = false;
			this.finished = false;
			this.reachedTop = false;
			totalFringesScan = new int[2][length];
			totalFringesSubDownFirstSimple = new int[2][length];
			totalFringesSubDownFirstComplex = new int[2][length];
			totalFringesSubUpFirst = new int[2][length];
			totalFringesSubDownSecond = new int[2][length];
			totalFringesSubUpSecond = new int[2][length];
			//avgLengthSubDown = new int[length];
			this.wordTime = new int[length];			
			initArray(totalFringesScan);
			initArray(totalFringesSubDownFirstSimple);
			initArray(totalFringesSubDownFirstComplex);
			initArray(totalFringesSubUpFirst);
			initArray(totalFringesSubDownSecond);
			initArray(totalFringesSubUpSecond);			
			//Arrays.fill(avgLengthSubDown,-1);			
			Arrays.fill(wordTime,-1);
		}
		
		private void initArray(int[][] array) {
			for(int[] a : array) {
				Arrays.fill(a, -1);
			}
		}
		
		private void outOfTime() {
			this.stop();
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
			
			if (printWordFringes)
				printLexFrags(tree.collectTerminalStrings());
			
			treeTimer.start();
			currentWordTimer = new Timer(true);
			
			currentWordIndex = 0;
			TermLabel initWordLabel = wordLabels.get(currentWordIndex);	
			readyForScanComplex = convertToFrigeDoubleTableArray(scanFringes.get(initWordLabel));
			readyForSubDownFirstComplex = convertToFrigeDoubleTableArray(subDownFirstFringes.get(initWordLabel));
			readyForSubUpFirstComplex = convertToFrigeTableArray(subUpFirstFringes.get(initWordLabel));
			
			wordTime[0] = (int)((currentWordTimer.stop())/1000);
			
			//updateStatistics(0);
			//updateAvgLengthReadyForSubDownFirst(currentWordIndex);
			
			if (0==lastWordIndex) {
				finished = true;
				treeTimer.stop();
				parsed = !readyForSubUpFirstComplex.isEmpty();
				reachedTop = readyForSubUpFirstComplex.get(TOPnode)!=null;				
			}
			else
				parseIncrementallyNextWord(1);
		}
		
		private void updateAvgLengthReadyForSubDownFirst(int i) {
			int avgLength = 0;
			int count = 0;
			for(IdentityHashMap<TermLabel,ArrayList<MultiFringe>> table : readyForSubDownFirstComplex.values()) {
				for(ArrayList<MultiFringe> amf : table.values()) {
					for(MultiFringe mf : amf) {
						avgLength += mf.averageLength();
						count++;
					}
				}
			}			
			//avgLengthSubDown[i] = count==0 ?  0 : avgLength/count;			
		}
		
		private IdentityHashMap<TermLabel, ArrayList<MultiFringe>> convertToFrigeTableArray(
				IdentityHashMap<TermLabel,MultiFringe> table) {
			
			IdentityHashMap<TermLabel, ArrayList<MultiFringe>> result =
					new IdentityHashMap<TermLabel, ArrayList<MultiFringe>>();
			if (table!=null) {
				for(Entry<TermLabel, MultiFringe> e : table.entrySet()) {
					ArrayList<MultiFringe> a = new ArrayList<MultiFringe>();
					a.add(e.getValue());
					result.put(e.getKey(), a);
				}				
			}
			return result;
		}

		private IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> convertToFrigeDoubleTableArray(
				IdentityHashMap<TermLabel,IdentityHashMap<TermLabel,MultiFringe>> table) {
			
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> result =
					new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>>();
			if (table!=null) {
				for(Entry<TermLabel,IdentityHashMap<TermLabel,MultiFringe>> f : table.entrySet()) {
					IdentityHashMap<TermLabel, ArrayList<MultiFringe>> resultEntry = 
						new IdentityHashMap<TermLabel, ArrayList<MultiFringe>>();
					result.put(f.getKey(), resultEntry);
					for(Entry<TermLabel, MultiFringe> e : f.getValue().entrySet()) {
						ArrayList<MultiFringe> a = new ArrayList<MultiFringe>();
						a.add(e.getValue());
						resultEntry.put(e.getKey(), a);
					}
				}				
			}
			return result;
		}
		
		private void initNewFringes() {
			readyForScanSimple = 
				new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>(); 
				// nextInLine (lex), root
			readyForSubDownFirstSimple = 
				new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>>(); 
				// nextInLine (non-lex), root
			readyForSubUpFirstSimple = 
				new IdentityHashMap<TermLabel, MultiFringe>(); 
				// root
			
			readyForScanComplex = 
				new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>>(); 
				// nextInLine (lex), root
			readyForSubDownFirstComplex = 
				new IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>>();
				// nextInLine (non-lex), root
			readyForSubUpFirstComplex = 
				new IdentityHashMap<TermLabel, ArrayList<MultiFringe>>();
				// root
		}
				
		private void parseIncrementallyNextWord(int i) {
			
			currentWordIndex = i;
			currentWordTimer = new Timer(true);
			
			int prevIndex = i-1;
			initStats(prevIndex);
			
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> previousReadyForScan = 
				readyForScanComplex; // nextInLine (lex), root
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> previousReadyForSubDown = 
				readyForSubDownFirstComplex; // nextInLine (non-lex), root
			IdentityHashMap<TermLabel, ArrayList<MultiFringe>> previousReadyForSubUp = 
				readyForSubUpFirstComplex; // root
			
			initNewFringes();
			
			TermLabel currentWordLabel = wordLabels.get(i);
			
			//SCAN
			scanTimer.start();
			
			IdentityHashMap<TermLabel, ArrayList<MultiFringe>> fringeToScanTable = 
				previousReadyForScan.get(currentWordLabel);
			
			
			if (fringeToScanTable!=null) {
				
				if (printWordFringes) {
					String prevWord = wordLabels.get(prevIndex).toString(false);
					outputFile = new File(basePath + (prevIndex) + "_" + prevWord + "_scan_filtered");
					PrintWriter pw = FileUtil.getPrintWriter(outputFile);
					pw.println("SCAN TABLE [" + prevWord + "]");
					printTableArray(fringeToScanTable,pw,false);
					pw.close();
				}			
			
				for(ArrayList<MultiFringe> fringeToScan : fringeToScanTable.values()) {
					totalFringesScan[0][prevIndex] += fringeToScan.size();					
					for(MultiFringe multiFringe : fringeToScan) {
						totalFringesScan[1][prevIndex] += multiFringe.totalFringes();
						MultiFringe result = multiFringe.scan(currentWordLabel);
						classifyFringe(result);
					}
				}
			}
			
			scanTimer.stop();
						
			
			//SUBDOWN
			subDownTimer.start();
			
			//updateStatisticsSecond(i, readyForSubDownSecond, readyForSubUpSecond);
			
			IdentityHashMap<TermLabel, MultiFringe> readyForSubDownSecondFringes =
					subDownSecondFringes.get(currentWordLabel);
			
			if (readyForSubDownSecondFringes!=null) {
			
				readyForSubDownSecondFringes.keySet().retainAll(previousReadyForSubDown.keySet());
				previousReadyForSubDown.keySet().retainAll(readyForSubDownSecondFringes.keySet());
				
				if (printWordFringes) {
					String prevWord = wordLabels.get(prevIndex).toString(false);
					outputFile = new File(basePath + (prevIndex) + "_" + prevWord + "_subDownFirst_filtered");
					PrintWriter pw = FileUtil.getPrintWriter(outputFile);
					pw.println("SUB-DOWN-FIRST TABLE FILTERED [" + prevWord + "]");
					printDoubleTableArray(previousReadyForSubDown,pw);
					pw.close();
					
					String w = currentWordLabel.toString(false);
					outputFile = new File(basePath + i + "_" + w + "_subDownSecond_filtered");
					pw = FileUtil.getPrintWriter(outputFile);
					pw.println("SUB-DOWN-SECOND TABLE FILTERED [" + w + "]");
					printTable(readyForSubDownSecondFringes,pw,false);
					pw.close();
				}
				
				//calculateCombinatiosSubDown(i, readyForSubDownSecondFringes);
				
				//int prunedSubDown = 0;
				for(Entry<TermLabel, MultiFringe> e : readyForSubDownSecondFringes.entrySet()) {
					TermLabel subSite_Root = e.getKey();
					IdentityHashMap<TermLabel, ArrayList<MultiFringe>> matchingFringeArrayFirsTable = 
						previousReadyForSubDown.get(subSite_Root);					
					//if (matchingFringeArrayFirsTable!=null) {
					MultiFringe multiFringeSecond = e.getValue();
					totalFringesSubDownSecond[0][i]++;
					totalFringesSubDownSecond[1][i]+=multiFringeSecond.totalFringes();					
					for(ArrayList<MultiFringe> matchingFringeArrayFirst : matchingFringeArrayFirsTable.values()) {
						for(MultiFringe multiFringeFirst : matchingFringeArrayFirst) {
							if (multiFringeFirst.isSimpleMultiFringe()) {
								totalFringesSubDownFirstSimple[0][prevIndex]++;
								totalFringesSubDownFirstSimple[1][prevIndex]+=multiFringeFirst.totalFringes();
							}
							else {
								totalFringesSubDownFirstComplex[0][prevIndex]++;
								totalFringesSubDownFirstComplex[1][prevIndex]+=multiFringeFirst.totalFringes();
							}
							MultiFringe result = multiFringeFirst.subDown(multiFringeSecond);
							classifyFringe(result);							
						}							
					}											 				
					//}
				}
				//prunedFringesSubDown[i] += prunedSubDown;
			}
			
			subDownTimer.stop();
						
			
			//SUBUP
			subUpTimer.start();
			
			IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> readyForSubUpSecondFringes =
				subUpSecondFringes.get(currentWordLabel);
			
			
			if (readyForSubUpSecondFringes!=null) {
				readyForSubUpSecondFringes.keySet().retainAll(previousReadyForSubUp.keySet());
				previousReadyForSubUp.keySet().retainAll(readyForSubUpSecondFringes.keySet());
				
				if (printWordFringes) {				
					String prevWord = wordLabels.get(prevIndex).toString(false);
					outputFile = new File(basePath + (prevIndex) + "_" + prevWord + "_subUpFirst_filtered");
					PrintWriter pw = FileUtil.getPrintWriter(outputFile);
					pw.println("SUB-UP-FIRST TABLE FILTERED [" + prevWord + "]");
					printTableArray(previousReadyForSubUp,pw,false);
					pw.close();
					
					String w = currentWordLabel.toString(false);
					outputFile = new File(basePath + i + "_" + w + "_subUpSecond_filtered");
					pw = FileUtil.getPrintWriter(outputFile);
					pw.println("SUB-UP-SECOND TABLE FILTERED [" + w + "]");
					printDoubleTable(readyForSubUpSecondFringes,pw);
					pw.close();
				}
				
				for(Entry<TermLabel, IdentityHashMap<TermLabel, MultiFringe>> e : readyForSubUpSecondFringes.entrySet()) {
					TermLabel root_subSite = e.getKey();
					ArrayList<MultiFringe> matchingFringeFirstArray = previousReadyForSubUp.get(root_subSite);
					totalFringesSubUpFirst[0][prevIndex] += matchingFringeFirstArray.size();
					totalFringesSubUpFirst[1][prevIndex] += countTotalFringes(matchingFringeFirstArray);					
					for(MultiFringe multiFringeSecond : e.getValue().values()) {							
						totalFringesSubUpSecond[0][i]++;
						totalFringesSubUpSecond[1][i] += multiFringeSecond.totalFringes();							
						for(MultiFringe matchingFringeFirst : matchingFringeFirstArray) {
							MultiFringe result = matchingFringeFirst.subUp(multiFringeSecond);
							classifyFringe(result);								
						}
					}	
				}
			}
			
			//subUpCombinations[i] = subUpComb;
			subUpTimer.stop();
			
			//updateStatistics(i, newReadyForScan, newReadyForSubDownFirst, newReadyForSubUpFirst);						
			
			mergeSimpleComplexDouble(readyForScanSimple, readyForScanComplex);
			mergeSimpleComplexDouble(readyForSubDownFirstSimple, readyForSubDownFirstComplex);
			mergeSimpleComplex(readyForSubUpFirstSimple, readyForSubUpFirstComplex);						
			
			//updateAvgLengthReadyForSubDownFirst(i);
			                 
			//updateStatistics(i);
			
			wordTime[i] = (int)((currentWordTimer.stop())/1000);
			
			if (i==lastWordIndex) {				
				parsed = !readyForSubUpFirstComplex.isEmpty();					
				reachedTop = readyForSubUpFirstComplex.get(TOPnode)!=null;
				treeTimer.stop();
				finished = true;
			}
			else 
				parseIncrementallyNextWord(i+1);
			
		}

		private void initStats(int i) {
			totalFringesScan[0][i] = 0;
			totalFringesScan[1][i] = 0;
			totalFringesSubDownFirstSimple[0][i]=0;
			totalFringesSubDownFirstSimple[1][i]=0;
			totalFringesSubDownFirstComplex[0][i]=0;
			totalFringesSubDownFirstComplex[1][i]=0;
			totalFringesSubUpFirst[0][i] = 0;
			totalFringesSubUpFirst[1][i] = 0;
			totalFringesSubDownSecond[0][i+1] = 0;
			totalFringesSubDownSecond[1][i+1] = 0;
			totalFringesSubUpSecond[0][i+1] = 0;
			totalFringesSubUpSecond[1][i+1] = 0;
		}

		private void mergeSimpleComplex(
				IdentityHashMap<TermLabel, MultiFringe> tableSimple,
				IdentityHashMap<TermLabel, ArrayList<MultiFringe>> tableComplex) {
			
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
		
		private void mergeSimpleComplexDouble(
				IdentityHashMap<TermLabel,IdentityHashMap<TermLabel,MultiFringe>> tableSimple,
				IdentityHashMap<TermLabel,IdentityHashMap<TermLabel,ArrayList<MultiFringe>>> tableComplex) {
			
			for(Entry<TermLabel,IdentityHashMap<TermLabel,MultiFringe>> e : tableSimple.entrySet()) {
				TermLabel firstKey = e.getKey();
				
				IdentityHashMap<TermLabel,ArrayList<MultiFringe>> arrayTable = tableComplex.get(firstKey);
				if (arrayTable==null) {
					arrayTable = new IdentityHashMap<TermLabel,ArrayList<MultiFringe>>();
					tableComplex.put(firstKey, arrayTable);
				}
				
				mergeSimpleComplex(e.getValue(), arrayTable);
				
			}			
		}

		private void classifyFringe(MultiFringe multiFringe) {						
			if (multiFringe.isSimpleMultiFringe())
				classifySimpleFringe(multiFringe);
			else
				classifyComplexFringe(multiFringe);
		}

		private void classifySimpleFringe(MultiFringe simpleMultiFringe) {
			TermLabel firstLex = simpleMultiFringe.firstTerminalLabel; 
			assert (firstLex.isLexical);
			TermLabel root = simpleMultiFringe.rootLabel;
			for(Entry<TermLabel,TerminalMatrix> e : simpleMultiFringe.firstTerminalMultiSet.entrySet()) {
				TermLabel nextInLine = e.getKey();
				TerminalMatrix tm = e.getValue();
				if (nextInLine==MultiFringe.emptyTerminal)
					addInTableSimple(readyForSubUpFirstSimple, root, firstLex, nextInLine, null);
				else if (nextInLine.isLexical)
					addInDoubleTableSimple(readyForScanSimple, nextInLine, root, firstLex, nextInLine, tm);
				else
					addInDoubleTableSimple(readyForSubDownFirstSimple, nextInLine, root, firstLex, nextInLine, tm);
			}				 				
		}

		private void classifyComplexFringe(MultiFringe complexMultiFringe) {
			TermLabel firstTerm = complexMultiFringe.firstTerminalLabel; 
			assert (firstTerm.isLexical);
			TermLabel rootLabel = complexMultiFringe.rootLabel;
			
			for(Entry<TermLabel,TerminalMatrix> e : complexMultiFringe.firstTerminalMultiSet.entrySet()) {
				
				TermLabel nextInLine = e.getKey();

				if (nextInLine==MultiFringe.emptyTerminal) {
					LinkedList<TerminalsMultiSetTable> otherTerminalMultiSetCopy = 
							new LinkedList<TerminalsMultiSetTable>(complexMultiFringe.otherTerminalMultiSet);
					MultiFringe shiftFringe = new MultiFringe(rootLabel, firstTerm,
							otherTerminalMultiSetCopy.removeFirst(), otherTerminalMultiSetCopy);
					classifyFringe(shiftFringe);
				}
				else {					
					TerminalMatrix tm = e.getValue();//.clone();
					MultiFringe multiFringe = new MultiFringe(rootLabel, firstTerm);
					multiFringe.firstTerminalMultiSet.put(nextInLine, tm);
					multiFringe.otherTerminalMultiSet = complexMultiFringe.otherTerminalMultiSet;		
					if (multiFringe.tryToSimplify()) {
						classifySimpleFringe(multiFringe);
					}
					else {
						if (nextInLine.isLexical)
							addInTableComplex(readyForScanComplex, nextInLine, multiFringe);
						else
							addInTableComplex(readyForSubDownFirstComplex, nextInLine, multiFringe);
					}
				}	
			}
		}

		/*
		private void calculateCombinatiosSubDown(int i,  
				IdentityHashMap<TermLabel, MultiFringe> readyForSubDownSecond) {

			int subDownComb = 0;
			for(Entry<TermLabel, ArrayList<MultiFringe>> e : readyForSubDownFirst.entrySet()) {
				TermLabel subSite_Root = e.getKey();
				MultiFringe matchingFragsSecond = readyForSubDownSecond.get(subSite_Root);
				if (matchingFragsSecond!=null) {					
					subDownComb += e.getValue().size();					
				}
			}
			combinationsSubDown[i] = subDownComb;			
		}
		*/
		
		private int[] totalSizeSimpleComplex(
				IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> table) {
			int totalSimple = 0;
			int totalComplex = 0;
			for(IdentityHashMap<TermLabel, ArrayList<MultiFringe>> subTables : table.values()) {
				for(ArrayList<MultiFringe> a : subTables.values()) {
					for(MultiFringe mf : a) {
						if (mf.isSimpleMultiFringe())
							totalSimple++;
						else
							totalComplex++;
					}
				}
			}		
			return new int[]{totalSimple,totalComplex};
		}
		
		private int totalSize(IdentityHashMap<TermLabel, ArrayList<MultiFringe>> table) {
			int total = 0;
			for(ArrayList<MultiFringe> a : table.values()) {
				total += a.size();
			}			
			return total;
		}

		private int totalDoubleSize(IdentityHashMap<TermLabel, IdentityHashMap<TermLabel, ArrayList<MultiFringe>>> table) {
			int total = 0;
			for(IdentityHashMap<TermLabel, ArrayList<MultiFringe>> subTables : table.values()) {
				for(ArrayList<MultiFringe> a : subTables.values()) {
					total += a.size();
				}	
			}				
			return total;
		}


		
		
		private void printDebug() {
			synchronized(logFile) {
				Parameters.logLine("Sentence # " + index);
				Parameters.logLine(tree.toStringQtree());
				Parameters.logLine("Sentence Length: " + length);			
				Parameters.logLine(wordLabels.toString());		
				Parameters.logLine("Ready for Scan total (multifringes)                  " + Utility.formatArrayNumbersReadable(totalFringesScan[0]));
				Parameters.logLine("Ready for Scan total (fringes)                       " + Utility.formatArrayNumbersReadable(totalFringesScan[1]));
				Parameters.logLine("Ready for SubDownFirst simple (multifringes)         " + Utility.formatArrayNumbersReadable(totalFringesSubDownFirstSimple[0]));
				Parameters.logLine("Ready for SubDownFirst simple (fringes)              " + Utility.formatArrayNumbersReadable(totalFringesSubDownFirstSimple[1]));				
				Parameters.logLine("Ready for SubDownFirst complex (multifringes)        " + Utility.formatArrayNumbersReadable(totalFringesSubDownFirstComplex[0]));				
				Parameters.logLine("Ready for SubDownFirst complex (fringes)             " + Utility.formatArrayNumbersReadable(totalFringesSubDownFirstComplex[1]));
				//Parameters.logLine("Ready for SubDownFirst avg length " + Utility.formatArrayNumbersReadable(avgLengthSubDown));				
				Parameters.logLine("Ready for SubUpFirst total (multifringes)            " + Utility.formatArrayNumbersReadable(totalFringesSubUpFirst[0]));
				Parameters.logLine("Ready for SubUpFirst total (fringes)                 " + Utility.formatArrayNumbersReadable(totalFringesSubUpFirst[1]));
				Parameters.logLine("Ready for SubDownSecond total (multifringes)         " + Utility.formatArrayNumbersReadable(totalFringesSubDownSecond[0]));
				Parameters.logLine("Ready for SubDownSecond total (fringes)              " + Utility.formatArrayNumbersReadable(totalFringesSubDownSecond[1]));
				Parameters.logLine("Ready for SubUpSecond total (multifringes)           " + Utility.formatArrayNumbersReadable(totalFringesSubUpSecond[0]));				
				Parameters.logLine("Ready for SubUpSecond total (fringes)                " + Utility.formatArrayNumbersReadable(totalFringesSubUpSecond[1]));
				Parameters.logLine("Finished: " + finished);
				Parameters.logLine("Parsed: " + parsed);
				Parameters.logLine("Reached TOP: " + reachedTop);
				
				if (!finished)
					Parameters.logLine("Got stuck at word: " + wordLabels.get(currentWordIndex) + " (" + currentWordIndex + ")");
				long runningTime = treeTimer.getTotalTime();
				long runningTimeSec = runningTime / 1000;
				long otherTime = runningTime - scanTimer.getTotalTime() - subDownTimer.getTotalTime() - 
						subUpTimer.getTotalTime();
				Parameters.logLine("Running time (sec): " + runningTimeSec);
				if (runningTimeSec>1) {
					Parameters.logLine("Word running times (sec): " + Arrays.toString(wordTime));
					Parameters.logLine("\tof which scanning time: " + ((float)scanTimer.getTotalTime())/runningTime*100);
					Parameters.logLine("\tand combining subDown time: " + ((float)subDownTimer.getTotalTime())/runningTime*100);
					Parameters.logLine("\tand combining subUp time: " + ((float)subUpTimer.getTotalTime())/runningTime*100);				
					Parameters.logLine("\tand remaining time: " + ((float)otherTime)/runningTime*100);
				}
				Parameters.logLineFlush("");
			}
		}
		
	}
		
		
	private static void trasformFragFile(File fragmentsFile, File testTB) throws Exception {
		
		Scanner treeScanner = FileUtil.getScanner(testTB);
		HashSet<Label> lexicon = new HashSet<Label>(); 
		while(treeScanner.hasNextLine()) {
			TSNodeLabel tree = new TSNodeLabel(treeScanner.nextLine());
			lexicon.addAll(tree.collectLexicalLabels());
		}
		Scanner fragScanner = FileUtil.getScanner(fragmentsFile);
		ArrayList<TSNodeLabel> fragList = new ArrayList<TSNodeLabel>();
		while(fragScanner.hasNextLine()) {
			String line = fragScanner.nextLine().trim().split(" ",2)[1];
			if (line.startsWith("\"")) continue;
			if (line.startsWith("(")) return;
			line = line.replaceAll("\\\\", "").replaceAll("\\[\\.", "(").replaceAll(" \\]", ")");
			TSNodeLabel frag = new TSNodeLabel(line);
			ArrayList<TSNodeLabel> lexNodes = frag.collectLexicalItems();
			for(TSNodeLabel n : lexNodes) {
				if (!lexicon.contains(n.label))
					n.isLexical = false;
			}
			fragList.add(frag);
		}
		PrintWriter pw = FileUtil.getPrintWriter(fragmentsFile);
		for(TSNodeLabel frag : fragList) {
			pw.println(frag.toString(false, true));
		}
		pw.close();
		
	}

	public static void main(String[] args) throws Exception {
		
		treeLenghtLimit = -1;
		int threads = 1;		
		MultiFringe.maxCombinationsToSimplify = 2;
				
		//printFilteredFragmetnsToFile = true;
		
		///*
		//printWordFringes = true;
		String homePath = System.getProperty("user.home");
		basePath = homePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";
		//File testTB = new File(basePath + "wsj-22.mrg");
		//File testTB = new File(basePath + "wsj-22_time_faster.mrg");
		//File testTB = new File(basePath + "wsj-22_disclosed.mrg");
		File testTB = new File(basePath + "wsj-22_advertising.mrg");
		File fragmentsFile = new File(basePath + "lexFrags.mrg");
		basePath += "Debug/";
		Incremental_TSG_Parser_old ITSGP = new Incremental_TSG_Parser_old(fragmentsFile, testTB, threads);
		//String sentece = "This time around , they 're moving even faster .";		
		//ITSGP.printLexFrags(sentece.split(" "));		
		ITSGP.run();
		//*/
		
		/*
		//for(int maxComb : new int[]{100}) {
			//MultiFringe.maxCombinationsToSimplify = maxComb;
			//System.out.println("MaxCombinationsToSimplify: " + maxComb);
			long start = System.currentTimeMillis();
			File testTB = new File(args[0]);
			File fragmentsFile = new File(args[1]);		
			threads = ArgumentReader.readIntOption(args[2]);		
			for(int j : new int[]{-1}) { //new int[]{5,10,20,40,-1}			
				treeLenghtLimit = j;
				new Incremental_TSG_Parser(fragmentsFile, testTB, threads).run();
				System.out.println();
			}
			long end = System.currentTimeMillis();
			System.out.println("Took (sec): " + (end-start)/1000);
		//}
		*/		
		
	}

	
}
