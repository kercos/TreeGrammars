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

public class PruneTable_Align_New {
	
	File systemLogFile;
	File inputTableFile;
	File outputFile;
	int[] pairsIndexes, pairsIndexesAligned;
	
	Vector<HashMap<Integer, HashSet<Integer>>> alignmentTables; 
	// every word index in source is mapped to all aligned word indexes in target sentence
	Vector<String[]> sourceSentences, targetSentences;

	public PruneTable_Align_New(File sourceFile, File targetFile,
			File alignFile, File inputTableFile) throws FileNotFoundException {
		
		this.inputTableFile = inputTableFile;
		outputFile =  FileUtil.changeExtension(inputTableFile, "prune.align_new.gz");;
		systemLogFile = FileUtil.changeExtension(inputTableFile, "prune.align_new.log");
		//this.alignMethod = alignMethod;		

		pairsIndexes = new int[2];
		pairsIndexesAligned = new int[2];
		
		Parameters.openLogFile(systemLogFile);
		Parameters.logStdOutPrintln("Split Table With Giza Alignment");
		Parameters.logStdOutPrintln("Source Sentences File: " + sourceFile);
		Parameters.logStdOutPrintln("Target Sentences File: " + targetFile);
		Parameters.logStdOutPrintln("Kernel Table File: " + inputTableFile);
		Parameters.logStdOutPrintln("Alignment File: " + alignFile);
		Parameters.logStdOutPrintln("Output File Giza: " + outputFile);
		//Parameters.logStdOutPrintln("Align Method: " + alignMethod);
		
		Parameters.logStdOutPrint("Reading source sentences: " );
		sourceSentences = readInternSentence(sourceFile);
		Parameters.logStdOutPrintln(""+sourceSentences.size());
		Parameters.logStdOutPrint("Reading target sentences: " );
		targetSentences = readInternSentence(targetFile);
		Parameters.logStdOutPrintln(""+targetSentences.size());
				
		readAlignemntFile(alignFile);
		
		pruneTable();
		
		Parameters.logStdOutPrintln("Total phrase-pairs/indexes " + Arrays.toString(pairsIndexes));
		Parameters.logStdOutPrintln("Total phrase-pairs/indexes aligned " + Arrays.toString(pairsIndexesAligned));
		
		
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


	public void pruneTable() {
		
		Scanner scan = FileUtil.getGzipScanner(inputTableFile);
		PrintWriter pw = FileUtil.getGzipPrintWriter(outputFile);

		PrintProgress pp = new PrintProgress("Pruning table", 1000, 0);
		String[] sourcePhrase=null, targetPhrase=null;
		boolean printedKey = false;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) { //2:
				pw.println(line);
				continue; 
			}				 
			if (split.length==2) { //\t[check, for]				 
				sourcePhrase = ParallelSubstrings.getInternedStringArrayFromBracket(split[1]);
				printedKey = false;
				continue;
			}
			// split.length==4 //\t\t[che, per, la]\t[23687, 34596, 186687]
			pp.next();
			targetPhrase = ParallelSubstrings.getInternedStringArrayFromBracket(split[2]);
			int[] indexSet = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);
			int[] alignedIndexes = getAlignedIndexes(sourcePhrase, targetPhrase, indexSet);
			
			pairsIndexes[0]++;
			pairsIndexes[1]+=indexSet.length;
			
			if (alignedIndexes.length>1) {
				pairsIndexesAligned[0]++;
				if (!printedKey) {
					pw.println("\t" + Arrays.toString(sourcePhrase));
					printedKey = true;
				}
				pw.println("\t\t" + Arrays.toString(targetPhrase) + "\t" + Arrays.toString(alignedIndexes));
			}
		}
		
		pp.end();
		
		pw.close();
	}

	private int[] getAlignedIndexes(
			String[] sourcePhrase,
			String[] targetPhrase,
			int[] indexSet) {
		
		ArrayList<Integer> indexGiza = new ArrayList<Integer>();
		for(int index : indexSet) {
			String[] sentenceSource = sourceSentences.get(index);			
			String[] sentenceTarget = targetSentences.get(index);
			HashMap<Integer, HashSet<Integer>> alTable = alignmentTables.get(index);
			boolean aligned = areAligned(sentenceSource, sentenceTarget, sourcePhrase, targetPhrase, alTable);
					
			//1: aligned
			//0: not aligned
			//-1: skip index (multiple matches)
			if (aligned) {
				indexGiza.add(index);
				pairsIndexesAligned[1]++;
			}
		}		
		return Utility.arrayListIntegerToArray(indexGiza);
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
	public static boolean areAligned(String[] sentenceSource, String[] sentenceTarget, 
			String[] sourcePhrase, String[] targetPhrase, 
			HashMap<Integer, HashSet<Integer>> alignTable) {	
		
		boolean[] sCovered = new boolean[sourcePhrase.length];
		boolean[] tCovered = new boolean[targetPhrase.length];
		
		int[] sourceStarts = getStarts(sourcePhrase, sentenceSource);
		
		//accept only one mactch in source and target
		if (sourceStarts.length==0) {
			// punctuation issues
			//System.err.println("Error in source, phrase not found! ");
			return false;
		}		
		
		if (sourceStarts.length>1)
			return false;
		int[] targetStarts = getStarts(targetPhrase, sentenceTarget);
		if (targetStarts.length>1)
			return false;
		
		if (targetStarts.length==0) {
			//System.err.println("Error in target, phrase not found! ");
			// punctuation issues
			return false;
		}
		
		int s_start = sourceStarts[0];
		int s_end = s_start + sourcePhrase.length;
		int t_start = targetStarts[0];
		int t_end = t_start + targetPhrase.length;
		
		HashMap<Integer, HashSet<Integer>> reversedAlignTable = Utility.reverseHashMap(alignTable);
		
		int sourcePos = 0;
		for(int indexSource = s_start; indexSource<s_end; indexSource++) {
			HashSet<Integer> targetPeerIndexes = alignTable.get(indexSource);			
			if (targetPeerIndexes != null) {								
				if (targetPeerIndexes.size()==1) {
					Integer peer = targetPeerIndexes.iterator().next();
					if (peer>=t_start && peer<t_end && reversedAlignTable.get(peer).size()==1) {
						return false; //1-1 relations
					}
				}
				for (int i : targetPeerIndexes) {
					if (i >= t_start && i < t_end) {
						sCovered[sourcePos]=true;
						tCovered[i-t_start]=true;
					}
				}
			}
			sourcePos++;
		}	
		
		return Utility.someTrue(sCovered) && Utility.someTrue(tCovered);
		
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
		
		new PruneTable_Align_New(sourceFile, targetFile, alignFile, inputTable);
		
		
		/*
		String[] sentence = ParallelSubstrings.getInternedStringArrayFromBracket("[and, there, was, something, incredibly, profound, about, sitting, down, with, my, closest, friends, and, telling, them, what, they, meant, to, me, .]");
		String[] phrase = ParallelSubstrings.getInternedStringArrayFromBracket("[something, incredibly]");
		int[] sourceStarts = getStarts(phrase, sentence);
		System.out.println(Arrays.toString(sourceStarts));
		*/
	}
}
