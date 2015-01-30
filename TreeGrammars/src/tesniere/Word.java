package tesniere;

import java.util.Arrays;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;

public abstract class Word extends Entity {

	public static final String[] emptyPosSorted = new String[]{"#" , "''", ",", 
		"-LRB-", "-RRB-", ".", ":", "CC", "DT" , "EX", "IN", "MD", "POS",  
		"RP", "SYM", "TO", "WDT", "WRB", "``"};
	public static final String[] fullPosSorted = new String[]{"$", "AUX", "AUXG", "CD", "FW", "JJ", 
		"JJR", "JJS", "LS", "NN", "NNP", "NNPS", "NNS", "PDT", "PRP", "PRP$",
		"RB", "RBR", "RBS", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WP", "WP$"};
	public static final String[] allPosSorted = new String[]{"#", "$", "''", ",", "-LRB-", 
		"-RRB-", ".", ":", "AUX", "AUXG", "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", 
		"MD", "NN", "NNP", "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", 
		"RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", 
		"WRB", "``"};
	public static final String[] properNounsSorted = new String[]{"NNP","NNPS"};
	public static final String[] conjunctionsSorted = new String[]{"CC","CONJP"};
	public static final String[] comaColonAndConjunctionsSorted = new String[]{",",":","CC","CONJP"};
	public static final String[] quotationSorted = new String[]{"''", "``"};
	public static final String[] punctuationSorted = new String[]{"''", ",", 
		"-LRB-", "-RRB-", ".", ":", "``"};
	public static final String[] verbsSorted = new String[]{"AUX", "AUXG", "MD", "VB", "VBD", "VBG", 
		"VBN", "VBP", "VBZ"};	
	public static final String[] nounsSorted = new String[]{"$","DT","NN", "NNP", "NNPS", "NNS","PRP","WP"};
	public static final String[] adjsSorted = new String[]{"CD","JJ", "JJR", "JJS", "PRP$", "WP$"};
	public static final String[] adverbTypesSorted = new String[]{"ADVP", "RB", "RBR", "RBS"};	
	public static final String[] adverbsSorted = new String[]{"RB", "RBR", "RBS"};	
		
	public static final String[] emptyPairsSorted = getFullPosEmptyPairSorted();
	
	public static String dq = "\"";
	
	private static String[] getFullPosEmptyPairSorted() {
		String[] emptyPairsSorted = new String[] {
				
				"even though",								
				"more like",
				"more than",
				"other than",
				"rather than",
				//"likely to",
				//"unlikely to",
				//"such as",								
				
				//"according to", \\(verb)
				"across from",
				"ahead of",
				"along with",
				"alongside of",
				"apart from",
				"as for",
				"as from",
				"as of",
				"as per",
				"as to",
				"aside from",
				//"away from", \\ x is away from y (170)
				//"based on", \\(verb)
				"because of",
				"close by",
				"close to",
				//"compared to", \\(verb)
				//"compared with", \\(verb)
				//"due to", \\(verb)
				//"depending on", \\(verb)
				"except for",
				"exclusive of",
				"contrary to",
				//"followed by", \\(verb)
				"inside of",
				"instead of",
				"irrespective of",
				"next to",
				"near to",
				"off of",
				"out of",
				"outside of",
				//"owing to", \\(verb)
				"preliminary to",
				"preparatory to",
				"previous to",
				"prior to",
				"pursuant to",
				"regardless of",
				"subsequent to",
				"such as",
				"thanks to",
				"together with"
		};
		Arrays.sort(emptyPairsSorted);
		return emptyPairsSorted;
	}
	
	public static String getSingularPoS(String cat) {
		if (cat.equals("NNPS")) return "NNP";
		if (cat.equals("NNS")) return "NN";
		return cat;
	
	}
	
	public static final String[] emptyTripleSorted = getFullPosEmptyTripleSorted();
	
	private static String[] getFullPosEmptyTripleSorted() {
		String[] emptyTripleSorted = new String[] {
				"as much as",
				"as long as",
								
				"by means of",
				
				"in accordance with",
				"in addition to",
				"in case of",
				"in front of",
				"in lieu of",
				"in place of",
				"in spite of",
				"in order to",
				
				"on account of",
				"on behalf of",
				"on top of",
				
				"with respect to",				
				"with regard to"
		};
		Arrays.sort(emptyTripleSorted);
		return emptyTripleSorted;
	}

	String word;
	Label posLabel;
	Label posParentLabel;
	int position;
	int posGrandParentProle;
	
	public static boolean hasOnlyProperNounDaughers(TSNodeLabel node) {
		for(TSNodeLabel d : node.daughters) {					
			if (!Word.isProperNoun(d.label())) return false;
		}
		return true;
	}
	
	public static boolean isProperNoun(String pos) {
		return Arrays.binarySearch(properNounsSorted, pos)>=0;
	}
	
	public boolean isPunctuation() {
		return Arrays.binarySearch(punctuationSorted, posLabel.toString())>=0;		
	}	
	
	public static boolean isPunctuation(String s) {
		return Arrays.binarySearch(punctuationSorted, s)>=0;
	}
	
	
	public boolean isVerb() {
		return Arrays.binarySearch(verbsSorted, posLabel.toString())>=0;
	}
	
	public boolean isVerbOrAdjVerb() {
		if (isVerb()) return true;
		if (!posLabel.toString().equals("JJ")) return false;		
		if (posParentLabel!=null && posParentLabel.toString().equals("VP")) {
			Parameters.logPrintln("Found an adjective which should be a verb: " + word);
			return true;
		}
		return false;
	}
	
	public boolean isNoun() {
		return Arrays.binarySearch(nounsSorted, posLabel.toString())>=0;		
	}
	
	public boolean isNounOrNPnumb() {
		if (isNoun()) return true;
		return (posLabel.toString().equals("CD") && posParentLabel.toString().equals("NP") && posGrandParentProle==1); // number identified as NP
	}
	
	public boolean isAdjective() {
		return Arrays.binarySearch(adjsSorted, posLabel.toString())>=0;		
	}
	
	public boolean isAdverb() {
		return Arrays.binarySearch(adverbsSorted, posLabel.toString())>=0;		
	}
	
	public static boolean isAdverbTypeCat(String cat) {
		return Arrays.binarySearch(adverbTypesSorted, cat)>=0;
	}
	
	public static boolean isVerb(String s) {
		return Arrays.binarySearch(verbsSorted, s)>=0;
	}
	
	public static String[] emptyPosFullIf_NP_ADVP = new String[]{"DT","IN","WDT"};
	public static String[] NP_ADVP = new String[]{"ADVP","NP"};
	
	public static boolean isEmpty(TSNodeLabel terminal, boolean report) {		
		TSNodeLabel pos = terminal.parent;
		String posLabel = pos.label();		
		TSNodeLabel firstParentWithSiblings = pos;
		if (!isEmpty(posLabel)) return false;		
		while(firstParentWithSiblings.isUniqueDaughter()) {
			firstParentWithSiblings = firstParentWithSiblings.parent;	
		}
		if (firstParentWithSiblings.parent!=null) {
			if (pos!=firstParentWithSiblings) {
				String firstParentLabel = firstParentWithSiblings.label.toStringWithoutSemTags();
				if (Arrays.binarySearch(emptyPosFullIf_NP_ADVP, posLabel)>=0 
						&& (Arrays.binarySearch(NP_ADVP, firstParentLabel))>=0) { 
					if (report) Parameters.logPrintln("Found empty word being an NP/ADVP: " + pos.daughters[0]);
					return false;
				}
				else if (posLabel.equals("MD") && firstParentLabel.equals("VP")) {
					if (report) Parameters.logPrintln("Found empty word MD being a single daugher of VP: " + pos.daughters[0]);
					return false;
				}
			}
		}	
		return true;
	}

	
	public static int isEmptyLookAtNextAndPrevious(TSNodeLabel terminal, boolean report) {		
		TSNodeLabel pos = terminal.parent;
		String posLabel = pos.label();		
		TSNodeLabel firstParentWithSiblings = pos;
		boolean posIsEmpty = isEmpty(posLabel);
		int result = posIsEmpty ? 1 : 0;
		while(firstParentWithSiblings.isUniqueDaughter()) {
			firstParentWithSiblings = firstParentWithSiblings.parent;	
		}		
		if (firstParentWithSiblings.parent!=null) {
			if (posIsEmpty && pos!=firstParentWithSiblings) {
				String firstParentLabel = firstParentWithSiblings.label.toStringWithoutSemTags();
				if ( Arrays.binarySearch(emptyPosFullIf_NP_ADVP, posLabel)>=0 && 
						(Arrays.binarySearch(NP_ADVP, firstParentLabel))>=0) {					 
							if (report) Parameters.logPrintln("Found empty word being an NP/ADVP: " + pos.daughters[0]);
							result = 0;
				}
				else if (posLabel.equals("MD") && firstParentLabel.equals("VP")) {
					if (report) Parameters.logPrintln("Found empty word MD being a single daugher of VP: " + pos.daughters[0]);
					result = 0;
				}
			}
			
			//if (posIsEmpty) return true;			
			
			TSNodeLabel rightWord = firstParentWithSiblings.nextWord();
			if (rightWord==null) return result;
			//while (!rightWord.isTerminal()) rightWord =  rightWord.daughters[0];
			String pair = terminal.label() + " " + rightWord.label();
			pair = pair.toLowerCase();				
			if (isEmptyPair(pair)) {
				if (report) Parameters.logPrintln("Found empty pair of words: " + pair);
				return 2;			
			}
			
			TSNodeLabel leftWord = firstParentWithSiblings.previousWord();
			if (leftWord==null) return result;
			//while (!leftWord.isTerminal()) leftWord =  leftWord.daughters[leftWord.prole()-1];
			String triple = leftWord.label() + " " + terminal.label() + " " + rightWord.label();
			triple = triple.toLowerCase();				
			if (isEmptyTriple(triple)) {
				if (report) Parameters.logPrintln("Found empty triple of words: " + triple);
				return 3;			
			}
		}				 
		return result;
	}
	

	public static boolean isEmpty(String pos) {
		return (Arrays.binarySearch(emptyPosSorted, pos)>=0);
	}
	
	private static boolean isEmptyPair(String pos) {
		return (Arrays.binarySearch(emptyPairsSorted, pos)>=0);
	}
	
	private static boolean isEmptyTriple(String pos) {
		return (Arrays.binarySearch(emptyTripleSorted, pos)>=0);
	}
	
	public static Word getWord(String w, TSNodeLabel p, int posit, int wordType) {
		if (isEmpty(p, true)) return new FunctionalWord(w, p, posit, wordType);
		else return new ContentWord(w,p,posit);
	}
	
	public String getLex() {
		return word;
	}
	
	public String getPos() {
		return posLabel.toString();
	}
	
	/*public int compareTo(Word anotherWord) {
		int thisPos = this.getPosition();
		int otherPos = anotherWord.getPosition();
		return (thisPos<otherPos ? -1 : (thisPos==otherPos) ? 0 : 1);
	}*/
	
	public boolean equals(Object obj) {
		if (this==obj) return true;
		if (obj instanceof Word) {
			Word otherWord = (Word)obj;
			return (word.equals(otherWord.word) && posLabel.toString().equals(otherWord.posLabel.toString())
					&& getPosition()==otherWord.getPosition());
		}
		return false;
	}
	
	public abstract boolean isEmpty();
	
	/**
	 * 0:content, 1:functional, 2:conjunction, 3:punct
	 * @return
	 */
	public abstract int wordType();

	public void setPosition(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}
	
	@Override
	public int endPosition() {
		return position;
	}

	@Override
	public int startPosition() {
		return position;
	}


	public static boolean isConjunction(TSNodeLabel node) {
		if (node==null) return false;
		return isConjunction(node.label());
	}
	
	public static boolean isConjunction(String s) {
		return (Arrays.binarySearch(conjunctionsSorted, s)>=0);
	}

	public static boolean isPos(String s) {
		return (Arrays.binarySearch(allPosSorted, s)>=0);		
	}
	
	public static String getValueOfXmlAttribute(String attributeValue) {
		return attributeValue.substring(attributeValue.indexOf('=')+2, attributeValue.length()-1);
	}
	
	public boolean sameType(Word w) {
		return this.isEmpty()==w.isEmpty();
	}

	public abstract String toString();
	public abstract String toXmlString();

	public TSNodeLabel toPhraseStructurePosWord() {
		TSNodeLabel pos = new TSNodeLabel(this.posLabel, false);
		TSNodeLabel word = new TSNodeLabel(Label.getLabel(this.word), true);
		pos.assignUniqueDaughter(word);
		return pos;
	}
	
	public TSNodeLabel toPhraseStructureLexWord() {
		TSNodeLabel pos = new TSNodeLabel(Label.getLabel(this.posLabel + "_" + this.word), false);
		TSNodeLabel word = new TSNodeLabel(Label.getLabel(this.word), true);
		pos.assignUniqueDaughter(word);
		return pos;
	}
			
}
