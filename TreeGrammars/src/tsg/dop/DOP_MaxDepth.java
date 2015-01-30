package tsg.dop;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.CFSG;
import tsg.TSNode;
import tsg.corpora.*;
import tsg.parser.Parser;
import util.*;
import util.file.FileUtil;


public class DOP_MaxDepth extends CFSG<Integer>{
	
	Hashtable<String, Integer> Etrees;	
	
	public DOP_MaxDepth() {		
		super();			
		Etrees = new Hashtable<String, Integer>();
	}
	
	public void freeMemory() {
		Etrees.clear();
	}
	
	private void readTreesFromCorpus() {		
		int count = 1;
		PrintProgressStatic.start("Reading trees from sentence:");
		for(TSNode TreeLine : Parameters.trainingCorpus.treeBank) {
			List<String> subTrees = TreeLine.allSubTrees(Parameters.maxDepth);			
			for (String etree : subTrees ) {
				Utility.increaseStringInteger(Etrees, etree, 1);
			}			
			PrintProgressStatic.next();
			count++;
		}
		PrintProgressStatic.end();
		String log = "Read trees from corups. \n # Trees: " + Etrees.size();
		FileUtil.appendReturn(log, Parameters.logFile);				
	}
	
	private Hashtable[] sortTreesAndFilter() {
		Hashtable[] ETreesDepth = new Hashtable[Parameters.maxDepth];
		for(int d=0; d<Parameters.maxDepth; d++) ETreesDepth[d] = new Hashtable();
		for (Enumeration e = Etrees.keys(); e.hasMoreElements() ;) {
			String eTree = (String)e.nextElement();			
			Object count = Etrees.get(eTree);
			TSNode TN = new TSNode(eTree, false);
			int bucket = TN.maxDepth(false)-1;
			ETreesDepth[bucket].put(eTree, count);
		}
		String log = "Sorted trees. \n";
		for(int d=0; d<Parameters.maxDepth; d++) {
			log += "\t#trees of depth " + (d+1) + ": " +  ETreesDepth[d].size() + "\n";
		}
		String[][] TreeArrayDepth = new String[Parameters.maxDepth-1][];
		for(int d=1; d<Parameters.maxDepth; d++) {
			TreeArrayDepth[d-1] = (String[])ETreesDepth[d].keySet().toArray(new String[]{});
			int length = TreeArrayDepth[d-1].length;
			while (ETreesDepth[d].size()>Parameters.maxTreesInDepth) {
				int randomIndex = Utility.randomInteger(length);
				String treeToRemove = TreeArrayDepth[d-1][randomIndex];
				ETreesDepth[d].remove(treeToRemove);
			}
		}		
		log += "Each set of tree of depth d>1 are randomly reduced to " + Parameters.maxTreesInDepth;
		FileUtil.appendReturn(log, Parameters.logFile);
		return ETreesDepth;
	}
	
	private void toPCFG() {
		Hashtable[] ETreesDepth = sortTreesAndFilter();
		int uniqueLableIndex = 0;
		for(int d=0; d<Parameters.maxDepth; d++) {
			for (Enumeration e = ETreesDepth[d].keys(); e.hasMoreElements() ;) {
				String eTree = (String)e.nextElement();			
				Integer count = (Integer)ETreesDepth[d].get(eTree);
				TSNode TN = new TSNode(eTree, false);
				TN.toNormalForm();
				uniqueLableIndex = TN.toUniqueInternalLabels(false, uniqueLableIndex, false);
				List<TSNode> nonLexicalNodes = TN.collectNonTerminalNodes();
				for(TSNode nonTerminal : nonLexicalNodes) {
					boolean prelexical = nonTerminal.isPrelexical();
					Hashtable<String, Integer> toAdd = (prelexical) ? lexRules : internalRules;					
					String rule = nonTerminal.toCFG(false);
					Utility.increaseStringInteger(toAdd, rule, 1);
				}
			}
		}
		String log = "Converted trees to PCFG. \n # Internal Rules: " + internalRules.size() 
					+ "\n # Lex Rules: " + lexRules.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	
	public static void main(String args[]) {
		Parameters.setDefaultParam();				
		
		Parameters.semanticTags = false;
		Parameters.ukLimit = 1;
		Parameters.lengthLimitTraining = 10;
		Parameters.lengthLimitTest = 10;
		Parameters.smoothing = false;
		Wsj.transformNPbasal = false;
		
		Parameters.maxDepth = 2;
		Parameters.maxTreesInDepth = 50000;
		
		Parameters.parserName = Parser.fedePar;
		Parameters.nBest = 100;
		Parameters.cachingActive = false;				
		
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/DOP_MaxDepth/Depth" + Parameters.maxDepth + "/";
		File path = new File(Parameters.outputPath);
		path.mkdir();
		
		DOP_MaxDepth Grammar = new DOP_MaxDepth();
		Grammar.readTreesFromCorpus();					
		Grammar.toPCFG();
		Grammar.freeMemory();

		Grammar.printLexiconAndGrammarFiles();		
		new Parser(Grammar);
	}
	
}
