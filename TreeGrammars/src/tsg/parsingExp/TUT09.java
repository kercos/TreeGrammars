package tsg.parsingExp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Map.Entry;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelStructure;
import tsg.corpora.ConstCorpus;
import tsg.kernels.CommonSubtreesMUBFreqFlush;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class TUT09 {

	public static String corpusPath = Parameters.corpusPath + "Evalita09/Treebanks/Constituency/";
	public static File trainFile = new File(corpusPath + "TUTinPENN-train.readable.penn");
	public static File trainFileNoTraces = new File(corpusPath + "TUTinPENN-train.readable.notraces.penn");
	public static File trainFileNoTracesNoSemTags = new File(corpusPath + 
			"TUTinPENN-train.readable.notraces.noSemTags.penn");
	public static File testFile = new File(corpusPath + "TUTinPENN-Evalita09-testset14-9-09.penn");	
	
	public static String lexPosSeparationString = "^";
	public static char lexPosSeparationChar = lexPosSeparationString.charAt(0);
	
	static int nBest = 1000;
	static String bitparApp = "/home/fsangati/SOFTWARE/BitPar_Web/bitpar";			
	static String bitparArgs = "-vp -b " + nBest + " -o -s TOP ";	
	
	public static void cleanCorpus() {
		ConstCorpus trainingCorpus = new ConstCorpus(trainFile, "UTF-8");
		trainingCorpus.removeTraces("-NONE-");
		trainingCorpus.removeNumbersInLables();		
		trainingCorpus.toFile_Complete(trainFileNoTraces, false);
		trainingCorpus.removeSemanticTags();
		trainingCorpus.toFile_Complete(trainFileNoTracesNoSemTags, false);
	}
	
	public static void buildKernelFragmetnFile() throws Exception {
		System.out.println("Max depth: " + CommonSubtreesMUBFreqFlush.maxDepth);
		//File inputFile = TUT09.trainFileNoTraces;
		String dirPath = Parameters.resultsPath + "TSG/TSGkernels/TUT09/SemTagOn/";
		File inputFile = new File(dirPath + "TUTinPENN-train.readable.notraces_quotesFixed.penn");
		//String outputFolder = Parameters.resultsPath + "TSG/TSGkernels/TUT09/" + FileUtil.dataFolder() + "/";
		//new File(outputFolder).mkdirs();
		File learningCurveFileC0 = new File(dirPath + "learningFragments_C0_MUB_freq_all.txt");
		File learningCurveFileC1 = new File(dirPath + "learningFragments_C1_MUB_freq_all.txt");
		File learningCurveFileTot = new File(dirPath + "learningFragments_Tot_MUB_freq_all.txt");
		ArrayList<TSNodeLabelStructure> treebank = 
			TSNodeLabelStructure.readTreebank(inputFile, "UTF-8", 20000);
		System.out.println("Treebank size: " + treebank.size());
		CommonSubtreesMUBFreqFlush cs = new CommonSubtreesMUBFreqFlush(treebank);
		cs.extractFromTreebankAndLearningCurve(learningCurveFileC0, learningCurveFileC1, learningCurveFileTot);
		String fragmentDepthReport = cs.reportFragmentDepth(); 
		FileUtil.appendReturn(fragmentDepthReport, new File(dirPath + "fragmentDepthReport_MUB_freq_all.txt"));
		System.out.println(fragmentDepthReport);		
		cs.printFragmentsToFile(new File(dirPath + "fragments_MUB_freq_all.txt"));
	}
	
	@SuppressWarnings("unused")
	private static void addOneInKernelFragmentFileAndSwapColumns() {
		String dirPath = Parameters.resultsPath + "TSG/TSGkernels/TUT09/SemTagOn/";
		File fragmentFile = new File(dirPath + "fragments_MUB_freq_all.txt");
		File outputFile = new File(dirPath + "fragments_MUB_freq_all_freqFirst_plusOne.txt");
		Scanner scan = FileUtil.getScanner(fragmentFile, "UTF-8");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile, "UTF-8");
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
	
	public static void extractCFGFileDepths() throws Exception {
		boolean semTag = true;
		File inputFile = semTag ? trainFileNoTraces : trainFileNoTracesNoSemTags;
		String basePath = Parameters.resultsPath + "TSG/TSGkernels/TUT09/";		
		String outputFolder =  basePath + (semTag? "SemTagOn/" : "SemTagOff/");
		File outputFile = new File(outputFolder + "fragments_CFG_freq_all.txt");
		Hashtable<String, int[]> cfgRules = new Hashtable<String, int[]>();
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		
		for(TSNodeLabel t : treebank) {					
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();									
				Utility.increaseStringIntArray(cfgRules, rule);
			}
		}		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<String, int[]> e : cfgRules.entrySet()) {
			String rule = e.getKey();
			int freq = e.getValue()[0];
			TSNodeLabel ruleTree = new TSNodeLabel(rule, false);			
			pw.println(freq + "\t" + ruleTree.toString(false, true));			
		}
		pw.close();
		
	}
	
	@SuppressWarnings("unchecked")
	public static void extractFragmentsFileDepths() throws Exception {		
		int maxDepth = 1;
		int maxProle = 100;
		String dirPath = Parameters.resultsPath + "TSG/TSGkernels/TUT09/SemTagOn/";
		File inputFile = new File (dirPath +  "TUTinPENN-train.readable.notraces_quotesFixed.penn");		
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
			File outputFile = new File(dirPath + "fragments_Depth_" + depth + 
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
	
	/**
	 * TEST_LEX TRAIN_LEX TEST_POS TRAIN_POS
	 * , , PUNCT ,
	 * . . PUNCT .
	 * " " PUNCT "
	 * ( -LRB- PUNCT -LRB-
	 * ) -RRB- PUNCT -RRB-
	 * ... ... PUNCT .
	 * ? ? PUNCT ?
	 * ; ; PUNCT :
	 * : : PUNCT : 
	 * @throws Exception 
	 */
	public static String[] changingAllowed = 
		new String[]{"-RRB-", "''", "...", "-LRB-", ".", ",", ";", ":", "?"};
	
	public static void prepareTest() throws Exception {
		String dirPath = Parameters.resultsPath + "TSG/TSGkernels/TUT09/";
		File trainigFile = new File(dirPath + "TUTinPENN-train.readable.notraces_quotesFixed.penn");
		File testFile = new File(dirPath + "TUTinPENN-Evalita09-testset14-9-09_quotesFixed.penn");
		File testFileFixed = new File(dirPath + "TUTinPENN-Evalita09-testset14-9-09_quotesFixed_posFixed.penn");
		File testFileFixedLog = new File(dirPath + "TUTinPENN-Evalita09-testset14-9-09_quotesFixed_posFixed.log");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainigFile, "UTF-8", 10000);
		Hashtable<String, HashSet<String>> lexPosTable = TSNodeLabel.getLexPosTableFromTreebank(treebank);
		HashSet<String> posSet = TSNodeLabel.getPosSetFromTreebank(treebank);
		ArrayList<String[]> testSentencesPosWord = getTestSentencesPosWord(testFile);
		PrintWriter pw = FileUtil.getPrintWriter(testFileFixed, "UTF-8");
		HashSet<String> changingWords = new HashSet<String>(Arrays.asList(changingAllowed));
		PrintWriter pwLog = FileUtil.getPrintWriter(testFileFixedLog, "UTF-8");
		pwLog.println("Changing allowed:\n" + changingWords.toString() + "\n\n");
		int sentenceIndex = 1;		 
		for(String[] sentencePosWord : testSentencesPosWord ) {
			pwLog.println("Sentence: " + sentenceIndex++);
			int posWordIndex = 1;
			for(String posWord : sentencePosWord) {
				//String[] posWordSplit = posWord.split(lexPosSeparationString);
				int lexSepIndex = posWord.indexOf(lexPosSeparationChar);
				String pos = posWord.substring(0, lexSepIndex);
				String word = posWord.substring(lexSepIndex+1);
				if (word.equals("(")) {
					pwLog.println("Fixed open bracket '(' to '-LRB-'");
					word = "-LRB-";
				}
				if (word.equals(")")) {
					pwLog.println("Fixed closed bracket ')' to '-RRB-'");
					word = "-RRB-";
				}
				HashSet<String> storedPosWord = lexPosTable.get(word);
				String newPos = null;
				if (storedPosWord==null) {
					newPos = pos;
					pwLog.println("Unknown word: " + word + " (" + newPos + ")");
					if (!posSet.contains(pos)) pwLog.println("Unknown pos: " + pos);
				}
				else if (storedPosWord.contains(pos)) {
					newPos = pos;					
				}
				else {
					pwLog.print("Known word '" + word + "' with unseen pos: '" + pos + "' ");
					if (!posSet.contains(pos)) pwLog.print("(Unknown pos) ");
					if (storedPosWord.size()==1) {
						if (changingWords.contains(word) || !posSet.contains(pos)) {
							newPos = storedPosWord.iterator().next();
							pwLog.println("Change to " + newPos);
						}
						else {
							pwLog.println("No change, word not allowed, probably unknown.");
							newPos = pos;
						}
					}
					else {
						newPos = pos;
						pwLog.println("No change, multiple choice: " + storedPosWord);
					}
				}
				pw.println(posWordIndex++ + " " + word + " (" + newPos + ")");				
			}
			pwLog.println();
			pw.println();
		}		
		pw.close();		
		pwLog.close();
	}
	
	private static void reportLine(String line) {
		System.out.println(line);
		pwLog.println(line);
	}
	
	static PrintWriter pwLog;
	
	public static void buildAndParseUniqueGrammar() throws Exception {
		//FILES DEFINITION
		nBest = 1000;
		String parentPath = Parameters.resultsPath + "TSG/TSGkernels/TUT09/SemTagOff/";
		String ouputPath = parentPath + "MediumParse1000Unique/";
		File testFile = new File(parentPath + "TUTinPENN-Evalita09-testset14-9-09_quotesFixed_posFixed.penn");
		File fragmentsFile = new File(ouputPath + "ALL_FRAGMENTS.txt");
		File fragmentsUniqueFile = new File(ouputPath + "ALL_FRAGMENTS_UNIQUE.txt");
		File probGrammarFile = new File(ouputPath + "CFG_FREQ_GRAMMAR.txt");
		File cfgRuleFragmentMappingFile = new File(ouputPath + "CFG_RULE_FRAGMENT_MAPPING.txt");		
		File ouputBitParPosProcessedFile = new File(ouputPath + "BITPAR_OUTPUT_POSTPROCESSED_BEST.txt");
		File grammarFile = new File(ouputPath + "GRAMMAR.txt");
		File lexiconFile = new File(ouputPath + "LEXICON.txt");
		File parseLog = new File(ouputPath + "parse.log");
		pwLog = FileUtil.getPrintWriter(parseLog, "UTF-8");
		
		//MAKE CFG GRAMMAR AND MAPPING CFG-FRAGMETNS
		makeFragmentsUniqueFile(fragmentsFile, fragmentsUniqueFile);
		File ambiguousFragmentsLogFile = new File(ouputPath + "log_ambiguousFragmetnsCFG.txt");
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
		String bitParOuputPath = ouputPath + "bitparOutput/";
		new File(bitParOuputPath).mkdir();				
		ArrayList<String[]> testSentencesPosWord = getTestSentencesPosWord(testFile);
		int maxCounterLength = Integer.toString(testSentencesPosWord.size()).length();
		PrintWriter pwFinal = FileUtil.getPrintWriter(ouputBitParPosProcessedFile, "UTF-8");
		int sentenceIndex = 1;
		for(String[] sentencePosWord : testSentencesPosWord) {																		
			startTime = System.currentTimeMillis();
			reportLine("Parsing sentence " + sentenceIndex);				
				String sentenceIndexPad = Utility.padZero(maxCounterLength, sentenceIndex);
				String sentenceFolder = bitParOuputPath + "Sentence" + sentenceIndexPad + "/";
				new File(sentenceFolder).mkdir();
				File testSenteceBitParFormat = 
					new File(sentenceFolder + "sentence_" + sentenceIndexPad + "_words.txt");
				File outputBitParSentenceFile = new File(
						sentenceFolder + "sentence_" + sentenceIndexPad  + "_bitParOut.txt");
				createTestSentenceFile(sentencePosWord, testSenteceBitParFormat);
				runBitPar(grammarFile, lexiconFile, 
						testSenteceBitParFormat, outputBitParSentenceFile, sentenceFolder);
				posprocessNbest(outputBitParSentenceFile, ruleFragmentTable, pwFinal);
				tookSec = (float)(System.currentTimeMillis()-startTime) / 1000;
				reportLine("Finished parsing. Took " + tookSec + " sec.");
			reportLine("");
			//if (sentenceIndex==3) break;
			sentenceIndex++;			
		}
		pwFinal.close();
		pwLog.close();
	}

	public static void buildAndParse() throws Exception {
		//FILES DEFINITION
		String parentPath = Parameters.resultsPath + "TSG/TSGkernels/TUT09/SemTagOn/";
		String ouputPath = parentPath + "EasyParse1000L1/";
		File testFile = new File(parentPath + "TUTinPENN-Evalita09-testset14-9-09_quotesFixed_posFixed.penn");
		File fragmentsFile = new File(ouputPath + "ALL_FRAGMENTS.txt");
		File fragmentsUniqueFile = new File(ouputPath + "ALL_FRAGMENTS_UNIQUE.txt");
		File probGrammarFile = new File(ouputPath + "CFG_FREQ_GRAMMAR.txt");
		File cfgRuleFragmentMappingFile = new File(ouputPath + "CFG_RULE_FRAGMENT_MAPPING.txt");		
		File ouputBitParPosProcessedFile = new File(ouputPath + "BITPAR_OUTPUT_POSTPROCESSED_BEST.txt");
		File parseLog = new File(ouputPath + "parse.log");
		pwLog = FileUtil.getPrintWriter(parseLog, "UTF-8");
		
		//MAKE CFG GRAMMAR AND MAPPING CFG-FRAGMETNS
		makeFragmentsUniqueFile(fragmentsFile, fragmentsUniqueFile);
		File ambiguousFragmentsLogFile = new File(ouputPath + "log_ambiguousFragmetnsCFG.txt");
		ConvertFragmentsToCFGRulesAmbiguous converter = new ConvertFragmentsToCFGRulesAmbiguous(
				fragmentsUniqueFile, ambiguousFragmentsLogFile);
		converter.printGrammarFile(probGrammarFile);
		converter.printRuleBestFragmentMappingFile(cfgRuleFragmentMappingFile);
		Hashtable<String, TSNodeLabel> ruleFragmentTable = converter.getRuleBestFragmentMappingTable();
		//Hashtable<String, TSNodeLabel> ruleFragmentTable = ConvertFragmentsToCFGRules.readRuleBestFragmentMappingFile(cfgRuleFragmentMappingFile);			
		
		//PARSE TEST SENTENCES
		ArrayList<String[]> testSentencesPosWord = getTestSentencesPosWord(testFile);
		String bitParOuputPath = ouputPath + "bitparOutput/";
		new File(bitParOuputPath).mkdir();		
		int sentenceIndex = 0;
		int maxCounterLength = Integer.toString(testSentencesPosWord.size()).length();
		PrintWriter pwFinal = FileUtil.getPrintWriter(ouputBitParPosProcessedFile, "UTF-8");
		for(String[] sentencePosWord : testSentencesPosWord) {
			sentenceIndex++;
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
				runBitPar(sentenceGrammarFile, sentenceLexiconFile, 
						testSenteceBitParFormat, outputBitParSentenceFile, sentenceFolder);
				posprocessNbest(outputBitParSentenceFile, ruleFragmentTable, pwFinal);
				tookSec = (float)(System.currentTimeMillis()-startTime) / 1000;
				reportLine("Finished parsing. Took " + tookSec + " sec.");
			reportLine("");
			//if (sentenceIndex==3) break;					
		}
		pwFinal.close();
		pwLog.close();
	}

	private static void createTestSentenceFile(String[] flatWordArrayPosTerminal, File testSentencesPreprocessed) {
		PrintWriter flatPW = FileUtil.getPrintWriter(testSentencesPreprocessed);
		flatPW.println(Utility.joinStringArrayToString(flatWordArrayPosTerminal, "\n")+"\n");
		flatPW.close();
	}
	
	private static void runBitPar(File grammarFile, File lexiconFile, 
			File testSentencesWordsFile, File outputBitParSentenceFile, String workingDir) {
		try {					
			Process p = Runtime.getRuntime().exec(
				bitparApp + " " + bitparArgs + " " + grammarFile + " " + 
				lexiconFile + " " +  testSentencesWordsFile + " " + outputBitParSentenceFile,
				null, new File(workingDir));
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    while ((line = input.readLine()) != null) reportLine(line);
		    input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		    while ((line = input.readLine()) != null) reportLine(line);
		    input.close();
		}
		catch (Exception err) {
		   err.printStackTrace();
		}
	}
	
	static String viterbProbPrefix = "vitprob=";
	static int viterbProbPrefixLength = viterbProbPrefix.length();
	
	private static void posprocessNbest(File outputBitParSentenceFile,
			Hashtable<String, TSNodeLabel> ruleFragmentTable, PrintWriter pwFinal) throws Exception {
		File outputBitParSentencePosProcessedFile = new File(outputBitParSentenceFile.getParent() + "/" +
				FileUtil.getFileNameWithoutExtensions(outputBitParSentenceFile) + "_posprocessed.txt");
		Scanner scan = FileUtil.getScanner(outputBitParSentenceFile, "UTF-8");
		PrintWriter pw = FileUtil.getPrintWriter(outputBitParSentencePosProcessedFile, "UTF-8");
		double prob = -1;
		Hashtable<String, double[]> parseProbTable = new Hashtable<String, double[]>(); 
		if (!scan.hasNextLine()) reportLine("EMPTY FILE!!!!");
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
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
		pwFinal.println(Utility.getMaxKey(parseProbTable));
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

	private static void makeFragmentsUniqueFile(File fragmentsFile, File fragmentsUniqueFile) {
		Hashtable<String, int[]> fragFreq = new Hashtable<String, int[]>();
		Scanner scan = FileUtil.getScanner(fragmentsFile, "UTF-8");
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
		
		PrintWriter pw = FileUtil.getPrintWriter(fragmentsUniqueFile, "UTF-8");
		for(Entry<String, int[]> e : fragFreq.entrySet()) {
			pw.println(e.getValue()[0] + "\t" + e.getKey());
		}
		pw.println();
		pw.close();		
	}

	//25 . (PUNCT)
	private static ArrayList<String[]> getTestSentencesPosWord(File testFile) {
		ArrayList<String[]> result = new ArrayList<String[]>();
		Scanner scan = FileUtil.getScanner(testFile, "UTF-8");
		ArrayList<String> sentencePosWords = new ArrayList<String>(); 
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("") || !line.matches("^\\d+.+")) {
				if (!sentencePosWords.isEmpty()) {
					result.add(sentencePosWords.toArray(new String[sentencePosWords.size()]));
					sentencePosWords.clear();
				}
				continue;
			}
			String[] lineSplit = line.split("\\s+"); // index, word, (POS)
			String word = lineSplit[1];
			String pos = lineSplit[2];
			pos = pos.substring(1, pos.length()-1);
			sentencePosWords.add(pos + lexPosSeparationString + word);
		}
		return result;
	}
	
	private static HashSet<String> getTestSentencesPosWordSet(File testFile) {
		HashSet<String> result = new HashSet<String>();
		Scanner scan = FileUtil.getScanner(testFile, "UTF-8");
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("") || !line.matches("^\\d+.+")) continue;
			String[] lineSplit = line.split("\\s+"); // index, word, (POS)
			String word = lineSplit[1];
			String pos = lineSplit[2];
			pos = pos.substring(1, pos.length()-1);
			result.add(pos + lexPosSeparationString + word);
		}
		return result;
	}
	
	public static void getLexStatistics() throws Exception {
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainFile);
		File lexTableFile = new File(corpusPath + "lexiconPosFreq.txt");
		Hashtable<String, int[]> lexTable = new Hashtable<String, int[]>(); 
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for(TSNodeLabel l : lex) {
				Utility.increaseStringIntArray(lexTable, l.parent.label()+"_"+l.label());
			}
		}
		PrintWriter pw = FileUtil.getPrintWriter(lexTableFile);
		for(Entry<String, int[]> e : lexTable.entrySet()) {
			pw.println(e.getValue()[0] + " " + e.getKey());
		}
		pw.close();
	}
	
	public static void getFragmentFreq() throws Exception {
		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainFileNoTracesNoSemTags);
		File fragmentFile = new File("/Users/fsangati/Documents/UNIVERSITY/UVA/Papers/Evalita09/constPaper/Fragments/fragments_MUB_freq_all_sorted_VMA_�.txt");
		ArrayList<TSNodeLabel> treebankFragments = TSNodeLabel.getTreebank(fragmentFile, false);
		int i=0;
		for(TSNodeLabel target : treebankFragments) {
			i++;
			int freq = TSNodeLabel.countFragmentInTreebank(treebank, target);
			System.out.println(target + "\t" + freq);
			if (i==100) break;
		}
		//TSNodeLabel target = treebankFragments.get(11);
		//TSNodeLabel.printSearch(treebank, target, Integer.MAX_VALUE);
		
	}

	public static void main(String[] args) throws Exception {
		//buildKernelFragmetnFile();
		//addOneInKernelFragmentFileAndSwapColumns();
		//extractFragmentsFileDepths();		
		//prepareTest();
		//buildAndParse();
		//buildAndParseUniqueGrammar();
		//getLexStatistics();
		getFragmentFreq();
	}


	
}
