package tsg;

import java.util.Hashtable;

import settings.Parameters;

public class Label {

	protected static Hashtable<String, Label> IDtableLabel = new Hashtable<String, Label>();
	
	protected static int labelCounter;
	
	public String labelString;
	
	public static int getLabelCounter() {
		return labelCounter;		
	}
	
	public static void reset() {		
		IDtableLabel.clear();
		labelCounter = 0;
	}
		
	private Label(String labelString) {
		this.labelString = labelString;
	}
	
	public synchronized static Label getLabel(String labelString) {
		Label storedLabel = IDtableLabel.get(labelString);		
		if (storedLabel!=null) return storedLabel;
		if (labelCounter==Integer.MAX_VALUE) {
			Parameters.reportError("Label reached max capacity!");
			return null;
		}
		labelCounter++;
		storedLabel = new Label(labelString);
		IDtableLabel.put(labelString, storedLabel);
		return storedLabel;
	}
	

	public String toString() {
		return labelString;		
	}
	
	public String toStringWithoutSemTags() {
		int dash_index = labelString.indexOf('-');		
		if (dash_index>0) { //avoid to deal with -NONE-, -RRB-, -RCB-, ...
			return labelString.substring(0,dash_index);
		}
		return labelString;
	}
	
	public String[] getSemTags() {
		return labelString.split("[-=]");
	}
	
	public boolean hasSemTags() {
		return labelString.indexOf('-')>0;
	}
		
	public Label getLabelWithoutSemTags() {
		int dashIndex = labelString.indexOf('-');
		if (dashIndex==-1) return this;
		return getLabel(labelString.substring(0,dashIndex));
	}
	
    public boolean equals(Object anObject) {
    	return this == anObject;
    }
    
    @Override
    public int hashCode() {
    	return labelString.hashCode();
    }

	public static Label[] getLabelArray(String[] lex) {
		Label[] result = new Label[lex.length];
		for(int i=0; i<lex.length; i++) {
			result[i] = Label.getLabel(lex[i]);
		}
		return result;		
	}	
	
	
}
