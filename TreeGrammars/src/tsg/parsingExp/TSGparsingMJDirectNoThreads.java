package tsg.parsingExp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;

public class TSGparsingMJDirectNoThreads extends TSGparsingBitPar {
	
	MetricOptimizerArray metricOptimizer;
	
	public TSGparsingMJDirectNoThreads(File trainingFile, File testFile, File fragmentFile, File outputDir) {
		super(trainingFile, testFile, fragmentFile, outputDir);
		metricOptimizer = new MetricOptimizerArray(TSGparsingMJDirectNoThreads.this);
	}	
	
	public String getClassName() {
		return "TSGparsingMJDirectNoThreads";
	}
	
	public static void printMJFlatSentence(ArrayList<String[]> testSentencesWords, File outputFile) {
		PrintWriter pwMJInput = FileUtil.getPrintWriter(outputFile);
		for(String[] sentencePosWord : testSentencesWords) {															
			String sentenceBitParFormat = Utility.joinStringArrayToString(sentencePosWord, " ");
			pwMJInput.println(sentenceBitParFormat);
		}
		pwMJInput.close();
	}

	protected void parseWithBitPar() throws Exception {
		
		Parameters.reportLineFlush("Parsing with MT");
		
		String mjBuildingFilesPath = outputPath + "MJWorkingDir/";
		File mjBuildingFilesDir = new File(mjBuildingFilesPath);
		mjBuildingFilesDir.mkdir();
		
		//File ouputMJFile = new File(mjBuildingFilesPath + "outputMJ.txt");		
		File flatFileForMJ = new File(mjBuildingFilesPath + "flatFileForMJ.txt");		
		
		ArrayList<String[]> testSentencesWords = getSentencesWords(testTreebank);
		printMJFlatSentence(testSentencesWords, flatFileForMJ);
		
		File mjGrammar = new File(outputPath + "mjGrammar.txt");
		ConvertBitParGrammarInMJFormat.makeConversion(bibtpar_grammarFile, bitpar_lexiconFile, mjGrammar, topSymbol);
		
		ArrayList<String[]> originalSentencesWords = getSentencesWords(originalTestTreebank);;
		
		//PrintWriter pw = FileUtil.getPrintWriter(ouputMJFile);
		Iterator<String[]> originalTestIterator = originalSentencesWords.iterator();
				
		try {		
			String command = mjCKYApp + " " + flatFileForMJ + " 0 " + mjGrammar;
			Parameters.reportLine("Running MJcky: " + command);
			Process p = Runtime.getRuntime().exec(command);
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    while ((line = input.readLine()) != null) {
		    	//Parameters.reportLineFlush("MJStdOut:" + line);
		    	//pw.println(line);
		    	processLine(line, originalTestIterator);
		    	doneWithOneSentence();
		    }
		    input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		    while ((line = input.readLine()) != null) Parameters.reportLineFlush("MJStdErr:" + line);
		    input.close();
		}
		catch (Exception err) {
		   err.printStackTrace();
		}
		
		//pw.close();		
		//postProcessNbest(ouputMJFile, originalSentencesWords);
		
		parsedOutputFiles = metricOptimizer.makeFileOutputList(outputPath + "MJparser_", ".mrg");
		parsedOutputFilesIdentifiers = metricOptimizer.getIdentifiers();
		metricOptimizer.appendResult(parsedOutputFiles);
		
		Parameters.reportLineFlush("Finished Parsing.");
				
	}	
	
	private void processLine(String line, Iterator<String[]> originalTestIterator) throws Exception  {
		
		String[] originalTestSentenceWords = originalTestIterator.next();		
		
		if (line.charAt(0)=='(') {
			TSNodeLabel tree = null;
			line = line.replaceAll("\\\\", "");
			try {
				tree = new TSNodeLabel(line);
			} catch (Exception e) {
				e.printStackTrace();
			}				
			if (!shortestDerivation)
				tree = postProcessParseTree(tree);
			metricOptimizer.prepareNextSentence(originalTestSentenceWords);
			metricOptimizer.addNewDerivation(tree, 1);
			metricOptimizer.storeCurrentBestParseTrees();													
			return;
		}
		else if (line.startsWith("parse_failure")) {							
			TSNodeLabel tree = dealWithNOParsedSentences(originalTestSentenceWords);;
			metricOptimizer.prepareNextSentence(originalTestSentenceWords);
			metricOptimizer.addNewDerivation(tree, 1);
			metricOptimizer.storeCurrentBestParseTrees();
			return;				
		}
		System.out.println("Unknown line in bitpar output: " + line);
	}

	
	
}
