package ccg;

import util.Utility;

/**
 * 
 * @author fsangati
 *
 * <L CCGcat mod_POS-tag orig_POS-tag word PredArgCat>
 * The original POS tag is the tag assigned to this word 
 * in the PennTreebank. 
 * The modified POS tag might differ from this tag if it 
 * was changed during the translation to CCG. 
 * PredArgCat is another representation of the lexical 
 * category (CCGcat) which encodes
 * the underlying predicate-argument structure
 */
public class CCGTerminalNode extends CCGNode {

	String mod_POS, orig_POS;
	String word;
	String predArgCat;
	
	public CCGTerminalNode(String label) {
		//<L CCGcat mod_POS-tag orig_POS-tag word PredArgCat>
		label = label.substring(3, label.length()-1);		
		//CCGcat mod_POS-tag orig_POS-tag word PredArgCat
		String[] labelSplit = label.split("\\s+");
		this.cat = labelSplit[0];
		this.mod_POS = labelSplit[1];
		this.orig_POS = labelSplit[2];
		this.word = labelSplit[3];
		this.predArgCat = labelSplit[4];	
	}
	
	public String word() {
		return word;
	}
	
	public String toString() {
		return "(<L " + cat + " " +  mod_POS + " " + 
		orig_POS + " " + word + " " + predArgCat + ">)";
	}
	
	public boolean isTerminal() {
		return true;
	}
	
	public boolean isPunctuation() {
		return Utility.isPunctuation(this.cat);
	}
}
