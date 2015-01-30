package tdg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Scanner;
import java.util.Vector;

import tdg.TDNode;
import util.Utility;
import util.file.FileUtil;

public class MstSentenceUlabAdvanced extends MstSentenceUlab{

	public int[] depths;
	public boolean hasLoops;
	public BitSet[] daughters, constituents;
	public BitSet loopIndexes;
	
	public MstSentenceUlabAdvanced() {
		
	}
	
	public MstSentenceUlabAdvanced(MstSentenceUlab s) {
		this.words = s.words;
		this.postags = s.postags;
		this.indexes = s.indexes;
		this.roots = s.roots;
		length = s.length;
		getDepths();
		getDaughters();
		getConstituents();
	}
	
	public MstSentenceUlabAdvanced(String[] words, String[] postags, int[] indexes) {
		this.words = words;
		this.postags = postags;
		this.indexes = indexes;
		length = indexes.length;			
		getRoots();
		getDepths();
		getDaughters();
		getConstituents();
	}
	
	public MstSentenceUlabAdvanced(TDNode oneBest) {
		TDNode[] structure = oneBest.getStructureArray();
		this.length = structure.length;
		this.words = new String[length];
		this.postags = new String[length];
		this.indexes = new int[length];
		for(int i=0; i<length; i++) {
			TDNode iNode = structure[i];
			this.words[i] = iNode.lex;
			this.postags[i] = iNode.postag;
			TDNode parent = iNode.parent;
			this.indexes[i] = (parent==null) ? 0 : parent.index+1;
		}
		getRoots();
		getDepths();
		getDaughters();
		getConstituents();
	}

	public ArrayList<MstSentenceUlab> getOneStepVariation() {
		ArrayList<MstSentenceUlab> result = new ArrayList<MstSentenceUlab>();
		if (this.hasLoops || !this.isProjective()) {
			System.err.println("Current Structure has loops or is not projective");
			return null;
		}
		for(int i=0; i<length; i++) {
			if (Arrays.binarySearch(roots, i)>=0) continue;
			for(int j=1; j<=length; j++) {
				if (j==i+1 || indexes[i]==j) continue;
				MstSentenceUlabAdvanced newStructure = new MstSentenceUlabAdvanced();
				newStructure.words = Arrays.copyOf(words, length);
				newStructure.indexes = Arrays.copyOf(indexes, length);
				newStructure.postags = Arrays.copyOf(postags, length);
				newStructure.roots = Arrays.copyOf(roots, roots.length);
				newStructure.length = length;
				newStructure.indexes[i] = j;
				newStructure.getRoots();
				newStructure.getDepths();
				newStructure.getDaughters();
				newStructure.getConstituents();
				if (newStructure.hasLoops || !newStructure.isProjective()) continue;
				result.add(newStructure);
			}			
		}
		return result;
	}
	
	public boolean hasLoops() {
		return hasLoops;
	}
	
	public int[] roots() {
		return roots;
	}
	
	public int[] depths() {
		return depths;
	}
	
	public boolean hasMultipleRoots() {
		return roots.length>1;
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
	
	public static ArrayList<MstSentenceUlabAdvanced> getTreebankAdvancedFromConnl(File inputFile, String encoding) {
		Scanner inputScanner = FileUtil.getScanner(inputFile, encoding);
		ArrayList<MstSentenceUlabAdvanced> result = new ArrayList<MstSentenceUlabAdvanced>();
		do {
			MstSentenceUlab mstSentenceUlab = ConnlToUlab.getNextConnlLinesSentenceInMstUlab(inputScanner);
			if (mstSentenceUlab==null) break;
			result.add(new MstSentenceUlabAdvanced(mstSentenceUlab));			
		} while(true);
		return result;
	}
	
	public static ArrayList<MstSentenceUlabAdvanced> getTreebankAdvancedFromMst(File inputFile, String encoding) {
		Scanner inputScanner = FileUtil.getScanner(inputFile, encoding);
		ArrayList<MstSentenceUlabAdvanced> result = new ArrayList<MstSentenceUlabAdvanced>();
		int sentenceNumber = 0;
		while(inputScanner.hasNextLine()) {
			sentenceNumber++;
			//System.out.println(sentenceNumber);
			String words = inputScanner.nextLine();
			while(inputScanner.hasNextLine() && words.equals("")) words = inputScanner.nextLine();
			if (words.equals("")) break;
			String postags = inputScanner.nextLine();
			String indexesString = inputScanner.nextLine();			
			int[] indexes = Utility.parseIndexList(indexesString.split("\t"));
			result.add(
				new MstSentenceUlabAdvanced(words.split("\t"), postags.split("\t"), indexes)
			);
			//if (sentenceNumber==100) break;
		}
		return result;
	}
	
	public static void main(String[] args) {

	}
	
}
