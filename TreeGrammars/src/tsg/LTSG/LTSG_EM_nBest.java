package tsg.LTSG;

import java.util.*;

import settings.Parameters;
import tsg.TSNode;
import tsg.corpora.*;
import tsg.parser.Parser;
import util.*;
import util.file.FileUtil;

public class LTSG_EM_nBest extends LTSG{
	
	Hashtable<String, Double> template_prob;
	Hashtable<String, Double> root_prob;	
	public LTSG_EM_nBest() {
		super();
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
	
	private void extractUniformTreeProbExcluding(TSNode treeToExclude) {		
		this.decreaseElementayTreesFrom(treeToExclude);
		template_prob = new Hashtable<String, Double>();
		for(Map.Entry<String, Integer> e : template_freq.entrySet()) {
			String tree = e.getKey();
			double count = e.getValue();
			template_prob.put(tree, count);
		}		
		normalizeTemplateProb();
		this.increaseElementaryTreeesFrom(treeToExclude);
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
	
	public void checkEMCoverage() {
		ConstCorpus originalTrainingCorpus = null;
		if (Parameters.posTagConversion) {
			originalTrainingCorpus = Parameters.trainingCorpus.deepClone();
			Parameters.trainingCorpus.makePosTagsLexicon();
		}
		this.extractAllLexTrees();
		int covered = 0;
		for(TSNode inputTree : Parameters.trainingCorpus.treeBank) {
			this.decreaseElementayTreesFrom(inputTree);
			if (checkCoverageTree(inputTree)) covered++;
			this.increaseElementaryTreeesFrom(inputTree);
		}
		if (Parameters.posTagConversion) {
			Parameters.trainingCorpus.unMakePosTagsLexicon(originalTrainingCorpus);
		}
		int treebankSize = Parameters.trainingCorpus.size();
		float ratio = (float)covered/treebankSize;
		System.out.println("Covered tree in training corpus: " + covered 
				+ " / " + treebankSize + " (" + ratio + ")");
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
	private boolean checkCoverageTree(TSNode inputTree) {
		List<ArrayList<TSNode>> levelsSubTrees = inputTree.getNodesInDepthLevels();
		IdentityHashMap<TSNode, Boolean> coveredSubTrees = new IdentityHashMap<TSNode, Boolean>();
		for(ArrayList<TSNode> level : levelsSubTrees) {
			for (TSNode TN : level) {
				if (TN.isLexical || TN.isUniqueDaughter()) continue;
				boolean covered = false;
				if (TN.isPrelexical() || !TN.hasMoreThanNBranching(1)) {
					if (template_freq.keySet().contains(TN.toString(false, true))) {
						covered = true;
					}
				}
				else {
					List<TSNode> lexicon = TN.collectLexicalItems();
					for(TSNode anchor : lexicon) {
						TN.markHeadPathToAnchor(anchor);
						TSNode lexTemplate = TN.lexicalizedTreeCopy();
						lexTemplate.applyAllConversions();																
						List<TSNode> subSites = TN.collectSubstitutionSites();
						TN.unmarkHeadPathToAnchor(anchor);				
						if (!template_freq.keySet().contains(lexTemplate.toString(false, true))) continue;
						covered = true;
						for(TSNode SS : subSites) {
							if (!coveredSubTrees.keySet().contains(SS)) {
								covered = false;
								break;
							}
						}
						if (covered) break;
					}									
				}
				if (covered) coveredSubTrees.put(TN, true);
			}
		}
		return coveredSubTrees.keySet().contains(inputTree);
	}
	
	public void EMHeldOutAlgorithm() {
		ConstCorpus originalTrainingCorpus = null;
		if (Parameters.posTagConversion) {
			originalTrainingCorpus = Parameters.trainingCorpus.deepClone();
			Parameters.trainingCorpus.makePosTagsLexicon();
		}
		this.extractAllLexTrees();
		PrintProgressStatic.start("Estimating EM param. sentence:");
		for(TSNode observedTree : Parameters.trainingCorpus.treeBank) {
			PrintProgressStatic.next();
			extractUniformTreeProbExcluding(observedTree);
			int cycle = 0;
			double previousLikelihood = -Double.MAX_VALUE, delta = 0;			
			do {
				cycle++;						
				observedTree.removeHeadAnnotations();
				Hashtable<String, Double> new_template_prob = new Hashtable<String, Double>();
				Double actualLikelihood = getNBestHeadAnnotations(observedTree, new_template_prob);
				if (actualLikelihood==null) {
					FileUtil.appendReturn("No coverage for " + observedTree, Parameters.logFile);
					observedTree.assignRandomHeads();
					break;
				}
				delta = actualLikelihood - previousLikelihood;
				previousLikelihood = actualLikelihood;
				template_prob = new_template_prob;
				normalizeTemplateProb();
			} while (delta > 0 && delta > Parameters.EM_deltaThreshold && cycle<Parameters.EM_maxCycle);
		}
		PrintProgressStatic.end();
		if (Parameters.posTagConversion) {
			Parameters.trainingCorpus.unMakePosTagsLexicon(originalTrainingCorpus);
		}
	}

	public void EMalgorithm() {
		ConstCorpus originalTrainingCorpus = null;
		if (Parameters.posTagConversion) {
			originalTrainingCorpus = Parameters.trainingCorpus.deepClone();
			Parameters.trainingCorpus.makePosTagsLexicon();
		}
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
		} while (delta > 0 && delta > Parameters.EM_deltaThreshold && cycle<Parameters.EM_maxCycle);
		if (Parameters.posTagConversion) {
			Parameters.trainingCorpus.unMakePosTagsLexicon(originalTrainingCorpus);
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
				TSNode  lexicalTree = TN.lexicalizedTreeCopy();
				lexicalTree.applyAllConversions();	
				Double weight = template_prob.get(lexicalTree.toString(false, true));
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
	private Double getNBestHeadAnnotations(TSNode inputTree, Hashtable<String, Double> new_template_prob) {
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
		if (nBestTable==null) return null;
		int i;
		for (i=0; i<nBestTable.length; i++) {
			totalProbability += Math.exp(nBestTable[i].logDerivationProb);
		}
		for (--i; i>-1; i--) {
			inputTree.removeHeadAnnotations();
			assignHeadAnnotation(inputTree, nBestDerivationsSubTrees, i);
			List<TSNode> eTrees = inputTree.lexicalizedTreesFromHeadAnnotation();
			double newWeight = Math.exp(nBestTable[i].logDerivationProb) / totalProbability;
			for(TSNode lexTree : eTrees) {
				lexTree.applyAllConversions();	
//				double oldWeight = template_prob.get(lexTree.toString(false, true));				
				Utility.increaseStringDouble(new_template_prob, lexTree.toString(false, true), newWeight);
			}
		}
		return Math.log(totalProbability);
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
	
	public static void coverage() {
		Parameters.setDefaultParam();		
		Parameters.lengthLimitTraining = 40;
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		Parameters.LTSGtype = "EM"; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";
		LTSG_EM_nBest Grammar = new LTSG_EM_nBest();
		Grammar.checkEMCoverage();
	}

	public static void EmStandard(String args[]) {
		Parameters.setDefaultParam();		
		Parameters.lengthLimitTraining = 10;
		Parameters.lengthLimitTest = 10;
		
		Parameters.LTSGtype = "EM"; //GreedyTop GreedyBottom GreedyTopEntropy
		
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";

		Parameters.EM_nBest = 400;
		Parameters.EM_deltaThreshold = 0.1;
		Parameters.EM_maxCycle = Integer.MAX_VALUE;
		
		Parameters.parserName = Parser.bitPar;
		Parameters.nBest = 1;		
		Parameters.cachingActive = false;
		
		Parameters.posTagConversion = false;
		Parameters.spineConversion = false;
		
		LTSG_EM_nBest Grammar = new LTSG_EM_nBest();
		Grammar.reportMaxLexicalDerivations();
		Grammar.EMalgorithm();
		Grammar.readTreesFromCorpus();
		//java.io.File templateFile = new java.io.File("/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/EM/Tue_Jun_24_21_14_50/TemplatesFile");
		//Grammar.readTreesFromFile(templateFile);
		
		
		
		Grammar.printTemplatesToFile();
		
		Grammar.treatTreeBank();			
		Grammar.toPCFG();
		
		Parameters.printTrainingCorpusToFile();		
		Grammar.printLexiconAndGrammarFiles();		
				
		
		new Parser(Grammar);
	}
	
	public static void EMHeldOut() {
		Parameters.setDefaultParam();		
		//Parameters.lengthLimitTraining = 7;
		Parameters.smoothing = false;
		
		Parameters.LTSGtype = "EM"; //GreedyTop GreedyBottom GreedyTopEntropy
		
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";

		Parameters.EM_nBest = 100;
		Parameters.EM_deltaThreshold = 0.1;
		Parameters.EM_maxCycle = 1;
		
		Parameters.parserName = Parser.bitPar;
		Parameters.nBest = 1;		
		Parameters.cachingActive = false;
		
		Parameters.posTagConversion = false;
		Parameters.spineConversion = true;
		
		LTSG_EM_nBest Grammar = new LTSG_EM_nBest();
		Grammar.reportMaxLexicalDerivations();
		Grammar.EMHeldOutAlgorithm();
		Grammar.readTreesFromCorpus();
		//java.io.File templateFile = new java.io.File("/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/EM/Tue_Jun_24_21_14_50/TemplatesFile");
		//Grammar.readTreesFromFile(templateFile);
		
		
		
		Grammar.printTemplatesToFile();
		
		Grammar.treatTreeBank();			
		Grammar.toPCFG();
		
		Grammar.printTrainingCorpusToFile();		
		Grammar.printLexiconAndGrammarFiles();		
				
		
		new Parser(Grammar);
	}
	
	public static void main(String[] args) {
		EmStandard(args);
		//EMHeldOut();
	}

}
