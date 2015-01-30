package ccg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import util.file.FileUtil;

public class AnalyzeCcgBank {

	private static void analyzeLexFile() {
		File lexFile = new File("/disk/scratch/fsangati/CORPORA/ccgbank_1_1/wordFreqModel");			
		Scanner scan = FileUtil.getScanner(lexFile);
		File fileUnary = new File("/disk/scratch/fsangati/CORPORA/ccgbank_1_1/wordFreqModel_unary");
		File fileFirstLex = new File("/disk/scratch/fsangati/CORPORA/ccgbank_1_1/wordFreqModel_firstLex");
		File fileFirstSub = new File("/disk/scratch/fsangati/CORPORA/ccgbank_1_1/wordFreqModel_firstSub");
		PrintWriter pwUnary = FileUtil.getPrintWriter(fileUnary);
		PrintWriter pwFirstLex = FileUtil.getPrintWriter(fileFirstLex);
		PrintWriter pwFirstSub = FileUtil.getPrintWriter(fileFirstSub);
		int total = 0;
		int unaryCat = 0;
		int firstLex = 0;
		int firstSub = 0;
		while(scan.hasNextLine()) {
			total++;
			String line = scan.nextLine();
			String[] split = line.split(" ");
			String wordCat = split[0];
			String[] wordCatSplit = wordCat.split("\\|");
			String word = wordCatSplit[0];
			String cat = wordCatSplit[1];
			ArrayList<String> catChunks = decomposeCat(cat);
			int length = catChunks.size();
			int numberOfCats = length/2+1;
			if (numberOfCats==1) {
				unaryCat++;
				pwUnary.append(word + "\t" + catChunks + "\n");
			}
			else if (catChunks.get(1).equals("\\")) {
				firstSub++;
				pwFirstSub.append(word + "\t" + catChunks + "\n");
			}
			else {
				firstLex++;
				pwFirstLex.append(word + "\t" + catChunks + "\n");
			}
			//System.out.println(catChunks);
		}
		pwUnary.close();
		pwFirstLex.close();
		pwFirstSub.close();
		System.out.println("Total: " + total);
		System.out.println("Unary: " + unaryCat);
		System.out.println("First Lex: " + firstLex);
		System.out.println("First Sub: " + firstSub);
		
	}
	
	private static ArrayList<String> decomposeCat(String cat) {
		char[] charArray = cat.toCharArray();
		ArrayList<String> chunks = new ArrayList<String>(); 
		StringBuilder currentChunk = new StringBuilder();
		int parenthesis = 0;
		for(char c : charArray) {
			switch (c) {
				case '(':
					parenthesis++;
					currentChunk.append(c);
					continue;
				case ')':
					parenthesis--;
					currentChunk.append(c);
					if (parenthesis==0 && currentChunk.length()>0) {
						chunks.add(currentChunk.toString());
						currentChunk = new StringBuilder();
					}
					continue;
				case '\\':					
				case '/':
					if (parenthesis==0) {
						if (currentChunk.length()>0) {
							chunks.add(currentChunk.toString());
							currentChunk = new StringBuilder();
						}
						chunks.add(Character.toString(c));
					}					
					else {
						currentChunk.append(c);
					}
					continue;
				default:
					currentChunk.append(c);					
			}
		}
		if (currentChunk.length()>0) {
			chunks.add(currentChunk.toString());			
		}
		return chunks;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		analyzeLexFile();
	}



}
