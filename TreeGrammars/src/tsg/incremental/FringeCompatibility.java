package tsg.incremental;

import java.util.HashMap;
import java.util.HashSet;

import tsg.TermLabel;

public class FringeCompatibility {


	public static boolean isCompatible(TermLabel[] fringe, TermLabel[] sentence, 
			int startIndexSentence, HashMap<TermLabel,HashSet<TermLabel>> posLexTable) {
		int lengthX = fringe.length;
		int lengthY = sentence.length;
		if (lengthX>lengthY)
			return false;
		boolean[][] matrix = new boolean[lengthX][lengthY];
		Boolean[][] supportMatrix = new Boolean[lengthX][lengthY];
		int lowestRightCornerY=0;
		for(int x=0; x<lengthX; x++) {
			TermLabel f = fringe[x];
			boolean fIsNonTerminalNonPos = !f.isLexical && !posLexTable.containsKey(f);
			for(int y=startIndexSentence; y<lengthY; y++) {
				TermLabel s = sentence[y];
				boolean m = fIsNonTerminalNonPos || s==f || 
						(posLexTable.containsKey(f) && posLexTable.get(f).contains(s));				
				matrix[x][y] = m;
				if (m && x==lengthX-1)
					lowestRightCornerY = y;
			}			
		}				
		//printMatrix(matrix,lengthX,lengthY);
		return solveMatrix(matrix,supportMatrix,lengthX-1,lowestRightCornerY);
	}
	
	public static boolean isCompatibleLex(TermLabel[] fringe, TermLabel[] sentence,
			int startIndexSentence) {
		int lengthX = fringe.length;
		int lengthY = sentence.length;
		if (lengthX>lengthY)
			return false;
		boolean[][] matrix = new boolean[lengthX][lengthY];
		Boolean[][] supportMatrix = new Boolean[lengthX][lengthY];
		int lowestRightCornerY=0;
		for(int x=0; x<lengthX; x++) {
			TermLabel f = fringe[x];
			boolean fIsNonLex = !f.isLexical;
			for(int y=startIndexSentence; y<lengthY; y++) {
				TermLabel s = sentence[y];
				boolean m = fIsNonLex || s==f;				
				matrix[x][y] = m;
				if (m && x==lengthX-1)
					lowestRightCornerY = y;
			}			
		}				
		//printMatrix(matrix,lengthX,lengthY);
		return solveMatrix(matrix,supportMatrix,lengthX-1,lowestRightCornerY);
	}
	
	
	public static void printMatrix(boolean[][] matrix, int lengthX, int lengthY) {
		StringBuilder sb = new StringBuilder();
		for(int y=0; y<lengthY; y++) {
			for(int x=0; x<lengthX; x++) {
				sb.append("\t" + (matrix[x][y] ? 1 : "_"));
			}
			sb.append("\n");
		}	
		System.out.println(sb.toString());
	}


	private static boolean solveMatrix(boolean[][] m, Boolean[][] S, int x, int y) {
		if (!m[x][y])
			return false;
		if (S[x][y] != null) {
			return S[x][y];
		}
		if (x==0)
			return true;
		if (y==0)
			return false;
		return S[x][y] = solveMatrix(m,S,x-1,y) || solveMatrix(m,S,x,y-1) || solveMatrix(m,S,x-1,y-1);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}


}
