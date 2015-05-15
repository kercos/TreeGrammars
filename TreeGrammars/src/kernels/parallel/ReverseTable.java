package kernels.parallel;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;

import settings.Parameters;
import util.IdentityArrayList;
import util.PrintProgress;
import util.file.FileUtil;

public class ReverseTable {
	
	public ReverseTable(File inputFile, File outputFile) {
		File systemLogFile = FileUtil.changeExtension(outputFile, "log");
		Parameters.openLogFile(systemLogFile);	
		Parameters.logStdOutPrintln("Reversng table.");
		Parameters.logStdOutPrintln("Input file: " + inputFile);
		Parameters.logStdOutPrintln("Output file: " + outputFile);
		HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>>
			outputTable = reverseTable(inputFile);
		ParallelSubstrings.printTable(outputTable, outputFile);
		int[] keys_pairs = ParallelSubstrings.totalKeysAndPairs(outputTable);
		Parameters.logStdOutPrintln("Total keys, pairs: " + Arrays.toString(keys_pairs));				
		Parameters.closeLogFile();	
	}
	
	public static HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> 
		reverseTable(File inputFile) {
		
		HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> result =
				new HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>>();
		PrintProgress pp = new PrintProgress("Reversing table", 10000, 0);
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		IdentityArrayList<String> key=null, value=null;
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = null;
		int[] indexes = null;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //seperate line in new implementation
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] // new implementation only has these lines
				key =  ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			value = ParallelSubstrings.getIdentityArrayListFromBracket(split[2]);
			subTable = result.get(value);
			if (subTable==null) {
				subTable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
				result.put(value, subTable);
			}
			indexes = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);			
			TreeSet<Integer> valueSet = new TreeSet<Integer>();					
			subTable.put(key, valueSet);			
			for(int i : indexes) {
				valueSet.add(i);
			}			
		}
		pp.end();		
		return result;
	}
	
	

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		new ReverseTable(inputFile, outputFile);
		
		/*
		String dir = "/gardner0/data/Results/ParalleSubstring_TED_Words_Min2/";
		File inputFile = new File(dir + "finalTable.gz");
		File outputFile = new File(dir + "finalTable_pruneScores.gz");
		new PruneTable(inputFile, outputFile);
		*/
		
	}
}
