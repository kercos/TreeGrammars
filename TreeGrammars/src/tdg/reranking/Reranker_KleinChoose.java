package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_KleinChoose extends Reranker_ProbModel{
		
	private static boolean printTables = false;
	int SLP_type_parent, SLP_type_daughter; 
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	
	public Reranker_KleinChoose(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, int SLP_type_parent, 
			int SLP_type_daughter, int limitTestToFirst, boolean addEOS, boolean countEmptyDaughters,
			boolean LR) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, false, printTables, LR);
		
		this.SLP_type_parent = SLP_type_parent;
		this.SLP_type_daughter = SLP_type_daughter;
		updateCondFreqTables();		
	}
	
	
	public void updateCondFreqTables(TDNode thisNode) {
		String head = thisNode.lexPosTag(SLP_type_parent) + "_";
		String key, condKey;		
		if (thisNode.leftDaughters!=null) {
			for(TDNode ld : thisNode.leftDaughters) {
				String ldTag = ld.lexPosTag(SLP_type_daughter) + "_";
				key = head + left + ldTag;
				condKey = head + left;				
				increaseInTables(key, condKey);
				updateCondFreqTables(ld);
			}			
		}
		if (thisNode.rightDaughters!=null) {
			for(TDNode rd : thisNode.rightDaughters) {	
				String rdTag = rd.lexPosTag(SLP_type_daughter) + "_";
				key = head + right + rdTag;
				condKey = head + right;				
				increaseInTables(key, condKey);
				updateCondFreqTables(rd);			
			}
		}
	}
	
	public double getProb(TDNode thisNode) {
		double prob = 1f;
		String head = thisNode.lexPosTag(SLP_type_parent) + "_";
		String key, condKey;		
		if (thisNode.leftDaughters!=null) {
			for(TDNode ld : thisNode.leftDaughters) {
				String ldTag = ld.lexPosTag(SLP_type_daughter) + "_";
				key = head + left + ldTag;
				condKey = head + left;				
				prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey) *
						getProb(ld);
			}			
		}
		if (thisNode.rightDaughters!=null) {
			for(TDNode rd : thisNode.rightDaughters) {	
				String rdTag = rd.lexPosTag(SLP_type_daughter) + "_";
				key = head + right + rdTag;
				condKey = head + right;				
				prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey) *
						getProb(rd);			
			}
		}
		return prob;
	}
	
	public void updateKeyCondKeyLog(TDNode thisNode, Vector<String> keyCondKeyLog) {
		String head = thisNode.lexPosTag(SLP_type_parent) + "_";
		String key, condKey;		
		if (thisNode.leftDaughters!=null) {
			for(TDNode ld : thisNode.leftDaughters) {
				String ldTag = ld.lexPosTag(SLP_type_daughter) + "_";
				key = head + left + ldTag;
				condKey = head + left;				
				keyCondKeyLog.add(key);
				updateKeyCondKeyLog(ld, keyCondKeyLog);
			}			
		}
		if (thisNode.rightDaughters!=null) {
			for(TDNode rd : thisNode.rightDaughters) {		
				String rdTag = rd.lexPosTag(SLP_type_daughter) + "_";
				key = head + right + rdTag;
				condKey = head + right;				
				keyCondKeyLog.add(key);
				updateKeyCondKeyLog(rd, keyCondKeyLog);
			}
		}		
	}
	
	/*
	 * Toy Sentence
	 */
	public static void main1(String[] args) {
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		int limitTestSetToFirst = 10000;
		
		String toySentence = Parameters.corpusPath + "ToyGrammar/toySentence";		
		
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
		
		boolean addEOS = true;
		boolean countEmptyDaughters = false;
		boolean LR = true;
		int SLP_type_parent = 0;
		int SLP_type_daughter = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		
				
		String parameters =
			"Reranker_kleinChoose" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Asdd EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"SLP_type_parent\t" + SLP_type_parent + "\n" +
			"SLP_type_daughter\t" + SLP_type_daughter + "\n" +
			"Use LR direction\t" + LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);				
		System.out.println(parameters);
		
		FileUtil.appendReturn(parameters, Parameters.logFile);				
		System.out.println(parameters);
		
		new Reranker_KleinChoose(outputTestGold, parsedFile, nBest, training, uk_limit, SLP_type_parent, 
				SLP_type_daughter, limitTestSetToFirst, addEOS, countEmptyDaughters, LR).runToyGrammar();
		
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
		
		boolean addEOS = true;
		boolean countEmptyDaughters = false;
		boolean LR = true;
		int SLP_type_parent = 0;
		int SLP_type_daughter = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		
				
		String parameters =
			"Reranker_kleinChoose" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Asdd EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"SLP_type_parent\t" + SLP_type_parent + "\n" +
			"SLP_type_daughter\t" + SLP_type_daughter + "\n" +
			"Use LR direction\t" + LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);				
		System.out.println(parameters);
		
		new Reranker_KleinChoose(outputTestGold, parsedFile, nBest, training, uk_limit, SLP_type_parent, 
				SLP_type_daughter, limitTestSetToFirst, addEOS, countEmptyDaughters, LR).reranking();
	}


}
