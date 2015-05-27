package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import util.ArgumentReader;
import util.IdentityArrayList;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class PruneTable_Indexes {
	
	HashMap<IdentityArrayList<String>, 
		HashMap<IdentityArrayList<String>, TreeSet<Integer>>> sourceTargetTable, targetSourceTable;
	
	
	public PruneTable_Indexes(File inputTableFile, int first, int last) throws FileNotFoundException {
		
		int totalElements = last - first + 1;
		
		String suffix = "_" + first + "_" + last;
		File outputTableFile =  FileUtil.changeExtension(inputTableFile, "prune.indexes" + suffix + ".gz");;
		File systemLogFile = FileUtil.changeExtension(inputTableFile, "prune.indexes" + suffix + ".log");
		//this.alignMethod = alignMethod;		

		Parameters.openLogFile(systemLogFile);
		Parameters.logStdOutPrintln("Split Table By indexes");
		Parameters.logStdOutPrintln("Input Table File: " + inputTableFile);
		Parameters.logStdOutPrintln("Output Table File: " + outputTableFile);
		Parameters.logStdOutPrintln("First index: " + first);
		Parameters.logStdOutPrintln("Last index: " + last);

		int[] totalPairsIndexes = new int[2];
		int[] selectedPairsIndexes = new int[2];
		
		sourceTargetTable = ParallelSubstrings.readTableFromFile(inputTableFile);
		targetSourceTable = ReverseTable.reverseTable(inputTableFile);
		
		Scanner scan = FileUtil.getGzipScanner(inputTableFile);
		PrintWriter pw = FileUtil.getGzipPrintWriter(outputTableFile);
		PrintProgress pp = new PrintProgress("Reading table", 10000, 0);
		boolean printedPhraseSource = false;
		IdentityArrayList<String> sourcePhrase=null, targetPhrase=null;
		while (scan.hasNextLine()) {
			pp.next();
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) {
				pw.println(line);
				continue; //2: //new implementation
			}
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] // new implementation only has these lines				
				totalPairsIndexes[0]++;
				sourcePhrase = ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				printedPhraseSource = false;
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			targetPhrase = ParallelSubstrings.getIdentityArrayListFromBracket(split[2]);
			int[] indexes = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);
			totalPairsIndexes[1]+=indexes.length;
			TreeSet<Integer> valueSet = new TreeSet<Integer>();								
			for(int i : indexes) {
				valueSet.add(i-first+1);
			}
			valueSet = new TreeSet<Integer>(valueSet.subSet(1, true,totalElements, true)); //1-based
			if (!valueSet.isEmpty()) {
				if (!printedPhraseSource) {
					pw.println("\t" + sourcePhrase);
					printedPhraseSource = true;
					selectedPairsIndexes[0]++;
				}
				selectedPairsIndexes[1] += valueSet.size();
				double score = getScore(sourcePhrase, targetPhrase);
				pw.println("\t\t" + targetPhrase + "\t" + valueSet + "\t" + score);				
			}
		}
		
		pp.end();
		pw.close();
		scan.close();
		Parameters.logStdOutPrintln("Total pairs/indexes " + Arrays.toString(totalPairsIndexes));
		Parameters.logStdOutPrintln("Selected pairs/indexes " + Arrays.toString(selectedPairsIndexes));		
		
		Parameters.closeLogFile();
	}
	
	private double getScore(IdentityArrayList<String> sourcePhrase, IdentityArrayList<String> targetPhrase) {				
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> targetSubTable = sourceTargetTable.get(sourcePhrase);
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> sourceSubTable = targetSourceTable.get(targetPhrase);
		TreeSet<Integer> table = targetSubTable.get(targetPhrase);
		if (table==null) {
			System.out.println(sourcePhrase);
			System.out.println(targetPhrase);
			System.out.println(targetSubTable);
			System.out.println(sourceSubTable);
		}
		double sourceTargetIndexesSize = targetSubTable.get(targetPhrase).size();
		int sourceIndexSetSize = ParallelSubstrings.getAllIndexes(targetSubTable).size();
		int targetIndexSetSize = ParallelSubstrings.getAllIndexes(sourceSubTable).size();
		double ratioOnTarget = sourceTargetIndexesSize/targetIndexSetSize;
		double ratioOnSource = sourceTargetIndexesSize/sourceIndexSetSize;
		return ratioOnTarget*ratioOnSource;
	}

	static void getIndexes(File bigGzFile, File smallGzFile) throws FileNotFoundException {
		Scanner scanBig = new Scanner(bigGzFile);
		Scanner scanSmall = new Scanner(smallGzFile);
		String lineBig=null, lineSmall=null;
		boolean foundLine = false, foundFirst = false;
		int first=-1, last=-1;
		int lineIndex = -1;
		while(scanSmall.hasNextLine()) {			
			lineSmall = scanSmall.nextLine();
			foundLine = false;
			while(scanBig.hasNextLine()) {
				lineIndex++;
				lineBig = scanBig.nextLine();
				if (lineBig.equals(lineSmall)) {
					foundLine = true;
					break;
				}
			}		
			if (!foundLine) {
				scanSmall.close();
				scanSmall = FileUtil.getGzipScanner(smallGzFile);
				foundFirst = false;
				continue;
			}
			if (!foundFirst) {
				foundFirst = true;
				first = lineIndex;
			}
		}
		last = lineIndex;
		System.out.println(first + "-" + last);
		scanBig.close();
		scanSmall.close();
	}


	public static void main(String[] args) throws FileNotFoundException {
		
		/*
		String path = "/Users/fedja/Dropbox/ted_experiment/corpus_en_it/";
		File bigFile = new File(path + "all.en-it.clean.tok.lc.en");
		File smallFile = new File(path + "IWSLT14.TED.tst2010.en-it.tok.lc.en");
		getIndexes(bigFile, smallFile);
		*/
		
		
		File sourceFile = new File(args[0]);
		//int first = ArgumentReader.readIntOption(args[1]);
		//int last = ArgumentReader.readIntOption(args[2]);
		int first = 189302;
		int last = 190830;
		
		new PruneTable_Indexes(sourceFile, first, last);
		
	}
}
