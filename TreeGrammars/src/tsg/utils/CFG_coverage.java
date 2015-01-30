package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class CFG_coverage {
	
	HashSet<TSNodeLabel> cfgInternalRulesSet = new HashSet<TSNodeLabel>();
	HashSet<TSNodeLabel> cfgLexRulesSet = new HashSet<TSNodeLabel>();
	String outputPath;
	int testSentences;
	int totalSentencesCoveredAll, totalSentencesCoveredInternal;
	int totalInternalNodes, coveredInternalNodes;	
	HashSet<TSNodeLabel> intMissingRules = new HashSet<TSNodeLabel>();
	HashSet<TSNodeLabel> lexMissingRules = new HashSet<TSNodeLabel>();

	public CFG_coverage(File trainTB, File testTB, String outputPath) throws Exception {
		if (outputPath.endsWith("/")) outputPath += "/";
		this.outputPath = outputPath;
		//printGrammars();
		readCfgFromTrainTB(trainTB);		
		checkCfgCoverage(testTB);
		printResults();		
		printMissedRules();
	}

	private void printGrammars() {
		PrintWriter pw = FileUtil.getPrintWriter(new File(outputPath + "IntRules.txt"));
		for(TSNodeLabel r : cfgInternalRulesSet) {
			pw.println(r.toString(false, true));
		}
		pw.close();
		pw = FileUtil.getPrintWriter(new File(outputPath + "LexRules.txt"));
		for(TSNodeLabel r : cfgLexRulesSet) {
			pw.println(r.toString(false, true));
		}
		pw.close();
	}
	
	private void printMissedRules() {
		PrintWriter pw = FileUtil.getPrintWriter(new File(outputPath + "IntMissingRules.txt"));
		for(TSNodeLabel r : intMissingRules) {
			pw.println(r.toString(false, true));
		}
		pw.close();
		pw = FileUtil.getPrintWriter(new File(outputPath + "LexMissingRules.txt"));
		for(TSNodeLabel r : lexMissingRules) {
			pw.println(r.toString(false, true));
		}
		pw.close();
	}

	private void readCfgFromTrainTB(File trainTB) throws Exception {
		System.out.println("Reading training File...");
		Scanner scan = FileUtil.getScanner(trainTB);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			ArrayList<TSNodeLabel> nonLexNode = tree.collectNonLexicalNodes();
			for(TSNodeLabel n : nonLexNode) {
				TSNodeLabel cfgRule = n.cloneOneLevel();
				if (n.isPreLexical())
					cfgLexRulesSet.add(cfgRule);
				else
					cfgInternalRulesSet.add(cfgRule);
			}
		}	
		System.out.println("Total internal rules in grammar: " + cfgInternalRulesSet.size());
		System.out.println("Total lex rules in grammar: " + cfgLexRulesSet.size());
	}
	
	private void checkCfgCoverage(File testTB) throws Exception {
		Scanner scan = FileUtil.getScanner(testTB);
		while(scan.hasNextLine()) {
			testSentences++;
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			ArrayList<TSNodeLabel> nonLexNode = tree.collectNonLexicalNodes();
			boolean coveredAll = true;
			boolean coveredInternal = true;
			for(TSNodeLabel n : nonLexNode) {
				TSNodeLabel cfgRule = n.cloneOneLevel();
				boolean prelex = n.isPreLexical(); 
				if (prelex) {
					if (!cfgLexRulesSet.contains(cfgRule)) {
						coveredAll = false;
						lexMissingRules.add(cfgRule);
					}
				}
				else {
					totalInternalNodes++;
					if (cfgInternalRulesSet.contains(cfgRule))
						coveredInternalNodes++;
					else {
						coveredInternal = false;
						intMissingRules.add(cfgRule);
					}
				}
			}
			if (coveredAll)
				totalSentencesCoveredAll++;
			if (coveredInternal)
				totalSentencesCoveredInternal++;
		}
		
	}



	private void printResults() {
		System.out.println("Test sentences: " + testSentences);
		System.out.println("Total Sentences Covered All: " + totalSentencesCoveredAll);
		System.out.println("Total Sentences Covered Internal: " + totalSentencesCoveredInternal);
		System.out.println("Total Internal Nodes: " + totalInternalNodes);
		System.out.println("Covered Internal Nodes: " + coveredInternalNodes);		
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//File trainTB = new File(args[0]);
		//File testTB = new File(args[1]);
		String outputPath = "/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/PetrovBinH1V2Uk4/";
		File trainTB = new File(outputPath + "wsj-02-21.mrg");
		File testTB = new File(outputPath + "wsj-22.mrg");
		new CFG_coverage(trainTB, testTB, outputPath);
	}

}
