package tesniere;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import backoff.BackoffModel;
import backoff.BackoffModel_DivHistory;
import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.ProbModel.Event;
import tesniere.ProbModelStructureFiller.BoxBoundaryEvent;
import tsg.HeadLabeler;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;

public class ProbModelTagChunkerFiller extends ProbModel {		
	
	ProbModelTagChunker_smp chunkingModel;
	ProbModelStructureFiller_adv fillingModel;
	
	public ProbModelTagChunkerFiller(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);
	}
	
	public void initModelsDivHistory() {
		chunkingModel = new ProbModelTagChunker_smp(trainingTreebank);
		chunkingModel.initPosTaggingModel();
		chunkingModel.initChunkTaggingModelDivHistory();				
		
		fillingModel = new ProbModelStructureFiller_adv(trainingTreebank);
		fillingModel.initModelDivHistory();		
	}
	
	public void initModelsEisner() {
		chunkingModel = new ProbModelTagChunker_smp(trainingTreebank);
		chunkingModel.initPosTaggingModel();
		chunkingModel.initChunkTaggingModelEisner();				
		
		fillingModel = new ProbModelStructureFiller_adv(trainingTreebank);
		fillingModel.initModelEisner();		
	}
	
	public void train() {
		chunkingModel.train();
		fillingModel.train();
	}
	
	
	@Override
	public void extractEventsFromStructure(Box structure,
			ArrayList<Event> trainingEvents) {
		chunkingModel.extractEventsFromStructure(structure, trainingEvents);
		fillingModel.extractEventsFromStructure(structure, trainingEvents);		
	}
	
	public static ArrayList<TSNodeLabel> nextNBest(int nBest, Scanner s) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(nBest);
		int count = 0;
		while(s.hasNextLine() && count<nBest) {
			String line = s.nextLine();
			if (line.equals("")) return result;
			if (line.startsWith("vitprob=")) continue;
			result.add(new TSNodeLabel(line));
			count++;
		}
		while(s.hasNextLine() && !s.nextLine().equals("")) {};
		return result;
	}

	
	public static void rerank(int nBest) throws Exception {
		File treebankTrainFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_04_08_10"));		
		ArrayList<Box> treebankTrain = Conversion.getTesniereTreebank(treebankTrainFile, HL);
		
		ProbModelTagChunkerFiller tagChunkerModel = new ProbModelTagChunkerFiller(treebankTrain);
		tagChunkerModel.initModelsEisner();
		tagChunkerModel.train();
		
		/*File eventTablesFile = new File(Parameters.resultsPath + "TDS/Reranker/sec22_C/rerank_tagPosChunks/eventTable.txt");
		PrintStream ps = new PrintStream(eventTablesFile);
		tagChunkerModel.reportModelsTables(ps);
		ps.close();*/
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String outputDir = baseDir + "rerank_chunkFiller/";		
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");		
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");
		
		/*String outputDir = Parameters.resultsPath + "TSG/CFG/Nbest/TDS_Rerank/";		
		File goldFile = new File(outputDir + "wsj-22_TOP.mrg");
		File nBestFile = new File(outputDir + "BITPAR_OUTPUT_POSTPROCESSED.txt");*/
		
		File resultFile = new File(outputDir + "wsj-22_reranked_" + nBest + "results.txt");
		File rerankedFile = new File(outputDir + "wsj-22_reranked_" + nBest + "best.mrg");
		File oneBestFile = new File(outputDir + "wsj-22_oneBest.mrg");
		PrintWriter pwReranked = FileUtil.getPrintWriter(rerankedFile);
		PrintWriter pwOneBest = FileUtil.getPrintWriter(oneBestFile);
		PrintWriter pwResults = FileUtil.getPrintWriter(resultFile);
		
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();
		
		int size = goldTreebank.size();
		int[] oneBestScoreTotal = new int[2];
		int[] rerankedScoreTotal = new int[2];
		int inconsistentTaggedGold = 0;
		int inconsistentTaggedOneBest = 0;
		int inconsistentTaggedReranked = 0;
		int activelyReranked = 0;
		int[] depScoreOneBest = new int[]{0,0,0};
		int[] depScoreReranked = new int[]{0,0,0};
		
		for(int i=0; i<size; i++) {			
			ArrayList<TSNodeLabel> nBestTrees = nextNBest(nBest, nBestScanner);
			TSNodeLabel goldTree = goldIter.next();
			Box goldTDS = Conversion.getTesniereStructure(goldTree.clone(), HL);
			
			SymbolString[] goldChunkTags = Chunker.wordChunkLabeler(goldTDS);
			//ArrayList<Label> goldPosTags = goldTree.collectPreLexicalLabels();
			if (!Chunker.isConsistentChunking(goldChunkTags)) inconsistentTaggedGold++;			
			
			boolean reranked = false;
			boolean first = true;
			double maxProb = -1;
			SymbolString[] bestRerankedTags = null;
			//SymbolString[] oneBestRerankedTags = null;
			int[] oneBestScore = new int[2];
			int[] rerankedScore = new int[2];
			TSNodeLabel rerankedTree = null;
			
			for(TSNodeLabel t : nBestTrees) {
				Box b = Conversion.getTesniereStructure(t.clone(), HL);
				SymbolString[] chunkTags = Chunker.wordChunkLabeler(b);
				boolean isConsistent = Chunker.isConsistentChunking(chunkTags);				
				double p = tagChunkerModel.getProb(b);
				int[] scores = Chunker.getChunkTaggingScores(goldChunkTags, chunkTags);
				if (first) {					
					first = false;
					if (!isConsistent) inconsistentTaggedOneBest++;
					rerankedTree = t;
					maxProb = p;
					//oneBestRerankedTags = 
					bestRerankedTags = chunkTags;
					pwOneBest.println(t.toString());					
					oneBestScore = scores;
					Utility.arrayIntPlus(oneBestScore, oneBestScoreTotal);
					rerankedScore = scores;
					int[] evalDepOneBest = EvalTDS.getCatRawDepMatch(goldTDS, b);					
					Utility.arrayIntPlus(evalDepOneBest, depScoreOneBest);
					continue;
				}												
				if (p>maxProb) {					
					maxProb = p;					
					bestRerankedTags = chunkTags;
					rerankedScore = scores;
					rerankedTree = t;
					reranked = true;
				}
			}
			
			//ArrayList<Label> rerankedPosTags =  rerankedTree.collectPreLexicalLabels();
			pwReranked.println(rerankedTree.toString());
			if (reranked) activelyReranked++;
			if (!Chunker.isConsistentChunking(bestRerankedTags)) inconsistentTaggedReranked++;
			Utility.arrayIntPlus(rerankedScore, rerankedScoreTotal);
			
			Box bestRerankedTDS = Conversion.getTesniereStructure(rerankedTree, HL);
			int[] evalDepReranked = EvalTDS.getCatRawDepMatch(goldTDS, bestRerankedTDS);
			Utility.arrayIntPlus(evalDepReranked, depScoreReranked);
			
			/*int posTagsWrong = rerankedScore[1] - Utility.match(goldPosTags, rerankedPosTags);
			int diff = rerankedScore[0] - oneBestScore[0];
			String plus = diff<=0 ? "" : "+"; 
			reportLine((i+1) + "\t" + rerankedScore[1]  + "\t" + 
					oneBestScore[0] + " " + rerankedScore[0] + " " + " (" + plus + diff + ")" + "\t" +
					posTagsWrong, pwResults);*/
			//System.out.println("\t" + Arrays.toString(oneBestRerankedTags));
			//System.out.println("\t" + Arrays.toString(bestRerankedTags));
		}
		
		float scoreOneBest = (float) oneBestScoreTotal[0] / oneBestScoreTotal[1];
		float scoreReranked = (float) rerankedScoreTotal[0] / rerankedScoreTotal[1];
		reportLine("Chunking One Best scores: " + Arrays.toString(oneBestScoreTotal) + "\t" + scoreOneBest, pwResults);
		reportLine("Chunking Reranked scores: " + Arrays.toString(rerankedScoreTotal) + "\t" + scoreReranked, pwResults);
		reportLine("Total reranked: " + activelyReranked, pwResults);
		reportLine("Inconsistently tagged Gold: " + inconsistentTaggedGold, pwResults);
		reportLine("Inconsistently tagged One Best: " + inconsistentTaggedOneBest, pwResults);
		reportLine("Inconsistently tagged Reranked: " + inconsistentTaggedReranked, pwResults);
		pwReranked.close();
		pwOneBest.close();
		pwResults.close();
		
		File rerankedFileEvalF = new File(outputDir + "wsj-22_reranked_" + nBest + "best.evalF");
		
		System.out.println("Actively Reranked: " + activelyReranked);
		float[] rerankedFScore = EvalC.staticEvalC(goldFile, rerankedFile, rerankedFileEvalF);		
		System.out.println("One Best Dep Total Raw Dep: " + Arrays.toString(depScoreOneBest));
		System.out.println("Reranked Dep Total Raw Dep: " + Arrays.toString(depScoreReranked));
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(rerankedFScore));
		EvalTDS evalTDS = new EvalTDS(rerankedFile, goldFile);
		evalTDS.compareStructures();
		float[] TDSresults = evalTDS.getResults();
		System.out.println("F-Score\tUAS\tBDS\tBAS\tJDS");
		System.out.println(rerankedFScore[2]/100 + "\t" + TDSresults[0] + "\t" + TDSresults[1] + "\t" + TDSresults[2] + "\t" + TDSresults[3]);
	}
	
	public static void reportLine(String line, PrintWriter pw) {
		System.out.println(line);
		pw.println(line);
	}

	
	public static void main(String[] args) throws Exception {
		//printTables();
		int[] nBest = new int[]{1,5,100}; //{1,5,10,100,500,1000}; //1, 10, 100,		
		for(int n: nBest) {
			System.out.println("n = " + n);
			rerank(n);		
		}
		//rerankDebug(100,30);
	}


	
	

}
