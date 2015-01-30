package tesniere;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import symbols.*;
import tesniere.ProbModelStructureFiller.BoxStructureExpansionEvent;
import tesniere.ProbModelStructureFiller.BoxStructureRelationEvent;
import tesniere.ProbModelStructureFiller.FillStructureEvent;
import tsg.HeadLabeler;
import tsg.TSNodeLabel;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;
import backoff.BackoffModel_Eisner;

public class ProbModelStructureFiller_original extends ProbModelStructureFiller implements Serializable {	
	
	private static final long serialVersionUID = 0L;
	
	public ProbModelStructureFiller_original(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);		
	}
	
	public ProbModelStructureFiller_original() {
		super();
	}
	
	public void initModels() {
		structureRelationModel = new BackoffModel_Eisner(5, 0.005, 0.5, 3); //P(node | parent dir sister)
		boxExpansionModel = new BackoffModel_Eisner(2, 0.005, 0.5, 3); //P(elementsList |  box)
		fillStructureModel = new BackoffModel_Eisner(4, 0.005, 0.5, 3); //P(contentNode | node parent dir sister )
	}
	
	public static ProbModelStructureFiller_original fromBinaryFile(File inputFile) {
		ProbModelStructureFiller_original model = null; 
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
			model = (ProbModelStructureFiller_original) in.readObject();
		} catch (Exception e) {FileUtil.handleExceptions(e);}
		return model;
	}
	
	public void toBinaryFile(File outputFile) {
		try{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
			out.writeObject(this);
		} catch (Exception e) {FileUtil.handleExceptions(e);}		
	}

	public Symbol[][] encodeBoxStructureRelationEvent(BoxStructureRelationEvent e) {
		//P(node | grandParent, parent dir sister)
		//Box grandParent;
		//Box parent;
		//Box node;
		//int dir;
		//Box previousSisterSameSide;
				
		Symbol grandParentEncoding = boxOrigCatEncoding(e.grandParent);
		Symbol parentEncoding = boxOrigCatEncoding(e.parent);
		Symbol nodeEncoding = boxCatsEmptyWordsEncoding(e.node);
		Symbol dirEncoding = new SymbolInt(e.dir);
		Symbol sisterEncoding = boxCatsEmptyWordsEncoding(e.previousSisterSameSide);
		
		Symbol[][] result = new Symbol[][]{
				{nodeEncoding,grandParentEncoding, parentEncoding, dirEncoding, sisterEncoding}
		};
		return result;
	}

	public Symbol[][] encodeBoxStructureExpansionEvent(BoxStructureExpansionEvent e) {
		//P(elementsList |  box)
		//Box box;
		//TreeSet<Entity> elementsSet; (coord, box, punct)
	
		Symbol boxEncoding = boxDerivedCatEncoding(e.box);		
		
		TreeSet<Entity> elementsSet = e.elementsSet;
		Symbol elementsEncoding = null;
		if (elementsSet==null) elementsEncoding = new SymbolString("*NULL_EVENT*");
		else {
			Vector<Symbol> vectorEncoding = new Vector<Symbol>();			
			for(Entity elem : elementsSet) {			
				if (elem.isBox()) vectorEncoding.add(boxCatsEmptyWordsEncoding((Box)elem));
				else {					
					if (elem.isFunctionalWord()) {
						FunctionalWord ew = (FunctionalWord)elem;
						if (ew.isVerb()) continue;
						vectorEncoding.add(new SymbolString((ew.getLex())));
					}
					else vectorEncoding.add(new SymbolString(((ContentWord)elem).getCatString()));
				}
			}
			elementsEncoding = new SymbolList(vectorEncoding);
		}
		Symbol[][] result = new Symbol[][]{{elementsEncoding, boxEncoding}};
		return result;
	}

	public Symbol[][][] encodeFillStructureEvent(FillStructureEvent e) {
		//P(contentNode | node parent dir sister )
		//TreeSet<BoxStandard> parents;
		//BoxStandard node;
		//int dir;
		//TreeSet<BoxStandard> previousSistersSameSide;		
		
		BoxStandard node = e.node;
		Symbol nodeStructureEncoding = boxDerivedCatEmptyWordsEncoding(node);		
		Symbol nodeContentEncoding = boxStdFullPosEncoding(node);
		Symbol dirEncoding = new SymbolInt(e.dir);
		
		TreeSet<BoxStandard> parents = e.parents;
		TreeSet<BoxStandard> previousSistersSameSide = e.previousSistersSameSide;
		
		if (parents==null) {
			Symbol parentEncoding = new SymbolString("*NULL_EVENT*");
			Symbol sisterEncoding = new SymbolString("*NULL_EVENT*");
			Symbol[][][] result = new Symbol[][][]{{
				{nodeContentEncoding, nodeStructureEncoding, parentEncoding, dirEncoding, sisterEncoding}				
			}};
			return result;			
		}		
		if (previousSistersSameSide==null) {
			Symbol sisterEncoding = new SymbolString("*NULL_EVENT*");
			int size = parents.size();
			Symbol[][][] result = new Symbol[size][][];
			int index = 0;
			for(BoxStandard p : parents) {				
				Symbol parentEncoding = boxStdFullPosEncoding(p);
				result[index++] = new Symbol[][]{
					{nodeContentEncoding, nodeStructureEncoding, parentEncoding, dirEncoding, sisterEncoding}					
				};				
			}
			return result;
		}
		int size = parents.size() * previousSistersSameSide.size();
		Symbol[][][] result = new Symbol[size][][];
		int index = 0;
		for(BoxStandard p : parents) {
			Symbol parentEncoding = boxStdFullPosEncoding(p);			
			for(BoxStandard s : previousSistersSameSide) {				
				Symbol sisterEncoding = boxDerivedCatEncoding(s);
				result[index++] = new Symbol[][]{
					{nodeContentEncoding, nodeStructureEncoding, parentEncoding, dirEncoding, sisterEncoding}					
				};
			}			
		}
		return result;		
	}

	public static Symbol boxOrigCatEncoding(Box b) {
		if (b==null) return new SymbolString("*NULL_EVENT*");		
		return new SymbolString(b.getOriginalCatString());
	}
	
	public static Symbol boxDerivedCatEncoding(Box b) {
		if (b==null) return new SymbolString("*NULL_EVENT*");		
		return new SymbolString(b.getDerivedCatString());
	}
	
	public static Symbol boxDerivedCatEmptyWordsEncoding(Box b) {
		if (b==null) return new SymbolString("*NULL_EVENT*");
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();		
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		if (b.ewList!=null) {
			for(FunctionalWord ew : b.ewList) {
				vectorEncoding.add(new SymbolString(ew.getLex()));
			}
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxCatsEncoding(Box b) {
		if (b==null) return new SymbolString("*NULL_EVENT*");
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxOrigCatEmptyWordsEncoding(Box b) {
		if (b==null) return new SymbolString("*NULL_EVENT*");
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));		
		if (b.ewList!=null) {
			for(FunctionalWord ew : b.ewList) {
				vectorEncoding.add(new SymbolString(ew.getLex()));
			}			
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxOrigLabelEncoding(Box b) {
		if (b==null) return new SymbolString("*NULL_EVENT*");		
		return new SymbolString(b.originalTSNodeLabel.toString());
	}
	
	public static Symbol boxCatsFirstEmptyWordEncoding(Box b) {
		if (b==null) return new SymbolString("*NULL_EVENT*");
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		if (b.ewList!=null) vectorEncoding.add(new SymbolString(b.ewList.first().getLex()));
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxCatsEmptyWordsEncoding(Box b) {
		if (b==null) return new SymbolString("*NULL_EVENT*");
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		if (b.ewList!=null) {
			for(FunctionalWord ew : b.ewList) {
				vectorEncoding.add(new SymbolString(ew.getLex()));
			}
		}
		return new SymbolList(vectorEncoding);
	}

	public static Symbol boxStdFullEmptyWordsEncoding(BoxStandard b) {
		TreeSet<Word> fullEmptyWords = new TreeSet<Word>();
		if (b.ewList!=null) fullEmptyWords.addAll(b.ewList);
		if (b.fwList!=null) fullEmptyWords.addAll(b.fwList);		
		Vector<Symbol> nodeContentVectorEncoding = new Vector<Symbol>();
		for(Word w : fullEmptyWords) {
			nodeContentVectorEncoding.add(new SymbolString(w.getLex()));
		}
		return new SymbolList(nodeContentVectorEncoding);
	}
	
	public static Symbol boxStdEmptyWordsFullPosEncoding(BoxStandard b) {
		TreeSet<Word> fullEmptyWords = new TreeSet<Word>();
		if (b.ewList!=null) fullEmptyWords.addAll(b.ewList);
		if (b.fwList!=null) fullEmptyWords.addAll(b.fwList);		
		Vector<Symbol> nodeContentVectorEncoding = new Vector<Symbol>();
		for(Word w : fullEmptyWords) {
			SymbolString encoding = new SymbolString(w.isContentWord() ? w.getPos() : w.getLex());
			nodeContentVectorEncoding.add(encoding);
		}
		return new SymbolList(nodeContentVectorEncoding);
	}
	
	public static Symbol boxStdEmptyWordsFullCatEncoding(BoxStandard b) {
		TreeSet<Word> fullEmptyWords = new TreeSet<Word>();
		if (b.ewList!=null) fullEmptyWords.addAll(b.ewList);
		if (b.fwList!=null) fullEmptyWords.addAll(b.fwList);		
		Vector<Symbol> nodeContentVectorEncoding = new Vector<Symbol>();
		for(Word w : fullEmptyWords) {
			SymbolString encoding = new SymbolString(w.isContentWord() ? ((ContentWord)w).getCatString() : w.getLex());
			nodeContentVectorEncoding.add(encoding);
		}
		return new SymbolList(nodeContentVectorEncoding);
	}
	
	public static Symbol boxStdFullWordsEncoding(BoxStandard b) {
		if (b.fwList==null) return new SymbolString("*NULL_EVENT*");
		Vector<Symbol> nodeContentVectorEncoding = new Vector<Symbol>();
		for(Word w : b.fwList) {
			nodeContentVectorEncoding.add(new SymbolString(w.getLex()));
		}
		return new SymbolList(nodeContentVectorEncoding);
	}
	
	public static Symbol boxStdFullPosEncoding(BoxStandard b) {
		if (b.fwList==null) return new SymbolString("*NULL_EVENT*");
		Vector<Symbol> nodeContentVectorEncoding = new Vector<Symbol>();
		for(Word w : b.fwList) {
			nodeContentVectorEncoding.add(new SymbolString(w.getPos()));
		}
		return new SymbolList(nodeContentVectorEncoding);
	}
	
	public static void test() throws Exception {
		//File trainingSet = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		//File sec00 = new File(Wsj.WsjOriginalCleaned + "wsj-00.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_01_02_10"));
		//ArrayList<Box> trainingTreebank = 
		//	Conversion.getTesniereTreebank(trainingSet, HL);
		//ArrayList<Box> sec00Treebank = Conversion.getTesniereTreebank(sec00, HL);		
		//ProbModelStructureFiller_1LB PME = new ProbModelStructureFiller_1LB(trainingTreebank);
		ProbModelStructureFiller_original PME = new ProbModelStructureFiller_original();
		
		//File binaryFileModel = new File("resources/ProbModelStructureFiller_1LB_training.bin");
		//PME.toBinaryFile(binaryFileModel);
		//ProbModelStructureFiller_1LB PME = fromBinaryFile(binaryFileModel);
		//PME.preprocessTrainig();
		//PME.train();
		//Box b = sec00Treebank.get(551);
		//PME.trainFromStructure(b);		
		//PME.reportModelsTables();
		//double prob = PME.getProb(b, true);
		//System.out.println("\n\nTotal Prob: " + prob);
		
		TSNodeLabel oneBest = new TSNodeLabel( "(TOP (SINV (`` ``) (S (NP (PRP It)) (VP (AUX 's) (NP (NP (DT a) (NN problem)) (SBAR (WHNP (WDT that)) (S (ADVP (RB clearly)) (VP (AUX has) (S (VP (TO to) (VP (AUX be) (VP (VBN resolved))))))))))) (, ,) ('' '') (VP (VBD said)) (NP (NP (NNP David) (NNP Cooke)) (, ,) (NP (NP (JJ executive) (NN director)) (PP (IN of) (NP (DT the) (NNP RTC))))) (. .)))");
		TSNodeLabel reranked = new TSNodeLabel("(TOP (SINV (`` ``) (S (NP (PRP It)) (VP (VBZ 's) (NP (NP (DT a) (NN problem)) (SBAR (WHNP (WHNP (DT that)) (ADVP (RB clearly))) (S (VP (VBZ has) (S (VP (TO to) (VP (VB be) (VP (VBN resolved))))))))))) (, ,) ('' '') (VP (VBD said)) (NP (NP (NNP David) (NNP Cooke)) (, ,) (NP (NP (JJ executive) (NN director)) (PP (IN of) (NP (DT the) (NNP RTC))))) (. .)))");
		
		Box oneBestTDS = Conversion.getTesniereStructure(oneBest, HL);
		Box rerankedTDS = Conversion.getTesniereStructure(reranked, HL);
		
		ArrayList<Event> list = PME.getEventsList(oneBestTDS);
		for(Event e : list) {
			System.out.println(e.encodingToStringFreq(0,0));
		}
		
		System.out.println("\n------------------\n");
		
		list = PME.getEventsList(rerankedTDS);
		for(Event e : list) {
			System.out.println(e.encodingToStringFreq(0,0));
		}
		
	}
	
	public static String intPairToString(int[] a) {
		StringBuilder sb = new StringBuilder();
		int length = a.length;
		int index = 0;
		while(index<length) {
			if (index>0) sb.append("\t");
			sb.append(a[index++] + ", " + a[index++]);			
		}
		return sb.toString();
	}
	
	public static void compareOneBestOracle() throws Exception {
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_22_01_10"));
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);
		ProbModelStructureFiller_original PME = new ProbModelStructureFiller_original(trainingTreebank);
		PME.train();		
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");
		File oneBestFile = new File(baseDir + "wsj-22_oneBest_ADV.mrg");
		File oracleBestFile = new File(baseDir + "wsj-22_oracleBest_ADV.mrg");		
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);
		ArrayList<TSNodeLabel> oneBestTreebank = TSNodeLabel.getTreebank(oneBestFile);
		ArrayList<TSNodeLabel> oracleBestTreebank = TSNodeLabel.getTreebank(oracleBestFile);
				
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();
		Iterator<TSNodeLabel> oneBestIter = oneBestTreebank.iterator();
		Iterator<TSNodeLabel> oracleBestIter = oracleBestTreebank.iterator();
		
		int fscoreImprovedWellReranked = 0;
		int fscoreImprovedBadlyReranked = 0;
		
		int size = goldTreebank.size();
		for(int i=0; i<size; i++) {
			System.out.println("Comparing sentence: " + (i+1));
			TSNodeLabel goldTree = goldIter.next();
			TSNodeLabel oneBestTree = oneBestIter.next();
			TSNodeLabel oracleBestTree = oracleBestIter.next();
			Box goldTDS = Conversion.getTesniereStructure(goldTree, HL);
			Box oneBestTDS = Conversion.getTesniereStructure(oneBestTree, HL);
			Box oracleBestTDS = Conversion.getTesniereStructure(oracleBestTree, HL);
			
			ArrayList<Event> oneBestEventsList = PME.getEventsList(oneBestTDS);
			ArrayList<Event> oracleBestEventsList = PME.getEventsList(oracleBestTDS);
			
			ArrayList<Event> oneBestEventsUnique = new ArrayList<Event>(oneBestEventsList);
			ArrayList<Event> oracleBestEventsUnique = new ArrayList<Event>(oracleBestEventsList);
			Utility.removeAllOnce(oneBestEventsUnique, oracleBestEventsList);
			Utility.removeAllOnce(oracleBestEventsUnique, oneBestEventsList);
			
			int[] oneBestUniqueTotal = new int[]{oneBestEventsUnique.size(), oneBestEventsList.size()};
			int[] oracleBestUniqueTotal = new int[]{oracleBestEventsUnique.size(), oracleBestEventsList.size(), };
			
			
			float fscoreOneBest = EvalC.getFScore(oneBestTree, goldTree);
			float fscoreOracleBest = EvalC.getFScore(oracleBestTree, goldTree);
			
			//double probGold = PME.getProb(goldTDS);
			double probOneBest = ProbModel.getProb(oneBestEventsList);
			double probOracleBest = ProbModel.getProb(oracleBestEventsList);
			
			int[] evalDepOneBest = EvalTDS.getCatRawDepMatch(goldTDS, oneBestTDS);
			int[] evalDepOracleBest = EvalTDS.getCatRawDepMatch(goldTDS, oracleBestTDS);
			
			float fScoreImprovement = fscoreOracleBest - fscoreOneBest;
						
			int rowDepImprovement = evalDepOracleBest[1] - evalDepOneBest[1];
			int catDepImprovement = evalDepOracleBest[2] - evalDepOneBest[2];
			
			double probRatio = probOracleBest / probOneBest; 
			boolean probImprovement = probRatio>1;
			
			if (fScoreImprovement>0) {
				if (probImprovement) fscoreImprovedWellReranked++;
				else fscoreImprovedBadlyReranked++;
			}
			
			System.out.println(fScoreImprovement + "\t" + rowDepImprovement + 
					" " + catDepImprovement + "\t" + probImprovement + " (" + probRatio + ")");
			System.out.println("One Best Unique: " + Arrays.toString(oneBestUniqueTotal));
			for(Event e : oneBestEventsUnique) {
				System.out.println(e.encodingToStringFreq(0,0));
			}
			System.out.println("Oracle Best Unique: " + Arrays.toString(oracleBestUniqueTotal));
			for(Event e : oracleBestEventsUnique) {
				System.out.println(e.encodingToStringFreq(0,0));
			}
			System.out.println("\n\n");		
		}
		
		System.out.println("Total sentences: " + size);
		System.out.println("Equals: " + (size-fscoreImprovedWellReranked-fscoreImprovedBadlyReranked));
		System.out.println("Well reranked: " + fscoreImprovedWellReranked);
		System.out.println("Wrong reranked: " + fscoreImprovedBadlyReranked);
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
	
	public static void rerankDebug(int nBest) throws Exception {
		File trainingSet = new File(Wsj.WsjOriginalCleanedSemTagsOffAuxify + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_03_02_10"));
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);
		ProbModelStructureFiller_original PME = new ProbModelStructureFiller_original(trainingTreebank);
		PME.initModels();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22/";
		File goldFile = new File(baseDir + "wsj-22_gold.mrg");
		//File oneBestFile = new File(baseDir + "wsj-22_oneBest_ADV.mrg");
		//File oneBestFileEvalF = new File(baseDir + "wsj-22_oneBest_ADV.evalF");
		//EvalF.staticEvalF(goldFile, oneBestFile, oneBestFileEvalF, true);
		
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_cleaned.mrg");
		File rerankedFile = new File(baseDir + "wsj-22_reranked_" + nBest + "best.mrg");
		//File rerankedFileEvalF = new File(baseDir + "wsj-22_reranked_" + nBest + "best.evalF");
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFile);
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);		
				
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();		
		
		int size = goldTreebank.size();
		
		int[] depScoreReranked = new int[]{0,0,0};
		int[] fScoreReranked = new int[]{0,0,0};
		int[] depScoreOneBest = new int[]{0,0,0};
		int[] fScoreOneBest = new int[]{0,0,0};
		
		for(int i=0; i<size; i++) {			
			ArrayList<TSNodeLabel> nBestTrees = nextNBest(nBest, nBestScanner);
			TSNodeLabel gold = goldIter.next();
			Box goldTDS = Conversion.getTesniereStructure(gold, HL);								
			double maxProb = -1;
			boolean first = true;
			TSNodeLabel oneBest = null;
			Box oneBestTDS = null;
			TSNodeLabel bestReranked = null;
			Box bestRerankedTDS = null;
			double oneBestProb = -1;
			for(TSNodeLabel t : nBestTrees) {				
				if (first) {
					Box b = Conversion.getTesniereStructure(t, HL);
					first = false;
					oneBest = t;	
					oneBestTDS = b;
					oneBestProb = maxProb = PME.getProb(b);
					bestReranked = t;
					bestRerankedTDS = b;
					continue;
				}
				//if (!t.samePos(oneBest)) continue;
				Box b = Conversion.getTesniereStructure(t, HL);
				double p = PME.getProb(b);
				if (p>maxProb) {
					maxProb = p;
					bestReranked = t;
					bestRerankedTDS = b;
				}
			}
			
			int[] evalDepOneBest = EvalTDS.getCatRawDepMatch(goldTDS, oneBestTDS);
			Utility.arrayIntPlus(evalDepOneBest, depScoreOneBest);
			int[] evalFOneBest = EvalC.getScores(gold, oneBest);
			Utility.arrayIntPlus(evalFOneBest, fScoreOneBest);
			
			int[] evalDepReranked = EvalTDS.getCatRawDepMatch(goldTDS, bestRerankedTDS);
			Utility.arrayIntPlus(evalDepReranked, depScoreReranked);
			int[] evalFReranked = EvalC.getScores(gold, bestReranked);
			Utility.arrayIntPlus(evalFReranked, fScoreReranked);
									
			ArrayList<Event> oneBestEvents = PME.getEventsList(oneBestTDS);
			ArrayList<Event> rerankedEvents = PME.getEventsList(bestRerankedTDS);
			
			ArrayList<Event> oneBestEventsUnique = new ArrayList<Event>(oneBestEvents);
			ArrayList<Event> rerankedEventsUnique = new ArrayList<Event>(rerankedEvents);
			Utility.removeAllOnce(oneBestEventsUnique, rerankedEvents);
			Utility.removeAllOnce(rerankedEventsUnique, oneBestEvents);
			
			int[] oneBestUniqueTotal = new int[]{oneBestEventsUnique.size(), oneBestEvents.size()};
			int[] rerankedUniqueTotal = new int[]{rerankedEventsUnique.size(), rerankedEvents.size()};
												
			pw.println(bestReranked.toString());
			
			double probRatio = maxProb / oneBestProb; 
			//boolean probImprovement = probRatio>1;			
			
			float[] oneBestAttScore = EvalTDS.rawDepAttachmentScore(evalDepOneBest);
			float[] rerankedAttScore = EvalTDS.rawDepAttachmentScore(evalDepReranked);
			float oneBestFScore = EvalC.fscore(evalFOneBest);
			float rerankedFScore = EvalC.fscore(evalFReranked);
			
			int[] diffDep = new int[]{evalDepReranked[1]-evalDepOneBest[1], evalDepReranked[2]-evalDepOneBest[2]};			
			float diffFscore = rerankedFScore - oneBestFScore;
			
			System.out.println((i+1) + 
					"\n" + "One Best FScore, dep\t" + 
						oneBestFScore + "\t" + Arrays.toString(oneBestAttScore) +
					"\n" + "Reranked Best FScore, dep\t" +	
						rerankedFScore + "\t" + Arrays.toString(rerankedAttScore) +
					"\n" + "FScore, Diff Ratio " + 	diffFscore + "\t" + probRatio +
					"\n" + "Diff Raw, Dep attachment: " + Arrays.toString(diffDep) + "\n-----");
						
			System.out.println("One Best Unique: " + Arrays.toString(oneBestUniqueTotal));
			for(Event e : oneBestEventsUnique) {
				System.out.println(e.encodingToStringFreq(0,0));
			}
			System.out.println("Reranked Unique: " + Arrays.toString(rerankedUniqueTotal));
			for(Event e : rerankedEventsUnique) {
				System.out.println(e.encodingToStringFreq(0,0));
			}
			System.out.println("\n\n");
			
			if (i==9) break;
		}
		pw.close();
		float rerankedFScore = EvalC.fscore(fScoreReranked);
		float oneBestFScore = EvalC.fscore(fScoreOneBest);
		float[] oneBestAttScore = EvalTDS.rawDepAttachmentScore(depScoreOneBest);
		float[] rerankedAttScore = EvalTDS.rawDepAttachmentScore(depScoreReranked);
		System.out.println("----------------------\n\n");
		System.out.println("One Best Attachment Score Raw Dep: " + Arrays.toString(oneBestAttScore));
		System.out.println("Reranked ttachment Score Raw Dep: " + Arrays.toString(rerankedAttScore));
		System.out.println("One Best FScore: " + oneBestFScore);
		System.out.println("Reranked FScore: " + rerankedFScore);
	}
	
	public static void rerank(int nBest) throws Exception {
		File trainingSet = new File(Wsj.WsjOriginalCleanedSemTagsOffAuxify + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_03_02_10"));
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);
		ProbModelStructureFiller_original PME = new ProbModelStructureFiller_original(trainingTreebank);
		PME.initModels();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22/";
		File goldFile = new File(baseDir + "wsj-22_gold.mrg");
		//File oneBestFile = new File(baseDir + "wsj-22_oneBest_ADV.mrg");											 
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_cleaned.mrg");
		File rerankedFile = new File(baseDir + "wsj-22_reranked_" + nBest + "best.mrg");
		File rerankedFileEvalF = new File(baseDir + "wsj-22_reranked_" + nBest + "best.evalF");
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFile);
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);		
				
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();		
		
		int size = goldTreebank.size();
		int[] depScoreReranked = new int[]{0,0,0};
		int[] depScoreOneBest = new int[]{0,0,0};
		for(int i=0; i<size; i++) {			
			ArrayList<TSNodeLabel> nBestTrees = nextNBest(nBest, nBestScanner);
			TSNodeLabel gold = goldIter.next();
			Box goldTDS = Conversion.getTesniereStructure(gold, HL);			
			TSNodeLabel bestReranked = null;			
			double maxProb = -1;
			boolean first = true;
			int[] evalDepOneBest = null;
			TSNodeLabel oneBest = null;
			for(TSNodeLabel t : nBestTrees) {				
				if (first) {
					Box b = Conversion.getTesniereStructure(t, HL);
					first = false;
					oneBest = t;
					evalDepOneBest = EvalTDS.getCatRawDepMatch(goldTDS, b);
					Utility.arrayIntPlus(evalDepOneBest, depScoreOneBest);
					maxProb = PME.getProb(b);
					bestReranked = t;
					continue;
				}
				//if (!t.samePos(oneBest)) continue;
				Box b = Conversion.getTesniereStructure(t, HL);
				double p = PME.getProb(b);
				if (p>maxProb) {
					maxProb = p;
					bestReranked = t;
				}
			}
			pw.println(bestReranked.toString());
			Box bestRerankedTDS = Conversion.getTesniereStructure(bestReranked, HL);
			int[] evalDepReranked = EvalTDS.getCatRawDepMatch(goldTDS, bestRerankedTDS);
			Utility.arrayIntPlus(evalDepReranked, depScoreReranked);
			float rerankedFScore = EvalC.getFScore(gold, bestReranked);
			float oneBestFScore = EvalC.getFScore(gold, oneBest);						
			float diffFScore = rerankedFScore-oneBestFScore;
			int[] diffDep = new int[]{evalDepReranked[0], evalDepReranked[1]-evalDepOneBest[1],
					evalDepReranked[2]-evalDepOneBest[2]};
			System.out.println((i+1) + "\t" + diffFScore + "\t" + Arrays.toString(diffDep));
		}
		pw.close();
		float[] oneBestFScore = EvalC.staticEvalF(goldFile, rerankedFile, rerankedFileEvalF, true);
		System.out.println("One Best Dep Total Raw Dep: " + Arrays.toString(depScoreOneBest));
		System.out.println("Reranked Dep Total Raw Dep: " + Arrays.toString(depScoreReranked));
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(oneBestFScore));		
	}
		

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//test();
		//compareOneBestOracle();
		//rerankDebug(10);
		rerank(10);		
	}

	
}
