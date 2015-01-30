package tdg.reranking;
import java.io.*;
import java.util.*;

import pos.TanlPos;
import pos.WsjPos;
import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.TanlD;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_EisnerC extends Reranker_ProbModel {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	boolean distance;
	int SLP_type_parent, SLP_type_daughter, SLP_type_previous; 
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	
	public Reranker_EisnerC(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int SLP_type_parent, int SLP_type_daughter, int SLP_type_previous, int limitTestToFirst,
			boolean addEOS, boolean markTerminalNodes, boolean LR, boolean distance) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, markTerminalNodes, printTables, LR);
		
		this.SLP_type_parent = SLP_type_parent;
		this.SLP_type_daughter = SLP_type_daughter;
		this.SLP_type_previous = SLP_type_previous;
		this.distance = distance;

		updateCondFreqTables();				
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
	public void updateCondFreqTables(TDNode thisNode) {		
		String head = thisNode.lexPosTag(SLP_type_parent) + "_";
		String previousTag;
		String key, condKey;		
		previousTag = noChild;
		if (thisNode.leftDaughters!=null) {
			for(int li = thisNode.leftProle()-1; li>=0; li--) {
				TDNode ld = thisNode.leftDaughters[li];
				String ldTag = ld.lexPosTag(SLP_type_daughter) + "_";
				key = head + left + ldTag + previousTag;
				if (distance) key += "_" + getDistance(thisNode, ld);
				condKey = head + left + previousTag;				
				increaseInTables(key, condKey);
				previousTag = ld.lexPosTag(SLP_type_previous);
				updateCondFreqTables(ld);				
			}
		}
		key = head+ left + EOC + previousTag;		
		if (distance) key += "_" + groupDistance(thisNode.leftProle()+1);
		condKey = head + left + previousTag;	
		increaseInTables(key, condKey);

		previousTag = noChild;
		if (thisNode.rightDaughters!=null) {
			for(TDNode rd : thisNode.rightDaughters) {		
				String rdTag = rd.lexPosTag(SLP_type_daughter) + "_";
				key = head + right + rdTag + previousTag;
				if (distance) key += "_" + getDistance(thisNode, rd);
				condKey = head + right + previousTag;				
				increaseInTables(key, condKey);
				previousTag = rd.lexPosTag(SLP_type_previous);
				updateCondFreqTables(rd);				
			}
		}
		key = head + right + EOC + previousTag;
		if (distance) key += "_" + groupDistance(thisNode.rightProle()+1);
		condKey = head + right + previousTag;
		increaseInTables(key, condKey);
	}
	
	public double getProb(TDNode thisNode) {
		
		double prob = 1d;
		
		String head = thisNode.lexPosTag(SLP_type_parent) + "_";
		String previousTag;
		String key, condKey;
		
		previousTag = noChild;
		if (thisNode.leftDaughters!=null) {
			for(int li = thisNode.leftProle()-1; li>=0; li--) {
				TDNode ld = thisNode.leftDaughters[li];
				String ldTag = ld.lexPosTag(SLP_type_daughter) + "_";
				key = head + left + ldTag + previousTag;
				if (distance) key += "_" + getDistance(thisNode, ld);
				condKey = head + left + previousTag;				
				prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey) *
						getProb(ld);
				previousTag = ld.lexPosTag(SLP_type_previous);
			}
		}
		key = head + left + EOC + previousTag;
		if (distance) key += "_" + groupDistance(thisNode.leftProle()+1);
		condKey = head + left + previousTag;	
		prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey);

		previousTag = noChild;
		if (thisNode.rightDaughters!=null) {
			for(TDNode rd : thisNode.rightDaughters) {		
				String rdTag = rd.lexPosTag(SLP_type_daughter) + "_";
				key = head + right + rdTag + previousTag;
				if (distance) key += "_" + getDistance(thisNode, rd);
				condKey = head + right + previousTag;						
				prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey) *
				getProb(rd);
				previousTag = rd.lexPosTag(SLP_type_previous);		
			}
		}
		key = head + right + EOC + previousTag;
		if (distance) key += "_" + groupDistance(thisNode.rightProle()+1);
		condKey = head + right + previousTag;
		prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey);
		
		return prob;
	}
	
	public static int getDistance(TDNode head, TDNode dep) {
		int dis = Math.abs(head.index - dep.index);
		return groupDistance(dis);
	}
	
	public static int groupDistance(int dis) {
		if (dis==1) return 1;
		if (dis==2) return 2;
		if (dis<7) return 3;
		return 4;
	}
	
	public void updateKeyCondKeyLog(TDNode thisNode, Vector<String> keyCondKeyLog) {
		String head = thisNode.lexPosTag(SLP_type_parent) + "_";
		String previousTag;
		String key, condKey;
		
		previousTag = noChild;
		if (thisNode.leftDaughters!=null) {
			for(int li = thisNode.leftProle()-1; li>=0; li--) {
				TDNode ld = thisNode.leftDaughters[li];
				String ldTag = ld.lexPosTag(SLP_type_daughter) + "_";
				key = head + left + ldTag + previousTag;
				condKey = head + left + previousTag;		
				keyCondKeyLog.add(key);
				updateKeyCondKeyLog(ld, keyCondKeyLog);
				previousTag = ld.lexPosTag(SLP_type_previous);
			}
		}
		key = head + left + EOC + previousTag;
		condKey = head + left + previousTag;	
		keyCondKeyLog.add(key);
		
		previousTag = noChild;
		if (thisNode.rightDaughters!=null) {
			for(TDNode rd : thisNode.rightDaughters) {		
				String rdTag = rd.lexPosTag(SLP_type_daughter) + "_";
				key = head + right + rdTag + previousTag;
				condKey = head + right + previousTag;	
				keyCondKeyLog.add(key);
				updateKeyCondKeyLog(rd, keyCondKeyLog);
				previousTag = rd.lexPosTag(SLP_type_previous);		
			}
		}
		key = head + right + EOC + previousTag;
		condKey = head + right + previousTag;
		keyCondKeyLog.add(key);
	}
	
	@Override
	public int compareTo(Number a, Number b) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Number getScore(TDNode t) {
		// TODO Auto-generated method stub
		return null;
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
		boolean distance = false;
		boolean markTerminalNodes = true;
		int SLP_type_parent = 0;
		int SLP_type_daughter = 0;
		int SLP_type_previous = 0;
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
			"SLP_type_previous\t" + SLP_type_previous + "\n" +
			"Use LR direction\t" + LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerC(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_type_parent, SLP_type_daughter, SLP_type_previous, limitTestSetToFirst,
				addEOS, markTerminalNodes, LR, distance).runToyGrammar();
		
	}
	
	public static void mainTanl(int section, int nBest) {			
		TDNode.posAnalyzer = new TanlPos();
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;		
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		boolean includeGoldInNBest = false;
		int order = 2;
		String MSTver = "0.5";
		String corpusName = TanlD.TanlD_NoLoops_Name;
		
		String withGold = includeGoldInNBest ? "_withGold" : "";
		String mstVerOrderDir = "MST_" + MSTver + "_" + order + "order";
		String basePath = Parameters.resultsPath + "Reranker/Parsed/" +
							mstVerOrderDir + "/" + ((mxPos) ? "mxPOS/" : "goldPOS/") +
							corpusName + "/";							
		
		String outputPath = basePath  + corpusName + "_nBest" + nBest + "/";
						
		Parameters.outputPath = Parameters.resultsPath + "Reranker/" + FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		File trainingFile = new File(TanlD.TanlD_Train_MstUlab_NoLoops_EOS);
		File testFile = new File(TanlD.TanlD_Dev_MstUlab_NoLoops_EOS);
		String testFileName = FileUtil.getFileNameWithoutExtensions(testFile);
		String trainingFileName = FileUtil.getFileNameWithoutExtensions(trainingFile);
				
		File parsedFile = new File(outputPath + testFileName +
				"_LLtr" + LL_tr + "_LLts" + LL_ts + "_nBest" + nBest + withGold);		
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);
		//training = new ArrayList<TDNode> (training.subList(0, 100));
			
		File outputTestGold = new File(Parameters.outputPath + testFileName + ".gold.ulab");
		//File outputTtraining = new File(Parameters.outputPath + trainingFileName + ".ulab");
		DepCorpus.toFileMSTulab(outputTestGold, test, false);
		//DepCorpus.toFileMSTulab(outputTtraining, training, false);
		
		if(!parsedFile.exists()) {
			System.out.println("NBest file not found: " + parsedFile);
			return;
			//makeNBest(LL_tr, LL_ts, nBest, corpusName, trainingFile, 
			//		testFile, mxPos, order, mstVerOrderDir);
		}
		
		
		boolean addEOS = true;
		boolean LR = true;
		boolean distance = false;
		boolean markTerminalNodes = false;
		int SLP_type_parent = 0;
		int SLP_type_daughter = 0;
		int SLP_type_previous = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix			
		
		String parameters =
			"Eisner_C" + "\n" +
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
			"SLP_type_previous\t" + SLP_type_previous + "\n" +
			"Use LR direction\t" + LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerC(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_type_parent, SLP_type_daughter, SLP_type_previous, limitTestSetToFirst,
				addEOS, markTerminalNodes, LR, distance).reranking();
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
		
		Parameters.outputPath = parsedFileBase + "Reranker_EisnerC/" + //"Reranker/" + 
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
		boolean distance = false;
		boolean markTerminalNodes = false;
		int SLP_type_parent = 0;
		int SLP_type_daughter = 0;
		int SLP_type_previous = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix			
		
		String parameters =
			"Eisner_C" + "\n" +
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
			"SLP_type_previous\t" + SLP_type_previous + "\n" +
			"Use LR direction\t" + LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerC(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_type_parent, SLP_type_daughter, SLP_type_previous, limitTestSetToFirst,
				addEOS, markTerminalNodes, LR, distance).reranking();
		
		File rerankedFileConll = new File(Parameters.outputPath+ "reranked.ulab");		
		selectRerankedFromFileUlab(parsedFile, rerankedFileConll, Reranker.rerankedIndexes);
	}
	
	public static void main(String args[]) {
		
		int[] kBest = new int[]{2,3,4,5,6,7,8,9,10,100,1000}; //,15,20,25,50,150,200,250,500,750}; 
		for(int k : kBest) {
			mainWsj(22, k);
		}
		
	}

}
