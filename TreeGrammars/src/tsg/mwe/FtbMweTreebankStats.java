package tsg.mwe;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import tsg.Label;
import tsg.TSNodeLabel;
import util.PrintProgress;
import util.Utility;

public class FtbMweTreebankStats {

	static String MWE_Regex = "^MW.*";  
	
	File inputFile, outputFile;
	TSNodeLabel longestMwe;
	HashMap<TSNodeLabel, int[]> mweStructFreq = new HashMap<TSNodeLabel, int[]>();
	HashMap<Integer, HashSet<TSNodeLabel>> mweLengthTable = new HashMap<Integer, HashSet<TSNodeLabel>>();
	HashSet<String> listOfMWE = new HashSet<String>();
	
	public FtbMweTreebankStats(File inputFile, File outputFile) throws Exception {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		readTreebank();
		printStats(20, 5);
		
	}
	
	private void readTreebank()  throws Exception {
		PrintProgress progress = new PrintProgress("Reading trees", 1, 0);
		Scanner scan = new Scanner(inputFile);
		//HashMap<Label, HashSet<TSNodeLabel>> mweTable = new HashMap<Label,HashSet<TSNodeLabel>>();
		int totalMwes = 0;
		int maxLength = -1;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			progress.next();
			ArrayList<TSNodeLabel> mweNodes = tree.collectInternalNodesMatching(MWE_Regex);
			totalMwes += mweNodes.size();
			for(TSNodeLabel n : mweNodes) {
				listOfMWE.add(n.label());
				int l = n.countLexicalNodes(); 
				if (l>maxLength) {
					maxLength = l;
					longestMwe = n;
				}
				Utility.putInHashMap(mweLengthTable, l, n);
				Utility.increaseInHashMap(mweStructFreq, n, 1);
			}
		}
		progress.end();		
	}

	private void printStats(int n, int m) throws Exception {		
		//System.out.println("Longest MWE: " + longestMwe.toString());
		PrintWriter pw = new PrintWriter(outputFile);
		
		pw.println("Total MWE labels: " + listOfMWE.size());
		pw.println("List MWE labels: " + listOfMWE);
		pw.println("Total MWE frag types: " + Utility.countAllSubElements(mweLengthTable));
		
		pw.println("\n\n");
		
		pw.println("Printing MWE length stats");
		pw.println("Length\tTypes");
		for(Entry<Integer, HashSet<TSNodeLabel>> e : mweLengthTable.entrySet()) {
			pw.println(e.getKey() + "\t" + e.getValue().size());
		}
		
		pw.println("\n\n");
		
		pw.println("Printing the longest " + n + " MWEs in treebank");
		pw.println("Length\tFreq\tMWE");
		TreeSet<Integer> lengthSet = new TreeSet<Integer>(mweLengthTable.keySet());
		Iterator<Integer> iter = lengthSet.descendingIterator();
		int count = 0;
		outer: while(iter.hasNext()) {
			Integer l = iter.next();
			HashSet<TSNodeLabel> mweLengthSet = mweLengthTable.get(l);
			for(TSNodeLabel t : mweLengthSet) {
				int f = mweStructFreq.get(t)[0];
				pw.println(l + "\t" + f + "\t" + t);
				if (++count==n)
					break outer;
			}						
		}
		
		pw.println("\n\n");
		
		HashMap<TSNodeLabel, Integer> tableInt = Utility.convertHashMapIntArrayInteger(mweStructFreq);
		TreeMap<Integer, HashSet<TSNodeLabel>> tableIntReverse = Utility.reverseAndSortTable(tableInt);
		int max = tableIntReverse.descendingKeySet().iterator().next();
		int totalFragGe2 = 0;
		for(int i=2; i<=max; i++) {
			HashSet<TSNodeLabel> s = tableIntReverse.get(i);
			if (s!=null)
				totalFragGe2 += s.size();
		}
		
		pw.println("Total MWEs frag types with freq > 2: " + totalFragGe2);
		
		pw.println("\n\n");
		
		pw.println("Printing MWEs with freq > 2");
		pw.println("Freq\tFrag\tMWE");
		for(int i=2; i<=max; i++) {
			HashSet<TSNodeLabel> s = tableIntReverse.get(i);
			if (s!=null) {
				for(TSNodeLabel f : s) {
					pw.println(i + "\t" + f.toString(false, true));
				}
			}				
		}
		
		pw.println("\n\n");
		
		pw.println("Printing " + m + " MWEs per length");
		pw.println("Length\tFreq\tMWE");
		iter = lengthSet.descendingIterator();		
		while(iter.hasNext()) {
			Integer l = iter.next();
			HashSet<TSNodeLabel> mweLengthSet = mweLengthTable.get(l);
			count = 0;
			for(TSNodeLabel t : mweLengthSet) {
				int f = mweStructFreq.get(t)[0];
				pw.println(l + "\t" + f + "\t" + t.toString(false, true));
				if (++count==m)
					break;
			}			
			if (mweLengthSet.size()>m)
				pw.println(l + "\t" + "..." + "\t" + "...");
		}
		
		pw.close();
		
	}

	public static void main(String[] args) throws Exception {
		/*
		String workingPath = "/home/sangati/Work/FBK/TSG_MWE/FTB/Treebanks/";
		File inputFile = new File(workingPath + "ftbStanford/candito.cc.train");
		File outputFile = new File(workingPath + "mweStats/candito.train.mwe.stats");		
		new FtbMweStats(inputFile, outputFile);
		*/
		
		//String dir = "/home/sangati/Work/FBK/TSG_MWE/FTB/";
		//args = new String[]{dir+"ftbsplits.train.sorted.twolex.quotations.frag", dir+"stats/mweStats.txt"};
		
		String usage = "java FtbMweTreebankStats inputFile outputFile";
		if (args.length!=2) {
			System.err.println(usage);
			return;
		}
		new FtbMweTreebankStats(new File(args[0]), new File(args[1]));
		
		/*
		String a = "MWN";
		System.out.println(a.matches(MWE_Regex));
		*/
	}
	
}
