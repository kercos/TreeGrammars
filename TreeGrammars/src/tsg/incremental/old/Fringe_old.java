package tsg.incremental.old;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import tsg.Label;
import tsg.TSNodeLabel;

public class Fringe_old  {
		
	Label rootLabel;
	TSNodeLabel firstTerminalNode;
	TSNodeLabel secondTerminalNode;
	LinkedList<TSNodeLabel> otherTerminals = new LinkedList<TSNodeLabel>();
	
	/**
	 * General Fringe
	 * @param rootLabel
	 * @param firstTerminalNode
	 * @param secondTerminalNode
	 * @param otherTerminals
	 */
	public Fringe_old(Label rootLabel, TSNodeLabel firstTerminalNode, TSNodeLabel secondTerminalNode,
			LinkedList<TSNodeLabel> otherTerminals) {
		
		this.rootLabel = rootLabel;
		this.firstTerminalNode = firstTerminalNode;
		this.secondTerminalNode = secondTerminalNode;
		this.otherTerminals = otherTerminals;
	}
	
	/**
	 * Empty Fringe
	 * @param rootLabel
	 * @param firstTerminalNode
	 */
	public Fringe_old(Label rootLabel, TSNodeLabel firstTerminalNode) {
		this.rootLabel = rootLabel;
		this.firstTerminalNode = firstTerminalNode;
	}
	
	public static Fringe_old computeFringe(TSNodeLabel fragment) {
		Label rootLabel = fragment.label;
		LinkedList<TSNodeLabel> otherTerminals = new LinkedList<TSNodeLabel>();				
		otherTerminals.addAll(fragment.collectTerminalItems());				  
		TSNodeLabel firstTerminalNode = otherTerminals.removeFirst();
		TSNodeLabel secondTerminalNode = otherTerminals.isEmpty() ? null : otherTerminals.removeFirst();				
		return new Fringe_old(rootLabel, firstTerminalNode, secondTerminalNode, otherTerminals);
	}
	
	
	public static Fringe_old computeFringeTime(TSNodeLabel fragment, long[] time) {
		long start = System.currentTimeMillis();
		Fringe_old fringe = computeFringe(fragment);
		long stop = System.currentTimeMillis();
		time[0] += (stop-start);
		return fringe;
	}
	
	public int size() {
		return otherTerminals.size() + 2;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(rootLabel.toString());
		sb.append(": [");
		sb.append(firstTerminalNode.label());
		sb.append("|");
		sb.append(secondTerminalNode==null ? "#null#" : secondTerminalNode.label());
		sb.append("|");
		Iterator<TSNodeLabel> iter = otherTerminals.iterator();
		while(iter.hasNext()) {
			sb.append(iter.next().label());
			if (iter.hasNext())
			sb.append(",");
		}
		sb.append("]");
		return sb.toString(); 
	}
	
	public TSNodeLabel firstTerminalNode() {
		return firstTerminalNode;
	}
	
	public Label firstTerminalLabel() {
		return firstTerminalNode.label;
	}
	
	public TSNodeLabel secondTerminalNode() {
		return secondTerminalNode;
	}
	
	public Label secondTerminalLabel() {
		return secondTerminalNode.label;
	}
	
	public boolean isFirstLexFringe() {
		return firstTerminalNode.isLexical;
	}
	
	// firstLexSecondLex
	public boolean isScanFringe() {
		return 	isFirstLexFringe() &&
				secondTerminalNode!=null &&  
				secondTerminalNode.isLexical;
	}
	
	public boolean isFirstLexNextSubFringe() {
		return 	isFirstLexFringe() &&
				secondTerminalNode!=null &&
				!secondTerminalNode.isLexical;
	}
	
	public boolean isEmpty() {
		return isFirstLexFringe() && 
			   secondTerminalNode==null;
	}
	
	public boolean isFirstSubNextLexFringe() {
		return 	!firstTerminalNode.isLexical &&
				secondTerminalNode!=null &&
				secondTerminalNode.isLexical;
	}
			
	public boolean equals(Object o) {
		if (o==this) return true;		
		if (o instanceof Fringe_old) {
			Fringe_old f = (Fringe_old)o;
			boolean thisSeconIsNull = this.secondTerminalNode==null;
			boolean fSeconIsNull = f.secondTerminalNode==null;
			if (thisSeconIsNull != fSeconIsNull) return false;
			return 	this.rootLabel.equals(f.rootLabel) &&
					this.firstTerminalNode.equals(f.firstTerminalNode) &&
					(thisSeconIsNull || this.secondTerminalNode.equals(f.secondTerminalNode)) && 
					this.otherTerminals.equals(f.otherTerminals);
		}
		return false;				
	}
		

	@Override	
	public int hashCode() {	
		int result = 31 + rootLabel.hashCode();
		result = 31 * result + firstTerminalNode.hashCode();
		result = 31 * result + (secondTerminalNode==null ? 1 : secondTerminalNode.hashCode());
		result = 31 * result + otherTerminals.hashCode();
		//for(TSNodeLabel t : terminals) {
		//	result = 31 * result + t.label.hashCode();
		//}
		return result;
	}
	
	
	public boolean checkSubDown(Fringe_old nextFringe) {
		return 	this.isFirstLexNextSubFringe() &&  
				nextFringe.isFirstLexFringe() &&
				this.secondTerminalLabel().equals(nextFringe.rootLabel);
	}

	
	public Fringe_old subDownTime(Fringe_old nextFringe, long[] time) {
		long start = System.currentTimeMillis();
		Fringe_old f = this.subDown(nextFringe);
		long stop = System.currentTimeMillis();
		time[0] += (stop-start);
		return f;
	}

	public Fringe_old subDown(Fringe_old nextFringe) {
		
		LinkedList<TSNodeLabel> newOtherTerminals = new LinkedList<TSNodeLabel>();
		if (nextFringe.secondTerminalNode!=null) 
			newOtherTerminals.add(nextFringe.secondTerminalNode);
		newOtherTerminals.addAll(nextFringe.otherTerminals);
		newOtherTerminals.addAll(this.otherTerminals);
		
		Label newRootLabel = this.rootLabel;
		TSNodeLabel newFirstTerminalNode = nextFringe.firstTerminalNode;
		TSNodeLabel newSecondTerminalNode = newOtherTerminals.isEmpty() ? null : newOtherTerminals.removeFirst();
				
		return new Fringe_old(newRootLabel, newFirstTerminalNode, newSecondTerminalNode, newOtherTerminals);
	}
	
	public boolean checkSubUp(Fringe_old nextFringe) {
		return 	this.isEmpty() &&
				nextFringe.isFirstSubNextLexFringe() &&
				this.rootLabel.equals(nextFringe.firstTerminalLabel());
	}
	
	public Fringe_old subUpTime(Fringe_old nextFringe, long[] time) {
		long start = System.currentTimeMillis();
		Fringe_old f = this.subUp(nextFringe);
		long stop = System.currentTimeMillis();
		time[0] += (stop-start);
		return f;
	}


	public Fringe_old subUp(Fringe_old nextFringe) {
		
		LinkedList<TSNodeLabel> newOtherTerminals = new LinkedList<TSNodeLabel>();
		newOtherTerminals.addAll(nextFringe.otherTerminals);
		
		Label newRootLabel = nextFringe.rootLabel;
		TSNodeLabel newFirstTerminalNode = nextFringe.secondTerminalNode;
		TSNodeLabel newSecondTerminalNode = newOtherTerminals.isEmpty() ? null : newOtherTerminals.removeFirst();
		
		return new Fringe_old(newRootLabel, newFirstTerminalNode, newSecondTerminalNode, newOtherTerminals);
	}
	
	public Fringe_old scanTime(long[] time) {
		long start = System.currentTimeMillis();
		Fringe_old f = this.scan();
		long stop = System.currentTimeMillis();
		time[0] += (stop-start);
		return f;
	}
	
	public Fringe_old scan() {
		
		LinkedList<TSNodeLabel> newOtherTerminals = new LinkedList<TSNodeLabel>();
		newOtherTerminals.addAll(this.otherTerminals);
		
		Label newRootLabel = this.rootLabel;
		TSNodeLabel newFirstTerminalNode = this.secondTerminalNode;
		TSNodeLabel newSecondTerminalNode = newOtherTerminals.isEmpty() ? null : newOtherTerminals.removeFirst();
		
		return new Fringe_old(newRootLabel, newFirstTerminalNode, newSecondTerminalNode, newOtherTerminals);
	}
	
}
