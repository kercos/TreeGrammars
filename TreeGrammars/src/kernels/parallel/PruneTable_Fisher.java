package kernels.parallel;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Map.Entry;

import settings.Parameters;
import util.FisherExact;
import util.IdentityArrayList;
import util.PrintProgress;
import util.PrintProgressPercentage;
import util.Utility;
import util.file.FileUtil;

public class PruneTable_Fisher {
	
	HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>> inputTable;
	HashMap<IdentityArrayList<String>,TreeSet<Integer>> targetTable;
	int totalIndexes;
	int totalTableEntries;
	FisherExact fe;
	double pruningThreshold;

	public PruneTable_Fisher(File inputFile, File outputFile) {
		File systemLogFile = FileUtil.changeExtension(outputFile, "log");
		File outputFileWithValues = FileUtil.changeExtension(outputFile, "prunedValues.gz");
		Parameters.openLogFile(systemLogFile);	
		inputTable = new HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>,TreeSet<Integer>>>();
		targetTable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
		readFile(inputFile);
		
		totalIndexes = ParallelSubstrings.getTotalUniqueIndexes(inputTable);
		Parameters.logStdOutPrintln("Total Indexes: " + totalIndexes);		
		fe = new FisherExact(totalIndexes);				
		pruningThreshold = fe.getTwoTailedP(2,0,0,totalIndexes-2);	
		Parameters.logStdOutPrintln("Pruning threshold (2,0,0,N): " + pruningThreshold);
		
		Parameters.logStdOutPrintln("Printing table with pruning values: " + outputFileWithValues);
		Parameters.logStdOutPrintln("Printing pruned table to file: " + outputFile);		
		pruneAndPrintTable(outputFile, outputFileWithValues);
		Parameters.closeLogFile();	
	}
	
	private void readFile(File inputFile) {
		PrintProgress pp = new PrintProgress("Reading table", 10000, 0);
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		IdentityArrayList<String> key=null, value=null;
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = null;
		int[] indexes = null;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //new implementation
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] // new implementation only has these lines
				key =  ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				subTable = inputTable.get(key);
				if (subTable==null) {
					subTable = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
					inputTable.put(key, subTable);
				}
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			totalTableEntries++;
			value = ParallelSubstrings.getIdentityArrayListFromBracket(split[2]);
			indexes = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);			
			TreeSet<Integer> valueSet = new TreeSet<Integer>();					
			subTable.put(value, valueSet);
			TreeSet<Integer> targetSet = targetTable.get(value);
			if (targetSet==null) {
				targetSet = new TreeSet<Integer>();
				targetTable.put(value, targetSet);
			}
			for(int i : indexes) {
				valueSet.add(i);
				targetSet.add(i);
			}			
		}
		pp.end();		
	}

	private void pruneAndPrintTable(File outputFile, File outputFileWithValues) {
		int initialKeys = inputTable.keySet().size();
		int initialSubKeys = totalTableEntries;
		Parameters.logStdOutPrintln("Initial keys, and subkeys: " + initialKeys + ", " + initialSubKeys);
		PrintProgressPercentage progress = new PrintProgressPercentage("Pruning end prining table", 10000, 0, totalTableEntries);
		PrintWriter pw = FileUtil.getGzipPrintWriter(outputFile);
		PrintWriter pw_values = FileUtil.getGzipPrintWriter(outputFileWithValues);
		int size = inputTable.size();
		int count = 0;
		int removedKeys = 0, removedSubKeys = 0;
		for(int i=0; i<10000; i++) {	// order by the length of the source substring		
			boolean foundSize = false;
			for( Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> e : inputTable.entrySet()) {
				IdentityArrayList<String> source = e.getKey();										
				int length = source.size();
				if (length==i) {
					count++;
					if (!foundSize) {
						pw.println(length + ":");
						pw_values.println(length + ":");
						foundSize = true;
					}					
					HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = e.getValue();					
					HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTableKeep = new HashMap<IdentityArrayList<String>, TreeSet<Integer>>();
					TreeSet<Integer> sourceSet = ParallelSubstrings.getAllIndexes(subTable); //remove
					int Cs = sourceSet.size(); //remove
					pw_values.println("\t" + source);
					for(Entry<IdentityArrayList<String>, TreeSet<Integer>> f : subTable.entrySet()) {
						progress.next();
						IdentityArrayList<String> target = f.getKey();
						TreeSet<Integer> valueSet = f.getValue();
						TreeSet<Integer> targetSet = targetTable.get(target);
						int[] abcd = getContingencyTable(valueSet.size(), Cs, targetSet.size());  //C(s,t), C(s), C(t)
						double p = fe.getTwoTailedP(abcd[0],abcd[1],abcd[2],abcd[3]);
						if (p>pruningThreshold) {
							totalTableEntries--;
							removedSubKeys++;							
							pw_values.println("\t\t" + target + "\t" + "-" + "\t" + valueSet.toString() + "\t" + Arrays.toString(abcd) + "\t" + p);
						}
						else {
							subTableKeep.put(target, valueSet);
							pw_values.println("\t\t" + target + "\t" + "+" + "\t" + valueSet.toString() + "\t" + Arrays.toString(abcd) + "\t" + p);							
						}
						
					}					
					if (!subTableKeep.isEmpty()) {
						pw.println("\t" + source);
						for(Entry<IdentityArrayList<String>, TreeSet<Integer>> f : subTableKeep.entrySet()) {
							pw.println("\t\t" + f.getKey() + "\t" + f.getValue().toString());
						}
					}
					else {
						removedKeys++;
					}
				}								
			}
			if (count==size)
				break;
		}
		progress.end();
		pw.close();		
		pw_values.close();
		int totalKeys = inputTable.keySet().size()-removedKeys;
		Parameters.logStdOutPrintln("Final keys and subkeys: " + totalKeys + ", " + totalTableEntries);
		Parameters.logStdOutPrintln("Removed keys and subkeys: " + removedKeys + ", " + removedSubKeys);
		Parameters.logStdOutPrintln("Removed keys and subkeys: " + 
				Utility.percentage(removedKeys,initialKeys) + ", " + Utility.percentage(removedSubKeys,initialSubKeys));
		
	}
	
	public int[] getContingencyTable(int st, int s, int t) {
		return getContingencyTable(st, s, t, totalIndexes);
	}
	
	public static int[] getContingencyTable(int st, int s, int t, int N) {
		int a = st; // C(s,t)
		int b = s - st; //C(s) - C(s,t)
		int c = t - st; //C(t) - C(s,t)
		int d = N - s - t + st; //N - C(s) - C(t) + C(s,t) 
		return new int[]{a,b,c,d};
	}
	
	public static double getFisherP(FisherExact fe, int st, int s, int t, int N) {
		int[] abcd = getContingencyTable(st, s, t, N);
		return fe.getTwoTailedP(abcd[0],abcd[1],abcd[2],abcd[3]);
	}
	

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		new PruneTable_Fisher(inputFile, outputFile);
		
		/*
		String dir = "/gardner0/data/Results/ParalleSubstring_TED_Words_Min2/";
		File inputFile = new File(dir + "finalTable.gz");
		File outputFile = new File(dir + "finalTable_pruneScores.gz");
		new PruneTable(inputFile, outputFile);
		*/
		
	}
}
