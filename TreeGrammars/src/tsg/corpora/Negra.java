package tsg.corpora;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class Negra extends ConstCorpus{
	
	private static final long serialVersionUID = 0L;
	public static String NegraTrainingBinaryPath = "/home/fsangati/CORPUS/Negra/Binary/Binary_Train_NoTraces";
	private static final String traceTag = "-NONE-";
	public static String[] nonCountCatInLength = new String[]{traceTag};
	
	public static String baseDir = "/scratch/fsangati/CORPUS/Negra/";
	public static String original = baseDir + "negra_original_UTF8.mrg";
	public static String readable = baseDir + "negra_readable.mrg";
	public static String cleanTOP = baseDir + "negra_clean_TOP.mrg";
	public static String train = baseDir + "train.mrg";
	public static String develop = baseDir + "develop.mrg";
	public static String test = baseDir + "test.mrg";	
	public static String trainCharBased = baseDir + "train_charBased.mrg";
	public static String developCharBased = baseDir + "develop_charBased.mrg";
	public static String testCharBased = baseDir + "test_charBased.mrg";
	
	public static void negra() {
		binarizeNegraAndPrint();
		ExtrWordsFlatGoldNegra();
	}

	public static void binarizeNegraAndPrint() {
		boolean quotations = false;
		File trainFile = new File("/home/fsangati/CORPUS/Negra/TRAIN_noTraceTags");
		File developFile = new File("/home/fsangati/CORPUS/Negra/DEVELOP_noTraceTags");
		File testFile = new File("/home/fsangati/CORPUS/Negra/TEST_noTraceTags");		
		String outputTrainBinary = "/home/fsangati/CORPUS/Negra/Binary/Binary_Train_NoTraces";
		String outputDevelopBinary = "/home/fsangati/CORPUS/Negra/Binary/Binary_Develop_NoTraces";
		String outputTestBinary = "/home/fsangati/CORPUS/Negra/Binary/Binary_Test_NoTraces";
		String outputTrainComplete = "/home/fsangati/CORPUS/Negra/Complete/Complete_Train_NoTraces";
		String outputDevelopComplete = "/home/fsangati/CORPUS/Negra/Complete/Complete_Develop_NoTraces";
		String outputTestComplete = "/home/fsangati/CORPUS/Negra/Complete/Complete_Test_NoTraces";
		ConstCorpus trainCorpora = new ConstCorpus(trainFile, "NEGRA");
		ConstCorpus developCorpora = new ConstCorpus(developFile, "NEGRA");
		ConstCorpus testCorpora = new ConstCorpus(testFile, "NEGRA");
		trainCorpora.removeTraces(traceTag);
		developCorpora.removeTraces(traceTag);
		testCorpora.removeTraces(traceTag);
		//trainCorpora.convertDoubleQuotesForNegra();
		//developCorpora.convertDoubleQuotesForNegra();
		//testCorpora.convertDoubleQuotesForNegra();
		trainCorpora.toBinaryFile(new File(outputTrainBinary));
		developCorpora.toBinaryFile(new File(outputDevelopBinary));
		testCorpora.toBinaryFile(new File(outputTestBinary));
		trainCorpora.toFile_Complete(new File(outputTrainComplete), quotations);
		developCorpora.toFile_Complete(new File(outputDevelopComplete), quotations);
		testCorpora.toFile_Complete(new File(outputTestComplete), quotations);
		int[] LL = new int[] {40,10};
		for(int i=0; i<LL.length; i++) {
			trainCorpora.removeTreesLongerThan(LL[i], nonCountCatInLength);
			developCorpora.removeTreesLongerThan(LL[i], nonCountCatInLength);
			testCorpora.removeTreesLongerThan(LL[i], nonCountCatInLength);
			trainCorpora.toBinaryFile(new File(outputTrainBinary + "_upto" + LL[i]));
			trainCorpora.toBinaryFile(new File(outputDevelopBinary + "_upto" + LL[i]));
			trainCorpora.toBinaryFile(new File(outputTestBinary + "_upto" + LL[i]));
			trainCorpora.toFile_Complete(new File(outputTrainComplete + "_upto" + LL[i]), quotations);
			developCorpora.toFile_Complete(new File(outputDevelopComplete + "_upto" + LL[i]), quotations);
			testCorpora.toFile_Complete(new File(outputTestComplete + "_upto" + LL[i]), quotations);
		}
	}

	public static void ExtrWordsFlatGoldNegra() {
		boolean quotations = false;
		File developFile = new File("/home/fsangati/CORPUS/Negra/DEVELOP_noTraceTags");
		File testFile = new File("/home/fsangati/CORPUS/Negra/TEST_noTraceTags");
		String developOutputPath = "/home/fsangati/CORPUS/Negra/Develop/develop_noTraces";
		String testOutputPath = "/home/fsangati/CORPUS/Negra/Test/test_noTraces";
		ConstCorpus developCorpora = new ConstCorpus(developFile, "NEGRA");
		ConstCorpus testCorpora = new ConstCorpus(testFile, "NEGRA");
		developCorpora.removeTraces(traceTag);
		testCorpora.removeTraces(traceTag);
		int[] LL = new int[] {20};
		for(int i=0; i<LL.length; i++) {
			developCorpora.removeTreesLongerThan(LL[i], nonCountCatInLength);
			testCorpora.removeTreesLongerThan(LL[i], nonCountCatInLength);
			developCorpora.toFile_Flat(new File(developOutputPath + "_upto" + LL[i] + "_Flat"));
			developCorpora.toFile_ExtractWords(new File(developOutputPath + "_upto" + LL[i] + "_ExtrWords"));
			developCorpora.toFile_Complete(new File(developOutputPath + "_upto" + LL[i] + "_Gold"), quotations);
			testCorpora.toFile_Flat(new File(testOutputPath + "_upto" + LL[i] + "_Flat"));
			testCorpora.toFile_ExtractWords(new File(testOutputPath + "_upto" + LL[i] + "_ExtrWords"));
			testCorpora.toFile_Complete(new File(testOutputPath + "_upto" + LL[i] + "_Gold"), quotations);
		}
	}
	
	public static void makeReadable(File inputFile, File newFile) {
		Scanner reader = FileUtil.getScanner(inputFile);
		PrintWriter writer = FileUtil.getPrintWriter(newFile);
		int parenthesis = 0;
		String sentence = "";
		while(reader.hasNextLine()) {
			String line = reader.nextLine();
			if (line.length()==0) continue; 
			if (line.charAt(0)=='%') {				
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
	
	private static void makeCleanTOP(File inputFile, File outputFile) throws Exception {		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner scanner = FileUtil.getScanner(inputFile);
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.replaceAll("\"", "''");
			TSNodeLabel t = new TSNodeLabel(line);
			t.pruneSubTreesStartingWith("*T");
			t.replaceAllNonTerminalLabels("-\\*T.*\\*", "");
			t.removeSemanticTags();
			t.removeRedundantRules();
			t = t.addTop();
			pw.println(t.toString());
		}
		pw.close();
	}
	
	// first 18,602  test
	// followiing 1,000 develop
	// last 1,000 test
	// (Dubey 2005)
	private static void splitTrainTestNegra(File complete, File train, File develop, File test) {
		Scanner scanner = FileUtil.getScanner(complete);
		PrintWriter pwTrain = FileUtil.getPrintWriter(train);
		PrintWriter pwDevelop = FileUtil.getPrintWriter(develop);
		PrintWriter pwTest = FileUtil.getPrintWriter(test);
		PrintWriter pw = pwTrain;
		int index = 0;
		while(scanner.hasNextLine()) {
			index++;
			String line = scanner.nextLine();
			if (index==18603) pw = pwDevelop;
			else if (index==19603) pw = pwTest;			
			pw.println(line);			
		}
		pwTrain.close();
		pwDevelop.close();
		pwTest.close();		
	}
	
	public static void makeCharBased(File inputFile, File outputFile) throws Exception {		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		for(TSNodeLabel t : treebank) {
			t.makeTreeCharBased();			
			pw.println(t.toString());
		}
		pw.close();
	}

	public static void main(String args[]) throws Exception {						
		//System.out.println(FileUtil.countNonEmptyLines(new File(original)));		
		//makeReadable(new File(original), new File(readable));
		//makeCleanTOP(new File(readable), new File(cleanTOP));
		//splitTrainTestNegra(new File(cleanTOP), new File(train), new File(develop), new File(test));
		//binarizeNegraAndPrint();
		makeCharBased(new File(train), new File(trainCharBased));
		makeCharBased(new File(develop), new File(developCharBased));
		makeCharBased(new File(test), new File(testCharBased));		
	}

	

}
