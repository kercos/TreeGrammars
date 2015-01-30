package tsg.dop;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import settings.Parameters;
import tsg.*;

import tsg.TSNode;
import tsg.corpora.*;
import tsg.parser.Parser;
import util.*;

public class DOP_Coverage {
	
	//public static String[] jollyCat = new String[]{}; //new String[]{",",":","-LRB-","-RRB-","JJ","ADVP","ADJP","SBAR", "RB"};
	
	DOP_MaxDepth grammar;
	Hashtable<String, Integer> internalUncovered;
	TreeSet<String> uncoveredRulesSet;
	
	public DOP_Coverage(DOP_MaxDepth grammar) {
		this.grammar = grammar;
		internalUncovered = new Hashtable<String, Integer>();
		uncoveredRulesSet = new TreeSet<String>();
		//Arrays.sort(jollyCat);
		//removeJollyCat();
	}
	
	
	public void removeJollyCat() {
		//if (jollyCat.length==0) return;
		Hashtable<String, Integer> new_templates = new Hashtable<String, Integer>();		
		for(String eTree : grammar.Etrees.keySet()) {
			TSNode TN = new TSNode(eTree, false);
			Integer count = grammar.Etrees.get(eTree);
			//TN.removeSubstitutionSitesInLexTree(jollyCat);
			new_templates.put(TN.toString(false, true), count);
		}
		//grammar.template_freq = new_templates;
	}
	
	/*public void checkCoverageOnSet() {
		File coverageReport = new File(Parameters.outputPath + "CoverageReport");
		File uncoveredSentences = new File(Parameters.outputPath + "UncoveredSentences");
		File uncoveredRules = new File(Parameters.outputPath + "UncoveredRules");
		File coveredExtrWords = new File(Parameters.outputPath + "covered_ExtrWords");
		File coveredGold = new File(Parameters.outputPath + "covered_Gold");
		int[] coverage_statistics = new int[8];		
		try {
			PrintWriter outputCoverage= new PrintWriter(coverageReport, "ISO-8859-1");
			PrintWriter outputUncovSent= new PrintWriter(uncoveredSentences, "ISO-8859-1");
			PrintWriter outputUncovRules= new PrintWriter(uncoveredRules, "ISO-8859-1");
			PrintWriter outputExtrWord = new PrintWriter(coveredExtrWords, "ISO-8859-1");			
			PrintWriter outputGold = new PrintWriter(coveredGold, "ISO-8859-1");
			for(TreeNode evalTree : Parameters.evalCorpus.treeBank) {		
				List<TreeNode> terminals = evalTree.collectTerminals();
				List<ArrayList<TreeNode>> goldTrees = LTSG.allLexTreesForEachAnchor(evalTree, false, terminals);
				IdentityHashMap<TreeNode,TreeSet<Integer>> markTable = new IdentityHashMap<TreeNode,TreeSet<Integer>>();
				int terminalIndex = 0;
				for(ArrayList<TreeNode> terminalTrees : goldTrees) {
					TreeNode terminal = terminals.get(terminalIndex);
					String word = terminal.toString();
					boolean unknown = grammar.lexicon_freq.get(word)==null;
					if (unknown) continue;	
					Utility.putTreeNodeMark(markTable, terminal, terminalIndex);
					for(TreeNode goldTemplate : terminalTrees) {
						if (Parameters.spineConversion) goldTemplate.convertToSpine();
						else goldTemplate.removeSubstitutionSitesInLexTree(jollyCat);						
						String goldTemplateString = goldTemplate.toString(false, true);
						goldTemplate = new TreeNode(goldTemplateString, false);						
						if (grammar.template_freq.keySet().contains(goldTemplateString)) {	
							int depth = goldTemplate.maxDepth(false);					
							Utility.putTreeNodeMark(markTable, terminal, terminalIndex);
							for(int i=0; i<depth; i++) {
								terminal = terminal.parent;
								Utility.putTreeNodeMark(markTable, terminal, terminalIndex);
							}		
							break;
						}
					}
					terminalIndex++;
				}
				int temp = coverage_statistics[0];
				boolean covered = checkCoverageOnTree(evalTree, markTable, coverage_statistics);
				//evalTree.removeQuotationsInLexicon();
				if (coverage_statistics[0]==temp) { // correct coverage
					outputExtrWord.write(evalTree.toExtractWord() + "\n");
					outputGold.write(evalTree.toString() + "\n");
				}
				//evalTree.addQuotationsInLexicon();
				evalTree.addMarkersInTree(markTable);
				//evalTree.removeQuotationsInLexicon();				
				outputCoverage.write(evalTree.toString() + "\n");
				if (!covered) outputUncovSent.write(evalTree.toString() + "\n");
			}
			for(String uncRule : uncoveredRulesSet) {
				outputUncovRules.println(uncRule);
			}
			outputUncovSent.close();
			outputUncovRules.close();
			outputCoverage.close();
			outputExtrWord.close();
			outputGold.close();
		} catch (IOException e) {FileUtil.handleExceptions(e);}
		String report = "Coverage report: \n";
		report += 	"\tTOTAL SENTENCES " + Parameters.evalCorpus.size() + "\n" +
					"\tSentences Uncovered " + coverage_statistics[0] + "\n" +
					"\tNodes Uncovered " + coverage_statistics[1] + "\n" +
					"\tTop Nodes Uncovered " + coverage_statistics[2] + "\n" +
					"\tFirst level Nodes Uncovered " + coverage_statistics[3] + "\n" +
					"\tLexical Uncovered " + coverage_statistics[4] + "\n" +
					"\tPrelex Uncovered " + coverage_statistics[5] +
					"(of which referred to Lexical Uncovered " + coverage_statistics[6] + ")\n" +
					"\tInternal Uncovered " + coverage_statistics[7] + "\n";
		report += "\nINTERNAL LABELS UNCOVERED \n";
		for(Enumeration<String> e = internalUncovered.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Integer count = (Integer)internalUncovered.get(key);
			report += "\t" + key + "\t" + count + "\n";			
		}
		FileUtil.append(report, coverageReport);		
	}*/



	/*public boolean checkCoverageOnTree(TreeNode evalTree, IdentityHashMap<TreeNode,TreeSet<Integer>> markTable, 
										int[] statistics) {
		List<TreeNode> nodes = evalTree.collectAllNodes();		
		boolean uncovered = false; // current uncovered nodes
		for(ListIterator<TreeNode> i = nodes.listIterator(); i.hasNext();) {
			TreeNode node = i.next();
			TreeSet<Integer> indexesInNode = markTable.get(node);
			if (indexesInNode!=null) {
				LinkedList<Integer> dominatedLeavesIndex = new LinkedList<Integer>();
				if (!node.isTerminal()) {
					List<TreeNode> leaves = node.collectTerminals();
					for(ListIterator<TreeNode> l = leaves.listIterator(); l.hasNext(); ) {
						TreeNode terminal = l.next();
						TreeSet<Integer> indexTerminal = markTable.get(terminal);
						if (indexTerminal!=null) dominatedLeavesIndex.addAll(indexTerminal);
					}
					indexesInNode.retainAll(dominatedLeavesIndex);
				}
			}
			boolean isJollyCat = Arrays.binarySearch(jollyCat, node.label) >= 0; 
			if ((indexesInNode==null || indexesInNode.isEmpty()) && !isJollyCat) { //hole
				uncovered = true;
				uncoveredRulesSet.add(node.toCFG(true));
				statistics[1]++;
				if (node.parent==null) statistics[2]++;
				else if (node.isTerminal()) statistics[4]++;								
				else {										
					if (node.hight()==1) statistics[3]++;
					else if (node.isPreterminal()) {
						statistics[5]++;
						if (!markTable.containsKey(node.daughters[0])) statistics[6]++;
					}
					else {
						statistics[7]++;
						Utility.increaseStringInteger(internalUncovered, node.label, 1);
					}
				}
			}
		}
		if (uncovered) statistics[0]++;
		return !uncovered;
	}*/
	
	public static void main(String args[]) {
		Parameters.setDefaultParam();
		
		Wsj.transformNPbasal = false;
		Parameters.lengthLimitTraining = 40;
		Parameters.lengthLimitTest = 40;
		Parameters.semanticTags = false;		
		Parameters.ukLimit = 20;
		Wsj.testSet = "22"; //00 01 22 23 24
		Parameters.spineConversion = true;
		
		Parameters.LTSGtype = "AllLexTrees";
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";
		
		tsg.LTSG.LTSG_All Grammar = new tsg.LTSG.LTSG_All();
		Grammar.extractAllLexTrees();
		Grammar.treatTreeBank();			
		
		//Grammar.printTemplatesToFile();
		//Grammar.printLexiconAndGrammarFiles();
		Grammar.checkNegativeFrequencies();
		
		//DOP_Coverage cov = new DOP_Coverage(Grammar);
		//cov.checkCoverageOnSet();
	}
}
