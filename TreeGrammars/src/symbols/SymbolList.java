package symbols;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

public class SymbolList extends Symbol{

	protected static Hashtable<Collection<Symbol>, Integer> IDtableSymbol 
		= new Hashtable<Collection<Symbol>, Integer>();	
	protected static Vector<Collection<Symbol>> IDtableReverseSymbol 
		= new Vector<Collection<Symbol>>();
	protected static Vector<SymbolList> SymbolVector 
		= new Vector<SymbolList>();
	
	protected static int SymbolCounter;
	
	public static int getSymbolCounter() {
		return SymbolCounter;
	}
	
	public SymbolList(Collection<Symbol> symbol)  {
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
		
    public boolean equals(Object anOtherSymbolList) {
    	if (this == anOtherSymbolList) {
    	    return true;
    	}
    	if (anOtherSymbolList instanceof SymbolList) {
    		SymbolList anotherSymbol = (SymbolList)anOtherSymbolList;
    	    return this.id == anotherSymbol.id;
    	}
    	return false;
    }
    
    public int hashCode() {
    	return id;
    }

    public boolean equals(SymbolList anotherSymbol) {    	
    	return this.id == anotherSymbol.id;
    }
	
}
