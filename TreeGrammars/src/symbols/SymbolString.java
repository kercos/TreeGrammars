package symbols;

import java.util.Hashtable;
import java.util.Vector;

public class SymbolString extends Symbol{

	private static final long serialVersionUID = 1L;
	protected static Hashtable<String, Integer> IDtableSymbol = new Hashtable<String, Integer>();	
	protected static Vector<String> IDtableReverseSymbol = new Vector<String>();
	protected static Vector<SymbolString> SymbolVector = new Vector<SymbolString>();
	
	protected static int SymbolCounter;
	
	public static int getSymbolCounter() {
		return SymbolCounter;
	}
	
	public SymbolString(String symbol)  {
		Integer storedId = IDtableSymbol.get(symbol);
		if (storedId==null) {
			storedId = SymbolCounter++;					
			IDtableSymbol.put(symbol, storedId);
			IDtableReverseSymbol.add(symbol);
			SymbolVector.add(this);
		}
		this.id = storedId;
	}
	
	public String getOriginalString() {
		return IDtableReverseSymbol.get(this.id);
	}
	
	public Object getOriginalObject() {
		return getOriginalString();
	}
	
	/**
	 * Assign a new id to the current lable.
	 * Carefull: all the nodes associated with this Symbol will be renamed.
	 * @param newSymbol
	 */
	public void relabel(String newSymbol) {
		IDtableSymbol.remove(getOriginalString());
		Integer storedId = IDtableSymbol.get(newSymbol);
		if (storedId==null) {					
			IDtableSymbol.put(newSymbol, this.id);			
			IDtableReverseSymbol.set(this.id, newSymbol);			
			SymbolVector.set(this.id, this);
		}
		else this.id = storedId;
	}

	public String toString() {
		return getOriginalString().toString();		
	}
		
    public boolean equals(Object anObject) {
    	if (this == anObject) {
    	    return true;
    	}
    	if (anObject instanceof SymbolString) {
    		SymbolString anotherSymbol = (SymbolString)anObject;
    	    return this.id == anotherSymbol.id;
    	}
    	return false;
    }
    
    public int hashCode() {
    	return id;
    }

    public boolean equals(SymbolString anotherSymbol) {    	
    	return this.id == anotherSymbol.id;
    }
	
}
