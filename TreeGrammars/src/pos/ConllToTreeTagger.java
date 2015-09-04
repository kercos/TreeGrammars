package pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;

import util.Utility;

public class ConllToTreeTagger {

	private static void makeGold(File inputFile, File outputFile) throws FileNotFoundException {
		
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		
		boolean newSentence = true;
		int sentenceCount = 0;
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				newSentence = true;
				sentenceCount++;
				pw.append("_EOS_").append('\t').append("SENT").append('\t').append("_").append("\n");
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
			String lemma = tabs[2];
			String pos = tabs[3]; //coarsePos ? tabs[3] : tabs[4];
			pw.append(word).append('\t').append(pos).append('\t').append(lemma).append("\n");
		}

		pw.close();

		System.out.println("Successfully processed " + sentenceCount + " sentences.");
		
	}
	
	private static void prepareTreeTaggerTrainFiles() throws FileNotFoundException {
		String root = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/may_15/";
		File inputFile = new File(root + "data/it-ud-train.conllx");		
		File outputFile = new File(root + "TreeTagger/it-ud-train_TreeTagger_Coarse.gold");
		File lexiconFile = new File(root + "TreeTagger/lexicon");
		
		HashMap<String,HashMap<String, HashMap<String, int[]>>> wpl_table = new HashMap<String,HashMap<String, HashMap<String, int[]>>>();
		//word -> pos -> lemma
		Utility.increaseInHashMap(wpl_table, "_EOS_", "SENT", "_");
		
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		
		boolean newSentence = true;
		int sentenceCount = 0;
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				newSentence = true;
				sentenceCount++;
				pw.append("_EOS_").append('\t').append("SENT").append('\t').append("_").append("\n");
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
			String lemma = tabs[2];
			String pos = tabs[3]; //coarsePos ? tabs[3] : tabs[4];
			pw.append(word).append('\t').append(pos).append('\t').append(lemma).append("\n");
			Utility.increaseInHashMap(wpl_table, word, pos, lemma);
		}

		//last line
		
		//pw.append("_EOS_").append('\t').append("SENT").append("\n");

		pw.close();
		HashSet<String> ignoredDigits = new HashSet<String>();
		HashSet<String> includeddDigits = new HashSet<String>();

		pw = new PrintWriter(lexiconFile);
		for(Entry<String, HashMap<String, HashMap<String, int[]>>> e : wpl_table.entrySet()) {
			String w = e.getKey();			
			//if (w.matches("\\d+")) {
			//	ignoredDigits.add(w);
			//	continue;
			//}
			if (w.matches(".*\\d.*")) {
				includeddDigits.add(w);
			}
			StringBuilder sb = new StringBuilder();
			sb.append(w);			
			for(Entry<String, HashMap<String, int[]>> f : e.getValue().entrySet()) {
				String p = f.getKey();
				HashMap<String, int[]> lemmaTable = f.getValue();
				String l = Utility.getMaxKeyInt(lemmaTable);
				if (lemmaTable.size()!=1) {
					System.err.println("Multiple values for wpl: " + w + "|" + p + "|" + Utility.toStringFlat(lemmaTable) + " --> " + l);					
				}				
				//for(String l : lemmaSet) {
					//sb.append('\t').append(p).append(' ').append(l);
				//}								
				sb.append('\t').append(p).append(' ').append(l);
			}			
			pw.println(sb.toString());
		}
		
		pw.close();
		System.out.println("Ignored digits in lexicon: " + ignoredDigits);
		System.out.println("Included digits in lexicon: " + includeddDigits);
		System.out.println("Successfully processed " + sentenceCount + " sentences.");
		
	}
	
	public static void procesConllToFlat(File inputFile, File outputFile) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		int sentenceCount = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				sentenceCount++;
				pw.append("_EOS_").append('\n');
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
			pw.append(word).append('\n');
		}
		pw.close();
		System.out.println("Successfully processed " + sentenceCount + " sentences.");
	}

	public static void main(String[] args) throws FileNotFoundException {
		String root = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/may_15/";
		makeGold(new File(root + "data/it-ud-dev.conllx"),new File(root + "TreeTagger/it-ud-dev.gold"));
		makeGold(new File(root + "data/it-ud-test.conllx"),new File(root + "TreeTagger/it-ud-test.gold"));
		//prepareTreeTaggerTrainFiles();

		
		//procesConllToFlat(new File(root + "data/it-ud-dev.conllx"),new File(root + "TreeTagger/it-ud-dev.flat"));
		//procesConllToFlat(new File(root + "data/it-ud-test.conllx"),new File(root + "TreeTagger/it-ud-test.flat"));
		
	}
	
}
