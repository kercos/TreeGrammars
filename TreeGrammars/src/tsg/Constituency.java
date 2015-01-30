package tsg;

import java.util.ArrayList;
import java.util.List;


public abstract class Constituency {
		
	TSNodeLabel node;
	boolean countLabel;
	
	public Constituency(TSNodeLabel node, boolean countLabel) {
		this.node = node;
		this.countLabel = countLabel;
	}
	
	public abstract Constituency unlabeledCopy();
	
	public TSNodeLabel getNode() {
		return node;
	}
	
	public String label() {
		return node.label();
	}
	
	public abstract boolean equals(Object anObject);
	public abstract int hashCode();
	
	
	/**
	 * Returns a LinkedList of type Constituency containing the constituencies
	 * of the current treeNode excluding the labels present in the input array
	 * excludeLabels which should be sorted beforehands (i.e. Arrays.sort(excludeLabels)).
	 * If labeled is false we still exclude categories excludeLabels from statistics as in EvalB
	 * const_type: 0 (WORDS), 1 (CHARACTERS), 2 (YIELD)
	 */
	public static ArrayList<? extends Constituency> collectConsituencies(TSNodeLabel tree, 
			boolean labeled, String[] excludeLabels, int const_type, boolean includePos) {		
		switch(const_type) {
			case 0:
				return ConstituencyWords.collectConsituencies(tree, labeled, excludeLabels, includePos);
			case 1:
				return ConstituencyChars.collectConsituencies(tree, labeled, excludeLabels, includePos);
			case 2:
				return ConstituencyYield.collectConsituencies(tree, labeled, excludeLabels, includePos);
			default: return null;
		}
	}
	
	public static ArrayList<Constituency> makeUnlabeledList(ArrayList<Constituency> list) {
		ArrayList<Constituency> result = new ArrayList<Constituency>();
		for(Constituency c : list) {
			result.add(c.unlabeledCopy());
		}
		return result;		
	}

		
}
