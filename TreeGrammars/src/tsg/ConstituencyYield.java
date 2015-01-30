package tsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstituencyYield extends Constituency{
	Yield yield;
	
	public ConstituencyYield(TSNodeLabel node, Yield yield, boolean countLabel) {
		super(node, countLabel);
		this.yield = yield;
	}
	
	public Constituency unlabeledCopy() {
		return new ConstituencyYield(node, yield, false);
	}

	
	/**
	 * Returns a LinkedList of type Constituency containing the constituencies
	 * of the current treeNode excluding the labels present in the input array
	 * excludeLabels which should be sorted beforehands (i.e. Arrays.sort(excludeLabels)).
	 * If labeled is false we still exclude categories excludeLabels from statistics as in EvalB
	 * const_type: 0 (WORDS), 1 (CHARACTERS), 2 (YIELD)
	 */
	public static ArrayList<Constituency> collectConsituencies(TSNodeLabel tree, boolean labeled, 
			String[] excludeLabels, boolean includePos) {
		ArrayList<Constituency> result = new ArrayList<Constituency>();
		List<TSNodeLabel> allNodes = tree.collectAllNodes();		
		for(TSNodeLabel NT : allNodes) {
			if (NT.isLexical) continue;
			if (NT.isPreLexical() && !includePos) continue;
			if (Arrays.binarySearch(excludeLabels, NT.label())>=0) continue;
			result.add(new ConstituencyYield(NT, new Yield(NT.getYield()), labeled));
		}
		return result;
	}
	
	public boolean equals(Object anObject) {
		if (this == anObject) {
		    return true;
		}
		if (anObject instanceof ConstituencyYield) {
			ConstituencyYield c = (ConstituencyYield)anObject;			
		    if ( (!countLabel || c.node.sameLabel(this.node)) 
		    		&& c.yield.equals(this.yield)) return true; 
		}		
		return false;
	}
	
	public int hashCode() {
		return yield.hashCode();
	}
	
	public boolean equalsConst(Constituency anOtherConst, boolean labeled) {
		if (this == anOtherConst) {
		    return true;
		}
		if (anOtherConst instanceof ConstituencyYield) {
			ConstituencyYield c = (ConstituencyYield)anOtherConst;			
		    if ( (!labeled || c.node.sameLabel(this.node)) 
		    		&& c.yield.equals(this.yield)) return true; 
		}		
		return false;
	}
	
	public String toString() {
		return "(" + this.node.label() + ": " + this.yield + ")";
	}
}

class Yield {
	String yield;
	
	public Yield(String yield) {
		this.yield = yield;
	}
	
	public boolean equals(Object anObject) {
		if (this == anObject) {
		    return true;
		}
		if (anObject instanceof Yield) {
			Yield c = (Yield)anObject;
			// modify the following for more specific equivalence between yields
		    if (c.yield.equals(this.yield)) return true;		    
		}		
		return false;
	}
	
	public int hashCode() {
		return yield.hashCode();
	}
	
	
	
	
	
	public String toString() {
		return yield;
	}
}
