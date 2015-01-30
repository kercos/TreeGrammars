package tsg.parser;

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

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelFreqInt;
import tsg.corpora.Wsj;
import tsg.parsingExp.ConvertFragmentsToCFGRulesAmbiguous;
import tsg.parsingExp.ConvertGrammarInBitParFormat;
import util.Utility;
import util.file.FileUtil;

public class ParseWSJprob {
	
	public static String topSymbol = "TOP";
	public static String posTerminalSuffix = "_#TERM#";
	public static String artificialNodePrefix = ConvertGrammarInBitParFormat.artificialNodePrefix;
	public static int nBest = 1000;
	
	File trainingFile, testFile, fragmentFile, probGrammarFile, 
		ambiguousRulesFile, logFile, testSentencesPreprocessed, ruleBestFragmentMappingFile,
		ruleFragmentsAmbiguousFile, grammarFile, lexiconFile, 
		outputBitParSentenceFile, newIntroduceCFGRulesFile;
	PrintWriter logPw;
	String workingDir, outputBitParDir;
	HashSet<String> posLex;
	ConvertFragmentsToCFGRulesAmbiguous converter;
	
		
	public ParseWSJprob(File trainingFile, File testFile, File fragmentFile) throws Exception {
		this.trainingFile = trainingFile;
		this.testFile = testFile;
		this.fragmentFile = fragmentFile;
		posLex = new HashSet<String>();
		prepareWorkingDir();	
		converter = new ConvertFragmentsToCFGRulesAmbiguous(fragmentFile, ruleFragmentsAmbiguousFile);				
		System.out.println("Converted fragments to CFG rules. Total Rules: " + converter.ruleSize());
		addCFGAndInitialAndTerminalPosRules();		
		System.out.println("Added initial and terminal and CFG rules. Total Rules: " + converter.ruleSize());
		//addInitialAndTerminalPosRules();
		//System.out.println("Added initial and terminal rules. Total Rules: " + converter.ruleSize());
		converter.printGrammarFile(probGrammarFile);
		converter.printRuleBestFragmentMappingFile(ruleBestFragmentMappingFile);
		//parseTestFile();
		logPw.close();
	}
	
	private void prepareWorkingDir() {
		workingDir = Parameters.resultsPath + 
			"TSG/TSGkernels/parsing/" + FileUtil.dateTimeString() + "/";
		outputBitParDir = workingDir + "bitparOutput/";
		new File(workingDir).mkdirs();
		new File(outputBitParDir).mkdirs();
		probGrammarFile = new File(workingDir + "probGrammar.txt");
		grammarFile = new File(workingDir + "grammar.txt");
		lexiconFile = new File(workingDir + "lexicon.txt");
		ruleBestFragmentMappingFile = new File(workingDir + "ruleBestFragmentMapping.txt");
		ambiguousRulesFile = new File(workingDir + "ambiguous_rules.txt");
		newIntroduceCFGRulesFile = new File(workingDir + "newIntroduceCFGRules.txt");
		testSentencesPreprocessed = new File(workingDir + "testSentencesPreprocessed.txt");
		ruleFragmentsAmbiguousFile = new File(workingDir + "ruleFragmentsAmbiguous.txt");
		logFile = new File(workingDir + "log.txt");
		logPw = FileUtil.getPrintWriter(logFile);
	}
	
	@SuppressWarnings("unused")
	private void addInitialAndTerminalPosRules() throws Exception {
		Hashtable<String, int[]> initialRules = new Hashtable<String, int[]>();		
		HashSet<String> posTags = new HashSet<String>();
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingFile);
		for(TSNodeLabel t : treebank) {
			Utility.increaseStringIntArray(initialRules, t.label());
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			for(TSNodeLabel l : lexItems) {
				String parentLabel = l.parent.label();
				String pL = parentLabel + " " + l.label(false, true);
				posLex.add(pL);
				posTags.add(parentLabel);				
			}
		}		
		PrintWriter newRulesPW = FileUtil.getPrintWriter(newIntroduceCFGRulesFile);
		for(Entry<String, int[]> e : initialRules.entrySet()) {
			String initialRule = topSymbol + " " + e.getKey();
			TSNodeLabel fragment = new TSNodeLabel("(" + initialRule + ")", false);
			TSNodeLabelFreqInt fragmentFreq = new TSNodeLabelFreqInt(fragment, e.getValue()[0]);
			newRulesPW.println(fragmentFreq.toString(false, true));
			converter.logSum(initialRule, fragmentFreq);
		}
		for(String p : posTags) {
			String pTerminal = "\"" + p + posTerminalSuffix + "\"";
			String rule = p + " " + pTerminal;
			TSNodeLabel fragment = new TSNodeLabel("(" + rule + ")", false);
			TSNodeLabelFreqInt fragmentFreq = new TSNodeLabelFreqInt(fragment, 1);
			newRulesPW.println(fragmentFreq.toString(false, true));
			converter.logSum(rule, fragmentFreq);
		}
		newRulesPW.close();
	}
	
	private void addCFGAndInitialAndTerminalPosRules() throws Exception {
		Hashtable<String, int[]> cfgRules = new Hashtable<String, int[]>();
		HashSet<String> posTags = new HashSet<String>();
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingFile);
		
		for(TSNodeLabel t : treebank) {
			String initalRule = topSymbol + " " + t.label();
			Utility.increaseStringIntArray(cfgRules, initalRule);
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();				
				if (n.isPreLexical()) {	
					posTags.add(n.label());
					posLex.add(rule);					
				}					
				Utility.increaseStringIntArray(cfgRules, rule);
			}
		}		
		PrintWriter newRulesPW = FileUtil.getPrintWriter(newIntroduceCFGRulesFile);
		for(Entry<String, int[]> e : cfgRules.entrySet()) {
			String rule = e.getKey();
			if (converter.containsRule(rule)) continue;
			TSNodeLabel fragment = new TSNodeLabel("(" + rule + ")", false);
			TSNodeLabelFreqInt fragmentFreq = new TSNodeLabelFreqInt(fragment, e.getValue()[0]);
			newRulesPW.println(fragmentFreq.toString(false, true));
			converter.logSum(rule, fragmentFreq);			
		}
		for(String p : posTags) {
			String pTerminal = "\"" + p + posTerminalSuffix + "\"";
			String rule = p + " " + pTerminal;
			TSNodeLabel fragment = new TSNodeLabel("(" + rule + ")", false);
			TSNodeLabelFreqInt fragmentFreq = new TSNodeLabelFreqInt(fragment, 1);
			newRulesPW.println(fragmentFreq.toString(false, true));
			converter.logSum(rule, fragmentFreq);			
		}
		newRulesPW.close();
	}
		
	@SuppressWarnings("unused")
	private void parseTestFile() throws Exception {
		ArrayList<TSNodeLabel> testTreebank = TSNodeLabel.getTreebank(testFile);		
		int sentenceNumber = 1;		
		for(TSNodeLabel t : testTreebank) {						
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
			reportLine("Preparing grammar for sentence: " + sentenceNumber);
			reportLine("Original  sentece: " + 
					Utility.joinStringArrayToString(flatWordArray, " "));
			reportLine("Preprocessed  sentece: " + 
					Utility.joinStringArrayToString(flatWordArrayPosTerminal, " "));
			long startTime = System.currentTimeMillis();
			ConvertGrammarInBitParFormat.probGrammarForOneSentence(probGrammarFile, flatWordArrayPosTerminal,
					grammarFile, lexiconFile);
			int tookSec = (int) ((System.currentTimeMillis()-startTime) / 1000);
			reportLine("Took " + tookSec);
			createTestSentenceFile(flatWordArrayPosTerminal);
			outputBitParSentenceFile = new File(
					outputBitParDir + "bitParOut_" + Utility.padZero(4, sentenceNumber) + ".txt");												
			startTime = System.currentTimeMillis();
			reportLine("Parsing sentence...");
			runBitPar();					
			tookSec = (int) ((System.currentTimeMillis()-startTime) / 1000);
			reportLine("Finished parsing. Took " + tookSec + " sec.");
			reportLine("");
			sentenceNumber++;
		}		
	}
	
	private void createTestSentenceFile(String[] flatWordArrayPosTerminal) {
		PrintWriter flatPW = FileUtil.getPrintWriter(testSentencesPreprocessed);
		flatPW.println(Utility.joinStringArrayToString(flatWordArrayPosTerminal, "\n")+"\n");
		flatPW.close();
	}
	
	static String bitparApp = "/home/fsangati/SOFTWARE/BitPar_Web/bitpar";			
	static String bitparArgs = "-p -v -b " + nBest + " -o -s TOP ";	
	
	private void runBitPar() {
		try {					
			Process p = Runtime.getRuntime().exec(
				bitparApp + " " + bitparArgs + " " + grammarFile + " " + 
				lexiconFile + " " +  testSentencesPreprocessed + " " + outputBitParSentenceFile,
				null, new File(workingDir));
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    while ((line = input.readLine()) != null) reportLine(line);
		    input.close();
		}
		catch (Exception err) {
		   err.printStackTrace();
		}
	}
	
	private void reportLine(String line) {
		System.out.println(line);
		logPw.println(line);
	}
	
	public static void getFirstParsesAndProcess(String dir, File outputFile,
			Hashtable<String, TSNodeLabel> ruleFragmentTable,
			ArrayList<String> flatSentences) throws Exception {
		File dirFile = new File(dir);
		File[] outputFiles = dirFile.listFiles();
		Arrays.sort(outputFiles);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Iterator<String> sentenceIterator = flatSentences.iterator();
		for(File f : outputFiles) {
			Scanner scan = FileUtil.getScanner(f);
			String line = scan.nextLine();
			scan.close();
			line = line.replaceAll("\\\\", "");
			String flatSentence = sentenceIterator.next();
			String[] sentenceWords = flatSentence.split("\\s+");
			TSNodeLabel fragment = new TSNodeLabel(line);			
			fragment.removePreterminalWithPrefix(artificialNodePrefix,-1);			
			TSNodeLabel fragmentConverted = fragment.replaceRulesWithFragments(ruleFragmentTable);			
			fragmentConverted = fragmentConverted.daughters[0]; //remove TOP
			fragmentConverted.adjustLexicalItems(sentenceWords);
			pw.println(fragmentConverted.toString());
		}
		pw.close();
	}

	
	public static ArrayList<String> readTestFlat(File inputFile) {
		ArrayList<String> result = new ArrayList<String>();
		Scanner scan = FileUtil.getScanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			line = line.replaceAll("\\\\", "");
			result.add(line);
		}
		scan.close();
		return result;
	}
	
	public static void writeGoldFile(File inputFile, File outputFile) {
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			line = line.replaceAll("\\\\", "");
			pw.println(line);
		}
		scan.close();
		pw.close();
	}
	
	/*public static void main(String[] args) throws Exception {
		File trainingFile = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		File testFile = new File(Wsj.WsjOriginalCleaned + "wsj-22.mrg");
		//File fragmentFile = new File(args[0]);
		File fragmentFile = new File(Parameters.resultsPath + 
				"TSG/TSGkernels/subTree/all/correct/fragments_MUB_freq_all.txt");
		new ParseWSJprob(trainingFile, testFile, fragmentFile);
	}*/
	
	public static void main(String[] args) throws Exception {
		String workingDir = Parameters.resultsPath + "TSG/TSGkernels/parsing/kernelDOP1s_prob/";
		String bitparOutputDir = workingDir + "bitparOutput/";
		File flatSentences = new File(Wsj.WsjFlatNoTraces + "wsj-22.mrg");
		ArrayList<String> flatSentencesArray = readTestFlat(flatSentences);
		File ruleFragmentTableFile = new File(workingDir + "ruleBestFragmentMapping.txt");
		File outputFile = new File(workingDir + "bitParBest.txt");
		File goldFile = new File(workingDir + "gold.txt");
		writeGoldFile(new File(Wsj.WsjOriginalCleaned + "wsj-22.mrg"), goldFile);
		Hashtable<String, TSNodeLabel> ruleFragmentTable =
			ConvertFragmentsToCFGRulesAmbiguous.readRuleBestFragmentMappingFile(ruleFragmentTableFile);
		getFirstParsesAndProcess(bitparOutputDir, outputFile,ruleFragmentTable,
				flatSentencesArray);
	}
	
}
