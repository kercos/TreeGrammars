package tsg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;

import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;



public class HTB_trans {

	public static String baseDir = "/scratch/fsangati/CORPUS/Hebrew/Translitarated/";
	public static String OriginalComplete = baseDir + "complete_original.mrg";	
	public static String OriginalCompleteReadable = baseDir + "complete_original_readable.mrg";
	public static String OriginalReadableSplit = baseDir + "ORIGINAL_READABLE_SPLIT/";
	public static String CleanTOP = baseDir + "CLEAN_TOP/";
	public static String CleanTOPCharBased = baseDir + "CLEAN_TOP_CharBased/";
	
	private static void checkSentences(File file) {
		Scanner scan = FileUtil.getScanner(file);
		int expectedIndex = 1;
		int readIndex = -1;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith("tree")) {
				readIndex = Integer.parseInt(line.substring(line.indexOf('#')+1));
				if (readIndex==expectedIndex) {
					//System.out.println("Good Tree: " + readIndex);
					expectedIndex++;
				}
				else {
					System.out.println("Jump Tree: " + readIndex);
					expectedIndex = readIndex+1;
				}
			}
		}
		System.out.println("Last tree: " + readIndex);
	}
	
	public static void makeReadable(File inputFile, File newFile) {
		Scanner reader = FileUtil.getScanner(inputFile);
		PrintWriter writer = FileUtil.getPrintWriter(newFile);
		int parenthesis = 0;
		String sentence = "";
		while(reader.hasNextLine()) {
			String line = reader.nextLine();
			if (line.length()==0) continue; 
			if (line.startsWith("tree")) {				
				continue;
			}
			parenthesis += Utility.countParenthesis(line);
			sentence += line;
			if (parenthesis==0) {
				if (line.length()==0) continue;
				sentence = sentence.trim();								
				sentence = sentence.replaceAll("\n", "");
				sentence = sentence.replaceAll("\\s+", " ");
				writer.println(ConstCorpus.adjustParenthesisation(sentence));
				//writer.println(sentence);
				sentence = "";
			}				
		}
		reader.close();
		writer.close();
	}

	/**
	 * train: 501-6000
	 * develop: 1-500
	 * test: 6001-6501
	 */
	private static void makeSplit() {
		Scanner scan = FileUtil.getScanner(new File(OriginalCompleteReadable));
		File train = new File(OriginalReadableSplit + "train.mrg");
		File develop = new File(OriginalReadableSplit + "develop.mrg");
		File test = new File(OriginalReadableSplit + "test.mrg");
		PrintWriter pwTrain = FileUtil.getPrintWriter(train);
		PrintWriter pwDevelop = FileUtil.getPrintWriter(develop);
		PrintWriter pwTest = FileUtil.getPrintWriter(test);
		int lineNumber = 1;
		while(scan.hasNextLine()) {	
			String line = scan.nextLine();
			if (lineNumber>=501 && lineNumber <= 6000) {
				pwTrain.println(line);
			}
			else if (lineNumber >= 1 && lineNumber <= 500) {
				pwDevelop.println(line);
			}
			else if (lineNumber >=6001 && lineNumber <=6501) {
				pwTest.println(line);
			}
			lineNumber++;
		}
		pwTrain.close();
		pwDevelop.close();
		pwTest.close();		
	}
	
	private static void makeCleanTOP() throws Exception {
		File inputDir = new File(OriginalReadableSplit);		
		for(File inputFile : inputDir.listFiles()) {
			String fileName = inputFile.getName();
			if (!fileName.endsWith(".mrg")) {
				continue;
			}			
			File outputFile = new File(CleanTOP + fileName);				
			PrintWriter pw = FileUtil.getPrintWriter(outputFile);
			Scanner scanner = FileUtil.getScanner(inputFile);
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				line = line.replaceAll("\"", "''");
				line = line.replaceAll("\\|NO_MATCH", "");
				line = line.replaceAll("NO_MATCH\\|", "");
				if (line.equals("(S (yyDOT yyDOT))") || line.equals("(yyDOT yyDOT)")) {					
					continue;
				}
				TSNodeLabel t = new TSNodeLabel(line);				
				//t.makeCleanWsj();	
				t.replaceAllLabels("=.*", "");
				t.removeSemanticTags();
				t.pruneSubTreesMatching(".*\\*.*"); // if contains *
				t.removeRedundantRules();
				t = t.addTop();
				pw.println(t.toString());																
			}
			pw.close();
		}		
	}
	
	public static void makeCharBased(File inputFile, File outputFile) throws Exception {		
		if (inputFile.isDirectory()) {
			File[][] srcDstFiles = Wsj.getFilePairs(inputFile, outputFile);
			int size = srcDstFiles[0].length;
			for(int i=0; i<size; i++) {
				makeCharBased(srcDstFiles[0][i], srcDstFiles[1][i]);				
			}
			return;
		}
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		for(TSNodeLabel t : treebank) {
			t.makeTreeCharBased();			
			pw.println(t.toString());
		}
		pw.close();
	}
	
	public static void makeCharBased(File inputFile, File outputFile, int LL) throws Exception {		
		if (inputFile.isDirectory()) {
			File[][] srcDstFiles = Wsj.getFilePairs(inputFile, outputFile);
			int size = srcDstFiles[0].length;
			for(int i=0; i<size; i++) {
				makeCharBased(srcDstFiles[0][i], srcDstFiles[1][i], LL);				
			}
			return;
		}
		outputFile = new File(outputFile + "_upto" + LL);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile, LL);
		for(TSNodeLabel t : treebank) {
			t.makeTreeCharBased();			
			pw.println(t.toString());
		}
		pw.close();
	}
	
	private static void getPosStats() throws Exception {
		File dirFile = new File(CleanTOP);
		File[] fileList = dirFile.listFiles();
		//File outputStatsFile = new File(baseDir + "PoS_Stats.txt");
		TreeSet<String> posSet = new TreeSet<String>();
		for(File f : fileList) {
			if (!f.getName().endsWith(".mrg")) {
				continue;
			}
			Scanner scan = FileUtil.getScanner(f);			 
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				TSNodeLabel t = new TSNodeLabel(line);
				ArrayList<Label> posLabels = t.collectPreLexicalLabels();
				for(Label l : posLabels) {
					posSet.add(l.toString());
				}
			}
		}		
		System.out.println(posSet);
		
	}
	
	private static void getLexPosStats() throws Exception {
		File dirFile = new File(CleanTOP);
		File[] fileList = dirFile.listFiles();
		//File outputStatsFile = new File(baseDir + "PoS_Stats.txt");
		Hashtable<String,TreeSet<String>> lexPosTable = new Hashtable<String,TreeSet<String>>(); 		
		for(File f : fileList) {
			if (!f.getName().endsWith(".mrg")) {
				continue;
			}
			Scanner scan = FileUtil.getScanner(f);			 
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				TSNodeLabel t = new TSNodeLabel(line);
				ArrayList<TSNodeLabel> lexicon = t.collectLexicalItems();								
				for(TSNodeLabel l : lexicon) {
					addInLexPosTable(l.label(), l.parent.label(), lexPosTable);
				}
			}
		}		
		for(Entry<String,TreeSet<String>> e : lexPosTable.entrySet()) {
			System.out.println(e.getKey() + "\t" + e.getValue());
		}
	}
	
	public static void addInLexPosTable(String lex, String pos, Hashtable<String,TreeSet<String>> lexPosTable) {
		TreeSet<String> posList = lexPosTable.get(lex);
		if (posList==null) {
			posList = new TreeSet<String>();
			lexPosTable.put(lex, posList);
		}
		posList.add(pos);
	}
	
	public static void main(String[] args) throws Exception {
				
		//checkSentences(new File(OriginalComplete));
		//makeReadable(new File(OriginalComplete), new File(OriginalCompleteReadable));
		//makeSplit();
		//makeCleanTOP();
		//makeCharBased(new File(CleanTOP), new File(CleanTOPCharBased));
		makeCharBased(new File(CleanTOP), new File(CleanTOPCharBased), 40);
		//getPosStats();
		//getLexPosStats();
		
	}

	

	

	

	

	

	
}
