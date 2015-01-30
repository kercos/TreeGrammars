package tsg;
import java.util.*;

import util.Utility;

public class ConstituencyWords extends Constituency{
	int initialIndex, finalIndex;
	
	public ConstituencyWords(TSNodeLabel node, int initialIndex, int finalIndex, boolean countLabel) {
		super(node, countLabel);
		this.initialIndex = initialIndex;
		this.finalIndex = finalIndex;
	}
	
	public int getInitialIndex() {
		return initialIndex;
	}
	
	public int getFinalIndex() {
		return finalIndex;
	}
	
	public Constituency unlabeledCopy() {
		return new ConstituencyWords(node, initialIndex, finalIndex, false);
	}
	
	/**
	 * Returns a LinkedList of type Constituency containing the constituencies
	 * of the current treeNode excluding the labels present in the input array
	 * excludeLabels which should be sorted beforehands (i.e. Arrays.sort(excludeLabels)).
	 * If labeled is false we still exclude categories excludeLabels from statistics as in EvalB
	 * const_type: 0 (WORDS), 1 (CHARACTERS), 2 (YIELD)
	 */
	public static ArrayList<ConstituencyWords> collectConsituencies(TSNodeLabel tree, boolean labeled, 
			String[] excludeLabels, boolean includePos) {
		ArrayList<ConstituencyWords> result = new ArrayList<ConstituencyWords>();
		IdentityHashMap<TSNodeLabel, Integer> lexIntMapping = new IdentityHashMap<TSNodeLabel, Integer>();
		ArrayList<TSNodeLabel> lexLabels = tree.collectLexicalItems();
		int i=0;
		for(TSNodeLabel l : lexLabels) {
			lexIntMapping.put(l, i++);	
		}
		List<TSNodeLabel> allNodes = tree.collectAllNodes();		
		for(TSNodeLabel NT : allNodes) {
			if (NT.isLexical) continue;
			if (NT.isPreLexical() && !includePos) continue;
			if (Arrays.binarySearch(excludeLabels, NT.label())>=0) continue;
			int start = lexIntMapping.get(NT.getLeftmostTerminalNode());
			int end = lexIntMapping.get(NT.getRightmostTerminal());										
			result.add(new ConstituencyWords(NT, start, end, labeled));			
		}
		return result;
	}

	
	public boolean isCrossing(ConstituencyWords c) {
		return (	this.initialIndex < c.initialIndex && 
					(this.finalIndex>=c.initialIndex && this.finalIndex < c.finalIndex) 
				||
					this.initialIndex>c.initialIndex && 
					(this.initialIndex<=c.finalIndex && this.finalIndex > c.finalIndex )
				);
	}

	public static int updateCrossingBrackets(List<? extends Constituency> testConst, 
			List<? extends Constituency> goldConst,  Hashtable<String, Integer> crossingBracketsCatTable) {
		int result = 0;
		for(Constituency testC : testConst) {
			for(Constituency goldC : goldConst) {
				if (((ConstituencyWords)testC).isCrossing((ConstituencyWords)goldC)) {
					result++;
					if (crossingBracketsCatTable!=null) {
						String key = testC.node.label() + "/" + goldC.node.label();
						Utility.increaseStringInteger(crossingBracketsCatTable, key, 1);
					}					
					break;
				}
			}
		}
		return result;
	}
	
	public boolean equals(Object anObject) {
		if (this == anObject) {
		    return true;
		}
		if (anObject instanceof ConstituencyWords) {
			ConstituencyWords c = (ConstituencyWords)anObject;
		    if ( (!countLabel || c.node.sameLabel(this.node)) && 
		    		c.initialIndex==this.initialIndex 
		    		&& c.finalIndex==this.finalIndex) return true;		    
		}		
		return false;
	}
	
	public int hashCode() {
		if (countLabel) {
			return 31 * ( this.initialIndex + 31 * (this.finalIndex + 31 * this.label().hashCode())) ;
		}
		return 31 * ( this.initialIndex + 31 * this.finalIndex) ;
	}

	
	public static ArrayList<Constituency> toYieldConstituency(ArrayList<Constituency> list, 
			TSNodeLabel tree, boolean labeled) {
		ArrayList<Constituency> result = new ArrayList<Constituency>();
		ArrayList<TSNodeLabel> lexicon = tree.collectLexicalItems();
		for(Constituency C : list) {
			ConstituencyWords IC = (ConstituencyWords)C;
			List<TSNodeLabel> subLex = lexicon.subList(IC.initialIndex, IC.finalIndex+1);
			String yield = "";
			for(TSNodeLabel lex : subLex) yield += lex + " ";
			yield = yield.trim();
			result.add(new ConstituencyYield(IC.node, new Yield(yield), labeled));
		}
		return result;
	}
	
	public String toString() {
		return "(" + this.node.label() + ": " + this.initialIndex + "-" + this.finalIndex + ")";
	}
}
