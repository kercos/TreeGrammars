package tdg.reranking;
import java.util.*;

import tdg.TDNode;
import tdg.TDNodePair;

public class DepKernelGraphArgFrameOrder extends DepKernelGraph {
	

	public DepKernelGraphArgFrameOrder(TDNode currentNode, ArrayList<TDNode> treebank, 
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
			BitSet defaultBs = new BitSet();
			defaultBs.set(A.index);
			TDNode[][] A_LR = A.daughters();
			TDNode[][] B_LR = B.daughters();
			keepArgumentsInDaughters(A_LR, B_LR);
			if (haveEmptyOrDifferentDaughters(A_LR, B_LR, SP)) {
				return CPG[A.index][B.index] = bsCollector;
			}
			TDNode[] A_U = unifyAndRemoveEmptyDaughters(A_LR);
			TDNode[] B_U = unifyAndRemoveEmptyDaughters(B_LR);
			int length = A_U.length;
			NodeSetCollector[] CDGArray = new NodeSetCollector[length];
			int[] listLengths = new int[length];
			for(int i=0; i<length; i++) {
				TDNode Ad = A_U[i];
				TDNode Bd = B_U[i];
				NodeSetCollector CPGFS = getCPG(Ad, Bd, CPG, SP);
				CDGArray[i] = CPGFS;
				listLengths[i] = CPGFS.size();				
			}
			bsCollector.addAllCombinations(CDGArray, listLengths, A.index);
		}
		return CPG[A.index][B.index] = bsCollector;
	}
	
	public static void keepArgumentsInDaughters(TDNode[][] A_LR, TDNode[][] B_LR) {
		A_LR[0] = keepArgumentsInDaughters(A_LR[0]);
		A_LR[1] = keepArgumentsInDaughters(A_LR[1]);
		B_LR[0] = keepArgumentsInDaughters(B_LR[0]);
		B_LR[1] = keepArgumentsInDaughters(B_LR[1]);
	}
	
	public static TDNode[] keepArgumentsInDaughters(TDNode[] daughters) {
		if (daughters==null) return new TDNode[]{};
		ArrayList<TDNode> argDaughters = new ArrayList<TDNode>(); 
		for(TDNode d : daughters) {
			if (d.isArgumentDaughter()) argDaughters.add(d);
		}
		return argDaughters.toArray(new TDNode[argDaughters.size()]);
	}
	
	
	private static boolean haveEmptyOrDifferentDaughters(TDNode[][] A_LR, 
			TDNode[][] B_LR, boolean[][] SP) {
		if (A_LR[0].length==0 && A_LR[1].length==0) return true;
		return 	(haveDifferentDaughters(A_LR[0], B_LR[0], SP) || 
				haveDifferentDaughters(A_LR[1], B_LR[1], SP));
	}
	
	private static boolean haveDifferentDaughters(TDNode[] A, TDNode[] B, boolean[][] SP) {
		if (A.length != B.length) return true;
		for(int i=0; i<A.length; i++) {
			TDNode Ad = A[i];
			TDNode Bd = B[i];
			if (!SP[Ad.index][Bd.index]) return true; 
		}
		return false;
	}
	
	/**
	 * Unify the left and right daughters (LR) in a unique array.
	 * Left Daughter first, Right daughters follow.
	 * If Left (Right) daughters are empty a null pointer stands at the beggining (Left)
	 * or at the end of the list (Right).
	 * @param LR
	 * @return
	 */
	private static TDNode[] unifyAndRemoveEmptyDaughters(TDNode[][] LR) {
		TDNode[] result = new TDNode[LR[0].length+LR[1].length];
		int index = 0;
		for(TDNode Ld : LR[0]) {
			result[index] = Ld;
			index++;
		}
		for(TDNode Rd : LR[1]) {
			result[index] = Rd;
			index++;
		}
		return result;
	}
	
}
