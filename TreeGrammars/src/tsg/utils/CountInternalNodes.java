package tsg.utils;

import java.io.File;
import java.util.ArrayList;

import tsg.TSNodeLabel;

public class CountInternalNodes {

	
	public static void main(String[] args) throws Exception {
		File inputTreebank = new File(args[0]);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputTreebank);
		int total=0;		
		for(TSNodeLabel t : treebank) {			
			total += t.countNonLexicalNodes();
		}
		System.out.println("Total trees: " + treebank.size());
		System.out.println("Total non-lexical nodes: " + total);			
	}
	
	public int countInternalNodes(TSNodeLabel n) {		
		if (n.isPreLexical()) return 0;
		int result = n.parent==null ?  0 : 1;
		for(TSNodeLabel d : n.daughters) {
			result += countInternalNodes(d);
		}
		return result;
	}
	
}
