package tdg.reranking;

import java.util.*;

import util.*;
import tdg.*;

public class NodeSetCollectorFreq extends NodeSetCollector{
	
	ArrayList<Duet<BitSet, int[]>> bitSetTable;
	
	public NodeSetCollectorFreq() {
		super();
		bitSetTable = new ArrayList<Duet<BitSet, int[]>>();
	}
	
	public Object clone() {
		NodeSetCollectorFreq clone = new NodeSetCollectorFreq();
		clone.bitSetTable = (ArrayList<Duet<BitSet, int[]>>)this.bitSetTable.clone();
		return clone;
	}
	
	public boolean addInMub(BitSet bs) {
		return add(bs, 1);
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
	
	public void uniteSubGraphs() {
		BitSet unionBS = new BitSet();
		int unionFreq = 0;
		for(Duet<BitSet, int[]> element : bitSetTable) {
			unionBS.or(element.getFirst());
			unionFreq += element.getSecond()[0];
		}
		Duet<BitSet, int[]> unionElement = new Duet<BitSet, int[]>(unionBS, new int[]{unionFreq});
		bitSetTable.clear();
		bitSetTable.add(unionElement);
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
			BitSet ks = defaultBitSet(defaultBitSet);	
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
	
	public boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, BitSet defaultBitSet) {
		System.err.println("addAllCombinations not implemented");
		return false;
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
	
	public int getHeadDependentsScore() {
		//int result = emptyDaughter[0] + emptyDaughter[1];
		int result = 0;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			Duet<BitSet, int[]> element = iter.next();
			int freq = element.getSecond()[0];
			result += freq;
		}
		return result;
	}
	
	public void updateDependentHeadScores(int[] depHeadFreq, TDNode[] currentNodeStrucutre) {
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			Duet<BitSet, int[]> element = iter.next();
			BitSet bs = element.getFirst();
			if (bs.cardinality()==2) {
				int freq = element.getSecond()[0];
				int firstIndex = bs.nextSetBit(0);
				int secondIndex = bs.nextSetBit(firstIndex+1);
				TDNode firstNode = currentNodeStrucutre[firstIndex];
				TDNode secondNode = currentNodeStrucutre[secondIndex];
				int dependentIndex = (firstNode.parent == secondNode) ? firstIndex : secondIndex;
				depHeadFreq[dependentIndex] = freq; 
			}
		}
	}
	
	/**
	 * 
	 * @param structure
	 * @param headFreqTable freq of rules in training LHS -> freq where LHS = head
	 * @param headLRNullStatTable freq of rules in trainng where LHS_L -> freq LHS_R -> freq
	 * where LHS = head and LHS_L counts the rules where the LHS has no left daughters
	 * @param headIndex
	 * @param SLP_type
	 * @return
	 */
	public float relFreqHeadLRStatProduct(TDNode[] structure,
			Hashtable<String,Integer> headFreqTable, 
			int headIndex, int SLP_type) {		
		TDNode head = structure[headIndex];
		//if (head.isEmptyDaughter()) return 1;
		String headLabel = head.lexPosTag(SLP_type);
		Integer conditFreq = headFreqTable.get(headLabel);
		if (conditFreq==null) return 0;
		float result = 1;
		if (bitSetTable.size()!=2) return 0;
		for(Iterator<Duet<BitSet, int[]>> iter = bitSetTable.iterator(); iter.hasNext(); ) {
			Duet<BitSet, int[]> element = iter.next();
			//BitSet bs = element.getFirst();
			//int firstSetBit = bs.nextSetBit(0);
			int freq = element.getSecond()[0];						
			result *= (float)freq / conditFreq;
		}
		return result;
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
	
	public BitSet[][] wordCoverageReport(BitSet minCoverage, int nodeIndex) {
		System.err.println("wordCoverageReport to implement yet");
		return null;
	}
	
	public String toString() {
		String result = "";
		for(Duet<BitSet, int[]> element : bitSetTable) {
			result += element.getFirst() + "\t" + element.getSecond()[0] + "\n";
		}
		return result.trim();
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
