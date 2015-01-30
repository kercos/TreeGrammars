package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_ArgAdjFrame_backoff extends Reranker_ProbModelBackoff {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = true;
	
	
	// P(darg/adj | h, dir, gh)
	public Reranker_ArgAdjFrame_backoff(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int limitTestToFirst, boolean addEOS, boolean markTerminalNodes, 
			int[][][] backoffLevels, boolean[] skipEvents, int[][] groupEventLevels) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, markTerminalNodes, printTables, 
				backoffLevels, skipEvents, groupEventLevels);

		updateCondFreqTables();				
	}
	
	public static String[][] fillBackoffTagsTable(TDNode h, TDNode gh, 
			String arg_adj, String dir) {		
		String[][] result = new String[4][];
		//0: arg/adj markers
		//-----
		//1: parent_node
		//2: direction (dir, "*")		
		//3: granParent_node
		result[0] = (arg_adj==null)? new String[]{nullNode, ""} : new String[]{arg_adj, ""};;
		//-----
		result[1] =  standardReduction(h);
		result[2] = new String[]{dir, ""};		
		result[3] = (gh==null)? 
					new String[]{nullNode, nullNode, nullNode, nullNode, nullNode, nullNode, ""} : 
					standardReduction(gh);
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
	
	public static int maxArgs = -1;
	
	public void updateCondFreqTables(TDNode thisNode) {		
		String[][] backoffTagsTable = null;
		TDNode parent = thisNode.parent;
		String arg_adj = null;	
		if (thisNode.leftDaughters!=null) {
			arg_adj = "";
			int args = 0;
			for(TDNode ld : thisNode.leftDaughters) {
				if (ld.isArgumentDaughter()) {
					arg_adj += "+";
					args++;
				}
				arg_adj += "-";
				updateCondFreqTables(ld);
			}			
			if (args>maxArgs) maxArgs = args;
		}
		backoffTagsTable = fillBackoffTagsTable(thisNode, parent, arg_adj, left);
		increaseInTables(backoffTagsTable);
		
		arg_adj = null;	
		if (thisNode.rightDaughters!=null) {
			arg_adj = "";
			int args=0;
			for(TDNode rd : thisNode.rightDaughters) {
				if (rd.isArgumentDaughter()) {
					arg_adj += "+";
					args++;
				}
				arg_adj += "-";
				updateCondFreqTables(rd);
			}
			if (args>maxArgs) maxArgs = args;
		}
		backoffTagsTable = fillBackoffTagsTable(thisNode, parent, arg_adj, right);
		increaseInTables(backoffTagsTable);
	}
	
	public double getProb(TDNode thisNode) {
		
		double prob = 1d;
		
		String[][] backoffTagsTable = null;
		TDNode parent = thisNode.parent;
		String arg_adj = null;	
		if (thisNode.leftDaughters!=null) {
			arg_adj = "";
			for(TDNode ld : thisNode.leftDaughters) {
				arg_adj += ld.isArgumentDaughter() ? "+" : "-";
				prob *= getProb(ld);
			}			
		}
		backoffTagsTable = fillBackoffTagsTable(thisNode, parent, arg_adj, left);
		prob *= getCondProb(backoffTagsTable);
		
		arg_adj = null;	
		if (thisNode.rightDaughters!=null) {
			arg_adj = "";
			for(TDNode rd : thisNode.rightDaughters) {
				arg_adj += rd.isArgumentDaughter() ? "+" : "-";
				prob *= getProb(rd);
			}			
		}
		backoffTagsTable = fillBackoffTagsTable(thisNode, parent, arg_adj, right);
		prob *= getCondProb(backoffTagsTable);
				
		return prob;
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
				//darg/adj | h, dir, gh 
				{
					{0, 	 2,   0, 6}
				}
		};
		
		boolean[] skipEvents = new boolean[]{false};
		int[][] groupEventLevels = new int[][]{{1}};
		
		new Reranker_ArgAdjFrame_backoff(outputTestGold, parsedFile, nBest, 
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
		
		int depType = 3; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter", "COLLINS99ArgTer"
		
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


 		int[][][] backoffLevels = new int[][][] {
 				//darg/adj | h, dir, gh 
				{
					{0, 	 4,   0, 6}
				}
		};
		boolean[] skipEvents = new boolean[]{false};
		int[][] groupEventLevels = new int[][]{{1}};
				
		new Reranker_ArgAdjFrame_backoff(outputTestGold, parsedFile, nBest, 
				training, uk_limit, limitTestSetToFirst, addEOS, markTerminalNodes, 
				backoffLevels, skipEvents, groupEventLevels).reranking();
		System.out.println("Max argument: " + maxArgs);
	}
}
