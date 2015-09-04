package tsg.incremental;

import tsg.TermLabel;
import util.Utility;

public class CondProbAttach_Basic_Forward extends CondProbAttach_Basic {

	@Override
	public TermLabel getCondHistory(SymbolicChartState a) {		
		return a.root();
	}
	
	@Override
	public void addTrainEvent(SymbolicChartState a, FragFringeUnambigous b, double d) {
		TermLabel firstKey = b.firstTerminal();
		TermLabel secondKey = b.secondTerminal();		
		assert a.root()==firstKey;
		Utility.increaseHashMap(table, firstKey, secondKey, b, new double[]{d});		
	}

	@Override
	public TermLabel getCondHistoryBasic(FragFringeUnambigous a) {
		return a.firstTerminal();
	}

}
