package kernels.parallel.ted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import tsg.TSNodeLabel;
import util.IdentityArrayList;
import util.SetPartition;
import util.Utility;
import kernels.parallel.ParallelSubstrings;

public class Kernel_PMI {
	
	private static void rankPMI(File trainFile, File fullTableFile, File selectedKeyFile, File outputFile) throws FileNotFoundException {
		
		
		ArrayList<String[]> trainCorpus = ParallelSubstrings.getInternedSentences(trainFile, true);				
		IdentityHashMap<String, int[]> uniFreqTable = getUnigramFreq(trainCorpus);
		//HashMap<IdentityArrayList<String>, int[]> biFreqTable = getBigramsFreq(trainCorpus);
		
		HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> fullTable 
			= ParallelSubstrings.readTableFromFile(fullTableFile, true);		
		
		HashMap<IdentityArrayList<String>, Double> selectedKeyCorpus = 
				getInternedSentencesFirstField(selectedKeyFile, fullTable);
		
		
		HashMap<IdentityArrayList<String>, Double> mpiTable = getMPI(selectedKeyCorpus, uniFreqTable);
		
		//Set<IdentityArrayList<String>> testBigramSet = getBigramsFreq(testCorpus).keySet();
		//mpiTable.keySet().retainAll(testBigramSet);
				
		printRankedTestBigrams(outputFile, mpiTable);
		
	}	
	
	
	public static HashMap<IdentityArrayList<String>, Double> getInternedSentencesFirstField(File inputFile, 
			HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> testTable) throws FileNotFoundException {
		
		HashMap<IdentityArrayList<String>, Double> result = new HashMap<IdentityArrayList<String>, Double>();
		TreeSet<Integer> allIndexes = new TreeSet<Integer>();
		for(HashMap<IdentityArrayList<String>, TreeSet<Integer>> e : testTable.values()) {
			allIndexes.addAll(ParallelSubstrings.getAllIndexes(e));
		}
		int allIndexesSize = allIndexes.size();
		
		Scanner scanner = new Scanner(inputFile, "utf-8");
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine().trim().split("\t")[0];
			line = line.toLowerCase(); 				
			IdentityArrayList<String> mwe = ParallelSubstrings.getIdentityArrayList(line, "\\s+");
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = testTable.get(mwe);
			double selectedSize = 1;
			if (subTable!=null) {
				//System.out.println("MWE not found: " + mwe);
				//return null;
				selectedSize = ParallelSubstrings.getAllIndexes(subTable).size();
			}			
			Double prob = selectedSize / allIndexesSize;
			result.put(mwe, prob);
		}		
		return result;
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


	private static HashMap<IdentityArrayList<String>, Double> getMPI(
			HashMap<IdentityArrayList<String>, Double> selectedKeyCorpus,
			IdentityHashMap<String, int[]> uniFreqTable) {
		
		HashMap<IdentityArrayList<String>, Double> result =
				new HashMap<IdentityArrayList<String>, Double>();
		
		int totUniFreq = Utility.totalSumValues(uniFreqTable);
		
		for(Entry<IdentityArrayList<String>, Double> e : selectedKeyCorpus.entrySet()) {
			double d = 1;
			IdentityArrayList<String> mwe = e.getKey();
			for(String w : mwe) {
				double pw = uniFreqTable.get(w)[0];
				pw /= totUniFreq;
				d *= pw;
			}
			double n = e.getValue();
			result.put(mwe, n/d);
		}
		
		return result;
	}

	public static void main(String[] args) throws FileNotFoundException {
				
		/*
		String dir = "~/Dropbox/ted_experiment/";
		args = new String[]{				
				dir + "corpus_en_it/all.en-it.clean.tok.lc.en",
				dir + "en_it/kernels_all/kernels.table.m2.gz",
				dir + "en_it/kernels_all/kernels.table.m2.prune.indexes_189302_190830.ranked.source.txt",
				dir + "en_it/eval_mwe/tst2010_kernels_PMI_ranked.txt",
			};
		
		*/
		
		//alien creatures
		File trainFile = new File(args[0]);
		File fullTableFile = new File(args[1]);
		File selectedKeyFile = new File(args[2]);
		File outputFile = new File(args[3]);
		
		
		rankPMI(trainFile, fullTableFile, selectedKeyFile, outputFile);


	}


}
