package kernels.parallel;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.BitSet;

class AlignementInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	final static DecimalFormat df = new DecimalFormat("#.##");
	
	public static int maxIndexesPerMatch = 100;
	
	//int numberOfMatches;
	BitSet indexes;
	double probTS, probST;
	int totTargetIndexes;

	public AlignementInfo(int index1, int index2) {
		this.indexes = new BitSet();
		addIndexes(index1, index2);
	}

	/*
	public void computeWeight(ArrayList<Integer> countsMatch) {
		double[] sumEntr = Utility.totalSumAndEntropy(countsMatch);
		totMatches = (int)sumEntr[0];
		entropy = sumEntr[1];
	}
	*/

	public void setTotTargetIndexes(int totTargetIndexes) {
		this.totTargetIndexes = totTargetIndexes;			
	}

	public void setProbTS(double probTS) {
		this.probTS = probTS;			
	}
	
	public void setProbST(double probST) {
		this.probST = probST;			
	}

	public void addIndexes(int index1, int index2) {			
		/*
		if (indexes.size() <= maxIndexesPerMatch) {
			this.indexes.add(index1);
			this.indexes.add(index2);
		}
		numberOfMatches += 2;
		*/
		
		indexes.set(index1);
		indexes.set(index2);
	}

	public int getNumberOfMatches() {
		//return numberOfMatches;
		return indexes.cardinality();
	}
	
	public String toString() {
		
		int matches = getNumberOfMatches();
		
		StringBuilder sb = new StringBuilder();
		sb.append("P(T|S):").append(df.format(probTS)).append('\t');
		sb.append("P(S|T):").append(df.format(probST)).append('\t');
		sb.append("Indexes: (" + matches + "/" + totTargetIndexes + ") ").append('{');
		
		int c = 0;
		for (int i = indexes.nextSetBit(0); i >= 0; i = indexes.nextSetBit(i+1)) {
			sb.append(i).append(", ");
			if (c==maxIndexesPerMatch)
				break;
			c++;
		}
		
		
		/*
		for (int i : indexes) {
			sb.append(i).append(", ");
		}
		*/
		
		if (getNumberOfMatches() > maxIndexesPerMatch) {
			sb.append("...");
		} else {
			int l = sb.length();
			sb.delete(l - 2, l);
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return indexes.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		// strong equality: no duplicates
		return this == o;

		/*
		 * if (o instanceof AlignementInfo) { AlignementInfo oa =
		 * (AlignementInfo) o; return
		 * this.numberOfIndexes==oa.numberOfIndexes &&
		 * this.indexes.equals(oa.numberOfIndexes) &&
		 * this.weight==oa.weight; } return false;
		 */
	}

}