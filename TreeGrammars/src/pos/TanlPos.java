package pos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import tdg.TDNode;

public class TanlPos extends OpenClosePos {
	
	/**
	 * Short Pos
	 * A  adjective
	 * B  adverb
	 * C  conjunction
	 * D  determiner
	 * E  preposition
	 * F  punctuation
	 * I  interjection
	 * N  numeral
	 * P  pronoun
	 * R  article
	 * S  noun
	 * T  predeterminer
	 * V  verb
	 * X  residual class
	 * 
	 * Fine-grained tags 
	 * A  adjective
	 * AP  possessive adjective
	 * B  adverb
	 * BN  negation adverb
	 * CC  coordinate conjunction
	 * CS  subordinate conjunction
	 * DD  demonstrative determiner
	 * DE  exclamative determiner
	 * DI  indefinite determiner (qualche, ognuna, pochi, alcuni)
	 * DQ  interrogative determiner (quali, quanti))
	 * DR  relative determiner (quale)
	 * E  preposition
	 * EA  articulated preposition (della, grazie_al, prima_del)
	 * FB  balanced punctuation
	 * FC  clause boundary punctuation
	 * FF  comma
	 * FS  sentence boundary punctuation
	 * I  interjection
	 * N  cardinal number
	 * NO  ordinal number
	 * PC  clitic pronoun
	 * PD  demonstrative pronoun (questo, quello, costui, stesso)
	 * PE  personal pronoun (loro, lui, essi, noi)
	 * PI  indefinite pronoun
	 * PP  possessive pronoun
	 * PQ  interrogative pronoun
	 * PR  relative pronoun
	 * RD  determinative article
	 * RI  indeterminative article
	 * S  common noun
	 * SA  abbreviation
	 * SP  proper noun
	 * T  predeterminer
	 * V main verb
	 * VA auxiliary verb
	 * VM modal verb
	 */
	
	public static final String[] allPosSorted = new String[]{"A","AP","B","BN", 
		"CC","CS","DD","DE","DI","DQ","DR","E","EA","EOS","FB","FC","FF", "FS",
		"I","N","NO","PC","PD","PE","PI","PP","PQ","PR","RD","RI", 
		"S","SA","SP","T","V","VA","VM","X"};
	
	public static final String[] allPosShortSorted = new String[]{
		"A","B","C","D","E","EOS","F","I","N","P","R","S","T","V","X"};
	
	// doubts: CS,DE, DI, DR, EA, PI, 
	
	public static final String[] closeClassPosSorted = new String[]{
		"AP","BN","CC","DD","DQ","E","EOS","FB","FC","FF","FS",
		"PC","PD","PE","PP","PQ","PR","RD","RI","T"};
		
	public static final String[] openClassPosSorted = new String[]{
		"A","B","CS","DE","DI","DR","EA","I","N","NO","PI","S","SA","SP","V","VA","VM","X"};
	
	
	public static final String[] verbPosSorted = new String[]{"V","VA","VM"};
	
	public int confidentFreqLimit;
	Hashtable<String, int[]> wordsFreq;
	
	public TanlPos() {
		
	}
	
	public TanlPos(ArrayList<TDNode> training, int confidentFreqLimit) {
		this.confidentFreqLimit = confidentFreqLimit;		
		wordsFreq = new Hashtable<String, int[]>();
		for(TDNode t : training) {
			t.updateLowLexiconFreqTable(wordsFreq);
		}
	}
	
	public boolean isFreqWord(String lowLex) {
		int[] freq = wordsFreq.get(lowLex);
		if (freq==null) return false;
		return freq[0]>=confidentFreqLimit;
	}
		
	public boolean isOpenClass(String pos) {
		return Arrays.binarySearch(openClassPosSorted, pos)>=0;
	}
	
	public boolean isCloseClass(String pos) {
		return Arrays.binarySearch(closeClassPosSorted, getSuperShortPos(pos))>=0;		
	}
	
	/**
	 * 0: lexExtendedPos
	 * 1: lemmaExtendedPos
	 * 2: lexPos
	 * 3: lemmaPos
	 * 4: lexShortPos
	 * 5: lemmaShortPos
	 * 6: extendedPos
	 * 7: pos
	 * 8: shortPos
	 */
	// close | open(freq) | open(infreq)
	int[][] reductionTypesLevels = new int[][]{
			{0,0,5}, 	//0: lexExtendedPos(0)		| lexExtendedPos(0)		| lemmaShortPos(5)
			{0,1,5}, 	//1: lexExtendedPos(0)		| lemmaExtendedPos(1) 	| lemmaShortPos(5)
			{0,1,7},	//2: lexExtendedPos(0)		| lemmaExtendedPos(1)	| lemmaShortPos(5)
			{0,3,7},	//3: lexExtendedPos(0)		| lemmaShortPos(5)		| lemmaShortPos(5)
			{1,5,7},	//4: lemmaExtendedPos(1)	| pos(7)				| pos(7)
			{7,8,8}		//5: shortPos(8)			| shortPos(8)			| pos(7)
						//6: no specified		
	};
	
	
	/**
	 * 0: lexPos
	 * 1: lemmaPos
	 * 2: lexShortPos
	 * 3: lemmaShortPos
	 * 4: pos
	 * 5: shortPos
	 */
	public String[] standardReductions(TDNode n) {
		String[] result = new String[7];
		int type = (isCloseClass(getPos(n))) ? 0 :	(isFreqWord(getLex(n))) ? 1 : 2;		
		for(int i=0; i<6; i++) {
			int reduction_type = reductionTypesLevels[i][type];
			result[i] = lexPosReduction(n, reduction_type);
		}
		result[6] = "";
		return result;
	}
	

	public String lexPosReduction(TDNode n, int reduction_type) {				
		switch((reduction_type) ) {
			case 0: return getLexExtendedPos(n); 
			case 1: return getLemmaExtendedPos(n);
			case 2: return getLexPos(n);
			case 3: return getLemmaPos(n);
			case 4: return getLexShortPos(n);
			case 5: return getLemmaShortPos(n);
			case 6: return getExtendedPos(n);
			case 7: return getPos(n);
			case 8: return getShortPos(n);
		}
		return null;
	}
	
	public String getLex(TDNode n) {
		int sepIndex = n.lex.indexOf('|');
		if (sepIndex==-1) return n.lex.toLowerCase();
		return n.lex.substring(0, sepIndex).toLowerCase();					
	}
	
	public String getLemma(TDNode n) {
		int sepIndex = n.lex.indexOf('|');
		if (sepIndex==-1) return n.lex.toLowerCase();
		return n.lex.substring(sepIndex+1).toLowerCase();					
	}
	
	public String getExtendedPos(TDNode n) {
		return n.postag;
	}
	
	public String getPos(TDNode n) {
		return getPos(n.postag);
	}
	
	public String getPos(String pos) {
		int sepIndex = pos.indexOf('|');
		if (sepIndex==-1) return pos;
		int sepIndex2 = pos.indexOf('|', sepIndex+1);
		if (sepIndex2==-1) return pos;
		return pos.substring(sepIndex+1, sepIndex2);		
	}
	
	public String getShortPos(TDNode n) {
		return getShortPos(n.postag);
	}
		
	public String getShortPos(String pos) {
		int sepIndex = pos.indexOf('|');
		if (sepIndex==-1) return pos;
		return pos.substring(0, sepIndex);		
	}
	
	public String getLexExtendedPos(TDNode n) {
		return getLex(n) + "|" + getExtendedPos(n);
	}
		
	public String getLexPos(TDNode n) {
		return getLex(n) + "|" + getPos(n);
	}
	
	public String getLemmaExtendedPos(TDNode n) {
		return getLemma(n) + "|" + getExtendedPos(n);
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

	@Override
	public String getSuperShortPos(String pos) {
		return getShortPos(pos);
	}

}
