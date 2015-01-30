package tsg.parsingExp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import tsg.parsingExp.TSGparsingBitParFile.BitParThreadRunner;
import util.Utility;
import util.file.FileUtil;

public class TSGparsingBitParFileSeparate extends TSGparsingBitPar {
	
	String bitParBuildingFilesPath;
	
	public TSGparsingBitParFileSeparate(File trainingFile, File testFile, File fragmentFile, File outputDir) {
		super(trainingFile, testFile, fragmentFile, outputDir);
		bitParBuildingFilesPath = outputPath + "BitParWorkingDir/";
	}	
	
	public String getClassName() {
		return "TSGparsingBitParFileSeparate";
	}
	
	protected void parseWithBitPar() throws Exception {
		
		Parameters.reportLineFlush("Preparing threads");		
				
		File bitParBuildingFilesDir = new File(bitParBuildingFilesPath);
		bitParBuildingFilesDir.mkdir();
		
		int threadLength = Integer.toString(threads).length();
		int sentencesPerThreads = testSize / threads;
		int remainingSentences = testSize % threads;		
		
		ArrayList<String[]> testSentencesWords = getSentencesWords(testTreebank);
		ArrayList<String[]> originalSentencesWords = getSentencesWords(originalTestTreebank);
			
		BitParThreadRunner[] bitParThreadArray = new BitParThreadRunner[threads];
		int reachedIndex = 0;		
		
		for(int i=0; i<threads; i++) {
			int threadIndex = i+1;
			String number = Utility.padZero(threadLength, threadIndex);
			int sentencesThreads = sentencesPerThreads;
			if (i<remainingSentences) sentencesThreads++;
			int startingIndex = reachedIndex;
			reachedIndex += sentencesThreads;
			ArrayList<String[]> testSentencesWordsThread = new ArrayList<String[]>( 
				testSentencesWords.subList(startingIndex, reachedIndex));
			
			ArrayList<String[]> originalTestSentencesWordsThread = null;
			originalTestSentencesWordsThread = new ArrayList<String[]>(
					originalSentencesWords.subList(startingIndex, reachedIndex));
			bitParThreadArray[i] = new BitParThreadRunner(threadIndex, sentencesThreads,
					testSentencesWordsThread, originalTestSentencesWordsThread, number);			
		}		
		
		Parameters.reportLineFlush("Parsing with BitPar using " + threads + " threads");
		Parameters.reportLineFlush("Parsing " + testSize + " sentences:");
		submitMultithreadJob(bitParThreadArray);
		
		parsedOutputFiles = bitParThreadArray[0].metricOptimizer.makeFileOutputList(outputPath + "BITPAR_", ".mrg");
		parsedOutputFilesIdentifiers = bitParThreadArray[0].metricOptimizer.getIdentifiers();

		for(BitParThreadRunner t : bitParThreadArray) {
			t.metricOptimizer.appendResult(parsedOutputFiles);
		}

		Parameters.reportLineFlush("Finished Parsing.");
	}
	
	private void submitMultithreadJob(BitParThreadRunner[] bitParThreadArray) {		
		
		for(int i=0; i<threads-1; i++) {
			BitParThreadRunner t = bitParThreadArray[i];
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
	
	
	protected class BitParThreadRunner extends Thread {
		
		int threadIndex, numberSentences;		
		ArrayList<String[]> testSentencesWordsThread, originalTestSentencesWordsThread;
		String threadNumberString;
		MetricOptimizerArray metricOptimizer;
						
		public BitParThreadRunner (int threadIndex, int numberSentences, 
				ArrayList<String[]> testSentencesWordsThread, 
				ArrayList<String[]> originalTestSentencesWordsThread, 
				String threadNumberString) {
			
			this.threadIndex = threadIndex;
			this.numberSentences = numberSentences;			
			this.testSentencesWordsThread = testSentencesWordsThread;
			this.originalTestSentencesWordsThread = originalTestSentencesWordsThread;
			this.threadNumberString = threadNumberString;
			metricOptimizer = new MetricOptimizerArray(TSGparsingBitParFileSeparate.this);
								
		}

		public void run () {
			
			Process p = null;
			int maxIndexSize = Integer.toString(numberSentences).length();
			
			Iterator<String[]> testSentencesIterator = testSentencesWordsThread.iterator();			
			Iterator<String[]> originalTestSentencesIterator = originalTestSentencesWordsThread.iterator();
			
			for(int currentIndex = 0; currentIndex<numberSentences; currentIndex++) {
				
				String currentIndexString = Utility.padZero(maxIndexSize, currentIndex);
				String[] testSentence = testSentencesIterator.next();
				String[] originalSentence = originalTestSentencesIterator.next();
				
				File outputBitParNbestSentence = new File(bitParBuildingFilesPath + "outputBitPar_" + nBest + "best_" + 
						"thread" + threadNumberString + "_" + currentIndexString + ".txt");
				File flatFileForBitParSentence = new File(bitParBuildingFilesPath +
						"flatFileForBitPar_thread" + threadNumberString + "_" + currentIndexString + ".txt");	
				
				printBitParFlatSentence(testSentence, flatFileForBitParSentence);
				
				try {
					p = Runtime.getRuntime().exec(bitparCommandAndArgs + " " + flatFileForBitParSentence + " " + outputBitParNbestSentence);
					BufferedReader inputStd = new BufferedReader(new InputStreamReader(p.getInputStream()));
					BufferedReader inputErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					ReadInputStream inputStandardThread = new ReadInputStream(inputStd);
					ReadErrorStream inputErrorThread = new ReadErrorStream(inputErr);
					inputStandardThread.start();
					inputErrorThread.start();
					
					try {
						inputStandardThread.join();
						inputErrorThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}				
				    inputStd.close();			    
				    inputErr.close();	
				}
				catch (IOException err) {
				   err.printStackTrace();
				   return;
				}
				
				try {
					p.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}		    
			    				
				try {
					postProcessNbest(outputBitParNbestSentence, originalSentence);
				} catch (Exception err) {
				   err.printStackTrace();
				   Parameters.reportLineFlush(err.getMessage());
				   return;
				}
				
			}
						
		}
		
		private void postProcessNbest(File outputBitParNbestSentence, String[] originalSentence) throws Exception  {
			
			//PrintWriter pwFinal = FileUtil.getPrintWriter(ouputBitParPostProcessedSentence);
			
			Scanner scan = FileUtil.getScanner(outputBitParNbestSentence);			
			if (!scan.hasNextLine()) {
				Parameters.reportLineFlush("EMPTY FILE!!!!");
				scan.close();
				return;
			}						
			
			double lastReadProb = -1;						
			TSNodeLabel tree = null;
			
			metricOptimizer.prepareNextSentence(originalSentence);
						
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.equals("")) {
					metricOptimizer.storeCurrentBestParseTrees();
					continue;
				}
				if (line.charAt(0)=='(') {
					line = line.replaceAll("\\\\", "");
					try {
						tree = new TSNodeLabel(line);
					} catch (Exception e) {
						e.printStackTrace();
					}				
					if (!shortestDerivation)
						tree = postProcessParseTree(tree);
					metricOptimizer.addNewDerivation(tree, lastReadProb);
					continue;
				}
				if (line.startsWith(viterbProbPrefix)) {
					lastReadProb = Double.parseDouble(line.substring(viterbProbPrefixLength));
					continue;
				}												
				if (line.startsWith(noParseMessage)) {
					tree = dealWithNOParsedSentences(originalSentence);;
					metricOptimizer.addNewDerivation(tree, lastReadProb);
					continue;
				}		
				System.out.println("Unknown line in bitpar output: " + line);
			}
			scan.close();			
			//pwFinal.close();
		}
		
		protected class ReadErrorStream extends Thread {
		
			BufferedReader input;
			String reportPrefix;
			float parsingTime;
			
			public ReadErrorStream(BufferedReader input) {
				this.input = input;
				reportPrefix = "[stdErr_" + threadIndex + "]:";
			}
			
			public void run () {
				try {
					String line;
					while ( ((line = input.readLine()) != null) ) {												
						if (line.equals("")) continue;
						if (Character.isDigit(line.charAt(0))) {
							doneWithOneSentence();
							continue;
						}
						if (line.startsWith("reading") || line.startsWith("parameter")) continue;
						if (line.startsWith("finished")) continue;
						if (line.startsWith("raw")) {
							//parsingTime = Float.parseFloat(line.substring(line.lastIndexOf(' ')+1));							
							continue;
						}
						Parameters.reportLineFlush(reportPrefix + line);
				    }
				}
				catch (IOException err) {
				   err.printStackTrace();
				   Parameters.reportLineFlush(err.getMessage());
				   return;
				}				
				
			}
			
		} // end threadErrorStreamReader

		
		protected class ReadInputStream extends Thread {
			
			BufferedReader input;						
			String reportPrefix;
			
			
			public ReadInputStream(BufferedReader input) {
				this.input = input;				
				this.reportPrefix = "[stdOut_" + threadIndex + "]:";
			}
			
			public void run () {
				try {
					String line;
					while ( (line = input.readLine()) != null ) {						
						Parameters.reportLineFlush(reportPrefix + line);
				    }
				}
				catch (IOException err) {
				   err.printStackTrace();
				   Parameters.reportLineFlush(err.getMessage());
				   return;
				}
				
			}					
		
		} // end threadInputStreamReader		
		
	} // end parseThread
	



	
}
