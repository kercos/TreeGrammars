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
import backoff.BackoffModel_Eisner;
import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.ProbModel.Event;
import tesniere.ProbModelStructureFiller.BoxBoundaryEvent;
import tesniere.ProbModelTagChunker_smp.ChunkTaggingEvent;
import tesniere.ProbModelTagChunker_smp.PosTaggingEvent;
import tsg.HeadLabeler;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;

public class ProbModelTagChunker_adv extends ProbModel {		
	
	BackoffModel posTaggingModel;
	BackoffModel chunkTaggingModel;
	int skippedSentences;
		
	public ProbModelTagChunker_adv(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);
		skippedSentences = 0;
	}	
	
	public ProbModelTagChunker_adv() {		
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
			
	private void exractPostaggingAndChunkingEvents(Box structure, ArrayList<Event> events) {
		
		int sentenceLength = structure.countAllWords();
		Word[] wordsArray = new Word[sentenceLength];
		IdentityHashMap<Word, Box> wordBoxTable = new IdentityHashMap<Word, Box>(); 		
		structure.fillWordArrayTable(wordsArray, wordBoxTable);
		
		SymbolString[] chunking = Chunker.wordChunkLabeler(structure);
		/*if (!Chunker.isConsistentChunking(chunking)) {
			skippedSentences++;
			return;
		}*/
		
		int index=0;
		for(int i=-2; i<sentenceLength; i++) {			
			Word[] threeGrams = new Word[3];
			//Word nextWord = null;
			for(int j=0; j<3; j++) {
				index = i+j;				
				threeGrams[j] = (index<0 || index>=sentenceLength) ? null : wordsArray[index];
			}			
			PosTaggingEvent pte = new PosTaggingEvent(threeGrams); 
			events.add(pte);
			if (index>=sentenceLength) continue;
			Word nextWord = (index+1>=sentenceLength) ? null : wordsArray[index+1];					
			SymbolString chunkTag = chunking[index];
			Word word = wordsArray[index];
			
			TreeSet<Word> previousWordsInChunk = getPreviousEmptyWordsInChunk(word, wordsArray, chunking, 0);
			/*Box wordBox = wordBoxTable.get(word);
			TreeSet<Word> previousWordsInChunkOld = getPreviousEmptyWordsInChunk(word, wordBox, chunking, 0); 
			if (!previousWordsInChunk.equals(previousWordsInChunkOld)) {
				System.err.println("differ");
			}*/
			
			events.add(new ChunkTaggingEvent(threeGrams, nextWord, chunkTag, previousWordsInChunk));	
		}
	}	
	
	public static TreeSet<Word> getPreviousEmptyWordsInChunk(Word word, Word[] wordsArray, SymbolString[] chunking, int goBack) {
		TreeSet<Word> previousWordsInChunk = new TreeSet<Word>();
		SymbolString wordChunkTag = chunking[word.position];
		boolean isI = wordChunkTag == Chunker.I;
		boolean isIplus = wordChunkTag == Chunker.Ip;
		if (!isI && !isIplus) return previousWordsInChunk;
		int level = isI ? 0 : -1;
		int wordPosLimit = word.position - goBack;
		for(int i=word.position-1; i>=0; i--) {
			SymbolString previousWordChunking = chunking[i];
			if (previousWordChunking == Chunker.Ip) {
				if (level==0 && i<wordPosLimit) {
					Word previousWord = wordsArray[i];
					if (previousWord.isEmpty()) previousWordsInChunk.add(wordsArray[i]);					
				}
				level--;
				continue;
			}
			if (previousWordChunking == Chunker.I) {
				if (level==0 && i<wordPosLimit) {
					Word previousWord = wordsArray[i];
					if (previousWord.isEmpty()) previousWordsInChunk.add(wordsArray[i]);
				}
				continue;
			}
			if (previousWordChunking == Chunker.Nm ) {
				if (level==0) {
					if (i<wordPosLimit) {
						Word previousWord = wordsArray[i];
						if (previousWord.isEmpty()) previousWordsInChunk.add(wordsArray[i]);
					}
					break;
				}
				level++;
				continue;
			}
			if (previousWordChunking == Chunker.N) {
				if (level==0) {
					if (i<wordPosLimit) {
						Word previousWord = wordsArray[i];
						if (previousWord.isEmpty()) previousWordsInChunk.add(wordsArray[i]);
					}
					break;
				}				
			}
		}	
		return previousWordsInChunk;
	}	
	
	/*public static TreeSet<Word> getPreviousEmptyWordsInChunk(Word word, Box wordBox, String[] chunking, int goBack) {
	TreeSet<Word> previousWordsInChunk = new TreeSet<Word>();
	String wordChunkTag = chunking[word.position]; 		
	if (wordBox.isJunctionBlock() || (!wordChunkTag.equals("I") && !wordChunkTag.equals("+I"))) {
		return previousWordsInChunk;
	}
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
}*/

	
	
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
				if (otherObject instanceof PosTaggingEvent) {
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
		SymbolString chunkTag;
		TreeSet<Word> previousWordsInChunk;
		Word nextWord;
		
		public ChunkTaggingEvent(Word[] threeGrams, Word nextWord, SymbolString chunkTag,
				TreeSet<Word> previousWordsInChunk) {
			this.threeGrams = threeGrams;
			this.chunkTag = chunkTag;
			this.nextWord = nextWord;
			this.previousWordsInChunk = previousWordsInChunk;
		}
	
	
		@Override
		public String encodingToStringFreq(int decompNumb, int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeChunkTaggingEvent(this);
			return "Chunk Tagging\t" + 
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
			if (otherObject instanceof ChunkTaggingEvent) {
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
	public void extractEventsFromStructure(Box structure,ArrayList<Event> trainingEvents) {
		
		exractPostaggingAndChunkingEvents(structure, trainingEvents);
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
		
		Symbol cL = wordLex(currentWord);
		Symbol cP = wordPos(currentWord);
		Symbol pL = wordLex(previousWord);
		Symbol pP = wordPos(previousWord);		
		Symbol ppP = wordPos(previousPreviousWord);
		Symbol ppL = wordLex(previousPreviousWord);
		
		return new Symbol[][]{
				{cL, cP, pL, pP, ppL, ppP}
		};		
	}
	
	public void initPosTaggingModel() {
		int[][][] posTaggingBOL = new int[][][]{
				{
					{   0,  0,  0,  0,  0}, 
					{   0,  0,  0,  -1,  0},
					{   0, -1,  0,  -1,  0}
				},
				{   {0, 0,  0,  0,  0,  0},
					{0, 0, -1,  0, -1,  0},
					{0, 0, -1, -1, -1, -1}
				}	
		};
		boolean[] posTaggingSkip = new boolean[]{false, false};
		int[][] posTaggingGroup = new int[][]{{1,1,1},{1,1,1}};
		posTaggingModel = new BackoffModel_DivHistory(posTaggingBOL, posTaggingSkip, posTaggingGroup,
				0, 5, 0.000001);
	}
	
	public void initPosTaggingModelEisner() {
		int[][][] posTaggingBOL = new int[][][]{
				{
					{   0,  0,  0,  0,  0}, 
					{   0,  0,  0,  -1,  0},
					{   0, -1,  0,  -1,  0}
				},
				{   {0, 0,  0,  0,  0,  0},
					{0, 0, -1,  0, -1,  0},
					{0, 0, -1, -1, -1, -1}
				}	
		};
		boolean[] posTaggingSkip = new boolean[]{false, false};
		int[][] posTaggingGroup = new int[][]{{1,1,1},{1,1,1}};
		posTaggingModel = new BackoffModel_Eisner(posTaggingBOL, posTaggingSkip, posTaggingGroup,
				0.005, 0.5, 3);
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
		 
		Symbol chunkTag = e.chunkTag;		
		Symbol cP = wordPos(currentWord);
		Symbol cL = wordLex(currentWord);
		Symbol pP = wordPos(previousWord);		
		Symbol ppP = wordPos(previousPreviousWord);
		Symbol nP = wordPos(e.nextWord);
		
		
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		for(Word w : e.previousWordsInChunk) {
			vectorEncoding.add(wordPos(w));
		}
		Symbol previousPosInChunk = vectorEncoding.isEmpty() ? nullEvent : new SymbolList(vectorEncoding);
		
		return new Symbol[][]{{chunkTag, cL, cP, pP, ppP, nP, previousPosInChunk}};
	}
	
	public void initChunkTaggingModelDivHistory() {
		int[][][] chunkTaggingBOL = new int[][][]{
				{
					{0, 0, 0, 0, 0, 0, 0},
					{0, 0, 0, 0, 0, 0, -1},
					{0, -1, 0, 0, 0, 0, -1},
					{0, -1, 0, 0, -1, 0, -1},
					{0, -1, 0, 0, -1, -1, -1}
				}
		};
		boolean[] chunkTaggingSkip = new boolean[]{false};
		int[][] chunkTaggingGroup = new int[][]{{1,1,1,1,1}};
		chunkTaggingModel = new BackoffModel_DivHistory(chunkTaggingBOL, chunkTaggingSkip, chunkTaggingGroup,
				0, 5, 0.000001);
	}
	
	public void initChunkTaggingModelEisner() {
		int[][][] chunkTaggingBOL = new int[][][]{
				{
					{0, 0, 0, 0, 0, 0, 0},
					{0, 0, 0, 0, 0, 0, -1},
					{0, -1, 0, 0, 0, 0, -1},
					{0, -1, 0, 0, -1, 0, -1},
					{0, -1, 0, 0, -1, -1, -1}
				}
		};
		boolean[] chunkTaggingSkip = new boolean[]{false};
		int[][] chunkTaggingGroup = new int[][]{{1,1,1,1,1}};
		chunkTaggingModel = new BackoffModel_Eisner(chunkTaggingBOL, chunkTaggingSkip, chunkTaggingGroup, 0.005, 0.5, 3);
	}
	
	public static Symbol wordPos(Word w) {
		return w==null ? nullEvent : new SymbolString(w.getPos());
	}
	
	public static Symbol wordLex(Word w) {
		return w==null ? nullEvent : new SymbolString(w.getLex());
	}
	
	public static Symbol wordPosLexIfEmpty(Word w) {
		if (w==null) return nullEvent;
		if (w.isContentWord()) return new SymbolString(w.getPos());
		return new SymbolString(w.getLex() + "_" + w.getPos());
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
	
	public static ArrayList<TSNodeLabel> getNBest(int nBest, int sentenceNumber, File f) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(nBest);
		Scanner scan = FileUtil.getScanner(f);		
		int i = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) i++;
			if (i==sentenceNumber) {
				int count = 0;
				while(scan.hasNextLine() && count<nBest) {
					line = scan.nextLine();
					if (line.equals("")) break;
					result.add(new TSNodeLabel(line));
					count++;
				}
				break;
			}			
		}
		return result;
	}
	
	public static void smallTest() throws Exception {
		File treebankTrainFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		File treebankTestFile = new File(Wsj.WsjOriginal + "wsj-00.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_12_04_10"));
		ArrayList<Box> treebankTrain = Conversion.getTesniereTreebank(treebankTrainFile, HL);
		ProbModelTagChunker_adv tagChunkerModel = new ProbModelTagChunker_adv(treebankTrain);
		tagChunkerModel.initPosTaggingModel();
		tagChunkerModel.initChunkTaggingModelDivHistory();		
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
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_30_06_10"));		
		ArrayList<Box> treebankTrain = Conversion.getTesniereTreebank(treebankTrainFile, HL);
		
		ProbModelTagChunker_adv tagChunkerModel = new ProbModelTagChunker_adv(treebankTrain);
		tagChunkerModel.initPosTaggingModel();
		tagChunkerModel.initChunkTaggingModelDivHistory();		
		tagChunkerModel.train();
		
		File outputFile = new File("/scratch/fsangati/RESULTS/TDS/Reranker/sec22_C/rerank_tagPosChunks/eventTable.txt");
		PrintStream ps = new PrintStream(outputFile);
		tagChunkerModel.reportModelsTables(ps);
		ps.close();
	}
	
	public static void rerankDebug(int nBest, int sentenceNumber) throws Exception {
		File treebankTrainFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_12_04_10"));		
		ArrayList<Box> treebankTrain = Conversion.getTesniereTreebank(treebankTrainFile, HL);
		
		ProbModelTagChunker_adv tagChunkerModel = new ProbModelTagChunker_adv(treebankTrain);
		tagChunkerModel.initPosTaggingModel();
		tagChunkerModel.initChunkTaggingModelDivHistory();		
		tagChunkerModel.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";		
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");		
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");	
		 
		ArrayList<TSNodeLabel> nBestStructures = getNBest(nBest, sentenceNumber, nBestFile);
		TSNodeLabel firstBestPS = nBestStructures.get(0);
		TSNodeLabel goldPS = TSNodeLabel.getTreebank(goldFile).get(sentenceNumber);
		Box goldTDS = Conversion.getTesniereStructure(goldPS.clone(), HL);
		Box firstBestTDS = Conversion.getTesniereStructure(firstBestPS.clone(), HL);
		SymbolString[] goldChunkTags = Chunker.wordChunkLabeler(goldTDS);
		SymbolString[] firstBestChunkTags = Chunker.wordChunkLabeler(firstBestTDS);
		int[] firstBestScores = Chunker.getChunkTaggingScores(goldChunkTags, firstBestChunkTags);
		Box bestRerankedTDS = null;
		SymbolString[] bestRerankedChunkTags = null;
		int[] bestRerankedScores = null;
				
		double maxProb = -1;		
		for(TSNodeLabel iBestStructure : nBestStructures) {						
			Box b = Conversion.getTesniereStructure(iBestStructure.clone(), HL);
			SymbolString[] chunkTags = Chunker.wordChunkLabeler(b);				
			double p = tagChunkerModel.getProb(b);
			int[] scores = Chunker.getChunkTaggingScores(goldChunkTags, chunkTags);									
			if (p>maxProb) {					
				maxProb = p;	
				bestRerankedTDS = b;
				bestRerankedChunkTags = chunkTags;
				bestRerankedScores = scores;
			}
		}
		
		int diff = bestRerankedScores[0] - firstBestScores[0];
		System.out.println("First Best Chunking" + "\n\t" + Arrays.toString(firstBestChunkTags));
		System.out.println("Reranked Chunking" + "\n\t" + Arrays.toString(bestRerankedChunkTags));
		System.out.println("Diff score: " + (diff<=0 ? "" : "+")  + diff);
		
		ArrayList<Event> bestRerankedEvents = tagChunkerModel.getEventsList(bestRerankedTDS);
		ArrayList<Event> bestRerankedEventsUnique = new ArrayList<Event>(bestRerankedEvents);
		
		ArrayList<Event> firstBestEvents = tagChunkerModel.getEventsList(firstBestTDS);
		ArrayList<Event> firstBestEventsUnique = new ArrayList<Event>(firstBestEvents);
		
		Utility.removeAllOnce(bestRerankedEventsUnique, firstBestEvents);
		Utility.removeAllOnce(firstBestEventsUnique, bestRerankedEvents);
		
		System.out.println("First Best events:");
		for(Event e : firstBestEventsUnique) {
			System.out.println(e.encodingToStringFreq(0,0));
		}
		System.out.println("Reranked events:");
		for(Event e : bestRerankedEventsUnique) {
			System.out.println(e.encodingToStringFreq(0,0));
		}
	}
	
	public static void rerank(int nBest) throws Exception {
		File treebankTrainFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_09_06_10"));		
		ArrayList<Box> treebankTrain = Conversion.getTesniereTreebank(treebankTrainFile, HL);
		
		ProbModelTagChunker_adv tagChunkerModel = new ProbModelTagChunker_adv(treebankTrain);
		tagChunkerModel.initPosTaggingModel();
		tagChunkerModel.initChunkTaggingModelDivHistory();		
		tagChunkerModel.train();
		
		File eventTablesFile = new File(Parameters.resultsPath + "TDS/Reranker/sec22_C/rerank_tagPosChunks/eventTable.txt");
		PrintStream ps = new PrintStream(eventTablesFile);
		tagChunkerModel.reportModelsTables(ps);
		ps.close();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String outputDIr = baseDir + "rerank_tagPosChunks/";
		
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");		
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");		
		File resultFile = new File(outputDIr + "wsj-22_reranked_" + nBest + "results.txt");
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
			
			SymbolString[] goldChunkTags = Chunker.wordChunkLabeler(gold);
			ArrayList<Label> goldPosTags = goldTree.collectPreLexicalLabels();
			if (!Chunker.isConsistentChunking(goldChunkTags)) inconsistentTaggedGold++;			
			
			boolean reranked = false;
			boolean first = true;
			double maxProb = -1;
			SymbolString[] bestRerankedTags = null;			
			SymbolString[] oneBestRerankedTags = null;			
			int[] oneBestScore = new int[2];
			int[] rerankedScore = new int[2];
			TSNodeLabel rerankedTree = null;
			
			for(TSNodeLabel t : nBestTrees) {
				Box b = Conversion.getTesniereStructure(t.clone(), HL);
				SymbolString[] chunkTags = Chunker.wordChunkLabeler(b);
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
			
			ArrayList<Label> rerankedPosTags =  rerankedTree.collectPreLexicalLabels();
			pwReranked.println(rerankedTree.toString());
			if (reranked) activelyReranked++;
			if (!Chunker.isConsistentChunking(bestRerankedTags)) inconsistentTaggedReranked++;
			Utility.arrayIntPlus(rerankedScore, rerankedScoreTotal);
			
			int posTagsWrong = rerankedScore[1] - Utility.match(goldPosTags, rerankedPosTags);
			int diff = rerankedScore[0] - oneBestScore[0];
			String plus = diff<=0 ? "" : "+"; 
			reportLine((i+1) + "\t" + rerankedScore[1]  + "\t" + 
					oneBestScore[0] + " " + rerankedScore[0] + " " + " (" + plus + diff + ")" + "\t" +
					posTagsWrong, pwResults);
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
		pwResults.close();
	}
	
	public static void reportLine(String line, PrintWriter pw) {
		System.out.println(line);
		pw.println(line);
	}

	
	public static void main(String[] args) throws Exception {
		printTables();
		//rerank(1);
		//rerankDebug(100,30);
	}
	
	

}
