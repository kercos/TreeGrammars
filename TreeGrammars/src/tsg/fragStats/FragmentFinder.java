package tsg.fragStats;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.file.FileUtil;

public class FragmentFinder {
	
	public static void findInTUT() throws Exception {
		File treebankFile = new File(Parameters.corpusPath + "Evalita09/Treebanks/Constituency/" +
		"TUTinPENN-train.readable.notraces.quotesFixed.noSemTags.penn");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile, "UTF-8",Integer.MAX_VALUE);
		TSNodeLabel target = new TSNodeLabel("(VMA~RE mette)"); 
		findInTreebankAndPrint(treebank, target,100);
	}

	
	public static void findInWSJ(TSNodeLabel target) throws Exception {
		File treebankFile = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-02-21.mrg");//"wsj-complete.mrg");		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		//TSNodeLabel target = new TSNodeLabel("(VP * (S NP-SBJ NP-PRD))", false); 
		findInTreebankAndPrint(treebank, target, Integer.MAX_VALUE);
	}
	
	public static int countInTreebank(File treebankFile, TSNodeLabel frag) throws Exception {
		Scanner scan = FileUtil.getScanner(treebankFile);
		int count = 0;
		while(scan.hasNextLine()) {
			TSNodeLabel tree = new TSNodeLabel(scan.nextLine());
			if (tree.containsRecursiveFragment(frag))
				count++;
		}
		return count;
	}
	
	public static void findInTreebankAndPrint(ArrayList<TSNodeLabel> treebank, TSNodeLabel target, int hits) {
		int count = 0;
		for(TSNodeLabel t : treebank) {
			if (t.containsRecursiveFragment(target)) {
				System.out.println(t);
				System.out.println("\t"+t.toFlatSentence());
				count++;
				if (count==hits) break;
			}
		}
	}
	
	public static ArrayList<TSNodeLabel> findInTreebank(ArrayList<TSNodeLabel> treebank, TSNodeLabel target) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		for(TSNodeLabel t : treebank) {
			if (t.containsRecursiveFragment(target)) {
				result.add(t);
			}
		}
		return result;
	}
	
	public static void findYieldInTreebankAndPrint(ArrayList<TSNodeLabel> treebank, HashSet<Label> labs) {
		for(TSNodeLabel t : treebank) {
			ArrayList<Label> terms = t.collectLexicalLabels();
			if (terms.containsAll(labs)) System.out.println(t);
		}		
	}
	
	public static ArrayList<TSNodeLabel> findAdjacentLexInTreebank(ArrayList<TSNodeLabel> treebank, String words) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		words = " " + words + " ";
		for(TSNodeLabel t : treebank) {
			String lex = " " + t.toFlatSentence() + " ";			
			if (lex.contains(words)) result.add(t);			
		}
		return result;
	}
	
	public static void findShortestInTreebankAndPrint(ArrayList<TSNodeLabel> treebank, TSNodeLabel target) {
		int min = Integer.MAX_VALUE;
		TSNodeLabel result = null;
		for(TSNodeLabel t : treebank) {
			if (t.containsRecursiveFragment(target)) {
				int length = t.countLexicalNodes();
				if (length<min) {
					min = length;
					result = t;
				}
			}
		}
		if (result!=null) {
			System.out.println(result);
		}
	}


	public static void findInFragment(TSNodeLabel target) throws Exception {
		String dir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/KenelFragments/SemTagOff_Top/all/";
		File fragmentsFile = new File(dir + "fragments_MUB_freq_all.txt");
		Scanner scan = FileUtil.getScanner(fragmentsFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			String fragmentString  = line.split("\t")[0];
			TSNodeLabel fragment = new TSNodeLabel(fragmentString, false);
			if (fragment.containsRecursiveFragment(target)) System.out.println(line);
		}		
	}
	
	public static void findPartialFragment(int index, String containsWord) throws Exception {
		File treebankFile = new File(Wsj.WsjOriginalCleanedTop + "wsj-02-21.mrg");
		File partialFragmentFile = new File(Parameters.resultsPath + 
				"TSG/TSGkernels/Wsj/KenelFragments/SemTagOn/subBranch/20/correct/" +
				"fragments_subBranch_MUB_freq_20_sorted_" + containsWord + "_correctCount.txt");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		TSNodeLabel tree = treebank.get(index);
		System.out.println(tree + "\n");
		Scanner scan = FileUtil.getScanner(partialFragmentFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			String[] fragmentFreq = line.split("\t");
			String fragmentString  = fragmentFreq[0];
			TSNodeLabel fragment = new TSNodeLabel(fragmentString, false);
			if (tree.containsRecursivePartialFragment(fragment)) {
				//if (line.indexOf(containsWord)==-1) continue;
				System.out.println(line);
			}
		}				
	}
	
	public static void filterFragmentsPresentInTree(File fragmentFile, TSNodeLabel tree,
			File outputFile) throws Exception {
		Scanner scan = FileUtil.getScanner(fragmentFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String fragString = line.split("\t")[0];
			TSNodeLabel frag = new TSNodeLabel(fragString, false);
			if (tree.containsRecursiveFragment(frag))
				pw.println(line);
		}
		pw.close();
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//File treebankFile = new File(Wsj.WsjOriginalCleanedTop + "wsj-02-21.mrg");//"wsj-complete.mrg");
		//File treebankFile = new File(Wsj.WsjOriginalReadable + "wsj-02-21.mrg");//"wsj-complete.mrg");
		//ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		 
		//findInTreebankAndPrint(treebank,
		//		new TSNodeLabel("(VP (VBD \"gave\") NP)", false),
		//		Integer.MAX_VALUE);
		//System.out.println("---------------");
		
		//ArrayList<TSNodeLabel> woTrees = findInTreebank(treebank,
		//		new TSNodeLabel(Label.getLabel("wo"), true));
		/*
		ArrayList<TSNodeLabel> woTrees = findAdjacentLexInTreebank(treebank,
		"wo");
		ArrayList<TSNodeLabel> wontTrees = findAdjacentLexInTreebank(treebank,
				"wo n't");
		System.out.println(woTrees.size());
		System.out.println(wontTrees.size());
		woTrees.removeAll(wontTrees);		
		//System.out.println(woTrees);		
		for(TSNodeLabel t : woTrees) {
			System.out.println(t);
		}*/
		
		/*
		String path = System.getProperty("user.home") + "/PLTSG/WSJ_RightBin_H0V1_UK4/";
		File treebankFile = new File(path + "wsj-02-21.mrg");
		TSNodeLabel frag = new TSNodeLabel("(VP ADVP (VP@ VBD))", false);		
		int count = countInTreebank(treebankFile, frag);
		System.out.println(count);
		*/
		
		String path = "/disk/scratch/fsangati/PLTSG/ToyCorpus3/";
		File treeFile = new File(path+"trainTB.mrg");
		TSNodeLabel tree = new TSNodeLabel(FileUtil.getScanner(treeFile).nextLine());
		File fragsFileFirstLex = new File(path + "firstLexFrags.mrg");
		File fragsFileFirstLexFiltered = new File(path + "firstLexFragsFiltered.mrg");
		filterFragmentsPresentInTree(fragsFileFirstLex, tree, fragsFileFirstLexFiltered);
		File fragsFileFirstSub = new File(path + "firstSubFrags.mrg");
		File fragsFileFirstSubFiltered = new File(path + "firstSubFragsFiltered.mrg");
		filterFragmentsPresentInTree(fragsFileFirstSub, tree, fragsFileFirstSubFiltered);

	}


	

}
