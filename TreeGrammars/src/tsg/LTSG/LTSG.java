package tsg.LTSG;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.CFSG;
import tsg.TreeAnalizer;
import tsg.TSNode;
import tsg.corpora.*;
import util.*;
import util.file.FileUtil;


public abstract class LTSG extends CFSG<Integer>{
	
	Hashtable<String, Integer> template_freq;
	Hashtable<String, Integer> lexicon_freq;
	
	public LTSG() {
		super();
		template_freq = new Hashtable<String, Integer>();
		lexicon_freq = new Hashtable<String, Integer>();
	}
	
	public void readTreesFromFile(File templateFile) {
		template_freq.clear();
		lexicon_freq.clear();
		Scanner scan = FileUtil.getScanner(templateFile);
		while(scan.hasNextLine()) {
			Integer freq = scan.nextInt();
			String tree = scan.nextLine().trim();			
			template_freq.put(tree, freq);
			Utility.increaseStringInteger(lexicon_freq, TSNode.get_unique_lexicon(tree), freq);
		}
	}
	
	public void readTreesFromCorpus() {
		readTreesFromCorpus(false);
	}
	
	public void readTreesFromCorpus(boolean applySpineConversion) {
		Parameters.trainingCorpus.correctHeadAnnotation();		
		template_freq.clear();
		lexicon_freq.clear();
		for(TSNode TreeLine : Parameters.trainingCorpus.treeBank) {
			List<TSNode> trees = TreeLine.lexicalizedTreesFromHeadAnnotation();			
			for (TSNode LT : trees) {		
				if (applySpineConversion) LT.convertToSpine();
				TSNode lexAnchor = LT.getAnchor();				
				Utility.increaseStringInteger(template_freq, LT.toString(false, true), 1);
				Utility.increaseStringInteger(lexicon_freq, lexAnchor.label(), 1);
			}
		}
		String log = "Read trees from corups. # Trees: " + template_freq.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	public void treatTreeBank() {
		this.entropy();
		if (Parameters.delexicalize) this.delexicalize();
		if (Parameters.cutTopRecursion) this.cutTopRecursion();
		//if (Parameters.removeTreesLimit>0) this.removeTrees();
	}

	public void delexicalize() {
		Hashtable<String,Integer> new_template_freq = new Hashtable<String,Integer>();
		for (Enumeration<String> e = template_freq.keys(); e.hasMoreElements() ;) {
			String template = e.nextElement();
			Integer count = template_freq.get(template);
			TSNode TN = new TSNode(template, false);
			//TreeNode TNroot = TN.root();
			//if (TNroot.label().equals("TOP") || TNroot.label().equals("S"));
			if (TN.isPrelexical()) {
				template = TN.toString(false, true);				
				Utility.increaseStringInteger(new_template_freq, template, count);
			}
			else {				
				String[] delexTree_lexProd = TN.splitUniqueLexProduction();				
				Utility.increaseStringInteger(new_template_freq, delexTree_lexProd[0], count);
				Utility.increaseStringInteger(new_template_freq, delexTree_lexProd[1], count);
			}
		}
		template_freq = new_template_freq;
		FileUtil.appendReturn("Delexicalized trees", Parameters.logFile);
	}
	
	public void cutTopRecursion() {
		PrintWriter cutTreePW = FileUtil.getPrintWriter(new File(Parameters.outputPath + "cutTopTrees"));
		Hashtable<String,Integer> new_template_freq = new Hashtable<String,Integer>();
		int cutRecursionCounter = 0;
		for (Enumeration<String> e = template_freq.keys(); e.hasMoreElements() ;) {
			String template = e.nextElement();
			Integer count = template_freq.get(template);
			TSNode TN = new TSNode(template, false);			
			TSNode[] split = TN.cutRecursiveOld();
			if (split!=null) {
				cutRecursionCounter++;
				cutTreePW.println(TN.toString(false, true));
				for(TSNode TN_split : split) {
					cutTreePW.println(TN_split.toString(false, true));
					Utility.increaseStringInteger(new_template_freq, TN_split.toString(false, true), count);
				}
				cutTreePW.println();
			}
			else Utility.increaseStringInteger(new_template_freq, template, count);
		}
		cutTreePW.close();
		String log = "Cutted top recursion in trees (" + cutRecursionCounter + "trees cutted)";
		if (Parameters.cutTopRecursion) FileUtil.appendReturn(log, Parameters.logFile);
		template_freq = new_template_freq;
	}
	
	public void removeTrees() {
		FileUtil.appendReturn("REMOVING TREES occuring <= " + Parameters.removeTreesLimit, Parameters.logFile);
		for (Enumeration<String> e = template_freq.keys(); e.hasMoreElements() ;) {
			String template = e.nextElement();
			String lex = TSNode.get_unique_lexicon(template);
			Integer count = (Integer)(template_freq.get(template));
			if (count>Parameters.removeTreesLimit) continue;
			template_freq.remove(template);
			Utility.decreaseStringInteger(lexicon_freq, lex, count);
		}
		String log = "# Trees after removing trees occuring <= " + 
						Parameters.removeTreesLimit + " : " + template_freq.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}

	public void toPCFG() {
		lexRules.clear();
		internalRules.clear();
		int uniqueLableIndex = 1;
		for (Enumeration<String> e = template_freq.keys(); e.hasMoreElements() ;) {
			String eTree = e.nextElement();			
			Integer count = template_freq.get(eTree);
			TSNode TN = new TSNode(eTree, false);
			TN.toNormalForm();
			uniqueLableIndex = TN.toUniqueInternalLabels(false, uniqueLableIndex, false);
			List<TSNode> nonTerminals = TN.collectNonTerminalNodes();
			for (TSNode nonTerminal : nonTerminals) {
				String rule = nonTerminal.toCFG(false);
				Hashtable<String, Integer> toAdd = (nonTerminal.isPrelexical()) ? lexRules : internalRules;
				if (count > Parameters.removeTreesLimit) {					
					Utility.increaseStringInteger(toAdd, rule, count * Parameters.smoothingFactor);
				}									
				if (Parameters.smoothing) {
					rule = rule.replaceAll("@\\d+", "");
					Utility.increaseStringInteger(toAdd, rule, 1);
				}
			}
		}
		
		String log = "Converted trees to PCFG (smoothing = " + Parameters.smoothing + ")"
						+ "\n\t# Internal Rules: " + internalRules.size() 
						+ "\n\t# Lex Rules: " + lexRules.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}

	/**
	*  The entropy of the grammar is returned as output where each stochastic
	*  variable instance is template_freq/lex_freq.
	*/
	public double entropy() {
		double entropy = 0;
		for (Enumeration<String> e = template_freq.keys(); e.hasMoreElements() ;) {
			String template = e.nextElement();
			entropy += entropy(template, template_freq, lexicon_freq);
		}
		FileUtil.appendReturn("Entropy of the extracted trees:" + -entropy, Parameters.logFile);
		return -entropy;
	}
	
	public static double entropy(String template, Hashtable<String,Integer> template_freq, Hashtable<String, Integer> lexicon_freq) {
		String lex = TSNode.get_unique_lexicon(template);
		double tree_freq = (Integer)(template_freq.get(template));
		double lex_freq = (Integer)lexicon_freq.get(lex);
		double prob = tree_freq/lex_freq;
		return Utility.pLogp(prob); //old (without freq)
		//return lex_freq * Utility.pLogp(prob);
	}
	
	/**
	*  The entropy of the grammar is returned as output where each stochastic
	*  variable instance is template_freq/lex_freq.
	*/
	public static double entropy(Hashtable<String,Integer> template_freq, Hashtable<String, Integer> lexicon_freq) {
		double entropy = 0;
		for (Enumeration<String> e = template_freq.keys(); e.hasMoreElements() ;) {
			String template = e.nextElement();
			entropy += entropy(template, template_freq, lexicon_freq);
		}
		return -entropy;
	}
	
	public static double filteredEntropyGreedy(String template, Hashtable<String,Integer> template_freq) {
		Hashtable<String,Integer> filtered_template_freq =  new Hashtable<String,Integer>();
		Hashtable<String, Integer> filtered_lexicon_freq = new Hashtable<String,Integer>();
		String template_root = TSNode.get_unique_root(template);
		String template_lex = TSNode.get_unique_lexicon(template);
		for (Enumeration<String> e = template_freq.keys(); e.hasMoreElements() ;) {
			String tree = e.nextElement();
			String tree_root = TSNode.get_unique_root(tree);
			String tree_lex = TSNode.get_unique_lexicon(tree);
			if ( tree_root.equals(template_root) && tree_lex.equals(template_lex) ) {
				Integer count = template_freq.get(tree);
				filtered_template_freq.put(tree, count);
				Utility.increaseStringInteger(filtered_lexicon_freq, tree_lex, count);
			}
		}
		return entropy(filtered_template_freq, filtered_lexicon_freq);
	}
	
	public void extractAllLexTrees() {
		Parameters.trainingCorpus.removeHeadAnnotations();
		for(TSNode inputTree : Parameters.trainingCorpus.treeBank) {
			increaseElementaryTreeesFrom(inputTree);
		}
		String log = "Extracted all possible lexicalized trees\n\t# Trees: " + template_freq.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	public void printElementaryTreeStatistics(int[] treeIndex) {
		for(int index : treeIndex) {
			TSNode inputTree = Parameters.trainingCorpus.treeBank.get(index);
			List<TSNode> lexicon = inputTree.collectLexicalItems();			
			List<ArrayList<TSNode>> eTrees = 
				LTSG.allLexTreesForEachAnchor(inputTree, Parameters.spineConversion, lexicon);
			int terminalIndex = 0;
			for(ArrayList<TSNode> terminalTrees : eTrees) {		
				String lexAnchor = lexicon.get(terminalIndex).label();
				System.out.println(lexAnchor + " : " + lexicon_freq.get(lexAnchor));
	 			for (TSNode lexTree : terminalTrees) {
	 				String treeString = lexTree.toString(false,true);
	 				System.out.println("\t" + treeString + " : " + template_freq.get(treeString));
				}
	 			terminalIndex++;
			}
		}
		System.out.print("\n\n");
	}
	
	protected void decreaseElementayTreesFrom(TSNode inputTree) {
		List<TSNode> lexicon = inputTree.collectLexicalItems();
		List<ArrayList<TSNode>> eTrees = 
			LTSG.allLexTreesForEachAnchor(inputTree, Parameters.spineConversion, lexicon);
		int terminalIndex = 0;
		for(ArrayList<TSNode> terminalTrees : eTrees) {		
			String lexAnchor = lexicon.get(terminalIndex).label();
 			for (TSNode lexTree : terminalTrees) {																	
				Utility.decreaseStringInteger(template_freq, lexTree.toString(false, true), 1);
				Utility.decreaseStringInteger(lexicon_freq, lexAnchor, 1);
			}
 			terminalIndex++;
		}
	}
	
	protected void increaseElementaryTreeesFrom(TSNode inputTree) {
		List<TSNode> lexicon = inputTree.collectLexicalItems();
		List<ArrayList<TSNode>> eTrees = allLexTreesForEachAnchor(inputTree, lexicon);
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
	
	/**
	 * Return all possible lexicalized trees of the current parse tree
	 * divided for each lexical item.
	 * @return an array of LinkedList each containing all the lexicalized tree having
	 * for anchor a specific lexical item of the current parse tree
	 */
	protected static List<ArrayList<TSNode>> allLexTreesForEachAnchor(TSNode tree, 
			boolean spineConvertion, List<TSNode> anchors) {		
		ArrayList<ArrayList<TSNode>> result = new ArrayList<ArrayList<TSNode>>(anchors.size());
		for (TSNode lex : anchors) {			
			ArrayList<TSNode> result_i = new ArrayList<TSNode>();
			result.add(result_i);
			tree.markHeadPathToAnchor(lex);
			TSNode ET = new TSNode(tree);
			tree.unmarkHeadPathToAnchor(lex);
			ET.toLexicalizeFrame();
			if (spineConvertion) ET.convertToSpine();
			result_i.add(ET);
			do {
				ET = ET.getHeadDaughter();
				//if (!ET.isUniqueDaughter()) result_i.add(ET); // if avoiding unary production (problems with spine transorm)
				result_i.add(ET);
			} while(!ET.isPrelexical());			
		}
		return result;
	}
	
	/**
	 * New implementations, retrive parameters option automatically
	 * Return all possible lexicalized trees of the current parse tree
	 * divided for each lexical item.
	 * @return an array of LinkedList each containing all the lexicalized tree having
	 * for anchor a specific lexical item of the current parse tree
	 */
	protected List<ArrayList<TSNode>> allLexTreesForEachAnchor(TSNode tree, List<TSNode> anchors) {		
		ArrayList<ArrayList<TSNode>> result = new ArrayList<ArrayList<TSNode>>(anchors.size());
		for (TSNode lex : anchors) {			
			ArrayList<TSNode> result_i = new ArrayList<TSNode>();
			result.add(result_i);
			tree.markHeadPathToAnchor(lex);
			TSNode ET = new TSNode(tree);
			tree.unmarkHeadPathToAnchor(lex);
			ET.toLexicalizeFrame();
			ET.applyAllConversions();
			result_i.add(ET);
			do {
				ET = ET.getHeadDaughter();
				//if (!ET.isUniqueDaughter()) result_i.add(ET); // if avoiding unary production (problems with spine transorm)
				result_i.add(ET);
			} while(!ET.isPrelexical());			
		}
		return result;
	}
	
	
	public void evaluateTreebankOnSet(ConstCorpus evalSet, int displayPerLex) {
		File evalReport = new File(Parameters.outputPath + "EvalReport");
		//Hashtable<String, LinkedList<String>> indexedTrees = indexTreeByLexicon();
		TreeAnalizer analizer = new TreeAnalizer(this.template_freq);
		try {
			PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(evalReport)));
			for(ListIterator<TSNode> i = evalSet.treeBank.listIterator(); i.hasNext();) {				
				TSNode evalTree = (TSNode)i.next();				
				String line = evalTree.toString() + "\n";				
				List<TSNode> terminals = evalTree.collectTerminals();
				List<ArrayList<TSNode>> goldTrees = allLexTreesForEachAnchor(evalTree, false, terminals);
				int terminalIndex = 0;
				for(ArrayList<TSNode> terminalTree : goldTrees) {
					TSNode term = (TSNode)terminals.get(terminalIndex);
					String word = Utility.removeDoubleQuotes(term.label);
					boolean unknown = this.lexicon_freq.get(word)==null;
					line += word + ((unknown)? " (unknown)" : "") + "\n";					
					boolean closestFound = false;
					for(TSNode goldTemplate : terminalTree) {	
						Object[][] closestTrees = analizer.getClosestTrees(goldTemplate.toString(false, true));							
						if (closestTrees!=null) {														
							closestFound = true;
							line += "\t" + goldTemplate + "\n";
							Hashtable<String, Integer> lexiconCount = new Hashtable<String, Integer>();							
							for(int k=0; k<closestTrees[0].length; k++) {
								String cT = (String)closestTrees[0][k];
								String lexicalAnchor = TSNode.get_unique_lexicon(cT);
								Integer count = (Integer)lexiconCount.get(lexicalAnchor); 
								if (count!=null && count>displayPerLex) continue;
								String recall_precision = " (" + closestTrees[1][0] + "/" + closestTrees[1][1] +
															"," + closestTrees[1][0] + "/" + closestTrees[1][2] + ")";
								line += "\t\t" + cT + recall_precision + "\n";
								Utility.increaseStringInteger(lexiconCount, lexicalAnchor, 1);
							}								
						}
					}
					if (!closestFound) line += "\tNo closest tree found";
					terminalIndex++;
				}
				output.write(line + "\n");
			}
			output.close();
		} catch (IOException e) {FileUtil.handleExceptions(e);}
	}
	
	public void printTemplatesToFile() {
		File templatesFile = new File(Parameters.outputPath + "TemplatesFile");
		FileUtil.printHashtableToFile(template_freq, templatesFile);
		String log = "Printed templates to file `templatesFile`";
		FileUtil.appendReturn(log, Parameters.logFile);
	}
}
