package pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class ConllToStanford {

	public static void goldWithTestPosConll(File inputFile, File outputFile, File stanfordPosTags) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		boolean newSentence = true;
		int sentenceCount = 0;
		ArrayList<ArrayList<String>> pos = getPos(stanfordPosTags);
		Iterator<ArrayList<String>> iterPos = pos==null ? null : pos.iterator();
		Iterator<String> iterIterPos = pos==null ? null : iterPos.next().iterator();
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.isEmpty()) {
				if (!newSentence) { //very first sentence
					pw.println();
				}
				newSentence = true;
				iterIterPos = pos==null || !iterPos.hasNext() ? null : iterPos.next().iterator();
				sentenceCount++;
				continue;
			}
			if (line.startsWith("#")) {
				//pw.println(line);
				continue;
			}
			String[] tabs = line.split("\t");
			//0: index
			//1: word
			//2: lemma
			//3: pos_coarse
			//4: pos_fine
			//5: morph
			//6: head
			//7: arclabel
			//8: ?
			//9: ?
			String outputline = 
					tabs[0] + "\t" + tabs[1] + "\t" + "_" +  
					"\t" + iterIterPos.next() + "\t" + "_" + "\t" + "_" + "\t" + tabs[6] +
					"\t" + tabs[7] + "\t" + "_" + "\t" + "_";	
			pw.println(outputline);
			newSentence = false;
		}
		pw.close();
		System.out.println("Successfully processed " + sentenceCount + " sentences.");
	}
	
	private static ArrayList<ArrayList<String>> getPos(File stanfordPosTags) throws FileNotFoundException {
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();		
		Scanner scan = new Scanner(stanfordPosTags);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			ArrayList<String> linePos = new ArrayList<String>();
			String[] wordsPos = line.split(" ");
			for(String wp : wordsPos) {
				String p = wp.split("_")[1];
				linePos.add(p);
			}
			result.add(linePos);
		}
		return result;
	}
	
	public static void procesConllToStanfordPosTrain(File inputFile, 
			File outputFile, boolean coarsePos) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		boolean newSentence = true;
		int sentenceCount = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				if (!newSentence) { //very first sentence
					pw.append("\n");
				}
				newSentence = true;
				sentenceCount++;
				continue;
			}
			if (line.startsWith("#"))
				continue;
			String[] tabs = line.split("\t");
			//0: index
			//1: word
			//2: lemma
			//3: pos_coarse
			//4: pos_fine
			//5: morph
			//6: head
			//7: arclabel
			//8: ?
			//9: ?
			String word = tabs[1];
			String pos = coarsePos ? tabs[3] : tabs[4];
			if (!newSentence) {
				pw.append(' ');
			}
			pw.append(word + "_" + pos);
			newSentence = false;
		}
		pw.close();
		System.out.println("Successfully processed " + sentenceCount + " sentences.");
	}
	
	public static void procesConllToFlat(File inputFile, File outputFile) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		boolean newSentence = true;
		int sentenceCount = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				if (!newSentence) { //very first sentence
					pw.append("\n");
				}
				newSentence = true;
				sentenceCount++;
				continue;
			}
			if (line.startsWith("#"))
				continue;
			String[] tabs = line.split("\t");
			//0: index
			//1: word
			//2: lemma
			//3: pos_coarse
			//4: pos_fine
			//5: morph
			//6: head
			//7: arclabel
			//8: ?
			//9: ?
			String word = tabs[1];
			if (!newSentence) {
				pw.append(' ');
			}
			pw.append(word);
			newSentence = false;
		}
		pw.close();
		System.out.println("Successfully processed " + sentenceCount + " sentences.");
	}
	
	public static void convertUTBpos() throws FileNotFoundException {
		String root = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";		
		for (String f : new String[]{"train","dev","test"}) {
			File inputFile = new File(root + "it-ud-" + f + ".conllx");
			File outputFileCoarse = new File(root + "it-ud-" + f + ".stanfordCoarsePoS.gold");
			procesConllToStanfordPosTrain(inputFile, outputFileCoarse, true);
			//File outputFileFine = new File(root + "it-ud-" + f + ".stanfordFinePoS.gold");
			//procesConllToStanfordPosTrain(inputFile, outputFileFine, false);
		}		
	}
	
	public static void convertUTBflat() throws FileNotFoundException {
		String root = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";		
		for (String f : new String[]{"train","dev","test"}) {
			File inputFile = new File(root + "it-ud-" + f + ".conllx");
			File outputFile = new File(root + "it-ud-" + f + ".flat");
			procesConllToFlat(inputFile, outputFile);
		}		
	}
	

	
	public static void convertISDT() throws FileNotFoundException {
		File inputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL.conll");
		File outputFileCoarse = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL.stanfordTrainCoarsePoS");
		procesConllToStanfordPosTrain(inputFile, outputFileCoarse, true);
		File outputFileFine = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL.stanfordTrainFinePoS");
		procesConllToStanfordPosTrain(inputFile, outputFileFine, false);
	}
	
	
	public static void main(String[] args) throws FileNotFoundException {
		//convertISDT();
		//convertUTBpos();
		//convertUTBflat();
		//goldWithTestPosUTB();
	}
	
}
