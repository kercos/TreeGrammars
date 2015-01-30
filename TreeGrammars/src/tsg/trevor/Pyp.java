package tsg.trevor;

import java.util.Arrays;

import util.Utility;

public class Pyp {
	
	static final double a = .5;
	static final double b = 0;


	static double logGamma(double x) {
	      double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
	      double ser = 1.0 + 76.18009173    / (x + 0)   - 86.50532033    / (x + 1)
	                       + 24.01409822    / (x + 2)   -  1.231739516   / (x + 3)
	                       +  0.00120858003 / (x + 4)   -  0.00000536382 / (x + 5);
	      return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
	}
	   
	static double gamma(double x) {
		return Math.exp(logGamma(x)); 
	}
	
	/**
	 * @param args
	 */
	public static void prob1(int[] peopleTables) {
		int K = peopleTables.length; //tables
		
		int n = Utility.sum(peopleTables); //people
		
		/*
		int[] z = new int[n];
		int x = 0;
		for(int i=0; i<K; i++) {
			int peopleAtI = peopleTables[i];
			for(int j=0; j<peopleAtI; j++) {				
				z[x++] = i+1;
			}
		}
		System.out.println(Arrays.toString(z));
		*/
		
		System.out.println("# people: " + n);
		
		
		double firstFactor = ((double)gamma(1+b))/gamma(n+b); 
		
		double secondFactor = 1;
		for(int k=1; k<K; k++) {
			secondFactor *= k*a+b;
		}
		
		double thirdFactor = 1;
		for(int k=1; k<=K; k++) {
			int nkminus = peopleTables[(k-1)]-1;
			if (nkminus==0)
				continue;
			thirdFactor *= gamma(nkminus-a)/gamma(1-a);
		}
		
		double result = firstFactor * secondFactor * thirdFactor;
		
		System.out.println("first: " + firstFactor);
		System.out.println("second: " + secondFactor);
		System.out.println("third: " + thirdFactor);
		
		System.out.println("result: " + result);
	}
	
	/**
	 * @param args
	 */
	public static double prob(int[] peopleTables) {		
		int K = peopleTables.length; //tables
		
		int n = Utility.sum(peopleTables); //people
		
		//System.out.println("# people: " + n);		
		
		double firstFactor = 1;
		for(int j=1; j<n; j++) {
			firstFactor *= 1/(j+b);
		}
		
		double secondFactor = 1;
		for(int k=1; k<K; k++) {
			secondFactor *= (k*a+b);
		}
		
		double thirdFactor = 1;
		for(int k=0; k<K; k++) {
			int peopleAtTableK = peopleTables[k];						
			for(int j=1; j<peopleAtTableK; j++) {
				thirdFactor *= (j-a);
			}			
		}
		
		double result = firstFactor * secondFactor * thirdFactor;
		
		//System.out.println("first: " + firstFactor);
		//System.out.println("second: " + secondFactor);
		//System.out.println("third: " + thirdFactor);
		
		//System.out.println("result: " + result);
		return result;
	}

	public static void main(String[] args) {
		//int[] peopleTables = new int[]{6,1,2,1};
		int[][] combs = new int[][]{
				{5,1,2,1,1},
				{6,1,2,1},				
				{7,2,1},				
				{9,1},
				{10}				
			};
		double result = 0;
		for(int[] peopleTables : combs) {			
			double prob = prob(peopleTables);
			result += prob;
			System.out.println(Arrays.toString(peopleTables) +  " " + prob);
		}
		System.out.println(result);
	}

}
