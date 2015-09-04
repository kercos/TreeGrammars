package pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

import util.Utility;

public class PosEval {

	static enum Mode {
		STANFORD, TREETAGGER
	}

	int total;
	int correctPos;
	int correctLemmas;
	Mode mode;
	TreeMap<String, int[]> posTotal = new TreeMap<String, int[]>();
	TreeMap<String, int[]> posCorrectPos = new TreeMap<String, int[]>();
	TreeMap<String, int[]> posCorrectLemma = new TreeMap<String, int[]>();

	public PosEval(File goldFile, File testFile, Mode m) throws FileNotFoundException {
		this.mode = m;
		Scanner scanGold = new Scanner(goldFile);
		Scanner scanTest = new Scanner(testFile);
		int lineNum = 0;
		while(scanGold.hasNextLine() || scanTest.hasNextLine()) {
			lineNum++;
			String goldLine = scanGold.hasNextLine() ? scanGold.nextLine() : null;
			String testLine = scanTest.hasNextLine() ? scanTest.nextLine() : null;
			if (goldLine==null || testLine==null) {
				System.err.println("Gold and test file don't match in number of lines!");
				break;
			}
	        switch (mode) {
	        	case STANFORD:
	        		scoreStanford(goldLine, testLine, lineNum);
	        		break;
	        	case TREETAGGER:
	        		scoreTreeTagger(goldLine, testLine, lineNum);
	        		break;
	        }
		}
		printResults();
		scanGold.close();
		scanTest.close();		
	}
	
	private void printResults() {
		System.out.println("Total:\t" + total);
		System.out.println("Correct PoS:\t" + correctPos);
		
		System.out.println("Accuracy PoS:\t" + ((double)correctPos)/total);

		System.out.println();
		System.out.println("Pos\tTotal\tCorrectPos\t%");
		for(Entry<String, int[]> e : posTotal.entrySet()) {
			String pos = e.getKey();
			int tot = e.getValue()[0];
			int[] v = posCorrectPos.get(pos);
			int freq = v == null? 0 : v[0];			
			System.out.println(pos + "\t" + tot + "\t" + freq + "\t" + ((double)freq)/tot);
		}

		System.out.println();
		if (!posCorrectLemma.isEmpty()) {
			System.out.println("Correct Lemmas:\t" + correctLemmas);
			System.out.println("Accuracy Lemma:\t" + ((double)correctLemmas)/total);
			System.out.println("Pos\tTotal\tCorrectLemma\t%");
			for(Entry<String, int[]> e : posTotal.entrySet()) {
				String pos = e.getKey();
				int tot = e.getValue()[0];
				int[] v = posCorrectLemma.get(pos);
				int freq = v == null? 0 : v[0];
				System.out.println(pos + "\t" + tot + "\t" + freq + "\t" + ((double)freq)/tot);
			}
		}
		
	}

	private void scoreTreeTagger(String goldLine, String testLine, int lineNum) {
		String[] goldSplit = goldLine.split("\t");
		String goldWord = goldSplit[0];
		String goldPos = goldSplit[1];
		String goldLemma = goldSplit[2];
		String[] testSplit = testLine.split("\t");
		String testWord = testSplit[0];
		String testPos = testSplit[1];
		String testLemma = testSplit[2];
		if (!goldWord.equals(testWord)) {
			System.err.println("Gold and test words don't match in line " + lineNum);
			return;
		}
		if (goldWord.equals("_EOS_"))
			return;
		total ++;
		Utility.increaseInHashMap(posTotal, goldPos);
		if (goldPos.equals(testPos)) {
			correctPos++;
			Utility.increaseInHashMap(posCorrectPos, goldPos);
		}
		if (goldLemma.equalsIgnoreCase(testLemma)) {
			correctLemmas++;
			Utility.increaseInHashMap(posCorrectLemma, goldPos);
		}
		
	}

	private void scoreStanford(String goldLine, String testLine, int lineNum) {
		String[] goldPos = getPosStanford(goldLine);
		String[] testPos = getPosStanford(testLine);
		if (goldPos.length != testPos.length) {
			System.err.println("Sentence in two files differs in length in line: " + lineNum);
			System.err.println("goldLine: " + goldLine);
			System.err.println("testLine: " + testLine);
			return;
		}
		total += goldPos.length;
		for(int i=0; i<goldPos.length; i++) {
			Utility.increaseInHashMap(posTotal, goldPos[i]);
			if (goldPos[i].equals(testPos[i])) {
				correctPos++;
				Utility.increaseInHashMap(posCorrectPos, goldPos[i]);
			}
		}
		
	}

	private static String[] getPosStanford(String line) {
		String[] wordPos = line.split("\\s");
		int length = wordPos.length;
		String[] result = new String[length];
		for(int i=0; i<length; i++) {
			String w = wordPos[i];
			result[i] = w.substring(w.indexOf('_')+1);
		}
		return result;
	}

	public static void main(String args[]) throws FileNotFoundException {
		
		/*
		String root = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";
		args = new String[]{
			root + "it-ud-dev.stanfordCoarsePoS.test",
			root + "it-ud-dev.stanfordCoarsePoS.gold"
		};
		*/
		Mode m = (args[0].toLowerCase().contains("stanford")) ? Mode.STANFORD : Mode.TREETAGGER;
		new PosEval(new File(args[1]),new File(args[2]), m);
	}
}
