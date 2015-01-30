package tsg.incremental.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.corpora.Wsj;
import tsg.incremental.FragFringe;
import tsg.incremental.FringeCompatibility;
import tsg.incremental.old.IncrementalTSGEarlyParser.ChartParserThread.ChartColumn;
import tsg.incremental.old.IncrementalTSGEarlyParserViterbiSD.ChartParserThread.ChartColumn.ChartState;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationRight_LC;
import tsg.parseEval.EvalC;
import util.Duet;
import util.Pair;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class IncrementalTSGEarlyParserViterbiSD extends Thread {
	
	static final Label TOPlabel = Label.getLabel("TOP");
	static final TermLabel TOPnode = TermLabel.getTermLabel(TOPlabel, false);
	static final TermLabel emptyLexNode = TermLabel.getTermLabel("", true);
	
	static boolean debug = true;
	static int treeLenghtLimit = -1;	
	static long maxSecPerWord = 5*60;	
	static long threadCheckingTime = 10000;	
	static int threads = 1;
	static int limitTestToSentenceIndex = -1;
	static boolean removeFringesNonCompatibleWithSentence;
	
	File testTBfile, logFile, parsedFile, evalFile, goldTBFile;	
	FragmentExtractor FE;
	String outputPath;
	ArrayList<TSNodeLabel> testTB;
	TSNodeLabel[] parsedTB;		
	PrintProgress progress;	
	int parsedTrees, reachedTopTrees, interruptedTrees, indexLongestWordTime;
	float longestWordTime;
	Iterator<TSNodeLabel> testTBiterator;
	int treebankIteratorIndex;
	boolean smoothing;
	EvalC evalC;
	
	
	
	HashMap<TermLabel,  //indexed on lex
		HashMap<TermLabel, HashMap<FragFringe, Double>>>
		firstLexFringes, // and root  
		firstSubFringes; // and firstTerm
	
	HashMap<TermLabel,  //indexed on pos
		HashMap<TermLabel, HashMap<FragFringe, Double>>>
		firstLexFringesSmoothing, // and root
		firstSubFringesSmoothing; // and first Term
	
	HashMap<TermLabel, HashSet<TermLabel>> posLexTable, lexPosTable;
	Set<TermLabel> posSet;
	
	HashMap<FragFringe,HashSet<TSNodeLabel>> lexFringeFragMap;
	HashMap<FragFringe,HashSet<TSNodeLabel>> templateFringeFragMap;
	

	
	public IncrementalTSGEarlyParserViterbiSD (FragmentExtractor FE,
			File testTBfile, String outputPath) throws Exception {
		this.FE = FE;
		this.testTBfile = testTBfile;
		this.testTB = Wsj.getTreebank(testTBfile);
		this.outputPath = outputPath;
		this.smoothing = FragmentExtractor.smoothingFromFrags || FragmentExtractor.smoothingFromMinSet;				
		
		String logFilename = outputPath + "log_earlyParserViterbiSD_";
		logFilename += treeLenghtLimit==-1 ? "ALL" : "upTo" + treeLenghtLimit;
		String minSetThreshold = FragmentExtractor.minFragFreqForSmoothingFromMinSet == -1 ?
				"NoThreshold" : ""+FragmentExtractor.minFragFreqForSmoothingFromMinSet;
		String fragThreshold = FragmentExtractor.minFragFreqForSmoothingFromFrags == -1 ?
				"NoThreshold" : ""+FragmentExtractor.minFragFreqForSmoothingFromFrags;
		if (FragmentExtractor.addMinimalFragments) {
			logFilename+= "_MinSetExt(" + minSetThreshold + ")";			
		}
		if (FE.fragmentsFile!=null) {
			logFilename+= "_Frags(" + fragThreshold + ")";			
		}
		logFile = new File(logFilename + ".txt");
		
		parsedFile = new File(logFilename + ".mrg");
		evalFile = new File(logFilename + ".evalC"); 		
		goldTBFile = new File(logFilename + "_GOLD.mrg");
		
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
			parseIncremental();
			evaluate();
			printFinalSummary();		
		Parameters.closeLogFile();			
	}

	private void printParameters() {
		Parameters.reportLineFlush("\n\n");
		Parameters.reportLineFlush("-----------------------------------------------------");
		Parameters.reportLineFlush("INCREMENTAL TSG EARLY VITERBI SHORTEST DERIVATION ");
		Parameters.reportLineFlush("-----------------------------------------------------");
		Parameters.reportLineFlush("");
		Parameters.reportLineFlush("Test Tree Length Limit: " + treeLenghtLimit);		
		Parameters.reportLineFlush("Max sec Per Tree: " + maxSecPerWord);
		Parameters.reportLineFlush("Number of threads: " + threads);
	}
	
	private void printFinalSummary() {
		Parameters.reportLineFlush("Number of successfully parsed trees: " + parsedTrees);
		Parameters.reportLineFlush("of which reached TOP: " + reachedTopTrees);
		Parameters.reportLineFlush("Number of interrupted trees: " + interruptedTrees);
		Parameters.reportLineFlush("Longest time to parse a word (sec): " + longestWordTime + " (" + indexLongestWordTime + ")");
		Parameters.reportLineFlush("Final evalC scores: " + Arrays.toString(evalC.getRecallPrecisionFscore()));
		
	}
	
	private void convertPosSet() {
		posLexTable = convertTable(FE.posLexFinal, false, true);
		lexPosTable = convertTable(FE.lexPosFinal, true, false);
		posSet = posLexTable.keySet();
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

	private void convertLexFragmentsToFringes() {
		
		Parameters.reportLineFlush("Converting lex fragments into fringes...");
		
		firstLexFringes = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, Double>>>();
		firstSubFringes = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, Double>>>();
		
		lexFringeFragMap = new HashMap<FragFringe,HashSet<TSNodeLabel>>() ;		
		
		int countFirstLexFrags = 0;
		int countFirstLexFringes = 0;
		int countFirstSubFrags = 0;
		int countFirstSubFringes = 0;
		
		for(Entry<Label, HashSet<TSNodeLabel>> e : FE.lexFragsTableFirstLex.entrySet()) {
			TermLabel firstLexLabel = TermLabel.getTermLabel(e.getKey(),true);
			HashSet<TSNodeLabel> fragSet = e.getValue();
			countFirstLexFrags += fragSet.size();
			for(TSNodeLabel firstLexFrag : fragSet) {
				FragFringe ff = new FragFringe(firstLexFrag);
				Utility.putInHashMap(lexFringeFragMap, ff, firstLexFrag);
				if (initHashMapDouble(firstLexFringes, firstLexLabel, ff.root(), ff))
					countFirstLexFringes++;				
			}	
		}
		
		for(Entry<Label, HashSet<TSNodeLabel>> e : FE.lexFragsTableFirstSub.entrySet()) {
			TermLabel lexLabel = TermLabel.getTermLabel(e.getKey(), true);
			HashSet<TSNodeLabel> fragSet = e.getValue();
			countFirstSubFrags += fragSet.size();
			for(TSNodeLabel firstSubFrag : fragSet) {
				FragFringe ff = new FragFringe(firstSubFrag);
				Utility.putInHashMap(lexFringeFragMap, ff, firstSubFrag);
				if (initHashMapDouble(firstSubFringes, lexLabel, ff.firstTerminal(), ff))
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
	
	public static  boolean initHashMapDouble(
			HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, Double>>> table,
			TermLabel firstKey, TermLabel secondKey, FragFringe ff) {
		
		HashMap<TermLabel, HashMap<FragFringe, Double>> mapValue = table.get(firstKey);
		if (mapValue==null) {
			mapValue = new HashMap<TermLabel, HashMap<FragFringe, Double>>();
			table.put(firstKey,mapValue);
		}
		return initHashMap(mapValue, secondKey, ff);	
	}

	static final Double minusOne = (double)-1;
	
	public static boolean initHashMap(
			HashMap<TermLabel, HashMap<FragFringe, Double>> table,
			TermLabel key, FragFringe ff) {
		
		HashMap<FragFringe, Double> value = table.get(key);
		if (value==null) {
			value = new HashMap<FragFringe, Double>();
			table.put(key,value);
		}
		return value.put(ff,minusOne)==null;	
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void convertSmoothedFragmentsToFringes() {
		
		Parameters.reportLineFlush("Converting smoothed fragments into fringes...");
		
		firstLexFringesSmoothing = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe,Double>>>();
		firstSubFringesSmoothing = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe,Double>>>();
		
		templateFringeFragMap = new HashMap<FragFringe,HashSet<TSNodeLabel>>();
		
		int[] countFirstLexSubFrags = new int[2];
		int[] countFirstLexSubFringes = new int[2];
		
		for(Entry<Label, HashSet<TSNodeLabel>> e : FE.posFragSmoothingMerged.entrySet()) {
			Label firstTermLabel = e.getKey();
			TermLabel firstPosLabel = TermLabel.getTermLabel(firstTermLabel,false);
			HashSet<TSNodeLabel> fragSet = e.getValue();
			for(TSNodeLabel frag : fragSet) {
				FragFringe ff = new FragFringe(frag);
				Utility.putInHashMap(templateFringeFragMap, ff, frag);
				if (ff.firstTerminalIsLexical()) {
					countFirstLexSubFrags[0]++;
					if (initHashMapDouble(firstLexFringesSmoothing, firstPosLabel, ff.root(), ff))
						countFirstLexSubFringes[0]++;					
					
				}
				else {
					countFirstLexSubFrags[1]++;
					if (initHashMapDouble(firstSubFringesSmoothing, firstPosLabel, ff.firstTerminal(), ff))
						countFirstLexSubFringes[0]++;
				}
			}
		}
		
		Parameters.reportLineFlush("Total number of smoothed fragments: " + (Utility.sum(countFirstLexSubFrags)));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexSubFrags[0]);
		Parameters.reportLineFlush("\tSub first: " + countFirstLexSubFrags[1]);
		Parameters.reportLineFlush("Total number of smoothed fringes: " + (Utility.sum(countFirstLexSubFringes)));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexSubFringes[0]);
		Parameters.reportLineFlush("\tSub first: " + countFirstLexSubFringes[1]);
		
	}
	
	public HashSet<TSNodeLabel> getOriginalFrags(FragFringe ff) {		 
		 HashSet<TSNodeLabel> result = lexFringeFragMap.get(ff);
		 if (result==null) {
			 FragFringe ffCopy = ff.cloneFringe();
			 boolean isFirstLex = ffCopy.firstTerminalIsLexical();
			 int lexIndex = isFirstLex ? 0 : 1;
			 Label lexLabel = ffCopy.yield[lexIndex].label;
			 ffCopy.yield[lexIndex] = emptyLexNode;			 			 
			 HashSet<TSNodeLabel> anonymizedFrags =  templateFringeFragMap.get(ffCopy);
			 result = new HashSet<TSNodeLabel>();
			 for(TSNodeLabel frag : anonymizedFrags) {
				 frag = frag.clone();
				 frag.getLeftmostLexicalNode().label = lexLabel;
				 result.add(frag);
			 }
		 }
		 return result;
		 
	}

	private void printFringesToFile() {
		// TODO Auto-generated method stub
		
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

	private void parseIncremental() {
		
		Parameters.reportLineFlush("Parsing incrementally...");		
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
		
		this.parsedTB = new TSNodeLabel[testTB.size()];
		
		progress = new PrintProgress("Parsing tree:", 1, 0);		
		testTBiterator = testTB.iterator();
		
		Parameters.logLineFlush("\n");
		
		ChartParserThread[] threadsArray = new ChartParserThread[threads];
		for(int i=0; i<threads; i++) {
			ChartParserThread t = new ChartParserThread();
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
			for(int i=0; i<threads; i++) {
				ChartParserThread t = threadsArray[i];
				if (t.isAlive()) {
					someRunning = true;			
					SimpleTimer wordTimer = t.currentWordTimer;
					if (wordTimer==null || !wordTimer.isRunning()) continue;
					float runningTimeSec = wordTimer.checkEllapsedTimeSecDecimal();					
					if (runningTimeSec > maxSecPerWord)
						t.interrupt();
					if (runningTimeSec > longestWordTime) {
						longestWordTime = runningTimeSec;
						indexLongestWordTime = t.sentenceIndex;
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
	
	static final TreeMarkoBinarization TMB = new TreeMarkoBinarizationRight_LC();
	private void evaluate() {
		PrintWriter pw = FileUtil.getPrintWriter(parsedFile);
		int i=0;
		for(TSNodeLabel t : parsedTB) {
			if (t==null) {
				t = TSNodeLabel.defaultWSJparse(testTB.get(i).toFlatWordArray(), "TOP");
			}
			else {
				t = TMB.undoMarkovBinarization(t);
			}
			String tString = t.toString().replace("@", "");
			pw.println(tString);
			//pw.println(t);
			i++;
		}		
		pw.close();
		pw = FileUtil.getPrintWriter(goldTBFile);
		for(TSNodeLabel t : testTB) {
			t = TMB.undoMarkovBinarization(t);
			String tString = t.toString().replace("@", "");
			pw.println(tString);
			//pw.println(t);
		}		
		pw.close();	
		evalC = new EvalC(goldTBFile, parsedFile, evalFile);
		evalC.makeEval();
	}

	private synchronized ArrayList<Label> getNextSentence(int[] index) {
		if (!testTBiterator.hasNext()) 
			return null;			
		index[0] = treebankIteratorIndex++;
		return testTBiterator.next().collectLexicalLabels();
	}
	
	private synchronized TSNodeLabel getNextTree(int[] index) {
		if (!testTBiterator.hasNext()) 
			return null;			
		index[0] = treebankIteratorIndex++;
		return testTBiterator.next();
	}
	

	
	public static TSNodeLabel combineDerivation(ArrayList<TSNodeLabel> derivation) {
		Iterator<TSNodeLabel> derivationIter = derivation.iterator();
		TSNodeLabel result = derivationIter.next().clone();
		LinkedList<TSNodeLabel> subSitesList = new LinkedList<TSNodeLabel>();  
		result.addSubSites(subSitesList, true);
		while(derivationIter.hasNext()) {
			TSNodeLabel nextFrag = derivationIter.next().clone();
			if (subSitesList.isEmpty()) {
				//SUB-UP
				nextFrag.addSubSites(subSitesList,false);
				TSNodeLabel subSite = subSitesList.removeFirst();
				subSite.assignDaughters(result.daughters);
				result = nextFrag;
			}
			else {
				//SUB-DOWN
				TSNodeLabel subSite = subSitesList.removeFirst();
				subSite.assignDaughters(nextFrag.daughters);
				nextFrag.addSubSites(subSitesList,true);
			}
		}
		return result;
	}

	protected class ChartParserThread extends Thread {
		
		int sentenceIndex;	
		TSNodeLabel goldTestTree;
				 	
		ArrayList<Label> wordLabels;
		TermLabel[] wordTermLabels;
		HashSet<TermLabel> wordTermLabelsSet;
		int sentenceLength;
		Vector<HashMap<TermLabel,HashMap<FragFringe,Double>>> wordFragFringeFirstLex, wordFragFringeFirstSub;
		Vector<ChartColumn> chart;
		int[][] fringeCounterFirstLexSubBin;
		int[] totalFringesLexSub, removedFringesLexSub;
		boolean parsingSuccess, reachedTop;
		boolean finished = false;
		SimpleTimer currentWordTimer = new SimpleTimer(), treeTimer = new SimpleTimer();
		int[][] wordScanSize;
		int[][] wordSubDownFirstSize;
		int[][] wordSubDownSecondSize;
		int[][] wordSubUpFirstSize;
		int[][] wordSubUpSecondSize;
		int[][] wordCompleteSize;
		String[] wordTimes;
		String initChartTime;
		
		public void run() {
			//ArrayList<Label> sentenceWords = null;
			int[] i = {0};
			//while ( (sentenceWords = getNextSentence(i)) != null) {
			while ( (goldTestTree = getNextTree(i)) != null) {
				sentenceIndex = i[0];
				initVariables(goldTestTree.collectLexicalLabels());
				parse();
			}			
		}

		private void initVariables(ArrayList<Label> sentenceWords) {	
			this.wordLabels = sentenceWords;			
			this.sentenceLength = wordLabels.size();
			wordTermLabels = new TermLabel[sentenceLength];
			wordTermLabelsSet = new HashSet<TermLabel>();
			int i=0;
			for(Label l : wordLabels) {
				TermLabel tl = TermLabel.getTermLabel(l, true);
				wordTermLabels[i++] = tl;
				wordTermLabelsSet.add(tl);
			}
			wordTimes = new String[sentenceLength+1];
			wordScanSize = new int[2][sentenceLength+1];
			wordSubDownFirstSize = new int[2][sentenceLength+1];
			wordSubDownSecondSize = new int[2][sentenceLength+1];
			wordSubUpFirstSize = new int[2][sentenceLength+1];
			wordSubUpSecondSize = new int[2][sentenceLength+1];
			wordCompleteSize = new int[2][sentenceLength+1];
			Arrays.fill(wordTimes, "-");
			this.wordFragFringeFirstLex = //indexed on root
					new Vector<HashMap<TermLabel,HashMap<FragFringe,Double>>>(sentenceLength);
			this.wordFragFringeFirstSub = //indexed on first term
					new Vector<HashMap<TermLabel,HashMap<FragFringe,Double>>>(sentenceLength);
			chart = new Vector<ChartColumn> (sentenceLength+1); //termstate
			fringeCounterFirstLexSubBin = new int[2][sentenceLength];
			removedFringesLexSub = new int[2];
			totalFringesLexSub = new int[2];
			this.parsingSuccess = false;
			this.reachedTop = false;
			this.finished = false;			
		}
		
		private void parse() {					
			treeTimer.start();
						
			initChart();
			
			ChartColumn startChartColumn = chart.get(0);
			startChartColumn.performStart();
			
			int i;
			for(i=0; i<=sentenceLength; i++) {
				currentWordTimer.start();
				ChartColumn c = chart.get(i);
				if (i<sentenceLength)
					c.completeSubDownSubUpScan();
				else
					c.completeAndTerminate();
				c.countFragFringes();
				wordTimes[i] = currentWordTimer.checkEllapsedTimeSecFormat();
				if (isInterrupted()) break;
			}
			
			treeTimer.stop();
			
			TSNodeLabel parsedTree = null;
			ArrayList<TSNodeLabel> derivation = null;
			HashSet<ChartState> derivationPath = null;
			double[] viterbiLogProb = new double[]{-Double.MAX_VALUE};
			
			if (parsingSuccess) {				 
				derivationPath = retrieveRandomViterbiPath(viterbiLogProb);
				derivation = retrieveFragDerivation(derivationPath);
				parsedTree = combineDerivation(derivation);
				if (parsedTree.label!=TOPlabel) {
					if (reachedTop) {
						// a parsed tree non rooted on TOP obtained a better score of all parsed tree
						// rooted on TOP
						reachedTop = false;
					}
					TSNodeLabel topNode = new TSNodeLabel(TOPlabel,false);
					topNode.assignUniqueDaughter(parsedTree);
					parsedTree = topNode;
				}
			}
			
			if (debug)
				printDebug(parsedTree, derivation, derivationPath, viterbiLogProb[0]);
			
			synchronized(progress) {
				if(Thread.interrupted() && !finished)
					interruptedTrees++;
				if (parsingSuccess) parsedTrees++;
				if (reachedTop) reachedTopTrees++;			
				parsedTB[sentenceIndex] = parsedTree;
				progress.next();					
			}
			
		}
		
		private void initChart() {
			SimpleTimer t = new SimpleTimer(true);
			int i=0;
			for(Label w : wordLabels) {				
				TermLabel wordTermLabel = TermLabel.getTermLabel(w, true);
				populateFragFringesFirstLexSub(wordTermLabel, i);				
				chart.add(new ChartColumn(wordTermLabel, i));
				i++;
			}
			chart.add(new ChartColumn(null, i));
			initChartTime = t.checkEllapsedTimeSecFormat();
		}

		private void populateFragFringesFirstLexSub(TermLabel w, int index) {	

			HashMap<TermLabel,HashMap<FragFringe,Double>> wordFFlex = 
				Utility.deepHashMapDoubleClone(firstLexFringes.get(w));
			HashMap<TermLabel,HashMap<FragFringe,Double>> wordFFsub = 
					Utility.deepHashMapDoubleClone(firstSubFringes.get(w));			
			 
			if (smoothing) {
				for(TermLabel pos : lexPosTable.get(w)) {
					//first lex
					HashMap<TermLabel, HashMap<FragFringe,Double>> posFringes = firstLexFringesSmoothing.get(pos);
					if (posFringes!=null) {									
						for(Entry<TermLabel, HashMap<FragFringe,Double>> e : posFringes.entrySet()) {
							TermLabel root = e.getKey();																			
							HashMap<FragFringe,Double> presentSet = wordFFlex.get(root);							
							if (presentSet==null) {
								presentSet = new HashMap<FragFringe,Double>();
								wordFFlex.put(root, presentSet);
							}
							for(Entry<FragFringe,Double> f : e.getValue().entrySet()) {
								FragFringe ffCopy = f.getKey().cloneFringe();
								ffCopy.setInYield(0, w);
								presentSet.put(ffCopy,f.getValue());
							}
						}						
					}
					//firstSub
					posFringes = firstSubFringesSmoothing.get(pos);
					if (posFringes!=null) {
						for(Entry<TermLabel, HashMap<FragFringe,Double>> e : posFringes.entrySet()) {		
							TermLabel subSite = e.getKey();
							HashMap<FragFringe,Double> presentSet = wordFFsub.get(subSite);							
							if (presentSet==null) {
								presentSet = new HashMap<FragFringe,Double>();
								wordFFsub.put(subSite, presentSet);
							}
							for(Entry<FragFringe,Double> f : e.getValue().entrySet()) {
								FragFringe ffCopy = f.getKey().cloneFringe();
								ffCopy.setInYield(1, w);
								presentSet.put(ffCopy,f.getValue());
							}
						}
					}					
				}
			}
			
			if (removeFringesNonCompatibleWithSentence) {					
				Vector<HashMap<TermLabel,HashMap<FragFringe,Double>>> wordFFlexSub =
					new Vector<HashMap<TermLabel,HashMap<FragFringe,Double>>>(2); 
				wordFFlexSub.add(wordFFlex);
				wordFFlexSub.add(wordFFsub);
				int i=0;
				for(HashMap<TermLabel,HashMap<FragFringe,Double>> tab : wordFFlexSub) {
					int totalRemoved = 0;
					for(HashMap<FragFringe,Double> subtab : tab.values()) {
						Iterator<Entry<FragFringe, Double>> it = subtab.entrySet().iterator();
						while(it.hasNext()) {
							FragFringe ff = it.next().getKey();
							if (!isCompatibleWithFringe(ff,index)) {
								totalRemoved++;
								it.remove();
							}
						}
					}
					removedFringesLexSub[i++] += totalRemoved;
				}
			}
			
			wordFragFringeFirstLex.add(wordFFlex);		
			wordFragFringeFirstSub.add(wordFFsub);
			totalFringesLexSub[0] += Utility.countTotalDouble(wordFFlex);
			totalFringesLexSub[1] += Utility.countTotalDouble(wordFFsub);
			fringeCounterFirstLexSubBin[0][index] = wordFFlex.size();
			fringeCounterFirstLexSubBin[1][index] = wordFFsub.size();

		}
		
		private boolean isCompatibleWithFringe(FragFringe ff, int index) {						 
			TermLabel[] fringe = ff.yield;
			return FringeCompatibility.isCompatible(fringe, wordTermLabels, index, posLexTable);
		}

		public void toStringTex(PrintWriter pw, 
				HashSet<ChartState> derivationPath, boolean onlyDerivation) {

			pw.print("\n\\begin{tabular}{|c|" + Utility.repeat("ll|",sentenceLength) + "l|}\n");
			pw.print("\\hline\n");
			
			int i=0;
			for(Label l : wordLabels) {
				pw.print("& \\multicolumn{2}{c|}{ " +
						"\\begin{tabular}{c} " +
						"\\Huge{(" + i + ")} \\\\ " +
						"\\Huge{" + l.toString() + "}" +
						"\\end{tabular} }" );
				i++;
			}
			pw.print("& \\multicolumn{1}{c|}{\\Huge{STOP}}");
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SCAN} &\n");
			for(i=0; i<sentenceLength; i++) {
				pw.print("\\multicolumn{2}{l|}{ " +
						toStringTexTable(chart.get(i).scanStatesSet, derivationPath, onlyDerivation) + 
						"}");
				pw.print(" & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SUB-DOWN} &\n");
			for(i=0; i<sentenceLength; i++) {
				ChartColumn cc = chart.get(i);
				pw.print(toStringTexTable(cc.subDownFirstStatesSet, derivationPath, onlyDerivation));
				pw.print(" & \n");
				pw.print(toStringFragTex(cc.subDownSecondStateSet, derivationPath, i, true, onlyDerivation));
				pw.print(" & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SUB-UP} &\n");
			for(i=0; i<=sentenceLength; i++) {
				ChartColumn cc = chart.get(i);
				if (i<sentenceLength) {
					pw.print(toStringTexTable(chart.get(i).subUpFirstStatesSet, derivationPath, onlyDerivation));
					pw.print(" & \n");
					pw.print(toStringFragTex(cc.subUpSecondFragFringeSet, derivationPath, i, false, onlyDerivation));
					pw.print(" & \n");
				}
				else {
					pw.print("\\multicolumn{1}{l|}{ " +
							toStringTexTable(chart.get(i).subUpFirstStatesSet, derivationPath, onlyDerivation) + 
							"}");
					pw.print("\n");
				}
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{COMPLETE} &\n");
			for(i=0; i<=sentenceLength; i++) {
				int cols = i==sentenceLength ? 1 : 2;
				pw.print("\\multicolumn{" + cols + "}{l|}{ " +
						toStringTexTable(chart.get(i).completeStatesSet, derivationPath, onlyDerivation) + 
						"}");
				pw.print(i==sentenceLength ? "\n" : " & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\end{tabular}\n");
		}

		
		private void printDebug(TSNodeLabel parsedTree,
				ArrayList<TSNodeLabel> derivation, 
				HashSet<ChartState> derivationPath,
				double viterbiLogProb) {
			synchronized(logFile) {				
				Parameters.logLine("Sentence #                                  " + sentenceIndex);
				Parameters.logLine("Sentence Length:                            " + sentenceLength);	
				Parameters.logLine("Total used fringes lex/sub:                 " + Arrays.toString(totalFringesLexSub));
				Parameters.logLine("Total removed fringes lex/sub:              " + Arrays.toString(removedFringesLexSub));
				
				
				
				
				Parameters.logLine("Sentence words:                             " + Utility.removeBrackAndDoTabulation(wordLabels.toString()));
				
				Parameters.logLine("SCAN-set sizes (bin):                       " + Utility.formatIntArray(wordScanSize[0]));
				Parameters.logLine("SCAN-set sizes:                             " + Utility.formatIntArray(wordScanSize[1]));
				
				
				
				Parameters.logLine("SUB-DOWN-FIRST-set sizes (bin):             " + Utility.formatIntArray(wordSubDownFirstSize[0]));
				Parameters.logLine("SUB-DOWN-FIRST-set sizes:                   " + Utility.formatIntArray(wordSubDownFirstSize[1]));
				Parameters.logLine("SUB-DOWN-SECOND-set sizes (bin) NO FILTER:  " + Utility.formatIntArray(fringeCounterFirstLexSubBin[0]));
				Parameters.logLine("SUB-DOWN-SECOND-set sizes (bin):            " + Utility.formatIntArray(wordSubDownSecondSize[0]));
				Parameters.logLine("SUB-DOWN-SECOND-set sizes:                  " + Utility.formatIntArray(wordSubDownSecondSize[1]));
				Parameters.logLine("SUB-UP-FIRST-set sizes (bin):               " + Utility.formatIntArray(wordSubUpFirstSize[0]));
				Parameters.logLine("SUB-UP-FIRST-set sizes:                     " + Utility.formatIntArray(wordSubUpFirstSize[1]));
				Parameters.logLine("SUB-UP-SECOND-set sizes (bin):  NO FILTER   " + Utility.formatIntArray(fringeCounterFirstLexSubBin[1]));
				Parameters.logLine("SUB-UP-SECOND-set sizes (bin):              " + Utility.formatIntArray(wordSubUpSecondSize[0]));
				Parameters.logLine("SUB-UP-SECOND-set sizes:                    " + Utility.formatIntArray(wordSubUpSecondSize[1]));
				Parameters.logLine("COMPLETE-set sizes (bin):                   " + Utility.formatIntArray(wordCompleteSize[0]));
				Parameters.logLine("COMPLETE-set sizes:                         " + Utility.formatIntArray(wordCompleteSize[1]));
				
				Parameters.logLine("Total Running time (sec):                   " + treeTimer.readTotalTimeSecFormat());
				Parameters.logLine("Chart init time (sec):                      " + initChartTime);
				Parameters.logLine("Word times (sec):                           " + Utility.removeBrackAndDoTabulation(Arrays.toString(wordTimes)));				
				Parameters.logLine("Finished:                                   " + finished);							
				Parameters.logLine("Successfully parsed:                        " + parsingSuccess);
				Parameters.logLine("Reached top:                                " + reachedTop);			

				if (parsingSuccess) {
					Parameters.logLine("Viterbi best log prob: " + viterbiLogProb);
					Parameters.logLine("Viterbi best derivation:\n" + toStringTex(derivation));					
					Parameters.logLine("Parsed tree: " + parsedTree.toStringQtree());
					Parameters.logLine("Gold tree: " + goldTestTree.toStringQtree());
					Parameters.logLine("EvalC Scores: " + Arrays.toString(EvalC.getRecallPrecisionFscore(parsedTree, goldTestTree)));
					if (-(int)viterbiLogProb!=derivation.size()) {
						Parameters.reportError("\nDerivation length does not match: " + sentenceIndex);
					}
				}
				
				Parameters.logLineFlush("");
				
				if (limitTestToSentenceIndex!=-1) {				
					File tableFile = new File(outputPath + "chart.txt");
					PrintWriter pw = FileUtil.getPrintWriter(tableFile);
					this.toStringTex(pw,derivationPath,true);
					pw.close();
					System.out.println("Total elements in chart: " + totalElementsInChart() );
				}
				
			}
		}
		
		private int totalElementsInChart() {
			return Utility.sum(wordSubDownFirstSize[1]) +
					Utility.sum(wordSubDownSecondSize[1]) +
					Utility.sum(wordSubUpFirstSize[1]) +
							Utility.sum(wordSubUpSecondSize[1]) +
									Utility.sum(wordCompleteSize[1]);
		}
				
		private String toStringTex(ArrayList<TSNodeLabel> derivation) {
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{" + Utility.repeat("c", derivation.size()) + "}\n");
			Iterator<TSNodeLabel> iter = derivation.iterator();
			while(iter.hasNext()) {
				sb.append(iter.next().toStringTex(false,true));
				if (iter.hasNext())
					sb.append(" $\\circ$ &");
				sb.append(" \n");
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}
		
		static final String highlight = "\\rowcolor{lemon}";
		
		public String toStringTexTable(
				HashMap<TermLabel,HashMap<ChartColumn.ChartState,double[]>> table,
				HashSet<ChartState> derivationPath, boolean onlyDerivation) {
			
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{llll}\n");		
			for(HashMap<ChartColumn.ChartState,double[]> csSet : table.values()) {			
				for(Entry<ChartColumn.ChartState,double[]> csProb : csSet.entrySet()) {
					ChartState cs = csProb.getKey();
					boolean isInDerivation = derivationPath!=null && derivationPath.contains(cs);
					if (isInDerivation) {
						String initString = isInDerivation && !onlyDerivation ?
								"\t" + highlight : "\t";
						sb.append(initString + csProb.getKey().toStringTex() + " [" + csProb.getValue()[0] + "] " + "\\\\\n");
					}
					else if (!onlyDerivation) {
						sb.append("\t" + csProb.getKey().toStringTex() + " [" + csProb.getValue()[0] + "] " + "\\\\\n");
					}					
				}
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}
		
		public String toStringFragTex(
				HashMap<TermLabel, HashMap<FragFringe, Double>> table,
				HashSet<ChartState> derivationPath, int wordIndex, boolean subDown,
				boolean onlyDerivation) {
			
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{lll}\n");							
			for(HashMap<FragFringe, Double> set : table.values()) {
				for(Entry<FragFringe, Double> e : set.entrySet()) {
					FragFringe ff = e.getKey();
					ChartState cs = chart.get(wordIndex).new ChartState(ff, subDown ? wordIndex : -1);
					
					if (!subDown)
						cs.dotIndex++;
					boolean isInDerivation = derivationPath!=null && derivationPath.contains(cs);
					if (isInDerivation) {
						String initString = isInDerivation && !onlyDerivation ?
								"\t" + highlight : "\t";
						sb.append(initString + ff.toStringTex() + " [" + e.getValue() + "]" + "\\\\\n");
					}
					else if (!onlyDerivation) {
						sb.append("\t" + ff.toStringTex() + " [" + e.getValue() + "]" + "\\\\\n");
					}
					
				}
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}

		private ArrayList<TSNodeLabel> retrieveFragDerivation(
				HashSet<ChartState> path) {
			
			FragFringe[] derivationFringessPath = getFragsFromPath(path);			
			ArrayList<TSNodeLabel> derivationFrags = new ArrayList<TSNodeLabel>();
			for(FragFringe ff : derivationFringessPath) {
				if (ff==null) 
					continue;
				derivationFrags.add(getOriginalFrags(ff).iterator().next());
			}
			return derivationFrags;
		}
		
		private FragFringe[] getFragsFromPath(HashSet<ChartState> path) {
			FragFringe[] frags = new FragFringe[sentenceLength];
			for(ChartState cs : path) {
				if (cs.isFirstStateWithCurrentWord()) {
					int index = cs.wordIndex();
					frags[index] = cs.fragFringe;
				}
			}
			return frags;
		}
		
		private HashSet<ChartState> retrieveRandomViterbiPath(double[] viterbiLogProb) {
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<ChartState, double[]> topStateSet = lastColumn.subUpFirstStatesSet.get(TOPnode);
			Entry<ChartState, double[]> topStateEntry = null;
			if (topStateSet!=null)
				topStateEntry = Utility.getMaxEntry(topStateSet);
			
			HashMap<TermLabel, HashMap<ChartState,double[]>> endStates = lastColumn.subUpFirstStatesSet;
			Entry<ChartState, double[]> nonTopStateEntry = Utility.getMaxEntryDouble(endStates);
			
			if (topStateEntry==null || nonTopStateEntry.getValue()[0] > topStateEntry.getValue()[0])
				topStateEntry = nonTopStateEntry;
			// using a non top anyway
				
			viterbiLogProb[0] = topStateEntry.getValue()[0];
			HashSet<ChartState> path = new HashSet<ChartState>();
			addViterbiBestPathRecursive(path,topStateEntry.getKey());
			return path;
		}

		private void addViterbiBestPathRecursive(HashSet<ChartState> path, ChartState cs) {			
			path.add(cs);
			Duet<ChartState, ChartState> parentPairs = cs.getViterbiBestParentStates();
			if (parentPairs==null)
				return;
			cs = parentPairs.getFirst();
			if (cs!=null) {
				addViterbiBestPathRecursive(path,cs);
			}
			cs = parentPairs.getSecond();
			if (cs!=null) {
				addViterbiBestPathRecursive(path,cs);
			}
		}
		
		private void addViterbiBestFragsRecursive(FragFringe[] path, ChartState cs) {			
			if (cs.isFirstStateWithCurrentWord()) {
				int index = cs.wordIndex();
				path[index] = cs.fragFringe;
			}
			Duet<ChartState, ChartState> parentPairs = cs.getViterbiBestParentStates();
			if (parentPairs==null)
				return;
			cs = parentPairs.getFirst();
			if (cs!=null) {
				addViterbiBestFragsRecursive(path,cs);
			}
			cs = parentPairs.getSecond();
			if (cs!=null) {
				addViterbiBestFragsRecursive(path,cs);
			}
		}

		class ChartColumn {
			
			TermLabel word;
			int wordIndex;
			
			HashMap<TermLabel,HashMap<ChartState,double[]>> 
				scanStatesSet, //indexed on root
				subDownFirstStatesSet, //indexed on non-term after dot				
				subUpFirstStatesSet, //indexed on root				
				completeStatesSet; //indexed on root
			HashMap<TermLabel,HashMap<FragFringe,Double>>
			subDownSecondStateSet, //indexed on root
			subUpSecondFragFringeSet; //indexed on first term (sub-site)				
			
			public ChartColumn (TermLabel word, int i) {
				this.word = word;
				this.wordIndex = i;
				initVariabels();
			}
			
			private void initVariabels() {
				scanStatesSet = new HashMap<TermLabel,HashMap<ChartState,double[]>>();
				subDownFirstStatesSet = new HashMap<TermLabel,HashMap<ChartState,double[]>>();
				subUpFirstStatesSet = new HashMap<TermLabel,HashMap<ChartState,double[]>>();
				completeStatesSet = new HashMap<TermLabel,HashMap<ChartState,double[]>>();
				subDownSecondStateSet = (wordIndex!=0 && wordIndex!=sentenceLength) ?
					wordFragFringeFirstLex.get(wordIndex) :
					new HashMap<TermLabel,HashMap<FragFringe,Double>>();	
				subUpSecondFragFringeSet = (wordIndex!=0 && wordIndex!=sentenceLength) ?
					wordFragFringeFirstSub.get(wordIndex) :
					new HashMap<TermLabel,HashMap<FragFringe,Double>>();				
			}

			private void performStart() {
				for(Entry<TermLabel,HashMap<FragFringe,Double>> e : wordFragFringeFirstLex.get(wordIndex).entrySet()) {
					for(Entry<FragFringe,Double> f : e.getValue().entrySet()) {
						FragFringe lexFragFringe = f.getKey();
						ChartState cs = new ChartState(lexFragFringe, -1);
						addProbStateOrMaxViterbi(scanStatesSet, lexFragFringe.root, cs, f.getValue());
					}
				}				
			}

			/**
			 * 
			 * @return true only if a new ChartState is inserted as the result of this operation
			 */
			private boolean addProbStateOrMaxViterbi(
					HashMap<TermLabel, HashMap<ChartState, double[]>> table,
					TermLabel key, ChartState cs, double prob) {
				
				HashMap<ChartState, double[]> value = table.get(key);
				if (value==null) {
					value = new HashMap<ChartState, double[]>();
					table.put(key,value);
					value.put(cs, new double[]{prob});
					return true;
				}

				return addProbStateOrMaxViterbiSimple(value, cs, prob);
			}
			
			/**
			 * 
			 * @return true only if a new ChartState is inserted of modified as the result of this operation
			 */
			private boolean addProbStateOrMaxViterbiSimple(
					HashMap<ChartState, double[]> table,
					ChartState cs, double prob) {
				
				double[] probPresent = table.get(cs);
				if (probPresent==null) {
					table.put(cs, new double[]{prob});
					return true;
				}
				if (probPresent[0]<prob) {
					probPresent[0] = prob;
					return true;					
				}	
				return false;
			}

			private ChartColumn previousChartColumn() {
				return chart.get(wordIndex-1);
			}
			

			public String toString() {
				StringBuilder sb = new StringBuilder();
				sb.append("Chart Column\n");
				sb.append("\tWord index: " + wordIndex + "\n");
				sb.append("\tWord: " + word + "\n");
				sb.append("\tScan States: " + scanStatesSet.size() + "\n");
				sb.append("\tSub-Down States: " + Utility.countTotalDouble(subDownFirstStatesSet) + "\n");
				sb.append("\tSub-Up States: " + Utility.countTotalDouble(subUpFirstStatesSet) + "\n");
				sb.append("\tComplete States: " + Utility.countTotalDouble(completeStatesSet));
				return sb.toString();
			}

			public void completeSubDownSubUpScan() {
				performComplete();
				if (isInterrupted()) return;
				performSubDown();
				if (isInterrupted()) return;
				performSubUp();				
				if (isInterrupted()) return;
				performScan();				
			}			
			
			public void completeAndTerminate() {
				performComplete();
				if (isInterrupted()) return;
				terminate();
			}

			public void countFragFringes() {
				wordScanSize[0][wordIndex] = scanStatesSet.size();
				wordScanSize[1][wordIndex] = Utility.countTotalDouble(scanStatesSet);
				wordSubDownFirstSize[0][wordIndex] = subDownFirstStatesSet.size();
				wordSubDownFirstSize[1][wordIndex] = Utility.countTotalDouble(subDownFirstStatesSet);
				wordSubDownSecondSize[0][wordIndex] = subDownSecondStateSet.size();
				wordSubDownSecondSize[1][wordIndex] = Utility.countTotalDouble(subDownSecondStateSet);
				wordSubUpFirstSize[0][wordIndex] = subUpFirstStatesSet.size();
				wordSubUpFirstSize[1][wordIndex] = Utility.countTotalDouble(subUpFirstStatesSet);
				wordSubUpSecondSize[0][wordIndex] = subUpSecondFragFringeSet.size();
				wordSubUpSecondSize[1][wordIndex] = Utility.countTotalDouble(subUpSecondFragFringeSet);
				wordCompleteSize[0][wordIndex] = completeStatesSet.size();
				wordCompleteSize[1][wordIndex] = Utility.countTotalDouble(completeStatesSet);
			}

			private void performScan() {
				ChartColumn nextChartColumn = chart.get(wordIndex+1);
				for(HashMap<ChartState,double[]> scanRootSet : scanStatesSet.values()) {
					for(Entry<ChartState,double[]> e : scanRootSet.entrySet()) {
						ChartState scanState = e.getKey();							
						ChartState newState = nextChartColumn.new ChartState(scanState);;
						newState.dotIndex++;
						nextChartColumn.addState(newState, e.getValue()[0]);
						if (isInterrupted()) return;
					}
				}
			}

			private void performSubDown() {
				subDownFirstStatesSet.keySet().retainAll(subDownSecondStateSet.keySet());
				subDownSecondStateSet.keySet().retainAll(subDownFirstStatesSet.keySet());
				for(HashMap<FragFringe,Double> subDownSecondSet : subDownSecondStateSet.values()) {
					for(Entry<FragFringe,Double> e : subDownSecondSet.entrySet()) {
						FragFringe subDownSecondFragFringe = e.getKey();
						double prob = e.getValue();
						ChartState cs = new ChartState(subDownSecondFragFringe, wordIndex);
						addProbStateOrMaxViterbi(scanStatesSet, subDownSecondFragFringe.root, cs, prob);
					}
					if (isInterrupted()) return;
				}				
			}

			private void performSubUp() {
				subUpFirstStatesSet.keySet().retainAll(subUpSecondFragFringeSet.keySet());
				subUpSecondFragFringeSet.keySet().retainAll(subUpFirstStatesSet.keySet());				
				
				for(Entry<TermLabel, HashMap<FragFringe,Double>> subUpSecondEntry : subUpSecondFragFringeSet.entrySet()) {					
					TermLabel root = subUpSecondEntry.getKey();
					HashMap<FragFringe,Double> subUpSecondTable = subUpSecondEntry.getValue();
					double mostProbSubUpFirst = Utility.getMaxValue(subUpFirstStatesSet.get(root));
					for(Entry<FragFringe,Double> e : subUpSecondTable.entrySet()) {
						FragFringe subUpSecondFragFringe = e.getKey();
						double prob = e.getValue()+mostProbSubUpFirst;
						ChartState cs = new ChartState(subUpSecondFragFringe, -1);
						cs.dotIndex++;
						addProbStateOrMaxViterbi(scanStatesSet, subUpSecondFragFringe.root, cs,prob);
					}											
					if (isInterrupted()) return;
				}		
			}
			
			private class CompletePriorityQueue {

				int pastColumns;
				Vector<LinkedHashMap<TermLabel, Duet<ChartState,double[]>>>
					completeStatesTable;
				int[] sizes;
				int totalSize;
				
				public CompletePriorityQueue(int wordIndex) {
					this.pastColumns = wordIndex;
					completeStatesTable = 
						new Vector<LinkedHashMap<TermLabel, Duet<ChartState,double[]>>>(wordIndex);
					sizes = new int[wordIndex];
					for(int i=0; i<wordIndex; i++) {
						completeStatesTable.add(
								new LinkedHashMap<TermLabel, Duet<ChartState,double[]>>());
					}
				}
				
				public boolean isEmpty() {
					return totalSize==0;
				}
				
				public boolean add(ChartState cs,  double prob) {
					int index = cs.subScriptIndex;
					TermLabel root = cs.root();
					LinkedHashMap<TermLabel, Duet<ChartState,double[]>> table =
						completeStatesTable.get(index);
					Duet<ChartState,double[]> present = table.get(root);
					if (present==null) {
						present = new Duet<ChartState,double[]>(cs, new double[]{prob});
						table.put(root, present);
						sizes[index]++;
						totalSize++;
						return true;
					}
					double presentProb = present.getSecond()[0];
					if (prob>presentProb) {
						table.remove(root);
						table.put(root, new Duet<ChartState,double[]>(cs, new double[]{prob}));
						//present.firstElement = cs;
						//present.secondElement[0] = prob;						
						return true;
					}
					return false;
				}

				public Duet<ChartState,double[]> poll() {
					for(int i=pastColumns-1; i>=0; i--) {
						if (sizes[i]==0)
							continue;
						HashMap<TermLabel, Duet<ChartState,double[]>> table =
							completeStatesTable.get(i);
						sizes[i]--;
						totalSize--;
						Iterator<Entry<TermLabel, Duet<ChartState, double[]>>> it = table.entrySet().iterator();
						Duet<ChartState, double[]> result = it.next().getValue();
						it.remove();
						return result;						
					}
					return null;
				}

				
			}
			

			private void performComplete() {
				
				if (completeStatesSet.isEmpty())
					return;
				
				CompletePriorityQueue completeStatesQueue = 
						new CompletePriorityQueue(wordIndex);
											
				for(HashMap<ChartState,double[]> completeStates : completeStatesSet.values()) {
					for(Entry<ChartState,double[]> e : completeStates.entrySet()) {
						completeStatesQueue.add(e.getKey(), e.getValue()[0]);
					}
				}
				
				//completeStatesSet.clear();

				HashMap<ChartState,double[]> newNonCompleteStates = 
						new HashMap<ChartState,double[]>();
				
				while(!completeStatesQueue.isEmpty()) {
					Duet<ChartState, double[]> duet = completeStatesQueue.poll();
					ChartState cs = duet.getFirst();					
					double csProb = duet.getSecond()[0];
					int index = cs.subScriptIndex;
					TermLabel root = cs.fragFringe.root;
					HashMap<ChartState, double[]> pastStateSubDownSet = chart.get(index).subDownFirstStatesSet.get(root);		
					for(Entry<ChartState, double[]> e : pastStateSubDownSet.entrySet()) {
						ChartState pastStateSubDown = e.getKey();
						double newCsProb = e.getValue()[0] + csProb;
						ChartState newCs = new ChartState(pastStateSubDown);					
						newCs.dotIndex++;
						if (newCs.isCompleteState()) {
							completeStatesQueue.add(newCs, newCsProb);
							addProbStateOrMaxViterbi(completeStatesSet, 
										newCs.root(), newCs, newCsProb);													
						}	
						else {
							addProbStateOrMaxViterbiSimple(newNonCompleteStates,newCs, newCsProb);
						}
					}
				}
				
				for(Entry<ChartState,double[]> e : newNonCompleteStates.entrySet()) {
					addState(e.getKey(),e.getValue()[0]);
				}
				
				
			}
			
			private void performCompleteOld() {					
				HashMap<ChartState,double[]> newCompleteStates
					= new HashMap<ChartState,double[]>();
				HashMap<ChartState,double[]> newNonCompleteStates = 
					new HashMap<ChartState,double[]>();
					
				
				for(Entry<TermLabel,HashMap<ChartState,double[]>> e : completeStatesSet.entrySet()) {
					for(Entry<ChartState,double[]> f : e.getValue().entrySet()) {
						ChartState cs = f.getKey();
						performCompleteRecursive(cs, f.getValue()[0], newCompleteStates, newNonCompleteStates);											
					}
					if (isInterrupted()) return;
				}
				
				

				for(Entry<ChartState,double[]> e : newCompleteStates.entrySet()) {
					ChartState newCs = e.getKey();
					TermLabel newCsRoot = newCs.root();
					addProbStateOrMaxViterbi(completeStatesSet, newCsRoot, newCs, e.getValue()[0]);
				}	
				for(Entry<ChartState,double[]> e : newNonCompleteStates.entrySet()) {
					addState(e.getKey(),e.getValue()[0]);
				}

			}
			
			private void performCompleteRecursive(ChartState cs, double csProb, 
					HashMap<ChartState, double[]> newCompleteStates,
					HashMap<ChartState, double[]> newNonCompleteStates) {
				
				int index = cs.subScriptIndex;
				TermLabel root = cs.fragFringe.root;
				HashMap<ChartState, double[]> pastStateSubDownSet = chart.get(index).subDownFirstStatesSet.get(root);		
				for(Entry<ChartState, double[]> e : pastStateSubDownSet.entrySet()) {
					ChartState pastStateSubDown = e.getKey();
					double prob = e.getValue()[0] + csProb;
					ChartState newCs = new ChartState(pastStateSubDown);					
					newCs.dotIndex++;	
					//TermLabel newCsRoot = newCs.root();					
					if (newCs.isCompleteState()) {						
						if (addProbStateOrMaxViterbiSimple(newCompleteStates, newCs, prob)) { //overlapping?
							performCompleteRecursive(newCs, prob, newCompleteStates, newNonCompleteStates); // which prob?
						}
					}	
					else {
						addProbStateOrMaxViterbiSimple(newNonCompleteStates,newCs, prob);
						//addState(newCs,prob);
					}
				}

			}
			
			private boolean containsComplete(ChartState cs) {
				TermLabel root = cs.root();
				HashMap<ChartState, double[]> csSet = completeStatesSet.get(root);
				if (csSet==null)
					return false;
				return csSet.containsKey(cs);
			}
			
			private HashMap<Integer, HashMap<ChartState, double[]>> getCompleteStatesSubScriptIndexed(
					TermLabel beforeDot) {
				HashMap<Integer, HashMap<ChartState, double[]>> result = 
					new HashMap<Integer, HashMap<ChartState, double[]>>();
				HashMap<ChartState, double[]> completeStates = completeStatesSet.get(beforeDot);
				for(Entry<ChartState, double[]> e : completeStates.entrySet()) {
					ChartState cs = e.getKey();
					Integer index = cs.subScriptIndex;
					Utility.putInMapDouble(result, index, cs, e.getValue());
				}
				return result;
			}
			
			public void terminate() {
				finished = true;
				if (!subUpFirstStatesSet.isEmpty()) {
					parsingSuccess = true;
					if (subUpFirstStatesSet.keySet().contains(TOPnode)) 
						reachedTop = true;
				}								
			}
			
			public boolean addState(ChartState cs, double prob) {				
				if (!cs.hasElementAfterDot()) {
					TermLabel root = cs.root();
					if (cs.isStarred)
						return addProbStateOrMaxViterbi(subUpFirstStatesSet, root, cs, prob);
					else
						return addProbStateOrMaxViterbi(completeStatesSet, root, cs, prob);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {
					if (nextTerm==word) {
						return addProbStateOrMaxViterbi(scanStatesSet, cs.root(), cs, prob);
					}
					return false;
				}
				return addProbStateOrMaxViterbi(subDownFirstStatesSet, nextTerm, cs, prob);
			}
			
			class ChartState {
				FragFringe fragFringe;
				int dotIndex;
				int subScriptIndex;
				int length;
				boolean isStarred;
				
				public ChartState(FragFringe fragFringe, int subScriptIndex) {
					this.fragFringe = fragFringe;
					this.dotIndex = 0;
					this.subScriptIndex = subScriptIndex;
					this.isStarred = subScriptIndex==-1;
					this.length = fragFringe.yield.length;
				}
				
				public ChartState(ChartState cs) {
					this.fragFringe = cs.fragFringe;
					this.dotIndex = cs.dotIndex;
					this.subScriptIndex = cs.subScriptIndex;
					this.isStarred = cs.isStarred;
					this.length = cs.length;
				}
				
				public int wordIndex() {
					return wordIndex;
				}
				
				public int typeOfChartState() {
					if (hasElementAfterDot()) {
						if (peekAfterDot().isLexical)
							return 0; //SCAN
						return 1; //SUB-DOWN FIRST
					}
					if (isStarred) 
						return 2; //SUB-DOWN SECOND
					return 3; //COMPLETE
				}
				
				public boolean isScanState() {
					return hasElementAfterDot() && peekAfterDot().isLexical;
				}
				
				public Vector<Duet<ChartState, HashMap<ChartState, double[]>>> getParentStates() {

					if (dotIndex==0) {
						return null;
					}
					
					Vector<Duet<ChartState, HashMap<ChartState, double[]>>> result =
						new Vector<Duet<ChartState, HashMap<ChartState, double[]>>>();
					
					if (isScanState()) { //SCAN					
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.dotIndex--;
						TermLabel beforeDot = peekBeforeDot();
						if (beforeDot.isLexical) {							
							result.add(new Duet<ChartState, HashMap<ChartState, double[]>>(
									previousState, null));
							return result;
						}
						result.add(new Duet<ChartState, HashMap<ChartState, double[]>>(
								previousState.dotIndex==0 ? null : previousState,
								subUpFirstStatesSet.get(beforeDot)));
						return result;
					}
					 					
					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.dotIndex--;						
						result.add(new Duet<ChartState, HashMap<ChartState, double[]>>(
								previousState, null));
						return result;
					}					
					HashMap<Integer,HashMap<ChartState, double[]>> completeStatesSubScriptIndexed =
						getCompleteStatesSubScriptIndexed(beforeDot);					
					for(Entry<Integer,HashMap<ChartState, double[]>> e : completeStatesSubScriptIndexed.entrySet()) {
						int previousStateColumnIndex = e.getKey();
						HashMap<ChartState, double[]> completeStates = e.getValue();						
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);						
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.dotIndex--;
						result.add(new Duet<ChartState, HashMap<ChartState, double[]>>(
								previousState, completeStates));
					}		
					return result;																		
				}
				
				
				public Duet<ChartState, ChartState> getViterbiBestParentStates() {
					
					if (dotIndex==0) {
						return null;
						// if current state is SCAN STATE the previous was
						// a SUB-DOWN FIRST, but must be handled by completion.
					}
					
					if (isScanState()) { //CURREN IS SCAN STATE
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.dotIndex--;
						TermLabel beforeDot = peekBeforeDot();
						if (beforeDot.isLexical) {				
							//PREVIOUS WAS SCAN STATE
							return new Duet<ChartState, ChartState>(
									previousState, null);
						}
						//PREVIOUS WAS SUB-UP-FIRST STATE
						if (dotIndex==1) {						
							return new Duet<ChartState, ChartState>(
									null, 
									Utility.getMaxKey(subUpFirstStatesSet.get(beforeDot)));
						}
						//PREVIOUS WAS COMPLETE STATE
						//(equal as the last case below)
					}
					 					
					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.dotIndex--;						
						return new Duet<ChartState, ChartState>(
								previousState, null);						
					}					
					
					double max= -Double.MAX_VALUE;
					ChartState bestCompleteState = null;
					ChartState bestPreviousState = null;
					
					for(Entry<ChartState, double[]> e : completeStatesSet.get(beforeDot).entrySet()) {
						ChartState completeState = e.getKey();
						int previousStateColumnIndex = completeState.subScriptIndex;
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.dotIndex--;
						HashMap<ChartState,double[]> previousSubDownFirstStateTable = 
								previousStateColumn.subDownFirstStatesSet.get(beforeDot); 
						if (previousSubDownFirstStateTable!=null) { 
							double[] previousProb = previousSubDownFirstStateTable.get(previousState);
							if (previousProb!=null) {
								double prob = e.getValue()[0]+previousProb[0];
								if (prob>max) {
									max = prob;
									bestCompleteState = e.getKey();
									bestPreviousState = previousState;
								}	
							}											
						}
					}
					
					return new Duet<ChartState, ChartState>(
							bestPreviousState, bestCompleteState);
					
				}
				

				public TermLabel root() {
					return fragFringe.root;
				}
				
				public String toString() {
					StringBuilder sb = new StringBuilder();
					sb.append(wordIndex + ": ");
					sb.append(isStarred ? "{*} " : "{" + subScriptIndex + "} ");
					sb.append(fragFringe.root + " < ");
					int i=0;
					for(TermLabel t : fragFringe.yield) {
						if (dotIndex==i++)
							sb.append(". ");
						sb.append(t + " ");
					}
					if (dotIndex==i)
						sb.append(". ");
					return sb.toString();
				}
				
				public String toStringTex() {
					StringBuilder sb = new StringBuilder();
					//sb.append("$" + wordIndex + ":\\;\\;$ ");
					sb.append(isStarred ? "$(*)$ & " : "$(" + subScriptIndex + ")$ & ");
					sb.append("$" + fragFringe.root.toStringTex() + "$ & $\\yields$ & ");
					int i=0;
					sb.append("$ ");
					for(TermLabel t : fragFringe.yield) {
						if (dotIndex==i++)
							sb.append(" \\bullet " + " \\;\\; ");
						sb.append(t.toStringTex() + " \\;\\; ");
					}
					if (dotIndex==i)
						sb.append(" \\bullet ");
					sb.append("$");
					return sb.toString();
				}
				
				public boolean hasElementAfterDot() {
					return dotIndex<length;
				}
				
				public boolean hasElementBeforeDot() {					
					return dotIndex>0;
				}
				
				public boolean isFirstStateWithCurrentWord() {					
					return dotIndex==0 || (dotIndex==1 && isScanState() && !peekBeforeDot().isLexical);
				}

				public TermLabel peekBeforeDot() {
					return fragFringe.yield[dotIndex-1];
					
				}

				public TermLabel peekAfterDot() {
					return fragFringe.yield[dotIndex];
				}
				
				public boolean isCompleteState() {
					return !hasElementAfterDot() && !isStarred;					
				}
				
				@Override
				public int hashCode() {
					return fragFringe.hashCode();
				}
				
				
				@Override
				public boolean equals(Object o) {
					if (this==o)
						return true;
					if (o instanceof ChartState) {
						ChartState oCS = (ChartState)o;
						return 	oCS.wordIndex() == this.wordIndex() &&
								oCS.fragFringe==this.fragFringe &&
								oCS.subScriptIndex == this.subScriptIndex &&
								oCS.dotIndex==this.dotIndex;
					}
					return false;
				}
				
				/*
				@Override
				public int compareTo(ChartState o) {
					int thisVal = this.subScriptIndex;
					int anotherVal = o.subScriptIndex;
					return (anotherVal<thisVal ? -1 : (thisVal==anotherVal ? 0 : 1));
				}
				*/
				
			}
			
			/*
			private Comparator<ChartState> ChartStateComparator = new Comparator<ChartState>() {
				@Override
				public int compare(ChartState o1, ChartState o2) {
					int thisVal = o1.subScriptIndex;
					int anotherVal = o2.subScriptIndex;
					return (anotherVal<thisVal ? -1 : (thisVal==anotherVal ? 0 : 1));
				}				
			};
			*/
		}
		
	}
	
	public static void mainToy(String[] args) throws Exception {
		
		debug = true;
		treeLenghtLimit = 20;
		maxSecPerWord = 5*60;		
		threadCheckingTime = 1000;	
		threads = 15;
		limitTestToSentenceIndex = 0;
		
		String homePath = System.getProperty("user.home");
		String workingPath = homePath + "/PLTSG/ToyCorpus/";	
		File trainTB = new File(workingPath + "trainTB.mrg");
		File testTB = new File(workingPath + "testTB.mrg");
		File fragFile = new File(workingPath + "fragments.mrg");
		
		FragmentExtractor FE = FragmentExtractor.getDefaultFragmentExtractor(
				workingPath, "NOSMOOTHING", "NOSMOOTHING", trainTB, testTB, fragFile);
		
		FragmentExtractor.printFilteredFragmentsToFile = true;
		
		new IncrementalTSGEarlyParserViterbiSD(FE, testTB, workingPath).run();
	}
	
	

	public static void main(String[] args) throws Exception {
				
		EvalC.DELETE_LABELS = new String[]{};
		
		debug = true;
		treeLenghtLimit = -1;
		maxSecPerWord = 10*60;		
		threadCheckingTime = 1000;	
		threads = 15;
		limitTestToSentenceIndex = -1;		
		removeFringesNonCompatibleWithSentence = true;
		
		String homePath = System.getProperty("user.home");
		String workingPath = homePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";
		File trainTB = new File(workingPath + "wsj-02-21.mrg");
		File testTB = new File(workingPath + "wsj-22.mrg");	
		File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		//File fragFile = new File(workingPath + "fragments_small.txt");
		String fragExtrWorkingPath  = workingPath + "FragmentExtractor/";
		String outputPathParsing = workingPath + "Parsing/";
		
		

		String[][] settings = new String[][]{
				//	minSet			frags	
				//only minset			
				//new String[]{	"1",			"No"},
				//new String[]{	"5",			"No"},					
				//new String[]{	"10",			"No"},
				//new String[]{	"100",			"No"},
				//new String[]{	"1000",			"No"},
				//new String[]{	"NoSmoothing",	"No"},
				
				//only frags
				//new String[]{	"No",			"100"},
				//new String[]{	"No",			"1000"},
				new String[]{	"No",			"NoSmoothing"},
				
				//combination
				//new String[]{	"100",			"100"},
				//new String[]{	"1000",			"1000"},				
				//new String[]{	"10",			"NoSmoothing"},
				//new String[]{	"100",			"NoSmoothing"},
				new String[]{	"1000",			"NoSmoothing"},
				new String[]{	"NoSmoothing",	"NoSmoothing"},
		};
		
		for(String[] set : settings) {
			FragmentExtractor FE = FragmentExtractor.getDefaultFragmentExtractor(
					fragExtrWorkingPath, set[0], set[1], trainTB, testTB, fragFile);
			new IncrementalTSGEarlyParserViterbiSD(FE, testTB, outputPathParsing).run();
			System.gc();
		}	
		
	}	
	
}
