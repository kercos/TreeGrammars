package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Map.Entry;

import kernels.NodeSetCollector;
import kernels.NodeSetCollectorMUB;
import kernels.NodeSetCollectorSimple;
import kernels.NodeSetCollectorStandard;
import backoff.BackoffModel_DivHistory;
import backoff.BackoffModel_Eisner;
import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.Box;
import tesniere.Conversion;
import tesniere.EvalTDS;
import tsg.DOP_IO_Log.TSNodeLabel_Double;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class DOP1_reranker {
	
	public static ArrayList<TSNodeLabelIndex> nextNBest(int nBest, Scanner s) throws Exception {
		ArrayList<TSNodeLabelIndex> result = new ArrayList<TSNodeLabelIndex>(nBest);
		int count = 0;
		while(s.hasNextLine() && count<nBest) {
			String line = s.nextLine();
			if (line.equals("")) return result;
			TSNodeLabelIndex tree = new TSNodeLabelIndex(line);
			result.add(tree);			
			count++;
		}
		while(s.hasNextLine() && !s.nextLine().equals("")) {};
		return result;
	}
	
	public static HashMap<TSNodeLabel, Double> fragmentTableFreq;
	public static Hashtable<Label, double[]> rootTableFreq;
	
	public static void readFragmentsFile(File fragmentFile) throws Exception {
		System.out.println("Reading fragments from: " + fragmentFile.toString());
		fragmentTableFreq = new HashMap<TSNodeLabel, Double>();		
		Scanner scan = FileUtil.getScanner(fragmentFile);
		int countFragments = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			countFragments++;
			String[] fragmentFreq = line.split("\t");
			String fragmentString = fragmentFreq[0];
			Double freq = Double.parseDouble(fragmentFreq[1]);
			fragmentString = fragmentString.replaceAll("\\\\", "");
			TSNodeLabel fragment= new TSNodeLabel(fragmentString, false);
			fragmentTableFreq.put(fragment, freq);
		}
		System.out.println("Read " + countFragments + " fragments");
		scan.close();
	}
	
	public static void addCFGfragments(File trainingCorpus) throws Exception {
		ArrayList<TSNodeLabel> corpus = TSNodeLabel.getTreebank(trainingCorpus);		
		Hashtable<String, int[]> ruleTable = new Hashtable<String, int[]>(); 
		for(TSNodeLabel t : corpus) {
			t.addTop();
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
			double freq = e.getValue()[0];
			fragmentTableFreq.put(ruleFragment,freq);
			kept++;
		}
		System.out.println("Added " + kept + " CFG fragments");
		
	}
	
	public static void getRootFreq() {
		rootTableFreq = new Hashtable<Label, double[]>();
		for(Entry<TSNodeLabel, Double> e : fragmentTableFreq.entrySet()) {
			Label rootLabel = e.getKey().label;
			double freq = e.getValue();
			Utility.increaseInTableDoubleArray(rootTableFreq, rootLabel, freq);
		}
		System.out.println("Built root freq. table: " + rootTableFreq.size() + " entries.");
	}
	
	public static boolean allowUnknownCFG = true;
	
	private static double getParseTreeProb(TSNodeLabelIndex t) {
		NodeSetCollectorSimple setCollector = new NodeSetCollectorSimple();
		HashMap<BitSet, Double> bitSetFreqTable = new HashMap<BitSet, Double>();
		for(Entry<TSNodeLabel, Double> e : fragmentTableFreq.entrySet()) {			
			getCFGSetCoveringFragment(t, e.getKey(), e.getValue(), setCollector, bitSetFreqTable);
		}
		BitSet union = setCollector.uniteSubGraphs();				
		ArrayList<TSNodeLabel> nonLexicalNodes = t.collectNonLexicalNodes();		
		BitSet preLexNonCovered = new BitSet();
		//int lexNonCovered = 0;
		if (allowUnknownCFG) {
			for(TSNodeLabel nlN : nonLexicalNodes) {
				TSNodeLabelIndex nlNI = (TSNodeLabelIndex)nlN;
				int index = nlNI.index;
				if (!union.get(index)) {
					BitSet set = new BitSet();
					set.set(index);
					setCollector.add(set);
					bitSetFreqTable.put(set, 1d);
				}
			}
		}
		else {
			for(TSNodeLabel nlN : nonLexicalNodes) {
				TSNodeLabelIndex nlNI = (TSNodeLabelIndex)nlN;
				int index = nlNI.index;
				if (nlNI.isPreLexical()) {
					if (!union.get(index)) {
						//lexNonCovered++;
						preLexNonCovered.set(index);
					}				
					continue;
				}						
				if (!union.get(index)) return -1;			
			}
		}
		TSNodeLabelStructure tStructure = new TSNodeLabelStructure(t);
		ProbChart pc = new ProbChart(setCollector, tStructure, preLexNonCovered, bitSetFreqTable);
		return pc.getProb();
	}
	
	private static void getCFGSetCoveringFragment(TSNodeLabelIndex t, TSNodeLabel fragment, 
			double fragmentFreq, NodeSetCollector setCollector, 
			HashMap<BitSet,Double> bitSetFreqLogTable) {		
		if (t.isLexical) return;		
		if (t.sameLabel(fragment)) {
			BitSet set = new BitSet();
			if (getCFGSetCoveringFragmentNonRecursive(t, fragment, set) && !set.isEmpty()) {
				setCollector.add(set);
				bitSetFreqLogTable.put(set, fragmentFreq);
			}
		}		
		for(TSNodeLabel d : t.daughters) {
			TSNodeLabelIndex di = (TSNodeLabelIndex)d;
			getCFGSetCoveringFragment(di, fragment, fragmentFreq, setCollector, bitSetFreqLogTable);
		}		
	}
	
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
	
/*	private static void getCFGSetCoveringFragment(TSNodeLabelIndex t, TSNodeLabel fragment, 
			Integer fragmentFreq, NodeSetCollector setCollector, HashMap<BitSet,Integer> bitSetFreqTable) {		
		if (t.isLexical) return;		
		if (t.sameLabel(fragment)) {
			BitSet set = new BitSet();
			if (getCFGSetCoveringFragmentNonRecursive(t, fragment, set) && !set.isEmpty()) {
				setCollector.add(set);
				bitSetFreqTable.put(set, fragmentFreq);
			}
		}		
		for(TSNodeLabel d : t.daughters) {
			TSNodeLabelIndex di = (TSNodeLabelIndex)d;
			getCFGSetCoveringFragment(di, fragment, fragmentFreq, setCollector, bitSetFreqTable);
		}		
	}
	
	private static boolean getCFGSetCoveringFragmentNonRecursive(TSNodeLabelIndex t, TSNodeLabel fragment, BitSet set) {
		if  (t.isLexical || fragment.isTerminal()) return true;
		if (!t.sameDaughterLabel(fragment)) return false;
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
*/
	
	static class ProbChart {
		
		NodeSetCollectorSimple setCollector;
		TSNodeLabelStructure t;
		int totalNodes;
		BitSet preLexNonCovered;
		double[] probNodes;
		NodeSetCollectorSimple[] nodesCollector;
		HashMap<BitSet, Double> bitSetFreqTable;
		
		public ProbChart(NodeSetCollectorSimple setCollector, TSNodeLabelStructure t, 
				BitSet preLexNonCovered, HashMap<BitSet, Double> bitSetFreqTable) {
			this.setCollector = setCollector;
			this.t = t;
			this.preLexNonCovered = preLexNonCovered;
			totalNodes = t.length;
			probNodes = new double[totalNodes];
			Arrays.fill(probNodes, -1d);
			nodesCollector = new NodeSetCollectorSimple[totalNodes];
			this.bitSetFreqTable = bitSetFreqTable;
		}
		
		public double getProb() {												
			// distribute the subtrees in the parent node index
			for(BitSet bs : setCollector.bitSetSet) {
				int firstIndex = bs.nextSetBit(0);
				if (nodesCollector[firstIndex]==null) {
					nodesCollector[firstIndex] = new NodeSetCollectorSimple();				
				}
				nodesCollector[firstIndex].add(bs);
			}				
			return getProbRecursive(0);
		}
		
		private double getProbRecursive(int index) {
			if (probNodes[index]!=-1) return probNodes[index];
			NodeSetCollectorSimple setCollector = nodesCollector[index]; 
			if (setCollector==null) return probNodes[index] = 0;
			TSNodeLabelIndex root = t.structure[index];
			double rootFreq = rootTableFreq.get(root.label)[0];
			double prob = 0;
			for(BitSet initialSubTree : setCollector.bitSetSet) {				
				ArrayList<Integer> subSitesIndexes = new ArrayList<Integer>();
				collectSubSites(root, initialSubTree, subSitesIndexes);
				double partialProb = 1d;
				for(int subSiteIndex : subSitesIndexes) {
					double subSiteProb = getProbRecursive(subSiteIndex);
					if (subSiteProb==0) {
						partialProb=0;
						break;
					}
					partialProb *= subSiteProb;
				}
				if (partialProb==0) continue;
				double initialSubTreeFreq = bitSetFreqTable.get(initialSubTree);
				partialProb *= (double) initialSubTreeFreq / rootFreq;
				prob += partialProb;
			}
			return probNodes[index] = prob;
		}

		private void collectSubSites(TSNodeLabelIndex root, BitSet initialSubTree, 
				ArrayList<Integer> subSitesIndexes) {		
			for(TSNodeLabel d : root.daughters) {
				if (d.isLexical) return;			
				TSNodeLabelIndex di = (TSNodeLabelIndex)d;			
				int index = di.index;
				if (preLexNonCovered.get(index)) continue;
				if (!initialSubTree.get(index)) subSitesIndexes.add(index);
				else collectSubSites(di, initialSubTree, subSitesIndexes);
			}
		}
	}



	

	public static void rerank(int nBest) throws Exception {
		
		String fragmentFileDir = Parameters.resultsPath + 
			"TSG/TSGkernels/Wsj/KenelFragments/SemTagOff_Top/all/correctCount/";
		File fragmentFile = new File(fragmentFileDir + "fragments_MUB_freq_all_correctCount.txt");
		readFragmentsFile(fragmentFile);
		
		File trainingCorpus = new File(Wsj.WsjOriginalCleanedSemTagsOff + "wsj-02-21.mrg");
		addCFGfragments(trainingCorpus);
		
		getRootFreq();
		
		String baseDir = Parameters.resultsPath + "TSG/DOP1_Reranker/";
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_cleaned.mrg");											 
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		
		File goldFile = new File(baseDir + "wsj-22_gold.mrg");
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);				
		
		File rerankedFile = new File(baseDir + "wsj-22_reranked_" + nBest + "best.mrg");
		File rerankedFileEvalF = new File(baseDir + "wsj-22_reranked_" + nBest + "best.evalF");
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFile);
		
		int size = goldTreebank.size();
		int totalActivelyReranked = 0;
		int totalNonCovered = 0;
		
		System.out.println("Rerankin n = " + nBest);
		PrintProgressStatic.start("Sentence ");
		
		for(int i=0; i<size; i++) {
		
			PrintProgressStatic.next();
			ArrayList<TSNodeLabelIndex> nBestTrees = nextNBest(nBest, nBestScanner);
			Iterator<TSNodeLabelIndex> iter = nBestTrees.iterator();
			TSNodeLabelIndex bestReranked = iter.next();			
			double bestProb = getParseTreeProb(bestReranked);
			
			boolean reranked = false;
			while(iter.hasNext()) {				
				TSNodeLabelIndex t = iter.next();
				double prob = getParseTreeProb(t);
				if (prob>bestProb) {
					bestProb = prob;
					bestReranked = t;
					reranked = true;					
				}
			}			
			
			if (reranked) totalActivelyReranked++;
			if (bestProb == -1) {
				//System.err.println((i+1) + ": no coverage, choosing 1-best.");
				totalNonCovered++;
			}
			//else System.err.println((i+1) + ": covered!");
			pw.println(bestReranked.toString());			
		}		
		
		pw.close();
		PrintProgressStatic.end();
		float[] rerankedFScore = EvalC.staticEvalF(goldFile, rerankedFile, rerankedFileEvalF, true);
		System.out.println("Actively reranked: " + totalActivelyReranked);
		System.out.println("Non covered: " + totalNonCovered);
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(rerankedFScore));
		//new EvalTDS(rerankedFile, goldFile).compareStructures();
	}
	
	public static void rerankEM(int nBest, int cycle) throws Exception {
		
		String baseDir = Parameters.resultsPath + "TSG/DOP_IO/"; //"TSG/DOP_IO_SMALL/";
		File fragmentFile = new File(baseDir + "kernelsMUB_CFG_freq_EM_cycle_" + cycle + ".txt");
		readFragmentsFile(fragmentFile);
		
		getRootFreq();
		
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_cleaned.mrg");											 
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		
		File goldFile = new File(baseDir + "wsj-22_gold.mrg");
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);				
		
		File rerankedFile = new File(baseDir + "wsj-22_reranked_" + nBest + "best_cycle_" + cycle + ".mrg");
		File rerankedFileEvalF = new File(baseDir + "wsj-22_reranked_" + nBest + "best_cycle_" + cycle + ".evalF");
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFile);
		
		int size = goldTreebank.size();
		int totalActivelyReranked = 0;
		int totalNonCovered = 0;
		
		System.out.println("Rerankin n = " + nBest);
		PrintProgressStatic.start("Sentence ");
		
		for(int i=0; i<size; i++) {
		
			PrintProgressStatic.next();
			ArrayList<TSNodeLabelIndex> nBestTrees = nextNBest(nBest, nBestScanner);
			Iterator<TSNodeLabelIndex> iter = nBestTrees.iterator();
			TSNodeLabelIndex bestReranked = iter.next();			
			double bestProb = getParseTreeProb(bestReranked);
			
			boolean reranked = false;
			while(iter.hasNext()) {				
				TSNodeLabelIndex t = iter.next();
				double prob = getParseTreeProb(t);
				if (prob>bestProb) {
					bestProb = prob;
					bestReranked = t;
					reranked = true;					
				}
			}			
			
			if (reranked) totalActivelyReranked++;
			if (bestProb == -1) {
				//System.err.println((i+1) + ": no coverage, choosing 1-best.");
				totalNonCovered++;
			}
			//else System.err.println((i+1) + ": covered!");
			pw.println(bestReranked.toString());			
		}		
		
		pw.close();
		PrintProgressStatic.end();
		float[] rerankedFScore = EvalC.staticEvalF(goldFile, rerankedFile, rerankedFileEvalF, true);
		System.out.println("Actively reranked: " + totalActivelyReranked);
		System.out.println("Non covered: " + totalNonCovered);
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(rerankedFScore));
		//new EvalTDS(rerankedFile, goldFile).compareStructures();
	}

	public static void main1(String[] args) throws Exception {
		//int[] nBest = new int[]{1,5,10,100,500,1000};
		int[] nBest = new int[]{5,10,100};
		for(int n : nBest) {
			rerank(n);
		}		
	}
	
	public static void main(String[] args) throws Exception {
		allowUnknownCFG = true;
		//int[] nBest = new int[]{1,5,10,100,500,1000};		
		//for(int c = 13; c<=30; c++) {
		//	rerankEM(5,c);
		//}		
		//rerankEM(5,1);
		rerankEM(10,1);
		rerankEM(100,1);
	}

}
