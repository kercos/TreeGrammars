package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class DOP_EM_BigDecimal {

	public static Hashtable<TSNodeLabel, BigDecimal[]> fragmentTableFreq;
	public static Hashtable<Label, BigDecimal[]> rootTableFreq;

	public static ArrayList<TSNodeLabelIndex> trainingCorpus;
	
	public static void readFragmentsFile(File fragmentFile) throws Exception {
		fragmentTableFreq = new Hashtable<TSNodeLabel, BigDecimal[]>();
		Scanner scan = FileUtil.getScanner(fragmentFile);
		int countFragments = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			countFragments++;
			String[] fragmentFreq = line.split("\t");
			String fragmentString = fragmentFreq[0];
			BigDecimal freq = new BigDecimal(fragmentFreq[1]);
			TSNodeLabel fragment= new TSNodeLabel(fragmentString, false);
			fragmentTableFreq.put(fragment, new BigDecimal[]{freq});
		}
		System.out.println("Read " + countFragments + " fragments");
		scan.close();
	}
	
	private static void readTreeBank(ArrayList<TSNodeLabel> treebank) throws Exception {
		trainingCorpus = new ArrayList<TSNodeLabelIndex>();
		for(TSNodeLabel t : treebank) {
			trainingCorpus.add(new TSNodeLabelIndex(t));
		}		
	}
	
	public static void printFragmentFreq(File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<TSNodeLabel,BigDecimal[]> e : fragmentTableFreq.entrySet()) {
			String fragmentString = e.getKey().toString(false, true);
			double freq = e.getValue()[0].doubleValue();
			pw.println(fragmentString + "\t" + freq) ;
		}
		pw.close();
	}
	
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
		System.out.println("Read " + ruleTable.size() + " CFG fragments");
		int kept = 0;
		for(Entry<String, int[]> e : ruleTable.entrySet()) {
			TSNodeLabel ruleFragment = new TSNodeLabel("( " + e.getKey() + ")", false);
			if (fragmentTableFreq.containsKey(ruleFragment)) continue;
			BigDecimal freq = new BigDecimal(e.getValue()[0]);
			fragmentTableFreq.put(ruleFragment, new BigDecimal[]{freq});
			kept++;
		}
		System.out.println("Added " + kept + " CFG fragments");
		
	}
	
	public static void getRootFreq() {
		rootTableFreq = new Hashtable<Label, BigDecimal[]>();
		for(Entry<TSNodeLabel, BigDecimal[]> e : fragmentTableFreq.entrySet()) {
			Label rootLabel = e.getKey().label;
			BigDecimal freq = e.getValue()[0];
			Utility.increaseInTableBigDecimalArray(rootTableFreq, rootLabel, freq);
		}
		System.out.println("Built root freq. table: " + rootTableFreq.size() + " entries.");
	}
	
	/*public static void readDevelpCorpus(ArrayList<TSNodeLabelIndex> developCorpus) {
		develoCorpusCleaned = new ArrayList<TSNodeLabelIndex>();
		for(TSNodeLabelIndex t : developCorpus) {
			if (isCovered(t)) develoCorpusCleaned.add(t);
		}
		System.out.println("Read developed corpus. Kept covered sentences: " + 
				develoCorpusCleaned.size() + "/" + developCorpus.size());
	}*/

	/*private static boolean isCovered(TSNodeLabelIndex t) {
		BitSet union = new BitSet();
		for(TSNodeLabel fragment : fragmentTableFreq.keySet()) {			
			getCFGSetCoveringFragment(t, fragment, union);
		}		
		ArrayList<TSNodeLabel> internalNodes = t.collectInternalNodes();
		if (union.cardinality()<internalNodes.size()) return false;
		for(TSNodeLabel iN : internalNodes) {
			TSNodeLabelIndex iNI = (TSNodeLabelIndex)iN;
			if (!union.get(iNI.index)) return false;
		}
		return true;
	}*/
	
	/*private static void getCFGSetCoveringFragment(TSNodeLabelIndex t, TSNodeLabel fragment, BitSet union) {		
		if (t.isLexical) return;		
		if (t.sameLabel(fragment)) {
			BitSet set = new BitSet();
			if (getCFGSetCoveringFragmentNonRecursive(t, fragment, set) && !set.isEmpty()) {
				union.or(set);
			}
		}		
		for(TSNodeLabel d : t.daughters) {
			TSNodeLabelIndex di = (TSNodeLabelIndex)d;
			getCFGSetCoveringFragment(di, fragment, union);
		}		
	}*/
	
	public static int endCycle = 10;
	
	public static void runEM() {
		int cycle = 0;
		BigDecimal previousLikelihood = BigDecimal.ZERO;		
		do {
			Hashtable<TSNodeLabel, BigDecimal[]> newFragmentTableFreq = new Hashtable<TSNodeLabel, BigDecimal[]>();			
			BigDecimal currentLikelihood = BigDecimal.ONE;
			PrintProgressStatic.start("Iterating Training Corpus:");
			for(TSNodeLabelIndex t : trainingCorpus) {
				PrintProgressStatic.next();
				//TSNodeLabelIndex t = trainingCorpus.get(11726);
				BigDecimal prob = updateNewFragmentTableFreq(t, newFragmentTableFreq);
				if (prob.compareTo(BigDecimal.ZERO)==0) {
					System.err.println("Zero prob. + " + PrintProgressStatic.currentIndex());
					return;
				}
				currentLikelihood = currentLikelihood.multiply(prob);
			}
			PrintProgressStatic.end();
			System.out.println("EM cyle " + (cycle++) + ". Likelihood: " + currentLikelihood);
			if (currentLikelihood.compareTo(previousLikelihood)<0) break;
			previousLikelihood = currentLikelihood;
			fragmentTableFreq = newFragmentTableFreq;
			getRootFreq();
			printFragmentFreq(new File(workingDir + "kernelsMUB_CFG_freq_EM_cycle" + cycle + ".txt"));
			if (cycle==endCycle) break;
		} while(true);		
	}

	private static BigDecimal updateNewFragmentTableFreq(
			TSNodeLabelIndex t, Hashtable<TSNodeLabel, BigDecimal[]> newFragmentTableFreq) {
		
		NodeSetCollectorSimple setCollector = new NodeSetCollectorSimple();
		HashMap<BitSet, TSNodeLabel_BigDecimal> bitSetFreqTable = new HashMap<BitSet, TSNodeLabel_BigDecimal>();
		for(Entry<TSNodeLabel, BigDecimal[]> e : fragmentTableFreq.entrySet()) {			
			getCFGSetCoveringFragment(t, e.getKey(), e.getValue()[0], setCollector, bitSetFreqTable);
		}
		//BitSet union = setCollector.uniteSubGraphs();				
		//ArrayList<TSNodeLabel> nonLexicalNodes = t.collectNonLexicalNodes();
		//BitSet preLexNonCovered = new BitSet();
		/*int lexNonCovered = 0;
		for(TSNodeLabel nlN : nonLexicalNodes) {
			TSNodeLabelIndex nlNI = (TSNodeLabelIndex)nlN;
			int index = nlNI.index;
			if (nlNI.isPreLexical()) {
				if (!union.get(index)) {
					lexNonCovered++;
					//preLexNonCovered.set(index);
				}
				continue;
			}			
			if (!union.get(index)) {
				return -1;			
			}
		}*/
		TSNodeLabelStructure tStructure = new TSNodeLabelStructure(t);
		ProbChart pc = new ProbChart(setCollector, tStructure, bitSetFreqTable, newFragmentTableFreq);
		BigDecimal prob = pc.getProb();
		pc.extractNewFragmentFrequencies();
		return prob;
	}
	
	private static void getCFGSetCoveringFragment(TSNodeLabelIndex t, TSNodeLabel fragment, 
			BigDecimal fragmentFreq, NodeSetCollector setCollector, HashMap<BitSet,TSNodeLabel_BigDecimal> bitSetFreqTable) {		
		if (t.isLexical) return;		
		if (t.sameLabel(fragment)) {
			BitSet set = new BitSet();
			if (getCFGSetCoveringFragmentNonRecursive(t, fragment, set) && !set.isEmpty()) {
				setCollector.add(set);
				bitSetFreqTable.put(set, new TSNodeLabel_BigDecimal(fragment, fragmentFreq));
			}
		}		
		for(TSNodeLabel d : t.daughters) {
			TSNodeLabelIndex di = (TSNodeLabelIndex)d;
			getCFGSetCoveringFragment(di, fragment, fragmentFreq, setCollector, bitSetFreqTable);
		}		
	}
	
	private static boolean getCFGSetCoveringFragmentNonRecursive(TSNodeLabelIndex t, TSNodeLabel fragment, BitSet set) {
		if  (t.isLexical || fragment.isTerminal()) return true;
		if (!t.sameDaughtersLabel(fragment)) return false;
		int prole = t.prole();
		for(int i=0; i<prole; i++) {
			TSNodeLabel thisDaughter = t.daughters[i];
			TSNodeLabel otherDaughter = fragment.daughters[i];
			//if (!thisDaughter.sameLabel(otherDaughter)) return;
			TSNodeLabelIndex thisDaughterIndex = (TSNodeLabelIndex) thisDaughter; 
			if (!getCFGSetCoveringFragmentNonRecursive(thisDaughterIndex, otherDaughter, set)) return false;
		}
		set.set(t.index);		
		return true;					
	}
	
	static class TSNodeLabel_BigDecimal {
		TSNodeLabel tree;
		BigDecimal d;
		
		public TSNodeLabel_BigDecimal(TSNodeLabel tree, BigDecimal d) {
			this.tree = tree;
			this.d = d;
		}
	}
	
	static class PartialDerivation {
		
		TSNodeLabel intialFragment;
		ArrayList<Integer> subSites;
		BigDecimal partialDerivProb;
		
		public PartialDerivation(TSNodeLabel intialFragment, 
				ArrayList<Integer> subSites, BigDecimal partialDerivProb) {
			
			this.intialFragment = intialFragment;
			this.subSites = subSites;
			this.partialDerivProb = partialDerivProb;
		}
	}
	
	static class DerivationsNode {		
		
		ArrayList<PartialDerivation> partialDerivations;
		BigDecimal totalProb;
		BigDecimal newProbMass;
		
		public DerivationsNode() {
			partialDerivations = new ArrayList<PartialDerivation>();	
			totalProb = BigDecimal.ZERO;
			newProbMass = BigDecimal.ZERO;
		}
		
		public void addDerivation(TSNodeLabel intialFragment, 
				ArrayList<Integer> subSites, BigDecimal derivationProb) {
			partialDerivations.add(new PartialDerivation(intialFragment, subSites, derivationProb));
			totalProb = totalProb.add(derivationProb);
		}
		
		public void addProbMass(BigDecimal probMass) {
			newProbMass = newProbMass.add(probMass);
		}
				
	}
	
	
	static class ProbChart {
				
		NodeSetCollectorSimple setCollector;
		TSNodeLabelStructure t;
		int totalNodes;
		//BitSet preLexNonCovered;
		DerivationsNode[] derivationsNodes;
		NodeSetCollectorStandard[] nodesCollector;
		HashMap<BitSet, TSNodeLabel_BigDecimal> bitSetFreqTable;
		Hashtable<TSNodeLabel, BigDecimal[]> newFragmentTableFreq;
		
		public ProbChart(NodeSetCollectorSimple setCollector, TSNodeLabelStructure t, 
				HashMap<BitSet, TSNodeLabel_BigDecimal> bitSetFreqTable, 
				Hashtable<TSNodeLabel, BigDecimal[]> newFragmentTableFreq) {

			this.setCollector = setCollector;
			this.t = t;
			//this.preLexNonCovered = preLexNonCovered;
			this.newFragmentTableFreq = newFragmentTableFreq; 
			totalNodes = t.length;
			derivationsNodes = new DerivationsNode[totalNodes];
			nodesCollector = new NodeSetCollectorStandard[totalNodes];
			this.bitSetFreqTable = bitSetFreqTable;
		}
		
		public BigDecimal getProb() {												
			// distribute the subtrees in the parent node index
			for(BitSet bs : setCollector.bitSetSet) {
				int firstIndex = bs.nextSetBit(0);
				if (nodesCollector[firstIndex]==null) {
					nodesCollector[firstIndex] = new NodeSetCollectorStandard();				
				}
				nodesCollector[firstIndex].add(bs);
			}				
			return getProbRecursive(0);
		}
		
		private BigDecimal getProbRecursive(int index) {
			if (derivationsNodes[index]!=null) return derivationsNodes[index].totalProb;
			NodeSetCollectorStandard setCollector = nodesCollector[index]; 
			if (setCollector==null) {
				return derivationsNodes[index].totalProb = BigDecimal.ZERO;
			}
			TSNodeLabelIndex root = t.structure[index];
			BigDecimal rootFreq = rootTableFreq.get(root.label)[0];
			
			DerivationsNode derivation = new DerivationsNode();
			for(BitSet initialSubTree : setCollector.bitSetArray) {				
				ArrayList<Integer> subSitesIndexes = new ArrayList<Integer>();
				collectSubSites(root, initialSubTree, subSitesIndexes);
				BigDecimal partialProb = BigDecimal.ONE;
				for(int subSiteIndex : subSitesIndexes) {
					BigDecimal subSiteProb = getProbRecursive(subSiteIndex);
					if (subSiteProb.compareTo(BigDecimal.ZERO)==0) {
						partialProb = BigDecimal.ZERO;
						break;
					}
					partialProb = partialProb.multiply(subSiteProb);
				}
				if (partialProb.compareTo(BigDecimal.ZERO)==0) {
					continue;
				}
				TSNodeLabel_BigDecimal treeDouble = bitSetFreqTable.get(initialSubTree); 
				BigDecimal initialSubTreeFreq = treeDouble.d;
				if (initialSubTreeFreq.compareTo(BigDecimal.ZERO)==0) {
					System.out.println();
				}
				TSNodeLabel initialFragment = treeDouble.tree;
				partialProb = partialProb.multiply(initialSubTreeFreq.divide(rootFreq, MathContext.DECIMAL128));
				derivation.addDerivation(initialFragment, subSitesIndexes, partialProb);			
			}
			derivationsNodes[index] = derivation;
			if (derivationsNodes[index].totalProb.compareTo(BigDecimal.ZERO)==0) {
				System.out.println();
			}
			return derivationsNodes[index].totalProb;
		}

		private void collectSubSites(TSNodeLabelIndex root, BitSet initialSubTree, 
				ArrayList<Integer> subSitesIndexes) {		
			for(TSNodeLabel d : root.daughters) {
				if (d.isLexical) return;			
				TSNodeLabelIndex di = (TSNodeLabelIndex)d;			
				int index = di.index;
				//if (preLexNonCovered.get(index)) continue;
				if (!initialSubTree.get(index)) subSitesIndexes.add(index);
				else collectSubSites(di, initialSubTree, subSitesIndexes);
			}
		}
		
		public void extractNewFragmentFrequencies() {
			derivationsNodes[0].newProbMass = BigDecimal.ONE;
			extractNewFragmentFrequenciesRecursive(0);
		}

		private void extractNewFragmentFrequenciesRecursive(int index) {
			DerivationsNode derivations = derivationsNodes[index];
			BigDecimal totalMass = derivations.newProbMass;
			BigDecimal derivationsTotProb = derivations.totalProb;			
			TreeSet<Integer> allEncounteredSubSites = new TreeSet<Integer>();
			for(PartialDerivation pd : derivations.partialDerivations) {
				TSNodeLabel initialFragment = pd.intialFragment;
				BigDecimal pdProb = pd.partialDerivProb;
				BigDecimal partialMass = pdProb.divide(derivationsTotProb, MathContext.DECIMAL128).multiply(totalMass);
				Utility.increaseInTableBigDecimalArray(newFragmentTableFreq, initialFragment, partialMass);				
				for(int subSite : pd.subSites) {
					allEncounteredSubSites.add(subSite);			
					DerivationsNode subSiteDerivation = derivationsNodes[subSite];
					subSiteDerivation.addProbMass(partialMass);		
				}				
			}
			for(int subSite : allEncounteredSubSites) {
				extractNewFragmentFrequenciesRecursive(subSite);
			}
		}
	}
	
	public static String workingDir;
	
	public static void main(String[] args) throws Exception {
		workingDir = new String(Parameters.resultsPath + "TSG/DOP_EM/");
		System.out.println("Working Dir: " + workingDir);
		
		String fragmentFileDir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/KenelFragments/SemTagOff_Top/all/";
		File fragmentFile = new File(fragmentFileDir + "fragments_MUB_freq_all.txt");
		//File newFragmetnFreqFile = new File(workingDir + "kernelsMUB_CFG_freq_EM.txt");
		
		File corpusFile = new File(Wsj.WsjOriginalCleanedTop + "wsj-02-21.mrg");		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(corpusFile);		
		TSNodeLabel.removeSemanticTagsInTreebank(treebank);				
		
		endCycle = 10;
		
		readTreeBank(treebank);
		readFragmentsFile(fragmentFile);
		addCFGfragments();
		getRootFreq();
		runEM();
		//printFragmentFreq(newFragmetnFreqFile);
		
	}


	
}
