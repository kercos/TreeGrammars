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

public class PruneTable_Stats {
	
	//static double overlapFactor = 0.5;
	
	HashMap<IdentityArrayList<String>,TreeSet<Integer>> sourceTotIndexSet;
	HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> reversedTable;
	File inputFile;
	File systemLogFile;
	File outputFileStats;
	int totalIndexes;
	FisherExact fe;
	double pruningThreshold;
	int singletons;

	public PruneTable_Stats(File inputFile, File outputFile) {
		this.inputFile = inputFile;
		systemLogFile = FileUtil.changeExtension(outputFile, "log");
		outputFileStats = FileUtil.changeExtension(outputFile, "stats.gz");
		
		Parameters.openLogFile(systemLogFile);
		Parameters.logStdOutPrintln("Prune Table Heurisitcs");
		Parameters.logStdOutPrintln("Input File: " + inputFile);
		Parameters.logStdOutPrintln("Output File: " + outputFileStats);
		
		reversedTable = ReverseTable.reverseTable(inputFile);
		totalIndexes = ParallelSubstrings.getTotalIndexes(reversedTable);
		
		Parameters.logStdOutPrintln("Total Indexes (N): " + totalIndexes);		
		fe = new FisherExact(totalIndexes);				
		pruningThreshold = fe.getTwoTailedP(2,0,0,totalIndexes-2);	
				
		Parameters.logStdOutPrintln("Pruning threshold (2,0,0,N-2): " + pruningThreshold);
		//Parameters.logStdOutPrintln("OverlapFactor: " + overlapFactor);
				
		buildSourceTotIndexSet();
		pruneTable();
		
		Parameters.logStdOutPrintln("Number of keys in singleton: " + singletons);
		
		Parameters.closeLogFile();
	}
	
	private void buildSourceTotIndexSet() {
		sourceTotIndexSet = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
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
					sourceTotIndexSet.put(key, indexSet);
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
		sourceTotIndexSet.put(key, indexSet);
		pp.end();
	}

	private void pruneTable() {
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		PrintWriter pwStats = FileUtil.getGzipPrintWriter(outputFileStats);
		
		PrintProgress pp = new PrintProgress("Reading and pruning table", 10000, 0);
		IdentityArrayList<String> key=null, value=null;
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
		int[] indexes = null;				
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) {
				pwStats.println(line);
				continue; //2:
			}				 
			if (split.length==2) {
				//        [check, for] //  and new implementation 
				if (key!=null) {
					simplify(key, subTable, pwStats);
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
		simplify(key, subTable, pwStats);		
		
		pp.end();
		
		pwStats.close();
	}

	private void simplify(IdentityArrayList<String> sourceKey,
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> targetTable,
			PrintWriter pwStats) {
		
		TreeSet<Integer> sourceIndexSet = ParallelSubstrings.getAllIndexes(targetTable);
		
		int souceTargetKeysCount = targetTable.size();
				
		pwStats.println("\t" + sourceKey +
				"\t" + "TotSourceIndexCount:" + sourceIndexSet.size() +
				"\t" + "TotTargetKeysCount:" + souceTargetKeysCount				
		);
		
		Vector<IdentityArrayList<String>> targetKeys = new Vector<IdentityArrayList<String>>(souceTargetKeysCount);		
		Vector<TreeSet<Integer>> souceTargetCommonIndexes =  new Vector<TreeSet<Integer>>(souceTargetKeysCount);
		Vector<HashMap<IdentityArrayList<String>, TreeSet<Integer>>> targetSourceTable =  
				new Vector<HashMap<IdentityArrayList<String>, TreeSet<Integer>>>(souceTargetKeysCount);
		Vector<TreeSet<Integer>> targetIndexesSet = new Vector<TreeSet<Integer>>(souceTargetKeysCount);
		Vector<int[]> targetStats = new Vector<int[]>(souceTargetKeysCount);
		Vector<Double> targetFisherP = new Vector<Double>(souceTargetKeysCount);
		
		for(Entry<IdentityArrayList<String>, TreeSet<Integer>> e : targetTable.entrySet()) {
			IdentityArrayList<String> targetKey = e.getKey();
			TreeSet<Integer> sourceTargetIndexes = e.getValue();			
			targetKeys.add(targetKey);
			souceTargetCommonIndexes.add(sourceTargetIndexes);
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> sourceTable = reversedTable.get(targetKey);
			targetSourceTable.add(sourceTable);			
			TreeSet<Integer> targetIndexSet = ParallelSubstrings.getAllIndexes(sourceTable);
			targetIndexesSet.add(targetIndexSet);
			int st = sourceTargetIndexes.size();
			int[] contTab = new int[]{
					st, // st: indexes in common between s and t
					sourceIndexSet.size() - st, // s: indexes of s without t
					targetIndexSet.size() - st, // t: indexes of t without s
					totalIndexes 
				}; 
			targetStats.add(contTab);
			double fisherP = PruneTable_Fisher.getFisherP(
					fe, st, sourceIndexSet.size(), targetIndexSet.size(), totalIndexes); 
			targetFisherP.add(fisherP);
			pwStats.println("\t\t" + targetKey + 
					"\t" +  "TotTargetIndexCount:" + targetIndexSet.size() +
					"\t" +  "TargetSourceKeysCount:" + sourceTable.size() + // number of source keys mapped with targets
					"\t" +  "SourceTargetIndexCount:" + sourceTargetIndexes.size() +
					"\t" +  "SourceTargetIndexes:" + sourceTargetIndexes +
					"\t" +  "ContTab:" + Arrays.toString(contTab) +
					"\t" +  "FisherP:" + fisherP
			);
			/*
			if (souceTargetKeysCount==1 && sourceTable.size()==1) {
				pwSingleton.println("\t" + sourceKey);
				pwSingleton.println("\t\t" + targetKey + "\t" + sourceTargetIndexes);
				singletons++;
			}
			*/
			/*
			for(Entry<IdentityArrayList<String>, TreeSet<Integer>> f : sourceTable.entrySet()) {				
				IdentityArrayList<String> targetSourceKey = f.getKey();
				if (targetSourceKey.equals(sourceKey))
					continue;
				TreeSet<Integer> totSourceIndex = sourceTotIndexSet.get(targetSourceKey);
				TreeSet<Integer> targetSourceSet = f.getValue();
				pwStats.println("\t\t\t" + targetSourceKey + 
						"\t" +  "TargetSourceIndexCount:" + targetSourceSet.size() +
						"\t" +  "TotSourceIndex:" + totSourceIndex.size() +
						"\t" +  "TargetSourceIndexes:" + targetSourceSet
				);
			}
			*/
		}
		
		/*
		boolean[] prune = new boolean[targetsCount];
		
		for(int i=0; i<targetsCount; i++) {
			if (prune[i])
				continue;
			IdentityArrayList<String> target_i = targetKeys.get(i);
			for(int j=i+1; j<targetsCount; j++) {
				if (prune[j])
					continue;
				IdentityArrayList<String> target_j = targetKeys.get(i);
				if (target_i.size()==target_j.size())
					continue;
				boolean reversed = false;				
				if (target_i.size()>target_j.size()) {
					reversed = true;
					IdentityArrayList<String> tmp = target_i;
					target_j = target_i;
					target_i = tmp;					
				}
				// target_i.size() < target_j.size()
				if (target_i.isSublistOf(target_j)) {
					
				}
			}
		}
		*/
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputGzFile = new File(args[0]);
		File outputGzFile = new File(args[1]);
		new PruneTable_Stats(inputGzFile, outputGzFile);
	}

}
