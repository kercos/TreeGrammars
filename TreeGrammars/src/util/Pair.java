package util;

import java.util.Arrays;


public class Pair <T> {
	
	private T firstElement, secondElement;
	
	public Pair(T first, T second) {
		firstElement = first;
		secondElement = second;
	}
	
	public T getFirst() {
		return firstElement;
	}
	
	public T getSecond() {
		return secondElement;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof Pair)) return false;		
		Pair<T> otherPair = (Pair<T>) o;
		if (!firstElement.equals(otherPair.firstElement)) return false;
		return secondElement.equals(otherPair.secondElement);
	}
	
	public int hashCode() {
		return 31 * firstElement.hashCode() + secondElement.hashCode();
	}
	
	public String toString() {
		return "(" + firstElement.toString() + ", " + 
		secondElement.toString() + ")";
	}
}
