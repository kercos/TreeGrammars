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

public class CompareMJCykBitPar {

	static double minProbRule;
	static boolean normalize;
	
	static String outputPath;
	static File petrovGrammarFile, petrovLexiconFile;
	static File petrovParsedFile, bitparParsedFile;	
	
	Hashtable<String,Double> grammarRulesLogProb;
	Hashtable<String,Double> lexRulesLogProb;
	
	
	public CompareMJCykBitPar() {
		convertGrammar();
		convertLexicon();
		if (normalize)			
			normalizeAndMakeLog();
		else
			makeLog();
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
				//String lhs = rule.split("\\s")[0];
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
			double prob = Double.parseDouble(lineSplit[0]);		
			if (prob<minProbRule) {
				System.out.println("Skipped rule: " + line);
				smallProbRuleSkipped++;
				continue;			
			}
			totalAcceptedRules++;
			String lhs = lineSplit[1];
			boolean unaryRule = length == 3;
			if (unaryRule) {
				String rhsChild = lineSplit[2];
				String rule = lhs + " " + rhsChild;
				grammarRulesLogProb.put(rule, prob);
			}
			else {
				String rule = lhs;
				for(int i=2; i<length; i++) {
					String rhsChild = lineSplit[i];
					rule += " " + rhsChild;
				}				
				grammarRulesLogProb.put(rule, prob);
			}			
		}
		System.out.println("Skipped small prob internal rules: " + smallProbRuleSkipped);		
		System.out.println("Total accepted internal rules: " + totalAcceptedRules);
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
			//line = line.replaceAll("\\\\", "");			
			String[] lineSplit = line.split("\t");
			int length = lineSplit.length;
			String lex = lineSplit[0];
			
			for(int i=1; i<length; i++) {
				String posProb = lineSplit[i];
				String[] posProbSplit = posProb.split("\\s");
				String pos = posProbSplit[0];
				double prob = Double.parseDouble(posProbSplit[1]);
				if (prob>minProbRule) {
					totalAcceptedRules++;
					String rule = pos + " " + lex;
					lexRulesLogProb.put(rule, prob);
				}
				else 
					smallProbRuleSkipped++;
			}
							
		}
		System.out.println("Skipped small prob lex rules: " + smallProbRuleSkipped);		
		System.out.println("Total accepted lex rules: " + totalAcceptedRules);

	}

	
	private void makeComparison(TSNodeLabel bitparTree, TSNodeLabel cyk) {
		compareProbTrees(cyk, bitparTree, 0);
		
	}
	

	private int compareProbTrees(TSNodeLabel petrovTree,
			TSNodeLabel bitparTree, int index) {
		System.out.println("Index: " + index);
		int length = petrovTree.countLexicalNodes();
		System.out.println("Length: " + length);
		System.out.println(petrovTree.toFlatSentence());		
		System.out.println("Petrov: ");
		double petrovLogProb = getLogProb(petrovTree, true);
		System.out.println("BitPar: ");
		double bitparLogProb = getLogProb(bitparTree, false);
		int result = petrovLogProb>bitparLogProb ? 0 :
			(petrovLogProb<bitparLogProb ? 1 : 2);		
		double diff = petrovLogProb - bitparLogProb;
		String lineReport = index + " " + length + " " + "Petrov log prob: " + petrovLogProb + 
			" BitPar log prob: " + bitparLogProb + " diff: " + diff;
		System.out.println(lineReport);
		
		System.out.println();
		return result;
	}
	

	private double getLogProb(TSNodeLabel tree, boolean petrov) {
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
			System.out.println("\t" + rule + " " + prob);
		}
		System.out.println("\tTotalProb: " + result);		
		return result;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
						
		minProbRule = 0;	
		outputPath = "tmp/compare/newTest/";	
		normalize = true;
		//petrovGrammarFile = new File("tmp/compare/eng_sm6_readable.gr.grammar");
		//petrovLexiconFile = new File("tmp/compare/eng_sm6_readable.gr.lexicon");
		//petrovParsedFile = new File("tmp/compare/wsj-24_eng_sm6_viterbi_sub_40.mrg");
		//bitparParsedFile = new File("tmp/compare/BITPAR_MPD_RAW.mrg");
		petrovGrammarFile = new File("tmp/compare/newTest/bitpar_grammar.txt");
		petrovLexiconFile = new File("tmp/compare/newTest/bitpar_lexicon.txt");
		petrovParsedFile = new File("tmp/compare/newTest/bitpar_parses.mrg");
		bitparParsedFile = new File("tmp/compare/newTest/cyk_parses.mrg");		
		CompareMJCykBitPar C = new CompareMJCykBitPar();
		
		TSNodeLabel bitparTree44 = new TSNodeLabel("(ROOT-0 (FRAG-0 (@FRAG-2 (@FRAG-0 (INTJ-0 (UH-0 Ah)) (,-0 ,)) (PP-25 (IN-22 UNK-LC) (NP-53 (NNP-37 Columbia)))) (.-2 !)))");
		TSNodeLabel cyk44 =        new TSNodeLabel("(ROOT-0 (FRAG-0 (@FRAG-2 (@FRAG-0 (INTJ-0 (UH-0 Ah)) (,-0 ,)) (NP-14 (JJ-10 UNK-LC) (NNP-41 Columbia))) (.-2 !)))");
		C.makeComparison(bitparTree44, cyk44);
	}

}
