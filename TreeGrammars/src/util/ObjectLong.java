package util;

public class ObjectLong<T> implements Comparable<ObjectLong<T>> {

	static int counter = 0;
	
	T object;
	private long longInteger;
	int id;
	
	public ObjectLong(T o, long i) {
		this.object = o;
		this.setLongInteger(i);
		id = counter++;
	}
	
	public T getObject() {
		return object;
	}
	
	public long getLongInteger() {
		return longInteger;
	}

	public void setLongInteger(long integer) {
		this.longInteger = integer;
	}

	public int compareTo(ObjectLong<T> o) {
		if (this.longInteger<o.longInteger) return -1;
		if (this.longInteger>o.longInteger) return 1;
		if (this.id<o.id) return -1;
		return 1;
	}

	
}
