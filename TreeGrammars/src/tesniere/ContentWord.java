package tesniere;

import tsg.Label;
import tsg.TSNodeLabel;

/**
 * @author fedja
 * 
 * A simple entity is composed of one full word.
 *
 */
public class ContentWord extends Word {		
	
	public ContentWord() {		
	}
	
	public ContentWord(String w, TSNodeLabel p, int posit) {
		word = w;
		posLabel = p.label;
		TSNodeLabel parent = p.parent;
		TSNodeLabel grandParent = (parent==null) ? null : parent.parent;
		posParentLabel = (parent==null) ? null :  p.parent.label;
		posGrandParentProle = (grandParent==null) ? -1 : grandParent.prole();
		setPosition(posit);
	}
	
	@Override
	public String toString() {		
		return "<" + word + "," + posLabel.toString() + "," + "full" + "," + getPosition() + ">";
	}	
	
	@Override
	public String toXmlString() {
		return "<" + "FullWord" + " Word=" + dq + word + dq + " PosLabel=" + dq + posLabel.toString() + dq + " Position=" + dq + getPosition() + dq + " />";  		
	}
	
	public static ContentWord getFullWordFromXmlLine(String line) {
		ContentWord result = new ContentWord();
		String[] lineSplit = line.split("\\s");
		result.word = getValueOfXmlAttribute(lineSplit[1]);
		result.posLabel = Label.getLabel(getValueOfXmlAttribute(lineSplit[2]));
		result.setPosition(Integer.parseInt(getValueOfXmlAttribute(lineSplit[3])));
		return result;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
	
	@Override
	public int wordType() {
		return 0;
	}
	

	public FunctionalWord convertToEmptyWord() {
		return new FunctionalWord(word, posLabel, posParentLabel, position, posGrandParentProle, 1);
	}
	
	// 0,1,2,3: verb, adverb, nount, adj
	public int getCat() {		
		if (isVerbOrAdjVerb()) return 0;
		if (isAdverb()) return 1;
		if (isNounOrNPnumb()) return 2;
		if (isAdjective()) return 3;
		return -1;
	}
	
	// catsString = new String[]{"NUL","VER","ADV","NOU","ADJ"}; 
	public String getCatString() {
		int cat = getCat();
		return Box.catToString(cat);
	}




}
