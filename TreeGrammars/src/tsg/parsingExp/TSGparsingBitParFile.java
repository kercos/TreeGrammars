package tsg.parsingExp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import util.Utility;
import util.file.FileUtil;

public class TSGparsingBitParFile extends TSGparsingBitPar {
	
	public TSGparsingBitParFile(File trainingFile, File testFile, File fragmentFile, File outputDir) {
		super(trainingFile, testFile, fragmentFile, outputDir);		
	}	
	
	public String getClassName() {
		return "TSGparsingBitParFile";
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
			
		BitParThreadRunner[] bitParThreadArray = new BitParThreadRunner[threads];
		int reachedIndex = 0;		
		
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
			printBitParFlatSentence(testSentencesWordsThread, flatFileForBitParThread);
			ArrayList<String[]> originalTestSentencesWordsThread = null;
			originalTestSentencesWordsThread = new ArrayList<String[]>(
					originalSentencesWords.subList(startingIndex, reachedIndex));
			bitParThreadArray[i] = new BitParThreadRunner(threadIndex, flatFileForBitParThread,
					outputBitParNbestBackupThread, outputBitParNbestPostProcessedThread, 
					originalTestSentencesWordsThread);			
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
		
		int threadIndex;		
		File flatFileForBitPar, outputBitParNbestThread, outputBitParNbestPostProcessedThread;		
		ArrayList<String[]> originalTestSentencesWordsThread;
		MetricOptimizerArray metricOptimizer;
						
		public BitParThreadRunner (int threadIndex, File flatFileForBitPar, 
				File outputBitParNbestThread, File outputBitParNbestPostProcessedThread,
				ArrayList<String[]> originalTestSentencesWordsThread) {
			
			this.threadIndex = threadIndex;
			this.flatFileForBitPar = flatFileForBitPar;
			this.outputBitParNbestThread = outputBitParNbestThread;
			this.outputBitParNbestPostProcessedThread = outputBitParNbestPostProcessedThread;
			this.originalTestSentencesWordsThread = originalTestSentencesWordsThread;
			metricOptimizer = new MetricOptimizerArray(TSGparsingBitParFile.this);
		}

		public void run () {
			
			Process p = null;
			
			try {
				p = Runtime.getRuntime().exec(bitparCommandAndArgs + " " + flatFileForBitPar + " " + outputBitParNbestThread);
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
				postProcessNbest();
			} catch (Exception err) {
			   err.printStackTrace();
			   Parameters.reportLineFlush(err.getMessage());
			   return;
			}
		}
		
		private void postProcessNbest() throws Exception  {
			
			PrintWriter pw = FileUtil.getPrintWriter(outputBitParNbestPostProcessedThread);
			
			//File nBestCleaned = new File(outputBitParNbestBackupThread + "_cleaned");
			//PrintWriter pwCleaned = FileUtil.getPrintWriter(nBestCleaned);
			
			Scanner scan = FileUtil.getScanner(outputBitParNbestThread);			
			if (!scan.hasNextLine()) {
				Parameters.reportLineFlush("EMPTY FILE!!!!");
				scan.close();
				return;
			}
			Iterator<String[]> originalTestIterator = null;
			String[] originalTestSentenceWords = null;
			
			originalTestIterator = originalTestSentencesWordsThread.iterator();
			originalTestSentenceWords = originalTestIterator.next();
			
			double lastReadProb = -1;						
			TSNodeLabel tree = null;
			metricOptimizer.prepareNextSentence(originalTestSentenceWords);
						
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.equals("")) {
					metricOptimizer.storeCurrentBestParseTrees();
					if (originalTestIterator.hasNext()) {
						originalTestSentenceWords = originalTestIterator.next();
						metricOptimizer.prepareNextSentence(originalTestSentenceWords);
					}
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
					pw.println(tree);
					metricOptimizer.addNewDerivation(tree, lastReadProb);
					continue;
				}
				if (line.startsWith(viterbProbPrefix)) {
					lastReadProb = Double.parseDouble(line.substring(viterbProbPrefixLength));
					pw.println(line);
					continue;
				}												
				if (line.startsWith(noParseMessage)) {
					tree = dealWithNOParsedSentences(originalTestSentenceWords);;
					metricOptimizer.addNewDerivation(tree, lastReadProb);
					pw.println(line);
					continue;
				}		
				System.out.println("Unknown line in bitpar output: " + line);
			}
			scan.close();			
			pw.close();
			//pwCleaned.close();
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
