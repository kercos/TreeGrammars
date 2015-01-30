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
import symbols.SymbolInt;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.ProbModel.Event;
import tesniere.ProbModelStructureFiller.BoxStructureExpansionEvent;
import tesniere.ProbModelStructureFiller.BoxStructureRelationEvent;
import tesniere.ProbModelStructureFiller.FillStructureEvent;
import tsg.HeadLabeler;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;

public class ProbModelStructureFiller_new extends ProbModel {		
	
	BackoffModel structureRelationModel;	
	BackoffModel boxExpansionModel;
	BackoffModel fillStructureModel;
	
	public ProbModelStructureFiller_new(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);
	}	
	
	public ProbModelStructureFiller_new() {		
		super();
	}
	
	public void reportModelsTables(PrintStream out) {
		out.println("Structure Relation Model:");
		structureRelationModel.printTables(out);
		out.println("---------------------\n\n");
		out.println("Box Expansion Model:");
		boxExpansionModel.printTables(out);
		out.println("---------------------\n\n");
		out.println("Fill Structure Model:");
		fillStructureModel.printTables(out);
		out.println("---------------------\n\n");
	}
	
	private TreeSet<BoxStandard> getConjunctsStd(BoxJunction junction) {
		if (junction==null) return null;
		TreeSet<BoxStandard> result = new TreeSet<BoxStandard>();
		for(Box conj : junction.conjuncts) {
			if (conj.isJunctionBlock()) result.addAll(getConjunctsStd((BoxJunction)conj));
			else result.add((BoxStandard)conj);
		}
		return result;
	}	
	
	private TreeSet<BoxStandard> getGovParentsStd(Box gov) {
		if (gov==null) return null;
		TreeSet<BoxStandard> result = new TreeSet<BoxStandard>();
		if (gov.isJunctionBlock()) {
			BoxJunction govJun = (BoxJunction) gov;
			result.addAll(getConjunctsStd(govJun));
		}
		else result.add((BoxStandard)gov);
		return result;
	}
		
	
	public void extractEventsFromStructure(Box b, ArrayList<Event> eventsCollector) {
		eventsCollector.add(new BoxStructureRelationEvent(null, null, b, -5, -5, null, 0));
		extractStructure(null, b, eventsCollector);
		
		int sentenceLength = b.countAllWords();
		IdentityHashMap<Word, Box> wordBoxMapping = new IdentityHashMap<Word, Box>();		
		Word[] wordsArray = new Word[sentenceLength];				
		b.fillWordArrayTable(wordsArray, wordBoxMapping);
	}
	
	//three directions new way
	 private void extractStructure(Box gp, Box p, ArrayList<Event> eventsCollector) {
		TreeSet<Box> leftDep = new TreeSet<Box>();
		TreeSet<Box> innerDep = new TreeSet<Box>();
		TreeSet<Box> rightDep = new TreeSet<Box>();
			
		 if (p.dependents!=null) {						
			for(Box d : p.dependents) {
				extractStructure(p, d, eventsCollector);
				int dir = Entity.leftOverlapsRight(d, p);
				switch(dir) {
					case -1: 
						leftDep.add(d);
						break;	
					case 0: 
						innerDep.add(d);
						break;
					case 1: 
						rightDep.add(d);
						break;
				}
			}
		 }
			
		Box previousLD = null;
		int parentDir = 0;
		int ldi = leftDep.size();
		for(Box ld : leftDep) {
			ldi--;
			parentDir = gp==null ? -5 : Entity.leftOverlapsRight(p, gp);
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, ld, -1, parentDir, previousLD, ldi);
			eventsCollector.add(e);
			previousLD = ld;			
		}
		parentDir = gp==null ? -5 : Entity.leftOverlapsRight(p, gp);
		eventsCollector.add(new BoxStructureRelationEvent(gp, p, null, -1, parentDir, previousLD, 0)); //STOP
		
		Box previousID = null;
		for(Box id : innerDep) {
			parentDir = gp==null ? -5 : Entity.leftOverlapsRight(p, gp);
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, id, 0, parentDir, previousID, 0);
			eventsCollector.add(e);
			previousID = id;
		}
		parentDir = gp==null ? -5 : Entity.leftOverlapsRight(p, gp);
		eventsCollector.add(new BoxStructureRelationEvent(gp, p, null, 0, parentDir, previousID, 0)); //STOP
		
		Box previousRD = null;
		int rdi = 0;
		for(Box rd : rightDep) {
			parentDir = gp==null ? -5 : Entity.leftOverlapsRight(p, gp);
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, rd, 1, parentDir, previousRD, rdi);
			eventsCollector.add(e);
			previousRD = rd;
			rdi++;
		}
		parentDir = gp==null ? -5 : Entity.leftOverlapsRight(p, gp);
		eventsCollector.add(new BoxStructureRelationEvent(gp, p, null, 1, parentDir, previousRD, rdi)); //STOP

		doTheExpansionAndFilling(gp, p, eventsCollector);
	}

	private void doTheExpansionAndFilling(Box gp, Box b, ArrayList<Event> eventsCollector) {
		TreeSet<Entity> elementsSet = new TreeSet<Entity>();
		if (b.isJunctionBlock()) {			
			BoxJunction bJun = (BoxJunction)b;			
			if (bJun.conjuncts!=null) {
				elementsSet.addAll(bJun.conjuncts);
				for(Box c : bJun.conjuncts) {
					extractStructure(gp, c, eventsCollector);
				}
			}
			if (bJun.conjunctions!=null) {
				int previousConjPos = -5;
				FunctionalWord currenctConjunctionBlock = null;
				for(FunctionalWord c : bJun.conjunctions) {
					if (c.position==previousConjPos+1) {
						currenctConjunctionBlock.word += "_" + c.word;
					}
					else {
						if (currenctConjunctionBlock!=null) elementsSet.add(currenctConjunctionBlock);
						currenctConjunctionBlock = c.clone();
					}
				}
				if (currenctConjunctionBlock!=null) elementsSet.add(currenctConjunctionBlock);				
			}
			
		}
		else {
			BoxStandard bStd = (BoxStandard)b;
			fillStructure(bStd, eventsCollector);
		}
		Entity previous = null;
		Entity previousPrevious = null;
		for(Entity element : elementsSet) {
			Entity[] triplet = new Entity[]{previousPrevious, previous, element};
			BoxStructureExpansionEvent event = new BoxStructureExpansionEvent(b, triplet);
			eventsCollector.add(event);
			previousPrevious = previous;
			previous = element;
		}
		Entity[] triplet = new Entity[]{previousPrevious, previous, null};
		BoxStructureExpansionEvent event = new BoxStructureExpansionEvent(b, triplet);
		eventsCollector.add(event);
		//triplet = new Entity[]{previous, null, null};
		//event = new BoxStructureExpansionEvent(b, triplet);
		//eventsCollector.add(event);
	}
	
	private void fillStructure(BoxStandard bs, ArrayList<Event> eventsCollector) {
		Box gov = bs.governingParent();
		if (gov==null) {
			FillStructureEvent e = new FillStructureEvent(null, bs, -1, null);
			eventsCollector.add(e);
			return;
		}
		TreeSet<BoxStandard> govParentsStd = getGovParentsStd(gov);

		Box sameSideSister = getSameSidePrevoiusSister(gov, bs);
		
		//int dir = Entity.leftOverlapsRight(bs, gov);
		int dir = leftRight(bs, gov);
		
		FillStructureEvent e = new FillStructureEvent(govParentsStd, bs, dir, sameSideSister); 
		eventsCollector.add(e);				
	}		
	
	private static int leftRight(Box b1, Box b2) {
		if (b1==null || b2==null) return -1;
		return leftRight(b1.firstFullWord(), b2.firstFullWord());
	}
	
	private static int leftRight(ContentWord w1, ContentWord w2) {
		if (w1==null || w2==null) return -1;
		return w1.position < w2.position ? -1 : 1;
	}
	
	private Box getSameSidePrevoiusSister(Box gov, Box daughter) {		
		Box previousBox = null;		
		for(Box d : gov.dependents) {			
			if (d==daughter) {			 
				if (previousBox==null) return null;												
				if (!sameSide(gov, previousBox, daughter)) return null;
				return previousBox;
			}
			previousBox = d;
		}
		return null;
	}	
		
	private boolean sameSide(Box gov, Box b1, Box b2) {
		//return Entity.leftOverlapsRight(b1, gov)==Entity.leftOverlapsRight(b2, gov);
		ContentWord govFW = gov.firstFullWord();
		ContentWord b1FW = b1.firstFullWord();
		ContentWord b2FW = b2.firstFullWord();
		return leftRight(b1FW,govFW)==leftRight(b2FW,govFW);
	}

		
	
	public final static String[] leftInnerRight = new String[]{"LEFT","INNER","RIGHT"};
	
	public final static String nullNode = "*NULL*";
	
	public static String toStringCatsEmptyWords(Box b) {
		if (b==null) return nullNode;
		StringBuilder result = new StringBuilder();		
		result.append("[" + Box.catToString(b.originalCat) + ", " + 
				Box.catToString(b.derivedCat) + "]");		
		if (b.ewList!=null) result.append(" Empty Words: " + b.ewList);
		return result.toString();
	}
	
	public static String toStringCatsEmptyFullWords(BoxStandard b) {
		if (b==null) return nullNode;
		StringBuilder result = new StringBuilder();		
		result.append("[" + Box.catToString(b.originalCat) + ", " + 
				Box.catToString(b.derivedCat) + "]" );		
		if (b.ewList!=null) result.append(" Empty Words: " + b.ewList);
		if (b.fwList!=null) result.append(" Full Words: " + b.fwList);
		return result.toString();
	}
	
	public static SymbolString emptyEvent = new SymbolString("{}");
	public static SymbolString nullEvent = new SymbolString("*NULL*");
	
	public static Symbol boxOrigCatEncoding(Box b) {
		if (b==null) return nullEvent;		
		return new SymbolString(b.getOriginalCatString());
	}
	
	public static Symbol boxCatsEncoding(Box b, Symbol eventIfNull) {
		if (b==null) return eventIfNull;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxCatsEmptyWordsPunctEncoding(Box b) {
		if (b==null) return nullEvent;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		TreeSet<FunctionalWord> emptyWordsPunt = new TreeSet<FunctionalWord>();
		if (b.punctList!=null) emptyWordsPunt.addAll(b.punctList);
		if (b.ewList!=null) emptyWordsPunt.addAll(b.ewList);
		for (FunctionalWord ewp : emptyWordsPunt) {
			vectorEncoding.add(new SymbolString(ewp.getLex()));
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxCatsEmptyWordsEncoding(Box b, Symbol eventIfNull) {
		if (b==null) return eventIfNull;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		TreeSet<FunctionalWord> emptyWordsPunt = new TreeSet<FunctionalWord>();
		if (b.ewList!=null) emptyWordsPunt.addAll(b.ewList);
		for (FunctionalWord ewp : emptyWordsPunt) {
			//vectorEncoding.add(new SymbolString(ewp.getLex()));
			vectorEncoding.add(new SymbolString(ewp.getPos()));
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxDerivedCatEmptyWordsPunctEncoding(Box b) {
		if (b==null) return nullEvent;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();		
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		TreeSet<FunctionalWord> emptyWordsPunt = new TreeSet<FunctionalWord>();
		if (b.punctList!=null) emptyWordsPunt.addAll(b.punctList);
		if (b.ewList!=null) emptyWordsPunt.addAll(b.ewList);
		for (FunctionalWord ewp : emptyWordsPunt) {
			vectorEncoding.add(new SymbolString(ewp.getLex()));
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxDerivedCatEmptyWordsEncoding(Box b) {
		if (b==null) return nullEvent;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();		
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		TreeSet<FunctionalWord> emptyWords = b.ewList;
		if (emptyWords!=null) {
			for (FunctionalWord ewp : emptyWords) {
				vectorEncoding.add(new SymbolString(ewp.getLex()));
			}
		}
		return new SymbolList(vectorEncoding);
	}

	
	public static Symbol boxDerivedCatEncoding(Box b) {
		if (b==null) return nullEvent;		
		return new SymbolString(b.getDerivedCatString());
	}
	
	public static Symbol boxStdFullWordsPosEncoding(BoxStandard b) {
		if (b.fwList==null) return nullEvent;
		Vector<Symbol> nodeContentVectorEncoding = new Vector<Symbol>();
		for(Word w : b.fwList) {
			nodeContentVectorEncoding.add(new SymbolString(w.getLex()+"_"+w.getPos())); //AffixSuffixManual.tagWord(w.getLex())));			
		}
		return new SymbolList(nodeContentVectorEncoding);
	}
	
	public static Symbol boxStdFullPosEncoding(BoxStandard b) {
		if (b.fwList==null) return nullEvent;
		Vector<Symbol> nodeContentVectorEncoding = new Vector<Symbol>();
		for(Word w : b.fwList) {
			nodeContentVectorEncoding.add(new SymbolString(w.getPos()));
		}
		return new SymbolList(nodeContentVectorEncoding);
	}
	
	public static int wordDistance(BoxStandard b1, BoxStandard b2) {
		if (b1.fwList==null || b2.fwList==null) return -1;
		return wordDistance(b1.fwList.first(), b2.fwList.first());
	}
	
	public static int wordDistance(Word w1, Word w2) {		
		//return Math.abs(w1.position-w2.position);
		int dist = Math.abs(w1.position-w2.position);
		if (dist<3) return dist;
		if (dist<6) return 5;
		if (dist<9) return 8;
		return 10;
		//return 10;
		//if (dist<6) return 1;
		//if (dist<9) return 2;
		//if (dist<12) return 3;
		//return 2;		
	}
	
	
	


	
	public void initModels() {
		initStructureRelationModel();
		initBoxExpansionModel();
		initFillStructureModel();
	}
	
	
	
	protected class BoxStructureRelationEvent extends Event {
		
		private static final long serialVersionUID = 0L;
		
		Box grandParent;
		Box parent;		
		Box generatedNode;
		int dir;
		int dirParent;
		Box previousSisterSameSide;
		int distance;
		
		public BoxStructureRelationEvent(Box grandParent, Box parent, Box generatedNode, 
				int dir, int dirParent, Box previousSisterSameSide, int distance) {
			this.grandParent = grandParent;
			this.parent = parent;
			this.generatedNode = generatedNode;
			this.dir = dir;
			this.dirParent = dirParent;
			this.previousSisterSameSide = previousSisterSameSide;
			this.distance = distance;
		}
		
		public void storeEvent() {
			Symbol[][] eventEncoding = encodeBoxStructureRelationEvent(this);
			structureRelationModel.increaseInTables(eventEncoding);
		}
		
		public double getProb() {
			Symbol[][] eventEncoding = encodeBoxStructureRelationEvent(this);
			return structureRelationModel.getCondProb(eventEncoding);
			//return 1d;
		}
		
		
		@Override
		public boolean equals(Object otherObject) {
			if (otherObject==this) return true;
			if (otherObject instanceof BoxStructureRelationEvent) { 			
				BoxStructureRelationEvent otherBoxStructureRelationEvent = 
					(BoxStructureRelationEvent)otherObject;
				Symbol[][] thisEncoding = encodeBoxStructureRelationEvent(this);
				Symbol[][] otherEncoding = encodeBoxStructureRelationEvent(otherBoxStructureRelationEvent);								
				return Arrays.deepEquals(thisEncoding, otherEncoding);
			}
			return false;
		}

		@Override
		public int[] getCondFreqEventFreq(int decompNumb , int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeBoxStructureRelationEvent(this);
			return structureRelationModel.getCondFreqEventFreq(thisEncodingLevel, 0, backoffLevel);			
		}
		
		@Override
		public String encodingToStringFreq(int decompNumb , int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeBoxStructureRelationEvent(this);
			return "\tStructure\t" + 
			structureRelationModel.getEventFreqToString(thisEncodingLevel, decompNumb, backoffLevel)
			 + "\t" + getProb();
		}

		
	}
	
	public void initStructureRelationModel() {
		//P(structure(box)) = P(node | parent dir dirParent sister)
		int[][][] structureBOL = new int[][][]{{{0,0,0,0,0}}};
		boolean[] structureSkip = new boolean[]{false};
		int[][] structureGroup = new int[][]{{1}};		
		structureRelationModel = new BackoffModel_DivHistory(structureBOL, structureSkip, structureGroup, 0, 5, 0.000001); 				
	}
	
	public static SymbolString stopEvent = new SymbolString("*STOP*");
	
	public Symbol[][] encodeBoxStructureRelationEvent(BoxStructureRelationEvent e) {
		//P(node | grandParent, parent dir sister)
		//Box grandParent;
		//Box parent;
		//Box node;
		//int dir;
		//Box previousSisterSameSide;
		
		//Symbol grandParentEncoding = boxOrigCatEncoding(e.grandParent);
		Symbol parentEncoding = boxOrigCatEncoding(e.parent);
		Box generatedNode = e.generatedNode;
		Symbol nodeEncoding1 = boxCatsEncoding(generatedNode, stopEvent);
		
		//Symbol nodeEncoding2 = boxDerivedCatPunctEncoding(e.node);
		Symbol dirEncoding = new SymbolInt(e.dir);
		Symbol dirParentEncoding = new SymbolInt(e.dirParent);
		Symbol sisterEncoding1 = boxCatsEncoding(e.previousSisterSameSide, nullEvent);
		//Symbol sisterEncoding2 = boxDerivedCatPunctEncoding(e.previousSisterSameSide);
		//Symbol parentDistance = new SymbolInt(e.distance);
		
		
		//Symbol dirEncodingGrandParent = e.parent==null || e.grandParent==null ? nullEvent : 
		//	new SymbolInt(Entity.leftOverlapsRight(e.parent, e.grandParent));
		//Symbol sisterAdj = adjacency(e.previousSisterSameSide, e.node) ? 
		//		new SymbolString("ADJ") : new SymbolString("NO_ADJ");
		
		Symbol[][] result = new Symbol[][]{
				{nodeEncoding1, parentEncoding, dirEncoding, dirParentEncoding, sisterEncoding1}, //parentDistance				
		};
		return result;
	}
	
	
	
	
	protected class BoxStructureExpansionEvent  extends Event {
		
		private static final long serialVersionUID = 0L;
		
		Box box;
		Entity[] tripletExpansion;
		
		public BoxStructureExpansionEvent(Box box, Entity[] tripletExpansion) {
			this.box = box;
			this.tripletExpansion = tripletExpansion;
		}
		
		public void storeEvent() {
			Symbol[][] eventEncoding = encodeBoxStructureExpansionEvent(this);
			boxExpansionModel.increaseInTables(eventEncoding);
		}
		
		public double getProb() {
			Symbol[][] eventEncoding = encodeBoxStructureExpansionEvent(this);
			return boxExpansionModel.getCondProb(eventEncoding);
			//return 1d;
		}
		
		@Override
		public boolean equals(Object otherObject) {
			if (otherObject==this) return true;
			if (otherObject instanceof BoxStructureExpansionEvent) {
				BoxStructureExpansionEvent otherBoxStructureExpansionEvent = 
					(BoxStructureExpansionEvent)otherObject;
				Symbol[][] thisEncoding = encodeBoxStructureExpansionEvent(this);
				Symbol[][] otherEncoding = encodeBoxStructureExpansionEvent(otherBoxStructureExpansionEvent);								
				return Arrays.deepEquals(thisEncoding, otherEncoding);
			}
			return false;
		}
		
		@Override
		public int[] getCondFreqEventFreq(int decompNumb , int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeBoxStructureExpansionEvent(this);
			return boxExpansionModel.getCondFreqEventFreq(thisEncodingLevel, decompNumb, backoffLevel);			
		}
		
		@Override
		public String encodingToStringFreq(int decompNumb , int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeBoxStructureExpansionEvent(this);
			return "\tExpansion\t" + 
			boxExpansionModel.getEventFreqToString(thisEncodingLevel, decompNumb, backoffLevel)
			+ "\t" + getProb();
		}
	
	
	}

	public void initBoxExpansionModel() {
		//P(expansion(box)) = P(elementsList |  box)
		int[][][] expansionBOL = new int[][][]{{{0,0}}};
		boolean[] expansionSkip = new boolean[]{false};
		int[][] expansionGroup = new int[][]{{1}};
		boxExpansionModel = new BackoffModel_DivHistory(expansionBOL, expansionSkip, expansionGroup, 0, 5, 0.000001); 		
	}
	
	public static SymbolString StdBlockExpansionEvent = new SymbolString("StandardBlockExpansion");
	
	public Symbol[][] encodeBoxStructureExpansionEvent(BoxStructureExpansionEvent e) {
		//P(elementsList |  box)
		//Box box;
		//TreeSet<Entity> elementsSet; (coord, box, punct)
	
		Symbol conditioningEvent = null;
		Symbol expansionEvent = null;
		
		//Symbol boxEncoding = boxCatsEmptyWordsEncoding(e.box, nullEvent);
		Entity[] triplet = e.tripletExpansion;
		
		Vector<Symbol> conditionalEventVector = new Vector<Symbol>();				
		//conditionalEventVector.add(boxEncoding);			
		for(int i=0; i<2; i++) {
			Entity element = triplet[i];
			if (element==null) conditionalEventVector.add(nullEvent);
			else {
				if (element.isBox()) conditionalEventVector.add(boxCatsEncoding((Box)element, nullEvent));
				else conditionalEventVector.add(new SymbolString(((Word)element).getLex())); 
			}
		}
		conditioningEvent = new SymbolList(conditionalEventVector);
		Entity expansionElement = triplet[2]; 
		if (expansionElement==null) expansionEvent = nullEvent;
		else {
			if (expansionElement.isBox()) expansionEvent = boxCatsEncoding((Box)expansionElement, stopEvent);
			else expansionEvent = new SymbolString(((Word)expansionElement).getLex()); 
		}
		
		Symbol[][] result = new Symbol[][]{{expansionEvent, conditioningEvent}};
		return result;
	}
	
	
	
	
	protected class FillStructureEvent extends Event {
		
		private static final long serialVersionUID = 0L;
		
		TreeSet<BoxStandard> parents;
		BoxStandard node;
		int dir;
		Box previousSistersSameSide;
		
		public FillStructureEvent(TreeSet<BoxStandard> parents, BoxStandard node, 
				int dir, Box previousSistersSameSide) {			
			this.parents = parents;
			this.node = node;
			this.dir = dir;
			this.previousSistersSameSide = previousSistersSameSide;
		}
		
		public void storeEvent() {
			Symbol[][][] eventEncodingJunction = encodeFillStructureEvent(this);
			for(Symbol[][] eventEncoding : eventEncodingJunction) {
				fillStructureModel.increaseInTables(eventEncoding);
			}
		}
		
		public double getProb() {
			Symbol[][][] eventEncodingJunction = encodeFillStructureEvent(this);
			//return fillStructureModel.getCondProbConjunct(eventEncodingJunction);
			return fillStructureModel.getCondProbAverage(eventEncodingJunction);			
		}
				
		public void reportEvent(BoxStandard parent, BoxStandard node, int dir, BoxStandard previousSisterSameSide) {
			System.out.println("Filling box: " +
					"\n\tParent: " + toStringCatsEmptyFullWords(parent) + 
					"\n\tDaughter: " + toStringCatsEmptyFullWords(node) +
					"\n\tDirection: " + leftInnerRight[dir+1] +				
					"\n\tLeft Sister same side: " + toStringCatsEmptyFullWords(previousSisterSameSide)				
			);		
		}
	
		@Override
		public boolean equals(Object otherObject) {
			if (otherObject==this) return true;
			if (otherObject instanceof FillStructureEvent) {
				FillStructureEvent otherFillStructureEvent = 
					(FillStructureEvent)otherObject;
				Symbol[][][] thisEncoding = encodeFillStructureEvent(this);
				Symbol[][][] otherEncoding = encodeFillStructureEvent(otherFillStructureEvent);								
				return Arrays.deepEquals(thisEncoding, otherEncoding);
			}
			return false;
		}
		
		@Override
		public int[] getCondFreqEventFreq(int decompNumb , int backoffLevel) {
			Symbol[][][] thisEncodingLevel = encodeFillStructureEvent(this);
			int[][] preResult = fillStructureModel.getCondFreqEventFreqConjunct(thisEncodingLevel, decompNumb, backoffLevel);
			int size = preResult.length;
			int[] result = new int[2*size];
			int index = 0;
			while(index<size) {
				int newIndex = index*2;
				result[newIndex++] = preResult[index][0];
				result[newIndex] = preResult[index][1];
			}
			return result;
		}
		
		@Override
		public String encodingToStringFreq(int decompNumb , int backoffLevel) {
			Symbol[][][] thisEncodingLevel = encodeFillStructureEvent(this);
			String[] preResult =  fillStructureModel.getEventFreqToStringConjunct(thisEncodingLevel, decompNumb, backoffLevel);
			int size = preResult.length;
			if (size==1) return "\tFilling \t" + preResult[0] + "\t" + getProb();
			StringBuilder result = new StringBuilder("{");
			for(int i=0; i<size; i++) {				
				result.append("\n\tFilling \t" + preResult[i] + "\t" + getProb());
			}
			result.append("\n}");
			return result.toString();
		}
		
	}

	public void initFillStructureModel() {
		//P(filling) =  P(contentNode | node parent dir sister)
		int[][][] fillingBOL = new int[][][]{{{0,0,0,0,0}}};
		//int[][][] fillingBOL = new int[][][]{{{0,0,0,0,0,0,0},{1,0,1,0,0,0,0}}}; //1,0,1,0,0
		boolean[] fillingSkip = new boolean[]{false};
		int[][] fillingGroup = new int[][]{{1,1}};
		fillStructureModel = new BackoffModel_DivHistory(fillingBOL, fillingSkip, fillingGroup, 0, 5, 0.000001);
	}
	
	public Symbol[][][] encodeFillStructureEvent(FillStructureEvent e) {
		//P(contentNode | node parent grandParent dir sister distance )
		//TreeSet<BoxStandard> parents;
		//BoxStandard node;
		//int dir;
		//TreeSet<BoxStandard> previousSistersSameSide;		
		
		BoxStandard node = e.node;
		Symbol nodeStructureEncoding = boxCatsEncoding(node, nullEvent);
		//Symbol nodeContentEncoding1 = boxStdFullWordsPosEncoding(node);
		Symbol nodeContentEncoding2 = boxStdFullPosEncoding(node);
		Symbol dirEncoding = new SymbolInt(e.dir);		
		
		TreeSet<BoxStandard> parents = e.parents;
		//TreeSet<BoxStandard> previousSistersSameSide = e.previousSistersSameSide;
		
		if (parents==null) {
			Symbol parentEncoding = nullEvent;
			//Symbol grandParentEncoding = nullEvent;
			Symbol sisterEncoding = nullEvent;
			Symbol[][][] result = new Symbol[][][]{{
				{nodeContentEncoding2, nodeStructureEncoding, parentEncoding, dirEncoding, sisterEncoding},
				//{nodeContentEncoding1, nodeStructureEncoding, parentEncoding, grandParentEncoding, dirEncoding, sisterEncoding, emptyEvent},
				//{nodeContentEncoding2, emptyEvent, emptyEvent, emptyEvent, emptyEvent, emptyEvent, emptyEvent}
			}};
			return result;			
		}		
		Symbol sisterEncoding = boxDerivedCatEncoding(e.previousSistersSameSide);
		int size = parents.size();
		Symbol[][][] result = new Symbol[size][][];
		int index = 0;
		for(BoxStandard p : parents) {
			//Symbol parentEncoding1 = boxStdFullWordsPosEncoding(p);
			Symbol parentEncoding2 = boxStdFullPosEncoding(p);
			//Symbol grandParentEncoding = boxOrigCatEncoding(p.parent);
			//Symbol nodeParentDistance = new SymbolInt(wordDistance(node, p));
			result[index++] = new Symbol[][]{
				{nodeContentEncoding2, nodeStructureEncoding, parentEncoding2, dirEncoding, sisterEncoding},
				//{nodeContentEncoding1, nodeStructureEncoding, parentEncoding1, grandParentEncoding, dirEncoding, sisterEncoding, nodeParentDistance},
				//{nodeContentEncoding2, emptyEvent, parentEncoding2, emptyEvent, emptyEvent, emptyEvent, emptyEvent}
			};				
		}
		return result;
	}
	
	public static ArrayList<TSNodeLabel> nextNBest(int nBest, Scanner s) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(nBest);
		int count = 0;
		while(s.hasNextLine() && count<nBest) {
			String line = s.nextLine();
			if (line.equals("")) return result;
			if (line.startsWith("vitprob=")) continue;
			result.add(new TSNodeLabel(line));
			count++;
		}
		while(s.hasNextLine() && !s.nextLine().equals("")) {};
		return result;
	}
	
	
	public static void printTables() throws Exception{
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_30_06_10"));
		ArrayList<Box> trainingTreebank = Conversion.getTesniereTreebank(trainingSet, HL);

		ProbModelStructureFiller_new PME = new ProbModelStructureFiller_new(trainingTreebank);
		PME.initModels();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String outputDir = baseDir + "rerank_new/";
		File tableFile = new File(outputDir + "tables.txt");
		PrintStream ps = new PrintStream(tableFile);
		PME.reportModelsTables(ps);
		ps.close();
	}
	
	
	public static void rerank(int nBest) throws Exception {
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_30_06_10"));
		ArrayList<Box> trainingTreebank = Conversion.getTesniereTreebank(trainingSet, HL);

		ProbModelStructureFiller_new PME = new ProbModelStructureFiller_new(trainingTreebank);
		PME.initModels();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String outputDir = baseDir + "rerank_new/";
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");
		//File oneBestFile = new File(baseDir + "wsj-22_oneBest_ADV.mrg");
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");
		
		/*String outputDir = Parameters.resultsPath + "TSG/CFG/Nbest/TDS_Rerank/";		
		File goldFile = new File(outputDir + "wsj-22_TOP.mrg");
		File nBestFile = new File(outputDir + "BITPAR_OUTPUT_POSTPROCESSED.txt");*/
		
		/*String outputDir = Parameters.resultsPath + "TSG/PCFG_Stanford_1000best/";		
		File goldFile = new File(outputDir + "wsj-22_TOP.mrg");
		File nBestFile = new File(outputDir + "wsj-22_PCFG_stanford_best1000_clean.mrg");*/
		
		
		File rerankedFile = new File(outputDir + "wsj-22_reranked_" + nBest + "best.mrg");
		File rerankedFileEvalF = new File(outputDir + "wsj-22_reranked_" + nBest + "best.evalF");
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFile);
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);		
				
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();		
		
		int size = goldTreebank.size();
		int[] depScoreReranked = new int[]{0,0,0};
		int[] depScoreOneBest = new int[]{0,0,0};
		int activelyReranked = 0;
		
		for(int i=0; i<size; i++) {			
			ArrayList<TSNodeLabel> nBestTrees = nextNBest(nBest, nBestScanner);
			TSNodeLabel gold = goldIter.next();
			Box goldTDS = Conversion.getTesniereStructure(gold.clone(), HL);			
			TSNodeLabel bestReranked = null;			
			double maxProb = -1;
			boolean first = true;
			int[] evalDepOneBest = null;
			TSNodeLabel oneBest = null;
			
			boolean reranked = false;
			for(TSNodeLabel t : nBestTrees) {				
				TSNodeLabel tOriginal = t.clone();
				Box b = Conversion.getTesniereStructure(t, HL);
				double p = PME.getProb(b);
				if (first) {					
					first = false;
					oneBest = tOriginal;
					evalDepOneBest = EvalTDS.getCatRawDepMatch(goldTDS, b);					
					Utility.arrayIntPlus(evalDepOneBest, depScoreOneBest);
					maxProb = p;
					bestReranked = tOriginal;
					continue;
				}
				//if (!t.samePos(oneBest)) continue;												
				if (p>maxProb) {
					maxProb = p;
					bestReranked = tOriginal;
					reranked = true;
				}
			}
			
			if (reranked) activelyReranked++;
			
			pw.println(bestReranked.toString());		
			//float rerankedFScore = EvalC.getScore(gold, bestReranked.clone(), true);
			//float oneBestFScore = EvalC.getScore(gold, oneBest.clone(), true);
			
			Box bestRerankedTDS = Conversion.getTesniereStructure(bestReranked, HL);
			int[] evalDepReranked = EvalTDS.getCatRawDepMatch(goldTDS, bestRerankedTDS);
			Utility.arrayIntPlus(evalDepReranked, depScoreReranked);			
									
			/*float diffFScore = rerankedFScore-oneBestFScore;
			int[] diffDep = new int[]{evalDepReranked[0], evalDepReranked[1]-evalDepOneBest[1],
					evalDepReranked[2]-evalDepOneBest[2]};
			System.out.println((i+1) + "\t" + diffFScore + "\t" + Arrays.toString(diffDep));*/
		}
		pw.close();
		
		System.out.println("Actively Reranked: " + activelyReranked);
		float[] rerankedFScore = EvalC.staticEvalC(goldFile, rerankedFile, rerankedFileEvalF);		
		System.out.println("One Best Dep Total Raw Dep: " + Arrays.toString(depScoreOneBest));
		System.out.println("Reranked Dep Total Raw Dep: " + Arrays.toString(depScoreReranked));
		System.out.println("Reranked Recall Precision FScore: " + Arrays.toString(rerankedFScore));
		EvalTDS evalTDS = new EvalTDS(rerankedFile, goldFile);
		evalTDS.compareStructures();
		float[] TDSresults = evalTDS.getResults();
		System.out.println("F-Score\tUAS\tBDS\tBAS\tJDS");
		System.out.println(rerankedFScore[2]/100 + "\t" + TDSresults[0] + "\t" + TDSresults[1] + "\t" + TDSresults[2] + "\t" + TDSresults[3]);
	}

	public static void printEventsFromStructure() throws Exception {
		TSNodeLabel structure = new TSNodeLabel("(S (PP-LOC (IN Under) (NP (NP (DT the) (NNS stars) (CC and) (NNS moons) ) (PP (IN of) (NP (DT the) (VBN renovated) (NNP Indiana) (NNP Roof) (NN ballroom) )))) (, ,) (NP-SBJ (NP (CD nine) ) (PP (IN of) (NP (NP (DT the) (JJS hottest) (NNS chefs) ) (PP (IN in) (NP (NN town) ))))) (VP (VBD fed) (NP (PRP them) ) (NP (NP (NNP Indiana) (NN duckling) (NN mousseline) ) (, ,) (NP (NN lobster) (NN consomme) ) (, ,) (NP (NN veal) (NN mignon) ) (CC and) (NP (NP (JJ chocolate) (NN terrine) ) (PP (IN with) (NP (DT a) (NN raspberry) (NN sauce) ))))) (. .) )");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_30_06_10"));
		Box b = Conversion.getTesniereStructure(structure, HL);
		ProbModelStructureFiller_new PME = new ProbModelStructureFiller_new(null);
		PME.initModels();
		ArrayList<Event> events = new ArrayList<Event>(); 
		PME.extractEventsFromStructure(b, events);
		for(Event e : events) {
			System.out.println(e.encodingToStringFreq(0, 0));
		}		
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//printTables();
		int[] nBest = new int[]{100};
		for(int n: nBest) {
			System.out.println("n = " + n);
			rerank(n);		
		}
		//printEventsFromStructure();
	}
	

}
