package pos;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.*;
import tdg.corpora.*;
import util.Utility;
import util.file.FileUtil;

public class PosTagger {
	
	public static String posTagWsjBaseDir = Parameters.corpusPath + "WSJ/POSTAGS/";
	public static String[] depTypeName = 
		new String[]{"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg","COLLINS99Ter", "COLLINS99ArgTer"};
	public static String[] depTypeBase = new String[]{WsjD.WsjMSTulab, WsjD.WsjCOLLINS97_ulab, 
		WsjD.WsjCOLLINS99_ulab, WsjD.WsjCOLLINS99Arg_ulab, WsjD.WsjCOLLINS99Ter_ulab,
		WsjD.WsjCOLLINS99ArgTer_ulab};
	
	public static void depToAdWaitTraining() {
		int depType = 4;
		
		String baseDepDir = depTypeBase[depType];
		
		boolean till11_21 = false;
		String trSec = till11_21 ? "02-11" : "02-21";
		File trainingFile = new File (baseDepDir + "wsj-" + trSec + ".ulab");
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, 1000, false, true);
		File trainigAdWait = new File(posTagWsjBaseDir + "wsj-" + trSec + "_Terminals" + ".pos");
		DepCorpus.toAdWait(training, trainigAdWait);
	}
	
	public static void depToAdWaitGold() {
		int depType = 4;
		
		String baseDepDir = depTypeBase[depType];
		
		String tsSec = "22";
		File goldFile = new File (baseDepDir + "wsj-" + tsSec + ".ulab");
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(goldFile, 1000, false, true);
		File trainigAdWait = new File(posTagWsjBaseDir + "wsj-" + tsSec + "_Terminals" + ".pos");
		DepCorpus.toAdWait(training, trainigAdWait);
	}
	
	public static void adWaitToDep() {		
		String baseDepDir =  WsjD.WsjCOLLINS99Ter_ulab;		
		
		String tsSec = "22";
		File goldFileAdWait = new File (posTagWsjBaseDir + "TERMINALS_100/" + "wsj-" + tsSec + "_Terminals" + ".mxpost");
		File goldFileDep = new File(baseDepDir + "wsj-" + tsSec + ".ulab");
		File goldFileDepMxpost = new File(baseDepDir + "wsj-" + tsSec + ".mxpost.ulab");
		ArrayList<TDNode> depCorpus = DepCorpus.readTreebankFromFileMST(goldFileDep, 1000, false, false);
		
		DepCorpus.fromAdWait(depCorpus, goldFileAdWait, goldFileDepMxpost);
	}
	
	
	public static void eval() {
		File testFile = new File(posTagWsjBaseDir + "ARGSTRUC_100/wsj-22_ArgStruc.mxpost");
		File goldFile = new File(posTagWsjBaseDir + "ARGSTRUC_100/wsj-22_ArgStruc.pos");
		File evalFile = new File(testFile + ".eval");
		
		PrintWriter pw = FileUtil.getPrintWriter(evalFile);
		Scanner testScanner = FileUtil.getScanner(testFile);
		Scanner goldScanner = FileUtil.getScanner(goldFile);
		Hashtable<String, Integer> errorTypesTable = new Hashtable<String, Integer>();
		
		int line=0, total=0, 
			correctPosAndArg=0, 
			correctPosAndTestOrGoldIsArg=0, 
			correctPosAndTestIsArg=0, 
			correctPosAndGoldIsArg=0, 
			correctArguments=0,
			wrongArguments=0,
			correctPosWrongArg=0, 
			correctPos=0, 
			wrongPos=0,
			wrongPosAndWrongArg=0;		
		
		while(testScanner.hasNextLine()) {
			line++;
			String testLine = testScanner.nextLine();
			String goldLine = goldScanner.nextLine();
			String[] testLineSplit = testLine.split("\\s");
			String[] goldLineSplit = goldLine.split("\\s");
			int length = testLineSplit.length;
			if (length != goldLineSplit.length) {
				System.err.println("Test and Gold don't match in length at line " + line);
				break;
			}
			for(int i=0; i<length; i++) {
				total++;
				String testLexPos =  testLineSplit[i];
				String goldLexPos =  goldLineSplit[i];
				String testPosAndArgument = testLexPos.substring(testLexPos.indexOf('_') + 1);
				String goldPosAndArgument = goldLexPos.substring(goldLexPos.indexOf('_') + 1);	
				boolean testIsArgumnet = testPosAndArgument.endsWith("-A");
				boolean goldIsArgumnet = goldPosAndArgument.endsWith("-A");	
				String testPos = removeArgMark(testPosAndArgument);
				String goldPos = removeArgMark(goldPosAndArgument);	
				if (testIsArgumnet == goldIsArgumnet) correctArguments++;
				else wrongArguments++;
				if (testPos.equals(goldPos)) {
					correctPos++;					
					if (testIsArgumnet && goldIsArgumnet) correctPosAndArg++;
					if (testIsArgumnet || goldIsArgumnet) {
						correctPosAndTestOrGoldIsArg++;
						if (testIsArgumnet != goldIsArgumnet) {
							correctPosWrongArg++;
							String errorKey = goldPosAndArgument + "_" + testPosAndArgument;
							Utility.increaseInTableInteger(errorTypesTable, errorKey, 1);
						}
					}					
					if (testIsArgumnet) correctPosAndTestIsArg++;
					if (goldIsArgumnet) correctPosAndGoldIsArg++;					
				}
				else {
					wrongPos++;
					if (testIsArgumnet != goldIsArgumnet) wrongPosAndWrongArg++;
				}
			}
		}
		
		float percentageCorrectPosAndArg = (float) correctPosAndArg / total * 100;
		float percentageCorrectPos = (float) correctPos / total * 100;
		float percentageWrongPos = (float) wrongPos / total * 100;
		float percentageCorrectArg = (float) correctArguments / total * 100;
		float percentageWrongArg = (float) wrongArguments / total * 100;
		float percentageCorrectPosAndWrongArg = (float) correctPosWrongArg / total * 100;
		float percentageCorrectPosAndTestOrGoldIsArg = (float) correctPosAndTestOrGoldIsArg / total * 100;
		float percentageCorrectPosAndTestIsArg = (float) correctPosAndTestIsArg / total * 100;
		float percentageCorrectPosAndGoldIsArg = (float) correctPosAndGoldIsArg / total * 100;
		float percentageWrongPosAndWrongArg = (float) wrongPosAndWrongArg / total * 100;
		
		pw.println("Sentences: " + line);
		pw.println("Total postags: " + total);
		pw.println("Total correct Postags and Arguments: " + correctPosAndArg);
		pw.println("Percentage correct Postags and Arguments: " + percentageCorrectPosAndArg);
		pw.println("Percentage correct Postags: " + percentageCorrectPos);
		pw.println("Percentage wrong pos: " + percentageWrongPos);
		pw.println("Percentage correct arguments: " + percentageCorrectArg);
		pw.println("Percentage wrong arguments: " + percentageWrongArg);
		pw.println("Percentage correct pos and wrong arguments: " + percentageCorrectPosAndWrongArg);
		pw.println("Percentage correct pos and Test or Gold is Arg: " + percentageCorrectPosAndTestOrGoldIsArg);
		pw.println("Percentage correct Pos and Test is Arg: " + percentageCorrectPosAndTestIsArg);
		pw.println("Percentage correct Pos and Gold is Arg: " + percentageCorrectPosAndGoldIsArg);
		pw.println("Percentage Wrong Pos And Wrong Arg: " + percentageWrongPosAndWrongArg);
		pw.println("\nError type chart: ");
		FileUtil.printHashtableToPwOrder(errorTypesTable, pw);		
		pw.close();
	}
	
	public static void getArgumentStructureStatistics() {
		File goldFile = new File(posTagWsjBaseDir + "wsj-02-21_ArgStruc.pos");
		File evalFile = new File(goldFile + ".statistics");
		
		PrintWriter pw = FileUtil.getPrintWriter(evalFile);
		Scanner goldScanner = FileUtil.getScanner(goldFile);
		Hashtable<String, Integer> argumentPos = new Hashtable<String, Integer>();
		Hashtable<String, Integer> nonArgumentPos = new Hashtable<String, Integer>();
		
		int line=0, total=0, arguments=0, nonArguments=0;		
		
		while(goldScanner.hasNextLine()) {
			line++;
			String goldLine = goldScanner.nextLine();
			String[] goldLineSplit = goldLine.split("\\s");
			int length = goldLineSplit.length;
			for(int i=0; i<length; i++) {
				total++;
				String goldLexPos =  goldLineSplit[i];
				String goldPos = goldLexPos.substring(goldLexPos.indexOf('_') + 1);				
				if (goldPos.endsWith("-A")) {
					String pos = removeArgMark(goldPos);
					arguments++;
					Utility.increaseInTableInteger(argumentPos, pos, 1);
				}
				else {
					nonArguments++;
					Utility.increaseInTableInteger(nonArgumentPos, goldPos, 1);
				}
			}
		}
		
		float percentageArguments = (float) arguments / total * 100;
		float percentageNonArguments = (float) nonArguments / total * 100;
		
		pw.println("Sentences: " + line);
		pw.println("Total postags: " + total);
		pw.println("Total arguments: " + arguments);
		pw.println("Total non arguments: " + nonArguments);
		pw.println("Percentage of arguments: " + percentageArguments);
		pw.println("Percentage of non arguments: " + percentageNonArguments);
		pw.println("\nArgument type chart: ");
		FileUtil.printHashtableToPwOrder(argumentPos, pw);		
		pw.println("\nNon-argument type chart: ");
		FileUtil.printHashtableToPwOrder(nonArgumentPos, pw);
		pw.close();
	}
	
	public static String removeArgMark(String s) {
		if (s.endsWith("-A")) return s.substring(0, s.length()-2);
		return s;
	}
	
	
	public static void main(String[] args) {
		//depToAdWaitTraining();
		//depToAdWaitGold();
		//eval();
		adWaitToDep();
		//getArgumentStructureStatistics();
	}
	
}
