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

public class Reranker_EisnerBb extends Reranker_ProbModel {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	int SLP_type_parent, SLP_type_daughter; 
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	
	public Reranker_EisnerBb(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int SLP_type_parent, int SLP_type_daughter, int limitTestToFirst,
			boolean addEOS, boolean markTerminalNodes, boolean LR) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, markTerminalNodes, printTables, LR);
		
		this.SLP_type_parent = SLP_type_parent;
		this.SLP_type_daughter = SLP_type_daughter;

		updateCondFreqTables();		
	}
	
	/**
	 * Recursively increase the frequency of the current node and L/R children in the table,
	 * condition on the type and direction of parents. For each node the rules to be stored are:
	 * 
	 * - daughter + left + parent					|   daughter + left
	 * 
	 * @param fragmentSet
	 * @param SLP_type
	 */
	public void updateCondFreqTables(TDNode t) {		
		TDNode[] structure = t.getStructureArray();
		for(int i=0; i<structure.length-1; i++) {
			TDNode thisNode = structure[i];
			TDNode parent = thisNode.parent;
			String dir = (parent.index < thisNode.index) ? left : right;
			String daughterTag = thisNode.lexPosTag(SLP_type_daughter) + "_";
			String parentTag = parent.lexPosTag(SLP_type_parent);
			String key = daughterTag + dir + parentTag;
			String condKey = daughterTag + dir;
			increaseInTables(key, condKey);
		}
	}
	
	public double getProb(TDNode t) {		
		double prob = 1f;
		
		TDNode[] structure = t.getStructureArray();
		for(int i=0; i<structure.length-1; i++) {
			TDNode thisNode = structure[i];
			TDNode parent = thisNode.parent;
			String dir = (parent.index < thisNode.index) ? left : right;
			String daughterTag = thisNode.lexPosTag(SLP_type_daughter) + "_";
			String parentTag = parent.lexPosTag(SLP_type_parent);
			String key = daughterTag + dir + parentTag;
			String condKey = daughterTag + dir;
			prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey);			
		}
		
		return prob;
	}
	
	public void updateKeyCondKeyLog(TDNode t, Vector<String> keyCondKeyLog) {
		TDNode[] structure = t.getStructureArray();
		for(int i=0; i<structure.length-1; i++) {
			TDNode thisNode = structure[i];
			TDNode parent = thisNode.parent;
			String dir = (parent.index < thisNode.index) ? left : right;
			String daughterTag = thisNode.lexPosTag(SLP_type_daughter) + "_";
			String parentTag = parent.lexPosTag(SLP_type_parent);
			String key = daughterTag + dir + parentTag;
			String condKey = daughterTag + dir;
			keyCondKeyLog.add(key + " | " + condKey);
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
		boolean LR = true;
		boolean markTerminalNodes = true;
		int SLP_type_parent = 0;
		int SLP_type_daughter = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
						
		
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
			"SLP_type_parent\t" + SLP_type_parent + "\n" +
			"SLP_type_daughter\t" + SLP_type_daughter + "\n" +
			"Use LR direction\t" + LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerBb(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_type_parent, SLP_type_daughter, limitTestSetToFirst,
				addEOS, markTerminalNodes, LR).runToyGrammar();
		
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
		
		Parameters.outputPath = parsedFileBase + "Reranker_EisnerBb/" + //"Reranker/" + 
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
		boolean LR = true;
		boolean markTerminalNodes = false;
		int SLP_type_parent = 0;
		int SLP_type_daughter = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
						
		
		String parameters =
			"Eisner_Bb" + "\n" +
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
		new Reranker_EisnerBb(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_type_parent, SLP_type_daughter, limitTestSetToFirst,
				addEOS, markTerminalNodes, LR).reranking();
		
		File rerankedFileConll = new File(Parameters.outputPath+ "reranked.ulab");		
		selectRerankedFromFileUlab(parsedFile, rerankedFileConll, Reranker.rerankedIndexes);
	}
	
	public static void main(String args[]) {
		
		int[] kBest = new int[]{2,3,4,5,6,7,8,9,10,100,1000}; //,15,20,25,50,150,200,250,500,750}; 
		for(int k : kBest) {
			mainWsj(22, k);
		}
		
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

}
