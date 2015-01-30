package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import tdg.kernels.DepKernelGraph;
import util.*;
import util.file.FileUtil;

public class RerankerGraphsProbArgInsertion extends Reranker{
	
	private static boolean printTables = true;
	
	int maxBranching;
	
	boolean adjacency;
	
	int DKG_type;
			//0: order
			//1: noOrder
			//2: LR
			//3: orderNoFreq
			//4: argFrameOrder 
	int SLP_type; 
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	int collectionType; 
			//0: NodeSetCollectorStandard, 
			//1: NodeSetCollectorFreq, 
			//2: NodeSetCollectorMUB
			//3: NodeSetCollectorMUBFreq
			//4: NodeSetCollectorUnion
			//5: NodeSetCollectorArgFreq
	
	Hashtable<String, Integer> substitutionCondFreqTable;
	Hashtable<String, Integer> emptyDaughtersFreqTable, nodeCondFreqTable;
	Hashtable<String, Integer> insertionFreqTable, insertionCondFreqTable;
	float[] rerankedKernelProb;
	
	public RerankerGraphsProbArgInsertion(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, boolean countEmptyDaughters,
			boolean addTerminalNodes, int DKG_type, int maxBranching, int collectionType, 
			int SLP_type, int limitTestToFirst, boolean adjacency, boolean addEOS) {
		
		super(goldFile, parsedFile, nBest, addEOS, trainingCorpus, 
				uk_limit, false, addTerminalNodes, limitTestToFirst);

		this.countEmptyDaughters = countEmptyDaughters;
		this.DKG_type = DKG_type;
		this.maxBranching = maxBranching;
		this.collectionType = collectionType;
		this.SLP_type = SLP_type;
		this.adjacency = adjacency;
		
		initStatTables();
	}
	
	public boolean bestRerankedIsZeroScore() {
		return Utility.allZero(rerankedKernelProb);
	}
	
	@Override
	public void updateKeyCondKeyLog(TDNode thisNode,
			Vector<String> keycondKeyLog) {
		// TODO Auto-generated method stub
		
	}

	public void initBestRerankedScore() {
		rerankedKernelProb = null;
	}
	
	public String getRerankedScoreAsString() {
		return Arrays.toString(rerankedKernelProb);
	}
	
	private void initStatTables() {
		substitutionCondFreqTable = new Hashtable<String, Integer>();		
		emptyDaughtersFreqTable = new Hashtable<String, Integer>();	
		nodeCondFreqTable = new Hashtable<String, Integer>();
		insertionFreqTable = new Hashtable<String, Integer>(); 
		insertionCondFreqTable = new Hashtable<String, Integer>();
		for(TDNode t : trainingCorpus) {
			updateTables(t);				
		}	
		if (printTables) {
			File substitutionCondFreqTableFile = 
				new File(Parameters.outputPath + "substitutionCondFreqTable.txt");
			FileUtil.printHashtableToFileOrder(substitutionCondFreqTable, substitutionCondFreqTableFile);
			
			File emptyDaughtersFreqTableFile = 
				new File(Parameters.outputPath + "emptyDaughtersFreqTable.txt");
			FileUtil.printHashtableToFileOrder(emptyDaughtersFreqTable, emptyDaughtersFreqTableFile);
			
			File nodeCondFreqTableFile = 
				new File(Parameters.outputPath + "nodeCondFreqTable.txt");
			FileUtil.printHashtableToFileOrder(nodeCondFreqTable, nodeCondFreqTableFile);
			
			File insertionFreqTableFile = 
				new File(Parameters.outputPath + "insertionFreqTable.txt");
			FileUtil.printHashtableToFileOrder(insertionFreqTable, insertionFreqTableFile);
			
			File insertionCondFreqTableFile = 
				new File(Parameters.outputPath + "insertionCondFreqTable.txt");
			FileUtil.printHashtableToFileOrder(insertionCondFreqTable, insertionCondFreqTableFile);
		}
	}
	
	private void updateTables(TDNode tree) {
		TDNode[] structure = tree.getStructureArray();
		int[] subCondKeyArray = new int[structure.length]; 
		Arrays.fill(subCondKeyArray, -1);
		
		for(TDNode p : structure) {
			
			int subCondFreq = getSubCondFreq(p, subCondKeyArray); 
			String subCondKey = removeMarks(p.lexPosTag(SLP_type));
			Utility.increaseInTableInteger(substitutionCondFreqTable, subCondKey, subCondFreq);
			
			Utility.increaseInTableInteger(nodeCondFreqTable, subCondKey, 1);			
			if (p.countArgumentDaughters()==0) {
				Utility.increaseInTableInteger(emptyDaughtersFreqTable, subCondKey, 1);
			}
			TDNode[][] LR_daughters = p.daughters();
			for(int LR = 0; LR<2; LR++) {
				if (LR_daughters[LR]!=null) {
					String dir = (LR==0) ? "_L_" : "_R_";
					for(TDNode d : LR_daughters[LR]) {
						if (d.isArgumentDaughter()) continue;
						String insFreqKey = removeMarks(p.lexPosTag(SLP_type)) + dir + 
												d.lexPosTag(SLP_type);
						String insCondFreqKey = removeMarks(p.lexPosTag(SLP_type)) + dir;
						Utility.increaseInTableInteger(insertionFreqTable, insFreqKey, 1);
						Utility.increaseInTableInteger(insertionCondFreqTable, insCondFreqKey, 1);
					}
				}
			}	
		}				
	}
	
	private static int getSubCondFreq(TDNode p, int[] subCondKeyArray) {
		int index = p.index;
		int subCondFreq = subCondKeyArray[index];
		if (subCondFreq!=-1) return subCondFreq;
		TDNode[] argDaughters = p.getArgumentDaughters();
		subCondFreq = 0;
		if (argDaughters.length!=0) {
			for(TDNode ad : argDaughters) {
				int subCondFreqAd = getSubCondFreq(ad, subCondKeyArray);
				subCondFreq *= (subCondFreqAd+1);
			}	
		}
		return subCondKeyArray[index] = subCondFreq;
	}
	
	private static int getSubCondFreqNoEmptyDaughters(TDNode p, int[] subCondKeyArray) {
		int index = p.index;
		int subCondFreq = subCondKeyArray[index];
		if (subCondFreq!=-1) return subCondFreq;
		TDNode[] argDaughters = p.getArgumentDaughters();
		subCondFreq = 1;
		if (argDaughters.length!=0) {
			for(TDNode ad : argDaughters) {
				int subCondFreqAd = getSubCondFreq(ad, subCondKeyArray);
				subCondFreq *= (subCondFreqAd+1);
			}	
		}
		return subCondKeyArray[index] = subCondFreq;
	}
		
	/**
	 * Update the new prob of t if greater than rerankedKernelProb
	 * Returns 1 if greater (and modified)
	 * 0 if equal (non modified)
	 * -1 if less (non modified)
	 */
	public int updateRerankedScore(TDNode t, int index, String[] nBestScoresRecords) {
		float[] score =	DepKernelGraph.computeKernelProbArgInsertion(DKG_type, t, 
				trainingCorpus, -5, maxBranching, collectionType, SLP_type, 
				countEmptyDaughters, adjacency, substitutionCondFreqTable, emptyDaughtersFreqTable,
				nodeCondFreqTable, insertionFreqTable, insertionCondFreqTable);
		nBestScoresRecords[index] = Arrays.toString(score);
		if (rerankedKernelProb==null || Utility.greaterThan(score, rerankedKernelProb)) {
			rerankedKernelProb = score;
			return 1;
		}
		else if (Arrays.equals(score, rerankedKernelProb)) return 0;
		return -1;
	}
		
	public String getScoreAsString(TDNode t) {
		float[] score =	DepKernelGraph.computeKernelProbArgInsertion(DKG_type, t, 
				trainingCorpus, -5, maxBranching, collectionType, SLP_type, 
				countEmptyDaughters, adjacency, substitutionCondFreqTable, emptyDaughtersFreqTable,
				nodeCondFreqTable, insertionFreqTable, insertionCondFreqTable);
				
		return Arrays.toString(score);
	}
	
	/*
	 * Toy Sentence
	 */
	public static void main1(String[] args) {
		int uk_limit = -1;
		int LL_tr = 40;		
		int LL_ts = 40;
		int nBest = 10;				
		boolean mxPos = false;		
		int limitTestSetToFirst = 10000;
		
		String toySentence = Parameters.corpusPath + "ToyGrammar/toySentence_ArgStruc";		
		
		Parameters.outputPath = Parameters.resultsPath + "Reranker/" + FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		File trainingFile = new File (toySentence);
		File testFile = new File (toySentence);	
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);
			
		File outputTestGold = new File(Parameters.outputPath + "toySentence.ulab");
		DepCorpus.toFileMSTulab(outputTestGold, test, false);
		
		File parsedFile = new File(toySentence); 
		
		boolean countEmptyDaughters = false;
		boolean addTerminalNodes = true;
		boolean adjacency = false;
		boolean addEOS = true;
		int maxBranching = 10;
		
		int DKG_type = 4;
				//0: order
				//1: noOrder
				//2: LR1Level
				//3: orderNoFreq
				//4: argFrameOrder 
		int SLP_type = 0; 
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		int collectionType = 6; 
				//0: NodeSetCollectorStandard, 
				//1: NodeSetCollectorFreq, 
				//2: NodeSetCollectorMUB
				//3: NodeSetCollectorMUBFreq
				//4: NodeSetCollectorUnion
				//5: NodeSetCollectorArgFreq
				//6: NodeSetCollectorArgFreqTerm						
		
		String parameters =
			"RerankerGraphProbArgInsertion" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Count Empty Daughters\t" + countEmptyDaughters + "\n" +
			"Adjacency\t" + adjacency + "\n" +
			"Asdd EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"DKG type\t" + DKG_type + "\n" +
			"Collection Type\t" +  collectionType + "\n" +
			"SLP_type\t" + SLP_type + "\n" +
			"Max Branching\t" +  maxBranching + "\n" +
			"Use mxPos\t" + mxPos + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		//getOracle(outputTestGold, parsedFile, rerankingFile, nBest, training, uk_limit, 
		//		DKG_type, maxBranching, collectionType, SLP_type, limitTestSetToFirst,
		//		adjacency);
		//checkCoverage(rerankingFile, parsedFile, nBest, training, uk_limit, DKG_type, maxBranching, 
		//		collectionType, SLP_type, adjacency);
		new RerankerGraphsProbArgInsertion(outputTestGold, parsedFile, nBest, training, uk_limit, 
				countEmptyDaughters, addTerminalNodes, DKG_type, maxBranching, collectionType, SLP_type, 
				limitTestSetToFirst, adjacency, addEOS).runToyGrammar();
		
	}

	public static void main(String args[]) {
		int uk_limit = -1;
		int LL_tr = 40;		
		int LL_ts = 40;
		int nBest = 10;				
		boolean till11_21 = false;
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		
		int depType = 3; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter"
		
		String parsedFileBase = Parameters.resultsPath + "Reranker/Parsed/" + 
								((mxPos) ? "mxPOS/" : "goldPOS/") +
								wsjDepTypeName[depType] + "_sec22_nBest" + nBest + "/";		
		String baseDepDir = depTypeBase[depType];
		
		Parameters.outputPath = Parameters.resultsPath + "Reranker/" + FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		String trSec = till11_21 ? "02-11" : "02-21";
		
		File trainingFile = new File (baseDepDir + "wsj-" + trSec + ".ulab");
		File testFile = new File (baseDepDir + "wsj-22" + ((mxPos) ? ".mxpost" : "") + ".ulab");	
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);
			
		File outputTestGold = new File(Parameters.outputPath + wsjDepTypeName[depType] + ".22.gold.ulab");
		DepCorpus.toFileMSTulab(outputTestGold, test, false);
		
		File parsedFile = new File(parsedFileBase +
				"tr" + trSec + "_LLtr" + LL_tr + "_LLts" + LL_ts + "_nBest" + nBest); 
				//tr02-11_LLtr10_LLts10_nBest100.ulab
		
		if(!parsedFile.exists()) {
			makeNBest(LL_tr, LL_ts, nBest, till11_21, depType, mxPos);
		}
		
		boolean countEmptyDaughters = false;
		boolean addTerminalNodes = true;
		boolean adjacency = false;
		boolean addEOS = true;
		int maxBranching = 10;
		
		int DKG_type = 4;
				//0: order
				//1: noOrder
				//2: LR1Level
				//3: orderNoFreq
				//4: argFrameOrder 
		int SLP_type = 0; 
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		int collectionType = 6; 
				//0: NodeSetCollectorStandard, 
				//1: NodeSetCollectorFreq, 
				//2: NodeSetCollectorMUB
				//3: NodeSetCollectorMUBFreq
				//4: NodeSetCollectorUnion
				//5: NodeSetCollectorArgFreq
				//6: NodeSetCollectorArgFreqTerm		
						
		
		String parameters =
			"RerankerGraphProbArgInsertion" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Count Empty Daughters\t" + countEmptyDaughters + "\n" +
			"Adjacency\t" + adjacency + "\n" +
			"Asdd EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"DKG type\t" + DKG_type + "\n" +
			"Collection Type\t" +  collectionType + "\n" +
			"SLP_type\t" + SLP_type + "\n" +
			"Max Branching\t" +  maxBranching + "\n" +
			"Use mxPos\t" + mxPos + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		//getOracle(outputTestGold, parsedFile, rerankingFile, nBest, training, uk_limit, 
		//		DKG_type, maxBranching, collectionType, SLP_type, limitTestSetToFirst,
		//		adjacency);
		//checkCoverage(rerankingFile, parsedFile, nBest, training, uk_limit, DKG_type, maxBranching, 
		//		collectionType, SLP_type, adjacency);
		new RerankerGraphsProbArgInsertion(outputTestGold, parsedFile, nBest, training, uk_limit, 
				countEmptyDaughters, addTerminalNodes, DKG_type, maxBranching, collectionType, SLP_type, 
				limitTestSetToFirst, adjacency, addEOS).reranking();
	}		
}
