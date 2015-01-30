package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class RetainTreesLength {

	public static void main(String[] args) throws Exception {
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		int lengthLimit = Integer.parseInt(args[2]);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile, lengthLimit);
		TSNodeLabel.printTreebankToFile(outputFile, treebank, false, false);
	}
	
	public static void main1(String[] args) throws Exception {
		main1(new String[]{
			"tmp/wsj-24_eng_sm6.mrg",
			"tmp/wsj-24_eng_sm6_upTo40.mrg",
			"40"
		});
	}	
	
}
