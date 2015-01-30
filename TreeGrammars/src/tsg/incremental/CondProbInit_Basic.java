package tsg.incremental;

import java.util.HashMap;

import tsg.TermLabel;
import util.Utility;

public class CondProbInit_Basic extends CondProbInit {

	
	private static final long serialVersionUID = 1L;
	
	HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> table;
	//indexed on first lex
	
	public CondProbInit_Basic() {
		table = new HashMap<TermLabel, HashMap<FragFringeUnambigous,double[]>>();
	}
	
	/**
	 * 
	 * @param a null
	 * @param b the init fringe
	 */
	public void addTrainEvent(FragFringeUnambigous a) {
		addTrainEvent(a, 1d);
	}
	
	@Override
	public void addTrainEvent(FragFringeUnambigous a, double d) {
		TermLabel lex = a.firstTerminal();
		Utility.increaseInHashMap(table, lex, a, d);		
	}
	
	@Override
	public void addTrainEventBasic(FragFringeUnambigous a) {
		TermLabel lex = a.firstTerminal();
		if (!table.containsKey(lex) || !table.get(lex).containsKey(a)) {
			Utility.increaseInHashMap(table, lex, a, 1d);
		}
		
	}

	@Override
	public void addAllTrainingEvents(CondProbInit initModel) {
		CondProbInit_Basic otherModel = (CondProbInit_Basic)initModel;
		table.putAll(otherModel.table);
	}
	
	/**
	 * To run after training is finished
	 */
	public void estimateLogProbabilities() {
		double sum = Utility.getSumValueDouble(table);
		for(HashMap<FragFringeUnambigous, double[]> subtable : table.values()) {
			for(double[] v : subtable.values()) {
				v[0] = Math.log(v[0]/sum);
			}
		}		
	}
	
	/**
	 * 
	 * @param a the left fringe
	 * @param b the right fringe
	 * @return p(b|a)
	 */
	public HashMap<FragFringeUnambigous, double[]> getEventsLogProb(TermLabel lex) {
		return table.get(lex);		
	}




	
}
