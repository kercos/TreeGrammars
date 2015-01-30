package util;

public class StringLong implements Comparable<StringLong> {

	String string;
	private long longInteger;
	
	public StringLong(String s, long l) {
		this.string = s;
		this.setLongInterger(l);
	}
	
	public String getString() {
		return string;
	}
	
	public long getLongInteger() {
		return longInteger;
	}
	
	public void setString(String s) {
		this.string = s;
	}

	public void setLongInterger(long longInteger) {
		this.longInteger = longInteger;
	}


	public int compareTo(StringLong o) {
		StringLong anotherStringInteger = (StringLong) o;
		long thisVal = this.longInteger;
		long anotherVal = anotherStringInteger.longInteger;
		if (thisVal<anotherVal) return -1;
		if (thisVal>anotherVal) return 1;
		return this.string.compareTo(anotherStringInteger.string);
	}
	
}
