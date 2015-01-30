package tdg.reranking;
import java.io.*;
import java.util.*;

import pos.IsstPos;
import pos.TanlPos;
import pos.TutPos;
import pos.WsjPos;
import settings.Parameters;
import tdg.DSConnl;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.TanlD;
import tdg.corpora.TutD;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_EisnerCBackoff_left_extended_Tanl extends Reranker_ProbModelBackoff {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	public static TanlPos tanlPosAnalyzer;
	
	
	// P(dist, term(d), w(d),t(d) | h, s, gh, dir)
	public Reranker_EisnerCBackoff_left_extended_Tanl(File goldFile, File parsedFile, int nBest, 
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
		//1: d is terminal
		//2: word(daughter)
		//3: tag(daughter)
		//-----
		//4: parent
		//5: previous sister
		//6: gran parent
		//7: direction
		result[0] = new String[]{Integer.toString(dist), Integer.toString(groupDistance(dist)), ""};
		result[1] = (d==null)? new String[]{EOC, ""} : 
					(d.isTerminalNode())? new String[]{"1", ""} : new String[]{"0", ""};
		result[2] = (d==null)? new String[]{EOC, EOC, ""} : 
						new String[]{tanlPosAnalyzer.getLex(d), tanlPosAnalyzer.getLemma(d), ""};
		result[3] = (d==null)? new String[]{EOC, EOC, ""} : 
						new String[]{tanlPosAnalyzer.getPos(d),tanlPosAnalyzer.getShortPos(d),""};		
		//-----
		result[4] = tanlPosAnalyzer.standardReductions(h);
		result[5] = (s==null) ? 
					new String[]{nullNode, nullNode, nullNode, nullNode, nullNode, nullNode, ""} :
						tanlPosAnalyzer.standardReductions(s);
		result[6] = (gh==null) ? 
					new String[]{nullNode, nullNode, nullNode, nullNode, nullNode, nullNode, ""} :
					tanlPosAnalyzer.standardReductions(gh);			
		result[7] = new String[]{dir, ""};
		return result;		
	}
	
	public void updateCondFreqTables(TDNode thisNode) {		
		String[][] backoffTagsTable = null;
		TDNode d = null;
		int dist;
			
		TDNode previousChild = null;
		if (thisNode.leftDaughters!=null) {
			for(TDNode ld : thisNode.leftDaughters) {
				dist = getDistance(thisNode,ld);
				backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, ld, left, dist);
				increaseInTables(backoffTagsTable);
				previousChild = ld;
				updateCondFreqTables(ld);				
			}
		}
		d = null;
		dist = thisNode.leftProle()+1;
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
		dist = thisNode.rightProle()+1;
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
			for(TDNode ld : thisNode.leftDaughters) {
				dist = getDistance(thisNode,ld);
				backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, ld, left, dist);
				prob *= getCondProb(backoffTagsTable) * getProb(ld);
				previousChild = ld;				
			}
		}
		d = null;
		dist = thisNode.leftProle()+1;
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
		dist = thisNode.rightProle()+1;
		backoffTagsTable = fillBackoffTagsTable(thisNode, previousChild, d, right, dist);
		prob *= getCondProb(backoffTagsTable);
		
		return prob;
	}
	
	public static int getDistance(TDNode head, TDNode dep) {
		int dis = Math.abs(head.index - dep.index);
		return dis;
	}
	
	public static int groupDistance(int dis) {
		if (dis==1) return 1;
		if (dis==2) return 2;
		if (dis<7) return 3; // 3,4,5,6
		return 4; // 7,8, 9, 10, ..., inf.
	}
	
	public void updateKeyCondKeyLog(TDNode thisNode, Vector<String> keyCondKeyLog) {

	}
	

	
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
	
	static public int[][][] default_backoffLevels = new int[][][] {				
		{//dist | term(d), w(d),t(d), h, s, gh, dir 									
			{0,        1,    2,   1, 0, 0,  2,   0},
			
			{0,        1,    2,   1, 2, 2,  4,   0},
			
			{0,        1,    2,   1, 4, 4,  5,   0},
		},
		{//term(d) | w(d),t(d), h, s, gh, dir
				{0,     2,   1, 0, 0, 3,    1},
			
			    {0,     2,   1, 2, 2, 4,    1},
			    
			    {0,     2,   1, 4, 4, 5,    1},
		},				
		{//w(d) | t(d), h, s, gh, dir
			 {0,     2, 0, 0,  3,   0},
			
			 {0,     2, 2, 2,  4,   0},
			 
			 {0,     2, 4, 4,  5,   0},
		},
		{//t(d) | h, s, gh, dir			
			{0,   0, 0,  2,   0},
			
			{0,   0, 2,  2,   0},					
			{0,   2, 0,  2,   0},
			
			{0,   4, 4,  5,   0},
			
			{0,   5, 5,  5,   0}
		}};

	static boolean[] default_skipEvents = new boolean[]{false, false, false, false};
	static int[][] default_groupEventLevels = new int[][]{{1,1,1},{1,1,1},{1,1,1},{1,2,1,1}};
	
	public static void mainTest(String[] args) { //mainTest				
		int uk_limit = -1;
		int nBest = 10;
		boolean mxPos= false;
		int limitTestSetToFirst = 10000;
		boolean includeGoldInNBest = false;
		int order = 2;
		String MSTver = "0.5";
		String corpusName = "Tut_Evalita09";
		
		String withGold = includeGoldInNBest ? "_withGold" : "";
		String mstVerOrderDir = "MST_" + MSTver + "_" + order + "order";
		String basePath = Parameters.resultsPath + "Reranker/Parsed/" +
							mstVerOrderDir + "/" + ((mxPos) ? "mxPOS/" : "goldPOS/") +
							corpusName + "/";									
						
		Parameters.outputPath = basePath + "Reranker/" + FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		File trainingFile = new File(TutD.TutTrainSetConnl);
		File testBlindFile = new File(TutD.TutTestSetBlindConnl);
		String testFileName = FileUtil.getFileNameWithoutExtensions(testBlindFile);
				
		String nBestPath = basePath  + corpusName + "_nBest" + nBest + "/";
		File parsedFile = new File(nBestPath + testFileName + "_nBest" + nBest + withGold);		
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileConll07(trainingFile, 10000);
		ArrayList<TDNode> testLB = DepCorpus.readTreebankFromFileConll07(testBlindFile, 10000);
				
		File test_LB_ulab = new File(basePath + "TUT-Evalita09-testset10-9-09_nBest1.ulab");
		//DepCorpus.convertConll07ToUlab(test1BestFileConll, test1BestFileUlab);
		DepCorpus.toFileMSTulab(test_LB_ulab, testLB, false);
		
		
		if(!parsedFile.exists()) {
			System.out.println("NBest file not found: " + parsedFile);
			Reranker.makeNBestConll(nBest, corpusName, 
					trainingFile, testBlindFile, mxPos, order, MSTver);
		}
		
		File parsedFileUlab = new File(Parameters.outputPath + testFileName + "_nBest" + nBest + withGold + ".ulab");
		DepCorpus.convertConll07ToUlab(parsedFile, parsedFileUlab);
		
		boolean addEOS = false;
		boolean markTerminalNodes = false;

		if (includeGoldInNBest) nBest++;
		new Reranker_EisnerCBackoff_left_extended_Tanl(test_LB_ulab, parsedFileUlab, nBest, 
				training, uk_limit, limitTestSetToFirst, addEOS, markTerminalNodes, 
				default_backoffLevels, default_skipEvents, default_groupEventLevels).reranking();
		
		File rerankedFileConll = new File(Parameters.outputPath+ "reranked.conll");		
		DSConnl.selectRerankedFromFile(parsedFile, rerankedFileConll, Reranker.rerankedIndexes);
		File rerankedFileUlabVerify = new File(Parameters.outputPath + "reranked.verify.ulab");
		DepCorpus.convertConll07ToUlab(rerankedFileConll, rerankedFileUlabVerify);
	}
	
	public static void mainDev(String[] args) {	//mainDev
		int uk_limit = -1;
		int nBest = 50;
		boolean mxPos= false;
		int limitTestSetToFirst = 10000;
		boolean includeGoldInNBest = false;
		int order = 2;
		String MSTver = "0.5";
		String corpusName = "Tanl_Evalita09Develop";
		
		String withGold = includeGoldInNBest ? "_withGold" : "";
		String mstVerOrderDir = "MST_" + MSTver + "_" + order + "order";
		String basePath = Parameters.resultsPath + "Reranker/Parsed/" +
							mstVerOrderDir + "/" + ((mxPos) ? "mxPOS/" : "goldPOS/") +
							corpusName + "/";							
		
		String outputPath = basePath  + corpusName + "_nBest" + nBest + "/";
						
		Parameters.outputPath = basePath + "Reranker/" + FileUtil.dateTimeString() + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		
		File trainingFile = new File(TanlD.TanlD_Train_Connl);
		File testBlindFile = new File(TanlD.TanlD_Dev_Blind_Connl);
		File testGoldFile = new File(TanlD.TanlD_Dev_Connl);
		String testFileName = FileUtil.getFileNameWithoutExtensions(testBlindFile);
				
		File parsedFile = new File(outputPath + testFileName + "_nBest" + nBest + withGold);		
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileConll07(trainingFile, 10000);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileConll07(testGoldFile, 10000);
		
		int freqConfidence = 4;
		tanlPosAnalyzer = new TanlPos(training, freqConfidence);
			
		File outputTestGold = new File(Parameters.outputPath + testFileName + ".gold.ulab");
	
		DepCorpus.toFileMSTulab(outputTestGold, test, false);		
		
		if(!parsedFile.exists()) {
			System.out.println("NBest file not found: " + parsedFile);
			Reranker.makeNBestConll(nBest, corpusName, 
					trainingFile, testBlindFile, mxPos, order, MSTver);
		}
		
		File parsedFileUlab = new File(outputPath + testFileName + "_nBest" + nBest + withGold + ".ulab");
		DepCorpus.convertConll07ToUlab(parsedFile, parsedFileUlab);
		
		boolean addEOS = false;
		boolean markTerminalNodes = false;

		if (includeGoldInNBest) nBest++;
		new Reranker_EisnerCBackoff_left_extended_Tanl(outputTestGold, parsedFileUlab, nBest, 
				training, uk_limit, limitTestSetToFirst, addEOS, markTerminalNodes, 
				default_backoffLevels, default_skipEvents, default_groupEventLevels).reranking();
	}
	
	
	public static void main(String[] args) {
		mainDev(args);
	}

}
