package util;
import java.util.*;

/**
 * This class handles Strings as Integers
 * @author fsangati
 *
 */
public class StringAtoms {
	
	public Hashtable<String, Integer> table;
	public int counter;
	
	public StringAtoms() {
		table = new Hashtable<String, Integer>();
		counter = 0;
	}
	
	public Integer addRetrive(String s) {
		Integer result = table.get(s);
		if (result==null) {
			result = counter++;
			table.put(s, result);
		}
		return result;
	}
}
