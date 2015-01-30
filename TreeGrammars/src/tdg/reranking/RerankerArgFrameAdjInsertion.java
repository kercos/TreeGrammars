package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class RerankerArgFrameAdjInsertion extends Reranker_MultiModels {
	
	private static boolean countEmptyDaughters = false;
	
	public RerankerArgFrameAdjInsertion(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int SLP_sub_parent, int SLP_sub_daughter, int SLP_ins_parent, int SLP_ins_daughter, 
			int limitTestToFirst, boolean addEOS, boolean markTerminalNodes, 
			int rerankingComputationType, boolean LR_ins) {
		
		super(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, countEmptyDaughters,
				limitTestToFirst, addEOS, markTerminalNodes, rerankingComputationType);
		
		Reranker_ProbModel ArgFrameModel = new RerankerArgFrame(goldFile, parsedFile, nBest, 
					trainingCorpus, uk_limit, SLP_sub_parent, SLP_sub_daughter, 
					limitTestToFirst, false, false);
		Reranker_ProbModel AdjInsertionModel = new RerankerAdjInsertion(goldFile, 
				parsedFile, nBest, trainingCorpus, uk_limit, 
				SLP_ins_parent, SLP_ins_daughter, 
				limitTestToFirst, addEOS, markTerminalNodes, LR_ins);
		
		subModels = new Reranker_ProbModel[]{AdjInsertionModel, ArgFrameModel};
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
	

	public static void main(String args[]) {			
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		boolean till11_21 = false;
		boolean mxPos = true;
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
		boolean LR_ins = true;
		boolean markTerminalNodes = false;
		int SLP_sub_parent = 0;
		int SLP_sub_daughter = 0;
		int SLP_ins_parent = 0;
		int SLP_ins_daughter = 0;
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
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"SLP_sub_parent\t" + SLP_sub_parent + "\n" +
			"SLP_sub_daughter\t" + SLP_sub_daughter + "\n" +
			"SLP_ins_parent\t" + SLP_ins_parent + "\n" +
			"SLP_ins_daughter\t" + SLP_ins_daughter + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new RerankerArgFrameAdjInsertion(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_sub_parent, SLP_sub_daughter, SLP_ins_parent, SLP_ins_daughter,
				limitTestSetToFirst, addEOS, markTerminalNodes, rerankingComputationType, 
				LR_ins).reranking();
	}


}
