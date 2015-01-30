package wordModel;

import java.util.ArrayList;
import java.util.Hashtable;

import symbols.SymbolString;
import tsg.TSNodeLabel;
import util.Utility;

public class WordPosFreq {

	Hashtable<SymbolString, int[]> freqTable;
	
	public WordPosFreq(ArrayList<TSNodeLabel> corpus) {
		freqTable = new Hashtable<SymbolString, int[]>();
		for(TSNodeLabel t : corpus) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for(TSNodeLabel l : lex) {
				String word = l.label();
				String pos = l.parent.label();
				SymbolString wordPos = new SymbolString(word + "_" + pos);
				Utility.increaseInTableInt(freqTable, wordPos);
			}			
		}
	}
	
	public int getFreq(TSNodeLabel l) {
		if (!l.isLexical) return 0;
		String word = l.label();
		String pos = l.parent.label();
		return getFreq(word, pos);
	}
	
	public int getFreq(String word, String pos) {
		SymbolString wordPos = new SymbolString(word + "_" + pos);
		int[] freq = freqTable.get(wordPos);
		if (freq==null) return 0;
		return freq[0];
	}
	
}
