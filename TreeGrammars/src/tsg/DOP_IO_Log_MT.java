package tsg;

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
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class DOP_IO_Log_MT extends Thread {

	public static int threads = 1;
	public static int minFreqFragment = 0;
	public static int maxDepthFragment = Integer.MAX_VALUE;
	public static int endCycle = 10;
	public static double deltaLogLikelihoodThreshold = 1E-5;
	public static int printProgressEvery = 100;
	public static boolean normalizeCounts = false;

	
	String workingDir;
	ArrayList<TSNodeLabelIndex> trainingCorpus;
	Hashtable<TSNodeLabel, double[]> fragmentTableLogFreq;
	Hashtable<Label, double[]> rootTableLogFreq;
	PrintProgress printProgress;
	int treebankSize;
	
	
	public DOP_IO_Log_MT ( File corpusFile, 
			File fragmentFile, String workingDir) throws Exception {		
		readTreeBank(corpusFile);
		readFragmentsFile(fragmentFile);
		if (normalizeCounts) {
			normalizeFragCounts();
		}
		this.workingDir = workingDir;		
		addCFGfragments();		
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
	public void readFragmentsFile(File fragmentFile) throws Exception {
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
			TSNodeLabel fragment= new TSNodeLabel(fragmentString, false);
			int depth = fragment.maxDepth();
			if (depth>maxDepthFragment) {
				discarded++;
				continue;				
			}
			double logFreq = Math.log(freq);			
			fragmentTableLogFreq.put(fragment, new double[]{logFreq});
		}
		Parameters.reportLine("Read " + countFragments + " fragments");
		Parameters.reportLineFlush("Discarded " + discarded + " (freq < " + minFreqFragment + 
				" || depth >" + maxDepthFragment + ")");
		scan.close();
	}
	
	private void normalizeFragCounts() {
		Parameters.reportLine("Normalizing frequency counts");
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogFreq.entrySet()) {
			double[] count = e.getValue();
			count[0] = 0;
		}
		
	}
	
	public void printFragmentFreq(File outputFile) {
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
	public void addCFGfragments() throws Exception {		
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
	public void getRootFreq() {
		rootTableLogFreq = new Hashtable<Label, double[]>();
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogFreq.entrySet()) {
			Label rootLabel = e.getKey().label;
			double logFreq = e.getValue()[0];
			Utility.increaseInTableDoubleLogArray(rootTableLogFreq, rootLabel, logFreq);
			// add logFreq to the value of rootLable in the table (taking care of sum of logs)
		}
		Parameters.reportLineFlush("Built root freq. table: " + rootTableLogFreq.size() + " entries.");
	}
		
	
	
	public void run() {				
		try {
			runEM();
		} catch (InterruptedException e) {			
			Parameters.reportError(e.getMessage());
		}
	}

	public void runEM() throws InterruptedException {		
		int sentencesPerThreads = treebankSize / threads;
		int remainingSentences = treebankSize % threads;
		if (remainingSentences!=0) sentencesPerThreads++;
		
		getRootFreq();
		
		int cycle = 0;
		double previousLogLikelihood = Double.NEGATIVE_INFINITY;
		File startFile = new File(workingDir + "kernelsMUB_CFG_freq_EM_cycle_" + cycle + ".txt");
		printFragmentFreq(startFile);
		Parameters.reportLineFlush("Written starting frequencies to file: " + startFile);
		do {
			printProgress = new PrintProgress("Iterating Training Corpus:", printProgressEvery, 0);
			EMThreadRunner[] bitParThreadArray = new EMThreadRunner[threads];
			for(int i=0; i<threads; i++) {
				int startIndex = sentencesPerThreads*i;
				EMThreadRunner t = null;;
				if (i<threads-1) {
					int endIndex = sentencesPerThreads*(i+1);
					ArrayList<TSNodeLabelIndex> subtreebank = 
						new ArrayList<TSNodeLabelIndex>(trainingCorpus.subList(startIndex, endIndex));
					t = new EMThreadRunner(subtreebank);
					t.start();
					
				}
				else {
					ArrayList<TSNodeLabelIndex> subtreebank = 
						new ArrayList<TSNodeLabelIndex>(trainingCorpus.subList(startIndex, treebankSize));
					t = new EMThreadRunner(subtreebank);
					t.run();										
				}
				bitParThreadArray[i] = t;
			}
			
			for(EMThreadRunner t : bitParThreadArray) { 
				t.join();				
			}
			
			double currentLogLikelihood = 0;
			fragmentTableLogFreq.clear();
			
			for(EMThreadRunner t : bitParThreadArray) {
				currentLogLikelihood += t.currentLogLikelihood;
				addAllLog(t.fragmentTableLogFreqThread);
			}
			
			getRootFreq();
			
			printProgress.end();
			double deltaLogLikelihood = currentLogLikelihood - previousLogLikelihood;
			Parameters.reportLineFlush("EM cyle " + (++cycle) + ". Log-Likelihood: " + currentLogLikelihood +
					" Delta: " + deltaLogLikelihood) ;
			if (deltaLogLikelihood<=deltaLogLikelihoodThreshold) break;
			previousLogLikelihood = currentLogLikelihood;			
			File outputFile = new File(workingDir + "kernelsMUB_CFG_freq_EM_cycle_" + cycle + ".txt");
			printFragmentFreq(outputFile);
			Parameters.reportLineFlush("Written new frequencies to file: " + outputFile);
			if (cycle==endCycle) break;
			
		} while(true);		
	}

	private void addAllLog(Hashtable<TSNodeLabel, double[]> fragmentTableLogFreqThread) {
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogFreqThread.entrySet()) {
			Utility.increaseInTableDoubleLogArray(fragmentTableLogFreq, e.getKey(), e.getValue()[0]);
		}
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
	private double updateNewFragmentTableFreq(
			TSNodeLabelIndex t, Hashtable<TSNodeLabel, double[]> newFragmentTableFreq) {
		
		NodeSetCollectorSimple setCollector = new NodeSetCollectorSimple(); 
		// setCollector is a set of bitsets, each representing a certain fragment contained in the
		// current tree. The bitset {0} represents the CFG-rule having the root of the tree as LHS,
		// the bitset {0,1} represents the fragment including the starting CFG-rule connected to the CFG-rule
		// of the first daughter node of the root of the structure. The union of all the bitsets in setCollector
		// should contain all the node-indexes of the current tree, except for the terminals. 
		HashMap<BitSet, TSNodeLabelFreqDouble> bitSetFreqTable = new HashMap<BitSet, TSNodeLabelFreqDouble>();
		for(Entry<TSNodeLabel, double[]> e : fragmentTableLogFreq.entrySet()) {			
			getCFGSetCoveringFragment(t, e.getKey(), e.getValue()[0], setCollector, bitSetFreqTable);
		}
		TSNodeLabelStructure tStructure = new TSNodeLabelStructure(t);
		// build the IO Chart for the current structure
		IOChart pc = new IOChart(setCollector, tStructure, bitSetFreqTable);
		//pc.checkCoverage();
		pc.buildChart();
		//pc.checkProbConsistency();
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
	
	public synchronized void printProgressNext() {
		printProgress.next(printProgressEvery);
	}
	
	protected class EMThreadRunner extends Thread {

		ArrayList<TSNodeLabelIndex> subTreebank;
		Hashtable<TSNodeLabel, double[]> fragmentTableLogFreqThread;
		double currentLogLikelihood;
		
		public EMThreadRunner(ArrayList<TSNodeLabelIndex> subTreebank) {
			this.subTreebank = subTreebank;
			fragmentTableLogFreqThread = new Hashtable<TSNodeLabel, double[]>();
			currentLogLikelihood = 0;
		}
		
		public void run() {					
			int i = 0;
			for(TSNodeLabelIndex t : subTreebank) {
				if (++i==printProgressEvery) {
					printProgressNext();
					i=0;
				}
				double logInsideProb = updateNewFragmentTableFreq(t, fragmentTableLogFreqThread);				
				currentLogLikelihood += logInsideProb;
			}
		}
		
				
	}
	
	class IOChart {
		
		NodeSetCollectorSimple setCollector;
		TSNodeLabelStructure t;
		int totalNodes;
		IOSubNode[] IOSubNodesChart;
		// the chart of internal node of the current tree each keeping track of inside and outside prob.
		NodeSetCollectorStandard[] nodesCollector;
		// nodesCollector[i] contains all the fragments in the current tree 't' rooted on node-index i.
		HashMap<BitSet, TSNodeLabelFreqDouble> bitSetFreqTable;
		
		public IOChart(NodeSetCollectorSimple setCollector, TSNodeLabelStructure t, 
				HashMap<BitSet, TSNodeLabelFreqDouble> bitSetFreqTable) {

			this.setCollector = setCollector;
			this.t = t; 
			totalNodes = t.length;
			IOSubNodesChart = new IOSubNode[totalNodes];
			nodesCollector = new NodeSetCollectorStandard[totalNodes];
			this.bitSetFreqTable = bitSetFreqTable;
		}
		
		public void checkCoverage() {
			int covered = setCollector.uniteSubGraphs().cardinality();
			int toCover = t.structure[0].countNonLexicalNodes();
			if (covered!=toCover) {
				System.err.println("Tree not covered!");
			}			
		}
		
		public void checkProbConsistency() {
			double logProbTree = getInsideLogProb();
			for(int i=1; i<totalNodes; i++) {
				if (!t.structure[i].isLexical) continue;
				double logInsideCell = IOSubNodesChart[i].insideLogProb;
				double logOutsideCell = IOSubNodesChart[i].outsideLogProb;
				double logInPlusOut = logInsideCell + logOutsideCell;
				double absDiff = Math.abs(logInPlusOut-logProbTree); 
				if (absDiff>1E-5) {
					System.err.println("IO inconsistency: diff = " + absDiff);
				}
			}			
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
				if (t.structure[i].isLexical) {
					//IOSubNode IOSubNodeIndex = new IOSubNode();
					//IOSubNodeIndex.insideLogProb = 0; // inside of a word is 1
					//IOSubNodesChart[i] = IOSubNodeIndex;
					continue;
				}
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
				//ArrayList<Integer> subSitesLexNodesIndexes = new ArrayList<Integer>();
				//collectSubSitesLexNodes(root, initialSubTree, 
				//		subSitesLexNodesIndexes); // get the substitution sites and lex nodes of the current fragment
				double initialSubTreeInsideLogProb = 0d;
				for(int subSiteIndex : subSitesIndexes) { // for every substitution site
					double subSiteInsideLogProb = IOSubNodesChart[subSiteIndex].insideLogProb; // get the inside log-prob
					initialSubTreeInsideLogProb += subSiteInsideLogProb; // multiply the probabilities
				}
				TSNodeLabelFreqDouble treeDouble = bitSetFreqTable.get(initialSubTree); 
				double initialSubTreeFreq = treeDouble.freq;
				TSNodeLabel initialFragment = treeDouble.tree;
				initialSubTreeInsideLogProb += initialSubTreeFreq - rootLogFreq; // multiply by the relative freq.
				IOSubNodeIndex.addDerivation(initialFragment, subSitesIndexes, initialSubTreeInsideLogProb);		
				// add the prob of the current partial-derivation starting with the current initial fragment 
			}
			IOSubNodesChart[index] = IOSubNodeIndex;
		}

		private void collectSubSitesLexNodes(TSNodeLabelIndex root, BitSet initialSubTree, 
				ArrayList<Integer> subSitesLexNodesIndexes) {		
			for(TSNodeLabel d : root.daughters) {
				TSNodeLabelIndex di = (TSNodeLabelIndex)d;			
				int index = di.index;
				if (d.isLexical) {
					subSitesLexNodesIndexes.add(index);
					return;			
				}				
				if (!initialSubTree.get(index)) subSitesLexNodesIndexes.add(index);
				else collectSubSitesLexNodes(di, initialSubTree, subSitesLexNodesIndexes);
			}
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
				//double insideLogProb = IOSubNodeIndex.insideLogProb;					
				for(InitFragmentDerivations ifd : IOSubNodeIndex.partialDerivations) { // for every partial-derivation
					TSNodeLabel initialFragment = ifd.intialFragment;
					double initialFragmInsideLogProb = ifd.initFragmentInsideLogProb;
					//double newFreqToAdd = ousideLogProb + initialFragmInsideLogProb;
					//double newFreqToAdd = initialFragmInsideLogProb - insideLogProb;
					double newFreqToAdd = ousideLogProb + initialFragmInsideLogProb - insideLogProbTOP;
						// newFreqToAdd is the probability of generating structure 't' passing through 'initialFragment'
					Utility.increaseInTableDoubleLogArray(newFragmentTableFreq, initialFragment, newFreqToAdd);
				}
			}
		}		
	}
	
	
	
	
	class IOSubNode {		
		
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




	class InitFragmentDerivations {
		
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


	public static void main1(String[] args) throws Exception {		
		
		minFreqFragment = 100;
		maxDepthFragment = Integer.MAX_VALUE;				
		deltaLogLikelihoodThreshold = 0; //1E-5;
		
		String baseDir = "IOdebug/";
		String workingDir = baseDir + "IO_minFreq100";
		File corpusFile = new File(baseDir + "trainingTreebank_UK_MB_elianti.mrg");
		File fragmentFile = new File(baseDir + "fragments_approxFreq_andCfgRules.txt");				
		threads = 1;
		endCycle = 10;
		
		File workingDirFile = new File(workingDir); 
		if (workingDirFile.exists()) {
			workingDirFile = new File(workingDir + FileUtil.dateTimeString() + "/");			
		}
		workingDir = workingDirFile + "/";
		workingDirFile.mkdir();
		
		Parameters.openLogFile(new File(workingDir + "log.txt"));		
		
		Parameters.reportLine("Working Dir: " + workingDirFile);
		Parameters.reportLine("Fragment File: " + fragmentFile);
		Parameters.reportLineFlush("Corpus File: " + corpusFile);										
		Parameters.reportLine("threads: " + threads);
		Parameters.reportLine("minFreqFragment: " + minFreqFragment);
		Parameters.reportLine("maxDepthFragment: " + maxDepthFragment);
		Parameters.reportLine("endCycle: " + endCycle);
		Parameters.reportLineFlush("deltaLogLikelihoodThreshold: " + deltaLogLikelihoodThreshold);
					
		new DOP_IO_Log_MT (corpusFile, fragmentFile, workingDir).run();		
		
	}

	public static void main(String[] args) throws Exception {
		
		minFreqFragment = 1;
		maxDepthFragment = Integer.MAX_VALUE;				
		deltaLogLikelihoodThreshold = 0; //1E-5;
		
		File corpusFile = ArgumentReader.readFileOption(args[0]);
		File fragmentFile = ArgumentReader.readFileOption(args[1]);
		String workingDir = args[2];		
		threads = ArgumentReader.readIntOption(args[3]);
		minFreqFragment = ArgumentReader.readIntOption(args[4]);
		maxDepthFragment  = ArgumentReader.readIntOption(args[5]);
		normalizeCounts = ArgumentReader.readBooleanOption(args[6]);
		endCycle = ArgumentReader.readIntOption(args[7]);		
		
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
		Parameters.reportLine("minFreqFragment: " + minFreqFragment);
		Parameters.reportLine("maxDepthFragment: " + maxDepthFragment);
		Parameters.reportLine("endCycle: " + endCycle);
		Parameters.reportLineFlush("deltaLogLikelihoodThreshold: " + deltaLogLikelihoodThreshold);
					
		new DOP_IO_Log_MT (corpusFile, fragmentFile, workingDir).run();		
	}

	
}
