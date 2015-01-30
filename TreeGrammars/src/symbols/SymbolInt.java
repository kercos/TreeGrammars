package symbols;

public class SymbolInt extends Symbol {	
	
	public SymbolInt(int symbol)  {		
		this.id = symbol;
	}	

	public String toString() {
		return Integer.toString(id);				
	}
		
    public boolean equals(Object anObject) {
    	if (this == anObject) {
    	    return true;
    	}
    	if (anObject instanceof SymbolInt) {
    		SymbolInt anotherSymbol = (SymbolInt)anObject;
    	    return this.id == anotherSymbol.id;
    	}
    	return false;
    }
    
    public Object getOriginalObject() {
    	return new Integer(id);
    }
    
    public int hashCode() {
    	return id;
    }

    public boolean equals(SymbolInt anotherSymbol) {    	
    	return this.id == anotherSymbol.id;
    }
	
}
