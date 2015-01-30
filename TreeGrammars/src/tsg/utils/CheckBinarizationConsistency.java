package tsg.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import tsg.TSNodeLabel;

public class CheckBinarizationConsistency {

	
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
		while(iter1.hasNext()) {
			index++;
			TSNodeLabel t1 = iter1.next();
			TSNodeLabel t2 = iter2.next();
			if (!hasConsistentBinarization(t1,t2)) {
				System.out.println("Index: " + index);
				System.out.println(t1);
				System.out.println(t2);
				break;
			}
		}					
	}
	
	public static boolean hasConsistentBinarization(TSNodeLabel t1, TSNodeLabel t2) {		
		if (t1.isLexical != t2.isLexical) return false;
		boolean terminal = t1.isTerminal(); 
		if (terminal != t2.isTerminal()) return false;
		if (terminal) {
			int prole = t1.prole();
			if (prole != t2.prole()) {
				return false;
			}
			for(int i=0; i<prole; i++) {
				if (!hasConsistentBinarization(t1.daughters[i],t2.daughters[i])) {
					return false;
				}
			}
		}
		return true;
	}
	
}
