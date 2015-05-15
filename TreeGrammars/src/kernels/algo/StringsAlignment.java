package kernels.algo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import util.IdentityArrayList;
import util.LongestCommonSubsequence;
import util.Utility;

public class StringsAlignment {
	
	static final String gapSymbol = "___".intern();
		
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
        
        int i = 0, j = 0;
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
	
	public int getBestAlignemntGaps() {
        // recover LCS itself and print it to standard output
		int result = 0;
        int i = 0, j = 0;
        boolean inGap = true;
        while(i < M && j < N) {
            if (move[i][j]=='x') {
                i++;
                j++;
                inGap = false;
            }
            else {
            	if (!inGap) {
            		inGap = true;
            		result++;
            	}
            	if (move[i][j]=='r') i++;
            	else j++;
            }            
        }        
        
        return result;
	}
	
	public ArrayList<String> getBestAlignedSubseqWithGaps() {
        // recover LCS itself and print it to standard output
		ArrayList<String> result = new ArrayList<String>();
        int i = 0, j = 0;
        boolean inGap = true;
        while(i < M && j < N) {
            if (move[i][j]=='x') {
            	result.add(x[i]);
                i++;
                j++;
                inGap = false;                
            }
            else {
            	if (!inGap) {
            		result.add(gapSymbol);
            		inGap = true;
            	}
            	if (move[i][j]=='r') i++;
            	else j++;
            }            
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
	
	public static ArrayList<Integer> getBestIndexAlignemnt(String[] substring, String[] sentece) {
		StringsAlignment SA = new StringsAlignment(substring,sentece);
		return SA.getBestIndexAlignemnt();
	}
	
	public static int getBestAlignemntGaps(String[] substring, String[] sentece) {
		StringsAlignment SA = new StringsAlignment(substring,sentece);
		return SA.getBestAlignemntGaps();
	}
	
	public static IdentityArrayList<String> getBestAlignedSubseqWithGaps(String[] substring, String[] sentece) {
		StringsAlignment SA = new StringsAlignment(substring,sentece);
		ArrayList<String> result = SA.getBestAlignedSubseqWithGaps();
		return new IdentityArrayList<String>(result);
	}
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] x = new String("it, a, the, ocean").split(", ");
		String[] y = new String("it, can, be, a, very, complicated, thing, the, ocean, it, a, the, ocean").split(", ");		
		StringsAlignment SA = new StringsAlignment(x,y);
        System.out.println(SA.getBestIndexAlignemnt());
        System.out.println(SA.getBestAlignemntGaps());
        System.out.println(SA.isBestAlignementContiguous());        
        System.out.println(SA.getBestAlignedSubseqWithGaps());
	}


}
