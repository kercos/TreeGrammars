package tsg.incremental;

import tsg.TermLabel;

public class CondProbAttach_Anchor_Backward extends CondProbAttach_Anchor {

	@Override
	public TermLabel getCondHistory(SymbolicChartState a) {		
		return a.peekAfterDot();
	}

	@Override
	public TermLabel getCondHistoryBasic(FragFringeUnambigous a) {
		return a.root;
	}


}
