package tdg.kernels;
import java.util.*;

import kernels.NodeSetCollector;

import tdg.TDNode;
import tdg.TDNodePair;

public class DepKernelGraphLR1L extends DepKernelGraph {
	

	public DepKernelGraphLR1L(TDNode currentNode, ArrayList<TDNode> treebank, 
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
			TDNode[][] A_LR = A.daughters();
			TDNode[][] B_LR = B.daughters();			
			for(int LR = 0; LR<2; LR++) {
				TDNode[] A_dir = A_LR[LR];
				TDNode[] B_dir = B_LR[LR];				
				if ( A_dir==null || B_dir==null || (A_dir.length != B_dir.length)) continue;					
				BitSet bs = new BitSet();
				boolean allEquals = true;
				for(int i=0; i<A_dir.length; i++) {
					if (!SP[A_dir[i].index][B_dir[i].index]) {
						allEquals = false;
						break;
					}
					bs.set(A_dir[i].index);
				}
				if (!allEquals) continue;
				bs.set(A.index);
				bsCollector.add(bs);
			}
		}
		return CPG[A.index][B.index] = bsCollector;
	}
		
}
