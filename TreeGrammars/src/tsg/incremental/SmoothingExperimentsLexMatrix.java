package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.corpora.Wsj;
import util.BoundedPriorityDoubleList;
import util.ObjectDouble;
import util.ObjectInteger;
import util.PrintProgress;
import util.SimpleTimer;
import util.Utility;
import util.file.FileUtil;

public class SmoothingExperimentsLexMatrix extends Thread {

	static final Label emptyLexNode = Label.getLabel("");
	
	static int thresholdFragFreq = 0;
	
	//static int threads = 1;
	//static long threadCheckingTime = 4000;

	File logFile;
	File initFragsFile, firstLexFragsFile, firstSubFragsFile;
	File[] fragFiles;
	String outputPath;

	Iterator<TSNodeLabel> iterator;
	PrintProgress progress;

	SimpleTimer globalTimer;

	HashMap<TSNodeLabel, HashMap<TSNodeLabel, int[]>> lexFragmentsFreqOriginal; 
	// indexed on pos node of first lex	
	
	HashMap<Label, HashMap<TSNodeLabel, int[]>> templateFragmentsFreqIndex; 
	// indexed on pos label of first lex
	
	HashMap<Label, HashMap<Integer, ObjectInteger<TSNodeLabel>>> templateIndexFragmentFreq; 
	// indexed on pos label of first lex and index in templateFragmentsFreqIndex
	
	HashMap<TSNodeLabel, BitSet> lexTemplateBitsetIndexes; 
	// indexed on pos label of first lex
	
	HashMap<TSNodeLabel, HashMap<Integer, double[]>> lexPosTemplateIndexSmoothFactor; 
	// indexed on pos node of first lex
	
	LinkedHashSet<TSNodeLabel> posLexSet;
	
	double[][] similarityMatrix;
	double avgSimilarity;
	
	
	public SmoothingExperimentsLexMatrix(File initFrags, File firstLexFrags, File fistSubFrags,
			String outputPath) throws Exception {
		this.initFragsFile = initFrags;
		this.firstLexFragsFile = firstLexFrags;
		this.firstSubFragsFile = fistSubFrags;	
		fragFiles = new File[]{initFragsFile,firstLexFrags,fistSubFrags};
		this.outputPath = outputPath;
	}

	public void run() {
		initVariables();
		Parameters.openLogFile(logFile);
		printParameters();
		for(File f : fragFiles) {
			readFragments(f);
			buildReverseIndexFragFreqTable();
			buildSimilarityMatrix(100);
			extractSmoothedFragments();
			//extractSmoothedFrags();
			//printFinalFragsFreq();
			//printFinalSummary(f);
		}					
		Parameters.closeLogFile();
	}

	private void initVariables() {
		globalTimer = new SimpleTimer(true);

		outputPath += "ITSG_SmoothingExpMatrix";
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

	private void printParameters() {
		Parameters.reportLine("\n\n");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("INCREMENTAL TSG ESTIMATOR MATRIX");
		Parameters.reportLine("-----------------------");
		Parameters.reportLine("");
		Parameters.reportLine("Log file: " + logFile);
		Parameters.reportLine("Threshold Frag Freq (rejected if <): " + thresholdFragFreq);
	}

	private void printFinalSummary() {
		Parameters.reportLine("Took in Total (sec): " + globalTimer.checkEllapsedTimeSec());
		Parameters.reportLineFlush("Log file: " + logFile);

	}

	private void readFragments(File fragFile) {

		Parameters.reportLineFlush("Reading lex fragments from file: " + fragFile.getName());
		
		lexFragmentsFreqOriginal = new HashMap<TSNodeLabel, HashMap<TSNodeLabel, int[]>>();
		templateFragmentsFreqIndex = new HashMap<Label, HashMap<TSNodeLabel, int[]>> ();
		lexTemplateBitsetIndexes = new HashMap<TSNodeLabel, BitSet>();		
		posLexSet = new LinkedHashSet<TSNodeLabel>();
		
		HashSet<Label> posSet = new HashSet<Label>();
		HashSet<Label> lexSet = new HashSet<Label>();
		
		Scanner scan = FileUtil.getScanner(fragFile);
		int countAcceptedFrag = 0;
		int countFrag = 0;
		int countTemplates = 0;
		while(scan.hasNextLine()) {			
			countFrag++;
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			int freq = (int) Double.parseDouble(lineSplit[1]);
			if (freq < thresholdFragFreq)
				continue;
			countAcceptedFrag++;
			TSNodeLabel frag = null;			
			try {
				frag = new TSNodeLabel(lineSplit[0],false);
			} catch (Exception e) {
				e.printStackTrace();
			}			
						
			//store lex frag
			TSNodeLabel lexNode = frag.getLeftmostLexicalNode();			
			TSNodeLabel posLexNode = lexNode.parent;			
			lexSet.add(lexNode.label);
			posSet.add(posLexNode.label);
			Utility.putInMapDouble(lexFragmentsFreqOriginal, posLexNode, frag, new int[]{freq});			
			
			//store lexPos
			posLexSet.add(posLexNode);
			
			//store lex template
			TSNodeLabel fragTemplate = frag.clone();
			fragTemplate.getLeftmostLexicalNode().label = emptyLexNode;
			Label posLabel = posLexNode.label;
			boolean[] newTemplate = new boolean[1];
			int index = increaseInHashMapIndex(templateFragmentsFreqIndex, posLabel, 
					fragTemplate, freq, newTemplate);
			if (newTemplate[0])	{				
				countTemplates++;
			}
			
			
			//set lexTemplate index
			BitSet bs = lexTemplateBitsetIndexes.get(posLexNode);
			if (bs==null) {
				bs = new BitSet();
				lexTemplateBitsetIndexes.put(posLexNode, bs);
			}
			bs.set(index);		
		}
		
		Parameters.reportLineFlush("\tTotal frags:            " + countFrag);
		Parameters.reportLineFlush("\tTotal accepted frags:   " + countAcceptedFrag);
		Parameters.reportLineFlush("\tTotal accepted lex:     " + lexSet.size());
		Parameters.reportLineFlush("\tTotal accepted pos:     " + posSet.size());
		Parameters.reportLineFlush("\tTotal accepted pos-lex: " + posLexSet.size());
		Parameters.reportLineFlush("\tTotal templates:        " + countTemplates);
		
		

	}
	
	public static int increaseInHashMapIndex(HashMap<Label, HashMap<TSNodeLabel, int[]>> table,
			Label key, TSNodeLabel frag, int toAdd, boolean[] newTemplate) {
		HashMap<TSNodeLabel, int[]> linkedMap = table.get(key);
		if (linkedMap==null) {
			linkedMap = new HashMap<TSNodeLabel, int[]>();
			table.put(key,linkedMap);
			linkedMap.put(frag, new int[]{toAdd,0});
			newTemplate[0] = true;
			return 0;
		}
		int index = linkedMap.size();
		int[] freqIndex = linkedMap.get(frag);
		if (freqIndex == null) {
			freqIndex = new int[]{toAdd,index};
			linkedMap.put(frag,freqIndex);
			newTemplate[0] = true;
			return index;
		}
		freqIndex[0]+=toAdd;
		newTemplate[0] = false;
		return freqIndex[1];		
	}

	private void buildReverseIndexFragFreqTable() {
		templateIndexFragmentFreq = new HashMap<Label, HashMap<Integer,ObjectInteger<TSNodeLabel>>>();
		//templateFragmentsFreqIndex = new HashMap<Label, HashMap<TSNodeLabel, int[]>> ();
		
		for(Entry<Label, HashMap<TSNodeLabel, int[]>> e : templateFragmentsFreqIndex.entrySet()) {
			Label key = e.getKey();
			HashMap<TSNodeLabel, int[]> subMap = e.getValue();
			HashMap<Integer,ObjectInteger<TSNodeLabel>> newSubMap = 
				new HashMap<Integer,ObjectInteger<TSNodeLabel>>();
			templateIndexFragmentFreq.put(key, newSubMap);
			for(Entry<TSNodeLabel, int[]> f : subMap.entrySet()) {
				TSNodeLabel fragTemplate = f.getKey();
				int[] freqIndex = f.getValue();
				int freq = freqIndex[0];
				int index = freqIndex[1];
				ObjectInteger<TSNodeLabel> fragFreq = new ObjectInteger<TSNodeLabel>(fragTemplate, freq);
				newSubMap.put(index, fragFreq);
			}
		}
		
		
	}
	
	private void buildSimilarityMatrix(int printMostSimilar) {

		lexPosTemplateIndexSmoothFactor = 
			new HashMap<TSNodeLabel, HashMap<Integer, double[]>>();
		
		int posLexSize = posLexSet.size();
		int totalPairs = posLexSize*posLexSize-posLexSize;		
		
		BoundedPriorityDoubleList<ObjectDouble<String>> mostSimilar =
			printMostSimilar>0 ? 
			new BoundedPriorityDoubleList<ObjectDouble<String>>(printMostSimilar) : null;
		
		Parameters.reportLineFlush("Building similarity matrix");
		Parameters.reportLineFlush("\tTotal pos-lex:       " + posLexSize);
		Parameters.reportLineFlush("\tTotal pos-lex pairs: " + totalPairs);
		
		LinkedList<TSNodeLabel> posLexList = new LinkedList<TSNodeLabel>(posLexSet);
		// need to iterate columns from a certain index
		
		similarityMatrix = new double[posLexSize][posLexSize];
		
		PrintProgress pp = new PrintProgress("Computing pair",100,0);
		
		Iterator<TSNodeLabel> iterPosLexR = posLexSet.iterator();
		// iterate all lex-pos in the matrix (rows)
		for(int r=0; r<posLexSize; r++) {
			ListIterator<TSNodeLabel> iterPosLexC = posLexList.listIterator(r);
			TSNodeLabel posLexR = iterPosLexR.next();
			BitSet bsR = lexTemplateBitsetIndexes.get(posLexR);
			HashMap<Integer, double[]> missingRtable = new HashMap<Integer, double[]>();
			// iterate all lex-pos in the matrix (columns)
			for(int c=r; c<posLexSize; c++) {				
				TSNodeLabel posLexC = iterPosLexC.next();
				BitSet bsC = lexTemplateBitsetIndexes.get(posLexC);
				BitSet[] missingRC = new BitSet[2];
				double sim = computeSimilarityScore(bsR, bsC, posLexR, posLexC, missingRC);
				similarityMatrix[r][c] = sim;
				boolean identical = sim==1d;
				assert(c!=r || identical);
				if (c!=r) {
					pp.next();
					avgSimilarity += sim;
					if (mostSimilar!=null) {
						String pair = posLexR.toString() + "-" + posLexC.toString();
						mostSimilar.add(new ObjectDouble<String>(pair, sim));
					}
					if (!identical) {
						BitSet missingR = missingRC[0];
						addMissingIndexesFactorInTable(missingR,missingRtable,sim);
						BitSet missingC = missingRC[1];
						HashMap<Integer, double[]> missingCtable =
							lexPosTemplateIndexSmoothFactor.get(posLexC);
						if (missingCtable==null) {
							missingCtable = new HashMap<Integer, double[]>();
							lexPosTemplateIndexSmoothFactor.put(posLexC,missingCtable);
						}
						addMissingIndexesFactorInTable(missingC,missingCtable,sim);
					}
				}
			}
			lexPosTemplateIndexSmoothFactor.put(posLexR,missingRtable);
		}
		pp.end();
		
		avgSimilarity /= totalPairs;
		
		if (mostSimilar!=null) {
			Parameters.reportLineFlush("\nMost similar pos-lex pairs: ");
			Object[] list = mostSimilar.toArray();
			for(int i=printMostSimilar-1; i>=0; i--) {
				Parameters.reportLine("\t" + list[i]);
			}
			Parameters.reportLineFlush("");
		}
		
		Parameters.reportLineFlush("Average similarity: " + avgSimilarity);
		
	}


	private void addMissingIndexesFactorInTable(BitSet bs, 
			HashMap<Integer, double[]> missingRtable, double sim) {
		
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
			Utility.increaseInHashMap(missingRtable, i, sim);
		} 
	}
	
	

	private double computeSimilarityScore(BitSet bsR, BitSet bsC, TSNodeLabel posLexR, 
			TSNodeLabel posLexC, BitSet[] missingRC) {
		//int bsRcard = bsR.cardinality();
		//int bsCcard = bsC.cardinality();
		
		BitSet intersection = intersection(bsR, bsC);
		int interCard = intersection.cardinality();
		
		BitSet union = union(bsR, bsC);
		int unionCard = union.cardinality();
		
		missingRC[0] = minus(bsC, bsR);		
		missingRC[1] = minus(bsR, bsC);
		
		
		// The Jaccard coefficient measures similarity between sample sets, 
		// and is defined as the size of the intersection divided by the size of 
		// the union of the sample sets
		return ((double)interCard)/unionCard;
	}
	
	public static BitSet intersection(BitSet a, BitSet b) {
		BitSet result = (BitSet) a.clone();
		result.and(b);
		return result;
	}
	
	public static BitSet union(BitSet a, BitSet b) {
		BitSet result = (BitSet) a.clone();
		result.or(b);
		return result;
	}
	
	public static BitSet  minus(BitSet a, BitSet b) {
		BitSet result = (BitSet) a.clone();
		result.andNot(b);
		return result;
	}

	
	private void extractSmoothedFragments() {
		PrintProgress pp = new PrintProgress("Extracting smoothed frags ");
		
		Iterator<TSNodeLabel> iterPosLex = posLexSet.iterator();
		double threshold = avgSimilarity;
		int countSmoothed = 0;
		while(iterPosLex.hasNext()) {
			TSNodeLabel posLex = iterPosLex.next();
			Label posLabel = posLex.label;
			HashMap<Integer, double[]> missingFragsTable = 
				lexPosTemplateIndexSmoothFactor.get(posLex);
			for(Entry<Integer, double[]> e : missingFragsTable.entrySet()) {
				pp.next();
				Integer index = e.getKey();
				double sim = e.getValue()[0];
				ObjectInteger<TSNodeLabel> fragTemplateFreq = 
					templateIndexFragmentFreq.get(posLabel).get(index);
				//TSNodeLabel fragTemplate = fragTemplateFreq.getObject();
				double freq = fragTemplateFreq.getInteger();
				freq = psi(freq,sim);
				if (freq>threshold) {
					countSmoothed++;
				}
			}
		}
		pp.end();
		
		Parameters.reportStringFlush("Total smoothed frags: " + countSmoothed);
	}
	
	private double psi(double freq, double sim) {		
		return freq*sim;
	}
	
	/*
	private void addSmoothing() {
		File initSmoothedFragsFile = new File(outputPath + "onlySmoothed_initFrags.txt");
		File firstLexSmoothedFragsFile = new File(outputPath + "onlySmoothed_firstLexFrags.txt");
		File firstSubSmoothedFragsFile = new File(outputPath + "onlySmoothed_firstSubFrags.txt");
		
		Parameters.reportLineFlush("Smoothing init frags...");
		int countInit = addSmoothing(initFragmentsTmp, initFragmentsTemplates, 
				initFragmentsSmooth, initSmoothedFragsFile);
		Parameters.reportLineFlush("\tadded frags: " + countInit);
		
		Parameters.reportLineFlush("Smoothing first lex frags...");
		int countFirstLex = addSmoothing(firstLexFragmentsTmp, firstLexFragmentsTemplates, 
				firstLexFragmentsSmooth, firstLexSmoothedFragsFile);
		Parameters.reportLineFlush("\tadded frags: " + countFirstLex);
		
		Parameters.reportLineFlush("Smoothing first sub frags...");
		int countFirstSub = addSmoothing(firstSubFragmentsTmp, firstSubFragmentsTemplates, 
				firstSubFragmentsSmooth, firstSubSmoothedFragsFile);	
		Parameters.reportLineFlush("\tadded frags: " + countFirstSub);
		
	}

	private int addSmoothing(
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> heldFrags, 
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> templateFrags, 
			HashMap<Label, HashMap<Label, HashMap<TSNodeLabel, int[]>>> smoothFrags,
			File additionalFrags) {
		
		int countAdding = 0;
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
					//int[] freq = fragFreq.getValue();
					//if (freq[0] > 1) {
						if (!Utility.containsThirdKey(smoothFrags, firstKey, secondKey, frag)) {
							TSNodeLabel fragAnonym = frag.clone();
							TSNodeLabel firstLex = fragAnonym.getLeftmostLexicalNode();
							firstLex.label = emptyLexNode;
							Label pos = firstLex.parent.label;							
							if (Utility.containsThirdKey(templateFrags, pos, secondKey, fragAnonym)) {
								int[] freq = new int[]{1};
								Utility.putInHashMapTriple(smoothFrags, firstKey, secondKey, frag, freq);
								countAdding++;
								pw.println(frag.toString(false, true) + "\t" + freq[0]);
								smoothedLex.add(firstLex.label);
							}							
						}
					//}
				}
			}
		}
		pw.close();
		Parameters.reportLineFlush("\tSmoothed frags lex: " + smoothedLex);
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
						Utility.putInHashMapTriple(tableTarget, firstKey, secondKey, frag, freq);
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
	*/



	public static void main(String[] args) throws Exception {

		//threads = 7;
		
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

		new SmoothingExperimentsLexMatrix(initFrags, firstLexFrags, fistSubFrags, 
				outputPathSmoothing).run();
		

	}

}
