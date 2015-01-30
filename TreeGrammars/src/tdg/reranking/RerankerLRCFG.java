package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class RerankerLRCFG extends Reranker_ProbModel{
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	int uk_limit;
	int SLP_parent, SLP_children; 
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	
	Set<String> lexicon;
	
	public RerankerLRCFG(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, int SLP_parent,
			int SLP_children, int limitTestToFirst, boolean markTerminalNodes, boolean addEOS,
			boolean LR) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, 
				uk_limit, limitTestToFirst, addEOS, countEmptyDaughters, 
				markTerminalNodes, printTables, LR);
				
		updateCondFreqTables();
	}

	@Override
	public void updateCondFreqTables(TDNode thisNode) {
		String head = thisNode.lexPosTag(SLP_parent) + "_";
		String key, condKey;				
		
		condKey = key = head + left;
		if (thisNode.leftDaughters != null) {
			for(TDNode ld : thisNode.leftDaughters) {
				String ldTag = ld.lexPosTag(SLP_children) + "_";
				key += ldTag;
				updateCondFreqTables(ld);	
			}					
		}
		key += EOC;
		increaseInTables(key, condKey);
		
		condKey = key = head + right;
		if (thisNode.rightDaughters != null) {
			for(TDNode rd : thisNode.rightDaughters) {
				String rdTag = rd.lexPosTag(SLP_children) + "_";
				key += rdTag;
				updateCondFreqTables(rd);	
			}			
		}	
		key += EOC;
		increaseInTables(key, condKey);
	}
	
	@Override
	public double getProb(TDNode thisNode) {
		double prob = 1d;
		String head = thisNode.lexPosTag(SLP_parent) + "_";
		String key, condKey;
		
		condKey = key = head + left;
		if (thisNode.leftDaughters != null) {
			for(TDNode ld : thisNode.leftDaughters) {
				String ldTag = ld.lexPosTag(SLP_children) + "_";
				key += ldTag;
				prob *= getProb(ld);	
			}			
		}	
		key += EOC;
		prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey);
		
		condKey = key = head + right;
		if (thisNode.rightDaughters != null) {
			for(TDNode rd : thisNode.rightDaughters) {
				String rdTag = rd.lexPosTag(SLP_children) + "_";
				key += rdTag;
				prob *= getProb(rd);		
			}			
		}		
		key += EOC;
		prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey);
		
		return prob;
	}


	@Override
	public void updateKeyCondKeyLog(TDNode thisNode,
			Vector<String> keycondKeyLog) {
		// TODO Auto-generated method stub
		
	}	

	public static void main(String args[]) {			
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		boolean till11_21 = false;
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		
		int depType = 3; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter", "COLLINS99ArgTer" 
		
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
		boolean LR = true;
		boolean markTerminalNodes = false;
		
		int SLP_parent = 0;
		int SLP_children = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix						
		
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
			"SLP_parent\t" + SLP_parent + "\n" +
			"SLP_children\t" + SLP_children + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new RerankerLRCFG(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_parent, SLP_children, limitTestSetToFirst, markTerminalNodes, 
				addEOS, LR).reranking();
	}


}
