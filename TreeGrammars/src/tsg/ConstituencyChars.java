package tsg;
import java.util.*;

import util.Utility;

public class ConstituencyChars extends Constituency{
	int initialIndex, finalIndex;
	
	public ConstituencyChars(TSNodeLabel node, int initialIndex, int finalIndex, boolean countLabel) {
		super(node, countLabel);
		this.initialIndex = initialIndex;
		this.finalIndex = finalIndex;
	}
	
	public Constituency unlabeledCopy() {
		return new ConstituencyChars(node, initialIndex, finalIndex, false);
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
		IdentityHashMap<TSNodeLabel, int[]> lexicalCharSpans = new IdentityHashMap<TSNodeLabel, int[]>(); 
		List<TSNodeLabel> lexicalNodes = tree.collectLexicalItems();
		int charCount = 0;
		for(int i=0; i<lexicalNodes.size(); i++) {
			TSNodeLabel LN = lexicalNodes.get(i);
			String label = LN.label();
			label = label.replaceAll("\\s+", "");
			int startCharIndex = ++charCount;
			int endCharIndex = charCount = startCharIndex + label.length()-1;			
			lexicalCharSpans.put(LN, new int[]{startCharIndex, endCharIndex});
		}
		List<TSNodeLabel> internalNodes = tree.collectAllNodes();		
		for(TSNodeLabel NT : internalNodes) {
			if (NT.isLexical) continue;
			if (NT.isPreLexical() && !includePos) continue;
			if (Arrays.binarySearch(excludeLabels, NT.label())>=0) continue;
			int[] charSpanStartWord = lexicalCharSpans.get(NT.getLeftmostTerminalNode());
			int[] charSpanEndWord = lexicalCharSpans.get(NT.getRightmostTerminal());										
			result.add(new ConstituencyChars(NT, charSpanStartWord[0], charSpanEndWord[1], labeled));			
		}
		return result;
	}
	
	
	public boolean isCrossing(ConstituencyChars c) {
		return (	this.initialIndex < c.initialIndex && 
					(this.finalIndex>=c.initialIndex && this.finalIndex < c.finalIndex) 
				||
					this.initialIndex>c.initialIndex && 
					(this.initialIndex<=c.finalIndex && this.finalIndex > c.finalIndex )
				);
	}

	public static int updateCrossingBrackets(List<Constituency> testConst, 
			List<Constituency> goldConst,  Hashtable<String, Integer> crossingBracketsCatTable) {
		int result = 0;
		for(Constituency testC : testConst) {
			for(Constituency goldC : goldConst) {
				if (((ConstituencyChars)testC).isCrossing((ConstituencyChars)goldC)) {
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
		if (anObject instanceof ConstituencyChars) {
			ConstituencyChars c = (ConstituencyChars)anObject;
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
	
	public static List<Constituency> toYieldConstituency(List<Constituency> list, TSNodeLabel tree, boolean labeled) {
		List<Constituency> result = new ArrayList<Constituency>();
		List<TSNodeLabel> lexicon = tree.collectLexicalItems();
		for(Constituency C : list) {
			ConstituencyChars IC = (ConstituencyChars)C;
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
	
	public static void main(String[] args) throws Exception {
		TSNodeLabel goldTree = new TSNodeLabel("(S (A (P this)) (B (Q1 is) (Q2 n't)) (A (R easy)))");
		TSNodeLabel testTree = new TSNodeLabel("(S (A (P this)) (B (Q isn't)) (A (R easy))))");
		List<Constituency> goldConsts = ConstituencyChars.collectConsituencies(goldTree, true, new String[]{}, false);
		List<Constituency> testConsts = ConstituencyChars.collectConsituencies(testTree, true, new String[]{}, false);
		System.out.println(goldConsts.toString());
		System.out.println(testConsts.toString());
	}
}
