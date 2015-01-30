package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_EisnerCBackoff_extended2 extends Reranker_ProbModelBackoff {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	
	// P(dist, w(d),t(d), term(d) | h, s, gh, dir)
	public Reranker_EisnerCBackoff_extended2(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int limitTestToFirst, boolean addEOS, boolean markTerminalNodes, 
			int[][][] backoffLevels, boolean[] skipEvents, int[][] groupEventLevels) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, markTerminalNodes, printTables, 
				backoffLevels, skipEvents, groupEventLevels);

		updateCondFreqTables();				
	}
	
	public static String[][] fillBackoffTagsTable(TDNode h, TDNode s, TDNode d, String dir, int dist) {
		TDNode gh = h.parent;
		String[][] result = new String[8][];
		//0: distance		
		//1: word(daughter)
		//2: tag(daughter)
		//3: d is terminal
		//-----
		//4: parent
		//5: previous sister
		//6: gran parent
		//7: direction
		result[0] = new String[]{Integer.toString(dist), ""};		
		result[1] = (d==null)? new String[]{EOC, ""} : new String[]{d.lexPosTag(1), ""};
		result[2] = (d==null)? new String[]{EOC, ""} : new String[]{d.lexPosTag(0), ""};
		result[3] = (d==null)? new String[]{EOC, ""} : 
			(d.isTerminalNode())? new String[]{"1", ""} : new String[]{"0", ""};
		//-----
		result[4] = standardReduction(h);
		result[5] = (s==null) ? 
					new String[]{nullNode, nullNode, nullNode, nullNode, nullNode, nullNode, ""} :
					standardReduction(s);
		result[6] = (gh==null) ? 
					new String[]{nullNode, nullNode, nullNode, nullNode, nullNode, nullNode, ""} :
					standardReduction(gh);			
		result[7] = new String[]{dir, ""};
		return result;		
	}
	
	public static String[] standardReduction(TDNode n) {
		/* r: open         | close
		 * ------------------
		 * 0: lexPos       | lexPos
		 * 1: lexShortPos  | lexPos
		 * 2: pos		   | lexPos
		 * 3: shortPos     | lexPos
		 * 4: shortPos     | pos
		 * 5: shortPos     | shortPos
		 * 6: no specified
		 */
		return new String[]{n.lexPosTag2(0,0), n.lexPosTag2(1,0), n.lexPosTag2(2,0),
				n.lexPosTag2(3,0), n.lexPosTag2(3,2), n.lexPosTag2(3,3), ""};
	}
	
	
	public void updateCondFreqTables(TDNode thisNode) {		
		String[][] backoffTagsTable = null;
		TDNode d = null;
		int dist;
			
		TDNode previousChild = null;
		if (thisNode.leftDaughters!=null) {
			for(int li = thisNode.leftProle()-1; li>=0; li--) {
				TDNode ld = thisNode.leftDaughters[li];
				dist = getDistance(thisNode,ld);
				backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, ld, left, dist);
				increaseInTables(backoffTagsTable);
				previousChild = ld;
				updateCondFreqTables(ld);				
			}
		}
		d = null;
		dist = groupDistance(thisNode.leftProle()+1);
		backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, d, left, dist);
		increaseInTables(backoffTagsTable);
		
		previousChild = null;
		if (thisNode.rightDaughters!=null) {
			for (TDNode rd : thisNode.rightDaughters) {		
				dist = getDistance(thisNode,rd);
				backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, rd, right, dist);
				increaseInTables(backoffTagsTable);
				previousChild = rd;
				updateCondFreqTables(rd);					
			}
		}
		d = null;
		dist = groupDistance(thisNode.rightProle()+1);
		backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, d, right, dist);
		increaseInTables(backoffTagsTable);
	}
	
	public double getProb(TDNode thisNode) {
		
		double prob = 1d;
		
		String[][] backoffTagsTable = null;
		TDNode d = null;
		int dist;
			
		TDNode previousChild = null;
		if (thisNode.leftDaughters!=null) {
			for(int li = thisNode.leftProle()-1; li>=0; li--) {
				TDNode ld = thisNode.leftDaughters[li];
				dist = getDistance(thisNode,ld);
				backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, ld, left, dist);
				prob *= getCondProb(backoffTagsTable) * getProb(ld);
				previousChild = ld;				
			}
		}
		d = null;
		dist = groupDistance(thisNode.leftProle()+1);
		backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, d, left, dist);
		prob *= getCondProb(backoffTagsTable);
		
		previousChild = null;
		if (thisNode.rightDaughters!=null) {
			for (TDNode rd : thisNode.rightDaughters) {		
				dist = getDistance(thisNode,rd);
				backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, rd, right, dist);
				prob *= getCondProb(backoffTagsTable) * getProb(rd);
				previousChild = rd;					
			}
		}
		d = null;
		dist = groupDistance(thisNode.rightProle()+1);
		backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, d, right, dist);
		prob *= getCondProb(backoffTagsTable);
		
		return prob;
	}
	
	public static int getDistance(TDNode head, TDNode dep) {
		int dis = Math.abs(head.index - dep.index);
		return groupDistance(dis);
	}
	
	public static int groupDistance(int dis) {
		if (dis==1) return 1;
		if (dis==2) return 2;
		if (dis<7) return 3;
		return 4;
	}
	
	public void updateKeyCondKeyLog(TDNode thisNode, Vector<String> keyCondKeyLog) {

	}
	
	/*
	 * Toy Sentence
	 */
	public static void main1(String[] args) {
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		int limitTestSetToFirst = 10000;
		
		String toySentence = Parameters.corpusPath + "ToyGrammar/toySentence";		
		
		Parameters.outputPath = Parameters.resultsPath + "Reranker/" + FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		File trainingFile = new File (toySentence);
		File testFile = new File (toySentence);	
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);
			
		File outputTestGold = new File(Parameters.outputPath + "toySentence.ulab");
		DepCorpus.toFileMSTulab(outputTestGold, test, false);
		
		File parsedFile = new File(toySentence); 
		
		boolean addEOS = true;
		boolean markTerminalNodes = true;
		

		int[][][] backoffLevels = new int[][][] {
				//dist | w(d), t(d), h, s, dir 
				{
					{0, 	0,    0, 2, 3, 1},
					{0, 	1,    0, 2, 3, 1},
				},
				 //w(d) | t(d), h, s, dir
				{	 
					{0,		 0, 0, 3, 0},
					{0,		 0, 2, 3, 0},
					{0,		 0, 3, 3, 1},
				},
				{//t(d) | h, s, dir
					{0,	  0, 2, 0},
					{0,	  2, 2, 0},
					{0,	  2, 3, 0},
				}, 
		};
		
		boolean[] skipEvents = new boolean[]{true, false, false};
		int[][] groupEventLevels = new int[][]{{1,1},{1,1,1},{1,1,1}};
		
		new Reranker_EisnerCBackoff_extended2(outputTestGold, parsedFile, nBest, 
				training, uk_limit, limitTestSetToFirst, addEOS, markTerminalNodes, 
				backoffLevels, skipEvents, groupEventLevels).runToyGrammar();
		
	}
	
	public static void main(String args[]) {			
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;
		boolean till11_21 = false;
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		
		int depType = 2; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter", "COLLINS99ArgTer"
		
		String parsedFileBase = Parameters.resultsPath + "Reranker/Parsed/" +
								((mxPos) ? "mxPOS/" : "goldPOS/") +
								wsjDepTypeName[depType] + "_sec22_nBest" + nBest + "/";		
		String baseDepDir = depTypeBase[depType];
		
		Parameters.outputPath = Parameters.resultsPath + "Reranker/" + FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		String trSec = till11_21 ? "02-11" : "02-21";
		
		File trainingFile = new File (baseDepDir + "wsj-" + trSec + ".ulab");
		File testFile = new File (baseDepDir + "wsj-22" + ((mxPos) ? ".mxpost" : "") + ".ulab");	
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);
			
		File outputTestGold = new File(Parameters.outputPath + wsjDepTypeName[depType] + ".22.gold.ulab");
		DepCorpus.toFileMSTulab(outputTestGold, test, false);
		
		File parsedFile = new File(parsedFileBase +
				"tr" + trSec + "_LLtr" + LL_tr + "_LLts" + LL_ts + "_nBest" + nBest); 
				//tr02-11_LLtr10_LLts10_nBest100.ulab
		
		if(!parsedFile.exists()) {
			makeNBest(LL_tr, LL_ts, nBest, till11_21, depType, mxPos);
		}
		
		
		boolean addEOS = true;
		boolean markTerminalNodes = false;


 		/*int[][][] backoffLevels = new int[][][] {
				//dist | w(d), t(d), h, s, dir 
				{
					{0, 	0,    0, 2, 3, 1},
					{0, 	1,    0, 2, 3, 1},
				},
				 //w(d) | t(d), h, s, dir
				{	 
					{0,		 0, 0, 3, 0},
					{0,		 0, 2, 3, 0},
					{0,		 0, 3, 3, 1},
				},
				{//t(d) | h, s, dir
					{0,	  0, 2, 0},
					{0,	  2, 2, 0},
					{0,	  2, 3, 0},
				}
		};
		boolean[] skipEvents = new boolean[]{false, false, false};
		int[][] groupEventLevels = new int[][]{{1,1},{1,1,1},{1,1,1}};*/
		
		//dist, w(d),t(d), term(d) | h, s, gh, dir
		int[][][] backoffLevels = new int[][][] {				
				{//dist | w(d),t(d), term(d), h, s, gh, dir 
					 {0,     1,   0,       0, 2, 2,  6,  0}
				},
				{//w(d) | t(d), term(d), h, s, gh, dir
					 {0,     1,       0, 2, 2,  6,   0}				
				},
				{//t(d) | term(d), h, s, gh, dir
				     {0,        0, 0, 2,  2,   0}
				},
				{//term(d) | h, s, gh, dir
					{    0,  0, 2,  2, 0}
				}
		};
		
		boolean[] skipEvents = new boolean[]{false, false, false, false};
		int[][] groupEventLevels = new int[][]{{1},{1},{1},{1}};

		
		new Reranker_EisnerCBackoff_extended2(outputTestGold, parsedFile, nBest, 
				training, uk_limit, limitTestSetToFirst, addEOS, markTerminalNodes, 
				backoffLevels, skipEvents, groupEventLevels).reranking();
	}

}
