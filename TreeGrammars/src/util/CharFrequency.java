package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

public class CharFrequency {
	
	HashMap<Character, int[]> charTable;
	
	public CharFrequency(File[] fileArray, boolean connlCompressed) throws IOException {
		charTable = new HashMap<Character, int[]>();
		for(File f : fileArray) {
			if (connlCompressed)
				readFileConllCompressed(charTable, f);
			else	
				readFile(charTable, f);			
		}		
		HashMap<Character, Integer> charTableInteger = Utility.convertHashMapIntArrayInteger(charTable);
		TreeMap<Integer, Character> sortedChartTable = Utility.invertHashMapInTreeMap(charTableInteger);
		printSortedTable(sortedChartTable);
	}
	
	DecimalFormat df = new DecimalFormat("0.00000");
	private void printSortedTable(TreeMap<Integer, Character> sortedChartTable) {
		int total = 0;
		for(int freq : sortedChartTable.keySet()) {
			total += freq;
		}		
		for(Entry<Integer, Character> e : sortedChartTable.descendingMap().entrySet()) {
			String percentage = df.format(((double)e.getKey())/total);
			System.out.println(e.getValue() + "\t" + percentage);
		}
	}

	public static void readFile(HashMap<Character, int[]> table, File inputFile) throws FileNotFoundException {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			char[] charArray = line.toCharArray();
			for(Character c : charArray) {
				Utility.increaseInHashMap(table, c);
			}
		}
	}
	
	public void readFileConllCompressed(HashMap<Character, int[]> table, File inputFile) throws IOException {
		InputStream fileStream = new FileInputStream(inputFile);
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		
		String line = null;
		
		while((line = buffered.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			}
			String[] tabs = line.split("\t");
			if (tabs.length!=10) {
				continue;
			}
			//1: word
			for(Character c : tabs[1].toCharArray()) {
				Utility.increaseInHashMap(table, c);
			}			
		}						
		buffered.close();
	}
	
	public static void main(String[] args) throws IOException {
		File inputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/PAISA/paisa.annotated.CoNLL.utf8.gz");
		new CharFrequency(new File[]{inputFile}, true);
	}
	
}
