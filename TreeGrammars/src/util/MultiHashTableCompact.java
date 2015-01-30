package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import util.file.FileUtil;

public class MultiHashTableCompact {
	
	int intCounter;
	Hashtable<String, Integer> stringIntegerTable;
	Vector<String> stringVector;
	static Integer dummyIndex = 0; 
	
	Hashtable<Integer, Object> mainStructure;
	int levels;
	
	public MultiHashTableCompact() {
		intCounter = 1;
		mainStructure = new Hashtable<Integer, Object>();
		stringIntegerTable = new Hashtable<String, Integer>();
		stringVector = new Vector<String>();
	}
	
	public Integer addNewString(String s) {
		Integer newInt = intCounter;
		intCounter++;
		stringIntegerTable.put(s, newInt);
		stringVector.add(s);						
		return newInt;
	}
		
	@SuppressWarnings("unchecked")
	public void addOne(String multiString) {
		Hashtable<Integer, Object> structure = mainStructure;
		String[] multiStringSplit = multiString.split("_");
		for(String s : multiStringSplit) {
			//if (s.isEmpty()) continue;
			Integer stringIndex = stringIntegerTable.get(s);
			if (stringIndex==null) stringIndex = addNewString(s);
			Hashtable<Integer, Object> newStructure = 
				(Hashtable<Integer, Object>) structure.get(stringIndex);
			if (newStructure == null) {	
				newStructure = new Hashtable<Integer, Object>();
				structure.put(stringIndex, newStructure);								
			}
			structure = newStructure;			
		}
		increaseOneInTable(structure);
	}
	
	public static void increaseOneInTable(Hashtable<Integer, Object> table) {
		int[] freq = (int[]) table.get(dummyIndex);
		if (freq==null) {
			freq = new int[]{1};
			table.put(dummyIndex, freq);
		}
		else freq[0]++;
	}
	
	@SuppressWarnings("unchecked")
	public int get(String multiString) {
		Hashtable<Integer, Object> structure = mainStructure;
		String[] multiStringSplit = multiString.split("_");
		for(String s : multiStringSplit) {
			//if (s.isEmpty()) continue;
			Integer stringIndex = stringIntegerTable.get(s);		
			if (stringIndex==null) return 0;
			structure = (Hashtable<Integer, Object>) structure.get(stringIndex);
			if (structure==null) return 0;
		}
		return ((int[])structure.get(dummyIndex))[0];
	}
	
	@SuppressWarnings("unchecked")
	private static void printToFileRecursive(Hashtable<Integer, Object> structure, 
			String prefix, PrintWriter pw, Vector stringVector) { 
		Object freq = structure.get(dummyIndex);
		if (freq != null) {			
			pw.println(freq + "\t" + prefix);
			return;
		}		
		for(Integer key : structure.keySet()) {
			Hashtable<Integer, Object> subTable = (Hashtable<Integer, Object>) structure.get(key);
			prefix += "_" + stringVector.get(key);
			printToFileRecursive(subTable, prefix, pw, stringVector);
		}
	}

	public void printToFile(File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		printToFileRecursive(mainStructure, "", pw, stringVector);
		pw.close();
	}
}
