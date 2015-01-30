package tsg.LTSG;

import java.util.*;

import settings.Parameters;
import tsg.TSNode;
import tsg.corpora.*;
import tsg.parser.Parser;
import util.*;
import util.file.FileUtil;

public class LTSG_EM extends LTSG{
	
	Hashtable<String, Double> template_prob;
	Hashtable<String, Double> root_prob;	
	public static final String initializeUNIFORM = "initializeUNIFORM";
	public static final String initializeDOP = "initializeDOP";
	
	
	public LTSG_EM() {
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
	
	private void initializeProb() {
		initializeProbExcluding(null);
	}
	
	private void initializeProbExcluding(TSNode treeToExclude) {
		this.extractAllLexTrees();
		if (treeToExclude!=null) this.decreaseElementayTreesFrom(treeToExclude);
		if (Parameters.EM_initialization.equals(LTSG_EM.initializeDOP)) {
			template_prob = new Hashtable<String, Double>();
			for(Map.Entry<String, Integer> e : template_freq.entrySet()) {
				String tree = e.getKey();
				double count = e.getValue();
				template_prob.put(tree, count);
			}		
		}
		else { //uniform
			template_prob = new Hashtable<String, Double>();
			for(Map.Entry<String, Integer> e : template_freq.entrySet()) {
				String tree = e.getKey();
				template_prob.put(tree, 1.);
			}					
		}
		normalizeTemplateProb();
		if (treeToExclude!=null) this.increaseElementaryTreeesFrom(treeToExclude);
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
			initializeProbExcluding(observedTree);
			int cycle = 0;
			double previousLikelihood = -Double.MAX_VALUE, delta = 0;			
			do {
				cycle++;						
				observedTree.removeHeadAnnotations();
				Hashtable<String, Double> new_template_prob = new Hashtable<String, Double>();
				Double actualLikelihood = getLikelihoodAndBestAnnotation(observedTree, new_template_prob);
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
	
	public void EMHeldOutAlgorithm(ConstCorpus heldOutCorpus) {
		ConstCorpus originalTrainingCorpus = null;
		ConstCorpus originalHeldOutCorpus = null;
		if (Parameters.posTagConversion) {
			originalTrainingCorpus = Parameters.trainingCorpus.deepClone();
			originalHeldOutCorpus = heldOutCorpus.deepClone();
			Parameters.trainingCorpus.makePosTagsLexicon();
			heldOutCorpus.makePosTagsLexicon();
		}
		initializeProb();
		int cycle = 0;
		double previousLikelihood = -Double.MAX_VALUE, delta = 0;
		do {		
			cycle++;
			boolean[] headVariation = new boolean[]{false};
			double currentLikelihood = emStepsHeldOutCorpus(heldOutCorpus);
			delta = currentLikelihood - previousLikelihood;
			previousLikelihood = currentLikelihood;
			String line = "EM cycle: " + cycle + "\tCurrent LogLikeLihood: " + 
				currentLikelihood + "\tDelta LogLikeLihood: " + delta + "\tHead Variation: " + headVariation[0];
			FileUtil.appendReturn(line, Parameters.logFile);
		} while (delta > 0 && delta > Parameters.EM_deltaThreshold && cycle<Parameters.EM_maxCycle);
		if (Parameters.posTagConversion) {
			Parameters.trainingCorpus.unMakePosTagsLexicon(originalTrainingCorpus);
			heldOutCorpus.unMakePosTagsLexicon(originalHeldOutCorpus);
		}
	}

	public void EMalgorithm() {
		ConstCorpus originalTrainingCorpus = null;
		if (Parameters.posTagConversion) {
			originalTrainingCorpus = Parameters.trainingCorpus.deepClone();
			Parameters.trainingCorpus.makePosTagsLexicon();
		}
		initializeProb();
		int cycle = 0;
		double previousLikelihood = -Double.MAX_VALUE, delta = 0;
		do {		
			cycle++;
			boolean[] headVariation = new boolean[]{false};
			double currentLikelihood = emSteps(headVariation);
			delta = currentLikelihood - previousLikelihood;
			previousLikelihood = currentLikelihood;
			String line = "EM cycle: " + cycle + "\tCurrent LogLikeLihood: " + 
				currentLikelihood + "\tDelta LogLikeLihood: " + delta + "\tHead Variation: " + headVariation[0];
			FileUtil.appendReturn(line, Parameters.logFile);
		} while (delta > 0 && delta > Parameters.EM_deltaThreshold && cycle<Parameters.EM_maxCycle);
		if (Parameters.posTagConversion) {
			Parameters.trainingCorpus.unMakePosTagsLexicon(originalTrainingCorpus);
		}
	}
	
	
	public void EMalgorithmIntermediateResults() {
		ConstCorpus originalTrainingCorpus = null;
		if (Parameters.posTagConversion) {
			originalTrainingCorpus = Parameters.trainingCorpus.deepClone();
			Parameters.trainingCorpus.makePosTagsLexicon();
		}
		initializeProb();
		int cycle = 0;
		double previousLikelihood = -Double.MAX_VALUE, delta = 0;
		do {		
			cycle++;
			if (Parameters.posTagConversion && cycle>1) Parameters.trainingCorpus.makePosTagsLexicon();
			boolean[] headVariation = new boolean[]{false};
			double currentLikelihood = emSteps(headVariation);
			delta = currentLikelihood - previousLikelihood;
			previousLikelihood = currentLikelihood;
			String line = "EM cycle: " + cycle + "\tCurrent LogLikeLihood: " + 
				currentLikelihood + "\tDelta LogLikeLihood: " + delta + "\tHead Variation: " + headVariation[0];
			FileUtil.appendReturn(line, Parameters.logFile);
			if (Parameters.posTagConversion) Parameters.trainingCorpus.unMakePosTagsLexicon(originalTrainingCorpus);
			this.readTreesFromCorpus();		
			this.treatTreeBank();			
			this.toPCFG();		
			this.printTrainingCorpusToFile();		
			this.printLexiconAndGrammarFiles();		
			new Parser(this);
		} while (delta > 0 && delta > Parameters.EM_deltaThreshold && cycle<Parameters.EM_maxCycle);		
	}
	
	
	/**
	 * Method to assign the head annotation according to the method getNBestHeadAnnotations()
	 * @param bestDerivationTable
	 * @param index
	 */
	private void assignHeadAnnotation(TSNode inputTree, 
			IdentityHashMap<TSNode, NodeStructure> bestDerivationTable) {		
		NodeStructure bestDerivation = bestDerivationTable.get(inputTree);
		LexTreeStructure bestAnchorDerivation = bestDerivation.getBestLexTree();
		inputTree.markHeadPathToAnchor(bestAnchorDerivation.anchor);
		List<TSNode> subSites = inputTree.collectSubstitutionSites();
		for(TSNode D : subSites) assignHeadAnnotation(D, bestDerivationTable);
	}
	
	private void calculateInsideProbNodes(TSNode inputTree,
			IdentityHashMap<TSNode, NodeStructure> bestDerivationTable) {
		
		List<ArrayList<TSNode>> levelsSubTrees = inputTree.getNodesInDepthLevels();
		for(ArrayList<TSNode> level : levelsSubTrees) {
			for (TSNode TN : level) {
				if (TN.isLexical || TN.isUniqueDaughter()) continue;
				NodeStructure nodeInfos = null;
				if (TN.isPrelexical() || !TN.hasMoreThanNBranching(1)) {					
					Double weight = template_prob.get(TN.toString(false, true));
					if (weight==null) continue;
					double logWeight = Math.log(weight);
					TSNode anchor = TN.getAnchor();
					TN.markHeadPathToAnchor(anchor);
					TSNode  lexicalTree = TN.lexicalizedTreeCopy();
					TN.unmarkHeadPathToAnchor(anchor);
					LexTreeStructure bestAnchorDerivation = new LexTreeStructure(anchor, lexicalTree, logWeight);					
					bestAnchorDerivation.bestDerivationLogProb = logWeight;
					nodeInfos = new NodeStructure();
					nodeInfos.insideLogProb = logWeight;
					nodeInfos.bestRootAnchorLexTree.put(TN.getAnchor(), bestAnchorDerivation);
				}
				else {
					TSNode[] lexicalsTN = TN.collectTerminals().toArray(new TSNode[]{});
					double insideProb = 0;
					for (TSNode anchor  : lexicalsTN) {
						TN.markHeadPathToAnchor(anchor);
						List<TSNode> subSites = TN.collectSubstitutionSites();
						TSNode  lexicalTree = TN.lexicalizedTreeCopy();
						TN.unmarkHeadPathToAnchor(anchor);
						lexicalTree.applyAllConversions();	
						Double weight = template_prob.get(lexicalTree.toString(false, true));
						if (weight==null) continue;
						double logWeight = Math.log(weight);
						LexTreeStructure bestAnchorDerivation = new LexTreeStructure(anchor, lexicalTree, logWeight, subSites);
						bestAnchorDerivation.bestDerivationLogProb = logWeight;
						Double insideLogProbTree = bestAnchorDerivation.getInsideLogProbTree(bestDerivationTable);
						if (insideLogProbTree==null) continue;
						if (nodeInfos==null) nodeInfos = new NodeStructure(); 
						insideProb += Math.exp(insideLogProbTree);						
						nodeInfos.bestRootAnchorLexTree.put(anchor, bestAnchorDerivation);				
					}
					if (nodeInfos!=null) nodeInfos.insideLogProb = Math.log(insideProb);
				}
				if (nodeInfos!=null) bestDerivationTable.put(TN, nodeInfos);
			}
		}			
	}
	
	private void calculateOutsideProbNodes(TSNode inputTree,
			IdentityHashMap<TSNode, NodeStructure> bestDerivationTable) {
		List<TSNode> nodeList = inputTree.collectNonLexicalNodes();
		for(TSNode node : nodeList) {	
			if (node.isUniqueDaughter()) continue;
			NodeStructure nodeInfos = bestDerivationTable.get(node);
			if (nodeInfos==null) continue;
			if (node.isRoot()) {
				nodeInfos.outsideProb = 1;
				continue;
			}			
			TSNode parentNode = node.parent;
			while (parentNode.isUniqueDaughter()) parentNode = parentNode.parent;
			List<TSNode> selectedLexicon = new ArrayList<TSNode>(parentNode.collectLexicalItems());
			selectedLexicon.removeAll(node.collectLexicalItems());			
			List<TSNode> ancestorList = node.collectAncestorNodes();
			for(TSNode ancestor : ancestorList) {
				if (ancestor.isUniqueDaughter()) continue;
				NodeStructure ancestorInfos = bestDerivationTable.get(ancestor);
				if (ancestorInfos==null || ancestorInfos.outsideProb==0.) continue;
				double outsideProbAncestorPart = 0;
				for(TSNode lex : selectedLexicon) {
					LexTreeStructure lexTreeInfo = ancestorInfos.bestRootAnchorLexTree.get(lex);
					if (lexTreeInfo==null) continue;
					Double insideProbTreeExcludingNode = 
						Math.exp(lexTreeInfo.getInsideLogProbTreeExludingSubSite(bestDerivationTable, node));					
					outsideProbAncestorPart += insideProbTreeExcludingNode;
				}
				outsideProbAncestorPart *= ancestorInfos.outsideProb;
				nodeInfos.outsideProb += outsideProbAncestorPart;			
			}
		}		
	}
	
	private void updateNewProbTable(TSNode inputTree, double logLikelihood,
			IdentityHashMap<TSNode, NodeStructure> bestDerivationTable, 
			Hashtable<String, Double> new_template_prob) {
		
		List<TSNode> internalNodes = inputTree.collectNonLexicalNodes();
		for(TSNode node : internalNodes) {
			if (node.isUniqueDaughter()) continue;
			NodeStructure nodeInfos = bestDerivationTable.get(node);
			if (nodeInfos==null || nodeInfos.outsideProb==0) continue;
			double nodeOutsideLogProb = Math.log(nodeInfos.outsideProb);
			for(Map.Entry <TSNode, LexTreeStructure> i : nodeInfos.bestRootAnchorLexTree.entrySet()) {
				LexTreeStructure lexTree = i.getValue();
				double lexTreeInsideLogProb = lexTree.getInsideLogProbTree(bestDerivationTable);
				double lexTreeIncrementCount =  Math.exp(lexTreeInsideLogProb + nodeOutsideLogProb - logLikelihood);
				String lexTreeString = lexTree.lexTreeCopy.toString(false, true);
				Utility.increaseStringDouble(new_template_prob, lexTreeString, lexTreeIncrementCount);
			}
		}
	}

	private double emSteps(boolean[] headVariation) {		
		double likelihood = 0;
		Hashtable<String, Double> new_template_prob = new Hashtable<String, Double>();
		for(TSNode inputTree : Parameters.trainingCorpus.treeBank) {
			TSNode inputTreeCopy = null;
			if (!headVariation[0]) inputTreeCopy = new TSNode(inputTree);
			inputTree.removeHeadAnnotations();
			likelihood += getLikelihoodAndBestAnnotation(inputTree, new_template_prob);
			if (!headVariation[0]) headVariation[0] = !(inputTree.hasSameHeadAnnotation(inputTreeCopy));
			if (inputTree.hasWrongHeadAssignment()) {
				System.err.println("Wrong Head Assignment: " + inputTree.toString(true, true));
			}
		}
		template_prob = new_template_prob;
		normalizeTemplateProb();
		return likelihood;
	}
	
	private double emStepsHeldOutCorpus(ConstCorpus heldOutCorpus) {		
		double likelihood = 0;
		Hashtable<String, Double> new_template_prob = new Hashtable<String, Double>();
		for(TSNode inputTree : heldOutCorpus.treeBank) {
			inputTree.removeHeadAnnotations();
			Double actualLikelihood = getLikelihoodAndBestAnnotation(inputTree, new_template_prob);			
			if (actualLikelihood==null) {
				FileUtil.appendReturn("No coverage for " + inputTree, Parameters.logFile);
				//inputTree.assignRandomHeads();
			}
			else likelihood += actualLikelihood;
			//if (!inputTree.checkHeadConsistency(false)) {
			//	System.err.println("Wrong Head Assignment: " + inputTree.toString(true, true));
			//}
		}
		template_prob = new_template_prob;
		normalizeTemplateProb();
		return likelihood;
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
	private Double getLikelihoodAndBestAnnotation(TSNode inputTree, Hashtable<String, 
			Double> new_template_prob) {
			
		IdentityHashMap<TSNode, NodeStructure> bestDerivationTable = 
			new IdentityHashMap<TSNode, NodeStructure>(); 
		calculateInsideProbNodes(inputTree, bestDerivationTable);
		calculateOutsideProbNodes(inputTree, bestDerivationTable);
		if (bestDerivationTable.get(inputTree)==null) return null;
		Double logLikelihood = bestDerivationTable.get(inputTree).insideLogProb;
		if (logLikelihood!=null) {
			updateNewProbTable(inputTree, logLikelihood, bestDerivationTable, new_template_prob);
			assignHeadAnnotation(inputTree, bestDerivationTable);
		}
		return logLikelihood;
	}
	
	public static void coverage() {
		Parameters.setDefaultParam();		
		Parameters.lengthLimitTraining = 40;
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		Parameters.LTSGtype = "EM"; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";
		LTSG_EM Grammar = new LTSG_EM();
		Grammar.checkEMCoverage();
	}

	public static void EmStandard(String args[]) {
		Parameters.setDefaultParam();		
		
		Parameters.LTSGtype = "EM"; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";

		Parameters.EM_initialization = LTSG_EM.initializeUNIFORM; // initializeUNIFORM initializeDOP
		Parameters.EM_deltaThreshold = 1;
		Parameters.EM_maxCycle = Integer.MAX_VALUE;		
		
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		
		LTSG_EM Grammar = new LTSG_EM();
		//Grammar.reportMaxLexicalDerivations();
		Grammar.EMalgorithm();
		//Grammar.EMHeldOutAlgorithm();
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
	
	public static void EmIntermediateResults() {
		Parameters.setDefaultParam();		
		Parameters.writeGlobalResults = false;
		Parameters.lengthLimitTraining = 10;
		Parameters.lengthLimitTest = 10;
		Parameters.smoothing = false;
		
		Parameters.LTSGtype = "EM"; //GreedyTop GreedyBottom GreedyTopEntropy
		
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";
		
		Parameters.EM_initialization = LTSG_EM.initializeUNIFORM; // initializeUNIFORM initializeDOP
		Parameters.EM_nBest = -1;
		Parameters.EM_deltaThreshold = 0.0001;
		Parameters.EM_maxCycle = 100;
		
		
		Parameters.parserName = Parser.bitPar;
		Parameters.nBest = 1;		
		Parameters.cachingActive = false;
		
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		
		LTSG_EM Grammar = new LTSG_EM();
		Grammar.EMalgorithmIntermediateResults();		
	}
	
	public static void EMHeldOut(String[] args) {
		Parameters.setDefaultParam();		
		
		Parameters.LTSGtype = "EM"; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";

		Parameters.EM_initialization = LTSG_EM.initializeUNIFORM; // initializeUNIFORM initializeDOP
		Parameters.EM_deltaThreshold = 1;
		Parameters.EM_maxCycle = Integer.MAX_VALUE;		
		
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		
		LTSG_EM Grammar = new LTSG_EM();
		//Grammar.reportMaxLexicalDerivations();
		Grammar.EMHeldOutAlgorithm();
		//Grammar.EMHeldOutAlgorithm();
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
		//EMHeldOut();
		EmStandard(args);
		//EmIntermediateResults();
	}

}

class LexTreeStructure {
	TSNode anchor;
	TSNode lexTreeCopy;
	List<TSNode> subSites;
	double lexTreeLogProb;
	Double bestDerivationLogProb;
	Double insideLogProbTree; // P(tree) * prod(insideProb(X)) for all subsite X of tree
	boolean computedInsidedLogProbTree;
	
	public LexTreeStructure(TSNode anchor, TSNode lexTreeCopy, double lexTreeLogProb) {
		this.anchor = anchor;
		this.lexTreeCopy = lexTreeCopy;
		this.lexTreeLogProb = lexTreeLogProb;
		this.subSites = new ArrayList<TSNode>();
		computedInsidedLogProbTree = false;
	}
	
	public LexTreeStructure(TSNode anchor, TSNode lexTreeCopy, double lexTreeLogProb, List<TSNode> subSites) {
		this.anchor = anchor;
		this.lexTreeCopy = lexTreeCopy;
		this.lexTreeLogProb = lexTreeLogProb;
		this.subSites = subSites;
		computedInsidedLogProbTree = false;
	}
	
	public Double getInsideLogProbTree(IdentityHashMap<TSNode, NodeStructure> bestDerivationTable) {
		if (!computedInsidedLogProbTree) {
			insideLogProbTree = lexTreeLogProb;
			bestDerivationLogProb = lexTreeLogProb;
			for(TSNode S : subSites) {
				NodeStructure strucureS = bestDerivationTable.get(S);
				if (strucureS==null) {
					bestDerivationLogProb = null;
					return null;
				}
				bestDerivationLogProb += strucureS.getBestLexTree().bestDerivationLogProb;
				insideLogProbTree += strucureS.insideLogProb;
			}
		}
		computedInsidedLogProbTree = true;
		return insideLogProbTree;
	}
	
	public Double getInsideLogProbTreeExludingSubSite(IdentityHashMap<TSNode, NodeStructure> bestDerivationTable,
			TSNode subSiteToExclude) {
		double result = lexTreeLogProb;
		for(TSNode S : subSites) {
			if (S==subSiteToExclude) continue;
			NodeStructure strucureS = bestDerivationTable.get(S);
			if (strucureS==null) return null;
			result += strucureS.insideLogProb;
		}
		return result;
	}
	
	public String toString() {
		return this.lexTreeCopy.toString(false, true) + ": " + bestDerivationLogProb;
	}
}

class NodeStructure {
	IdentityHashMap<TSNode, LexTreeStructure> bestRootAnchorLexTree;
	double insideLogProb;
	double outsideProb;
	private LexTreeStructure bestLexTree;
	
	public NodeStructure() {
		bestRootAnchorLexTree = new IdentityHashMap<TSNode, LexTreeStructure>();
	}
	
	public LexTreeStructure getBestLexTree() {
		if (bestLexTree==null) computeBestLexDerivation();
		return bestLexTree;
	}
	
	public void computeBestLexDerivation() {
		for(Map.Entry<TSNode, LexTreeStructure> i : bestRootAnchorLexTree.entrySet()) {			
			LexTreeStructure value = i.getValue();			
			if (bestLexTree==null || value.bestDerivationLogProb > bestLexTree.bestDerivationLogProb) {
				bestLexTree = value;
			}
		}
	}
}