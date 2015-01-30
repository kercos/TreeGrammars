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

public class DOP_EM_Log {

	public static Hashtable<TSNodeLabel, double[]> fragmentTableFreq;
	public static Hashtable<Label, double[]> rootTableFreq;

	public static ArrayList<TSNodeLabelIndex> trainingCorpus;
	
	public static void readFragmentsFile(File fragmentFile) throws Exception {
		fragmentTableFreq = new Hashtable<TSNodeLabel, double[]>();
		Scanner scan = FileUtil.getScanner(fragmentFile);
		int countFragments = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			countFragments++;
			String[] fragmentFreq = line.split("\t");
			String fragmentString = fragmentFreq[0];
			double freq = Math.log(Integer.parseInt(fragmentFreq[1]));
			fragmentString = fragmentString.replaceAll("\\\\", "");
			TSNodeLabel fragment= new TSNodeLabel(fragmentString, false);
			fragmentTableFreq.put(fragment, new double[]{freq});
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
		for(Entry<TSNodeLabel,double[]> e : fragmentTableFreq.entrySet()) {
			String fragmentString = e.getKey().toString(false, true);
			double freq = Math.exp(e.getValue()[0]);
			if (freq==0) continue;
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
			double freq = Math.log(e.getValue()[0]);
			fragmentTableFreq.put(ruleFragment, new double[]{freq});
			kept++;
		}
		System.out.println("Added " + kept + " CFG fragments");
		
	}
	
	public static void getRootFreq() {
		rootTableFreq = new Hashtable<Label, double[]>();
		for(Entry<TSNodeLabel, double[]> e : fragmentTableFreq.entrySet()) {
			Label rootLabel = e.getKey().label;
			double freq = e.getValue()[0];
			Utility.increaseInTableDoubleLogArray(rootTableFreq, rootLabel, freq);
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
		double previousLogLikelihood = -Double.MAX_VALUE;		
		PrintProgress printProgress = new PrintProgress("Iterating Training Corpus:", 10, 0);;
		printFragmentFreq(new File(workingDir + "kernelsMUB_CFG_freq_EM_cycle_" + cycle + ".txt"));
		do {
			Hashtable<TSNodeLabel, double[]> newFragmentTableFreq = new Hashtable<TSNodeLabel, double[]>();			
			double currentLogLikelihood = 0d;			
			for(TSNodeLabelIndex t : trainingCorpus) {
				printProgress.next();
				//TSNodeLabelIndex t = trainingCorpus.get(11726);
				double logProb = updateNewFragmentTableFreq(t, newFragmentTableFreq);
				currentLogLikelihood += logProb;
				//if (PrintProgress.currentIndex()==10) break;
			}
			printProgress.end();
			double detaLogLikelihood = currentLogLikelihood - previousLogLikelihood;
			System.out.println("EM cyle " + (++cycle) + ". Log-Likelihood: " + currentLogLikelihood +
					"\tDelta Log-Likelihood: " + detaLogLikelihood);
			if (detaLogLikelihood<0) break;
			previousLogLikelihood = currentLogLikelihood;
			fragmentTableFreq = newFragmentTableFreq;
			getRootFreq();
			printFragmentFreq(new File(workingDir + "kernelsMUB_CFG_freq_EM_cycle_" + cycle + ".txt"));
			if (cycle==endCycle) break;			
		} while(true);		
	}

	private static double updateNewFragmentTableFreq(
			TSNodeLabelIndex t, Hashtable<TSNodeLabel, double[]> newFragmentTableFreq) {
		
		NodeSetCollectorSimple setCollector = new NodeSetCollectorSimple();
		HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable = new HashMap<BitSet, TSNodeLabel_Double>();
		for(Entry<TSNodeLabel, double[]> e : fragmentTableFreq.entrySet()) {			
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
		double prob = pc.getProb();
		pc.extractNewFragmentFrequencies();
		return prob;
	}
	
	private static void getCFGSetCoveringFragment(TSNodeLabelIndex t, TSNodeLabel fragment, 
			double fragmentFreq, NodeSetCollector setCollector, HashMap<BitSet,TSNodeLabel_Double> bitSetFreqTable) {		
		if (t.isLexical) return;		
		if (t.sameLabel(fragment)) {
			BitSet set = new BitSet();
			if (getCFGSetCoveringFragmentNonRecursive(t, fragment, set) && !set.isEmpty()) {
				setCollector.add(set);
				bitSetFreqTable.put(set, new TSNodeLabel_Double(fragment, fragmentFreq));
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
	
	static class TSNodeLabel_Double {
		TSNodeLabel tree;
		double d;
		
		public TSNodeLabel_Double(TSNodeLabel tree, double d) {
			this.tree = tree;
			this.d = d;
		}
	}
	
	static class PartialDerivation {
		
		TSNodeLabel intialFragment;
		ArrayList<Integer> subSites;
		double partialDerivProb;
		
		public PartialDerivation(TSNodeLabel intialFragment, 
				ArrayList<Integer> subSites, double partialDerivProb) {
			
			this.intialFragment = intialFragment;
			this.subSites = subSites;
			this.partialDerivProb = partialDerivProb;
		}
	}
	
	static class DerivationsNode {		
		
		ArrayList<PartialDerivation> partialDerivations;
		double totalProb;
		double newProbMass;
		
		public DerivationsNode() {
			partialDerivations = new ArrayList<PartialDerivation>();	
			totalProb = 0;
			newProbMass = 0;
		}
		
		public void addDerivation(TSNodeLabel intialFragment, 
				ArrayList<Integer> subSites, double derivationProb) {
			partialDerivations.add(new PartialDerivation(intialFragment, subSites, derivationProb));
			totalProb = Utility.logSum(totalProb, derivationProb);
		}
		
		public void addProbMass(double probMass) {
			newProbMass = Utility.logSum(newProbMass, probMass);
		}
				
	}
	
	static class ProbChart {
		
		NodeSetCollectorSimple setCollector;
		TSNodeLabelStructure t;
		int totalNodes;
		DerivationsNode[] derivationsNodes;
		NodeSetCollectorStandard[] nodesCollector;
		HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable;
		Hashtable<TSNodeLabel, double[]> newFragmentTableFreq;
		
		public ProbChart(NodeSetCollectorSimple setCollector, TSNodeLabelStructure t, 
				HashMap<BitSet, TSNodeLabel_Double> bitSetFreqTable, 
				Hashtable<TSNodeLabel, double[]> newFragmentTableFreq) {

			this.setCollector = setCollector;
			this.t = t;
			this.newFragmentTableFreq = newFragmentTableFreq; 
			totalNodes = t.length;
			derivationsNodes = new DerivationsNode[totalNodes];
			nodesCollector = new NodeSetCollectorStandard[totalNodes];
			this.bitSetFreqTable = bitSetFreqTable;
		}
		
		public double getProb() {												
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
		
		private double getProbRecursive(int index) {
			if (derivationsNodes[index]!=null) return derivationsNodes[index].totalProb;
			NodeSetCollectorStandard setCollector = nodesCollector[index]; 
			if (setCollector==null) {
				return derivationsNodes[index].totalProb = 0;
			}
			TSNodeLabelIndex root = t.structure[index];
			double rootLogFreq = rootTableFreq.get(root.label)[0];
			
			DerivationsNode derivation = new DerivationsNode();
			for(BitSet initialSubTree : setCollector.bitSetArray) {				
				ArrayList<Integer> subSitesIndexes = new ArrayList<Integer>();
				collectSubSites(root, initialSubTree, subSitesIndexes);
				double partialLogProb = 0d;
				for(int subSiteIndex : subSitesIndexes) {
					double subSiteProb = getProbRecursive(subSiteIndex);
					/*if (subSiteProb==0) {
						partialProb=0;
						break;
					}*/
					partialLogProb += subSiteProb;
				}
				/*if (partialProb==0) {
					continue;
				}*/
				TSNodeLabel_Double treeDouble = bitSetFreqTable.get(initialSubTree); 
				double initialSubTreeFreq = treeDouble.d;
				/*if (initialSubTreeFreq==0) {
					System.out.println();
				}*/
				TSNodeLabel initialFragment = treeDouble.tree;
				partialLogProb += (initialSubTreeFreq - rootLogFreq);
				derivation.addDerivation(initialFragment, subSitesIndexes, partialLogProb);			
			}
			derivationsNodes[index] = derivation;
			/*if (derivationsNodes[index].totalProb==0) {
				System.out.println();
			}*/
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
			derivationsNodes[0].newProbMass = 0d;
			extractNewFragmentFrequenciesRecursive(0);
		}

		private void extractNewFragmentFrequenciesRecursive(int index) {
			DerivationsNode derivations = derivationsNodes[index];
			double totalMass = derivations.newProbMass;
			double derivationsTotProb = derivations.totalProb;			
			TreeSet<Integer> allEncounteredSubSites = new TreeSet<Integer>();
			for(PartialDerivation pd : derivations.partialDerivations) {
				TSNodeLabel initialFragment = pd.intialFragment;
				double pdProb = pd.partialDerivProb;
				double partialMass = pdProb - derivationsTotProb + totalMass;
				Utility.increaseInTableDoubleArray(newFragmentTableFreq, initialFragment, partialMass);				
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
	
	public static void main1(String[] args) throws Exception {
		workingDir = new String(Parameters.resultsPath + "TSG/DOP_EM/");
		System.out.println("Working Dir: " + workingDir);
		
		String fragmentFileDir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/KenelFragments/SemTagOff_Top/all/";
		File fragmentFile = new File(fragmentFileDir + "fragments_MUB_freq_all.txt");
		
		File corpusFile = new File(Wsj.WsjOriginalCleanedTop + "wsj-02-21.mrg");		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(corpusFile);		
		TSNodeLabel.removeSemanticTagsInTreebank(treebank);				
		
		endCycle = 1000;
		
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
				
		readTreeBank(treebank);
		readFragmentsFile(fragmentFile);
		addCFGfragments();
		getRootFreq();
		runEM();		
	}



	
}
