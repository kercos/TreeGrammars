package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.BitSet;
import java.util.Scanner;

import util.Utility;

public class SearchParallelSubstrings {
	
	static final int maxIndexes = 10;	

	private static BitSet[] analyzePatterns(File srcFile, File dstFile, String[][] patterns) throws FileNotFoundException {
		BitSet srcPatternLineNumber = getPatternLineNumber(patterns[0], srcFile); 
		BitSet dstPatternLineNumber = getPatternLineNumber(patterns[1], dstFile);
		BitSet intersection = (BitSet) srcPatternLineNumber.clone();
		intersection.and(dstPatternLineNumber);
		srcPatternLineNumber.andNot(intersection);
		dstPatternLineNumber.andNot(intersection);
		return new BitSet[]{intersection, srcPatternLineNumber, dstPatternLineNumber};
	}
	
	@SuppressWarnings("resource")
	private static BitSet getPatternLineNumber(String[] pattern, File file) throws FileNotFoundException {
		BitSet bs = new BitSet();
		Scanner scan  = new Scanner(file);
		int lineNumber = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			//if (line.indexOf(Utility.joinStringArrayToString(pattern, " "))!=-1) {
				String[] words = line.split(" ");			
				if (hasPattern(words, pattern))
					bs.set(lineNumber);
			//}			
			lineNumber++;
		}
		return bs;
	}

	private static boolean hasPattern(String[] words, String[] pattern) {
		int currentMatch = 0;
		for(int i=0; i<words.length; i++) {
			if (words[i].equals(pattern[currentMatch])) {
				if (++currentMatch==pattern.length)
					return true;
			}
			else 
				currentMatch=0;
		}
		return false;
	}

	private static String[][] getPatterns(String pattern) {
		String[][] result = new String[2][];
		String[] split = pattern.split("\\|");
		if (split.length!=2) {
			return null;
		}
		result[0] = split[0].split("_");
		result[1] = split[1].split("_");
		return result;
	}
	
	public static String toString(BitSet bs) {
		if (bs.cardinality()<=maxIndexes)
			return bs.toString();
		
		StringBuilder sb = new StringBuilder("{");
		int index = -1;
		for (int i=0; i<maxIndexes; i++) {
			index = bs.nextSetBit(++index);
			sb.append(index);
			sb.append(", ");
		}
		sb.append("...}");
		return sb.toString();
	}
	
	@SuppressWarnings("resource")
	private static String getSentence(File file, int randomIndex) throws FileNotFoundException {
		Scanner scan  = new Scanner(file);
		for(int i=0; i<randomIndex; i++) {
			scan.nextLine();
		}
		return scan.nextLine();
	}

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		
		/*
		String workingdir = "/gardner0/data/Corpora/TED_Parallel/en-it/giza_align/";
		args = new String[]{
			workingdir + "train.tags.en-it.tok.selection.en",	
			workingdir + "train.tags.en-it.tok.selection.it",
			"come_up_with|mente"
		};
		*/
		
		String usage = "java SearchParallelSubstrings sourceFile, targetFile, pattern\n" +
				"\tpattern should be a single string (with underscores instead of spaces)\n" +
				"\tand  a '|' to separate src from dst pattern (e.g. 'by_the_way|comunque')";		
		
		File sourceFile = new File(args[0]);
		File targetFile = new File(args[1]);
		String[][] patterns = getPatterns(args[2]); // "by_the_way|comunque"
		if (patterns==null) {
			System.err.println("Pattern doesn't follow requirements:" + args[2]);
			System.err.println(usage);
			return;
		}		
		BitSet[] intersectiopn_onlySrc_onlyDst = analyzePatterns(sourceFile, targetFile, patterns);
		BitSet intSet = intersectiopn_onlySrc_onlyDst[0];
		BitSet sourceSet = intersectiopn_onlySrc_onlyDst[1];
		BitSet targetSet = intersectiopn_onlySrc_onlyDst[2];
		
		System.out.println("Intersection indexes (" + intSet.cardinality() + "):   " + toString(intSet));
		System.out.println("Only Source indexes (" + sourceSet.cardinality() + "): " + toString(sourceSet));
		System.out.println("Only Target indexes (" + targetSet.cardinality() + "): " + toString(targetSet));
		
		boolean giveRandomExample = true;
		if (giveRandomExample && !intSet.isEmpty()) {
			int size = intSet.cardinality();
			int randomPos = Utility.randomInteger(size);
			int randomIndex = -1;
			for (int j=0; j<=randomPos; j++) {
				randomIndex = intSet.nextSetBit(++randomIndex);
			}
			String sourceSentence = getSentence(sourceFile, randomIndex);
			String targetSentence = getSentence(targetFile, randomIndex);
			System.out.println("Random Index: " + randomIndex);
			System.out.println("Random Souce Sentence: " + sourceSentence);
			System.out.println("Random Souce Sentence: " + targetSentence);			
		}

	}





}
