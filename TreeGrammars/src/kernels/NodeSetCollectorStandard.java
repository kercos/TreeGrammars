package kernels;

import java.util.*;

import tdg.TDNode;
import util.*;

public class NodeSetCollectorStandard extends NodeSetCollector{
	
	HashSet<BitSet> bitSetSet;
	public ArrayList<BitSet> bitSetArray;	
	
	public NodeSetCollectorStandard() {
		super();
		bitSetArray = new ArrayList<BitSet>();
		bitSetSet = new HashSet<BitSet>();
	}
	
	@SuppressWarnings("unchecked")
	public Object clone() {
		NodeSetCollectorStandard clone = new NodeSetCollectorStandard();
		clone.bitSetSet = (HashSet<BitSet>)this.bitSetSet.clone();
		clone.bitSetArray = (ArrayList<BitSet>)this.bitSetArray.clone();
		return clone;
	}
	
	public void makeEmpty() {
		bitSetSet.clear();
		bitSetArray.clear();
	}
	
	public boolean add(BitSet newBs) {
		if (bitSetSet.add(newBs)) {
			bitSetArray.add(newBs);
			return true;
		}
		return false;
	}
	
	public boolean addAll(NodeSetCollector collector) {
		boolean result = false;
		NodeSetCollectorStandard coll = (NodeSetCollectorStandard)collector;
		for(BitSet bs : coll.bitSetArray) {
			result = this.add(bs) || result;
		}
		return result;
	}
	
	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, int defaultBitSet) {
		boolean result = false;
		int[][] combinations = Utility.combinations(collectorsSizes);
		for(int[] comb : combinations) {
			BitSet ks = getDefaultBitSet(defaultBitSet);			
			for(int i=0; i<collectors.length; i++) {
				int comb_i = comb[i];
				BitSet choosenBitSet = 
					((NodeSetCollectorStandard)collectors[i]).bitSetArray.get(comb_i);
				ks.or(choosenBitSet);
			}
			result = this.add(ks) || result;
		}
		return result;
	}
	
	public boolean removeSingleton(int index) {
		if (this.bitSetArray.isEmpty()) return false;
		for(Iterator<BitSet> iter = bitSetArray.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next();
			if (bs.cardinality()==1 && bs.get(index)) {
				iter.remove();
				this.bitSetSet.remove(bs);
				return true;
			}
		}
		return false;
	}
	
	public boolean removeUniqueSingleton() {
		if (this.bitSetArray.isEmpty()) return false;
		for(Iterator<BitSet> iter = bitSetArray.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next();
			if (bs.cardinality()==1) {
				iter.remove();
				this.bitSetSet.remove(bs);
				return true;
			}
		}
		return false;
	}
	
	public int size() {
		return bitSetArray.size();
	}
	
	public BitSet uniteSubGraphs() {
		BitSet unionBS = new BitSet();
		for(BitSet bs : bitSetArray) {
			unionBS.or(bs);
		}
		return unionBS;
	}
	
	public BitSet[] getBitSetsAsArray() {
		int size = this.size();
		if (size==0) return null;
		return this.bitSetArray.toArray(new BitSet[size]);
	}
	
	public int maxCardinality() {
		if (this.isEmpty()) return 0;
		int maxCardinality = -1;
		for(Iterator<BitSet> iter = bitSetArray.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next();
			int bsCard = bs.cardinality(); 
			if (bsCard > maxCardinality) maxCardinality =  bsCard;
		}
		return maxCardinality;
	}
	
	public String toString() {
		String result = "";
		for(BitSet bs : bitSetArray) result += bs.toString() + "\n"; 
		return result.trim();
	}

}
