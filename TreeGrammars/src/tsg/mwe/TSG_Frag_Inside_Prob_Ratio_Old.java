package tsg.mwe;

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
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;
import tsg.corpora.Wsj;
import util.ArgumentReader;
import util.PrintProgress;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class TSG_Frag_Inside_Prob_Ratio_Old {

	public static Hashtable<TSNodeLabel, double[]> fragmentTableLogFreq;
	public static Hashtable<Label, double[]> rootTableLogFreq;

	public static ArrayList<TSNodeLabelIndex> treebankIndex;
	
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
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			countFragments++;
			String[] fragmentFreq = line.split("\t");
			String fragmentString = fragmentFreq[0];			
			int freq = Integer.parseInt(fragmentFreq[1]);
			double logFreq = Math.log(freq);
			TSNodeLabel fragment= TSNodeLabel.newTSNodeLabelStd(fragmentString);
			fragmentTableLogFreq.put(fragment, new double[]{logFreq, -1});
		}
		Parameters.reportLine("Read " + countFragments + " fragments");
		scan.close();
	}
	
	/**
	 * Read the treebank. Every structure is now represented as a TSNodeLabelIndex,
	 * a recursive structure of nodes where every node has a unique index in a depth-first
	 * manner (the root has index 0).  
	 * @param treebankFile
	 * @throws Exception
	 */
	private static void readTreeBank(File treebankFile) throws Exception {
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		treebankIndex = new ArrayList<TSNodeLabelIndex>();		
		for(TSNodeLabel t : treebank) {
			treebankIndex.add(new TSNodeLabelIndex(t));
		}		
	}
	
	public static void printFragmentLogInsideProbRatio(File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<TSNodeLabel,double[]> e : fragmentTableLogFreq.entrySet()) {
			String fragmentString = e.getKey().toStringStandard();
			double[] logProbInside = e.getValue();
			double logRatio = logProbInside[0]-logProbInside[1];
			pw.println(fragmentString + "\t" + logRatio);
		}
		pw.close();
	}
	
	/**
	 * Add in the fragment hashtable the CFG extracted from the treebank (if not
	 * already present), with their log-frequencies.
	 * @throws Exception
	 */
	public static void addCFGfragmentsFromTreebank() throws Exception {		
		Hashtable<String, int[]> ruleTable = new Hashtable<String, int[]>(); 
		for(TSNodeLabel t : treebankIndex) {
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();
				Utility.increaseInTableInt(ruleTable, rule);
			}
		}
		Parameters.reportLineFlush("Read " + ruleTable.size() + " CFG rules from treebank");
		int kept = 0;
		for(Entry<String, int[]> e : ruleTable.entrySet()) {
			TSNodeLabel ruleFragment = new TSNodeLabel("( " + e.getKey() + ")", false);
			if (fragmentTableLogFreq.containsKey(ruleFragment)) continue;
			double logFreq = Math.log(e.getValue()[0]);
			fragmentTableLogFreq.put(ruleFragment, new double[]{logFreq, -1});
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
	
	
	public static void calculateInsideProbs() {
		printProgress = new PrintProgress("Computing Fragments Inside Probs:");
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogFreq.entrySet()) {
			printProgress.next();
			TSNodeLabelIndex frag = new TSNodeLabelIndex(e.getKey());
			e.getValue()[1] = getInsideLogProb(frag);
		}
		printProgress.end();
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
	private static double getInsideLogProb(TSNodeLabelIndex frag) {
		
		NodeSetCollectorSimple setCollector = new NodeSetCollectorSimple(); 
		// setCollector is a set of bitsets, each representing a certain fragment contained in the
		// current tree. The bitset {0} represents the CFG-rule having the root of the tree as LHS,
		// the bitset {0,1} represents the fragment including the starting CFG-rule connected to the CFG-rule
		// of the first daughter node of the root of the structUre. The union of all the bitsets in setCollector
		// should contain all the node-indexes of the current tree, except for the terminals. 
		HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable = new HashMap<BitSet, TSNodeLabel_Double>();
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogFreq.entrySet()) {			
			getCFGSetCoveringFragment(frag, e.getKey(), e.getValue()[0], setCollector, bitSetFreqTable);
		}
		TSNodeLabelStructure fragStructure = new TSNodeLabelStructure(frag);
		// build the IO Chart for the current structure
		InsideChart pc = new InsideChart(setCollector, fragStructure, bitSetFreqTable);
		pc.buildChart();
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
		if (t.isTerminal()) return;		
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
		if  (t.isTerminal() || fragment.isTerminal()) return true;
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
		
	static class InsideSubNode {		
		
		ArrayList<InitFragmentDerivations> partialDerivations;
		double insideLogProb;
		
		public InsideSubNode() {
			partialDerivations = new ArrayList<InitFragmentDerivations>();	
			insideLogProb = Double.NEGATIVE_INFINITY;
		}
		
		public void addDerivation(TSNodeLabel intialFragment, 
				ArrayList<Integer> subSites, double initFragmInsideLogProb) {
			partialDerivations.add(new InitFragmentDerivations(intialFragment, subSites, initFragmInsideLogProb));
			insideLogProb = Utility.logSum(insideLogProb, initFragmInsideLogProb);
			// sum the prob. of the partial derivation to the inside prob.
		}
						
	}
	
	static class InsideChart {
		
		NodeSetCollectorSimple setCollector;
		TSNodeLabelStructure t;
		int totalNodes;
		InsideSubNode[] IOSubNodesChart;
		// the chart of internal node of the current tree each keeping track of inside and outside prob.
		NodeSetCollectorStandard[] nodesCollector;
		// nodesCollector[i] contains all the fragments in the current tree 't' rooted on node-index i.
		HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable;
		
		public InsideChart(NodeSetCollectorSimple setCollector, TSNodeLabelStructure t, 
				HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable) {

			this.setCollector = setCollector;
			this.t = t; 
			totalNodes = t.length();
			IOSubNodesChart = new InsideSubNode[totalNodes];
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
				if (t.structure[i].isTerminal()) continue;
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
			
			InsideSubNode IOSubNodeIndex = new InsideSubNode();
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
				if (d.isTerminal()) return;			
				TSNodeLabelIndex di = (TSNodeLabelIndex)d;			
				int index = di.index;
				if (!initialSubTree.get(index)) subSitesIndexes.add(index);
				else collectSubSites(di, initialSubTree, subSitesIndexes);
			}
		}
					
	}
	
	public static void main(String[] args) throws Exception {
		File treebankFile = ArgumentReader.readFileOption(args[0]);
		File fragmentFile = ArgumentReader.readFileOption(args[1]);		
		File outputFile = ArgumentReader.readFileOption(args[2]);		
		
		Parameters.reportLine("Treebank File: " + treebankFile);
		Parameters.reportLine("Fragment File: " + fragmentFile);		
		Parameters.reportLineFlush("Output File: " + outputFile);						
							
		readTreeBank(treebankFile);
		readFragmentsFile(fragmentFile);
		addCFGfragmentsFromTreebank();
		getRootFreq();
		calculateInsideProbs();	
		printFragmentLogInsideProbRatio(outputFile);
	}

	
}
