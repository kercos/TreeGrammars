package util;

import java.util.LinkedList;

public class BoundedLinkedList<T> {

	LinkedList<T> list;
	int maxSize;
	boolean reachedMax;
	
	public BoundedLinkedList (int maxSize) {
		this.maxSize = maxSize;
		list = new LinkedList<T>();
	}
	
	public LinkedList<T> getList() {
		return list;
	}
	
	public void add(T element) {		
		if (maxSize==0) return;
		if (reachedMax) list.pollFirst();			
		list.add(element);
		if (list.size()==maxSize) reachedMax = true;
	}
	
}
