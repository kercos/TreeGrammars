package tdg.kernels;

import java.util.*;

import util.*;
import tdg.*;

/**
 * This NodeSetCollector assumes that the structure have argument structure informations. 
 * @author fsangati
 *
 */
public class NodeSetCollectorArgFreq extends NodeSetCollector{
	
	ArrayList<Duet<BitSet, int[]>> bitSetTable;
	
	public NodeSetCollectorArgFreq() {
		super();
		bitSetTable = new ArrayList<Duet<BitSet, int[]>>();
	}
	
	public Object clone() {
		NodeSetCollectorArgFreq clone = new NodeSetCollectorArgFreq();
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
		NodeSetCollectorArgFreq coll = (NodeSetCollectorArgFreq)collector;
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
					((NodeSetCollectorArgFreq)collectors[i]).bitSetTable.get(comb_i);
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
	
	public float[] getProbArgInsertion(NodeSetCollector[] wordsKernels, TDNode[] structure,
			TDNode tree, Hashtable<String, Integer> substitutionCondFreqTable,
			Hashtable<String, Integer> insertionFreqTable,
			Hashtable<String, Integer> insertionCondFreqTable,
			int SLP_type) {
		
		//float[] insideProbArray = new float[wordsKernels.length];
		//Arrays.fill(insideProbArray, -1);
		//return new float[]{getInsideProbability(tree, insideProbArray, wordsKernels, structure,
		//		substitutionCondFreqTable, insertionFreqTable, insertionCondFreqTable, SLP_type)};
		
		float[] globalSubProbArray = new float[wordsKernels.length];
		Arrays.fill(globalSubProbArray, -1);
		float globalSubProb = getGlobalSubstProb(tree, globalSubProbArray,
				wordsKernels, structure, substitutionCondFreqTable, SLP_type);
		float globalInsProb = getGlobalInsertionProb(structure,
				insertionFreqTable, insertionCondFreqTable,	SLP_type);
		return new float[]{globalSubProb * globalInsProb};
		
	}
	
	private static float getInsideProbability(TDNode tree, float[] insideProbArray,
			NodeSetCollector[] wordsKernels, TDNode[] structure,
			Hashtable<String, Integer> substitutionCondFreqTable,
			Hashtable<String, Integer> insertionFreqTable,
			Hashtable<String, Integer> insertionCondFreqTable, int SLP_type) {
		
		int thisIndex = tree.index;
		float insideProb = insideProbArray[thisIndex];
		if (insideProb!=-1) return insideProb;
		
		NodeSetCollectorArgFreq thisKernel = (NodeSetCollectorArgFreq)wordsKernels[thisIndex];
		if (thisKernel.isEmpty()) {
			return insideProbArray[thisIndex] = 0;
		}
		
		insideProb = 0;
		for(Duet<BitSet, int[]> element : thisKernel.bitSetTable) {
			BitSet bs = element.getFirst();
			float subFreq = element.getSecond()[0];
			String condFreqKey = removeArgMark(tree.lexPosTag(SLP_type));
			int subCondFreq = substitutionCondFreqTable.get(condFreqKey);
			float derivationProb = subFreq / subCondFreq;
			
			TDNode[][] insSubNodes = getInseritionNodesAndSubNodes(structure, bs);						
			for(TDNode insNode : insSubNodes[0]) {
				float insertionProbNode = getInsertionProbDaughter(insNode, insertionFreqTable, 
						insertionCondFreqTable, SLP_type);
				float insideProbNode = getInsideProbability(insNode,
						insideProbArray, wordsKernels, structure,  substitutionCondFreqTable, 
						insertionFreqTable, insertionCondFreqTable,	SLP_type);
				derivationProb *= insertionProbNode * insideProbNode;
			}
			for(TDNode subNode : insSubNodes[1]) {
				float subProbNode = getInsideProbability(subNode, insideProbArray,
						wordsKernels, structure, substitutionCondFreqTable,
						insertionFreqTable, insertionCondFreqTable,	SLP_type);
				derivationProb *= subProbNode;			
			}
			insideProb += derivationProb;
		}
		
		return insideProbArray[thisIndex] = insideProb;
	}
	
	/*private static float getInsertionProb(TDNode[] structure,
			Hashtable<String, Integer> insertionFreqTable,
			Hashtable<String, Integer> insertionCondFreqTable,	int SLP_type) {

		float insProb = 1;
		for (TDNode p : structure) {
			TDNode[][] LR_daughters = p.daughters();
			for(int LR = 0; LR<2; LR++) {
				if (LR_daughters[LR]!=null) {
					String dir = (LR==0) ? "_L_" : "_R_";
					for(TDNode d : LR_daughters[LR]) {
						String freqKey = removeArgMark(p.lexPosTag(SLP_type)) + dir + 
							removeArgMark(d.lexPosTag(SLP_type));
						String condFreqKey = removeArgMark(p.lexPosTag(SLP_type)) + dir;						
						float freq = insertionFreqTable.get(freqKey);
						float condFreq = insertionCondFreqTable.get(condFreqKey);
						insProb *= freq /condFreq;
					}
				}				
			}
		}
		return insProb;
	}*/
	
	private static float getGlobalSubstProb(TDNode tree, float[] globalSubstProbArray,
			NodeSetCollector[] wordsKernels, TDNode[] structure,
			Hashtable<String, Integer> substitutionCondFreqTable, int SLP_type) {
		
		int thisIndex = tree.index;
		float globalSubProb = globalSubstProbArray[thisIndex];
		if (globalSubProb!=-1) return globalSubProb;
		
		NodeSetCollectorArgFreq thisKernel = (NodeSetCollectorArgFreq)wordsKernels[thisIndex];
		if (thisKernel.isEmpty()) {
			return globalSubstProbArray[thisIndex] = 0;
		}
		
		globalSubProb = 0;
		for(Duet<BitSet, int[]> element : thisKernel.bitSetTable) {
			BitSet bs = element.getFirst();
			float subFreq = element.getSecond()[0];
			String condFreqKey = removeArgMark(tree.lexPosTag(SLP_type));
			int subCondFreq = substitutionCondFreqTable.get(condFreqKey);
			float derivationProb = subFreq / subCondFreq;
			
			TDNode[][] insSubNodes = getInseritionNodesAndSubNodes(structure, bs);						
			for(TDNode insNode : insSubNodes[0]) {				
				float subProbNode = getGlobalSubstProb(insNode, globalSubstProbArray,
						wordsKernels, structure, substitutionCondFreqTable, SLP_type);
				derivationProb *= subProbNode;
			}
			for(TDNode subNode : insSubNodes[1]) {
				float subProbNode = getGlobalSubstProb(subNode, globalSubstProbArray,
						wordsKernels, structure, substitutionCondFreqTable, SLP_type);
				derivationProb *= subProbNode;			
			}
			globalSubProb += derivationProb;
		}
		
		return globalSubstProbArray[thisIndex] = globalSubProb;
	}
	
	private static float getGlobalInsertionProb(TDNode[] structure,
			Hashtable<String, Integer> insertionFreqTable,
			Hashtable<String, Integer> insertionCondFreqTable,	int SLP_type) {
		
		float insertionProb = 1f;
		for(TDNode t : structure) {
			TDNode[][] LR_daughters = t.daughters();
			for(int LR = 0; LR<2; LR++) {
				if (LR_daughters[LR]==null) continue;
				for(TDNode d : LR_daughters[LR]) {
					if (!d.isArgumentDaughter()) {
						insertionProb *= getInsertionProbDaughter(d, 
								insertionFreqTable, insertionCondFreqTable, SLP_type);
					}
				}
			}
		}
		return insertionProb;
	}
	
	private static float getInsertionProbDaughter(TDNode daughter,
			Hashtable<String, Integer> insertionFreqTable,
			Hashtable<String, Integer> insertionCondFreqTable,	int SLP_type) {

		TDNode parent = daughter.parent;
		String dir = (daughter.index < parent.index) ? "_L_" : "_R_";
		String freqKey = removeArgMark(parent.lexPosTag(SLP_type)) + dir + 
							daughter.lexPosTag(SLP_type);
		String condFreqKey = removeArgMark(parent.lexPosTag(SLP_type)) + dir;						
		Integer freq = insertionFreqTable.get(freqKey);
		if (freq==null) return 0;
		float condFreq = insertionCondFreqTable.get(condFreqKey);
		return (float)freq /condFreq;
	}	
	
	private static TDNode[][] getInseritionNodesAndSubNodes(TDNode[] structure, BitSet kernel) {
		
		Vector<TDNode> insNodes = new Vector<TDNode>();
		Vector<TDNode> subNodes = new Vector<TDNode>();
		int length = structure.length;
		
		
		int index = 0;
		while( (index = kernel.nextSetBit(index)) != -1) {			
			if (index < length) {
				TDNode t = structure[index];
				addInsertionNodes(insNodes, t, kernel, length);
				addSubstitutionNodes(subNodes, t, kernel, length);
			}
			index++;
		}		
		TDNode[][] result = new TDNode[2][];
		result[0] = insNodes.toArray(new TDNode[insNodes.size()]);
		result[1] = subNodes.toArray(new TDNode[subNodes.size()]);
		return result;
	}
	
	private static void addInsertionNodes(Vector<TDNode> insNodes, TDNode t, BitSet kernel, int length) {
		if (isLeafOfKernel(t, kernel, length)) return;
		if (t.leftDaughters==null && t.rightDaughters==null) return;
		TDNode[][] LR_daughters = t.daughters();
		for(int LR = 0; LR<2; LR++) {
			if (LR_daughters[LR]==null) continue;
			for(TDNode d : LR_daughters[LR]) {
				if (!d.isArgumentDaughter()) insNodes.add(d);
			}
		}
	}
	
	private static void addSubstitutionNodes(Vector<TDNode> subNodes, TDNode t, BitSet kernel, int length) {
		if (t.leftDaughters==null && t.rightDaughters==null) return;
		TDNode[][] LR_daughters = t.daughters();
		for(int LR = 0; LR<2; LR++) {
			if (LR_daughters[LR]==null) continue;
			for(TDNode d : LR_daughters[LR]) {
				if (isLeafOfKernel(d, kernel, length)) subNodes.add(d);
			}
		}		
	}
	
	private static boolean isLeafOfKernel(TDNode t, BitSet kernel, int length) {
		if (!kernel.get(t.index)) return false;
		TDNode[][] LR_daughters = t.daughters();
		for(int LR = 0; LR<2; LR++) {
			if (LR_daughters[LR]==null) {
				int nullLeftDaughterIndex = 2 * t.index + length + LR;
				boolean nullDaughterInKernel = kernel.get(nullLeftDaughterIndex);
				if (nullDaughterInKernel) return false;
				continue;
			}
			for(TDNode d : LR_daughters[LR]) {
				if (kernel.get(d.index)) return false; 
			}
		}
		return true;
	}
	
	public static String removeArgMark(String s) {
		if (s.endsWith("-A")) return s.substring(0, s.length()-2);
		return s;
	}
	
	public String toString() {
		String result = "";
		for(Duet<BitSet, int[]> element : bitSetTable) {
			result += element.getFirst() + "\t" + element.getSecond()[0] + "\n";
		}
		return result.trim();
	}

	@Override
	public double getProbArgFrame(NodeSetCollector[] wordsKernels,
			TDNode[] structure, TDNode tree,
			Hashtable<String, Double> substitutionCondFreqTable, int SLP_type) {
		// TODO Auto-generated method stub
		return 0;
	}

}
