package util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.ArrayList;

import tsg.TSNodeLabel;

public class IdentitySet <T> implements Set<T> {
	
	ArrayList<T> set;

	public IdentitySet() {
		this.set = new ArrayList<T>();
	}
	
	public IdentitySet(Collection<? extends T> c) {
		this.set = new ArrayList<T>(c);
	}

	public boolean add(T e) {
		for(T t : set) {
			if (t==e) return false;
		}
		set.add(e);
		return true;
	}

	public boolean addAll(Collection<? extends T> c) {
		boolean changed = false;
		for(T t : c) {
			if (add(t)) changed = true;
		}		
		return changed;
	}

	public void clear() {
		set.clear();		
	}

	public boolean contains(Object o) {
		for(T t : set) {
			if (t==o) return true;
		}
		return false;
	}

	public boolean containsAll(Collection<?> c) {
		for(Object o : c) {			
			if (!contains(o)) return false;
		}
		return true;
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	public Iterator<T> iterator() {
		return set.iterator();
	}

	public boolean remove(Object o) {
		ListIterator<T> l = set.listIterator();
		while(l.hasNext()) {
			if (l.next()==o) {
				l.remove();
				return true;
			}
		}
		return false;
	}

	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for(Object o : c) {			
			if (remove(o)) changed = true;
		}
		return changed;
	}

	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		for(Object o : c) {
			ListIterator<T> l = set.listIterator();
			while(l.hasNext()) {
				if (!contains(o)) {				
					l.remove();
					changed = true;
				}
			}
		}		
		return changed;
	}

	public int size() {
		return set.size();
	}

	public Object[] toArray() {
		return set.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}


}
