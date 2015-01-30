package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;


public abstract class Reranker_Eisner extends Reranker{
	
	Hashtable<String,Integer> freqTable, condFreqTable;
	float maxScore;
	boolean LR;
	boolean printTables;
	static String EOC = "EOC_";
	static String noChild = "NULL";
	String left, right;
	
	public Reranker_Eisner(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int limitTestToFirst, boolean addEOS, boolean countEmptyDaughters, 
			boolean printTables, boolean LR) {		
		
		super(goldFile, parsedFile, nBest, addEOS, trainingCorpus, 
				uk_limit, countEmptyDaughters, limitTestToFirst);
		
		this.LR = LR;
		left = (LR) ? "L_" : "";
		right = (LR) ? "R_" : "";
		this.printTables = printTables;
		
		freqTable = new Hashtable<String, Integer>();
		condFreqTable = new Hashtable<String, Integer>();
	}
	
	public boolean bestRerankedIsZeroScore() {
		return maxScore==0;
	}
	
	public void updateCondFreqTables() {
		
		for(TDNode t : trainingCorpus) {
			updateCondFreqTables(t);			
		}
		
		if (printTables) {
			File headLRMarkovFreqTableFile = 
				new File(Parameters.outputPath + "headLRMarkovFreqTable.txt");
			FileUtil.printHashtableToFileOrder(freqTable, headLRMarkovFreqTableFile);
			
			File headLRPreviousFreqTableFile = 
				new File(Parameters.outputPath + "headLRPreviousFreqTable.txt");
			FileUtil.printHashtableToFileOrder(condFreqTable, headLRPreviousFreqTableFile);
		}
	
	}
	
	
	/**
	 * Recursively increase the frequency of the current node and L/R children in the table,
	 * in a one order markov process fashion. For each node the rules to be stored are:
	 * 
	 * - head + left + ld1 + noChild   				|   head + left + noChild
	 * - head + left + ld2 + ld1					|	head + left + ld1
	 * - ...										|	...
	 * - head + left + ld_last + ld_last-1			|	head + left + ld_last-1
	 * - head + left + EOC + ld_last				|	head + left + ld_last
	 * 
	 * - head + right + rd1 + noChild				|	head + right + noChild
	 * - head + right + rd2 + rd1					|	head + right + rd1
	 * - head + right + rd_last + rd_last-1			| 	head + right + rd_last-1
	 * - ...										|	...
	 * - head + right + EOC + rd_last				|	head + right + rd_last
	 * 
	 * @param fragmentSet
	 * @param SLP_type
	 */
	public abstract void updateCondFreqTables(TDNode thisNode);
	
	public abstract float getProb(TDNode thisNode);
	
	public void increaseInTables(String key, String condKey) {
		Utility.increaseInTableInteger(freqTable, key, 1);
		Utility.increaseInTableInteger(condFreqTable, condKey, 1);
	}

	@Override
	public String getRerankedScoreAsString() {
		return Float.toString(maxScore);
	}

	@Override
	public String getScoreAsString(TDNode t) {
		float score = getProb(t);
		return Float.toString(score);
	}

	@Override
	public void initBestRerankedScore() {
		maxScore = -1;
		
	}

	@Override
	public int updateRerankedScore(TDNode t) {
		float score = getProb(t);
		if (score > maxScore) {
			maxScore = score;
			return 1;
		}
		else if (score == maxScore) return 0;
		return -1;
	}		

}
