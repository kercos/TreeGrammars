package tsg.parseEval;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import tsg.Constituency;
import tsg.ConstituencyWords;
import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class EvalC {
	
	private static boolean LABELED = true;
	public static boolean REMOVE_SEMANTIC_TAGS = true;
	public static boolean DELETE_BACK_SLASH = true;
	public static int CUTOFF_LENGTH = 40;
	public static int CONSTITUENTS_UNIT = 0;  // 0:WORDS, 1:CHARACTERS, 2:YIELD
	public static String ENCODING = "UTF-8";
	public static int SENTENCE_LENGTH_LIMIT = Integer.MAX_VALUE;	

	// EXCLUDE_CAT contains the categories that are not counted when extracting constituencies
	public static String[] EXCLUDE_CAT = new String[]{"TOP", "S1"}; //new String[]{"TOP"};

	// EXCLUDE_POS contains the categories that are not counted when extracting constituencies.
	// Note that the check is only done in each POStag of the gold parsetree
	// The assumption is that the excluded tags are either equals to the corresponding ones
	// in the test tree, or the difference is not relevant for the scoring purpose...
	public static String[] EXCLUDE_POS = new String[]{}; //new String[]{"!", "''", ",", ".", ":", "?", "``"};
	
	// DELETE_LABEL are the POStags to prune before processing each test and gold parsetree
	// Delete labels list of labels to be ignored.
	// If it is a pre-terminal label, delete the word along with the brackets.
	// If it is a non-terminal label, just delete the brackets (don't delete childrens)
	public static String[] DELETE_LABELS = new String[]{"!", "''", ",", "-NONE-", ".", ":", "?", "``"}; 
	
	// DELETE_LABEL_FOR_LENGTH
	// Delete labels for length calculation list of labels to be ignored for length calculation purpose
	public static String[] DELETE_LABEL_FOR_LENGTH = new String[]{"-NONE-"}; 
	
	// EQ_LABEL are labels which are considered equals to one another
	public static String[][] EQUAL_LABELS = new String[][] { {"PRT", "ADVP"} };
	

	public static NumberFormat formatter = new DecimalFormat("0.00");	
	PrintWriter out, log;	
	
	// variables for all sentences
	File goldFile, testFile, outputFile, logFile;
	ArrayList<TSNodeLabel> goldCorpus, testCorpus;
	int sentences, totalMatchBrackets, totalGoldBrackets, totalTestBrackets, totalCrossBracket, 
		totalWords, totalCorrectTags, totalExactMatch, noCrossing, twoOrLessCrossing;
	float totalRecallPercentage, totalPrecisionPercentage, taggingAccuracyPercentage;
	boolean areCompatible;
	int[] sentencesLength;
	
	//category variables
	Hashtable<String, Integer> crossingBracketsCatTable;
	Hashtable<String, int[]> categoryStatistics;
	Hashtable<String, Integer> wrongCatStatistics;
	int totalCategoryGold;
	
	// variables for sentences with length less or equal than maxSentenceLength (MSL)
	int sentencesMSL, totalMatchBracketsMSL, totalGoldBracketsMSL, totalTestBracketsMSL, totalCrossBracketMSL, 
		totalWordsMSL, totalCorrectTagsMSL, totalExactMatchMSL, noCrossingMSL, twoOrLessCrossingMSL;
	float totalRecallPercentageMSL, totalPrecisionPercentageMSL, taggingAccuracyPercentageMSL;		
	
	HashSet<Integer> skipSentences;
	
	public static void setLabeled(boolean l) {
		LABELED = l;
	}
	
	public static boolean getLabeled() {
		return LABELED;
	}
	
	public EvalC(File goldFile, File testFile, File outputFile, File logFile, boolean preprocess) {
		this.goldFile = goldFile;
		this.testFile = testFile;
		this.outputFile = outputFile;
		this.logFile = logFile;	
		this.getTreebanks();
		getSentencesLength();
		if (preprocess) {
			this.preprocessCorporaAndCheckCompatibility();
		}
	}
	
	public EvalC(File goldFile, File testFile, File outputFile, File logFile) {
		this(goldFile, testFile, outputFile, logFile, true);
	}
	
	public EvalC(ArrayList<TSNodeLabel> goldCorpus, ArrayList<TSNodeLabel> testCorpus, 
			File outputFile, File logFile) {
		this.goldCorpus = goldCorpus;
		this.testCorpus = testCorpus;
		this.outputFile = outputFile;
		this.logFile = logFile;	
		getSentencesLength();
		this.preprocessCorporaAndCheckCompatibility();
	}
	
	public EvalC(File goldFile, File testFile, File outputFile) {
		this(goldFile, testFile, outputFile, null);
	}
	
	public void setGoldFile(File goldFile) {
		this.goldFile = goldFile;
		try {
			goldCorpus = TSNodeLabel.getTreebank(goldFile, ENCODING, Integer.MAX_VALUE);
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
	public void setTestFile(File testFile) {
		this.testFile = testFile;
		try {
			testCorpus = TSNodeLabel.getTreebank(testFile, ENCODING, Integer.MAX_VALUE);
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}
	
	public int size() {
		return goldCorpus.size();
	}
	
	public ArrayList<TSNodeLabel> getGoldCorpus() {
		return goldCorpus;
	}
	
	public ArrayList<TSNodeLabel> getTestCorpus() {
		return testCorpus;
	}
	
	public boolean areComparable() {
		return this.areCompatible;
	}
		
	public static float[] staticEvalC(File goldFile, File testFile, File outputFile, File logFile, boolean preprocess) {
		EvalC EC = new EvalC(goldFile, testFile, outputFile, logFile, preprocess);
		if (!EC.areCompatible) return null;		
		return EC.makeEval();
	}
	
	public static float[] staticEvalC(File goldFile, File testFile, File outputFile, File logFile) {
		EvalC EC = new EvalC(goldFile, testFile, outputFile, logFile);
		if (!EC.areCompatible) return null;		
		return EC.makeEval();
	}
	
	public static float[] staticEvalC(File goldFile, File testFile, File outputFile) {
		return staticEvalC(goldFile, testFile, outputFile, null);
	}
		
	public static void readParametersFromFile(File inputFile) {
		Scanner scan = FileUtil.getScanner(inputFile, ENCODING);
		ArrayList<String> exclude_cat_array = new ArrayList<String>();
		ArrayList<String> exclude_pos_array = new ArrayList<String>();
		ArrayList<String> delete_pos_array = new ArrayList<String>();
		ArrayList<String[]> equal_labels_array = new ArrayList<String[]>();
				
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.length()==0 || line.charAt(0)=='#') continue;
			int spaceIndex = line.indexOf(' ');
			String key = line.substring(0, spaceIndex);
			String value = line.substring(spaceIndex+1);
			if (key.equals("LABELED")) {
				LABELED = Boolean.parseBoolean(value);
				continue;
			}
			if (key.equals("CONSTITUENTS_UNIT")) {
				CONSTITUENTS_UNIT = Integer.parseInt(value);
				continue;
			}
			if (key.equals("REMOVE_SEMANTIC_TAGS")) {
				REMOVE_SEMANTIC_TAGS = Boolean.parseBoolean(value);
				continue;
			}
			if (key.equals("CUTOFF_LENGTH")) {
				CUTOFF_LENGTH = Integer.parseInt(value);
				continue;
			}
			if (key.equals("ENCODING")) {
				ENCODING = value;
				continue;
			}
			if (key.equals("EXCLUDE_CAT")) {
				exclude_cat_array.add(value);
				continue;
			}
			if (key.equals("EXCLUDE_POS")) {
				exclude_pos_array.add(value);
				continue;
			}
			if (key.equals("DELETE_POS")) {
				delete_pos_array.add(value);
				continue;
			}
			if (key.equals("EQUAL_LABELS")) {
				equal_labels_array.add(value.split("\\s"));
				continue;
			}
		}
		EXCLUDE_CAT = exclude_cat_array.toArray(new String[]{});
		EXCLUDE_POS = exclude_pos_array.toArray(new String[]{});
		DELETE_LABELS = delete_pos_array.toArray(new String[]{});
		EQUAL_LABELS = equal_labels_array.toArray(new String[][]{});		
		//printParameters(System.out);
	}
	
	private static String formatNumber(double d) {
		if (Double.isNaN(d)) return Double.toString(Double.NaN);
		return formatter.format(d);
	}
	
	public static void printParameters(PrintStream ps) {
		ps.println("LABELED" + "\t" + LABELED);
		ps.println("CONSTITUENTS_UNIT" + "\t" + CONSTITUENTS_UNIT);
		ps.println("REMOVE_SEMANTIC_TAGS" + "\t" + REMOVE_SEMANTIC_TAGS);
		ps.println("CUTOFF_LENGTH" + "\t" + CUTOFF_LENGTH);
		ps.println("ENCODING" + "\t" + ENCODING);
		ps.println("EXCLUDE_CAT" + "\t" + Arrays.toString(EXCLUDE_CAT));
		ps.println("EXCLUDE_POS" + "\t" + Arrays.toString(EXCLUDE_POS));
		ps.println("DELETE_POS" + "\t" + Arrays.toString(DELETE_LABELS));
		for(String[] eqPair : EQUAL_LABELS) {
			ps.println("EQUAL_LABELS" + "\t" + Arrays.toString(eqPair));
		}
	}
	
	
	public void getTreebanks() {
		try {
			if (goldFile!=null) goldCorpus = TSNodeLabel.getTreebank(goldFile, ENCODING, SENTENCE_LENGTH_LIMIT);
			if (testFile!=null) testCorpus = TSNodeLabel.getTreebank(testFile, ENCODING, SENTENCE_LENGTH_LIMIT);
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
	public void getSentencesLength() {
		sentencesLength = new int[testCorpus.size()];
		int i=0;
		for(TSNodeLabel goldTree : goldCorpus) {
			sentencesLength[i] = goldTree.countLexicalNodesExcludingPosLabels(DELETE_LABEL_FOR_LENGTH);
			i++;
		}
	}
	
	public boolean preprocessCorporaAndCheckCompatibility() {		
		skipSentences = new HashSet<Integer>();
		Arrays.sort(EXCLUDE_CAT);
		Arrays.sort(EXCLUDE_POS);
		Arrays.sort(DELETE_LABELS);						
		if (testCorpus.size()!=goldCorpus.size()) {
			String report = "Sentences in goldFile and testFile don't match in number (" + goldCorpus.size() +
							"|" + testCorpus.size() + ")";
			System.err.println(goldFile.getName() + " " + testFile.getName());
			System.err.println(report);
			writeLog(report);
			return areCompatible = false;
		}
		
		if (CONSTITUENTS_UNIT!=2) {
			Iterator<TSNodeLabel> testIter = testCorpus.iterator();
			Iterator<TSNodeLabel> goldIter = goldCorpus.iterator();
			
			for(int i = 0; i<testCorpus.size(); i++) {
				TSNodeLabel testTree = testIter.next();			
				TSNodeLabel goldTree = goldIter.next();			
				preprocessStructures(testTree, goldTree);
				List<TSNodeLabel> testLexicon = testTree.collectLexicalItems();
				List<TSNodeLabel> goldLexicon = goldTree.collectLexicalItems();
				
				if (testLexicon.size() != goldLexicon.size()) {
					String report = "" + (i+1) + " : " + "Length gold|test unmatch (" + goldLexicon.size() + 
									"|" + testLexicon.size() + ")";
					System.err.println(report);
					writeLog(report);
					this.skipSentences.add(new Integer(i));
					continue;
				}					
				for(int j=0; j<testLexicon.size(); j++) {
					String testWord = testLexicon.get(j).label();
					String goldWord = goldLexicon.get(j).label();
					if (!testWord.equals(goldWord)) {
						String report = "" + (i+1) + " : " + "Words gold|test unmatch (" + goldWord + 
										"|" + testWord + ")";
						System.err.println(report);
						writeLog(report);
						this.skipSentences.add(new Integer(i));
						break;
					}				
				}
			}
		}
		
		return areCompatible = true;
	}
	
	private static void preprocessStructures(TSNodeLabel testTree, TSNodeLabel goldTree) {
		if (REMOVE_SEMANTIC_TAGS) {
			testTree.removeSemanticTags();
			goldTree.removeSemanticTags();
		}		
		if (DELETE_BACK_SLASH) {
			testTree.removeBackSlashInLexicon();
			goldTree.removeBackSlashInLexicon();
		}
		if (EQUAL_LABELS!=null && EQUAL_LABELS.length>0) {
			for(String[] equalPairs : EQUAL_LABELS) {
				testTree.replaceLabels(equalPairs[0], equalPairs[1]);
				goldTree.replaceLabels(equalPairs[0], equalPairs[1]);
			}
		}
		if (DELETE_LABELS!=null && DELETE_LABELS.length>0) {
			testTree.pruneSubTrees(DELETE_LABELS);
			goldTree.pruneSubTrees(DELETE_LABELS);
		}
	}
	
	
	public float[] makeEval() {		
		initEval();
		for(int i=0; i<goldCorpus.size(); i++) {
			TSNodeLabel TNgold = goldCorpus.get(i);
			TSNodeLabel TNtest = testCorpus.get(i);					
			updateStatistics(i+1, TNgold, TNtest);
		}
		return finishEval();
	}
	
	public void initEval() {
		out = FileUtil.getPrintWriter(outputFile, ENCODING);
		log = (logFile==null) ? null : FileUtil.getPrintWriter(logFile, ENCODING);
		if (LABELED) {
			categoryStatistics = new Hashtable<String, int[]>();
			wrongCatStatistics = new Hashtable<String, Integer>();
			if (CONSTITUENTS_UNIT!=2) {
				crossingBracketsCatTable = new Hashtable<String, Integer>();
			}			
		}
		printHeader();
	}
	
	public float[] finishEval() {
		printFinalStatistics();
		if (LABELED) {
			printCategoryStatistics();
			printWrongCatStatistics();
		}
		float[] results = printSummaryStatistics();		// recall precision fscore
		out.close();
		if (log!=null) log.close();
		return results;
	}
	
	public void writeLog(String logString) {
		if (log!=null) log.println(logString);
	}

	public void updateStatistics(int index, TSNodeLabel TNgold, TSNodeLabel TNtest) {
		boolean compatible = !this.skipSentences.contains(new Integer(index-1));
		if (!compatible) {
			out.println(Utility.fsb(4,Integer.toString(index)) + ' ' +
						 Utility.fca(73, " SKIPPED SENTENCE ", '-'));
			return;
		}
		
		/*if (DELETE_LABELS.length>0) {
			TNgold.pruneSubTrees(DELETE_LABELS);
			TNtest.pruneSubTrees(DELETE_LABELS);
		}*/		
				
		ArrayList<? extends Constituency> goldConst = Constituency.collectConsituencies(
				TNgold, LABELED, EXCLUDE_CAT, CONSTITUENTS_UNIT, false);
		ArrayList<Constituency> goldConstCopy = new ArrayList<Constituency>(goldConst); 
		ArrayList<? extends Constituency> testConst = Constituency.collectConsituencies(
				TNtest, LABELED, EXCLUDE_CAT, CONSTITUENTS_UNIT, false);
		ArrayList<Constituency> testConstCopy = new ArrayList<Constituency>(testConst);
		int goldBrackets = goldConst.size();
		int testBrackets = testConst.size();	
		
		ArrayList<Constituency> matchedConst = new ArrayList<Constituency>();
		for(Constituency c : testConst) {
			if (goldConstCopy.remove(c)) {
				matchedConst.add(c);
				testConstCopy.remove(c);
			}
		}		
		
		if (log!=null) {
			ArrayList<Constituency> unmatchedGold = goldConstCopy;
			ArrayList<Constituency> unmatchedTest = testConstCopy;
			if (CONSTITUENTS_UNIT!=2) {
				unmatchedGold = ConstituencyWords.toYieldConstituency(unmatchedGold, TNgold, LABELED);
				unmatchedTest = ConstituencyWords.toYieldConstituency(unmatchedTest, TNtest, LABELED);
			}			
			log.println( "Sentence: " + index + "\n" +
					"\tGold: " + TNgold + "\n" +
					"\tTest: " + TNtest + "\n" +
					"\tUnmatched Gold: " + unmatchedGold + "\n" +
					"\tUnmatched Test: " + unmatchedTest + "\n\n" );
		}
		
		
		if (LABELED) {
			updateCategoryStatistics(testConst, goldConst, matchedConst);
			updateWrongCatStatistics(testConstCopy, goldConstCopy);
		}
		int words = TNgold.countLexicalNodesExcludingPosLabels(EXCLUDE_POS);				
		int matchBrackets = matchedConst.size();
		float recallPercentage = (float)matchBrackets * 100 / goldBrackets;
		float precisionPercentage = (float)matchBrackets * 100 / testBrackets;
		
		int correctTags=-1;
		float tagAccuracyPercentage=-1;
		if (compatible) {
			correctTags = CONSTITUENTS_UNIT!=2 ?
					TNtest.countCorrectPOS(TNgold, EXCLUDE_POS) :
					TNtest.countCorrectPOSdiffSizes(TNgold, EXCLUDE_POS);	
			tagAccuracyPercentage = (float)correctTags * 100 / words;
			/*if(tagAccuracyPercentage!=100) {
				System.out.println(TNtest.collectPreLexicalItems());
				System.out.println(TNgold.collectPreLexicalItems());
			}*/
		}		
		boolean exactMatch = matchBrackets==testBrackets && matchBrackets==goldBrackets;
		
		int crossBracket = -1;
		if (CONSTITUENTS_UNIT!=2) {
			crossBracket = ConstituencyWords.updateCrossingBrackets(testConst, goldConst, crossingBracketsCatTable);
		}
		
		int length = sentencesLength[index-1]; //TNgold.countLexicalNodes();
		
		out.println(Utility.fsb(4,Integer.toString(index)) + 
					(compatible ? ' ' : '*') + 
					Utility.fsb(6,Integer.toString(length)) + ' ' +
					Utility.fsb(7,formatNumber(recallPercentage)) + ' ' +
					Utility.fsb(7,formatNumber(precisionPercentage)) + ' ' +
					Utility.fsb(7,Integer.toString(matchBrackets)) + ' ' +
					Utility.fsb(6,Integer.toString(goldBrackets)) + ' ' +
					Utility.fsb(6,Integer.toString(testBrackets)) + ' ' +
					(CONSTITUENTS_UNIT==2 ? Utility.fsb(7, "-") : Utility.fsb(7,Integer.toString(crossBracket))) + ' ' +
					Utility.fsb(6,Integer.toString(words)) + ' ' +
					(compatible ? Utility.fsb(5,Integer.toString(correctTags)) : Utility.fsb(5,"-")) + ' ' +
					(compatible ? Utility.fsb(7,formatNumber(tagAccuracyPercentage)) : Utility.fsb(7,"-")));
		
		sentences ++;
		totalMatchBrackets += matchBrackets;
		totalGoldBrackets += goldBrackets;
		totalTestBrackets += testBrackets;		
		totalWords += words;
		totalCorrectTags += correctTags;
		if (exactMatch) totalExactMatch++;
		if (CONSTITUENTS_UNIT!=2) {
			totalCrossBracket += crossBracket;
			if (crossBracket==0) noCrossing++;
			if (crossBracket<=2) twoOrLessCrossing++;	
		}		
		
		if (length<=CUTOFF_LENGTH) {
			sentencesMSL ++;
			totalMatchBracketsMSL += matchBrackets;
			totalGoldBracketsMSL += goldBrackets;
			totalTestBracketsMSL += testBrackets;
			totalWordsMSL += words;
			totalCorrectTagsMSL += correctTags;
			if (exactMatch) totalExactMatchMSL++;
			if (CONSTITUENTS_UNIT!=2) {
				totalCrossBracketMSL += crossBracket;
				if (crossBracket==0) noCrossingMSL++;
				if (crossBracket<=2) twoOrLessCrossingMSL++;
			}			
		}
	}
	
	public static float getFScore(TSNodeLabel TNgold, TSNodeLabel TNtest, boolean preprocessStructures) {		
		int[] scores = getScores(TNgold, TNtest, preprocessStructures);
		return fscore(scores);
	}
	
	public static float getFScore(TSNodeLabel TNgold, TSNodeLabel TNtest) {		
		int[] scores = getScores(TNgold, TNtest, false);
		return fscore(scores);
	}
	
	public static int[] getScores(TSNodeLabel TNgold, TSNodeLabel TNtest) {
		return getScores(TNgold, TNtest, false);
	}
	
	public static float[] getRecallPrecisionFscore(TSNodeLabel TNgold, TSNodeLabel TNtest) {
		int[] scores = getScores(TNgold, TNtest, false);
		return recallPrecisionFscore(scores[0],scores[1],scores[2]);
	}
	
	/**
	 * matchBrackets, goldBrackets, parsedBrackets
	 * @param TNgold
	 * @param TNtest
	 * @return
	 */
	public static int[] getScores(TSNodeLabel TNgold, TSNodeLabel TNtest, boolean preprocessStructures) {
		
		if (preprocessStructures) {
			preprocessStructures(TNgold, TNtest);
		}		
		
		List<? extends Constituency> goldConst = Constituency.collectConsituencies(
				TNgold, LABELED, EXCLUDE_CAT, CONSTITUENTS_UNIT, false);
		List<Constituency> goldConstCopy = new ArrayList<Constituency>(goldConst); 
		List<? extends Constituency> testConst = Constituency.collectConsituencies(
				TNtest, LABELED, EXCLUDE_CAT, CONSTITUENTS_UNIT, false);
		
		int goldBrackets = goldConst.size();
		int testBrackets = testConst.size();	
		
		List<Constituency> matchedConst = new ArrayList<Constituency>();
		for(Constituency c : testConst) {
			if (goldConstCopy.remove(c)) {
				matchedConst.add(c);
			}
		}		
								
		int matchBrackets = matchedConst.size();
		return new int[]{matchBrackets, goldBrackets, testBrackets};
	}

	public static float fscore(int[] scores) {
		return fscore(scores[0], scores[1], scores[2]);
	}
	
	public static float fscore(int matchBrackets, int goldBrackets, int parsedBrackets) {
		float recall = (float)matchBrackets / goldBrackets;
		float precision = (float)matchBrackets / parsedBrackets;		
		return (recall==0 && precision==0) ? 0 : 2 * recall * precision / (recall + precision);
	}
	
	public static float[] recallPrecisionFscore(int matchBrackets, int goldBrackets, int parsedBrackets) {
		float recall = (float)matchBrackets / goldBrackets;
		float precision = (float)matchBrackets / parsedBrackets;		
		float fscore = (recall==0 && precision==0) ? 0 : 2 * recall * precision / (recall + precision);
		return new float[]{recall, precision, fscore};
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayList<TSNodeLabel>[] getDiff(TSNodeLabel TNgold, TSNodeLabel TNtest) {		
		ArrayList<? extends Constituency> goldConst = Constituency.collectConsituencies(
				TNgold, LABELED, EXCLUDE_CAT, CONSTITUENTS_UNIT, true);
		ArrayList<Constituency> goldConstCopy = new ArrayList<Constituency>(goldConst); 
		ArrayList<? extends Constituency> testConst = Constituency.collectConsituencies(
				TNtest, LABELED, EXCLUDE_CAT, CONSTITUENTS_UNIT, true);
		ArrayList<Constituency> testConstCopy = new ArrayList<Constituency>(testConst);		
		
		for(Constituency c : testConst) goldConstCopy.remove(c);		
		for(Constituency c : goldConst) testConstCopy.remove(c);	
		
		ArrayList<TSNodeLabel> goldConstDiffArray = new ArrayList<TSNodeLabel>();		
		ArrayList<TSNodeLabel> testConstDiffArray = new ArrayList<TSNodeLabel>();
		
		for(Constituency c : goldConstCopy) goldConstDiffArray.add(c.getNode());
		for(Constituency c : testConstCopy) testConstDiffArray.add(c.getNode());
		
		ArrayList<TSNodeLabel>[] diffGoldGuess = new ArrayList[]{goldConstDiffArray,testConstDiffArray};					

		return diffGoldGuess;
	}
	
	public void updateCategoryStatistics(List<? extends Constituency> goldConst, 
			List<? extends Constituency> testConst, List<Constituency> matchedConst) {
		totalCategoryGold += goldConst.size();
		for(Constituency c : goldConst) {
			Utility.increaseStringIntArray(categoryStatistics, 3, c.label(), 0, 1);
		}
		for(Constituency c : testConst) { 
			Utility.increaseStringIntArray(categoryStatistics, 3, c.label(), 1, 1);
		}
		for(Constituency c : matchedConst) { 
			Utility.increaseStringIntArray(categoryStatistics, 3, c.label(), 2, 1);
		}
	}
	
	public void updateWrongCatStatistics(
			ArrayList<Constituency> wrongTestConst, ArrayList<Constituency> wrongGoldConst) {
		
		int indexGold = -1;
		
		for(Constituency C : wrongGoldConst) {
			indexGold++;
			Constituency C_unlab = C.unlabeledCopy();
			int indexTestUnlabeled = wrongTestConst.indexOf(C_unlab);
			/*boolean ambiguous = ULwrongTestConst.lastIndexOf(C)!=indexTest;
			if (ambiguous) {
				System.err.println("Ambiguous costituency match");
			}*/
			if (indexTestUnlabeled!=-1) {
				Constituency labelTest = wrongTestConst.get(indexTestUnlabeled);
				Constituency labelGold = wrongGoldConst.get(indexGold);
				String key = labelTest.label() + "/" + labelGold.label();
				Utility.increaseStringInteger(this.wrongCatStatistics, key, 1);
			}
		}
	}
	
	public void printHeader() {
		//out.println(" Sent.              Matched    Bracket    Cross        Correct   Tag"); 
		out.println(Utility.fsb(11,"Sentence") + ' ' + 
				Utility.fsb(15,"") + ' ' +
				Utility.fsb(7,"Matched") + ' ' +
				Utility.fca(13,"Brackets",' ') + ' ' +
				Utility.fsb(7,"Cross") + ' ' +
				Utility.fsb(6,"") + ' ' +
				Utility.fsb(5,"Corr") + ' ' +
				Utility.fsb(7,"Tag") );
		//out.println("  ID   Length Recal  Prec. Bracket  gold   test  Bracket Words  Tags  Accracy");
		out.println(Utility.fsb(4,"ID") + ' ' + 
					Utility.fsb(6,"Length") + ' ' +
					Utility.fsb(7,"Recall") + ' ' +
					Utility.fsb(7,"Precis") + ' ' +
					Utility.fsb(7,"Bracket") + ' ' +
					Utility.fsb(6,"gold") + ' ' +
					Utility.fsb(6,"test") + ' ' +
					Utility.fsb(7,"Bracket") + ' ' +
					Utility.fsb(6,"Words") + ' ' +
					Utility.fsb(5,"Tags") + ' ' +
					Utility.fsb(7,"Accracy") );
		out.println(Utility.fillChar(78, '_'));
	}
	
	private void calculateFinalResults() {
		totalRecallPercentage = (float)totalMatchBrackets * 100 / totalGoldBrackets;
		totalPrecisionPercentage = (float)totalMatchBrackets * 100 / totalTestBrackets;		
		taggingAccuracyPercentage = (float) totalCorrectTags * 100 / totalWords;		
		
		totalRecallPercentageMSL = (float)totalMatchBracketsMSL * 100 / totalGoldBracketsMSL;
		totalPrecisionPercentageMSL = (float)totalMatchBracketsMSL * 100 / totalTestBracketsMSL;		
		taggingAccuracyPercentageMSL = (float) totalCorrectTagsMSL * 100 / totalWordsMSL;
	}

	public void printFinalStatistics() {
		calculateFinalResults();
		out.println(Utility.fillChar(78, '_'));
		out.println(
					Utility.fsb(4,"") + ' ' + 
					Utility.fsb(6,"") + ' ' +
					Utility.fsb(7,formatNumber(totalRecallPercentage)) + ' ' +
					Utility.fsb(7,formatNumber(totalPrecisionPercentage)) + ' ' +
					Utility.fsb(7,Integer.toString(totalMatchBrackets)) + ' ' +
					Utility.fsb(6,Integer.toString(totalGoldBrackets)) + ' ' +
					Utility.fsb(6,Integer.toString(totalTestBrackets)) + ' ' +
					(CONSTITUENTS_UNIT==2 ? Utility.fsb(7, "-") : Utility.fsb(7,Integer.toString(totalCrossBracket))) + ' ' +
					Utility.fsb(6,Integer.toString(totalWords)) + ' ' +
					Utility.fsb(5,Integer.toString(totalCorrectTags)) + ' ' +
					Utility.fsb(7,formatNumber(taggingAccuracyPercentage)) );		
	}
	
	public void printCategoryStatistics() {
		out.println();
		out.println(Utility.fca(58, " Category Statistics ", '_'));
		out.println();
		out.println( 	Utility.fsb(10, "label") + 
						Utility.fsb(12, "% gold") + 
						Utility.fsb(12, "catRecall") + 
						Utility.fsb(12, "catPrecis") +
						Utility.fsb(12, "catFScore") );
		out.println(Utility.fillChar(58, '_'));
		
		IdentityHashMap<Float,String> orderedCategory = new IdentityHashMap<Float,String>();		
		for(String label : categoryStatistics.keySet()) {
			int[] values = categoryStatistics.get(label);
			float percentageGold = (float) values[1] * 100 / totalCategoryGold; 
			float catRecall = (float) values[2] * 100 / values[0];
			float catPrecision = (float)values[2] * 100 / values[1];
			float catFScore = 2 * catRecall * catPrecision / (catRecall + catPrecision);			
			String line =	Utility.fsb(10, label) + 
							Utility.fsb(12, formatNumber(percentageGold)) +
							Utility.fsb(12, formatNumber(catRecall)) + 
							Utility.fsb(12, formatNumber(catPrecision)) +
							Utility.fsb(12, formatNumber(catFScore));
			orderedCategory.put(percentageGold, line);
		}
		Float[] percentageSorted = orderedCategory.keySet().toArray(new Float[]{});
		Arrays.sort(percentageSorted);
		for(int i=percentageSorted.length-1; i>=0; i--) {
			String line = orderedCategory.get(percentageSorted[i]);
			out.println(line);
		}
		out.println();
		if (CONSTITUENTS_UNIT!=2) {
			out.println(Utility.fsb(20, "Crossing brackets"));
			out.println( 	Utility.fsb(12, "test/gold") + 
							Utility.fsb(8, "count") );		
			out.println(Utility.fillChar(20, '_'));
			IdentityHashMap<Integer,String> reversedTable;
			reversedTable = Utility.reverseStringIntegerTable(crossingBracketsCatTable);
			
			Integer[] countSorted = reversedTable.keySet().toArray(new Integer[]{});
			Arrays.sort(countSorted);
			for(int i=countSorted.length-1; i>=0; i--) {
				Integer count = countSorted[i];
				String pair = reversedTable.get(count);
				out.println( Utility.fsb(12, pair) + Utility.fsb(8, count.toString()) );
			}
			out.println();
		}
	}
	
	public void printWrongCatStatistics() {
		out.println(Utility.fsb(20, "Wrong Category Statistics"));
		out.println( 	Utility.fsb(12, "test/gold") + 
						Utility.fsb(8, "count") );		
		out.println(Utility.fillChar(20, '_'));
		IdentityHashMap<Integer,String> reversedTable;
		reversedTable = Utility.reverseStringIntegerTable(wrongCatStatistics);
		
		Integer[] countSorted = reversedTable.keySet().toArray(new Integer[]{});
		Arrays.sort(countSorted);
		for(int i=countSorted.length-1; i>=0; i--) {
			Integer count = countSorted[i];
			String pair = reversedTable.get(count);
			out.println( Utility.fsb(12, pair) + Utility.fsb(8, count.toString()) );
		}		
		out.println();
	}
	
	public float[] getRecallPrecisionFscore() {
		float totalFMeasurePercentageMSL = 2 * totalRecallPercentageMSL * totalPrecisionPercentageMSL / 
				(totalRecallPercentageMSL + totalPrecisionPercentageMSL);
		return new float[]{totalRecallPercentageMSL, totalPrecisionPercentageMSL, totalFMeasurePercentageMSL};
	}
	
	public float[] getRecallPrecisionFscoreAll() {
		float totalFMeasurePercentage = 2 * totalRecallPercentage * totalPrecisionPercentage / 
				(totalRecallPercentage + totalPrecisionPercentage);
		return new float[]{totalRecallPercentage, totalPrecisionPercentage, totalFMeasurePercentage};
	}

	public float[] printSummaryStatistics() {
		float totalFMeasurePercentage = 2 * totalRecallPercentage * totalPrecisionPercentage / 
										(totalRecallPercentage + totalPrecisionPercentage);
		float averageCrossing = (CONSTITUENTS_UNIT==2 ? 0 : (float)totalCrossBracket / sentences);
		float completeMatchPercentage = (float) totalExactMatch * 100 / sentences;
		float noCrossingPercentage = (CONSTITUENTS_UNIT==2 ? 0 : (float) noCrossing * 100 / sentences);
		float twoOrLessCrossingPercentage = (CONSTITUENTS_UNIT==2 ? 0 : (float) twoOrLessCrossing * 100 / sentences);
		
		float totalFMeasurePercentageMSL = 2 * totalRecallPercentageMSL * totalPrecisionPercentageMSL / 
											(totalRecallPercentageMSL + totalPrecisionPercentageMSL);
		float averageCrossingMSL = (CONSTITUENTS_UNIT==2 ? 0 : (float)totalCrossBracketMSL / sentencesMSL);
		float completeMatchPercentageMSL = (float) totalExactMatchMSL * 100 / sentencesMSL;
		float noCrossingPercentageMSL = (CONSTITUENTS_UNIT==2 ? 0 : (float) noCrossingMSL * 100 / sentencesMSL);
		float twoOrLessCrossingPercentageMSL = (CONSTITUENTS_UNIT==2 ? 0 : (float) twoOrLessCrossingMSL * 100 / sentencesMSL);

		out.println();
		out.println(Utility.fca(34, " Summary ", '_'));
		out.println();
		out.println(Utility.fca(34, " All ", '_'));		
		out.println(
			"Number of sentences       =" + Utility.fsb(7,Integer.toString(sentences)) + "\n" +
			"Bracketing Recall         =" + Utility.fsb(7,formatNumber(totalRecallPercentage)) + "\n" +
			"Bracketing Precision      =" + Utility.fsb(7,formatNumber(totalPrecisionPercentage)) + "\n" +
			"Bracketing FMeasure       =" + Utility.fsb(7,formatNumber(totalFMeasurePercentage)) + "\n" +
			"Complete match            =" + Utility.fsb(7,formatNumber(completeMatchPercentage)) + "\n" +			
			"Average crossing          =" + (CONSTITUENTS_UNIT==2 ? Utility.fsb(7, "-") : Utility.fsb(7,formatNumber(averageCrossing))) + "\n" +
			"No crossing               =" + (CONSTITUENTS_UNIT==2 ? Utility.fsb(7, "-") : Utility.fsb(7,formatNumber(noCrossingPercentage))) + "\n" +
			"2 or less crossing        =" + (CONSTITUENTS_UNIT==2 ? Utility.fsb(7, "-") : Utility.fsb(7,formatNumber(twoOrLessCrossingPercentage))) + "\n" +
			"Tagging accuracy          =" + Utility.fsb(7,formatNumber(taggingAccuracyPercentage)) );
		out.println();
		out.println(Utility.fca(34, " len<=" + CUTOFF_LENGTH + " ", '_'));
		out.println(
				"Number of sentences       =" + Utility.fsb(7,Integer.toString(sentencesMSL)) + "\n" +
				"Bracketing Recall         =" + Utility.fsb(7,formatNumber(totalRecallPercentageMSL)) + "\n" +
				"Bracketing Precision      =" + Utility.fsb(7,formatNumber(totalPrecisionPercentageMSL)) + "\n" +
				"Bracketing FMeasure       =" + Utility.fsb(7,formatNumber(totalFMeasurePercentageMSL)) + "\n" +
				"Complete match            =" + Utility.fsb(7,formatNumber(completeMatchPercentageMSL)) + "\n" +
				"Average crossing          =" + (CONSTITUENTS_UNIT==2 ? Utility.fsb(7, "-") : Utility.fsb(7,formatNumber(averageCrossingMSL))) + "\n" +
				"No crossing               =" + (CONSTITUENTS_UNIT==2 ? Utility.fsb(7, "-") : Utility.fsb(7,formatNumber(noCrossingPercentageMSL))) + "\n" +
				"2 or less crossing        =" + (CONSTITUENTS_UNIT==2 ? Utility.fsb(7, "-") : Utility.fsb(7,formatNumber(twoOrLessCrossingPercentageMSL))) + "\n" +
				"Tagging accuracy          =" + Utility.fsb(7,formatNumber(taggingAccuracyPercentageMSL)) );
		
		return new float[]{totalRecallPercentageMSL, totalPrecisionPercentageMSL, totalFMeasurePercentageMSL};
	}
	
	public static void main1() {
		String basePath = "/scratch/fsangati/RESULTS/TSG/DOP_SD_Reranker/";
		File gold = new File(basePath + "wsj-22_gold.mrg");
		File test = new File(basePath + "wsj-22_reranked_5best_PQ.mrg");
		File evalF = new File(basePath + "wsj-22_reranked_5best_PQ.evalF");
		float[] rerankedFScore = EvalC.staticEvalC(gold, test, evalF);
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(rerankedFScore));
	}
	
	public static void main2(String[] args) {
		if (args.length != 3) {
			System.err.println("Error. Correct usage: EvalF goldFile testFile ouputFile");
			return;
		}
		File gold = new File(args[0]);
		File test = new File(args[1]);
		File evalF = new File(args[2]);
		float[] rerankedFScore = EvalC.staticEvalC(gold, test, evalF);
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(rerankedFScore));		
	}
	
	public static void main(String[] args) {
		
		/*
		args = new String[]{
			"-p", "/Users/fedja/Desktop/djame/ftb.prm",
			"-log", "/Users/fedja/Desktop/djame/evalC.log",
			"/Users/fedja/Desktop/djame/ftb_2.mrg.gold",
			"/Users/fedja/Desktop/djame/ftb_2.mrg.none_tagged.tobeparsed.parsed_smoothed_5.retokenized",
			"/Users/fedja/Desktop/djame/evalC.txt"
		};
		*/
		
		String usage = "USAGE: java -jar evalC.jar [-p paramFile] [-log logFile] goldFile testFile outputFile";
		//System.out.println(args.length + "\t" + Arrays.toString(args));		
		if (args.length==0 || args.length>7) {
			System.err.println("Incorrect number of arguments");
			System.err.println(usage);
			System.exit(-1);
		}
		int i=0;
		File paramFile=null, goldFile=null, testFile=null, outputFile=null, logFile=null;
		do {
			String currentArg = args[i];
			if (currentArg.equals("-p")) {
				i++;
				if (i==args.length) {
					System.err.println("Incorrect arguments");
					System.err.println(usage);
					System.exit(-1);
				}
				paramFile = new File(args[i++]);
				continue;
			}
			if (currentArg.equals("-log")) {
				i++;
				if (i==args.length) {
					System.err.println("Incorrect arguments");
					System.err.println(usage);
					System.exit(-1);
				}
				logFile = new File(args[i++]);
				continue;
			}
			if (goldFile==null) {
				goldFile =  new File(args[i++]);
				continue;
			}
			if (testFile==null) {
				testFile =  new File(args[i++]);
				continue;
			}			
			outputFile = new File(args[i++]);
		} while (i<args.length);
		if (goldFile==null || !goldFile.exists() || !goldFile.canRead()) {
			System.err.println("Gold file missing or cannot read it.");
			System.err.println(usage);
			System.exit(-1);
		}
		if (testFile==null || !testFile.exists() || !testFile.canRead()) {
			System.err.println("Test file missing or cannot read");
			System.err.println(usage);
			System.exit(-1);
		}
		if (outputFile==null) {
			System.err.println("Output file missing.");
			System.err.println(usage);
			System.exit(-1);
		}
		if (paramFile!=null) readParametersFromFile(paramFile);
		float[] rerankedFScore = EvalC.staticEvalC(goldFile, testFile, outputFile, logFile);
		if (rerankedFScore==null) System.out.println("Gold and Test files not compatible (different number of structures)");
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(rerankedFScore));		
	}
		
	/*public static void main(String[] args) {
		String workingDir = "/Users/fedja/Work/Code/TreeGrammars/evalC/sample/";
		args = new String[]{"-p", workingDir+"param.evalC", "-log", workingDir+"evalC.log", 
				workingDir+"sample.gld", workingDir+"sample.tst", workingDir+"sample1.evalC"};
		main1(args);
	}*/
		

	
}

