package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;

import kernels.algo.CommonSubstring;
import settings.Parameters;
import util.ArgumentReader;
import util.IdentityArrayList;

public class ParallelSubstringsWPL extends ParallelSubstrings{
	
	protected static String getVersion() { return "0.02"; }
	
	ArrayList<String[][]> sentencesSouceWPL, sentencesTargetWPL;
	File sourceFileWPL, targetFileWPL;	

	public ParallelSubstringsWPL(File sourceFile, File sourceFileWPL, 
			File targetFile, File targetFileWPL, File outputFile, int minMatchSize)  {		
		
		super(sourceFile, targetFile, outputFile, minMatchSize);
		
		this.sourceFileWPL = sourceFileWPL;
		this.targetFileWPL = targetFileWPL;
		sentencesSouceWPL = new ArrayList<String[][]>();
		sentencesTargetWPL = new ArrayList<String[][]>();
		
	}
	
	protected void printParameters() {
		Parameters.logStdOutPrintln("Source File WPL: " + sourceFileWPL);
		Parameters.logStdOutPrintln("Target File WPL " + targetFileWPL);		
		super.printParameters();		
	}
	
	protected boolean getSentences() throws FileNotFoundException {
		super.getSentences();
		sentencesSouceWPL = getSentencesWPL(sentencesSource, sourceFileWPL);
		if (sentencesSouceWPL==null) {
			System.err.println("Problem in Source Files");
			return false;
		}	
		sentencesTargetWPL = getSentencesWPL(sentencesTarget, targetFileWPL);
		if (sentencesTargetWPL==null) {
			System.err.println("Problem in Target Files");
			return false;
		}
		return true;
	}

	private ArrayList<String[][]> getSentencesWPL(ArrayList<String[]> sentences, File fileWPL) throws FileNotFoundException {
		ArrayList<String[][]> result = new ArrayList<String[][]>();
		Iterator<String[]> iter = sentences.iterator();
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(fileWPL, "utf-8");
		int sentenceCounter=0;
		int wordCounter=0;
		while(iter.hasNext()) {
			sentenceCounter++;
			String[] sentenceWords = iter.next();
			int length = sentenceWords.length;
			String[][] sentenceWPL = new String[length][];			
			for(int i=0; i<length; i++) {
				wordCounter++;
				String line = scanner.nextLine();
				line = line.toLowerCase();
				String[] lineSplit = line.split("\t"); //word, pos, lemma
				lineSplit[1] = "<" + lineSplit[1] + ">"; //pos
				lineSplit[2] = "|" + lineSplit[2] + "|"; //lemma
				for(int j=0; j<3; j++) {
					lineSplit[j] = lineSplit[j].intern();
					//INTERNING WORD, POS, and LEMMAS
				}
				if (lineSplit[0] != sentenceWords[i]) {
					System.err.println("Original file and tagged files differ in words");
					System.err.println("At sentence in original file: " + sentenceCounter);
					System.err.println("At word in WPL file: " + wordCounter);
					return null;
				}
				sentenceWPL[i] = lineSplit;
			}
			result.add(sentenceWPL);
		}				
		return result;
	}


	protected void computePairwiseMatch()  {
				
		ListIterator<String[][]> sourceIter1 = sentencesSouceWPL.listIterator();
		ListIterator<String[][]> targetIter1 = sentencesTargetWPL.listIterator();
		
		int index1 = -1, index2=0;
		long count = 0;
		while(sourceIter1.hasNext()) {			
			index1++;
			index2 = index1 + 1;
			String[][] source1 = sourceIter1.next();
			String[][] target1 = targetIter1.next();			
			ListIterator<String[][]> sourceIter2 = sentencesSouceWPL.listIterator(index2);
			ListIterator<String[][]> targetIter2 = sentencesTargetWPL.listIterator(index2);
			while (sourceIter2.hasNext()) {
				progress.next();
				if (++count == reportEvery) {
					count = 0;
					progress.suspend();
					Parameters.logStdOutPrintln("Total size of table keys, subkey " + Arrays.toString(totalKeysAndPairs(finalTable)));
					if (CommonSubstring.minMatchLength==1)
						Parameters.logStdOutPrintln("Total size of table keys, subkey (of size 1) " + Arrays.toString(totalKeysLengthOneAndPairs()));
					progress.resume();
				}
				String[][] source2 = sourceIter2.next();
				String[][] target2 = targetIter2.next();
				computeMatch(index1, index2, source1, source2, target1, target2);
				index2++;
			}										
		}
		
		
	}

	protected void computeMatch(int index1, int index2, String[][] source1, String[][] source2,
			String[][] target1, String[][] target2) {
		
		if (source1[0][0].isEmpty() || source2[0][0].isEmpty())
			return;
		
		if (ignoreSentence(source1[0][0]) || ignoreSentence(source2[0][0])) 
			return;
		
		// GET ALL SUBSTRING MATCHES FROM SOURCE PAIR
		HashSet<IdentityArrayList<String>> resultMatchSource = 
				CommonSubstring.getAllMaxCommonSubstringsWPL(source1, source2);
		
		// GET ALL SUBSTRING MATCHES FROM TARGET PAIR
		HashSet<IdentityArrayList<String>> resultMatchTarget = 
				CommonSubstring.getAllMaxCommonSubstringsWPL(target1, target2);
		
		//Remove exact matches (e.b., proper nouns, etc...)
		cleanExactMatches(resultMatchSource, resultMatchTarget);
		
		// both not empty
		if (!resultMatchSource.isEmpty() && !resultMatchTarget.isEmpty()) {
			addMatchesToFinalTable(index1, index2, resultMatchSource, resultMatchTarget);
		}
		
	}


	public static void main(String args[]) throws FileNotFoundException {
		
		/*
		String dir = "/Volumes/HardDisk/Scratch/CORPORA/TED_Parallel/en-it/giza_align/";
		args = new String[]{
			"-sourceFile:" + dir + "train.tags.en-it.tok.selection.en",
			"-sourceFileWPL:" + dir + "train.tags.en-it.tok.selection.onewpl.TTposlemmas.en",
			"-targetFile:" + dir + "train.tags.en-it.tok.selection.it",
			"-targetFileWPL:" + dir + "train.tags.en-it.tok.selection.onewpl.TTposlemmas.it",
			"-outputFile:" + dir + "outputTest.gz",
			"-minMatchSize:2"
		};
		*/
		
		String usage = "ParallelSubstringsWPL v. " + getVersion() + "\n" + 
				"usage: java ParallelSubstringsMatch "
				+ "-sourceFile:file "
				+ "-sourceFileWPL:file "
				+ "-targetFile:file "
				+ "-targetFileWPL:file "
				+ "-outputFile:file "
				+ "-minMatchSize:n";		
				
		if (args.length!=6) {
			System.err.println("Wrong number of arguments!");
			System.err.println(usage);
		}
		
		File sourceFile = ArgumentReader.readFileOption(args[0]);
		File sourceFileWPL = ArgumentReader.readFileOption(args[1]);
		File targetFile = ArgumentReader.readFileOption(args[2]);
		File targetFileWPL = ArgumentReader.readFileOption(args[3]);
		File outputFile = ArgumentReader.readFileOption(args[4]);
		int minMatchSize = ArgumentReader.readIntOption(args[5]);
	
		ParallelSubstringsWPL PS = new ParallelSubstringsWPL(
				sourceFile, sourceFileWPL, targetFile, targetFileWPL, outputFile, minMatchSize);
		PS.run();

	}
	
}
