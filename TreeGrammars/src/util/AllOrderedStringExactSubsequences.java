package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Random;

import util.Pair;

/**
 * The array of nodes a1 should contain in order all the nodes in a2 (only the operation of skipping one is allowed).
 * All the matched pairs of combinations are found. 
 * @author fsangati
 *
 */
public class AllOrderedStringExactSubsequences {

	public static final int maxComb = 1000;
	
	Cell[][] cellMatrix;
	String[] a1, a2;	
	int xMax, yMax;
	int xLength, yLength;
	int xRegionStart, xRegionEnd;
	int totalMatched;
	boolean hasOneSolution;
	
	public AllOrderedStringExactSubsequences(String[] a1, String[] a2) {
		if (xLength < yLength) return;
		this.a1 = a1;
		this.a2 = a2;
		this.xLength = a1.length;
		this.yLength = a2.length;		
		this.xMax = this.xLength - 1;
		this.yMax = this.yLength - 1;
		initMatrix();		
	}
	
	private void initMatrix() {
		
		this.cellMatrix = new Cell[xLength][yLength];
		
		boolean foundFirst = false;
		for(int x=0; x<xLength; x++) { // first row, set xRegionStart
			boolean match = equal(a1[x],a2[0]);
			if (!foundFirst && match) {
				foundFirst = true;
				xRegionStart = x;
			}
			this.cellMatrix[x][0] = new Cell(x, 0, match);
		}
		
		if (!foundFirst) {
			hasOneSolution = false;
			return;
		}
		
		boolean foundLast = false;
		for(int x=xMax; x>=0; x--) { // last row, set xRegionEnd
			boolean match = equal(a1[x],a2[yMax]);
			if (!foundLast && match) {
				foundLast = true;
				xRegionEnd = x;
			}
			this.cellMatrix[x][yMax] = new Cell(x, yMax, match);
		}
		
		if (!foundLast) {
			hasOneSolution = false;
			return;
		}
		
		for(int x=xRegionStart; x<=xRegionEnd; x++) { // second till one-but-last row in x Region
			for(int y=1; y<yMax; y++) {
				boolean match = equal(a1[x],a2[y]);
				this.cellMatrix[x][y] = new Cell(x, y, match);
			}
		}
		
		for(int x=0; x<xRegionStart; x++) { // out of bounds before xRegionStart
			for(int y=1; y<yMax; y++) {
				this.cellMatrix[x][y] = new Cell(x, y, false);
			}
		}
		
		for(int x=xRegionEnd+1; x<xLength; x++) { // out of bounds after xRegionEnd
			for(int y=1; y<yMax; y++) {
				this.cellMatrix[x][y] = new Cell(x, y, false);
			}
		}
		
		if (!hasOneSolution()) return;
		
		for(int x=0; x<xLength; x++) {
			Cell c = cellMatrix[x][0];						
			if (c.match) {							
				c.reachable = true; 
				for(++x; x<xLength; x++) {
					c = cellMatrix[x][0];
					c.setLeftReachable();
				}
			}
		}
		
		for(int y=1; y<yLength; y++) {
			for(int x=1; x<xLength; x++) {
				Cell c = cellMatrix[x][y];
				Cell leftCell = cellMatrix[x-1][y];
				if (leftCell.reachable) c.setLeftReachable();
				if (c.match) {
					Cell diagonalCell = cellMatrix[x-1][y-1];
					if (diagonalCell.reachable) c.setDiagonalReachable(); 
				}
			}
		}
	}

	public boolean hasOneSolution() {
		if (totalMatched < yLength) {
			return hasOneSolution = false;
		}
		int currentXIndex = 0;
		for(int y=0; y<yLength; y++) {
			boolean found = false;
			while(currentXIndex<xLength) {				
				if (cellMatrix[currentXIndex][y].match) {
					currentXIndex++;
					found = true;
					break;
				}
				currentXIndex++;
			}
			if (!found) return hasOneSolution = false;
		}
		return hasOneSolution = true;		
	}

	private ArrayList<ArrayList<Pair<String>>> getOneSolution() {
		if (!hasOneSolution) return null;
		System.out.println("One solution!");
		ArrayList<ArrayList<Pair<String>>> result = new ArrayList<ArrayList<Pair<String>>>();
		ArrayList<Pair<String>> onlySolution = new ArrayList<Pair<String>>();
		int currentXIndex = 0;
		for(int y=0; y<yLength; y++) {
			while(currentXIndex<xLength) {				
				if (cellMatrix[currentXIndex][y].match) {
					onlySolution.add(new Pair<String>(a1[currentXIndex],a2[y]));
					currentXIndex++;
					break;
				}
				currentXIndex++;
			}
		}		
		result.add(onlySolution);
		return result;
	}

	public static boolean equal(String a, String b) {
		return a.charAt(0)==b.charAt(0);
	}
		
	public long getExactSubsequencesNumber() {
		if (!hasOneSolution) return 0;
		setTotalPaths();
		Cell c = cellMatrix[xRegionStart][0];
		return c.totalPaths; 
	}

	public ArrayList<ArrayList<Pair<String>>> getExactSubsequences() {
		if (!hasOneSolution) return null;
		Cell c = cellMatrix[xMax][yMax];		
		ArrayList<ArrayList<Pair<String>>> result = new ArrayList<ArrayList<Pair<String>>>();
		c.addAllSubSequences(new ArrayList<Pair<String>>(), result);
		return result;
	}
	
	private void setTotalPaths() {
		Cell c = cellMatrix[xMax][yMax];
		long paths = 0l;
		for(int x=xRegionEnd; x>=xRegionStart; x--) {
			c = cellMatrix[x][yMax];			
			if (c.match) {
				c.totalPaths = ++paths;
			}
			else {
				c.totalPaths = paths;
			}
		}
		
		for(int y=yMax-1; y>=0; y--) {
			for(int x=xRegionEnd-1; x>=0; x--) {
				c = cellMatrix[x][y];
				c.totalPaths = cellMatrix[x+1][y].totalPaths;				
				if (c.match) {
					c.totalPaths += cellMatrix[x+1][y+1].totalPaths;
				}
			}
		}
		
	}

	public static ArrayList<ArrayList<Pair<String>>> getallExactSubsequences(
			String[] t1, String[] t2) {		
		AllOrderedStringExactSubsequences O = new AllOrderedStringExactSubsequences(t1, t2);		
		return O.getExactSubsequences();
	}
	
	public static ArrayList<ArrayList<Pair<String>>> getallExactSubsequencesBackupOnSimple(
			String[] t1, String[] t2) {		
		AllOrderedStringExactSubsequences O = new AllOrderedStringExactSubsequences(t1, t2);
		long combinations = O.getExactSubsequencesNumber();
		if (combinations==0) return null;
		if (combinations>maxComb) return O.getOneSolution();
		return O.getExactSubsequences();
	}
	
	protected class Cell {
		int x,y;
		boolean match, reachable, leftReachable, diagonalReachable;
		long totalPaths;
		
		protected Cell(int x, int y, boolean match) {
			this.x = x;
			this.y = y;
			if (match) {
				this.match = true;
				totalMatched++;
			}
		}
		
		public void setLeftReachable() {
			reachable = leftReachable = true;			
		}
		
		public void setDiagonalReachable() {
			reachable = diagonalReachable = true;			
		}
	
		
		public void addAllSubSequences( ArrayList<Pair<String>> currentThread, 
				ArrayList<ArrayList<Pair<String>>> result) {
			
			if (leftReachable && diagonalReachable) {
				
				ArrayList<Pair<String>> newThread = new ArrayList<Pair<String>>(currentThread);
				newThread.add(0,new Pair<String>(a1[x],a2[y]));
				cellMatrix[x-1][y-1].addAllSubSequences(newThread, result);
			
				cellMatrix[x-1][y].addAllSubSequences(currentThread, result);
			}
			
			else if (y==0) {
				if (leftReachable) {					
					if (match) {
						ArrayList<Pair<String>> newThread = new ArrayList<Pair<String>>(currentThread);
						cellMatrix[x-1][y].addAllSubSequences(newThread, result);
						currentThread.add(0,new Pair<String>(a1[x],a2[y]));
						result.add(currentThread);						
					}
					else cellMatrix[x-1][y].addAllSubSequences(currentThread, result);
				}
				else { // match
					currentThread.add(0,new Pair<String>(a1[x],a2[y]));
					result.add(currentThread);
				}
			}
			
			else {
				if (leftReachable) {
					cellMatrix[x-1][y].addAllSubSequences(currentThread, result);
				}
				else { //diagonalReachable
					currentThread.add(0,new Pair<String>(a1[x],a2[y]));
					cellMatrix[x-1][y-1].addAllSubSequences(currentThread, result);
				}
			}
			
		}
	}
	
	static char firstChar = 'A';
	static Random random  = new Random();
	public static String[] getRandomString(int length, int choices) {
		String[] result = new String[length];
		for(int i=0; i<length; i++) {
			char c = (char) (firstChar + random.nextInt(choices));
			result[i] = "" + c + (i+1);
		}
		return result;
	}

	public static void main(String[] args) {		 
		String[] a1 = new String[]{"A1", "C2", "C3", "A4", "A5", "C6", "A7", "A8", "C9"};
		String[] a2 = new String[]{"C1"};
		//for(int i=0; i<100000; i++) {
			//String[] a1 = getRandomString(55,3);
			//String[] a2 = getRandomString(3,3);			
			System.out.println(Arrays.toString(a1));
			System.out.println(Arrays.toString(a2));
			ArrayList<ArrayList<Pair<String>>> result =
				getallExactSubsequencesBackupOnSimple(a1, a2);					 
			//long cardinality = O.getExactSubsequencesNumber();
			//int size = result==null ? 0 : result.size();
			//if (size != cardinality) {				
				//System.out.println(cardinality);
				System.out.println(result.size());
				for(ArrayList<Pair<String>> pList : result) {
					System.out.println(pList);
				}
				//break;
			//}			
			//System.out.println();			
		//}
		
	}

	
}
