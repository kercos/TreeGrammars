package tdg;

public class TDNodePair {
	public TDNode first, second;
	
	public TDNodePair(TDNode first, TDNode second) {
		this.first = first;
		this.second = second;
	}
	
	public String toString() {
		return "(" + first.lexPosTag() + "," + second.lexPosTag() + ")";
	}
	
}