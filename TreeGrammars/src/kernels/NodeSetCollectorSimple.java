package kernels;

import java.util.*;

import util.*;

/**
 * 
 * @author fedya
 *Like standard but without the ArrayList of BitSet
 */
public class NodeSetCollectorSimple extends NodeSetCollector{
	
	public HashSet<BitSet> bitSetSet;
	
	public NodeSetCollectorSimple() {
		super();
		bitSetSet = new HashSet<BitSet>();
	}
	
	@SuppressWarnings("unchecked")
	public Object clone() {
		NodeSetCollectorSimple clone = new NodeSetCollectorSimple();
		clone.bitSetSet = (HashSet<BitSet>)this.bitSetSet.clone();
		return clone;
	}
	
	public void makeEmpty() {
		bitSetSet.clear();
	}
	
	public boolean add(BitSet newBs) {
		return bitSetSet.add(newBs);
	}
	
	public boolean addAll(NodeSetCollector collector) {
		boolean result = false;
		NodeSetCollectorSimple coll = (NodeSetCollectorSimple)collector;
		for(BitSet bs : coll.bitSetSet) {
			result = this.add(bs) || result;
		}
		return result;
	}
	
	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, int defaultBitSet) {
		System.err.println("Not implemented for this node set collector... " +
				"use standard instead");
		return false;
	}
	
	public boolean removeSingleton(int index) {
		for(BitSet bs : bitSetSet) {
			if (bs.cardinality()==1 && bs.get(index)) {
				bitSetSet.remove(bs);
				return true;
			}
		}
		return false;
	}
	
	public boolean removeUniqueSingleton() {
		for(BitSet bs : bitSetSet) {
			if (bs.cardinality()==1) {
				bitSetSet.remove(bs);
				return true;
			}
		}
		return false;
	}
	
	public int size() {
		return bitSetSet.size();
	}
	
	public BitSet uniteSubGraphs() {
		BitSet unionBS = new BitSet();
		for(BitSet bs : bitSetSet) {
			unionBS.or(bs);
		}
		return unionBS;
	}
	
	public BitSet[] getBitSetsAsArray() {
		int size = this.size();
		if (size==0) return null;
		return this.bitSetSet.toArray(new BitSet[size]);
	}
	
	public int maxCardinality() {
		if (this.isEmpty()) return 0;
		int maxCardinality = -1;
		for(BitSet bs : this.bitSetSet) {			
			int bsCard = bs.cardinality(); 
			if (bsCard > maxCardinality) maxCardinality =  bsCard;
		}
		return maxCardinality;
	}
	
	public String toString() {
		String result = "";
		for(BitSet bs : bitSetSet) result += bs.toString() + "\n"; 
		return result.trim();
	}

}
