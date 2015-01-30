package kernels;

import java.util.*;

import util.*;
import tdg.*;

public class NodeSetCollectorFreq extends NodeSetCollector{
	
	ArrayList<Duet<BitSet, int[]>> bitSetTable;
	
	public NodeSetCollectorFreq() {
		super();
		bitSetTable = new ArrayList<Duet<BitSet, int[]>>();
	}
	
	@SuppressWarnings("unchecked")
	public Object clone() {
		NodeSetCollectorFreq clone = new NodeSetCollectorFreq();
		clone.bitSetTable = (ArrayList<Duet<BitSet, int[]>>)this.bitSetTable.clone();
		return clone;
	}
	
	public boolean add(BitSet bs) {
		return add(bs, 1);
	}
	
	public void makeEmpty() {
		bitSetTable.clear();
	}
	
	public boolean add(BitSet bs, int toAdd) {
		for(Duet<BitSet, int[]> element : bitSetTable) {
			if (element.getFirst().equals(bs)) {
				element.getSecond()[0] += toAdd;
				return true;
			}	
		}
		Duet<BitSet, int[]> newElement = new Duet<BitSet, int[]>(bs, new int[]{toAdd});
		bitSetTable.add(newElement);
		return true;
	}
	
	public BitSet uniteSubGraphs() {
		BitSet unionBS = new BitSet();
		for(Duet<BitSet, int[]> element : bitSetTable) {
			unionBS.or(element.getFirst());
		}
		return unionBS;
	}
	
	public boolean addAll(NodeSetCollector collector) {
		boolean result = false;
		NodeSetCollectorFreq coll = (NodeSetCollectorFreq)collector;
		for(Duet<BitSet, int[]> duet : coll.bitSetTable) {
			result = this.add(duet.getFirst(), duet.getSecond()[0]) || result;
		}
		return result;
	}

	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, int defaultBitSet) {
		boolean result = false;
		int[][] combinations = Utility.combinations(collectorsSizes);
		for(int[] comb : combinations) {
			BitSet ks = getDefaultBitSet(defaultBitSet);	
			int min = Integer.MAX_VALUE;
			for(int i=0; i<collectors.length; i++) {
				int comb_i = comb[i];
				Duet<BitSet, int[]> choosenElement = 
					((NodeSetCollectorFreq)collectors[i]).bitSetTable.get(comb_i);
				int elementFreq = choosenElement.getSecond()[0];
				if (elementFreq < min) min = elementFreq;
				ks.or((choosenElement.getFirst()));
			}			
			result = this.add(ks, min) || result;
		}
		return result;
	}
	
	public boolean removeSingleton(int index) {
		if (this.bitSetTable.isEmpty()) return false;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next().getFirst();
			if (bs.cardinality()==1 && bs.get(index)) {
				iter.remove();
				return true;
			}
		}
		return false;
	}
	
	public boolean removeUniqueSingleton() {
		if (this.bitSetTable.isEmpty()) return false;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next().getFirst();
			if (bs.cardinality()==1) {
				iter.remove();
				return true;
			}
		}
		return false;
	}

	
	public int size() {
		return bitSetTable.size();
	}
	
	public int maxCardinalitySharedSubGraph() {
		int maxCardinality = 0;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			Duet<BitSet, int[]> element = iter.next();
			BitSet bs = element.getFirst();
			int bsCard = bs.cardinality(); 
			if (bsCard > maxCardinality) {
				maxCardinality =  bsCard;
			}			
		}
		return maxCardinality;
	}
	
	public int freqMaxCard() {
		int maxCardinality = 0;
		int freqMaxCard = 0;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			Duet<BitSet, int[]> element = iter.next();
			BitSet bs = element.getFirst();
			int bsCard = bs.cardinality(); 
			if (bsCard > maxCardinality) {
				maxCardinality =  bsCard;
				freqMaxCard = element.getSecond()[0];
			}			
		}
		return freqMaxCard;
	}
	
	public BitSet[] getBitSetsAsArray() {
		int size = this.size();
		if (size==0) return null;
		BitSet[] result = new BitSet[size];
		int index = 0;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			result[index] = iter.next().getFirst();
			index++;
		}
		return result;
	}
	
	public Duet<BitSet,int[]>[] getBitSetsFreqAsArray() {
		int size = this.size();
		if (size==0) return null;
		Duet<BitSet,int[]>[] result = new Duet[size];
		int index = 0;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			result[index] = iter.next();
			index++;
		}
		return result;
	}
	
	public int totalFreq() {
		int totalFreq = 0;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			totalFreq = iter.next().getSecond()[0];
		}
		return totalFreq;
	}
	
	public int maxCardinality() {
		int maxCardinality = 0;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			BitSet bs = iter.next().getFirst();
			int bsCard = bs.cardinality(); 
			if (bsCard > maxCardinality) maxCardinality =  bsCard;
		}
		return maxCardinality;
	}
	
	public String toString() {
		String result = "";
		for(Duet<BitSet, int[]> element : bitSetTable) {
			result += element.getFirst() + "\t" + element.getSecond()[0] + "\n";
		}
		return result.trim();
	}

}
