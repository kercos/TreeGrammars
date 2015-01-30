package tesniere;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import tsg.Label;
import util.Utility;

/**
 * 
 * @author fedya
 * The list of entities are either Boxes or empty words (coordination or punctuation)
 */
public class BoxJunction extends Box {

	TreeSet<Box> conjuncts;
	TreeSet<FunctionalWord> conjunctions;	
	
	public BoxJunction(TreeSet<Box> boxes, TreeSet<FunctionalWord> coordinants,
			TreeSet<FunctionalWord> emptyWords) {		
		setBoxesCoordEmptyWords(boxes, coordinants, emptyWords);
		originalCat = -2;
		derivedCat = -2;
	}	

	public BoxJunction() {
		originalCat = -2;
		derivedCat = -2;
	}
	
	public void addConjunctionWord(FunctionalWord cw) {
		if (conjunctions == null) conjunctions = new TreeSet<FunctionalWord>();
		conjunctions.add(cw);
	}
	
	public void addConjunct(Box conjunct) {
		if (this.conjuncts==null) conjuncts = new TreeSet<Box>();
		conjuncts.add(conjunct);
		conjunct.parent = this;
		conjunct.isConjunct = true;
	}
	
	public void setBoxesCoordEmptyWords(TreeSet<Box> conjuncts, 
			TreeSet<FunctionalWord> conjunctions, TreeSet<FunctionalWord> ewList) {
		this.conjuncts = (conjuncts.isEmpty()) ? null : conjuncts;
		this.conjunctions = (conjunctions.isEmpty()) ? null : conjunctions;
		this.addEmptyWordPunctuationList(ewList);
		for(Box b : conjuncts) {
			b.parent = this;
			b.isConjunct = true;
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("CoordStruct:");
		result.append(" Node " + originalTSNodeLabel.toString());
		if (conjunctions!=null) result.append(" Conjunctions: " + conjunctions);
		if (ewList!=null) result.append(" Empty words: " + ewList);
		if (punctList!=null) result.append(" Punctuation words: " + punctList);			
		result.append(" Blocks: " + conjuncts);
		if (this.dependents!=null) result.append(" Dependents: " + dependents);
		result.append(" In/Out Cat: " + originalCat + "/" + derivedCat);
		return result.toString();
	}
	
	@Override
	public String toXmlString(int indentLevel) {
		int indentLevelPlus = indentLevel + 1;
		int indentLevelPlusPlus = indentLevel + 2;
		String indentString = Utility.fillChar(indentLevel, '\t');		
		String indentStringPlus = Utility.fillChar(indentLevelPlus, '\t');
		String indentStringPlusPlus = Utility.fillChar(indentLevelPlusPlus, '\t');
		StringBuilder sb = new StringBuilder();
		sb.append(indentString + "<" + "JunctionBox" + " OrigNodeLabel=" + dq + originalTSNodeLabel.toString() + dq + 
				" OrigCat=" + dq + originalCat + dq + " DerivedCat=" + dq + derivedCat + dq + " >\n");
		if (conjunctions!=null) {
			sb.append(indentStringPlus + "<ConjunctionWords>\n");
			for(FunctionalWord ew : conjunctions) sb.append(indentStringPlusPlus + ew.toXmlString() + "\n");
			sb.append(indentStringPlus + "</ConjunctionWords>\n");
		}
		if (ewList!=null) {
			sb.append(indentStringPlus + "<EmptyWords>\n");
			for(FunctionalWord ew : ewList) sb.append(indentStringPlusPlus + ew.toXmlString() + "\n");
			sb.append(indentStringPlus + "</EmptyWords>\n");
		}
		if (punctList!=null) {
			sb.append(indentStringPlus + "<PunctWords>\n");
			for(FunctionalWord ew : punctList) sb.append(indentStringPlusPlus + ew.toXmlString() + "\n");
			sb.append(indentStringPlus + "</PunctWords>\n");
		}
		sb.append(indentStringPlus + "<ConjunctBlocks>\n");
		for(Box b : conjuncts) sb.append(b.toXmlString(indentLevelPlusPlus));		
		sb.append(indentStringPlus + "</ConjunctBlocks>\n");
		if (dependents!=null) {
			sb.append(indentStringPlus + "<DependentBlocks>\n");
			for(Box b : dependents) sb.append(b.toXmlString(indentLevelPlusPlus));		
			sb.append(indentStringPlus + "</DependentBlocks>\n");
		}		
		sb.append(indentString + "</JunctionBox>\n");
		return sb.toString();
	}

	public static BoxJunction getJunctionBoxFromXmlScan(Scanner scan, String firstLine) {
		String[] firstLineSplit = firstLine.split("\\s");
		BoxJunction result = new BoxJunction();
		result.originalTSNodeLabel = Label.getLabel(getValueOfXmlAttribute(firstLineSplit[1]));
		result.originalCat = Integer.parseInt(getValueOfXmlAttribute(firstLineSplit[2]));		
		result.derivedCat = Integer.parseInt(getValueOfXmlAttribute(firstLineSplit[3]));
		String line = scan.nextLine().trim();
		if (line.equals("<ConjunctionWords>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</ConjunctionWords>")) break;
				result.addConjunctionWord(FunctionalWord.getEmptyWordFromXmlLine(line,2));
			} while(true);
			line = scan.nextLine().trim();
		}
		if (line.equals("<EmptyWords>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</EmptyWords>")) break;
				result.addEmptyOrPunctWord(FunctionalWord.getEmptyWordFromXmlLine(line,1));
			} while(true);
			line = scan.nextLine().trim();
		}
		if (line.equals("<PunctWords>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</PunctWords>")) break;
				result.addPunctuationWord(FunctionalWord.getEmptyWordFromXmlLine(line,3));
			} while(true);
			line = scan.nextLine().trim();
		}
		if (line.equals("<ConjunctBlocks>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</ConjunctBlocks>")) break;					
				result.addConjunct(Box.getBoxFromXmlScan(scan, line));
				continue;
			} while(true);
			line = scan.nextLine().trim();
		}
		if (line.equals("<DependentBlocks>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</DependentBlocks>")) break;					
				result.addDependent(Box.getBoxFromXmlScan(scan, line));
				continue;
			} while(true);
			line = scan.nextLine().trim();
		}
		// line: </JunctionBox>
		return result;
	}

	@Override
	public int startPosition(boolean includePunctuation, boolean includeDepOfCurrentBox, boolean includeDepOfRecursiveBox) {
		int min = Integer.MAX_VALUE;
		if (includePunctuation && punctList!=null) min = punctList.first().getPosition();
		if (conjunctions!=null) min = Math.min(min, conjunctions.first().getPosition());
		if (ewList!=null) min = Math.min(min, ewList.first().getPosition());
		if (conjuncts!=null) {
			int firstConjPos = conjuncts.first().startPosition(includePunctuation, includeDepOfRecursiveBox, includeDepOfRecursiveBox);
			min = Math.min(min,firstConjPos);
		}
		if (includeDepOfCurrentBox && dependents!=null) {
			int firstDepPos = dependents.first().startPosition(includePunctuation, includeDepOfRecursiveBox, includeDepOfRecursiveBox);
			min = Math.min(min, firstDepPos);
		}
		return min;
	}
	
	@Override
	public int endPosition(boolean includePunctuation, boolean includeDepOfCurrentBox, boolean includeDepOfRecursiveBox) {
		int max = -1;
		if (includePunctuation && punctList!=null) max = punctList.last().getPosition();
		if (conjunctions!=null) max = Math.max(max, conjunctions.last().getPosition());
		if (ewList!=null) max = Math.max(max, ewList.last().getPosition());
		if (conjuncts!=null) {
			int lastConjPos = conjuncts.last().endPosition(includePunctuation, includeDepOfRecursiveBox, includeDepOfRecursiveBox);
			max = Math.max(max,lastConjPos);
		}
		if (includeDepOfCurrentBox && dependents!=null) {
			int lastDepPos = dependents.last().endPosition(includePunctuation, includeDepOfRecursiveBox, includeDepOfRecursiveBox);
			max = Math.max(max, lastDepPos);
		}
		return max;
	}
	

	@Override
	public int countAllNodes() {		
		int result = 1;
		for(Box b : conjuncts) result += b.countAllNodes();
		if (dependents==null) return result;
		for(Box b : dependents) result += b.countAllNodes();
		return result;
	}
	
	@Override
	public int countAllWords() {		
		int result = (ewList==null ? 0 : ewList.size()) + 
			(punctList==null ? 0 : punctList.size()) +
			(conjunctions==null ? 0 : conjunctions.size());
		if (conjuncts!=null) {
			for(Box b : conjuncts) result += b.countAllWords();
		}
		if (dependents!=null) {
			for(Box b : dependents) result += b.countAllWords();
		}
		return result;
	}

	@Override
	void collectBoxStructure(TreeSet<Box> set) {
		set.add(this);
		for(Box b : conjuncts) b.collectBoxStructure(set);
		if (dependents==null) return;
		for(Box b : dependents) b.collectBoxStructure(set);
	}

	@Override
	public int maxDepth() {
		int maxDepthConj = -1;
		int maxDepthDep = -1;
		for(Box b : conjuncts) {
			int md = b.maxDepth();
			if (md > maxDepthConj) maxDepthConj = md;
		}
		if (maxDepthConj==-1) maxDepthConj++;
		if (dependents!=null) {
			for(Box d : dependents) {
				int md = d.maxDepth();
				if (md > maxDepthDep) maxDepthDep = md;
			}
		}
		maxDepthDep++;
		return maxDepthConj + maxDepthDep;
	}

	@Override
	public boolean isJunctionBlock() {		
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this==obj) return true;
		if (obj instanceof BoxJunction) {
			BoxJunction otherCoord = (BoxJunction)obj;
		    return conjuncts.equals(otherCoord.conjuncts) && 
		    	conjunctions.equals(otherCoord.conjunctions) &&
		    	ewList.equals(otherCoord.ewList) &&
		    	punctList.equals(otherCoord.punctList);
		}
		return false;
	}

	@Override
	public void fillWordArrayTable(Word[] wordsArray,
			IdentityHashMap<Word, Box> table) {
		for(Box b : conjuncts) b.fillWordArrayTable(wordsArray,table);
		if (dependents!=null) {
			for(Box b : dependents) b.fillWordArrayTable(wordsArray,table);
		}
		if (conjunctions!=null) {
			for(FunctionalWord w : conjunctions) {
				wordsArray[w.getPosition()] = w;
				table.put(w, this);
			}
		}
		if (ewList!=null) {
			for(FunctionalWord w : ewList) {
				wordsArray[w.getPosition()] = w;
				table.put(w, this);
			}
		}
		if (punctList!=null) {
			for(FunctionalWord w : punctList) {
				wordsArray[w.getPosition()] = w;
				table.put(w, this);
			}
		}
	}
	
	@Override
	public void fillWordArray(Word[] wordsArray) {
		for(Box b : conjuncts) b.fillWordArray(wordsArray);
		if (dependents!=null) {
			for(Box b : dependents) b.fillWordArray(wordsArray);
		}
		if (conjunctions!=null) {
			for(FunctionalWord w : conjunctions) {
				wordsArray[w.getPosition()] = w;
			}
		}
		if (ewList!=null) {
			for(FunctionalWord w : ewList) {
				wordsArray[w.getPosition()] = w;
			}
		}
		if (punctList!=null) {
			for(FunctionalWord w : punctList) {
				wordsArray[w.getPosition()] = w;
			}
		}
	}
	
	@Override
	public Word leftMostWord(boolean includeDepStdBox, boolean includeDepJunc) {
		Word result = null;
		if (ewList!=null) result = ewList.first();
		if (conjunctions!=null) {
			Word firstConj = conjunctions.first();
			if (result==null || firstConj.position<result.position) result = firstConj; 
		}
		Word leftWordFirstConjuct = this.conjuncts.first().leftMostWord(includeDepStdBox, includeDepJunc);
		if (result==null || leftWordFirstConjuct.position<result.position) result = leftWordFirstConjuct;
		if (includeDepJunc && this.dependents!=null) {
			Word firstDep = this.dependents.first().leftMostWord(includeDepStdBox, includeDepJunc);			
			if (result==null || firstDep.position<result.position) result = firstDep;
		}
		return result;
	}

	@Override
	public Word rightMostWord(boolean includeDepStdBox, boolean includeDepJunc) {
		Word result = null;
		if (ewList!=null) result = ewList.last();
		if (conjunctions!=null) {
			Word lastConj = conjunctions.last();
			if (result==null || lastConj.position>result.position) result = lastConj; 
		}
		Word rightWordlastConjuct = this.conjuncts.last().rightMostWord(includeDepStdBox, includeDepJunc);
		if (result==null || rightWordlastConjuct.position>result.position) result = rightWordlastConjuct;
		if (includeDepJunc && this.dependents!=null) {
			Word lastDep = this.dependents.last().rightMostWord(includeDepStdBox, includeDepJunc);			
			if (result==null || lastDep.position>result.position) result = lastDep;
		}
		return result;
	}

	public Box firstBlock() {
		return conjuncts.first();
	}
	
	public Box lastBlock() {
		return conjuncts.last();
	}
	
	@Override
	public void lowestBlocks(ArrayList<Box> list) {		
		for(Box b : conjuncts) b.lowestBlocksWithDependents(list);
	}

	@Override
	public void lowestBlocksWithDependents(ArrayList<Box> list) {		
		if (dependents==null) {
			for(Box b : conjuncts) b.lowestBlocksWithDependents(list);
			return;
		}
		for(Box d : dependents) d.lowestBlocksWithDependents(list); 		
	}

	@Override
	public int getWidth() {
		if (conjuncts==null) return 1;
		int maxWidth = -1;
		for(Box b : conjuncts) {
			int w = b.getWidthWithDependents(); 
			if (w>maxWidth) maxWidth = w;
		}
		return maxWidth;
	}
	
	@Override
	public int getWidthWithDependents() {
		int result = getWidth();
		if (dependents==null) return result;
		int maxDepWidth = -1;
		for(Box d : dependents) {
			int w = d.getWidthWithDependents(); 
			if (w>maxDepWidth) maxDepWidth = w;
		}
		result += maxDepWidth;
		return result;
	}

	@Override
	public Word getUpperWord() {		
		if (conjunctions!=null) return conjunctions.first();
		if (ewList!=null) return ewList.first();
		//if (punctList!=null) return punctList.first();
		return conjuncts.first().getUpperWord();
		//return punctList.first();
	}

	@Override
	public void uniteVerbs() {
		for(Box b : conjuncts) b.uniteVerbs();
		if (dependents!=null) {
			for(Box d : dependents) d.uniteVerbs(); 
		}		
	}
	
	@Override
	public Box uniteCompoundCoordinatedVerbs() {
		ArrayList<Box> boxesCunction = new ArrayList<Box>(conjuncts); 
		for(Box b : boxesCunction) b = b.uniteCompoundCoordinatedVerbs();
		if (dependents!=null) {
			boxesCunction = new ArrayList<Box>(dependents);
			for(Box d : boxesCunction) d = d.uniteCompoundCoordinatedVerbs(); 
		}		
		return this;
	}
	
	@Override
	public void fixRBJJ() {
		for(Box b : conjuncts) b.fixRBJJ();
		if (dependents!=null) {
			for(Box d : dependents) d.fixRBJJ(); 
		}
	}
	
	
	public int getOriginalCat() {
		if (originalCat!=-2) return originalCat;
		if (conjuncts==null) return originalCat = -1;
		//return originalCat = conjuncts.first().getOriginalCat();
		int[] catFreq = new int[5];
		for(Box b : conjuncts) catFreq[b.getOriginalCat()+1]++;
		int winningCatFreq = -1;
		winningCatFreq = Utility.max(catFreq);
		for(Box b : conjuncts) {
			int boc = b.getOriginalCat();
			if (catFreq[boc+1]==winningCatFreq) return originalCat = boc;
		}		
		return 5; //never happens
	}

	public boolean hasJunctionAmongConjuncts() {
		for(Box b : this.conjuncts) {
			if (b.isJunctionBlock()) return true;
		}
		return false;
	}

	@Override
	public boolean replaceBlock(Box box, Box newBox) {
		if (this.conjuncts.remove(box)) {
			this.conjuncts.add(newBox);
			newBox.isConjunct = true;
			return true;
		}
		if (this.dependents==null) return false;
		if (this.dependents.remove(box)) {
			this.dependents.add(newBox);
			return true;
		}
		return false;
	}

	public boolean matchFullWordsStructure(BoxJunction bj) {
		if (this.conjuncts.size() != bj.conjuncts.size()) return false;
		Iterator<Box> thisIter = this.conjuncts.iterator();
		Iterator<Box> otherIter = bj.conjuncts.iterator();
		while(thisIter.hasNext()) {
			Box thisC = thisIter.next();
			Box otherC = otherIter.next();
			if (!thisC.matchFullWordsStructure(otherC)) return false;			
		}
		return true;
	}

	public void addAllDirectIndirectStdBoxConjuncts(ArrayList<BoxStandard> directIndirectStdBoxConjuncts) {
		for(Box b : this.conjuncts) {
			if (b.isJunctionBlock()) {
				BoxJunction bj = (BoxJunction)b;
				bj.addAllDirectIndirectStdBoxConjuncts(directIndirectStdBoxConjuncts);
			}
			else {
				BoxStandard bs = (BoxStandard)b;
				directIndirectStdBoxConjuncts.add(bs);
			}
		}
		
	}
	
	public BoxStandard getLeftMostStadardBoxConjunct() {
		Box result = this;
		do {
			result = ((BoxJunction)result).conjuncts.first();
		} while(result.isJunctionBlock());
		return (BoxStandard)result;
	}
	
	public BoxStandard getRightMostStadardBoxConjunct() {
		Box result = this;
		do {
			result = ((BoxJunction)result).conjuncts.last();
		} while(result.isJunctionBlock());
		return (BoxStandard)result;
	}

}
