package tdg.reranking;
import settings.*;
import tdg.TDNode;
import tdg.TDNodePair;
import tdg.corpora.*;
import java.util.*;
import java.io.*;
import util.*;

public class DepKernelPathsOrder extends DepKernelPaths{
		
	
	public DepKernelPathsOrder(TDNode currentNode, ArrayList<TDNode> treebank, 
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
			TDNode[][] A_daughters = A.daughters();
			TDNode[][] B_daughters = B.daughters();
			TDNode[][][] AB_daughters = simplify(A_daughters, B_daughters, SLP);
			A_daughters = AB_daughters[0];
			B_daughters = AB_daughters[1];
			ArrayList<ArrayList<TDNodePair>> pairs= n_airs(2, A_daughters, B_daughters, SLP, adjacency);
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

	/**
	 * Return the ntuples of TDNodePairs in common between the array A and B.
	 * A and B are each a 2 arrays of TDNodes (each possibly null).
	 * Each ntuple contains n pairs of nodes in common between the nodes in the arrays A and B.
	 * Each pair (a,b) is such that a.sameLexicalPrelexical(b) and 
	 * (a in A[0] & b in B[0]) || (a in A[1] & b in B[1])
	 * All the pairs in each ntuple are ordered (first the ordered pairs in (A[0],B[0])
	 * then the ordered pais in (A[1],B[1]).
	 * Better if A and B are simplified
	 * @param n
	 * @param A 2 arrays of TDNode: left/right daughters of a certain TDNode
	 * @param B 2 arrays of TDNode: left/right daughters of a certain TDNode
	 * @param SLP 
	 * @return
	 */
	public static ArrayList<ArrayList<TDNodePair>> n_airs(int n, TDNode[][] A, 
			TDNode[][] B, boolean[][] SLP, boolean adjacency) {		
		ArrayList<ArrayList<TDNodePair>> result = new ArrayList<ArrayList<TDNodePair>>();
		if (A[0]==null && A[1]==null) return result;
		result.addAll(n_airs(true, n, A[0], B[0], SLP, adjacency));
		result.addAll(n_airs(true, n, A[1], B[1], SLP, adjacency));
		if (A[0]==null || A[1]==null || B[0]==null || B[1]==null) return result;
		if(adjacency) {			
			int ALlength = A[0].length;
			int ARlength = A[1].length;
			int BLlength = B[0].length;
			int BRlength = B[1].length;
			int lMax = Math.min(ALlength, BLlength);
			int rMax = Math.min(ARlength, BRlength);
			for(int lsize=1; lsize<n; lsize++) {
				if (lsize > lMax) break;
				int rsize = n - lsize;		
				if (rsize > rMax) continue;
				int lsA = ALlength - lsize; //left start in A[0]
				int lsB = BLlength - lsize; //left start in B[0]
				ArrayList<TDNodePair> matchedNair = new ArrayList<TDNodePair>(n);
				boolean matched = true;
				for(int pl=0; pl<lsize; pl++) {
					TDNode a_match = A[0][lsA + pl]; 
					TDNode b_match = B[0][lsB + pl];
					if (!SLP[a_match.index][b_match.index]) {
						matched = false;
						break;
					}
					matchedNair.add(new TDNodePair(a_match, b_match));
				}
				if (!matched) continue;
				for(int pr=0; pr<rsize; pr++) {
					TDNode a_match = A[1][pr]; 
					TDNode b_match = B[1][pr];
					if (!SLP[a_match.index][b_match.index]) {
						matched = false;
						break;
					}
					matchedNair.add(new TDNodePair(a_match, b_match));
				}
				if (matched) result.add(matchedNair);
			}
			return result;
		}
		else {
			Vector<ArrayList<ArrayList<TDNodePair>>> leftSplits = 
				new Vector<ArrayList<ArrayList<TDNodePair>>>(n-1);
			Vector<ArrayList<ArrayList<TDNodePair>>> rightSplits =
				new Vector<ArrayList<ArrayList<TDNodePair>>>(n-1);
			int[][] splits = Utility.split(n);
	
			// the ways of breaking n in two (non zero) part are n-1
			for(int i=1; i<n; i++) {
				leftSplits.add(n_airs(true, i, A[0], B[0], SLP, adjacency));
				rightSplits.add(n_airs(true, i, A[1], B[1], SLP, adjacency));
			}
			
			for(int[] s : splits) {
				for(ArrayList<TDNodePair> leftPart : leftSplits.get(s[0]-1)) {
					for(ArrayList<TDNodePair> rightPart : rightSplits.get(s[1]-1)) {
						if (leftPart.isEmpty() || rightPart.isEmpty()) continue;
						ArrayList<TDNodePair> nair = new ArrayList<TDNodePair>(n);
						nair.addAll(leftPart);
						nair.addAll(rightPart);
						result.add(nair);
					}
				}
			}
			return result;
		}
	}


	public static TDNode[][][] simplify(TDNode[][] A, TDNode[][] B, boolean[][] SLP) {
		TDNode[][][] result = new TDNode[2][2][];
		TDNode[][] simplifyFirst = simplify(A[0],B[0], SLP);
		TDNode[][] simplifySecond = simplify(A[1],B[1], SLP);
		result[0][0] = simplifyFirst[0];
		result[1][0] = simplifyFirst[1];
		result[0][1] = simplifySecond[0];
		result[1][1] = simplifySecond[1];		
		return result;
	}
	


	public static void main(String[] args) {
		File toyFile = new File(Parameters.corpusPath + "ToyGrammar/2Dtrees.txt");
		ArrayList<TDNode> treebank = DepCorpus.readTreebankFromFileMST(toyFile, 100, false, false);
		int index = 0;
		boolean adjacency = false;
		int SLP_type = 0;
		TDNode TDN = treebank.get(index);
		DepKernelPathsOrder DKPO = new DepKernelPathsOrder(TDN, treebank, index, 1, 0, adjacency, SLP_type);
		DKPO.computeKernelPathSimilarity();
	}
	
}
