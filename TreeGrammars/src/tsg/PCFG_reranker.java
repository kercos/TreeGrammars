package tsg;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import backoff.BackoffModel_DivHistory;
import backoff.BackoffModel_Eisner;
import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.Box;
import tesniere.Conversion;
import tesniere.EvalTDS;
import tesniere.ProbModelStructureFiller_adv;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;

public class PCFG_reranker {

	public static ArrayList<TSNodeLabel> nextNBest(int nBest, Scanner s) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(nBest);
		int count = 0;
		while(s.hasNextLine() && count<nBest) {
			String line = s.nextLine();
			if (line.equals("")) return result;
			result.add(new TSNodeLabel(line));
			count++;
		}
		while(s.hasNextLine() && !s.nextLine().equals("")) {};
		return result;
	}
	
	public static  void trainFromCorpus(File trainingCorpus) throws Exception {
		ArrayList<TSNodeLabel> corpus = TSNodeLabel.getTreebank(trainingCorpus);
		for(TSNodeLabel t : corpus) {
			t.pruneSubTrees(TSNodeLabel.nullTag);
			t.addTop();
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				SymbolList rule = n.cfgRuleSymbol();
				SymbolString lhs = new SymbolString(n.label());
				Symbol[][] event = new Symbol[][]{{rule, lhs}};
				rulesModel.increaseInTables(event);
			}
		}		
	}
	
	public static double getProb(TSNodeLabel t) {
		ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
		double totalProb = 1d;
		for(TSNodeLabel n : nodes) {
			if (n.isLexical) continue;
			SymbolList rule = n.cfgRuleSymbol();
			SymbolString lhs = new SymbolString(n.label());
			Symbol[][] event = new Symbol[][]{{rule, lhs}};
			totalProb *= rulesModel.getCondProb(event);
		}
		return totalProb;
	}
	
	
	public static BackoffModel_Eisner rulesModel = null;
	//public static BackoffModel_DivHistory rulesModel = null;
	
	public static void getProbDistribution() throws Exception {
		rulesModel = new BackoffModel_Eisner(2, 0.005, 0.5, 0);
		File trainingCorpus = new File(Wsj.WsjConstBase + "ORIGINAL_READABLE_CIRCUMSTANTIALS_SEMTAGSOFF/" + "wsj-02-21.mrg");		
		trainFromCorpus(trainingCorpus);		
		
		String baseDir = Parameters.resultsPath + "TSG/PCFG_Reranker/";
		String outputDir = baseDir + "probDistrUnsorted/";
		//File oneBestFile = new File(baseDir + "wsj-22_oneBest_ADV.mrg");
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		
		ArrayList<TSNodeLabel> nBestTrees = null;
		File outputTotalFile = new File(outputDir + "totalProb.txt");
		PrintWriter pwTotal = FileUtil.getPrintWriter(outputTotalFile);
		for(int i=0; i<1700; i++) {			
			nBestTrees = nextNBest(1000, nBestScanner);
			File outputSentenceFile = new File(outputDir + "sentence_" + Utility.padZero(4, i+1) + ".txt");
			PrintWriter pwSentence = FileUtil.getPrintWriter(outputSentenceFile);
			double sum = 0;
			Vector<Double> probVector = new Vector<Double>(1000);
			for(TSNodeLabel t : nBestTrees) {								
				double p = getProb(t);
				probVector.add(p);				
				sum += p;								
			}						
			//Collections.sort(probVector);
			for(int j=probVector.size()-1; j>=0; j--) {
				pwSentence.println(probVector.get(j));
			}
			pwSentence.close();
			pwTotal.println(sum);
		}	
		pwTotal.close();		
	}
	
	public static void getProbGold() throws Exception {
		rulesModel = new BackoffModel_Eisner(2, 0.005, 0.5, 0);
		File trainingCorpus = new File(Wsj.WsjConstBase + "ORIGINAL_READABLE_CIRCUMSTANTIALS_SEMTAGSOFF/" + "wsj-02-21.mrg");		
		trainFromCorpus(trainingCorpus);
		String baseDir = Parameters.resultsPath + "TSG/PCFG_Reranker/";
		String outputDir = baseDir + "probDistr/";
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);
		File outpuFile = new File(outputDir + "goldProb.txt");
		PrintWriter pw = FileUtil.getPrintWriter(outpuFile);
		for(TSNodeLabel t : goldTreebank) {
			double p = getProb(t);
			pw.println(p);
		}
		pw.close();		
	}
	
	public static void printTables() throws Exception {
		rulesModel = new BackoffModel_Eisner(2, 0.005, 0.5, 0);
		File trainingCorpus = new File(Wsj.WsjConstBase + "ORIGINAL_READABLE_CIRCUMSTANTIALS_SEMTAGSOFF/" + "wsj-02-21.mrg");		
		trainFromCorpus(trainingCorpus);
		String baseDir = Parameters.resultsPath + "TSG/PCFG_Reranker/";
		File PCFGfile = new File(baseDir + "pcfg.txt");
		PrintStream ps = new PrintStream(PCFGfile);
		rulesModel.printTables(ps);
		ps.close();
		
	}
	
	public static void rerank(int nBest) throws Exception {
		
		rulesModel = new BackoffModel_Eisner(2, 0.005, 0.5, 0);
		
		/*int[][][] bol = new int[][][]{{{0,0}}};
		boolean[] skip = new boolean[]{false};
		int[][] group = new int[][]{{1}};		
		rulesModel = new BackoffModel_DivHistory(bol, skip, group, 0, 10, 0.000001);*/
		
		//File trainingCorpus = new File(Wsj.WsjOriginalCleanedSemTagsOff + "wsj-02-21.mrg");
		File trainingCorpus = new File(Wsj.WsjConstBase + "ORIGINAL_READABLE_CIRCUMSTANTIALS_SEMTAGSOFF/" + "wsj-02-21.mrg");		
		trainFromCorpus(trainingCorpus);
		
		String baseDir = Parameters.resultsPath + "TSG/PCFG_Reranker/";
		//File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_cleaned.mrg");
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");
		//File PCFGfile = new File(baseDir + "pcfg.txt");
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		
		//rulesModel.printTablesToFile(PCFGfile,0);
		
		//File goldFile = new File(baseDir + "wsj-22_gold.mrg");
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);				
		
		File rerankedFile = new File(baseDir + "wsj-22_reranked_" + nBest + "best.mrg");
		File rerankedFileEvalF = new File(baseDir + "wsj-22_reranked_" + nBest + "best.evalF");
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFile);
		
		int size = goldTreebank.size();
		int activelyReranked = 0;
		
		for(int i=0; i<size; i++) {
		
			ArrayList<TSNodeLabel> nBestTrees = nextNBest(nBest, nBestScanner);
			Iterator<TSNodeLabel> iter = nBestTrees.iterator();
			TSNodeLabel bestReranked = iter.next();			
			double maxProb = getProb(bestReranked);			
			
			boolean reranked = false;
			while(iter.hasNext()) {
				TSNodeLabel t = iter.next();
				double p = getProb(t);
				if (p>maxProb) {					
					maxProb = p;
					bestReranked = t;
					reranked = true;
				}
			}	
			if (reranked) activelyReranked++;
			pw.println(bestReranked.toString());
			
		}		
		
		pw.close();
		float[] rerankedFScore = EvalC.staticEvalC(goldFile, rerankedFile, rerankedFileEvalF);
		System.out.println("Actively Reranked: " + activelyReranked);
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(rerankedFScore));
		EvalTDS evalTDS = new EvalTDS(rerankedFile, goldFile);
		evalTDS.compareStructures();
		float[] TDSresults = evalTDS.getResults();
		System.out.println("F-Score\tUAS\tBDS\tBAS\tJDS");
		System.out.println(rerankedFScore[2]/100 + "\t" + TDSresults[0] + "\t" + TDSresults[1] + "\t" + TDSresults[2] + "\t" + TDSresults[3]);
	}
	
	public static void main(String[] args) throws Exception {
		int[] nBest = new int[]{1,5,10,100,500,1000};		
		for(int n : nBest) {
			System.out.println("n = " + n);
			rerank(n);
		}
		//getProbDistribution();
		//getProbGold();
		//printTables();
		
	}
	
}
