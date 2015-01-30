package tsg;

import java.io.Serializable;
import java.util.HashMap;

import settings.Parameters;

public class TermLabel implements Serializable{

	
	private static final long serialVersionUID = 1L;
	
	protected static HashMap<Label, TermLabel[]> IDtableTermLabel 
		= new HashMap<Label, TermLabel[]>(); // 0:nonLex 1:lex	
	protected static HashMap<Short, TermLabel> IDTermLabel
		= new HashMap<Short, TermLabel>();
	protected static int labelCounter;
	protected static short lexLabelCounter = 1;
	protected static short nonLexLabelCounter = -1;	
	
	public Label label;
	public boolean isLexical;
	public short id;
	
	public static int getLabelCounter() {
		return labelCounter;		
	}
	
	public synchronized static void reset() {		
		IDtableTermLabel.clear();
		labelCounter = 0;
		IDTermLabel.clear();
		lexLabelCounter = 1;
		nonLexLabelCounter = -1;
	}
		
	private TermLabel(Label label, boolean isLexical) {
		this.label = label;
		this.isLexical = isLexical;
		
		if (isLexical) {
			if (lexLabelCounter==Short.MAX_VALUE) {
				Parameters.reportError("TermLabelShort reached max capacity for lex items!");				
			}
			id = lexLabelCounter++;			
		}
		else {
			if (nonLexLabelCounter==Short.MIN_VALUE) {
				Parameters.reportError("TermLabelShort reached max capacity for non-lex items!");				
			}
			id = nonLexLabelCounter--;			
		}
		
	}
	
	public static TermLabel getTermLabel(short id) {
		return IDTermLabel.get(id);
	}
	
	public static boolean isLexical(short id) {
		return id>0;
	}
	
	public synchronized static TermLabel getTermLabel(String labelString, boolean isLexical) {
		Label l = Label.getLabel(labelString);
		return getTermLabel(l,isLexical);
	}
	
	public synchronized static TermLabel acquireTerminalLabel(String labelString) {
		boolean isLexical = labelString.charAt(0)=='"';
		if (isLexical) {
			labelString = labelString.substring(1, labelString.length()-1);
		}
		Label l = Label.getLabel(labelString);
		return getTermLabel(l,isLexical);
	}
	
	public synchronized static TermLabel getTermLabel(Label label, boolean isLexical) {
		int index = isLexical ? 1 : 0;
		TermLabel[] storedArray = IDtableTermLabel.get(label);
		if (storedArray!=null) {
			TermLabel storedLabel = storedArray[index];
			if (storedLabel!=null) return storedLabel;
		}
		else {
			storedArray = new TermLabel[2];
		}
		if (labelCounter==Integer.MAX_VALUE) {
			Parameters.reportError("TermLabel reached max capacity!");
			return null;
		}
		labelCounter++;  
		TermLabel storedLabel = new TermLabel(label, isLexical);
		storedArray[index] = storedLabel;
		IDtableTermLabel.put(label, storedArray);
		IDTermLabel.put(storedLabel.id, storedLabel);
		return storedLabel;
	}
	
	public synchronized static TermLabel getTermLabel(TSNodeLabel t) {
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
	
	public String toStringTex() {
		return toString(true).
			replace("$", "\\$").
			replace("%", "\\%").
			replaceFirst("\"", "``").
			replaceFirst("_","\\\\_");
	}
	
	@Override
	public boolean equals(Object anObject) {
		return this==anObject;
    }
	
    @Override
    public int hashCode() {
    	//return label.hashCode();
    	return id;
    }

	
	public static void main(String[] args) {
		String a = "_";
		System.out.println(a.replaceFirst("_", "\\\\_"));
	}
	
}



