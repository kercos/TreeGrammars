package tsg.parser.petrov;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import tsg.CFSG;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import tsg.metrics.ParseMetricOptimizer;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import tsg.parser.Parser;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class RunPetrovGrammarWithFedepar extends Thread {

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
	
	Hashtable<String, Double> lexicon;
	Hashtable<String, Double> internalRules;	
	File flatTestSet, parsedFile;
	
	public void run() {		
		outputPath = outputDir + "/" + "Parsing_" + FileUtil.dateTimeString() + "/";
		new File(outputPath).mkdir();		
		Parameters.openLogFile(new File(outputPath + "log.txt"));
		outputParametersToLogFile();
		testTreebankFileClean = new File(outputPath + testTreebankFile.getName());		
		getTrainingAndTestTreebanks();
		preprocessUnknownWords();		
		convertGrammar();
		convertLexicon();	
		writeFlatTest();
		try {
			parse();
		} catch (Exception e) {			
			e.printStackTrace();
		}		
		parseEval();
	}	
	
    private void writeFlatTest() {
        flatTestSet = new File(outputPath + "test.flat");
        PrintWriter pw = FileUtil.getPrintWriter(flatTestSet);
        for(TSNodeLabel t : testTreebank) {
                pw.println(t.toFlatSentence());
        }
        pw.close();
}


	private void parse() throws Exception {		
		Parameters.reportLineFlush("Parsing with FedePar");
		Parameters.reportLineFlush("Parsing " + testSize + " sentences:");		
		Parameters.nBest = 1;		
		CFSG<Double> grammar = new CFSG<Double>(lexicon, internalRules);
		parsedFile = new File(outputPath + "fedPar_parsed.mrg");
		new Parser(grammar, flatTestSet, parsedFile);							
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

	/**
	 * A line of Petrov Grammar file looks like this:
	 * S^g_1 -> PP^g_21 NP^g_45 4.1686166964667563E-10
	 */
	private void convertGrammar() {				
		
		Parameters.reportLineFlush("Extracting internal rules");
		internalRules = new Hashtable<String, Double>(); 		
		
		Scanner grammarScan = FileUtil.getScanner(petrovGrammarFile);		
		while(grammarScan.hasNextLine()) {
			String line = grammarScan.nextLine();
			String[] lineSplit = line.split("\\s");
			int length = lineSplit.length;
			double prob = Double.parseDouble(lineSplit[length-1]);		
			if (prob<minProbRule) continue;
			String rule = convertLable(lineSplit[0]);
			for(int i=2; i<length-1; i++) {
				rule += " " + convertLable(lineSplit[i]);				
			}
			internalRules.put(rule, prob);
		}
	}
	
	static String regexConvertLabel = "\\^g";
		
	private static String convertLable(String label) {
		return label.replace('_', '-').replaceFirst(regexConvertLabel, "");				
	}

	/**
	 * A line of Petrov Lexicon file looks like this:
	 * IN via [1.6034755262090546E-14, 3.9819306306509576E-14, ...]
	 */
	private void convertLexicon() {		
			
		Parameters.reportLineFlush("Extracting lexical rules");
		lexicon = new Hashtable<String, Double>();
		
		Scanner lexiconScan = FileUtil.getScanner(petrovLexiconFile); 
		while(lexiconScan.hasNextLine()) {
			String line = lexiconScan.nextLine();
			line = line.replaceAll("\\\\", "");			
			String[] lineSplit = line.split("\\s");
			int length = lineSplit.length;
			String pos = lineSplit[0];
			String lex = lineSplit[1];
				
			int index = 0;
			for(int i=2; i<length; i++) {				
				double prob = cleanProb(lineSplit[i]);
				if (prob>minProbRule) {
					String rule = pos + "-" + index + " " + lex;
					lexicon.put(rule, prob);
				}
				index++;
			}
			
		}
				
	}

	static String regexCleanProb = "[\\[\\]\\,]";
	private static double cleanProb(String p) {
		return Double.parseDouble(p.replaceAll(regexCleanProb, ""));
	}
	
	
	private void outputParametersToLogFile() {
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

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
				
		UkWordMapping.ukThreashold = 0;		
		ukModel = new UkWordMappingPetrov();		
		nBest = 1000;
		topSymbol = "ROOT-0";		
		sentenceLengthLimitTest = 40;
		minProbRule = 1E-50; //-Double.MAX_VALUE; //		
		MetricOptimizerArray.setLambdaValues(0, 2, 0.5);
		
		petrovGrammarFile = new File(args[0]);
		petrovLexiconFile = new File(args[1]);
		outputDir = new File(args[2]);
		trainTreebankFile = new File(args[3]);
		testTreebankFile = new File(args[4]);
		threads = Integer.parseInt(args[5]);		
		new RunPetrovGrammarWithFedepar().run();		
		
	}

}
