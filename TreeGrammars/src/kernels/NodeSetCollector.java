package kernels;

import java.util.*;

import util.*;
import tdg.*;

public abstract class NodeSetCollector {

	int maxLenght;
	
	public NodeSetCollector() {	
	}
	
	public void setMaxLength(int maxLength) {
		this.maxLenght = maxLength;
	}
	
	public abstract Object clone();	
	
	public abstract boolean add(BitSet bs);
	
	public boolean addDefaultBitSet(int defaultIndex) {
		return this.add(getDefaultBitSet(defaultIndex));
	}
	
	public abstract boolean addAll(NodeSetCollector collector);
	
	/**
	 * 
	 * @param collectors
	 * @param collectorsSizes
	 * @param defaultBitSet contains the head index plus in case
	 * 1 of the trace for empty daughter (left or right).
	 * @return
	 */
	public abstract boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, int defaultBitSet);	

	
	public abstract int size();
	
	public boolean isEmpty() {
		return this.size()==0;
	}
	
	public abstract String toString();
	
	public abstract BitSet uniteSubGraphs();

	public abstract boolean removeSingleton(int index);
	
	public abstract boolean removeUniqueSingleton();
	
	public abstract BitSet[] getBitSetsAsArray();
	
	public abstract int maxCardinality();
	
	public abstract void makeEmpty();
		
	public BitSet getDefaultBitSet(int setIndex) {
		BitSet bs = new BitSet(maxLenght);
		bs.set(setIndex);
		return bs;
	}
	
	/**
	 * Returns true if A includes B (i.e. all elements in B are also in A).
	 * @param A
	 * @param B
	 * @return
	 */
	public static boolean includes(BitSet A, BitSet B) {
		BitSet intersection = (BitSet)A.clone();
		intersection.and(B);
		if (intersection.cardinality()==B.cardinality()) return true;
		return false;
	}
}
