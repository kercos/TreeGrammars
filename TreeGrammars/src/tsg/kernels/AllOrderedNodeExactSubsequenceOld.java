package tsg.kernels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import tsg.TSNodeLabel;
import util.Pair;

/**
 * The array of nodes a1 should contain in order all the nodes in a2 (only the operation of skipping one is allowed).
 * All the matched pairs of combinations are found. 
 * @author fsangati
 *
 */
public class AllOrderedNodeExactSubsequenceOld {
	
	Cell[][] cellMatrix;
	TSNodeLabel[] a1, a2;	
	int xMax, yMax;
	int xLength, yLength;
	int lengthToMatch;
	int totalMatched;
	
	protected class Cell {
		int x,y;
		boolean match, leftReachable, diagonalReachable;
		int maxSequenceLength;
		
		protected Cell(int x, int y, boolean match) {
			this.x = x;
			this.y = y;
			if (match) {
				this.match = true;
				totalMatched++;
			}
		}

		public void addAllSubSequences(
				ArrayList<Pair<TSNodeLabel>> currentThread, ArrayList<ArrayList<Pair<TSNodeLabel>>> result) {
			if (leftReachable) {
				if (diagonalReachable) {
					ArrayList<Pair<TSNodeLabel>> newThread = new ArrayList<Pair<TSNodeLabel>>(currentThread);
					cellMatrix[x][y+1].addAllSubSequences(newThread, result);
				}
				else cellMatrix[x][y+1].addAllSubSequences(currentThread, result);
			}
			if (match) {
				currentThread.add(0,new Pair<TSNodeLabel>(a1[x],a2[y]));
				if (y==0) {
					result.add(currentThread);
					return;
				}
			}
			if (diagonalReachable) cellMatrix[x][y].addAllSubSequences(currentThread, result);			 			
		}
				
	}
	
	public AllOrderedNodeExactSubsequenceOld(TSNodeLabel[] a1, TSNodeLabel[] a2) {
		this.a1 = a1;
		this.a2 = a2;
		this.xLength = a1.length+1;
		this.yLength = a2.length+1;
		lengthToMatch = a2.length;
		this.xMax = a1.length;
		this.yMax = a2.length;
		this.cellMatrix = new Cell[xLength][yLength];
		for(int x=0; x<xLength; x++) this.cellMatrix[x][0] = new Cell(x-1, -1, false);
		for(int y=0; y<yLength; y++) this.cellMatrix[0][y] = new Cell(-1, y-1, false);
		for(int x=1; x<xLength; x++) {
			for(int y=1; y<yLength; y++) {
				this.cellMatrix[x][y] = new Cell(x-1, y-1, areEqual(a1[x-1],a2[y-1]));
			}
		}
	}
	
	public static boolean areEqual(TSNodeLabel a, TSNodeLabel b) {
		return a.sameLabel(b);
	}
	
	public static ArrayList<ArrayList<Pair<TSNodeLabel>>> getallExactSubsequences(
			TSNodeLabel[] t1, TSNodeLabel[] t2) {
		if (t1.length<t2.length) return null;
		AllOrderedNodeExactSubsequenceOld O = new AllOrderedNodeExactSubsequenceOld(t1, t2);
		return O.getExactSubsequences();
	}
	
	public ArrayList<ArrayList<Pair<TSNodeLabel>>> getExactSubsequences() {
		if (totalMatched<lengthToMatch) return null;
		int firstindexXmatchFirstY = -1;
		int lastIndexXmatchLastY = -1;
		for(int x=1; x<xLength; x++) {
			if (cellMatrix[x][1].match) {
				firstindexXmatchFirstY = x;
				break;
			}			
		}
		for(int x=xMax; x>=yMax; x--) {
			if (cellMatrix[x][yMax].match) {
				lastIndexXmatchLastY = x;
				break;
			}			
		}
		if (lastIndexXmatchLastY-firstindexXmatchFirstY+1<lengthToMatch) return null;
		boolean previousReachable = false;
		for(int x=1; x<xLength; x++) {
			Cell c = cellMatrix[x][1];						
			if (c.match) {							
				c.maxSequenceLength = 1; 
				previousReachable = true;
			}
			else {
				if (previousReachable) {
					c.leftReachable = true;
					c.maxSequenceLength = 1; 
					previousReachable = true;
				}
			}
		}
		for(int y=2; y<yLength; y++) {
			for(int x=1; x<xLength; x++) {
				Cell c = cellMatrix[x][y];
				Cell cl = cellMatrix[x-1][y];				
				int maxSequenceLenghtFromL=cl.maxSequenceLength;
				int maxSequenceLenghtFromD=0;
				if (c.match) {
					Cell cd = cellMatrix[x-1][y-1];
					maxSequenceLenghtFromD = cd.maxSequenceLength+1;					
				}					
				int maxSL = Math.max(maxSequenceLenghtFromL, maxSequenceLenghtFromD);
				if (maxSL==0) continue;
				c.maxSequenceLength = maxSL;				
				if (maxSequenceLenghtFromL==maxSL) c.leftReachable = true;
				if (maxSequenceLenghtFromD==maxSL) c.diagonalReachable = true;					
			}
		}
		ArrayList<ArrayList<Pair<TSNodeLabel>>> result = new ArrayList<ArrayList<Pair<TSNodeLabel>>>();
		for(int x=1; x<xLength; x++) {
			Cell c = cellMatrix[x][yMax];
			if (!c.match) continue;			
			if (c.maxSequenceLength!=lengthToMatch) continue;
			c.leftReachable = false;
			c.addAllSubSequences(new ArrayList<Pair<TSNodeLabel>>(), result);			
		}
		return result;
	}
	
	public static void main(String[] args) {
	
	}

	
}
