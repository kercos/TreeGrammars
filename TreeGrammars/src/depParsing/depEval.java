package depParsing;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import util.*;
import util.file.FileUtil;
import tdg.*;

public class depEval {

	public static void UASeval(ArrayList<TDNode> test, ArrayList<TDNode> gold, File outputFile) {		
		if (test.size() != gold.size()) {
			System.err.println("Gold and test unmatch in number of sentences");
			System.exit(-1);
		}		
		int[] result = new int[]{0,0}; 
		for(int i=0; i<test.size(); i++) {
			TDNode t = test.get(i);
			TDNode g = gold.get(i);
			t.UAS_Total(g, result);
		}
		String print = "Number of sentences: " + test.size() + "\n" +
						"Total Tokens: " + result[1] + "\n" +
						"Correct attachments: " + result[0] + "\n" +
						"UAS: " + (float)result[0]/result[1];
		System.out.println(print);
		java.io.PrintWriter out = FileUtil.getPrintWriter(outputFile);
		out.println(print);
		out.close();
	}
	
	public static void MALTevalUAS(File test, File gold, File outputFile) {
		Scanner testScan = FileUtil.getScanner(test);
		Scanner goldScan = FileUtil.getScanner(gold);
		int line = 0;
		int sentences = 0;
		int[] result = new int[]{0,0};
		
		while(testScan.hasNextLine()) {
			line++;
			String testLine = testScan.nextLine();
			String goldLine = goldScan.nextLine();
			if (testLine.isEmpty()) {
				sentences++;
				continue;
			}
			String[] testLineSplit = testLine.split("\t");
			String[] goldLineSplit = goldLine.split("\t");
			if (!testLineSplit[0].equals(goldLineSplit[0])) {
				System.err.println("Lexincon unmatch in line " + line + testLineSplit[0]+"-"+goldLineSplit[0]);
				return;
			}
			if (testLineSplit[2].equals(goldLineSplit[2])) result[0]++;
			result[1]++;
		}
		String print = "Number of sentences: " + sentences + "\n" +
		"Total Tokens: " + result[1] + "\n" +
		"Correct attachments: " + result[0] + "\n" +
		"UAS: " + (float)result[0]/result[1];
		System.out.println(print);
		java.io.PrintWriter out = FileUtil.getPrintWriter(outputFile);
		out.println(print);

	}
	
	public static void DepEvalUlab(File gold, File test, File outputFile) {
		Scanner testScan = FileUtil.getScanner(test);
		Scanner goldScan = FileUtil.getScanner(gold);
		int line = 0;
		int sentences = 0;
		int[] result = new int[]{0,0};
		Hashtable<String, int[]> categoryUas = new Hashtable<String, int[]>(); 
		
		while(testScan.hasNextLine()) {
			line++;
			String lexLineTest = testScan.nextLine();
			goldScan.nextLine(); //lexLineGold
			if (lexLineTest.isEmpty()) {
				sentences++;
				continue;
			}
			testScan.nextLine(); // String posLineTest
			String posLineGold = goldScan.nextLine(); //
			String indexLineTest = testScan.nextLine();
			String indexLineGold = goldScan.nextLine();
			String[] indexTestLineSplit = indexLineTest.split("\t");
			String[] indexGoldLineSplit = indexLineGold.split("\t");
			String[] posGoldLineSplit = posLineGold.split("\t");
			/*if (!testLineSplit[0].equals(goldLineSplit[0])) {
				System.err.println("Lexincon unmatch in line " + line + testLineSplit[0]+"-"+goldLineSplit[0]);
				return;
			}*/
			for(int i=0; i<indexTestLineSplit.length; i++) {
				if (indexTestLineSplit[i].equals(indexGoldLineSplit[i])) {
					result[0]++;
					addCategoryScore(categoryUas, posGoldLineSplit[i],1,1);
				}
				else addCategoryScore(categoryUas, posGoldLineSplit[i],0,1);
			}			
			result[1]+=indexTestLineSplit.length;
		}
		String print = "Number of sentences: " + sentences + "\n" +
		"Total Tokens: " + result[1] + "\n" +
		"Correct attachments: " + result[0] + "\n" +
		"UAS: " + (float)result[0]/result[1];
		System.out.println(print);
		java.io.PrintWriter out = FileUtil.getPrintWriter(outputFile);
		out.println(print);
		out.println("\nCategory UAS report:");
		for(Entry<String, int[]> e : categoryUas.entrySet()) {
			int[] value = e.getValue();
			out.println(e.getKey() + "\t" + value[0] + "\t" + value[1]);
		}
		out.close();
	}
	
	private static void addCategoryScore(Hashtable<String, int[]> categoryUas,
			String key, int correctToAdd, int totalToAdd) {
		int[] value = categoryUas.get(key);
		if (value==null) {
			value = new int[]{correctToAdd, totalToAdd};
			categoryUas.put(key, value);
		}
		else {
			value[0] += correctToAdd;
			value[1] += totalToAdd;
		}		
	}

	public static void MSTevalUAS(File test, File gold) {
		Scanner testScan = FileUtil.getScanner(test);
		Scanner goldScan = FileUtil.getScanner(gold);
		int line = 0;
		int sentences = 0;
		int[] result = new int[]{0,0};
		
		while(testScan.hasNextLine()) {
			line++;
			testScan.nextLine();
			String goldLineLex = goldScan.nextLine();
			if (goldLineLex.length()==0) continue;
			testScan.nextLine();
			goldScan.nextLine();
			String testLineIndexes = testScan.nextLine();
			String goldLineIndexes = goldScan.nextLine();			
			int[] testIndexes = Utility.parseIndexList(testLineIndexes.split("\t"));
			int[] goldIndexes = Utility.parseIndexList(goldLineIndexes.split("\t"));
			result[1] += testIndexes.length;
			for(int i=0; i<testIndexes.length; i++) {
				if (testIndexes[i] == goldIndexes[i]) result[0]++;
			}
			
		}
		String print = "Number of sentences: " + sentences + "\n" +
		"Total Tokens: " + result[1] + "\n" +
		"Correct attachments: " + result[0] + "\n" +
		"UAS: " + (float)result[0]/result[1];
		System.out.println(print);
	}
}
