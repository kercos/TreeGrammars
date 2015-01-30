package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.TreeSet;

import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class TreebankNodesStats {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File treebankFile = new File(args[0]);
		File outputFile = new File(args[1]);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		Hashtable<Label, int[]> internalNodesStats = new Hashtable<Label, int[]>();
		Hashtable<Label, int[]> posNodesStats = new Hashtable<Label, int[]>();
		Hashtable<Label, int[]> lexNodesStats = new Hashtable<Label, int[]>();
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical)
					Utility.increaseInTableInt(lexNodesStats, n.label);
				else if (n.isPreLexical())
					Utility.increaseInTableInt(posNodesStats, n.label);
				else 
					Utility.increaseInTableInt(internalNodesStats, n.label);
			}
		}
		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		pw.println("Internal nodes stats:");
		printStats(internalNodesStats, pw);
		pw.println("\nPos nodes stats:");
		printStats(posNodesStats, pw);
		pw.println("\nLexical nodes stats:");
		printStats(lexNodesStats, pw);
		pw.close();

	}
	
	public static void printStats(Hashtable<Label, int[]> nodesStats, PrintWriter pw) {
		TreeSet<String> orderedStats = new TreeSet<String>();
		for(Entry<Label, int[]> e : nodesStats.entrySet()) {
			orderedStats.add(e.getKey() + "\t" + e.getValue()[0]);
		}
		for(String s : orderedStats) {
			pw.println(s);
		}
	}

}
