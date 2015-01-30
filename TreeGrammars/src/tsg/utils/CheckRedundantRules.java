package tsg.utils;

import java.io.File;
import java.util.ArrayList;

import tsg.TSNodeLabel;

public class CheckRedundantRules {

	
	public static void main(String[] args) throws Exception {
		File inputTreebank = new File(args[0]);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputTreebank);
		int i=0;
		int foundLoops = 0;
		for(TSNodeLabel t : treebank) {
			i++;
			if (t.containsLoops()) {
				foundLoops++;
				System.out.println("Loop found at index: " + i);
				System.out.println(t);
			}
		}
		if (foundLoops==0) {
			System.out.println("No loop found!");
		}
		else {
			System.out.println("\nFound totla loops: " + foundLoops);			
		}
	}
	
}
