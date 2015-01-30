package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.corpora.Wsj;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class SmoothingExperimentsHeldOut extends Thread {

	static final Label emptyLexNode = Label.getLabel("");
	
	static boolean constrainPos = false;
	static boolean constrainFreq = false;
	static int minFreqThreshold = -1;
	
	
	static int threads = 1;
	static long threadCheckingTime = 4000;

	File trainingTBfile, logFile;
	File initFragsFile, firstLexFragsFile, firstSubFragsFile;
	int heldOutSize;
	String outputPath;

	ArrayList<TSNodeLabel> trainTB, heldOutTB;
	Iterator<TSNodeLabel> iterator;
	PrintProgress progress;
	
	HashMap<Label, HashSet<Label>> lexPosMapTrain;
	HashMap<Label, int[]> lexFreqTrain;

	int treebankIteratorIndex;

	SimpleTimer globalTimer;

	HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> 
			initFragments, // indexed on lex and root
			firstLexFragments, // indexed on lex and root
			firstSubFragments; // indexed on lex and firstTerm
	
	HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> 
		initFragmentsTmp, firstLexFragmentsTmp, firstSubFragmentsTmp,
		initFragmentsSmooth, firstLexFragmentsSmooth, firstSubFragmentsSmooth;
	
	HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> 
		initFragmentsTemplates, // indexed on lex-pos and root
		firstLexFragmentsTemplates, // indexed on lex-pos and root
		firstSubFragmentsTemplates; // indexed on lex-pos and firstTerm

	public SmoothingExperimentsHeldOut(File initFrags, File firstLexFrags, File fistSubFrags,
			File trainingTBfile, int heldOutSize, String outputPath) throws Exception {
		this.initFragsFile = initFrags;
		this.firstLexFragsFile = firstLexFrags;
		this.firstSubFragsFile = fistSubFrags;	
		this.trainingTBfile = trainingTBfile;
		this.heldOutSize = heldOutSize;			
		this.outputPath = outputPath;
	}

	private void initVariables() {
		globalTimer = new SimpleTimer(true);

		outputPath += "ITSG_SmoothingExp";
		if (!constrainPos && !constrainFreq)
			outputPath += "_all";
		else {
			if (constrainPos)
				outputPath += "_posConstraint";
			if (constrainFreq)
				outputPath += "_freq" + minFreqThreshold;
		}
		outputPath += "/";

		File dirFile = new File(outputPath);
		if (dirFile.exists()) {
			System.out.println("Output dir already exists, I'll delete the content...");
			FileUtil.removeAllFileAndDirInDir(dirFile);
		} else {
			dirFile.mkdirs();
		}

		logFile = new File(outputPath + "LOG.log");

	}

	public void run() {
		initVariables();
		Parameters.openLogFile(logFile);
		getTrainHeldOutTB();
		printParameters();		
		readFragments();
		retrieveFrequencies();
		printFinalSummary();
		Parameters.closeLogFile();
	}

	private void getTrainHeldOutTB() {
		try {
			trainTB = Wsj.getTreebank(trainingTBfile);
		} catch (Exception e) {
			e.printStackTrace();
		}				
		heldOutTB = new ArrayList<TSNodeLabel>();
		
		Parameters.reportLineFlush("Original treebank size: " + trainTB.size());
		
		//Collections.shuffle(trainTB);
		Iterator<TSNodeLabel> iter = trainTB.iterator();
		
		for(int size=0; size<heldOutSize; size++) {
			TSNodeLabel next = iter.next();
			heldOutTB.add(next);
			iter.remove();
		}
		
		//if (constrainPos) {
			lexPosMapTrain = new HashMap<Label, HashSet<Label>>();
			lexFreqTrain = new HashMap<Label, int[]>();
			for(TSNodeLabel t : trainTB) {
				ArrayList<TSNodeLabel> lexNodes = t.collectLexicalItems();
				for(TSNodeLabel l : lexNodes) {
					Utility.putInHashMap(lexPosMapTrain, l.label, l.parent.label);
					Utility.increaseInHashMap(lexFreqTrain, l.label, new int[]{1});
				}
			}
		//}
		
		Parameters.reportLineFlush("Training treebank size: " + trainTB.size());
		Parameters.reportLineFlush("Held out treebank size: " + heldOutTB.size());
		
	}

	private void printParameters() {
		Parameters.reportLine("\n\n");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("INCREMENTAL TSG ESTIMATOR ");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("");
		Parameters.reportLine("Log file: " + logFile);
		Parameters.reportLine("Number of threads: " + threads);
		Parameters.reportLine("Treebank file: " + trainingTBfile);
		Parameters.reportLine("Number of trees: " + trainTB.size());
	}

	private void printFinalSummary() {
		Parameters.reportLine("Took in Total (sec): " + globalTimer.checkEllapsedTimeSec());
		Parameters.reportLineFlush("Log file: " + logFile);

	}

	private void readFragments() {

		Parameters.reportLineFlush("Converting lex fragments into fringes...");

		initFragments = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
		firstLexFragments = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
		firstSubFragments = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();

		Parameters.reportLineFlush("Reading init frags from " + initFragsFile);		
		int countInit = readFragmentFile(initFragsFile, true, initFragments);
		Parameters.reportLineFlush("\tRead frags: " + countInit);

		Parameters.reportLineFlush("Reading first lex frags from " + firstLexFragsFile);
		int countFirstLex = readFragmentFile(firstLexFragsFile, true, firstLexFragments);
		Parameters.reportLineFlush("\tRead frags: " + countFirstLex);

		Parameters.reportLineFlush("Reading first sub frags from " + firstSubFragsFile);
		int countFirstSub = readFragmentFile(firstSubFragsFile, false, firstSubFragments);
		Parameters.reportLineFlush("\tRead frags: " + countFirstSub);

		int totalFrags = countInit + countFirstLex + countFirstSub;
		Parameters.reportLineFlush("Total number of lex fragments: " + totalFrags);
		Parameters.reportLineFlush("\tInit:      " + countInit);
		Parameters.reportLineFlush("\tLex first: " + countFirstLex);
		Parameters.reportLineFlush("\tSub first: " + countFirstSub);
	}
	
	private static int readFragmentFile(File inputFile, boolean firstLex,  
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> table) {
	
		Scanner scan = FileUtil.getScanner(inputFile);
		int count = 0;
		while (scan.hasNextLine()) {			
			String[] lineSplit = scan.nextLine().split("\t");
			TSNodeLabel frag = null;
			try {
				frag = new TSNodeLabel(lineSplit[0], false);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			int freq = (int) Double.parseDouble(lineSplit[1]);
			
			Label lexLabel = frag.getLeftmostLexicalNode().label;
			Label secondKey = firstLex ? frag.label : frag.getLeftmostTerminalNode().label;
			Utility.putInMapTriple(table, lexLabel, secondKey, frag, new int[] { freq });
			count++;
		}
	
		return count;
	}

	private void retrieveFrequencies() {

		Parameters.reportLineFlush("Retrieving fragment frequencies from training");
		Parameters.reportLineFlush("Number of trees: " + trainTB.size());
		extractFrequencies(trainTB); //using tableTmp
		
		printFragFrequenciesDouble("training");
		
		// Select double frags from training (tableTmp), anonymize and marginalize frequencies 
		// from tableTmp to tableTemplate
		extractTemplateFragsFromTraining();
		
		// copy double frags from tableTmp (training) to tableSMooth 
		initSmoothing();		
		
		Parameters.reportLineFlush("Retrieving fragment frequencies from held out");
		Parameters.reportLineFlush("Number of trees: " + heldOutTB.size());
		extractFrequencies(heldOutTB); //using tableTmp
		
		printFragFrequencies("heldout", initFragmentsTmp, firstLexFragmentsTmp, firstSubFragmentsTmp);
		
		addSmoothing();
		printFragFrequencies("smoothing", initFragmentsSmooth, firstLexFragmentsSmooth, firstSubFragmentsSmooth);
		
	}


	private void addSmoothing() {
		File initSmoothedFragsFile = new File(outputPath + "onlySmoothed_initFrags.txt");
		File firstLexSmoothedFragsFile = new File(outputPath + "onlySmoothed_firstLexFrags.txt");
		File firstSubSmoothedFragsFile = new File(outputPath + "onlySmoothed_firstSubFrags.txt");
		
		Parameters.reportLineFlush("Smoothing init frags...");
		addSmoothing(initFragmentsTmp, initFragmentsTemplates, 
				initFragmentsSmooth, initSmoothedFragsFile);		
		
		Parameters.reportLineFlush("Smoothing first lex frags...");
		addSmoothing(firstLexFragmentsTmp, firstLexFragmentsTemplates, 
				firstLexFragmentsSmooth, firstLexSmoothedFragsFile);
		
		Parameters.reportLineFlush("Smoothing first sub frags...");
		addSmoothing(firstSubFragmentsTmp, firstSubFragmentsTemplates, 
				firstSubFragmentsSmooth, firstSubSmoothedFragsFile);	
		
	}

	private int addSmoothing(
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> heldFrags, 
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> templateFrags, 
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> smoothFrags,
			File additionalFrags) {
		
		int countAdding = 0;
		int countAddingNewPos = 0;
		PrintWriter pw = FileUtil.getPrintWriter(additionalFrags);
		HashSet<Label> smoothedLex = new HashSet<Label>();
		for (Entry<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> e : heldFrags.entrySet()) {
			Label firstKey = e.getKey();
			HashMap<Label, HashMap<TSNodeLabel, int[]>> firstValue = e.getValue();
			for (Entry<Label, HashMap<TSNodeLabel, int[]>> f : firstValue.entrySet()) {
				Label secondKey = f.getKey();
				HashMap<TSNodeLabel, int[]> secondValue = f.getValue();
				for (Entry<TSNodeLabel, int[]> fragFreq : secondValue.entrySet()) {
					TSNodeLabel frag = fragFreq.getKey();
					
					if (!Utility.containsThirdKey(smoothFrags, firstKey, secondKey, frag)) {
						TSNodeLabel fragAnonym = frag.clone();
						TSNodeLabel firstLex = fragAnonym.getLeftmostLexicalNode();	
						Label firstLexLabelOrig = firstLex.label;
						Label pos = firstLex.parent.label;	
						
						HashSet<Label> posSet = lexPosMapTrain.get(firstLexLabelOrig);
						boolean unseenPos = posSet==null || !posSet.contains(pos);
						
						if (constrainPos && unseenPos) 								
							continue;

						firstLex.label = emptyLexNode;
						int[] templateFreq = Utility.getThirdKey(templateFrags, pos, secondKey, fragAnonym); 
						if (templateFreq!=null && (!constrainFreq || templateFreq[0] > minFreqThreshold)) {
							int[] freq = new int[]{1};
							Utility.putInMapTriple(smoothFrags, firstKey, secondKey, frag, freq);
							countAdding++;
							if (unseenPos)
								countAddingNewPos++;
							//int[] originalFreqArray = lexFreqTrain.get(firstLexLabelOrig);
							//int originalFreq = originalFreqArray==null ? 0 : originalFreqArray[0];
							pw.println(firstLexLabelOrig + "\t" + templateFreq[0] + "\t" + unseenPos + "\t" +
									frag.toString(false, true));
							smoothedLex.add(firstLexLabelOrig);
						}							
					}
				}
			}
		}
		pw.close();
		//Parameters.reportLineFlush("\tSmoothed frags lex: " + smoothedLex);
		Parameters.reportLineFlush("\tadded frags: " + countAdding);
		Parameters.reportLineFlush("\tadded frags new pos: " + countAddingNewPos);
		return countAdding;
	}

	private void initSmoothing() {
		Parameters.reportLineFlush("Initializing smoothing frags with training double");
		initFragmentsSmooth = new HashMap<Label, HashMap<Label,HashMap<TSNodeLabel,int[]>>>();
		firstLexFragmentsSmooth = new HashMap<Label, HashMap<Label,HashMap<TSNodeLabel,int[]>>>();
		firstSubFragmentsSmooth = new HashMap<Label, HashMap<Label,HashMap<TSNodeLabel,int[]>>>();
		copyDouble(initFragmentsTmp, initFragmentsSmooth);
		copyDouble(firstLexFragmentsTmp, firstLexFragmentsSmooth);
		copyDouble(firstSubFragmentsTmp, firstSubFragmentsSmooth);				
	}
	
	private void copyDouble(
			HashMap<Label, HashMap<Label,HashMap<TSNodeLabel,int[]>>> tableSource,
			HashMap<Label, HashMap<Label,HashMap<TSNodeLabel,int[]>>> tableTarget) {
		
		for (Entry<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> e : tableSource.entrySet()) {
			Label firstKey = e.getKey();
			HashMap<Label, HashMap<TSNodeLabel, int[]>> firstValue = e.getValue();
			for (Entry<Label, HashMap<TSNodeLabel, int[]>> f : firstValue.entrySet()) {
				Label secondKey = f.getKey();
				HashMap<TSNodeLabel, int[]> secondValue = f.getValue();
				for (Entry<TSNodeLabel, int[]> fragFreq : secondValue.entrySet()) {
					TSNodeLabel frag = fragFreq.getKey();
					int[] freq = fragFreq.getValue();					
					if (freq[0] > 1) {
						Utility.putInMapTriple(tableTarget, firstKey, secondKey, frag, freq);
					}
				}
			}
		}
	}

	private void extractTemplateFragsFromTraining() {
		initFragmentsTemplates = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
		firstLexFragmentsTemplates = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
		firstSubFragmentsTemplates = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
		anonymizeAndAddSmoothingDouble(initFragmentsTmp, initFragmentsTemplates);
		anonymizeAndAddSmoothingDouble(firstLexFragmentsTmp, firstLexFragmentsTemplates);
		anonymizeAndAddSmoothingDouble(firstSubFragmentsTmp, firstSubFragmentsTemplates);
		
	}

	private void anonymizeAndAddSmoothingDouble(
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> tableFrags, 
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> tableFragsSmoothing) {
		
		for(HashMap<Label, HashMap<TSNodeLabel, int[]>> subTable : tableFrags.values()) {
			for(Entry<Label, HashMap<TSNodeLabel, int[]>> e : subTable.entrySet()) {
				Label secondKey = e.getKey();
				HashMap<TSNodeLabel, int[]> value = e.getValue();
				for(Entry<TSNodeLabel, int[]> f : value.entrySet()) {
					TSNodeLabel frag = f.getKey().clone();
					int freq = f.getValue()[0];
					if (freq>1) {
						TSNodeLabel firstLex = frag.getLeftmostLexicalNode();
						firstLex.label = emptyLexNode;
						Label pos = firstLex.parent.label;
						Utility.increaseInHashMapDouble(tableFragsSmoothing, pos, secondKey, frag, freq);
					}
				}
			}
		}
		
	}

	private void printFragFrequenciesDouble(String filePrefix ) {
		Parameters.reportLineFlush("Printing fragments to files (" + filePrefix + ").");
		File initFragFile = new File(outputPath + filePrefix + "_double_initFrags.txt");
		File firstLexFragFile = new File(outputPath + filePrefix + "_double_firstLexFrags.txt");
		File firstSubFragFile = new File(outputPath + filePrefix + "_double_firstSubFrags.txt");
		int[] countInit = printToFileDouble(initFragmentsTmp, initFragFile);
		int[] countFirstLex = printToFileDouble(firstLexFragmentsTmp, firstLexFragFile);
		int[] countFirstSub = printToFileDouble(firstSubFragmentsTmp, firstSubFragFile);
		Parameters.reportLineFlush("\tInit fragments: " + countInit[0] + 
				" (excluded unary: " + countInit[1] + ")");
		Parameters.reportLineFlush("\tFirst-Lex fragments: " + countFirstLex[0] +
				" (excluded unary: " + countFirstLex[1] + ")");
		Parameters.reportLineFlush("\tFirst-Sub fragments: " + countFirstSub[0] +
				" (excluded unary: " + countFirstSub[1] + ")");
	}

	public static int[] printToFileDouble(
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> frags, File outputFile) {

		int doubleCount = 0;
		int unaryCount = 0;
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for (HashMap<Label, HashMap<TSNodeLabel, int[]>> subTable : frags.values()) {
			for (HashMap<TSNodeLabel, int[]> subsubTable : subTable.values()) {
				for (Entry<TSNodeLabel, int[]> fragProb : subsubTable.entrySet()) {
					String fragString = fragProb.getKey().toString(false, true);
					int freq = fragProb.getValue()[0];
					if (freq > 1) {
						doubleCount++;
						pw.println(fragString + "\t" + freq);
					}
					else
						unaryCount++;
				}
			}
		}
		pw.close();
		return new int[]{doubleCount, unaryCount};
	}

	private void printFragFrequencies(String filePrefix,
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> initFrags,
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> firstLexFrags,
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> firstSubFrags) {
		Parameters.reportLineFlush("Printing fragments to files (" + filePrefix + ").");
		File initFragFile = new File(outputPath + filePrefix + "_initFrags.txt");
		File firstLexFragFile = new File(outputPath + filePrefix + "_firstLexFrags.txt");
		File firstSubFragFile = new File(outputPath + filePrefix + "_firstSubFrags.txt");
		int countInit = printToFile(initFrags, initFragFile);
		int countFirstLex = printToFile(firstLexFrags, firstLexFragFile);
		int countFirstSub = printToFile(firstSubFrags, firstSubFragFile);
		Parameters.reportLineFlush("\tInit fragments: " + countInit);
		Parameters.reportLineFlush("\tFirst-Lex fragments: " + countFirstLex);
		Parameters.reportLineFlush("\tFirst-Sub fragments: " + countFirstSub);
	}

	public static int printToFile(
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> frags, File outputFile) {

		int count = 0;
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for (HashMap<Label, HashMap<TSNodeLabel, int[]>> subTable : frags.values()) {
			for (HashMap<TSNodeLabel, int[]> subsubTable : subTable.values()) {
				for (Entry<TSNodeLabel, int[]> fragProb : subsubTable.entrySet()) {
					String fragString = fragProb.getKey().toString(false, true);
					int freq = fragProb.getValue()[0];					
					pw.println(fragString + "\t" + freq);
					count++;
				}
			}
		}
		pw.close();
		return count;
	}
	



	private void extractFrequencies(ArrayList<TSNodeLabel> treebank) {
		progress = new PrintProgress("Extracting from tree:", 1, 0);

		initFragmentsTmp = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
		firstLexFragmentsTmp = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
		firstSubFragmentsTmp = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
		iterator = treebank.iterator();

		RetrieveFragsThread[] threadsArray = new RetrieveFragsThread[threads];
		for (int i = 0; i < threads; i++) {
			RetrieveFragsThread t = new RetrieveFragsThread();
			threadsArray[i] = t;
			t.start();
		}

		for(RetrieveFragsThread t : threadsArray) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		

		progress.end();

	}

	private synchronized TSNodeLabel getNextTree(int[] index) {
		if (!iterator.hasNext())
			return null;
		index[0] = treebankIteratorIndex++;
		progress.next();
		return iterator.next();
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

	protected class RetrieveFragsThread extends Thread {

		int sentenceIndex;
		TSNodeLabel currentTree;

		ArrayList<TSNodeLabel> wordNodes;
		int sentenceLength;

		HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> 
			initFragmentsNewCounts, firstLexFragmentsNewCounts, firstSubFragmentsNewCounts;

		public void run() {
			int[] i = { 0 };
			initFragmentsNewCounts = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
			firstLexFragmentsNewCounts = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
			firstSubFragmentsNewCounts = new HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>>();
			while ((currentTree = getNextTree(i)) != null) {
				sentenceIndex = i[0];
				this.wordNodes = currentTree.collectLexicalItems();
				this.sentenceLength = wordNodes.size();
				extractFragments();
			}
			copyAllFragTables();
		}

				
		private void extractFragments() {
			boolean first = true;
			for(TSNodeLabel wn : wordNodes) {
				Label wnLabel = wn.label;
				HashMap<Label, HashMap<TSNodeLabel, int[]>> lexFragTable = null;
				if (first) {
					lexFragTable = initFragments.get(wnLabel);
					checkIfInTreeAndAdd(lexFragTable, wn, initFragmentsNewCounts);
					first = false;
				}
				else {
					lexFragTable = firstLexFragments.get(wnLabel);
					checkIfInTreeAndAdd(lexFragTable, wn, firstLexFragmentsNewCounts);
					lexFragTable = firstSubFragments.get(wnLabel);
					checkIfInTreeAndAdd(lexFragTable, wn, firstSubFragmentsNewCounts);
				}
				
			}			
		}
		
		private void checkIfInTreeAndAdd(HashMap<Label, HashMap<TSNodeLabel, int[]>> fragTable,
			TSNodeLabel treeLexNode, 
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> newTable) {
			
			if (fragTable==null)
				return;
			
			for (Entry<Label, HashMap<TSNodeLabel, int[]>> e : fragTable.entrySet()) {
				Label secondKey = e.getKey();
				HashMap<TSNodeLabel, int[]> subTable = e.getValue();
				for (TSNodeLabel frag : subTable.keySet()) {
					if (fragInTreeFromTerm(frag.getLeftmostLexicalNode(), treeLexNode)) {
						Utility.increaseInHashMapDouble(newTable, treeLexNode.label, secondKey, frag, 1);
					}
				}
			}
		}

		private void copyAllFragTables() {
			synchronized (progress) {
				Utility.increaseAllIntHashMapTriple(initFragmentsNewCounts, initFragmentsTmp);
				Utility.increaseAllIntHashMapTriple(firstLexFragmentsNewCounts, firstLexFragmentsTmp);
				Utility.increaseAllIntHashMapTriple(firstSubFragmentsNewCounts, firstSubFragmentsTmp);				
			}

		}

	}

	public static void main(String[] args) throws Exception {

		threads = 7;
		
		constrainPos = false;		
		constrainFreq = false;
		minFreqThreshold = -1;
		
		String basePath = Parameters.scratchPath;

		String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4_notop/";

		File trainTB = new File(workingPath + "wsj-02-21.mrg");
		File trainTBSmall = new File(workingPath + "wsj-02-21_first1000.mrg");
		//File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		File initFrags = new File(workingPath + "fragments_approxFreq_initFrags.txt");
		File firstLexFrags = new File(workingPath + "fragments_approxFreq_firstLexFrags.txt");
		File fistSubFrags = new File(workingPath + "fragments_approxFreq_firstSubFrags.txt");;

		String outputPathSmoothing = workingPath + "Smoothing/";
		new File(outputPathSmoothing).mkdirs();
		
		int heldOutSetSize = 1000;
		
		for(int cf : new int[]{500,1000}) {
			constrainPos = true;
			constrainFreq = true;			
			minFreqThreshold = cf;
			new SmoothingExperimentsHeldOut(initFrags, firstLexFrags, fistSubFrags, 
					trainTB, heldOutSetSize, outputPathSmoothing).run();
		}
		

	}

}
