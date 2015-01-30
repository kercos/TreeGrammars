package tsg.parser.petrov;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.metrics.MetricOptimizerArray;
import tsg.metrics.ParseMetricOptimizer;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class ComparePetroBitPar extends Thread {

	static double minProbRule;
	static boolean normalize;
	
	static String outputPath;
	static File petrovGrammarFile, petrovLexiconFile;
	static File petrovParsedFile, bitparParsedFile;	
	
	Hashtable<String,Double> grammarRulesLogProb;
	Hashtable<String,Double> lexRulesLogProb;
	
	public void run() {		
		convertGrammar();
		convertLexicon();
		if (normalize)			
			normalizeAndMakeLog();
		else
			makeLog();
		try {
			makeComparison();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	

	private void normalizeAndMakeLog() {		
		
		for(int i=0; i<2; i++) {
		
			Hashtable<String, Double> rulesProb = i==0 ? grammarRulesLogProb : lexRulesLogProb;
			Hashtable<String, double[]> rootProb = new Hashtable<String, double[]>();
			for(Entry<String,Double> e : rulesProb.entrySet()) {
				String rule = e.getKey();
				double prob = e.getValue();
				String lhs = rule.split("\\s")[0];
				Utility.increaseInTableDoubleArray(rootProb, lhs, prob);				
			}
			Hashtable<String, Double> newRulesLogProb = new Hashtable<String, Double>();
			for(Entry<String,Double> e : rulesProb.entrySet()) {
				String rule = e.getKey();
				double prob = e.getValue();
				String lhs = rule.split("\\s")[0];
				double lhsProb = rootProb.get(lhs)[0];
				double logProb = Math.log(prob/lhsProb);
				newRulesLogProb.put(rule,logProb);
			}
			if (i==0)
				grammarRulesLogProb = newRulesLogProb;
			else 
				lexRulesLogProb = newRulesLogProb;
									
		}
		
	}
	
	private void makeLog() {		
		
		for(int i=0; i<2; i++) {
		
			Hashtable<String, Double> rulesProb = i==0 ? grammarRulesLogProb : lexRulesLogProb;
			
			Hashtable<String, Double> newRulesLogProb = new Hashtable<String, Double>();
			for(Entry<String,Double> e : rulesProb.entrySet()) {
				String rule = e.getKey();
				double prob = e.getValue();
				String lhs = rule.split("\\s")[0];
				double logProb = Math.log(prob);
				newRulesLogProb.put(rule,logProb);				
			}
			
			if (i==0)
				grammarRulesLogProb = newRulesLogProb;
			else 
				lexRulesLogProb = newRulesLogProb;
									
		}
		
	}



	/**
	 * A line of Petrov Grammar file looks like this:
	 * S^g_1 -> PP^g_21 NP^g_45 4.1686166964667563E-10
	 */
	private void convertGrammar() {				
		
		grammarRulesLogProb = new Hashtable<String,Double>();
		
		Scanner grammarScan = FileUtil.getScanner(petrovGrammarFile);				
		int smallProbRuleSkipped = 0;
		int totalAcceptedRules = 0;
		while(grammarScan.hasNextLine()) {
			String line = grammarScan.nextLine();
			String[] lineSplit = line.split("\\s");			
			int length = lineSplit.length;			
			double prob = Double.parseDouble(lineSplit[length-1]);		
			if (prob<minProbRule) {
				smallProbRuleSkipped++;
				continue;			
			}
			totalAcceptedRules++;
			String lhs = convertLable(lineSplit[0]);
			boolean unaryRule = length == 4;
			if (unaryRule) {
				String rhsChild = convertLable(lineSplit[2]);
				String rule = lhs + " " + rhsChild;
				grammarRulesLogProb.put(rule, prob);
			}
			else {
				String rule = lhs;
				for(int i=2; i<length-1; i++) {
					String rhsChild = convertLable(lineSplit[i]);
					rule += " " + rhsChild;
				}				
				grammarRulesLogProb.put(rule, prob);
			}			
		}
		System.out.println("Skipped small prob internal rules: " + smallProbRuleSkipped);		
		System.out.println("Total accepted internal rules: " + totalAcceptedRules);
	}
	
	static String regexConvertLabel = "\\^g";
		
	private static String convertLable(String label) {
		return label.replace('_', '-').replaceFirst(regexConvertLabel, "");				
	}

	/**
	 * A line of Petrov Lexicon file looks like this:
	 * IN via [1.6034755262090546E-14, 3.9819306306509576E-14, ...]
	 */
	private void convertLexicon() {		
		
		lexRulesLogProb = new Hashtable<String,Double>();
		int smallProbRuleSkipped = 0;
		int totalAcceptedRules = 0;
		
		Scanner lexiconScan = FileUtil.getScanner(petrovLexiconFile); 
		while(lexiconScan.hasNextLine()) {
			String line = lexiconScan.nextLine();
			line = line.replaceAll("\\\\", "");			
			String[] lineSplit = line.split("\\s");
			int length = lineSplit.length;
			String pos = lineSplit[0];
			String lex = lineSplit[1];
			
			int index = 0;
			for(int i=2; i<length; i++) {				
				double prob = cleanProb(lineSplit[i]);
				if (prob>minProbRule) {
					totalAcceptedRules++;
					String refinedPos = pos + "-" + index;
					String rule = refinedPos + " " + lex;
					lexRulesLogProb.put(rule, prob);
				}
				else 
					smallProbRuleSkipped++;
				index++;
			}
			
		}
		System.out.println("Skipped small prob lex rules: " + smallProbRuleSkipped);		
		System.out.println("Total accepted lex rules: " + totalAcceptedRules);

	}

	static String regexCleanProb = "[\\[\\]\\,]";
	private static double cleanProb(String p) {
		return Double.parseDouble(p.replaceAll(regexCleanProb, ""));
	}
	

	private void makeComparison() throws Exception {
		
		ArrayList<TSNodeLabel> petrovTreebank = TSNodeLabel.getTreebank(petrovParsedFile);
		ArrayList<TSNodeLabel> bitparTreebank = TSNodeLabel.getTreebank(bitparParsedFile);
		if (petrovTreebank.size() != bitparTreebank.size()) {
			System.err.println("Sizes differ");
		}
		
		File logFile = new File(outputPath + "compareReport.log");
		PrintWriter pw = FileUtil.getPrintWriter(logFile);
		
		Iterator<TSNodeLabel> petrovIter =  petrovTreebank.iterator();
		Iterator<TSNodeLabel> bitparIter =  bitparTreebank.iterator();
		int differInLex = 0;
		int equalCounter = 0;
		int totalEqualLex = 0;
		int[] petrovBitparEqual = new int[]{0,0,0};
		int index = 0;
		while(petrovIter.hasNext()) {
			index++;
			TSNodeLabel petrovTree = petrovIter.next();
			TSNodeLabel bitparTree = bitparIter.next();
			if (!petrovTree.sameLexLabels(bitparTree)) {
				//differing lables: unknown words
				differInLex++;
				continue;				
			}
			totalEqualLex++;
			if (petrovTree.equals(bitparTree)) {
				equalCounter++;
				continue;				
			}
			int winner = compareProbTrees(petrovTree, bitparTree, pw, index);
			petrovBitparEqual[winner]++;
		}
		pw.close();
		System.out.println("Total differ in lex: " + differInLex);
		System.out.println("Total equal lex: " + totalEqualLex);
		System.out.println("Equal trees: " + equalCounter);
		System.out.println("Petrov wins: " + petrovBitparEqual[0]);
		System.out.println("Bitpar wins: " + petrovBitparEqual[1]);
		System.out.println("Equal wins: " + petrovBitparEqual[2]);
	}


	private int compareProbTrees(TSNodeLabel petrovTree,
			TSNodeLabel bitparTree, PrintWriter pw, int index) {
		pw.println("Index: " + index);
		int length = petrovTree.countLexicalNodes();
		pw.println("Length: " + length);
		pw.println(petrovTree.toFlatSentence());		
		pw.println("Petrov: ");
		double petrovLogProb = getLogProb(petrovTree, pw, true);
		pw.println("BitPar: ");
		double bitparLogProb = getLogProb(bitparTree, pw, false);
		int result = petrovLogProb>bitparLogProb ? 0 :
			(petrovLogProb<bitparLogProb ? 1 : 2);		
		double diff = petrovLogProb - bitparLogProb;
		String lineReport = index + " " + length + " " + "Petrov log prob: " + petrovLogProb + 
			" BitPar log prob: " + bitparLogProb + " diff: " + diff;
		System.out.println(lineReport);
		pw.println(lineReport);
		
		pw.println();
		return result;
	}

	private double getLogProb(TSNodeLabel tree, PrintWriter pw, boolean petrov) {
		double result = 0;
		ArrayList<TSNodeLabel> nodes = tree.collectAllNodes();		
		for(TSNodeLabel n : nodes) {
			if (n.isLexical) continue;
			String rule = n.cfgRuleNoQuotes();
			Double prob = n.isPreLexical() ? lexRulesLogProb.get(rule) : grammarRulesLogProb.get(rule);
			if (prob==null) {
				System.out.println("\tNot found rule: " + "(" + (petrov? "petrov" : "bitpar") + ") " + rule);
				return 0;
			}
			result += prob;
			pw.println("\t" + rule + " " + prob);
		}
		pw.println("\tTotalProb: " + result);		
		return result;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
						
		minProbRule = 0;	
		outputPath = "tmp/compare/";		
		petrovGrammarFile = new File("tmp/compare/eng_sm6_readable.gr.grammar");
		petrovLexiconFile = new File("tmp/compare/eng_sm6_readable.gr.lexicon");
		petrovParsedFile = new File("tmp/compare/wsj-24_eng_sm6_viterbi_sub_40.mrg");
		bitparParsedFile = new File("tmp/compare/BITPAR_MPD_RAW.mrg");
		new ComparePetroBitPar().run();
	}

}
