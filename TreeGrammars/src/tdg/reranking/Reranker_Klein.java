package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_Klein extends Reranker_MultiModels {
	
	private static boolean countEmptyDaughters = false;
	
	
	public Reranker_Klein(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int choose_SLP_type_parent, int choose_SLP_type_daughter, 
			int stop_SLP_type_parent, int limitTestToFirst, 
			boolean addEOS, boolean markTerminalNodes, int rerankingComputationType,
			boolean choose_LR, boolean stop_LR) {
		
		super(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, countEmptyDaughters,
				limitTestToFirst, addEOS, markTerminalNodes, rerankingComputationType);
		
		Reranker_ProbModel chooseModel = new Reranker_KleinChoose(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, choose_SLP_type_parent, choose_SLP_type_daughter, 
				limitTestToFirst, false, false, choose_LR);
		
		Reranker_ProbModel stopModel = new Reranker_KleinStop(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, stop_SLP_type_parent, limitTestToFirst,
				false, false, stop_LR);		
		
		subModels = new Reranker_ProbModel[]{chooseModel, stopModel};
		subModelsNumber = 2;
	}
	
	@Override
	public void updateCondFreqTables(TDNode thisNode) {
		// TODO Auto-generated method stub
		
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
		boolean markTerminalNodes = false;
		boolean choose_LR = true;
		boolean stop_LR = true;
		
		int choose_SLP_type_parent = 0;
		int choose_SLP_type_daughter = 0;		
		int stop_SLP_type_parent = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix				
		
		int rerankingComputationType = 0;
		
		String parameters =
			"Reranker_Klein" + "\n" +
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
			"SLP_type_parent in choose model\t" + choose_SLP_type_parent + "\n" +
			"SLP_type_daughter in choose model\t" + choose_SLP_type_daughter + "\n" +
			"SLP_type_parent in stop model\t" + stop_SLP_type_parent + "\n" +
			"Use LR direction in choose model\t" + choose_LR + "\n" +
			"Use LR direction in stop model\t" + stop_LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_Klein(outputTestGold, parsedFile, nBest, training, uk_limit, 
				choose_SLP_type_parent, choose_SLP_type_daughter, 
				stop_SLP_type_parent, limitTestSetToFirst, addEOS, markTerminalNodes, 
				rerankingComputationType, choose_LR, stop_LR).runToyGrammar();
		
	}
	
	public static void main(String args[]) {			
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		boolean till11_21 = false;
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		
		int depType = 4; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter", "COLLINS99ArgTer"
		
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
		boolean choose_LR = true;
		boolean stop_LR = true;
		
		int choose_SLP_type_parent = 0;
		int choose_SLP_type_daughter = 0;		
		int stop_SLP_type_parent = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix				
		
		int rerankingComputationType = 0;
		
		String parameters =
			"Reranker_Klein" + "\n" +
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
			"SLP_type_parent in choose model\t" + choose_SLP_type_parent + "\n" +
			"SLP_type_daughter in choose model\t" + choose_SLP_type_daughter + "\n" +
			"SLP_type_parent in stop model\t" + stop_SLP_type_parent + "\n" +
			"Use LR direction in choose model\t" + choose_LR + "\n" +
			"Use LR direction in stop model\t" + stop_LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_Klein(outputTestGold, parsedFile, nBest, training, uk_limit, 
				choose_SLP_type_parent, choose_SLP_type_daughter, 
				stop_SLP_type_parent, limitTestSetToFirst, addEOS, markTerminalNodes, 
				rerankingComputationType, choose_LR, stop_LR).reranking();
	}



}
