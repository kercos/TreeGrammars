package backoff;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;

import symbols.Symbol;
import symbols.SymbolString;
import util.Utility;

/**
 * A backoff model defines a backoff probabilistic model of an event E 
 * E = A B C D | E F G H
 * P(E) = P (D | E F G H) x 		(1st decomposition)
 * 		  P (C | D E F G H) x		(2nd decomposition)
 * 		  P (B | C D E F G H) x		(3rd decomposition)
 * 		  P (A | B C D E F G H)		(4th decomposition)
 * each decomposition is defined as a backoff probabilistic (undecomposable) event.
 * Required 1: an array of backoff characterization of all the elements (A, B, ..., H):
 * i.e. Symbol[8][] backoffElements;
 * e.g. backoffElements[0] = new Symbol[]{"A_0","A_1","A_2","A_3","A_4"};
 * Required 2: an array of backoff levels (0 being the more detailed) for all decompositions 
 * i.e. int[4][][] backoffLevels;
 * e.g. backoffLevels[0] = new int[][]	{ {0, 0, 0, 0, 0},
 * 										  {0, 1, 2, 2, 3},
 * 										  {1, 2, 2, 4, 4},
 * 										};
 * 
 * @author fsangati
 *
 */
public class BackoffModel_DivHistory extends BackoffModel implements Serializable {
	
	private static final long serialVersionUID = 0L;
		
	int decompositions;
	int[][][] backoffLevels;
	BackoffEvent[] eventDecomposition;
	boolean[] skipDecomposition;
	int[][] groupBackoffLevels;	
	double addFactor, multFactor, lastWeight;
	
	public BackoffModel_DivHistory( int[][][] backoffLevels, 
			boolean[] skipDecomposition, int[][] groupBackoffLevels,
			double addFactor, double multFactor, double lastWeight){				
		
		this.decompositions = backoffLevels.length;
		this.backoffLevels = backoffLevels;
		this.skipDecomposition = skipDecomposition;
		this.groupBackoffLevels = groupBackoffLevels;
		this.addFactor = addFactor;
		this.multFactor = multFactor;
		this.lastWeight = lastWeight;
				
		eventDecomposition = new BackoffEvent[decompositions];
		for(int e=0; e<decompositions; e++) {
			if (skipDecomposition[e]) continue;
			int startIndexCondContext = decompositions - e;
			eventDecomposition[e] = new BackoffEvent(backoffLevels[e], e, startIndexCondContext);
		}				
	}
	
		
	public void increaseInTables(Symbol[][] backoffKeyTable) {
		for(int e = 0; e<decompositions; e++) {
			if (skipDecomposition[e]) continue;
			eventDecomposition[e].increaseInTables(backoffKeyTable);
		}		
	}
	
	public int[] getCondFreqEventFreq(Symbol[][] backoffKeyTable, int decompNumb, int level) {
		if (skipDecomposition[decompNumb]) return null;		
		return eventDecomposition[decompNumb].getFreqCondFreq(backoffKeyTable, level);			
	}
	
	public int[][] getCondFreqEventFreqConjunct(Symbol[][][] backoffKeyTableConjunct, int decompNumb, int level) {
		if (skipDecomposition[decompNumb]) return null;
		int size = backoffKeyTableConjunct.length;
		int[][] result = new int[size][];
		for(int i=0; i<size; i++) {
			Symbol[][] backoffKeyTable = backoffKeyTableConjunct[i];
			result[i] = getCondFreqEventFreq(backoffKeyTable, decompNumb, level);
		}
		return result;			
	}
	
	public String getEventFreqToString(Symbol[][] backoffKeyTable, int decompNumb, int level) {
		if (skipDecomposition[decompNumb]) return null;
		return eventDecomposition[decompNumb].getCondEventToString(backoffKeyTable, level) +
			"\t" + Arrays.toString(eventDecomposition[decompNumb].getFreqCondFreq(backoffKeyTable, level));
	}
	
	public String[] getEventFreqToStringConjunct(Symbol[][][] backoffKeyTableConjunct, int decompNumb, int level) {
		if (skipDecomposition[decompNumb]) return null;
		int size = backoffKeyTableConjunct.length;
		String[] result = new String[size];
		for(int i=0;i<size; i++) {
			Symbol[][] backoffKeyTable = backoffKeyTableConjunct[i];
			result[i] = getEventFreqToString(backoffKeyTable, decompNumb, level);
		}
		return result;
	}
	
	public double getCondProb(Symbol[][] backoffKeyTable) {
		double prob = 1d;
		for(int e=0; e<decompositions; e++) {
			if (skipDecomposition[e]) continue;		
			int totalRowsInThisEvent = backoffLevels[e].length;
			prob *= getCondProb(backoffKeyTable, e, 0, 0, totalRowsInThisEvent);			
		}
		return prob;
	}
	
	public double getCondProbConjunct(Symbol[][][] conjunctBackoffKeyTable) {
		double prob = 1d;
		for(int e=0; e<decompositions; e++) {
			if (skipDecomposition[e]) continue;		
			int totalRowsInThisEvent = backoffLevels[e].length;
			prob *= getCondProbConjunct(conjunctBackoffKeyTable, e, 0, 0, totalRowsInThisEvent);			
		}		
		return prob;
	}
	
	public double getCondProbAverage(Symbol[][][] conjunctBackoffKeyTable) {		
		int size = conjunctBackoffKeyTable.length;
		double totalProb = 0d;
		for(Symbol[][] backoffKeyTable : conjunctBackoffKeyTable) {
			double prob = 1d;
			for(int e=0; e<decompositions; e++) {
				if (skipDecomposition[e]) continue;		
				int totalRowsInThisEvent = backoffLevels[e].length;
				prob *= getCondProb(backoffKeyTable, e, 0, 0, totalRowsInThisEvent);			
			}
			totalProb += prob;
		}	
		totalProb = totalProb / size;
		return totalProb;
	}
	
	private double getCondProb(Symbol[][] backoffKeyTable, int decompLevel, 
			int groupRow, int row, int totalRows) {
		int levelSize = groupBackoffLevels[decompLevel][groupRow];
		int totalFreq = 0;
		int totalCondFreq = 0;
		int diversityOfHistory = 0;
		for(int i=0; i<levelSize; i++) {
			int[] freqCondFreqDivHist = eventDecomposition[decompLevel].getFreqCondFreqDivHist(backoffKeyTable, row);			
			totalFreq += freqCondFreqDivHist[0];
			totalCondFreq += freqCondFreqDivHist[1];
			diversityOfHistory += freqCondFreqDivHist[2];
			row++;
		}
				
		double lambda = totalCondFreq==0 ? 0 : (double) totalCondFreq / 
				(totalCondFreq + addFactor + multFactor * diversityOfHistory);
		double prob = totalCondFreq==0 ? 0 :  (double) totalFreq / totalCondFreq;
		double backoffProb = (row==totalRows) ? lastWeight : 
			getCondProb(backoffKeyTable, decompLevel, groupRow+1, row, totalRows);
		
		return lambda * prob + (1d-lambda) * backoffProb;
	}
	
	private double getCondProbConjunct(Symbol[][][] backoffKeyTableConjunct, int decompLevel, 
			int groupRow, int row, int totalRows) {
		int levelSize = groupBackoffLevels[decompLevel][groupRow];
		int totalFreq = 0;
		int totalCondFreq = 0;
		int diversityOfHistory = 0;
		for(Symbol[][] backoffKeyTable : backoffKeyTableConjunct) {			
			for(int i=0; i<levelSize; i++) {
				int[] freqCondFreqDivHist = eventDecomposition[decompLevel].getFreqCondFreqDivHist(backoffKeyTable, row+i);
				totalFreq += freqCondFreqDivHist[0];
				totalCondFreq += freqCondFreqDivHist[1];	
				diversityOfHistory += freqCondFreqDivHist[2];
			}
		}
		
		row += levelSize;
		
		double lambda = totalCondFreq==0 ? 0 : (double) totalCondFreq / 
				(totalCondFreq + addFactor + multFactor * diversityOfHistory);
		double prob = totalCondFreq==0 ? 0 : (double) totalFreq / totalCondFreq;
		double backoffProb = (row==totalRows) ? lastWeight : 
			getCondProbConjunct(backoffKeyTableConjunct, decompLevel, groupRow+1, row, totalRows);
		
		return lambda * prob + (1d-lambda) * backoffProb;
		
	}
	
	public void printTables(PrintStream out) {
		out.println("Decompositions: " + decompositions);
		out.println("Backoff Levels:\n");
		int level = 0;
		for(int[][] bl : backoffLevels) {
			out.println("level: " + level++);
			for(int[] b : bl) out.println("\t" + Arrays.toString(b));
		}
		out.println("Skip Decompositions: " + Arrays.toString(skipDecomposition));
		out.println("Group Backoff Levels: ");
		for(int[] g : groupBackoffLevels) out.print(Arrays.toString(g));
		out.println();
		int ed = 0;
		for(BackoffEvent be : eventDecomposition) {
			out.println("Event Decomposition: " + ed);
			be.printTables(out);
			ed++;
		}
	}
	
	public static void testSmall() {
		// A B | C D
		int[][][] backoffLevels = new int[][][] {
			{
			//	 B  C  D
				{0, 0, 0},
				{1, 0, 1},
				{1, 2, 1},
				{3, 3, 2}
			},
			{
			//	 A  B  C  D
				{0, 0, 1, 0},
				{1, 0, 2, 1},
				{2, 0, 3, 2},
				{3, 3, 3, 2}
			},
		};
		boolean[] skipDecomposition = new boolean[]{false, false};
		int[][] groupBackoffLevels = new int[][]{{1,1,2},{1,3}};	
		double addFactor = 0;
		double multFactor = 3;
		double lastWeight = 0.000001;
		
		 		
		BackoffModel_DivHistory BM = new BackoffModel_DivHistory(backoffLevels, skipDecomposition, 
				groupBackoffLevels, addFactor, multFactor, lastWeight);
		
		for(int i=0; i<100; i++) {
			Symbol[][] backoffKeyTable = getRandomEventTable(4, 4);
			BM.increaseInTables(backoffKeyTable);			
		}
		
		BM.printTables(System.out);
		
		for(int i=0; i<10; i++) {
			Symbol[][] backoffKeyTable = getRandomEventTable(4, 4);
			for(Symbol[] b : backoffKeyTable) {
				System.out.println(Arrays.toString(b));
			}
			System.out.println(BM.getCondProb(backoffKeyTable));
			System.out.println();
		}		
		
	}
	
	public static void test() {
		// A B C D | E F G H
		int[][][] backoffLevels = new int[][][] {
			{
			//	 D  E  F  G  H
				{0, 0, 0, 0, 0},
				{1, 1, 1, 1, 1},
				{2, 2, 2, 2, 2},
				{3, 3, 3, 3, 3}
			},
			{
			//	 C  D  E  F  G  H
				{0, 0, 0, 0, 0, 0},
				{1, 1, 1, 1, 1, 1},
				{2, 2, 2, 2, 2, 2},
				{3, 3, 3, 3, 3, 3}
			},
			{
			//	 B  C  D  E  F  G  H
				{0, 0, 0, 0, 0, 0, 0},
				{1, 1, 1, 1, 1, 1, 1},
				{2, 2, 2, 2, 2, 2, 2},
				{3, 3, 3, 3, 3, 3, 3}
			},
			//	 A  B  C  D  E  F  G  H
			{
				{0, 0, 0, 0, 0, 0, 0, 0},
				{1, 1, 1, 1, 1, 1, 1, 1},
				{2, 2, 2, 2, 2, 2, 2, 2},
				{3, 3, 3, 3, 3, 3, 3, 3}
			}					
		};
		boolean[] skipDecomposition = new boolean[]{false, false, false, false};
		int[][] groupBackoffLevels = new int[][]{{1,1,1,1},{1,1,1,1},{1,1,1,1},{1,1,1,1}};
		double addFactor = 0;
		double multFactor = 3;
		double lastWeight = 0.000001;

		BackoffModel_DivHistory BM = new BackoffModel_DivHistory(backoffLevels, skipDecomposition, 
				groupBackoffLevels, addFactor, multFactor, lastWeight);
		
		for(int i=0; i<100; i++) {
			Symbol[][] backoffKeyTable = getRandomEventTable(4, 8);
			BM.increaseInTables(backoffKeyTable);			
		}
		
		for(int i=0; i<100; i++) {
			Symbol[][] backoffKeyTable = getRandomEventTable(4, 8);
			for(Symbol[] b : backoffKeyTable) {
				System.out.println(Arrays.toString(b));
			}
			System.out.println(BM.getCondProb(backoffKeyTable));
			System.out.println();
		}
		
		//BM.printTables();
		
	}
	
	private static char randomLetter() {
		int n = (int)(Math.random()*2);
		return (char)(105+n);
	}
	
	private static Symbol[][] getRandomEventTable(int rows, int cols) {
		Symbol[][] result = new Symbol[rows][cols];
		for(int r = 0; r<rows; r++) {
			for(int c = 0; c<cols; c++) {
				char letter = (char)(65+c);
				result[r][c] = new SymbolString("" + letter + r + randomLetter() );
			}
		}
		return result;
	}

	public static void main(String[] args) {
		//testSmall();
		test();
		
	}

}

