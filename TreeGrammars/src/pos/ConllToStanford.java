package pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ConllToStanford {

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
	
	
	public static void main(String[] args) throws FileNotFoundException {
		File inputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL.conll");
		File outputFileCoarse = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL.stanfordTrainCoarsePoS");
		procesConllToStanfordPosTrain(inputFile, outputFileCoarse, true);
		File outputFileFine = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL.stanfordTrainFinePoS");
		procesConllToStanfordPosTrain(inputFile, outputFileFine, false);
	}
	
}
