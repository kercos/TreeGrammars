package ngram;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import ngram.NGramSyntax.NGram;
import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class NGramSyntax {

	int n;
	int freqThreshold; // removing those < threashold
	
	String workingDir;
	HashMap<NGram, int[]> nGramTable;
	HashMap<NGram, ArrayList<IndexBlipp87>> nGramIndexTable;
	HashMap<NGram, HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>> nGramSyntaxTable;
	HashMap<NGram, Double> nGramSyntaxEntropyTable;
	TreeMap<Short, TreeMap<Short, HashSet<Short>>> inverseIndexedTablePrevious; 
	
	static class NGram {
		
		Label[] ngram;
		
		public NGram(Label[] ngram) {
			this.ngram = ngram;
		}
		
		public int hashCode() {
			return Arrays.hashCode(ngram);			
		}
		
		public boolean equals(Object o) {
			if (o instanceof NGram) {
				NGram otherNGram = (NGram)o;
				return Arrays.equals(ngram, otherNGram.ngram);
			}
			return false;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(ngram[0]);
			for(int i=1; i<ngram.length; i++) {
				sb.append(" " + ngram[i]);
			}
			
			return sb.toString();
		}
		
	}


	public NGramSyntax(int n, int freqThreshold, 
			HashMap<NGram, ArrayList<IndexBlipp87>> previousNGramIndexTable, 
			String workingDir, boolean buildIndex) throws Exception {
		
		this.n = n;
		this.freqThreshold = freqThreshold;
		
		this.workingDir = workingDir;
		if (this.workingDir.endsWith("/"))
			this.workingDir += "/";
		File logFile = new File(workingDir + "NGramSyntax_" + n + ".log");
		Parameters.openLogFile(logFile);		
		
		printParameters();
		
		if (previousNGramIndexTable!=null) {
			Parameters.reportLine("Inverting previous indexed table.");
			inverseIndexedTablePrevious = getInverse(previousNGramIndexTable);
			previousNGramIndexTable = null;
		};
		
		extractNGrams();
		//printNGramsSnippet();
		filterNGrams();		
		indexNGramsAndExtractSyntax(buildIndex);
		computeSyntacticEntropy();
		//printFilteredIndexedNGramsToFile();
		printSyntaxTableToFile(new File(workingDir + "NGramSyntaxTable_" + n + ".txt"));
		printEntropySyntaxTableToFile(new File(workingDir + "NGramSyntacticEntropyTable_" + n + ".txt"),
				new File(workingDir + "NGramSyntacticEntropyTableGraph_" + n + ".txt"));
		Parameters.reportLine("Done\n\n");
		Parameters.closeLogFile();
		
	}
	
	private void printNGramsSnippet() {
		int count = 0; 
		for(Entry<NGram, int[]> e : nGramTable.entrySet()) {
			Parameters.reportLine("\t" + e.getKey() + "\t" + e.getValue()[0]);
			if (++count==100)
				break;
		}
		
	}

	private void printParameters() {
		Parameters.reportLine("NGramSyntax");
		Parameters.reportLine("Working dir: " + workingDir);
		Parameters.reportLine("n = " + n);
		Parameters.reportLine("freqThreshold = " + freqThreshold);		
		Parameters.reportLine("--------------");
	}

	private void extractNGrams() throws Exception {
		
		Parameters.reportLine("Building files array");		
		IndexBlipp87.buildFilesArray();
		Parameters.reportLine("Number of files: " + IndexBlipp87.filesArray.length);
		Parameters.reportLine("Extracting " + n + "-grams");
		
		nGramTable = new HashMap<NGram, int[]>();		
		
		if (inverseIndexedTablePrevious==null) {
			//unigram (n=1)
			PrintProgress pp = new PrintProgress("Reading files (" + IndexBlipp87.filesArray.length + ")");
			for(File f : IndexBlipp87.filesArray) {
				pp.next();
				Scanner scan = FileUtil.getScanner(f);
				while(scan.hasNextLine()) {
					String line = scan.nextLine();
					TSNodeLabel t = new TSNodeLabel(line);
					ArrayList<Label> yieldList = t.collectLexicalLabels();
					int length = yieldList.size();
					Label[] yieldArray = yieldList.toArray(new Label[length]);				
					for(int start=0; start<=length-n; start++) { 
						Label[] nGramArray = new Label[]{yieldArray[start]};
						NGram nGram = new NGram(nGramArray);
						Utility.increaseInHashMap(nGramTable, nGram);
					}
				}
			}
			pp.end();
		}
		else {
			//nGram (n>1)			
			PrintProgress pp = new PrintProgress("Reading files (" + inverseIndexedTablePrevious.size() + ")");			
			for(Entry<Short, TreeMap<Short, HashSet<Short>>> f : inverseIndexedTablePrevious.entrySet()) {
				pp.next();
				int fileIndex = f.getKey();
				File file = IndexBlipp87.filesArray[fileIndex];
				TreeMap<Short, HashSet<Short>> firstValue = f.getValue();
				Scanner scanner = FileUtil.getScanner(file);
				int scanIndex = 0;								
				for(Entry<Short, HashSet<Short>> l : firstValue.entrySet()) {
					int lineIndex = l.getKey();					
					while(scanIndex<lineIndex) {
						scanner.nextLine();
						scanIndex++;
					}
					String line = scanner.nextLine();
					scanIndex++;
					TSNodeLabel t = new TSNodeLabel(line);
					ArrayList<Label> yieldList = t.collectLexicalLabels();
					int length = yieldList.size();
					Label[] yieldArray = yieldList.toArray(new Label[length]);
					HashSet<Short> positions = l.getValue();
					for(int p : positions) {
						int end = p+n;	
						if (end>length)
							continue;
						Label[] nGramArray = Arrays.copyOfRange(yieldArray, p, end);
						NGram nGram = new NGram(nGramArray);
						Utility.increaseInHashMap(nGramTable, nGram);
					}
				}
			}
			pp.end();						
		}
		
		Parameters.reportLine("Number of " + n + "-Gram types (before filtering): " + nGramTable.size());
		
	}

	private TreeMap<Short, TreeMap<Short, HashSet<Short>>> getInverse(
			HashMap<NGram, ArrayList<IndexBlipp87>> nGramIndexTable) {
		
		TreeMap<Short, TreeMap<Short, HashSet<Short>>> result =
				new TreeMap<Short, TreeMap<Short, HashSet<Short>>>();
		
		for(Entry<NGram, ArrayList<IndexBlipp87>> e : nGramIndexTable.entrySet())  {
			ArrayList<IndexBlipp87> indexArray = e.getValue();
			for(IndexBlipp87 i : indexArray) {
				Utility.putInTreeMapSet(result, i.fileIndex, i.sentencePosition, i.wordPosition);
			}
		}
		
		return result;
	}

	private void filterNGrams() {
		Parameters.reportLine("Filering ngrams < " + freqThreshold);
		Iterator<Entry<NGram, int[]>> iter = nGramTable.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<NGram, int[]> e = iter.next();
			if (e.getValue()[0]<freqThreshold) {
				iter.remove();
			}
		}
		Parameters.reportLine("Number of " + n + "-Gram types (after filtering): " + nGramTable.size());
	}

	private void indexNGramsAndExtractSyntax(boolean buildIndex) throws Exception {		
		
		if (buildIndex)
			Parameters.reportLine("Indexing filtered nGrams and extracting syntax");
		else 
			Parameters.reportLine("Extracting syntax");
		
		nGramIndexTable = new HashMap<NGram, ArrayList<IndexBlipp87>>();
		nGramSyntaxTable = new HashMap<NGram, HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>> ();
				
		if (inverseIndexedTablePrevious==null) {
			//unigram (n=1)
			short fileIndex = 0;
			PrintProgress pp = new PrintProgress("Reading files (" + IndexBlipp87.filesArray.length + ")");
			for(File f : IndexBlipp87.filesArray) {
				pp.next();
				Scanner scan = FileUtil.getScanner(f);
				short lineIndex = 0;
				while(scan.hasNextLine()) {
					String line = scan.nextLine();
					TSNodeLabel t = new TSNodeLabel(line);
					ArrayList<Label> yieldList = t.collectLexicalLabels();
					Vector<TSNodeLabel> yieldNodeList = new Vector<TSNodeLabel>(t.collectLexicalItems());
					int length = yieldList.size();
					Label[] yieldArray = yieldList.toArray(new Label[length]);				
					for(int start=0; start<=length-n; start++) { 
						Label[] nGramArray = new Label[]{yieldArray[start]};
						NGram nGram = new NGram(nGramArray); 
						if (nGramTable.containsKey(nGram)) {
							IndexBlipp87 index = new IndexBlipp87(fileIndex, lineIndex, (short)start);
							if (buildIndex) {								
								addNGramIndex(nGramIndexTable, nGram, index);
							}
							// extract syntax
							TSNodeLabel nodeWord = yieldNodeList.get(start);
							TSNodeLabel structure = nodeWord.parent;
							Utility.putInHashMapDoubleTreeSet(nGramSyntaxTable, nGram, structure, index);		
						}													
					}
					lineIndex++;
				}
				fileIndex++;
			}
			pp.end();
		}
		else {
			//nGram (n>1)
			PrintProgress pp = new PrintProgress("Reading files (" + inverseIndexedTablePrevious.size() + ")");
			for(Entry<Short, TreeMap<Short, HashSet<Short>>> f : inverseIndexedTablePrevious.entrySet()) {
				pp.next();
				short fileIndex = f.getKey();
				File file = IndexBlipp87.filesArray[fileIndex];
				TreeMap<Short, HashSet<Short>> linesSubTable = f.getValue();
				Scanner scanner = FileUtil.getScanner(file);
				int scanLineIndex = 0;								
				for(Entry<Short, HashSet<Short>> l : linesSubTable.entrySet()) {
					short lineIndex = l.getKey();					
					while(scanLineIndex<lineIndex) {
						scanner.nextLine();
						scanLineIndex++;
					}
					String line = scanner.nextLine();
					scanLineIndex++;
					TSNodeLabel t = new TSNodeLabel(line);
					ArrayList<Label> yieldList = t.collectLexicalLabels();
					int length = yieldList.size();
					Label[] yieldArray = yieldList.toArray(new Label[length]);
					Vector<TSNodeLabel> yieldNodeList = new Vector<TSNodeLabel>(t.collectLexicalItems());
					HashSet<Short> positions = l.getValue();
					for(short p : positions) {
						int end = p+n;
						if (end>length)
							continue;
						Label[] nGramArray = Arrays.copyOfRange(yieldArray, p, end);
						NGram nGram = new NGram(nGramArray);
						if (nGramTable.containsKey(nGram)) {
							IndexBlipp87 index = new IndexBlipp87(fileIndex, lineIndex, p);
							if (buildIndex) {								
								addNGramIndex(nGramIndexTable, nGram, index);
							}
							// extract syntax
							TSNodeLabel firstNodeWord = yieldNodeList.get(p);
							TSNodeLabel lastNodeWord = yieldNodeList.get(end-1);
							TSNodeLabel structure = TSNodeLabel.getMinimalConnectedStructure(firstNodeWord, lastNodeWord);
							structure.removeRedundantRules();
							/*
							if (structure.hasNullElements()) {
								System.err.println("Error in mca: " + firstNodeWord + " (" + p + ") " + 
										lastNodeWord + " (" + (end-1) + ")" );
								//TSNodeLabel root = TSNodeLabel.getLowestCommonAncestorOld(firstNodeWord, lastNodeWord);
								System.err.println(t);
								System.exit(-1);
							}
							*/
							Utility.putInHashMapDoubleTreeSet(nGramSyntaxTable, nGram, structure, index);
						}
					}
				}
			}
			pp.end();
		}
	}
	
	private void computeSyntacticEntropy() {
		nGramSyntaxEntropyTable = new HashMap<NGram, Double>();
		for(Entry<NGram, HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>> e : nGramSyntaxTable.entrySet()) {
			NGram ng = e.getKey();
			HashMap<TSNodeLabel, TreeSet<IndexBlipp87>> table = e.getValue();
			int[] distribution = new int[table.size()];
			int total = 0;
			int i=0;
			for(TreeSet<IndexBlipp87> set : table.values()) {
				int size = set.size();
				distribution[i++] = size;
				total += size;				
			}
			Double entropy = entropy(total, distribution);
			nGramSyntaxEntropyTable.put(ng, entropy);
		}
		
	}

	public static double entropy(int total, int[] distribution) {
		double result = 0;
		for(int d : distribution) {
			double p = ((double)d)/total;
			result += p * Math.log(p);
		}
		return -result;
	}

	public static void addNGramIndex(HashMap<NGram, ArrayList<IndexBlipp87>> table,
			NGram nGram, IndexBlipp87 index) {
		
		ArrayList<IndexBlipp87> list = table.get(nGram);
		if (list==null) {
			list = new ArrayList<IndexBlipp87>();
			table.put(nGram, list);
		} 
		list.add(index);
	}

	private void printSyntaxTableToFile(File outputFile) {
		
		Parameters.reportLine("Printing Indexed Syntax table to file: " + outputFile);
		
		//HashMap<NGram, HashMap<TSNodeLabel, ArrayList<IndexBlipp87>>> nGramSyntaxTable;
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		TreeMap<Integer, HashSet<NGram>> sortedNGramTable = invertFreqTable(nGramTable);
		
		NavigableSet<Integer> reverseSet = sortedNGramTable.descendingKeySet();
		for(Integer freq : reverseSet) {
			HashSet<NGram> nGramFreq = sortedNGramTable.get(freq);
			for(NGram ng : nGramFreq) {
				pw.println(freq + "\t" + ng);
				HashMap<TSNodeLabel, TreeSet<IndexBlipp87>> syntaxTable = nGramSyntaxTable.get(ng);
				TreeMap<Integer, HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>> freqSyntaxTable = 
						getFreqSyntaxTable(syntaxTable);
				NavigableSet<Integer> descendingSet = freqSyntaxTable.descendingKeySet();
				for(Integer size : descendingSet) {
					HashMap<TSNodeLabel, TreeSet<IndexBlipp87>> subTable = freqSyntaxTable.get(size);
					for(Entry<TSNodeLabel, TreeSet<IndexBlipp87>> e : subTable.entrySet()) {
						TSNodeLabel structure = e.getKey();
						TreeSet<IndexBlipp87> indexes = e.getValue();
						String firstNIndex = getFirstIndexes(indexes, 5);
						pw.println("\t" + indexes.size() + "\t" + structure.toString() + "\t" + firstNIndex);
					}
				}		
				pw.println();
			}
		}
		pw.close();
	}
	
	private void printEntropySyntaxTableToFile(File outputFileVerbose, File outputFileGraph) {
		
		Parameters.reportLine("Printing Indexed Entropy Syntax table to file: " + outputFileVerbose);
		Parameters.reportLine("Printing Indexed Entropy Syntax table for graph to file: " + outputFileGraph);
		
		//HashMap<NGram, HashMap<TSNodeLabel, ArrayList<IndexBlipp87>>> nGramSyntaxTable;
		PrintWriter pwVerbose = FileUtil.getPrintWriter(outputFileVerbose);
		PrintWriter pwGraph = FileUtil.getPrintWriter(outputFileGraph);
		
		TreeMap<Double, HashSet<NGram>> sortedNGramEntropyTable = invertDoubleTable(nGramSyntaxEntropyTable);
		
		for(Entry<Double, HashSet<NGram>> e : sortedNGramEntropyTable.entrySet()) {
			Double entropy = e.getKey();
			HashSet<NGram> nGramFreq = e.getValue();
			int freq = nGramTable.get(nGramFreq)[0];
			for(NGram ng : nGramFreq) {
				pwVerbose.println(entropy + "\t" + freq + "\t" + ng);
				pwGraph.println(freq + "\t" + entropy);
				HashMap<TSNodeLabel, TreeSet<IndexBlipp87>> syntaxTable = nGramSyntaxTable.get(ng);
				TreeMap<Integer, HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>> freqSyntaxTable = 
						getFreqSyntaxTable(syntaxTable);
				NavigableSet<Integer> descendingSet = freqSyntaxTable.descendingKeySet();
				for(Integer size : descendingSet) {
					HashMap<TSNodeLabel, TreeSet<IndexBlipp87>> subTable = freqSyntaxTable.get(size);
					for(Entry<TSNodeLabel, TreeSet<IndexBlipp87>> f : subTable.entrySet()) {
						TSNodeLabel structure = f.getKey();
						TreeSet<IndexBlipp87> indexes = f.getValue();
						String firstNIndex = getFirstIndexes(indexes, 5);
						pwVerbose.println("\t" + indexes.size() + "\t" + structure.toString() + "\t" + firstNIndex);
					}
				}					
				pwVerbose.println();
			}
		}
		pwVerbose.close();
		pwGraph.close();
	}
	
	private String getFirstIndexes(TreeSet<IndexBlipp87> indexSet, int n) {
		ArrayList<IndexBlipp87> firstN = new ArrayList<IndexBlipp87>();
		int i=0;
		for(IndexBlipp87 index : indexSet) {
			firstN.add(index);
			if (++i==n)
				break;
		}
		return firstN.toString();
	}

	public static TreeMap<Integer, HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>> getFreqSyntaxTable(
			HashMap<TSNodeLabel, TreeSet<IndexBlipp87>> syntaxTable) {
		
		TreeMap<Integer, HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>> result =
				new TreeMap<Integer, HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>>();
		for(Entry<TSNodeLabel, TreeSet<IndexBlipp87>> e : syntaxTable.entrySet()) {
			TSNodeLabel t = e.getKey();
			TreeSet<IndexBlipp87> v = e.getValue();
			Integer freq = v.size();
			HashMap<TSNodeLabel, TreeSet<IndexBlipp87>> subTable = result.get(freq);
			if (subTable==null) {
				subTable = new HashMap<TSNodeLabel, TreeSet<IndexBlipp87>>();
				result.put(freq, subTable);
			}
			subTable.put(t, v);
		}
		return result;
	}

	public static TreeMap<Integer, HashSet<NGram>> invertFreqTable(
			HashMap<NGram, int[]> table) {
		
		TreeMap<Integer, HashSet<NGram>> result = new TreeMap<Integer, HashSet<NGram>>();
		
		for(Entry<NGram, int[]> e : table.entrySet()) {
			NGram ngram = e.getKey();
			Integer freq = e.getValue()[0];			
			HashSet<NGram> set = result.get(freq);
			if (set==null) {
				set = new HashSet<NGram>();
				result.put(freq, set);
			}
			set.add(ngram);			
		}
		
		return result;
	}


	private TreeMap<Double, HashSet<NGram>> invertDoubleTable(
			HashMap<NGram, Double> table) {
		
		TreeMap<Double, HashSet<NGram>> result = new TreeMap<Double, HashSet<NGram>>();
		
		for(Entry<NGram, Double> e : table.entrySet()) {
			NGram ngram = e.getKey();
			Double p = e.getValue();			
			HashSet<NGram> set = result.get(p);
			if (set==null) {
				set = new HashSet<NGram>();
				result.put(p, set);
			}
			set.add(ngram);			
		}
		
		return result;
	}

	public static void main(String[] args) throws Exception {
		
		int threshold = 50;
		HashMap<NGram, ArrayList<IndexBlipp87>> previousNGramIndexTable = null;
		int lastNGram = 5;
		String workingDir = "/disk/scratch/fsangati/JointProjects/NgramParsing/result/blipp87/NgramSyntax/";		
		new File(workingDir).mkdirs();
		
		for(int n=1; n<=lastNGram; n++) {
			NGramSyntax current = new NGramSyntax(n, threshold, previousNGramIndexTable, workingDir, n<lastNGram);
			previousNGramIndexTable = current.nGramIndexTable;
		}

		/*
		Label l = Label.getLabel("cat");
		NGram a = new NGram(new Label[]{l});
		NGram b = new NGram(new Label[]{l});
		HashMap<NGram, int[]> nGramTable = new HashMap<NGram, int[]>();
		
		Utility.increaseInHashMap(nGramTable, a);
		Utility.increaseInHashMap(nGramTable, b);
		System.out.println(nGramTable);
		*/
		
	}

	
}
