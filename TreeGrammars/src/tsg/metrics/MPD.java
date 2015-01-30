package tsg.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import tsg.Label;
import tsg.TSNodeLabel;

public class MPD extends ParseMetricOptimizer{

	TSNodeLabel firstTree;
	
	public MPD() {
		super();
		this.identifier = "MPD";
	}
	
	protected void initSentence() {		
		firstTree = null;
	}
	
		
	public void addNewDerivationChecked(TSNodeLabel tree, double prob) {
		if (firstTree==null) {
			firstTree = tree;
		}
	}
	
	
	public TSNodeLabel getBestTree() throws Exception {
		TSNodeLabel bestTree = firstTree;
		ArrayList<TSNodeLabel> bestTreeLex = bestTree.collectLexicalItems();
		/*
		if (sentenceLength!=bestTreeLex.size()) {
			System.err.println("Wrong lenght of sentence:");
			System.err.println("Original Lex: " + Arrays.toString(lexicalItems));
			System.err.println("Best Tree: " + bestTree);
			return null;
		}
		*/
		Iterator<TSNodeLabel> iterBest = bestTreeLex.iterator();		
		for(Label originalLabel : lexicalItems) {
			iterBest.next().label = originalLabel;
		}
		return bestTree;
	}
		
}
