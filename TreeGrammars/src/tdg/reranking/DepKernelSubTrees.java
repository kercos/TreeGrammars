package tdg.reranking;
import java.io.*;
import java.util.*;

import tdg.TDNode;
import tdg.TDNodePair;
import tdg.corpora.DepCorpus;
import util.*;
import util.file.FileUtil;

public class DepKernelSubTrees {
	
	final public static float alfa = 1f;
	final public static float alfa2 = alfa*alfa; 
	final public static boolean keepDaughterOrdering = false;
	final public static boolean printTable = true;
	
	TDNode currentNode;
	TDNode[] thisStructureArray;
	int currentLength;
	ArrayList<TDNode> treebank;
	public BitSet[] bestKS;
	public boolean[] ambiguityTracker;
	public int[] cardinalityTracker;
	int maxCardinalityIndex;
	float selfK;
	

	public DepKernelSubTrees(TDNode currentNode, ArrayList<TDNode> treebank) {
		this.currentNode = currentNode;
		this.treebank = treebank;
		currentLength = currentNode.length();
		thisStructureArray = currentNode.getStructureArray();
		bestKS = new BitSet[currentLength];
		ambiguityTracker = new boolean[currentLength];
		cardinalityTracker = new int [currentLength];
		Arrays.fill(cardinalityTracker, -1);
		selfK = getK(currentNode, currentNode);
		//computeKernelSimilaity();
		//computeCPP();
		//computeBestKS();		
		//printBestKS();
	}
	
	public void computeBestKS() {
		for(TDNode otherTree : treebank) {
			if (currentNode==otherTree) continue;
			getKS(otherTree);
		}
	}
	
	public int computeKernelPathSimilarity() {
		int K = 0;
		for(TDNode otherTree : treebank) {
			if (currentNode==otherTree) continue;
			K += getK(currentNode, otherTree);
		}
		return K;
	}
	
	public float computeKernelPathSimilarityNorm() {
		float K = 0;
		if (selfK==0) return 0;
		for(TDNode otherTree : treebank) {
			if (currentNode==otherTree) continue;
			float KcurrentOther = getK(currentNode, otherTree);
			float otherK = getK(otherTree, otherTree);			
			if (otherK==0) continue;
			K += (float) KcurrentOther / Math.sqrt((selfK * otherK));
		}
		return K;
	}
	
	public static BitSet[] bestKS(TDNode currentNode, ArrayList<TDNode> treebank) {
		DepKernelSubTrees DK = new DepKernelSubTrees(currentNode, treebank);
		DK.computeBestKS();
		return DK.bestKS;
	}
	
	public void getKS(TDNode otherTree) {		
		TDNode[] otherStructureArray = otherTree.getStructureArray();
		int otherLength = otherStructureArray.length;		
		BitSet[][] KStable = new BitSet[currentLength][otherLength];
		for(TDNode nodeA : thisStructureArray) {
			for(TDNode nodeB : otherStructureArray) {
				getKS(nodeA, nodeB, KStable);				
			}			
		}
	}
	
	public void printBestKS() {
		for(TDNode nodeA : thisStructureArray) {
			System.out.println(TDNodeToString(nodeA, bestKS[nodeA.index]));
		}		
	}
	
	
	public BitSet getKS(TDNode nodeA, TDNode nodeB, BitSet[][] KStable) {
		BitSet ks = KStable[nodeA.index][nodeB.index];
		if (ks != null) return ks;
		ks = new BitSet(currentLength);
		if (nodeA.sameLexPosTag(nodeB)) {
			ks.set(nodeA.index);
			ArrayList<ArrayList<TDNode>> ump = uniqueMappingPairs(nodeA, nodeB);
			for(ArrayList<TDNode> pair : ump) {
				ks.or(getKS(pair.get(0), pair.get(1), KStable));
			}
		}
		KStable[nodeA.index][nodeB.index] = ks;
		updateBestKS(nodeA, ks);
		return ks;
	}
	
	public void updateBestKS(TDNode node, BitSet ks) {
		int index = node.index;
		int cardinality = ks.cardinality();
		if (cardinality > cardinalityTracker[index]) {
			cardinalityTracker[index] = cardinality;
			bestKS[index] = ks;
			ambiguityTracker[index] = false;
		}
		else if (cardinality == cardinalityTracker[index]) ambiguityTracker[index] = true;
	}
	
	public static String TDNodeToString(TDNode node, BitSet gst) {
		String result = "( "; 
		result += node.lexPosTag() + " ";	
		if (node.leftDaughters==null && node.rightDaughters==null) return result + ") ";
		if (node.leftDaughters!=null) {
			for(TDNode TDN : node.leftDaughters) {
				if (gst.get(TDN.index)) result += TDNodeToString(TDN, gst);
			}
		}		
		result += "* ";
		if (node.rightDaughters!=null) {
			for(TDNode TDN : node.rightDaughters) {
				if (gst.get(TDN.index)) result += TDNodeToString(TDN, gst);
			}
		}
		result += ") ";
		return result;
		
	}
	
	public static ArrayList<ArrayList<TDNode>> uniqueMappingPairs(TDNode nodeA, TDNode nodeB) {
		ArrayList<ArrayList<TDNode>> pairs = new ArrayList<ArrayList<TDNode>>();
		TDNode[][] nodeADaughters = new TDNode[][]{nodeA.leftDaughters, nodeA.rightDaughters};
		TDNode[][] nodeBDaughters = new TDNode[][]{nodeB.leftDaughters, nodeB.rightDaughters};
		
		for(int i=0; i<2; i++) {
			if(nodeADaughters[i] !=null && nodeBDaughters[i] != null) {
				int bIndex = 0;
				for(TDNode CA : nodeADaughters[i]) {
					for(int j=bIndex; j<nodeBDaughters[i].length; j++) {
						TDNode CB = nodeBDaughters[i][j];
						if (CA.sameLexPosTag(CB)) {
							pairs.add(makePair(CA,CB));
							bIndex = j;
							break;
						}
					}
				}
			}
		}
		return pairs;
	}
	
	public static ArrayList<TDNode> makePair(TDNode nodeA, TDNode nodeB) {
		ArrayList<TDNode> pair = new ArrayList<TDNode>(2);
		pair.add(nodeA);
		pair.add(nodeB);
		return pair;
	}

	
	public static float getK(TDNode thisTree, TDNode otherTree) {		
		float K = 0;
		TDNode[] thisStructureArray = thisTree.getStructureArray();
		TDNode[] otherStructureArray = otherTree.getStructureArray();
		int thisSize = thisStructureArray.length;
		int otherSize = otherStructureArray.length;
		float[][] CDP = new float[thisSize][otherSize];
		float[][] CPP = new float[thisSize][otherSize];
		Utility.fillDoubleFloatArray(CDP, -1);
		Utility.fillDoubleFloatArray(CPP, -1);		
		for(TDNode nodeA : thisStructureArray) {
			for(TDNode nodeB : otherStructureArray) {
				if (nodeA.sameLexPosTag(nodeB)) {
					K += 1 + getCPP(nodeA, nodeB, CDP, CPP);
				}
			}
		}
		/*String[] columnHeader = otherTree.getStructureLabelsArray();
		String[] headHeader = thisTree.getStructureLabelsArray();
		Utility.printFloatChart(CDP, columnHeader, headHeader);
		Utility.printFloatChart(CPP, columnHeader, headHeader);*/
		return K;
	}
	
	
	public static float getCDP(TDNode A, TDNode B, float[][] CDP) {
		float cdp = CDP[A.index][B.index];
		if (cdp != -1) return cdp;
		cdp = 0;
		if(A.sameLexPosTag(B)) {
			TDNode[][] ADaughters = A.daughters();
			TDNode[][] BDaughters = B.daughters();
			for(int i=0; i<2; i++) {
				if(ADaughters[i] !=null && BDaughters[i] != null) {
					for(TDNode CA : ADaughters[i]) {
						for(TDNode CB : BDaughters[i]) {
							if(CA.sameLexPosTag(CB)) {
								cdp += alfa + alfa*getCDP(CA, CB, CDP);
							}	
						}
					}
				}
			}
		}
		CDP[A.index][B.index] = cdp;
		return cdp;
	}
	
	public static float getCPP(TDNode A, TDNode B, float[][] CDP, float[][] CPP) {
		float cpp = CPP[A.index][B.index];
		if (cpp != -1) return cpp;
		cpp = getCDP(A, B, CDP);
		if (A.sameLexPosTag(B)) {
			ArrayList<ArrayList<TDNodePair>> pairs= n_airs(2, A.daughters(), B.daughters());
			for(ArrayList<TDNodePair> p : pairs) {
				TDNodePair first = p.get(0);
				TDNodePair second = p.get(1);
				TDNode firstA = first.first;
				TDNode firstB = first.second;
				TDNode secondA = second.first;
				TDNode secondB = second.second;
				float CDPfirst = getCDP(firstA, firstB, CDP);
				float CDPsecond = getCDP(secondA, secondB, CDP);
				cpp += alfa2 + alfa * CDPfirst + alfa * CDPsecond + alfa2 * CDPfirst * CDPsecond;
			}
		}
		CPP[A.index][B.index] = cpp;
		return cpp;
	}

	public static float getCPS(TDNode A, TDNode B, float[][] CDP, float[][] CPS) {
		float cpp = CPS[A.index][B.index];
		if (cpp != -1) return cpp;
		cpp = getCDP(A, B, CDP);
		if (A.sameLexPosTag(B)) {
			int maxN = Utility.max(new int[]{A.leftProle(), A.rightProle(), 
					B.leftProle(), B.rightProle()});
			ArrayList<ArrayList<TDNodePair>> nairs= new ArrayList<ArrayList<TDNodePair>>();
			for(int n=2; n<maxN; n++) {
				nairs.addAll(n_airs(n, A.daughters(), B.daughters()));
			}			
			for(ArrayList<TDNodePair> tuple : nairs) {
				TDNodePair first = tuple.get(0);
				TDNodePair second = tuple.get(1);
				TDNode firstA = first.first;
				TDNode firstB = first.second;
				TDNode secondA = second.first;
				TDNode secondB = second.second;
				float CDPfirst = getCDP(firstA, firstB, CDP);
				float CDPsecond = getCDP(secondA, secondB, CDP);
				cpp += 1 + CDPfirst + CDPsecond + (CDPfirst * CDPsecond);
			}
		}
		CPS[A.index][B.index] = cpp;
		return cpp;
	}

	
	public static ArrayList<ArrayList<TDNodePair>> n_airs(int n, TDNode[][] A, TDNode[][] B) {		
		ArrayList<ArrayList<TDNodePair>> result = new ArrayList<ArrayList<TDNodePair>>();
		result.addAll(n_airs(n, A[0], B[0]));
		result.addAll(n_airs(n, A[1], B[1]));
		int[][] splits = Utility.split(n);
		ArrayList<ArrayList<ArrayList<TDNodePair>>> leftSplits, rightSplits;
		leftSplits = new ArrayList<ArrayList<ArrayList<TDNodePair>>>(n-1);
		rightSplits = new ArrayList<ArrayList<ArrayList<TDNodePair>>>(n-1);
		for(int i=1; i<n; i++) {
			leftSplits.add(n_airs(i, A[0], B[0]));
			rightSplits.add(n_airs(i, A[1], B[1]));
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

	
	public static ArrayList<ArrayList<TDNodePair>> n_airs(int n, TDNode[] A, TDNode[] B) {
		ArrayList<ArrayList<TDNodePair>> result = new ArrayList<ArrayList<TDNodePair>>();
		if(A==null || B==null || A.length<n || B.length<n) return result;		
		int[][] Anairs = Utility.n_air(A.length, n);
		int[][] Bnairs = Utility.n_air(B.length, n);		
		for(int[] a : Anairs) {
			for(int[] b : Bnairs) {				
				ArrayList<TDNodePair> matchedNair = new ArrayList<TDNodePair>(n);
				boolean matched = true;
				for(int i=0; i<n; i++) {
					TDNode a_match = A[a[i]]; 
					TDNode b_match = B[b[i]];
					if (!a_match.sameLexPosTag(b_match)) {
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
	
	public static void correlation_UAS_K(int index, int LL){
		File trainFile = new File("/scratch/fsangati/CORPUS/WSJ/DEPWSJ/wsj-02-21.dep");
		//File trainFile = new File("./tmp/twoDTrees.txt");
		File tableFile = new File("./tmp/wsjN_correlation.txt");
		PrintWriter out = FileUtil.getPrintWriter(tableFile);
		ArrayList<TDNode> treebank = DepCorpus.readTreebankFromFileYM(trainFile, LL);
		TDNode goldTree = treebank.get(index);
		treebank.remove(index);
		ArrayList<HashSet<TDNode>> UASspectrum = goldTree.collectVariationUASspectrumSameRoot(10);
		float goldK = 0;
		int mistakes = -1;
		int goldTreeLength = goldTree.length();
		System.out.println(goldTree.toStringSentenceStructure());
		for(HashSet<TDNode> bin : UASspectrum) {
			mistakes++;
			int UAS = goldTreeLength-mistakes;
			float UASscore = (float)UAS/goldTreeLength;
			for(TDNode t: bin) {				
				DepKernelSubTrees DK = new DepKernelSubTrees(t, treebank);
				float Kscore_norm = DK.computeKernelPathSimilarityNorm();
				if (mistakes==0) goldK = Kscore_norm;
				Kscore_norm = Kscore_norm / goldK;
				out.println(UASscore + "\t" + Kscore_norm);
			}
		}			
		out.close();
	}

	public static void total_correlation_UAS_K(){
		//File trainFile = new File("/home/fsangati/CORPUS/WSJ/DEPWSJ/wsj-02-21.dep");
		//File trainFile = new File("/scratch/fsangati/CORPUS/WSJ/DEPWSJ/wsj-02-21.dep");
		//File trainFile = new File("/Users/fedya/CORPUS/WSJ/DEPWSJ/wsj-02-21.dep");
		File trainFile = new File("./tmp/twoDTrees.txt");
		//File tableFileExact = new File("/home/fsangati/Code/TreeGrammars/tmp/wsj10_UAS_K_correlation_exact.txt");
		//File tableFileNoise = new File("/home/fsangati/Code/TreeGrammars/tmp/wsj10_UAS_K_correlation_noise.txt");
		//File tableFile = new File("./tmp/wsj10_UAS_K_correlation.txt");
		File tableFileExact = new File("./tmp/toy_correlation_exact.txt");
		File tableFileNoise = new File("./tmp/toy_correlation_noise.txt");
		File tmpFile = new File("./tmp/tmp.txt");
		PrintWriter out_exact = FileUtil.getPrintWriter(tableFileExact);
		PrintWriter out_noise = FileUtil.getPrintWriter(tableFileNoise);
		PrintWriter out_tmp = FileUtil.getPrintWriter(tmpFile);
		ArrayList<TDNode> treebank = DepCorpus.readTreebankFromFileYM(trainFile, 20);
		int corpusSize = treebank.size();
		float goldK = 0;
		for(ListIterator<TDNode> i = treebank.listIterator(); i.hasNext(); ) {
			System.out.println((i.previousIndex()+2) + "/" + corpusSize);
			TDNode goldTree = i.next();
			i.remove();
			int goldTreeLength = goldTree.length();
			ArrayList<HashSet<TDNode>> UASspectrum = goldTree.collectVariationUASspectrumSameRoot(100);
			int mistakes = -1;			
			for(HashSet<TDNode> bin : UASspectrum) {
				mistakes++;
				int UAS = goldTreeLength-mistakes;
				float UASscore = (float)UAS/goldTreeLength;
				for(TDNode t: bin) {					
					DepKernelSubTrees DK = new DepKernelSubTrees(t, treebank);
					float Kscore_norm = DK.computeKernelPathSimilarityNorm();
					if (mistakes==0) goldK = Kscore_norm;
					Kscore_norm = Kscore_norm / goldK;
					out_noise.println(Utility.randomNoise(UASscore, 0.01f) + "\t" 
							+ Utility.randomNoise(Kscore_norm, 0.0001f));
					out_exact.println(UASscore + "\t" + Kscore_norm);
				}
			}
			i.add(goldTree);
		}
		out_exact.close();
		out_noise.close();
		out_tmp.close();
	}	
	public static void main(String[] args) {
		/*File toyFile = new File("./tmp/twoDTrees.txt");		
		ArrayList<TDNode> treebank = Corpus.readTreebankFromFile(toyFile);
		TDNode TDN = treebank.get(1);
		System.out.println(TDN);
		System.out.println(DepKernels.getK(TDN, TDN));*/
		correlation_UAS_K(50, 10);
		//total_correlation_UAS_K();
	}
	
}
