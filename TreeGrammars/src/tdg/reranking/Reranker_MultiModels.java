package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;

public abstract class Reranker_MultiModels extends Reranker_ProbModel {
	
	private static boolean printTables = false;
	
	Reranker_ProbModel[] subModels;
	int rerankingComputationType;
		// 0: scores product
		// 1: at least one score greater and all other non smaller
		// 2: ordered scores (look at first; if equal look at second; 
		//		when looking, if greater keep it, if smaller give up)
	double[] maxScoreVector;
	int subModelsNumber;
	
	public Reranker_MultiModels(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, boolean countEmptyDaughters,
			int limitTestToFirst, boolean addEOS, boolean markTerminalNodes,
			int rerankingComputationType) {
		
		super(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, 
				markTerminalNodes, printTables, false);
		
		this.rerankingComputationType = rerankingComputationType;
		
	}
	
	public void printTables() {
		for(Reranker_ProbModel model : subModels) {
			model.printTables();
		}
	}

	@Override
	public void initBestRerankedScore() {
		maxScoreVector = (rerankingComputationType==0) ? 
				new double[1] : new double[subModelsNumber];
		Arrays.fill(maxScoreVector, -1);
	}
	
	public boolean bestRerankedIsZeroScore() {
		return Utility.allZero(maxScoreVector);
	}
	
	@Override
	public int updateRerankedScore(TDNode t, int index, String[] nBestScoresRecords) {
		double[] scoreVector = getProbVector(t);
		nBestScoresRecords[index] = Arrays.toString(scoreVector);
		if (Arrays.equals(scoreVector, maxScoreVector)) return 0;
		switch(rerankingComputationType) {
			case 0: 
				if (scoreVector[0] > maxScoreVector[0]) {
					maxScoreVector = scoreVector;
					return 1;
				}
				break;
			case 1:
				if (Utility.greaterThan(scoreVector, maxScoreVector)) {
					maxScoreVector = scoreVector;
					return 1;
				}
				break;
			case 2:
				if (Utility.greaterThanPriority(scoreVector, maxScoreVector)) {
					maxScoreVector = scoreVector;
					return 1;
				}
				break;
		}
		return -1;
	}
	
	public double[] getProbVector(TDNode t) {
		if (rerankingComputationType==0) {
			double productProb = 1;
			for(int i=0; i<subModelsNumber; i++) {
				productProb *= subModels[i].getProb(t);
			}
			return new double[]{productProb};
		}
		double[] result = new double[subModelsNumber];
		for(int i=0; i<subModelsNumber; i++) {
			result[i] = subModels[i].getProb(t);
		}
		return result;
	}
	
	public double getProb(TDNode t) {	
		return -1;
	}
	
	@Override
	public String getScoreAsString(TDNode t) {
		return Arrays.toString(getProbVector(t));
	}

}
