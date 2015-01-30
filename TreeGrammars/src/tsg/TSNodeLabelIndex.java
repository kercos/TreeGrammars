package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;

import util.file.FileUtil;
import kernels.NodeSetCollector;

public class TSNodeLabelIndex extends TSNodeLabel {

	public int index;
	
	public TSNodeLabelIndex(String s) throws Exception {
		this(s, true);
		setIndexRecursive(new int[]{0});
	}
	
	public TSNodeLabelIndex(String s, boolean allTerminalsAreLeaves) throws Exception {		
		this(ParenthesesBlockPenn.getParenthesesBlocks(s), allTerminalsAreLeaves);
		setIndexRecursive(new int[]{0});
	}
	
	public TSNodeLabelIndex(TSNodeLabel t) {		
		this(t, new int[]{0});
	}
	
	public TSNodeLabelIndex(TSNodeLabel t, int[] index) {
		this.label = t.label;
		this.headMarked = t.headMarked;
		this.isLexical = t.isLexical;
		this.index = index[0]++;
		if (t.isTerminal()) return;
		int prole = t.prole();
		this.daughters = new TSNodeLabelIndex[prole];
		for(int i=0; i<prole; i++) {
			TSNodeLabel d = t.daughters[i];
			this.daughters[i] = new TSNodeLabelIndex(d, index);
			this.daughters[i].parent = this;
		}		
	}
	
	public TSNodeLabelIndex clone() {
		TSNodeLabelIndex result = new TSNodeLabelIndex(this);
		return result;
	}
	
	private TSNodeLabelIndex(ParenthesesBlockPenn p, boolean allTerminalsAreLeaves) {
		if (p.isTerminal()) acquireTerminalLabel(p, allTerminalsAreLeaves);
		else {
			acquireNonTerminalLabel(p);
			this.daughters = new TSNodeLabelIndex[p.subBlocks.size()];
			int i=0;
			for(ParenthesesBlockPenn pd : p.subBlocks) {
				this.daughters[i] = new TSNodeLabelIndex(pd, allTerminalsAreLeaves);
				this.daughters[i].parent = this;
				i++;
			}
		}	
	}
	
	private void setIndexRecursive(int[] index) {
		this.index = index[0]++;
		if (this.isTerminal()) return;
		for(TSNodeLabel t : daughters) {
			((TSNodeLabelIndex)t).setIndexRecursive(index);
		}
	}

    
	
	/**
	 * Returns an array with all the nodes in the current tree.
	 * Root is in position 0;
	 * Position is depth-left first.
	 * @return
	 */	
	public TSNodeLabelIndex[] collectAllNodesInArray() {
		TSNodeLabelIndex[] result = new TSNodeLabelIndex[countAllNodes()];
		this.putInArrayRecursive(result);		
		return result;
	}
	
	private void putInArrayRecursive(TSNodeLabelIndex[] array) {
		array[index] = this;
		if (this.isTerminal()) return;
		for(TSNodeLabel TN : this.daughters) {			
			((TSNodeLabelIndex)TN).putInArrayRecursive(array);				
		}
	}	

	public TSNodeLabel getSubTree(BitSet bs) {		
		TSNodeLabel result = this.nonRecursiveCopy();
		if (this.isTerminal()) return result; 
		TSNodeLabelIndex firstDaughter = (TSNodeLabelIndex)this.daughters[0];
		if (bs.get(firstDaughter.index)) {
			result.daughters = new TSNodeLabel[this.daughters.length];
			for(int i=0; i<this.daughters.length; i++) {
				TSNodeLabel d = ((TSNodeLabelIndex)this.daughters[i]).getSubTree(bs);
				d.parent = result;
				result.daughters[i] = d;				
			}
		}
		return result;
	}
	
	public int getLeftmostSubSiteIndex(BitSet bs) {
		if (this.isLexical) return -1;		 
		TSNodeLabelIndex firstDaughter = (TSNodeLabelIndex)this.daughters[0];
		if (bs.get(firstDaughter.index)) {			
			for(int i=0; i<this.daughters.length; i++) {
				TSNodeLabelIndex d = ((TSNodeLabelIndex)this.daughters[i]); 
				int result = d.getLeftmostSubSiteIndex(bs);
				if (result!=-1) return result;
			}
		}
		else return index;
		return -1;
	}
	
	public void getSubSiteIndexes(BitSet bs, BitSet result) {
		if (this.isLexical)
			return;
		TSNodeLabelIndex firstDaughter = (TSNodeLabelIndex)this.daughters[0];
		if (bs.get(firstDaughter.index)) {			
			for(int i=0; i<this.daughters.length; i++) {
				TSNodeLabelIndex d = ((TSNodeLabelIndex)this.daughters[i]); 
				d.getSubSiteIndexes(bs, result);				
			}
		}
		else
			result.set(index);
	}
	
	public boolean get2LeftmostSubSitesIndex(BitSet bs, int[] result, int[] nextIndex) {
		if (this.isLexical) {
			return false;
		}
		TSNodeLabelIndex firstDaughter = (TSNodeLabelIndex)this.daughters[0];
		if (bs.get(firstDaughter.index)) {			
			for(int i=0; i<this.daughters.length; i++) {
				TSNodeLabelIndex d = ((TSNodeLabelIndex)this.daughters[i]); 
				boolean done = d.get2LeftmostSubSitesIndex(bs, result, nextIndex);
				if (done) return true;
			}
		}
		else {
			result[nextIndex[0]++] = index;
			return nextIndex[0]==2;
		}
		return false;
	}
	
	public int getLeftmostLexNodeIndex(BitSet bs) {
		if (this.isLexical) return index;
		if (this.isTerminal()) return -1; 
		TSNodeLabelIndex firstDaughter = (TSNodeLabelIndex)this.daughters[0];
		if (bs.get(firstDaughter.index)) {			
			for(int i=0; i<this.daughters.length; i++) {
				TSNodeLabelIndex d = ((TSNodeLabelIndex)this.daughters[i]); 
				int result = d.getLeftmostLexNodeIndex(bs);
				if (result!=-1) return result;
			}
		}
		return -1;
	}
	
	public void getLexNodeIndexes(BitSet result, BitSet cover) {
		if (this.isLexical) {
			result.set(index);
			return;
		}
		if (this.isTerminal()) 
			return;
		TSNodeLabelIndex firstDaughter = (TSNodeLabelIndex)this.daughters[0];
		if (cover.get(firstDaughter.index)) {
			for(int i=0; i<this.daughters.length; i++) {
				TSNodeLabelIndex d = ((TSNodeLabelIndex)this.daughters[i]); 
				d.getLexNodeIndexes(result, cover);
			}
		}		
	}
	
	public void fillWordIndexes(int[] wordIndexes, int[] nextIndex) {
		if (this.isLexical) {
			wordIndexes[nextIndex[0]++] = this.index;
			return;
		}
		if (this.isTerminal()) 
			return;
		for(int i=0; i<this.daughters.length; i++) {
			TSNodeLabelIndex d = ((TSNodeLabelIndex)this.daughters[i]); 
			d.fillWordIndexes(wordIndexes, nextIndex);
		}		
	}
	
	public TSNodeLabel getSubBranch(BitSet bs) {		
		TSNodeLabel result = this.nonRecursiveCopy();
		if (this.isTerminal()) return result;
		int selectedDaughtersNumber = 0;
		int prole = this.prole();
		ArrayList<TSNodeLabel> selectedDaughter = new ArrayList<TSNodeLabel>(prole); 
		for(int i=0; i<prole; i++) {			
			TSNodeLabelIndex d = (TSNodeLabelIndex)this.daughters[i];
			if (bs.get(d.index)) {
				TSNodeLabel dNew = d.getSubBranch(bs);
				dNew.parent = result;
				selectedDaughter.add(dNew);
				selectedDaughtersNumber++;
			}
		}
		if (selectedDaughtersNumber>0) {
			result.daughters = selectedDaughter.toArray(
					new TSNodeLabel[selectedDaughtersNumber]);	
		}				
		return result;
	}	
	
    public static void main(String[] args) throws Exception {
    	String p = "(S (NP-SBJ (NP (NNP-H Pierre) (NNP Vinken) ) (, ,) (ADJP (NP-H (CD 61) (NNS years) ) (JJ old) ) (, ,) ) (VP (MD will) (VP (VB join) (NP (DT the) (NN board) ) (PP-CLR (IN as) (NP (DT a) (JJ nonexecutive) (NN director) )) (NP-TMP (NNP Nov.) (CD 29) ))) (. .) )";
    	//String p = "(a (b (c (d (e) (f) ) ) ) )";
    	TSNodeLabelIndex t = new TSNodeLabelIndex(p);
    	System.out.println(p);
    	System.out.println(t);
    }

	

	


}
