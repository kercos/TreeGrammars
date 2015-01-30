package tsg.parser;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class GetStartingRules {

	public static String topSymbol = "TOP";
	
	public static void main(String[] args) throws Exception {
		HashSet<String> initialSymbols = new HashSet<String>();
		File treebankFile = new File(args[0]);
		File outputFile = new File(args[1]);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		for(TSNodeLabel t : treebank) {
			initialSymbols.add(t.label());
		}
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(String s : initialSymbols) {
			pw.println(topSymbol + " " + s);
		}
		pw.close();
	}
	
}
