package symbols;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

public class SymbolArray extends Symbol{

	protected static HashMap<Collection<Symbol>, Integer> IDtableSymbol 
		= new HashMap<Collection<Symbol>, Integer>();	
	protected static Vector<Collection<Symbol>> IDtableReverseSymbol 
		= new Vector<Collection<Symbol>>();
	protected static Vector<SymbolArray> SymbolVector 
		= new Vector<SymbolArray>();
	
	protected static int SymbolCounter;
	
	public static int getSymbolCounter() {
		return SymbolCounter;
	}
	
	public SymbolArray(Collection<Symbol> symbol)  {
		Integer storedId = IDtableSymbol.get(symbol);
		if (storedId==null) {
			storedId = SymbolCounter++;					
			IDtableSymbol.put(symbol, storedId);
			IDtableReverseSymbol.add(symbol);
			SymbolVector.add(this);
		}
		this.id = storedId;
	}
	
	public Collection<Symbol> getOriginalCollection() {
		return IDtableReverseSymbol.get(this.id);
	}
	
	public Object getOriginalObject() {
		return getOriginalCollection();
	}
	
	/**
	 * Assign a new id to the current lable.
	 * Carefull: all the nodes associated with this Symbol will be renamed.
	 * @param newSymbol
	 */
	public void relabel(Collection<Symbol> newSymbol) {
		IDtableSymbol.remove(getOriginalCollection());
		Integer storedId = IDtableSymbol.get(newSymbol);
		if (storedId==null) {					
			IDtableSymbol.put(newSymbol, this.id);			
			IDtableReverseSymbol.set(this.id, newSymbol);			
			SymbolVector.set(this.id, this);
		}
		else this.id = storedId;
	}

	public String toString() {
		return getOriginalCollection().toString();		
	}
		
    public boolean equals(Object anOtherSymbolArray) {
    	if (this == anOtherSymbolArray) {
    	    return true;
    	}
    	if (anOtherSymbolArray instanceof SymbolArray) {
    		SymbolArray anotherSymbol = (SymbolArray)anOtherSymbolArray;
    	    return this.id == anotherSymbol.id;
    	}
    	return false;
    }
    
    public int hashCode() {
    	return id;
    }

    public boolean equals(SymbolArray anotherSymbol) {    	
    	return this.id == anotherSymbol.id;
    }
	
}
