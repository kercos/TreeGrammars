package util;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;

public class SortTable {

	public static <S,T> LinkedHashMap<T,S> getHighestValues(AbstractMap<T,S> table, double percentage) {				
		
		LinkedHashMap<T,S> result = new LinkedHashMap<T,S>();
		TreeMap<S, HashSet<T>> reversedTable = reverseAndSortTable(table);
		int size = (int)(table.size() * percentage);
		Iterator<Entry<S, HashSet<T>>> iter = reversedTable.descendingMap().entrySet().iterator();
		int count = 0;
		while(count!=size && iter.hasNext()) {
			Entry<S, HashSet<T>> e = iter.next();
			S key = e.getKey();
			for(T value : e.getValue()) {
				result.put(value,key);
				count++;
			}
		}
		
		return result;
	}
	
	public static <S,T> TreeMap<S, HashSet<T>> reverseAndSortTable(
			AbstractMap<T, S> table) {

		TreeMap<S, HashSet<T>> result = new TreeMap<S, HashSet<T>>();
		for (Entry<T, S> e : table.entrySet()) {
			T key = e.getKey();
			S value = e.getValue();
			HashSet<T> set = result.get(value);
			if (set == null) {
				set = new HashSet<T>();
				result.put(value, set);
			}
			set.add(key);
		}
		return result;
	}
	
	private static final char[] symbols;
	private static final Random random = new Random();
	
	static {
	    StringBuilder tmp = new StringBuilder();
	    for (char ch = 'a'; ch <= 'z'; ++ch)
	      tmp.append(ch);
	    symbols = tmp.toString().toCharArray();
	  } 
	
	public static String getRandomString(int size) {
		char[] buf = new char[size];
		for(int i=0; i<size; i++) {
			buf[i] = symbols[random.nextInt(symbols.length)];
		}		
	    return new String(buf);
	}
	
	public static <T, S> void printHashMap(AbstractMap<T, S> table) {
		for (Entry<T, S> e : table.entrySet()) {
			System.out.println(e.getKey() + "\t" + e.getValue());
		}
	}

	
	public static void main(String[] args) {
		HashMap<String,Float> table = new HashMap<String,Float>();
		for(int i=0; i<1000; i++) {
			Float f =  (float) Math.round(random.nextFloat() * 100) / 100;
			table.put(getRandomString(2), f);
		}
		HashMap<String, Float> topTable = getHighestValues(table, 0.1); // highest 10 percent
		printHashMap(topTable);
	}

}
