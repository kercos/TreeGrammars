package kernels.parallel;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Map.Entry;

import settings.Parameters;
import util.IdentityArrayList;
import util.PrintProgress;
import util.file.FileUtil;

public class ExtracSourceFromTable {
	
	public ExtracSourceFromTable(File inputFile, File outputFile) {
		File systemLogFile = FileUtil.changeExtension(outputFile, "log");
		Parameters.openLogFile(systemLogFile);	
		Parameters.logStdOutPrintln("Extract source from table.");
		Parameters.logStdOutPrintln("Input file: " + inputFile);
		Parameters.logStdOutPrintln("Output file: " + outputFile);
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> sourceTable = extractSourceFreqFromTable(inputFile);	
		printSourceTable(sourceTable, outputFile);
		Parameters.logStdOutPrintln("Total keys: " + sourceTable.size());
		Parameters.closeLogFile();	
	}
	
	public static HashMap<IdentityArrayList<String>, TreeSet<Integer>> 
		extractSourceFreqFromTable(File inputFile) {
		
		PrintProgress pp = new PrintProgress("Extracting source from table", 10000, 0);
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> sourceTable =
				new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		TreeSet<Integer> indexes = null;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //seperate line in new implementation			
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] // new implementation only has these lines				
				IdentityArrayList<String> source =  ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				indexes = new TreeSet<Integer>();
				sourceTable.put(source, indexes);
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			int[] subIndexes = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);
			for(int i : subIndexes)
				indexes.add(i); 			
		}
		pp.end();		
		return sourceTable;
	}
	
	public static void printSourceTable(
			HashMap<IdentityArrayList<String>,TreeSet<Integer>> sourceTable,
			File printTableFile) {
		PrintWriter pw = FileUtil.getGzipPrintWriter(printTableFile);
		int size = sourceTable.size();
		int count = 0;
		for(int i=0; i<10000; i++) {	// order by the length of the source substring		
			boolean foundSize = false;
			for( Entry<IdentityArrayList<String>, TreeSet<Integer>> e : sourceTable.entrySet()) {
				IdentityArrayList<String> source = e.getKey();
				int length = source.size();
				if (length==i) {
					count++;
					if (!foundSize) {
						pw.println(length + ":");
						foundSize = true;
					}
					pw.println("\t" + source + "\t" + e.getValue().toString());
				}								
			}
			if (count==size)
				break;
		}
		pw.close();
	}

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		new ExtracSourceFromTable(inputFile, outputFile);
		
		/*
		String dir = "/gardner0/data/Results/ParalleSubstring_TED_Words_Min2/";
		File inputFile = new File(dir + "finalTable.gz");
		File outputFile = new File(dir + "finalTable_pruneScores.gz");
		new PruneTable(inputFile, outputFile);
		*/
		
	}
}
