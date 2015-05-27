package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;

import settings.Parameters;
import util.IdentityArrayList;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class SortTableIndexes {
	
	int min=Integer.MAX_VALUE, max=-1;
	HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> sourceTargetIndex;
	HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, Double>> sourceTargetScore;
	HashMap<IdentityArrayList<String>, IdentityArrayList<String>> bestMatches;
	
	public SortTableIndexes(File inputTableFile, 
			File sentSourceFile, File sentTargetFile, File outputFile) throws FileNotFoundException {	
		int totalSentences=0, sourcePhrases=0;
		
		sourceTargetIndex = ParallelSubstrings.readTableFromFile(inputTableFile);
		sourceTargetScore = readTableScoreFromFile(inputTableFile);
		
		Vector<String[]> sentSource = new Vector<String[]>(
				ParallelSubstrings.getInternedSentences(sentSourceFile));
		Vector<String[]> sentTarget = new Vector<String[]>(
				ParallelSubstrings.getInternedSentences(sentTargetFile));
		
		getMinMax();
		PrintWriter pw = new PrintWriter(outputFile);
		for(int i=min; i<=max; i++) {
			getSubTable(i);
			if (!bestMatches.isEmpty()) {
				totalSentences++;
				sourcePhrases+=bestMatches.size();
				pw.println(i + "\t" + Utility.arrayToString(sentSource.get(i-1), ' ') 
						+ "\t\t" + Utility.arrayToString(sentTarget.get(i-1), ' '));
				for (Entry<IdentityArrayList<String>, IdentityArrayList<String>> e : bestMatches.entrySet()) {
					pw.println("\t" + e.getKey().toString(' ') + "\t" + "-" 
							+ "\t" + e.getValue().toString(' ') + "\t" + "-");
				}
				pw.println();
			}
		}
		pw.close();
		System.out.println("Sentences with MWEs: " + totalSentences);
		System.out.println("Total source MWEs: " + sourcePhrases);
	}
	
	private void getSubTable(int i) {
		bestMatches = new HashMap<IdentityArrayList<String>, IdentityArrayList<String>>();
		for(Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> e : sourceTargetIndex.entrySet()) {
			IdentityArrayList<String> sourcePhrase = e.getKey();
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> value = e.getValue();
			double bestScore = -1;
			IdentityArrayList<String> bestTarget = null; 
			for(Entry<IdentityArrayList<String>, TreeSet<Integer>> f : value.entrySet()) {
				IdentityArrayList<String> targetPhrase = f.getKey();
				TreeSet<Integer> subValue = f.getValue();
				if (subValue.contains(i)) {
					double score = sourceTargetScore.get(sourcePhrase).get(targetPhrase);
					if (score>bestScore) {
						bestScore = score;
						bestTarget = targetPhrase;
					}
				}
			}
			if (bestTarget!=null) {				
				bestMatches.put(sourcePhrase, bestTarget);
			}
		}
		if (bestMatches.isEmpty())
			return;
		//eliminate overlapping
		Iterator<Entry<IdentityArrayList<String>, IdentityArrayList<String>>> iter1 = bestMatches.entrySet().iterator();
		Iterator<Entry<IdentityArrayList<String>, IdentityArrayList<String>>> iter2 = null;
		Entry<IdentityArrayList<String>, IdentityArrayList<String>> e1=null, e2=null;
		IdentityArrayList<String> source1 = null, source2 = null;
		boolean[] remove = new boolean[bestMatches.size()];
		int i1=-1, i2=-1;
		outer:
		while(iter1.hasNext()) {
			e1 = iter1.next();
			source1 = e1.getKey();
			iter2 = bestMatches.entrySet().iterator();
			if (remove[++i1])
				continue;
			i2=-1;
			do {				
				e2 = iter2.next();	
				source2 = e2.getKey();
				i2++;
			} while(source2!=source1);
			while(iter2.hasNext()) {
				e2 = iter2.next();
				source2 = e2.getKey();
				if (remove[++i2])
					continue;
				IdentityArrayList<String> value1 = e1.getValue();
				IdentityArrayList<String> value2 = e2.getValue();
				if (conflictingPhrases(source1, source2) || conflictingPhrases(value1, value2)) {
					double s1 =  sourceTargetScore.get(source1).get(value1);
					double s2 =  sourceTargetScore.get(source2).get(value2);
					if (s1>=s2) {
						remove[i2]=true;
						continue;
					}
					remove[i1]=true;
					continue outer;
				}
			}
		}
		iter1 = bestMatches.entrySet().iterator();
		i1=-1;
		while(iter1.hasNext()) {
			iter1.next();
			if (remove[++i1])
				iter1.remove();
		}
		
	}

	private boolean conflictingPhrases(IdentityArrayList<String> s1, IdentityArrayList<String> s2) {
		int l1 = s1.size();
		int l2 = s2.size();
		return (l1<=l2) ? hasContiguous(s2, s1) : hasContiguous(s1, s2);
	}
	
	public static boolean hasContiguous(IdentityArrayList<String> l,  IdentityArrayList<String> s) {
		int lastIndex = l.size()-s.size();
		for(int i=0; i<=lastIndex; i++) {
			if (hasContiguous(l, s, i)) {				
				return true;
			}				
		}
		return false;
	}
	
	private static boolean hasContiguous(IdentityArrayList<String> l,
			IdentityArrayList<String> s, int i) {
		Iterator<String> iterShort = s.iterator();
		Iterator<String> iterLong = l.listIterator(i);
		while(iterShort.hasNext()) {
			if (iterLong.next()!=iterShort.next())
				return false;
		}
		return true;
	}

	private void getMinMax() {	
		for(HashMap<IdentityArrayList<String>, TreeSet<Integer>> v : sourceTargetIndex.values()) {
			for(TreeSet<Integer> s : v.values()) {
				int first = s.first();
				int last = s.last();
				if (min>first)
					min = first;
				if (max<last)
					max = last;
			}
		}
		
	}

	public static HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,Double>> readTableScoreFromFile(File inputFile) {

		HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,Double>> result = 
				new HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,Double>> ();

		PrintProgress pp = new PrintProgress("Reading table", 10000, 0);
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		IdentityArrayList<String> key=null, value=null;
		HashMap<IdentityArrayList<String>, Double> subTable = null;
		int[] indexes = null;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //new implementation
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] // new implementation only has these lines
				key =  ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				subTable = result.get(key);
				if (subTable==null) {
					subTable = new HashMap<IdentityArrayList<String>, Double>();
					result.put(key, subTable);
				}
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			value = ParallelSubstrings.getIdentityArrayListFromBracket(split[2]);
			//indexes = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);			
			Double score = Double.parseDouble(split[4]);					
			subTable.put(value, score);
		}
		pp.end();
		return result;
	}



	public static void main(String[] args) throws FileNotFoundException {
		
		String path = "/Users/fedja/Dropbox/ted_experiment/en_it/kernels_all/";
		File in = new File(path + "kernels.table.m2.prune.align.prune.indexes_189302_190830.gz");
		File out = new File(path + "kernels.table.m2.prune.align.prune.indexes_189302_190830_sorted.txt");
		
		String pathCorpus = "/Users/fedja/Dropbox/ted_experiment/corpus_en_it/";
		File source = new File(pathCorpus + "IWSLT14.TED.tst2010.en-it.tok.lc.en");
		File target = new File(pathCorpus + "IWSLT14.TED.tst2010.en-it.tok.lc.it");
		//train.tags.en-it.clean.tok.lc.en
		//train.tags.en-it.clean.tok.lc.it
		new SortTableIndexes(in, source, target, out);
		
	}
}
