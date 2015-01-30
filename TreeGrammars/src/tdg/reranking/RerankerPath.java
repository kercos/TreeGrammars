package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import tdg.kernels.DepKernelPaths;
import util.Utility;
import util.file.FileUtil;

public class RerankerPath extends Reranker{
		
	boolean orderInDaughters;
	boolean adjacency;
	float alfa;
	int SLP_type;
	float kTreshold;
	float maxKernelScore;

	public RerankerPath(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, boolean orderInDaughter, float alfa, 
			float kTreshold, boolean adjacency, boolean addEOS,
			int uk_limit, int limitTestToFirst, boolean countEmptyDaughters, int SLP_type) {	
		
		super(goldFile, parsedFile, nBest, addEOS, trainingCorpus, 
				uk_limit, countEmptyDaughters, limitTestToFirst);
		
		this.orderInDaughters = orderInDaughter;
		this.alfa = alfa;
		this.kTreshold = kTreshold;
		this.adjacency = adjacency;
		this.SLP_type = SLP_type;
		
		reranking();
	}
	
	public boolean bestRerankedIsZeroScore() {
		return maxKernelScore==0;
	}


	@Override
	public void updateKeyCondKeyLog(TDNode thisNode,
			Vector<String> keycondKeyLog) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void initBestRerankedScore() {
		maxKernelScore = -1;		
	}

	@Override
	public String getScoreAsString(TDNode t) {
		float score = DepKernelPaths.computeKernelPathSimilarity(t, trainingCorpus, -5, 
				alfa, orderInDaughters, kTreshold, adjacency, SLP_type);
		return Float.toString(score);
	}

	@Override
	public int updateRerankedScore(TDNode t, int index, String[] nBestScoresRecords) {
		float score = DepKernelPaths.computeKernelPathSimilarity(t, trainingCorpus, -5, 
				alfa, orderInDaughters, kTreshold, adjacency, SLP_type);
		nBestScoresRecords[index] = Float.toString(score);
		if (score > maxKernelScore) {
			maxKernelScore = score;
			return 1;
		}
		else if (score == maxKernelScore) return 0;
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
		
		boolean orderInDaughters = true;		
		boolean adjacency = false;
		boolean addEOS = true;
		boolean countEmptyDaughters = false;
		float alfa = 0.8f;//0.2f * i;
		float kTreshold = 1;
		int SLP_type = 2;
						
		
		String parameters =
			"RerankerGraphProb" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Count Empty Daughters\t" + countEmptyDaughters + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new RerankerPath(outputTestGold, parsedFile, nBest, 
				training, orderInDaughters, alfa, kTreshold, adjacency, addEOS,
				uk_limit, limitTestSetToFirst, countEmptyDaughters, SLP_type);
	}	

}
