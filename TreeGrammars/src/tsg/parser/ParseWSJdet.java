package tsg.parser;

import grammars.SymbolicGrammar;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import nodes.TerminalNode;
import nodes.Node.NoMoreNodeAllowedExeption;
import settings.Parameters;
import stateManagers.SymbolicStateManagerArray;
import stateManagers.SymbolicStateManagerArrayTC;
import tsg.*;
import tsg.corpora.Wsj;
import tsg.parsingExp.ConvertFragmentsToCFGRulesAmbiguous;
import tsg.parsingExp.ConvertGrammarInBitParFormat;
import util.Utility;
import util.file.FileUtil;

public class ParseWSJdet {
	
	public static String topSymbol = "TOP";
	public static String posTerminalSuffix = "_#TERM#";
	
	File trainingFile, testFile, fragmentFile, symbolicGrammarFile, 
		ambiguousRulesFile, logFile, testSentencesPreprocessed;
	PrintWriter logPw;
	String workingDir;
	HashSet<String> posLex;
	SymbolicGrammar G;
	SymbolicStateManagerArrayTC SM;
	
	public ParseWSJdet(File trainingFile, File testFile, File fragmentFile) throws Exception {
		this.trainingFile = trainingFile;
		this.testFile = testFile;
		this.fragmentFile = fragmentFile;
		posLex = new HashSet<String>();
		prepareWorkingDir();						
		new ConvertFragmentsToCFGRulesAmbiguous(fragmentFile, symbolicGrammarFile, logFile);
		//addInitialAndTerminalPosRules();		
		addCFGAndInitialAndTerminalPosRules();		
		parseTestFile();
		logPw.close();
	}
	
	private void prepareWorkingDir() {
		workingDir = Parameters.resultsPath + 
			"TSG/TSGkernels/parsing/" + FileUtil.dateTimeString() + "/";
		new File(workingDir).mkdirs();
		symbolicGrammarFile = new File(workingDir + "symbolicGrammar.txt");
		ambiguousRulesFile = new File(workingDir + "ambiguous_rules.txt");
		testSentencesPreprocessed = new File(workingDir + "testSentencesPreprocessed.txt");
		logFile = new File(workingDir + "log.txt");
		logPw = FileUtil.getPrintWriter(logFile);
	}
	
	/*private void addInitialAndTerminalPosRules() throws Exception {
		HashSet<String> initialSymbols = new HashSet<String>();		
		HashSet<String> posTags = new HashSet<String>();
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingFile);
		for(TSNodeLabel t : treebank) {
			initialSymbols.add(t.label());
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			for(TSNodeLabel l : lexItems) {
				String parentLabel = l.parent.label();
				String pL = parentLabel + " " + l.label(false, true);
				posLex.add(pL);
				posTags.add(parentLabel);				
			}
		}
		FileWriter grammarWriter = new FileWriter(symbolicGrammarFile, true);
		for(String s : initialSymbols) grammarWriter.write(topSymbol + " " + s + "\n");
		for(String p : posTags) {
			String pTerminal = "\"" + p + posTerminalSuffix + "\"";
			grammarWriter.write(p + " " + pTerminal + "\n");
		}
		grammarWriter.close();
	}*/
	
	private void addCFGAndInitialAndTerminalPosRules() throws Exception {
		HashSet<String> initialSymbols = new HashSet<String>();
		HashSet<String> posTags = new HashSet<String>();
		HashSet<String> cfgRules = new HashSet<String>();
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingFile);
		
		for(TSNodeLabel t : treebank) {
			initialSymbols.add(t.label());
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();
				if (n.isPreLexical()) {	
					posTags.add(n.label());
					posLex.add(rule);					
				}	
				cfgRules.add(rule);
			}
		}
		FileWriter grammarWriter = new FileWriter(symbolicGrammarFile, true);
		for(String r : cfgRules) grammarWriter.write(r + "\n");
		for(String s : initialSymbols) {
			String rule = topSymbol + " " + s;
			grammarWriter.write(rule + "\n");
		}
		for(String p : posTags) {
			String pTerminal = "\"" + p + posTerminalSuffix + "\"";
			String rule = p + " " + pTerminal;
			grammarWriter.write(rule + "\n");
		}
		grammarWriter.close();
	}
	
	private void prepareParser() throws Exception {
		System.out.println("Preparing parser.");
		G = SymbolicGrammar.fromUniqueFile(symbolicGrammarFile, 
				FileUtil.defaultEncoding, topSymbol);
		long startTime = System.currentTimeMillis();
		SM = new SymbolicStateManagerArrayTC(G);
		int tookSec = (int) ((System.currentTimeMillis()-startTime) / 1000);
		System.out.println("Finish. Took " + tookSec + " sec.");
	}
		
	private void parseTestFile() throws Exception {
		//prepareParser();
		ArrayList<TSNodeLabel> testTreebank = TSNodeLabel.getTreebank(testFile);		
		int sentenceNumber = 1;
		PrintWriter flatPW = FileUtil.getPrintWriter(testSentencesPreprocessed);
		for(TSNodeLabel t : testTreebank) {
			/*if (sentenceNumber!=14) {
				sentenceNumber++;
				continue;
			}*/
			long startTime = System.currentTimeMillis();
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			int length = lexItems.size();
			String[] flatWordArray = new String[length];;			
			String[] flatWordArrayPosTerminal = new String[length];
			int i=0;
			
			for(TSNodeLabel l : lexItems) {
				String iWord = l.label();
				flatWordArray[i] = iWord;
				TSNodeLabel lParent = l.parent;
				String p = lParent.label(); 
				String pL = p + " " + l.label(false, true);
				flatWordArrayPosTerminal[i] =
					posLex.contains(pL) ?
							iWord : 
							p + posTerminalSuffix;							
				i++;				
			}
			String sentenceFlat = Utility.joinStringArrayToString(flatWordArrayPosTerminal, " ");
			ConvertGrammarInBitParFormat.deterministicGrammarForOneSentence(
					symbolicGrammarFile, sentenceFlat);
			flatPW.println(Utility.joinStringArrayToString(flatWordArrayPosTerminal, "\n")+"\n");
			/*reportLine("Parsing  sentece " + sentenceNumber);
			reportLine("Original  sentece: " + 
					Utility.joinStringArrayToString(flatWordArray, " "));
			reportLine("Preprocessed  sentece: " + 
					Utility.joinStringArrayToString(flatWordArrayPosTerminal, " "));			
			TerminalNode[] sentenceNodes = G.convertSentenceWords(flatWordArrayPosTerminal);
			SM.parseForest(sentenceNodes);
			reportLine("Grammatical: " + SM.isGrammatical());
			int tookSec = (int) ((System.currentTimeMillis()-startTime) / 1000);
			reportLine("Finish. Took " + tookSec + " sec.");
			reportLine("");*/
			sentenceNumber++;
			break;
		}		
		flatPW.close();
	}
	
	private void reportLine(String line) {
		System.out.println(line);
		logPw.println(line);
	}
	
	public static void main(String[] args) throws Exception {
		File trainingFile = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		File testFile = new File(Wsj.WsjOriginalCleaned + "wsj-22.mrg");
		//File fragmentFile = new File(args[0]);
		File fragmentFile = new File(Parameters.resultsPath + 
				"TSG/TSGkernels/subTree/all/correct/fragments_MUB_freq_all.txt");
		new ParseWSJdet(trainingFile, testFile, fragmentFile);
	}
}
