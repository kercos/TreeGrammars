package depParsing;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import util.*;
import util.file.FileUtil;
import tdg.*;

public class DepEval {

	static enum Mode {
		CONLLX, MST
	}

	int total;
	int correctLab;
	int correctUlab;
	Mode mode;
	TreeMap<String, int[]> posTotal = new TreeMap<String, int[]>();
	TreeMap<String, int[]> posCorrectLab = new TreeMap<String, int[]>();
	TreeMap<String, int[]> posCorrectUlab = new TreeMap<String, int[]>();

	public DepEval(File gold, File test, Mode m) {
		this.mode = m;
		switch(mode) {
			case CONLLX:
				evalCoNLLX(gold, test);
				break;
			case MST:
				evalMST(gold, test);
				break;
		}
		printResults();
	}
	
	private void printResults() {
		System.out.println("Total:\t" + total);
		System.out.println("Correct LAs:\t" + correctLab);
		System.out.println("Correct UAs:\t" + correctUlab);
		
		System.out.println("LAS Accuracy PoS:\t" + ((double)correctLab)/total*100);
		System.out.println("UAS Accuracy PoS:\t" + ((double)correctUlab)/total*100);

		System.out.println();
		System.out.println("Pos\tTotal\tCorrectLAS\t%");
		for(Entry<String, int[]> e : posTotal.entrySet()) {
			String pos = e.getKey();
			if (pos.equals("SYM") || pos.equals("X"))
				continue;
			int tot = e.getValue()[0];
			int[] v = posCorrectLab.get(pos);			
			int freq = v == null? 0 : v[0];			
			System.out.println(pos + "\t" + tot + "\t" + freq + "\t" + ((double)freq)/tot*100);
			//System.out.println(pos + "\t" + ((double)freq)/tot*100);
		}
		
		System.out.println();
		System.out.println("Pos\tTotal\tCorrectUAS\t%");
		for(Entry<String, int[]> e : posTotal.entrySet()) {
			String pos = e.getKey();
			if (pos.equals("SYM") || pos.equals("X"))
				continue;
			int tot = e.getValue()[0];
			int[] v = posCorrectUlab.get(pos);
			int freq = v == null? 0 : v[0];			
			System.out.println(pos + "\t" + tot + "\t" + freq + "\t" + ((double)freq)/tot*100);
			//System.out.println(pos + "\t" + ((double)freq)/tot*100);
		}
	}

	private void evalCoNLLX(File gold, File test)  {		
		Scanner scanGold=null, scanTest=null;
		try {
			scanGold = new Scanner(gold);
			scanTest = new Scanner(test);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		while(scanTest.hasNextLine()) {
			String testLine = scanTest.nextLine();
			String testGold = scanGold.nextLine();
			//0: index, 1: word, 2: lemma, 3: pos_coarse, 4: pos_fine, 5: morph, 6: head, 7: arclabel
			if (testLine.isEmpty())
				continue;
			String[] tls = testLine.split("\t");
			String[] gls = testGold.split("\t");
			//if (!tls[0].equals(gls[0])
			String goldPoS = gls[3];
			if (!tls[3].equals("_") && !tls[3].equals(gls[3])) {
				System.err.println("wp missmatch");
			}
			total++;
			Utility.increaseInHashMap(posTotal, goldPoS);			
			if (tls[6].equals(gls[6])) { //head
				correctUlab++;
				Utility.increaseInHashMap(posCorrectUlab, goldPoS);
				if (tls[7].equals(gls[7])) { //label
					correctLab++;
					Utility.increaseInHashMap(posCorrectLab, goldPoS);
				}					
			}
		}
		scanGold.close();
		scanTest.close();
		
	}
	
	private void evalMST(File gold, File test)  {		
		Scanner scanGold=null, scanTest=null;
		try {
			scanGold = new Scanner(gold);
			scanTest = new Scanner(test);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		while(scanTest.hasNextLine()) {
			String wordsTest = scanTest.nextLine(); //words
			String wordsGold = scanGold.nextLine(); //words
			if (wordsGold.isEmpty())
				continue;
			String[] posTest = scanTest.nextLine().split("\t"); //pos
			String[] posGold = scanGold.nextLine().split("\t"); //pos
			String[] labelsTest = scanTest.nextLine().split("\t"); //heads
			String[] labelsGold = scanGold.nextLine().split("\t"); //heads
			String[] headsTest = scanTest.nextLine().split("\t"); //heads
			String[] headsGold = scanGold.nextLine().split("\t"); //heads
			
			
			for(int i=0; i<posGold.length; i++) {
				total++;
				String goldPoS = posGold[i];
				String goldH = headsGold[i];
				String testH = headsTest[i];
				String goldL = labelsGold[i];
				String testL = labelsTest[i];
				Utility.increaseInHashMap(posTotal, goldPoS);
				if (goldH.equals(testH)) { //head
					correctUlab++;
					Utility.increaseInHashMap(posCorrectUlab, goldPoS);
					if (goldL.equals(testL)) { //label
						correctLab++;
						Utility.increaseInHashMap(posCorrectLab, goldPoS);
					}					
				}
			}
			
		}
		scanGold.close();
		scanTest.close();
		
	}

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
	
	public static void main(String[] args) {
		String root = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/may_15/";
		//File gold = new File(root + "data/it-ud-test.wphl.conllx");
		File gold = new File(root + "mst/it-ud-test.lab");
		//File test = new File(root + "desr/it-ud-test.wphl.DESR.conllx");
		//File test = new File(root + "malt/it-ud-test.wphl.MALT.conllx");		
		//File test = new File(root + "mst/it-ud-test.wphl.MST.conllx");
		//File test = new File(root + "stanford/dep_parser/it-ud-test.wphl.STANFORD.conllx");
		File test = new File(root + "mst/it-ud-test.MST.lab");
		//new DepEval(gold, test, Mode.CONLLX);
		new DepEval(gold, test, Mode.MST);
	}
}
