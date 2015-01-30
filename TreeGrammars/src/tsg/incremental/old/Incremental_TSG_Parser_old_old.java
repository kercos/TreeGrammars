package tsg.incremental.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.ArgumentReader;
import util.PrintProgress;
import util.Timer;
import util.Utility;
import util.file.FileUtil;

public class Incremental_TSG_Parser_old_old extends Thread {
	
	static int treeLenghtLimit = 10;
	static boolean debug = true;	
	static long maxMsecPerTree = 5*60*1000;
	static long threadCheckingTime = 10000;
	static File logFile = new File("./log.txt");
	static File filteredFragmentsFile = new File("./tmp/lexFrags.mrg");
	static boolean printFilteredFragmetnsToFile = false;
	static boolean pruneFringes = false;
	static int pruneFringeLength = 10;
		
	ArrayList<TSNodeLabel> testTB;	
	Iterator<TSNodeLabel> treebankIterator;
	HashMap<Label, HashSet<TSNodeLabel>> lexiconPosRule;
	int treebankIteratorIndex;
	File fragmentsFile, outputFile;
	IdentityHashMap<Label, ArrayList<TSNodeLabel>> firstLexFragmentsTable;
	IdentityHashMap<Label, ArrayList<TSNodeLabel>> firstSubFragmentsTable;
	int threads;
	PrintProgress progress;	
	int parsedTrees, interruptedTrees;
	
	public Incremental_TSG_Parser_old_old(File fragmentsFile, File testFile, int threads) throws Exception {
		this(fragmentsFile, Wsj.getTreebank(testFile), threads);		
	}
	
	public Incremental_TSG_Parser_old_old (File fragmentsFile, ArrayList<TSNodeLabel> testTB, int threads) {
		this.testTB = testTB;
		this.fragmentsFile = fragmentsFile;
		this.threads = threads;
		treebankIterator = testTB.iterator();		
	}
	
	public void run() {
		if (debug)
			Parameters.openLogFile(logFile);
		printParameters();
		filterTreebank();
		readLexiconFromTreebank();
		readFragmentsFile();
		addFragmentPos();
		printFragmentsToFile();
		//if (threads==1)
		//	parseIncrementalOneThread();
		//else
			parseIncremental();
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
		Parameters.reportLineFlush("Number of parsed trees: " + parsedTrees);
		Parameters.reportLineFlush("Number of interruted trees: " + interruptedTrees);
		if (debug)
			Parameters.closeLogFile();
	}
	
	private void printParameters() {
		Parameters.reportLineFlush("");
		Parameters.reportLineFlush("Tree Lenght Limit: " + treeLenghtLimit);
		Parameters.reportLineFlush("Max sec Per Tree: " + maxMsecPerTree/1000);
		Parameters.reportLineFlush("Number of threads: " + threads);
		Parameters.reportLineFlush("Prune Fringes: " + pruneFringes + 
				(pruneFringes ? " (max yield: " + pruneFringeLength + ")" : ""));
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
		lexiconPosRule = new HashMap<Label, HashSet<TSNodeLabel>>();
		for(TSNodeLabel t : testTB) {
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			for(TSNodeLabel l : lexItems) {
				Label word = l.label;
				TSNodeLabel posRule = l.parent.clone();
				HashSet<TSNodeLabel> wordPosRuleSet = lexiconPosRule.get(word);
				if (wordPosRuleSet==null) {
					wordPosRuleSet = new HashSet<TSNodeLabel>();
					lexiconPosRule.put(word, wordPosRuleSet);
				}
				wordPosRuleSet.add(posRule);
			}
		}		
		//Parameters.reportLineFlush("Total words in gold lexicon: " + lexiconPosRule.size());
	}
	

	private void readFragmentsFile() {		
		Parameters.reportLineFlush("Reading Fragments File...");	
		firstLexFragmentsTable = new IdentityHashMap<Label, ArrayList<TSNodeLabel>>();
		firstSubFragmentsTable = new IdentityHashMap<Label, ArrayList<TSNodeLabel>>();
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
					ArrayList<Label> terminals = fragment.collectTerminalLabels();
					if (pruneFringes) {
						if (terminals.size()>pruneFringeLength) {
							prunedFrags++;
							continue;
						}
					}					
					Label lexLabel = lexNode.label;
					//if (!lexiconPosRule.keySet().containsAll(words)) continue;					
					int lexNodeIndex = terminals.indexOf(lexLabel);
					if (lexNodeIndex>1) continue; //more than 1 sub site to the left of leftmost lex
					/*
					ArrayList<TSNodeLabel> terms = fragment.collectTerminalItems();
					int numSubSitesBeforeLex = 0;
					for(TSNodeLabel t : terms) {
						if (t==lexNode) break;
						if (++numSubSitesBeforeLex==2)
							continue;			//more than 1 sub site to the left of leftmost lex			
					}
					*/
					totalLexicalizedFragments++;
					
					IdentityHashMap<Label, ArrayList<TSNodeLabel>> lexFragmentsTable = null;
					//if (terms.get(0).isLexical) {
					if (lexNodeIndex==0) {
						initalFrags++;	
						lexFragmentsTable = firstLexFragmentsTable;
					}
					else {
						nextFrags++;
						lexFragmentsTable = firstSubFragmentsTable;
					}
															
					//Label lexLabel = lexNode.label;
					ArrayList<TSNodeLabel> lexFragList = lexFragmentsTable.get(lexLabel);
					if (lexFragList==null) {
						lexFragList = new ArrayList<TSNodeLabel>();
						lexFragmentsTable.put(lexLabel,lexFragList);
					}
					lexFragList.add(fragment);
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
		Parameters.reportLineFlush("Pruned Fragments for Fringe_old length limit: " + prunedFrags);		
		Parameters.reportLineFlush("Total valid Lexicalized Fragments: " + totalLexicalizedFragments);
		Parameters.reportLineFlush("of which lex first: " + initalFrags);
		Parameters.reportLineFlush("and sub first: " + nextFrags);
		
	}
	
	private void addFragmentPos() {
		int newWordPos = 0;
		for(Entry<Label, HashSet<TSNodeLabel>> e : lexiconPosRule.entrySet()) {
			Label word = e.getKey();
			HashSet<TSNodeLabel> posRuleSet = e.getValue();
			ArrayList<TSNodeLabel> wordFrags = firstLexFragmentsTable.get(word);
			if (wordFrags==null) {
				wordFrags = new ArrayList<TSNodeLabel>();
				firstLexFragmentsTable.put(word, wordFrags);
			}
			for(TSNodeLabel posRule : posRuleSet) {
				if (!wordFrags.contains(posRule)) {
					wordFrags.add(posRule);
					newWordPos++;
				}	
			}
		}
		Parameters.reportLineFlush("Adding total unseen Pos-Word pairs in gold: " + newWordPos);
		
	}

	private void printFragmentsToFile() {
		if (printFilteredFragmetnsToFile) {
			PrintWriter pw = FileUtil.getPrintWriter(filteredFragmentsFile);
			for(ArrayList<TSNodeLabel> fragList : firstLexFragmentsTable.values()) {
				for(TSNodeLabel f : fragList) {
					pw.println(f.toString(false, true));					
				}
			}			
			for(ArrayList<TSNodeLabel> fragList : firstSubFragmentsTable.values()) {
				for(TSNodeLabel f : fragList) {
					pw.println(f.toString(false, true));					
				}
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
					long runningTime = t.treeTimer.getEllapsedTime();
					//System.out.println("Thread " + i + ": "  + runningTime/1000);					 
					if (runningTime > maxMsecPerTree) {						
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
	
	/*
	private synchronized TSNodeLabel getNextTree(int[] index,
			IdentityHashMap<Label, ArrayList<TSNodeLabel>> firstLexFragmentsSmallTable,
			IdentityHashMap<Label, ArrayList<TSNodeLabel>> firstSubFragmentsSmallTable) {
		if (!treebankIterator.hasNext()) 
			return null;			
		index[0] = treebankIteratorIndex++;
		TSNodeLabel t = treebankIterator.next();
		
		firstLexFragmentsSmallTable.clear();
		firstSubFragmentsSmallTable.clear();
		ArrayList<Label> lexLabels = t.collectLexicalLabels();
		for(Label w : lexLabels) {
			ArrayList<TSNodeLabel> wordFragsFirstLex = firstLexFragmentsTable.get(w);
			firstLexFragmentsSmallTable.put(w, wordFragsFirstLex);
			ArrayList<TSNodeLabel> wordFragsFirstSub = firstSubFragmentsTable.get(w);
			firstSubFragmentsSmallTable.put(w, wordFragsFirstSub);
		}
		return t;
	}
	*/
	
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
		Timer treeTimer, scanTimer, subDownTimer, subUpTimer, computeNextFringesTimer, fragmentReadingTimer;
		//long startTime, stopTime; 		
		//long scanTime, subDownTime, subUpTime, computingNextFringesTime, fragmentsReadingTime;
		TSNodeLabel tree;
		Vector<Label> wordLabels;
		int length, lastWordIndex;
		boolean parsed;
		boolean finished = false;
		int[] totalFringesTable, fringesForScanTable, fringesForSubDownTable, fringesForSubUpTable;
		long[] subDownCombinations, subUpCombinations;
		int[] fringesForSubDownFirst, fringesForSubDownSecond, fringesForSubUpSecond;
		int[] prunedFringesSubDown;
		int[] wordTime;
		//IdentityHashMap<Label, ArrayList<TSNodeLabel>> firstLexFragmentsSmallTable =
		//		new IdentityHashMap<Label, ArrayList<TSNodeLabel>>();
		//IdentityHashMap<Label, ArrayList<TSNodeLabel>> firstSubFragmentsSmallTable =
		//		new IdentityHashMap<Label, ArrayList<TSNodeLabel>>();
		
		public void run() {
			TSNodeLabel t = null;
			int[] i = {0};
			//while ( (t = getNextTree(i,firstLexFragmentsSmallTable,firstSubFragmentsSmallTable)) != null) {
			while ( (t = getNextTree(i)) != null) {
				index = i[0];
				initTree(t);
				parseIncrementally();
				synchronized(progress) {
					if (parsed) parsedTrees++;
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
			computeNextFringesTimer = new Timer();
			fragmentReadingTimer = new Timer();
			this.tree = t;
			this.wordLabels = new Vector<Label>(tree.collectLexicalLabels());			
			this.length = wordLabels.size();
			this.lastWordIndex = length-1;
			this.parsed = false;
			this.finished = false;
			this.totalFringesTable = new int[length];
			this.fringesForScanTable = new int[length];
			this.fringesForSubDownTable = new int[length]; 
			this.fringesForSubUpTable = new int[length];
			this.fringesForSubDownFirst = new int[length];
			this.fringesForSubDownSecond = new int[length];
			this.fringesForSubUpSecond = new int[length];
			this.subDownCombinations = new long[length];
			this.subUpCombinations = new long[length];
			this.prunedFringesSubDown = new int[length];
			this.wordTime = new int[length];
			Arrays.fill(totalFringesTable,-1);
			Arrays.fill(fringesForScanTable,-1);
			Arrays.fill(fringesForSubDownTable,-1);
			Arrays.fill(fringesForSubUpTable,-1);
			Arrays.fill(fringesForSubDownFirst,-1);			
			Arrays.fill(fringesForSubDownSecond,-1);
			Arrays.fill(fringesForSubUpSecond,-1);
			Arrays.fill(subDownCombinations,-1);
			Arrays.fill(subUpCombinations,-1);
			Arrays.fill(prunedFringesSubDown,-1);
			Arrays.fill(wordTime,-1);
		}
		
		private void outOfTime() {
			this.stop();
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
			computeNextFringesTimer.stop();
			fragmentReadingTimer.stop();
		}
		
		private void parseIncrementally() {			
			
			treeTimer.start();
			Timer wordTimer = new Timer(true);
					
			Label initWordLabel = wordLabels.get(0);	
			ArrayList<TSNodeLabel> initWordFrags = getLexFragments(initWordLabel, true);			
			
			IdentityHashMap<Label, HashSet<Fringe_old>> readyForScan = new IdentityHashMap<Label, HashSet<Fringe_old>>(); 
			// scan fringes indexed on secondTerminal
			
			IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubDownFirst = new IdentityHashMap<Label, HashSet<Fringe_old>>(); 
			// firstLexNextSub fringes indexed on secondTerminal
			
			IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubUpFirst = new IdentityHashMap<Label, HashSet<Fringe_old>>(); 
			// empty fringes indexed on root		
			
			//long start = System.currentTimeMillis();
			for(TSNodeLabel t : initWordFrags) {				
				//Fringe_old fringe = Fringe_old.computeFringeTime(t,computingFringesTime);
				Fringe_old fringe = Fringe_old.computeFringe(t);
				classifyFringeFirst(fringe, readyForScan, readyForSubDownFirst, readyForSubUpFirst);										
			}				
			//long stop = System.currentTimeMillis();
			//classifyFringeTime += (stop-start);

						
			updateStatistics(0, readyForScan, readyForSubDownFirst, readyForSubUpFirst);
			
			
			
			wordTime[0] = (int)((wordTimer.stop())/1000);
			
			if (0==lastWordIndex) {
				finished = true;
				treeTimer.stop();
				if (!readyForSubUpFirst.isEmpty())
					parsed = true;				
			}
			else
				parseIncrementallyNextWord(1, readyForScan, readyForSubDownFirst, readyForSubUpFirst);
		}
				
		private void parseIncrementallyNextWord(int i, 
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForScan,
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubDownFirst, 
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubUpFirst) {
			
			Timer wordTimer = new Timer(true);
			
			IdentityHashMap<Label, HashSet<Fringe_old>> newReadyForScan = 
					new IdentityHashMap<Label, HashSet<Fringe_old>>(); 
			// scan fringes indexed on secondTerminal
			
			IdentityHashMap<Label, HashSet<Fringe_old>> newReadyForSubDownFirst = 
					new IdentityHashMap<Label, HashSet<Fringe_old>>(); 
			// firstLexNextSub fringes indexed on secondTerminal
			
			IdentityHashMap<Label, HashSet<Fringe_old>> newReadyForSubUpFirst = 
					new IdentityHashMap<Label, HashSet<Fringe_old>>(); 
			// empty fringes indexed on root
			
			Label currentWordLabel = wordLabels.get(i);
			
			//SCAN
			scanTimer.start();
			HashSet<Fringe_old> fragmentsToScan = readyForScan.get(currentWordLabel);
			if (fragmentsToScan!=null) {
				for(Fringe_old f : fragmentsToScan) {
					//Fringe_old newFringe = f.scanTime(combiningFringesTime);
					Fringe_old newFringe = f.scan();
					classifyFringeFirst(newFringe, newReadyForScan, newReadyForSubDownFirst, newReadyForSubUpFirst);
				}
			}
			scanTimer.stop();
			
			computeNextFringesTimer.start();
			IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubDownSecond = 
					getReadyForSubDownSecond(currentWordLabel); 
			// firstLex fringes indexed on rootLabel
			
			IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubUpSecond = 
					getReadyForSubUpSecond(currentWordLabel); 
			// firstSubNextLex fringes indexed on firstTerminalLabel
			computeNextFringesTimer.stop();
			
			updateStatisticsSecond(i, readyForSubDownSecond, readyForSubUpSecond);
			
			calculateCombinatiosSubDown(i, readyForSubDownFirst, readyForSubDownSecond);
			
			//SUBDOWN
			subDownTimer.start();
			int prunedSubDown = 0;
			for(Entry<Label, HashSet<Fringe_old>> e : readyForSubDownFirst.entrySet()) {
				Label subSite_Root = e.getKey();
				HashSet<Fringe_old> matchingFragsSecond = readyForSubDownSecond.get(subSite_Root);
				if (matchingFragsSecond!=null) {
					HashSet<Fringe_old> matchingFragsFirst = e.getValue();					
					for(Fringe_old f1 : matchingFragsFirst) {
						for(Fringe_old f2 : matchingFragsSecond) {
							//assert f1.checkSubDown(f2);
							//Fringe_old newFringe = f1.subDownTime(f2,combiningFringesTime);
							if (pruneFringes && (f1.size()+f2.size()-1)>pruneFringeLength) {
								prunedSubDown++;
								continue;									
							}
							Fringe_old newFringe = f1.subDown(f2);							
							classifyFringeFirst(newFringe, newReadyForScan, newReadyForSubDownFirst, newReadyForSubUpFirst);
						}
					}					
				}
			}
			prunedFringesSubDown[i] += prunedSubDown;
			subDownTimer.stop();
			
			//SUBUP
			subUpTimer.start();
			long subUpComb = 0;
			for(Entry<Label, HashSet<Fringe_old>> e : readyForSubUpFirst.entrySet()) {
				Label root_SubSite = e.getKey();
				HashSet<Fringe_old> matchingFragsSecond = readyForSubUpSecond.get(root_SubSite);
				if (matchingFragsSecond!=null) {
					HashSet<Fringe_old> matchingFragsFirst = e.getValue();
					subUpComb += matchingFragsFirst.size()*matchingFragsSecond.size();
					for(Fringe_old f1 : matchingFragsFirst) {
						for(Fringe_old f2 : matchingFragsSecond) {
							//assert f1.checkSubUp(f2);
							//Fringe_old newFringe = f1.subUpTime(f2,combiningFringesTime);
							Fringe_old newFringe = f1.subUp(f2);
							classifyFringeFirst(newFringe, newReadyForScan, newReadyForSubDownFirst, newReadyForSubUpFirst);
						}
					}					
				}
			}
			subUpCombinations[i] = subUpComb;
			subUpTimer.stop();
			
			updateStatistics(i, newReadyForScan, newReadyForSubDownFirst, newReadyForSubUpFirst);
			
			wordTime[i] = (int)((wordTimer.stop())/1000);
			
			if (i==lastWordIndex) {				
				if (!newReadyForSubUpFirst.isEmpty())
					parsed = true;					
				treeTimer.stop();
				finished = true;
			}
			else 
				parseIncrementallyNextWord(i+1,newReadyForScan, newReadyForSubDownFirst, newReadyForSubUpFirst);
			
		}
		
		private void calculateCombinatiosSubDown(int i, 
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubDownFirst, 
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubDownSecond) {

			long subDownComb = 0;
			for(Entry<Label, HashSet<Fringe_old>> e : readyForSubDownFirst.entrySet()) {
				Label subSite_Root = e.getKey();
				HashSet<Fringe_old> matchingFragsSecond = readyForSubDownSecond.get(subSite_Root);
				if (matchingFragsSecond!=null) {
					HashSet<Fringe_old> matchingFragsFirst = e.getValue();
					subDownComb += matchingFragsFirst.size()*matchingFragsSecond.size();					
				}
			}
			subDownCombinations[i] = subDownComb;
			fringesForSubDownFirst[i] = totalSize(readyForSubDownFirst);
		}

		private ArrayList<TSNodeLabel> getLexFragments(Label wordLabel, boolean firstLex) {					
			ArrayList<TSNodeLabel> wordFrags = firstLex ?
					firstLexFragmentsTable.get(wordLabel) :
					firstSubFragmentsTable.get(wordLabel);					
			
			return wordFrags;
		}

		
		private void updateStatistics(int i, IdentityHashMap<Label, HashSet<Fringe_old>> readyForScan, 
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubDownFirst, 
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubUpFirst) {			 
			
			int a = fringesForScanTable[i] = totalSize(readyForScan);			
			int b = fringesForSubDownTable[i] = totalSize(readyForSubDownFirst);
			int c = fringesForSubUpTable[i] = totalSize(readyForSubUpFirst);
			totalFringesTable[i] = a+b+c;
			
		}
		
		private void updateStatisticsSecond(int i, IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubDownSecond, 
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubUpSecond) {
			fringesForSubDownSecond[i] = totalSize(readyForSubDownSecond);
			fringesForSubUpSecond[i] = totalSize(readyForSubUpSecond);
		}
				
		
		private int totalSize(IdentityHashMap<Label, HashSet<Fringe_old>> table) {
			int result = 0;
			for(HashSet<Fringe_old> set : table.values()) {
				result += set.size();
			}
			return result;
		}
		
		private void classifyFringeFirst(Fringe_old fringe, IdentityHashMap<Label, HashSet<Fringe_old>> readyForScan,
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubDownFirst, 
				IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubUpFirst) {			
			if (fringe.isFirstLexFringe()) {
				if (fringe.isScanFringe())
					addInHashMap(readyForScan, fringe.secondTerminalLabel(), fringe);
					// scan fringes indexed on secondTerminal
				else if (fringe.isEmpty())
					addInHashMap(readyForSubUpFirst, fringe.rootLabel, fringe);
					// empty fringes indexed on root
				else 
					addInHashMap(readyForSubDownFirst, fringe.secondTerminalLabel(), fringe);
					// firstLexNextSub fringes indexed on secondTerminal
					
			}
		}

		private void addInHashMap(IdentityHashMap<Label, HashSet<Fringe_old>> table,
				Label key, Fringe_old fringe) {
			HashSet<Fringe_old> set = table.get(key);
			if (set==null) {
				set =  new HashSet<Fringe_old>();
				table.put(key, set);
			}		
			//if (!set.contains(fringe))
			set.add(fringe);
		}

		private IdentityHashMap<Label, HashSet<Fringe_old>> getReadyForSubDownSecond(Label currentWordLabel) {
			//long start = System.currentTimeMillis();
			IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubUpSecond = 
					new IdentityHashMap<Label, HashSet<Fringe_old>>();
			// firstLex fringes indexed on rootLabel
			ArrayList<TSNodeLabel> currentWordFragsFirstLex = getLexFragments(currentWordLabel, true);
			if (currentWordFragsFirstLex!=null) {
				for(TSNodeLabel t : currentWordFragsFirstLex) {
					//Fringe_old fringe = Fringe_old.computeFringeTime(t,computingFringesTime);
					Fringe_old fringe = Fringe_old.computeFringe(t);
					addInHashMap(readyForSubUpSecond, fringe.rootLabel, fringe);
				}
			}
			//long stop = System.currentTimeMillis();
			//classifyFringeTime += (stop-start);
			return readyForSubUpSecond;

		}
		
		private IdentityHashMap<Label, HashSet<Fringe_old>> getReadyForSubUpSecond(Label currentWordLabel) {
			//long start = System.currentTimeMillis();
			IdentityHashMap<Label, HashSet<Fringe_old>> readyForSubUpSecond = 
					new IdentityHashMap<Label, HashSet<Fringe_old>>();
			// firstSubNextLex fringes indexed on firstTerminalLabel
			ArrayList<TSNodeLabel> currentWordFragsFirstSub = getLexFragments(currentWordLabel, false);
			if (currentWordFragsFirstSub!=null) {
				for(TSNodeLabel t : currentWordFragsFirstSub) {
					//Fringe_old fringe = Fringe_old.computeFringeTime(t,computingFringesTime);
					Fringe_old fringe = Fringe_old.computeFringe(t);
					addInHashMap(readyForSubUpSecond, fringe.firstTerminalLabel(), fringe);				
				}
			}
			//long stop = System.currentTimeMillis();
			//classifyFringeTime += (stop-start);
			return readyForSubUpSecond;
		}
		
		
		private void printDebug() {
			synchronized(logFile) {
				Parameters.logLine("Sentence # " + index);
				Parameters.logLine(tree.toStringQtree());
				Parameters.logLine("Sentence Length: " + length);			
				Parameters.logLine(wordLabels.toString());			
				Parameters.logLine("Total Fringes" + Utility.formatArrayNumbersReadable(totalFringesTable));
				//Parameters.logLine("Scan fringes" + Utility.formatArrayNumbersReadable(fringesForScanTable));
				Parameters.logLine("Ready for SubDown fringes" + Utility.formatArrayNumbersReadable(fringesForSubDownTable));
				//Parameters.logLine("Ready for SubUp fringes" + Utility.formatArrayNumbersReadable(fringesForSubUpTable));
				Parameters.logLine("SubDown combinations" + Utility.formatArrayNumbersReadable(subDownCombinations));								
				Parameters.logLine("SubDownFirst total size" + Utility.formatArrayNumbersReadable(fringesForSubDownFirst));
				Parameters.logLine("SubDownSecond total size" + Utility.formatArrayNumbersReadable(fringesForSubDownSecond));
				//Parameters.logLine("SubUp combinations" + Utility.formatArrayNumbersReadable(subUpCombinations));
				//Parameters.logLine("SubUpSecond total size" + Utility.formatArrayNumbersReadable(fringesForSubUpSecond));
				Parameters.logLine("SubDown Pruned Fringes" + Utility.formatArrayNumbersReadable(prunedFringesSubDown));				
				Parameters.logLine("Finished: " + finished);
				Parameters.logLine("Parsed: " + parsed);
				long runningTime = treeTimer.getTotalTime();
				long runningTimeSec = runningTime / 1000;
				long otherTime = runningTime - scanTimer.getTotalTime() - subDownTimer.getTotalTime() - 
						subUpTimer.getTotalTime() - computeNextFringesTimer.getTotalTime();
				Parameters.logLine("Running time (sec): " + runningTimeSec);
				if (runningTimeSec>10) {
					Parameters.logLine("Word running times (sec): " + Arrays.toString(wordTime));
					Parameters.logLine("\tof which scanning time: " + ((float)scanTimer.getTotalTime())/runningTime*100);
					Parameters.logLine("\tand combining subDown time: " + ((float)subDownTimer.getTotalTime())/runningTime*100);
					Parameters.logLine("\tand combining subUp time: " + ((float)subUpTimer.getTotalTime())/runningTime*100);
					Parameters.logLine("\tand computing next fringes time: " + ((float)computeNextFringesTimer.getTotalTime())/runningTime*100);					
					Parameters.logLine("\tand remaining time: " + ((float)otherTime)/runningTime*100);
				}
				Parameters.logLineFlush("");
			}
		}
		
	}
		
		
	public static void main(String[] args) throws Exception {
		
		treeLenghtLimit = 5;
		int threads = 1;		
				
		/*
		File testTB = new File("./treeLondon.mrg");
		File fragmentsFile = new File("./lexFragsSmall.mrg");
		new Incremental_TSG_Parser(fragmentsFile, testTB, threads).run();
		*/
					
		///*
		File testTB = new File(args[0]);
		File fragmentsFile = new File(args[1]);		
		threads = ArgumentReader.readIntOption(args[2]);		
		for(int j : new int[]{5}) { //new int[]{5,10,20,40,-1} 
			treeLenghtLimit = j;
			new Incremental_TSG_Parser_old_old(fragmentsFile, testTB, threads).run();
			System.out.println();
		}
		//*/		
		
	}
	
}
