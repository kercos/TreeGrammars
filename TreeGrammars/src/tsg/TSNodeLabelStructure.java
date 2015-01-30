package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.ListIterator;
import java.util.Scanner;

import tsg.corpora.Wsj;
import util.PrintProgress;
import util.file.FileUtil;

public class TSNodeLabelStructure {

	public TSNodeLabelIndex[] structure;
	int length;
	//root is in position 0
	
	public TSNodeLabelStructure(String s) throws Exception {
		structure = (new TSNodeLabelIndex(s)).collectAllNodesInArray();
		length = structure.length;
	}
	
	public TSNodeLabelStructure(TSNodeLabelIndex t) {
		structure = t.collectAllNodesInArray();
		length = structure.length;
	}
	
	public TSNodeLabelStructure(TSNodeLabel t) {
		TSNodeLabelIndex ti = new TSNodeLabelIndex(t);
		structure = ti.collectAllNodesInArray();
		length = structure.length;
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder("[");	
		for(int i=0; i<length; i++) {
			result.append(structure[i].label());
			if (i!=length-1) result.append(", ");
		}
		result.append("]");
		return result.toString();
	}
	
	public int length() {
		return length;
	}
	
    private int lexiconLength() {
		int result = 0;
		for(TSNodeLabelIndex n : structure) {
			if (n.isLexical) result++;
		}
		return result;
	}
	
	public TSNodeLabelIndex[] structure() {
		return structure;
	}
	
	public int hashCode() {
		return structure[0].hashCode();
	}
	
	public boolean equals(Object o) {
		if (o==this) return true;
		if (o instanceof TSNodeLabelStructure) {
			TSNodeLabelStructure anOtherNodeStructure = (TSNodeLabelStructure) o;
			if (this.structure.length != anOtherNodeStructure.structure.length) return false;
			return structure[0].equals(anOtherNodeStructure.structure[0]);
		}
		return false;		
	}
	
	public static ArrayList<TSNodeLabelStructure> readTreebank(File inputFile) throws Exception {
		return readTreebank(inputFile, FileUtil.defaultEncoding, Integer.MAX_VALUE);
	}
	
	public static ArrayList<TSNodeLabelStructure> readTreebank(File inputFile, int LL) throws Exception {
		return readTreebank(inputFile, FileUtil.defaultEncoding, LL);
	}
			
	
	public static ArrayList<TSNodeLabelStructure> readTreebank(
			File inputFile, String encoding, int LL) throws Exception {
		ArrayList<TSNodeLabelStructure> result = new ArrayList<TSNodeLabelStructure>();
		Scanner scan = FileUtil.getScanner(inputFile, encoding);
		PrintProgress pp = new PrintProgress("Reading Treebank: ");
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.equals("")) continue;
			pp.next();
			TSNodeLabelStructure lineStructure = new TSNodeLabelStructure(line);
			if (lineStructure.lexiconLength() > LL) continue;
			result.add(lineStructure);
			//if (result.size()==1000) break;
		}
		pp.end();
		return result;
	}
	
	public static void removeSemanticTagInTreebank(ArrayList<TSNodeLabelStructure> treebank) {
		for(TSNodeLabelStructure t : treebank) {
			t.structure[0].removeSemanticTags();
		}
	}
	
	public static void makeTreebakWithNoDoubles(
			File inputFile, String encoding, File outputFile) throws Exception {
		LinkedHashSet<TSNodeLabelStructure> treebank = new LinkedHashSet<TSNodeLabelStructure>();
		Scanner scan = FileUtil.getScanner(inputFile, encoding);
		PrintProgress pp = new PrintProgress("Reading Treebank: ");
		while(scan.hasNextLine()) {			
			String line = scan.nextLine();
			if (line.equals("")) continue;
			pp.next();
			TSNodeLabelStructure lineStructure = new TSNodeLabelStructure(line);			
			treebank.add(lineStructure);
		}
		pp.end();
		pp = new PrintProgress("Writing Treebank: ");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile, encoding);
		for(TSNodeLabelStructure t : treebank) {
			pp.next();
			pw.println(t.structure[0]);
		}
		pp.end();
		pw.close();
	}
	
	public static void findEqualStructures(ArrayList<TSNodeLabelStructure> treebank,
			File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		int t1Index = -1;
		BitSet foundIndexes = new BitSet(treebank.size());
		for(TSNodeLabelStructure t1 : treebank) {			
			t1Index++;		
			if (foundIndexes.get(t1Index)) continue;
			ListIterator<TSNodeLabelStructure> i2 = treebank.listIterator(t1Index+1);
			ArrayList<Integer> equals = new ArrayList<Integer>();
			int t2Index = t1Index;
			while (i2.hasNext()) {	
				t2Index++;
				TSNodeLabelStructure t2 = i2.next();				
				if (t1.structure[0].equals(t2.structure[0])) {
					equals.add(t2Index);
					foundIndexes.set(t2Index);
				}				
			}
			if (!equals.isEmpty()) {
				pw.println(t1Index + ": " + equals);
				pw.println(t1.structure[0]);
				pw.println();
			}
		}
		pw.close();
	}

	public static int maxDepthTreebank(ArrayList<TSNodeLabelStructure> treebank) {
		int maxDepth = -1;		
		for(TSNodeLabelStructure t : treebank) {			
			int depth = t.structure[0].maxDepth();
			if (depth>maxDepth) maxDepth = depth;
		}
		return maxDepth;
	}

	public static void main(String[] args) throws Exception {
		File inputFile = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		File outputFile = new File(Wsj.WsjOriginalCleaned + "wsj-02-21_noDoubles.mrg");	
		//ArrayList<TSNodeLabelStructure> treebank = 
		//	TSNodeLabelStructure.readTreebank(inputFile, FileUtil.defaultEncoding, 1000);
		//findEqualStructures(treebank, new File("tmp/equalStructures.txt"));
		makeTreebakWithNoDoubles(inputFile, FileUtil.defaultEncoding, outputFile);
    }


	


	
}
