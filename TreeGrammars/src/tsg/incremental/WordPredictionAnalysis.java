package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

import util.file.FileUtil;

public class WordPredictionAnalysis {

	static String nextWordLabel = "NEXT_WORD";
	static String separator = " ";
	
	public static HashSet<String> getVocabulary(File trainingFileFlat) {
		Scanner scan = FileUtil.getScanner(trainingFileFlat);
		HashSet<String> result = new HashSet<String>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split(" ");
			for(String word : lineSplit) {
				result.add(word);
			}
		}
		return result;
	}
	
	public static void prepareMapFile(File trainingFileFlat, File outputFile) {
		HashSet<String> vocab = getVocabulary(trainingFileFlat);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(String w : vocab) {
			pw.println(w + separator + w);
		}
		pw.print(nextWordLabel + separator);
		Iterator<String> it = vocab.iterator();
		while(it.hasNext()) {
			pw.print(it.next());
			if (it.hasNext())
				pw.print(" ");
		}
		pw.println("");
		pw.close();
		System.out.println("Total number of words: " + vocab.size());
	}
	
	public static void main(String[] args) {
		prepareMapFile(new File(args[0]), new File(args[1]));
	}
	
}
