package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class InvertArray {

	public static void main(String[] args) {
		System.out.println("Write an array of number separated by spaces (e.g. '1 2 3 4 5')");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
	    try {
			s = in.readLine();
		} catch (IOException e) {
			System.err.println("Some error occured.");			
			return;
		}
	    String[] numbersString = s.split("\\s+");	    
	    int length = numbersString.length;
	    int[] invertedArray = new int[length];
	    int j=length-1;
	    for(int i=0; i<length; i++) {
	    	String num = numbersString[i];
	    	try{	    		
	    		int n = Integer.parseInt(numbersString[i]);
	    		invertedArray[j--] = n;
	    	}
	    	catch(NumberFormatException e) {
	    		System.err.println("Wrong number: " + num);			
				return;
	    	}	    	
	    }
	    System.out.println("Inverted array: " + Arrays.toString(invertedArray));	    
	}
	
}
