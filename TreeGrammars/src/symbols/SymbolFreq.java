package symbols;


public class SymbolFreq implements Comparable<SymbolFreq>{

	Symbol symbol;
	int freq;
	
	public SymbolFreq(Symbol s, int f) {
		symbol = s;
		freq = f;
	}
	
	public int compareTo(SymbolFreq o) {		
		int otherFreq = o.freq;
		return (freq<otherFreq ? -1 : 
			(freq==otherFreq ? 
					new Integer(symbol.id).compareTo(new Integer(o.symbol.id)) : 1));		
	}
	
	public void increaseFreq() {
		freq++;
	}
	
	public int freq() {
		return freq;
	}
	
	public Symbol symbol() {
		return symbol;
	}
	
	public String toString() {
		return symbol.toString() + "(" + freq + ")";
	}

}
