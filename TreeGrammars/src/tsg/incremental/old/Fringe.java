package tsg.incremental.old;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;

public class Fringe  {
		
	TermLabel rootLabel;
	TermLabel firstTerminalLabel;
	TermLabel secondTerminalLabel;
	LinkedList<TermLabel> otherTerminals = new LinkedList<TermLabel>();
	
	/**
	 * General Fringe
	 * @param rootLabel
	 * @param firstTerminalNode
	 * @param secondTerminalNode
	 * @param otherTerminals
	 */
	public Fringe(TermLabel rootLabel, TermLabel firstTerminalNode, TermLabel secondTerminalNode, 
			LinkedList<TermLabel> otherTerminals) {
		
		this.rootLabel = rootLabel;
		this.firstTerminalLabel = firstTerminalNode;
		this.secondTerminalLabel = secondTerminalNode;
		this.otherTerminals = otherTerminals;
	}
	
	/**
	 * Empty Fringe
	 * @param rootLabel
	 * @param firstTerminalNode
	 */
	public Fringe(TermLabel rootLabel, TermLabel firstTerminalNode) {
		this.rootLabel = rootLabel;
		this.firstTerminalLabel = firstTerminalNode;
	}
	
	public static Fringe computeFringe(TSNodeLabel fragment) {
		TermLabel rootLabel = TermLabel.getTermLabel(fragment);
		ArrayList<TSNodeLabel> terms = fragment.collectTerminalItems();
		LinkedList<TermLabel> otherTerminals = new LinkedList<TermLabel>();		
		for(TSNodeLabel t : terms) {
			otherTerminals.add(TermLabel.getTermLabel(t.label, t.isLexical));
		}				  
		TermLabel firstTerminalNode = otherTerminals.removeFirst();
		TermLabel secondTerminalNode = otherTerminals.isEmpty() ? null : otherTerminals.removeFirst();
						
		return new Fringe(rootLabel, firstTerminalNode, secondTerminalNode, otherTerminals);
	}
	
	
	public static Fringe computeFringeTime(TSNodeLabel fragment, long[] time) {
		long start = System.currentTimeMillis();
		Fringe fringe = computeFringe(fragment);
		long stop = System.currentTimeMillis();
		time[0] += (stop-start);
		return fringe;
	}
	
	public int size() {
		int i = this.secondTerminalLabel==null ? 1 : 2;
		return otherTerminals.size() + i;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(rootLabel.toString());
		sb.append(": [");
		sb.append(firstTerminalLabel);
		sb.append("|");
		sb.append(secondTerminalLabel==null ? "#null#" : secondTerminalLabel);
		sb.append("|");
		Iterator<TermLabel> iter = otherTerminals.iterator();
		while(iter.hasNext()) {
			sb.append(iter.next());
			if (iter.hasNext())
			sb.append(",");
		}
		sb.append("]");
		return sb.toString(); 
	}
		
	public boolean isFirstLexFringe() {
		return firstTerminalLabel.isLexical;
	}
	
	// firstLexSecondLex
	public boolean isScanFringe() {
		return 	firstTerminalLabel.isLexical && 
				secondTerminalLabel!=null &&
				secondTerminalLabel.isLexical;
	}
	
	public boolean isFirstLexNextSubFringe() {
		return 	firstTerminalLabel.isLexical && 
				secondTerminalLabel!=null &&
				!secondTerminalLabel.isLexical;
	}
	
	public boolean isEmpty() {
		return firstTerminalLabel.isLexical && 
			   secondTerminalLabel==null;
	}
	
	public boolean isFirstSubNextLexFringe() {
		return 	!firstTerminalLabel.isLexical && 
				secondTerminalLabel!=null &&
				secondTerminalLabel.isLexical;
	}
			
	public boolean equals(Object o) {
		if (o==this) return true;		
		if (o instanceof Fringe) {
			Fringe f = (Fringe)o;
			boolean thisSeconIsNull = this.secondTerminalLabel==null;
			boolean fSeconIsNull = f.secondTerminalLabel==null;
			if (thisSeconIsNull != fSeconIsNull) return false;
			return 	this.rootLabel.equals(f.rootLabel) &&
					this.firstTerminalLabel.equals(f.firstTerminalLabel) &&
					(thisSeconIsNull || this.secondTerminalLabel.equals(f.secondTerminalLabel)) && 
					this.otherTerminals.equals(f.otherTerminals);
		}
		return false;				
	}
		

	@Override	
	public int hashCode() {	
		int result = 31 + rootLabel.hashCode();
		result = 31 * result + firstTerminalLabel.hashCode();
		result = 31 * result + (secondTerminalLabel==null ? 1 : secondTerminalLabel.hashCode());
		result = 31 * result + otherTerminals.hashCode();
		//for(TSNodeLabel t : terminals) {
		//	result = 31 * result + t.label.hashCode();
		//}
		return result;
	}
	
	
	public boolean checkSubDown(Fringe nextFringe) {
		return 	this.isFirstLexNextSubFringe() &&  
				nextFringe.isFirstLexFringe() &&
				this.secondTerminalLabel.equals(nextFringe.rootLabel);
	}

	
	public Fringe subDownTime(Fringe nextFringe, long[] time) {
		long start = System.currentTimeMillis();
		Fringe f = this.subDown(nextFringe);
		long stop = System.currentTimeMillis();
		time[0] += (stop-start);
		return f;
	}

	public Fringe subDown(Fringe nextFringe) {
		
		LinkedList<TermLabel> newOtherTerminals = new LinkedList<TermLabel>();
		if (nextFringe.secondTerminalLabel!=null) 
			newOtherTerminals.add(nextFringe.secondTerminalLabel);
		newOtherTerminals.addAll(nextFringe.otherTerminals);
		newOtherTerminals.addAll(this.otherTerminals);
		
		TermLabel newRootLabel = this.rootLabel;
		TermLabel newFirstTerminalNode = nextFringe.firstTerminalLabel;
		TermLabel newSecondTerminalNode = newOtherTerminals.isEmpty() ? null : newOtherTerminals.removeFirst();
		 
				
		return new Fringe(newRootLabel, newFirstTerminalNode, 
				newSecondTerminalNode, newOtherTerminals);
	}
	
	public boolean checkSubUp(Fringe nextFringe) {
		return 	this.isEmpty() &&
				nextFringe.isFirstSubNextLexFringe() &&
				this.rootLabel.equals(nextFringe.firstTerminalLabel);
	}
	
	public Fringe subUpTime(Fringe nextFringe, long[] time) {
		long start = System.currentTimeMillis();
		Fringe f = this.subUp(nextFringe);
		long stop = System.currentTimeMillis();
		time[0] += (stop-start);
		return f;
	}


	public Fringe subUp(Fringe nextFringe) {
		
		LinkedList<TermLabel> newOtherTerminals = new LinkedList<TermLabel>();
		newOtherTerminals.addAll(nextFringe.otherTerminals);
		
		TermLabel newRootLabel = nextFringe.rootLabel;
		TermLabel newFirstTerminalNode = nextFringe.secondTerminalLabel;
		TermLabel newSecondTerminalNode = newOtherTerminals.isEmpty() ? null : newOtherTerminals.removeFirst();
		
		return new Fringe(newRootLabel, newFirstTerminalNode, newSecondTerminalNode, newOtherTerminals);
	}
	
	public Fringe scanTime(long[] time) {
		long start = System.currentTimeMillis();
		Fringe f = this.scan();
		long stop = System.currentTimeMillis();
		time[0] += (stop-start);
		return f;
	}
	
	public Fringe scan() {
		
		LinkedList<TermLabel> newOtherTerminals = new LinkedList<TermLabel>();
		newOtherTerminals.addAll(this.otherTerminals);
		
		TermLabel newRootLabel = this.rootLabel;
		TermLabel newFirstTerminalNode = this.secondTerminalLabel;
		TermLabel newSecondTerminalNode = newOtherTerminals.isEmpty() ? null : newOtherTerminals.removeFirst();
		
		return new Fringe(newRootLabel, newFirstTerminalNode, newSecondTerminalNode, newOtherTerminals);
	}
	
}
