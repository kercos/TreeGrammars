package tsg.metrics;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;

public abstract class ParseMetricOptimizer {

	static double minProb = 1e-100;//0d; //1e-10;		
	public static Label topLabel = Label.getLabel("TOP");
	
	String[] currentSentenceWords;
	Label[] lexicalItems;
	ArrayList<TSNodeLabel> currentBestTrees;
	int sentenceLength;	
	String identifier;
	
	
	public ParseMetricOptimizer() {
		currentBestTrees = new ArrayList<TSNodeLabel>();
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public String[] getCurrentSentenceWords() {
		return currentSentenceWords;
	}
	
	public void prepareNextSentence(String[] lexicalItems) {
		currentSentenceWords = lexicalItems;
		this.lexicalItems = getLabelArray(lexicalItems);
		sentenceLength = lexicalItems.length;
		initSentence();
	}
	
	protected abstract void initSentence();	
		
	public void addNewDerivation(TSNodeLabel tree, double prob) {
		if (prob<=0) prob = minProb;		
		addNewDerivationChecked(tree, prob);
	}
	
	protected abstract void addNewDerivationChecked(TSNodeLabel tree, double prob);
	
	
	protected abstract TSNodeLabel getBestTree() throws Exception;
	
	
	public void storeCurrentBestParseTrees() throws Exception {
		TSNodeLabel bestTree = getBestTree();
		if (bestTree==null) {
			Parameters.reportError("Error in storeCurrentBestParseTrees(): bestTree==null");
			return;
		}
		currentBestTrees.add(bestTree);		
	}
	
	public void prinBestTrees(OutputStreamWriter pw) throws Exception {
		for(TSNodeLabel t : currentBestTrees) {
			pw.write(t.toString() + "\n");
		}
	}	
	
	public static Label[] getLabelArray(String[] stringArray) {
		int length = stringArray.length;
		Label[] result = new Label[length];
		for(int i=0; i<length; i++) {
			result[i] = Label.getLabel(stringArray[i]);
		}
		return result;
	}

	

	
}
