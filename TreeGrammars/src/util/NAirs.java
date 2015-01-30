package util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;


public class NAirs {
	
	private static int absoluteMax = 15;
	
	int max;
	int[][][][] nair;
	boolean noDiscontinuous;
	
	public NAirs(int max) {
		this(max, false);
	}
	
	public NAirs(int max, boolean noDiscontinuous) {
		this.noDiscontinuous = noDiscontinuous;
		if (max > absoluteMax) max = absoluteMax;
		this.max = max;		
		initNair();
	}
	
	private void initNair() {
		//  binomial(int c, int n) : | ways of choosing n elements from c |
		// c >= n		
		nair = new int[max+1][max+1][][];
		fillNairWithNulls();
		if (noDiscontinuous) {
			for(int c=1; c<=max; c++) {
				for(int n=1; n<=c; n++) {
					nair[c][n] = Utility.n_air(c, n);
				}
			}	
		}
		else {
			for(int c=1; c<=max; c++) {
				for(int n=1; n<=c; n++) {
					nair[c][n] = Utility.n_air_continuous(c, n);
				}
			}
		}		
	}
	
	private void fillNairWithNulls() {
		for(int i=0; i<=max; i++) {
			for(int j=0; j<=max; j++) {
				nair[i][j] = null;
			}
		}
	}
	
	public int[][] get(int c, int n) {		
		return nair[c][n];
	}
	
	/*
	 * s * s+1 * ... * e-1 * e max s,e = 12
	 */
	public static int factorial(int s, int e) {
		int result = s;
		for (int i = s + 1; i <= e; i++)
			result *= i;
		return result;
	}
	
	/*
	 * 1 * 2 * ... * n-1 * n max n = 12
	 */
	public static int factorial(int n) {
		return factorial(1, n);
	}

	
	/**
	 * All possible ways of choosing n elements from c.
	 * 
	 * @param c
	 * @param n
	 * @return
	 */
	public static int binomial(int c, int n) {
		if (n == 0 || n == c)
			return 1;
		if (n == 1 || n == c - 1)
			return c;
		/*
		 * if (c>17 || n>17) { System.err.println("Binomial max variable = 17");
		 * System.exit(-1); }
		 */
		if (n > c / 2)
			return factorial(n + 1, c) / factorial(c - n); // n = c - n
		return factorial(c - n + 1, c) / factorial(n);
	}
	
	/**
	 * Returns all possible sets of n elements taken from c possible elements.
	 * i.e. n_air(4,2): 0 1, 0 2, 0 3, 1 2, 1 3, 2 3
	 * 
	 * @param c
	 * @param n
	 * @return max c,n = 17
	 */
	public static int[][] n_air(int c, int n) {
		int combinations = binomial(c, n);
		int[][] result = new int[combinations][n];
		if (n == 1) {
			int row = 0;
			for (int i = 0; i < c; i++) {
				result[row][0] = i;
				row++;
			}
			return result;
		}
		if (n == 2) {
			int row = 0;
			for (int i = 0; i < c; i++) {
				for (int j = i + 1; j < c; j++) {
					result[row][0] = i;
					result[row][1] = j;
					row++;
				}
			}
			return result;
		}
		int[][] partialResult = n_air(c - 1, n - 1);
		int row = 0;
		for (int i = 0; i < partialResult.length; i++) {
			int k = partialResult[i][n - 2] + 1;
			do {
				for (int j = 0; j < n - 1; j++) {
					result[row][j] = partialResult[i][j];
				}
				result[row][n - 1] = k++;
				row++;
			} while (k < c);
		}
		return result;
	}
	
	public static <T> Set<Set<T>> n_air(Set<T> inputSet, int n) {
		Set<Set<T>> result = new HashSet<Set<T>>();
		int c = inputSet.size();
		Vector<T> v = new Vector<T>(inputSet);		
		int[][] indexComb = n_air(c,n);
		for(int[] setIndex : indexComb) {
			Set<T> set = new HashSet<T>(setIndex.length);
			result.add(set);
			for(int i : setIndex) {
				set.add(v.get(i));
			}			
		}
		return result;		
	}

	
	public static void main(String[] args) {
		
		/* Don't look at this
		NAirs N = new NAirs(10);
		for(int c=0; c<=10; c++) {
			for(int n=0; n<=c; n++) {
				System.out.println("Nair\t" + c + "\t" + n);
				Utility.printIntArray(N.get(c, n));
				System.out.println();
			}			
		}
		*/
		
		// This is to get the indexes
		int n = 5;
		int k = 3;		
		System.out.println(Arrays.deepToString(n_air(n,k)));
		
		// This is to get the actual subsets
		String[] stringList = new String[]{"banana", "apple", "orange", "plum", "pear"};
		HashSet<String> stringSet = new HashSet<String>(Arrays.asList(stringList));
		k = 2;		
		Set<Set<String>> setOfSets = n_air(stringSet, 2);
		System.out.println(setOfSets);
		
		
	}
	
}
