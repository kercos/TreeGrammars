package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import tdg.kernels.DepKernelGraph;
import util.*;
import util.file.FileUtil;

public class RerankerKernelArgDOP extends Reranker_ProbModel{
	
	private static boolean printTables = true;
	
	int maxBranching;
	
	boolean adjacency;
	
	int DKG_type;
			//0: order
			//1: noOrder
			//2: LR
			//3: orderNoFreq
			//4: argFrameOrder 
	int SLP_type; 
			//0: same postag
			//1: same lex
			//2: same lexpostag
			//3: mix		
	int collectionType; 
			//0: NodeSetCollectorStandard, 
			//1: NodeSetCollectorFreq, 
			//2: NodeSetCollectorMUB
			//3: NodeSetCollectorMUBFreq
			//4: NodeSetCollectorUnion
			//5: NodeSetCollectorArgFreq
	
	Hashtable<String, Double> condFreqTableDouble;
	
	public RerankerKernelArgDOP(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, boolean countEmptyDaughters,
			boolean markTerminalNodes, int DKG_type, int maxBranching, int collectionType, 
			int SLP_type, int limitTestToFirst, boolean adjacency, boolean addEOS) {
		
		super(goldFile, parsedFile, nBest, trainingCorpus, uk_limit, limitTestToFirst, 
				addEOS, countEmptyDaughters, markTerminalNodes, printTables, false);

		this.DKG_type = DKG_type;
		this.maxBranching = maxBranching;
		this.collectionType = collectionType;
		this.SLP_type = SLP_type;
		this.adjacency = adjacency;
		this.condFreqTableDouble = new Hashtable<String, Double>();
		
		updateCondFreqTables();
	}
	
	
	public void updateCondFreqTables(TDNode tree) {
		TDNode[] structure = tree.getStructureArray();
		int[][] subCondKeyArray = new int[structure.length][]; 
		
		for(TDNode p : structure) {			
			String subCondKey_internalNodes = removeMarks(p.lexPosTag(SLP_type));
			int[] subCondFreq_internalNodes = getSubCondFreq(p, subCondKeyArray); 		
			int subCondFreq = subCondFreq_internalNodes[0];
			int internalNodes = subCondFreq_internalNodes[1]-1; // remove root
			double freqWithDiscount = Math.pow(2, -internalNodes) * subCondFreq; 
			Utility.increaseInTableDouble(condFreqTableDouble, subCondKey_internalNodes, 
					freqWithDiscount);			
		}				
	}
	
	private static int[] getSubCondFreq(TDNode p, int[][] subCondFreq_inArray) {
		int index = p.index;
		int[] subCondFreq_in = subCondFreq_inArray[index];
		if (subCondFreq_in!=null) return subCondFreq_in;
		TDNode[] argDaughters = p.getArgumentDaughters();
		if (argDaughters.length==0) subCondFreq_in = new int[]{0,0};
		else  {
			subCondFreq_in = new int[]{1,1};
			for(TDNode ad : argDaughters) {
				int[] subCondFreq_inAd = getSubCondFreq(ad, subCondFreq_inArray);
				subCondFreq_in[0] *= (subCondFreq_inAd[0]+1);
				subCondFreq_in[1] += (subCondFreq_inAd[1]);
			}				
		}
		return subCondFreq_inArray[index] = subCondFreq_in;
	}
	
		
	public double getProb(TDNode t) {
		return	DepKernelGraph.computeKernelProbArgInsertion(DKG_type, t, 
				trainingCorpus, -5, maxBranching, collectionType, SLP_type, 
				countEmptyDaughters, adjacency, condFreqTableDouble);
	}
	
	@Override
	public void updateKeyCondKeyLog(TDNode thisNode,
			Vector<String> keycondKeyLog) {
		// TODO Auto-generated method stub
		
	}	
	
	/*
	 * Toy Sentence
	 */
	public static void main1(String[] args) {
		int uk_limit = -1;
		int LL_tr = 40;		
		int LL_ts = 10;
		int nBest = 10;				
		boolean mxPos = false;		
		int limitTestSetToFirst = 10000;
		
		String toySentence = Parameters.corpusPath + "ToyGrammar/toySentence_ArgStruc";		
		
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
		
		boolean countEmptyDaughters = false;
		boolean addTerminalNodes = true;
		boolean adjacency = false;
		boolean addEOS = true;
		int maxBranching = 10;
		
		int DKG_type = 4;
				//0: order
				//1: noOrder
				//2: LR1Level
				//3: orderNoFreq
				//4: argFrameOrder 
		int SLP_type = 0; 
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		int collectionType = 6; 
				//0: NodeSetCollectorStandard, 
				//1: NodeSetCollectorFreq, 
				//2: NodeSetCollectorMUB
				//3: NodeSetCollectorMUBFreq
				//4: NodeSetCollectorUnion
				//5: NodeSetCollectorArgFreq
				//6: NodeSetCollectorArgFreqTerm						
		
		String parameters =
			"RerankerGraphProbArgInsertion" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Count Empty Daughters\t" + countEmptyDaughters + "\n" +
			"Adjacency\t" + adjacency + "\n" +
			"Asdd EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"DKG type\t" + DKG_type + "\n" +
			"Collection Type\t" +  collectionType + "\n" +
			"SLP_type\t" + SLP_type + "\n" +
			"Max Branching\t" +  maxBranching + "\n" +
			"Use mxPos\t" + mxPos + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		//getOracle(outputTestGold, parsedFile, rerankingFile, nBest, training, uk_limit, 
		//		DKG_type, maxBranching, collectionType, SLP_type, limitTestSetToFirst,
		//		adjacency);
		//checkCoverage(rerankingFile, parsedFile, nBest, training, uk_limit, DKG_type, maxBranching, 
		//		collectionType, SLP_type, adjacency);
		new RerankerKernelArgDOP(outputTestGold, parsedFile, nBest, training, uk_limit, 
				countEmptyDaughters, addTerminalNodes, DKG_type, maxBranching, collectionType, SLP_type, 
				limitTestSetToFirst, adjacency, addEOS).runToyGrammar();
		
	}

	public static void main(String args[]) {
		int uk_limit = -1;
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 10;				
		boolean till11_21 = false;
		boolean mxPos = false;
		int limitTestSetToFirst = 10000;
		
		int depType = 3; //"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter"
		
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
		
		boolean countEmptyDaughters = false;
		boolean addTerminalNodes = false;
		boolean adjacency = false;
		boolean addEOS = true;
		int maxBranching = 10;
		
		int DKG_type = 4;
				//0: order
				//1: noOrder
				//2: LR1Level
				//3: orderNoFreq
				//4: argFrameOrder 
		int SLP_type = 0; 
				//0: same postag
				//1: same lex
				//2: same lexpostag
				//3: mix		
		int collectionType = 6; 
				//0: NodeSetCollectorStandard, 
				//1: NodeSetCollectorFreq, 
				//2: NodeSetCollectorMUB
				//3: NodeSetCollectorMUBFreq
				//4: NodeSetCollectorUnion
				//5: NodeSetCollectorArgFreq
				//6: NodeSetCollectorArgFreqTerm		
						
		
		String parameters =
			"RerankerGraphProbArgInsertion" + "\n" +
			"LL_tr\t" + LL_tr + "\n" +
			"LL_ts\t" + LL_ts + "\n" +
			"UK_limit\t" + uk_limit + "\n" +
			"Count Empty Daughters\t" + countEmptyDaughters + "\n" +
			"Adjacency\t" + adjacency + "\n" +
			"Asdd EOS\t" + addEOS + "\n" +
			"nBest\t" + nBest + "\n" +
			"Training File\t" + trainingFile + "\n" +			
			"Test File\t" + testFile + "\n" +
			"Parsed File\t" + parsedFile + "\n" +
			"Training Size\t" + training.size() + "\n" +			
			"Test Size\t" + test.size() + "\n" +
			"DKG type\t" + DKG_type + "\n" +
			"Collection Type\t" +  collectionType + "\n" +
			"SLP_type\t" + SLP_type + "\n" +
			"Max Branching\t" +  maxBranching + "\n" +
			"Use mxPos\t" + mxPos + "\n";
		
		FileUtil.appendReturn(parameters, Parameters.logFile);
				
		System.out.println(parameters);
		//getOracle(outputTestGold, parsedFile, rerankingFile, nBest, training, uk_limit, 
		//		DKG_type, maxBranching, collectionType, SLP_type, limitTestSetToFirst,
		//		adjacency);
		//checkCoverage(rerankingFile, parsedFile, nBest, training, uk_limit, DKG_type, maxBranching, 
		//		collectionType, SLP_type, adjacency);
		new RerankerKernelArgDOP(outputTestGold, parsedFile, nBest, training, uk_limit, 
				countEmptyDaughters, addTerminalNodes, DKG_type, maxBranching, collectionType, SLP_type, 
				limitTestSetToFirst, adjacency, addEOS).reranking();
	}
	
}
