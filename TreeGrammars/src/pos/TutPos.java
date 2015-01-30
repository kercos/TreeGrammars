package pos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import tdg.TDNode;

public class TutPos extends OpenClosePos {
	
	/*
	 * 1.  ADJ (adjectives)
	 * 2.  ADV (adverbs)
	 * 3.  ART (articles)
	 * 4.  CONJ (conjunctions)
	 * 5.  DATE (dates)
	 * 6.  INTERJ (interjections)
	 * 7.  MARKER (markers)
	 * 8.  NOUN (nouns)
	 * 9.  NUM (numbers)
	 * 10. PHRAS (phrasal)
	 * 11. PREDET (predeterminers)
	 * 12. PREP (prepositions)
	 * 13. PRON (pronouns)
	 * 14. PUNCT (punctuation)
	 * 15. SPECIAL (special symbols)
	 * 16. VERB (verbs)
	 */	
	
	public static final String[] allPosSorted = new String[]{
		"ADJ", "ADV", "ART", "CONJ", "DATE", "EOS", "INTERJ", "MARKER", "NOUN", 
		"NUM", "PHRAS", "PREDET", "PREP", "PRON", "PUNCT", "SPECIAL", "VERB" };
	
	public static final String[] closeClassPosSorted = new String[]{
		"ART", "CONJ", "EOS", "PHRAS", "PREDET", "PREP", "PRON", "PUNCT", "SPECIAL"};
	
	public static final String[] openClassPosSorted = new String[]{
		"ADJ", "ADV", "DATE", "INTERJ", "MARKER", "NOUN", "NUM", "VERB"};			
	
	public static final String[] verbPosSorted = new String[]{"VERB"};
	
	public int confidentFreqLimit;
	Hashtable<String, int[]> wordsFreq;
	
	public TutPos() {	
	}
	
	public TutPos(ArrayList<TDNode> training, int confidentFreqLimit) {
		this.confidentFreqLimit = confidentFreqLimit;		
		wordsFreq = new Hashtable<String, int[]>();
		for(TDNode t : training) {
			t.updateLowLexiconFreqTable(wordsFreq);
		}
	}
	
	public boolean isOpenClass(String pos) {
		return Arrays.binarySearch(openClassPosSorted, getSuperShortPos(pos))>=0;		
	}
	
	public boolean isCloseClass(String pos) {
		return Arrays.binarySearch(closeClassPosSorted, getSuperShortPos(pos))>=0;		
	}
	
	public boolean isFreqWord(String lowLex) {
		int[] freq = wordsFreq.get(lowLex);
		if (freq==null) return false;
		return freq[0]>=confidentFreqLimit;
	}

	
	/**
	 * 0: lexPos
	 * 1: lemmaPos
	 * 2: lexShortPos
	 * 3: lemmaShortPos
	 * 4: pos
	 * 5: shortPos
	 */
	// close | open(freq) | open(infreq)
	int[][] reductionTypesLevels = new int[][]{
			{0,0,3}, 	//0: lexPos(0)		| lexPos(0)
			{0,1,3}, 	//1: lexPos(0)		| lemmaPos(1) 
			{0,3,3},	//3: lexPos(0)		| lemmaShortPos(3)
			{1,4,4},	//4: lemmaPos(1)	| pos(4)
			{5,5,5}		//5: shortPos(5)	| shortPos(5)
						//6: no specified		
	};
	
	public String[] standardReductions(TDNode n) {
		String[] result = new String[7];
		for(int i=0; i<5; i++) {
			result[i] = lexPosReduction(n, reductionTypesLevels[i]);
		}
		result[5] = "";
		return result;
	}
	
	public String getLexPos(TDNode n) {
		return getLex(n) + "|" + getPos(n);
	}
	
	public String getLemmaPos(TDNode n) {
		return getLemma(n) + "|" + getPos(n);
	}
	
	public String getLexShortPos(TDNode n) {
		return getLex(n) + "|" + getShortPos(n);
	}
	
	public String getLemmaShortPos(TDNode n) {
		return getLemma(n) + "|" + getShortPos(n);
	}
	
	public String getLex(TDNode n) {
		int sepIndex = n.lex.indexOf('|');
		if (sepIndex==-1) return n.lex.toLowerCase();
		return n.lex.substring(0, sepIndex).toLowerCase();					
	}
	
	public String getLexLemma(TDNode n) {		
		return n.lex.toLowerCase();					
	}
	
	public String getLemma(TDNode n) {
		int sepIndex = n.lex.indexOf('|');
		if (sepIndex==-1) return n.lex.toLowerCase();
		return n.lex.substring(sepIndex+1).toLowerCase();					
	}
	
	public String getPos(TDNode n) {
		return n.postag;
	}
	
	public String getShortPos(TDNode n) {
		return getSuperShortPos(getPos(n));
	}
	
	public String getSuperShortPos(String pos) {
		int sepIndex = pos.indexOf('|');
		if (sepIndex==-1) return pos;
		return pos.substring(0, sepIndex);		
	}
	
	/**
	 * 
	 * @param n
	 * @param reduction_type_ifOpen
	 * @param reduction_type_ifClose
	 * @return
	 * 
	 * 0: lexPos
	 * 1: lemmaPos
	 * 2: lexShortPos
	 * 3: lemmaShortPos
	 * 4: pos
	 * 5: shortPos
	 */
	public String lexPosReduction(TDNode n, int[] reduction_type_clos_freqOpen_infreqOpen) {		
		int type = (isCloseClass(getShortPos(n))) ? 0 :	
			(isFreqWord(getLex(n))) ? 1 : 2;
		int reduction_type = reduction_type_clos_freqOpen_infreqOpen[type];
		switch((reduction_type) ) {
			case 0: return getLexPos(n); 
			case 1: return getLemmaPos(n);
			case 2: return getLexShortPos(n);
			case 3: return getLemmaShortPos(n);
			case 4: return getPos(n);
			case 5: return getShortPos(n);
		}
		return null;
	}

}
