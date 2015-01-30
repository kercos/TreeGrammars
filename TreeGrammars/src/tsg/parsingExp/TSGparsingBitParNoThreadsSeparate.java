package tsg.parsingExp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import util.Utility;
import util.file.FileUtil;

public class TSGparsingBitParNoThreadsSeparate extends TSGparsingBitPar {
	
	double lastReadProb;
	MetricOptimizerArray metricOptimizer;
	
	public TSGparsingBitParNoThreadsSeparate(File trainingFile, File testFile, File fragmentFile, File outputDir) {
		super(trainingFile, testFile, fragmentFile, outputDir);		
		metricOptimizer = new MetricOptimizerArray(this);
	}	
	
	public String getClassName() {
		return "TSGparsingBitParNoThreadsSeparate";
	}

	protected void parseWithBitPar() throws Exception {
		
		Parameters.reportLineFlush("Parsing with BitPar");
		
		ArrayList<String[]> testSentencesWords = getSentencesWords(testTreebank);		
		
		ArrayList<String[]> originalSentencesWords = null;
		
		originalSentencesWords = getSentencesWords(originalTestTreebank);
		
		Iterator<String[]> testSentenceIterator = testSentencesWords.iterator();
		Iterator<String[]> originalTestSentenceIterator = originalSentencesWords.iterator();
		
		while(testSentenceIterator.hasNext()) {
			
			String[] testSentenceWords = testSentenceIterator.next();
			String[] originalSentenceWords = originalTestSentenceIterator.next();
			
			try {					
				
				Process p = Runtime.getRuntime().exec(bitparCommandAndArgs);
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
				BufferedReader inputStd = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader inputErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				
				for(String word : testSentenceWords) {
					output.write(word + "\n");
				}
				output.write("\n");
				output.flush();
				output.close();
				
				String line;
				
				metricOptimizer.prepareNextSentence(originalSentenceWords);
				
			    while ((line = inputStd.readLine()) != null) { 
			    	// Parameters.reportLineFlush("BitParStdOut:" + line);
			    	processLine(line);
			    }
			    inputStd.close();
			    
			    while ((line = inputErr.readLine()) != null) {
			    	// Parameters.reportLineFlush("BitParStdErr:" + line);
			    }
			    inputErr.close();
			    
			    doneWithOneSentence();
			    
			}
			catch (Exception err) {
			   err.printStackTrace();
			   //pwFinal.close();
			   return;
			}
		}	
		
		parsedOutputFiles = metricOptimizer.makeFileOutputList(outputPath + "BITPAR_", ".mrg");
		parsedOutputFilesIdentifiers = metricOptimizer.getIdentifiers();
		metricOptimizer.appendResult(parsedOutputFiles);
		
		Parameters.reportLineFlush("Finished Parsing.");
		
		//pwFinal.close();
	}	
	
	
	private void processLine(String line) throws Exception  {				
		if (line.equals("")) {
			metricOptimizer.storeCurrentBestParseTrees();										
			return;
		}
		if (line.charAt(0)=='(') {
			TSNodeLabel tree = null;
			String fragmentConvertedString = null;
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
			TSNodeLabel tree = dealWithNOParsedSentences(metricOptimizer.getCurrentSentenceWords());
			metricOptimizer.addNewDerivation(tree, lastReadProb);
			return;
		}		
		Parameters.reportLineFlush("Unknown line in bitpar output: " + line);					
	}
	
	
	
}
