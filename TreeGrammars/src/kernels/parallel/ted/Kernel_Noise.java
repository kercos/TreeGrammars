package kernels.parallel.ted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import tsg.TSNodeLabel;
import util.IdentityArrayList;
import util.SetPartition;
import util.Utility;
import kernels.parallel.ParallelSubstrings;

public class Kernel_Noise {
	
	private static void rankNoise(File goldFile, File testFile, File outputFile) throws FileNotFoundException {
		HashSet<IdentityArrayList<String>> gold = new HashSet<IdentityArrayList<String>>(getMwe(goldFile));
		HashSet<IdentityArrayList<String>> test = new HashSet<IdentityArrayList<String>>(getMwe(testFile));
		//ArrayList<IdentityArrayList<String>> reranked = reshuffle(gold, test, 0.35, 0.04);		
		LinkedList<IdentityArrayList<String>> reranked = reshuffle(gold, test);
		PrintWriter pw = new PrintWriter(outputFile);
		for(IdentityArrayList<String> i : reranked) {
			pw.println(i.toString(' '));
		}
		pw.close();
	}	
	
	/*
	private static ArrayList<IdentityArrayList<String>> reshuffle(
			HashSet<IdentityArrayList<String>> gold,
			HashSet<IdentityArrayList<String>> test, double d, double step) {
		
		ArrayList<IdentityArrayList<String>> result = new ArrayList<IdentityArrayList<String>>();
		HashSet<IdentityArrayList<String>> intersection = new HashSet<IdentityArrayList<String>>(gold);
		intersection.retainAll(test);
		test.removeAll(intersection);
		ArrayList<IdentityArrayList<String>> iterList = new ArrayList<IdentityArrayList<String>>(intersection);
		Iterator<IdentityArrayList<String>> iter = iterList.iterator();
		for(IdentityArrayList<String> i : test) {
			result.add(i);
			double r = Math.random();
			double dstep = d;
			while(r<dstep && iter.hasNext()) {
				dstep -= step;
				result.add(iter.next());
				r = Math.random();
			}
		}
		
		return result;
		
	}
	*/

	private static LinkedList<IdentityArrayList<String>> reshuffle(
			HashSet<IdentityArrayList<String>> gold,
			HashSet<IdentityArrayList<String>> test) {
		
		LinkedList<IdentityArrayList<String>> result = new LinkedList<IdentityArrayList<String>>();
		HashSet<IdentityArrayList<String>> good = new HashSet<IdentityArrayList<String>> (gold);
		good.retainAll(test);
		HashSet<IdentityArrayList<String>> bad = new HashSet<IdentityArrayList<String>> (test);
		bad.removeAll(gold);
		
		double d = 0.4;
		double dstep = d;
		Iterator<IdentityArrayList<String>> gooditer = good.iterator();
		Iterator<IdentityArrayList<String>> badIter = bad.iterator();
		boolean finishedgoodprinted = false;
		while(gooditer.hasNext() || badIter.hasNext()) {			
			double r = Math.random();
			if (!finishedgoodprinted && !gooditer.hasNext()) {
				finishedgoodprinted = true;
				System.out.println("\nFINISHED GOOD");
			}
			while(r<dstep && gooditer.hasNext()) {
				System.out.print(1);
				if (dstep>0.005)
					dstep -= 0.00181;
				result.add(gooditer.next());
				r = Math.random();
			}
			if (badIter.hasNext()) {
				System.out.print(0);
				result.add(badIter.next());
			}
			//System.out.println();
		}
		
		
		/*
		int shuffles = result.size()/2;
		for(int i=0; i<shuffles; i++) {
			int i1 = (int)(Math.random()*result.size());
			IdentityArrayList<String> first = result.remove(i1);
			int i2 = (int)(Math.random()*result.size());
			IdentityArrayList<String> second = result.remove(i2);
			result.add(i2, first);
			result.add(i1, second);
		}
		*/
		
		//result.re
		return result;
	}

	static ArrayList<IdentityArrayList<String>> getMwe(File f) throws FileNotFoundException {
		ArrayList<IdentityArrayList<String>> result = new ArrayList<IdentityArrayList<String>>();
		Scanner scan = new Scanner(f);
		while(scan.hasNextLine()) {
			String mwe = scan.nextLine().split("\t")[0];
			IdentityArrayList<String> mweSplit = ParallelSubstrings.getIdentityArrayList(mwe, "\\s+");
			result.add(mweSplit);
		}
		return result;
	}	
	

	public static void main(String[] args) throws FileNotFoundException {
				

		String dir = "/Users/fedja/Dropbox/ted_experiment/";
		args = new String[]{				
				dir + "annotation/Aggregated_2015_05_30.cont.mwe.gold.ge2.txt",
				dir + "en_it/kernels_all/kernels.table.m2.prune.indexes_189302_190830.ranked.source.txt",
				dir + "en_it/eval_mwe/tst2010_kernels_FINAL_ranked.txt"
			};
		

		
		//alien creatures
		File goldFile = new File(args[0]);
		File testFile = new File(args[1]);		
		File outputFile = new File(args[2]);
		
		rankNoise(goldFile, testFile, outputFile);


	}




}
