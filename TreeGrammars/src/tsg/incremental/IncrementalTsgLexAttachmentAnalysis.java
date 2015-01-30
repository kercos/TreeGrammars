package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
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
import java.util.TreeMap;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.corpora.Wsj;
import tsg.incremental.FragSpanChartProb.SpanProb;
import tsg.incremental.IncrementalTsgLexAttachmentAnalysis.AttachStatsWordPair;
import tsg.incremental.IncrementalTsgLexAttachmentAnalysis.ChartParserThread.ChartColumn.ChartState;
import tsg.incremental.IncrementalTsgParser_Stop.ProbBox;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationRight_LC;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Duet;
import util.LongestCommonSubsequence;
import util.Pair;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class IncrementalTsgLexAttachmentAnalysis extends Thread {

	static boolean addSTOP = false;

	static int threads = 1;
	static long threadCheckingTime = 4000;

	static boolean normalizedCounts = false;

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

	static boolean usingPosAttachment = false; 
	HashMap<Pair<Label>, AttachStatsWordPair> attachStatsGlobalTable;
	
	File sentenceAttachGraphFile;
	PrintWriter sentenceAttachGraphPw;

	int treebankIteratorIndex;

	SimpleTimer globalTimer;

	HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>> initFragments, // indexed
																					// on
																					// lex
																					// and
																					// root
			firstLexFragments, // indexed on lex and root
			firstSubFragments; // indexed on lex and firstTerm

	static class AttachStatsSentence  {
		
		ArrayList<TSNodeLabel> wordNodes;
		int length;
		int[][] subForwardRelation, subBackwardRelation, presenceRelations;
		
		public AttachStatsSentence(ArrayList<TSNodeLabel> wordNodes) {
			this.wordNodes = wordNodes;
			this.length = wordNodes.size();
			subForwardRelation = new int[length][length]; 
			subBackwardRelation = new int[length][length];
			presenceRelations = new int[length][length];
		}
		
		public void addSubForwardRelation(int a, int b) {
			subForwardRelation[a][b]++;
		}
		
		public void addSubBackwardRelation(int a, int b) {
			subBackwardRelation[a][b]++;
		}
		
		public void addPresenceRelations(int a, int b) {
			presenceRelations[a][b]++;
		}
		
		public String toStringTex() {
			StringBuilder sb = new StringBuilder();
			sb.append("\\begin{dependency}\n");
			sb.append("\t\\begin{deptext}\n");
			sb.append("\t\t");
			Iterator<TSNodeLabel> lexIter = wordNodes.iterator();
			while(lexIter.hasNext()) {
				sb.append(lexIter.next().label());
				sb.append(lexIter.hasNext() ? " \\& " : " \\\\\n");
			}			
			sb.append("\t\\end{deptext}\n");
			for(int i=0; i<length; i++) {
				int ip = i+1;
				for(int j=0; j<length; j++) {					
					int jp = j+1;
					int subForw = subForwardRelation[i][j];
					int subBack = subBackwardRelation[i][j];
					int presence = presenceRelations[i][j];
					String label = "";
					if (subForw>0)
						label += "F" + subForw;
					if (subBack>0) {
						if (!label.isEmpty())
							label += "\\_";
						label += "B" + subBack;
					}
					if (presence>0) {
						if (!label.isEmpty())
							label += "\\_";
						label += "P" + presence;
					}
					if (!label.isEmpty())
						sb.append("\t\\depedge{" + jp + "}{" + ip + "}{" + label + "}\n");					
				}
			}
			sb.append("\\end{dependency}\n");
			return sb.toString();
		}
		
	}
	
	static class AttachStatsWordPair implements Comparable<AttachStatsWordPair> {

		int sentenceFreq;
		int totalAttach;
		int backSubAttach;
		int forwSubAttach;
		int totalDistance;
		//int present;

		public void addCounts(AttachStatsWordPair a) {
			this.sentenceFreq += a.sentenceFreq;
			this.totalAttach += a.totalAttach;
			this.backSubAttach += a.backSubAttach;
			this.forwSubAttach += a.forwSubAttach;
			this.totalDistance += a.totalDistance;
			//this.present += a.present;
		}

		public static void addAttachStats(HashMap<Pair<Label>, AttachStatsWordPair> table, 
				Label firstLex, Label secondLex, int distance, boolean backSub) {
			Pair<Label> p = new Pair<Label>(firstLex, secondLex);
			AttachStatsWordPair as = table.get(p);
			if (as==null) {
				as = new AttachStatsWordPair();
				as.sentenceFreq++;
				table.put(p, as);
			}
			as.totalAttach++;
			if (backSub)
				as.backSubAttach++;
			else
				as.forwSubAttach++;
			as.totalDistance += distance;
		}
		
		public float avgDistance() {
			return ((float)totalDistance)/totalAttach;
		}
		
		public static String stringHeader =
			"SentenceFreq" + "\t" + "TotalAttach" + "\t" + "Backward" + "\t" + "Forward" + "\t" + "AvgDst";
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(sentenceFreq);
			sb.append("\t");
			sb.append(totalAttach);
			sb.append("\t");
			sb.append(backSubAttach);
			sb.append("\t");
			sb.append(forwSubAttach);
			sb.append("\t");
			sb.append(Utility.formatNumberTwoDigit(avgDistance()));
			return sb.toString();
		}

		@Override
		public int compareTo(AttachStatsWordPair a) {
			return new Integer(this.sentenceFreq).compareTo(new Integer(a.sentenceFreq));
			//return new Integer(this.totalAttach).compareTo(new Integer(a.totalAttach));
		}
		
		public boolean equals(Object o) {
			if (o instanceof AttachStatsWordPair) {
				AttachStatsWordPair oAs = (AttachStatsWordPair)o;
				return 
					this.sentenceFreq == oAs.sentenceFreq &&	
					this.totalAttach == oAs.totalAttach &&
					this.backSubAttach == oAs.backSubAttach &&
					this.forwSubAttach == oAs.forwSubAttach &&
					this.totalDistance == oAs.totalDistance;
			}
			return false;
		}
	}
	


	public IncrementalTsgLexAttachmentAnalysis(FragmentExtractorFreq FE, File trainingTBfile, String outputPath) throws Exception {
		this.FE = FE;
		this.trainingTBfile = trainingTBfile;
		this.treebank = Wsj.getTreebank(trainingTBfile);
		this.outputPath = outputPath;
	}

	private void initVariables() {
		globalTimer = new SimpleTimer(true);
		
		if (usingPosAttachment)
			outputPath += "ITSG_Pos_Attachment_Analysis";
		else 
			outputPath += "ITSG_Lex_Attachment_Analysis";
		outputPath += "/";

		File dirFile = new File(outputPath);
		if (dirFile.exists()) {
			System.out.println("Output dir already exists, I'll delete the content...");
			FileUtil.removeAllFileAndDirInDir(dirFile);
		} else {
			dirFile.mkdirs();
		}

		attachStatsGlobalTable = new HashMap<Pair<Label>, AttachStatsWordPair>();
		
		sentenceAttachGraphFile = new File(outputPath + "sentenceAttachGraphs.txt");
		sentenceAttachGraphPw = FileUtil.getPrintWriter(sentenceAttachGraphFile);

		logFile = new File(outputPath + "LOG.log");

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
		readFragments();
		runChartParser();
		printFinalTable();
		printFinalSummary();
		sentenceAttachGraphPw.close();
		Parameters.closeLogFile();
	}

	private void printFinalTable() {
		File outputFile = new File(outputPath + "attachStats");
		Parameters.reportLine("Sorting final table...");
		TreeMap<AttachStatsWordPair,HashSet<Pair<Label>>> reversedTable = 
			Utility.reverseAndSortTable(attachStatsGlobalTable);
		
		Parameters.reportLine("Printing final table to: " + outputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		pw.println("FirstLex" + "\t" + "SecondLex" + "\t" + AttachStatsWordPair.stringHeader);
		for(Entry<AttachStatsWordPair, HashSet<Pair<Label>>> e : reversedTable.descendingMap().entrySet()) {
			AttachStatsWordPair as = e.getKey();
			for(Pair<Label> p : e.getValue()) {
				pw.println(p.getFirst() + "\t" + p.getSecond() + "\t" + as.toString());
			}			
		}
		pw.close();		
		
	}



	private void printParameters() {
		Parameters.reportLine("\n\n");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("INCREMENTAL TSG LEX ATTACHMENT ANALYSIS ");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("");
		Parameters.reportLine("Log file: " + logFile);
		Parameters.reportLine("Number of threads: " + threads);
		Parameters.reportLine("Treebank file: " + trainingTBfile);
		Parameters.reportLine("Number of trees: " + treebank.size());
		Parameters.reportLine("Sentence Attachment Graph File: " + sentenceAttachGraphFile);		
	}

	private void printFinalSummary() {
		Parameters.reportLine("Took in Total (sec): " + globalTimer.checkEllapsedTimeSec());
		Parameters.reportLineFlush("Log file: " + logFile);

	}

	private void readFragments() {

		Parameters.reportLineFlush("Converting lex fragments into fringes...");

		initFragments = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>>();
		firstLexFragments = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>>();
		firstSubFragments = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>>();

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
					if (normalizedCounts)
						d = 1;
					Utility.putInMapTriple(firstLexFragments, firstLexLabel, firstLexFrag.label, firstLexFrag, new double[] { d });
				}

				// init fragments
				freq = f.getValue()[1];
				d = freq == 0 && initFragSmoothing ? minFreqFirstWordFragSmoothing : freq;
				if (d > 0) {
					if (normalizedCounts)
						d = 1;
					Utility.putInMapTriple(initFragments, firstLexLabel, firstLexFrag.label, firstLexFrag, new double[] { d });
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
				int freq = normalizedCounts ? 1 : f.getValue()[0];
				assert freq > 0;
				Utility.putInMapTriple(firstSubFragments, firstLexLabel, firstSubLabel, firstSubFrag, new double[] { freq });
			}
		}

		if (addSTOP) {
			// STOP fringes as first-sub fringes STOP > Y STOP
			for (Entry<Label, int[]> e : FE.topFreq.entrySet()) {
				countFirstSubFrags++;
				Label firstSub = e.getKey();
				int freq = normalizedCounts ? 1 : e.getValue()[0];
				TSNodeLabel stopFrag = FragFringe.getStopFragment(firstSub);
				Label firstSubLabel = stopFrag.getLeftmostTerminalNode().label;
				Utility.putInMapTriple(firstSubFragments, FragFringe.STOPlabel, firstSubLabel, stopFrag, new double[] { freq });
			}
		}

		int totalLexFrags = countFirstLexFrags + countFirstSubFrags;
		Parameters.reportLineFlush("Total number of lex fragments: " + totalLexFrags);
		Parameters.reportLineFlush("\tLex first (including init): " + countFirstLexFrags);
		Parameters.reportLineFlush("\tSub first (including stop): " + countFirstSubFrags);
	}

	private void runChartParser() {

		Parameters.reportLineFlush("Estimating fragment frequencies...");
		Parameters.reportLineFlush("Number of trees: " + treebank.size());

		convertFreqToLogRelativeFreq();
		extractLexAttachmentCounts();

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

	private void convertFreqToLogRelativeFreq() {

		Parameters.reportLineFlush("Converting Freq to Log Relative Freq");

		// FIRST WORD
		double totalSumFirstWord = getTotalSummedProb(initFragments);
		convertToLogRelativeFreq(initFragments, totalSumFirstWord);

		// FIRST LEX
		HashMap<Label, double[]> totalRootFreqFirstLex = new HashMap<Label, double[]>();
		addSecondKeyFreq(firstLexFragments, totalRootFreqFirstLex);
		convertToLogSecondKeyRelativeFreq(firstLexFragments, totalRootFreqFirstLex);

		// FIRST SUB
		HashMap<Label, double[]> totalFirstTermFreqSecondLex = new HashMap<Label, double[]>();
		addSecondKeyFreq(firstSubFragments, totalFirstTermFreqSecondLex);
		convertToLogSecondKeyRelativeFreq(firstSubFragments, totalFirstTermFreqSecondLex);

		int removedInit = removeMinusInfinity(initFragments);
		if (removedInit > 0)
			Parameters.reportLineFlush("Removed init frag negative infinity: " + removedInit);

		int removedFirstLex = removeMinusInfinity(firstLexFragments);
		if (removedFirstLex > 0)
			Parameters.reportLineFlush("Removed first lex frag negative infinity: " + removedFirstLex);

		int removedFirstSub = removeMinusInfinity(firstSubFragments);
		if (removedFirstSub > 0)
			Parameters.reportLineFlush("Removed first sub frag negative infinity: " + removedFirstSub);

	}

	private int removeMinusInfinity(HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>> table) {
		int result = 0;
		Iterator<Entry<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>>> i = table.entrySet().iterator();
		while (i.hasNext()) {
			HashMap<Label, HashMap<TSNodeLabel, double[]>> subTable = i.next().getValue();
			Iterator<Entry<Label, HashMap<TSNodeLabel, double[]>>> i2 = subTable.entrySet().iterator();
			while (i2.hasNext()) {
				HashMap<TSNodeLabel, double[]> subSubTable = i2.next().getValue();
				Iterator<Entry<TSNodeLabel, double[]>> i3 = subSubTable.entrySet().iterator();
				while (i3.hasNext()) {
					Entry<TSNodeLabel, double[]> e = i3.next();
					double value = e.getValue()[0];
					if (value == Double.NEGATIVE_INFINITY) {
						i3.remove();
						result++;
					}
				}
				if (subSubTable.isEmpty())
					i2.remove();
			}
			if (subTable.isEmpty())
				i.remove();
		}
		return result;
	}

	private double getTotalSummedProb(HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>> table) {
		double result = 0;
		for (HashMap<Label, HashMap<TSNodeLabel, double[]>> t : table.values()) {
			for (HashMap<TSNodeLabel, double[]> t1 : t.values()) {
				for (double[] d : t1.values()) {
					result += d[0];
				}
			}
		}
		return result;
	}

	private static void convertToLogRelativeFreq(HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>> table, double totalFreq) {

		for (HashMap<Label, HashMap<TSNodeLabel, double[]>> t : table.values()) {
			for (HashMap<TSNodeLabel, double[]> s : t.values()) {
				for (double[] d : s.values()) {
					d[0] = Math.log(d[0] / totalFreq);
				}
			}
		}
	}

	private void addSecondKeyFreq(HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>> table, HashMap<Label, double[]> secondKeyFreq) {

		for (HashMap<Label, HashMap<TSNodeLabel, double[]>> e : table.values()) {
			for (Entry<Label, HashMap<TSNodeLabel, double[]>> f : e.entrySet()) {
				Label secondKey = f.getKey();
				for (Entry<TSNodeLabel, double[]> g : f.getValue().entrySet()) {
					Utility.increaseInHashMap(secondKeyFreq, secondKey, g.getValue()[0]);
				}
			}
		}

	}

	private static void convertToLogSecondKeyRelativeFreq(HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, double[]>>> table, HashMap<Label, double[]> secondKeyFreqTable) {

		for (HashMap<Label, HashMap<TSNodeLabel, double[]>> t : table.values()) {
			for (Entry<Label, HashMap<TSNodeLabel, double[]>> s : t.entrySet()) {
				Label secondKey = s.getKey();
				double secondKeyFreq = secondKeyFreqTable.get(secondKey)[0];
				for (Entry<TSNodeLabel, double[]> e : s.getValue().entrySet()) {
					double[] freqArray = e.getValue();
					freqArray[0] = Math.log(freqArray[0] / secondKeyFreq);
				}
			}
		}
	}

	private void extractLexAttachmentCounts() {
		progress = new PrintProgress("Extracting lex counts from tree:", 1, 0);

		trainTBiterator = treebank.iterator();

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
		return trainTBiterator.next();
	}

	public static TSNodeLabel combineDerivation(ArrayList<TSNodeLabel> derivation) {

		Iterator<TSNodeLabel> derivationIter = derivation.iterator();
		TSNodeLabel result = derivationIter.next().clone();
		LinkedList<TSNodeLabel> subSitesList = new LinkedList<TSNodeLabel>();
		result.addSubSites(subSitesList, true);
		while (derivationIter.hasNext()) {
			TSNodeLabel nextFrag = derivationIter.next();
			if (FragFringe.isStopFrag(nextFrag))
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

	static class ProbBox {

		double innerProb;
		Double outerProb;
		double innerViterbiProb; // what Stolke defines viterbi
		// ( it represents the prob. of the best path from the time the state
		// was created)
		ChartState previousState;

		// ( viterbi best previous state )

		public ProbBox(double innerProb, double innerViterbiProb, ChartState previousState) {
			this.innerProb = innerProb;
			this.innerViterbiProb = innerViterbiProb;
			this.previousState = previousState;
			this.outerProb = Double.NaN;
		}

		public double inXout() {
			return innerProb + outerProb;
		}

		public static <S, T> int countTotalDoubleAlive(HashMap<S, HashMap<T, ProbBox>> table) {
			int count = 0;
			for (HashMap<T, ProbBox> set : table.values()) {
				for (ProbBox pb : set.values()) {
					if (!pb.outerProb.isNaN())
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

		public static <T, C> Entry<C, ProbBox> getMaxInnerViterbiEntryDouble(HashMap<T, HashMap<C, ProbBox>> doubleTable) {

			if (doubleTable.isEmpty())
				return null;

			Iterator<Entry<T, HashMap<C, ProbBox>>> e = doubleTable.entrySet().iterator();
			Entry<C, ProbBox> result = getMaxInnerViterbiEntry(e.next().getValue());
			double max = result.getValue().innerViterbiProb;

			while (e.hasNext()) {
				Entry<C, ProbBox> bestSubEntry = getMaxInnerViterbiEntry(e.next().getValue());
				double viterbi = bestSubEntry.getValue().innerViterbiProb;
				if (viterbi > max) {
					max = viterbi;
					result = bestSubEntry;
				}
			}
			return result;
		}

		public static <C> double getTotalSumInnerOuterMargProb(HashMap<TermLabel, HashMap<C, ProbBox>> statesTable) {

			int size = ProbBox.countTotalDoubleAlive(statesTable) - 1;

			double inXoutMax = -Double.MAX_VALUE;
			double[] inXoutOthers = new double[size];
			boolean first = true;

			int i = 0;
			for (HashMap<C, ProbBox> subTable : statesTable.values()) {
				for (ProbBox pb : subTable.values()) {
					if (pb.outerProb.isNaN())
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

		public static <T, C> double getTotalSumInnerDouble(HashMap<T, HashMap<C, ProbBox>> statesTable) {

			int size = Utility.countTotalDouble(statesTable) - 1;
			double[] innerOthers = new double[size];
			double innerMax = 0;

			boolean first = true;

			int i = 0;
			for (HashMap<C, ProbBox> subTable : statesTable.values()) {
				for (ProbBox pb : subTable.values()) {
					double nextInner = pb.innerProb;
					if (first) {
						first = false;
						innerMax = nextInner;
						continue;
					}
					if (nextInner > innerMax) {
						innerOthers[i++] = innerMax;
						innerMax = nextInner;
					} else
						innerOthers[i++] = nextInner;
				}
			}
			return Utility.logSum(innerOthers, innerMax);
		}

		public static <C> double getTotalSumInnerProb(HashMap<C, ProbBox> table) {

			Iterator<ProbBox> iter = table.values().iterator();
			ProbBox pb = iter.next();
			double innerMax = pb.innerProb;
			int size = table.size() - 1;
			double[] innerOthers = new double[size];
			int i = 0;
			while (iter.hasNext()) {
				pb = iter.next();
				double nextInner = pb.innerProb;
				if (nextInner > innerMax) {
					innerOthers[i] = innerMax;
					innerMax = nextInner;
				} else
					innerOthers[i] = nextInner;
				i++;
			}
			return Utility.logSum(innerOthers, innerMax);
		}

		/**
		 * 
		 * @return the ProbBox which is introduced or modified as the result of
		 *         this operation (null if the the table is not modified)
		 */

		public static boolean addProbState(HashMap<TermLabel, HashMap<ChartState, ProbBox>> table, TermLabel firstKey, ChartState currentState, double currentInnerProb, double currentInnerViterbiProb, ChartState currentPreviousState) {

			if (currentInnerProb == Double.NEGATIVE_INFINITY)
				return false;

			HashMap<ChartState, ProbBox> subTable = table.get(firstKey);
			if (subTable == null) {
				subTable = new HashMap<ChartState, ProbBox>();
				table.put(firstKey, subTable);
				ProbBox newPb = new ProbBox(currentInnerProb, currentInnerViterbiProb, currentPreviousState);
				subTable.put(currentState, newPb);
				currentState.probBox = newPb;
				return true;
			}

			ProbBox probPresent = subTable.get(currentState);
			if (probPresent == null) {
				ProbBox pb = new ProbBox(currentInnerProb, currentInnerViterbiProb, currentPreviousState);
				subTable.put(currentState, pb);
				currentState.probBox = pb;
				return true;
			}
			probPresent.innerProb = Utility.logSum(probPresent.innerProb, currentInnerProb);
			if (probPresent.innerViterbiProb < currentInnerViterbiProb) {
				probPresent.innerViterbiProb = currentInnerViterbiProb;
				probPresent.previousState = currentPreviousState;
				currentState.probBox = probPresent;
				return true;
			}

			currentState.probBox = probPresent;
			return false;
		}

		public void logSumOuter(double outerContribution) {
			outerProb = Utility.logSum(outerProb, outerContribution);
		}

		public String toStringForChart(boolean normalProb) {
			double margIn = normalProb ? Math.exp(innerProb) : innerProb;
			double margOut = normalProb ? Math.exp(outerProb) : outerProb;
			String inString = Float.toString((float) margIn);
			String outString = Float.toString((float) margOut);
			return " [" + inString + ", " + outString + "] ";
		}

	}

	protected class ChartParserThread extends Thread {

		int sentenceIndex;
		TSNodeLabel currentTree;

		ArrayList<TSNodeLabel> wordNodes;
		int sentenceLength;

		Vector<ChartColumn> chart;

		HashMap<Pair<Label>, AttachStatsWordPair> attachStatsLocalTable;
		AttachStatsSentence sentenceAttachStats;
		String sentenceAttachGraphTex;

		double treeLogProb;

		public void run() {
			int[] i = { 0 };
			while ((currentTree = getNextTree(i)) != null) {
				sentenceIndex = i[0];
				initVariables();
				parse();
			}
		}

		private void initVariables() {
			this.wordNodes = currentTree.collectLexicalItems();
			this.sentenceLength = wordNodes.size();

			chart = new Vector<ChartColumn>(sentenceLength + 1); // termstate

			attachStatsLocalTable = new HashMap<Pair<Label>, AttachStatsWordPair>();
			sentenceAttachStats = new AttachStatsSentence(wordNodes);
			
			treeLogProb = Double.NaN;

		}

		private void parse() {
			initChart();

			ChartColumn startChartColumn = chart.get(0);
			startChartColumn.performStart();

			for (int i = 0; i <= sentenceLength; i++) {
				ChartColumn c = chart.get(i);
				if (i == 0)
					c.performScan();
				else if (i < sentenceLength) {
					c.performCompleteSlow();
					c.performSubDown();
					c.performSubUp();
					c.performScan();
				} else { // last column
					c.performCompleteSlow();
					if (addSTOP)
						c.performSubUp();
				}
			}
			
			sentenceAttachGraphTex = sentenceAttachStats.toStringTex();
			updateMainResults();

		}

		private void updateMainResults() {
			synchronized (progress) {

				for (Entry<Pair<Label>, AttachStatsWordPair> e : attachStatsLocalTable.entrySet()) {
					Pair<Label> p = e.getKey();
					AttachStatsWordPair localAs = e.getValue();
					AttachStatsWordPair gloablAs = attachStatsGlobalTable.get(p);
					if (gloablAs == null)
						attachStatsGlobalTable.put(p, localAs);
					else
						gloablAs.addCounts(localAs);
				}
				
				if (sentenceLength<20) {
					sentenceAttachGraphPw.println("%Sentence: " + sentenceIndex);
					sentenceAttachGraphPw.println("% " + currentTree.toFlatSentence());
					sentenceAttachGraphPw.println(currentTree.toStringTex(false, false) + "\n");
					sentenceAttachGraphPw.println(sentenceAttachGraphTex + "\n\n");
				}

				progress.next();
			}

		}

		private void initChart() {
			int i = 0;
			for (TSNodeLabel wn : wordNodes) {
				TermLabel wordTermLabel = TermLabel.getTermLabel(wn.label, true);
				chart.add(new ChartColumn(wordTermLabel, wn, i));
				i++;
			}

			chart.add(new ChartColumn(FragFringe.STOPnodeLex, null, i));
		}

		private void checkFinalProbConsistency() {
			int i = 0;
			for (ChartColumn col : chart) {
				double sentenceProbAlt = ProbBox.getTotalSumInnerOuterMargProb(col.scanStatesSet);
				double diff = Math.abs(treeLogProb - sentenceProbAlt);
				if (diff > 0.00000001) {
					System.err.println(sentenceIndex + " " + diff);
				}
				if (++i == sentenceLength)
					break;
			}
		}

		// for outer probabilities
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
					present.outerProb = outerContribution;
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
			HashMap<TermLabel, HashMap<ChartState, ProbBox>> endStates = getEndStatesTable();
			ChartStateColumnIndexSubScritpIndexPriorityQueue allUsedChartStateInOrder = new ChartStateColumnIndexSubScritpIndexPriorityQueue();
			for (HashMap<ChartState, ProbBox> finalStateTable : endStates.values()) {
				for (Entry<ChartState, ProbBox> e : finalStateTable.entrySet()) {
					ChartState cs = e.getKey();
					ProbBox pb = cs.probBox;
					pb.outerProb = 0d; // outer of final state is 1
					allUsedChartStateInOrder.add(cs, 0); // final states are
															// scan=0
				}
			}
			allUsedChartStateInOrder.computeOuterProbabilities(false);
		}

		public ArrayList<ChartState> retrieveRandomViterbiPath(double[] viterbiLogProb) {
			HashMap<TermLabel, HashMap<ChartState, ProbBox>> endStates = getEndStatesTable();
			Entry<ChartState, ProbBox> topStateEntry = ProbBox.getMaxInnerViterbiEntryDouble(endStates);
			viterbiLogProb[0] = topStateEntry.getValue().innerViterbiProb;
			ArrayList<ChartState> path = new ArrayList<ChartState>();
			ChartState cs = topStateEntry.getKey();
			retrieveViterbiBestPathRecursiveBackTrace(path, cs, false);
			return path;
		}

		public HashMap<TermLabel, HashMap<ChartState, ProbBox>> getEndStatesTable() {
			ChartColumn lastColumn = chart.get(sentenceLength);
			return addSTOP ? lastColumn.scanStatesSet : lastColumn.subForwardFirstStatesSet;
		}

		public double getTreeProbability() {
			HashMap<TermLabel, HashMap<ChartState, ProbBox>> endStates = getEndStatesTable();
			return ProbBox.getTotalSumInnerDouble(endStates);
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

		class ChartColumn {

			TermLabel nextWordTermLabel;
			TSNodeLabel nextWordNode;
			Label nextWordLabel;
			int nextWordIndex;

			HashMap<TermLabel, HashMap<ChartState, ProbBox>> scanStatesSet, // indexed
																			// on
																			// root
					subBackwardFirstStatesSet, // indexed on non-term after dot
					subForwardFirstStatesSet, // indexed on root
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

			HashMap<FragFringe, TSNodeLabel> firstLexFringeFragMapping,
					firstSubFringeFragMapping;

			public ChartColumn(TermLabel nextWord, TSNodeLabel wordTreeNode, int i) {
				this.nextWordTermLabel = nextWord;
				this.nextWordNode = wordTreeNode;
				this.nextWordLabel = nextWord.label;
				this.nextWordIndex = i;
				initVariabels();
			}

			public Vector<HashMap<TermLabel, HashMap<ChartState, ProbBox>>> getAllChartStateSets() {
				Vector<HashMap<TermLabel, HashMap<ChartState, ProbBox>>> result = new Vector<HashMap<TermLabel, HashMap<ChartState, ProbBox>>>(4);
				result.add(scanStatesSet);
				result.add(subBackwardFirstStatesSet);
				result.add(subForwardFirstStatesSet);
				result.add(completeStatesSet);
				return result;
			}

			private void initVariabels() {
				scanStatesSet = new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
				subBackwardFirstStatesSet = new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
				subForwardFirstStatesSet = new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
				completeStatesSet = new HashMap<TermLabel, HashMap<ChartState, ProbBox>>();
				firstLexFringeFragMapping = new HashMap<FragFringe, TSNodeLabel>();
				firstSubFringeFragMapping = new HashMap<FragFringe, TSNodeLabel>();
				populateFirstLexFragFringeSet();
				populateFirstSubFragFringeSet();
			}

			private void populateFirstLexFragFringeSet() {
				firstLexFragFringeSet = new HashMap<TermLabel, HashMap<FragFringe, double[]>>();
				if (nextWordIndex == sentenceLength) {
					return;
				}

				HashMap<Label, HashMap<TSNodeLabel, double[]>> lexFragTable = (nextWordIndex > 0) ? firstLexFragments.get(nextWordLabel) : initFragments.get(nextWordLabel);

				if (lexFragTable == null)
					return;

				for (HashMap<TSNodeLabel, double[]> e : lexFragTable.values()) {
					for (Entry<TSNodeLabel, double[]> fragProb : e.entrySet()) {
						TSNodeLabel frag = fragProb.getKey();
						if (fragInTreeFromTerm(frag.getLeftmostLexicalNode(), nextWordNode)) {
							FragFringe ff = new FragFringe(frag);
							Utility.putInMapDouble(firstLexFragFringeSet, ff.root, ff, fragProb.getValue());
							firstLexFringeFragMapping.put(ff, frag);
						}
					}
				}
			}

			private void populateFirstSubFragFringeSet() {
				firstSubFragFringeSet = new HashMap<TermLabel, HashMap<FragFringe, double[]>>();
				if (nextWordIndex == 0) {
					return;
				}
				boolean stopFrag = nextWordIndex == sentenceLength;
				HashMap<Label, HashMap<TSNodeLabel, double[]>> subTable = firstSubFragments.get(nextWordLabel);
				if (subTable == null)
					return;
				for (HashMap<TSNodeLabel, double[]> e : subTable.values()) {
					for (Entry<TSNodeLabel, double[]> fragProb : e.entrySet()) {
						TSNodeLabel frag = fragProb.getKey();
						if (stopFrag || fragInTreeFromTerm(frag.getLeftmostLexicalNode(), nextWordNode)) {
							FragFringe ff = new FragFringe(frag);
							Utility.putInMapDouble(firstSubFragFringeSet, ff.firstTerminal(), ff, fragProb.getValue());
							firstSubFringeFragMapping.put(ff, frag);
						}
					}
				}
			}

			private ChartColumn previousChartColumn() {
				return chart.get(nextWordIndex - 1);
			}

			public String toString() {
				StringBuilder sb = new StringBuilder();
				sb.append("Chart Column\n");
				sb.append("\tWord index: " + nextWordIndex + "\n");
				sb.append("\tWord: " + nextWordTermLabel + "\n");
				sb.append("\tScan States: " + scanStatesSet.size() + "\n");
				sb.append("\tSub-Down States: " + Utility.countTotalDouble(subBackwardFirstStatesSet) + "\n");
				sb.append("\tSub-Up States: " + Utility.countTotalDouble(subForwardFirstStatesSet) + "\n");
				sb.append("\tComplete States: " + Utility.countTotalDouble(completeStatesSet));
				return sb.toString();
			}

			private void performStart() {
				for (Entry<TermLabel, HashMap<FragFringe, double[]>> e : firstLexFragFringeSet.entrySet()) {
					for (Entry<FragFringe, double[]> f : e.getValue().entrySet()) {
						FragFringe lexFragFringe = f.getKey();
						ChartState cs = new ChartState(lexFragFringe, 0);
						double fringeViterbiProb = f.getValue()[0]; // innerViterbi/forwardViterbi
						ProbBox.addProbState(scanStatesSet, lexFragFringe.root, cs, fringeViterbiProb, fringeViterbiProb, null);
						// cs.probBox.forwardViterbiProb = currentInnerViterbi;
					}
				}
			}

			private void performScan() {
				ChartColumn nextChartColumn = chart.get(nextWordIndex + 1);
				for (HashMap<ChartState, ProbBox> scanRootSet : scanStatesSet.values()) {
					for (Entry<ChartState, ProbBox> e : scanRootSet.entrySet()) {
						ChartState scanState = e.getKey();
						ProbBox pb = e.getValue();
						ChartState newState = nextChartColumn.new ChartState(scanState);
						newState.advanceDot();
						nextChartColumn.identifyAndAddProbState(newState, pb.innerProb, pb.innerViterbiProb, scanState);
						// always added apart when next is lex and lookahead
						// doesn't match
					}
				}
			}

			private void performSubDown() {

				Set<TermLabel> firstSubSiteSet = subBackwardFirstStatesSet.keySet();
				Set<TermLabel> secondRootSet = firstLexFragFringeSet.keySet();

				firstSubSiteSet.retainAll(secondRootSet);
				secondRootSet.retainAll(firstSubSiteSet);

				for (TermLabel root : secondRootSet) {
					HashMap<FragFringe, double[]> subDownSecondMap = firstLexFragFringeSet.get(root);
					HashMap<ChartState, ProbBox> firstSubDownMatch = subBackwardFirstStatesSet.get(root);
					Entry<ChartState, ProbBox> firstSubDownBestEntry = ProbBox.getMaxInnerViterbiEntry(firstSubDownMatch);
					ChartState firstSubDownBestState = firstSubDownBestEntry.getKey();

					for (Entry<FragFringe, double[]> e : subDownSecondMap.entrySet()) {
						FragFringe subDownSecondFragFringe = e.getKey();
						ChartState newState = new ChartState(subDownSecondFragFringe, nextWordIndex);
						double secondViterbiProb = e.getValue()[0];
						double newInnerMargProb = secondViterbiProb;
						double newInnerViterbiProb = secondViterbiProb;

						ProbBox.addProbState(scanStatesSet, subDownSecondFragFringe.root, newState, newInnerMargProb, newInnerViterbiProb, firstSubDownBestState);
						// new state always new

						// add attach stats
						Label secondLex = usingPosAttachment ?
							wordNodes.get(nextWordIndex).parent.label :
							subDownSecondFragFringe.firstLex().label;						
						for (ChartState firstCs : firstSubDownMatch.keySet()) {
							int firstLexIndex = firstCs.subScriptIndex;
							Label firstLex = usingPosAttachment ?
								wordNodes.get(firstLexIndex).parent.label :
								firstCs.fragFringe.firstLex().label;							
							int dst = nextWordIndex - firstLexIndex;
							AttachStatsWordPair.addAttachStats(attachStatsLocalTable, 
									firstLex, secondLex, dst, true);
							sentenceAttachStats.addSubBackwardRelation(firstLexIndex, nextWordIndex);
						}
						
						/*
						ArrayList<Label> secondLexList = subDownSecondFragFringe.collectLexLabels();
						secondLexList.remove(0);
						for(Label presenceLex : secondLexList) {
							sentenceAttachStats.addPresenceStats(attachStatsLocalTable, 
									firstLex, secondLex, true);
						}
						*/
					}
				}
			}

			private void performSubUp() {

				Set<TermLabel> firstRootSet = subForwardFirstStatesSet.keySet();
				Set<TermLabel> secondSubSiteSet = firstSubFragFringeSet.keySet();

				firstRootSet.retainAll(secondSubSiteSet);
				secondSubSiteSet.retainAll(firstRootSet);

				for (TermLabel secondSubSite : secondSubSiteSet) {
					HashMap<FragFringe, double[]> subUpSecondTable = firstSubFragFringeSet.get(secondSubSite);
					HashMap<ChartState, ProbBox> firstSubUpMatch = subForwardFirstStatesSet.get(secondSubSite);
					double totalSumInnerProb = ProbBox.getTotalSumInnerProb(firstSubUpMatch);
					Entry<ChartState, ProbBox> firstSubUpBestEntry = ProbBox.getMaxInnerViterbiEntry(firstSubUpMatch);
					ChartState mostProbSubUpFirstState = firstSubUpBestEntry.getKey();
					ProbBox maxPB = firstSubUpBestEntry.getValue();
					double bestInnerViterbiFirst = maxPB.innerViterbiProb;
					for (Entry<FragFringe, double[]> e : subUpSecondTable.entrySet()) {
						FragFringe subUpSecondFragFringe = e.getKey();
						double secondProb = e.getValue()[0];
						double newInnerProb = totalSumInnerProb + secondProb;
						// assert newInnerProb!=Double.NEGATIVE_INFINITY;
						double newInnerViterbiProb = bestInnerViterbiFirst + secondProb;
						ChartState newState = new ChartState(subUpSecondFragFringe, 0);
						newState.advanceDot();
						ProbBox.addProbState(scanStatesSet, subUpSecondFragFringe.root, newState, newInnerProb, newInnerViterbiProb, mostProbSubUpFirstState);
						// new state always new
						
						// add attach stats
						Label secondLex = usingPosAttachment ?
								wordNodes.get(nextWordIndex).parent.label :
								subUpSecondFragFringe.firstLex().label;						
						for (ChartState firstCs : firstSubUpMatch.keySet()) {
							int firstLexIndex = firstCs.subScriptIndex;
							Label firstLex = usingPosAttachment ?
								wordNodes.get(firstLexIndex).parent.label :
								firstCs.fragFringe.firstLex().label;							
							int dst = nextWordIndex - firstLexIndex;
							AttachStatsWordPair.addAttachStats(attachStatsLocalTable, firstLex, 
									secondLex, dst, false);
							sentenceAttachStats.addSubForwardRelation(firstLexIndex, nextWordIndex);
						}
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
					ChartState cs = completeStatesQueue.poll();
					assert cs.probBox != null;
					double csProbViterbi = cs.probBox.innerViterbiProb;
					double csProbMarg = cs.probBox.innerProb;
					int index = cs.subScriptIndex;
					TermLabel root = cs.fragFringe.root;
					HashMap<ChartState, ProbBox> pastStateSubDownSet = chart.get(index).subBackwardFirstStatesSet.get(root);

					for (Entry<ChartState, ProbBox> e : pastStateSubDownSet.entrySet()) {
						ChartState pastStateSubDown = e.getKey();
						ProbBox pastStateSubDownPb = e.getValue();
						double currentInnerMargProb = pastStateSubDownPb.innerProb + csProbMarg;
						// assert
						// currentInnerMargProb!=Double.NEGATIVE_INFINITY;
						double currentInnerViterbiProb = pastStateSubDownPb.innerViterbiProb + csProbViterbi;
						ChartState newState = new ChartState(pastStateSubDown);
						newState.advanceDot();
						if (identifyAndAddProbState(newState, currentInnerMargProb, currentInnerViterbiProb, cs)) {
							if (newState.isCompleteState())
								completeStatesQueue.add(newState);
						}
					}

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
					double newMargViterbi = pb.innerProb;
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
						pb.innerProb = newMargViterbi;
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

			public boolean identifyAndAddProbState(ChartState cs, double currentInnerProb, double currrentInnerViterbiProb, ChartState previousCs) {

				if (!cs.hasElementAfterDot()) {
					assert previousCs != null;
					TermLabel root = cs.root();
					if (cs.isStarred)
						return ProbBox.addProbState(subForwardFirstStatesSet, root, cs, currentInnerProb, currrentInnerViterbiProb, previousCs);
					return ProbBox.addProbState(completeStatesSet, root, cs, currentInnerProb, currrentInnerViterbiProb, previousCs);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {
					if (nextTerm == nextWordTermLabel) {
						return ProbBox.addProbState(scanStatesSet, cs.root(), cs, currentInnerProb, currrentInnerViterbiProb, previousCs);
					}
					return false;
				}
				return ProbBox.addProbState(subBackwardFirstStatesSet, nextTerm, cs, currentInnerProb, currrentInnerViterbiProb, previousCs);
			}

			public ProbBox identifyStateAndGetProbBox(ChartState cs) {
				if (!cs.hasElementAfterDot()) {
					TermLabel root = cs.root();
					if (cs.isStarred)
						return subForwardFirstStatesSet.get(root).get(cs);
					return completeStatesSet.get(root).get(cs);
				}
				TermLabel nextTerm = cs.peekAfterDot();
				if (nextTerm.isLexical) {
					return scanStatesSet.get(cs.root()).get(cs);
				}
				return subBackwardFirstStatesSet.get(nextTerm).get(cs);
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
							for (ChartState subDownFirst : subBackwardFirstStatesSet.get(this.root()).keySet()) {
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
							for (Entry<ChartState, ProbBox> e : subForwardFirstStatesSet.get(beforeDot).entrySet()) {
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
						HashMap<ChartState, ProbBox> previousSubDownFirstStateTable = previousStateColumn.subBackwardFirstStatesSet.get(beforeDot);
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

					Double currentOuter = probBox.outerProb;
					if (currentOuter.isNaN())
						return;

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
							HashMap<ChartState, ProbBox> subUpFirstTable = subForwardFirstStatesSet.get(beforeDot);
							double firstSubFragMargProb = firstSubFragFringeSet.get(beforeDot).get(fragFringe)[0];
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
						double completeStateInner = completeState.probBox.innerProb;
						int previousStateColumnIndex = completeState.subScriptIndex;
						ChartColumn previousStateColumn = chart.get(previousStateColumnIndex);
						ChartState previousState = previousStateColumn.new ChartState(this);
						previousState.retreatDot();
						HashMap<ChartState, ProbBox> previousSubDownFirstStateTable = previousStateColumn.subBackwardFirstStatesSet.get(beforeDot);
						if (previousSubDownFirstStateTable != null) {
							ProbBox previousPb = previousSubDownFirstStateTable.get(previousState);
							if (previousPb != null) {
								previousState.probBox = previousPb;
								double outerContributionPreviousForSubDownFirst = currentOuter + completeStateInner;
								double outerContributionForCompleteState = currentOuter + previousPb.innerProb;

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
						assert !previousState.probBox.outerProb.isNaN();
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
							HashMap<ChartState, ProbBox> subUpFirstTable = subForwardFirstStatesSet.get(beforeDot);
							double innerTotal = ProbBox.getTotalSumInnerProb(subUpFirstTable);
							for (ChartState previousSubUpFirst : subUpFirstTable.keySet()) {
								ProbBox previousPb = previousSubUpFirst.probBox;
								assert !previousPb.outerProb.isNaN();
								double innerContribution = previousPb.innerProb;
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
						HashMap<ChartState, ProbBox> previousSubDownFirstStateTable = previousStateColumn.subBackwardFirstStatesSet.get(beforeDot);
						if (previousSubDownFirstStateTable != null) {
							ProbBox previousPb = previousSubDownFirstStateTable.get(previousState);
							if (previousPb != null) {
								assert !previousPb.outerProb.isNaN();
								Integer previousStartIndex = previousState.nextWordIndex();
								double innerContribution = completeState.probBox.innerProb;

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
							ChartState subDownFirst = ProbBox.getMaxInnerViterbiEntry(subBackwardFirstStatesSet.get(this.root())).getKey();
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
							return new Duet<ChartState, ChartState>(null, ProbBox.getMaxInnerViterbiEntry(subForwardFirstStatesSet.get(beforeDot)).getKey());
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
						HashMap<ChartState, ProbBox> previousSubDownFirstStateTable = previousStateColumn.subBackwardFirstStatesSet.get(beforeDot);
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
					bestPreviousState.probBox = previousStateColumn.subBackwardFirstStatesSet.get(beforeDot).get(bestPreviousState);
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
		TSNodeLabel t = new TSNodeLabel("(TOP (S (NP (VBN Estimated) (NN volume)) (VP (VBD was) (NP (DT a) (JJ moderate) (QP (CD 3.5) (CD million)) (NNS ounces))) (. .)))");
		ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
		AttachStatsSentence ass = new AttachStatsSentence(lex);
		ass.addSubBackwardRelation(0, 1);
		ass.addSubBackwardRelation(1, 4);
		ass.addSubBackwardRelation(4, 2);
		ass.addSubBackwardRelation(5, 3);
		ass.addSubForwardRelation(0, 1);
		ass.addSubForwardRelation(6, 2);
		ass.addSubForwardRelation(7, 6);
		ass.addSubForwardRelation(8, 1);
		System.out.println(ass.toStringTex());
	}

	public static void main(String[] args) throws Exception {

		threads = 7;

		addSTOP = false;

		usingPosAttachment = true;
		initFragSmoothing = false;
		minFreqFirstWordFragSmoothing = 1E-1;
		normalizedCounts = false;

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

		FE.posSmoothing = false;
		new IncrementalTsgLexAttachmentAnalysis(FE, trainTB, outputPathParsing).run();

	}

}
