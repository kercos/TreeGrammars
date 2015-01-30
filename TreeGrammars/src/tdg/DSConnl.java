package tdg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.Vector;
import java.util.Map.Entry;

import settings.Parameters;
import tdg.corpora.MstSentenceUlab;
import tdg.corpora.TanlD;
import util.file.FileUtil;

/**
 *  @author fsangati
 *  
 *  columns of each Connl line
 *  1 ID: Token counter, starting at 1 for each new sentence.
 *  2 FORM: Word form or punctuation symbol.
 *  3 LEMMA: Lemma or stem (depending on particular data set) of word form, or an underscore if not available.
 *  4 CPOSTAG: Coarse-grained part-of-speech tag, where tagset depends on the language.
 *  5 POSTAG: Fine-grained part-of-speech tag, where the tagset depends on the language, or identical to the coarse-grained part-of-speech tag if not available.
 *  6 FEATS: Unordered set of syntactic and/or morphological features (depending on the particular language), separated by a vertical bar (|), or an underscore if not available.
 *  7 HEAD: Head of the current token, which is either a value of ID or zero ('0'). Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
 *  8 DEPREL: Dependency relation to the HEAD. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningful or simply 'ROOT'.
 *  9 PHEAD: Projective head of current token, which is either a value of ID or zero ('0'), or an underscore if not available. Note that depending on the original treebank annotation, there may be multiple tokens an with ID of zero. The dependency structure resulting from the PHEAD column is guaranteed to be projective (but is not available for all languages), whereas the structures resulting from the HEAD column will be non-projective for some sentences of some languages (but is always available).
 *  10 PDEPREL: Dependency relation to the PHEAD, or an underscore if not available. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningful or simply 'ROOT'. 
 *
 */
public class DSConnl {
	
	public int length;
	String[][] rows_Array;		
	public String[] words, posTags, labels;
	public int[] indexes;
	public int[] depths;
	public BitSet loopIndexes;
	public BitSet[] daughters, constituents;
	public boolean hasLoops;
	public int[] roots;
	
	static String emptyColString = "_";
	static int wordFieldIndex = 1;
	static int cpostagFieldIndex = 3;
	static int headFieldIndex = 6;
	static int deprelFieldIndex = 7;
	
	public DSConnl(ArrayList<String> linesArray) {
		length = linesArray.size();
		rows_Array = new String[length][10];
		int i=0;
		for(String line : linesArray) {			
			String[] lineSplit = line.split("\t");
			for(int j=0; j<10; j++) {
				rows_Array[i][j] = lineSplit[j];
			}			
			i++;
		}		
		buildAll();
	}
	
	public DSConnl(String[] words, String[] pos, int[] indexes) {
		length = words.length;
		rows_Array = new String[length][10];
		for(String[] line : rows_Array) Arrays.fill(line, emptyColString);
		for(int i=0; i<length; i++) rows_Array[i][wordFieldIndex] = words[i];
		for(int i=0; i<length; i++) rows_Array[i][cpostagFieldIndex] = pos[i];
		for(int i=0; i<length; i++) rows_Array[i][headFieldIndex] = Integer.toString(indexes[i]);
		buildAll();
	}
	
	public DSConnl(String[] words, String[] pos, int[] indexes, String[] labels) {
		length = words.length;
		rows_Array = new String[length][10];
		for(String[] line : rows_Array) Arrays.fill(line, emptyColString);
		for(int i=0; i<length; i++) rows_Array[i][wordFieldIndex] = words[i];
		for(int i=0; i<length; i++) rows_Array[i][cpostagFieldIndex] = pos[i];
		for(int i=0; i<length; i++) rows_Array[i][headFieldIndex] = Integer.toString(indexes[i]);
		for(int i=0; i<length; i++) rows_Array[i][deprelFieldIndex] = labels[i];
		buildAll();
	}
	
	private void buildAll() {
		words = getColumn(wordFieldIndex);
		posTags = getColumn(cpostagFieldIndex);
		labels = getColumn(deprelFieldIndex);
		indexes = getIndexes();
		getRoots();
		getDepths();
		getDaughters();
		getConstituents();
	}
	
	private void getRoots() {
		int numberOfRoots = 0;
		for(int i : indexes) {
			if (i==0) numberOfRoots++;
		}
		roots = new int[numberOfRoots];
		int p=0;
		for(int j=0; j<length; j++) {
			if (indexes[j]==0) {
				roots[p]=j;
				p++;
			}			
		}
	}
	
	private void getDepths() {
		loopIndexes = new BitSet();
		depths = new int[length];
		Arrays.fill(depths, -1);
		for(int r : roots) depths[r] = 0;
		for(int i=0; i<length; i++) {
			getDepths(i, new BitSet());
		}
	}
	
	private int getDepths(int i, BitSet visited) {
		if (depths[i]!=-1) return depths[i];
		int pIndex = indexes[i]-1; //0-based
		if (visited.get(pIndex)) {
			hasLoops = true;
			loopIndexes.set(i);
			return depths[i] = 0;
		}
		visited.set(pIndex);
		return depths[i] = getDepths(pIndex, visited)+1;
	}
	
	private void getDaughters() {
		daughters = new BitSet[length];
		for(int i=0; i<length; i++) {
			daughters[i] = new BitSet();
		}
		for(int i=0; i<length; i++) {
			int pIndex = indexes[i];
			if (pIndex==0) continue;
			pIndex--; //0-based
			daughters[pIndex].set(i);
		}
	}
	
	private void getConstituents() {
		constituents = new BitSet[length];
		for(int i=0; i<length; i++) {
			getConstituent(i);
		}
	}
	
	private BitSet getConstituent(int i) {
		if (constituents[i]!=null) return constituents[i]; 
		constituents[i] = new BitSet();
		constituents[i].set(i);
		if (daughters[i].isEmpty()) return constituents[i];
		int d=-1;
		do {
			d = daughters[i].nextSetBit(d+1);
			if (d==-1) break;
			constituents[i].or(getConstituent(d));
		} while(true);
		return constituents[i];
	}
	
	public boolean isProjective() {
		for(int i=0; i<length; i++) {
			BitSet iBs = constituents[i];
			int p=-1, n;
			do {
				n = iBs.nextSetBit(p+1);
				if (n==-1) break;
				if (p!=-1 && n!=p+1) return false;
				p=n;				
			} while(true);
		}
		return true;
	}

	
	public boolean isBlind() {
		return rows_Array[0][headFieldIndex].equals(emptyColString);
	}
	
	public int[] getIndexes() {
		if (isBlind()) return null;		
		int[] result = new int[length];
		int i = 0;
		for(String[] row : rows_Array) {
			result[i] = Integer.parseInt(row[headFieldIndex]);
			i++;
		}		
		return result;
	}
	
	private String[] getColumn(int c) {
		if (isBlind()) return null;		
		String[] result = new String[length];
		int i = 0;
		for(String[] row : rows_Array) {
			result[i] = row[c];
			i++;
		}		
		return result;
	}

	
	public void makeBlind() {
		for(String[] line : rows_Array) {
			for(int i=6; i<10; i++) {
				line[i] = emptyColString;
			}			
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<length; i++) {
			for(int j=0; j<9; j++) {
				sb.append(rows_Array[i][j]).append("\t");
			}
			sb.append(rows_Array[i][9]).append("\n");
		}
		return sb.toString(); 
	}
	
	public static ArrayList<DSConnl> getConnlTreeBank(File inputFile) {
		ArrayList<DSConnl> treebank = new ArrayList<DSConnl>();
		Scanner scan = FileUtil.getScanner(inputFile);
		ArrayList<String> linesArray = new ArrayList<String>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();			
			if (line.length()==0) {
				if (linesArray.isEmpty()) continue;
				treebank.add(new DSConnl(linesArray));
				linesArray.clear();
			}
			else linesArray.add(line);
		}
		if (!linesArray.isEmpty()) treebank.add(new DSConnl(linesArray));
		scan.close();
		return treebank;
	}
	
	public static void makeBlindTreeBank(ArrayList<DSConnl> treebank) {		
		for(DSConnl t: treebank) {
			t.makeBlind();
		}		
	}
	
	public static void treeBankToFile(ArrayList<DSConnl> treebank, File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(DSConnl t: treebank) {
			pw.println(t.toString());
		}
		pw.close();
	}
	
	public static Integer[] wrongWords(DSConnl s1, DSConnl s2) {
		int[] i1 = s1.indexes;
		int[] i2 = s2.indexes;
		Vector<Integer> score = new Vector<Integer>();
		for(int i=0; i<i1.length; i++) {
			if (i1[i]!=i2[i]) score.add(i);
		}
		return score.toArray(new Integer[score.size()]);
	}
	
	public static BitSet[] wrongIndexesPosLabels(DSConnl s1, DSConnl s2) {
		BitSet wrongIndexes = new BitSet();
		BitSet wrongPos = new BitSet();
		BitSet wrongLabels = new BitSet();		
		
		for(int i=0; i<s1.length; i++) {
			if (s1.indexes[i]!=s2.indexes[i]) wrongIndexes.set(i);
			if (!s1.posTags[i].equals(s2.posTags[i])) wrongPos.set(i);
			if (!s1.labels[i].equals(s2.labels[i])) wrongLabels.set(i);
		}
		return new BitSet[]{wrongIndexes, wrongPos, wrongLabels};
	}
	
	
	
	public static void makeEvalita09TutDevelop() {		
		String filePath = Parameters.corpusPath + "Evalita09/Treebanks/Dependency/TUT/develop2/";
		File inputFile = new File(filePath + "TUT-Evalita09-trainset.conll");
		File outputFileTreebakShuffled = new File(filePath + "TUT-Evalita09-trainset_shuffled.conll");
		File outputFileTrain = new File(filePath + "TUT-Evalita09-trainset90.conll");
		File outputFileTest = new File(filePath + "TUT-Evalita09-trainset10.conll");
		ArrayList<DSConnl> treebank = getConnlTreeBank(inputFile);
		Collections.shuffle(treebank);
		int size = treebank.size();
		int size90 = (int)(0.9*size);
		int size10 = size - size90;
		
		ArrayList<DSConnl> treebank90 = new ArrayList<DSConnl>();
		ArrayList<DSConnl> treebank10 = new ArrayList<DSConnl>();
		for(ListIterator<DSConnl> i = treebank.listIterator(); i.nextIndex()<size90; ) {
			treebank90.add(i.next());
		}
		for(ListIterator<DSConnl> i = treebank.listIterator(size90); i.hasNext(); ) {
			treebank10.add(i.next());
		}
		System.out.println(size + "\t" + size90 + "\t" + size10);
		System.out.println(treebank90.size() + "\t" + treebank10.size());
		treeBankToFile(treebank, outputFileTreebakShuffled);
		treeBankToFile(treebank90, outputFileTrain);
		treeBankToFile(treebank10, outputFileTest);
	}
	
	public static void makeEvalita09TutDevelopBlind() {
		String filePath = Parameters.corpusPath + "Evalita09/Treebanks/Dependency/TUT/develop2/";
		File inputFileTest = new File(filePath + "TUT-Evalita09-trainset10.conll");
		File ouputFileTestBlind = new File(filePath + "TUT-Evalita09-trainset10.blind.conll");
		ArrayList<DSConnl> treebank = getConnlTreeBank(inputFileTest);
		makeBlindTreeBank(treebank);
		treeBankToFile(treebank, ouputFileTestBlind);		
	}
	
	public static void makeTanl09DevelopBlind() {		
		File inputFileTest = new File (TanlD.TanlD_Dev_Connl);
		File ouputFileTestBlind = new File (TanlD.TanlD_Dev_Blind_Connl);
		ArrayList<DSConnl> treebank = getConnlTreeBank(inputFileTest);
		makeBlindTreeBank(treebank);
		treeBankToFile(treebank, ouputFileTestBlind);		
	}
	
	public static void selectRerankedFromFile(File parsedFile,
			File rerankedFileConll, ArrayList<Integer> rerankedIndexes) {
		Scanner scanner = FileUtil.getScanner(parsedFile);
		PrintWriter pw = FileUtil.getPrintWriter(rerankedFileConll);
		ListIterator<Integer> iter = rerankedIndexes.listIterator();
		int counter = 0;		
		int targetIndex = iter.next();
		boolean priviousIsEmtpyLine = false;
		while(scanner.hasNextLine()) {			
			String line = scanner.nextLine();			
			if (line.length()>0) {
				if (counter==targetIndex) pw.println(line);
				priviousIsEmtpyLine = false;				
			}
			else {				
				if (priviousIsEmtpyLine) {
					counter = 0;
					targetIndex = iter.hasNext()? iter.next() : -1;
					//pw.println();
				}
				else {
					if (counter==targetIndex) pw.println();
					counter++;								
				}
				priviousIsEmtpyLine = true;													
			}									
		}
		pw.println();
		pw.close();
	}
	
	public static void selectOneBestFromFile(File parsedFile, File outputFileConll) {
		Scanner scanner = FileUtil.getScanner(parsedFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFileConll);
		int counter = 0;		
		int targetIndex = 0;
		boolean priviousIsEmtpyLine = false;
		while(scanner.hasNextLine()) {			
			String line = scanner.nextLine();			
			if (line.length()>0) {
				if (counter==targetIndex) pw.println(line);
				priviousIsEmtpyLine = false;				
			}
			else {				
				if (priviousIsEmtpyLine) {
					counter = 0;
					//pw.println();
				}
				else {
					if (counter==targetIndex) pw.println();
					counter++;								
				}
				priviousIsEmtpyLine = true;													
			}									
		}
		pw.println();
		pw.close();
	}
	
	public static void evalDepConllUlab(File gold, File parsed) {
		ArrayList<DSConnl> goldCorpus = getConnlTreeBank(gold);
		ArrayList<DSConnl> parsedCorpus = getConnlTreeBank(parsed);
		if (goldCorpus.size()!=parsedCorpus.size()) {
			System.err.println("Gold and Parsed file differ in number of sentences");
			return;
		}
		Iterator<DSConnl> goldIter = goldCorpus.iterator();
		Iterator<DSConnl> parsedIter = parsedCorpus.iterator();
		int correct = 0;
		int total = 0;
		while(goldIter.hasNext()) {
			DSConnl goldTree = goldIter.next();
			DSConnl parsedTree = parsedIter.next();
			int[] indexesGold = goldTree.getIndexes();
			int[] indexesParsed = parsedTree.getIndexes();
			total += goldTree.length;
			for(int i=0; i<goldTree.length; i++) {
				if (indexesGold[i]==indexesParsed[i]) correct++;
			}
		}
		float UAS = (float) correct / total;
		System.out.println("Correct/Total: " + correct + "/" + total);
		System.out.println("UAS: " + UAS);
	}
	
	public static void evalDepConllLab(File gold, File parsed) {
		ArrayList<DSConnl> goldCorpus = getConnlTreeBank(gold);
		ArrayList<DSConnl> parsedCorpus = getConnlTreeBank(parsed);
		if (goldCorpus.size()!=parsedCorpus.size()) {
			System.err.println("Gold and Parsed file differ in number of sentences");
			return;
		}
		Iterator<DSConnl> goldIter = goldCorpus.iterator();
		Iterator<DSConnl> parsedIter = parsedCorpus.iterator();
		int correct = 0;
		int total = 0;
		while(goldIter.hasNext()) {
			DSConnl goldTree = goldIter.next();
			DSConnl parsedTree = parsedIter.next();
			int[] indexesGold = goldTree.getIndexes();
			int[] indexesParsed = parsedTree.getIndexes();
			String[] labelGold = goldTree.labels;
			String[] labelParsed = parsedTree.labels;
			total += goldTree.length;
			for(int i=0; i<goldTree.length; i++) {
				if (indexesGold[i]==indexesParsed[i] && labelGold[i].equals(labelParsed[i])) correct++;
			}
		}
		float UAS = (float) correct / total;
		System.out.println("Correct/Total: " + correct + "/" + total);
		System.out.println("LAS: " + UAS);
	}
	
	public static void evalDepQualitativeLab(File gold, File parsed, File outputFile) {
		ArrayList<DSConnl> goldCorpus = getConnlTreeBank(gold);
		ArrayList<DSConnl> parsedCorpus = getConnlTreeBank(parsed);
		if (goldCorpus.size()!=parsedCorpus.size()) {
			System.err.println("Gold and Parsed file differ in number of sentences");
			return;
		}
		
		int sentences = goldCorpus.size();
		int[] result = new int[]{0,0};
		Hashtable<String, int[]> categoryDaughterLas = new Hashtable<String, int[]>(); 
		Iterator<DSConnl> goldIter = goldCorpus.iterator();
		Iterator<DSConnl> parsedIter = parsedCorpus.iterator();
		while(goldIter.hasNext()) {
			DSConnl goldTree = goldIter.next();
			DSConnl parsedTree = parsedIter.next();
			int[] indexesGold = goldTree.getIndexes();
			int[] indexesParsed = parsedTree.getIndexes();
			String[] labelGold = goldTree.labels;
			String[] labelParsed = parsedTree.labels;
			String[] cpostag = goldTree.posTags;
			for(int i=0; i<goldTree.length; i++) {
				String categoryDaughter = cpostag[i];
				if (indexesGold[i]==indexesParsed[i] && labelGold[i].equals(labelParsed[i])) {
					result[0]++;
					addCategoryScore(categoryDaughterLas, categoryDaughter,1,1);
				}
				else addCategoryScore(categoryDaughterLas, categoryDaughter,0,1);
			}			
			result[1]+=goldTree.length;
		}
		String print = "Number of sentences: " + sentences + "\n" +
		"Total Tokens: " + result[1] + "\n" +
		"Correct attachments: " + result[0] + "\n" +
		"LAS: " + (float)result[0]/result[1];
		System.out.println(print);
		java.io.PrintWriter out = FileUtil.getPrintWriter(outputFile);
		out.println(print);
		out.println("\nParent Category LAS report:");
		for(Entry<String, int[]> e : categoryDaughterLas.entrySet()) {
			int[] value = e.getValue();
			out.println(e.getKey() + "\t" + value[0] + "\t" + value[1]);
		}
		out.println();
		out.println("\nDaughter Category LAS report:");
		for(Entry<String, int[]> e : categoryDaughterLas.entrySet()) {
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
	
	public static void evalitaTUT() {
		//makeEvalita09TutDevelop();
		//makeEvalita09TutDevelopBlind();
		//makeTanl09DevelopBlind();
		String baseDir = Parameters.resultsPath + "Reranker/Parsed/MST_0.5_2order/goldPOS/Tut_Evalita09/Reranker/final/";
		File oneBestFile = new File(baseDir + "TUT-Evalita09-testset10-9-09_nBest1.conll");
		File rerankedFile = new File(baseDir + "reranked.conll");
		File goldFile = new File(baseDir + "TUT-Evalita09-testsetGOLD5-10-09.conll");
		//evalDepConllUlab(goldFile, rerankedFile);
		//evalDepConllUlab(goldFile, oneBestFile);
		//evalDepConllLab(goldFile, rerankedFile);
		//evalDepConllLab(goldFile, oneBestFile);
		String ouputDir = "/Users/fsangati/Documents/UNIVERSITY/UVA/Papers/Evalita09/depPaper/Graphs/";
		File outputFile1Best = new File(ouputDir + "evalQualit_1Best_ulab.txt");
		File outputFileReranked = new File(ouputDir + "evalQualit_Reranked_ulab.txt");
		//evalDepQualitativeLab(goldFile, rerankedFile, outputFile1Best);
		//evalDepQualitativeLab(goldFile, oneBestFile, outputFileReranked);
		evalDepConllLab(goldFile, oneBestFile);
		evalDepConllLab(goldFile, rerankedFile);
	}
	
	public static void main(String[] args) {
		//evalitaTUT();
		File inputFile = new File("/Users/fsangati/Documents/UNIVERSITY/UVA/Papers/TLT8/trees/666.conll");
		DSConnl.getConnlTreeBank(inputFile);
	}

	
	
}
