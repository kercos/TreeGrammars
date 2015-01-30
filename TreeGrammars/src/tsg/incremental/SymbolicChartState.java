package tsg.incremental;

import tsg.TermLabel;

public class SymbolicChartState {

	FragFringeUnambigous fragFringe;
	int dotIndex;
	int startIndex;
	int nextWordIndex;
	int length;
	boolean isStarred;

	public SymbolicChartState(FragFringeUnambigous fragFringe, int startIndex, int nextWordIndex) {
		this.fragFringe = fragFringe;
		this.dotIndex = 0;
		this.startIndex = startIndex;
		this.nextWordIndex = nextWordIndex;
		this.isStarred = startIndex == 0;
		this.length = fragFringe.yield.length;
	}

	public SymbolicChartState(SymbolicChartState cs, int nextWordIndex) {
		this.fragFringe = cs.fragFringe;
		this.dotIndex = cs.dotIndex;
		this.startIndex = cs.startIndex;
		this.nextWordIndex = nextWordIndex; //in input
		this.isStarred = cs.isStarred;
		this.length = cs.length;
	}


	public void advanceDot() {
		dotIndex++;
	}

	public void retreatDot() {
		dotIndex--;
	}

	public int typeOfChartState() {
		if (hasElementAfterDot()) {
			if (peekAfterDot().isLexical)
				return 0; // SCAN
			return 1; // SUB-DOWN FIRST
		}
		if (isStarred)
			return 2; // SUB-DOWN SECOND
		return 3; // COMPLETE
	}

	public boolean isScanState() {
		return hasElementAfterDot() && peekAfterDot().isLexical;
	}

	public TermLabel root() {
		return fragFringe.root;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(nextWordIndex + ": ");
		sb.append(isStarred ? "{*} " : "{" + startIndex + "} ");
		sb.append(fragFringe.root + " < ");
		int i = 0;
		for (TermLabel t : fragFringe.yield) {
			if (dotIndex == i++)
				sb.append(". ");
			sb.append(t + " ");
		}
		if (dotIndex == i)
			sb.append(". ");
		return sb.toString();
	}

	public String toStringTex() {
		StringBuilder sb = new StringBuilder();
		 sb.append("$" + nextWordIndex + ":\\;\\;$ ");
		sb.append(isStarred ? "$(*)$ & " : "$(" + startIndex + ")$ & ");
		sb.append("$" + fragFringe.root.toStringTex() + "$ & $\\yields$ & ");
		int i = 0;
		sb.append("$ ");
		for (TermLabel t : fragFringe.yield) {
			if (dotIndex == i++)
				sb.append(" \\bullet " + " \\;\\; ");
			sb.append(t.toStringTex() + " \\;\\; ");
		}
		if (dotIndex == i)
			sb.append(" \\bullet ");
		sb.append("$");
		return sb.toString();
	}

	public boolean hasElementAfterDot() {
		return dotIndex < length;
	}

	public boolean hasElementBeforeDot() {
		return dotIndex > 0;
	}

	public boolean isFirstStateWithCurrentWord() {
		return dotIndex == 0 || (dotIndex == 1 && isScanState() && !peekBeforeDot().isLexical);
	}

	public TermLabel peekBeforeDot() {
		return fragFringe.yield[dotIndex - 1];
	}

	public TermLabel peekAfterDot() {
		return fragFringe.yield[dotIndex];
	}

	public boolean isCompleteState() {
		return !hasElementAfterDot() && !isStarred;
	}

	@Override
	public int hashCode() {
		return fragFringe.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o instanceof SymbolicChartState) {
			SymbolicChartState oCS = (SymbolicChartState) o;
			return 
				oCS.nextWordIndex == this.nextWordIndex && 
				oCS.fragFringe == this.fragFringe && 
				oCS.startIndex == this.startIndex && 
				oCS.dotIndex == this.dotIndex;
		}
		return false;
	}
	
}
