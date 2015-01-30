package tsg.evalHeads;

import java.util.*;
import java.io.*;

import settings.Parameters;
import tsg.*;
import tsg.LTSG.LTSG_Entropy;
import tsg.LTSG.LTSG_Greedy;
import tsg.LTSG.LTSG_Naive;
import tsg.corpora.*;
import util.*;
import util.file.FileUtil;

public class negraEval {

	/*static void entropyGreedyTest() {
		int limit = -2;
		boolean cutTopRecursion = true,
				semanticTags = true,
				quotations = false,
				anonimizeLex = false,
				delexicalize = false,
				conversion = false,
				smoothing = false;
		int removeTreesLimit = -1;	
		int smoothingFactor = -1;
		
		String greedyType = "GreedyTop"; //GreedyTop GreedyBottom GreedyTopEntropy
		boolean punctuation = true;
		boolean random = true;
		
		String startingHeads = "Current"; //Current Random FirstLeft FirstRight
		int changes = -1;  
		int order = 0; //0:random, 1:biggest change first, 2: smallest change first
		int maxCycles = -1;
		double delta_threshold = 0.01;
		
		String outPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + greedyType + "/";	 		
		int[] LL = new int[] {10}; // 5,10,15,20,25,30,35,40,100
		for(int i=0; i<LL.length; i++) {
			int lengthLimit = LL[i];						
			String inputFile = "/home/fsangati/CORPUS/Negra/Binary/Binary_Train_NoTraces" + "_upto" + lengthLimit;
			File corpusFile = new File(inputFile);
			Corpus gold = Corpus.fromBinaryFile(corpusFile);
			LTSG_Greedy Grammar = new LTSG_Greedy(	outPath, corpusFile, lengthLimit, semanticTags, quotations, anonimizeLex,
													cutTopRecursion, delexicalize, conversion, removeTreesLimit, greedyType,
													punctuation, random, smoothing, smoothingFactor);
			Grammar.assignGreedyAnnotations(limit);		
			Grammar.readTreesFromCorpus();	
			Grammar.entropy();
			LTSG_Entropy Grammar1 = new LTSG_Entropy(outPath, corpusFile, lengthLimit, semanticTags, quotations, anonimizeLex,
									cutTopRecursion, delexicalize, conversion, removeTreesLimit, startingHeads,
									changes, order, maxCycles, delta_threshold, smoothing, smoothingFactor);			
			Grammar1.hillClimbing();
			int[] score = evalDependency(Grammar.corpus, Grammar1.corpus, limit);
			File EvalDepFile = new File (Grammar.outputPath + "EvalDepScore");
			Grammar.corpus.toFile_Complete(new File(Grammar.outputPath + "GreedyTopCorpus"), true, true);
			gold.toFile_Complete(new File(Grammar1.outputPath + "EntropyCorpus"), true, true);
			String line = 	"EVAL DEPENDENCY SCORE:\n" +
							"TOTAL\t" + score[0] + "\n" +
							"CORRECT\t" + score[1] + "\n" +
							"PERCENTAGE\t" + (float)score[1]/score[0];			
			FileUtil.append(line, EvalDepFile);
		}
	}*/

	static void entropyTest() {
		Parameters.corpusName = "Negra";
		Parameters.semanticTags = true;
		
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.startingHeads = LTSG_Entropy.Random; //Random Current FirstLeft FirstRight
		Parameters.maxNumberOfChanges = -1; //-1 = no limits  
		Parameters.orderOfChange = 2; //0:random, 1:biggest change first, 2: smallest change first
		Parameters.maxEntropyCycles = -1; //-1 = no limits  
		Parameters.entropy_delta_threshold = 0.01;
		
		Parameters.outputPath += "Entropy/";	
		
		int lengthLimit = 40; // 10 40
		String inputFile = 	Negra.NegraTrainingBinaryPath + "_upto" + lengthLimit;
		File trainingCorpusFile = new File(inputFile);
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.testCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
				
		LTSG_Entropy Grammar = new LTSG_Entropy();
		Grammar.hillClimbing();		
		int[] recall = EvalDependency.evalHeads(Parameters.trainingCorpus, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}

	static void cfgTest() {
		Parameters.corpusName = "Negra";
		Parameters.semanticTags = true;
		
		Parameters.outputPath += "CFG/";
		
		int lengthLimit = 40; // 10 40
		String inputFile = 	Negra.NegraTrainingBinaryPath + "_upto" + lengthLimit;
		File trainingCorpusFile = new File(inputFile);
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.testCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		//Parameters.lexiconTable = Parameters.trainingCorpus.buildLexFreq();	
						
		CFSG<Integer> Grammar = new CFSG<Integer>();
		boolean allowPunctuation = false;
		boolean onlyExternalChoices = false;
		Grammar.assignHeadAnnotations(allowPunctuation, onlyExternalChoices);			
		
		ConstCorpus negraSample = Parameters.trainingCorpus.returnFirst(200);
		File ouputFile = new File(Parameters.outputPath + "CFGHeads");
		negraSample.toFile_Complete(ouputFile, true);
		
		int[] recall = EvalDependency.evalHeads(Parameters.trainingCorpus, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		String report = "Total/Recall " + Arrays.toString(recall) + " " + ratio;
		FileUtil.appendReturn(report, Parameters.logFile);
		System.out.println(report);
	}
	
	static void greedyTest() {
		Parameters.corpusName = "Negra";
		Parameters.semanticTags = true;
		
		Parameters.spineConversion = true;
		Parameters.removeRedundencyInSpine = true;
		Parameters.posTagConversion = true;
		Parameters.jollyConversion = false;
		Parameters.jollyInclusion = false;
		Parameters.jollyLabels = new String[]{"NP"}; //",",":","-LRB-","-RRB-","-LCB-","-RCB-","JJ","RB","ADVP","ADJP"
		Arrays.sort(Parameters.jollyLabels);
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 2; //0random, 1left, 2right, 	
		
		Parameters.outputPath += Parameters.LTSGtype + "/";
		
		int lengthLimit = 40; // 10 40
		String inputFile = 	Negra.NegraTrainingBinaryPath + "_upto" + lengthLimit;
		File trainingCorpusFile = new File(inputFile);
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.testCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.lexiconTable = Parameters.trainingCorpus.buildLexFreq();	
						
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Grammar.assignGreedyAnnotations();		
		
		ConstCorpus negraSample = Parameters.trainingCorpus.returnFirst(200);
		File ouputFile = new File(Parameters.outputPath + "greedyHeads");
		negraSample.toFile_Complete(ouputFile, true);
		
		int[] recall = EvalDependency.evalHeads(Parameters.trainingCorpus, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		String report = "Total/Recall " + Arrays.toString(recall) + " " + ratio;
		FileUtil.appendReturn(report, Parameters.logFile);
		System.out.println(report);
	}

	/*static void intraEntropyTest() {
		boolean cutTopRecursion = true,
				semanticTags = true,
				quotations = false,
				anonimizeLex = false,
				delexicalize = false,
				conversion = false,
				smoothing = false;
		int removeTreesLimit = -1;
		int smoothingFactor = -1;
		
		String startingHeads = "Random"; //Current Random FirstLeft FirstRight
		int changes = 1;  
		int order = 2; //0:random, 1:biggest change first, 2: smallest change first
		int maxCycles = -1;
		double delta_threshold = 0.01;
		
		String outPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/Entropy/";		 		
		int[] LL = new int[] {10}; // 5,10,15,20,25,30,35,40,100
		for(int i=0; i<LL.length; i++) {
			int lengthLimit = LL[i];						
			String inputFile = "/home/fsangati/CORPUS/Negra/Binary/Binary_Train_NoTraces" + "_upto" + lengthLimit;
			File corpusFile = new File(inputFile);
			LTSG_Entropy Grammar = new LTSG_Entropy(outPath, corpusFile, lengthLimit, semanticTags, quotations, anonimizeLex,
												cutTopRecursion, delexicalize, conversion, removeTreesLimit, startingHeads,
												changes, order, maxCycles, delta_threshold, smoothing, smoothingFactor);
			Grammar.hillClimbing();
			Corpus corpus0 = Grammar.corpus.deepClone();
			Grammar.hillClimbing();		
			int[] score = evalDependency(corpus0, Grammar.corpus, -2);
			File EvalDepFile = new File (Grammar.outputPath + "EvalDepScore");
			corpus0.toFile_Complete(new File(Grammar.outputPath + "Entropy1Corpus"), true, true);
			Grammar.corpus.toFile_Complete(new File(Grammar.outputPath + "Entropy2Corpus"), true, true);
			String line = 	"EVAL DEPENDENCY SCORE:\n" +
							"TOTAL\t" + score[0] + "\n" +
							"CORRECT\t" + score[1] + "\n" +
							"PERCENTAGE\t" + (float)score[1]/score[0];			
			FileUtil.append(line, EvalDepFile);
		}
	}*/

	static void naiveTest() {
		Parameters.corpusName = "Negra";
		Parameters.semanticTags = true;
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.LTSGtype = LTSG_Naive.FirstRight; //Collins Random FirstLeft FirstRight
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/" + Parameters.LTSGtype + "/";	 
		
		int lengthLimit = 40; // 10 40
		String inputFile = 	Negra.NegraTrainingBinaryPath + "_upto" + lengthLimit;
		File trainingCorpusFile = new File(inputFile);
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.testCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
						
		LTSG_Naive Grammar = new LTSG_Naive();
		Grammar.assignNaiveAnnotations();		
		int[] recall = EvalDependency.evalHeads(Parameters.trainingCorpus, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);		
				
	}
	
	
	public static void reportGoldStatistics(int lengthLimit) {		
		File statisticFile = new File("/home/fsangati/CORPUS/Negra/negra_" 
				+ lengthLimit + "Heads_Statistic.txt");		
		
		File goldFile = 	new File(Negra.NegraTrainingBinaryPath + "_upto" + lengthLimit);
		ConstCorpus goldCorpus = ConstCorpus.fromBinaryFile(goldFile);
		File corpusOutput = new File("/home/fsangati/CORPUS/Negra/Heads/Negra_heads_upTo" + lengthLimit);
		goldCorpus.toFile_Complete(corpusOutput, true);
		goldCorpus.checkHeadAnnotationStatistics(statisticFile);		
	}

	
	public static void main(String[] args) {
		Parameters.outputPath = "/home/fsangati/PROJECTS/EvalHeads/Negra/";
		//reportGoldStatistics(10);
		//reportGoldStatistics(40);
		//entropyTest();
		//cfgTest();
		greedyTest();
		//naiveTest();
	}

}
