package tdg.reranking;
import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import util.*;


public abstract class Reranker_ProbModelBackoff_Compact extends Reranker{
		
	double maxScore;
	boolean printTables;
	static String EOC = "EOC";
	static String nullNode = "NULL";
	static String left = "L", right = "R";
	int events;
	int[][][] backoffLevels;
	BackoffTablesCompact[] eventTables;
	boolean[] skipEvents;
	int[][] groupEventLevelSize;
	
	public Reranker_ProbModelBackoff_Compact(File goldFile, File parsedFile, int nBest, 
			ArrayList<TDNode> trainingCorpus, int uk_limit, 
			int limitTestToFirst, boolean addEOS, boolean countEmptyDaughters, 
			boolean markTerminalNodes, boolean printTables, 
			int[][][] backoffLevels, boolean[] skipEvents, int[][] groupEventLevelSize) {		
		
		super(goldFile, parsedFile, nBest, addEOS, trainingCorpus, 
				uk_limit, countEmptyDaughters, markTerminalNodes, limitTestToFirst);
				
		this.printTables = printTables;
		this.events = backoffLevels.length;
		this.backoffLevels = backoffLevels;
		this.skipEvents = skipEvents;
		this.groupEventLevelSize = groupEventLevelSize;
		
		eventTables = new BackoffTablesCompact[events];
		for(int e=0; e<events; e++) {
			if (skipEvents[e]) continue;
			eventTables[e] = new BackoffTablesCompact(backoffLevels[e], e);
		}		
	}
	
	public boolean bestRerankedIsZeroScore() {
		return maxScore==0;
	}
	
	public void updateCondFreqTables() {
		util.PrintProgressStatic.start("Update tables");	
		for(TDNode t : trainingCorpus) {
			util.PrintProgressStatic.next(1000);
			updateCondFreqTables(t);			
		}
		util.PrintProgressStatic.end();
		if (printTables) printTables();
	}

	public void printTables() {
		for(int e=0; e<events; e++) {
			if (skipEvents[e]) continue;
			eventTables[e].printTables(this.getClass().getName() + "_e" + e);
		}
	}
	
	public abstract void updateCondFreqTables(TDNode thisNode);
	
	public abstract double getProb(TDNode thisNode);
	
	public void increaseInTables(String[][] backoffKeyTable) {
		for(int e = 0; e<events; e++) {
			if (skipEvents[e]) continue;
			eventTables[e].increaseInTables(backoffKeyTable);
		}		
	}
	
	public double getCondProb(String[][] backoffKeyTable) {
		double prob = 1d;
		for(int e=0; e<events; e++) {
			if (skipEvents[e]) continue;		
			int totalRowsInThisEvent = backoffLevels[e].length;
			prob *= getCondProb(backoffKeyTable, e, 0, 0, totalRowsInThisEvent);			
		}
		return prob;
	}
	
	private double getCondProb(String[][] backoffKeyTable, int event, 
			int level, int row, int totalRows) {
		int levelSize = groupEventLevelSize[event][level];
		int totalFreq = 0;
		int totalCondFreq = 0;
		for(int i=0; i<levelSize; i++) {
			int[] freqCondFreq = eventTables[event].getFreqCondFreq(backoffKeyTable, row);
			totalFreq += freqCondFreq[0];
			totalCondFreq += freqCondFreq[1];
			row++;
		}
		if (row==totalRows) {
			return (double) (totalFreq + 0.005d) / (totalCondFreq + 0.5d);
		}
		else {
			return (totalFreq + 3d * getCondProb(backoffKeyTable, event, level+1, row, totalRows)) / 
					(totalCondFreq + 3d);
		}
	}

	@Override
	public void initBestRerankedScore() {
		maxScore = -1;		
	}
	

	@Override
	public String getScoreAsString(TDNode t) {
		return Double.toString(getProb(t));
	}

	@Override
	public int updateRerankedScore(TDNode t, int index, String[] nBestScoresRecords) {
		double score = getProb(t);
		nBestScoresRecords[index] = Double.toString(score);
		if (score > maxScore) {
			maxScore = score;
			return 1;
		}
		else if (score == maxScore) return 0;
		return -1;
	}

}

class BackoffTablesCompact {
	
	int[][] backoffLevels;
	int rows;
	int lastRow;
	int eventNumber;
	MultiHashTableCompact[] freqTables, condFreqTables;
	
	@SuppressWarnings("unchecked")
	public BackoffTablesCompact(int[][] backoffLevels, int eventNumber) {
		this.backoffLevels = backoffLevels;
		this.eventNumber = eventNumber;
		this.rows = backoffLevels.length;		
		this.lastRow = rows-1;
		freqTables = new MultiHashTableCompact[rows];
		condFreqTables = new MultiHashTableCompact[rows];
		for(int i=0; i<rows; i++) {
			freqTables[i] = new MultiHashTableCompact();
			condFreqTables[i] = new MultiHashTableCompact();
		}
	}
	
	public void printTables(String className) {		
		className = className.substring(className.lastIndexOf('.')+1);
		for(int i=0; i<rows; i++) {
			File headLRMarkovFreqTableFile = 
				new File(Parameters.outputPath + className + "_freqTable" + "_boff" + i + ".txt");
			freqTables[i].printToFile(headLRMarkovFreqTableFile);
			
			File headLRPreviousFreqTableFile = 
				new File(Parameters.outputPath + className + "_condFreqTable" + "_boff" + i + ".txt");
			condFreqTables[i].printToFile(headLRPreviousFreqTableFile);
		}		
	}
	
	public void increaseInTables(String[][] backoffKeyTable) {
		for(int l=0; l<rows; l++) {
			String condKey = getTableSelection(backoffKeyTable, backoffLevels[l], "_");
			String eventTag = backoffKeyTable[eventNumber][ backoffLevels[l][0]];
			String key = eventTag + "_" + condKey;
			freqTables[l].addOne(key);
			condFreqTables[l].addOne(condKey);
		}
	}
	
	public String getTableSelection(String[][] backoffKeyTable, int[] choices, String separator) {
		int ch = eventNumber+1;
		String result = backoffKeyTable[ch][choices[ch]];
		while(ch < choices.length-1) {					
			ch++;
			String cell = backoffKeyTable[ch][choices[ch]];
			if (cell.length()!=0) result += separator + cell;			
		} 
		return result;
	}
	
	public int[] getFreqCondFreq(String[][] backoffKeyTable, int row) {
		String condKey = getTableSelection(backoffKeyTable, backoffLevels[row], "_");
		String eventTag = backoffKeyTable[eventNumber][ backoffLevels[row][0]];
		String key = eventTag + "_" + condKey;
		Integer keyFreqInteger = freqTables[row].get(key);
		int keyFreq = keyFreqInteger==null ? 0 : keyFreqInteger;		
		Integer keyCondFreqInteger = condFreqTables[row].get(condKey);
		int keyCondFreq = keyCondFreqInteger==null ? 0 : keyCondFreqInteger;
		return new int[]{keyFreq, keyCondFreq};
	}
	
}