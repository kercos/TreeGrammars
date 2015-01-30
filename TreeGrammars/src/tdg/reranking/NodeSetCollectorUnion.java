package tdg.reranking;

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
		singleUnion = new BitSet();
	}
	
	public Object clone() {
		NodeSetCollectorUnion clone = new NodeSetCollectorUnion();
		clone.singleUnion = (BitSet)this.singleUnion.clone();
		clone.cardinality = this.cardinality;
		return clone;
	}
	
	public boolean addInMub(BitSet newBs) {
		singleUnion.or(newBs);
		int newCardinality = singleUnion.cardinality();
		if (cardinality != newCardinality) {
			cardinality = newCardinality;
			return true;
		}
		return false;
		
	}
	
	public boolean addAll(NodeSetCollector collector) {
		boolean result = false;
		NodeSetCollectorUnion coll = (NodeSetCollectorUnion)collector;
		return this.addInMub(coll.singleUnion) || result;
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
	
	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, BitSet defaultBitSet) {
		System.err.println("addAllCombinations not implemented");
		return false;
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
		return (singleUnion.isEmpty()) ? 0 : 1;
	}
	
	public int getHeadDependentsScore() {
		return -1;
	}
	
	public void updateDependentHeadScores(int[] depHeadFreq, TDNode[] currentNodeStrucutre) {
		if (singleUnion.cardinality()>1) {
			int currentDepIndex = singleUnion.nextSetBit(0);
			do {					
				TDNode depNode = currentNodeStrucutre[currentDepIndex];
				TDNode headNode = depNode.parent;
				if (headNode!=null) { 
					int headIndex = depNode.parent.index;
					if (singleUnion.get(headIndex)) {
						depHeadFreq[currentDepIndex]++;
					}
				}
				currentDepIndex = singleUnion.nextSetBit(currentDepIndex+1);
			} while(currentDepIndex!=-1);
		}
	}
	
	public float relFreqHeadLRStatProduct(TDNode[] structure,
			Hashtable<String,Integer> headStatTable, 
			int headIndex, int SLP_type) {
		return -1;
	}
	
	public void uniteSubGraphs() {
		// alreadu united
	}
	
	public int maxCardinalitySharedSubGraph() {
		return cardinality;
	}
	
	public BitSet[] getBitSetsAsArray() {
		return new BitSet[]{singleUnion};
	}
	
	public Duet<BitSet,int[]>[] getBitSetsFreqAsArray() {
		Duet<BitSet,int[]>[] result = new Duet[1];
		result[0] = new Duet<BitSet, int[]>(singleUnion, new int[]{1});
		return result;
	}
	
	public String toString() {
		return singleUnion.toString();
	}
	
	public BitSet[][] wordCoverageReport(BitSet minCoverage, int nodeIndex) {
		System.err.println("wordCoverageReport to implement yet");
		return null;
	}
	
	public static void main(String[] args) {

	}

	@Override
	public float[] getProbArgInsertion(NodeSetCollector[] wordsKernels,
			TDNode[] structure, TDNode tree,
			Hashtable<String, Integer> substitutionCondFreqTable,
			Hashtable<String, Integer> insertionFreqTable,
			Hashtable<String, Integer> insertionCondFreqTable,
			int SLP_type) {
		// TODO Auto-generated method stub
		return null;
	}

}
