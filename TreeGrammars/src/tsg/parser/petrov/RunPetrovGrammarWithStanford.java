package tsg.parser.petrov;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import edu.stanford.nlp.parser.lexparser.BinaryGrammar;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.Lexicon;
import edu.stanford.nlp.parser.lexparser.Options;
import edu.stanford.nlp.parser.lexparser.ParserData;
import edu.stanford.nlp.parser.lexparser.PetrovLexiconFede;
import edu.stanford.nlp.parser.lexparser.UnaryGrammar;
import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import tsg.metrics.ParseMetricOptimizer;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class RunPetrovGrammarWithStanford extends Thread {

	static UkWordMapping ukModel;
	static int nBest;
	static String topSymbol;
	static double minProbRule;
	
	static File petrovGrammarFile, petrovLexiconFile;
	static File outputDir;	
	static int threads;
	static File trainTreebankFile, testTreebankFile;
	static int sentenceLengthLimitTest;
	
	String outputPath;
	File testTreebankFileClean;
	ArrayList<TSNodeLabel> originalTrainingTreebank, trainingTreebank;
	ArrayList<TSNodeLabel> originalTestTreebank, testTreebank;
	int testSize;
	
	File parsedFile;
	
	LexicalizedParser lp;
	
	public void run() {		
		outputPath = outputDir + "/" + "Parsing_" + FileUtil.dateTimeString() + "/";
		new File(outputPath).mkdir();		
		Parameters.openLogFile(new File(outputPath + "log.txt"));
		outputParametersToLogFile();
		testTreebankFileClean = new File(outputPath + testTreebankFile.getName());		
		getTrainingAndTestTreebanks();
		preprocessUnknownWords();		
				
		try {
			buildGrammar();
			parse();
		} catch (Exception e) {			
			e.printStackTrace();
		}		
		parseEval();
	}	
	
	private void buildGrammar() throws FileNotFoundException {
		Parameters.reportLineFlush("Acquiring petrov grammar...");
		ParserData pd = LexicalizedParser.getParserDataFromPetrovFiles(
				petrovGrammarFile.toString(), petrovLexiconFile.toString());
		Parameters.reportLineFlush("Finished Acquisition");
		lp = new LexicalizedParser(pd);
		lp.setGoalString(topSymbol);		
		//lp.printNumberer(new File(outputPath + "statesTagsWords.txt"));
	}	

	private void parse() throws Exception {		
		Parameters.reportLineFlush("Parsing Petrov with Stanford Parser using " + threads + " threads");
		Parameters.reportLineFlush("Parsing " + testSize + " sentences:");
		parsedFile = new File(outputPath + "parsedFile.mrg");
		PrintWriter pw = FileUtil.getPrintWriter(parsedFile);
		for(TSNodeLabel t : testTreebank) {			
			String flatSentence = t.toFlatSentence();
			System.out.println("Parsing sentence: " + flatSentence);
			lp.parse(flatSentence);
			pw.println(lp.getBestParse().toString());			
		}		
		pw.close();							
	}
	
	
	private void parseEval() {
		Parameters.reportLineFlush("Running EvalB and EvalC");
		DecimalFormat df = new DecimalFormat("0.00");
						
		File evalBfile = FileUtil.changeExtension(parsedFile, "evalB");
		File evalCfile = FileUtil.changeExtension(parsedFile, "evalC");
		new EvalB(testTreebankFileClean, parsedFile, evalBfile);					
		//File evalCfileLog = new File(outputPath + "BITPAR_MOST_PROB_PARSES.evalC.log");
		EvalC eval = new EvalC(testTreebankFileClean, parsedFile, evalCfile, null, true);	
		float[] results = eval.makeEval(); //recall precision fscore
		Parameters.reportLineFlush(Utility.fse(15, "MPD") + 
				":\tRecall, Precision, Fscore (<=" + EvalC.CUTOFF_LENGTH + "):  [" 
				+ df.format(results[0]) + ", " + df.format(results[1]) + ", " + df.format(results[2])
				+ "]");
	}


	private void getTrainingAndTestTreebanks() {		
		try {
			Parameters.reportLineFlush("Reading Traininig Treebank");
			trainingTreebank = TSNodeLabel.getTreebank(trainTreebankFile);
			int trainingSize = trainingTreebank.size();
			Parameters.reportLineFlush("Traininig Treebank Size: " + trainingSize);
			Parameters.reportLineFlush("Reading Test Treebank");			
			testTreebank = TSNodeLabel.getTreebank(testTreebankFile, sentenceLengthLimitTest);
			TSNodeLabel.printTreebankToFile(testTreebankFileClean, testTreebank, false, false);
			testSize = testTreebank.size();
			Parameters.reportLineFlush("Test Treebank Size: " + testSize);
			originalTrainingTreebank = trainingTreebank;
			originalTestTreebank = testTreebank;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void preprocessUnknownWords() {
		Parameters.reportLineFlush("Processing Unknown Words with model: " + ukModel.getClass());		
		Parameters.reportLineFlush("UK threshold: " + UkWordMapping.ukThreashold);
		ukModel.init(trainingTreebank, testTreebank);
		trainingTreebank = ukModel.transformTrainingTreebank();
		testTreebank = ukModel.transformTestTreebank();
		File transformedTrainingTreBankFile = new File(outputPath + "trainingTreebank_UK.mrg");
		File transformedTestTreBankFile = new File(outputPath + "testTreebank_UK.mrg");
		TSNodeLabel.printTreebankToFile(transformedTrainingTreBankFile, trainingTreebank, false, false);
		TSNodeLabel.printTreebankToFile(transformedTestTreBankFile, testTreebank, false, false);
		Parameters.reportLineFlush("Printed training treebank after unknonw word process to: " + transformedTrainingTreBankFile);
		Parameters.reportLineFlush("Printed test treebank after unknonw word process to: " + transformedTestTreBankFile);
	}

	
	
	private void outputParametersToLogFile() {
		Parameters.reportLine("PetrovGrammarWithStanford\n");
		Parameters.reportLine("nBest: " + nBest);
		Parameters.reportLine("topSymbol: " + topSymbol);
		Parameters.reportLine("sentenceLengthLimitTest: " + sentenceLengthLimitTest);
		Parameters.reportLine("minProbRule: " + minProbRule);
		Parameters.reportLine("petrovGrammarFile: " + petrovGrammarFile);
		Parameters.reportLine("petrovLexiconFile: " + petrovLexiconFile);
		Parameters.reportLine("trainTreebankFile: " + trainTreebankFile);
		Parameters.reportLine("testTreebankFile: " + testTreebankFile);
		Parameters.reportLineFlush("threads: " + threads);
	}	
	
	public static void main(String[] args) throws Exception {
		main1(new String[]{
			"tmp/eng_sm6_readable.gr.grammar",
			"tmp/eng_sm6_readable.gr.lexicon",
			"tmp/",
			"/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/ORIGINAL_READABLE_CLEANED_TOP_SEMTAGSOFF/wsj-02-21.mrg",
			"/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/ORIGINAL_READABLE_CLEANED_TOP_SEMTAGSOFF/wsj-24.mrg",
			"2"
		});
	}


	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main1(String[] args) throws Exception {
				
		UkWordMapping.ukThreashold = 0;		
		ukModel = new UkWordMappingPetrov();		
		nBest = 1000;
		topSymbol = "ROOT-0";		
		sentenceLengthLimitTest = 40;
		minProbRule = PetrovLexiconFede.minProbRule = 0; //1E-10;
		MetricOptimizerArray.setLambdaValues(0, 2, 0.5);
		
		petrovGrammarFile = new File(args[0]);
		petrovLexiconFile = new File(args[1]);
		outputDir = new File(args[2]);
		trainTreebankFile = new File(args[3]);
		testTreebankFile = new File(args[4]);
		threads = Integer.parseInt(args[5]);		
		new RunPetrovGrammarWithStanford().run();		
		
	}
	
	
}
