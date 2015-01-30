package tsg.LTSG;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import settings.Parameters;
import tsg.CFSG;
import tsg.TSNode;
import tsg.corpora.*;
import tsg.parser.Parser;
import util.*;
import util.file.FileUtil;

public class LTSG_EMProb extends CFSG<Double>{
	
	Hashtable<String, Integer> template_freq;
	Hashtable<String, Integer> lexicon_freq;
	Hashtable<String, Double> template_prob;
	Hashtable<String, Double> root_prob;	
	
	
	public LTSG_EMProb() {
		super();
		template_freq = new Hashtable<String, Integer>();
		lexicon_freq = new Hashtable<String, Integer>();
		template_prob = new Hashtable<String, Double>();
		root_prob = new Hashtable<String, Double>();
	}
	
	public void toPCFG() {
		if (Parameters.smoothing) {
			for(TSNode TreeLine : Parameters.trainingCorpus.treeBank) {
				TreeLine.toNormalForm();
				List<TSNode> nonLexicalNodes = TreeLine.collectNonLexicalNodes();
				for(TSNode nonTerminal : nonLexicalNodes) {
					Hashtable<String, Double> toAdd = (nonTerminal.isPrelexical()) ? lexRules : internalRules;
					String rule = nonTerminal.toCFG(false);
					Utility.increaseStringDouble(toAdd, rule, 1.);
				}
			}
		}
		int uniqueLableIndex = 1;
		for (Enumeration<String> e = template_prob.keys(); e.hasMoreElements() ;) {
			String eTree = e.nextElement();			
			Double count = template_prob.get(eTree);
			TSNode TN = new TSNode(eTree, false);
			TN.toNormalForm();
			uniqueLableIndex = TN.toUniqueInternalLabels(false, uniqueLableIndex, false);
			List<TSNode> nonTerminals = TN.collectNonTerminalNodes();
			for (TSNode nonTerminal : nonTerminals) {
				String rule = nonTerminal.toCFG(false);
				Hashtable<String, Double> toAdd = (nonTerminal.isPrelexical()) ? lexRules : internalRules;
				Utility.increaseStringDouble(toAdd, rule, count * Parameters.smoothingFactor);					
			}
		}
		
		String log = "Converted trees to PCFG (smoothing = " + Parameters.smoothing + ")"
						+ "\n\t# Internal Rules: " + internalRules.size() 
						+ "\n\t# Lex Rules: " + lexRules.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	public void removeZeroProbTrees() {		
		int templatesSizeBefore = template_prob.size();
		for (Iterator<Map.Entry<String,Double>> i= template_prob.entrySet().iterator(); i.hasNext() ;) {
			Map.Entry<String,Double> entry = i.next();			
			Double count = entry.getValue();
			if (count==0.) i.remove();
		}
		String log = "Removing templates with zero probability"
						+ "\n\t# Templates # before removal: " + templatesSizeBefore
						+ "\n\t# Templates # after removal: " + template_prob.size();				
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	
	public void printTemplatesToFile() {
		File templatesFile = new File(Parameters.outputPath + "TemplatesFile");
		try {
			PrintWriter grammar = new PrintWriter(new BufferedWriter(new FileWriter(templatesFile)));
			for (Enumeration<String> e = template_prob.keys(); e.hasMoreElements() ;) {
				String tree = e.nextElement();			
				Double count = template_prob.get(tree);										
				String line = count.toString() + " " + tree;
				grammar.write(line + "\n");
			}	
			grammar.close();
		} catch (Exception e) {FileUtil.handleExceptions(e);}	
		String log = "Printed templates to file `templatesFile`";
		FileUtil.appendReturn(log, Parameters.logFile);
	}	
	
	
	public void readTreesFromFile(File templateFile) {
		template_prob.clear();
		root_prob.clear();
		Scanner scan = FileUtil.getScanner(templateFile);
		while(scan.hasNextLine()) {
			Double prob = scan.nextDouble();
			String tree = scan.nextLine().trim();			
			template_prob.put(tree, prob);
			Utility.increaseStringDouble(root_prob, TSNode.get_unique_root(tree), prob);
		}
	}
	
	private void extractRootProb() {
		root_prob = new Hashtable<String, Double>();
		for(Map.Entry<String, Double> e : template_prob.entrySet()) {		
			String root = TSNode.get_unique_root(e.getKey());
			Double count = e.getValue();
			Utility.increaseStringDouble(root_prob, root, count);			
		}
	}
	
	private void initializeUniformTreeProb() {
		this.extractAllLexTrees();
		template_prob = new Hashtable<String, Double>();
		for(Map.Entry<String, Integer> e : template_freq.entrySet()) {
			String tree = e.getKey();
			double count = e.getValue();
			template_prob.put(tree, count);
		}		
		normalizeTemplateProb();
	}
	
	private void normalizeTemplateProb() {
		extractRootProb();
		for(Map.Entry<String, Double> e : template_prob.entrySet()) {			
			String tree = e.getKey();
			double treeProb = e.getValue();
			String root = TSNode.get_unique_root(tree);
			double rootCount = root_prob.get(root);
			double newTreeProb = treeProb / rootCount;			
			e.setValue(newTreeProb);
		}
	}
	
	public void reportMaxLexicalDerivations() {
		long maxDerivation = 0;
		for(TSNode inputTree : Parameters.trainingCorpus.treeBank) {
			long lexDerivations = inputTree.lexDerivations();
			if (lexDerivations>maxDerivation) maxDerivation = lexDerivations;
		}
		FileUtil.appendReturn("Max number of derivation per tree: " + maxDerivation, Parameters.logFile);
	}
	
	public void EMalgorithm() {
		initializeUniformTreeProb();
		int cycle = 0;
		double previousLikelihood = -Double.MAX_VALUE, delta = 0;
		do {		
			cycle++;			
			//System.out.print("EM Cycle " + cycle + "Tr. Sentence #  ");
			double actualLikelihood = emSteps();
			//System.out.println();
			delta = actualLikelihood - previousLikelihood;
			previousLikelihood = actualLikelihood;
			String line = "EM cycle: " + cycle + "\tActual LikeLihood: " + 
				actualLikelihood + "\tDelta LikeLihood: " + delta;
			FileUtil.appendReturn(line, Parameters.logFile);
		} while (delta > 0 && delta > Parameters.EM_deltaThreshold);
	}
	
	private void subtractProbability(TSNode tree) {
		List<TSNode> anchors = tree.collectLexicalItems();
		List<ArrayList<TSNode>> eTrees = LTSG.allLexTreesForEachAnchor(tree, false, anchors);
		//ArrayList<String> modifiedRoot = new ArrayList<String>();
		for(ArrayList<TSNode> terminalTrees : eTrees) {				
 			for (TSNode lexTree : terminalTrees) {		
 				//String root = lexTree.label();
 				//modifiedRoot.add(root);
				Utility.decreaseStringInteger(template_freq, lexTree.toString(false, true), 1);				
			}
		}
	}

	private void addProbability(TSNode tree) {
		List<TSNode> anchors = tree.collectLexicalItems();
		List<ArrayList<TSNode>> eTrees = LTSG.allLexTreesForEachAnchor(tree, false, anchors);
		//ArrayList<String> modifiedRoot = new ArrayList<String>();
		for(ArrayList<TSNode> terminalTrees : eTrees) {				
 			for (TSNode lexTree : terminalTrees) {		
 				//String root = lexTree.label();
 				//modifiedRoot.add(root);
				Utility.increaseStringInteger(template_freq, lexTree.toString(false, true), 1);				
			}
		}
	}

	
	private double emSteps() {		
		double likelihood = 0;
		Hashtable<String, Double> new_template_prob = new Hashtable<String, Double>();
		int treeIndex = 0;
		for(TSNode inputTree : Parameters.trainingCorpus.treeBank) {
			treeIndex++;			
			inputTree.removeHeadAnnotations();
			likelihood += getNBestHeadAnnotations(inputTree, new_template_prob);
			//Utility.printProgress(treeIndex);
			if (inputTree.hasWrongHeadAssignment()) {
				System.err.println("Wrong Head Assignment: " + inputTree.toString(true, true));
			}
		}
		template_prob = new_template_prob;
		normalizeTemplateProb();
		return likelihood;
	}

	
	/**
	 * Method to assign the head annotation according to the method getNBestHeadAnnotations()
	 * @param nBestDerivationsSubTrees
	 * @param index
	 */
	private void assignHeadAnnotation(TSNode inputTree, 
			IdentityHashMap<TSNode, LexicalDerivation[]> nBestDerivationsSubTrees, int index) {		
		LexicalDerivation lexD = nBestDerivationsSubTrees.get(inputTree)[index];	
		int[] indexes = lexD.subSiteDerivationsIndexes;		
		TSNode anchor = lexD.anchor;
		TSNode lexiconPath = anchor.parent;
		TSNode lexiconPathDaughter = anchor;
		int substitutionIndex = 0;
		do {
			if (lexiconPath == inputTree) break;
			lexiconPath.headMarked = true;
			lexiconPath = lexiconPath.parent;
			lexiconPathDaughter = lexiconPathDaughter.parent;			
			for(TSNode D : lexiconPath.daughters) {				
				if (D == lexiconPathDaughter) continue;
				assignHeadAnnotation(D, nBestDerivationsSubTrees, indexes[substitutionIndex++]);
			}												
		} while(lexiconPath != inputTree);
	}
	
	private LexicalDerivation[] getNBestTable(TSNode TN, 
			IdentityHashMap<TSNode, LexicalDerivation[]> nBestDerivationsSubTrees){
			TSNode[] lexicalsTN = TN.collectTerminals().toArray(new TSNode[]{});
			double[] lexTreeWeights = new double[lexicalsTN.length];
			LexicalDerivation[][][] lex_SubSite_NBestTable = new LexicalDerivation[lexicalsTN.length][][];
			int nonNullLex = 0;
			int lexicalIndex = -1;
			for (TSNode anchor  : lexicalsTN) {
				lexicalIndex++;
				int substitutionSites = 0;
				TSNode lexiconPath = anchor.parent;
				while(lexiconPath != TN) {					
					substitutionSites += lexiconPath.prole() - 1;
					lexiconPath.headMarked = true;
					lexiconPath = lexiconPath.parent;							
				} 
				substitutionSites += lexiconPath.prole() - 1;						
				String  lexicalTree = TN.lexicalizedTreeCopy().toString(false, true);
				Double weight = template_prob.get(lexicalTree);
				if (weight==null) {
					TN.unmarkHeadPathToAnchor(anchor);
					lexTreeWeights[lexicalIndex] = -1;					
					lex_SubSite_NBestTable[lexicalIndex] = null;					
					continue;
				}				
				double logWeight = Math.log(weight);
				lexTreeWeights[lexicalIndex] = logWeight;
				lexiconPath = anchor.parent;
				lexiconPath.headMarked = false;
				TSNode lexiconPathDaughter = anchor;
				lex_SubSite_NBestTable[lexicalIndex] = new LexicalDerivation[substitutionSites][]; 
				int substitutionIndex = 0;
				boolean nullSubSide = false;
				do {												
					lexiconPath = lexiconPath.parent;
					lexiconPathDaughter = lexiconPathDaughter.parent;
					lexiconPath.headMarked = false;
					for(TSNode D : lexiconPath.daughters) {
						if (D == lexiconPathDaughter) continue;
						lex_SubSite_NBestTable[lexicalIndex][substitutionIndex] = 
							nBestDerivationsSubTrees.get(D);
						if (lex_SubSite_NBestTable[lexicalIndex][substitutionIndex]==null) nullSubSide=true;
						substitutionIndex++;
					}												
				} while(lexiconPath != TN);
				if (nullSubSide) {
					lexTreeWeights[lexicalIndex] = -1;					
					lex_SubSite_NBestTable[lexicalIndex] = null;
					continue;
				}
				nonNullLex++;
			}
			return computeNBestTable(nBestDerivationsSubTrees, lex_SubSite_NBestTable, nonNullLex,
					lexicalsTN, lexTreeWeights);
	}
	
	/**
	* 
	*/
	private static LexicalDerivation[] computeNBestTable(IdentityHashMap<TSNode, LexicalDerivation[]> 
	 nBestDerivationsSubTrees, LexicalDerivation[][][] lex_SubSite_NBestTable, int nonNullLex, 
	 TSNode[] lexicalsTN, double[] lexTreeWeights) {
		if (nonNullLex==0) return null;
		LexDerivationQueue[] lexQueues = new LexDerivationQueue[lexicalsTN.length];
		int totalCombination = 0;
		for(int i=0; i<lexicalsTN.length; i++) {
			if (lex_SubSite_NBestTable[i]==null) continue;
			lexQueues[i] = new LexDerivationQueue(lex_SubSite_NBestTable[i], lexicalsTN[i], i, lexTreeWeights[i]);
			totalCombination += lexQueues[i].combinations;
		}
		LDQQueue superQueue = new LDQQueue(lexQueues);
		int nBestTableSize = Math.min(Parameters.EM_nBest, totalCombination);
		if (nBestTableSize<0) nBestTableSize = Parameters.EM_nBest; //in case of overflow
		LexicalDerivation[] nBestTable = new LexicalDerivation[nBestTableSize];
		nBestTable[0] = superQueue.pollFirst();
		for(int i=1; i<nBestTableSize; i++) {
			nBestTable[i] = superQueue.addNeighboursAndPoll();
		}		
		return nBestTable;
	}

	/**
	 * Method to assign the head annotation according to EM algorithm
	 * @param n number of best derivations
	 * @param ETrees hashtable containing the frequency of the elementary trees
	 * @param newETrees
	 * @param categoryProbability
	 * @param operation
	 * @return
	 */
	private double getNBestHeadAnnotations(TSNode inputTree, Hashtable<String, Double> new_template_prob) {
		List<ArrayList<TSNode>> levelsSubTrees = inputTree.getNodesInDepthLevels();
		IdentityHashMap<TSNode, LexicalDerivation[]> nBestDerivationsSubTrees = 
			new IdentityHashMap<TSNode, LexicalDerivation[]>();
		for(ArrayList<TSNode> level : levelsSubTrees) {
			for (TSNode TN : level) {
				if (TN.isLexical || TN.isUniqueDaughter()) continue;
				LexicalDerivation[] nBestTable;
				//lexical, weight, list of other indexes
				if (TN.isPrelexical() || !TN.hasMoreThanNBranching(1)) {
					Double weight = template_prob.get(TN.toString(false, true));
					if (weight==null) continue;
					double logWeight = Math.log(weight);
					nBestTable = new LexicalDerivation[]{ 
							new LexicalDerivation(TN.getAnchor(), 0, logWeight, null, null)};
				}
				else nBestTable = getNBestTable(TN, nBestDerivationsSubTrees);
				nBestDerivationsSubTrees.put(TN, nBestTable);
			}
		}
		double totalProbability = 0;	
		LexicalDerivation[] nBestTable = nBestDerivationsSubTrees.get(inputTree);
		int i;
		for (i=0; i<nBestTable.length; i++) {
			totalProbability += Math.exp(nBestTable[i].logDerivationProb);
		}
		for (--i; i>-1; i--) {
			inputTree.removeHeadAnnotations();
			assignHeadAnnotation(inputTree, nBestDerivationsSubTrees, i);
			List<TSNode> eTrees = inputTree.lexicalizedTreesFromHeadAnnotation();
			for(TSNode lexTree : eTrees) {
				//double oldWeight = template_prob.get(lexTree.toString(false, true));
				double newWeight = Math.exp(nBestTable[i].logDerivationProb) / totalProbability;
				Utility.increaseStringDouble(new_template_prob, lexTree.toString(false, true), newWeight);
			}
		}
		return Math.log(totalProbability);
	}
	
	
	public void extractAllLexTrees() {
		Parameters.trainingCorpus.removeHeadAnnotations();
		int i = -1;
		for(TSNode inputTree : Parameters.trainingCorpus.treeBank) {
			i++;
			List<TSNode> lexicon = inputTree.collectLexicalItems();
			List<ArrayList<TSNode>> eTrees = 
				LTSG.allLexTreesForEachAnchor(inputTree, Parameters.spineConversion, lexicon);
			int terminalIndex = 0;
			for(ArrayList<TSNode> terminalTrees : eTrees) {		
				String lexAnchor = lexicon.get(terminalIndex).label();
	 			for (TSNode lexTree : terminalTrees) {																	
					Utility.increaseStringInteger(template_freq, lexTree.toString(false, true), 1);
					Utility.increaseStringInteger(lexicon_freq, lexAnchor, 1);
				}
	 			terminalIndex++;
			}
		}
		String log = "Extracted all possible lexicalized trees\n\t# Trees: " + template_freq.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}

	/**
	* 
	*/
	public static int[][] bestNcombinations(Double[][] substitutionSiteWeightLists, int n) {
		int elements = substitutionSiteWeightLists.length;
		int[] indexes = new int[elements];
		Arrays.fill(indexes, 1);
		int indexMax = 0;
		while(Utility.product(indexes)<n && indexMax!=-1) {
			double max = 0;
			indexMax = -1;
			for(int i=0; i<elements; i++) {
				Double nextWeightElement = substitutionSiteWeightLists[i][indexes[i]];
				if (nextWeightElement==null) continue;
				double nextWeightElementDouble = nextWeightElement.doubleValue(); 
				if (nextWeightElementDouble>max) {
					max = nextWeightElementDouble;
					indexMax = i;
				}				
			}
			if (indexMax != -1) indexes[indexMax]++;
		}
		return Utility.combinations(indexes);
	}

	public static void main(String args[]) {
		Parameters.setDefaultParam();
		Parameters.smoothing = false;
		
		
		Parameters.LTSGtype = "EM"; //GreedyTop GreedyBottom GreedyTopEntropy
		
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";

		Parameters.EM_nBest = 100;
		Parameters.EM_deltaThreshold = 0.1;
		
		Parameters.parserName = Parser.fedePar;
		Parameters.nBest = 1;		
		Parameters.cachingActive = true;
		
		LTSG_EMProb Grammar = new LTSG_EMProb();
		Grammar.reportMaxLexicalDerivations();
		
		Grammar.EMalgorithm();
		//java.io.File templateFile = new java.io.File("/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/EM/Wed_Jun_25_22_49_13/TemplatesFile");
		//Grammar.readTreesFromFile(templateFile);
						
		Grammar.removeZeroProbTrees();
		Grammar.toPCFG();
		
		Grammar.printTemplatesToFile();
		Grammar.printTrainingCorpusToFile();		
		Grammar.printLexiconAndGrammarFiles();		
				
		
		new Parser(Grammar);
	}

}
