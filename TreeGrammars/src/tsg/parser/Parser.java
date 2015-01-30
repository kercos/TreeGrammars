package tsg.parser;
import util.*;
import util.file.FileUtil;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.*;
import tsg.corpora.*;
import tsg.parseEval.EvalC;

public class Parser extends Thread {
	
	public static final String fedePar = "fedePar";
	public static final String bitPar = "bitPar";
	Grammar grammar;
	TestSet testSet;
	CacheManager cache;	
	double pruningFactor;
	
	//public static String filePrefix = Parameters.outputPath + Parameters.parserName;
	//public static File nbestParsesFile = new File (filePrefix + ".parses");
	//public static File bestParseFile = new File (filePrefix + ".best");
	//public static File bestParseCoveredFile = new File (filePrefix + ".best.covered");
	
	//public static int[] coveredTotal;
	File flatFile, outputFile;
	CFSG<? extends Number> CFGgrammar;
	
	public Parser(CFSG<? extends Number> CFGgrammar, File flatFile, File outputFile) {
		this.outputFile = outputFile;
		this.flatFile = flatFile;	
		this.CFGgrammar = CFGgrammar;
	}
	
	public void run() {
		List<String> inputSentences = FileUtil.convertFileToStringList(flatFile);
		this.grammar = new Grammar(CFGgrammar);
		//this.grammar.checkCircularUnaryProductions(2);
		this.grammar.checkRecursiveUnaryProductions();			
		testSet = new TestSet(inputSentences, this.grammar);
		cache = new RamCacheManager(this.testSet);
		initializeStatics();
		//long[] timeSentenceLength =  new long[Parameters.lengthLimitTraining+1];
		//int[] countSentenceLength = new int[Parameters.lengthLimitTraining+1];
		PrintProgressStatic.start("FedePar is parsing sentence:");
		for(ArrayList<Integer> sentence : testSet.inputSentences) {
			System.gc();
			//util.Timer.timerStart();
			CKYChart chart = new CKYChart(sentence);
			//chart.checkDerivationConsistency();		
			String nBestDerivations = chart.outputBestNDerivation();
			//String firstDerivation = PostProcess.processSentence(nBestDerivations);
			FileUtil.appendReturn(nBestDerivations, outputFile);
			//FileUtil.append(firstDerivation, Parser.bestParseFile);			
			//long ellapsed = util.Timer.timerStop();
			//int sentenceLength = sentence.size();
			//timeSentenceLength[sentenceLength] += ellapsed;
			//countSentenceLength[sentenceLength]++;
			PrintProgressStatic.next();
		}
		PrintProgressStatic.end();
		//printTimeStatistics(countSentenceLength, timeSentenceLength);
		//Runtime.getRuntime().gc();		
	}
	
	private void printTimeStatistics(int[] countSentenceLength, long[] timeSentenceLength) {
		FileUtil.appendReturn("\n Parsing time statistics", grammar.logFile);
		FileUtil.appendReturn("Length \t AvgParsingTime", grammar.logFile);
		for(int i=0; i<countSentenceLength.length; i++) {
			if (countSentenceLength[i]==0) continue;
			float avg = (float)timeSentenceLength[i]/(countSentenceLength[i]*1000);
			FileUtil.appendReturn("" + i + "\t" + avg, grammar.logFile);
		}
	}
	
	public void initializeStatics() {
		CKYChart.grammar = this.grammar;
		CKYChart.cache = this.cache;
		Cell.grammar = this.grammar;						
	}
	
	/*
	public void postProcess() {
		tsg.parser.PostProcess.processFile(nbestParsesFile, bestParseFile, false);
		if (Parameters.markHeadsInPostprocessing) {
			tsg.parser.PostProcess.processFile(nbestParsesFile, new File(bestParseFile + ".heads"), true); 
		}
		if (Parameters.makeCoveredStatistics) {
			coveredTotal = PostProcess.keepCovered(Parameters.testGold, Parameters.testGoldCovered, 
					bestParseFile, bestParseCoveredFile);
		}
	}
	*/
	
	/*
	private void runBitPar() {
		try {
			String bitparApp = "/home/fsangati/SOFTWARE/BitPar_Last/bitpar";			
			String bitparArgs = "-s TOP -b " + Parameters.nBest + " -vp ";
			
			Process p = Runtime.getRuntime().exec(
						bitparApp + " " + bitparArgs + " grammar lexicon " + 
						Parameters.testExtrWords, null, new File(Parameters.outputPath));			
			redirectOutput(p, nbestParsesFile, true);			
		}
		catch (Exception e) {
			FileUtil.handleExceptions(e);
        }
	}
	*/
	
	/*
	private void runEvalB() {
		try {
			String evalbApp = "/home/fsangati/SOFTWARE/EVALB/evalb";
			String paramFolder = "/home/fsangati/SOFTWARE/EVALB/param/";
			String evalbArgs = "-p " + paramFolder + "collins" + Parameters.lengthLimitTest + ".prm";
			String evalbArgsUL = "-p " + paramFolder + "collins" + Parameters.lengthLimitTest + ".UL.prm";															
						
			File parserScoresFile = new File(Parameters.outputPath + "EvalB" + ".scores");
			File parserScoresULFile = new File(Parameters.outputPath + "EvalB" + ".scores.UL");
			File parserScoresCoveredFile = new File(Parameters.outputPath + "EvalB" + ".covered.scores");
			File parserScoresCoveredULFile = new File(Parameters.outputPath + "EvalB" + ".covered.scores.UL");			
									
			Process p = Runtime.getRuntime().exec(
						evalbApp + " " + evalbArgs + " " + Parameters.testGold + " " + bestParseFile);
			redirectOutput(p, parserScoresFile, false);
			
			p = Runtime.getRuntime().exec(
						evalbApp + " " + evalbArgsUL + " " + Parameters.testGold + " " + bestParseFile);
			redirectOutput(p, parserScoresULFile, false);
			
			if (Parameters.makeCoveredStatistics) {
				p = Runtime.getRuntime().exec(
							evalbApp + " " +  Parameters.testGoldCovered + " " + bestParseCoveredFile + " " + 
							evalbArgs, null, new File(Parameters.outputPath));
				redirectOutput(p, parserScoresCoveredFile, false);
				
				p = Runtime.getRuntime().exec(
							evalbApp + " " +  Parameters.testGoldCovered + " " + bestParseCoveredFile + " " 
							+ evalbArgsUL, null, new File(Parameters.outputPath));			
				redirectOutput(p, parserScoresCoveredULFile, false);
			}
			
			//printResults(coveredTotal, parserScoresFile, parserScoresULFile, 
			//		parserScoresCoveredFile, parserScoresCoveredULFile, "Results.EvalB.txt");			
		}
		catch (Exception e) {
			FileUtil.handleExceptions(e);
        }

	}
	*/
	
	/*
	private void runEvalF() {
		File parserScoresFile = new File(Parameters.outputPath + "EvalF" + ".scores");
		File evalFLog = new File(Parameters.outputPath + "EvalF" + ".log");
		File parserScoresULFile = new File(Parameters.outputPath + "EvalF" + ".scores.UL");
		File parserScoresCoveredFile = new File(Parameters.outputPath + "EvalF" + ".covered.scores");
		File evalFLogCovered = new File(Parameters.outputPath + "EvalF" + ".covered.log");
		File parserScoresCoveredULFile = new File(Parameters.outputPath + "EvalF" + ".covered.scores.UL");
		File resultFile = new File(Parameters.outputPath + "Results.EvalF.txt");
		
		EvalF.EvalC = Parameters.lengthLimitTest;
		
		float[] results = EvalC.staticEvalF(Parameters.testGold, bestParseFile, parserScoresFile, evalFLog, true);
		float[] resultsUL = EvalC.staticEvalF(Parameters.testGold, bestParseFile, parserScoresULFile, false);
		if (Parameters.makeCoveredStatistics) {
			new EvalC(Parameters.testGoldCovered, bestParseCoveredFile, parserScoresCoveredFile, evalFLogCovered, true);
			new EvalC(Parameters.testGoldCovered, bestParseCoveredFile, parserScoresCoveredULFile, false);
		}
				
		String resultReport = Arrays.toString(results) + "\n" + Arrays.toString(resultsUL);
		FileUtil.appendReturn(resultReport, resultFile);
		
		//printResults(coveredTotal, parserScoresFile, parserScoresULFile, 
		//		parserScoresCoveredFile, parserScoresCoveredULFile, "Results.EvalF.txt");
		
		if (Parameters.makeOracleStatistics) {
			File oracleFile = new File(Parameters.outputPath + "oracle.best");
			File oracleScore = new File(Parameters.outputPath + "EvalF" + ".oracle.scores");
			Oracle.processFile(nbestParsesFile, Parameters.testGold, oracleFile);
			new EvalC(Parameters.testGold, oracleFile, oracleScore, true);
		}
	}
	*/
	
	private static void redirectOutput(Process p, File outFile, boolean printProgress) {
	    try {            
	    	String s = null;
	    	BufferedReader stdInput = new BufferedReader(new 
	    	InputStreamReader(p.getInputStream()));
	        PrintWriter pw = FileUtil.getPrintWriter(outFile);

	        int sentenceIndex = 1;
	        if (printProgress) PrintProgressStatic.start("Running bitpar, outputted lines:");
	        while ((s = stdInput.readLine()) != null) {
	        	pw.println(s);
	        	if (!printProgress || s.length()==0 || s.startsWith("vitprob")) continue;
	        	PrintProgressStatic.next();
	        	sentenceIndex++;
	        }
	        if (printProgress) PrintProgressStatic.end();
	        p.getInputStream().close();
	        p.getOutputStream().close();
	        p.getErrorStream().close();
	        
	    	pw.close();
	    		    		        
	    } catch (IOException e) { FileUtil.handleExceptions(e); }
	}
	
	private static void printResults(int[] coveredTotal, File score, File scoreUL, 
		File scoreCovered, File scoreCoveredUL, String resultFile) {
		String prefix = "Bracketing FMeasure";
		String scoreValue = FileUtil.getLineInFileStartingWith(score, prefix).split("=")[1].trim();
		String scoreULValue = FileUtil.getLineInFileStartingWith(scoreUL, prefix).split("=")[1].trim();
		String scoreCoveredValue = FileUtil.getLineInFileStartingWith(scoreCovered, prefix).split("=")[1].trim();
		String scoreCoveredULValue = FileUtil.getLineInFileStartingWith(scoreCoveredUL, prefix).split("=")[1].trim();
		File resultCompressed = new File(Parameters.outputPath + resultFile);
		if (resultCompressed.length()==0) {
			String firstLine = "covered" + "\t" + "total" + "\t" + "score" + "\t" + "scoreUL" + "\t" + "scoreCovered" + 
			"\t" + "scoreCoveredUL" + "\n"; 
			FileUtil.appendReturn(firstLine, resultCompressed);
		}
		String results = coveredTotal[0] + "\t" + coveredTotal[1] + "\t" + scoreValue + "\t" + scoreULValue + 
			"\t" + scoreCoveredValue + "\t" + scoreCoveredULValue;
		FileUtil.appendReturn(results, resultCompressed);
		/*if (!Parameters.writeGlobalResults) return;
		if (Parameters.globalResults.length()==0) {
			String firstLine = 
				"Grammar_Type" + "\t" +
				"TestSet" + "\t" +
				"LLTraining" + "\t" +
				"LLTest" + "\t" +
				"Uk" + "\t" +
				"SemTag" + "\t" +
				"Repl#" + "\t" +
				"Skip120" + "\t" +
				"NPB" + "\t" +
				"SG" + "\t" +
				"cutTopRecursion" + "\t" +
				"delexicalize" + "\t" +
				"spine" + "\t" +
				"posTags" + "\t" +
				"removeTreesLimit" + "\t" +
				"smoothing" + "\t" +
				"smoothingFactor" + "\t" +
				"nBest" + "\t" +
				"TestSize" + "\t" +
				"NonParsed" + "\t" +
				"Total" + "\t" +
				"FscoreTotal" + "\t" +
				"FSTotalUL" + "\t" +
				"FSCovered" + "\t" +
				"FSCoveredUL" + "\t" +
				"Dir" + "\t";
			FileUtil.append(firstLine, Parameters.globalResults);				
		}
		String resultLine = 		
			Parameters.grammarType + "\t" + 
			Wsj.testSet + "\t" +
			Parameters.lengthLimitTraining + "\t" +
			Parameters.lengthLimitTest + "\t" +
			Parameters.ukLimit + "\t" +
			Parameters.semanticTags + "\t" +
			Parameters.replaceNumbers + "\t" +		
			Wsj.skip120TrainingSentences + "\t" +
			Wsj.transformNPbasal + "\t" +
			Wsj.transformSG + "\t" +
			Parameters.cutTopRecursion + "\t" + 
			Parameters.delexicalize + "\t" + 
			Parameters.spineConversion + "\t" +
			Parameters.posTagConversion + "\t" +
			Parameters.removeTreesLimit + "\t" +
			Parameters.smoothing + "\t" + 
			Parameters.smoothingFactor + "\t" +		
			Parameters.nBest + "\t" +
			Parameters.testCorpus.size() + "\t" +
			coveredTotal[0] + "\t" + 
			coveredTotal[1] + "\t" +
			scoreValue + "\t" +
			scoreULValue + "\t" +
			scoreCoveredValue + "\t" +
			scoreCoveredULValue + "\t" +			
			Parameters.outputPath;
		FileUtil.append(resultLine, Parameters.globalResults);*/
	}
	
	
}
