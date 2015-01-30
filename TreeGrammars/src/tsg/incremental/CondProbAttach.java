package tsg.incremental;

import java.io.File;
import java.util.HashMap;

import settings.Parameters;
import tsg.TermLabel;


public abstract class CondProbAttach {

	/**
	 * 
	 * @param a the left fringe
	 * @param b the right fringe
	 * @param d 
	 */
	public abstract void addTrainEvent(SymbolicChartState a, FragFringeUnambigous b, double d);
	
	public abstract void addTrainEventBasic(FragFringeUnambigous a);
	
	public abstract void addAllTrainingEvents(CondProbAttach attachModel);
	
	/**
	 * To run after training is finished
	 */
	public abstract void estimateLogProbabilities();
	
	public abstract int[] totalEvents();

	public abstract CondProbAttachLex getConProbLex(TermLabel lex);
	
	public void printLexEntryToFile(TermLabel lex, File outputFile) {
		CondProbAttachLex cpal = getConProbLex(lex);
		if (!cpal.hasEvents())
			Parameters.reportLine("No events for lex: " + lex);
		else
			cpal.printToFile(outputFile);
	}
	
	public abstract class CondProbAttachLex {

		public abstract HashMap<FragFringeUnambigous, double[]> getEventsLogProb(SymbolicChartState a);
	
		public abstract boolean hasEvents();

		public abstract void printToFile(File outputFile);
	}


}
