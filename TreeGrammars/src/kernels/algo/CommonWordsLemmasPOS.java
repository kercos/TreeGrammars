package kernels.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import util.IdentityArrayList;

public class CommonWordsLemmasPOS {
	
	public static int minMatchLength = 1;
	public static int maxConsecutivePoSGaps = 5;
	static char[] ignorePunctChar = new char[]{'.',',',':',';','?','!','"'};
	static String[] ignorePunctString = new String[]{"--", "..."};
	
	static {
		Arrays.sort(ignorePunctChar);
		Arrays.sort(ignorePunctString);
	}
	

	public static TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> 
		getAllMaxCommonSubstringsIdentity(String[][] x, String[][] y) {
		
		String[] x_words = x[0];
		String[] x_pos = x[1];
		String[] x_lemmas = x[2];
		
		String[] y_words = y[0];
		String[] y_pos = y[1];
		String[] y_lemmas = y[2];
		
		int lengthX = x_words.length;
        int lengthY = y_words.length;
        
        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[lengthX+2][lengthY+2];
        boolean[][] eq_words = new boolean[lengthX+2][lengthY+2];
        boolean[][] eq_pos = new boolean[lengthX+2][lengthY+2];
        boolean[][] eq_lemmas = new boolean[lengthX+2][lengthY+2];
        boolean[][] matchedSpansX = new boolean[lengthX][lengthX];
        
        // compute length of LCS and all subproblems via dynamic programming
        TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultTable = 
        		new TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>>(); 
        
        int i=0,j=0;
        int s=0,t=0;
        for (s = 1; s <= lengthX; s++) {
            for (t = 1; t <= lengthY; t++) {
            	i = s-1;
            	j = t-1;
                if (x_words[i]==y_words[j]) {
                	eq_words[s][t]=true;
                    opt[s][t] = opt[i][j] + 1;
                }
                else {
                    opt[s][t] = 0;                    
                }
                if (x_pos[i]==y_pos[j]) {
                	eq_pos[s][t]=true;
                }
                if (x_lemmas[i]==y_lemmas[j]) {
                	eq_lemmas[s][t]=true;
                }
            }
        }
        for(s = 1; s <= lengthX+1; s++) {
        	for(t = 1; t <= lengthY+1; t++) {
        		if (!eq_words[s][t]) {
        			i = s-1;
                	j = t-1;   
                	if (eq_words[i][j]) {
                		int prevLength = opt[i][j];
                		if (prevLength<minMatchLength)
                			continue;
                		int startX = i-prevLength;
                		int endX = i;
                		int startY = j-prevLength;
                		//int endY = j;
                		int[] startXY = new int[]{startX, startY};
                		String[] match_words_array = Arrays.copyOfRange(x_words, startX, endX);
                		//int matchLength = endX-startX;
                		match_words_array = checkPunctuation(match_words_array, startXY);
                		if (match_words_array==null)
                			continue;
                		List<String> ext_match = getExtendedMatch(lengthX, lengthY, startXY, match_words_array, 
                				x_pos, x_lemmas, y_pos, y_lemmas);
            			if (matchedSpansX[startXY[0]][startXY[1]])
            				continue;
            			else
            				matchedSpansX[startXY[0]][startXY[1]] = true;
            			
            			addResultIdentity(resultTable, ext_match, startXY);
                    }
        		}            	
        	}
        }
                
        return resultTable;
	}
	
	/*
	public static HashMap<IdentityArrayList<String>, ArrayList<int[]>> getAllMaxCommonSubstringsIdentityFlat(String[] x, String[] y) {
		int M = x.length;
        int N = y.length;
        
        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M+2][N+2];
        boolean[][] eq = new boolean[M+2][N+2];
        
        // compute length of LCS and all subproblems via dynamic programming
        HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultTable = 
        		new HashMap<IdentityArrayList<String>, ArrayList<int[]>>(); 
        int i=0,j=0;
        int s=0,t=0;
        for (s = 1; s <= M; s++) {
            for (t = 1; t <= N; t++) {
            	i = s-1;
            	j = t-1;
                if (x[i]==y[j]) {
                	eq[s][t]=true;
                    opt[s][t] = opt[i][j] + 1;
                }
                else {
                    opt[s][t] = 0;                    
                }
            }
        }
        for(s = 1; s <= M+1; s++) {
        	for(t = 1; t <= N+1; t++) {
        		if (!eq[s][t]) {
        			i = s-1;
                	j = t-1;   
                	if (eq[i][j]) {
                		int prevLength = opt[i][j];
                		if (prevLength<minMatchLength)
                			continue;
                		int startX = i-prevLength;
                		int endX = i;
                		int startY = j-prevLength;
                		//int endY = j;
                		int[] startXY = new int[]{startX, startY};
                		String[] match = Arrays.copyOfRange(x, startX, endX);
                		match = checkPunctuation(match, startXY); 
                		if (match!=null) {
                			addResultIdentity(resultTable, Arrays.asList(match), startXY);
                		}
                    }
        		}            	
        	}
        }
                
        return resultTable;
	}
	*/
	
	private static List<String> getExtendedMatch(int lengthX, int lengthY, int[] startXY, String[] match_words,
			String[] x_pos, String[] x_lemmas, String[] y_pos, String[] y_lemmas) {
		
		ArrayList<String> result = new ArrayList<String>();
		
		int matchLength = match_words.length;
		
		int x = startXY[0]-1;
		int y = startXY[1]-1;
		int currentPoSgapSize = 0;
		int backwardSteps = 0;
		int lastEqualLemmaBackSteps=0;
		LinkedList<String> frontExtension = new LinkedList<String>(); 
		while(x>=0 && y>=0) {
			if (x_pos[x]!=y_pos[y])
				break;
			backwardSteps++;
			if (x_lemmas[x]==y_lemmas[y]) {
				frontExtension.addFirst(x_lemmas[x]);
				lastEqualLemmaBackSteps = backwardSteps;
				currentPoSgapSize=0;
			}
			else {				
				if (++currentPoSgapSize>maxConsecutivePoSGaps)
					break;
				frontExtension.addFirst(x_pos[x]);
			}
			x--;
			y--;
		}
		int removeStartingPoS = backwardSteps - lastEqualLemmaBackSteps;
		for(int i=0; i<removeStartingPoS; i++) {
			frontExtension.removeFirst();
		}
		result.addAll(frontExtension);
		
		result.addAll(Arrays.asList(match_words));
		
		LinkedList<String> tailExtension = new LinkedList<String>();
		x = startXY[0] + matchLength;
		y = startXY[1] + matchLength;
		currentPoSgapSize = 0;
		int forwardSteps = 0;
		int lastEqualLemmaForwardSteps=0;
		while(x<lengthX && y<lengthY) {
			if (x_pos[x]!=y_pos[y])
				break;
			forwardSteps++;
			if (x_lemmas[x]==y_lemmas[y]) {
				tailExtension.addLast(x_lemmas[x]);
				lastEqualLemmaForwardSteps = forwardSteps;
				currentPoSgapSize=0;
			}
			else {
				if (++currentPoSgapSize>maxConsecutivePoSGaps)
					break;
				tailExtension.addLast(x_pos[x]);
			}
			x++;
			y++;
		}
		int removeEndingPoS = forwardSteps - lastEqualLemmaForwardSteps;
		for(int i=0; i<removeEndingPoS; i++) {
			tailExtension.removeLast();
		}
		
		startXY[0] = startXY[0]-lastEqualLemmaBackSteps;
		startXY[1] = startXY[1]+lastEqualLemmaForwardSteps;
		
		result.addAll(tailExtension);
		
		return result;
	}

	private static String[] checkPunctuation(String[] match, int[] startXY) {
		
		int countPunct = 0;
		boolean[] punct = new boolean[match.length];
		for(int i=0; i<match.length; i++) {
			String s = match[i];
			if (isPunctuation(s)) {
				countPunct++;
				punct[i]=true;
			}
		}
		if (match.length-countPunct<minMatchLength)
			return null;
		int lastIndex = match.length-1;
		int countPunctStart=0;
		while(punct[countPunctStart])
			countPunctStart++;
		int countPunctEnd = 0;
		for(int i=lastIndex; i>=0; i--) {
			if (punct[i])
				countPunctEnd++;
			else
				break;
		}
		if (punct[0] || punct[lastIndex]) {
			if (punct[0])
				startXY[0]+=countPunctStart;
			return Arrays.copyOfRange(match, 
					punct[0] ? countPunctStart : 0, 
					punct[lastIndex] ? match.length-countPunctEnd : match.length);
		}		
		return match;
		
	}
	
	public static boolean isPunctuation(String s) {
		if (s.length()==1) 
			return Arrays.binarySearch(ignorePunctChar, s.charAt(0))>=0;
		return Arrays.binarySearch(ignorePunctString, s)>=0;
	}
	
	public static <T> void printAllMaxCommonSubstrings(String[][] x, String[][] y) {
		TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultTable = 
				getAllMaxCommonSubstringsIdentity(x, y);
		for(Entry<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> e : resultTable.descendingMap().entrySet()) {
			int length = e.getKey();
			System.out.println(length + ":");
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> subMap = e.getValue();
			for (Entry<IdentityArrayList<String>, ArrayList<int[]>> f : subMap.entrySet()) {
				System.out.println("\t" + f.getKey().toString() + 
						" @ " + Arrays.deepToString(f.getValue().toArray(new int[][]{})) );
			}			
		}
	}
	
	/*
	 private static <T> void addResult(
			 TreeMap<Integer, HashMap<List<T>, ArrayList<int[]>>> table, T[] match, int[] indexes) {
		int length = match.length;
		HashMap<List<T>, ArrayList<int[]>> subMap = table.get(length);
		List<T> matchList = Arrays.asList(match);
		if (subMap==null) {
			subMap = new HashMap<List<T>, ArrayList<int[]>>();
			table.put(length, subMap);
			ArrayList<int[]> indexList = new ArrayList<int[]>();
			indexList.add(indexes);
			subMap.put(matchList, indexList);
		}
		else {
			ArrayList<int[]> indexList = subMap.get(matchList);
			if (indexList==null) {
				indexList = new ArrayList<int[]>();
				subMap.put(matchList, indexList);
			}
			indexList.add(indexes);
		}
	}
	*/
	 
	 private static <T> void addResultIdentity(
			 TreeMap<Integer, HashMap<IdentityArrayList<T>, ArrayList<int[]>>> table, List<T> match, int[] indexes) {
		 int length = match.size();
		 HashMap<IdentityArrayList<T>, ArrayList<int[]>> subMap = table.get(length);
		 IdentityArrayList<T> matchList = new IdentityArrayList<T>(match);
		 if (subMap==null) {
			 subMap = new HashMap<IdentityArrayList<T>, ArrayList<int[]>>();
			 table.put(length, subMap);
			 ArrayList<int[]> indexList = new ArrayList<int[]>();
			 indexList.add(indexes);
			 subMap.put(matchList, indexList);
		 }
		 else {
			 ArrayList<int[]> indexList = subMap.get(matchList);
			 if (indexList==null) {
				 indexList = new ArrayList<int[]>();
				 subMap.put(matchList, indexList);
			 }
			 indexList.add(indexes);
		 }
	 }
	 
	 @SuppressWarnings("unused")
	private static <T> void addResultIdentity(
			 HashMap<IdentityArrayList<T>, ArrayList<int[]>> table, List<T> match, int[] indexes) {
		 
		 IdentityArrayList<T> matchList = new IdentityArrayList<T>(match);
		 ArrayList<int[]> indexList = table.get(matchList);
		 if (indexList==null) {
			 indexList = new ArrayList<int[]>();
			 table.put(matchList, indexList);
		 }
		 indexList.add(indexes);
	 }



	public static Character[] getCharArray(String x) {
		char[] charResult = x.toCharArray();
		int length = x.length();
		Character[] result = new Character[length];
		for(int i=0; i<length; i++) {
			result[i] = charResult[i];			
		}
		return result;
	}


	public static String[][] getTokenPosLemma(String x) {	// token, pos, lemma			
		String[] lines = x.split("\n");		
		String[] words = new String[lines.length];
		String[] lemmas = new String[lines.length];
		String[] pos = new String[lines.length];
		int i=0;
		for(String l : lines) {
			String[] word_pos_lemma = l.split("\t");
			words[i] = word_pos_lemma[0];
			pos[i] = "||" + word_pos_lemma[1] + "||";
			lemmas[i] = '|' + word_pos_lemma[2] + '|';			
			i++;
		}		
		return new String[][]{words, pos, lemmas};
	}

	public static void internWords(String[][] s) {
		for(int i=0; i<s.length; i++) {
			String[] a = s[i];
			for(int j=0; j<a.length; j++) {
				a[j] = a[j].intern();
			}
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		String x = "Gli	DET	il" + "\n"
				+ "uomini	NOM	uomo" + "\n"
				+ "desiderano	VER	desiderare" + "\n"
				+ "essere	VER	essere" + "\n"
				+ "felici	ADJ	felice" + "\n"
				+ ",	PON	," + "\n"
				+ "soltanto	ADV	soltanto" + "\n"
				+ "essere	VER	essere" + "\n"
				+ "felici	ADJ	felice" + "\n"
				+ ",	PON	," + "\n"
				+ "e	CON	e" + "\n"
				+ "non	ADV	non" + "\n"
				+ "possono	VER	potere" + "\n"
				+ "fare	VER	fare" + "\n"
				+ "a	PRE	a" + "\n"
				+ "meno	ADV	meno" + "\n"
				+ "di	PRE	di" + "\n"
				+ "desiderarlo	VER	desiderare" + "\n"
				+ ".	SENT	.";
		String y = "Non	ADV	non" + "\n"
				+ "posso	VER	potere" + "\n"
				+ "fare	VER	fare" + "\n"
				+ "a	PRE	a" + "\n"
				+ "meno	ADV	meno" + "\n"
				+ "di	PRE	di" + "\n"
				+ "ricordare	VER	ricordare" + "\n"
				+ "gli	DET	il" + "\n"
				+ "organismi	NOM	organismo" + "\n"
				+ "e	CON	e" + "\n"
				+ "gli	DET	il" + "\n"
				+ "ecosistemi	NOM	ecosistema" + "\n"
				+ "che	PRO	che" + "\n"
				+ "sanno	VER	sapere" + "\n"
				+ "come	PRE	come" + "\n"
				+ "vivere	VER	vivere" + "\n"
				+ "qui	ADV	qui" + "\n"
				+ "con	PRE	con" + "\n"
				+ "grazia	NOM	grazia" + "\n"
				+ ",	PON	," + "\n"
				+ "su	PRE	su" + "\n"
				+ "questo	PRO	questo" + "\n"
				+ "pianeta	NOM	pianeta" + "\n"
				+ ".	SENT	.";
		String[][] X = getTokenPosLemma(x);
		String[][] Y = getTokenPosLemma(y);
		internWords(X);
		internWords(Y);
		System.out.println(Arrays.deepToString(X));
		System.out.println(Arrays.deepToString(Y));
        printAllMaxCommonSubstrings(X, Y);
	}
}
