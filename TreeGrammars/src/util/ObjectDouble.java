package util;

public class ObjectDouble<T> implements Comparable<ObjectDouble<T>> {

	static int counter = 0;
	
	T object;
	private double doubleValue;
	int id;
	
	public ObjectDouble(T o, double d) {
		this.object = o;
		this.setDoubleValue(d);
		id = counter++;
	}
	
	public T getObject() {
		return object;
	}
	
	public double getDoubleValue() {
		return doubleValue;
	}

	public void setDoubleValue(double d) {
		this.doubleValue = d;
	}
	
	public String toString() {
		return object.toString() + "\t" + doubleValue;
	}

	public int compareTo(ObjectDouble<T> o) {
		if (this.doubleValue<o.doubleValue) return -1;
		if (this.doubleValue>o.doubleValue) return 1;
		if (this.id<o.id) return -1;
		return 1;
	}

	
}
