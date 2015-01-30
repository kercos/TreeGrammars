package util;



public class IdentityPair<E> {

	E e1, e2;
	
	public IdentityPair(E[] a) {
		this.e1 = a[0];
		this.e2 = a[1];
	}
	
	public IdentityPair(E e1, E e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	public boolean contains(Object o) {
		return o==e1 || o==e2;
	}
	
	
	public boolean equals(Object o) {
		  if (o == this)
	            return true;
	        if (!(o instanceof IdentityPair))
	            return false;

	        
	        @SuppressWarnings("unchecked")
			IdentityPair<E> otherIP = ((IdentityPair<E>) o);
	        return this.e1==otherIP.e1 && this.e2==otherIP.e2;		
	}
	
	public int hashCode() {
		return 31 * (31 + e1.hashCode()) + e2.hashCode();
	}
	
}
