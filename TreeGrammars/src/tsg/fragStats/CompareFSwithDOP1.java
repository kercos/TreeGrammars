package tsg.fragStats;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.PrintProgress;
import util.file.FileUtil;

public class CompareFSwithDOP1 {

	private static void makeComparison(File treebankFile, 
			File fragmentFile, File outputFile) throws Exception {
		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		ArrayList<TSNodeLabel> fragments = getFragmentsInFile(fragmentFile);
		System.out.println("Number of trees in treebank: " + treebank.size());
		PrintProgress pp = new PrintProgress("Counting from Tree:");
		for(TSNodeLabel tree : treebank) {
			BigInteger allFragments = tree.countTotalFragments()[0];
			int recurrentFragments = countFragmnets(tree, fragments);
			pw.println(allFragments + "\t" + recurrentFragments);
			pp.next();
		}
		pp.end();
		pw.close();
		
	}
	
	private static int countFragmnets(TSNodeLabel tree,
			ArrayList<TSNodeLabel> fragments) {
		
		int count = 0;
		for(TSNodeLabel f : fragments) {
			if (tree.containsRecursiveFragment(f))
				count++;
		}
		return count;
	}

	public static ArrayList<TSNodeLabel> getFragmentsInFile(File fragmentFile) throws Exception {
		
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		Scanner scan = FileUtil.getScanner(fragmentFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			//int freq = Integer.parseInt(lineSplit[1]);
			TSNodeLabel fragment = new TSNodeLabel(lineSplit[0], false);
			result.add(fragment);
		}		
		return result;
		
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File treebankFile = new File(args[0]);
		File fragmentFile = new File(args[1]);
		File outputFile = new File(args[2]);
		makeComparison(treebankFile, fragmentFile, outputFile);
	}

	

}
