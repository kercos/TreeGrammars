package util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ShortArray {
	
	public short[] array;
	
	public ShortArray(ShortArray sa) {
		this(sa.array);
	}
	
	public ShortArray(short[] a) {
		this.array = Arrays.copyOf(a, a.length);
	}
	
	public ShortArray(List<Short> l) {
		this.array = toArray(l);
	}
	
	public void add(short s) {
		int newLength = array.length+1;
		array = Arrays.copyOf(array, newLength);		
		array[newLength-1]=s;
	}
	
	public int size() {
		return array.length;
	}
	
	public static short[] toArray(List<Short> l) {
		int size = l.size();
		short[] result = new short[size];
		Iterator<Short> lIter = l.iterator();
		for(int i=0; i<size; i++) {
			result[i]=lIter.next();
		}
		return result;
	}
	
	public void addAll(short[] secondArray) {
		int previousLength = this.array.length;
		int newLength = previousLength + secondArray.length;		
		array = Arrays.copyOf(array, newLength);
		int j = 0;
		for(int i=previousLength; i<newLength; i++) {
			array[i] = secondArray[j++];
		}
	}
	
	public short removeFirst() {
		short result = this.array[0];
		int length = array.length;
		array = length==1 ? new short[0] : Arrays.copyOfRange(array, 1, length);
		return result;
	}
	
	public boolean isEmpty() {
		return array.length==0;
	}
	
	public ShortArray clone() {
		return new ShortArray(Arrays.copyOf(array, array.length));		
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this)
			return true;
		if (o instanceof ShortArray) {
			return Arrays.equals(((ShortArray)o).array,this.array);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.array);
	}
	
	public String toString() {
		return Arrays.toString(array);
	}
	
	public static void main(String[] args) {
		ShortArray a = new ShortArray(new short[]{1,2,3,4});
		System.out.println(a.toString());
		a.addAll(new short[]{5,6});
		System.out.println(a.toString());
	}
}
