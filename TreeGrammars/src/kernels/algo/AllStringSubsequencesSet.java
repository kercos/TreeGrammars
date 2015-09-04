package kernels.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;

import util.IdentityArrayList;
import util.Pair;
import util.Utility;

public class AllStringSubsequencesSet {

	boolean[][] equalMatrix;
	String[] a1, a2;
	int[][] subSequencesSizeMatrix;
	int xMax, yMax;
	int xLength, yLength;
	int matched;
	
	// from AllOrderedCharSubsequencesNew
	
	public AllStringSubsequencesSet(String[] a1, String[] a2) {
		this.a1 = a1;
		this.a2 = a2;
		this.xLength = a1.length;
		this.yLength = a2.length;
		this.xMax = a1.length-1;
		this.yMax = a2.length-1;
		this.equalMatrix = new boolean[xLength][yLength];
		this.subSequencesSizeMatrix = new int[xLength][yLength];
		for(int i=0; i<a1.length; i++) {			
			for(int j=0; j<a2.length; j++) {
				subSequencesSizeMatrix[i][j] = -1;
				if (a1[i]==a2[j]) { //assuming interned
					this.equalMatrix[i][j] = true;
					matched++;
				}				
			}
		}
	}
	
	public int getNumberSubsequences() {
		if (matched==0) return 0;
		int result = 0;
		int startX = 0;
		int startY = 0;
		int endY = yMax;
		int endX = xMax;
		boolean startXExists = startX < xLength;
		boolean startYExists = startY < yLength;
		while(startXExists || startYExists) {
			if (startXExists) {
				for(int cellY=endY; cellY>startY; cellY--) {
					if (!equalMatrix[startX][cellY]) continue;
					endY = cellY;
					result += getNumberSubsequences(startX, cellY);
				}					
			}
			if (startYExists) {
				for(int cellX=endX; cellX>startX; cellX--) {
					if (!equalMatrix[cellX][startY]) continue;
					endX = cellX;
					result += getNumberSubsequences(cellX, startY);
				}					
			}								
			if (startXExists && startYExists && equalMatrix[startX][startY]) {
				result += getNumberSubsequences(startX, startY);
				break;
			}
			if (startXExists) {
				if (startX+1 <= endX) startX++;
				else startXExists = false;
			}
			if (startYExists) {
				if (startY+1 <= endY) startY++;
				else startYExists = false;
			}
		}
		return result;
	}
	
	private int getNumberSubsequences(int x, int y) {
		int stored = subSequencesSizeMatrix[x][y]; 
		if (stored!=-1) return stored;
		
		stored = 0;		
		int startX = x+1;
		int startY = y+1;
		int endY = yMax;
		int endX = xMax;
		boolean startXExists = startX < xLength;
		boolean startYExists = startY < yLength;
		while(startXExists || startYExists) {
			if (startXExists) {
				for(int cellY=endY; cellY>startY; cellY--) {
					if (!equalMatrix[startX][cellY]) continue;
					endY = cellY;
					stored += getNumberSubsequences(startX, cellY);
				}					
			}
			if (startYExists) {
				for(int cellX=endX; cellX>startX; cellX--) {
					if (!equalMatrix[cellX][startY]) continue;
					endX = cellX;
					stored += getNumberSubsequences(cellX, startY);
				}					
			}								
			if (startXExists && startYExists && equalMatrix[startX][startY]) {
				stored += getNumberSubsequences(startX, startY);
				break;
			}
			if (startXExists) {
				if (startX+1 <= endX) startX++;
				else startXExists = false;
			}
			if (startYExists) {
				if (startY+1 <= endY) startY++;
				else startYExists = false;
			}
			if (stored==0) stored = 1;
		}
		return subSequencesSizeMatrix[x][y] = stored;
	}
	
	public HashSet<IdentityArrayList<String>> getAllSubsequences() {
		return getAllSubsequences(0,0,true);
	}
	
	private HashSet<IdentityArrayList<String>> getAllSubsequences(int x, int y, boolean sumAll) {
		HashSet<IdentityArrayList<String>> result = new HashSet<IdentityArrayList<String>>();
		int startX = sumAll ? x : x+1;
		int startY = sumAll ? y : y+1;
		int endY = yMax;
		int endX = xMax;
		boolean startXExists = startX < xLength;
		boolean startYExists = startY < yLength;
		while(startXExists || startYExists) {
			if (startXExists) {
				for(int cellY=endY; cellY>startY; cellY--) {
					if (!equalMatrix[startX][cellY]) continue;
					endY = cellY;
					addSubSequences(result, getAllSubsequences(startX, cellY, false), x, y, sumAll);
				}					
			}
			if (startYExists) {
				for(int cellX=endX; cellX>startX; cellX--) {
					if (!equalMatrix[cellX][startY]) continue;
					endX = cellX;
					addSubSequences(result, getAllSubsequences(cellX, startY, false), x, y, sumAll);
				}					
			}								
			if (startXExists && startYExists && equalMatrix[startX][startY]) {
				addSubSequences(result, getAllSubsequences(startX, startY, false), x, y, sumAll);
				break;
			}
			if (startXExists) {
				if (startX+1 <= endX) startX++;
				else startXExists = false;
			}
			if (startYExists) {
				if (startY+1 <= endY) startY++;
				else startYExists = false;
			}
		}
		if (result.isEmpty()) {
			result.add(new IdentityArrayList<String>(new String[]{a1[x]}));
		}
	return result;
	}
	
	public static int indexOf(Character c, Character[] a) {
		for(int i=0; i<a.length; i++) {
			if (c==a[i]) return i;
		}
		return -1;
	}
	
	public void addSubSequences(HashSet<IdentityArrayList<String>> result,
			HashSet<IdentityArrayList<String>> otherSubSequences,
			int x, int y, boolean sumAll) {			
		if (sumAll) {
			result.addAll(otherSubSequences);
		}
		else {
			for(IdentityArrayList<String> otherListPair : otherSubSequences) {
				IdentityArrayList<String> newListPair = new IdentityArrayList<String>(otherListPair.size()+1);
				newListPair.add(a1[x]);
				newListPair.addAll(otherListPair);
				result.add(newListPair);
			}
		}
	}
	
	public static String toString(ArrayList<Pair<Character>> pairList,
			Character[] a1, Character[] a2) {
		String result = "(";
		for(ListIterator<Pair<Character>> iter2 = pairList.listIterator(); 
				iter2.hasNext();) {
			Pair<Character> p = iter2.next();
			Character c1 = p.getFirst();
			Character c2 = p.getSecond();
			int i1 = indexOf(c1, a1);
			int i2 = indexOf(c2, a2);
			result += "(" + c1 + i1 + "," + c2 + i2 + ")";
			if (iter2.hasNext()) result += ", ";
		}
		result += ")";
		return result;
	}
	
	public static void intern(String[] array) {		
		for(int i=0; i<array.length; i++) {
			array[i] = array[i].intern();
		}
	}
	
	static boolean verbose = false;
	
	public static HashSet<IdentityArrayList<String>> getAllMaxCommonSubstringsIdentity(
			String[] a1, String[] a2, int minLength) {
		AllStringSubsequencesSet S2 = new AllStringSubsequencesSet(a1,a2);
		HashSet<IdentityArrayList<String>> result = S2.getAllSubsequences();
		Iterator<IdentityArrayList<String>> iter = result.iterator();
		while(iter.hasNext()) {
			if (iter.next().size()<minLength)
				iter.remove();
		}
		
		if (verbose) {
			String[] xPrint = flatForPrint(a1);
			String[] yPrint = flatForPrint(a2);
			System.out.println("Eq array: ");
			Utility.printChart(S2.equalMatrix, yPrint, xPrint);
			System.out.println("\nOpt array: ");
			Utility.printChart(S2.subSequencesSizeMatrix, yPrint, xPrint);
		}

		
		
		return result;
	}
	
	public static String[] flatForPrint(String[] x) {
		int l = x.length;
		String[] result = new String[l+2];
		result[0] = "";
		for(int i=0; i<l; i++) {
			result[i+1] = x[i]; 
		}
		result[l+1] = "";
		return result;
	}
	
	public static void main(String[] args) {

		//String[] a1 = new String[]{"A","B","C","D","E","F"};		
		//String[] a2 = new String[]{"A","B","Z","C","D","F","A","B","C","E"};
		
		verbose = true;
		
		String[] a1 = new String("you don 't even take the name of the country for granted").split("\\s");
		String[] a2 = new String("we can take it completely for granted").split("\\s");
		
		for (int i = 0; i < a1.length; i++) {
			a1[i] = a1[i].intern();
		}
		for (int i = 0; i < a2.length; i++) {
			a2[i] = a2[i].intern();
		}
		
		HashSet<IdentityArrayList<String>> set = getAllMaxCommonSubstringsIdentity(a1, a2, 2);
		System.out.println(set);
		
			
		/*
		AllStringSubsequencesSet S2 = new AllStringSubsequencesSet(a1,a2);
		HashSet<IdentityArrayList<String>> allS2 = S2.getAllSubsequences();
		
		System.out.println(Arrays.toString(a1));
		System.out.println(Arrays.toString(a2));		
		
		System.out.println(allS2.size());
		
		for(IdentityArrayList<String> sub : allS2) {
			System.out.println(sub);
		}
		*/
		
	}
	
}
