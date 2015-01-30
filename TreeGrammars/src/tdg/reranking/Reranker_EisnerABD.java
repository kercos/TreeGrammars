package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_EisnerABD extends Reranker_MultiModels {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	Reranker_ProbModel modelA, modelB, modelD;
	
	public Reranker_EisnerABD(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int A_SLP_type_parent, int A_SLP_type_daughter, 
			int Bb_SLP_type_parent, int Bb_SLP_type_daughter, 
			int C_SLP_type_parent, int C_SLP_type_daughter, int C_SLP_type_previous,
			int D_SLP_type_parent, int D_SLP_type_neighbour, int D_SLP_type_daughter,
			int D_SLP_type_previous, int limitTestToFirst, boolean addEOS, boolean markTerminalNodes, 
			int rerankingComputationType, boolean A_LR, boolean Bb_LR, boolean C_LR, boolean D_LR) {
		
		super(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, countEmptyDaughters,
				limitTestToFirst, addEOS, markTerminalNodes, rerankingComputationType);
		
		Reranker_ProbModel modelA = new Reranker_EisnerA(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, A_SLP_type_parent, 
				A_SLP_type_daughter, limitTestToFirst, false, false, false, A_LR);
		Reranker_ProbModel modelBb = new Reranker_EisnerBb(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, Bb_SLP_type_parent, Bb_SLP_type_daughter, 
				limitTestToFirst, false, false, Bb_LR);
		Reranker_ProbModel modelC = new Reranker_EisnerC(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, C_SLP_type_parent, C_SLP_type_daughter, 
				C_SLP_type_previous, limitTestToFirst, false, false, C_LR);	
		Reranker_ProbModel modelD = new Reranker_EisnerD(goldFile, parsedFile, nBest, 
				trainingCorpus, uk_limit, D_SLP_type_parent, D_SLP_type_neighbour, D_SLP_type_daughter, 
				D_SLP_type_previous, limitTestToFirst, false, false, D_LR);
		
		subModels = new Reranker_ProbModel[]{modelA, modelBb, modelC, modelD};
		subModelsNumber = 4;
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
		boolean A_LR = true;
		boolean Bb_LR = true;
		boolean C_LR = true;
		boolean D_LR = true;
		
		int A_SLP_type_parent = 0;
		int A_SLP_type_daughter = 0;
		int Bb_SLP_type_parent = 0;
		int Bb_SLP_type_daughter = 0;
		
		int C_SLP_type_parent = 0;
		int C_SLP_type_daughter = 0;
		int C_SLP_type_previous = 0;
		
		int D_SLP_type_parent = 0;
		int D_SLP_type_neighbour = 0;
		int D_SLP_type_daughter = 0;
		int D_SLP_type_previous = 0;
		
		int rerankingComputationType = 0;
		
		String parameters =
			"Eisner_B" + "\n" +
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
		
		new Reranker_EisnerABD(outputTestGold, parsedFile, nBest, training, uk_limit, 
				A_SLP_type_parent, A_SLP_type_daughter, 
				Bb_SLP_type_parent, Bb_SLP_type_daughter, 
				C_SLP_type_parent, C_SLP_type_daughter, C_SLP_type_previous,
				D_SLP_type_parent, D_SLP_type_neighbour, D_SLP_type_daughter, D_SLP_type_previous, 
				limitTestSetToFirst, addEOS, markTerminalNodes, rerankingComputationType, 
				A_LR, Bb_LR, C_LR, D_LR).runToyGrammar();
		
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
		boolean A_LR = true;
		boolean Bb_LR = true;
		boolean C_LR = true;
		boolean D_LR = true;
		
		int A_SLP_type_parent = 0;
		int A_SLP_type_daughter = 0;
		int Bb_SLP_type_parent = 0;
		int Bb_SLP_type_daughter = 0;
		
		int C_SLP_type_parent = 0;
		int C_SLP_type_daughter = 0;
		int C_SLP_type_previous = 0;
		
		int D_SLP_type_parent = 0;
		int D_SLP_type_neighbour = 0;
		int D_SLP_type_daughter = 0;
		int D_SLP_type_previous = 0;
		
		int rerankingComputationType = 1;
		
		String parameters =
			"Eisner_ABD" + "\n" +
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
		
		new Reranker_EisnerABD(outputTestGold, parsedFile, nBest, training, uk_limit, 
				A_SLP_type_parent, A_SLP_type_daughter, 
				Bb_SLP_type_parent, Bb_SLP_type_daughter, 
				C_SLP_type_parent, C_SLP_type_daughter, C_SLP_type_previous,
				D_SLP_type_parent, D_SLP_type_neighbour, D_SLP_type_daughter, D_SLP_type_previous, 
				limitTestSetToFirst, addEOS, markTerminalNodes, 
				rerankingComputationType, A_LR, Bb_LR, C_LR, D_LR).reranking();
	}

}
