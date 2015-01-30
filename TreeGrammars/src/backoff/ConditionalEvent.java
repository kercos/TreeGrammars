package backoff;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import symbols.Symbol;
import util.Utility;

public class ConditionalEvent implements Serializable{
	
	private static final long serialVersionUID = 0L;

	HashMap<ArrayList<Symbol>, CondContextEntry> conditionalEventTable;		
	
	public ConditionalEvent() {		
		this.conditionalEventTable = new HashMap<ArrayList<Symbol>, CondContextEntry>();
	}
	
	public void addConditionalEvent(ArrayList<Symbol> conditioningContext, Symbol event) {
		CondContextEntry entry = conditionalEventTable.get(conditioningContext);
		if (entry==null) {
			conditionalEventTable.put(conditioningContext, new CondContextEntry(event));
		}
		else {
			entry.addEvent(event);
		}
	}
	
	public int[] getFreqCondFreq(ArrayList<Symbol> conditioningContext, Symbol event) {
		CondContextEntry entry = conditionalEventTable.get(conditioningContext);
		if (entry==null) return new int[]{0,0};
		return entry.getFreqCondFreq(event);
	}
	
	public int[] getFreqCondFreqDivHist(ArrayList<Symbol> conditioningContext, Symbol event) {
		CondContextEntry entry = conditionalEventTable.get(conditioningContext);
		if (entry==null) return new int[]{0,0,0};
		return entry.getFreqCondFreqDivHist(event);
	}
	
	public void printTables(PrintStream out) {
		for(Entry<ArrayList<Symbol>, CondContextEntry> e : conditionalEventTable.entrySet()) {
			CondContextEntry entry = e.getValue();
			out.println("Cond. Context: " + e.getKey() + "\t" +  "freq: " + entry.condCotextFreq);
			entry.printTables(out);
		}		
	}
	
	public int[] eventsFreq_Total_WithFreqOne() {
		int[] result = new int[2];
		for(Entry<ArrayList<Symbol>, CondContextEntry> e : conditionalEventTable.entrySet()) {
			int[] eventsFreqTotalWithFreqOne = e.getValue().eventsTotalFreq_WithFreqOne();
			Utility.arrayIntPlus(eventsFreqTotalWithFreqOne, result);
		}
		return result;
	}
	
		
	class CondContextEntry {
		
		int condCotextFreq;
		HashMap<Symbol, int[]> eventTable;
		
		public CondContextEntry(Symbol event) {			
			this.eventTable = new HashMap<Symbol, int[]>();
			eventTable.put(event, new int[]{1});
			condCotextFreq = 1;
		}
		
		public int size() {
			return eventTable.size();
		}
		
		public int[] getFreqCondFreq(Symbol event) {
			int[] storedFreq = eventTable.get(event);
			int freq = storedFreq==null ? 0 : storedFreq[0];
			return new int[]{freq, condCotextFreq};
		}
		
		public int[] getFreqCondFreqDivHist(Symbol event) {
			int[] storedFreq = eventTable.get(event);
			int freq = storedFreq==null ? 0 : storedFreq[0];
			return new int[]{freq, condCotextFreq, eventTable.size()};
		}

		public void addEvent(Symbol event) {
			condCotextFreq++;
			int[] oldFreq = eventTable.get(event);
			if (oldFreq==null) {			
				eventTable.put(event, new int[]{1});
			}
			else {
				oldFreq[0]++;
			}
		}
		
		public void printTables(PrintStream out) {
			for(Entry<Symbol, int[]> e : eventTable.entrySet()) {
				out.println("\t" + "Event: " + e.getKey() + "\t" + "freq: " + e.getValue()[0]);
			}
		}
		
		public int[] eventsTotalFreq_WithFreqOne() {
			int total = 0;
			int freqOne = 0;
			for(Entry<Symbol, int[]> e : eventTable.entrySet()) {
				int freq = e.getValue()[0]; 
				total += freq;
				if (freq==1) freqOne++;
			}
			return new int[]{total, freqOne};
		}
			
	}


	
}
