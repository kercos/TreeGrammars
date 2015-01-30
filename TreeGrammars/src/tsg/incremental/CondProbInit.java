package tsg.incremental;

import java.util.HashMap;

import tsg.TermLabel;


public abstract class CondProbInit {


	/**
	 * 
	 * @param a init fringe
	 * @param d 
	 */
	public abstract void addTrainEvent(FragFringeUnambigous a, double d);
	
	
	public abstract void addAllTrainingEvents(CondProbInit initModel);
	
	
	/**
	 * To run after training is finished
	 */
	public abstract void estimateLogProbabilities();
	
	/**
	 * 
	 * @param a the init fringe
	 * @return p(a)
	 */
	public abstract HashMap<FragFringeUnambigous, double[]> getEventsLogProb(TermLabel lex);


	public abstract void addTrainEventBasic(FragFringeUnambigous a);
	
}
