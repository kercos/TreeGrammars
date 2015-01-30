package tesniere;

import tsg.Label;
import tsg.TSNodeLabel;

/**
 * @author chiara
 * 
 * A simple entity is composed of one full word.
 *
 */
public class FunctionalWord extends Word {
	
	int wordType; //-1: not defined yet, 0:contenteWord, 1:functionalWord, 2:conjunction, 3:punct
	
	public FunctionalWord(int wordType) {
		this.wordType = wordType;
	}
	
	public FunctionalWord(String w, TSNodeLabel p, int posit, int wordType) {
		word = w;
		posLabel = p.label;		
		TSNodeLabel parent = p.parent;
		this.wordType = wordType;
		TSNodeLabel grandParent = (parent==null) ? null : parent.parent;
		posParentLabel = (parent==null) ? null :  p.parent.label;
		posGrandParentProle = (grandParent==null) ? -1 : grandParent.prole();		
		setPosition(posit);
	}
	
	public FunctionalWord(String w, Label pos, Label posParent, int posit, int grandParentProle, int wordType) {
		word = w;
		posLabel = pos;
		posParentLabel = posParent;
		this.posGrandParentProle = grandParentProle;
		this.wordType = wordType;
		setPosition(posit);
	}
	
	public FunctionalWord clone() {
		return new FunctionalWord(word, posLabel, posParentLabel, position, posGrandParentProle, wordType);
	}
	
	public String toString() {		
		return "<" + word + "," + posLabel + "," + "empty" + "," + getPosition() + ">";
	}
	
	@Override
	public String toXmlString() {
		return "<" + "EmptyWord" + " Word=" + dq + word + dq + " PosLabel=" + dq + posLabel + dq + " Position=" + dq + getPosition() + dq + " />";  		
	}
	
	public static FunctionalWord getEmptyWordFromXmlLine(String line, int wordType) {
		FunctionalWord result = new FunctionalWord(wordType);
		String[] lineSplit = line.split("\\s");
		result.word = getValueOfXmlAttribute(lineSplit[1]);
		result.posLabel = Label.getLabel(getValueOfXmlAttribute(lineSplit[2]));
		result.setPosition(Integer.parseInt(getValueOfXmlAttribute(lineSplit[3])));
		return result;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}
	
	//0:contenteWord, 1:functionalWord, 2:conjunction, 3:punct
	public int defineWordType() {
		if (wordType!=-1) return wordType; // if conjunction (2) should be already defined
		if (this.isPunctuation()) return wordType = 3; //punct
		return wordType = 1; //functional word		
	}
		
	@Override
	public int wordType() {
		return wordType;
	}

}
