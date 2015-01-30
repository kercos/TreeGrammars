package tdg.reranking;
import java.io.*;
import java.util.*;

import pos.WsjPos;
import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_EisnerB extends Reranker_MultiModels {
	
	private static boolean countEmptyDaughters = false;
	
	public Reranker_EisnerB(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit,
			int Bb_SLP_type_parent, int Bb_SLP_type_daughter, 
			int C_SLP_type_parent, int C_SLP_type_daughter, int C_SLP_type_previous,
			int limitTestToFirst, boolean addEOS, boolean markTerminalNodes, 
			int rerankingComputationType, boolean Bb_LR, boolean C_LR) {
		
		super(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, countEmptyDaughters,
				limitTestToFirst, addEOS, markTerminalNodes, rerankingComputationType);
		
		Reranker_ProbModel parentModel = new Reranker_EisnerBb(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, Bb_SLP_type_parent, Bb_SLP_type_daughter, 
				limitTestToFirst, false, false, Bb_LR);

		Reranker_ProbModel childrenModel = new Reranker_EisnerC(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, 
				C_SLP_type_parent, C_SLP_type_daughter, C_SLP_type_previous, limitTestToFirst,
				false, false, C_LR, false);	
		
		subModels = new Reranker_ProbModel[]{childrenModel, parentModel};
		subModelsNumber = 2;
	}
	
	@Override
	public void updateCondFreqTables(TDNode thisNode) {
		// already implemented in each model
		
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
		boolean Bb_LR = true;
		boolean C_LR = true;
		
		int Bb_SLP_type_parent = 0;
		int Bb_SLP_type_daughter = 0;
		
		int C_SLP_type_parent = 0;
		int C_SLP_type_daughter = 0;
		int C_SLP_type_previous = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix						
		
		int rerankingComputationType = 0;
		
		String parameters =
			"RerankerLR1LProb" + "\n" +
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
			"SLP_type_parent in parent frame\t" + Bb_SLP_type_parent + "\n" +
			"SLP_type_daughter in parent frame\t" + Bb_SLP_type_daughter + "\n" +
			"SLP_type_parent in children frame\t" + C_SLP_type_parent + "\n" +
			"SLP_type_daughter in children frame\t" + C_SLP_type_daughter + "\n" +
			"SLP_type_previous in children frame\t" + C_SLP_type_previous + "\n" +
			"Use LR direction in parent frame\t" + Bb_LR + "\n" +
			"Use LR direction in chidren frame\t" + C_LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerB(outputTestGold, parsedFile, nBest, training, uk_limit, 
				Bb_SLP_type_parent, Bb_SLP_type_daughter, 
				C_SLP_type_parent, C_SLP_type_daughter, C_SLP_type_previous,
				limitTestSetToFirst, addEOS, markTerminalNodes, rerankingComputationType,
				Bb_LR, C_LR).runToyGrammar();
		
	}
	
	public static void mainWsj(int section, int nBest) {			
		TDNode.posAnalyzer = new WsjPos();
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;	
		boolean till11_21 = false;
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		boolean includeGoldInNBest = false;
		int order = 2;
		String MSTver = "0.5";
		
		int depType = 2; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter", "COLLINS99ArgTer"
		
		String withGold = includeGoldInNBest ? "_withGold" : "";
		String baseDepDir = depTypeBase[depType];
		String corpusName = wsjDepTypeName[depType] + "_sec" + section;
		
		String parsedFileBase = Parameters.resultsPath + "Reranker/Parsed/" +
								"MST_" + MSTver + "_" + order + "order/" + 
								((mxPos) ? "mxPOS/" : "goldPOS/") + corpusName + "/" +
								corpusName + "_nBest" + nBest + "/";				
		
		Parameters.outputPath = parsedFileBase + "Reranker_EisnerB/" + //"Reranker/" + 
			FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		String trSec = till11_21 ? "02-11" : "02-21";
		
		File trainingFile = new File (baseDepDir + "wsj-" + trSec + ".ulab");
		File testFile = new File (baseDepDir + "wsj-" + section + ((mxPos) ? ".mxpost" : "") + ".ulab");	
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);
		//training = new ArrayList<TDNode> (training.subList(0, 100));
		
		File outputTestGold = new File(Parameters.outputPath + wsjDepTypeName[depType] + "." + section + ".gold.ulab");
		//File outputTtraining = new File(Parameters.outputPath + depTypeName[depType] + trSec + ".ulab");
		DepCorpus.toFileMSTulab(outputTestGold, test, false);
		//DepCorpus.toFileMSTulab(outputTtraining, training, false);
		
		File parsedFile = new File(parsedFileBase +
				"tr" + trSec + "_LLtr" + LL_tr + "_LLts" + LL_ts + "_nBest" + nBest + withGold);
				//tr02-11_LLtr10_LLts10_nBest100.ulab
		
		if(!parsedFile.exists()) {
			System.out.println("NBest file not found: " + parsedFile);
			makeNBestWsj(LL_tr, LL_ts, nBest, till11_21, depType, mxPos, order, MSTver);
		}
		
		boolean addEOS = true;
		boolean Bb_LR = true;
		boolean C_LR = true;
		boolean markTerminalNodes = false;
		
		int Bb_SLP_type_parent = 0;
		int Bb_SLP_type_daughter = 0;
		
		int C_SLP_type_parent = 0;
		int C_SLP_type_daughter = 0;
		int C_SLP_type_previous = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		
		int rerankingComputationType = 2;
		
		String parameters =
			"Eisner_B" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Add EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"SLP_type_parent in parent frame\t" + Bb_SLP_type_parent + "\n" +
			"SLP_type_daughter in parent frame\t" + Bb_SLP_type_daughter + "\n" +
			"SLP_type_parent in children frame\t" + C_SLP_type_parent + "\n" +
			"SLP_type_daughter in children frame\t" + C_SLP_type_daughter + "\n" +
			"SLP_type_previous in children frame\t" + C_SLP_type_previous + "\n" +
			"Use LR direction in parent frame\t" + Bb_LR + "\n" +
			"Use LR direction in chidren frame\t" + C_LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerB(outputTestGold, parsedFile, nBest, training, uk_limit, 
				Bb_SLP_type_parent, Bb_SLP_type_daughter, 
				C_SLP_type_parent, C_SLP_type_daughter, C_SLP_type_previous,
				limitTestSetToFirst, addEOS, markTerminalNodes, rerankingComputationType,
				Bb_LR, C_LR).reranking();
		
		File rerankedFileConll = new File(Parameters.outputPath+ "reranked.ulab");		
		selectRerankedFromFileUlab(parsedFile, rerankedFileConll, Reranker.rerankedIndexes);
	}

	@Override
	public Number getScore(TDNode t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int compareTo(Number a, Number b) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public static void main(String args[]) {
		
		int[] kBest = new int[]{2,3,4,5,6,7,8,9,10,100,1000}; //,15,20,25,50,150,200,250,500,750}; 
		for(int k : kBest) {
			mainWsj(22, k);
		}
		
	}

}
