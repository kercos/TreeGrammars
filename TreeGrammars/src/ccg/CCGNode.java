package ccg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Scanner;

import settings.Parameters;
import tsg.TSNode;
import util.Utility;
import util.file.FileUtil;

public abstract class CCGNode {
	
	String cat;
	CCGInternalNode parent;
	
	public abstract boolean isTerminal();
	
		
	public static CCGNode getCCGNodeFromString(String parString) {
		ParenthesesBlockCCG p = ParenthesesBlockCCG.getParenthesesBlocks(parString);
		return (getCCGNodeFromParenthesesBlockCCG(p));
		
	}
	
	public static CCGNode getCCGNodeFromParenthesesBlockCCG(ParenthesesBlockCCG p) {
		if (p.subBlocks.isEmpty()) return new CCGTerminalNode(p.label);
		CCGInternalNode intNode = new CCGInternalNode(p.label);
		int i=0;
		for(ParenthesesBlockCCG subP : p.subBlocks) {			
			CCGNode d = getCCGNodeFromParenthesesBlockCCG(subP);
			intNode.daughters[i] = d;
			d.parent = intNode;
			i++;
		}
		return intNode;
	}
	
	public static ArrayList<CCGNode> readCCGFile(File inputFile, String encoding) {
		ArrayList<CCGNode> result = new ArrayList<CCGNode>();
		Scanner scan = FileUtil.getScanner(inputFile, encoding);
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.charAt(0)!='(') continue;
			result.add(getCCGNodeFromString(line));			
		}
		return result;
	}
	
	public CCGInternalNode parent() {
		return parent;
	}
	
	public String category() {
		return cat;
	}
	
	public boolean isConjunct() {
		return this.cat.endsWith("[conj]");
	}
	
	public boolean isUniqueDaughter() {
		return this.parent.prole==1;
	}
	
	public int countAllNodes() {
		int result = 1;
		if (this.isTerminal()) return result; 
		for(CCGNode n : ((CCGInternalNode)this).daughters) {
			result += n.countAllNodes();
		}
		return result;
	}
	
	public int countTerminalNodes() {
		int result = 0;
		if (this.isTerminal()) return 1;
		for(CCGNode n : ((CCGInternalNode)this).daughters) {
			result += n.countTerminalNodes();
		}
		return result;
	}
	
	/**
	 * Returns a LinkedList of type CCGNode containing the lexical items of
	 * the current CCGNode. 
	 */
	public List<CCGTerminalNode> collectTerminalNodes() {
		List<CCGTerminalNode> result = new ArrayList<CCGTerminalNode>();
		this.collectTerminalNodes(result);
		return result;
	}
	
	/**
	 * Method to implement collectTerminals()
	 * @param terminals
	 */
	private void collectTerminalNodes(List<CCGTerminalNode> terminals) {
		if (this.isTerminal()) {
			terminals.add((CCGTerminalNode)this);
			return;
		}
		for(CCGNode n : ((CCGInternalNode)this).daughters) {
			n.collectTerminalNodes(terminals);
		}		
	}
	
	/**
	 * Returns a LinkedList of type CCGNode containing the lexical items of
	 * the current CCGNode. 
	 */
	public ArrayList<CCGInternalNode> collectNonTerminalNodes() {
		ArrayList<CCGInternalNode> result = new ArrayList<CCGInternalNode>();
		this.collectNonTerminalNodes(result);
		return result;
	}
	
	/**
	 * Method to implement collectTerminals()
	 * @param nonTerminals
	 */
	private void collectNonTerminalNodes(List<CCGInternalNode> nonTerminals) {
		if (this.isTerminal()) {			
			return;
		}
		nonTerminals.add((CCGInternalNode)this);
		for(CCGNode n : ((CCGInternalNode)this).daughters) {			
			n.collectNonTerminalNodes(nonTerminals);
		}		
	}
	
	/**
	 * Returns a LinkedList with all the terminal and non terminal nodes
	 * in the current CCGNode.
	 * @return
	 */	
	public List<CCGNode> collectAllNodes() {
		List<CCGNode> list = new ArrayList<CCGNode>(); 
		collectAllNodes(list);
		return list;
	}
	
	private void collectAllNodes(List<CCGNode> list) {
		list.add(this);
		if (this.isTerminal()) return;
		for(CCGNode n : ((CCGInternalNode)this).daughters) {
			n.collectAllNodes(list);
		}
	}
	
	public ArrayList<CCGInternalNode> collectConjNodes() {
		ArrayList<CCGInternalNode> list = new ArrayList<CCGInternalNode>(); 
		collectConjNodes(list);
		return list;
	}
	
	private void collectConjNodes(ArrayList<CCGInternalNode> list) {
		if (this.cat.endsWith("[conj]")) list.add((CCGInternalNode)this);
		if (this.isTerminal()) return;
		for(CCGNode n : ((CCGInternalNode)this).daughters) {
			n.collectConjNodes(list);
		}
	}
	
	public int maxDepth() {
		if (this.isTerminal()) return 0;
		int maxDepth = 0;
		for(CCGNode n : ((CCGInternalNode)this).daughters) {
			int increase = 1;
			int depth = increase + n.maxDepth();
			if (depth > maxDepth) maxDepth = depth;
		}
		return maxDepth;
	}
	
	/**
	 * Return the hight of the current CCGNode in the tree
	 * @return an integer corresponding to the hight of the current TreeNode
	 */	
	public int hight() {		
		int hight = 0;
		CCGNode n = this;
		while(n.parent!=null) {
			n = n.parent;
			hight++;
		}
		return hight;		
	}
	
	public boolean isHead() {
		return parent!=null && this == parent.daughters[parent.headIndex];
	}
	
	public boolean isRoot() {
		return this.parent==null;
	}
	
	public static String toMSTUlab(CCGNode node) {
		// word, postag, label, index
		String words = "";
		String cat = "";
		String indexes = "";
		List<CCGTerminalNode> terminals = node.collectTerminalNodes();
		if (terminals.size()==1) {
			CCGTerminalNode n = terminals.get(0);
			words = n.word;
			cat = n.cat;
			indexes = "0";
		}
		else {
			for(CCGTerminalNode leaf : terminals) {			
				String dependentCat = leaf.cat;
				words += leaf.word + "\t";
				cat += dependentCat + "\t";
				CCGInternalNode ancestor = leaf.parent;
				if (leaf.isHead()) {
					while(ancestor.isHead() && !ancestor.isRoot()) ancestor = ancestor.parent;
					if (ancestor.isRoot()) {				
						indexes += 0 + "\t";
						continue;
					}
					ancestor = ancestor.parent;
				}													
				CCGNode head = ancestor.getAnchorThroughPercolation();
				int indexHead = terminals.indexOf(head) + 1;
				indexes += indexHead + "\t";		 
			}
		}
		String result = "";
		String[] resultArray = new String[]{words, cat, indexes};
		for(int i=0; i<resultArray.length; i++) {
			result += resultArray[i].trim() + "\n";
		}
		return result;		
	}
	
	public static void toMSTUlab(File inputFile, File outputFile, String encoding) {
		ArrayList<CCGNode> treebank = CCGNode.readCCGFile(inputFile, encoding);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile, encoding);
		int index=0;
		for(CCGNode n : treebank) {
			index++;
			pw.println(CCGNode.toMSTUlab(n));
		}
		pw.close();
	}
	
	public static void hasNonMinimalHeads(File inputFile, String encoding) {
		ArrayList<CCGNode> treebank = CCGNode.readCCGFile(inputFile, encoding);
		int i=0;
		for(CCGNode t : treebank) {
			i++;
			t.hasNonMinimalHeads(false, i);
		}
	}
	
	public void hasNonMinimalHeads(boolean countPunctuation, int sentenceNumber) {
		List<CCGInternalNode> allNodes = this.collectNonTerminalNodes();
		List<CCGTerminalNode> allTerminals = this.collectTerminalNodes();
		IdentityHashMap<CCGTerminalNode, Integer> hights = new IdentityHashMap<CCGTerminalNode, Integer>(); 
		for(CCGTerminalNode t : allTerminals) {
			 hights.put(t, t.hight());			
		}
		for(CCGInternalNode n : allNodes) {
			CCGTerminalNode terminalHead = n.getAnchorThroughPercolation();
			int terminalHeadHight = hights.get(terminalHead);
			List<CCGTerminalNode> terminals = n.collectTerminalNodes();
			CCGTerminalNode minimalHead = terminalHead;
			int minimalHight = terminalHeadHight;
			for(CCGTerminalNode t : terminals) {
				if (t == terminalHead) continue;
				if (!countPunctuation && t.isPunctuation()) continue;
				int tHight = hights.get(t);
				if (tHight < minimalHight) {
					minimalHight = tHight;
					minimalHead = t;										
				}
			}
			if (minimalHead != terminalHead) {
				System.out.println(sentenceNumber + ":\t" + this.toString() + "\n" +
						"\tParent: " + n.cat + 
						"  Current Head: " + terminalHead.word + 
						"  Min Head: " + minimalHead.word);
			}
		}
	}
	
    public static void main(String[] args) {
    	//String p = "(<T S[dcl] 0 2> (<T S[dcl] 1 2> (<T NP 0 1> (<T N 1 2> (<L N/N NNP NNP Mr. N_142/N_142>) (<L N NNP NNP Vinken N>) ) ) (<T S[dcl]\\NP 0 2> (<L (S[dcl]\\NP)/NP VBZ VBZ is (S[dcl]\\NP_87)/NP_88>) (<T NP 0 2> (<T NP 0 1> (<L N NN NN chairman N>) ) (<T NP\\NP 0 2> (<L (NP\\NP)/NP IN IN of (NP_99\\NP_99)/NP_100>) (<T NP 0 2> (<T NP 0 1> (<T N 1 2> (<L N/N NNP NNP Elsevier N_109/N_109>) (<L N NNP NNP N.V. N>) ) ) (<T NP[conj] 1 2> (<L , , , , ,>) (<T NP 1 2> (<L NP[nb]/N DT DT the NP[nb]_131/N_131>) (<T N 1 2> (<L N/N NNP NNP Dutch N_126/N_126>) (<T N 1 2> (<L N/N VBG VBG publishing N_119/N_119>) (<L N NN NN group N>) ) ) ) ) ) ) ) ) ) (<L . . . . .>) )";
    	//String p = "(a (b (c (d (e) (f) ) ) ) )";
    	//CCGNode node = getCCGNodeFromString(p);
    	//System.out.println(p);
    	//System.out.println(node);
    	//System.out.println(CCGNode.toMSTUlab(node));
    	/*File inputFile = new File(Parameters.corpusPath + "ccgbank_1_1/data/AUTO/wsj-24.auto");
    	File outputFile = new File(Parameters.corpusPath + "ccgbank_1_1/data/DEP/wsj-24.ulab");
    	CCGNode.toMSTUlab(inputFile, outputFile, FileUtil.defaultEncoding);*/
    	File inputFile = new File(Parameters.corpusPath + "ccgbank_1_1/data/AUTO/wsj-00.auto");
    	hasNonMinimalHeads(inputFile, FileUtil.defaultEncoding);
    }
}
