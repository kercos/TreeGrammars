package tsg.LTSG;

import java.io.*;

import settings.Parameters;
import tsg.CFSG;
import tsg.TSNode;
import tsg.corpora.*;
import tsg.parseEval.EvalC;
import tsg.parser.Parser;
import util.file.FileUtil;


public class LTSG_ShortTrain extends LTSG {
	
	static float trainingFraction;
	
	public static LTSG mainNaive() {
		Parameters.LTSGtype = LTSG_Naive.FirstLeft; //Collins Random FirstLeft FirstRight
		Parameters.outputPath = "/home/fsangati/PROJECTS/SmallTrain/" + Parameters.LTSGtype + "/";		
		LTSG_Naive Grammar = new LTSG_Naive();		
		Parameters.trainingCorpus.keepRandomFraction(trainingFraction);
		Grammar.assignNaiveAnnotations();	
		Grammar.readTreesFromCorpus();
		return Grammar;
	}
	
	
	public static LTSG mainGreedy() {
		Parameters.LTSGtype = LTSG_Greedy.GreedyBottom; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 0;		
		Parameters.outputPath = "/home/fsangati/PROJECTS/SmallTrain/" + Parameters.LTSGtype + "/";		
		Parameters.spineConversion = true;
		Parameters.posTagConversion = true;		
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Parameters.trainingCorpus.keepRandomFraction(trainingFraction);
		Grammar.assignGreedyAnnotations();		
		Grammar.readTreesFromCorpus();
		return Grammar;
	}
	
	public static LTSG mainEntropy() {
		Parameters.LTSGtype = "Entropy";
		Parameters.outputPath = "/home/fsangati/PROJECTS/SmallTrain/" + Parameters.LTSGtype + "/";		
		Parameters.startingHeads = LTSG_Entropy.Random; //Random Current FirstLeft FirstRight
		Parameters.maxNumberOfChanges = -1; //-1 = no limits  
		Parameters.orderOfChange = 2; //0:random, 1:biggest change first, 2: smallest change first
		Parameters.maxEntropyCycles = -1; //-1 = no limits  
		Parameters.entropy_delta_threshold = 0.01;
		Parameters.spineConversion = true;
		Parameters.posTagConversion = true;		
		LTSG_Entropy Grammar = new LTSG_Entropy();
		Parameters.trainingCorpus.keepRandomFraction(trainingFraction);
		Grammar.hillClimbing();		
		Grammar.readTreesFromCorpus();
		return Grammar;
	}
	
	public static void setDefaultParam() {
		Parameters.corpusName = "Wsj"; //Wsj, Negra, Parc, Tiger, Wsj 
		Parameters.lengthLimitTraining = 20;
		Parameters.lengthLimitTest = 20;
		EvalF.EvalC = 20;
		
		Wsj.testSet = "22"; //00 01 22 23 24
		Wsj.skip120TrainingSentences = false;		
		
		Parameters.semanticTags = true;
		Parameters.replaceNumbers = false;
		Parameters.ukLimit = 4;
		
		Parameters.cutTopRecursion = false;
		Parameters.delexicalize = false;					
		Parameters.removeTreesLimit = -1;		
		Parameters.smoothing = true;
		Parameters.smoothingFactor = 100;
		
		Wsj.transformNPbasal = false;
		Wsj.transformSG = false;		
		
		Parameters.parserName = Parser.bitPar;
		Parameters.nBest = 1;		
	}
	
	public static void mainLTSG() {
		trainingFraction = 0.1f;
		setDefaultParam();
		LTSG Grammar;		
		//Grammar = mainNaive();
		//Grammar = mainEM();		
		//Grammar = mainEMHeldOut();
		Grammar = mainGreedy();
		//Grammar = mainEntropy();
		
		Grammar.printTemplatesToFile();
		
		Grammar.treatTreeBank();
		Grammar.toPCFG();

		Grammar.printTrainingCorpusToFile();				
		Grammar.printLexiconAndGrammarFiles();		
		
		new Parser(Grammar);
	}
	
	public static void mainCFG() {
		trainingFraction = 0.1f;
		setDefaultParam();
		Parameters.outputPath = "/home/fsangati/PROJECTS/SmallTrain/CFG/";		
		CFSG<Integer> Grammar = new CFSG<Integer>();
		Parameters.trainingCorpus.keepRandomFraction(trainingFraction);
		Grammar.readCFGFromCorpus();	
		Grammar.printTrainingCorpusToFile();				
		Grammar.printLexiconAndGrammarFiles();				
		new Parser(Grammar);		
	}
	
	public static void main(String args[]) {
		mainLTSG();
		//mainCFG();
	}
}
