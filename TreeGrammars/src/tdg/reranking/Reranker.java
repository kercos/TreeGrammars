package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.MstSentenceUlab;
import tdg.corpora.TanlD;
import tdg.corpora.TutD;
import tdg.corpora.WsjD;
import util.*;
import util.file.FileUtil;
import mstparser.*;

import org.apache.commons.io.FileUtils;

public abstract class Reranker {
	
	//public static boolean markVerbDomination = true;
	//public static int markFirstLevelsNodes = 2;	
	
	File goldFile;
	TDNode gold, oneBest, oracleBest, oracleWorse, rerankedFirst, rerankedBest, rerankedWorse;
	int oneBestIndex, oracleBestIndex, rerankedFirstIndex, oracleWorseIndex, rerankedBestIndex, rerankedWorseIndex;
	int oneBestUAS, oracleBestUAS, oracleWorseUAS, rerankedFirstUAS, rerankedBestUAS, rerankedWorseUAS;
	String goldScore, oneBestScore, oracleBestScore, oracleWorseScore, rerankedScore;
	int length, zeroScoreSentences, zeroScoreTotalLength, sentenceIndex, allNullNBestSentences;
	String[] currentWords, currentPostags;
	int[] oneBestIndexes;
	TDNode[] nBest;
	Scanner parsedScanner, goldScanner;
	int n, uk_limit, limitTestToFirst;
	Set<String> lexicon;
	boolean addEOS, hasEOS, countEmptyDaughters, markTerminalNodes;	
	ArrayList<TDNode> trainingCorpus;	
	File renrankedInfoFile = new File(Parameters.outputPath + "reranking.info.txt");
	File renrankedUlabFile = new File(Parameters.outputPath + "reranking.ulab");
	PrintWriter outputRerankedInfo = FileUtil.getPrintWriter(renrankedInfoFile);
	PrintWriter outputRerankedUlab =	FileUtil.getPrintWriter(renrankedUlabFile);
	File renrankedIndexesFile = new File(Parameters.outputPath + "reranking.indexes.txt");
	public static ArrayList<Integer> rerankedIndexes = new ArrayList<Integer>();
	
	String[] nBestScoresRecords;
	
	int totalWords=0;
	int totalOneBestUAS, totalOracleBestUAS, totalOracleWorseUAS;
	int totalRerankedFirstUAS, totalRerankedBestUAS, totalRerankedWorseUAS;
	int totalLowerRerankedFirst, totalAboveRerankedFirst;
	
	String left, right;
	
	public Reranker(File goldFile, File parsedFile, int n, boolean addEOS,
			ArrayList<TDNode> trainingCorpus, int uk_limit, boolean countEmptyDaughters,
			boolean markTerminalNodes, int limitTestToFirst) {
		
		this.goldFile = goldFile;
		goldScanner = FileUtil.getScanner(goldFile);
		parsedScanner = FileUtil.getScanner(parsedFile);
		this.n = n;
		this.addEOS = addEOS;
		this.hasEOS = addEOS || DepCorpus.checkIfPresentEOS(trainingCorpus);
		this.trainingCorpus = trainingCorpus;
		this.uk_limit = uk_limit;
		this.countEmptyDaughters = countEmptyDaughters;
		this.markTerminalNodes = markTerminalNodes;
		this.limitTestToFirst = limitTestToFirst;
		
		if(uk_limit > 0) lexicon = DepCorpus.extractLexiconWithoutUnknown(trainingCorpus, uk_limit);
		if (addEOS) DepCorpus.addEOS(trainingCorpus);
		if (countEmptyDaughters) DepCorpus.addEmptyDaughters(trainingCorpus);
		if (markTerminalNodes) DepCorpus.markTerminalNodes(trainingCorpus);
		//if (markVerbDomination) DepCorpus.markVerbDomination(trainingCorpus);
		//if (markFirstLevelsNodes!=-1) 
		//	DepCorpus.markFirstLevelNodes(trainingCorpus, markFirstLevelsNodes);
	}
	
	public void runToyGrammar() {
		TDNode toySentence = trainingCorpus.get(0);
		String scoreOfToySentence = getScoreAsString(toySentence);
		
		Vector<String> keyCondKeyLog = new Vector<String>();		
		updateKeyCondKeyLog(toySentence, keyCondKeyLog);
		String keyCondKeyLogPrint = "Keys | condKeys: " + keyCondKeyLog;
		String lofLine = "Probability of toy sentence:  " + scoreOfToySentence;
		FileUtil.appendReturn(keyCondKeyLogPrint + "\n\n" + lofLine, Parameters.logFile);
	}
	
	public void reranking() {
		if (n==1) rerankingFromVariation();
		else rerankingFromNBest();
	}
	
	public void rerankingFromNBest() {
		sentenceIndex = 0;
		util.PrintProgressStatic.start("Reranking sentence");				
		do {			
			sentenceIndex++;
			util.PrintProgressStatic.next();
			getGold();			
			if (gold==null) break;			
			getNBest(currentWords, currentPostags);
			if (allNBestNull()) {
				allNullNBestSentences++;
				continue;
			}
			outputRerankedInfo.flush();
			outputRerankedUlab.flush();
			getOneBest();
			getOracle();
			length = gold.lengthWithoutEmpty();
			boolean zeroScore = getRerankingScore();
			if (hasEOS) length--;
			if (zeroScore) {
				zeroScoreSentences++; 
				zeroScoreTotalLength += length;			
				totalRerankedBestUAS += oneBestUAS;
				totalRerankedWorseUAS += oneBestUAS;
			}									
			else {
				totalRerankedBestUAS += rerankedBestUAS;
				totalRerankedWorseUAS += rerankedWorseUAS;
			}
			totalWords += length;
			totalOneBestUAS += oneBestUAS; 			
			totalOracleBestUAS += oracleBestUAS;
			totalOracleWorseUAS += oracleWorseUAS;
			totalRerankedFirstUAS += rerankedFirstUAS;			
			int rerankedFirstDiff = rerankedFirstUAS - oneBestUAS;
			
			if (rerankedFirstDiff<0) totalLowerRerankedFirst += -rerankedFirstDiff;
			else totalAboveRerankedFirst += rerankedFirstDiff;			
			
			rerankedIndexes.add(rerankedFirstIndex);
			outputRerankedUlab.println(rerankedFirst.toStringMSTulab(false, countEmptyDaughters) + "\n");
			
			if (rerankedFirstUAS!=oneBestUAS) {
				// if the reranking best choice differes from 1-best or
				System.out.println("\nSencence # " + sentenceIndex + "\t" + rerankedFirstDiff + 
						"\t-" + totalLowerRerankedFirst + "\t+" + totalAboveRerankedFirst + "\n");
				
				oracleBestScore = getScoreAsString(oracleBestIndex);
				oracleWorseScore = getScoreAsString(oracleWorseIndex);
				oneBestScore = getScoreAsString(oneBestIndex);
				goldScore = getScoreAsString(gold);
				rerankedScore = getScoreAsString(rerankedFirstIndex);										
				outputRerankedInfo.println(
						  "#: " + sentenceIndex + "   GOLD" + "   KScore: " + goldScore	  		
						  		+ "\n" + gold.toStringMSTulab(false, countEmptyDaughters) + "\n" +		
						  "#: " + sentenceIndex + "   ORACLE BEST" + "   nBest: " + (oracleBestIndex+1)
						  		+ "   UAS: " + oracleBestUAS + "/" + length + "   KScore: " 
						  		+ oracleBestScore
						  		+ "\n" + oracleBest.toStringMSTulab(false, countEmptyDaughters) + "\n" +
						  "#: " + sentenceIndex + "   ORACLE WORSE" + "   nBest: " + (oracleWorseIndex+1)
								+ "   UAS: " + oracleWorseUAS + "/" + length + "   KScore: " 
							  	+ oracleWorseScore
							  	+ "\n" + oracleWorse.toStringMSTulab(false, countEmptyDaughters) + "\n"  +
						  "#: " + sentenceIndex + "   1Best" + "   UAS: " + oneBestUAS + "/" 
						  		+ length + "   KScore: " + oneBestScore
						  		+ "\n" + oneBest.toStringMSTulab(false, countEmptyDaughters) + "\n" +
						  "#: " + sentenceIndex + "   RERANKED FIRST" + "   nBest: " + (rerankedFirstIndex+1) 
						  		+ "   UAS: " + rerankedFirstUAS + "/" + length  + "   KScore: " 
						  		+ rerankedScore   
						  		+ "\n" + rerankedFirst.toStringMSTulab(false, countEmptyDaughters) + "\n" +
						  "#: " + sentenceIndex + "   RERANKED BEST" + "   nBest: " + (rerankedBestIndex+1) 
						  		+ "   UAS: " + rerankedBestUAS + "/" + length  + "   KScore: " 
						  		+ rerankedScore   
						  		+ "\n" + rerankedBest.toStringMSTulab(false, countEmptyDaughters) + "\n" +
						  "#: " + sentenceIndex + "   RERANKED WORSE" + "   nBest: " + (rerankedWorseIndex+1) 
						  		+ "   UAS: " + rerankedWorseUAS + "/" + length  + "   KScore: " 
						  		+ rerankedScore   
						  		+ "\n" + rerankedWorse.toStringMSTulab(false, countEmptyDaughters) + "\n");	
			}
			if (sentenceIndex == limitTestToFirst) break;	
		} while(true);
		util.PrintProgressStatic.end();
		sentenceIndex--;	
		int upDownDiff = totalAboveRerankedFirst - totalLowerRerankedFirst;
		String results = "Number of sentences in test: " + sentenceIndex + "\n" +
						"1 Best UAS: " + totalOneBestUAS + "\t" + 
						totalWords + "\t" + (float)totalOneBestUAS/totalWords + "\n" +
						"Oracle Best UAS : " + totalOracleBestUAS + "\t" + 
						totalWords + "\t" + (float)totalOracleBestUAS/totalWords + "\n" +
						"Oracle Worse UAS : " + totalOracleWorseUAS + "\t" + 
						totalWords + "\t" + (float)totalOracleWorseUAS/totalWords + "\n\n" +
						"Sentences with all non valid nBest: " + allNullNBestSentences + "\n" +
						"Sentences with zero reranking: " + zeroScoreSentences + "\n" +
						"Total Length of sentences zero reranking: " + zeroScoreTotalLength + "\n" +
						"Reranked First UAS : " + totalRerankedFirstUAS + "\t" + 
						totalWords + "\t" + (float)totalRerankedFirstUAS/totalWords + "\n" +
						"Reranked First up/down 1-best : +" + totalAboveRerankedFirst + "\t-" 
							+ totalLowerRerankedFirst + " (" + upDownDiff + ")\n\n" +
						"Reranked Best UAS : " + totalRerankedBestUAS + "\t" + 
						totalWords + "\t" + 
						(float)totalRerankedBestUAS/totalWords + "\n\n" +
						"Reranked Worse UAS : " + totalRerankedWorseUAS + "\t" + 
						totalWords + "\t" + 
						(float)totalRerankedWorseUAS/totalWords;
						
		System.out.println(results);		
		FileUtil.appendReturn(results, Parameters.logFile);
		goldScanner.close();
		parsedScanner.close();
		outputRerankedInfo.close();		
		outputRerankedUlab.close();
		FileUtil.appendReturn(rerankedIndexes.toString(), renrankedIndexesFile);
	}
	
	public void rerankingFromVariation() {
		sentenceIndex = 0;
		util.PrintProgressStatic.start("Reranking sentence");				
		do {			
			sentenceIndex++;
			util.PrintProgressStatic.next();
			getGold();			
			if (gold==null) break;			
			getNBest(currentWords, currentPostags);
			if (allNBestNull()) {
				allNullNBestSentences++;
				continue;
			}
			outputRerankedInfo.flush();
			outputRerankedUlab.flush();
			getOneBest();			
			length = gold.lengthWithoutEmpty();
			getBestScoreVariations();			
			if (hasEOS) length--;			
			totalWords += length;
			totalOneBestUAS += oneBestUAS; 			
			totalRerankedFirstUAS += rerankedFirstUAS;			
			int rerankedFirstDiff = rerankedFirstUAS - oneBestUAS;
			
			if (rerankedFirstDiff<0) totalLowerRerankedFirst += -rerankedFirstDiff;
			else totalAboveRerankedFirst += rerankedFirstDiff;
			
			if (rerankedFirstUAS!=oneBestUAS) {
				// if the reranking best choice differes from 1-best or
				System.out.println("\nSencence # " + sentenceIndex + "\t" + rerankedFirstDiff + 
						"\t-" + totalLowerRerankedFirst + "\t+" + totalAboveRerankedFirst + "\n");
				
				oneBestScore = getScoreAsString(oneBest);
				goldScore = getScoreAsString(gold);
				outputRerankedUlab.println(rerankedFirst.toStringMSTulab(false, countEmptyDaughters) + "\n");
				outputRerankedInfo.println(
						  "#: " + sentenceIndex + "   GOLD" + "   KScore: " + goldScore	  		
						  		+ "\n" + gold.toStringMSTulab(false, countEmptyDaughters) + "\n" +		
						  "#: " + sentenceIndex + "   1Best" + "   UAS: " + oneBestUAS + "/" 
						  		+ length + "   KScore: " + oneBestScore
						  		+ "\n" + oneBest.toStringMSTulab(false, countEmptyDaughters) + "\n" +
						  "#: " + sentenceIndex + "   VARIATION FIRST" + "   nBest: " + (rerankedFirstIndex+1) 
								+ "   UAS: " + rerankedFirstUAS + "/" + length  + "   KScore: " 
							  	+ rerankedScore   
							  	+ "\n" + rerankedFirst.toStringMSTulab(false, countEmptyDaughters) + "\n");
						  /*"#: " + sentenceIndex + "   VARIATION BEST" + "   nBest: " + (rerankedBestIndex+1) 
							  	+ "   UAS: " + rerankedBestUAS + "/" + length  + "   KScore: " 
							  	+ rerankedScore   
							  	+ "\n" + rerankedBest.toStringMSTulab(false, countEmptyDaughters) + "\n" +
						  "#: " + sentenceIndex + "   VARIATION WORSE" + "   nBest: " + (rerankedWorseIndex+1) 
							  	+ "   UAS: " + rerankedWorseUAS + "/" + length  + "   KScore: " 
							  	+ rerankedScore   
							  	+ "\n" + rerankedWorse.toStringMSTulab(false, countEmptyDaughters) + "\n");*/	
			}
			if (sentenceIndex == limitTestToFirst) break;	
		} while(true);
		util.PrintProgressStatic.end();
		sentenceIndex--;
		String results = "Number of sentences in test: " + sentenceIndex + "\n" +
						"1 Best UAS: " + totalOneBestUAS + "\t" + 
						totalWords + "\t" + (float)totalOneBestUAS/totalWords + "\n" +
						"Sentences with all non valid nBest: " + allNullNBestSentences + "\n" +
						"Sentences with zero reranking: " + zeroScoreSentences + "\n" +
						"Total Length of sentences zero reranking: " + zeroScoreTotalLength + "\n" +
						"Variation First UAS : " + totalRerankedFirstUAS + "\t" + 
						totalWords + "\t" + (float)totalRerankedFirstUAS/totalWords + "\n" +
						"Variation First up/down 1-best : +" + totalAboveRerankedFirst + "\t-" 
							+ totalLowerRerankedFirst + "\n\n";
						/*"Variation Best UAS : " + totalRerankedBestUAS + "\t" + 
						totalWords + "\t" + 
						(float)totalRerankedBestUAS/totalWords + "\n\n" +
						"Variation Worse UAS : " + totalRerankedWorseUAS + "\t" + 
						totalWords + "\t" + 
						(float)totalRerankedWorseUAS/totalWords;*/					
		System.out.println(results);
		FileUtil.appendReturn(results, Parameters.logFile);
		goldScanner.close();
		parsedScanner.close();
		outputRerankedInfo.close();		
	}
	
	
	public void printNBestScores(String[] words, String[] postags) {
		File nBestScoresFile = new File(Parameters.outputPath + "nBestScores");
		PrintWriter pw = FileUtil.getPrintWriter(nBestScoresFile);
		do {			
			getGold();			
			if (gold==null) break;			
			getNBest(words, postags);
			if (allNBestNull()) continue;
			getRerankingScore();
			for(String score : nBestScoresRecords) {
				pw.println(score);
			}
			pw.println();
		} while(true);
		pw.close();
	}
	
	
	public boolean allNBestNull() {
		for(TDNode t : nBest) {
			if (t!=null) return false;
		}
		return true;
	}
	
	public void getOracle() {		
		int index = -1;
		oracleBestUAS = -1;
		oracleWorseUAS = Integer.MAX_VALUE;
		for(TDNode ibest : nBest) {
			index++;
			if (ibest==null) continue;
			int UAS = ibest.UAS(gold, countEmptyDaughters, !hasEOS);
			if (UAS < 0) continue; //evaluation prob.
			if (UAS>oracleBestUAS) {
				oracleBestUAS = UAS;
				oracleBestIndex = index;
				oracleBest = ibest;
			}
			if (UAS<oracleWorseUAS) {
				oracleWorseUAS = UAS;
				oracleWorseIndex = index;
				oracleWorse = ibest;
			}			
		}		
	}

	public void getNBest(String[] words, String[] postags) {
		nBest = new TDNode[n];
		String indexes;		
		int i;
		boolean isFirst = true;
		for(i=0; i<n; i++) {
			String wordsLine = parsedScanner.nextLine(); //words 
			if (wordsLine.length()==0) break; // reached end of nbest before n
			parsedScanner.nextLine(); //postags 
			//labels = parsedScanner.nextLine();
			indexes = parsedScanner.nextLine();
			int[] indexesArray = Utility.parseIndexList(indexes.split("\t"));
			if (hasMoreThanOneRoot(indexesArray, 0)) continue;
			if (isFirst) {
				oneBestIndexes = indexesArray;
				isFirst = false;
			}			
			TDNode iBest = new TDNode(words, postags, indexesArray, 1, 0);
			if (uk_limit > 0) iBest.renameUnknownWords(lexicon, DepCorpus.ukTag);
			if (addEOS) iBest.addEOS();
			if (countEmptyDaughters) iBest.addEmptyDaughters();
			if (markTerminalNodes) iBest.markTerminalNodesPOS();
			//if (markVerbDomination) iBest.markVerbDominationPOS();
			//if (markFirstLevelsNodes!=-1) iBest.markFirstLevelNodesPOS(0, markFirstLevelsNodes);
			nBest[i] = iBest;				
		}		
		if (i==n) parsedScanner.nextLine(); // empty line after n best
	}
	
	public void getOneBest() {
		int i=0;
		do {
			oneBest = nBest[i];
			oneBestIndex = i;
			i++;
		} while(oneBest==null && i<n);		
		oneBestUAS = oneBest.UAS(gold, countEmptyDaughters, !hasEOS);		
	}

	public void getGold() {
		while(goldScanner.hasNextLine()) {
			String words = goldScanner.nextLine();
			if (words.length()==0) continue;
			String postags = goldScanner.nextLine();
			//String labels = goldScanner.nextLine();
			String indexes = goldScanner.nextLine();
			currentWords = words.split("\t");
			currentPostags = postags.split("\t");
			gold = new TDNode(currentWords, currentPostags, 
					Utility.parseIndexList(indexes.split("\t")), 1, 0);
			if (uk_limit > 0) gold.renameUnknownWords(lexicon, DepCorpus.ukTag);
			if (addEOS) gold.addEOS();
			if (countEmptyDaughters) gold.addEmptyDaughters();
			if (markTerminalNodes) gold.markTerminalNodesPOS();
			//if (markVerbDomination) gold.markVerbDominationPOS();
			//if (markFirstLevelsNodes!=-1) gold.markFirstLevelNodesPOS(0, markFirstLevelsNodes);
			return;
		}
		gold = null;
	}
	
	public List<TDNode> getGoldAsTreebank() {
		ArrayList<TDNode> goldCorpus = new ArrayList<TDNode>(); 
		Scanner goldScanner = FileUtil.getScanner(goldFile);
		while(goldScanner.hasNextLine()) {
			String words = goldScanner.nextLine();
			if (words.length()==0) continue;
			String postags = goldScanner.nextLine();
			//String labels = goldScanner.nextLine();
			String indexes = goldScanner.nextLine();
			gold = new TDNode(words.split("\t"), postags.split("\t"), 
					Utility.parseIndexList(indexes.split("\t")), 1, 0);
			if (uk_limit > 0) gold.renameUnknownWords(lexicon, DepCorpus.ukTag);
			if (addEOS) gold.addEOS();
			if (countEmptyDaughters) gold.addEmptyDaughters();
			if (markTerminalNodes) gold.markTerminalNodesPOS();
			//if (markVerbDomination) gold.markVerbDominationPOS();
			//if (markFirstLevelsNodes!=-1) gold.markFirstLevelNodesPOS(0, markFirstLevelsNodes);
			goldCorpus.add(gold);
		}
		return goldCorpus;
	}
	
	public static String removeMarks(String s) {
		return removeArgumentMark(removeTerminalMark(s));
	}
	
	public static String removeArgumentMark(String s) {
		if (s.endsWith("-A")) return s.substring(0, s.length()-2);
		return s;
	}
	
	public static String removeTerminalMark(String s) {
		if (s.endsWith("-T")) return s.substring(0, s.length()-2);
		return s;
	}
	
	static public boolean hasMoreThanOneRoot(int[] indexesArray, int rootIndex) {
		int count = 0;
		for(int i : indexesArray) {
			if (i==rootIndex) count++;
		}
		return (count>1);
	}
	
	public abstract void initBestRerankedScore();
	public abstract int updateRerankedScore(TDNode t, int index, String[] nBestScoresRecords);
	public abstract String getScoreAsString(TDNode t);
	public abstract Number getScore(TDNode t);
	public abstract int compareTo(Number a, Number b);
	public abstract void updateKeyCondKeyLog(TDNode thisNode, Vector<String> keycondKeyLog);
	
	
	private String getScoreAsString(int index) {
		return nBestScoresRecords[index];
	}
		
	
	public abstract boolean bestRerankedIsZeroScore();
	
	public boolean getRerankingScore() {
		initBestRerankedScore();
		nBestScoresRecords = new String[n];
		int index = -1;
		for(TDNode ibest : nBest) {			
			index++;
			if (ibest==null) continue;
			int compare = updateRerankedScore(ibest, index, nBestScoresRecords);
			if (compare==-1) continue;
			if (compare==1) {				
				rerankedFirst = rerankedWorse = rerankedBest = ibest;
				rerankedFirstIndex = rerankedWorseIndex = rerankedBestIndex = index;				
				rerankedFirstUAS = rerankedWorseUAS = rerankedBestUAS = 
					ibest.UAS(gold, countEmptyDaughters, !hasEOS);				
			}			
			else { //equals
				int UAS = ibest.UAS(gold, countEmptyDaughters, !hasEOS);
				if (UAS>rerankedBestUAS) {
					rerankedBestUAS = UAS;
					rerankedBestIndex = index;
					rerankedBest = ibest;
				}
				else if (UAS<rerankedWorseUAS) {
					rerankedWorseUAS = UAS;
					rerankedWorseIndex = index;
					rerankedWorse = ibest;
				}
			}
		}
		return bestRerankedIsZeroScore();
	}	
	
	// put result in rerankedFirst
	public void getBestScoreVariations() {		
		MstSentenceUlab oneBestMst = new MstSentenceUlab(oneBest);
		MstSentenceUlab nextBestVariations = oneBestMst;
		Number bestScore = getScore(oneBest);	
		rerankedFirst = rerankedWorse = rerankedBest = oneBest;								
		rerankedFirstUAS = rerankedWorseUAS = rerankedBestUAS = oneBestUAS;
		
		while(nextBestVariations!=null) {
			ArrayList<MstSentenceUlab> stepVariations = nextBestVariations.getOneStepVariation();			
			nextBestVariations = null;
			 
			for(MstSentenceUlab v : stepVariations) {
				TDNode t = v.toTDNode();
				Number score = getScore(t);
				int compare = compareTo(score, bestScore);
				if (compare==-1) continue;
				if (compare>0) {				
					rerankedFirst = rerankedWorse = rerankedBest = t;								
					rerankedFirstUAS = rerankedWorseUAS = rerankedBestUAS = 
						t.UAS(gold, countEmptyDaughters, !hasEOS);
					bestScore = score;
					nextBestVariations = v;
				}			
				/*else { //equals
					int UAS = t.UAS(gold, countEmptyDaughters, !hasEOS);
					if (UAS>rerankedBestUAS) {
						rerankedBestUAS = UAS;
						rerankedBest = t;
					}
					else if (UAS<rerankedWorseUAS) {
						rerankedWorseUAS = UAS;
						rerankedWorse = t;
					}
				}*/							
			}
		}					
		rerankedScore = bestScore.toString();
	}
	
	/*public static void checkCoverage(File outputFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, boolean countEmptyDaughters, 
			int DKG_type, int maxBranching, int collectionType, int SLP_type,
			boolean adjacency, boolean addEOS) {
		PrintWriter output = FileUtil.getPrintWriter(outputFile);
		int scoreType = 1; //maxSpanning
		Scanner parsedScanner = FileUtil.getScanner(parsedFile);
		int sentences = 0;
		int covered = 0;
		Set<String> lexicon = null;
		if(uk_limit > 0) lexicon = DepCorpus.extractLexiconWithoutUnknown(trainingCorpus, uk_limit);
		util.PrintProgress.start("Checking Coverage");
		do {
			util.PrintProgress.next();
			TDNode[] Kbest = getKBest(parsedScanner, nBest, lexicon, addEOS);
			if (Kbest==null) break;
			sentences++;
			int sentenceLength = Kbest[0].length();
			boolean found = false;
			for(TDNode tdn : Kbest) {
				if (tdn==null) break;
				if(uk_limit > 0) tdn.renameUnknownWords(lexicon, DepCorpus.ukTag);
				int[] maxSpanning = DepKernelGraph.computeKernelScore(scoreType, DKG_type,
						tdn, trainingCorpus, -5, maxBranching, collectionType, SLP_type,
						countEmptyDaughters, adjacency);
				if (maxSpanning[0] == sentenceLength) {
					covered++;
					found = true;
					break;
				}
			}
			if (!found) {
				output.println(sentences);
				output.println(Kbest[0].toStringMSTulab(false));
				output.println();
			}
			output.flush();
		} while(true);
		util.PrintProgress.end();
		System.out.println("Total Sentences\t" + sentences);
		System.out.println("Total Covered\t" + covered);
		output.close();
		
	}*/
	
	/*public static void getOracle(File goldFile, File parsedFile, File outputFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit,
			boolean countEmptyDaughters, int DKG_type, int maxBranching, int collectionType, 
			int SLP_type, int limitTestToFirst, boolean adjacency, boolean addEOS) {

		String baseOutputFile = outputFile.toString();
		Set<String> lexicon = null;
		if(uk_limit > 0) lexicon = DepCorpus.extractLexiconWithoutUnknown(trainingCorpus, uk_limit);		
		Scanner goldScanner = FileUtil.getScanner(goldFile);
		Scanner parsedScanner = FileUtil.getScanner(parsedFile);		
		int sentenceIndex = 1;
		util.PrintProgress.start("Reranking sentence");
		do {			
			util.PrintProgress.next();
			TDNode gold = getGold(goldScanner, addEOS);
			if(uk_limit > 0) gold.renameUnknownWords(lexicon, DepCorpus.ukTag);
			if (gold==null) break;
			TDNode[] Kbest = getKBest(parsedScanner, nBest, lexicon, addEOS);
			TDNode oneBest = Kbest[0];			
			int length = gold.length();
			int bestUAS = Kbest[0].UAS(gold);			
			int[] bOS = bestOracleScore(Kbest, gold);				
			if (bOS[1]!=0) { 
				// if the oracle choice differes from 1-best or
				TDNode oracleBest = Kbest[bOS[1]];
				outputFile = new File(baseOutputFile + "_" + sentenceIndex);
				PrintWriter output = FileUtil.getPrintWriter(outputFile);		
					
				output.println(
						"#: " + sentenceIndex + "   GOLD" + "\n" + 
						gold.toStringMSTulab(false) + "\n" +
						DepKernelGraph.toStringGraphKernels(DKG_type, gold, trainingCorpus, 
								-5, maxBranching, collectionType, SLP_type, countEmptyDaughters,
								adjacency) +
						"\n---------------------------------------------------------------\n\n"
				);
				output.println(
						"#: " + sentenceIndex + "   ORACLE" + "   nBest: " + (bOS[1]+1) + 
						"   UAS: " + bOS[0] + "/" + length + "\n" +		
						oracleBest.toStringMSTulab(false)  + "\n" +
						DepKernelGraph.toStringGraphKernels(DKG_type, oracleBest, trainingCorpus, 
								-5, maxBranching, collectionType, SLP_type, countEmptyDaughters,
								adjacency) +
						"\n---------------------------------------------------------------\n\n"
				);	

				output.close();
			}
			if (sentenceIndex > limitTestToFirst) break;
			sentenceIndex++;			
		} while(true);
		util.PrintProgress.end();
		sentenceIndex--;
		goldScanner.close();
		parsedScanner.close();		
		
	}*/
	
	public static void makeNBestOfShorterLength() {
		String basePath = Parameters.resultsPath + "Reranker/Parsed/" + "goldPOS/COLLINS99Arg_sec22_nBest10/";
		File inputNBestFile = new File(basePath + "tr02-21_LLtr40_LLts40_nBest10");		
		File outputNBestFile = new File(basePath + "tr02-21_LLtr40_LLts10_nBest10");
		int new_LL = 10;
		Scanner scanner = FileUtil.getScanner(inputNBestFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputNBestFile);
		boolean writtenBlock = false;
		while(scanner.hasNextLine()) {			
			String words = scanner.nextLine();
			if (words.length()==0) {
				if (writtenBlock) {
					pw.println();
					writtenBlock = false;
				}				
				continue;
			}
			String postags = scanner.nextLine();
			//labels = parsedScanner.nextLine();
			String indexes = scanner.nextLine();
			int[] indexesArray = Utility.parseIndexList(indexes.split("\t"));
			if (indexesArray.length>new_LL) continue;			
			pw.println(words + "\n" + postags + "\n" + indexes);
			writtenBlock = true;
		}
		
		pw.close();
		scanner.close();		
	}
	
	public static void makeNBestOfLessN() {
		int oldN = 1000;
		int newN = 7;
		String corpusName = "COLLINS99_sec23";
		String basePath = 	Parameters.resultsPath + "Reranker/Parsed/MST_0.5_2order/" + 
							"goldPOS/" + corpusName + "/";
		String inputBasePath = 	basePath + corpusName + "_nBest"  + oldN + "/";
		String outputBasePath = 	basePath + corpusName + "_nBest"  + newN + "/";
		new File(outputBasePath).mkdir();
		File inputNBestFile = new File(inputBasePath + "tr02-21_LLtr40_LLts40_nBest" + oldN);		
		File outputNBestFile = new File(outputBasePath + "tr02-21_LLtr40_LLts40_nBest" + newN);
		Scanner scanner = FileUtil.getScanner(inputNBestFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputNBestFile);
		boolean writtenBlock = false;
		int counter = 0;
		while(scanner.hasNextLine()) {			
			String words = scanner.nextLine();
			if (words.length()==0) {
				if (writtenBlock) {
					pw.println();
					writtenBlock = false;
					counter=0;
				}				
				continue;
			}
			String postags = scanner.nextLine();
			//labels = parsedScanner.nextLine();
			String indexes = scanner.nextLine();
			if (counter<newN) {			
				pw.println(words + "\n" + postags + "\n" + indexes);
				writtenBlock = true;
			}
			counter++;
		}
		
		pw.close();
		scanner.close();	
	}
	
	public static void makeNBestOfLessNConnl() {
		int currentN = 100;
		int newN = 1;
		String basePath = 	Parameters.resultsPath + "Reranker/Parsed/MST_0.5_2order/" + 
							"goldPOS/Tanl_Evalita09Develop/";
		String inputPath = 	basePath + "Tanl_Evalita09Develop_nBest" + currentN + "/";
		String outputPath = 	basePath + "Tanl_Evalita09Develop_nBest" + newN + "/";
		new File(outputPath).mkdir();
		File inputNBestFile = new File(inputPath + "isst_dev_nBest" + currentN);		
		File outputNBestFile = new File(outputPath + "isst_dev_nBest" + newN);
		Scanner scanner = FileUtil.getScanner(inputNBestFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputNBestFile);
		int counter = 0;
		boolean priviousIsEmtpyLine = false;
		while(scanner.hasNextLine()) {			
			String line = scanner.nextLine();			
			if (line.length()>0) {
				if (counter<newN) pw.println(line);
				priviousIsEmtpyLine = false;				
			}
			else {				
				if (priviousIsEmtpyLine) {
					counter = 0;
					pw.println();
				}
				else {
					if (counter<newN) pw.println();
					counter++;								
				}
				priviousIsEmtpyLine = true;													
			}									
		}		
		pw.close();
		scanner.close();	
	}
	
	public static void makeNBestWithGold() {
		int lengthLimit = 40;
		String basePath = 	Parameters.resultsPath + "Reranker/Parsed/" + 
							"goldPOS/COLLINS99_sec22_nBest5/";
		File inputNBestFile = new File(basePath + "tr02-21_LLtr40_LLts40_nBest5");
		File outputNBestFile = new File(basePath + "tr02-21_LLtr40_LLts40_nBest5_withGold");
		File goldFile = new File(WsjD.WsjCOLLINS99_ulab + "wsj-22.ulab");
		Scanner scanner = FileUtil.getScanner(inputNBestFile);
		Scanner goldScanner = FileUtil.getScanner(goldFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputNBestFile);
		boolean writtenBlock = false;
		while(scanner.hasNextLine()) {		
			String words = scanner.nextLine();
			if (words.length()==0) {
				if (writtenBlock) {
					do {
						String wordsGold = goldScanner.nextLine();
						String postagsGold = goldScanner.nextLine();
						String indexesGold = goldScanner.nextLine();
						goldScanner.nextLine(); // emptyline
						int[] indexesArray = Utility.parseIndexList(indexesGold.split("\t"));						
						if (indexesArray.length>lengthLimit) continue;						
						pw.println(wordsGold + "\n" + postagsGold + "\n" + indexesGold);
						break;
					} while(true);										
					pw.println();
					writtenBlock = false;
				}				
				continue;
			}
			String postags = scanner.nextLine();
			//labels = parsedScanner.nextLine();
			String indexes = scanner.nextLine();
			pw.println(words + "\n" + postags + "\n" + indexes);
			writtenBlock = true;
		}		
		pw.close();
		scanner.close();	
	}
	
	public static String[] wsjDepTypeName = 
		new String[]{"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter", "COLLINS99ArgTer"};
	public static String[] depTypeBase = new String[]{WsjD.WsjMSTulab, WsjD.WsjCOLLINS97_ulab, 
		WsjD.WsjCOLLINS99_ulab, WsjD.WsjCOLLINS99Arg_ulab, WsjD.WsjCOLLINS99Ter_ulab,
		WsjD.WsjCOLLINS99ArgTer_ulab};
	
	public static void makeNBestWsj(int LL_tr, int LL_ts, int nBest, boolean till11_21, 
			int depType, boolean mxPos, int order, String MSTver) {
		
		int section = 22;
		String mstVerOrderDir = "MST_" + MSTver + "_" + order + "order";
		String basePath = Parameters.resultsPath + "Reranker/Parsed/" +
							mstVerOrderDir + "/";
		String corpusName = wsjDepTypeName[depType] + "_sec" + section;
		String outputPathParent = basePath +
							((mxPos) ? "mxPOS/" : "goldPOS/") + corpusName + "/"; 
		String outputPath = outputPathParent + corpusName + "_nBest" + nBest + "/";
		
		String baseDepDir = depTypeBase[depType];
		
		File outputPathFile = new File(outputPath);
		outputPathFile.mkdirs();
		
		//mstparser.ParserOptions.testK = nBest;
		mstparser.DependencyParser.testK = nBest;
		
		String trSec = till11_21 ? "02-11" : "02-21";
		File trainingFile = new File (baseDepDir + "wsj-" + trSec + ".ulab");
		File testFile = new File (baseDepDir + "wsj-" + section + ((mxPos) ? ".mxpost" : "") + ".ulab");
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);		
		
		File outputTraining = new File(outputPath + wsjDepTypeName[depType] + "." + trSec + ".ulab");
		File outputTestBlind = new File(outputPath + wsjDepTypeName[depType] + "." + section + ".test.ulab");
		File parsedFile = new File(outputPath + "tr" + trSec + 
				"_LLtr" + LL_tr + "_LLts" + LL_ts + "_nBest" + nBest);
		
		DepCorpus.toFileMSTulab(outputTraining, training, false);
		DepCorpus.toFileMSTulab(outputTestBlind, test, true);
		
		String[] argsTrainTestEval = {
				"train", 
				"train-file:" + outputTraining,
				"model-name:" + outputPathParent + "dep.model",
				"training-iterations:" + 10,
				"decode-type:" + "proj",
				"training-k:" + 1,
				"loss-type:" + "punc",
				"create-forest:" + "true",
				"order:" + order,
				"format:MST",
				
				"test",				
				"test-file:" + outputTestBlind,
				"model-name:" + outputPathParent + "dep.model",
				"output-file:" + parsedFile ,
				"decode-type:" + "proj",
				"order:" + order,
				"format:MST"
		};	
		
		System.out.println(Arrays.toString(argsTrainTestEval));		

		try{
		mstparser.DependencyParser.main(argsTrainTestEval);
		} catch (Exception e) {FileUtil.handleExceptions(e);}
	}
	
	public static void makeNBest(int LL_tr, int LL_ts, int nBest,
			String corpusName, File trainingFile, File testFile,
			boolean mxPos, int order, String MSTver) {

		String testFileName = FileUtil.getFileNameWithoutExtensions(testFile);
		String trainingFileName = FileUtil.getFileNameWithoutExtensions(trainingFile);
		String mstVerOrderDir = "MST_" + MSTver + "_" + order + "order";
		String basePath = Parameters.resultsPath + "Reranker/Parsed/" +
							mstVerOrderDir + "/" + ((mxPos) ? "mxPOS/" : "goldPOS/") +
							corpusName + "/";							
							
		String outputPath = basePath  + corpusName + "_nBest" + nBest + "/";
		
		File outputPathFile = new File(outputPath);
		outputPathFile.mkdirs();
		
		mstparser.DependencyParser.testK = nBest;
		
		ArrayList<TDNode> training = DepCorpus.readTreebankFromFileMST(trainingFile, LL_tr, false, true);
		ArrayList<TDNode> test = DepCorpus.readTreebankFromFileMST(testFile, LL_ts, false, true);		
		
		File outputTraining = new File(outputPath + trainingFileName + ".ulab");
		File outputTestBlind = new File(outputPath + testFileName + ".test.ulab");
		File parsedFile = new File(outputPath + testFileName +
				"_LLtr" + LL_tr + "_LLts" + LL_ts + "_nBest" + nBest);
		
		DepCorpus.toFileMSTulab(outputTraining, training, false);
		DepCorpus.toFileMSTulab(outputTestBlind, test, true);
		
		File depModelFile = new File(basePath + "dep.model");		
		File depModelInfoFile = new File(basePath + "dep.model.info");
		
		String[] argsTrain = {
				"train", 
				"train-file:" + outputTraining,
				"model-name:" + depModelFile,
				"training-iterations:" + 10,
				"decode-type:" + "proj",
				"training-k:" + 1,
				"loss-type:" + "punc",
				"create-forest:" + "true",
				"order:" + order,
				"format:MST"
 		};
		String[] argsTest = {
				"test",				
				"test-file:" + outputTestBlind,
				"model-name:" + depModelFile,
				"output-file:" + parsedFile ,
				"decode-type:" + "proj",
				"order:" + order,
				"format:MST"
		};
		
		String[] args = depModelFile.exists() ? argsTest : Utility.concat(argsTrain, argsTest);
		
		try{
			System.out.println("Running mstparser with args:\n" + Arrays.toString(args));
			FileUtil.appendReturn(Arrays.toString(argsTrain), depModelInfoFile);
			mstparser.DependencyParser.main(args);
		} catch (Exception e) {FileUtil.handleExceptions(e);}
	}
	
	public static void makeNBestConll(int nBest,
			String corpusName, File trainingFile, File testBlindFile,
			boolean mxPos, int order, String MSTver) {
		
		
		String mstVerOrderDir = "MST_" + MSTver + "_" + order + "order";
		String basePath = Parameters.resultsPath + "Reranker/Parsed/" +
							mstVerOrderDir + "/" + ((mxPos) ? "mxPOS/" : "goldPOS/") +
							corpusName + "/";							
							
		String outputPath = basePath  + corpusName + "_nBest" + nBest + "/";
		
		File outputPathFile = new File(outputPath);
		outputPathFile.mkdirs();
		
		mstparser.DependencyParser.testK = nBest;
				
		
		File outputTraining = new File(basePath + trainingFile.getName());
		File outputTestBlind = new File(basePath + testBlindFile.getName());		
		try {
			FileUtils.copyFile(trainingFile, outputTraining);
			FileUtils.copyFile(testBlindFile, outputTestBlind);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		String testFileName = FileUtil.getFileNameWithoutExtensions(testBlindFile);
		File parsedFile = new File(outputPath + testFileName + "_nBest" + nBest);
		
		File depModelFile = new File(basePath + "dep.model");		
		File depModelInfoFile = new File(basePath + "dep.model.info");
		
		String[] argsTrain = {
				"train", 
				"train-file:" + outputTraining,
				"model-name:" + depModelFile,
				"training-iterations:" + 10,
				"decode-type:" + "proj",
				"training-k:" + 1,
				"loss-type:" + "punc",
				"create-forest:" + "true",
				"order:" + order,
				"format:CONLL"
 		};
		String[] argsTest = {
				"test",				
				"test-file:" + outputTestBlind,
				"model-name:" + depModelFile,
				"output-file:" + parsedFile ,
				"decode-type:" + "proj",
				"order:" + order,
				"format:CONLL"
		};
		
		String[] args = depModelFile.exists() ? argsTest : Utility.concat(argsTrain, argsTest);
		
		try{
			System.out.println("Running mstparser with args:\n" + Arrays.toString(args));
			FileUtil.appendReturn(Arrays.toString(argsTrain), depModelInfoFile);
			mstparser.DependencyParser.main(args);
		} catch (Exception e) {FileUtil.handleExceptions(e);}
	}
	
	public static void main(String args[]) {
		long timeStart = System.currentTimeMillis();
		//makeNBestOfShorterLength();
		//makeNBestOfLessN();
		//makeNBestWithGold();
		//mainTutD();
		//mainDevelopTutD();
		//makeNBestOfLessNConnl();
		//mainDevelopTanlD();
		//mainTestTanlD();
		mainWsj();
		long timeEnd = System.currentTimeMillis();
		System.out.println("Took " + (timeEnd-timeStart) + " seconds.");
	}		
	
	public static void mainWsj() {
		int LL_tr = 40;
		int LL_ts = 40;
		int nBest = 1;
		int order = 2;
		String MSTver = "0.5";
		boolean till11_21 = false;
		boolean mxPos = false;
		//"MST", "COLLINS97", "COLLINS99", "COLLINS99Arg", "COLLINS99Ter", "COLLINS99ArgTer"
		int depType = 2; 		
		makeNBestWsj(LL_tr, LL_ts, nBest, till11_21, depType, mxPos, order, MSTver);
	}
	
	public static void mainTutD() {
		//for(int nBest : new int[]{5,10,100,1000}) {
			int nBest = 100;
			int order = 2;			
			String MSTver = "0.5";
			boolean mxPos = false;
			String corpusName = "Tut_Evalita09";
			File trainingFile = new File(TutD.TutTrainSetConnl);
			File testFile = new File(TutD.TutTestSetBlindConnl);
			Reranker.makeNBestConll(nBest, corpusName, 
					trainingFile, testFile, mxPos, order, MSTver);
		//}
	}
	
	public static void mainDevelopTutD() {
		//for(int nBest : new int[]{5,10,100,1000}) {
			int nBest = 100;
			int order = 2;			
			String MSTver = "0.5";
			boolean mxPos = false;
			String corpusName = "Tut_Evalita09Develop2";
			File trainingFile = new File(TutD.TutDevTrainConnl2);
			File testFile = new File(TutD.TutDevTestBlindConnl2);
			Reranker.makeNBestConll(nBest, corpusName, 
					trainingFile, testFile, mxPos, order, MSTver);
		//}
	}
	
	public static void mainDevelopTanlD() {
		//for(int nBest : new int[]{5,10,100,1000}) {
			int nBest = 100;
			int order = 2;			
			String MSTver = "0.5";
			boolean mxPos = false;
			String corpusName = "Tanl_Evalita09Develop";
			File trainingFile = new File(TanlD.TanlD_Train_Connl);
			File testFile = new File(TanlD.TanlD_Dev_Connl);
			Reranker.makeNBestConll(nBest, corpusName, 
					trainingFile, testFile, mxPos, order, MSTver);
		//}
	}
	
	public static void mainTestTanlD() {
		//for(int nBest : new int[]{5,10,100,1000}) {
			int nBest = 100;
			int order = 2;			
			String MSTver = "0.5";
			boolean mxPos = false;
			String corpusName = "Tanl_Evalita09Test";
			File trainingFile = new File(TanlD.TanlD_Train_Connl);
			File testFile = new File(TanlD.TanlD_Test_Connl);
			Reranker.makeNBestConll(nBest, corpusName, 
					trainingFile, testFile, mxPos, order, MSTver);
		//}
	}
	
	public static void selectRerankedFromFileUlab(File parsedFile,
			File rerankedFile, ArrayList<Integer> rerankedIndexes) {
		Scanner scanner = FileUtil.getScanner(parsedFile);
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFile);
		ListIterator<Integer> iter = rerankedIndexes.listIterator();
		int counter = 0;		
		int targetIndex = iter.next();
		int structureLine=0;
		while(scanner.hasNextLine()) {			
			String line = scanner.nextLine();					
			if (line.length()>0) {
				if (counter==targetIndex) pw.println(line);
				if (structureLine==2) {
					if (counter==targetIndex) targetIndex = -1;
					structureLine=0;
					counter++;
				}
				else structureLine++;				
			}
			else {
				counter = 0;										
				targetIndex = iter.hasNext()? iter.next() : -1;
				pw.println();
			}
		}
		pw.println();
		pw.close();
	}
	
}
