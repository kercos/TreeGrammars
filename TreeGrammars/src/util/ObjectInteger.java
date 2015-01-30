package util;

public class ObjectInteger<T> implements Comparable<ObjectInteger<T>> {

	static int counter = 0;
	
	T object;
	private int integer;
	int id;
	
	public ObjectInteger(T o, int i) {
		this.object = o;
		this.setInteger(i);
		id = counter++;
	}
	
	public T getObject() {
		return object;
	}
	
	public int getInteger() {
		return integer;
	}

	public void setInteger(int integer) {
		this.integer = integer;
	}

	public int compareTo(ObjectInteger<T> o) {
		if (this.integer<o.integer) return -1;
		if (this.integer>o.integer) return 1;
		if (this.id<o.id) return -1;
		return 1;
	}

	
}
