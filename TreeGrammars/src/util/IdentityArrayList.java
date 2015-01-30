package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public class IdentityArrayList<E> extends ArrayList<E> {

	private static final long serialVersionUID = -8782127472986284140L;
	
	public IdentityArrayList(List<E> asList) {
		super(asList);
	}
	
	public IdentityArrayList(E[] array) {
		super(Arrays.asList(array));
	}


	public boolean contains(Object o) {
		for(E e : this) {
			if (e==o)
				return true;
		}
		return false;
	}
	
	
	public boolean equals(Object o) {
		  if (o == this)
	            return true;
	        if (!(o instanceof IdentityArrayList))
	            return false;

	        ListIterator<E> e1 = listIterator();
	        
	        @SuppressWarnings("unchecked")
			IdentityArrayList<E> otherList = ((IdentityArrayList<E>) o);
	        ListIterator<E> e2 = otherList.listIterator();
	        if (this.size()!=otherList.size()) {
	        	return false;
	        }
	        while (e1.hasNext()) {
	            E o1 = e1.next();
	            Object o2 = e2.next();
	            if (o1!=o2)
	            	return false;
	        }
	        return true;		
	}
	
	public static <E> boolean equals(List<E> l1, List<E> l2) {
		  if (l1 == l2)
	            return true;
		  if (l1.size()!=l2.size())
	        	return false;
		  
		  ListIterator<E> e1 = l1.listIterator();

		  @SuppressWarnings("unchecked")
		  List<E> otherList = ((List<E>) l2);
		  ListIterator<E> e2 = otherList.listIterator();

		  while (e1.hasNext()) {
			  E o1 = e1.next();
			  Object o2 = e2.next();
			  if (o1!=o2)
				  return false;
		  }
		  return true;	
	}

	public boolean isSublistOf(IdentityArrayList<E> o) {
		if (this.size()>=o.size()) {
			return false;
		}
		ListIterator<E> oLi = o.listIterator();
		ListIterator<E> tLi = null;
		while(oLi.hasNext()) {
			E oNext = oLi.next();
			tLi = this.listIterator();
			while(tLi.hasNext()) {				
				E tNext = tLi.next();
				if (oNext==tNext) {
					if (!tLi.hasNext())
						return true;
					oNext = oLi.next();
				}
				else
					break;
			}
		}
		return false;
	}
	
	public static void main(String[] args) {
		String a = "a";
		String b = "b";
		String c = "c";
		String d = "d";
		IdentityArrayList<String> l1 = new IdentityArrayList<String>(new String[]{a,b,c,d});
		IdentityArrayList<String> l2 = new IdentityArrayList<String>(new String[]{c,d});
		System.out.println(l2.isSublistOf(l1));
	}

	public String toString(char separator) {
		StringBuilder sb = new StringBuilder();
		Iterator<E> iter = this.iterator();
		while(iter.hasNext()) {
			sb.append(iter.next());
			if (iter.hasNext())
				sb.append(separator);
		}
		return sb.toString();
	}
	
}
