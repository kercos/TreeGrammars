package tsg.incremental;

import tsg.TermLabel;
import util.Utility;

public class CondProbAttach_Basic_Backward extends CondProbAttach_Basic {

	@Override
	public TermLabel getCondHistory(SymbolicChartState a) {		
		return a.peekAfterDot();
	}
	
	@Override
	public void addTrainEvent(SymbolicChartState a, FragFringeUnambigous b, double d) {
		TermLabel firstKey = b.root;
		TermLabel secondKey = b.firstTerminal();		
		assert a.peekAfterDot()==firstKey;
		Utility.increaseHashMap(table, firstKey, secondKey, b, new double[]{d});
		
	}

	@Override
	public TermLabel getCondHistoryBasic(FragFringeUnambigous a) {
		return a.root;
	}

}
