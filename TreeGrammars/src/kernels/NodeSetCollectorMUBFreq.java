package kernels;

import java.util.*;

import tdg.TDNode;
import util.*;

public class NodeSetCollectorMUBFreq extends NodeSetCollector{
	
	ArrayList<Duet<BitSet, int[]>> bitSetTable;
	
	public NodeSetCollectorMUBFreq() {
		super();
		bitSetTable = new ArrayList<Duet<BitSet, int[]>>();
	}
	
	@SuppressWarnings("unchecked")
	public Object clone() {
		NodeSetCollectorFreq clone = new NodeSetCollectorFreq();
		clone.bitSetTable = (ArrayList<Duet<BitSet, int[]>>)this.bitSetTable.clone();
		return clone;
	}
	
	public void makeEmpty() {
		bitSetTable.clear();
	}
	
	public boolean add(BitSet bs) {
		return add(bs, 1);
	}
	
	public boolean add(BitSet newBs, int toAdd) {
		int newBsCard = newBs.cardinality(); 
		for(ListIterator<Duet<BitSet, int[]>> iter = bitSetTable.listIterator(); iter.hasNext(); ) {
			Duet<BitSet, int[]> element = iter.next();
			BitSet bs = element.getFirst();
			int bsCard = bs.cardinality();
			BitSet intersection = (BitSet)bs.clone();
			intersection.and(newBs);
			int intersectionCard = intersection.cardinality();
			if (intersectionCard == newBsCard) return true; // bs contains newBs
			if (intersectionCard == bsCard) { // newBs contains bs
				iter.remove();
				insertInOrder(newBs, newBsCard, toAdd);
				return true; 
			}
		}
		insertInOrder(newBs, newBsCard, toAdd);
		return true;
	}
	
	private void insertInOrder(BitSet newBs, int newBsCard, int toAdd) {
		Duet<BitSet, int[]> newElement = new Duet<BitSet, int[]>(newBs, new int[]{toAdd});
		ListIterator<Duet<BitSet, int[]>> iter = bitSetTable.listIterator();
		while(iter.hasNext()) {			
			Duet<BitSet, int[]> element = iter.next();
			BitSet bs = element.getFirst();			
			int bsCard = bs.cardinality();
			if (newBsCard >= bsCard) {				
				iter.previous();
				iter.add(newElement);
				while(iter.hasNext()) {
					element = iter.next();
					bs = element.getFirst();	
					bsCard = bs.cardinality();
					BitSet intersection = (BitSet)bs.clone();
					intersection.and(newBs);
					int intersectionCard = intersection.cardinality();
					if (intersectionCard == bsCard) iter.remove();					
				}
				return;
			}
		}
		iter.add(newElement);
	}
	
	public boolean addAll(NodeSetCollector collector) {
		boolean result = false;
		NodeSetCollectorMUBFreq coll = (NodeSetCollectorMUBFreq)collector;
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
					((NodeSetCollectorMUBFreq)collectors[i]).bitSetTable.get(comb_i);
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
	
	public BitSet uniteSubGraphs() {
		BitSet unionBS = new BitSet();
		int unionFreq = 0;
		for(Duet<BitSet, int[]> element : bitSetTable) {
			unionBS.or(element.getFirst());
			unionFreq += element.getSecond()[0];
		}
		Duet<BitSet, int[]> unionElement = new Duet<BitSet, int[]>(unionBS, new int[]{unionFreq});
		return unionBS;
	}
	
	public int maxCardinalitySharedSubGraph() {
		if (this.isEmpty()) return 0;
		Duet<BitSet, int[]> biggest = bitSetTable.get(0);
		return biggest.getFirst().cardinality();
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
