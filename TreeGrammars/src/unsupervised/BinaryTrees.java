package unsupervised;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;

public class BinaryTrees {
	
	static Label xLabel = Label.getLabel("X");
	public static ArrayList<TSNodeLabel> getBinaryTrees(int initialIndex, int numberOfWords) {
		
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		
		if (numberOfWords==1) {
			String firstWord = "w" + initialIndex + "";
			TSNodeLabel tree = new TSNodeLabel(Label.getLabel(firstWord), true);			
			result.add(tree);
			return result;
		}
		
		if (numberOfWords==2) {
			String firstWord = "w" + initialIndex++ + "";
			String secondWord = "w"+ initialIndex + "";
			TSNodeLabel tree = new TSNodeLabel(xLabel, false);
			TSNodeLabel t1 = new TSNodeLabel(Label.getLabel(firstWord), true);
			TSNodeLabel t2 = new TSNodeLabel(Label.getLabel(secondWord), true);
			tree.daughters = new TSNodeLabel[]{t1,t2};
			t1.parent = t2.parent = tree;
			result.add(tree);
			return result;
		}		
		
		int secondSpanStart = initialIndex + 1;
		int secondSpanSize = numberOfWords - 1;
		for(int firstSpanSize = 1; firstSpanSize<numberOfWords; firstSpanSize++) {			
			ArrayList<TSNodeLabel> firstSpanTrees = getBinaryTrees(initialIndex, firstSpanSize);
			ArrayList<TSNodeLabel> secondSpanTrees = getBinaryTrees(secondSpanStart++, secondSpanSize--);
			for(TSNodeLabel t1 : firstSpanTrees) {
				for(TSNodeLabel t2 : secondSpanTrees) {
					TSNodeLabel tree = new TSNodeLabel(xLabel, false);
					tree.daughters = new TSNodeLabel[]{t1,t2};
					t1.parent = t2.parent = tree;
					result.add(tree);
				}
			}
		}		
		return result;		
	}
	
	private static int addAllSubtrees(TSNodeLabel t, Hashtable<String, int[]> allSubtresTable) {		
		ArrayList<String> subTrees = t.allSubTrees(Integer.MAX_VALUE, Integer.MAX_VALUE);
		for(String s : subTrees) {
			Utility.increaseInTableInt(allSubtresTable, s);
		}
		return subTrees.size();
	}
	
	private static Hashtable<String, int[]> addAllSubtrees(TSNodeLabel t) {
		Hashtable<String, int[]> allSubtresTable = new Hashtable<String, int[]>();
		ArrayList<String> subTrees = t.allSubTrees(Integer.MAX_VALUE, Integer.MAX_VALUE);
		for(String s : subTrees) {
			Utility.increaseInTableInt(allSubtresTable, s);
		}
		return allSubtresTable;
	}
	
	public static void increaseAll(Hashtable<String, int[]> source, Hashtable<String, int[]> destination) {
		for(Entry<String,int[]> e : source.entrySet()) {
			Utility.increaseInTableInt(destination, e.getKey(), e.getValue()[0]);
		}
	}
	
    public static int countInternalNodes(TSNodeLabel t) {		
		if (t.isLexical) return 0;
		int result = t.parent==null ? 0 : 1;
		for(TSNodeLabel d : t.daughters) {
			result += countInternalNodes(d);
		}
		return result;
	}


	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void computeSubTrees(int sentenceLength) {
		int startingIndex = 1;
		//int sentenceLength = 12;
		ArrayList<TSNodeLabel> result = getBinaryTrees(startingIndex,sentenceLength);
		//Hashtable<String, int[]> allSubtresTable = new Hashtable<String, int[]>();
						
		BigInteger totalSubtreesTokens = BigInteger.ZERO;
		BigInteger treeSubtreesTokensMin = BigInteger.valueOf(Integer.MAX_VALUE);
		BigInteger treeSubtreesTokensMax = BigInteger.ZERO;		
		long treeSubtreesTypesMin = Long.MAX_VALUE;
		long treeSubtreesTypesMax = 0l;
		int internalNodesMax = -1;
		int internalNodesMin = Integer.MAX_VALUE;
		for(TSNodeLabel t : result) {			
			Hashtable<String, int[]> allSubtresTree = addAllSubtrees(t);
			int currentSubtreesTypes = allSubtresTree.size();
			//increaseAll(allSubtresTree, allSubtresTable);
			BigInteger currentSubtrees = t.countTotalFragments()[1];			
			if (currentSubtrees.compareTo(treeSubtreesTokensMin)==-1) treeSubtreesTokensMin = BigInteger.ZERO.add(currentSubtrees);
			if (currentSubtrees.compareTo(treeSubtreesTokensMax)==1) treeSubtreesTokensMax = BigInteger.ZERO.add(currentSubtrees);
			totalSubtreesTokens = totalSubtreesTokens.add(currentSubtrees);			
			if (currentSubtreesTypes < treeSubtreesTypesMin) treeSubtreesTypesMin = currentSubtreesTypes;
			if (currentSubtreesTypes > treeSubtreesTypesMax) treeSubtreesTypesMax = currentSubtreesTypes;
			int currentInternalNodes = countInternalNodes(t);
			if (currentInternalNodes < internalNodesMin) internalNodesMin = currentInternalNodes;
			if (currentInternalNodes > internalNodesMax) internalNodesMax = currentInternalNodes;			
			//System.out.println(t);
			//System.out.println("Tokens: " + currentSubtrees);
			//System.out.println("Types: " + currentSubtreesTypes);
		}
		int totalSubtreesTypes = allSubtresTable.size();
		System.out.println("---------------------------------");
		System.out.println("Sentence Length: " + sentenceLength);
		System.out.println("Number of binary trees: " + result.size());
		System.out.println("---------------------------------");
		System.out.println("Max internal nodes: " + internalNodesMax);
		System.out.println("Min internal nodes: " + internalNodesMin);
		System.out.println("---------------------------------");
		System.out.println("Tree Subtree Tokens Min: " + treeSubtreesTokensMin);
		System.out.println("Tree Subtree Tokens Max: " + treeSubtreesTokensMax);
		System.out.println("Total Subtrees Tokens: " + totalSubtreesTokens);
		System.out.println("---------------------------------");
		System.out.println("Tree Subtree Types Min: " + treeSubtreesTypesMin);
		System.out.println("Tree Subtree Types Max: " + treeSubtreesTypesMax);
		System.out.println("Total Subtrees Types: " + totalSubtreesTypes);
		//Utility.printHashTableInt(allSubtresTable);
		System.out.println("---------------------------------");

	}
	
	public static void main(String[] args) {
		for(int sentenceLength=15; sentenceLength<=32; sentenceLength++) {			
			computeSubTrees(sentenceLength);
		}
		//computeSubTrees(8);
	}



}
