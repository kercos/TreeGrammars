package tdg.reranking;
import java.util.*;

import tdg.TDNode;
import tdg.TDNodePair;

public class DepKernelGraphOrderNoFreq extends DepKernelGraph {
	

	public DepKernelGraphOrderNoFreq(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, int maxBranching, int collectionType, int SLP_type,
			boolean countEmptyDaughters, boolean adjacency) {		
		super(currentNode, treebank, ignoreIndex, maxBranching, collectionType, SLP_type,
				countEmptyDaughters, adjacency);
	}

	
	/**
	 * Common peak graphs
	 * @param A
	 * @param B
	 * @param CDP
	 * @param CPP
	 * @param SP
	 * @return
	 */
	public NodeSetCollector getCPG(TDNode A, TDNode B, NodeSetCollector[][] CPG, boolean[][] SP) {
		NodeSetCollector stored = CPG[A.index][B.index];
		if (stored != null) return stored;
		NodeSetCollector bsCollector = getNewCollecor();		
		if (SP[A.index][B.index]) {		
			BitSet defaultBs = new BitSet();
			defaultBs.set(A.index);			
			TDNode[][] A_LR = A.daughters();
			TDNode[][] B_LR = B.daughters();
			for(int LR = 0; LR<2; LR++) {
				if(A_LR[LR]==null && B_LR[LR]==null) {
					defaultBs.set(2*A.index + currentLength + LR);
				}
			}			
			TDNode[][][] AB_daughters = DepKernelPathsOrder.simplify(A_LR, B_LR, SP);
			A_LR = AB_daughters[0];
			B_LR = AB_daughters[1];
			for(int b=1; b<=maxBranching; b++) {
				ArrayList<ArrayList<TDNodePair>> b_airs = 
					DepKernelPathsOrder.n_airs(b, A_LR, B_LR, SP, adjacency);
				if (b_airs.isEmpty()) break;
				for(ArrayList<TDNodePair> tuple : b_airs) {
					NodeSetCollector[] CDGArray = new NodeSetCollector[b];
					int[] listLengths = new int[b];
					for(int i=0; i<b; i++) {
						TDNodePair pair = tuple.get(i);
						TDNode first = pair.first;
						TDNode second = pair.second;
						NodeSetCollector CPGFS = getCPG(first, second, CPG, SP);
						CDGArray[i] = CPGFS;
						listLengths[i] = CPGFS.size();
					}
					bsCollector.addAllCombinations(CDGArray, listLengths, defaultBs);					
				}
			}
			if (bsCollector.isEmpty()) bsCollector.addInMub(defaultBs);
		}
		return CPG[A.index][B.index] = bsCollector;
	}
	
}
