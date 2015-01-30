package tsg;

import java.util.Hashtable;
import java.util.Vector;

import settings.Parameters;

public class LabelOld {

	protected static Hashtable<String, Integer> IDtableLabel = new Hashtable<String, Integer>();
	//protected static Vector<String> IDtableReverseLabel = new Vector<String>();
	protected static Vector<LabelOld> labelVector = new Vector<LabelOld>();
	
	protected static int labelCounter;
	
	public int id;
	public String labelString;
	
	public static int getLabelCounter() {
		return labelCounter;		
	}
	
	public static void reset() {		
		IDtableLabel.clear();
		//IDtableReverseLabel.clear();
		labelVector.clear();
		labelCounter = 0;
	}
	
	/*public Label(String labelString)  {
		Integer storedId = IDtableLabel.get(labelString);
		if (storedId==null) {
			storedId = labelCounter++;		
			if (storedId==Integer.MAX_VALUE) {
				storedId = -1;
				Parameters.reportError("Label reached max capacity!");
				return;
			}
			IDtableLabel.put(labelString, storedId);
			//IDtableReverseLabel.add(label);
			labelVector.add(this);
		}
		this.id = storedId;
	}*/
	
	private LabelOld(int id, String labelString) {
		this.id = id;
		this.labelString = labelString;
	}
	
	public synchronized static LabelOld getLabel(String labelString) {
		Integer storedId = IDtableLabel.get(labelString);		
		if (storedId==null) {
			if (labelCounter==Integer.MAX_VALUE) {
				storedId = -1;
				Parameters.reportError("Label reached max capacity!");
				return null;
			}
			storedId = labelCounter++;					
			IDtableLabel.put(labelString, storedId);
			//IDtableReverseLabel.add(label);
			LabelOld result = new LabelOld(storedId, labelString);
			labelVector.add(result);
		}
		return labelVector.get(storedId);
	}
	
	/**
	 * Assign a new id to the current lable.
	 * Carefull: all the nodes associated with this label will be renamed.
	 * @param newLabel
	 */
	/*public void relabel(String newLabel) {
		Integer storedId = IDtableLabel.get(newLabel);
		if (storedId==null) {
			storedId = labelCounter++;			
			IDtableLabel.put(newLabel, storedId);
			IDtableReverseLabel.add(newLabel);
			labelVector.add(this);
		}
		this.id = storedId;
	}*/

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
		
	public LabelOld getLabelWithoutSemTags() {
		int dashIndex = labelString.indexOf('-');
		if (dashIndex==-1) return this;
		return getLabel(labelString.substring(0,dashIndex));
	}
	
    public boolean equals(Object anObject) {
    	return this == anObject;
    }
    
    public int hashCode() {
    	return labelString.hashCode();
    }

	public static LabelOld[] getLabelArray(String[] lex) {
		LabelOld[] result = new LabelOld[lex.length];
		for(int i=0; i<lex.length; i++) {
			result[i] = LabelOld.getLabel(lex[i]);
		}
		return result;		
	}	
	
	
}
