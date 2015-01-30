package tsg.metrics;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;

public class MPP extends ParseMetricOptimizer{

	Hashtable<String, double[]> treeStringProbTable;
	
	public MPP() {
		super();
		this.identifier = "MPP";
	}
	
	protected void initSentence() {
		treeStringProbTable = new Hashtable<String, double[]>();
	}
	
		
	public void addNewDerivationChecked(TSNodeLabel tree, double prob) {
		String treeString = tree.toString();
		Utility.increaseInTableDoubleArray(treeStringProbTable, treeString, prob);
	}
	
	
	public TSNodeLabel getBestTree() throws Exception {
		String bestTreeString = Utility.getMaxKey(treeStringProbTable);
		TSNodeLabel bestTree = new TSNodeLabel(bestTreeString);
		ArrayList<TSNodeLabel> bestTreeLex = bestTree.collectLexicalItems();
		Iterator<TSNodeLabel> iterBest = bestTreeLex.iterator();				
		for(Label originalLabel : lexicalItems) {
			iterBest.next().label = originalLabel;
		}
		return bestTree;
	}
		
}
