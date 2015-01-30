package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class CompareTreebanks {

	public static void main(String[] args) throws Exception {		
		File treebankFile1 = new File(args[0]);
		File treebankFile2 = new File(args[1]);
		
		ArrayList<TSNodeLabel> treebank1 = TSNodeLabel.getTreebank(treebankFile1);
		ArrayList<TSNodeLabel> treebank2 = TSNodeLabel.getTreebank(treebankFile2);
		if (treebank1.size()!=treebank2.size()) {
			System.out.println("TREEBANKS DIFFER IN SIZE.");
		}
		
		Iterator<TSNodeLabel> iter1 = treebank1.iterator();
		Iterator<TSNodeLabel> iter2 = treebank2.iterator();
		
		int index = 0;
		while(iter1.hasNext()) {
			index++;
			TSNodeLabel tree1 = iter1.next();
			TSNodeLabel tree2 = iter2.next();
			if (!tree1.equals(tree2)) {
				System.out.println("Differ at index: " + index);
				System.out.println(tree1.toString());
				System.out.println(tree2.toString());
				System.out.println();
			}
		}
	}
	
}
