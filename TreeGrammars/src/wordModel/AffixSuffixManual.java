package wordModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class AffixSuffixManual {

	public static String[] affixes5 = new String[]{"inter", "trans", "under"};
	public static String[] affixes4 = new String[]{"over"};
	public static String[] affixes3 = new String[]{"com", "con", "dis", "pre", "pro"};
	public static String[] affixes2 = new String[]{"co", "de", "in", "re", "un"};
	
	public static String[] suffixes5 = new String[]{"ments"};
	public static String[] suffixes4 = new String[]{"ance", "dent", "ence", "ists", "line", "ment", "ship", "time"};
	public static String[] suffixes3 = new String[]{"ans", "ant", "are", "ate", "ble", "cal", "ess", "est", "ful", "ian", "ics", 
		"ing", "ion", "ist", "ive", "man", "ons", "ory", "ous", "son", "tor", "ure"};
	public static String[] suffixes2 = new String[]{"al", "ce", "ck", "cy", "de", "ds", "ed", "er", "es", "et", "ey", "gs", "gy", 
		"ic", "is", "ks", "ld", "le", "ls", "ly", "ne", "rd", "rs", "se", "sh", "sm", "th", "ts", "ty", "ze"};	
	
	public static String[][][] affixesSuffixes = new String[][][]{
		{affixes5,affixes4,affixes3,affixes2},
		{suffixes5,suffixes4,suffixes3,suffixes2}
	};
	
	public final static String noFixTag = "NOFIX";
	
	public static int[][] defaultPriority = new int[][]{
		{1,0},{0,0}, //suffix5, affixes5
		{1,1},{0,1}, //suffix4, affixes4
		{1,2},{1,3}, //suffix3, suffix2
		{0,3},{0,3} //affixes3, affixes2
	};
	
	public static void checkOrder() {
		for(String[][] block : affixesSuffixes) {
			for(String[] list : block) {
				String[] copy = Arrays.copyOf(list, list.length);
				Arrays.sort(copy);
				if (!Arrays.equals(list, copy)) {
					System.err.println("NON SORTED: " + Arrays.toString(list));
				}
			}
		}
	}
	
	public static String tagWord(String word) {
		for(int[] level : defaultPriority) {
			String[] searchArray = affixesSuffixes[level[0]][level[1]];
			boolean affix = level[0]==0;
			int word_length = word.length();
			int fix_length = searchArray[0].length();
			if (word_length<=fix_length) continue;
			String fix = affix ?
					word.substring(0, fix_length) : 
					word.substring(word_length-fix_length);
			if (Arrays.binarySearch(searchArray, fix)>=0) {
				return (affix ? "A_" : "S_") + fix;
			}
		}
		return noFixTag;
	}
	
	public static void main(String[] args) throws IOException {
		//checkOrder();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    String str = "";
	    while (str != null) {
	        System.out.print("<prompt> ");
	        str = in.readLine();
	        System.out.println(str + ": " + tagWord(str));
	    }
	}
	
}
