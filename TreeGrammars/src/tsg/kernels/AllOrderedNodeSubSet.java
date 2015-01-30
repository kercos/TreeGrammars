package tsg.kernels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import util.Pair;

public class AllOrderedNodeSubSet {

	public static long maxComb = 1000;
	
	boolean[][] equalMatrix;
	TSNodeLabel[] a1, a2;
	Cell[][] subSequencesMatrix;	
	int[][] subSequencesSizeMatrix;
	int xMax, yMax;
	int xLength, yLength;
	int matched;
	
	public AllOrderedNodeSubSet(TSNodeLabel[] a1, TSNodeLabel[] a2) {
		this.a1 = a1;
		this.a2 = a2;
		initVariables();
	}
	
	public void initVariables() {
		this.xLength = a1.length;
		this.yLength = a2.length;
		this.xMax = a1.length-1;
		this.yMax = a2.length-1;
		this.equalMatrix = new boolean[xLength][yLength];
		this.subSequencesMatrix = new Cell[xLength][yLength];
		for(int i=0; i<a1.length; i++) {
			for(int j=0; j<a2.length; j++) {
				if (a1[i].sameLabel(a2[j])) {
					this.equalMatrix[i][j] = true;
					matched++;
				}
			}
		}
		subSequencesSizeMatrix = new int[xLength][yLength];
		for(int[] array : subSequencesSizeMatrix) Arrays.fill(array, -1);	
	}
	
	public void reduceA1A2() {
		HashSet<Label> a1Set = new HashSet<Label>();
		HashSet<Label> a2Set = new HashSet<Label>();
		for(TSNodeLabel n1 : a1) a1Set.add(n1.label);
		for(TSNodeLabel n2 : a2) a2Set.add(n2.label);
		a1Set.retainAll(a2Set);
		ArrayList<TSNodeLabel> l1 = new ArrayList<TSNodeLabel>(a1.length);
		ArrayList<TSNodeLabel> l2 = new ArrayList<TSNodeLabel>(a2.length);
		for(TSNodeLabel n1 : a1) {
			if (a1Set.contains(n1.label)) l1.add(n1);
		}
		for(TSNodeLabel n2 : a2) {
			if (a1Set.contains(n2.label)) l2.add(n2);
		}
		this.a1 = l1.toArray(new TSNodeLabel[l1.size()]);
		this.a2 = l2.toArray(new TSNodeLabel[l2.size()]);
		initVariables();
	}
	
	public static ArrayList<ArrayList<Pair<TSNodeLabel>>> allDaughtersMatchBackupOnSimple(
			TSNodeLabel t1, TSNodeLabel t2) {
		AllOrderedNodeSubSet O = new AllOrderedNodeSubSet(t1.daughters, t2.daughters);
		long comb = O.getNumberSubsequences();
		if (comb>maxComb) {
			Parameters.appendReturnInLogFile("Backup on siple match of daughteers. Max comb: " + comb +
					"\n\t" + t1.toString() + "\n\t" + t2.toString() + "\n");
			O.reduceA1A2();
			return O.getSimpleSubsequence();
		}
		return O.getAllSubsequences();
	}
	
	public static ArrayList<ArrayList<Pair<TSNodeLabel>>> allDaughtersMatch(
			TSNodeLabel t1, TSNodeLabel t2) {
		return (new AllOrderedNodeSubSet(t1.daughters, t2.daughters)).getAllSubsequences();
	}
	
	public static long allDaughtersMatchSize(TSNodeLabel t1, TSNodeLabel t2) {
		return (new AllOrderedNodeSubSet(t1.daughters, t2.daughters)).getNumberSubsequences();
	}
	
	public long getNumberSubsequences() {
		if (matched==0) return 0;		
		long result = 0;
		int ringX = 0;
		int ringY = 0;
		int lowY = yMax;
		int rightX = xMax;
		boolean ringNewXExists = ringX < xLength;
		boolean ringNewYExists = ringY < yLength;
		while(ringNewXExists || ringNewYExists) {
			if (ringNewXExists) {
				for(int cellY=lowY; cellY>ringY; cellY--) {
					if (!equalMatrix[ringX][cellY]) continue;
					lowY = cellY;
					result += getNumberSubsequences(ringX, cellY);
				}					
			}
			if (ringNewYExists) {
				for(int cellX=rightX; cellX>ringX; cellX--) {
					if (!equalMatrix[cellX][ringY]) continue;
					rightX = cellX;
					result += getNumberSubsequences(cellX, ringY);
				}					
			}								
			if (ringNewXExists && ringNewYExists && equalMatrix[ringX][ringY]) {
				result += getNumberSubsequences(ringX, ringY);
				break;
			}
			if (ringNewXExists) {
				if (ringX+1 <= rightX) ringX++;
				else ringNewXExists = false;
			}
			if (ringNewYExists) {
				if (ringY+1 <= lowY) ringY++;
				else ringNewYExists = false;
			}
		}
		return result;
	}
	
	private int getNumberSubsequences(int x, int y) {
		int stored = subSequencesSizeMatrix[x][y]; 
		if (stored!=-1) return stored;
		
		stored = 0;		
		int ringX = x+1;
		int ringY = y+1;
		int lowY = yMax;
		int rightX = xMax;
		boolean ringNewXExists = ringX < xLength;
		boolean ringNewYExists = ringY < yLength;
		while(ringNewXExists || ringNewYExists) {
			if (ringNewXExists) {
				for(int cellY=lowY; cellY>ringY; cellY--) {
					if (!equalMatrix[ringX][cellY]) continue;
					lowY = cellY;
					stored += getNumberSubsequences(ringX, cellY);
				}					
			}
			if (ringNewYExists) {
				for(int cellX=rightX; cellX>ringX; cellX--) {
					if (!equalMatrix[cellX][ringY]) continue;
					rightX = cellX;
					stored += getNumberSubsequences(cellX, ringY);
				}					
			}								
			if (ringNewXExists && ringNewYExists && equalMatrix[ringX][ringY]) {
				stored += getNumberSubsequences(ringX, ringY);
				break;
			}
			if (ringNewXExists) {
				if (ringX+1 <= rightX) ringX++;
				else ringNewXExists = false;
			}
			if (ringNewYExists) {
				if (ringY+1 <= lowY) ringY++;
				else ringNewYExists = false;
			}			
		}
		if (stored==0) stored = 1;
		return subSequencesSizeMatrix[x][y] = stored;
	}
	
	
	public ArrayList<ArrayList<Pair<TSNodeLabel>>> getSimpleSubsequence() {
		ArrayList<ArrayList<Pair<TSNodeLabel>>> result = 
			new ArrayList<ArrayList<Pair<TSNodeLabel>>>();
		ArrayList<Pair<TSNodeLabel>> skipX = new ArrayList<Pair<TSNodeLabel>>();
		ArrayList<Pair<Integer>> skipXIndexes = new ArrayList<Pair<Integer>>();
		int j=0;
		for(int i=0; i<xLength; i++) {
			if (this.equalMatrix[i][j]) {
				skipX.add(new Pair<TSNodeLabel>(a1[i],a2[j]));
				skipXIndexes.add(new Pair<Integer>(i,j));
				j++;
				if (j==yLength) break;
			}
		}		
		ArrayList<Pair<TSNodeLabel>> skipY = new ArrayList<Pair<TSNodeLabel>>();
		ArrayList<Pair<Integer>> skipYIndexes = new ArrayList<Pair<Integer>>();
		int i=0;
		for(j=0; j<yLength; j++) {
			if (this.equalMatrix[i][j]) {
				skipY.add(new Pair<TSNodeLabel>(a1[i],a2[j]));
				skipYIndexes.add(new Pair<Integer>(i,j));
				i++;
				if (i==xLength) break;
			}
		}	
		result.add(skipX);
		if (skipXIndexes.equals(skipYIndexes)) return result;				
		result.add(skipY);
		return result;
	}
	
	public ArrayList<ArrayList<Pair<TSNodeLabel>>> getAllSubsequences() {
		if (matched==0) return null;
		ArrayList<ArrayList<Pair<TSNodeLabel>>> result = 
			new ArrayList<ArrayList<Pair<TSNodeLabel>>>();		
		int ringX = 0;
		int ringY = 0;
		int lowY = yMax;
		int rightX = xMax;
		boolean ringNewXExists = ringX < xLength;
		boolean ringNewYExists = ringY < yLength;
		while(ringNewXExists || ringNewYExists) {
			if (ringNewXExists) {
				for(int cellY=lowY; cellY>ringY; cellY--) {
					if (!equalMatrix[ringX][cellY]) continue;
					lowY = cellY;
					result.addAll(getAllSubsequences(ringX, cellY));
				}					
			}
			if (ringNewYExists) {
				for(int cellX=rightX; cellX>ringX; cellX--) {
					if (!equalMatrix[cellX][ringY]) continue;
					rightX = cellX;
					result.addAll(getAllSubsequences(cellX, ringY));
				}					
			}								
			if (ringNewXExists && ringNewYExists && equalMatrix[ringX][ringY]) {
				result.addAll(getAllSubsequences(ringX, ringY));
				break;
			}
			if (ringNewXExists) {
				if (ringX+1 <= rightX) ringX++;
				else ringNewXExists = false;
			}
			if (ringNewYExists) {
				if (ringY+1 <= lowY) ringY++;
				else ringNewYExists = false;
			}
		}
		return result;
	}
	
	private ArrayList<ArrayList<Pair<TSNodeLabel>>> getAllSubsequences(int x, int y) {
		Cell stored = subSequencesMatrix[x][y]; 
		if (stored==null) {			
			stored = new Cell(x, y);
			subSequencesMatrix[x][y] = stored;
			int ringX = x+1;
			int ringY = y+1;
			int lowY = yMax;
			int rightX = xMax;
			boolean ringNewXExists = ringX < xLength;
			boolean ringNewYExists = ringY < yLength;
			while(ringNewXExists || ringNewYExists) {
				if (ringNewXExists) {
					for(int cellY=lowY; cellY>ringY; cellY--) {
						if (!equalMatrix[ringX][cellY]) continue;
						lowY = cellY;
						stored.addSubSequences(getAllSubsequences(ringX, cellY));
					}					
				}
				if (ringNewYExists) {
					for(int cellX=rightX; cellX>ringX; cellX--) {
						if (!equalMatrix[cellX][ringY]) continue;
						rightX = cellX;
						stored.addSubSequences(getAllSubsequences(cellX, ringY));
					}					
				}								
				if (ringNewXExists && ringNewYExists && equalMatrix[ringX][ringY]) {
					stored.addSubSequences(getAllSubsequences(ringX, ringY));
					break;
				}
				if (ringNewXExists) {
					if (ringX+1 <= rightX) ringX++;
					else ringNewXExists = false;
				}
				if (ringNewYExists) {
					if (ringY+1 <= lowY) ringY++;
					else ringNewYExists = false;
				}
			}
			if (stored.subSequences.isEmpty()) {
				stored.addDefaultSingletonPair();
			}
		}
		return stored.subSequences;
	}
	
	public static int indexOf(TSNodeLabel c, TSNodeLabel[] a) {
		for(int i=0; i<a.length; i++) {
			if (c==a[i]) return i;
		}
		return -1;
	}
	
	private class Cell {
		ArrayList<ArrayList<Pair<TSNodeLabel>>> subSequences;
		Pair<TSNodeLabel> pair;
		TSNodeLabel cX, cY;
		
		public Cell(int x, int y) {
			cX = a1[x];
			cY = a2[y];
			this.pair = new Pair<TSNodeLabel>(a1[x],a2[y]);
			subSequences = new ArrayList<ArrayList<Pair<TSNodeLabel>>>(); 
		}
		
		public void addDefaultSingletonPair() {
			ArrayList<Pair<TSNodeLabel>> singleton = new ArrayList<Pair<TSNodeLabel>>(1);
			singleton.add(new Pair<TSNodeLabel>(cX,cY));
			this.subSequences.add(singleton);
		}
		
		public void addSubSequences(ArrayList<ArrayList<Pair<TSNodeLabel>>> otherSubSequences) {			
			for(ArrayList<Pair<TSNodeLabel>> otherListPair : otherSubSequences) {
				ArrayList<Pair<TSNodeLabel>> newListPair = new ArrayList<Pair<TSNodeLabel>>(otherListPair.size()+1);
				newListPair.add(pair);
				newListPair.addAll(otherListPair);
				subSequences.add(newListPair);
			}
		}
		
		public String toString() {
			String result = "";
			for(ListIterator<ArrayList<Pair<TSNodeLabel>>> iter1 = 
				subSequences.listIterator(); iter1.hasNext();) {				
				ArrayList<Pair<TSNodeLabel>> s = iter1.next();
				result += "(";
				for(ListIterator<Pair<TSNodeLabel>> iter2 = s.listIterator(); 
						iter2.hasNext();) {
					Pair<TSNodeLabel> p = iter2.next();
					TSNodeLabel c1 = p.getFirst();
					TSNodeLabel c2 = p.getSecond();
					int i1 = indexOf(c1, a1);
					int i2 = indexOf(c2, a2);
					result += "(" + c1 + i1 + "," + c2 + i2 + ")";
					if (iter2.hasNext()) result += ", ";
				}
				result += ")";
				if (iter1.hasNext()) result += ", ";
			}
			return result;
		}
				
	}
	
}
