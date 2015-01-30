package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class RerankerArgFrame extends Reranker_ProbModel {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	int uk_limit;
	int SLP_parent;
	int SLP_daughters;
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	String div;
	
	Set<String> lexicon;
	
	public RerankerArgFrame(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int SLP_parent, int SLP_daughters, 
			int limitTestToFirst, boolean addEOS, boolean markTerminalNodes) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, markTerminalNodes, printTables, false);
		
		this.SLP_parent = SLP_parent;
		this.SLP_daughters = SLP_daughters;
		
		div = "|";
		
		updateCondFreqTables();
	}

	
	public void updateCondFreqTables(TDNode t) {
		String parentSubTag = t.lexPosTag(SLP_parent);
		String subCondKey = parentSubTag;
		String subKey = parentSubTag + "_" + EOC;
		if (t.leftDaughters!=null) {
			for(TDNode ld : t.leftDaughters) {
				if (ld.isArgumentDaughter()) {
					subKey += ld.lexPosTag(SLP_daughters) + "_";
				}
				updateCondFreqTables(ld);
			}
		}		
		subKey += div;
		if (t.rightDaughters != null) {
			for(TDNode rd : t.rightDaughters) {
				if (rd.isArgumentDaughter()) {
					subKey += rd.lexPosTag(SLP_daughters) + "_";
				}
				updateCondFreqTables(rd);
			}
		}		
		subKey += EOC;
		if (subKey.equals(parentSubTag + "_" + EOC + div + EOC)) return;
		increaseInTables(subKey, subCondKey);
	}
	
	public double getProb(TDNode t) {
		double prob = 1;
		String parentSubTag = t.lexPosTag(SLP_parent);		
		String subCondKey = parentSubTag;
		String subKey = parentSubTag + "_" + EOC;
		if (t.leftDaughters!=null) {
			for(TDNode ld : t.leftDaughters) {
				if (ld.isArgumentDaughter()) {
					subKey += ld.lexPosTag(SLP_daughters) + "_";
				}
				prob *= getProb(ld);
			}
		}
		subKey += div;
		if (t.rightDaughters != null) {
			for(TDNode rd : t.rightDaughters) {
				if (rd.isArgumentDaughter()) {
					subKey += rd.lexPosTag(SLP_daughters) + "_";
				}
				prob *= getProb(rd);
			}
		}
		subKey += EOC;
		if (subKey.equals(parentSubTag + "_" + EOC + div + EOC)) return prob;
		prob *= getCondProb(subKey, subCondKey);
		return prob;
	}
	
	@Override
	public void updateKeyCondKeyLog(TDNode thisNode,
			Vector<String> keycondKeyLog) {
		// TODO Auto-generated method stub
		
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
		
		boolean addEOS = true;
		boolean markTerminalNodes = false;
		int SLP_parent = 0;
		int SLP_daughters = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
					
		
		String parameters =
			"RerankerArgFrame" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"SLP_sub_parent\t" + SLP_parent + "\n" +
			"SLP_sub_daughters\t" + SLP_daughters + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new RerankerArgFrame(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_parent, SLP_daughters,
				limitTestSetToFirst, addEOS, markTerminalNodes).runToyGrammar();
		
	}

	public static void main(String args[]) {			
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		boolean till11_21 = false;
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		
		int depType = 3; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter", "COLLINS99TerArg"
		
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
		boolean markTerminalNodes = false;
		int SLP_parent = 0;
		int SLP_daughters = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
					
		
		String parameters =
			"RerankerArgFrame" + "\n" +
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
			"SLP_daughters\t" + SLP_daughters + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new RerankerArgFrame(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_parent, SLP_daughters, 
				limitTestSetToFirst, addEOS, markTerminalNodes).reranking();
	}


}
