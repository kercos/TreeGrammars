package kernels;

import java.util.*;

import tdg.TDNode;
import util.*;

/**
 * Node Set Collector Minimum Upper Bound
 * The intersection between each pair of sets in the collection is empty, or differs
 * from the two sets of the pair.
 * When adding a new element check whether there is any other set that includes it
 * (in this case do no add the new set) or whether there is any other set that
 * is included in it (in this case remove that specific set).
 * @author fsangati
 *
 */
public class NodeSetCollectorMUB extends NodeSetCollector {
	
	public ArrayList<BitSet> bitSetSet;
	
	public NodeSetCollectorMUB() {
		super();
		bitSetSet = new ArrayList<BitSet>();
	}
	
	@SuppressWarnings("unchecked")
	public Object clone() {
		NodeSetCollectorMUB clone = new NodeSetCollectorMUB();
		clone.bitSetSet = (ArrayList<BitSet>)this.bitSetSet.clone();
		return clone;
	}
	
	public void makeEmpty() {
		bitSetSet.clear();
	}
	
	public boolean add(BitSet newBs) {
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
	

	public boolean addDefaultBitSet(BitSet bs) {		
		return this.add(bs);
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
		if (collector.isEmpty()) return false; 
		boolean result = false;
		NodeSetCollectorMUB coll = (NodeSetCollectorMUB)collector;
		for(BitSet bs : coll.bitSetSet) {
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
					((NodeSetCollectorMUB)collectors[i]).bitSetSet.get(comb_i);
				ks.or(choosenBitSet);
			}
			result = this.add(ks) || result;
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
	
	public boolean removeSet(BitSet bs) {
		return this.bitSetSet.remove(bs);
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
	
	public BitSet getUniqueBitSetContainingElement(int element) {
		BitSet result = null;
		for(BitSet bs : bitSetSet) {
			if (bs.get(element)) {
				if (result!=null) return null;
				result = bs;
			}
		}
		return result;
	}

	public BitSet getSetWithMaxUncoveredElements(BitSet currentCover) {
		int maxUncovered = -1;
		BitSet result = null;
		for(BitSet bs : bitSetSet) {
			BitSet uncovered = (BitSet)bs.clone();
			uncovered.andNot(currentCover);
			int card = uncovered.cardinality();
			if (card > maxUncovered) {
				maxUncovered = card;
				result = bs;
			}
		}
		return result;
	}
	


}
