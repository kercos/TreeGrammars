package tsg.metrics;

import java.util.ArrayList;
import java.util.Iterator;

import tsg.Label;
import tsg.TSNodeLabel;
import tsg.parsingExp.TSGparsingBitPar;

public class SD extends ParseMetricOptimizer {

	final static String internalFakeNodeLabel = TSGparsingBitPar.internalFakeNodeLabel;
	
	TSGparsingBitPar tSGparsing;
	int shortestDer = Integer.MAX_VALUE;
	TSNodeLabel shortDerTree;
	int index;
	int nBest;
	
	public SD(TSGparsingBitPar tSGparsing, int nBest) {
		super();		
		this.identifier = "SD_" + nBest + "best";
		this.tSGparsing = tSGparsing;
		this.nBest = nBest;
	}

	@Override
	protected void initSentence() {		
		shortestDer = Integer.MAX_VALUE;
		shortDerTree = null;
		index = 0;
	}

	@Override
	protected void addNewDerivationChecked(TSNodeLabel tree, double prob) {
		if (++index > nBest) return;
		int derLength = getDerivationLength(tree);
		if (derLength<shortestDer) {
			shortestDer = derLength;
			shortDerTree = tSGparsing.postProcessParseTree(tree);
		}
	}
	
	public static int getDerivationLength(TSNodeLabel tree) {
		ArrayList<TSNodeLabel> nodes = tree.collectNonLexicalNodes();
		int result = 0;
		for(TSNodeLabel n : nodes) {
			if (n.label().startsWith(internalFakeNodeLabel))
				result++;
		}
		return result;
	}

	@Override
	protected TSNodeLabel getBestTree() throws Exception {
		ArrayList<TSNodeLabel> bestTreeLex = shortDerTree.collectLexicalItems();
		Iterator<TSNodeLabel> iterBest = bestTreeLex.iterator();		
		for(Label originalLabel : lexicalItems) {
			iterBest.next().label = originalLabel;
		}
		return shortDerTree;
	}

}
