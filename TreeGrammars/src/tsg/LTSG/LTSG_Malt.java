package tsg.LTSG;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.CFSG;
import tsg.TSNode;
import tsg.corpora.*;
import util.*;


public class LTSG_Malt extends LTSG {
			
	public static LTSG mainNaive() {
		Parameters.LTSGtype = LTSG_Naive.FirstLeft; //Collins Random FirstLeft FirstRight
		Parameters.outputPath = "/home/fsangati/PROJECTS/Malt/" + Parameters.LTSGtype + "/";		
		LTSG_Naive Grammar = new LTSG_Naive();		
		Grammar.assignNaiveAnnotations();				
		return Grammar;
	}
	

	public static LTSG mainEM() {	
		Parameters.LTSGtype = "EM"; 		
		Parameters.outputPath = "/home/fsangati/PROJECTS/Malt/" + Parameters.LTSGtype + "/";		
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
		Parameters.outputPath = "/home/fsangati/PROJECTS/Malt/CFG/";		 		
		CFSG<Integer> Grammar = new CFSG<Integer>();
		boolean allowPunctuation = false;
		boolean onlyExternalChoices = false;
		Grammar.assignHeadAnnotations(allowPunctuation, onlyExternalChoices);	
	}
	
	public static LTSG mainGreedy() {
		Parameters.spineConversion = true;
		Parameters.removeRedundencyInSpine = true;
		Parameters.posTagConversion = true;
		Parameters.jollyConversion = false;
		Parameters.jollyInclusion = false;
		Parameters.jollyLabels = new String[]{"NP"}; //",",":","-LRB-","-RRB-","-LCB-","-RCB-","JJ","RB","ADVP","ADJP"
		Arrays.sort(Parameters.jollyLabels);
		
		Parameters.greedy_punctuation = false;
		Parameters.greedy_ambiguityChoice = 2; //0random, 1left, 2right, 3backoffLex
		
		Parameters.LTSGtype = LTSG_Greedy.GreedyTop; //GreedyTop GreedyBottom GreedyTopEntropy
		Parameters.outputPath = "/home/fsangati/PROJECTS/Malt/" + Parameters.LTSGtype + "/";		
		LTSG_Greedy Grammar = new LTSG_Greedy();
		Grammar.assignGreedyAnnotations();						
		return Grammar;
	}
	
	public static LTSG mainEntropy() {
		Parameters.LTSGtype = "Entropy";
		Parameters.outputPath = "/home/fsangati/PROJECTS/Malt/" + Parameters.LTSGtype + "/";		
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
	
	public static void smallTry(String args[]) {
		
		String path = "maltTest_1.6/";
		
		String[] argsTrainer = {
				"-w", path,				
				"-c", "test",				
				"-if", "malttab",
				"-of", "malttab",
				"-i", path + "input.tab",
				"-lsx", "/home/fsangati/SOFTWARE/libsvm-2.86/svm-train", 
				"-m", "learn",
				"-d", "POSTAG", 
				"-s", "Input[0]", 
				"-T", "100" 
		};	
		
		String[] argsParser = {	
				"-w", path,
				"-c", "test",
				"-i", path + "test.tab",
				"-o", path + "test.out",
				"-of", "malttab",
				"-m", "parse"
		};
		
		
		org.maltparser.Malt.main(argsTrainer);
		org.maltparser.Malt.main(argsParser);				

	}
	
	public static void main(String args[]) {														  
		ConstCorpus wsj_02_11 = new ConstCorpus(new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg"),"noProcess");
		ConstCorpus wsj_22 = new ConstCorpus(new File(Wsj.WsjOriginalCleaned + "wsj-22.mrg"),"noProcess");
		ConstCorpus complete = wsj_02_11.deepClone();
		complete.treeBank.addAll(wsj_22.treeBank);
		int wsj_02_11_size = wsj_02_11.size();
		int wsj_22_size = wsj_22.size();
		
		Parameters.trainingCorpus = complete;
		Parameters.corpusName = "noProcess";
			
		//mainCFG();
		mainNaive();
		//Grammar = mainEM();		
		//mainGreedy();
		//Grammar = mainEntropy();
		
		wsj_02_11 = complete.returnFirst(wsj_02_11_size);
		wsj_22 = complete.returnLast(wsj_22_size);
		
		wsj_02_11.toFile_Complete(new File(Parameters.outputPath + "penn.02-11.heads"), true);
		wsj_22.toFile_Complete(new File(Parameters.outputPath + "penn.22.heads"), true);
		
		util.ConstDepConverter.printMaltOutput(wsj_02_11, new File(Parameters.outputPath + "malt.02-11.tab"),false);
		util.ConstDepConverter.printMaltOutput(wsj_22, new File(Parameters.outputPath + "malt.22.gold.tab"), false);
		util.ConstDepConverter.printMaltOutput(wsj_22, new File(Parameters.outputPath + "malt.22.test"), true);
		
		String[] argsTrainer = {
				"-w", Parameters.outputPath,
				"-c", "config",
				"-if", "malttab",
				"-of", "malttab",
				"-i", Parameters.outputPath + "malt.02-11.tab",
				"-lsx", "/home/fsangati/SOFTWARE/libsvm-2.86/svm-train", 
				"-m", "learn",
				"-d", "POSTAG", 
				"-s", "Input[0]", 
				"-T", "1000" 
		};		
		
		
		
		String[] argsParser = {			
				"-w", Parameters.outputPath,
				"-c", "config",
				"-of", "malttab",
				"-i", Parameters.outputPath + "malt.22.gold.tab",
				"-o", Parameters.outputPath + "malt.22.out.tab",
				"-m", Parameters.outputPath + "parse"
		};
		
		org.maltparser.Malt.main(argsTrainer);
		//org.maltparser.Malt.main(argsParser);
						
		//java -Xmx1024M -jar ~/Code/Malt/malt.jar -w ~/PROJECTS/Malt/ -c test_wsj00-21_collins -if malttab -i ~/CORPUS/WSJ/MALT2/wsj-02-21.tab -lsx ~/SOFTWARE/libsvm-2.86/svm-train -m learn -d POSTAG -s "Input[0]" -T 1000
		//java -jar malt.jar -c test -i examples/data/talbanken05_test.conll -o out.conll -m parse

	}
	

}
