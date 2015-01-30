package util;

import java.util.Arrays;

public class LongestCommonSubsequence {
	
	
	public static <T> int getLCSLength(T[] x, T[] y) {
		int M = x.length;
        int N = y.length;
        
        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M+1][N+1];
        boolean[][] eq = new boolean[M+1][N+1];
        
        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M-1; i >= 0; i--) {
            for (int j = N-1; j >= 0; j--) {
                if (x[i].equals(y[j])) {
                	eq[i][j]=true;
                    opt[i][j] = opt[i+1][j+1] + 1;
                }
                else 
                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
            }
        }
        return opt[0][0];
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] getLCS(T[] x, T[] y) {
		int M = x.length;
        int N = y.length;
        
        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M+1][N+1];
        boolean[][] eq = new boolean[M+1][N+1];
        
        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M-1; i >= 0; i--) {
            for (int j = N-1; j >= 0; j--) {
                if (x[i].equals(y[j])) {
                	eq[i][j]=true;
                    opt[i][j] = opt[i+1][j+1] + 1;
                }
                else 
                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
            }
        }
        
        // recover LCS itself and print it to standard output
        int resultLength = opt[0][0];
        T[] result = (T[]) new Object[resultLength];
        
        int i = 0, j = 0, r=0;
        while(i < M && j < N) {
            if (eq[i][j]) {
            	result[r++] = x[i];
                i++;
                j++;
            }
            else if (opt[i+1][j] >= opt[i][j+1]) i++;
            else                                 j++;
        }        
        
        return result;
	}
	

	
	 private static Character[] getCharArray(String x) {
		char[] charResult = x.toCharArray();
		int length = x.length();
		Character[] result = new Character[length];
		for(int i=0; i<length; i++) {
			result[i] = charResult[i];			
		}
		return result;
	}

	public static void main1(String[] args) {
	        String x = "fdasfdsaff";
	        String y = "fdsagfssa";
	        int M = x.length();
	        int N = y.length();

	        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
	        int[][] opt = new int[M+1][N+1];

	        // compute length of LCS and all subproblems via dynamic programming
	        for (int i = M-1; i >= 0; i--) {
	            for (int j = N-1; j >= 0; j--) {
	                if (x.charAt(i) == y.charAt(j))
	                    opt[i][j] = opt[i+1][j+1] + 1;
	                else 
	                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
	            }
	        }

	        // recover LCS itself and print it to standard output
	        int i = 0, j = 0;
	        while(i < M && j < N) {
	            if (x.charAt(i) == y.charAt(j)) {
	                System.out.print(x.charAt(i));
	                i++;
	                j++;
	            }
	            else if (opt[i+1][j] >= opt[i][j+1]) i++;
	            else                                 j++;
	        }
	        System.out.println();

	}
	
	public static int getLCSLength(String x, String y) {
		Character[] X = getCharArray(x);
        Character[] Y = getCharArray(y);
        return getLCSLength(X, Y);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String x = "fdasfdsaff";
        String y = "fdsagfssa";
        Character[] X = getCharArray(x);
        Character[] Y = getCharArray(y);
        System.out.println(Arrays.toString(getLCS(X,Y)));
	}


}
