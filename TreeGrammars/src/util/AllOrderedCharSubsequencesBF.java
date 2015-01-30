package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.ListIterator;

public class AllOrderedCharSubsequencesBF {

	Character[] a1, a2;
	
	
	public AllOrderedCharSubsequencesBF(Character[] a1, Character[] a2) {
		this.a1 = a1;
		this.a2 = a2;		
	}
	
	public static Character[][] getAllSubSequences(Character[] a) {
		int resultSize = (int)Math.pow(2, a.length)-1;
		Character[][] result = new Character[resultSize][];
		int i=0;
		for(int l=1; l<=a.length; l++) {
			int[][] nairs = Utility.n_air(a.length, l);			
			for(int[] comb : nairs) {
				Character[] result_i = new Character[l];
				for(int j=0; j<l; j++) {
					result_i[j] = a[comb[j]];
				}
				result[i] = result_i;
				i++;								
			}				
		}
		return result;
	}
	
	public static boolean equals(Character[] a, Character[] b) {
		if (a.length != b.length) return false;
		for(int i=0; i<a.length; i++) {
			if (!a[i].equals(b[i])) return false;
		}
		return true;
	}
	
	public ArrayList<ArrayList<Pair<Character>>> getAllSubsequences() {
		ArrayList<ArrayList<Pair<Character>>> result = 
			new ArrayList<ArrayList<Pair<Character>>>();
		Character[][] allSubSequencesA1 = getAllSubSequences(a1);
		Character[][] allSubSequencesA2 = getAllSubSequences(a2);
		for(Character[] subA1 : allSubSequencesA1) {
			for(Character[] subA2 : allSubSequencesA2) {
				if (equals(subA1, subA2)) {
					ArrayList<Pair<Character>> pairList = buildPairList(subA1, subA2);
					addClosure(result, pairList);
				}
			}
		}
		return result;
	}
	
	public static void addClosure(
			ArrayList<ArrayList<Pair<Character>>> closureList,
			ArrayList<Pair<Character>> pairList) {
		ListIterator<ArrayList<Pair<Character>>> iter = closureList.listIterator();
		while(iter.hasNext()) {			
			ArrayList<Pair<Character>> list = iter.next();
			if (list.size()==pairList.size()) continue;
			if (pairList.size()>list.size()) {
				if (isSuperSet(pairList, list)) iter.remove();
			}
			else if (isSuperSet(list, pairList)) return;
		}
		closureList.add(pairList);
	}
	
	public static ArrayList<Pair<Character>> buildPairList(
			Character[] subA1, Character[] subA2) {
		ArrayList<Pair<Character>> result = new ArrayList<Pair<Character>>(subA1.length);
		for(int i=0; i<subA1.length; i++) {
			Pair<Character> pair = new Pair<Character>(subA1[i],subA2[i]);
			result.add(pair);
		}
		return result;
	}
	
	public static boolean isSuperSet(
			ArrayList<Pair<Character>> bigPairList,
			ArrayList<Pair<Character>> smallPairList) {
		ListIterator<Pair<Character>> smallLI = smallPairList.listIterator();
		ListIterator<Pair<Character>> bigLI = bigPairList.listIterator();
		while(smallLI.hasNext()) {
			Pair<Character> p1 = smallLI.next();
			boolean found = false;
			while(bigLI.hasNext()) {
				Pair<Character> p2 = bigLI.next();
				if (p1.getFirst()==p2.getFirst() && 
						p1.getSecond()==p2.getSecond()) {
					found = true;
					break;
				}
			}				
			if (!found) return false;
		}
		return true;
	}
	
	public static boolean isEqualPairList(
			ArrayList<Pair<Character>> pairList1,
			ArrayList<Pair<Character>> pairList2) {		
		if (pairList1.size() != pairList2.size()) return false;
		ListIterator<Pair<Character>> i1 = pairList1.listIterator();
		ListIterator<Pair<Character>> i2 = pairList2.listIterator();
		while(i1.hasNext()) {
			Pair<Character> p1 = i1.next();
			Pair<Character> p2 = i2.next();
			if (p1.getFirst()!=p2.getFirst() || 
					p1.getSecond()!=p2.getSecond()) return false;
		}
		return true;
	}
			
	
	public static boolean isEqual(
			ArrayList<ArrayList<Pair<Character>>> allS1,
			ArrayList<ArrayList<Pair<Character>>> allS2) {		
		if (allS1.size() != allS2.size()) return false;
		BitSet indexMatched = new BitSet(allS1.size());
		int index1 = 0;
		for(ArrayList<Pair<Character>> pairList1 : allS1) {
			int index2=0;
			boolean found = false;
			for(ArrayList<Pair<Character>> pairList2 : allS2) {
				if (!indexMatched.get(index2)) {
					if (isEqualPairList(pairList1, pairList2)) {
						found = true;
						indexMatched.set(index2);
						break;
					}
				}
				index2++;
			}
			if (!found) {
				return false;
			}
			index1++;
		}
		return true;
	}
	
	public static int indexOf(Character c, Character[] a) {
		for(int i=0; i<a.length; i++) {
			if (c==a[i]) return i;
		}
		return -1;
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
	
	public static void main1(String[] args) {
		Character[] a1 = new Character[]{new Character('A'),new Character('B'),new Character('C'),new Character('C'),new Character('A'),new Character('F'),new Character('G'),new Character('A'),new Character('A'),new Character('I'),new Character('L')}; 
		Character[] a2 = new Character[]{new Character('A'),new Character('B'),new Character('C'),new Character('D'),new Character('F'),new Character('A'),new Character('C'),new Character('F'),new Character('G'),new Character('H')};
		//Character[] a1 = new Character[]{new Character('A'),new Character('B'),new Character('C')}; 
		//Character[] a2 = new Character[]{new Character('A'),new Character('B'),new Character('B')};
		AllOrderedCharSubsequencesBF S = new AllOrderedCharSubsequencesBF(a1,a2);
		ArrayList<ArrayList<Pair<Character>>> allS = S.getAllSubsequences(); 
		for(ArrayList<Pair<Character>> pairList : allS) {
			System.out.println(pairList);
		}	
		System.out.println(allS.size());
	}
	
	public static Character[] makeNewRandomCharArray(Character[] alphabet, int size) {
		int lastAlphaChar = alphabet.length-1;
		Character[] result = new Character[size];
		for(int i=0; i<size; i++) {
			result[i] = new Character(
					alphabet[Utility.randomInteger(lastAlphaChar)]);
		}
		return result;
	}
	
	public static Character[] makeNewCharArray(Character[] array) {		
		Character[] result = new Character[array.length];
		for(int i=0; i<array.length; i++) {
			result[i] = new Character(array[i]);
		}
		return result;
	}
	
	public static void main4(String[] args) {

		Character[] alphabet = new Character[]{'A','B','C','D','E'};
		int maxLength = 8;
		
		while(true) {
			int a1Size = Utility.randomInteger(maxLength);		
			Character[] a1 = makeNewRandomCharArray(alphabet, a1Size);
			
			int a2Size = Utility.randomInteger(maxLength);
			Character[] a2 = makeNewRandomCharArray(alphabet, a2Size);
			
			
			AllOrderedCharSubsequencesBF S1 = new AllOrderedCharSubsequencesBF(a1,a2);
			ArrayList<ArrayList<Pair<Character>>> allS1 = S1.getAllSubsequences();
			
			AllOrderedCharSubsequences S2 = new AllOrderedCharSubsequences(a1,a2);
			ArrayList<ArrayList<Pair<Character>>> allS2 = S2.getAllSubsequences();
			
			System.out.println(Arrays.toString(a1));
			System.out.println(Arrays.toString(a2));		
			
			boolean isEqual = isEqual(allS1, allS2);
			if (isEqual) System.out.println("CORRECT\n"); 
			else {
				System.out.println("NON CORRECT:\n");
				for(ArrayList<Pair<Character>> pairList : allS1) {
					System.out.println(toString(pairList, a1, a2));
				}
				System.out.println("------------");
				for(ArrayList<Pair<Character>> pairList : allS2) {
					System.out.println(toString(pairList, a1, a2));
				}			
				break;
			}
		}
	}
	
	public static void main(String[] args) {

		Character[] a1 = makeNewCharArray(new Character[]{'B','C','B','D'});		
		Character[] a2 = makeNewCharArray(new Character[]{'B','B','C'});
			
		AllOrderedCharSubsequencesBF S1 = new AllOrderedCharSubsequencesBF(a1,a2);
		ArrayList<ArrayList<Pair<Character>>> allS1 = S1.getAllSubsequences();
		
		AllOrderedCharSubsequences S2 = new AllOrderedCharSubsequences(a1,a2);
		ArrayList<ArrayList<Pair<Character>>> allS2 = S2.getAllSubsequences();
		
		System.out.println(Arrays.toString(a1));
		System.out.println(Arrays.toString(a2));		
		
		boolean isEqual = isEqual(allS1, allS2);
		if (isEqual) System.out.println("CORRECT\n"); 
		else System.out.println("NON CORRECT:\n");
		for(ArrayList<Pair<Character>> pairList : allS1) {
			System.out.println(toString(pairList, a1, a2));
		}
	}
}
