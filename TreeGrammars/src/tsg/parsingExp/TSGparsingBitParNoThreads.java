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

public class TSGparsingBitParNoThreads extends TSGparsingBitPar {
	
	MetricOptimizerArray metricOptimizer;
	
	public TSGparsingBitParNoThreads(File trainingFile, File testFile, File fragmentFile, File outputDir) {
		super(trainingFile, testFile, fragmentFile, outputDir);
		metricOptimizer = new MetricOptimizerArray(this);
	}	
	
	public String getClassName() {
		return "TSGparsingBitParNoThreads";
	}

	protected void parseWithBitPar() throws Exception {
		
		Parameters.reportLineFlush("Parsing with BitPar");
		
		String bitParBuildingFilesPath = outputPath + "BitParWorkingDir/";
		File bitParBuildingFilesDir = new File(bitParBuildingFilesPath);
		bitParBuildingFilesDir.mkdir();
		
		File ouputBitParFile = new File(bitParBuildingFilesPath + 
				"outputBitPar_" + nBest + "best.txt");		
		File flatFileForBitPar = new File(bitParBuildingFilesPath + 
				"flatFileForBitPar.txt");		
		
		ArrayList<String[]> testSentencesWords = getSentencesWords(testTreebank);
		printBitParFlatSentence(testSentencesWords, flatFileForBitPar);
		
		/*
		if (changeEncodingBitpar) {
			Parameters.reportLineFlush("Changing encoding for bitpar");
			FileUtil.changeFileEncoding(flatFileForBitPar, "UTF-8", "Cp1250");			
		}
		*/
		
		ArrayList<String[]> originalSentencesWords = getSentencesWords(originalTestTreebank);;
				
		try {					
			Process p = Runtime.getRuntime().exec(bitparCommandAndArgs +
					 " " + flatFileForBitPar + " " + ouputBitParFile);
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    while ((line = input.readLine()) != null) Parameters.reportLineFlush("BitParStdOut:" + line);
		    input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		    while ((line = input.readLine()) != null) Parameters.reportLineFlush("BitParStdErr:" + line);
		    input.close();
		}
		catch (Exception err) {
		   err.printStackTrace();
		}
			
		postProcessNbest(ouputBitParFile, originalSentencesWords);
		
		parsedOutputFiles = metricOptimizer.makeFileOutputList(outputPath + "BITPAR_", ".mrg");
		parsedOutputFilesIdentifiers = metricOptimizer.getIdentifiers();
		metricOptimizer.appendResult(parsedOutputFiles);
		
		/*
		if (changeEncodingBitpar) {
			for(File f : parsedOutputFiles) {
				FileUtil.changeFileEncoding(f, "Cp1250", "UTF-8");
			}
		}
		*/
		
		Parameters.reportLineFlush("Finished Parsing.");
				
	}	
	
	private void postProcessNbest(File ouputBitParFile, ArrayList<String[]> originalSentencesWords) throws Exception  {
		
		//PrintWriter pwFinal = FileUtil.getPrintWriter(mostProbableParsesFile);
		
		/*
		if (changeEncodingBitpar) {
			Parameters.reportLineFlush("Changing encoding from bitpar");
			FileUtil.changeFileEncoding(ouputBitParFile, "Cp1252", "UTF-8");
		}
		*/		
		
		Scanner scan = FileUtil.getScanner(ouputBitParFile);			
		if (!scan.hasNextLine()) {
			Parameters.reportLineFlush("EMPTY FILE!!!!");
			scan.close();
			return;
		}
		Iterator<String[]> originalTestIterator = null;
		String[] originalTestSentenceWords = null;
		
		originalTestIterator = originalSentencesWords.iterator();
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
				metricOptimizer.addNewDerivation(tree, lastReadProb);
				continue;
			}
			if (line.startsWith(viterbProbPrefix)) {
				lastReadProb = Double.parseDouble(line.substring(viterbProbPrefixLength));
				continue;
			}												
			if (line.startsWith(noParseMessage)) {
				tree = dealWithNOParsedSentences(originalTestSentenceWords);;
				metricOptimizer.addNewDerivation(tree, lastReadProb);
				continue;
			}		
			System.out.println("Unknown line in bitpar output: " + line);
		}		
		scan.close();			
		//pwFinal.close();
	}
	
	
}
