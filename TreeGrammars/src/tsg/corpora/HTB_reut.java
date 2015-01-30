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



public class HTB_reut {

	public static String baseDir = "/scratch/fsangati/CORPUS/Hebrew/Reut/";
	public static String Complete = baseDir + "TB-reut-phd-experiments";	
	public static String CompleteClean = baseDir + "TB-reut-phd-experiments-clean.mrg";
	public static String SplitDir = baseDir + "SPLIT/";	
	public static String CharBased = baseDir + "CharBased/";
	
	public static String nullTag = "-NONE-";
	
	private static void makeClean() throws Exception {
		File inputFile = new File(Complete);		
		File outputFile = new File(CompleteClean);				
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner scanner = FileUtil.getScanner(inputFile);
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			//line = line.replaceAll("\"", "''");
			//line = line.replaceAll("\\|NO_MATCH", "");
			//line = line.replaceAll("NO_MATCH\\|", "");
			//if (line.equals("(S (yyDOT yyDOT))") || line.equals("(yyDOT yyDOT)")) {					
			//	continue;
			//}
			TSNodeLabel t = new TSNodeLabel(line);				
			//t.makeCleanWsj();	
			t.replaceAllLabels("=.*", "");
			t.removeSemanticTags();
			t.pruneSubTrees(nullTag);
			//t.pruneSubTreesMatching(".*\\*.*"); // if contains *
			t.removeRedundantRules();
			//t = t.addTop();
			pw.println(t.toString());																
		}
		pw.close();		
	}
	
	/**
	 * train: 484-5724
	 * develop: 1-483
	 * test: 5725-6220
	 */
	private static void makeSplit() {
		Scanner scan = FileUtil.getScanner(new File(CompleteClean));
		File train = new File(SplitDir + "train.mrg");
		File develop = new File(SplitDir + "develop.mrg");
		File test = new File(SplitDir + "test.mrg");
		PrintWriter pwTrain = FileUtil.getPrintWriter(train);
		PrintWriter pwDevelop = FileUtil.getPrintWriter(develop);
		PrintWriter pwTest = FileUtil.getPrintWriter(test);
		int lineNumber = 1;
		while(scan.hasNextLine()) {	
			String line = scan.nextLine();
			if (lineNumber>=484 && lineNumber <= 5724) {
				pwTrain.println(line);
			}
			else if (lineNumber >= 1 && lineNumber <= 483) {
				pwDevelop.println(line);
			}
			else if (lineNumber >=5725 && lineNumber <=6220) {
				pwTest.println(line);
			}
			lineNumber++;
		}
		pwTrain.close();
		pwDevelop.close();
		pwTest.close();		
	}
	
	public static void main(String[] args) throws Exception {
			
		//makeClean();
		makeSplit();
		
	}

	

	

	

	

	

	
}
