package tdg.corpora;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeSet;

import util.file.FileUtil;

public class ConnlU {

	/*
	1  	 ID  	Token counter, starting at 1 for each new sentence.
	2  	 FORM  	Word form or punctuation symbol. 		
	3 	LEMMA 	Lemma or stem (depending on particular data set) of word form, or an underscore if not available.
	4 	CPOSTAG Coarse-grained part-of-speech tag, where tagset depends on the language.
	5 	POSTAG 	Fine-grained part-of-speech tag, where the tagset depends on the language, or identical to the coarse-grained part-of-speech tag if not available.
	6 	FEATS 	Unordered set of syntactic and/or morphological features (depending on the particular language), separated by a vertical bar (|), or an underscore if not available.
	7 	HEAD 	Head of the current token, which is either a value of ID or zero ('0'). Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
	8 	DEPREL 	Dependency relation to the HEAD. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
	9 	DEPS 	List of secondary dependencies (head-deprel pairs).
	10 	MISC 	Any other annotation. 
	*/
	
	
	public static void connlU_to_ConnlX(File inputFile, File outputFile) throws FileNotFoundException {
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter (outputFile);
		int lineCount = -1;
		TreeSet<String> multiWords = new TreeSet<String>(); 
		while(scan.hasNextLine()) {
			lineCount++;
			String line = scan.nextLine();
			if (line.isEmpty()) {
				pw.println(line);
				continue;
			}
			if (line.startsWith("#")) {
				continue;
			}
			String[] split = line.split("\t");
			String index = split[0];
			if (index.indexOf('-')!=-1) {
				String word = split[1];
				multiWords.add(word);
				continue;
			}
			pw.println(line);
			String headIndex = split[6];
			if (headIndex.indexOf('-')!=-1)
				System.err.println("Dash in head index at line " + lineCount);
		}
		pw.close();
		scan.close();
		System.out.println("Ignore multiwords: " + multiWords);
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		String rootPath = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";
		File itDevU = new File(rootPath + "it-ud-dev.conllu");
		File itDevX = new File(rootPath + "it-ud-dev.conllx");
		File itTestU = new File(rootPath + "it-ud-test.conllu");
		File itTestX = new File(rootPath + "it-ud-test.conllx");
		File itTrainU = new File(rootPath + "it-ud-train.conllu");
		File itTrainX = new File(rootPath + "it-ud-train.conllx");
		connlU_to_ConnlX(itDevU, itDevX);
		connlU_to_ConnlX(itTestU, itTestX);
		connlU_to_ConnlX(itTrainU, itTrainX);
	}
}
