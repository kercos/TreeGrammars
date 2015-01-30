package kernels.parallel;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;

import settings.Parameters;
import util.IdentityArrayList;
import util.PrintProgress;
import util.file.FileUtil;

public class PruneTable_Singletong {
	
	//static double overlapFactor = 0.5;
	
	HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> reversedTable;
	File inputFile;
	File systemLogFile;
	File outputFileSingleton;
	int singletons;

	public PruneTable_Singletong(File inputTable, File outputFile) {
		this.inputFile = inputTable;
		outputFileSingleton =  FileUtil.changeExtension(outputFile, "sing.gz");;
		systemLogFile = FileUtil.changeExtension(outputFile, "log");
		
		Parameters.openLogFile(systemLogFile);
		Parameters.logStdOutPrintln("Prune Table Heurisitcs");
		Parameters.logStdOutPrintln("Input File: " + inputTable);
		Parameters.logStdOutPrintln("Output Singleton File: " + outputFileSingleton);
		
		reversedTable = ReverseTable.reverseTable(inputTable);
		
		pruneTable();
		
		Parameters.logStdOutPrintln("Number of keys in singleton: " + singletons);
		
		Parameters.closeLogFile();
	}
	


	private void pruneTable() {
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		PrintWriter pwSing = FileUtil.getGzipPrintWriter(outputFileSingleton);
		
		PrintProgress pp = new PrintProgress("Reading and pruning table", 10000, 0);
		IdentityArrayList<String> key=null, value=null;
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
		int[] indexes = null;				
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) {
				pwSing.println(line);
				continue; //2:
			}				 
			if (split.length==2) {
				//        [check, for] //  and new implementation 
				if (key!=null) {
					simplify(key, subTable, pwSing);
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
		simplify(key, subTable, pwSing);		
		
		pp.end();
		
		pwSing.close();
	}

	private void simplify(IdentityArrayList<String> sourceKey,
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> targetTable,
			PrintWriter pwSingleton) {
		
		int souceTargetKeysCount = targetTable.size();
						
		for(Entry<IdentityArrayList<String>, TreeSet<Integer>> e : targetTable.entrySet()) {
			IdentityArrayList<String> targetKey = e.getKey();
			TreeSet<Integer> sourceTargetIndexes = e.getValue();			

			HashMap<IdentityArrayList<String>, TreeSet<Integer>> sourceTable = reversedTable.get(targetKey);
			if (souceTargetKeysCount==1 && sourceTable.size()==1) {
				pwSingleton.println("\t" + sourceKey);
				pwSingleton.println("\t\t" + targetKey + "\t" + sourceTargetIndexes);
				singletons++;
			}
		}
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File inputGzFile = new File(args[0]);
		File outputGzFile = new File(args[1]);
		new PruneTable_Singletong(inputGzFile, outputGzFile);
	}

}
