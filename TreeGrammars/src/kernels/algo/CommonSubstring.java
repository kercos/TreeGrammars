package kernels.algo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;

import util.IdentityArrayList;

public class CommonSubstring {

	public static int minMatchLength = 1;
	static char[] ignorePunctChar = new char[] { '.', ',', ':', ';', '?', '!', '"' };
	static String[] ignorePunctString = new String[] { "--", "..." };

	static {
		Arrays.sort(ignorePunctChar);
		Arrays.sort(ignorePunctString);
	}

	public static TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> getAllMaxCommonSubstringsIdentityLength(String[] x, String[] y) {
		int M = x.length;
		int N = y.length;

		// opt[i][j] = length of LCS of x[i..M] and y[j..N]
		int[][] opt = new int[M + 2][N + 2];
		boolean[][] eq = new boolean[M + 2][N + 2];

		// compute length of LCS and all subproblems via dynamic programming
		TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultTable = new TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>>();
		int i = 0, j = 0;
		int s = 0, t = 0;
		for (s = 1; s <= M; s++) {
			for (t = 1; t <= N; t++) {
				i = s - 1;
				j = t - 1;
				if (x[i] == y[j]) {
					eq[s][t] = true;
					opt[s][t] = opt[i][j] + 1;
				} else {
					opt[s][t] = 0;
				}
			}
		}
		for (s = 1; s <= M + 1; s++) {
			for (t = 1; t <= N + 1; t++) {
				if (!eq[s][t]) {
					i = s - 1;
					j = t - 1;
					if (eq[i][j]) {
						int prevLength = opt[i][j];
						if (prevLength < minMatchLength)
							continue;
						int startX = i - prevLength;
						int endX = i;
						int startY = j - prevLength;
						// int endY = j;
						int[] startXY = new int[] { startX, startY };
						String[] match = Arrays.copyOfRange(x, startX, endX);
						match = checkPunctuation(match, startXY);
						if (match != null) {
							addResultIdentity(resultTable, match, startXY);
						}
					}
				}
			}
		}

		return resultTable;
	}

	/**
	 * Return matching common string with initial indexes
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static HashMap<IdentityArrayList<String>, ArrayList<int[]>> getAllMaxCommonSubstringsIdentityIndexes(String[] x, String[] y) {

		int M = x.length;
		int N = y.length;

		// opt[i][j] = length of LCS of x[i..M] and y[j..N]
		int[][] opt = new int[M + 2][N + 2];
		boolean[][] eq = new boolean[M + 2][N + 2];

		// compute length of LCS and all subproblems via dynamic programming
		HashMap<IdentityArrayList<String>, ArrayList<int[]>> resultTable = 
				new HashMap<IdentityArrayList<String>, ArrayList<int[]>>();
		int i = 0, j = 0;
		int s = 0, t = 0;
		for (s = 1; s <= M; s++) {
			for (t = 1; t <= N; t++) {
				i = s - 1;
				j = t - 1;
				if (x[i] == y[j]) {
					eq[s][t] = true;
					opt[s][t] = opt[i][j] + 1;
				} else {
					opt[s][t] = 0;
				}
			}
		}
		for (s = 1; s <= M + 1; s++) {
			for (t = 1; t <= N + 1; t++) {
				if (!eq[s][t]) {
					i = s - 1;
					j = t - 1;
					if (eq[i][j]) {
						int prevLength = opt[i][j];
						if (prevLength < minMatchLength)
							continue;
						int startX = i - prevLength;
						int endX = i;
						int startY = j - prevLength;
						// int endY = j;
						int[] startXY = new int[] { startX, startY };
						String[] match = Arrays.copyOfRange(x, startX, endX);
						match = checkPunctuation(match, startXY);
						if (match != null) {
							addResultIdentity(resultTable, match, startXY);
						}
					}
				}
			}
		}

		return resultTable;
	}

	public static HashSet<IdentityArrayList<String>> getAllMaxCommonSubstringsIdentity(String[] x, String[] y) {

		int M = x.length;
		int N = y.length;

		// opt[i][j] = length of LCS of x[i..M] and y[j..N]
		int[][] opt = new int[M + 2][N + 2];
		boolean[][] eq = new boolean[M + 2][N + 2];

		// compute length of LCS and all subproblems via dynamic programming
		HashSet<IdentityArrayList<String>> resultSet = new HashSet<IdentityArrayList<String>>();
		int i = 0, j = 0;
		int s = 0, t = 0;
		for (s = 1; s <= M; s++) {
			for (t = 1; t <= N; t++) {
				i = s - 1;
				j = t - 1;
				if (x[i] == y[j]) {
					eq[s][t] = true;
					opt[s][t] = opt[i][j] + 1;
				} else {
					opt[s][t] = 0;
				}
			}
		}
		
		/*
		String[] xPrint = flatForPrint(x);
		String[] yPrint = flatForPrint(y);
		System.out.println("Eq array: ");
		Utility.printChart(eq, yPrint, xPrint);
		System.out.println("\nOpt array: ");
		Utility.printChart(opt, yPrint, xPrint);
		*/
		
		for (s = 1; s <= M + 1; s++) {
			for (t = 1; t <= N + 1; t++) {
				if (!eq[s][t]) {
					i = s - 1;
					j = t - 1;
					if (eq[i][j]) {
						int prevLength = opt[i][j];
						if (prevLength < minMatchLength)
							continue;
						int startX = i - prevLength;
						int endX = i;
						int startY = j - prevLength;
						// int endY = j;
						int[] startXY = new int[] { startX, startY };
						String[] match = Arrays.copyOfRange(x, startX, endX);
						match = checkPunctuation(match, startXY);
						if (match != null) {
							IdentityArrayList<String> matchList = new IdentityArrayList<String>(Arrays.asList(match));
							resultSet.add(matchList);
						}
					}
				}
			}
		}

		return resultSet;
	}

	final static String unk = new String("|<unknown>|").intern();
	
	public static HashSet<IdentityArrayList<String>> getAllMaxCommonSubstringsWPL(String[][] x, String[][] y) {
		
		int M = x.length;
		int N = y.length;

		// opt[i][j] = length of LCS of x[i..M] and y[j..N]
		int[][] opt = new int[M + 2][N + 2];		
		byte[][] wpl = new byte[M + 2][N + 2];
		boolean[][] eq = new boolean[M + 2][N + 2];

		HashSet<IdentityArrayList<String>> resultSet = new HashSet<IdentityArrayList<String>>();
		int i = 0, j = 0;
		int s = 0, t = 0;

		
		// filling opt with -1 and 0 on the borders
		for (s = 1; s <= M; s++) {
			for (t = 1; t <= N; t++) {
				opt[s][t]=-1;
			}
		}
		for (s = 0; s < M+2; s++)
			opt[s][0]=0;
		for (t = 0; t < N+2; t++)
			opt[0][t]=0;
		
		// compute length of LCS and all subproblems via dynamic programming
		
		// calculating wpl (1:p, 2:l, 3:w)
		for (s = 1; s <= M; s++) {
			for (t = 1; t <= N; t++) {
				i = s - 1;
				j = t - 1;
				String[] a = x[i];
				String[] b = y[j];
				// note that 2 words could match in lemma but not in POS (e.g., is-was VBZ-VBD)
				// in some cases they can match in word but not in lemma (exception and mistakes)
				if (a[0] == b[0]) { //word 
					wpl[s][t] = 3;	
					eq[s][t] = true;
				}
				else if (a[2] == b[2] && a[2]!=unk) { //lemma
					wpl[s][t] = 2;
					eq[s][t] = true;
				}
				else if (a[1] == b[1]) { //pos
					wpl[s][t] = 1;
					eq[s][t] = true;
				} 
			}
		}
		
		// calculating opt
		for (s = 1; s <= M; s++) {
			for (t = 1; t <= N; t++) {				
				if (opt[s][t]!=-1)
					continue;
				i = s - 1;
				j = t - 1;
				//String[] a = x[i];
				//String[] b = y[j];
				if (wpl[s][t] > 1) { //word || lemma
						opt[s][t] = opt[i][j] + 1;
						continue;
				}
				if (eq[i][j] && wpl[i][j]>1) { //previous matches in lemma (or word)						 		
			 		int lastLemmaMatch = 0;
			 		int max = Math.min(M-s, N-t);
			 		int i1 = i,j1 = j;
			 		for(int k=0; k<max; k++) {
			 			i1++;
			 			j1++;
			 			if (eq[i1][j1]) {
			 				int nextPWL = wpl[i1][j1]; 
				 			if (nextPWL>1)
				 				lastLemmaMatch = k;							 								
				 			continue;
			 			}
			 			break;						 			
			 		}
			 		if (lastLemmaMatch>0) {
			 			int previousOpt = opt[i][j];
			 			i1 = i; j1 = j;
				 		for(int k=0; k<lastLemmaMatch; k++) {
				 			opt[++i1][++j1] = ++previousOpt;
				 		}
				 		continue;
			 		}				 		
				}
				opt[s][t] = 0;
			}
		}
		
		/*
		String[] xflat = flatForPrintWPL(x);
		String[] yflat = flatForPrintWPL(y);
		System.out.println("WPL array: ");
		Utility.printChart(wpl, yflat, xflat);
		System.out.println("\nOpt array: ");
		Utility.printChart(opt, yflat, xflat);
		*/
		
		//retrieving sequences
		for (s = 1; s <= M + 1; s++) {
			for (t = 1; t <= N + 1; t++) {
				if (!eq[s][t] || opt[s][t]==0) { // 0 or 1: a sequence must end when there is a no match or a POS match
					i = s - 1;
					j = t - 1;
					if (eq[i][j] && opt[i][j]>0) {
						int prevLength = opt[i][j];
						if (prevLength < minMatchLength)
							continue;
						int startX = i - prevLength;
						int endX = i;
						int startY = j - prevLength;
						// int endY = j;
						int[] startXY = new int[] { startX, startY };
						String[] match = getMatchWPL(x, startX, startY, endX, wpl);
						match = checkPunctuation(match, startXY);
						if (match != null) {
							IdentityArrayList<String> matchList = new IdentityArrayList<String>(Arrays.asList(match));
							resultSet.add(matchList);
						}
					}
				}
			}
		}
		
		return resultSet;
		
	}
	
	public static String[] flatForPrint(String[] x) {
		int l = x.length;
		String[] result = new String[l+2];
		result[0] = "";
		for(int i=0; i<l; i++) {
			result[i+1] = x[i]; 
		}
		result[l+1] = "";
		return result;
	}
	
	public static String[] flatForPrintWPL(String[][] x) {
		int l = x.length;
		String[] result = new String[l+2];
		result[0] = "";
		for(int i=0; i<l; i++) {
			result[i+1] = x[i][0] + '_' + x[i][1] + '_' + x[i][2]; 
		}
		result[l+1] = "";
		return result;
	}

	//eq = 0:nomatch, 1:pos, 2:lemma, 3:word
	//WPL = 0:word, 1:pos, 2:lemma
	static int[] eqToWPL = new int[]{-1, 1, 2, 0};

	private static String[] getMatchWPL(String[][] x, int startX, int startY, int endX, byte[][] wpl) {
		int length = endX - startX;
		String[] result = new String[length];
		int cx = startX;
		int cy = startY;
		for(int i=0; i<length; i++) {
			int index = eqToWPL[wpl[cx+1][cy+1]];
			result[i] = x[cx][index];
			cx++;
			cy++;
		}
		return result;
	}

	private static String[] checkPunctuation(String[] match, int[] startXY) {

		int countPunct = 0;
		boolean[] punct = new boolean[match.length];
		for (int i = 0; i < match.length; i++) {
			String s = match[i];
			if (isPunctuation(s)) {
				countPunct++;
				punct[i] = true;
			}
		}
		if (match.length - countPunct < minMatchLength)
			return null;
		int lastIndex = match.length - 1;
		int countPunctStart = 0;
		while (punct[countPunctStart])
			countPunctStart++;
		int countPunctEnd = 0;
		for (int i = lastIndex; i >= 0; i--) {
			if (punct[i])
				countPunctEnd++;
			else
				break;
		}
		if (punct[0] || punct[lastIndex]) {
			if (punct[0])
				startXY[0] += countPunctStart;
			return Arrays.copyOfRange(match, punct[0] ? countPunctStart : 0, punct[lastIndex] ? match.length - countPunctEnd : match.length);
		}
		return match;

	}

	public static boolean isPunctuation(String s) {
		if (s.length() == 1)
			return Arrays.binarySearch(ignorePunctChar, s.charAt(0)) >= 0;
		return Arrays.binarySearch(ignorePunctString, s) >= 0;
	}

	public static <T> void printAllMaxCommonSubstrings(String[] x, String[] y) {
		TreeMap<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> resultTable = getAllMaxCommonSubstringsIdentityLength(x, y);
		for (Entry<Integer, HashMap<IdentityArrayList<String>, ArrayList<int[]>>> e : resultTable.descendingMap().entrySet()) {
			int length = e.getKey();
			System.out.println(length + ":");
			HashMap<IdentityArrayList<String>, ArrayList<int[]>> subMap = e.getValue();
			for (Entry<IdentityArrayList<String>, ArrayList<int[]>> f : subMap.entrySet()) {
				System.out.println("\t" + f.getKey().toString() + " @ " + Arrays.deepToString(f.getValue().toArray(new int[][] {})));
			}
		}
	}

	/*
	 * private static <T> void addResult( TreeMap<Integer, HashMap<List<T>,
	 * ArrayList<int[]>>> table, T[] match, int[] indexes) { int length =
	 * match.length; HashMap<List<T>, ArrayList<int[]>> subMap =
	 * table.get(length); List<T> matchList = Arrays.asList(match); if
	 * (subMap==null) { subMap = new HashMap<List<T>, ArrayList<int[]>>();
	 * table.put(length, subMap); ArrayList<int[]> indexList = new
	 * ArrayList<int[]>(); indexList.add(indexes); subMap.put(matchList,
	 * indexList); } else { ArrayList<int[]> indexList = subMap.get(matchList);
	 * if (indexList==null) { indexList = new ArrayList<int[]>();
	 * subMap.put(matchList, indexList); } indexList.add(indexes); } }
	 */

	private static <T> void addResultIdentity(TreeMap<Integer, HashMap<IdentityArrayList<T>, ArrayList<int[]>>> table, T[] match, int[] indexes) {
		int length = match.length;
		HashMap<IdentityArrayList<T>, ArrayList<int[]>> subMap = table.get(length);
		IdentityArrayList<T> matchList = new IdentityArrayList<T>(Arrays.asList(match));
		if (subMap == null) {
			subMap = new HashMap<IdentityArrayList<T>, ArrayList<int[]>>();
			table.put(length, subMap);
			ArrayList<int[]> indexList = new ArrayList<int[]>();
			indexList.add(indexes);
			subMap.put(matchList, indexList);
		} else {
			ArrayList<int[]> indexList = subMap.get(matchList);
			if (indexList == null) {
				indexList = new ArrayList<int[]>();
				subMap.put(matchList, indexList);
			}
			indexList.add(indexes);
		}
	}

	private static <T> void addResultIdentity(HashMap<IdentityArrayList<T>, ArrayList<int[]>> table, T[] match, int[] indexes) {

		IdentityArrayList<T> matchList = new IdentityArrayList<T>(Arrays.asList(match));
		ArrayList<int[]> indexList = table.get(matchList);
		if (indexList == null) {
			indexList = new ArrayList<int[]>();
			table.put(matchList, indexList);
		}
		indexList.add(indexes);
	}

	public static Character[] getCharArray(String x) {
		char[] charResult = x.toCharArray();
		int length = x.length();
		Character[] result = new Character[length];
		for (int i = 0; i < length; i++) {
			result[i] = charResult[i];
		}
		return result;
	}
	

	private static String[][] unflatString(String flat) {
		String[] unfold = flat.split("\\], \\[");
		int l = unfold.length;
		String[][] result = new String[l][];
		for(int i=0; i<l; i++) {
			String s = unfold[i];
			if (s.startsWith("[["))
				s = s.substring(2);
			if (s.endsWith("]]"))
				s = s.substring(0,s.length()-2);
			result[i] = s.split(", ");			
		}
		return result;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		/*
		System.out.println("Enter firse sentence with space for token separation");
		BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
	    String x = bufferRead.readLine();
	    System.out.println("Enter second sentence with space for token separation");
	    String y = bufferRead.readLine();
	    String[] X = x.split("\\s+");
		String[] Y = y.split("\\s+");
		for (int i = 0; i < X.length; i++) {
			X[i] = X[i].intern();
		}
		for (int i = 0; i < Y.length; i++) {
			Y[i] = Y[i].intern();
		}
		HashSet<IdentityArrayList<String>> match = getAllMaxCommonSubstringsIdentity(X, Y);
		//System.out.println(Arrays.toString(X));
		//System.out.println(Arrays.toString(Y));
		System.out.println("Longest Matching Substrings:");
		System.out.println(match);
		*/
		
		
		String x = "uno due tre quattro cinque";
		String y = "due uno due due tre quattro cinque";
		String[] X = x.split("\\s+");
		String[] Y = y.split("\\s+");
		for (int i = 0; i < X.length; i++) {
			X[i] = X[i].intern();
		}
		for (int i = 0; i < Y.length; i++) {
			Y[i] = Y[i].intern();
		}
		HashSet<IdentityArrayList<String>> match = getAllMaxCommonSubstringsIdentity(X, Y);
		System.out.println(Arrays.toString(X));
		System.out.println(Arrays.toString(Y));
		System.out.println(match);
		
		/*
		String[][] X = new String[][]{
				{"anche","<C>","|anche|"},
				{"se","<P>","|se|"},
				{"non","<A>","|non|"},
				{"lo","<PRN>","|lo|"},
				{"so","<V>","|sapere|"},
				{"bene","<A>","|bene|"},
				{"non","<A>","|non|"},
				{"posso","<V>","|potere|"},
				{"fare","<V>","|fare|"},
				{"a","<P>","|a|"},
				{"meno","<N>","|meno|"},
				{"di","<P>","|di|"},
				{"te","<PRN>","|te|"}
		};
		String[][] Y = new String[][]{ //"non possono fare a meno di te, questo lo so bene"};
			{"non","<A>","|non|"},
			{"possono","<V>","|potere|"},
			{"fare","<V>","|fare|"},
			{"a","<P>","|a|"},
			{"meno","<N>","|meno|"},
			{"di","<P>","|di|"},
			{"te","<PRN>","|te|"},
			{",","<,>","|,|"},
			{"questo","<ADJ>","|questo|"},
			{"lo","<PRN>","|lo|"},
			{"sanno","<V>","|sapere|"},
			{"bene","<A>","|bene|"}
		};
		for (int i = 0; i < X.length; i++) {
			for(int j=0; j<3; j++) {
				X[i][j] = X[i][j].intern();
			}			
		}
		for (int i = 0; i < Y.length; i++) {
			for(int j=0; j<3; j++) {
				Y[i][j] = Y[i][j].intern();
			}			
		}
		HashSet<IdentityArrayList<String>> match = getAllMaxCommonSubstringsWPL(X, Y);
		System.out.println(Arrays.deepToString(X));
		System.out.println(Arrays.deepToString(Y));
		System.out.println(match);
		*/
		
		/*
		String[][] X = new String[][]{ 
				{"sotto","ADV","sotto"},
				{"la","DET:def","il"},
				{"pressione","NOM","pressione"},
				{"del","PRE:det","del"},
				{"clima","NOM","clima"},
				{"che","PRO:rela","che"},
				{"cambia","VER:pres","cambiare"}
		};
		String[][] Y = new String[][]{ //"non possono fare a meno di te, questo lo so bene"};
				{"sottovalutò","VER:remo","sottovalutare"},
				{"l'","DET:def","il"},
				{"importanza","NOM","importanza"},
				{"del","PRE:det","del"},
				{"clima","NOM","clima"},
				{"nell'","PRE:det","nel"},
				{"evoluzione","NOM","evoluzione"},
				{"della","PRE:det","del"},
				{"pigmentazione","NOM","pigmentazione"}
		};
		for (int i = 0; i < X.length; i++) {
			for(int j=0; j<3; j++) {
				X[i][j] = X[i][j].toLowerCase().intern();
			}			
		}
		for (int i = 0; i < Y.length; i++) {
			for(int j=0; j<3; j++) {
				Y[i][j] = Y[i][j].toLowerCase().intern();
			}			
		}
		minMatchLength = 2;
		HashSet<IdentityArrayList<String>> match = getAllMaxCommonSubstringsWPL(X, Y);
		//System.out.println(Arrays.deepToString(X));
		//System.out.println(Arrays.deepToString(Y));
		System.out.println(match);
		*/
		
		/*
		String xflat = "[[può, <ver:remo>, |<unknown>|], [essere, <ver:infi>, |essere|], [una, <det:indef>, |una|], [cosa, <nom>, |cosa|], [davvero, <adv>, |davvero|], [complicata, <ver:pper>, |complicare|], [,, <pon>, |,|], [l', <det:def>, |il|], [oceano, <nom>, |oceano|], [., <sent>, |.|]]";
		String yflat = "[[ora, <adv>, |ora|], [,, <pon>, |,|], [questo, <pro:demo>, |questo|], [gioco, <nom>, |gioco|], [del, <pre:det>, |del|], [monopoli, <nom>, |monopolio|], [può, <ver:pres>, |potere|], [essere, <ver:infi>, |essere|], [utilizzato, <ver:pper>, |utilizzare|], [come, <pre>, |come|], [una, <det:indef>, |una|], [metafora, <nom>, |metafora|], [per, <pre>, |per|], [comprendere, <ver:infi>, |comprendere|], [la, <det:def>, |il|], [società, <nom>, |società|], [e, <con>, |e|], [la, <det:def>, |il|], [sua, <pro:poss>, |suo|], [struttura, <nom>, |struttura|], [gerarchica, <adj>, |gerarchico|], [,, <pon>, |,|], [in, <pre>, |in|], [cui, <pro:rela>, |cui|], [alcune, <pro:indef>, |alcuno|], [persone, <nom>, |persona|], [posseggono, <ver:pres>, |possedere|], [molta, <pro:indef>, |molto|], [ricchezza, <nom>, |ricchezza|], [ed, <con>, |ed|], [uno, <det:indef>, |uno|], [status, <nom>, |status|], [elevato, <adj>, |elevato|], [,, <pon>, |,|], [e, <con>, |e|], [molte, <pro:indef>, |molto|], [altre, <adj>, |altro|], [invece, <adv>, |invece|], [no, <adv>, |no|], [., <sent>, |.|]]";
		String[][] X = unflatString(xflat);
		String[][] Y = unflatString(yflat);
		for (int i = 0; i < X.length; i++) {
			for(int j=0; j<3; j++) {
				X[i][j] = X[i][j].intern();
			}			
		}
		for (int i = 0; i < Y.length; i++) {
			for(int j=0; j<3; j++) {
				Y[i][j] = Y[i][j].intern();
			}			
		}
		HashSet<IdentityArrayList<String>> match = getAllMaxCommonSubstringsWPL(X, Y);
		System.out.println(Arrays.deepToString(X));
		System.out.println(Arrays.deepToString(Y));
		System.out.println(match);
		*/
	}


}
