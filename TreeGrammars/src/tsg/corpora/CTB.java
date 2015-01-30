package tsg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;



public class CTB {

	
	public static String baseDir = "/scratch/fsangati/CORPUS/Chinese/CTB-3.0/";
	public static String OriginalDir = baseDir + "ORIGINAL/";
	public static String SplitDir = baseDir + "ORIGINAL_SPLIT/";
	public static String ReadableDir = baseDir + "ORIGINAL_READABLE/";
	public static String CleanTopDir = baseDir + "CLEAN_TOP/";
	public static String CleanTopDirCharBased = baseDir + "CLEAN_TOP_CharBased/";
	public static String StatDir = baseDir + "Stats/";
	
	/**
	 * Training: Articles 1-270, 400-1151
	 * Develop: Articles 301-325
	 * Test: Articles 271-300
	 */
	private static void makeSplit(File originalDir, String outputDir) {
		File[] origianlFiles = originalDir.listFiles();
		File train = new File(outputDir + "train.fid");
		File develop = new File(outputDir + "develop.fid");
		File test = new File(outputDir + "test.fid");
		PrintWriter pwTrain = FileUtil.getPrintWriter(train);
		PrintWriter pwDevelop = FileUtil.getPrintWriter(develop);
		PrintWriter pwTest = FileUtil.getPrintWriter(test);
		for(File f : origianlFiles) {
			String fileName = f.getName();
			String numberString = fileName.substring(fileName.indexOf("_")+1,fileName.indexOf('.'));
			int number = Integer.parseInt(numberString);			
			if (number<=270 || (number>=400 && number <= 1151)) {
				FileUtil.append(f, pwTrain);
			}
			else if (number >= 301 && number <= 325) {
				FileUtil.append(f, pwDevelop);
			}
			else if (number >=271 && number <=300) {
				FileUtil.append(f, pwTest);
			}
		}
		pwTrain.close();
		pwDevelop.close();
		pwTest.close();		
	}

	
	public static void makeReadable(File inputDir, String outputDir) {
		for(File inputFile : inputDir.listFiles()) {
			String fileName = inputFile.getName();
			if (!fileName.endsWith(".fid")) {
				continue;
			}
			Scanner reader = FileUtil.getScanner(inputFile);
			File outputFile = FileUtil.changeExtension(new File(outputDir + fileName), "mrg");			
			PrintWriter writer = FileUtil.getPrintWriter(outputFile);			
			int parenthesis = 0;
			String sentence = "";
			while(reader.hasNextLine()) {
				String line = reader.nextLine();
				line = line.trim();
				if (line.length()==0) continue; 
				if (line.charAt(0)=='<') {				
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
	}
	
	private static void makeCleanTOP(File inputDir, String outputDir) throws Exception {
		for(File inputFile : inputDir.listFiles()) {
			String fileName = inputFile.getName();
			if (!fileName.endsWith(".mrg")) {
				continue;
			}			
			File outputFile = new File(outputDir + fileName);				
			PrintWriter pw = FileUtil.getPrintWriter(outputFile);
			Scanner scanner = FileUtil.getScanner(inputFile);
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				line = line.replaceAll("\"", "''");
				TSNodeLabel t = new TSNodeLabel(line);				
				t.makeCleanWsj();
				t.removeSemanticTags();
				t.removeRedundantRules();
				t = t.addTop();
				pw.println(t.toString());																
			}
			pw.close();
		}
	}

	private static void lexStats(String cleanTopDir, String baseDir) throws Exception {
		File treebankFile = new File(cleanTopDir + "train.mrg");
		Scanner scan = FileUtil.getScanner(treebankFile);
		Hashtable<String,int[]> wordTable = new Hashtable<String,int[]>();
		Hashtable<Character,int[]> charTable = new Hashtable<Character,int[]>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			ArrayList<String> treeWords = tree.collectLexicalWords();
			for(String w : treeWords) {
				Utility.increaseInTableInt(wordTable, w);
				for(Character c : w.toCharArray()) {
					Utility.increaseInTableInt(charTable, c);
				}
			}
		}
		
		File outputFile = new File(baseDir + "lexStat.txt");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<String,int[]> e : wordTable.entrySet()) {
			String word = e.getKey();
			pw.println(e.getValue()[0] + " " + word + " " + getCharFreqString(word.toCharArray(), charTable));
		}
		pw.close();
		
		outputFile = new File(baseDir + "charStat.txt");
		pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<Character,int[]> e : charTable.entrySet()) {
			pw.println(e.getValue()[0] + " " + e.getKey());
		}
		pw.close();
	}
	
	
	private static String getCharFreqString(char[] charArray,
			Hashtable<Character, int[]> charTable) {
		StringBuilder sb = new StringBuilder();
		for(Character c : charArray) {			
			sb.append(c + "(" + charTable.get(c)[0] + ")" + " ");
		}
		return sb.toString();
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


	public static void main(String[] args) throws Exception {
		
		//makeSplit(new File(OriginalDir), SplitDir);
		//makeReadable(new File(SplitDir), ReadableDir);
		makeCleanTOP(new File(ReadableDir), CleanTopDir);		
		//lexStats(CleanTopDir, StatDir);
		makeCharBased(new File(CleanTopDir), new File(CleanTopDirCharBased));
	}


	


	
}
