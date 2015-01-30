package kernels.parallel;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeSet;

import util.IdentityArrayList;
import util.PrintProgress;
import util.file.FileUtil;

public class TableStats {
	
	public static HashSet<IdentityArrayList<String>> getKeysValues(File inputFile, boolean keys) {
		
		HashSet<IdentityArrayList<String>> result = new HashSet<IdentityArrayList<String>>();		
		PrintProgress pp = new PrintProgress("Reading table", 10000, 0);
		Scanner scan = FileUtil.getGzipScanner(inputFile);
		IdentityArrayList<String> key=null, value=null;
		HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = null;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1)
				continue; //2: //new implementation
			if (split.length==2) {
				//2:      [of, climate] // inlcuding case of old implementation
				//        [check, for] // new implementation only has these lines				
				if (keys) {
					key =  ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
					result.add(key);
				}
				continue;
			}
			// split.length==4 //\t\t[che, per, la]  [23687, 34596, 186687]
			pp.next();
			if (!keys) {
				value = ParallelSubstrings.getIdentityArrayListFromBracket(split[2]);
				result.add(value);
			}
			
			//indexes = getIndexes(split[3]);			
		}
		pp.end();
		return result;
	}

	public static void main(String args[]) {
		//File inputFile = new File(args[0]);
		//int[] keysPairs = ParallelSubstrings.totalKeysSubKeys(inputFile);
		//System.out.println("Total keys and pairs: " + Arrays.toString(keysPairs));
		
		File inputFile = new File(args[0]);
		File inputFileReverse = new File(args[1]);
		HashSet<IdentityArrayList<String>> keys1 = getKeysValues(inputFile, true);
		HashSet<IdentityArrayList<String>> values2 = getKeysValues(inputFileReverse, false);
		System.out.println("Keys in file 1: " + keys1.size());
		System.out.println("Values in file 2: " + values2.size());
		if (keys1.size()>values2.size()) {
			keys1.removeAll(values2);
			System.out.println("Additional keys in file 1" + keys1);
		}
		else {
			values2.removeAll(keys1);
			System.out.println("Additional values in file 2" + values2);
		}
	}
	
}
