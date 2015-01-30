package tesniere;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.TreeSet;

import settings.Parameters;
import symbols.SymbolString;
import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;

public abstract class Box extends Entity {

	TreeSet<FunctionalWord> ewList;
	TreeSet<FunctionalWord> punctList;	
	Label originalTSNodeLabel;
	public TreeSet<Box> dependents;
	public Box parent;
	boolean isConjunct;
	public int originalCat;
	public int derivedCat;
	
	public static String dq = "\"";
	
	public static String[] advSemTagSorted = new String[]{"ADV","BNF","CLR","DIR","EXT","LOC",
		"MNR","PRP","TMP","VOC"}; // maybe CLR out?
	
	public Box clone() {
		return clone(this.parent);
	}
	
	private Box clone(Box parent) {
		if (this.isJunctionBlock()) {
			BoxJunction boxJun = new BoxJunction();
			cloneBoxAttributes(boxJun, parent);
			BoxJunction thisJun = (BoxJunction)this;
			if (thisJun.conjunctions!=null) {
				boxJun.conjunctions = new TreeSet<FunctionalWord>();
				for(FunctionalWord c : thisJun.conjunctions) boxJun.conjunctions.add(c);				
			}
			if (thisJun.conjuncts!=null) {
				boxJun.conjuncts = new TreeSet<Box>();
				for(Box c : thisJun.conjuncts) {
					Box newConjunct = c.clone(boxJun);
					boxJun.conjuncts.add(newConjunct);
				}				
			}
			return boxJun;
		}
		else {
			BoxStandard boxStd = new BoxStandard();
			cloneBoxAttributes(boxStd, parent);
			BoxStandard thisStd = (BoxStandard)this;
			if (thisStd.fwList!=null) {
				boxStd.fwList = new TreeSet<ContentWord>();
				for(ContentWord fw : thisStd.fwList) boxStd.fwList.add(fw);
			}							
			return boxStd;
		}			
	}
	
	private void cloneBoxAttributes(Box b, Box parent) {
		b.parent = parent;
		b.originalTSNodeLabel = this.originalTSNodeLabel;
		b.isConjunct = this.isConjunct;
		b.originalCat = this.originalCat;
		b.derivedCat = this.derivedCat;
		if (this.punctList!=null) {
			b.punctList = new TreeSet<FunctionalWord>();
			b.punctList.addAll(this.punctList);
		}
		if (this.ewList!=null) {
			b.ewList = new TreeSet<FunctionalWord>();
			b.ewList.addAll(this.ewList);
		}		
		if (this.dependents!=null) {
			b.dependents = new TreeSet<Box>();
			for(Box d : this.dependents) {
				Box newDep = d.clone(b);
				b.dependents.add(newDep); 
			}
		}		
	}
	
	public void setOriginalTSNode(TSNodeLabel originalTSNode) {
		this.originalTSNodeLabel = originalTSNode.label;
	}
	
	public void setDependents(TreeSet<Box> depenents) {
		this.dependents = depenents;
		for(Box d : dependents) d.parent = this;
	}
	
	public void addDependentList(Collection<Box> depCollection) {
		if (depCollection==null || depCollection.isEmpty()) return;
		for(Box d : depCollection) d.parent = this;
		if (dependents == null) dependents = new TreeSet<Box>();
		this.dependents.addAll(depCollection);		
	}
	
	public void addDependent(Box d) {		
		if (dependents == null) dependents = new TreeSet<Box>();
		dependents.add(d);
		d.parent = this;
	}
	
	public void addEmptyOrPunctWord(FunctionalWord ew) {
		if (ew.isPunctuation()) {
			ew.wordType = 3;
			if (punctList == null) punctList = new TreeSet<FunctionalWord>();
			punctList.add(ew);
		}
		else {
			ew.wordType = 1;
			if (ewList == null) ewList = new TreeSet<FunctionalWord>();
			ewList.add(ew);
		}
	}
	
	public void addPunctuationWord(FunctionalWord pw) {
		if (punctList == null) punctList = new TreeSet<FunctionalWord>();
		punctList.add(pw);
	}
	
	public void addEmptyWordPunctuationList(Collection<FunctionalWord> ewCollection) {
		if (ewCollection==null || ewCollection.isEmpty()) return;
		if (ewList==null) ewList = new TreeSet<FunctionalWord>();
		if (punctList==null) punctList = new TreeSet<FunctionalWord>();		
		for(FunctionalWord ew : ewCollection) {
			int wordType = ew.defineWordType();
			if (wordType==3) punctList.add(ew);
			else ewList.add(ew);
		}
		if (this.ewList.isEmpty()) this.ewList = null;
		if (this.punctList.isEmpty()) this.punctList = null;					
	}	
	
	public int compareTo(Box anotherBox) {
		if (this.equals(anotherBox)) return 0;
		int thisStart = this.startPosition();
		int otherStart = anotherBox.startPosition();
		if (thisStart<otherStart) return -1;
		if (thisStart==otherStart) {
			return (this.endPosition() > anotherBox.endPosition()) ? -1 : 1;
		}
		return 1;
	}
	
	public abstract int startPosition(boolean includePunctuation, boolean includeDepOfCurrentBox, boolean includeDepOfRecursiveBox);	
	public abstract int endPosition(boolean includePunctuation, boolean includeDepOfCurrentBox, boolean includeDepOfRecursiveBox);	
	
	public int startPosition() {
		return startPosition(false, false, false);
	}
	
	public int endPosition() {
		return endPosition(false, false, false);
	}	
		
	public abstract String toString();
	
	public abstract String toXmlString(int indentLevel);
	
	public static String getValueOfXmlAttribute(String attributeValue) {
		return attributeValue.substring(attributeValue.indexOf('=')+2, attributeValue.length()-1);
	}
	
	public static Box getBoxFromXmlScan(Scanner scan, String firstLine) {
		if (firstLine.startsWith("<StandardBox")) return BoxStandard.getStandardBoxFromXmlScan(scan, firstLine);
		return BoxJunction.getJunctionBoxFromXmlScan(scan, firstLine);
	}

	public abstract int countAllNodes();
	
	public abstract int countAllWords();

	public TreeSet<Box> collectBoxStructure() {
		TreeSet<Box> result = new TreeSet<Box>();
		collectBoxStructure(result);
		return result;
	}
	
	public TreeSet<BoxJunction> collectJunctionStructure() {
		TreeSet<BoxJunction> collector = new TreeSet<BoxJunction>();
		collectJunctionStructure(collector);
		return collector;
	}
	
	public void collectJunctionStructure(TreeSet<BoxJunction> collector) {
		if (this.isJunctionBlock()) {
			BoxJunction bj = (BoxJunction)this;
			collector.add(bj);
			for(Box conjunct : bj.conjuncts) {
				conjunct.collectJunctionStructure(collector);
			}
		}
		if (this.dependents==null) return;
		for(Box b : this.dependents) b.collectJunctionStructure(collector);
	}
	
	
	abstract void collectBoxStructure(TreeSet<Box> set);

	public abstract int maxDepth();
	
	public abstract boolean isJunctionBlock();
	
	public abstract boolean equals(Object o);
	
	public boolean isConjuct() {
		//return parent.isCoordination() && ((BoxJunction)parent).boxes.contains(this);
		return isConjunct;
	}

	public int getDepth() {
		if (parent==null) return 0;
		int parentDepth = parent.getDepth();
		int parentWidth = parent.getWidth();
		if (isConjunct) return parentDepth;
		return parentDepth+parentWidth; 
	}

	public abstract int getWidth();
	public abstract int getWidthWithDependents();

	public abstract void fillWordArrayTable(Word[] wordsArray,
			IdentityHashMap<Word, Box> table);
	
	public abstract void fillWordArray(Word[] wordsArray);
	
	public abstract Word leftMostWord(boolean includeDepStdBox, boolean includeDepJunc);
	
	public abstract Word rightMostWord(boolean includeDepStdBox, boolean includeDepJunc);
	
	public ArrayList<Box> getLowestBlocks() {
		ArrayList<Box> result = new ArrayList<Box>();
		lowestBlocks(result);
		return result;
	}	
	
	public abstract void lowestBlocks(ArrayList<Box> list);
	
	public abstract void lowestBlocksWithDependents(ArrayList<Box> list);

	public abstract Word getUpperWord();

	public abstract void uniteVerbs();

	public static String[] circomstantCatSorted = new String[]{"ADVP","PP","PRN","RB","RBR","RBS"};
	
	public void establishCategories() {
		if (originalCat==-2) {
			getOriginalCat();
			if (originalCat==-1) {
				Parameters.logPrintln("Warning: missing original cat: " + this.toString());
			}
		}
		if (derivedCat==-2) {
			getDerivedCat();
			if (derivedCat==-1) {
				Parameters.logPrintln("Warning: missing derived cat: " + this.toString());
			}
		}
		if (this.isJunctionBlock()) {
			BoxJunction bc = (BoxJunction)this;
			for(Box b : bc.conjuncts) b.establishCategories(); 
		}
		if (dependents!=null) {
			for(Box d : dependents) d.establishCategories(); 
		}	
	}
	
	public static String[] catsString = new String[]{"NUL","VER","ADV","NOU","ADJ"};
	
	public static String catToString(int catNumber) {
		if (catNumber<0) return catsString[0];
		return catsString[catNumber+1];
	}
	
	protected String getDerivedCatString() {
		return catToString(derivedCat);
	}
	
	protected String getOriginalCatString() {
		return catToString(originalCat);
	}

	// 0,1,2,3: verb, adverb, nount, adj	
	protected int getDerivedCat() {
		if (derivedCat!=-2) return derivedCat;
		if (parent==null) return derivedCat = getOriginalCat();
		if (this.isConjunct) {
			if (this.parent.ewList==null)  return derivedCat = parent.getDerivedCat();
			return derivedCat = getOriginalCat();
		}
		int parentCat = parent.getOriginalCat();		
		if (parentCat==-1) return derivedCat = -1;
		if (parentCat==0) {
			String label = this.originalTSNodeLabel.toString();
			String[] semTags = label.split("-");
			//if (containsAdvSemTag(semTags)) return 1;
			if (containsAdvSemTag(semTags) 
					//|| hasINTOInEmptyWordsAndNotFirstArgument()
					|| Arrays.binarySearch(circomstantCatSorted, semTags[0])>=0) return derivedCat = 1;
			//|| (ewList!=null && ewList.first().posLabel.toString().equals("IN"))) return derivedCat = 1;
			return derivedCat = 2;
		}
		if (parentCat==2) return derivedCat = 3;
		return derivedCat = 1; //(parentCat==3) 	
	}
	
	private static String[] INTO = new String[]{"IN","TO"};
	
	private boolean hasINTOInEmptyWordsAndNotFirstArgument() {
		if (this.ewList==null) return false;
		if (parent.rightMostWord(false, false).position + 1 == this.leftMostWord(false, false).position) return false;
		for(FunctionalWord ew : ewList) {
			if (Arrays.binarySearch(INTO, ew.posLabel.toString())>=0) return true;
		}
		return false;
	}
	
	protected abstract int getOriginalCat();

	public abstract void fixRBJJ();	
	
	public static boolean containsAdvSemTag(String[] semTags) {
		for(String t : semTags) {
			if (Arrays.binarySearch(advSemTagSorted, t)>=0) return true;
		}
		return false;
	}

	public int numberOfCoordinationGapsAbove() {
		int result = this.isConjunct ? 1 : 0;
		if (this.parent==null) return result;
		if (!this.isConjunct && this.parent.isJunctionBlock()) { 
			//dependent of a coordination
			int lowestBoxDepth = this.parent.getMaxLowestBoxDepth(false);
			result = 2 + 2 * this.parent.getInnerMaxCoordBox(lowestBoxDepth, false, true);
			//result += 2 + 2 * this.parent.getMaxNumberOfInnerCoordination(false);
		}
		result += this.parent.numberOfCoordinationGapsAbove();
		return result;		
	}
	
	public int numberOfCoordinationGapsAboveBeforeReaching(Box parentBoxStop) {
		if (this==parentBoxStop) return 1;
		int result = this.isJunctionBlock() ? 1 : 0;		
		result += this.parent.numberOfCoordinationGapsAboveBeforeReaching(parentBoxStop);
		return result;
	}

	public void fixAppositionPP() {			
		if (this.isJunctionBlock()) {
			BoxJunction bJ = (BoxJunction)this;
			for(Box b : bJ.conjuncts) b.fixAppositionPP();
		}
		if (this.dependents==null) return;
		ArrayList<Box> dependentsList = new ArrayList<Box>(dependents);
		boolean modified = false;
		ArrayList<Box> otherDependents = new ArrayList<Box>(); 
		for(ListIterator<Box> li = dependentsList.listIterator(); li.hasNext(); ) {
			Box dep = li.next();
			if (dep.isJunctionBlock()) {
				BoxJunction bj = (BoxJunction)dep;
				boolean PPappostion = bj.conjunctions==null && 
					bj.conjuncts.first().originalTSNodeLabel.toString().startsWith("PP");
				if (PPappostion) {								
					li.remove();					
					if (bj.ewList!=null) this.addEmptyWordPunctuationList(bj.ewList);
					if (bj.punctList!=null) this.addEmptyWordPunctuationList(bj.punctList);
					for(Box conjPP : bj.conjuncts) {
						li.add(conjPP);
						conjPP.parent = this;
						conjPP.isConjunct = false;
					}				
					if (bj.dependents!=null) otherDependents.addAll(bj.dependents);					
					modified = true;
				}			
				
			}
			dep.fixAppositionPP();
		}	
		if (modified) {
			dependents = new TreeSet<Box>(dependentsList);
			this.addDependentList(otherDependents);
			Parameters.logPrintln("Fixed Apposition PP");					
		}
	}
	
	public void checkCoordinations() {		
		if (this.isJunctionBlock()) {
			BoxJunction bj = (BoxJunction)this;
			if (bj.conjuncts.size()<2) {
				Parameters.logPrintln("Warning: junction with less than 2 juncted structures.");
			}
			for(Box conjunct : bj.conjuncts) {
				conjunct.checkCoordinations();
			}
		}
		if (this.dependents==null) return;
		for(Box b : this.dependents) b.checkCoordinations();
	}
	
	public void spotComplexCoordinations(int sentenceIndex) {
		if (this.isJunctionBlock()) {
			BoxJunction bj = (BoxJunction)this;			
			for(Box conjunct : bj.conjuncts) {
				if (conjunct.isJunctionBlock() && conjunct.dependents!=null) {
					System.out.println(sentenceIndex + ": complex coordination");
				}
				conjunct.spotComplexCoordinations(sentenceIndex);
			}
		}
		if (this.dependents==null) return;
		for(Box b : this.dependents) b.spotComplexCoordinations(sentenceIndex);
	}

	public void checkFullWords() {
		if (!this.isJunctionBlock()) {
			BoxStandard b = (BoxStandard) this;
			if (b.fwList==null) {
				Parameters.logPrintln("Warning: standard block doesn't have full words: " + originalTSNodeLabel);
			}
			else if (b.fwList.size()>1) {
				for(ContentWord w : b.fwList) {
					if (!w.posLabel.toString().startsWith("NNP")) {
						Parameters.logPrintln("Warning: standard block with more than one full word not NNP*: " + this.toString());
						break;
					}
				}				
			}
		}
		else {
			BoxJunction c = (BoxJunction) this;
			for(Box b : c.conjuncts) b.checkFullWords();
		}
		if (this.dependents==null) return;
		for(Box b : dependents) b.checkFullWords();
	}
	
	public boolean hasStdBlocksWithoutFullWords() {
		if (this.isJunctionBlock()) {
			BoxJunction c = (BoxJunction) this;
			for(Box b : c.conjuncts) {
				if (b.hasStdBlocksWithoutFullWords()) return true;					
			}
		}
		else {
			BoxStandard b = (BoxStandard) this;
			if (b.fwList==null) return true;
		}
		if (this.dependents==null) return false;
		for(Box b : dependents) {
			if (b.hasStdBlocksWithoutFullWords()) return true;
		}
		return false;
	}
	
	public void fixOriginalCategories() {
		if (originalCat<0 || derivedCat>=0) originalCat = derivedCat;
		if (this.isJunctionBlock()) {
			BoxJunction c = (BoxJunction) this;
			for(Box b : c.conjuncts) b.fixOriginalCategories(); 					
		}
		if (this.dependents!=null) {
			for(Box b : dependents) b.fixOriginalCategories();
		}
	}
	
	public boolean hasNullCategories() {
		if (this.originalCat<0 || this.derivedCat<0) return true; 
		if (this.isJunctionBlock()) {
			BoxJunction c = (BoxJunction) this;
			for(Box b : c.conjuncts) {
				if (b.hasNullCategories()) return true;					
			}
		}
		if (this.dependents==null) return false;
		for(Box b : dependents) {
			if (b.hasNullCategories()) return true;
		}
		return false;		
	}
	
	public int countJunctionBlocks() {
		int result = 0;
		if (this.isJunctionBlock()) {
			result ++;
			BoxJunction c = (BoxJunction) this;
			for(Box b : c.conjuncts) result += b.countJunctionBlocks();
		}
		if (this.dependents==null) return result;
		for(Box b : dependents) result += b.countJunctionBlocks();
		return result;
	}

	public int countCoordinations() {
		int result = 0;
		if (this.isJunctionBlock()) {
			BoxJunction c = (BoxJunction) this;
			boolean foundCC = false;
			if (c.conjunctions!=null) {
				for(FunctionalWord ew : c.conjunctions) {
					if (ew.posLabel.toString().equals("CC") || 
							(ew.posParentLabel!=null && ew.posParentLabel.toString().equals("CONJP"))) {
						foundCC = true;
						break;
					}
				}
			}
			if (!foundCC && c.ewList!=null) {
				for(FunctionalWord ew : c.ewList) {
					if (ew.posLabel.toString().equals("CC") || 
							(ew.posParentLabel!=null && ew.posParentLabel.toString().equals("CONJP"))) {
						foundCC = true;
						break;
					}
				}
			}
			if (foundCC) result++;
			for(Box b : c.conjuncts) result += b.countCoordinations();
		}
		if (this.dependents==null) return result;
		for(Box b : dependents) result += b.countCoordinations();
		return result;
	}	
	
	public int getInnerMaxCoordBox(int targetLowestDepth, boolean first, boolean countInnerDep) {
		int max = 0;
		if (this.isJunctionBlock()) {
			BoxJunction boxJ = (BoxJunction)this;
			for(Box b : boxJ.conjuncts) {
				int comb = b.getInnerMaxCoordBox(targetLowestDepth, true, countInnerDep);				
				if (b.isJunctionBlock() && b.getMaxLowestBoxDepth(countInnerDep)==targetLowestDepth) comb++;
				if (comb>max) max = comb;				
			}
		}
		if (!first || dependents==null) return max;
		for(Box b : dependents) {
			int comb = b.getInnerMaxCoordBox(targetLowestDepth, true, countInnerDep);
			if (b.isJunctionBlock() && b.getMaxLowestBoxDepth(countInnerDep)==targetLowestDepth) comb++;
			if (comb>max) max = comb;
		}
		return max;
		
	}
	
	public int getMaxNumberOfInnerCoordination(boolean countDependent) {
		int max = 0;
		if (this.isJunctionBlock()) {
			BoxJunction boxJ = (BoxJunction)this;
			for(Box b : boxJ.conjuncts) {
				int comb = b.getMaxNumberOfInnerCoordination(true);				
				if (b.isJunctionBlock()) comb++;
				if (comb>max) max = comb;				
			}
		}
		if (!countDependent || dependents==null) return max;
		for(Box b : dependents) {
			int comb = b.getMaxNumberOfInnerCoordination(true);
			if (b.isJunctionBlock()) comb++;
			if (comb>max) max = comb;
		}
		return max;
	}

	public int getMaxLowestBoxDepth(boolean first) {		
		int maxDepth = this.getDepth();
		if (this.isJunctionBlock()) {
			BoxJunction boxJ = (BoxJunction)this;
			for(Box b : boxJ.conjuncts) {
				int depth = b.getMaxLowestBoxDepth(true);								
				if (depth>maxDepth) maxDepth = depth;				
			}
		}
		if (!first || dependents==null) return maxDepth;
		for(Box b : dependents) {
			int depth = b.getMaxLowestBoxDepth(true);								
			if (depth>maxDepth) maxDepth = depth;
		}				
		return maxDepth;		
	}

	public int junctionBoxUpHavingRightmostWord(Word w) {
		int result = this.isJunctionBlock() && this.rightMostWord(true,true)==w ? 1 : 0;
		if (this.parent==null) return result;
		result += this.parent.junctionBoxUpHavingRightmostWord(w);
		return result;
	}
	
	public int junctionBoxUpHavingLeftmostWord(Word w) {
		int result = this.isJunctionBlock() && this.leftMostWord(true,true)==w ? 1 : 0;
		if (this.parent==null) return result;
		result += this.parent.junctionBoxUpHavingLeftmostWord(w);
		return result;
	}

	public int junctionBoxDownHavingRightmostWord(Word word, boolean first) {
		int result = 0;
		if (this.isJunctionBlock()) {
			BoxJunction boxJ = (BoxJunction)this;
			for(Box b : boxJ.conjuncts) {
				if (b.isJunctionBlock() && b.rightMostWord(true,false)==word) result++;
				result += b.junctionBoxDownHavingRightmostWord(word, false);				
			}
		}
		if (first || dependents==null) return result;
		for(Box b : dependents) {
			if (b.isJunctionBlock() && b.rightMostWord(true,false)==word) result++;
			result += b.junctionBoxDownHavingRightmostWord(word, false);			
		}
		return result;
	}

	public int junctionBoxDownHavingLeftmostWord(Word word, boolean first) {
		int result = 0;
		if (this.isJunctionBlock()) {
			BoxJunction boxJ = (BoxJunction)this;
			for(Box b : boxJ.conjuncts) {
				if (b.isJunctionBlock() && b.leftMostWord(true,false)==word) result++;
				result += b.junctionBoxDownHavingLeftmostWord(word, false);				
			}
		}
		if (first || dependents==null) return result;
		for(Box b : dependents) {
			if (b.isJunctionBlock() && b.leftMostWord(true,false)==word) result++;
			result += b.junctionBoxDownHavingLeftmostWord(word, false);			
		}
		return result;
	}

	public boolean isDirectlyInCoordination() {
		if (this.isConjunct) return true;
		if (this.parent.isJunctionBlock()) return false;
		return this.parent.isDirectlyInCoordination();
	}

	public abstract boolean replaceBlock(Box box, Box dep);

	public abstract Box uniteCompoundCoordinatedVerbs();

	public Word getLeftMostWordExcludingPunctuation() {
		return leftMostWord(false, false);
	}
	
	public Word getRightMostWordExcludingPunctuation() {
		return rightMostWord(false, false);
	}

	public boolean sameType(Box box) {
		return this.isJunctionBlock()==box.isJunctionBlock();
	}

	public boolean matchFullWordsStructure(Box otherBox) {
		if (!this.sameType(otherBox)) return false;
		if (this.isJunctionBlock()) return ((BoxJunction)this).matchFullWordsStructure((BoxJunction)otherBox);
		else return ((BoxStandard)this).matchFullWordsStructure((BoxStandard)otherBox);
	}
	
	public void appendFullWordStringList(ArrayList<String> list) {
		if (this.isJunctionBlock()) {
			BoxJunction boxJ = (BoxJunction)this;
			for(Box b : boxJ.conjuncts) {
				b.appendFullWordStringList(list);				
			}
		}
		else {
			BoxStandard boxS = (BoxStandard)this;
			if (boxS.fwList!=null) {
				for(ContentWord fw : boxS.fwList) {
					list.add(fw.word);
				}			
			}
		}		
	}

	public Box governingParent() {
		if (this.isConjunct) {
			return this.parent.governingParent();
		}
		return this.parent;				
	}
	
	public int subClausesCardinality() {
		if (this.isJunctionBlock()) {
			int result = 0;
			BoxJunction boxJ = (BoxJunction)this;
			for(Box b : boxJ.conjuncts) {
				result += b.subClausesCardinality();				
			}
			if (boxJ.dependents==null) return result;
			for(Box b : boxJ.dependents) {
				result *= b.subClausesCardinality();
			}
			return result;
		}
		BoxStandard boxS = (BoxStandard)this;
		int result = 1;
		if (boxS.dependents==null) return result;
		for(Box b : boxS.dependents) {
			result *= b.subClausesCardinality();
		}		
		return result;
	}
	
	public void addStandardBoxesInCollection(Collection<BoxStandard> list) {
		if (this.isJunctionBlock()) {			
			BoxJunction boxJ = (BoxJunction)this;
			for(Box b : boxJ.conjuncts) {
				b.addStandardBoxesInCollection(list);				
			}			
		}
		else {
			BoxStandard boxS = (BoxStandard)this;
			list.add(boxS);				
		}
		if (dependents!=null) {
			for(Box b : dependents) {
				b.addStandardBoxesInCollection(list);
			}
		}
	}	
	
	public BoxJunction getUpmostJunctionParent() {
		if (!this.isConjuct()) return null;		
		Box p = this.parent;
		while(p.isConjunct && p.parent!=null) {
			p = p.parent;
		}
		return (BoxJunction)p;
	}
	
	public ContentWord firstFullWord() {
		if (this.isJunctionBlock()) {
			return ((BoxJunction)this).conjuncts.first().firstFullWord();
		}
		BoxStandard bs = (BoxStandard)this;
		if (bs.fwList==null) return null;
		return bs.fwList.first();
	}
	
	public void fillDistributionConjunctsNumber(int[] stats) {
		if (this.isJunctionBlock()) {			
			BoxJunction boxJ = (BoxJunction)this;
			if (boxJ.conjuncts==null) stats[0]++;
			else {
				int n = boxJ.conjuncts.size();
				stats[n]++;
				for(Box b : boxJ.conjuncts) {
					b.fillDistributionConjunctsNumber(stats);				
				}
			}						
		}
		if (dependents!=null) {
			for(Box b : dependents) {
				b.fillDistributionConjunctsNumber(stats);
			}
		}
	}
	
	public Box leftMostBox() {		
		Box result = this;		
		if (dependents!=null) {
			Box firstDep = dependents.first();
			if (firstDep.startPosition()<result.startPosition()) result = firstDep;
		}
		if (this.isJunctionBlock()) {			
			BoxJunction boxJ = (BoxJunction)this;
			Box firstConjunctLeftMostBox = boxJ.conjuncts.first().leftMostBox();			
			if (firstConjunctLeftMostBox.startPosition()<result.startPosition()) result = firstConjunctLeftMostBox;						 
		}
		if (result==this) return result;
		return result.leftMostBox();
	}

	public Box fixStartCoordination() {
		Box leftMostBox = this.leftMostBox();
		TreeSet<FunctionalWord> ewList = leftMostBox.ewList; 
		if (ewList==null) return this;
		FunctionalWord firstEmptyWord = ewList.first(); 
		if (firstEmptyWord.getPos().equals("CC")) {
			if (!Character.isUpperCase(firstEmptyWord.word.charAt(0))) return this;
			BoxJunction newJunction = new BoxJunction();
			newJunction.addConjunctionWord(firstEmptyWord);			
			ewList.remove(firstEmptyWord);
			firstEmptyWord.wordType = 2;
			if (ewList.isEmpty()) leftMostBox.ewList = null;
			newJunction.parent = leftMostBox.parent;
			boolean leftMostBoxWasConjunct = leftMostBox.isConjunct;
			newJunction.addConjunct(leftMostBox);			 
			int firstEwPos = firstEmptyWord.position; 
			if (leftMostBox.punctList!=null && firstEwPos>0) {
				Iterator<FunctionalWord> li = leftMostBox.punctList.iterator();
				while(li.hasNext()) {
					FunctionalWord p = li.next();
					if (p.position<firstEwPos) {
						newJunction.addPunctuationWord(p);
						li.remove();
					}
				}
				if (leftMostBox.punctList.isEmpty()) leftMostBox.punctList = null;
			}
			newJunction.originalTSNodeLabel = leftMostBox.originalTSNodeLabel;
			Parameters.logPrintln("Fixed conjunction as first word.");
			if (newJunction.parent==null) return newJunction;
			Box top = newJunction.parent;
			if (leftMostBoxWasConjunct) {
				BoxJunction topJ = (BoxJunction)top;
				topJ.conjuncts.remove(leftMostBox);
				topJ.conjuncts.add(newJunction);
			}
			else {
				top.dependents.remove(leftMostBox);
				top.dependents.add(newJunction);
			}			
			while(top.parent!=null) top = top.parent;
			return top;
		}	
		return this;
	}

	public Box fixCoordVerbEllipsis() {
		Box result = this;		
		if (this.isJunctionBlock()) {			
			BoxJunction boxJ = (BoxJunction)this;
			if (boxJ.conjuncts.size()==2) {
				ArrayList<Box> conjList = new ArrayList<Box>(boxJ.conjuncts); 
				ListIterator<Box> iter = conjList.listIterator();
				Box firstConjunct = iter.next();				
				if (firstConjunct.isStandardBox() && firstConjunct.originalCat==0 && 
						firstConjunct.dependents!=null) { // first is verb
					BoxStandard firstConjuctStd = (BoxStandard)firstConjunct;
					Box secondConjunct = iter.next();
					if (secondConjunct.originalCat!=0) { // second is not a verb
						iter.previous();
						iter.previous();
						iter.remove();
						Box firstDepRight = null;
						ArrayList<Box> otherDepRight = new ArrayList<Box>();
						Iterator<Box> depIter = firstConjuctStd.dependents.iterator();
						while(depIter.hasNext()) {
							Box d = depIter.next();
							if (Entity.leftOverlapsRight(d, firstConjuctStd)==1) { // rightDep
								depIter.remove();
								if (firstDepRight==null) firstDepRight = d;
								else otherDepRight.add(d);
							}
						}
						if (firstDepRight!=null) {
							if (!otherDepRight.isEmpty()) {
								firstDepRight.addDependentList(otherDepRight);
							}						
							iter.add(firstDepRight);
							firstDepRight.parent = boxJ;
							firstDepRight.isConjunct = true;
							boxJ.conjuncts = new TreeSet<Box>(conjList);
							firstConjuctStd.addDependent(boxJ);
							firstConjuctStd.addEmptyWordPunctuationList(boxJ.ewList);
							boxJ.ewList = null;
							if (boxJ.dependents!=null) {
								ArrayList<Box> depList = new ArrayList<Box>(boxJ.dependents);
								depIter = depList.listIterator();
								boolean modified = false;
								while(depIter.hasNext()) {
									Box d = depIter.next();
									if (Entity.leftOverlapsRight(d, firstConjuctStd)<1) {								
										modified = true;
										depIter.remove();
										firstConjuctStd.addDependent(d);
									}
								}
								if (modified) {
									if (depList.isEmpty()) boxJ.dependents = null;
									else boxJ.dependents = new TreeSet<Box>(depList);
								}
							}
							boxJ.originalTSNodeLabel = firstDepRight.originalTSNodeLabel;
							boxJ.originalCat = boxJ.derivedCat = -2;
							boxJ.getOriginalCat();
							boxJ.getDerivedCat();
							firstConjuctStd.isConjunct = false;
							Parameters.logPrintln("Fixed conjunction with gapping.");
							firstConjuctStd.parent = null;
							return firstConjuctStd;
						}
					}
				}
			}
			
			ArrayList<Box> conjunctsList = new ArrayList<Box>(boxJ.conjuncts); 
			ListIterator<Box> iter = conjunctsList.listIterator();
			boolean modified = false;
			while(iter.hasNext()) {
				Box c = iter.next();
				Box newC = c.fixCoordVerbEllipsis();
				if (newC!=c) {
					modified = true;
					iter.remove();
					iter.add(newC);
					newC.parent = this;
					newC.getOriginalCat();
				}
			}
			if (modified) {
				boxJ.conjuncts = new TreeSet<Box>(conjunctsList);
			}
			
		}
		if (dependents!=null) {
			ArrayList<Box> depList = new ArrayList<Box>(this.dependents);
			ListIterator<Box> iter = depList.listIterator();
			boolean modified = false;
			while(iter.hasNext()) {
				Box c = iter.next();
				Box newC = c.fixCoordVerbEllipsis();
				if (newC!=c) {
					modified = true;
					iter.remove();
					iter.add(newC);
					newC.parent = this;
					newC.getOriginalCat();
				}
			}
			if (modified) {
				this.dependents = new TreeSet<Box>(depList);
			}
		}
		return result;		
	}

	public TSNodeLabel toPhraseStructure() {
		
		TreeSet<Entity> entitySet = new TreeSet<Entity>();
		boolean isJunction = false;
		BoxJunction boxJ = null;
		BoxStandard bs = null;
		TreeSet<FunctionalWord> functWordsSet = null;
		if (this.isJunctionBlock()) {
			isJunction = true;
			//label = Label.getLabel("JB_" + this.getOriginalCatString() + "^" + this.getDerivedCatString());			
			boxJ = (BoxJunction)this;						
			if (boxJ.conjuncts!=null) entitySet.addAll(boxJ.conjuncts);
			if (boxJ.dependents!=null) entitySet.addAll(boxJ.dependents);
			if (boxJ.conjunctions!=null) entitySet.addAll(boxJ.conjunctions);			
			if (boxJ.ewList!=null) entitySet.addAll(boxJ.ewList);
			if (boxJ.punctList!=null) entitySet.addAll(boxJ.punctList);
			functWordsSet = boxJ.ewList;
		}
		else {
			bs = (BoxStandard)this;
			//label = Label.getLabel("SB_" + this.getOriginalCatString() + "^" + this.getDerivedCatString());			
			if (bs.dependents!=null) entitySet.addAll(bs.dependents);
			if (bs.fwList!=null) entitySet.addAll(bs.fwList);
			if (bs.ewList!=null) entitySet.addAll(bs.ewList);
			if (bs.punctList!=null) entitySet.addAll(bs.punctList);			
			functWordsSet = bs.ewList;
		}	
		
		if (functWordsSet==null)
			functWordsSet = new TreeSet<FunctionalWord>();
		
		Label label = Label.getLabel( (this.isConjunct ? "BOX_" : "BOX_") + 
						this.getOriginalCatString() + "^" + this.getDerivedCatString());
		//Label label = Label.getLabel( (this.isConjunct ? "CONJBOX_" : "BOX_") + 
		//		this.getOriginalCatString() + "^" + this.getDerivedCatString());
		TSNodeLabel result = new TSNodeLabel(label, false);
		//entitySet.add(this);
		//boolean left = true;
		ArrayList<TSNodeLabel> daughters = new ArrayList<TSNodeLabel>();
		ArrayList<Entity> entityList = new ArrayList<Entity>();
		entityList.addAll(entitySet);
		ListIterator<Entity> entityListIter = entityList.listIterator();			
		while(entityListIter.hasNext()) {
			Entity e = entityListIter.next();
			//if (e==this) {
			//	left = false;
			//	continue;
			//}
			if (e.isBox()) {
				TSNodeLabel d = ((Box)e).toPhraseStructure();
				if (isJunction && boxJ.conjuncts.contains(e)) {
					TSNodeLabel dSub = d;
					String boxClabel = dSub.label.toString();
					int firstUnderscore = boxClabel.indexOf('_');
					String cats = boxClabel.substring(firstUnderscore);
					boxClabel = boxClabel.subSequence(0, firstUnderscore) + "C" + cats;
					dSub.relabel(boxClabel);
					d = new TSNodeLabel(Label.getLabel("CONJUNCT" + cats), false);
					d.assignUniqueDaughter(dSub);
				}
				daughters.add(d);
				continue;
			}
			Word w = (Word)e;
			if (w.isFunctionalWord()) {
				//TSNodeLabel funcWord = new TSNodeLabel(Label.getLabel(w.word), true);
				TSNodeLabel funcWord = w.toPhraseStructureLexWord();
				daughters.add(funcWord);
			}
			/*
			if (isJunction && boxJ.conjunctions.contains(w)) {
				TSNodeLabel conjWords = new TSNodeLabel(Label.getLabel("JW"), false);
				ArrayList<TSNodeLabel> conjDaughters = new ArrayList<TSNodeLabel>();
				conjDaughters.add(w.toPhraseStructurePosWord());
				while(entityListIter.hasNext()) {
					Entity next = entityListIter.next();
					if (next.isWord() && boxJ.conjunctions.contains(w)) {
						Word nextConj = (Word)next;
						conjDaughters.add(nextConj.toPhraseStructurePosWord());
					}
					else {
						entityListIter.previous();
						break;
					}
				}
				conjWords.assignDaughters(conjDaughters);
				daughters.add(conjWords);
			}
			else if (w.isPunctuation()) {
				// puntuation word
				daughters.add(w.toPhraseStructurePosWord());
			}	
			else if (w.isFunctionalWord()){ //if (boxJ.ewList.contains(w)) {
				TSNodeLabel funcWords = new TSNodeLabel(Label.getLabel("FW"), false);
				ArrayList<TSNodeLabel> functDaughters = new ArrayList<TSNodeLabel>();
				functDaughters.add(w.toPhraseStructurePosWord());
				while(entityListIter.hasNext()) {
					Entity next = entityListIter.next();
					if (next.isWord() && next.isFunctionalWord() && functWordsSet.contains(next)) {						
						Word nextFunct = (Word)next;
						functDaughters.add(nextFunct.toPhraseStructurePosWord());
					}
					else {
						entityListIter.previous();
						break;
					}
				}
				funcWords.assignDaughters(functDaughters);
				daughters.add(funcWords);
			}
			*/
			/*
			else { //content word
				TSNodeLabel contentWords = new TSNodeLabel(Label.getLabel("CW"), false);
				ArrayList<TSNodeLabel> contentDaughters = new ArrayList<TSNodeLabel>();
				contentDaughters.add(w.toPhraseStructurePosWord());
				while(entityListIter.hasNext()) {
					Entity next = entityListIter.next();
					if (next.isWord() && next.isContentWord()) {
						Word nextFunct = (Word)next;
						contentDaughters.add(nextFunct.toPhraseStructurePosWord());
					}
					else {
						entityListIter.previous();
						break;
					}
				}
				contentWords.assignDaughters(contentDaughters);
				daughters.add(contentWords);
			}
			*/
			else  {
				TSNodeLabel contWord = w.toPhraseStructurePosWord();
				daughters.add(contWord);
			}
		}
		
		result.assignDaughters(daughters);
		return result;
		
	}

	
}
