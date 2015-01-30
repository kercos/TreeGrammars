package tdg;
import settings.Parameters;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import tsg.TSNode;
import tsg.corpora.Wsj;
import util.*;
import util.file.FileUtil;

import java.io.*;
import java.util.*;

import pos.OpenClosePos;


public class TDNode implements Serializable, Comparable<TDNode> {
	
	public final static int FORMAT_MALT_TAB = 0;
	public final static int FORMAT_MST = 1;	
	public final static int FORMAT_CONNL = 2;
	
	public final static int default_base_index = 1;
	public final static int default_root_index = 0;

	private static final long serialVersionUID = 0L;
	public static int rootIndex = -1;
	
	public String lex, postag;
	public int index;
	public TDNode parent;
	public TDNode[] leftDaughters, rightDaughters;
	
		
	/**
	 * Default contructor
	 */
	public TDNode() {
	}
	
	public TDNode(String[] wordArray, String[] postagsArray, int[] indexesArray) {
		this(wordArray, postagsArray, indexesArray, default_base_index, default_root_index);
	}
	
	public TDNode(String[] wordArray, String[] postagsArray, int[] indexesArray, 
			int baseIndex, int rootIndex) {
		
		int sentenceLength = wordArray.length;
		TDNode[] treeStructure = new TDNode[sentenceLength];
		ArrayList<ArrayList<TDNode>> leftDaughtersNodes = 
			new ArrayList<ArrayList<TDNode>>(sentenceLength);
		ArrayList<ArrayList<TDNode>> rightDaughtersNodes = 
			new ArrayList<ArrayList<TDNode>>(sentenceLength);
		for(int i=0; i<sentenceLength; i++) {
			boolean isRoot = (indexesArray[i]==rootIndex);
			treeStructure[i] = (isRoot) ? this : (new TDNode()); 
			leftDaughtersNodes.add(new ArrayList<TDNode>());
			rightDaughtersNodes.add(new ArrayList<TDNode>());
		}
		for(int i=0; i<sentenceLength; i++) {
			int parentIndex = indexesArray[i]; //1-based index
			if (parentIndex!=rootIndex) {
				parentIndex -= baseIndex; //0-based index
				TDNode parentNode = treeStructure[parentIndex];
				treeStructure[i].parent = parentNode;
				if (i<parentIndex) leftDaughtersNodes.get(parentIndex).add(treeStructure[i]);
				else rightDaughtersNodes.get(parentIndex).add(treeStructure[i]);
			}
			treeStructure[i].index = i;
			treeStructure[i].lex = wordArray[i];
			treeStructure[i].postag = postagsArray[i];			
		}
		for(int i=0; i<sentenceLength; i++) {
			treeStructure[i].leftDaughters = 
				leftDaughtersNodes.get(i).toArray(new TDNode[]{});			
			treeStructure[i].rightDaughters = 
				rightDaughtersNodes.get(i).toArray(new TDNode[]{});
			if (treeStructure[i].leftDaughters.length==0) treeStructure[i].leftDaughters=null;
			if (treeStructure[i].rightDaughters.length==0) treeStructure[i].rightDaughters=null;
		}
		
	}
	
	public static TDNode from(String sentenceStructure, int format) {
		return from(sentenceStructure, default_base_index, default_root_index, format);
	}
	
	public static TDNode from(String sentenceStructure, int baseIndex, int rootIndex, int format) {
		switch(format) {
		case FORMAT_MALT_TAB:
			return from_Malt_Tab(sentenceStructure.split("\n"), baseIndex, rootIndex);
		case FORMAT_MST:
			return from_MST(sentenceStructure.split("\n"), baseIndex, rootIndex);
		case FORMAT_CONNL:
			return from_CONNL(sentenceStructure.split("\n"), baseIndex, rootIndex);
		}
		return null;
	}
		
	/**
	 * MALT-TAB
	 * Constructor starting from an array of Strings
	 * each representing a list of three items (word '\t' postag '\t' parentIndex)
	 * parentIndex is assumed to be 1-based 
	 * @param sentenceStructure
	 */
	public static TDNode from_Malt_Tab(String[] sentenceStructure, int baseIndex, int rootIndex) {
		int sentenceLength = sentenceStructure.length;
		TDNode result = new TDNode();
	    TDNode[] treeStructure = new TDNode[sentenceLength];
	    ArrayList<ArrayList<TDNode>> leftDaughtersNodes =
	            new ArrayList<ArrayList<TDNode>>(sentenceLength);
	    ArrayList<ArrayList<TDNode>> rightDaughtersNodes =
	            new ArrayList<ArrayList<TDNode>>(sentenceLength);
	    String rootIndexString = "\t"+rootIndex;
	    for(int i=0; i<sentenceLength; i++) {        		 
	            boolean isRoot = sentenceStructure[i].endsWith(rootIndexString);
	            treeStructure[i] = (isRoot) ? result : (new TDNode());
	            leftDaughtersNodes.add(new ArrayList<TDNode>());
	            rightDaughtersNodes.add(new ArrayList<TDNode>());
	    }
	    for(int i=0; i<sentenceLength; i++) {
	            String line = sentenceStructure[i];
	            String[] triple = line.split("\t");
	            int parentIndex = Integer.parseInt(triple[2]); //1-based index
	            if (parentIndex!=rootIndex) {
	                    parentIndex -= baseIndex; //0-based index
	                    TDNode parentNode = treeStructure[parentIndex];
	                    treeStructure[i].parent = parentNode;
	                    if (i<parentIndex) leftDaughtersNodes.get(parentIndex).add(treeStructure[i]);
	                    else rightDaughtersNodes.get(parentIndex).add(treeStructure[i]);
	            }
	            treeStructure[i].index = i;
	            treeStructure[i].lex = triple[0];
	            treeStructure[i].postag = triple[1];
	    }
	    for(int i=0; i<sentenceLength; i++) {
	            treeStructure[i].leftDaughters =
	                    leftDaughtersNodes.get(i).toArray(new TDNode[]{});
	            treeStructure[i].rightDaughters =
	                    rightDaughtersNodes.get(i).toArray(new TDNode[]{});
	            if (treeStructure[i].leftDaughters.length==0) treeStructure[i].leftDaughters=null;
	            if (treeStructure[i].rightDaughters.length==0) treeStructure[i].rightDaughters=null;
	    }
	    return result;
	}
	
	public static TDNode from_MST(String[] sentenceStructure, int baseIndex, int rootIndex) {
		// words postags (labels) indexes
		boolean isLabeled = (sentenceStructure.length==4);
		String words = sentenceStructure[0];
		String postags = sentenceStructure[1];
		//String labels = null;
		String indexes = null;
		if (isLabeled) {
			//labels = sentenceStructure[2];
			indexes = sentenceStructure[3];
		}
		else indexes = sentenceStructure[2];
		return new TDNode(words.split("\t"), postags.split("\t"), 
				Utility.parseIndexList(indexes.split("\t")), baseIndex, rootIndex);
	}
	
	public static TDNode from_CONNL(String[] sentenceStructure, int baseIndex, int rootIndex) {
		int length = sentenceStructure.length;
		int lengthWithEOS = length+1;
		String[] words = new String[lengthWithEOS];
		String[] pos = new String[lengthWithEOS];
		int[] indexes = new int[lengthWithEOS];
		for(int l=0; l<length; l++) {
			String line = sentenceStructure[l];
			String[] fields = line.split("\t");
			words[l] = fields[1] + "|" + fields[2]; //lex + lemma
			pos[l] = fields[3] + "|" + fields[4] + "|" + fields[5]; // coarse POS + fine POS + morpho-syntax feats
			if (fields[6].equals("_")) fields[6] = Integer.toString(l);
			int headIndex = Integer.parseInt(fields[6]);
			indexes[l] = (headIndex==rootIndex) ? lengthWithEOS : headIndex;
		}
		// adding EOS
		words[length] = "EOS";
		pos[length] = "EOS";
		indexes[length] = rootIndex;
		return new TDNode(words, pos, indexes, baseIndex, rootIndex);
	}

	/**
     * Copy constructor with new copy of daughters (no references).
     */
	public TDNode(TDNode original) {
		this.lex = original.lex;
		this.postag = original.postag;
		this.index = original.index;
		if (original.leftDaughters!=null) {
			this.leftDaughters = new TDNode[original.leftDaughters.length];
			for(int d=0; d<original.leftDaughters.length; d++) {
				this.leftDaughters[d] = new TDNode(original.leftDaughters[d]);
				this.leftDaughters[d].parent = this;
			}
		}
		if (original.rightDaughters!=null) {
			this.rightDaughters = new TDNode[original.rightDaughters.length];
			for(int d=0; d<original.rightDaughters.length; d++) {
				this.rightDaughters[d] = new TDNode(original.rightDaughters[d]);
				this.rightDaughters[d].parent = this;
			}
		}				
	}
	
	public int compareTo(TDNode o) {
		return (index<o.index ? -1 : (index==o.index ? 0 : 1));
	}
	
	public int hashCode() {
		String label = lex;
		if (leftDaughters!=null) for(TDNode d : leftDaughters) label += d.lex;
		if (rightDaughters!=null) for(TDNode d : rightDaughters) label += d.lex;
		return label.hashCode();
	}
	
	/**
	 * Equality function
	 */
	public boolean equals(Object anObject) {
		if (this == anObject) {
		    return true;
		}
		if (anObject instanceof TDNode) {
			TDNode anotherNode = (TDNode)anObject;
			return this.equals(anotherNode);
		}
		return false;
	}
	
	/**
	 * Equality function
	 * @param O
	 * @return
	 */
	public boolean equals(TDNode O) {
		if (!this.sameLexPosTag(O)) return false;
		if (this.index!=O.index) return false;
		if (!Arrays.equals(this.leftDaughters, O.leftDaughters)) return false;
		if (!Arrays.equals(this.rightDaughters, O.rightDaughters)) return false;
		return true;		
	}
	
	/**
	 * Returns the depth of the current TDNode (0 for the root).
	 * @return
	 */
	public int depth() {
		int depth = 0;
		TDNode p = this.parent;
		while(p!=null) {			
			p = p.parent;
			depth++;
		}
		return depth;
	}
	
	/**
	 * Returns the daughters of the current node as array of array.
	 * @return
	 */
	public TDNode[][] daughters() {
		return new TDNode[][]{this.leftDaughters, this.rightDaughters};
	}
	
	/**
	 * Number of left daughters
	 * @return
	 */
	public int leftProle() {
		if (this.leftDaughters==null) return 0;
		return this.leftDaughters.length;
	}
	
	/**
	 * Number of right daughters
	 * @return
	 */
	public int rightProle() {
		if (this.rightDaughters==null) return 0;
		return this.rightDaughters.length;
	}
	
	
	/**
	 * Returns 1 + the number of TreeDepNode attached to the current node
	 * @return
	 */
	public int length() {
		int result = 1;
		if (this.leftDaughters!=null) {
			for(TDNode TDN : this.leftDaughters) {
				result += TDN.length();
			}
		}
		if (this.rightDaughters!=null) {
			for(TDNode TDN : this.rightDaughters) {
				result += TDN.length();
			}
		}
		return result;
	}
	
	public int lengthWithoutEmpty() {
		int result = (isEmptyDaughter())? 0 : 1;
		if (this.leftDaughters!=null) {
			for(TDNode TDN : this.leftDaughters) {
				result += TDN.lengthWithoutEmpty();
			}
		}
		if (this.rightDaughters!=null) {
			for(TDNode TDN : this.rightDaughters) {
				result += TDN.lengthWithoutEmpty();
			}
		}
		return result;
	}
	
	public int lengthWithEmpty() {
		int result = 1;
		if (this.leftDaughters==null) result++;
		else for(TDNode TDN : this.leftDaughters) {
			result += TDN.lengthWithEmpty();
		}
		if (this.rightDaughters==null) result++;
		else for(TDNode TDN : this.rightDaughters) {
			result += TDN.lengthWithEmpty();
		}
		return result;
	}
	
	/**
	 * Returns an array of TDNode each for a node of the current TDNode.
	 * Each node is at the position referred by its index.
	 * @return
	 */
	public TDNode[] getStructureArray() {
		TDNode[] result = new TDNode[length()];
		addInStructureArray(result);
		return result;
	}
	
	/**
	 * To implement getStructureArray
	 * @param result
	 */
	private void addInStructureArray(TDNode[] result) {
		result[this.index] = this;
		if (this.leftDaughters!=null) {
			for(TDNode TDN : this.leftDaughters) {
				TDN.addInStructureArray(result);
			}
		}
		if (this.rightDaughters!=null) {
			for(TDNode TDN : this.rightDaughters) {
				TDN.addInStructureArray(result);
			}
		}
	}
	
	public TDNode[] getStructureArrayWithoutEmpty() {
		TDNode[] result = new TDNode[lengthWithoutEmpty()];
		TDNode[] allDaughters = getStructureArray();
		int i = 0;
		for(TDNode tdn : allDaughters) {
			if (tdn.isEmptyDaughter()) continue;
			result[i] = tdn;
			i++;
		}
		return result;
	}
	
	public void addEmptyDaughters() {
		TDNode[] structure = this.getStructureArray();
		int newIndex = 0;
		for(TDNode n : structure) {
			if (n.leftDaughters==null) {
				TDNode empty = n.getEmptyDaughter(newIndex);
				n.leftDaughters = new TDNode[]{empty};
				newIndex++;
			}
			n.index = newIndex;
			newIndex++;
			if (n.rightDaughters==null) {
				TDNode empty = n.getEmptyDaughter(newIndex);
				n.rightDaughters = new TDNode[]{empty};
				newIndex++;
			}			
		}
	}
	
	public void addEOS() {
		int length = this.length();
		TDNode copyNode = new TDNode();
		copyNode.lex = new String(this.lex);
		copyNode.postag = new String(this.postag);
		if (this.leftDaughters != null) {
			copyNode.leftDaughters = this.leftDaughters;
			for(TDNode ld : leftDaughters)  ld.parent = copyNode;
		}
		if (this.rightDaughters != null) {
			copyNode.rightDaughters = this.rightDaughters;
			for(TDNode rd : rightDaughters)  rd.parent = copyNode;			
		}
		copyNode.index = this.index;
		copyNode.parent = this;
		this.leftDaughters = new TDNode[]{copyNode};
		this.rightDaughters = null;
		this.index = length;
		this.lex = "EOS";
		this.postag = "EOS";
	}
	
	public static void addEOS(TDNode[] array) {
		for(TDNode n : array) {
			if (n!=null) n.addEOS();
		}
	}
	
	public void removeEOS() {
		TDNode top = this.leftDaughters[0];
		this.lex = top.lex;
		this.postag = top.postag;
		this.index = top.index;
		this.leftDaughters = top.leftDaughters;
		this.rightDaughters = top.rightDaughters;		
		if (this.leftDaughters != null) {
			for(TDNode ld : this.leftDaughters) ld.parent = this;
		}
		if (this.rightDaughters != null) {
			for(TDNode rd : this.rightDaughters) rd.parent = this;
		}
	}
	
	public void removeEmptyDaughters() {
		TDNode[] structure = this.getStructureArray();
		int newIndex = 0;
		for(TDNode n : structure) {
			if (n.isEmptyDaughter()) continue;
			if (n.leftDaughters!=null && n.leftDaughters[0].isEmptyDaughter()) {
				n.leftDaughters = null;
			}
			if (n.rightDaughters!=null && n.rightDaughters[0].isEmptyDaughter()) {
				n.rightDaughters = null;
			}
			n.index = newIndex;
			newIndex++;
		}
	}
	
	private TDNode getEmptyDaughter(int newIndex) {
		TDNode emptyDaughter = new TDNode();
		emptyDaughter.index = newIndex;
		emptyDaughter.lex = "";
		emptyDaughter.postag = "";
		emptyDaughter.parent = this;
		return emptyDaughter;
	}
	
	public boolean isEmptyDaughter() {
		return (this.lex=="" && this.postag=="");
	}
	
	/**
	 * Returns an array of String each for a node of the current TDNode.
	 * Each element is the label of the corresponding node of the corresponding index.
	 * @return
	 */
	public String[] getStructureLabelsArray() {
		String[] result = new String[length()];
		getStructureLabelsArray(result);
		return result;
	}
	
	/**
	 * To implement getStructureArray
	 * @param result
	 */
	private void getStructureLabelsArray(String[] result) {
		result[this.index] = this.lexPosTag();
		if (this.leftDaughters!=null) {
			for(TDNode TDN : this.leftDaughters) {
				TDN.getStructureLabelsArray(result);
			}
		}
		if (this.rightDaughters!=null) {
			for(TDNode TDN : this.rightDaughters) {
				TDN.getStructureLabelsArray(result);
			}
		}
	}
	
	
	/**
	 * Returns a list of the CFDG rules, one for each node.
	 * P l1 l2 d1 d2 (if direction is false)
	 * P l1 l2 * d1 d2 (if direction is true)
	 * @param direction
	 * whether the rules distinguish left from right children 
	 * @return
	 */
	public List<String> getCFDRListPos(boolean direction) {
		List<String> result = new ArrayList<String>();
		getCFDRListPos(result, direction);
		return result;
	}
	
	/**
	 * To implement getCFDRListPos
	 * @param list
	 * @param direction
	 */
	private void getCFDRListPos(List<String> list, boolean direction) {
		if (this.leftDaughters==null && this.rightDaughters==null) return;
		String CFDR = this.postag;
		if (this.leftDaughters!=null) {			
			for(TDNode TDN: this.leftDaughters) {
				TDN.getCFDRListPos(list, direction);
				CFDR += " " + TDN.postag;
			}
		}
		if (this.rightDaughters!=null) {
			if (direction) CFDR += " *";
			for(TDNode TDN: this.rightDaughters) {
				TDN.getCFDRListPos(list, direction);
				CFDR += " " + TDN.postag;
			}
		}
		list.add(CFDR);
	}
	
	/**
	 * Returns the current lexicon and postag: lex|postag
	 * @return
	 */
	public String lexPosTag() {
		return this.lex + "|" + this.postag;
	}
	
	
	/**
	 * 
	 * @param SLP_type
	 * 			0: postag
	 *			1: lex
	 *			2: lexpostag
	 * @return
	 */
	public String lexPosTag(int SLP_type) {
		switch(SLP_type) {
		case 0: return this.postag;// return removeMarks(this.postag); //
		case 1: return this.lex;
		case 2: return this.lexPosTag();		
		}
		return null;
	}
	
	public static OpenClosePos posAnalyzer;
	
	/**
	 * 
	 * @param SLP_type
	 *		0: lexPos
	 *		1: lexShortPos
	 *		2: Pos
	 *		3: shortPos 
	 * @return
	 */
	public String lexPosTag2(int reduction_type_ifOpen, int reduction_type_ifClose) {
		String posWithoutMarks = removeMarks(postag);
		int reduction_type = (posAnalyzer.isOpenClass(posWithoutMarks)) ? 
				reduction_type_ifOpen : reduction_type_ifClose;		
		switch((reduction_type) ) {
			case 0: return lex; // + "|" + posWithoutMarks;
			case 1: return lex.toLowerCase(); // + "|" + Wsj.getShortPos(posWithoutMarks);
			case 2: return posWithoutMarks;
			case 3: return posAnalyzer.getSuperShortPos(removeMarks(posWithoutMarks));
		}
		return null;
	}
	
	public static String removeMarks(String s) {
		while (s.length()>1 && s.charAt(s.length()-2) == '-') {
			s = s.substring(0, s.length()-2);
		}
		return s;
	}
	
	/**
	 * Returns the current lexicon and postag: lex|postag
	 * @return
	 */
	public String lexPosTagIndex() {
		return this.lex + "|" + this.postag + "|" + this.index;
	}
	
	public String toString() {		
		return "(" + lexPosTagIndex() + ")\n" + this.root().toStringSentenceStructure();
		/*String leftDaughterPrint = (leftDaughters==null) ? "null" : 
									leftDaughters[leftDaughters.length-1].lexPosTagIndex();
		String rightDaughterPrint = (rightDaughters==null) ? "null" : 
									rightDaughters[0].lexPosTagIndex();
		return this.lexPosTagIndex() + " < " + leftDaughterPrint + " > " + rightDaughterPrint;*/
	}
	
	/**
	 * Returns the string representation of the current tree
	 */
	public String toStringStar() {		
		return "( " + this.toStringRecursive() + ")"; 		
	}
	
	public String toStringStart(BitSet gst) {
		if (gst.get(this.index))
			return "( " + this.toStringRecursive(gst) + ")";
		else return "()";
	}
	
	public String toStringFlat() {
		String result = "";
		TDNode[] structure = this.getStructureArray();
		for(TDNode t : structure) {
			result += t.lex + " ";
		}
		return result.trim();
	}
	
	
	/**
	 * To implement toString()
	 * @return
	 */
	private String toStringRecursive() {
		String result = this.lexPosTag();	
		if (this.leftDaughters==null && this.rightDaughters==null) return result;
		result += " (";
		if (this.leftDaughters!=null) {			
			for(TDNode TDN : this.leftDaughters) {
				result += " " + TDN.toStringRecursive();
			}
		}
		result += " * ";
		if (this.rightDaughters!=null) {
			for(TDNode TDN : this.rightDaughters) {
				result += " " + TDN.toStringRecursive();
			}
		}
		result += ") ";
		return result;
	}
	
	public String toStringRecursive(BitSet gst) {
		String result = this.lexPosTag();
		if (this.leftDaughters==null && this.rightDaughters==null) return result;
		result += " (";
		if (this.leftDaughters!=null) {
			for(TDNode TDN : this.leftDaughters) {
				if (gst.get(TDN.index)) result += toStringRecursive(gst);
			}
		}		
		result += " * ";
		if (this.rightDaughters!=null) {
			for(TDNode TDN : this.rightDaughters) {
				if (gst.get(TDN.index)) result += toStringRecursive(gst);
			}
		}
		result += ") ";
		return result;
		
	}
	
	public String toStringSentenceStructure() {
		String result = "";
		TDNode[] structure = this.getStructureArray();
		for(TDNode t : structure) {
			result += 
				t.lex + "\t" + 
				t.postag + "\t" + 
				((t.parent==null)? rootIndex : t.parent.index+1) + 
				"\n";
		}
		return result;
	}
	
	public String toStringMALTulab(int rootIndex, boolean blind) {
		return toStringMALTulab(rootIndex, blind, true);
	}
	
	public String toStringMALTulab(int rootIndex, boolean blind, boolean useLabel) {
		String result = "";
		TDNode[] structure = this.getStructureArray();
		for(TDNode t : structure) {
			int index = -1;
			String lab = "";
			if (t.parent==null) {
				index = rootIndex;
				lab = "ROOT";
			}
			else {
				index = t.parent.index+1;
				lab = "LAB";
			}
			result += 	t.lex + "\t" + 
						t.postag + 
						((blind) ? "" : "\t" + index)  +
						((useLabel) ? "\t" + lab : "") +
						"\n";
		}
		return result;
	}
	
	public String toStringMSTulab(boolean blind) {
		return toStringMSTulab(blind, false);
	}
	
	/**
	 * word
	 * postag
	 * index
	 * @param givenRootIndex
	 * @param blind
	 * @return
	 */
	public String toStringMSTulab(boolean blind, boolean removeEmptyDaughters) {
		if (removeEmptyDaughters) this.removeEmptyDaughters();
		int givenRootIndex = 0;
		String[] result = new String[]{"","",""};
		TDNode[] structure = this.getStructureArray();
		for(int i=0; i<structure.length; i++) {
			TDNode t = structure[i];
			if (i==structure.length-1) {
				result[0] += t.lex;
				result[1] += t.postag;
				if (blind) result[2] += 0; 
				else result[2] += ((t.parent==null)? givenRootIndex : t.parent.index+1);
			}
			else {
				result[0] += t.lex + "\t";
				result[1] += t.postag + "\t";
				if (blind) result[2] += 0 + "\t";
				else result[2] += ((t.parent==null)? givenRootIndex : t.parent.index+1) + "\t";
			}
		}
		if (removeEmptyDaughters) this.addEmptyDaughters();
		return result[0]+"\n"+result[1]+"\n"+result[2];
	}
	
	/**
	 * word
	 * postag
	 * index
	 * @param givenRootIndex
	 * @param blind
	 * @return
	 */
	public String toStringMSTulab(BitSet gst) {
		int givenRootIndex = 0;
		String[] result = new String[]{"","",""};
		TDNode[] structure = this.getStructureArray();
		for(int i=0; i<structure.length; i++) {
			TDNode t = structure[i];
			if (!gst.get(t.index)) continue;
			if (i==structure.length-1) {
				result[0] += t.lex;
				result[1] += t.postag;
				result[2] += ((t.parent==null)? givenRootIndex : t.parent.index+1);
			}
			else {
				result[0] += t.lex + "\t";
				result[1] += t.postag + "\t";
				result[2] += ((t.parent==null)? givenRootIndex : t.parent.index+1) + "\t";
			}
		}
		return result[0]+"\n"+result[1]+"\n"+result[2];
	}

	/**
	 * Whether the current node has the same lex of the otherTree 
	 * @param otherTree
	 * @return
	 */
	public boolean sameLex(TDNode otherTree) {
		return (this.lex.equals(otherTree.lex));
	}
	
	/**
	 * Whether the current node has the same lexPostag of the otherTree 
	 * @param otherTree
	 * @return
	 */
	public boolean sameLexPosTag(TDNode otherTree) {
		return (this.lex.equals(otherTree.lex) && this.postag.equals(otherTree.postag));
	}
	
	public boolean sameLPtype(TDNode otherNode, int SLP_type) {
		return this.lexPosTag(SLP_type).equals(otherNode.lexPosTag(SLP_type));
	}
	
	/**
	 * Whether the current node has the same Postag of the otherTree 
	 * @param otherTree
	 * @return
	 */
	public boolean samePosTag(TDNode otherTree) {
		return this.postag.equals(otherTree.postag);
	}
	
	/**
	 * Compute the Unlabeled Attachment Score between the current node
	 * and the otherTree.
	 * @param otherTree
	 * @return
	 * An array of two int: UAS e total lexical words
	 */
	public int[] UAS_Total(TDNode otherTree) {
		int[] result = new int[2]; //score, total						
		result[0] = this.UAS(otherTree);
		if (result[0]==-1) return null;
		result[1] = otherTree.length();		
		return result;
	}
	
	/**
	 * Compute the Unlabeled Attachment Score between the current node
	 * and the otherTree.
	 * @param otherTree
	 * @return
	 * An array of two int: UAS e total lexical words
	 */
	public void UAS_Total(TDNode otherTree, int[] result) {
		result[0] += this.UAS(otherTree); 
		result[1] += otherTree.length();		
	}
	
	/**
	 * Compute the Unlabeled Attachment Score between the current node
	 * and the otherTree.
	 * @param otherTree
	 * @return
	 */
	private int UAS(TDNode otherTree) {
		int result = 0; //score, total				
		TDNode[] thisStructureArray = this.getStructureArray();
		TDNode[] otherStructureArray = otherTree.getStructureArray();
		if (thisStructureArray.length!=otherStructureArray.length) {
			System.err.println("Trees not comparable.");
			return -Integer.MAX_VALUE;
		}
		for(int i=0; i<thisStructureArray.length; i++) {
			TDNode parentA, parentB;
			parentA = thisStructureArray[i].parent;
			parentB = otherStructureArray[i].parent;
			/*if (!thisStructureArray[i].sameLexPosTag(otherStructureArray[i])) {
				System.err.println("Trees not comparable: " + 
						thisStructureArray[i].lexPosTag() + "\t" + 
						otherStructureArray[i].lexPosTag());
				//return -1;
			}*/
			if  (parentA==null || parentB==null) {
				if (parentA!=null || parentB!=null) {
					System.err.println("Trees not comparable. Don't have EOS as root.");
					return -Integer.MAX_VALUE;
				}
			}
			else if (parentA.index==parentB.index) result++;
		}
		return result;
	}
	
	/**
	 * Compute the Unlabeled Attachment Score between the current node
	 * and the otherTree.
	 * @param otherTree
	 * @return
	 */
	public int UAS(TDNode otherTree, boolean removeEmptyDaughters, boolean addEOS) {
		if (removeEmptyDaughters) {
			this.removeEmptyDaughters();
			otherTree.removeEmptyDaughters();
		}
		if (addEOS) {
			this.addEOS();
			otherTree.addEOS();
		}
		int result = this.UAS(otherTree);
		if (removeEmptyDaughters) {
			this.addEmptyDaughters();
			otherTree.addEmptyDaughters();
		}
		if (addEOS) {
			this.removeEOS();
			otherTree.removeEOS();
		}
		return result;
	}
		
	public TDNode[] siblingArray() {
		return (this.index<parent.index) ? parent.leftDaughters : parent.rightDaughters;
	}
	
	public TDNode[] sisters() {		 
		if (parent==null) return null;
		TDNode[] result = new TDNode[2];
		TDNode[] daughters = siblingArray();
		int length=daughters.length;
		if (length==1) return result;
		int index = indexOf(this, daughters);
		if (index>0) result[0] = daughters[index-1];
		if (index<length-1) result[1] = daughters[index+1];
		return result;
	}
	
	private void addRightRightmostDescendents(TreeSet<TDNode> list, TDNode toExclude) {
		if (this==toExclude) return;
		list.add(this);
		TDNode rightmostDaughter = this.rightmostDaughter();
		if (rightmostDaughter!=null) {
			rightmostDaughter.addRightRightmostDescendents(list, toExclude);
		}
	}
	
	private void addLeftLeftmostDescendents(TreeSet<TDNode> list, TDNode toExclude) {
		if (this==toExclude) return;
		list.add(this);
		TDNode leftmostDaughter = this.leftmostDaughter();
		if (leftmostDaughter!=null) {
			leftmostDaughter.addLeftLeftmostDescendents(list, toExclude);
		}
	}
	
	public TDNode rightmostDaughter() {
		return (rightDaughters==null) ? null : rightDaughters[rightDaughters.length-1];
	}

	public TDNode leftmostDaughter() {
		return (leftDaughters==null) ? null : leftDaughters[0];
	}
	
	public static TDNode[] daughtersPair(TDNode[] daughters, int index) {
		TDNode[] result = new TDNode[2];		
		if (index>0) result[0] = daughters[index-1];
		if (index<daughters.length) result[1] = daughters[index];
		return result;
	}
	
	/*public TDNode[] daughtersPair(int index, int daughterSet) {
		TDNode[] result = null; 				
		if (daughterSet==0) {
			result = daughtersPair(leftDaughters,index);
			if (result[1]==null && rightDaughters!=null) result[1] = rightDaughters[0]; 
		}
		else {
			result = daughtersPair(rightDaughters,index);
			if (result[0]==null && leftDaughters!=null) result[0] = leftDaughters[leftProle()-1];
		}
		return result;
	}*/

	
	public boolean isBoundaryChild() {
		return (parent.leftmostDaughter()==this || parent.rightmostDaughter()==this);
	}
	
	public ArrayList<TDNodePair> oneStepVariationPairs(TDNode excludeParent) {
		TreeSet<TDNode> parentsSet = this.oneStepVariationParents();
		if (excludeParent!=null) parentsSet.remove(excludeParent);
		ArrayList<TDNodePair> result = new ArrayList<TDNodePair>(parentsSet.size());
		for(TDNode newParent : parentsSet) result.add(new TDNodePair(this,newParent));
		return result;		
	}
	
	public TreeSet<TDNode> oneStepVariationParents() {
		TreeSet<TDNode> result = new TreeSet<TDNode>();
		oneStepVariationParents(result);
		//result.remove(parent);
		//result.remove(this);
		//System.out.print("OneStepVariationParents from " + lex + "(" + index + "): ");
		//for(TDNode t : result) System.out.print(t.lex + " ");
		//System.out.println();
		return result;
	}
	
	private void oneStepVariationParents(TreeSet<TDNode> list) {
		TDNode[] sisters = this.sisters();
		if (sisters==null) return; //this is root --> no one step variations
		if (sisters[0]!=null) sisters[0].addRightRightmostDescendents(list, null);
		if (sisters[1]!=null) sisters[1].addLeftLeftmostDescendents(list, null);
		if (!this.isBoundaryChild()) return;
		TDNode ancestor = this.parent.parent;
		while(ancestor!=null) {
			list.add(ancestor);
			if (this.index<ancestor.index) {
				int position = canonicPosition(ancestor.leftDaughters, this);
				sisters = daughtersPair(ancestor.leftDaughters, position);
				if (sisters[1]!=null) sisters[1].addLeftLeftmostDescendents(list, parent);
				if (sisters[0]!=null) {
					sisters[0].addRightRightmostDescendents(list, parent);					
					break;
				}				
			}
			else {
				int position = canonicPosition(ancestor.rightDaughters, this);
				sisters = daughtersPair(ancestor.rightDaughters, position);
				if (sisters[0]!=null) sisters[0].addRightRightmostDescendents(list, parent);
				if (sisters[1]!=null) {
					sisters[1].addLeftLeftmostDescendents(list, parent);					
					break;
				}			
			}
			ancestor = ancestor.parent;
		}
	}
	
	public static int canonicPosition(TDNode[] daughters, TDNode A) {
		int i=0;
		while(i<daughters.length && A.index > daughters[i].index) i++;
		return i;
	}
	
	/**
	 * Returns a TDNode which contains one random variation
	 * from the current node. Chooses a random node from the current 
	 * (different from the root) and applies there a variation.  
	 * @return
	 */
	public TDNode randomVariation() {
		TDNode copy = new TDNode(this);
		TDNode[] newStructureArray = copy.getStructureArray();
		int randomIndex = -1;
		TDNode B = null;
		do {
			randomIndex = Utility.randomInteger(newStructureArray.length);
			B = newStructureArray[randomIndex];
		} while(B.parent==null);
		variation(B);		
		while(B.parent != null) B = B.parent;	
		return B;
	}
	
	/**
	 * Applies a fix number of variations in the current node, and returns
	 * the node resulting from the series of variations.
	 * @param repetitions
	 * Number of variations.
	 * @return
	 */
	public TDNode randomVariationSeries(int repetitions) {
		TDNode copy = new TDNode(this);
		TDNode[] newStructureArray = copy.getStructureArray();
		int randomIndex = -1;
		TDNode B = null;
		for(int r=0; r<repetitions; r++) {
			randomIndex = Utility.randomInteger(newStructureArray.length-1);
			if (randomIndex>=copy.index)  randomIndex++;
			B = newStructureArray[randomIndex];
			variation(B);			
		}	
		while(B.parent != null) B = B.parent;
		return B;	
	}
	
	/**
	 * Applies a fix number of variations in the current node, and return a 
	 * list of node resulting at each stage of the variations.
	 * @param repetitions
	 * Number of variations.
	 * @return
	 */
	public ArrayList<TDNode> randomVariationSeriesIntermediate(int repetitions) {
		ArrayList<TDNode> result = new ArrayList<TDNode>(repetitions);
		result.add(this);
		int length = this.length();
		TDNode current = this;
		while(result.size()<repetitions) {
			int randomIndex = Utility.randomInteger(length-1);
			if (randomIndex>=current.index)  randomIndex++;
			TDNode newTDN = current.variation(randomIndex);
			result.add(newTDN);
			current = newTDN;				
			//System.out.println(newTDN.toString());
			System.out.println(this.UAS(newTDN));
		}
		return result;	
	}
	
	public int[] randomVariationSeriesIntermediateUAS(int repetitions) {
		int[] result = new int[repetitions];
		int length = this.length();
		TDNode current = this;
		for(int i=0; i<repetitions; i++) {
			int randomIndex = Utility.randomInteger(length-1);
			if (randomIndex>=current.index)  randomIndex++;
			TDNode newTDN = current.variation(randomIndex);
			current = newTDN;				
			System.out.println(this.UAS(newTDN));
		}
		return result;	
	}
	
	
	/**
	 * Applies a variation in a copy of the current tree, at a given node, and
	 * return the tree copy.
	 * The node on which the variation occurs should be different from
	 * the root of the tree (otherwise null is returned).
	 * The three possible transformation are: head and left/right sister (the last
	 * two only if the node has a left/right sister).
	 * @param Bindex
	 * Index of the node on which to apply the transformation.
	 * @return
	 */
	public TDNode variation(int Bindex) {
		TDNode copy = new TDNode(this);
		TDNode[] newStructureArray = copy.getStructureArray();
		TDNode B = newStructureArray[Bindex];
		if (B.parent==null) return null;
		variation(B);		
		while(B.parent != null) B = B.parent;	
		return B;	
	}
	
	/**
	 * Applies a variation in a copy of the current tree, at a given node, and
	 * return the tree copy.
	 * The node on which the variation occurs should be different from
	 * the root of the tree (otherwise null is returned).
	 * The three possible transformation are: head and left/right sister (the last
	 * two only if the node has a left/right sister).
	 * @param B
	 * Node at which to apply the transformation.
	 */
	public static void variation(TDNode B) {
		TDNode P = B.parent; 
		TDNode[] daughters = (B.index<P.index) ? P.leftDaughters : P.rightDaughters;
		if (daughters.length==1 || Utility.randomBoolean()) B.parentFlip();
		else {
			int index = indexOf(B, daughters);
			if (index==0) B.rightSisterFlip(daughters[1]);
			else if (index==daughters.length-1) B.leftSisterFlip(daughters[daughters.length-2]);
			else {
				if (Utility.randomBoolean()) B.leftSisterFlip(daughters[index-1]);
				else B.rightSisterFlip(daughters[index+1]);										
			}
		}		
	}
	
	public ArrayList<TDNode> oneStepVariations() {
		ArrayList<TDNode> result = new ArrayList<TDNode>();
		TDNode[] structure = this.getStructureArray();
		int[][] constituentsExtremes = getConstituentsExtremes(structure);
		System.out.println(this.toStringMSTulab(false) + "\n");
		for(TDNode t : structure) {
			if (t.parent==null) continue;
			Set<Integer> osn = t.oneStepNeighbours(constituentsExtremes, structure);
			TDNode p = t.parent;			
			for(Integer i : osn) {
				TDNode n = structure[i];
				System.out.println("Flip " + t.index + "-" + n.index);				
				t.newParentFlip(n);
				System.out.println(this.toStringMSTulab(false) + "\n");
				result.add(new TDNode(this));
				t.newParentFlip(p);
				//System.out.println("Back Flip " + t.index + "-" + p.index);
				//System.out.println(this.toStringMSTulab(false) + "\n");
			}
		}
		return result;		
	}
	
	public Set<Integer> oneStepNeighbours(int[][] constExt, TDNode[] structure) {
		TreeSet<Integer> result = new TreeSet<Integer>();
		int[] c = constExt[this.index];
		TDNode[] sisters = this.sisters();
		for(TDNode s : sisters) {
			if (s!=null && !Utility.isInInterval(s.index, c)) 
				result.add(s.index);
		}
		if (this.index!=0) {
			TDNode previousNode = structure[this.index-1];
			if (!Utility.isInInterval(previousNode.index, c)) { 
				result.add(previousNode.index);
				addAncestorsIndexes(previousNode, previousNode.parent, result, constExt);
			}
		}
		if (this.index!=structure.length-1) {
			TDNode nextNode = structure[this.index+1];
			if (!Utility.isInInterval(nextNode.index, c)) { 
				result.add(nextNode.index);
				addAncestorsIndexes(nextNode, nextNode.parent, result, constExt);
			}
		}
		addAncestorsIndexes(this, this.parent, result, constExt);
		if (c[0]!=0) result.add(c[0]-1);
		if (c[1]!=structure.length-1) result.add(c[1]+1);
		result.remove(this.parent.index);
		return result;
	}
	
	private static void addAncestorsIndexes(TDNode thisNode, TDNode ancestor,
			Set<Integer> set, int[][] constExt) {
		int[] c = constExt[thisNode.index];
		while(ancestor!=null && (Utility.isIntervalExtreme(c, constExt[ancestor.index])) ) {		
			set.add(ancestor.index);			
			ancestor = ancestor.parent;			
		}
	}
	
	
	private static int[][] getConstituentsExtremes(TDNode[] structure) {
		int length = structure.length;
		int[][] constituentsExtreemes = new int[length][];
		for(TDNode n : structure) {
			getConstituentsExtremes(n, constituentsExtreemes);
		}
		return constituentsExtreemes;
	}
	
	private static int[] getConstituentsExtremes(TDNode thisNode, int[][] constituentsExtreemes) {
		int i = thisNode.index;
		if (constituentsExtreemes[i]!=null) return constituentsExtreemes[i];
		int lowerEnd = i;
		int upperEnd = i;
		TDNode ld = thisNode.leftmostDaughter();
		if (ld!=null) {
			lowerEnd = getConstituentsExtremes(ld, constituentsExtreemes)[0];
		}
		TDNode rd = thisNode.rightmostDaughter();
		if (rd!=null) {
			upperEnd = getConstituentsExtremes(rd, constituentsExtreemes)[1];
		}
		return constituentsExtreemes[i] = new int[]{lowerEnd, upperEnd};
	}
	
	private static BitSet[] getConstituents(TDNode[] structure) {
		int length = structure.length;
		BitSet[] constituents = new BitSet[length];
		for(TDNode n : structure) {
			getConstituent(n, constituents);
		}
		return constituents;
	}
	
	private static BitSet getConstituent(TDNode thisNode, BitSet[] constituents) {
		int i = thisNode.index;
		if (constituents[i]!=null) return constituents[i];
		BitSet bs = new BitSet();
		bs.set(i);
		if (thisNode.leftDaughters!=null) {
			for(TDNode d : thisNode.leftDaughters) {
				bs.or(getConstituent(d, constituents));
			}
		}
		if (thisNode.rightDaughters!=null) {
			for(TDNode d : thisNode.rightDaughters) {
				bs.or(getConstituent(d, constituents));
			}
		}
		return constituents[i] = bs;
	}
	
	
	/**
	 * Performs a variation one at each node of the current tree (discarding the root),
	 * and returns a list containing each tree coming from each single variation.
	 * @return
	 */
	public ArrayList<TDNode> roundVariation() {
		TDNode[] structure = this.getStructureArray();
		ArrayList<TDNode> result = new ArrayList<TDNode>(structure.length);
		for(int i=0; i<structure.length; i++) {
			if (structure[i].parent==null) continue;
			TDNode newTDN = this.variation(i);
			result.add(newTDN);
			System.out.println(newTDN.toString());
			System.out.println(Arrays.toString(this.UAS_Total(newTDN)));
		}
		return result;
	}

	/**
	 * Returns a node resulting from a random series of variation,
	 * whose UAS with the current node is given as input.
	 * @param score
	 * @return
	 */
	public TDNode variationUASscore(int score) {
		int maxScore = this.length();
		int currentScore;
		TDNode result = null;
		do {
			result = this;
			currentScore = maxScore;
			while(currentScore>score) {
				result = result.randomVariation();
				currentScore = this.UAS(result);
			}
		} while(currentScore<score);		
		return result;
	}
	
	/**
	 * Returns a list of list (bins) of nodes.
	 * Each bin has (besides case of insufficient number) 
	 * a specific number of trees (binSize), 
	 * having a specific UAS with the current node.
	 * 
	 * @param binSize
	 * @param maxAttempts
	 * The number of repeat routines in the attempt to fill each binSize.
	 * @return
	 */
	public ArrayList<ArrayList<TDNode>> collectVariationUASspectrumOld(int binSize, int maxAttempts) {
		TDNode[] structure = this.getStructureArray();
		ArrayList<ArrayList<TDNode>> result = new ArrayList<ArrayList<TDNode>>(structure.length+1);
		ArrayList<TDNode> bestBinScore = new ArrayList<TDNode>(1);
		bestBinScore.add(new TDNode(this));
		result.add(bestBinScore);
		for(int i=1; i<=structure.length; i++) {
			int score = structure.length-i;			
			ArrayList<TDNode> binScore = new ArrayList<TDNode>(binSize);
			result.add(binScore);
			int currentAttempts = 0;
			while(binScore.size()<binSize && currentAttempts<maxAttempts) {
				currentAttempts++;
				TDNode newNode = this.variationUASscore(score);
				if (binScore.contains(newNode)) continue;
				binScore.add(newNode);				
			}
		}
		return result;
	}
	
	public int totalDaughtersNumber() {
		int result = 0;
		if (leftDaughters!=null) result += leftProle();
		if (rightDaughters!=null) result += rightProle();
		return result;
	}
	
	public int edgeSisters() {
		if (this.index<parent.index) {
			TDNode[] daughters = parent.leftDaughters;
			return indexOf(this, daughters);			
		}
		else {
			TDNode[] daughters = parent.rightDaughters;
			return parent.rightProle()-indexOf(this, daughters)-1;
		}
	}
	
	public ArrayList<TDNode> collectAllVariationsSameRoot(int maxBinSize) {
		ArrayList<TDNode> result = new ArrayList<TDNode>();
		ArrayList<HashSet<TDNode>> spectrum = collectVariationUASspectrumSameRoot(maxBinSize);
		for(HashSet<TDNode> bin : spectrum) {
			System.out.println(bin.size());
			result.addAll(bin);
		}
		return result;
	}
	
	public ArrayList<HashSet<TDNode>> collectVariationUASspectrumSameRoot(int maxBinSize) {
		TDNode[] thisStructure = this.getStructureArray();
		int length = thisStructure.length;
		ArrayList<HashSet<TDNode>> result = new ArrayList<HashSet<TDNode>>(length);
		HashSet<TDNode> bestBinScore = new HashSet<TDNode>(1);
		bestBinScore.add(new TDNode(this));
		result.add(bestBinScore);		
		for(int mistakes=1; mistakes<length; mistakes++) {
			ArrayList<TDNodePair> oneStepFlipPairs = new ArrayList<TDNodePair>();
			//int score = length-mistakes;
			for(TDNode oneStepBetter : result.get(mistakes-1)) {
				TDNode[] structure = oneStepBetter.getStructureArray();
				for(TDNode N : structure) {
					if (N.parent==null) continue;
					if (N.parent.index != thisStructure[N.index].parent.index) continue;
					oneStepFlipPairs.addAll(N.oneStepVariationPairs(null));
				}
			}
			int finalSize = oneStepFlipPairs.size();
			if(finalSize>maxBinSize) {
				finalSize = maxBinSize;
				Collections.shuffle(oneStepFlipPairs);
			}
			HashSet<TDNode> binScore = new HashSet<TDNode>(finalSize);
			result.add(binScore);			 			
			int index=0;
			for(TDNodePair NP : oneStepFlipPairs) {
				TDNode original = NP.first.root();
				TDNode oneStepVariation = new TDNode(original);
				TDNode[] newStructure = oneStepVariation.getStructureArray();
				TDNode node = newStructure[NP.first.index];
				TDNode newParent = newStructure[NP.second.index];
				node.newParentFlip(newParent);
				//int UAS = this.UAS(oneStepVariation); 
				//if (UAS==score) {					
					//System.out.println(score + ":\tnewParentFlip\t" + node.lex+"|"+newParent.lex 
					//		+ "\n\t" + original + "\n\t" + oneStepVariation+"\n");
					//oneStepVariation.checkProjectivity();
					if (binScore.add(oneStepVariation)) index++;										
					if (index==finalSize) break;
				//}
				//else {
				//	System.out.println("Wrong UAS: " + UAS);
				//}
			}	
		}
		return result;
	}
	
	public TDNode leftmostDescendent() {
		if (leftDaughters==null) return null;
		TDNode leftmostDescendent = leftDaughters[0];
		while(leftmostDescendent.leftDaughters!=null) {
			leftmostDescendent = leftmostDescendent.leftDaughters[0];
		}
		return leftmostDescendent;
	}

	public TDNode rightmostDescendent() {
		if (rightDaughters==null) return null;
		TDNode rightmostDescendent = rightDaughters[rightProle()-1];
		while(rightmostDescendent.rightDaughters!=null) {
			int rightDescentProle = rightmostDescendent.rightProle();
			rightmostDescendent = rightmostDescendent.rightDaughters[rightDescentProle-1];
		}
		return rightmostDescendent;
	}
	
	public TDNode rightLeftmostDescendent() {
		if (rightDaughters==null) return null;
		return rightDaughters[0].leftmostDescendent();
	}
	
	public TDNode leftRightmostDescendent() {
		if (leftDaughters==null) return null;
		return leftDaughters[leftProle()-1].rightmostDescendent();
	}

	
	public boolean checkProjectivity() {
		TDNode lrmD = leftRightmostDescendent();
		TDNode rlmD = rightLeftmostDescendent();
		if 	((lrmD!=null && lrmD.index>this.index) || (rlmD!=null && rlmD.index<this.index)) {
			System.err.println("nonProjective!");
			return false;
		}
		if (leftDaughters!=null) {
			for(TDNode ld : leftDaughters) {
				if (!ld.checkProjectivity()) return false;	
			}
		}
		if (rightDaughters!=null) {
			for(TDNode rd : rightDaughters) {
				if (!rd.checkProjectivity()) return false; 
			}
		}
		return true;
	}

	public TDNode root() {
		if (parent==null) return this;
		else return parent.root();
	}	


	/**
	 * Performs a leftSister flip of the current node.
	 * The new parent of the current node becomes S 
	 * @param S
	 * Left immediate sister of the current node.
	 */
	private void leftSisterFlip(TDNode S) {
		//System.out.println("Left sister flip at: " + this.index);
		S.rightDaughters = postpendNodes(S.rightDaughters, new TDNode[]{this});
		this.parent.removeFromDaugthters(this);
		this.parent = S;		
	}

	/**
	 * Performs a rightSister flip of the current node.
	 * The new parent of the current node becomes S 
	 * @param S
	 * Right immediate sister of the current node.
	 */
	private void rightSisterFlip(TDNode S) {
		//System.out.println("Right sister flip at: " + this.index);
		S.leftDaughters = appendNodes(S.leftDaughters, new TDNode[]{this});
		this.parent.removeFromDaugthters(this);
		this.parent = S;
	}
	
	private void newParentFlip(TDNode P) {
		parent.removeFromDaugthters(this);
		this.parent = P;
		if (index<P.index) {
			if (P.leftDaughters==null) {
				P.leftDaughters = new TDNode[]{this};
				return;
			}	
			P.leftDaughters = insertNode(P.leftDaughters, this);
		}
		else {
			if (P.rightDaughters==null) {
				P.rightDaughters = new TDNode[]{this};
				return;
			}	
			P.rightDaughters = insertNode(P.rightDaughters, this);
		}			
	}
	
	/**
	 * Performs a parent flip.
	 * B now becomes the head of its former head.
	 * @param B
	 * The new head.
	 * @param P
	 * The old head.
	 */
	private void parentFlip() {
		//System.out.println("Parent flip at: " + this.index);		
		TDNode P = this.parent;
		if (P.index<this.index) {
			TDNode[][] split = split(P.rightDaughters, this);
			if (split[1]!=null) {
				this.rightDaughters = postpendNodes(this.rightDaughters, split[1]);
				for(TDNode d : split[1]) d.parent = this;
			}
			P.rightDaughters = split[0];			
			this.leftDaughters = appendNodes(this.leftDaughters, new TDNode[]{P});
			
		}
		else {
			TDNode[][] split = split(P.leftDaughters, this);
			if (split[0]!=null) {
				this.leftDaughters = appendNodes(this.leftDaughters, split[0]);
				for(TDNode d : split[0]) d.parent = this;
			}
			P.leftDaughters = split[1];
			this.rightDaughters = postpendNodes(this.rightDaughters, new TDNode[]{P});
		}
		this.parent = P.parent;
		P.parent = this;
		if (this.parent!=null) this.parent.replaceInDaughters(P, this);				
	}
	
	/**
	 * Replace A with B in the daughters of the current node
	 * @param A
	 * Old daughter.
	 * @param B
	 * New daughter to replace A.
	 */
	public void replaceInDaughters(TDNode A, TDNode B) {
		TDNode[] daughters = (A.index<this.index) ? this.leftDaughters : this.rightDaughters;  
		int index = indexOf(A, daughters);
		daughters[index] = B;
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
		TDNode[] structure = this.getStructureArray(); 
		for(TDNode tdn : structure) {			
			if (tdn.lex.length() > 1 && tdn.lex.matches("[\\d.,\\\\/]+")) tdn.lex = numberTag;
			else if (tdn.lex.matches("\\d")) tdn.lex = numberTag;
		}
	}
	
	/**
	 * Remove A from the daughters of the current node
	 * @param A
	 */
	public void removeFromDaugthters(TDNode A) {
		if (A.index<this.index) {
			TDNode[] newLeftDaughters = new TDNode[leftProle()-1];
			if (newLeftDaughters.length==0) {
				leftDaughters = null;
				return;
			}
			int i=0;
			for(TDNode d : leftDaughters) {
				if (d==A) continue;
				newLeftDaughters[i] = d;
				i++;
			}
			leftDaughters = newLeftDaughters;
		}
		else {
			TDNode[] newRightDaughters = new TDNode[rightProle()-1];
			if (newRightDaughters.length==0) {
				rightDaughters = null;
				return;
			}
			int i=0;
			for(TDNode d : rightDaughters) {
				if (d==A) continue;
				newRightDaughters[i] = d;
				i++;
			}
			rightDaughters = newRightDaughters;
		}		
	}
	
	public TDNode[] gatherAllDaughters() {
		if (rightDaughters==null && leftDaughters==null) return null;
		if (rightDaughters==null) return leftDaughters;
		if (leftDaughters==null) return rightDaughters;
		TDNode[] result = new TDNode[leftDaughters.length + rightDaughters.length];
		int i=0;
		for(; i<leftDaughters.length; i++) {
			result[i] = leftDaughters[i];
		}
		for(int j=0; j<rightDaughters.length; j++) {
			result[i] = rightDaughters[j];
			i++;
		}
		return result;
	}
	
	/**
	 * Find a node in an array of nodes.
	 * @param toFind
	 * @param daughters
	 * @return
	 */
	public static int indexOf(TDNode toFind, TDNode[] daughters) {
		for(int i=0; i<daughters.length; i++) {
			if (daughters[i]==toFind) return i;
		}
		return -1;
	}
	
	/**
	 * Returns an aray of two arrays of TDNode.
	 * The first contains the daughters to the left of splittingNode (null if none)
	 * The second contains the daughters to the right of splittingNode (null if none)
	 * @param daughters
	 * @param splittingNode
	 * @return
	 */
	public static TDNode[][] split(TDNode[] daughters, TDNode splittingNode) {
		int splittingIndex = indexOf(splittingNode, daughters);
		TDNode[][] result = new TDNode[2][];
		result[0] = (splittingIndex==0) ? null : 
			new TDNode[splittingIndex];
		result[1] = (splittingIndex==daughters.length-1) ? null : 
			new TDNode[daughters.length-splittingIndex-1];
		if (result[0]!=null) {
			for(int i=0; i<result[0].length; i++) result[0][i] = daughters[i];
		}
		if (result[1]!=null) {
			for(int i=0; i<result[1].length; i++) result[1][i] = daughters[splittingIndex+1+i];
		}
		return result;
	}
	
	/**
	 * Returns an array of nodes resulting from appending toAppend in daughters
	 * @param daughters
	 * @param toAppend
	 * @return
	 */
	public static TDNode[] appendNodes(TDNode[] daughters, TDNode[] toAppend) {
		int appendLength = toAppend.length;
		int daughtersLength = (daughters==null) ? 0 : daughters.length;
		TDNode[] result = new TDNode[appendLength + daughtersLength];		
		for(int i=0; i<appendLength; i++) result[i] = toAppend[i];
		for(int i=0; i<daughtersLength; i++) result[appendLength+i] = daughters[i];
		return result;
	}
	
	/**
	 * Returns an array of nodes resulting from postpending toPostpend in daughters
	 * @param daughters
	 * @param toPostpend
	 * @return
	 */
	public static TDNode[] postpendNodes(TDNode[] daughters, TDNode[] toPostpend) {
		int postpendLength = toPostpend.length;
		int daughtersLength = (daughters==null) ? 0 : daughters.length;
		TDNode[] result = new TDNode[postpendLength + daughtersLength];
		for(int i=0; i<daughtersLength; i++) result[i] = daughters[i];
		for(int i=0; i<postpendLength; i++) result[daughtersLength+i] = toPostpend[i];		
		return result;
	}
	
	public boolean isLeaf() {
		return this.leftDaughters==null && this.rightDaughters==null;
	}
	
	public boolean isArgumentDaughter() {
		return this.postag.contains("-A");
	}
	
	public boolean isTerminalNode() {
		return this.postag.contains("-T");
	}
	
	public boolean hasPosTagLeaf(String posTag) {		
		if (this.isLeaf()) return this.postag.equals(posTag);
		if (this.leftDaughters != null) {
			for(TDNode ld : this.leftDaughters) {
				if (ld.hasPosTagLeaf(posTag)) return true;
			}
		}
		if (this.rightDaughters != null) {
			for(TDNode ld : this.rightDaughters) {
				if (ld.hasPosTagLeaf(posTag)) return true;
			}
		}
		return false;
	}
	
	public static TDNode[] insertNode(TDNode[] daughters, TDNode toInsert) {
		int length = daughters.length;
		TDNode[] result = new TDNode[length + 1];
		
		int i=0;
		while(i<daughters.length && toInsert.index > daughters[i].index) {
			result[i] = daughters[i];
			i++;
		}
		result[i] = toInsert;
		i++;
		while(i<length+1) {
			result[i] = daughters[i-1];
			i++;
		}
		return result;
	}
	
	public void posConversion(Hashtable<String, String> posConvTable, Set<String> applicable) {
		if (applicable.contains(this.postag)) {
			this.postag = posConvTable.get(this.postag);
		}
		if (this.leftDaughters != null) {
			for(TDNode ld : this.leftDaughters) {
				ld.posConversion(posConvTable, applicable);
			}
		}
		if (this.rightDaughters != null) {
			for(TDNode rd : this.rightDaughters) {
				rd.posConversion(posConvTable, applicable);
			}
		}
	}
	
	public void renameUnknownWords(Set<String> lexicon, String ukTag) {
		if (!lexicon.contains(this.lex)) this.lex = ukTag;
		if (this.leftDaughters != null) {
			for(TDNode ld : this.leftDaughters) {
				ld.renameUnknownWords(lexicon, ukTag);
			}
		}
		if (this.rightDaughters != null) {
			for(TDNode rd : this.rightDaughters) {
				rd.renameUnknownWords(lexicon, ukTag);
			}
		}
	}
	
	public int prole() {
		int prole = 0;
		if (this.leftDaughters!=null) prole += leftDaughters.length;
		if (this.rightDaughters!=null) prole += rightDaughters.length;
		return prole;
	}
	
	public void updateLowLexiconFreqTable(Hashtable<String, int[]>  lexFreqTable) {
		int sepIndex = lex.indexOf('|');
		String lexLow = (sepIndex==-1) ? lex.toLowerCase() : lex.substring(0, sepIndex).toLowerCase();							
		int[] freq = lexFreqTable.get(lexLow);
		if (freq==null) {
			freq = new int[]{1};
			lexFreqTable.put(lexLow, freq);
		}
		else freq[0]++;
		if (leftDaughters!=null) {
			for(TDNode d : leftDaughters) d.updateLowLexiconFreqTable(lexFreqTable);
		}
		if (rightDaughters!=null) {
			for(TDNode d : rightDaughters) d.updateLowLexiconFreqTable(lexFreqTable);
		}
	}
	
	public void updateLexiconFreqTable(Hashtable<String, Integer>  lexFreqTable) {
		this.updateHeadFreqTable(lexFreqTable, 1);
	}
	
	/**
	 * Recursively increase the frequency of the current node in the table.
	 * The rule to be store is: this.lexPosTag(SLP_type)
	 * i.e. for SLP_type = 0 (postag only) and this.postag = NN
	 * increase of 1 the value of the key NN in the table 
	 * @param freqTable
	 * @param SLP_type
	 */
	public void updateHeadFreqTable(Hashtable<String, Integer>  freqTable, int SLP_type) {
		Utility.increaseInTableInteger(freqTable, this.lexPosTag(SLP_type), 1);
		if (this.leftDaughters != null) {
			for(TDNode ld : this.leftDaughters) {
				ld.updateHeadFreqTable(freqTable, SLP_type);
			}
		}
		if (this.rightDaughters != null) {
			for(TDNode rd : this.rightDaughters) {
				rd.updateHeadFreqTable(freqTable, SLP_type);
			}
		}
	}
	
	/**
	 * Recursively increase the frequency of the current node and L/R children presence/absence in the table.
	 * The rules to be stored are: 
	 * - this.lexPosTag(SLP_type) + "_" + L + 1/0 (presence or absence of left children)
	 * - this.lexPosTag(SLP_type) + "_" + R + 1/0 (presence or absence of right children)
	 * i.e. for SLP_type = 0 (postag only) and this.postag = NN and this has only left children
	 * increase of 1 the value of two keys in the table:
	 * - NN_L_1
	 * - NN_R_0 
	 * @param fragmentSet
	 * @param SLP_type
	 */
	public void updateHeadLRProlePresenceFreqTable(Hashtable<String, Integer>  HeadLRStatTable, 
			int SLP_type) {
		String head = this.lexPosTag(SLP_type);	
		String L = (leftDaughters==null) ? "0" : "1";
		String R = (rightDaughters==null) ? "0" : "1";
		Utility.increaseInTableInteger(HeadLRStatTable, head + "_" + "L" + L, 1);
		Utility.increaseInTableInteger(HeadLRStatTable, head + "_" + "R" + R, 1);
		if (this.leftDaughters != null) {
			for(TDNode ld : this.leftDaughters) {
				ld.updateHeadLRProlePresenceFreqTable(HeadLRStatTable, SLP_type);
			}
		}
		if (this.rightDaughters != null) {
			for(TDNode rd : this.rightDaughters) {
				rd.updateHeadLRProlePresenceFreqTable(HeadLRStatTable, SLP_type);
			}
		}
	}
	
	/**
	 * Recursively increase the frequency of the current node and L/R children absence in the table.
	 * The rules to be stored are: 
	 * - this.lexPosTag(SLP_type) + "_" + L (if the current node has no left children)
	 * - this.lexPosTag(SLP_type) + "_" + R (if the current node has no right children)
	 * i.e. for SLP_type = 0 (postag only) and this.postag = NN and this has only left children
	 * increase of 1 the value of the key in the table:
	 * - NN_L 
	 * @param fragmentSet
	 * @param SLP_type
	 */
	public void updateHeadLRNullFreqTable(Hashtable<String, Integer>  HeadLRStatTable, int SLP_type) {
		String head = this.lexPosTag(SLP_type);
		if (leftDaughters==null) Utility.increaseInTableInteger(
				HeadLRStatTable, head + "_L", 1);
		else {
			for(TDNode ld : this.leftDaughters) {
				ld.updateHeadLRNullFreqTable(HeadLRStatTable, SLP_type);
			}
		}
		if (rightDaughters==null) Utility.increaseInTableInteger(
				HeadLRStatTable, head + "_R", 1);
		else {
			if (this.rightDaughters != null) {
				for(TDNode rd : this.rightDaughters) {
					rd.updateHeadLRNullFreqTable(HeadLRStatTable, SLP_type);
				}
			}
		}		
	}

	/**
	 * Recursively increase the frequency of the current node and L/R children in the table.
	 * The rules to be stored are: 
	 * - this.lexPosTag(SLP_type) + "_" + L + ListOfLeftChildren_ or NULL if absent 
	 * - this.lexPosTag(SLP_type) + "_" + R + ListOfRightChildren_ or NULL if absent
	 * i.e. for SLP_type = 0 (postag only) and this.postag = NN and this has only left children (DT JJ)
	 * increase of 1 the value of two keys in the table:
	 * - NN_L_DT_JJ_
	 * - NN_R_NULL
	 * @param fragmentSet
	 * @param SLP_type
	 */
	public void updateHeadLR_RuleFreqTable(Hashtable<String, Integer>  HeadLRFreqTable,
			int SLP_type) {
		String head = this.lexPosTag(SLP_type);
		if (leftDaughters==null) 
			Utility.increaseInTableInteger(HeadLRFreqTable, head + "_L_NULL", 1);
		else {
			String left = "";
			for(TDNode ld : this.leftDaughters) {
				ld.updateHeadLR_RuleFreqTable(HeadLRFreqTable, SLP_type);
				left += ld.lexPosTag(SLP_type) + "_";
			}
			Utility.increaseInTableInteger(HeadLRFreqTable, head + "_L_" + left, 1);
		}
		if (rightDaughters==null) 
			Utility.increaseInTableInteger(HeadLRFreqTable, head + "_R_NULL", 1);
		else {
			String right = "";
			for(TDNode rd : this.rightDaughters) {
				rd.updateHeadLR_RuleFreqTable(HeadLRFreqTable, SLP_type);
				right += rd.lexPosTag(SLP_type) + "_";
			}
			Utility.increaseInTableInteger(HeadLRFreqTable, head + "_R_" + right, 1);
		}		
	}
	
	/**
	 * Compute the probability of the current node rewriting to the specific set of children,
	 * conditioned on the current node. (Based on the tables obtained with 
	 * updateHeadLRRuleFreqTable and updateHeadFreqTable) 
	 * @param headLRFreqTable
	 * @param headFreqTable
	 * @param SLP_type
	 * @return
	 */
	public float getLR_RuleProb(Hashtable<String, Integer>  headLRFreqTable,
			Hashtable<String, Integer>  headFreqTable, int SLP_type) {
		String head = this.lexPosTag(SLP_type);
		Integer condFreq = headFreqTable.get(head);
		if (condFreq==null) return 0;
		float prob = 1f;
		if (leftDaughters==null) {
			Integer leftRuleFreq = headLRFreqTable.get(head + "_L_NULL");
			if (leftRuleFreq==null) return 0;
			prob *= (float) leftRuleFreq / condFreq;
		}
		else {
			String left = "";
			for(TDNode ld : this.leftDaughters) {
				left += ld.lexPosTag(SLP_type) + "_";
				float ldProb = ld.getLR_RuleProb(headLRFreqTable, headFreqTable, SLP_type);
				if (ldProb==0) return 0;
				prob *= ldProb;				
			}
			Integer leftRuleFreq = headLRFreqTable.get(head + "_L_" + left);
			if (leftRuleFreq==null) return 0;
			prob *= (float) leftRuleFreq / condFreq;
		}
		if (rightDaughters==null) {
			Integer rightRuleFreq = headLRFreqTable.get(head + "_R_NULL");
			if (rightRuleFreq==null) return 0;
			prob *= (float) rightRuleFreq / condFreq;
		}
		else {
			String right = "";
			for(TDNode rd : this.rightDaughters) {
				right += rd.lexPosTag(SLP_type) + "_";
				float rdProb = rd.getLR_RuleProb(headLRFreqTable, headFreqTable, SLP_type);
				if (rdProb==0) return 0;
				prob *= rdProb;				
			}
			Integer rightRuleFreq = headLRFreqTable.get(head + "_R_" + right);
			if (rightRuleFreq==null) return 0;
			prob *= (float) rightRuleFreq / condFreq;
		}
		return prob;
	}
	
	/**
	 * Get the number sub(this) of all possible subtrees (considering 0 or any number of daughters) rooted
	 * in the current node. sub(this) = 2+sub(ld1) * 2+sub(ld2) * ... * 2+sub(rd1) 2+sub(rd2) * ...
	 * Increase this number in the value of the table key correspondent to the current node      
	 * @param freq
	 * @param SLP_type
	 * @return
	 */
	public long updateTableFrequencyAllPossibleSubTrees(
			Hashtable<String, Long> freq, 
			int SLP_type) {
		if (this.leftDaughters==null && this.rightDaughters==null) return 0;
		int prole = prole();
		long[] daughtersStat = new long[prole];
		int i=0;
		if (this.leftDaughters!=null) {
			for(TDNode ld : leftDaughters) {
				daughtersStat[i] = 2+ld.updateTableFrequencyAllPossibleSubTrees(freq, SLP_type);
				i++;
			}			
		}
		if (this.rightDaughters!=null) {
			for(TDNode rd : rightDaughters) {
				daughtersStat[i] = 2+rd.updateTableFrequencyAllPossibleSubTrees(freq, SLP_type);
				i++;
			}			
		}
		long result = Utility.product(daughtersStat) - 1;
		String key = this.lexPosTag(SLP_type);		
		Utility.increaseInTableLong(freq, key, result);
		return result;
	}
	
	/**
	 * Get the number sub(this) of all possible subtrees (considering 0 or any number of ADJACENT daughters) 
	 * rooted in the current node.
	 * Increase this number in the value of the table key correspondent to the current node      
	 * @param freq
	 * @param SLP_type
	 * @return
	 */
	public long updateTableFrequencyAllPossibleAdjacentSubTrees(
			Hashtable<String, Long> freq, 
			int SLP_type) {
		if (this.leftDaughters==null && this.rightDaughters==null) return 0;		
		TDNode[] daughters = this.gatherAllDaughters();
		int prole = daughters.length;
		long[] daughtersStat = new long[prole];
		for(int i=0; i<prole; i++) {
			TDNode d = daughters[i];
			daughtersStat[i] = 1+d.updateTableFrequencyAllPossibleAdjacentSubTrees(freq, SLP_type);
		}
		long result = 0;
		for(int i=0; i<prole; i++) {
			long result_i = daughtersStat[i];
			for(int j=i+1; j<prole; j++) {
				result_i *= daughtersStat[j];
			}
			result += result_i;
		}		
		String key = this.lexPosTag(SLP_type);
		Utility.increaseInTableLong(freq, key, result);
		return result;
	}
	
	public static void main(String[] args) {
		String outputPath = Parameters.resultsPath + "Reranker/Dummy/";
		File outputFile = new File(outputPath + "dummy.txt");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		int LL_tr = 10;
		boolean till11_21 = true;
		int SLP_type = 0;
		String trSec = till11_21 ? "02-11" : "02-21";
		File trainingFile = new File (WsjD.WsjMSTulab + "wsj-" + trSec + ".ulab");
		pw.println(Long.MAX_VALUE);
		ArrayList<TDNode> training = 
			DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		
		Hashtable<String, Long> freq = new Hashtable<String, Long>(); 
		for(TDNode tdn : training) {
			//if (tdn.length()>40) {
			//	pw.println(tdn.toStringMSTulab(false));
				//tdn.updateTableFrequencyAllPossibleSubTrees(freq, SLP_type);
			//	break;
			//}
			System.out.println(tdn.toStringMSTulab(false));
			tdn.addEOS();
			System.out.println(tdn.toStringMSTulab(false));
			tdn.removeEOS();
			System.out.println(tdn.toStringMSTulab(false));
			break;
		}
		pw.println(freq.toString());
		pw.close();
	}

	public static String coordPos = "CC";
	
	public boolean isCoordinationSentence() {
		TDNode[] structure = this.getStructureArray();
		for(TDNode t : structure) {
			if (t.postag.equals(coordPos)) return true;
		}
		return false;
	}

	public boolean isEOS() {
		return this.lex.equals("EOS") && this.postag.equals("EOS");
	}
	
	public boolean hasEOS() {
		return this.root().isEOS();
	}
	
	public String toStringAdWait() {
		if (this.isEOS()) return this.leftDaughters[0].toStringAdWait();
		TDNode[] structure = this.getStructureArray();
		String result = "";
		for(int i=0; i<structure.length; i++) {
			TDNode w = structure[i];
			result += w.lex + "_" + w.postag;
			if (i<structure.length-1) result += " ";
		}
		return result;
	}

	public TDNode[] getArgumentDaughters() {
		Vector<TDNode> argDaughters = new Vector<TDNode>();
		if (leftDaughters!=null) {
			for(TDNode ld : leftDaughters) {
				if (ld.isArgumentDaughter()) argDaughters.add(ld);
			}
		}
		if (rightDaughters!=null) {
			for(TDNode rd : rightDaughters) {
				if (rd.isArgumentDaughter()) argDaughters.add(rd);
			}
		}
		return argDaughters.toArray(new TDNode[argDaughters.size()]);
	}
	
	public TDNode[] getAdjunctDaughters() {
		Vector<TDNode> argDaughters = new Vector<TDNode>();
		if (leftDaughters!=null) {
			for(TDNode ld : leftDaughters) {
				if (!ld.isArgumentDaughter()) argDaughters.add(ld);
			}
		}
		if (rightDaughters!=null) {
			for(TDNode rd : rightDaughters) {
				if (!rd.isArgumentDaughter()) argDaughters.add(rd);
			}
		}
		return argDaughters.toArray(new TDNode[argDaughters.size()]);
	}
	
	public boolean hasArgumentDaughters() {
		if (leftDaughters!=null) {
			for(TDNode ld : leftDaughters) {
				if (ld.isArgumentDaughter()) return true;
			}
		}
		if (rightDaughters!=null) {
			for(TDNode rd : rightDaughters) {
				if (rd.isArgumentDaughter()) return true;
			}
		}
		return false;
	}
	
	public int countArgumentDaughters() {
		int result = 0;
		if (leftDaughters!=null) {
			for(TDNode ld : leftDaughters) {
				if (ld.isArgumentDaughter()) result++;
			}
		}
		if (rightDaughters!=null) {
			for(TDNode rd : rightDaughters) {
				if (rd.isArgumentDaughter()) result++;
			}
		}
		return result;
	}
	
	public int[] countArgumentDaughtersLR() {
		int[] result = new int[2];
		if (leftDaughters!=null) {
			for(TDNode ld : leftDaughters) {
				if (ld.isArgumentDaughter()) result[0]++;;
			}
		}
		if (rightDaughters!=null) {
			for(TDNode rd : rightDaughters) {
				if (rd.isArgumentDaughter()) result[1]++;
			}
		}
		return result;
	}

	public void markTerminalNodesPOS() {
		TDNode[] structure = this.getStructureArray();
		for(TDNode t : structure) {
			if (t.leftDaughters==null && t.rightDaughters==null) {
				t.postag += "-T";
			}
		}
	}

	public boolean markVerbDominationPOS() {
		boolean dominatesVerb = false;
		if (this.leftDaughters!=null) {
			for(TDNode n : this.leftDaughters) {
				if (n.markVerbDominationPOS()) dominatesVerb = true;
			}
		}
		if (this.rightDaughters!=null) {
			for(TDNode n : this.rightDaughters) {
				if (n.markVerbDominationPOS()) dominatesVerb = true;		
			}
		}
		if (dominatesVerb) this.postag += "-V";
		return dominatesVerb || this.postag.startsWith("V");		
	}

	public void markFirstLevelNodesPOS(int current, int limit) {
		if (current<=limit) this.postag += "-" + current;
		else return;
		current++;
		if (this.leftDaughters!=null) {
			for(TDNode n : this.leftDaughters) {
				n.markFirstLevelNodesPOS(current, limit);
			}
		}
		if (this.rightDaughters!=null) {
			for(TDNode n : this.rightDaughters) {
				n.markFirstLevelNodesPOS(current, limit);		
			}
		}		
	}
	
}
