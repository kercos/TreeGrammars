package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;

import kernels.NodeSetCollector;
import kernels.NodeSetCollectorMUB;
import backoff.BackoffModel_DivHistory;
import backoff.BackoffModel_Eisner;
import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.Box;
import tesniere.Conversion;
import tesniere.EvalTDS;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class DOP_SD_reranker {
		

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
	
	public static ArrayList<TSNodeLabel> fragmentBag;
	
	public static void readFragmentsFile(File fragmentFile) throws Exception {
		fragmentBag = new ArrayList<TSNodeLabel>();
		Scanner scan = FileUtil.getScanner(fragmentFile);
		int countFragments = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			countFragments++;
			String fragmentString = line.split("\t")[0];
			fragmentString = fragmentString.replaceAll("\\\\", "");
			TSNodeLabel fragment= new TSNodeLabel(fragmentString, false);
			fragmentBag.add(fragment);
		}
		System.out.println("Read " + countFragments + " fragments");
		scan.close();
	}
	
	public static void addCFGfragments(File trainingCorpus) throws Exception {
		ArrayList<TSNodeLabel> corpus = TSNodeLabel.getTreebank(trainingCorpus);
		HashSet<String> ruleSet = new HashSet<String>(); 
		for(TSNodeLabel t : corpus) {
			t.addTop();
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();
				ruleSet.add(rule);
			}
		}	
		for(String rule : ruleSet) {
			TSNodeLabel ruleFragment = new TSNodeLabel("( " + rule + ")", false);
			fragmentBag.add(ruleFragment);			
		}
		System.out.println("Read " + ruleSet.size() + " CFG fragments");
	}
	
	private static int getMinDerivationSize(TSNodeLabelIndex t) {
		NodeSetCollectorMUB setCollector = new NodeSetCollectorMUB();
		for(TSNodeLabel fragment : fragmentBag) {			
			getCFGSetCoveringFragment(t, fragment, setCollector);
		}
		BitSet union = setCollector.uniteSubGraphs();
		ArrayList<TSNodeLabel> internalNodes = t.collectPhrasalNodes();
		if (union.cardinality()<internalNodes.size()) return Integer.MAX_VALUE;
		for(TSNodeLabel iN : internalNodes) {
			TSNodeLabelIndex iNI = (TSNodeLabelIndex)iN;
			if (!union.get(iNI.index)) {
				return Integer.MAX_VALUE;
			}
		}
		int lexNonCovered = 0;
		ArrayList<TSNodeLabel> lexNodes = t.collectLexicalItems();
		for(TSNodeLabel lN : lexNodes) {
			TSNodeLabelIndex iNPI = (TSNodeLabelIndex)lN.parent;
			if (!union.get(iNPI.index)) lexNonCovered++;
		}
		int minCover = getMinCover(setCollector, union);
		return minCover + lexNonCovered;
	}
	
	private static void getCFGSetCoveringFragment(TSNodeLabelIndex t, TSNodeLabel fragment, 
			NodeSetCollector setCollector) {		
		if (t.isLexical) return;		
		if (t.sameLabel(fragment)) {
			BitSet set = new BitSet();
			getCFGSetCoveringFragmentNonRecursive(t, fragment, set);
			if (!set.isEmpty()) setCollector.add(set);
		}		
		for(TSNodeLabel d : t.daughters) {
			TSNodeLabelIndex di = (TSNodeLabelIndex)d;
			getCFGSetCoveringFragment(di, fragment, setCollector);
		}		
	}
	
	private static void getCFGSetCoveringFragmentNonRecursive(TSNodeLabelIndex t, TSNodeLabel fragment, BitSet set) {
		if  (t.isLexical || fragment.isTerminal()) return;
		if (!t.sameDaughtersLabel(fragment)) return;
		int prole = t.prole();
		for(int i=0; i<prole; i++) {
			TSNodeLabel thisDaughter = t.daughters[i];
			TSNodeLabel otherDaughter = fragment.daughters[i];
			//if (!thisDaughter.sameLabel(otherDaughter)) return;
			TSNodeLabelIndex thisDaughterIndex = (TSNodeLabelIndex) thisDaughter; 
			getCFGSetCoveringFragmentNonRecursive(thisDaughterIndex, otherDaughter, set);
		}
		set.set(t.index);		
		return;					
	}

	private static int getMinCover(NodeSetCollectorMUB setCollector, BitSet union) {
		int result = 0;
		int unionCardinality = union.cardinality();
		BitSet currentCover = new BitSet();
		int nextElement = union.nextSetBit(0);;
		while(nextElement!=-1) {			
			if (!currentCover.get(nextElement)) {
				BitSet uniqueSetContainingElement = setCollector.getUniqueBitSetContainingElement(nextElement);
				if (uniqueSetContainingElement!=null) {
					currentCover.or(uniqueSetContainingElement);
					setCollector.removeSet(uniqueSetContainingElement);
					result++;
				}
			}
			nextElement = union.nextSetBit(nextElement+1);
		}
		while(currentCover.cardinality() != unionCardinality) {
			BitSet nextSet = setCollector.getSetWithMaxUncoveredElements(currentCover);
			currentCover.or(nextSet);
			setCollector.removeSet(nextSet);
			result++;
		}		 
		return result;
	}

	public static void rerank(int nBest) throws Exception {
		
		String fragmentFileDir = Parameters.resultsPath + 
			"TSG/TSGkernels/Wsj/KenelFragments/SemTagOff_Top/all/correctCount/";
		File fragmentFile = new File(fragmentFileDir + "fragments_MUB_freq_all_correctCount.txt");
		readFragmentsFile(fragmentFile);
		
		File trainingCorpus = new File(Wsj.WsjOriginalCleanedSemTagsOff + "wsj-02-21.mrg");
		addCFGfragments(trainingCorpus);
		
		String baseDir = Parameters.resultsPath + "TSG/DOP_SD_Reranker/";
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
			int minDerivSize = getMinDerivationSize(bestReranked);
			boolean reranked = false;			
			while(iter.hasNext()) {				
				TSNodeLabelIndex t = iter.next();
				int derSize = getMinDerivationSize(t);
				if (derSize<minDerivSize) {
					minDerivSize = derSize;
					bestReranked = t;
					reranked = true;
				}
			}			
			if (reranked) totalActivelyReranked++;
			if (minDerivSize == Integer.MAX_VALUE) {
				//System.err.println((i+1) + ": no coverage, choosing 1-best.");
				totalNonCovered++;
			}						
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

	public static void main(String[] args) throws Exception {
		//int[] nBest = new int[]{1,5,10,100,500,1000};
		int[] nBest = new int[]{5};
		for(int n : nBest) {
			rerank(n);
		}		
	}
	
	
}
