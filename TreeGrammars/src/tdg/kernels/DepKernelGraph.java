package tdg.kernels;
import java.io.*;
import java.util.*;

import kernels.NodeSetCollector;
import kernels.NodeSetCollectorArgFreq;
import kernels.NodeSetCollectorArgFreqTerm;
import kernels.NodeSetCollectorFreq;
import kernels.NodeSetCollectorMUB;
import kernels.NodeSetCollectorMUBFreq;
import kernels.NodeSetCollectorStandard;
import kernels.NodeSetCollectorUnion;
import util.*;
import util.file.FileUtil;
import settings.*;
import tdg.TDNode;
import tdg.TDNodePair;
import tdg.corpora.*;
import tdg.reranking.Reranker;

public abstract class DepKernelGraph {
	
	public static boolean printTable = true;
	public static boolean countCommonWordsInK = false;	
	
	public static PrintWriter out;
	public static Set<String> posMatchLex = new HashSet<String>(Arrays.asList(
			new String[]{"IN", "VBD", "VBZ", "VB", "VBP", "VBN", "VBG"}));
	
	TDNode currentNode;
	TDNode[] thisStructure, thisStructureWithoutEmpty;
	int ignoreIndex;	
	int currentLength, currentLengthWithoutEmpty;
	ArrayList<TDNode> treebank;
	int maxBranching; // branching factor
	
	boolean adjacency;
	boolean countEmptyDaughters;
	
	int SLP_type = 0; 
						//0: same postag, 
						//1: same lex, 
						//2: same lexpostag, 
						//3: mix
	
	int collectionType; 
						//0: NodeSetCollectorStandard, 
						//1: NodeSetCollectorFreq, 
						//2: NodeSetCollectorMUB
						//3: NodeSetCollectorMUBFreq
						//4: NodeSetCollectorUnion
						//5: NodeSetCollectorArgFreq
						//6: NodeSetCollectorArgFreqTerm

	
	public DepKernelGraph(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, int maxBranching, int collectionType, int SLP_type,
			boolean countEmptyDaughters, boolean adjacency) {
		this.currentNode = currentNode;
		this.treebank = treebank;
		this.ignoreIndex = ignoreIndex;
		this.maxBranching = maxBranching;			
		this.collectionType = collectionType;
		this.SLP_type = SLP_type;
		this.countEmptyDaughters = countEmptyDaughters;
		this.adjacency = adjacency;
		thisStructure = currentNode.getStructureArray();
		thisStructureWithoutEmpty = currentNode.getStructureArrayWithoutEmpty();
		currentLength = thisStructure.length;
		currentLengthWithoutEmpty = thisStructureWithoutEmpty.length;
	}
	
	public static DepKernelGraph getDepKernelGraph(int DKG_type, 
			TDNode currentNode, ArrayList<TDNode> treebank, int ignoreIndex, int maxBranching, 
			int collectionType, int SLP_type, boolean countEmptyDaughters,
			boolean adjacency) {
		//0: order
		//1: noOrder
		//2: LR1L
		//3: orderNoFreq
		//4: ArgFrameOrder
		DepKernelGraph DKGO = null;
		switch (DKG_type) {
		case 0: //order
			DKGO = new DepKernelGraphOrder(currentNode, treebank, ignoreIndex, 
					maxBranching, collectionType, SLP_type, countEmptyDaughters,
					adjacency);
			break;
		case 1: //noOrder
			DKGO = new DepKernelGraphNoOrder(currentNode, treebank, ignoreIndex, 
					maxBranching, collectionType, SLP_type, countEmptyDaughters,
					adjacency);
			break;
		case 2: //LR
			DKGO = new DepKernelGraphLR1L(currentNode, treebank, ignoreIndex, 
					maxBranching, collectionType, SLP_type, countEmptyDaughters,
					adjacency);
			break;		
		case 3: //orderNoFreq
			DKGO = new DepKernelGraphOrderNoFreq(currentNode, treebank, ignoreIndex, 
					maxBranching, collectionType, SLP_type, countEmptyDaughters,
					adjacency);
			break;
		case 4: //ArgFrameOrder
			DKGO = new DepKernelGraphArgFrameOrder(currentNode, treebank, ignoreIndex, 
					maxBranching, collectionType, SLP_type, countEmptyDaughters,
					adjacency);
			break;
		}
		return DKGO;
	}
	
	public static int[] computeKernelScore(int scoreType, int DKG_type, TDNode currentNode, 
			ArrayList<TDNode> treebank, int ignoreIndex, int maxBranching, 
			int collectionType, int SLP_type, boolean countEmptyDaughters,
			boolean adjacency) {
		
		DepKernelGraph DKGO = getDepKernelGraph(DKG_type, 
				currentNode, treebank, ignoreIndex, maxBranching, 
				collectionType, SLP_type, countEmptyDaughters, adjacency);
		
		
		NodeSetCollector[] wordsKernels = DKGO.computePeakGraphKernels();	
		
		//scoreType
		//				0: maxCardinalitySharedSubGraph
		//				1: maxSpanning
		//				2: maxSpanningFreq
		//				3: totalSpanningFreq
		//				4: sumMaxCardinalitiesSharedSubGraphs
		//				5: dependentHeadScores
		//				6: headDependentsScores
		//				7: minDerivationLength
		
		switch(scoreType) {
			case 0: return new int[]{NodeSetCollector.maxCardinalitySharedSubGraph(wordsKernels)};
			case 1: return new int[]{NodeSetCollector.maxSpanning(wordsKernels)};
			case 2: return new int[]{NodeSetCollector.maxSpanningFreq(wordsKernels)};
			case 3: return new int[]{NodeSetCollector.totalSpanningFreq(wordsKernels)};			
			case 4: return new int[]{
					NodeSetCollector.sumMaxCardinalitiesSharedSubGraphs(wordsKernels)};
			case 5: return 
					NodeSetCollector.dependentHeadScores(wordsKernels, DKGO.thisStructureWithoutEmpty);
			case 6: return
					NodeSetCollector.headDependentsScores(wordsKernels, DKGO.thisStructureWithoutEmpty);
			case 7:
					return new int[]{NodeSetCollector.minDerivationLength(wordsKernels, 
							DKGO.currentNode, DKGO.thisStructureWithoutEmpty,
							countEmptyDaughters)};
		}
		System.err.println("Wrong score type");
		return null;
		
	}
	
	public float[] computeKernelProb(Hashtable<String,Integer> headStatTable) {
		NodeSetCollector[] wordsKernels = this.computePeakGraphKernels();
		
		return NodeSetCollector.relFreqProduct(wordsKernels, this.thisStructureWithoutEmpty, 
				headStatTable, SLP_type);
	}
	
	public static float[] computeKernelProb(int DKG_type, TDNode currentNode, 
			ArrayList<TDNode> treebank, int ignoreIndex, int maxBranching, 
			int collectionType, int SLP_type, boolean countEmptyDaughters,
			boolean adjacency, Hashtable<String,Integer> headStatTable) {
		
		DepKernelGraph DKGO = getDepKernelGraph(DKG_type, 
				currentNode, treebank, ignoreIndex, maxBranching, 
				collectionType, SLP_type, countEmptyDaughters, adjacency);
		
		return DKGO.computeKernelProb(headStatTable);		
	}
	
	public double computeKernelProbArgInsertion(Hashtable<String,Double> substitutionCondFreqTable) {
		
		NodeSetCollector[] wordsKernels = this.computePeakGraphKernels();
		NodeSetCollector initialKernel = wordsKernels[this.currentNode.index];
		return initialKernel.getProbArgFrame(wordsKernels, thisStructure,
				currentNode, substitutionCondFreqTable, SLP_type);
	}
	
	public static double computeKernelProbArgInsertion(int DKG_type, TDNode currentNode, 
			ArrayList<TDNode> treebank, int ignoreIndex, int maxBranching, 
			int collectionType, int SLP_type, boolean countEmptyDaughters, 
			boolean adjacency, Hashtable<String, Double> substitutionCondFreqTable) {
		
		DepKernelGraph DKGO = getDepKernelGraph(DKG_type, 
				currentNode, treebank, ignoreIndex, maxBranching, 
				collectionType, SLP_type, countEmptyDaughters, adjacency);
		
		return DKGO.computeKernelProbArgInsertion(substitutionCondFreqTable);
	}
	
	public void printAllScores(NodeSetCollector[] wordsKernels, TDNode tree, TDNode[] structure) {
		//out.println("Max Cardinality Shared SubGraph: " + 
		//		NodeSetCollector.maxCardinalitySharedSubGraph(wordsKernels));
		//out.println("Max spanning: " + NodeSetCollector.maxSpanning(wordsKernels));
		//out.println("Max spanning freq: " + NodeSetCollector.maxSpanningFreq(wordsKernels));
		//out.println("Total spanning freq: " + NodeSetCollector.totalSpanningFreq(wordsKernels));
		//out.println("Sum max cardinalities shared subGraphs: " + 
		//		NodeSetCollector.sumMaxCardinalitiesSharedSubGraphs(wordsKernels));
		//out.println("Dependent Head Scores: " +
		//		Arrays.toString(NodeSetCollector.dependentHeadScores(wordsKernels, thisStructureWithoutEmpty)));
		//out.println("Head Dependencts Scores: " +
		//		Arrays.toString(NodeSetCollector.headDependentsScores(wordsKernels, thisStructureWithoutEmpty)));
		out.println(
			"Min derivation length " + NodeSetCollector.minDerivationLength(wordsKernels, tree,
					thisStructureWithoutEmpty, countEmptyDaughters));
	}
	
	public static String toStringGraphKernels(int DKG_type, TDNode currentNode, 
			ArrayList<TDNode> treebank, int ignoreIndex, int maxBranching, 
			int collectionType, int SLP_type, boolean countEmptyDaughters,
			boolean adjacency) {
		
		DepKernelGraph DKGO = getDepKernelGraph(DKG_type, 
				currentNode, treebank, ignoreIndex, maxBranching, 
				collectionType, SLP_type, countEmptyDaughters, adjacency);
		
		NodeSetCollector[] wordsKernels = DKGO.computePeakGraphKernels();
		
		TDNode[] structure = currentNode.getStructureArray();
		String result = "";
		for(TDNode nodeA : structure) {
			result += toStringKernels(wordsKernels[nodeA.index], nodeA);
		}
		return result;
	}

	
	/*public NodeSetCollector[] computePeakGraphKernelsDebug(TDNode[] structure) {
		int index = 0;
		NodeSetCollector[] wordsKernels = new NodeSetCollector[currentLength];
		initKernelTables(wordsKernels);
		for(TDNode otherTree : treebank) {			
			if (index!=ignoreIndex) {
				NodeSetCollector[] wordsKernelsTemp = new NodeSetCollector[currentLength];
				initKernelTables(wordsKernelsTemp);
				NodeSetCollector[][] CPG = getCPG(otherTree);	
				removeSingletons(CPG);
				if (extractKernel(CPG, wordsKernelsTemp)) {
					out.println(index);
					out.println(otherTree.toStringMSTulab(false));
					out.println(Arrays.toString(wordsKernelsTemp));
					out.println();
					for(int i=0; i<wordsKernels.length; i++) {
						wordsKernels[i].addAll(wordsKernelsTemp[i]);
					}
				}
			}
			index++;
		}
		printKernelsAndScores(wordsKernels);		
		printAllScores(wordsKernels, structure);
		return wordsKernels;
	}*/
	

	
	public NodeSetCollector[] computePeakGraphKernels() {
		int index = 0;
		NodeSetCollector[] wordsKernels = new NodeSetCollector[currentLengthWithoutEmpty];
		initKernelTables(wordsKernels);
		for(TDNode otherTree : treebank) {
			TDNode[] otherStructure = otherTree.getStructureArray();
			if (index!=ignoreIndex) {
				NodeSetCollector[][] CPG = getCPG(otherStructure);
				removeSingletons(CPG);
				extractKernel(CPG, otherStructure, wordsKernels);
			}
			index++;
		}		
		return wordsKernels;
	}
	
	public NodeSetCollector[] computePeakGraphKernelsDebug() {
		int index = 0;
		NodeSetCollector[] wordsKernels = new NodeSetCollector[currentLength];
		NodeSetCollector[] tempWordsKernels = new NodeSetCollector[currentLength];
		initKernelTables(wordsKernels);		
		for(TDNode otherTree : treebank) {
			initKernelTables(tempWordsKernels);
			TDNode[] otherStructure = otherTree.getStructureArray();
			if (index!=ignoreIndex) {
				NodeSetCollector[][] CPG = getCPG(otherStructure);
				removeSingletons(CPG);
				if (extractKernel(CPG, otherStructure, tempWordsKernels)) {
					out.println(index);
					out.println(otherTree.toStringMSTulab(false, countEmptyDaughters));
					out.println(Arrays.toString(tempWordsKernels));
					out.println();
					for(int i=0; i<wordsKernels.length; i++) {
						wordsKernels[i].addAll(tempWordsKernels[i]);
					}
				}
			}
			index++;
		}
		printKernels(wordsKernels);
		return wordsKernels;
	}
	
	private void removeSingletons(NodeSetCollector[][] CPG) {
		for(NodeSetCollector[] wordCollectors : CPG) {
			for(NodeSetCollector coll : wordCollectors) {
				coll.removeUniqueSingleton();
			}			
		}		
	}
	
	private void removeSingletons(NodeSetCollector[] wordKernels) {
		for(TDNode nodeA : thisStructure) {
			NodeSetCollector wordKernel = wordKernels[nodeA.index];
			wordKernel.removeSingleton(nodeA.index);
		}		
	}

	private void initKernelTables(NodeSetCollector[] kernels) {
		// int collectionType; 0: NodeSetCollectorStandard, 1: NodeSetCollectorFreq, 2: NodeSetCollectorMUB
		switch(collectionType) {
			case 0:
				for(int i=0; i<kernels.length; i++) 
					kernels[i] = new NodeSetCollectorStandard();
				return;
			case 1:
				for(int i=0; i<kernels.length; i++) 
					kernels[i] = new NodeSetCollectorFreq();
				return;
			case 2:
				for(int i=0; i<kernels.length; i++) 
					kernels[i] = new NodeSetCollectorMUB();
				return;
			case 3:
				for(int i=0; i<kernels.length; i++) 
					kernels[i] = new NodeSetCollectorMUBFreq();
				return;
			case 4:
				for(int i=0; i<kernels.length; i++) 
					kernels[i] = new NodeSetCollectorUnion();
				return;
			case 5:
				for(int i=0; i<kernels.length; i++) 
					kernels[i] = new NodeSetCollectorArgFreq();
				return;
			case 6:
				for(int i=0; i<kernels.length; i++) 
					kernels[i] = new NodeSetCollectorArgFreqTerm();
				return;
		}
	}
	
	public NodeSetCollector getNewCollecor() {
		switch(collectionType) {
			case 0: return new NodeSetCollectorStandard();
			case 1: return new NodeSetCollectorFreq();
			case 2: return new NodeSetCollectorMUB();
			case 3: return new NodeSetCollectorMUBFreq();
			case 4: return new NodeSetCollectorUnion();
			case 5: return new NodeSetCollectorArgFreq();
			case 6: return new NodeSetCollectorArgFreqTerm();
		}
		return null;
	}
	
	
	private boolean extractKernel(NodeSetCollector[][] CPG, TDNode[] otherTreeStructure,
			NodeSetCollector[] wordsKernels) {
		boolean result = false;
		int rows = CPG.length;
		int columns = CPG[0].length;
		int index = 0;
		for(int i=0; i<rows; i++) {
			if (countEmptyDaughters && thisStructure[i].isEmptyDaughter()) continue;
			for(int j=0; j<columns; j++) {
				if (countEmptyDaughters && otherTreeStructure[j].isEmptyDaughter()) continue;
				result = wordsKernels[index].addAll(CPG[i][j]) || result;				
			}
			index++;
		}
		return result;
	}
	
	public void printKernels(NodeSetCollector[] wordKernels) {
		int index = 0;
		for(TDNode nodeA : thisStructureWithoutEmpty) {
			printKernels(wordKernels[index], nodeA);
			index++;
		}
	}
	
	
	public void printKernelsAndScores(NodeSetCollector[] wordKernels) {
		int index = 0;
		for(TDNode nodeA : thisStructureWithoutEmpty) {
			printKernels(wordKernels[index], nodeA);
			index++;
		}				
		this.printAllScores(wordKernels, currentNode, thisStructure);
	}
	
	public void printKernelsAndProb(NodeSetCollector[] wordKernels, 
			Hashtable<String,Integer> headStatTable) {
		int index = 0;
		for(TDNode nodeA : thisStructureWithoutEmpty) {
			printKernels(wordKernels[index], nodeA);
			index++;
		}		
		float[] probs = this.computeKernelProb(headStatTable);
		out.println("Kerenel probs: " + Arrays.toString(probs));
	}
	
	public static void printKernels(NodeSetCollector wordKernels, TDNode wordNode) {
		if (wordKernels.isEmpty()) return;
		out.println(wordNode.lexPosTagIndex());
		out.println(wordKernels);
		out.println();
	}
	
	public static String toStringKernels(NodeSetCollector wordKernels, TDNode wordNode) {
		if (wordKernels.isEmpty()) return "";
		return wordNode.lexPosTagIndex() + "\n" + wordKernels + "\n\n";
	}
	
	/**
	 * Get common peak graphs
	 * @param otherTree
	 * @return
	 */
	public NodeSetCollector[][] getCPG(TDNode[] otherStructureArray) {		
		int otherSize = otherStructureArray.length;
		boolean[][] SP = buildSLP(otherStructureArray);	
		NodeSetCollector[][] CPG = new NodeSetCollector[currentLength][otherSize];		
		for(TDNode nodeA : thisStructure) {
			for(TDNode nodeB : otherStructureArray) {				
				getCPG(nodeA, nodeB, CPG, SP);				
			}			
		}
		return CPG;
	}
	
	private boolean[][] buildSLP(TDNode[]  otherStructureArray) {
		//int SLP_type = 0; //0: same postag, 1: same lex, 2: same lexpostag, 3: mix
		return DepKernelPaths.buildSLP(thisStructure, otherStructureArray, SLP_type);
	}
	
	/**
	 * Common peak graphs
	 * @param A
	 * @param B
	 * @param CDP
	 * @param CPP
	 * @param SP
	 * @return
	 */
	public abstract NodeSetCollector getCPG(TDNode A, TDNode B, 
			NodeSetCollector[][] CPG, boolean[][] SP);

	public static ArrayList<TDNode> makePair(TDNode nodeA, TDNode nodeB) {
		ArrayList<TDNode> pair = new ArrayList<TDNode>(2);
		pair.add(nodeA);
		pair.add(nodeB);
		return pair;
	}
	
	public static void mainScore() {
		int uk_limit = -1;
		int LL_tr = 10;			
		boolean till11_21 = false;	
		int maxBranching = 10;
		
		boolean countEmptyDaughters = false;
		boolean adjacency = false;
		boolean addEOS = true;		
		
		int depType = 3; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg"	
		
		int DKG_type = 4;
				//0: order
				//1: noOrder
				//2: LR1Level
				//3: orderNoFreq
				//4: argFrameOrder 
		
		int SLP_type = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		
		int collectionType = 1;
				//0: NodeSetCollectorStandard, 
				//1: NodeSetCollectorFreq, 
				//2: NodeSetCollectorMUB
				//3: NodeSetCollectorMUBFreq
				//4: NodeSetCollectorUnion
				//5: NodeSetCollectorArgFreq
		
		String trSec = till11_21 ? "02-11" : "02-21";
		File output = new File(Parameters.corpusPath + "ToyGrammar/rerankOut8.txt");
		out = FileUtil.getPrintWriter(output);
		
		File toyFile = new File(Parameters.corpusPath + "ToyGrammar/rerank.txt");
		File trainingFile = new File (Reranker.depTypeBase[depType] + "wsj-" + trSec + ".ulab");
		ArrayList<TDNode> treebank = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		Duet<ArrayList<TDNode>,ArrayList<String>> testComment = 
			DepCorpus.readTreebankFromFileMSTComments(toyFile, LL_tr+1, false, true);
		ArrayList<TDNode> test = testComment.getFirst();
		
		Set<String> lexicon = null;
		
		if (addEOS) {
			DepCorpus.addEOS(treebank);
			//DepCorpus.addEOS(test);
		}
		if (countEmptyDaughters) {
			DepCorpus.addEmptyDaughters(treebank);
			DepCorpus.addEmptyDaughters(test);
		}
		if (uk_limit > 0) lexicon = DepCorpus.extractLexiconWithoutUnknown(treebank, uk_limit);
		
		for(TDNode TDN : test) {			
			//TDNode TDN = test.get(0);
			out.println("Test sentence:");
			out.println(TDN.toStringMSTulab(false));			
			out.println("---------------------------------");
			if (uk_limit > 0) TDN.renameUnknownWords(lexicon, DepCorpus.ukTag);
			
			DepKernelGraph DKSTO = getDepKernelGraph(DKG_type, 
					TDN, treebank, -5, maxBranching, 
					collectionType, SLP_type, countEmptyDaughters, adjacency);
			
			
			//DKSTO.printKernels(DKSTO.computePeakGraphKernels());
			//DKSTO.printKernelsAndScores(DKSTO.computePeakGraphKernels());
			DKSTO.computePeakGraphKernelsDebug();
			
			//DKSTO.computeKernel();
			//System.out.println(DepKernelGraphOrder.computeKernelScore(TDN, treebank, -5, 
			//		maxBranching, collectionType, SLP_type));
			out.println("---------------------------------");
			out.flush();
		}
		out.close();
	}
	
	public static void mainProb() {
		int LL_tr = 10;				
		int uk_limit = -1;
		boolean till11_21 = false;
		int maxBranching = 10; //branching factor
		
		boolean countEmptyDaughters = false;
		boolean adjacency = false;
		
		int DKG_type = 4;
				//0: order
				//1: noOrder
				//2: LR1L
				//3: orderNoFreq
				//4: ArgFrameOrder
		int collectionType = 5; 
				//0: NodeSetCollectorStandard, 
				//1: NodeSetCollectorFreq, 
				//2: NodeSetCollectorMUB
				//3: NodeSetCollectorMUBFreq
				//4: NodeSetCollectorUnion
				//5: NodeSetCollectorArgFreq
		int SLP_type = 0; 
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		
		String trSec = till11_21 ? "02-11" : "02-21";
		Parameters.outputPath = Parameters.corpusPath + "ToyGrammar/";
		File output = new File(Parameters.outputPath + "rerankOut2.txt");
		out = FileUtil.getPrintWriter(output);
		
		File toyFile = new File(Parameters.outputPath + "rerank.txt");
		File trainingFile = new File (WsjD.WsjMSTulab + "wsj-" + trSec + ".ulab");
		ArrayList<TDNode> treebank = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);		
		Duet<ArrayList<TDNode>,ArrayList<String>> testComment = DepCorpus.readTreebankFromFileMSTComments(toyFile, LL_tr, false, true);
		ArrayList<TDNode> test = testComment.getFirst();
		
		Set<String> lexicon = null;
		if (uk_limit > 0) lexicon = DepCorpus.extractLexiconWithoutUnknown(treebank, uk_limit);
		
		Hashtable<String, Integer> headStatTable = 
			DepCorpus.getHeadFreqTable(treebank, SLP_type);
		FileUtil.printHashtableToFile(headStatTable, 
				new File(Parameters.outputPath + "HeadStatTable.txt"));
		
		//File ruleTableFile = new File(Parameters.outputPath + "rules.txt");
		//Hashtable<String, Integer> ruleTable = DepCorpus.updateHeadLRRuleFreqTable(treebank, SLP_type);
		//FileUtil.printHashtableToFile(ruleTable, ruleTableFile);
		
		if (countEmptyDaughters) {
			DepCorpus.addEmptyDaughters(treebank);
			DepCorpus.addEmptyDaughters(test);
		}		
		
		for(TDNode TDN : test) {			
			//TDNode TDN = test.get(1);
			out.println("Test sentence:");
			out.println(TDN.toStringMSTulab(false));			
			out.println("---------------------------------");
			if (uk_limit > 0) TDN.renameUnknownWords(lexicon, DepCorpus.ukTag);
			
			DepKernelGraph DKSTO = getDepKernelGraph(DKG_type, 
					TDN, treebank, -5, maxBranching, 
					collectionType, SLP_type, countEmptyDaughters, adjacency);
				
			
			DKSTO.printKernelsAndProb(DKSTO.computePeakGraphKernels(), headStatTable );
			//DKSTO.computePeakGraphKernelsDebug(DKSTO.thisStructureArray);
			
			//DKSTO.computeKernel();
			//System.out.println(DepKernelGraphOrder.computeKernelScore(TDN, treebank, -5, 
			//		maxBranching, collectionType, SLP_type));
			out.println("---------------------------------");
		}
		out.close();
	}
	
	
	public static void main(String[] args) {
		mainProb();
		//mainScore();
	}

	
}
