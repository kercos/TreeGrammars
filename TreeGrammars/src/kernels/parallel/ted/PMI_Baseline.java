package kernels.parallel.ted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import util.IdentityArrayList;
import util.Utility;
import kernels.parallel.ParallelSubstrings;

public class PMI_Baseline {
	
	private static void rankPMI(File trainFile, File testFile, File outputFile) throws FileNotFoundException {
		ArrayList<String[]> trainCorpus = ParallelSubstrings.getInternedSentences(trainFile, true);
		IdentityHashMap<String, int[]> uniFreqTable = getUnigramFreq(trainCorpus);
		HashMap<IdentityArrayList<String>, int[]> biFreqTable = getBigramsFreq(trainCorpus);
		HashMap<IdentityArrayList<String>, Double> mpiTable = getMPI(biFreqTable, uniFreqTable);
		
		ArrayList<String[]> testCorpus = ParallelSubstrings.getInternedSentences(testFile, true);
		Set<IdentityArrayList<String>> testBigramSet = getBigramsFreq(testCorpus).keySet();
		mpiTable.keySet().retainAll(testBigramSet);
				
		printRankedTestBigrams(outputFile, mpiTable);
		
	}	

	private static void printRankedTestBigrams(File outputFile, 
			HashMap<IdentityArrayList<String>, Double> mpiTable) throws FileNotFoundException {
		
		TreeMap<Double, HashSet<IdentityArrayList<String>>> reversedTable = 
				Utility.reverseAndSortTable(mpiTable);
		PrintWriter pw = new PrintWriter(outputFile);
		for(Entry<Double, HashSet<IdentityArrayList<String>>> e : reversedTable.descendingMap().entrySet()) {
			Double value = e.getKey();
			for(IdentityArrayList<String> bigram : e.getValue()) {
				pw.println(bigram.toString(' ') + "\t" + value);
			}
		}
		pw.close();
		
	}

	private static IdentityHashMap<String, int[]> getUnigramFreq(ArrayList<String[]> trainCorpus) {
		IdentityHashMap<String, int[]> result = new IdentityHashMap<String, int[]>();
		for(String[] sentence : trainCorpus) {
			for(String w : sentence) {
				Utility.increaseInHashMap(result, w);
			}
		}
		return result;
		
	}

	private static HashMap<IdentityArrayList<String>, int[]> getBigramsFreq(ArrayList<String[]> trainCorpus) {
		HashMap<IdentityArrayList<String>, int[]> result = new HashMap<IdentityArrayList<String>, int[]>();
		for(String[] sentence : trainCorpus) {
			for(int i=0; i<sentence.length-1; i++) {
				String w1 = sentence[i];
				String w2 = sentence[i+1];
				IdentityArrayList<String> a = new IdentityArrayList<String>(new String[]{w1,w2});
				Utility.increaseInHashMap(result, a);
			}
		}		
		return result;
		
	}

	private static HashMap<IdentityArrayList<String>, Double> getMPI(
			HashMap<IdentityArrayList<String>, int[]> biFreqTable,
			IdentityHashMap<String, int[]> uniFreqTable) {
		
		HashMap<IdentityArrayList<String>, Double> result =
				new HashMap<IdentityArrayList<String>, Double>();
		
		double totBiFreq = Utility.totalSumValues(biFreqTable);
		double totUniFreq = Utility.totalSumValues(uniFreqTable);
		for(Entry<IdentityArrayList<String>, int[]> e : biFreqTable.entrySet()) {
			IdentityArrayList<String> a = e.getKey();
			double biFreq = e.getValue()[0];
			if (biFreq<2) {
				continue;
			}
			double num = biFreq/totBiFreq;
			double den = 1d;
			for(String w : a) {
				double fw = uniFreqTable.get(w)[0];
				double pw = fw / totUniFreq;
				den *= pw;
			}
			Double pmi = Math.log(num/den);
			result.put(a, pmi);
		}
		return result;
	}

	public static void main(String[] args) throws FileNotFoundException {
		
		/*
		String dir = "/Users/fedja/Dropbox/";
		args = new String[]{
				dir + "annotation/Aggregated_2015_05_30.cont.sent.txt",
				dir + "corpus_en_it/Users/fedja/Dropbox/ted_experiment/en_it/kernels_all/kernels.table.m2.prune.indexes._189302_190830.ranked.source.txt"
				"/Users/fedja/Dropbox/ted_experiment/en_it/kernels_all/kernels.table.m2.prune.align.prune.indexes._189302_190830.ranked.source.txt"
			};
		*/
			
		//alien creatures
		File trainFile = new File(args[0]);
		File testFile = new File(args[1]);
		File outputFile = new File(args[2]);
		rankPMI(trainFile, testFile, outputFile);


	}


}
