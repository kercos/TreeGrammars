package util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class BoundedPriorityDoubleList<T> extends PriorityQueue<T>{

	int maxSize;
	boolean reachedMax;
	
	public BoundedPriorityDoubleList (int maxSize) {
		super();
		this.maxSize = maxSize;
		reachedMax = maxSize==0;
	}
	
	
	public boolean add(T e) {
		super.add(e);
		if (reachedMax) {
			T r = poll();
			return r!=e;
		}
		
		if (size()==maxSize) 
			reachedMax = true;
		
		return true;
	}
	
	public static void main(String[] args) {
		
		BoundedPriorityDoubleList<ObjectDouble<Character>> p = 
			new BoundedPriorityDoubleList<ObjectDouble<Character>>(10);
		for(int i=0; i<100000; i++) {
			double random = Math.random();
			ObjectDouble<Character> od = new ObjectDouble<Character>(new Character('c'), random);
			p.add(od);
		}
		Iterator<ObjectDouble<Character>> iter = p.iterator();
		while(iter.hasNext()) {
			System.out.println(iter.next().getDoubleValue() + " ");
		}
	}
	
}
