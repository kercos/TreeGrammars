package tsg.LTSG;

import java.util.*;

import settings.Parameters;
import tsg.TSNode;
import tsg.corpora.ConstCorpus;



public class lexicalTreeParser {
	ConstCorpus testSet;
	LTSG grammar;
	Hashtable<String, Hashtable<String, HashSet<String>>> treeStructure; // String --> String --> HashSet 
	
	public lexicalTreeParser(ConstCorpus testSet, LTSG grammar) {
		this.testSet = testSet;
		this.grammar = grammar;	
		buildTreeStructure();
		parseTestSet();
		
	}
	
	public void buildTreeStructure() {
		treeStructure = new Hashtable<String, Hashtable<String, HashSet<String>>>();
		for (Enumeration<String> e = grammar.template_freq.keys(); e.hasMoreElements() ;) {
			String template = (String)e.nextElement();
			String lex = TSNode.get_unique_lexicon(template);
			String root = TSNode.get_unique_root(template);
			Hashtable<String, HashSet<String>> rootStructure = treeStructure.get(lex);
			if (rootStructure==null) {
				rootStructure = new Hashtable<String, HashSet<String>>();
				treeStructure.put(lex, rootStructure);
			}
			HashSet<String> treeSet = rootStructure.get(root);
			if (treeSet==null) {
				treeSet = new HashSet<String>();				
				rootStructure.put(root, treeSet);
			}
			treeSet.add(template);
		}
	}
	
	public void parseTestSet() {
		for(ListIterator<TSNode> i = testSet.treeBank.listIterator(); i.hasNext(); ) {
			TSNode element = (TSNode)i.next();
			//element.removeQuotationsInLexicon();
			List<TSNode> terminals = element.collectTerminals();
			parseSentence(terminals);
		}
	}
	
	public HashSet<TSNode> parseSentence(List<TSNode> terminals) {
		HashSet<TSNode> parseTrees = new HashSet<TSNode>();
		for(ListIterator<TSNode> j = terminals.listIterator(); j.hasNext(); ) {
			TSNode tree = (TSNode)j.next();
			Hashtable<String, HashSet<String>> rootStructure = treeStructure.get(tree.label);
			if (rootStructure==null) continue;
			HashSet<String> rootTrees = rootStructure.get("TOP");
			if (rootTrees==null) continue;
			
		}
		return parseTrees;
	}
	
	public static void ltsg_all() {
		String corpusName = "Wsj"; //Negra, Parc, Tiger, Wsj
		
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/AllLexTrees/";
		Parameters.ukLimit = 4;
		Parameters.semanticTags = true;		
		//Parameters.quotations = false;
		//Parameters.anonimizeLex = false;		
		
		Parameters.cutTopRecursion = false;		
		Parameters.delexicalize = false;
		Parameters.spineConversion = false;
		Parameters.removeTreesLimit = -1;
		Parameters.smoothing = false;
		Parameters.smoothingFactor = 100;
		
		int[] LL = new int[] {20}; // 5,10,15,20,25,30,35,40,100
		for(int i=0; i<LL.length; i++) {
			Parameters.lengthLimitTraining = LL[i];						
			LTSG Grammar = new LTSG_All();			
			Grammar.extractAllLexTrees();
			//new lexicalTreeParser(Parameters.evalSet, Grammar);			
		}
	}
	
	public static void main(String args[]) {
		ltsg_all();
	}
}
