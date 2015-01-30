package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.corpora.Wsj;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class IncrementalTsgAdvancedTrainer extends Thread {

	static boolean addSTOP = false;
	
	static boolean fastTrain = false;
	static boolean shortTrain = false;
	static boolean addBadicEvents = true;

	static int threads = 7;
	static long threadCheckingTime = 4000;

	static boolean initFragSmoothing = false;
	static double minFreqFirstWordFragSmoothing = 1E-1;
	// freq given to firstWord frags seen only as lexFirst frags or the way
	// around

	File trainingTBfile, logFile;
	FragmentExtractorFreq FE;
	String outputPath;

	ArrayList<TSNodeLabel> treebank;
	Iterator<TSNodeLabel> trainTBiterator;
	PrintProgress progress;
	
	int treebankIteratorIndex;

	SimpleTimer globalTimer;

	HashMap<Label, HashMap<Label, HashSet<TSNodeLabel>>> 
		initFragments, // indexed on lex and root
		firstLexFragments, // indexed on lex and root
		firstSubFragments; // indexed on lex and firstTerm
	
	String condProbModelId;
	CondProbModel globalCondProbModel;	
	

	public IncrementalTsgAdvancedTrainer(FragmentExtractorFreq FE, 
			File trainingTBfile, String outputPath,
			String condProbModelId) throws Exception {
		
		this.FE = FE;
		this.trainingTBfile = trainingTBfile;
		this.treebank = Wsj.getTreebank(trainingTBfile);
		this.outputPath = outputPath;		
		this.condProbModelId = condProbModelId;
	}

	public void run() {
		
		initVariables();
		this.globalCondProbModel = CondProbModelFactory.getCondProbModel(condProbModelId);
		Parameters.openLogFile(logFile);
		try {
			FE.extractFragments(logFile);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		printParameters();
		readFragments();
		if (fastTrain)
			runFastTrainer();
		else
			runChartEstimator();
		
		if (addBadicEvents && !fastTrain)
			addBasicEvents();
		
		Parameters.reportLine("Estimating log probabilities...");
		globalCondProbModel.estimateLogProbabilities();
		
		printFinalSummary();
		Parameters.closeLogFile();		
	}


	private void initVariables() {
		globalTimer = new SimpleTimer(true);
		logFile = new File(outputPath + "Train_" + condProbModelId + 
				"_" + FileUtil.dateTimeString() + "_LOG.log");
	}

	private void printParameters() {
		Parameters.reportLine("\n\n");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("INCREMENTAL TSG TRAINER ");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("");
		Parameters.reportLine("Log file: " + logFile);
		Parameters.reportLine("Number of threads: " + threads);
		Parameters.reportLine("Treebank file: " + trainingTBfile);
		Parameters.reportLine("Number of trees: " + treebank.size());		
		Parameters.reportLine("Cond prob model ID: " + globalCondProbModel.idString());		
	}

	private void printFinalSummary() {
		Parameters.reportLine("Took in Total (sec): " + globalTimer.checkEllapsedTimeSec());
		Parameters.reportLineFlush("Log file: " + logFile);

	}

	private void readFragments() {

		Parameters.reportLineFlush("Reading lex fragments...");

		initFragments = new HashMap<Label, HashMap<Label, HashSet<TSNodeLabel>>>();
		firstLexFragments = new HashMap<Label, HashMap<Label, HashSet<TSNodeLabel>>>();
		firstSubFragments = new HashMap<Label, HashMap<Label, HashSet<TSNodeLabel>>>();

		int countFirstLexFrags = 0;
		int countFirstSubFrags = 0;

		// FIRST LEX FRINGES
		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstLex.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			Label firstLexLabel = e.getKey();
			for (Entry<TSNodeLabel, int[]> f : fragSet.entrySet()) {
				countFirstLexFrags++;
				TSNodeLabel firstLexFrag = f.getKey();
				int[] freqArray = f.getValue();

				int freq = freqArray[0] - freqArray[1]; // remove initial frag
														// counts
				double d = freq == 0 && initFragSmoothing ? minFreqFirstWordFragSmoothing : freq;
				if (d > 0) {
					Utility.putInHashMapHashSet(firstLexFragments, firstLexLabel, firstLexFrag.label, firstLexFrag);
				}

				// init fragments
				freq = f.getValue()[1];
				d = freq == 0 && initFragSmoothing ? minFreqFirstWordFragSmoothing : freq;
				if (d > 0) {
					Utility.putInHashMapHashSet(initFragments, firstLexLabel, firstLexFrag.label, firstLexFrag);
				}
			}
		}

		// FIRST SUB FRINGES
		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstSub.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			Label firstLexLabel = e.getKey();
			for (Entry<TSNodeLabel, int[]> f : fragSet.entrySet()) {
				countFirstSubFrags++;
				TSNodeLabel firstSubFrag = f.getKey();
				Label firstSubLabel = firstSubFrag.getLeftmostTerminalNode().label;
				//int freq = normalizedCounts ? 1 : f.getValue()[0];
				//assert freq > 0;
				Utility.putInHashMapHashSet(firstSubFragments, firstLexLabel, firstSubLabel, firstSubFrag);
			}
		}

		if (addSTOP) {
			// STOP fringes as first-sub fringes STOP > Y STOP
			for (Entry<Label, int[]> e : FE.topFreq.entrySet()) {
				countFirstSubFrags++;
				Label firstSub = e.getKey();
				//int freq = normalizedCounts ? 1 : e.getValue()[0];
				TSNodeLabel stopFrag = FragFringeUnambigous.getStopFragment(firstSub);
				Label firstSubLabel = stopFrag.getLeftmostTerminalNode().label;
				Utility.putInHashMapHashSet(firstSubFragments, FragFringeUnambigous.STOPlabel, firstSubLabel, stopFrag);
			}
		}

		int totalLexFrags = countFirstLexFrags + countFirstSubFrags;
		Parameters.reportLineFlush("Total number of lex fragments: " + totalLexFrags);
		Parameters.reportLineFlush("\tLex first (including init): " + countFirstLexFrags);
		Parameters.reportLineFlush("\tSub first (including stop): " + countFirstSubFrags);
	}

	private void runFastTrainer() {
		Parameters.reportLineFlush("Running fast trainer...");
		
		// FIRST LEX FRINGES
		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstLex.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			for (Entry<TSNodeLabel, int[]> f : fragSet.entrySet()) {
				TSNodeLabel firstLexFrag = f.getKey();
				int[] freqArray = f.getValue();
				int freq = freqArray[0] - freqArray[1]; // remove initial frag														// counts
				FragFringeUnambigous ff = new FragFringeUnambigous(firstLexFrag);
				double d = freq == 0 && initFragSmoothing ? minFreqFirstWordFragSmoothing : freq;
				if (d > 0)
					globalCondProbModel.addTrainEventSubBackward(null, ff,d);
				// init fragments
				freq = f.getValue()[1];
				d = freq == 0 && initFragSmoothing ? minFreqFirstWordFragSmoothing : freq;
				if (d > 0)					
					globalCondProbModel.addTrainEventInit(ff,d);
			}
		}

		// FIRST SUB FRINGES
		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstSub.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			for (Entry<TSNodeLabel, int[]> f : fragSet.entrySet()) {
				TSNodeLabel firstSubFrag = f.getKey();
				FragFringeUnambigous ff = new FragFringeUnambigous(firstSubFrag);
				globalCondProbModel.addTrainEventSubForward(null, ff, f.getValue()[0]);
			}
		}
		
	}

	private void addBasicEvents() {
		Parameters.reportLineFlush("Inserting basic events...");
		
		// FIRST LEX FRINGES
		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstLex.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			for (Entry<TSNodeLabel, int[]> f : fragSet.entrySet()) {
				TSNodeLabel firstLexFrag = f.getKey();
				int[] freqArray = f.getValue();
				int freq = freqArray[0] - freqArray[1]; // remove initial frag														// counts
				FragFringeUnambigous ff = new FragFringeUnambigous(firstLexFrag);
				double d = freq == 0 && initFragSmoothing ? minFreqFirstWordFragSmoothing : freq;
				if (d > 0)
					globalCondProbModel.addTrainEventBasicSubBackward(ff);
				// init fragments
				freq = f.getValue()[1];
				d = freq == 0 && initFragSmoothing ? minFreqFirstWordFragSmoothing : freq;
				if (d > 0)					
					globalCondProbModel.addTrainEventBasicInit(ff);
			}
		}

		// FIRST SUB FRINGES
		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstSub.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			for (Entry<TSNodeLabel, int[]> f : fragSet.entrySet()) {
				TSNodeLabel firstSubFrag = f.getKey();
				FragFringeUnambigous ff = new FragFringeUnambigous(firstSubFrag);
				globalCondProbModel.addTrainEventBasicSubForward(ff);
			}
		}
		
	}

	private void runChartEstimator() {

		Parameters.reportLineFlush("Estimating fragment frequencies...");
		Parameters.reportLineFlush("Number of trees: " + treebank.size());

		//convertFreqToLogRelativeFreq();
		extractTrainingCounts();

		Parameters.logLineFlush("\n");

	}

	public static int printToFile(HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>> frags, File outputFile) {

		int totalCount = 0;
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for (HashMap<Label, HashMap<TSNodeLabel, double[]>> subTable : frags.values()) {
			for (HashMap<TSNodeLabel, double[]> subsubTable : subTable.values()) {
				for (Entry<TSNodeLabel, double[]> fragProb : subsubTable.entrySet()) {
					String fragString = fragProb.getKey().toString(false, true);
					double prob = fragProb.getValue()[0];
					if (prob > 0) {
						totalCount++;
						pw.println(fragString + "\t" + fragProb.getValue()[0]);
					}
				}
			}
		}
		pw.close();
		return totalCount;
	}

	private void extractTrainingCounts() {
		progress = new PrintProgress("Extracting lex counts from tree:", 1, 0);

		if (shortTrain)
			treebank = new ArrayList<TSNodeLabel>(treebank.subList(0, 2000));
		
		trainTBiterator = treebank.iterator();

		ChartParserThread[] threadsArray = new ChartParserThread[threads];
		for (int i = 0; i < threads; i++) {
			ChartParserThread t = new ChartParserThread(
					CondProbModelFactory.getCondProbModel(condProbModelId));
			threadsArray[i] = t;
			t.start();
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		boolean someRunning = false;
		do {
			someRunning = false;
			for (int i = 0; i < threads; i++) {
				ChartParserThread t = threadsArray[i];
				if (t.isAlive()) {
					someRunning = true;
				}
			}

			if (!someRunning)
				break;

			try {
				Thread.sleep(threadCheckingTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} while (true);

		progress.end();

	}

	private synchronized TSNodeLabel getNextTree(int[] index) {
		if (!trainTBiterator.hasNext())
			return null;
		index[0] = treebankIteratorIndex++;
		progress.next();
		return trainTBiterator.next();
	}

	public static TSNodeLabel combineDerivation(ArrayList<TSNodeLabel> derivation) {

		Iterator<TSNodeLabel> derivationIter = derivation.iterator();
		TSNodeLabel result = derivationIter.next().clone();
		LinkedList<TSNodeLabel> subSitesList = new LinkedList<TSNodeLabel>();
		result.addSubSites(subSitesList, true);
		while (derivationIter.hasNext()) {
			TSNodeLabel nextFrag = derivationIter.next();
			if (FragFringeUnambigous.isStopFrag(nextFrag))
				continue;
			TSNodeLabel nextFragClone = nextFrag.clone();
			if (subSitesList.isEmpty()) {
				// SUB-FORWARD
				nextFragClone.addSubSites(subSitesList, false);
				TSNodeLabel subSite = subSitesList.removeFirst();
				assert subSite.label == result.label;
				subSite.assignDaughters(result.daughters);
				result = nextFragClone;
			} else {
				// SUB-BACKWARD
				TSNodeLabel subSite = subSitesList.removeFirst();
				assert subSite.label == nextFragClone.label;
				subSite.assignDaughters(nextFragClone.daughters);
				nextFragClone.addSubSites(subSitesList, true);
			}
		}
		return result;
	}

	public static boolean fragInTreeFromTerm(TSNodeLabel f, TSNodeLabel t) {
		assert t.sameLabel(f);
		TSNodeLabel tp = t.parent, fp = f.parent;
		do {
			if (tp == null || !tp.sameLabel(fp))
				return false;
			f = fp;
			t = tp;
			tp = t.parent;
			fp = f.parent;
		} while (fp != null);
		return t.containsNonRecursiveFragment(f);
	}

	protected class ChartParserThread extends Thread {

		int sentenceIndex;
		TSNodeLabel currentTree;

		ArrayList<TSNodeLabel> wordNodes;
		int sentenceLength;

		Vector<ChartColumn> chart;
		CondProbModel localCondProbModel;
		
		public ChartParserThread(CondProbModel cpm) {
			this.localCondProbModel = cpm;
		}

		public void run() {
			int[] i = { 0 };
			while ((currentTree = getNextTree(i)) != null) {
				sentenceIndex = i[0];
				initVariables();				
				parse();				
			}
			updateMainResults();
		}

		private void initVariables() {
			this.wordNodes = currentTree.collectLexicalItems();
			this.sentenceLength = wordNodes.size();			
			initChart();
		}

		private void initChart() {
			chart = new Vector<ChartColumn>(sentenceLength + 1); // termstate
			int i = 0;
			for (TSNodeLabel wn : wordNodes) {
				TermLabel wordTermLabel = TermLabel.getTermLabel(wn.label, true);
				chart.add(new ChartColumn(wordTermLabel, wn, i));
				i++;
			}
			chart.add(new ChartColumn(FragFringeUnambigous.STOPnodeLex, null, i));
		}

		private void parse() {			

			ChartColumn startChartColumn = chart.get(0);
			startChartColumn.performStart();

			for (int i = 0; i <= sentenceLength; i++) {
				ChartColumn c = chart.get(i);
				if (i == 0)
					c.performScan();
				else if (i < sentenceLength) {
					c.performCompleteSlow();
					c.performSubBackward();
					c.performSubForward();
					c.performScan();
				} else { // last column
					c.performCompleteSlow();
					if (addSTOP)
						c.performSubForward();
				}
			}
		}

		private void updateMainResults() {
			synchronized (progress) {
				globalCondProbModel.addAllTrainingEvents(localCondProbModel);				
			}
			localCondProbModel = null;
		}

		public HashMap<TermLabel, HashSet<SymbolicChartState>> getEndStatesTable() {
			ChartColumn lastColumn = chart.get(sentenceLength);
			return addSTOP ? lastColumn.scanStatesSet : lastColumn.subForwardFirstStatesSet;
		}

		class ChartColumn {

			TermLabel nextWordTermLabel;
			TSNodeLabel nextWordNode;
			Label nextWordLabel;
			int nextWordIndex;

			HashMap<TermLabel, HashSet<SymbolicChartState>> 
				scanStatesSet, // indexed on root
				subBackwardFirstStatesSet, // indexed on non-term after dot
				subForwardFirstStatesSet, // indexed on root
				completeStatesSet; // indexed on root

			HashMap<TermLabel, HashSet<FragFringeUnambigous>> 
				firstLexFragFringeSet, // indexed on root - used in start and sub-backward second
				firstSubFragFringeSet; // indexed on first term (sub-site) - used in sub-forward second

			HashMap<FragFringeUnambigous, TSNodeLabel> firstLexFringeFragMapping, firstSubFringeFragMapping;

			public ChartColumn(TermLabel nextWord, TSNodeLabel wordTreeNode, int i) {
				this.nextWordTermLabel = nextWord;
				this.nextWordNode = wordTreeNode;
				this.nextWordLabel = nextWord.label;
				this.nextWordIndex = i;
				initVariabels();
			}


			private void initVariabels() {
				scanStatesSet = new HashMap<TermLabel, HashSet<SymbolicChartState>>();
				subBackwardFirstStatesSet = new HashMap<TermLabel, HashSet<SymbolicChartState>>();
				subForwardFirstStatesSet = new HashMap<TermLabel, HashSet<SymbolicChartState>>();
				completeStatesSet = new HashMap<TermLabel, HashSet<SymbolicChartState>>();
				firstLexFringeFragMapping = new HashMap<FragFringeUnambigous, TSNodeLabel>();
				firstSubFringeFragMapping = new HashMap<FragFringeUnambigous, TSNodeLabel>();
				populateFirstLexFragFringeSet();
				populateFirstSubFragFringeSet();
			}

			private void populateFirstLexFragFringeSet() {
				firstLexFragFringeSet = new HashMap<TermLabel, HashSet<FragFringeUnambigous>>();
				if (nextWordIndex == sentenceLength) {
					return;
				}

				HashMap<Label, HashSet<TSNodeLabel>> lexFragTable = 
					(nextWordIndex > 0) ? 
							firstLexFragments.get(nextWordLabel) : 
							initFragments.get(nextWordLabel);

				if (lexFragTable == null)
					return;

				for (Entry<Label, HashSet<TSNodeLabel>> e : lexFragTable.entrySet()) {					
					HashSet<FragFringeUnambigous> filteredRootSet = new HashSet<FragFringeUnambigous>(); 
					for (TSNodeLabel frag : e.getValue()) {
						if (fragInTreeFromTerm(frag.getLeftmostLexicalNode(), nextWordNode)) {
							FragFringeUnambigous ff = new FragFringeUnambigous(frag);
							filteredRootSet.add(ff);
							firstLexFringeFragMapping.put(ff, frag);
						}
					}
					if (!filteredRootSet.isEmpty()) {						
						TermLabel root = TermLabel.getTermLabel(e.getKey(), false);
						firstLexFragFringeSet.put(root, filteredRootSet);
					}
				}
			}

			private void populateFirstSubFragFringeSet() {
				firstSubFragFringeSet =  new HashMap<TermLabel, HashSet<FragFringeUnambigous>>();
				if (nextWordIndex == 0) {
					return;
				}
				boolean stopFrag = nextWordIndex == sentenceLength;
				HashMap<Label, HashSet<TSNodeLabel>> subTable = firstSubFragments.get(nextWordLabel);
				
				if (subTable == null)
					return;
				for (Entry<Label, HashSet<TSNodeLabel>> e : subTable.entrySet()) {					
					HashSet<FragFringeUnambigous> filteredFirstTermSet = new HashSet<FragFringeUnambigous>();
					for (TSNodeLabel frag : e.getValue()) {
						if (stopFrag || fragInTreeFromTerm(frag.getLeftmostLexicalNode(), nextWordNode)) {
							FragFringeUnambigous ff = new FragFringeUnambigous(frag);
							filteredFirstTermSet.add(ff);
							firstSubFringeFragMapping.put(ff, frag);
						}
					}
					if (!filteredFirstTermSet.isEmpty()) {
						TermLabel firstTerm = TermLabel.getTermLabel(e.getKey(), false);
						firstSubFragFringeSet.put(firstTerm, filteredFirstTermSet);
					}
				}
			}

			public ChartColumn previousChartColumn() {
				return chart.get(nextWordIndex - 1);
			}

			public String toString() {
				StringBuilder sb = new StringBuilder();
				sb.append("Chart Column\n");
				sb.append("\tWord index: " + nextWordIndex + "\n");
				sb.append("\tWord: " + nextWordTermLabel + "\n");
				sb.append("\tScan States: " + scanStatesSet.size() + "\n");
				sb.append("\tSub-Down States: " + Utility.countTotal(subBackwardFirstStatesSet) + "\n");
				sb.append("\tSub-Up States: " + Utility.countTotal(subForwardFirstStatesSet) + "\n");
				sb.append("\tComplete States: " + Utility.countTotal(completeStatesSet));
				return sb.toString();
			}
			
			public boolean addProbState(
					HashMap<TermLabel, HashSet<SymbolicChartState>> table,
					TermLabel firstKey, SymbolicChartState currentState) {

				HashSet<SymbolicChartState> subSet = table.get(firstKey);
				if (subSet == null) {
					subSet = new HashSet<SymbolicChartState>();					
					table.put(firstKey, subSet);										
				}
				
				return subSet.add(currentState);

			}

			private void performStart() {
				for (Entry<TermLabel, HashSet<FragFringeUnambigous>> e : firstLexFragFringeSet.entrySet()) {
					for (FragFringeUnambigous lexFragFringe : e.getValue()) {
						SymbolicChartState cs = new SymbolicChartState(lexFragFringe, 0, 0);
						Utility.putInHashMap(scanStatesSet, lexFragFringe.root, cs);
						localCondProbModel.addTrainEventInit(lexFragFringe);
					}
				}
			}

			private void performScan() {
				ChartColumn nextChartColumn = chart.get(nextWordIndex + 1);
				for (HashSet<SymbolicChartState> scanRootSet : scanStatesSet.values()) {
					for (SymbolicChartState scanState : scanRootSet) {
						SymbolicChartState newState = new SymbolicChartState(scanState, nextChartColumn.nextWordIndex);
						newState.advanceDot();
						nextChartColumn.identifyAndAddSymbolicState(newState);
						// always added apart when next is lex and lookahead doesn't match
					}
				}
			}

			private void performSubBackward() {

				Set<TermLabel> firstSubSiteSet = subBackwardFirstStatesSet.keySet();
				Set<TermLabel> secondRootSet = firstLexFragFringeSet.keySet();

				firstSubSiteSet.retainAll(secondRootSet);
				secondRootSet.retainAll(firstSubSiteSet);

				for (TermLabel root : secondRootSet) {
					HashSet<FragFringeUnambigous> subBackwardSecondFringeSet = firstLexFragFringeSet.get(root);
					HashSet<SymbolicChartState> firstSubBackwardSetMatch = subBackwardFirstStatesSet.get(root);					

					for (FragFringeUnambigous subDownSecondFragFringe : subBackwardSecondFringeSet) {						
						SymbolicChartState newState = new SymbolicChartState(
								subDownSecondFragFringe, nextWordIndex, nextWordIndex);
						Utility.putInHashMap(scanStatesSet, subDownSecondFragFringe.root, newState);
						// new state always new
						
						localCondProbModel.addTrainEventSetSubBackward(firstSubBackwardSetMatch, subDownSecondFragFringe);

					}
				}
			}

			private void performSubForward() {

				Set<TermLabel> firstRootSet = subForwardFirstStatesSet.keySet();
				Set<TermLabel> secondSubSiteSet = firstSubFragFringeSet.keySet();

				firstRootSet.retainAll(secondSubSiteSet);
				secondSubSiteSet.retainAll(firstRootSet);

				for (TermLabel secondSubSite : secondSubSiteSet) {
					HashSet<FragFringeUnambigous> subForwardSecondFringeSet = firstSubFragFringeSet.get(secondSubSite);
					HashSet<SymbolicChartState> firstSubForwardSetMatch = subForwardFirstStatesSet.get(secondSubSite);
										
					for (FragFringeUnambigous subForwardSecondFragFringe : subForwardSecondFringeSet) {
						SymbolicChartState newState = new SymbolicChartState(subForwardSecondFragFringe, 0, nextWordIndex);
						newState.advanceDot();
						Utility.putInHashMap(scanStatesSet, subForwardSecondFragFringe.root, newState);
						// new state always new
						
						localCondProbModel.addTrainEventSetSubForward(firstSubForwardSetMatch, subForwardSecondFragFringe);
						
					}
				}
			}

			private void performCompleteSlow() {

				if (completeStatesSet.isEmpty())
					return;

				ChartStateStartIndexPriorityQueue completeStatesQueue = 
					new ChartStateStartIndexPriorityQueue(nextWordIndex);

				for (HashSet<SymbolicChartState> completeStates : completeStatesSet.values()) {
					for (SymbolicChartState s : completeStates) {
						completeStatesQueue.add(s);
					}
				}

				while (!completeStatesQueue.isEmpty()) {
					SymbolicChartState cs = completeStatesQueue.poll();
					int index = cs.startIndex;
					TermLabel root = cs.fragFringe.root;
					HashSet<SymbolicChartState> pastStateSubDownSet = 
						chart.get(index).subBackwardFirstStatesSet.get(root);

					for (SymbolicChartState pastStateSubDown : pastStateSubDownSet) {
						SymbolicChartState newState = new SymbolicChartState(pastStateSubDown, nextWordIndex);
						newState.advanceDot();
						if (identifyAndAddSymbolicState(newState)) {
							if (newState.isCompleteState())
								completeStatesQueue.add(newState);
						}
					}

				}

			}

			private class ChartStateStartIndexPriorityQueue {

				int pastColumns;
				Vector<HashMap<TermLabel, HashSet<SymbolicChartState>>> completeStatesTable;
				int[] sizes;
				int totalSize;

				public ChartStateStartIndexPriorityQueue(int wordIndex) {
					this.pastColumns = wordIndex;
					completeStatesTable = new Vector<HashMap<TermLabel, HashSet<SymbolicChartState>>>(wordIndex);
					sizes = new int[wordIndex];
					for (int i = 0; i < wordIndex; i++) {
						completeStatesTable.add(new HashMap<TermLabel, HashSet<SymbolicChartState>>());
					}
				}

				public boolean isEmpty() {
					return totalSize == 0;
				}

				public boolean add(SymbolicChartState cs) {
					int index = cs.startIndex;
					TermLabel root = cs.root();
					HashMap<TermLabel, HashSet<SymbolicChartState>> table = completeStatesTable.get(index);
					HashSet<SymbolicChartState> present = table.get(root);
					if (present == null) {
						present = new HashSet<SymbolicChartState>();
						table.put(root, present);
						
					}
					if (present.add(cs)) {
						sizes[index]++;
						totalSize++;
						return true;
					}

					return false;
				}

				public SymbolicChartState poll() {
					for (int i = pastColumns - 1; i >= 0; i--) {
						if (sizes[i] == 0)
							continue;
						HashMap<TermLabel, HashSet<SymbolicChartState>> table = completeStatesTable.get(i);
						Iterator<Entry<TermLabel, HashSet<SymbolicChartState>>> it1 = table.entrySet().iterator();
						HashSet<SymbolicChartState> rootTable = it1.next().getValue();
						Iterator<SymbolicChartState> it2 = rootTable.iterator();
						SymbolicChartState result = it2.next();
						it2.remove();
						sizes[i]--;
						totalSize--;
						if (rootTable.isEmpty())
							it1.remove();
						return result;
					}
					return null;
				}

			}

			public boolean identifyAndAddSymbolicState(SymbolicChartState cs) {

				if (!cs.hasElementAfterDot()) {
					TermLabel root = cs.root();
					if (cs.isStarred)
						return Utility.putInHashMap(subForwardFirstStatesSet, root, cs);
					return Utility.putInHashMap(completeStatesSet, root, cs);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {
					if (nextTerm == nextWordTermLabel) {
						return Utility.putInHashMap(scanStatesSet, cs.root(), cs);
					}
					return false;
				}
				return Utility.putInHashMap(subBackwardFirstStatesSet, nextTerm, cs);
			}

		}

	}
	

/*
	public static void main(String[] args) throws Exception {

		threads = 7;

		addSTOP = false;

		initFragSmoothing = false;
		minFreqFirstWordFragSmoothing = 1E-1;

		String basePath = Parameters.scratchPath;

		String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4_notop/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4_Stop/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4/";
		// String workingPath = basePath + "/PLTSG/Chelba/Chelba_Right_H0_V1/";
		// String workingPath = basePath + "/PLTSG/Chelba/Chelba_Right_H2_V1/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkMBasic_UkT5/";

		File trainTB = new File(workingPath + "wsj-02-21.mrg");
		File trainTBsmall = new File(workingPath + "wsj-02-21_first1000.mrg");
		File testTB = new File(workingPath + "wsj-24.mrg");
		File fragFile = new File(workingPath + "fragments_approxFreq.txt");

		String fragExtrWorkingPath = workingPath + "FragmentExtractor/";
		String outputPathParsing = workingPath + "LexAttachAnalysis/";
		new File(fragExtrWorkingPath).mkdirs();
		new File(outputPathParsing).mkdirs();

		FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(
				fragExtrWorkingPath, "NoSmoothing", "NoSmoothing", trainTB, testTB, fragFile);

		String condProbModelId = CondProbModelFactory.modelId_basicNorm; 
		new IncrementalTsgAdvancedTrainer(FE, trainTB, outputPathParsing, 
				condProbModelId).run();

	}
*/
	
}
