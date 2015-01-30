package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.file.FileUtil;

public class ReplaceLexWithPos {
	
	public static void replaceLex(File inputFile, File outputFile) throws Exception {		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		for(TSNodeLabel t : treebank) {
			t.replaceLexWithPos();			
			pw.println(t.toString());
		}
		pw.close();
	}

	public static void main(String[] args) throws Exception {
		
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		replaceLex(inputFile, outputFile);		
	}
	
}
