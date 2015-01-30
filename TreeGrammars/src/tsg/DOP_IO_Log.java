package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Map.Entry;

import kernels.NodeSetCollector;
import kernels.NodeSetCollectorSimple;
import kernels.NodeSetCollectorStandard;
import settings.Parameters;
import tsg.corpora.Wsj;
import util.ArgumentReader;
import util.PrintProgress;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class DOP_IO_Log {

	public static Hashtable<TSNodeLabel, double[]> fragmentTableLogFreq;
	public static Hashtable<Label, double[]> rootTableLogFreq;

	public static ArrayList<TSNodeLabelIndex> trainingCorpus;
	
	public static int minFreqFragment = 0;
	
	static PrintProgress printProgress;
	
	/**
	 * Fill in the hashtable fragmentTableLogFreq with the fragments present in the
	 * input file (discarding fragments having a frequency below the given
	 * threshold minFreqFragment). Every fragment is now mapped to its log-frequency.
	 * @param fragmentFile
	 * @throws Exception
	 */
	public static void readFragmentsFile(File fragmentFile) throws Exception {
		fragmentTableLogFreq = new Hashtable<TSNodeLabel, double[]>();
		Scanner scan = FileUtil.getScanner(fragmentFile);
		int countFragments = 0;
		int discarded = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			countFragments++;
			String[] fragmentFreq = line.split("\t");
			String fragmentString = fragmentFreq[0];			
			int freq = Integer.parseInt(fragmentFreq[1]);
			if (freq<minFreqFragment) {
				discarded++;
				continue;
			}
			double logFreq = Math.log(freq);
			TSNodeLabel fragment= new TSNodeLabel(fragmentString, false);
			fragmentTableLogFreq.put(fragment, new double[]{logFreq});
		}
		Parameters.reportLine("Read " + countFragments + " fragments");
		Parameters.reportLineFlush("Discarded " + discarded + " (freq < " + minFreqFragment + ")");
		scan.close();
	}
	
	/**
	 * Read the treebank. Every structure is now represented as a TSNodeLabelIndex,
	 * a recursive structure of nodes where every node has a unique index in a depth-first
	 * manner (the root has index 0).  
	 * @param treebank
	 * @throws Exception
	 */
	private static void readTreeBank(ArrayList<TSNodeLabel> treebank) throws Exception {
		trainingCorpus = new ArrayList<TSNodeLabelIndex>();
		for(TSNodeLabel t : treebank) {
			trainingCorpus.add(new TSNodeLabelIndex(t));
		}		
	}
	
	public static void printFragmentFreq(File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<TSNodeLabel,double[]> e : fragmentTableLogFreq.entrySet()) {
			String fragmentString = e.getKey().toString(false, true);
			double freq = Math.exp(e.getValue()[0]);
			if (freq==0) continue;
			pw.println(fragmentString + "\t" + freq) ;
		}
		pw.close();
	}
	
	/**
	 * Add in the fragment hashtable the CFG extracted from the treebank (if not
	 * already present), with their log-frequencies.
	 * @throws Exception
	 */
	public static void addCFGfragments() throws Exception {		
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
			if (fragmentTableLogFreq.containsKey(ruleFragment)) continue;
			double logFreq = Math.log(e.getValue()[0]);
			fragmentTableLogFreq.put(ruleFragment, new double[]{logFreq});
			kept++;
		}
		Parameters.reportLineFlush("Added " + kept + " CFG fragments");
		
	}
	
	/**
	 * Fill the hashtable rootTableLogFreq with the log-frequencies of the root labels
	 * of the fragments.
	 */
	public static void getRootFreq() {
		rootTableLogFreq = new Hashtable<Label, double[]>();
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogFreq.entrySet()) {
			Label rootLabel = e.getKey().label;
			double logFreq = e.getValue()[0];
			Utility.increaseInTableDoubleLogArray(rootTableLogFreq, rootLabel, logFreq);
			// add logFreq to the value of rootLable in the table (taking care of sum of logs)
		}
		Parameters.reportLineFlush("Built root freq. table: " + rootTableLogFreq.size() + " entries.");
	}
		
	public static int endCycle = 10;
	public static double deltaLogLikelihoodThreshold = 1E-5;
	
	
	public static void runEM() {
		int cycle = 0;
		double previousLogLikelihood = Double.NEGATIVE_INFINITY;
		File startFile = new File(workingDir + "kernelsMUB_CFG_freq_EM_cycle_" + cycle + ".txt");
		printFragmentFreq(startFile);
		Parameters.reportLineFlush("Written starting frequencies to file: " + startFile);
		do {
			Hashtable<TSNodeLabel, double[]> newFragmentTableLogFreq = new Hashtable<TSNodeLabel, double[]>();			
			double currentLogLikelihood = 0;
			printProgress = new PrintProgress("Iterating Training Corpus:");			
			int index = -1;
			for(TSNodeLabelIndex t : trainingCorpus) { // for every tree structure in the treebank
				index++;
				printProgress.next();
				double logInsideProb = updateNewFragmentTableFreq(t, newFragmentTableLogFreq);				
				currentLogLikelihood += logInsideProb;
				//if (PrintProgress.currentIndex()==10) break;
			}
			printProgress.end();
			double deltaLogLikelihood = currentLogLikelihood - previousLogLikelihood;
			Parameters.reportLineFlush("EM cyle " + (++cycle) + ". Log-Likelihood: " + currentLogLikelihood +
					" Delta: " + deltaLogLikelihood) ;
			if (deltaLogLikelihood<=deltaLogLikelihoodThreshold) break;
			previousLogLikelihood = currentLogLikelihood;
			fragmentTableLogFreq = newFragmentTableLogFreq;
			getRootFreq();
			File outputFile = new File(workingDir + "kernelsMUB_CFG_freq_EM_cycle_" + cycle + ".txt");
			printFragmentFreq(outputFile);
			Parameters.reportLineFlush("Written new frequencies to file: " + outputFile);
			if (cycle==endCycle) break;
		} while(true);		
	}

	/**
	 * Establish the new frequencies of the fragments contained in the current tree structure t
	 * that maximize the inside probability of the whole structures according to the
	 * current model parameters (current fragment frequencies fragmentTableLogFreq). The inside
	 * log-probability of the structure is returned.
	 * @param t an input tree strucutre
	 * @param newFragmentTableFreq the new frequencies extracted
	 * @return
	 */
	private static double updateNewFragmentTableFreq(
			TSNodeLabelIndex t, Hashtable<TSNodeLabel, double[]> newFragmentTableFreq) {
		
		NodeSetCollectorSimple setCollector = new NodeSetCollectorSimple(); 
		// setCollector is a set of bitsets, each representing a certain fragment contained in the
		// current tree. The bitset {0} represents the CFG-rule having the root of the tree as LHS,
		// the bitset {0,1} represents the fragment including the starting CFG-rule connected to the CFG-rule
		// of the first daughter node of the root of the strucutre. The union of all the bitsets in setCollector
		// should contain all the node-indexes of the current tree, exept for the terminals. 
		HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable = new HashMap<BitSet, TSNodeLabel_Double>();
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogFreq.entrySet()) {			
			getCFGSetCoveringFragment(t, e.getKey(), e.getValue()[0], setCollector, bitSetFreqTable);
		}
		TSNodeLabelStructure tStructure = new TSNodeLabelStructure(t);
		// build the IO Chart for the current structure
		IOChart pc = new IOChart(setCollector, tStructure, bitSetFreqTable);
		pc.buildChart();
		// update the log-frequencies statistics according to the new fragments frequency counts
		pc.updateNewFragmentLogFreq(newFragmentTableFreq);
		// return the inside log-probability of the current structure
		return pc.getInsideLogProb();
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
	private static void getCFGSetCoveringFragment(TSNodeLabelIndex t, TSNodeLabel fragment, 
			double fragmentFreq, NodeSetCollector setCollector, 
			HashMap<BitSet,TSNodeLabel_Double> bitSetFreqLogTable) {
		if (t.isLexical) return;		
		if (t.sameLabel(fragment)) {
			BitSet set = new BitSet();
			if (getCFGSetCoveringFragmentNonRecursive(t, fragment, set) && !set.isEmpty()) {
				setCollector.add(set);
				bitSetFreqLogTable.put(set, new TSNodeLabel_Double(fragment, fragmentFreq));
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
	private static boolean getCFGSetCoveringFragmentNonRecursive(TSNodeLabelIndex t, 
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
	
	static class TSNodeLabel_Double {
		TSNodeLabel fragment;
		double logFreq;
		
		public TSNodeLabel_Double(TSNodeLabel fragment, double logFreq) {
			this.fragment = fragment;
			this.logFreq = logFreq;
		}
	}
	
	static class InitFragmentDerivations {
		
		TSNodeLabel intialFragment;
		ArrayList<Integer> subSites;
		double initFragmentInsideLogProb;
		
		public InitFragmentDerivations(TSNodeLabel intialFragment, 
				ArrayList<Integer> subSites, double initFragmentInsideLogProb) {
			
			this.intialFragment = intialFragment;
			this.subSites = subSites;
			this.initFragmentInsideLogProb = initFragmentInsideLogProb;
		}
	}
		
	static class IOSubNode {		
		
		ArrayList<InitFragmentDerivations> partialDerivations;
		double insideLogProb;
		double outsideLogProb;
		
		public IOSubNode() {
			partialDerivations = new ArrayList<InitFragmentDerivations>();	
			insideLogProb = Double.NEGATIVE_INFINITY;
			outsideLogProb = Double.NEGATIVE_INFINITY;
		}
		
		public void addDerivation(TSNodeLabel intialFragment, 
				ArrayList<Integer> subSites, double initFragmInsideLogProb) {
			partialDerivations.add(new InitFragmentDerivations(intialFragment, subSites, initFragmInsideLogProb));
			insideLogProb = Utility.logSum(insideLogProb, initFragmInsideLogProb);
			// sum the prob. of the partial derivation to the inside prob.
		}
		
		public void addOutisdeProb(double outsideLogProbToAdd) {
			outsideLogProb = Utility.logSum(outsideLogProb, outsideLogProbToAdd);
			// sum the contribution prob. of a partial derivation to the outside prob.
		}
				
	}
	
	static class IOChart {
		
		NodeSetCollectorSimple setCollector;
		TSNodeLabelStructure t;
		int totalNodes;
		IOSubNode[] IOSubNodesChart;
		// the chart of internal node of the current tree each keeping track of inside and outside prob.
		NodeSetCollectorStandard[] nodesCollector;
		// nodesCollector[i] contains all the fragments in the current tree 't' rooted on node-index i.
		HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable;
		
		public IOChart(NodeSetCollectorSimple setCollector, TSNodeLabelStructure t, 
				HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable) {

			this.setCollector = setCollector;
			this.t = t; 
			totalNodes = t.length;
			IOSubNodesChart = new IOSubNode[totalNodes];
			nodesCollector = new NodeSetCollectorStandard[totalNodes];
			this.bitSetFreqTable = bitSetFreqTable;
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
			buildInsideProb();
			buildOutsideProb();
		}
		
		public double getInsideLogProb() {
			return IOSubNodesChart[0].insideLogProb;
		}
		
		/**
		 * Calculate and store the inside probability of the internal
		 * nodes of the current structure. Start with higher node-indexes,
		 * to insure a bottom-up iteration of the nodes.
		 */
		private void buildInsideProb() {
			for(int i=totalNodes-1; i>=0; i--) {
				if (t.structure[i].isLexical) continue;
				buildInsideProb(i);
			}
		}
		
		/**
		 * Calculate and store the inside probability of node-index 'index'.
		 * @param index
		 */
		private void buildInsideProb(int index) {			
			NodeSetCollectorStandard setCollector = nodesCollector[index]; 
			TSNodeLabelIndex root = t.structure[index];
			double rootLogFreq = rootTableLogFreq.get(root.label)[0];
			
			IOSubNode IOSubNodeIndex = new IOSubNode();
			for(BitSet initialSubTree : setCollector.bitSetArray) {	// for every fragment rooted in 'index'			
				ArrayList<Integer> subSitesIndexes = new ArrayList<Integer>();
				collectSubSites(root, initialSubTree, subSitesIndexes); // get the substitution sites of the current fragment
				double initialSubTreeInsideLogProb = 0d;
				for(int subSiteIndex : subSitesIndexes) { // for every substitution site
					double subSiteInsideLogProb = IOSubNodesChart[subSiteIndex].insideLogProb; // get the inside log-prob
					initialSubTreeInsideLogProb += subSiteInsideLogProb; // multiply the probabilities
				}
				TSNodeLabel_Double treeDouble = bitSetFreqTable.get(initialSubTree); 
				double initialSubTreeFreq = treeDouble.logFreq;
				TSNodeLabel initialFragment = treeDouble.fragment;
				initialSubTreeInsideLogProb += initialSubTreeFreq - rootLogFreq; // multiply by the relative freq.
				IOSubNodeIndex.addDerivation(initialFragment, subSitesIndexes, initialSubTreeInsideLogProb);		
				// add the prob of the current partial-derivation starting with the current initial fragment 
			}
			IOSubNodesChart[index] = IOSubNodeIndex;
		}

		private void collectSubSites(TSNodeLabelIndex root, BitSet initialSubTree, 
				ArrayList<Integer> subSitesIndexes) {		
			for(TSNodeLabel d : root.daughters) {
				if (d.isLexical) return;			
				TSNodeLabelIndex di = (TSNodeLabelIndex)d;			
				int index = di.index;
				if (!initialSubTree.get(index)) subSitesIndexes.add(index);
				else collectSubSites(di, initialSubTree, subSitesIndexes);
			}
		}
		
		/**
		 * Calculate and store the outside probability of the internal
		 * nodes of the current structure. Start with lower node-indexes,
		 * to insure a top-down iteration of the nodes.
		 */
		private void buildOutsideProb() {
			IOSubNodesChart[0].outsideLogProb = 0d; // outside probability of the root equals one
			for(int i=0; i<totalNodes; i++) {
				if (t.structure[i].isLexical) continue;
				buildOutsideProb(i);
			}
		}		

		/**
		 * Update the outside probability of all the substitution sites of all the fragments
		 * rooted in the given 'index'. 
		 * @param index
		 */
		private void buildOutsideProb(int index) {
			IOSubNode IOSubNodeIndex = IOSubNodesChart[index];
			double ousideLogProb = IOSubNodeIndex.outsideLogProb;			
			for(InitFragmentDerivations ifd : IOSubNodeIndex.partialDerivations) { // for every partial-derivation
				double initialFragmInsideLogProb = ifd.initFragmentInsideLogProb; // get the starting fragment inside log-prob										
				for(int subSite : ifd.subSites) { // for every substitution site
					double subSiteInsideLogProb = IOSubNodesChart[subSite].insideLogProb; // inside prob of the partial-derivation
					double outsideLogProbToAdd = ousideLogProb + initialFragmInsideLogProb - subSiteInsideLogProb; 
					// contribution of the outside prob. of the current partial-derivation on the current sub-site.
					IOSubNode subSiteDerivation = IOSubNodesChart[subSite];
					subSiteDerivation.addOutisdeProb(outsideLogProbToAdd);		
				}				
			}
		}
		
		/**
		 * Get the new frequencies of the fragments present in the current tree, and update the table
		 * given as input.
		 * @param newFragmentTableFreq
		 */
		public void updateNewFragmentLogFreq(Hashtable<TSNodeLabel,double[]> newFragmentTableFreq) {
			double insideLogProbTOP = IOSubNodesChart[0].insideLogProb;
			for(int i=0; i<totalNodes; i++) { // for every node in the tree
				if (t.structure[i].isLexical) continue; // non terminal
				IOSubNode IOSubNodeIndex = IOSubNodesChart[i];
				double ousideLogProb = IOSubNodeIndex.outsideLogProb;
				for(InitFragmentDerivations ifd : IOSubNodeIndex.partialDerivations) { // for every partial-derivation
					TSNodeLabel initialFragment = ifd.intialFragment;
					double initialFragmInsideLogProb = ifd.initFragmentInsideLogProb;
					double newFreqToAdd = ousideLogProb + initialFragmInsideLogProb - insideLogProbTOP;
					// newFreqToAdd is the probability of generating structure 't' passing through 'initialFragment'
					Utility.increaseInTableDoubleLogArray(newFragmentTableFreq, initialFragment, newFreqToAdd);
				}
			}
		}		
	}
	
	public static String workingDir;
	
	public static void main1(String[] args) throws Exception {
		workingDir = new String(Parameters.resultsPath + "TSG/DOP_IO_SMALL/");
		System.out.println("Working Dir: " + workingDir);
		
		String fragmentFileDir = Parameters.resultsPath + 
			"TSG/TSGkernels/Wsj/KenelFragments/SemTagOff_Top/all/correctCount/"; //correctCount
		File fragmentFile = new File(fragmentFileDir + "fragments_MUB_freq_all_correctCount.txt"); //fragments_MUB_freq_all_correctCount.txt
		
		File corpusFile = new File(Wsj.WsjOriginalCleanedTop + "wsj-02-21.mrg");		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(corpusFile);		
		TSNodeLabel.removeSemanticTagsInTreebank(treebank);				
		
		minFreqFragment = 5;
		endCycle = 50;
		deltaLogLikelihoodThreshold = 0; //1E-5;
		
		readTreeBank(treebank);
		readFragmentsFile(fragmentFile);
		addCFGfragments();
		getRootFreq();
		runEM();		
	}
	
	public static void main(String[] args) throws Exception {
		workingDir = new File(args[0]) + "/";
		File fragmentFile = ArgumentReader.readFileOption(args[1]);
		File corpusFile = ArgumentReader.readFileOption(args[2]);
		endCycle = ArgumentReader.readIntOption(args[3]);
		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(corpusFile);
		
		File workingDirFile = new File(workingDir); 
		if (workingDirFile.exists()) {
			workingDirFile = new File(workingDir + FileUtil.dateTimeString() + "/");
			
		}
		workingDir = workingDirFile.toString() + "/";
		workingDirFile.mkdir();
		
		Parameters.reportLine("Working Dir: " + workingDir);
		Parameters.reportLine("Fragment File: " + fragmentFile);
		Parameters.reportLine("Corpus File: " + corpusFile);
		Parameters.reportLineFlush("Corpus size: " + treebank.size());						
		
		Parameters.openLogFile(new File(workingDir + "log.txt"));
		minFreqFragment = 1;
		
		deltaLogLikelihoodThreshold = 0; //1E-5;		
				
		readTreeBank(treebank);
		readFragmentsFile(fragmentFile);
		addCFGfragments();
		getRootFreq();
		runEM();		
	}

	
}
