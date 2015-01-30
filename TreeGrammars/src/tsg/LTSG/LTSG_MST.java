package tsg.LTSG;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.CFSG;
import tsg.TSNode;
import tsg.corpora.*;
import util.*;
import util.file.FileUtil;


public class LTSG_MST extends LTSG {
		
	public static LTSG mainNaive(int n) {
		switch(n) {
		case 0: Parameters.LTSGtype = LTSG_Naive.Collins97;
				break;
		case 1: Parameters.LTSGtype = LTSG_Naive.Random;
				break;
		case 2: Parameters.LTSGtype = LTSG_Naive.FirstLeft;
				break;
		case 3: Parameters.LTSGtype = LTSG_Naive.FirstRight;
				break;
		}
		 //Collins Random FirstLeft FirstRight
		Parameters.outputPath = Parameters.resultsPath + "MST/" + Parameters.LTSGtype + "/";		
		LTSG_Naive Grammar = new LTSG_Naive();		
		Grammar.assignNaiveAnnotations();				
		return Grammar;
	}
	

	public static LTSG mainEM() {	
		Parameters.LTSGtype = "EM"; 		
		Parameters.outputPath = Parameters.resultsPath + "MST/" + Parameters.LTSGtype + "/";		
		Parameters.EM_initialization = LTSG_EM.initializeUNIFORM; // initializeUNIFORM initializeDOP
		Parameters.EM_nBest = -1;
		Parameters.EM_deltaThreshold = 1;
		Parameters.EM_maxCycle = 10; //1 Integer.MAX_VALUE;				
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;		
		LTSG_EM Grammar = new LTSG_EM();
		Grammar.EMalgorithm();
		//Grammar.EMHeldOutAlgorithm();
		return Grammar;				
	}
	
	public static void mainCFG() {
		Parameters.outputPath = Parameters.resultsPath + "MST/CFG/";		 		
		CFSG<Integer> Grammar = new CFSG<Integer>();
		boolean allowPunctuation = false;
		boolean onlyExternalChoices = false;
		Grammar.assignHeadAnnotations(allowPunctuation, onlyExternalChoices);	
	}
	
	public static LTSG mainGreedy() {
		Parameters.spineConversion = false;
		Parameters.removeRedundencyInSpine = false;
		Parameters.posTagConversion = true;
		Parameters.jollyConversion = false;
		Parameters.jollyInclusion = false;
		Parameters.jollyLabels = new String[]{"NP"}; //",",":","-LRB-","-RRB-","-LCB-","-RCB-","JJ","RB","ADVP","ADJP"
		Arrays.sort(Parameters.jollyLabels);
		
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 2; //0random, 1left, 2right, 3backoffLex
		
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.outputPath = Parameters.resultsPath + "MST/" + Parameters.LTSGtype + "/";		
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Grammar.assignGreedyAnnotations();						
		return Grammar;
	}
	
	public static LTSG mainEntropy() {
		Parameters.LTSGtype = "Entropy";
		Parameters.outputPath = Parameters.resultsPath + "MST/" + Parameters.LTSGtype + "/";		
		Parameters.startingHeads = LTSG_Entropy.Random; //Random Current FirstLeft FirstRight
		Parameters.maxNumberOfChanges = -1; //-1 = no limits  
		Parameters.orderOfChange = 2; //0:random, 1:biggest change first, 2: smallest change first
		Parameters.maxEntropyCycles = -1; //-1 = no limits  
		Parameters.entropy_delta_threshold = 0.01;
		Parameters.spineConversion = false;
		Parameters.posTagConversion = false;		
		LTSG_Entropy Grammar = new LTSG_Entropy();
		Grammar.hillClimbing();		
		return Grammar;
	}

	
	public static void main(String args[]) {
		
		ConstCorpus wsj_02_11 = new ConstCorpus(new File(Parameters.corpusPath + "WSJ/MALT/penn.02-11.heads"),"noProcess");
		ConstCorpus wsj_22 = new ConstCorpus(new File(Parameters.corpusPath + "WSJ/MALT/penn.22.heads"),"noProcess");
		
		wsj_02_11.removeTreesLongerThan(10, Wsj.nonCountCatInLength);
		wsj_22.removeTreesLongerThan(10, Wsj.nonCountCatInLength);
		
		ConstCorpus complete = wsj_02_11.deepClone();
		complete.treeBank.addAll(wsj_22.treeBank);
		int wsj_02_11_size = wsj_02_11.size();
		int wsj_22_size = wsj_22.size();
		
		Parameters.trainingCorpus = complete;
		Parameters.corpusName = "noProcess";
		
		mainNaive(0);
		//mainGreedy();
		//mainEntropy();
		//mainCFG();
		
		wsj_02_11 = complete.returnFirst(wsj_02_11_size);
		wsj_22 = complete.returnLast(wsj_22_size);
		
		wsj_02_11.toFile_Complete(new File(Parameters.outputPath + "penn.02-11.heads"), true);
		wsj_22.toFile_Complete(new File(Parameters.outputPath + "penn.22.heads"), true);
		
		boolean labels = true;
		
		util.ConstDepConverter.printMSTOutput(wsj_02_11, new File(Parameters.outputPath + "MST.02-11.lab"),false, labels);
		util.ConstDepConverter.printMSTOutput(wsj_22, new File(Parameters.outputPath + "MST.22.gold.lab"), false, labels);
		util.ConstDepConverter.printMSTOutput(wsj_22, new File(Parameters.outputPath + "MST.22.test"), true, labels);		
		
		String[] argsTrainTestEval = {
			"train", 
			"train-file:" + Parameters.outputPath + "MST.02-11.lab",
			"model-name:" + Parameters.outputPath + "dep.model",
			"training-iterations:" + 10,
			"decode-type:" + "proj",
			"training-k:" + 1,
			"loss-type:" + "punc",
			"create-forest:" + "true",
			"order:" + 1,
			
			"test",
			"test-file:" + Parameters.outputPath + "MST.22.test",
			"model-name:" + Parameters.outputPath + "dep.model",
			"output-file:" + Parameters.outputPath + "MST.22.parsed.lab",
			"decode-type:" + "proj",
			"order:" + 1,
			
			"eval",
			"gold-file:" + Parameters.outputPath + "MST.22.gold.lab",
			"output-file:" + Parameters.outputPath + "MST.22.parsed.lab"

		};		
						
		try{
		mstparser.DependencyParser.main(argsTrainTestEval);
		} catch (Exception e) {FileUtil.handleExceptions(e);}		

	}
	

}
