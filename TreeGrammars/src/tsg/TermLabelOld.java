package tsg;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;

import settings.Parameters;

public class TermLabelOld {

	protected static IdentityHashMap<Label, TermLabelOld[]> IDtableTermLabel 
		= new IdentityHashMap<Label, TermLabelOld[]>(); // 0:nonLex 1:lex	
	protected static int labelCounter;
	
	public Label label;
	public boolean isLexical;
	
	public static int getLabelCounter() {
		return labelCounter;		
	}
	
	public static void reset() {		
		IDtableTermLabel.clear();
		labelCounter = 0;
	}
		
	private TermLabelOld(Label label, boolean isLexical) {
		this.label = label;
		this.isLexical = isLexical;
	}
	
	public synchronized static TermLabelOld getTermLabel(String labelString, boolean isLexical) {
		Label l = Label.getLabel(labelString);
		return getTermLabel(l,isLexical);
	}
	
	public synchronized static TermLabelOld acquireTerminalLabel(String labelString) {
		boolean isLexical = labelString.charAt(0)=='"';
		if (isLexical) {
			labelString = labelString.substring(1, labelString.length()-1);
		}
		Label l = Label.getLabel(labelString);
		return getTermLabel(l,isLexical);
	}
	
	public synchronized static TermLabelOld getTermLabel(Label label, boolean isLexical) {
		int index = isLexical ? 1 : 0;
		TermLabelOld[] storedArray = IDtableTermLabel.get(label);
		if (storedArray!=null) {
			TermLabelOld storedLabel = storedArray[index];
			if (storedLabel!=null) return storedLabel;
		}
		else {
			storedArray = new TermLabelOld[2];
		}
		if (labelCounter==Integer.MAX_VALUE) {
			Parameters.reportError("TermLabel reached max capacity!");
			return null;
		}
		labelCounter++;  
		TermLabelOld storedLabel = new TermLabelOld(label, isLexical);
		storedArray[index] = storedLabel;
		IDtableTermLabel.put(label, storedArray);
		return storedLabel;
	}
	
	public synchronized static TermLabelOld getTermLabel(TSNodeLabel t) {
		return getTermLabel(t.label, t.isLexical);
	}
	

	public String toString() {
		return toString(true);
		
	}
	
	public String toString(boolean quotes) {
		if (quotes && this.isLexical)
			return "\"" + label.toString() + "\"";
		return label.toString();
		
	}
	
	public boolean equals(Object anObject) {
		return this==anObject;
    }
    
    @Override
    public int hashCode() {
    	return label.hashCode();
    }	
	
	
}






/*
package tsg;

public class TermLabel {

	public Label label;
	public boolean isLexical;
	
	public TermLabel(Label label, boolean isLexical) {
		this.label = label;
		this.isLexical = isLexical;
	}
	
	public String toString() {		
		if (this.isLexical)
			return "\"" + label.toString() + "\"";
		else
			return label.toString();
	}

	public boolean equals(Object anObject) {
		if (anObject instanceof TermLabel) {
			TermLabel otherTermLabel = (TermLabel) anObject;
			return this.label.equals(otherTermLabel.label) &&
					this.isLexical==otherTermLabel.isLexical;
		}
    	return false;
    }
	
	@Override
    public int hashCode() {
    	return label.hashCode();
    }
	
}
*/