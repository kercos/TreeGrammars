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

public class RerankerGraphsScore extends Reranker{
		
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
	
	int scoreType;
			//0: maxCardinalitySharedSubGraph
			//1: maxSpanning
			//2: maxSpanningFreq
			//3: totalSpanningFreq
			//4: sumMaxCardinalitiesSharedSubGraphs
			//5: dependentHeadScores
			//6: headDependentsScores
			//7: minDerivationLength
	int[] maxScore;
	
	
	public RerankerGraphsScore(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, int scoreType, 
			boolean countEmptyDaughters, int DKG_type, int maxBranching, int collectionType, 
			int SLP_type, int limitTestToFirst, boolean adjacency, boolean addEOS) {
		
		super(goldFile, parsedFile, nBest, addEOS, trainingCorpus, 
				uk_limit, countEmptyDaughters, limitTestToFirst);
		
		this.scoreType = scoreType;
		this.DKG_type = DKG_type;
		this.maxBranching = maxBranching;
		this.collectionType = collectionType;
		this.SLP_type = SLP_type;
		this.adjacency = adjacency;
		
		reranking();
			
	}
	
	public boolean bestRerankedIsZeroScore() {
		return Utility.allZero(maxScore);
	}

	@Override
	public void updateKeyCondKeyLog(TDNode thisNode,
			Vector<String> keycondKeyLog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initBestRerankedScore() {
		maxScore = null;		
	}

	@Override
	public String getScoreAsString(TDNode t) {
		int[] score =	DepKernelGraph.computeKernelScore(scoreType, DKG_type, t, 
				trainingCorpus, -5, maxBranching, collectionType, SLP_type, countEmptyDaughters,
				adjacency);
		return Arrays.toString(score);
	}
	
	@Override
	public int updateRerankedScore(TDNode t, int index, String[] nBestScoresRecords) {
		int[] score =	DepKernelGraph.computeKernelScore(scoreType, DKG_type, t, 
				trainingCorpus, -5, maxBranching, collectionType, SLP_type, countEmptyDaughters,
				adjacency);
		nBestScoresRecords[index] = Arrays.toString(score);
		if (maxScore==null || Utility.greaterThan(score, maxScore)) {
			maxScore = score;
			return 1;
		}
		else if (Arrays.equals(score, maxScore)) return 0;
		return -1;
	}	


	
	public static void main(String args[]) {			
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		boolean till11_21 = false;
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		
		int depType = 2; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg"
		
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
		boolean adjacency = false;
		boolean addEOS = true;	
		int maxBranching = 10;
		
		int DKG_type = 0;
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
		
		int collectionType = 2;
				//0: NodeSetCollectorStandard, 
				//1: NodeSetCollectorFreq, 
				//2: NodeSetCollectorMUB
				//3: NodeSetCollectorMUBFreq
				//4: NodeSetCollectorUnion
		
		int scoreType = 7;
				//0: maxCardinalitySharedSubGraph
				//1: maxSpanning
				//2: maxSpanningFreq
				//3: totalSpanningFreq
				//4: sumMaxCardinalitiesSharedSubGraphs		
				//5: dependentHeadScores
				//6: headDependentsScores
				//7: minDerivationLength
				
		
		String parameters =
			"RerankerGraphProb" + "\n" +
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
			"Score type\t" + scoreType + "\n" +
			"DKG type\t" + DKG_type + "\n" +
			"Collection Type\t" +  collectionType + "\n" +
			"SLP_type\t" + SLP_type + "\n" +
			"Max Branching\t" +  maxBranching + "\n";
		
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		//getOracle(outputTestGold, parsedFile, rerankingFile, nBest, training, uk_limit, 
		//		countEmptyDaughters, DKG_type, maxBranching, collectionType, SLP_type, 
		//		limitTestSetToFirst, adjacency);
		//checkCoverage(rerankingFile, parsedFile, nBest, training, uk_limit, 
		//		countEmptyDaughters, DKG_type, maxBranching, 
		//		collectionType, SLP_type, adjacency);
		new RerankerGraphsScore(outputTestGold, parsedFile, nBest, training, uk_limit, 
				scoreType, countEmptyDaughters, DKG_type, maxBranching, collectionType, SLP_type, 
				limitTestSetToFirst, adjacency, addEOS);
	}
	
}
