package tesniere;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.TreeSet;

import backoff.BackoffModel;
import backoff.BackoffModel_Eisner;

import symbols.Symbol;
import tesniere.ProbModel.Event;

public abstract class ProbModelStructureFiller extends ProbModel {		
	
	BackoffModel posTaggingModel;
	BackoffModel boxBoundaryModel;
	
	BackoffModel structureRelationModel;	
	BackoffModel boxExpansionModel;
	BackoffModel fillStructureModel;
	
	public ProbModelStructureFiller(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);
	}	
	
	public ProbModelStructureFiller() {		
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
		eventsCollector.add(new BoxStructureRelationEvent(null, null, b, -1, null, 0));
		extractStructure(null, b, eventsCollector);
		
		int sentenceLength = b.countAllWords();
		IdentityHashMap<Word, Box> wordBoxMapping = new IdentityHashMap<Word, Box>();		
		Word[] wordsArray = new Word[sentenceLength];				
		b.fillWordArrayTable(wordsArray, wordBoxMapping);
		
		//exractPostaggingAndBoundariesEvent(wordsArray, wordBoxMapping, sentenceLength, eventsCollector);
		
	}
	
	
	private void exractPostaggingAndBoundariesEvent(Word[] wordsArray,
			IdentityHashMap<Word, Box> wordBoxMapping, int sentenceLength,
			ArrayList<Event> eventsCollector) {
		
		Word[] wordsArray_headTail = new Word[sentenceLength+4];
		for(int i=0; i<sentenceLength; i++) {
			wordsArray_headTail[i+2] = wordsArray[i];			
		}
						
		for(int i=0; i<sentenceLength; i++) {
			Word[] fivegrams = new Word[]{null, null, null, null, null};
			for(int j=0; j<5; j++) {
				fivegrams[j] =wordsArray_headTail[i+j];
			}
			PosTaggingEvent pte = new PosTaggingEvent(fivegrams);
			eventsCollector.add(pte);
			
			Box middleWordBox = wordBoxMapping.get(fivegrams[2]);
			Box previous = fivegrams[1]==null ? null : wordBoxMapping.get(fivegrams[1]);
			boolean isBoundary = (middleWordBox!=previous);
			//Box next = fivegrams[3]==null ? null : wordBoxMapping.get(fivegrams[3]);
			//boolean isBoundary = (middleWordBox!=previous || middleWordBox!=next);
			BoxBoundaryEvent bbe = new BoxBoundaryEvent(fivegrams, isBoundary); 
			eventsCollector.add(bbe);
		}
	}

	
	
	/*private void extractStructure(Box gp, Box p, ArrayList<Event> eventsCollector) {						
		boolean[] observedEventsDir = new boolean[]{false, false, false};
		if (p.dependents!=null) {			
			int previousDir = -2;
			Box previousNodeSameSide = null;		
			for(Box d : p.dependents) {				
				extractStructure(p, d, eventsCollector);
				int dir = Entity.leftOverlapsRight(d, p); // -1 0 1
				if (previousDir==dir) {
					BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, d, dir, previousNodeSameSide); 
					eventsCollector.add(e);						
				}
				else {
					if (previousDir!=-2) {
						BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, null, previousDir, previousNodeSameSide); 
						eventsCollector.add(e);										
					}
					eventsCollector.add(new BoxStructureRelationEvent(gp, p, d, dir, null));
				}
				observedEventsDir[dir+1] = true;
				previousDir = dir;
				previousNodeSameSide = d;				
			}
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, null, previousDir, previousNodeSameSide);
			eventsCollector.add(e);			
		}
		for(int i=-1; i<2; i++) {
			if (!observedEventsDir[i+1]) {
				BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, null, i, null);
				eventsCollector.add(e);
			}
		}	
		doTheExpansionAndFilling(gp, p, eventsCollector);
	}*/
	 
	
	
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
		int ldi = leftDep.size();
		for(Box ld : leftDep) {
			ldi--;
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, ld, -1, previousLD, ldi);
			eventsCollector.add(e);
			previousLD = ld;			
		}
		eventsCollector.add(new BoxStructureRelationEvent(gp, p, null, -1, previousLD, 0)); //STOP
		
		Box previousID = null;
		for(Box id : innerDep) {
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, id, 0, previousID, 0);
			eventsCollector.add(e);
			previousID = id;
		}
		eventsCollector.add(new BoxStructureRelationEvent(gp, p, null, 0, previousID, 0)); //STOP
		
		Box previousRD = null;
		int rdi = 0;
		for(Box rd : rightDep) {
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, rd, 1, previousRD, rdi);
			eventsCollector.add(e);
			previousRD = rd;
			rdi++;
		}
		eventsCollector.add(new BoxStructureRelationEvent(gp, p, null, 1, previousRD, rdi)); //STOP

		doTheExpansionAndFilling(gp, p, eventsCollector);
	}

	// two directions
	/*private void extractStructure(Box gp, Box p, ArrayList<Event> eventsCollector) {
	 							
	 	TreeSet<Box> leftDep = new TreeSet<Box>();
		TreeSet<Box> rightDep = new TreeSet<Box>();
		if (p.dependents!=null) {						
			for(Box d : p.dependents) {
				extractStructure(p, d, eventsCollector);
				if (leftRight(d,p)==-1) leftDep.add(d);
				else rightDep.add(d);
			}
		}
		
		Box previousLD = null;
		for(Box ld : leftDep) {
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, ld, -1, previousLD);
			eventsCollector.add(e);
			previousLD = ld;
		}
		eventsCollector.add(new BoxStructureRelationEvent(gp, p, null, -1, previousLD)); //STOP
		Box previousRD = null;
		for(Box rd : rightDep) {
			BoxStructureRelationEvent e = new BoxStructureRelationEvent(gp, p, rd, 1, previousRD);
			eventsCollector.add(e);
			previousRD = rd;
		}
		eventsCollector.add(new BoxStructureRelationEvent(gp, p, null, 1, previousRD)); //STOP
		
		doTheExpansionAndFilling(gp, p, eventsCollector);
	}*/
	
	private void doTheExpansionAndFilling(Box gp, Box b, ArrayList<Event> eventsCollector) {
		TreeSet<Entity> elementsSet = new TreeSet<Entity>();
		if (b.ewList!=null) elementsSet.addAll(b.ewList);
		if (b.isJunctionBlock()) {
			BoxJunction bJun = (BoxJunction)b;												
			if (bJun.conjuncts!=null) {
				elementsSet.addAll(bJun.conjuncts);
				for(Box c : bJun.conjuncts) {
					extractStructure(gp, c, eventsCollector);
				}
			}
			if (bJun.conjunctions!=null) elementsSet.addAll(bJun.conjunctions);
			//if (bJun.punctList!=null) elementsSet.addAll(bJun.punctList);			
			//Entity firstElement = elementsSet.first();
			//Entity lastElement = elementsSet.last();
			//if (firstElement.isPunctWord()) elementsSet.remove(firstElement);
			//if (lastElement.isPunctWord()) elementsSet.remove(lastElement);			
			BoxStructureExpansionEvent e = new BoxStructureExpansionEvent(b, elementsSet);
			eventsCollector.add(e);
		}
		else {
			BoxStandard bStd = (BoxStandard)b;
			if (bStd.fwList!=null) elementsSet.addAll(bStd.fwList);
			//if (bStd.ewList!=null) elementsSet.addAll(bStd.ewList);
			//elementsSet.add(bStd);
			BoxStructureExpansionEvent e = new BoxStructureExpansionEvent(b, elementsSet); 
			eventsCollector.add(e);
			fillStructure(bStd, eventsCollector);
		}
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
	
	/*private TreeSet<BoxStandard> getSameSideSistersStd(Box gov, Box daughter) {		
		Box previousBox = null;		
		for(Box d : gov.dependents) {			
			if (d==daughter) {			 
				if (previousBox==null) return null;				
				if (previousBox.isJunctionBlock()) {					
					TreeSet<BoxStandard> previousSisters = getConjunctsStd((BoxJunction)previousBox);
					if (!sameSide(gov, previousSisters.first(), daughter)) return null;
					return previousSisters;
				}
				BoxStandard previousSister = (BoxStandard)previousBox;
				if (!sameSide(gov, previousSister, daughter)) return null;
				TreeSet<BoxStandard> result = new TreeSet<BoxStandard>();
				result.add(previousSister);
				return result;
			}
			previousBox = d;
		}
		return null;
	}*/	
	
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
	
	
	public abstract Symbol[][] encodeBoxBoundaryEvent(BoxBoundaryEvent e);
	public abstract Symbol[][] encodePosTaggingEvent(PosTaggingEvent e);
	public abstract Symbol[][] encodeBoxStructureRelationEvent(BoxStructureRelationEvent e);
	public abstract Symbol[][] encodeBoxStructureExpansionEvent(BoxStructureExpansionEvent e);
	public abstract Symbol[][][] encodeFillStructureEvent(FillStructureEvent e);
	
	
	
	
	
	
	
	
	
	protected class PosTaggingEvent  extends Event {
		
		private static final long serialVersionUID = 0L;
		
		Word[] fivegrams;
		
		public PosTaggingEvent(Word[] fivegrams) {
			this.fivegrams = fivegrams;
		}

		@Override
		public String encodingToStringFreq(int decompNumb, int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodePosTaggingEvent(this);
			return "\tExpansion\t" + 
			posTaggingModel.getEventFreqToString(thisEncodingLevel, decompNumb, backoffLevel)
			+ "\t" + getProb();
		}

		@Override
		public boolean equals(Object otherObject) {
			if (otherObject==this) return true;
			if (otherObject instanceof PosTaggingEvent) {
				PosTaggingEvent otherPosTaggingEvent = 
					(PosTaggingEvent)otherObject;
				Symbol[][] thisEncoding = encodePosTaggingEvent(this);
				Symbol[][] otherEncoding = encodePosTaggingEvent(otherPosTaggingEvent);								
				return Arrays.deepEquals(thisEncoding, otherEncoding);
			}
			return false;
		}

		@Override
		public int[] getCondFreqEventFreq(int decompNumb, int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodePosTaggingEvent(this);
			return posTaggingModel.getCondFreqEventFreq(thisEncodingLevel, decompNumb, backoffLevel);
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




	protected class BoxBoundaryEvent  extends Event {
		
		private static final long serialVersionUID = 0L;
		
		Word[] fivegrams;
		boolean isBoundary;
		
		public BoxBoundaryEvent(Word[] fivegrams, boolean isBoundary) {
			this.fivegrams = fivegrams;
			this.isBoundary = isBoundary;
		}
	
		@Override
		public String encodingToStringFreq(int decompNumb, int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeBoxBoundaryEvent(this);
			return "\tExpansion\t" + 
			boxBoundaryModel.getEventFreqToString(thisEncodingLevel, decompNumb, backoffLevel)
			+ "\t" + getProb();
		}
	
		@Override
		public boolean equals(Object otherObject) {
			if (otherObject==this) return true;
			if (otherObject instanceof BoxBoundaryEvent) {
				BoxBoundaryEvent otherBoxBoundaryEvent = 
					(BoxBoundaryEvent)otherObject;
				Symbol[][] thisEncoding = encodeBoxBoundaryEvent(this);
				Symbol[][] otherEncoding = encodeBoxBoundaryEvent(otherBoxBoundaryEvent);								
				return Arrays.deepEquals(thisEncoding, otherEncoding);
			}
			return false;
		}
	
		@Override
		public int[] getCondFreqEventFreq(int decompNumb, int backoffLevel) {
			Symbol[][] thisEncodingLevel = encodeBoxBoundaryEvent(this);
			return boxBoundaryModel.getCondFreqEventFreq(thisEncodingLevel, decompNumb, backoffLevel);
		}
	
		@Override
		public double getProb() {
			Symbol[][] eventEncoding = encodeBoxBoundaryEvent(this);
			return boxBoundaryModel.getCondProb(eventEncoding);
		}
	
		@Override
		public void storeEvent() {
			Symbol[][] eventEncoding = encodeBoxBoundaryEvent(this);
			boxBoundaryModel.increaseInTables(eventEncoding);			
		}
		
	}

	
	
	protected class BoxStructureRelationEvent extends Event {
		
		private static final long serialVersionUID = 0L;
		
		Box grandParent;
		Box parent;		
		Box node;
		int dir;
		Box previousSisterSameSide;
		int distance;
		
		public BoxStructureRelationEvent(Box grandParent, Box parent, Box node, 
				int dir, Box previousSisterSameSide, int distance) {
			this.grandParent = grandParent;
			this.parent = parent;
			this.node = node;
			this.dir = dir;
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

	
	
	protected class BoxStructureExpansionEvent  extends Event {
		
		private static final long serialVersionUID = 0L;
		
		Box box;
		TreeSet<Entity> elementsSet;
		
		public BoxStructureExpansionEvent(Box box, TreeSet<Entity> elementsSet) {
			this.box = box;
			this.elementsSet = elementsSet;
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
	

}
