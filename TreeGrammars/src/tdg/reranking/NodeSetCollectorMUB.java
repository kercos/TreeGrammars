package tdg.reranking;

import java.util.*;

import tdg.TDNode;
import util.*;

/**
 * Node Set Collector Minimum Upper Bound
 * The intersection between each pair of sets in the collection is empty, or differs
 * between the two sets of the pair.
 * When adding a new element check whether there is any other set that includes it
 * (in this case do no add the new set) or whether there is any other set that
 * is included in it (in this case remove that specific set).
 * @author fsangati
 *
 */
public class NodeSetCollectorMUB extends NodeSetCollector{
	
	ArrayList<BitSet> bitSetSet;
	
	public NodeSetCollectorMUB() {
		super();
		bitSetSet = new ArrayList<BitSet>();
	}
	
	public Object clone() {
		NodeSetCollectorMUB clone = new NodeSetCollectorMUB();
		clone.bitSetSet = (ArrayList<BitSet>)this.bitSetSet.clone();
		return clone;
	}
	
	public boolean addInMub(BitSet newBs) {
		int newBsCard = newBs.cardinality(); 
		for(Iterator<BitSet> iter = bitSetSet.iterator(); iter.hasNext();) {
			BitSet bs = iter.next();
			int bsCard = bs.cardinality();
			BitSet intersection = (BitSet)bs.clone();
			intersection.and(newBs);
			int intersectionCard = intersection.cardinality();
			if (intersectionCard == newBsCard) return false; // bs contains newBs
			if (intersectionCard == bsCard) { // newBs contains bs
				iter.remove();
				insertInOrder(newBs, newBsCard);
				return true; 
			}
		}
		insertInOrder(newBs, newBsCard);
		return true;
	}
	
	/**
	 * bs contains max 3 elements and min 1 element (the head node)
	 * the other 2 are the possible traces of empty daughters
	 */
	public boolean addDefaultBitSet(BitSet bs) {		
		return this.addInMub(bs);
	}
	
	private void insertInOrder(BitSet newBs, int newBsCard) {
		ListIterator<BitSet> iter = bitSetSet.listIterator();
		while(iter.hasNext()) {			
			BitSet bs = iter.next();			
			int bsCard = bs.cardinality();
			if (newBsCard >= bsCard) {				
				iter.previous();
				iter.add(newBs);
				while(iter.hasNext()) {
					bs = iter.next();
					bsCard = bs.cardinality();
					BitSet intersection = (BitSet)bs.clone();
					intersection.and(newBs);
					int intersectionCard = intersection.cardinality();
					if (intersectionCard == bsCard) iter.remove();					
				}
				return;
			}
		}
		iter.add(newBs);
	}
	
	public boolean addAll(NodeSetCollector collector) {		
		boolean result = false;
		NodeSetCollectorMUB coll = (NodeSetCollectorMUB)collector;
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
					((NodeSetCollectorMUB)collectors[i]).bitSetSet.get(comb_i);
				ks.or(choosenBitSet);
			}
			result = this.addInMub(ks) || result;
		}
		return result;
	}
	
	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, BitSet defaultBitSet) {
		boolean result = false;
		int[][] combinations = Utility.combinations(collectorsSizes);
		for(int[] comb : combinations) {
			BitSet ks = (BitSet)defaultBitSet.clone();;		
			for(int i=0; i<collectors.length; i++) {
				int comb_i = comb[i];
				BitSet choosenBitSet = 
					((NodeSetCollectorMUB)collectors[i]).bitSetSet.get(comb_i);
				ks.or(choosenBitSet);
			}
			result = this.addInMub(ks) || result;
		}
		return result;
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
		if (this.isEmpty()) return 0;
		BitSet biggest = bitSetSet.get(0);
		return biggest.cardinality();
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
	
	public String toString() {
		String result = "";
		int size = size();
		for(ListIterator<BitSet> i = bitSetSet.listIterator(); i.hasNext(); ) {
			BitSet bs = i.next();
			result += bs.toString();
			if (i.previousIndex() != size-1) result += "-"; 
		}
		return result.trim();
	}
	
	public BitSet getSubGraphsUnion() {
		BitSet unionBS = new BitSet();
		for(BitSet bs : bitSetSet) unionBS.or(bs);
		return unionBS;		
	}
	
	public BitSet[] getTwoSet(int nodeIndex, BitSet minCoverage) {
		Vector<BitSet> result = new Vector<BitSet>(); 
		int i = minCoverage.nextSetBit(0);
		while(i!=-1) {
			if (i!=nodeIndex) {
				BitSet bs = new BitSet(2);
				bs.set(nodeIndex);
				bs.set(i);
				result.add(bs);
			}
			i = minCoverage.nextSetBit(i+1);
		}
		return result.toArray(new BitSet[result.size()]);
	}
	
	public BitSet[][] wordCoverageReport(BitSet minCoverage, int nodeIndex) {		
		Vector<BitSet[]> wordCoverage = new Vector<BitSet[]>();
		if (!includes(getSubGraphsUnion(),minCoverage)) return null;
		BitSet[] twoSet = getTwoSet(nodeIndex, minCoverage);
		BitSet bs = new BitSet();
		if(bitSetSet.size()==0 && twoSet.length==1) {
			bs.or(twoSet[0]);
			if (includes(bs, minCoverage)) return new BitSet[][]{{twoSet[0]}};
			return null;
		}
		for(BitSet bs1 : bitSetSet) {
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

	
	public static void main(String[] args) {
		NodeSetCollectorMUB nsc = new NodeSetCollectorMUB();
		BitSet a = new BitSet(5);
			a.set(0);a.set(2);
		BitSet b = new BitSet(5);
			b.set(1);b.set(2);
		BitSet c = new BitSet(5);
			c.set(0);c.set(1);c.set(2);
		
		nsc.addInMub(a);
		nsc.addInMub(b);
		nsc.addInMub(c);
		
		System.out.println(nsc);
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
