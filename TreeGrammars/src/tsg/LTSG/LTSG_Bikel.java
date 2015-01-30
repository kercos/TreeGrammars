package tsg.LTSG;

import java.io.*;
import java.util.Arrays;




import settings.Parameters;
import tsg.CFSG;
import tsg.TSNode;
import tsg.corpora.*;
import tsg.evalHeads.EvalDependency;
import tsg.parseEval.*;
import util.file.FileUtil;
import danbikel.parser.Trainer;
import danbikel.parser.Parser;
import danbikel.parser.english.Training;


public class LTSG_Bikel extends LTSG {
	
	public static void mainCFG() {
		Parameters.outputPath = "/home/fsangati/PROJECTS/Bikel/CFG/";		 		
		CFSG<Integer> Grammar = new CFSG<Integer>();
		boolean allowPunctuation = false;
		boolean onlyExternalChoices = true;
		Grammar.assignHeadAnnotations(allowPunctuation, onlyExternalChoices);	
	}
	
	public static LTSG mainNaive() {
		Parameters.LTSGtype = LTSG_Naive.Collins97; //Magerman Collins97 Collins99 Random FirstLeft FirstRight
		LTSG_Naive.setStartingHead();
		Parameters.outputPath = Parameters.resultsPath + "Bikel/" + Parameters.LTSGtype + "/";		
		LTSG_Naive Grammar = new LTSG_Naive();		
		Grammar.assignNaiveAnnotations();				
		return Grammar;
	}
	
	public static LTSG extensiveRandom() {
		Parameters.LTSGtype = LTSG_Naive.Random; //Collins Random FirstLeft FirstRight
		Parameters.outputPath = Parameters.resultsPath + "Bikel/" + Parameters.LTSGtype + "/";		
		LTSG_Naive Grammar = new LTSG_Naive();	
		Parameters.trainingCorpus.multiplyCorpus(10);
		Grammar.assignNaiveAnnotations();				
		return Grammar;
	}
	
	public static LTSG mainEM() {	
		Parameters.LTSGtype = "EM"; 		
		Parameters.outputPath = Parameters.resultsPath + "Bikel/" + Parameters.LTSGtype + "/";		
		Parameters.EM_initialization = LTSG_EM.initializeUNIFORM; // initializeUNIFORM initializeDOP
		Parameters.EM_nBest = -1;
		Parameters.EM_deltaThreshold = 0.1;
		Parameters.EM_maxCycle = 1; //Integer.MAX_VALUE;				
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;		
		LTSG_EM Grammar = new LTSG_EM();
		Grammar.EMalgorithm();
		return Grammar;				
	}
	
	public static LTSG mainEMHeldOut() {	
		Parameters.LTSGtype = "EM"; 		
		Parameters.outputPath = Parameters.resultsPath + "Bikel/" + Parameters.LTSGtype + "/";		
		Parameters.EM_initialization = LTSG_EM.initializeUNIFORM; // initializeUNIFORM initializeDOP
		Parameters.EM_nBest = -1;
		Parameters.EM_deltaThreshold = 0.1;
		Parameters.EM_maxCycle = Integer.MAX_VALUE;				
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;		
		LTSG_EM Grammar = new LTSG_EM();
		Grammar.EMHeldOutAlgorithm();
		return Grammar;				
	}
	
	public static LTSG mainGreedy() {		
		Parameters.spineConversion = false;
		Parameters.removeRedundencyInSpine = true;
		Parameters.posTagConversion = false;
		Parameters.jollyConversion = false;
		Parameters.jollyInclusion = false;
		Parameters.jollyLabels = new String[]{"NP"}; //",",":","-LRB-","-RRB-","-LCB-","-RCB-","JJ","RB","ADVP","ADJP"
		Arrays.sort(Parameters.jollyLabels);
		
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 2; //0random, 1left, 2right, 3backoffLex
		
		
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.outputPath = Parameters.resultsPath + "Bikel/" + Parameters.LTSGtype + "/";		
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Grammar.assignGreedyAnnotations();						
		return Grammar;
	}
	
	public static LTSG mainEntropy() {
		Parameters.LTSGtype = "Entropy";
		Parameters.outputPath = Parameters.resultsPath + "Bikel/" + Parameters.LTSGtype + "/";		
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
	
	public static void makeTestWithParenthesis(File outputFile) {						
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		for(TSNode inputTree : Parameters.testCorpus.treeBank) {
			String line = inputTree.toFlat();
			out.write("(" + line + ")\n");
		}
		out.close();	
	}
	
	public static void setDefaultParam() {
		Parameters.corpusName = "noProcess"; //Wsj, Negra, Parc, Tiger, Wsj 
		Parameters.trainingCorpus =  new ConstCorpus(new File(Wsj.WsjOriginalBikelPreprocessed + "wsj-02-21.mrg"),"noProcess");
		Parameters.lengthLimitTraining = 20;
		Parameters.lengthLimitTest = 20;
		EvalF.EvalC = 20;
		Parameters.traces = true;
		Wsj.testSet = "22"; //00 01 22 23 24				
		Parameters.semanticTags = true;

		// do all the processing of the training and test corpus here
	}
	
	public static void main(String args[]) {
		setDefaultParam();
		
		//mainCFG();
		//mainNaive();
		//extensiveRandom();
		//mainEM();		
		//mainEMHeldOut();
		//mainGreedy();
		//mainEntropy();
		
		//Parameters.trainingCorpus.correctHeadAnnotation();
		//Parameters.trainingCorpus.removeTop();
		//Parameters.testCorpus.removeTop();
		
		//Parameters.trainingCorpus.removeHeadAnnotations();
		
		String trainingComplete = Parameters.outputPath + "TrainingCorpus.processed";
		String outputFile = Parameters.outputPath + "test.parsed";
		File testParenthesis = new File(Parameters.outputPath + "test.parethesis");		
		String goldFile = Parameters.outputPath + "test.gold";
		String evalF = Parameters.outputPath + "test.evalF";
		String evalFLog = Parameters.outputPath + "log.evalF";
		String evalF_UL = Parameters.outputPath + "test.evalF.UL";
		String evalB = Parameters.outputPath + "test.evalB";
		String evalB_UL = Parameters.outputPath + "test.evalB.UL";
		
		for(TSNode treeLine : Parameters.testCorpus.treeBank) {
			treeLine.pruneSubTrees(Wsj.nonNecessaryLabels); // remove trees rooted on |``|''|.|
			treeLine.pruneSubTrees(Wsj.traceTag);
			treeLine.removeNumberInLabels();
			treeLine.prunePunctuationBeginning(); // remove punctuation at the beginning of the tree
			treeLine.prunePunctuationEnd(); // remove punctuation at the end of the tree
			treeLine.raisePunctuation();
			treeLine.removeRedundantRules();
		}
		
		//Parameters.trainingCorpus.removeHeadAnnotations();
		Parameters.testCorpus.removeHeadAnnotations();
		
		Parameters.trainingCorpus.toFile_Complete(new File(trainingComplete), true, false);
		Parameters.testCorpus.toFile_Complete(new File(goldFile), true);
		makeTestWithParenthesis(testParenthesis);
		
		//String settingFile = "/home/fsangati/Code/Bikel/settings/collins.properties";
		//String settingFile = "/home/fsangati/Code/Bikel/settings/bikel.properties";
		//String settingFile = "/home/fsangati/Code/Bikel/settings/fede.properties";
		//String settingFile = "/home/fsangati/Code/Bikel/settings/collins-no-skip.properties";
		
		
		Training.writeCorpus = FileUtil.getPrintWriter(
				new File(Parameters.outputPath + "Training.bikelPreprocessed"));
		
		String[] argsTrainer = {
				"-i", trainingComplete, 
				"-o", Parameters.outputPath + "wsj-02-21.observed.gz", 
				"-od", Parameters.outputPath + "wsj-02-21.obj.gz"
		};		
		
		String[] argsParser = {
				"-is", Parameters.outputPath + "wsj-02-21.obj.gz", 
				"-sa", testParenthesis.getPath(),
				"-out", outputFile
		};

		Trainer.main(argsTrainer);
		Training.writeCorpus.close();
		Parser.main(argsParser);		
		new EvalC(new File(goldFile), new File(outputFile), new File(evalF), new File(evalFLog), true);
		new EvalB(new File(goldFile), new File(outputFile), new File(evalB), true);
		new EvalC(new File(goldFile), new File(outputFile), new File(evalF_UL), false);
		new EvalB(new File(goldFile), new File(outputFile), new File(evalB_UL), false);
	}
}
