package kernels.parallel;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import util.FisherExact;
import util.IdentityArrayList;
import util.PrintProgress;
import util.file.FileUtil;

public class PruneTable_RatiosThreshold {
	
	static boolean outputStats = false;
		
	HashMap<IdentityArrayList<String>,TreeSet<Integer>> sourceTotIndexSet;
	HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> reversedTable;
	double ratiosThreshold;
	File inputFile;
	File systemLogFile;
	File outputFile, outputFileStats;
	PrintWriter pw, pwStats;
	int selectedPairs;

	public PruneTable_RatiosThreshold(File inputFile, String thresholdString) {		
		this.inputFile = inputFile;
		systemLogFile = FileUtil.changeExtension(inputFile, "prune.threshold."+thresholdString+".log");
		outputFileStats = outputStats ?
				FileUtil.changeExtension(inputFile, "prune.threshold."+thresholdString+".stats.gz") :
				null;
		this.outputFile = FileUtil.changeExtension(inputFile, "prune.threshold."+thresholdString+".gz");
		ratiosThreshold = Double.parseDouble(thresholdString);
		
		Parameters.openLogFile(systemLogFile);
		Parameters.logStdOutPrintln("Prune Table Heurisitcs");
		Parameters.logStdOutPrintln("Input File: " + inputFile);
		Parameters.logStdOutPrintln("Ratios threshold: " + thresholdString);
		Parameters.logStdOutPrintln("Output File: " + outputFile);
		if (outputStats)
			Parameters.logStdOutPrintln("Output File Stats: " + outputFileStats);
		
		reversedTable = ReverseTable.reverseTable(inputFile);
		
		sourceTotIndexSet = buildSourceTotIndexSet(inputFile);
		pruneTable();
		
		Parameters.logStdOutPrintln("Number of selected pairs: " + selectedPairs);		
		
		Parameters.closeLogFile();
	}
	
	public static HashMap<IdentityArrayList<String>, TreeSet<Integer>> buildSourceTotIndexSet(File inputFile) {
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> result = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
		PrintProgress pp = new PrintProgress("Building Source Index Table", 10000, 0);
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		IdentityArrayList<String> key=null;
		TreeSet<Integer> indexSet = new TreeSet<Integer>();		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //new implementation
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] //  and new implementation 
				if (key!=null) {
					result.put(key, indexSet);
					indexSet = new TreeSet<Integer>();
				}
				key = ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				continue;
			}
			pp.next();			
			int[] indexes = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);
			for(int i : indexes) {
				indexSet.add(i);
			}
		}
		// last key-subtable
		result.put(key, indexSet);
		pp.end();
		return result;
	}

	private void pruneTable() {
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		pwStats = outputStats ? FileUtil.getGzipPrintWriter(outputFileStats) : null;
		pw = FileUtil.getGzipPrintWriter(outputFile);
		
		PrintProgress pp = new PrintProgress("Reading and pruning table", 1000, 0);
		IdentityArrayList<String> key=null, value=null;
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
		int[] indexes = null;				
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) {
				pw.println(line);
				if (outputStats)
					pwStats.println(line);
				continue; //2:
			}				 
			if (split.length==2) {
				//        [check, for] //  and new implementation 
				if (key!=null) {
					simplify(key, subTable);
					subTable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
				}
				key = ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			value = ParallelSubstrings.getIdentityArrayListFromBracket(split[2]);
			indexes = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);			
			TreeSet<Integer> valueSet = new TreeSet<Integer>();					
			subTable.put(value, valueSet);
			for(int i : indexes) {
				valueSet.add(i);
			}
		}
		// last key-subtable
		simplify(key, subTable);		
		
		pp.end();
		
		pw.close();
		if (outputStats)
			pwStats.close();
		
	}

	private void simplify(IdentityArrayList<String> sourceKey,
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> targetTable) {
		
		TreeSet<Integer> sourceIndexSet = ParallelSubstrings.getAllIndexes(targetTable);
		
		int souceTargetKeysCount = targetTable.size();
				
		if (outputStats) {
			pwStats.println("\t" + sourceKey +
				"\t" + "TotSourceIndexCount:" + sourceIndexSet.size() +
				"\t" + "TotTargetKeysCount:" + souceTargetKeysCount);
		}
		
		boolean printedSource = false;
		
		for(Entry<IdentityArrayList<String>, TreeSet<Integer>> e : targetTable.entrySet()) {
			IdentityArrayList<String> targetKey = e.getKey();
			TreeSet<Integer> sourceTargetIndexes = e.getValue();			
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> sourceTable = reversedTable.get(targetKey);
			TreeSet<Integer> targetIndexSet = ParallelSubstrings.getAllIndexes(sourceTable);
			
			int targetIndexSetSize = targetIndexSet.size();
			int sourceIndexSetSize = sourceIndexSet.size();
			int sourceTargetIndexesSize = sourceTargetIndexes.size();
			
			double[] ratios = new double[]{
					((double) sourceTargetIndexesSize)/targetIndexSetSize,
					((double) sourceTargetIndexesSize)/sourceIndexSetSize
			}; 
			
			boolean accept = ratios[0]>ratiosThreshold && ratios[1]>ratiosThreshold;
			if (accept) {
				if (!printedSource) {
					pw.println("\t" + sourceKey);
					printedSource = true;
				}
				pw.println("\t\t" + targetKey + "\t" + sourceTargetIndexes.toString());
				selectedPairs++;
			}
			
			if (outputStats) {
				pwStats.println("\t\t" + targetKey +
					"\t" +  (accept ? "+" : "-") + 
					"\t" +  Arrays.toString(ratios) +
					"\t" +  "Ratios(target/source):" + targetIndexSetSize +
					"\t" +  "TotTargetIndexCount:" + targetIndexSetSize +
					"\t" +  "TargetSourceKeysCount:" + sourceTable.size() + // number of source keys mapped with targets
					"\t" +  "SourceTargetIndexCount:" + sourceTargetIndexesSize +
					"\t" +  "SourceTargetIndexes:" + sourceTargetIndexes);
			}
		}
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputGzFile = new File(args[0]);
		String threshold = args[1];
		new PruneTable_RatiosThreshold(inputGzFile, threshold);
	}

}
