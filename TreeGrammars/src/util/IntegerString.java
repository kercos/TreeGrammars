package util;

public class IntegerString implements Comparable<IntegerString> {

	Integer integer;
	String string;
	
	
	public IntegerString(Integer i, String s) {
		this.string = s;
		this.integer = i;
	}


	@Override
	public int compareTo(IntegerString o) {
		int result = o.integer.compareTo(integer);
		if (result==0) {
			return o.string.compareTo(string);
		}
		return result;
	}


	public Integer getInteger() {
		return integer;
	}
	
	public String getString() {
		return string;
	}
	
	public String toString() {
		return string + " (" + integer + ")"; 
	}
	
	
}
