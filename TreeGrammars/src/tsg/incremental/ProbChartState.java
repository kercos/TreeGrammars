package tsg.incremental;

public class ProbChartState extends SymbolicChartState {

	ProbBox probBox;
	
	public ProbChartState(FragFringeUnambigous fragFringe, int startIndex, int nextWordIndex, 
			ProbBox probBox) {
		super(fragFringe, startIndex, nextWordIndex);
		this.probBox = probBox;
	}

	public ProbChartState(ProbChartState cs, int nextWordIndex) {
		super(cs,nextWordIndex);
		//probBox is null
	}

	public ProbChartState(FragFringeUnambigous fragFringe, int startIndex, int nextWordIndex) {
		super(fragFringe,startIndex,nextWordIndex);
		//probBox is null
	}	

}
