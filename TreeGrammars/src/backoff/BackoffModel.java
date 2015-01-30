package backoff;

import java.io.PrintStream;

import symbols.Symbol;

public abstract class BackoffModel {

	public abstract void printTables(PrintStream out); 
	
	public abstract void increaseInTables(Symbol[][] backoffKeyTable);
	
	public abstract double getCondProb(Symbol[][] backoffKeyTable);
	
	public abstract int[] getCondFreqEventFreq(Symbol[][] backoffKeyTable, int decompNumb, int level);
	
	public abstract String getEventFreqToString(Symbol[][] backoffKeyTable, int decompNumb, int level);
	
	public abstract double getCondProbAverage(Symbol[][][] conjunctBackoffKeyTable);
	
	public abstract int[][] getCondFreqEventFreqConjunct(Symbol[][][] backoffKeyTableConjunct, 
			int decompNumb, int level);
	
	public abstract String[] getEventFreqToStringConjunct(Symbol[][][] backoffKeyTableConjunct, 
			int decompNumb, int level);
	
}
