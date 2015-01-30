package tsg;

public class TSNodeLabelFreqDouble implements Comparable<TSNodeLabelFreqDouble>{

	TSNodeLabel tree;
	double freq;
	
	public TSNodeLabelFreqDouble(TSNodeLabel tree, double freq) {
		this.tree = tree;
		this.freq = freq;
	}
	
	public TSNodeLabel tree() {
		return tree;
	}
	
	public double freq() {
		return freq;
	}
	
	public String toString() {
		return toString(false, true);
	}
	
	public String toString(boolean printHeads, boolean printLexiconInQuotes) {
		return freq + "\t" + tree.toString(printHeads, printLexiconInQuotes);
	}

	public int compareTo(TSNodeLabelFreqDouble anotherTreeFreq) {
		double thisVal = this.freq;
		double anotherVal = anotherTreeFreq.freq;
		if (thisVal<anotherVal) return -1;
		if (thisVal>anotherVal) return 1;
		return this.tree.compareTo(anotherTreeFreq.tree);
		//return this.tree.toString().compareTo(anotherTreeFreq.tree.toString());
	}

}
