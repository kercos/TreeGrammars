package tsg.incremental;

import tsg.TermLabel;

public class CondProbAttach_Anchor_Forward extends CondProbAttach_Anchor {

	@Override
	public TermLabel getCondHistory(SymbolicChartState a) {		
		return a.root();
	}

	@Override
	public TermLabel getCondHistoryBasic(FragFringeUnambigous a) {
		return a.firstTerminal();
	}


}
