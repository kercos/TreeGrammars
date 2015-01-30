package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;

class Brench {
		
	int[] startBlock;
	ArrayList<Brench> subBrenches;
	boolean isValidPath;
		
	public Brench(int[] startBlock) {
		this.startBlock = startBlock;
	}

	int startBlockSize() {
		return startBlock.length;
	}
	
	boolean isFinalBlock() {
		return subBrenches==null;
	}
	
	public ArrayList<ArrayList<int[]>> getAllPaths() {
		ArrayList<ArrayList<int[]>> result = new ArrayList<ArrayList<int[]>>();		
		if (isFinalBlock()) {
			ArrayList<int[]> uniquePath = new ArrayList<int[]>();
			uniquePath.add(startBlock);
			result.add(uniquePath);
		}
		else {
			for(Brench sb : subBrenches) {
				ArrayList<ArrayList<int[]>> subPaths = sb.getAllPaths();
				for(ArrayList<int[]> path : subPaths) {
					path.add(0, startBlock);
				}
				result.addAll(subPaths);
			}
		}
		return result;
	}
	
	public String toString() {
		return Arrays.toString(startBlock) + (subBrenches==null? " X " : " ...");
	}
	
	public boolean startBlockIsConsecutive() {
		int size = startBlock.length;
		return startBlock[0] + size - 1 == startBlock[size-1];
	}
	
	/**
	 * 
	 * @return true if startBlock has non-consecutive elements or
	 * has removed all subBrenches because they were all non-consecutive.
	 */
	public boolean removeNonConsecutiveRecursive() {
		if (!startBlockIsConsecutive())
			return true;
		if (subBrenches!=null) {
			for(ListIterator<Brench> iter = subBrenches.listIterator(); iter.hasNext();) {
				Brench next = iter.next();
				if (next.removeNonConsecutiveRecursive()) {
					iter.remove();
				}
			}
			if (subBrenches.isEmpty())
				return true;			
		}
		return false;		
	}
	
}

public class SetPartition {
	
	int length;	
	ArrayList<Brench> roots;

	public SetPartition(int length) {
		this.length = length;
		BitSet unseenIndexes = new BitSet(length);
		unseenIndexes.set(0, length);
		roots = createBrenchRecursive(unseenIndexes, null);
	}
	
	public static ArrayList<Brench> createBrenchRecursive(BitSet remainingIndexes, Brench previousBrench) {		
		ArrayList<Brench> result = new ArrayList<Brench>();
		if (remainingIndexes.isEmpty())
			return result;
		int remainingIndexesSetSize = remainingIndexes.cardinality();		
		int previousLength = previousBrench==null ? -1 : previousBrench.startBlockSize(); 
		int ceiling = previousBrench==null ? remainingIndexesSetSize : Math.min(remainingIndexesSetSize, previousLength);
		//New block should be smaller than previous block 
		//in terms of size of lexicographically ordering
		for (int i = ceiling; i >= 1; i--) {
			int[][] subPartStart = Utility.n_air(remainingIndexesSetSize, i);
			int[] remainingIndexesArray = Utility.makeArray(remainingIndexes);
			int[][] subPartStartIndexes = Utility.translateIndexes(subPartStart, remainingIndexesArray);
			for (int[] start : subPartStartIndexes) {
				if (i == previousLength && !smallerLexOrder(start,previousBrench.startBlock))
					continue;
				BitSet newRemainingIndexes = (BitSet) remainingIndexes.clone();
				for(int s : start) {
					newRemainingIndexes.set(s, false);					
				}
				Brench path = new Brench(start);
				ArrayList<Brench> subBrenches = createBrenchRecursive(newRemainingIndexes, path);				
				for(Iterator<Brench> iter = subBrenches.iterator(); iter.hasNext(); ) {
					if (!iter.next().isValidPath) 
						iter.remove();
				}				
				if (!subBrenches.isEmpty()) {					
					path.subBrenches = subBrenches;
				}
				else if (!newRemainingIndexes.isEmpty()) {
					// path is not a valid path (not consumed all remaining indexes)
					continue;
				}
				path.isValidPath = true;
				result.add(path);
			}
		}			
		return result;
	}
	
	private static boolean smallerLexOrder(int[] newBlock, int[] previousBlock) {
		//two blocks have same size 		
		for(int i=0; i<newBlock.length; i++) {
			if (newBlock[i]>previousBlock[i])
				return false;
			if (newBlock[i]<previousBlock[i])
				return true;
		}
		return true;
	}
	
	public void removeNonConsecutivePartitions() {
		for(ListIterator<Brench> iter = roots.listIterator(); iter.hasNext();) {
			Brench next = iter.next();
			if (next.removeNonConsecutiveRecursive()) {
				iter.remove();
			}
		}
	}
	
	public void removeUniquePartition() {
		for(ListIterator<Brench> iter = roots.listIterator(); iter.hasNext();) {
			Brench next = iter.next();
			if (next.startBlockSize()==length) {
				iter.remove();
				break;
			}
		}
	}
	
	public ArrayList<ArrayList<int[]>> getAllPaths(boolean removeNonConsecutive, boolean removeUnique) {
		if (removeNonConsecutive)
			this.removeNonConsecutivePartitions();
		if (removeUnique)
			this.removeUniquePartition();
		ArrayList<ArrayList<int[]>> result = new ArrayList<ArrayList<int[]>>();
		for(Brench r : roots) {
			result.addAll(r.getAllPaths());
		}
		return result;
	}
	
	public int[][][] getAllPathsArray(boolean removeNonConsecutive, boolean removeUnique) {
		ArrayList<ArrayList<int[]>> al = getAllPaths(removeNonConsecutive, removeUnique);
		int[][][] result = new int[al.size()][][];
		for(ListIterator<ArrayList<int[]>> i = al.listIterator(); i.hasNext();) {
			ArrayList<int[]> ei = i.next();
			int[][] subRes = new int[ei.size()][];
			for(ListIterator<int[]> j = ei.listIterator(); j.hasNext(); ) {
				subRes[j.nextIndex()] = j.next();								
			}
			result[i.previousIndex()] = subRes;
		}
		return result;
	}
	
	private static String pathToString(ArrayList<int[]> path) {
		StringBuilder sb = new StringBuilder();
		//sb.append('[');		
		for(Iterator<int[]> i = path.iterator(); i.hasNext(); ) {
			int[] next = i.next();
			//sb.append(Arrays.toString(next));
			sb.append('[');
			for(int j=0; j<next.length; j++) {
				sb.append(next[j]);
				if (j!=next.length-1)
					sb.append(' ');
			}
			sb.append(']');
			if (i.hasNext())
				sb.append(',');
		}
		//sb.append(']');
		return sb.toString();
	}
	
	public String toString(boolean removeNonConsecutive, boolean removeUnique) {
		StringBuilder sb = new StringBuilder();
		ArrayList<ArrayList<int[]>> allPaths = this.getAllPaths(removeNonConsecutive, removeUnique);
		//sb.append('[');
		for(Iterator<ArrayList<int[]>> pathIter = allPaths.iterator(); pathIter.hasNext(); ) {
			sb.append(pathToString(pathIter.next()));
			if (pathIter.hasNext())
				sb.append('\n');
		}
		//sb.append(']');
		return sb.toString();
	}
	
	public String toString() {
		return toString(false, false);
	}
	
	public static int[][][] getAllPartitions(int n, boolean removeConsecutive,  boolean removeUnique) {
		SetPartition sp = new SetPartition(n);
		return sp.getAllPathsArray(removeConsecutive, removeUnique);
	}
	
	public static boolean samePartitions(File a, File b) throws FileNotFoundException {
		ArrayList<String[]> partA = new ArrayList<String[]>();
		ArrayList<String[]> partB = new ArrayList<String[]>();
		Scanner scan = new Scanner(a);		 
		while(scan.hasNextLine()) {
			String l = scan.nextLine();
			partA.add(l.split(","));
		}
		scan = new Scanner(b);		 
		while(scan.hasNextLine()) {
			String l = scan.nextLine();
			partB.add(l.split(","));
		}
		if (partA.size() != partB.size())
			return false;
		for(String[] aList : partA) {
			boolean found = false;
			Arrays.sort(aList);
			for(String[] bList : partB) {
				Arrays.sort(bList);
				if (Arrays.equals(aList, bList)) {
					found = true;
					break;
				}
			}
			if (!found)
				return false;
		}
		return true;
	}
	
	public static void comparePartitions() throws FileNotFoundException {
		File[] partFede = new File("/home/sangati/tmp/partizioniFede/").listFiles();
		File[] partSte = new File("/home/sangati/tmp/partizioniStefano/").listFiles();
		Arrays.sort(partFede);
		Arrays.sort(partSte);
		for(int i=0; i<7; i++) {
			File f = partFede[i];
			File s = partSte[i];
			System.out.println("Test file " + f.getName() + " & " + s.getName() + ": " + (samePartitions(f,s) ? "YES" : "NO"));
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		
		//comparePartitions();
		
		/*
		for(int i=2; i<10; i++) {
			SetPartition sp = new SetPartition(i);
			File outputFile = new File("/home/sangati/tmp/partizioniFede/partitions" + "_" + Utility.padZero(2, i));
			String output = sp.toString(true, false);
			FileUtil.append(output, outputFile);			
		}
		*/
		
		
		SetPartition sp = new SetPartition(3);		
		System.out.println(sp.toString(true, true));
		
		
	}
	
	

}
