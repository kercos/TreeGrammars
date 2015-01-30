package tsg.evalHeads;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.LTSG.*;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Negra;
import tsg.*;
import util.*;
import util.file.FileUtil;

public class tigerEval {
	
	public static String basePath = Parameters.corpusPath + "Tiger/";
	public static String gbaseDB = basePath + "Tiger_DB/tigerDB_Aug07/";
	public static String gbaseTB = basePath + "Tiger_TB/Jul03/";
	public static File tigerDBGoldFile = new File(gbaseDB + "tigerDB_Aug07_heads.mrg");
	public static File indexFile = new File(gbaseDB + "indexes.txt");
	public static File tigerTBCorpusFile = new File(gbaseTB + "tiger_release_july03.penn");
	
	public static int[] readIndexes() {
		String line = "";
		try {
			Scanner scan = new Scanner(indexFile);			
			while ( scan.hasNextLine()) line += scan.nextLine();														
			scan.close();
		} catch (IOException e) {FileUtil.handleExceptions(e);}
		line = line.trim();
		String[] indexeStrings = line.split(", ");
		int[] result = new int[indexeStrings.length];
		for(int i=0; i<indexeStrings.length; i++) {
			result[i] = Integer.parseInt(indexeStrings[i]);
		}
		return result;
	}
	
	public static void tigerVsGold() {		
		File trainingCorpusFile = new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_binary_complete");
		File testComplete = new File("/home/fsangati/CORPUS/Tiger/tigerDB/tigerDB_Heads_complete"); 
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.testCorpus = new ConstCorpus(testComplete, "TigerDB");
						
		ConstCorpus tigerEval = Parameters.trainingCorpus.returnIndexes(readIndexes());		
		tigerEval.removeDoubleQuoteTiger();
		File toFile = new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_extractTigerDB");
		tigerEval.toFile_Complete(toFile, true);
		int[] recall = EvalDependency.evalHeads(tigerEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}

	public static void cfgVsGold() {
		Parameters.corpusName = "Tiger";
		Parameters.semanticTags = true;
		
		Parameters.outputPath += "CFG/";
				
		
		File trainingCorpusFile = new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_binary_complete");
		File testComplete = new File("/home/fsangati/CORPUS/Tiger/tigerDB/tigerDB_Heads_complete"); 
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.testCorpus = new ConstCorpus(testComplete, "TigerDB");
		Parameters.trainingCorpus.removeDoubleQuoteTiger();
		Parameters.trainingCorpus.removeHeadAnnotations();
		//Parameters.lexiconTable = Parameters.trainingCorpus.buildLexFreq();	
		
		CFSG<Integer> Grammar = new CFSG<Integer>();
		boolean allowPunctuation = false;
		boolean onlyExternalChoices = false;
		Grammar.assignHeadAnnotations(allowPunctuation, onlyExternalChoices);	
		ConstCorpus tigerEval = Parameters.trainingCorpus.returnIndexes(readIndexes());
		
		File ouputFile = new File(Parameters.outputPath + "CFGHeads");
		tigerEval.toFile_Complete(ouputFile, true);
		
		int[] recall = EvalDependency.evalHeads(tigerEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		String report = "EvalHead:\n" + "Total/Recall " + Arrays.toString(recall) + " " + ratio;
		FileUtil.appendReturn(report, Parameters.logFile);
		System.out.println(report);
	}
	
	public static void greedyVsGold() {
		Parameters.corpusName = "Tiger";
		Parameters.semanticTags = true;
		
		Parameters.spineConversion = false;
		Parameters.removeRedundencyInSpine = false;
		Parameters.posTagConversion = false;
		
		Parameters.jollyConversion = false;
		Parameters.jollyInclusion = false;
		Parameters.jollyLabels = new String[]{"NP"}; //",",":","-LRB-","-RRB-","-LCB-","-RCB-","JJ","RB","ADVP","ADJP"
		Arrays.sort(Parameters.jollyLabels);
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 2; //0random, 1left, 2right, 3BackoffLex
		
		Parameters.outputPath += Parameters.LTSGtype + "/";
				
		
		File trainingCorpusFile = new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_binary_complete");
		File testComplete = new File("/home/fsangati/CORPUS/Tiger/tigerDB/tigerDB_Heads_complete"); 
		Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingCorpusFile);
		Parameters.testCorpus = new ConstCorpus(testComplete, "TigerDB");
		Parameters.trainingCorpus.removeDoubleQuoteTiger();
		Parameters.lexiconTable = Parameters.trainingCorpus.buildLexFreq();	
		
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Grammar.assignGreedyAnnotations();	
		ConstCorpus tigerEval = Parameters.trainingCorpus.returnIndexes(readIndexes());
		
		File ouputFile = new File(Parameters.outputPath + "greedyHeads");
		tigerEval.toFile_Complete(ouputFile, true);
		
		int[] recall = EvalDependency.evalHeads(tigerEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		String report = "EvalHead:\n" + "Total/Recall " + Arrays.toString(recall) + " " + ratio;
		FileUtil.appendReturn(report, Parameters.logFile);
		System.out.println(report);
	}
	
	public static void entropyVsGold() {
		Parameters.corpusName = "Tiger";
		Parameters.semanticTags = true;
		
		Parameters.posTagConversion = true;
		Parameters.spineConversion = true;
		
		Parameters.removeTreesLimit = -1;
		
		Parameters.startingHeads = LTSG_Entropy.Random; //Random Current FirstLeft FirstRight
		Parameters.maxNumberOfChanges = -1; //-1 = no limits  
		Parameters.orderOfChange = 2; //0:random, 1:biggest change first, 2: smallest change first
		Parameters.maxEntropyCycles = -1; //-1 = no limits  
		Parameters.entropy_delta_threshold = 0.01;
		
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/LTSG/Entropy/";	
		
		File trainingComplete = new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_complete");
		File testComplete = new File("/home/fsangati/CORPUS/Tiger/tigerDB/tigerDB_Heads_complete"); 
		Parameters.trainingCorpus = new ConstCorpus(trainingComplete, "TigerDB");
		Parameters.testCorpus = new ConstCorpus(testComplete, "TigerDB");
		Parameters.trainingCorpus.removeDoubleQuoteTiger();
		//Parameters.trainingCorpus.toFile_Complete(new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_complete"), true);

		LTSG_Entropy Grammar = new LTSG_Entropy();
		Grammar.hillClimbing();		
		ConstCorpus tigerEval = Parameters.trainingCorpus.returnIndexes(readIndexes());
		int[] recall = EvalDependency.evalHeads(tigerEval, Parameters.testCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	public static void naiveVsGold() {
		File tigerGoldFile =  new File("/home/fsangati/CORPUS/Tiger/tigerDB/tigerDB_Heads_complete");
		ConstCorpus tigerGoldCorpus = new ConstCorpus(tigerGoldFile, "TigerDB");
		ConstCorpus tigerNaiveCorpus = new ConstCorpus(tigerGoldFile, "TigerDB");
		tigerNaiveCorpus.assignRandomHeads();
		//tigerNaiveCorpus.assignFirstLeftHeads();
		//tigerNaiveCorpus.assignFirstRightHeads();
		//parcNaiveCorpus.toFile_Complete(new File("/home/fsangati/CORPUS/PARC/parc700_firstRight"), true, true);
		int[] recall = EvalDependency.evalHeads(tigerNaiveCorpus, tigerGoldCorpus, -2);
		float ratio = (float)recall[1]/recall[0];
		System.out.println("Total/Recall " + Arrays.toString(recall) + " " + ratio);
	}
	
	public static void reportGoldStatistics() {
		//File statisticFile = new File("/scratch/fsangati/CORPUS/Tiger/Tiger_DB/tigerDB_Aug07/tigerDB_Aug07_heads_Statistic.txt");
		//File goldFile =  new File("/scratch/fsangati/CORPUS/Tiger/Tiger_DB/tigerDB_Aug07/tigerDB_Aug07_heads.mrg");
		File statisticFile = new File("/scratch/fsangati/CORPUS/Tiger/Tiger_DB/tigerDB_Apr08/tigerDB_Apr08_heads_Statistic.txt");
		File goldFile =  new File("/scratch/fsangati/CORPUS/Tiger/Tiger_DB/tigerDB_Apr08/tigerDB_Apr08_heads.mrg");
		ConstCorpus goldCorpus = new ConstCorpus(goldFile, "tigerGold");
		goldCorpus.checkHeadAnnotationStatistics(statisticFile);
	}
	
	public static void reportTigerStatistics() {
		File statisticFile = new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_evalStatistics.txt");
		File tigerFile =  new File("/home/fsangati/CORPUS/Tiger/Complete/tiger_binary_complete");
		ConstCorpus tigerCorpus = ConstCorpus.fromBinaryFile(tigerFile);
		ConstCorpus tigerEval = tigerCorpus.returnIndexes(readIndexes());
		tigerEval.removeDoubleQuoteTiger();
		tigerEval.checkHeadAnnotationStatistics(statisticFile);
	}
	
	
	
	public static void main(String[] args) {
		//Parameters.outputPath = "/home/fsangati/PROJECTS/EvalHeads/Tiger/";
		
		reportGoldStatistics();
		//reportTigerStatistics();
		//cfgVsGold();
		//tigerVsGold();
		//naiveVsGold();
		//greedyVsGold();
		//entropyVsGold();
	}
	
}
