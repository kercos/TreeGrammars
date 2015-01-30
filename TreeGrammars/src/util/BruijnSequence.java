package util;

import java.io.FileNotFoundException;
import java.util.Arrays;

public class BruijnSequence {		
	
	/**
     * Given a one dimensional array of integer (i.e. {2,3,2}) the methods returns
     * a twodimensional array of integers given by means of all combinations
     * of indexes 
     * i.e. [[0, 0, 0], [0, 0, 1], [0, 1, 0], 
     * 		 [0, 1, 1], [0, 2, 0], [0, 2, 1], 
     * 		 [1, 0, 0], [1, 0, 1], [1, 1, 0], 
     * 		 [1, 1, 1], [1, 2, 0], [1, 2, 1]]
     */	
	public static int[][] combinations(int[] list) {
		int combinations = product(list);
		int[][] result = new int[combinations][list.length];
		if (list.length==1) {
			int row = 0;
			for(int i=0; i<list[0]; i++) {
				result[row][0] = i;
				row++;
			}
			return result;
		}
		if (list.length==2) {
			int row = 0;
			for(int i=0; i<list[0]; i++) {
				for(int j=0; j<list[1]; j++) {
					result[row][0] = i;
					result[row][1] = j;
					row++;
				}
			}
			return result;
		}
		int[] newList = new int[list.length-1];
		for(int i=0; i<list.length-1; i++) newList[i] = list[i];
		int[][] partialResult = combinations(newList);
		int row = 0;
		for(int i=0; i<partialResult.length; i++) {
			for(int k=0; k<list[list.length-1]; k++) {
				for(int j=0; j<list.length-1; j++) {
					result[row][j] = partialResult[i][j];
				}
				result[row][list.length-1] = k; 
				row++;
			}
		}
		return result;
	}
	
	/**
	* Given an input array of int the method returns the
	* product of the integers in it.
	*/   
	public static int product(int[] array) {
		int result = array[0];
		for(int i=1; i<array.length; i++) {
			result *= array[i];
		}
		return result;
	}


	/**
	 * 
	 * @param k number of symbols in the alphabet
	 * @param n length of the sequence
	 */
	public static void bruinSequences(int k, int n) {
		int[] list = new int[n];
		Arrays.fill(list, k);
		int[][] sequences = BruijnSequence.combinations(list);
		//for(int[] seq : sequences) {
		//	System.out.print(Arrays.toString(seq) + " ");
		//}
		int length = sequences.length;
		boolean[] taken = new boolean[length];
		
		int[] result = new int[length + n-1];
		int[] seq = sequences[0];
		for(int i=0; i<n; i++) {
			result[i] = seq[i];
		}
		taken[0] = true;
		int resIndex = n;
		int[] lastDigits = Arrays.copyOfRange(seq, 1, n);
		
		while(resIndex < result.length) {
			boolean found = false;
			for(int i=length-1; i>=0; i--) {
				if (taken[i]) continue;
				seq = sequences[i];
				int[] firstDigits = Arrays.copyOfRange(seq, 0, n-1);
				if (Arrays.equals(lastDigits, firstDigits)) {
					taken[i] = true;
					found = true;
					lastDigits = Arrays.copyOfRange(seq, 1, n);
					result[resIndex++] = seq[n-1];
					break;
				}
			}
			if (!found) {
				System.err.println("Sequence not found!");
				return;
			}			
		}
		
		System.out.println(Arrays.toString(result));
	}
	
	
	public static void main(String args[]) throws FileNotFoundException {
		 //k number of symbols in the alphabet
		 //n length of the sequence
		
		int kmax = 6;
		int nmax = 6;
		
		for(int k=2; k<=kmax; k++) {
			for(int n=2; n<=nmax; n++) {
				System.out.println("k=" + k + " n=" + n);
				bruinSequences(k,n);
				System.out.println();
			}		
		}		
	}

	

	

	

	

	

	






}
