package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import tsg.Label;
import tsg.TSNode;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import tsg.incremental.FragFringe;
import util.file.FileUtil;

public class Utility {

	public static NAirs Nair = new NAirs(10, false);
	public static NAirs NairAdj = new NAirs(10, true);
	public static Binomials Bino = new Binomials(10, false);
	public static Binomials BinoAdj = new Binomials(10, true);

	/**
	 * Returns a String with n tabs
	 */
	public static String fillTab(int n) {
		return fillChar(n, '\t');
	}

	/**
	 * Fill space beginning Returns a String of length n finishing with the
	 * input String s and starting with spaces
	 */
	public static String fsb(int n, String s) {
		return fillChar(n - s.length(), ' ') + s;
	}

	/**
	 * Fill space end Returns a String of length n beginning with the input
	 * String s and ending with spaces
	 */
	public static String fse(int n, String s) {
		return s + fillChar(n - s.length(), ' ');
	}

	/**
	 * @param n
	 *            totalLength
	 * @param i
	 *            number
	 * @return
	 */
	public static String padZero(int n, int i) {
		String s = Integer.toString(i);
		return fillChar(n - s.length(), '0') + s;
	}

	/**
	 * Returns a String of length n whose middle part is the input String s,
	 * surrounded by a repetition of the input char.
	 */
	public static String fca(int n, String s, char c) {
		int repBeginning = (n - s.length()) / 2;
		int repEnd = n - s.length() - repBeginning;
		return fillChar(repBeginning, c) + s + fillChar(repEnd, c);
	}

	/**
	 * Returns a String with n times c
	 */
	public static String fillChar(int n, char c) {
		StringBuilder result = new StringBuilder(n);
		for (int i = 0; i < n; i++)
			result.append(c);
		return result.toString();
	}

	public static <T> boolean increaseInTableInteger(
			Hashtable<T, Integer> table, T key, int toAdd) {
		Integer count = table.get(key);
		boolean newKey = (count == null);
		count = (newKey) ? toAdd : count + toAdd;
		table.put(key, count);
		return newKey;
	}

	public static <T> void increaseInTableInt(Hashtable<T, int[]> table, T key) {
		int[] count = table.get(key);
		if (count == null) {
			count = new int[] { 1 };
			table.put(key, count);
		} else
			count[0]++;
	}

	public static <T> void increaseInTableInt(HashMap<T, int[]> table, T key) {
		int[] count = table.get(key);
		if (count == null) {
			count = new int[] { 1 };
			table.put(key, count);
		} else
			count[0]++;
	}

	public static <T> void increaseInTableLong(Hashtable<T, long[]> table, T key) {
		long[] count = table.get(key);
		if (count == null) {
			count = new long[] { 1 };
			table.put(key, count);
		} else
			count[0]++;
	}

	public static <T> void increaseInTableInt(Hashtable<T, int[]> table, T key,
			int toAdd) {
		int[] count = table.get(key);
		if (count == null) {
			count = new int[] { toAdd };
			table.put(key, count);
		} else
			count[0] += toAdd;
	}

	public static <T> void increaseInTableDoubleArray(
			Hashtable<T, double[]> table, T key, double toAdd) {
		double[] count = table.get(key);
		if (count == null) {
			count = new double[] { toAdd };
			table.put(key, count);
		} else
			count[0] += toAdd;
	}

	public static <T> void increaseInTableDoubleLogArray(
			Hashtable<T, double[]> table, T key, double toAdd) {
		double[] count = table.get(key);
		if (count == null) {
			count = new double[] { toAdd };
			table.put(key, count);
		} else
			count[0] = logSum(count[0], toAdd);
	}

	public static <T> void increaseInTableBigDecimalArray(
			Hashtable<T, BigDecimal[]> table, T key, BigDecimal toAdd) {
		BigDecimal[] count = table.get(key);
		if (count == null) {
			count = new BigDecimal[] { toAdd };
			table.put(key, count);
		} else
			count[0] = count[0].add(toAdd);
	}

	public static <T> boolean increaseInTableDouble(Hashtable<T, Double> table,
			T key, double toAdd) {
		Double count = table.get(key);
		boolean newKey = (count == null);
		count = (newKey) ? toAdd : count + toAdd;
		table.put(key, count);
		return newKey;
	}

	public static <T> boolean increaseInTableLong(Hashtable<T, Long> table,
			T key, long toAdd) {
		Long count = table.get(key);
		boolean newKey = (count == null);
		count = (newKey) ? toAdd : count + toAdd;
		table.put(key, count);
		return newKey;
	}

	/**
	 * Remove in the table all the keys occuring <= limit times
	 * 
	 * @param <T>
	 * @param table
	 * @param key
	 * @param limit
	 * @return
	 */
	public static <T> void removeInTable(Hashtable<T, Integer> table, int limit) {
		for (Iterator<Integer> i = table.values().iterator(); i.hasNext();) {
			Integer freq = i.next();
			if (freq <= limit)
				i.remove();
		}
	}

	public static <T, S> void printHashTable(Hashtable<T, S> table) {
		for (T key : table.keySet()) {
			System.out.println(key + "\t" + table.get(key));
		}
	}

	public static <T> void printHashTableInt(Hashtable<T, int[]> table) {
		for (T key : table.keySet()) {
			System.out.println(key + "\t" + table.get(key)[0]);
		}
	}

	/**
	 * Given an Hashtable of type String-->Integer, the method adds a constant
	 * toAdd to the specific key in input.
	 */
	@SuppressWarnings("unchecked")
	public static boolean increaseStringInteger(Hashtable table, String key,
			int toAdd) {
		Integer count = (Integer) table.get(key);
		boolean newKey = (count == null);
		if (newKey)
			count = toAdd;
		else
			count = count + toAdd;
		table.put(key, count);
		return newKey;
	}

	public static int countTotalTypesInTable(
			Hashtable<? extends Object, Integer> table) {
		return table.keySet().size();
	}

	public static int countTotalTokensInTable(
			Hashtable<? extends Object, Integer> table) {
		int totalTokens = 0;
		for (Object key : table.keySet()) {
			totalTokens += table.get(key);
		}
		return totalTokens;
	}

	public static IdentityHashMap<Integer, String> reverseStringIntegerTable(
			Hashtable<String, Integer> table) {
		IdentityHashMap<Integer, String> result = new IdentityHashMap<Integer, String>();
		for (Map.Entry<String, Integer> tuple : table.entrySet()) {
			result.put(new Integer(tuple.getValue()), tuple.getKey());
		}
		return result;
	}

	public static <S extends Comparable<S>, T> TreeMap<S, HashSet<T>> reverseAndSortTable(
			HashMap<T, S> table) {

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
	
	public static  <S extends Comparable<S>, T> void printSortedKey(HashMap<T, S> table, boolean reverse, File outputFile) throws FileNotFoundException {
		TreeMap<S, HashSet<T>> reversedMap = reverseAndSortTable(table);		
		Iterator<Entry<S, HashSet<T>>> iter = reverse ?
				reversedMap.descendingMap().entrySet().iterator() :
				reversedMap.entrySet().iterator();				
		PrintWriter pw = new PrintWriter(outputFile);
		while(iter.hasNext()) {
			for (T t : iter.next().getValue()) {
				pw.println(t);
			}
		}
		pw.close();		
	}

	/**
	 * Given an Hashtable of type String-->int[], the method adds (toAdd) to the
	 * specific index of the value of the corresponding key in input.
	 */
	public static void increaseStringIntArray(Hashtable<String, int[]> table,
			int arraySize, String key, int index, int toAdd) {
		int[] stat = table.get(key);
		if (stat == null) {
			stat = new int[arraySize];
			table.put(key, stat);
		}
		stat[index]++;
	}

	public static void increaseStringDoubleArray(
			Hashtable<String, double[]> table, String key, double toAdd) {
		double[] stat = table.get(key);
		if (stat == null) {
			stat = new double[] { toAdd };
			table.put(key, stat);
			return;
		}
		stat[0] += toAdd;
	}

	public static String getMaxKey(Hashtable<String, double[]> table) {
		double max = -Double.MAX_VALUE;
		String maxKey = null;
		for (Entry<String, double[]> e : table.entrySet()) {
			double stat = e.getValue()[0];
			if (stat > max) {
				max = stat;
				maxKey = e.getKey();
			}
		}
		return maxKey;
	}

	public static String getMaxKeyInt(Hashtable<String, int[]> table) {
		int max = Integer.MIN_VALUE;
		String maxKey = null;
		for (Entry<String, int[]> e : table.entrySet()) {
			int stat = e.getValue()[0];
			if (stat > max) {
				max = stat;
				maxKey = e.getKey();
			}
		}
		return maxKey;
	}

	public static boolean increaseStringIntArray(
			Hashtable<String, int[]> table, String key) {
		int[] freq = table.get(key);
		if (freq == null) {
			freq = new int[] { 1 };
			table.put(key, freq);
			return true;
		} else
			freq[0]++;
		return false;
	}

	public static void increaseStringIntArray(Hashtable<String, int[]> table,
			String key, int toAdd) {
		int[] freq = table.get(key);
		if (freq == null) {
			freq = new int[] { toAdd };
			table.put(key, freq);
		} else
			freq[0] += toAdd;
	}

	/**
	 * Given an Hashtable of type String-->Long, the method adds a constant
	 * toAdd to the specific key in input.
	 */
	public static boolean increaseStringLong(Hashtable<String, Long> table,
			String key, long toAdd) {
		Long count = (Long) table.get(key);
		boolean newKey = (count == null);
		if (newKey)
			count = new Long(toAdd);
		else
			count = new Long(count.longValue() + toAdd);
		table.put(key, count);
		return newKey;
	}

	/**
	 * Given an Hashtable of type String-->Double, the method adds a constant
	 * toAdd to the specific key in input.
	 */
	public static boolean increaseStringDouble(Hashtable<String, Double> table,
			String key, double weight) {
		Double count = (Double) table.get(key);
		boolean newKey = (count == null);
		if (newKey)
			count = new Double(weight);
		else
			count = new Double(count.doubleValue() + weight);
		table.put(key, count);
		return newKey;
	}

	/**
	 * Given an Hashtable of type Object-->Integer, the method adds a constant
	 * toAdd to the specific key in input.
	 */
	public static boolean increaseIntegerListInteger(
			Hashtable<ArrayList<Integer>, Integer> table,
			ArrayList<Integer> key, int toAdd) {
		Integer count = table.get(key);
		boolean newKey = (count == null);
		if (newKey)
			count = new Integer(toAdd);
		else
			count = new Integer(count + toAdd);
		table.put(key, count);
		return newKey;
	}

	/**
	 * Given an Hashtable of type String-->Integer, the method subtracts a
	 * constant toAdd to the specific key in input. If the new counter is 0 or
	 * negative, the key will be removed from the hashtable.
	 */
	public static void decreaseStringInteger(Hashtable<String, Integer> table,
			String key, int weight) {
		int count = table.get(key);
		count = count - weight;
		if (count <= 0)
			table.remove(key);
		else
			table.put(key, count);
	}

	/**
	 * Given an Hashtable of type String-->Long, the method subtracts a constant
	 * toAdd to the specific key in input. If the new counter is 0 or negative,
	 * the key will be removed from the hashtable.
	 */
	public static void decreaseStringLong(Hashtable<String, Long> table,
			String key, long weight) {
		long count = table.get(key);
		count = count - weight;
		if (count <= 0)
			table.remove(key);
		else
			table.put(key, count);
	}

	/**
	 * Given an Hashtable of type String-->Double, the method subtracts a
	 * constant toAdd to the specific key in input. If the new counter is 0 or
	 * negative, the key will be removed from the hashtable.
	 */
	public static void decreaseStringDouble(Hashtable<String, Double> table,
			String key, double weight) {
		double count = (Double) table.get(key);
		count = count - weight;
		if (count <= 0)
			table.remove(key);
		else
			table.put(key, new Double(count));
	}

	/**
	 * Add the input TreeNode TN in the IdentityHashMap markTable with the
	 * corresponding mark
	 * 
	 * @param markTable
	 * @param TN
	 * @param mark
	 */
	public static void putTreeNodeMark(
			IdentityHashMap<TSNode, TreeSet<Integer>> markTable, TSNode TN,
			Integer mark) {
		TreeSet<Integer> markRecord = markTable.get(TN);
		if (markRecord == null) {
			markRecord = new TreeSet<Integer>();
			markTable.put(TN, markRecord);
		}
		markRecord.add(mark);
	}

	/**
	 * Given two Hashtable of type String-->Double, the method copies all the
	 * key-value pairs (where value is not null) from the originTable to the
	 * destinationTable
	 */
	public static void putStringDoubleFromToTable(String key[],
			Hashtable<String, Double> originTable,
			Hashtable<String, Double> destinationTable) {
		for (int i = 0; i < key.length; i++) {
			String tree = key[i];
			Double count = (Double) originTable.get(tree);
			if (count == null)
				continue;
			destinationTable.put(tree, count);
		}
	}

	/**
	 * Given two Hashtable of type String-->Integer, the method copies all the
	 * key-value pairs (where value is not null) from the originTable to the
	 * destinationTable
	 */
	public static void putStringIntegerFromToTable(String key[],
			Hashtable<String, Integer> originTable,
			Hashtable<String, Integer> destinationTable) {
		for (int i = 0; i < key.length; i++) {
			String tree = key[i];
			Integer count = (Integer) originTable.get(tree);
			if (count == null)
				continue;
			destinationTable.put(tree, count);
		}
	}

	public static void arrayIntPlus(int[] source, int[] destination) {
		for (int i = 0; i < destination.length; i++)
			destination[i] += source[i];
	}

	public static void arrayDoublePlus(double[] source, double[] destination) {
		for (int i = 0; i < destination.length; i++)
			destination[i] += source[i];
	}

	public static int[] arrayListIntegerToArray(List<Integer> list) {
		int[] result = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i);
		}
		return result;
	}

	public static void addToAll(int[] array, int toAdd) {
		for (int i = 0; i < array.length; i++) {
			array[i] += toAdd;
		}
	}

	/**
	 * Given two Hashtable of type String-->Double, the method adds the values
	 * of all the key-value pairs (where value is not null) from the originTable
	 * to the destinationTable
	 */
	public static void increaseAllStringDoubleFromToTable(
			Hashtable<String, Double> originTable,
			Hashtable<String, Double> destinationTable) {
		for (Enumeration<String> e = originTable.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Double toAdd = originTable.get(key);
			if (toAdd == null)
				continue;
			Double originalCount = destinationTable.get(key);
			if (originalCount == null)
				originalCount = toAdd;
			else
				originalCount = new Double(originalCount.doubleValue()
						+ toAdd.doubleValue());
			destinationTable.put(key, originalCount);
		}
	}

	/**
	 * Given two Hashtable of type String-->Double, the method decreses the
	 * values of all the key-value pairs (where value is not null) from the
	 * originTable to the destinationTable
	 */
	public static void decreaseAllStringDoubleFromToTable(
			Hashtable<String, Double> originTable,
			Hashtable<String, Double> destinationTable) {
		for (Enumeration<String> e = originTable.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Double toAdd = (Double) originTable.get(key);
			Double originalCount = (Double) destinationTable.get(key);
			originalCount = new Double(originalCount.doubleValue()
					- toAdd.doubleValue());
			if (originalCount.doubleValue() == 0)
				destinationTable.remove(key);
			else
				destinationTable.put(key, originalCount);
		}
	}

	/**
	 * Given an Hashtable origin of type String-->Double, the method returns an
	 * Hashtable of the same type containing the key,value mappings as in the
	 * origin table of the keys present in table1 and table2. If a certain key
	 * in table1 or table2 is not present in the origin table the key will be in
	 * the returned table with value equal to new Double(0).
	 */
	public static Hashtable<String, Double> keepInTable(
			Hashtable<String, Double> origin, Hashtable<String, Double> table1,
			Hashtable<String, Double> table2) {
		Hashtable<String, Double> result = new Hashtable<String, Double>();
		for (Enumeration<String> e = table1.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Double value = (Double) origin.get(key);
			if (value == null)
				value = new Double(0);
			result.put(key, value);
		}
		for (Enumeration<String> e = table2.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Double value = (Double) origin.get(key);
			if (value == null)
				value = new Double(0);
			result.put(key, value);
		}
		return result;
	}

	/**
	 * Given two Hashtable of type Object-->Integer, the method adds the values
	 * of all the key-value pairs from the partialRules to Rules. If any key is
	 * mapped to null value in parialRules the method treats it as if it is
	 * mapped to value Double(0).
	 */
	public static <T> void addAllInt(Hashtable<T, Integer> partialTable,
			Hashtable<T, Integer> table) {
		for (Enumeration<T> e = partialTable.keys(); e.hasMoreElements();) {
			T key = e.nextElement();
			Integer partialCount = (Integer) partialTable.get(key);
			Integer count = (Integer) table.get(key);
			if (count == null)
				count = new Integer(0);
			int newCount = count.intValue() + partialCount.intValue();
			table.put(key, new Integer(newCount));
		}
	}

	public static <T> void addAll(Hashtable<T, int[]> partialTable,
			Hashtable<T, int[]> table) {
		for (Enumeration<T> e = partialTable.keys(); e.hasMoreElements();) {
			T key = e.nextElement();
			int[] partialCount = partialTable.get(key);
			int[] count = table.get(key);
			if (count == null) {
				count = new int[] { partialCount[0] };
				table.put(key, count);
			} else {
				count[0] += partialCount[0];
			}
		}
	}

	/**
	 * Given two Hashtable of type Object-->Integer[2], the method adds the
	 * corresponding values of all the key-value pairs from the partialRules to
	 * Rules. The boolean removeUniqueIndex in input specifies if the key values
	 * in partialRules need to have the unique indexes cleaned before the adding
	 * procedure to take place.
	 */
	public static void addAllDuets(Hashtable<String, Integer[]> partialRules,
			Hashtable<String, Integer[]> Rules, boolean removeUniqueIndex) {
		for (Enumeration<String> e = partialRules.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Integer[] partialCount = partialRules.get(key);
			if (removeUniqueIndex)
				key = key.replaceAll("@\\d+", "");
			Integer[] count = (Integer[]) Rules.get(key);
			if (count == null)
				Rules.put(key, partialCount);
			else {
				count[0] = new Integer(count[0].intValue()
						+ partialCount[0].intValue());
				count[1] = new Integer(count[1].intValue()
						+ partialCount[1].intValue());
			}
		}
	}

	/**
	 * Print the time in standard output
	 */
	public static void printTime() {
		Date now = new Date();
		System.out.println(now);
	}
	
	static final SimpleDateFormat sdf = new SimpleDateFormat("YYMMdd_HH:mm:ss");
	
	public static String getDateTime() {
		Date now = new Date();		
		return sdf.format(now);
	}

	/**
	 * Returns true iff the array of String match contains a String which is
	 * equal to the String s.
	 */
	public static boolean stringMatches(String s, String[] match) {
		for (int i = 0; i < match.length; i++) {
			if (s.equals(match[i]))
				return true;
		}
		return false;
	}

	/**
	 * Returns a random integer between 0 and (max-1)
	 */
	public static int randomInteger(int max) {
		return (int) (Math.random() * max);
	}

	public static float randomNoise(float value, float maxNoise) {
		return (float) (Math.random() * 2 * maxNoise - maxNoise + value);

	}

	/**
	 * Returns a random boolean
	 */
	public static boolean randomBoolean() {
		return (Math.random() > .5) ? true : false;
	}

	public static void increaseAllOne(int[] a) {
		for (int i = 0; i < a.length; i++)
			a[i]++;
	}

	public static int[][][][] multiCombinations(int[] list) {
		int length = list.length;
		int[][] sizeChoices = combinations(list);
		int sizeChoicesLength = sizeChoices.length;
		int[][][][] result = new int[sizeChoicesLength][][][];

		for (int i = 0; i < sizeChoicesLength; i++) {
			int[] sizeChoice = sizeChoices[i];
			increaseOne(sizeChoice);
			int[] sizeBino = makeSizeBino(list, sizeChoice);
			int[][] partialResult = combinations(sizeBino);
			int partialResultLength = partialResult.length;
			result[i] = new int[partialResultLength][length][];
			for (int j = 0; j < partialResultLength; j++) {
				for (int l = 0; l < length; l++) {
					int nairIndex = partialResult[j][l];
					result[i][j][l] = Nair.get(list[l], sizeChoice[l])[nairIndex];
				}
			}
		}
		return result;
	}

	public static int[] makeSizeBino(int[] list, int[] sizeChoices) {
		int[] result = new int[list.length];
		for (int i = 0; i < list.length; i++) {
			result[i] = Bino.get(list[i], sizeChoices[i]);
		}
		return result;
	}

	public static void increaseOne(int[] list) {
		for (int i = 0; i < list.length; i++)
			list[i]++;
	}

	/**
	 * Given a one dimensional array of integer (i.e. {2,3,2}) the methods
	 * returns a twodimensional array of integers given by means of all
	 * combinations of indexes i.e. [[0, 0, 0], [0, 0, 1], [0, 1, 0], [0, 1, 1],
	 * [0, 2, 0], [0, 2, 1], [1, 0, 0], [1, 0, 1], [1, 1, 0], [1, 1, 1], [1, 2,
	 * 0], [1, 2, 1]]
	 */
	public static int[][] combinations(int[] list) {
		int combinations = product(list);
		int[][] result = new int[combinations][list.length];
		if (list.length == 1) {
			int row = 0;
			for (int i = 0; i < list[0]; i++) {
				result[row][0] = i;
				row++;
			}
			return result;
		}
		if (list.length == 2) {
			int row = 0;
			for (int i = 0; i < list[0]; i++) {
				for (int j = 0; j < list[1]; j++) {
					result[row][0] = i;
					result[row][1] = j;
					row++;
				}
			}
			return result;
		}
		int[] newList = new int[list.length - 1];
		for (int i = 0; i < list.length - 1; i++)
			newList[i] = list[i];
		int[][] partialResult = combinations(newList);
		int row = 0;
		for (int i = 0; i < partialResult.length; i++) {
			for (int k = 0; k < list[list.length - 1]; k++) {
				for (int j = 0; j < list.length - 1; j++) {
					result[row][j] = partialResult[i][j];
				}
				result[row][list.length - 1] = k;
				row++;
			}
		}
		return result;
	}
	
	/**
	 * Given two arrays return a double array with each element being a pair combination
	 * between elements of the respective input arrays.
	 * @param a
	 * @param b
	 * @return
	 */
	public static int[][] combinations(int[] a, int[] b) {
		int comb = a.length * b.length;
		int[][] result = new int[comb][];
		int i=0;
		for(int ea : a) {
			for(int eb : b) {
				result[i++] = new int[]{ea,eb};
			}
		}
		return result;
	}
	
	/**
	 * Given two double arrays return a triple array with each element being a pair combination
	 * between elements of the respective input arrays.
	 * @param a
	 * @param b
	 * @return
	 */
	public static int[][][] combinations(int[][] a, int[][] b) {
		int comb = a.length * b.length;
		int[][][] result = new int[comb][][];
		int i=0;
		for(int[] ea : a) {
			for(int[] eb : b) {
				result[i++] = new int[][]{ea,eb};
			}
		}
		return result;
	}

	/*
	 * 1 * 2 * ... * n-1 * n max n = 12
	 */
	public static int factorial(int n) {
		return factorial(1, n);
	}

	/*
	 * 1 * 2 * ... * n-1 * n max n = 20
	 */
	public static long factorialLong(long n) {
		return factorialLong(1, n);
	}

	/*
	 * s * s+1 * ... * e-1 * e max s,e = 12
	 */
	public static int factorial(int s, int e) {
		int result = s;
		for (int i = s + 1; i <= e; i++)
			result *= i;
		return result;
	}

	/*
	 * s * s+1 * ... * e-1 * e max s,e = 20
	 */
	public static long factorialLong(long s, long e) {
		long result = s;
		for (long i = s + 1; i <= e; i++)
			result *= i;
		return result;
	}

	/**
	 * All possible ways of choosing n elements from c.
	 * 
	 * @param c
	 * @param n
	 * @return
	 */
	public static int binomial(int c, int n) {
		if (n == 0 || n == c)
			return 1;
		if (n == 1 || n == c - 1)
			return c;
		/*
		 * if (c>17 || n>17) { System.err.println("Binomial max variable = 17");
		 * System.exit(-1); }
		 */
		if (n > c / 2)
			return factorial(n + 1, c) / factorial(c - n); // n = c - n
		return factorial(c - n + 1, c) / factorial(n);
	}

	/**
	 * All possible ways of choosing n consecutive elements from c.
	 * 
	 * @param c
	 * @param n
	 * @return
	 */
	public static int binomial_continuous(int c, int n) {
		return c - n + 1;
	}

	/**
	 * All possible ways of choosing n elements from c.
	 * 
	 * @param c
	 * @param n
	 * @return
	 */
	public static long binomialLong(long c, long n) {
		if (c == 0 || c == n)
			return 1;
		if (c == 1 || c == n - 1)
			return n;
		if (n > c / 2)
			return factorialLong(n + 1, c) / factorialLong(c - n); // n = c - n
		return factorialLong(c - n + 1, c) / factorialLong(n);
	}

	/**
	 * Returns all possible sets of n elements taken from c possible elements.
	 * i.e. n_air(4,2): 0 1, 0 2, 0 3, 1 2, 1 3, 2 3
	 * 
	 * @param c
	 * @param n
	 * @return max c,n = 17
	 */
	public static int[][] n_air(int c, int n) {
		int combinations = binomial(c, n);
		int[][] result = new int[combinations][n];
		if (n == 1) {
			int row = 0;
			for (int i = 0; i < c; i++) {
				result[row][0] = i;
				row++;
			}
			return result;
		}
		if (n == 2) {
			int row = 0;
			for (int i = 0; i < c; i++) {
				for (int j = i + 1; j < c; j++) {
					result[row][0] = i;
					result[row][1] = j;
					row++;
				}
			}
			return result;
		}
		int[][] partialResult = n_air(c - 1, n - 1);
		int row = 0;
		for (int i = 0; i < partialResult.length; i++) {
			int k = partialResult[i][n - 2] + 1;
			do {
				for (int j = 0; j < n - 1; j++) {
					result[row][j] = partialResult[i][j];
				}
				result[row][n - 1] = k++;
				row++;
			} while (k < c);
		}
		return result;
	}

	/**
	 * Returns all possible NON DISCOUNTINUOUS sublist of n elements taken from
	 * a list of c elements. i.e. n_air(4,2): 0 1, 1 2, 2 3
	 * 
	 * @param c
	 * @param n
	 * @return max c,n = 17
	 */
	public static int[][] n_air_continuous(int c, int n) {
		int combinations = c - n + 1;
		int[][] result = new int[combinations][n];
		for (int s = 0; s < combinations; s++) {
			for (int e = 0; e < n; e++) {
				result[s][e] = s + e;
			}
		}
		return result;
	}

	/**
	 * Returns all possible sets of 1, 2, ..., c, c elements taken from
	 * a set of c possible elements. i.e. n_air(4):
	 * [[0], [1], [2], [3]]
	 * [[0, 1], [0, 2], [0, 3], [1, 2], [1, 3], [2, 3]]
	 * [[0, 1, 2], [0, 1, 3], [0, 2, 3], [1, 2, 3]]
	 * [[0, 1, 2, 3]]
	 * 
	 * @param c
	 * @param c
	 * @return max n = 17
	 */
	public static int[][][] nair_multiple(int c) {
		int[][][] result = new int[c][][];
		for (int i = 1; i <= c; i++) {
			result[i-1] = n_air(c,i);
		}
		return result;
	}

	/**
	 * Given an array with ordered elements from 0 to c-1 it outputs an array
	 * with the missing elements from the set (0, 1, ..., c-1)
	 * @param subSet
	 * @param c
	 * @return
	 */
	public static int[] makeComplementary(int[] subSet, int c) {
		int[] result = new int[c - subSet.length];
		int findIndex = 0;
		int storedIndex = 0;
		for (int i : subSet) {
			while (findIndex != i) {
				result[storedIndex++] = findIndex++;
			}
			findIndex++;
		}
		while (findIndex != c) {
			result[storedIndex++] = findIndex++;
		}
		return result;
	}

	public static int countTotalElements(int[][] input) {
		int r = 0;
		for(int[] a : input) {
			r += a.length;
		}
		return r;
	}

	public static int[][][] convertArray4To3(int[][][][] input) {
		int l = 0;
		for(int[][][] a : input) {
			l += a.length;			
		}
		int[][][] result = new int[l][][];
		int i=0;
		for(int[][][] a : input) {
			for(int[][] b: a) {
				result[i++] = b;				
			}			
		}
		return result;
	}
	

	public static int[][] prepend(int[] subSet, int[][] completion) {
		int l = completion.length;
		int[][] result = new int[l+1][];
		result[0] = subSet;
		for(int i=1; i<=l; i++) {
			result[i] = completion[i-1];
		}
		return result;
	}

	

	
	public static int[][][][] translateIndexes(int[][][][] input, int[] indexes) {
		int l = input.length;
		int[][][][] result = new int[l][][][];
		for(int i=0; i<l; i++) {
			result[i] = translateIndexes(input[i], indexes);
		}
		return result;
	}

	public static int[][][] translateIndexes(int[][][] input, int[] indexes) {
		int l = input.length;
		int[][][] result = new int[l][][];
		for(int i=0; i<l; i++) {
			result[i] = translateIndexes(input[i], indexes);
		}
		return result;
	}

	public static int[][] translateIndexes(int[][] input, int[] indexes) {
		int l = input.length;
		int[][] result = new int[l][];
		for(int i=0; i<l; i++) {
			result[i] = translateIndexes(input[i], indexes);
		}
		return result;
	}

	public static int[] translateIndexes(int[] input, int[] indexes) {
		int l = input.length;
		int[] result = new int[l];
		for(int i=0; i<l; i++) {
			result[i] = indexes[input[i]];
		}
		return result;
	}

	/**
	 * Given an input array of int the method returns the product of the
	 * integers in it.
	 */
	public static int product(int[] array) {
		int result = array[0];
		for (int i = 1; i < array.length; i++) {
			result *= array[i];
		}
		return result;
	}

	public static int product(List<Integer> array) {
		int result = 1;
		for (int i : array) {
			result *= i;
		}
		return result;
	}

	public static long productLong(int[] array) {
		long result = array[0];
		for (int i = 1; i < array.length; i++) {
			result *= array[i];
		}
		return result;
	}

	public static double product(double[] array) {
		double result = array[0];
		for (int i = 1; i < array.length; i++) {
			result *= array[i];
		}
		return result;
	}

	/**
	 * Given an input array of long the method returns the product of the
	 * integers in it.
	 */
	public static long product(long[] array) {
		long result = array[0];
		for (int i = 1; i < array.length; i++) {
			result *= array[i];
		}
		return result;
	}

	/**
	 * Counting parenthesis in the input String
	 * 
	 * @return an integer i = 1* number_of_( - 1* number_of_)
	 */
	public static int countParenthesis(String line) {
		int result = 0;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '(')
				result++;
			else if (line.charAt(i) == ')')
				result--;
		}
		return result;
	}

	/**
	 * Returns a array of int containing a permutation of the first n integers
	 * (0,1,...,n-1).
	 */
	public static int[] permutation(int n) {
		int[] result = new int[n];
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < n; i++)
			list.add(new Integer(i));
		Collections.shuffle(list);
		for (int i = 0; i < n; i++)
			result[i] = list.get(i);
		return result;
	}

	/**
	 * Return the root of the tree raprsented by the input bracketted String.
	 */
	public static String get_unique_root(String eTree) {
		return eTree.substring(eTree.indexOf('(') + 1, eTree.indexOf(' '))
				.trim();
	}

	/**
	 * Return the lhs part (category) of the CFG rule reported in the input
	 * String 'CFGRule' (if space(s) are in between lhs and rhs).
	 */
	public static String getCFGcategory(String CFGRule) {
		return CFGRule.substring(0, CFGRule.indexOf(' '));
	}

	/**
	 * Two Hashtable of type String-->Double are in the input: 'CFGrules'
	 * containing CFG rules and relative counts and 'CFGcategories' containing
	 * lhs (categories) and relative counts. The method updates the counts of
	 * the 'CFGrules' normalizing each rule by the count of the relative
	 * category contained in 'CFGcategories'.
	 */
	public static void normalizenCFRulesOnCategory(
			Hashtable<String, Double> CFGrules,
			Hashtable<String, Double> CFGcategories) {
		for (Enumeration<String> e = CFGrules.keys(); e.hasMoreElements();) {
			String rule = (String) e.nextElement();
			String category = Utility.getCFGcategory(rule);
			double count_category = ((Double) CFGcategories.get(category))
					.doubleValue();
			double weight = ((Double) CFGrules.get(rule)).doubleValue()
					/ count_category;
			CFGrules.put(rule, new Double(weight));
		}
	}

	/**
	 * Given a LinkedList of CFG rules in input (rules), the method updateds two
	 * Hashtable of type String-->Double 'CFGRules' and 'CFGcategories'
	 * ('CFGcategories' can be null and in this case will not be updated). Every
	 * rule in 'rules' will be added in 'CFGRules', and its category (lhs) in
	 * 'CFGcategories' with value 'count'. The boolean in input 'prelexical' and
	 * 'lexical' determine whether the non-lexical and lexical rules are allowed
	 * (if we set both variables to true all rules will be considered).
	 */
	public static void addAllCFGRules(LinkedList<String> rules, double count,
			Hashtable<String, Double> CFGRules,
			Hashtable<String, Double> CFGcategories, boolean prelexical,
			boolean lexical) {
		for (ListIterator<String> j = rules.listIterator(); j.hasNext();) {
			String rule = j.next();
			boolean isLex = (rule.indexOf('"') != -1);
			if (isLex && !lexical)
				continue;
			if (!isLex && !prelexical)
				continue;
			if (CFGcategories != null) {
				String category = getCFGcategory(rule);
				increaseStringDouble(CFGcategories, category, count);
			}
			increaseStringDouble(CFGRules, rule, count);
		}
	}

	/**
	 * Returns a double being the contribution of each stochastic variable
	 * instance 'weight' to the determine its global entropy.
	 */
	public static double pLogp(double weight) {
		if (weight == 0)
			return 0;
		return weight * Math.log(weight);
	}

	/**
	 * Given an Object 'o' which has to be either an Integer or a Double, the
	 * method returns the double value of 'o'.
	 */
	public static double getDouble(Object o) {
		if (o.getClass().isInstance(new Double(0)))
			return ((Double) o).doubleValue();
		return ((Integer) o).doubleValue();
	}

	/**
	 * Convert \' --> ', \$ --> $,
	 * 
	 * @param s
	 *            input string
	 * @return the converted string
	 */
	public static String cleanSlash(String s) {
		char slash = '\\';
		int index = s.lastIndexOf(slash);
		if (index == -1)
			return s;
		String result;
		char symbol = s.charAt(index + 1);
		if (symbol == '$' || symbol == '\'' || symbol == '#' || symbol == '=') {
			if (index == 0)
				result = s.substring(1);
			else
				result = s.substring(0, index) + s.substring(index + 1);
			if (symbol == '/')
				return result;
		} else
			return s;
		return cleanSlash(result);
	}

	public static String replaceDoubleSlash(String s) {
		int index = s.indexOf("\\\\");
		if (index == -1)
			return s;
		return s.substring(0, index) + "\\" + s.substring(index + 2);
	}

	/**
	 * Given a LinkedList of objects 'inputSet' and a double 'percentage' (0,1)
	 * the method returns a new LinkedList containing a random subset of element
	 * of the 'inputSet' being a portion of the entire set equivalent to the
	 * input percentage.
	 */
	public static LinkedList<Object> extractRandomPercentage(
			LinkedList<Object> inputSet, double percentage) {
		LinkedList<Object> result = new LinkedList<Object>();
		int input_size = inputSet.size();
		int target_size = (int) (input_size * percentage);
		int actual_size = 0;
		while (actual_size < target_size) {
			int index = Utility.randomInteger(input_size);
			Object o = inputSet.remove(index);
			result.add(o);
			input_size--;
			actual_size++;
		}
		return result;
	}
	
	static DecimalFormat df = new DecimalFormat("0.0");
	public static String percentage(int num, int den) {
		return df.format(((double)num)*100/den) + "%";
	}

	/**
	 * Given a input TreeNode 'TN' representing a one-lexicalized etree, the
	 * method returns an array of two String representing two part of 'TN' being
	 * braken at depth 3, when a recursive structure is being found at the top
	 * (precisely when the TOP nod has a daughter labeled 'S' and when this 'S'
	 * has itself an other doughter labeled 'S'.) The method returns null if no
	 * such recursive feature is being detected.
	 */
	/*
	 * public static String[] cutTop(TreeNode TN) { if (TN.label.equals("TOP")
	 * && TN.daughters[0].label.equals("S")) { TreeNode TN_S = TN.daughters[0];
	 * for(int j=0; j<TN_S.daughters.length; j++) { TreeNode D =
	 * TN_S.daughters[j]; if (D.daughters != null && D.label.equals("S")) {
	 * String[] result = new String[2]; TreeNode[] daughters = D.daughters;
	 * D.daughters = null; result[0] = TN.toString(); D.daughters = daughters;
	 * TN = D; result[1] = TN.toString(); return result; } } } return null; }
	 */

	public static int indexOf(Object o, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == o)
				return i;
		}
		return -1;
	}

	public static int indexOf(int[] o, int[][] array) {
		for (int i = 0; i < array.length; i++) {
			if (Arrays.equals(array[i], o))
				return i;
		}
		return -1;
	}
	
	/**
	 * n-th index of c in s
	 * @param s
	 * @param c
	 * @param n
	 * @return
	 */
	public static int indexOf(String s, char ch, int n) {
		int index = -1;
		for (int i = 0; i < n; i++) {
			index = s.indexOf(ch, index+1);
		}
		return index;
	}

	public static int[] intArrayConversion(List<Integer> list) {
		int[] result = new int[list.size()];
		for (ListIterator<Integer> i = list.listIterator(); i.hasNext();) {
			int interger = i.next();
			result[i.previousIndex()] = interger;
		}
		return result;
	}

	/**
	 * Returns a new array of int with `value` inserted at the correct position
	 * (natural order) of the incoming array. Check for the present of value in
	 * the array. If it's present return the same array as the one in input
	 * without modification.
	 * 
	 * @param array
	 *            input ordered array
	 * @param value
	 * @return
	 */
	public static int[] appendIntArraySet(int[] array, int value) {
		if (array == null)
			return new int[] { value };
		int insertionPoint = Arrays.binarySearch(array, value);
		if (insertionPoint >= 0)
			return array;
		insertionPoint = -insertionPoint - 1;
		int[] newArray = new int[array.length + 1];
		int index = 0;
		for (; index < insertionPoint; index++)
			newArray[index] = array[index];
		newArray[index++] = value;
		for (; index < newArray.length; index++)
			newArray[index] = array[index - 1];
		return newArray;
	}

	public static boolean isPunctuation(String word) {
		return word.matches("[.,:;'`?!()]+");
	}

	public static boolean matchString(String key, String[] array) {
		Arrays.sort(array);
		return (Arrays.binarySearch(array, key) >= 0);
	}

	public static String removeDoubleQuotes(String word) {
		return word.replaceAll("\"", "");
	}

	public static String booleanToOnOff(boolean value) {
		return (value) ? "on" : "off";
	}

	public static void removeOneToIntArray(int[] array) {
		for (int i = 0; i < array.length; i++)
			array[i]--;
	}

	public static boolean containsFalse(boolean[] array) {
		for (boolean b : array) {
			if (!b)
				return true;
		}
		return false;
	}

	public static String hashtableStringIntegerToString(
			Hashtable<String, Integer> table) {
		String result = "";
		String[] keySet = table.keySet().toArray(new String[] {});
		Arrays.sort(keySet);
		for (String s : keySet) {
			result += s + "\t" + table.get(s) + "\n";
		}
		return result;
	}

	public static void printListStandardOutput(List<? extends Object> list) {
		for (Object o : list) {
			System.out.println(o.toString());
		}
	}

	public static void hashtableOrderedToFile(Hashtable<String, Integer> table,
			File outputFile) {
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		IdentityHashMap<Integer, String> reversedTable;
		reversedTable = Utility.reverseStringIntegerTable(table);
		Integer[] countSorted = reversedTable.keySet()
				.toArray(new Integer[] {});
		Arrays.sort(countSorted);
		for (int i = countSorted.length - 1; i >= 0; i--) {
			Integer count = countSorted[i];
			String pair = reversedTable.get(count);
			out.println(pair + "\t" + count);
		}
		out.println();
		out.close();
	}

	public static void hashtableRankedToFile(Hashtable<String, Integer> table,
			File outputFile) {
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		out.println("Rank\tFreq\tTokens\tTotalFreq");
		IdentityHashMap<Integer, String> reversedTable;
		reversedTable = Utility.reverseStringIntegerTable(table);
		Integer[] countSorted = reversedTable.keySet()
				.toArray(new Integer[] {});
		Arrays.sort(countSorted);
		int rank = 1;
		int rankFreq = countSorted[countSorted.length - 1], rankTokens = 0;
		for (int i = countSorted.length - 1; i >= 0; i--) {
			Integer count = countSorted[i];
			// String value = reversedTable.get(count);
			if (count.intValue() != rankFreq) {
				out.println(rank + "\t" + rankFreq + "\t" + rankTokens + "\t"
						+ rankFreq * rankTokens);
				rank++;
				rankFreq = count;
				rankTokens = 0;
			}
			rankTokens++;
		}
		out.println(rank + "\t" + rankFreq + "\t" + rankTokens + "\t"
				+ rankFreq * rankTokens);
		out.println();
		out.close();
	}

	public static int[] countIntegerListClasses(List<Integer> list, int binSize) {
		int max = maxIntegerList(list);
		int binNumber = max / binSize + 1;
		int[] result = new int[binNumber];
		for (Integer i : list) {
			int bin = i / binSize;
			result[bin]++;
		}
		return result;
	}

	public static String printIntegerListClasses(List<Integer> list, int binSize) {
		int[] classes = countIntegerListClasses(list, binSize);
		String result = "";
		for (int i = 0; i < classes.length; i++) {
			int lowerBound = i * binSize;
			int upperBound = (i + 1) * binSize - 1;
			result += lowerBound + "-" + upperBound + "\t" + classes[i] + "\n";
		}
		return result;
	}

	public static int maxIntegerList(List<Integer> list) {
		int max = Integer.MIN_VALUE;
		for (Integer i : list) {
			if (i > max)
				max = i;
		}
		return max;
	}

	public static int maxIndex(int[] list) {
		int maxIndex = 0;
		int max = list[0];
		for (int i = 1; i < list.length; i++) {
			if (list[i] > max) {
				maxIndex = i;
				max = list[i];
			}
		}
		return maxIndex;
	}

	public static int max(int[] list) {
		int max = list[0];
		for (int i = 1; i < list.length; i++) {
			if (list[i] > max)
				max = list[i];
		}
		return max;
	}
	
	public static int maxIndex(double[] list) {
		int maxIndex = 0;
		double max = list[0];
		for (int i = 1; i < list.length; i++) {
			if (list[i] > max) {
				maxIndex = i;
				max = list[i];
			}
		}
		return maxIndex;
	}

	public static double max(double[] list) {
		double max = list[0];
		for (int i = 1; i < list.length; i++) {
			if (list[i] > max)
				max = list[i];
		}
		return max;
	}

	public static double max(double[] list, int[] index) {
		double max = list[0];
		for (int i = 1; i < list.length; i++) {
			if (list[i] > max) {
				max = list[i];
				index[0] = i;
			}
		}
		return max;
	}

	public static int min(int[] list) {
		int min = list[0];
		for (int i = 1; i < list.length; i++) {
			if (list[i] < min)
				min = list[i];
		}
		return min;
	}

	public static void fillDoubleIntArray(int[][] array, int value) {
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				array[i][j] = value;
			}
		}
	}

	public static void fillDoubleFloatArray(float[][] array, float value) {
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				array[i][j] = value;
			}
		}
	}

	public static void printChart(int[][] array, String[] columnHeader, String[] rowHeader) {
		String result = "";
		for (int i = 0; i < columnHeader.length; i++)
			result += "\t" + columnHeader[i];
		result += "\n";
		for (int i = 0; i < array.length; i++) {
			result += rowHeader[i];
			for (int j = 0; j < array[i].length; j++) {
				result += "\t" + array[i][j];
			}
			result += "\n";
		}
		System.out.println(result);
	}
	
	public static void printChart(int[][] array) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				sb.append("\t" + array[i][j]);
			}
			sb.append("\n");
		}
		System.out.println(sb.toString());
	}
	
	public static void printChart(byte[][] array, String[] columnHeader, String[] rowHeader) {
		String result = "";
		for (int i = 0; i < columnHeader.length; i++)
			result += "\t" + columnHeader[i];
		result += "\n";
		for (int i = 0; i < array.length; i++) {
			result += rowHeader[i];
			for (int j = 0; j < array[i].length; j++) {
				result += "\t" + array[i][j];
			}
			result += "\n";
		}
		System.out.println(result);		
	}
	
	public static void printChart(boolean[][] array, String[] columnHeader, String[] rowHeader) {
		String result = "";
		for (int i = 0; i < columnHeader.length; i++)
			result += "\t" + columnHeader[i];
		result += "\n";
		for (int i = 0; i < array.length; i++) {
			result += rowHeader[i];
			for (int j = 0; j < array[i].length; j++) {
				result += "\t" + (array[i][j] ? "1" : "");
			}
			result += "\n";
		}
		System.out.println(result);		
	}

	public static void printFloatChart(float[][] array, String[] columnHeader,
			String[] rowHeader) {
		String result = "";
		for (int i = 0; i < columnHeader.length; i++)
			result += "\t" + columnHeader[i];
		result += "\n";
		for (int i = 0; i < array.length; i++) {
			result += rowHeader[i];
			for (int j = 0; j < array[i].length; j++) {
				result += "\t" + array[i][j];
			}
			result += "\n";
		}
		System.out.println(result);
	}

	public static void printFloatArray(float[][] array) {
		String result = "";
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				result += "\t" + array[i][j];
			}
			result += "\n";
		}
		System.out.println(result);
	}

	public static void printIntArray(int[][] array) {
		if (array == null) {
			System.out.println("null");
			return;
		}
		String result = "";
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				result += "\t" + array[i][j];
			}
			result += "\n";
		}
		System.out.println(result);
	}

	public static void printIntArray(int[][][][] array) {
		if (array == null) {
			System.out.println("null");
			return;
		}
		String result = "<\n";
		for (int i = 0; i < array.length; i++) {
			result += "\t< ";
			for (int j = 0; j < array[i].length; j++) {
				result += "<";
				for (int h = 0; h < array[i][j].length; h++) {
					result += "<";
					for (int k = 0; k < array[i][j][h].length; k++) {
						result += array[i][j][h][k];
						if (k != array[i][j][h].length - 1)
							result += ",";
					}
					result += (h != array[i][j].length - 1) ? ">," : ">";
				}
				result += (j != array[i].length - 1) ? ">, " : ">";
			}
			result += " >\n";
		}
		result += ">\n";
		System.out.println(result);
	}

	/**
	 * Computes the possible ways in which it's possible to split the integer n
	 * in two (non zero) parts. i.e. split(6): 1 5, 2 4, 3 3, 4 2, 5 1,
	 * 
	 * @param n
	 * @return
	 */
	public static int[][] split(int n) {
		int[][] result = new int[n - 1][2];
		int row = 0;
		for (int i = 1; i < n; i++) {
			result[row][0] = i;
			result[row][1] = n - i;
			row++;
		}
		return result;
	}

	public static int[] parseIndexList(String[] words) {
		int[] result = new int[words.length];
		for (int i = 0; i < words.length; i++) {
			result[i] = Integer.parseInt(words[i]);
		}
		return result;
	}

	public static void tartaglia() {
		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			for (int j = 1; j <= i; j++) {
				System.out.print(Utility.binomial(i, j) + "\t");
			}
			System.out.println();
		}
	}

	/**
	 * Returns true if a is greater than b in at least one of its components.
	 * and greater or equal in all the others a = 1,4,7,8 b = 1,3,7,8 true a =
	 * 1,4,7,8 b = 1,4,7,8 false a = 1,5,8,9 b = 2,4,7,8 false
	 * 
	 * @param a
	 *            ascendly ordered list of integer
	 * @param b
	 *            ascendly ordered list of integer
	 * @return
	 */
	public static boolean greaterThan(int[] a, int[] b) {
		int length = a.length;
		boolean foundGreater = false;
		for (int i = length - 1; i > -1; i--) {
			if (a[i] < b[i])
				return false;
			if (a[i] > b[i])
				foundGreater = true;
		}
		return foundGreater;
	}

	public static boolean allZero(int[] a) {
		for (int ai : a) {
			if (ai != 0)
				return false;
		}
		return true;
	}

	public static boolean allZero(float[] a) {
		for (float ai : a) {
			if (ai != 0f)
				return false;
		}
		return true;
	}

	public static boolean allZero(double[] a) {
		for (double ai : a) {
			if (ai != 0)
				return false;
		}
		return true;
	}

	public static boolean greaterThan(float[] a, float[] b) {
		int length = a.length;
		boolean foundGreater = false;
		for (int i = length - 1; i > -1; i--) {
			if (a[i] < b[i])
				return false;
			if (a[i] > b[i])
				foundGreater = true;
		}
		return foundGreater;
	}

	public static boolean greaterThan(double[] a, double[] b) {
		int length = a.length;
		boolean foundGreater = false;
		for (int i = 0; i < length; i++) {
			if (a[i] < b[i])
				return false;
			if (a[i] > b[i])
				foundGreater = true;
		}
		return foundGreater;
	}

	public static boolean greaterThanPriority(double[] a, double[] b) {
		int length = a.length;
		for (int i = 0; i < length; i++) {
			if (a[i] < b[i])
				return false;
			if (a[i] > b[i])
				return true;
		}
		return false;
	}

	public static int[] concat(int[] a, int[] b) {
		int[] result = new int[a.length + b.length];
		int i = 0;
		for (int ai : a) {
			result[i] = ai;
			i++;
		}
		for (int bi : b) {
			result[i] = bi;
			i++;
		}
		return result;
	}

	public static int[] concat(int a, int[] b) {
		int[] result = new int[b.length + 1];
		result[0] = a;
		int i = 1;
		for (int bi : b) {
			result[i] = bi;
			i++;
		}
		return result;
	}

	public static int[] concat(int[] a, int b) {
		int[] result = new int[a.length + 1];
		int i = 0;
		for (int ai : a) {
			result[i] = ai;
			i++;
		}
		result[i] = b;
		return result;
	}

	public static double getCondProb(Hashtable<String, Integer> freqTable,
			Hashtable<String, Integer> condFreqTable, String key, String condKey) {
		Integer keyFreq = freqTable.get(key);
		if (keyFreq == null)
			return 0;
		Integer keyCondFreq = condFreqTable.get(condKey);
		return (double) keyFreq / keyCondFreq;
	}

	public static String joinStringArrayToString(String[] array,
			String separator) {
		if (array.length == 0)
			return "";
		StringBuilder result = new StringBuilder(array[0]);
		for (int i = 1; i < array.length; i++) {
			result.append(separator).append(array[i]);
		}
		return result.toString();
	}

	public static String joinIntArrayToString(int[] array, String separator) {
		String result = "";
		for (int i = 0; i < array.length; i++) {
			result += array[i];
			if (i != array.length - 1)
				result += separator;
		}
		return result;
	}

	public static String[] splitOnTabs(String a) {
		return a.split("\t");
	}

	public static String[] splitOnNewLine(String a) {
		return a.split("\n");
	}

	public static boolean isInInterval(int index, int[] c) {
		return (index >= c[0] && index <= c[1]);
	}

	public static boolean isInInterval(int[] c1, int[] c2) {
		return (c1[0] >= c2[0] && c1[1] <= c2[1]);
	}

	public static boolean isIntervalExtreme(int index, int[] c) {
		return (index == c[0] || index == c[1]);
	}

	public static boolean isIntervalExtreme(int[] c1, int[] c2) {
		return (c1[0] == c2[0] || c1[1] == c2[1]);
	}

	public static String[] concat(String[] as1, String[] as2) {
		String[] result = new String[as1.length + as2.length];
		int i = 0;
		for (String s : as1) {
			result[i] = s;
			i++;
		}
		for (String s : as2) {
			result[i] = s;
			i++;
		}
		return result;
	}

	public static boolean isIntegerList(String[] list) {
		Pattern p = Pattern.compile("-{0,1}\\d+");
		for (String s : list) {
			if (!p.matcher(s).matches())
				return false;
		}
		return true;
	}

	public static boolean isInteger(String s) {
		Pattern p = Pattern.compile("-{0,1}\\d+");
		return p.matcher(s).matches();
	}

	public static boolean xor(boolean b1, boolean b2) {
		return ((b1 || b2) && !(b1 && b2));
	}

	public static int sum(int[] array) {
		int result = 0;
		for (int i : array)
			result += i;
		return result;
	}

	public static double sum(double[] array) {
		double result = 0;
		for (double i : array)
			result += i;
		return result;
	}

	public static int sum(Collection<Integer> list) {
		int result = 0;
		for (int i : list)
			result += i;
		return result;
	}

	public static long times(List<Integer> list) {
		long result = 1;
		for (int i : list)
			result *= i;
		return result;
	}

	public static boolean iff(boolean a, boolean b) {
		return ((a && b) || (!a && !b));
	}

	/**
	 * Remove each elements in a if there is an element in b equals to it and
	 * not previously associated with a removed element in a.
	 * 
	 * @param c
	 * @return
	 */
	public static <T> boolean removeAllOnce(ArrayList<T> a, ArrayList<T> b) {
		boolean modified = false;
		BitSet removedElementBIndex = new BitSet();
		ListIterator<T> aIter = a.listIterator();
		while (aIter.hasNext()) {
			T aNext = aIter.next();
			boolean found = false;
			ListIterator<T> bIter = b.listIterator();
			int index = 0;
			while (bIter.hasNext()) {
				T bNext = bIter.next();
				if (!removedElementBIndex.get(index) && aNext.equals(bNext)) {
					removedElementBIndex.set(index);
					found = true;
					break;
				}
				index++;
			}
			if (found) {
				aIter.remove();
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * Addition in the log domain. Returns an approximation to ln(e^a + e^b).
	 * Just doing it naively might lead to underflow if a and b are very
	 * negative. Without loss of generality, let b<a . If a>-10, calculates it
	 * in the standard way. Otherwise, rewrite it as a + ln(1 + e^(b-a)) and
	 * approximate that by the first-order Taylor expansion to be a + (e^(b-a)).
	 * So if b is much smaller than a, there will still be underflow in the last
	 * term, but in that case, the error is small relative to the final answer.
	 */
	public static double logSum2(double a, double b) {
		if (a > b) {
			if (b == Double.NEGATIVE_INFINITY) {
				return a;
			} else if (a > -10) {
				return Math.log(Math.exp(a) + Math.exp(b));
			}

			else {
				return a + Math.exp(b - a);
			}
		} else {
			if (a == Double.NEGATIVE_INFINITY) {
				return b;
			} else if (b > -10) {
				return Math.log(Math.exp(a) + Math.exp(b));
			} else {
				return b + Math.exp(a - b);
			}
		}
	}

	/**
	 * From Markos
	 * (http://en.wikipedia.org/wiki/List_of_logarithmic_identities#Summation
	 * .2Fsubtraction) returns log(e^x + e^y) (log = natural logarithm)
	 */
	public static double logSum(double x, double y) {
		if (y == 0 || x == 0) {
			return logSumLongWay(x, y);
		}

		if (x < y) {
			double diff = x - y; // diff alway <= 0
			return y + Math.log1p(Math.exp(diff)); // Math.log(1. +
													// Math.exp(diff))
		}

		double diff = y - x; // diff alway <= 0
		return x + Math.log1p(Math.exp(diff)); // Math.log(1. + Math.exp(diff))
	}

	/**
	 * min should not be part of array
	 * 
	 * @param logArray
	 * @param logMin
	 * @return
	 */
	public static double logSum(double[] logArray, double logMax) {
		if (logArray.length == 0)
			return logMax;
		double sum = 0;
		for (double logA : logArray) {
			sum += Math.exp(logA - logMax);
		}
		return logMax + Math.log1p(sum);
	}

	public static double logSum(double[] logArray) {
		double[] maxArray = new double[1];
		int maxIndex = Utility.getMax(logArray, maxArray);
		double logMax = maxArray[0];
		double sum = 0;
		for (int i = 0; i < logArray.length; i++) {
			if (i == maxIndex)
				continue;
			sum += Math.exp(logArray[i] - logMax);
		}
		return logMax + Math.log1p(sum);
	}

	private static int getMax(double[] array, double[] maxArray) {
		int maxIndex = 0;
		double max = array[0];
		for (int i = 1; i < array.length; i++) {
			double next = array[i];
			if (next > max) {
				max = next;
				maxIndex = i;
			}
		}
		maxArray[0] = max;
		return maxIndex;
	}

	public static <T> double logSumHashMap(HashMap<T, double[]> table, int index) {
		Iterator<double[]> iter = table.values().iterator();
		double max = iter.next()[0];
		int size = table.size() - 1;
		double[] others = new double[size];
		int i = 0;
		while (iter.hasNext()) {
			double next = iter.next()[index];
			if (next > max) {
				others[i] = max;
				max = next;
			} else
				others[i] = next;
			i++;
		}
		return Utility.logSum(others, max);
	}

	public static <T> boolean increaseInHashMapLogSum(
			HashMap<T, double[]> table, T key, double d) {
		double[] value = table.get(key);
		if (value == null) {
			value = new double[] { d };
			table.put(key, value);
			return true;
		}
		value[0] = Utility.logSum(value[0], d);
		return false;
	}

	public static double logSumLongWay(double x, double y) {
		/*
		 * if (y==0 || x==0) { System.err.println("logSum with zero"); return
		 * -1; }
		 */

		double a = Math.exp(x);
		double b = Math.exp(y);
		return Math.log(a + b);

	}

	public static void regExTest() {
		String a = "  the `/Christian view they *argue |";
		a = a.replaceAll("[^\\w\\s]", "");
		System.out.println(a);
	}

	public static void testLog() {
		// for(int i=0; i<10; i++) {
		double a = Math.random();
		double b = 0;
		// double b = Math.random() * 0.1;
		// double b = a;
		// double a = 1;
		// double b = 1;

		double lnA = Math.log(a);
		double lnB = Math.log(b);

		double correct = Math.log(a + b);

		double logSumLongWay = logSumLongWay(lnA, lnB);
		double logSumLongWayError = Math.abs(correct - logSumLongWay);

		double logMarkos = logSum(lnA, lnB);
		double logMarkosError = Math.abs(correct - logMarkos);

		double logOtherVersion = logSum2(lnA, lnB);
		double logOtherVersionError = Math.abs(correct - logOtherVersion);

		System.out.println(lnA + " " + lnB + "\n\tLong Way Error:"
				+ logSumLongWayError + "\t" + correct + "\n\tWiki Way Error:"
				+ logMarkosError + "\n\tAlternative way Error:"
				+ logOtherVersionError + "\n");
		// }
	}

	public static void testLogSum() {

		int size = 200000; // min exluded
		double[] prob = new double[size];
		double[] lnProb = new double[size];
		double max = Math.random();
		for (int i = 0; i < size; i++) {
			double next = Math.random();
			if (next > max) {
				prob[i] = max;
				lnProb[i] = Math.log(max);
			} else {
				prob[i] = next;
				lnProb[i] = Math.log(next);
			}
		}

		double lnMax = Math.log(max);

		double correct = Math.log(Utility.sum(prob) + max);

		long now = System.currentTimeMillis();
		double logSumLongWay = logSumLongWay(lnMax, lnProb[0]);
		for (int i = 1; i < size; i++) {
			logSumLongWay = logSumLongWay(logSumLongWay, lnProb[i]);
		}
		long logSumLongWayTime = System.currentTimeMillis() - now;

		now = System.currentTimeMillis();
		double logSumApproxLongWay = logSum(lnMax, lnProb[0]);
		for (int i = 1; i < size; i++) {
			logSumApproxLongWay = logSumLongWay(logSumApproxLongWay, lnProb[i]);
		}
		long logSumApproxLongWayTime = System.currentTimeMillis() - now;

		now = System.currentTimeMillis();
		double maxAgain = lnProb[0];
		for (int i = 1; i < size; i++) {
			double next = lnProb[i];
			if (next > maxAgain)
				maxAgain = next;
		}
		if (max > maxAgain)
			maxAgain = max;

		assert maxAgain == max;

		double logApprox = logSum(lnProb, lnMax);
		long logApproxTime = System.currentTimeMillis() - now;

		now = System.currentTimeMillis();
		double[] lnProbWithMax = Arrays.copyOf(lnProb, size + 1);
		lnProbWithMax[size] = lnMax;
		double logApproxArray = logSum(lnProbWithMax);
		long logApproxArrayTime = System.currentTimeMillis() - now;

		System.out.println("Correct :        " + correct);
		System.out.println("Long way:        " + logSumLongWay + "\t"
				+ logSumLongWayTime);
		System.out.println("Long way approx: " + logSumApproxLongWay + "\t"
				+ logSumApproxLongWayTime);
		System.out.println("Approx  :        " + logApprox + "\t"
				+ logApproxTime);
		System.out.println("Approx array :   " + logApproxArray + "\t"
				+ logApproxArrayTime);
	}

	public static void convertFreqTypesTokens() throws FileNotFoundException {
		File inputFile = new File(
				"/Users/fedja/Desktop/danigraph/daniData2.txt");
		File outputFile = new File(
				"/Users/fedja/Desktop/danigraph/daniData2_conv.txt");
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			int binSize = Integer.parseInt(lineSplit[0]);
			int freq = Integer.parseInt(lineSplit[1]);
			for (int i = 0; i < binSize; i++) {
				pw.println(freq);
			}
		}
		pw.close();
	}

	public static void getFreqCount() throws FileNotFoundException {
		File inputFile = new File(
				"/Users/fedja/Work/Papers/LREC10/Results/fragmentsTypesFreq.txt");
		File outputFile = new File(
				"/Users/fedja/Work/Papers/LREC10/Results/freqCounts.txt");
		Scanner scan = new Scanner(inputFile);
		int currentFreq = -1;
		int currentTypesCount = 0;
		PrintWriter pw = new PrintWriter(outputFile);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			Integer freq = Integer.parseInt(lineSplit[1]);
			if (freq != currentFreq) {
				if (currentFreq != -1) {
					pw.println(currentFreq + "\t" + currentTypesCount);
				}
				currentFreq = freq;
				currentTypesCount = 1;
			} else
				currentTypesCount++;
		}
		pw.println(currentFreq + "\t" + currentTypesCount);
		pw.close();
	}

	public static void getColumn() throws FileNotFoundException {
		File inputFile = new File(
				"/Users/fedja/Work/Papers/LREC10/Results/fragmentsTypesFreq.txt");
		File outputFile = new File(
				"/Users/fedja/Work/Papers/LREC10/Results/freqs.txt");
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			int freq = Integer.parseInt(lineSplit[1]);
			pw.println(freq);
		}
		pw.close();
	}

	public static <T> String arrayToStringDeep(T[][] array) {
		String result = "{";
		for (T[] t : array) {
			result += Arrays.toString(t);
		}
		result += "}";
		return result;

	}

	public static String arrayToString(int[] array, char separator) {
		StringBuilder sb = new StringBuilder();
		int last = array.length - 1;
		for (int i = 0; i <= last; i++) {
			sb.append(array[i]);
			if (i != last)
				sb.append(separator);
		}
		return sb.toString();
	}
	
	public static String arrayToString(String[] array, char separator) {
		StringBuilder sb = new StringBuilder();
		int last = array.length - 1;
		for (int i = 0; i <= last; i++) {
			sb.append(array[i]);
			if (i != last)
				sb.append(separator);
		}
		return sb.toString();
	}

	public static String arrayToString(Object[] array, char separator) {
		StringBuilder sb = new StringBuilder();
		int last = array.length - 1;
		for (int i = 0; i <= last; i++) {
			sb.append(array[i] == null ? "null" : array[i].toString());
			if (i != last)
				sb.append(separator);
		}
		return sb.toString();
	}

	public static String arrayToString(double[] array, char separator) {
		StringBuilder sb = new StringBuilder();
		int last = array.length - 1;
		for (int i = 0; i <= last; i++) {
			sb.append(array[i]);
			if (i != last)
				sb.append(separator);
		}
		return sb.toString();
	}

	public static void expArray(double[] array) {
		for (int i = 0; i < array.length; i++) {
			array[i] = Math.exp(array[i]);
		}
	}

	public static void logArray(double[] array) {
		for (int i = 0; i < array.length; i++) {
			array[i] = Math.log(array[i]);
		}
	}

	public static <T> int match(ArrayList<T> a, ArrayList<T> b) {
		if (a.size() != b.size())
			return -1;
		Iterator<T> aIter = a.iterator();
		Iterator<T> bIter = b.iterator();
		int count = 0;
		while (aIter.hasNext()) {
			T aNext = aIter.next();
			T bNext = bIter.next();
			if (aNext.equals(bNext))
				count++;
		}
		return count;
	}

	public static int countCharInString(String currentLabel, char charToFind) {
		char[] array = currentLabel.toCharArray();
		int result = 0;
		for (char c : array) {
			if (c == charToFind)
				result++;
		}
		return result;
	}

	public static int roulette(double[] numberProb) {
		double random = Math.random();
		double total = 0;
		for (int i = 0; i < numberProb.length; i++) {
			total += numberProb[i];
			if (random < total)
				return i;
		}
		return -1;
	}

	public static void bruinSequences(int k, int n) {
		int[] list = new int[n];
		Arrays.fill(list, k);
		int[][] sequences = Utility.combinations(list);
		// for(int[] seq : sequences) {
		// System.out.print(Arrays.toString(seq) + " ");
		// }
		int length = sequences.length;
		boolean[] taken = new boolean[length];

		int[] result = new int[length + n - 1];
		int[] seq = sequences[0];
		for (int i = 0; i < n; i++) {
			result[i] = seq[i];
		}
		taken[0] = true;
		int resIndex = n;
		int[] lastDigits = Arrays.copyOfRange(seq, 1, n);

		while (resIndex < result.length) {
			boolean found = false;
			for (int i = length - 1; i >= 0; i--) {
				if (taken[i])
					continue;
				seq = sequences[i];
				int[] firstDigits = Arrays.copyOfRange(seq, 0, n - 1);
				if (Arrays.equals(lastDigits, firstDigits)) {
					taken[i] = true;
					found = true;
					lastDigits = Arrays.copyOfRange(seq, 1, n);
					result[resIndex++] = seq[n - 1];
					break;
				}
			}
			if (!found) {
				System.err.println("Sequence not found!");
				return;
			}
		}

		System.out.println(Arrays.toString(result));
	}

	public static <T> Vector<ArrayList<T>> splitArrayList(
			ArrayList<T> fullList, int splits) {

		Vector<ArrayList<T>> result = new Vector<ArrayList<T>>(splits);

		int size = fullList.size();
		int elementsPerSplit = size / splits;
		int remainingSentences = size % splits;

		if (remainingSentences != 0) {
			elementsPerSplit++;
		}

		for (int i = 0; i < splits - 1; i++) {
			int startIndex = elementsPerSplit * i;
			int endIndex = elementsPerSplit * (i + 1);
			result.add(new ArrayList<T>(fullList.subList(startIndex, endIndex)));
		}

		// last split
		int startIndex = elementsPerSplit * (splits - 1);
		result.add(new ArrayList<T>(fullList.subList(startIndex, size)));

		return result;
	}

	public static int[] countDownArray(int length) {
		int[] result = new int[length];
		int index = 0;
		for (int i = length - 1; i >= 0; i--) {
			result[index++] = i;
		}
		return result;
	}

	public static int[] countUpArray(int length) {
		int[] result = new int[length];
		for (int i = 0; i < length; i++) {
			result[i] = i;
		}
		return result;
	}

	public static BitSet makeBitSet(int[] array) {
		BitSet bs = new BitSet(array.length);
		for (int i : array) {
			bs.set(i);
		}
		return bs;
	}
	
	public static int[] makeArray(BitSet bs) {
		int c = bs.cardinality();
		int[] result = new int[c];		
		int nextIndex = 0;
		for(int i=0; i<c; i++) {
			nextIndex = bs.nextSetBit(nextIndex);
			result[i] = nextIndex;
			nextIndex++;
		}
		return result;
	}

	public static String formatArrayNumbersReadable(int[] intArray) {
		int length = intArray.length;
		String[] result = new String[length];
		for (int i = 0; i < length; i++) {
			result[i] = formatNumberReaable(intArray[i]);
		}
		return Arrays.toString(result);
	}

	public static String formatArrayNumbersReadable(long[] longArray) {
		int length = longArray.length;
		String[] result = new String[length];
		for (int i = 0; i < length; i++) {
			result[i] = formatNumberReadable(longArray[i]);
		}
		return Arrays.toString(result);
	}

	public static final DecimalFormat twoDigitFormatter = new DecimalFormat(
			"#.#");

	public static String formatNumberReaable(int number) {
		// max int 2G
		// max float
		if (number < 100)
			// 0 - 999
			return Integer.toString(number);
		if (number < 99500) {
			double d = ((double) number) / 1000;
			return twoDigitFormatter.format(d) + "K";
		}
		if (number < 995000000) {
			double d = ((double) number) / 1000000;
			return twoDigitFormatter.format(d) + "M";
		}
		double d = ((double) number) / 1000000000;
		return twoDigitFormatter.format(d) + "B";
	}

	public static String formatNumberTwoDigit(float f) {
		return twoDigitFormatter.format(f);
	}

	public static String formatNumberReadable(long number) {
		// max int 2G
		// max float
		if (number < 100)
			// 0 - 999
			return Long.toString(number);
		if (number < 99500) {
			double d = ((double) number) / 1000;
			return twoDigitFormatter.format(d) + "K";
		}
		if (number < 995000000) {
			double d = ((double) number) / 1000000;
			return twoDigitFormatter.format(d) + "M";
		}
		double d = ((double) number) / 1000000000;
		return twoDigitFormatter.format(d) + "B";
	}

	public static byte[] getDigits(int number) {
		char[] digitChar = Integer.toString(number).toCharArray();
		int length = digitChar.length;
		byte[] result = new byte[length];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) (digitChar[i] - 48);
		}
		return result;
	}

	public static String removeBrackAndDoTabulation(String s) {
		String result = s.replaceAll(", ", "\t");
		return result.substring(1, result.length() - 1);
	}

	public static String[] makePercentage(int[] array) {
		int total = sum(array);
		if (total == 0)
			return new String[] { "0%", "0%" };
		int length = array.length;
		String[] result = new String[length];
		for (int i = 0; i < length; i++) {
			result[i] = twoDigitFormatter.format((double) array[i] * 100
					/ total)
					+ "%";
		}
		return result;
	}

	public static String makePercentage(int a, int total) {
		if (total == 0)
			return "NaN";
		return twoDigitFormatter.format((double) a * 100 / total) + "%";
	}

	public static <S, T> boolean putInHashMap(HashMap<S, HashSet<T>> table,
			S key, T valueElement) {
		HashSet<T> value = table.get(key);
		if (value == null) {
			value = new HashSet<T>();
			table.put(key, value);
		}
		return value.add(valueElement);
	}

	public static <S, T> boolean putInTreeMapSet(TreeMap<S, HashSet<T>> table,
			S key, T valueElement) {
		HashSet<T> value = table.get(key);
		if (value == null) {
			value = new HashSet<T>();
			table.put(key, value);
		}
		return value.add(valueElement);
	}
	
	public static <S, T> void putInTreeMapCollection(TreeMap<S, ArrayList<T>> table, 
			S key, T valueElement) {
		ArrayList<T> value = table.get(key);
		if (value == null) {
			value = new ArrayList<T>();
			table.put(key, value);
		}
		value.add(valueElement);		
	}

	public static <S, T, Z> boolean putInTreeMapSet(
			TreeMap<S, TreeMap<T, HashSet<Z>>> table, S firstKey, T secondKey,
			Z valueElement) {

		TreeMap<T, HashSet<Z>> value = table.get(firstKey);
		if (value == null) {
			value = new TreeMap<T, HashSet<Z>>();
			table.put(firstKey, value);
			HashSet<Z> set = new HashSet<Z>();
			set.add(valueElement);
			value.put(secondKey, set);
			return true;
		}
		return putInTreeMapSet(value, secondKey, valueElement);
	}

	public static <S, T, Z> boolean putInHashMapDoubleArrayList(
			AbstractMap<S, HashMap<T, ArrayList<Z>>> table, S firstKey,
			T secondKey, Z value) {

		HashMap<T, ArrayList<Z>> subTable = table.get(firstKey);
		if (subTable == null) {
			subTable = new HashMap<T, ArrayList<Z>>();
			ArrayList<Z> array = new ArrayList<Z>();
			array.add(value);
			subTable.put(secondKey, array);
			table.put(firstKey, subTable);
			return true;
		}
		ArrayList<Z> array = subTable.get(secondKey);
		if (array == null) {
			array = new ArrayList<Z>();
			subTable.put(secondKey, array);
		}
		return array.add(value);
	}

	public static <S, T, Z> boolean putInHashMapDoubleTreeSet(
			AbstractMap<S, HashMap<T, TreeSet<Z>>> table, S firstKey,
			T secondKey, Z value) {

		HashMap<T, TreeSet<Z>> subTable = table.get(firstKey);
		if (subTable == null) {
			subTable = new HashMap<T, TreeSet<Z>>();
			TreeSet<Z> set = new TreeSet<Z>();
			set.add(value);
			subTable.put(secondKey, set);
			table.put(firstKey, subTable);
			return true;
		}
		TreeSet<Z> set = subTable.get(secondKey);
		if (set == null) {
			set = new TreeSet<Z>();
			subTable.put(secondKey, set);
		}
		return set.add(value);
	}

	public static <S, T> boolean putInHashMapArrayList(
			HashMap<S, ArrayList<T>> table, S key, T valueElement) {
		ArrayList<T> value = table.get(key);
		if (value == null) {
			value = new ArrayList<T>();
			table.put(key, value);
		}
		return value.add(valueElement);
	}


	public static <S, T> boolean putInHashMapHashSet(
			HashMap<S, HashMap<S, HashSet<T>>> table, S firstKey, S secondKey,
			T valueElement) {
		HashMap<S, HashSet<T>> mapValue = table.get(firstKey);
		if (mapValue == null) {
			mapValue = new HashMap<S, HashSet<T>>();
			table.put(firstKey, mapValue);
		}
		return putInHashMap(mapValue, secondKey, valueElement);
	}

	public static <R, Q, S, T, Z> boolean putInMapQuadruple(
			AbstractMap<R, HashMap<Q, HashMap<S, HashMap<T, Z>>>> table,
			R firstKey, Q secondKey, S thirdKey, T forthKey, Z value) {

		HashMap<Q, HashMap<S, HashMap<T, Z>>> valueOne = table.get(firstKey);
		if (valueOne == null) {
			valueOne = new HashMap<Q, HashMap<S, HashMap<T, Z>>>();
			HashMap<S, HashMap<T, Z>> valueTwo = new HashMap<S, HashMap<T, Z>>();
			HashMap<T, Z> valueThree = new HashMap<T, Z>();
			valueThree.put(forthKey, value);
			valueTwo.put(thirdKey, valueThree);
			valueOne.put(secondKey, valueTwo);
			table.put(firstKey, valueOne);
			return true;
		}
		return putInMapTriple(valueOne, secondKey, thirdKey, forthKey,
				value);
	}

	public static <Q, S, T, Z> boolean putInMapTriple(
			AbstractMap<Q, HashMap<S, HashMap<T, Z>>> table, Q firstKey,
			S secondKey, T thirdKey, Z value) {

		HashMap<S, HashMap<T, Z>> valueOne = table.get(firstKey);
		if (valueOne == null) {
			valueOne = new HashMap<S, HashMap<T, Z>>();
			HashMap<T, Z> valueTwo = new HashMap<T, Z>();
			valueTwo.put(thirdKey, value);
			valueOne.put(secondKey, valueTwo);
			table.put(firstKey, valueOne);
			return true;
		}
		return putInMapDouble(valueOne, secondKey, thirdKey, value);
	}

	public static <S, T, Z> boolean putInMapDouble(
			AbstractMap<S, HashMap<T, Z>> table, S firstKey, T secondKey, Z value) {
		HashMap<T, Z> mapValue = table.get(firstKey);
		if (mapValue == null) {
			mapValue = new HashMap<T, Z>();
			table.put(firstKey, mapValue);
			mapValue.put(secondKey, value);
			return true;
		}
		return mapValue.put(secondKey, value) == null;
	}

	public static <S, T, Z> boolean containsSecondKey(
			HashMap<S, HashMap<T, Z>> table, S firstKey, T secondKey) {
		HashMap<T, Z> mapValue = table.get(firstKey);
		if (mapValue == null) {
			return false;
		}
		return mapValue.containsKey(secondKey);
	}

	public static <S, T, Y, Z> boolean containsThirdKey(
			HashMap<S, HashMap<T, HashMap<Y, Z>>> table, S firstKey,
			T secondKey, Y thirdKey) {
		HashMap<T, HashMap<Y, Z>> mapValue = table.get(firstKey);
		if (mapValue == null) {
			return false;
		}
		return containsSecondKey(mapValue, secondKey, thirdKey);
	}

	public static <S, T, Z> Z getSecondKey(HashMap<S, HashMap<T, Z>> table,
			S firstKey, T secondKey) {
		HashMap<T, Z> mapValue = table.get(firstKey);
		if (mapValue == null) {
			return null;
		}
		return mapValue.get(secondKey);
	}

	public static <S, T, Y, Z> Z getThirdKey(
			HashMap<S, HashMap<T, HashMap<Y, Z>>> table, S firstKey,
			T secondKey, Y thirdKey) {
		HashMap<T, HashMap<Y, Z>> mapValue = table.get(firstKey);
		if (mapValue == null) {
			return null;
		}
		return getSecondKey(mapValue, secondKey, thirdKey);
	}

	public static <T> boolean increaseInHashMap(AbstractMap<T, int[]> table, T key) {
		int[] value = table.get(key);
		if (value == null) {
			value = new int[] { 1 };
			table.put(key, value);
			return true;
		}
		value[0]++;
		return false;
	}

	public static <T> int[] increaseInHashMapIndex(HashMap<T, int[]> result,
			T firstKey, int toAdd, int index, int size) {
		int[] value = result.get(firstKey);
		if (value == null) {
			value = new int[size];
			result.put(firstKey, value);
		}
		value[index] += toAdd;
		return value;
	}
	
	public static <S,T> void increaseInHashMapIndex(HashMap<S, HashMap<T, int[]>> result,
			S firstKey, T secondKey, int toAdd, int index, int size) {
		
		HashMap<T, int[]> value = result.get(firstKey);
		if (value==null) {
			value = new HashMap<T, int[]>();
			result.put(firstKey, value);
		}
		increaseInHashMapIndex(value, secondKey, toAdd, index, size);
	}

	public static <T> boolean increaseInHashMap(HashMap<T, double[]> table,
			T key, double d) {
		double[] value = table.get(key);
		if (value == null) {
			value = new double[] { d };
			table.put(key, value);
			return true;
		}
		value[0] += d;
		return false;
	}

	public static <S, T> boolean increaseInHashMap(
			HashMap<S, HashMap<T, double[]>> table, S firstKey, T secondKey,
			double d) {
		HashMap<T, double[]> value = table.get(firstKey);
		if (value == null) {
			value = new HashMap<T, double[]>();
			table.put(firstKey, value);
			value.put(secondKey, new double[] { d });
			return true;
		}
		return increaseInHashMap(value, secondKey, d);
	}

	public static <T> boolean increaseInHashMapArray(
			HashMap<T, double[]> table, T key, double[] toAdd) {
		double[] value = table.get(key);
		if (value == null) {
			value = Arrays.copyOf(toAdd, toAdd.length);
			table.put(key, value);
			return true;
		}
		arrayDoublePlus(toAdd, value);
		return false;
	}

	public static <T> boolean increaseInHashMapLog(HashMap<T, double[]> table,
			T key, double d) {
		double[] value = table.get(key);
		if (value == null) {
			value = new double[] { d };
			table.put(key, value);
			return true;
		}
		value[0] = Utility.logSum(value[0], d);
		return false;
	}

	public static <S, T> boolean increaseInHashMapLogDouble(
			HashMap<S, HashMap<T, double[]>> table, S firstKey, T secondKey,
			double d) {
		HashMap<T, double[]> value = table.get(firstKey);
		if (value == null) {
			value = new HashMap<T, double[]>();
			table.put(firstKey, value);
			value.put(secondKey, new double[] { d });
			return true;
		}
		return increaseInHashMapLog(value, secondKey, d);
	}

	public static <T> boolean increaseInHashMap(HashMap<T, int[]> table, T key,
			int toAdd) {
		int[] value = table.get(key);
		if (value == null) {
			value = new int[] { toAdd };
			table.put(key, value);
			return true;
		}
		value[0] += toAdd;
		return false;
	}

	public static <S, T> boolean increaseInHashMap(
			HashMap<S, HashMap<T, int[]>> table, S key, T valueElement) {
		HashMap<T, int[]> value = table.get(key);
		if (value == null) {
			value = new HashMap<T, int[]>();
			table.put(key, value);
			value.put(valueElement, new int[] { 1 });
			return true;
		}
		return increaseInHashMap(value, valueElement);
	}

	public static <S, T> boolean increaseInHashMap(
			HashMap<S, HashMap<T, int[]>> table, S key, T valueElement,
			int toAdd) {
		HashMap<T, int[]> value = table.get(key);
		if (value == null) {
			value = new HashMap<T, int[]>();
			table.put(key, value);
			value.put(valueElement, new int[] { toAdd });
			return true;
		}
		return increaseInHashMap(value, valueElement, toAdd);
	}

	public static <V, S, T> boolean increaseInHashMapDouble(
			HashMap<V, HashMap<S, HashMap<T, int[]>>> table, V firstKey,
			S secondkey, T valueElement, int toAdd) {
		HashMap<S, HashMap<T, int[]>> value = table.get(firstKey);
		if (value == null) {
			value = new HashMap<S, HashMap<T, int[]>>();
			table.put(firstKey, value);
		}
		return increaseInHashMap(value, secondkey, valueElement, toAdd);
	}

	public static <S, T> boolean increaseInHashMap(
			HashMap<S, HashMap<T, int[]>> table, S key, T valueElement,
			int[] toAdd) {
		HashMap<T, int[]> value = table.get(key);
		if (value == null) {
			value = new HashMap<T, int[]>();
			table.put(key, value);
			value.put(valueElement, Arrays.copyOf(toAdd, toAdd.length));
			return true;
		}
		return increaseInHashMap(value, valueElement, toAdd);
	}

	public static <T> boolean increaseInHashMap(HashMap<T, int[]> table, T key,
			int[] toAdd) {
		int[] value = table.get(key);
		if (value == null) {
			value = Arrays.copyOf(toAdd, toAdd.length);
			table.put(key, value);
			return true;
		}
		arrayIntPlus(toAdd, value);
		return false;
	}

	public static <S, T, Z> void increaseAllHashMapTriple(
			HashMap<S, HashMap<T, HashMap<Z, double[]>>> source,
			HashMap<S, HashMap<T, HashMap<Z, double[]>>> target) {

		for (Entry<S, HashMap<T, HashMap<Z, double[]>>> e : source.entrySet()) {
			S key = e.getKey();
			HashMap<T, HashMap<Z, double[]>> subSource = e.getValue();
			HashMap<T, HashMap<Z, double[]>> subTarget = target.get(key);
			if (subTarget == null) {
				subTarget = new HashMap<T, HashMap<Z, double[]>>();
				target.put(key, subTarget);
			}
			increaseAllHashMapDouble(subSource, subTarget);
		}

	}

	public static <T, Z> void increaseAllHashMapDouble(
			HashMap<T, HashMap<Z, double[]>> source,
			HashMap<T, HashMap<Z, double[]>> target) {

		for (Entry<T, HashMap<Z, double[]>> e : source.entrySet()) {
			T key = e.getKey();
			HashMap<Z, double[]> subSource = e.getValue();
			HashMap<Z, double[]> subTarget = target.get(key);
			if (subTarget == null) {
				subTarget = new HashMap<Z, double[]>();
				target.put(key, subTarget);
			}
			increaseAllHashMap(subSource, subTarget);
		}
	}

	public static <Z> void increaseAllHashMap(HashMap<Z, double[]> source,
			HashMap<Z, double[]> target) {

		for (Entry<Z, double[]> e : source.entrySet()) {
			Z key = e.getKey();
			double[] valueSource = e.getValue();
			double[] valueTarget = target.get(key);
			if (valueTarget == null) {
				valueTarget = new double[] { valueSource[0] };
				target.put(key, valueTarget);
			} else {
				valueTarget[0] += valueSource[0];
			}

		}
	}

	public static <S, T, Z> void increaseAllIntHashMapTriple(
			HashMap<S, HashMap<T, HashMap<Z, int[]>>> source,
			HashMap<S, HashMap<T, HashMap<Z, int[]>>> target) {

		for (Entry<S, HashMap<T, HashMap<Z, int[]>>> e : source.entrySet()) {
			S key = e.getKey();
			HashMap<T, HashMap<Z, int[]>> subSource = e.getValue();
			HashMap<T, HashMap<Z, int[]>> subTarget = target.get(key);
			if (subTarget == null) {
				subTarget = new HashMap<T, HashMap<Z, int[]>>();
				target.put(key, subTarget);
			}
			increaseAllIntHashMapDouble(subSource, subTarget);
		}

	}

	public static <T, Z> void increaseAllIntHashMapDouble(
			HashMap<T, HashMap<Z, int[]>> source,
			HashMap<T, HashMap<Z, int[]>> target) {

		for (Entry<T, HashMap<Z, int[]>> e : source.entrySet()) {
			T key = e.getKey();
			HashMap<Z, int[]> subSource = e.getValue();
			HashMap<Z, int[]> subTarget = target.get(key);
			if (subTarget == null) {
				subTarget = new HashMap<Z, int[]>();
				target.put(key, subTarget);
			}
			increaseAllIntHashMap(subSource, subTarget);
		}
	}

	public static <Z> void increaseAllIntHashMap(HashMap<Z, int[]> source,
			HashMap<Z, int[]> target) {

		for (Entry<Z, int[]> e : source.entrySet()) {
			Z key = e.getKey();
			int[] valueSource = e.getValue();
			int[] valueTarget = target.get(key);
			if (valueTarget == null) {
				target.put(key, valueSource);
			} else {
				valueTarget[0] += valueSource[0];
			}

		}
	}

	public static <S, T, Z> void increaseHashMapTriple(
			HashMap<S, HashMap<T, HashMap<Z, double[]>>> target, S firstKey,
			T secondKey, Z thirdKey, double[] value) {

		HashMap<T, HashMap<Z, double[]>> subTarget = target.get(firstKey);
		if (subTarget == null) {
			subTarget = new HashMap<T, HashMap<Z, double[]>>();
			target.put(firstKey, subTarget);
		}
		increaseHashMapDouble(subTarget, secondKey, thirdKey, value);

	}

	public static <T, Z> void increaseHashMapDouble(
			HashMap<T, HashMap<Z, double[]>> target, T firstKey, Z secondKey,
			double[] value) {

		HashMap<Z, double[]> subTarget = target.get(firstKey);
		if (subTarget == null) {
			subTarget = new HashMap<Z, double[]>();
			target.put(firstKey, subTarget);
		}
		increaseHashMap(subTarget, secondKey, value);
	}

	public static <Z> void increaseHashMap(HashMap<Z, double[]> target, Z key,
			double[] value) {

		double[] valueTarget = target.get(key);
		if (valueTarget == null) {
			valueTarget = new double[] { value[0] };
			target.put(key, valueTarget);
		} else {
			valueTarget[0] += value[0];
		}
	}

	public static <S, T> boolean maximizeInHashMap(
			HashMap<S, HashMap<T, int[]>> table, S key, T valueElement,
			int[] toAdd) {
		HashMap<T, int[]> value = table.get(key);
		if (value == null) {
			value = new HashMap<T, int[]>();
			table.put(key, value);
			value.put(valueElement, Arrays.copyOf(toAdd, toAdd.length));
			return true;
		}
		return maximizeInHashMap(value, valueElement, toAdd);
	}

	public static <T> boolean maximizeInHashMap(HashMap<T, int[]> table, T key,
			int[] toAdd) {
		int[] value = table.get(key);
		if (value == null) {
			value = Arrays.copyOf(toAdd, toAdd.length);
			table.put(key, value);
			return true;
		}
		if (toAdd[0] > value[0]) {
			for (int i = 0; i < toAdd.length; i++) {
				value[i] = toAdd[i];
			}
		}
		return false;
	}

	public static <S, T> void replaceIfGreaterInHashMap(
			HashMap<S, HashMap<T, int[]>> table, S key, T valueElement, int freq) {
		HashMap<T, int[]> value = table.get(key);
		if (value == null) {
			value = new HashMap<T, int[]>();
			table.put(key, value);
			value.put(valueElement, new int[] { freq });
			return;
		}
		replaceIfGreaterInHashMap(value, valueElement, freq);
	}

	public static <T> void replaceIfGreaterInHashMap(HashMap<T, int[]> table,
			T key, int freq) {
		int[] value = table.get(key);
		if (value == null) {
			value = new int[] { freq };
			table.put(key, value);
			return;
		}
		if (freq > value[0])
			value[0] = freq;
	}

	public static <S, T> int countTotalElements(
			HashMap<S, HashMap<T, int[]>> table) {
		int count = 0;
		for (HashMap<T, int[]> set : table.values()) {
			count += set.size();
		}
		return count;
	}

	public static <S, T> int countTotal(HashMap<S, HashSet<T>> table) {
		int count = 0;
		for (HashSet<T> set : table.values()) {
			count += set.size();
		}
		return count;
	}

	public static <S, T> int countTotalArray(HashMap<S, ArrayList<T>> table) {
		int count = 0;
		for (ArrayList<T> list : table.values()) {
			count += list.size();
		}
		return count;
	}

	public static <S, T, Z> int countTotalArrayDouble(
			HashMap<S, HashMap<T, ArrayList<Z>>> table) {
		int count = 0;
		for (HashMap<T, ArrayList<Z>> subTable : table.values()) {
			count += countTotalArray(subTable);
		}
		return count;
	}

	public static <S, T, Z> int countTotalDouble(HashMap<S, HashMap<T, Z>> table) {
		int count = 0;
		for (HashMap<T, Z> set : table.values()) {
			count += set.size();
		}
		return count;
	}

	public static <S, T, Z, W> int countTotalTriple(
			HashMap<S, HashMap<T, HashMap<Z, W>>> table) {
		int count = 0;
		for (HashMap<T, HashMap<Z, W>> set : table.values()) {
			for (HashMap<Z, W> t : set.values()) {
				count += t.size();
			}
		}
		return count;
	}

	public static String repeat(String s, int repetitions) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < repetitions; i++) {
			sb.append(s);
		}
		return sb.toString();
	}

	public static String formatIntArray(int[] a) {
		return Utility.removeBrackAndDoTabulation(Utility
				.formatArrayNumbersReadable(a));
	}

	public static <S, T> HashMap<S, HashSet<T>> deepHashMapClone(
			HashMap<S, HashSet<T>> table) {
		HashMap<S, HashSet<T>> result = new HashMap<S, HashSet<T>>();
		if (table == null)
			return result;
		for (Entry<S, HashSet<T>> e : table.entrySet()) {
			result.put(e.getKey(), new HashSet<T>(e.getValue()));
		}
		return result;
	}

	public static <S, T> HashMap<S, HashMap<T, int[]>> deepHashMapCloneFreq(
			HashMap<S, HashMap<T, int[]>> table) {

		HashMap<S, HashMap<T, int[]>> result = new HashMap<S, HashMap<T, int[]>>();
		if (table == null)
			return result;
		for (Entry<S, HashMap<T, int[]>> e : table.entrySet()) {
			HashMap<T, int[]> subTable = new HashMap<T, int[]>();
			for (Entry<T, int[]> f : e.getValue().entrySet()) {
				subTable.put(f.getKey(), new int[] { f.getValue()[0] });
			}
			result.put(e.getKey(), subTable);
		}
		return result;
	}

	public static <S, T, Z> HashMap<S, HashMap<T, Z>> deepHashMapDoubleClone(
			HashMap<S, HashMap<T, Z>> table) {
		HashMap<S, HashMap<T, Z>> result = new HashMap<S, HashMap<T, Z>>();
		if (table == null)
			return result;
		for (Entry<S, HashMap<T, Z>> e : table.entrySet()) {
			result.put(e.getKey(), new HashMap<T, Z>(e.getValue()));
		}
		return result;
	}

	public static <S, T> HashMap<S, HashMap<T, double[]>> deepHashMapDoubleCloneArray(
			HashMap<S, HashMap<T, double[]>> table) {
		HashMap<S, HashMap<T, double[]>> result = new HashMap<S, HashMap<T, double[]>>();
		if (table == null)
			return result;
		for (Entry<S, HashMap<T, double[]>> e : table.entrySet()) {
			result.put(e.getKey(), hashMapDoubleClone(e.getValue()));
		}
		return result;
	}

	public static <T> HashMap<T, double[]> hashMapDoubleClone(
			HashMap<T, double[]> table) {
		HashMap<T, double[]> result = new HashMap<T, double[]>();
		for (Entry<T, double[]> e : table.entrySet()) {
			double[] original = e.getValue();
			result.put(e.getKey(), Arrays.copyOf(original, original.length));
		}
		return result;
	}

	static final double minDouble = -Double.MAX_VALUE;

	public static <S> double getMaxValue(HashMap<S, double[]> table) {
		double max = minDouble;
		for (double[] d : table.values()) {
			double dValue = d[0];
			if (dValue > max)
				max = dValue;
		}
		return max;
	}

	public static <S> double getSumValue(HashMap<S, double[]> table) {
		double sum = 0;
		for (double[] d : table.values()) {
			sum += d[0];
		}
		return sum;
	}

	public static <S> double getSumValue(HashMap<S, double[]> table, int index) {
		double sum = 0;
		for (double[] d : table.values()) {
			sum += d[index];
		}
		return sum;
	}

	public static <S> int getSumValueInt(HashMap<S, int[]> table) {
		int sum = 0;
		for (int[] d : table.values()) {
			sum += d[0];
		}
		return sum;
	}

	public static <S, T> double getSumValueDouble(
			HashMap<S, HashMap<T, double[]>> table) {
		double sum = 0;
		for (HashMap<T, double[]> subTable : table.values()) {
			sum += getSumValue(subTable);
		}
		return sum;
	}

	public static <S> int getMaxValueInt(HashMap<S, int[]> table) {
		int max = Integer.MIN_VALUE;
		for (int[] i : table.values()) {
			int iValue = i[0];
			if (iValue > max)
				max = iValue;
		}
		return max;
	}

	public static <S> Entry<S, double[]> getMaxEntry(HashMap<S, double[]> table) {
		double max = minDouble;
		Entry<S, double[]> result = null;
		for (Entry<S, double[]> e : table.entrySet()) {
			double prob = e.getValue()[0];
			if (prob > max) {
				max = prob;
				result = e;
			}
		}
		return result;
	}

	static final int minInt = Integer.MIN_VALUE;

	public static <S> S getMaxKeyInt(HashMap<S, int[]> table) {
		int max = minInt;
		S result = null;
		for (Entry<S, int[]> e : table.entrySet()) {
			int freq = e.getValue()[0];
			if (freq > max) {
				max = freq;
				result = e.getKey();
			}
		}
		return result;
	}

	public static <S> S getMaxKeyDouble(HashMap<S, double[]> table) {
		double max = minDouble;
		S result = null;
		for (Entry<S, double[]> e : table.entrySet()) {
			double freq = e.getValue()[0];
			if (freq > max) {
				max = freq;
				result = e.getKey();
			}
		}
		return result;
	}

	public static <S, T> Entry<T, double[]> getMaxEntryDouble(
			HashMap<S, HashMap<T, double[]>> table) {

		double max = minDouble;
		Entry<T, double[]> result = null;
		for (HashMap<T, double[]> subTable : table.values()) {
			Entry<T, double[]> e = getMaxEntry(subTable);
			// if (e!=null) {
			double prob = e.getValue()[0];
			if (prob > max) {
				max = prob;
				result = e;
			}
			// }
		}
		return result;
	}

	public static <S, T> HashMap<T, double[]> getAllMaxEntryDouble(
			HashMap<S, HashMap<T, double[]>> table) {

		HashMap<T, double[]> result = new HashMap<T, double[]>();

		double max = minDouble;
		for (HashMap<T, double[]> subTable : table.values()) {
			Entry<T, double[]> e = getMaxEntry(subTable);
			double prob = e.getValue()[0];
			if (prob > max) {
				max = prob;
				result.clear();
				result.put(e.getKey(), e.getValue());
			}
			if (prob == max)
				result.put(e.getKey(), e.getValue());
		}
		return result;
	}

	public static <S> S getMaxKey(HashMap<S, double[]> table) {
		double max = minDouble;
		S result = null;
		for (Entry<S, double[]> e : table.entrySet()) {
			double prob = e.getValue()[0];
			if (prob > max) {
				max = prob;
				result = e.getKey();
			}
		}
		return result;
	}

	public static <S> S getRandomElement(Set<S> keySet) {
		ArrayList<S> list = new ArrayList<S>(keySet);
		Collections.shuffle(list);
		return list.iterator().next();
	}

	public static <S> HashMap<S, int[]> convertHashSetInHashMap(Set<S> set,
			int defaultCount) {
		HashMap<S, int[]> result = new HashMap<S, int[]>();
		for (S key : set) {
			result.put(key, new int[] { defaultCount });
		}
		return result;
	}

	public static <S> HashMap<S, Integer> convertHashMapIntArrayInteger(
			HashMap<S, int[]> map) {
		HashMap<S, Integer> result = new HashMap<S, Integer>();
		for (Entry<S, int[]> e : map.entrySet()) {
			result.put(e.getKey(), e.getValue()[0]);
		}
		return result;
	}

	public static int compare(int[] a, int[] b) {
		int last = a.length - 1;
		int result = 0;
		for (int i = 0; i <= last; i++) {
			result = new Integer(a[i]).compareTo(new Integer(b[i]));
			if (result != 0)
				return result;
		}
		return result;
	}

	public static <T> String tableIntToString(HashMap<T, int[]> table) {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<T, int[]>> iter = table.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<T, int[]> e = iter.next();
			sb.append(e.getKey()).append(Arrays.toString(e.getValue()));
			if (iter.hasNext())
				sb.append(", ");
		}
		return sb.toString();
	}

	public static void multiply(int[] a, int f) {
		for (int i = 0; i < a.length; i++) {
			a[i] *= f;
		}
	}

	public static <T> void removeAll(ArrayList<T> a, T t) {
		Iterator<T> i = a.iterator();
		while (i.hasNext()) {
			if (i.next().equals(t))
				i.remove();
		}
	}

	public static <S, T, Z> int countAmbiguousMapping(
			HashMap<S, HashMap<T, Z>> table) {
		int result = 0;
		for (HashMap<T, Z> subTabel : table.values()) {
			if (subTabel.size() > 1)
				result++;
		}
		return result;
	}

	public static <S, T, Z> HashMap<T, HashMap<S, Z>> invertHashMapDouble(
			HashMap<S, HashMap<T, Z>> table) {

		HashMap<T, HashMap<S, Z>> newTable = new HashMap<T, HashMap<S, Z>>();

		for (Entry<S, HashMap<T, Z>> e : table.entrySet()) {
			S keyS = e.getKey();
			for (Entry<T, Z> f : e.getValue().entrySet()) {
				T keyT = f.getKey();
				Z valueZ = f.getValue();
				putInMapDouble(newTable, keyT, keyS, valueZ);
			}
		}

		return newTable;
	}
	
	/**
	 * 
	 * @param <S>
	 * @param <T> assumes all value in the original table are not duplicates
	 * @param table
	 * @return
	 */
	public static <S, T> TreeMap<T, S> invertHashMapInTreeMap(HashMap<S, T> table) {

		TreeMap<T, S> result = new TreeMap<T, S>();
		for (Entry<S,T> e : table.entrySet()) {
			result.put(e.getValue(), e.getKey());
		}
		return result;
	}

	public static <P, S, T, Z> boolean putInHashMapIfNotPresent(
			HashMap<P, HashMap<S, HashMap<T, Z>>> table, P firstKey,
			S secondKey, T thirdKey, Z value) {
		HashMap<S, HashMap<T, Z>> subTable = table.get(firstKey);
		if (subTable == null) {
			subTable = new HashMap<S, HashMap<T, Z>>();
			putInMapDouble(subTable, secondKey, thirdKey, value);
			table.put(firstKey, subTable);
			return true;
		}
		return putInHashMapIfNotPresent(subTable, secondKey, thirdKey, value);
	}

	public static <S, T, Z> boolean putInHashMapIfNotPresent(
			HashMap<S, HashMap<T, Z>> table, S firstKey, T secondKey, Z value) {
		HashMap<T, Z> subTable = table.get(firstKey);
		if (subTable == null) {
			subTable = new HashMap<T, Z>();
			subTable.put(secondKey, value);
			table.put(firstKey, subTable);
			return true;
		}
		return putInHashMapIfNotPresent(subTable, secondKey, value);
	}

	public static <T, Z> boolean putInHashMapIfNotPresent(HashMap<T, Z> table,
			T key, Z value) {
		if (!table.containsKey(key)) {
			table.put(key, value);
			return true;
		}
		return false;
	}

	public static <T> int countEquals(List<T> list, T e) {
		int result = 0;
		for (T t : list) {
			if (t.equals(e))
				result++;
		}
		return result;
	}

	public static double[] totalSumAndEntropy(Collection<Integer> values) {
		double sum = sum(values);
		double entropy = 0;
		for (double d : values) {
			double p = d / sum;
			entropy += p * Math.log(p);
		}
		entropy = -entropy;
		return new double[] { sum, entropy };
	}
	
	public static double entropy(Collection<Integer> values) {
		double sum = sum(values);
		double entropy = 0;
		for (double d : values) {
			double p = d / sum;
			entropy += p * Math.log(p);
		}
		entropy = -entropy;
		return entropy;
	}

	public static <T> int totalSumValues(HashMap<T, int[]> table) {
		int result = 0;
		for (int[] v : table.values()) {
			result += v[0];
		}
		return result;
	}
	
	public static int[] addOne(int[] a) {		
		int[] result = new int[a.length];
		for(int i=0; i<a.length; i++) {
			result[i] = a[i]+1;
		}
		return result;
	}
	
	public static ArrayList<Integer> convertToArrayList(int[] a) {
		int l = a.length;
		ArrayList<Integer> result = new ArrayList<Integer>(l);
		for(int i: a) {
			result.add(i);
		}
		return result;
	}
	
	public static int[] convertToArrayList(ArrayList<Integer> a) {
		int[] result = new int[a.size()];
		int i=0;
		for(int e : a) {
			result[i++] = e;
		}
		return result;
	}
	
	public static <S,T> int countAllSubElements(
			HashMap<S, HashSet<T>> table) {
		int result = 0;
		for(HashSet<T> s : table.values()) {
			result += s.size();
		}
		return result;
	}


	public static <T> void setBitSetInHashMap(HashMap<T, BitSet> table, T key, int index) {
		BitSet bs = table.get(key);
		if (bs == null) {
			bs = new BitSet();
			table.put(key, bs);
		}
		bs.set(index);		
	}
	
	public static boolean allTrue(boolean[] a) {
		for(boolean b : a) {
			if (!b) return false;
		}
		return true;
	}

	public static void main(String args[]) throws FileNotFoundException {
		// testLogSum();
		// double d = Double.NaN;
		// System.out.println(d==Double.NaN);
		// HashMap<String,Integer> map = new HashMap<String,Integer>();
		// Iterator<Entry<String, Integer>> it = map.entrySet().iterator();

		// System.out.println(Arrays.deepToString(n_air(6,3)));
		
		/*
		int [][][] tot_comb = nair_multiple(4); 
		for(int[][] nair : tot_comb) {
			System.out.println(Arrays.deepToString(nair)); 
		}
		*/
	
		/*
		int[] compl = makeComplementary(new int[] { 0, 1, 2, 3, 4, 5 }, 6);
		System.out.println(Arrays.toString(compl));
		 */
		/*
		int[][][] i = new int[10][][];
		System.out.println(i[5]);
		*/
		System.out.println(getDateTime());
	}

	















}
