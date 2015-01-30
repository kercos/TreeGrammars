package ccg;

/**
 * 
 * @author fsangati
 *
 * <T CCGcat head dtrs>
 * For non-leaf nodes, the description contains the following three
 * fields: the category of the node CCGcat, the index of its head
 * daughter head (0 = left or only daughter, 1 = right daughter), and the
 * number of its daughters, dtrs: 
 */
public class CCGInternalNode extends CCGNode {
	
	int headIndex;
	int prole;
	CCGNode[] daughters;
	
	public CCGInternalNode(String label) {
		//<T CCGcat head dtrs>
		label = label.substring(3, label.length()-1);		
		//T CCGcat head dtrs
		String[] labelSplit = label.split("\\s+");
		this.cat = labelSplit[0];
		this.headIndex = Integer.parseInt(labelSplit[1]);
		this.prole = Integer.parseInt(labelSplit[2]);
		this.daughters = new CCGNode[prole];
	}
	
	public boolean isTerminal() {
		return false;
	}
	
	public int prole() {
		return prole;
	}
	
	public String toString() {
		String result =  "(<T " + cat + " " +  headIndex + " " + prole + "> ";
		for(CCGNode n : daughters) {
    		result += n.toString() + " ";
    	}
		result += ")";
		return result;
	}
	
	public CCGNode[] daughters() {
		return daughters;
	}
	
	public CCGNode getHeadDaughter() {
		return this.daughters[this.headIndex];
	}
	
	public CCGTerminalNode getAnchorThroughPercolation() {
		CCGNode headDaughter = getHeadDaughter();
		if (headDaughter.isTerminal()) return (CCGTerminalNode)headDaughter;
		return ((CCGInternalNode)headDaughter).getAnchorThroughPercolation();
	}
}
