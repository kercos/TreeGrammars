package tesniere;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import backoff.BackoffModel;
import backoff.BackoffModel_DivHistory;
import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.ProbModelStructureFiller.BoxBoundaryEvent;
import tsg.HeadLabeler;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.Utility;
import util.file.FileUtil;

public class ProbModelTagChunker extends ProbModel {		
	
	BackoffModel posTaggingModel;
	BackoffModel chunkTaggingModel;
	int skippedSentences;
		
	public ProbModelTagChunker(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);
		skippedSentences = 0;
	}	
	
	public ProbModelTagChunker() {		
		super();
		skippedSentences = 0;
	}
	
	public void reportModelsTables(PrintStream out) {
		out.println("Pos Tagging Model:");
		posTaggingModel.printTables(out);
		out.println("---------------------\n\n");
		out.println("Chunk Tagging Model:");
		chunkTaggingModel.printTables(out);
		out.println("---------------------\n\n");
	}
			
	private void exractPostaggingAndChunkingEvents(Word[] wordsArray,
			IdentityHashMap<Word, Box> wordBoxMapping, int sentenceLength,
			ArrayList<Event> events) {
								
		String[] chunking = Chunker.wordChunkLabeler(wordsArray, wordBoxMapping);
		if (!Chunker.isConsistentChunking(chunking)) {
			skippedSentences++;
			return;
		}
		
		int index=0;
		for(int i=-2; i<sentenceLength; i++) {			
			Word[] threeGrams = new Word[3];
			Word nextWord = null;
			for(int j=0; j<3; j++) {
				index = i+j;				
				threeGrams[j] = (index<0 || index>=sentenceLength) ? null : wordsArray[index];				
			}			
			PosTaggingEvent pte = new PosTaggingEvent(threeGrams); 
			events.add(pte);
			if (index>=sentenceLength) continue;
			nextWord = (index+1>=sentenceLength) ? null : wordsArray[index+1];
			Word word = wordsArray[index];
			Box wordBox = wordBoxMapping.get(word);
			String chunkTag = chunking[index];
			TreeSet<Word> previousWordsInChunk = getPreviousEmptyWordsInChunk(word, wordBox, chunking, 2); 
			events.add(new ChunkTaggingEvent(threeGrams, nextWord, chunkTag, previousWordsInChunk));	
		}
	}	
	
	public static TreeSet<Word> getPreviousEmptyWordsInChunk(Word word, Box wordBox, String[] chunking, int goBack) {
		TreeSet<Word> previousWordsInChunk = new TreeSet<Word>();
		String wordChunkTag = chunking[word.position]; 
		if (wordChunkTag.equals("O") || wordChunkTag.equals("C")) return previousWordsInChunk;
		int wordPosLimit = word.position - goBack;
		if (wordBox.ewList!=null) {
			for(Word ew : wordBox.ewList) {
				if (ew.position<wordPosLimit) previousWordsInChunk.add(ew);
			}
		}
		Box parent = wordBox.parent;
		while (parent!=null && parent.isJunctionBlock()) {
			BoxJunction parentJun = (BoxJunction) parent;			
			if (parentJun.conjuncts==null || parentJun.conjuncts.first()!=wordBox) break;
			if (parent.ewList!=null) {
				for(Word ew : parent.ewList) {
					int ewPos = ew.position;
					String ewChunkTag = chunking[ewPos];
					if (ewPos<wordPosLimit && !ewChunkTag.equals("O") && !ewChunkTag.equals("C")) {
						previousWordsInChunk.add(ew);
					}
				}
			}
			wordBox = parent;
			parent = parent.parent;
		}
		return previousWordsInChunk;
	}
	
	
	protected class PosTaggingEvent  extends Event {
			
			private static final long serialVersionUID = 0L;
			
			Word[] threeGrams;
			
			public PosTaggingEvent(Word[] threeGrams) {
				this.threeGrams = threeGrams;
			}
		
			@Override
			public String encodingToStringFreq(int decompNumb, int backoffLevel) {
				Symbol[][] thisEncoding = encodePosTaggingEvent(this);
				return "Pos Tagging\t" + 
				posTaggingModel.getEventFreqToString(thisEncoding, decompNumb, backoffLevel)
				+ "\t" + getProb();
			}
			
			public String toString() {
				Symbol[][] thisEncoding = encodePosTaggingEvent(this);
				return "Pos Tagging\t" + posTaggingModel.getEventFreqToString(thisEncoding, 0, 0) + 
				"\tWord Generation\t" + posTaggingModel.getEventFreqToString(thisEncoding, 1, 0)
				+ "\t" + getProb();
			}
		
			@Override
			public boolean equals(Object otherObject) {
				if (otherObject==this) return true;
				if (otherObject instanceof BoxBoundaryEvent) {
					PosTaggingEvent otherPosTaggingEvent = (PosTaggingEvent)otherObject;
					Symbol[][] thisEncoding = encodePosTaggingEvent(this);
					Symbol[][] otherEncoding = encodePosTaggingEvent(otherPosTaggingEvent);								
					return Arrays.deepEquals(thisEncoding, otherEncoding);
				}
				return false;
			}
		
			@Override
			public int[] getCondFreqEventFreq(int decompNumb, int backoffLevel) {
				Symbol[][] thisEncoding = encodePosTaggingEvent(this);
				return posTaggingModel.getCondFreqEventFreq(thisEncoding, decompNumb, backoffLevel);
			}
		
			@Override
			public double getProb() {
				Symbol[][] eventEncoding = encodePosTaggingEvent(this);
				return posTaggingModel.getCondProb(eventEncoding);
			}
		
			@Override
			public void storeEvent() {
				Symbol[][] eventEncoding = encodePosTaggingEvent(this);
				posTaggingModel.increaseInTables(eventEncoding);			
			}
			
		}
	
	protected class ChunkTaggingEvent  extends Event {
		
		private static final long serialVersionUID = 0L;
		
		Word[] threeGrams;
		String chunkTag;
		TreeSet<Word> previousWordsInChunk;
		Word nextWord;
		
		public ChunkTaggingEvent(Word[] threeGrams, Word nextWord, String chunkTag,
				TreeSet<Word> previousWordsInChunk) {
			this.threeGrams = threeGrams;
			this.nextWord = nextWord;
			this.chunkTag = chunkTag;
			this.previousWordsInChunk = previousWordsInChunk;
		}
	
	
		@Override
		public String encodingToStringFreq(int decompNumb, int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeChunkTaggingEvent(this);
			return "\tExpansion\t" + 
			chunkTaggingModel.getEventFreqToString(thisEncodingLevel, decompNumb, backoffLevel)
			+ "\t" + getProb();
		}
		
		public String toString() {
			Symbol[][] thisEncoding = encodeChunkTaggingEvent(this);
			return "Chunk Tagging\t" + 
			chunkTaggingModel.getEventFreqToString(thisEncoding, 0, 0) + "\t" + getProb();
		}

	
		@Override
		public boolean equals(Object otherObject) {
			if (otherObject==this) return true;
			if (otherObject instanceof BoxBoundaryEvent) {
				ChunkTaggingEvent otherChunkTaggingEvent = (ChunkTaggingEvent)otherObject;
				Symbol[][] thisEncoding = encodeChunkTaggingEvent(this);
				Symbol[][] otherEncoding = encodeChunkTaggingEvent(otherChunkTaggingEvent);								
				return Arrays.deepEquals(thisEncoding, otherEncoding);
			}
			return false;
		}
	
		@Override
		public int[] getCondFreqEventFreq(int decompNumb, int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeChunkTaggingEvent(this);
			return chunkTaggingModel.getCondFreqEventFreq(thisEncodingLevel, decompNumb, backoffLevel);
		}
	
		@Override
		public double getProb() {
			Symbol[][] eventEncoding = encodeChunkTaggingEvent(this);
			return chunkTaggingModel.getCondProb(eventEncoding);
		}
	
		@Override
		public void storeEvent() {
			Symbol[][] eventEncoding = encodeChunkTaggingEvent(this);
			chunkTaggingModel.increaseInTables(eventEncoding);			
		}
		
	}
	
	
	@Override
	public void extractEventsFromStructure(Box structure,
			ArrayList<Event> trainingEvents) {
		
		int sentenceLength = structure.countAllWords();
		Word[] wordsArray = new Word[sentenceLength];
		IdentityHashMap<Word, Box> wordBoxTable = new IdentityHashMap<Word, Box>(); 
		
		structure.fillWordArrayTable(wordsArray, wordBoxTable);		
		exractPostaggingAndChunkingEvents(wordsArray, wordBoxTable, sentenceLength, trainingEvents);
	}
	
	public static SymbolString nullEvent = new SymbolString("#NULL#");
	
	/**
	 * Encoding a pos tagging event P(t_{1,n}|w_{1,n}) = \prod P(w_i|t_i) * P(t_{i}|t_{i-1},t_{i-2})
	 * event: t_{i} w_{i} | t_{i-1} w_{i-1} t_{i-2}
	 * @param e
	 * @return
	 */
	public Symbol[][] encodePosTaggingEvent(PosTaggingEvent e) {
		
		Word currentWord = e.threeGrams[2];
		Word previousWord = e.threeGrams[1];
		Word previousPreviousWord = e.threeGrams[0];
		
		Symbol cL = currentWord==null ? nullEvent : new SymbolString(currentWord.getLex());
		Symbol cP = currentWord==null ? nullEvent : new SymbolString(currentWord.getPos());
		Symbol pP = previousWord==null ? nullEvent : new SymbolString(previousWord.getPos());		
		Symbol ppP = previousPreviousWord==null ? nullEvent : new SymbolString(previousPreviousWord.getPos());
		
		return new Symbol[][]{
				{cL, cP, pP, ppP}
		};		
	}
	
	public void initPosTaggingModel() {
		int[][][] posTaggingBOL = new int[][][]{
				{{   0, 0, 0}},
				{{0, 0, -1, -1}}	
		};
		boolean[] posTaggingSkip = new boolean[]{false, false};
		int[][] posTaggingGroup = new int[][]{{1},{1}};
		posTaggingModel = new BackoffModel_DivHistory(posTaggingBOL, posTaggingSkip, posTaggingGroup,
				0, 5, 0.000001);
	}
	
	/**
	 * Encoding the tag of each word specifying its chunk role.
	 * P(chunkTag | t_{i}, t_{i-1}, t_{i-2}, previousPosInChunk)
	 * @param e
	 * @return
	 */
	public Symbol[][] encodeChunkTaggingEvent(ChunkTaggingEvent e) {
		Word currentWord = e.threeGrams[2];
		Word previousWord = e.threeGrams[1];
		Word previousPreviousWord = e.threeGrams[0];
		 
		Symbol chunkTag = new SymbolString(e.chunkTag);		
		Symbol cP = currentWord==null ? nullEvent : new SymbolString(currentWord.getPos());
		Symbol pP = previousWord==null ? nullEvent : new SymbolString(previousWord.getPos());		
		Symbol ppP = previousPreviousWord==null ? nullEvent : new SymbolString(previousPreviousWord.getPos());
		Symbol nP = e.nextWord==null ? nullEvent : new SymbolString(e.nextWord.getPos());
		
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		for(Word w : e.previousWordsInChunk) {
			vectorEncoding.add(new SymbolString(w.getPos()));
		}
		Symbol previousPosInChunk = vectorEncoding.isEmpty() ? nullEvent : new SymbolList(vectorEncoding);
		
		return new Symbol[][]{{chunkTag, cP, pP, ppP, nP, previousPosInChunk}};
	}
	
	public void initChunkTaggingModel() {
		int[][][] chunkTaggingBOL = new int[][][]{
				{{0, 0, 0, 0, 0, 0}}
		};
		boolean[] chunkTaggingSkip = new boolean[]{false};
		int[][] chunkTaggingGroup = new int[][]{{1}};
		chunkTaggingModel = new BackoffModel_DivHistory(chunkTaggingBOL, chunkTaggingSkip, chunkTaggingGroup,
				0, 5, 0.000001);
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
	
	public static void smallTest() throws Exception {
		File treebankTrainFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		File treebankTestFile = new File(Wsj.WsjOriginal + "wsj-00.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_12_04_10"));
		ArrayList<Box> treebankTrain = Conversion.getTesniereTreebank(treebankTrainFile, HL);
		ProbModelTagChunker tagChunkerModel = new ProbModelTagChunker(treebankTrain);
		tagChunkerModel.initPosTaggingModel();
		tagChunkerModel.initChunkTaggingModel();		
		tagChunkerModel.train();
		//tagChunkerModel.reportModelsTables();
		
		ArrayList<Box> treebankTest = Conversion.getTesniereTreebank(treebankTestFile, HL);
		int length = treebankTest.size();
		int random = (int)(Math.random()*length);
		System.out.println("Sentence #: " + (random+1));
		Box b = treebankTest.get(random);
		ArrayList<Event> eventList = tagChunkerModel.getEventsList(b);
		System.out.println("----------------");
		for(Event e : eventList) {
			System.out.println(e.toString());
		}
	}
	
	public static void printTables() throws Exception {
		File treebankTrainFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_12_04_10"));		
		ArrayList<Box> treebankTrain = Conversion.getTesniereTreebank(treebankTrainFile, HL);
		
		ProbModelTagChunker tagChunkerModel = new ProbModelTagChunker(treebankTrain);
		tagChunkerModel.initPosTaggingModel();
		tagChunkerModel.initChunkTaggingModel();		
		tagChunkerModel.train();
		
		File outputFile = new File("/scratch/fsangati/RESULTS/TDS/Reranker/sec22_C/rerank_tagPosChunks/eventTable.txt");
		PrintStream ps = new PrintStream(outputFile);
		tagChunkerModel.reportModelsTables(ps);
		ps.close();
	}
	
	public static void rerank(int nBest) throws Exception {
		File treebankTrainFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_12_04_10"));		
		ArrayList<Box> treebankTrain = Conversion.getTesniereTreebank(treebankTrainFile, HL);
		
		ProbModelTagChunker tagChunkerModel = new ProbModelTagChunker(treebankTrain);
		tagChunkerModel.initPosTaggingModel();
		tagChunkerModel.initChunkTaggingModel();		
		tagChunkerModel.train();
		
		//File eventTablesFile = new File(Parameters.resultsPath + "TDS/Reranker/sec22_C/rerank_tagPosChunks/eventTable.txt");
		//PrintStream ps = new PrintStream(eventTablesFile);
		//tagChunkerModel.reportModelsTables(ps);
		//ps.close();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String outputDIr = baseDir + "rerank_tagPosChunks/";
		
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");		
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");		
		File resultFile = new File(baseDir + "wsj-22_reranked_results.txt");
		File rerankedFile = new File(outputDIr + "wsj-22_reranked_" + nBest + "best.mrg");
		File oneBestFile = new File(outputDIr + "wsj-22_oneBest.mrg");
		PrintWriter pwReranked = FileUtil.getPrintWriter(rerankedFile);
		PrintWriter pwOneBest = FileUtil.getPrintWriter(oneBestFile);
		PrintWriter pwResults = FileUtil.getPrintWriter(resultFile);
		
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();
		
		int size = goldTreebank.size();
		int[] oneBestScoreTotal = new int[2];
		int[] rerankedScoreTotal = new int[2];
		int inconsistentTaggedGold = 0;
		int inconsistentTaggedOneBest = 0;
		int inconsistentTaggedReranked = 0;
		int activelyReranked = 0;
		
		for(int i=0; i<size; i++) {			
			ArrayList<TSNodeLabel> nBestTrees = nextNBest(nBest, nBestScanner);
			TSNodeLabel goldTree = goldIter.next();
			Box gold = Conversion.getTesniereStructure(goldTree.clone(), HL);
			
			String[] goldChunkTags = Chunker.wordChunkLabeler(gold);
			if (!Chunker.isConsistentChunking(goldChunkTags)) inconsistentTaggedGold++;			
			
			boolean reranked = false;
			boolean first = true;
			double maxProb = -1;
			String[] bestRerankedTags = null;
			String[] oneBestRerankedTags = null;
			int[] oneBestScore = new int[2];
			int[] rerankedScore = new int[2];
			TSNodeLabel rerankedTree = null;
			
			for(TSNodeLabel t : nBestTrees) {
				Box b = Conversion.getTesniereStructure(t.clone(), HL);
				String[] chunkTags = Chunker.wordChunkLabeler(b);
				boolean isConsistent = Chunker.isConsistentChunking(chunkTags);				
				double p = tagChunkerModel.getProb(b);
				int[] scores = Chunker.getChunkTaggingScores(goldChunkTags, chunkTags);
				if (first) {					
					first = false;
					if (!isConsistent) inconsistentTaggedOneBest++;
					rerankedTree = t;
					maxProb = p;
					oneBestRerankedTags = bestRerankedTags = chunkTags;
					pwOneBest.println(t.toString());					
					oneBestScore = scores;
					Utility.arrayIntPlus(oneBestScore, oneBestScoreTotal);
					rerankedScore = scores;
					continue;
				}												
				if (p>maxProb) {					
					maxProb = p;					
					bestRerankedTags = chunkTags;
					rerankedScore = scores;
					rerankedTree = t;
					reranked = true;
				}
			}
			
			pwReranked.println(rerankedTree.toString());
			if (reranked) activelyReranked++;
			if (!Chunker.isConsistentChunking(bestRerankedTags)) inconsistentTaggedReranked++;
			Utility.arrayIntPlus(rerankedScore, rerankedScoreTotal);
			
			int diff = rerankedScore[0] - oneBestScore[0];
			String plus = diff<=0 ? "" : "+"; 
			reportLine((i+1) + "\t" + rerankedScore[1]  + "\t" + 
					oneBestScore[0] + " " + rerankedScore[0] + " " + " (" + plus + diff + ")", pwResults);
			//System.out.println("\t" + Arrays.toString(oneBestRerankedTags));
			//System.out.println("\t" + Arrays.toString(bestRerankedTags));
		}
		
		float scoreOneBest = (float) oneBestScoreTotal[0] / oneBestScoreTotal[1];
		float scoreReranked = (float) rerankedScoreTotal[0] / rerankedScoreTotal[1];
		reportLine("One Best scores: " + Arrays.toString(oneBestScoreTotal) + "\t" + scoreOneBest, pwResults);
		reportLine("Reranked scores: " + Arrays.toString(rerankedScoreTotal) + "\t" + scoreReranked, pwResults);
		reportLine("Total reranked: " + activelyReranked, pwResults);
		reportLine("Inconsistently tagged Gold: " + inconsistentTaggedGold, pwResults);
		reportLine("Inconsistently tagged One Best: " + inconsistentTaggedOneBest, pwResults);
		reportLine("Inconsistently tagged Reranked: " + inconsistentTaggedReranked, pwResults);
		pwReranked.close();
		pwOneBest.close();
	}
	
	public static void reportLine(String line, PrintWriter pw) {
		System.out.println(line);
		pw.println(line);
	}

	
	public static void main(String[] args) throws Exception {
		//printTables();
		rerank(1000);		
	}
	
	

}
