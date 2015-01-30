package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class FilterFragmentsInTree {

	public static void main(String[] args) throws Exception {
		
		args = new String[]{
			"./FedeTB_2Dop/Segmentation/fragLogProb.txt",	
			"(TOP (S|TOP (S|TOP@_VP|S (NP|S (NP|S@_NN|NP (DT|NP A) (NN|NP record)) (NN|NP date)) (VP|S (VP|S@_RB|VP (VBZ|VP has) (RB|VP n't)) (VP|VP (VBN|VP been) (VP|VP (VBN|VP set))))) (.|S .)))",
			"./FedeTB_2Dop/fragments_singleSentence_logProb.txt",
		};
		
		File fragmentFile = new File(args[0]);
		String treeString = args[1];
		File outputFile = new File(args[2]);
		TSNodeLabel tree = new TSNodeLabel(treeString);		
		Scanner scan = FileUtil.getScanner(fragmentFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String fragLine = line.split("\t")[0];
			TSNodeLabel frag = new TSNodeLabel(fragLine, false);
			if (tree.containsRecursiveFragment(frag)) {
				pw.println(line);
			}
		}
		pw.close();
	}
	
}
