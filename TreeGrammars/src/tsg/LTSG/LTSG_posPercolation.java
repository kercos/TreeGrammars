package tsg.LTSG;

import java.io.*;

import settings.Parameters;
import tsg.*;
import tsg.corpora.*;
import tsg.parser.Parser;
import util.file.FileUtil;

public class LTSG_posPercolation extends LTSG {
	
	public static LTSG mainNaive() {
		Parameters.LTSGtype = LTSG_Naive.Collins97; //Collins Random FirstLeft FirstRight
		Parameters.outputPath += Parameters.LTSGtype + "/";		
		LTSG_Naive Grammar = new LTSG_Naive();		
		Grammar.assignNaiveAnnotations();				
		return Grammar;
	}
	

	public static LTSG mainEM() {	
		Parameters.LTSGtype = "EM"; 		
		Parameters.outputPath = Parameters.LTSGtype + "/";		
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
	
	public static LTSG mainGreedy() {
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 1;		
		Parameters.outputPath = Parameters.LTSGtype + "/";		
		Parameters.spineConversion = true;
		Parameters.posTagConversion = true;		
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Grammar.assignGreedyAnnotations();						
		return Grammar;
	}
	
	public static LTSG mainEntropy() {
		Parameters.LTSGtype = "Entropy";
		Parameters.outputPath = Parameters.LTSGtype + "/";		
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
		
		Parameters.setDefaultParam();	
		Parameters.outputPath = "/home/fsangati/PROJECTS/PosPercolation/";
		
		LTSG Grammar;		
		Grammar = mainNaive();
		//Grammar = mainEM();		
		//Grammar = mainGreedy();
		//Grammar = mainEntropy();
		
		Parameters.trainingCorpus.correctHeadAnnotation();
		//Parameters.trainingCorpus.percolatePosTagsInCorpus();
		Parameters.trainingCorpus.removeRedundantRules();
						
		Grammar.printTrainingCorpusToFile();
		Grammar.readTreesFromCorpus();
		Grammar.toPCFG();
		
		//((CFG)Grammar).readCFGFromCorpus();
		Grammar.printLexiconAndGrammarFiles();
		
		new Parser(Grammar);
	}
	

}
