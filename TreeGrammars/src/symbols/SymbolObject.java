package symbols;

import java.util.Hashtable;
import java.util.Vector;

public class SymbolObject extends Symbol{

	protected static Hashtable<Object, Integer> IDtableSymbol = new Hashtable<Object, Integer>();	
	protected static Vector<Object> IDtableReverseSymbol = new Vector<Object>();
	protected static Vector<SymbolObject> SymbolVector = new Vector<SymbolObject>();
	
	protected static int SymbolCounter;
	
	public static int getSymbolCounter() {
		return SymbolCounter;
	}
	
	public SymbolObject(Object symbol)  {
		Integer storedId = IDtableSymbol.get(symbol);
		if (storedId==null) {
			storedId = SymbolCounter++;					
			IDtableSymbol.put(symbol, storedId);
			IDtableReverseSymbol.add(symbol);
			SymbolVector.add(this);
		}
		this.id = storedId;
	}
	
	public Object getOriginalObject() {
		return IDtableReverseSymbol.get(this.id);
	}
	
	/**
	 * Assign a new id to the current lable.
	 * Carefull: all the nodes associated with this Symbol will be renamed.
	 * @param newSymbol
	 */
	public void relabel(Object newSymbol) {
		IDtableSymbol.remove(getOriginalObject());
		Integer storedId = IDtableSymbol.get(newSymbol);
		if (storedId==null) {					
			IDtableSymbol.put(newSymbol, this.id);			
			IDtableReverseSymbol.set(this.id, newSymbol);			
			SymbolVector.set(this.id, this);
		}
		else this.id = storedId;
	}

	public String toString() {
		return getOriginalObject().toString();		
	}
		
    public boolean equals(Object anObject) {
    	if (this == anObject) {
    	    return true;
    	}
    	if (anObject instanceof SymbolObject) {
    		SymbolObject anotherSymbol = (SymbolObject)anObject;
    	    return this.id == anotherSymbol.id;
    	}
    	return false;
    }
    
    public int hashCode() {
    	return id;
    }

    public boolean equals(SymbolObject anotherSymbol) {    	
    	return this.id == anotherSymbol.id;
    }
	
}
