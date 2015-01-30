package ccg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.TreeSet;

import tsg.corpora.Wsj;
import util.Utility;
import util.file.FileUtil;

public class CCGindexes {	
	
	public static boolean equalSentences(String wsjSentence, String ccgSentence) {
		ArrayList<String> wsjWords = new ArrayList<String>(Arrays.asList(wsjSentence.split("\\s")));
		ArrayList<String> ccgWords = new ArrayList<String>(Arrays.asList(ccgSentence.split("\\s")));
		
		ListIterator<String> wsjWordsIter = wsjWords.listIterator();		
		while(wsjWordsIter.hasNext()) {
			String wsjW = wsjWordsIter.next();
			if (wsjW.equals("''") || wsjW.equals("``") || wsjW.equals("'") || wsjW.equals("`")) {
				wsjWordsIter.remove();
			}			 
		}
		
		ListIterator<String> ccgWordsIter = ccgWords.listIterator();
		while(ccgWordsIter.hasNext()) {
			String ccgW = ccgWordsIter.next();
			if (ccgW.equals("''") || ccgW.equals("``") || ccgW.equals("'") || ccgW.equals("`")) {
				ccgWordsIter.remove();
			}		
			if (ccgW.equals("K-H")) {
				ccgWordsIter.remove();
				ccgWordsIter.add("K");
			}
		}
		
		if (ccgWords.size()!=wsjWords.size()) return false;
		ccgWordsIter = ccgWords.listIterator();
		for(String wsjW : wsjWords) {
			String ccgW = ccgWordsIter.next();
			if (!wsjW.equals(ccgW)) return false;
		}		
		return true;
	}
	
	public static void findIndexes() {
		File ccgRaw = new File("/scratch/fsangati/CORPUS/ccgbank_1_1/data/RAW/CCGbank.00-24.raw");
		File wsjRaw = new File("/scratch/fsangati/CORPUS/WSJ/FLAT_NOTRACES/wsj-00-24.mrg");
		
		Scanner ccgScan = FileUtil.getScanner(ccgRaw);
		Scanner wsjScan = FileUtil.getScanner(wsjRaw);
		
		ArrayList<Integer> indexes = new ArrayList<Integer>(); 
		
		int wsjIndex = -1;
		while(ccgScan.hasNextLine() && wsjScan.hasNextLine()) {
			String ccgNext = ccgScan.nextLine();
			String wsjNext = wsjScan.nextLine();
			wsjIndex++;
			if (equalSentences(wsjNext, ccgNext)) {
				indexes.add(wsjIndex);
			}
			else {
				while(wsjScan.hasNextLine()) {
					wsjNext = wsjScan.nextLine();
					wsjIndex++;
					if (equalSentences(wsjNext, ccgNext)) {
						indexes.add(wsjIndex);
						break;
					}
				}
			}			
		}
		
		System.out.println(indexes);
		System.out.println(indexes.size());
	}
	
	public static int[] getCcgIndexes() {
		File ccgIndexes = new File("/scratch/fsangati/CORPUS/ccgbank_1_1/wsjIndexes.txt");
		Scanner scan = FileUtil.getScanner(ccgIndexes);
		String indexLine = scan.nextLine();
		String[] indexesString = indexLine.split(",\\s");
		//System.out.println(indexesString.length);
		int[] indexes = new int[indexesString.length];
		for(int i=0; i<indexesString.length; i++) {
			indexes[i] = Integer.parseInt(indexesString[i]);
		}
		//System.out.println(Arrays.toString(indexes));
		return indexes;
	}
	
	private static void getFlatSentencesCCG() {
		int[] indexes = getCcgIndexes();
		int length = indexes.length;
		File wsj00Flat = new File(Wsj.WsjFlatNoTraces + "wsj-00.mrg");
		File outputFile = new File("/scratch/fsangati/CORPUS/ccgbank_1_1/wsj00ccgFlat.txt");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner scan = FileUtil.getScanner(wsj00Flat);
		int lineNumber = 0;
		int i = 0;
		int nextIndex = indexes[i++];
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (lineNumber==nextIndex) {
				pw.println(line);
				if (i!=length) {
					nextIndex = indexes[i++];
				}
			}
			lineNumber++;			
		}
		pw.close();		
	}
	
	private static TreeSet<Integer> getAmbiguityIndexes() {
		File inputFile = new File("/Users/fedja/Work/Code/TreeGrammars/Viewers/TDS/TDSvsCCG/TDS_origNPbrack_vs_CCG_Report.txt");
		Scanner scanner = FileUtil.getScanner(inputFile);
		TreeSet<Integer> result = new TreeSet<Integer>();
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (!line.matches("\\d+\\:.*")) {
				continue;
			}
			int colonIndex = line.indexOf(':');
			String indexString = line.substring(0,colonIndex);
			int index = Integer.parseInt(indexString);
			result.add(index);
		}
		//System.out.println(result);
		return result;
	}
	
	private static void getFlatSentencesCCGAmbiguity() {
		TreeSet<Integer> indexes = getAmbiguityIndexes();
		File flatFile = new File("/scratch/fsangati/CORPUS/ccgbank_1_1/wsj00ccgFlat.txt");
		File outputFile = new File("/Users/fedja/Work/Code/TreeGrammars/Viewers/TDS/TDSvsCCG/ambiguitySentences.txt");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner scan = FileUtil.getScanner(flatFile);
		int lineNumber = 1;
		Iterator<Integer> indexIter = indexes.iterator();
		int nextIndex = indexIter.next();
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (lineNumber==nextIndex) {
				pw.println(nextIndex + ": " + line);
				if (!indexIter.hasNext()) {
					break;
				}
				nextIndex = indexIter.next();
			}
			lineNumber++;			
		}
		pw.close();
		
	}
	
	public static TreeSet<Integer> getComplementary(int[] indexes, int lastIndex) {		
		TreeSet<Integer> result = new TreeSet<Integer>();
		int i=0;
		int expectedNumber = 1;
		while(expectedNumber<=lastIndex) {
			if (i>=indexes.length || indexes[i]!=expectedNumber) {
				result.add(expectedNumber);
			}
			else {
				i++;
			}
			expectedNumber++;
		}
		return result;
	}
	
	public static void main(String[] args) {
		//getFlatSentencesCCG();
		//getAmbiguityIndexes();
		//getFlatSentencesCCGAmbiguity();
		
		int[] indexes = getCcgIndexes();
		Utility.addToAll(indexes, 1);
		TreeSet<Integer> complIndexes = getComplementary(indexes, 1921);
		System.out.println(complIndexes);
	}

	

	

	
		
}
