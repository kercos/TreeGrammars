package kernels.parallel.ted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import kernels.parallel.CheckGaps;
import kernels.parallel.ParallelSubstrings;
import util.IdentityArrayList;
import util.file.FileUtil;

public class MWE_Stats {

	private static void getStats(File mweFile, File corpusFile) throws FileNotFoundException {
		File outputStatFile = FileUtil.changeExtension(mweFile, ".stats.txt");
		File outputGoldFile = FileUtil.changeExtension(mweFile, ".gold.ge2.txt");
		int[] freqCount = new int[2];
		int freqCountGe1 = 0;
		ArrayList<String[]> corpusSentences = ParallelSubstrings.getInternedSentences(corpusFile, true);
		Scanner scan = new Scanner(mweFile);
		PrintWriter pw = new PrintWriter(outputStatFile);
		PrintWriter pwGold = new PrintWriter(outputGoldFile);
		int totLines = 0;
		while(scan.hasNextLine()) {
			totLines++;
			String mwe = scan.nextLine().split("\t")[0];
			IdentityArrayList<String> mweSplit = ParallelSubstrings.getIdentityArrayList(mwe, "\\s+");
			int freq = getFreq(mweSplit, corpusSentences);
			if (freq<2) {
				freqCount[freq]++;
			}
			else {
				freqCountGe1++;
				pwGold.println(mwe);
			}
			pw.println(mwe + "\t" + freq);
		}
		pw.close();
		pwGold.close();
		
		System.out.println("TotLines " + totLines);
		System.out.println("FreqCount =0: " + freqCount[0]);
		System.out.println("FreqCount =1: " + freqCount[1]);
		System.out.println("FreqCount >1: " + freqCountGe1);
	}
	
	private static int getFreq(IdentityArrayList<String> mweSplit,
			ArrayList<String[]> corpusSentences) {
		int result = 0;
		for(String[] snt : corpusSentences) {
			if (CheckGaps.checkPresence(snt, mweSplit) && !CheckGaps.hasGap(snt, mweSplit)) {
				result++;
			}				
		}
		return result;
	}

	public static void main(String[] args) throws FileNotFoundException {
		
		/*
		args = new String[]{
			"/Users/fedja/Dropbox/ted_experiment/annotation/Aggregated_2015_05_30.cont.mwe.txt",
			"/Users/fedja/Dropbox/ted_experiment/corpus_en_it/all.en-it.clean.tok.lc.en"
		};
		*/
		
		File mweFile = new File(args[0]);
		File corpusFile = new File(args[1]);
		getStats(mweFile,corpusFile);
	}

	

}
