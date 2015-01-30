package tsg.incremental.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import tsg.incremental.old.IncrementalTSGEarlyParser.ChartParserThread.ChartColumn.ChartState;
import tsg.parseEval.EvalC;
import util.Duet;
import util.Pair;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class IncrementalTSGEarlyParser extends Thread {
	
	static final Label TOPlabel = Label.getLabel("TOP");
	static final TermLabel TOPnode = TermLabel.getTermLabel("TOP", false);
	static final TermLabel emptyLexNode = TermLabel.getTermLabel("", true);
	
	static boolean debug = true;
	static int treeLenghtLimit = -1;	
	static long maxSecPerWord = 5*60;	
	static long threadCheckingTime = 10000;	
	static int threads = 1;
	static int limitTestToSentenceIndex = -1;
	
	File testTBfile, logFile, parsedFile, goldTBFile, evalFile;	
	FragmentExtractor FE;
	String outputPath;
	ArrayList<TSNodeLabel> testTB;
	TSNodeLabel[] parsedTB;		
	PrintProgress progress;	
	int parsedTrees, reachedTopTrees, interruptedTrees, longestWordTime;
	Iterator<TSNodeLabel> testTBiterator;
	int treebankIteratorIndex;
	boolean smoothing;
	EvalC evalC;	
	
	
	HashMap<TermLabel,  //indexed on lex
		HashMap<TermLabel, HashSet<FragFringe>>>
		firstLexFringes, // and root  
		firstSubFringes; // and firstTerm
	
	HashMap<TermLabel,  //indexed on pos
		HashMap<TermLabel, HashSet<FragFringe>>>
		firstLexFringesSmoothing, // and root
		firstSubFringesSmoothing; // and first Term
	
	HashMap<TermLabel, HashSet<TermLabel>> posLexTable, lexPosTable;
	
	HashMap<FragFringe,HashSet<TSNodeLabel>> lexFringeFragMap;
	HashMap<FragFringe,HashSet<TSNodeLabel>> templateFringeFragMap;
	

	
	public IncrementalTSGEarlyParser (FragmentExtractor FE,
			File testTBfile, String outputPath) throws Exception {
		this.FE = FE;
		this.testTBfile = testTBfile;
		this.testTB = Wsj.getTreebank(testTBfile);
		this.outputPath = outputPath;
		this.smoothing = FragmentExtractor.smoothingFromFrags || FragmentExtractor.smoothingFromMinSet;				
		
		String logFilename = outputPath + "log_earlyParser_";
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
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("INCREMENTAL TSG EARLY PARSER");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("");
		Parameters.reportLineFlush("Test Tree Length Limit: " + treeLenghtLimit);
		Parameters.reportLineFlush("Max sec Per Tree: " + maxSecPerWord);
		Parameters.reportLineFlush("Number of threads: " + threads);
	}
	
	private void printFinalSummary() {
		Parameters.reportLineFlush("Number of successfully parsed trees: " + parsedTrees);
		Parameters.reportLineFlush("of which reached TOP: " + reachedTopTrees);
		Parameters.reportLineFlush("Number of interrupted trees: " + interruptedTrees);
		Parameters.reportLineFlush("Longest time to parse a word (sec): " + longestWordTime);
		if (evalC!=null)
			Parameters.reportLineFlush("Final evalC scores: " + Arrays.toString(evalC.getRecallPrecisionFscore()));
	}
	
	private void convertPosSet() {
		posLexTable = convertTable(FE.posLexFinal, false, true);
		lexPosTable = convertTable(FE.lexPosFinal, true, false);
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
		
		firstLexFringes = new HashMap<TermLabel, HashMap<TermLabel, HashSet<FragFringe>>>();
		firstSubFringes = new HashMap<TermLabel, HashMap<TermLabel, HashSet<FragFringe>>>();
		
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
				if (Utility.putInHashMapHashSet(firstLexFringes, firstLexLabel, ff.root(), ff))
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
				if (Utility.putInHashMapHashSet(firstSubFringes, lexLabel, ff.firstTerminal(), ff))
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
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void convertSmoothedFragmentsToFringes() {
		
		Parameters.reportLineFlush("Converting smoothed fragments into fringes...");
		
		firstLexFringesSmoothing = new HashMap<TermLabel, HashMap<TermLabel, HashSet<FragFringe>>>();
		firstSubFringesSmoothing = new HashMap<TermLabel, HashMap<TermLabel, HashSet<FragFringe>>>();
		
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
					if (Utility.putInHashMapHashSet(firstLexFringesSmoothing, firstPosLabel, ff.root(), ff))
						countFirstLexSubFringes[0]++;					
					
				}
				else {
					countFirstLexSubFrags[1]++;
					if (Utility.putInHashMapHashSet(firstSubFringesSmoothing, firstPosLabel, ff.firstTerminal(), ff))
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
					if (wordTimer==null || !wordTimer.isStarted()) continue;
					int runningTimeSec = wordTimer.checkEllapsedTimeSec();					
					if (runningTimeSec > maxSecPerWord)
						t.interrupt();
					if (runningTimeSec > longestWordTime)
						longestWordTime = runningTimeSec;
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
	
	private void evaluate() {
		if (limitTestToSentenceIndex>=0)
			return;
		PrintWriter pw = FileUtil.getPrintWriter(parsedFile);
		for(TSNodeLabel t : parsedTB) {
			pw.println(t);
		}		
		pw.close();
		pw = FileUtil.getPrintWriter(goldTBFile);
		for(TSNodeLabel t : testTB) {
			pw.println(t);
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
		LinkedList<TSNodeLabel> subSites = new LinkedList<TSNodeLabel>();  
		result.addSubSites(subSites,true);
		while(derivationIter.hasNext()) {
			TSNodeLabel nextFrag = derivationIter.next().clone();
			if (subSites.isEmpty()) {
				//SUB-UP
				nextFrag.addSubSites(subSites,false);
				TSNodeLabel subSite = subSites.removeFirst();
				subSite.assignDaughters(result.daughters);
				result = nextFrag;
			}
			else {
				//SUB-DOWN
				TSNodeLabel subSite = subSites.removeFirst();
				subSite.assignDaughters(nextFrag.daughters);
				nextFrag.addSubSites(subSites,true);
			}
		}
		return result;
	}
	
	
	class FragFringe {
		private TermLabel root;
		private TermLabel[] yield;
		
		public FragFringe(TermLabel root, TermLabel[] yield) {
			this.root = root;
			this.yield = yield;
		}
		
		public boolean firstTerminalIsLexical() {
			return yield[0].isLexical;
		}

		public FragFringe(TSNodeLabel frag) {
			root = TermLabel.getTermLabel(frag);
			ArrayList<TSNodeLabel> terms = frag.collectTerminalItems();
			int length = terms.size();
			yield = new TermLabel[length];
			int i=0;
			for(TSNodeLabel term : frag.collectTerminalItems()) {
				yield[i++] = TermLabel.getTermLabel(term);
			}
		}
		
		public FragFringe cloneFringe() {
			return new FragFringe(this.root, Arrays.copyOf(this.yield, this.yield.length));
		}
		
		public TermLabel root() {
			return root;
		}
		
		public TermLabel secondTerminal() {
			return yield[1]; 
		}

		public TermLabel firstTerminal() {
			return yield[0];
		}

		@Override
		public int hashCode() {
			return 31 * Arrays.hashCode(yield) + root.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (this==o)
				return true;
			if (o instanceof FragFringe) {
				FragFringe oFF = (FragFringe)o;
				return oFF.root==this.root &&				
					Arrays.equals(oFF.yield, this.yield);
			}
			return false;
		}		
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(root);
			sb.append(" <");
			for(TermLabel s : yield) {
				sb.append(" " + s);
			}
			return sb.toString();
		}

		public String toStringTex() {
			StringBuilder sb = new StringBuilder();
			sb.append("$" + root.toStringTex() + "$ & $\\yields$ & ");
			sb.append("$ ");
			for(TermLabel t : yield) {
				sb.append(t.toStringTex().replaceFirst("\"", "``") + " \\;\\; ");
			}
			sb.append("$ ");
			return sb.toString();
		}

		public void setInYield(int i, TermLabel w) {
			yield[i] = w;
			
		}
		
		
	}

	protected class ChartParserThread extends Thread {
		
		int sentenceIndex;	
		TSNodeLabel goldTestTree;
				 	
		ArrayList<Label> wordLabels;
		int sentenceLength;
		Vector<HashMap<TermLabel,HashSet<FragFringe>>> wordFragFringeFirstLex, wordFragFringeFirstSub;
		Vector<ChartColumn> chart;
		int[][] fringeCounterFirstLexSubBin;
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
			wordTimes = new String[sentenceLength+1];
			wordScanSize = new int[2][sentenceLength+1];
			wordSubDownFirstSize = new int[2][sentenceLength+1];
			wordSubDownSecondSize = new int[2][sentenceLength+1];
			wordSubUpFirstSize = new int[2][sentenceLength+1];
			wordSubUpSecondSize = new int[2][sentenceLength+1];
			wordCompleteSize = new int[2][sentenceLength+1];
			Arrays.fill(wordTimes, "-");
			this.wordFragFringeFirstLex = //indexed on root
					new Vector<HashMap<TermLabel,HashSet<FragFringe>>>(sentenceLength);
			this.wordFragFringeFirstSub = //indexed on first term
					new Vector<HashMap<TermLabel,HashSet<FragFringe>>>(sentenceLength);
			chart = new Vector<ChartColumn> (sentenceLength+1); //termstate
			fringeCounterFirstLexSubBin = new int[2][sentenceLength];
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
			
			if (parsingSuccess) {
				derivationPath = retrieveOneDerivationPath();
				derivation = retrieveOneFragDerivation(derivationPath);
				parsedTree = combineDerivation(derivation);
				if (!reachedTop) {
					TSNodeLabel topNode = new TSNodeLabel(TOPlabel,false);
					topNode.assignUniqueDaughter(parsedTree);
					parsedTree = topNode;
				}
				else {
					parsedTree = TSNodeLabel.defaultWSJparse(goldTestTree.toFlatWordArray(), "TOP");
				}
			
				if (debug)
					printDebug(parsedTree, derivation, derivationPath);
				
				synchronized(progress) {
					if(Thread.interrupted() && !finished)
						interruptedTrees++;
					if (parsingSuccess) parsedTrees++;
					if (reachedTop) reachedTopTrees++;			
					parsedTB[sentenceIndex] = parsedTree;
					progress.next();					
				}
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

			HashMap<TermLabel,HashSet<FragFringe>> wordFFlex = 
					Utility.deepHashMapClone(firstLexFringes.get(w));
			HashMap<TermLabel,HashSet<FragFringe>> wordFFsub = 
					Utility.deepHashMapClone(firstSubFringes.get(w));			
			 
			if (smoothing) {
				for(TermLabel pos : lexPosTable.get(w)) {
					//first lex
					HashMap<TermLabel, HashSet<FragFringe>> posFringes = firstLexFringesSmoothing.get(pos);
					if (posFringes!=null) {									
						for(Entry<TermLabel, HashSet<FragFringe>> e : posFringes.entrySet()) {
							TermLabel root = e.getKey();																			
							HashSet<FragFringe> presentSet = wordFFlex.get(root);							
							if (presentSet==null) {
								presentSet = new HashSet<FragFringe>();
								wordFFlex.put(root, presentSet);
							}
							for(FragFringe ff : e.getValue()) {
								FragFringe ffCopy = ff.cloneFringe();
								ffCopy.setInYield(0, w);
								presentSet.add(ffCopy);
							}
						}						
					}
					//firstSub
					posFringes = firstSubFringesSmoothing.get(pos);
					if (posFringes!=null) {
						for(Entry<TermLabel, HashSet<FragFringe>> e : posFringes.entrySet()) {		
							TermLabel subSite = e.getKey();
							HashSet<FragFringe> presentSet = wordFFsub.get(subSite);							
							if (presentSet==null) {
								presentSet = new HashSet<FragFringe>();
								wordFFsub.put(subSite, presentSet);
							}
							for(FragFringe ff : e.getValue()) {
								FragFringe ffCopy = ff.cloneFringe();
								ffCopy.setInYield(1, w);
								presentSet.add(ffCopy);
							}
						}
					}					
				}
			}
			
			wordFragFringeFirstLex.add(wordFFlex);		
			wordFragFringeFirstSub.add(wordFFsub);
			fringeCounterFirstLexSubBin[0][index] = wordFFlex.size();
			fringeCounterFirstLexSubBin[1][index] = wordFFsub.size();

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
						toStringTex(chart.get(i).scanStatesSet, derivationPath, onlyDerivation) + 
						"}");
				pw.print(" & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SUB-DOWN} &\n");
			for(i=0; i<sentenceLength; i++) {
				ChartColumn cc = chart.get(i);
				pw.print(toStringTex(cc.subDownFirstStatesSet, derivationPath, onlyDerivation));
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
					pw.print(toStringTex(chart.get(i).subUpFirstStatesSet, derivationPath, onlyDerivation));
					pw.print(" & \n");
					pw.print(toStringFragTex(cc.subUpSecondFragFringeSet, derivationPath, i, false, onlyDerivation));
					pw.print(" & \n");
				}
				else {
					pw.print("\\multicolumn{1}{l|}{ " +
							toStringTex(chart.get(i).subUpFirstStatesSet, derivationPath, onlyDerivation) + 
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
						toStringTex(chart.get(i).completeStatesSet, derivationPath, onlyDerivation) + 
						"}");
				pw.print(i==sentenceLength ? "\n" : " & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\end{tabular}\n");
		}
				 
		static final String highlight = "\\rowcolor{lemon}";
		
		public String toStringTex(HashMap<TermLabel,HashSet<ChartColumn.ChartState>> table,
				HashSet<ChartState> derivationPath, boolean onlyDerivation) {			
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{llll}\n");							
			for(HashSet<ChartColumn.ChartState> csSet : table.values()) {
				for(ChartColumn.ChartState cs : csSet) {
					boolean isInDerivation = derivationPath!=null && derivationPath.contains(cs);					
					if (isInDerivation) {
						String initString = isInDerivation && !onlyDerivation ?
								"\t" + highlight : "\t";
						sb.append(initString + cs.toStringTex() + "\\\\\n");
					}
					else if (!onlyDerivation) {
						sb.append("\t" + cs.toStringTex() + "\\\\\n");
					}					
				}
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}
		
		public String toStringFragTex(HashMap<TermLabel,HashSet<FragFringe>> table,
				HashSet<ChartState> derivationPath, int wordIndex, boolean subDown,
				boolean onlyDerivation) {
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{lll}\n");							
			for(HashSet<FragFringe> set : table.values()) {
				for(FragFringe ff : set) {
					ChartState cs = chart.get(wordIndex).new ChartState(ff, subDown ? wordIndex : -1);
					if (!subDown)
						cs.dotIndex++;
					boolean isInDerivation = derivationPath!=null && derivationPath.contains(cs);					
					if (isInDerivation) {
						String initString = isInDerivation && !onlyDerivation ?
								"\t" + highlight : "\t";
						sb.append(initString + ff.toStringTex() + "\\\\\n");						
					}
					else if (!onlyDerivation) {
						sb.append("\t" + ff.toStringTex() + "\\\\\n");
					}
						
				}
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
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

		
		private void printDebug(TSNodeLabel parsedTree, 
				ArrayList<TSNodeLabel> derivation, HashSet<ChartState> derivationPath) {
			synchronized(logFile) {				
				Parameters.logLine("Sentence #                                  " + sentenceIndex);
				Parameters.logLine("Sentence Length:                            " + sentenceLength);			
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
					Parameters.logLine("Derivation: " + derivation);
					Parameters.logLine("Parsed tree: " + parsedTree.toStringQtree());
					Parameters.logLine("Gold tree: " + goldTestTree.toStringQtree());
					Parameters.logLine("Scores: " + Arrays.toString(EvalC.getRecallPrecisionFscore(parsedTree, goldTestTree)));
				}
				
				if (limitTestToSentenceIndex!=-1) {				
					File tableFile = new File(outputPath + "chart.txt");
					PrintWriter pw = FileUtil.getPrintWriter(tableFile);
					pw.println(toStringTex(derivation));
					pw.println("\n\n");
					toStringTex(pw,derivationPath,true);
					pw.close();
				}
				
				Parameters.logLineFlush("");
								
			}
		}
				
		private ArrayList<TSNodeLabel> retrieveOneFragDerivation(HashSet<ChartState> path) {									
			FragFringe[] derivationFringessPath = getFragsFromPath(path);			
			ArrayList<TSNodeLabel> derivationFrags = new ArrayList<TSNodeLabel>();
			for(FragFringe ff : derivationFringessPath) {
				if (ff==null) 
					continue;
				derivationFrags.add(getOriginalFrags(ff).iterator().next());
			}
			return derivationFrags;
		}
		
		private HashSet<ChartState> retrieveOneDerivationPath() {
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashSet<ChartState> topStateSet = lastColumn.subUpFirstStatesSet.get(TOPnode);
			if (topStateSet==null) {
				HashMap<TermLabel, HashSet<ChartState>> endStates = lastColumn.subUpFirstStatesSet;
				TermLabel randomRoot = Utility.getRandomElement(endStates.keySet());
				topStateSet = endStates.get(randomRoot);
			}			
			ChartState topState = Utility.getRandomElement(topStateSet);
			HashSet<ChartState> path = new HashSet<ChartState>();
			addOneDerivationPathRecursive(path,topState);
			return path;
		}

		private void addOneDerivationPathRecursive(HashSet<ChartState> path, ChartState cs) {
			
			path.add(cs);
			Vector<Duet<ChartState, HashSet<ChartState>>> 
			parentPairs = cs.getParentStates();			
			if (parentPairs==null)
				return;
			Collections.shuffle(parentPairs);
			Duet<ChartState, HashSet<ChartState>> firstParentPair = parentPairs.get(0);
			cs = firstParentPair.getFirst();
			if (cs!=null) {
				addOneDerivationPathRecursive(path,cs);
			}
			HashSet<ChartState> parents = firstParentPair.getSecond();
			if (parents!=null) {
				cs = Utility.getRandomElement(parents);
				addOneDerivationPathRecursive(path,cs);
			}
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

		class ChartColumn {
			
			TermLabel word;
			int wordIndex;
			
			HashMap<TermLabel,HashSet<ChartState>> 
				scanStatesSet, //indexed on root
				subDownFirstStatesSet, //indexed on non-term after dot				
				subUpFirstStatesSet, //indexed on root				
				completeStatesSet; //indexed on root
			HashMap<TermLabel,HashSet<FragFringe>>
			subDownSecondStateSet, //indexed on root
			subUpSecondFragFringeSet; //indexed on first term (sub-site)				
			
			public ChartColumn (TermLabel word, int i) {
				this.word = word;
				this.wordIndex = i;
				initVariabels();
			}
			
			private void initVariabels() {
				scanStatesSet = new HashMap<TermLabel,HashSet<ChartState>>();
				subDownFirstStatesSet = new HashMap<TermLabel,HashSet<ChartState>>();
				subUpFirstStatesSet = new HashMap<TermLabel,HashSet<ChartState>>();
				completeStatesSet = new HashMap<TermLabel,HashSet<ChartState>>();
				subDownSecondStateSet = (wordIndex!=0 && wordIndex!=sentenceLength) ?
					wordFragFringeFirstLex.get(wordIndex) :
					new HashMap<TermLabel,HashSet<FragFringe>>();	
				subUpSecondFragFringeSet = (wordIndex!=0 && wordIndex!=sentenceLength) ?
					wordFragFringeFirstSub.get(wordIndex) :
					new HashMap<TermLabel,HashSet<FragFringe>>();				
			}

			private void performStart() {
				for(Entry<TermLabel,HashSet<FragFringe>> e : wordFragFringeFirstLex.get(wordIndex).entrySet()) {
					for(FragFringe lexFragFringe : e.getValue()) {
						ChartState cs = new ChartState(lexFragFringe, -1);
						Utility.putInHashMap(scanStatesSet, lexFragFringe.root, cs);
					}
				}				
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
				sb.append("\tSub-Down States: " + Utility.countTotal(subDownFirstStatesSet) + "\n");
				sb.append("\tSub-Up States: " + Utility.countTotal(subUpFirstStatesSet) + "\n");
				sb.append("\tComplete States: " + Utility.countTotal(completeStatesSet));
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
				wordScanSize[1][wordIndex] = Utility.countTotal(scanStatesSet);
				wordSubDownFirstSize[0][wordIndex] = subDownFirstStatesSet.size();
				wordSubDownFirstSize[1][wordIndex] = Utility.countTotal(subDownFirstStatesSet);
				wordSubDownSecondSize[0][wordIndex] = subDownSecondStateSet.size();
				wordSubDownSecondSize[1][wordIndex] = Utility.countTotal(subDownSecondStateSet);
				wordSubUpFirstSize[0][wordIndex] = subUpFirstStatesSet.size();
				wordSubUpFirstSize[1][wordIndex] = Utility.countTotal(subUpFirstStatesSet);
				wordSubUpSecondSize[0][wordIndex] = subUpSecondFragFringeSet.size();
				wordSubUpSecondSize[1][wordIndex] = Utility.countTotal(subUpSecondFragFringeSet);
				wordCompleteSize[0][wordIndex] = completeStatesSet.size();
				wordCompleteSize[1][wordIndex] = Utility.countTotal(completeStatesSet);
			}

			private void performScan() {
				ChartColumn nextChartColumn = chart.get(wordIndex+1);
				for(HashSet<ChartState> scanRootSet : scanStatesSet.values()) {
					for(ChartState scanState : scanRootSet) {							
						ChartState newState = nextChartColumn.new ChartState(scanState);;
						newState.dotIndex++;
						nextChartColumn.addState(newState);
						if (isInterrupted()) return;
					}
				}
			}

			private void performSubDown() {
				subDownFirstStatesSet.keySet().retainAll(subDownSecondStateSet.keySet());
				subDownSecondStateSet.keySet().retainAll(subDownFirstStatesSet.keySet());

				for(HashSet<FragFringe> subDownSecondSet : subDownSecondStateSet.values()) {						 
					for(FragFringe subDownSecondFragFringe : subDownSecondSet) {
						ChartState cs = new ChartState(subDownSecondFragFringe, wordIndex);
						Utility.putInHashMap(scanStatesSet, subDownSecondFragFringe.root, cs);
					}
					if (isInterrupted()) return;
				}				
			}

			private void performSubUp() {
				subUpFirstStatesSet.keySet().retainAll(subUpSecondFragFringeSet.keySet());
				subUpSecondFragFringeSet.keySet().retainAll(subUpFirstStatesSet.keySet());				
				for(HashSet<FragFringe> subUpSecondSet : subUpSecondFragFringeSet.values()) {						
					for(FragFringe subUpSecondFragFringe : subUpSecondSet) {
						ChartState cs = new ChartState(subUpSecondFragFringe, -1);
						cs.dotIndex++;
						Utility.putInHashMap(scanStatesSet, subUpSecondFragFringe.root, cs);
					}						
					if (isInterrupted()) return;
				}
		
			}

			private void performCompleteRedundant() {					
				HashSet<ChartState> newCompleteStates;
				do {
					newCompleteStates = new HashSet<ChartState>();
					for(Entry<TermLabel,HashSet<ChartState>> e : completeStatesSet.entrySet()) {
						for(ChartState cs : e.getValue()) {
							int index = cs.subScriptIndex;
							TermLabel root = cs.fragFringe.root();
							HashSet<ChartState> pastStateSubDownSet = chart.get(index).subDownFirstStatesSet.get(root);
							//if (pastStateSubDownSet!=null) {
								for(ChartState pastStateSubDown : pastStateSubDownSet) {
									ChartState newCs = new ChartState(pastStateSubDown);
									newCs.dotIndex++;
									if (newCs.isCompleteState()) {									
										if (!containsComplete(newCs)) {
											newCompleteStates.add(newCs);
											//performCompleteRecursive(cs, newCompleteStates);
										}
									}	
									else {
										addState(newCs);
									}
								}
							//}															
						}
						if (isInterrupted()) return;
					}
					for(ChartState newCs : newCompleteStates) {
						TermLabel newCsRoot = newCs.fragFringe.root();
						Utility.putInHashMap(completeStatesSet, newCsRoot, newCs);
					}
				} while(!newCompleteStates.isEmpty());								
			}
			
			private void performComplete() {					
				HashSet<ChartState> newCompleteStates;
				newCompleteStates = new HashSet<ChartState>();
				for(Entry<TermLabel,HashSet<ChartState>> e : completeStatesSet.entrySet()) {
					for(ChartState cs : e.getValue()) {
						performCompleteRecursive(cs,newCompleteStates);											
					}
					if (isInterrupted()) return;
				}
				for(ChartState newCs : newCompleteStates) {
					TermLabel newCsRoot = newCs.fragFringe.root;
					Utility.putInHashMap(completeStatesSet, newCsRoot, newCs);
				}								
			}
			
			private void performCompleteRecursive(ChartState cs, HashSet<ChartState> newCompleteStates) {
				int index = cs.subScriptIndex;
				TermLabel root = cs.fragFringe.root;
				HashSet<ChartState> pastStateSubDownSet = chart.get(index).subDownFirstStatesSet.get(root);		
				for(ChartState pastStateSubDown : pastStateSubDownSet) {
					ChartState newCs = new ChartState(pastStateSubDown);
					newCs.dotIndex++;
					if (newCs.isCompleteState()) {									
						if (!containsComplete(newCs) && newCompleteStates.add(newCs)) {
							//newCompleteStates.add(newCs);
							performCompleteRecursive(newCs, newCompleteStates);
						}
					}	
					else {
						addState(newCs);
					}
				}

			}
			
			private boolean containsComplete(ChartState cs) {
				TermLabel root = cs.root();
				HashSet<ChartState> csSet = completeStatesSet.get(root);
				if (csSet==null)
					return false;
				return csSet.contains(cs);
			}
			
			private HashMap<Integer, HashSet<ChartState>> getCompleteStatesSubScriptIndexed(
					TermLabel beforeDot) {
				HashMap<Integer, HashSet<ChartState>> result = 
					new HashMap<Integer, HashSet<ChartState>>();
				HashSet<ChartState> completeStates = completeStatesSet.get(beforeDot);
				for(ChartState cs : completeStates) {
					int index = cs.subScriptIndex;
					Utility.putInHashMap(result, index, cs);
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
			
			public boolean addState(ChartState cs) {				
				if (!cs.hasElementAfterDot()) {
					TermLabel root = cs.root();
					if (cs.isStarred)
						return Utility.putInHashMap(subUpFirstStatesSet, root, cs);
					else
						return Utility.putInHashMap(completeStatesSet, root, cs);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {
					if (nextTerm==word) {
						return Utility.putInHashMap(scanStatesSet, cs.root(), cs);
					}
					return false;
				}
				return Utility.putInHashMap(subDownFirstStatesSet, nextTerm, cs);
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
				
				public Vector<Duet<ChartState, HashSet<ChartState>>> getParentStates() {

					if (dotIndex==0) {
						return null;
					}
					
					Vector<Duet<ChartState, HashSet<ChartState>>> result =
						new Vector<Duet<ChartState, HashSet<ChartState>>>();
					
					if (isScanState()) { //SCAN					
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.dotIndex--;
						TermLabel beforeDot = peekBeforeDot();
						if (beforeDot.isLexical) {							
							result.add(new Duet<ChartState, HashSet<ChartState>>(
									previousState, null));
							return result;
						}
						result.add(new Duet<ChartState, HashSet<ChartState>>(
								previousState.dotIndex==0 ? null : previousState,
								subUpFirstStatesSet.get(beforeDot)));
						return result;
					}
					 					
					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.dotIndex--;						
						result.add(new Duet<ChartState, HashSet<ChartState>>(
								previousState, null));
						return result;
					}					
					HashMap<Integer,HashSet<ChartState>> completeStatesSubScriptIndexed =
						getCompleteStatesSubScriptIndexed(beforeDot);					
					for(Entry<Integer,HashSet<ChartState>> e : completeStatesSubScriptIndexed.entrySet()) {
						int previousStateColumnIndex = e.getKey();
						HashSet<ChartState> completeStates = e.getValue();
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.dotIndex--;
						HashSet<ChartState> previousSubDownFirstStateSet = 
								previousStateColumn.subDownFirstStatesSet.get(beforeDot); 
						if (previousSubDownFirstStateSet!=null && previousSubDownFirstStateSet.contains(previousState)) {
							result.add(new Duet<ChartState, HashSet<ChartState>>(
									previousState, completeStates));
						}																	
					}		
					return result;																		
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
					//sb.append("$" + wordIndex + ":$ & ");
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
						return 	oCS.fragFringe==this.fragFringe &&
								oCS.subScriptIndex == this.subScriptIndex &&
								oCS.dotIndex==this.dotIndex;
					}
					return false;
				}
			}
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		
		debug = true;
		treeLenghtLimit = 20;
		maxSecPerWord = 5*60;		
		threadCheckingTime = 1000;	
		threads = 1;
		limitTestToSentenceIndex = 0;
		
		String homePath = System.getProperty("user.home");
		String workingPath = homePath + "/PLTSG/ToyCorpus/";	
		File trainTB = new File(workingPath + "trainTB.mrg");
		File testTB = new File(workingPath + "testTB.mrg");
		File fragFile = new File(workingPath + "fragments.mrg");
		
		FragmentExtractor FE = FragmentExtractor.getDefaultFragmentExtractor(
				workingPath, "-1", "-1", trainTB, testTB, fragFile);
		
		FragmentExtractor.printFilteredFragmentsToFile = true;
		
		new IncrementalTSGEarlyParser(FE, testTB, workingPath).run();
	}
	
	

	public static void mainMain(String[] args) throws Exception {
				
		debug = true;
		treeLenghtLimit = 5;
		maxSecPerWord = 5*60;		
		threadCheckingTime = 1000;	
		threads = 1;
		limitTestToSentenceIndex = -1;
		
		String homePath = System.getProperty("user.home");
		String workingPath = homePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";
		File trainTB = new File(workingPath + "wsj-02-21.mrg");
		File testTB = new File(workingPath + "wsj-22.mrg");	
		File fragFile = new File(workingPath + "fragments_approxFreq.txt");
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
				//new String[]{	"No",			"NoSmoothing"},
				
				//combination
				//new String[]{	"100",			"100"},
				//new String[]{	"1000",			"1000"},
				new String[]{	"1000",			"NoSmoothing"},
				//new String[]{	"NoSmoothing",	"NoSmoothing"},
				//new String[]{	"10",			"NoSmoothing"},
				//new String[]{	"100",			"NoSmoothing"},			
		};
		
		for(String[] set : settings) {
			FragmentExtractor FE = FragmentExtractor.getDefaultFragmentExtractor(
					fragExtrWorkingPath, set[0], set[1], trainTB, testTB, fragFile);
			new IncrementalTSGEarlyParser(FE, testTB, outputPathParsing).run();
		}	
		
	}	
	
}
