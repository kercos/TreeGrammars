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

public class MstSentenceUlab {

	public String[] words, postags;
	public int length;
	public int[] indexes;
	public int[] roots;
	
	public MstSentenceUlab() {
		
	}
	
	public MstSentenceUlab(String[] words, String[] postags, int[] indexes) {
		this.words = words;
		this.postags = postags;
		this.indexes = indexes;
		length = indexes.length;	
		getRoots();
	}
	
	public MstSentenceUlab(TDNode t) {
		TDNode[] structure = t.getStructureArray();
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
	}
	
	public TDNode toTDNode() {
		return new TDNode(this.words, this.postags, this.indexes);
	}
	
	public String[] words() {
		return words;
	}
	
	public String[] postags() {
		return postags;
	}
	
	public int[] indexes() {
		return indexes;
	}
	
	public int[] roots() {
		return roots;
	}
		
	public boolean hasMultipleRoots() {
		return roots.length>1;
	}
	
	public BitSet getLoopIndexes() {
		return new MstSentenceUlabAdvanced(this).loopIndexes;
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
		
	public static ArrayList<MstSentenceUlab> getTreebank(File inputFile, String encoding) {
		Scanner inputScanner = FileUtil.getScanner(inputFile, encoding);
		ArrayList<MstSentenceUlab> result = new ArrayList<MstSentenceUlab>();
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
				new MstSentenceUlab(words.split("\t"), postags.split("\t"), indexes)
			);
			//if (sentenceNumber==100) break;
		}
		return result;
	}
	
	public static void discardLoopsCases(File mstFile, File outputFile, String encoding) {
		PrintWriter outputWriter = FileUtil.getPrintWriter(outputFile, encoding);
		ArrayList<MstSentenceUlab> treebank = getTreebank(mstFile, encoding);
		for(MstSentenceUlab sentence : treebank) {
			if (!sentence.hasLoops()) {
				outputWriter.println(sentence.toMstUlab() + "\n");
			}
			
		}
		outputWriter.close();
	}
	
	public static void addEOS(File inputFile, File outputFile, String encoding) {
		ArrayList<MstSentenceUlab> treebank = getTreebank(inputFile, encoding);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile, encoding);
		for(MstSentenceUlab s : treebank) {
			s.addEOS();
			pw.println(s.toMstUlab()+ "\n");
		}
		pw.close();
	}
	
	public void addEOS() {
		int lastIndex = length;
		length++;
		words = Arrays.copyOf(words, length);
		postags = Arrays.copyOf(postags, length);
		indexes = Arrays.copyOf(indexes, length);
		words[lastIndex] = "EOS";
		postags[lastIndex] = "EOS";
		
		for(int i=0; i<lastIndex; i++) {
			if (indexes[i]==0) indexes[i] = length;
		}
		indexes[lastIndex] = 0;
		
	}
	
	public static int uasScore(MstSentenceUlab s1, MstSentenceUlab s2) {
		int[] i1 = s1.indexes;
		int[] i2 = s2.indexes;
		int score = 0;
		for(int i=0; i<i1.length; i++) {
			if (i1[i]==i2[i]) score++;
		}
		return score;
	}
	
	public static Integer[] wrongWords(MstSentenceUlab s1, MstSentenceUlab s2) {
		int[] i1 = s1.indexes;
		int[] i2 = s2.indexes;
		Vector<Integer> score = new Vector<Integer>();
		for(int i=0; i<i1.length; i++) {
			if (i1[i]!=i2[i]) score.add(i);
		}
		return score.toArray(new Integer[score.size()]);
	}
	
	public String toMstUlab() {
		return 
		Utility.joinStringArrayToString(words, "\t") + "\n" +
		Utility.joinStringArrayToString(postags, "\t") + "\n" +
		Utility.joinIntArrayToString(indexes, "\t");
	}
	
	public String toString() {
		return toMstUlab();
	}
	
	public MstSentenceUlabAdvanced getAdvanced() {
		return new MstSentenceUlabAdvanced(this);
	}
	
	public ArrayList<MstSentenceUlab> getOneStepVariation() {
		return getAdvanced().getOneStepVariation();
	}
	
	public boolean isProjective() {
		return getAdvanced().isProjective();
	}
	
	public boolean hasLoops() {
		return getAdvanced().hasLoops();
	}
	
	public static void main(String[] args) {
		File inputFile = new File(TanlD.TanlD_Dev_MstUlab_EOS);
		File outputFile = new File(TanlD.TanlD_Dev_MstUlab_NoLoops_EOS);
		discardLoopsCases(inputFile, outputFile, "UTF-8");
	}

	
}
