package tsg.LTSG;

import java.util.*;
import java.io.*;

import settings.Parameters;
import tsg.TSNode;
import tsg.corpora.*;
import tsg.parser.Parser;
import util.*;
import util.file.FileUtil;

public class LTSG_Greedy extends LTSG{
	
	public final static String GreedyTop = "GreedyTop";
	public final static String GreedyBottom = "GreedyBottom";
	
	private static final boolean traceAmbiguity = true;
	private int totalHeadChoices, ambiguousHeadChoices;
	private int secondTotalHeadChoices;
	private File ambiguityReportFile, ambiguityCategoryFile;
	private PrintWriter ambiguityWriter;
	private Hashtable<String,Integer> ambiguityCategory; 
	
	
	public LTSG_Greedy() {
		super();
		if (traceAmbiguity) {
			ambiguityReportFile = new File(Parameters.outputPath + "AmbiguityReport.txt");
			ambiguityCategoryFile = new File(Parameters.outputPath + "AmbiguityCategory.txt");
			ambiguityWriter = FileUtil.getPrintWriter(ambiguityReportFile);
			ambiguityCategory = new Hashtable<String,Integer>();
		}	
	}
	
	public void assignGreedyAnnotations() {
		template_freq.clear();
		lexicon_freq.clear();
		Parameters.trainingCorpus.removeHeadAnnotations();
		/*Corpus originalTrainingCorpus = null;
		if (Parameters.posTagConversion) {
			originalTrainingCorpus = Parameters.trainingCorpus.deepClone();
			Parameters.trainingCorpus.makePosTagsLexicon();
		}*/
		this.extractAllLexTrees();	
		//printElementaryTreeStatistics(new int[]{1031});
		int i = 0;
		PrintProgressStatic.start("Assigning heads to sentence:");
		for(TSNode tree : Parameters.trainingCorpus.treeBank) {
			i++;
			PrintProgressStatic.next();
			if (Parameters.LTSGtype.equals(GreedyTop)) {
				assignBestHeadsGreedyTop(tree);
			}
			else if (Parameters.LTSGtype.equals(GreedyBottom)) {
				assignBestHeadsGreedyBottom(tree);
			}
		}
		PrintProgressStatic.end();
		/*if (Parameters.posTagConversion) {
			Parameters.trainingCorpus.unMakePosTagsLexicon(originalTrainingCorpus);
		}*/
		if (traceAmbiguity) printAmbiguityReport();
		 		
	}
	
	public void printAmbiguityReport() {
		ambiguityWriter.close();
		float aR = (float)ambiguousHeadChoices/totalHeadChoices;
		String ambiguityReport = "Ambiguity: " + ambiguousHeadChoices + "/" + totalHeadChoices + " -> " + aR;
		FileUtil.appendReturn(ambiguityReport, Parameters.logFile);
		FileUtil.printHashtableToFile(ambiguityCategory, ambiguityCategoryFile);
	}
	

	/**
	 * Perform the greedy algorithm Bottom-Up to determine the assignment of head markers.
	 * @param template_freq the Hashtable<String, Integer> conaining the frequencies of all the 
	 * lexical trees in the treebank
	 * @param punctuation if to allow non terminals with punctuation symbols .,:;'`?!() to be
	 * marked as heads
	 * @param random if true when more than one non terminals at a specific level is an optimal 
	 * head choose one at random, if false choose the first to the left
	 * @param conversion if true the lexical trees are reduced to lexical path (without substitution sites). 
	 * In this case the template_freq should also contain lexical tree templates in this format.
	 */
	public void assignBestHeadsGreedyBottom(TSNode tree) {
		ArrayList<ArrayList<TSNode>> nodesInLevels = tree.getNodesInDepthLevels();	
		for(ArrayList<TSNode> levelList : nodesInLevels) {			
			for (TSNode TN : levelList) {				
				if (TN.isTerminal()) continue;				
				if (TN.daughters.length==1) {
					if (!TN.isPrelexical()) TN.daughters[0].headMarked = true; 
					continue;
				}
				int maxWeight = 0;
				List<TSNode> maxDaughters = new ArrayList<TSNode>();
				LinkedList<String> maxLeavesOldLex = new LinkedList<String>();
				String cat = "";
				for(TSNode D : TN.daughters) {
					D.headMarked = true;
					TSNode TNcopy = new TSNode(TN);
					D.headMarked = false;
					TNcopy.toLexicalizeFrame();
					TSNode anchor = TNcopy.getAnchor();
					String oldLex = anchor.label;
					if (cat.equals("")) {
						TSNode catNode = anchor.parent.parent;
						cat = (catNode==null) ? null : catNode.label;
					}
					TNcopy.applyAllConversions();	
					TSNode leaf = TNcopy.getAnchor();
					if (!Parameters.greedy_punctuation && leaf.isPunctuation()) continue;
					int tree_weight = template_freq.get(TNcopy.toString(false, true)); 					
					if (tree_weight<maxWeight) continue;
					if (tree_weight>maxWeight) {
						maxWeight = tree_weight;
						maxDaughters.clear();
						maxLeavesOldLex.clear();
					}
					maxDaughters.add(D); // if tree_weight>=maxWeight
					maxLeavesOldLex.add(oldLex);
				}	
				TSNode bestDaughter = null;
				if (!Parameters.greedy_punctuation && maxDaughters.isEmpty()) { //has only punctuation leaves
					bestDaughter = getAmbiguityChoice(Arrays.asList(TN.daughters), null, TN, "punct", null);
				}
				else bestDaughter = getAmbiguityChoice(maxDaughters, maxLeavesOldLex, TN, ""+maxWeight, cat);		
				if (bestDaughter!=null) bestDaughter.headMarked = true;
			}
		}
	}

	/**
	 * Perform the greedy algorithm Top-Down to determine the assignment of head markers.
	 * @param template_freq the Hashtable<String, Integer> conaining the frequencies of all the 
	 * lexical trees in the treebank
	 * @param punctuation if to allow non terminals with punctuation symbols .,:;'`?!() to be
	 * marked as heads
	 * @param random if true when more than one non terminals at a specific level is an optimal 
	 * head choose one at random, if false choose the first to the left
	 * @param conversion if true the lexical trees are reduced to lexical path (without substitution sites). 
	 * In this case the template_freq should also contain lexical tree templates in this format.
	 */
	public void assignBestHeadsGreedyTop(TSNode tree) {		
		List<TSNode> anchors = tree.collectLexicalItems();
		int maxWeight = 0;
		LinkedList<TSNode> maxLeaves = new LinkedList<TSNode>();
		LinkedList<String> ambiguousTrees = new LinkedList<String>();
		TSNode bestLeaf = null;
		if (!Parameters.greedy_punctuation && TSNode.areAllPunctuationLabels(anchors)) {
			bestLeaf = getAmbiguityChoice(anchors, null, tree, "punct", null);
		}	
		else {
			String cat = "";
			for(TSNode leaf : anchors) {
				if (!Parameters.greedy_punctuation && leaf.isPunctuation()) continue;
				TSNode eTree = tree.lexicalizedTreeToAnchor(leaf);
				String oldLex = leaf.label;
				if (cat.equals("")) {
					TSNode catNode = leaf.parent.parent;
					cat = (catNode==null) ? null : catNode.label;
				}
				eTree.applyAllConversions();
				int tree_weight = template_freq.get(eTree.toString(false, true));			
				if (tree_weight<maxWeight) continue;
				if (tree_weight>maxWeight) {
					maxWeight = tree_weight;
					maxLeaves.clear();
					ambiguousTrees.clear();
				}
				maxLeaves.add(leaf); // if tree_weight>=maxWeight
				ambiguousTrees.add(oldLex);
			}
			bestLeaf = getAmbiguityChoice(maxLeaves, ambiguousTrees, tree, ""+maxWeight, cat);
		}												
		TSNode up = bestLeaf.parent;
		while(up!=tree.parent) {
			if (up!=tree) up.headMarked = true;			
			int prole = up.daughters.length;
			if (prole>1) {
				for(int d=0; d<prole; d++) {
					TSNode sister = up.daughters[d];
					if (sister!=bestLeaf) assignBestHeadsGreedyTop(sister);					
				}
			}			
			bestLeaf = up;
			up = up.parent;			
		} 		
	}
	
	private TSNode getAmbiguityChoice(List<TSNode> list, List<String> oldLex, 
			TSNode tree, String label, String category) {
		if (traceAmbiguity) {
			totalHeadChoices++;
			if (list.size()>1) {
				if (category==null) {
					category = "nullCat";
					String worning = "null category found in tree: " + tree;
					System.out.println(worning);
					FileUtil.appendReturn(worning, Parameters.logFile);
				}
				ambiguityWriter.println(label + "\t" + oldLex + "\t" + tree.toString(false,true));
				Utility.increaseStringInteger(ambiguityCategory, category, 1);
				ambiguousHeadChoices++;			
			}
		}		
		if (list.size()==1) return list.get(0);
		switch(Parameters.greedy_ambiguityChoice) {
			case 0: return list.get(Utility.randomInteger(list.size()));
			case 1: return list.get(0);
			case 2: return list.get(list.size()-1);
			case 3:
				secondTotalHeadChoices++;
				int maxWeight = -1;
				TSNode maxLeaf = null;
				int index = -1;				
				for(String lex : oldLex) {
					index++;
					int tree_weight = Parameters.lexiconTable.get(lex);
					if (tree_weight<maxWeight) continue;					
					else {						
						maxWeight = tree_weight;
						maxLeaf = list.get(index);
					}										
				}				
				return maxLeaf;
		}
		return null;
	}
	

	
	public static void main(String args[]) {
		Parameters.setDefaultParam();				
		
		Parameters.lengthLimitTraining = 40;
		Parameters.lengthLimitTest = 40;
		//Parameters.removeTreesLimit = 1;
		
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop; //GreedyTop GreedyBottom
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 1; //"random", "left", "right", "backoff to lexicon"
		
		Parameters.outputPath = Parameters.resultsPath + "TSG/LTSG/" + Parameters.LTSGtype + "/";
		
		Parameters.spineConversion = false;
		Parameters.posTagConversion = false;
		Parameters.jollyConversion = false;
		Parameters.jollyLabels = new String[]{}; //new String[]{"ADVP","ADJP"};
		Arrays.sort(Parameters.jollyLabels);
		
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Grammar.assignGreedyAnnotations();						
		Grammar.readTreesFromCorpus();
		
		Grammar.printTemplatesToFile();
		Grammar.treatTreeBank();			
		Grammar.toPCFG();
		
		Grammar.printLexiconAndGrammarFiles();		
		
		new Parser(Grammar);
	}
	
}
