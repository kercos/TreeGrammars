package tdg.kernels;

import java.util.*;

import tdg.TDNode;
import tdg.TDNodePair;
import util.*;

public class DepKernelPathsNoOrder extends DepKernelPaths{
		
	public DepKernelPathsNoOrder(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, float alfa, float kTreshold, boolean adjacency, int SLP_type) {
		super(currentNode, treebank, ignoreIndex, alfa, kTreshold, adjacency, SLP_type);
	}
	
	
	/**
	 * Compute the Common Peek Path between the two input trees
	 * @param A
	 * @param B
	 * @param CDP
	 * @param CPP
	 * @param SLP
	 * @return
	 */
	public float getCPP(TDNode A, TDNode B, float[][] CDP, float[][] CPP, boolean[][] SLP) {		
		float cpp = CPP[A.index][B.index];
		if (cpp != -1) return cpp;
		cpp = getCDP(A, B, CDP, SLP);
		if (SLP[A.index][B.index]) {
			TDNode[] A_daughters = A.gatherAllDaughters();
			TDNode[] B_daughters = B.gatherAllDaughters();
			TDNode[][] AB_daughters = DepKernelPaths.simplify(A_daughters, B_daughters, SLP);
			A_daughters = AB_daughters[0];
			B_daughters = AB_daughters[1];
			ArrayList<ArrayList<TDNodePair>> pairs= 
				n_airs(false, 2, A_daughters, B_daughters, SLP, adjacency);
			for(ArrayList<TDNodePair> tuple : pairs) {
				TDNodePair first = tuple.get(0);
				TDNodePair second = tuple.get(1);
				TDNode firstA = first.first;
				TDNode firstB = first.second;
				TDNode secondA = second.first;
				TDNode secondB = second.second;
				float CDPfirst = getCDP(firstA, firstB, CDP, SLP);
				float CDPsecond = getCDP(secondA, secondB, CDP, SLP);
				cpp += alfa2 + alfa * CDPfirst + alfa * CDPsecond + alfa2 * CDPfirst * CDPsecond;
			}
		}
		CPP[A.index][B.index] = cpp;
		return cpp;
	}
	
	public static void main(String[] args) {
		/*File toyFile = new File("./tmp/twoDTrees.txt");		
		ArrayList<TDNode> treebank = Corpus.readTreebankFromFile(toyFile);
		TDNode TDN0 = treebank.get(0);
		TDNode TDN1 = treebank.get(1);
		System.out.println(DepKernelPathsNoOrder.getK(TDN0, TDN1));*/
		//correlation_UAS_K(50, 10);
		//total_correlation_UAS_K(false);
	}
	
}
