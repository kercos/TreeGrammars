package tsg.incremental;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import tsg.TSNodeLabel;

public class BNC_Surprisal {

	private static void checkOutOfVoc(File vocabulary, File inputFile) throws Exception {
		HashSet<String> vocab = new HashSet<String>();
		Scanner scan = new Scanner(vocabulary); 
		while(scan.hasNextLine()) {
			String w = scan.nextLine().trim();
			if (!w.isEmpty())
				vocab.add(w);
		}	
		
		scan = new Scanner(inputFile);
		int lineNum = 0;
		 
		HashSet<String> outOfVocab = new HashSet<String>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			ArrayList<TSNodeLabel> lex = tree.collectLexicalItems();
			for(TSNodeLabel l : lex) {
				String w = l.label(); 
				if (!vocab.contains(w)) {
					//System.out.println(lineNum + "\t" + l.label());
					outOfVocab.add(w);
				}
			}
			lineNum++;
		}
		System.out.println(outOfVocab);
		System.out.println(outOfVocab.size());
		
	}
	
	private static void checkCapitalization(File inputFile) throws Exception {
		Scanner scan = new Scanner(inputFile);
		int lineNum = 0;
		HashSet<String> capitalizedLex = new HashSet<String>(); 
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			ArrayList<TSNodeLabel> lex = tree.collectLexicalItems();
			for(TSNodeLabel l : lex) {
				String w = l.label(); 
				if (w.matches("[A-Z].*")) {
					//System.out.println(lineNum + "\t" + l.label());
					capitalizedLex.add(w);
				}
			}
			lineNum++;
		}
		System.out.println(capitalizedLex);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String workPath = "/Users/fedja/Work/JointProjects/Stefan/BNC.treebanks/";
		File inputFile = new File(workPath + "BNC.treebank.part0.txt");
		File vocabulary = new File(workPath + "vocabulary.txt");
		//checkCapitalization(inputFile);
		checkOutOfVoc(vocabulary, inputFile);

	}

	

	

}
