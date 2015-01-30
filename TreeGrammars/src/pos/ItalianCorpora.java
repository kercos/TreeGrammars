package pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

import util.Utility;

public class ItalianCorpora {

	public static void makeCoarseWordTabPos(File inputFile, File outputFile) throws FileNotFoundException {
		// coarse PoS shoul have 1 letter whereas ISST sometimes has FF
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		int wordCount=0, sentenceCount=0;
		String word, pos;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				sentenceCount++;
				pw.println();
				continue;
			}
			String[] tabs = line.split("\t");
			wordCount++;
			word = tabs[0];
			pos = tabs[1];
			pw.println(word + "\t" + pos.charAt(0));
		}		
		pw.close();
		System.out.println("Successfully processed " + wordCount + " words and " + sentenceCount + " sentences.");
	}
	
	public static void makeWordTabPosWordsFlat(File inputFile, File outputFile) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		int wordCount=0, sentenceCount=0;
		String word, pos;
		boolean firstWordInSentence = true;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				sentenceCount++;
				pw.println();
				firstWordInSentence = true;
				continue;
			}
			String[] tabs = line.split("\t");
			wordCount++;
			word = tabs[0];			
			pw.print(firstWordInSentence ? word : " " + word);
			firstWordInSentence = false;
		}		
		pw.close();
		System.out.println("Successfully processed " + wordCount + " words and " + sentenceCount + " sentences.");
	}
	
	public static void makeWordTabPosWordsPosStanford(File inputFile, File outputFile) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		int wordCount=0, sentenceCount=0;
		String word, pos;
		boolean firstWordInSentence = true;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				sentenceCount++;
				pw.println();
				firstWordInSentence = true;
				continue;
			}
			String[] tabs = line.split("\t");
			wordCount++;
			word = tabs[0];			
			pos = tabs[1];
			String wp = word + '_' + pos;
			pw.print(firstWordInSentence ? wp : " " + wp);
			firstWordInSentence = false;
		}		
		pw.close();
		System.out.println("Successfully processed " + wordCount + " words and " + sentenceCount + " sentences.");
	}
	
	public static void main(String args[]) throws FileNotFoundException {
		//makeCoarseWordTabPos(new File(args[0]),new File(args[1]));
		//makeWordTabPosWordsFlat(new File(args[0]),new File(args[1]));
		makeWordTabPosWordsPosStanford(new File(args[0]),new File(args[1]));
	}
	
}
