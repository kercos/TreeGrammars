package unsupervised;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import settings.Parameters;
import tsg.corpora.Wsj;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class BilexFreqWsj {

	public static File wsj10Flat = new File(Wsj.Wsj10 + "wsj-Flat.mrg");
	public static File bilexFreqFile = new File(Parameters.corpusPath + "Web_1T_English/WSJ/BilexFreq/bilexFreqWsjDistance.txt");
	public static File vocabFreq = new File(Parameters.corpusPath + "Web_1T_English/WSJ/1gms/vocab.txt");
	
	public static HashMap<String, Long> getWordFreq() {
		HashMap<String, Long> result = new HashMap<String, Long>();
		Scanner scan = FileUtil.getScanner(vocabFreq);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] wordFreq = line.split("\t");
			result.put(wordFreq[0], Long.parseLong(wordFreq[1]));
		}
		System.out.println("Read vocabulary file");
		return result;
	}
	
	public static String mergeStringArray(String[] a) {
		String result = a[0];
		for(int i=1; i<a.length; i++) {
			result += "\t" + a[i];
		}
		return result;
	}
	
	public static ArrayList<Integer> machedIndexes(String[] array, String key) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for(int i=0; i<array.length; i++) {
			String item = array[i];
			if (item.equals(key)) result.add(i);
		}
		return result;
	}
	
	public static void main(String[] args) {
		
		HashMap<String, Long> wordsFreq = getWordFreq();
		
		Scanner scan = FileUtil.getScanner(wsj10Flat);
		File outputFile = new File(Wsj.Wsj10 + "wsj-BilexFreq.mrg");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		PrintProgressStatic.start("Processing Sentences:");
		
		while (scan.hasNextLine()) {
			String sentence = scan.nextLine();
			String[] words = sentence.split("\\s+");
			int sentenceLength = words.length;
			String[] wordsSorted = Arrays.copyOf(words, sentenceLength);
			Arrays.sort(wordsSorted);			
			
			long[][][] chart = new long[sentenceLength][sentenceLength][5];
			for(int i =0; i<sentenceLength; i++) {				
				String word = words[i];
				Long freq = wordsFreq.get(word);
				if (freq==null) freq = new Long(0);				
				chart[i][i][2] = freq;				
			}
			
			Scanner bilexScan = FileUtil.getScanner(bilexFreqFile);
			//int lineIndex = 0;
			while (bilexScan.hasNextLine()) {
				//lineIndex++;
				String line = bilexScan.nextLine();
				String[] bilexFreq = line.split("\t");
				String bilex = bilexFreq[0];
				String[] w1w2 = bilex.split("\\s+");
				String w1 = w1w2[0];
				String w2 = w1w2[1];
				if (Arrays.binarySearch(wordsSorted, w1)>=0 && Arrays.binarySearch(wordsSorted, w2)>=0) {
					ArrayList<Integer> machedIndexesW1 = machedIndexes(words, w1);
					ArrayList<Integer> machedIndexesW2 = machedIndexes(words, w2);
					for(int i : machedIndexesW1) {
						for(int j : machedIndexesW2) {
							if (i>=j) continue;
							for(int k=0; k<5; k++) {
								chart[i][j][k] = Long.parseLong(bilexFreq[k+1]);
							}
							//chart[i][j] = "(" + bilexFreq[1] + "," + bilexFreq[2] + "," + bilexFreq[3] + "," + bilexFreq[4] + "," + bilexFreq[5] + ")";
						}
					}
				}
			}
			
			pw.println("\t" + mergeStringArray(words));
			
			for(int row = 0; row<sentenceLength*5; row++) {
				int i = row/5;
				int k = row%5;
				if (k==2) pw.print(words[i] + "\t");
				else pw.print("\t");
				for(int j =0; j<sentenceLength; j++) {
					if (j<i || (j==i && k!=2)) pw.print("\t");					
					else pw.print(chart[i][j][k] + "\t");
				}
				pw.println();
			}
			bilexScan.close();
			pw.println();
			pw.flush();
			PrintProgressStatic.next();			
		}
		
		pw.close();
		
	}
	
}
