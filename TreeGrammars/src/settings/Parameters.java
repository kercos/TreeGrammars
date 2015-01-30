package settings;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import tsg.CFSG;
import tsg.LTSG.LTSG;
import tsg.LTSG.LTSG_EM;
import tsg.LTSG.LTSG_EM_nBest;
import tsg.LTSG.LTSG_Entropy;
import tsg.LTSG.LTSG_Greedy;
import tsg.LTSG.LTSG_Naive;
import tsg.corpora.*;
import tsg.dop.DOP_MaxDepth;
import tsg.parser.Parser;
import util.*;
import util.file.FileUtil;


public class Parameters {
	
	static {
		System.setProperty("file.encoding", "UTF-8");
	}
	
	//const		
	//public static final boolean imVici = (new File("/home/fsangati/.imVici")).exists();
	public static final boolean imFedeMBP = (new File("/.imFedeMBP")).exists();
	public static final boolean imInSara = (new File("/home/sangati/.imInSara")).exists();
	public static final boolean imInLaco = (new File("/datastore/fsangati")).exists();
	public static final boolean imInBanff = (new File("/disk/scratch/fsangati")).exists();
	public static final String homePath = System.getProperty("user.home") + "/";	
	public static final String scratchPath = 
			imInLaco? "/datastore/fsangati/" : (imInBanff ? "/disk/scratch/fsangati/" : "/scratch/fsangati/");
	public static final String softwarePath = homePath + ( imFedeMBP ? "Work/SOFTWARE/" : "SOFTWARE/") ;
	public static final String corpusPath = homePath + "CORPORA/";
	public static final String resultsPath = scratchPath + "RESULTS/";
	public static final File globalResults = new File(resultsPath + "TSG/Trace_Results.txt");
	public static boolean writeGlobalResults = true;
	
	// external applications
	public static String bitparApp = imFedeMBP ? "/Users/fedja/Work/SOFTWARE/BitPar_Web/bitpar" : 
		(imInSara ?  "/home/sangati/SOFTWARE/BitPar_Web/bitpar": "/home/fsangati/SOFTWARE/BitPar_Web/bitpar");	
	
	public static String mjCKYApp = imFedeMBP ? "/Users/fedja/Work/SOFTWARE/MJ_cky/lncky" : 
		(imInSara ?  "/home/sangati/SOFTWARE/MJ_cky/lncky": "/home/fsangati/SOFTWARE/MJ_cky/lncky");
	
	public static String codeBase = imFedeMBP ? "/Users/fedja/Work/Code/TreeGrammars/" : 
		(imInSara ?  "/home/sangati/Code/TreeGrammars/": "/home/fsangati/Code/TreeGrammars/");
	
	//public static String bitparApp = imFedeMBP ? "/Users/fedja/Work/SOFTWARE/BitPar_new/bitpar" : 
	//	(imInSara ?  "/home/sangati/SOFTWARE/BitPar_new/bitpar": "/home/fsangati/SOFTWARE/BitPar_new/bitpar");	
	
	//parameters necessary for all runs
	public static String outputPath;
	public static String grammarType;
	public static File trainingProcessed, testGold, testGoldCovered, testFlat, testExtrWords;
	
	//corpus parameters
	public static String corpusName;
	public static int lengthLimitTraining;
	public static int lengthLimitTest;
	
	public static boolean traces;
		
	public static boolean removePunctStartEnd;
	public static boolean removeNonNecessaryLables;
	public static boolean removeRedundantRules;
	public static boolean raisePunctuation;
	
	public static int ukLimit;
	public static boolean semanticTags;
	public static boolean replaceNumbers;
	
	//coprus statistics
	public static String[] internalLabels;
	public static String[] posTagLabels;
	public static Hashtable<String,Integer> lexiconTable;
	
	
	//parameters for Wsj in Wsj
	
	//parameters for CFG
	public static boolean toNormalForm;
	
	//parameters for DOP_max_depth
	public static int maxDepth;
	public static int maxTreesInDepth;	
		
	// parameters necessary for LTSG and all extended classes
	public static String LTSGtype;
	public static boolean cutTopRecursion; 
	public static boolean delexicalize; 
	public static int removeTreesLimit;
	public static boolean smoothing; 
	public static int smoothingFactor;
	
	public static boolean spineConversion;
	public static boolean removeRedundencyInSpine;
	public static boolean posTagConversion;
	public static boolean jollyConversion;
	public static boolean jollyInclusion; //true: only include jolly labels | false: only exclude jolly labels 
	public static String[] jollyLabels;
	
	
	//parameters necessary for LTSG_Naive
	public static int naive_iterations;
	
	//parameters necessary for LTSG_Greedy
	public static boolean greedy_punctuation;
	public static int greedy_ambiguityChoice; //  0        1        2                 3        
	private static String ambiguityChoice[] = {"random", "left", "right", "backoff to lexicon"};
	
	//parameters necessary for LTSG_Entropy
	public static String startingHeads;
	public static int orderOfChange;
	public static int maxNumberOfChanges;
	public static int maxEntropyCycles;
	public static double entropy_delta_threshold;
	
	//parameters necessary for LTSG_EM
	public static int EM_nBest;
	public static double EM_deltaThreshold;
	public static int EM_maxCycle;
	public static String EM_initialization;
	
	//parser parameters
	public static String parserName;
	public static int nBest;	
	public static boolean cachingActive;
	public static boolean markHeadsInPostprocessing;
	public static boolean makeCoveredStatistics;
	public static boolean makeOracleStatistics;
	
	
	// parameters created at runtime
	public static File paramFile, logFile;	
	public static PrintWriter logPW;
	public static ConstCorpus trainingCorpus, testCorpus;	
	

	public static File getLogFile(File sourceFile, String c) {
		return new File(new File(sourceFile.getAbsolutePath()).getParentFile().getAbsoluteFile() + "/" + c + "_" + Utility.getDateTime() + ".log");
	}
	
	public static void openLogFile(File lf) {
		System.out.println("System log file: " + lf);
		logFile = lf;
		logPW = FileUtil.getPrintWriter(logFile);
	}
	
	public static void closeLogFile() {		
		if (logPW!=null) logPW.close();
	}
	
	public static void logString(String lineLog) {
		if (logPW!=null) logPW.print(lineLog);
	}
	
	public static void logStringFlush(String lineLog) {
		if (logPW!=null) {
			logPW.print(lineLog);
			logPW.flush();
		}		
	}
	
	public static synchronized void logLine(String lineLog) {
		if (logPW!=null) logPW.println(lineLog);		
	}
	
	public static synchronized void logLineFlush(String lineLog) {
		if (logPW!=null) {
			logPW.println(lineLog);
			logPW.flush();
		}		
	}
	
	
	public static void reportString(String lineLog) {
		if (logPW!=null) logPW.print(lineLog);
		System.out.print(lineLog);
	}
	
	public static void reportStringFlush(String lineLog) {
		if (logPW!=null) {
			logPW.print(lineLog);
			logPW.flush();
		}		
		System.out.print(lineLog);
	}
	
	public static void flushLog() {
		logPW.flush();
	}
	
	public static synchronized void reportLine(String lineLog) {
		if (logPW!=null) logPW.println(lineLog);		
		System.out.println(lineLog);
	}
	
	public static void logStdOutPrintln(String lineLog) {
		if (logPW!=null) logPW.println(lineLog);
		System.out.println(lineLog);
	}
	
	public static void logStdOutPrint(String lineLog) {
		if (logPW!=null) logPW.print(lineLog);
		System.out.print(lineLog);
	}
	
	public static void logPrint(String lineLog) {
		if (logPW!=null) logPW.print(lineLog);
	}

	public static void logPrintln(String lineLog) {
		if (logPW!=null) logPW.println(lineLog);
	}

	public static synchronized void reportLineFlush(String lineLog) {
		if (logPW!=null) {
			logPW.println(lineLog);
			logPW.flush();
		}		
		System.out.println(lineLog);
	}
	
	public static synchronized void reportError(String lineLog) {
		if (logPW!=null) {
			logPW.println(lineLog);
			logPW.flush();
		}		
		System.err.println(lineLog);
	}
	
	public static void appendReturnInLogFile(String lineLog) {
		if (logFile!=null) FileUtil.appendReturn(lineLog, logFile);		
	}
	
	public static void appendInLogFile(String lineLog) {
		if (logFile!=null) FileUtil.append(lineLog, logFile);		
	}

	public static void newRun(Object grammar) {
		buildDirectory();		
		if (corpusName.equals("Wsj")) {
			Wsj.retriveTrainingAndTestCorpus();
		}		
		else if (corpusName.equals("Parc"));
		else if (corpusName.equals("Negra"));
		else if (corpusName.equals("Tiger"));
		else if (corpusName.equals("noProcess"));
		else {
			System.err.println("Corpus name not yet implemented");
			System.exit(-1);
		}		
		writeParam(grammar);
		FileUtil.appendReturn("Read corpus. Total # of sentences: " + trainingCorpus.size(), logFile);
	}
	
	public static void buildDirectory() {		
		if (!outputPath.endsWith("/")) outputPath += "/";
		outputPath += FileUtil.dateTimeString() + "/";
		File f = new File(outputPath);
		f.mkdirs();
		paramFile = new File(outputPath + "Parameters");
		logFile = new File(outputPath + "Log");	
		trainingProcessed = new File(Parameters.outputPath + "TrainingCorpus.processed");
		testGold = new File(Parameters.outputPath + "TestCorpus.gold");
		testGoldCovered = new File(Parameters.outputPath + "TestCorpus.gold.covered");
		testFlat = new File(Parameters.outputPath + "TestCorpus.flat");
		testExtrWords = new File(Parameters.outputPath + "TestCorpus.extrWords");
		System.out.println("Output folder: " + outputPath);
	}
	
	private static void writeParam(Object grammar) {		
		grammarType = grammar.getClass().getName();
		String param = grammarType;
		if ((CFSG.class).isAssignableFrom(grammar.getClass()))	{		
			param += "\n\n" + "Training Length Limit: " + lengthLimitTraining;
			param += "\n" + "Testing Length Limit: " + lengthLimitTest;
			param += "\n" + "Unknown Threshold (<=): " + ukLimit; // in Collins = 5
			param += "\n" + "Semantic Tags: " + semanticTags;	
			param += "\n" + "Replace numbers with unique tag: " + replaceNumbers;
			param += "\n" + "Traces are present (-NONE-, NP-2): " + traces;	
			param += "\n" + "Remove non-necessary labels (" + Arrays.toString(Wsj.nonNecessaryLabels) + "): "  + removeNonNecessaryLables;
			param += "\n" + "Remove punctuation at the start and end of each sentence: " + removePunctStartEnd;
			param += "\n" + "Remove redundant rules (S -> S): " + removeRedundantRules;
			param += "\n" + "Raise punctuation: " + raisePunctuation;
			if (Parameters.corpusName.equals("Wsj")) param += Wsj.writeParam();							
		}
		if (CFSG.class.isAssignableFrom(grammar.getClass())) {
			param += "\n\n" + "Convert CFG rules to normal form: " + toNormalForm;
		}			
		if (DOP_MaxDepth.class.isAssignableFrom(grammar.getClass())) {
			grammarType += "_depth" + maxDepth;
			param += "\n\n" + "Max Depth: " + maxDepth;
			param += "\n" + "Max Trees for depth > 1: " + maxTreesInDepth;		
		}
		if ((LTSG.class).isAssignableFrom(grammar.getClass()))	{
			grammarType += "_" + LTSGtype;
			param += "\n\n" + "LTSG type " + LTSGtype;
			param += "\n\n" + "Cut top recursions: " + cutTopRecursion;
			param += "\n" + "Delexicalization: " + delexicalize;		
			param += "\n" + "Remove trees occurring <= " + removeTreesLimit;
			param += "\n" + "Smoothing: " + smoothing + 
						((smoothing)? "\t(Smoothing Factor: " + smoothingFactor + " )" : "");
			if (!smoothing) smoothingFactor = 1; 
			param += "\n" + "Conversion to spine: " + spineConversion;
			param += "\nRemove redundency in spine: " + removeRedundencyInSpine;
			param += "\n" + "Conversion to posTags: " + posTagConversion;
			param += "\n" + "Jolly Conversion: " + jollyConversion;			
			if (jollyConversion) {
				param += (Parameters.jollyInclusion) ? " (inclusion)" : " (exclusion)";
				param += "\n\t" + "Jolly labels: " + Arrays.toString(jollyLabels);
			}
			
		}
		if ((LTSG_Naive.class).isAssignableFrom(grammar.getClass()))	{
			param += "\n\n" + "Iterations: " + naive_iterations;
		}
		if ((LTSG_EM.class).isAssignableFrom(grammar.getClass()))	{
			param += "\n\n" + "EM_nBest: " + Parameters.EM_nBest;
			param += "\n" + "EM_deltaThreshold: " + Parameters.EM_deltaThreshold;
			param += "\n" + "EM_maxCycle: " + Parameters.EM_maxCycle;
			param += "\n" + "EM_initialization: " + Parameters.EM_initialization;
		}
		if ((LTSG_Greedy.class).isAssignableFrom(grammar.getClass()))	{
			param += "\n\n" + "\tPunctuation: " + Parameters.greedy_punctuation;
			param += "\n" + "\tAmbiguity Choice: " + ambiguityChoice[greedy_ambiguityChoice];
		}
		if ((LTSG_Entropy.class).isAssignableFrom(grammar.getClass()))	{
			param += "\n\n" + "Starting from Heads: " + startingHeads;
			param += "\n" + "Order of change: " + LTSG_Entropy.OrderOfChangeTypes[orderOfChange];			
			param += "\n" + "Number of changes per tree per cylces: " + maxNumberOfChanges;
			param += "\n" + "Maximum number of cycles: " + maxEntropyCycles;
			param += "\n" + "Delta threshold: " + entropy_delta_threshold;
		}
		if ((LTSG_EM_nBest.class).isAssignableFrom(grammar.getClass())){
			param += "\n\n" + "EM n best: " + EM_nBest;
			param += "\n" + "EM delta threshold: " + EM_deltaThreshold;
			param += "\n" + "EM max cycles: " + EM_maxCycle;
			
		}
		param += "\n\n" + "Parser name: " + Parameters.parserName;
		param += "\n" + "nBest: " + Parameters.nBest; 
		param += "\n" + "Caching Active: " + Parameters.cachingActive; 
		param += "\n" + "Mark heads in postprocessing: " + Parameters.markHeadsInPostprocessing;
		param += "\n" + "Make statistics for covered sentences: " + Parameters.makeCoveredStatistics;
		param += "\n" + "Make oracle statistics: " + Parameters.makeOracleStatistics;
		FileUtil.appendReturn(param, paramFile);
	}
	
	/**
	 * Print the training corpus in the output directory
	 */
	public static void printTrainingCorpusToFile() {
		Parameters.trainingCorpus.toFile_Complete(Parameters.trainingProcessed, true);
	}
	
	/**
	 * Print the test corpus in the output directory
	 */
	public static void printTestCorpusToFile() {
		Parameters.testCorpus.toFile_Complete(Parameters.testGold, false);
		Parameters.testCorpus.toFile_Flat(Parameters.testFlat);
		Parameters.testCorpus.toFile_ExtractWords(Parameters.testExtrWords);
	}
	
	public static void printTrainingAndTest() {
		printTrainingCorpusToFile();
		printTestCorpusToFile();
	}
	
	public static void setDefaultParam() {
		Parameters.corpusName = "Wsj"; //Wsj, Negra, Parc, Tiger 
		Parameters.lengthLimitTraining = 20;
		Parameters.lengthLimitTest = 20;
		
		Wsj.initialHeads = 0; // noheads, magerman, collins97, collins99, y&m

		Parameters.traces = false;
		
		Wsj.testSet = "22"; //00 01 22 23 24
		Wsj.skip120TrainingSentences = false;
		
		Parameters.removeNonNecessaryLables = true;
		Parameters.removePunctStartEnd = true;		
		Parameters.raisePunctuation = true;
		Parameters.removeRedundantRules = true;
		
		Parameters.toNormalForm = false;
		
		Wsj.transformNPbasal = true;
		Wsj.transformSG = false;
		Parameters.semanticTags = true;
		
		Parameters.replaceNumbers = true;
		Parameters.ukLimit = 1;
		
		Parameters.cutTopRecursion = false;
		Parameters.delexicalize = false;					
		Parameters.removeTreesLimit = -1;		
		Parameters.smoothing = true;
		Parameters.smoothingFactor = 100;
		
		Parameters.parserName = Parser.bitPar; //Parser.bitPar;
		Parameters.nBest = 1;	
		Parameters.markHeadsInPostprocessing = true;
		Parameters.makeCoveredStatistics = !Parameters.smoothing;
		Parameters.makeOracleStatistics = Parameters.nBest > 1;

	}

	public static void main(String[] args) {
	}



}
