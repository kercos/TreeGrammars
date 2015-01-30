package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import settings.Parameters;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.file.FileUtil;

public class TsgReranker {

	String workingDir;
	File trainingCorpusFile, goldTestSetFile, parsedTestSetFile;
	TSNodeLabel nextGoldTree;
	ArrayList<TSNodeLabel> nextNBestParses;
	Scanner testScanner, nBestScanner;
	int nBest;
	ArrayList<Integer> iBestIndexes, iWorstIndexes;
	float firstBestFScore, oracleBestFScore, oracleWorstFScore;
	
	public TsgReranker(String workingDir, File trainingCorpusFile,File goldTestSetFile, 
			File parsedTestSetFile, int nBest) {
		this.workingDir = workingDir;
		this.trainingCorpusFile = trainingCorpusFile;
		this.goldTestSetFile = goldTestSetFile;
		this.parsedTestSetFile = parsedTestSetFile;
		this.nBest = nBest;
		testScanner = FileUtil.getScanner(goldTestSetFile);
		nBestScanner = FileUtil.getScanner(parsedTestSetFile);
	}
	
	public void rewind() {
		testScanner = FileUtil.getScanner(goldTestSetFile);
		nBestScanner = FileUtil.getScanner(parsedTestSetFile);
	}
	
	public  boolean getNext() throws Exception {
		nextGoldTree = null;
		while (testScanner.hasNextLine()) {
			String line = testScanner.nextLine();
			if (line.equals("")) continue;
			nextGoldTree = new TSNodeLabel(line);
			break;
		}
		if (nextGoldTree==null) return false;
		nextNBestParses = getNextNBestParses();
		return true;
	}
	
	public void makeOracleBest(File oracleBest, boolean printIntermediateResults) throws Exception {		
		int totalMatchedBrackets=0;
		int totalGoldBrackets=0;
		int totalParsedBrackets=0;
		iBestIndexes = new ArrayList<Integer>();
		int treeIndex = 0;		
		PrintWriter pw = oracleBest==null ? null : FileUtil.getPrintWriter(oracleBest);
		while(getNext()) {						
			float maxNBestFScore = -1;
			int maxNBestMatchedBrackets=-1, maxNBestParsedBrackets=-1;
			TSNodeLabel bestTree = null;
			int bestIndex = -1;
			int i=0;
			for(TSNodeLabel iBest : nextNBestParses) {				
				int[] scores = EvalC.getScores(nextGoldTree, iBest, true); //matchBrackets, goldBrackets, parsedBrackets
				if(i==0) totalGoldBrackets += scores[1];
				float fscore = EvalC.fscore(scores);
				if (fscore>maxNBestFScore) {
					maxNBestFScore = fscore;
					maxNBestMatchedBrackets = scores[0];
					maxNBestParsedBrackets = scores[2];
					bestTree = iBest;
					bestIndex = i;
				}
				i++;
			}
			if (oracleBest!=null) pw.println(bestTree.toString());
			totalMatchedBrackets += maxNBestMatchedBrackets;
			totalParsedBrackets += maxNBestParsedBrackets;
			iBestIndexes.add(bestIndex);
			treeIndex++;			
			oracleBestFScore = EvalC.fscore(totalMatchedBrackets, totalGoldBrackets, totalParsedBrackets);
			if (printIntermediateResults) System.out.println(treeIndex + ") fscore: " + maxNBestFScore + "  Temp. Oracle score: " + oracleBestFScore);
		}
		oracleBestFScore = EvalC.fscore(totalMatchedBrackets, totalGoldBrackets, totalParsedBrackets);
		System.out.println(" Oracle best score: " + oracleBestFScore);
		if (oracleBest!=null) pw.close();
	}
	
	public void makeOracleFirstBestWorst(File firstBest, File oracleBest, File oracleWorst) throws Exception {
		int totalGoldBrackets=0;
		int firstTotalMatchedBrackets=0;		
		int firstTotalParsedBrackets=0;
		int bestTotalMatchedBrackets=0;
		int bestTotalParsedBrackets=0;
		int worstTotalMatchedBrackets=0;
		int worstTotalParsedBrackets=0;
		iBestIndexes = new ArrayList<Integer>();
		iWorstIndexes = new ArrayList<Integer>();
		int treeIndex = 0;		
		PrintWriter pwFirst = FileUtil.getPrintWriter(firstBest);
		PrintWriter pwBest = FileUtil.getPrintWriter(oracleBest);
		PrintWriter pwWorst = FileUtil.getPrintWriter(oracleWorst);
		while(getNext()) {						
			float maxNBestFScore = -1f;
			float minNBestFScore = 2f;
			int maxNBestMatchedBrackets=0, maxNBestParsedBrackets=0;
			int minNBestMatchedBrackets=0, minNBestParsedBrackets=0;
			TSNodeLabel bestTree = null;
			TSNodeLabel worstTree = null;
			int bestIndex = -1;
			int worstIndex = -1;
			int i=0;
			for(TSNodeLabel iBest : nextNBestParses) {
				int[] scores = EvalC.getScores(nextGoldTree, iBest, true); //matchBrackets, goldBrackets, parsedBrackets
				float fscore = EvalC.fscore(scores);
				if(i==0) {
					totalGoldBrackets += scores[1];
					pwFirst.println(iBest.toString());
					firstTotalMatchedBrackets += scores[0];
					firstTotalParsedBrackets += scores[2];
				}
				if (fscore>maxNBestFScore) {
					maxNBestFScore = fscore;
					maxNBestMatchedBrackets = scores[0];
					maxNBestParsedBrackets = scores[2];
					bestTree = iBest;
					bestIndex = i;
				}
				if (fscore<minNBestFScore) {
					minNBestFScore = fscore;
					minNBestMatchedBrackets = scores[0];
					minNBestParsedBrackets = scores[2];
					worstTree = iBest;
					worstIndex = i;
				}
				i++;
			}
			pwBest.println(bestTree.toString());
			pwWorst.println(worstTree.toString());
			bestTotalMatchedBrackets += maxNBestMatchedBrackets;
			bestTotalParsedBrackets += maxNBestParsedBrackets;
			worstTotalMatchedBrackets += minNBestMatchedBrackets;
			worstTotalParsedBrackets += minNBestParsedBrackets;
			iBestIndexes.add(bestIndex);
			iWorstIndexes.add(worstIndex);					
			treeIndex++;
		}
		firstBestFScore = EvalC.fscore(firstTotalMatchedBrackets, totalGoldBrackets, firstTotalParsedBrackets);
		oracleBestFScore = EvalC.fscore(bestTotalMatchedBrackets, totalGoldBrackets, bestTotalParsedBrackets);
		oracleWorstFScore = EvalC.fscore(worstTotalMatchedBrackets, totalGoldBrackets, worstTotalParsedBrackets);
		System.out.println("First best score: " + firstBestFScore);
		System.out.println("Oracle best score: " + oracleBestFScore);
		System.out.println("Oracle worst score: " + oracleWorstFScore);
		pwFirst.close();
		pwBest.close();
		pwWorst.close();
	}
	
	public void makeOneBestFile(File oneBestFile) throws Exception {
		PrintWriter pw = FileUtil.getPrintWriter(oneBestFile);
		while(getNext()) {
			TSNodeLabel oneBestTree = nextNBestParses.get(0);
			pw.println(oneBestTree.toString());
		}
		pw.close();	
	}
	
	public static ArrayList<TSNodeLabel> nextNBest(int nBest, Scanner s) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(nBest);
		int count = 0;
		while(s.hasNextLine() && count<nBest) {
			String line = s.nextLine();
			if (line.equals("")) return result;
			result.add(new TSNodeLabel(line));
			count++;
		}
		while(s.hasNextLine() && !s.nextLine().equals("")) {};
		return result;
	}

	private ArrayList<TSNodeLabel> getNextNBestParses() throws Exception {
		ArrayList<TSNodeLabel> currentNBest = new ArrayList<TSNodeLabel>();
		while(nBestScanner.hasNextLine()) {
			String line = nBestScanner.nextLine();			
			if (line.equals("")) {
				if (currentNBest.isEmpty()) continue;
				break;
			}
			if (!line.startsWith("(")) continue;
			currentNBest.add(new TSNodeLabel(line));
		}
		return currentNBest;
	}

	public static void cfg() throws Exception {
		int nBest = 1000;
		EvalC.REMOVE_SEMANTIC_TAGS = false;
		String workingDir = Parameters.resultsPath + "TSG/CFG/Nbest/";
		File nBestFile = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED.txt");
		File goldFile = new File(workingDir + "wsj-22_clean.mrg");
		File trainigFile = new File(workingDir + "wsj-02-21_TOP.mrg");
		TsgReranker reranker = new TsgReranker(workingDir, trainigFile,goldFile,nBestFile,nBest);
		File oracleBest = new File(workingDir + "oracleBest_semTagOn.mrg");
		reranker.makeOracleBest(oracleBest, false);			
		File evalF = new File(workingDir + "oracle_evalF_semTagOn.txt");
		File evalFlog = new File(workingDir + "oracle_evalF_semTagOn.log");
		EvalC.staticEvalC(goldFile, oracleBest, evalF, evalFlog, true);		
	}
		
	public static void kernelSimple() throws Exception {
		int nBest = 1000;
		//EvalF.REMOVE_SEMANTIC_TAGS = false;
		String workingDir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/parsing/BigGrammar1000L1_SemTagOff/";
		File nBestFile = new File(workingDir + "BITPAR_OUTPUT_POSTPROCESSED.txt");
		File goldFile = new File(workingDir + "wsj-22_clean.mrg");
		//File trainigFile = new File(workingDir + "wsj-02-21_TOP.mrg");
		File oracleBestFile = new File(workingDir + "oracle_best.mrg");
		File oneBestFile = new File(workingDir + "wsj-22_one_best.mrg");
		TsgReranker reranker = new TsgReranker(workingDir, null,goldFile,nBestFile,nBest);
		reranker.makeOracleBest(oracleBestFile, true);
		reranker.makeOneBestFile(oneBestFile);
		File evalF = new File(workingDir + "oracle_evalF_semTagOff.txt");
		File evalFlog = new File(workingDir + "oracle_evalF_semTagOff.log");
		EvalC.staticEvalC(goldFile, oracleBestFile, evalF, evalFlog, true);
	}
	
	public static void standfordKernelOracle() throws Exception {
		String workingDir = Parameters.resultsPath + "TSG/TSGkernels/Wsj/parsing/Standford_BigKernelGrammar/";
		File goldFile = new File(workingDir + "wsj-22_clean.mrg");
		File kernelOracleFile = new File(workingDir + "kernel_oracle_best.mrg");
		File standfordOracleFile = new File(workingDir + "standford_oracle_best.mrg");
		File superOracleFile = new File(workingDir + "super_oracle_best.mrg");
		PrintWriter pw = FileUtil.getPrintWriter(superOracleFile);
		Scanner goldScanner = FileUtil.getScanner(goldFile);
		Scanner kernelScanner = FileUtil.getScanner(kernelOracleFile);
		Scanner standfordScanner = FileUtil.getScanner(standfordOracleFile);		
		while(kernelScanner.hasNextLine()) {
			String goldLine = goldScanner.nextLine();
			TSNodeLabel goldTree = new TSNodeLabel(goldLine);
			String kernelLine = kernelScanner.nextLine();
			TSNodeLabel kernelTree = new TSNodeLabel(kernelLine);
			String standfordLine = standfordScanner.nextLine();
			TSNodeLabel standfordTree = new TSNodeLabel(standfordLine);
			int[] kernelScores = EvalC.getScores(goldTree, kernelTree, true);
			int[] standfordScores = EvalC.getScores(goldTree, standfordTree, true);
			float kernelFScore = EvalC.fscore(kernelScores);
			float standfordFScore = EvalC.fscore(standfordScores);
			if (kernelFScore>=standfordFScore) pw.println(kernelTree.toString());
			else pw.println(standfordTree.toString());			
		}
		pw.close();
		kernelScanner.close();
		standfordScanner.close();
		File evalF = new File(workingDir + "super_oracle_evalF_semTagOff.txt");
		File evalFlog = new File(workingDir + "super_oracle_evalF_semTagOff.log");
		EvalC.staticEvalC(goldFile, superOracleFile, evalF, evalFlog, true);
	}
	
	public static void evalStandfordNBest() throws Exception {
		int nBest = 10;
		String workingDir = Parameters.resultsPath + "StandfordParser/";
		File nBestFileClean = new File(workingDir + "wsj-22_parsed_1000_factored_clean.mrg");
		File goldFile = new File(workingDir + "wsj-22_gold_clean.mrg");
		File oracleBestFile = new File(workingDir + "wsj-22_oracle_best.mrg");
		//File oneBestFile = new File(workingDir + "wsj-22_one_best.mrg");
		TsgReranker reranker = new TsgReranker(workingDir, null,goldFile,nBestFileClean,nBest);
		reranker.makeOracleBest(oracleBestFile, true);
		//reranker.makeOneBestFile(oneBestFile);
		File evalF = new File(workingDir + "oracleBest_evalF_semTagOff.txt");
		File evalFlog = new File(workingDir + "oracleBest_evalF_semTagOff.log");
		EvalC.staticEvalC(goldFile, oracleBestFile, evalF, evalFlog, true);
	}
	
	public static void evalNBest(String workingDir, File goldFile, File nBestFileClean, File oracleBestFile, 
			File oracleWorstFile, File oneBestFile, int nBest) throws Exception {		
		TsgReranker reranker = new TsgReranker(workingDir, null, goldFile, nBestFileClean, nBest);
		reranker.makeOracleFirstBestWorst(oneBestFile, oracleBestFile, oracleWorstFile);
		File evalF = new File(workingDir + "oracleBest_evalF_semTagOff.txt");
		File evalFlog = new File(workingDir + "oracleBest_evalF_semTagOff.log");
			EvalC.staticEvalC(goldFile, oracleBestFile, evalF, evalFlog, true);
		evalF = new File(workingDir + "oracleWorst_evalF_semTagOff.txt");
		evalFlog = new File(workingDir + "oracleWorst_evalF_semTagOff.log");
			EvalC.staticEvalC(goldFile, oracleWorstFile, evalF, evalFlog, true);	
		evalF = new File(workingDir + "oneBest_evalF_semTagOff.txt");
		evalFlog = new File(workingDir + "oneBest_evalF_semTagOff.log");
		EvalC.staticEvalC(goldFile, oneBestFile, evalF, evalFlog, true);
	}
	
	public static void cleanStandfordNBest() throws Exception {
		String workingDir = Parameters.resultsPath + "StandfordParser/";
		File nBestFile = new File(workingDir + "wsj-22_parsed_1000_factored.mrg");
		File nBestFileClean = new File(workingDir + "wsj-22_parsed_1000_factored_clean.mrg");
		Scanner scan = FileUtil.getScanner(nBestFile);
		PrintWriter pw = FileUtil.getPrintWriter(nBestFileClean);
		String currentTreeLines = "";
		String currentTreeFlat = null; 
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith("#")) continue;
			if (line.length()==0) {
				currentTreeLines = currentTreeLines.replaceAll("\\s+", " ").replaceAll("\\\\", "");
				TSNodeLabel currentTree = new TSNodeLabel(currentTreeLines);
				String flat = currentTree.toFlatSentence();
				if (!flat.equals(currentTreeFlat)) {
					if (currentTreeFlat!=null) pw.println();
					currentTreeFlat = flat;					
				}
				pw.println(currentTree.daughters[0].toString());
				currentTreeLines = "";
			}
			currentTreeLines += line;
			//int index = Integer.parseInt(line.substring(8, line.indexOf(' ',9)));
		}
		pw.close();
	}
	
	private static void evalChiarniack() throws Exception {
		String workingDir = Parameters.resultsPath + "ChiarniakParser/wsj22/";
		File wsj22parsed = new File(workingDir + "wsj-22_chiarniak_parsed1000.mrg");
		File wsj22parsedCleaned = new File(workingDir + "wsj-22_chiarniak_parsed1000_cleaned.mrg");
		Scanner scan = FileUtil.getScanner(wsj22parsed);
		PrintWriter pw = FileUtil.getPrintWriter(wsj22parsedCleaned);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) {
				pw.println();
				continue;
			}
			if (!line.startsWith("(")) continue;
			pw.println(line.replaceAll("S1", "TOP").replaceAll("\\\\", ""));
		}
		scan.close();
		pw.close();
		
		File goldFile = new File(workingDir + "wsj-22_clean.mrg");
		File oracleBestFile = new File(workingDir + "wsj-22_oracleBest.mrg");
		File oracleWorstFile = new File(workingDir + "wsj-22_oracleWorst.mrg");
		File oneBestFile = new File(workingDir + "wsj-22_oneBest.mrg");
		evalNBest(workingDir, goldFile, wsj22parsedCleaned, oracleBestFile, oracleWorstFile, oneBestFile, 1000);
	}
	
	private static void getOracleExactMatchCharniack() throws Exception {
		String workingDir = "ChiarniakParser/WSJ23/wsj23_EN_noAux/";
		File best1000File = new File(Parameters.resultsPath + workingDir + "wsj-23_1000best_noAux_cleaned.mrg");
		//File oracleBestFile = new File(Parameters.resultsPath + workingDir + "wsj-23_Oracle1000best_noAux_cleaned.mrg");
		Scanner scan = FileUtil.getScanner(best1000File);
		//PrintWriter pw = FileUtil.getPrintWriter(oracleBestFile);
		File origianlFile = new File(Wsj.WsjOriginalCleanedTop + "wsj-23.mrg");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(origianlFile);
		System.out.println("Total trees: " + treebank.size());
		int exactMatch = 0;
		int treeBankCount = 0;
		for(TSNodeLabel g : treebank) {
			treeBankCount++;
			ArrayList<TSNodeLabel> nBest = nextNBest(1000, scan);
			float maxFScore = -1;
			//TSNodeLabel bestTree = null;
			for(TSNodeLabel t : nBest) {
				//TSNodeLabel copyT = t.clone();
				int[] scores = EvalC.getScores(g, t, true);
				float fscore = EvalC.fscore(scores);
				boolean hasSamePosTag = t.hasSamePosTag(g);
				//if (scores[0]==scores[1] && scores[1]==scores[2]) {
				if (hasSamePosTag && fscore==1f) {
					exactMatch++;
					System.out.println(exactMatch + " / " + treeBankCount);
					//pw.println(copyT.toString());
					//bestTree = null;
					break;
				}
				if (fscore>maxFScore) {
					maxFScore = fscore;
					//bestTree = copyT;
				}	
			}
			/*if (bestTree!=null) {
				pw.println(bestTree.toString());
			}*/
		}		
		System.out.println("Total trees: " + treeBankCount);
		System.out.println("Exact match: " + exactMatch);
		float exactMatchRate = (float)exactMatch / treeBankCount;
		System.out.println("Exact match reate: " + exactMatchRate);
		//pw.close();
	}
	
	public static void main(String[] args) throws Exception {
		//cfg();
		//kernelSimple();
		//cleanStandfordNBest();
		//evalStandfordNBest();
		//standfordKernelOracle();
		//makeFileChiarniack();
		//evalChiarniack();
		getOracleExactMatchCharniack();
		//clean();
	}	
	
}
