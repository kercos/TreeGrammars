package tsg;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.corpora.*;
import util.*;

public class TSNode implements Serializable{
	
	public String label;
	public TSNode parent;
	public boolean headMarked;
	public boolean isLexical;
	public TSNode[] daughters;
	private static final long serialVersionUID = 0L;
	// special symbols
	// ~ : CNF transformation
	// " : lexical nodes	
	// - : semantic tag separation
	
	/**
	 * Default contructor
	 */
	public TSNode() {		
	}
	
	public TSNode(String label, TSNode[] daughters) {
		this.label = label;
		this.daughters = daughters;
		for(TSNode d : daughters) d.parent = this;
	}
	
	public static TSNode TSNodeLexical(String label) {
		TSNode TSN = new TSNode();
		TSN.label = label;
		TSN.isLexical = true;
		return TSN;
	}
	
	/**
     * Constructor of a treenode given a bracketing line representing a complete
     * (all the leaves are terminals).
	 * @param bracketing the bracketing line in input
	 */	
	public TSNode(String bracketing) {
		this(bracketing, null, true);
	}
	
	/**
     * Constructor of a treenode given a bracketing line.
	 * @param bracketing the bracketing line in input
	 * @param complete whether the tree is a complete tree (all the leaf are terminal) or not.
	 * In this second case the terminals are marked with quotations (")
	 */	
	public TSNode(String bracketing, boolean allTerminalsAreLeaves) {
		this(bracketing, null, allTerminalsAreLeaves);
	}
	
	/**
     * Constructor of recursive daughters given the parent head
     */
	private TSNode(String bracketing, TSNode parent, boolean allTerminalsAreLexical) {
		this.parent = parent;
		if (bracketing.indexOf('(')==-1) {
			this.acquireHeads(bracketing);
			if (allTerminalsAreLexical) this.isLexical = true;
			else acquireLexicalMark();
			return;
		}
		String[] subBrackets = subBrackets(bracketing);
		if (!this.isLexical) this.acquireHeads(subBrackets[0]);				
		this.daughters = new TSNode[subBrackets.length-1];
		for(int i=1; i<subBrackets.length; i++) {
			this.daughters[i-1] = new TSNode(subBrackets[i], this, allTerminalsAreLexical);
		}	
	}
	
	/**
	 * Use to retrive the bracketing of the string in input.
	 */
	private static String[] subBrackets(String bracketing) {
		LinkedList<String> subBrackets = new LinkedList<String>();
		bracketing = bracketing.substring(1,bracketing.length()-1);
		bracketing = bracketing.replaceAll("\\)\\(", ") (");
		int parenthesis = 0;
		String currentSubBracket = "";
		for(int i=0; i<bracketing.length(); i++) {
			char c = bracketing.charAt(i);
			if (c==' ' && currentSubBracket.equals("")) continue;
			currentSubBracket += c;
			if (c=='(') parenthesis ++;
			else if (c==')') parenthesis --;
			if (c==' ' || i==bracketing.length()-1) {
				if (parenthesis==0) {
					subBrackets.add(currentSubBracket.trim());
					currentSubBracket = "";
					continue;
				}
			}
		}
		return subBrackets.toArray(new String[] {});
	}

	/**
	 * Used to read the heads in the structure of the bracketted string in input.
	 */
	private void acquireHeads(String label) {
		if (label.endsWith("-H")) {
			this.headMarked = true;
			label = label.substring(0, label.length()-2);
		}
		else this.headMarked = false;
		this.label = label;
	}
	
	
	private void acquireLexicalMark() {
		if (this.label.indexOf("\"")==-1) return;
		this.label = this.label.replaceAll("\"","");
		this.isLexical = true;		
	}
	
	public String label() {
		return label;
	}
	
	public String label(boolean removeSematicTags) {
		return (removeSematicTags) ? removeSemanticTag(label) : label;
	}
	
	public boolean isRoot() {
		return this.parent == null;
	}
	
	public boolean isTop() {
		return this.isRoot() && this.label.equals(ConstCorpus.topTag);
	}
	
	public void removeTop() {
		if (!this.isTop()) return;
		this.label = this.daughters[0].label;
		this.daughters = this.daughters[0].daughters;
		if (this.daughters!=null) { 
			for(TSNode d : daughters) {
				d.parent = this;
			}
		}
	}
	
	public boolean isHeadMarked() {
		return this.headMarked;
	}
	
	public boolean isArgumentMarked() {
		return this.label.endsWith("-A");
	}
	
	public TSNode root() {
		if (this.parent==null) return this;
		return this.parent.root();
	}
	
	public int prole() {
		return this.daughters.length;
	}

	/**
     * Update the label of the current node
     */
	public void updateLabel(String newlabel) {
		this.label = newlabel;
	}
	
	/**
	 * Returns the left most daughter of the current tree
	 * @return
	 */
	public TSNode firstDaughter() {
		return this.daughters[0];
	}
	
	/**
	 * Returns the right most daughter of the current tree
	 * @return
	 */
	public TSNode lastDaughter() {
		return this.daughters[this.daughters.length-1];
	}
	
	/**
     * Copy constructor with new copy of daughters (no references).
     */
	public TSNode(TSNode original) {
		this.label = original.label;
		this.headMarked = original.headMarked;
		this.isLexical = original.isLexical;
		if (original.daughters==null) return;
		this.daughters = new TSNode[original.daughters.length];
		for(int d=0; d<original.daughters.length; d++) {
			this.daughters[d] = new TSNode(original.daughters[d]);
			this.daughters[d].parent = this;
		}		
	}
	
	/*public boolean equals(Object obj) {
		if (this == obj) return true;		
		if (obj instanceof TreeNode) {
		    TreeNode node = (TreeNode)obj;
		    if (!node.label.equals(this.label)) return false;
		    if (!node.parent.equals(this.parent)) return false;
		    if (node.headMarked!=this.headMarked) return false;
		    if (node.isLexical!=this.isLexical) return false;
		    if (node.isTerminal()!=this.isTerminal()) return false;
		    if (node.isTerminal()) return true;
		    if (node.prole()!=this.prole()) return false;		    
		    for(int i=0; i<node.prole(); i++) {
		    	if (!node.daughters[i].equals(this.daughters[i])) return false;
		    }
		    return true;
		}
		return false;
	}*/
	
	public boolean equals(Object obj) {
		return this == obj;
	}
	
	/**
	 * @return true if the current TreeNode is a unique daughter, i.e. it's parent
	 * has only one daughter.
	 */
	public boolean isUniqueDaughter() {
		TSNode parent = this.parent;
		if (parent==null) return false;
		return (this.parent.daughters.length==1);
	}
		
	public boolean isTerminal() {
		return this.daughters==null;
	}
	
	public boolean isPreterminal() {
		return (this.daughters.length==1 && this.daughters[0].isTerminal());
	}
	
	public boolean isPrelexical() {
		return (this.daughters.length==1 && this.daughters[0].isLexical);
	}
	
	public boolean containsSubstringInDaughter(String substring) {
		if (this.daughters==null) return false;
		for(int i=0; i<this.daughters.length; i++) {
			if (this.daughters[i].label.contains(substring)) return true;
		}
		return false;
	}
	
	public boolean containsMultipleHeadInDaughters() {
		if (this.daughters==null) return false;
		int prole = this.daughters.length;		
		int countHead = 0;
		for(int d=0; d<prole; d++) {
			if (this.daughters[d].headMarked) countHead++;
		}
		if (countHead>1) return true;
		return false;
	}
	
	/**
	 * Returns the maximum depth of the current treenode
	 * @param tilde if to consider the levels created with the CNF normalization
	 * @return the max depth of the current treenode
	 */
	public int maxDepth(boolean tilde) {
		if (this.daughters==null) return 0;
		int maxDepth = 0;
		for(int i=0; i<this.daughters.length; i++) {
			int increase = 1;
			if (!tilde && this.isTildeNode()) increase = 0;
			int depth = increase + this.daughters[i].maxDepth(tilde);
			if (depth > maxDepth) maxDepth = depth;
		}
		return maxDepth;
	}
	
	public int maxDepth() {
		if (this.daughters==null) return 0;
		int maxDepth = 0;
		for(int i=0; i<this.daughters.length; i++) {
			int increase = 1;
			int depth = increase + this.daughters[i].maxDepth();
			if (depth > maxDepth) maxDepth = depth;
		}
		return maxDepth;
	}
	
	/**
	 * Returns the maximum of all the minimal depths of the internal 
	 * nodes (TOP node discarded) of the current treenode (according to Yoav Seginer's PhD thesis 
	 * this should be <=1)
	 */
	public int maxOfMinDepth() {
		if (this.daughters==null) return 0;
		int maxOfMinDepth = this.minDepth();
		for(int i=0; i<this.daughters.length; i++) {
			int minDepth = this.daughters[i].minDepth();
			if (minDepth > maxOfMinDepth) maxOfMinDepth = minDepth;
		}
		return maxOfMinDepth;
		
	}

	/**
	 * Returns the minimal depth of the current treenode
	 * (Yoav Seginer's PhD thesis defines this to be the hight 
	 * of the TreeNode)
	 */
	public int minDepth() {
		if (this.daughters.length==1) {
			if (this.daughters[0].daughters==null) return 0;
			else return this.daughters[0].minDepth();
		}		
		int minDepth = Integer.MAX_VALUE;
		for(int i=0; i<this.daughters.length; i++) {
			int depth = this.daughters[i].minDepth();
			if (this.daughters[i].daughters.length>1) depth++;
			if (depth < minDepth) minDepth = depth;
		}
		return minDepth;
	}
	
	/**
	 * Returns the number of lexical derivation in the current treenode
	 * obtain by multiplying the number of daughters nodes for each
	 * node.
	 * @return
	 */
	public long lexDerivations() {
		long result = this.prole();
		if (this.isPrelexical()) return result;
		for(TSNode D : this.daughters) result *= D.lexDerivations(); 
		return result;
	}
	
	/**
	 * Return the hight of the current TreeNode in the tree
	 * @return an integer corresponding to the hight of the current TreeNode
	 */	
	public int hight() {		
		int hight = 0;
		TSNode TN = this;
		while(TN.parent!=null) {
			TN = TN.parent;
			hight++;
		}
		return hight;		
	}
	
	/**
	 * Return the daughter of the current treenode which is headmarked (possibly null).
	 * @return
	 */
	public TSNode markedDaughter() {
		if (this.daughters==null) return null;
		for(int i=0; i<this.daughters.length; i++) {
			if (this.daughters[i].headMarked) return this.daughters[i];
		}
		return null;
	}
	
	/**
	*  Given a TreeNode 'TN' and an int 'n' as input, the method returns a boolean,
	*  specifying whether the tree contains a contruction with more than
	*  'n' branching.
	*/
	public boolean hasMoreThanNBranching(int n) {
		if (this.isTerminal()) return false;
		if (this.prole() > n) return true;
		for(TSNode TN : this.daughters) {
			if (TN.hasMoreThanNBranching(n)) return true;
		}
		return false;
	}
	
	/**
	 * Returns the maximum number of internal unary productions in row.
	 * @return [0] serial unary production from the current TreeNode;
	 * [1] max serial unary production in the whole tree
	 */
	public int[] maxSerialUnaryProduction() {
		int prole = this.daughters.length;
		if (prole==1 && this.daughters[0].daughters==null) return new int[]{0,0};
		int[] max = new int[]{0,0};
		for(int i=0; i<prole; i++) {
			max[1] = Math.max(max[1], this.daughters[i].maxSerialUnaryProduction()[1]);
			max[0] = Math.max(max[0], this.daughters[i].maxSerialUnaryProduction()[0]);
		}		
		if (prole==1 && this.parent!=null) {
			max[0]++;
			max[1] = Math.max(max[1], max[0]);
		}
		else max[0] = 0;		
		return max;
	}
	
	/**
	 * Make the postags of the current tree its lexicon
	 * @param uniqueLex
	 */
	public void makePosTagsLexicon() {
		List<TSNode> words = this.collectTerminals();
		for(TSNode w : words) {
			w.label = w.parent.label;
		}
	}
	
	/**
	 * Return in the previous lexicon production
	 * @param uniqueLex
	 */
	public void unMakePosTagsLexicon(TSNode original) {
		List<TSNode> posTags = this.collectLexicalItems();
		List<TSNode> lexicon = original.collectLexicalItems();
		ListIterator<TSNode> p = posTags.listIterator();
		ListIterator<TSNode> l = lexicon.listIterator();		
		while (p.hasNext() && l.hasNext()) {
			p.next().label = l.next().label;
		}
	}
	
	/**
	 * Rename all the lexicon representing numbers with a unique numberTag.
	 * 4.5 --> numberTag
	 * 4,600 --> numberTag
	 * 4,600.5 --> numberTag
	 * 1\/2 --> numberTag
	 * @param numberTag
	 */
	public void replaceNumbers(String numberTag) {
		List<TSNode> words = this.collectLexicalItems();
		for(TSNode w : words) {
			if (w.label.length() > 1 && w.label.matches("[\\d.,\\\\/]+")) w.label = numberTag;
			else if (w.label.matches("\\d")) w.label = numberTag;
		}
	}
	
	/**
	 * Return the index of the input Daughter in the daughters
	 * of the current node.
	 * @return
	 */
	public int indexOfDaughter(TSNode daughter) {
		if (this.daughters==null) return -1;
		for(int i=0; i<this.daughters.length; i++) {
			if (this.daughters[i]==daughter) return i;
		}
		return -1;
	}
	
	/**
	 * Remove the input daughter from the current treenode 
	 * @param daughter the node to be removed from the daughters 
	 * of the current treenode 
	 */
	public void pruneDaughter(TSNode daughter) {
		pruneDaughter(this.indexOfDaughter(daughter));
	}
	
	
	/**
	 * Inser a new daughter in the daughters of the current tree
	 * at a given position
	 * @param daughter
	 * @param newPosition
	 */
	public void insertNewDaughter(TSNode daughter, int newPosition) {
		TSNode[] newDaughters = new TSNode[this.daughters.length+1];
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
	 * Remove the input daughter from the current treenode 
	 * @param index the daughter index to be removed from the daughters 
	 * of the current treenode 
	 */
	public void pruneDaughter(int index) {
		TSNode[] newDaughters = new TSNode[this.daughters.length-1];
		int i = 0;
		for(int j=0; j<this.daughters.length; j++) {
			if (j==index) continue;
			newDaughters[i] = this.daughters[j];
			i++;
		}
		this.daughters = newDaughters;
	}
	
	/**
	 * Remove the semantic tags of the type "-SBJ" in the labels TreeNodes within 
	 * the current treenode. 
	 */	
	public void removeSemanticTags() {
		if (this.isLexical) return;
		this.label = removeSemanticTag(label);
		for(TSNode D : this.daughters) D.removeSemanticTags();
	}
	
	/**
	 * Remove one specific semantic tags of the type "-SBJ" in the labels TreeNodes within 
	 * the current treenode. 
	 */	
	public void removeSemanticTags(String tag) {
		if (this.isLexical) return;
		this.label = removeSemanticTag(label, tag);
		for(TSNode D : this.daughters) D.removeSemanticTags(tag);
	}
	
	public static boolean hasSemanticTag(String label) {		
		return (label.indexOf('-')>0);
	}
	
	public static String removeSemanticTag(String label) {
		int dash_index = label.indexOf('-');
		if (dash_index>0) { //avoid to deal with -NONE-, -RRB-, -RCB-, ...
			label = label.substring(0,dash_index);
		}
		return label;
	}
	
	public static String removeSemanticTag(String label, String tag) {
		int dash_index = label.indexOf('-' + tag);
		if (dash_index>0) { //avoid to deal with -NONE-, -RRB-, -RCB-, ...
			label = label.substring(0,dash_index);
		}
		return label;
	}
	
	/**
	 * Update the terminal labels of the current tree with unknownTag
	 * if its frequency in lexFreq is <= limit
	 * @param limit
	 * @param lexFreq
	 */
	public void updateUnknown(int limit, Hashtable<String, Integer> lexFreq, String unknownTag,
			String exception) {
		List<TSNode> lexicon = this.collectLexicalItems();
		for(TSNode w : lexicon) {
			if (w.label.equals(exception)) continue;
			Integer freq = lexFreq.get(w.label);
			if (freq==null || freq<=limit) w.label = unknownTag;
		}
	}
	
	/**
	 * Prune the subtrees labeled by the input label from the current tree 
	 */
	public TSNode pruneSubTrees(String tag) {
		if (this.toString().indexOf(tag)==-1) return null;
		if (this.label.equals(tag)) return this;
		if (this.isTerminal()) return null;
		// contains traces
		LinkedList<TSNode> nonTraces = new LinkedList<TSNode>();
		for(int i=0; i<this.daughters.length; i++) {
			TSNode D = this.daughters[i];
			if (D.pruneSubTrees(tag)==null) nonTraces.add(D);
		}
		if (nonTraces.size() == this.daughters.length) return null;
		if (nonTraces.isEmpty()) return this;
		this.daughters = (TSNode[]) nonTraces.toArray(new TSNode[] {});
		return null;
	}
	
	/**
	 * Replace all the occurances of oldLabel with newLabel in the
	 * current TreeNode. 
	 * @param oldLabel
	 * @param newLabel
	 */
	public void replaceLabels(String oldLabel, String newLabel) {
		if (this.label.equals(oldLabel)) this.label = newLabel;
		if (this.daughters==null) return;
		for(TSNode d : this.daughters) {
			d.replaceLabels(oldLabel, newLabel);
		}
	}
	
	/**
	 * Prune the subtrees labeled by one of the label in the input array removeLabels
	 * from the current tree. The input array removeLabels should be sorted beforehand
	 * (as for instance with Arrays.sort(removeLabels); 
	 */
	public TSNode pruneSubTrees(String[] removeLabels) {
		if (Arrays.binarySearch(removeLabels, this.label)>=0) return this;
		if (this.isTerminal()) return null;
		// contains traces
		LinkedList<TSNode> nonTraces = new LinkedList<TSNode>();
		for(int i=0; i<this.daughters.length; i++) {
			TSNode D = this.daughters[i];
			if (D.pruneSubTrees(removeLabels)==null) nonTraces.add(D);
		}
		if (nonTraces.size() == this.daughters.length) return null;
		if (nonTraces.isEmpty()) return this;
		this.daughters = (TSNode[]) nonTraces.toArray(new TSNode[] {});
		return null;
	}
	
	/**
	 * Remove rules of the kind X -> X
	 */
	public void removeRedundantRules() {
		if (this.isPrelexical()) return;
		if (this.prole()==1 && this.label.equals(this.daughters[0].label)) {
			this.daughters = this.daughters[0].daughters;
			for(TSNode D : this.daughters) D.parent = this;
			this.removeRedundantRules();
		}
		else {
			for(TSNode D : this.daughters) D.removeRedundantRules();
		}
	}
	
	/**
	 * Check the presence rules of the kind X -> X
	 */
	public boolean hasRedundentRules() {
		List<TSNode> nodes = this.collectNonLexicalNodes();
		for(TSNode TN : nodes) {
			if (TN.isPrelexical()) continue;
			if (TN.prole()==1 && TN.label.equals(TN.daughters[0].label)) return true;
			
		}
		return false;
	}
	
	
	/**
	 * Prune (possible multiple instances of) punctuation at the 
	 * beginning (left) of the tree
	 */
	public void prunePunctuationBeginning() {
		TSNode[] terminals = this.collectTerminals().toArray(new TSNode[]{});
		int i = 0;
		while(i<terminals.length && Utility.isPunctuation(terminals[i].parent.label)) i++;
		int lastIndex = i - 1;
		if (lastIndex==-1) return;
		TSNode pruningRoot = (lastIndex==0) ? 
				terminals[0] : this.lowestCommonParent(terminals[0], terminals[lastIndex]);
		if (lastIndex!=0) {
			System.out.print("Multiple punctuation: " + this);
		}
		while  (pruningRoot.isUniqueDaughter()) pruningRoot = pruningRoot.parent;
		pruningRoot.parent.pruneDaughter(pruningRoot);
	}

	/**
	 * Prune (possible multiple instances of) punctuation at the 
	 * end (right) of the tree
	 */
	public void prunePunctuationEnd() {
		TSNode[] terminals = this.collectTerminals().toArray(new TSNode[]{});
		int i = terminals.length-1;
		while(i>-1 && Utility.isPunctuation(terminals[i].parent.label)) i--;
		int firstIndex = i + 1;
		if (firstIndex==terminals.length) return;
		TSNode pruningRoot = (firstIndex==terminals.length-1) ? terminals[terminals.length-1] : 
			this.lowestCommonParent(terminals[terminals.length-1], terminals[firstIndex]);
		if (firstIndex!=terminals.length-1) {
			System.out.print("Multiple punctuation: " + this);
		}
		while  (pruningRoot.isUniqueDaughter()) pruningRoot = pruningRoot.parent;
		pruningRoot.parent.pruneDaughter(pruningRoot);
	}
	
	/**
	 * Raising punctuation hat occurs at the very beginning or end of a sentence, so that
	 * it always sits between two other nonterminals
	 */
	public void raisePunctuation() {
		List<TSNode> terminals = this.collectTerminals();
		for(TSNode leaf : terminals) {
			TSNode postag = leaf.parent;
			if (Utility.isPunctuation(postag.label)) {
				if (postag.isUniqueDaughter()) {
					System.out.println("Nodes that only dominate punctuation preterminals: " + this);
					continue;
				}
				TSNode parentNode = postag.parent;				
				if (parentNode.firstDaughter()==postag) {
					parentNode.pruneDaughter(0);
					while (parentNode==parentNode.parent.firstDaughter()) {
						parentNode=parentNode.parent;
					}
					int newPosition = parentNode.parent.indexOfDaughter(parentNode);
					parentNode.parent.insertNewDaughter(postag, newPosition);
				}
				else if (parentNode.lastDaughter()==postag) {
					parentNode.pruneDaughter(parentNode.daughters.length-1);
					while (parentNode==parentNode.parent.lastDaughter()) {
						parentNode = parentNode.parent;
					}
					int newPosition = parentNode.parent.indexOfDaughter(parentNode) + 1;
					parentNode.parent.insertNewDaughter(postag, newPosition);
				}
			}			
		}
		
	}

	
	/**
	 * Remove the numbers at the end of the tags:
	 * NP-sbj-3 --> NP-sbj
	 * Doesn't remove the semantic tags and doesn't affect the heads marking
	 */
	public void removeNumberInLabels() {
		if (this.isTerminal()) return;
		this.label = this.label.replaceAll("-\\d+", "");
		this.label = this.label.replaceAll("=\\d+", "");
		for(int i=0; i<this.daughters.length; i++) this.daughters[i].removeNumberInLabels();
	}

	
	/**
	 * Returns the number of POS which are equal in label with the 
	 * gold treee in input. The count excludes the gold labels present in the input array
	 * exludePOS which should be sorted beforehand (i.e. Arrays.sort(excludeLabels)).
	 */
	public int countCorrectPOS(TSNode TNgold, String[] exludePOS) {
		List<TSNode> thisLex = this.collectLexicalItems();
		List<TSNode> goldLex = TNgold.collectLexicalItems();
		int result = 0;
		for(int i=0; i<thisLex.size(); i++) {
			String thisPOS = thisLex.get(i).parent.label;
			String goldPOS = goldLex.get(i).parent.label;
			if (Arrays.binarySearch(exludePOS, goldPOS)>=0) continue;
			if (thisPOS.equals(goldPOS)) result++;
		}
		return result;
	}

	/**
     * Returns the number of internal nodes (non terminals, non root) of the current treenode
     */
	public int countInternalNodes() {
		int result = 0;
		if (this.isTerminal()) return result;
		if (this.parent != null) result++;
		for(TSNode TN : this.daughters) {
			result += TN.countInternalNodes();
		}
		return result;
	}
	
	public int countNodes(boolean top, boolean terminals, boolean preterminals) {
		int result = 0;
		if (this.isTerminal()) return (terminals) ? 1 : 0;
		boolean isRoot = this.isRoot();
		boolean isPreterminal = this.isPreterminal(); 
		if (isRoot && top) result++;
		if (isPreterminal && preterminals) result++;	
		if (!isRoot && !isPreterminal) result++;
		for(TSNode TN : this.daughters) {
			result += TN.countNodes(top, terminals, preterminals);
		}
		return result;		
	}
	
	public int countAllNodes() {
		int result = 1;
		if (this.isTerminal()) return result; 
		for(TSNode TN : this.daughters) {
			result += TN.countAllNodes();
		}
		return result;
	}
	
	/**
	 * Returns the number of lexical nodes yielded by the current TreeNode
	 */
	public int countLexicalNodes() {
		int result = 0;
		if (this.isLexical) return 1;
		for(TSNode TN : this.daughters) result += TN.countLexicalNodes();
		return result;
	}
	
	/**
	 * Returns the number of lexical nodes yielded by the current TreeNode,
	 * exluding the ones present in the input array excludeLexLabels which
	 * has to be sorted beforehand (as with Arrays.sort(excludeLexLabels)).
	 */
	public int countLexicalNodesExcludingLexLabels(String[] excludeLexLabels) {
		int result = 0;
		if (this.isLexical) {
			return  (Arrays.binarySearch(excludeLexLabels, this.label)<0) ? 1 : 0;
		}
		for(TSNode TN : this.daughters) result += TN.countLexicalNodesExcludingLexLabels(excludeLexLabels);
		return result;
	}
	
	/**
	 * Returns the number of lexical nodes yielded by the current TreeNode,
	 * exluding the ones yield by categories present in the input array exludeCatLabels which
	 * has to be sorted beforehand (as with Arrays.sort(excludeCatLabels)).
	 */
	public int countLexicalNodesExcludingCatLabels(String[] excludeCatLabels) {
		int result = 0;
		if (this.isLexical) return 1;
		if (Arrays.binarySearch(excludeCatLabels, this.label)>=0) return 0; 
		for(TSNode TN : this.daughters) {
			result += TN.countLexicalNodesExcludingCatLabels(excludeCatLabels);
		}
		return result;
	}
	
	/**
	 * Return a string representation of the current label
	 * optionally incorporating head dependecies and lexical quotations marks.
	 * @param headDependencies
	 * @param lexquot
	 * @return
	 */
	public String label(boolean headDependencies, boolean lexquot) {
		return label(headDependencies, lexquot, true);
	}
	
	/**
	 * Return a string representation of the current label
	 * optionally incorporating head dependecies and lexical quotations marks.
	 * @param headDependencies
	 * @param lexquot
	 * @return
	 */
	public String label(boolean headDependencies, boolean lexquot, boolean semTag) {
		String result = (semTag) ? this.label : removeSemanticTag(label);
		if (headDependencies && this.headMarked) result += "-H";
		if (lexquot && this.isLexical) result = "\"" + result + "\"";
		return result;
	}
	
	/**
     * Print the current TreeNode in Penn standard format (without head dependencies
     * and without quotation symbols for lexical items).
     */
	public String toString() {
		return this.toString(false, false);
	}

	/**
     * Print the current TreeNode in Penn standard format with optional 
     * head dependencies and quotation symbols for lexical items.
     * 
     * @param  headDependencies   if <code>true</code>, print also head dependencies.
     * @param  lexquot   if <code>true</code>, print also quotation symbols for lexical items.
     */
	public String toString(boolean headDependencies, boolean lexquot) {
		String result = this.label(headDependencies, lexquot);
		if (this.isTerminal()) return result;
		result = "(" + result;
		for(TSNode TN : this.daughters) { 
			result += " " + TN.toString(headDependencies, lexquot);
		}
		result += ")";
		return result;
	}
	
	/**
	 * Return the one contex free grammar production from the current
	 * node which has to be non-terminal
	 * @param lexquot
	 * @return
	 */
	public String toCFG(boolean lexquot) {
		String result = this.label;
		for(TSNode TN : this.daughters) result += " " + TN.label(false, lexquot);
		return result;
	}
	
	/**
	 * Return the contex free grammar production from the current node
	 * considering only the child with "-A" augmentation.
	 * Add 1 to exluded for each child not considered
	 * @param semTag
	 * @return
	 */
	public String toCFGCompl(boolean semTag, int[] exluded) {
		String result = label(false, false, semTag);
		for(TSNode TN : this.daughters) {
			if (TN.label.endsWith("-A")) {
				result += " " + TN.label(false, false, semTag);
			}
			else exluded[0]++;
		}
		return result;
	}
	
	/**
     * Returns the yield of the current TreeNode: a String with the terminal labels
     * separated by one space. 
     */
	public String toFlat() {
		String flat = "";
		List<TSNode> terminals = this.collectTerminals();
		for(TSNode TN : terminals) flat += TN.label + " ";
		flat = flat.trim();
		return flat;
	}
	
	/**
     * Returns the yield of the current TreeNode: a String with the terminal labels
     * separated by '/n'. 
     */
	public String toExtractWord() {
		String result = "";
		List<TSNode> terminals = this.collectTerminals();
		for(TSNode TN : terminals) result += TN.label + "\n";
		return result;
	}
	
	
	/**
	 * TO DELETE
     * Converts the labels of the terminals of the current TreeNode
     * adding quotation marks.
     */
	/*public void addQuotationsInLexicon() {
		LinkedList<TreeNode> lexicons = this.collectTerminals();
		for (ListIterator<TreeNode> i=lexicons.listIterator(); i.hasNext(); ) {
			TreeNode TN = (TreeNode) i.next();
			TN.label = "\"" + TN.label + "\"";
		}
	}*/
	
	/**
	 * TO DELETE
     * Converts the labels of the terminals of the current TreeNode
     * removing quotation marks.
     */
	/*public void removeQuotationsInLexicon() {
		LinkedList lexicons = this.collectTerminals();
		for (ListIterator i=lexicons.listIterator(); i.hasNext(); ) {
			TreeNode TN = (TreeNode) i.next();
			TN.label = TN.label.replaceAll("\"","");
		}
	}*/
	
	/**
	*  Given a TreeNode 'constructor' and a TreeNode 'TN' the method
	*  returns a boolean specifying whether the 'constructor' is present
	*  in 'TN'. The boolean variable in input 'recursive' specifies whether
	*  the search has to stop to the first level (superficial) of the tree or recursivly
	*  throughtout its depth (recursive).
	*/
	public boolean containsConstructor(TSNode constructor) {
		return containsConstructor(constructor, true, null);
	}
	
	public TSNode whereIsConstructor(TSNode constructor) {
		TSNode[] result = new TSNode[1];
		containsConstructor(constructor, true, result);
		return result[0];
	}
	
	public boolean containsSpine(TSNode constructor) {
		TSNode lex = constructor.getAnchor();
		List<TSNode> anchors = this.collectLexicalItems();
		for(TSNode a : anchors) {
			if (a.label.equals(lex.label)) {
				boolean equals = true;
				TSNode ancestor = lex;
				while(ancestor.parent != null) {
					ancestor = ancestor.parent;
					a = a.parent;
					if (!ancestor.label.equals(a.label)) {
						equals = false;
						break;
					}
				}
				if (equals) return true;
			}
		}
		return false;
	}

	/**
	*  Method to implement containsConstructor(TreeNode constructor)
	*/
	private boolean containsConstructor(TSNode constructor, boolean recursive, TSNode[] pointer) {
		if (this.daughters==null) {
			return (constructor.daughters==null && (this.label.equals(constructor.label) 
					|| constructor.label.equals("*")));
		}
		if (this.label.equals(constructor.label) || constructor.label.equals("*")) {
			boolean sameDaughters = true;
			if (constructor.daughters!=null) {
				if (this.daughters.length==constructor.daughters.length) {
					for(int i=0; i<this.daughters.length; i++) {
						if (!this.daughters[i].containsConstructor(constructor.daughters[i], 
								false, pointer)) {
							sameDaughters = false;
							break;
						}
					}		
				}
				else sameDaughters = false;
			}
			if (sameDaughters) {
				if (pointer!=null && constructor.parent==null) pointer[0] = this;
				return true;
			}
		}
		if (recursive) {
			for(int i=0; i<this.daughters.length; i++) {
				if (this.daughters[i].containsConstructor(constructor, true, pointer)) return true;
			}	
		}
		return false;
	}
	
	/**
	 * Returns the leftmost descendent of the current treenode
	 * @return
	 */
	public TSNode getLeftmostLexicon() {
		while(!this.isLexical) {
			return this.firstDaughter().getLeftmostLexicon();
		}
		return this;
	}
	
	/**
	 * Returns the rightmost descendent of the current treenode
	 * @return
	 */
	public TSNode getRightmostLexicon() {
		while(!this.isLexical) {
			return this.lastDaughter().getRightmostLexicon();
		}
		return this;
	}

	/**
	 * Returns a LinkedList of type TreeNode containing the terminals of
	 * the current TreeNode. 
	 */
	public List<TSNode> collectTerminals() {
		List<TSNode> result = new ArrayList<TSNode>();
		this.collectTerminals(result);
		return result;
	}
	
	/**
	 * Returns the yield of the current TreeNode i.e. all the words
	 * it dominates separated by single spaces.
	 */
	public String getYield() {
		List<TSNode> terminals = this.collectTerminals();
		String result = "";
		for(TSNode TN : terminals) {
			result += TN.label + " ";
		}
		result = result.trim();
		return result;
	}
	
	/**
	 * Method to implement collectTerminals()
	 * @param terminals
	 */
	private void collectTerminals(List<TSNode> terminals) {
		for(TSNode TN : this.daughters) {
			if (TN.isTerminal()) terminals.add(TN);
			else TN.collectTerminals(terminals);
		}		
	}
	
	/**
	 * Returns a LinkedList of type Constituency containing the constituencies
	 * of the current treeNode excluding the labels present in the input array
	 * excludeLabels which should be sorted beforehands (i.e. Arrays.sort(excludeLabels)).
	 * If labeled is false we still exclude categories excludeLabels from statistics as in EvalB
	 */
	/*public List<Constituency> collectConsituencies(boolean labeled, String[] excludeLabels, boolean yield) {
		List<Constituency> result = new ArrayList<Constituency>();
		List<String> lexicalLabels = null;
		List<TSNode> lexicalNodes = this.collectLexicalItems();
		if (!yield) {
			lexicalLabels = this.collectTerminalStrings();
			for(int i=0; i<lexicalNodes.size(); i++) {
				TSNode LN = lexicalNodes.get(i);
				LN.label = ""+i;
			}			
		}
		List<TSNode> nonTerminals = this.collectNodes(true, false, false, true);		
		for(TSNode NT : nonTerminals) {
			if (Arrays.binarySearch(excludeLabels, NT.label)>=0) continue;
			String NTlabel = (labeled) ? NT.label : "";
			if (yield) {				
				result.add(new ConstituencyYield(NTlabel, new Yield(NT.getYield()), labeled));
			}
			else {
				int start = Integer.parseInt(NT.getLeftmostLexicon().label);
				int end = Integer.parseInt(NT.getRightmostLexicon().label);										
				result.add(new ConstituencyWords(NTlabel, start, end, labeled));
			}			
		}
		if (!yield) {
			for(int i=0; i<lexicalNodes.size(); i++) {
				TSNode LN = lexicalNodes.get(i);
				LN.label = lexicalLabels.get(i);
			}			
		}
		return result;
	}*/
	
	/**
	 * Returns a LinkedList of type TreeNode containing the lexical items of
	 * the current TreeNode. 
	 */
	public List<TSNode> collectLexicalItems() {
		List<TSNode> result = new ArrayList<TSNode>();
		this.collectLexicalItems(result);
		return result;
	}
	
	/**
	 * Method to implement collectTerminals()
	 * @param terminals
	 */
	private void collectLexicalItems(List<TSNode> terminals) {
		for(TSNode TN : this.daughters) {
			if (TN.isLexical) terminals.add(TN);
			else TN.collectLexicalItems(terminals);
		}		
	}
	
	/**
	 * Returns a LinkedList of type String containing the terminals of
	 * the current TreeNode. 
	 */
	public List<String> collectTerminalStrings() {		
		List<TSNode> terminals = this.collectTerminals();
		List<String> result = new ArrayList<String>(terminals.size());
		for(TSNode TN : terminals) result.add(TN.toString(false, false));
		return result;
	}
	
	
	/**
	 * Returns a LinkedList of type TreeNode containing the non-terminal nodes of the current
	 * TreeNode which are not HeadMarked
	 */
	public List<TSNode> collectSubstitutionSites() {
		List<TSNode> result = new ArrayList<TSNode>();
		this.collectSubstitutionSites(result);
		return result;
	}
	
	private void collectSubstitutionSites(List<TSNode> list) {
		if (this.isPrelexical()) return;
		for(TSNode D: this.daughters) {			
			if (!D.headMarked) list.add(D);
			else D.collectSubstitutionSites(list);
		}
	}

	/**
	 * Collect non root non terminal nodes with optional preterminal nodes
	 * and add them in the list provided as input. 
	 * and nodes with only one daughter (unary).
	 * @param prelexical if true include nodes in the pre-terminal positions
	 * @param unary if true include nodes with only one daughter
	 * @param list the list where to store the internal nodes.
	 */
	public List<TSNode> collectNodes(boolean top, boolean prelexical, boolean lexical, boolean unary) {
		ArrayList<TSNode> nodes = new ArrayList<TSNode>();
		this.collectNodes(top, prelexical, lexical, unary, nodes);
		return nodes;
	}
	
	
	private void collectNodes(boolean top, boolean prelexical, boolean lexical, 
			boolean unary, List<TSNode> list) {
		if 	( !( (!top && this.parent==null) ||
				 (!lexical && this.isLexical) ||
				 (!prelexical && this.isPrelexical()) || 				 
				 (!unary && this.prole()==1) ) ) list.add(this);
		if (this.isLexical) return;
		for(TSNode TN : this.daughters) 
			TN.collectNodes(top, prelexical, lexical, unary, list);
	}
	
	
	
	/**
	 * Returns a List with all the non-lexical nodes
	 * in the current TreeNode.
	 * @return
	 */
	public List<TSNode> collectNonLexicalNodes() {
		List<TSNode> list = new ArrayList<TSNode>(); 
		collectNonLexicalNodes(list);
		return list;
	}
	
	/**
	 * Method to implement collectNonLexicalNodes()
	 * @param list
	 */
	private void collectNonLexicalNodes(List<TSNode> list) {
		if (this.isLexical) return;
		list.add(this);		
		for(TSNode TN : this.daughters) TN.collectNonLexicalNodes(list);
	}
	
	
	/**
	 * Returns a List with all the non-terminal nodes
	 * in the current TreeNode.
	 * @return
	 */
	public List<TSNode> collectNonTerminalNodes() {
		List<TSNode> list = new ArrayList<TSNode>(); 
		collectNonTerminalNodes(list);
		return list;
	}
	
	/**
	 * Method to implement collectNonLexicalNodes()
	 * @param list
	 */
	private void collectNonTerminalNodes(List<TSNode> list) {
		if (this.isTerminal()) return;
		list.add(this);		
		for(TSNode TN : this.daughters) TN.collectNonTerminalNodes(list);
	}
	
	
	/**
	 * Returns a list with all the Nodes in the parent chain of 
	 * the current node
	 * @return
	 */
	public List<TSNode> collectAncestorNodes() {
		List<TSNode> list = new ArrayList<TSNode>();
		TSNode node = this.parent;
		while(node!=null) {
			list.add(node);
			node = node.parent;
		}
		return list;
	}
	
	/**
	 * Returns a LinkedList with all the terminal and non terminal nodes
	 * in the current TreeNode.
	 * @return
	 */	
	public List<TSNode> collectAllNodes() {
		List<TSNode> list = new ArrayList<TSNode>(); 
		collectAllNodes(list);
		return list;
	}
	
	private void collectAllNodes(List<TSNode> list) {
		list.add(this);
		if (this.daughters==null) return;
		for(TSNode TN : this.daughters) {
			TN.collectAllNodes(list);
		}
	}

	/**
	 * Extract the unique lexical production which occurs in the input
	 * one anchored lexicalized tree. Return two string representing
	 * the delexicalized tree and the lexical production. 
	 * and returns it as a String (POSTAG "word").   
	 * @return an array of two strings
	 */
	public String[] splitUniqueLexProduction() {
		String[] result = new String[2];
		TSNode TN = this.getAnchor();
		TN = TN.parent;
		result[0] = TN.toString(false, true);
		TN.daughters = null;
		result[1] = this.toString(false, true);
		return result;
	}
	
	/**
	 * Returns the nodes within the current TreeNode according to their depth.
	 * (S (A (D w1) (E w2) ) (B w3) (C w4) )
	 * depth 3: [w1, w2]
	 * depth 2: [(D w1), (E w2)]
	 * depth 1: [(A (D w1) (E w2)), (B w3) (C w4)]
	 * depth 0: [(S (A (D w1) (E w2)) (B w3) (C w4))] 
	 */
	public ArrayList<ArrayList<TSNode>> getNodesInDepthLevels() {
		int levels = this.maxDepth(true)+1;
		ArrayList<ArrayList<TSNode>> result = new ArrayList<ArrayList<TSNode>>(levels);
		ArrayList<TSNode> currentLevel = new ArrayList<TSNode>();
		currentLevel.add(this);
		for(int i=0; i<levels; i++) {
			result.add(0,currentLevel);			
			ArrayList<TSNode> newLevel = new ArrayList<TSNode>();
			for (TSNode TN : currentLevel) {			
				if (TN.isTerminal()) continue;
				for(TSNode TNdaughter : TN.daughters) newLevel.add(TNdaughter);
			}
			currentLevel = newLevel;
		}
		return result;
	}
	
	/**
	 * Return the unique lexical item in this tree
	 * Note: valid only for one anchor lexicalized trees
	 * @return
	 */
	
	public TSNode getAnchor() {
		for(TSNode TN : this.daughters) {
			if (TN.isLexical) return TN;
			if (TN.isTerminal()) continue;
			return TN.getAnchor();								
		}
		return null;
	}
	
	/**
	 * Returns the lexical anchor following the head percolation spine
	 * @return
	 */
	public TSNode getAnchorThroughPercolation() {
		TSNode node = this;
		while(!node.isPrelexical()) node = node.getHeadDaughter();
		return node.firstDaughter();
	}
	
	/**
	 * Return the daughter of the current treenode
	 * which is head marked.
	 * @return
	 */
	public TSNode getHeadDaughter() {
		for(TSNode TN : this.daughters) {
			if (TN.headMarked) return TN;
		}
		return null;
	}
	
	/**
	 * Returns the CFG rules for the terminal productions with
	 * or without quotation: (NN "Part") (NN Part)
	 */
	public List<String> getLexicalProduction(boolean quotation) {
		List<String> result = new ArrayList<String>();
		getLexicalProduction(quotation, result);
		return result;
	}
	
	/**
	 * Method to implement getLexicalProduction(boolean quotation)
	 */
	private void getLexicalProduction(boolean quotation, List<String> result) {		
		if (this.isPrelexical()) result.add(this.toString(false, quotation));
		else for(TSNode TN : this.daughters) {
			if (!TN.isTerminal()) TN.getLexicalProduction(quotation, result);
		}
	}

	/**
	 * Convert the terminal items in the current TreeNode to lower case
	 */
	public void toLowerCase() {
		List<TSNode> terminals = this.collectTerminals();
		for (TSNode TN : terminals) TN.label = TN.label.toLowerCase();
	}
	
	/**
	 * Convert the terminal items in the current TreeNode to upper case
	 */
	public void toUpperCase() {
		List<TSNode> terminals = this.collectTerminals();
		for (TSNode TN : terminals) TN.label = TN.label.toUpperCase();
	}
	
	/**
	 * Remove the terminals from the current TreeNode
	 */
	public void removeLexicon() {
		if (this.isPrelexical()) this.daughters = null;
		else for(TSNode TN : this.daughters) TN.removeLexicon();
	}
	
	public boolean dominatesNodeLabel(String lab) {
		if (this.isPrelexical()) return false;
		for (TSNode D : this.daughters) {
			if (D.label.equals(lab) || D.dominatesNodeLabel(lab)) return true;
		}
		return false;
	}
	
	public void transformNodebasal(String tag, String basalTag) {
		if (this.isPrelexical()) return;
		if (this.label.equals(tag) && !this.dominatesNodeLabel(tag)) {
			this.label = basalTag;
		}
		for (TSNode D : this.daughters) {
			D.transformNodebasal(tag, basalTag);
		}
	}
	
	public void transformSubjectlessSentences(String SubjectLessTag) {
		System.err.println("transformSubjectlessSentences() not yet implemented");
	}
	
	public void convertTag(String oldTag, String newTag) {
		if (this.label.equals(oldTag)) this.label = newTag;
		if (this.isTerminal()) return;
		for (TSNode D : this.daughters) {
			D.convertTag(oldTag, newTag);
		}
	}
	
	/**
	 * Assign a unique label to the internal nodes of the current  TreeNode
	 * adding a unique "@counter" after the label.
	 * @param top if to label the top symbol with a unique label
	 * @param index the index which was last assigned
	 * @return the last index that was assigned
	 */
	public int toUniqueInternalLabels(boolean top, int index, boolean followHeads) {
		if (this.daughters==null) return index;
		if (top || this.parent!=null) {
			if (!followHeads || this.headMarked || this.isTildeNode()) this.label += "@" + index++;
		}
		for(TSNode TN : this.daughters) {
			index = TN.toUniqueInternalLabels(top, index, followHeads);
		}
		return index;
	}

	/**
	 * Remove the unique labels to the internal nodes of the current TreeNode
	 */
	public void removeUniqueInternalLabels(boolean markHeads) {
		if (markHeads && this.label.indexOf('@')!=-1) this.headMarked = true; 
		this.label = this.label.replaceAll("@\\d+", "");
		if (this.daughters==null) return;
		for(TSNode TN : this.daughters) TN.removeUniqueInternalLabels(markHeads);
	}
	
	/**
	 * Builds a set of CFG rules from the current TreeNode
	 * following the algorithm of the Goodman reduction.
	 * Note: it works only for binarized tree. You need to convert
	 * the TreeNode to CNF. Not all the 8 rules specified in the
	 * Goodman reduction are applied for CNF internal nodes (conaining the ~ character)
	 * @param lexQuotes specifies whether to output the lexical items in between quotation marks
	 * @return a LinkedList of String each representing a CFG rule
	 */
	public LinkedList<String> goodman(boolean lexQuotes) {
		LinkedList<String> result = new LinkedList<String>();
		this.goodman(lexQuotes, result);
		return result;
	}
	
	public long goodman(boolean lexQuotes, LinkedList<String> CFGRules) {
		long result = 1L;
		if (this.isLexical) return result;
		String Aj = this.label;
		String A = this.label.replaceAll("@\\d+","");
		boolean AisTildeNode = this.isTildeNode();
		if (this.isPrelexical()) {
			String lex = this.daughters[0].label;
			if (lexQuotes) lex = "\"" + lex + "\"";
			CFGRules.add(Aj + " " + lex + " " + "1");
			CFGRules.add(A + " " + lex + " " + "1");
			return result;
		}
		String Bk = this.daughters[0].label;
		String B = this.daughters[0].label.replaceAll("@\\d+","");
		long b_k =  this.daughters[0].goodman(lexQuotes, CFGRules);
		result = b_k + 1L;
		if (this.daughters.length == 1) {			
			if (!A.equals(ConstCorpus.topTag)) {
				CFGRules.add(Aj + " " + B + " 1");
				CFGRules.add(Aj + " " + Bk + " " + b_k);				
			}
			CFGRules.add(A + " " + B + " 1");
			CFGRules.add(A + " " + Bk + " " + b_k);							
			return result;
		}
		String Cl = this.daughters[1].label;
		String C = this.daughters[1].label.replaceAll("@\\d+","");
		boolean CisTildeNode = this.daughters[1].isTildeNode();
		long c_l = this.daughters[1].goodman(lexQuotes, CFGRules);
		
		if(!CisTildeNode) {
			result *= c_l + 1L;
			CFGRules.add(Aj + " " + B + " " + C + " 1");
			CFGRules.add(Aj + " " + Bk + " " + C + " " + b_k);
		}
		else result *= c_l;
		CFGRules.add(Aj + " " + B + " " + Cl + " " + c_l);
		CFGRules.add(Aj + " " + Bk + " " + Cl + " " + b_k*c_l);
		
		if(!AisTildeNode) {
			if(C.indexOf('~')==-1) {
				CFGRules.add(A + " " + B + " " + C + " 1");
				CFGRules.add(A + " " + Bk + " " + C + " " + b_k);	
			}
			CFGRules.add(A + " " + B + " " + Cl + " " + c_l);
			CFGRules.add(A + " " + Bk + " " + Cl + " " + b_k*c_l);			
		}
		return result;
	}
	
	/**
	 * Substitute the internal nodes of the current treenode
	 * with the postags of the word according to the current head annotation.
	 * Note: should follow one head per node constraint
	 */
	public void percolatePosTags() {
		ArrayList<ArrayList<TSNode>> nodesInLevels = this.getNodesInDepthLevels();	
		boolean first = true;
		for(ArrayList<TSNode> levelList : nodesInLevels) {
			if (first) {
				first = false;
				continue;
			}
			for (TSNode TN : levelList) {				
				if (TN.isLexical || TN.isPrelexical() || TN.label.equals(ConstCorpus.topTag)) continue;				
				TSNode daughter = TN.getHeadDaughter();
				TN.label = daughter.label;
			}
		}
	}
	
	/**
	 * Run conversion on the current one-anchor lexicalized tree
	 * according to the parameter options.
	 * Returns: the lexicon that was removed in case of posTagConversion.
	 */
	public void applyAllConversions() {	 
		if (Parameters.spineConversion) {
			this.convertToSpine();
			if (Parameters.removeRedundencyInSpine) this.removeRedundantRules();
		}		
		if (Parameters.posTagConversion) this.makePosTagsLexicon();
		else if (Parameters.jollyConversion) this.convertToJolly();
	}
		
	/** 
	 *	The method converts this TreeNode to a spine: makes the path from the root to the 
	 *	lexical anchor a unique path: it removes all the internal nodes not in the path	
	 *	connecting the root to the lexical anchor.
	 *	Note: applicable only to TreeNode representing a one-anchored-lexical-tree.
	 */ 
	public void convertToSpine() {
		if (this.isTerminal()) return;
		if (this.daughters.length!=1) {
			TSNode[] newDaughter = new TSNode[1];
			for(TSNode D : this.daughters) {
				if (D.isTerminal()) continue;
				newDaughter[0] = D;
				break;
			}
			this.daughters = newDaughter;
		}		
		this.daughters[0].convertToSpine();
	}
	
	/** 
	 *	The method removes the substitution sites being in the jolly conversion labels array
	 *	(Parameters.jollyLabels).
	 *	Note: applicable only to TreeNode representing a one-anchored-lexical-tree.
	 */ 
	public void convertToJolly() {
		if (this.isPrelexical()) return;
		LinkedList<TSNode> newDaughters = new LinkedList<TSNode>();
		TSNode headDaughter = null;
		for(TSNode d : this.daughters) {
			if (!d.isTerminal()) {
				headDaughter = d;
				newDaughters.add(d);
				continue;
			}
			boolean present = Arrays.binarySearch(Parameters.jollyLabels, d.label)>=0; 
			if (Parameters.jollyInclusion && present) newDaughters.add(d);
			else if (!Parameters.jollyInclusion && !present) newDaughters.add(d);
		}
		if (this.prole() != newDaughters.size()) {
			this.daughters = newDaughters.toArray(new TSNode[newDaughters.size()]);
		}
		headDaughter.convertToJolly();
	}
	
	/**
	 * This method compute all subtrees up to depth maxDepth in 
	 * the current TreeNode.
	 * @param maxDepth maxDepth of subtrees
	 * @return a LinkedList of TreeNode each being a subtree of maximum depth maxDepth
	 */
	public List<String> allSubTrees(int maxDepth) {
		Pair<? extends List<String>> duet = this.allSubTreesB(maxDepth);
		List<String> result = duet.getFirst();
		result.addAll(duet.getSecond());
		return result;
	}
	
	/**
	 * Method to cumpute allSubTrees(int maxDepth))
	 */
	private Pair<ArrayList<String>> allSubTreesB(int maxDepth) {
		// subTrees[0] = NEW subTrees;
		// subTrees[1] = OLD subTrees;
		Pair<ArrayList<String>> subTrees = 
			new Pair<ArrayList<String>> (new ArrayList<String>(), new ArrayList<String>());
		if (this.isTerminal()) return subTrees;
		int prole = this.daughters.length;
		ArrayList<ArrayList<String>> daughterSubTrees = new ArrayList<ArrayList<String>>(prole);
		int[] daughterSubTreesSize = new int[prole];
		int index = 0;
		for(TSNode TN : this.daughters) {				
			Pair<ArrayList<String>> daugherSubTreesNewOld = TN.allSubTreesB(maxDepth);			
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
			if (rule == null) continue;
			subTrees.getFirst().add(rule);
		}		
		return subTrees;
	}
	
	/**
	 * Computes the number of subTrees (of indefinite depth) in the current TreeNode.
	 * @param includeTilde if true count the subtrees created by the CNF normalization
	 * @return number of subTrees
	 */
	public long countSubTrees(boolean includeTilde) {
		if (this.daughters==null) return 0;
		long result = this.daughters[0].countSubTrees(includeTilde)+1;
		if (this.daughters.length==1) return result;
		for(int i=1; i<this.daughters.length; i++) {
			long count = this.daughters[i].countSubTrees(includeTilde);
			if (includeTilde || this.daughters[i].label.indexOf('~')==-1) count++;
			result *= count;			
		}
		return result;
	}
	
	public boolean isTildeNode() {
		return (this.label.indexOf("~") != -1);
	}
	
	/**
	 * Remove the internal (non preterminal and non pos-starting i.e. S, FRAG) unary productions.
	 * (TOP (S (A (B w)))) -> (TOP (S (B w)))
	 */
	/*public void removeInternalUnaryNodes() {
		if (this.isPreterminal()) return;
		if (this.parent!=null) {
			while (this.daughters.length==1) {
				this.daughters = this.daughters[0].daughters;
				if (this.isPreterminal()) return;
			}			
		}
		for(int i=0; i<this.daughters.length; i++) {
			this.daughters[i].removeInternalUnaryNodes();
		}		
	}*/
	
	
	/**
	 * Remove the non preterminal unary productions.
	 * (TOP (S (A (B w)))) -> (TOP (B w))
	 */
	/*public void removeUnaryNodes() {
		if (this.isPreterminal()) return;
		while (this.daughters.length==1) {
			this.daughters = this.daughters[0].daughters;
			if (this.isPreterminal()) return;
		}			
		for(int i=0; i<this.daughters.length; i++) {
			this.daughters[i].removeUnaryNodes();
		}		
	}*/
	
	
	/**
	 * Remove the internal (non preterminal) unary productions.
	 * (TOP (S (A (B w)))) -> (TOP (B w))
	 */
	/*public void markPosTagUnique() {
		if (this.isPreterminal() && this.parent!=null) {
			this.label += "_POS";
		}
		if (this.isPreterminal()) return;
		for(int i=0; i<this.daughters.length; i++) {
			this.daughters[i].markPosTagUnique();
		}
	}*/
	

	
	/**
	 * Transform this TreeNode in Chomsky Normal Form (CNF).
	 * (S (A (D w1) (E w2) ) (B w3) (C w4) ) --> (S (A (D w1) (E w2)) (S_A_B_C~1 (B w3) (C w4)))
	 */
	public void toNormalForm() {
		if (this.isTerminal()) return;
		if (this.daughters.length <= 2) {
			for(TSNode TN : this.daughters) TN.toNormalForm();
			return;
		}
		// if more than 2 daughters
		this.daughters[0].toNormalForm();
		int index = this.label.indexOf('~');
		int level = (index==-1)? 1 : Integer.parseInt(this.label.substring(index+1))+1;
		String newLabel;		
		if (index==-1) {
			newLabel = new String(this.label); 
			for (TSNode TN : this.daughters) newLabel += "_" + TN.label;
		}
		else newLabel = this.label.substring(0,index);
		TSNode tildeDaughter = new TSNode();
		tildeDaughter.label = newLabel+"~"+level;
		tildeDaughter.parent = this;
		tildeDaughter.daughters = new TSNode[this.daughters.length-1];
		tildeDaughter.headMarked = false;		
		for (int i=1; i<this.daughters.length; i++) {
			tildeDaughter.daughters[i-1] = this.daughters[i];
			tildeDaughter.daughters[i-1].parent = tildeDaughter;
			tildeDaughter.daughters[i-1].toNormalForm();
			if (tildeDaughter.daughters[i-1].headMarked) tildeDaughter.headMarked = true;
		}
		this.daughters = new TSNode[] {this.daughters[0], tildeDaughter};
		tildeDaughter.toNormalForm();
	}
		
	/**
	 * Transform this TreeNode from Chomsky Normal Form (CNF) into the original format.
	 * (S (A (D w1) (E w2)) (S_A_B_C~1 (B w3) (C w4))) --> (S (A (D w1) (E w2) ) (B w3) (C w4) )
	 */
	public void fromNormalForm() {
		if (this.daughters == null) return;
		int last = this.daughters.length-1;
		if (this.daughters[last].isTildeNode()) {
			int new_dim = this.daughters.length + 1;
			TSNode[] newDaughters = new TSNode[new_dim];
			for(int i=0; i<last; i++) newDaughters[i] = this.daughters[i];
			newDaughters[last] = this.daughters[last].daughters[0];
			newDaughters[last].parent = this;
			newDaughters[last+1] = this.daughters[last].daughters[1];
			newDaughters[last+1].parent = this;			
			this.daughters = newDaughters;
			this.fromNormalForm();
			return;
		}
		for(int i=0; i<this.daughters.length; i++) this.daughters[i].fromNormalForm();		
	}
	
	/**
	 * Compute the number of derivations made of one-anchored-lexicalized subtrees.
	 * Should not be run on TreeNode normalized in CNF.
	 */
	public int lexicalizedHeadDerivations() {		
		if (this.daughters == null) return 1;
		int result = this.daughters.length;
		for(TSNode TN : this.daughters) {
			result *= TN.lexicalizedHeadDerivations();
		}
		return result;
	}
	

	/**
	 * This method prunes all the subtrees in the non head marked nodes
	 */
	public void toLexicalizeFrame() {		
		if (this.isTerminal()) return;
		for(TSNode TN : this.daughters) {
			if (!TN.headMarked) TN.daughters = null;
			else TN.toLexicalizeFrame();
		}
	}

	
	/**
	 * Split the current TreeNode in two TreeNodes if there is
	 * a recursion rule at the TOP node. i.e.
	 * (TOP (S (S NP VP)))
	 */
	public TSNode[] cutRecursiveNew() {
		if (!this.label.equals("TOP")) return null;
		TSNode recursiveNode = null;
		for(int i=0; i<this.daughters.length; i++) {
			if (this.daughters[i].daughters!=null) {
				recursiveNode = this.daughters[i];
				break;
			}
		}
		for(int i=0; i<recursiveNode.daughters.length; i++) {
			TSNode D = recursiveNode.daughters[i];
			if (D.daughters == null) continue;
			if (!D.label.equals(recursiveNode.label)) return null;
			TSNode[] splitting = new TSNode[2];
			TSNode[] daughters =  D.daughters;
			D.daughters = null;		
			splitting[0] = new TSNode(this);
			D.daughters = daughters;
			D.parent = null;
			splitting[1] = D;
			return splitting;
		}
		return null;
	}
	
	public TSNode[] cutRecursiveOld() {
		if (this.label.equals("TOP") && this.daughters[0].label.equals("S")) {
			TSNode TN_S = this.daughters[0];
			for(int j=0; j<TN_S.daughters.length; j++) {
				TSNode D = TN_S.daughters[j];
				if (D.daughters != null && D.label.equals("S")) {
					TSNode[] splitting = new TSNode[2];
					TSNode[] daughters =  D.daughters;
					D.daughters = null;						
					splitting[0] = new TSNode(this);
					D.daughters = daughters;
					splitting[1] = D;
					return splitting;
				}
			}
		}
		return null;
	}
	
	public TSNode[] cutRecursive() {
		if (this.label.equals("TOP")) {
			for(int j=0; j<this.daughters.length; j++) {
				TSNode D = this.daughters[j];
				if (D.isTerminal()) continue;
				TSNode[] splitting = new TSNode[2];
				TSNode[] daughters =  D.daughters;
				D.daughters = null;						
				splitting[0] = new TSNode(this);
				D.daughters = daughters;
				splitting[1] = D;
				return splitting;
			}
		}
		return null;
	}

	
	/**
	 * Removed the head markers in all the TreeNode within the current TreeNode
	 *
	 */
	public void removeHeadAnnotations() {
		this.headMarked=false;
		if (this.daughters==null) return;
		for(TSNode TN : this.daughters) TN.removeHeadAnnotations();
	}
	
	/**
	 * Remove the argument label (-A) in nodes that are already marked as heads.
	 */
	public void removeArgumentInHeads() {		
		if (this.label.endsWith("-H-A")) {
			this.headMarked = true;
			label = label.substring(0, label.length()-4);
		}
		if (this.daughters==null) return;
		for(TSNode TN : this.daughters) TN.removeArgumentInHeads();
	}
	
	/**
	 * Assign the head marker to the first daughter recursively downwards this TreeNode.
	 * Note: the current TreeNode should be cleaned from head annotations before runnint this method
	 *
	 */
	public void assignFirstLeftHeads() {
		int prole = this.daughters.length;
		if (prole==1 && this.daughters[0].daughters==null) return;
		this.daughters[0].headMarked = true;
		for(TSNode TN : this.daughters) TN.assignFirstLeftHeads();
	}
	
	/**
	 * Assign the head marker to the last daughter recursively downwards this TreeNode.
	 * Note: the current TreeNode should be cleaned from head annotations before runnint this method
	 *
	 */
	public void assignFirstRightHeads() {
		int prole = this.daughters.length;
		if (prole==1 && this.daughters[0].daughters==null) return;
		this.daughters[prole-1].headMarked = true;
		for(TSNode TN : this.daughters) TN.assignFirstRightHeads();
	}
	
	/**
	 * Assign the head marker to a random daughter recursively downwards this TreeNode
	 * Note: the current TreeNode should be cleaned from head annotations before runnint this method
	 *
	 */
	public void assignRandomHeads() {
		int prole = this.daughters.length;
		if (prole==1 && this.daughters[0].daughters==null) return;
		int randomDaughter = Utility.randomInteger(prole);
		this.daughters[randomDaughter].headMarked = true;
		for(TSNode TN : this.daughters) TN.assignRandomHeads();
	}
	
	
	public TSNode lowestCommonParent(TSNode[] set) {
		if (set.length>2) {
			TSNode[] newSet = new TSNode[set.length-1];
			newSet[0] = lowestCommonParent(set[0], set[1]);
			for(int i=2; i<set.length; i++) newSet[i-1] = set[i];
			return lowestCommonParent(newSet);
		}
		return lowestCommonParent(set[0], set[1]);
	}
	
	public TSNode lowestCommonParent(TSNode terminal1, TSNode terminal2) {
		int hightDifference = terminal1.hight() - terminal2.hight(); 
		if (hightDifference!=0) {
			if (hightDifference<0) { //terminal2 more deep than terminal1
				for(; hightDifference!=0; hightDifference++) terminal2 = terminal2.parent; 
			}
			else { //terminal1 more deep than terminal2
				for(; hightDifference!=0; hightDifference--) terminal1 = terminal1.parent;
			}
		}
		// the two termianl are on the same hight
		while(terminal1!=terminal2) {
			terminal1 = terminal1.parent;
			terminal2 = terminal2.parent;
		}
		return terminal1;
	}
	
	/**
	 * Copy all the head dependency in the "fromTree" TreeNode into the current one
	 * It assumes that the current tree has the same tree structure of the fromTree
	 * @param fromTree tree from which to copy the head dependency
	 */
	public void copyHeadAnnotation(TSNode fromTree) {
		this.headMarked = fromTree.headMarked;
		if (this.isTerminal()) return;
		for(int d=0; d<this.daughters.length; d++) {
			this.daughters[d].copyHeadAnnotation(fromTree.daughters[d]);
		}
	}
	
	public boolean hasSameHeadAnnotation(TSNode otherTree) {
		if (this.headMarked!=otherTree.headMarked) return false;
		if (this.isTerminal()) return true;
		for(int i=0; i<this.prole(); i++) {
			if (!this.daughters[i].hasSameHeadAnnotation(otherTree.daughters[i])) return false;
		}
		return true;
	}

	
	
	/**
	 * Check if each internal node in the current TreeNode has a proper
	 * assignment of head dependencies: each internal node should have exactly
	 * one daughter with head annotation.
	 * @param correct if to correct the assignment of heads: choose the leftmost daughter as head
	 * in those cases where 0 or more than one head daughters are detected
	 * @return 
	 * [0] total number of (potential) correction
	 * [1] number of (potential) superfluous correction (in the cases where there is only one daughter
	 * which is not head annotated) 
	 */
	public int[] checkHeadConsistency(boolean correct) {
		int[] result = new int[2];
		checkHeadConsistency(correct, result);
		return result;		
	}
	
	public boolean hasWrongHeadAssignment() {
		int[] correction = new int[2];
		checkHeadConsistency(false, correction);
		return correction[0]>0;		
	}
	
	/**
	 * to implement checkHeadConsistency
	 * @param correct
	 * @param result
	 */
	private void checkHeadConsistency(boolean correct, int[] result) {
		if (this.daughters==null) return;
		int prole = this.daughters.length;		
		if(this.isPreterminal()) return;
		int countHead = 0;
		for(int d=0; d<prole; d++) {
			if (this.daughters[d].headMarked) countHead++;
			this.daughters[d].checkHeadConsistency(correct, result);
		}
		if (countHead!=1) {
			if (correct) {
				if (countHead>1) {
					for(TSNode d : this.daughters) d.headMarked = false;
				}
				//int randomDaughter = Utility.randomInteger(prole);
				this.daughters[0].headMarked = true;
			}
			result[0]++;
			if (prole==1) result[1]++; 
		}
	}
	
	public void fixUnaryHeadConsistency() {
		if (this.daughters==null) return;
		int prole = this.daughters.length;	
		if(prole==1 && this.daughters[0].daughters!=null) this.daughters[0].headMarked = true;
		for(int d=0; d<prole; d++) this.daughters[d].fixUnaryHeadConsistency();
	}

	/**
	 * Return a List of (copied) lexicalized trees according
	 * to the head annotation of the current parse tree
	 * Don't run it in binarized trees
	 * @return a LinkedList of TreeNodes
	 */
	public List<TSNode> lexicalizedTreesFromHeadAnnotation() {
		List<TSNode> result = new ArrayList<TSNode>();
		List<TSNode> lexicon = this.collectLexicalItems();
		for (TSNode TN : lexicon) {
			TSNode up = TN.parent;
			while(up.headMarked && up.parent!=null) up = up.parent;						
			result.add(up.lexicalizedTreeCopy());	
		}
		return result;
	}
	
	/**
	 * Return a copy of the lexicalized tree starting from the current treenode
	 * @return
	 */
	public TSNode lexicalizedTreeCopy() {
		TSNode lexicalTree = new TSNode(this);
		//LexicalTree.parent = null;
		lexicalTree.toLexicalizeFrame();
		return lexicalTree;
	}
	
	/**
	 * Return a copy of the lexical tree rooted on this TreeNode and anchored on leaf
	 * @param leaf the anchor of the returned lexical tree
	 * @return the lexical tree as a string
	 */
	public TSNode lexicalizedTreeToAnchor(TSNode leaf) {
		markHeadPathToAnchor(leaf);
		TSNode lexTree = new TSNode(this);
		lexTree.toLexicalizeFrame();
		unmarkHeadPathToAnchor(leaf);
		return lexTree;
	}
	
	/**
	 * Mark the path from the current node to the input leaf with
	 * head annotations
	 * @param leaf
	 */
	public void markHeadPathToAnchor(TSNode leaf) {
		TSNode up = leaf.parent;
		while(up!=this) {
			up.headMarked = true;
			up = up.parent;
		}
	}
	
	/**
	 * Unmark the path from the current node to the input leaf with
	 * head annotations
	 * @param leaf
	 */
	public void unmarkHeadPathToAnchor(TSNode leaf) {
		TSNode up = leaf.parent;
		while(up!=this) {
			up.headMarked = false;
			up = up.parent;
		}
	}
	
	/**
	 * Mark the path from the current node to the root of the tree
	 * 
	 */
	public void markHeadPathToTop() {
		TSNode up = this.parent;
		while(up!=null) {
			up.headMarked = true;
			up = up.parent;
		}
	}
	
	/**
	 * Remove the substitution sites in the current lexical tree contained in the
	 * imput arrays of labels. The input arrays must be sorted.
	 * @param removeCat
	 */
	public void removeSubstitutionSitesInLexTree(String[] removeCat) {
		TSNode daughter = this.getAnchor();
		TSNode parent = daughter.parent;
		while(parent!=null) {
			LinkedList<TSNode> newDaughters = new LinkedList<TSNode>();
			for(TSNode d : parent.daughters) {					
				if (d==daughter || Arrays.binarySearch(removeCat, d.label)< 0) newDaughters.add(d);					
			}
			if (newDaughters.size()!=parent.daughters.length) {
				parent.daughters = newDaughters.toArray(new TSNode[]{});
			}
			daughter = parent;
			parent = daughter.parent;
		}
	}
	
	public void addMarkersInTree(IdentityHashMap<TSNode,TreeSet<Integer>> markTable) {
		TreeSet<Integer> markRecord = markTable.get(this);
		if (markRecord==null) this.label = "*_" + this.label;
		else for(Integer mark : markRecord) {
			this.label += "_" + mark;				
		}
		if (this.daughters==null) return;
		for(int i=0; i<this.daughters.length; i++) this.daughters[i].addMarkersInTree(markTable);		
	}
	
	/**
	*  Return the first string in the input string eTree which 
	*  occurs in between quotation marks.
	*/
	public static String get_unique_lexicon(String eTree) {
		int quoteChar = eTree.indexOf('"');
		if (quoteChar==-1) return null;
		String lexicon = eTree.substring(quoteChar+1,eTree.indexOf('"', quoteChar+1));
		return lexicon;
	}
	
	/**
	*  Return the first string in the input string which represents the
	*  root of the TreeNode.
	*/
	public static String get_unique_root(String eTree) {
		return eTree.substring(1, eTree.indexOf(' '));
	}
	
	public boolean isPunctuation() {
		return Utility.isPunctuation(this.label);
	}
	
	public boolean hasOnlyPuncLeaves() {
		List<TSNode> terminals = this.collectTerminals();
		for(TSNode l : terminals) {
			if (!l.isPunctuation()) return false;
		}
		return true;
	}
	
	public static boolean areAllPunctuationLabels(List<TSNode> nodes) {
		for(TSNode l : nodes) {
			if (!l.isPunctuation()) return false;
		}
		return true;
	}
	
	public String printLatex(int level) {
		String result = "";
		if (level==0) result += "\\begin{parsetree}\n";
		result += Utility.fillTab(level) + "( ." + this.label + ".";
		for(int i=0; i<this.daughters.length; i++) {
			TSNode TN = this.daughters[i];			
			if (TN.daughters==null) {
				String lexicalLabel = TN.label;
				if (lexicalLabel.equals("")) lexicalLabel = "$\\varnothing$";
				result += " ~ `" + lexicalLabel + "')";
			}
			else {
				result +=  "\n" + TN.printLatex(level+1);
				if (i==this.daughters.length-1) result += "\n" + Utility.fillTab(level) + ")";
			}			
		}					
		if (level==0) result += "\n\\end{parsetree}\n\n";
		//if (level==0) result += "\\pagebreak\n\n";
		return result;
	}
	
	public String printLatex2() {
		String result = "\\Tree " + this.toString();
		result = result.replaceAll("\\(","[.");
		result = result.replaceAll("\\)"," ]");
		return result;
	}
	
	public BitSet getHeadsBitSet() {		
		List<TSNode> allNodes = this.collectAllNodes();
		BitSet result = new BitSet(allNodes.size());
		int index = 0;
		for(TSNode t : allNodes) {
			if (t.isHeadMarked()) result.set(index);
			index++;
		}
		return result;
	}
	
	public void assignHeadFromBitSet(BitSet bs) {		
		List<TSNode> allNodes = this.collectAllNodes();
		int index = bs.nextSetBit(0);
		while(index!=-1) {
			allNodes.get(index).headMarked=true;
			index = bs.nextSetBit(index+1);
		}
	}
	
	public void hasNonMinimalHeads(boolean countPunctuation, int sentenceNumber) {
		List<TSNode> allNodes = this.collectNonTerminalNodes();
		List<TSNode> allTerminals = this.collectTerminals();
		IdentityHashMap<TSNode, Integer> hights = new IdentityHashMap<TSNode, Integer>(); 
		for(TSNode t : allTerminals) {
			 hights.put(t, t.hight());			
		}
		for(TSNode n : allNodes) {
			TSNode terminalHead = n.getAnchorThroughPercolation();
			int terminalHeadHight = hights.get(terminalHead);
			List<TSNode> terminals = n.collectTerminals();
			TSNode minimalHead = terminalHead;
			int minimalHight = terminalHeadHight;
			for(TSNode t : terminals) {
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
						"\tParent: " + n.label + 
						"  Current Head: " + terminalHead.label + 
						"  Min Head: " + minimalHead.label);
			}
		}
	}
	
	public static void main(String args[]) {
		
	}

	


}
