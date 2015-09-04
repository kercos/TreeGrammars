package kernels.parallel.ted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import util.IdentityArrayList;
import util.file.FileUtil;
import kernels.parallel.ParallelSubstrings;

public class Eval_MWE_Extraction {

	static void checkCoverage(File goldMwe, File testMwe) throws FileNotFoundException {
		HashSet<IdentityArrayList<String>> goldMweSet = new HashSet<IdentityArrayList<String>>(getMwe(goldMwe));
		HashSet<IdentityArrayList<String>> testMweSet = new HashSet<IdentityArrayList<String>>(getMwe(testMwe));
		System.out.println("Gold MWE Set size: " + goldMweSet.size());
		System.out.println("Test MWE Set size: " + testMweSet.size());
		goldMweSet.retainAll(testMweSet);
		System.out.println("Coverage: " + goldMweSet.size());
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
	
	static int maxK = 20000; //476
	
	static void getPRFatK(File goldFile, File testFile) throws FileNotFoundException {		
		ArrayList<IdentityArrayList<String>> goldList = getMwe(goldFile);
		ArrayList<IdentityArrayList<String>> testList = getMwe(testFile);
		File evalFile = FileUtil.changeExtension(testFile, ".eval");
		PrintWriter pw = new PrintWriter(evalFile);
		for(int k=1; k<=Math.min(maxK,testList.size()); k++) {
			HashSet<IdentityArrayList<String>> testSubList = 
					new HashSet<IdentityArrayList<String>>(testList.subList(0, k));
			testSubList.retainAll(goldList);
			double match = testSubList.size();
			double precision = match/k;
			double recall = match/goldList.size();
			double fscore = precision==0 || recall==0 ? 0 :
				2 * precision  * recall / (precision + recall);
			pw.println(k + "\t" + precision + "\t" + recall + "\t" + fscore);
		}
		pw.close();
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		
		/*
		String evalDir = "/Users/fedja/Dropbox/ted_experiment/en_it/eval_mwe/";
		
		
		args = new String[]{
			"/Users/fedja/Dropbox/ted_experiment/annotation/Aggregated_2015_05_30.cont.mwe.gold.ge2.txt",
			evalDir +
			//"tst2010_bigram_mpi_ranked.txt"
			//"tst2010_kernels_prob_ranked.txt"
			//"tst2010_kernels_alignnew_prob_ranked.txt"
			"tst2010_kernels_FINAL_ranked.txt"
		};
		*/
		
		
		File goldMwe = new File(args[0]);
		File testMwe = new File(args[1]);
		//checkCoverage(goldMwe, testMwe);
		getPRFatK(goldMwe, testMwe);
	}

}
