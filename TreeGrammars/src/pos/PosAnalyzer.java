package pos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

import util.PrintProgress;
import util.Utility;

public class PosAnalyzer {
	
	static final int printWordPerPosLowHigh = 5;
	static final boolean detailedAnalysis = true;
	HashMap<String, int[]> posFreq = new HashMap<String, int[]>();
	HashMap<String, HashMap<String, int[]>> posWordFreq = new HashMap<String, HashMap<String,int[]>>();
	int wordCount, sentenceCount;
	
	public PosAnalyzer(File inputFile, File outputFile, boolean coarsePos) throws IOException {
		String inputStr = inputFile.toString().toLowerCase(); 
		if (inputStr.contains("conll")) {
			if (inputStr.endsWith(".gz"))
				readConllCompressed(inputFile, coarsePos);
			else
				readConll(inputFile, coarsePos);
		}
		else
			readWordTabPosFile(inputFile);
		printPosStats(outputFile);
	}

	public void readConll(File inputFile, 
			boolean coarsePos) throws FileNotFoundException {
		System.out.println("Reading Conll File");
		PrintProgress progress = new PrintProgress("Reading input file line ", 100, 0);
		Scanner scan = new Scanner(inputFile);
		while(scan.hasNextLine()) {
			progress.next();
			String line = scan.nextLine();
			if (line.isEmpty()) {
				sentenceCount++;
				continue;
			}
			if (line.startsWith("#")) {
				// comment line
				continue;
			}
			String[] tabs = line.split("\t");
			//0: index
			//1: word
			//2: lemma
			//3: pos_coarse
			//4: pos_fine
			//5: morph
			//6: head
			//7: arclabel
			//8: ?
			//9: ?
			wordCount++;
			String word = tabs[1];
			String pos = coarsePos ? tabs[3] : tabs[4];
			word = word.intern();
			pos = pos.intern();
			Utility.increaseInHashMap(posFreq, pos);
			if (detailedAnalysis)
				Utility.increaseInHashMap(posWordFreq, pos, word);
		}		
		progress.end();
		System.out.println("Successfully processed " + wordCount + " words and " + sentenceCount + " sentences.");		
		
	}
	
	public void readConllCompressed(File inputFile, boolean coarsePos) throws IOException {
		System.out.println("Reading Conll Compressed File");
		InputStream fileStream = new FileInputStream(inputFile);
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		
		String line = null;
		
		PrintProgress progress = new PrintProgress("Reading input file line ", 1000, 0);
		String word=null, pos=null;
		while((line = buffered.readLine()) != null) {
			progress.next();
			if (line.isEmpty()) {
				sentenceCount++;
				continue;
			}
			if (line.startsWith("#") || line.startsWith("<")) {
				// comment line
				continue;
			}
			String[] tabs = line.split("\t");
			//0: index
			//1: word
			//2: lemma
			//3: pos_coarse
			//4: pos_fine
			//5: morph
			//6: head
			//7: arclabel
			//8: ?
			//9: ?
			wordCount++;
			word = tabs[1];
			pos = coarsePos ? tabs[3] : tabs[4];
			//word = word.intern();
			//pos = pos.intern();
			Utility.increaseInHashMap(posFreq, pos);
			if (detailedAnalysis)
				Utility.increaseInHashMap(posWordFreq, pos, word);
		}		
		progress.end();
		System.out.println("Successfully processed " + wordCount + " words and " + sentenceCount + " sentences.");		
		buffered.close();
	}
	
	public void readWordTabPosFile(File inputFile) throws FileNotFoundException {
		System.out.println("Reading Word tab Pos File");
		Scanner scan = new Scanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) {
				sentenceCount++;
				continue;
			}
			String[] tabs = line.split("\t");
			wordCount++;
			String word = tabs[0];
			String pos = tabs[1];
			word = word.intern();
			pos = pos.intern();
			Utility.increaseInHashMap(posFreq, pos);
			if (detailedAnalysis)
				Utility.increaseInHashMap(posWordFreq, pos, word);
		}		
		System.out.println("Successfully processed " + wordCount + " words and " + sentenceCount + " sentences.");		
		
	}
	
	public void printPosStats(File outputFile) throws IOException {
		PrintWriter pw = new PrintWriter(outputFile);
		printPosStats(pw);
		pw.close();
	}
	
	public void printPosStats() throws IOException {
		printPosStats(System.out);
	}
	
	public void printPosStats(Appendable out) throws IOException {
		out.append("Successfully processed " + wordCount + " words and " + sentenceCount + " sentences.\n\n");
		
		HashMap<String, Integer> posFreqIntTable = Utility.convertHashMapIntArrayInteger(posFreq);		
		TreeMap<Integer, HashSet<String>> posFreqIntTableSorted = Utility.reverseAndSortTable(posFreqIntTable);
		
		out.append("Total number of pos " + posFreq.size() + "\n");
		ArrayList<String> posOrder = new ArrayList<String>(); 
		for(HashSet<String> s : posFreqIntTableSorted.descendingMap().values()) {
			posOrder.addAll(s);
		}
		out.append(posOrder + "\n");
		
		if (!detailedAnalysis)
			return;
		
		for(Entry<Integer, HashSet<String>> e : posFreqIntTableSorted.descendingMap().entrySet()) {
			for(String pos : e.getValue()) {
				HashMap<String, int[]> wordFreq = posWordFreq.get(pos);
				int words = wordFreq.size();
				out.append(pos + "\t(" + e.getKey() + "," + words + ")\n");
				if (words<=2*printWordPerPosLowHigh)
					printSampleTable(wordFreq, false, out);
				else {
					printSampleTable(wordFreq, false, out);
					out.append("\t...\t...\n"); 
					printSampleTable(wordFreq, true, out);
				}
			}
			
		}
	}
	

	private static void printSampleTable(HashMap<String, int[]> wordFreq, 
			boolean reverseOrder, Appendable out) throws IOException {
		
		
		HashMap<String, Integer> wordFreqIntTable = Utility.convertHashMapIntArrayInteger(wordFreq);
		TreeMap<Integer, HashSet<String>> wordFreqIntTableSorted = Utility.reverseAndSortTable(wordFreqIntTable);
		
		Set<Entry<Integer, HashSet<String>>> entrySet = reverseOrder ? 
				wordFreqIntTableSorted.descendingMap().entrySet() : wordFreqIntTableSorted.entrySet();
		
		int count = 0;
		StringBuilder sb = new StringBuilder();
		outer: for(Entry<Integer, HashSet<String>> f : entrySet) {			
			for(String w : f.getValue()) {
				String line = "\t" + f.getKey() + "\t" + w + "\n";
				if (reverseOrder)
					sb.insert(0, line);
				else
					sb.append(line);
				if (++count==printWordPerPosLowHigh)
					break outer;
			}					
		}
		out.append(sb.toString());
	}

	public static void main(String[] args) throws IOException {
		
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		boolean coarsePos = Boolean.parseBoolean(args[2]);
		new PosAnalyzer(inputFile, outputFile, coarsePos);
		
		/*
		//ISDT
		File inputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL.conll");
		//File outputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL_posStat_coarse.txt");
		File outputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/ISDT_1.0/ALL_posStat_fine.txt");
		PosAnalyzer PA = new PosAnalyzer(true);
		PA.readConll(inputFile, false);
		PA.printPosStats(outputFile);
		*/
		
		/*		
		//EVALITA 2009
		File inputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/Evalita2009/train");
		File outputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/Evalita2009/train_posStat.txt");
		PosAnalyzer PA = new PosAnalyzer(true);
		PA.readWordTabPosFile(inputFile);
		PA.printPosStats(outputFile);
		*/
		
		/*
		//TUT 2009		
		File inputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/TUT/Conl/ALL.conl");
		//File outputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/TUT/Conl/ALL_posStat_coarse.txt");
		File outputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/TUT/Conl/ALL_posStat_fine.txt");
		PosAnalyzer PA = new PosAnalyzer(true);
		PA.readConll(inputFile, false);
		PA.printPosStats(outputFile);
		*/
		
		
		//PAISÀ		
		/*
		File inputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/paisa.annotated.CoNLL.utf8.gz");
		File outputFile = new File("/Volumes/HardDisk/Scratch/CORPORA/Italian/paisa.posStats.coarse");
		PosAnalyzer PA = new PosAnalyzer(false);
		PA.readConllCompressed(inputFile, true);
		PA.printPosStats(outputFile);
		*/
		
		
	}
	
}
