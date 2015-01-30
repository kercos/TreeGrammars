package tsg.parsingExp;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationLeft_Petrov;
import tsg.metrics.MetricOptimizerArray;
import tsg.metrics.ParseMetricOptimizer;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import tsg.utils.CleanPetrov;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;
import wordModel.WordFeatures;

public abstract class TSGparsingBitPar extends Thread {
	
	final static String lexPosSeparationString = "^";
	final static char lexPosSeparationChar = lexPosSeparationString.charAt(0);
	
	static UkWordMapping ukModel;
	
	static int nBest = 1000;
	static int fragmentFreqThreashold = -1;
	static boolean extractUnseenCFGrules = true;	
	static int threads;
	static int sentenceLengthLimitTest, restrictTestToFirst;
	
	static boolean sortGrammar = false;
	static boolean usingPetrov = false;
	//static boolean getLexiconFromTreebank = false;
	
	static boolean parse = true;
	static boolean removeTmpFiles = true;
	static String bitparApp = Parameters.bitparApp;
	static String mjCKYApp = Parameters.mjCKYApp;
	static String bitparCommandAndArgs;
		
	final public static String internalFakeNodeLabel = "NODE@";
	final static int internalFakeNodeLabelLength = 5;
	final static String fakePrelexPrefix = "^";		
	
	static boolean markoBinarize;
	static TreeMarkoBinarization treeMarkovBinarizer;
	
	static boolean freqAreInt = true;
	
	static double petrovSmoothingFactor;
	
	static boolean smoothLexicon;
	static double smoothLexFactor = 0.01;
	static int openClassThreshold = 50;
	
	static double pruneThreshold = -1;
	
	static boolean charBased = false;
	static boolean shortestDerivation = false;
	
	String outputPath;	
	
	File trainingFile, testFile, testFileClean;
	ArrayList<TSNodeLabel> trainingTreebank, testTreebank;
	ArrayList<TSNodeLabel> originalTrainingTreebank, originalTestTreebank;
	int trainingSize, testSize;
	
	File fragmentsFile, finalFragmentsFile;	
	File bibtpar_grammarFile, bitpar_lexiconFile;
	
	File[] parsedOutputFiles;
	String[] parsedOutputFilesIdentifiers;
	
		
	TreeSet<String> trainingLexicon, testLexiconUnwnown;
	
	Vector<TSNodeLabel> ambiguousCFGmapping;
	Hashtable<String, TSNodeLabel> unambiguousCFGmapping;	
	PrintProgress progress;
	
	String topSymbol;	
	DefaultParseMaker defaultParseMaker;
	int noParsedSentences = 0;

	public TSGparsingBitPar(File trainingFile, File testFile, File fragmentsFile, File outputDir) {
		this.trainingFile = trainingFile;
		this.testFile = testFile;		
		this.fragmentsFile = fragmentsFile;
		this.outputPath = outputDir.exists() ? 
			outputDir + "/" + "Parsing_" + FileUtil.dateTimeString() + "/" : 
			outputDir + "/";
		this.testFileClean = new File(outputPath + "test_clean.mrg");
		new File(outputPath).mkdirs();		
	}	
	
	public void run() {
		
		Parameters.openLogFile(new File(outputPath + "log.txt"));
		long time = System.currentTimeMillis();		
		
		printStartingParameters();
		getTrainingAndTestTreebanks();
		buildDefaultPosTagger();
		preprocessUnknownWords();		
		checkUnknownWords();		
		markoBinarizeTraining();
		getTopSymbol();
		filterFragmentsFreq();
		
		if (petrovSmoothingFactor>0.) { 
			freqAreInt = false;
			extractCFGfreqPetrovSmooth();
		}
		else 
			extractUnseenCFGrules();
				
		makeMappingFragmentsToCFRules();		
		makeLexiconBitPar();		
		
		try {
			smoothLexicon();
		} catch (Exception e1) {			
			e1.printStackTrace();
			return;
		}
		
		if (parse) {			
			System.gc();
			String pruneString = pruneThreshold==-1 ? "" : " -prune " + pruneThreshold; 
			bitparCommandAndArgs = bitparApp + " -vp -b " + nBest + " -s " + topSymbol + pruneString + 
				" " + bibtpar_grammarFile + " " + bitpar_lexiconFile;
			Parameters.reportLineFlush("Parsing with BitPar using " + threads + " threads");
			Parameters.reportLineFlush("Parsing " + testSize + " sentences:");			
			progress = new PrintProgress("Sentence #:");			
			try {
				parseWithBitPar();
			} catch (Exception e) {
				e.printStackTrace();
				Parameters.reportError(e.getMessage());
				return;
			}
			progress.end();			
			parseEval();
			Parameters.reportLineFlush("Number of default parsed sentences: " + noParsedSentences);
		}	
		Parameters.reportLineFlush("Took: " + (System.currentTimeMillis() - time)/1000 + " seconds.");
		Parameters.closeLogFile();
	}

	
	private void getTrainingAndTestTreebanks() {		
		try {
			Parameters.reportLineFlush("Reading Traininig Treebank");
			trainingTreebank = TSNodeLabel.getTreebank(trainingFile);
			trainingSize = trainingTreebank.size();
			Parameters.reportLineFlush("Traininig Treebank Size: " + trainingSize);
			Parameters.reportLineFlush("Reading Test Treebank");			
			testTreebank = TSNodeLabel.getTreebank(testFile, sentenceLengthLimitTest);
			if (restrictTestToFirst>0) {
				testTreebank = new ArrayList<TSNodeLabel>(testTreebank.subList(0, restrictTestToFirst));
			}
			if (charBased) 
				TSNodeLabel.printTreeFromCharBased(testTreebank, testFileClean);
			else 
				TSNodeLabel.printTreebankToFile(testFileClean, testTreebank, false, false);
			testSize = testTreebank.size();
			Parameters.reportLineFlush("Test Treebank Size: " + testSize);
			originalTrainingTreebank = trainingTreebank;
			originalTestTreebank = testTreebank;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void buildDefaultPosTagger() {
		Parameters.reportLineFlush("Building Defualt PosTagger.");
		defaultParseMaker = new DefaultParseMaker(trainingTreebank);
		Parameters.reportLineFlush("Most frequent POS: " + defaultParseMaker.getMostFrequentPos());		
	}
	
	private void preprocessUnknownWords() {
		if (UkWordMapping.ukThreashold <= 0) return;
		Parameters.reportLineFlush("Processing Unknown Words");	
		ukModel.init(trainingTreebank, testTreebank);
		trainingTreebank = ukModel.transformTrainingTreebank();
		testTreebank = ukModel.transformTestTreebank();
		File transformedTrainingTreBankFile = new File(outputPath + "trainingTreebank_UK.mrg");
		File transformedTestTreBankFile = new File(outputPath + "testTreebank_UK.mrg");
		TSNodeLabel.printTreebankToFile(transformedTrainingTreBankFile, trainingTreebank, false, false);
		TSNodeLabel.printTreebankToFile(transformedTestTreBankFile, testTreebank, false, false);
		Parameters.reportLineFlush("Printed training treebank after unknonw word process to: " + transformedTrainingTreBankFile);
		Parameters.reportLineFlush("Printed test treebank after unknonw word process to: " + transformedTestTreBankFile);
	}
	
	private void markoBinarizeTraining() {		
		if (!markoBinarize) return; 
		Parameters.reportLineFlush("MarkoBinarize Training Treebank");
		trainingTreebank = treeMarkovBinarizer.markoBinarizeTreebank(trainingTreebank);
		File transformedTrainingTreBankFile = new File(outputPath + "trainingTreebank_UK_MB.mrg");
		TSNodeLabel.printTreebankToFile(transformedTrainingTreBankFile, trainingTreebank, false, false);
		Parameters.reportLineFlush("Printed training treebank after MarkoBinarization to: " + transformedTrainingTreBankFile);
	}
	
	private void getTopSymbol() {
		topSymbol = trainingTreebank.get(0).label();
		ParseMetricOptimizer.topLabel = Label.getLabel(topSymbol);
		Parameters.reportLineFlush("Grammar Starting Symbol:" + topSymbol);
	}
	
	private void checkUnknownWords() {	
		
		/*
		trainingLexicon = fragmentsFile==null ?
				getLexiconFromTreebank(trainingTreebank) :
				getLexiconFromFragments(fragmentsFile);
		*/
		
		trainingLexicon = getLexiconFromTreebank(trainingTreebank);
					 
		testLexiconUnwnown = getLexiconFromTreebank(testTreebank);
		testLexiconUnwnown.removeAll(trainingLexicon);
		if (!testLexiconUnwnown.isEmpty()) {
			Parameters.reportLineFlush("Test treebank contains following unknown words:\n" + testLexiconUnwnown);
			Parameters.reportLineFlush("Will Smooth them");
		}							
	}
	
	private void filterFragmentsFreq() {
		if (fragmentsFile==null) {
			Parameters.reportLineFlush("No fragment File");
			return;
		}
		if (fragmentFreqThreashold<=0) {
			Parameters.reportLineFlush("Using all fragments (threashold<=1)");
			return;
		}

		Parameters.reportLineFlush("Filtering Fragments: keeping only those with freq >= " + fragmentFreqThreashold);
		File filteredFile = new File(outputPath + "filteredFragments_ge" + fragmentFreqThreashold + ".txt");
		PrintWriter pw = FileUtil.getPrintWriter(filteredFile);
		Scanner fragmentScan = FileUtil.getScanner(fragmentsFile);
		int originalTotalFragments = 0;
		int filteredFragments = 0;
		while(fragmentScan.hasNextLine()) {
			originalTotalFragments++;
			String line = fragmentScan.nextLine();
			String[] treeFreq = line.split("\t");
			long freq = Long.parseLong(treeFreq[1]);
			if (freq<fragmentFreqThreashold) continue;				
			filteredFragments++;				
			pw.println(line);				
		}
		pw.close();	
		fragmentsFile = filteredFile;
		Parameters.reportLineFlush("Printing filtered fragments to " + filteredFile);
		Parameters.reportLineFlush("Original Fragments # : " + originalTotalFragments);
		Parameters.reportLineFlush("Kept Fragments # : " + filteredFragments);					
		
	}
	
	private void extractUnseenCFGrules() {		
		if (!extractUnseenCFGrules) {
			finalFragmentsFile = fragmentsFile;
			return;		
		}
		File unseenCFGrulesFile = new File(outputPath + "unseenCFG.txt");
		finalFragmentsFile = new File(outputPath + "fragmentsAndCfgRules.txt");
		Parameters.reportLine("Collecting Unseen CFG rules in " + unseenCFGrulesFile);
		Parameters.reportLineFlush("Printing all fragments to " + finalFragmentsFile);
		PrintWriter allFragmPW = FileUtil.getPrintWriter(finalFragmentsFile);				
		HashSet<String> fragmentSet = new HashSet<String>();
		int totalFragments = 0;
		if (fragmentsFile!=null) {
			Scanner fragmentScan = FileUtil.getScanner(fragmentsFile);			
			while(fragmentScan.hasNextLine()) {
				String line = fragmentScan.nextLine();
				String[] treeFreq = line.split("\t");			
				String fragment = treeFreq[0];
				fragmentSet.add(fragment);
				allFragmPW.println(line);
				totalFragments++;
			}
		}
		Hashtable<String, int[]> cfgRulesFreq = extractCFGfreq();
			
		int added = 0;		
		PrintWriter unseenCFRulesPW = FileUtil.getPrintWriter(unseenCFGrulesFile);
		for(Entry<String, int[]> e : cfgRulesFreq.entrySet()) {
			String cfgRule = "(" + e.getKey() + ")";
			if (fragmentSet.contains(cfgRule)) continue;
			int freq = e.getValue()[0];
			String newLine = cfgRule + "\t" + freq;
			unseenCFRulesPW.println(newLine);			
			allFragmPW.println(newLine);
			added++;			
		}		
		Parameters.reportLine("Total fragments: " + totalFragments);		
		Parameters.reportLine("Added CFGrules: " + added);
		Parameters.reportLineFlush("Total fragments + CFG rules: " + (totalFragments+added));
		unseenCFRulesPW.close();
		allFragmPW.close();
	}
	
		
	private Hashtable<String, int[]> extractCFGfreq() {		
		Hashtable<String, int[]> cfgRulesFreq = new Hashtable<String, int[]>();
		for(TSNodeLabel t : trainingTreebank) {					
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();		
				//if (rule.indexOf('"')==-1 && !rule.startsWith(topSymbol)) continue; //TO SIMULATE NO PARSE
				Utility.increaseStringIntArray(cfgRulesFreq, rule);
			}
		}
		Parameters.reportLine("Total CFG rules: " + cfgRulesFreq.size());
		return cfgRulesFreq;
	}
	
	private void extractCFGfreqPetrovSmooth() {		
		
		ExtractPetrovPCFG petrovPCFG = new ExtractPetrovPCFG(trainingTreebank, petrovSmoothingFactor);
		Hashtable<String, double[]> cfgFreqSmooth = petrovPCFG.getCFGfreq();
		
		finalFragmentsFile = new File(outputPath + "fragmentsAndCfgRules.txt");
		Parameters.reportLineFlush("Printing all fragments to " + finalFragmentsFile);
		PrintWriter allFragmPW = FileUtil.getPrintWriter(finalFragmentsFile);				
		HashSet<String> fragmentSet = new HashSet<String>();
		int totalFragments = 0;
		if (fragmentsFile!=null) {
			Scanner fragmentScan = FileUtil.getScanner(fragmentsFile);			
			while(fragmentScan.hasNextLine()) {
				String line = fragmentScan.nextLine();
				String[] treeFreq = line.split("\t");			
				String fragment = treeFreq[0];
				fragmentSet.add(fragment);
				allFragmPW.println(line);
				totalFragments++;
			}
		}
		
		int added = 0;		
		for(Entry<String, double[]> e : cfgFreqSmooth.entrySet()) {
			String cfgRule = "(" + e.getKey() + ")";
			if (fragmentSet.contains(cfgRule)) continue;
			double freq = e.getValue()[0];
			String newLine = cfgRule + "\t" + freq;			
			allFragmPW.println(newLine);
			added++;			
		}		
		Parameters.reportLine("Total fragments: " + totalFragments);		
		Parameters.reportLine("Added CFGrules: " + added);
		Parameters.reportLineFlush("Total fragments + CFG rules: " + (totalFragments+added));
		allFragmPW.close();		
	}	
	
	
	private void makeMappingFragmentsToCFRules() {		
		Parameters.reportLineFlush("Making mapping Fragments to CFG rules and preparing grammar for BitPar");
		bibtpar_grammarFile = new File(outputPath + "bitpar_grammar.txt");
		File ambiguousFragmentsFile = new File(outputPath + "ambiguousFragmetnsCFG.txt");
		
		int numberCFGTypes = 0;
		
		if (freqAreInt) {
			ConvertFragmentsToCFGRulesInt converter = 
				new ConvertFragmentsToCFGRulesInt(finalFragmentsFile, ambiguousFragmentsFile, 
						bibtpar_grammarFile, internalFakeNodeLabel, fakePrelexPrefix);		
					
			ambiguousCFGmapping = converter.getAmbiguousCFGmapping();
			unambiguousCFGmapping = converter.getunambiguousCFGmapping();
			
			numberCFGTypes = converter.getCFGtypes();
		}
		
		else {
			ConvertFragmentsToCFGRulesDouble converter = 
				new ConvertFragmentsToCFGRulesDouble(finalFragmentsFile, ambiguousFragmentsFile, 
						bibtpar_grammarFile, internalFakeNodeLabel, fakePrelexPrefix);		
					
			ambiguousCFGmapping = converter.getAmbiguousCFGmapping();			
			unambiguousCFGmapping = converter.getunambiguousCFGmapping();
			
			numberCFGTypes = converter.getCFGtypes();
		}
		
		if (sortGrammar) {
			FileUtil.sortFile(bibtpar_grammarFile);
		}
		
		int numberAmbiguousFragments = ambiguousCFGmapping.size();
		int numberUnambiguousFragments = unambiguousCFGmapping.size();		
		float ambiguityFactor = (float) numberAmbiguousFragments / (numberCFGTypes - numberUnambiguousFragments);		
		Parameters.reportLineFlush("Total CFG types: " + numberCFGTypes);
		Parameters.reportLineFlush("Total unambiguous fragments: " + numberUnambiguousFragments);
		Parameters.reportLineFlush("Total ambiguous fragments: " + numberAmbiguousFragments);
		Parameters.reportLineFlush("Ambiguity Factor: " + ambiguityFactor);		
	}
	
	private void makeLexiconBitPar() {
		Parameters.reportLineFlush("Preparing lexicon for BitPar");
				
		bitpar_lexiconFile = new File(outputPath + "bitpar_lexicon.txt");
				
		PrintWriter lexiconPW = FileUtil.getPrintWriter(bitpar_lexiconFile);
		for(String word : trainingLexicon) {
			lexiconPW.println(word + "\t" + fakePrelexPrefix + word + " " + 1);			
		}
		for (String word : testLexiconUnwnown) {
			lexiconPW.println(word + "\t" + fakePrelexPrefix + word + " " + 1);
		}
		lexiconPW.close();
	}
	
	private void smoothLexicon() throws Exception {
		if (!smoothLexicon && testLexiconUnwnown.isEmpty()) {
			// nothing to do
			return;
		}
		Parameters.reportLineFlush("Smoothing lexicon");			
		Hashtable<Label,HashSet<Label>> wordPosMapping = new Hashtable<Label,HashSet<Label>>();
		Hashtable<Label,HashSet<Label>> posWordMapping = new Hashtable<Label,HashSet<Label>>();
				
		for(TSNodeLabel t : trainingTreebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for(TSNodeLabel l : lex) {
				Label pos = l.parent.label;				
				Label word = l.label;
				if (WordFeatures.isNumber(word.toString())) {
					continue;
				}
				addInWordPostable(wordPosMapping, word, pos);
				addInWordPostable(posWordMapping, pos, word);
			}
		}
		
		HashSet<Label> openClassPosSet = new HashSet<Label>();
		int openClassPos = 0;
		int closeClassPos = 0;
		for(Entry<Label,HashSet<Label>> e : posWordMapping.entrySet()) {
			int size = e.getValue().size();
			if (size>openClassThreshold) {
				openClassPosSet.add(e.getKey());
				openClassPos++;
			}					
			else closeClassPos++;
		}		
		
		Parameters.reportLineFlush("Close class postags: " + closeClassPos);
		Parameters.reportLineFlush("Open class postags (" + openClassPos + "): " + openClassPosSet);		
		
		FileWriter pw = new FileWriter(bibtpar_grammarFile, true);
		int added = 0;
		
		if (smoothLexicon) {
			for(Entry<Label,HashSet<Label>> e : wordPosMapping.entrySet()) {
				Label word = e.getKey();
				HashSet<Label> existingPosForWord = e.getValue();
				boolean isOpenClassWord = true;
				for(Label pos : existingPosForWord) {
					if (!openClassPosSet.contains(pos)) {
						isOpenClassWord = false;
						break;
					}
				}
				if (!isOpenClassWord) 
					continue;			
				
				for(Label pos : openClassPosSet) {
					if (existingPosForWord.contains(pos)) {
						continue;
					}
					String treeCFGrule = pos + " " + fakePrelexPrefix + word;
					String line = smoothLexFactor + "\t" + treeCFGrule + "\n";
					TSNodeLabel fragment = new TSNodeLabel(pos,false);				
					fragment.assignUniqueDaughter(new TSNodeLabel(word,true));
					unambiguousCFGmapping.put(treeCFGrule, fragment);
					pw.append(line);
					added++;
				}
			}
		}
		for (String l : testLexiconUnwnown) {
			Label word = Label.getLabel(l);			
			for(Label pos : openClassPosSet) {
				String treeCFGrule = pos + " " + fakePrelexPrefix + word;
				String line = smoothLexFactor + "\t" + treeCFGrule + "\n";
				TSNodeLabel fragment = new TSNodeLabel(pos,false);				
				fragment.assignUniqueDaughter(new TSNodeLabel(word,true));
				unambiguousCFGmapping.put(treeCFGrule, fragment);
				pw.append(line);
				added++;
			}
		}
		
		pw.close();
		Parameters.reportLineFlush("Added unseen word pos: " + added);		
	}
	
	private static void addInWordPostable(Hashtable<Label,HashSet<Label>> wordPosMapping,
			Label word, Label pos) {
		
		HashSet<Label> posSet = wordPosMapping.get(word);
		if (posSet==null) {
			posSet = new HashSet<Label>();
			wordPosMapping.put(word,posSet);
		}
		posSet.add(pos);
	}
	
	private static TreeSet<String> getLexiconFromTreebank(ArrayList<TSNodeLabel> treebank) {
		TreeSet<String> result = new TreeSet<String>();
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for(TSNodeLabel l : lex) {
				String word = l.label();
				result.add(word);
			}
		}
		return result;
	}
	
	private static TreeSet<String> getLexiconFromFragments(File fragmentsFile)  {
		TreeSet<String> result = new TreeSet<String>();
		Scanner scan = FileUtil.getScanner(fragmentsFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			try {
				TSNodeLabel fragment = new TSNodeLabel(line, false);
				ArrayList<TSNodeLabel> lex = fragment.collectLexicalItems();
				for(TSNodeLabel l : lex) {
					String word = l.label();
					result.add(word);
				}
			} catch (Exception e) {				
				e.printStackTrace();
				return null;
			}
		}
		return result;
	}	
		
	protected abstract void parseWithBitPar() throws Exception;	
	
	public static void printBitParFlatSentence(ArrayList<String[]> testSentencesWords, File outputFile) {
		PrintWriter pwBitParInput = FileUtil.getPrintWriter(outputFile);
		for(String[] sentencePosWord : testSentencesWords) {															
			String sentenceBitParFormat = Utility.joinStringArrayToString(sentencePosWord, "\n")+"\n";
			pwBitParInput.println(sentenceBitParFormat);
		}
		pwBitParInput.close();
	}
	
	public static void printBitParFlatSentence(String[] testSentenceWords, File outputFile) {
		PrintWriter pwBitParInput = FileUtil.getPrintWriter(outputFile);
		String sentenceBitParFormat = Utility.joinStringArrayToString(testSentenceWords, "\n")+"\n";
		pwBitParInput.println(sentenceBitParFormat);
		pwBitParInput.close();
	}
	
	protected synchronized void doneWithOneSentence() {
		progress.next();		
	}	
	
	final static String viterbProbPrefix = "vitprob=";
	final static String noParseMessage = "No parse for: ";
	final static int viterbProbPrefixLength = viterbProbPrefix.length();
	
	
		
	public TSNodeLabel postProcessParseTree(TSNodeLabel tree) {
		tree = replaceRulesWithFragments(tree);
		if (markoBinarize) {
			tree = treeMarkovBinarizer.undoMarkovBinarization(tree);
		}
		if (usingPetrov) {
			tree = CleanPetrov.cleanPetrovTree(tree);
		}
		return tree;
	}
	
	private synchronized TSNodeLabel replaceRulesWithFragments(TSNodeLabel tree) {
		String firstDaughetLabel = tree.firstDaughter().label(); 
		boolean ambiguousFragment = firstDaughetLabel.startsWith(internalFakeNodeLabel);
		TSNodeLabel fragment = null;
		
		if (ambiguousFragment) {
			int index = 0;
			try {
				String digits = firstDaughetLabel.substring(internalFakeNodeLabelLength);
				index = Integer.parseInt(digits);
			}
			catch(StringIndexOutOfBoundsException e) {
				Parameters.reportLineFlush("Error302: " + firstDaughetLabel + " " + tree);				
				return null;
			}
			fragment = ambiguousCFGmapping.get(index);
			if (fragment==null) {
				Parameters.reportError("Couldn't find the fragment uniquely associated with unique index:" + index);
				return null;
			}
			tree.daughters = tree.firstDaughter().daughters;
		}
		else {
			fragment = unambiguousCFGmapping.get(tree.cfgRule());			
		}

		TSNodeLabel result = fragment.clone();
		ArrayList<TSNodeLabel> terminals = result.collectTerminalItems();
		Iterator<TSNodeLabel> termIter = terminals.iterator();
		for(TSNodeLabel d : tree.daughters) {
			TSNodeLabel term = termIter.next();
			if (term.isLexical) continue;			
			TSNodeLabel subFragment = replaceRulesWithFragments(d);			
			term.daughters = subFragment.daughters;
			for(TSNodeLabel d1 : subFragment.daughters) {
				d1.parent = subFragment;
			}			
		}
		return result;
	}	
	
	public static boolean runEvalC = true;	
	
	private void parseEval() {
		Parameters.reportLineFlush("Running EvalB and EvalC");
		DecimalFormat df = new DecimalFormat("0.00");
		int length  = parsedOutputFiles.length;
		for(int i=0; i<length; i++) {
			File f = parsedOutputFiles[i];
			if (charBased) {
				convertFromCharBasedFile(f);
			}
			String id = parsedOutputFilesIdentifiers[i];
			File evalBfile = FileUtil.changeExtension(f, "evalB");
			File evalCfile = FileUtil.changeExtension(f, "evalC");
			new EvalB(testFileClean, f, evalBfile);					
			//File evalCfileLog = new File(outputPath + "BITPAR_MOST_PROB_PARSES.evalC.log");
			
			if (runEvalC) {
				EvalC eval = new EvalC(testFileClean, f, evalCfile, null, true);	
				float[] results = eval.makeEval(); //recall precision fscore
				Parameters.reportLineFlush(Utility.fse(15, id) + 
						":\tRecall, Precision, Fscore (<=" + EvalC.CUTOFF_LENGTH + "):  [" 
						+ df.format(results[0]) + ", " + df.format(results[1]) + ", " + df.format(results[2])
						+ "]");
			}
		}
	}
	
	private void convertFromCharBasedFile(File inputFile) {
		File tmpFile = new File(inputFile + "~");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(tmpFile);
		while(scan.hasNextLine()) {
			try {
				TSNodeLabel tree = new TSNodeLabel(scan.nextLine());
				tree.makeTreeFromCharBased();
				pw.println(tree);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
		}
		pw.close();
		tmpFile.renameTo(inputFile);
	}
	
	protected static ArrayList<String[]> getSentencesWords(ArrayList<TSNodeLabel> treebank) {
		ArrayList<String[]> result = new ArrayList<String[]>();
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();			
			String[] sentenceWords = new String[lex.size()];
			int index = 0;
			for(TSNodeLabel l : lex) {
				String word = l.label();
				sentenceWords[index] = word;
				index++;
			}
			result.add(sentenceWords);
		}
		return result;
	}
	
	abstract public String getClassName();
	
	
	private void printStartingParameters() {					
		Parameters.reportLine("\nTSGparsingBitPar (" + getClassName() + ")\n");		
		Parameters.reportLine("Threads: " + threads);
		
		Parameters.reportLine("Encoding: " + System.getProperty("file.encoding"));		
				
		Parameters.reportLine("Extract Unseen CFGrules: " + extractUnseenCFGrules);
		Parameters.reportLine("Fragment frequency threashold (>=): " + fragmentFreqThreashold);
		
		boolean preprocessUnknownWords = UkWordMapping.ukThreashold > 0;
		Parameters.reportLine("Preprocess Unknown Words: " + preprocessUnknownWords);
		if (preprocessUnknownWords) { 
			Parameters.reportLine("Threashold Uk: " + UkWordMapping.ukThreashold);
			Parameters.reportLine("UkModel: " + ukModel.getName());
		}
		
		//Parameters.reportLine("getLexiconFromTreebank: " + getLexiconFromTreebank);		
		
		Parameters.reportLine("Remove Tmp Files: " + removeTmpFiles);
		Parameters.reportLine("Marko Binarize: " + markoBinarize);
		if (markoBinarize) {			
			Parameters.reportLine("Binarization Model: " + treeMarkovBinarizer.getDescription());
			Parameters.reportLine("Markov Horizontal: " + TreeMarkoBinarization.markH);
			Parameters.reportLine("Markov Vertical: " + TreeMarkoBinarization.markV);
		}
		
		Parameters.reportLine("Frequencies are integers: " + freqAreInt);
		//Parameters.reportLine("Run only MPD: " + runOnlyMPD);
		
		Parameters.reportLine("nBest: " + nBest);
		Parameters.reportLine("Sentence Length Limit Test: " + sentenceLengthLimitTest);
		Parameters.reportLine("Training Treebank File: " + trainingFile);
		Parameters.reportLine("Test Treebank File: " + testFile);
		Parameters.reportLine("Test Treebank Cleaned File: " + testFileClean);
				
		Parameters.reportLine("Using Petrov: " + usingPetrov);
		if (usingPetrov) {
			Parameters.reportLine("Petrov smoothing factor: " + petrovSmoothingFactor);			
		}
		
		Parameters.reportLine("Char Based: " + charBased);
		
		Parameters.reportLine("Smoothing lexicon: " + smoothLexicon);
		if (smoothLexicon) {
			Parameters.reportLine("Smoothing lexicon factor: " + smoothLexFactor);			
			//Parameters.reportLine("Open class pos: " + Arrays.toString(openClassPos));
			Parameters.reportLine("Open class threshold: " + openClassThreshold);			
		}
		
		if (pruneThreshold!=-1) {
			Parameters.reportLine("Pruning threshold: " + pruneThreshold);
		}
		
		Parameters.reportLine("Selecting Shortest Derivation: " + shortestDerivation);
		
		Parameters.reportLine("Fragments File: " + fragmentsFile);
		Parameters.reportLineFlush("Output Directory: " + outputPath);
	}
	
	protected synchronized TSNodeLabel dealWithNOParsedSentences(String[] originalTestSentenceWords) {
		if (charBased) {
			originalTestSentenceWords = compactWords(originalTestSentenceWords);
		}
		TSNodeLabel result = defaultParseMaker.defaultParse(originalTestSentenceWords, topSymbol);
		if (charBased) {
			result.makeTreeCharBased();
		}
		Parameters.reportLineFlush("No parse for sentence: " + 
				Arrays.toString(originalTestSentenceWords) + "\n\t" +
				"Default parse: " + result);
		noParsedSentences++;
		return result;		
	}

	public static String[] compactWords(String[] chars) {
		ArrayList<String> result = new ArrayList<String>(); 
		String currentWord = "";
		for(String c : chars) {
			if (c.equals("<w>")) {
				currentWord = "";
				continue;
			}
			if (c.equals("</w>")) {
				result.add(currentWord);
				continue;
			}
			currentWord += c;
		}
		return result.toArray(new String[result.size()]);		
	}

	public static void main1(String[] args) throws Exception {
		String outputPath = "/Users/fedja/Work/Code/TreeGrammars/tmp/WsjUk4/";
		//File fragmentsFile1 = new File(outputPath + "fragments_ALL_exactFreq.txt");
		//File fragmentsFile2 = new File(outputPath + "fragments_ALL_approxFreq.txt");
		File fragmentsFile1 = new File(outputPath + "Parsing_Tue_Nov_16_00_18_57/fragmentsAndCfgRules.txt");
		File fragmentsFile2 = new File(outputPath + "Parsing_Tue_Nov_16_00_20_50/fragmentsAndCfgRules.txt");
		Scanner scan1 = new Scanner(fragmentsFile1);
		Scanner scan2 = new Scanner(fragmentsFile2);
		while(scan1.hasNextLine() && scan2.hasNextLine()) {
			String line1 = scan1.nextLine().split("\t")[0];
			String line2 = scan2.nextLine().split("\t")[0];
			if (!line1.equals(line2)) {
				System.out.println(line1);
				System.out.println(line2);
				return;
			}
		}

	}
	
	public static void mainPersian(String[] args) throws Exception {
		
		String parentPath = "/Users/fedja/Work/Edinburgh/PersianParsing/"; 
		String path = parentPath + "Persian_LeftBin_H1V2_UK4_UL4/";
		String cmd = "-threads:1 -markoBinarize:true -markovH:1 -markovV:2 -ukThreshold:1 -ukModel:Petrov_Level_4 " +
				"-smoothLexicon:true -smoothLexiconFactor:0.01 -openClassThreshold:50 " +
				"-runOnlyMPD:true ../PersianTreeBank.mrg ../testPersian.mrg ./fragments_approxFreq.txt ./parsing/";
		cmd = cmd.replaceAll("\\../", parentPath);
		cmd = cmd.replaceAll("\\./", path);
		runEvalC = false;
		main(cmd.split("\\s+"));
		
	}
	
	public static void main(String[] args) throws Exception {		
				
		String nBestOption = "-nBest:";
		String threadsOption = "-threads:";		
		String fragmentFreqThreasholdOption = "-fragmentFreqThreashold:";
		String extractUnseenCFGrulesOption = "-extractUnseenCFGrules:";		
		String unknownThreasholdOption = "-ukThreshold:";
		String ukModelOption = "-ukModel:";
		String removeTmpFilesOption = "-removeTmpFiles:";
		String directParsingOption = "-directParsing:";
		String sentenceLengthLimitTestOption = "-sentenceLengthLimitTest:";
		String runSeparateBitParPerSentenceOption = "-runSeparateBitParPerSentence:";
		String restrictTestToFirstOption = "-restrictTestToFirst:";
		String markoBinarizeOption = "-markoBinarize:";		
		String markovH_option = "-markovH:";
		String markovV_option = "-markovV:";
		String runOnlyMPD_option = "-runOnlyMPD:";
		String freqAreIntOption = "-freqAreInt:";
		String sortGrammarFileOption = "-sortGrammarFile:";
		String usingPetrovOption = "-usingPetrov:";
		String petrovSmoothingFactorOption = "-petrovSmoothingFactor:";
		String smoothLexiconOption = "-smoothLexicon:";
		String openClassThresholdOption = "-openClassThreshold:";		
		String smoothLexiconFactorOption = "-smoothLexiconFactor:";
		String bitparPathOption = "-bitparPath:";
		String useMjCkyOption = "-useMJ:";
		String charBasedOption = "-charBased:";		
		String pruneThresholdOption = "-pruneThreshold:";	
		String shortestDerivationsOption = "-shortestDerivation:";

		
		nBest = 1000;
		threads = 1;
		sentenceLengthLimitTest = Integer.MAX_VALUE;
		restrictTestToFirst = -1;
		fragmentFreqThreashold = -1;
		extractUnseenCFGrules = true;
		removeTmpFiles = true;		
		parse = true;			
		
		boolean directParsing = true;
		boolean runSeparateBitParPerSentence = false;
		boolean useMjCky = false;
		
		UkWordMapping.ukThreashold = -1;
		ukModel = new UkWordMappingPetrov(); //was new UkWordMappingStd();		
		//ukModel = new UkWordMappingStd();
		
		TreeMarkoBinarization.markH = 1;
		TreeMarkoBinarization.markV = 2;		
		//treeMarkovBinarizer = new MarkoBinarizationBerkeley(Binarization.RIGHT); // LEFT, RIGHT, PARENT, HEAD		
		treeMarkovBinarizer = new TreeMarkoBinarizationLeft_Petrov();		
		//treeMarkovBinarizer = new TreeMarkoBinarizationLeft_Klein();
		//treeMarkovBinarizer = new MarkoBinarizationHead();
		//treeMarkovBinarizer = new TreeMarkoBinarizationLeft_LC();
		//treeMarkovBinarizer = new TreeMarkoBinarizationLeft_New();
		//treeMarkovBinarizer = new TreeMarkoBinarizationLeft_RC();		
		//treeMarkovBinarizer = new TreeMarkoBinarizationRight_LC();
		//treeMarkovBinarizer = new TreeMarkoBinarizationRight_New();
		//treeMarkovBinarizer = new TreeMarkoBinarizationRightStop_LC();
		
		MetricOptimizerArray.setLambdaValues(0, 2, 0.05);		
		
		String usage = "USAGE: java [-Xmx10G] tsg.parsingExp.TSGparsingBitPar " +
		"[-threads:2] [-nBest:1000] [-ukModel:petrov] [-ukThreshold:-1] [-fragmentFreqThreashold:-1] " +
		"[-extractUnseenCFGrules:true] [-removeTmpFiles:true] [-directParsing:true] [-runSeparateBitParPerSentence:false] " + 
		"[-sentenceLengthLimitTest:1000] [-restrictTestToFirst:-1] [-markoBinarize:false] [-markovH:1] [-markovV:2] " +
		"[-runOnlyMPD:false] [-freqAreInt:true] [-usingPetrov:false] [-petrovSmoothingFactor:0] [-smoothLexicon:false]" +
		"[-pruneThresholdOption:-1] [-shortestDerivation:false] [-bitparPath:] " +
		"trainingTreebankFile testTreebankFile fragmentFile outputDir";
		
		/*
		String usage = "USAGE: java [-Xmx1G] DoubleDOP.jar " +
		"[-threads:1] [-nBest:1000] [-markoBinarize:false] [-ukThreshold:-1] [-smoothLexicon:false] " +
		"[-bitparPath:./BitPar/src/bitpar] trainingTreebankFile testTreebankFile fragmentFile outputDir";
		bitparApp = "./BitPar/src/bitpar";
		*/
				
		if (args.length==0 || args.length>30) {			
			System.err.println("Incorrect number of arguments: " + args.length);
			System.err.println(usage);
			System.exit(-1);
		}				

		
		int i=0;		
		for(; i<args.length-4; i++) {
			String option = args[i];
			if (option.startsWith(nBestOption))			
				nBest = ArgumentReader.readIntOption(option);
			else if (option.startsWith(threadsOption))	
				threads = ArgumentReader.readIntOption(option);
			else if (option.startsWith(fragmentFreqThreasholdOption))
				fragmentFreqThreashold = ArgumentReader.readIntOption(option);
			else if (option.startsWith(extractUnseenCFGrulesOption))
				extractUnseenCFGrules = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(ukModelOption)) {
				ukModel = UkWordMapping.getModel(ArgumentReader.readStringOption(option));
				if (ukModel==null) {
					System.err.println("Invalid ukModel");
					// only Basic, English_Fede, French_Arun, English_Petrov, Petrov_Level_x, XXX_Petrov
					return;
				}
			}
			else if (option.startsWith(unknownThreasholdOption))
				UkWordMapping.ukThreashold = ArgumentReader.readIntOption(option); 
			else if (option.startsWith(removeTmpFilesOption))
				removeTmpFiles = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(directParsingOption))
				directParsing = ArgumentReader.readBooleanOption(option);			
			else if (option.startsWith(runSeparateBitParPerSentenceOption))
				runSeparateBitParPerSentence = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(sentenceLengthLimitTestOption))
				sentenceLengthLimitTest = ArgumentReader.readIntOption(option);
			else if (option.startsWith(markoBinarizeOption))
				markoBinarize = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(markovH_option))
				TreeMarkoBinarization.markH = ArgumentReader.readIntOption(option);
			else if (option.startsWith(markovV_option))
				TreeMarkoBinarization.markV = ArgumentReader.readIntOption(option);
			else if (option.startsWith(restrictTestToFirstOption))
				restrictTestToFirst = ArgumentReader.readIntOption(option);
			else if (option.startsWith(freqAreIntOption))
				freqAreInt = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(runOnlyMPD_option)) {
				boolean mpd = MetricOptimizerArray.runOnlyMPD = ArgumentReader.readBooleanOption(option);
				if (mpd) nBest=1;
			}
			else if (option.startsWith(sortGrammarFileOption))
				sortGrammar = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(usingPetrovOption))
				usingPetrov = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(petrovSmoothingFactorOption)) 
				petrovSmoothingFactor = ArgumentReader.readDoubleOption(option);
			else if (option.startsWith(smoothLexiconOption))
				smoothLexicon = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(smoothLexiconFactorOption))
				smoothLexFactor = ArgumentReader.readDoubleOption(option);
			else if (option.startsWith(bitparPathOption))
				bitparApp = ArgumentReader.readStringOption(option);
			else if (option.startsWith(useMjCkyOption)) {
				useMjCky = ArgumentReader.readBooleanOption(option);
				if (useMjCky) MetricOptimizerArray.runOnlyMPD = true;
			}
			else if (option.startsWith(charBasedOption))
				charBased = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(openClassThresholdOption)) {
				openClassThreshold = ArgumentReader.readIntOption(option);
			}
			else if (option.startsWith(pruneThresholdOption)) {
				pruneThreshold = ArgumentReader.readDoubleOption(option);
			}
			else if (option.startsWith(shortestDerivationsOption)) {
				MetricOptimizerArray.runOnlyShortestDerivation = shortestDerivation = 
					ArgumentReader.readBooleanOption(option);				
			}
			else {
				System.err.println("Not a valid option: " + option);
				System.err.println(usage);
				System.exit(-1);
			}
		}
				
		File trainingFile = ArgumentReader.readFileOptionNoSeparation(args[i++]);
		File testFile = ArgumentReader.readFileOptionNoSeparation(args[i++]);
		File fragmentsFile = ArgumentReader.readFileOptionNoSeparation(args[i++]);
		File outputDir = ArgumentReader.readFileOptionNoSeparation(args[i++]);			
		
		
		/*
		//String outputPath = "Negra/";
		//File trainingFile = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-02-21.mrg"); 
		//File testFile = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-24.mrg");				
		File trainingFile = new File(CTB.CleanTopDirCharBased + "train_charBased.mrg"); 
		File testFile = new File(CTB.CleanTopDirCharBased + "develop_charBased.mrg");		
		File fragmentsFile = null; //new File(outputPath + "fragments_approxFreq.txt");
		File outputDir = new File("CTB/");		
		nBest = 1;
		threads = 2;			
		sentenceLengthLimitTest = 40;
		restrictTestToFirst = 5;
		fragmentFreqThreashold = -1;		
		removeTmpFiles = false;
		directParsing = true;
		runSeparateBitParPerSentence = false;
		UkWordMapping.ukThreashold = -1;
		markoBinarize = true;
		TreeMarkoBinarization.markH = 1;
		TreeMarkoBinarization.markV = 2;
		ukModel = new UkWordMappingPetrov(); //was new UkWordMappingStd();
		UkWordMappingPetrov.unknownLevel = 4;
		MetricOptimizerArray.runOnlyMPD = true;
		charBased = true;
		parse = true;		
		useMjCky = true;
		*/
		
		
		TSGparsingBitPar T = null;	
		
		if (useMjCky) {
			if (threads==1) {
				T = new TSGparsingMJDirectNoThreads(trainingFile, testFile, fragmentsFile, outputDir);
			}
			else {
				T = new TSGparsingMJ(trainingFile, testFile, fragmentsFile, outputDir);
			}
		}
		else {		
			if (threads==1) {			
				T = runSeparateBitParPerSentence ?
					new TSGparsingBitParNoThreadsSeparate(trainingFile, testFile, fragmentsFile, outputDir) :
					new TSGparsingBitParNoThreads(trainingFile, testFile, fragmentsFile, outputDir);
			}
			else {
				if (directParsing) {
					T = runSeparateBitParPerSentence ?
						new TSGparsingBitParDirectSeparate(trainingFile, testFile, fragmentsFile, outputDir) :
						new TSGparsingBitParDirect(trainingFile, testFile, fragmentsFile, outputDir);					
				}
				else {
					T = runSeparateBitParPerSentence ?
						new TSGparsingBitParFileSeparate(trainingFile, testFile, fragmentsFile, outputDir) :
						new TSGparsingBitParFile(trainingFile, testFile, fragmentsFile, outputDir);
					
				}
			}			
		}
		
		T.run();		
		
		//TSNodeLabel tree = new TSNodeLabel("(FRAG (FRAG (ADJP (VBN (^1stY_1capY_dashN_slshN_alfY_digN_sfx:NONE_afx:NONE 1stY_1capY_dashN_slshN_alfY_digN_sfx:NONE_afx:NONE)))) (, (^, ,)) (FRAG (RB (^1stN_1capN_dashN_slshN_alfY_digN_sfx:ous 1stN_1capN_dashN_slshN_alfY_digN_sfx:ous)) (NP (FRAG@72967 (NNP (^Columbia Columbia)))) (. (^! !))))");
		//TSNodeLabel newTree = T.replaceRulesWithFragments(tree);
		//System.out.println(newTree);		
		
	}
	
	
}
