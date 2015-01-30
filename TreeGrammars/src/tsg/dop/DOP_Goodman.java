package tsg.dop;

import settings.Parameters;
import tsg.CFSG;
import tsg.TSNode;
import tsg.corpora.*;
import tsg.parser.Parser;
import tsg.parser.PostProcess;
import util.*;
import util.file.FileUtil;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;



public class DOP_Goodman extends CFSG<Long>{
	
	public DOP_Goodman() {
		super();	
	}
	
	public void readCFGFromCorpus() {		
		int uniqueLableIndex = 1;
		for(TSNode TreeLine : Parameters.trainingCorpus.treeBank) {
			TreeLine.toNormalForm();
			uniqueLableIndex = TreeLine.toUniqueInternalLabels(false, uniqueLableIndex, false); 
			LinkedList<String> goodmanPCFG = TreeLine.goodman(true);
			for(String rule : goodmanPCFG) {
				int lastSpace = rule.lastIndexOf(" ");				
				long count = Long.parseLong(rule.substring(lastSpace+1));
				rule = rule.substring(0,lastSpace);
				if (rule.indexOf('"')==-1) Utility.increaseStringLong(internalRules, rule, count);
				else {
					rule = Utility.removeDoubleQuotes(rule);					
					Utility.increaseStringLong(lexRules, rule, count);
				}
			}
		}
		String log = "Read rules from corups. \n # Internal Rules: " + internalRules.size() 
		+ "\n # Lex Rules: " + lexRules.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	public static void printGoodmanCFG(File corpusFile, File grammarFile, File lexiconFile) {		
		int uniqueLableIndex = 1;
		ConstCorpus corpus = new ConstCorpus(corpusFile);
		Hashtable<String, Long> internalRules = new Hashtable<String, Long>();
		Hashtable<String, Long> lexRules = new Hashtable<String, Long>();
		PrintProgressStatic.start("Reading sentence:");
		for(TSNode TreeLine : corpus.treeBank) {
			PrintProgressStatic.next();
			TreeLine.toNormalForm();
			uniqueLableIndex = TreeLine.toUniqueInternalLabels(false, uniqueLableIndex, false); 
			LinkedList<String> goodmanPCFG = TreeLine.goodman(true);
			for(String rule : goodmanPCFG) {
				int lastSpace = rule.lastIndexOf(" ");				
				long count = Long.parseLong(rule.substring(lastSpace+1));
				rule = rule.substring(0,lastSpace);
				if (rule.indexOf('"')==-1) Utility.increaseStringLong(internalRules, rule, count);
				else {
					rule = Utility.removeDoubleQuotes(rule);					
					Utility.increaseStringLong(lexRules, rule, count);
				}
			}
		}
		PrintProgressStatic.end();
		String log = "Read rules from corups. \n # Internal Rules: " + internalRules.size() 
		+ "\n # Lex Rules: " + lexRules.size();
		System.out.println(log);
		
		PrintWriter grammar = FileUtil.getPrintWriter(grammarFile);
		TreeSet<String> orderedInternal = new TreeSet<String>(internalRules.keySet()); 
		for (String rule : orderedInternal) {			
			Object count = internalRules.get(rule);										
			String line = count.toString() + "\t" + rule;
			grammar.write(line + "\n");
		}	
		grammar.close();
		
		PrintWriter lexicon = FileUtil.getPrintWriter(lexiconFile);
		TreeSet<String> orderedLexical = new TreeSet<String>(lexRules.keySet());		
		for (String rule : orderedLexical) {
			Object count = lexRules.get(rule);										
			String line = count.toString() + "\t" + rule;
			lexicon.write(line + "\n");
		}	
		lexicon.close();
		
		System.out.println("Printed `lexicon` and `grammar` files");
		
	}
	
	public static void convertLexiconInBitParFormat(File inputFile, File outputFile) {
		Scanner scan = FileUtil.getScanner(inputFile);
		Hashtable<String, String[]> lexPosCountsTable = new Hashtable<String, String[]>(); 
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length()==0) continue;
			String[] lineSplit = line.split("\t");
			int count = Integer.parseInt(lineSplit[0]);
			String[] posLex = lineSplit[1].split("\\s+");
			String pos = posLex[0];
			String lex = posLex[1];
			String[] posCount = lexPosCountsTable.get(lex);
			String toAddInPosCount = pos + " " + count;
			if (posCount==null) {
				posCount = new String[]{toAddInPosCount};
				lexPosCountsTable.put(lex, posCount);
			}
			else posCount[0] += "\t" + toAddInPosCount;			
		}
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<String, String[]> e : lexPosCountsTable.entrySet()) {
			pw.println(e.getKey() + "\t" + e.getValue()[0]);
		}
		pw.close();
	}
	
	public static void mainOld(String args[]) {		
		Parameters.corpusName = "Wsj"; //Wsj, Negra, Parc, Tiger, Wsj 
		Parameters.lengthLimitTraining = 20;
		Parameters.lengthLimitTest = 20;
		Wsj.testSet = "22"; //00 01 23 24
		Wsj.skip120TrainingSentences = false;
		Wsj.transformNPbasal = true;
		Wsj.transformSG = false;		
		Parameters.semanticTags = true;
		Parameters.replaceNumbers = true;
		Parameters.ukLimit = 1;		
		Parameters.outputPath = "/home/fsangati/PROJECTS/TSG/RESULTS/DOP_Goodman/";
		
		Parameters.parserName = Parser.fedePar;
		Parameters.nBest = 1;
		Parameters.cachingActive = false;

		DOP_Goodman Grammar = new DOP_Goodman();
		Parameters.trainingCorpus.checkRedundentRules();
		Grammar.readCFGFromCorpus();					
		//Grammar.printTrainingCorpusToFile();
		Grammar.printLexiconAndGrammarFiles();
				
		//new Parser(Grammar);
	}
	
	public static void postProcessFile(File inputFile, File outputFile) {
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			if (line.startsWith("vitprob")) {
				pw.println(line);
				continue;
			}
			TSNode TN = new TSNode(line);
			TN.removeUniqueInternalLabels(false);
			TN.fromNormalForm();				
			pw.println(TN.toString());	
		}
		pw.close();
	}
	
	public static void main(String[] args) {
		//String dir = "/Users/fsangati/Desktop/goodman/";
		//args = new String[]{dir+"ex1.mrg", dir+"grammar.txt", dir+"lexicon.txt"};
		File corpusFile = new File(args[0]); 
		File grammarFile = new File(args[1]);
		File lexiconFile = new File(args[2]);
		printGoodmanCFG(corpusFile, grammarFile, lexiconFile);
		
		/*
		File corpusFile = new File("Goodman/ex/treebankExample.mrg"); 
		File grammarFile = new File("Goodman/ex/grammar.txt");
		File lexiconFile = new File("Goodman/ex/lexicon.txt");
		File lexiconFileBP = new File("Goodman/ex/lexicon_BP.txt");
		File bitParOut = new File("Goodman/ex/bitpar.out");
		File bitParOutPos = new File("Goodman/ex/bitpar_postprocessed.out");
		*/
		
		//printGoodmanCFG(corpusFile, grammarFile, lexiconFile);
		//convertLexiconInBitParFormat(lexiconFile, lexiconFileBP);
		//postProcessFile(bitParOut, bitParOutPos);
	}
	
	
}
