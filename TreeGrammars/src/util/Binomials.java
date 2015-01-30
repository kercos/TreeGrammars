package util;


public class Binomials {
	
	private static int absoluteMax = 15;
	
	int max;
	int[][] binomial;
	boolean noDiscontinuous;
	
	public Binomials(int max) {
		this(max,false);
	}
	
	public Binomials(int max, boolean noDiscontinuous) {
		this.noDiscontinuous = noDiscontinuous;
		if (max > absoluteMax) max = absoluteMax;
		this.max = max;		
		initBinomial();
	}
	
	private void initBinomial() {
		//  binomial(int c, int n) : | ways of choosing n elements from c |
		// c >= n		
		binomial = new int[max+1][max+1];
		Utility.fillDoubleIntArray(binomial, -1);
		if (noDiscontinuous) {
			for(int c=0; c<=max; c++) {
				for(int n=0; n<=c; n++) {
					binomial[c][n] = Utility.binomial(c, n);
				}
			}
		}
		else {
			for(int c=0; c<=max; c++) {
				for(int n=0; n<=c; n++) {
					binomial[c][n] = Utility.binomial_continuous(c, n);
				}
			}
		}			
	}

	public int get(int c, int n) {		
		return binomial[c][n];
	}
	
	public static void main(String[] args) {
		Binomials B = new Binomials(10);
		for(int c=0; c<=10; c++) {
			for(int n=0; n<=c; n++) {
				System.out.print(B.binomial[c][n] + "\t");
			}
			System.out.println();
		}
	}
	
}
