package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class RerankerLRCFGBackoff extends Reranker_ProbModelBackoff{
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	//P (daughters_in_order | parent, grandParent, grandGrandParent, direction)
	public RerankerLRCFGBackoff(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit,
			int limitTestToFirst, boolean markTerminalNodes, boolean addEOS,
			int[][][] backoffLevels, boolean[] skipEvents, int[][] groupEventLevels) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, markTerminalNodes, printTables, 
				backoffLevels, skipEvents, groupEventLevels);
				
		updateCondFreqTables();
	}

	public static String[][] fillBackoffTagsTable(TDNode h, TDNode[] daughters, String dir) {
		String[][] result = new String[5][];
		//0: daughters_in_order (lexpos, pos, lexpos(first), pos(first), "*"))
		//1: parent (lexpostag, lex, postag, "*")
		//2: grandparent (lexpostag, lex, postag, "*")
		//3: grandGrandparent (lexpostag, lex, postag, "*")
		//4: direction (dir, "*")
		String lexpos="", pos="", lexpos_first=null, pos_first=null;
		if (daughters!=null) {
			lexpos_first = daughters[0].lexPosTag(2);
			pos_first = daughters[0].lexPosTag(0);
			if (dir==left) {
				for(int di=daughters.length-1; di>=0; di--) {
					TDNode d = daughters[di];
					lexpos += d.lexPosTag(2) + "/";
					pos += d.lexPosTag(0) + "/";
				}				
			}
			else {
				for(int di=0; di<daughters.length; di++) {
					TDNode d = daughters[di];
					lexpos += d.lexPosTag(2) + "/";
					pos += d.lexPosTag(0) + "/";
				}
			}	
			lexpos += EOC;
			pos += EOC;
		}
		else {
			lexpos_first = EOC;
			pos_first = EOC;
		}
		
		TDNode gP = h.parent;
		TDNode ggP = (gP==null) ? null : gP.parent;
		result[0] = new String[]{lexpos, pos, lexpos_first, pos_first, "*"};
		result[1] = new String[]{h.lexPosTag(2), h.lexPosTag(1), h.lexPosTag(0), "*"};
		result[2] = (gP==null) ? new String[]{"NULL","NULL","NULL","*"} :					
					new String[]{gP.lexPosTag(2), gP.lexPosTag(1), gP.lexPosTag(0), "*"};		
		result[3] = (ggP==null) ? new String[]{"NULL","NULL","NULL","*"} :					
			new String[]{ggP.lexPosTag(2), ggP.lexPosTag(1), ggP.lexPosTag(0), "*"};
		result[4] = new String[]{dir, "*"};
		return result;
	}
	
	@Override
	public void updateCondFreqTables(TDNode thisNode) {
		TDNode[] structure = thisNode.getStructureArray();
		for(TDNode t : structure) {
			increaseInTables(fillBackoffTagsTable(t, t.leftDaughters, left));
			increaseInTables(fillBackoffTagsTable(t, t.rightDaughters, right));
		}		
	}
	
	@Override
	public double getProb(TDNode thisNode) {
		double prob = 1d;
		TDNode[] structure = thisNode.getStructureArray();
		for(TDNode t : structure) {
			prob	*= getCondProb(fillBackoffTagsTable(t, t.leftDaughters, left))
					* getCondProb(fillBackoffTagsTable(t, t.rightDaughters, right));
		}	
		return prob;
	}


	@Override
	public void updateKeyCondKeyLog(TDNode thisNode,
			Vector<String> keycondKeyLog) {
		// TODO Auto-generated method stub
		
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
						
		
		String parameters =
			"RerankerLRCFGBackoff" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
		
 		int[][][] backoffLevels = new int[][][] {
 				//(lexpos, pos, lexpos(first), pos(first), "*"))
				//production | parent, grandparent, grandGrandParent, dir 
				{//production | p, gP, ggP, dir
					{1,	2, 2, 3, 0},
					{1,	2, 3, 3, 0},
					{1,	3, 3, 3, 1},
					{2,	2, 3, 3, 0},
				}
		};
		boolean[] skipEvents = new boolean[]{false};
		int[][] groupEventLevels = new int[][]{{1,1,1,1}};
				
		System.out.println(parameters);
		new RerankerLRCFGBackoff(outputTestGold, parsedFile, nBest, training, uk_limit, 
				limitTestSetToFirst, markTerminalNodes, 
				addEOS, backoffLevels, skipEvents, groupEventLevels).reranking();
	}


}
