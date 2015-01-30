package tsg;

public class ConstituentLabelSpan {
	Label label;
	int initialIndex, finalIndex;
	
	public ConstituentLabelSpan(Label label, int initialIndex, int finalIndex) {
		this.label = label;
		this.initialIndex = initialIndex;
		this.finalIndex = finalIndex;		
	}
		
	public boolean equals(Object anObject) {
		if (this == anObject) {
		    return true;
		}
		if (anObject instanceof ConstituentLabelSpan) {
			ConstituentLabelSpan c = (ConstituentLabelSpan)anObject;
		    if (c.label.equals(this.label) && 
		    		c.initialIndex==this.initialIndex 
		    		&& c.finalIndex==this.finalIndex) return true;		    
		}		
		return false;
	}
	
	public int getStartIndex() {
		return initialIndex;
	}
	
	public int getEndIndex() {
		return finalIndex;
	}
	
	public Label getLabel() {
		return label;
	}
	
	
	public String toString() {
		return "(" + this.label + ": " + this.initialIndex + "-" + this.finalIndex + ")";
	}

}
