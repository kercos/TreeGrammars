package tdg.kernels;

import java.io.*;
import java.util.*;

import util.*;
import util.file.FileUtil;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import settings.Parameters;

public abstract class DepKernelPathsMix {
	
	public static NAirs NAirs = new NAirs(10);
	public static boolean countCommonWordsInK = false;
	
	float alfa,alfa2;
	TDNode currentNode;
	TDNode[] thisStructureArray;
	int currentLength, ignoreIndex;
	ArrayList<TDNode> treebank;
	float selfK;
	float kTreshold;
	float SLP_factor, SP_factor;
	int maxBranching;
	
	public DepKernelPathsMix() {		
	}
	
	/**
	 * DepKernelPath constructor.
	 * @param currentNode
	 * @param treebank
	 */
	public DepKernelPathsMix(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, float alfa, float kTreshold, float SLP_factor, float SP_factor,
			int maxBranching) {
		this.alfa = alfa;
		this.alfa2 = alfa*alfa;
		this.currentNode = currentNode;
		this.treebank = treebank;
		this.ignoreIndex = ignoreIndex;
		this.kTreshold = kTreshold;
		this.SLP_factor = SLP_factor;
		this.SP_factor = SP_factor;
		this.maxBranching = maxBranching;
		currentLength = currentNode.length();
		thisStructureArray = currentNode.getStructureArray();
		selfK = getK(currentNode, currentNode);
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
				float KcurrentOther = getK(currentNode, otherTree); 
				if (KcurrentOther > kTreshold) K += KcurrentOther;
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
		float K = 0;
		int index = 0;
		for(TDNode otherTree : treebank) {
			if (index!=ignoreIndex) {
				float KcurrentOther = getK(currentNode, otherTree);
				float otherK = getK(otherTree, otherTree);		
				K += KcurrentOther / Math.sqrt((selfK * otherK));
			}
			index++;
		}
		return K;
	}
	
	public static float computeKernelPathSimilarity(TDNode currentNode, ArrayList<TDNode> treebank, 
			int ignoreIndex, float alfa, boolean orderInDaughters, float kTreshold,
			float SLP_factor, float SP_factor, int maxBranching) {
		DepKernelPathsMix DK = (orderInDaughters) ?
				new DepKernelPathsMixOrder(currentNode, treebank, ignoreIndex, 
						alfa, kTreshold, SLP_factor, SP_factor, maxBranching) :
				new DepKernelPathsMixNoOrder(currentNode, treebank, ignoreIndex, 
						alfa, kTreshold, SLP_factor, SP_factor);	
		return DK.computeKernelPathSimilarity();
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
		float[][] SLPmix =  new float[thisSize][otherSize]; // same lexical postags
		build_SLPmix(SLPmix, thisStructureArray, otherStructureArray);
		float[][] CDP = new float[thisSize][otherSize];
		float[][] CPP = new float[thisSize][otherSize];
		Utility.fillDoubleFloatArray(CDP, -1);
		Utility.fillDoubleFloatArray(CPP, -1);
		for(TDNode nodeA : thisStructureArray) {
			for(TDNode nodeB : otherStructureArray) {
					K += getCPP(nodeA, nodeB, CDP, CPP, SLPmix);
					if (countCommonWordsInK) K++;
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
	 * SLP initialized to 0
	 */
	public void build_SLPmix(float[][] SLP, TDNode[] thisStructureArray, TDNode[] otherStructureArray) {
		for(int i=0; i<thisStructureArray.length; i++) {
			for(int j=0; j<otherStructureArray.length; j++) {
				if (!thisStructureArray[i].samePosTag(otherStructureArray[j])) continue;
				SLP[i][j] = (thisStructureArray[i].sameLexPosTag(otherStructureArray[j])) ? 
							this.SLP_factor : this.SP_factor;
			}
		}
	}
	
	/**
	 * Computes the Common Downwards Path table between the two
	 * input trees.
	 * @param A
	 * @param B
	 * @param CDP
	 * @return
	 */
	public float getCDP(TDNode A, TDNode B, float[][] CDP, float[][] SLPmix) {
		float cdp = CDP[A.index][B.index];
		if (cdp != -1) return cdp;
		cdp = 0;
		if(SLPmix[A.index][B.index]>0) {
			TDNode[][] ADaughters = A.daughters();
			TDNode[][] BDaughters = B.daughters();
			for(int d=0; d<2; d++) { // d: directions (left, right)
				if(ADaughters[d] !=null && BDaughters[d] != null) {
					for(TDNode CA : ADaughters[d]) {
						for(TDNode CB : BDaughters[d]) {							
							cdp += SLPmix[CA.index][CB.index] * alfa 
									+ alfa*getCDP(CA, CB, CDP, SLPmix);
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
	public abstract float getCPP(TDNode A, TDNode B, float[][] CDP, float[][] CPP, float[][] SLP);
	
	public static TDNode[][] simplify(TDNode[] A, TDNode[] B, float[][] SLP) {
		TDNode[][] result = new TDNode[2][];
		if (A==null || B==null) return result;		
		BitSet AtoKeep = new BitSet();
		BitSet BtoKeep = new BitSet();
		ArrayList<TDNode> newA = new ArrayList<TDNode>(A.length);		
		ArrayList<TDNode> newB = new ArrayList<TDNode>(B.length);
		for(int i=0; i<A.length; i++) {
			for(int j=0; j<B.length; j++) {
				if (SLP[A[i].index][B[j].index]>0) {
					if (!AtoKeep.get(i))  {
						AtoKeep.set(i);
						newA.add(A[i]);						
					}
					if (!BtoKeep.get(j)) {
						BtoKeep.set(j);
						newB.add(B[j]);
					}					
				}
			}
		}		
		result[0] = newA.toArray(new TDNode[AtoKeep.cardinality()]);
		result[1] = newB.toArray(new TDNode[BtoKeep.cardinality()]);		
		return result;
	}
	
	public static void correlation_UAS_K(int index, int LL, boolean orderInDaughter, 
			float alfa, float kTreshold, float SLP_factor, float SP_factor, int maxBranching){
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
				DepKernelPathsMix DK = (orderInDaughter) ?
						new DepKernelPathsMixOrder(t, treebank, index, alfa, kTreshold, 
								SLP_factor, SP_factor, maxBranching) :
						new DepKernelPathsMixNoOrder(t, treebank, index, alfa, kTreshold, 
								SLP_factor, SP_factor);	
				float Kscore = DK.computeKernelPathSimilarityNorm();
				out.println(UASscore + "\t" + Kscore);
			}
		}			
		out.close();
	}
	
	public static void total_correlation_UAS_K(boolean orderInDaughter, 
			float alfa, float kTreshold, float SLP_factor, float SP_factor, int maxBranching) {
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
					DepKernelPathsMix DK = (orderInDaughter) ?
							new DepKernelPathsMixOrder(t, treebank, index, alfa, kTreshold, 
									SLP_factor, SP_factor, maxBranching) :
							new DepKernelPathsMixNoOrder(t, treebank, index, alfa, kTreshold, 
									SLP_factor, SP_factor);
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
		float kTreshold = 0f;
		int LL = 10;
		int K = 10;
		float alfa = 1f;//0.05f * i; //0.25f;
		float SLP_factor = 1;
		float SP_factor = 1; 
		int maxBranching = 2;
		File MST_wsj_02_11 = new File (WsjD.WsjMSTulab + "wsj-02-11.ulab");		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(MST_wsj_02_11, LL, false, true);
		String words = "What	happened	Friday	was	the	worst	of	all	worlds	.";
		String posTags = "WP	VBD	NNP	VBD	DT	JJS	IN	DT	NNS	.";
		String indexes = "2	0	4	2	6	4	6	9	7	4";
		//String indexes = "4	1	2	0	6	4	6	9	7	4";
		TDNode testTDN = new TDNode(words.split("\t"), posTags.split("\t"), 
				Utility.parseIndexList(indexes.split("\t")), 1, 0);
		float Kscore = DepKernelPathsMix.computeKernelPathSimilarity(testTDN, training, -5, 
					alfa, orderInDaughter, kTreshold, SLP_factor, SP_factor, maxBranching);
		System.out.println(Kscore);
	}
	
}
