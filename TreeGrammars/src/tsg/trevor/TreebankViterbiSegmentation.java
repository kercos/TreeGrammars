package tsg.trevor;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Scanner;

import kernels.NodeSetCollector;
import kernels.NodeSetCollectorSimple;
import kernels.NodeSetCollectorStandard;
import settings.Parameters;
import tsg.*;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class TreebankViterbiSegmentation extends Thread {

	public static int threads = 1;
	public static int printProgressEvery = 1;
	static boolean computeLogProb = true;

	
	String workingDir;
	ArrayList<TSNodeLabelIndex> trainingCorpus;
	Hashtable<TSNodeLabel, double[]> fragmentTableLogProb;
	PrintProgress printProgress;
	int treebankSize;
	
	
	public TreebankViterbiSegmentation ( File corpusFile, 
			File fragmentFile, String workingDir) throws Exception {		
		readTreeBank(corpusFile);
		readFragmentsFile(fragmentFile);
		this.workingDir = workingDir;		
		addCFGfragments();
		if (computeLogProb)
			convertFreqToLogProb();
		printFragmentTable(new File(workingDir + "fragLogProb.txt"));
	}


	/**
	 * Read the treebank. Every structure is now represented as a TSNodeLabelIndex,
	 * a recursive structure of nodes where every node has a unique index in a depth-first
	 * manner (the root has index 0).  
	 * @param treebankFile
	 * @throws Exception
	 */
	private void readTreeBank(File trainingFile) throws Exception {
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingFile);
		trainingCorpus = new ArrayList<TSNodeLabelIndex>();
		for(TSNodeLabel t : treebank) {
			trainingCorpus.add(new TSNodeLabelIndex(t));
		}		
		treebankSize = treebank.size();
		Parameters.reportLineFlush("Corpus size: " + treebankSize);
	}
	
	/**
	 * Fill in the hashtable fragmentTableLogFreq with the fragments present in the
	 * input file (discarding fragments having a frequency below the given
	 * threshold minFreqFragment). Every fragment is now mapped to its log-frequency.
	 * @param fragmentFile
	 * @throws Exception
	 */
	private void readFragmentsFile(File fragmentFile) throws Exception {
		fragmentTableLogProb = new Hashtable<TSNodeLabel, double[]>();
		Scanner scan = FileUtil.getScanner(fragmentFile);
		int countFragments = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			countFragments++;
			String[] fragmentFreq = line.split("\t");
			String fragmentString = fragmentFreq[0];			
			double freq = Double.parseDouble(fragmentFreq[1]);			
			TSNodeLabel fragment= new TSNodeLabel(fragmentString, false);			
			fragmentTableLogProb.put(fragment, new double[]{freq});
		}
		Parameters.reportLineFlush("Read " + countFragments + " fragments");
		scan.close();
	}

	
	private void printFragmentTable(File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<TSNodeLabel,double[]> e : fragmentTableLogProb.entrySet()) {
			String fragmentString = e.getKey().toString(false, true);
			double logProb = e.getValue()[0];
			pw.println(fragmentString + "\t" + logProb) ;
		}
		pw.close();
		Parameters.reportLineFlush("Printed fragment logProb table to: " + outputFile);
	}
	
	/**
	 * Add in the fragment hashtable the CFG extracted from the treebank (if not
	 * already present), with their log-frequencies.
	 * @throws Exception
	 */
	private void addCFGfragments() throws Exception {		
		Hashtable<String, int[]> ruleTable = new Hashtable<String, int[]>(); 
		for(TSNodeLabel t : trainingCorpus) {
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();
				Utility.increaseInTableInt(ruleTable, rule);
			}
		}
		Parameters.reportLineFlush("Read " + ruleTable.size() + " CFG fragments");
		int kept = 0;
		for(Entry<String, int[]> e : ruleTable.entrySet()) {
			TSNodeLabel ruleFragment = new TSNodeLabel("( " + e.getKey() + ")", false);
			if (fragmentTableLogProb.containsKey(ruleFragment)) continue;
			int freq = e.getValue()[0];
			fragmentTableLogProb.put(ruleFragment, new double[]{freq});
			kept++;
		}
		Parameters.reportLineFlush("Added " + kept + " CFG fragments");
		
	}
	
	
	
	/**
	 * Fill the hashtable rootTableLogFreq with the log-frequencies of the root labels
	 * of the fragments.
	 */
	private void convertFreqToLogProb() {
		Hashtable<Label, double[]> rootTableLogFreq = new Hashtable<Label, double[]>(); 		
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogProb.entrySet()) {
			Label rootLabel = e.getKey().label;
			double freq = e.getValue()[0];
			Utility.increaseInTableDoubleArray(rootTableLogFreq, rootLabel, freq);
			// add logFreq to the value of rootLable in the table (taking care of sum of logs)
		}
		Parameters.reportLineFlush("Built root freq. table: " + rootTableLogFreq.size() + " entries.");
		
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogProb.entrySet()) {
			Label rootLabel = e.getKey().label;
			double[] freqArray = e.getValue();
			double freq = freqArray[0];
			double prob = freq / rootTableLogFreq.get(rootLabel)[0];
			freqArray[0] = Math.log(prob);
		}
		
		Parameters.reportLineFlush("Converted fragmetn freq to logProb.");
	}


	public void run() {				
		try {
			runViterbiSegmentation();
		} catch (InterruptedException e) {			
			Parameters.reportError(e.getMessage());
		}
	}

	public void runViterbiSegmentation() throws InterruptedException {		
		int sentencesPerThreads = treebankSize / threads;
		int remainingSentences = treebankSize % threads;
		if (remainingSentences!=0) sentencesPerThreads++;
		
		printProgress = new PrintProgress("Segmenting tree:", printProgressEvery, 0);
		SegmentingThreadRunner[] bitParThreadArray = new SegmentingThreadRunner[threads];
				
		for(int i=0; i<threads; i++) {
			int startIndex = sentencesPerThreads*i;
			SegmentingThreadRunner t = null;;
			if (i<threads-1) {
				int endIndex = sentencesPerThreads*(i+1);
				ArrayList<TSNodeLabelIndex> subtreebank = 
					new ArrayList<TSNodeLabelIndex>(trainingCorpus.subList(startIndex, endIndex));
				t = new SegmentingThreadRunner(subtreebank);
				bitParThreadArray[i] = t;				
				t.start();								
			}
			else {
				ArrayList<TSNodeLabelIndex> subtreebank = 
					new ArrayList<TSNodeLabelIndex>(trainingCorpus.subList(startIndex, treebankSize));
				t = new SegmentingThreadRunner(subtreebank);
				t.run();						
			}
			bitParThreadArray[i] = t;
		}
		
		for(SegmentingThreadRunner t : bitParThreadArray) { 
			t.join();				
		}
		
		ArrayList<TSNodeLabel> newTreebank = new ArrayList<TSNodeLabel>();
		Hashtable<TSNodeLabel, int[]> newFragFreqTable = new Hashtable<TSNodeLabel, int[]>(); 
		
		for(SegmentingThreadRunner t : bitParThreadArray) {
			newTreebank.addAll(t.newSubTreebank);
			Utility.addAll(t.newFragFreqTable, newFragFreqTable);
		}
		
		TSNodeLabel.printTreebankToFile(new File(workingDir + "segmentedTreebank.mrg"), newTreebank, false, false);
		printFragFreq(newFragFreqTable, new File(workingDir + "fragCountsSegmentation.txt"));
				
	}


	private static void printFragFreq(Hashtable<TSNodeLabel, int[]> fragFreqTable, File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<TSNodeLabel, int[]> e : fragFreqTable.entrySet()) {
			pw.println(e.getKey().toString(false, true) + "\t" + e.getValue()[0]);
		}
		pw.close();		
	}


	public synchronized void doneWithOneSentence() {
		printProgress.next(printProgressEvery);
	}
	
	protected class SegmentingThreadRunner extends Thread {

		Hashtable<TSNodeLabel, double[]> fragTableLogProbLocal;
		ArrayList<TSNodeLabelIndex> subTreebank;
		ArrayList<TSNodeLabelIndex> newSubTreebank;
		Hashtable<TSNodeLabel, int[]> newFragFreqTable;
		
		public SegmentingThreadRunner(ArrayList<TSNodeLabelIndex> subTreebank) {
			fragTableLogProbLocal = new Hashtable<TSNodeLabel, double[]>(fragmentTableLogProb);
			this.subTreebank = subTreebank;
			newSubTreebank = new ArrayList<TSNodeLabelIndex>();
			newFragFreqTable = new Hashtable<TSNodeLabel, int[]>();
		}
		
		public void run() {					
			for(TSNodeLabelIndex t : subTreebank) {
				getViterbiSegmentation(t);
				doneWithOneSentence();
			}
		}

		private void getViterbiSegmentation(TSNodeLabelIndex t) {
			
			NodeSetCollectorSimple setCollector = new NodeSetCollectorSimple(); 
			// setCollector is a set of bitsets, each representing a certain fragment contained in the
			// current tree. The bitset {0} represents the CFG-rule having the root of the tree as LHS,
			// the bitset {0,1} represents the fragment including the starting CFG-rule connected to the CFG-rule
			// of the first daughter node of the root of the structure. The union of all the bitsets in setCollector
			// should contain all the node-indexes of the current tree, except for the terminals. 
			HashMap<BitSet, TSNodeLabelFreqDouble> bitSetFreqTable = new HashMap<BitSet, TSNodeLabelFreqDouble>();
			for(Entry<TSNodeLabel, double[]> e : fragTableLogProbLocal.entrySet()) {			
				getCFGSetCoveringFragment(t, e.getKey(), e.getValue()[0], setCollector, bitSetFreqTable);
			}
			TSNodeLabelStructure tStructure = new TSNodeLabelStructure(t);

			// build the Segmentation Chart for the current structure
			SegmentationChart SC = new SegmentationChart(setCollector, tStructure, bitSetFreqTable);
			SC.buildChart();			
			
			ArrayList<Integer> segmentationIndexes = new ArrayList<Integer>();
			SC.IOSubNodesChart[0].tracebackSegmentation(segmentationIndexes, newFragFreqTable);
			
			TSNodeLabelIndex segmentedTree = markSegmentationInTree(segmentationIndexes, t.clone());
			newSubTreebank.add(segmentedTree);			
			
		}

		private TSNodeLabelIndex markSegmentationInTree(
				ArrayList<Integer> segmentationIndexes, TSNodeLabelIndex t) {
			
			TSNodeLabelStructure tStructure = new TSNodeLabelStructure(t);
			TSNodeLabelIndex[] structure = tStructure.structure;
			
			for(int i : segmentationIndexes) {
				TSNodeLabel subNode = structure[i]; 
				subNode.relabel("@" + subNode.label());
			}
			return structure[0];
		}

		/**
		 * Recursively find weather the current tree 't' contains the fragment 'fragment'.
		 * If so, it adds in setCollector the set(s) of nodes (possibly more the one) which match 'fragment',
		 * and add it as well in the bitSetFreqLogTable which keep track of the 'fragment' log-frequency.
		 * 
		 * @param t
		 * @param fragment
		 * @param fragmentFreq
		 * @param setCollector
		 * @param bitSetFreqLogTable
		 */
		private void getCFGSetCoveringFragment(TSNodeLabelIndex t, TSNodeLabel fragment, 
				double fragmentFreq, NodeSetCollector setCollector, 
				HashMap<BitSet,TSNodeLabelFreqDouble> bitSetFreqLogTable) {
			if (t.isLexical) return;		
			if (t.sameLabel(fragment)) {
				BitSet set = new BitSet();
				if (getCFGSetCoveringFragmentNonRecursive(t, fragment, set) && !set.isEmpty()) {
					setCollector.add(set);
					bitSetFreqLogTable.put(set, new TSNodeLabelFreqDouble(fragment, fragmentFreq));
				}
			}		
			for(TSNodeLabel d : t.daughters) {
				TSNodeLabelIndex di = (TSNodeLabelIndex)d;
				getCFGSetCoveringFragment(di, fragment, fragmentFreq, setCollector, bitSetFreqLogTable);
			}		
		}

		/**
		 * Check weather the current tree 't' starts with the fragment 'fragment'
		 * @param t
		 * @param fragment
		 * @param set
		 * @return
		 */
		private boolean getCFGSetCoveringFragmentNonRecursive(TSNodeLabelIndex t, 
				TSNodeLabel fragment, BitSet set) {
			if  (t.isLexical || fragment.isTerminal()) return true;
			if (!t.sameDaughtersLabel(fragment)) return false;
			int prole = t.prole();
			for(int i=0; i<prole; i++) {
				TSNodeLabel thisDaughter = t.daughters[i];
				TSNodeLabel otherDaughter = fragment.daughters[i];
				TSNodeLabelIndex thisDaughterIndex = (TSNodeLabelIndex) thisDaughter; 
				if (!getCFGSetCoveringFragmentNonRecursive(thisDaughterIndex, otherDaughter, set)) return false;
			}
			set.set(t.index);		
			return true;					
		}

		class SegmentationChart {
			
			NodeSetCollectorSimple setCollector;
			TSNodeLabelStructure t;
			int totalNodes;
			IOSubNode[] IOSubNodesChart;
			// the chart of internal node of the current tree each keeping track of inside and outside prob.
			NodeSetCollectorStandard[] nodesCollector;
			// nodesCollector[i] contains all the fragments in the current tree 't' rooted on node-index i.
			HashMap<BitSet, TSNodeLabelFreqDouble> bitSetFreqTable;
			
			public SegmentationChart(NodeSetCollectorSimple setCollector, TSNodeLabelStructure t, 
					HashMap<BitSet, TSNodeLabelFreqDouble> bitSetFreqTable) {
		
				this.setCollector = setCollector;				
				this.t = t; 
				totalNodes = t.length();
				IOSubNodesChart = new IOSubNode[totalNodes];
				nodesCollector = new NodeSetCollectorStandard[totalNodes];
				this.bitSetFreqTable = bitSetFreqTable;
				checkCoverage();
			}
			
			public void buildChart() {
				// distribute all the fragments contained in this structure by root-node-index
				for(BitSet bs : setCollector.bitSetSet) { // for every fragment present in this structure
					int firstIndex = bs.nextSetBit(0); // get the node-index of the root of the fragment
					if (nodesCollector[firstIndex]==null) {
						nodesCollector[firstIndex] = new NodeSetCollectorStandard();				
					}
					nodesCollector[firstIndex].add(bs);
				}				
				buildViterbiSegmentation();
			}
			
			public void checkCoverage() {
				int covered = setCollector.uniteSubGraphs().cardinality();
				int toCover = t.structure[0].countNonLexicalNodes();
				assert covered==toCover; 
			}
		
			
			private void buildViterbiSegmentation() {
				for(int i=totalNodes-1; i>=0; i--) {
					if (t.structure[i].isLexical)
						continue;
					buildViterbiSegmentation(i);
				}
			}
			
			private void buildViterbiSegmentation(int index) {			
				NodeSetCollectorStandard setCollector = nodesCollector[index]; 
				TSNodeLabelIndex root = t.structure[index];			
				
				IOSubNode IOSubNodeIndex = new IOSubNode();
				for(BitSet initialSubTree : setCollector.bitSetArray) {	// for every fragment rooted in 'index'			
					ArrayList<Integer> subSitesIndexes = new ArrayList<Integer>();
					collectSubSites(root, initialSubTree, subSitesIndexes); // get the substitution sites of the current fragment
					
					TSNodeLabelFreqDouble treeDouble = bitSetFreqTable.get(initialSubTree);
					TSNodeLabel frag = treeDouble.tree();
					double initialSubTreeLogProb = treeDouble.freq();
					
					IOSubNodeIndex.addDerivation(frag, subSitesIndexes, initialSubTreeLogProb);					
				}
				IOSubNodesChart[index] = IOSubNodeIndex;
			}
		
			private void collectSubSites(TSNodeLabelIndex root, BitSet initialSubTree, 
					ArrayList<Integer> subSitesIndexes) {		
				for(TSNodeLabel d : root.daughters) {
					TSNodeLabelIndex di = (TSNodeLabelIndex)d;			
					int index = di.index;
					if (d.isLexical) {
						return;			
					}				
					if (!initialSubTree.get(index)) subSitesIndexes.add(index);
					else collectSubSites(di, initialSubTree, subSitesIndexes);
				}
			}
			
		
			class IOSubNode {		
				
				TSNodeLabel intialFragmentBest;
				ArrayList<Integer> subSitesBest;		
				double viterbiLogProbBest = -Double.MAX_VALUE;
				
				public void addDerivation(TSNodeLabel intialFragment, 
						ArrayList<Integer> subSites, double initFragLogProb) {
					
					double viterbi = initFragLogProb;
					for(int subNodeIndex : subSites) {
						viterbi += IOSubNodesChart[subNodeIndex].viterbiLogProbBest;							
					}
					
					if (viterbi>viterbiLogProbBest) {						
						this.intialFragmentBest = intialFragment;
						this.subSitesBest = subSites;			
						this.viterbiLogProbBest = viterbi;
					}
				}

				public void tracebackSegmentation(
						ArrayList<Integer> segmentationIndexes, 
						Hashtable<TSNodeLabel, int[]> newFragFreqTable) {
					
					Utility.increaseInTableInt(newFragFreqTable, intialFragmentBest);
					for(Integer i : subSitesBest) {
						segmentationIndexes.add(i);
						IOSubNodesChart[i].tracebackSegmentation(segmentationIndexes, newFragFreqTable);						
					}					
					
				}
				
			}		
		}
		
				
	}
	
	public static void main(String[] args) throws Exception {
		
		/*
		computeLogProb = false;
		args = new String[]{
				"./FedeTB_2Dop/singleSentence.mrg",
				"./FedeTB_2Dop/fragments_singleSentence_logProb.txt",
				"./FedeTB_2Dop/Segmentation",
				"1"
		};
		*/
		
		
		File corpusFile = ArgumentReader.readFileOption(args[0]);
		File fragmentFile = ArgumentReader.readFileOption(args[1]);
		String workingDir = args[2];		
		threads = ArgumentReader.readIntOption(args[3]);
		
		File workingDirFile = new File(workingDir); 
		if (workingDirFile.exists()) {
			workingDirFile = new File(workingDir + FileUtil.dateTimeString() + "/");
			
		}
		workingDir = workingDirFile.toString() + "/";
		workingDirFile.mkdir();
		
		Parameters.openLogFile(new File(workingDir + "log.txt"));		
		
		Parameters.reportLine("Working Dir: " + workingDirFile);
		Parameters.reportLine("Fragment File: " + fragmentFile);
		Parameters.reportLineFlush("Corpus File: " + corpusFile);										
		Parameters.reportLine("threads: " + threads);
					
		new TreebankViterbiSegmentation(corpusFile, fragmentFile, workingDir).run();		
	}

	
}
