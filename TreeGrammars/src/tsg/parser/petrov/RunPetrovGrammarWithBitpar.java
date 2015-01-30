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
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import tsg.metrics.ParseMetricOptimizer;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class RunPetrovGrammarWithBitpar extends Thread {

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
	File outputBitparGrammar, outputBitparLexicon;
	File testTreebankFileClean;
	ArrayList<TSNodeLabel> originalTrainingTreebank, trainingTreebank;
	ArrayList<TSNodeLabel> originalTestTreebank, testTreebank;
	int testSize;
	BitParParserPetrov parser;
	
	static boolean removeCyclicRules;
	
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
		
		try {
			parse();
		} catch (Exception e) {			
			e.printStackTrace();
		}		
		parseEval();
		
	}	
	
	private void parse() throws Exception {		
		Parameters.reportLineFlush("Parsing with BitPar using " + threads + " threads");
		Parameters.reportLineFlush("Parsing " + testSize + " sentences:");		
		parser = new BitParParserPetrov(testTreebank, originalTestTreebank, 
				threads, outputPath, outputBitparGrammar, outputBitparLexicon, nBest, topSymbol);
		System.gc();
		parser.runParser();					
	}
	
	
	private void parseEval() {
		Parameters.reportLineFlush("Running EvalB and EvalC");
		DecimalFormat df = new DecimalFormat("0.00");
		File[] parsedOutputFiles = parser.getParsedFiles();
		String[] parsedOutputFilesIdentifiers = parser.getParsedFilesIdentifiers();
		int length  = parsedOutputFiles.length;
		for(int i=0; i<length; i++) {
			File f = parsedOutputFiles[i];
			String id = parsedOutputFilesIdentifiers[i];
			File evalBfile = FileUtil.changeExtension(f, "evalB");
			File evalCfile = FileUtil.changeExtension(f, "evalC");
			new EvalB(testTreebankFileClean, f, evalBfile);					
			//File evalCfileLog = new File(outputPath + "BITPAR_MOST_PROB_PARSES.evalC.log");
			EvalC eval = new EvalC(testTreebankFileClean, f, evalCfile, null, true);	
			float[] results = eval.makeEval(); //recall precision fscore
			Parameters.reportLineFlush(Utility.fse(15, id) + 
					":\tRecall, Precision, Fscore (<=" + EvalC.CUTOFF_LENGTH + "):  [" 
					+ df.format(results[0]) + ", " + df.format(results[1]) + ", " + df.format(results[2])
					+ "]");
		}
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
		
		outputBitparGrammar = new File(outputPath + "bitpar_grammar.txt");
		Parameters.reportLineFlush("Writing grammar to: " + outputBitparGrammar);
		
		Scanner grammarScan = FileUtil.getScanner(petrovGrammarFile);		
		PrintWriter pw = FileUtil.getPrintWriter(outputBitparGrammar);
		int cyclicRulesSkipped = 0;
		int smallProbRuleSkipped = 0;
		while(grammarScan.hasNextLine()) {
			String line = grammarScan.nextLine();
			String[] lineSplit = line.split("\\s");			
			int length = lineSplit.length;			
			double prob = Double.parseDouble(lineSplit[length-1]);		
			if (prob<minProbRule) {
				smallProbRuleSkipped++;
				continue;			
			}
			String lhs = convertLable(lineSplit[0]);
			boolean unaryRule = length == 4;
			if (unaryRule) {
				String rhsChild = convertLable(lineSplit[2]);
				if (removeCyclicRules && lhs.equals(rhsChild)) {
					cyclicRulesSkipped++;
					continue;
				}
				pw.println(prob + " " + lhs + " " + rhsChild);				
			}
			else {
				pw.print(prob + " " + lhs);
				for(int i=2; i<length-1; i++) {
					String rhsChild = convertLable(lineSplit[i]);
					pw.print(" " + rhsChild);
				}				
				pw.println();
			}			
		}
		pw.close();
		Parameters.reportLine("Skipped small prob rules: " + smallProbRuleSkipped);
		Parameters.reportLineFlush("Skipped cyclic rules: " + cyclicRulesSkipped);		
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
		
		outputBitparLexicon = new File(outputPath + "bitpar_lexicon.txt");
		System.out.println("Writing lexicon to: " + outputBitparLexicon);
		
		Scanner lexiconScan = FileUtil.getScanner(petrovLexiconFile);
		Hashtable<String,Vector<String>> lexPosProbTable = new Hashtable<String,Vector<String>>(); 
		while(lexiconScan.hasNextLine()) {
			String line = lexiconScan.nextLine();
			line = line.replaceAll("\\\\", "");			
			String[] lineSplit = line.split("\\s");
			int length = lineSplit.length;
			String pos = lineSplit[0];
			String lex = lineSplit[1];

			Vector<String> lexPosProbInTable = lexPosProbTable.get(lex);
			if (lexPosProbInTable==null) {
				lexPosProbInTable = new Vector<String>();
				lexPosProbTable.put(lex, lexPosProbInTable);
			}
				
			int index = 0;
			for(int i=2; i<length; i++) {				
				double prob = cleanProb(lineSplit[i]);
				if (prob>minProbRule) 
					lexPosProbInTable.add(pos + "-" + index + " " + prob);
				index++;
			}
			
		}
		
		
		PrintWriter pw = FileUtil.getPrintWriter(outputBitparLexicon);
		for(Entry<String, Vector<String>> e : lexPosProbTable.entrySet()) {
			String lex = e.getKey();
			Vector<String> lexPosProb = e.getValue();
			if (lexPosProb.isEmpty()) continue;
			pw.print(lex);
			for(String posProb : lexPosProb) {
				pw.print("\t" + posProb);
			}
			pw.println();
		}
		pw.close();
	}

	static String regexCleanProb = "[\\[\\]\\,]";
	private static double cleanProb(String p) {
		return Double.parseDouble(p.replaceAll(regexCleanProb, ""));
	}
	
	
	private void outputParametersToLogFile() {
		Parameters.reportLine("RunPetrovGrammarWithBitpar\n");

		Parameters.reportLine("outputPath: " + outputPath);
		Parameters.reportLine("nBest: " + nBest);
		Parameters.reportLine("topSymbol: " + topSymbol);
		Parameters.reportLine("sentenceLengthLimitTest: " + sentenceLengthLimitTest);
		Parameters.reportLine("minProbRule: " + minProbRule);
		Parameters.reportLine("petrovGrammarFile: " + petrovGrammarFile);
		Parameters.reportLine("petrovLexiconFile: " + petrovLexiconFile);
		Parameters.reportLine("trainTreebankFile: " + trainTreebankFile);
		Parameters.reportLine("testTreebankFile: " + testTreebankFile);
		Parameters.reportLine("removeCyclicRules: " + removeCyclicRules);
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
		//minProbRule = 0; //1E-20		
		MetricOptimizerArray.setLambdaValues(0, 2, 0.5);
		removeCyclicRules = true;
		
		petrovGrammarFile = new File(args[0]);
		petrovLexiconFile = new File(args[1]);
		outputDir = new File(args[2]);
		trainTreebankFile = new File(args[3]);
		testTreebankFile = new File(args[4]);
		threads = Integer.parseInt(args[5]);
		minProbRule = Double.parseDouble(args[6]);		
		new RunPetrovGrammarWithBitpar().run();		
		
	}

}
