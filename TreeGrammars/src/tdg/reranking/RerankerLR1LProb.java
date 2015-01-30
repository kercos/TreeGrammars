package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class RerankerLR1LProb extends Reranker{
	
	private static boolean countEmptyDaughters = false;
	
	int uk_limit;
	int SLP_type; 
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	
	Set<String> lexicon;
	Hashtable<String,Integer> headFreqTable, headLRRuleFreqTable;
	float maxScore;
	
	public RerankerLR1LProb(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, int SLP_type, 
			int limitTestToFirst, Hashtable<String,Integer> headFreqTable, 
			Hashtable<String,Integer> headLRRuleFreqTable, boolean addEOS) {
		
		super(goldFile, parsedFile, nBest, addEOS, trainingCorpus, 
				uk_limit, countEmptyDaughters, limitTestToFirst);
		
		this.SLP_type = SLP_type;
		this.headFreqTable = headFreqTable;
		this.headLRRuleFreqTable = headLRRuleFreqTable;
				
		Hashtable<String, Integer> headStatTable = 
			DepCorpus.getHeadFreqTable(trainingCorpus, SLP_type);
		FileUtil.printHashtableToFile(headStatTable, 
				new File(Parameters.outputPath + "HeadStatTable.txt"));
		
		reranking();
	}
	
	public boolean bestRerankedIsZeroScore() {
		return maxScore==0;
	}
	
	@Override
	public String getRerankedScoreAsString() {
		return Float.toString(maxScore);
	}

	@Override
	public String getScoreAsString(TDNode t) {
		float score = t.getLR_RuleProb(headLRRuleFreqTable, headFreqTable, SLP_type);
		return Float.toString(score);
	}

	@Override
	public void initBestRerankedScore() {
		maxScore = -1;		
	}

	@Override
	public int updateRerankedScore(TDNode t) {
		float score = t.getLR_RuleProb(headLRRuleFreqTable, headFreqTable, SLP_type);
		if (score > maxScore) {
			maxScore = score;
			return 1;
		}
		else if (score == maxScore) return 0;
		return -1;
	}		

	public static void main(String args[]) {			
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		boolean till11_21 = false;
		int limitTestSetToFirst = 10000;
		
		boolean addEOS = true;
		
		int SLP_type = 1; 
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		
		Parameters.outputPath = Parameters.resultsPath + "Reranker/" + FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		String trSec = till11_21 ? "02-11" : "02-21";
		
		File trainingFile = new File (WsjD.WsjMSTulab + "wsj-" + trSec + ".ulab");
		File testFile = new File (WsjD.WsjMSTulab + "wsj-22.ulab");		
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);
		
		Hashtable<String, Integer> headFreqTable = 
			DepCorpus.getHeadFreqTable(training, SLP_type);
		File headFreqTableFile = new File(Parameters.outputPath + "headFreqTable.txt");
		FileUtil.printHashtableToFile(headFreqTable, headFreqTableFile);
		
		Hashtable<String, Integer> headLRRuleFreqTable = 
			DepCorpus.getHeadLRRuleFreqTable(training, SLP_type);
		File headLRRuleFreqTableFile = 
			new File(Parameters.outputPath + "headLRRuleFreqTableFile.txt");
		FileUtil.printHashtableToFile(headLRRuleFreqTable, headLRRuleFreqTableFile);
		
		
		File outputTestGold = new File(Parameters.outputPath + "MST.22.gold.ulab");
		DepCorpus.toFileMSTulab(outputTestGold, test, false);
		
		File parsedFile = new File(Parameters.resultsPath + "Reranker/MST_sec22_nbest/" +
				"tr" + trSec + "_LLtr" + LL_tr + "_LLts" + LL_ts + "_nBest" + nBest); 
				//tr02-11_LLtr10_LLts10_nBest100.ulab
		if(!parsedFile.exists()) {
			makeNBest(LL_tr, LL_ts, nBest, till11_21);
		}
				
		
		String parameters =
			"RerankerLR1LProb" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"SLP_type\t" + SLP_type + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new RerankerLR1LProb(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_type, limitTestSetToFirst, headFreqTable, headLRRuleFreqTable, addEOS);
	}


}
