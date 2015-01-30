package tsg.fragStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import org.apache.tools.bzip2.CBZip2InputStream;

import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class FragmentFileStatistics {

	static boolean read2bytes = true;
	static int absoluteMax = 100;
	static int printProgressEvery = 100000;
	
	final int[] freqBins = new int[]{1,10,100,Integer.MAX_VALUE};	
	final int freqBinsNumbers = freqBins.length;
	
	int empiricalMaxDepth, empiricalMaxBranching, empiricalMaxWords, empiricalMaxSubSites, 
		empiricalMaxFronteerNodes;
	File fragmentFile, outputFile;	
	long[][] maxDepthTypesTokens, maxBranchingTypesTokens, wordsTypesTokens, subSitesTypesTokens,
		fronteerNodesTypesTokens;	
	PrintProgress progress;
	
	Hashtable<Integer, long[]> freqTable = new Hashtable<Integer, long[]>();
	HashMap<Integer, int[]> depthBinTable = new HashMap<Integer, int[]>();
	
	public FragmentFileStatistics(File fragmentFile,
		File outputFile, boolean compressed) throws Exception {
		
		this.fragmentFile = fragmentFile;
		this.outputFile = outputFile;
		
		maxDepthTypesTokens = new long[absoluteMax][2];
		maxBranchingTypesTokens = new long[absoluteMax][2];
		wordsTypesTokens = new long[absoluteMax][2];
		subSitesTypesTokens = new long[absoluteMax][2];
		fronteerNodesTypesTokens = new long[absoluteMax][2];
		
		progress = new PrintProgress("Reading fragments", printProgressEvery, 0);
		if (compressed) readFragemntFileCompressed();
		else readFragemntFile();
		progress.end();
		writeStats();
	}
	
	private void analyzeNextLine(String line) throws Exception {
		progress.next();		
		String[] lineSplit = line.split("\t");
		TSNodeLabel fragment = new TSNodeLabel(lineSplit[0], false);
		int freq = Integer.parseInt(lineSplit[1]);
		int maxDepth = fragment.maxDepth();
		int maxBranching = fragment.maxBranching();
		int words = fragment.countLexicalNodes();
		int subSites = fragment.countNonLexicalFronteer();
		int fronteerNodes = fragment.countTerminalNodes();
		if (maxDepth>empiricalMaxDepth) empiricalMaxDepth = maxDepth;
		if (maxBranching>empiricalMaxBranching) empiricalMaxBranching = maxBranching;
		if (words>empiricalMaxWords) empiricalMaxWords = words;
		if (subSites>empiricalMaxSubSites) empiricalMaxSubSites = subSites;
		if (fronteerNodes>empiricalMaxFronteerNodes) empiricalMaxFronteerNodes = fronteerNodes;
		
		if (fronteerNodes<words) {
			System.out.println(line);
			System.out.println("Words: " + words);
			System.out.println("Fronteers Nodes: " + fronteerNodes);			
		}
		
		maxDepthTypesTokens[maxDepth][0] ++;
		maxDepthTypesTokens[maxDepth][1] += freq;
		maxBranchingTypesTokens[maxBranching][0] ++;
		maxBranchingTypesTokens[maxBranching][1] += freq;
		wordsTypesTokens[words][0]++;
		wordsTypesTokens[words][1] += freq;
		subSitesTypesTokens[subSites][0]++;
		subSitesTypesTokens[subSites][1] += freq;
		fronteerNodesTypesTokens[fronteerNodes][0]++;
		fronteerNodesTypesTokens[fronteerNodes][1] += freq;		
		
		Utility.increaseInTableLong(freqTable, freq);
		increaseInDepthFreqBinTable(maxDepth, freq);
	}	

	private void increaseInDepthFreqBinTable(int maxDepth, int freq) {
		int[] freqBin = depthBinTable.get(maxDepth);
		if (freqBin==null) {
			freqBin = new int[freqBinsNumbers];
			depthBinTable.put(maxDepth, freqBin);
		}
		int binIndex;
		for(binIndex = 0; binIndex<freqBinsNumbers; binIndex++) {
			if (freq<=freqBins[binIndex])
				break;
		}
		freqBin[binIndex]++;		
	}

	private void printDepthFreqBinTable(PrintWriter pw) {
		pw.println("Max Deph - Frequency Bin - Table");
		pw.print("Max_Deph");
		for(int i=0; i<freqBinsNumbers; i++) {
			pw.print("\t<=" + freqBins[i]);
		}
		pw.println();
		TreeSet<Integer> depths = new TreeSet<Integer>(depthBinTable.keySet());
		for(int d : depths) {
			pw.print(d);
			int[] freqBin = depthBinTable.get(d);
			for(int i=0; i<freqBinsNumbers; i++) {
				pw.print("\t" + freqBin[i]);
			}
			pw.println();
		}
		pw.println();
	}

	private void readFragemntFile() throws Exception {
						
		Scanner scan = FileUtil.getScanner(fragmentFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			analyzeNextLine(line);
		}		

		scan.close();		
				
	}
	
	private void readFragemntFileCompressed() throws Exception {
		
		FileInputStream inputStream = new FileInputStream(fragmentFile);
		if (read2bytes) {
			inputStream.read();
			inputStream.read();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new CBZip2InputStream(inputStream),"UTF-8"));		
		
		while(true) {
			String line = reader.readLine();
			if (line==null) break;
			analyzeNextLine(line);
		}		

		reader.close();		
				
	}
	
	private void writeStats() {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		pw.println("Frequency" + "\t" + "Frag. Types");
		TreeSet<Integer> freqSet = new TreeSet<Integer>(freqTable.keySet());
		Iterator<Integer> iter = freqSet.descendingIterator();		
		while(iter.hasNext()) {
			Integer freq = iter.next();
			long types = freqTable.get(freq)[0];
			pw.println(freq + "\t" + types);
		}
		
		pw.println();
		pw.println("MaxDepth in Fragments" + "\t" + "Types" + "\t" + "Tokens");
		printTable(pw,maxDepthTypesTokens,empiricalMaxDepth);
		
		pw.println();
		pw.println("MaxBranching in Fragments" + "\t" + "Types" + "\t" + "Tokens");
		printTable(pw,maxBranchingTypesTokens,empiricalMaxBranching);
		
		pw.println();
		pw.println("Words in Fragments" + "\t" + "Types" + "\t" + "Tokens");
		printTable(pw,wordsTypesTokens,empiricalMaxWords);
		
		pw.println();
		pw.println("Substitution Sites in Fragments" + "\t" + "Types" + "\t" + "Tokens");
		printTable(pw,subSitesTypesTokens,empiricalMaxSubSites);
		
		pw.println();
		pw.println("Fronteer Nodes in Fragments" + "\t" + "Types" + "\t" + "Tokens");
		printTable(pw,fronteerNodesTypesTokens,empiricalMaxFronteerNodes);
		
		
		absoluteMax = Utility.max(new int[]{empiricalMaxDepth, empiricalMaxBranching, 
				empiricalMaxWords, empiricalMaxSubSites, empiricalMaxFronteerNodes});
			
		pw.println();
		pw.println("Summary:" + "\n" + 
				"Count" + "\t" +
				"Depth" + "\t" +
				"Branch." + "\t" +
				"Words" + "\t" +
				"SubSit" + "\t" +
				"Front.");
		for(int i=0; i<=absoluteMax; i++) {
			pw.print(i);
			printTableEntry(pw, maxDepthTypesTokens, i, empiricalMaxDepth);
			printTableEntry(pw, maxBranchingTypesTokens, i, empiricalMaxBranching);
			printTableEntry(pw, wordsTypesTokens, i, empiricalMaxWords);
			printTableEntry(pw, subSitesTypesTokens, i, empiricalMaxSubSites);
			printTableEntry(pw, fronteerNodesTypesTokens, i, empiricalMaxFronteerNodes);
			pw.println();			
		}
		
		pw.println();
		printDepthFreqBinTable(pw);
		
		pw.println();
		pw.close();
	}
	
	private static void printTable(PrintWriter pw, long[][] table, int max) {
		for(int i=0; i<=max; i++) {
			long[] val = table[i];
			pw.println(i + "\t" + val[0] + "\t" + val[1]);
		}
	}
	
	private static void printTableEntry(PrintWriter pw, long[][] table, int i, int max) {
		if (i<=max)
			pw.print("\t" + table[i][0]);
		else 
			pw.print("\t" + 0);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		File fragmentFile = new File(args[0]);
		File outputFile = new File(args[1]);
		boolean compressed = ArgumentReader.readBooleanOption(args[2]);
		
		new FragmentFileStatistics(fragmentFile, outputFile, compressed);
	}	

}
