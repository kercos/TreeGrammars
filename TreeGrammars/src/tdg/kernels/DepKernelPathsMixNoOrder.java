package tdg.kernels;

import java.util.*;

import tdg.TDNode;
import tdg.TDNodePair;
import util.*;

public class DepKernelPathsMixNoOrder extends DepKernelPathsMix{
	
	public DepKernelPathsMixNoOrder(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, float alfa, float kTreshold, float SLP_factor, float SP_factor) {
		super(currentNode, treebank, ignoreIndex, alfa, kTreshold, SLP_factor, SP_factor);
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
			TDNode[] A_daughters = A.gatherAllDaughters();
			TDNode[] B_daughters = B.gatherAllDaughters();
			ArrayList<ArrayList<TDNodePair>> pairs= n_airs(2, A_daughters, B_daughters, SLPmix);
			for(ArrayList<TDNodePair> tuple : pairs) {
				TDNodePair first = tuple.get(0);
				TDNodePair second = tuple.get(1);
				TDNode firstA = first.first;
				TDNode firstB = first.second;
				TDNode secondA = second.first;
				TDNode secondB = second.second;
				float CDPfirst = getCDP(firstA, firstB, CDP, SLPmix);
				float CDPsecond = getCDP(secondA, secondB, CDP, SLPmix);
				cpp += alfa2 + alfa * CDPfirst + alfa * CDPsecond + alfa2 * CDPfirst * CDPsecond;
			}
			cpp = cpp * SLPmix[A.index][B.index];
		}
		CPP[A.index][B.index] = cpp;
		return cpp;
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
		TDNode[][] AB = simplify(A,B, SLPmix);
		A = AB[0];
		B = AB[1];
		if(A==null || A.length<n || B.length<n) return result;		
		int[][] Anairs = NAirs.get(A.length, n);
		int[][] Bnairs = NAirs.get(B.length, n);		
		for(int[] a : Anairs) {
			for(int[] b : Bnairs) {
				ArrayList<TDNodePair> matchedNair = new ArrayList<TDNodePair>(n);
				BitSet machedInB = new BitSet();
				int matched = 0;
				for(int i=0; i<n; i++) {
					TDNode a_match = A[a[i]];
					for(int j=0; j<n; j++) {
						if (machedInB.get(j)) continue;
						TDNode  b_match = B[b[j]];						
						if (SLPmix[a_match.index][b_match.index]>0) {
							machedInB.set(j);							
							matchedNair.add(new TDNodePair(a_match, b_match));
							matched++;
							break;
						}	
					}					
				}
				if (matched==n) result.add(matchedNair);
			}
		}
		return result;
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
