package tsg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;



public class Brown {

	
	public static void makeReadable(File inputFile, File newFile) {
		Scanner reader = FileUtil.getScanner(inputFile);
		PrintWriter writer = FileUtil.getPrintWriter(newFile);
		int parenthesis = 0;
		String sentence = "";
		while(reader.hasNextLine()) {
			String line = reader.nextLine();
			if (line.length()==0) continue; 
			if (line.charAt(0)=='*') {				
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

	private static void makeClean(File inputFile, File outputFile) throws Exception {		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner scanner = FileUtil.getScanner(inputFile);
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			t.makeCleanWsj();				
			pw.println(t.toString());
		}
		pw.close();
	}
	
	private static void removeSemTagsTOP(File inputFile, File ouputFile) throws Exception {		
		PrintWriter pw = FileUtil.getPrintWriter(ouputFile);
		Scanner scanner = FileUtil.getScanner(inputFile);
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			t.removeSemanticTags();
			t.removeRedundantRules();
			t = t.addTop();
			pw.println(t.toString());
		}
		pw.close();			
	}
	
	private static void splitTrainTestBrown(File complete, File train, File test) {
		Scanner scanner = FileUtil.getScanner(complete);
		PrintWriter pwTrain = FileUtil.getPrintWriter(train);
		PrintWriter pwTest = FileUtil.getPrintWriter(test);
		int index = -1;
		while(scanner.hasNextLine()) {
			index++;
			String line = scanner.nextLine();
			if (index%10 == 0) pwTest.println(line);
			else pwTrain.println(line);
		}
		pwTrain.close();
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
	
	public static void main(String[] args) throws Exception {
		
		String baseDir = "/scratch/fsangati/CORPUS/BROWN/";
		String originalComplete = baseDir + "complete.mrg";
		String originalCompleteReadable = baseDir + "completeReadable.mrg";
		String originalCompleteClean = baseDir + "completeClean.mrg";
		String originalCompleteCleanSemTagOffTOP = baseDir + "completeClean_SemTagOff_TOP.mrg";
		String train = baseDir + "train.mrg";
		String test = baseDir + "test.mrg";
		String testUpTo40 = baseDir + "test_upTo40.mrg";
		String testUpTo40charBased = baseDir + "test_upTo40_charBased.mrg";
				
		//makeReadable(new File(originalComplete), new File(originalCompleteReadable));
		//makeClean(new File(originalCompleteReadable), new File(originalCompleteClean));
		//removeSemTagsTOP(new File(originalCompleteClean), new File(originalCompleteCleanSemTagOffTOP));
		//splitTrainTestBrown(new File(originalCompleteCleanSemTagOffTOP), new File(train), new File(test));
		
		//ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(new File(test), 40);
		//TSNodeLabel.printTreebankToFile(new File(testUpTo40), treebank, false, false);
		//makeCharBased(new File(testUpTo40), new File(testUpTo40charBased));		
	}


	
}
