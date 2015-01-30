package unsupervised;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tsg.Constituency;
import tsg.ConstituencyWords;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.parseEval.EvalC;

public class Wsj10Eval {
	
	public static void removeUnaryConst(HashSet<Constituency> constSet) {
		Iterator<Constituency> iter = constSet.iterator();
		while(iter.hasNext()) {
			ConstituencyWords cw = (ConstituencyWords)iter.next();
			if (cw.getInitialIndex()==cw.getFinalIndex()) {
				iter.remove();
			}
		}
	}
	
	
	public static HashSet<Constituency> getRightBranchingConstituencyYoav(
			int sentenceLength) {
		HashSet<Constituency> result = new HashSet<Constituency>();
		int lastWordIndex = sentenceLength - 1;
		for(int i=0; i<sentenceLength-1; i++) {
			TSNodeLabel node = new TSNodeLabel(Label.getLabel("X"), false);
			result.add(new ConstituencyWords(node, i, lastWordIndex, false));
		}
		return result;
	}
	
	public static HashSet<Constituency> getRightBranchingConstituencyKlein(
			int sentenceLength) {
		HashSet<Constituency> result = new HashSet<Constituency>();
		int lastWordIndex = sentenceLength - 1;
		for(int i=0; i<sentenceLength; i++) {
			TSNodeLabel node = new TSNodeLabel(Label.getLabel("X"), false);
			result.add(new ConstituencyWords(node, i, lastWordIndex, false));
		}
		return result;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		
		String traceSymbol = "-NONE-";
		Label Xlabel = Label.getLabel("X");
		//Label TOPlabel = Label.getLabel("TOP");
		
		String[] punctuation = new String[]{",", ".", ":", "''", "``", "-LRB-", "-RRB-", "$", "#"};
		//String[] punctuation = new String[]{",", ".", ":", "''", "``", "-LRB-", "-RRB-"};
		Arrays.sort(punctuation);
		
		//File wsj10File = new File("/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/WSJ10/wsj-singleLabelX.mrg");
		File wsj10File = new File("/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/WSJ10/wsj-originalReadable.mrg");		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(wsj10File);
		
		double totalFScores = 0;
		double totalRecalls = 0;
		double totalPrecisions = 0;
		double totalTestBrackets = 0;
		double totalGoldBrackets = 0;
		double totalMatchedBrackets = 0;
		
		for(TSNodeLabel goldTree : treebank) {
			
			goldTree.pruneSubTrees(punctuation);
			goldTree.pruneSubTrees(traceSymbol);
			
			ArrayList<TSNodeLabel> terminals = goldTree.collectPreLexicalItems();			
			TSNodeLabel rightBranching = TSNodeLabel.makeRightBranchingKlein(terminals, Xlabel, Xlabel);
			///TSNodeLabel rightBranching = TSNodeLabel.makeRightBranchingYoav(terminals, Xlabel, Xlabel);
			
			//System.out.println(goldTree);
			//System.out.println(rightBranching);
			
			HashSet<Constituency> goldConst = new HashSet<Constituency>(Constituency.collectConsituencies(
					goldTree, false, new String[]{}, 0, false));
			HashSet<Constituency> testConst = new HashSet<Constituency>(Constituency.collectConsituencies(
					rightBranching, false, new String[]{}, 0, false));
			
			removeUnaryConst(goldConst);
			removeUnaryConst(testConst);
			
			HashSet<Constituency> matchedConst = new HashSet<Constituency>(goldConst);
			matchedConst.retainAll(testConst);
			
			int goldBrackets = goldConst.size();
			int testBrackets = testConst.size();	
			int matchedBrackets = matchedConst.size();
					
			double recall = (matchedBrackets==0 && goldBrackets==0) ? 1 : (double)matchedBrackets / goldBrackets;
			double precision = (matchedBrackets==0 && testBrackets==0) ? 1 : (double)matchedBrackets / testBrackets;
			double fscore = (recall==0 && precision==0) ? 0 : 2 * recall * precision / (recall + precision);

			//System.out.println(Arrays.toString(result));
			//System.out.println("Precision: " + precision);
			//System.out.println("Recall: " + recall);
			//System.out.println("Fscore: " + fscore);
			if (Double.isNaN(fscore) || Double.isInfinite(fscore)) {
				System.out.println();				
			}
			
			totalFScores += fscore;
			totalRecalls += recall;
			totalPrecisions += precision;
			totalTestBrackets += testBrackets;
			totalGoldBrackets += goldBrackets;
			totalMatchedBrackets += matchedBrackets;
			//break;
		}
		
		int numberOfSentence = treebank.size();
		double averageFScore = totalFScores / numberOfSentence;
		double averageRecall = totalRecalls / numberOfSentence;
		double averagePrecision = totalPrecisions / numberOfSentence;
		System.out.println("Macro f-score:" + averageFScore);
		System.out.println("Macro recall:" + averageRecall);
		System.out.println("Macro precision:" + averagePrecision);
		double harmonic = 2 * averageRecall * averagePrecision / (averageRecall + averagePrecision);
		System.out.println("Harmonic mean Macro precision-recall:" + harmonic);
		double microRecall = totalMatchedBrackets / totalGoldBrackets;
		double microPrecision = totalMatchedBrackets / totalTestBrackets;
		double microFScore = 2 * microRecall * microPrecision / (microRecall + microPrecision);		
		System.out.println("Micro recall:" + microRecall);
		System.out.println("Micro precision:" + microPrecision);
		System.out.println("Micro f-score:" + microFScore);
		
	}



}
