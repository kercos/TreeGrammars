package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.corpora.Wsj;
import tsg.incremental.FragSpanChartProb.SpanProb;
import tsg.incremental.IncrementalTsgParser.ChartParserThread.ChartColumn.ChartState;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationRight_LC;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Duet;
import util.LongestCommonSubsequence;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class IncrementalTsgParser extends Thread {

	static final Label TOPlabel = Label.getLabel("S");
	static final TermLabel TOPnode = TermLabel.getTermLabel(TOPlabel, false);
	// static final Label Slabel = Label.getLabel("S");
	// static final TermLabel Snode = TermLabel.getTermLabel(Slabel, false);
	static final TermLabel emptyLexNode = TermLabel.getTermLabel("", true);
	static final String WsjFlatGoldPath = Parameters.corpusPath + "WSJ/FLAT_NOTRACES/";
	static final String WsjOrigianlGoldPath = Parameters.corpusPath + "WSJ/ORIGINAL_READABLE_CLEANED_TOP_SEMTAGSOFF/";

	static boolean debug = true;
	static boolean forceTop = false;

	static File originalGoldFile = null;

	static int treeLenghtLimit = -1;
	static long maxSecPerWord = 5 * 60;
	static long threadCheckingTime = 10000;
	static int threads = 1;
	static int limitTestToSentenceIndex = -1;

	static double minFreqTemplatesSmoothingFactor = 1E-5;
	static double minFreqOpenPoSSmoothing = 1E-2;
	static double minFreqFirstWordFragSmoothing = 1E-1;

	static boolean removeFringesNonCompatibleWithSentence;

	static boolean printAllLexFragmentsToFileWithProb;
	static boolean printTemplateFragsToFileWithProb;

	static boolean computeStandardEvaluation = true;
	static boolean computeIncrementalEvaluation = false;
	static boolean computePrefixProbabilities = false;

	static boolean computeMPPtree = true;
	static boolean computeMinRiskTree = true;

	static TreeMarkoBinarization TMB;

	static boolean completeSlow = true;

	static boolean pruning = false;
	static double pruningExp = 2;
	static double pruningGamma = 1E-15;

	File testTBfile, logFile, goldTBFile;
	File evalC_MPD_File, evalB_MPD_File, evalC_MRP_prod_File,
			evalB_MRP_prod_File, evalC_MPP_File, evalB_MPP_File;
	File parsedMPDFile, parsedMRPprodFile, parsedMPPFile;
	String incrementalParsedMinConnectPrefixFileName,
			incrementalGoldMinConnectPrefixFileName,
			incrementalEvalMinConnectPrefixFileName;
	String incrementalParsedMaxPredictedPrefixFileName,
			incrementalGoldMaxPredictedPrefixFileName,
			incrementalEvalMaxPredictedPrefixFileName;
	File incrementalParsed,
			incrementalParsedMinConnectedAllFile,
			incrementalParsedMinConnectedLastFile,
			incrementalGoldMinConnectedAllFile,
			incrementalEvalMinConnectedAllFile;
	File incrementalParsedMaxPredictedAllFile,
			incrementalGoldMaxPredictedAllFile,
			incrementalEvalMaxPredictedAllFile;
	File allMinConnPrefixResults;
	FragmentExtractorFreq FE;
	String outputPath;
	ArrayList<TSNodeLabel> testTB;
	TSNodeLabel[] parsedTB_MPD, parsedTB_MRP_prod, // parsedTB_MRP_sum,
			parsedTB_MPP;
	TSNodeLabel[][] partialParsedTB;
	double[] sentenceLogProbTestSet;
	double[][] sentenceprefixProb;
	PrintProgress progress;
	int parsedTrees, potentiallyReachingTopTrees, actuallyReachedTopTrees,
			interruptedTrees, indexLongestWordTime, totalSubDerivations,
			ambiguousViterbiDerivations;
	int[] countSubDownSubUpTotal, totalPruned;
	float longestWordTime;
	Iterator<TSNodeLabel> testTBiterator;
	int treebankIteratorIndex;
	boolean templateSmoothing;
	EvalC evalC_MPD, evalC_MRP_prod, evalC_MPP,
			incrementalMinConnectedAllEvalC, incrementalMaxPredictedAllEvalC;
	EvalC[] incrementalMinConnectedEvalC, incrementalMaxPredictedEvalC;
	int[] predictedWordCorrect;
	HashMap<Integer, int[]> predictedLengthFreq;
	StringBuilder[] orderedDebugSBArray;
	int orderedDebugNextIndex;
	SimpleTimer globalTimer;

	// MAPPING FROM FRINGES TO FRAGMENTS
	HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> firstWordLexFringeFragMap,
			firstLexFringeFragMap, firstSubFringeFragMap;

	// MAPPING FROM FRINGES TEMPLATES TO FRAGMENTS
	HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> firstWordFringesTemplatesFragMap,
			firstLexTemplateFragMap, firstSubTemplateFragMap;

	HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> firstLexFringes,
			firstLexFringesTemplates, // indexed on lex and root
			firstSubFringes, firstSubFringesTemplates; // indexed on lex and
														// firstTerm

	// used only for first word
	HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> firstWordFirstLexFringes,
			firstWordFirstLexFringesTemplates; // indexed on lex and root

	HashMap<TermLabel, HashMap<TermLabel, int[]>> posLexFreqTable,
			lexPosFreqTable;
	HashMap<TermLabel, HashSet<TermLabel>> posLexTable;
	Set<TermLabel> posSet;

	public IncrementalTsgParser(FragmentExtractorFreq FE, File testTBfile, String outputPath) throws Exception {
		this.FE = FE;
		this.testTBfile = testTBfile;
		this.testTB = Wsj.getTreebank(testTBfile);
		this.outputPath = outputPath;
	}

	private void initVariables() {
		globalTimer = new SimpleTimer(true);
		this.templateSmoothing = FragmentExtractorFreq.smoothingFromFrags || FragmentExtractorFreq.smoothingFromMinSet;
		if (!templateSmoothing)
			minFreqTemplatesSmoothingFactor = 0;

		String minSetThreshold = FragmentExtractorFreq.minFragFreqForSmoothingFromMinSet == -1 ? "NoThreshold" : "" + FragmentExtractorFreq.minFragFreqForSmoothingFromMinSet;
		String fragThreshold = FragmentExtractorFreq.minFragFreqForSmoothingFromFrags == -1 ? "NoThreshold" : "" + FragmentExtractorFreq.minFragFreqForSmoothingFromFrags;

		String section = testTBfile.getName().substring(testTBfile.getName().indexOf('.') - 2, testTBfile.getName().indexOf('.'));

		outputPath += "ITSG_wsj" + section + "_";
		outputPath += FileUtil.dateTimeString();
		/*
		outputPath += treeLenghtLimit == -1 ? "ALL" : "upTo" + treeLenghtLimit;
		outputPath += "_openPosTh_" + FragmentExtractorFreq.openClassPoSThreshold;
		outputPath += "_openPosSmooth_" + minFreqOpenPoSSmoothing;
		outputPath += "_minFreqFirstWordFragSmoothing_" + minFreqFirstWordFragSmoothing;
		outputPath += "_minFreqTemplatesSmoothingFactor_" + minFreqTemplatesSmoothingFactor;
		outputPath += FragmentExtractorFreq.addMinimalFragments ? ("_MinSetExt_" + minSetThreshold) : "_NoMinSet";
		outputPath += FE.fragmentsFile != null ? ("_Frags_" + fragThreshold) : "_NoFrags";
		outputPath += pruning ? "_Prune-ON-" + pruningExp + "-" + pruningGamma : "_Prune-OFF";
		outputPath += "_complete" + (completeSlow ? "Slow" : "Fast");
		*/
		outputPath += "/";

		File dirFile = new File(outputPath);
		if (dirFile.exists()) {
			System.out.println("Output dir already exists, I'll delete the content...");
			FileUtil.removeAllFileAndDirInDir(dirFile);
		} else {
			dirFile.mkdirs();
		}

		logFile = new File(outputPath + "LOG.log");

		parsedMPDFile = new File(outputPath + "PARSED_MPD.mrg");
		evalC_MPD_File = new File(outputPath + "Eval_MPD.evalC");
		evalB_MPD_File = new File(outputPath + "Eval_MPD.evalB");
		if (computeMinRiskTree) {
			parsedMRPprodFile = new File(outputPath + "PARSED_MRP_prod.mrg");
			evalC_MRP_prod_File = new File(outputPath + "Eval_MRP_prod.evalC");
			evalB_MRP_prod_File = new File(outputPath + "Eval_MRP_prod.evalB");
		}
		if (computeMPPtree) {
			parsedMPPFile = new File(outputPath + "PARSED_MPP.mrg");
			evalC_MPP_File = new File(outputPath + "Eval_MPP.evalC");
			evalB_MPP_File = new File(outputPath + "Eval_MPP.evalB");
		}
		goldTBFile = new File(outputPath + "GOLD.mrg");

		this.countSubDownSubUpTotal = new int[2];
		this.totalPruned = new int[2];

		if (computeIncrementalEvaluation) {
			String incrementalSubDir = outputPath + "incremental/";
			new File(incrementalSubDir).mkdir();
			incrementalParsedMinConnectPrefixFileName = incrementalSubDir + "PARSED_MinConnected_prefixLength_";
			incrementalGoldMinConnectPrefixFileName = incrementalSubDir + "GOLD_MinConnected_prefixLength_";
			incrementalEvalMinConnectPrefixFileName = incrementalSubDir + "Eval_MinConnected_prefixLength_";
			incrementalParsedMaxPredictedPrefixFileName = incrementalSubDir + "PARSED_MaxPredicted_prefixLength_";

			incrementalGoldMaxPredictedPrefixFileName = incrementalSubDir + "GOLD_MaxPredicted_prefixLength_";
			incrementalEvalMaxPredictedPrefixFileName = incrementalSubDir + "Eval_MaxPredicted_prefixLength_";

			incrementalParsed = new File(incrementalSubDir + "PARSED_Incremental_All.mrg");
			incrementalParsedMinConnectedAllFile = new File(incrementalSubDir + "PARSED_MinConnected_All.mrg");
			incrementalParsedMinConnectedLastFile = new File(incrementalSubDir + "PARSED_MinConnected_Last.mrg");
			incrementalGoldMinConnectedAllFile = new File(incrementalSubDir + "GOLD_MinConnected_All.mrg");
			incrementalEvalMinConnectedAllFile = new File(incrementalSubDir + "Eval_MinConnected_All.evalC");

			incrementalParsedMaxPredictedAllFile = new File(incrementalSubDir + "PARSED_MaxPredicted_All.mrg");
			incrementalGoldMaxPredictedAllFile = new File(incrementalSubDir + "GOLD_MaxPredicted_All.mrg");
			incrementalEvalMaxPredictedAllFile = new File(incrementalSubDir + "Eval_MaxPredicted_All.evalC");

			allMinConnPrefixResults = new File(incrementalSubDir + "Prefix_MinConn_Results.txt");
		}

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
		printParsedTrees();
		evaluate();
		printPrefixProbabilities();
		printFinalSummary();
		printLogProbTableToFile();
		Parameters.closeLogFile();
	}

	private void printParameters() {
		Parameters.reportLine("\n\n");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("INCREMENTAL TSG PARSER ");
		Parameters.reportLine("-----------------------");
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

		Parameters.reportLine("Pruning: " + pruning);
		if (pruning) {
			Parameters.reportLine("Pruning Exp: " + pruningExp);
			Parameters.reportLine("Pruning Gamme: " + pruningGamma);
		}

	}

	private void printFinalSummary() {
		Parameters.reportLine("Number of successfully parsed trees: " + parsedTrees);
		Parameters.reportLine("\t... of which potentially reaching TOP: " + potentiallyReachingTopTrees);
		Parameters.reportLine("\t... and actually reached TOP: " + actuallyReachedTopTrees);
		Parameters.reportLine("Number of interrupted trees: " + interruptedTrees);
		Parameters.reportLine("Longest time to parse a word (sec): " + longestWordTime + " (" + indexLongestWordTime + ")");
		Parameters.reportLine("Total Sub-Down Sub-Up: " + Arrays.toString(countSubDownSubUpTotal) + " " + Arrays.toString(Utility.makePercentage(countSubDownSubUpTotal)));
		Parameters.reportLine("Total Scan State/Pruned: " + Arrays.toString(totalPruned) + " " + Utility.makePercentage(totalPruned[1], totalPruned[0]));

		if (computeStandardEvaluation)
			Parameters.reportLine("Final evalC scores MPD: " + Arrays.toString(evalC_MPD.getRecallPrecisionFscoreAll()));

		if (computeMinRiskTree) {
			Parameters.reportLine("Final evalC scores MRP product: " + Arrays.toString(evalC_MRP_prod.getRecallPrecisionFscoreAll()));
		}

		if (computeMPPtree) {
			Parameters.reportLine("Final evalC scores MPP: " + Arrays.toString(evalC_MPP.getRecallPrecisionFscoreAll()));
		}
		Parameters.reportLine("Average tree viterbi sub-derivations: " + ((float) totalSubDerivations) / testTB.size());
		Parameters.reportLine("Trees having ambiguous viterbi derivations: " + ambiguousViterbiDerivations + " (" + Utility.makePercentage(ambiguousViterbiDerivations, testTB.size()) + ")");
		if (computeIncrementalEvaluation) {
			StringBuilder pw = new StringBuilder();

			pw.append("Final incremental evalC scores MIN CONNECTED ALL (R_P_F1): ");
			pw.append(Arrays.toString(incrementalMinConnectedAllEvalC.getRecallPrecisionFscoreAll()));
			pw.append('\n');

			pw.append("Final incremental evalC scores MIN CONNECTED (prefix_length/R_P_F1):");
			for (int i = 0; i < incrementalMinConnectedEvalC.length; i++) {
				pw.append(" " + (i + 1) + ":" + Arrays.toString(incrementalMinConnectedEvalC[i].getRecallPrecisionFscoreAll()));
			}
			pw.append('\n');

			Parameters.reportLine(pw.toString());
			Parameters.reportLine("Predicted words length|frequency|correct% : " + predictedLengthFreqToString());
			Parameters.reportLine("Predicted words/correctly: " + Arrays.toString(predictedWordCorrect) + " (" + (float) predictedWordCorrect[1] / predictedWordCorrect[0] * 100 + "%)");
		}
		Parameters.reportLine("Took in Total (sec): " + globalTimer.checkEllapsedTimeSec());
		Parameters.reportLineFlush("Log file: " + logFile);

	}

	private void printLogProbTableToFile() {
		File sentenceLogFile = new File(outputPath + "sentenceLogProbLength.txt");
		PrintWriter pw = FileUtil.getPrintWriter(sentenceLogFile);
		int i = 0;
		for (TSNodeLabel tree : testTB) {
			int length = tree.countLexicalNodes();
			double sentenceLogProb = sentenceLogProbTestSet[i++];
			pw.println(length + "\t" + sentenceLogProb);
		}
		pw.close();
	}

	private String predictedLengthFreqToString() {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<Integer, int[]>> iter = predictedLengthFreq.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, int[]> e = iter.next();
			int predictedLength = e.getKey();
			int[] freqCorrect = e.getValue();
			sb.append(predictedLength);
			sb.append("|");
			sb.append(freqCorrect[0]);
			sb.append("|");
			sb.append((float) freqCorrect[1] / (predictedLength * freqCorrect[0]) * 100 + "%");
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

	private static HashMap<TermLabel, HashMap<TermLabel, int[]>> convertFreqTable(HashMap<Label, HashMap<Label, int[]>> posLexFinal, boolean keyIsLex, boolean valuesAreLex) {

		HashMap<TermLabel, HashMap<TermLabel, int[]>> result = new HashMap<TermLabel, HashMap<TermLabel, int[]>>();

		for (Entry<Label, HashMap<Label, int[]>> e : posLexFinal.entrySet()) {
			TermLabel key = TermLabel.getTermLabel(e.getKey(), keyIsLex);
			HashMap<TermLabel, int[]> value = new HashMap<TermLabel, int[]>();
			for (Entry<Label, int[]> f : e.getValue().entrySet()) {
				value.put(TermLabel.getTermLabel(f.getKey(), valuesAreLex), new int[] { f.getValue()[0] });
			}
			result.put(key, value);
		}

		return result;
	}

	private static HashMap<TermLabel, HashSet<TermLabel>> convertTable(HashMap<Label, HashMap<Label, int[]>> posLexFinal, boolean keyIsLex, boolean valuesAreLex) {

		HashMap<TermLabel, HashSet<TermLabel>> result = new HashMap<TermLabel, HashSet<TermLabel>>();

		for (Entry<Label, HashMap<Label, int[]>> e : posLexFinal.entrySet()) {
			TermLabel key = TermLabel.getTermLabel(e.getKey(), keyIsLex);
			HashSet<TermLabel> value = new HashSet<TermLabel>();
			for (Label l : e.getValue().keySet()) {
				value.add(TermLabel.getTermLabel(l, valuesAreLex));
			}
			result.put(key, value);
		}

		return result;
	}

	private void convertLexFragmentsToFringes() {

		Parameters.reportLineFlush("Converting lex fragments into fringes...");

		firstLexFringes = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
		firstSubFringes = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
		firstLexFringeFragMap = new HashMap<FragFringe, HashMap<TSNodeLabel, double[]>>();
		firstSubFringeFragMap = new HashMap<FragFringe, HashMap<TSNodeLabel, double[]>>();

		firstWordFirstLexFringes = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
		firstWordLexFringeFragMap = new HashMap<FragFringe, HashMap<TSNodeLabel, double[]>>();

		int countFirstLexFrags = 0;
		int countFirstSubFrags = 0;

		// FIRST LEX FRINGES
		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstLex.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			for (Entry<TSNodeLabel, int[]> f : fragSet.entrySet()) {
				TSNodeLabel firstLexFrag = f.getKey();
				FragFringe ff = new FragFringe(firstLexFrag);
				int[] freqArray = f.getValue();
				boolean posSmoothing = freqArray[0] == 0;
				int freq = freqArray[0] - freqArray[1]; // freqArray[0]
				// always >=
				// freqArray[1]
				double d = posSmoothing ? minFreqOpenPoSSmoothing : (freq == 0 ? minFreqFirstWordFragSmoothing : freq);
				if (d > 0) {
					countFirstLexFrags++;
					Utility.putInMapDouble(firstLexFringeFragMap, ff, firstLexFrag, new double[] { d });
				}
				freq = f.getValue()[1];
				d = posSmoothing ? minFreqOpenPoSSmoothing : (freq == 0 ? minFreqFirstWordFragSmoothing : freq);
				if (d > 0) {
					Utility.putInMapDouble(firstWordLexFringeFragMap, ff, firstLexFrag, new double[] { d });
				}
			}
		}

		// PUTTING HIGHEST AND MARGINAL FREQ PER FRAG FRINGE FIRST LEX
		for (Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstLexFringeFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			TermLabel firstLexLabel = ff.firstTerminal();
			double maxFreq = Utility.getMaxValue(e.getValue());
			double totFreq = Utility.getSumValue(e.getValue());
			Utility.putInMapTriple(firstLexFringes, firstLexLabel, ff.root(), ff, new double[] { maxFreq, totFreq });
		}
		// PUTTING HIGHEST AND MARGINAL FREQ PER FRAG FRINGE FIRST LEX FIRST
		// WORD
		for (Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstWordLexFringeFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			TermLabel firstLexLabel = ff.firstTerminal();
			double maxFreq = Utility.getMaxValue(e.getValue());
			double totFreq = Utility.getSumValue(e.getValue());
			Utility.putInMapTriple(firstWordFirstLexFringes, firstLexLabel, ff.root(), ff, new double[] { maxFreq, totFreq });
		}

		// FIRST SUB FRINGES
		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.lexFragsTableFirstSub.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragSet = e.getValue();
			for (Entry<TSNodeLabel, int[]> f : fragSet.entrySet()) {
				TSNodeLabel firstSubFrag = f.getKey();
				FragFringe ff = new FragFringe(firstSubFrag);
				int freq = f.getValue()[0];
				double d = freq == 0 ? minFreqOpenPoSSmoothing : freq;
				if (d > 0) {
					countFirstSubFrags++;
					Utility.putInMapDouble(firstSubFringeFragMap, ff, firstSubFrag, new double[] { d });
				}
			}
		}

		// PUTTING HIGHEST AND MARGINAL FREQ PER FRAG FRINGE FIRST SUB
		for (Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : firstSubFringeFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			TermLabel firstLexLabel = ff.yield[1];
			double maxFreq = Utility.getMaxValue(e.getValue());
			double totFreq = Utility.getSumValue(e.getValue());
			Utility.putInMapTriple(firstSubFringes, firstLexLabel, ff.firstTerminal(), ff, new double[] { maxFreq, totFreq });
		}

		int totalLexFrags = countFirstLexFrags + countFirstSubFrags;
		Parameters.reportLineFlush("Total number of lex fragments: " + totalLexFrags);
		Parameters.reportLineFlush("\tLex first: " + countFirstLexFrags);
		Parameters.reportLineFlush("\tSub first: " + countFirstSubFrags);
		int countFirstLexFringes = firstLexFringeFragMap.size();
		int countFirstSubFringes = firstSubFringeFragMap.size();
		int totalLexFringes = countFirstLexFringes + countFirstSubFringes;
		Parameters.reportLineFlush("Total number of lex fringes: " + totalLexFringes);
		Parameters.reportLineFlush("\tLex first: " + countFirstLexFringes);
		Parameters.reportLineFlush("\tSub first: " + countFirstSubFringes);
		int ambiguousFringesFirstLex = Utility.countAmbiguousMapping(firstLexFringeFragMap);
		int ambiguousFringesFirstSub = Utility.countAmbiguousMapping(firstSubFringeFragMap);
		int totalAmbiguousFringe = ambiguousFringesFirstLex + ambiguousFringesFirstSub;
		Parameters.reportLineFlush("Ambiguous fringes: " + totalAmbiguousFringe + " (" + Utility.makePercentage(totalAmbiguousFringe, totalLexFringes) + ")");
	}

	private void convertTemplateFragmentsToFringes() {

		if (!templateSmoothing)
			return;

		Parameters.reportLineFlush("Converting template fragments into fringes...");

		int[] countFirstLexSubFrags = new int[2];

		firstLexFringesTemplates = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
		firstSubFringesTemplates = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
		firstLexTemplateFragMap = new HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>>();
		firstSubTemplateFragMap = new HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>>();

		firstWordFirstLexFringesTemplates = new HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>>();
		firstWordFringesTemplatesFragMap = new HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>>();

		for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : FE.posFragSmoothingMerged.entrySet()) {
			HashMap<TSNodeLabel, int[]> fragMap = e.getValue();
			for (Entry<TSNodeLabel, int[]> f : fragMap.entrySet()) {
				TSNodeLabel frag = f.getKey();
				TermLabel pos = TermLabel.getTermLabel(frag.getLeftmostLexicalNode().parent);
				FragFringe ff = new FragFringe(frag);
				boolean isFirstLex = ff.firstTerminalIsLexical();
				double d = minFreqTemplatesSmoothingFactor;
				if (d > 0) {
					if (isFirstLex) {
						countFirstLexSubFrags[0]++;
						Utility.putInMapTriple(firstLexTemplateFragMap, ff, pos, frag, new double[] { d });
					} else {
						countFirstLexSubFrags[1]++;
						Utility.putInMapTriple(firstSubTemplateFragMap, ff, pos, frag, new double[] { d });
					}
				}
				d = minFreqTemplatesSmoothingFactor;
				if (isFirstLex && d > 0) {
					Utility.putInMapTriple(firstWordFringesTemplatesFragMap, ff, pos, frag, new double[] { d });
				}
			}
		}

		// PUTTING HIGHEST AND MARGINAL FREQ PER FRAG FRINGE

		// first lex
		for (Entry<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> e : firstLexTemplateFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			HashMap<TermLabel, HashMap<TSNodeLabel, double[]>> posTable = e.getValue();
			for (Entry<TermLabel, HashMap<TSNodeLabel, double[]>> f : posTable.entrySet()) {
				TermLabel pos = f.getKey();
				HashMap<TSNodeLabel, double[]> value = f.getValue();
				double maxFreq = Utility.getMaxValue(value);
				double maxFreqMarg = Utility.getSumValue(value);
				Utility.putInMapTriple(firstLexFringesTemplates, pos, ff.root(), ff, new double[] { maxFreq, maxFreqMarg });
			}
		}

		// first sub
		for (Entry<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> e : firstSubTemplateFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			HashMap<TermLabel, HashMap<TSNodeLabel, double[]>> posTable = e.getValue();
			for (Entry<TermLabel, HashMap<TSNodeLabel, double[]>> f : posTable.entrySet()) {
				TermLabel pos = f.getKey();
				HashMap<TSNodeLabel, double[]> value = f.getValue();
				double maxFreq = Utility.getMaxValue(value);
				double maxFreqMarg = Utility.getSumValue(value);
				Utility.putInMapTriple(firstSubFringesTemplates, pos, ff.firstTerminal(), ff, new double[] { maxFreq, maxFreqMarg });
			}
		}

		for (Entry<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> e : firstWordFringesTemplatesFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			HashMap<TermLabel, HashMap<TSNodeLabel, double[]>> posTable = e.getValue();
			for (Entry<TermLabel, HashMap<TSNodeLabel, double[]>> f : posTable.entrySet()) {
				TermLabel pos = f.getKey();
				HashMap<TSNodeLabel, double[]> value = f.getValue();
				double maxFreq = Utility.getMaxValue(value);
				double maxFreqMarg = Utility.getSumValue(value);
				Utility.putInMapTriple(firstWordFirstLexFringesTemplates, pos, ff.root(), ff, new double[] { maxFreq, maxFreqMarg });
			}

		}

		int[] countFirstLexSubFringes = new int[] { firstLexTemplateFragMap.size(), firstSubTemplateFragMap.size() };

		Parameters.reportLineFlush("Total number of smoothed fragments: " + (Utility.sum(countFirstLexSubFrags)));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexSubFrags[0]);
		Parameters.reportLineFlush("\tSub first: " + countFirstLexSubFrags[1]);
		Parameters.reportLineFlush("Total number of smoothed fringes: " + (Utility.sum(countFirstLexSubFringes)));
		Parameters.reportLineFlush("\tLex first: " + countFirstLexSubFringes[0]);
		Parameters.reportLineFlush("\tSub first: " + countFirstLexSubFringes[1]);

	}

	private void convertFreqToLogRelativeFreq() {

		Parameters.reportLineFlush("Converting Freq to Log Relative Freq");

		// FIRST WORD
		double totalSumFirstWord = getTotalSummedProb(firstWordLexFringeFragMap);
		if (templateSmoothing) {
			totalSumFirstWord += getTotalSummedTemplateProb(firstWordFringesTemplatesFragMap);
		}
		convertToLogRelativeFreq(firstWordFirstLexFringes, totalSumFirstWord);
		convertToLogRelativeFreqFragMap(firstWordLexFringeFragMap, totalSumFirstWord);
		if (templateSmoothing) {
			convertToLogRelativeFreq(firstWordFirstLexFringesTemplates, totalSumFirstWord);
			convertToLogRelativeFreqFragMapTemplate(firstWordFringesTemplatesFragMap, totalSumFirstWord);
		}

		// FIRST LEX
		HashMap<TermLabel, double[]> totalRootFreqFirstLex = new HashMap<TermLabel, double[]>();
		addRootFreq(firstLexFringeFragMap, totalRootFreqFirstLex);

		if (templateSmoothing) {
			addRootFreqTemplate(firstLexTemplateFragMap, totalRootFreqFirstLex);
		}
		convertToLogRootRelativeFreq(firstLexFringes, totalRootFreqFirstLex);
		convertToLogRootRelativeFreqFragMap(firstLexFringeFragMap, totalRootFreqFirstLex);

		if (templateSmoothing) {
			convertToLogRootRelativeFreq(firstLexFringesTemplates, totalRootFreqFirstLex);
			convertToLogRootRelativeFreqFragMapTemplate(firstLexTemplateFragMap, totalRootFreqFirstLex);
		}

		// FIRST SUB
		HashMap<TermLabel, double[]> totalFirstTermFreqSecondLex = new HashMap<TermLabel, double[]>();
		addFirstTermFreq(firstSubFringeFragMap, totalFirstTermFreqSecondLex);

		if (templateSmoothing) {
			addFirstTermFreqTemplate(firstSubTemplateFragMap, totalFirstTermFreqSecondLex);
		}
		convertToLogFirstTermRelativeFreq(firstSubFringes, totalFirstTermFreqSecondLex);
		convertToLogFirstTermRelativeFreqFragMap(firstSubFringeFragMap, totalFirstTermFreqSecondLex);

		if (templateSmoothing) {
			convertToLogFirstTermRelativeFreq(firstSubFringesTemplates, totalFirstTermFreqSecondLex);
			convertToLogFirstTermRelativeFreqFragMapTemplate(firstSubTemplateFragMap, totalFirstTermFreqSecondLex);
		}

	}

	private void addRootFreq(HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> source, HashMap<TermLabel, double[]> target) {

		for (Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : source.entrySet()) {
			TermLabel root = e.getKey().root;
			for (Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {
				Utility.increaseInHashMap(target, root, f.getValue()[0]);
			}
		}

	}

	private void addRootFreqTemplate(HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> source, HashMap<TermLabel, double[]> target) {

		for (Entry<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> e : source.entrySet()) {
			TermLabel root = e.getKey().root;
			for (HashMap<TSNodeLabel, double[]> f : e.getValue().values()) {
				for (double[] d : f.values()) {
					Utility.increaseInHashMap(target, root, d[0]);
				}
			}
		}

	}

	private void addFirstTermFreq(HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> source, HashMap<TermLabel, double[]> target) {

		for (Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : source.entrySet()) {
			TermLabel firstTerm = e.getKey().firstTerminal();
			for (Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {
				Utility.increaseInHashMap(target, firstTerm, f.getValue()[0]);
			}
		}

	}

	private void addFirstTermFreqTemplate(HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> source, HashMap<TermLabel, double[]> target) {

		for (Entry<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> e : source.entrySet()) {
			TermLabel firstTerm = e.getKey().firstTerminal();
			for (HashMap<TSNodeLabel, double[]> t : e.getValue().values()) {
				for (Entry<TSNodeLabel, double[]> f : t.entrySet()) {
					Utility.increaseInHashMap(target, firstTerm, f.getValue()[0]);
				}
			}
		}

	}

	private <T> double getTotalSummedProb(HashMap<T, HashMap<TSNodeLabel, double[]>> source) {
		double result = 0;
		for (HashMap<TSNodeLabel, double[]> t : source.values()) {
			for (double[] d : t.values()) {
				result += d[0];
			}
		}
		return result;
	}

	private double getTotalSummedTemplateProb(HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> source) {
		double result = 0;
		for (HashMap<TermLabel, HashMap<TSNodeLabel, double[]>> t : source.values()) {
			result += getTotalSummedProb(t);
		}
		return result;
	}

	private void convertToLogRootRelativeFreqFragMap(HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> table, HashMap<TermLabel, double[]> totalRootFreq) {

		for (Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : table.entrySet()) {
			TermLabel root = e.getKey().root;
			for (Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {
				double rootFreq = totalRootFreq.get(root)[0];
				double[] freqArray = f.getValue();
				freqArray[0] = Math.log(freqArray[0] / rootFreq);
			}
		}
	}

	private void convertToLogRootRelativeFreqFragMapTemplate(HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> table, HashMap<TermLabel, double[]> totalRootFreq) {

		for (Entry<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> e : table.entrySet()) {
			TermLabel root = e.getKey().root;
			for (HashMap<TSNodeLabel, double[]> t : e.getValue().values()) {
				for (Entry<TSNodeLabel, double[]> f : t.entrySet()) {
					double rootFreq = totalRootFreq.get(root)[0];
					double[] freqArray = f.getValue();
					freqArray[0] = Math.log(freqArray[0] / rootFreq);
				}
			}
		}
	}

	private void convertToLogFirstTermRelativeFreqFragMap(HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> table, HashMap<TermLabel, double[]> totalRootFreq) {

		for (Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : table.entrySet()) {
			TermLabel firstTerm = e.getKey().firstTerminal();
			for (Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {
				double rootFreq = totalRootFreq.get(firstTerm)[0];
				double[] freqArray = f.getValue();
				freqArray[0] = Math.log(freqArray[0] / rootFreq);
			}
		}
	}

	private void convertToLogFirstTermRelativeFreqFragMapTemplate(HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> table, HashMap<TermLabel, double[]> totalRootFreq) {

		for (Entry<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> e : table.entrySet()) {
			TermLabel firstTerm = e.getKey().firstTerminal();
			for (HashMap<TSNodeLabel, double[]> t : e.getValue().values()) {
				for (Entry<TSNodeLabel, double[]> f : t.entrySet()) {
					double rootFreq = totalRootFreq.get(firstTerm)[0];
					double[] freqArray = f.getValue();
					freqArray[0] = Math.log(freqArray[0] / rootFreq);
				}
			}
		}
	}

	private static void convertToLogRootRelativeFreq(HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table, HashMap<TermLabel, double[]> totalRootFreq) {

		for (HashMap<TermLabel, HashMap<FragFringe, double[]>> t : table.values()) {
			convertToLogRootRelativeFreqSimple(t, totalRootFreq);
		}
	}

	private static void convertToLogRootRelativeFreqSimple(HashMap<TermLabel, HashMap<FragFringe, double[]>> table, HashMap<TermLabel, double[]> totalRootFreq) {

		for (HashMap<FragFringe, double[]> s : table.values()) {
			for (Entry<FragFringe, double[]> e : s.entrySet()) {
				TermLabel root = e.getKey().root;
				double rootFreq = totalRootFreq.get(root)[0];
				double[] freqArray = e.getValue();
				freqArray[0] = Math.log(freqArray[0] / rootFreq);
				freqArray[1] = Math.log(freqArray[1] / rootFreq);
			}
		}
	}

	private static void convertToLogFirstTermRelativeFreq(HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table, HashMap<TermLabel, double[]> totalRootFreq) {

		for (HashMap<TermLabel, HashMap<FragFringe, double[]>> t : table.values()) {
			for (HashMap<FragFringe, double[]> s : t.values()) {
				for (Entry<FragFringe, double[]> e : s.entrySet()) {
					TermLabel firstTerm = e.getKey().firstTerminal();
					double rootFreq = totalRootFreq.get(firstTerm)[0];
					double[] freqArray = e.getValue();
					freqArray[0] = Math.log(freqArray[0] / rootFreq);
					freqArray[1] = Math.log(freqArray[1] / rootFreq);
				}
			}
		}
	}

	private static void convertToLogRelativeFreq(HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table, double totalFreq) {

		for (HashMap<TermLabel, HashMap<FragFringe, double[]>> t : table.values()) {
			for (HashMap<FragFringe, double[]> s : t.values()) {
				for (double[] d : s.values()) {
					d[0] = Math.log(d[0] / totalFreq);
					d[1] = Math.log(d[1] / totalFreq);
				}
			}
		}
	}

	private void convertToLogRelativeFreqFragMap(HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> table, double totalFreq) {

		for (HashMap<TSNodeLabel, double[]> t : table.values()) {
			for (double[] d : t.values()) {
				d[0] = Math.log(d[0] / totalFreq);
			}
		}
	}

	private void convertToLogRelativeFreqFragMapTemplate(HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> table, double totalFreq) {

		for (HashMap<TermLabel, HashMap<TSNodeLabel, double[]>> u : table.values()) {
			for (HashMap<TSNodeLabel, double[]> t : u.values()) {
				for (double[] d : t.values()) {
					d[0] = Math.log(d[0] / totalFreq);
				}
			}
		}
	}

	public HashMap<TSNodeLabel, double[]> getOriginalFrags(FragFringe ff, boolean firstWord) {

		HashMap<TSNodeLabel, double[]> result = null;

		if (firstWord)
			result = firstWordLexFringeFragMap.get(ff);
		else if (ff.firstTerminalIsLexical()) {
			result = firstLexFringeFragMap.get(ff);
		} else {
			result = firstSubFringeFragMap.get(ff);
		}

		if (templateSmoothing && result == null) { // template
			FragFringe ffCopy = ff.cloneFringe();
			boolean isFirstLex = ffCopy.firstTerminalIsLexical();
			int lexIndex = isFirstLex ? 0 : 1;
			Label lexLabel = ffCopy.yield[lexIndex].label;
			TermLabel lexLabelTerm = TermLabel.getTermLabel(lexLabel, true);
			ffCopy.yield[lexIndex] = emptyLexNode;
			HashMap<TermLabel, HashMap<TSNodeLabel, double[]>> anonymizedPosFrags = 
				firstWord ? firstWordFringesTemplatesFragMap.get(ffCopy) : 
					(isFirstLex ? firstLexTemplateFragMap.get(ffCopy) : firstSubTemplateFragMap.get(ffCopy));
			result = new HashMap<TSNodeLabel, double[]>();
			Set<TermLabel> validPos = lexPosFreqTable.get(lexLabelTerm).keySet();
			assert (validPos != null);
			for (Entry<TermLabel, HashMap<TSNodeLabel, double[]>> e : anonymizedPosFrags.entrySet()) {
				TermLabel pos = e.getKey();
				if (!validPos.contains(pos))
					continue;
				for (Entry<TSNodeLabel, double[]> f : e.getValue().entrySet()) {
					TSNodeLabel frag = f.getKey().clone();
					double freq = f.getValue()[0];
					frag.getLeftmostLexicalNode().label = lexLabel;
					result.put(frag, new double[] { freq });
				}
			}
		}
		return result;

	}

	public double getOriginalMargProb(FragFringe ff, boolean firstWord) {

		boolean isFirstLex = ff.firstTerminalIsLexical();
		int lexIndex = isFirstLex ? 0 : 1;
		TermLabel lexLabel = ff.yield[lexIndex];
		TermLabel root = ff.root;

		double[] result = null;

		if (firstWord)
			result = firstWordFirstLexFringes.get(lexLabel).get(root).get(ff);
		else if (isFirstLex) {
			result = firstLexFringes.get(lexLabel).get(root).get(ff);
		} else {
			TermLabel firstSub = ff.yield[0];
			result = firstSubFringes.get(lexLabel).get(firstSub).get(ff);
		}

		if (result == null) { // template
			FragFringe ffCopy = ff.cloneFringe();
			ffCopy.yield[lexIndex] = emptyLexNode;
			if (firstWord)
				result = firstWordFirstLexFringesTemplates.get(lexLabel).get(root).get(ffCopy);
			else if (isFirstLex) {
				result = firstLexFringesTemplates.get(lexLabel).get(root).get(ff);
			} else {
				TermLabel firstSub = ff.yield[0];
				result = firstSubFringesTemplates.get(lexLabel).get(firstSub).get(ff);
			}
		}
		return result[1];

	}

	public static void printProbTable(HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table, HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> fringeFragMap, File outputFile, boolean separateLines) {

		HashMap<TermLabel, HashMap<FragFringe, double[]>> secondKeyTable = new HashMap<TermLabel, HashMap<FragFringe, double[]>>();

		distributeElements(table, secondKeyTable);

		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for (Entry<TermLabel, HashMap<FragFringe, double[]>> e : secondKeyTable.entrySet()) {
			for (Entry<FragFringe, double[]> f : e.getValue().entrySet()) {
				double prob = Math.exp(f.getValue()[0]);
				int numberOfFrags = fringeFragMap.get(f.getKey()).size();
				double totalProb = prob * numberOfFrags;
				pw.println(f.getKey().toString() + "\t" + prob + " x " + numberOfFrags + " = " + totalProb);
			}
			if (separateLines)
				pw.println();
		}
		pw.close();

	}

	public static void printProbTableTemplate(HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table, HashMap<FragFringe, HashMap<TermLabel, HashMap<TSNodeLabel, double[]>>> fringeFragMap, File outputFile, boolean separateLines) {

		HashMap<TermLabel, HashMap<FragFringe, double[]>> secondKeyTable = new HashMap<TermLabel, HashMap<FragFringe, double[]>>();

		distributeElements(table, secondKeyTable);

		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for (Entry<TermLabel, HashMap<FragFringe, double[]>> e : secondKeyTable.entrySet()) {
			for (Entry<FragFringe, double[]> f : e.getValue().entrySet()) {
				double prob = Math.exp(f.getValue()[0]);
				int numberOfFrags = Utility.countTotalDouble(fringeFragMap.get(f.getKey()));
				double totalProb = prob * numberOfFrags;
				pw.println(f.getKey().toString() + "\t" + prob + " x " + numberOfFrags + " = " + totalProb);
			}
			if (separateLines)
				pw.println();
		}
		pw.close();

	}

	public static void printAmbiguousFringes(HashMap<FragFringe, HashMap<TSNodeLabel, double[]>> fringeFragMap, File outputFile) {

		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for (Entry<FragFringe, HashMap<TSNodeLabel, double[]>> e : fringeFragMap.entrySet()) {
			FragFringe ff = e.getKey();
			HashMap<TSNodeLabel, double[]> fragTable = e.getValue();
			if (fragTable.size() > 1) {
				pw.println(ff.toString());
				for (Entry<TSNodeLabel, double[]> r : fragTable.entrySet()) {
					TSNodeLabel t = r.getKey();
					double prob = Math.exp(r.getValue()[0]);
					pw.println("\t" + t.toString(false, true) + "\t" + prob);
				}
				pw.println();
			}
		}
		pw.close();

	}

	private static void distributeElements(HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> table, HashMap<TermLabel, HashMap<FragFringe, double[]>> secondKeyTable) {

		for (HashMap<TermLabel, HashMap<FragFringe, double[]>> subtable : table.values()) {
			for (Entry<TermLabel, HashMap<FragFringe, double[]>> e : subtable.entrySet()) {
				TermLabel secondKey = e.getKey();
				HashMap<FragFringe, double[]> secondKeySubTable = secondKeyTable.get(secondKey);
				if (secondKeySubTable == null) {
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

			File outputFile = new File(outputPath + "_lexFrags_firstLex.mrg");
			Parameters.reportLine("Printing lex fragments first lex with probs to " + outputFile);
			printProbTable(firstLexFringes, firstLexFringeFragMap, outputFile, true);

			outputFile = new File(outputPath + "_lexFrags_firstSub.mrg");
			Parameters.reportLine("Printing lex fragments first sub with probs to " + outputFile);
			printProbTable(firstSubFringes, firstSubFringeFragMap, outputFile, true);

			outputFile = new File(outputPath + "_lexFrags_firstLex_ambiguous.mrg");
			Parameters.reportLine("Printing first-lex ambiguous fringes/frags " + outputFile);
			printAmbiguousFringes(firstLexFringeFragMap, outputFile);

			outputFile = new File(outputPath + "_lexFrags_firstSub_ambiguous.mrg");
			Parameters.reportLine("Printing first-sub ambiguous fringes/frags " + outputFile);
			printAmbiguousFringes(firstSubFringeFragMap, outputFile);

			outputFile = new File(outputPath + "_lexFrags_firstWord.mrg");
			Parameters.reportLine("Printing lex fragments first word with probs to " + outputFile);
			printProbTable(firstWordFirstLexFringes, firstWordLexFringeFragMap, outputFile, false);
		}

		if (printTemplateFragsToFileWithProb && templateSmoothing) {
			if (firstLexFringesTemplates != null) {
				File gericFragsForSmoothingFile = new File(outputPath + "_templatesFrags_firstLex.mrg");
				Parameters.reportLine("Printing template fragments first lex with probs to " + gericFragsForSmoothingFile);
				printProbTableTemplate(firstLexFringesTemplates, firstLexTemplateFragMap, gericFragsForSmoothingFile, true);
			}

			if (firstSubFringesTemplates != null) {
				File gericFragsForSmoothingFile = new File(outputPath + "_templatesFrags_firstSub.mrg");
				Parameters.reportLine("Printing template fragments first sub with probs to " + gericFragsForSmoothingFile);
				printProbTableTemplate(firstSubFringesTemplates, firstSubTemplateFragMap, gericFragsForSmoothingFile, true);
			}

			if (firstWordFirstLexFringesTemplates != null) {
				File gericFragsForSmoothingFile = new File(outputPath + "_templatesFrags_firstWord.mrg");
				Parameters.reportLine("Printing template fragments first word with probs to " + gericFragsForSmoothingFile);
				printProbTableTemplate(firstWordFirstLexFringesTemplates, firstWordFringesTemplatesFragMap, gericFragsForSmoothingFile, false);
			}
		}

	}

	private void filterTestTB() {
		Parameters.reportLineFlush("Filtering TestTB");
		if (treeLenghtLimit > 0) {
			if (originalGoldFile == null)
				originalGoldFile = new File(WsjOrigianlGoldPath + testTBfile.getName());
			if (!originalGoldFile.exists()) {
				Parameters.reportLine("Original test file not found: " + originalGoldFile);
				Parameters.reportLine("Using the one in input as the gold: " + testTBfile);
				originalGoldFile = testTBfile;
			}
			Scanner scan = FileUtil.getScanner(originalGoldFile);
			Iterator<TSNodeLabel> treebankIterator = testTB.iterator();
			while (treebankIterator.hasNext()) {
				treebankIterator.next();
				TSNodeLabel t = null;
				try {
					t = new TSNodeLabel(scan.nextLine());
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (t.countLexicalNodes() > treeLenghtLimit)
					treebankIterator.remove();
			}
		}

		if (limitTestToSentenceIndex != -1) {
			Parameters.reportLineFlush("Only 1 tree");
			TSNodeLabel t = testTB.get(limitTestToSentenceIndex);
			testTB.clear();
			testTB.add(t);
		}
		Parameters.reportLineFlush("Number of trees: " + testTB.size());
	}

	private void parseIncremental() {

		int testSize = testTB.size();

		Parameters.reportLineFlush("Parsing incrementally...");
		Parameters.reportLineFlush("Number of trees: " + testSize);

		orderedDebugSBArray = new StringBuilder[testSize];

		this.parsedTB_MPD = new TSNodeLabel[testSize];
		if (computeMinRiskTree) {
			this.parsedTB_MRP_prod = new TSNodeLabel[testSize];
		}
		if (computeMPPtree) {
			this.parsedTB_MPP = new TSNodeLabel[testSize];
		}
		if (computeIncrementalEvaluation) {
			this.partialParsedTB = new TSNodeLabel[testSize][];
		}
		if (computePrefixProbabilities) {
			sentenceprefixProb = new double[testSize][];
		}

		progress = new PrintProgress("Parsing tree:", 1, 0);
		testTBiterator = testTB.iterator();

		sentenceLogProbTestSet = new double[testSize];

		Parameters.logLineFlush("\n");

		ChartParserThread[] threadsArray = new ChartParserThread[threads];
		for (int i = 0; i < threads; i++) {
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
			for (int i = 0; i < threads; i++) {
				ChartParserThread t = threadsArray[i];
				if (t.isAlive()) {
					someRunning = true;
					SimpleTimer wordTimer = t.currentWordTimer;
					if (wordTimer == null || !wordTimer.isRunning())
						continue;
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

		} while (true);

		progress.end();
	}

	private void evaluate() {		
		if (computeStandardEvaluation)
			evaluateStandard();
		if (computeIncrementalEvaluation)
			evaluateIncremental();
	}
	
	private void printParsedTrees() {
		Parameters.reportLine("Printing parsed trees");
		
		PrintWriter pwMPD = FileUtil.getPrintWriter(parsedMPDFile);
		PrintWriter pwMRP_prod = null;
		if (computeMinRiskTree) {
			pwMRP_prod = FileUtil.getPrintWriter(parsedMRPprodFile);
		}
		PrintWriter pwMPP = null;
		if (computeMPPtree) {
			pwMPP = FileUtil.getPrintWriter(parsedMPPFile);
		}
		PrintWriter pwGold = FileUtil.getPrintWriter(goldTBFile);
		int size = parsedTB_MPD.length;
		for (int i = 0; i < size; i++) {
			TSNodeLabel goldTree = testTB.get(i);
			if (goldTree.toString().indexOf('@') != -1) {
				goldTree = TMB.undoMarkovBinarization(goldTree);
				String topLabel = goldTree.label();
				if (topLabel.indexOf('@') != -1) {
					topLabel = topLabel.replaceAll("@", "");
					goldTree.label = Label.getLabel(topLabel);
				}
			}
			pwGold.println(goldTree);
			String[] words = goldTree.toFlatWordArray();
			String[] pos = goldTree.toPosArray();
			pwMPD.println(getParsedTree(parsedTB_MPD[i], words, pos));
			if (computeMinRiskTree) {
				pwMRP_prod.println(getParsedTree(parsedTB_MRP_prod[i], words, pos));
			}
			if (computeMPPtree) {
				pwMPP.println(getParsedTree(parsedTB_MPP[i], words, pos));
			}
		}
		pwGold.close();
		pwMPD.close();
		if (computeMinRiskTree) {
			pwMRP_prod.close();
		}
		if (computeMPPtree) {
			pwMPP.close();
		}
	}

	private void evaluateStandard() {
		Parameters.reportLine("Running standard evaluation");
		evalC_MPD = new EvalC(goldTBFile, parsedMPDFile, evalC_MPD_File);
		evalC_MPD.makeEval();
		new EvalB(goldTBFile, parsedMPDFile, evalB_MPD_File);

		if (computeMinRiskTree) {
			evalC_MRP_prod = new EvalC(goldTBFile, parsedMRPprodFile, evalC_MRP_prod_File);
			evalC_MRP_prod.makeEval();
			new EvalB(goldTBFile, parsedMRPprodFile, evalB_MRP_prod_File);
		}

		if (computeMPPtree) {
			evalC_MPP = new EvalC(goldTBFile, parsedMPPFile, evalC_MPP_File);
			evalC_MPP.makeEval();
			new EvalB(goldTBFile, parsedMPPFile, evalB_MPP_File);
		}

	}

	private TSNodeLabel getParsedTree(TSNodeLabel parsedTree, String[] words, String[] pos) {
		if (parsedTree == null) {
			return TSNodeLabel.defaultWSJparse(words, pos, "TOP");
		} else {
			// t.changeLexLabelsFromStringArray(goldWords);
			if (parsedTree.toString().indexOf('@') != -1) {
				parsedTree = TMB.undoMarkovBinarization(parsedTree);
				String topLabel = parsedTree.label();
				if (topLabel.indexOf('@') != -1) {
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
		for (TSNodeLabel t : testTB) {
			int length = t.countLexicalNodes();
			if (length > maxLength)
				maxLength = length;
		}
		// incrementalParsedMaxPredictedFileName
		// incrementalEvalMaxPredictedFileName
		File[] fileParsedArrayMinConnected = new File[maxLength];
		File[] fileGoldArrayMinConnected = new File[maxLength];
		File[] fileParsedArrayMaxPredicted = new File[maxLength];
		File[] fileGoldArrayMaxPredicted = new File[maxLength];
		this.incrementalMinConnectedEvalC = new EvalC[maxLength];
		this.incrementalMaxPredictedEvalC = new EvalC[maxLength];
		PrintWriter[] pwParsedArrayMinConnected = new PrintWriter[maxLength];
		PrintWriter[] pwGoldArrayMinConnected = new PrintWriter[maxLength];
		// PrintWriter[] pwParsedArrayMaxPredicted = new PrintWriter[maxLength];
		// PrintWriter[] pwGoldArrayMaxPredicted = new PrintWriter[maxLength];
		PrintWriter pwParsedIncremental = FileUtil.getPrintWriter(incrementalParsed);
		PrintWriter pwParsedMinConnectedAll = FileUtil.getPrintWriter(incrementalParsedMinConnectedAllFile);
		PrintWriter pwParsedMinConnectedLast = FileUtil.getPrintWriter(incrementalParsedMinConnectedLastFile);
		PrintWriter pwGoldMinConnectedAll = FileUtil.getPrintWriter(incrementalGoldMinConnectedAllFile);
		// PrintWriter pwParsedMaxPredictedAll =
		// FileUtil.getPrintWriter(incrementalParsedMaxPredictedAllFile);
		// PrintWriter pwGoldMaxPredictedAll =
		// FileUtil.getPrintWriter(incrementalGoldMaxPredictedAllFile);

		for (int i = 0; i < maxLength; i++) {
			String prefixLength = Utility.padZero(3, i + 1);
			File parsedFileMinConnected = new File(incrementalParsedMinConnectPrefixFileName + prefixLength + ".mrg");
			fileParsedArrayMinConnected[i] = parsedFileMinConnected;
			pwParsedArrayMinConnected[i] = FileUtil.getPrintWriter(parsedFileMinConnected);
			File goldFileMinConnected = new File(incrementalGoldMinConnectPrefixFileName + prefixLength + ".mrg");
			fileGoldArrayMinConnected[i] = goldFileMinConnected;
			pwGoldArrayMinConnected[i] = FileUtil.getPrintWriter(goldFileMinConnected);
			File parsedFileMaxPredicted = new File(incrementalParsedMaxPredictedPrefixFileName + prefixLength + ".mrg");
			fileParsedArrayMaxPredicted[i] = parsedFileMaxPredicted;
			// pwParsedArrayMaxPredicted[i] =
			// FileUtil.getPrintWriter(parsedFileMaxPredicted);
			File goldFileMaxPredicted = new File(incrementalGoldMaxPredictedPrefixFileName + prefixLength + ".mrg");
			fileGoldArrayMaxPredicted[i] = goldFileMaxPredicted;
			// pwGoldArrayMaxPredicted[i] =
			// FileUtil.getPrintWriter(goldFileMaxPredicted);
		}

		predictedWordCorrect = new int[2];
		predictedLengthFreq = new HashMap<Integer, int[]>();
		File wordPredictionFile = new File(outputPath + "wordPredictions.txt");
		PrintWriter wordPredictionPw = FileUtil.getPrintWriter(wordPredictionFile);

		int sentenceIndex = 0;
		for (TSNodeLabel[] partialTrees : partialParsedTB) {
			int length = partialTrees.length;
			TSNodeLabel goldTree = testTB.get(sentenceIndex);
			String goldTreeString = goldTree.toString();
			if (goldTreeString.indexOf('@') != -1 || goldTreeString.indexOf('|') != -1) {
				goldTree = TMB.undoMarkovBinarization(goldTree);
			}
			String[] goldWords = goldTree.toFlatWordArray();
			for (int j = 0; j < length; j++) {
				int prefixLength = j + 1;
				// gold tree
				TSNodeLabel goldTreeChopped = goldTree.getMinimalConnectedStructure(prefixLength);
				goldTreeChopped.removeRedundantRules();
				pwGoldArrayMinConnected[j].append(goldTreeChopped.toString()).append('\n');
				pwGoldMinConnectedAll.append(goldTreeChopped.toString()).append('\n');
				// parsed tree
				TSNodeLabel parsedPartialTree = partialTrees[j];				
				TSNodeLabel parsedPartialTreeMinConnected = null;
				String[] posPrefix = Arrays.copyOf(goldTree.toPosArray(), prefixLength);
				if (parsedPartialTree == null) {
					String[] wordPrefix = Arrays.copyOf(goldWords, prefixLength);
					parsedPartialTree = TSNodeLabel.defaultWSJparse(wordPrefix, posPrefix, "TOP");
					parsedPartialTreeMinConnected = parsedPartialTree.clone();
				} else {
					String[] parsedWords = parsedPartialTree.toFlatWordArray();
					String[] gp = Arrays.copyOfRange(goldWords, prefixLength, goldWords.length);
					String[] pp = Arrays.copyOfRange(parsedWords, prefixLength, parsedWords.length);
					int predictedLength = pp.length;
					if (predictedLength > 0) {
						int lcs = LongestCommonSubsequence.getLCSLength(gp, pp);
						predictedWordCorrect[0] += predictedLength;
						predictedWordCorrect[1] += lcs;
						Utility.increaseInHashMap(predictedLengthFreq, new Integer(predictedLength), new int[] { 1, lcs });
					}
					parsedPartialTree.removeSubSitesRecursive(".*@.*");
					String topLabel = parsedPartialTree.label();
					if (topLabel.indexOf('@') != -1) {
						topLabel = topLabel.replaceAll("@", "");
						parsedPartialTree.label = Label.getLabel(topLabel);
					}
					String parsedPartialTreeString = parsedPartialTree.toString();
					if (parsedPartialTreeString.indexOf('@') != -1 || parsedPartialTreeString.indexOf('|') != -1) {
						parsedPartialTree = TMB.undoMarkovBinarization(parsedPartialTree);
					}
					ensureQuotePos(parsedPartialTree, posPrefix);
					// GET MINIMAL CONNECTED STRUCTURE
					parsedPartialTreeMinConnected = parsedPartialTree.getMinimalConnectedStructure(prefixLength);

				}

				parsedPartialTreeMinConnected.removeRedundantRules();
				wordPredictionPw.println(parsedPartialTree.toFlatSentence());
				assert parsedPartialTreeMinConnected.countLexicalNodes() == goldTreeChopped.countLexicalNodes();

				String parsedPartialTreeMinConnectedString = parsedPartialTreeMinConnected.toString();
				pwParsedIncremental.append(parsedPartialTree.toString()).append('\n');;
				pwParsedArrayMinConnected[j].append(parsedPartialTreeMinConnectedString).append('\n');
				pwParsedMinConnectedAll.append(parsedPartialTreeMinConnectedString).append('\n');
				if (j == length - 1) {
					pwParsedMinConnectedLast.append(parsedPartialTreeMinConnectedString).append('\n');
				}

			}
			sentenceIndex++;
		}

		wordPredictionPw.close();
		pwParsedMinConnectedAll.close();
		pwParsedMinConnectedLast.close();
		pwGoldMinConnectedAll.close();

		PrintWriter allMinConnPrefixPW = FileUtil.getPrintWriter(allMinConnPrefixResults);

		for (int i = 0; i < maxLength; i++) {

			EvalC.CONSTITUENTS_UNIT = 0;

			pwParsedArrayMinConnected[i].close();
			pwGoldArrayMinConnected[i].close();

			if (i == 0) { // only once for ALL
				incrementalMinConnectedAllEvalC = new EvalC(incrementalGoldMinConnectedAllFile, incrementalParsedMinConnectedAllFile, incrementalEvalMinConnectedAllFile);
				incrementalMinConnectedAllEvalC.makeEval();

				File evalBFile = FileUtil.changeExtension(incrementalEvalMinConnectedAllFile, "evalB");
				new EvalB(incrementalGoldMinConnectedAllFile, incrementalParsedMinConnectedAllFile, evalBFile);
			}

			String prefixLength = Utility.padZero(3, i + 1);
			File evalCFileMinConnected = new File(incrementalEvalMinConnectPrefixFileName + prefixLength + ".evalC");
			EvalC incrementalEvalMinConnected = new EvalC(fileGoldArrayMinConnected[i], fileParsedArrayMinConnected[i], evalCFileMinConnected);
			incrementalEvalMinConnected.makeEval();
			incrementalMinConnectedEvalC[i] = incrementalEvalMinConnected;

			File evalBFile = new File(incrementalEvalMinConnectPrefixFileName + prefixLength + ".evalB");
			new EvalB(fileGoldArrayMinConnected[i], fileParsedArrayMinConnected[i], evalBFile);
			String[] recPrecF1 = EvalB.getAllRecallPrecisionF1(evalBFile);
			allMinConnPrefixPW.println(prefixLength + "\t" + Utility.arrayToString(recPrecF1, '\t'));

		}

		allMinConnPrefixPW.close();

	}

	private void printPrefixProbabilities() {
		if (!computePrefixProbabilities)
			return;
		File prefixProbFile = new File(outputPath + "prefixProbs");
		PrintWriter pw = FileUtil.getPrintWriter(prefixProbFile);
		int count = 0;
		int infCount = 0;
		int sentenceNumber = 1;
		double sum = 0;
		double previous = 0;
		for (double[] prefixProb : sentenceprefixProb) {
			for (int i = 0; i < prefixProb.length; i++) {
				double pp = -prefixProb[i];
				pw.println("Sentence # " + sentenceNumber + "\tPrefix length: " + (i + 1) + "\t" + pp);
				if (!Double.isInfinite(pp)) {
					count++;
					double condProb = i == 0 ? pp : pp - previous;
					sum += condProb;
				} else {
					infCount++;
				}
				previous = pp;
			}
			sentenceNumber++;
		}
		pw.close();
		double ppl = Math.exp(sum / count);
		Parameters.reportLineFlush("Perplexity: " + ppl + " (N=" + count + ", infCount=" + infCount + ")");
	}

	static String[] quotesPos = new String[] { "''", "``" }; // already sorted

	private void ensureQuotePos(TSNodeLabel parsedPartialTree, String[] posPrefix) {
		ArrayList<TSNodeLabel> posTree = parsedPartialTree.collectPreLexicalItems();
		int length = posPrefix.length;
		int i = 0;
		for (TSNodeLabel p : posTree) {
			String goldPos = posPrefix[i++];
			if (Arrays.binarySearch(quotesPos, goldPos) >= 0 && !p.label().equals(goldPos)) {
				p.relabel(goldPos);
			}
			if (i == length)
				break;
		}

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
		while (derivationIter.hasNext()) {
			TSNodeLabel nextFrag = derivationIter.next().clone();
			if (subSitesList.isEmpty()) {
				// SUB-FORWARD
				nextFrag.addSubSites(subSitesList, false);
				TSNodeLabel subSite = subSitesList.removeFirst();
				assert subSite.label == result.label;
				subSite.assignDaughters(result.daughters);
				result = nextFrag;
			} else {
				// SUB-BACKWARD
				TSNodeLabel subSite = subSitesList.removeFirst();
				assert subSite.label == nextFrag.label;
				subSite.assignDaughters(nextFrag.daughters);
				nextFrag.addSubSites(subSitesList, true);
			}
		}
		return result;
	}

	public static TSNodeLabel combineFrags(TSNodeLabel frag1, TSNodeLabel frag2) {
		TSNodeLabel subSite = frag1.getLeftmostSubSite();
		if (subSite == null) {
			// SUB-FORWARD
			subSite = frag2.getLeftmostSubSite();
			assert subSite.label == frag1.label;
			subSite.assignDaughters(frag1.daughters);
			return frag2;
		} else {
			// SUB-BACKWARD
			assert subSite.label == frag2.label;
			subSite.assignDaughters(frag2.daughters);
			return frag1;
		}
	}

	public static void extractSubDerivations(ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags, HashMap<TSNodeLabel, double[]> mergeProbTable) {

		Iterator<HashMap<TSNodeLabel, double[]>> iter = possibleFrags.iterator();

		IdentityHashMap<TSNodeLabel, double[]> bestDerivations = new IdentityHashMap<TSNodeLabel, double[]>();
		for (Entry<TSNodeLabel, double[]> e : iter.next().entrySet()) {
			bestDerivations.put(e.getKey().clone(), new double[] { e.getValue()[0] });
		}

		while (iter.hasNext()) {
			HashMap<TSNodeLabel, double[]> newFragTable = iter.next();
			IdentityHashMap<TSNodeLabel, double[]> newBestDerivations = new IdentityHashMap<TSNodeLabel, double[]>();
			for (Entry<TSNodeLabel, double[]> d : bestDerivations.entrySet()) {
				TSNodeLabel partialDer = d.getKey();
				double logProb1 = d.getValue()[0];
				for (Entry<TSNodeLabel, double[]> f : newFragTable.entrySet()) {
					TSNodeLabel frag1 = partialDer.clone();
					TSNodeLabel frag2 = f.getKey().clone();
					double logProb2 = f.getValue()[0];
					TSNodeLabel newDer = combineFrags(frag1, frag2);
					double newLogProb = logProb1 + logProb2;
					newBestDerivations.put(newDer, new double[] { newLogProb });
				}
			}
			bestDerivations = newBestDerivations;
		}

		for (Entry<TSNodeLabel, double[]> e : bestDerivations.entrySet()) {
			Utility.increaseInHashMap(mergeProbTable, e.getKey(), Math.exp(e.getValue()[0]));
		}

	}

	synchronized void appendInLogFile(StringBuilder sb, int index) {
		if (index == orderedDebugNextIndex) {
			Parameters.logLineFlush(sb.toString());
			orderedDebugNextIndex++;
			while (orderedDebugNextIndex < testTB.size()) {
				sb = orderedDebugSBArray[orderedDebugNextIndex];
				if (sb == null)
					break;
				Parameters.logLineFlush(sb.toString());
				orderedDebugSBArray[orderedDebugNextIndex] = null;
				orderedDebugNextIndex++;
			}
			;
		} else
			orderedDebugSBArray[index] = sb;
	}

	public static double computePruningLogProbThreshold(double maxLogProb, int alternatives) {
		double logF = Math.log(pruningGamma * Math.pow(alternatives, pruningExp));
		if (logF > 0) {
			System.err.println("LogF>0: alternatives " + alternatives);
		}
		return maxLogProb + logF;
	}

	public static <T> void pruneLogProbTable(HashMap<T, double[]> table) {
		double maxLogProb = Utility.getMaxValue(table);
		int size = table.size();
		double minLogProbThreshold = computePruningLogProbThreshold(maxLogProb, size);
		Iterator<Entry<T, double[]>> iter = table.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<T, double[]> nextEntry = iter.next();
			if (nextEntry.getValue()[0] < minLogProbThreshold)
				iter.remove();
		}
	}

	public static void pruneProbBoxDoubleTableViterbi(HashMap<TermLabel, HashMap<ChartState, ProbBox>> scanStatesSet, int[] totalPruned) {

		Entry<ChartState, ProbBox> maxState = ProbBox.getMaxForwardViterbiEntryDouble(scanStatesSet);
		if (maxState == null)
			return;
		double maxLogProb = maxState.getValue().forwardViterbiProb; // forwardProb;
																	// //;
																	// //innerViterbiProb;
		int totalSize = Utility.countTotalDouble(scanStatesSet);
		double minLogProbThreshold = computePruningLogProbThreshold(maxLogProb, totalSize);
		Iterator<Entry<TermLabel, HashMap<ChartState, ProbBox>>> iterA = scanStatesSet.entrySet().iterator();
		int pruned = 0;
		while (iterA.hasNext()) {
			Entry<TermLabel, HashMap<ChartState, ProbBox>> nextIterA = iterA.next();
			HashMap<ChartState, ProbBox> nextIterAValue = nextIterA.getValue();
			Iterator<Entry<ChartState, ProbBox>> iterB = nextIterAValue.entrySet().iterator();
			while (iterB.hasNext()) {
				Entry<ChartState, ProbBox> iterBnext = iterB.next();
				if (iterBnext.getValue().forwardViterbiProb < minLogProbThreshold) { // forwardProb
																						// forwardViterbiProb
																						// innerViterbiProb;
					iterB.remove();
					pruned++;
				}
			}
			if (nextIterAValue.isEmpty())
				iterA.remove();
		}

		totalPruned[0] += totalSize;
		totalPruned[1] += pruned;
	}

	static class ProbBox {

		double innerMargProb, forwardMargProb;
		Double outerMargProb;
		double innerViterbiProb; // what Stolke defines viterbi
		// ( it represents the prob. of the best path from the time the state
		// was created)
		double forwardViterbiProb;
		// ( it represents the prob. of the best path from the start operation)
		ChartState previousState;

		// ( viterbi best previous state )

		public ProbBox(double innerProb, double forwardProb, double innerViterbiProb, double forwardViterbiProb, ChartState previousState) {
			this.innerMargProb = innerProb;
			this.forwardMargProb = forwardProb;
			this.innerViterbiProb = innerViterbiProb;
			this.forwardViterbiProb = forwardViterbiProb;
			this.previousState = previousState;
			this.outerMargProb = Double.NaN;
		}

		public double inXout() {
			return innerMargProb + outerMargProb;
		}

		public static <S, T> int countTotalDoubleAlive(HashMap<S, HashMap<T, ProbBox>> table) {
			int count = 0;
			for (HashMap<T, ProbBox> set : table.values()) {
				for (ProbBox pb : set.values()) {
					if (!pb.outerMargProb.isNaN())
						count++;
				}
			}
			return count;
		}

		public static <S, T> int countTotalDouble(HashMap<S, HashMap<T, ProbBox>> table) {
			int count = 0;
			for (HashMap<T, ProbBox> set : table.values()) {
				count += set.size();
			}
			return count;
		}

		public static <T, C> Entry<C, ProbBox> getMaxViterbiEntryDouble(HashMap<T, HashMap<C, ProbBox>> doubleTable) {

			double max = 0;
			Entry<C, ProbBox> result = null;
			for (Entry<T, HashMap<C, ProbBox>> e : doubleTable.entrySet()) {
				Entry<C, ProbBox> bestSubEntry = getMaxInnerViterbiEntry(e.getValue());
				double viterbi = bestSubEntry.getValue().innerViterbiProb;
				if (result == null || viterbi > max) {
					max = viterbi;
					result = bestSubEntry;
				}
			}
			return result;
		}

		public static <C> Entry<C, ProbBox> getMaxInnerViterbiEntry(HashMap<C, ProbBox> table) {

			Iterator<Entry<C, ProbBox>> e = table.entrySet().iterator();
			Entry<C, ProbBox> result = e.next();
			double max = result.getValue().innerViterbiProb;
			while (e.hasNext()) {
				Entry<C, ProbBox> next = e.next();
				double viterbi = next.getValue().innerViterbiProb;
				if (viterbi > max) {
					max = viterbi;
					result = next;
				}
			}
			return result;
		}

		public static <T, C> Entry<C, ProbBox> getMaxForwardViterbiEntryDouble(HashMap<T, HashMap<C, ProbBox>> doubleTable) {

			if (doubleTable.isEmpty())
				return null;

			Iterator<Entry<T, HashMap<C, ProbBox>>> e = doubleTable.entrySet().iterator();
			Entry<C, ProbBox> result = getMaxForwardViterbiEntry(e.next().getValue());
			double max = result.getValue().forwardViterbiProb;

			while (e.hasNext()) {
				Entry<C, ProbBox> bestSubEntry = getMaxForwardViterbiEntry(e.next().getValue());
				double viterbi = bestSubEntry.getValue().forwardViterbiProb;
				if (viterbi > max) {
					max = viterbi;
					result = bestSubEntry;
				}
			}
			return result;
		}

		public static <T, C> double getMaxForwardViterbiDouble(HashMap<T, HashMap<C, ProbBox>> doubleTable) {

			if (doubleTable.isEmpty())
				return Double.NEGATIVE_INFINITY;

			Iterator<Entry<T, HashMap<C, ProbBox>>> e = doubleTable.entrySet().iterator();
			Entry<C, ProbBox> result = getMaxForwardViterbiEntry(e.next().getValue());
			double max = result.getValue().forwardViterbiProb;

			while (e.hasNext()) {
				Entry<C, ProbBox> bestSubEntry = getMaxForwardViterbiEntry(e.next().getValue());
				double viterbi = bestSubEntry.getValue().forwardViterbiProb;
				if (viterbi > max) {
					max = viterbi;
					result = bestSubEntry;
				}
			}
			return max;
		}

		public static <C> Entry<C, ProbBox> getMaxForwardViterbiEntry(HashMap<C, ProbBox> table) {

			Iterator<Entry<C, ProbBox>> e = table.entrySet().iterator();
			Entry<C, ProbBox> result = e.next();
			double max = result.getValue().forwardViterbiProb;
			while (e.hasNext()) {
				Entry<C, ProbBox> next = e.next();
				double viterbi = next.getValue().forwardViterbiProb;
				if (viterbi > max) {
					max = viterbi;
					result = next;
				}
			}
			return result;
		}

		public static <C> double[] getTotalSumInnerMargProbForwardMargProb(HashMap<C, ProbBox> table) {
			Iterator<ProbBox> iter = table.values().iterator();
			ProbBox pb = iter.next();
			double innerMax = pb.innerMargProb;
			double forwardMax = pb.innerMargProb;
			int size = table.size() - 1;
			double[] innerOther = new double[size];
			double[] forwardOther = new double[size];
			int i = 0;
			while (iter.hasNext()) {
				pb = iter.next();
				double nextInner = pb.innerMargProb;
				if (nextInner > innerMax) {
					innerOther[i] = innerMax;
					innerMax = nextInner;
				} else
					innerOther[i] = nextInner;
				double nextForward = pb.forwardMargProb;
				if (nextForward > forwardMax) {
					forwardOther[i] = forwardMax;
					forwardMax = nextForward;
				} else
					forwardOther[i] = nextForward;
				i++;
			}
			double logSumInnerProb = Utility.logSum(innerOther, innerMax);
			double logSumForwardProb = Utility.logSum(forwardOther, forwardMax);
			return new double[] { logSumInnerProb, logSumForwardProb };
		}

		public static <C> double getTotalSumForwardMargProb(HashMap<C, ProbBox> table) {

			Iterator<ProbBox> iter = table.values().iterator();
			ProbBox pb = iter.next();
			double forwardMax = pb.forwardMargProb;
			int size = table.size() - 1;
			double[] forwardOther = new double[size];
			int i = 0;
			while (iter.hasNext()) {
				pb = iter.next();
				double nextForward = pb.forwardMargProb;
				if (nextForward > forwardMax) {
					forwardOther[i] = forwardMax;
					forwardMax = nextForward;
				} else
					forwardOther[i] = nextForward;
				i++;
			}
			return Utility.logSum(forwardOther, forwardMax);
		}

		public static <C> double getTotalSumInxOutProb(HashMap<C, ProbBox> table) {

			Iterator<ProbBox> iter = table.values().iterator();
			ProbBox pb = iter.next();
			double inXoutMax = pb.inXout();
			int size = table.size() - 1;
			double[] inXoutOther = new double[size];
			int i = 0;
			while (iter.hasNext()) {
				pb = iter.next();
				double nextInXout = pb.inXout();
				if (nextInXout > inXoutMax) {
					inXoutOther[i] = inXoutMax;
					inXoutMax = nextInXout;
				} else
					inXoutOther[i] = nextInXout;
				i++;
			}
			return Utility.logSum(inXoutOther, inXoutMax);
		}

		public static <C> double getTotalSumInnerMargProb(HashMap<C, ProbBox> table) {

			Iterator<ProbBox> iter = table.values().iterator();
			ProbBox pb = iter.next();
			double innerMax = pb.innerMargProb;
			int size = table.size() - 1;
			double[] innerOthers = new double[size];
			int i = 0;
			while (iter.hasNext()) {
				pb = iter.next();
				double nextInner = pb.innerMargProb;
				if (nextInner > innerMax) {
					innerOthers[i] = innerMax;
					innerMax = nextInner;
				} else
					innerOthers[i] = nextInner;
				i++;
			}
			return Utility.logSum(innerOthers, innerMax);
		}

		public static <C> double getTotalSumForwardMargProbDouble(HashMap<TermLabel, HashMap<C, ProbBox>> statesTable) {

			int size = ProbBox.countTotalDouble(statesTable) - 1;

			if (size == -1) {
				// zero elements
				return Double.NEGATIVE_INFINITY;
			}

			if (size == 0) {
				// only one element
				return statesTable.values().iterator().next().values().iterator().next().forwardMargProb;
			}

			double innerMargMax = -Double.MAX_VALUE;
			double[] innerMargOthers = new double[size];
			boolean first = true;

			int i = 0;
			for (HashMap<C, ProbBox> subTable : statesTable.values()) {
				for (ProbBox pb : subTable.values()) {
					double nextInnerMarg = pb.forwardMargProb;
					if (first) {
						first = false;
						innerMargMax = nextInnerMarg;
						continue;
					}
					if (nextInnerMarg > innerMargMax) {
						innerMargOthers[i++] = innerMargMax;
						innerMargMax = nextInnerMarg;
					} else
						innerMargOthers[i++] = nextInnerMarg;
				}
			}
			return Utility.logSum(innerMargOthers, innerMargMax);
		}

		public static <C> double getTotalSumInnerOuterMargProb(HashMap<TermLabel, HashMap<C, ProbBox>> statesTable) {

			int size = // Utility.countTotalDouble(statesTable) - 1;
			ProbBox.countTotalDoubleAlive(statesTable) - 1;

			double inXoutMax = -Double.MAX_VALUE;
			double[] inXoutOthers = new double[size];
			boolean first = true;

			int i = 0;
			for (HashMap<C, ProbBox> subTable : statesTable.values()) {
				for (ProbBox pb : subTable.values()) {
					if (pb.outerMargProb.isNaN())
						continue;
					double nextInXout = pb.inXout();
					if (first) {
						first = false;
						inXoutMax = nextInXout;
						continue;
					}
					if (nextInXout > inXoutMax) {
						inXoutOthers[i++] = inXoutMax;
						inXoutMax = nextInXout;
					} else
						inXoutOthers[i++] = nextInXout;
				}
			}
			return Utility.logSum(inXoutOthers, inXoutMax);
		}

		/**
		 * 
		 * @return the ProbBox which is introduced or modified as the result of
		 *         this operation (null if the the table is not modified)
		 */

		public static boolean addProbState(HashMap<TermLabel, HashMap<ChartState, ProbBox>> table, TermLabel firstKey, ChartState currentState, double currentInnerProb, double currentForwardProb, double currentInnerViterbiProb, double currentForwardViterbiProb, ChartState currentPreviousState) {

			HashMap<ChartState, ProbBox> subTable = table.get(firstKey);
			if (subTable == null) {
				subTable = new HashMap<ChartState, ProbBox>();
				table.put(firstKey, subTable);
				ProbBox newPb = new ProbBox(currentInnerProb, currentForwardProb, currentInnerViterbiProb, currentForwardViterbiProb, currentPreviousState);
				subTable.put(currentState, newPb);
				currentState.probBox = newPb;
				return true;
			}

			ProbBox probPresent = subTable.get(currentState);
			if (probPresent == null) {
				ProbBox pb = new ProbBox(currentInnerProb, currentForwardProb, currentInnerViterbiProb, currentForwardViterbiProb, currentPreviousState);
				subTable.put(currentState, pb);
				currentState.probBox = pb;
				return true;
			}
			probPresent.innerMargProb = Utility.logSum(probPresent.innerMargProb, currentInnerProb);
			probPresent.forwardMargProb = Utility.logSum(probPresent.forwardMargProb, currentForwardProb);
			if (probPresent.innerViterbiProb < currentInnerViterbiProb) {
				probPresent.innerViterbiProb = currentInnerViterbiProb;
				assert (probPresent.forwardViterbiProb - currentForwardViterbiProb < 0.00000001);
				probPresent.forwardViterbiProb = currentForwardViterbiProb;
				probPresent.previousState = currentPreviousState;
				currentState.probBox = probPresent;
				return true;
			}
			assert (currentForwardViterbiProb - probPresent.forwardViterbiProb < 0.00000001);

			currentState.probBox = probPresent;
			return false;
		}

		public void logSumOuter(double outerContribution) {
			outerMargProb = Utility.logSum(outerMargProb, outerContribution);
		}

		public String toStringForChart(boolean normalProb) {
			// double viterbi = normalProb ? Math
			// .exp(innerViterbiProb) : innerViterbiProb;
			// double totVit = normalProb ? Math
			// .exp(forwardViterbiProb)
			// : forwardViterbiProb;
			// String vitString = Float.toString((float) viterbi);
			// String totVitString = Float.toString((float) totVit);
			// return " [" + vitString + ", " + totVitString + "] ";
			double margForward = normalProb ? Math.exp(forwardMargProb) : forwardMargProb;
			double margIn = normalProb ? Math.exp(innerMargProb) : innerMargProb;
			double margOut = normalProb ? Math.exp(outerMargProb) : outerMargProb;
			String forwardString = Float.toString((float) margForward);
			String inString = Float.toString((float) margIn);
			String outString = Float.toString((float) margOut);
			return " [" + forwardString + ", " + inString + ", " + outString + "] ";
		}

	}

	protected class ChartParserThread extends Thread {

		int sentenceIndex;
		TSNodeLabel goldTestTree;

		ArrayList<Label> wordLabels;
		TermLabel[] wordTermLabels;
		HashSet<TermLabel> wordTermLabelsSet;
		int sentenceLength;

		// Vector<HashMap<TermLabel,HashMap<FragFringe,double[]>>>
		// wordFragFringeFirstLex,
		// wordFragFringeFirstSub;

		Vector<ChartColumn> chart;

		int[][] fringeCounterFirstLexSubBin;
		int[] totalFringesLexSub, totalFringesSmoothingLexSub,
				removedFringesLexSub;
		boolean parsingSuccess, potentiallyReachingTop, actuallyReachedTop;
		boolean finished = false;
		SimpleTimer currentWordTimer = new SimpleTimer(),
				treeTimer = new SimpleTimer();
		int[][] wordScanSize;
		int[][] wordSubDownFirstSize;
		int[][] wordSubDownSecondSize;
		int[][] wordSubUpFirstSize;
		int[][] wordSubUpSecondSize;
		int[][] wordCompleteSize;

		int[] totalPrunedSentence;

		String[] wordTimes;
		String initChartTime;
		int[] countSubDownSubUpSentence;
		StringBuilder debugSB = new StringBuilder();

		TSNodeLabel MPDtree, MPPtree, MRP_prod_tree; // , MRP_sum_tree;
		ArrayList<ChartState> derivationPath;
		ArrayList<TSNodeLabel> derivation;
		double[] MPDLogProb, MPPLogProb;
		ArrayList<Integer> numberOfPossibleFrags;
		ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags;
		int treeSubDerivations;
		TSNodeLabel[] incrementalTrees;
		double[] prefixProb;

		double sentenceLogProb;

		public void run() {
			// ArrayList<Label> sentenceWords = null;
			int[] i = { 0 };
			// while ( (sentenceWords = getNextSentence(i)) != null) {
			while ((goldTestTree = getNextTree(i)) != null) {
				sentenceIndex = i[0];
				ArrayList<Label> lex = goldTestTree.collectLexicalLabels();
				initVariables(lex);
				parse();
			}
		}

		private void initVariables(ArrayList<Label> sentenceWords) {
			this.wordLabels = sentenceWords;
			this.sentenceLength = wordLabels.size();
			wordTermLabels = new TermLabel[sentenceLength];
			wordTermLabelsSet = new HashSet<TermLabel>();
			int i = 0;
			for (Label l : wordLabels) {
				TermLabel tl = TermLabel.getTermLabel(l, true);
				wordTermLabels[i++] = tl;
				wordTermLabelsSet.add(tl);
			}
			wordTimes = new String[sentenceLength + 1];
			wordScanSize = new int[2][sentenceLength + 1];
			wordSubDownFirstSize = new int[2][sentenceLength + 1];
			wordSubDownSecondSize = new int[2][sentenceLength + 1];
			wordSubUpFirstSize = new int[2][sentenceLength + 1];
			wordSubUpSecondSize = new int[2][sentenceLength + 1];
			wordCompleteSize = new int[2][sentenceLength + 1];

			totalPrunedSentence = new int[2];
			Arrays.fill(wordTimes, "-");

			chart = new Vector<ChartColumn>(sentenceLength + 1); // termstate

			fringeCounterFirstLexSubBin = new int[2][sentenceLength];
			removedFringesLexSub = new int[2];
			totalFringesLexSub = new int[2];
			totalFringesSmoothingLexSub = new int[2];
			this.parsingSuccess = false;
			this.potentiallyReachingTop = false;
			this.actuallyReachedTop = false;
			this.finished = false;
			this.countSubDownSubUpSentence = new int[2];

			MPDtree = null;
			MPPtree = null;
			MRP_prod_tree = null;
			derivationPath = null;
			derivation = null;
			MPDLogProb = new double[] { -Double.MAX_VALUE };
			MPPLogProb = new double[] { -Double.MAX_VALUE };
			numberOfPossibleFrags = new ArrayList<Integer>();
			possibleFrags = new ArrayList<HashMap<TSNodeLabel, double[]>>();
			treeSubDerivations = 1;
			incrementalTrees = null;

			sentenceLogProb = Double.NaN;

		}

		private void parse() {
			treeTimer.start();

			initChart();

			ChartColumn startChartColumn = chart.get(0);
			startChartColumn.performStart();

			for (int i = 0; i <= sentenceLength; i++) {
				currentWordTimer.start();
				ChartColumn c = chart.get(i);
				if (i == 0)
					c.performScan();
				else if (i < sentenceLength)
					c.completeSubDownSubUpScan();
				else
					c.completeAndTerminate();
				c.countFragFringes();
				currentWordTimer.stop();
				wordTimes[i] = currentWordTimer.readTotalTimeSecFormat();
				if (isInterrupted())
					break;
			}

			treeTimer.stop();

			// HashMap<HashSet<ChartState>,double[]> derivationPaths = null;
			// double parseProb = 0;

			// parsingSuccess = false;

			if (parsingSuccess) {

				// derivationPath =
				// retrieveRandomBestViterbiPath(viterbiLogProb);
				derivationPath = retrieveRandomViterbiPath(MPDLogProb);
				// assert derivationPathOld.equals(derivationPath);
				derivation = retrieveFragDerivation(derivationPath, numberOfPossibleFrags, possibleFrags, MPDLogProb, false);
				treeSubDerivations = Utility.product(numberOfPossibleFrags);
				MPDtree = combineDerivation(derivation);

				if (computeMinRiskTree) {
					computeOuterProbabilities();
					// checkFinalProbConsistency();
					sentenceLogProb = ProbBox.getTotalSumInnerOuterMargProb(chart.get(sentenceLength).subUpFirstStatesSet);
					computeMinRiskParses(); // needs sentenceLogProb
				}

				if (MPDtree.label == TOPlabel)
					actuallyReachedTop = true;

				if (computeMPPtree) {
					countFirstLexSubOperations(derivationPath);
					HashMap<TSNodeLabel, double[]> derivationPaths = retrieveAllFinalViterbiDerivations();
					// HashMap<TSNodeLabel, double[]> derivationPaths =
					// retrieveAllDerivations();
					Entry<TSNodeLabel, double[]> MPPtreeMaxEntry = Utility.getMaxEntry(derivationPaths);
					MPPtree = MPPtreeMaxEntry.getKey();
					MPPLogProb = MPPtreeMaxEntry.getValue();

				}

			}

			if (computeIncrementalEvaluation) {
				incrementalTrees = new TSNodeLabel[sentenceLength];
				for (int i = 0; i < sentenceLength; i++) {
					double[] viterbiLogProbPartial = new double[] { -Double.MAX_VALUE };
					ArrayList<ChartState> partialPath = retrieveRandomViterbiPartialPath(i + 1, viterbiLogProbPartial);
					if (partialPath == null)
						incrementalTrees[i] = null;
					else {
						ArrayList<TSNodeLabel> partialDer = retrieveFragDerivation(partialPath, null, null, viterbiLogProbPartial, false);
						TSNodeLabel tree = combineDerivation(partialDer);
						incrementalTrees[i] = tree;
					}
				}
			}

			if (computePrefixProbabilities) {
				computePrefixProbabilities();
			}

			if (debug)
				printDebug();
			// printDebug(parsedTree, null, null,
			// parseProb, null, null);

			updateMainResults();

		}

		private void updateMainResults() {
			synchronized (progress) {
				if (Thread.interrupted() && !finished)
					interruptedTrees++;
				totalSubDerivations += treeSubDerivations;
				if (treeSubDerivations > 1)
					ambiguousViterbiDerivations++;
				if (computeIncrementalEvaluation) {
					partialParsedTB[sentenceIndex] = incrementalTrees;
				}
				if (computePrefixProbabilities) {
					sentenceprefixProb[sentenceIndex] = prefixProb;
				}
				if (parsingSuccess) {
					parsedTrees++;
					Utility.arrayIntPlus(countSubDownSubUpSentence, countSubDownSubUpTotal);
					Utility.arrayIntPlus(totalPrunedSentence, totalPruned);
					if (actuallyReachedTop)
						actuallyReachedTopTrees++;
					if (potentiallyReachingTop)
						potentiallyReachingTopTrees++;
				}
				parsedTB_MPD[sentenceIndex] = MPDtree;
				if (computeMinRiskTree) {
					parsedTB_MRP_prod[sentenceIndex] = MRP_prod_tree;
					// parsedTB_MRP_sum[sentenceIndex] = MRP_sum_tree;
				}
				if (computeMPPtree) {
					parsedTB_MPP[sentenceIndex] = MPPtree;
				}
				sentenceLogProbTestSet[sentenceIndex] = sentenceLogProb;
				progress.next();

			}

		}

		private void initChart() {
			SimpleTimer t = new SimpleTimer(true);
			int i = 0;
			for (Label w : wordLabels) {
				TermLabel wordTermLabel = TermLabel.getTermLabel(w, true);
				chart.add(new ChartColumn(wordTermLabel, i));
				i++;
			}

			chart.add(new ChartColumn(null, i));
			initChartTime = t.checkEllapsedTimeSecFormat();
		}

		public void toStringTexChartHorizontal(PrintWriter pw, Vector<ArrayList<ChartState>> derPaths, boolean onlyDerivation, boolean filterSeconFrags, boolean normalProb) {

			pw.print("\\begin{tabular}{|c|" + Utility.repeat("ll|", sentenceLength) + "l|}\n");
			pw.print("\\hline\n");

			int i = 0;
			for (Label l : wordLabels) {
				pw.print("& \\multicolumn{2}{c|}{ " + "\\begin{tabular}{c} " + "\\Huge{(" + i + ")} \\\\ " + "\\Huge{" + l.toString() + "}" + "\\end{tabular} }");
				i++;
			}
			pw.print("& \\multicolumn{1}{c|}{\\Huge{STOP}}");
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SCAN} &\n");
			HashMap<TermLabel, HashMap<ChartState, ProbBox>> scanSet = chart.get(i).scanStatesSet;
			for (i = 0; i < sentenceLength; i++) {
				pw.print("\\multicolumn{2}{l|}{ " + toStringTexTable(scanSet, derPaths, onlyDerivation, normalProb) + "}");
				pw.print(" & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SUB-DOWN} &\n");
			for (i = 0; i < sentenceLength; i++) {
				ChartColumn cc = chart.get(i);
				pw.print(toStringTexTable(cc.subDownFirstStatesSet, derPaths, onlyDerivation, normalProb));
				pw.print(" & \n");
				pw.print(toStringFragTex(cc.firstLexFragFringeSet, derPaths, i, true, onlyDerivation, normalProb, filterSeconFrags ? scanSet : null));
				pw.print(" & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{SUB-UP} &\n");
			for (i = 0; i <= sentenceLength; i++) {
				ChartColumn cc = chart.get(i);
				if (i < sentenceLength) {
					pw.print(toStringTexTable(chart.get(i).subUpFirstStatesSet, derPaths, onlyDerivation, normalProb));
					pw.print(" & \n");
					pw.print(toStringFragTex(cc.firstSubFragFringeSet, derPaths, i, false, onlyDerivation, normalProb, filterSeconFrags ? scanSet : null));
					pw.print(" & \n");
				} else {
					pw.print("\\multicolumn{1}{l|}{ " + toStringTexTable(chart.get(i).subUpFirstStatesSet, derPaths, onlyDerivation, normalProb) + "}");
					pw.print("\n");
				}
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\Huge{COMPLETE} &\n");
			for (i = 0; i <= sentenceLength; i++) {
				int cols = i == sentenceLength ? 1 : 2;
				pw.print("\\multicolumn{" + cols + "}{l|}{ " + toStringTexTable(chart.get(i).completeStatesSet, derPaths, onlyDerivation, normalProb) + "}");
				pw.print(i == sentenceLength ? "\n" : " & \n");
			}
			pw.print("\\\\\n");
			pw.print("\\hline\n");
			pw.print("\\end{tabular}\n");
		}

		public void toStringTexChartVertical(PrintWriter pw, Vector<ArrayList<ChartState>> derPaths, boolean onlyDerivation, boolean filterSecondFrags, boolean normalProb, boolean printSecondFrags) {

			String tabularStartString = printSecondFrags ? "\\begin{tabular}{|c|ll|}\n" : "\\begin{tabular}{|c|l|}\n";
			pw.print(tabularStartString);

			for (int i = 0; i <= sentenceLength; i++) {
				String word = i < sentenceLength ? wordLabels.get(i).toString() : "STOP";
				ChartColumn cc = chart.get(i);
				pw.print("\\hline\n");

				pw.print("& \\multicolumn{" + (printSecondFrags ? "2" : "1") + "}{c|}{ ");
				pw.print("\\begin{tabular}{c} " + "(" + i + ") \\\\ " + word + "\\end{tabular}");
				pw.print(" }");
				pw.print("\\\\\n");
				pw.print("\\hline\n");

				pw.print("SCAN &\n");
				HashMap<TermLabel, HashMap<ChartState, ProbBox>> scanSet = chart.get(i).scanStatesSet;
				if (printSecondFrags)
					pw.print("\\multicolumn{2}{l|}{ ");
				pw.print(toStringTexTable(scanSet, derPaths, onlyDerivation, normalProb));
				if (printSecondFrags)
					pw.print(" }");
				pw.print("\\\\\n");

				pw.print("\\hline\n");
				pw.print("SUB-DOWN &\n");
				pw.print(toStringTexTable(cc.subDownFirstStatesSet, derPaths, onlyDerivation, normalProb));
				if (printSecondFrags) {
					pw.print(" & \n");
					pw.print(toStringFragTex(cc.firstLexFragFringeSet, derPaths, i, true, onlyDerivation, normalProb, filterSecondFrags ? scanSet : null));
				}
				pw.print("\\\\\n");

				pw.print("\\hline\n");
				pw.print("SUB-UP &\n");
				pw.print(toStringTexTable(chart.get(i).subUpFirstStatesSet, derPaths, onlyDerivation, normalProb));
				if (printSecondFrags) {
					pw.print(" & \n");
					pw.print(toStringFragTex(cc.firstSubFragFringeSet, derPaths, i, false, onlyDerivation, normalProb, filterSecondFrags ? scanSet : null));
				}
				pw.print("\\\\\n");

				pw.print("\\hline\n");
				pw.print("COMPLETE &\n");
				if (printSecondFrags)
					pw.print("\\multicolumn{2}{l|}{ ");
				pw.print(toStringTexTable(chart.get(i).completeStatesSet, derPaths, onlyDerivation, normalProb));
				if (printSecondFrags)
					pw.print(" }");
				pw.print("\\\\\n");
				pw.print("\\hline\n\n");

				pw.print("\\multicolumn{" + (printSecondFrags ? "3" : "2") + "}{c}{}");
				pw.print("\\\\");
			}

			pw.print("\\end{tabular}\n");
		}

		private int totalElementsInChart() {
			return Utility.sum(wordSubDownFirstSize[1]) + Utility.sum(wordSubDownSecondSize[1]) + Utility.sum(wordSubUpFirstSize[1]) + Utility.sum(wordSubUpSecondSize[1]) + Utility.sum(wordCompleteSize[1]);
		}

		private String toStringTex(ArrayList<TSNodeLabel> derivation) {
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{" + Utility.repeat("c", derivation.size()) + "}\n");
			Iterator<TSNodeLabel> iter = derivation.iterator();
			while (iter.hasNext()) {
				sb.append(iter.next().toStringTex(false, true));
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
			while (iter.hasNext()) {
				HashMap<TSNodeLabel, double[]> next = iter.next();
				sb.append("\t\\begin{tabular}{" + Utility.repeat("l", next.size()) + "}\n");
				Iterator<Entry<TSNodeLabel, double[]>> iter2 = next.entrySet().iterator();
				while (iter2.hasNext()) {
					Entry<TSNodeLabel, double[]> e = iter2.next();
					sb.append("\t(" + e.getValue()[0] + ") " + e.getKey().toStringTex(false, true));
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

		public String toStringTexTable(HashMap<TermLabel, HashMap<ChartState, ProbBox>> table, Vector<ArrayList<ChartState>> derPaths, boolean onlyDerivation, boolean normalProb) {

			int derPathSize = derPaths == null ? 0 : derPaths.size();
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{llll}\n");
			for (HashMap<ChartState, ProbBox> csSet : table.values()) {
				for (Entry<ChartState, ProbBox> csProb : csSet.entrySet()) {
					ChartState cs = csProb.getKey();
					int derPathIndex = -1;
					if (derPaths != null) {
						int i = 0;
						for (ArrayList<ChartState> dp : derPaths) {
							if (dp.contains(cs)) {
								derPathIndex = i;
								break;
							}
							i++;
						}
					}
					if (derPathIndex != -1) {
						String initString = (!onlyDerivation || derPathSize > 1) && derPathIndex == 0 ? "\t" + highlight : "\t";
						ProbBox pb = csProb.getValue();
						sb.append(initString + cs.toStringTex() + pb.toStringForChart(normalProb) + "\\\\\n");
					} else if (!onlyDerivation) {
						ProbBox pb = csProb.getValue();
						sb.append("\t" + cs.toStringTex() + pb.toStringForChart(normalProb) + "\\\\\n");
					}
				}
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}

		public String toStringFragTex(HashMap<TermLabel, HashMap<FragFringe, double[]>> table, Vector<ArrayList<ChartState>> derPaths, int wordIndex, boolean subDown, boolean onlyDerivation, boolean normalProb, HashMap<TermLabel, HashMap<ChartState, ProbBox>> scanSet) {

			int derPathSize = derPaths == null ? 0 : derPaths.size();
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{tabular}{lll}\n");
			for (HashMap<FragFringe, double[]> set : table.values()) {
				for (Entry<FragFringe, double[]> e : set.entrySet()) {
					FragFringe ff = e.getKey();
					ChartState cs = chart.get(wordIndex).new ChartState(ff, subDown ? wordIndex : -1);

					if (!subDown)
						cs.dotIndex++;
					int derPathIndex = -1;
					if (derPaths != null) {
						int i = 0;
						for (ArrayList<ChartState> dp : derPaths) {
							if (dp.contains(cs)) {
								derPathIndex = i;
								break;
							}
							i++;
						}
					}
					if (!onlyDerivation && derPathIndex == -1 && scanSet != null) {
						HashMap<ChartState, ProbBox> subScanSet = scanSet.get(ff.root);
						if (subScanSet != null) {
							for (ChartState scanState : subScanSet.keySet()) {
								if (scanState.fragFringe.equals(ff)) {
									derPathIndex = 100;
									break;
								}
							}
						}
					}

					if (derPathIndex != -1) {
						String initString = (!onlyDerivation || derPathSize > 1) && derPathIndex == 0 ? "\t" + highlight : "\t";
						// double vit = normalProb ? Math.exp(e.getValue()[0]) :
						// e
						// .getValue()[0];
						// String vitString = Float.toString((float) vit);
						// sb.append(initString + ff.toStringTex() + " ["
						// + vitString + "]" + "\\\\\n");
						double marg = normalProb ? Math.exp(e.getValue()[1]) : e.getValue()[1];
						String margString = Float.toString((float) marg);
						sb.append(initString + ff.toStringTex() + " [" + margString + "]" + "\\\\\n");
					} else if (!onlyDerivation && scanSet == null) {
						// double vit = normalProb ? Math.exp(e.getValue()[0]) :
						// e
						// .getValue()[0];
						// String vitString = Float.toString((float) vit);
						// sb.append("\t" + ff.toStringTex() + " [" + vitString
						// + "]" + "\\\\\n");
						double marg = normalProb ? Math.exp(e.getValue()[1]) : e.getValue()[1];
						String margString = Float.toString((float) marg);
						sb.append("\t" + ff.toStringTex() + " [" + margString + "]" + "\\\\\n");
					}

				}
			}
			sb.append("\\end{tabular}\n");
			return sb.toString();
		}

		private void checkFinalProbConsistency() {
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<TermLabel, HashMap<ChartState, ProbBox>> endStates = lastColumn.subUpFirstStatesSet;
			for (HashMap<ChartState, ProbBox> finalStateTable : endStates.values()) {
				for (Entry<ChartState, ProbBox> e : finalStateTable.entrySet()) {
					ProbBox pb = e.getValue();
					double diff = Math.abs(pb.innerViterbiProb - pb.forwardViterbiProb);
					if (diff > 0.00000001)
						System.err.println("Bad mistake: " + sentenceIndex + " " + diff);
				}
			}
			int i = 0;
			for (ChartColumn col : chart) {
				double sentenceProbAlt = ProbBox.getTotalSumInnerOuterMargProb(col.scanStatesSet);
				double diff = Math.abs(sentenceLogProb - sentenceProbAlt);
				if (diff > 0.00000001) {
					System.err.println(sentenceIndex + " " + diff);
				}
				if (++i == sentenceLength)
					break;
			}
		}

		private void computePrefixProbabilities() {
			prefixProb = new double[sentenceLength + 1];
			int i = 0;
			for (; i < sentenceLength; i++) {
				ChartColumn col = chart.get(i);
				prefixProb[i] = // ProbBox.getMaxForwardViterbiDouble(col.scanStatesSet);
				ProbBox.getTotalSumForwardMargProbDouble(col.scanStatesSet);
			}
			ChartColumn lastCol = chart.get(i);
			prefixProb[i] = // ProbBox.getMaxForwardViterbiDouble(lastCol.subUpFirstStatesSet);
			ProbBox.getTotalSumForwardMargProbDouble(lastCol.subUpFirstStatesSet);
		}

		private class ChartStateColumnIndexSubScritpIndexPriorityQueue {

			int totalColumns;
			Vector<Vector<HashMap<ChartState, ProbBox>>> scanSubDownSubUp; // scan
																			// subdown
																			// subup
			Vector<Vector<HashMap<ChartState, ProbBox>>> completeChartStates;
			// int[] sizes;
			int totalSize;

			public ChartStateColumnIndexSubScritpIndexPriorityQueue() {
				this.totalColumns = sentenceLength + 1;
				scanSubDownSubUp = new Vector<Vector<HashMap<ChartState, ProbBox>>>();
				completeChartStates = new Vector<Vector<HashMap<ChartState, ProbBox>>>();
				// sizes = new int[totalColumns];
				for (int i = 0; i < totalColumns; i++) {
					Vector<HashMap<ChartState, ProbBox>> scanSubDownSubUpColumn = new Vector<HashMap<ChartState, ProbBox>>(3);
					for (int j = 0; j < 3; j++) {
						scanSubDownSubUpColumn.add(new HashMap<ChartState, ProbBox>());
					}
					scanSubDownSubUp.add(scanSubDownSubUpColumn);

					Vector<HashMap<ChartState, ProbBox>> completeColumn = new Vector<HashMap<ChartState, ProbBox>>();
					for (int j = 0; j <= i; j++) { // including the column
													// itself
						completeColumn.add(new HashMap<ChartState, ProbBox>());
					}
					completeChartStates.add(completeColumn);
				}
			}

			public boolean isEmpty() {
				return totalSize == 0;
			}

			public void add(ChartState cs, int type) { // scan=0. subdown=1,
														// subup=2, complete=3
				int columnIndex = cs.nextWordIndex();
				if (type == 3) {
					int subScriptIndex = cs.subScriptIndex;
					HashMap<ChartState, ProbBox> table = completeChartStates.get(columnIndex).get(subScriptIndex);
					assert table.get(cs) == null;
					table.put(cs, cs.probBox);
				} else {
					HashMap<ChartState, ProbBox> table = scanSubDownSubUp.get(columnIndex).get(type);
					assert table.get(cs) == null;
					table.put(cs, cs.probBox);
				}
				// sizes[columnIndex]++;
				totalSize++;
			}

			public void logSum(ChartState cs, double outerContribution, int type) {
				int columnIndex = cs.nextWordIndex();
				ProbBox present = null;
				HashMap<ChartState, ProbBox> table = null;
				if (type == 3) {
					int subScriptIndex = cs.subScriptIndex;
					table = completeChartStates.get(columnIndex).get(subScriptIndex);
					present = table.get(cs);
				} else {
					table = scanSubDownSubUp.get(columnIndex).get(type);
					present = table.get(cs);
				}
				// assert (present!=null);

				if (present == null) {
					present = cs.probBox;
					table.put(cs, present);
					// sizes[columnIndex]++;
					totalSize++;
					present.outerMargProb = outerContribution;
				} else
					present.logSumOuter(outerContribution);
			}

			public boolean contains(ChartState cs, int type) {
				int columnIndex = cs.nextWordIndex();
				if (type == 3) {
					int subScriptIndex = cs.subScriptIndex;
					HashMap<ChartState, ProbBox> table = completeChartStates.get(columnIndex).get(subScriptIndex);
					return table.containsKey(cs);
				}
				return scanSubDownSubUp.get(columnIndex).get(type).containsKey(cs);
			}

			public void computeOuterProbabilities(boolean partialDerivation) {
				for (int i = totalColumns - 1; i >= 0; i--) {
					// if (sizes[i] == 0)
					// continue;

					Vector<HashMap<ChartState, ProbBox>> nonCircularState = scanSubDownSubUp.get(i);
					for (HashMap<ChartState, ProbBox> nonCircularStateType : nonCircularState) {
						for (ChartState cs : nonCircularStateType.keySet()) {
							cs.computeOuterBackTrace(this, partialDerivation);
						}
					}

					Vector<HashMap<ChartState, ProbBox>> table = completeChartStates.get(i);
					// for (int j = i; j >= 0; j--) {
					for (int j = 0; j <= i; j++) {
						HashMap<ChartState, ProbBox> subTable = table.get(j);
						if (subTable.isEmpty())
							continue;
						Iterator<ChartState> it = subTable.keySet().iterator();
						while (it.hasNext()) {
							ChartState next = it.next();
							next.computeOuterBackTrace(this, partialDerivation);
						}
					}
				}

			}

		}

		public void computeOuterProbabilities() {
			// outerMargProb
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<TermLabel, HashMap<ChartState, ProbBox>> endStates = lastColumn.subUpFirstStatesSet;
			ChartStateColumnIndexSubScritpIndexPriorityQueue allUsedChartStateInOrder = new ChartStateColumnIndexSubScritpIndexPriorityQueue();
			for (HashMap<ChartState, ProbBox> finalStateTable : endStates.values()) {
				for (Entry<ChartState, ProbBox> e : finalStateTable.entrySet()) {
					ChartState cs = e.getKey();
					ProbBox pb = cs.probBox;
					pb.outerMargProb = 0d; // outer of final state is 1
					allUsedChartStateInOrder.add(cs, 2); // final states are
															// subUp=2
				}
			}
			allUsedChartStateInOrder.computeOuterProbabilities(false);
		}

		private void computeMinRiskParses() {

			MinRiskParseBuilder maxRiskParseBuilder = new MinRiskParseBuilder(sentenceLength, sentenceLogProb);

			// indexed by FragFring and columnIndex
			HashMap<FragFringe, HashMap<Integer, ArrayList<ChartState>>> fragFringChartStatesTable = collectAliveStatesInFringeTableStartIndex();

			int i = 0;

			for (Entry<FragFringe, HashMap<Integer, ArrayList<ChartState>>> e : fragFringChartStatesTable.entrySet()) {
				HashMap<Integer, ArrayList<ChartState>> fragFringeStatesStartIndex = e.getValue();
				FragFringe ff = e.getKey();

				HashMap<TSNodeLabel, double[]> fragTableProbFractionFirstWord = null;
				HashMap<TSNodeLabel, double[]> fragTableProbFractionFollowingWords = null;
				boolean firstTermIsLex = ff.firstTerminalIsLexical();

				if (firstTermIsLex) {
					HashMap<TSNodeLabel, double[]> initFragOriginalTable = getOriginalFrags(ff, true);
					if (initFragOriginalTable != null) {
						double margProbFirstWord = getOriginalMargProb(ff, true);
						fragTableProbFractionFirstWord = Utility.hashMapDoubleClone(initFragOriginalTable);
						for (double[] prob : fragTableProbFractionFirstWord.values()) {
							prob[0] = prob[0] - margProbFirstWord;
						}
					}
				}

				HashMap<TSNodeLabel, double[]> otherFragOriginalTable = getOriginalFrags(ff, false);
				if (otherFragOriginalTable != null) {
					double margProbFollowingWords = getOriginalMargProb(ff, false);
					fragTableProbFractionFollowingWords = Utility.hashMapDoubleClone(otherFragOriginalTable);
					for (double[] prob : fragTableProbFractionFollowingWords.values()) {
						prob[0] = prob[0] - margProbFollowingWords;
					}
				}

				for (Entry<Integer, ArrayList<ChartState>> f : fragFringeStatesStartIndex.entrySet()) {
					int startIndex = f.getKey();
					boolean firstWord = startIndex == 0 && firstTermIsLex;
					HashMap<TSNodeLabel, double[]> fragAmbiguityTable = firstWord ? fragTableProbFractionFirstWord : fragTableProbFractionFollowingWords;

					double fragMargProb = 5;
					Iterator<ChartState> iter = f.getValue().iterator();
					if (firstWord) {
						while (iter.hasNext()) {
							ChartState cs = iter.next();
							if (cs.dotIndex == 0) {
								fragMargProb = cs.computeInXOut();
								iter.remove();
								break;
							}
						}
					} else {
						boolean first = true;
						while (iter.hasNext()) {
							ChartState cs = iter.next();
							if (cs.dotIndex == 1) {
								if (first) {
									first = false;
									fragMargProb = cs.computeInXOut();
								} else
									fragMargProb = Utility.logSum(fragMargProb, cs.computeInXOut());
							}
						}
					}
					assert fragMargProb != 5;

					HashMap<Integer, HashMap<Integer, ArrayList<SpanProb>>> termIndexStartSpanProbTable = new HashMap<Integer, HashMap<Integer, ArrayList<SpanProb>>>();
					for (ChartState cs : f.getValue()) {
						cs.partitionStartIndexElementInYieldBeforeDot(false, fragMargProb, termIndexStartSpanProbTable);
					}

					// normalize probs
					for (Entry<Integer, HashMap<Integer, ArrayList<SpanProb>>> termIndexSpanArray : termIndexStartSpanProbTable.entrySet()) {
						for (ArrayList<SpanProb> tspArray : termIndexSpanArray.getValue().values()) {
							double totalLogSum = SpanProb.totalLogSum(tspArray);
							for (SpanProb tsp : tspArray) {
								tsp.prob -= totalLogSum;
							}
						}
					}

					for (Entry<TSNodeLabel, double[]> g : fragAmbiguityTable.entrySet()) {
						i++;
						TSNodeLabel frag = g.getKey();
						double fragAmbiguityPortion = g.getValue()[0];
						double fragProb = fragMargProb + fragAmbiguityPortion - sentenceLogProb;
						FragSpanChartProb fscp = new FragSpanChartProb(frag, startIndex, fragProb);
						for (Entry<Integer, HashMap<Integer, ArrayList<SpanProb>>> termIndexSpanArray : termIndexStartSpanProbTable.entrySet()) {
							int termIndex = termIndexSpanArray.getKey();
							for (ArrayList<SpanProb> tspArray : termIndexSpanArray.getValue().values()) {
								for (SpanProb tsp : tspArray) {
									fscp.addTermSpanProb(termIndex, tsp);
								}
							}
						}
						fscp.finalizeNodesProb(maxRiskParseBuilder);
					}
				}
			}

			TSNodeLabel[] MRP_prod_sum = maxRiskParseBuilder.getMinRiskParsesProdSum();
			MRP_prod_tree = MRP_prod_sum[0];
			// MRP_sum_tree = MRP_prod_sum[1];

		}

		public HashMap<FragFringe, HashMap<Integer, ArrayList<ChartState>>> collectAliveStatesInFringeTableStartIndex() {
			HashMap<FragFringe, HashMap<Integer, ArrayList<ChartState>>> result = new HashMap<FragFringe, HashMap<Integer, ArrayList<ChartState>>>();
			for (ChartColumn col : chart) {
				col.collectAliveStatesInFringeTableStartIndexStartIndex(result);
			}
			return result;
		}

		public void removeDeadStatesFromChart() {
			for (ChartColumn col : chart) {
				col.removeDeadStates();
			}
		}

		public HashMap<TSNodeLabel, double[]> retrieveAllFinalViterbiDerivations() {
			HashMap<TSNodeLabel, double[]> result = new HashMap<TSNodeLabel, double[]>();
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<TermLabel, HashMap<ChartState, ProbBox>> endStates = lastColumn.subUpFirstStatesSet;
			for (HashMap<ChartState, ProbBox> finalStateTable : endStates.values()) {
				for (Entry<ChartState, ProbBox> e : finalStateTable.entrySet()) {
					ChartState cs = e.getKey();
					double prob = e.getValue().innerViterbiProb;
					ArrayList<ChartState> path = new ArrayList<ChartState>();
					retrieveViterbiBestPathRecursiveBackTrace(path, cs, false);
					ArrayList<TSNodeLabel> derivation = retrieveFragDerivation(path, null, null, new double[] { prob }, false);
					TSNodeLabel t = combineDerivation(derivation);
					Utility.increaseInHashMapLog(result, t, prob);
				}
			}
			return result;
		}

		public HashMap<TSNodeLabel, double[]> retrieveAllDerivations() {
			HashMap<TSNodeLabel, double[]> result = new HashMap<TSNodeLabel, double[]>();
			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<TermLabel, HashMap<ChartState, ProbBox>> endStates = lastColumn.subUpFirstStatesSet;
			int j = 0;
			for (HashMap<ChartState, ProbBox> finalStateTable : endStates.values()) {
				for (Entry<ChartState, ProbBox> e : finalStateTable.entrySet()) {
					j++;
					ChartState cs = e.getKey();
					// double prob = e.getValue().innerViterbiProb;
					ArrayList<ArrayList<ChartState>> allPaths = new ArrayList<ArrayList<ChartState>>();
					ArrayList<ChartState> currentPath = new ArrayList<ChartState>();
					allPaths.add(currentPath);
					currentPath.add(cs);
					retrieveAllPathRecursive(allPaths, currentPath, cs);
					int i = 0;
					for (ArrayList<ChartState> path : allPaths) {
						i++;
						double[] prob = new double[1];
						System.out.println("Der " + i);
						for (ChartState s : path) {
							System.out.println("\t" + s);
						}
						System.out.println();
						ArrayList<TSNodeLabel> der = retrieveFragDerivation(path, null, null, prob, true);// new
																											// double[]{prob}
						TSNodeLabel t = combineDerivation(der);
						Utility.increaseInHashMapLog(result, t, prob[0]);
					}
				}
			}
			return result;
		}

		public ArrayList<ChartState> retrieveRandomViterbiPath(double[] viterbiLogProb) {

			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<ChartState, ProbBox> topStateSet = lastColumn.subUpFirstStatesSet.get(TOPnode);
			Entry<ChartState, ProbBox> topStateEntry = null;
			if (topStateSet != null)
				topStateEntry = ProbBox.getMaxInnerViterbiEntry(topStateSet);

			HashMap<TermLabel, HashMap<ChartState, ProbBox>> endStates = lastColumn.subUpFirstStatesSet;
			Entry<ChartState, ProbBox> nonTopStateEntry = ProbBox.getMaxViterbiEntryDouble(endStates);

			if (!forceTop && (topStateEntry == null || nonTopStateEntry.getValue().innerViterbiProb > topStateEntry.getValue().innerViterbiProb))
				topStateEntry = nonTopStateEntry;
			// using a non top anyway

			viterbiLogProb[0] = topStateEntry.getValue().innerViterbiProb;
			ArrayList<ChartState> path = new ArrayList<ChartState>();
			ChartState cs = topStateEntry.getKey();
			retrieveViterbiBestPathRecursiveBackTrace(path, cs, false);
			// without backtrace
			// retrieveViterbiBestPathRecursive(path,topStateEntry.getKey(),
			// false);
			return path;
		}

		public ArrayList<ChartState> retrieveRandomViterbiPartialPath(int prefixLength, double[] vit) {
			// ChartColumn nextChartColumn = chart.get(prefixLength); //after
			// last was scanned
			ChartColumn prefixLastColum = chart.get(prefixLength - 1);
			// before last wasscanned
			// look only in scan states
			// assuming look ahead 1 (all states in the scanStates have the
			// current word to be scanned)
			Entry<ChartState, ProbBox> bestScanEntryState = ProbBox.getMaxForwardViterbiEntryDouble(prefixLastColum.scanStatesSet);
			if (bestScanEntryState == null)
				return null;
			if (vit != null)
				vit[0] = bestScanEntryState.getValue().forwardViterbiProb;
			ArrayList<ChartState> path = new ArrayList<ChartState>();
			retrieveViterbiBestPathRecursiveBackTrace(path, bestScanEntryState.getKey(), true);
			return path;

		}

		public void retrieveViterbiBestPathRecursiveBackTrace(ArrayList<ChartState> path, ChartState cs, boolean partialDerivation) {
			path.add(cs);
			Duet<ChartState, ChartState> parentPairs = cs.getViterbiBestParentStatesBackTrace(partialDerivation);
			if (parentPairs == null)
				return;
			cs = parentPairs.getFirst();
			boolean firstNull = true;
			if (cs != null) {
				firstNull = false;
				retrieveViterbiBestPathRecursiveBackTrace(path, cs, partialDerivation);
			}
			cs = parentPairs.getSecond();
			if (cs != null) {
				if (!firstNull && partialDerivation)
					partialDerivation = false;
				retrieveViterbiBestPathRecursiveBackTrace(path, cs, partialDerivation);
			}
		}

		public void retrieveViterbiBestPathRecursive(ArrayList<ChartState> path, ChartState cs, boolean partialDerivation) {
			path.add(cs);
			Duet<ChartState, ChartState> parentPairs = cs.getViterbiBestParentStates(partialDerivation);
			if (parentPairs == null)
				return;
			cs = parentPairs.getFirst();
			boolean firstNull = true;
			if (cs != null) {
				firstNull = false;
				retrieveViterbiBestPathRecursive(path, cs, partialDerivation);
			}
			cs = parentPairs.getSecond();
			if (cs != null) {
				if (!firstNull && partialDerivation)
					partialDerivation = false;
				retrieveViterbiBestPathRecursive(path, cs, partialDerivation);
			}
		}

		// path already inserted in allPaths
		public void retrieveAllPathRecursive(ArrayList<ArrayList<ChartState>> allPaths, ArrayList<ChartState> path, ChartState cs) {

			ArrayList<Duet<ChartState, ChartState>> allParentPairs = cs.getAllParentStates(false);
			if (allParentPairs == null)
				return;

			int size = allParentPairs.size();
			if (size == 1) {
				Duet<ChartState, ChartState> parentPairs = allParentPairs.get(0);
				cs = parentPairs.getSecond();
				if (cs != null) {
					path.add(cs);
					retrieveAllPathRecursive(allPaths, path, cs);
				}
				cs = parentPairs.getFirst();
				if (cs != null) {
					path.add(cs);
					retrieveAllPathRecursive(allPaths, path, cs);
				}
				return;
			}

			int i = 1;
			ArrayList<ChartState> pathCopy = null;
			for (Duet<ChartState, ChartState> parentPairs : allParentPairs) {
				if (i == size)
					pathCopy = path;
				else {
					pathCopy = new ArrayList<ChartState>(path);
					allPaths.add(pathCopy);
				}

				ChartState csFirst = parentPairs.getFirst();
				ChartState csSecond = parentPairs.getSecond();

				if (csFirst != null)
					pathCopy.add(csFirst);
				if (csSecond != null)
					pathCopy.add(csSecond);

				if (csFirst != null)
					retrieveAllPathRecursive(allPaths, pathCopy, csFirst);
				if (csSecond != null)
					retrieveAllPathRecursive(allPaths, pathCopy, csSecond);
				i++;
			}
		}

		public ArrayList<TSNodeLabel> retrieveFragDerivation(ArrayList<ChartState> path, ArrayList<Integer> numberOfFragDerivations, ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags, double[] vitCheck, boolean fillViterbi) {

			boolean check = vitCheck != null;
			Vector<ArrayList<FragFringe>> derivationFringessPath = getFragsFromPathInOrder(path);
			ArrayList<TSNodeLabel> derivationFrags = new ArrayList<TSNodeLabel>();
			double checkLogProb = 0;
			boolean firstWord = true;
			for (ArrayList<FragFringe> ffcolumn : derivationFringessPath) {
				for (FragFringe ff : ffcolumn) {
					if (ff == null)
						continue;
					HashMap<TSNodeLabel, double[]> ftable = getOriginalFrags(ff, firstWord);
					if (numberOfFragDerivations != null)
						numberOfFragDerivations.add(ftable.size());
					if (possibleFrags != null)
						possibleFrags.add(ftable);
					// vitCheck
					Entry<TSNodeLabel, double[]> maxEntry = Utility.getMaxEntry(ftable);
					derivationFrags.add(maxEntry.getKey());
					if (check)
						checkLogProb += maxEntry.getValue()[0];
					firstWord = false;
				}
			}
			if (check) {
				if (fillViterbi) {
					vitCheck[0] = checkLogProb;
				} else if (Math.abs(vitCheck[0] - checkLogProb) > 0.00000001) {
					System.err.println("Wrong viterbi chart|recomputed: " + vitCheck[0] + "|" + checkLogProb);
					System.err.println("\tDer: " + derivationFrags);
				}
			}
			return derivationFrags;
		}

		private Vector<ArrayList<FragFringe>> getFragsFromPathInOrder(ArrayList<ChartState> path) {
			Vector<ArrayList<FragFringe>> frags = new Vector<ArrayList<FragFringe>>(sentenceLength + 1);
			for (int i = 0; i <= sentenceLength; i++) {
				frags.add(new ArrayList<FragFringe>());
			}
			for (ChartState cs : path) {
				if (cs.isFirstStateWithCurrentWord()) {
					int index = cs.nextWordIndex();
					frags.get(index).add(cs.fragFringe);
				}
			}

			return frags;
		}

		private void countFirstLexSubOperations(ArrayList<ChartState> derivationPath) {
			for (ChartState cs : derivationPath) {
				if (!cs.isScanState() && !cs.isCompleteState() && cs.nextWordIndex() != sentenceLength) {
					if (cs.hasElementAfterDot())
						countSubDownSubUpSentence[0]++; // SUB-DOWN
					else
						countSubDownSubUpSentence[1]++; // SUB-UP
				}
			}
		}

		// Appendable app = orderedDebug ? orderedDebugSB : Parameters.logPW;

		private void appendLn(String line) {
			debugSB.append(line).append('\n');
		}

		private void printDebug() {

			appendLn("Sentence #                                  " + sentenceIndex);
			appendLn("Sentence Length:                            " + sentenceLength);
			appendLn("Total fringes lex/sub:                      " + Arrays.toString(totalFringesLexSub));
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
				if (numberOfPossibleFrags != null && possibleFrags != null) {
					long totalSubDerivations = Utility.times(numberOfPossibleFrags);
					appendLn("Number of possible frags in fringe derivation: " + numberOfPossibleFrags);
					appendLn("Total number of frags-derivations: " + totalSubDerivations);
					appendLn("Total Sub-Down Sub-Up: " + Arrays.toString(countSubDownSubUpSentence) + " " + Arrays.toString(Utility.makePercentage(countSubDownSubUpSentence)));
					if (totalSubDerivations == 1)
						appendLn("Viterbi forest derivation:\n" + toStringTex(derivation));
					else
						appendLn("Viterbi forest derivation:\n" + toStringTexMultiple(possibleFrags));
				}
				if (derivation != null)
					appendLn("Viterbi best derivation length: " + derivation.size());
				// appendLn("Viterbi best derivation:\n" +
				// toStringTex(derivation));

				appendLn("Sentence Log Prob: " + sentenceLogProb);

				appendLn("Gold tree: " + goldTestTree.toStringQtree());
				appendLn("MPD Parsed tree: " + MPDtree.toStringQtree());
				if (computeMinRiskTree) {
					appendLn("MRprod Parsed tree: " + MRP_prod_tree.toStringQtree());
				}
				if (computeMPPtree) {
					appendLn("MPP Parsed tree: " + MPPtree.toStringQtree());
				}
				appendLn("MPD log prob: " + MPDLogProb[0]);
				if (computeMPPtree)
					appendLn("MPP log prob: " + MPPLogProb[0]);
				appendLn("MPD EvalC Scores: " + Arrays.toString(EvalC.getRecallPrecisionFscore(MPDtree, goldTestTree)));
				if (computeMPPtree)
					appendLn("MPP EvalC Scores: " + Arrays.toString(EvalC.getRecallPrecisionFscore(MPPtree, goldTestTree)));
			}

			appendLn("");

			if (limitTestToSentenceIndex != -1) {
				File tableFile = new File(outputPath + "chart.txt");
				PrintWriter pw = FileUtil.getPrintWriter(tableFile);
				Vector<ArrayList<ChartState>> derPaths = null;
				if (derivationPath != null) {
					derPaths = new Vector<ArrayList<ChartState>>();
					derPaths.add(derivationPath);
					// for(int i=0; i<sentenceLength; i++) {
					// derPaths.add(retrieveRandomViterbiPartialPath(i+1,
					// null));
					// }
					// derPaths.add(retrieveRandomViterbiPartialPath(sentenceLength));
					// this.toStringTexChartHorizontal(pw,derPaths,false, true,
					// true);
				}
				this.toStringTexChartVertical(pw, derPaths, false, true, true, true);
				pw.close();
				System.out.println("Total elements in chart: " + totalElementsInChart());
			}

			appendInLogFile(debugSB, sentenceIndex);
			debugSB = new StringBuilder();

		}

		class ChartColumn {

			TermLabel nextWord;
			int nextWordIndex;

			HashMap<TermLabel, HashMap<ChartState, ProbBox>> scanStatesSet, // indexed
																			// on
																			// root
					subDownFirstStatesSet, // indexed on non-term after dot
					subUpFirstStatesSet, // indexed on root
					completeStatesSet; // indexed on root

			HashMap<TermLabel, HashMap<FragFringe, double[]>> firstLexFragFringeSet, // indexed
																						// on
																						// root
																						// -
																						// used
																						// in
																						// start
																						// and
																						// sub-backward
																						// second
					firstSubFragFringeSet; // indexed on first term (sub-site) -
											// used in sub-forward second

			public ChartColumn(TermLabel word, int i) {
				this.nextWord = word;
				this.nextWordIndex = i;
				initVariabels();
			}

			public void collectAliveStatesInFringeTableStartIndexStartIndex(HashMap<FragFringe, HashMap<Integer, ArrayList<ChartState>>> result) {
				collectAliveStatesInFringeTableStartIndex(scanStatesSet, result);
				collectAliveStatesInFringeTableStartIndex(subDownFirstStatesSet, result);
				collectAliveStatesInFringeTableStartIndex(subUpFirstStatesSet, result);
				collectAliveStatesInFringeTableStartIndex(completeStatesSet, result);
			}

			private void collectAliveStatesInFringeTableStartIndex(HashMap<TermLabel, HashMap<ChartState, ProbBox>> table, HashMap<FragFringe, HashMap<Integer, ArrayList<ChartState>>> result) {
				for (HashMap<ChartState, ProbBox> subTable : table.values()) {
					for (ChartState cs : subTable.keySet()) {
						if (!cs.probBox.outerMargProb.isNaN()) {
							FragFringe ff = cs.fragFringe;
							Integer startIndex = cs.subScriptIndex;
							HashMap<Integer, ArrayList<ChartState>> resultMap = result.get(ff);
							if (resultMap == null) {
								resultMap = new HashMap<Integer, ArrayList<ChartState>>();
								result.put(ff, resultMap);
							}
							Utility.putInHashMapArrayList(resultMap, startIndex, cs);
						}
					}
				}
			}

			public void removeDeadStates() {
				removeDeadStates(scanStatesSet);
				removeDeadStates(subDownFirstStatesSet);
				removeDeadStates(subUpFirstStatesSet);
				removeDeadStates(completeStatesSet);
			}

			private void removeDeadStates(HashMap<TermLabel, HashMap<ChartState, ProbBox>> table) {
				if (table.isEmpty())
					return;
				Iterator<Entry<TermLabel, HashMap<ChartState, ProbBox>>> it = table.entrySet().iterator();
				while (it.hasNext()) {
					Set<ChartState> stateSet = it.next().getValue().keySet();
					Iterator<ChartState> subTableKeysIter = stateSet.iterator();
					while (subTableKeysIter.hasNext()) {
						ChartState nextCs = subTableKeysIter.next();
						if (nextCs.probBox.outerMargProb.isNaN())
							subTableKeysIter.remove();
					}
					if (stateSet.isEmpty())
						it.remove();
				}

			}

			private void initVariabels() {
				scanStatesSet = new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
				subDownFirstStatesSet = new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
				subUpFirstStatesSet = new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
				completeStatesSet = new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
				populateFirstLexFragFringeSet();
				populateFirstSubFragFringeSet();
				if (removeFringesNonCompatibleWithSentence)
					removeFringesNonCompatibleWithSentence();
			}

			private void populateFirstLexFragFringeSet() {
				if (nextWordIndex == sentenceLength) {
					firstLexFragFringeSet = new HashMap<TermLabel, HashMap<FragFringe, double[]>>();
					return;
				}

				boolean firstWord = nextWordIndex == 0;

				firstLexFragFringeSet = firstWord ? Utility.deepHashMapDoubleCloneArray(firstWordFirstLexFringes.get(nextWord)) : Utility.deepHashMapDoubleCloneArray(firstLexFringes.get(nextWord));

				if (templateSmoothing) {
					HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringe, double[]>>> templateTable = firstWord ? firstWordFirstLexFringesTemplates : firstLexFringesTemplates;
					for (TermLabel pos : lexPosFreqTable.get(nextWord).keySet()) {
						HashMap<TermLabel, HashMap<FragFringe, double[]>> posFringes = templateTable.get(pos);
						if (posFringes != null) {
							for (Entry<TermLabel, HashMap<FragFringe, double[]>> e : posFringes.entrySet()) {
								TermLabel root = e.getKey();
								HashMap<FragFringe, double[]> presentSet = firstLexFragFringeSet.get(root);
								if (presentSet == null) {
									presentSet = new HashMap<FragFringe, double[]>();
									firstLexFragFringeSet.put(root, presentSet);
								}
								for (Entry<FragFringe, double[]> f : e.getValue().entrySet()) {
									FragFringe ff = f.getKey();
									FragFringe ffCopy = ff.cloneFringe();
									ffCopy.setInYield(0, nextWord);
									if (!presentSet.containsKey(ffCopy)) {
										double[] d = f.getValue();
										presentSet.put(ffCopy, new double[] { d[0], d[1] });
										totalFringesSmoothingLexSub[0]++;
									}
								}
							}
						}
					}
				}

				totalFringesLexSub[0] += Utility.countTotalDouble(firstLexFragFringeSet);
				fringeCounterFirstLexSubBin[0][nextWordIndex] = firstLexFragFringeSet.size();

			}

			private void populateFirstSubFragFringeSet() {
				if (nextWordIndex == 0 || nextWordIndex == sentenceLength) {
					firstSubFragFringeSet = new HashMap<TermLabel, HashMap<FragFringe, double[]>>();
					return;
				}
				firstSubFragFringeSet = Utility.deepHashMapDoubleCloneArray(firstSubFringes.get(nextWord));

				if (templateSmoothing) {
					for (TermLabel pos : lexPosFreqTable.get(nextWord).keySet()) {
						HashMap<TermLabel, HashMap<FragFringe, double[]>> posFringes = firstLexFringesTemplates.get(pos);
						posFringes = firstSubFringesTemplates.get(pos);
						if (posFringes != null) {
							for (Entry<TermLabel, HashMap<FragFringe, double[]>> e : posFringes.entrySet()) {
								TermLabel subSite = e.getKey();
								HashMap<FragFringe, double[]> presentSet = firstSubFragFringeSet.get(subSite);
								if (presentSet == null) {
									presentSet = new HashMap<FragFringe, double[]>();
									firstSubFragFringeSet.put(subSite, presentSet);
								}
								for (Entry<FragFringe, double[]> f : e.getValue().entrySet()) {
									FragFringe ff = f.getKey();
									FragFringe ffCopy = ff.cloneFringe();
									ffCopy.setInYield(1, nextWord);
									if (!presentSet.containsKey(ffCopy)) {
										double[] d = f.getValue();
										presentSet.put(ffCopy, new double[] { d[0], d[1] });
										totalFringesSmoothingLexSub[1]++;
									}
								}
							}
						}
					}
				}

				totalFringesLexSub[1] += Utility.countTotalDouble(firstSubFragFringeSet);
				fringeCounterFirstLexSubBin[1][nextWordIndex] = firstSubFragFringeSet.size();

			}

			private void removeFringesNonCompatibleWithSentence() {
				Vector<HashMap<TermLabel, HashMap<FragFringe, double[]>>> wordFFlexSub = new Vector<HashMap<TermLabel, HashMap<FragFringe, double[]>>>(2);
				wordFFlexSub.add(firstLexFragFringeSet);
				wordFFlexSub.add(firstSubFragFringeSet);
				int i = 0;
				for (HashMap<TermLabel, HashMap<FragFringe, double[]>> tab : wordFFlexSub) {
					int totalRemoved = 0;
					for (HashMap<FragFringe, double[]> subtab : tab.values()) {
						Iterator<Entry<FragFringe, double[]>> it = subtab.entrySet().iterator();
						while (it.hasNext()) {
							FragFringe ff = it.next().getKey();
							TermLabel[] fringe = ff.yield;
							// if (!FringeCompatibility.isCompatible(fringe,
							// wordTermLabels, index, posLexTable)
							if (!FringeCompatibility.isCompatibleLex(fringe, wordTermLabels, nextWordIndex)) {
								totalRemoved++;
								it.remove();
							}
						}
					}
					removedFringesLexSub[i++] += totalRemoved;
				}
			}

			private ChartColumn previousChartColumn() {
				return chart.get(nextWordIndex - 1);
			}

			public String toString() {
				StringBuilder sb = new StringBuilder();
				sb.append("Chart Column\n");
				sb.append("\tWord index: " + nextWordIndex + "\n");
				sb.append("\tWord: " + nextWord + "\n");
				sb.append("\tScan States: " + scanStatesSet.size() + "\n");
				sb.append("\tSub-Down States: " + Utility.countTotalDouble(subDownFirstStatesSet) + "\n");
				sb.append("\tSub-Up States: " + Utility.countTotalDouble(subUpFirstStatesSet) + "\n");
				sb.append("\tComplete States: " + Utility.countTotalDouble(completeStatesSet));
				return sb.toString();
			}

			public void completeSubDownSubUpScan() {
				if (completeSlow)
					performCompleteSlow();
				else
					performCompleteFast();
				if (isInterrupted())
					return;
				performSubDown();
				if (isInterrupted())
					return;
				performSubUp();
				if (isInterrupted())
					return;
				performScan();
			}

			public void completeAndTerminate() {
				if (completeSlow)
					performCompleteSlow();
				else
					performCompleteFast();

				if (isInterrupted())
					return;
				terminate();
			}

			public void countFragFringes() {
				wordScanSize[0][nextWordIndex] = scanStatesSet.size();
				wordScanSize[1][nextWordIndex] = Utility.countTotalDouble(scanStatesSet);
				wordSubDownFirstSize[0][nextWordIndex] = subDownFirstStatesSet.size();
				wordSubDownFirstSize[1][nextWordIndex] = Utility.countTotalDouble(subDownFirstStatesSet);
				wordSubDownSecondSize[0][nextWordIndex] = firstLexFragFringeSet.size();
				wordSubDownSecondSize[1][nextWordIndex] = Utility.countTotalDouble(firstLexFragFringeSet);
				wordSubUpFirstSize[0][nextWordIndex] = subUpFirstStatesSet.size();
				wordSubUpFirstSize[1][nextWordIndex] = Utility.countTotalDouble(subUpFirstStatesSet);
				wordSubUpSecondSize[0][nextWordIndex] = firstSubFragFringeSet.size();
				wordSubUpSecondSize[1][nextWordIndex] = Utility.countTotalDouble(firstSubFragFringeSet);
				wordCompleteSize[0][nextWordIndex] = completeStatesSet.size();
				wordCompleteSize[1][nextWordIndex] = Utility.countTotalDouble(completeStatesSet);
			}

			private void performStart() {
				for (Entry<TermLabel, HashMap<FragFringe, double[]>> e : firstLexFragFringeSet.entrySet()) {
					for (Entry<FragFringe, double[]> f : e.getValue().entrySet()) {
						FragFringe lexFragFringe = f.getKey();
						ChartState cs = new ChartState(lexFragFringe, 0);
						double fringeViterbiProb = f.getValue()[0]; // innerViterbi/forwardViterbi
						double fringeMargProb = f.getValue()[1]; // forward/inner
						ProbBox.addProbState(scanStatesSet, lexFragFringe.root, cs, fringeMargProb, fringeMargProb, fringeViterbiProb, fringeViterbiProb, null);
						// cs.probBox.forwardViterbiProb = currentInnerViterbi;
					}
				}
			}

			private void performScan() {
				if (pruning)
					pruneProbBoxDoubleTableViterbi(scanStatesSet, totalPrunedSentence);
				ChartColumn nextChartColumn = chart.get(nextWordIndex + 1);
				for (HashMap<ChartState, ProbBox> scanRootSet : scanStatesSet.values()) {
					for (Entry<ChartState, ProbBox> e : scanRootSet.entrySet()) {
						ChartState scanState = e.getKey();
						ProbBox pb = e.getValue();
						ChartState newState = nextChartColumn.new ChartState(scanState);
						newState.advanceDot();
						nextChartColumn.identifyAndAddProbState(newState, pb.innerMargProb, pb.forwardMargProb, pb.innerViterbiProb, pb.forwardViterbiProb, scanState);
						// always added apart when next is lex and lookahead
						// doesn't match
						if (isInterrupted())
							return;
					}
				}
			}

			private void performSubDown() {

				Set<TermLabel> firstSubSiteSet = subDownFirstStatesSet.keySet();
				Set<TermLabel> secondRootSet = firstLexFragFringeSet.keySet();

				firstSubSiteSet.retainAll(secondRootSet);
				secondRootSet.retainAll(firstSubSiteSet);

				for (TermLabel root : secondRootSet) {
					HashMap<FragFringe, double[]> subDownSecondMap = firstLexFragFringeSet.get(root);
					HashMap<ChartState, ProbBox> firstSubDownMatch = subDownFirstStatesSet.get(root);
					double totalSumFowardMargProb = ProbBox.getTotalSumForwardMargProb(firstSubDownMatch);
					Entry<ChartState, ProbBox> firstSubDownBestEntry = ProbBox.getMaxForwardViterbiEntry(firstSubDownMatch);
					ChartState firstSubDownBestState = firstSubDownBestEntry.getKey();
					double bestForwardViterbiFirst = firstSubDownBestEntry.getValue().forwardViterbiProb;

					for (Entry<FragFringe, double[]> e : subDownSecondMap.entrySet()) {
						FragFringe subDownSecondFragFringe = e.getKey();
						ChartState newState = new ChartState(subDownSecondFragFringe, nextWordIndex);
						double secondViterbiProb = e.getValue()[0];
						double secondMargProb = e.getValue()[1];
						double newInnerMargProb = secondMargProb;
						double newForwardMargProb = totalSumFowardMargProb + secondMargProb;
						double newInnerViterbiProb = secondViterbiProb;
						double newForwardViterbiProb = bestForwardViterbiFirst + secondViterbiProb;

						ProbBox.addProbState(scanStatesSet, subDownSecondFragFringe.root, newState, newInnerMargProb, newForwardMargProb, newInnerViterbiProb, newForwardViterbiProb, firstSubDownBestState);
						// new state always new
					}
					if (isInterrupted())
						return;
				}
			}

			private void performSubUp() {

				Set<TermLabel> firstRootSet = subUpFirstStatesSet.keySet();
				Set<TermLabel> secondSubSiteSet = firstSubFragFringeSet.keySet();

				firstRootSet.retainAll(secondSubSiteSet);
				secondSubSiteSet.retainAll(firstRootSet);

				for (TermLabel secondSubSite : secondSubSiteSet) {
					HashMap<FragFringe, double[]> subUpSecondTable = firstSubFragFringeSet.get(secondSubSite);
					HashMap<ChartState, ProbBox> firstSubUpMatch = subUpFirstStatesSet.get(secondSubSite);
					double[] totalSumInnerMargProbForwardMargProb = ProbBox.getTotalSumInnerMargProbForwardMargProb(firstSubUpMatch);
					Entry<ChartState, ProbBox> firstSubUpBestEntry = ProbBox.getMaxForwardViterbiEntry(firstSubUpMatch);
					ChartState mostProbSubUpFirstState = firstSubUpBestEntry.getKey();
					ProbBox maxPB = firstSubUpBestEntry.getValue();
					double bestInnerViterbiFirst = maxPB.innerViterbiProb;
					double bestForwardViterbiFirst = maxPB.forwardViterbiProb;
					for (Entry<FragFringe, double[]> e : subUpSecondTable.entrySet()) {
						FragFringe subUpSecondFragFringe = e.getKey();
						double secondViterbiProb = e.getValue()[0];
						double secondMargProb = e.getValue()[1];
						double newInnerMargProb = totalSumInnerMargProbForwardMargProb[0] + secondMargProb;
						double newForwardMargProb = totalSumInnerMargProbForwardMargProb[1] + secondMargProb;
						double newInnerViterbiProb = bestInnerViterbiFirst + secondViterbiProb;
						double newForwardViterbiProb = bestForwardViterbiFirst + secondViterbiProb;
						ChartState newState = new ChartState(subUpSecondFragFringe, 0);
						newState.advanceDot();
						ProbBox.addProbState(scanStatesSet, subUpSecondFragFringe.root, newState, newInnerMargProb, newForwardMargProb, newInnerViterbiProb, newForwardViterbiProb, mostProbSubUpFirstState);
						// new state always new
					}
					if (isInterrupted())
						return;
				}
			}

			private void performCompleteFast() {

				if (completeStatesSet.isEmpty())
					return;

				ChartStateSubscriptIndexPriorityQueue completeStatesQueue = new ChartStateSubscriptIndexPriorityQueue(nextWordIndex);

				for (HashMap<ChartState, ProbBox> completeStates : completeStatesSet.values()) {
					for (Entry<ChartState, ProbBox> e : completeStates.entrySet()) {
						completeStatesQueue.add(e.getKey(), e.getValue().innerViterbiProb);
						assert e.getKey().probBox != null;
					}
				}

				while (!completeStatesQueue.isEmpty()) {
					Duet<ChartState, double[]> duet = completeStatesQueue.poll();
					ChartState cs = duet.getFirst();
					assert cs.probBox != null;
					// double csProb = duet.getSecond()[0];
					double csProbViterbi = cs.probBox.innerViterbiProb;
					double csProbMarg = cs.probBox.innerMargProb;
					int index = cs.subScriptIndex;
					TermLabel root = cs.fragFringe.root;
					HashMap<ChartState, ProbBox> pastStateSubDownSet = chart.get(index).subDownFirstStatesSet.get(root);

					for (Entry<ChartState, ProbBox> e : pastStateSubDownSet.entrySet()) {
						ChartState pastStateSubDown = e.getKey();
						ProbBox pastStateSubDownPb = e.getValue();
						double currentInnerMargProb = pastStateSubDownPb.innerMargProb + csProbMarg;
						double currentForwardMargProb = pastStateSubDownPb.forwardMargProb + csProbMarg;
						double currentInnerViterbiProb = pastStateSubDownPb.innerViterbiProb + csProbViterbi;
						double currentForwardViterbiProb = pastStateSubDownPb.forwardViterbiProb + csProbViterbi;
						ChartState newState = new ChartState(pastStateSubDown);
						newState.advanceDot();
						if (identifyAndAddProbState(newState, currentInnerMargProb, currentForwardMargProb, currentInnerViterbiProb, currentForwardViterbiProb, cs)) {
							// newState.probBox.forwardViterbiProb = csProb +
							// pastStateSubDownPb.forwardViterbiProb;
							if (newState.isCompleteState())
								completeStatesQueue.add(newState, newState.probBox.innerViterbiProb);
						}
						// else if (newState.probBox!=null){
						// // IT OCCURS!
						// }
					}

				}

			}

			private void performCompleteSlow() {

				if (completeStatesSet.isEmpty())
					return;

				ChartStateSubscriptIndexPriorityQueueOuter completeStatesQueue = new ChartStateSubscriptIndexPriorityQueueOuter(nextWordIndex);

				for (HashMap<ChartState, ProbBox> completeStates : completeStatesSet.values()) {
					for (Entry<ChartState, ProbBox> e : completeStates.entrySet()) {
						completeStatesQueue.add(e.getKey());
						// e.getValue().innerViterbiProb);
						assert e.getKey().probBox != null;
					}
				}

				while (!completeStatesQueue.isEmpty()) {
					// Duet<ChartState, double[]> duet =
					// completeStatesQueue.poll();
					ChartState cs = completeStatesQueue.poll();
					assert cs.probBox != null;
					// double csProb = duet.getSecond()[0];
					double csProbViterbi = cs.probBox.innerViterbiProb;
					double csProbMarg = cs.probBox.innerMargProb;
					int index = cs.subScriptIndex;
					TermLabel root = cs.fragFringe.root;
					HashMap<ChartState, ProbBox> pastStateSubDownSet = chart.get(index).subDownFirstStatesSet.get(root);

					for (Entry<ChartState, ProbBox> e : pastStateSubDownSet.entrySet()) {
						ChartState pastStateSubDown = e.getKey();
						ProbBox pastStateSubDownPb = e.getValue();
						double currentInnerMargProb = pastStateSubDownPb.innerMargProb + csProbMarg;
						double currentForwardMargProb = pastStateSubDownPb.forwardMargProb + csProbMarg;
						double currentInnerViterbiProb = pastStateSubDownPb.innerViterbiProb + csProbViterbi;
						double currentForwardViterbiProb = pastStateSubDownPb.forwardViterbiProb + csProbViterbi;
						ChartState newState = new ChartState(pastStateSubDown);
						newState.advanceDot();
						if (identifyAndAddProbState(newState, currentInnerMargProb, currentForwardMargProb, currentInnerViterbiProb, currentForwardViterbiProb, cs)) {
							// newState.probBox.forwardViterbiProb = csProb +
							// pastStateSubDownPb.forwardViterbiProb;
							if (newState.isCompleteState())
								completeStatesQueue.add(newState);
							// newState.probBox.innerViterbiProb);
						}
						// else if (newState.probBox!=null){
						// // IT OCCURS!
						// }
					}

				}

			}

			private class ChartStateSubscriptIndexPriorityQueue {

				int pastColumns;
				Vector<HashMap<TermLabel, Duet<ChartState, double[]>>> completeStatesTable;
				int[] sizes;
				int totalSize;

				public ChartStateSubscriptIndexPriorityQueue(int wordIndex) {
					this.pastColumns = wordIndex;
					completeStatesTable = new Vector<HashMap<TermLabel, Duet<ChartState, double[]>>>(wordIndex);
					sizes = new int[wordIndex];
					for (int i = 0; i < wordIndex; i++) {
						completeStatesTable.add(new HashMap<TermLabel, Duet<ChartState, double[]>>());
					}
				}

				public boolean isEmpty() {
					return totalSize == 0;
				}

				public boolean add(ChartState cs, double prob) {
					int index = cs.subScriptIndex;
					TermLabel root = cs.root();
					HashMap<TermLabel, Duet<ChartState, double[]>> table = completeStatesTable.get(index);
					Duet<ChartState, double[]> present = table.get(root);
					if (present == null) {
						present = new Duet<ChartState, double[]>(cs, new double[] { prob });
						table.put(root, present);
						sizes[index]++;
						totalSize++;
						return true;
					}
					double presentProb = present.getSecond()[0];
					if (prob > presentProb) {
						table.remove(root);
						table.put(root, new Duet<ChartState, double[]>(cs, new double[] { prob }));
						// present.firstElement = cs;
						// present.secondElement[0] = prob;
						return true;
					}
					// this happens
					return false;
				}

				public Duet<ChartState, double[]> poll() {
					for (int i = pastColumns - 1; i >= 0; i--) {
						if (sizes[i] == 0)
							continue;
						HashMap<TermLabel, Duet<ChartState, double[]>> table = completeStatesTable.get(i);
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

			private class ChartStateSubscriptIndexPriorityQueueOuter {

				int pastColumns;
				Vector<HashMap<TermLabel, HashMap<ChartState, double[]>>> completeStatesTable;
				int[] sizes;
				int totalSize;

				public ChartStateSubscriptIndexPriorityQueueOuter(int wordIndex) {
					this.pastColumns = wordIndex;
					completeStatesTable = new Vector<HashMap<TermLabel, HashMap<ChartState, double[]>>>(wordIndex);
					sizes = new int[wordIndex];
					for (int i = 0; i < wordIndex; i++) {
						completeStatesTable.add(new HashMap<TermLabel, HashMap<ChartState, double[]>>());
					}
				}

				public boolean isEmpty() {
					return totalSize == 0;
				}

				public boolean add(ChartState cs) {
					int index = cs.subScriptIndex;
					TermLabel root = cs.root();
					HashMap<TermLabel, HashMap<ChartState, double[]>> table = completeStatesTable.get(index);
					HashMap<ChartState, double[]> present = table.get(root);
					ProbBox pb = cs.probBox;
					double newInsideViterbi = pb.innerViterbiProb;
					double newMargViterbi = pb.innerMargProb;
					if (present == null) {
						present = new HashMap<ChartState, double[]>();
						table.put(root, present);
						present.put(cs, new double[] { newInsideViterbi, newMargViterbi });
						sizes[index]++;
						totalSize++;
						return true;
					}
					double[] presentProb = present.get(cs);
					if (presentProb == null) {
						present.put(cs, new double[] { newInsideViterbi, newMargViterbi });
						sizes[index]++;
						totalSize++;
						return true;
					}
					if (newInsideViterbi > presentProb[0]) {
						// newMargViterbi = Utility.logSum(newMargViterbi,
						// presentProb[1]);
						pb.innerMargProb = newMargViterbi;
						present.put(cs, new double[] { newInsideViterbi, newMargViterbi });
						return true;
					}
					System.out.println("This happens!");
					return false;
					// never happens
				}

				public ChartState poll() {
					for (int i = pastColumns - 1; i >= 0; i--) {
						if (sizes[i] == 0)
							continue;
						HashMap<TermLabel, HashMap<ChartState, double[]>> table = completeStatesTable.get(i);
						Iterator<Entry<TermLabel, HashMap<ChartState, double[]>>> it1 = table.entrySet().iterator();
						HashMap<ChartState, double[]> rootTable = it1.next().getValue();
						Iterator<Entry<ChartState, double[]>> it2 = rootTable.entrySet().iterator();
						ChartState result = it2.next().getKey();
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

			public void terminate() {
				finished = true;
				if (!subUpFirstStatesSet.isEmpty()) {
					potentiallyReachingTop = subUpFirstStatesSet.keySet().contains(TOPnode);
					parsingSuccess = forceTop ? potentiallyReachingTop : true;
				}
			}

			public boolean identifyAndAddProbState(ChartState cs, double currentInnerProb, double currentForwardProb, double currrentInnerViterbiProb, double currentForwardViterbiProb, ChartState previousCs) {

				if (!cs.hasElementAfterDot()) {
					assert previousCs != null;
					TermLabel root = cs.root();
					if (cs.isStarred)
						return ProbBox.addProbState(subUpFirstStatesSet, root, cs, currentInnerProb, currentForwardProb, currrentInnerViterbiProb, currentForwardViterbiProb, previousCs);
					return ProbBox.addProbState(completeStatesSet, root, cs, currentInnerProb, currentForwardProb, currrentInnerViterbiProb, currentForwardViterbiProb, previousCs);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {
					if (nextTerm == nextWord) {
						return ProbBox.addProbState(scanStatesSet, cs.root(), cs, currentInnerProb, currentForwardProb, currrentInnerViterbiProb, currentForwardViterbiProb, previousCs);
					}
					return false;
				}
				return ProbBox.addProbState(subDownFirstStatesSet, nextTerm, cs, currentInnerProb, currentForwardProb, currrentInnerViterbiProb, currentForwardViterbiProb, previousCs);
			}

			public ProbBox identifyStateAndGetProbBox(ChartState cs) {
				if (!cs.hasElementAfterDot()) {
					TermLabel root = cs.root();
					if (cs.isStarred)
						return subUpFirstStatesSet.get(root).get(cs);
					return completeStatesSet.get(root).get(cs);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {
					return scanStatesSet.get(cs.root()).get(cs);
				}
				return subDownFirstStatesSet.get(nextTerm).get(cs);
			}

			/*
			 * public int classifyState(ChartState cs) { // -1 scan but not next
			 * word // 0 scan // 1 subdown first // 2 subup first // 3 complete
			 * if (!cs.hasElementAfterDot()) { if (cs.isStarred) return 2;
			 * return 3; } TermLabel nextTerm = cs.peekAfterDot(); if
			 * (nextTerm.isLexical) { if (nextTerm==nextWord) return 0; return
			 * -1; } return 1; }
			 */

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
					this.isStarred = subScriptIndex == 0;
					this.length = fragFringe.yield.length;
				}

				public ChartState(ChartState cs) {
					this.fragFringe = cs.fragFringe;
					this.dotIndex = cs.dotIndex;
					this.subScriptIndex = cs.subScriptIndex;
					this.isStarred = cs.isStarred;
					this.length = cs.length;
				}

				public int nextWordIndex() {
					return nextWordIndex;
				}

				public void advanceDot() {
					dotIndex++;
				}

				public void retreatDot() {
					dotIndex--;
				}

				public int typeOfChartState() {
					if (hasElementAfterDot()) {
						if (peekAfterDot().isLexical)
							return 0; // SCAN
						return 1; // SUB-DOWN FIRST
					}
					if (isStarred)
						return 2; // SUB-DOWN SECOND
					return 3; // COMPLETE
				}

				public boolean isScanState() {
					return hasElementAfterDot() && peekAfterDot().isLexical;
				}

				public ArrayList<Duet<ChartState, ChartState>> getAllParentStates(boolean partialDerivation) {

					ArrayList<Duet<ChartState, ChartState>> result = new ArrayList<Duet<ChartState, ChartState>>();

					if (dotIndex == 0) {
						if (partialDerivation && nextWordIndex > 0) {
							for (ChartState subDownFirst : subDownFirstStatesSet.get(this.root()).keySet()) {
								result.add(new Duet<ChartState, ChartState>(null, subDownFirst));
							}
							return result;
						}
						return null;
						// if current state is SCAN STATE the previous state was
						// a SUB-DOWN FIRST, but is retrieved when tracing back
						// completion.
					}

					// SCAN or SUB-UP
					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						// PREVIOUS WAS SCAN STATE
						ChartState previousState = previousChartColumn().new ChartState(this);
						previousState.retreatDot();
						result.add(new Duet<ChartState, ChartState>(previousState, null));
						return result;
					}

					if (isScanState()) { // CURREN IS SCAN STATE
						// PREVIOUS WAS SUB-UP-FIRST STATE
						if (dotIndex == 1) {
							for (Entry<ChartState, ProbBox> e : subUpFirstStatesSet.get(beforeDot).entrySet()) {
								result.add(new Duet<ChartState, ChartState>(null, e.getKey()));
							}
							return result;
						}
						// PREVIOUS WAS COMPLETE STATE
						// (same as last case below)
					}

					// SUB-DOWN-FIRST COMPLETE

					// before dot is non-lexical
					// GENERATE THROUGH COMPLETION

					for (Entry<ChartState, ProbBox> e : completeStatesSet.get(beforeDot).entrySet()) {
						ChartState completeState = e.getKey();
						int previousStateColumnIndex = completeState.subScriptIndex;
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.retreatDot();
						HashMap<ChartState, ProbBox> previousSubDownFirstStateTable = previousStateColumn.subDownFirstStatesSet.get(beforeDot);
						if (previousSubDownFirstStateTable != null) {
							ProbBox previousPb = previousSubDownFirstStateTable.get(previousState);
							if (previousPb != null) {
								result.add(new Duet<ChartState, ChartState>(previousState, completeState));
							}
						}
					}

					return result;

				}

				public void computeOuterBackTrace(ChartStateColumnIndexSubScritpIndexPriorityQueue allUsedChartStateInOrder, boolean partialDerivation) {

					double currentOuter = probBox.outerMargProb;

					if (dotIndex == 0) {
						// check this!!!!!
						if (partialDerivation && nextWordIndex > 0) {
							/*
							 * for (ChartState subDownFirst :
							 * subDownFirstStatesSet .get(this.root()).keySet())
							 * { double outerContribution = 0;
							 * allUsedChartStateInOrder.logSum(subDownFirst,
							 * outerContribution); }
							 */
							assert (false);
							return;
						}
						return;
						// if current state is SCAN STATE the previous state was
						// a SUB-DOWN FIRST, but is retrieved when tracing back
						// completion.
					}

					// SUB-UP-FIRST or SCAN
					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						// PREVIOUS WAS SCAN STATE
						allUsedChartStateInOrder.logSum(probBox.previousState, currentOuter, 0);
						return;
					}

					if (isScanState()) { // CURREN IS SCAN STATE
						// PREVIOUS WAS SUB-UP-FIRST STATE
						if (dotIndex == 1) {
							HashMap<ChartState, ProbBox> subUpFirstTable = subUpFirstStatesSet.get(beforeDot);
							double firstSubFragMargProb = firstSubFragFringeSet.get(beforeDot).get(fragFringe)[1];
							double outerContribution = currentOuter + firstSubFragMargProb;
							// double totalInnerFirstSub =
							// ProbBox.getTotalSumInnerMargProb(subUpFirstTable);
							// double outerContributionAlt = currentOuter +
							// probBox.innerMargProb - totalInnerFirstSub;
							// assert
							// (Math.abs(outerContributionAlt-outerContribution)<0.00000001);
							for (Entry<ChartState, ProbBox> e : subUpFirstTable.entrySet()) {
								ChartState previousSubUpFirst = e.getKey();
								allUsedChartStateInOrder.logSum(previousSubUpFirst, outerContribution, 2);
							}
							return;
						}
						// PREVIOUS WAS COMPLETE STATE
						// (same as last case below)
					}

					// before dot is non-lexical
					// GENERATE THROUGH COMPLETION

					// boolean viterbiPreviousFoundCheck = false;
					// ChartState previousCheck = this.probBox.previousState;
					for (Entry<ChartState, ProbBox> e : completeStatesSet.get(beforeDot).entrySet()) {
						ChartState completeState = e.getKey();
						double completeStateInner = completeState.probBox.innerMargProb;
						int previousStateColumnIndex = completeState.subScriptIndex;
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.retreatDot();
						HashMap<ChartState, ProbBox> previousSubDownFirstStateTable = previousStateColumn.subDownFirstStatesSet.get(beforeDot);
						if (previousSubDownFirstStateTable != null) {
							ProbBox previousPb = previousSubDownFirstStateTable.get(previousState);
							if (previousPb != null) {
								previousState.probBox = previousPb;
								double outerContributionPreviousForSubDownFirst = currentOuter + completeStateInner;
								double outerContributionForCompleteState = currentOuter + previousPb.innerMargProb;

								allUsedChartStateInOrder.logSum(previousState, outerContributionPreviousForSubDownFirst, 1);
								allUsedChartStateInOrder.logSum(completeState, outerContributionForCompleteState, 3);

								// if (!viterbiPreviousFoundCheck &&
								// previousCheck.equals(completeState))
								// viterbiPreviousFoundCheck = true;
							}
						}
					}
					// if (!viterbiPreviousFoundCheck) {
					// assert false;
					// }

				}

				public void partitionStartIndexElementInYieldBeforeDot(boolean partialDerivation, double fragMargProb, HashMap<Integer, HashMap<Integer, ArrayList<SpanProb>>> termIndexStartSpanProbTable) {

					int stopSpanIndex = nextWordIndex();
					Integer termIndex = dotIndex - 1;
					ProbBox currentPb = this.probBox;
					// double inXOutCurrent = computeInXOut();

					if (dotIndex == 0) {
						// check this!!!!!
						if (partialDerivation && nextWordIndex > 0) {
							/*
							 * for (ChartState subDownFirst :
							 * subDownFirstStatesSet .get(this.root()).keySet())
							 * { double outerContribution = 0;
							 * allUsedChartStateInOrder.logSum(subDownFirst,
							 * outerContribution); }
							 */
							assert false;
							return;
						}
						return;
						// if current state is SCAN STATE the previous state was
						// a SUB-DOWN FIRST, but is retrieved when tracing back
						// completion.
					}

					// SUB-UP-FIRST or SCAN
					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						// PREVIOUS WAS SCAN STATE
						ChartState previousState = currentPb.previousState;
						assert !previousState.probBox.outerMargProb.isNaN();
						// assert
						// Math.abs(this.computeInXOut()-fragMargProb)<0.0000001;
						// no there could be many if lex is not first in yield
						int startSpanIndex = stopSpanIndex - 1;
						double prob = this.computeInXOut() - fragMargProb;
						SpanProb tsp = new SpanProb(new int[] { startSpanIndex, stopSpanIndex }, prob);
						Utility.putInHashMapDoubleArrayList(termIndexStartSpanProbTable, termIndex, startSpanIndex, tsp);
						// one possibility with prob. 1
						return;
					}

					HashMap<Integer, double[]> result = new HashMap<Integer, double[]>();

					if (isScanState()) { // CURREN IS SCAN STATE
						// PREVIOUS WAS SUB-UP-FIRST STATE
						if (dotIndex == 1) {
							HashMap<ChartState, ProbBox> subUpFirstTable = subUpFirstStatesSet.get(beforeDot);
							double innerTotal = ProbBox.getTotalSumInnerMargProb(subUpFirstTable);
							for (ChartState previousSubUpFirst : subUpFirstTable.keySet()) {
								ProbBox previousPb = previousSubUpFirst.probBox;
								assert !previousPb.outerMargProb.isNaN();
								double innerContribution = previousPb.innerMargProb;
								Integer previousStartIndex = previousSubUpFirst.subScriptIndex;
								Utility.increaseInHashMapLogSum(result, previousStartIndex, innerContribution);
							}
							for (Entry<Integer, double[]> e : result.entrySet()) {
								int startSpanIndex = e.getKey();
								double prob = e.getValue()[0] - innerTotal + this.computeInXOut() - fragMargProb;
								SpanProb tsp = new SpanProb(new int[] { startSpanIndex, stopSpanIndex }, prob);
								Utility.putInHashMapDoubleArrayList(termIndexStartSpanProbTable, termIndex, startSpanIndex, tsp);
								// prob[0] -= innerTotal; // prob[0] = prob[0] -
								// innerTotal
							}
							// assert (result.size() == 1);
							return;
						}
						// PREVIOUS WAS COMPLETE STATE
						// (same as last case below)
					}

					// SUB-DOWN-FIRST COMPLETE

					// before dot is non-lexical
					// GENERATE THROUGH COMPLETION

					HashMap<ChartState, ProbBox> completeStates = completeStatesSet.get(beforeDot);
					double innerTotal = Double.NaN;
					boolean first = true;
					for (Entry<ChartState, ProbBox> e : completeStates.entrySet()) {
						ChartState completeState = e.getKey();
						int previousStateColumnIndex = completeState.subScriptIndex;
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.retreatDot();
						HashMap<ChartState, ProbBox> previousSubDownFirstStateTable = previousStateColumn.subDownFirstStatesSet.get(beforeDot);
						if (previousSubDownFirstStateTable != null) {
							ProbBox previousPb = previousSubDownFirstStateTable.get(previousState);
							if (previousPb != null) {
								assert !previousPb.outerMargProb.isNaN();
								Integer previousStartIndex = previousState.nextWordIndex();
								double innerContribution = completeState.probBox.innerMargProb;

								Utility.increaseInHashMapLogSum(result, previousStartIndex, innerContribution);

								if (first) {
									first = false;
									innerTotal = innerContribution;
								} else {
									innerTotal = Utility.logSum(innerTotal, innerContribution);
								}
							}
						}
					}

					for (Entry<Integer, double[]> e : result.entrySet()) {
						int startSpanIndex = e.getKey();
						double prob = e.getValue()[0] - innerTotal + this.computeInXOut() - fragMargProb;
						SpanProb tsp = new SpanProb(new int[] { startSpanIndex, stopSpanIndex }, prob);
						Utility.putInHashMapDoubleArrayList(termIndexStartSpanProbTable, termIndex, startSpanIndex, tsp);
						// double[] prob = e.getValue();
						// prob[0] -= innerTotal; // prob[0] = prob[0] -
						// innerTotal
					}

					// return result;
				}

				private double computeInXOut() {
					return probBox.inXout();
				}

				public Duet<ChartState, ChartState> getViterbiBestParentStates(boolean partialDerivation) {

					if (dotIndex == 0) {
						if (partialDerivation && nextWordIndex > 0) {
							ChartState subDownFirst = ProbBox.getMaxForwardViterbiEntry(subDownFirstStatesSet.get(this.root())).getKey();
							return new Duet<ChartState, ChartState>(null, subDownFirst);
						}
						return null;
						// if current state is SCAN STATE the previous state was
						// a SUB-DOWN FIRST, but is retrieved when tracing back
						// completion.
					}

					// SUB-UP-FIRST or SCAN
					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						// PREVIOUS WAS SCAN STATE
						// ChartState previousState =
						// previousChartColumn().new ChartState(this);
						// previousState.retreatDot();
						return new Duet<ChartState, ChartState>(this.probBox.previousState, null);
					}

					if (isScanState()) { // CURREN IS SCAN STATE
						// PREVIOUS WAS SUB-UP-FIRST STATE
						if (dotIndex == 1) {
							return new Duet<ChartState, ChartState>(null, ProbBox.getMaxForwardViterbiEntry(subUpFirstStatesSet.get(beforeDot)).getKey());
						}
						// PREVIOUS WAS COMPLETE STATE
						// (same as last case below)
					}

					// SUB-DOWN-FIRST COMPLETE

					// before dot is non-lexical
					// GENERATE THROUGH COMPLETION
					double max = -Double.MAX_VALUE;
					ChartState bestCompleteState = null;
					ChartState bestPreviousState = null;

					for (Entry<ChartState, ProbBox> e : completeStatesSet.get(beforeDot).entrySet()) {
						ChartState completeState = e.getKey();
						int previousStateColumnIndex = completeState.subScriptIndex;
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.retreatDot();
						HashMap<ChartState, ProbBox> previousSubDownFirstStateTable = previousStateColumn.subDownFirstStatesSet.get(beforeDot);
						if (previousSubDownFirstStateTable != null) {
							ProbBox previousPb = previousSubDownFirstStateTable.get(previousState);
							if (previousPb != null) {
								double prob = e.getValue().innerViterbiProb + previousPb.innerViterbiProb;
								if (prob > max) {
									max = prob;
									bestCompleteState = completeState;
									bestPreviousState = previousState;
								}
							}
						}
					}

					return new Duet<ChartState, ChartState>(bestPreviousState, bestCompleteState);

				}

				public Duet<ChartState, ChartState> getViterbiBestParentStatesBackTrace(boolean partialDerivation) {

					if (dotIndex == 0) {
						if (partialDerivation && nextWordIndex > 0) {
							return new Duet<ChartState, ChartState>(null, this.probBox.previousState);
						}
						return null;
						// if current state is SCAN STATE the previous state was
						// a SUB-DOWN FIRST, but is retrieved when tracing back
						// completion.
					}

					TermLabel beforeDot = peekBeforeDot();
					if (beforeDot.isLexical) {
						// PREVIOUS WAS SCAN STATE or SUB-UP-FIRST
						return new Duet<ChartState, ChartState>(this.probBox.previousState, null);
					}

					if (isScanState()) { // CURREN IS SCAN STATE
						// PREVIOUS WAS SUB-UP-FIRST STATE
						if (dotIndex == 1) {
							return new Duet<ChartState, ChartState>(null, this.probBox.previousState);
						}
						// PREVIOUS WAS COMPLETE STATE
						// (same as last case below)
					}

					// SUB-DOWN-FIRST COMPLETE

					// before dot is non-lexical
					// GENERATE THROUGH COMPLETION
					ChartState previousState = this.probBox.previousState;
					int previousStateColumnIndex = previousState.subScriptIndex;
					assert previousStateColumnIndex != 0;
					ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
					ChartState bestPreviousState = previousStateColumn.new ChartState(this);
					bestPreviousState.retreatDot();
					bestPreviousState.probBox = previousStateColumn.subDownFirstStatesSet.get(beforeDot).get(bestPreviousState);
					assert bestPreviousState.probBox != null; // CHECK HERE

					return new Duet<ChartState, ChartState>(bestPreviousState, previousState);

				}

				public TermLabel root() {
					return fragFringe.root;
				}

				public String toString() {
					StringBuilder sb = new StringBuilder();
					sb.append(nextWordIndex + ": ");
					sb.append(isStarred ? "{*} " : "{" + subScriptIndex + "} ");
					sb.append(fragFringe.root + " < ");
					int i = 0;
					for (TermLabel t : fragFringe.yield) {
						if (dotIndex == i++)
							sb.append(". ");
						sb.append(t + " ");
					}
					if (dotIndex == i)
						sb.append(". ");
					return sb.toString();
				}

				public String toStringTex() {
					StringBuilder sb = new StringBuilder();
					// sb.append("$" + wordIndex + ":\\;\\;$ ");
					sb.append(isStarred ? "$(*)$ & " : "$(" + subScriptIndex + ")$ & ");
					sb.append("$" + fragFringe.root.toStringTex() + "$ & $\\yields$ & ");
					int i = 0;
					sb.append("$ ");
					for (TermLabel t : fragFringe.yield) {
						if (dotIndex == i++)
							sb.append(" \\bullet " + " \\;\\; ");
						sb.append(t.toStringTex() + " \\;\\; ");
					}
					if (dotIndex == i)
						sb.append(" \\bullet ");
					sb.append("$");
					return sb.toString();
				}

				public boolean hasElementAfterDot() {
					return dotIndex < length;
				}

				public boolean hasElementBeforeDot() {
					return dotIndex > 0;
				}

				public boolean isFirstStateWithCurrentWord() {
					return dotIndex == 0 || (dotIndex == 1 && isScanState() && !peekBeforeDot().isLexical);
				}

				public TermLabel peekBeforeDot() {
					return fragFringe.yield[dotIndex - 1];
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
					if (this == o)
						return true;
					if (o instanceof ChartState) {
						ChartState oCS = (ChartState) o;
						return oCS.nextWordIndex() == this.nextWordIndex() && oCS.fragFringe == this.fragFringe && oCS.subScriptIndex == this.subScriptIndex && oCS.dotIndex == this.dotIndex;
					}
					return false;
				}

			}

		}

	}

	public static void mainT(String[] args) throws Exception {

		EvalC.DELETE_LABELS = new String[] {};

		debug = true;
		treeLenghtLimit = 20;
		maxSecPerWord = 10 * 60;
		threadCheckingTime = 1000;
		threads = 1;
		limitTestToSentenceIndex = 0;
		removeFringesNonCompatibleWithSentence = false;
		printAllLexFragmentsToFileWithProb = true;
		printTemplateFragsToFileWithProb = true;

		printAllLexFragmentsToFileWithProb = true;
		printTemplateFragsToFileWithProb = false;

		computeIncrementalEvaluation = false;
		computePrefixProbabilities = false;

		computeMinRiskTree = true;
		computeMPPtree = false;
		// kBest = 10;

		pruning = false;
		pruningExp = 3;
		pruningGamma = 1E-11;

		TMB = new TreeMarkoBinarizationRight_LC();
		TreeMarkoBinarization.markH = 0;
		TreeMarkoBinarization.markV = 1;

		minFreqTemplatesSmoothingFactor = 0; // 1E-8;
		minFreqOpenPoSSmoothing = 0; // 1E-4;
		minFreqFirstWordFragSmoothing = 0; // 1E-2;

		String basePath = Parameters.scratchPath;
		String workingPath = basePath + "/PLTSG/ToyCorpus3/";
		File trainTB = new File(workingPath + "trainTB.mrg");
		File testTB = new File(workingPath + "testTB.mrg");
		originalGoldFile = new File(workingPath + "testTB.mrg");
		File fragFile = null; // new File(workingPath + "fragments.mrg");

		// String workingPathFullFrags = basePath
		// + "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4/";
		// File fragFile = new File(workingPathFullFrags
		// + "fragments_approxFreq.txt");

		FragmentExtractorFreq.openClassPoSThreshold = 0;

		FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(workingPath, "noSmoothing", "noSmoothing", trainTB, testTB, fragFile);

		FragmentExtractorFreq.printAllLexFragmentsToFile = true;

		new IncrementalTsgParser(FE, testTB, workingPath).run();
	}

	public static void mainWSJ() throws Exception {

		System.out.println("Version 06.11.2012:00:30");		
		// EvalC.DELETE_LABELS = new String[]{};

		debug = true;
		treeLenghtLimit = -1;
		maxSecPerWord = 1000 * 60;
		threadCheckingTime = 1000;
		threads = 1;		
		limitTestToSentenceIndex = -1; // 17 (uptolength 5) And who would serve
										// 1775

		minFreqTemplatesSmoothingFactor = 1E-8;
		minFreqOpenPoSSmoothing = 1E-4;
		minFreqFirstWordFragSmoothing = 1E-2;

		removeFringesNonCompatibleWithSentence = false;

		printAllLexFragmentsToFileWithProb = false;
		printTemplateFragsToFileWithProb = false;

		computeIncrementalEvaluation = true;
		computePrefixProbabilities = false;

		computeMinRiskTree = false;
		computeMPPtree = true;
		// kBest = 10;

		pruning = false;
		pruningExp = 3;
		pruningGamma = 1E-21;

		completeSlow = false;		

		// TMB = new TreeMarkoBinarizationRight_LC_nullary();
		TMB = new TreeMarkoBinarizationRight_LC();
		// TMB = new TreeMarkoBinarizationRight_LC_LeftCorner();
		TreeMarkoBinarization.markH = 0;
		TreeMarkoBinarization.markV = 1;

		// String basePath = System.getProperty("user.home");
		// String workingPath = basePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";
		String basePath = Parameters.scratchPath;

		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4/";
		String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4_notop/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4_Stop/";
		// String workingPath = basePath + "/PLTSG/Chelba/Chelba_Right_H0_V1/";
		// String workingPath = basePath + "/PLTSG/Chelba/Chelba_Right_H2_V1/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkMBasic_UkT5/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkM5_UkT4_LeftCorner/";

		forceTop = false;

		// File trainTB = new File(workingPath + "f0-20.unk10.txt");
		File trainTB = new File(workingPath + "wsj-02-21.mrg");

		// File testTB = new File(workingPath + "f23-24.unk10.txt");
		//File testTB = new File(workingPath + "wsj-24.mrg");
		File testTB = new File(workingPath + "wsj-23.mrg");
		// File testTB = new File(workingPath + "wsj-02-21_first1000.mrg");

		File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		// File fragFile = new File(workingPath + "fragments_exactFreq.txt");

		String fragExtrWorkingPath = workingPath + "FragmentExtractor/";
		String outputPathParsing = workingPath + "Parsing/sec23/";
		new File(fragExtrWorkingPath).mkdirs();
		new File(outputPathParsing).mkdirs();

		String[][] settings = new String[][] {
		// minSet frags
		// only minset
		// new String[]{ "1", "No"},
		// new String[]{ "5", "No"},
		// new String[]{ "10", "No"},
		// new String[]{ "100", "No"},
		// new String[]{ "1000", "No"},
		// new String[]{ "NoSmoothing", "No"},

		// only frags
		// new String[]{ "No", "100"},
		// new String[]{ "No", "1000"},
		// new String[]{ "No", "NoSmoothing"},

		// combination
		// new String[]{ "100", "100"},
		// new String[]{ "1000", "1000"},
		// new String[]{ "10", "NoSmoothing"},
		// new String[]{ "100", "NoSmoothing"},
		// new String[]{ "1000", "NoSmoothing"},
		// new String[] { "NoSmoothing", "NoSmoothing" },
		
		//new String[] { "NoSmoothing", "100" } };
		new String[] { "NoSmoothing", "NoSmoothing" } };		

		// for(int pg=22; pg<=30; pg++) {
		// pruningGamma = Double.parseDouble("1E-" + pg);
		for (int ll : new int[] { 15 }) { // ,20,40,-1
			treeLenghtLimit = ll;
			for (int t : new int[] { 50 }) {
				FragmentExtractorFreq.openClassPoSThreshold = t;
				// for(double w : new double[]{1E-1,1E-2,1E-3,1E-4,1E-5}) {
				// for(double d : new double[]{1E-1,1E-2,1E-3,1E-4,1E-5}) {
				for (String[] set : settings) {
					// for(double a : new
					// double[]{0,1E-1,1E-2,1E-3,1E-4,1E-5,1E-6}) {
					// for(double b : new
					// double[]{0,1E-1,1E-2,1E-3,1E-4,1E-5,1E-6}) {
					minFreqTemplatesSmoothingFactor = 1E-5; // 1E-5;
					minFreqOpenPoSSmoothing = 1E-6; // 1E-6;
					minFreqFirstWordFragSmoothing = 1E-2; // 1E-2;					
					FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(fragExtrWorkingPath, set[0], set[1], trainTB, testTB, fragFile);
					// FragmentExtractorFreq.printAllLexFragmentsToFile = true;
					// FragmentExtractorFreq.printTemplateFragsToFile = true;
					new IncrementalTsgParser(FE, testTB, outputPathParsing).run();
					System.gc();
					// }
					// }
				}
				// }
				// }
			}
		}
		// }
				
	}
	
	public static void mainDundee() throws Exception {

		System.out.println("Version 13.12.2012:15:33");		
		// EvalC.DELETE_LABELS = new String[]{};
		
		debug = true;
		treeLenghtLimit = -1;
		maxSecPerWord = 1000 * 60;
		threadCheckingTime = 1000;
		threads = 1;		
		limitTestToSentenceIndex = -1; // 17 (uptolength 5) And who would serve
										// 1775

		minFreqTemplatesSmoothingFactor = 1E-8;
		minFreqOpenPoSSmoothing = 1E-4;
		minFreqFirstWordFragSmoothing = 1E-2;

		removeFringesNonCompatibleWithSentence = false;

		printAllLexFragmentsToFileWithProb = false;
		printTemplateFragsToFileWithProb = false;

		computeIncrementalEvaluation = false;
		computeStandardEvaluation = false;
		
		computePrefixProbabilities = true;

		computeMinRiskTree = false;
		computeMPPtree = false;
		// kBest = 10;

		pruning = false;
		pruningExp = 3;
		pruningGamma = 1E-21;

		completeSlow = false;		

		// TMB = new TreeMarkoBinarizationRight_LC_nullary();
		TMB = new TreeMarkoBinarizationRight_LC();
		// TMB = new TreeMarkoBinarizationRight_LC_LeftCorner();
		TreeMarkoBinarization.markH = 0;
		TreeMarkoBinarization.markV = 1;

		// String basePath = System.getProperty("user.home");
		// String workingPath = basePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";
		String basePath = Parameters.scratchPath;
		String dundeePath = Parameters.scratchPath + "PLTSG/Dundee/";
		
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4/";
		String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4_notop/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4_Stop/";
		// String workingPath = basePath + "/PLTSG/Chelba/Chelba_Right_H0_V1/";
		// String workingPath = basePath + "/PLTSG/Chelba/Chelba_Right_H2_V1/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkMBasic_UkT5/";
		// String workingPath = basePath +
		// "/PLTSG/MB_ROARK_Right_H0_V1_UkM5_UkT4_LeftCorner/";

		forceTop = false;

		// File trainTB = new File(workingPath + "f0-20.unk10.txt");
		File trainTB = new File(workingPath + "wsj-02-21.mrg");

		// File testTB = new File(workingPath + "f23-24.unk10.txt");
		//File testTB = new File(workingPath + "wsj-24.mrg");
		//File testTB = new File(workingPath + "wsj-23.mrg");
		// File testTB = new File(workingPath + "wsj-02-21_first1000.mrg");
		File testTB = new File(dundeePath + "sentsDundee_fixed_wsj_UkM4_UkT4_FlatTree.txt");

		File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		// File fragFile = new File(workingPath + "fragments_exactFreq.txt");

		String fragExtrWorkingPath = dundeePath + "FragmentExtractor/";
		String outputPathParsing = dundeePath + "Parsing/";
		new File(fragExtrWorkingPath).mkdirs();
		new File(outputPathParsing).mkdirs();

		String[][] settings = new String[][] {
		// minSet frags
		// only minset
		// new String[]{ "1", "No"},
		// new String[]{ "5", "No"},
		// new String[]{ "10", "No"},
		// new String[]{ "100", "No"},
		// new String[]{ "1000", "No"},
		// new String[]{ "NoSmoothing", "No"},

		// only frags
		// new String[]{ "No", "100"},
		// new String[]{ "No", "1000"},
		// new String[]{ "No", "NoSmoothing"},

		// combination
		// new String[]{ "100", "100"},
		// new String[]{ "1000", "1000"},
		// new String[]{ "10", "NoSmoothing"},
		// new String[]{ "100", "NoSmoothing"},
		// new String[]{ "1000", "NoSmoothing"},
		// new String[] { "NoSmoothing", "NoSmoothing" },
		
		//new String[] { "NoSmoothing", "100" } };
		new String[] { "NoSmoothing", "NoSmoothing" } };		

		// for(int pg=22; pg<=30; pg++) {
		// pruningGamma = Double.parseDouble("1E-" + pg);
		for (int ll : new int[] { -1 }) { // ,20,40,-1
			treeLenghtLimit = ll;
			for (int t : new int[] { 50 }) {
				FragmentExtractorFreq.openClassPoSThreshold = t;
				// for(double w : new double[]{1E-1,1E-2,1E-3,1E-4,1E-5}) {
				// for(double d : new double[]{1E-1,1E-2,1E-3,1E-4,1E-5}) {
				for (String[] set : settings) {
					// for(double a : new
					// double[]{0,1E-1,1E-2,1E-3,1E-4,1E-5,1E-6}) {
					// for(double b : new
					// double[]{0,1E-1,1E-2,1E-3,1E-4,1E-5,1E-6}) {
					minFreqTemplatesSmoothingFactor = 1E-5; // 1E-5;
					minFreqOpenPoSSmoothing = 1E-6; // 1E-6;
					minFreqFirstWordFragSmoothing = 1E-2; // 1E-2;					
					FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(fragExtrWorkingPath, set[0], set[1], trainTB, testTB, fragFile);
					FragmentExtractorFreq.checkGoldLexPosCoverage = false;
					// FragmentExtractorFreq.printAllLexFragmentsToFile = true;
					// FragmentExtractorFreq.printTemplateFragsToFile = true;
					new IncrementalTsgParser(FE, testTB, outputPathParsing).run();
					System.gc();
					// }
					// }
				}
				// }
				// }
			}
		}
		// }
				
	}
	
	public static void main(String[] args) throws Exception {
		//mainWSJ();
		mainDundee();
	}

}
