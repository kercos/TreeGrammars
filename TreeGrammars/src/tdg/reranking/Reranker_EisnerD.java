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

public class Reranker_EisnerD extends Reranker_ProbModel {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	
	int SLP_type_parent, SLP_type_neighbour, SLP_type_daughter, SLP_type_previous; 
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	
	
	public Reranker_EisnerD(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int SLP_type_parent, int SLP_type_neighbour, int SLP_type_daughter, 
			int SLP_type_previous, int limitTestToFirst,
			boolean addEOS, boolean markTerminalNodes, boolean LR) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, markTerminalNodes, printTables, LR);
		
		this.SLP_type_parent = SLP_type_parent;
		this.SLP_type_neighbour = SLP_type_neighbour;
		this.SLP_type_daughter = SLP_type_daughter;
		this.SLP_type_previous = SLP_type_previous;
		
		updateCondFreqTables();	
	}
	
	/**
	 * Recursively increase the frequency of the current node and L/R children in the table.
	 * 
	 * The rules to be stored are: 
	 * - head + "_L_" + ld1
	 * - head + "_L_" + ld2
	 * ...
	 * - head + "_NULL_" + ld2
	 * ...
	 * - head + "_R_" + rd1
	 * - head + "_R_" + rd2 
	 * ...
	 * - head + "_R_" + "_NULL_"
	 * ...
	 * @param fragmentSet
	 * @param SLP_type
	 */
	public void updateCondFreqTables(TDNode thisNode) {
		TDNode[] structure = thisNode.getStructureArray();
		//BitSet chosenDaughters = new BitSet(structure.length);
		String key, condKey;		
		for(TDNode t : structure) {
			String headTag = t.lexPosTag(SLP_type_parent) + "_";
			String previous = noChild;
			int lastChosenDaughterIndex = t.index;
			if (t.leftDaughters!=null) {								
				for(int li = t.leftProle()-1; li>=0; li--) {									
					for(int l=0; l<lastChosenDaughterIndex;l++) {
						//if (chosenDaughters.get(l)) continue;
						TDNode lN = structure[l];
						String lNTag = lN.lexPosTag(SLP_type_neighbour) + "_";
						condKey = headTag + left + lNTag + previous;
						Utility.increaseInTableInteger(condFreqTable, condKey, 1);
					}
					condKey = headTag + left + EOC + previous;
					Utility.increaseInTableInteger(condFreqTable, condKey, 1);
					TDNode ld = t.leftDaughters[li];
					String ldTag = ld.lexPosTag(SLP_type_daughter) + "_";
					key = headTag + left + ldTag + previous;
					Utility.increaseInTableInteger(freqTable, key, 1);									
					previous = ld.lexPosTag(SLP_type_previous);
					lastChosenDaughterIndex = ld.index;
					//chosenDaughters.set(ld.index);
				}			
			}
			for(int l=0; l<lastChosenDaughterIndex;l++) {
				//if (chosenDaughters.get(l)) continue;
				TDNode lN = structure[l];
				String lNTag = lN.lexPosTag(SLP_type_neighbour) + "_";
				condKey = headTag + left + lNTag + previous;
				Utility.increaseInTableInteger(condFreqTable, condKey, 1);
			}
			key = headTag + left + EOC + previous;
			condKey = key;
			Utility.increaseInTableInteger(freqTable, key, 1);
			Utility.increaseInTableInteger(condFreqTable, condKey, 1);

			previous = noChild;
			lastChosenDaughterIndex = t.index;
			if (t.rightDaughters!=null) {				
				for(TDNode rd : t.rightDaughters) {								
					for(int r=lastChosenDaughterIndex+1; r<structure.length; r++) {
						//if (chosenDaughters.get(r)) continue;
						TDNode rN = structure[r];
						String rNTag = rN.lexPosTag(SLP_type_neighbour) + "_";
						condKey = headTag + right + rNTag + previous;
						Utility.increaseInTableInteger(condFreqTable, condKey, 1);
					}
					condKey = headTag + right + EOC + previous;
					Utility.increaseInTableInteger(condFreqTable, condKey, 1);
					String rdTag = rd.lexPosTag(SLP_type_daughter) + "_";
					key = headTag + right + rdTag + previous;
					Utility.increaseInTableInteger(freqTable, key, 1);														
					previous = rd.lexPosTag(SLP_type_previous);
					lastChosenDaughterIndex = rd.index;
					//chosenDaughters.set(rd.index);
				}
			}	
			for(int r=lastChosenDaughterIndex+1; r<structure.length; r++) {
				//if (chosenDaughters.get(r)) continue;
				TDNode rN = structure[r];
				String rNTag = rN.lexPosTag(SLP_type_neighbour) + "_";
				condKey = headTag + right + rNTag + previous;
				Utility.increaseInTableInteger(condFreqTable, condKey, 1);
			}
			key = headTag + right + EOC + previous;
			condKey = key;
			Utility.increaseInTableInteger(freqTable, key, 1);
			Utility.increaseInTableInteger(condFreqTable, condKey, 1);
		}		
	}
	
	
	/**
	 * Compute the probability of the current node rewriting to the specific set of children,
	 * conditioned on the current node. (Based on the tables obtained with 
	 * updateHeadLRRuleFreqTable and updateHeadFreqTable) 
	 * @param headLRFreqTable
	 * @param headFreqTable
	 * @param SLP_type
	 * @return
	 */
	public double getProb(TDNode thisNode) {
		
		double prob = 1f;
		
		String headTag = thisNode.lexPosTag(SLP_type_parent) + "_";
		String key, condKey;		
		String previousTag = noChild;
		if (thisNode.leftDaughters!=null) {
			for(int li = thisNode.leftProle()-1; li>=0; li--) {
				TDNode ld = thisNode.leftDaughters[li];
				String ldDTag = ld.lexPosTag(SLP_type_daughter) + "_";
				String ldNTag = ld.lexPosTag(SLP_type_neighbour) + "_";
				key = headTag + left + ldDTag + previousTag;
				condKey = headTag + left + ldNTag + previousTag;
				previousTag = ld.lexPosTag(SLP_type_previous);
				prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey)  * 
						getProb(ld);
			}						
		}
		key = headTag + left + EOC + previousTag;
		condKey = key;
		prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey);

		previousTag = noChild;
		if (thisNode.rightDaughters!=null) {
			for(TDNode rd : thisNode.rightDaughters) {		
				String rdDTag = rd.lexPosTag(SLP_type_daughter) + "_";
				String rdNTag = rd.lexPosTag(SLP_type_neighbour) + "_";
				key = headTag + right + rdDTag + previousTag;		
				condKey = headTag + right + rdNTag + previousTag;	
				previousTag = rd.lexPosTag(SLP_type_previous);
				prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey)  * 
						getProb(rd);						
			}
		}		
		key = headTag + right + EOC + previousTag;
		condKey = key;
		prob *= Utility.getCondProb(freqTable, condFreqTable, key, condKey);
		return prob;
	}
	
	public void updateKeyCondKeyLog(TDNode thisNode, Vector<String> keyCondKeyLog) {
		String headTag = thisNode.lexPosTag(SLP_type_parent) + "_";
		String key, condKey;				
		String previousTag = noChild;
		if (thisNode.leftDaughters!=null) {
			for(int li = thisNode.leftProle()-1; li>=0; li--) {
				TDNode ld = thisNode.leftDaughters[li];
				String ldDTag = ld.lexPosTag(SLP_type_daughter) + "_";
				String ldNTag = ld.lexPosTag(SLP_type_neighbour) + "_";
				key = headTag + left + ldDTag + previousTag;				
				condKey = headTag + left + ldNTag + previousTag;
				keyCondKeyLog.add(key + " | " + condKey);
				previousTag = ld.lexPosTag(SLP_type_previous);
				updateKeyCondKeyLog(ld, keyCondKeyLog);
			}			
			
		}
		key = headTag + left + EOC + previousTag;
		condKey = key;
		keyCondKeyLog.add(key + " | " + condKey);

		previousTag = noChild;
		if (thisNode.rightDaughters!=null) {
			for(TDNode rd : thisNode.rightDaughters) {		
				String rdDTag = rd.lexPosTag(SLP_type_daughter) + "_";
				String rdNTag = rd.lexPosTag(SLP_type_neighbour) + "_";
				key = headTag + right + rdDTag + previousTag;						
				condKey = headTag + right + rdNTag + previousTag;	
				keyCondKeyLog.add(key + " | " + condKey);
				previousTag = rd.lexPosTag(SLP_type_previous);	
				updateKeyCondKeyLog(rd, keyCondKeyLog);
			}
		}		
		key = headTag + right + EOC + previousTag;
		condKey = key;
		keyCondKeyLog.add(key + " | " + condKey);
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
		boolean markTerminalNodes = false;
		int SLP_type_parent = 0;
		int SLP_type_neighbour = 0;
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
			"SLP_type_neighbour\t" + SLP_type_neighbour + "\n" +
			"SLP_type_daughter\t" + SLP_type_daughter + "\n" +
			"SLP_type_previous\t" + SLP_type_previous + "\n" +
			"Use LR direction\t" + LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerD(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_type_parent, SLP_type_neighbour, SLP_type_daughter, SLP_type_previous, 
				limitTestSetToFirst, addEOS, markTerminalNodes, LR).runToyGrammar();
		
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
		
		Parameters.outputPath = parsedFileBase + "Reranker_EisnerD/" + //"Reranker/" + 
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
		int SLP_type_neighbour = 0;
		int SLP_type_daughter = 0;
		int SLP_type_previous = 0;
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
				
		
		String parameters =
			"Eisner_D" + "\n" +
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
			"SLP_type_neighbour\t" + SLP_type_neighbour + "\n" +
			"SLP_type_daughter\t" + SLP_type_daughter + "\n" +
			"SLP_type_previous\t" + SLP_type_previous + "\n" +
			"Use LR direction\t" + LR + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerD(outputTestGold, parsedFile, nBest, training, uk_limit, 
				SLP_type_parent, SLP_type_neighbour, SLP_type_daughter, SLP_type_previous, 
				limitTestSetToFirst, addEOS, markTerminalNodes, LR).reranking();
		
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
