package tsg.LTSG;

import java.util.*;

import tsg.TSNode;

public class LexicalDerivation implements Comparable<LexicalDerivation>{
	
	TSNode anchor;
	int anchorIndex;
	double logDerivationProb; //complete derivation including the lex tree prob
	LexicalDerivation[] subSiteDerivations;
	int[] subSiteDerivationsIndexes;
	
	public LexicalDerivation(TSNode anchor, int anchorIndex, double logDerivationProb, 
			LexicalDerivation[] subSiteDerivations, int[] subSiteDerivationsIndexes) {
		this.anchor = anchor;
		this.anchorIndex = anchorIndex;
		this.logDerivationProb = logDerivationProb;
		this.subSiteDerivations = subSiteDerivations;	
		this.subSiteDerivationsIndexes = subSiteDerivationsIndexes;
	}
	
	/**
	 * Comparator to arrange the elements in the DerivationForest in decreasing order
	 * (from big to small)
	 */
	public int compareTo(LexicalDerivation o) {
		return (this.logDerivationProb>o.logDerivationProb)? -1 : 
			(this.logDerivationProb==o.logDerivationProb)? 0 : 1; 
	}
	
	public String toString() {
		return anchor.label() + " (" + anchorIndex + ") " + logDerivationProb + " | " + 
		Arrays.toString(subSiteDerivationsIndexes);
	}
}
