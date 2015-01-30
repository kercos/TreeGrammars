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

public class Incremental_TSG_Parser extends Thread {
		
	static final TermLabel TOPnode = TermLabel.getTermLabel(Label.getLabel("TOP"), false);
	static final boolean debug = true;
	
	static int treeLenghtLimit = 10;		
	static long maxMsecPerWord = 60*1000;
	static long threadCheckingTime = 20000;
	static String basePath = "./";
	static File logFile;
	static File filteredFragmentsFile;
	static boolean printFilteredFragmetnsToFile = false;
	static boolean printWordFringes = false;
	static int printWordFringesIndex = 0;
	static String scratchDebugPath = "/disk/scratch/fsangati/Debug/";
	static boolean tryToRemoveDuplicates = true;
	//static boolean pruneFringes = true;
	//static int pruneFringeLength = 10;
	static int oneTestSentenceIndex = -1;
	static boolean doNotInterrupt = false;
	static boolean removeFringesNonCompatibleWithSentence = false;
	static boolean addMinimalFragments = true;
	static boolean extendedMinimal = false;
	static int restricFragmentsWithFreqGreaterThan = -1;
	static int removeFragsWithSubSitesNumbGreaterThan = -1;
		
	ArrayList<TSNodeLabel> trainTB, testTB;
	HashSet<TSNodeLabel> 	fragSet = new HashSet<TSNodeLabel>(),
							lexTest = new HashSet<TSNodeLabel>();	
	Iterator<TSNodeLabel> testTBIterator;
	HashMap<TermLabel, HashSet<TSNodeLabel>> lexiconPosRuleTest;
	HashMap<TermLabel, HashSet<TermLabel>> posLexTable
		= new HashMap<TermLabel, HashSet<TermLabel>>();
	HashMap<TermLabel, HashSet<TermLabel>> nonPosFirstLexTable
		= new HashMap<TermLabel, HashSet<TermLabel>>();
	int treebankIteratorIndex;
	File fragmentsFile, outputFile;
	
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

	int threads;
	PrintProgress progress;	
	int parsedTrees, reachedTopTrees, interruptedTrees;
	
	public Incremental_TSG_Parser(File trainingTBfile, File fragmentsFile, 
			File testFile, int threads) throws Exception {
		this(Wsj.getTreebank(trainingTBfile), fragmentsFile, Wsj.getTreebank(testFile), threads);		
	}
	
	public Incremental_TSG_Parser (ArrayList<TSNodeLabel> trainTB, 
			File fragmentsFile, ArrayList<TSNodeLabel> testTB, int threads) {
		this.trainTB = trainTB;
		this.testTB = testTB;
		this.fragmentsFile = fragmentsFile;
		this.threads = threads;		
		logFile = new File(basePath + "log.txt");
		filteredFragmentsFile = new File(basePath + "lexFrags.mrg");
		readTreeBanksAndFragments();
	}
	
	private void readTreeBanksAndFragments() {
		if (debug)
			Parameters.openLogFile(logFile);								
		printParameters();		
		readLexiconFromTrainingTreebank();		
		readLexiconFromTestTreebank();
		filterTestTreebank();
		readFragmentsFile();
		addMinimalFragmentsFromTrainTB();
		addFragmentPosTest();
		printFragmentsToFile();		
		MultiFringe.posSet = posLexTable.keySet();
		MultiFringe.posSetLexTable = posLexTable;
	}
	
	public void run() {		
		
		testTBIterator = testTB.iterator();
		
		if (doNotInterrupt)
			parseIncrementalOneThread();
		else
			parseIncremental();
		
			Parameters.reportLineFlush("Number of trees: " + testTB.size());
		Parameters.reportLineFlush("Number of finished analysis: " + (testTB.size()-interruptedTrees));
		Parameters.reportLineFlush("Number of interrupted analysis: " + interruptedTrees);
		Parameters.reportLineFlush("Number of successfully parsed trees: " + parsedTrees);		
		Parameters.reportLineFlush("... of which reached top: " + reachedTopTrees);				
		if (debug)
			Parameters.closeLogFile();
	}
	
	private void printParameters() {
		Parameters.reportLineFlush("");
		Parameters.reportLineFlush("Tree Lenght Limit: " + treeLenghtLimit);
		Parameters.reportLineFlush("Max sec Per Word: " + maxMsecPerWord/1000);
		Parameters.reportLineFlush("Trying to remove duplicates: " + tryToRemoveDuplicates);		
		Parameters.reportLineFlush("Number of threads: " + threads);
		Parameters.reportLineFlush("Max comb. when simplifying: " + MultiFringe.maxCombinationsToSimplify);
		Parameters.reportLineFlush("Remove frags not compatible with sentence: " + removeFringesNonCompatibleWithSentence);
		Parameters.reportLineFlush("Add minimal fragmetns from training TB: " + addMinimalFragments);
		Parameters.reportLineFlush("Extended minimal: " + extendedMinimal);
		Parameters.reportLineFlush("Restricting fragmetns with freq greater than: " + restricFragmentsWithFreqGreaterThan);
		Parameters.reportLineFlush("Remove Frags with subsites number greater than: " + removeFragsWithSubSitesNumbGreaterThan);
		//Parameters.reportLineFlush("Prune Fringes: " + pruneFringes + 
		//		(pruneFringes ? " (max yield: " + pruneFringeLength + ")" : ""));
		
	}

	private void filterTestTreebank() {
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

	private void readLexiconFromTestTreebank() {
		Parameters.reportLineFlush("Reading Lexicon from test treebank...");
		lexiconPosRuleTest = new HashMap<TermLabel, HashSet<TSNodeLabel>>();
		
		for(TSNodeLabel t : testTB) {
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			for(TSNodeLabel l : lexItems) {
				lexTest.add(l);
				TermLabel word = TermLabel.getTermLabel(l);
				TSNodeLabel posRule = l.parent.clone();
				TermLabel pos =  TermLabel.getTermLabel(l.parent);
				put(lexiconPosRuleTest, word, posRule);
				put(posLexTable,pos, word);
			}
		}		
	}
	
	private void readLexiconFromTrainingTreebank() {
		Parameters.reportLineFlush("Reading Lexicon from training treebank...");
		for(TSNodeLabel t : trainTB) {
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			for(TSNodeLabel l : lexItems) {
				TermLabel word = TermLabel.getTermLabel(l);
				TermLabel pos = TermLabel.getTermLabel(l.parent);
				put(posLexTable, pos, word);
			}
		}		
	}
	
	private void readFragmentsFile() {		
		if (fragmentsFile==null) {
			Parameters.reportLineFlush("Empty Fragment File...");
			return;
		}
		boolean restrictFreq = restricFragmentsWithFreqGreaterThan>0;
		Parameters.reportLineFlush("Reading Fragments File...");	
		Scanner fragmentScan = FileUtil.getScanner(fragmentsFile);
		int totalFragments = 0;
		int lexFirstCount = 0;
		int lexSecondCount = 0;
		int prunedFrags = 0;
		progress = new PrintProgress("Reading Fragment", 10000, 0);
		while(fragmentScan.hasNextLine()) {
			totalFragments++;
			progress.next();
			String line = fragmentScan.nextLine();
			String[] treeFreq = line.split("\t");
			if (restrictFreq && Integer.parseInt(treeFreq[1])<=restricFragmentsWithFreqGreaterThan) {
				continue;
			}
			String fragmentString = treeFreq[0];
			try {
				TSNodeLabel fragment = new TSNodeLabel(fragmentString, false);
				//if (removeFringesNonCompatibleWithSentence &&
				//		!lexTest.containsAll(fragment.collectLexicalItems()))
				//	continue;
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
					int subSitesNumber = 0;
					for(TSNodeLabel n : terminalNodes) {
						TermLabel t = TermLabel.getTermLabel(n);
						termLabels.add(t);
						if (!t.isLexical) 
							subSitesNumber++;
					}
					
					if (removeFragsWithSubSitesNumbGreaterThan!=-1 &&
							subSitesNumber>removeFragsWithSubSitesNumbGreaterThan)
						continue;
					
					int lexNodeIndex = termLabels.indexOf(lexLabel);
					if (lexNodeIndex>1) continue; //more than 1 sub site to the left of leftmost lex
					
					if (printFilteredFragmetnsToFile)
						fragSet.add(fragment);
					
					TermLabel rootLabel = TermLabel.getTermLabel(fragment);
					put(nonPosFirstLexTable,rootLabel,lexLabel);
					TermLabel firstTerminal = termLabels.removeFirst();
					if (firstTerminal.isLexical) {
						if (addFirstLexFringe(firstTerminal, rootLabel, termLabels))
							lexFirstCount++;							
					}
					else {
						if (addFirstSubFringe(lexLabel, firstTerminal, rootLabel, termLabels))
							lexSecondCount++;						
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
		Parameters.reportLineFlush("Pruned Fragments for yield length limit: " + prunedFrags);		
		Parameters.reportLineFlush("Total valid fringes: " + (lexFirstCount+lexSecondCount));
		Parameters.reportLineFlush("\tlex first: " + lexFirstCount);
		Parameters.reportLineFlush("\tsub first: " + lexSecondCount);
		
	}

	//static final int[] firstSecondLexLimit = new int[]{2,1};	
	
	private void addMinimalFragmentsFromTrainTB() {
		if (!addMinimalFragments)
			return;
		PrintProgress pp = new PrintProgress("Adding minimal fragments from training treebank (" + 
				trainTB.size() + ")");
		int newFrags = 0;
		int[] firstSecondFrags = new int[2];
		for(TSNodeLabel t : trainTB) {
			pp.next();
			ArrayList<TSNodeLabel> wordNodes = t.collectLexicalItems();
			for(TSNodeLabel lexNode : wordNodes) {				
				//if (removeFringesNonCompatibleWithSentence && !lexTest.contains(lexNode)) 
				//	continue;
				TSNodeLabel p = lexNode.parent;
				TSNodeLabel d = lexNode;
				TSNodeLabel dFrag = d.clone();
				int lexIndex = 0;
				int[] firstSecondLex = new int[2]; 
				boolean branching = false;
				do {
					if (p.prole()>1)
						branching = true;
					int dIndex = p.indexOfDaughter(d);
					lexIndex += dIndex;
					if (lexIndex>1)
						break;	
					TSNodeLabel frag = p.cloneCfg();
					frag.daughters[dIndex] = dFrag;
					dFrag.parent = frag;
					//firstSecondLex[lexIndex]!=firstSecondLexLimit[lexIndex] && 
					if (addLexFrag(lexNode, frag.clone())) {						
						firstSecondFrags[lexIndex]++;
						newFrags++;
						firstSecondLex[lexIndex]++;
						//if (firstSecondLex[0]==firstSecondLexLimit[0] &&
						//		firstSecondLex[1]==firstSecondLexLimit[1])
						//	break;
					}
					if (!extendedMinimal && branching)
						break;	
					d = p;
					p = p.parent;
					dFrag = frag;
				} while(p!=null);
			}
		}
		pp.end();
		Parameters.reportLineFlush("New lex fringes: " + newFrags);
		Parameters.reportLineFlush("\t lex first:" + firstSecondFrags[0]);
		Parameters.reportLineFlush("\t lex second:" + firstSecondFrags[1]);		
	}

	private boolean addLexFrag(TSNodeLabel lexNode, TSNodeLabel fragment) {
		
		if (removeFragsWithSubSitesNumbGreaterThan!=-1) {
			int subSiteNumber = fragment.countTerminalNonLexNodes();
			if (subSiteNumber>removeFragsWithSubSitesNumbGreaterThan)
				return false;
		}
		
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
		//int lexNodeIndex = termLabels.indexOf(lexLabel);
		//if (lexNodeIndex>1) continue; //more than 1 sub site to the left of leftmost lex
		
		if (printFilteredFragmetnsToFile)
			fragSet.add(fragment);
		
		TermLabel rootLabel = TermLabel.getTermLabel(fragment);
		put(nonPosFirstLexTable,rootLabel,lexLabel);
		TermLabel firstTerminal = termLabels.removeFirst();
		if (firstTerminal.isLexical) {	
			return addFirstLexFringe(firstTerminal, rootLabel, termLabels);
		}
		return addFirstSubFringe(lexLabel, firstTerminal, rootLabel, termLabels);
	}

	private static <T> boolean put(HashMap<TermLabel, HashSet<T>> table,
			TermLabel key, T value) {
		HashSet<T> present = table.get(key);
		if (present==null) {
			present = new HashSet<T>();
			table.put(key, present);
		}
		return present.add(value);
	}
	

	static TermLabel thisLabel = TermLabel.getTermLabel("This", true);
	
	private boolean addFirstLexFringe(			
			TermLabel firstLex, TermLabel rootLabel,
			LinkedList<TermLabel> otherTermLabels) {
		
		MultiFringe multiFringe = null;
		HashMap<TermLabel, MultiFringe> lexFringe = firstLexFringes.get(firstLex);
		
		if (lexFringe==null) {
			lexFringe = new HashMap<TermLabel, MultiFringe>();
			firstLexFringes.put(firstLex,lexFringe);						
		}
		else {
			multiFringe = lexFringe.get(rootLabel);
		}
		if (multiFringe == null) {
			multiFringe = new MultiFringe(rootLabel, firstLex);
			lexFringe.put(rootLabel, multiFringe);
		}
		return multiFringe.addFringe(otherTermLabels);
	}
	
	private boolean addFirstSubFringe(
			TermLabel lexLabel, TermLabel firstTermLabel, TermLabel rootLabel,
			LinkedList<TermLabel> otherTermLabels) {
		
		HashMap<TermLabel, MultiFringe> multiFringeTable = null;
		HashMap<TermLabel, HashMap<TermLabel, MultiFringe>> 
			lexFringe = firstSubFringes.get(lexLabel);
		
		if (lexFringe==null) {
			lexFringe = new HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>();
			firstSubFringes.put(lexLabel,lexFringe);						
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
		return multiFringe.addFringe(otherTermLabels);
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
	
	private void addFragmentPosTest() {
		int newWordPos = 0;
		for(Entry<TermLabel, HashSet<TSNodeLabel>> e : lexiconPosRuleTest.entrySet()) {
			TermLabel word = e.getKey();
			HashSet<TSNodeLabel> posRuleSet = e.getValue();
			LinkedList<TermLabel> termLabels = new LinkedList<TermLabel>(); 
			for(TSNodeLabel posRule : posRuleSet) {
				TermLabel posLabel = TermLabel.getTermLabel(posRule);
				if (addFirstLexFringe(word, posLabel, termLabels)) {					
					newWordPos++;
					if (printFilteredFragmetnsToFile)
						fragSet.add(posRule);
				}
			}
		}
		Parameters.reportLineFlush("Adding total unseen Pos-Word pairs in gold: " + newWordPos);
		
	}


	private void printFragmentsToFile() {
		if (printFilteredFragmetnsToFile) {
			Parameters.reportLineFlush("Printing Fragments to " + filteredFragmentsFile  + "...");
			PrintWriter pw = FileUtil.getPrintWriter(filteredFragmentsFile);
			for(TSNodeLabel frag : fragSet) {
				pw.println(frag.toString(false, true));
			}			
			pw.close();
		}		
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
		int index;
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
				index = i[0];
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
			HashMap<TermLabel, MultiFringe> subTable = firstLexFringes.get(word);
			if (subTable==null) {
				firstLexFringesWords.add(null);
				return;
			}
			HashMap<TermLabel, MultiFringe> subTableLocal = 
				new HashMap<TermLabel, MultiFringe>();
			
			if (removeFringesNonCompatibleWithSentence)
				filterFringes(restOfSentenceLexicon, subTable, subTableLocal);
			else
				cloneTable(subTable, subTableLocal);
			
			firstLexFringesWords.add(subTableLocal.isEmpty() ? null : subTableLocal);
		}
		
		private void buildFirstSubFringes(LinkedList<TermLabel> restOfSentenceLexicon) {
			
			TermLabel word = restOfSentenceLexicon.getFirst();
			HashMap<TermLabel, 
				HashMap<TermLabel, MultiFringe>> subTable = firstSubFringes.get(word);
			if (subTable==null) {
				firstSubFringesWords.add(null);
				return;
			}
			
			HashMap<TermLabel, 
			HashMap<TermLabel, MultiFringe>> subTableLocal =
				new HashMap<TermLabel, HashMap<TermLabel, MultiFringe>>();
			
			
			for(Entry<TermLabel, HashMap<TermLabel, MultiFringe>> e : subTable.entrySet()) {
				HashMap<TermLabel, MultiFringe> subSubTable = e.getValue();
				HashMap<TermLabel, MultiFringe> subSubTableLocal = 
					new HashMap<TermLabel, MultiFringe>();
				if (removeFringesNonCompatibleWithSentence)
					filterFringes(restOfSentenceLexicon, subSubTable, subSubTableLocal);					
				else
					cloneTable(subSubTable, subSubTableLocal);
				if (!subSubTableLocal.isEmpty())
					subTableLocal.put(e.getKey(), subSubTableLocal);
			}
			
			firstSubFringesWords.add(subTableLocal.isEmpty() ? null : subTableLocal);
				
		}

		private void filterFringes(
				LinkedList<TermLabel> restOfTheSentence,
				HashMap<TermLabel, MultiFringe> tableFringes,
				HashMap<TermLabel, MultiFringe> tableFringesFiltered) {
			
			for(Entry<TermLabel, MultiFringe> e : tableFringes.entrySet()) {
				MultiFringe mf = e.getValue().clone();
				int[] totRem = mf.firstTerminalMultiSet.removeNonCompatibleFringes(restOfTheSentence);
				totalInitLexFringes+= totRem[0];
				totalInitLexFringesRemoved += totRem[1];
				if (!mf.firstTerminalMultiSet.isEmpty()) {
					addInTableSimple(tableFringesFiltered, e.getKey(), mf);
				}
			}
			
		}
		
		private void cloneTable(HashMap<TermLabel, MultiFringe> table,
				HashMap<TermLabel, MultiFringe> clonedTable) {
			for(Entry<TermLabel, MultiFringe> e : table.entrySet()) {
				MultiFringe mf = e.getValue().clone();
				totalInitLexFringes+= mf.totalFringes();
				clonedTable.put(e.getKey(),mf);
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
			
			if (printWordFringes && printWordFringesIndex==index) {
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
			
			if (printWordFringes && printWordFringesIndex==index)			
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
			
			if (printWordFringes && printWordFringesIndex==index)
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
				
				outputFile = new File(scratchDebugPath + (nextWordIndex) + "_" + nextWord + "_scan_filtered");
				pw = FileUtil.getPrintWriter(outputFile);
				pw.println("SCAN TABLE [" + nextWord + "]");
				printTableArray(scanFringesComplex,pw,false);
				pw.close();
								
			}
			
			if (currentWordIndex!=0) {
				outputFile = new File(scratchDebugPath + currentWordIndex + "_" + currentWord + "_subDownSecond_filtered");
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
			
			 		
			outputFile = new File(scratchDebugPath + (currentWordIndex) + "_" + currentWord + "_subDownFirst_filtered");
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
				Parameters.logLine("Sentence # " + index);
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
		
		printFilteredFragmetnsToFile = false;
		printWordFringes = false;
		printWordFringesIndex = -1;
		doNotInterrupt = false;
		oneTestSentenceIndex = -1;
		
		maxMsecPerWord = 2*60*1000;
		treeLenghtLimit = 20;		
		int threads = 1;
		MultiFringe.tryToSimplify = true;
		MultiFringe.maxCombinationsToSimplify = 1000;
		tryToRemoveDuplicates = true;		
		removeFringesNonCompatibleWithSentence = true;
		addMinimalFragments = true;		
		extendedMinimal = true;		
		restricFragmentsWithFreqGreaterThan = -1;		
		removeFragsWithSubSitesNumbGreaterThan = -1;		
		
		
		long start = System.currentTimeMillis();
		
		String homePath = System.getProperty("user.home");
		basePath = homePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";		
		File trainTB = new File(basePath + "wsj-02-21.mrg");
		
		File testTB = new File(basePath + "wsj-22.mrg");
		//File testTB = new File(basePath + "wsj-22_time_faster.mrg");
		//File testTB = new File(basePath + "wsj-22_disclosed.mrg");
		//File testTB = new File(basePath + "wsj-22_advertising.mrg");
		//File testTB = new File(basePath + "wsj-22_law_averages.mrg");
		//File testTB = new File(basePath + "wsj-22_bill_intends.mrg");
		//File testTB = new File(basePath + "wsj-22_shortage.mrg");
		//File testTB = new File(basePath + "wsj-22_Ways.mrg");
		//File testTB = new File(basePath + "wsj-22_parents.mrg");
		//File testTB = new File(basePath + "wsj-22_CALL.mrg");
		
		File fragmentsFile = null;
		//File fragmentsFile = new File(basePath + "lexFrags.mrg");
		//File fragmentsFile = new File(basePath + "fragments_approxFreq.txt");
		
		basePath += "Debug/";
		//for (int threshold : new int[]{20,10,5,3,2,-1}) {
			//restricFragmentsWithFreqGreaterThan = threshold;
			Incremental_TSG_Parser ITSGP = new Incremental_TSG_Parser(
				trainTB, fragmentsFile, testTB, threads);
			//String sentece = "This time around , they 're moving even faster .";		
			//ITSGP.printLexFrags(sentece.split(" "));		
			ITSGP.run();
		//}
		//*/
		
		/*
		//for(int maxComb : new int[]{100}) {
			//MultiFringe.maxCombinationsToSimplify = maxComb;
			//System.out.println("MaxCombinationsToSimplify: " + maxComb);
			File testTB = new File(args[0]);
			File fragmentsFile = new File(args[1]);		
			threads = ArgumentReader.readIntOption(args[2]);		
			for(int j : new int[]{-1}) { //new int[]{5,10,20,40,-1}			
				treeLenghtLimit = j;
				new Incremental_TSG_Parser(fragmentsFile, testTB, threads).run();
				System.out.println();
			}			
		//}
		*/	
		
		long end = System.currentTimeMillis();
		System.out.println("Took (sec): " + (end-start)/1000);
		
	}

	
}
