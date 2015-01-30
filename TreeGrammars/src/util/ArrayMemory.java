package util;

public class ArrayMemory {

	long[][] array;
	
	public ArrayMemory(int size) {
		array = new long[size][size];
		for(int i=0; i<size; i++) {
			for(int j=0; j<size; j++) {
				array[i][j] = (long)(Math.random() * Integer.MAX_VALUE);
			}
		}
		System.out.println("done");
	}
	
	public static void main(String[] args) {
		new ArrayMemory(40000);
	}
}
