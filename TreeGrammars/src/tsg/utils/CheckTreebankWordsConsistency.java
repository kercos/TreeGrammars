package tsg.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import tsg.TSNodeLabel;

public class CheckTreebankWordsConsistency {

	
	public static void main(String[] args) throws Exception {
		File treebankFile1 = new File(args[0]);
		File treebankFile2 = new File(args[0]);
		ArrayList<TSNodeLabel> treebank1 = TSNodeLabel.getTreebank(treebankFile1);
		ArrayList<TSNodeLabel> treebank2 = TSNodeLabel.getTreebank(treebankFile2);
		if (treebank1.size() != treebank2.size()) {
			System.err.println("Sizes differ:");
			System.err.println(treebankFile1 + " -> " + treebank1.size());
			System.err.println(treebankFile2 + " -> " + treebank2.size());
			return;
		}
		Iterator<TSNodeLabel> iter1 = treebank1.iterator();
		Iterator<TSNodeLabel> iter2 = treebank2.iterator();
		int index = 0;
		boolean mistake = false;
		while(iter1.hasNext()) {
			index++;
			TSNodeLabel t1 = iter1.next();
			TSNodeLabel t2 = iter2.next();
			if (!t1.toFlatSentence().equals(t2.toFlatSentence())) {
				System.out.println("Inconsistent at index: " + index);
				System.out.println(t1.toFlatSentence());
				System.out.println(t2.toFlatSentence());
				mistake = true;
				break;
			}
		}					
		if (!mistake)
			System.out.println("OK!");
	}

	
}
