package backoff;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import symbols.Symbol;
import util.file.FileUtil;

public class BackoffEvent  implements Serializable{
	
	private static final long serialVersionUID = 0L;
	
	int[][] backoffLevels;
	int rows;
	int decompositionLevel;
	int startIndexCondContext;
	int eventIndex;
	int conditioningLength;
	ConditionalEvent[] conditionalEventsLevels;
	
	
	public BackoffEvent(int[][] backoffLevels, int decompositionLevel, int startIndexCondContext) {
		this.backoffLevels = backoffLevels;
		this.decompositionLevel = decompositionLevel;
		this.rows = backoffLevels.length;
		this.startIndexCondContext = startIndexCondContext;
		this.eventIndex = startIndexCondContext - 1;
		this.conditioningLength = backoffLevels[0].length - 1;
		conditionalEventsLevels = new ConditionalEvent[rows];
		for(int i=0; i<rows; i++) {
			conditionalEventsLevels[i] = new ConditionalEvent();			
		}
	}
	
	public void increaseInTables(Symbol[][] backoffKeyTable) {
		for(int l=0; l<rows; l++) {
			Symbol event = getEvent(backoffKeyTable, l);
			ArrayList<Symbol> conditioningContext = getConditioningContext(backoffKeyTable, l);
			conditionalEventsLevels[l].addConditionalEvent(conditioningContext, event);
		}
	}
	
	public Symbol getEvent(Symbol[][] backoffKeyTable, int level) {
		int eventBackoffLevel = backoffLevels[level][0];
		Symbol event = backoffKeyTable[eventBackoffLevel][eventIndex];
		return event;
	}
	
	public ArrayList<Symbol> getConditioningContext(Symbol[][] backoffKeyTable, int level) {
		ArrayList<Symbol> conditioningContext = new ArrayList<Symbol>(conditioningLength);
		for(int c=0; c<conditioningLength; c++) {
			int condContextIndexBackoffLevel = backoffLevels[level][c+1];
			if (condContextIndexBackoffLevel==-1) continue;
			conditioningContext.add(backoffKeyTable[condContextIndexBackoffLevel][c + startIndexCondContext]);
		}
		return conditioningContext;
	}
		
	public int[] getFreqCondFreq(Symbol[][] backoffKeyTable, int level) {
		Symbol event = getEvent(backoffKeyTable, level);
		ArrayList<Symbol> conditioningContext = getConditioningContext(backoffKeyTable, level);		
		return conditionalEventsLevels[level].getFreqCondFreq(conditioningContext, event);
	}
	
	public int[] getFreqCondFreqDivHist(Symbol[][] backoffKeyTable, int level) {
		Symbol event = getEvent(backoffKeyTable, level);
		ArrayList<Symbol> conditioningContext = getConditioningContext(backoffKeyTable, level);		
		return conditionalEventsLevels[level].getFreqCondFreqDivHist(conditioningContext, event);
	}
	
	public String getCondEventToString(Symbol[][] backoffKeyTable, int level) {
		Symbol event = getEvent(backoffKeyTable, level);
		ArrayList<Symbol> conditioningContext = getConditioningContext(backoffKeyTable, level);
		StringBuilder sb = new StringBuilder();
		sb.append(event.toString());
		sb.append(" |");
		for(Symbol c : conditioningContext) {
			sb.append(" " + c.toString());
		}
		return sb.toString(); 
	}
	
	public void printTables(PrintStream out) {
		out.println("Decomp. Level: " + decompositionLevel);
		out.println("Rows: " + rows);
		out.println("Start Index Cond Context: " + startIndexCondContext);
		out.println("Event Index: " + eventIndex);
		out.println("Conditioning Length: " + conditioningLength);
		out.println("Backoff Levels:");
		for(int[] b : backoffLevels) out.println("\t" + Arrays.toString(b));
		
		int cel = 0;
		for(ConditionalEvent ce : conditionalEventsLevels) {
			out.println("Conditional Events Level:" + cel);
			
			int[] totalFreqWithFreqOne = ce.eventsFreq_Total_WithFreqOne();
			out.println("Total Events Frequencies:" + totalFreqWithFreqOne[0]);
			out.println("Events with Freq. 1:" + totalFreqWithFreqOne[1]);
			ce.printTables(out);
			cel++;
		}
	}
		
}
