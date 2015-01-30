package kernels;

import java.util.*;

import tdg.TDNode;
import util.*;

/**
 * Node Set Collector Union
 * It contains a unique bitset resulting as the union of all the bitset being added.
 * @author fsangati
 *
 */
public class NodeSetCollectorUnion extends NodeSetCollector{
	
	BitSet singleUnion;
	int cardinality;
	
	public NodeSetCollectorUnion() {
		super();		
	}
	
	public BitSet singleBS() {
		return singleUnion;
	}
	
	public void makeEmpty() {
		singleUnion.clear();
		cardinality = 0;
	}
	
	public Object clone() {
		NodeSetCollectorUnion clone = new NodeSetCollectorUnion();
		clone.singleUnion = singleUnion==null ? null : (BitSet)singleUnion.clone();
		clone.cardinality = this.cardinality;
		return clone;
	}
	
	public boolean add(BitSet newBs) {
		if (singleUnion==null) {
			singleUnion = (BitSet)newBs.clone();
			return true;
		}
		singleUnion.or(newBs);
		int newCardinality = singleUnion.cardinality();
		if (cardinality != newCardinality) {
			cardinality = newCardinality;
			return true;
		}
		return false;
		
	}
	
	public boolean addAll(NodeSetCollector collector) {
		NodeSetCollectorUnion coll = (NodeSetCollectorUnion)collector;
		return this.add(coll.singleUnion);
	}

	
	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, int defaultBitSet) {
		boolean modified = !singleUnion.get(defaultBitSet);
		singleUnion.set(defaultBitSet);
		for(NodeSetCollector coll : collectors) {
			modified = this.addAll(coll) || modified;
		}
		if (modified) this.cardinality = this.singleUnion.cardinality();
		return modified;
	}
	
	public boolean removeSingleton(int index) {
		if (cardinality==1 && singleUnion.get(index)) {
			singleUnion.clear();
			cardinality=0;
			return true;
		}
		return false;
	}
	
	public boolean removeUniqueSingleton() {
		if (cardinality==1) {
			singleUnion.clear();
			cardinality=0;
			return true;
		}
		return false;
	}

	
	public int size() {
		return (singleUnion==null) ? 0 : 1;
	}
	
	public BitSet[] getBitSetsAsArray() {
		return singleUnion==null ? null : new BitSet[]{singleUnion};
	}
	
	public String toString() {
		return singleUnion.toString();
	}

	@Override
	public int maxCardinality() {
		return cardinality;
	}

	@Override
	public BitSet uniteSubGraphs() {
		return singleUnion==null ? null : (BitSet)singleUnion.clone();
	}

}
