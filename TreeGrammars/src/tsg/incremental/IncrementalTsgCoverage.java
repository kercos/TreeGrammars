package tsg.incremental;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.corpora.Wsj;
import tsg.incremental.old.FragmentExtractor;
import util.PrintProgress;
import util.Utility;

public class IncrementalTsgCoverage extends Thread {
	
	static boolean debug = true;
	static int treeLenghtLimit = -1;	
	static long maxMsecPerTree = 5*60*1000;	
	static long threadCheckingTime = 10000;	
	static int threads = 1;
	static int limitTestToSentenceIndex = -1;
	
	File logFile;	
	FragmentExtractorFreq FE;
	String outputPath;
	ArrayList<TSNodeLabel> testTB;		
	PrintProgress progress;	
	int connectedTrees, coveredTrees, interruptedTrees;
	Iterator<TSNodeLabel> testTBiterator;
	int treebankIteratorIndex;
	boolean smoothing;

	
	public IncrementalTsgCoverage(FragmentExtractorFreq FE, File testTBfile, 
			String outputPath) throws Exception {
		this(FE, Wsj.getTreebank(testTBfile), outputPath);
	}
	
	public IncrementalTsgCoverage (FragmentExtractorFreq FE,
			ArrayList<TSNodeLabel> testTB, String outputPath) throws Exception {
		this.FE = FE;
		this.testTB = testTB;
		this.outputPath = outputPath;
		

		String logFilename = outputPath + "log_coverage_";
		logFilename += treeLenghtLimit==-1 ? "ALL" : "upTo" + treeLenghtLimit;
		if (FragmentExtractorFreq.addMinimalFragments) {
			logFilename+= "_MinSetExt(" + FragmentExtractorFreq.minFragFreqForSmoothingFromMinSet + ")";			
		}
		if (FE.fragmentsFile!=null) {
			logFilename+= "_Frags(" + FragmentExtractorFreq.minFragFreqForSmoothingFromFrags + ")";			
		}
		
		logFilename += ".txt";
		logFile = new File(logFilename);
		Parameters.openLogFile(logFile);
		FE.extractFragments(logFile);
	}
	
	public void run() {		
			printParameters();
			filterTestTB();
			checkIncrementalCoverage();
			printFinalSummary();
		Parameters.closeLogFile();			
	}

	private void printParameters() {
		Parameters.reportLineFlush("\n\n");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("INCREMENTAL TSG COVERAGE CHECKER");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("");
		Parameters.reportLineFlush("Test Tree Length Limit: " + treeLenghtLimit);
		Parameters.reportLineFlush("Max Msec Per Tree: " + maxMsecPerTree);
		Parameters.reportLineFlush("Number of threads: " + threads);		
	}
	
	private void printFinalSummary() {
		Parameters.reportLineFlush("Number of connected trees: " + connectedTrees);
		Parameters.reportLineFlush("Number of fully covered trees: " + coveredTrees);
		Parameters.reportLineFlush("Number of interrupted trees: " + interruptedTrees);
	}


	private void filterTestTB() {
		if (treeLenghtLimit<0)
			return;
		Iterator<TSNodeLabel> treebankIterator = testTB.iterator();		
		while(treebankIterator.hasNext()) {
			TSNodeLabel t = treebankIterator.next();
			if (t.countLexicalNodes()>treeLenghtLimit)
				treebankIterator.remove();
		}
		if (limitTestToSentenceIndex!=-1) {
			TSNodeLabel t = testTB.get(limitTestToSentenceIndex);
			testTB.clear();
			testTB.add(t);
		}
		treebankIterator = testTB.iterator();
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
	}

	private void checkIncrementalCoverage() {
		
		Parameters.reportLineFlush("Checking incremental coverage...");
		
		progress = new PrintProgress("Checking coverage on tree:", 1, 0);		
		testTBiterator = testTB.iterator();
		
		Parameters.logLineFlush("\n");
		
		FragmentsExtractorThread[] threadsArray = new FragmentsExtractorThread[threads];
		for(int i=0; i<threads; i++) {
			FragmentsExtractorThread t = new FragmentsExtractorThread();
			threadsArray[i] = t;
			t.start();
		}
		
		if (threads==1)
			threadCheckingTime = 1000;
		
		try {
			Thread.sleep(2000);				
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
		boolean someRunning = false;		
		do {						
			someRunning = false;
			for(int i=0; i<threads; i++) {
				FragmentsExtractorThread t = threadsArray[i];
				if (t.isAlive()) {
					someRunning = true;				
					long startTime = t.startTime;
					if (startTime==0) continue;
					long runningTime = System.currentTimeMillis() - startTime;
					if (runningTime > maxMsecPerTree) {
						t.interrupt();
					}									
				}	
				/*
				if (t.isAlive()) {
					someRunning = true;					
					long runningTime = System.currentTimeMillis() - t.startTime;
					//System.out.println("Thread " + i + ": "  + runningTime/1000);					 
					if (runningTime > maxMsecPerTree) {						
						t.stop();
						interruptedTrees++;
						t.stopTime = System.currentTimeMillis();
						if (debug)
							t.printDebug();
						synchronized(progress) {
							progress.next();
						}
						t = new FragmentsExtractorThread();
						threadsArray[i] = t;
						t.start();
					}
										
				}			
				*/	
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
		if (!testTBiterator.hasNext()) 
			return null;			
		index[0] = treebankIteratorIndex++;
		return testTBiterator.next();
	}
	
	static 	Comparator<TSNodeLabel> fragComparator = new Comparator<TSNodeLabel>() {
	
		//@Override
		public int compare(TSNodeLabel t1, TSNodeLabel t2) {
			Integer size1 = t1.countAllNodes();
			Integer size2 = t2.countAllNodes();
			return size1.compareTo(size2);
		}
		
	};

	protected class FragmentsExtractorThread extends Thread {
		
		long numberOfDerivations;
		int[] bestDerivationIndexes;
		int index;
		
		long startTime, stopTime; 		
		TSNodeLabel tree;
		TSNodeLabelIndex treeIndex;
		ArrayList<TSNodeLabel> wordNodes;
		int length, lastWordIndex, size;
		int totalCoverCardinality;
		Vector<ArrayList<TSNodeLabel>> wordNodesFrags;
		Vector<Vector<BitSet>> wordFragmentCoverSets;
		int[] wordCoverSize;
		boolean connected, allCovered;
		boolean finished = false;
		
		public void run() {
			TSNodeLabel t = null;
			int[] i = {0};
			while ( (t = getNextTree(i)) != null) {
				index = i[0];
				initTree(t);
				checkIncrementality();
				synchronized(progress) {
					if (connected) connectedTrees++;
					if (allCovered) coveredTrees++;
					progress.next();
				}
			}			
		}
		
		private void initTree(TSNodeLabel t) {
			startTime = System.currentTimeMillis();
			this.tree = t;
			this.treeIndex = new TSNodeLabelIndex(tree);
			this.wordNodes = treeIndex.collectLexicalItems();
			this.length = wordNodes.size();
			this.lastWordIndex = length-1;
			this.size = treeIndex.countAllNodes();			
			this.wordNodesFrags = new Vector<ArrayList<TSNodeLabel>>(length);
			this.wordFragmentCoverSets = new Vector<Vector<BitSet>>(length);
			this.wordCoverSize = new int[length];
			this.connected = false;
			this.allCovered = false;
			this.finished = false;			
		}
		
		private void checkIncrementality() {									
			int i=0;
			for(TSNodeLabel w : wordNodes) {
				ArrayList<TSNodeLabel> wordFragsList = getWordFrags(w);								
				wordNodesFrags.add(wordFragsList);		
				int size = wordFragsList.size();
				wordCoverSize[i++] = size;
				Vector<BitSet> wordFragCoverSets = new Vector<BitSet>(size);				
				wordFragmentCoverSets.add(wordFragCoverSets);
				for(TSNodeLabel wf : wordFragsList) {
					BitSet coveredNodes = setCoveredNodes((TSNodeLabelIndex)w, wf);
					wordFragCoverSets.add(coveredNodes);
				}
			}
			
			bestDerivationIndexes = new int[wordFragmentCoverSets.size()];
			Arrays.fill(bestDerivationIndexes, -1);			
			
			TSNodeLabelIndex[] treeNodeIndexes = treeIndex.collectAllNodesInArray();
			connected = getBestSubDerFirstWord(treeNodeIndexes);
			//connected = getBestSubDer(treeNodeIndexes, 0, new BitSet(size));
			
			stopTime = System.currentTimeMillis();
			if(Thread.interrupted()) {
				interruptedTrees++;
			}
			else {
				finished = true;
				totalCoverCardinality = getTotalCoverCardinality(wordFragmentCoverSets,bestDerivationIndexes,size);
				allCovered = totalCoverCardinality==size;
			}			
			
			if (debug) {
				printDebug();
			}
			
		}
		
		private BitSet[][] getLexNodesIndexes(TSNodeLabelIndex[] treeNodeIndexes) {
			BitSet[][] result = new BitSet[length][];
			for(int wordIndex=0; wordIndex<length; wordIndex++) {
				Vector<BitSet> currentWordCoverSet =  wordFragmentCoverSets.get(wordIndex);
				BitSet[] wordResult = new BitSet[currentWordCoverSet.size()];
				result[wordIndex] = wordResult;
				int i=0;
				for(BitSet wordCover : currentWordCoverSet) {					
					int rootFragIndex = wordCover.nextSetBit(0);
					TSNodeLabelIndex rootFragNode =  treeNodeIndexes[rootFragIndex];
					BitSet lexIndexes = new BitSet(size);
					rootFragNode.getLexNodeIndexes(lexIndexes,wordCover);
					wordResult[i++] = lexIndexes;
				}				
			}
			return result;
		}
		
		private int[][] getRootIndexFragments(TSNodeLabelIndex[] treeNodeIndexes) {
			int[][] result = new int[length][];
			for(int wordIndex=0; wordIndex<length; wordIndex++) {
				Vector<BitSet> currentWordCoverSet =  wordFragmentCoverSets.get(wordIndex);
				int[] wordFragsIndexes = new int[currentWordCoverSet.size()];
				result[wordIndex] = wordFragsIndexes;
				int i=0;
				for(BitSet wordCover : currentWordCoverSet) {					
					int rootFragIndex = wordCover.nextSetBit(0);
					wordFragsIndexes[i++] = rootFragIndex;
				}
			}
			return result;
		}
		
		private BitSet[][] getSubSitesIndexesFragments(TSNodeLabelIndex[] treeNodeIndexes) {
			BitSet[][] result = new BitSet[length][];
			for(int wordIndex=0; wordIndex<length; wordIndex++) {
				Vector<BitSet> currentWordCoverSet =  wordFragmentCoverSets.get(wordIndex);
				BitSet[] subSiteIndexes = new BitSet[currentWordCoverSet.size()];
				result[wordIndex] = subSiteIndexes;
				int i=0;
				for(BitSet wordCover : currentWordCoverSet) {					
					int rootFragIndex = wordCover.nextSetBit(0);
					TSNodeLabelIndex rootFragNode =  treeNodeIndexes[rootFragIndex];
					BitSet subSites = new BitSet(size);
					rootFragNode.getSubSiteIndexes(wordCover, subSites);
					subSiteIndexes[i++] = subSites;
				}
			}
			return result;
		}

		/*
		private int[][][] getRootFirstSubSiteSecondSubSiteIndexesFragments(TSNodeLabelIndex[] treeNodeIndexes) {
			int[][][] result = new int[length][][];
			for(int wordIndex=0; wordIndex<length; wordIndex++) {
				Vector<BitSet> currentWordCoverSet =  wordFragmentCoverSets.get(wordIndex);
				int[][] wordFragsIndexes = new int[currentWordCoverSet.size()][3];
				result[wordIndex] = wordFragsIndexes;
				int i=0;
				for(BitSet wordCover : currentWordCoverSet) {					
					int rootFragIndex = wordCover.nextSetBit(0);
					TSNodeLabelIndex rootFragNode =  treeNodeIndexes[rootFragIndex];
					int[] leftMost2SubSites = new int[]{-1,-1};
					rootFragNode.get2LeftmostSubSitesIndex(wordCover, leftMost2SubSites, new int[]{0});
					//int subSiteIndexFrag = rootFragNode.getLeftmostSubSiteIndex(wordCover);
					//int lexIndex = rootFragNode.getLeftmostLexNodeIndex(wordCover);
					//wordFragsIndexes[i++] = new int[]{rootFragIndex, subSiteIndexFrag, lexIndex};
					wordFragsIndexes[i++] = new int[]{rootFragIndex, leftMost2SubSites[0], leftMost2SubSites[1]};
				}
			}
			return result;			
		}
		*/
		
		private void printDebug() {
			synchronized(logFile) {
				ArrayList<String> words = tree.collectLexicalWords(); 
				numberOfDerivations = Utility.productLong(wordCoverSize);
				
				Parameters.logLine("Sentence # " + index);
				Parameters.logLine(tree.toString());
				Parameters.logLine(tree.toStringQtree());
				Parameters.logLine("Sentence Length: " + length);			
				Parameters.logLine(words.toString());				
				Parameters.logLine("Finished: " + finished);
				Parameters.logLine("Running time (sec): " + (stopTime - startTime) / 1000);
				Parameters.logLine("Word fragmets sizes: " + Arrays.toString(wordCoverSize));
				Parameters.logLine("Number of derivations: " + numberOfDerivations);								
				Parameters.logLine("Sentence size: " + size);
				Parameters.logLine("Connected: " + connected);
				Parameters.logLine("All covered: " + allCovered);
				if (!allCovered) {
					Parameters.logLine("Total Cover Cardinality: " + totalCoverCardinality);
				}
				if (connected) {
					Parameters.logLine("Best Derivation Indexes: " + Arrays.toString(bestDerivationIndexes));					
					for(int j=0; j<bestDerivationIndexes.length; j++) {
						int index = bestDerivationIndexes[j];
						if (index==-5)
							Parameters.logLine("\t" + j + ": \"" + words.get(j) + "\" WAS PREDICTED!");
						else if (index<0) {
							Parameters.logLine("\t" + j + ": SOMETHING WRONG HERE! Index=" + index);
						}
						else
							Parameters.logLine("\t" + j + ": " + wordNodesFrags.get(j).get(index).toStringQtree());
					}
				}
				if (!connected || limitTestToSentenceIndex!=-1) {
					int j=0;
					for(String w : words) {
						ArrayList<TSNodeLabel> wordFrags = wordNodesFrags.get(j);
						Parameters.logLine("\t" + j++ + ": " + w);
						for(TSNodeLabel f : wordFrags) {
							Parameters.logLine("\t\t" + f.toStringQtree());
						}
					}
				}
				Parameters.logLineFlush("");
			}
		}
		
		private int getTotalCoverCardinality(
				Vector<Vector<BitSet>> wordFragmentCoverSets,
				int[] bestDerivationIndexes, int size) {
			
			BitSet coverage = new BitSet(size);
			for(int i=0; i<bestDerivationIndexes.length; i++) {
				int index = bestDerivationIndexes[i];
				if (index<0) continue;
				BitSet cover = wordFragmentCoverSets.get(i).get(index);
				coverage.or(cover);
			}
			return coverage.cardinality();
		}
		
		


		/**
		 * FILTER OUT FRAGMENTS NOT PRESENT IN THE TREE
		 * @param treeWordNode
		 * @param wordFrags
		 * @return
		 */
		private ArrayList<TSNodeLabel> getWordFrags(TSNodeLabel treeWordNode) {
			
			HashSet<TSNodeLabel> fragSet = FE.getFreshWordFragsSetWithSmoothing(treeWordNode.label);			
			HashSet<TSNodeLabel> filteredWordFrags = new HashSet<TSNodeLabel>();
			outerloop:
			for(TSNodeLabel wf : fragSet) {
				TSNodeLabel lexNode = wf.getLeftmostLexicalNode();
				int wF_height = lexNode.height();
				TSNodeLabel p = treeWordNode;
				for(int i=0; i<wF_height; i++) {
					if (p.parent==null) {
						continue outerloop;
					}
					p = p.parent;
				}
				if (p.containsNonRecursiveFragment(wf)) {
					ArrayList<TSNodeLabel> terms = wf.collectTerminalItems();
					int numSubSitesBeforeLex = 0;
					for(TSNodeLabel t : terms) {
						if (t==lexNode) break;
						if (!t.isLexical && ++numSubSitesBeforeLex==2) {
							continue outerloop;						
						}
					}
					filteredWordFrags.add(wf);
				}
			}
			if (filteredWordFrags.isEmpty())
				filteredWordFrags.add(treeWordNode.parent.clone());
			
			ArrayList<TSNodeLabel> fragList = new ArrayList<TSNodeLabel>(filteredWordFrags);
			Collections.sort(fragList, fragComparator);				
			Collections.reverse(fragList);
			return fragList;
		}
		
		private boolean getBestSubDerFirstWord(TSNodeLabelIndex[] treeNodeIndexes) {
			
			if (lastWordIndex==0) {
				bestDerivationIndexes[0] = 0;
				return true;
			}
			
			int[][] rootIndexFragments = getRootIndexFragments(treeNodeIndexes);
			BitSet[][] subSitesIndexesFragments = getSubSitesIndexesFragments(treeNodeIndexes);
			BitSet[][] lexNodesIndexesFragments = getLexNodesIndexes(treeNodeIndexes);

			BitSet[] subSitesIndexes = subSitesIndexesFragments[0];
			BitSet[] lexIndexes =  lexNodesIndexesFragments[0];			
			int[] rootIndexes = rootIndexFragments[0];
			int fragsNumber = rootIndexes.length;
			
			int[] wordIndexes = new int[length]; 
			treeNodeIndexes[0].fillWordIndexes(wordIndexes, new int[]{0});			
			int nextWordPos = 1;
			
			for(int i=0; i<fragsNumber; i++) {								
				int rootFragIndex = rootIndexes[i];							
				BitSet subSitesFragIndexes = subSitesIndexes[i];
				int firstSubSiteIndexFrag = subSitesFragIndexes.nextSetBit(0);				
				BitSet fragLexIndexes = lexIndexes[i];				
				if ( getBestSubDerNextWord(rootIndexFragments, treeNodeIndexes, 
						wordIndexes, nextWordPos, rootFragIndex, firstSubSiteIndexFrag, 
						fragLexIndexes, subSitesFragIndexes, lexNodesIndexesFragments, subSitesIndexesFragments) ) {
					bestDerivationIndexes[0] = i;
					return true;
				}
				if (isInterrupted()) {
					return false;
				}
				continue;
			}
			return false;
			
		}

		private boolean getBestSubDerNextWord(int[][] rootIndexFragments, TSNodeLabelIndex[] treeNodeIndexes, 
				int[] wordIndexes, int wordPosition, int rootIndexGlobal, int firstSubSiteIndexGlobal,
				BitSet globalLexIndexes, BitSet gloablSubSiteIndexes, BitSet[][] lexNodesIndexesFragments,
				BitSet[][] subSitesIndexesFragments) {
			
			bestDerivationIndexes[wordPosition] = -1;				
			int wordIndex = wordIndexes[wordPosition];
						
			boolean lexAlreadyThere = globalLexIndexes.get(wordIndex);				
			if (lexAlreadyThere) {					
				boolean success = false;
				if (wordPosition==lastWordIndex)
					success = true;
				else {
					success = getBestSubDerNextWord(rootIndexFragments, treeNodeIndexes, 
							wordIndexes, wordPosition+1, rootIndexGlobal, firstSubSiteIndexGlobal, 
							globalLexIndexes, gloablSubSiteIndexes, lexNodesIndexesFragments, subSitesIndexesFragments);
				}
				if (success) {
					bestDerivationIndexes[wordPosition] = -5;
					return true;
				}
			}
			
			BitSet[] lexIndexes =  lexNodesIndexesFragments[wordPosition];
			BitSet[] subSiteIndexes = subSitesIndexesFragments[wordPosition];
			int[] rootIndexes = rootIndexFragments[wordPosition];
			int fragsNumber = rootIndexes.length;
			
			for(int i=0; i<fragsNumber; i++) {								
				int rootIndexFrag = rootIndexes[i];								
				BitSet subSiteIndexesFrag = subSiteIndexes[i];
				int firstSubSiteFrag = subSiteIndexesFrag.nextSetBit(0);
				
				boolean subDown = firstSubSiteIndexGlobal!=-1 && firstSubSiteIndexGlobal==rootIndexFrag;
				boolean subUp = firstSubSiteFrag!=-1 && firstSubSiteFrag==rootIndexGlobal;
				
				if (subDown || subUp) {
					if (wordPosition==lastWordIndex) {
						bestDerivationIndexes[wordPosition] = i;
						return true;
					}
					BitSet globalLexIndexesCopy = (BitSet)globalLexIndexes.clone();
					globalLexIndexesCopy.or(lexIndexes[i]);					
					
					BitSet gloablSubSiteIndexesCopy = (BitSet) gloablSubSiteIndexes.clone();
					gloablSubSiteIndexesCopy.or(subSiteIndexesFrag);
					
					int newRoot = -1;
					int newSubSite = -1;
					
					if (subDown) {
						newRoot = rootIndexGlobal;
						newSubSite = firstSubSiteFrag!=-1 ? 
								firstSubSiteFrag : gloablSubSiteIndexes.nextSetBit(wordIndex);						
					}
					else {
						newRoot = rootIndexFrag;
						newSubSite = subSiteIndexesFrag.nextSetBit(firstSubSiteFrag+1);;
					}
					
					if ( getBestSubDerNextWord(rootIndexFragments, treeNodeIndexes, 
							wordIndexes, wordPosition+1, newRoot, newSubSite, 
						globalLexIndexesCopy, gloablSubSiteIndexesCopy, lexNodesIndexesFragments, subSitesIndexesFragments)
					) {
						bestDerivationIndexes[wordPosition] = i;
						return true;
					}
					
					if (isInterrupted()) {
						return false;
					}
				}
			}				
			return false;
		}
		


		
		private BitSet setCoveredNodes(TSNodeLabelIndex treeWordNode, TSNodeLabel wordFrag) {
			BitSet result = new BitSet(size);
			TSNodeLabel lexNode = wordFrag.getLeftmostLexicalNode();
			int wF_height = lexNode.height();
			TSNodeLabelIndex treeNode = treeWordNode; 
			for(int i=0; i<wF_height; i++) {
				treeNode = (TSNodeLabelIndex)treeNode.parent;
			}
			setCoveredNodesRecursive(treeNode, wordFrag, result);
			return result;
		}
		
		private void setCoveredNodesRecursive(TSNodeLabelIndex treeNode, TSNodeLabel wordFrag, BitSet result) {
			result.set(treeNode.index);
			TSNodeLabel[] treeNodeDaughters = treeNode.daughters;
			TSNodeLabel[] wordFragDaughters = wordFrag.daughters;
			int prole = treeNodeDaughters.length;
			for(int i=0; i<prole; i++) {
				TSNodeLabelIndex dIndex = (TSNodeLabelIndex)treeNodeDaughters[i];
				result.set(dIndex.index);
				TSNodeLabel wfd = wordFragDaughters[i];
				if (!wfd.isTerminal()) {
					setCoveredNodesRecursive(dIndex, wfd, result);
				}
			}
		}
	}
	
	public static void mainToy(String[] args) throws Exception {
		
		debug = true;
		treeLenghtLimit = 20;	
		maxMsecPerTree = 1*60*1000;		
		threadCheckingTime = 1000;	
		threads = 1;
		
		String homePath = System.getProperty("user.home");
		String workingPath = homePath + "/PLTSG/ToyCorpus/";	
		File trainTB = new File(workingPath + "trainTB.mrg");
		File testTB = new File(workingPath + "testTB.mrg");
		File fragFile = new File(workingPath + "fragments.mrg");
		
		FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(
				workingPath, "-1", "-1", trainTB, testTB, fragFile);
		
		FragmentExtractorFreq.printAllLexFragmentsToFile = true;
		
		new IncrementalTsgCoverage(FE, testTB, workingPath).run();
	}
	
	public static void main(String[] args) throws Exception {
		
		debug = true;
		treeLenghtLimit = 20;	
		maxMsecPerTree = 1*60*1000;		
		threadCheckingTime = 1000;	
		threads = 5;		
		limitTestToSentenceIndex = -1;	
		
		//String homePath = System.getProperty("user.home");
		//String workingPath = basePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";
		String basePath = Parameters.scratchPath;
		//String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H1_V1_UkM4_UkT4/";
		String workingPath = basePath + "/PLTSG/MB_Right_H1_V1_UkM4_UkT4/";
		File trainTB = new File(workingPath + "wsj-02-21.mrg");
		File testTB = new File(workingPath + "wsj-22.mrg");
		File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		String fragExtrWorkingPath  = workingPath + "FragmentExtractor/";
		String outputPathCoverage = workingPath + "Coverage/";				
		

		String[][] settings = new String[][]{
						//	minSet			frags	
			//only minset			
			//new String[]{	"1",			"No"},
			//new String[]{	"5",			"No"},	
			//new String[]{	"10",			"No"},
			//new String[]{	"100",			"No"},
			//new String[]{	"1000",			"No"},
			new String[]{	"NoSmoothing",	"No"},
			
			//only frags
			//new String[]{	"No",			"100"},
			//new String[]{	"No",			"1000"},
			new String[]{	"No",			"NoSmoothing"},
			
			//combination
			//new String[]{	"1",			"1000"},
			//new String[]{	"100",			"100"},
			//new String[]{	"1000",			"1000"},
			//new String[]{	"1",	"NoSmoothing"},
			//new String[]{	"1000",			"NoSmoothing"},
			new String[]{	"NoSmoothing",	"NoSmoothing"},
			//new String[]{	"10",			"NoSmoothing"},
			//new String[]{	"100",			"NoSmoothing"},
			
		};
		
		for(String[] set : settings) {
			FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(
					fragExtrWorkingPath, set[0], set[1], trainTB, testTB, fragFile);
			new IncrementalTsgCoverage(FE, testTB, outputPathCoverage).run();
		}		
		
	}
	
}
