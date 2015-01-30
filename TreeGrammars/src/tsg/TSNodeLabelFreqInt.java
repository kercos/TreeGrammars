package tsg;

import java.util.Comparator;

import util.StringInteger;

public class TSNodeLabelFreqInt implements Comparable<TSNodeLabelFreqInt>{

	private TSNodeLabel tree;
	private int freq;
	
	public TSNodeLabelFreqInt(TSNodeLabel tree, int freq) {
		this.tree = tree;
		this.freq = freq;
	}
	
	public TSNodeLabel tree() {
		return tree;
	}
	
	public int freq() {
		return freq;
	}
	
	public String toString(boolean printHeads, boolean printLexiconInQuotes) {
		return freq + "\t" + tree.toString(printHeads, printLexiconInQuotes);
	}

	public int compareTo(TSNodeLabelFreqInt anotherTreeFreq) {
		int thisVal = this.freq;
		int anotherVal = anotherTreeFreq.freq;
		if (thisVal<anotherVal) return -1;
		if (thisVal>anotherVal) return 1;
		return this.tree.compareTo(anotherTreeFreq.tree);
		//return this.tree.toString().compareTo(anotherTreeFreq.tree.toString());
	}
		
}
