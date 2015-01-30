package tdg.kernels;
import java.util.*;

import tdg.TDNode;
import tdg.TDNodePair;
import util.*;

public class DepKernelPathsMixOrder extends DepKernelPathsMix{
	
	public DepKernelPathsMixOrder(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, float alfa, float kTreshold, float SLP_factor, float SP_factor,
			int maxBranching) {
		super(currentNode, treebank, ignoreIndex, alfa, kTreshold, SLP_factor, SP_factor,
				maxBranching);
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
	public float getCPP(TDNode A, TDNode B, float[][] CDP, float[][] CPP, float[][] SLPmix) {
		float cpp = CPP[A.index][B.index];
		if (cpp != -1) return cpp;
		cpp = getCDP(A, B, CDP, SLPmix);
		if (SLPmix[A.index][B.index]>0) {
			for(int b=1; b<=maxBranching; b++) {
				ArrayList<ArrayList<TDNodePair>> bairs= n_airs(b, A.daughters(), 
						B.daughters(), SLPmix);
				if (bairs.isEmpty()) break;				
				for(ArrayList<TDNodePair> tuple : bairs) {
					float sum = 0;
					float product = 1;
					for(TDNodePair irst : tuple) {
						TDNode irstA = irst.first;
						TDNode irstB = irst.second;
						float CDPirst = SLPmix[irstA.index][irstB.index] * getCDP(irstA, 
								irstB, CDP, SLPmix);
						sum += CDPirst;
						product *= CDPirst;
					}
					cpp +=  SLPmix[A.index][B.index] * alfa2 + 
							alfa * sum + alfa2 * product;
				}
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
	 * 
	 * @param n
	 * @param A 2 arrays of TDNode: left/right daughters of a certain TDNode
	 * @param B 2 arrays of TDNode: left/right daughters of a certain TDNode
	 * @param SLP 
	 * @return
	 */
	public static ArrayList<ArrayList<TDNodePair>> n_airs(int n, TDNode[][] A, TDNode[][] B, float[][] SLPmix) {
		TDNode[][][] AB = simplify(A,B, SLPmix);
		A = AB[0];
		B = AB[1];
		ArrayList<ArrayList<TDNodePair>> result = new ArrayList<ArrayList<TDNodePair>>();
		if (A[0]==null && A[1]==null) return result;
		result.addAll(n_airs(n, A[0], B[0], SLPmix));
		result.addAll(n_airs(n, A[1], B[1], SLPmix));		
		ArrayList<ArrayList<ArrayList<TDNodePair>>> leftSplits, rightSplits;
		// the ways of breaking n in two (non zero) part are n-1
		leftSplits = new ArrayList<ArrayList<ArrayList<TDNodePair>>>(n-1);
		rightSplits = new ArrayList<ArrayList<ArrayList<TDNodePair>>>(n-1);
		for(int i=1; i<n; i++) {
			leftSplits.add(n_airs(i, A[0], B[0], SLPmix));
			rightSplits.add(n_airs(i, A[1], B[1], SLPmix));
		}
		int[][] splits = Utility.split(n);
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

	/**
	 * Returns the ntuples of TDNodePairs in common between the array A and B.
	 * A and B are each a 1 array of TDNodes (possibly null).
	 * Each ntuple contains pairs (a,b) where a in A and b in B and a.sameLexicalPrelexical(b)
	 * Each ntuple ...,(a,b),(a',b'),(a'',b''),...
	 * is orderd in the sense that 
	 * A.index[a] < A.index[a'] < A.index[a''] and
	 * B.index[b] < B.index[b'] < B.index[b''].
	 * @param n
	 * @param A
	 * @param B
	 * @param SLP
	 * @return
	 */
	public static ArrayList<ArrayList<TDNodePair>> n_airs(int n, TDNode[] A, TDNode[] B, float[][] SLPmix) {
		ArrayList<ArrayList<TDNodePair>> result = new ArrayList<ArrayList<TDNodePair>>();
		if(A==null || A.length<n || B.length<n) return result;		
		int[][] Anairs = NAirs.get(A.length, n);
		int[][] Bnairs = NAirs.get(B.length, n);		
		for(int[] a : Anairs) {
			for(int[] b : Bnairs) {				
				ArrayList<TDNodePair> matchedNair = new ArrayList<TDNodePair>(n);
				boolean matched = true;
				for(int i=0; i<n; i++) {
					TDNode a_match = A[a[i]]; 
					TDNode b_match = B[b[i]];
					if (SLPmix[a_match.index][b_match.index]==0) {
						matched = false;
						break;
					}
					matchedNair.add(new TDNodePair(a_match, b_match));
				}
				if (matched) result.add(matchedNair);
			}
		}
		return result;
	}
	
	public static TDNode[][][] simplify(TDNode[][] A, TDNode[][] B, float[][] SLPmix) {
		TDNode[][][] result = new TDNode[2][2][];
		TDNode[][] simplifyFirst = simplify(A[0],B[0], SLPmix);
		TDNode[][] simplifySecond = simplify(A[1],B[1], SLPmix);
		result[0][0] = simplifyFirst[0];
		result[1][0] = simplifyFirst[1];
		result[0][1] = simplifySecond[0];
		result[1][1] = simplifySecond[1];		
		return result;
	}
	


	public static void main(String[] args) {
		/*File toyFile = new File("./tmp/twoDTrees.txt");		
		ArrayList<TDNode> treebank = Corpus.readTreebankFromFile(toyFile);
		TDNode TDN0 = treebank.get(0);
		TDNode TDN1 = treebank.get(1);
		System.out.println(DepKernelPaths.getK(TDN0, TDN1));*/
		//correlation_UAS_K(50, 10);
		//total_correlation_UAS_K(true);
	}
	
}
