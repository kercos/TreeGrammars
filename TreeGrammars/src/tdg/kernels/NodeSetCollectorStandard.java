package tdg.kernels;

import java.util.*;

import tdg.TDNode;
import util.*;

public class NodeSetCollectorStandard extends NodeSetCollector{
	
	ArrayList<BitSet> bitSetSet;
	
	public NodeSetCollectorStandard() {
		super();
		bitSetSet = new ArrayList<BitSet>();
	}
	
	public Object clone() {
		NodeSetCollectorStandard clone = new NodeSetCollectorStandard();
		clone.bitSetSet = (ArrayList<BitSet>)this.bitSetSet.clone();
		return clone;
	}
	
	public boolean addInMub(BitSet newBs) {
		for(BitSet bs : bitSetSet) {
			if (bs.equals(newBs)) return false;
		}
		return bitSetSet.add(newBs);
	}
	
	public boolean addAll(NodeSetCollector collector) {
		boolean result = false;
		NodeSetCollectorStandard coll = (NodeSetCollectorStandard)collector;
		for(BitSet bs : coll.bitSetSet) {
			result = this.addInMub(bs) || result;
		}
		return result;
	}
	
	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, int defaultBitSet) {
		boolean result = false;
		int[][] combinations = Utility.combinations(collectorsSizes);
		for(int[] comb : combinations) {
			BitSet ks = defaultBitSet(defaultBitSet);			
			for(int i=0; i<collectors.length; i++) {
				int comb_i = comb[i];
				BitSet choosenBitSet = 
					((NodeSetCollectorStandard)collectors[i]).bitSetSet.get(comb_i);
				ks.or(choosenBitSet);
			}
			result = this.addInMub(ks) || result;
		}
		return result;
	}
	
	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, BitSet defaultBitSet) {
		System.err.println("addAllCombinations not implemented");
		return false;
	}
	
	public boolean removeSingleton(int index) {
		if (this.bitSetSet.isEmpty()) return false;
		for(Iterator<BitSet> iter = bitSetSet.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next();
			if (bs.cardinality()==1 && bs.get(index)) {
				iter.remove();
				return true;
			}
		}
		return false;
	}
	
	public boolean removeUniqueSingleton() {
		if (this.bitSetSet.isEmpty()) return false;
		for(Iterator<BitSet> iter = bitSetSet.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next();
			if (bs.cardinality()==1) {
				iter.remove();
				return true;
			}
		}
		return false;
	}
	
	public int size() {
		return bitSetSet.size();
	}
	
	public int getHeadDependentsScore() {
		return -1;
	}
	
	public void updateDependentHeadScores(int[] depHeadFreq, TDNode[] currentNodeStrucutre) {
		for(Iterator<BitSet> iter = bitSetSet.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next();
			if (bs.cardinality()>1) {
				int currentDepIndex = bs.nextSetBit(0);
				do {					
					TDNode depNode = currentNodeStrucutre[currentDepIndex];
					TDNode headNode = depNode.parent;
					if (headNode!=null) { 
						int headIndex = depNode.parent.index;
						if (bs.get(headIndex)) {
							depHeadFreq[currentDepIndex]++;
						}
					}
					currentDepIndex = bs.nextSetBit(currentDepIndex+1);
				} while(currentDepIndex!=-1);
			}
		}
	}
	
	public float relFreqHeadLRStatProduct(TDNode[] structure,
			Hashtable<String,Integer> headStatTable, 
			int headIndex, int SLP_type) {
		return -1;
	}
	
	public void uniteSubGraphs() {
		BitSet unionBS = new BitSet();
		for(BitSet bs : bitSetSet) {
			unionBS.or(bs);
		}
		bitSetSet.clear();
		bitSetSet.add(unionBS);
	}
	
	public int maxCardinalitySharedSubGraph() {		
		int maxCard = 0;
		for(BitSet bs : bitSetSet) {
			int bsCard = bs.cardinality();
			if (bsCard > maxCard) {
				maxCard = bsCard;
			}
		}		
		return maxCard;
	}
	
	public BitSet[] getBitSetsAsArray() {
		int size = this.size();
		if (size==0) return null;
		return this.bitSetSet.toArray(new BitSet[size]);
	}
	
	public Duet<BitSet,int[]>[] getBitSetsFreqAsArray() {
		int size = this.size();
		if (size==0) return null;
		Duet<BitSet,int[]>[] result = new Duet[size];
		int index = 0;
		for(Iterator<BitSet> iter = bitSetSet.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next();
			result[index] = new Duet<BitSet, int[]>(bs, new int[]{1});
			index++;
		}
		return result;
	}
	
	public int maxCardinality() {
		if (this.isEmpty()) return 0;
		int maxCardinality = -1;
		for(Iterator<BitSet> iter = bitSetSet.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next();
			int bsCard = bs.cardinality(); 
			if (bsCard > maxCardinality) maxCardinality =  bsCard;
		}
		return maxCardinality;
	}
	
	public BitSet[][] splitTwoMultiSet() {
		Vector<BitSet> twoSet = new Vector<BitSet>();
		ArrayList<BitSet> multiSet = new ArrayList<BitSet>();
		for(BitSet bs : bitSetSet) {
			if (bs.cardinality()==2) twoSet.add(bs);
			else multiSet.add(bs);
		}
		ListIterator<BitSet> id = multiSet.listIterator();		
		while(id.hasNext()) {
			BitSet d = id.next();
			ListIterator<BitSet> id1 = multiSet.listIterator();
			boolean indispensible = true;
			while(id1.hasNext()) {
				BitSet d1 = id1.next();
				if (d1==d) continue;
				if (includes(d1,d)) {
					indispensible = false;
					break;
				}
			}
			if (!indispensible) id.remove();
		}
		BitSet[][] result = new BitSet[2][];
		result[0] = twoSet.toArray(new BitSet[twoSet.size()]); 
		result[1] = multiSet.toArray(new BitSet[multiSet.size()]);
		return result;
	}
	
	public BitSet[][] wordCoverageReport(BitSet minCoverage, int nodeIndex) {		
		Vector<BitSet[]> wordCoverage = new Vector<BitSet[]>();		
		BitSet[][] twoMultiSet = splitTwoMultiSet();
		BitSet[] twoSet = twoMultiSet[0];
		BitSet[] multiSet = twoMultiSet[1];
		BitSet bs = new BitSet();
		if(multiSet.length==0) {
			Vector<BitSet> coverage = new Vector<BitSet>();
			for(BitSet bs1 : twoSet) {
				bs.or(bs1);
				coverage.add(bs1);
			}					
			if (includes(bs, minCoverage)) {
				return new BitSet[][]{coverage.toArray(new BitSet[wordCoverage.size()])};
			}
			return null;
		}
		for(BitSet bs1 : multiSet) {
			Vector<BitSet> coverage = new Vector<BitSet>();
			coverage.add(bs1);
			bs.or(bs1);
			for(BitSet bs2: twoSet) {
				if (includes(bs1,bs2)) continue;
				coverage.add(bs2);
				bs.or(bs2);
			}
			int coverageSize = coverage.size();
			if (includes(bs, minCoverage)) {
				wordCoverage.add(coverage.toArray(new BitSet[coverageSize]));
			}			
		}
		int totalSize = wordCoverage.size();
		if (totalSize==0) return null;
		return wordCoverage.toArray(new BitSet[totalSize][]);
	}

	
	public String toString() {
		String result = "";
		for(BitSet bs : bitSetSet) result += bs.toString() + "\n"; 
		return result.trim();
	}

	public float[] getProbArgInsertion(NodeSetCollector[] wordsKernels,
			TDNode[] structure, TDNode tree,
			Hashtable<String, Integer> substitutionCondFreqTable,
			Hashtable<String, Integer> insertionFreqTable,
			Hashtable<String, Integer> insertionCondFreqTable,
			int SLP_type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getProbArgFrame(NodeSetCollector[] wordsKernels,
			TDNode[] structure, TDNode tree,
			Hashtable<String, Double> substitutionCondFreqTable, int SLP_type) {
		// TODO Auto-generated method stub
		return 0;
	}

}
