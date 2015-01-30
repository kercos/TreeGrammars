package tesniere;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import symbols.Symbol;
import symbols.SymbolInt;
import symbols.SymbolList;
import symbols.SymbolString;
import tesniere.ProbModel.Event;
import tesniere.ProbModelStructureFiller.BoxBoundaryEvent;
import tsg.HeadLabeler;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.Utility;
import util.file.FileUtil;
import wordModel.AffixSuffixManual;
import backoff.BackoffModel_DivHistory;
import backoff.BackoffModel_Eisner;

public class ProbModelStructureFiller_adv extends ProbModelStructureFiller implements Serializable {	
	
	private static final long serialVersionUID = 0L;
	
	public ProbModelStructureFiller_adv(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);		
	}
	
	public ProbModelStructureFiller_adv() {
		super();
	}
		
	public void initModelDivHistory() {
		
		//P(boxBoundary(fivegrams)) = P(B | cwL cwP ppwL ppwP pwL pwP nwL nwP nnwL nnwP)
													// B |  cwL  cwP ppwL ppwP pwL pwP nwL nwP nnwL nnwP
		int[][][] boxBoundaryBOL = new int[][][]{{	{  0,    0,   0,  1,   0,   0,  0,  0,  0,  1,   0  },
													{  0,    0,   0,  -1,   0,   1,  0,  1,  0,  -1,   0  },
													{  0,    -1,   0,  -1,   0,   -1,  0,  -1,  0,  -1,   0  }}};
		boolean[] boxBoundarySkip = new boolean[]{false};
		int[][] boxBoundaryGroup = new int[][]{{1,1,1}};		
		boxBoundaryModel = new BackoffModel_DivHistory(boxBoundaryBOL, boxBoundarySkip, boxBoundaryGroup, 0, 50, 0.000001);
		
		
		//P(boxBoundary(fivegrams)) = P(cwP | cwL ppwL ppwP pwL pwP nwL nwP nnwL nnwP)
													// cwP |   cwL  ppwL ppwP pwL pwP nwL nwP nnwL nnwP
		int[][][] posTaggerBOL = new int[][][]{{	{   0,      0,   0,   0,  0,  0,  0,  -1,   1,   -1  },
													{   0,      0,   -1,   0,  1,  0,  1,  -1,   -1,   -1  },
													{   0,      -1,   -1,   0,  -1,  0,  -1,  -1,   -1,   -1  } }};
		boolean[] posTaggerSkip = new boolean[]{false};
		int[][] posTaggerGroup = new int[][]{{1,1,1}};		
		posTaggingModel = new BackoffModel_DivHistory(posTaggerBOL, posTaggerSkip, posTaggerGroup, 0, 50, 0.000001);
		
		//P(structure(box)) = P(node | grandParent parent dir sister)
		int[][][] structureBOL = new int[][][]{{{0,-1,0,0,0}}};
		boolean[] structureSkip = new boolean[]{false};
		int[][] structureGroup = new int[][]{{1}};		
		structureRelationModel = new BackoffModel_DivHistory(structureBOL, structureSkip, structureGroup, 0, 5, 0.000001); 		
		
		//P(expansion(box)) = P(elementsList |  box)
		int[][][] expansionBOL = new int[][][]{{{0,0}}};
		boolean[] expansionSkip = new boolean[]{false};
		int[][] expansionGroup = new int[][]{{1}};
		boxExpansionModel = new BackoffModel_DivHistory(expansionBOL, expansionSkip, expansionGroup, 0, 50, 0.000001); 
		
		//P(filling) =  P(contentNode | node parent grandParent dir sister distance)
		//int[][][] fillingBOL = new int[][][]{{{1,0,1,0,0,0,0}}};
		int[][][] fillingBOL = new int[][][]{{{0,0,0,0,0,0,0},{1,0,1,0,0,0,0}}}; //1,0,1,0,0
		boolean[] fillingSkip = new boolean[]{false};
		int[][] fillingGroup = new int[][]{{1,1}};
		fillStructureModel = new BackoffModel_DivHistory(fillingBOL, fillingSkip, fillingGroup, 0, 50, 0.000001);
		
	}
	
	public void initModelEisner() {
		//P(structure(box)) = P(node | grandParent parent dir sister)
		int[][][] structureBOL = new int[][][]{{{0,-1,0,0,0}}};
		boolean[] structureSkip = new boolean[]{false};
		int[][] structureGroup = new int[][]{{1}};		
		structureRelationModel = new BackoffModel_Eisner(structureBOL, structureSkip, structureGroup, 0.005, 0.5, 3); //P(node | parent dir sister)
				
		int[][][] expansionBOL = new int[][][]{{{0,0}}};
		boolean[] expansionSkip = new boolean[]{false};
		int[][] expansionGroup = new int[][]{{1}};
		boxExpansionModel = new BackoffModel_Eisner(expansionBOL, expansionSkip, expansionGroup, 0.005, 0.5, 3); //P(elementsList |  box)
		
		int[][][] fillingBOL = new int[][][]{{{0,0,0,0,0,0,0},{1,0,1,0,0,0,0},}}; //1,0,1,0,0
		boolean[] fillingSkip = new boolean[]{false};
		int[][] fillingGroup = new int[][]{{1,1}};
		fillStructureModel = new BackoffModel_Eisner(fillingBOL, fillingSkip, fillingGroup, 0.005, 0.5, 3); //P(contentNode | node parent dir sister )

	}
	
	
	
	public static ProbModelStructureFiller_adv fromBinaryFile(File inputFile) {
		ProbModelStructureFiller_adv model = null; 
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
			model = (ProbModelStructureFiller_adv) in.readObject();
		} catch (Exception e) {FileUtil.handleExceptions(e);}
		return model;
	}
	
	public void toBinaryFile(File outputFile) {
		try{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
			out.writeObject(this);
		} catch (Exception e) {FileUtil.handleExceptions(e);}		
	}
	
	/*public static int boxDistance(Box parent, Box daughter, int direction) {
		if (direction==0) return 0;
		if (direction==-1) {
			int count = 0;
			boolean started = false;
			for(Box d : parent.dependents) {
				if (d==daughter) started = true;
				if (Entity.leftOverlapsRight(d, parent)!=-1) return count; 
				if (started) count++;				
			}
		}
		else {
			
		}
	}*/
	
	public static boolean adjacency(Box b1, Box b2) {
		if (b1==null || b2==null) return false;
		int b1L = b1.leftMostWord(false, false).position;
		int b1R = b1.rightMostWord(false, false).position;
		int b2L = b2.leftMostWord(false, false).position;
		int b2R = b2.rightMostWord(false, false).position;
		return Math.abs(b1L-b2R)==1 || Math.abs(b1R-b2L)==1;
		
	}
	
	public static SymbolString emptyEvent = new SymbolString("{}");
	public static SymbolString nullEvent = new SymbolString("*NULL_EVENT*");
	
	public String getWordFeatures(String word) {
		return AffixSuffixManual.tagWord(word) + " || "+ new WordFeatures(word).toFeatureVector();		
	}
	
	public String getPosEmptyFull(Word w) {
		return w.getPos() + "_" + (w.isEmpty() ? "E" : "F");
	}

	@Override
	public Symbol[][] encodeBoxBoundaryEvent(BoxBoundaryEvent e) {
		Word[] fivegrams = e.fivegrams;
		
		Word ppw = fivegrams[0];
		Word pw = fivegrams[1];
		Word cw = fivegrams[2];
		Word nw = fivegrams[3];
		Word nnw = fivegrams[4];
		
		Symbol B = new SymbolInt(e.isBoundary ? 1 : 0);
		
		Symbol cwL = new SymbolString(cw.getLex());
		Symbol cwLF = new SymbolString(getWordFeatures(cw.getLex()));
		Symbol cwP = new SymbolString(getPosEmptyFull(cw));
		Symbol ppwL = ppw==null ? nullEvent : new SymbolString(ppw.getLex());
		Symbol ppwLF = ppw==null ? nullEvent : new SymbolString(getWordFeatures(ppw.getLex()));
		Symbol ppwP = ppw==null ? nullEvent : new SymbolString(getPosEmptyFull(ppw));
		Symbol pwL = pw==null ? nullEvent : new SymbolString(pw.getLex());
		Symbol pwLF = pw==null ? nullEvent : new SymbolString(getWordFeatures(pw.getLex()));
		Symbol pwP = pw==null ? nullEvent : new SymbolString(getPosEmptyFull(pw));
		Symbol nwL = nw==null ? nullEvent : new SymbolString(nw.getLex());
		Symbol nwLF = nw==null ? nullEvent : new SymbolString(getWordFeatures(nw.getLex()));
		Symbol nwP = nw==null ? nullEvent : new SymbolString(getPosEmptyFull(nw));
		Symbol nnwL = nnw==null ? nullEvent : new SymbolString(nnw.getLex());
		Symbol nnwLF = nnw==null ? nullEvent : new SymbolString(getWordFeatures(nnw.getLex()));
		Symbol nnwP = nnw==null ? nullEvent : new SymbolString(getPosEmptyFull(nnw));
		
		Symbol[][] result = new Symbol[][]{
				{B, cwL, cwP, ppwL, ppwP, pwL, pwP, nwL, nwP, nnwL, nnwP},
				{B, cwLF, emptyEvent, ppwLF, emptyEvent, pwLF, emptyEvent, nwLF, emptyEvent, nnwLF, emptyEvent}
		};
	
		return result;
	}

	@Override
	public Symbol[][] encodePosTaggingEvent(PosTaggingEvent e) {
		Word[] fivegrams = e.fivegrams;
		
		Word ppw = fivegrams[0];
		Word pw = fivegrams[1];
		Word cw = fivegrams[2];
		Word nw = fivegrams[3];
		Word nnw = fivegrams[4];
				
		Symbol cwP_EF = new SymbolString(getPosEmptyFull(cw));
		
		Symbol cwL = new SymbolString(cw.getLex());
		Symbol cwLF = new SymbolString(getWordFeatures(cw.getLex()));		
		Symbol ppwL = ppw==null ? nullEvent : new SymbolString(ppw.getLex());
		Symbol ppwLF = ppw==null ? nullEvent : new SymbolString(getWordFeatures(ppw.getLex()));
		Symbol ppwP = ppw==null ? nullEvent : new SymbolString(getPosEmptyFull(ppw));
		Symbol pwL = pw==null ? nullEvent : new SymbolString(pw.getLex());
		Symbol pwLF = pw==null ? nullEvent : new SymbolString(getWordFeatures(pw.getLex()));
		Symbol pwP = pw==null ? nullEvent : new SymbolString(getPosEmptyFull(pw));
		Symbol nwL = nw==null ? nullEvent : new SymbolString(nw.getLex());
		Symbol nwLF = nw==null ? nullEvent : new SymbolString(getWordFeatures(nw.getLex()));
		Symbol nwP = nw==null ? nullEvent : new SymbolString(getPosEmptyFull(nw));
		Symbol nnwL = nnw==null ? nullEvent : new SymbolString(nnw.getLex());
		Symbol nnwLF = nnw==null ? nullEvent : new SymbolString(getWordFeatures(nnw.getLex()));
		Symbol nnwP = nnw==null ? nullEvent : new SymbolString(getPosEmptyFull(nnw));
		
		Symbol[][] result = new Symbol[][]{
				{cwP_EF, cwL, ppwL, ppwP, pwL, pwP, nwL, nwP, nnwL, nnwP},
				{cwP_EF, cwLF, ppwLF, emptyEvent, pwLF, emptyEvent, nwLF, emptyEvent, nnwLF, emptyEvent}
		};

		return result;
	}

	@Override
	public Symbol[][] encodeBoxStructureRelationEvent(BoxStructureRelationEvent e) {
		//P(node | grandParent, parent dir sister)
		//Box grandParent;
		//Box parent;
		//Box node;
		//int dir;
		//Box previousSisterSameSide;
		
		Symbol grandParentEncoding = boxOrigCatEncoding(e.grandParent);
		Symbol parentEncoding = boxOrigCatEncoding(e.parent);
		Symbol nodeEncoding1 = boxCatsEmptyWordsPunctEncoding(e.node); 
		//Symbol nodeEncoding2 = boxDerivedCatPunctEncoding(e.node);
		Symbol dirEncoding = new SymbolInt(e.dir);
		Symbol sisterEncoding1 = boxCatsEmptyWordsPunctEncoding(e.previousSisterSameSide);
		//Symbol sisterEncoding2 = boxDerivedCatPunctEncoding(e.previousSisterSameSide);
		//Symbol parentDistance = new SymbolInt(e.distance);
		
		
		//Symbol dirEncodingGrandParent = e.parent==null || e.grandParent==null ? nullEvent : 
		//	new SymbolInt(Entity.leftOverlapsRight(e.parent, e.grandParent));
		//Symbol sisterAdj = adjacency(e.previousSisterSameSide, e.node) ? 
		//		new SymbolString("ADJ") : new SymbolString("NO_ADJ");
		
		Symbol[][] result = new Symbol[][]{
				{nodeEncoding1,grandParentEncoding, parentEncoding, dirEncoding, sisterEncoding1}, //parentDistance				
		};
		return result;
	}

	
	@Override
	public Symbol[][] encodeBoxStructureExpansionEvent(BoxStructureExpansionEvent e) {
		//P(elementsList |  box)
		//Box box;
		//TreeSet<Entity> elementsSet; (coord, box, punct)
	
		Symbol boxEncoding = boxDerivedCatEncoding(e.box);		
		
		TreeSet<Entity> elementsSet = e.elementsSet;
		Symbol elementsEncoding = null;
		if (elementsSet==null) elementsEncoding = nullEvent;
		else {
			Vector<Symbol> vectorEncoding = new Vector<Symbol>();			
			for(Entity elem : elementsSet) {			
				if (elem.isBox()) vectorEncoding.add(boxCatsEmptyWordsPunctEncoding((Box)elem));
				else {
					if (elem.isFunctionalWord()) {
						FunctionalWord ew = (FunctionalWord)elem;
						//if (ew.wordType==2) vectorEncoding.add(new SymbolString((ew).getLex()));
						//continue;						
						if (ew.isVerb()) continue;		// auxiliary										
						vectorEncoding.add(new SymbolString((ew).getLex()));
					}
					else {
						vectorEncoding.add(new SymbolString(((ContentWord)elem).getCatString()));
					}
				}
			}
			elementsEncoding = new SymbolList(vectorEncoding);
		}
		
		Symbol[][] result = new Symbol[][]{{elementsEncoding, boxEncoding}};
		return result;
	}
	
	
	@Override
	public Symbol[][][] encodeFillStructureEvent(FillStructureEvent e) {
		//P(contentNode | node parent grandParent dir sister distance )
		//TreeSet<BoxStandard> parents;
		//BoxStandard node;
		//int dir;
		//TreeSet<BoxStandard> previousSistersSameSide;		
		
		BoxStandard node = e.node;
		Symbol nodeStructureEncoding = boxDerivedCatEmptyWordsPunctEncoding(node);
		Symbol nodeContentEncoding1 = boxStdFullWordsPosEncoding(node);
		Symbol nodeContentEncoding2 = boxStdFullPosEncoding(node);
		Symbol dirEncoding = new SymbolInt(e.dir);		
		
		TreeSet<BoxStandard> parents = e.parents;
		//TreeSet<BoxStandard> previousSistersSameSide = e.previousSistersSameSide;
		
		if (parents==null) {
			Symbol parentEncoding = nullEvent;
			Symbol grandParentEncoding = nullEvent;
			Symbol sisterEncoding = nullEvent;
			Symbol[][][] result = new Symbol[][][]{{
				{nodeContentEncoding1, nodeStructureEncoding, parentEncoding, grandParentEncoding, dirEncoding, sisterEncoding, emptyEvent},
				{nodeContentEncoding2, emptyEvent, emptyEvent, emptyEvent, emptyEvent, emptyEvent, emptyEvent}
			}};
			return result;			
		}		
		Symbol sisterEncoding = boxDerivedCatEncoding(e.previousSistersSameSide);
		int size = parents.size();
		Symbol[][][] result = new Symbol[size][][];
		int index = 0;
		for(BoxStandard p : parents) {
			Symbol parentEncoding1 = boxStdFullWordsPosEncoding(p);
			Symbol parentEncoding2 = boxStdFullPosEncoding(p);
			Symbol grandParentEncoding = boxOrigCatEncoding(p.parent);
			Symbol nodeParentDistance = new SymbolInt(wordDistance(node, p));
			result[index++] = new Symbol[][]{
				{nodeContentEncoding1, nodeStructureEncoding, parentEncoding1, grandParentEncoding, dirEncoding, sisterEncoding, nodeParentDistance},
				{nodeContentEncoding2, emptyEvent, parentEncoding2, emptyEvent, emptyEvent, emptyEvent, emptyEvent}
			};				
		}
		return result;
	}
	
	public static int wordDistance(BoxStandard b1, BoxStandard b2) {
		if (b1.fwList==null || b2.fwList==null) return -1;
		return wordDistance(b1.fwList.first(), b2.fwList.first());
	}
	
	public static int wordDistance(Word w1, Word w2) {		
		return Math.abs(w1.position-w2.position);
		//int dist = Math.abs(w1.position-w2.position);
		//if (dist<4) return dist;
		//if (dist<6) return 5;
		//if (dist<9) return 8;
		//return 10;
		//return 10;
		//if (dist<6) return 1;
		//if (dist<9) return 2;
		//if (dist<12) return 3;
		//return 2;		
	}
	
	public static Symbol boxOrigCatEncoding(Box b) {
		if (b==null) return nullEvent;		
		return new SymbolString(b.getOriginalCatString());
	}
		
	public static Symbol boxDerivedCatEncoding(Box b) {
		if (b==null) return nullEvent;		
		return new SymbolString(b.getDerivedCatString());
	}
	
	public static Symbol boxDerivedCatPunctEncoding(Box b) {
		if (b==null) return nullEvent;		
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		if (b.punctList!=null) {
			FunctionalWord firstPunt = b.punctList.first();
			FunctionalWord lastPunt = b.punctList.last();
			for(FunctionalWord p : b.punctList) {				
				if (p==firstPunt) vectorEncoding.add(new SymbolString("_"+p.getLex()));
				else if (p==lastPunt) vectorEncoding.add(new SymbolString(p.getLex()+"_"));
				else vectorEncoding.add(new SymbolString(p.getLex()));
			}			
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxOriginalCatPunctEncoding(Box b) {
		if (b==null) return nullEvent;		
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		if (b.punctList!=null) {
			FunctionalWord firstPunt = b.punctList.first();
			FunctionalWord lastPunt = b.punctList.last();
			for(FunctionalWord p : b.punctList) {				
				if (p==firstPunt) vectorEncoding.add(new SymbolString("_"+p.getLex()));
				else if (p==lastPunt) vectorEncoding.add(new SymbolString(p.getLex()+"_"));
				else vectorEncoding.add(new SymbolString(p.getLex()));
			}			
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxCatsEncoding(Box b) {
		if (b==null) return nullEvent;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxCatsFirstEmptyWordEncoding(Box b) {
		if (b==null) return nullEvent;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		if (b.ewList!=null) vectorEncoding.add(new SymbolString(b.ewList.first().getLex()));
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxCatsEmptyWordsEncoding(Box b) {
		if (b==null) return nullEvent;
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
	
	public static Symbol boxOrigLabelWordsEncoding(Box b) {
		if (b==null) return nullEvent;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.originalTSNodeLabel.toStringWithoutSemTags()));
		if (b.ewList!=null) {
			for(FunctionalWord ew : b.ewList) {
				vectorEncoding.add(new SymbolString(ew.getLex()));
			}
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxCatsEmptyWordsNonVerbEncoding(Box b) {
		if (b==null) return nullEvent;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();
		vectorEncoding.add(new SymbolString(b.getOriginalCatString()));
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		if (b.ewList!=null) {
			for(FunctionalWord ew : b.ewList) {
				if (ew.isVerb()) continue;
				vectorEncoding.add(new SymbolString(ew.getLex()));
			}
		}
		return new SymbolList(vectorEncoding);
	}
	
	public static Symbol boxDerivedCatEmptyWordsEncoding(Box b) {
		if (b==null) return nullEvent;
		Vector<Symbol> vectorEncoding = new Vector<Symbol>();		
		vectorEncoding.add(new SymbolString(b.getDerivedCatString()));
		if (b.ewList!=null) {
			for(FunctionalWord ew : b.ewList) {
				vectorEncoding.add(new SymbolString(ew.getLex()));
			}
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
		FunctionalWord firstEWP = emptyWordsPunt.isEmpty() ? null : emptyWordsPunt.first();
		FunctionalWord lastEWP = emptyWordsPunt.isEmpty() ? null : emptyWordsPunt.last();
		for (FunctionalWord ewp : emptyWordsPunt) {
			if (ewp==firstEWP) vectorEncoding.add(new SymbolString("_"+ewp.getLex()));
			else if (ewp==lastEWP) vectorEncoding.add(new SymbolString(ewp.getLex()+"_"));
			else vectorEncoding.add(new SymbolString(ewp.getLex()));
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
	
	public static void test_old() throws Exception {
		//File trainingSet = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		File sec00 = new File(Wsj.WsjOriginalCleaned + "wsj-00.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_22_01_10"));
		//ArrayList<Box> trainingTreebank = 
		//	Conversion.getTesniereTreebank(trainingSet, HL);
		ArrayList<Box> sec00Treebank = 
			Conversion.getTesniereTreebank(sec00, HL);		
		//ProbModelStructureFiller_1LB PME = new ProbModelStructureFiller_1LB(trainingTreebank);
		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv();
		
		//File binaryFileModel = new File("resources/ProbModelStructureFiller_1LB_training.bin");
		//PME.toBinaryFile(binaryFileModel);
		//ProbModelStructureFiller_1LB PME = fromBinaryFile(binaryFileModel);
		//PME.preprocessTrainig();
		//PME.train();
		Box b = sec00Treebank.get(551);
		PME.trainFromStructure(b);		
		PME.reportModelsTables(System.out);
		//double prob = PME.getProb(b, true);
		//System.out.println("\n\nTotal Prob: " + prob);
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
		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv(trainingTreebank);
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
			
			float fscoreOneBest = EvalC.getFScore(oneBestTree, goldTree, true);
			float fscoreOracleBest = EvalC.getFScore(oracleBestTree, goldTree, true);
			
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
			System.out.println("\n");		
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
			if (line.startsWith("vitprob=")) continue;
			result.add(new TSNodeLabel(line));
			count++;
		}
		while(s.hasNextLine() && !s.nextLine().equals("")) {};
		return result;
	}
	
	public static void printTables() throws Exception {
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_30_06_10"));
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);

		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv(trainingTreebank);
		//PME.initModelsSimple();
		//PME.initModelsLambda();
		//PME.initModelsBakoff();
		PME.initModelDivHistory();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String outputDir = baseDir + "rerank_adv/";
		File tableFile = new File(outputDir + "tables.txt");
		PrintStream ps = new PrintStream(tableFile);
		PME.reportModelsTables(ps);
		ps.close();
	}
	

	
	public static void getProbDistribution() throws Exception {
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_09_06_10"));
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);

		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv(trainingTreebank);
		PME.initModelDivHistory();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String outputDir = baseDir + "probDistr/";
		//File oneBestFile = new File(baseDir + "wsj-22_oneBest_ADV.mrg");
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		
		ArrayList<TSNodeLabel> nBestTrees = null;
		File outputTotalFile = new File(outputDir + "totalProb.txt");
		PrintWriter pwTotal = FileUtil.getPrintWriter(outputTotalFile);
		for(int i=0; i<1700; i++) {			
			nBestTrees = nextNBest(1000, nBestScanner);
			File outputSentenceFile = new File(outputDir + "sentence_" + Utility.padZero(4, i+1) + ".txt");
			PrintWriter pwSentence = FileUtil.getPrintWriter(outputSentenceFile);
			double sum = 0;
			Vector<Double> probVector = new Vector<Double>(1000);
			for(TSNodeLabel t : nBestTrees) {				
				Box b = Conversion.getTesniereStructure(t, HL);
				double p = PME.getProb(b);
				probVector.add(p);				
				sum += p;								
			}						
			Collections.sort(probVector);
			for(int j=probVector.size()-1; j>=0; j--) {
				pwSentence.println(probVector.get(j));
			}
			pwSentence.close();
			pwTotal.println(sum);
		}	
		pwTotal.close();		
	}
	
	public static void getProbGold() throws Exception {
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_09_06_10"));
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);

		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv(trainingTreebank);
		PME.initModelDivHistory();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);
		File outpuFile = new File(baseDir + "goldProb.txt");
		PrintWriter pw = FileUtil.getPrintWriter(outpuFile);
		for(TSNodeLabel t : goldTreebank) {
			Box b = Conversion.getTesniereStructure(t, HL);
			double p = PME.getProb(b);
			pw.println(p);
		}
		pw.close();		
	}

	
	public static void rerank(int nBest) throws Exception {
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_30_06_10"));
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);

		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv(trainingTreebank);
		//PME.initModelsSimple();
		//PME.initModelsLambda();
		//PME.initModelsBakoff();
		//PME.initModelDivHistory();
		PME.initModelEisner();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String outputDir = baseDir + "rerank_adv_eisner/";
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
	
	public static float ratio(int[] denNum) {
		return (float)denNum[1]/denNum[0];
	}
	
	public static void rerankDebug(int nBest) throws Exception {
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_03_02_10"));
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);
		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv(trainingTreebank);
		//PME.initModelsSimple();
		PME.train();
		
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		String baseDirSimple = baseDir + "rerank_simple/";
		
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");				
		File rerankedFile = new File(baseDirSimple + "wsj-22_reranked_" + nBest + "best.mrg");
		//File oracleBestFile = new File(baseDir + "wsj-22_oracleBest_" + nBest + ".mrg");
		File oracleBestFile = new File(baseDir + "wsj-22_oneBest_ADV.mrg");
		
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);
		ArrayList<TSNodeLabel> rerankedTreebank = TSNodeLabel.getTreebank(rerankedFile);
		ArrayList<TSNodeLabel> oracleBestTreebank = TSNodeLabel.getTreebank(oracleBestFile);
				
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();		
		Iterator<TSNodeLabel> rerankedIter = rerankedTreebank.iterator();
		Iterator<TSNodeLabel> oracleBestIter = oracleBestTreebank.iterator();
		
		int i=0;
		while(goldIter.hasNext()) {
			TSNodeLabel goldNext = goldIter.next();
			TSNodeLabel rerankedNext = rerankedIter.next();
			TSNodeLabel oracleBestNext = oracleBestIter.next();
			
			Box rerankedTDS = Conversion.getTesniereStructure(rerankedNext.clone(), HL);
			Box oracleBestTDS = Conversion.getTesniereStructure(oracleBestNext.clone(), HL);
												
			ArrayList<Event> oracleBestEvents = PME.getEventsList(oracleBestTDS);
			ArrayList<Event> rerankedEvents = PME.getEventsList(rerankedTDS);			
			
			ArrayList<Event> oracleBestEventsUnique = new ArrayList<Event>(oracleBestEvents);
			ArrayList<Event> rerankedEventsUnique = new ArrayList<Event>(rerankedEvents);
			Utility.removeAllOnce(oracleBestEventsUnique, rerankedEvents);
			Utility.removeAllOnce(rerankedEventsUnique, oracleBestEvents);
			
			int[] oneBestUniqueTotal = new int[]{oracleBestEventsUnique.size(), oracleBestEvents.size()};
			int[] rerankedUniqueTotal = new int[]{rerankedEventsUnique.size(), rerankedEvents.size()};
			
			double rerankedProb = getProb(rerankedEvents);														
			double oracleBestProb = getProb(oracleBestEvents);
			
			double probRatio = rerankedProb / oracleBestProb; 			
			
			float oracleBestFScore = EvalC.getFScore(goldNext, oracleBestNext, true);
			float rerankedFScore = EvalC.getFScore(goldNext, rerankedNext, true);
									
			float diffFscore = rerankedFScore - oracleBestFScore;
			
			System.out.println("Sentence " + (++i) + 
					"\n" + "Oracle Best FScore, Prob\t" + oracleBestFScore  + "\t" + oracleBestProb + 
					"\n" + "Reranked Best FScore, Prob\t" +	rerankedFScore + "\t" + rerankedProb +
					"\n" + "FScore Diff, Prob Ratio\t" + 	diffFscore + "\t" + probRatio +
					"\n-----");
						
			System.out.println("Oracle Best Unique/Total: " + Arrays.toString(oneBestUniqueTotal));
			for(Event e : oracleBestEventsUnique) {
				System.out.println(e.encodingToStringFreq(0,0));
			}
			System.out.println("Reranked Unique/Total: " + Arrays.toString(rerankedUniqueTotal));
			for(Event e : rerankedEventsUnique) {
				System.out.println(e.encodingToStringFreq(0,0));
			}
			System.out.println("\n\n");
			
			//if (i==100) break;
		}		
	}
	
	public static void test() throws Exception {
		//File trainingSet = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		//File sec00 = new File(Wsj.WsjOriginalCleaned + "wsj-00.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_03_02_10"));
		//ArrayList<Box> trainingTreebank = 
		//	Conversion.getTesniereTreebank(trainingSet, HL);
		//ArrayList<Box> sec00Treebank = Conversion.getTesniereTreebank(sec00, HL);		
		//ProbModelStructureFiller_1LB PME = new ProbModelStructureFiller_1LB(trainingTreebank);
		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv();
		//PME.initModelsSimple();
		
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
		
		TSNodeLabel oneBest = new TSNodeLabel( "(TOP (S (NP (DT The) (NN bill)) (VP (VBZ intends) (S (VP (TO to) (VP (VB restrict) (NP (DT the) (NNP RTC)) (PP-ADV (TO to) (NP (NNP Treasury) (NNS borrowings))) (ADVP (RB only)) (, ,) (SBAR-ADV (IN unless) (S (NP (DT the) (NN agency)) (VP (VBZ receives) (NP (JJ specific) (JJ congressional) (NN authorization))))))))) (. .)))");
		TSNodeLabel reranked = new TSNodeLabel("(TOP (S (NP (DT The) (NN bill)) (VP (VBZ intends) (S (VP (TO to) (VP (VB restrict) (NP (DT the) (NNP RTC)) (PP (TO to) (NP (NNP Treasury) (NNS borrowings))) (ADVP (RB only)) (, ,) (SBAR (IN unless) (S (NP (DT the) (NN agency)) (VP (VBZ receives) (NP (JJ specific) (JJ congressional) (NN authorization))))))))) (. .)))");
		
		Box oneBestTDS = Conversion.getTesniereStructure(oneBest, HL);
		Box rerankedTDS = Conversion.getTesniereStructure(reranked, HL);
		
		ArrayList<Event> list = PME.getEventsList(rerankedTDS);
		for(Event e : list) {
			System.out.println(e.encodingToStringFreq(0,0));
		}
		
		/*System.out.println("\n------------------\n");
		
		list = PME.getEventsList(rerankedTDS);
		for(Event e : list) {
			System.out.println(e.encodingToStringFreq(0,0));
		}*/
		
	}
	
	public static void getOracleBestWorst() throws Exception {				
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");		
		File nBestFile = new File(baseDir + "wsj-22_chiarniak_parsed1000_clean_ADV.mrg");
		Scanner nBestScanner = FileUtil.getScanner(nBestFile);
		int[] nBest = new int[]{5,10,100,500,1000};
		File[] oracleBestFile = new File[nBest.length];
		File[] oracleWorstFile = new File[nBest.length];
		PrintWriter[] oracleBestWriter = new PrintWriter[nBest.length];
		PrintWriter[] oracleWorstWriter = new PrintWriter[nBest.length];
		for(int i=0; i<nBest.length; i++) {
			int n = nBest[i];
			oracleBestFile[i] = new File(baseDir + "wsj-22_oracleBest_" + n + ".mrg");
			oracleBestWriter[i] = FileUtil.getPrintWriter(oracleBestFile[i]);
			oracleWorstFile[i] = new File(baseDir + "wsj-22_oracleWorst_" + n + ".mrg");
			oracleWorstWriter[i] = FileUtil.getPrintWriter(oracleWorstFile[i]);
		}
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);		
		Iterator<TSNodeLabel> goldIter = goldTreebank.iterator();
		
		while(goldIter.hasNext()) {
			TSNodeLabel goldTree = goldIter.next();
			ArrayList<TSNodeLabel> nBestTrees = nextNBest(1000, nBestScanner);
			int index = 0;		
			TSNodeLabel currentBestTree=null, currentWorstTree=null;
			float currentBestScore=-1, currentWorstScore=2;
			int nBestIndex = 0;
			int nBestTarget = nBest[nBestIndex];
			for(TSNodeLabel t : nBestTrees) {
				index++;
				float tScore = EvalC.getFScore(goldTree, t, true);
				if (tScore>currentBestScore) {
					currentBestScore = tScore;
					currentBestTree = t;
				}
				if (tScore<currentWorstScore) {
					currentWorstScore = tScore;
					currentWorstTree = t;
				}
				if (index==nBestTarget) {
					oracleBestWriter[nBestIndex].println(currentBestTree.toString());
					oracleWorstWriter[nBestIndex].println(currentWorstTree.toString());
					nBestIndex++;
					if (nBestIndex<nBest.length) {						
						nBestTarget = nBest[nBestIndex];
					}
				}
			}	
			for(int i=0; i<nBest.length; i++) {
				int n = nBest[i];
				if (index<n) {
					oracleBestWriter[i].println(currentBestTree.toString());
					oracleWorstWriter[i].println(currentWorstTree.toString());
				}
			}
		}			
		
		for(int i=0; i<nBest.length; i++) {		
			int n = nBest[i];
			oracleBestWriter[i].close();
			oracleWorstWriter[i].close();
			File oracleBestEvalF = new File(baseDir + "wsj-22_oracleBest_" + n + ".evalF");
			File oracleWorstEvalF = new File(baseDir + "wsj-22_oracleWorst_" + n + ".evalF");
			new EvalC(goldFile, oracleBestFile[i], oracleBestEvalF);
			new EvalC(goldFile, oracleWorstFile[i], oracleWorstEvalF);
		}
	}
		
	public static void sentencesLength() throws Exception {
		String baseDir = Parameters.resultsPath + "TDS/Reranker/sec22_C/";
		File goldFile = new File(baseDir + "wsj-22_gold_ADV.mrg");
		ArrayList<TSNodeLabel> goldTreebank = TSNodeLabel.getTreebank(goldFile);
		int i=0;
		for(TSNodeLabel t : goldTreebank) {
			i++;
			System.out.println(t.countLexicalNodes());
			if (i==10) break;
		}
	}
	
	public static void printEventsFromStructure() throws Exception {
		//TSNodeLabel structure = new TSNodeLabel("(S (NP-SBJ (DT The) (NN rule)) (ADVP (RB also)) (VP (VBZ prohibits) (NP (NP (NN funding)) (PP (IN for) (NP (NP (NNS activities)) (SBAR (WHNP (WDT that)) (`` ``) (S (VP (VBP encourage) (, ,) (VBP promote) (CC or) (VBP advocate) (NP (NN abortion))))))))) (. .) ('' ''))");
		TSNodeLabel structure = new TSNodeLabel("(S (NP (NNP Mary)) (VP-H (VBZ-H is) (VP (VBG-H singing) (NP (DT an) (JJP (JJ old) (CC and) (JJ beautiful)) (NN-H song)))))");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_30_06_10"));
		Box b = Conversion.getTesniereStructure(structure, HL);
		File trainingSet = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		ArrayList<Box> trainingTreebank = 
			Conversion.getTesniereTreebank(trainingSet, HL);

		ProbModelStructureFiller_adv PME = new ProbModelStructureFiller_adv(trainingTreebank);
		PME.initModelDivHistory();
		PME.train();

		ArrayList<Event> events = new ArrayList<Event>(); 
		PME.extractEventsFromStructure(b, events);
		
		for(Event e : events) {
			System.out.println(e.encodingToStringFreq(0, 0));
		}
		
		System.out.println(PME.getProb(b));
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		printTables();
		//compareOneBestOracle();
		//rerank(10);	
		//rerankDebug(100);
		//test();
		//getOracleBestWorst();
		int[] nBest = new int[]{1,5,10,100,500,1000}; //1,10,100,1000};		
		for(int n: nBest) {
			System.out.println("n = " + n);
			rerank(n);		
		}
		//getProbDistribution();
		//sentencesLength();
		//getProbGold();
		//printEventsFromStructure();
		
		
	}



	
}
