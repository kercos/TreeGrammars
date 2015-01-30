package util;

public class Duet <T,S> {
	
	public T firstElement;
	public S secondElement;
	
	public Duet(T first, S second) {
		firstElement = first;
		secondElement = second;
	}
	
	public T getFirst() {
		return firstElement;
	}
	
	public S getSecond() {
		return secondElement;
	}

	
	public String toString() {
		return "<" + 
			(firstElement==null ? "null" : firstElement.toString()) + 
			", " + 
			(secondElement==null ? "null" : secondElement.toString()) + ">";
	}
	
	/*public boolean equals(Object anObject) {
		if (this == anObject) return true;
		if (!(anObject instanceof Duet)) return false;
		Duet<T,S> anotherDuet = (Duet<T,S>)anObject;
		return (this.firstElement.equals(anotherDuet.firstElement));
	}*/
}
