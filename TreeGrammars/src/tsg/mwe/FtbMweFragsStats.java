package tsg.mwe;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;

import tsg.Label;
import tsg.TSNodeLabel;
import util.PrintProgress;
import util.Utility;

public class FtbMweFragsStats {

	static String[] MWE_list = new String[]{"MWC", "MWCL", "MWD", "MWADV", "MWI", "MWPRO", "MWV", "MWP", "MWET", "MWA", "MWN"}; 
	static {
		Arrays.sort(MWE_list);
	}
	
	File inputFile, outputFile;
	HashMap<Label, HashMap<TSNodeLabel, int[]>> fragFreqRootedOnMweLabel = new HashMap<Label, HashMap<TSNodeLabel,int[]>>();
	HashMap<TSNodeLabel, int[]> fragFreqContainingMweLabel = new HashMap<TSNodeLabel, int[]>();
	int totalFrags, totalFragRootedInMwes, totalFragContainigMwes;
	
	public FtbMweFragsStats(File inputFile, File outputFile) throws Exception {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		readFragments();
		printStats();
		
	}
	
	private void readFragments()  throws Exception {
		PrintProgress progress = new PrintProgress("Reading frags", 1, 0);
		Scanner scan = new Scanner(inputFile);
		//HashMap<Label, HashSet<TSNodeLabel>> mweTable = new HashMap<Label,HashSet<TSNodeLabel>>();
		totalFragRootedInMwes = 0;
		while(scan.hasNextLine()) {
			progress.next();
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			TSNodeLabel frag = new TSNodeLabel(lineSplit[0], false);
			int freq = Integer.parseInt(lineSplit[1]);			
			totalFrags++;
			if (Arrays.binarySearch(MWE_list, frag.label())>=0) {
				Utility.putInMapDouble(fragFreqRootedOnMweLabel, frag.label, frag, new int[]{freq});
				totalFragRootedInMwes++;
			}
			else {
				ArrayList<TSNodeLabel> mweNodes = frag.collectInternalNodesMatching(MWE_list);
				if (!mweNodes.isEmpty()) {
					fragFreqContainingMweLabel.put(frag, new int[]{freq});
					totalFragContainigMwes++;
				}				
			}
		}
		progress.end();		
	}

	private void printStats() throws Exception {		
		//System.out.println("Longest MWE: " + longestMwe.toString());
		PrintWriter pw = new PrintWriter(outputFile);
		
		pw.println("Total Frags: " + totalFrags);
		pw.println("Total Frags rooted on MWE: " + totalFragRootedInMwes);
		pw.println("Total Frags containing MWE: " + totalFragContainigMwes);
		
		pw.println("\n\n");
		
		pw.println("Printing frag stats rooted on MWE labels");
		pw.println("Root\tTypes");
		for(Entry<Label, HashMap<TSNodeLabel, int[]>> e : fragFreqRootedOnMweLabel.entrySet()) {
			pw.println(e.getKey() + "\t" + e.getValue().size());
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
		
		String dir = "/home/sangati/Work/FBK/TSG_MWE/FTB/";
		args = new String[]{dir+"ftbsplits.train.sorted.twolex.quotations.frag", dir+"stats/mwestats.ftbsplits.train.txt"};
		
		String usage = "java FtbMweFragsStats inputFile outputFile";
		if (args.length!=2) {
			System.err.println(usage);
			return;
		}
		new FtbMweFragsStats(new File(args[0]), new File(args[1]));
		
		
	}
	
}
