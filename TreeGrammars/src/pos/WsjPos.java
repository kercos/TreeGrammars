package pos;

import java.util.Arrays;

public class WsjPos extends OpenClosePos {
	
	public static final String[] closeClassPosSorted = new String[]{"#", "$", "''", ",", 
		"-LRB-", "-RRB-", ".", ":", "DT", "EOS", "EX", "IN", "LS", "MD", "POS", "PRP", 
		"PRP$", "RB", "TO", "WDT", "WP", "WP$", "WRB", "``"};
	public static final String[] openClassPosSorted = new String[]{"CC", "CD", "FW", "JJ", 
		"JJR", "JJS", "NN", "NNP", "NNPS", "NNS", "PDT", "RBR", "RBS", "RP", "SYM", "UH", 
		"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
	public static final String[] allPosSorted = new String[]{"#", "$", "''", ",", "-LRB-", 
		"-RRB-", ".", ":", "CC", "CD", "DT", "EOS", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", 
		"MD", "NN", "NNP", "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", 
		"RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", 
		"WRB", "``"};	
	public static final String[] allPosShortTags = new String[]{"P", "$", "P", "P", "P", 
		"P", "P", "P", "CC", "CD", "DT", "EOS", "EX", "FW", "IN", "ADJ", "ADJ", "ADJ", "LS", 
		"MD", "N", "N", "N", "N", "PDT", "POS", "PRP", "PRP$", "ADV", "ADV", "ADV", 
		"RP", "SYM", "TO", "UH", "V", "V", "V", "V", "V", "V", "WH", "WH", "WH", 
		"WH", "P"};
	public static final String[] verbPosSorted = new String[]{
		"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"
	};
	
	public WsjPos() {
		
	}
	
	public String getSuperShortPos(String pos) {
		int index = Arrays.binarySearch(allPosSorted, pos);
		if (index<0) {
			System.err.println("Pos: " + pos + " doesn't exist!");
			System.exit(0);
		}
		return allPosShortTags[index];
	}
	
	public boolean isOpenClass(String pos) {
		return Arrays.binarySearch(openClassPosSorted, pos)>=0;
	}

}
