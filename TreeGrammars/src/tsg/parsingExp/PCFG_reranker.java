package tsg.parsingExp;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.EvalTDS;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationLeft_LC;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingFede;
import backoff.BackoffModel_Eisner;

public class PCFG_reranker {
	
	static UkWordMapping ukModel;
	static boolean markovBinarize;
	static TreeMarkoBinarization treeMarkovBinarizer;
	
	static double defaultProbUnknown = 1E-10;
	
	File trainingCorpus, goldFile, nBestFile; 
	String rerankedFilePrefix; 
	int nBest;
	Hashtable<String, Double> cfgRuleLogProb;

	public PCFG_reranker(File trainingCorpus, File goldFile, 
			File nBestFile, String rerankedFilePrefix, int nBest) throws Exception {
		
		this.trainingCorpus = trainingCorpus;
		this.goldFile = goldFile;
		this.nBestFile = nBestFile;
		this.rerankedFilePrefix = rerankedFilePrefix;
		this.nBest = nBest;
		
		this.trainFromCorpus();
		this.rerank();
	}
	
	public static ArrayList<TSNodeLabel> nextNBest(int nBest, Scanner s) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(nBest);
		int count = 0;
		while(s.hasNextLine() && count<nBest) {
			String line = s.nextLine();
			if (line.equals("")) return result;
			line = line.replaceAll("\\\\", "");
			TSNodeLabel t = new TSNodeLabel(line);
			if (UkWordMapping.ukThreashold>0) {
				t = ukModel.transformTree(t);
			}
			if (markovBinarize) {
				t = treeMarkovBinarizer.performMarkovBinarization(t);
			}
			result.add(t);
			count++;
		}
		while(s.hasNextLine() && !s.nextLine().equals("")) {};
		return result;
	}
	
	private void trainFromCorpus() throws Exception {
		Hashtable<String, int[]> cfgRuleFreqTable = new Hashtable<String, int[]>();
		Hashtable<String, int[]> lhsFreqTable = new Hashtable<String, int[]>();
		ArrayList<TSNodeLabel> corpus = TSNodeLabel.getTreebank(trainingCorpus);
		double minProb = Double.MAX_VALUE;
		if (UkWordMapping.ukThreashold>0) {
			ukModel.init(corpus, null);
			corpus = ukModel.transformTrainingTreebank();
		}
		if (markovBinarize) {
			corpus = treeMarkovBinarizer.markoBinarizeTreebank(corpus);
		}
		for(TSNodeLabel t : corpus) {
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();
				String lhs = n.label();
				Utility.increaseInTableInt(cfgRuleFreqTable, rule);
				Utility.increaseInTableInt(lhsFreqTable, lhs);
			}
		}
		cfgRuleLogProb = new Hashtable<String, Double>();
		for(Entry<String, int[]> e : cfgRuleFreqTable.entrySet()) {
			String rule = e.getKey();
			String lhs = rule.substring(0, rule.indexOf(' '));
			int freq = e.getValue()[0];
			int lhsFreq = lhsFreqTable.get(lhs)[0];
			Double prob = ((double) freq)/lhsFreq;
			if (prob<minProb) {
				minProb = prob;
			}
			cfgRuleLogProb.put(rule, Math.log(prob));
		}
		System.out.println("Min Rule prob: " + minProb);		
	}
	
	public double getProb(TSNodeLabel t) {
		ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
		double totalLogProb = 1d;
		for(TSNodeLabel n : nodes) {
			if (n.isLexical) continue;
			String rule = n.cfgRule();
			Double ruleLogProb = cfgRuleLogProb.get(rule);
			if (ruleLogProb==null) {
				//System.err.println("Rule not found: " + rule);
				ruleLogProb = defaultProbUnknown;				
			}
			totalLogProb += ruleLogProb;
		}
		return Math.exp(totalLogProb);		
	}
	
	
	private void rerank() throws Exception {
				
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);		
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();
		
		File rerankedFileEvalB = new File(rerankedFilePrefix + ".evalB");
		File rerankedFileEvalC = new File(rerankedFilePrefix + ".evalC");
		
		File rerankedFile = new File(rerankedFilePrefix + ".mrg");
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFile);
		
		int size = goldTreebank.size();
		int activelyReranked = 0;
		
		System.out.println("Gold Test TreeBank size: " + size);
		TSNodeLabel bestReranked = null;
		
		for(int i=0; i<size; i++) {
		
			ArrayList<TSNodeLabel> nBestTrees = nextNBest(nBest, nBestScanner);
			Iterator<TSNodeLabel> nbestIter = nBestTrees.iterator();
			TSNodeLabel goldTree = goldIter.next();
			ArrayList<Label> goldTreeLexLabels = goldTree.collectLexicalLabels();
			bestReranked = nbestIter.next();
			/*
			if (!nbestIter.hasNext()) {
				System.err.println("Error in  sentence: " + (i+1));
				System.err.println("Last read tree: " + bestReranked);
			}						
			TSNodeLabel goldTreeUk = ukModel.transformTree(goldTree);
			if (!goldTreeUk.collectLexicalLabels().equals(bestReranked.collectLexicalLabels())) {
				System.err.println("Missmatched tree in n best: " + (i+1));
				System.err.println("Expected tree: " + goldTreeUk);				
				System.err.println("Found tree: " + bestReranked);
				System.err.println(goldTreeUk.collectLexicalLabels());
				System.err.println(bestReranked.collectLexicalLabels());				
				return;
			}
			*/
			double maxProb = getProb(bestReranked);			
			
			boolean reranked = false;
			while(nbestIter.hasNext()) {
				TSNodeLabel t = nbestIter.next();
				double p = getProb(t);
				if (p>maxProb) {					
					maxProb = p;
					bestReranked = t;
					reranked = true;
				}
			}	
			if (reranked) activelyReranked++;
			if (markovBinarize) {
				bestReranked = treeMarkovBinarizer.undoMarkovBinarization(bestReranked);
			}
			if (UkWordMapping.ukThreashold>0) {
				bestReranked.changeLexLabels(goldTreeLexLabels);
			}
			pw.println(bestReranked.toString());
			
		}		
		
		pw.close();
		new EvalB(goldFile, rerankedFile, rerankedFileEvalB);
		EvalC eval = new EvalC(goldFile, rerankedFile, rerankedFileEvalC, null, true);	
		float[] results = eval.makeEval(); //recall precision fscore		
		System.out.println("Actively Reranked: " + activelyReranked);
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(results));
	}
	
	public static void main(String[] args) throws Exception  {
				
		File trainingCorpus = new File(args[0]); 
		File goldFile = new File(args[1]); 
		File nBestFile = new File(args[2]); 
		String rerankedFile = args[3];
		
		UkWordMapping.ukThreashold = 4;
		ukModel =  new UkWordMappingFede();
		
		markovBinarize = true;
		treeMarkovBinarizer = new TreeMarkoBinarizationLeft_LC();
		TreeMarkoBinarization.markH = 0;
		TreeMarkoBinarization.markV = 0;
		
		defaultProbUnknown = 1E-10;
		System.out.println("default Prob Unknown:" + defaultProbUnknown);
		
		int[] nBest = new int[]{1,5,10,100,500,1000};		
		for(int n : nBest) {
			System.out.println("n = " + n);
			String rerankedFilePrefix = rerankedFile + "_" + n;
			new PCFG_reranker(trainingCorpus, goldFile, nBestFile, rerankedFilePrefix, n);
		}
	}
	
	public static void main1(String[] args) throws Exception  {
		main1(new String[]{
			Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-02-21.mrg",
			Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-24.mrg",
			"/scratch/fsangati/RESULTS/ChiarniakParser/WSJ24/wsj24_EN_noAux/wsj-24_1000best_noAux_cleaned.mrg",
			"tmp/reranked"
		});
	}
	
}
