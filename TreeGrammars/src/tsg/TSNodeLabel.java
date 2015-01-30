package tsg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolList;
import symbols.SymbolString;
import tsg.ParenthesesBlockPennStd.WrongParenthesesBlockException;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import tsg.kernels.AllOrderedNodeExactSubsequenceOld;
import tsg.kernels.AllOrderedNodeExactSubsequences;
import tsg.kernels.FragmentSeeker;
import tsg.parsingExp.TSGparsingBitPar;
import util.Pair;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class TSNodeLabel {

	public Label label;
	public TSNodeLabel parent;
	public boolean headMarked;
	public boolean isLexical;
	public TSNodeLabel[] daughters;
	
	protected TSNodeLabel() {	
	}
	
	public TSNodeLabel(String s) throws Exception {
		this(s, true);
	}
	
	public TSNodeLabel(Label label, boolean isLexical) {
		this.label = label;
		this.isLexical = isLexical;
	}
	
	/**
	 * 
	 * @param s
	 * @param allTerminalsAreLexical if false all lexical are marked with double quotations
	 * @throws Exception
	 */
	public TSNodeLabel(String s, boolean allTerminalsAreLexical) throws Exception {		
		this(ParenthesesBlockPenn.getParenthesesBlocks(s), allTerminalsAreLexical);
	}
	
	public static TSNodeLabel newTSNodeLabelStd(String s) throws WrongParenthesesBlockException {
		return getTSNodeLabelStd(ParenthesesBlockPennStd.getParenthesesBlocks(s));
	}
	
	private static TSNodeLabel getTSNodeLabelStd(ParenthesesBlockPennStd p)  {
		TSNodeLabel result = new TSNodeLabel(Label.getLabel(p.label), p.isLexical());
		if (!p.isTerminal()) {
			result.daughters = new TSNodeLabel[p.subBlocks.size()];
			int i=0;
			for(ParenthesesBlockPennStd pd : p.subBlocks) {
				TSNodeLabel d =  getTSNodeLabelStd(pd);				
				d.parent = result;
				result.daughters[i] = d;
				i++;
			}
		}
		return result;
	}
	
	
	
	public TSNodeLabel nonRecursiveCopy() {
		TSNodeLabel result = new TSNodeLabel();
		result.label = this.label;
		result.headMarked = this.headMarked;
		result.isLexical = this.isLexical;		
		return result;
	}
	
	public TSNodeLabel clone() {
		TSNodeLabel result = new TSNodeLabel();
		result.label = this.label;
		result.headMarked = this.headMarked;
		result.isLexical = this.isLexical;
		if (this.isTerminal()) return result;
		int prole = this.prole();
		result.daughters = new TSNodeLabel[prole];
		for(int i=0; i<prole(); i++) {
			result.daughters[i] = this.daughters[i].clone();
			result.daughters[i].parent = result;
		}
		return result;
	}
	
	public TSNodeLabel cloneOneLevel() {
		return cloneOneLevel(false);
	}	
	
	public TSNodeLabel cloneOneLevel(boolean stop) {
		TSNodeLabel result = new TSNodeLabel();
		result.label = this.label;
		result.headMarked = this.headMarked;
		result.isLexical = this.isLexical;
		if (stop || this.isTerminal()) return result;
		int prole = this.prole();
		result.daughters = new TSNodeLabel[prole];
		for(int i=0; i<prole(); i++) {
			result.daughters[i] = this.daughters[i].cloneOneLevel(true);
			result.daughters[i].parent = result;
		}
		return result;
	}
	
	public TSNodeLabel cloneCfg() {
		TSNodeLabel result = new TSNodeLabel();
		result.label = this.label;
		result.headMarked = this.headMarked;
		result.isLexical = this.isLexical;
		if (this.isTerminal()) return result;
		int prole = this.prole();
		result.daughters = new TSNodeLabel[prole];
		for(int i=0; i<prole(); i++) {
			result.daughters[i] = this.daughters[i].cloneOneLevel(true);
			result.daughters[i].parent = result;
		}
		return result;
	}
	
	public boolean checkParentDaughtersConsistency() {
		if (this.isTerminal()) return true;
		for(TSNodeLabel d : daughters) {
			if (d.parent != this) {
				System.err.println(d.label() + ".parent != " + this.label());
				return false;
			}
			if (!d.checkParentDaughtersConsistency()) return false;			
		}
		return true;
	}
	
	public boolean checkOnlyAndAllTerminalsAreLexical() {
		boolean terminal = this.isTerminal(); 
		if (terminal != this.isLexical) return false;
		if (!terminal) {
			for(TSNodeLabel d : daughters) {
				if (!d.checkOnlyAndAllTerminalsAreLexical()) return false;
			}
		}
		return true;
	}
	
	
	
	public TSNodeLabel addTop() {
		if (this.label().equals("TOP")) return this;
		TSNodeLabel top = new TSNodeLabel();
		top.label = Label.getLabel("TOP");
		top.daughters = new TSNodeLabel[]{this};
		this.parent = top;
		return top;
	}
	
	private TSNodeLabel(ParenthesesBlockPenn p, boolean allTerminalsAreLexical) {
		if (p.isTerminal()) acquireTerminalLabel(p, allTerminalsAreLexical);
		else {
			acquireNonTerminalLabel(p);
			this.daughters = new TSNodeLabel[p.subBlocks.size()];
			int i=0;
			for(ParenthesesBlockPenn pd : p.subBlocks) {
				this.daughters[i] = new TSNodeLabel(pd, allTerminalsAreLexical);
				this.daughters[i].parent = this;
				i++;
			}
		}	
	}
	
	protected void acquireTerminalLabel(ParenthesesBlockPenn p, boolean allTerminalsAreLexical) {
		if (allTerminalsAreLexical) {
			this.label = Label.getLabel(p.label);
			this.isLexical = true;
		}
		else {
			if (p.label.indexOf("\"")==-1) {
				this.label = Label.getLabel(p.label);
			}
			else {
				this.label = Label.getLabel(p.label.replaceAll("\"",""));
				this.isLexical = true;
			}	
		}
	}
	
	public void relabel(String newLabel) {
		this.label = Label.getLabel(newLabel);
	}
	
	/**
	 * Replace all the occurances of oldLabel with newLabel in the
	 * current TreeNode. 
	 * @param oldLabel
	 * @param newLabel
	 */
	public void replaceLabels(String oldLabel, String newLabel) {
		if (this.label().equals(oldLabel)) this.label = Label.getLabel(newLabel);;
		if (this.daughters==null) return;
		for(TSNodeLabel d : this.daughters) {
			d.replaceLabels(oldLabel, newLabel);
		}
	}
	
	protected void acquireNonTerminalLabel(ParenthesesBlockPenn p) {
		if (p.label.endsWith("-H")) {
			this.headMarked = true;
			this.label = Label.getLabel(p.label.substring(0, p.label.length()-2));
		}
		else this.label = Label.getLabel(p.label);		
	}
	
	public boolean isHeadMarked() {
		return this.headMarked;
	}
	
	public boolean isTerminal() {
		return this.daughters == null;
	}
	
	public boolean isPreTerminal() {
		return (this.daughters.length==1 && this.daughters[0].isTerminal());
	}
	
	public boolean isPreLexical() {
		return (this.daughters.length==1 && this.daughters[0].isLexical);
	}
		
	public boolean hasOnlyPrelexicalDaughters() {
		if (this.daughters==null) return false;
		for(TSNodeLabel d : daughters) {
			if (!d.isPreLexical()) return false;
		}
		return true;
	}
	
	public String label() {
		return this.label.toString();
	}
	
	public String cfgRule() {
		StringBuilder result = new StringBuilder(this.label());
		for(TSNodeLabel d : daughters) {
			result.append(" ").append(d.label(false, true));
		}
		return result.toString();
	}
	
	public String cfgRuleNoQuotes() {
		StringBuilder result = new StringBuilder(this.label());
		for(TSNodeLabel d : daughters) {
			result.append(" ").append(d.label(false, false));
		}
		return result.toString();
	}

	
	public SymbolList cfgRuleSymbol() {
		Vector<Symbol> rule = new Vector<Symbol>();
		rule.add(new SymbolString(this.label()));
		for(TSNodeLabel d : daughters) {
			rule.add(new SymbolString(d.label()));			
		}
		SymbolList result = new SymbolList(rule);
		return result;
	}
	
	public String compressToCFGRule() {
		StringBuilder sb = new StringBuilder(this.label());
		fillStringBuilderWithTerminals(sb);
		return sb.toString();
	}
	
	public void fillStringBuilderWithTerminals(StringBuilder sb) {
		if (this.isTerminal()) {
			sb.append(" " + this.label(false, true));
			return;
		}
		for(TSNodeLabel d  : this.daughters) {
			d.fillStringBuilderWithTerminals(sb);
		}
	}
	
	/**
	 * @return true if the current TreeNodeLabel is a unique daughter, 
	 * i.e. it's parent has only one daughter.
	 */
	public boolean isUniqueDaughter() {
		TSNodeLabel parent = this.parent;
		if (parent==null) return false;
		return (this.parent.daughters.length==1);
	}

	
	public String label(boolean printHeads, boolean printLexiconInQuotes) {
		if (this.isLexical && printLexiconInQuotes) return "\"" + label() + "\"";
		if (!printHeads || !this.headMarked) return label();		
		return label() + "-H";
	}
	
	public String toStringTexRule() {
		StringBuilder sb = new StringBuilder();
		if (this.isTerminal())
			sb.append(this.labelTex());
		else { 
			sb.append(this.labelTex() + " $\\rightarrow$");
			for(TSNodeLabel d : this.daughters) {
				sb.append(" " + d.labelTex());
			}
		}
		return sb.toString();
	}
	
    private String labelTex() {
		if (this.isLexical)
			return "``" + label() + "''";
		return label();
	}

	public String toString() {
    	if (this.isTerminal()) return label();
    	StringBuilder result = new StringBuilder("(" + label() + " ");
		int size = this.daughters.length;
		int i=0;
    	for(TSNodeLabel p : this.daughters) {
    		if (p==this) {
    			System.err.println();
    		}
    		result.append(p.toString());
    		if (++i!=size) result.append(" ");
    	}
    	result.append(")");
    	return result.toString();
    }
    
	public String toStringExtraParenthesis() {
		return "( " + this.toString() + ")";
		
	}
    
    public String toString(boolean printHeads, boolean printLexiconInQuotes) {
    	if (this.isLexical && printLexiconInQuotes) {
    		return "\"" + label() + "\"";
    	}
    	if (this.isTerminal()) return label(printHeads, printLexiconInQuotes);
    	StringBuilder result = new StringBuilder("(" + label(printHeads, printLexiconInQuotes) + " ");
		int size = this.daughters.length;
		int i=0;
    	for(TSNodeLabel p : this.daughters) {
    		result.append(p.toString(printHeads, printLexiconInQuotes));
    		if (++i!=size) result.append(" ");
    	}
    	result.append(")");
    	return result.toString();
	}
    
    /**
     * terminal which are not lexical have a space in front
     * @return
     */
    public String toStringStandard() {
    	if (this.isLexical) {
    		return label();
    	}
    	if (this.isTerminal()) {
    		return "(" + label() + " )";
    	}
    	StringBuilder result = new StringBuilder("(" + label() + " ");
		int size = this.daughters.length;
		int i=0;
    	for(TSNodeLabel p : this.daughters) {
    		result.append(p.toStringStandard());
    		if (++i!=size) result.append(" ");
    	}
    	result.append(")");
    	return result.toString();
    }
    
    public String toStringOneLevel() {
    	return toStringOneLevel(false,false);
    }
    
    public String toStringOneLevel(boolean printHeads, boolean printLexiconInQuotes) {
    	if (this.isLexical && printLexiconInQuotes) {
    		return "\"" + label() + "\"";
    	}
    	if (this.isTerminal()) return label();
    	StringBuilder result = new StringBuilder("(" + label(printHeads, printLexiconInQuotes) + " ");
		int size = this.daughters.length;
		int i=0;
    	for(TSNodeLabel p : this.daughters) {
    		result.append(p.label(printHeads, printLexiconInQuotes));
    		if (++i!=size) result.append(" ");
    	}
    	result.append(")");
    	return result.toString();
	}
    
    //non lexical non prelexical
    public int countInternalNodes() {		
		if (this.isPreLexical()) return 0;
		int result = 1;
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countInternalNodes();
		}
		return result;
	}
    
    public int countNonTerminalNodes() {		
		if (this.isTerminal()) return 0;
		int result = 1;
		for(TSNodeLabel d : this.daughters) {
			result += d.countNonTerminalNodes();
		}
		return result;
	}
    
	public int countAllNodes() {
		int result = 1;
		if (this.isTerminal()) return result; 
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countAllNodes();
		}
		return result;
	}
	
	public int countLexicalNodes() {
		if (this.isLexical) return 1;		
		if (this.isTerminal()) return 0;
		int result = 0;
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countLexicalNodes();
		}
		return result;
	}
	
	public int countNonLexicalNodes() {
		if (this.isLexical) return 0;		
		if (this.isTerminal()) return 1;
		int result = 1;
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countNonLexicalNodes();
		}
		return result;
	}
	
	public int countNonLexicalFronteer() {
		if (this.isLexical) return 0;
		if (this.isTerminal()) return 1;
		int result = 0;
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countNonLexicalFronteer();
		}
		return result;
	}
	
	/**
	 * Returns the number of lexical nodes yielded by the current TreeNode,
	 * excluding the pos-tags present in the input array excludePosLabels which
	 * has to be sorted beforehand (as with Arrays.sort(excludeLexLabels)).
	 */
	public int countLexicalNodesExcludingPosLabels(String[] excludePosLabels) {
		int result = 0;
		if (this.isPreLexical()) {
			return  (Arrays.binarySearch(excludePosLabels, this.label())<0) ? 1 : 0;
		}
		for(TSNodeLabel TN : this.daughters) result += TN.countLexicalNodesExcludingPosLabels(excludePosLabels);
		return result;
	}
	
	public int countLexicalNodesExcludingCatLabels(String[] excludeLexLabels) {
		int result = 0;
		if (this.isLexical) {
			return  (Arrays.binarySearch(excludeLexLabels, this.parent.label())<0) ? 1 : 0;
		}
		for(TSNodeLabel TN : this.daughters) result += TN.countLexicalNodesExcludingCatLabels(excludeLexLabels);
		return result;
	}
	
	public int countTerminalNodes() {
		if (this.isTerminal()) return 1;		
		int result = 0;
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countTerminalNodes();
		}
		return result;
	}
	
	public int countTerminalNonLexNodes() {
		if (this.isTerminal()) 
			return this.isLexical ? 0 : 1;		
		int result = 0;
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countTerminalNonLexNodes();
		}
		return result;
	}
	
	/**
	 * Returns the number of POS which are equal in label with the 
	 * gold treee in input. The count excludes the gold labels present in the input array
	 * exludePOS which should be sorted beforehand (i.e. Arrays.sort(excludeLabels)).
	 */
	public int countCorrectPOS(TSNodeLabel TNgold, String[] exludePOS) {
		List<TSNodeLabel> thisLex = this.collectLexicalItems();
		List<TSNodeLabel> goldLex = TNgold.collectLexicalItems();
		int result = 0;
		for(int i=0; i<thisLex.size(); i++) {
			String thisPOS = thisLex.get(i).parent.label();
			String goldPOS = goldLex.get(i).parent.label();
			if (Arrays.binarySearch(exludePOS, goldPOS)>=0) continue;
			if (thisPOS.equals(goldPOS)) result++;
		}
		return result;
	}
	
	public int countCorrectPOSdiffSizes(TSNodeLabel TNgold, String[] exludePOS) {
		ArrayList<Pair<Label>> thisPosLex = new ArrayList<Pair<Label>>();
		ArrayList<Pair<Label>> goldPosLex = new ArrayList<Pair<Label>>();
		for(TSNodeLabel l : this.collectLexicalItems()) {
			thisPosLex.add(new Pair<Label>(l.parent.label, l.label));
		}
		for(TSNodeLabel l : TNgold.collectLexicalItems()) {
			goldPosLex.add(new Pair<Label>(l.parent.label, l.label));
		}
		int result = 0;
		for(Pair<Label> g : goldPosLex) {
			if (thisPosLex.remove(g))
				result++;
		}
		return result;
	}
	
	/**
	 * Returns a LinkedList of type TreeNodeLabel containing the lexical items of
	 * the current TreeNodeLabel. 
	 */
	public ArrayList<TSNodeLabel> collectLexicalItems() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectLexicalItems(result);
		return result;
	}
	
	/**
	 * Returns a LinkedList of type String containing the lexical words of
	 * the current TreeNodeLabel. 
	 */
	public ArrayList<String> collectLexicalWords() {
		ArrayList<TSNodeLabel> lex = this.collectLexicalItems();
		ArrayList<String> result = new ArrayList<String>(lex.size());
		for(TSNodeLabel l : lex) {
			result.add(l.label());
		}
		return result;
	}
	
	/**
	 * Returns a ArrayList of type Label containing the lexical words of
	 * the current TreeNodeLabel. 
	 */
	public ArrayList<Label> collectLexicalLabels() {
		ArrayList<TSNodeLabel> lex = this.collectLexicalItems();
		ArrayList<Label> result = new ArrayList<Label>(lex.size());
		for(TSNodeLabel l : lex) {
			result.add(l.label);
		}
		return result;
	}
	
	public ArrayList<Label> collectTerminalLabels() {
		ArrayList<TSNodeLabel> terms = this.collectTerminalItems();
		ArrayList<Label> result = new ArrayList<Label>(terms.size());
		for(TSNodeLabel t : terms) {
			result.add(t.label);
		}
		return result;
	}
	
	/**
	 * Method to implement collectLexicalItems()
	 * @param lexItems
	 */
	private void collectLexicalItems(List<TSNodeLabel> lexItems) {
		if (this.isTerminal()) return;
		for(TSNodeLabel TN : this.daughters) {
			if (TN.isLexical) lexItems.add(TN);
			else TN.collectLexicalItems(lexItems);
		}		
	}
	
	public ArrayList<TSNodeLabel> collectPreLex2Items() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectLex2Items(result);
		return result;
	}
	
	private void collectLex2Items(List<TSNodeLabel> prelex2Items) {
		if (this.isTerminal()) return;
		if (this.firstDaughter().isLexical) prelex2Items.add(this);
		else for(TSNodeLabel TN : this.daughters) {
			TN.collectLex2Items(prelex2Items);
		}		
	}
	
	/**
	 * Returns a LinkedList of type TreeNodeLabel containing the lexical items of
	 * the current TreeNodeLabel. 
	 */
	public ArrayList<TSNodeLabel> collectPreLexicalItems() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectPreLexicalItems(result);
		return result;
	}	
	
	/**
	 * Method to implement collectLexicalItems()
	 * @param lexItems
	 */
	private void collectPreLexicalItems(List<TSNodeLabel> prelexItems) {
		if (this.isTerminal()) return;
		if (this.isPreLexical()) prelexItems.add(this);
		else for(TSNodeLabel TN : this.daughters) {
			TN.collectPreLexicalItems(prelexItems);
		}		
	}
	
	public ArrayList<Label> collectPreLexicalLabels() {
		ArrayList<Label> result = new ArrayList<Label>();
		this.collectPreLexicalLabels(result);
		return result;
	}
	
	private void collectPreLexicalLabels(List<Label> prelexLabels) {
		if (this.isTerminal()) return;
		if (this.isPreLexical()) prelexLabels.add(this.label);
		else for(TSNodeLabel TN : this.daughters) {
			TN.collectPreLexicalLabels(prelexLabels);
		}		
	}
	
	/**
	 * Returns a LinkedList of type TreeNodeLabel containing the terminal items of
	 * the current TreeNodeLabel. 
	 */
	public ArrayList<TSNodeLabel> collectTerminalItems() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectTerminalItems(result);
		return result;
	}
	
	/**
	 * Method to implement collectTerminals()
	 * @param terminals
	 */
	private void collectTerminalItems(List<TSNodeLabel> terminals) {
		if (this.isTerminal()) {
			terminals.add(this);
			return;
		}
		for(TSNodeLabel TN : this.daughters) {
			TN.collectTerminalItems(terminals);
		}		
	}
	
	public TSNodeLabel getTerminalItemsAtPosition(int pos) {
		return getTerminalItemsAtPosition(pos, new int[]{0});
	}
	
	private TSNodeLabel getTerminalItemsAtPosition(int pos, int[] currentPos) {
		if (this.isTerminal()) {
			if (currentPos[0]==pos)
				return this;
			currentPos[0]++;
			return null;
		}
		for(TSNodeLabel d : this.daughters) {
			TSNodeLabel target = d.getTerminalItemsAtPosition(pos, currentPos);
			if (target!=null)
				return target;			
		}
		return null;
	}
	
	/**
	 * Returns a LinkedList of type TreeNodeLabel containing all items of
	 * the current TreeNode, besides the one being a unique daughter of its parent.
	 */	
	public ArrayList<TSNodeLabel> collectConstituentNodes() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectAllNodes(result);
		for(ListIterator<TSNodeLabel> i = result.listIterator(); i.hasNext(); ) {
			TSNodeLabel node = i.next();
			TSNodeLabel parent = node.parent;
			if (node.isLexical || node.isPreLexical() || (parent!=null && parent.prole()==1)) i.remove();			
		}
		return result;
	}
	
	/**
	 * Returns a ArrayList of type TreeNodeLabel containing all items of
	 * the current TreeNode not being lexical or prelexical. 
	 */
	public ArrayList<TSNodeLabel> collectPhrasalNodes() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectPhrasalNodes(result);
		return result;
	}
	
	/**
	 * Method to implement collectInternalNodes()
	 * @param terminals
	 */
	private void collectPhrasalNodes(List<TSNodeLabel> allNodes) {
		if (this.isTerminal()) return;
		allNodes.add(this);
		for(TSNodeLabel TN : this.daughters) {
			TN.collectPhrasalNodes(allNodes);
		}		
	}
	
	/**
	 * Returns a ArrayList of type TreeNodeLabel containing all items of
	 * the current TreeNode not being lexical or prelexical. 
	 */
	public ArrayList<TSNodeLabel> collectInternalNodes() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		if (this.isTerminal()) return result;
		for(TSNodeLabel TN : this.daughters) {
			TN.collectPhrasalNodes(result);
		}
		return result;
	}
	
	/**
	 * Returns a ArrayList of type TreeNodeLabel containing all items of
	 * the current TreeNode not being lexical or prelexical. 
	 */
	public ArrayList<TSNodeLabel> collectInternalNodesMatching(String regex) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectInternalNodesMatching(result, regex);
		return result;
	}
	
	/**
	 * Method to implement collectInternalNodes()
	 * @param terminals
	 */
	private void collectInternalNodesMatching(List<TSNodeLabel> allNodes, String regex) {
		if (this.isLexical)
			return;
		if (this.label().matches(regex))
			allNodes.add(this);
		if (this.isTerminal()) return;
		for(TSNodeLabel TN : this.daughters) {
			TN.collectInternalNodesMatching(allNodes, regex);
		}		
	}
	
	/**
	 * Returns a ArrayList of type TreeNodeLabel containing all items of
	 * the current TreeNode not being lexical or prelexical. 
	 */
	public ArrayList<TSNodeLabel> collectInternalNodesMatching(String[] orderedList) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectInternalNodesMatching(result, orderedList);
		return result;
	}
	
	/**
	 * Method to implement collectInternalNodes()
	 * @param terminals
	 */
	private void collectInternalNodesMatching(List<TSNodeLabel> allNodes, String[] orderedList) {
		if (this.isLexical)
			return;
		if (Arrays.binarySearch(orderedList, this.label())>=0)
			allNodes.add(this);
		if (this.isTerminal()) return;
		for(TSNodeLabel TN : this.daughters) {
			TN.collectInternalNodesMatching(allNodes, orderedList);
		}		
	}
	
	/**
	 * Returns a ArrayList of type TreeNodeLabel containing all items of
	 * the current TreeNode not being lexical 
	 */
	public ArrayList<TSNodeLabel> collectNonLexicalNodes() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectNonLexicalNodes(result);
		return result;
	}
	
	/**
	 * Method to implement collectNonLexicalNodes()
	 * @param terminals
	 */
	private void collectNonLexicalNodes(List<TSNodeLabel> allNodes) {
		if (this.isLexical) return;
		allNodes.add(this);		
		for(TSNodeLabel TN : this.daughters) {
			TN.collectNonLexicalNodes(allNodes);
		}		
	}
	
	/**
	 * Returns a LinkedList of type TreeNodeLabel containing all items of
	 * the current TreeNode in depth first. 
	 */
	public ArrayList<TSNodeLabel> collectAllNodes() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectAllNodes(result);
		return result;
	}
	
	/**
	 * Method to implement collectTerminals()
	 * @param terminals
	 */
	private void collectAllNodes(List<TSNodeLabel> allNodes) {
		allNodes.add(this);
		if (this.isTerminal()) return;
		for(TSNodeLabel TN : this.daughters) {
			TN.collectAllNodes(allNodes);
		}		
	}
	
	/**
	 * Returns a LinkedList of type TreeNodeLabel containing all items of
	 * the current TreeNode in breath first. 
	 */
	public ArrayList<TSNodeLabel> collectAllNodesBreathFirst() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		result.add(this);
		this.collectAllNodesBreathFirst(result);
		return result;
	}
	
	/**
	 * Method to implement collectAllNodesBreathFirst()
	 * @param terminals
	 */
	private void collectAllNodesBreathFirst(List<TSNodeLabel> allNodes) {		
		if (this.isTerminal()) return;
		for(TSNodeLabel TN : this.daughters)
			allNodes.add(TN);			
		for(TSNodeLabel TN : this.daughters)
			TN.collectAllNodesBreathFirst(allNodes);
	}

	public void addSubSites(LinkedList<TSNodeLabel> result, boolean first) {
		ArrayList<TSNodeLabel> subSites = collectNonLexTerminals();
		if (first)
			result.addAll(0, subSites);
		else 
			result.addAll(subSites);
	}
	
	public ArrayList<TSNodeLabel> collectNonLexTerminals() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		collectNonLexTerminals(result);
		return result;
	}
	
	private void collectNonLexTerminals(List<TSNodeLabel> nodes) {
		if (this.isTerminal()) {
			if (!this.isLexical) {
				nodes.add(this);
			}
			return;
		}		
		for(TSNodeLabel TN : this.daughters) {
			TN.collectNonLexTerminals(nodes);
		}		
	}
	
	public boolean sameLabel(TSNodeLabel nodeB) {		
		return this.isLexical == nodeB.isLexical && this.label.equals(nodeB.label); 
		
	}
	
	public boolean sameLexLabels(TSNodeLabel nodeB) {		
		ArrayList<TSNodeLabel> lex1 = this.collectLexicalItems();
		ArrayList<TSNodeLabel> lex2 = nodeB.collectLexicalItems();
		return lex1.equals(lex2);
		
	}
	
	public boolean sameDaughtersLabel(TSNodeLabel nodeB) {
		if (this.daughters.length != nodeB.daughters.length) return false;
		for(int i=0; i<this.daughters.length; i++) {
			if (!this.daughters[i].sameLabel(nodeB.daughters[i])) return false;
		}
		return true;
	}
	
	public boolean sameLabelAndDaughersLabels(TSNodeLabel nodeB) {		
		if (this.isLexical != nodeB.isLexical || !this.label.equals(nodeB.label)) return false;
		if (this.daughters.length != nodeB.daughters.length) return false;
		for(int i=0; i<this.daughters.length; i++) {
			if (!this.daughters[i].sameLabel(nodeB.daughters[i])) return false;
		}
		return true;
	}
	
	public int hashCode() {
		int result = 31 + this.label().hashCode();
		if (!this.isTerminal()) {
			for(TSNodeLabel d : this.daughters) {
				result = 31 * result + d.hashCode(); 
			}
		}
		return result;
	}
	
	public boolean hasNullElements() {
		if (this.isTerminal()) return false;
		for(TSNodeLabel d : this.daughters) {
			if (d==null || d.hasNullElements())
				return true;
		}
		return false;
	}
	
	public int prole() {
		if (this.daughters==null) return 0;
		return this.daughters.length;
	}
	
	public boolean equals(Object o) {
		if (o==this) return true;
		if (o instanceof TSNodeLabel) {
			TSNodeLabel anOtherNode = (TSNodeLabel) o;
			return this.equals(anOtherNode);
		}
		return false;		
	}
	
	public boolean equals(TSNodeLabel anOtherNode) {
		if (!anOtherNode.label.equals(this.label) || 
				anOtherNode.headMarked != this.headMarked ||
				anOtherNode.isLexical != this.isLexical) return false;
		if (this.isTerminal() || anOtherNode.isTerminal()) {
			return this.isTerminal() == anOtherNode.isTerminal();
		}
		int thisProle = this.prole();
		if (anOtherNode.prole() != thisProle) return false;
		for(int d=0; d<thisProle; d++) {				
			if (!anOtherNode.daughters[d].equals(daughters[d])) return false;
		}
		return true;
	}
	
	public int maxDepth() {
		if (this.isTerminal()) return 0;		
		int maxDepth = 0;
		for(TSNodeLabel t : this.daughters) {
			int tDepth = t.maxDepth();
			if (tDepth > maxDepth) maxDepth = tDepth;
		}
		return maxDepth+1;
	}
	
	public int maxBranching() {
		if (this.isTerminal()) return 0;		
		int maxBranching = this.prole();
		for(TSNodeLabel t : this.daughters) {
			int tBranching = t.maxBranching();
			if (tBranching > maxBranching) maxBranching = tBranching;
		}
		return maxBranching;
	}
	
	/**
	 * Return the hight of the current TreeNodeLabel in the tree (0 if root)
	 */	
	public int height() {		
		int hight = 0;
		TSNodeLabel TN = this;
		while(TN.parent!=null) {
			TN = TN.parent;
			hight++;
		}
		return hight;		
	}
	
	/**
	 * Remove the semantic tags of the type "-SBJ" in the labels TreeNodes within 
	 * the current treenode. 
	 */	
	public void removeSemanticTags() {
		if (this.isLexical) return;
		if (this.label.hasSemTags()) {
			this.relabel(TSNode.removeSemanticTag(label()));
		}
		for(TSNodeLabel D : this.daughters) D.removeSemanticTags();
	}
	
	public void removeBackSlashInLexicon() {
		if (this.isTerminal()) {
			if (this.isLexical) {
				String word = this.label();
				if (word.indexOf('\\')!=-1);
				word = word.replaceAll("\\\\", "");
				this.relabel(word);
			}
			return;
		}
		for(TSNodeLabel d : this.daughters) {
			d.removeBackSlashInLexicon();
		}
	}
		
	public void removeHeadLabels() {
		this.headMarked = false;
		if (this.isTerminal()) return;		
		for(TSNodeLabel D : this.daughters) D.removeHeadLabels();
	}
	
	/**
	 * Remove rules of the kind X -> X
	 */
	public void removeRedundantRules() {
		if (this.isTerminal() || this.isPreLexical()) return;
		if (this.prole()==1 && this.label.equals(this.daughters[0].label)) {			
			this.daughters = this.daughters[0].daughters;
			if (!this.isTerminal()) {
				for(TSNodeLabel D : this.daughters) D.parent = this;
				this.removeRedundantRules();
			}			
		}
		else {
			for(TSNodeLabel D : this.daughters) D.removeRedundantRules();
		}
	}
	
	public boolean hasRedundantRules() {
		if (this.isTerminal() || this.isPreLexical()) return false;
		if (this.prole()==1 && this.label.equals(this.daughters[0].label)) {			
			return true;			
		}
		for(TSNodeLabel d : this.daughters) { 
			if (d.hasRedundantRules())
				return true;
		}
		return false;
	}
	
	public void replaceAllLabels(String regexMatch, String replace) {		
		String labelString = this.label();
		this.relabel(labelString.replaceAll(regexMatch, replace));
		if (this.isTerminal()) return;
		for(int i=0; i<this.daughters.length; i++) {
			this.daughters[i].replaceAllLabels(regexMatch, replace);
		}
	}
	
	public void replaceAllNonTerminalLabels(String regexMatch, String replace) {
		if (this.isTerminal()) return;
		String labelString = this.label();
		this.relabel(labelString.replaceAll(regexMatch, replace));
		for(int i=0; i<this.daughters.length; i++) this.daughters[i].replaceAllNonTerminalLabels(regexMatch, replace);
	}
	
	public void replaceAllNonTerminalLabels(Label newLabel, Label except) {
		if (this.isTerminal()) return;
		if (!this.label.equals(except)) this.label = newLabel;		
		for(int i=0; i<this.daughters.length; i++) {
			this.daughters[i].replaceAllNonTerminalLabels(newLabel, except);
		}
	}
	
	public void renameAllConstituentLabels(Label newLabel) {
		if (this.isLexical || this.isPreLexical()) return;
		this.label = newLabel;		
		for(int i=0; i<this.daughters.length; i++) {
			this.daughters[i].renameAllConstituentLabels(newLabel);
		}
	}
	
	public boolean checkHeadCorrespondance(TSNodeLabel o) {
		int prole = this.prole();
		if (prole==0) return true;
		for(int i=0; i<prole; i++) {
			TSNodeLabel d = this.daughters[i];
			TSNodeLabel dO = o.daughters[i];                               
			if (d.headMarked != dO.headMarked) {
				System.err.println("Not matching heads: " +
						"\n\t" + this.toStringOneLevel(true, false) +
						"\n\t" + o.toStringOneLevel(true, false));
				return false;
			}
			if (!d.checkHeadCorrespondance(dO)) return false;
		}
		return true;
	}
	
	
	public boolean containsRecursiveFragment(TSNodeLabel otherTree) {
		if (this.containsNonRecursiveFragment(otherTree)) return true;
		if (this.isTerminal()) return false;
		for(TSNodeLabel d : this.daughters) {
			if (d.containsRecursiveFragment(otherTree)) return true;
		}
		return false;
	}
	
	public boolean containsNonRecursiveFragment(TSNodeLabel otherTree) {
		if (this.label.equals(otherTree.label)) {
			if (otherTree.isTerminal()) return true;
			if (this.isTerminal()) return false;
			int prole = this.prole();
			if (prole!=otherTree.prole()) return false;			
			for(int i=0; i<prole; i++) {
				TSNodeLabel thisDaughter = this.daughters[i];
				TSNodeLabel otherDaughter = otherTree.daughters[i];
				if (!thisDaughter.containsNonRecursiveFragment(otherDaughter)) 
					return false;
			}
			return true;			
		}
		return false;
	}
	
	public int countRecursiveFragment(TSNodeLabel otherTree) {		
		int count = this.containsNonRecursiveFragment(otherTree) ? 1 : 0;
		if (this.isTerminal()) return count;
		for(TSNodeLabel d : this.daughters) {
			count += d.countRecursiveFragment(otherTree);
		}
		return count;
	}
	
	public void countRecursiveFragmentInitialFinal(TSNodeLabel otherTree, long[] result, 
			TSNodeLabel initialLexNode, TSNodeLabel finalLexNode) {		
		if (this.containsNonRecursiveFragment(otherTree)) {
			result[0]++;
			if (this.getLeftmostLexicalNode()==initialLexNode)
				result[1]++;
			if (this.getRightmostLexicalNode()==finalLexNode)
				result[2]++;
		}
		if (this.isTerminal()) 
			return;
		for(TSNodeLabel d : this.daughters) {
			d.countRecursiveFragmentInitialFinal(otherTree, result, initialLexNode, finalLexNode);
		}
	}
	
	/**
	 * Computes the number of subTrees (fragements of indefinite depth) in the current TSNodeLabel.
	 * @return number of subTrees (DOP-like fragments)
	 */
	public BigInteger countSubTrees() {
		if (this.isLexical) return BigInteger.ZERO;
		int prole = this.prole();
		BigInteger result = this.daughters[0].countSubTrees().add(BigInteger.ONE);
		if (prole==1) return result;		
		for(int i=1; i<prole; i++) {
			BigInteger count = this.daughters[i].countSubTrees().add(BigInteger.ONE);
			result = result.multiply(count);			
		}
		return result;
	}
	
	/**
	 * Computes the number of subTrees (fragements of indefinite depth) in the current TSNodeLabel.
	 * Result in store at index 1
	 * @return number of subTrees (DOP-like fragments)
	 */
	public BigInteger[] countTotalFragments() {
		if (this.isLexical) return new BigInteger[]{BigInteger.ZERO,BigInteger.ZERO};	
		BigInteger[] result = new BigInteger[]{BigInteger.ONE,BigInteger.ZERO};
		
		for(TSNodeLabel d : this.daughters) {
			BigInteger[] countTotalFragmentsD = d.countTotalFragments();
			result[0] = result[0].multiply(countTotalFragmentsD[0].add(BigInteger.ONE));			
			result[1] = result[1].add(countTotalFragmentsD[1]);			
		}
		result[1] = result[1].add(result[0]);
		return result;
	}
	
	/**
	 * Computes the number of subTrees (fragements of indefinite depth) in the current TSNodeLabel and distribute them in depth
	 * @return number of subTrees (DOP-like fragments)
	 */
	public BigInteger[][] countTotalFragmentsDepth() {
		if (this.isPreLexical()) return new BigInteger[][]{{BigInteger.ONE},{BigInteger.ONE}};
		int prole = this.prole();
		BigInteger[][][] resultDaughers = new BigInteger[prole][][];
		int[] maxDepthDaughters = new int[prole];
		int currentMaxDepth = 0;
		for(int d=0; d<prole; d++) {
			BigInteger[][] resultD = daughters[d].countTotalFragmentsDepth();
			resultDaughers[d] = resultD;
			int maxDepthD = resultD[0].length;
			maxDepthDaughters[d] = maxDepthD;
			if (maxDepthD>currentMaxDepth) currentMaxDepth = maxDepthD;	
		}
		currentMaxDepth++;
		BigInteger[][] result = new BigInteger[2][currentMaxDepth];
		Arrays.fill(result[0], BigInteger.ZERO);
		Arrays.fill(result[1], BigInteger.ONE);
		int iPrevious = 0;
		for(int i=1; i<currentMaxDepth; i++) {
			for(int d=0; d<prole; d++) {
				int maxDepthD = maxDepthDaughters[d];
				boolean reachEndDepthOfDaughter = (maxDepthD<i); 
				BigInteger countDi =  null;
				if (reachEndDepthOfDaughter) countDi = resultDaughers[d][1][maxDepthD-1];
				else {
					countDi = resultDaughers[d][1][iPrevious];
					result[0][iPrevious] = result[0][iPrevious].add(resultDaughers[d][0][iPrevious]);
				}
				countDi = countDi.add(BigInteger.ONE);
				result[1][i] = result[1][i].multiply(countDi); 
			}
			iPrevious++;
		}
		result[0][0] = result[0][0].add(BigInteger.ONE);
		for(int i=1; i<currentMaxDepth; i++) {
			result[0][i] = result[0][i].add(result[1][i].subtract(result[1][i-1])); 
		}
		return result;
	}
	
	/**
	 * Computes the number of subTrees (fragements of indefinite depth) in the current TSNodeLabel and distribute them in depth
	 * @return number of subTrees (DOP-like fragments)
	 */
	public BigInteger[][] countTotalFragmentsDepthMaxBranching(int maxBranching) {
		if (this.isPreLexical()) return new BigInteger[][]{{BigInteger.ONE},{BigInteger.ONE}};
		int prole = this.prole();
		BigInteger[][][] resultDaughers = new BigInteger[prole][][];
		int[][] maxDepthsDaughters = new int[prole][2];
		int currentMaxDepth = 0;
		//boolean allMaxDepthAreMaxBranching = true;
		for(int d=0; d<prole; d++) {
			BigInteger[][] resultD = daughters[d].countTotalFragmentsDepthMaxBranching(maxBranching);
			resultDaughers[d] = resultD;
			int maxDepthD0 = resultD[0].length;
			int maxDepthD1 = resultD[1].length;
			maxDepthsDaughters[d] = new int[]{maxDepthD0,maxDepthD1};
			if (maxDepthD0>currentMaxDepth) currentMaxDepth = maxDepthD0;
			/*if (maxDepthD0>currentMaxDepth) {
				currentMaxDepth = maxDepthD0;
				allMaxDepthAreMaxBranching = resultD[1][0].equals(BigInteger.ZERO); 
			}
			else if (maxDepthD0==currentMaxDepth) {
				if (allMaxDepthAreMaxBranching && !resultD[1][0].equals(BigInteger.ZERO)) {
					allMaxDepthAreMaxBranching = false;
				}
			}*/
		}
		if (prole>maxBranching) {
			BigInteger[][] result = new BigInteger[][]{
					new BigInteger[currentMaxDepth], new BigInteger[]{BigInteger.ZERO}};
			Arrays.fill(result[0], BigInteger.ZERO);
			for(int d=0; d<prole; d++) {
				for(int i=0; i<maxDepthsDaughters[d][0]; i++) {
					result[0][i] = result[0][i].add(resultDaughers[d][0][i]);
				}
			}
			return result;
		}
		//if (!allMaxDepthAreMaxBranching) currentMaxDepth++;
		currentMaxDepth++;
		BigInteger[][] result = new BigInteger[2][currentMaxDepth];
		Arrays.fill(result[0], BigInteger.ZERO);
		Arrays.fill(result[1], BigInteger.ONE);
		int iPrevious = 0;
		for(int i=1; i<currentMaxDepth; i++) {
			for(int d=0; d<prole; d++) {
				int maxDepthD1 = maxDepthsDaughters[d][1];
				int maxDepthD0 = maxDepthsDaughters[d][0];
				BigInteger countDi = maxDepthD1<i ?
					resultDaughers[d][1][maxDepthD1-1] :
					resultDaughers[d][1][iPrevious];
				if (maxDepthD0>=i) {
					result[0][iPrevious] = result[0][iPrevious].add(resultDaughers[d][0][iPrevious]);
				}
				countDi = countDi.add(BigInteger.ONE);
				result[1][i] = result[1][i].multiply(countDi); 
			}
			iPrevious++;
		}
		result[0][0] = result[0][0].add(BigInteger.ONE);
		for(int i=1; i<currentMaxDepth; i++) {
			result[0][i] = result[0][i].add(result[1][i].subtract(result[1][i-1])); 
		}
		return result;
	}
	
	public boolean containsRecursivePartialFragment(TSNodeLabel partialFrag) {		
		if (this.containsNonRecursivePartialFragment(partialFrag)) return true;
		if (this.isTerminal()) return false;
		for(TSNodeLabel d : this.daughters) {
			if (d.containsRecursivePartialFragment(partialFrag)) return true;
		}
		return false;
	}
	
	public boolean matchNonRecursivePartialFragment(TSNodeLabel partialFrag) {		
		if (this.label.equals(partialFrag.label)) {
			if (partialFrag.isTerminal()) {
				return true;
			}
			if (this.isTerminal()) return false;			
			ArrayList<ArrayList<Pair<TSNodeLabel>>> matchedDaughtersList =
				AllOrderedNodeExactSubsequences.getallExactSubsequencesBackupOnSimple(
						this.daughters, partialFrag.daughters);
			if (matchedDaughtersList==null) return false;
			for(ArrayList<Pair<TSNodeLabel>> dList : matchedDaughtersList) {
				boolean allMatched = true;
				for(Pair<TSNodeLabel> p : dList) {
					TSNodeLabel first = p.getFirst();
					TSNodeLabel second = p.getSecond();
					boolean partialMatch = first.matchNonRecursivePartialFragment(second); 
					if (!partialMatch) {
						allMatched = false;
						break;
					}
				}
				if (allMatched) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsNonRecursivePartialFragment(TSNodeLabel partialFrag) {
		if (this.label.equals(partialFrag.label)) {
			if (partialFrag.isTerminal()) return true;
			if (this.isTerminal()) return false;
			ArrayList<ArrayList<Pair<TSNodeLabel>>> matchedDaughtersList =
				AllOrderedNodeExactSubsequenceOld.getallExactSubsequences(this.daughters, partialFrag.daughters);
			if (matchedDaughtersList==null) return false;
			for(ArrayList<Pair<TSNodeLabel>> dList : matchedDaughtersList) {
				boolean allMatched = true;
				for(Pair<TSNodeLabel> p : dList) {
					if (!p.getFirst().containsNonRecursivePartialFragment(p.getSecond())) {
						allMatched = false;
						break;
					}
				}
				if (allMatched) {
					return true;
				}
			}
		}
		return false;
	}
	
	public long countRecursivePartialFragment(TSNodeLabel partialFrag) {		
		long count = 0;
		if (this.matchNonRecursivePartialFragment(partialFrag)) {
			count++;
			//System.out.println(this);
		}
		if (this.isTerminal()) return count;
		for(TSNodeLabel d : this.daughters) {
			count += d.countRecursivePartialFragment(partialFrag);
		}
		return count;
	}
	
	public void countRecursivePartialFragmentInitial(TSNodeLabel partialFrag, long[] result, TSNodeLabel initialNode) {		
		if (this.matchNonRecursivePartialFragment(partialFrag)) {
			result[0]++;
			if (this.getLeftmostLexicalNode()==initialNode)
				result[1]++;
			//System.out.println(this);
		}
		if (this.isTerminal()) 
			return;
		for(TSNodeLabel d : this.daughters) {
			d.countRecursivePartialFragmentInitial(partialFrag, result, initialNode);
		}
	}
	
	public static ArrayList<TSNodeLabel> getTreebank(File inputFile) throws Exception {
		return getTreebank(inputFile, FileUtil.defaultEncoding, Integer.MAX_VALUE);
	}
	
	public static ArrayList<TSNodeLabel> getTreebank(File inputFile, int LL) throws Exception {
		return getTreebank(inputFile, FileUtil.defaultEncoding, LL);
	}
	
	public static ArrayList<TSNodeLabel> getTreebank(File inputFile, boolean allTermAreLex) throws Exception {
		return getTreebank(inputFile, FileUtil.defaultEncoding, Integer.MAX_VALUE, allTermAreLex);
	}
			
	public static ArrayList<TSNodeLabel> getTreebank(
			File inputFile, String encoding, int LL, boolean allTermAreLex) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		Scanner scan = FileUtil.getScanner(inputFile, encoding);
		//PrintProgress.start("Reading Treebank: ");
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.equals("")) continue;
			//PrintProgress.next();
			line = line.replaceAll("\\\\", "");			
			TSNodeLabel lineStructure = new TSNodeLabel(line, allTermAreLex);
			if (lineStructure.countLexicalNodes() > LL) continue;
			result.add(lineStructure);
		}
		//PrintProgress.end();
		return result;
	}
	
	public static ArrayList<TSNodeLabel> getTreebank(
			File inputFile, String encoding, int LL) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		Scanner scan = FileUtil.getScanner(inputFile, encoding);
		//PrintProgress.start("Reading Treebank: ");
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.equals("")) continue;			
			//PrintProgress.next();
			line = line.replaceAll("\\\\", "");			
			line = ConstCorpus.adjustParenthesisation(line);
			TSNodeLabel lineStructure = new TSNodeLabel(line);
			if (lineStructure.countLexicalNodes() > LL) continue;
			result.add(lineStructure);
		}
		//PrintProgress.end();
		return result;
	}
	
	public static ArrayList<TSNodeLabel> getTreebankFirst(
			File inputFile, int n) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		Scanner scan = FileUtil.getScanner(inputFile, FileUtil.defaultEncoding);
		PrintProgressStatic.start("Reading Treebank: ");
		int i = 0;
		while(scan.hasNextLine() && i<n) {			
			String line = scan.nextLine();
			if (line.equals("")) continue;
			PrintProgressStatic.next();
			TSNodeLabel lineStructure = new TSNodeLabel(line);
			result.add(lineStructure);
			i++;
		}
		PrintProgressStatic.end();
		return result;
	}
	
	public static void printTreebankToFile(File outputFile,
			ArrayList<TSNodeLabel> constTreebank, boolean heads, boolean lexQuotes) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TSNodeLabel t : constTreebank) {
			pw.println(t.toString(heads, lexQuotes));
		}
		pw.close();
	}	
	
	public static void printTreebankToFileFlat(File outputFile,
			ArrayList<TSNodeLabel> constTreebank) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TSNodeLabel t : constTreebank) {
			pw.println(t.toFlatSentence());
		}
		pw.close();
	}
	
	public static void getTreebankComment(File treebankFile,
			ArrayList<TSNodeLabel> treebank, ArrayList<String> treebankComments) throws Exception {		
		Scanner scan = FileUtil.getScanner(treebankFile, FileUtil.defaultEncoding);
		PrintProgressStatic.start("Reading Treebank: ");
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			line = line.trim();			
			if (line.equals("") || line.charAt(0)!='(') continue;
			String[] lineSplit = line.split("\t");
 			PrintProgressStatic.next();
			TSNodeLabel lineStructure = new TSNodeLabel(lineSplit[0]);			
			treebank.add(lineStructure);
			treebankComments.add(lineSplit.length==1 ? null : lineSplit[1]);
		}
		PrintProgressStatic.end();
	}
	
	public static void getTreebankCommentFromString(String treebankString,
			ArrayList<TSNodeLabel> treebank, ArrayList<String> treebankComments) throws Exception {				
		PrintProgressStatic.start("Reading Treebank: ");
		String[] treebankStringLines = treebankString.split("\n");
		for(String line : treebankStringLines) {			
			line = line.trim();			
			if (line.equals("") || line.charAt(0)!='(') continue;
			String[] lineSplit = line.split("\t");
 			PrintProgressStatic.next();
			TSNodeLabel lineStructure = new TSNodeLabel(lineSplit[0]);			
			treebank.add(lineStructure);
			treebankComments.add(lineSplit.length==1 ? null : lineSplit[1]);
		}
		PrintProgressStatic.end();
	}
	
	public static void makeTreebankClausureUnderSubsetRelation(
			File inputFile, File outputFile) throws Exception {
		ArrayList<TSNodeLabel> treebank = getTreebank(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ListIterator<TSNodeLabel> i1 = treebank.listIterator();
		PrintProgressStatic.start("Making closure:");
		while(i1.hasNext()) {
			PrintProgressStatic.next();
			TSNodeLabel t1 = i1.next();
			int nextIndex = i1.previousIndex()+1;
			ListIterator<TSNodeLabel> i2 = treebank.listIterator(nextIndex);
			boolean foundSuperSet=false;
			while(i2.hasNext()) {
				TSNodeLabel t2 = i2.next();
				if (t2.containsRecursiveFragment(t1)) {
					foundSuperSet = true;
					break;
				}
				if (t1.containsRecursiveFragment(t2)) {
					i2.remove();
				}
			}
			if (!foundSuperSet) pw.println(t1);
		}
		pw.close();
		PrintProgressStatic.end();
	}
	
	public String toFlatSentence() {		
		ArrayList<TSNodeLabel> lexItems = this.collectLexicalItems();
		StringBuilder result = new StringBuilder();
		result.append(lexItems.get(0).label());
		if (lexItems.size()==1) return result.toString();
		ListIterator<TSNodeLabel> i = lexItems.listIterator(1);
		while(i.hasNext()) {
			result.append(" " + i.next().label());
		}
		return result.toString();
	}
	
	public String[] toFlatWordArray() {		
		ArrayList<TSNodeLabel> lexItems = this.collectLexicalItems();
		String[] result = new String[lexItems.size()];
		int i = 0;
		for(TSNodeLabel l : lexItems) {
			result[i++] = l.label();
		}
		return result;
	}
	
	public String[] toPosArray() {		
		ArrayList<TSNodeLabel> lexItems = this.collectLexicalItems();
		String[] result = new String[lexItems.size()];
		int i = 0;
		for(TSNodeLabel l : lexItems) {
			result[i] = l.parent.label();
			i++;
		}
		return result;
	}
	
	public void adjustLexicalItems(String[] sentenceWords) {
		ArrayList<TSNodeLabel> lexItems = this.collectLexicalItems();
		Iterator<TSNodeLabel> lexIter = lexItems.iterator();
		for(String w : sentenceWords) {
			TSNodeLabel lexNode = lexIter.next();
			lexNode.label = Label.getLabel(w);
		}
	}
	
	public void removePreterminalWithPrefix(String prefix, int position) {
		if (this.isLexical) return;
		if (this.label().startsWith(prefix)) {
			TSNodeLabel lexDaughter = daughters[0];
			parent.daughters[position] = lexDaughter;
			lexDaughter.parent = this.parent;
			return;
		}
		position = 0;
		for(TSNodeLabel d : daughters) {			
			d.removePreterminalWithPrefix(prefix, position);
			position++;
		}		
	}
	
	public TSNodeLabel replaceRulesWithFragments(
			Hashtable<String, TSNodeLabel> ruleFragmentTable) {
		String cfgRule = this.cfgRule();
		TSNodeLabel fragment = ruleFragmentTable.get(cfgRule);		
		if (fragment==null) return this;
		TSNodeLabel result = fragment.clone();
		ArrayList<TSNodeLabel> terminals = result.collectTerminalItems();
		Iterator<TSNodeLabel> termIter = terminals.iterator();
		for(TSNodeLabel d : daughters) {
			TSNodeLabel term = termIter.next();
			if (term.isLexical) continue;
			TSNodeLabel subFragment = d.replaceRulesWithFragments(ruleFragmentTable);			
			term.daughters = subFragment.daughters;
			for(TSNodeLabel d1 : subFragment.daughters) {
				d1.parent = subFragment;
			}			
		}
		return result;
	}


	public int indexOfHeadDaughter() {
		int i=0;
		for(TSNodeLabel d : this.daughters) {
			if (d.headMarked) return i;
			i++;
		}
		return -1;
	}
	
	public int indexOfDaughter(TSNodeLabel d) {
		if (this.daughters==null) return -1;
		for(int i=0; i<prole(); i++) {
			if (this.daughters[i]==d) return i;
		}
		return -1;
	}

	public boolean findLastDaughterIN() {		
		if (this.isTerminal()) return false;
		if (this.parent!=null && this.parent.label().equals("VP") && this.daughters[this.prole()-1].label().equals("IN")) return true;
		for(TSNodeLabel d : daughters) {
			if (d.findLastDaughterIN()) return true;
		}
		return false;
	}	
	


	public boolean yieldsOneWord() {
		if (this.isPreLexical()) return true;
		if (this.prole()>1) return false;
		return this.daughters[0].yieldsOneWord();
	}

	/**
	 * The current node is not a terminal, and not a preterminal 
	 * @return true if all the daughters are not preterminals
	 */
	public boolean allDaughtersAreNonPreterminal() {		
		for(TSNodeLabel d : this.daughters) {
			if (d.isPreLexical()) return false;
		}
		return true;
	}

	public TSNodeLabel getHeadDaughter() {
		if (this.daughters==null) return null;
		for(TSNodeLabel d : daughters) {
			if (d.headMarked) return d;
		}
		return null;
	}
	
	public TSNodeLabel getRootNode() {
		if (parent==null)
			return this;
		return parent.getRootNode();
	}
	
	public TSNodeLabel getPosLexHead() {
		TSNodeLabel result = this;
		while(result!=null && !result.isPreLexical()) {
			result = result.getHeadDaughter();
		};
		return result;
	}

	public int countNonPreterminalDaughters() {
		if (this.daughters==null) return 0;
		int result = 0;
		for(TSNodeLabel d : daughters) {
			if (!d.isPreLexical()) result++;
		}
		return result;
	}
	
	public TSNodeLabel nextWord() {
		TSNodeLabel current = this;
		TSNodeLabel ancestor = this.parent;		
		do {
			if (ancestor==null) return null;
			TSNodeLabel rightSister = current.getRightSister();
			if (rightSister==null) {
				current = ancestor;
				ancestor = ancestor.parent;
			}
			else {
				do {
					rightSister = rightSister.daughters[0];					
				} while (!rightSister.isLexical);
				return rightSister;
			}
		} while (true);
	}
	
	public TSNodeLabel previousWord() {
		TSNodeLabel current = this;
		TSNodeLabel ancestor = this.parent;		
		do {
			if (ancestor==null) return null;
			TSNodeLabel leftSister = current.getLeftSister();
			if (leftSister==null) {
				current = ancestor;
				ancestor = ancestor.parent;
			}
			else {
				do {
					leftSister = leftSister.daughters[leftSister.prole()-1];					
				} while (!leftSister.isLexical);
				return leftSister;
			}
		} while (true);
	}

	public TSNodeLabel getRightSister() {
		int prole = parent.prole();
		if (prole==1) return null;
		int lastChildIndex = prole-1;
		TSNodeLabel[] sibling = parent.daughters;
		for(int i=0; i<lastChildIndex; i++) {
			TSNodeLabel n = sibling[i];
			if (n==this) return sibling[i+1];
		}
		return null;
	}
	
	public TSNodeLabel getLeftSister() {
		int prole = parent.prole();
		if (prole==1) return null;		
		TSNodeLabel[] sibling = parent.daughters;
		TSNodeLabel previous = null;
		for(int i=0; i<prole; i++) {
			TSNodeLabel n = sibling[i];
			if (n==this) return previous;
			previous = n;
		}
		return null;
	}

	public int headCount() {
		if (daughters==null) return 0;
		int result = 0;		
		for(TSNodeLabel d : daughters) {
			if (d.headMarked) result++;
		}
		return result;
	}

	public void fillHeadDaughters(TSNodeLabel[] heads) {
		int i=0;
		for(TSNodeLabel d : daughters) {
			if (d.headMarked) heads[i++] = d;
		}		
	}
	
	/**
	 * Prune the subtrees labeled by the input label from the current tree 
	 */
	public boolean pruneSubTrees(String tag) {
		if (this.toString().indexOf(tag)==-1) return false;
		if (this.label().equals(tag)) return true;
		if (this.isTerminal()) return false;
		// contains traces
		ArrayList<TSNodeLabel> nonTraces = new ArrayList<TSNodeLabel>();
		for(int i=0; i<this.daughters.length; i++) {
			TSNodeLabel D = this.daughters[i];
			if (!D.pruneSubTrees(tag)) nonTraces.add(D);
		}
		if (nonTraces.size() == this.daughters.length) return false;
		if (nonTraces.isEmpty()) return true;
		if (nonTraces.size()==1) {
			TSNodeLabel uniqueDaughter = nonTraces.get(0);
			if (uniqueDaughter.sameLabel(this)) {
				this.daughters = uniqueDaughter.daughters;
				for(TSNodeLabel d : this.daughters) {
					d.parent = this;
				}
				return false;
			}
		}
		this.daughters = (TSNodeLabel[]) nonTraces.toArray(new TSNodeLabel[nonTraces.size()]);
		return false;
	}
	
	public boolean pruneSubTreesStartingWith(String tagStart) {
		if (this.toString().indexOf(tagStart)==-1) return false;
		if (this.label().startsWith(tagStart)) return true;
		if (this.isTerminal()) return false;
		// contains traces
		ArrayList<TSNodeLabel> nonTraces = new ArrayList<TSNodeLabel>();
		for(int i=0; i<this.daughters.length; i++) {
			TSNodeLabel D = this.daughters[i];
			if (!D.pruneSubTreesStartingWith(tagStart)) nonTraces.add(D);
		}
		if (nonTraces.size() == this.daughters.length) return false;
		if (nonTraces.isEmpty()) return true;
		if (nonTraces.size()==1) {
			TSNodeLabel uniqueDaughter = nonTraces.get(0);
			if (uniqueDaughter.sameLabel(this)) {
				this.daughters = uniqueDaughter.daughters;
				for(TSNodeLabel d : this.daughters) {
					d.parent = this;
				}
				return false;
			}
		}
		this.daughters = (TSNodeLabel[]) nonTraces.toArray(new TSNodeLabel[nonTraces.size()]);
		return false;
	}
	
	public boolean pruneSubTreesMatching(String regex) {
		if (this.label().matches(regex)) return true;
		if (this.isTerminal()) return false;
		// contains traces
		ArrayList<TSNodeLabel> nonTraces = new ArrayList<TSNodeLabel>();
		for(int i=0; i<this.daughters.length; i++) {
			TSNodeLabel D = this.daughters[i];
			if (!D.pruneSubTreesMatching(regex)) nonTraces.add(D);
		}
		if (nonTraces.size() == this.daughters.length) return false;
		if (nonTraces.isEmpty()) return true;
		if (nonTraces.size()==1) {
			TSNodeLabel uniqueDaughter = nonTraces.get(0);
			if (uniqueDaughter.sameLabel(this)) {
				this.daughters = uniqueDaughter.daughters;
				for(TSNodeLabel d : this.daughters) {
					d.parent = this;
				}
				return false;
			}
		}
		this.daughters = (TSNodeLabel[]) nonTraces.toArray(new TSNodeLabel[nonTraces.size()]);
		return false;
	}
	
	/*
	 * Remove daughters to phrasal nodes not yielding to lexical items
	 */
	public boolean prunePhraseNodeNotYieldingLexicalItems() {
		if (!this.isTerminal()) {
			if (!this.yieldsLexicalItems()) {
				this.daughters = null;
				return true;
			}
			boolean pruned = false;
			for(TSNodeLabel d : this.daughters) {
				if (d.prunePhraseNodeNotYieldingLexicalItems())
					pruned = true;
			}				
			return pruned;
		}
		return false;
			
	}
	
	/**
	 * Prune the subtrees labeled by one of the label in the input array removeLabels
	 * from the current tree. The input array removeLabels should be sorted beforehand
	 * (as for instance with Arrays.sort(removeLabels); 
	 */
	public TSNodeLabel pruneSubTrees(String[] removeLabels) {
		if (Arrays.binarySearch(removeLabels, this.label())>=0) return this;
		if (this.isTerminal()) return null;
		// contains traces
		LinkedList<TSNodeLabel> toKeep = new LinkedList<TSNodeLabel>();
		for(int i=0; i<this.daughters.length; i++) {
			TSNodeLabel D = this.daughters[i];
			if (D.pruneSubTrees(removeLabels)==null) toKeep.add(D);
		}
		if (toKeep.size() == this.daughters.length) return null;
		if (toKeep.isEmpty()) return this;
		this.daughters = (TSNodeLabel[]) toKeep.toArray(new TSNodeLabel[toKeep.size()]);
		return null;
	}
	
	/**
	 * Prune the lexicon items labeled by one of the label in the input array removeLex
	 * from the current tree. The input array removeLabels should be sorted beforehand
	 * (as for instance with Arrays.sort(removeLabels); 
	 */
	public TSNodeLabel pruneLex(String[] removeLex) {
		if (this.isPreLexical()) {
			if (Arrays.binarySearch(removeLex, this.daughters[0].label())>=0) return this;
			return null;
		}
		LinkedList<TSNodeLabel> toKeep = new LinkedList<TSNodeLabel>();
		for(int i=0; i<this.daughters.length; i++) {
			TSNodeLabel D = this.daughters[i];
			if (D.pruneLex(removeLex)==null) toKeep.add(D);
		}
		if (toKeep.size() == this.daughters.length) return null;
		if (toKeep.isEmpty()) return this;
		this.daughters = (TSNodeLabel[]) toKeep.toArray(new TSNodeLabel[toKeep.size()]);
		return null;
	}
	
	public void pruneAllLex() {
		if (this.isLexical) {
			this.parent.daughters = null;
			return;
		}		
		if (this.isTerminal())
			return;
		for(TSNodeLabel d : this.daughters) {
			d.pruneAllLex();
		}
	}
	
	public void pruneAndCollectAllLex(Collection<TSNodeLabel> col) {
		if (this.isLexical) {
			this.parent.daughters = null;
			col.add(this);
			return;
		}		
		if (this.isTerminal())
			return;
		for(TSNodeLabel d : this.daughters) {
			d.pruneAndCollectAllLex(col);
		}
	}
	
	public TSNodeLabel prunePos(String[] removeLex) {
		if (this.isPreLexical()) {
			if (Arrays.binarySearch(removeLex, this.label())>=0) return this;
			return null;
		}
		LinkedList<TSNodeLabel> toKeep = new LinkedList<TSNodeLabel>();
		for(int i=0; i<this.daughters.length; i++) {
			TSNodeLabel D = this.daughters[i];
			if (D.prunePos(removeLex)==null) toKeep.add(D);
		}
		if (toKeep.size() == this.daughters.length) return null;
		if (toKeep.isEmpty()) return this;
		this.daughters = (TSNodeLabel[]) toKeep.toArray(new TSNodeLabel[toKeep.size()]);
		return null;
	}
	
	/**
	 * Returns the yield of the current TreeNode i.e. all the words
	 * it dominates separated by single spaces.
	 */
	public String getYield() {
		List<TSNodeLabel> terminals = this.collectLexicalItems();
		String result = "";
		for(TSNodeLabel TN : terminals) {
			result += TN.label() + " ";
		}
		result = result.trim();
		return result;
	}
	
	/**
	 * This method compute all subtrees up to depth maxDepth in 
	 * the current TreeNode.
	 * @param maxDepth maxDepth of subtrees
	 * @return a LinkedList of TreeNode each being a subtree of maximum depth maxDepth
	 */
	public ArrayList<String> allSubTrees(int maxDepth, int maxProle) {
		Pair<ArrayList<String>> duet = this.allSubTreesB(maxDepth, maxProle);
		ArrayList<String> result = duet.getFirst();
		result.addAll(duet.getSecond());
		return result;
	}
	
	/**
	 * Method to cumpute allSubTrees(int maxDepth))
	 */
	private Pair<ArrayList<String>> allSubTreesB(int maxDepth, int maxProle) {
		// subTrees[0] = NEW subTrees;
		// subTrees[1] = OLD subTrees;
		Pair<ArrayList<String>> subTrees = 
			new Pair<ArrayList<String>> (new ArrayList<String>(), new ArrayList<String>());
		if (this.isTerminal()) return subTrees;
		int prole = this.daughters.length;
		if (prole>maxProle) {
			for(TSNodeLabel TN : this.daughters) {
				Pair<ArrayList<String>> daugherSubTreesNewOld = TN.allSubTreesB(maxDepth, maxProle);
				subTrees.getSecond().addAll(daugherSubTreesNewOld.getFirst());
				subTrees.getSecond().addAll(daugherSubTreesNewOld.getSecond());
			}
			subTrees.getFirst().add("(" + this.cfgRule() + ")");
		}
		else {
			ArrayList<ArrayList<String>> daughterSubTrees = new ArrayList<ArrayList<String>>(prole);
			int[] daughterSubTreesSize = new int[prole];
			int index = 0;
			for(TSNodeLabel TN : this.daughters) {				
				Pair<ArrayList<String>> daugherSubTreesNewOld = TN.allSubTreesB(maxDepth, maxProle);			
				subTrees.getSecond().addAll(daugherSubTreesNewOld.getFirst());
				subTrees.getSecond().addAll(daugherSubTreesNewOld.getSecond());
				ArrayList<String> TNlist = daugherSubTreesNewOld.getFirst();
				TNlist.add(TN.label(false, true));
				daughterSubTrees.add(TNlist);
				for(ListIterator<String> l = TNlist.listIterator(); l.hasNext();) {
					String sT = (String)l.next();
					TSNode TNsT = new TSNode(sT, false);
					if (TNsT.maxDepth(true)==maxDepth) l.remove();
				}
				daughterSubTreesSize[index] = TNlist.size();
				index++;
			}
			int[][] combinations = Utility.combinations(daughterSubTreesSize);
			for(int i=0; i<combinations.length; i++) {
				String rule = "(" + this.label(false, false) + " ";
				for(int j=0; j<prole; j++) {
					String sT = (String)daughterSubTrees.get(j).get(combinations[i][j]);				
					rule += sT;
					rule += (j==prole-1) ? ")" : " ";
				}
				subTrees.getFirst().add(rule);
			}		
		}
		return subTrees;
	}

	public static Hashtable<String, HashSet<String>> getLexPosTableFromTreebank(
			ArrayList<TSNodeLabel> treebank) {
		Hashtable<String, HashSet<String>> result = new Hashtable<String, HashSet<String>>(); 
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for(TSNodeLabel l : lex) {
				String l_label = l.label(); 
				HashSet<String> lStoredPos = result.get(l_label);
				if (lStoredPos==null) {
					lStoredPos = new HashSet<String>();
					result.put(l_label, lStoredPos);
				}
				lStoredPos.add(l.parent.label());
			}
		}
		return result;
	}	
	
	public static HashSet<String> getPosSetFromTreebank(ArrayList<TSNodeLabel> treebank) {
		HashSet<String> result = new HashSet<String>(); 
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for(TSNodeLabel l : lex) {								
				result.add(l.parent.label());
			}
		}
		return result;
	}
	
	/**
	 * Returns a LinkedList of type String containing the terminals of
	 * the current TreeNode. 
	 */
	public List<String> collectTerminalStrings() {		
		List<TSNodeLabel> terminals = this.collectTerminalItems();
		List<String> result = new ArrayList<String>(terminals.size());
		for(TSNodeLabel TN : terminals) result.add(TN.toString(false, false));
		return result;
	}

	/**
	 * Returns the left most daughter of the current tree
	 * @return
	 */
	public TSNodeLabel firstDaughter() {
		return this.daughters[0];
	}
	
	public TSNodeLabel secondDaughter() {
		return this.daughters[1];
	}
	
	/**
	 * Returns the right most daughter of the current tree
	 * @return
	 */
	public TSNodeLabel lastDaughter() {
		return this.daughters[this.prole()-1];
	}
	
	/**
	 * Returns the leftmost descendent of the current treenode
	 * @return
	 */
	public TSNodeLabel getLeftmostTerminalNode() {
		if (this.isTerminal()) return this;
		return this.firstDaughter().getLeftmostTerminalNode();		
	}
	
	public TSNodeLabel getRightmostTerminalNode() {
		if (this.isTerminal()) return this;
		return this.lastDaughter().getRightmostTerminalNode();		
	}
	
	public TSNodeLabel getLeftmostLexicalNode() {
		if (this.isLexical) return this;
		if (this.isTerminal()) return null;
		for (TSNodeLabel d : this.daughters) {
			TSNodeLabel result = d.getLeftmostLexicalNode();
			if (result!=null)
				return result;
		}
		return null;
	}
	
	public TSNodeLabel getRightmostLexicalNode() {
		if (this.isLexical) return this;
		if (this.isTerminal()) return null;
		for (int i=this.prole()-1; i>=0; i--) {
			TSNodeLabel d = this.daughters[i];
			TSNodeLabel result = d.getRightmostLexicalNode();
			if (result!=null)
				return result;
		}
		return null;
	}
	
	public TSNodeLabel getLeftmostSubSite() {
		if (this.isLexical) return null; 
		if (this.isTerminal()) return this;
		for (TSNodeLabel d : this.daughters) {
			TSNodeLabel result = d.getLeftmostSubSite();
			if (result!=null)
				return result;
		}
		return null;		
	}
	
	/**
	 * Returns the leftmost descendent prelexical item of the current treenode
	 * @return
	 */
	public TSNodeLabel getLeftmostPreTerminal() {
		if (this.isPreTerminal()) return this;
		return this.firstDaughter().getLeftmostPreTerminal();		
	}
	
	/**
	 * Returns the rightmost descendent of the current treenode
	 * @return
	 */
	public TSNodeLabel getRightmostTerminal() {
		while(!this.isTerminal()) {
			return this.lastDaughter().getRightmostTerminal();
		}
		return this;
	}
	
	
	/**
	 * Remove the current node from its parent, but add all its children to the
	 * parent node at the same position.
	 */
	public void removeCurrentNode() {
		TSNodeLabel p = this.parent;
		int position = p.indexOfDaughter(this);
		int prole = this.prole();
		int parentProle = parent.prole();
		int newParentProle = parentProle - 1 + prole;
		TSNodeLabel[] newDaughters = new TSNodeLabel[newParentProle];
		int oldIndex = 0;
		boolean addedOldDaughters = false;
		for(int i=0; i<newParentProle; i++) {
			if (addedOldDaughters || i<position) {
				newDaughters[i] = parent.daughters[oldIndex++];
			}
			else if (i==position) {
				oldIndex++;
				for(int j=0; j<prole; j++) {
					TSNodeLabel d = this.daughters[j];
					d.parent = parent;
					newDaughters[i++] = d;
				}
				i--;
				addedOldDaughters = true;
			}
		}
		parent.daughters = newDaughters;
	}
	
	
	public static long countPartialFragmentInTreebank(ArrayList<TSNodeLabel> treebank, TSNodeLabel target) {
		int count = 0;
		for(TSNodeLabel t : treebank) {
			count += t.countRecursivePartialFragment(target);
		}
		return count;
	}
	
	public static long[] countPartialFragmentInTreebankInitial(ArrayList<TSNodeLabel> treebank, TSNodeLabel target) {
		long[] count = new long[]{0,0};
		TSNodeLabel fragFirstLex = target.getLeftmostLexicalNode(); 
		for(TSNodeLabel t : treebank) {
			TSNodeLabel initialNode = null;
			if (fragFirstLex!=null) {
				initialNode = t.getLeftmostLexicalNode();
				if (fragFirstLex.label!=initialNode.label)
					initialNode = null;		
			}
			t.countRecursivePartialFragmentInitial(target,count,initialNode);
		}
		return count;
	}
	
	public static int countFragmentInTreebank(ArrayList<TSNodeLabel> treebank, TSNodeLabel target) {
		int count = 0;
		for(TSNodeLabel t : treebank) {
			count += t.countRecursiveFragment(target);
		}
		return count;
	}
	
	public static long[] countFragmentInTreebankInitialFinal(ArrayList<TSNodeLabel> treebank, TSNodeLabel target) {
		long[] count = new long[]{0,0,0};
		TSNodeLabel fragFirstLex = target.getLeftmostTerminalNode();
		if (!fragFirstLex.isLexical)
			fragFirstLex = null;
		TSNodeLabel fragLastLex = target.getRightmostTerminalNode();
		if (!fragLastLex.isLexical)
			fragLastLex = null;
		for(TSNodeLabel t : treebank) {
			TSNodeLabel initialNode = null;
			if (fragFirstLex!=null) {
				initialNode = t.getLeftmostLexicalNode();
				if (fragFirstLex.label!=initialNode.label)
					initialNode = null;					
			}
			TSNodeLabel finalNode = null;
			if (fragLastLex!=null) {
				finalNode = t.getRightmostLexicalNode();
				if (fragLastLex.label!=finalNode.label)
					finalNode = null;					
			}
			t.countRecursiveFragmentInitialFinal(target, count, initialNode, finalNode);
		}
		return count;
	}
	
	public static void removeSemanticTags(ArrayList<TSNodeLabel> treebank) {
		for(TSNodeLabel t : treebank) t.removeSemanticTags();
	}
	
	public static void replaceLabelTreebank(ArrayList<TSNodeLabel> treebank, String oldLabel, String newLabel) {
		for(TSNodeLabel inputTree : treebank) {
			inputTree.replaceLabels(oldLabel, newLabel);
		}
	}
	
	
	
	/**
	 * Check if there are coordinated structure in which the two coordinants are
	 * not specified as separate constituents. It only applies to constituents yielding
	 * PoS tags only.
	 * @return
	 */
	public void fixCCPatterns(String[] CClabels, String[] ignoreInitialLabels) {
		if (this.isPreLexical()) return;
		BitSet indexesCC = this.indexDaughtersWithLabels(CClabels);	
		if (indexesCC!=null) {
			int prole = this.prole();
			int startIndex = 0;
			for(int di=0; di<prole; di++) {
				TSNodeLabel d = this.daughters[di];
				if (Arrays.binarySearch(ignoreInitialLabels, d.label())<0) {
					startIndex = di;
					break;
				}
			}			
			int patternsNumber = indexesCC.cardinality()+1;			
			Vector<ArrayList<String>> patterns = new Vector<ArrayList<String>>(patternsNumber);
			for(int i=0; i<patternsNumber; i++) patterns.add(new ArrayList<String>());
			int currentPatternIndex = 0;
			for(int di=startIndex; di<prole; di++) {
				if (indexesCC.get(di)) {
					currentPatternIndex++;
					continue;
				}
				TSNodeLabel d = this.daughters[di];
				patterns.get(currentPatternIndex).add(d.label());								
			}
			ArrayList<String> firstPattern = patterns.get(0);
			int patternSize = firstPattern.size();
			if (patternSize>1) {
				boolean equalPatterns = true;				
				for(int i=1; i<patternsNumber; i++) {
					if (!patterns.get(i).equals(firstPattern)) {
						equalPatterns = false;
						break;
					}
				}
				if (equalPatterns) {
					String groupsLabel = this.label();						
					String lineReport = "Building separate constituents for coordination patter: " + this.toStringOneLevel() + " ---> ";
					int bI = startIndex -2;
					for(int i=0; i<patternsNumber; i++) {
						bI += 2;
						int eI = bI + patternSize - 1;
						this.groupConsecutiveDaughters(bI, eI, groupsLabel);					
					}
					lineReport += this.toStringOneLevel();
					Parameters.logPrintln(lineReport);
				}
			}
		}	
		for(TSNodeLabel d : this.daughters) {
			d.fixCCPatterns(CClabels,ignoreInitialLabels);
		}
		
	}
	
	public int countDaughtersWithLabel(String[] labels) {
		if (this.isLexical) return 0;
		int count = 0;
		boolean found = false;
		for(TSNodeLabel d : this.daughters) {
			if (Arrays.binarySearch(labels, d.label())>=0) count++;
		}
		if (found) count++;
		return count;
	}
	
	public boolean yieldNonLexicalNodeStartingWith(String prefix) {
		if (this.isLexical) return false;
		for(TSNodeLabel d : daughters) {
			if (d.label().startsWith(prefix) || d.yieldNonLexicalNodeStartingWith(prefix)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasDaughterStartingWith(String labelPrefix) {				
		for(int di=0; di<prole(); di++) {
			TSNodeLabel d = this.daughters[di];
			if (d.label().startsWith(labelPrefix)) return true;
		}
		return false;
	}	
	
	public boolean yieldPrelexicalStartingWith(String labelPrefix) {
		ArrayList<TSNodeLabel> lex = this.collectLexicalItems();
		for(TSNodeLabel l : lex) {
			if (l.parent.label().startsWith(labelPrefix)) return true;
		}
		return false;
	}
	
	public boolean yieldsLexicalItems() {
		if (this.isLexical)
			return true;
		if (this.isTerminal())
			return false;
		for(TSNodeLabel d : this.daughters) {
			if (d.yieldsLexicalItems())
				return true;
		}
		return false;
	}
	
	public boolean hasDaughterWithLabel(Label l) {				
		for(int di=0; di<prole(); di++) {
			TSNodeLabel d = this.daughters[di];
			if (d.label.equals(l)) return true;
		}
		return false;
	}
	
	public TSNodeLabel getDaughterWithLabel(Label l) {				
		for(int di=0; di<prole(); di++) {
			TSNodeLabel d = this.daughters[di];
			if (d.label.equals(l)) return d;
		}
		return null;
	}
	
	public TSNodeLabel getDaughterWithLabelStartingWith(String prefix) {				
		for(int di=0; di<prole(); di++) {
			TSNodeLabel d = this.daughters[di];
			if (d.label().startsWith(prefix)) return d;
		}
		return null;
	}
	
	public BitSet indexDaughtersWithLabels(String[] labels) {		
		BitSet result = new BitSet();
		for(int di=0; di<prole(); di++) {
			TSNodeLabel d = this.daughters[di];
			if (Arrays.binarySearch(labels, d.label())>=0) {
				result.set(di);
			}
		}
		return result.isEmpty() ? null : result;
	}
	
	public static int maxDepthTreebank(ArrayList<TSNodeLabel> treebank) {
		int maxDepth = -1;		
		for(TSNodeLabel t : treebank) {			
			int depth = t.maxDepth();
			if (depth>maxDepth) maxDepth = depth;
		}
		return maxDepth;
	}
	
	public static int maxBranchingTreebank(ArrayList<TSNodeLabel> treebank) {
		int maxBranching = -1;		
		for(TSNodeLabel t : treebank) {			
			int branching = t.maxBranching();
			if (branching>maxBranching) maxBranching = branching;
		}
		return maxBranching;
	}
	
	public void groupConsecutiveDaughters(int beginIndex, int endIndex, String groupLabel) {
		int groupSize = endIndex-beginIndex+1;
		int prole = this.prole();
		TSNodeLabel groupNode = new TSNodeLabel();
		groupNode.label = Label.getLabel(groupLabel);
		groupNode.parent = this;
		TSNodeLabel[] newGroupDaughters = new TSNodeLabel[groupSize];				 
		groupNode.daughters = newGroupDaughters;
		int k=0;
		for(int j=beginIndex; j<=endIndex; j++) {
			newGroupDaughters[k] = this.daughters[j];
			newGroupDaughters[k].parent = groupNode;
			k++;
		}
		int newProle = prole - groupSize + 1;
		TSNodeLabel[] newDaughters = new TSNodeLabel[newProle];
		k=0;
		for(int j=0; j<prole; j++) {
			if (j==beginIndex) {
				newDaughters[k++] = groupNode;
				continue;
			}
			if (j>beginIndex && j<=endIndex) continue;
			newDaughters[k++] = this.daughters[j];
		}
		this.daughters = newDaughters;
	}
	
	/**
	 * Labels should be PoS (e.g. NNP, NNPS)
	 * @param labels
	 * @param groupingLabel
	 */
	public void groupConsecutiveDaughters(String[] labels, String groupLabel) {
		groupConsecutiveDaughters(labels, groupLabel,0);
	}
	
	private void groupConsecutiveDaughters(String[] labels, String groupLabel, int startIndex) {
		if (this.isLexical) return;
		int beginIndex = -1;
		int endIndex = -1;
		int prole = this.prole();
		while(startIndex<prole) {
			TSNodeLabel d = this.daughters[startIndex];
			if (Arrays.binarySearch(labels, d.label())>=0) {
				if (beginIndex==-1) beginIndex = startIndex;
				endIndex = startIndex;
			}
			else {
				if (endIndex!=-1) break;				
			}
			startIndex++;
		}				
		if (beginIndex!=-1) {
			int groupSize = endIndex-beginIndex+1;
			if (groupSize>1 && groupSize!=prole) {
				String lineReport = "Grouped nodes: " + this.toStringOneLevel() + " --> ";
				groupConsecutiveDaughters(beginIndex, endIndex, groupLabel);				
				lineReport += this.toStringOneLevel();
				Parameters.logPrintln(lineReport);								
			}					
		}
		if (startIndex<prole) this.groupConsecutiveDaughters(labels, groupLabel, startIndex);
		else for(TSNodeLabel d : this.daughters) d.groupConsecutiveDaughters(labels, groupLabel,0);	
	}
	
	public void removeUniqueProductions(String[] labels) {
		if (this.isLexical) return;		
		int i = 0;
		for(TSNodeLabel d : daughters) {			
			if (d.hasUniqueDaugher(labels)) {				
				this.daughters[i] = this.daughters[i].daughters[0];
				this.daughters[i].parent = this;
			}
			else d.removeUniqueProductions(labels);
			i++;
		}				
	}
	
	public boolean hasUniqueDaugher(String[] labels) {
		if (this.isLexical || this.label().indexOf('-')>0) return false;
		if (this.prole()>1) return false;
		String firstLabel = this.daughters[0].label(); 
		return (Arrays.binarySearch(labels, firstLabel)>=0);
	}
	
	public boolean hasUniquePathToLexicalItem() {
		if (this.isLexical) return true;
		if (this.prole()>1) return false;
		return (this.daughters[0].hasUniquePathToLexicalItem());
	}
	
	public void fixPunctInNNP() {
		if (this.isLexical) return;
		if ((this.label().startsWith("NP") || this.label().startsWith("NML")) && this.prole()==2) {
			TSNodeLabel firstDaughter = this.daughters[0];
			TSNodeLabel secondDaughter = this.daughters[1];
			if (firstDaughter.label().startsWith("NNP") && secondDaughter.label().equals(".")) {
				String lineReport = "Fix dot in NNP: " + this.toStringOneLevel() + " --> ";
				TSNodeLabel term = firstDaughter.daughters[0];
				term.relabel(term.label()+"."); 			
				this.daughters = new TSNodeLabel[]{firstDaughter};
				lineReport += this.toStringOneLevel();
			}				
		}
		for(TSNodeLabel d : daughters) {
			d.fixPunctInNNP();
		}
	}

	
	//public static String regexMatch = ".*[-=]\\d+.*";
	public static String dashEqualDigits = "[-=]\\d+";
	public static String nullTag = "-NONE-";	
	/**
	 * Remove removeNullProductions, removeNumbersInLables (traces), removeRedundantRules (X->X)
	 */
	public void makeCleanWsj() {	
		this.pruneSubTrees(nullTag);
		this.replaceAllNonTerminalLabels(dashEqualDigits, "");
		this.removeRedundantRules();		
	}
	
	public static Label WHNP_label = Label.getLabel("WHNP");
	
	public void findWHNP(int line) {
		if (this.isLexical) return;
		if (this.label.equals(WHNP_label) && prole()>1) System.out.println(line + ":" + this);
		for(TSNodeLabel d : daughters) d.findWHNP(line);
	}
	
	public static void findWHNP(File inputFile) throws Exception {
		Scanner scan = FileUtil.getScanner(inputFile);
		int lineNumber = 1;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			tree.findWHNP(lineNumber);
			lineNumber++;
		}
	}
	
	public void collectNodesPos(TreeSet<String> nodeLabels, TreeSet<String> posLabels) {		
		if (this.isLexical) return;
		if (this.isPreLexical()) {
			posLabels.add(this.label());
			return;
		}
		nodeLabels.add(this.label());
		for(TSNodeLabel d : daughters) d.collectNodesPos(nodeLabels, posLabels); 
	}
	
	public void markCircumstantial(String[] advSemTagSorted, String suffix) {
		if (this.isLexical || this.isPreLexical()) return;		
		String label = this.label();
		String[] semTags = label.split("[-=]");
		if (semTags.length>1) {
			for(int i=1; i<semTags.length; i++) {
				if (Arrays.binarySearch(advSemTagSorted, semTags[i])>=0) {
					String newLabel = semTags[0] + suffix + label.substring(semTags[0].length());					
					this.relabel(newLabel);
					break;
				}
			}
		}		
		for(TSNodeLabel d : daughters) d.markCircumstantial(advSemTagSorted, suffix);
	}	
	
	public boolean samePos(TSNodeLabel other) {
		ArrayList<TSNodeLabel> thisLex = this.collectLexicalItems();
		ArrayList<TSNodeLabel> otherLex = other.collectLexicalItems();
		if (thisLex.size()!=otherLex.size()) return false;
		Iterator<TSNodeLabel> thisLexIter = thisLex.iterator();
		Iterator<TSNodeLabel> otherLexIter = otherLex.iterator();
		while(thisLexIter.hasNext()) {
			TSNodeLabel thisL = thisLexIter.next();
			TSNodeLabel otherL = otherLexIter.next();
			if (!thisL.parent.label.equals(otherL.parent.label)) return false;
		}
		return true;
	}
	
	public static void removeSemanticTagsInTreebank(ArrayList<TSNodeLabel> treebank) {
		for(TSNodeLabel t : treebank) {
			t.removeSemanticTags();
		}						
	}
	
	/**
	 * Remove the numbers at the end of the tags:
	 * NP-sbj-3 --> NP-sbj
	 * Doesn't remove the semantic tags and doesn't affect the heads marking
	 */
	public void removeNumberInLabels() {
		if (this.isTerminal()) return;
		String currentLabel = this.label();
		String newLabel = currentLabel.replaceAll("-\\d+", "").replaceAll("=\\d+", "");
		if (!currentLabel.equals(newLabel)) {
			this.relabel(newLabel);
		}
		for(int i=0; i<this.daughters.length; i++) {
			this.daughters[i].removeNumberInLabels();
		}
	}
	
	public void removeDashesInLabels() {
		if (this.isTerminal()) return;
		String currentLabel = this.label();
		String newLabel = currentLabel.replaceAll("#\\d+", "");
		if (!currentLabel.equals(newLabel)) {
			this.relabel(newLabel);
		}
		for(int i=0; i<this.daughters.length; i++) {
			this.daughters[i].removeDashesInLabels();
		}
	}
	
	
	
	/**
	 * Remove the input daughter from the current treenode 
	 * @param index the daughter index to be removed from the daughters 
	 * of the current treenode 
	 */
	public void pruneDaughter(int index) {
		int prole = this.prole();
		TSNodeLabel[] newDaughters = prole==1 ? null : new TSNodeLabel[prole-1];
		int i = 0;
		for(int j=0; j<prole; j++) {
			if (j==index) continue;
			newDaughters[i] = this.daughters[j];
			i++;
		}
		this.daughters = newDaughters;
	}
	
	public void pruneDaughter(TSNodeLabel d) {
		int prole = this.prole();
		TSNodeLabel[] newDaughters =  prole==1 ? null : new TSNodeLabel[prole-1];
		int i = 0;
		for(int j=0; j<prole; j++) {
			if (this.daughters[j]==d) continue;
			newDaughters[i] = this.daughters[j];
			i++;
		}
		this.daughters = newDaughters;
	}
	
	/**
	 * Remove the input daughter from the current treenode 
	 * @param index the daughter index to be removed from the daughters 
	 * of the current treenode 
	 */
	public void pruneDaughtersAfter(int index) {
		if (index==this.prole()-1)
			return;
		int newSize = index+1;
		TSNodeLabel[] newDaughters = new TSNodeLabel[newSize];
		for(int j=0; j<this.daughters.length; j++) {
			if (j>index) break;
			newDaughters[j] = this.daughters[j];
		}
		this.daughters = newDaughters;
	}
	
	public void pruneDaughtersBefore(int index) {		
		if (index==0)
			return;
		int prole = this.prole();
		int newSize = prole-index;
		TSNodeLabel[] newDaughters = new TSNodeLabel[newSize];
		int i=0;
		for(int j=0; j<this.daughters.length; j++) {
			if (j<index) continue;
			newDaughters[i++] = this.daughters[j];
		}
		this.daughters = newDaughters;
	}

	
	/**
	 * Inser a new daughter in the daughters of the current tree
	 * at a given position
	 * @param daughter
	 * @param newPosition
	 */
	public void insertNewDaughter(TSNodeLabel daughter, int newPosition) {
		TSNodeLabel[] newDaughters = new TSNodeLabel[this.daughters.length+1];
		int i=0;
		for(int j=0; j<newDaughters.length; j++) {
			if (j==newPosition) newDaughters[j] = daughter;
			else {
				newDaughters[j] = this.daughters[i];
				i++;
			}
		}
		daughter.parent = this;
		this.daughters = newDaughters;
	}

	/**
	 * Raising punctuation hat occurs at the very beginning or end of a sentence, so that
	 * it always sits between two other nonterminals
	 */	
	public void raisePunctuation(String[] punctuationSorted) {
		ArrayList<TSNodeLabel> lexicon = this.collectLexicalItems();
		for(TSNodeLabel leaf : lexicon) {
			TSNodeLabel postag = leaf.parent;
			if (Arrays.binarySearch(punctuationSorted, postag.label())>=0) {
				if (postag.isUniqueDaughter()) {
					//System.out.println("Nodes that only dominate punctuation preterminals: " + this);
					continue;
				}
				TSNodeLabel postagParent = postag.parent;					
				int prole = postagParent.prole();
				int index = postagParent.indexOfDaughter(postag);
				if (index==0) {	
					TSNodeLabel parentNode = postagParent;					
					while (parentNode.parent!=null && parentNode==parentNode.parent.firstDaughter()) {
						parentNode=parentNode.parent;
					}							
					if (parentNode.parent==null) continue;
					postagParent.pruneDaughter(index);
					int newPosition = parentNode.parent.indexOfDaughter(parentNode);										
					parentNode.parent.insertNewDaughter(postag, newPosition);
				}
				else if (index==prole-1) {
					TSNodeLabel parentNode = postagParent;					
					while (parentNode.parent!=null && parentNode==parentNode.parent.lastDaughter()) {
						parentNode=parentNode.parent;
					}							
					if (parentNode.parent==null) continue;
					postagParent.pruneDaughter(index);
					int newPosition = parentNode.parent.indexOfDaughter(parentNode)+1;
					parentNode.parent.insertNewDaughter(postag, newPosition);
				}
			}			
		}
		
	}
	
	public boolean hasSamePosTag(TSNodeLabel g) {
		ArrayList<TSNodeLabel> thisPreLex = this.collectPreLexicalItems();
		ArrayList<TSNodeLabel> gPreLex = g.collectPreLexicalItems();
		if (thisPreLex.size() != gPreLex.size()) return false;
		Iterator<TSNodeLabel> thisPreLexIter = thisPreLex.iterator();
		Iterator<TSNodeLabel> gPreLexIter = gPreLex.iterator();
		while(thisPreLexIter.hasNext()) {
			TSNodeLabel thisLex = thisPreLexIter.next();
			TSNodeLabel gLex = gPreLexIter.next();
			if (!thisLex.label.equals(gLex.label)) return false;
		}
		return true;
		
	}

	public static Label startWordChar = Label.getLabel("<w>");
	public static Label endWordChar = Label.getLabel("</w>");
	
	public void makeTreeCharBased() {
		ArrayList<TSNodeLabel> lex = this.collectLexicalItems();
		for(TSNodeLabel l : lex) {
			TSNodeLabel pos = l.parent;
			String word = l.label();
			char[] charArray = word.toCharArray();
			int length = charArray.length;
			TSNodeLabel[] newDaughters = new TSNodeLabel[length+2];
			newDaughters[0] = new TSNodeLabel(startWordChar, true);
			int i=0;
			for(; i<length; i++) {
				char c = charArray[i];
				Label lab = Label.getLabel(Character.toString(c));
				newDaughters[i+1] = new TSNodeLabel(lab, true);				
			}
			newDaughters[i+1] = new TSNodeLabel(endWordChar, true);
			pos.daughters = newDaughters;
		}		
	}
	
	public void makeTreeFromCharBased() {
		ArrayList<TSNodeLabel> pos = this.collectPreLex2Items();
		for(TSNodeLabel p : pos) {
			StringBuilder wordBuilder = new StringBuilder();
			int lastIndex = p.prole()-1;
			int index = -1;
			for(TSNodeLabel c : p.daughters) {
				index++;
				if (index==0 || index==lastIndex) continue;
				wordBuilder.append(c.label());				
			}
			String word = wordBuilder.toString();
			TSNodeLabel newWord = new TSNodeLabel(Label.getLabel(word), true);
			newWord.parent = p;
			p.daughters = new TSNodeLabel[]{newWord};
		}
	}	
	
	public static void printTreeFromCharBased(ArrayList<TSNodeLabel> treebank, File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TSNodeLabel t : treebank) {
			TSNodeLabel copy = t.clone();
			copy.makeTreeFromCharBased();
			pw.println(copy);
		}
		pw.close();
	}
	
	public void cutLexicon() {
		ArrayList<TSNodeLabel> lex = this.collectLexicalItems();
		for(TSNodeLabel l : lex) {
			l.parent.daughters = null;
		}		
	}
	
	public void replaceLexWithPos() {
		ArrayList<TSNodeLabel> lex = this.collectLexicalItems();
		for(TSNodeLabel l : lex) {
			l.label = l.parent.label;
		}		
	}
	
	public static TSNodeLabel makeRightBranchingKlein(
			ArrayList<TSNodeLabel> terminals, Label topNode, Label internalNodes) throws Exception {
		
		LinkedList<TSNodeLabel> terminalsList = new LinkedList<TSNodeLabel>(terminals); 
		Iterator<TSNodeLabel> iter = terminalsList.descendingIterator();
		
		TSNodeLabel last = iter.next();
		TSNodeLabel parent = new TSNodeLabel(iter.hasNext() ? internalNodes : topNode, false);
		parent.daughters = new TSNodeLabel[]{last};
		last.parent = parent;		
		
		while(iter.hasNext()) {
			TSNodeLabel previousParent = parent;
			last = iter.next();
			parent = new TSNodeLabel(iter.hasNext() ? internalNodes : topNode, false);
			parent.daughters = new TSNodeLabel[]{last, previousParent};
			last.parent = parent;
			previousParent.parent = parent;
		}
		
		return parent;
		
	}
	
	public static TSNodeLabel makeRightBranchingYoav(
			ArrayList<TSNodeLabel> terminals, Label topNode, Label internalNodes) throws Exception {
		
		TSNodeLabel rightBranching = makeRightBranchingKlein(terminals, topNode, internalNodes);
		int size = terminals.size(); 
		if (size==1) return rightBranching;
		TSNodeLabel lastLTerm = terminals.get(size-1);
		TSNodeLabel lastLexGrandParent = lastLTerm.parent.parent;
		lastLexGrandParent.daughters[1] = lastLTerm;
		return rightBranching;
		
	}
	
	public void uncompressUnaryProductions(String unaryProductionSeparator) {
		if (this.isLexical)
			return;
		TSNodeLabel currentNode = this;
		String currentLabel = currentNode.label();
		boolean originalIsLexical = currentNode.isLexical;
		while (currentLabel.contains(unaryProductionSeparator)) {
			int index = currentLabel.indexOf(unaryProductionSeparator);
			String label1 = currentLabel.substring(0, index);
			String label2 = currentLabel.substring(index+unaryProductionSeparator.length());
			currentNode.relabel(label1);
			currentNode.isLexical = false;
			TSNodeLabel newNode = new TSNodeLabel(Label.getLabel(label2), false);
			newNode.daughters = currentNode.daughters;
			if (!newNode.isTerminal()) {
				for(TSNodeLabel d : newNode.daughters) {
					d.parent = newNode;
				}
			}
			currentNode.daughters = new TSNodeLabel[]{newNode};
			currentNode = newNode;
			currentLabel = currentNode.label();
		}
		currentNode.isLexical = originalIsLexical;
		if (!currentNode.isTerminal()) {
			for(TSNodeLabel d : currentNode.daughters) {
				d.uncompressUnaryProductions(unaryProductionSeparator);
			}
		}
	}
	
	public void compressUnaryProductions(String unaryProductionSeparator, boolean includeLexicalItem) {
		if (this.isTerminal()) return; 
		if (this.daughters.length==1) {
			boolean prelexical = this.isPreLexical();
			if (!prelexical || includeLexicalItem) {
				TSNodeLabel singleDaughter = this.firstDaughter();
				String newLabel = this.label() + unaryProductionSeparator + singleDaughter.label();
				this.daughters = singleDaughter.daughters;
				this.relabel(newLabel);
				if (!this.isTerminal()) {
					for(TSNodeLabel d : this.daughters) {
						d.parent = this;
					}
				}
				this.isLexical = prelexical;
				this.compressUnaryProductions(unaryProductionSeparator, includeLexicalItem);
			}
		}
		else {
			for(TSNodeLabel d : this.daughters) {
				d.compressUnaryProductions(unaryProductionSeparator, includeLexicalItem);
			}
		}
	}
	
	/**
	 * Not applied to unary production leading to single lex
	 * @param unaryProductionSeparator
	 * @param includeLexicalItem
	 * @param includePosTags
	 */
	public void compressInternalUnaryProductions(String unaryProductionSeparator) {
		if (this.isTerminal()) return; 
		if (this.daughters.length==1 && !this.yieldsOneWord()) {
			boolean prelexical = this.isPreLexical();
			TSNodeLabel singleDaughter = this.firstDaughter();
			String newLabel = this.label() + unaryProductionSeparator + singleDaughter.label();
			this.daughters = singleDaughter.daughters;
			this.relabel(newLabel);
			if (!this.isTerminal()) {
				for(TSNodeLabel d : this.daughters) {
					d.parent = this;
				}
			}
			this.isLexical = prelexical;
			this.compressInternalUnaryProductions(unaryProductionSeparator);
		}
		else {
			for(TSNodeLabel d : this.daughters) {
				d.compressInternalUnaryProductions(unaryProductionSeparator);
			}
		}
	}
	
	public void unbinarizeEarly(String dotBinarizationMarker) {		
		int prole = this.prole();
		if (prole==0) return;
		if (prole==2) {			 
			if (this.daughters[1].label().contains(dotBinarizationMarker)) {
				TSNodeLabel currentParent = this;
				ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
				do {
					TSNodeLabel leftDaughter = currentParent.firstDaughter();					
					newDaughters.add(leftDaughter);
					leftDaughter.parent = this;
					currentParent = currentParent.daughters[1];
				} while(currentParent.label().contains(dotBinarizationMarker));
				newDaughters.add(currentParent);
				currentParent.parent = this;
				this.daughters = newDaughters.toArray(new TSNodeLabel[newDaughters.size()]);				
			}
		}
		for(TSNodeLabel d : this.daughters) {
			d.unbinarizeEarly(dotBinarizationMarker);
		}
	}
	
	public void binarizeEarly(String dotBinarizationMarker) {		
		int prole = this.prole();
		if (prole==0) return;
		if (prole>2) {
			TSNodeLabel[] originalDaughters = this.daughters;
			TSNodeLabel[] newDaughters = getDaughterDotLabels(this.label(), originalDaughters, this,
					dotBinarizationMarker, 1);
			this.daughters = newDaughters;
		}
		for(TSNodeLabel d : this.daughters) {
			d.binarizeEarly(dotBinarizationMarker);
		}
		
	}
	
	private static TSNodeLabel[] getDaughterDotLabels(String parentLabelString, TSNodeLabel[] originalDaughters, 
			TSNodeLabel parent, String dotBinarizationMarker, int dotIndex) {
		
		TSNodeLabel leftDaughter=originalDaughters[dotIndex-1];
		TSNodeLabel rightDaughter=null;
		if ( dotIndex+1 == originalDaughters.length ) { 
			rightDaughter = originalDaughters[dotIndex];
		}
		else {
			StringBuilder sb = new StringBuilder(parentLabelString).append("|").append(originalDaughters[0].label());			
			for(int i=1; i<originalDaughters.length; i++) {
				sb.append("_");
				if (dotIndex==i) {
					sb.append(dotBinarizationMarker);
					sb.append("_");
				}
				sb.append(originalDaughters[i].label());
			}
			rightDaughter = new TSNodeLabel(Label.getLabel(sb.toString()), false);
			rightDaughter.daughters = getDaughterDotLabels(parentLabelString, originalDaughters, rightDaughter,
					dotBinarizationMarker, dotIndex+1);
		}
		leftDaughter.parent = parent;
		rightDaughter.parent = parent;		
		return new TSNodeLabel[]{leftDaughter, rightDaughter};
	}

		
	public static TSNodeLabel getRandomTree(String[] internalLabels, String[] lexLabels, 
			int maxBranching, double[] branchingProb, boolean unary, double lexProb, int depth, int maxDepth) {
				
		if (unary && (depth==maxDepth || Math.random()<lexProb)) {
			String lexLabel = lexLabels[(int)(Math.random() * lexLabels.length)];
			return new TSNodeLabel(Label.getLabel(lexLabel), true);
		}		
		
		int prole = (depth==maxDepth-1) ? 1 : Utility.roulette(branchingProb)+1;
		String label = internalLabels[(int)(Math.random() * internalLabels.length)];
		TSNodeLabel result = new TSNodeLabel(Label.getLabel(label), false);
		TSNodeLabel[] daughters = new TSNodeLabel[prole];
		unary = prole==1;
		for(int i=0; i<prole; i++) {			
			TSNodeLabel d = getRandomTree(internalLabels, lexLabels, maxBranching, branchingProb, 
					unary, lexProb, depth+1, maxDepth);
			d.parent = result;
			daughters[i] = d;			
		}
		result.daughters = daughters;
		return result;		
	}
	
	public static TSNodeLabel defaultWSJparse(String[] originalTestSentenceWords, String topSymbol) {
		TSNodeLabel result = new TSNodeLabel(Label.getLabel(topSymbol), false);
		TSNodeLabel sNode = new TSNodeLabel(Label.getLabel("S"), false);
		result.daughters = new TSNodeLabel[]{sNode};
		sNode.parent = result;
		sNode.daughters =  new TSNodeLabel[originalTestSentenceWords.length];
		Label nnsLabel = Label.getLabel("NN");
		for(int i=0; i<originalTestSentenceWords.length; i++) {
			TSNodeLabel nnsNode = new TSNodeLabel(nnsLabel, false);
			nnsNode.parent = sNode;
			sNode.daughters[i] = nnsNode;
			Label lexLabel = Label.getLabel(originalTestSentenceWords[i]); 
			TSNodeLabel lexNode = new TSNodeLabel(lexLabel, true);
			nnsNode.daughters = new TSNodeLabel[]{lexNode};
			lexNode.parent = nnsNode;
		}
		return result;
	}
	
	public static TSNodeLabel defaultWSJparse(String[] originalTestSentenceWords, 
			String[] originalPos, String topSymbol) {
		TSNodeLabel result = new TSNodeLabel(Label.getLabel(topSymbol), false);
		TSNodeLabel sNode = new TSNodeLabel(Label.getLabel("S"), false);
		result.daughters = new TSNodeLabel[]{sNode};
		sNode.parent = result;
		sNode.daughters =  new TSNodeLabel[originalTestSentenceWords.length];		
		for(int i=0; i<originalTestSentenceWords.length; i++) {
			Label posLabel = Label.getLabel(originalPos[i]);
			TSNodeLabel posNode = new TSNodeLabel(posLabel, false);
			posNode.parent = sNode;
			sNode.daughters[i] = posNode;
			Label lexLabel = Label.getLabel(originalTestSentenceWords[i]); 
			TSNodeLabel lexNode = new TSNodeLabel(lexLabel, true);
			posNode.daughters = new TSNodeLabel[]{lexNode};
			lexNode.parent = posNode;
		}
		return result;
	}
	
	public void assignUniqueDaughter(TSNodeLabel firstDaughter) {
		this.daughters = new TSNodeLabel[]{firstDaughter};
		firstDaughter.parent = this;		
	}
	
	public void assignDaughters(TSNodeLabel[] daughters) {
		this.daughters = daughters;
		for(TSNodeLabel d : daughters) {
			d.parent = this;
		}
	}
	
	public void assignDaughters(ArrayList<TSNodeLabel> daughters) {
		this.daughters = new TSNodeLabel[daughters.size()];
		int i=0;
		for(TSNodeLabel d : daughters) {
			d.parent = this;
			this.daughters[i] = d;
			i++;
		}
	}
	
	public void assignDaughter(TSNodeLabel d, int pos) {
		d.parent = this;
		this.daughters[pos] = d;
	}
	
	public String toStringQtree() {
		return this.toString().
		replace("(", "[.").
		replace(")", " ]").
		replace("{", "\\{").
		replace("}", "\\}").
		replace("$", "\\$").
		replace("_", "\\_").
		replace("^", "\\^{}").
		replace("|", "$|$").
		replace("<", "$<$").
		replace(">", "$>$").
		replace("%", "\\%").		
		replace("&", "\\&");
	}
	
	public String toStringTex(boolean heads, boolean lexquotes) {
		String result = this.toString(heads, lexquotes).
				replace("{", "\\{").
				replace("}", "\\}").
				replace("$", "\\$").
				replace("_", "\\_").
				replace("^", "\\^{}").
				replace("|", "$|$").
				replace("<", "$<$").
				replace(">", "$>$").
				replace("%", "\\%").
				replace(" \"", " ``").
				replace("&", "\\&");
		if (result.indexOf('(')==-1) {
			result = "(" + result + ")";
		}
		result = replaceTexHeadWithOverHead(result);
		return  "\\Tree " + result.
				replace("(", "[.").
				replace(")", " ]");
				 
	}
	

	private static String replaceTexHeadWithOverHead(String result) {
		int i = result.indexOf("-H");
		if (i!=-1) {		
			for(int j=i-1; j>=0; j--) {
				char c = result.charAt(j);
				if (c==' ' || c=='(') {
					result = result.substring(0, j+1) + "$\\overline{" + 
								result.substring(j+1, i) + "}$" + result.substring(i+2);
					break;
				}
			}
			return replaceTexHeadWithOverHead(result);
		}
		return result;
	}

	public static void getMatchingLabels() throws Exception {
		
		TSNodeLabelStructure t1 = new TSNodeLabelStructure("(S (NP-SBJ (NNS Analysts)) (VP (VBP say) (SBAR (S (NP-SBJ (NNP USAir)) (VP (VBZ has) (NP (JJ great) (NN promise)))))) (. .))");
		TSNodeLabelStructure t2 = new TSNodeLabelStructure("(S (NP-SBJ (PRP I)) (VP (VBP say) (SBAR (S (NP-SBJ (PRP they)) (VP (VBP are) (ADJP-PRP (JJ ready)))))) (. .))");
		//boolean[][] matchedLables = new boolean[t1.length()][t2.length()];
		
		System.out.print("\t");
		for(TSNodeLabelIndex nodeB : t2.structure()) {
			System.out.print(nodeB.label() + "\t");
		}
		System.out.println();	
			
		
		int i=0;		
		for(TSNodeLabelIndex nodeA : t1.structure()) {
			System.out.print(nodeA.label() + "\t");
			int j=0;
			for(TSNodeLabelIndex nodeB : t2.structure()) {
				if (nodeA.label==nodeB.label) System.out.print("X");//matchedLables[i][j] = true;
				System.out.print("\t");
				j++;
			}
			System.out.println();
			i++;
		}		
	}
	
	public int compareTo(TSNodeLabel tree) {
		if (this.label!=tree.label) {
			return this.label().compareTo(tree.label());
		}
		int thisProle = this.prole();
		int otherProle = tree.prole();
		if (thisProle != otherProle) {
			return thisProle < otherProle ? -1 : 1;
		}
		for(int i=0; i<thisProle; i++) {
			int cmp = this.daughters[i].compareTo(tree.daughters[i]);
			if (cmp!=0) return cmp;
		}
		return 0;
	}
	
	public void changeLexLabels(ArrayList<Label> newLabels) {
		ArrayList<TSNodeLabel> lexLabels = this.collectLexicalItems();
		Iterator<TSNodeLabel> iterBest = lexLabels.iterator();				
		for(Label nl : newLabels) {
			iterBest.next().label = nl;
		}		
	}
	
	public void changeLexLabelsFromStringArray(String[] newLabelStrings) {
		ArrayList<TSNodeLabel> lexLabels = this.collectLexicalItems();
		Iterator<TSNodeLabel> iterBest = lexLabels.iterator();				
		for(String s : newLabelStrings) {
			Label nl = Label.getLabel(s);
			iterBest.next().label = nl;			
		}		
	}
	
	public boolean containsLoops() {
		ArrayList<TSNodeLabel> nodes = this.collectPhrasalNodes();
		for(TSNodeLabel n : nodes) {
			if (n.prole()==1 && n.label.equals(n.firstDaughter().label)) return true;
		}
		return false;
	}
	
	public TSNodeLabel getMinimalConnectedStructure(int prefixLength) {
		if (prefixLength==1) {
			TSNodeLabel firstLex = this.getLeftmostLexicalNode();
			return firstLex.parent.clone();
		}
		ArrayList<TSNodeLabel> lexNodes = this.collectLexicalItems();
		TSNodeLabel firstLexNode = lexNodes.get(0);
		TSNodeLabel lastLexNodePrefix = lexNodes.get(prefixLength-1);
		TSNodeLabel commonAncestorNode = lastLexNodePrefix.parent;
		while(commonAncestorNode.getLeftmostLexicalNode()!=firstLexNode) {
			commonAncestorNode = commonAncestorNode.parent;
		}
		TSNodeLabel result = commonAncestorNode.clone();
		lexNodes = result.collectLexicalItems();
		lastLexNodePrefix = lexNodes.get(prefixLength-1);
		TSNodeLabel p = lastLexNodePrefix.parent;
		TSNodeLabel d = lastLexNodePrefix;
		while(p!=null) {
			if (p.prole()>1) {
				// remove daghters to the right of d
				int index = p.indexOfDaughter(d);
				p.pruneDaughtersAfter(index);
			}
			d = p;
			p = p.parent;			
		}
		return result;
	}
	
	public static TSNodeLabel getLowestCommonAncestorOld(TSNodeLabel firstLex, TSNodeLabel secondLex) {		
		TSNodeLabel commonAncestorNode = secondLex.parent;				
		while(!commonAncestorNode.collectLexicalItems().contains(firstLex)) {
			commonAncestorNode = commonAncestorNode.parent;
		}
		
		return commonAncestorNode;		
	}
	
	public static TSNodeLabel getMinimalConnectedStructure(TSNodeLabel firstLex, TSNodeLabel secondLex) {
		int heightA = firstLex.height();
		int heightB = secondLex.height();
		
		TSNodeLabel ancestorA = firstLex;
		TSNodeLabel ancestorB = secondLex;
		
		while( heightA != heightB ) {
			if ( heightA > heightB ) {
				ancestorA = ancestorA.parent;
				heightA--;
			}
			else {
				ancestorB = ancestorB.parent;
				heightB--;
			}
		}
		
		while( ancestorA != ancestorB ) {
			ancestorA = ancestorA.parent;
			ancestorB = ancestorB.parent;
		}
		
		ArrayList<TSNodeLabel> lex = ancestorA.collectLexicalItems();
		int indexA=-1, indexB=-1;
		int i=0;
		for(TSNodeLabel l : lex) {
			if (l==firstLex)
				indexA=i;
			else if (l==secondLex) {
				indexB=i;
				break;
			}
			i++;
		}
		
		TSNodeLabel commonAncestor = ancestorA.clone();
		Vector<TSNodeLabel> newLex = new Vector<TSNodeLabel>(commonAncestor.collectLexicalItems());
		if (indexA!=0) {
			ancestorA = newLex.get(indexA);			
			while(ancestorA!=commonAncestor) {
				TSNodeLabel p = ancestorA.parent;
				int j = p.indexOfDaughter(ancestorA);
				p.pruneDaughtersBefore(j);
				ancestorA = p;
			}
		}
		if (indexB!=newLex.size()-1) {
			ancestorB = newLex.get(indexB);
			while(ancestorB!=commonAncestor) {
				TSNodeLabel p = ancestorB.parent;
				int j = p.indexOfDaughter(ancestorB);
				p.pruneDaughtersAfter(j);
				ancestorB = p;
			}
		}
				
		return commonAncestor;
	}
	
	
	public void removeSubSitesRecursive() {
		boolean allLex = false;
		do {
			ArrayList<TSNodeLabel> terms = this.collectTerminalItems();
			allLex = true;
			for(TSNodeLabel t : terms) {
				if (!t.isLexical) {
					t.parent.pruneDaughter(t);
					allLex = false;
				}
			}
		} while(!allLex);				
	}
	
	public void removeSubSitesRecursive(String regexMatch) {
		boolean allLex = false;
		do {
			ArrayList<TSNodeLabel> terms = this.collectTerminalItems();
			allLex = true;
			for(TSNodeLabel t : terms) {
				if (!t.isLexical && t.label().matches(regexMatch)) {					
					t.parent.pruneDaughter(t);
					allLex = false;
				}
			}
		} while(!allLex);				
	}
	
	public static void checkStdTSNodeLabelAcquisition(File fileQuotes, File fileStd) throws Exception {
		Scanner scanQuotes = new Scanner(fileQuotes);
		Scanner scanStd = new Scanner(fileStd);
		boolean failed = false;
		int count = 0;
		while(scanQuotes.hasNextLine()) {
			String lq = scanQuotes.nextLine().split("\t")[0];
			String lStd = scanStd.nextLine().split("\t")[0];
			TSNodeLabel tq = new TSNodeLabel(lq, false);
			TSNodeLabel t = TSNodeLabel.newTSNodeLabelStd(lStd);
			if (!t.equals(tq)) {
				System.out.println("Trees differ:");
				System.out.println(lq);
				System.out.println(lStd);
				failed = true;
				break;
			}
			count++;
		}
		if (!failed) {
			System.out.println("All trees match " + count);
		}				
	}
	
	

	public static void main(String[] args) throws Exception {		
		File fileQuotes = new File("/home/sangati/Work/FBK/TSG_MWE/Dutch/LassySmall/lassytrain-nomorph.mrg.frag.filtered.lexQuotes");
		File fileStd = new File("/home/sangati/Work/FBK/TSG_MWE/Dutch/LassySmall/lassytrain-nomorph.mrg.frag.filtered");
		checkStdTSNodeLabelAcquisition(fileQuotes, fileStd);
		
		/*
		String lq = "(SMAIN NP (SMAIN|<ww,INF> ww (INF (NP*0 (lid \"een\") (n \"boete\")) (INF|<ww,NP*1> ww NP*1))))";
		String l = "(SMAIN (NP ) (SMAIN|<ww,INF> (ww ) (INF (NP*0 (lid een) (n boete)) (INF|<ww,NP*1> (ww ) (NP*1 )))))";
		TSNodeLabel t = TSNodeLabel.newTSNodeLabelStd(l);
		TSNodeLabel tq = new TSNodeLabel(lq, false);
		System.out.println(t.equals(tq));
		System.out.println(t.toString(false, true));
		System.out.println(tq.toString(false, true));
		System.out.println(t.prunePhraseNodeNotYieldingLexicalItems());
		System.out.println(tq.prunePhraseNodeNotYieldingLexicalItems());
		System.out.println(t.toString(false, true));
		System.out.println(tq.toString(false, true));
		*/
		
	}



	



	

	

	

	

	

	
		

}
