package tdg.kernels;
import java.util.*;

import kernels.NodeSetCollector;

import tdg.TDNode;
import tdg.TDNodePair;

public class DepKernelGraphNoOrder extends DepKernelGraph {
	
	public DepKernelGraphNoOrder(TDNode currentNode, ArrayList<TDNode> treebank, 
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
			bsCollector.addDefaultBitSet(A.index);
			TDNode[] A_daughters = A.gatherAllDaughters();
			TDNode[] B_daughters = B.gatherAllDaughters();			
			TDNode[][] AB_daughters = DepKernelPaths.simplify(A_daughters, B_daughters, SP);
			A_daughters = AB_daughters[0];
			B_daughters = AB_daughters[1];
			for(int b=1; b<=maxBranching; b++) {
				ArrayList<ArrayList<TDNodePair>> b_airs = 
					DepKernelPaths.n_airs(false, b, A_daughters, B_daughters, SP, adjacency);
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
					bsCollector.addAllCombinations(CDGArray, listLengths, A.index);
				}
			}
		}
		return CPG[A.index][B.index] = bsCollector;
	}
	
}
