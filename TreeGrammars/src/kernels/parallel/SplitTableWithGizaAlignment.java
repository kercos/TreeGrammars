package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class SplitTableWithGizaAlignment {
	
	File systemLogFile;
	File inputTableFile;
	File outputFileGiza, outputFileNoGiza;
	boolean allCoveredConstraint;
	int[] pairsIndexes, pairsIndexesAligned, pairsIndexesNoAligned, pairsIndexesSkip;
	
	Vector<HashMap<Integer, HashSet<Integer>>> alignmentTables; 
	// every word index in source is mapped to all aligned word indexes in target sentence
	Vector<String[]> sourceSentences, targetSentences;

	public SplitTableWithGizaAlignment(File sourceFile, File targetFile,
			File alignFile, File inputTableFile, boolean allCoveredConstraint) throws FileNotFoundException {
		
		this.inputTableFile = inputTableFile;
		outputFileGiza =  FileUtil.changeExtension(inputTableFile, "align.giza.gz");;
		outputFileNoGiza =  FileUtil.changeExtension(inputTableFile, "align.nogiza.gz");;
		systemLogFile = FileUtil.changeExtension(inputTableFile, "align.log");
		this.allCoveredConstraint = allCoveredConstraint;		

		pairsIndexes = new int[2];
		pairsIndexesAligned = new int[2];
		pairsIndexesNoAligned = new int[2];
		pairsIndexesSkip = new int[2];
		
		Parameters.openLogFile(systemLogFile);
		Parameters.logStdOutPrintln("Split Table With Giza Alignment");
		Parameters.logStdOutPrintln("Source Sentences File: " + sourceFile);
		Parameters.logStdOutPrintln("Target Sentences File: " + targetFile);
		Parameters.logStdOutPrintln("Kernel Table File: " + inputTableFile);
		Parameters.logStdOutPrintln("Alignment File: " + alignFile);
		Parameters.logStdOutPrintln("Output File Giza: " + outputFileGiza);
		Parameters.logStdOutPrintln("Output File No Giza: " + outputFileNoGiza);
		Parameters.logStdOutPrintln("All covered constraint: " + allCoveredConstraint);
		
		Parameters.logStdOutPrint("Reading source sentences: " );
		sourceSentences = readInternSentence(sourceFile);
		Parameters.logStdOutPrintln(""+sourceSentences.size());
		Parameters.logStdOutPrint("Reading target sentences: " );
		targetSentences = readInternSentence(targetFile);
		Parameters.logStdOutPrintln(""+targetSentences.size());
				
		readAlignemntFile(alignFile);
		
		splitTable();
		
		Parameters.logStdOutPrintln("Total phrase-pairs/indexes " + Arrays.toString(pairsIndexes));
		Parameters.logStdOutPrintln("Total phrase-pairs/indexes aligned " + Arrays.toString(pairsIndexesAligned));
		Parameters.logStdOutPrintln("Total phrase-pairs/indexes not aligned " + Arrays.toString(pairsIndexesNoAligned));
		Parameters.logStdOutPrintln("Total phrase-pairs/indexes skipped (multiple matches) " + Arrays.toString(pairsIndexesSkip));
		
		
		Parameters.closeLogFile();
	}

	private Vector<String[]> readInternSentence(File inputFile) {
		Vector<String[]> result = new Vector<String[]>();
		Scanner scan = FileUtil.getScanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] words = line.split(" ");
			for(int i=0; i<words.length; i++) {
				words[i] = words[i].trim().intern();
			}
			result.add(words);
		}
		return result;
	}
	
	private void readAlignemntFile(File alignementFile) throws FileNotFoundException {
		Parameters.logStdOutPrint("Reading alignment indexes: " );
		alignmentTables = getAlignemntTableFromFile(alignementFile);
		Parameters.logStdOutPrintln(""+alignmentTables.size());
	}
	
	public static Vector<HashMap<Integer, HashSet<Integer>>> getAlignemntTableFromFile(File alignementFile) throws FileNotFoundException {
		Vector<HashMap<Integer, HashSet<Integer>>> result = new Vector<HashMap<Integer, HashSet<Integer>>> ();
		Scanner scan = new Scanner(alignementFile);
		while (scan.hasNextLine()) {
			HashMap<Integer, HashSet<Integer>> table = new HashMap<Integer, HashSet<Integer>>();
			result.add(table);
			String line = scan.nextLine();
			String[] split = line.split("\\s");
			for (String s : split) {
				String[] twoIndexes = s.split("-");
				int indexSource = Integer.parseInt(twoIndexes[0]);
				int indexTarget = Integer.parseInt(twoIndexes[1]);
				Utility.putInHashMap(table, indexSource, indexTarget);
			}			
		}
		scan.close();
		return result;
	}


	public void splitTable() {
		
		Scanner scan = FileUtil.getGzipScanner(inputTableFile);
		PrintWriter pwGiza = FileUtil.getGzipPrintWriter(outputFileGiza);
		PrintWriter pwNoGiza = FileUtil.getGzipPrintWriter(outputFileNoGiza);

		PrintWriter[] pwGizaNoGiza = new PrintWriter[]{pwGiza, pwNoGiza};
		PrintProgress pp = new PrintProgress("Splitting table", 1000, 0);
		String[] sourcePhrase=null, targetPhrase=null;
		boolean writtenSource[] = new  boolean[2];
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) { //2:
				pwGiza.println(line);
				pwNoGiza.println(line);
				continue; 
			}				 
			if (split.length==2) { //\t[check, for]				 
				sourcePhrase = ParallelSubstrings.getInternedStringArrayFromBracket(split[1]);
				Arrays.fill(writtenSource,false);
				continue;
			}
			// split.length==4 //\t\t[che, per, la]\t[23687, 34596, 186687]
			pp.next();
			targetPhrase = ParallelSubstrings.getInternedStringArrayFromBracket(split[2]);
			int[] indexSet = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);
			int[][] gizaNoGiza = splitSetGizaNoGiza(sourcePhrase, targetPhrase, indexSet);
			
			int gizaLength = gizaNoGiza[0].length;
			int noGizaLength = gizaNoGiza[1].length;
			int sum = gizaLength + noGizaLength;
			
			pairsIndexes[0]++;
			pairsIndexes[1]+=indexSet.length;
			
			if (sum!=indexSet.length)
				pairsIndexesSkip[0]++;
			if (gizaLength>0)
				pairsIndexesAligned[0]++;
			if (noGizaLength>0)
				pairsIndexesNoAligned[0]++;
			
			for(int i=0; i<2; i++) {
				int[] set = gizaNoGiza[i];
				if (set.length==0)
					continue;
				PrintWriter pw = pwGizaNoGiza[i];
				if (!writtenSource[i]) {
					pw.println("\t" + Arrays.toString(sourcePhrase));
					writtenSource[i] = true;
				}
				pw.println("\t\t" + Arrays.toString(targetPhrase) + "\t" + Arrays.toString(set));				
			}
		}
		
		pp.end();
		
		pwGiza.close();
		pwNoGiza.close();
	}

	private int[][] splitSetGizaNoGiza(
			String[] sourcePhrase,
			String[] targetPhrase,
			int[] indexSet) {
		
		ArrayList<Integer> indexGiza = new ArrayList<Integer>();
		ArrayList<Integer> indexNoGiza = new ArrayList<Integer>();
		for(int index : indexSet) {
			String[] sentenceSource = sourceSentences.get(index);			
			String[] sentenceTarget = targetSentences.get(index);
			HashMap<Integer, HashSet<Integer>> alTable = alignmentTables.get(index);
			int aligned =
					allCoveredConstraint ?
					areAllAligned(sentenceSource, sentenceTarget, sourcePhrase, targetPhrase, alTable) :
					areSomeButNotAllAligned(sentenceSource, sentenceTarget, sourcePhrase, targetPhrase, alTable);
			//1: aligned
			//0: not aligned
			//-1: skip index (multiple matches)
			if (aligned==1) {
				indexGiza.add(index);
				pairsIndexesAligned[1]++;
			}
			else if (aligned==0) {
				indexNoGiza.add(index);
				pairsIndexesNoAligned[1]++;
			}
			else {
				pairsIndexesSkip[1]++;
			}
		}		
		return new int[][]{Utility.arrayListIntegerToArray(indexGiza), 
				Utility.arrayListIntegerToArray(indexNoGiza)};
	}

	/**
	 * 
	 * @param sentenceSource
	 * @param sentenceTarget
	 * @param sourcePhrase
	 * @param targetPhrase
	 * @param alignTable
	 * @return
	 * 1: aligned
	 * 0: not aligned
	 * -1: skip sentence (multiple matches) 
	 */
	public static int areAllAligned(String[] sentenceSource, String[] sentenceTarget, 
			String[] sourcePhrase, String[] targetPhrase, 
			HashMap<Integer, HashSet<Integer>> alignTable) {	
		
		boolean[] sCovered = new boolean[sourcePhrase.length];
		boolean[] tCovered = new boolean[targetPhrase.length];
		
		if (setAlignmentCoverage(sentenceSource, sentenceTarget, sourcePhrase, targetPhrase, alignTable,
				sCovered, tCovered)) {
			return Utility.allTrue(sCovered) && Utility.allTrue(tCovered) ? 1 : 0;
		}
		else return -1;
		
	}
	
	/**
	 * 
	 * @param sentenceSource
	 * @param sentenceTarget
	 * @param sourcePhrase
	 * @param targetPhrase
	 * @param alignTable
	 * @return
	 * 1: aligned
	 * 0: not aligned
	 * -1: skip sentence (multiple matches) 
	 */
	public static int areSomeButNotAllAligned(String[] sentenceSource, String[] sentenceTarget, 
			String[] sourcePhrase, String[] targetPhrase, 
			HashMap<Integer, HashSet<Integer>> alignTable) {	
		
		boolean[] sCovered = new boolean[sourcePhrase.length];
		boolean[] tCovered = new boolean[targetPhrase.length];
		
		if (setAlignmentCoverage(sentenceSource, sentenceTarget, sourcePhrase, targetPhrase, alignTable,
				sCovered, tCovered)) {
			return Utility.someButNotAllTrue(sCovered) && Utility.allTrue(tCovered) ? 1 : 0;
		}
		else return -1;
		
	}
	
	public static boolean setAlignmentCoverage(String[] sentenceSource, String[] sentenceTarget, 
			String[] sourcePhrase, String[] targetPhrase, 
			HashMap<Integer, HashSet<Integer>> alignTable, boolean[] sCovered, boolean[] tCovered) {		
		
		int[] sourceStarts = getStarts(sourcePhrase, sentenceSource);
		
		//accept only one mactch in source and target
		if (sourceStarts.length>1)
			return false;
		int[] targetStarts = getStarts(targetPhrase, sentenceTarget);
		if (targetStarts.length>1)
			return false;
		
		int s_start = sourceStarts[0];
		int s_end = s_start + sourcePhrase.length;
		int t_start = targetStarts[0];
		int t_end = t_start + targetPhrase.length;
		
		int sourcePos = 0;
		for(int indexSource = s_start; indexSource<s_end; indexSource++) {
			HashSet<Integer> set = alignTable.get(indexSource);
			if (set != null) {				
				for (int i : set) {
					if (i >= t_start && i < t_end) {
						sCovered[sourcePos]=true;
						tCovered[i-t_start]=true;
					}
				}
			}
			sourcePos++;
		}		
		
		return true;
		
	}

	
	
	private static int[] getStarts(String[] phrase, String[] sentence) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		outer: for(int i=0; i<sentence.length; i++) {
			if (sentence[i] != phrase[0])
				continue;
			if (sentence.length < (i + phrase.length))
				continue;
			int j=1;
			for(int p=i+1; p<i+phrase.length; p++) {
				if (sentence[p]!=phrase[j++])
					continue outer;
			}
			result.add(i);
		}
		return Utility.arrayListIntegerToArray(result);
	}
	
	



	public static void main(String[] args) throws FileNotFoundException {
				
		/*
		String workingDir = "/Users/fedja/Dropbox/ted_experiment/";		
		args = new String[]{
			workingDir + "corpus_en_it/train.tags.en-it.clean.tok.lc.en",
			workingDir + "corpus_en_it/train.tags.en-it.clean.tok.lc.it",
			workingDir + "en_it/model/aligned.grow-diag-final-and",
			workingDir + "en_it/kernels/kernels.table.m2.prune.threshold.0.8.gz",
		};
		*/
				
		
		File sourceFile = new File(args[0]);
		File targetFile = new File(args[1]);
		File alignFile = new File(args[2]);
		File inputTable = new File(args[3]);
		boolean allCoveredConstraint = ArgumentReader.readBooleanOption(args[4]);
		
		new SplitTableWithGizaAlignment(sourceFile, targetFile, alignFile, inputTable, allCoveredConstraint);
		
		
		/*
		String[] sentence = ParallelSubstrings.getInternedStringArrayFromBracket("[and, there, was, something, incredibly, profound, about, sitting, down, with, my, closest, friends, and, telling, them, what, they, meant, to, me, .]");
		String[] phrase = ParallelSubstrings.getInternedStringArrayFromBracket("[something, incredibly]");
		int[] sourceStarts = getStarts(phrase, sentence);
		System.out.println(Arrays.toString(sourceStarts));
		*/
	}
}
