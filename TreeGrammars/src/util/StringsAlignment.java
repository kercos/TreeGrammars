package util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class StringsAlignment {
	
	String[] x, y;
	int M, N;
	// opt[i][j] = length of LCS of x[i..M] and y[j..N]
	double[][] score;
	double[][] matchMatrix;
	char[][] move;
	
	public StringsAlignment(String[] x, String[] y) {
		this.x = x;
		this.y = y;
		M = x.length;
        N = y.length;
        score = new double[M+1][N+1];
        matchMatrix = new double[M+1][N+1];
        move = new char[M+1][N+1];
        initMatrix();
	}
	
	private void initMatrix() {
		// compute length of LCS and all subproblems via dynamic programming
        for (int i = M-1; i >= 0; i--) {
            for (int j = N-1; j >= 0; j--) {
            	double match = computeMatchScore(x[i],y[j]);
            	matchMatrix[i][j]=match;
            	double[] scores = new double[]{score[i+1][j+1] + match, score[i+1][j], score[i][j+1]}; //stay, right, down
            	double bs = Utility.max(scores);
            	int bsi = Utility.maxIndex(scores);
            	move[i][j] = bsi==0 ? 'x' : (bsi==1 ? 'r' : 'd');
            	score[i][j] = bs;
            }
        }
	}
	

	private double computeMatchScore(String a, String b) {
		double lcs = LongestCommonSubsequence.getLCSLength(a, b); 
		return lcs/a.length() + lcs/b.length();
	}

	public ArrayList<Integer> getBestIndexAlignemnt() {
        // recover LCS itself and print it to standard output
        ArrayList<Integer> result = new ArrayList<Integer>();
        
        int i = 0, j = 0, r=0;
        while(i < M && j < N) {
            if (move[i][j]=='x') {
            	result.add(j);
                i++;
                j++;
            }
            else if (move[i][j]=='r') i++;
            else j++;
        }        
        
        return result;
	}
	
	public boolean isBestAlignementContiguous() {
		ArrayList<Integer> bia = getBestIndexAlignemnt();
		int size = bia.size();
		int first = bia.get(0);
		int last = bia.get(size-1);
		return last == (first + size - 1);
	}
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] x = new String("het idee krijgen").split("\\s");
		String[] y = new String("hij heeft het idee gekregen dat zij iets wil doen").split("\\s");
		StringsAlignment SA = new StringsAlignment(x,y);
        System.out.println(SA.getBestIndexAlignemnt());
        System.out.println(SA.isBestAlignementContiguous());        
	}


}
