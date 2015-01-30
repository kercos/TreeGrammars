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
import tsg.incremental.CondProbModel.CondProbModelLex;
import tsg.incremental.FragSpanChartProb.SpanProb;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationRight_LC;
import tsg.mb.TreeMarkoBinarizationRight_LC_LeftCorner;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Duet;
import util.LongestCommonSubsequence;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class IncrementalTsgAdvancedParser extends Thread {

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
	File incrementalParsedMinConnectedAllFile,
			incrementalParsedMinConnectedLastFile,
			incrementalGoldMinConnectedAllFile,
			incrementalEvalMinConnectedAllFile;
	File incrementalParsedMaxPredictedAllFile,
			incrementalGoldMaxPredictedAllFile,
			incrementalEvalMaxPredictedAllFile;
	File allMinConnPrefixResults;

	String outputPath;
	ArrayList<TSNodeLabel> testTB;
	TSNodeLabel[] parsedTB_MPD, parsedTB_MRP_prod, // parsedTB_MRP_sum,
			parsedTB_MPP;
	TSNodeLabel[][] partialParsedTB;
	double[] sentenceLogProbTestSet;
	double[][] sentenceprefixProb;
	PrintProgress progress;
	int parsedTrees, potentiallyReachingTopTrees, actuallyReachedTopTrees,
			interruptedTrees, indexLongestWordTime; 
		//totalSubDerivations, ambiguousViterbiDerivations;
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
	
	CondProbModel globalCondProbModel;	



	public IncrementalTsgAdvancedParser(CondProbModel condProbModel, File testTBfile, String outputPath) throws Exception {
		this.globalCondProbModel = condProbModel;
		this.testTBfile = testTBfile;
		this.testTB = Wsj.getTreebank(testTBfile);
		this.outputPath = outputPath;
	}

	public void run() {
		initVariables();
		Parameters.openLogFile(logFile);
		printParameters();
		filterTestTB();
		parseIncremental();
		evaluate();
		printPrefixProbabilities();
		printFinalSummary();
		printLogProbTableToFile();
		Parameters.closeLogFile();
	}

	private void initVariables() {
		globalTimer = new SimpleTimer(true);
		this.templateSmoothing = FragmentExtractorFreq.smoothingFromFrags || FragmentExtractorFreq.smoothingFromMinSet;
		if (!templateSmoothing)
			minFreqTemplatesSmoothingFactor = 0;

		outputPath += "ITSG_Advanced_" + FileUtil.dateTimeString() + "/";

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

	private void printParameters() {
		Parameters.reportLine("\n\n");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("INCREMENTAL TSG PARSER ADVANCED ");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("");
		Parameters.reportLine("Log file: " + logFile);
		Parameters.reportLine("Cond Prob Model: " + globalCondProbModel.idString());
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

		Parameters.reportLine("Final evalC scores MPD: " + Arrays.toString(evalC_MPD.getRecallPrecisionFscoreAll()));

		if (computeMinRiskTree) {
			Parameters.reportLine("Final evalC scores MRP product: " + Arrays.toString(evalC_MRP_prod.getRecallPrecisionFscoreAll()));
		}

		if (computeMPPtree) {
			Parameters.reportLine("Final evalC scores MPP: " + Arrays.toString(evalC_MPP.getRecallPrecisionFscoreAll()));
		}
		//Parameters.reportLine("Average tree viterbi sub-derivations: " + ((float) totalSubDerivations) / testTB.size());
		//Parameters.reportLine("Trees having ambiguous viterbi derivations: " + ambiguousViterbiDerivations + " (" + Utility.makePercentage(ambiguousViterbiDerivations, testTB.size()) + ")");
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
		evaluateStandard();
		if (computeIncrementalEvaluation)
			evaluateIncremental();
	}

	private void evaluateStandard() {
		Parameters.reportLine("Running standard evaluation");
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

				assert parsedPartialTreeMinConnected.countLexicalNodes() == goldTreeChopped.countLexicalNodes();

				String parsedPartialTreeMinConnectedString = parsedPartialTreeMinConnected.toString();
				pwParsedArrayMinConnected[j].append(parsedPartialTreeMinConnectedString).append('\n');
				pwParsedMinConnectedAll.append(parsedPartialTreeMinConnectedString).append('\n');
				if (j == length - 1) {
					pwParsedMinConnectedLast.append(parsedPartialTreeMinConnectedString).append('\n');
				}

			}
			sentenceIndex++;
		}

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

	public static void pruneProbBoxDoubleTableViterbi(HashMap<TermLabel, HashMap<ProbChartState, ProbBox>> scanStatesSet, int[] totalPruned) {

		Entry<ProbChartState, ProbBox> maxState = ProbBox.getMaxForwardViterbiEntryDouble(scanStatesSet);
		if (maxState == null)
			return;
		double maxLogProb = maxState.getValue().forwardViterbiProb; // forwardProb;
																	// //;
																	// //innerViterbiProb;
		int totalSize = Utility.countTotalDouble(scanStatesSet);
		double minLogProbThreshold = computePruningLogProbThreshold(maxLogProb, totalSize);
		Iterator<Entry<TermLabel, HashMap<ProbChartState, ProbBox>>> iterA = scanStatesSet.entrySet().iterator();
		int pruned = 0;
		while (iterA.hasNext()) {
			Entry<TermLabel, HashMap<ProbChartState, ProbBox>> nextIterA = iterA.next();
			HashMap<ProbChartState, ProbBox> nextIterAValue = nextIterA.getValue();
			Iterator<Entry<ProbChartState, ProbBox>> iterB = nextIterAValue.entrySet().iterator();
			while (iterB.hasNext()) {
				Entry<ProbChartState, ProbBox> iterBnext = iterB.next();
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

	protected class ChartParserThread extends Thread {

		int sentenceIndex;
		TSNodeLabel goldTestTree;

		ArrayList<Label> wordLabels;
		TermLabel[] wordTermLabels;
		HashSet<TermLabel> wordTermLabelsSet;
		int sentenceLength;

		// Vector<HashMap<TermLabel,HashMap<FragFringeUnambigous,double[]>>>
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
		ArrayList<ProbChartState> derivationPath;
		ArrayList<TSNodeLabel> derivation;
		double[] MPDLogProb, MPPLogProb;
		//ArrayList<Integer> numberOfPossibleFrags;
		//ArrayList<HashMap<TSNodeLabel, double[]>> possibleFrags;
		//int treeSubDerivations;
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
				updateMainResults();
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
			//numberOfPossibleFrags = new ArrayList<Integer>();
			//possibleFrags = new ArrayList<HashMap<TSNodeLabel, double[]>>();
			//treeSubDerivations = 1;
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

			// HashMap<HashSet<ProbChartState>,double[]> derivationPaths = null;
			// double parseProb = 0;

			// parsingSuccess = false;

			if (parsingSuccess) {

				// derivationPath =
				// retrieveRandomBestViterbiPath(viterbiLogProb);
				derivationPath = retrieveRandomViterbiPath(MPDLogProb);
				// assert derivationPathOld.equals(derivationPath);
				derivation = retrieveFragDerivation(derivationPath);
				//treeSubDerivations = Utility.product(numberOfPossibleFrags);
				MPDtree = combineDerivation(derivation);

				if (computeMinRiskTree) {
					//computeOuterProbabilities();
					// checkFinalProbConsistency();
					sentenceLogProb = ProbBox.getTotalSumInnerOuterMargProb(chart.get(sentenceLength).subForwardFirstStatesSet);
					//computeMinRiskParses(); // needs sentenceLogProb
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
					ArrayList<ProbChartState> partialPath = retrieveRandomViterbiPartialPath(i + 1, viterbiLogProbPartial);
					if (partialPath == null)
						incrementalTrees[i] = null;
					else {
						ArrayList<TSNodeLabel> partialDer = retrieveFragDerivation(partialPath);
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

		private void updateMainResults() {
			synchronized (progress) {
				if (Thread.interrupted() && !finished)
					interruptedTrees++;
				
				//totalSubDerivations += treeSubDerivations;
				//if (treeSubDerivations > 1)
				//	ambiguousViterbiDerivations++;
				
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

		private int totalElementsInChart() {
			return Utility.sum(wordSubDownFirstSize[1]) + Utility.sum(wordSubDownSecondSize[1]) + Utility.sum(wordSubUpFirstSize[1]) + Utility.sum(wordSubUpSecondSize[1]) + Utility.sum(wordCompleteSize[1]);
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
			ProbBox.getTotalSumForwardMargProbDouble(lastCol.subForwardFirstStatesSet);
		}

		public HashMap<FragFringeUnambigous, HashMap<Integer, ArrayList<ProbChartState>>> collectAliveStatesInFringeTableStartIndex() {
			HashMap<FragFringeUnambigous, HashMap<Integer, ArrayList<ProbChartState>>> result = new HashMap<FragFringeUnambigous, HashMap<Integer, ArrayList<ProbChartState>>>();
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
			HashMap<TermLabel, HashMap<ProbChartState, ProbBox>> endStates = lastColumn.subForwardFirstStatesSet;
			for (HashMap<ProbChartState, ProbBox> finalStateTable : endStates.values()) {
				for (Entry<ProbChartState, ProbBox> e : finalStateTable.entrySet()) {
					ProbChartState cs = e.getKey();
					double prob = e.getValue().innerViterbiProb;
					ArrayList<ProbChartState> path = new ArrayList<ProbChartState>();
					retrieveViterbiBestPathRecursiveBackTrace(path, cs, false);
					ArrayList<TSNodeLabel> derivation = retrieveFragDerivation(path);
					TSNodeLabel t = combineDerivation(derivation);
					Utility.increaseInHashMapLog(result, t, prob);
				}
			}
			return result;
		}

		public ArrayList<ProbChartState> retrieveRandomViterbiPath(double[] viterbiLogProb) {

			ChartColumn lastColumn = chart.get(sentenceLength);
			HashMap<ProbChartState, ProbBox> topStateSet = lastColumn.subForwardFirstStatesSet.get(TOPnode);
			Entry<ProbChartState, ProbBox> topStateEntry = null;
			if (topStateSet != null)
				topStateEntry = ProbBox.getMaxInnerViterbiEntry(topStateSet);

			HashMap<TermLabel, HashMap<ProbChartState, ProbBox>> endStates = lastColumn.subForwardFirstStatesSet;
			Entry<ProbChartState, ProbBox> nonTopStateEntry = ProbBox.getMaxViterbiEntryDouble(endStates);

			if (!forceTop && (topStateEntry == null || nonTopStateEntry.getValue().innerViterbiProb > topStateEntry.getValue().innerViterbiProb))
				topStateEntry = nonTopStateEntry;
			// using a non top anyway

			viterbiLogProb[0] = topStateEntry.getValue().innerViterbiProb;
			ArrayList<ProbChartState> path = new ArrayList<ProbChartState>();
			ProbChartState cs = topStateEntry.getKey();
			retrieveViterbiBestPathRecursiveBackTrace(path, cs, false);
			// without backtrace
			// retrieveViterbiBestPathRecursive(path,topStateEntry.getKey(),
			// false);
			return path;
		}

		public ArrayList<ProbChartState> retrieveRandomViterbiPartialPath(int prefixLength, double[] vit) {
			// ChartColumn nextChartColumn = chart.get(prefixLength); //after
			// last was scanned
			ChartColumn prefixLastColum = chart.get(prefixLength - 1);
			// before last wasscanned
			// look only in scan states
			// assuming look ahead 1 (all states in the scanStates have the
			// current word to be scanned)
			Entry<ProbChartState, ProbBox> bestScanEntryState = 
				ProbBox.getMaxForwardViterbiEntryDouble(prefixLastColum.scanStatesSet);
			if (bestScanEntryState == null)
				return null;
			if (vit != null)
				vit[0] = bestScanEntryState.getValue().forwardViterbiProb;
			ArrayList<ProbChartState> path = new ArrayList<ProbChartState>();
			retrieveViterbiBestPathRecursiveBackTrace(path, bestScanEntryState.getKey(), true);
			return path;

		}
		
		public void retrieveViterbiBestPathRecursiveBackTrace(ArrayList<ProbChartState> path, 
				ProbChartState cs, boolean partialDerivation) {
			
			path.add(cs);
			ChartColumn cc = chart.get(cs.nextWordIndex);
			Duet<ProbChartState, ProbChartState> parentPairs = 
				cc.getViterbiBestParentStatesBackTrace(cs, partialDerivation);
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

		public ArrayList<TSNodeLabel> retrieveFragDerivation(ArrayList<ProbChartState> path) {

			Vector<ArrayList<FragFringeUnambigous>> derivationFringessPath = getFragsFromPathInOrder(path);
			ArrayList<TSNodeLabel> derivationFrags = new ArrayList<TSNodeLabel>();
			double checkLogProb = 0;
			for (ArrayList<FragFringeUnambigous> ffcolumn : derivationFringessPath) {
				for (FragFringeUnambigous ff : ffcolumn) {
					if (ff == null)
						continue;
					TSNodeLabel frag = ff.fragment;
					derivationFrags.add(frag);
				}
			}
			return derivationFrags;
		}

		private Vector<ArrayList<FragFringeUnambigous>> getFragsFromPathInOrder(ArrayList<ProbChartState> path) {
			Vector<ArrayList<FragFringeUnambigous>> frags = new Vector<ArrayList<FragFringeUnambigous>>(sentenceLength + 1);
			for (int i = 0; i <= sentenceLength; i++) {
				frags.add(new ArrayList<FragFringeUnambigous>());
			}
			for (ProbChartState cs : path) {
				if (cs.isFirstStateWithCurrentWord()) {
					int index = cs.nextWordIndex;
					frags.get(index).add(cs.fragFringe);
				}
			}

			return frags;
		}

		private void countFirstLexSubOperations(ArrayList<ProbChartState> derivationPath) {
			for (ProbChartState cs : derivationPath) {
				if (!cs.isScanState() && !cs.isCompleteState() && cs.nextWordIndex != sentenceLength) {
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
				Vector<ArrayList<ProbChartState>> derPaths = null;
				if (derivationPath != null) {
					derPaths = new Vector<ArrayList<ProbChartState>>();
					derPaths.add(derivationPath);
					// for(int i=0; i<sentenceLength; i++) {
					// derPaths.add(retrieveRandomViterbiPartialPath(i+1,
					// null));
					// }
					// derPaths.add(retrieveRandomViterbiPartialPath(sentenceLength));
					// this.toStringTexChartHorizontal(pw,derPaths,false, true,
					// true);
				}
				//this.toStringTexChartVertical(pw, derPaths, false, true, true, true);
				pw.close();
				System.out.println("Total elements in chart: " + totalElementsInChart());
			}

			appendInLogFile(debugSB, sentenceIndex);
			debugSB = new StringBuilder();

		}

		class ChartColumn {

			TermLabel nextWord;
			int nextWordIndex;

			HashMap<TermLabel, HashMap<ProbChartState, ProbBox>> 
				scanStatesSet, // indexed on root
				subBackwardFirstStatesSet, // indexed on non-term after dot
				subForwardFirstStatesSet, // indexed on root
				completeStatesSet; // indexed on root
			
			CondProbModelLex condProbModelLex;

			//HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>
			//	firstLexFragFringeSet, // indexed on root - used in start and sub-backward second
			//	firstSubFragFringeSet; // indexed on first term (sub-site) - used in sub-forward second

			public ChartColumn(TermLabel word, int i) {
				this.nextWord = word;
				this.nextWordIndex = i;
				initVariabels();
			}

			public void collectAliveStatesInFringeTableStartIndexStartIndex(HashMap<FragFringeUnambigous, HashMap<Integer, ArrayList<ProbChartState>>> result) {
				collectAliveStatesInFringeTableStartIndex(scanStatesSet, result);
				collectAliveStatesInFringeTableStartIndex(subBackwardFirstStatesSet, result);
				collectAliveStatesInFringeTableStartIndex(subForwardFirstStatesSet, result);
				collectAliveStatesInFringeTableStartIndex(completeStatesSet, result);
			}

			private void collectAliveStatesInFringeTableStartIndex(HashMap<TermLabel, HashMap<ProbChartState, ProbBox>> table, HashMap<FragFringeUnambigous, HashMap<Integer, ArrayList<ProbChartState>>> result) {
				for (HashMap<ProbChartState, ProbBox> subTable : table.values()) {
					for (ProbChartState cs : subTable.keySet()) {
						if (!cs.probBox.outerMargProb.isNaN()) {
							FragFringeUnambigous ff = cs.fragFringe;
							Integer startIndex = cs.startIndex;
							HashMap<Integer, ArrayList<ProbChartState>> resultMap = result.get(ff);
							if (resultMap == null) {
								resultMap = new HashMap<Integer, ArrayList<ProbChartState>>();
								result.put(ff, resultMap);
							}
							Utility.putInHashMapArrayList(resultMap, startIndex, cs);
						}
					}
				}
			}

			public void removeDeadStates() {
				removeDeadStates(scanStatesSet);
				removeDeadStates(subBackwardFirstStatesSet);
				removeDeadStates(subForwardFirstStatesSet);
				removeDeadStates(completeStatesSet);
			}

			private void removeDeadStates(HashMap<TermLabel, HashMap<ProbChartState, ProbBox>> table) {
				if (table.isEmpty())
					return;
				Iterator<Entry<TermLabel, HashMap<ProbChartState, ProbBox>>> it = table.entrySet().iterator();
				while (it.hasNext()) {
					Set<ProbChartState> stateSet = it.next().getValue().keySet();
					Iterator<ProbChartState> subTableKeysIter = stateSet.iterator();
					while (subTableKeysIter.hasNext()) {
						ProbChartState nextCs = subTableKeysIter.next();
						if (nextCs.probBox.outerMargProb.isNaN())
							subTableKeysIter.remove();
					}
					if (stateSet.isEmpty())
						it.remove();
				}

			}

			private void initVariabels() {
				scanStatesSet = new HashMap<TermLabel, HashMap<ProbChartState, ProbBox>>();
				subBackwardFirstStatesSet = new HashMap<TermLabel, HashMap<ProbChartState, ProbBox>>();
				subForwardFirstStatesSet = new HashMap<TermLabel, HashMap<ProbChartState, ProbBox>>();
				completeStatesSet = new HashMap<TermLabel, HashMap<ProbChartState, ProbBox>>();
				if (nextWordIndex>0)
					condProbModelLex = globalCondProbModel.getCondProbModelLex(nextWord);
				//populateFirstLexFragFringeSet();
				//populateFirstSubFragFringeSet();
				//if (removeFringesNonCompatibleWithSentence)
				//	removeFringesNonCompatibleWithSentence();
			}

			/*
			private void populateFirstLexFragFringeSet() {
				if (nextWordIndex == sentenceLength) {
					firstLexFragFringeSet = new HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>();
					return;
				}

				boolean firstWord = nextWordIndex == 0;

				firstLexFragFringeSet = firstWord ? Utility.deepHashMapDoubleCloneArray(firstWordFirstLexFringes.get(nextWord)) : Utility.deepHashMapDoubleCloneArray(firstLexFringes.get(nextWord));

				if (templateSmoothing) {
					HashMap<TermLabel, HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>> templateTable = firstWord ? firstWordFirstLexFringesTemplates : firstLexFringesTemplates;
					for (TermLabel pos : lexPosFreqTable.get(nextWord).keySet()) {
						HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> posFringes = templateTable.get(pos);
						if (posFringes != null) {
							for (Entry<TermLabel, HashMap<FragFringeUnambigous, double[]>> e : posFringes.entrySet()) {
								TermLabel root = e.getKey();
								HashMap<FragFringeUnambigous, double[]> presentSet = firstLexFragFringeSet.get(root);
								if (presentSet == null) {
									presentSet = new HashMap<FragFringeUnambigous, double[]>();
									firstLexFragFringeSet.put(root, presentSet);
								}
								for (Entry<FragFringeUnambigous, double[]> f : e.getValue().entrySet()) {
									FragFringeUnambigous ff = f.getKey();
									FragFringeUnambigous ffCopy = ff.cloneFringe();
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
					firstSubFragFringeSet = new HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>();
					return;
				}
				firstSubFragFringeSet = Utility.deepHashMapDoubleCloneArray(firstSubFringes.get(nextWord));

				if (templateSmoothing) {
					for (TermLabel pos : lexPosFreqTable.get(nextWord).keySet()) {
						HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> posFringes = firstLexFringesTemplates.get(pos);
						posFringes = firstSubFringesTemplates.get(pos);
						if (posFringes != null) {
							for (Entry<TermLabel, HashMap<FragFringeUnambigous, double[]>> e : posFringes.entrySet()) {
								TermLabel subSite = e.getKey();
								HashMap<FragFringeUnambigous, double[]> presentSet = firstSubFragFringeSet.get(subSite);
								if (presentSet == null) {
									presentSet = new HashMap<FragFringeUnambigous, double[]>();
									firstSubFragFringeSet.put(subSite, presentSet);
								}
								for (Entry<FragFringeUnambigous, double[]> f : e.getValue().entrySet()) {
									FragFringeUnambigous ff = f.getKey();
									FragFringeUnambigous ffCopy = ff.cloneFringe();
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
			*/

			/*
			private void removeFringesNonCompatibleWithSentence() {
				Vector<HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>> wordFFlexSub = new Vector<HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>>(2);
				wordFFlexSub.add(firstLexFragFringeSet);
				wordFFlexSub.add(firstSubFragFringeSet);
				int i = 0;
				for (HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> tab : wordFFlexSub) {
					int totalRemoved = 0;
					for (HashMap<FragFringeUnambigous, double[]> subtab : tab.values()) {
						Iterator<Entry<FragFringeUnambigous, double[]>> it = subtab.entrySet().iterator();
						while (it.hasNext()) {
							FragFringeUnambigous ff = it.next().getKey();
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
			*/

			private ChartColumn previousChartColumn() {
				return chart.get(nextWordIndex - 1);
			}

			public String toString() {
				StringBuilder sb = new StringBuilder();
				sb.append("Chart Column\n");
				sb.append("\tWord index: " + nextWordIndex + "\n");
				sb.append("\tWord: " + nextWord + "\n");
				sb.append("\tScan States: " + scanStatesSet.size() + "\n");
				sb.append("\tSub-Down States: " + Utility.countTotalDouble(subBackwardFirstStatesSet) + "\n");
				sb.append("\tSub-Up States: " + Utility.countTotalDouble(subForwardFirstStatesSet) + "\n");
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
				performBackWardSubstitution();
				if (isInterrupted())
					return;
				performForwardSubstitution();
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
				wordSubDownFirstSize[0][nextWordIndex] = subBackwardFirstStatesSet.size();
				wordSubDownFirstSize[1][nextWordIndex] = Utility.countTotalDouble(subBackwardFirstStatesSet);
				//wordSubDownSecondSize[0][nextWordIndex] = firstLexFragFringeSet.size();
				//wordSubDownSecondSize[1][nextWordIndex] = Utility.countTotalDouble(firstLexFragFringeSet);
				wordSubUpFirstSize[0][nextWordIndex] = subForwardFirstStatesSet.size();
				wordSubUpFirstSize[1][nextWordIndex] = Utility.countTotalDouble(subForwardFirstStatesSet);
				//wordSubUpSecondSize[0][nextWordIndex] = firstSubFragFringeSet.size();
				//wordSubUpSecondSize[1][nextWordIndex] = Utility.countTotalDouble(firstSubFragFringeSet);
				wordCompleteSize[0][nextWordIndex] = completeStatesSet.size();
				wordCompleteSize[1][nextWordIndex] = Utility.countTotalDouble(completeStatesSet);
			}

			private void performStart() {
				
				HashMap<FragFringeUnambigous, double[]> initFringes = 
					globalCondProbModel.getEventsLogProbInit(nextWord);
				
				if (initFringes==null)
					return;
				
				for (Entry<FragFringeUnambigous, double[]> f : initFringes.entrySet()) {
					FragFringeUnambigous lexFragFringe = f.getKey();
					ProbChartState cs = new ProbChartState(lexFragFringe, 0, 0);
					double fringeViterbiProb = f.getValue()[0]; // innerViterbi/forwardViterbi
					ProbBox.addProbState(scanStatesSet, lexFragFringe.root, cs, 
							fringeViterbiProb, fringeViterbiProb, 
							fringeViterbiProb, fringeViterbiProb, null);
					// cs.probBox.forwardViterbiProb = currentInnerViterbi;
				}
			}

			private void performScan() {
				if (pruning)
					pruneProbBoxDoubleTableViterbi(scanStatesSet, totalPrunedSentence);
				int newNextWordIndex = nextWordIndex + 1;
				ChartColumn nextChartColumn = chart.get(newNextWordIndex);
				for (HashMap<ProbChartState, ProbBox> scanRootSet : scanStatesSet.values()) {
					for (Entry<ProbChartState, ProbBox> e : scanRootSet.entrySet()) {
						ProbChartState scanState = e.getKey();
						ProbBox pb = e.getValue();
						ProbChartState newState = new ProbChartState(scanState, newNextWordIndex);
						newState.advanceDot();
						nextChartColumn.identifyAndAddProbState(newState, pb.innerMargProb, pb.forwardMargProb, pb.innerViterbiProb, pb.forwardViterbiProb, scanState);
						// always added apart when next is lex and lookahead
						// doesn't match
						if (isInterrupted())
							return;
					}
				}
			}

			private void performBackWardSubstitution() {
				
				if (!condProbModelLex.hasBackwardEvents())
					return;

				for(HashMap<ProbChartState, ProbBox> subMap  : subBackwardFirstStatesSet.values()) {
					
					for(ProbChartState csFirst : subMap.keySet()) {
						
						HashMap<FragFringeUnambigous, double[]> subDownSecondMap = 
							condProbModelLex.getEventsLogProbBackward(csFirst);
						
						if (subDownSecondMap==null)
							continue;
						
						ProbBox csFirstProbBox = csFirst.probBox;						
						
						for (Entry<FragFringeUnambigous, double[]> e : subDownSecondMap.entrySet()) {
							FragFringeUnambigous subDownSecondFragFringe = e.getKey();
							ProbChartState newState = new ProbChartState(
									subDownSecondFragFringe, nextWordIndex, nextWordIndex);
							double secondAttachProb = e.getValue()[0];							
							double newForwardMargProb = csFirstProbBox.forwardMargProb + secondAttachProb;
							double newForwardViterbiProb = csFirstProbBox.forwardViterbiProb + secondAttachProb;

							ProbBox.addProbState(scanStatesSet, subDownSecondFragFringe.root, newState, 
									secondAttachProb, newForwardMargProb, 
									secondAttachProb, newForwardViterbiProb, 
									csFirst);
						}
						if (isInterrupted())
							return;
					}					
				}

			}

			private void performForwardSubstitution() {
				
				if (!condProbModelLex.hasForwardEvents())
					return;
				
				for(HashMap<ProbChartState, ProbBox> subMap  : subForwardFirstStatesSet.values()) {
					
					for(ProbChartState csFirst : subMap.keySet()) {						
						
						HashMap<FragFringeUnambigous, double[]> subForwardSecondMap = 
							condProbModelLex.getEventsLogProbForward(csFirst);
						
						if (subForwardSecondMap==null)
							continue;
						
						ProbBox csFirstProbBox = csFirst.probBox;
						
						for (Entry<FragFringeUnambigous, double[]> e : subForwardSecondMap.entrySet()) {
							FragFringeUnambigous subUpSecondFragFringe = e.getKey();
							double secondAttachProb = e.getValue()[0];
							double newInnerMargProb = csFirstProbBox.innerMargProb + secondAttachProb;
							double newForwardMargProb = csFirstProbBox.forwardMargProb + secondAttachProb;
							double newInnerViterbiProb = csFirstProbBox.innerViterbiProb + secondAttachProb;
							double newForwardViterbiProb = csFirstProbBox.forwardViterbiProb + secondAttachProb;
							ProbChartState newState = new ProbChartState(subUpSecondFragFringe, 0, nextWordIndex);
							newState.advanceDot();
							ProbBox.addProbState(scanStatesSet, subUpSecondFragFringe.root, newState, 
									newInnerMargProb, newForwardMargProb, 
									newInnerViterbiProb, newForwardViterbiProb, 
									csFirst);
							
						}
						
						if (isInterrupted())
							return;
					}
				}
				
			}

			private void performCompleteFast() {

				if (completeStatesSet.isEmpty())
					return;

				ChartStateSubscriptIndexPriorityQueue completeStatesQueue = new ChartStateSubscriptIndexPriorityQueue(nextWordIndex);

				for (HashMap<ProbChartState, ProbBox> completeStates : completeStatesSet.values()) {
					for (Entry<ProbChartState, ProbBox> e : completeStates.entrySet()) {
						completeStatesQueue.add(e.getKey(), e.getValue().innerViterbiProb);
						assert e.getKey().probBox != null;
					}
				}

				while (!completeStatesQueue.isEmpty()) {
					Duet<ProbChartState, double[]> duet = completeStatesQueue.poll();
					ProbChartState cs = duet.getFirst();
					assert cs.probBox != null;
					// double csProb = duet.getSecond()[0];
					double csProbViterbi = cs.probBox.innerViterbiProb;
					double csProbMarg = cs.probBox.innerMargProb;
					int index = cs.startIndex;
					TermLabel root = cs.fragFringe.root;
					HashMap<ProbChartState, ProbBox> pastStateSubDownSet = chart.get(index).subBackwardFirstStatesSet.get(root);

					for (Entry<ProbChartState, ProbBox> e : pastStateSubDownSet.entrySet()) {
						ProbChartState pastStateSubDown = e.getKey();
						ProbBox pastStateSubDownPb = e.getValue();
						double currentInnerMargProb = pastStateSubDownPb.innerMargProb + csProbMarg;
						double currentForwardMargProb = pastStateSubDownPb.forwardMargProb + csProbMarg;
						double currentInnerViterbiProb = pastStateSubDownPb.innerViterbiProb + csProbViterbi;
						double currentForwardViterbiProb = pastStateSubDownPb.forwardViterbiProb + csProbViterbi;
						ProbChartState newState = new ProbChartState(pastStateSubDown, nextWordIndex);
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

				for (HashMap<ProbChartState, ProbBox> completeStates : completeStatesSet.values()) {
					for (Entry<ProbChartState, ProbBox> e : completeStates.entrySet()) {
						completeStatesQueue.add(e.getKey());
						// e.getValue().innerViterbiProb);
						assert e.getKey().probBox != null;
					}
				}

				while (!completeStatesQueue.isEmpty()) {
					// Duet<ProbChartState, double[]> duet =
					// completeStatesQueue.poll();
					ProbChartState cs = completeStatesQueue.poll();
					assert cs.probBox != null;
					// double csProb = duet.getSecond()[0];
					double csProbViterbi = cs.probBox.innerViterbiProb;
					double csProbMarg = cs.probBox.innerMargProb;
					int index = cs.startIndex;
					TermLabel root = cs.fragFringe.root;
					HashMap<ProbChartState, ProbBox> pastStateSubDownSet = chart.get(index).subBackwardFirstStatesSet.get(root);

					for (Entry<ProbChartState, ProbBox> e : pastStateSubDownSet.entrySet()) {
						ProbChartState pastStateSubDown = e.getKey();
						ProbBox pastStateSubDownPb = e.getValue();
						double currentInnerMargProb = pastStateSubDownPb.innerMargProb + csProbMarg;
						double currentForwardMargProb = pastStateSubDownPb.forwardMargProb + csProbMarg;
						double currentInnerViterbiProb = pastStateSubDownPb.innerViterbiProb + csProbViterbi;
						double currentForwardViterbiProb = pastStateSubDownPb.forwardViterbiProb + csProbViterbi;
						ProbChartState newState = new ProbChartState(pastStateSubDown, nextWordIndex);
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
				Vector<HashMap<TermLabel, Duet<ProbChartState, double[]>>> completeStatesTable;
				int[] sizes;
				int totalSize;

				public ChartStateSubscriptIndexPriorityQueue(int wordIndex) {
					this.pastColumns = wordIndex;
					completeStatesTable = new Vector<HashMap<TermLabel, Duet<ProbChartState, double[]>>>(wordIndex);
					sizes = new int[wordIndex];
					for (int i = 0; i < wordIndex; i++) {
						completeStatesTable.add(new HashMap<TermLabel, Duet<ProbChartState, double[]>>());
					}
				}

				public boolean isEmpty() {
					return totalSize == 0;
				}

				public boolean add(ProbChartState cs, double prob) {
					int index = cs.startIndex;
					TermLabel root = cs.root();
					HashMap<TermLabel, Duet<ProbChartState, double[]>> table = completeStatesTable.get(index);
					Duet<ProbChartState, double[]> present = table.get(root);
					if (present == null) {
						present = new Duet<ProbChartState, double[]>(cs, new double[] { prob });
						table.put(root, present);
						sizes[index]++;
						totalSize++;
						return true;
					}
					double presentProb = present.getSecond()[0];
					if (prob > presentProb) {
						table.remove(root);
						table.put(root, new Duet<ProbChartState, double[]>(cs, new double[] { prob }));
						// present.firstElement = cs;
						// present.secondElement[0] = prob;
						return true;
					}
					// this happens
					return false;
				}

				public Duet<ProbChartState, double[]> poll() {
					for (int i = pastColumns - 1; i >= 0; i--) {
						if (sizes[i] == 0)
							continue;
						HashMap<TermLabel, Duet<ProbChartState, double[]>> table = completeStatesTable.get(i);
						sizes[i]--;
						totalSize--;
						Iterator<Entry<TermLabel, Duet<ProbChartState, double[]>>> it = table.entrySet().iterator();
						Duet<ProbChartState, double[]> result = it.next().getValue();
						it.remove();
						return result;
					}
					return null;
				}

			}

			private class ChartStateSubscriptIndexPriorityQueueOuter {

				int pastColumns;
				Vector<HashMap<TermLabel, HashMap<ProbChartState, double[]>>> completeStatesTable;
				int[] sizes;
				int totalSize;

				public ChartStateSubscriptIndexPriorityQueueOuter(int wordIndex) {
					this.pastColumns = wordIndex;
					completeStatesTable = new Vector<HashMap<TermLabel, HashMap<ProbChartState, double[]>>>(wordIndex);
					sizes = new int[wordIndex];
					for (int i = 0; i < wordIndex; i++) {
						completeStatesTable.add(new HashMap<TermLabel, HashMap<ProbChartState, double[]>>());
					}
				}

				public boolean isEmpty() {
					return totalSize == 0;
				}

				public boolean add(ProbChartState cs) {
					int index = cs.startIndex;
					TermLabel root = cs.root();
					HashMap<TermLabel, HashMap<ProbChartState, double[]>> table = completeStatesTable.get(index);
					HashMap<ProbChartState, double[]> present = table.get(root);
					ProbBox pb = cs.probBox;
					double newInsideViterbi = pb.innerViterbiProb;
					double newMargViterbi = pb.innerMargProb;
					if (present == null) {
						present = new HashMap<ProbChartState, double[]>();
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

				public ProbChartState poll() {
					for (int i = pastColumns - 1; i >= 0; i--) {
						if (sizes[i] == 0)
							continue;
						HashMap<TermLabel, HashMap<ProbChartState, double[]>> table = completeStatesTable.get(i);
						Iterator<Entry<TermLabel, HashMap<ProbChartState, double[]>>> it1 = table.entrySet().iterator();
						HashMap<ProbChartState, double[]> rootTable = it1.next().getValue();
						Iterator<Entry<ProbChartState, double[]>> it2 = rootTable.entrySet().iterator();
						ProbChartState result = it2.next().getKey();
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
				if (!subForwardFirstStatesSet.isEmpty()) {
					potentiallyReachingTop = subForwardFirstStatesSet.keySet().contains(TOPnode);
					parsingSuccess = forceTop ? potentiallyReachingTop : true;
				}
			}

			public boolean identifyAndAddProbState(ProbChartState cs, double currentInnerProb, double currentForwardProb, double currrentInnerViterbiProb, double currentForwardViterbiProb, ProbChartState previousCs) {

				if (!cs.hasElementAfterDot()) {
					assert previousCs != null;
					TermLabel root = cs.root();
					if (cs.isStarred)
						return ProbBox.addProbState(subForwardFirstStatesSet, root, cs, currentInnerProb, currentForwardProb, currrentInnerViterbiProb, currentForwardViterbiProb, previousCs);
					return ProbBox.addProbState(completeStatesSet, root, cs, currentInnerProb, currentForwardProb, currrentInnerViterbiProb, currentForwardViterbiProb, previousCs);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {
					if (nextTerm == nextWord) {
						return ProbBox.addProbState(scanStatesSet, cs.root(), cs, currentInnerProb, currentForwardProb, currrentInnerViterbiProb, currentForwardViterbiProb, previousCs);
					}
					return false;
				}
				return ProbBox.addProbState(subBackwardFirstStatesSet, nextTerm, cs, currentInnerProb, currentForwardProb, currrentInnerViterbiProb, currentForwardViterbiProb, previousCs);
			}

			public Duet<ProbChartState, ProbChartState> getViterbiBestParentStatesBackTrace(
					ProbChartState cs, boolean partialDerivation) {
			
				if (cs.dotIndex == 0) {
					if (partialDerivation && nextWordIndex > 0) {
						return new Duet<ProbChartState, ProbChartState>(null, cs.probBox.previousState);
					}
					return null;
					// if current state is SCAN STATE the previous state was
					// a SUB-DOWN FIRST, but is retrieved when tracing back
					// completion.
				}
			
				TermLabel beforeDot = cs.peekBeforeDot();
				if (beforeDot.isLexical) {
					// PREVIOUS WAS SCAN STATE or SUB-UP-FIRST
					return new Duet<ProbChartState, ProbChartState>(cs.probBox.previousState, null);
				}
			
				if (cs.isScanState()) { // CURREN IS SCAN STATE
					// PREVIOUS WAS SUB-UP-FIRST STATE
					if (cs.dotIndex == 1) {
						return new Duet<ProbChartState, ProbChartState>(null, cs.probBox.previousState);
					}
					// PREVIOUS WAS COMPLETE STATE
					// (same as last case below)
				}
			
				// SUB-DOWN-FIRST COMPLETE
			
				// before dot is non-lexical
				// GENERATE THROUGH COMPLETION
				ProbChartState previousState = cs.probBox.previousState;
				int previousStateColumnIndex = previousState.startIndex;
				assert previousStateColumnIndex != 0;
				ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
				ProbChartState bestPreviousState = new ProbChartState(cs, previousStateColumnIndex);
				bestPreviousState.retreatDot();
				bestPreviousState.probBox = previousStateColumn.subBackwardFirstStatesSet.get(beforeDot).get(bestPreviousState);
				assert bestPreviousState.probBox != null; // CHECK HERE
			
				return new Duet<ProbChartState, ProbChartState>(bestPreviousState, previousState);
			
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

		computeMinRiskTree = false;
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
		String workingPath = basePath + "/PLTSG/ToyCorpus2/";
		File trainTB = new File(workingPath + "trainTB.mrg");
		File testTB = new File(workingPath + "testTB.mrg");
		originalGoldFile = new File(workingPath + "testTB.mrg");
		File fragFile = null; // new File(workingPath + "fragments.mrg");

		// String workingPathFullFrags = basePath
		// + "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4/";
		// File fragFile = new File(workingPathFullFrags
		// + "fragments_approxFreq.txt");

		FragmentExtractorFreq.openClassPoSThreshold = 0;

		FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(
				workingPath, "noSmoothing", "noSmoothing", trainTB, testTB, fragFile);
		FragmentExtractorFreq.printAllLexFragmentsToFile = true;
		
		String condProbModelId = CondProbModelFactory.modelId_basicNorm;
		
		IncrementalTsgAdvancedTrainer train = new IncrementalTsgAdvancedTrainer(FE, 
				trainTB, workingPath, condProbModelId);
		train.run();
		CondProbModel condProbModel = train.globalCondProbModel;

		new IncrementalTsgAdvancedParser(condProbModel, testTB, workingPath).run();
	}

	public static void main(String[] args) throws Exception {

		// EvalC.DELETE_LABELS = new String[]{};

		debug = true;
		treeLenghtLimit = 10;
		maxSecPerWord = 1000 * 60;
		threadCheckingTime = 1000;
		threads = 7;
		limitTestToSentenceIndex = -1; // 17 (uptolength 5) And who would serve
										// ?

		minFreqTemplatesSmoothingFactor = 1E-8;
		minFreqOpenPoSSmoothing = 1E-4;
		minFreqFirstWordFragSmoothing = 1E-2;

		removeFringesNonCompatibleWithSentence = false;

		printAllLexFragmentsToFileWithProb = false;
		printTemplateFragsToFileWithProb = false;

		computeIncrementalEvaluation = false;
		computePrefixProbabilities = false;

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
		File testTB = new File(workingPath + "wsj-24.mrg");
		// File testTB = new File(workingPath + "wsj-02-21_first1000.mrg");

		File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		// File fragFile = new File(workingPath + "fragments_exactFreq.txt");

		String fragExtrWorkingPath = workingPath + "FragmentExtractor/";
		String outputPathParsing = workingPath + "ParsingAdvanced/";
		new File(fragExtrWorkingPath).mkdirs();
		new File(outputPathParsing).mkdirs();

		FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(
				workingPath, "noSmoothing", "noSmoothing", trainTB, testTB, fragFile);
		
		FragmentExtractorFreq.printAllLexFragmentsToFile = false;

		String condProbModelId = CondProbModelFactory.modelId_basicNorm;
		//String condProbModelId = CondProbModelFactory.modelId_anchorNorm;
		
		IncrementalTsgAdvancedTrainer.fastTrain = false;
		IncrementalTsgAdvancedTrainer.shortTrain = false;
		IncrementalTsgAdvancedTrainer.addBadicEvents = false;
		
		for(double f : new double[]{.1,.5,1,5,10}) {
			CondProbAttach_Anchor.multFactor = f;
			IncrementalTsgAdvancedTrainer train = new IncrementalTsgAdvancedTrainer(FE, 
					trainTB, workingPath, condProbModelId);
			train.run();			
			CondProbModel condProbModel = train.globalCondProbModel;
			train = null;
			System.gc();
			
			new IncrementalTsgAdvancedParser(condProbModel, testTB, outputPathParsing).run();
		}
		
	}

}
