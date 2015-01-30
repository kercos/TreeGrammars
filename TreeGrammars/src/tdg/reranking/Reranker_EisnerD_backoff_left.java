package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;

public class Reranker_EisnerD_backoff_left extends Reranker_ProbModelBackoff {
	
	private static boolean countEmptyDaughters = false;
	private static boolean printTables = false;
	
	// P(link | d, h, s, dir, distDH, distDS, daughterWasChosen)
	public Reranker_EisnerD_backoff_left(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int limitTestToFirst, boolean addEOS, boolean markTerminalNodes,
			int[][][] backoffLevels, boolean[] skipEvents, int[][] groupEventLevels) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, 
				limitTestToFirst, addEOS, countEmptyDaughters, markTerminalNodes, printTables, 
				backoffLevels, skipEvents, groupEventLevels);
		
		updateCondFreqTables();	
	}
	
	public static String[][] fillBackoffTagsTable(TDNode h, TDNode s, TDNode d, String dir,
			BitSet chosenDaughters, boolean countAsDaughterOrNeighbour, int previousDaughters,
			TDNode s2, int v) {
		//0: same postag
		//1: same lex
		//2: same lexpostag
		int distHD = getDistance(h, d);
		int distSD = getDistance(s, d);		
		previousDaughters = groupDistance(previousDaughters);
		v = groupDistance(v);
		String chosenDaughter = d==null ? "0" : (chosenDaughters.get(d.index) ? "1" : "0");
		String[][] result = new String[11][];
		//0: daughter / neighbour (1 / 0)
		//-----
		//1: daughter (lexpostag, lex, postag, "*")
		//2: parent (lexpostag, lex, postag, "*")
		//3: previous sister (lexpostag, lex, postag, "*")		
		//4: direction (dir, "*")
		//5: distance head daughter
		//6: distance sister daughter
		//7: daughter was previously chosen
		//8: previous previous sister (lexpostag, lex, postag, "*")
		result[0] = new String[]{countAsDaughterOrNeighbour ? "1" : "0", ""}; 		
		//------
		result[1] = (d==null)? 	new String[]{EOC, EOC, EOC, EOC, EOC, EOC, ""} : 
					standardReduction(d); 			
		result[2] = standardReduction(h);
		result[3] = (s==null) ? new String[]{nullNode, nullNode, nullNode, nullNode, nullNode, nullNode, ""} :
					standardReduction(s);
		result[4] = new String[]{dir, ""};
		result[5] = new String[]{Integer.toString(distHD), ""};
		result[6] = new String[]{Integer.toString(distSD), ""};
		result[7] = new String[]{chosenDaughter, ""};
		result[8] = new String[]{Integer.toString(previousDaughters), ""};
		result[9] = (s2==null) ? new String[]{nullNode, nullNode, nullNode, nullNode, nullNode, nullNode, ""} :
					standardReduction(s2);
		result[10] = new String[]{Integer.toString(v), ""};
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
	
	
	/**
	 * Recursively increase the frequency of the current node and L/R children in the table.
	 * 
	 * The rules to be stored are: 
	 * - head + "_L_" + ld1
	 * - head + "_L_" + ld2
	 * ...
	 * - head + "_NULL_" + ld2
	 * ...
	 * - head + "_R_" + rd1
	 * - head + "_R_" + rd2 
	 * ...
	 * - head + "_R_" + "_NULL_"
	 * ...
	 * @param freqTable
	 * @param SLP_type
	 */
	public void updateCondFreqTables(TDNode thisNode) {
		TDNode[] structure = thisNode.getStructureArray();
		TDNode previousChosenDaughter, nextChosenDaughter;
		BitSet chosenDaughters = new BitSet(structure.length);
		String[][] backoffTagsTable = null;
		
		for(TDNode t : structure) {		
			previousChosenDaughter = null;
			int previousChosenDaughterIndex = -1;
			int previousDaughters = 0;
			int prole = t.leftProle();
			if (t.leftDaughters!=null) {				
				for(int li = 0; li<prole; li++) {
					nextChosenDaughter = (li==prole-1) ? null : t.leftDaughters[li+1];
					for(int l=previousChosenDaughterIndex+1; l<t.index; l++) {
						TDNode lN = structure[l];						
						backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
								lN, left, chosenDaughters, false, previousDaughters, 
								nextChosenDaughter,prole );
						increaseInTables(backoffTagsTable);
					}					
					backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
							null, left, chosenDaughters, false, previousDaughters,
							nextChosenDaughter, prole);
					increaseInTables(backoffTagsTable);
					TDNode ld = t.leftDaughters[li];					
					backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
							ld, left, chosenDaughters, true, previousDaughters,
							nextChosenDaughter, prole);
					increaseInTables(backoffTagsTable);					
					previousChosenDaughter = ld;
					previousChosenDaughterIndex = previousChosenDaughter.index;					
					chosenDaughters.set(previousChosenDaughter.index);
					previousDaughters++;
				}			
			}
			for(int l=previousChosenDaughterIndex+1; l<t.index; l++) {
				TDNode lN = structure[l];						
				backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
						lN, left, chosenDaughters, false, previousDaughters, 
						null, prole);
				increaseInTables(backoffTagsTable);
			}
			backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
					null, left, chosenDaughters, false, previousDaughters, 
					null, prole);
			increaseInTables(backoffTagsTable);
			backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
					null, left, chosenDaughters, true, previousDaughters, 
					null, prole);
			increaseInTables(backoffTagsTable);			

			previousChosenDaughter = null;
			previousChosenDaughterIndex = t.index;
			previousDaughters = 0;
			prole = t.rightProle();		
			if (t.rightDaughters!=null) {
				for(int ri = 0; ri<prole; ri++) {		
					nextChosenDaughter = (ri==prole-1) ? null : t.rightDaughters[ri+1];
					for(int r=previousChosenDaughterIndex+1; r<structure.length; r++) {
						TDNode rN = structure[r];
						backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
								rN, right, chosenDaughters, false, previousDaughters,
								nextChosenDaughter, prole);
						increaseInTables(backoffTagsTable);
					}
					backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
							null, right, chosenDaughters, false, previousDaughters,
							nextChosenDaughter, prole);
					increaseInTables(backoffTagsTable);
					TDNode rd = t.rightDaughters[ri];
					backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
							rd, right, chosenDaughters, true, previousDaughters,
							nextChosenDaughter, prole);
					increaseInTables(backoffTagsTable);
					previousChosenDaughter = rd;
					previousChosenDaughterIndex = previousChosenDaughter.index;
					chosenDaughters.set(previousChosenDaughter.index);
					previousDaughters++;
				}
			}		
			for(int r=previousChosenDaughterIndex+1; r<structure.length; r++) {
				TDNode rN = structure[r];
				backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
						rN, right, chosenDaughters, false, previousDaughters,
						null, prole);
				increaseInTables(backoffTagsTable);
			}
			backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
					null, right, chosenDaughters, false, previousDaughters,
					null, prole);
			increaseInTables(backoffTagsTable);
			backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
					null, right, chosenDaughters, true, previousDaughters,
					null, prole);
			increaseInTables(backoffTagsTable);
			
		}
	}
	
	public static int getDistance(TDNode head, TDNode dep) {
		if (head==null || dep==null) return 0;
		int dis = Math.abs(head.index - dep.index);
		return groupDistance(dis);
	}
	
	public static int groupDistance(int dis) {
		if (dis==0) return 0;
		if (dis==1) return 1;
		if (dis==2) return 2;
		if (dis<7) return 3;
		return 4;
	}
	
	
	/**
	 * Compute the probability of the current node rewriting to the specific set of children,
	 * conditioned on the current node. (Based on the tables obtained with 
	 * updateHeadLRRuleFreqTable and updateHeadFreqTable) 
	 * @param headLRFreqTable
	 * @param headFreqTable
	 * @param SLP_type
	 * @return
	 */
	public double getProb(TDNode thisNode) {
		
		double prob = 1f;
				
		TDNode[] structure = thisNode.getStructureArray();
		TDNode previousChosenDaughter, nextChosenDaughter;
		BitSet chosenDaughters = new BitSet(structure.length);
		String[][] backoffTagsTable = null;
		
		for(TDNode t : structure) {		
			previousChosenDaughter = null;
			nextChosenDaughter = null;
			int previousDaughters = 0;
			int prole = t.leftProle();
			if (t.leftDaughters!=null) {								
				for(int li = 0; li<prole; li++) {									
					TDNode ld = t.leftDaughters[li];
					nextChosenDaughter = (li==prole-1) ? null : t.leftDaughters[li+1];
					backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
							ld, left, chosenDaughters, true, previousDaughters,
							nextChosenDaughter, prole);
					prob *= getCondProb(backoffTagsTable);
					previousChosenDaughter = ld;		
					chosenDaughters.set(previousChosenDaughter.index);
					previousDaughters++;
				}			
			}
			backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
					null, left, chosenDaughters, true, previousDaughters,
					null, prole);
			prob *= getCondProb(backoffTagsTable);			

			previousChosenDaughter = null;
			nextChosenDaughter = null;
			previousDaughters = 0;
			prole = t.rightProle();		
			if (t.rightDaughters!=null) {			
				int rightProle = t.rightProle();
				for(int ri=0; ri<rightProle; ri++) {
					TDNode rd = t.rightDaughters[ri];
					nextChosenDaughter = (ri==rightProle-1) ? null : t.rightDaughters[ri+1];
					backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
							rd, right, chosenDaughters, true, previousDaughters,
							nextChosenDaughter, prole);
					prob *= getCondProb(backoffTagsTable);
					previousChosenDaughter = rd;
					chosenDaughters.set(previousChosenDaughter.index);
					previousDaughters++;
				}
			}		
			backoffTagsTable = fillBackoffTagsTable(t, previousChosenDaughter, 
					null, right, chosenDaughters, true, previousDaughters,
					null, prole);
			prob *= getCondProb(backoffTagsTable);
			
		}
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
		boolean markTerminalNodes = false;		
		
		int[][][] backoffLevels = new int[][][] {
				//link | d, h, s, dir, distDH, distDS, daughterWasChosen 
				{
					{0,  0, 0, 0,   0,      1,      1,  1}
				},
		};
		
		boolean[] skipEvents = new boolean[]{false};
		int[][] groupEventLevels = new int[][]{{1}};
				
		
		String parameters =
			"RerankerLR1LProb" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Asdd EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerD_backoff_left(outputTestGold, parsedFile, nBest, training, uk_limit,  
				limitTestSetToFirst, addEOS, markTerminalNodes, backoffLevels, skipEvents, 
				groupEventLevels).runToyGrammar();
		
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
		
		int[][][] backoffLevels = new int[][][] {
				//link | d, h, s, dir, distDH, distDS, daughterWasChosen, previous#D, s2, v 
				{
					{0,  2, 2, 2,   0,      0,      0,                 1,          0,  6, 1}
					/*{0,  0, 0, 2,   0,      0,      1,                 1,          1,  6, 1},
					
					{0,  2, 0, 2,   0,      0,      1,                 1,          1,  6, 1},
					{0,  0, 2, 2,   0,      0,      1,                 1,          1,  6, 1},
					{0,  0, 0, 4,   0,      0,      1,                 1,          1,  6, 1},
					
					{0,  2, 0, 2,   0,      1,      1,                 1,          1,  6, 1},
					{0,  0, 2, 2,   0,      1,      1,                 1,          1,  6, 1},
					
					{0,  2, 2, 2,   0,      0,      1,                 1,          1,  6, 1},
					
					{0,  2, 2, 4,   0,      0,      1,                 1,          1,  6, 1}*/					
				}
		};
		
		boolean[] skipEvents = new boolean[]{false};
		int[][] groupEventLevels = new int[][]{{1}};
		//int[][] groupEventLevels = new int[][]{{1,3,3,1}};
		
		/*int[][][] backoffLevels = new int[][][] {
				//link | d, h, s, dir, distDH, distDS, daughterWasChosen  previousDaughters
				{
					{0,  0, 0, 2,   0,      0,      1,  1, 1},
					
					{0,  2, 0, 2,   0,      0,      1,  1, 1},
					{0,  0, 2, 2,   0,      0,      1,  1, 1},
					{0,  0, 0, 3,   0,      0,      1,  1, 1},
					
					{0,  2, 0, 2,   0,      1,      1,  1, 1},
					{0,  0, 2, 2,   0,      1,      1,  1, 1},
					
					{0,  2, 2, 2,   0,      0,      1,  1, 1},
					
					{0,  2, 2, 3,   0,      0,      1,  1, 1},
				},
		};
		
		boolean[] skipEvents = new boolean[]{false};
		int[][] groupEventLevels = new int[][]{{1,3,2,1,1}};	*/
		
		String parameters =
			"Eisner_D" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Asdd EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		new Reranker_EisnerD_backoff_left(outputTestGold, parsedFile, nBest, training, uk_limit,  
				limitTestSetToFirst, addEOS, markTerminalNodes, backoffLevels, skipEvents, 
				groupEventLevels).reranking();
	}
}
