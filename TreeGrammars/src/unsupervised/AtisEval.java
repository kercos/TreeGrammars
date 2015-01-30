package unsupervised;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import tsg.Constituency;
import tsg.ConstituencyWords;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Atis2;
import tsg.corpora.Atis3;
import util.Utility;

public class AtisEval {
	
	public static void removeUnaryConst(ArrayList<Constituency> constSet) {
		Iterator<Constituency> iter = constSet.iterator();
		while(iter.hasNext()) {
			ConstituencyWords cw = (ConstituencyWords)iter.next();
			if (cw.getInitialIndex()==cw.getFinalIndex()) {
				iter.remove();
			}
		}
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String traceSymbol = "XXX";
		Label Xlabel = Label.getLabel("X");
				
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(Atis2.AtisCleanNoTraces, 10);
		//ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(Atis3.AtisCleanNoTraces);
		
		int[][] combinations = Utility.combinations(new int[]{3,2,2,2});
		
		for(int[] settings : combinations) {

			double totalTestBrackets = 0;
			double totalGoldBrackets = 0;
			double totalMatchedBrackets = 0;
			double totalFScores = 0;
			
			boolean pruneTraceBeforeBinarizing = settings[0]==1;
			boolean pruneTraceAfterBinarizing = settings[0]==2;
			boolean kleinStyle = settings[1]==1;
			boolean removeUnary = settings[2]==1;
			boolean removeTopBracket = settings[3]==1;
			
			/*
			boolean pruneTraceBeforeBinarizing = false;
			boolean pruneTraceAfterBinarizing = false;
			boolean kleinStyle = true;
			boolean removeUnary = false;
			boolean removeTopBracket = true;
			*/
			
			for(TSNodeLabel goldTree : treebank) {
				
				if (pruneTraceBeforeBinarizing) {
					goldTree.pruneSubTrees(traceSymbol);
				}
				
				ArrayList<TSNodeLabel> terminals = goldTree.collectPreLexicalItems();			
				TSNodeLabel rightBranching = kleinStyle ?
						TSNodeLabel.makeRightBranchingKlein(terminals, Xlabel, Xlabel) :
						TSNodeLabel.makeRightBranchingYoav(terminals, Xlabel, Xlabel);
				
				if (pruneTraceAfterBinarizing) {
					goldTree.pruneSubTrees(traceSymbol);		
					rightBranching.pruneSubTrees(traceSymbol);
				}
						
				ArrayList<Constituency> goldConst = Constituency.collectConsituencies(
						goldTree, false, new String[]{}, 0, false);
				ArrayList<Constituency> testConst = Constituency.collectConsituencies(
						rightBranching, false, new String[]{}, 0, false);
				
				if (removeUnary) {
					removeUnaryConst(goldConst);
					removeUnaryConst(testConst);
				}
				
				ArrayList<Constituency> goldConstCopy = new ArrayList<Constituency>(goldConst); 						
				ArrayList<Constituency> matchedConst = new ArrayList<Constituency>();
				for(Constituency c : testConst) {
					if (goldConstCopy.remove(c)) {
						matchedConst.add(c);
					}
				}		
				
				int goldBrackets = goldConst.size();
				int testBrackets = testConst.size();	
				int matchedBrackets = matchedConst.size();				
				
				if (removeTopBracket) {
					goldBrackets--;
					testBrackets--;
					matchedBrackets--;
				}
						
				totalTestBrackets += testBrackets;
				totalGoldBrackets += goldBrackets;
				totalMatchedBrackets += matchedBrackets;
				
				double treeRecall = (goldBrackets==0) ? 1 : (double)matchedBrackets / goldBrackets;
				double treePrecision = (testBrackets==0) ? 1 : (double)matchedBrackets / testBrackets;
				double treRecallPlusPrecision = treeRecall + treePrecision;
				double treeFScore = (treRecallPlusPrecision==0) ? 1 : 
					2 * treeRecall * treePrecision / (treeRecall + treePrecision);
				
				totalFScores += treeFScore;
				
				//System.out.println("Tree f-score:" + treeFScore);
	
			}
			
			double microRecall = totalMatchedBrackets / totalGoldBrackets;
			double microPrecision = totalMatchedBrackets / totalTestBrackets;
			double microFScore = 2 * microRecall * microPrecision / (microRecall + microPrecision);		
			
			double macroFScore = totalFScores / treebank.size();
			//System.out.println("Micro recall:" + microRecall);
			//System.out.println("Micro precision:" + microPrecision);
			//System.out.println("Micro f-score:" + microFScore);
			
			//System.out.println("Macro f-score:" + macroFScore);
			
			//System.out.println(microFScore+ "\t" + Arrays.toString(settings) );
			System.out.println(macroFScore+ "\t" + Arrays.toString(settings) );
		}
		
	}

}
