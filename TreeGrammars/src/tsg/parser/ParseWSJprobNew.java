package tsg.parser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
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
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelFreqInt;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import tsg.parsingExp.ConvertFragmentsToCFGRulesAmbiguous;
import tsg.parsingExp.ConvertGrammarInBitParFormat;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class ParseWSJprobNew {
	
	public static String lexPosSeparationString = "^";
	public static char lexPosSeparationChar = lexPosSeparationString.charAt(0);
	static PrintWriter pwLog;
	static int nBest = 1000;				
	static String bitparArgs = "-vp -b " + nBest + " -o -s TOP ";	
	
	private static void reportLine(String line) {
		System.out.println(line);
		if (pwLog!=null) pwLog.println(line);
	}
	
	public static void extractCFGrules(File inputFile, File outputFile) throws Exception {
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);		
		Hashtable<String, int[]> rulesTableFreq = new Hashtable<String, int[]>();
		for(TSNodeLabel t : treebank) {
			ArrayList<String> fragments = t.allSubTrees(1, Integer.MAX_VALUE);			
			for(String s : fragments) {								
				Utility.increaseStringIntArray(rulesTableFreq, s);				
			}			
		}
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<String, int[]> e : ((Hashtable<String, int[]>)rulesTableFreq).entrySet()) {
			String fragmentString = e.getKey();
			int freq = e.getValue()[0];
			TSNodeLabel fragmentTree =new TSNodeLabel(fragmentString, false);
			pw.println(fragmentTree.toString(false, true) + "\t" + freq);							
		}
		pw.close();
	}
	
	@SuppressWarnings("unchecked")
	public static void extractFragmentsFileDepths(File inputFile, String outputDir, int maxDepth,
			int maxProle) throws Exception {		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		
		Hashtable[] fragmentsTablesDepths = new Hashtable[maxDepth];
		for(int i=0; i<maxDepth; i++) {
			fragmentsTablesDepths[i] = new Hashtable<String, int[]>();
		}		
		
		PrintProgressStatic.start("Extractrig fragments up to depth " + maxDepth + 
				" max braching " + maxProle + ": ");
		for(TSNodeLabel t : treebank) {
			PrintProgressStatic.next();
			ArrayList<String> fragments = t.allSubTrees(maxDepth, maxProle);
			for(String s : fragments) {				
				TSNodeLabel treeFragm = new TSNodeLabel(s, false);
				int depth = treeFragm.maxDepth();
				Utility.increaseStringIntArray(fragmentsTablesDepths[depth-1], s);				
			}			
		}
		PrintProgressStatic.end();
		
		for(int i=0; i<maxDepth; i++) {
			int depth = i+1;
			File outputFile = new File(outputDir + "fragments_Depth_" + depth + 
					"_maxProle_" + maxProle +"_freq_all.txt");
			PrintWriter pw = FileUtil.getPrintWriter(outputFile);			
			for(Entry<String, int[]> e : ((Hashtable<String, int[]>)fragmentsTablesDepths[i]).entrySet()) {
				String fragmentString = e.getKey();
				int freq = e.getValue()[0];
				TSNodeLabel fragmentTree =new TSNodeLabel(fragmentString, false);
				pw.println(freq + "\t" + fragmentTree.toString(false, true));							
			}
			pw.close();
		}					
	}
	
	public static void prepareKernelFragments() throws Exception {
	
	}
	
	private static void makeFragmentsUniqueFile(File fragmentsFile, File fragmentsUniqueFile) {
		Hashtable<String, int[]> fragFreq = new Hashtable<String, int[]>();
		Scanner scan = FileUtil.getScanner(fragmentsFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			String[] freqTree = line.split("\t");
			int freq = Integer.parseInt(freqTree[0]);
			String tree = freqTree[1];
			int[] storedFreq = fragFreq.get(tree);
			if(storedFreq==null) {
				storedFreq = new int[]{freq};
				fragFreq.put(tree, storedFreq);
				continue;
			}
			if (storedFreq[0]<freq) storedFreq[0]=freq;
		}
		
		PrintWriter pw = FileUtil.getPrintWriter(fragmentsUniqueFile);
		for(Entry<String, int[]> e : fragFreq.entrySet()) {
			pw.println(e.getValue()[0] + "\t" + e.getKey());
		}
		pw.println();
		pw.close();		
	}
	
	//25 . (PUNCT)
	private static ArrayList<String[]> getTestSentencesPosWord(File testFile) throws Exception {
		ArrayList<String[]> result = new ArrayList<String[]>();
		ArrayList<TSNodeLabel> testTreebank = TSNodeLabel.getTreebank(testFile);
		for(TSNodeLabel t : testTreebank) {
			ArrayList<String> sentencePosWords = new ArrayList<String>();
			ArrayList<TSNodeLabel> lexItems = t.collectLexicalItems();
			for(TSNodeLabel l : lexItems) {
				String word = l.label();
				String pos = l.parent.label();
				sentencePosWords.add(pos + lexPosSeparationString + word);
			}
			String[] sentencePosWordArray = sentencePosWords.toArray(new String[sentencePosWords.size()]); 
			result.add(sentencePosWordArray);
		}
		return result;
	}
	
	private static void createTestSentenceFile(String[] flatWordArrayPosTerminal, File testSentencesPreprocessed) {
		PrintWriter flatPW = FileUtil.getPrintWriter(testSentencesPreprocessed);
		flatPW.println(Utility.joinStringArrayToString(flatWordArrayPosTerminal, "\n")+"\n");
		flatPW.close();
	}
		
	public static File buildAndParse(String workingDir, File testFile, File fragmentsFile) throws Exception {
		//FILES DEFINITION		
		File fragmentsUniqueFile = new File(fragmentsFile + "_UNIQUE");
		File probGrammarFile = new File(workingDir + "CFG_FREQ_GRAMMAR.txt");
		File cfgRuleFragmentMappingFile = new File(workingDir + "CFG_RULE_FRAGMENT_MAPPING.txt");		
		File ouputBitParPosProcessedBestFile = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED_BEST.txt");
		File ouputBitParPosProcessedFile = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED.txt");				
		File parseLog = new File(workingDir + "parse.log");
		pwLog = FileUtil.getPrintWriter(parseLog);
		
		//MAKE CFG GRAMMAR AND MAPPING CFG-FRAGMETNS
		makeFragmentsUniqueFile(fragmentsFile, fragmentsUniqueFile);
		File ambiguousFragmentsLogFile = new File(workingDir + "log_ambiguousFragmetnsCFG.txt");
		ConvertFragmentsToCFGRulesAmbiguous converter = new ConvertFragmentsToCFGRulesAmbiguous(
				fragmentsUniqueFile, ambiguousFragmentsLogFile);
		converter.printGrammarFile(probGrammarFile);
		converter.printRuleBestFragmentMappingFile(cfgRuleFragmentMappingFile);
		Hashtable<String, TSNodeLabel> ruleFragmentTable = converter.getRuleBestFragmentMappingTable();
		//Hashtable<String, TSNodeLabel> ruleFragmentTable = ConvertFragmentsToCFGRules.readRuleBestFragmentMappingFile(cfgRuleFragmentMappingFile);			
		
		//PARSE TEST SENTENCES
		ArrayList<String[]> testSentencesPosWord = getTestSentencesPosWord(testFile);
		String bitParOuputPath = workingDir + "bitparOutput/";
		new File(bitParOuputPath).mkdir();		
		int sentenceIndex = 0;
		int maxCounterLength = Integer.toString(testSentencesPosWord.size()).length();
		PrintWriter pwBest = FileUtil.getPrintWriter(ouputBitParPosProcessedBestFile);
		PrintWriter pwComplete = FileUtil.getPrintWriter(ouputBitParPosProcessedFile);
		for(String[] sentencePosWord : testSentencesPosWord) {
			sentenceIndex++;
			if (sentenceIndex<1011) continue;
			reportLine("Preparing grammar for sentence: " + sentenceIndex);
			long startTime = System.currentTimeMillis();
				String sentenceIndexPad = Utility.padZero(maxCounterLength, sentenceIndex);
				String sentenceFolder = bitParOuputPath + "Sentence" + sentenceIndexPad + "/";
				new File(sentenceFolder).mkdir();
				File testSenteceBitParFormat = 
					new File(sentenceFolder + "sentence_" + sentenceIndexPad + "_words.txt");
				createTestSentenceFile(sentencePosWord, testSenteceBitParFormat);
				File sentenceGrammarFile = new File(sentenceFolder + "GRAMMAR.txt");
				File sentenceLexiconFile = new File(sentenceFolder + "LEXICON.txt");
				ConvertGrammarInBitParFormat.probGrammarForOneSentenceNew(probGrammarFile, 
						sentencePosWord, sentenceGrammarFile, sentenceLexiconFile, lexPosSeparationChar);
				float tookSec = (float)(System.currentTimeMillis()-startTime) / 1000;
			reportLine("Took " + tookSec);																	
			startTime = System.currentTimeMillis();
			reportLine("Parsing sentence...");
				File outputBitParSentenceFile = new File(
						sentenceFolder + "sentence_" + sentenceIndexPad  + "_bitParOut.txt");
				File outputBitParSentencePostProcessedFile = new File(
						sentenceFolder + "sentence_" + sentenceIndexPad  + "_bitParOut_postprocessed.txt");
				runBitPar(sentenceGrammarFile, sentenceLexiconFile, 
						testSenteceBitParFormat, outputBitParSentenceFile, sentenceFolder);				
				posprocessNbest(outputBitParSentenceFile, outputBitParSentencePostProcessedFile, ruleFragmentTable, pwBest);
				FileUtil.append(outputBitParSentencePostProcessedFile, pwComplete);
				pwComplete.println();
				tookSec = (float)(System.currentTimeMillis()-startTime) / 1000;
				reportLine("Finished parsing. Took " + tookSec + " sec.");
			reportLine("");
			//if (sentenceIndex==3) break;					
		}
		pwBest.close();
		pwLog.close();
		pwComplete.close();
		return ouputBitParPosProcessedBestFile;
	}
	
	private static HashSet<String> getTestSentencesPosWordSet(File testCorpus) throws Exception {
		HashSet<String> result = new HashSet<String>();
		ArrayList<TSNodeLabel> corpus = TSNodeLabel.getTreebank(testCorpus);				
		for(TSNodeLabel t : corpus) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for(TSNodeLabel l : lex) {
				String word = l.label();
				String pos = l.parent.label();
				result.add(pos + lexPosSeparationString + word);
			}
		}
		return result;
	}
	
	public static File buildAndParseUniqueGrammar(String workingDir, File testFile, 
			File fragmentsFile, int nBest) throws Exception {
		//FILES DEFINITION				
		File fragmentsUniqueFile = new File(workingDir + "ALL_FRAGMENTS_UNIQUE.txt");
		File probGrammarFile = new File(workingDir + "CFG_FREQ_GRAMMAR.txt");
		File cfgRuleFragmentMappingFile = new File(workingDir + "CFG_RULE_FRAGMENT_MAPPING.txt");
		File testSentencesBitParFormat = new File(workingDir + "Test_BitPar_Format.txt");
		File ouputBitParFile = new File(workingDir + "BITPAR_OUTPUT.txt");
		File ouputBitParPosProcessedFile = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED.txt");
		File ouputBitParPosProcessedBestFile = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED_BEST.txt");
		File grammarFile = new File(workingDir + "GRAMMAR.txt");
		File lexiconFile = new File(workingDir + "LEXICON.txt");
		File parseLog = new File(workingDir + "parse.log");
		pwLog = FileUtil.getPrintWriter(parseLog);
		
		//MAKE CFG GRAMMAR AND MAPPING CFG-FRAGMETNS
		makeFragmentsUniqueFile(fragmentsFile, fragmentsUniqueFile);
		File ambiguousFragmentsLogFile = new File(workingDir + "log_ambiguousFragmetnsCFG.txt");
		ConvertFragmentsToCFGRulesAmbiguous converter = new ConvertFragmentsToCFGRulesAmbiguous(
				fragmentsUniqueFile, ambiguousFragmentsLogFile);
		converter.printGrammarFile(probGrammarFile);
		converter.printRuleBestFragmentMappingFile(cfgRuleFragmentMappingFile);
		Hashtable<String, TSNodeLabel> ruleFragmentTable = converter.getRuleBestFragmentMappingTable();
		//Hashtable<String, TSNodeLabel> ruleFragmentTable = ConvertFragmentsToCFGRules.readRuleBestFragmentMappingFile(cfgRuleFragmentMappingFile);			
		
		//PARSE TEST SENTENCES
		HashSet<String> testSentencesPosWordSet = getTestSentencesPosWordSet(testFile);
		reportLine("Preparing unique grammar for all sentences.");
		long startTime = System.currentTimeMillis();						
			ConvertGrammarInBitParFormat.probGrammarForAllSentences(probGrammarFile, testSentencesPosWordSet, 
					grammarFile, lexiconFile, lexPosSeparationChar);
			float tookSec = (float)(System.currentTimeMillis()-startTime) / 1000;
		reportLine("Took " + tookSec);		
		String bitParOuputPath = workingDir + "bitparOutput/";
		new File(bitParOuputPath).mkdir();				
		ArrayList<String[]> testSentencesPosWord = getTestSentencesPosWord(testFile);
		PrintWriter pwTestBP = FileUtil.getPrintWriter(testSentencesBitParFormat);
		int sentenceIndex = 0;
		for(String[] sentencePosWord : testSentencesPosWord) {
			//if (sentenceIndex==10) break;
			pwTestBP.println(Utility.joinStringArrayToString(sentencePosWord, "\n")+"\n");
			sentenceIndex++;
		}
		pwTestBP.close();
				
		PrintWriter pwBestFinal = FileUtil.getPrintWriter(ouputBitParPosProcessedBestFile);
		runBitPar(grammarFile, lexiconFile, testSentencesBitParFormat, ouputBitParFile, workingDir);
		posprocessNbest(ouputBitParFile, ouputBitParPosProcessedFile, ruleFragmentTable, pwBestFinal);									
		
		pwBestFinal.close();
		pwLog.close();		
		return ouputBitParPosProcessedBestFile;
	}
	
	private static void runBitPar(File grammarFile, File lexiconFile, 
			File testSentencesWordsFile, File outputBitParSentenceFile, String workingDir) throws Exception {
		Process p = Runtime.getRuntime().exec(
				Parameters.bitparApp + " " + bitparArgs + " " + grammarFile + " " + 
				lexiconFile + " " +  testSentencesWordsFile + " " + outputBitParSentenceFile,
				null, new File(workingDir));
		p.waitFor();
		String line;			
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	    while ((line = input.readLine()) != null) reportLine(line);		    
	    input.close();
	    input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    while ((line = input.readLine()) != null) reportLine(line);
	    input.close();
        p.getOutputStream().close();
        p.getInputStream().close();
        p.getErrorStream().close();
        p.destroy();
	}
		
	static String viterbProbPrefix = "vitprob=";
	static int viterbProbPrefixLength = viterbProbPrefix.length();
	
	private static void posprocessNbest(File outputBitParSentenceFile, File outputBitParSentencePosProcessedFile,
			Hashtable<String, TSNodeLabel> ruleFragmentTable, PrintWriter pwFinal) throws Exception {		
		Scanner scan = FileUtil.getScanner(outputBitParSentenceFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputBitParSentencePosProcessedFile);
		double prob = -1;
		Hashtable<String, double[]> parseProbTable = new Hashtable<String, double[]>(); 
		if (!scan.hasNextLine()) reportLine("EMPTY FILE!!!!");
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) {
				if (parseProbTable.isEmpty()) continue;
				pwFinal.println(Utility.getMaxKey(parseProbTable));
				parseProbTable.clear();
				pw.println();
				continue;
			}
			if (line.startsWith(viterbProbPrefix)) {
				prob = Double.parseDouble(line.substring(viterbProbPrefixLength));
				pw.println(line);
				continue;
			}
			line = line.replaceAll("\\\\", "");			
			TSNodeLabel tree = null;
			String fragmentConvertedString = null;
			if (line.startsWith("No parse for: ")) {
				tree = new TSNodeLabel("(FIX FIX)");
				reportLine("NO PARSE SENTENCE!!");
				fragmentConvertedString = tree.toString();
			}
			else {
				tree = new TSNodeLabel(line);
				fixLexiconInTree(tree);
				fragmentConvertedString = tree.replaceRulesWithFragments(ruleFragmentTable).toString();				
			}						 			
			pw.println(fragmentConvertedString);
			Utility.increaseStringDoubleArray(parseProbTable, fragmentConvertedString, prob);
		}		
		pw.close();
		scan.close();
	}
	
	private static void posprocessNbest(File outputBitParSentenceFile, File outputBest) throws Exception {		
		Scanner scan = FileUtil.getScanner(outputBitParSentenceFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputBest);
		double prob = -1;
		Hashtable<String, double[]> parseProbTable = new Hashtable<String, double[]>(); 
		if (!scan.hasNextLine()) reportLine("EMPTY FILE!!!!");
		int sentenceIndex = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) {				
				if (parseProbTable.isEmpty()) continue;
				System.out.println(++sentenceIndex);
				pw.println(Utility.getMaxKey(parseProbTable));
				parseProbTable.clear();
				continue;
			}
			if (line.startsWith(viterbProbPrefix)) {
				prob = Double.parseDouble(line.substring(viterbProbPrefixLength));
				continue;
			}
			line = line.replaceAll("\\\\", "");			
			TSNodeLabel tree = new TSNodeLabel(line);			 						
			Utility.increaseStringDoubleArray(parseProbTable, tree.toString(), prob);
		}		
		pw.close();
		scan.close();
	}
	
	
	private static void fixLexiconInTree(TSNodeLabel tree) {		
		ArrayList<TSNodeLabel> lexicon = tree.collectLexicalItems();
		for(TSNodeLabel l : lexicon) {
			if (l.label().equals(l.parent.label())) {
				l.parent.daughters = null;
				l.parent.isLexical = true;
			}
			else {
				String label = l.label();
				String newLabel = label.substring(label.indexOf(lexPosSeparationChar)+1);
				l.relabel(newLabel);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private static void addOneInKernelFragmentFileAndSwapColumns(File inputFile, File outputFile) {
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			String[] freqTree = line.split("\t");
			String tree = freqTree[0];
			int newFreq = Integer.parseInt(freqTree[1])+1;			
			pw.println(newFreq + "\t" + tree);
		}
		pw.println();
		scan.close();
		pw.close();		
	}
	
	private static void concatFilesAndSwapColumns(File[] fileList, File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(File f : fileList) {
			Scanner scan = FileUtil.getScanner(f);
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.equals("")) continue;
				String[] treeFreq = line.split("\t");						
				pw.println(treeFreq[1] + "\t" + treeFreq[0]);
			}
			scan.close();
		}
		pw.println();
		pw.close();
	}
	
	public static void preProcessTreebank(File inputFile, File outputFile) throws Exception {
		Scanner scan = FileUtil.getScanner(inputFile);		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			line = ConstCorpus.addTop(line);
			TSNodeLabel t = new TSNodeLabel(line);
			t.removeSemanticTags();			
			pw.println(t.toString());
		}
		scan.close();
		pw.close();		
	}
	
	public static void toyGrammarParsing() throws Exception {
		File trainigFile = new File("tmp/toyGrammar.txt");
		//File testFile = new File(workingDir + "wsj-22_TOP.mrg");
		//File kernelFragments = new File(workingDir + "fragments_MUB_freq_all.txt");
		//File kernelFragmentsCorrect = new File(workingDir + "fragments_MUB_freq_all_plusOne_freqFirst.txt");
		String fragmentDir = "tmp/";
		extractFragmentsFileDepths(trainigFile, fragmentDir, 10, 100);
		//addOneInKernelFragmentFileAndSwapColumns(kernelFragments, kernelFragmentsCorrect);
		/*File allFragmentFile = new File(workingDir + "ALL_FRAGMENTS");
		File parsedFile = buildAndParse(workingDir, testFile, allFragmentFile);
		File evalF = new File(workingDir + "evalF.txt");
		File evalFLog = new File(workingDir + "evalF.log");
		File evalB = new File(workingDir + "evalB.txt");		
		EvalF.staticEvalF(testFile, parsedFile, evalF, evalFLog, true);
		new EvalB(testFile, parsedFile, evalB, true);*/
	}
	
	public static void smallParsing() throws Exception {
		String workingDir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/parsing/SmallGrammar1000L1/";
		//File trainigFile = new File(workingDir + "wsj-02-21_TOP.mrg");
		File testFile = new File(workingDir + "wsj-22_TOP.mrg");
		//File kernelFragments = new File(workingDir + "fragments_MUB_freq_all.txt");
		//File kernelFragmentsCorrect = new File(workingDir + "fragments_MUB_freq_all_plusOne_freqFirst.txt");
		//File fragment_cfg = new File(workingDir + "fragments_CFG_");
		//extractFragmentsFileDepths(trainigFile, fragment_cfg, 1, 100);
		//addOneInKernelFragmentFileAndSwapColumns(kernelFragments, kernelFragmentsCorrect);
		File allFragmentFile = new File(workingDir + "ALL_FRAGMENTS");
		File parsedFile = buildAndParse(workingDir, testFile, allFragmentFile);
		File evalF = new File(workingDir + "evalF.txt");
		File evalFLog = new File(workingDir + "evalF.log");
		//File evalB = new File(workingDir + "evalB.txt");		
		EvalC.staticEvalF(testFile, parsedFile, evalF, evalFLog, true);
		//new EvalB(testFile, parsedFile, evalB, true);
	}
	
	public static void bigParsing() throws Exception {
		String workingDir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/parsing/BigGrammar1000L1_SemTagOff/";		
		//File testFile = new File(workingDir + "wsj-22.mrg");
		File testFileClean = new File(workingDir + "wsj-22_clean.mrg");
		//cleanSlashInFile(testFile, testFileClean);
		
		File trainigFile = new File(workingDir + "wsj-02-21_TOP_semTagOff.mrg");		
		//ConstCorpus corpus = new ConstCorpus(trainigFile, FileUtil.defaultEncoding);
		//corpus.removeSemanticTags();
		//corpus.toFile_Complete(trainigFile, false);
		File kernelFragmentFile = new File(workingDir + "fragments_MUB_freq_all_semTagOff.txt");
		File cfgRulesFile = new File(workingDir + "cfgRules_semTagOff.txt");
		//extractCFGrules(trainigFile, cfgRulesFile);
		File kernelCfgFile = new File(workingDir + "fragments_CFG_all.txt");
		//concatFilesAndSwapColumns(new File[]{kernelFragmentFile, cfgRulesFile}, kernelCfgFile);		
		File parsedFile = buildAndParse(workingDir, testFileClean, kernelCfgFile);
		//File evalF = new File(workingDir + "1Best_evalF_semTagOff.txt");				
		//EvalF.staticEvalF(testFileClean, parsedFile, evalF, true);
		File evalF = new File(workingDir + "1Best_evalF_semTag.txt");
		EvalC.REMOVE_SEMANTIC_TAGS = false;
		EvalC.staticEvalF(testFileClean, parsedFile, evalF, true);
	} 
	
	public static void cfgParsing() throws Exception {
		int nBest = 1000;
		String workingDir = Parameters.resultsPath + "TSG/CFG/Nbest/";
		File trainigFile = new File(workingDir + "wsj-02-21_TOP.mrg");
		ArrayList<TSNodeLabel> corpus = TSNodeLabel.getTreebank(trainigFile);
		//extractFragmentsFileDepths(trainigFile, workingDir, 1, 100);		
		File testFile = new File(workingDir + "wsj-22.mrg");		
		File cfgRulesFile = new File(workingDir + "wsj-02-21_TOP_CFG_RULES.txt");
		File parsedFile = buildAndParseUniqueGrammar(workingDir, testFile, cfgRulesFile, nBest);
		File evalF = new File(workingDir + "evalF.txt");
		File evalFLog = new File(workingDir + "evalF.log");
		EvalC.staticEvalF(testFile, parsedFile, evalF, evalFLog, true);		
	}
	
	public static void cleanSlashInFile(File inputFile, File outputFile) {
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			pw.println(line.replaceAll("\\\\", ""));
		}		
		scan.close();
		pw.close();
	}
	
	public static void makeBestOnly() throws Exception {
		String workingDir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/parsing/BigGrammar1000L1_SemTagOff/";
		File goldFileClean = new File(workingDir + "wsj-22_clean.mrg");
		File parsedFile = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED.txt");
		File bestFile = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED_BEST.txt");
		posprocessNbest(parsedFile, bestFile);
		File evalF = new File(workingDir + "Best_evalF_semTagOff.txt");				
		EvalC.staticEvalF(goldFileClean, bestFile, evalF, true);
		//evalF = new File(workingDir + "Best_evalF_semTagOn.txt");
		//EvalF.REMOVE_SEMANTIC_TAGS = false;
		//EvalF.staticEvalF(goldFileClean, bestFile, evalF, true);
	}
	
	public static void postProcessOnly() throws Exception {
		String workingDir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/parsing/BigGrammar1000L1_SemTagOff/";
		File output = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED.txt");
		PrintWriter pw = FileUtil.getPrintWriter(output);
		workingDir += "bitparOutput/";
		for(int i=1; i<=1700; i++) {
			String numberPad = Utility.padZero(4, i);
			String dir = "Sentence" + numberPad + "/";
			File inputFile = new File(workingDir + dir + "sentence_" + numberPad + "_bitParOut_postprocessed.txt");
			Scanner scan = FileUtil.getScanner(inputFile);
			while(scan.hasNextLine()) pw.println(scan.nextLine());
		}
		pw.close();
	}
	
	public static void main(String[] args) throws Exception {
		//cfgParsing();		
		//smallParsing();
		//toyGrammarParsing();
		//postProcessOnly();
		//bigParsing();
		makeBestOnly();
	}
	
}
