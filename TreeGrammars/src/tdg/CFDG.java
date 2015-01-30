package tdg;
import java.util.*;
import java.io.*;

import tdg.corpora.DepCorpus;
import util.*;

public class CFDG {
	
	public ArrayList<TDNode> treebank;
	Hashtable<String, Integer> CFDGtable;
	boolean direction;	 
	
	public CFDG(File inputFile, boolean direction) {
		this.direction = direction;
		readTreebankFromFile(inputFile);	
	}
	
	public void readCFDGfromCorpus() {
		CFDGtable = new Hashtable<String, Integer>();
		for(TDNode TDN : treebank) {
			List<String> CFDRules = TDN.getCFDRListPos(this.direction);
			for(String CFDR : CFDRules) Utility.increaseStringInteger(CFDGtable, CFDR, 1);
		}
	}
	
	public void readTreebankFromFile(File inputFile) {
		treebank = DepCorpus.readTreebankFromFile(inputFile);
	}
	
	public void printTreebankToFile(File outputFile) {
		Utility.hashtableOrderedToFile(CFDGtable, outputFile);
	}
	
	public static void coverageCFDG(File trainFile, File evalFile) {
		boolean direction = false;
		CFDG grammarTrain = new CFDG(trainFile, direction);
		grammarTrain.readCFDGfromCorpus();
		CFDG grammarEval = new CFDG(evalFile, direction);
		
		Set<String> trainingCFDRules = grammarTrain.CFDGtable.keySet();
		int unmatchedSentences = 0;
		ArrayList<Integer> unmatchedSentencesLength = new ArrayList<Integer>(); 
		
		grammarEval.CFDGtable = new Hashtable<String,Integer>();
		for(TDNode TDN : grammarEval.treebank) {
			List<String> CFDRules = TDN.getCFDRListPos(grammarEval.direction);
			boolean unmatched = false;
			for(String CFDR : CFDRules) {				
				Utility.increaseStringInteger(grammarEval.CFDGtable, CFDR, 1);				
				if (!trainingCFDRules.contains(CFDR)) {			
					unmatched = true;
					unmatchedSentencesLength.add(TDN.length());
				}				
			}
			if (unmatched) unmatchedSentences++;
		}
		
		int trainTotalTypes = Utility.countTotalTypesInTable(grammarTrain.CFDGtable);
		int trainTotalTokens = Utility.countTotalTokensInTable(grammarTrain.CFDGtable);
		int evalTotalTypes = Utility.countTotalTypesInTable(grammarEval.CFDGtable);
		int evalTotalTokens = Utility.countTotalTokensInTable(grammarEval.CFDGtable);
		
		grammarEval.CFDGtable.keySet().removeAll(grammarTrain.CFDGtable.keySet());
		
		File outputFile = new File("/home/fsangati/CORPUS/WSJ/DEPWSJ/CFDG/wsj-22_uncovered_undir_nopunct.cfdg");
		Utility.hashtableOrderedToFile(grammarEval.CFDGtable, outputFile);
		
		int unmatchedEvalTotalTypes = Utility.countTotalTypesInTable(grammarEval.CFDGtable);
		int unmatchedEvalTotalTokens = Utility.countTotalTokensInTable(grammarEval.CFDGtable);
		float unmatchedEvalPercentTypes = (float) unmatchedEvalTotalTypes / evalTotalTypes;
		float unmatchedEvalPercentTokens = (float) unmatchedEvalTotalTokens / evalTotalTokens;
		String report = "\nCOVERAGE ANALYSIS:\n" +
						"Training Corpus initial total types|tokens: " + "\t" +
						trainTotalTypes + "\t" + trainTotalTokens + "\n" +
						"Eval Corpus initial total types|tokens: " + "\t" +
						evalTotalTypes + "\t" + evalTotalTokens + "\n" +
						"Eval Corpus unmatched total types|tokens: " + "\t" +
						unmatchedEvalTotalTypes + "\t" + unmatchedEvalTotalTokens + "\n" +
						"Eval Corpus unmatched % types|tokens: " + "\t" +
						unmatchedEvalPercentTypes + "\t" + unmatchedEvalPercentTokens + "\n" +
						"Eval Corpus unmatched sentences|total: " + "\t" +
						unmatchedSentences + "\t" + grammarEval.treebank.size() + "\n" +
						"Unmatched Sentences Length classes :\n" +
						Utility.printIntegerListClasses(unmatchedSentencesLength, 10);
		System.out.println(report);
	}
	
	public static void ruleAnalysis(File trainFile) {
		boolean direction = false;
		CFDG grammarTrain = new CFDG(trainFile, direction);
		grammarTrain.readCFDGfromCorpus();
		
		Set<String> trainingCFDRules = grammarTrain.CFDGtable.keySet();
		ArrayList<Integer> oneOccuranceSentencesLength = new ArrayList<Integer>(); 
		
		for(TDNode TDN : grammarTrain.treebank) {
			List<String> CFDRules = TDN.getCFDRListPos(grammarTrain.direction);
			for(String CFDR : CFDRules) {				
				int count = grammarTrain.CFDGtable.get(CFDR);	
				if (count==1) {			
					oneOccuranceSentencesLength.add(TDN.length());
				}				
			}
		}
		
		int trainTotalTypes = Utility.countTotalTypesInTable(grammarTrain.CFDGtable);
		int trainTotalTokens = Utility.countTotalTokensInTable(grammarTrain.CFDGtable);
		
		File outputFileFreq = new File("/home/fsangati/CORPUS/WSJ/DEPWSJ/CFDG/wsj-02-21.undir.cfdg.freq");
		File outputFileRank = new File("/home/fsangati/CORPUS/WSJ/DEPWSJ/CFDG/wsj-02-21.undir.cfdg.rank");
		Utility.hashtableOrderedToFile(grammarTrain.CFDGtable, outputFileFreq);
		Utility.hashtableRankedToFile(grammarTrain.CFDGtable, outputFileRank);
		
		String report = "\nCOVERAGE ANALYSIS:\n" +
						"Training Corpus initial total types|tokens: " + "\t" +
						trainTotalTypes + "\t" + trainTotalTokens + "\n" +
						"One Occurances Sentence Length classes :\n" +
						Utility.printIntegerListClasses(oneOccuranceSentencesLength, 10);
		System.out.println(report);
	}
	
	public static void main(String[] args) {
		String corpusBase = "/home/fsangati/CORPUS/";
		//String CorpusBase = "/Users/fedya/CORPUS/";
		File trainFile = new File(corpusBase + "WSJ/DEPWSJ/wsj-02-21.dep");
		//File evalFile = new File(corpusBase + "WSJ/DEPWSJ/wsj-22.dep");
		//File outputFile = new File(corpusBase + "WSJ/DEPWSJ/CFDG/wsj-02-21_undir.cfdg");
		//CFDG grammar = new CFDG(trainFile, false);
		//grammar.readCFDGfromCorpus();
		//grammar.printTreebankToFile(outputFile);
		//coverageCFDG(trainFile, evalFile);
		ruleAnalysis(trainFile);
	}

}
