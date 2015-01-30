package tdg.reranking;

import java.util.*;

import util.*;
import tdg.*;

public abstract class NodeSetCollector {

	
	public NodeSetCollector() {
	}
	
	public abstract Object clone();	
	
	public abstract boolean addInMub(BitSet bs);
	
	public boolean addDefaultBitSet(int defaultIndex) {
		BitSet bs = new BitSet();
		bs.set(defaultIndex);
		return this.addInMub(bs);
	}
	
	public abstract boolean addAll(NodeSetCollector collector);
	
	/**
	 * 
	 * @param collectors
	 * @param collectorsSizes
	 * @param defaultBitSet contains the head index plus in case
	 * 1 of the trace for empty daughter (left or right).
	 * @return
	 */
	public abstract boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, int defaultBitSet);	
	
	public abstract boolean addAllCombinations(NodeSetCollector[] collectors, 
			int[] collectorsSizes, BitSet defaultBitSet);	
	
	public abstract int size();
	
	public boolean isEmpty() {
		return this.size()==0;
	}
	
	public abstract String toString();
	
	public static BitSet defaultBitSet(int setIndex) {
		BitSet bs = new BitSet();
		bs.set(setIndex);
		return bs;
	}
	
	public abstract void uniteSubGraphs();

	public abstract boolean removeSingleton(int index);
	
	public abstract boolean removeUniqueSingleton();
	
	public abstract BitSet[] getBitSetsAsArray();
	
	public abstract Duet<BitSet,int[]>[] getBitSetsFreqAsArray();
	
	public abstract int maxCardinalitySharedSubGraph();
	
	public abstract void updateDependentHeadScores(int[] depHeadFreq, TDNode[] structure);
	
	public abstract int getHeadDependentsScore();
	
	public static int[] dependentHeadScores(NodeSetCollector[] wordsKernels, TDNode[] structure) {
		int[] result = new int[wordsKernels.length];
		for(NodeSetCollector nsc : wordsKernels) {
			nsc.updateDependentHeadScores(result, structure);
		}
		return result;
	}
	
	public static int[] headDependentsScores(NodeSetCollector[] wordsKernels, TDNode[] structure) {
		int[] result = new int[wordsKernels.length];
		int i=0;
		for(NodeSetCollector nsc : wordsKernels) {
			result[i] = nsc.getHeadDependentsScore();
			i++;
		}
		return result;
	}
	
	public abstract float relFreqHeadLRStatProduct(TDNode[] structure, 
	Hashtable<String,Integer> headStatTable,
	int headIndex, int SLP_type);

	public static float[] relFreqProduct(NodeSetCollector[] wordsKernels, TDNode[] structure, 
			Hashtable<String,Integer> headStatTable, int SLP_type) {
		float[] result = new float[wordsKernels.length+1];
		float prodResult = 1;
		for(int i=0; i<wordsKernels.length; i++) {
			NodeSetCollector nsc = wordsKernels[i];
			result[i] = nsc.relFreqHeadLRStatProduct(structure, 
					headStatTable, i, SLP_type);
			prodResult *= result[i];
		}
		result[wordsKernels.length] = prodResult;
		return new float[]{prodResult};
		//return result;
	}
	
	public abstract double getProbArgFrame(NodeSetCollector[] wordsKernels, TDNode[] structure,
			TDNode tree, Hashtable<String, Double> substitutionCondFreqTable,	int SLP_type);
	
	public static int maxCardinalitySharedSubGraph(NodeSetCollector[] wordsKernels) {
		int maxScore = -1;
		for(NodeSetCollector nsc : wordsKernels) {
			int nscMaxCard = nsc.maxCardinalitySharedSubGraph();
			if (nscMaxCard > maxScore) maxScore = nscMaxCard; 
		}
		return maxScore;
	}
	
	public static int[] maxCardinalitiesSharedSubGraphs(NodeSetCollector[] wordsKernels) {
		int[] maxScores = new int[wordsKernels.length];
		int i = 0;
		for(NodeSetCollector nsc : wordsKernels) {
			maxScores[i] = nsc.maxCardinalitySharedSubGraph();
			i++;
		}
		Arrays.sort(maxScores);
		return maxScores;
	}
	
	public static int sumMaxCardinalitiesSharedSubGraphs(NodeSetCollector[] wordsKernels) {
		int sumScore = 0;
		for(NodeSetCollector nsc : wordsKernels) {
			sumScore += nsc.maxCardinalitySharedSubGraph();
		}
		return sumScore;
	}
	
	public static int sizeSum(NodeSetCollector[] wordsKernels) {
		int sum = 0;
		for(NodeSetCollector nsc : wordsKernels) {
			sum += nsc.size();
		}
		return sum;
	}
	
	public static int countNonEmptySetCollectors(NodeSetCollector[] wordsCollectors) {
		int result = 0;
		for(NodeSetCollector nc : wordsCollectors) {
			if (!nc.isEmpty()) result ++;
		}
		return result;
	}
	
	public static NodeSetCollector[] uniteEachWordSubGraphs(NodeSetCollector[] wordsCollectors) {
		NodeSetCollector[] result = new NodeSetCollector[wordsCollectors.length];
		int i=0;
		for(NodeSetCollector nc : wordsCollectors) {
			NodeSetCollector nc_clone = (NodeSetCollector)nc.clone();
			nc_clone.uniteSubGraphs();
			result[i] = nc_clone;
			i++;
		}
		return result;
	}
	
	public static int maxSpanningUnion(NodeSetCollector[] wordsCollectors) {
		wordsCollectors = uniteEachWordSubGraphs(wordsCollectors);
		BitSet union = new BitSet();
		for(NodeSetCollector nc : wordsCollectors) {
			int size = nc.size();
			if (size==0) continue;
			BitSet bs = nc.getBitSetsAsArray()[0];
			union.or(bs);
		}
		return union.cardinality();
	}
	
	public static int maxSpanning(NodeSetCollector[] wordsCollectors) {
		int maxSpanning = 0;
		int nonEmptyColl = countNonEmptySetCollectors(wordsCollectors);
		if (nonEmptyColl==0) return 0;
		BitSet[][] wordsCollectorArray = new BitSet[nonEmptyColl][];		
		int[] sizes = new int[nonEmptyColl];
		int i=0;
		for(NodeSetCollector nc : wordsCollectors) {
			int size = nc.size();
			if (size==0) continue;
			wordsCollectorArray[i] = nc.getBitSetsAsArray();
			sizes[i] = size;
			i++;
		}
		int[][] combinations = Utility.combinations(sizes);
		for(int[] choice : combinations) {
			BitSet bs = new BitSet();
			i=0;
			for(BitSet[] bsi : wordsCollectorArray) {
				bs.or(bsi[choice[i]]);
				i++;
			}
			int spanning = bs.cardinality();
			if (spanning > maxSpanning) maxSpanning = spanning;
			
		}
		return maxSpanning;
	}
	
	public static int maxSpanningFreq(NodeSetCollector[] wordsCollectors) {
		int maxSpanningFreq = 0;
		int sentenceLength = wordsCollectors.length;
		int nonEmptyColl = countNonEmptySetCollectors(wordsCollectors);
		if (nonEmptyColl==0) return 0;
		Duet<BitSet,int[]>[][] wordsCollectorArray = new Duet[nonEmptyColl][];		
		int[] sizes = new int[nonEmptyColl];
		int i=0;
		for(NodeSetCollector nc : wordsCollectors) {
			int size = nc.size();
			if (size==0) continue;
			wordsCollectorArray[i] = nc.getBitSetsFreqAsArray();
			sizes[i] = size;
			i++;
		}
		int[][] combinations = Utility.combinations(sizes);
		for(int[] choice : combinations) {
			BitSet bs = new BitSet();
			int totalFreq = 0;
			i=0;
			for(Duet<BitSet,int[]>[] bsi : wordsCollectorArray) {
				Duet<BitSet,int[]> chosenElement = bsi[choice[i]];				
				BitSet chosenBs = chosenElement.getFirst();
				int freq = chosenElement.getSecond()[0];
				bs.or(chosenBs);
				totalFreq += freq;
				i++;
			}
			int spanning = bs.cardinality();
			if (spanning < sentenceLength) continue;
			if (totalFreq > maxSpanningFreq) maxSpanningFreq = totalFreq;			
		}		
		return maxSpanningFreq;
	}
	
	public static int totalSpanningFreqUnion(NodeSetCollector[] wordsCollectors) {
		int sentenceLength = wordsCollectors.length;
		wordsCollectors = uniteEachWordSubGraphs(wordsCollectors);
		BitSet union = new BitSet();
		int totalFreq = 0;
		for(NodeSetCollector nc : wordsCollectors) {
			int size = nc.size();
			if (size==0) continue;
			Duet<BitSet, int[]> element = nc.getBitSetsFreqAsArray()[0];
			BitSet bs = element.getFirst();			
			union.or(bs);
			totalFreq += element.getSecond()[0];
		}
		int spanning = union.cardinality();
		return (spanning < sentenceLength) ? 0 : totalFreq;
	}
	
	public static int totalSpanningFreq(NodeSetCollector[] wordsCollectors) {
		int totalSpanningFreq = 0;
		int sentenceLength = wordsCollectors.length;
		int nonEmptyColl = countNonEmptySetCollectors(wordsCollectors);
		if (nonEmptyColl==0) return 0;
		Duet<BitSet,int[]>[][] wordsCollectorArray = new Duet[nonEmptyColl][];		
		int[] sizes = new int[nonEmptyColl];
		int i=0;
		for(NodeSetCollector nc : wordsCollectors) {
			int size = nc.size();
			if (size==0) continue;
			wordsCollectorArray[i] = nc.getBitSetsFreqAsArray();
			sizes[i] = size;
			i++;
		}
		int[][] combinations = Utility.combinations(sizes);
		for(int[] choice : combinations) {
			BitSet bs = new BitSet();
			int totalFreq = 0;
			i=0;
			for(Duet<BitSet,int[]>[] bsi : wordsCollectorArray) {
				Duet<BitSet,int[]> chosenElement = bsi[choice[i]];				
				BitSet chosenBs = chosenElement.getFirst();
				int freq = chosenElement.getSecond()[0];
				bs.or(chosenBs);
				totalFreq += freq;
				i++;
			}
			int spanning = bs.cardinality();
			if (spanning < sentenceLength) continue;
			totalSpanningFreq += totalFreq;			
		}		
		return totalSpanningFreq;
	}
	
	public abstract BitSet[][] wordCoverageReport(BitSet minCoverage, int nodeIndex);
	
	public static BitSet getMinimalCoverage(TDNode wordTree) {
		BitSet minCoverage = new BitSet();
		minCoverage.set(wordTree.index);
		if (wordTree.leftDaughters!=null) {
			for(TDNode ld : wordTree.leftDaughters) {
				minCoverage.set(ld.index);
			}
		}
		if (wordTree.rightDaughters!=null) {
			for(TDNode rd : wordTree.rightDaughters) {
				minCoverage.set(rd.index);
			}
		}
		return minCoverage;
	}
	
	public static Duet<NodeSetCollector[],TDNode[]> reduceWordsCollector(NodeSetCollector[] wordsCollectors,
			TDNode[] treeStructure) {
		Vector<NodeSetCollector> newWordsCollector = new Vector<NodeSetCollector>();
		Vector<TDNode> newTreeStructure = new Vector<TDNode>();
		int index = 0;
		for(NodeSetCollector collector : wordsCollectors) {
			TDNode node = treeStructure[index];
			if (node.leftDaughters!=null || node.rightDaughters!=null) {
				newWordsCollector.add(collector);
				newTreeStructure.add(node);
			}
			index++;
		}
		
		return new Duet<NodeSetCollector[],TDNode[]>(
			newWordsCollector.toArray(new NodeSetCollector[newWordsCollector.size()]),
			newTreeStructure.toArray(new TDNode[newTreeStructure.size()]));
	}
	
	public static int minDerivationLength(NodeSetCollector[] wordsCollectors, 
			TDNode treeWithEmpty, TDNode[] treeStructureWithoutEmpty,
			boolean countEmptyDaughters){
		int totalLength = wordsCollectors.length;
		if (!countEmptyDaughters) { 
			Duet<NodeSetCollector[],TDNode[]> collStruc = 
				reduceWordsCollector(wordsCollectors, treeStructureWithoutEmpty);		
			wordsCollectors = collStruc.getFirst();
			treeStructureWithoutEmpty = collStruc.getSecond();
		}
		int length = wordsCollectors.length;
		if (length==0) return -1;
		int totalLengthWithEmpty = treeWithEmpty.length();
		int minLength = -1;
		BitSet[][][] wordCoverageReport = new BitSet[length][][]; 
		int[] sizes = new int[length];
		int i=0;
		for(NodeSetCollector nc : wordsCollectors) {
			BitSet minCoverage = getMinimalCoverage(treeStructureWithoutEmpty[i]);
			wordCoverageReport[i] = nc.wordCoverageReport(minCoverage, treeStructureWithoutEmpty[i].index);
			if (wordCoverageReport[i]==null) return -1;
			int size = wordCoverageReport[i].length;			
			if (size==0) return -1;
			sizes[i] = size;
			i++;
		}
		int[][] combinations = Utility.combinations(sizes);
		for(int[] comb : combinations) {
			BitSet bs = new BitSet();
			int derivationLength = 0;
			ArrayList<BitSet> derivation = new ArrayList<BitSet>();
			ArrayList<Integer> roots = new ArrayList<Integer>(); 
			for(i=0; i<length; i++) {
				BitSet[] chosenWordCoverageReport = wordCoverageReport[i][comb[i]];	
				int nodeIndex = treeStructureWithoutEmpty[i].index;
				for(BitSet bsi : chosenWordCoverageReport) {					
					bs.or(bsi);
					derivation.add(bsi);
					roots.add(nodeIndex);
				}					
			}
			minimizeDerivation(derivation, roots);
			derivationLength = derivation.size();
			if (bs.cardinality()==totalLengthWithEmpty && 
					isCorrectDerivation(derivation, roots, treeWithEmpty.index)) {
				if (minLength==-1 || minLength > derivationLength) {
					minLength = derivationLength;
				}
			}
		}
		if (minLength==-1) return -1;
		return totalLength-minLength;
	}
	
	public static void minimizeDerivation(ArrayList<BitSet> derivation, 
			ArrayList<Integer> roots) {
		ListIterator<BitSet> id = derivation.listIterator();		
		ListIterator<Integer> ir = roots.listIterator();
		while(id.hasNext()) {
			BitSet d = id.next();
			ir.next();
			ListIterator<BitSet> id1 = derivation.listIterator();
			boolean indispensible = true;
			while(id1.hasNext()) {
				BitSet d1 = id1.next();
				if (d1==d) continue;
				if (includes(d1,d)) {
					indispensible = false;
					break;
				}
			}
			if (!indispensible) {
				id.remove();
				ir.remove();
			}
		}
	}
	
	public static boolean isCorrectDerivation(ArrayList<BitSet> derivation, 
			ArrayList<Integer> roots, int topRootIndex) {
		for(ListIterator<Integer> rootIterator = roots.listIterator(); rootIterator.hasNext(); ) {
			int root = rootIterator.next();
			if (root==topRootIndex) continue; //root node doesn't need check for correct attachment
			int rootIndex = rootIterator.previousIndex();
			boolean correctRoot = false;
			for(ListIterator<BitSet> derivationIterator = derivation.listIterator(); 
				derivationIterator.hasNext();) {
				BitSet bs = derivationIterator.next();
				int bsIndex = derivationIterator.previousIndex();
				if (bsIndex==rootIndex) continue;
				if (bs.get(root)) {
					correctRoot = true;
					break;
				}
			}
			if (!correctRoot) return false;
		}
		return true;
	}
	
	/**
	 * Returns true if A includes B (i.e. all elements in B are also in A).
	 * @param A
	 * @param B
	 * @return
	 */
	public static boolean includes(BitSet A, BitSet B) {
		BitSet intersection = (BitSet)A.clone();
		intersection.and(B);
		if (intersection.cardinality()==B.cardinality()) return true;
		return false;
	}
}
