package tesniere;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;

import symbols.SymbolString;
import tsg.TSNodeLabel;
import util.Utility;

public class FullWordsPosFreq {

	Hashtable<SymbolString, int[]> freqTable;
	
	public FullWordsPosFreq(ArrayList<Box> corpus) {
		freqTable = new Hashtable<SymbolString, int[]>();
		for(Box t : corpus) {
			TreeSet<Box> boxes = t.collectBoxStructure();
			for(Box b : boxes) {
				if (b.isJunctionBlock()) continue;
				BoxStandard bs = (BoxStandard)b;
				SymbolString wordsPos = new SymbolString(getStingFullWords(bs));
				Utility.increaseInTableInt(freqTable, wordsPos);
			}
		}
	}
	
	public String getStingFullWords(BoxStandard bs) {
		if (bs.fwList==null) return "";
		String result = "";
		for(ContentWord fw : bs.fwList) {
			result += fw.word + "_" + fw.posLabel.toString() + "_";
		}
		return result;
	}
	
	public int getFreq(BoxStandard bs) {
		SymbolString wordsPos = new SymbolString(getStingFullWords(bs));
		int[] freq = freqTable.get(wordsPos);
		if (freq==null) return 0;
		return freq[0];
	}
	
	
	
}
