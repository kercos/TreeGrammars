package tsg.incremental;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

import util.LongestCommonSubsequence;
import util.Utility;
import util.file.FileUtil;

public class EvaluateWordPrediction {

	private static void evaluateWordPrediction(File origFile, File predFile) {
		Scanner scanOrig = FileUtil.getScanner(origFile);
		Scanner scanPred = FileUtil.getScanner(predFile);
		int[] next1WordPredictionResults = new int[3]; //correct,totalGold,totalTest
		int[] next2WordsPredictionResults = new int[3];
		int[] next3WordsPredictionResults = new int[3];
		int[] next4WordsPredictionResults = new int[3];
		int[] next1WordPresenceResults = new int[3];
		int[] next2WordsPresenceResults = new int[3];
		int[] next3WordsPresenceResults = new int[3];
		int[] next4WordsPresenceResults = new int[3];
		int[] lcsPredicted = new int[3];		
		while(scanOrig.hasNextLine()) {
			String sentence = scanOrig.nextLine();
			String[] words = sentence.split("\\s+");
			int length = words.length;
			for(int i=1; i<=length; i++) {
				String predLine = scanPred.nextLine();
				if (i==length) //last line always identical to the whole string --> no evaluation
					break;
				String[] predLineWords = predLine.split("\\s+");
				int predLineLength = predLineWords.length;
				String[] goldSuffix = Arrays.copyOfRange(words, i, length);
				String[] predSuffix = predLineLength == i ? new String[]{} : 
						Arrays.copyOfRange(predLineWords, i, predLineLength);
				computeNextWordPrediction(goldSuffix, predSuffix, next1WordPredictionResults);
				computeNextXWordsPrediction(goldSuffix, predSuffix, next2WordsPredictionResults, 2);
				computeNextXWordsPrediction(goldSuffix, predSuffix, next3WordsPredictionResults, 3);
				computeNextXWordsPrediction(goldSuffix, predSuffix, next4WordsPredictionResults, 4);
				computeNextWordPresence(goldSuffix, predSuffix, next1WordPresenceResults);
				computeNextXWordsPresence(goldSuffix, predSuffix, next2WordsPresenceResults, 2);
				computeNextXWordsPresence(goldSuffix, predSuffix, next3WordsPresenceResults, 3);
				computeNextXWordsPresence(goldSuffix, predSuffix, next4WordsPresenceResults, 4);
				computeLcsPredicted(goldSuffix, predSuffix, lcsPredicted);
			}
		}
		System.out.println("Next 1 word prediction [correct,totalGold,totalTest] recall precision): " + 
				reportResultRecallPrecision(next1WordPredictionResults));
		System.out.println("Next 2 words prediction [correct,totalGold,totalTest] recall precision): " + 
				reportResultRecallPrecision(next2WordsPredictionResults));
		System.out.println("Next 3 words prediction [correct,totalGold,totalTest] recall precision): " + 
				reportResultRecallPrecision(next3WordsPredictionResults));
		System.out.println("Next 4 words prediction [correct,totalGold,totalTest] recall precision): " + 
				reportResultRecallPrecision(next4WordsPredictionResults));

		System.out.println("Next 1 word presence [correct,totalGold,totalTest] recall precision): " +
				reportResultRecallPrecision(next1WordPresenceResults));
		System.out.println("Next 2 words presence [correct,totalGold,totalTest] recall precision): " +
				reportResultRecallPrecision(next2WordsPresenceResults));
		System.out.println("Next 3 words presence [correct,totalGold,totalTest] recall precision): " +
				reportResultRecallPrecision(next3WordsPresenceResults));
		System.out.println("Next 4 words presence [correct,totalGold,totalTest] recall precision): " +
				reportResultRecallPrecision(next4WordsPresenceResults));

		System.out.println("Total words predicted in longest common subsequence: " +
			"[lcsCorrect, totalGold, totalTest] percentage: " +
			reportResultRecallPrecision(lcsPredicted));
			//Arrays.toString(lcsPredicted) + " " + 
			//Utility.makePercentage(lcsPredicted[0], lcsPredicted[1]));

	}
	


	/**
	 * 
	 * @param result: [correct,totalGold,totalTest]
	 * @return
	 */
	private static String reportResultRecallPrecision(int[] result) {
		return 
			Arrays.toString(result) + " " +
			Utility.makePercentage(result[0], result[1]) + " " +
			Utility.makePercentage(result[0], result[2]);
	}


	private static void computeNextWordPrediction(String[] goldSuffix,
			String[] predSuffix, int[] nextWordPredResults) {
		nextWordPredResults[1]++;		
		if (predSuffix.length>0) {
			nextWordPredResults[2]++;
			if (predSuffix[0].equals(goldSuffix[0]))
				nextWordPredResults[0]++;
		}		
	}
	
	private static void computeNextXWordsPrediction(String[] goldSuffix,
			String[] predSuffix, int[] nextWordPredResults, int x) {
		int lengthGold = goldSuffix.length; 
		if (lengthGold<x)
			return;
		nextWordPredResults[1]++;		
		if (predSuffix.length>=x) {
			nextWordPredResults[2]++;
			for(int i=0; i<x; i++) {
				if (!predSuffix[i].equals(goldSuffix[i]))
					return;
			}
			nextWordPredResults[0]++;
		}		
	}
	
	private static void computeNextWordPresence(String[] goldSuffix,
			String[] predSuffix, int[] nextPredictedWordPresent) {
		nextPredictedWordPresent[1]++;		
		if (predSuffix.length>0) {
			nextPredictedWordPresent[2]++;
			String nextPredWord = predSuffix[0];
			for(String w : goldSuffix) {
				if (nextPredWord.equals(w)) {
					nextPredictedWordPresent[0]++;
					break;
				}
			}			
		}		
	}
	
	private static void computeNextXWordsPresence(String[] goldSuffix,
			String[] predSuffix, int[] nextPredictedWordPresent, int x) {
		int lengthGold = goldSuffix.length; 
		if (lengthGold<x)
			return;
		nextPredictedWordPresent[1]++;
		if (predSuffix.length>=x) {
			nextPredictedWordPresent[2]++;
			boolean allPresent = true;
			int indexGold = 0;
			for(int indexTest = 0; indexTest<x; indexTest++) {
				String nextPredWord = predSuffix[indexTest];
				boolean wordPresent = false;
				for(; indexGold<lengthGold; indexGold++) {					
					if (nextPredWord.equals(goldSuffix[indexGold])) {
						wordPresent = true;
						break;
					}					
				}
				if (!wordPresent) {
					allPresent = false;
					break;
				}
			}
			if (allPresent)
				nextPredictedWordPresent[0]++;
		}		
	}

	private static void computeLcsPredicted(String[] goldSuffix,
			String[] predSuffix, int[] lcsPredicted) {
		int lcs = LongestCommonSubsequence.getLCSLength(goldSuffix, predSuffix);
		lcsPredicted[0] += lcs;
		lcsPredicted[1] += goldSuffix.length;
		lcsPredicted[2] += predSuffix.length;
	}
	

	public static void main(String[] args) {
		String workPath = "/Users/fedja/Work/Edinburgh/PLTSG/WordPrediction/";
		File origFlatFile = new File(workPath + "MB_ROARK_Right_H0_V1_UkM4_UkT4_notop/wsj-23_flat.txt");
		File pltsgWP = new File(workPath + "PLTSG_wordPrediction/wsj-23_predictions_noSmoothing.txt");
		File srilmWP = new File(workPath + "SRILM_wordPrediction/wsj-23_predictions.txt");
		System.out.println("\nEval ITSG");
		evaluateWordPrediction(origFlatFile, pltsgWP);
		System.out.println("\nEval SRILM");
		evaluateWordPrediction(origFlatFile, srilmWP);
	}

	
	
}
