package tsg.incremental.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.corpora.Wsj;
import tsg.incremental.FragFringe;
import tsg.incremental.FragmentExtractorFreq;
import tsg.incremental.FringeCompatibility;
import tsg.incremental.old.IncrementalTSGEarlyParserViterbiProbBoxNullarySimplified.ChartParserThread.ChartColumn.ChartState;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationRight_LC;
import tsg.mb.TreeMarkoBinarizationRight_LC_nullary;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Duet;
import util.LongestCommonSubsequence;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class IncrementalTSGEarlyParserViterbiProbBoxNullarySimplified extends Thread {
	
	static final Label TOPlabel = Label.getLabel("TOP");
	static final TermLabel TOPnode = TermLabel.getTermLabel(TOPlabel, false);
	static final Label nullaryLabel = Label.getLabel("_");
	static final TermLabel nullaryNode = TermLabel.getTermLabel("_", true);
	static final TermLabel emptyLexNode = TermLabel.getTermLabel("", true);	
	static final String WsjFlatGoldPath = Parameters.corpusPath + "WSJ/FLAT_NOTRACES/";
	static final String WsjOrigianlGoldPath = Parameters.corpusPath + "WSJ/ORIGINAL_READABLE_CLEANED_TOP_SEMTAGSOFF/";
	static File originalGoldFile = null;
	
	static boolean debug = true;
	static boolean orderedDebug = true;
	static int treeLenghtLimit = -1;	
	static long maxSecPerWord = 5*60;	
	static long threadCheckingTime = 10000;	
	static int threads = 1;
	static int limitTestToSentenceIndex = -1;
	static double minFreqTemplatesSmoothingFactor = 1E-5;
	static double minFreqOpenPoSSmoothing = 1E-2;
	static double minFreqFirstWordFragSmoothing = 1E-1;
	static boolean removeFringesNonCompatibleWithSentence;
	static boolean printAllLexFragmentsToFileWithProb;
	static boolean printTemplateFragsToFileWithProb;
	static boolean topDownProb = false;
	static boolean computeIncrementalEvaluation = false;
	
	static boolean computeMPPtree = true;
	static int kBest = 10;
	static TreeMarkoBinarization TMB;
	
	File testTBfile, logFile, goldTBFile;	
	File evalC_MPD_File, evalB_MPD_File, evalC_MPP_File, evalB_MPP_File;
	File parsedMPDFile, parsedMPPFile;
	String incrementalParsedMinConnectPrefixFileName, incrementalGoldMinConnectPrefixFileName, incrementalEvalMinConnectPrefixFileName;
	String incrementalParsedMaxPredictedPrefixFileName, incrementalGoldMaxPredictedPrefixFileName, incrementalEvalMaxPredictedPrefixFileName;
	File incrementalParsedMinConnectedAllFile, incrementalParsedMinConnectedLastFile, incrementalGoldMinConnectedAllFile, incrementalEvalMinConnectedAllFile;
	File incrementalParsedMaxPredictedAllFile, incrementalGoldMaxPredictedAllFile, incrementalEvalMaxPredictedAllFile;
	File allMinConnPrefixResults;
	FragmentExtractorFreq FE;
	String outputPath;
	ArrayList<TSNodeLabel> testTB;
	TSNodeLabel[] parsedTB_MPD, parsedTB_MPP;		
	TSNodeLabel[][] partialParsedTB;
	PrintProgress progress;	
	int parsedTrees, potentiallyReachingTopTrees, 
		actuallyReachedTopTrees, interruptedTrees, 
		indexLongestWordTime, totalSubDerivations,
		ambiguousViterbiDerivations;
	int[] countSubDownSubUpTotal;
	float longestWordTime;
	Iterator<TSNodeLabel> testTBiterator;
	int treebankIteratorIndex;
	boolean templateSmoothing;
	EvalC evalC_MPD, evalC_MPP, incrementalMinConnectedAllEvalC, incrementalMaxPredictedAllEvalC;	
	EvalC[] incrementalMinConnectedEvalC, incrementalMaxPredictedEvalC;
	int[] predictedWordCorrect;
	HashMap<Integer,int[]> predictedLengthFreq;
	StringBuilder[] orderedDebugSBArray;
	int orderedDebugNextIndex;
	SimpleTimer globalTimer;
	boolean nullaryArePresent;	
	//String[][] goldSentenceWords;
	
	
	HashMap<FragFringe,HashMap<TSNodeLabel, double[]>> 
		firstLexFringeFragMap, firstLexTemplateFragMap,
		firstSubFringeFragMap, firstSubTemplateFragMap;
	
	//used only when nullaries are present
	/*
	HashMap<FragFringe,HashMap<TSNodeLabel, double[]>> 		
		firstLexNullaryFringeFragMap,
		firstSubNullaryFringeFragMap;
	*/
		
	//used only for first word in LEFT mode
	HashMap<FragFringe,HashMap<TSNodeLabel, double[]>> 
		firstWordLexFringeFragMap, firstWordFringesTemplatesFragMap;	
	
	HashMap<TermLabel, 
		HashMap<TermLabel, HashMap<FragFringe, double[]>>> 
			firstLexFringes, firstLexFringesTemplates, // indexed on lex and root
			firstSubFringes, firstSubFringesTemplates; // indexed on lex and firstTerm
			
	//used only when nullaries are present
	/*
	HashMap<TermLabel, HashMap<FragFringe, double[]>>			
			firstLexNullaryFringes; // indexed on root [only to close constituents, yield is only "_"]
	HashMap<TermLabel, 
		HashMap<TermLabel, HashMap<FragFringe, double[]>>>
			firstSubNullaryFringes; // indexed on lex (after nullary or null if none) and root
	*/
			
	//used only for first word in LEFT mode
	HashMap<TermLabel, 
		HashMap<TermLabel, HashMap<FragFringe, double[]>>>
			firstWordFirstLexFringes, firstWordFirstLexFringesTemplates; //indexed on lex and root 	
	
	HashMap<TermLabel, HashMap<TermLabel,int[]>> posLexFreqTable, lexPosFreqTable;
	HashMap<TermLabel, HashSet<TermLabel>> posLexTable;
	Set<TermLabel> posSet;
	
	public IncrementalTSGEarlyParserViterbiProbBoxNullarySimplified (FragmentExtractorFreq FE,
			File testTBfile, String outputPath) throws Exception {
		this.FE = FE;
		this.testTBfile = testTBfile;
		this.testTB = Wsj.getTreebank(testTBfile);
		this.outputPath = outputPath;						
	}
	
	private void initVariables() {
		globalTimer = new SimpleTimer(true);
		this.templateSmoothing = 
			FragmentExtractorFreq.smoothingFromFrags || 
			FragmentExtractorFreq.smoothingFromMinSet;
		if (!templateSmoothing)
			minFreqTemplatesSmoothingFactor = 0;
		if (topDownProb)
			minFreqFirstWordFragSmoothing = 0;
		
		String minSetThreshold = FragmentExtractorFreq.minFragFreqForSmoothingFromMinSet == -1 ?
				"NoThreshold" : "" + FragmentExtractorFreq.minFragFreqForSmoothingFromMinSet;
		String fragThreshold = FragmentExtractorFreq.minFragFreqForSmoothingFromFrags == -1 ?
				"NoThreshold" : "" + FragmentExtractorFreq.minFragFreqForSmoothingFromFrags;
		
		
		String section = testTBfile.getName().substring(
				testTBfile.getName().indexOf('.')-2,testTBfile.getName().indexOf('.'));
		
		outputPath += "EPVP_wsj" + section + "_";		
		outputPath += treeLenghtLimit==-1 ? "ALL" : "upTo" + treeLenghtLimit;
		outputPath += "_openPosTh_" + FragmentExtractorFreq.openClassPoSThreshold;
		outputPath += "_openPosSmooth_" + minFreqOpenPoSSmoothing;		
		outputPath += "_minFreqFirstWordFragSmoothing_" + minFreqFirstWordFragSmoothing;	
		outputPath += "_minFreqTemplatesSmoothingFactor_" + minFreqTemplatesSmoothingFactor;
		outputPath += FragmentExtractorFreq.addMinimalFragments ? 
				("_MinSetExt_" + minSetThreshold) : "_NoMinSet";
		outputPath += FE.fragmentsFile!=null ? 
				("_Frags_" + fragThreshold) : "_NoFrags";
		outputPath += topDownProb ? "_TOP" : "_LEFT";
		outputPath += "/";
		
		File dirFile = new File(outputPath);
		if (dirFile.exists()) {
			System.out.println("Output dir already exists, I'll delete the content...");
			FileUtil.removeAllFileAndDirInDir(dirFile);			
		}
		else {
			dirFile.mkdirs();
		}

		logFile = new File(outputPath + "LOG.log");		
		
		parsedMPDFile = new File(outputPath + "PARSED_MPD.mrg");
		evalC_MPD_File = new File(outputPath + "Eval_MPD.evalC"); 	
		evalB_MPD_File = new File(outputPath + "Eval_MPD.evalB");
		if (computeMPPtree) {
			parsedMPPFile = new File(outputPath + "PARSED_MPP.mrg");
			evalC_MPP_File = new File(outputPath + "Eval_MPP.evalC"); 	
			evalB_MPP_File = new File(outputPath + "Eval_MPP.evalB");
		}
		goldTBFile = new File(outputPath + "GOLD.mrg");
		
		this.countSubDownSubUpTotal = new int[2];
		
		if (computeIncrementalEvaluation) {
			String incrementalSubDir = outputPath + "incremental/";
			new File(incrementalSubDir).mkdir();
			incrementalParsedMinConnectPrefixFileName = incrementalSubDir + "PARSED_MinConnected_prefixLength_";
			incrementalGoldMinConnectPrefixFileName = incrementalSubDir + "GOLD_MinConnected_prefixLength_";
			incrementalEvalMinConnectPrefixFileName = incrementalSubDir + "Eval_MinConnected_prefixLength_";
			incrementalParsedMaxPredictedPrefixFileName = incrementalSubDir + "PARSED_MaxPredicted_prefixLength_";;
			incrementalGoldMaxPredictedPrefixFileName = incrementalSubDir + "GOLD_MaxPredicted_prefixLength_";
			incrementalEvalMaxPredictedPrefixFileName = incrementalSubDir + "Eval_MaxPredicted_prefixLength_";
			
			incrementalParsedMinConnectedAllFile = new File(incrementalSubDir + "PARSED_MinConnected_All.mrg");
			incrementalParsedMinConnectedLastFile = new File(incrementalSubDir + "PARSED_MinConnected_Last.mrg");
			incrementalGoldMinConnectedAllFile = new File(incrementalSubDir + "GOLD_MinConnected_All.mrg");
			incrementalEvalMinConnectedAllFile = new File(incrementalSubDir + "Eval_MinConnected_All.evalC");
			
			incrementalParsedMaxPredictedAllFile  = new File(incrementalSubDir + "PARSED_MaxPredicted_All.mrg");
			incrementalGoldMaxPredictedAllFile  = new File(incrementalSubDir + "GOLD_MaxPredicted_All.mrg");
			incrementalEvalMaxPredictedAllFile  = new File(incrementalSubDir + "Eval_MaxPredicted_All.evalC");

			allMinConnPrefixResults = new File(incrementalSubDir+ "Prefix_MinConn_Results.txt");
		}			
		nullaryArePresent = FragmentExtractorFreq.nullaryArePresent;

	}
	
	public void run() {		
		initVariables();
		Parameters.openLogFile(logFile);
			try {
				FE.extractFragments(logFile);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			printParameters();			
			convertPosSet();
			convertLexFragmentsToFringes();			
			convertTemplateFragmentsToFringes();
			convertFreqToLogRelativeFreq();
			printFringesToFile();
			filterTestTB();
			parseIncremental();
			evaluate();
			printFinalSummary();		
		Parameters.closeLogFile();			
	}



	private void printParameters() {
		Parameters.reportLine("\n\n");
		Parameters.reportLine("------------------------------------");
		Parameters.reportLine("INCREMENTAL TSG EARLY VITERBI PROB ");
		Parameters.reportLine("------------------------------------");
		Parameters.reportLine("");
		Parameters.reportLine("Log file: " + logFile);
		Parameters.reportLine("Test Tree Length Limit: " + treeLenghtLimit);		
		Parameters.reportLine("Max sec Per Tree: " + maxSecPerWord);
		Parameters.reportLine("Number of threads: " + threads);		
		Parameters.reportLine("Limit Test To Sentence Index: " + limitTestToSentenceIndex);
		Parameters.reportLine("Min Freq OpenPoS Smoothing: " + minFreqOpenPoSSmoothing);
		Parameters.reportLine("Min Freq Templates Smoothing: " + minFreqTemplatesSmoothingFactor);
		Parameters.reportLine("Min Freq First Word Frag Smoothing: " + minFreqFirstWordFragSmoothing);
		Parameters.reportLine("Compute Incremental Evaluation: " + computeIncrementalEvaluation);
		
		Parameters.reportLineFlush("Prob Model (top/left): " + (topDownProb ? "TOP-DOWN" : "LEFT-RIGHT"));
		
	}
	
	private void printFinalSummary() {
		Parameters.reportLine("Number of successfully parsed trees: " + parsedTrees);
		Parameters.reportLine("\t... of which potentially reaching TOP: " + potentiallyReachingTopTrees);
		Parameters.reportLine("\t... and actually reached TOP: " + actuallyReachedTopTrees);
		Parameters.reportLine("Number of interrupted trees: " + interruptedTrees);
		Parameters.reportLine("Longest time to parse a word (sec): " + longestWordTime + " (" + indexLongestWordTime + ")");
		Parameters.reportLine("Total Sub-Down Sub-Up: " + Arrays.toString(countSubDownSubUpTotal) + 
				" " + Arrays.toString(Utility.makePercentage(countSubDownSubUpTotal)));
		Parameters.reportLine("Final evalC scores MPD: " + Arrays.toString(evalC_MPD.getRecallPrecisionFscoreAll()));
		if (computeMPPtree) {			
			Parameters.reportLine("Final evalC scores MPP: " + Arrays.toString(evalC_MPP.getRecallPrecisionFscoreAll()));
		}
		Parameters.reportLine("Average tree viterbi sub-derivations: " + ((float)totalSubDerivations)/testTB.size());
		Parameters.reportLine("Trees having ambiguous viterbi derivations: " + ambiguousViterbiDerivations + 
				" (" + Utility.makePercentage(ambiguousViterbiDerivations, testTB.size()) + ")");
		if (computeIncrementalEvaluation) {
			StringBuilder pw = new StringBuilder();
			
			pw.append("Final incremental evalC scores MIN CONNECTED ALL (R_P_F1): ");
			pw.append(Arrays.toString(incrementalMinConnectedAllEvalC.getRecallPrecisionFscoreAll()));
			pw.append('\n');
			
			pw.append("Final incremental evalC scores MIN CONNECTED (prefix_length/R_P_F1):");
			for(int i=0; i<incrementalMinConnectedEvalC.length; i++) {
				pw.append(" " + (i+1) + ":" + 
						Arrays.toString(incrementalMinConnectedEvalC[i].getRecallPrecisionFscoreAll()));				
			}
			pw.append('\n');
			
			/*
			pw.append("Final incremental evalC scores MAX PREDICTED ALL (R_P_F1): ");
			pw.append(Arrays.toString(incrementalMaxPredictedAllEvalC.getRecallPrecisionFscoreAll()));
			pw.append('\n');
						
			pw.append("Final incremental evalC scores MAX PREDICTED (prefix_length/F1):");
			for(int i=0; i<incrementalMaxPredictedEvalC.length; i++) {
				pw.append(" " + (i+1) + ":" + 
						Arrays.toString(incrementalMaxPredictedEvalC[i].getRecallPrecisionFscoreAll()));				
			}
			*/
			
			Parameters.reportLine(pw.toString());
			Parameters.reportLine("Predicted words length|frequency|correct% : " + predictedLengthFreqToString());
			Parameters.reportLine("Predicted words/correctly: " 
					+ Arrays.toString(predictedWordCorrect) + " (" +
					(float)predictedWordCorrect[1]/predictedWordCorrect[0]*100 + "%)");					
		}
		Parameters.reportLine("Took in Total (sec): " + globalTimer.checkEllapsedTimeSec());
		Parameters.reportLineFlush("Log file: " + logFile);
		
	}
	
	private String predictedLengthFreqToString() {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<Integer,int[]>> iter = predictedLengthFreq.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Integer,int[]> e = iter.next();
			int predictedLength = e.getKey();
			int[] freqCorrect = e.getValue();
			sb.append(predictedLength);
			sb.append("|");
			sb.append(freqCorrect[0]);
			sb.append("|");
			sb.append((float)freqCorrect[1]/(predictedLength*freqCorrect[0])*100 + "%");			
			if (iter.hasNext())
				sb.append(", ");
		}		
		return sb.toString();
	}
	
	private void convertPosSet() {
		posLexFreqTable = convertFreqTable(FE.posLexFinal, false, true);
		lexPosFreqTable = convertFreqTable(FE.lexPosFinal, true, false);
		posLexTable = convertTable(FE.posLexFinal, false, true);
		posSet = posLexFreqTable.keySet();
	}

	private static HashMap<TermLabel, HashMap<TermLabel, int[]>>  convertFreqTable(
			HashMap<Label, HashMap<Label, int[]>> posLexFinal, 
			boolean keyIsLex, boolean valuesAreLex) {
		
		HashMap<TermLabel, HashMap<TermLabel, int[]>> result =
			new HashMap<TermLabel, HashMap<TermLabel, int[]>>(); 
		
		for(Entry<Label, HashMap<Label, int[]>>  e : posLexFinal.entrySet()) {
			TermLabel key = TermLabel.getTermLabel(e.getKey(), keyIsLex);
			HashMap<TermLabel,int[]> value = new HashMap<TermLabel,int[]>();
			for(Entry<Label,int[]> f : e.getValue().entrySet()) {
				value.put(TermLabel.getTermLabel(f.getKey(), valuesAreLex),
					new int[]{f.getValue()[0]});
			}			
			result.put(key,value);
		}
		
		return result;
	}
	
	private static HashMap<TermLabel, HashSet<TermLabel>> convertTable(
			HashMap<Label, HashMap<Label, int[]>> posLexFinal, 
			boolean keyIsLex, boolean valuesAreLex) {
		
		HashMap<TermLabel, HashSet<TermLabel>> result =
			new HashMap<TermLabel, HashSet<TermLabel>>(); 
		
		for(Entry<Label, HashMap<Label, int[]>>  e : posLexFinal.entrySet()) {
			TermLabel key = TermLabel.getTermLabel(e.getKey(), keyIsLex);
			HashSet<TermLabel> value = new HashSet<TermLabel>();
			for(Label l : e.getValue().keySet()) {
				value.add(TermLabel.getTermLabel(l, valuesAreLex));
			}			
			result.put(key,value);
		}
		
		return result;
	}

	private void convertLexFragmentsToFringes() {
		
		Parameters.reportLineFlush("Converting lex fragments into fringes...");
		
		firstLexFringes = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
		firstSubFringes = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
		firstLexFringeFragMap = new HashMap<FragFringe,HashMap<TSNodeLabel,double[]>>();
		firstSubFringeFragMap = new HashMap<FragFringe,HashMap<TSNodeLabel,double[]>>();
		
		/*
		if (nullaryArePresent) {
			firstLexNullaryFringes = new HashMap<TermLabel, HashMap<FragFringe, double[]>>();
			firstSubNullaryFringes = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
			firstLexNullaryFringeFragMap = new HashMap<FragFringe, HashMap<TSNodeLabel,double[]>>();
			firstSubNullaryFringeFragMap = new HashMap<FragFringe, HashMap<TSNodeLabel,double[]>>();
		}
		*/
		
		if (!topDownProb) {
			firstWordFirstLexFringes = new HashMap<TermLabel, HashMap<TermLabel,HashMap<FragFringe,double[]>>>();
			firstWordLexFringeFragMap = new HashMap<FragFringe,HashMap<TSNodeLabel,double[]>>();
		}
		
		int countFirstLexFrags = 0;
		int countFirstSubFrags = 0;
		
		//FIRST LEX FRINGES
		for(Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstLex.entrySet()) {			
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			for(Entry<TSNodeLabel,int[]> f : fragSet.entrySet()) {
				TSNodeLabel firstLexFrag = f.getKey();
				FragFringe ff = new FragFringe(firstLexFrag, nullaryArePresent);
				int[] freqArray = f.getValue();
				boolean posSmoothing = freqArray[0]==0;
				if (topDownProb) {
					int freq = freqArray[0];					
					double d = posSmoothing ? minFreqOpenPoSSmoothing : freq;
					if (d>0) {
						countFirstLexFrags++;
						Utility.putInMapDouble(firstLexFringeFragMap, 
								ff, firstLexFrag, new double[]{d});
					}
				}
				else {
					int freq = freqArray[0]-freqArray[1]; // freqArray[0] always >= freqArray[1]					
					double d = posSmoothing ? minFreqOpenPoSSmoothing : 
						(freq==0 ? minFreqFirstWordFragSmoothing : freq);
					if (d>0) { 
						countFirstLexFrags++;
						Utility.putInMapDouble(firstLexFringeFragMap, 
								ff, firstLexFrag, new double[]{d});
					}
					freq = f.getValue()[1];
					d = posSmoothing ? minFreqOpenPoSSmoothing : 
						(freq==0 ? minFreqFirstWordFragSmoothing : freq);					
					if (d>0) {
						Utility.putInMapDouble(firstWordLexFringeFragMap, ff, firstLexFrag, new double[]{d});
					}
				}								
			}	
		}
		
		// PUTTING HIGHEST FREQ PER FRAG FRINGE FIRST LEX
		for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstLexFringeFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			TermLabel firstLexLabel = ff.firstTerminal();
			double maxFreq = Utility.getMaxValue(e.getValue());
			Utility.putInMapTriple(firstLexFringes, firstLexLabel, 
					ff.root(), ff, new double[]{maxFreq});
		}
		// PUTTING HIGHEST FREQ PER FRAG FRINGE FIRST LEX FIRST WORD
		if (!topDownProb) {
			for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstWordLexFringeFragMap.entrySet()) {
				FragFringe ff = e.getKey();
				TermLabel firstLexLabel = ff.firstTerminal();
				double maxFreq = Utility.getMaxValue(e.getValue());
				Utility.putInMapTriple(firstWordFirstLexFringes, firstLexLabel, ff.root(), ff, new double[]{maxFreq});
			}
		}

		//FIRST SUB FRINGES
		for(Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstSub.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			for(Entry<TSNodeLabel,int[]> f : fragSet.entrySet()) {
				TSNodeLabel firstSubFrag = f.getKey();
				FragFringe ff = new FragFringe(firstSubFrag);
				int freq = f.getValue()[0];				
				double d = freq==0 ? minFreqOpenPoSSmoothing : freq;
				if (d>0) {
					countFirstSubFrags++;
					Utility.putInMapDouble(firstSubFringeFragMap, ff, firstSubFrag, new double[]{d});
				}													
			}
		}
		
		// PUTTING HIGHEST FREQ PER FRAG FRINGE FIRST SUB
		for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstSubFringeFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			TermLabel firstLexLabel = ff.yield[1];
			double maxFreq = Utility.getMaxValue(e.getValue());
			Utility.putInMapTriple(firstSubFringes, firstLexLabel, 
					ff.firstTerminal(), ff, new double[]{maxFreq});
		}
		
		/*
		if (nullaryArePresent) {
			//FIRST LEX NULLARY FRINGES (of the type NP@ < "_")
			for(Entry<TSNodeLabel, int[]> e : FE.lexFragsTableFirstLexNullary.entrySet()) {
				TSNodeLabel firstLexFrag = e.getKey();
				FragFringe ff = new FragFringe(firstLexFrag);
				int freq = e.getValue()[0];
				Utility.putInHashMapDouble(firstLexNullaryFringeFragMap, 
						ff, firstLexFrag, new double[]{freq});
				// only one no need to maximize
				Utility.putInHashMapDouble(firstLexNullaryFringes, ff.root, ff, new double[]{freq});
			}
			//FIRST SUB NULLARY FRINGES
			for(Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstSubNullary.entrySet()) {
				HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
				for(Entry<TSNodeLabel,int[]> f : fragSet.entrySet()) {
					TSNodeLabel firstSubFrag = f.getKey();
					FragFringe ff = new FragFringe(firstSubFrag);
					int freq = f.getValue()[0];				
					Utility.putInHashMapDouble(firstSubNullaryFringeFragMap, ff, 
							firstSubFrag, new double[]{freq});					
				}
			}
			// PUTTING HIGHEST FREQ PER NULLARY FRAG FRINGE FIRST SUB
			for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstSubNullaryFringeFragMap.entrySet()) {
				FragFringe ff = e.getKey();
				TermLabel firstLexLabel = ff.firstLexNonNullary();
				double maxFreq = Utility.getMaxValue(e.getValue());
				Utility.putInHashMapTriple(firstSubNullaryFringes, firstLexLabel, 
						ff.firstTerminal(), ff, new double[]{maxFreq});
			}
		}
		*/
		
		int totalLexFrags = countFirstLexFrags+countFirstSubFrags;
		Parameters.reportLineFlush("Total number of lex fragments: " + totalLexFrags);
		Parameters.reportLineFlush("\tLex first: " + countFirstLexFrags);
		Parameters.reportLineFlush("\tSub first: " + countFirstSubFrags);
		int countFirstLexFringes = firstLexFringeFragMap.size();
		int countFirstSubFringes = firstSubFringeFragMap.size();
		int totalLexFringes = countFirstLexFringes+countFirstSubFringes;
		Parameters.reportLineFlush("Total number of lex fringes: " + totalLexFringes);
		Parameters.reportLineFlush("\tLex first: " + countFirstLexFringes);
		Parameters.reportLineFlush("\tSub first: " + countFirstSubFringes);
		int ambiguousFringesFirstLex = Utility.countAmbiguousMapping(firstLexFringeFragMap);
		int ambiguousFringesFirstSub = Utility.countAmbiguousMapping(firstSubFringeFragMap);
		int totalAmbiguousFringe = ambiguousFringesFirstLex + ambiguousFringesFirstSub;
		Parameters.reportLineFlush("Ambiguous fringes: " + totalAmbiguousFringe + 
				" ("  + Utility.makePercentage(totalAmbiguousFringe, totalLexFringes) + ")");
		
		/*
		if (nullaryArePresent) {
			int countFirstLexNullaryFringes = firstLexNullaryFringeFragMap.size();
			int countFirstSubNullaryFringes = firstSubNullaryFringeFragMap.size();
			int totalNullaryFringes = countFirstLexNullaryFringes + countFirstSubNullaryFringes;
			Parameters.reportLineFlush("Total number of nullary fringes: " + totalNullaryFringes);
			Parameters.reportLineFlush("\tLex first: " + countFirstLexNullaryFringes);
			Parameters.reportLineFlush("\tSub first: " + countFirstSubNullaryFringes);
		}
		*/
	}
	
	

	private void convertTemplateFragmentsToFringes() {
		
		if (!templateSmoothing)
			return;
		
		Parameters.reportLineFlush("Converting template fragments into fringes...");
		
		int[] countFirstLexSubFrags = new int[2];		

		firstLexFringesTemplates = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe,double[]>>>();
		firstSubFringesTemplates = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe,double[]>>>();
		firstLexTemplateFragMap = new HashMap<FragFringe,HashMap<TSNodeLabel,double[]>>();
		firstSubTemplateFragMap = new HashMap<FragFringe,HashMap<TSNodeLabel,double[]>>();
		 		
		if (!topDownProb) {
			firstWordFirstLexFringesTemplates = new HashMap<TermLabel, HashMap<TermLabel,HashMap<FragFringe,double[]>>>();
			firstWordFringesTemplatesFragMap = new HashMap<FragFringe,HashMap<TSNodeLabel,double[]>>();			
		}									
		
		
		for(Entry<Label, HashMap<TSNodeLabel,int[]>> e : FE.posFragSmoothingMerged.entrySet()) {
			HashMap<TSNodeLabel,int[]> fragMap = e.getValue();
			for(Entry<TSNodeLabel,int[]> f : fragMap.entrySet()) {
				TSNodeLabel frag = f.getKey();
				FragFringe ff = new FragFringe(frag);
				boolean isFirstLex = ff.firstTerminalIsLexical(); 										
				double d = minFreqTemplatesSmoothingFactor;					
				if (d>0) {						
					if (isFirstLex) {
						countFirstLexSubFrags[0]++;
						Utility.putInMapDouble(firstLexTemplateFragMap, ff, frag, new double[]{d});							
					}
					else {
						countFirstLexSubFrags[1]++;
						Utility.putInMapDouble(firstSubTemplateFragMap, ff, frag, new double[]{d});
					}
				}
				if (!topDownProb) {
					d = minFreqTemplatesSmoothingFactor;
					if (d>0) {
						Utility.putInMapDouble(firstWordFringesTemplatesFragMap, ff, frag, new double[]{d});							
					}
				}
			}
		}		

		// PUTTING HIGHEST FREQ PER FRAG FRINGE
		for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstLexTemplateFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			TermLabel firstPosLabel = ff.firstTerminal();
			double maxFreq = Utility.getMaxValue(e.getValue());
			Utility.putInMapTriple(firstLexFringesTemplates, firstPosLabel, 
					ff.root(), ff, new double[]{maxFreq});
		}		
	
		for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstSubTemplateFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			TermLabel firstPosLabel = ff.yield[1];
			double maxFreq = Utility.getMaxValue(e.getValue());
			Utility.putInMapTriple(firstSubFringesTemplates, firstPosLabel, 
					ff.firstTerminal(), ff, new double[]{maxFreq});
		}
		
		if (!topDownProb) {
			for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstWordFringesTemplatesFragMap.entrySet()) {
				FragFringe ff = e.getKey();
				TermLabel firstPosLabel = ff.firstTerminal();
				double maxFreq = Utility.getMaxValue(e.getValue());
				Utility.putInMapTriple(firstWordFirstLexFringesTemplates, firstPosLabel, 
						ff.root(), ff, new double[]{maxFreq});
			}
		}
		
		int[] countFirstLexSubFringes = new int[]{firstLexTemplateFragMap.size(), firstSubTemplateFragMap.size()};
				
		Parameters.reportLineFlush("Total number of smoothed fragments: " + (Utility.sum(countFirstLexSubFrags)));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexSubFrags[0]);
		Parameters.reportLineFlush("\tSub first: " + countFirstLexSubFrags[1]);
		Parameters.reportLineFlush("Total number of smoothed fringes: " + (Utility.sum(countFirstLexSubFringes)));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexSubFringes[0]);
		Parameters.reportLineFlush("\tSub first: " + countFirstLexSubFringes[1]);
		
	}
	
	private void convertFreqToLogRelativeFreq() {
		Parameters.reportLineFlush("Converting Freq to Log Relative Freq");
		if (topDownProb)
			convertFreqToLogRelativeFreqTOP();
		else
			convertFreqToLogRelativeFreqLEFT();
	}
	
	private void convertFreqToLogRelativeFreqTOP() {
		
		HashMap<TermLabel, double[]> totalRootFreq = new HashMap<TermLabel,double[]>();
			
		addRootFreq(firstLexFringeFragMap,totalRootFreq);
		addRootFreq(firstSubFringeFragMap,totalRootFreq);
		
		/*
		if (nullaryArePresent) {
			addRootFreq(firstLexNullaryFringeFragMap,totalRootFreq);
			addRootFreq(firstSubNullaryFringeFragMap,totalRootFreq);
		}
		*/
		
		if (templateSmoothing) {
			addRootFreq(firstLexTemplateFragMap,totalRootFreq);
			addRootFreq(firstSubTemplateFragMap,totalRootFreq);
		}
		
		convertToLogRootRelativeFreq(firstLexFringes,totalRootFreq);
		convertToLogRootRelativeFreq(firstSubFringes,totalRootFreq);
		convertToLogRootRelativeFreqFragMap(firstLexFringeFragMap,totalRootFreq);
		convertToLogRootRelativeFreqFragMap(firstSubFringeFragMap,totalRootFreq);
		
		/*
		if (nullaryArePresent) {
			convertToLogRootRelativeFreqSimple(firstLexNullaryFringes,totalRootFreq);
			convertToLogRootRelativeFreq(firstSubNullaryFringes, totalRootFreq);
			convertToLogRootRelativeFreqFragMap(firstLexNullaryFringeFragMap,totalRootFreq);
			convertToLogRootRelativeFreqFragMap(firstSubNullaryFringeFragMap,totalRootFreq);
		}
		*/
		
		if (templateSmoothing) {
			convertToLogRootRelativeFreq(firstLexFringesTemplates,totalRootFreq);
			convertToLogRootRelativeFreq(firstSubFringesTemplates,totalRootFreq);
			convertToLogRootRelativeFreqFragMap(firstLexTemplateFragMap,totalRootFreq);
			convertToLogRootRelativeFreqFragMap(firstSubTemplateFragMap,totalRootFreq);
		}
				
	}

	private void convertFreqToLogRelativeFreqLEFT() {		
		
		//FIRST WORD
		double totalSumFirstWord = getTotalSummedProb(firstWordLexFringeFragMap);
		if (templateSmoothing) {
			totalSumFirstWord+= getTotalSummedProb(firstWordFringesTemplatesFragMap);
		}		
		convertToLogRelativeFreq(firstWordFirstLexFringes,totalSumFirstWord);
		convertToLogRelativeFreqFragMap(firstWordLexFringeFragMap,totalSumFirstWord);
		if (templateSmoothing) {
			convertToLogRelativeFreq(firstWordFirstLexFringesTemplates,totalSumFirstWord);
			convertToLogRelativeFreqFragMap(firstWordFringesTemplatesFragMap,totalSumFirstWord);
		}
		
		//FIRST LEX
		HashMap<TermLabel, double[]> totalRootFreqFirstLex = new HashMap<TermLabel,double[]>();		
		addRootFreq(firstLexFringeFragMap,totalRootFreqFirstLex);
		
		/*
		if (nullaryArePresent) {
			addRootFreq(firstLexNullaryFringeFragMap,totalRootFreqFirstLex);
		}
		*/
		
		if (templateSmoothing) {
			addRootFreq(firstLexTemplateFragMap,totalRootFreqFirstLex);
		}				
		convertToLogRootRelativeFreq(firstLexFringes,totalRootFreqFirstLex);
		convertToLogRootRelativeFreqFragMap(firstLexFringeFragMap,totalRootFreqFirstLex);
		
		/*
		if (nullaryArePresent) {
			convertToLogRootRelativeFreqSimple(firstLexNullaryFringes,totalRootFreqFirstLex);
			convertToLogRootRelativeFreqFragMap(firstLexNullaryFringeFragMap,totalRootFreqFirstLex);
		}
		*/
		
		if (templateSmoothing) {
			convertToLogRootRelativeFreq(firstLexFringesTemplates,totalRootFreqFirstLex);
			convertToLogRootRelativeFreqFragMap(firstLexTemplateFragMap,totalRootFreqFirstLex);
		}
		
		//FIRST SUB
		HashMap<TermLabel, double[]> totalFirstTermFreqSecondLex = new HashMap<TermLabel,double[]>();
		addFirstTermFreq(firstSubFringeFragMap,totalFirstTermFreqSecondLex);
		
		/*
		if (nullaryArePresent) {
			addFirstTermFreq(firstSubNullaryFringeFragMap,totalFirstTermFreqSecondLex);
		}
		*/
		
		if (templateSmoothing) {
			addFirstTermFreq(firstSubTemplateFragMap,totalFirstTermFreqSecondLex);
		}		
		convertToLogFirstTermRelativeFreq(firstSubFringes,totalFirstTermFreqSecondLex);
		convertToLogFirstTermRelativeFreqFragMap(firstSubFringeFragMap,totalFirstTermFreqSecondLex);
		
		/*
		if (nullaryArePresent) {
			convertToLogFirstTermRelativeFreq(firstSubNullaryFringes,totalFirstTermFreqSecondLex);
			convertToLogFirstTermRelativeFreqFragMap(firstSubNullaryFringeFragMap,totalFirstTermFreqSecondLex);
		}
		*/
		
		if (templateSmoothing) {
			convertToLogFirstTermRelativeFreq(firstSubFringesTemplates,totalFirstTermFreqSecondLex);
			convertToLogFirstTermRelativeFreqFragMap(firstSubTemplateFragMap,totalFirstTermFreqSecondLex);
		}
				
	}

	private void addRootFreq(
			HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> source,
			HashMap<TermLabel, double[]> target) {
		
		for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : source.entrySet()) {
			TermLabel root = e.getKey().root;
			for(Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {				
				Utility.increaseInHashMap(target, root, f.getValue()[0]);
			}
		}
		
	}
	
	private void addFirstTermFreq(
			HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> source,
			HashMap<TermLabel, double[]> target) {
		
		for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : source.entrySet()) {
			TermLabel firstTerm = e.getKey().firstTerminal();
			for(Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {				
				Utility.increaseInHashMap(target, firstTerm, f.getValue()[0]);
			}
		}
		
	}


	
	private double getTotalSummedProb(HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> source) {
		double result = 0;
		for(HashMap<TSNodeLabel, double[]> t : source.values()) {
			for(double[] d : t.values()) {
				result += d[0];
			}
		}
		return result;
	}

	private void convertToLogRootRelativeFreqFragMap(
			HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> table,
			HashMap<TermLabel, double[]> totalRootFreq) {
		
		for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : table.entrySet()) {
			TermLabel root = e.getKey().root;
			for(Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {				
				double rootFreq = totalRootFreq.get(root)[0];
				double[] freqArray = f.getValue();				
				freqArray[0] = Math.log(freqArray[0]/rootFreq);
			}
		}		
	}
	
	private void convertToLogFirstTermRelativeFreqFragMap(
			HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> table,
			HashMap<TermLabel, double[]> totalRootFreq) {
		
		for(Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : table.entrySet()) {
			TermLabel firstTerm = e.getKey().firstTerminal();
			for(Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {				
				double rootFreq = totalRootFreq.get(firstTerm)[0];
				double[] freqArray = f.getValue();				
				freqArray[0] = Math.log(freqArray[0]/rootFreq);
			}
		}	
	}

	private static void convertToLogRootRelativeFreq(
			HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table,
			HashMap<TermLabel, double[]> totalRootFreq) {
		
		for(HashMap<TermLabel, HashMap<FragFringe, double[]>> t : table.values()) {			
			convertToLogRootRelativeFreqSimple(t, totalRootFreq);
		}		
	}
	
	private static void convertToLogRootRelativeFreqSimple(
			HashMap<TermLabel, HashMap<FragFringe, double[]>> table,
			HashMap<TermLabel, double[]> totalRootFreq) {
		
		for(HashMap<FragFringe, double[]> s : table.values()) {
			for(Entry<FragFringe, double[]> e : s.entrySet()) {
				TermLabel root = e.getKey().root;
				double rootFreq = totalRootFreq.get(root)[0];
				double[] freqArray = e.getValue();
				freqArray[0] = Math.log(freqArray[0]/rootFreq);
			}
		}		
	}
	
	private static void convertToLogFirstTermRelativeFreq(
			HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table,
			HashMap<TermLabel, double[]> totalRootFreq) {
		
		for(HashMap<TermLabel, HashMap<FragFringe, double[]>> t : table.values()) {
			for(HashMap<FragFringe, double[]> s : t.values()) {
				for(Entry<FragFringe, double[]> e : s.entrySet()) {
					TermLabel firstTerm = e.getKey().firstTerminal();
					double rootFreq = totalRootFreq.get(firstTerm)[0];
					double[] freqArray = e.getValue();
					freqArray[0] = Math.log(freqArray[0]/rootFreq);
				}
			}
		}		
	}
	
	private static void convertToLogRelativeFreq(
			HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table,
			double totalFreq) {
		
		for(HashMap<TermLabel, HashMap<FragFringe, double[]>> t : table.values()) {
			for(HashMap<FragFringe, double[]> s : t.values()) {
				for(double[] d : s.values()) {
					d[0] = Math.log(d[0]/totalFreq);
				}
			}
		}
		
	}

	private void convertToLogRelativeFreqFragMap(
			HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> table,
			double totalFreq) {
		
		for(HashMap<TSNodeLabel, double[]> t : table.values()) {
			for(double[] d : t.values()) {
				d[0] = Math.log(d[0]/totalFreq);
			}
		}		
	}
	
	//firstLexFringeFragMap, firstLexTemplateFragMap,
	//firstSubFringeFragMap, firstSubTemplateFragMap,
	//firstWordLexFringeFragMap, firstWordFringesTemplatesFragMap;


	public HashMap<TSNodeLabel,double[]> getOriginalFrags(FragFringe ff, boolean firstWord) {		 
		 
		HashMap<TSNodeLabel,double[]> result = null;
		
		if (firstWord)
			result = firstWordLexFringeFragMap.get(ff); 
		else if (ff.firstTerminalIsLexical()) {
			/*
			if (nullaryArePresent && ff.firstTerminal()==nullaryNode)
				result = firstLexNullaryFringeFragMap.get(ff);
			else*/	
				result = firstLexFringeFragMap.get(ff);
		}
		else {
			/*
			if (nullaryArePresent && ff.secondTerminal()==nullaryNode)
				result = firstSubNullaryFringeFragMap.get(ff);
			else*/
				result = firstSubFringeFragMap.get(ff);
		}
		
		if (result==null) { //template
			FragFringe ffCopy = ff.cloneFringe();
			boolean isFirstLex = ffCopy.firstTerminalIsLexical();
			int lexIndex = isFirstLex ? 0 : 1;
			Label lexLabel = ffCopy.yield[lexIndex].label;
			ffCopy.yield[lexIndex] = emptyLexNode;			 			 
			HashMap<TSNodeLabel,double[]> anonymizedFrags =  
				firstWord ? firstWordFringesTemplatesFragMap.get(ffCopy) : 
					(isFirstLex ? firstLexTemplateFragMap.get(ffCopy) : firstSubTemplateFragMap.get(ffCopy));
				result = new HashMap<TSNodeLabel,double[]>();
				for(Entry<TSNodeLabel,double[]> e : anonymizedFrags.entrySet()) {
					TSNodeLabel frag = e.getKey().clone();
					double freq = e.getValue()[0];
					frag.getLeftmostLexicalNode().label = lexLabel;
					result.put(frag,new double[]{freq});
				}
		}
		 return result;
		 
	}
	
	public static void printProbTable(
			HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table, 
			HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> fringeFragMap, 
			File outputFile, boolean separateLines) {
		
		HashMap<TermLabel, HashMap<FragFringe, double[]>>
			secondKeyTable = new HashMap<TermLabel, HashMap<FragFringe, double[]>>();
		
		distributeElements(table, secondKeyTable);
		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<TermLabel, HashMap<FragFringe, double[]>> e : secondKeyTable.entrySet()) {
			for(Entry<FragFringe, double[]> f : e.getValue().entrySet()) {
				double prob = Math.exp(f.getValue()[0]);				
				int numberOfFrags = fringeFragMap.get(f.getKey()).size();
				double totalProb = prob * numberOfFrags;
				pw.println(f.getKey().toString() + "\t" + 
						prob + " x " + numberOfFrags + " = " + totalProb);
			}
			if (separateLines)
				pw.println();
		}
		pw.close();
		
		/*
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> e : table.entrySet()) {
			if (smoothing) pw.println(e.getKey());
			for(HashMap<FragFringe, double[]> subtable : e.getValue().values()) {
				for(Entry<FragFringe, double[]> f : subtable.entrySet()) {
					if (smoothing) pw.print("\t");
					pw.println(f.getKey().toString() + "\t" + 
							Utility.arrayToString(f.getValue(),'\t'));	
				}									
			}
		}		
		pw.close();
		*/
	}

	private static void distributeElements(
			HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table,
			HashMap<TermLabel, HashMap<FragFringe, double[]>> secondKeyTable) {
		
		for(HashMap<TermLabel, HashMap<FragFringe, double[]>> subtable : table.values()) {				
			for(Entry<TermLabel, HashMap<FragFringe, double[]>> e : subtable.entrySet()) {
				TermLabel secondKey = e.getKey();
				HashMap<FragFringe, double[]> secondKeySubTable = secondKeyTable.get(secondKey);
				if (secondKeySubTable==null) {
					secondKeySubTable = new HashMap<FragFringe, double[]>();
					secondKeyTable.put(secondKey, secondKeySubTable);	
				}
				HashMap<FragFringe, double[]> value = e.getValue();
				secondKeySubTable.putAll(value);
			}
		}
		
	}
	
 
	private void printFringesToFile() {
		
		if (printAllLexFragmentsToFileWithProb) {			
			
			File filteredFragmentsFile = new File(outputPath + "_lexFrags_firstLex.mrg");
			Parameters.reportLine("Printing lex fragments first lex with probs to " + filteredFragmentsFile);
			printProbTable(firstLexFringes, 
					firstLexFringeFragMap, 
					filteredFragmentsFile, true);
			
			filteredFragmentsFile = new File(outputPath + "_lexFrags_firstSub.mrg");
			Parameters.reportLine("Printing lex fragments first sub with probs to " + filteredFragmentsFile);	
			printProbTable(firstSubFringes, 
					firstSubFringeFragMap,
					filteredFragmentsFile, true);
			
			if (!topDownProb) {
				filteredFragmentsFile = new File(outputPath + "_lexFrags_firstWord.mrg");
				Parameters.reportLine("Printing lex fragments first word with probs to " + filteredFragmentsFile);	
				printProbTable(firstWordFirstLexFringes, firstWordLexFringeFragMap, 
						filteredFragmentsFile, false);
			}
		}		
		
		if (printTemplateFragsToFileWithProb) {
			File gericFragsForSmoothingFile = new File(outputPath + "_templatesFrags_firstLex.mrg");
			Parameters.reportLine("Printing template fragments first lex with probs to " + gericFragsForSmoothingFile);
			printProbTable(firstLexFringesTemplates, 
					firstLexTemplateFragMap, 
					gericFragsForSmoothingFile, true);

			gericFragsForSmoothingFile = new File(outputPath + "_templatesFrags_firstSub.mrg");
			Parameters.reportLine("Printing template fragments first sub with probs to " + gericFragsForSmoothingFile);
			printProbTable(firstSubFringesTemplates, 
					firstSubTemplateFragMap,						
					gericFragsForSmoothingFile, true);
			
			if (!topDownProb) {
				gericFragsForSmoothingFile = new File(outputPath + "_templatesFrags_firstWord.mrg");
				Parameters.reportLine("Printing template fragments first word with probs to " + gericFragsForSmoothingFile);	
				printProbTable(firstWordFirstLexFringesTemplates, 
						firstWordFringesTemplatesFragMap,
						gericFragsForSmoothingFile, false);
			}
		}
		
	}

	private void filterTestTB() {
		Parameters.reportLineFlush("Filtering TestTB");		
		if (treeLenghtLimit>0) {
			if (originalGoldFile==null)
				originalGoldFile = new File(WsjOrigianlGoldPath + testTBfile.getName());
			Scanner scan = FileUtil.getScanner(originalGoldFile);
			Iterator<TSNodeLabel> treebankIterator = testTB.iterator();
			while(treebankIterator.hasNext()) {	
				treebankIterator.next();
				TSNodeLabel t = null;
				try {
					t = new TSNodeLabel(scan.nextLine());
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (t.countLexicalNodes()>treeLenghtLimit)
					treebankIterator.remove();
			}
		}		
		//goldSentenceWords = new String[testTB.size()][];
		//Scanner goldWordsFileScanner = FileUtil.getScanner(
		//		new File(WsjFlatGoldPath + testTBfile.getName()));
		//int i=0;
		/*
		while(goldWordsFileScanner.hasNextLine()) {
			String sentence = goldWordsFileScanner.nextLine();
			String[] words = sentence.split(" ");
			if (words.length<=treeLenghtLimit)
				goldSentenceWords[i++] = words;
		}
		*/
		if (limitTestToSentenceIndex!=-1) {
			Parameters.reportLineFlush("Only 1 tree");
			TSNodeLabel t = testTB.get(limitTestToSentenceIndex);
			testTB.clear();
			testTB.add(t);
			//goldSentenceWords = new String[][]{goldSentenceWords[limitTestToSentenceIndex]};
		}
		//treebankIterator = testTB.iterator();
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
	}

	private void parseIncremental() {
		
		Parameters.reportLineFlush("Parsing incrementally...");		
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
				
		if (orderedDebug)
			orderedDebugSBArray = new StringBuilder[testTB.size()];
		
		this.parsedTB_MPD = new TSNodeLabel[testTB.size()];
		if (computeMPPtree) {
			this.parsedTB_MPP = new TSNodeLabel[testTB.size()];
		}
		if (computeIncrementalEvaluation) {
			this.partialParsedTB = new TSNodeLabel[testTB.size()][];			
		}
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
	
	
	
	private void evaluate() {
		evaluateStandard();
		if (computeIncrementalEvaluation)
			evaluateIncremental();
	}
	
	private void evaluateStandard() {
		Parameters.reportLine("Running standard evaluation");
		PrintWriter pwMPD = FileUtil.getPrintWriter(parsedMPDFile);
		PrintWriter pwMPP = null;
		if (computeMPPtree) {
			pwMPP = FileUtil.getPrintWriter(parsedMPPFile);
		}
		PrintWriter pwGold = FileUtil.getPrintWriter(goldTBFile);
		int size = parsedTB_MPD.length;
		for(int i=0; i<size; i++) {			
			TSNodeLabel goldTree = testTB.get(i);
			if (goldTree.toString().indexOf('@')!=-1) {
				goldTree = TMB.undoMarkovBinarization(goldTree);
				String topLabel = goldTree.label();
				if (topLabel.indexOf('@')!=-1) {
					topLabel = topLabel.replaceAll("@", "");
					goldTree.label = Label.getLabel(topLabel);
				}
			}
			pwGold.println(goldTree);
			String[] words = goldTree.toFlatWordArray();		
			String[] pos = goldTree.toPosArray();
			pwMPD.println(getParsedTree(parsedTB_MPD[i], words, pos));
			if (computeMPPtree) {
				pwMPP.println(getParsedTree(parsedTB_MPP[i], words, pos));
			}
		}	
		pwGold.close();
		pwMPD.close();
		if (computeMPPtree) {
			pwMPP.close();
		}
		
		evalC_MPD = new EvalC(goldTBFile, parsedMPDFile, evalC_MPD_File);
		evalC_MPD.makeEval();
		new EvalB(goldTBFile, parsedMPDFile, evalB_MPD_File);
		
		if (computeMPPtree) {
			evalC_MPP = new EvalC(goldTBFile, parsedMPPFile, evalC_MPP_File);
			evalC_MPP.makeEval();
			new EvalB(goldTBFile, parsedMPPFile, evalB_MPP_File);
		}
		
		
		
	}
	
	
	
	private TSNodeLabel getParsedTree(TSNodeLabel parsedTree, String[] words, String[] pos) {
		if (parsedTree==null) {									
			return TSNodeLabel.defaultWSJparse(words, pos, "TOP");
		}
		else {
			//t.changeLexLabelsFromStringArray(goldWords);
			if (parsedTree.toString().indexOf('@')!=-1) {
				parsedTree = TMB.undoMarkovBinarization(parsedTree);
				String topLabel = parsedTree.label();
				if (topLabel.indexOf('@')!=-1) {
					topLabel = topLabel.replaceAll("@", "");
					parsedTree.label = Label.getLabel(topLabel);
				}
			}
			ensureQuotePos(parsedTree, pos);
			return parsedTree;
		}		
	}

	private void evaluateIncremental() {
		Parameters.reportLine("Running incremental evaluation");
		
		
		EvalC.CONSTITUENTS_UNIT = 0;
		
		int maxLength = 0;
		for(TSNodeLabel t : testTB) {
			int length = t.countLexicalNodes();
			if (length>maxLength)
				maxLength = length;
		}
		//incrementalParsedMaxPredictedFileName 
		//incrementalEvalMaxPredictedFileName
		File[] fileParsedArrayMinConnected = new File[maxLength];
		File[] fileGoldArrayMinConnected = new File[maxLength];
		File[] fileParsedArrayMaxPredicted = new File[maxLength];
		File[] fileGoldArrayMaxPredicted = new File[maxLength];
		this.incrementalMinConnectedEvalC = new EvalC[maxLength];
		this.incrementalMaxPredictedEvalC = new EvalC[maxLength];
		PrintWriter[] pwParsedArrayMinConnected = new PrintWriter[maxLength];
		PrintWriter[] pwGoldArrayMinConnected = new PrintWriter[maxLength];
		//PrintWriter[] pwParsedArrayMaxPredicted = new PrintWriter[maxLength];
		//PrintWriter[] pwGoldArrayMaxPredicted = new PrintWriter[maxLength];
		PrintWriter pwParsedMinConnectedAll = FileUtil.getPrintWriter(incrementalParsedMinConnectedAllFile);
		PrintWriter pwParsedMinConnectedLast = FileUtil.getPrintWriter(incrementalParsedMinConnectedLastFile);
		PrintWriter pwGoldMinConnectedAll = FileUtil.getPrintWriter(incrementalGoldMinConnectedAllFile);
		//PrintWriter pwParsedMaxPredictedAll = FileUtil.getPrintWriter(incrementalParsedMaxPredictedAllFile);
		//PrintWriter pwGoldMaxPredictedAll = FileUtil.getPrintWriter(incrementalGoldMaxPredictedAllFile);
				
		for(int i=0; i<maxLength; i++) {
			String prefixLength = Utility.padZero(3, i+1);
			File parsedFileMinConnected =  new File(incrementalParsedMinConnectPrefixFileName + prefixLength + ".mrg");
			fileParsedArrayMinConnected[i] = parsedFileMinConnected;
			pwParsedArrayMinConnected[i] = FileUtil.getPrintWriter(parsedFileMinConnected);
			File goldFileMinConnected = new File(incrementalGoldMinConnectPrefixFileName + prefixLength + ".mrg");
			fileGoldArrayMinConnected[i] = goldFileMinConnected;
			pwGoldArrayMinConnected[i] = FileUtil.getPrintWriter(goldFileMinConnected);
			File parsedFileMaxPredicted =  new File(incrementalParsedMaxPredictedPrefixFileName + prefixLength + ".mrg");
			fileParsedArrayMaxPredicted[i] = parsedFileMaxPredicted;
			//pwParsedArrayMaxPredicted[i] = FileUtil.getPrintWriter(parsedFileMaxPredicted);
			File goldFileMaxPredicted = new File(incrementalGoldMaxPredictedPrefixFileName + prefixLength + ".mrg");
			fileGoldArrayMaxPredicted[i] = goldFileMaxPredicted;
			//pwGoldArrayMaxPredicted[i] = FileUtil.getPrintWriter(goldFileMaxPredicted);
		}
		
		predictedWordCorrect = new int[2];
		predictedLengthFreq = new HashMap<Integer,int[]>();
		
		int sentenceIndex = 0;
		for(TSNodeLabel[] partialTrees : partialParsedTB) {
			int length = partialTrees.length;
			TSNodeLabel goldTree = testTB.get(sentenceIndex);
			String goldTreeString = goldTree.toString();
			if (goldTreeString.indexOf('@')!=-1 || goldTreeString.indexOf('|')!=-1) {
				goldTree = TMB.undoMarkovBinarization(goldTree);
			}				
			//goldTree.prunePos(quotesPos);
			String[] goldWords = goldTree.toFlatWordArray();
			for(int j=0; j<length; j++) {
				int prefixLength = j+1;				
				// gold tree				
				TSNodeLabel goldTreeChopped = goldTree.getMinimalConnectedStructure(prefixLength);
				//goldTreeChopped.removeRedundantRules();
				//if (Arrays.binarySearch(quotesPos, goldTreeChopped.getRightmostTerminal().label())>=0) {
				//}
				pwGoldArrayMinConnected[j].append(goldTreeChopped.toString()).append('\n');
				//pwGoldArrayMaxPredicted[j].append(goldTree.toString()).append('\n');
				pwGoldMinConnectedAll.append(goldTreeChopped.toString()).append('\n');
				//pwGoldMaxPredictedAll.append(goldTree.toString()).append('\n');
				// parsed tree
				TSNodeLabel parsedPartialTree = partialTrees[j];				
				TSNodeLabel parsedPartialTreeMinConnected = null;
				String[] posPrefix = Arrays.copyOf(goldTree.toPosArray(), prefixLength);
				if (parsedPartialTree==null) {
					String[] wordPrefix = Arrays.copyOf(goldWords, prefixLength);					
					parsedPartialTree = TSNodeLabel.defaultWSJparse(wordPrefix, posPrefix, "TOP");					
					parsedPartialTreeMinConnected = parsedPartialTree.clone();
				}
				else {
					String[] parsedWords = parsedPartialTree.toFlatWordArray();
					String[] gp = Arrays.copyOfRange(goldWords, prefixLength, goldWords.length);
					String[] pp = Arrays.copyOfRange(parsedWords, prefixLength, parsedWords.length);
					int predictedLength = pp.length;					
					if (predictedLength>0) {
						int lcs = LongestCommonSubsequence.getLCSLength(gp, pp);
						predictedWordCorrect[0] += predictedLength;
						predictedWordCorrect[1] += lcs;
						Utility.increaseInHashMap(predictedLengthFreq, 
								new Integer(predictedLength), new int[]{1,lcs});
					}
					parsedPartialTree.removeSubSitesRecursive(".*@.*");	
					String topLabel = parsedPartialTree.label();
					if (topLabel.indexOf('@')!=-1) {
						topLabel = topLabel.replaceAll("@", "");
						parsedPartialTree.label = Label.getLabel(topLabel);
					}
					String parsedPartialTreeString = parsedPartialTree.toString();
					if (parsedPartialTreeString.indexOf('@')!=-1 || parsedPartialTreeString.indexOf('|')!=-1) {
						parsedPartialTree = TMB.undoMarkovBinarization(parsedPartialTree);						
					}
					//parsedPartialTree.removeRedundantRules();
					ensureQuotePos(parsedPartialTree, posPrefix);
					// GET MINIMAL CONNECTED STRUCTURE										
					parsedPartialTreeMinConnected = parsedPartialTree.getMinimalConnectedStructure(prefixLength);
					//parsedPartialTreeMinConnected.removeRedundantRules();
									
				}
				
				
				//parsedPartialTree.prunePos(quotesPos);
				//parsedPartialTreeMinConnected.prunePos(quotesPos);
				assert parsedPartialTreeMinConnected.countLexicalNodes()==goldTreeChopped.countLexicalNodes();
				// maybe should remove root here				
				//String parsedPartialTreeMaxPredictedString = parsedPartialTree.
				//		toString().replaceAll("@", ""); //possibly occuring in the root
				
				String parsedPartialTreeMinConnectedString = parsedPartialTreeMinConnected.toString();
				pwParsedArrayMinConnected[j].append(parsedPartialTreeMinConnectedString).append('\n');				
				//pwParsedArrayMaxPredicted[j].append(parsedPartialTreeMaxPredictedString).append('\n');
				pwParsedMinConnectedAll.append(parsedPartialTreeMinConnectedString).append('\n');
				//pwParsedMaxPredictedAll.append(parsedPartialTreeMaxPredictedString).append('\n');
				if (j==length-1) {
					pwParsedMinConnectedLast.append(parsedPartialTreeMinConnectedString).append('\n');
				}
				
			}
			sentenceIndex++;
		}	
		
		pwParsedMinConnectedAll.close();
		pwParsedMinConnectedLast.close();
		pwGoldMinConnectedAll.close();
		//pwParsedMaxPredictedAll.close();
		//pwGoldMaxPredictedAll.close();
		
		PrintWriter allMinConnPrefixPW = FileUtil.getPrintWriter(allMinConnPrefixResults);
		
		for(int i=0; i<maxLength; i++) {
			
			EvalC.CONSTITUENTS_UNIT = 0;
			
			pwParsedArrayMinConnected[i].close();
			pwGoldArrayMinConnected[i].close();
			//pwParsedArrayMaxPredicted[i].close();
			//pwGoldArrayMaxPredicted[i].close();
			
			if (i==0) { //only once for ALL
				incrementalMinConnectedAllEvalC = new EvalC(
						incrementalGoldMinConnectedAllFile, 
						incrementalParsedMinConnectedAllFile, 
						incrementalEvalMinConnectedAllFile);
				incrementalMinConnectedAllEvalC.makeEval();
				
				File evalBFile = FileUtil.changeExtension(incrementalEvalMinConnectedAllFile, "evalB");
				new EvalB(incrementalGoldMinConnectedAllFile, incrementalParsedMinConnectedAllFile, evalBFile);
			}
			
			String prefixLength = Utility.padZero(3, i+1);
			File evalCFileMinConnected = new File(incrementalEvalMinConnectPrefixFileName + prefixLength + ".evalC");
			EvalC incrementalEvalMinConnected = new EvalC(
					fileGoldArrayMinConnected[i], fileParsedArrayMinConnected[i], evalCFileMinConnected);
			incrementalEvalMinConnected.makeEval();		
			incrementalMinConnectedEvalC[i]=incrementalEvalMinConnected;
			
			//System.gc();			
			File evalBFile = new File(incrementalEvalMinConnectPrefixFileName + prefixLength + ".evalB");
			new EvalB(fileGoldArrayMinConnected[i], fileParsedArrayMinConnected[i], evalBFile);
			String[] recPrecF1 = EvalB.getAllRecallPrecisionF1(evalBFile);
			allMinConnPrefixPW.println(prefixLength + "\t" + Utility.arrayToString(recPrecF1, '\t'));
			
			/*
			EvalC.CONSTITUENTS_UNIT = 2; //yield evaluation
			
			if (i==0) { //only once for ALL
				incrementalMaxPredictedAllEvalC = new EvalC(
						incrementalGoldMaxPredictedAllFile, 
						incrementalParsedMaxPredictedAllFile, 
						incrementalEvalMaxPredictedAllFile);
				incrementalMaxPredictedAllEvalC.makeEval();				
			}
			
			File evalCFileMaxPredicted = new File(incrementalEvalMaxPredictedPrefixFileName + prefixLength + ".evalC");
			EvalC incrementalEvalMaxPredicted = new EvalC(
					fileGoldArrayMaxPredicted[i], fileParsedArrayMaxPredicted[i], evalCFileMaxPredicted);
			incrementalEvalMaxPredicted.makeEval();					
			incrementalMaxPredictedEvalC[i]=incrementalEvalMaxPredicted;
			*/
						
		}
		
		allMinConnPrefixPW.close();
				
	}


	static String[] quotesPos = new String[]{"''", "``"};
	//already sorted
	
	private void ensureQuotePos(TSNodeLabel parsedPartialTree, String[] posPrefix) {
		ArrayList<TSNodeLabel> posTree = parsedPartialTree.collectPreLexicalItems();
		int length = posPrefix.length;
		int i=0;
		for(TSNodeLabel p : posTree) {
			String goldPos = posPrefix[i++];
			if (Arrays.binarySearch(quotesPos, goldPos)>=0 && !p.label().equals(goldPos)) {
				p.relabel(goldPos);
			}
			if (i==length)
				break;
		}
		
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
				assert subSite.label==result.label;
				subSite.assignDaughters(result.daughters);
				result = nextFrag;
			}
			else {
				//SUB-DOWN
				TSNodeLabel subSite = subSitesList.removeFirst();
				assert subSite.label==nextFrag.label;
				subSite.assignDaughters(nextFrag.daughters);
				nextFrag.addSubSites(subSitesList,true);
			}
		}
		return result;
	}
	
	public static TSNodeLabel combineFrags(TSNodeLabel frag1, TSNodeLabel frag2) {
		TSNodeLabel subSite = frag1.getLeftmostSubSite();
		if (subSite == null) {
			//SUB-UP
			subSite = frag2.getLeftmostSubSite();
			assert subSite.label == frag1.label;
			subSite.assignDaughters(frag1.daughters);
			return frag2;
		}
		else {
			//SUB-DOWN			
			assert subSite.label == frag2.label;
			subSite.assignDaughters(frag2.daughters);
			return frag1;
		}
	}
	
	public static void extractSubDerivations(
			ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags,
			HashMap<TSNodeLabel, double[]> mergeProbTable) {
		
		Iterator<HashMap<TSNodeLabel, double[]>> iter = possibleFrags.iterator();
		
		IdentityHashMap<TSNodeLabel, double[]> bestDerivations = 
			new IdentityHashMap<TSNodeLabel, double[]>();
		for(Entry<TSNodeLabel, double[]> e : iter.next().entrySet()) {
			bestDerivations.put(e.getKey().clone(), new double[]{e.getValue()[0]});
		}
					
		while(iter.hasNext()) {			
			HashMap<TSNodeLabel, double[]> newFragTable = iter.next();
			IdentityHashMap<TSNodeLabel, double[]> newBestDerivations = 
				new IdentityHashMap<TSNodeLabel, double[]>();
			for(Entry<TSNodeLabel, double[]> d : bestDerivations.entrySet()) {
				TSNodeLabel partialDer = d.getKey();
				double logProb1 = d.getValue()[0];				
				for(Entry<TSNodeLabel, double[]> f : newFragTable.entrySet()) {
					TSNodeLabel frag1 = partialDer.clone();
					TSNodeLabel frag2 = f.getKey().clone();
					double logProb2 = f.getValue()[0];
					TSNodeLabel newDer = combineFrags(frag1, frag2);
					double newLogProb = logProb1 + logProb2;
					newBestDerivations.put(newDer, new double[]{newLogProb});
				}					
			}		
			bestDerivations = newBestDerivations;
		}
		
		for(Entry<TSNodeLabel, double[]> e : bestDerivations.entrySet()) {
			Utility.increaseInHashMap(mergeProbTable, e.getKey(), Math.exp(e.getValue()[0]));
		}		
		
	}
	
	synchronized void appendInLogFile(StringBuilder sb, int index) {
		if (orderedDebug) {
			if (index==orderedDebugNextIndex) {
				Parameters.logLineFlush(sb.toString());
				orderedDebugNextIndex++;
				while(orderedDebugNextIndex<testTB.size()) {
					sb = orderedDebugSBArray[orderedDebugNextIndex];
					if (sb==null) break;
					Parameters.logLineFlush(sb.toString());
					orderedDebugSBArray[orderedDebugNextIndex] = null;
					orderedDebugNextIndex++;				
				};
			}						
			else {
				orderedDebugSBArray[index] = sb;
			}
		}
		else {
			Parameters.logLineFlush(sb.toString());
		}
	}

	static class ProbBox {
		
		double viterbi; // accoring to Stolke definition of viterbi		
		//(would be better to call it inside viterbi because for non starred-states it represents the prob. of
		//the prob. of the best path from the time the state was created).
		double totalViterbi;
		ChartState previousState;
		
		
		public ProbBox(double viterbi, ChartState previousState) {
			this.viterbi = viterbi;
			//this.totalViterbi = viterbi;
			this.previousState = previousState;
		}
		
		/*
		public ProbBox(double viterbi, double totalViterbi) {
			this.viterbi = viterbi;
			this.totalViterbi = totalViterbi;
		}
		*/
		
		/*
		public boolean maxTotViterbi(double newTotViterbi, ChartState pS) {
			if (newTotViterbi>totalViterbi) {
				totalViterbi = newTotViterbi;
				//previousState = pS;
				return true;
			}
			return false;
		}
		*/
		
		public static <T,C> Entry<C, ProbBox> getMaxViterbiEntryDouble(
				HashMap<T, HashMap<C, ProbBox>> doubleTable) {
			
			double max=0;
			Entry<C, ProbBox> result = null;			
			for( Entry<T, HashMap<C, ProbBox>> e : doubleTable.entrySet()) {
				Entry<C, ProbBox> bestSubEntry = getMaxViterbiEntry(e.getValue());
				double viterbi = bestSubEntry.getValue().viterbi;
				if (result==null || viterbi>max) {
					max = viterbi;
					result = bestSubEntry;
				}
			}
			return result;
		}
		
		public static <C> Entry<C, ProbBox> getMaxViterbiEntry(
				HashMap<C, ProbBox> table) {
			
			double max=0;
			Entry<C, ProbBox> result = null;
			for(Entry<C, ProbBox> e : table.entrySet()) {
				double viterbi = e.getValue().viterbi;
				if (result==null || viterbi>max) {
					max = viterbi;
					result = e;
				}
			}
			return result;
		}
		
		public static <T,C> Entry<C, ProbBox> getMaxTotalViterbiEntryDouble(
				HashMap<T, HashMap<C, ProbBox>> doubleTable) {
			
			double max=0;
			Entry<C, ProbBox> result = null;			
			for( Entry<T, HashMap<C, ProbBox>> e : doubleTable.entrySet()) {
				Entry<C, ProbBox> bestSubEntry = getMaxTotalViterbiEntry(e.getValue());
				double viterbi = bestSubEntry.getValue().totalViterbi;
				if (result==null || viterbi>max) {
					max = viterbi;
					result = bestSubEntry;
				}
			}
			return result;
		}
		
		public static <C> Entry<C, ProbBox> getMaxTotalViterbiEntry(
				HashMap<C, ProbBox> table) {
			
			double max=0;
			Entry<C, ProbBox> result = null;
			for(Entry<C, ProbBox> e : table.entrySet()) {
				double viterbi = e.getValue().totalViterbi;
				if (result==null || viterbi>max) {
					max = viterbi;
					result = e;
				}
			}
			return result;
		}
		
		/**
		 * 
		 * @return the ProbBox which is introduced or modified as the result of this operation 
		 * (null if the the table is not modified) 
		 */

		public static <T> boolean addProbStateOrMaxViterbiDouble(
				HashMap<T, HashMap<ChartState, ProbBox>> table,
				T t, ChartState c, double newViterbi, ChartState newPreviousState) {
			
			HashMap<ChartState, ProbBox> value = table.get(t);
			if (value==null) {
				value = new HashMap<ChartState, ProbBox>();
				table.put(t,value);
				ProbBox newPb = new ProbBox(newViterbi, newPreviousState);
				value.put(c, newPb);
				c.probBox = newPb;
				return true;
			}
 
			return addProbStateOrMaxViterbi(value, c, newViterbi, newPreviousState);
		}

		
		
		/**
		 * 
		 * @return the ProbBox which is introduced or modified as the result of this operation 
		 * (null if the the table is not modified) 
		 */

		private static boolean addProbStateOrMaxViterbi(
				HashMap<ChartState, ProbBox> table,
				ChartState c, double newViterbi, ChartState newPreviousState) {
			
			ProbBox probPresent = table.get(c);
			if (probPresent==null) {
				ProbBox pb = new ProbBox(newViterbi, newPreviousState);
				table.put(c, pb);
				c.probBox = pb;
				return true;
			}
			if (probPresent.viterbi<newViterbi) {
				probPresent.viterbi = newViterbi;
				probPresent.previousState = newPreviousState;
				c.probBox = probPresent;
				return true;					
			}	
			c.probBox = probPresent;
			return false;
		}		 

		
		/*
		public static <T> boolean addProbStateOrMaxViterbiDouble(
				HashMap<T, HashMap<ChartState, ProbBox>> table,
				T t, ChartState c, double newViterbi, double newTotViterbi) {
			
			HashMap<ChartState, ProbBox> value = table.get(t);
			if (value==null) {
				value = new HashMap<ChartState, ProbBox>();
				table.put(t,value);
				ProbBox newPb = new ProbBox(newViterbi, newTotViterbi);
				value.put(c, newPb);
				c.probBox = newPb;
				return true;
			}
			return addProbStateOrMaxViterbi(value, c, newViterbi, newTotViterbi);
		}
		
		private static boolean addProbStateOrMaxViterbi(
				HashMap<ChartState, ProbBox> table,
				ChartState c, double newViterbi, double newTotViterbi) {
			
			ProbBox probPresent = table.get(c);
			if (probPresent==null) {
				ProbBox pb = new ProbBox(newViterbi, newTotViterbi);
				table.put(c, pb);
				c.probBox = pb;
				return true;
			}
			boolean modifid = false;
			if (probPresent.viterbi<newViterbi) {
				probPresent.viterbi = newViterbi;
			}	
			if (probPresent.totalViterbi < newTotViterbi) {
				probPresent.totalViterbi = newTotViterbi;
			}	
			return modifid;
		}
		*/
		
	}



	protected class ChartParserThread extends Thread {
		
		int sentenceIndex;	
		TSNodeLabel goldTestTree;
				 	
		ArrayList<Label> wordLabels;
		TermLabel[] wordTermLabels;
		HashSet<TermLabel> wordTermLabelsSet;
		int sentenceLength;
		
		Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>> 
			wordFragFringeFirstLex, 
			wordFragFringeFirstSub;
		
		/*
		Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>>
			wordNullaryFragFringeFirstLex,
			wordNullaryFragFringeFirstSub,
			wordNullaryFragFringeFirstSubNoLex;
		*/
			
				
		Vector<ChartColumn> chart;
		
		int[][] fringeCounterFirstLexSubBin;
		int[] totalFringesLexSub, totalFringesSmoothingLexSub, removedFringesLexSub;
		boolean parsingSuccess, potentiallyReachingTop, actuallyReachedTop;
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
		int[] countSubDownSubUpSentence;
		StringBuilder debugSB = new StringBuilder();
		
		public void run() {
			//ArrayList<Label> sentenceWords = null;
			int[] i = {0};
			//while ( (sentenceWords = getNextSentence(i)) != null) {
			while ( (goldTestTree = getNextTree(i)) != null) {
				sentenceIndex = i[0];
				ArrayList<Label> lex = goldTestTree.collectLexicalLabels();
				Utility.removeAll(lex, nullaryLabel);
				initVariables(lex);
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
			
			chart = new Vector<ChartColumn> (sentenceLength+1); //termstate
			
			wordFragFringeFirstLex = //indexed on root
					new Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>>(sentenceLength);
			wordFragFringeFirstSub = //indexed on first term
					new Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>>(sentenceLength);
			
			/*
			if (nullaryArePresent) {
				wordNullaryFragFringeFirstLex = new Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>>();
				wordNullaryFragFringeFirstSub = new Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>>();
				wordNullaryFragFringeFirstSubNoLex = new Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>>();
			}
			*/			
			
			fringeCounterFirstLexSubBin = new int[2][sentenceLength];
			removedFringesLexSub = new int[2];
			totalFringesLexSub = new int[2];
			totalFringesSmoothingLexSub = new int[2];
			this.parsingSuccess = false;
			this.potentiallyReachingTop = false;
			this.actuallyReachedTop = false;
			this.finished = false;			
			this.countSubDownSubUpSentence = new int[2];
		}
		
		private void parse() {					
			treeTimer.start();
						
			initChart();
			
			ChartColumn startChartColumn = chart.get(0);
			startChartColumn.performStart();
			
			for(int i=0; i<=sentenceLength; i++) {
				currentWordTimer.start();
				ChartColumn c = chart.get(i);
				if (i==0)
					c.performScan();
				else if (i<sentenceLength)
					c.completeSubDownSubUpScan();
				else
					c.completeAndTerminate();
				c.countFragFringes();
				currentWordTimer.stop();
				wordTimes[i] = currentWordTimer.readTotalTimeSecFormat();
				if (isInterrupted()) break;
			}
			
			treeTimer.stop();
			
			TSNodeLabel MPDtree = null, MPPtree = null;						
			ArrayList<ChartState> derivationPath = null;
			ArrayList<TSNodeLabel> derivation = null;
			double[] viterbiLogProb = new double[]{-Double.MAX_VALUE};
			ArrayList<Integer> numberOfPossibleFrags = new ArrayList<Integer>();
			int treeSubDerivations = 1;
			ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags = new ArrayList<HashMap<TSNodeLabel, double[]>>();			
			//HashMap<HashSet<ChartState>,double[]> derivationPaths = null;
			//double parseProb = 0;
			
			//parsingSuccess = false;
			
			if (parsingSuccess) {					
				//derivationPath = retrieveRandomBestViterbiPath(viterbiLogProb);
				derivationPath = retrieveRandomBestViterbiPathBackTrace(viterbiLogProb);
				//assert derivationPathOld.equals(derivationPath);
				derivation = retrieveFragDerivation(derivationPath, numberOfPossibleFrags, possibleFrags, viterbiLogProb);
				treeSubDerivations =  Utility.product(numberOfPossibleFrags);				
				MPDtree = combineDerivation(derivation);
				if (computeMPPtree) {
					double[] kBestViterbiLogProb = new double[kBest];
					HashMap<TSNodeLabel, double[]> parseTreeProbTable = new HashMap<TSNodeLabel, double[]>(); 
					ArrayList<ArrayList<ChartState>> derivationMultiPath 
						= retrieveKbestViterbiPath(viterbiLogProb, kBest);
					int i=0;
					for(ArrayList<ChartState> derPath_i : derivationMultiPath) {
						ArrayList<Integer> numFrags_i = new ArrayList<Integer>();
						ArrayList<HashMap<TSNodeLabel, double[]>> fragsDer_i = 
							new ArrayList<HashMap<TSNodeLabel, double[]>>();
						double[] vitLogProb_i = new double[1];
						retrieveFragDerivation(derPath_i, numFrags_i, fragsDer_i, vitLogProb_i);
						extractSubDerivations(fragsDer_i, parseTreeProbTable);
						kBestViterbiLogProb[i++] = vitLogProb_i[0];
					}					
					Utility.getMaxKey(parseTreeProbTable);
				}
				countFirstLexSubOperations(derivationPath);
				//derivationPaths = retrieveAllViterbiPaths();
				//countFirstLexSubOperations(derivationPaths.keySet());
				//Entry<TSNodeLabel, double[]> bestParseProb = retrieveBestFragParse(derivationPaths);		
				//parsedTree = bestParseProb.getKey();
				//parseProb = bestParseProb.getValue()[0];
				if (MPDtree.label==TOPlabel)
					actuallyReachedTop=true;
				/*
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
				*/
			}
			
			TSNodeLabel[] incrementalTrees = null;
			if (computeIncrementalEvaluation) {
				incrementalTrees = new TSNodeLabel[sentenceLength];
				for(int i=0; i<sentenceLength; i++) {
					double[] viterbiLogProbPartial = new double[]{-Double.MAX_VALUE};
					ArrayList<ChartState> partialPath = 
						retrieveRandomViterbiPartialPath(i+1, viterbiLogProbPartial);
					if (partialPath==null)
						incrementalTrees[i] = null;
					else {						
						ArrayList<TSNodeLabel> partialDer = retrieveFragDerivation(partialPath, null, null, viterbiLogProbPartial);
						TSNodeLabel tree = combineDerivation(partialDer);
						incrementalTrees[i] = tree;
					}
				}
			}
			
			if (debug)
				printDebug(MPDtree, derivation, derivationPath, 
						viterbiLogProb[0], numberOfPossibleFrags, possibleFrags);
				//printDebug(parsedTree, null, null, 
				//		parseProb, null, null);
			
			updateMainResults(MPDtree, MPPtree, incrementalTrees, treeSubDerivations);			
			
		}
		
		

		private void updateMainResults(TSNodeLabel MPDtree, TSNodeLabel MPPtree,
				TSNodeLabel[] partialDerivations, int treeSubDerivations) {
			synchronized(progress) {
				if(Thread.interrupted() && !finished)
					interruptedTrees++;
				totalSubDerivations += treeSubDerivations;
				if (treeSubDerivations>1)
					ambiguousViterbiDerivations++;
				if (computeIncrementalEvaluation) {
					partialParsedTB[sentenceIndex] = partialDerivations;
				}
				if (parsingSuccess) {
					parsedTrees++;
					Utility.arrayIntPlus(countSubDownSubUpSentence,countSubDownSubUpTotal);
					if (actuallyReachedTop)
						actuallyReachedTopTrees++;
					if (potentiallyReachingTop) 
						potentiallyReachingTopTrees++;					
				}							
				parsedTB_MPD[sentenceIndex] = MPDtree;
				if (computeMPPtree) {
					parsedTB_MPP[sentenceIndex] = MPPtree;
				}
				progress.next();	
				
			}
			
		}

		private void initChart() {
			SimpleTimer t = new SimpleTimer(true);
			int i=0;
			for(Label w : wordLabels) {				
				TermLabel wordTermLabel = TermLabel.getTermLabel(w, true);
				populateWordIndexFragFringesFirstLexSub(wordTermLabel, i);				
				chart.add(new ChartColumn(wordTermLabel, i));
				i++;
			}			
			
			/*
			if (nullaryArePresent) {
				HashMap<TermLabel,HashMap<FragFringe,double[]>> wordFFlexNullary = 
					Utility.deepHashMapDoubleCloneArray(firstLexNullaryFringes);				
				wordNullaryFragFringeFirstLex.add(wordFFlexNullary);					
			}
			*/
			
			chart.add(new ChartColumn(null, i));
			initChartTime = t.checkEllapsedTimeSecFormat();
		}


		private void populateWordIndexFragFringesFirstLexSub(TermLabel w, int index) {	

			HashMap<TermLabel,HashMap<FragFringe,double[]>> wordFFlex = null;
			HashMap<TermLabel,HashMap<FragFringe,double[]>> wordFFsub = null;			
			
			if (index>0) {
				wordFFlex = Utility.deepHashMapDoubleCloneArray(firstLexFringes.get(w));
				wordFFsub = Utility.deepHashMapDoubleCloneArray(firstSubFringes.get(w));
			}
			else {				 
				wordFFlex = topDownProb ?
						Utility.deepHashMapDoubleCloneArray(firstLexFringes.get(w)) :
						Utility.deepHashMapDoubleCloneArray(firstWordFirstLexFringes.get(w));
				wordFFsub = new HashMap<TermLabel,HashMap<FragFringe,double[]>>();
			}
			
			wordFragFringeFirstLex.add(wordFFlex);		
			wordFragFringeFirstSub.add(wordFFsub);
			
			/*
			if (nullaryArePresent) {
				HashMap<TermLabel,HashMap<FragFringe,double[]>> wordFFlexNullary = 
					Utility.deepHashMapDoubleCloneArray(firstLexNullaryFringes);				
				wordNullaryFragFringeFirstLex.add(wordFFlexNullary);
				
				HashMap<TermLabel,HashMap<FragFringe,double[]>> wordFFSubNullary =
					Utility.deepHashMapDoubleCloneArray(firstSubNullaryFringes.get(w)); 				
				wordNullaryFragFringeFirstSub.add(wordFFSubNullary); 
				
				HashMap<TermLabel,HashMap<FragFringe,double[]>> wordFFSubNullaryNoLex =
					Utility.deepHashMapDoubleCloneArray(firstSubNullaryFringes.get(null));
				wordNullaryFragFringeFirstSubNoLex.add(wordFFSubNullaryNoLex); 
					
			}
			*/									
			
			 
			if (templateSmoothing) {
				// extending this to nullary fringes?
				for(TermLabel pos : lexPosFreqTable.get(w).keySet()) {
					//first lex
					HashMap<TermLabel, HashMap<FragFringe,double[]>> posFringes = firstLexFringesTemplates.get(pos);
					if (posFringes!=null) {									
						for(Entry<TermLabel, HashMap<FragFringe,double[]>> e : posFringes.entrySet()) {
							TermLabel root = e.getKey();																			
							HashMap<FragFringe,double[]> presentSet = wordFFlex.get(root);							
							if (presentSet==null) {
								presentSet = new HashMap<FragFringe,double[]>();
								wordFFlex.put(root, presentSet);
							}
							for(Entry<FragFringe,double[]> f : e.getValue().entrySet()) {
								FragFringe ff = f.getKey();
								FragFringe ffCopy = ff.cloneFringe();
								ffCopy.setInYield(0, w);			
								if (!presentSet.containsKey(ffCopy)) {	
									presentSet.put(ffCopy,new double[]{f.getValue()[0]});
									totalFringesSmoothingLexSub[0]++;
								}
							}
						}						
					}
					//firstSub
					posFringes = firstSubFringesTemplates.get(pos);
					if (posFringes!=null) {
						for(Entry<TermLabel, HashMap<FragFringe,double[]>> e : posFringes.entrySet()) {		
							TermLabel subSite = e.getKey();
							HashMap<FragFringe,double[]> presentSet = wordFFsub.get(subSite);							
							if (presentSet==null) {
								presentSet = new HashMap<FragFringe,double[]>();
								wordFFsub.put(subSite, presentSet);
							}
							for(Entry<FragFringe,double[]> f : e.getValue().entrySet()) {
								FragFringe ff = f.getKey();								
								FragFringe ffCopy = ff.cloneFringe();
								ffCopy.setInYield(1, w);
								if (!presentSet.containsKey(ffCopy)) {
									presentSet.put(ffCopy,new double[]{f.getValue()[0]});
									totalFringesSmoothingLexSub[1]++;
								}
							}
						}
					}					
				}
			}
			
			totalFringesLexSub[0] += Utility.countTotalDouble(wordFFlex);
			totalFringesLexSub[1] += Utility.countTotalDouble(wordFFsub);
			fringeCounterFirstLexSubBin[0][index] = wordFFlex.size();
			fringeCounterFirstLexSubBin[1][index] = wordFFsub.size();
			
			if (removeFringesNonCompatibleWithSentence) {					
				Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>> wordFFlexSub =
					new Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>>(2); 
				wordFFlexSub.add(wordFFlex);
				wordFFlexSub.add(wordFFsub);
				int i=0;
				for(HashMap<TermLabel,HashMap<FragFringe,double[]>> tab : wordFFlexSub) {
					int totalRemoved = 0;
					for(HashMap<FragFringe,double[]> subtab : tab.values()) {
						Iterator<Entry<FragFringe, double[]>> it = subtab.entrySet().iterator();
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
				// need to modify this for nullary case
			}			
			

		}
		
		private boolean isCompatibleWithFringe(FragFringe ff, int index) {						 
			TermLabel[] fringe = ff.yield;
			return FringeCompatibility.isCompatibleLex(fringe, wordTermLabels, index);
			//return FringeCompatibility.isCompatible(fringe, wordTermLabels, index, posLexTable);
		}

		public void toStringTexChartHorizontal(PrintWriter pw, 
				Vector<ArrayList<ChartState>> derPaths, boolean onlyDerivation, boolean normalProb) {

			pw.print("\\begin{tabular}{|c|" + Utility.repeat("ll|",sentenceLength) + "l|}\n");
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
						toStringTexTable(chart.get(i).scanStatesSet, derPaths, onlyDerivation, normalProb) + 
						"}");
				pw.print(" & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SUB-DOWN} &\n");
			for(i=0; i<sentenceLength; i++) {
				ChartColumn cc = chart.get(i);
				pw.print(toStringTexTable(cc.subDownFirstStatesSet, derPaths, onlyDerivation, normalProb));
				pw.print(" & \n");
				pw.print(toStringFragTex(cc.subDownSecondStateSet, derPaths, i, true, onlyDerivation, normalProb));
				pw.print(" & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SUB-UP} &\n");
			for(i=0; i<=sentenceLength; i++) {
				ChartColumn cc = chart.get(i);
				if (i<sentenceLength) {
					pw.print(toStringTexTable(chart.get(i).subUpFirstStatesSet, derPaths, onlyDerivation, normalProb));
					pw.print(" & \n");
					pw.print(toStringFragTex(cc.subUpSecondFragFringeSet, derPaths, i, false, onlyDerivation, normalProb));
					pw.print(" & \n");
				}
				else {
					pw.print("\\multicolumn{1}{l|}{ " +
							toStringTexTable(chart.get(i).subUpFirstStatesSet, derPaths, onlyDerivation, normalProb) + 
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
						toStringTexTable(chart.get(i).completeStatesSet, derPaths, onlyDerivation, normalProb) + 
						"}");
				pw.print(i==sentenceLength ? "\n" : " & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\end{tabular}\n");
		}

		
		public void toStringTexChartVertical(PrintWriter pw, 
				Vector<ArrayList<ChartState>> derPaths, boolean onlyDerivation,
				boolean normalProb) {
			
			pw.print("\\begin{tabular}{|c|ll|}\n");
			
			for(int i=0; i<=sentenceLength; i++) {
				String word = i<sentenceLength ? wordLabels.get(i).toString() : "STOP";
				ChartColumn cc = chart.get(i);
				pw.print("\\hline\n");
				
				pw.print("& \\multicolumn{2}{c|}{ " +
						"\\begin{tabular}{c} " +
						"(" + i + ") \\\\ " +
						word +
						"\\end{tabular} }" );
				pw.print("\\\\\n");
				pw.print("\\hline\n");
				
				pw.print("SCAN &\n");
				pw.print("\\multicolumn{2}{l|}{ " +
						toStringTexTable(chart.get(i).scanStatesSet, derPaths, onlyDerivation, normalProb) + 
						"}\\\\\n");				
				
				pw.print("\\hline\n");
				pw.print("SUB-DOWN &\n");
				pw.print(toStringTexTable(cc.subDownFirstStatesSet, derPaths, onlyDerivation, normalProb));
				pw.print(" & \n");
				pw.print(toStringFragTex(cc.subDownSecondStateSet, derPaths, i, true, onlyDerivation, normalProb));
				pw.print("\\\\\n");
				
				pw.print("\\hline\n");
				pw.print("SUB-UP &\n");
				pw.print(toStringTexTable(chart.get(i).subUpFirstStatesSet, derPaths, onlyDerivation, normalProb));
				pw.print(" & \n");
				pw.print(toStringFragTex(cc.subUpSecondFragFringeSet, derPaths, i, false, onlyDerivation, normalProb));
				pw.print("\\\\\n");
				
				pw.print("\\hline\n");
				pw.print("COMPLETE &\n");
				pw.print("\\multicolumn{2}{l|}{ " +
						toStringTexTable(chart.get(i).completeStatesSet, derPaths, onlyDerivation, normalProb) + 
						"}\\\\\n");				
				pw.print("\\hline\n\n");
				
				pw.print("\\multicolumn{2}{c}{}\\\\");
			}			
			
			pw.print("\\end{tabular}\n");
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
		
		private String toStringTexMultiple(ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags) {
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{l}\n");
			Iterator<HashMap<TSNodeLabel, double[]>> iter = possibleFrags.iterator();
			while(iter.hasNext()) {
				HashMap<TSNodeLabel, double[]> next = iter.next();
				sb.append("\t\\begin{tabular}{" + Utility.repeat("l", next.size()) + "}\n");
				Iterator<Entry<TSNodeLabel, double[]>> iter2 = next.entrySet().iterator();
				while(iter2.hasNext()) {
					Entry<TSNodeLabel, double[]> e = iter2.next(); 
					sb.append("\t(" + e.getValue()[0] + ") " + e.getKey().toStringTex(false,true));
					if (iter2.hasNext())
						sb.append(" &");
					sb.append(" \n");
				}
				sb.append("\t\\end{tabular}\\\\\n");
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}
				
		static final String highlight = "\\rowcolor{lemon}";		
		
		public String toStringTexTable(
				HashMap<TermLabel,HashMap<ChartState,ProbBox>> table,
				Vector<ArrayList<ChartState>> derPaths, boolean onlyDerivation,
				boolean normalProb) {
			
			int derPathSize = derPaths==null ? 0 : derPaths.size();
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{llll}\n");		
			for(HashMap<ChartState,ProbBox> csSet : table.values()) {			
				for(Entry<ChartState,ProbBox> csProb : csSet.entrySet()) {
					ChartState cs = csProb.getKey();
					int derPathIndex = -1;
					if (derPaths!=null) {
						int i=0;
						for(ArrayList<ChartState> dp : derPaths) {
							if (dp.contains(cs)) {
								derPathIndex = i;
								break;
							}
							i++;
						}
					}
					if (derPathIndex!=-1) {
						String initString = (!onlyDerivation || derPathSize>1) && derPathIndex==0 ? "\t" + highlight : "\t";
						ProbBox pb = csProb.getValue();
						double viterbi = normalProb ? Math.exp(pb.viterbi) : pb.viterbi;
						double totVit = normalProb ? Math.exp(pb.totalViterbi) : pb.totalViterbi;
						String vitString = Float.toString((float)viterbi);
						String totVitString = Float.toString((float)totVit);
						sb.append(initString + cs.toStringTex() + " [" + vitString + ", " + totVitString + "] " + "\\\\\n");
					}
					else if (!onlyDerivation) {
						ProbBox pb = csProb.getValue();
						double viterbi = normalProb ? Math.exp(pb.viterbi) : pb.viterbi;
						double totVit = normalProb ? Math.exp(pb.totalViterbi) : pb.totalViterbi;
						String vitString = Float.toString((float)viterbi);
						String totVitString = Float.toString((float)totVit);
						sb.append("\t" + cs.toStringTex() + " [" + vitString + ", " + totVitString + "] " + "\\\\\n");
					}					
				}
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}
		
		public String toStringFragTex(
				HashMap<TermLabel, HashMap<FragFringe, double[]>> table,
				Vector<ArrayList<ChartState>> derPaths, int wordIndex, boolean subDown,
				boolean onlyDerivation, boolean normalProb) {
			
			int derPathSize = derPaths==null ? 0 : derPaths.size();
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{lll}\n");							
			for(HashMap<FragFringe, double[]> set : table.values()) {
				for(Entry<FragFringe, double[]> e : set.entrySet()) {
					FragFringe ff = e.getKey();
					ChartState cs = chart.get(wordIndex).new ChartState(ff, subDown ? wordIndex : -1);
					
					if (!subDown)
						cs.dotIndex++;
					int derPathIndex = -1;
					if (derPaths!=null) {
						int i=0;
						for(ArrayList<ChartState> dp : derPaths) {
							if (dp.contains(cs)) {
								derPathIndex = i;
								break;
							}
							i++;
						}
					}
					if (derPathIndex!=-1) {
						String initString = (!onlyDerivation || derPathSize>1) && derPathIndex==0 ? "\t" + highlight : "\t";
						double vit = normalProb ? Math.exp(e.getValue()[0]) : e.getValue()[0];
						String vitString = Float.toString((float)vit);
						sb.append(initString + ff.toStringTex() + " [" + vitString + "]" + "\\\\\n");
					}
					else if (!onlyDerivation) {
						double vit = normalProb ? Math.exp(e.getValue()[0]) : e.getValue()[0];
						String vitString = Float.toString((float)vit);
						sb.append("\t" + ff.toStringTex() + " [" + vitString + "]" + "\\\\\n");
					}
					
				}
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}

		private ArrayList<TSNodeLabel> retrieveFragDerivation(
				ArrayList<ChartState> path, 
				ArrayList<Integer> numberOfFragDerivations, 
				ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags,
				double[] vitCheck) {
			
			boolean check = vitCheck!=null;
			Vector<ArrayList<FragFringe>> derivationFringessPath = getFragsFromPathInOrder(path);			
			ArrayList<TSNodeLabel> derivationFrags = new ArrayList<TSNodeLabel>();
			double checkLogProb = 0;
			boolean firstWord = !topDownProb;
			for(ArrayList<FragFringe> ffcolumn : derivationFringessPath) {
				for(FragFringe ff : ffcolumn) {
					if (ff==null) 
						continue;				
					HashMap<TSNodeLabel, double[]> ftable = getOriginalFrags(ff, firstWord);
					if (numberOfFragDerivations!=null)
						numberOfFragDerivations.add(ftable.size());
					if (possibleFrags!=null)
						possibleFrags.add(ftable);
					//vitCheck
					Entry<TSNodeLabel, double[]> maxEntry = Utility.getMaxEntry(ftable);
					derivationFrags.add(maxEntry.getKey());
					if (check)
						checkLogProb += maxEntry.getValue()[0];
					firstWord = false;
				}
			}			
			if (check && Math.abs(vitCheck[0]-checkLogProb)>0.00000001) {
				System.err.println("Wrong viterbi chart|recomputed: " + vitCheck[0] + "|" + checkLogProb);
				System.err.println("\tDer: " + derivationFrags);
			}				
			return derivationFrags;
		}
		
		/*
		private Entry<TSNodeLabel, double[]> retrieveBestFragParse(
				HashMap<HashSet<ChartState>,double[]> paths) { //al with same prob
			
			HashMap<TSNodeLabel,double[]> parseTreeTable = new HashMap<TSNodeLabel,double[]>(); 
			for(Entry<HashSet<ChartState>, double[]> e : paths.entrySet()) {
				HashSet<ChartState> p = e.getKey();
				double prob = e.getValue()[0];
				ArrayList<TSNodeLabel> der = retrieveFragDerivation(p,null,null);
				TSNodeLabel parseTree = combineDerivation(der);
				Utility.increaseInHashMap(parseTreeTable, parseTree, prob);
			}						
			return Utility.getMaxEntry(parseTreeTable);
		}
		*/
		
		private Vector<ArrayList<FragFringe>> getFragsFromPathInOrder(ArrayList<ChartState> path) {
			Vector<ArrayList<FragFringe>> frags = new Vector<ArrayList<FragFringe>>(sentenceLength+1);
			for(int i=0; i<=sentenceLength; i++) {
				frags.add(new ArrayList<FragFringe>());
			}
			for(ChartState cs : path) {
				if (cs.isFirstStateWithCurrentWord()) {
					int index = cs.wordIndex();		
					frags.get(index).add(cs.fragFringe);
				}
			}
			/*
			if (nullaryArePresent) {
				for(int i=0; i<=sentenceLength; i++) {
					ArrayList<FragFringe> columnFrag = frags.get(i);
					if (columnFrag.size()>1) {
						ArrayList<FragFringe> columnFragNullSubDown = new ArrayList<FragFringe>();
						ArrayList<FragFringe> columnFragNullSubUp = new ArrayList<FragFringe>();
						ArrayList<FragFringe> columnFragStandard = new ArrayList<FragFringe>();
						for(FragFringe ff : columnFrag) {
							if (ff.isNullaryFragFringe()) {
								if (ff.firstTerminal()==nullaryNode)
									columnFragNullSubDown.add(ff);
								else
									columnFragNullSubUp.add(ff);
							}
							else
								columnFragStandard.add(ff);
						}
						columnFrag.clear();
						columnFrag.addAll(columnFragNullSubUp);
						columnFrag.addAll(columnFragNullSubDown);
						columnFrag.addAll(columnFragStandard);
					}
				}
			}
			*/
			return frags;
		}
		
		/*
		private HashMap<HashSet<ChartState>,double[]> retrieveAllViterbiPaths() {
			HashMap<HashSet<ChartState>,double[]> result = new HashMap<HashSet<ChartState>,double[]>();
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<TermLabel, HashMap<ChartState,double[]>> endStates = lastColumn.subUpFirstStatesSet;
			HashMap<ChartState, double[]> finalStateEntry = Utility.getAllMaxEntryDouble(endStates);
			for(Entry<ChartState, double[]> e : finalStateEntry.entrySet()) {
				ChartState cs = e.getKey();
				double prob = e.getValue()[0];
				HashSet<ChartState> path = new HashSet<ChartState>();
				addViterbiBestPathRecursive(path,cs,false);
				result.put(path,new double[]{prob});
			}
			return result;
		}
		*/
		
		private ArrayList<ChartState> retrieveRandomViterbiPartialPath(int prefixLength, double[] vit) {
			//ChartColumn nextChartColumn = chart.get(prefixLength); //after last was scanned
			ChartColumn prefixLastColum = chart.get(prefixLength-1); //before last was scanned
			// look only in scan states
			// assuming look ahead 1 (all states in the scanStates have the current word to be scanned)				
			Entry<ChartState, ProbBox> bestScanEntryState = 
					ProbBox.getMaxTotalViterbiEntryDouble(prefixLastColum.scanStatesSet);			
			if (bestScanEntryState==null)
				return null;			
			if (vit!=null)
				vit[0] = bestScanEntryState.getValue().totalViterbi;
			ArrayList<ChartState> path = new ArrayList<ChartState>();
			addViterbiBestPathRecursiveBackTrace(path,bestScanEntryState.getKey(),true);
			//addViterbiBestPathRecursive(path,bestScanEntryState.getKey(),true);
			return path;
			
		}
		
		
		private HashSet<ChartState> retrieveRandomBestViterbiPath(double[] viterbiLogProb) {
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<ChartState, ProbBox> topStateSet = lastColumn.subUpFirstStatesSet.get(TOPnode);
			Entry<ChartState, ProbBox> topStateEntry = null;
			if (topStateSet!=null)
				topStateEntry = ProbBox.getMaxViterbiEntry(topStateSet);
			
			HashMap<TermLabel, HashMap<ChartState,ProbBox>> endStates = lastColumn.subUpFirstStatesSet;
			Entry<ChartState, ProbBox> nonTopStateEntry = ProbBox.getMaxViterbiEntryDouble(endStates);
			
			if (topStateEntry==null || nonTopStateEntry.getValue().viterbi > topStateEntry.getValue().viterbi)
				topStateEntry = nonTopStateEntry;
			// using a non top anyway
				
			viterbiLogProb[0] = topStateEntry.getValue().viterbi;
			HashSet<ChartState> path = new HashSet<ChartState>();
			addViterbiBestPathRecursive(path,topStateEntry.getKey(), false);
			return path;
		}
		
		private void addViterbiBestPathRecursive(HashSet<ChartState> path, 
				ChartState cs, boolean partialDerivation) {			
			path.add(cs);
			Duet<ChartState, ChartState> parentPairs = cs.getViterbiBestParentStates(partialDerivation);
			if (parentPairs==null)
				return;
			cs = parentPairs.getFirst();
			boolean firstNull = true;
			if (cs!=null) {
				firstNull = false;
				addViterbiBestPathRecursive(path,cs, partialDerivation);
			}
			cs = parentPairs.getSecond();
			if (cs!=null) {				 
				if (!firstNull && partialDerivation)
					partialDerivation = false;
				addViterbiBestPathRecursive(path,cs, partialDerivation);					
			}
		}
		


		final Comparator<ChartState> ChartStateDecreasingTotVitComparator =
			new Comparator<ChartState>() {
			@Override
			public int compare(ChartState o1, ChartState o2) {
				return Double.compare(o2.probBox.totalViterbi, o1.probBox.totalViterbi);
			}
		};

		
		private ArrayList<ArrayList<ChartState>> retrieveKbestViterbiPath(double[] viterbiLogProb, int k) {			
			
			ChartColumn lastColumn = chart.get(sentenceLength);
			
			TreeSet<ChartState> allEndStates = new TreeSet<ChartState>(ChartStateDecreasingTotVitComparator);
			HashMap<TermLabel, HashMap<ChartState,ProbBox>> endStates = lastColumn.subUpFirstStatesSet;
			for(HashMap<ChartState,ProbBox> es : endStates.values()) {
				allEndStates.addAll(es.keySet());
			}
			
			ArrayList<ArrayList<ChartState>> result = new ArrayList<ArrayList<ChartState>>(k);
									
			for(int i=0; i<k; i++) {
				
				/*
				ChartState bestState = allEndStates.pollFirst();
				HashSet<ChartState> bestPath = new HashSet<ChartState>();
				addViterbiBestPathRecursiveBackTrace(bestPath,bestState,false);
				result.add(bestPath);
				viterbiLogProb[i] = bestState.probBox.totalViterbi;
				bestState.probBox.initKBestSubDerivations();
				
				HashSet<ChartState> path = new HashSet<ChartState>();
				viterbiLogProb[i] = 
					addNextBestViterbiPathRecursive(path, allEndStates, false);
				if (path.isEmpty())
					break;
				result.add(path);
				*/
			}
			
			return result;			
		}
		
		private double addNextBestViterbiPathRecursive(HashSet<ChartState> path,
				HashSet<ChartState> allEndStates, boolean partialDerivation) {
			
			/*
			path.add(allEndStates);
			Duet<ChartState, ChartState> parentPairs = allEndStates.getViterbiBestParentStates(partialDerivation);
			if (parentPairs==null)
				return  0d;
			allEndStates = parentPairs.getFirst();
			boolean firstNull = true;
			if (allEndStates!=null) {
				firstNull = false;
				addViterbiBestPathRecursive(path,allEndStates, partialDerivation);
			}
			allEndStates = parentPairs.getSecond();
			if (allEndStates!=null) {				 
				if (!firstNull && partialDerivation)
					partialDerivation = false;
				addViterbiBestPathRecursive(path,allEndStates, partialDerivation);					
			}
			*/
			return 0d;
		}

		private ArrayList<ChartState> retrieveRandomBestViterbiPathBackTrace(double[] viterbiLogProb) {
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<ChartState, ProbBox> topStateSet = lastColumn.subUpFirstStatesSet.get(TOPnode);
			Entry<ChartState, ProbBox> topStateEntry = null;
			if (topStateSet!=null)
				topStateEntry = ProbBox.getMaxViterbiEntry(topStateSet);
			
			HashMap<TermLabel, HashMap<ChartState,ProbBox>> endStates = lastColumn.subUpFirstStatesSet;
			Entry<ChartState, ProbBox> nonTopStateEntry = ProbBox.getMaxViterbiEntryDouble(endStates);
			
			if (topStateEntry==null || nonTopStateEntry.getValue().viterbi > topStateEntry.getValue().viterbi)
				topStateEntry = nonTopStateEntry;
			// using a non top anyway
				
			viterbiLogProb[0] = topStateEntry.getValue().viterbi;
			ArrayList<ChartState> path = new ArrayList<ChartState>();
			ChartState cs = topStateEntry.getKey();
			addViterbiBestPathRecursiveBackTrace(path, cs, false);
			return path;
		}
		

		private void addViterbiBestPathRecursiveBackTrace(ArrayList<ChartState> path, ChartState cs, boolean partialDerivation) {			
			path.add(cs);
			Duet<ChartState, ChartState> parentPairs = cs.getViterbiBestParentStatesBackTrace(partialDerivation);
			if (parentPairs==null)
				return;
			cs = parentPairs.getFirst();
			boolean firstNull = true;
			if (cs!=null) {
				firstNull = false;
				addViterbiBestPathRecursiveBackTrace(path,cs, partialDerivation);
			}
			cs = parentPairs.getSecond();
			if (cs!=null) {				 
				if (!firstNull && partialDerivation)
					partialDerivation = false;
				addViterbiBestPathRecursiveBackTrace(path,cs, partialDerivation);					
			}
		}



		/*
		private void countFirstLexSubOperations(
				Set<HashSet<ChartState>> derivationPaths) {			
			for(HashSet<ChartState> p : derivationPaths) {
				countFirstLexSubOperations(p);
			}
		}
		*/
				

		private void countFirstLexSubOperations(
				ArrayList<ChartState> derivationPath) {
			for(ChartState cs : derivationPath) {
				if (!cs.isScanState() && !cs.isCompleteState() && cs.wordIndex()!=sentenceLength) {
					if (cs.hasElementAfterDot())
						countSubDownSubUpSentence[0]++; // SUB-DOWN
					else
						countSubDownSubUpSentence[1]++; // SUB-UP
				}
			}			
		}
		
		//Appendable app = orderedDebug ? orderedDebugSB : Parameters.logPW;
		
		private void appendLn(String line) {
			debugSB.append(line).append('\n');
		}

		private void printDebug(TSNodeLabel parsedTree,
				ArrayList<TSNodeLabel> derivation, 
				ArrayList<ChartState> derivationPath,
				double viterbiLogProb, ArrayList<Integer> numberOfPossibleFrags, 
				ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags) {
							
			appendLn("Sentence #                                  " + sentenceIndex);
			appendLn("Sentence Length:                            " + sentenceLength);	
			appendLn("Total used fringes lex/sub:                 " + Arrays.toString(totalFringesLexSub));
			appendLn("   of which from smoothing lex/sub:         " + Arrays.toString(totalFringesSmoothingLexSub));				
			appendLn("Total removed fringes lex/sub:              " + Arrays.toString(removedFringesLexSub));
			
			appendLn("Sentence words:                             " + Utility.removeBrackAndDoTabulation(wordLabels.toString()));
			
			appendLn("SCAN-set sizes (bin):                       " + Utility.formatIntArray(wordScanSize[0]));
			appendLn("SCAN-set sizes:                             " + Utility.formatIntArray(wordScanSize[1]));
			
			appendLn("SUB-DOWN-FIRST-set sizes (bin):             " + Utility.formatIntArray(wordSubDownFirstSize[0]));
			appendLn("SUB-DOWN-FIRST-set sizes:                   " + Utility.formatIntArray(wordSubDownFirstSize[1]));
			appendLn("SUB-DOWN-SECOND-set sizes (bin) NO FILTER:  " + Utility.formatIntArray(fringeCounterFirstLexSubBin[0]));
			appendLn("SUB-DOWN-SECOND-set sizes (bin):            " + Utility.formatIntArray(wordSubDownSecondSize[0]));
			appendLn("SUB-DOWN-SECOND-set sizes:                  " + Utility.formatIntArray(wordSubDownSecondSize[1]));
			appendLn("SUB-UP-FIRST-set sizes (bin):               " + Utility.formatIntArray(wordSubUpFirstSize[0]));
			appendLn("SUB-UP-FIRST-set sizes:                     " + Utility.formatIntArray(wordSubUpFirstSize[1]));
			appendLn("SUB-UP-SECOND-set sizes (bin):  NO FILTER   " + Utility.formatIntArray(fringeCounterFirstLexSubBin[1]));
			appendLn("SUB-UP-SECOND-set sizes (bin):              " + Utility.formatIntArray(wordSubUpSecondSize[0]));
			appendLn("SUB-UP-SECOND-set sizes:                    " + Utility.formatIntArray(wordSubUpSecondSize[1]));
			appendLn("COMPLETE-set sizes (bin):                   " + Utility.formatIntArray(wordCompleteSize[0]));
			appendLn("COMPLETE-set sizes:                         " + Utility.formatIntArray(wordCompleteSize[1]));
			
			appendLn("Total Running time (sec):                   " + treeTimer.readTotalTimeSecFormat());
			appendLn("Chart init time (sec):                      " + initChartTime);
			appendLn("Word times (sec):                           " + Utility.removeBrackAndDoTabulation(Arrays.toString(wordTimes)));				
			appendLn("Finished:                                   " + finished);							
			appendLn("Successfully parsed:                        " + parsingSuccess);
			appendLn("Reached top:                                " + actuallyReachedTop);
			appendLn("Potentially Reaching top:                   " + potentiallyReachingTop);			
	
			if (parsingSuccess) {
				if (numberOfPossibleFrags!=null && possibleFrags!=null) {
					long totalSubDerivations = Utility.times(numberOfPossibleFrags);
					appendLn("Number of possible frags in fringe derivation: " + numberOfPossibleFrags);
					appendLn("Total number of frags-derivations: " + totalSubDerivations);
					appendLn("Total Sub-Down Sub-Up: " + Arrays.toString(countSubDownSubUpSentence) + 
							" " + Arrays.toString(Utility.makePercentage(countSubDownSubUpSentence)));
					if (totalSubDerivations==1)
						appendLn("Viterbi forest derivation:\n" + toStringTex(derivation));
					else
						appendLn("Viterbi forest derivation:\n" + toStringTexMultiple(possibleFrags));
				}
				if (derivation!=null)
					appendLn("Viterbi best derivation length: " + derivation.size());
				appendLn("Viterbi best log prob: " + viterbiLogProb);
				//appendLn("Viterbi best derivation:\n" + toStringTex(derivation));
				
				appendLn("Parsed tree: " + parsedTree.toStringQtree());
				appendLn("Gold tree: " + goldTestTree.toStringQtree());
				appendLn("EvalC Scores: " + Arrays.toString(EvalC.getRecallPrecisionFscore(parsedTree, goldTestTree)));
			}
			
			appendLn("");
			
			if (limitTestToSentenceIndex!=-1) {				
				File tableFile = new File(outputPath + "chart.txt");
				PrintWriter pw = FileUtil.getPrintWriter(tableFile);
				Vector<ArrayList<ChartState>> derPaths = null; 
				if (derivationPath!=null) {
					derPaths = new Vector<ArrayList<ChartState>>();
					derPaths.add(derivationPath);
					//for(int i=0; i<sentenceLength; i++) {
					//	derPaths.add(retrieveRandomViterbiPartialPath(i+1, null));
					//}
					//derPaths.add(retrieveRandomViterbiPartialPath(sentenceLength));
					//this.toStringTexChartHorizontal(pw,derPaths,true);
				}				
				this.toStringTexChartVertical(pw,derPaths,false, false);
				pw.close();
				System.out.println("Total elements in chart: " + totalElementsInChart() );
			}
			
			appendInLogFile(debugSB, sentenceIndex);
			debugSB = new StringBuilder();
			
				
		}

		class ChartColumn {
			
			TermLabel word;
			int wordIndex;			
			
			HashMap<TermLabel,HashMap<ChartState,ProbBox>> 
				scanStatesSet, //indexed on root
				subDownFirstStatesSet, //indexed on non-term after dot				
				subUpFirstStatesSet, //indexed on root				
				completeStatesSet; //indexed on root
			HashMap<TermLabel,HashMap<FragFringe,double[]>>
				subDownSecondStateSet, //indexed on root
				subUpSecondFragFringeSet; //indexed on first term (sub-site)
			
			//only if nullary are present
			HashMap<TermLabel,HashMap<FragFringe,double[]>>
				subDownSecondStateSetNullary, //indexed on root
				subUpSecondFragFringeSetNullary, //indexed on first term (sub-site)
				subUpSecondFragFringeSetNullaryNoLex; //indexed on first term (sub-site)
			
			// the prob is the viterbi prob (like forward but doing max instead of sum)
			
			public ChartColumn (TermLabel word, int i) {
				this.word = word;
				this.wordIndex = i;
				initVariabels();
			}
			
			private void initVariabels() {
				scanStatesSet = new HashMap<TermLabel,HashMap<ChartState,ProbBox>>();
				subDownFirstStatesSet = new HashMap<TermLabel,HashMap<ChartState,ProbBox>>();
				subUpFirstStatesSet = new HashMap<TermLabel,HashMap<ChartState,ProbBox>>();
				completeStatesSet = new HashMap<TermLabel,HashMap<ChartState,ProbBox>>();
				subDownSecondStateSet = (wordIndex!=0 && wordIndex!=sentenceLength) ?
					wordFragFringeFirstLex.get(wordIndex) :
					new HashMap<TermLabel,HashMap<FragFringe,double[]>>();	
				subUpSecondFragFringeSet = (wordIndex!=0 && wordIndex!=sentenceLength) ?
					wordFragFringeFirstSub.get(wordIndex) :
					new HashMap<TermLabel,HashMap<FragFringe,double[]>>();			
				
				/*
				if (nullaryArePresent) {
					subDownSecondStateSetNullary = wordNullaryFragFringeFirstLex.get(wordIndex);
					if (wordIndex!=sentenceLength) {						
						subUpSecondFragFringeSetNullary = wordNullaryFragFringeFirstSub.get(wordIndex);
						subUpSecondFragFringeSetNullaryNoLex = wordNullaryFragFringeFirstSubNoLex.get(wordIndex);
					}					
				}
				*/
					
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
				/*
				if (nullaryArePresent) {
					performSubDownSubUpCompleteNullaryCycle();
					if (isInterrupted()) return;
				}
				*/
				performSubDown();
				if (isInterrupted()) return;
				performSubUp();				
				if (isInterrupted()) return;
				performScan();				
			}			
			
			
			public void completeAndTerminate() {
				performComplete();
				if (isInterrupted()) return;
				/*
				if (nullaryArePresent) {
					performSubDownCompleteNullary();
					if (isInterrupted()) return;
				}
				*/
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

			private void performStart() {
				for(Entry<TermLabel,HashMap<FragFringe,double[]>> e : wordFragFringeFirstLex.get(wordIndex).entrySet()) {
					for(Entry<FragFringe,double[]> f : e.getValue().entrySet()) {
						FragFringe lexFragFringe = f.getKey();
						ChartState cs = new ChartState(lexFragFringe, -1);
						double viterbi = f.getValue()[0];
						ProbBox.addProbStateOrMaxViterbiDouble(
								scanStatesSet, lexFragFringe.root, cs, viterbi, null);
						cs.probBox.totalViterbi = viterbi; 
					}
				}				
			}

			private void performScan() {
				ChartColumn nextChartColumn = chart.get(wordIndex+1);
				for(HashMap<ChartState,ProbBox> scanRootSet : scanStatesSet.values()) {
					for(Entry<ChartState,ProbBox> e : scanRootSet.entrySet()) {
						ChartState scanState = e.getKey();
						double viterbi = e.getValue().viterbi;
						ChartState newState = nextChartColumn.new ChartState(scanState);						
						newState.advanceDot();											
						if (nextChartColumn.addState(newState, viterbi, scanState))
							newState.probBox.totalViterbi = e.getValue().totalViterbi;
						if (isInterrupted()) return;
					}
				}
			}
			
			/*
			private void performSubDownSubUpCompleteNullaryCycle() {
				do {
					performSubDownCompleteNullary();
					if (isInterrupted()) return;
					if (!performSubUpCompleteNullary())
						return;
					if (isInterrupted()) return;
				} while(true);
			}
			*/
			
			private void performSubDownCompleteNullary() {
				
				HashMap<TermLabel, HashMap<ChartState, ProbBox>> newSubDownFirstTable =
					new HashMap<TermLabel, HashMap<ChartState, ProbBox>>(subDownFirstStatesSet);								
				
				do {
					Set<TermLabel> secondRootSet = new HashSet<TermLabel>(subDownSecondStateSetNullary.keySet());					
					secondRootSet.retainAll(newSubDownFirstTable.keySet());
					
					HashMap<TermLabel, HashMap<ChartState, ProbBox>> tmpSubDownFirstTable =
						new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
					
					for(TermLabel root : secondRootSet) {
						Entry<ChartState, ProbBox> firstSubDownBestEntry = 
							ProbBox.getMaxTotalViterbiEntry(subDownFirstStatesSet.get(root));
						ChartState firstSubDownBestState = firstSubDownBestEntry.getKey();		
						//double firstSubDownBestEntryTotViterbi = firstSubDownBestEntry.getValue().totalViterbi;
						HashMap<FragFringe,double[]> subDownSecondMap = subDownSecondStateSetNullary.get(root);
						//only one
						for(Entry<FragFringe,double[]> e : subDownSecondMap.entrySet()) {
							FragFringe subDownSecondFragFringe = e.getKey();						
							double viterbi = e.getValue()[0];						
							ChartState newState = new ChartState(subDownSecondFragFringe, wordIndex);
							newState.advanceDot();
							processNewStateFromNullary(newState, viterbi, firstSubDownBestState,
									tmpSubDownFirstTable, null, null);
							if (isInterrupted()) return;
						}
					}
					
					newSubDownFirstTable = tmpSubDownFirstTable;
					
				} while(!newSubDownFirstTable.isEmpty());
				
			}
			
			private boolean performSubUpCompleteNullary() {
				
				boolean newSubFirstAdded = false;
								
				HashMap<TermLabel, HashMap<ChartState, ProbBox>> subUpFirstTableCopy = 
					new HashMap<TermLabel, HashMap<ChartState, ProbBox>>(subUpFirstStatesSet);
				
				//SUBUP-SECOND NULLARY NO LEX (THEY GO ALL TO SUBDOWN FIRST)
				do {
					
					HashMap<TermLabel, HashMap<ChartState, ProbBox>> tmpSubUpFirstTable =
						new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
					
					HashSet<TermLabel> subUpSecondNullaryNoLexSubSites = 
						new HashSet<TermLabel>(subUpSecondFragFringeSetNullaryNoLex.keySet());
					
					subUpFirstTableCopy.keySet().retainAll(subUpSecondNullaryNoLexSubSites);
					subUpSecondNullaryNoLexSubSites.retainAll(subUpFirstTableCopy.keySet());
					
					for(TermLabel root : subUpSecondNullaryNoLexSubSites) {
						HashMap<FragFringe,double[]> subUpSecondTable = 
							subUpSecondFragFringeSetNullaryNoLex.get(root);
						Entry<ChartState, ProbBox> firstSubUpBestEntry = 
							ProbBox.getMaxTotalViterbiEntry(subUpFirstTableCopy.get(root));
						ChartState mostProbSubUpFirstState = firstSubUpBestEntry.getKey();
						ProbBox mostProbSubUpFirstPb = firstSubUpBestEntry.getValue();											
						for(Entry<FragFringe,double[]> e : subUpSecondTable.entrySet()) {
							FragFringe subUpSecondFragFringe = e.getKey();
							double secondProb = e.getValue()[0];
							double viterbi = secondProb + mostProbSubUpFirstPb.viterbi;
							ChartState newState = new ChartState(subUpSecondFragFringe, -1);
							newState.advanceDot();
							assert (newState.isStarred && !newState.hasElementAfterDot());
							processNewStateFromNullary(newState, viterbi, 
									mostProbSubUpFirstState, null, tmpSubUpFirstTable, null);
						}											
					}	
					
					subUpFirstTableCopy = tmpSubUpFirstTable;
					
				} while(!subUpFirstTableCopy.isEmpty());
				
				//SUBUP-SECOND NULLARY WITH LEX (THEY GO ALL TO SCAN)
				subUpFirstTableCopy = 
					new HashMap<TermLabel, HashMap<ChartState, ProbBox>>(subUpFirstStatesSet);
				
				subUpFirstTableCopy.keySet().retainAll(subUpSecondFragFringeSetNullary.keySet());
				subUpSecondFragFringeSetNullary.keySet().retainAll(subUpFirstTableCopy.keySet());								
				for(Entry<TermLabel, HashMap<FragFringe,double[]>> subUpSecondEntry : subUpSecondFragFringeSetNullary.entrySet()) {					
					TermLabel root = subUpSecondEntry.getKey();
					Entry<ChartState, ProbBox> firstSubUpBestEntry = 
						ProbBox.getMaxTotalViterbiEntry(subUpFirstTableCopy.get(root));
					ChartState mostProbSubUpFirstState = firstSubUpBestEntry.getKey();
					ProbBox mostProbSubUpFirstPb = firstSubUpBestEntry.getValue();
					HashMap<FragFringe,double[]> subUpSecondTable = subUpSecondEntry.getValue();					
					for(Entry<FragFringe,double[]> e : subUpSecondTable.entrySet()) {
						FragFringe subUpSecondFragFringe = e.getKey();
						double secondProb = e.getValue()[0];
						double viterbi = secondProb + mostProbSubUpFirstPb.viterbi;
						ChartState newState = new ChartState(subUpSecondFragFringe, -1);
						newState.advanceDot();
						if (ProbBox.addProbStateOrMaxViterbiDouble(scanStatesSet, 
							subUpSecondFragFringe.root, newState, viterbi, mostProbSubUpFirstState))
							newState.probBox.totalViterbi = secondProb + mostProbSubUpFirstPb.totalViterbi;
					}																
				}							
				
				return newSubFirstAdded;
			}
			
			private boolean processNewStateFromNullary(ChartState cs, double viterbi, 
					ChartState bestPreviousState, 
					HashMap<TermLabel, HashMap<ChartState, ProbBox>> tmpSubDownFirstTable,
					HashMap<TermLabel, HashMap<ChartState, ProbBox>> tmpSubUpFirstTable,
					HashMap<TermLabel, HashMap<ChartState, ProbBox>> recursiveSubDownFirstSetArgument) {
				// new state could be complete (3), subup-first (2) or subdown-first (1)
				int stateType = classifyState(cs);
				switch (stateType) {
					case 0:
						// scan
						if (ProbBox.addProbStateOrMaxViterbiDouble(scanStatesSet, 
								cs.root(), cs, viterbi, bestPreviousState))
							cs.probBox.totalViterbi = viterbi + bestPreviousState.probBox.totalViterbi;
						return false;
					case 1:
						// subdown-first
						TermLabel nextTerm = cs.peekAfterDot();
						if (recursiveSubDownFirstSetArgument!=null) {
							if (ProbBox.addProbStateOrMaxViterbiDouble(recursiveSubDownFirstSetArgument, 
									nextTerm, cs, viterbi, bestPreviousState)) {
								cs.probBox.totalViterbi = viterbi + bestPreviousState.probBox.totalViterbi;
							}
							return false;
						}
						if (ProbBox.addProbStateOrMaxViterbiDouble(subDownFirstStatesSet, 
								nextTerm, cs, viterbi, bestPreviousState)) {
							cs.probBox.totalViterbi = viterbi + bestPreviousState.probBox.totalViterbi;
							if (tmpSubDownFirstTable!=null)
								Utility.putInMapDouble(tmpSubDownFirstTable, nextTerm, cs, cs.probBox);
							return true;
						}
						return false;								
					case 2:
						// subup-first
						TermLabel newStateRoot = cs.root();
						if (ProbBox.addProbStateOrMaxViterbiDouble(subUpFirstStatesSet, 
								newStateRoot, cs, viterbi, bestPreviousState)) {
							cs.probBox.totalViterbi = viterbi + bestPreviousState.probBox.totalViterbi;
							if (tmpSubUpFirstTable!=null)
								Utility.putInMapDouble(tmpSubUpFirstTable, newStateRoot, cs, cs.probBox);
						}
						return false;
					case 3:
						// complete
						newStateRoot = cs.root();
						boolean addNewSubDownFirst = false;
						if (ProbBox.addProbStateOrMaxViterbiDouble(completeStatesSet, 
								newStateRoot, cs, viterbi, bestPreviousState)) {
							cs.probBox.totalViterbi = viterbi + bestPreviousState.probBox.totalViterbi;
							//newComplete.add(newState);
						}
						int index = cs.subScriptIndex;
						HashMap<ChartState, ProbBox> pastStateSubDownSet = 
							chart.get(index).subDownFirstStatesSet.get(newStateRoot);
						boolean sameIndex = index==wordIndex;
						//int count = 0;
						do {						
							HashMap<TermLabel, HashMap<ChartState, ProbBox>> recursiveSubDownFirstSet 
								= new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();					
							for(Entry<ChartState, ProbBox> e : pastStateSubDownSet.entrySet()) {
								//count++;
								ChartState pastStateSubDown = e.getKey();
								ProbBox pastStateSubDownPb = e.getValue();
								double newViterbi = pastStateSubDownPb.viterbi + viterbi;
								ChartState newState = new ChartState(pastStateSubDown);					
								newState.advanceDot();
								
								if (sameIndex) {									
									//newState.hasElementAfterDot() && 
									//newState.peekAfterDot()==newStateRoot
									//recursive = true;
									//potentially recursive
									processNewStateFromNullary(newState, newViterbi, 
											cs, tmpSubDownFirstTable, tmpSubUpFirstTable, 
											recursiveSubDownFirstSet);									
								}
								else {
									if (processNewStateFromNullary(newState, newViterbi, 
											cs, tmpSubDownFirstTable, tmpSubUpFirstTable, 
											recursiveSubDownFirstSetArgument))
										addNewSubDownFirst = true;
								}								
							}
							if (isInterrupted()) return false;
							if (recursiveSubDownFirstSet.isEmpty())
								break;
							for(Entry<TermLabel, HashMap<ChartState, ProbBox>>  subDownFirst : recursiveSubDownFirstSet.entrySet()) {
								nextTerm = subDownFirst.getKey();
								boolean recursiveSet = nextTerm==newStateRoot;
								Iterator<ChartState> iter = subDownFirst.getValue().keySet().iterator();
								while(iter.hasNext()) {
									ChartState next = iter.next();
									ProbBox pb = next.probBox;										
									if (ProbBox.addProbStateOrMaxViterbiDouble(subDownFirstStatesSet, 
											nextTerm, next, pb.viterbi, pb.previousState)) {										
										if (tmpSubDownFirstTable!=null)
											Utility.putInMapDouble(tmpSubDownFirstTable, 
													nextTerm, next, next.probBox);
										addNewSubDownFirst = true;
									}
									else if (recursiveSet){
										iter.remove();
									}
								}
							}								
							pastStateSubDownSet = recursiveSubDownFirstSet.get(newStateRoot);
							
						} while(pastStateSubDownSet!=null && !pastStateSubDownSet.isEmpty());
						return addNewSubDownFirst;
				}
				return false;
			}

			private void performSubDown() {
				
				Set<TermLabel> firstSubSiteSet = subDownFirstStatesSet.keySet();
				Set<TermLabel> secondRootSet = subDownSecondStateSet.keySet();
				
				/*
				if (nullaryArePresent) {
					//firstSubSiteSet = new HashSet<TermLabel>(firstSubSiteSet);
					Set<TermLabel> secondRootSetWithNullary = new HashSet<TermLabel>(secondRootSet);
					secondRootSetWithNullary.addAll(subDownSecondStateSetNullary.keySet());
					firstSubSiteSet.retainAll(secondRootSetWithNullary);
				}
				else {*/
					firstSubSiteSet.retainAll(secondRootSet);
				//}
								
				secondRootSet.retainAll(firstSubSiteSet);
				
				for(TermLabel root : secondRootSet) {					
					HashMap<FragFringe,double[]> subDownSecondMap = subDownSecondStateSet.get(root);	
					if (subDownSecondMap!=null) {
						Entry<ChartState, ProbBox> firstSubDownBestEntry = 
							ProbBox.getMaxTotalViterbiEntry(subDownFirstStatesSet.get(root));
						ChartState firstSubDownBestState = firstSubDownBestEntry.getKey();		
						double firstSubDownBestEntryTotViterbi = firstSubDownBestEntry.getValue().totalViterbi;
						for(Entry<FragFringe,double[]> e : subDownSecondMap.entrySet()) {
							FragFringe subDownSecondFragFringe = e.getKey();						
							double viterbi = e.getValue()[0];						
							ChartState newState = new ChartState(subDownSecondFragFringe, wordIndex);
							if (ProbBox.addProbStateOrMaxViterbiDouble(scanStatesSet, 
									subDownSecondFragFringe.root, newState, viterbi, firstSubDownBestState))
								newState.probBox.totalViterbi = viterbi + firstSubDownBestEntryTotViterbi;	
						}
					}
					
					if (isInterrupted()) return;
				}				
			}

			private void performSubUp() {
				
				Set<TermLabel> firstRootSet = subUpFirstStatesSet.keySet();
				Set<TermLabel> secondSubSiteSet = subUpSecondFragFringeSet.keySet();
				
				/*
				if (nullaryArePresent) {
					Set<TermLabel> secondSubSiteSetWithNullary = new HashSet<TermLabel>(secondSubSiteSet);
					secondSubSiteSetWithNullary.addAll(subUpSecondFragFringeSetNullary.keySet());
					secondSubSiteSetWithNullary.addAll(subUpSecondFragFringeSetNullaryNoLex.keySet());
					firstRootSet.retainAll(secondSubSiteSetWithNullary);
				}
				else {*/
					firstRootSet.retainAll(secondSubSiteSet);
				//}
				
				secondSubSiteSet.retainAll(firstRootSet);
											
				for(TermLabel secondSubSite : secondSubSiteSet) {					
					HashMap<FragFringe,double[]> subUpSecondTable = subUpSecondFragFringeSet.get(secondSubSite);
					if (subUpSecondTable!=null) {
						Entry<ChartState, ProbBox> firstSubUpBestEntry = 
							ProbBox.getMaxTotalViterbiEntry(subUpFirstStatesSet.get(secondSubSite));
						ChartState mostProbSubUpFirstState = firstSubUpBestEntry.getKey();
						ProbBox mostProbSubUpFirstPb = firstSubUpBestEntry.getValue();									
						for(Entry<FragFringe,double[]> e : subUpSecondTable.entrySet()) {
							FragFringe subUpSecondFragFringe = e.getKey();
							double secondProb = e.getValue()[0];
							double viterbi = secondProb + mostProbSubUpFirstPb.viterbi;
							ChartState newState = new ChartState(subUpSecondFragFringe, -1);
							newState.advanceDot();
							if (ProbBox.addProbStateOrMaxViterbiDouble(scanStatesSet, 
								subUpSecondFragFringe.root, newState, viterbi, mostProbSubUpFirstState))
								newState.probBox.totalViterbi = secondProb + mostProbSubUpFirstPb.totalViterbi;
						}
					}
					if (isInterrupted()) return;
				}		
			}
			
			private void performComplete() {
				
				if (completeStatesSet.isEmpty())
					return;
				
				CompletePriorityQueue completeStatesQueue = 
						new CompletePriorityQueue(wordIndex);
											
				for(HashMap<ChartState,ProbBox> completeStates : completeStatesSet.values()) {
					for(Entry<ChartState,ProbBox> e : completeStates.entrySet()) {
						completeStatesQueue.add(e.getKey(), e.getValue().viterbi);
						assert e.getKey().probBox != null;
					}
				}
				
				while(!completeStatesQueue.isEmpty()) {
					Duet<ChartState, double[]> duet = completeStatesQueue.poll();
					ChartState cs = duet.getFirst();		
					assert cs.probBox != null;
					double csProb = duet.getSecond()[0];
					int index = cs.subScriptIndex;
					TermLabel root = cs.fragFringe.root;
					HashMap<ChartState, ProbBox> pastStateSubDownSet = 
							chart.get(index).subDownFirstStatesSet.get(root);	
					
					for(Entry<ChartState, ProbBox> e : pastStateSubDownSet.entrySet()) {
						ChartState pastStateSubDown = e.getKey();
						ProbBox pastStateSubDownPb = e.getValue();
						double newViterbi = pastStateSubDownPb.viterbi + csProb;
						ChartState newState = new ChartState(pastStateSubDown);					
						newState.advanceDot();
						if (newState.isCompleteState()) {
							boolean updated = ProbBox.addProbStateOrMaxViterbiDouble(
									completeStatesSet, newState.root(), newState, newViterbi, cs);	
							if (updated) {
								newState.probBox.totalViterbi = 
									csProb + pastStateSubDownPb.totalViterbi;				
							}
							//else if (newState.probBox!=null){
								// IT OCCURS!
							//}
							completeStatesQueue.add(newState, newState.probBox.viterbi);
						}	
						else {
							boolean updated = addState(newState, newViterbi, cs);
							if (updated) {
								newState.probBox.totalViterbi = csProb + pastStateSubDownPb.totalViterbi;
							}
							//else if (newState.probBox!=null){
							//	// IT OCCURS!
							//}
						}
					}
			
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
			

			
			public void terminate() {
				finished = true;
				if (!subUpFirstStatesSet.isEmpty()) {
					parsingSuccess = true;
					if (subUpFirstStatesSet.keySet().contains(TOPnode)) 
						potentiallyReachingTop = true;
				}								
			}

			
			
			public boolean addState(ChartState cs, double newViterbi, ChartState previousCs) {
				if (!cs.hasElementAfterDot()) {
					assert previousCs!=null;
					TermLabel root = cs.root();
					if (cs.isStarred)
						return ProbBox.addProbStateOrMaxViterbiDouble(
								subUpFirstStatesSet, root, cs, newViterbi, previousCs);
					return ProbBox.addProbStateOrMaxViterbiDouble(
							completeStatesSet, root, cs, newViterbi, previousCs);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {					
					if (nextTerm==word) {
						return ProbBox.addProbStateOrMaxViterbiDouble(
								scanStatesSet, cs.root(), cs, newViterbi, previousCs);
					}
					return false;
				}
				return ProbBox.addProbStateOrMaxViterbiDouble(
						subDownFirstStatesSet, nextTerm, cs, newViterbi, previousCs);
			}
			
			public int classifyState(ChartState cs) {
				// -1 scan but not next word
				// 0 scan
				// 1 subdown first
				// 2 subup first
				// 3 complete
				if (!cs.hasElementAfterDot()) {
					if (cs.isStarred)
						return 2;
					return 3;
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {					
					if (nextTerm==word) 
						return 0;
					return -1;
				}
				return 1;
			}
			
			class ChartState {
				
				FragFringe fragFringe;
				int dotIndex;
				int subScriptIndex;
				int length;
				boolean isStarred;
				ProbBox probBox;
				
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
				
				public void advanceDot() {
					dotIndex++;
				}
				
				/*
				public void advanceDotSkipNullary() {
					dotIndex++;
					if (nullaryArePresent) {
						while (hasElementAfterDot() && peekAfterDot()==nullaryNode) {
							dotIndex++;
						}
					}		
				}
				*/
				
				public void retreatDot() {
					dotIndex--;
				}
				
				/*
				public void retreatDotSkipNullary() {					
					if (nullaryArePresent) {
						while (hasElementBeforeDot() && peekBeforeDot()==nullaryNode) {
							dotIndex--;
						}
					}		
					dotIndex--;
				}
				*/
				
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
				
				
				public Duet<ChartState, ChartState> getViterbiBestParentStates(boolean partialDerivation) {
					
					
					if (dotIndex==0) {
						if (partialDerivation && wordIndex>0) {
							ChartState subDownFirst = ProbBox.getMaxTotalViterbiEntry(
									subDownFirstStatesSet.get(this.root())).getKey();
							return new Duet<ChartState, ChartState>(null, subDownFirst);
						}
						return null;
						// if current state is SCAN STATE the previous state was
						// a SUB-DOWN FIRST, but is retrieved when tracing back completion.
					}
					
					if (isScanState()) { //CURREN IS SCAN STATE
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.retreatDot();
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
									ProbBox.getMaxTotalViterbiEntry(subUpFirstStatesSet.get(beforeDot)).getKey());
						}
						//PREVIOUS WAS COMPLETE STATE
						//(same as last case below)
					}
					 
					//SUB-DOWN-FIRST SUB-UP-FIRST COMPLETE 
					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						//PREVIOUS WAS SCAN STATE
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.retreatDot();						
						return new Duet<ChartState, ChartState>(
								previousState, null);						
					}					
					
					//before dot is non-lexical
					//GENERATE THROUGH COMPLETION
					double max= -Double.MAX_VALUE;
					ChartState bestCompleteState = null;
					ChartState bestPreviousState = null;
					
					for(Entry<ChartState, ProbBox> e : completeStatesSet.get(beforeDot).entrySet()) {
						ChartState completeState = e.getKey();
						int previousStateColumnIndex = completeState.subScriptIndex;
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.retreatDot();
						HashMap<ChartState,ProbBox> previousSubDownFirstStateTable = 
								previousStateColumn.subDownFirstStatesSet.get(beforeDot); 
						if (previousSubDownFirstStateTable!=null) { 
							ProbBox previousPb = previousSubDownFirstStateTable.get(previousState);
							if (previousPb!=null) {
								double prob = e.getValue().viterbi+previousPb.viterbi;
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
				
				public Duet<ChartState, ChartState> getViterbiBestParentStatesBackTrace(boolean partialDerivation) {
					
					
					if (dotIndex==0) {
						if (partialDerivation && wordIndex>0) {
							return new Duet<ChartState, ChartState>(null, this.probBox.previousState);
						}
						return null;
						// if current state is SCAN STATE the previous state was
						// a SUB-DOWN FIRST, but is retrieved when tracing back completion.
					}
					
					TermLabel beforeDot = peekBeforeDot();
					//boolean scanState = false;
					
					if (isScanState()) { //CURREN IS SCAN STATE						
						if (beforeDot.isLexical) {				
							//PREVIOUS WAS SCAN STATE
							return new Duet<ChartState, ChartState>(
									this.probBox.previousState, null);
						}
						//PREVIOUS WAS SUB-UP-FIRST STATE
						if (dotIndex==1) {						
							return new Duet<ChartState, ChartState>(
									null, this.probBox.previousState);
						}
						//PREVIOUS WAS COMPLETE STATE
						//(same as last case below)
						//scanState = true;
					}
					
					/*
					if (nullaryArePresent && !scanState && this.fragFringe.isNullaryFragFringe()) {
						TermLabel firstTerminal = this.fragFringe.firstTerminal();
						if (firstTerminal==nullaryNode)
							return null; //complete nullary
						//if (!firstTerminal.isLexical) {
							//sub-up nullary loop
							return new Duet<ChartState, ChartState>(
								null, this.probBox.previousState);
						//}
					}
					*/
										
					//SUB-DOWN-FIRST SUB-UP-FIRST COMPLETE 
					if (beforeDot.isLexical) {
						//PREVIOUS WAS SCAN STATE
						return new Duet<ChartState, ChartState>(
								this.probBox.previousState, null);						
					}										

					
					//before dot is non-lexical
					//GENERATE THROUGH COMPLETION
					ChartState previousState = this.probBox.previousState;					
					int previousStateColumnIndex = previousState.subScriptIndex;
					assert previousStateColumnIndex!=-1;
					ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
					ChartState bestPreviousState = previousStateColumn.new ChartState(this);
					bestPreviousState.retreatDot();
					bestPreviousState.probBox = previousStateColumn.
							subDownFirstStatesSet.get(beforeDot).get(bestPreviousState);
					assert bestPreviousState.probBox != null; //CHECK HERE
					
					return new Duet<ChartState, ChartState>(
							bestPreviousState, previousState);
					
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
					/*
					if (nullaryArePresent) {
						if (fragFringe.isNullaryFragFringe())
							return true;	
						if (dotIndex==2 && isScanState() && 
								peekBeforeDot()==nullaryNode && !peekBeforeDotSkipNullary().isLexical)
							return true;
					}
					return dotIndex==0 || (dotIndex==1 && isScanState() && !peekBeforeDotSkipNullary().isLexical);
					*/
					return dotIndex==0 || (dotIndex==1 && isScanState() && !peekBeforeDot().isLexical);
				}

				
				public TermLabel peekBeforeDot() {
					return fragFringe.yield[dotIndex-1];
				}
				
				/*
				public TermLabel peekBeforeDotSkipNullary() {
					if (nullaryArePresent) {
						for(int i=dotIndex-1; i>=0; i--) {
							TermLabel d = fragFringe.yield[i]; 
							if (d!=nullaryNode)
								return d;								
						}
						return null;
					}
					return fragFringe.yield[dotIndex-1];
					
				}
				*/

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
				
			}
			
		}
		
	}
	
	public static void mainT(String[] args) throws Exception {
		
		EvalC.DELETE_LABELS = new String[]{};
		
		debug = true;
		treeLenghtLimit = 20;
		maxSecPerWord = 10*60;		
		threadCheckingTime = 1000;	
		threads = 1;
		limitTestToSentenceIndex = 0;		
		removeFringesNonCompatibleWithSentence = false;
		printAllLexFragmentsToFileWithProb = true;
		printTemplateFragsToFileWithProb = true;		
		
		printAllLexFragmentsToFileWithProb = true;
		printTemplateFragsToFileWithProb = false;
		topDownProb = false;
		
		computeIncrementalEvaluation = false;
		
		computeMPPtree = false;
		kBest = 10;
		
		TMB = new TreeMarkoBinarizationRight_LC_nullary();
		//TMB = new TreeMarkoBinarizationRight_LC();
		TreeMarkoBinarization.markH = 0;		
		TreeMarkoBinarization.markV = 1;
		
		minFreqOpenPoSSmoothing = 0;
		minFreqTemplatesSmoothingFactor = 0;
		minFreqFirstWordFragSmoothing = 0;
		
		String basePath = Parameters.scratchPath;
		String workingPath = basePath + "/PLTSG/ToyCorpusNull2/";	
		//String workingPath = basePath + "/PLTSG/ToyCorpus2/";
		File trainTB = new File(workingPath + "trainTB.mrg");
		File testTB = new File(workingPath + "testTB.mrg");
		originalGoldFile  = new File(workingPath + "testTB.mrg");
		File fragFile = new File(workingPath + "fragments.mrg");
		
		FragmentExtractorFreq.openClassPoSThreshold = 0;
		
		FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(
						workingPath, "noSmoothing", "noSmoothing", trainTB, testTB, fragFile);
		
		FragmentExtractorFreq.printAllLexFragmentsToFile = true;
		
		new IncrementalTSGEarlyParserViterbiProbBoxNullarySimplified(FE, testTB, workingPath).run();
	}
	
	

	public static void main(String[] args) throws Exception {
				
		//EvalC.DELETE_LABELS = new String[]{};
		
		debug = true;
		orderedDebug = true;
		treeLenghtLimit = -1;
		maxSecPerWord = 10*60;		
		threadCheckingTime = 1000;	
		threads = 5;
		limitTestToSentenceIndex = -1;
		
		minFreqTemplatesSmoothingFactor = 1E-8;
		minFreqOpenPoSSmoothing = 1E-4;
		minFreqFirstWordFragSmoothing = 1E-2;
		removeFringesNonCompatibleWithSentence = false;
		
		printAllLexFragmentsToFileWithProb = false;
		printTemplateFragsToFileWithProb = false;
		topDownProb = false;
		
		computeIncrementalEvaluation = false;
		
		computeMPPtree = false;
		kBest = 10;
		
		TMB = new TreeMarkoBinarizationRight_LC_nullary();
		//TMB = new TreeMarkoBinarizationRight_LC();
		TreeMarkoBinarization.markH = 0;		
		TreeMarkoBinarization.markV = 1;
		
		//String basePath = System.getProperty("user.home");
		//String workingPath = basePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";
		String basePath = Parameters.scratchPath;
		//String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4/";
		//String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H1_V1_UkM4_UkT4/";
		//String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H1_V2_UkM4_UkT4/";
		String workingPath = basePath + "/PLTSG/MB_ROARK_RightNull_H0_V1_UkM4_UkT4/";
		//String workingPath = basePath + "/PLTSG/MB_ROARK_RightNull_H1_V1_UkM4_UkT4/";
		File trainTB = new File(workingPath + "wsj-02-21.mrg");
		File testTB = new File(workingPath + "wsj-24.mrg");					
		File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		//File fragFile = new File(workingPath + "fragments_approxFreq_first500K.txt");
		//File fragFile = new File(workingPath + "fragments_small.txt");
		String fragExtrWorkingPath  = workingPath + "FragmentExtractor/";
		String outputPathParsing = workingPath + "Parsing/";
		new File(fragExtrWorkingPath).mkdirs();
		new File(outputPathParsing).mkdirs();

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
				//new String[]{	"10",			"NoSmoothing"},
				//new String[]{	"100",			"NoSmoothing"},
				//new String[]{	"1000",			"NoSmoothing"},
				new String[]{	"NoSmoothing",	"NoSmoothing"},
		};
		
		for(int ll : new int[]{10}) { //,20,40,-1
			treeLenghtLimit = ll;
			for(int t : new int[]{50}) {
				FragmentExtractorFreq.openClassPoSThreshold = t;
				//for(double w : new double[]{1E-1,1E-2,1E-3,1E-4,1E-5}) {					
					//for(double d : new double[]{1E-1,1E-2,1E-3,1E-4,1E-5}) {						
						for(String[] set : settings) {
							for(boolean b : new boolean[]{false}) {
								topDownProb = b;
								minFreqOpenPoSSmoothing = 1E-4;
								minFreqFirstWordFragSmoothing = 1E-2;
								FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(
										fragExtrWorkingPath, set[0], set[1], trainTB, testTB, fragFile);
								//FragmentExtractorFreq.printAllLexFragmentsToFile = true;
								//FragmentExtractorFreq.printTemplateFragsToFile = true;
								//FragmentExtractorFreq.fragmentsFreqFactor = 2;
								//FragmentExtractorFreq.useSizeMultFactor = true;
								new IncrementalTSGEarlyParserViterbiProbBoxNullarySimplified(FE, testTB, outputPathParsing).run();								
								System.gc();
							}			
						}
					//}
				//}
			}
		}

	}	
	
}
