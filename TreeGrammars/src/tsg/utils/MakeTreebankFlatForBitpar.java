package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class MakeTreebankFlatForBitpar {

	public static void main(String[] args) throws Exception {
		
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		int lengthLimit = -1;
		if (args.length>2) 
			lengthLimit = Integer.parseInt(args[2]);
		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		
		if (lengthLimit==-1) {			
			for(TSNodeLabel t : treebank) {
				String[] words = t.toFlatWordArray();
				for(String w : words) {
					pw.println(w);
				}
				pw.println();
			}
		}
		else {
			for(TSNodeLabel t : treebank) {
				String[] words = t.toFlatWordArray();
				if (words.length>lengthLimit)
					continue;
				for(String w : words) {
					pw.println(w);
				}
				pw.println();
			}
		}
		
		pw.close();
		
	}
	
}
