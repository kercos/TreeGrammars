package tsg.parser;
import java.util.*;

/**
 * The class represents a derivation element of a specific
 * derivation forest of a specific non terminal of a specific cell of a cyk parse chart.
 * @author fsangati
 *
 */
public class DerivationElement implements Comparable<DerivationElement>{	
	int[] indexes; //split, leftRoot, leftIndex, rightRoot, rightIndex
	double logProb;
	
	public DerivationElement(int[] indexes, double logProb ) {
		this.indexes = indexes;
		this.logProb = logProb;
	}
	
	public boolean equals(Object o) {
		return Arrays.equals(((DerivationElement)o).indexes, this.indexes);
	}

	/**
	 * Comparator to arrange the elements in the DerivationForest in decreasing order
	 * (from big to small)
	 */
	public int compareTo(DerivationElement o) {
		return (this.logProb>o.logProb)? -1 : (this.logProb==o.logProb)? 0 : 1; 
	 }
	
}
