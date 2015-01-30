package tdg.kernels;

import java.io.*;
import java.util.*;

import util.*;
import util.file.FileUtil;
import tdg.TDNode;
import tdg.TDNodePair;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import settings.Parameters;

public abstract class DepKernelPaths {
	
	private static int maxBranching = 10; 
	
	float alfa,alfa2;
	TDNode currentNode;
	TDNode[] thisStructureArray;
	int currentLength, ignoreIndex;
	ArrayList<TDNode> treebank;
	float selfK;
	float kTreshold;
	boolean adjacency;
	int SLP_type;
	
	
	public DepKernelPaths() {		
	}
	
	/**
	 * DepKernelPath constructor.
	 * @param currentNode
	 * @param treebank
	 */
	public DepKernelPaths(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, float alfa, float kTreshold, boolean adjacency, int SLP_type) {
		this.alfa = alfa;
		this.alfa2 = alfa*alfa;
		this.currentNode = currentNode;
		this.treebank = treebank;
		this.ignoreIndex = ignoreIndex;
		this.kTreshold = kTreshold;
		this.adjacency = adjacency;
		this.SLP_type = SLP_type;
		currentLength = currentNode.length();
		thisStructureArray = currentNode.getStructureArray();
		selfK = getK_SLP_SP(currentNode, currentNode);
	}
	
	/**
	 * Returns the sum of the K distance between the current tree
	 * and each trees in the treebank.
	 * @return
	 */
	public float computeKernelPathSimilarity_SLP_SP() {
		float K = 0f;
		int index = 0;
		for(TDNode otherTree : treebank) {
			if (index!=ignoreIndex) {
				float KcurrentOther = getK_SLP_SP(currentNode, otherTree);
				/*if (KcurrentOther[0]>0) {
					System.out.println("KscoreSLP: " + KcurrentOther[0] + " with tree:\n");
					System.out.println(otherTree.toStringMSTulab(false));
					System.out.println();
				}*/
				if (KcurrentOther > kTreshold) K += KcurrentOther;
			}
			index++;
		}
		return K;
	}
	
	/**
	 * Returns the sum of the K distance between the current tree
	 * and each trees in the treebank.
	 * @return
	 */
	public float computeKernelPathSimilarity() {
		float K = 0f;
		int index = 0;
		for(TDNode otherTree : treebank) {
			if (index!=ignoreIndex) {
				K += getK(currentNode, otherTree);
			}
			index++;
		}
		return K;
	}
	
	
	/**
	 * Returns the sum of the normalized K distance between the
	 * current tree each tree in the treebank.
	 * @return
	 */
	public float computeKernelPathSimilarityNorm() {
		float K = 0f;
		int index = 0;
		for(TDNode otherTree : treebank) {
			if (index!=ignoreIndex) {
				float KcurrentOther = getK_SLP_SP(currentNode, otherTree);
				float otherK = getK_SLP_SP(otherTree, otherTree);
				if (otherK==0 || selfK==0) continue;
				K += KcurrentOther / Math.sqrt((selfK * otherK));
			}
			index++;
		}
		return K;
	}
	
	public static float computeKernelPathSimilarity(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, float alfa, boolean orderInDaughters, float kTreshold, boolean adjacency,
			int SLP_type) {
		DepKernelPaths DK = (orderInDaughters) ?
				new DepKernelPathsOrder(currentNode, treebank, ignoreIndex, alfa, 
						kTreshold, adjacency, SLP_type) :
				new DepKernelPathsNoOrder(currentNode, treebank, ignoreIndex, alfa, 
						kTreshold, adjacency, SLP_type);	
		float Kscore = DK.computeKernelPathSimilarity_SLP_SP();
		//Kscore += SLP_factor * Kscore_SLP_SP[0] + SP_factor * Kscore_SLP_SP[1];
		return Kscore;
	}
	
	/**
	 * Computes the K distances between thisTree and otherTree,
	 * (counting SLP and SP)
	 * equivalent to the total number of common peek path between
	 * the two input trees, plus (if set) the number of common words. 
	 * @param thisTree
	 * @param otherTree
	 * @return
	 */
	public float getK_SLP_SP(TDNode thisTree, TDNode otherTree) {		
		float K = 0f;
		TDNode[] thisStructureArray = thisTree.getStructureArray();
		TDNode[] otherStructureArray = otherTree.getStructureArray();
		int thisSize = thisStructureArray.length;
		int otherSize = otherStructureArray.length;
		boolean[][] SLP =  buildSLP(thisStructureArray, otherStructureArray, SLP_type); // same lexical postags
		float[][] CDP_SLP = new float[thisSize][otherSize];
		float[][] CPP_SLP = new float[thisSize][otherSize];
		Utility.fillDoubleFloatArray(CDP_SLP, -1);
		Utility.fillDoubleFloatArray(CPP_SLP, -1);
		for(TDNode nodeA : thisStructureArray) {
			for(TDNode nodeB : otherStructureArray) {
				if (SLP[nodeA.index][nodeB.index]) {
					K += getCPP(nodeA, nodeB, CDP_SLP, CPP_SLP, SLP);
				}
			}
		}
		/*String[] columnHeader = otherTree.getStructureLabelsArray();
		String[] headHeader = thisTree.getStructureLabelsArray();
		Utility.printFloatChart(CDP, columnHeader, headHeader);
		Utility.printFloatChart(CPP, columnHeader, headHeader);*/
		return K;		
	}
	
	/**
	 * Computes the K distance between thisTree and otherTree,
	 * (counting SLP or SP)
	 * equivalent to the total number of common peek path between
	 * the two input trees, plus (if set) the number of common words. 
	 * @param thisTree
	 * @param otherTree
	 * @return
	 */
	public float getK(TDNode thisTree, TDNode otherTree) {		
		float K = 0;
		TDNode[] thisStructureArray = thisTree.getStructureArray();
		TDNode[] otherStructureArray = otherTree.getStructureArray();
		int thisSize = thisStructureArray.length;
		int otherSize = otherStructureArray.length;
		boolean[][] SLP = buildSLP(thisStructureArray, otherStructureArray, SLP_type);
		float[][] CDP = new float[thisSize][otherSize];
		float[][] CPP = new float[thisSize][otherSize];
		Utility.fillDoubleFloatArray(CDP, -1);
		Utility.fillDoubleFloatArray(CPP, -1);
		for(TDNode nodeA : thisStructureArray) {
			for(TDNode nodeB : otherStructureArray) {
				if (SLP[nodeA.index][nodeB.index]) {
					K += getCPP(nodeA, nodeB, CDP, CPP, SLP);
				}				
			}
		}
		/*String[] columnHeader = otherTree.getStructureLabelsArray();
		String[] headHeader = thisTree.getStructureLabelsArray();
		Utility.printFloatChart(CDP, columnHeader, headHeader);
		Utility.printFloatChart(CPP, columnHeader, headHeader);*/
		return K;		
	}
	
	/**
	 * Same lexical PosTag
	 */
	public static boolean[][] buildSLP(TDNode[] thisStructureArray, 
			TDNode[] otherStructureArray, int SLP_type) {
		boolean[][] SLP = new boolean[thisStructureArray.length][otherStructureArray.length];
		for(int i=0; i<thisStructureArray.length; i++) {
			for(int j=0; j<otherStructureArray.length; j++) {
				SLP[i][j] = thisStructureArray[i].sameLPtype(otherStructureArray[j], SLP_type);
			}
		}
		return SLP;
	}
	
	/**
	 * Same lexical PosTag
	 */
	public static boolean[][] buildSLPmix(TDNode[] thisStructureArray, 
			TDNode[] otherStructureArray, Set<String> posMatchLex) {
		boolean[][] SLP = new boolean[thisStructureArray.length][otherStructureArray.length];
		for(int i=0; i<thisStructureArray.length; i++) {
			for(int j=0; j<otherStructureArray.length; j++) {
				if (posMatchLex.contains(thisStructureArray[i]))
					SLP[i][j] = thisStructureArray[i].sameLexPosTag(otherStructureArray[j]);
				else SLP[i][j] = thisStructureArray[i].samePosTag(otherStructureArray[j]);
			}
		}
		return SLP;
	}
	
	/**
	 * Same Lex
	 */
	public static boolean[][] buildSL(TDNode[] thisStructureArray, TDNode[] otherStructureArray) {
		boolean[][] SP = new boolean[thisStructureArray.length][otherStructureArray.length];
		for(int i=0; i<thisStructureArray.length; i++) {
			for(int j=0; j<otherStructureArray.length; j++) {
				SP[i][j] = thisStructureArray[i].sameLex(otherStructureArray[j]);
			}
		}
		return SP;
	}
	
	/**
	 * Same PosTag
	 */
	public static boolean[][] buildSP(TDNode[] thisStructureArray, TDNode[] otherStructureArray) {
		boolean[][] SP = new boolean[thisStructureArray.length][otherStructureArray.length];
		for(int i=0; i<thisStructureArray.length; i++) {
			for(int j=0; j<otherStructureArray.length; j++) {
				SP[i][j] = thisStructureArray[i].samePosTag(otherStructureArray[j]);
			}
		}
		return SP;
	}
	
	/**
	 * Computes the Common Downwards Path table between the two
	 * input trees.
	 * @param A
	 * @param B
	 * @param CDP
	 * @return
	 */
	public float getCDP(TDNode A, TDNode B, float[][] CDP, boolean[][] SLP) {
		float cdp = CDP[A.index][B.index];
		if (cdp != -1) return cdp;
		cdp = 0;
		if(SLP[A.index][B.index]) {
			TDNode[][] ADaughters = A.daughters();
			TDNode[][] BDaughters = B.daughters();
			for(int d=0; d<2; d++) { // d: directions (left, right)
				if(ADaughters[d] !=null && BDaughters[d] != null) {
					for(TDNode CA : ADaughters[d]) {
						for(TDNode CB : BDaughters[d]) {							
							if(SLP[CA.index][CB.index]) {
								cdp += alfa + alfa*getCDP(CA, CB, CDP, SLP);
							}
						}
					}
				}
			}
		}
		CDP[A.index][B.index] = cdp;
		return cdp;
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
	public abstract float getCPP(TDNode A, TDNode B, float[][] CDP, float[][] CPP, boolean[][] SLP);
	
	/**
	 * Returns the ntuples of TDNodePairs in common between the array A and B.
	 * A and B are each a 1 array of TDNodes (possibly null).
	 * Each ntuple contains pairs (a,b) where a in A and b in B and a.sameLexicalPrelexical(b)
	 * Each ntuple ...,(a,b),(a',b'),(a'',b''),...
	 * is orderd in the sense that 
	 * A.index[a] < A.index[a'] < A.index[a''] and
	 * B.index[b] < B.index[b'] < B.index[b''].
	 * Better if A and B are simplified before
	 * @param n
	 * @param A
	 * @param B
	 * @param SLP
	 * @return
	 */
	public static ArrayList<ArrayList<TDNodePair>> n_airs(boolean order, int n, TDNode[] A, 
			TDNode[] B, boolean[][] SLP, boolean adjacency) {
		ArrayList<ArrayList<TDNodePair>> result = new ArrayList<ArrayList<TDNodePair>>();
		if(A==null || B==null || A.length<n || B.length<n) return result;
		int Alength = Math.min(A.length, maxBranching);
		int Blength = Math.min(B.length, maxBranching);
		int[][] Anairs = (adjacency) ? 
							Utility.NairAdj.get(Alength, n) : 
							Utility.Nair.get(Alength, n);
		int[][] Bnairs = (adjacency) ? 
							Utility.NairAdj.get(Blength, n) : 
							Utility.Nair.get(Blength, n);
		if (order) {
			for(int[] a : Anairs) {
				for(int[] b : Bnairs) {				
					ArrayList<TDNodePair> matchedNair = new ArrayList<TDNodePair>(n);
					boolean matched = true;
					for(int i=0; i<n; i++) {
						TDNode a_match = A[a[i]]; 
						TDNode b_match = B[b[i]];
						if (!SLP[a_match.index][b_match.index]) {
							matched = false;
							break;
						}
						matchedNair.add(new TDNodePair(a_match, b_match));
					}
					if (matched) result.add(matchedNair);
				}
			}
		}
		else {
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
							if (SLP[a_match.index][b_match.index]) {
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
		}		
		return result;
	}
	
	
	/**
	 * Returns a 2 arrays of TDNode
	 * result[0]: A removing the TDnode not present in B
	 * result[1]: B removing the TDnode not present in A
	 * @param A
	 * @param B
	 * @param SLP
	 * @return
	 */
	public static TDNode[][] simplify(TDNode[] A, TDNode[] B, boolean[][] SLP) {
		TDNode[][] result = new TDNode[2][];
		if (A==null || B==null) return result;		
		BitSet BtoKeep = new BitSet();
		ArrayList<TDNode> newA = new ArrayList<TDNode>(A.length);		
		int sizeA = 0;
		int sizeB = 0;
		for(TDNode Ai : A) {
			boolean addedAi = false;
			for(int j=0; j<B.length; j++) {
				if (SLP[Ai.index][B[j].index]) {
					if (!addedAi) {
						newA.add(Ai);
						addedAi = true;
						sizeA++;
					}
					if(!BtoKeep.get(j)) {
						BtoKeep.set(j);
						sizeB++;
					}	
				}
			}
		}
		if (sizeA==0) return result;
		result[0] = newA.toArray(new TDNode[sizeA]);
		result[1] = new TDNode[sizeB];
		int j = 0;		
		int nextBIndex = -1;
		do {
			nextBIndex = BtoKeep.nextSetBit(nextBIndex+1);
			result[1][j] = B[nextBIndex];			
			j++;
		} while(j<sizeB);
		return result;
	}
	
	public static void correlation_UAS_K(int index, int LL, boolean orderInDaughter, 
			float alfa, float kTreshold, boolean adjacency, int SLP_type){
		File trainFile = new File(WsjD.WsjYM + "wsj-02-21.dep");
		//File trainFile = new File("./tmp/twoDTrees.txt");
		File tableFile = new File("./tmp/wsjN_correlation.txt");
		PrintWriter out = FileUtil.getPrintWriter(tableFile);
		ArrayList<TDNode> treebank = DepCorpus.readTreebankFromFileYM(trainFile, LL);
		TDNode goldTree = treebank.get(index);
		treebank.remove(index);
		ArrayList<HashSet<TDNode>> UASspectrum = goldTree.collectVariationUASspectrumSameRoot(10);
		int mistakes = -1;
		int goldTreeLength = goldTree.length();
		System.out.println(goldTree.toStringSentenceStructure());
		for(HashSet<TDNode> bin : UASspectrum) {
			mistakes++;
			int UAS = goldTreeLength-mistakes;
			float UASscore = (float)UAS/goldTreeLength;
			for(TDNode t: bin) {
				DepKernelPaths DK = (orderInDaughter) ?
						new DepKernelPathsOrder(t, treebank, index, alfa, kTreshold, adjacency, SLP_type) :
						new DepKernelPathsNoOrder(t, treebank, index, alfa, kTreshold, adjacency, SLP_type);	
				float Kscore_norm = DK.computeKernelPathSimilarityNorm();
				out.println(UASscore + "\t" + Kscore_norm);
			}
		}			
		out.close();
	}
	
	public static void total_correlation_UAS_K(boolean orderInDaughter, 
			float alfa, float kTreshold, boolean adjacency, int SLP_type) {
		File trainFile = new File(WsjD.WsjYM + "wsj-02-21.dep");
		//File trainFile = new File("./tmp/twoDTrees.txt");
		File tableFileExact = new File(Parameters.resultsPath + "KernelPaths/wsj10_UAS_K_correlation_exact.txt");
		File tableFileNoise = new File(Parameters.resultsPath + "KernelPaths/wsj10_UAS_K_correlation_noise.txt");
		//File tableFileExact = new File("./tmp/toy_correlation_exact.txt");
		//File tableFileNoise = new File("./tmp/toy_correlation_noise.txt");
		//File tmpFile = new File("./tmp/tmp.txt");
		PrintWriter out_exact = FileUtil.getPrintWriter(tableFileExact);
		PrintWriter out_noise = FileUtil.getPrintWriter(tableFileNoise);
		//PrintWriter out_tmp = FileUtil.getPrintWriter(tmpFile);
		ArrayList<TDNode> treebank = DepCorpus.readTreebankFromFileYM(trainFile, 10);
		int corpusSize = treebank.size();		
		for(ListIterator<TDNode> i = treebank.listIterator(); i.hasNext(); ) {			
			TDNode goldTree = i.next();
			int index = i.previousIndex();
			System.out.println((index+1) + "/" + corpusSize);
			int goldLength = goldTree.length();
			if (goldLength==1) continue;
			float goldK = 0;
			ArrayList<HashSet<TDNode>> UASspectrum = goldTree.collectVariationUASspectrumSameRoot(1);
			int mistakes = -1;			
			for(HashSet<TDNode> bin : UASspectrum) {
				mistakes++;
				int UAS = goldLength-mistakes;
				float UASscore = (float)UAS/goldLength;
				for(TDNode t: bin) {
					if (t.length()==1) continue;
					DepKernelPaths DK = (orderInDaughter) ?
							new DepKernelPathsOrder(t, treebank, index, alfa, kTreshold, adjacency, SLP_type) :
							new DepKernelPathsNoOrder(t, treebank, index, alfa, kTreshold, adjacency, SLP_type);
					float Kscore_norm = DK.computeKernelPathSimilarityNorm(); 					
					if (mistakes==0) goldK = Kscore_norm;
					Kscore_norm = Kscore_norm / goldK; // normalization on the gold score
					if (Float.isNaN(Kscore_norm)) continue; // division by 0
					if (UASscore>1) {
						System.out.println("UAS>1: " + UASscore + " at sentence index " + 
								index + "\n" + t.toString() + "\n");
					}
					if (Kscore_norm>1) {
						System.out.println("Kscore_norm>1: " + Kscore_norm + " at sentence index " + 
								index + "\n" + "UAS: " + UASscore + "\n" + t.toString() + "\n");
					}
					out_noise.println(Utility.randomNoise(UASscore, 0.01f) + "\t" 
							+ Utility.randomNoise(Kscore_norm, 0.0001f));
					out_exact.println(UASscore + "\t" + Kscore_norm);
				}
				if (goldK==0) continue; // NaN result (division by 0)
			}
		}
		out_exact.close();
		out_noise.close();
		//out_tmp.close();
	}	
	
	public static void main(String args[]) {
		boolean orderInDaughter = true;
		float kTreshold = 0.f;
		int LL = 10;
		float alfa = 1f;//0.05f * i; //0.25f;
		boolean adjacency = false; 
		int SLP_type = 0;
		//0: same postag
		//1: same lex
		//2: same lexpostag
		//3: mix		
		
		File MST_wsj_02_11 = new File (WsjD.WsjMSTulab + "wsj-02-11.ulab");		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(MST_wsj_02_11, LL, false, true);
		String words = "What	happened	Friday	was	the	worst	of	all	worlds	.";
		String posTags = "WP	VBD	NNP	VBD	DT	JJS	IN	DT	NNS	.";
		String indexes = "2	0	4	2	6	4	6	9	7	4";
		//String indexes = "4	1	2	0	6	4	6	9	7	4";
		TDNode testTDN = new TDNode(words.split("\t"), posTags.split("\t"), 
				Utility.parseIndexList(indexes.split("\t")), 1, 0);
		float Kscore = 
			DepKernelPaths.computeKernelPathSimilarity(testTDN, training, -5, 
					alfa, orderInDaughter, kTreshold, adjacency, SLP_type);
		System.out.println(Kscore);		
	}
	
}
