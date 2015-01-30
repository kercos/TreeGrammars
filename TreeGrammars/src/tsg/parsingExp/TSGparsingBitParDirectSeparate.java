package tsg.parsingExp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;


public class TSGparsingBitParDirectSeparate extends TSGparsingBitPar {

	public TSGparsingBitParDirectSeparate(File trainingFile, File testFile, File fragmentFile, File outputDir) {
		super(trainingFile, testFile, fragmentFile, outputDir);
	}	
	
	public String getClassName() {
		return "TSGparsingBitParDirectSeparate";
	}
	
	protected void parseWithBitPar() throws Exception {
		
		Parameters.reportLineFlush("Preparing threads");		
		
		int sentencesPerThreads = testSize / threads;
		int remainingSentences = testSize % threads;
		
		ArrayList<String[]> testSentencesWords = getSentencesWords(testTreebank);
		ArrayList<String[]> originalSentencesWords = getSentencesWords(originalTestTreebank);
			
		BitParThreadRunner[] bitParThreadArray = new BitParThreadRunner[threads];
		int reachedIndex = 0;		
		
		for(int i=0; i<threads; i++) {
			int threadIndex = i+1;
			int sentencesThreads = sentencesPerThreads;
			if (i<remainingSentences) sentencesThreads++;
			int startingIndex = reachedIndex;
			reachedIndex += sentencesThreads;
			ArrayList<String[]> testSentencesWordsThread = new ArrayList<String[]>( 
				testSentencesWords.subList(startingIndex, reachedIndex));										
			ArrayList<String[]> originalTestSentencesWordsThread = null;
			originalTestSentencesWordsThread = new ArrayList<String[]>(
					originalSentencesWords.subList(startingIndex, reachedIndex));
			bitParThreadArray[i] = new BitParThreadRunner(threadIndex, startingIndex,
					testSentencesWordsThread, originalTestSentencesWordsThread);			
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
		
		int threadIndex, startSentenceIndex;		
		ArrayList<String[]> flatSentencesForBitPar, originalTestSentencesWordsThread;
		MetricOptimizerArray metricOptimizer;
						
		public BitParThreadRunner (int threadIndex, int startSentenceIndex, 
				ArrayList<String[]> testSentencesWordsThread, ArrayList<String[]> originalTestSentencesWordsThread) {
			this.threadIndex = threadIndex;
			this.startSentenceIndex = startSentenceIndex;
			this.flatSentencesForBitPar = testSentencesWordsThread;									
			this.originalTestSentencesWordsThread = originalTestSentencesWordsThread;
			metricOptimizer = new MetricOptimizerArray(TSGparsingBitParDirectSeparate.this);			
		}

		public void run () {
						
			Process p = null;
			int currentSentenceIndex = 0;
			
			Iterator<String[]> originalTestIterator = null;
			String[] originalTestSentenceWords = null;
			
			originalTestIterator = originalTestSentencesWordsThread.iterator();
			originalTestSentenceWords = originalTestIterator.next();						
			
			for(String[] flatSentence : flatSentencesForBitPar) {			
				
				try {
					p = Runtime.getRuntime().exec(bitparCommandAndArgs);
					BufferedWriter output = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
					BufferedReader inputStd = new BufferedReader(new InputStreamReader(p.getInputStream()));
					BufferedReader inputErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					ReadInputStream inputStandardThread = new ReadInputStream(inputStd, currentSentenceIndex, originalTestSentenceWords);
					ReadErrorStream inputErrorThread = new ReadErrorStream(inputErr);
					inputStandardThread.start();
					inputErrorThread.start();
					
					for(String word : flatSentence) {
						output.write(word + "\n");
					}
					
					output.write("\n");
					output.flush();
					output.close();
					
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
				   Parameters.reportLineFlush(err.getMessage());
				   return;
				}

				currentSentenceIndex++;
				if (originalTestIterator.hasNext()) {
					originalTestSentenceWords = originalTestIterator.next();				
				}
			}
		}
		
		protected class ReadErrorStream extends Thread {
			
			BufferedReader input;
			String reportPrefix;
			int currentSentenceIndex;
			float parsingTime;
			
			public ReadErrorStream(BufferedReader input) {
				this.currentSentenceIndex = startSentenceIndex;
				this.input = input;
				reportPrefix = "[stdOutErr_" + threadIndex + "]:";
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
			
		}

		
		protected class ReadInputStream extends Thread {
			
			BufferedReader input;
			double lastReadProb;
			String[] originalTestSentenceWords;						
			
			
			public ReadInputStream(BufferedReader input, int currentSentenceIndex, String[] originalTestSentenceWords) {
				this.input = input;				
				this.originalTestSentenceWords = originalTestSentenceWords;			
				metricOptimizer.prepareNextSentence(originalTestSentenceWords);
			}
			
			public void run () {
				try {
					String line;
					while ( (line = input.readLine()) != null ) {						
						processLine(line);
				    }
				}
				catch (Exception err) {
				   err.printStackTrace();
				   Parameters.reportLineFlush(err.getMessage());
				   return;
				}
				
			}
						
			private void processLine(String line) throws Exception  {
				if (line.equals("")) {				
					metricOptimizer.storeCurrentBestParseTrees();															
					return;
				}
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
					metricOptimizer.addNewDerivation(tree, lastReadProb);
					return;
				}
				if (line.startsWith(viterbProbPrefix)) {
					lastReadProb = Double.parseDouble(line.substring(viterbProbPrefixLength));
					return;
				}
				if (line.startsWith(noParseMessage)) {
					TSNodeLabel tree = dealWithNOParsedSentences(originalTestSentenceWords);;
					metricOptimizer.addNewDerivation(tree, lastReadProb);
					return;
				}		
				Parameters.reportLineFlush("Unknown line in bitpar output: " + line);					
			}		
		
		} // end threadInputStreamReader
		
	} // end parseThread
	
}
