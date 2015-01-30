package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.file.FileUtil;

public class MakeTreebankCharBased {
	
	public static void makeCharBased(File inputFile, File outputFile, int LL) throws Exception {		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile, LL);
		for(TSNodeLabel t : treebank) {
			t.makeTreeCharBased();			
			pw.println(t.toString());
		}
		pw.close();
	}

	public static void main(String[] args) throws Exception {
		
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		int LL = Integer.MAX_VALUE;
		if (args.length==3) {
			LL = Integer.parseInt(args[2]);
		}
		makeCharBased(inputFile, outputFile, LL);		
	}
	
}
