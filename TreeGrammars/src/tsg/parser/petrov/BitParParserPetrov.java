package tsg.parser.petrov;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import tsg.utils.CleanPetrov;
import util.PrintProgress;


public class BitParParserPetrov extends Thread {

	final static String viterbProbPrefix = "vitprob=";
	final static String noParseMessage = "No parse for: ";
	final static int viterbProbPrefixLength = viterbProbPrefix.length();
	
	ArrayList<TSNodeLabel> testTreebank;
	ArrayList<TSNodeLabel> originalTestTreebank;
	int testSize;
	int threads;
	String outputPath;
	String topSymbol;
	File[] parsedOutputFiles;
	String[] parsedOutputFilesIdentifiers;
	String bitparCommandAndArgs;
	PrintProgress progress;
	
	public BitParParserPetrov(ArrayList<TSNodeLabel> testTreebank, 
			ArrayList<TSNodeLabel> originalTestTreebank, int threads, String outputPath,
			File bitparGrammarFile, File bitparLexiconFile, int nBest, String topSymbol) {
		this.testTreebank = testTreebank;
		this.originalTestTreebank = originalTestTreebank;
		this.threads = threads;
		this.outputPath = outputPath;
		this.testSize = testTreebank.size();
		this.progress = new PrintProgress("Sentence #:");
		this.topSymbol = topSymbol;
		bitparCommandAndArgs = Parameters.bitparApp + " -vp -b " + nBest + " -s " + topSymbol + 
		" " + bitparGrammarFile + " " + bitparLexiconFile;		
	}
	
	public File[] getParsedFiles() {
		return parsedOutputFiles;
	}
	
	public String[] getParsedFilesIdentifiers() {
		return parsedOutputFilesIdentifiers;
	}
	
	protected void runParser() throws Exception {
		
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
		
		submitMultithreadJob(bitParThreadArray);
				
		parsedOutputFiles = bitParThreadArray[0].metricOptimizer.makeFileOutputList(outputPath + "BITPAR_", ".mrg");
		parsedOutputFilesIdentifiers = bitParThreadArray[0].metricOptimizer.getIdentifiers();

		for(BitParThreadRunner t : bitParThreadArray) {
			t.metricOptimizer.appendResult(parsedOutputFiles);
		}

		Parameters.reportLineFlush("Finished Parsing.");
	}
	
	protected static ArrayList<String[]> getSentencesWords(ArrayList<TSNodeLabel> treebank) {
		ArrayList<String[]> result = new ArrayList<String[]>();
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();			
			String[] sentenceWords = new String[lex.size()];
			int index = 0;
			for(TSNodeLabel l : lex) {
				String word = l.label();
				sentenceWords[index] = word;
				index++;
			}
			result.add(sentenceWords);
		}
		return result;
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
			this.metricOptimizer = new MetricOptimizerArray();
		}

		public void run () {
			
			Process p = null;
			
			try {
				p = Runtime.getRuntime().exec(bitparCommandAndArgs);
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
				BufferedReader inputStd = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader inputErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				ReadInputStream inputStandardThread = new ReadInputStream(inputStd);
				ReadErrorStream inputErrorThread = new ReadErrorStream(inputErr);
				inputStandardThread.start();
				inputErrorThread.start();
				//boolean first = true;
				for(String[] flatSentence : flatSentencesForBitPar) {					
					for(String word : flatSentence) {
						output.write(word + "\n");
					}
					output.write("\n");					
					output.flush();
				}				
				output.write("\n");
				output.flush();
				output.close();
				
				inputStandardThread.join();
				inputErrorThread.join();
								
			    inputStd.close();			    
			    inputErr.close();
			}
			catch (Exception err) {
			   err.printStackTrace();
			   Parameters.reportLineFlush(err.getMessage());
			   return;
			}			
		}
		
		protected synchronized void doneWithOneSentence() {
			progress.next();		
		}		
		
		protected class ReadErrorStream extends Thread {
		
			BufferedReader input;
			String reportPrefix;
			int currentSentenceIndex;
			float parsingTime;
			int sentenceCounter;
			
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
							//System.out.println("Read error bitpar " + (++sentenceCounter));
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
			Iterator<String[]> originalTestIterator = null;
			String[] originalTestSentenceWords = null;
			double lastReadProb;
			int completedSentences;
			
			
			public ReadInputStream(BufferedReader input) {
				this.input = input;				
				originalTestIterator = originalTestSentencesWordsThread.iterator();
				originalTestSentenceWords = originalTestIterator.next();
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
					if (originalTestIterator.hasNext()) {
						originalTestSentenceWords = originalTestIterator.next();
						metricOptimizer.prepareNextSentence(originalTestSentenceWords);
					}
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
					tree = CleanPetrov.cleanPetrovTree(tree);
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

	protected TSNodeLabel dealWithNOParsedSentences(String[] originalTestSentenceWords) {
		TSNodeLabel result = TSNodeLabel.defaultWSJparse(originalTestSentenceWords, topSymbol);
		Parameters.reportLineFlush("No parse for sentence: " + 
				Arrays.toString(originalTestSentenceWords) + "\n\t" +
				"Default parse: " + result);
		return result;		
	}
}
