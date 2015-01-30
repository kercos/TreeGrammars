package tsg.parsingExp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import util.Utility;
import util.file.FileUtil;

public class TSGparsingMJ extends TSGparsingBitPar {
	
	File mjGrammar;
	
	public TSGparsingMJ(File trainingFile, File testFile, File fragmentFile, File outputDir) {
		super(trainingFile, testFile, fragmentFile, outputDir);		
	}	
	
	public String getClassName() {
		return "TSGparsingMJ";
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
		
		Parameters.reportLineFlush("Preparing threads");		
		
		String bitParBuildingFilesPath = outputPath + "BitParWorkingDir/";
		File bitParBuildingFilesDir = new File(bitParBuildingFilesPath);
		bitParBuildingFilesDir.mkdir();
		
		int threadLength = Integer.toString(threads).length();
		int sentencesPerThreads = testSize / threads;
		int remainingSentences = testSize % threads;		
		
		ArrayList<String[]> testSentencesWords = getSentencesWords(testTreebank);
		ArrayList<String[]> originalSentencesWords = getSentencesWords(originalTestTreebank);
			
		MJThreadRunner[] bitParThreadArray = new MJThreadRunner[threads];
		int reachedIndex = 0;		
		
		mjGrammar = new File(outputPath + "mjGrammar.txt");
		ConvertBitParGrammarInMJFormat.makeConversion(bibtpar_grammarFile, bitpar_lexiconFile, mjGrammar, topSymbol);
		
		for(int i=0; i<threads; i++) {
			int threadIndex = i+1;
			String number = Utility.padZero(threadLength, threadIndex);
			File outputBitParNbestBackupThread = new File(bitParBuildingFilesPath + 
					"outputBitPar_" + nBest + "best_" + number + ".txt");
			File outputBitParNbestPostProcessedThread = new File(bitParBuildingFilesPath + 
					"outputBitPar_" + nBest + "best_" + number + "postProcessed.txt");
			File flatFileForBitParThread = new File(bitParBuildingFilesPath +
					"flatFileForBitPar_part" + number + ".txt");
			int sentencesThreads = sentencesPerThreads;
			if (i<remainingSentences) sentencesThreads++;
			int startingIndex = reachedIndex;
			reachedIndex += sentencesThreads;
			ArrayList<String[]> testSentencesWordsThread = new ArrayList<String[]>( 
				testSentencesWords.subList(startingIndex, reachedIndex));
			printMJFlatSentence(testSentencesWordsThread, flatFileForBitParThread);
			ArrayList<String[]> originalTestSentencesWordsThread = null;
			originalTestSentencesWordsThread = new ArrayList<String[]>(
					originalSentencesWords.subList(startingIndex, reachedIndex));
			bitParThreadArray[i] = new MJThreadRunner(threadIndex, flatFileForBitParThread,
					outputBitParNbestBackupThread, outputBitParNbestPostProcessedThread, 
					originalTestSentencesWordsThread);			
		}		
		
		Parameters.reportLineFlush("Parsing with MJcky using " + threads + " threads");
		Parameters.reportLineFlush("Parsing " + testSize + " sentences:");
		submitMultithreadJob(bitParThreadArray);
		
		parsedOutputFiles = bitParThreadArray[0].metricOptimizer.makeFileOutputList(outputPath + "BITPAR_", ".mrg");
		parsedOutputFilesIdentifiers = bitParThreadArray[0].metricOptimizer.getIdentifiers();

		for(MJThreadRunner t : bitParThreadArray) {
			t.metricOptimizer.appendResult(parsedOutputFiles);
		}

		Parameters.reportLineFlush("Finished Parsing.");
	}
	
	private void submitMultithreadJob(MJThreadRunner[] bitParThreadArray) {		
		
		for(int i=0; i<threads-1; i++) {
			MJThreadRunner t = bitParThreadArray[i];
			t.start();						
		}
		bitParThreadArray[threads-1].run();
		
		for(int i=0; i<threads-1; i++) {
			try {
				bitParThreadArray[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}	
	}
	
	
	protected class MJThreadRunner extends Thread {
		
		int threadIndex;		
		File flatFileForMJ, outputBitParNbestThread, outputBitParNbestPostProcessedThread;		
		ArrayList<String[]> originalTestSentencesWordsThread;
		Iterator<String[]> originalTestSentencesIter;
		MetricOptimizerArray metricOptimizer;
		String currentParseTree = "";
						
		public MJThreadRunner (int threadIndex, File flatFileForMJ, 
				File outputBitParNbestThread, File outputBitParNbestPostProcessedThread,
				ArrayList<String[]> originalTestSentencesWordsThread) {
			
			this.threadIndex = threadIndex;
			this.flatFileForMJ = flatFileForMJ;
			this.outputBitParNbestThread = outputBitParNbestThread;
			this.outputBitParNbestPostProcessedThread = outputBitParNbestPostProcessedThread;
			this.originalTestSentencesWordsThread = originalTestSentencesWordsThread;
			originalTestSentencesIter = originalTestSentencesWordsThread.iterator();
			metricOptimizer = new MetricOptimizerArray(TSGparsingMJ.this);
		}

		public void run () {
			
			Process p = null;
			int parenthesis = 0;
			
			//Parameters.reportLineFlush("Starting thread: " + threadIndex);
			
			try {
				String command = mjCKYApp + " " + flatFileForMJ + " 0 " + mjGrammar;
				p = Runtime.getRuntime().exec(command);
				String line;
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			    while ((line = input.readLine()) != null) {			    	
			    	parenthesis += Utility.countParenthesis(line);
			    	currentParseTree += line;
			    	if (parenthesis==0) {
				    	processLine(currentParseTree);
				    	doneWithOneSentence();
				    	currentParseTree = "";
			    	}
			    }
			    input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			    while ((line = input.readLine()) != null) {
			    	Parameters.reportLineFlush("MJStdErr[" + threadIndex + "]:" + line);
			    }
			    input.close();			    
			}
			catch (Exception err) {
			   err.printStackTrace();
			   return;
			}
			
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}		    
		}
		
		private void processLine(String line) throws Exception  {
			
			String[] originalTestSentenceWords = originalTestSentencesIter.next();			
			
			if (line.charAt(0)=='(') {
				TSNodeLabel tree = null;
				line = line.replaceAll("\\\\", "");
				try {
					tree = new TSNodeLabel(line);
				} catch (Exception e) {
					e.printStackTrace();
					Parameters.reportError("Problem when process line: " + line);
					Parameters.reportError("Original sentence: " + Arrays.toString(originalTestSentenceWords));
					return;
				}								
				if (!shortestDerivation)
					tree = postProcessParseTree(tree);
				metricOptimizer.prepareNextSentence(originalTestSentenceWords);
				metricOptimizer.addNewDerivation(tree, 1);
				metricOptimizer.storeCurrentBestParseTrees();													
				return;
			}
			if (line.startsWith("parse_failure")) {				
				//Parameters.reportLine("Parsing Failure on sentence: " + Arrays.toString(originalTestSentenceWords));				
				TSNodeLabel tree = dealWithNOParsedSentences(originalTestSentenceWords);
				metricOptimizer.prepareNextSentence(originalTestSentenceWords);
				metricOptimizer.addNewDerivation(tree, 1);
				metricOptimizer.storeCurrentBestParseTrees();
				return;				
			}
			System.err.println("Unknown line in MJcky output: " + line);
		}
		
		
	} // end parseThread
	



	
}
