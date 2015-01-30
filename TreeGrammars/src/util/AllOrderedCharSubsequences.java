package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

public class AllOrderedCharSubsequences {

	boolean[][] equalMatrix;
	Character[] a1, a2;
	Cell[][] subSequencesMatrix;
	int[][] subSequencesSizeMatrix;
	int xMax, yMax;
	int xLength, yLength;
	int matched;
	
	public AllOrderedCharSubsequences(Character[] a1, Character[] a2) {
		this.a1 = a1;
		this.a2 = a2;
		this.xLength = a1.length;
		this.yLength = a2.length;
		this.xMax = a1.length-1;
		this.yMax = a2.length-1;
		this.equalMatrix = new boolean[xLength][yLength];
		this.subSequencesMatrix = new Cell[xLength][yLength];
		this.subSequencesSizeMatrix = new int[xLength][yLength];
		for(int i=0; i<a1.length; i++) {			
			for(int j=0; j<a2.length; j++) {
				subSequencesSizeMatrix[i][j] = -1;
				if (a1[i].equals(a2[j])) {
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
	
	public ArrayList<ArrayList<Pair<Character>>> getAllSubsequences() {
		ArrayList<ArrayList<Pair<Character>>> result = 
			new ArrayList<ArrayList<Pair<Character>>>();
		if (matched==0) return result;
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
					result.addAll(getAllSubsequences(startX, cellY));
				}					
			}
			if (startYExists) {
				for(int cellX=endX; cellX>startX; cellX--) {
					if (!equalMatrix[cellX][startY]) continue;
					endX = cellX;
					result.addAll(getAllSubsequences(cellX, startY));
				}					
			}								
			if (startXExists && startYExists && equalMatrix[startX][startY]) {
				result.addAll(getAllSubsequences(startX, startY));
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
	
	private ArrayList<ArrayList<Pair<Character>>> getAllSubsequences(int x, int y) {
		Cell stored = subSequencesMatrix[x][y]; 
		if (stored==null) {			
			stored = new Cell(x, y);
			subSequencesMatrix[x][y] = stored;
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
						stored.addSubSequences(getAllSubsequences(startX, cellY));
					}					
				}
				if (startYExists) {
					for(int cellX=endX; cellX>startX; cellX--) {
						if (!equalMatrix[cellX][startY]) continue;
						endX = cellX;
						stored.addSubSequences(getAllSubsequences(cellX, startY));
					}					
				}								
				if (startXExists && startYExists && equalMatrix[startX][startY]) {
					stored.addSubSequences(getAllSubsequences(startX, startY));
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
			if (stored.subSequences.isEmpty()) {
				stored.addDefaultSingletonPair();
			}
		}
		return stored.subSequences;
	}
	
	public static int indexOf(Character c, Character[] a) {
		for(int i=0; i<a.length; i++) {
			if (c==a[i]) return i;
		}
		return -1;
	}
	
	private class Cell {
		ArrayList<ArrayList<Pair<Character>>> subSequences;
		Pair<Character> pair;
		Character cX, cY;
		
		public Cell(int x, int y) {
			cX = a1[x];
			cY = a2[y];
			this.pair = new Pair<Character>(a1[x],a2[y]);
			subSequences = new ArrayList<ArrayList<Pair<Character>>>(); 
		}
		
		public void addDefaultSingletonPair() {
			ArrayList<Pair<Character>> singleton = new ArrayList<Pair<Character>>(1);
			singleton.add(new Pair<Character>(cX,cY));
			this.subSequences.add(singleton);
		}
		
		public void addSubSequences(ArrayList<ArrayList<Pair<Character>>> otherSubSequences) {			
			for(ArrayList<Pair<Character>> otherListPair : otherSubSequences) {
				ArrayList<Pair<Character>> newListPair = new ArrayList<Pair<Character>>(otherListPair.size()+1);
				newListPair.add(pair);
				newListPair.addAll(otherListPair);
				subSequences.add(newListPair);
			}
		}
		
		public String toString() {
			String result = "";
			for(ListIterator<ArrayList<Pair<Character>>> iter1 = 
				subSequences.listIterator(); iter1.hasNext();) {				
				ArrayList<Pair<Character>> s = iter1.next();
				result += "(";
				for(ListIterator<Pair<Character>> iter2 = s.listIterator(); 
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
				if (iter1.hasNext()) result += ", ";
			}
			return result;
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
	
	public static Character[] makeNewCharArray(Character[] array) {		
		Character[] result = new Character[array.length];
		for(int i=0; i<array.length; i++) {
			result[i] = new Character(array[i]);
		}
		return result;
	}
	
	public static void main1(String[] args) {
		//Character[] a1 = new Character[]{new Character('B'),new Character('D'),new Character('E')}; 
		//Character[] a2 = new Character[]{new Character('B'),new Character('G'),new Character('H')};
		//Character[] a1 = new Character[]{new Character('A'),new Character('B'),new Character('C')}; 
		//Character[] a2 = new Character[]{new Character('D'),new Character('E'),new Character('F')};
		Character[] a1 = makeNewCharArray(new Character[]{'N',',','N',',','N',',','N',',','N',',','N',',','C','N'});
		Character[] a2 = makeNewCharArray(new Character[]{'N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N',',','N','C','N'});
		AllOrderedCharSubsequences S = new AllOrderedCharSubsequences(a1,a2);
		ArrayList<ArrayList<Pair<Character>>> allS = S.getAllSubsequences(); 
		for(ArrayList<Pair<Character>> pairList : allS) {
			System.out.println(pairList);
		}	
		System.out.println(allS.size());
		//System.out.println(S.getNumberSubsequences());
	}
	
	public static void main(String[] args) {

		Character[] a1 = makeNewCharArray(new Character[]{'A','B','B','A','B','B'});		
		Character[] a2 = makeNewCharArray(new Character[]{'A','B','B'});
			
		AllOrderedCharSubsequences S2 = new AllOrderedCharSubsequences(a1,a2);
		ArrayList<ArrayList<Pair<Character>>> allS2 = S2.getAllSubsequences();
		
		System.out.println(Arrays.toString(a1));
		System.out.println(Arrays.toString(a2));		
		
		for(ArrayList<Pair<Character>> pairList : allS2) {
			System.out.println(toString(pairList, a1, a2));
		}
		System.out.println(allS2.size());
	}
	
}
