package util;

public class StringInteger implements Comparable<StringInteger> {

	String string;
	private int integer;
	
	public StringInteger(String s, int i) {
		this.string = s;
		this.integer = i;
	}
	
	public String string() {
		return string;
	}
	
	public int integer() {
		return integer;
	}

	public void setInteger(int integer) {
		this.integer = integer;
	}

	public int compareTo(StringInteger anotherStringInteger) {
		int thisVal = this.integer;
		int anotherVal = anotherStringInteger.integer;
		if (thisVal<anotherVal) return -1;
		if (thisVal>anotherVal) return 1;
		return this.string.compareTo(anotherStringInteger.string);
	}
	
}
