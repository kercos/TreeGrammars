package tesniere;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Scanner;

import settings.Parameters;
import tsg.HeadLabeler;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.file.FileUtil;
import viewer.TesniereTreePanel;

public class EvalTDS {
	
	public static boolean skipSentencesWithNullCategories = true;
	public static boolean skipSentencesWithStdBlocksWithoutFullWords = true;
	public static HeadLabeler HL = new HeadLabeler(new File("resources/fede.rules_04_08_10"));
	boolean areXml;
	File test, gold, report;
	PrintWriter reportWriter;
	ArrayList<Box> testStructures, goldStructures;
	int wordsTypeMatch, wordsTotal, wordsTotalExclPunct,
	boxesMatchingBoundaries, boxesMatchingFWstructure,
	testBoxes, goldBoxes, rawDepMatch,	
	catDepMatch, testJunctions, goldJunctions, junctionsMatch,
	correctHeadAttachExclPunct;
	
	public EvalTDS(File test, File gold) throws Exception {
		this.test = test;
		this.gold = gold;
		checkFileType();
		getStructures();
		//compareStructures();
	}
	
	public EvalTDS(File test, File gold, File reportFile) throws Exception {
		this.test = test;
		this.gold = gold;
		this.report = reportFile;
		reportWriter = FileUtil.getPrintWriter(report);
		checkFileType();
		getStructures();
		//compareStructures();
		//reportWriter.close();
	}
	
	public void closeReportFile() {
		reportWriter.close();
	}
	
	public void resetAllCounters() {
		wordsTypeMatch=0; 
		wordsTotal=0;
		wordsTotalExclPunct=0;
		boxesMatchingBoundaries=0;
		boxesMatchingFWstructure=0;
		testBoxes=0; 
		goldBoxes=0; 
		rawDepMatch=0;	
		catDepMatch=0; 
		testJunctions=0; 
		goldJunctions=0; 
		junctionsMatch=0;
		correctHeadAttachExclPunct=0;
	}
	
	private void checkFileType() {
		String firstLineFile = FileUtil.getFirstLineInFile(test);
		if (firstLineFile.startsWith("<?xml")) areXml = true;
	}
	
	private static ArrayList<Box> cleanCorpusAndGetTesniereTreebank(File inputFile) throws Exception {
		ArrayList<Box> result = new ArrayList<Box>();
		ArrayList<TSNodeLabel> constTreebank = Wsj.getTreebankReadableAndClean(inputFile);
		for(TSNodeLabel constTree : constTreebank) {		
			constTree.replaceAllNonTerminalLabels("_C", "-ADV");
			result.add(Conversion.getTesniereStructure(constTree, HL));
		}		
		return result;
	}
	
	private void getStructures() throws Exception {
		testStructures = new ArrayList<Box>();
		goldStructures = new ArrayList<Box>();
		if (areXml) {
			testStructures = Conversion.getTreebankFromXmlFile(test);
			goldStructures = Conversion.getTreebankFromXmlFile(gold);			
		}
		else {
			testStructures = cleanCorpusAndGetTesniereTreebank(test);
			goldStructures = cleanCorpusAndGetTesniereTreebank(gold);			
		}
	}
	
	public void compareStructures() {
		if (testStructures.size() != goldStructures.size()) {
			reportLine("Test and gold have different size.");
			System.exit(-1);
		}
		Iterator<Box> testIter = testStructures.iterator();
		Iterator<Box> goldIter = goldStructures.iterator();
		int sentenceNumber = 0;
		while(testIter.hasNext()) {
			sentenceNumber++;
			Box testTree = testIter.next();
			Box goldTree = goldIter.next();
			testTree.fixOriginalCategories();
			goldTree.fixOriginalCategories();
			if (skipSentencesWithNullCategories) {
				if (goldTree.hasNullCategories()) {
					reportLine("Skipping sentence " + sentenceNumber + 
							" since gold contains blocks without categories.");
					continue;
				}
				if (testTree.hasNullCategories()) {
					reportLine("Skipping sentence " + sentenceNumber + 
							" since test contains blocks without categories.");
					continue;
				}
			}
			if (skipSentencesWithStdBlocksWithoutFullWords) {
				if (goldTree.hasStdBlocksWithoutFullWords()) {
					reportLine("Skipping sentence " + sentenceNumber + 
						" since gold contains standard blocks without full words.");
					continue;
				}
				if (testTree.hasStdBlocksWithoutFullWords()) {
					reportLine("Skipping sentence " + sentenceNumber + 
						" since test contains standard blocks without full words.");
					continue;
				}
			}	
			compare(testTree, goldTree, sentenceNumber);			
			/*if (!isPerfectMatch()) {
				System.out.println(sentenceNumber);
				reportResults();
			}	
			this.resetAllCounters();*/			
		}		
	}
	
	public void compareCoordination() {
		if (testStructures.size() != goldStructures.size()) {
			reportLine("Test and gold have different size.");
			System.exit(-1);
		}
		Iterator<Box> testIter = testStructures.iterator();
		Iterator<Box> goldIter = goldStructures.iterator();
		reportLine("#\tTest\tGold\tMatch");
		int sentenceNumber = 0;
		while(testIter.hasNext()) {
			sentenceNumber++;
			Box testTree = testIter.next();
			Box goldTree = goldIter.next();
			testTree.fixOriginalCategories();
			goldTree.fixOriginalCategories();
			if (skipSentencesWithNullCategories) {
				if (goldTree.hasNullCategories()) {
					reportLine(sentenceNumber + ": skipped (gold contains blocks without categories)"); 
					continue;
				}
				if (testTree.hasNullCategories()) {
					reportLine(sentenceNumber + ": skipped (test contains blocks without categories)"); 
					continue;
				}
			}
			if (skipSentencesWithStdBlocksWithoutFullWords) {
				if (goldTree.hasStdBlocksWithoutFullWords()) {
					reportLine(sentenceNumber + ": skipped (gold contains standard blocks without full words)");
					continue;
				}
				if (testTree.hasStdBlocksWithoutFullWords()) {
					reportLine(sentenceNumber + ": skipped (test contains standard blocks without full words)");
					continue;
				}
			}	
			int testBoxCount = testTree.countAllNodes();
			int goldBoxCount = goldTree.countAllNodes();		
			Box[] testBoxesArray = testTree.collectBoxStructure().toArray(new Box[testBoxCount]);
			Box[] goldBoxesArray = goldTree.collectBoxStructure().toArray(new Box[goldBoxCount]);
			int[] junctionResults = getJunctionMatch(testBoxesArray, goldBoxesArray);		
			testJunctions += junctionResults[0];
			goldJunctions += junctionResults[1];
			junctionsMatch += junctionResults[2];
			reportLine(sentenceNumber + ":\t" + junctionResults[0] + "\t" + junctionResults[1] + "\t" + junctionResults[2]);
		}
		float[] junctionsMatchingFWstructureScores = scores(junctionsMatch, goldJunctions, testJunctions);
		reportLine("Junctions Matching FullWords Structures Scores (recall/precision/fscore):\t" + 
				Arrays.toString(junctionsMatchingFWstructureScores));
	}
	
	
	public void compareDependencies() {
		if (testStructures.size() != goldStructures.size()) {
			reportLine("Test and gold have different size.");
			System.exit(-1);
		}		
		Iterator<Box> testIter = testStructures.iterator();
		Iterator<Box> goldIter = goldStructures.iterator();
		reportLine("#Words\t#Match");
		int sentenceNumber = 0;
		while(testIter.hasNext()) {
			sentenceNumber++;
			Box testTree = testIter.next();
			Box goldTree = goldIter.next();
			testTree.fixOriginalCategories();
			goldTree.fixOriginalCategories();
			if (skipSentencesWithNullCategories) {
				if (goldTree.hasNullCategories()) {
					reportLine(sentenceNumber + ": skipped (gold contains blocks without categories)"); 
					continue;
				}
				if (testTree.hasNullCategories()) {
					reportLine(sentenceNumber + ": skipped (test contains blocks without categories)"); 
					continue;
				}
			}
			if (skipSentencesWithStdBlocksWithoutFullWords) {
				if (goldTree.hasStdBlocksWithoutFullWords()) {
					reportLine(sentenceNumber + ": skipped (gold contains standard blocks without full words)");
					continue;
				}
				if (testTree.hasStdBlocksWithoutFullWords()) {
					reportLine(sentenceNumber + ": skipped (test contains standard blocks without full words)");
					continue;
				}
			}		
			IdentityHashMap<Word, Box> testWordBoxMapping = new IdentityHashMap<Word, Box>();
			IdentityHashMap<Word, Box> goldWordBoxMapping = new IdentityHashMap<Word, Box>();
			int sentenceLength = goldTree.countAllWords();
			Word[] testWordsArray = new Word[sentenceLength];		
			Word[] goldWordsArray = new Word[sentenceLength];
			testTree.fillWordArrayTable(testWordsArray, testWordBoxMapping);
			goldTree.fillWordArrayTable(goldWordsArray, goldWordBoxMapping);	
			
			int[] depResults = getRawDepMatch(testWordsArray, goldWordsArray, testWordBoxMapping, goldWordBoxMapping);
			wordsTotal+= depResults[0];
			rawDepMatch+= depResults[1];
			reportLine(sentenceNumber + ":\t" + depResults[0] + "\t" + depResults[1]);
		}
		float rawDepAccuracy = (float)rawDepMatch/wordsTotal;
		reportLine("Raw Dependency Match (match/total/accuracy):\t" + 
				rawDepMatch + "\t" + wordsTotal + "\t" + rawDepAccuracy);
	}
	
	public static float[] scores(int matchBrackets, int goldBrackets, int parsedBrackets) {
		float recall = (float)matchBrackets / goldBrackets;
		float precision = (float)matchBrackets / parsedBrackets;		
		float fscore = (recall==0 && precision==0) ? 0 : 2 * recall * precision / (recall + precision);
		return new float[]{recall, precision, fscore};
	}
	
	private boolean isPerfectMatch() {
		if (wordsTypeMatch!=wordsTotal) return false; 		
		if (boxesMatchingBoundaries!=goldBoxes || boxesMatchingBoundaries!=testBoxes) return false;		
		if (boxesMatchingFWstructure!=goldBoxes || boxesMatchingFWstructure!=testBoxes) return false;
		if (junctionsMatch!=goldJunctions || junctionsMatch!=testJunctions) return false;
		if (rawDepMatch!=wordsTotal) return false;
		if (catDepMatch!=wordsTotal) return false;
		if (correctHeadAttachExclPunct!=wordsTotalExclPunct) return false;
		return true;
	}
	
	public void reportLine(String s) {
		if (reportWriter!=null) reportWriter.println(s);
		System.out.println(s);
	}

	public void reportResults() {
		float wordsTypeAccuracy = (float)wordsTypeMatch/wordsTotal;
		reportLine("Words-Type Match (match/total/accuracy):\t" + 
				wordsTypeMatch + "\t" + wordsTotal + "\t" + wordsTypeAccuracy);
		float[] boxesMatchingBoundariesScores = scores(boxesMatchingBoundaries, goldBoxes, testBoxes);	
		reportLine("BDS: Boxes Matching Boundaries Scores (recall/precision/fscore):\t" + 
				Arrays.toString(boxesMatchingBoundariesScores));
		float[] boxesMatchingFWstructureScores = scores(boxesMatchingFWstructure, goldBoxes, testBoxes);
		reportLine("Boxes Matching FullWords Structures Scores (recall/precision/fscore):\t" + 
				Arrays.toString(boxesMatchingFWstructureScores));
		float[] junctionsMatchingFWstructureScores = scores(junctionsMatch, goldJunctions, testJunctions);
		reportLine("JDS: Junctions Matching FullWords Structures Scores (recall/precision/fscore):\t" + 
				Arrays.toString(junctionsMatchingFWstructureScores));
		float rawDepAccuracy = (float)rawDepMatch/wordsTotalExclPunct;
		reportLine("BAS: UnCat Dependency Match Exluse Punctuation (match/total/accuracy):\t" + 
				rawDepMatch + "\t" + wordsTotalExclPunct + "\t" + rawDepAccuracy);
		float catDepAccuracy = (float)catDepMatch/wordsTotalExclPunct;
		reportLine("Cat Dependency Match  Exluse Punctuation (match/total/accuracy):\t" + 
				catDepMatch + "\t" + wordsTotalExclPunct + "\t" + catDepAccuracy);		
		float attachmentScore = (float)correctHeadAttachExclPunct/wordsTotalExclPunct;
		reportLine("UAS: Attachment Score Exluse Punctuation (match/total/accuracy):\t" + 
				correctHeadAttachExclPunct + "\t" + wordsTotalExclPunct + "\t" + attachmentScore);						
	}
	
	/**
	 * UAS	BDS	BAS	JDS
	 * @return
	 */
	public float[] getResults() {		
		float UAS = (float)correctHeadAttachExclPunct/wordsTotalExclPunct;
		float BDS = scores(boxesMatchingBoundaries, goldBoxes, testBoxes)[2];
		float JDS = scores(junctionsMatch, goldJunctions, testJunctions)[2];
		float BAS = (float)rawDepMatch/wordsTotalExclPunct;
		return new float[]{UAS, BDS, BAS, JDS};
	}

	private void compare(Box testTree, Box goldTree, int sentenceNumber) {
		int testSentenceLength = testTree.countAllWords();
		int goldSentenceLength = goldTree.countAllWords();
		if (testSentenceLength != goldSentenceLength) {
			System.err.println("Test and gold structure #" + sentenceNumber + " don't match in length." + 
					"\nGold: " + goldTree + "\nTest: " + testTree);
			
			System.exit(-1);
		}
		IdentityHashMap<Word, Box> testWordBoxMapping = new IdentityHashMap<Word, Box>();
		IdentityHashMap<Word, Box> goldWordBoxMapping = new IdentityHashMap<Word, Box>();
		Word[] testWordsArray = new Word[testSentenceLength];		
		Word[] goldWordsArray = new Word[goldSentenceLength];
		testTree.fillWordArrayTable(testWordsArray, testWordBoxMapping);
		goldTree.fillWordArrayTable(goldWordsArray, goldWordBoxMapping);		
		int testBoxCount = testTree.countAllNodes();
		int goldBoxCount = goldTree.countAllNodes();		
		Box[] testBoxesArray = testTree.collectBoxStructure().toArray(new Box[testBoxCount]);
		Box[] goldBoxesArray = goldTree.collectBoxStructure().toArray(new Box[goldBoxCount]);
		int[][] testBoxBoundaries = getBoxBoundaries(testBoxesArray);
		int[][] goldBoxBoundaries = getBoxBoundaries(goldBoxesArray);
		wordsTotal += testSentenceLength;
		wordsTotalExclPunct += countWordsExclPunctuation(goldWordsArray);
		for(int i=0; i<testSentenceLength; i++) {
			if (testWordsArray[i].sameType(goldWordsArray[i])) wordsTypeMatch++;
		}
		testBoxes += testBoxCount;
		goldBoxes += goldBoxCount;
		boxesMatchingBoundaries += countBoxesMatchingBoundaries(testBoxesArray, 
				goldBoxesArray, testBoxBoundaries, goldBoxBoundaries);
		boxesMatchingFWstructure += countBoxesMatchingFWstructure(testBoxesArray, goldBoxesArray);
		updateJunctionMatch(testBoxesArray, goldBoxesArray);
		updateDepMatch(testWordsArray, goldWordsArray, testWordBoxMapping, goldWordBoxMapping);
		updateAttachmentScore(testWordsArray, goldWordsArray, testWordBoxMapping, goldWordBoxMapping);
	}
	
	public static int countWordsExclPunctuation(Word[] array) {
		int result = 0;
		for(Word w : array) {
			if (!w.isPunctuation()) result++;
		}
		return result;
	}

	private void updateAttachmentScore(Word[] testWordsArray,
			Word[] goldWordsArray,
			IdentityHashMap<Word, Box> testWordBoxMapping,
			IdentityHashMap<Word, Box> goldWordBoxMapping) {
		for(int i=0; i<testWordsArray.length; i++) {
			Word testWord = testWordsArray[i];
			if (testWord.isPunctuation()) continue;			
			Word goldWord = goldWordsArray[i];
			int testHeadPosition = getHeadPosition(testWord, testWordBoxMapping.get(testWord));
			int goldHeadPosition = getHeadPosition(goldWord, goldWordBoxMapping.get(goldWord));
			if (testHeadPosition==goldHeadPosition) correctHeadAttachExclPunct++;
		}		
	}

	public static int[] getAttScore(Box goldTree, Box testTree) {
		int testSentenceLength = testTree.countAllWords();
		int goldSentenceLength = goldTree.countAllWords();
		Word[] testWordsArray = new Word[testSentenceLength];		
		Word[] goldWordsArray = new Word[goldSentenceLength];	
		
		if (testSentenceLength != goldSentenceLength) {
			System.err.println("Test and gold structure don't match in length." + 
					"\nGold: " + goldTree + "\nTest: " + testTree);
			
			System.exit(-1);
		}
		IdentityHashMap<Word, Box> testWordBoxMapping = new IdentityHashMap<Word, Box>();
		IdentityHashMap<Word, Box> goldWordBoxMapping = new IdentityHashMap<Word, Box>();
		testTree.fillWordArrayTable(testWordsArray, testWordBoxMapping);
		goldTree.fillWordArrayTable(goldWordsArray, goldWordBoxMapping);
		int[] result = new int[]{0,0};
		for(int i=0; i<testWordsArray.length; i++) {
			Word testWord = testWordsArray[i];
			if (testWord.isPunctuation()) continue;
			result[0]++;
			Word goldWord = goldWordsArray[i];
			int testHeadPosition = getHeadPosition(testWord, testWordBoxMapping.get(testWord));
			int goldHeadPosition = getHeadPosition(goldWord, goldWordBoxMapping.get(goldWord));
			if (testHeadPosition==goldHeadPosition) result[1]++;
		}
		return result;
	}

	/**
	 * The head of an empty word is the last full word of the same block.
	 * The head of a full word is the full word of the parent of the current block, if
	 * the parent is a standard block.
	 * If the parent is a junction block, return the full word of the first conjunct if
	 * it's not the same block, go up otherwise.
	 * @param testWord
	 * @param box
	 * @return
	 */
	private static int getHeadPosition(Word testWord, Box box) {
		if (testWord.isEmpty()) { 
			while(box.isJunctionBlock()) {
				box = ((BoxJunction) box).conjuncts.first();				
			}
			return ((BoxStandard) box).fwList.last().position;
		}
		else {
			Box pBox = box.parent;
			if (pBox==null) return -1;
			while(pBox.isJunctionBlock()) {
				Box firstConjunct = ((BoxJunction) pBox).conjuncts.first();
				if (box==firstConjunct) {
					pBox = pBox.parent;
					box = pBox;
					if (pBox==null) return -1;
				}
				else {
					pBox = firstConjunct;
					while(pBox.isJunctionBlock()) {
						pBox = ((BoxJunction) pBox).conjuncts.first();				
					}
					return ((BoxStandard) pBox).fwList.last().position;
				}
			}
			return ((BoxStandard) pBox).fwList.last().position;
		}
	}

	private void updateDepMatch(Word[] testWordsArray, Word[] goldWordsArray,
			IdentityHashMap<Word, Box> testWordBoxMapping,
			IdentityHashMap<Word, Box> goldWordBoxMapping) {
		
		for(int i=0; i<testWordsArray.length; i++) {
			Word tw = testWordsArray[i];
			if (tw.isPunctuation()) continue;		
			Word gw = goldWordsArray[i];
			Box btw = testWordBoxMapping.get(tw);
			Box bgw = goldWordBoxMapping.get(gw);
			Box pbtw = btw.governingParent();
			Box pbgw = bgw.governingParent();
			if (pbtw==null || pbgw==null) {
				if (pbtw==null && pbgw==null) {
					rawDepMatch++;
					if (btw.getOriginalCat()==bgw.getOriginalCat() &&
							btw.getDerivedCat()==bgw.getDerivedCat()) catDepMatch++;
				}
			}
			else if (pbtw.matchFullWordsStructure(pbgw)) {
				rawDepMatch++;
				if (btw.getOriginalCat()==bgw.getOriginalCat() &&
						btw.getDerivedCat()==bgw.getDerivedCat()) catDepMatch++;
			}
		}		
	}
	
	public static float[] rawDepAttachmentScore(int[] totalRawDepScore) {
		float rawScore = (float)totalRawDepScore[1]/totalRawDepScore[0];
		float depScore = (float)totalRawDepScore[2]/totalRawDepScore[0];
		return new float[]{rawScore, depScore};
	}
	
	public static int[] getCatRawDepMatch(Box goldTree, Box testTree) {
		int testSentenceLength = testTree.countAllWords();
		int goldSentenceLength = goldTree.countAllWords();
		if (testSentenceLength != goldSentenceLength) {
			System.err.println("Test and gold structure don't match in length." + 
					"\nGold: " + goldTree + "\nTest: " + testTree);
			
			System.exit(-1);
		}
		IdentityHashMap<Word, Box> testWordBoxMapping = new IdentityHashMap<Word, Box>();
		IdentityHashMap<Word, Box> goldWordBoxMapping = new IdentityHashMap<Word, Box>();
		Word[] testWordsArray = new Word[testSentenceLength];		
		Word[] goldWordsArray = new Word[goldSentenceLength];
		testTree.fillWordArrayTable(testWordsArray, testWordBoxMapping);
		goldTree.fillWordArrayTable(goldWordsArray, goldWordBoxMapping);				
		int[] result = new int[]{0, 0, 0};
		for(int i=0; i<testWordsArray.length; i++) {
			Word tw = testWordsArray[i];
			if (tw.isPunctuation()) continue;
			result[0]++;
			Word gw = goldWordsArray[i];
			Box btw = testWordBoxMapping.get(tw);
			Box bgw = goldWordBoxMapping.get(gw);
			Box pbtw = btw.governingParent();
			Box pbgw = bgw.governingParent();
			if (pbtw==null || pbgw==null) {
				if (pbtw==null && pbgw==null) {
					result[1]++;
					if (btw.getOriginalCat()==bgw.getOriginalCat() &&
							btw.getDerivedCat()==bgw.getDerivedCat()) result[2]++;
				}
			}
			else if (pbtw.matchFullWordsStructure(pbgw)) {
				result[1]++;
				if (btw.getOriginalCat()==bgw.getOriginalCat() &&
						btw.getDerivedCat()==bgw.getDerivedCat()) result[2]++;
			}
		}	
		return result;
	}
	
	public static int[] getRawDepMatch(Word[] testWordsArray, Word[] goldWordsArray,
			IdentityHashMap<Word, Box> testWordBoxMapping,
			IdentityHashMap<Word, Box> goldWordBoxMapping) {
		int[] result = new int[]{0, 0};
		for(int i=0; i<testWordsArray.length; i++) {
			Word tw = testWordsArray[i];
			if (tw.isPunctuation()) continue;
			result[0]++;
			Word gw = goldWordsArray[i];
			Box btw = testWordBoxMapping.get(tw);
			Box bgw = goldWordBoxMapping.get(gw);
			Box pbtw = btw.governingParent();
			Box pbgw = bgw.governingParent();
			if (pbtw==null || pbgw==null) {
				if (pbtw==null && pbgw==null) result[1]++;
			}
			else if (pbtw.matchFullWordsStructure(pbgw)) result[1]++;
		}	
		return result;
	}

	private void updateJunctionMatch(Box[] testBoxesArray, Box[] goldBoxesArray) {
		ArrayList<BoxJunction> testBoxJunction = new ArrayList<BoxJunction>();
		ArrayList<BoxJunction> goldBoxJunction = new ArrayList<BoxJunction>();
		for(Box b : testBoxesArray) {
			if (b.isJunctionBlock()) testBoxJunction.add( (BoxJunction) b);
		}
		for(Box b : goldBoxesArray) {
			if (b.isJunctionBlock()) goldBoxJunction.add( (BoxJunction) b);
		}
		testJunctions += testBoxJunction.size(); 
		goldJunctions += goldBoxJunction.size();
		for(BoxJunction bt : testBoxJunction) {
			for(BoxJunction bg : goldBoxJunction) {
				if (bt.matchFullWordsStructure(bg)) junctionsMatch++;
			}			
		}
	}
	
	public static int[] getJunctionMatch(Box[] testBoxesArray, Box[] goldBoxesArray) {
		ArrayList<BoxJunction> testBoxJunction = new ArrayList<BoxJunction>();
		ArrayList<BoxJunction> goldBoxJunction = new ArrayList<BoxJunction>();
		for(Box b : testBoxesArray) {
			if (b.isJunctionBlock()) testBoxJunction.add( (BoxJunction) b);
		}
		for(Box b : goldBoxesArray) {
			if (b.isJunctionBlock()) goldBoxJunction.add( (BoxJunction) b);
		}
		int[] result = new int[]{testBoxJunction.size(), goldBoxJunction.size(), 0};
		for(BoxJunction bt : testBoxJunction) {
			for(BoxJunction bg : goldBoxJunction) {
				if (bt.matchFullWordsStructure(bg)) result[2]++;
			}			
		}
		return result;
	}
	
	private static int countBoxesMatchingFWstructure(Box[] testBoxesArray, Box[] goldBoxesArray) {
		int result = 0;
		for(Box bt : testBoxesArray) {
			for(Box bg : goldBoxesArray) {
				if (bt.matchFullWordsStructure(bg)) result++;
			}
		}
		return result;
	}

	private static int countBoxesMatchingBoundaries(Box[] testBoxesArray, Box[] goldBoxesArray,
			int[][] testBoxBoundaries, int[][] goldBoxBoundaries) {
		int totalMatch = 0;
		for(int i=0; i<testBoxesArray.length; i++) {
			Box testBox = testBoxesArray[i];
			int matchingIndex = getIndexMatchingBoundaries(testBoxBoundaries[i], goldBoxBoundaries);
			if (matchingIndex==-1) continue;
			if (testBox.sameType(goldBoxesArray[matchingIndex])) totalMatch++;
		}
		return totalMatch;
	}

	private static int getIndexMatchingBoundaries(int[] boundariesToMatch,
			int[][] goldBoxBoundaries) {
		for(int i=0; i<goldBoxBoundaries.length; i++) {
			if (goldBoxBoundaries[i][0]==boundariesToMatch[0] &&
					goldBoxBoundaries[i][1]==boundariesToMatch[1]) return i;
		}
		return -1;		
	}

	private int[][] getBoxBoundaries(Box[] testBoxesArray) {
		int[][] result = new int[testBoxesArray.length][2];
		for(int i=0; i<testBoxesArray.length; i++) {
			Box currentBox = testBoxesArray[i];
			Word leftWord = currentBox.getLeftMostWordExcludingPunctuation();
			Word rightWord = currentBox.getRightMostWordExcludingPunctuation();
			result[i][0] = leftWord.position;
			result[i][1] = rightWord.position;
		}
		return result;
	}
	
	public static void eval() throws Exception {
		String workingDir = Parameters.resultsPath + "ChiarniakParser/wsj22/";
		//File goldFile = new File(workingDir + "wsj-22_clean.mrg");		
		File goldFileSemTagOff = new File(workingDir + "wsj-22_clean_semTagOff.mrg");
		//Wsj.removeSemTag(goldFile, goldFileSemTagOff);
		File oneBestFile = new File(workingDir + "wsj-22_oneBest.mrg");
		File oneBestreportFile = new File(workingDir + "oneBest_evalTDS.txt");
		EvalTDS e = new EvalTDS(oneBestFile, goldFileSemTagOff, oneBestreportFile);
		e.compareStructures();
		e.closeReportFile();
		File oracleBestFile = new File(workingDir + "wsj-22_oracleBest.mrg");
		File oracleBestreportFile = new File(workingDir + "oracleBest_evalTDS.txt");
		e = new EvalTDS(oracleBestFile, goldFileSemTagOff, oracleBestreportFile);
		e.compareStructures();
		e.closeReportFile();
		File oracleWorstFile = new File(workingDir + "wsj-22_oracleWorst.mrg");
		File oracleWorstreportFile = new File(workingDir + "oracleWorst_evalTDS.txt");
		e = new EvalTDS(oracleWorstFile, goldFileSemTagOff, oracleWorstreportFile);
		e.compareStructures();
		e.closeReportFile();
		
	}
	
	public static void evalCoordination() throws Exception {
		String workingDir = Parameters.resultsPath + "ChiarniakParser/wsj22/";		
		File goldFileSemTagOff = new File(workingDir + "wsj-22_clean_semTagOff.mrg");		
		File oneBestFile = new File(workingDir + "wsj-22_oneBest.mrg");
		File oracleBestFile = new File(workingDir + "wsj-22_oracleBest.mrg");
		File oneBestReportFile = new File(workingDir + "oneBest_evalTDS_coord.txt");
		File oracleBestReportFile = new File(workingDir + "oracleBest_evalTDS_coord.txt");
		EvalTDS e = new EvalTDS(oneBestFile, goldFileSemTagOff, oneBestReportFile);
		e.compareCoordination();
		e.closeReportFile();
		e = new EvalTDS(oracleBestFile, goldFileSemTagOff, oracleBestReportFile);
		e.compareCoordination();
		e.closeReportFile();
		File report = new File(workingDir + "oneBest_oracleBest_evalTDS_coord.txt");
		File treeReport = new File(workingDir + "oneBest_oracleBest_evalTDS_coord_trees.txt");
		
		PrintWriter reportWriter = FileUtil.getPrintWriter(report);
		PrintWriter treeReportWriter = FileUtil.getPrintWriter(treeReport);
		
		Scanner oneBestScanner = FileUtil.getScanner(oneBestReportFile);
		Scanner oracleBestScanner = FileUtil.getScanner(oracleBestReportFile);
		
		Scanner goldTreeScanner = FileUtil.getScanner(goldFileSemTagOff);
		Scanner oneBestTreeScanner = FileUtil.getScanner(oneBestFile);
		Scanner oracleBestTreeScanner = FileUtil.getScanner(oracleBestFile);
		reportWriter.println("#\tgold\toneBest\toneBestMatch\toracleBest\toracleBestMatch");
		
		int sentenceNumber = -1;
		while(oneBestScanner.hasNextLine()) {
			sentenceNumber++;									
			String oneBestLine = oneBestScanner.nextLine();
			String oracleBestLine = oracleBestScanner.nextLine();
			if (sentenceNumber==0) continue;
			String goldTree = null;
			String oneBestTree = null;
			String oracleBestTree = null;
			if (oneBestTreeScanner.hasNext()) {
				goldTree = goldTreeScanner.nextLine();
				oneBestTree = oneBestTreeScanner.nextLine();
				oracleBestTree = oracleBestTreeScanner.nextLine();
			}
			String[] oneBestLineSplit = oneBestLine.split("\t");
			String[] oracleBestLineSplit = oracleBestLine.split("\t");
			if (oneBestLineSplit.length!=4 || oracleBestLineSplit.length!=4) continue;
			int gold = Integer.parseInt(oneBestLineSplit[2]);			
			int oneBest = Integer.parseInt(oneBestLineSplit[1]);
			int oneBestMatch = Integer.parseInt(oneBestLineSplit[3]);
			int oracleBest = Integer.parseInt(oracleBestLineSplit[1]);
			int oracleBestMatch = Integer.parseInt(oracleBestLineSplit[3]);
			if (oneBestMatch!=oracleBestMatch) {
				reportWriter.println(sentenceNumber + "\t" + gold + "\t" + oneBest + "\t" +
						oneBestMatch + "\t" + oracleBest + "\t" + oracleBestMatch);
				TSNodeLabel goldTreeStructure = new TSNodeLabel(goldTree);
				TSNodeLabel oneBestTreeStructure = new TSNodeLabel(oneBestTree);
				TSNodeLabel oracleBestTreeStructure = new TSNodeLabel(oracleBestTree);
				goldTreeStructure.replaceAllNonTerminalLabels("_C", "-ADV ");
				oneBestTreeStructure.replaceAllNonTerminalLabels("_C", "-ADV ");
				oracleBestTreeStructure.replaceAllNonTerminalLabels("_C", "-ADV ");				
				treeReportWriter.println(oneBestTreeStructure);
				treeReportWriter.println(oracleBestTreeStructure);
				treeReportWriter.println(goldTreeStructure);
			}
		}
		
		reportWriter.close();
		treeReportWriter.close();
	}
	
	public static void evalDep() throws Exception {
		String workingDir = Parameters.resultsPath + "ChiarniakParser/wsj22/";		
		File goldFileSemTagOff = new File(workingDir + "wsj-22_clean_semTagOff.mrg");		
		File oneBestFile = new File(workingDir + "wsj-22_oneBest.mrg");
		File oracleBestFile = new File(workingDir + "wsj-22_oracleBest.mrg");
		File oneBestReportFile = new File(workingDir + "oneBest_evalTDS_dep.txt");
		File oracleBestReportFile = new File(workingDir + "oracleBest_evalTDS_dep.txt");
		EvalTDS e = new EvalTDS(oneBestFile, goldFileSemTagOff, oneBestReportFile);
		e.compareDependencies();
		e.closeReportFile();
		e = new EvalTDS(oracleBestFile, goldFileSemTagOff, oracleBestReportFile);
		e.compareDependencies();
		e.closeReportFile();
		File report = new File(workingDir + "oneBest_oracleBest_evalTDS_dep.txt");
		File treeReport = new File(workingDir + "oneBest_oracleBest_evalTDS_dep_trees.txt");
		
		PrintWriter reportWriter = FileUtil.getPrintWriter(report);
		PrintWriter treeReportWriter = FileUtil.getPrintWriter(treeReport);
		
		Scanner oneBestScanner = FileUtil.getScanner(oneBestReportFile);
		Scanner oracleBestScanner = FileUtil.getScanner(oracleBestReportFile);
		
		Scanner goldTreeScanner = FileUtil.getScanner(goldFileSemTagOff);
		Scanner oneBestTreeScanner = FileUtil.getScanner(oneBestFile);
		Scanner oracleBestTreeScanner = FileUtil.getScanner(oracleBestFile);
		reportWriter.println("#\twords\toneBestCorrect\toracleBestCorrect\timprovement");
		
		int sentenceNumber = -1;
		while(oneBestScanner.hasNextLine()) {
			sentenceNumber++;									
			String oneBestLine = oneBestScanner.nextLine();
			String oracleBestLine = oracleBestScanner.nextLine();
			if (sentenceNumber==0) continue;
			String goldTree = null;
			String oneBestTree = null;
			String oracleBestTree = null;
			if (oneBestTreeScanner.hasNext()) {
				goldTree = goldTreeScanner.nextLine();
				oneBestTree = oneBestTreeScanner.nextLine();
				oracleBestTree = oracleBestTreeScanner.nextLine();
			}
			String[] oneBestLineSplit = oneBestLine.split("\t");
			String[] oracleBestLineSplit = oracleBestLine.split("\t");
			if (oneBestLineSplit.length!=3 || oracleBestLineSplit.length!=3) continue;
			int words = Integer.parseInt(oneBestLineSplit[1]);			
			int oneBestMatch = Integer.parseInt(oneBestLineSplit[2]);			
			int oracleBestMatch = Integer.parseInt(oracleBestLineSplit[2]);
			if (oneBestMatch!=oracleBestMatch) {
				int improvement = oracleBestMatch - oneBestMatch;
				reportWriter.println(sentenceNumber + "\t" + words + "\t" + oneBestMatch + "\t" +
						oracleBestMatch + "\t" + improvement);
				TSNodeLabel goldTreeStructure = new TSNodeLabel(goldTree);
				TSNodeLabel oneBestTreeStructure = new TSNodeLabel(oneBestTree);
				TSNodeLabel oracleBestTreeStructure = new TSNodeLabel(oracleBestTree);
				//goldTreeStructure.replaceAllLabels("_C", "-ADV ");
				//oneBestTreeStructure.replaceAllLabels("_C", "-ADV ");
				//oracleBestTreeStructure.replaceAllLabels("_C", "-ADV ");				
				treeReportWriter.println(oneBestTreeStructure);
				treeReportWriter.println(oracleBestTreeStructure);
				treeReportWriter.println(goldTreeStructure);
			}
		}
		
		reportWriter.close();
		treeReportWriter.close();
	}
	
	public static void fscore() throws Exception {
		String workingDir = Parameters.resultsPath + "ChiarniakParser/wsj22/";		
		File goldFileSemTagOff = new File(workingDir + "wsj-22_clean_semTagOff.mrg");		
		File oneBestFile = new File(workingDir + "wsj-22_oneBest.mrg");
		File oracleBestFile = new File(workingDir + "wsj-22_oracleBest.mrg");
		File oneBestReportFile = new File(workingDir + "oneBest_evalTDS_dep.txt");
		File oracleBestReportFile = new File(workingDir + "oracleBest_evalTDS_dep.txt");
		
		File report = new File(workingDir + "oneBest_oracleBest_evalTDS_dep.txt");
		File treeReport = new File(workingDir + "oneBest_oracleBest_evalTDS_dep_trees.txt");
		
		PrintWriter reportWriter = FileUtil.getPrintWriter(report);
		PrintWriter treeReportWriter = FileUtil.getPrintWriter(treeReport);
		
		Scanner oneBestScanner = FileUtil.getScanner(oneBestReportFile);
		Scanner oracleBestScanner = FileUtil.getScanner(oracleBestReportFile);
		
		Scanner goldTreeScanner = FileUtil.getScanner(goldFileSemTagOff);
		Scanner oneBestTreeScanner = FileUtil.getScanner(oneBestFile);
		Scanner oracleBestTreeScanner = FileUtil.getScanner(oracleBestFile);
		reportWriter.println("#\twords\toneBestCorrect\toracleBestCorrect\timprovement");
		
		int sentenceNumber = -1;
		while(oneBestScanner.hasNextLine()) {
			sentenceNumber++;									
			String oneBestLine = oneBestScanner.nextLine();
			String oracleBestLine = oracleBestScanner.nextLine();
			if (sentenceNumber==0) continue;
			String goldTree = null;
			String oneBestTree = null;
			String oracleBestTree = null;
			if (oneBestTreeScanner.hasNext()) {
				goldTree = goldTreeScanner.nextLine();
				oneBestTree = oneBestTreeScanner.nextLine();
				oracleBestTree = oracleBestTreeScanner.nextLine();
			}
			String[] oneBestLineSplit = oneBestLine.split("\t");
			String[] oracleBestLineSplit = oracleBestLine.split("\t");
			if (oneBestLineSplit.length!=3 || oracleBestLineSplit.length!=3) continue;
			int words = Integer.parseInt(oneBestLineSplit[1]);			
			int oneBestMatch = Integer.parseInt(oneBestLineSplit[2]);			
			int oracleBestMatch = Integer.parseInt(oracleBestLineSplit[2]);
			//if (oneBestMatch!=oracleBestMatch) {
				int improvement = oracleBestMatch - oneBestMatch;
				reportWriter.println(sentenceNumber + "\t" + words + "\t" + oneBestMatch + "\t" +
						oracleBestMatch + "\t" + improvement);
				TSNodeLabel goldTreeStructure = new TSNodeLabel(goldTree);
				TSNodeLabel oneBestTreeStructure = new TSNodeLabel(oneBestTree);
				TSNodeLabel oracleBestTreeStructure = new TSNodeLabel(oracleBestTree);
				//goldTreeStructure.replaceAllLabels("_C", "-ADV ");
				//oneBestTreeStructure.replaceAllLabels("_C", "-ADV ");
				//oracleBestTreeStructure.replaceAllLabels("_C", "-ADV ");				
				treeReportWriter.println(oneBestTreeStructure);
				treeReportWriter.println(oracleBestTreeStructure);
				treeReportWriter.println(goldTreeStructure);
			//}
		}
		
		reportWriter.close();
		treeReportWriter.close();
	}
	
	public static String nextStructureXml(Scanner scan) {
		StringBuilder sb = new StringBuilder();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) break;
			sb.append(line).append("\n");
		}
		return sb.toString();
	}
	
	public static void printStructureNumberDifference(File fileOneXml, File fileTwoXml) {
		Scanner scanOne = FileUtil.getScanner(fileOneXml);
		Scanner scanTwo = FileUtil.getScanner(fileTwoXml);
		ArrayList<Integer> differingStructures = new ArrayList<Integer>();
		int i = 1;
		for(int l=0; l<4; l++) { // jump the first lines before the first structure
			scanOne.nextLine();
			scanTwo.nextLine();
		}
		while(scanOne.hasNext() && scanTwo.hasNextLine()) {			
			String nextOneStructure = nextStructureXml(scanOne);
			String nextTwoStructure = nextStructureXml(scanTwo);
			if (!nextOneStructure.equals(nextTwoStructure)) differingStructures.add(i);
			i++;
		}
		System.out.println(differingStructures.size() + ":" + differingStructures);		
	}
	
	public static void printStructureNumberDifference(File fileOneXml, File fileTwoXml, String regexRemoveInLines) {
		Scanner scanOne = FileUtil.getScanner(fileOneXml);
		Scanner scanTwo = FileUtil.getScanner(fileTwoXml);
		ArrayList<Integer> differingStructures = new ArrayList<Integer>();
		int i = 1;
		for(int l=0; l<4; l++) { // jump the first lines before the first structure
			scanOne.nextLine();
			scanTwo.nextLine();
		}
		while(scanOne.hasNext() && scanTwo.hasNextLine()) {			
			String nextOneStructure = nextStructureXml(scanOne);
			nextOneStructure = nextOneStructure.replaceAll(regexRemoveInLines, "");			
			String nextTwoStructure = nextStructureXml(scanTwo);
			nextTwoStructure = nextTwoStructure.replaceAll(regexRemoveInLines, "");
			if (!nextOneStructure.equals(nextTwoStructure)) differingStructures.add(i);
			i++;
		}
		System.out.println(differingStructures.size() + ":" + differingStructures);		
	}
	
	public static void printStructureNumberDifferenceIgnoreLines(File fileOneXml, File fileTwoXml, String regexIgnoreLines) {
		Scanner scanOne = FileUtil.getScanner(fileOneXml);
		Scanner scanTwo = FileUtil.getScanner(fileTwoXml);
		ArrayList<Integer> differingStructures = new ArrayList<Integer>();
		int i = 1;
		for(int l=0; l<4; l++) { // jump the first lines before the first structure
			scanOne.nextLine();
			scanTwo.nextLine();
		}
		while(scanOne.hasNext() && scanTwo.hasNextLine()) {
			String nextOneStructure = nextStructureXml(scanOne);
			String nextTwoStructure = nextStructureXml(scanTwo);
			String[] nextOneStructureArray = nextOneStructure.split("\n");
			String[] nextTwoStructureArray = nextTwoStructure.split("\n");
			ArrayList<String> newxOneStructureRelevant = new ArrayList<String>();
			ArrayList<String> newxTwoStructureRelevant = new ArrayList<String>();
			for(String s : nextOneStructureArray) {
				if (!s.matches(regexIgnoreLines)) newxOneStructureRelevant.add(s);
			}
			for(String s : nextTwoStructureArray) {
				if (!s.matches(regexIgnoreLines)) newxTwoStructureRelevant.add(s);
			}			
			if (!newxOneStructureRelevant.equals(newxTwoStructureRelevant)) differingStructures.add(i);
			i++;
		}
		System.out.println(differingStructures.size() + ":" + differingStructures);		
	}

	public static void countJunctionStructures() throws Exception {
		File inputFile = new File(Parameters.resultsPath + "TDS/Reranker/sec22_C/rerank_adv/wsj-22_reranked_1best.mrg");
		ArrayList<Box> treebank = Conversion.getTesniereTreebank(inputFile, HL);
		int count = 0;
		for(Box b : treebank) {
			count += b.countJunctionBlocks();
		}
		System.out.println(count);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//evalCoordination();
		//eval();
		//evalDep();
		//fscore();
		String dir = "Viewers/TDS/";
		File fileFeb17 = new File(dir + "Feb17/wsj-00_Feb17.xml");
		File fileFeb22 = new File(dir + "Feb22/wsj-00_Feb22.xml");
		File fileFeb25 = new File(dir + "Feb25/wsj-00_Feb25.xml");
		File fileMar01 = new File(dir + "Mar/wsj-00_Mar01.xml");
		File fileMar02 = new File(dir + "Mar/wsj-00_Mar02.xml");
		File fileMar04 = new File(dir + "wsj-00_Mar04.xml");		
		File fileApr08 = new File(dir + "wsj-00_Apr08.xml");
		File fileApr09 = new File(dir + "wsj-00_Apr09.xml");
		File fileApr10 = new File(dir + "wsj-00_Apr10.xml");
		File fileApr10a = new File(dir + "wsj-00_Apr10a.xml");
		File fileApr10b = new File(dir + "wsj-00_Apr10b.xml");
		File fileApr12 = new File(dir + "wsj-00_Apr12.xml");
		File fileApr12a = new File(dir + "wsj-00_Apr12a.xml");
		File fileApr12b = new File(dir + "wsj-00_Apr12b.xml");
		File fileApr13 = new File(dir + "wsj-00_Apr13.xml");
		File fileMay13 = new File(dir + "wsj-00_Original_May13.xml");
		File fileJune2 = new File(dir + "wsj-00_Original_Jun2.xml");
		File fileJune3 = new File(dir + "wsj-00_Original_Jun3.xml");
		File fileJune9 = new File(dir + "wsj-00_Original_Jun9.xml");
		File fileJune9a = new File(dir + "wsj-00_Original_Jun9a.xml");
		File fileJune9b = new File(dir + "wsj-00_Original_Jun9b.xml");
		File fileJune10 = new File(dir + "wsj-00_Original_Jun10.xml");
		File fileJune17 = new File(dir + "wsj-00_Original_Jun17.xml");
		File fileJune17a = new File(dir + "wsj-00_Original_Jun17a.xml");
		File fileJune17b = new File(dir + "wsj-00_Original_Jun17b.xml");
		File fileJune28 = new File(dir + "wsj-00_Original_Jun28.xml");
		File fileJune29 = new File(dir + "wsj-00_Original_Jun29.xml");
		File fileJune29b = new File(dir + "wsj-00_Original_Jun29b.xml");
		File fileJune29c = new File(dir + "wsj-00_Original_Jun29c.xml");
		File fileJune30 = new File(dir + "wsj-00_Original_Jun30.xml");
		File fileJune30a = new File(dir + "wsj-00_Original_Jun30a.xml");
		File fileJuly01 = new File(dir + "wsj-00_Original_Jul01.xml");
		File fileJuly04 = new File(dir + "wsj-00_Original_Jul04.xml");
		File fileJuly31 = new File(dir + "wsj-00_Original_Jul31.xml");
		File fileJuly31a = new File(dir + "wsj-00_Original_Jul31a.xml");
		File fileJuly31b = new File(dir + "wsj-00_Original_Jul31b.xml");
		File fileAug02 = new File(dir + "wsj-00_Original_Aug02.xml");
		File fileAug02a = new File(dir + "wsj-00_Original_Aug02a.xml");
		File fileAug02b = new File(dir + "wsj-00_Original_Aug02b.xml");
		File fileAug02c = new File(dir + "wsj-00_Original_Aug02c.xml");
		File fileAug02d = new File(dir + "wsj-00_Original_Aug02d.xml");
		File fileAug02e = new File(dir + "wsj-00_Original_Aug02e.xml");
		File fileAug02f = new File(dir + "wsj-00_Original_Aug02f.xml");
		File fileAug02g = new File(dir + "wsj-00_Original_Aug02g.xml");
		File fileAug02h = new File(dir + "wsj-00_Original_Aug02h.xml");
		File fileAug02i = new File(dir + "wsj-00_Original_Aug02i.xml");
		File fileAug02l = new File(dir + "wsj-00_Original_Aug02l.xml");
		File fileAug03 = new File(dir + "wsj-00_Original_Aug03.xml");
		File fileAug03a = new File(dir + "wsj-00_Original_Aug03a.xml");
		File fileAug03b = new File(dir + "wsj-00_Original_Aug03b.xml");
		File fileAug03b_np = new File(dir + "wsj-00_Original_Aug03b_np.xml");
		File fileAug04 = new File(dir + "wsj-00_Original_Aug04.xml");
		File fileAug04_np = new File(dir + "wsj-00_Original_Aug04_np.xml");
		File fileAug04b_np = new File(dir + "wsj-00_Original_Aug04b_np.xml");
		File fileAug04c_np = new File(dir + "wsj-00_Original_Aug04c_np.xml");
		File fileAug04d_np = new File(dir + "wsj-00_Original_Aug04d_np.xml");
		File fileAug04e_np = new File(dir + "wsj-00_Original_Aug04e_np.xml");
		File fileAug04f_np = new File(dir + "wsj-00_Original_Aug04f_np.xml");
		File fileAug04g_np = new File(dir + "wsj-00_Original_Aug04g_np.xml");
		File fileAug04h_np = new File(dir + "wsj-00_Original_Aug04h_np.xml");
		File fileAug04i_np = new File(dir + "wsj-00_Original_Aug04i_np.xml");		
		File fileAug05_np = new File(dir + "wsj-00_Original_Aug05_np.xml");
		File fileAug05a_np = new File(dir + "wsj-00_Original_Aug05a_np.xml");
		File fileAug05b_np = new File(dir + "wsj-00_Original_Aug05b_np.xml");
		File fileAug05c_np = new File(dir + "wsj-00_Original_Aug05c_np.xml");
		File fileAug05d_np = new File(dir + "wsj-00_Original_Aug05d_np.xml");		
		File fileAug05 = new File(dir + "wsj-00_Original_Aug05.xml");
		File fileAug06_np = new File(dir + "wsj-00_Original_Aug06_np.xml");
		File fileAug06a_np = new File(dir + "wsj-00_Original_Aug06a_np.xml");
		File fileAug06b_np = new File(dir + "wsj-00_Original_Aug06b_np.xml");
		File fileAug06c_np = new File(dir + "wsj-00_Original_Aug06c_np.xml");
		File fileAug06d_np = new File(dir + "wsj-00_Original_Aug06d_np.xml");
		File fileAug06e_np = new File(dir + "wsj-00_Original_Aug06e_np.xml");		
		File fileAug06g_np = new File(dir + "wsj-00_Original_Aug06g_np.xml");
		File fileAug06h_np = new File(dir + "wsj-00_Original_Aug06h_np.xml");
		File fileAug06i_np = new File(dir + "wsj-00_Original_Aug06i_np.xml");
		File fileAug06j_np = new File(dir + "wsj-00_Original_Aug06j_np.xml");
		File fileAug06k_np = new File(dir + "wsj-00_Original_Aug06k_np.xml");
		File fileAug06l_np = new File(dir + "wsj-00_Original_Aug06l_np.xml");
		File fileAug06m_np = new File(dir + "wsj-00_Original_Aug06m_np.xml");
		File fileAug10_np = new File(dir + "wsj-00_Original_Aug10_np.xml");
		
		//printStructureNumberDifference(fileAug02h, fileAug02i, "PosLabel=\".*\"");
		//printStructureNumberDifference(fileAug05, fileAug05d_np, "OrigNodeLabel=\".*\"");		
		//printStructureNumberDifferenceIgnoreLines(fileAug06j_np, fileAug06k_np, 
		//		"^\\t+</*PunctWords>.*$|^\\t+</*EmptyWord.*$|^\\t+</*ConjunctionWord.*$");
		printStructureNumberDifference(fileAug06m_np, fileAug10_np);
		
		//countJunctionStructures();
	}

}
