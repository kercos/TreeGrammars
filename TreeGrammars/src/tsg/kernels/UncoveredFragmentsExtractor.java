package tsg.kernels;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.corpora.Wsj;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class UncoveredFragmentsExtractor extends Thread {

	ArrayList<TSNodeLabel> treebank;
	Iterator<TSNodeLabel> treebankIterator;
	File fragmentsFile, outputFile;
	ArrayList<TSNodeLabel> fragmentsList;	
	int threads;
	PrintProgress progress;
	Hashtable<String, int[]> unseenFragmentsTable;
	
	public UncoveredFragmentsExtractor(File treebankFile, File fragmentsFile,
			File outputFile, int threads) throws Exception {
		this(Wsj.getTreebank(treebankFile), fragmentsFile, outputFile, threads);		
	}
	
	public UncoveredFragmentsExtractor (ArrayList<TSNodeLabel> treebank, File fragmentsFile,
			File outputFile, int threads) {
		this.treebank = treebank;
		this.fragmentsFile = fragmentsFile;
		this.outputFile = outputFile;
		this.threads = threads;
		treebankIterator = treebank.iterator();
		unseenFragmentsTable = new Hashtable<String, int[]>();
	}
	
	public void run() {
		extractUnseenFragments();
	}
	
	private void extractUnseenFragments() {			
		fragmentsList = new ArrayList<TSNodeLabel>();
		Scanner fragmentScan = FileUtil.getScanner(fragmentsFile);
		while(fragmentScan.hasNextLine()) {
			String line = fragmentScan.nextLine();
			String[] treeFreq = line.split("\t");			
			String fragment = treeFreq[0];
			try {
				fragmentsList.add(new TSNodeLabel(fragment, false));
			} catch (Exception e) {				
				e.printStackTrace();
				Parameters.reportError("Problems in fragmentsFile: " + line);
				Parameters.reportError(e.getMessage());
				return;
			}
		}			
		Parameters.reportLineFlush("Extracting unseen fragments from treebank.");
		Parameters.reportLineFlush("Total trees in treebank: " + treebank.size());
		progress = new PrintProgress("Extracting from tree:", 100, 0);				
		extractUnseenFragmentsWithThreads();		
		progress.end();
		
		PrintWriter unseenFragmentsPW = FileUtil.getPrintWriter(outputFile);
		for(Entry<String,int[]> e : unseenFragmentsTable.entrySet()) {
			String newLine = e.getKey() + "\t" + e.getValue()[0];
			unseenFragmentsPW.println(newLine);
		}
		unseenFragmentsPW.close();
	}
	
	private void extractUnseenFragmentsWithThreads() {
		
		FragmentsExtractorThread[] threadsArray = new FragmentsExtractorThread[threads];
		for(int i=0; i<threads; i++) {
			threadsArray[i] = new FragmentsExtractorThread();
		}
		
		int lastThreadIndex = threads-1;
		for(int i=0; i<lastThreadIndex; i++) {
			threadsArray[i].start();
		}
		threadsArray[lastThreadIndex].run();
		
		for(int i=0; i<lastThreadIndex; i++) {
			try {
				threadsArray[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private synchronized TSNodeLabel getNextTree() {
		if (!treebankIterator.hasNext()) return null;
		progress.next();
		return treebankIterator.next();
	}
	
	private synchronized void addFragmentInTable(String fragmentLine) {
		Utility.increaseInTableInt(unseenFragmentsTable, fragmentLine);
	}
	
	protected class FragmentsExtractorThread extends Thread {
		
		public void run() {
			TSNodeLabel t = null;
			while ( (t = getNextTree()) != null) {
				getUncoveredFragments(t);
			}			
		}
		
		private void getUncoveredFragments(TSNodeLabel t) {
			TSNodeLabelIndex treeIndex = new TSNodeLabelIndex(t);
			int totalNodes = treeIndex.countAllNodes();
			BitSet coveredNodesSet = new BitSet();
			boolean allCovered = false;
			for(TSNodeLabel fragment : fragmentsList) {
				setCoveredNodes(treeIndex, fragment, coveredNodesSet);
				if (coveredNodesSet.cardinality()==totalNodes) {
					allCovered = true;
					break;
				}
			}
			if (allCovered) return;
			printUncoveredFragmentsRecursive(treeIndex, coveredNodesSet);				
		}
		
		
		private void printUncoveredFragmentsRecursive(TSNodeLabelIndex treeIndex, BitSet coveredNodesSet) {
			if (!coveredNodesSet.get(treeIndex.index)) {
				if (treeIndex.isPreLexical()) {
					addFragmentInTable(treeIndex.toString(false, true));
					return;
				}
				ArrayList<TSNodeLabelIndex> nonLexicalFroniers = new ArrayList<TSNodeLabelIndex>(); 
				TSNodeLabel subTree = getSubTreeAndInternalFrontiers(treeIndex, coveredNodesSet, nonLexicalFroniers);
				addFragmentInTable(subTree.toString(false, true));
				for(TSNodeLabelIndex f : nonLexicalFroniers) {
					TSNodeLabelIndex fNodeIndex = (TSNodeLabelIndex)f;
					printUncoveredFragmentsRecursive(fNodeIndex, coveredNodesSet);				
				}
				return;
			}	
			if (treeIndex.isPreLexical()) return;
			for(TSNodeLabel d : treeIndex.daughters) {
				TSNodeLabelIndex dNodeIndex = (TSNodeLabelIndex)d;
				printUncoveredFragmentsRecursive(dNodeIndex, coveredNodesSet);
			}		
		}
				
	}
	
	private static void setCoveredNodes(TSNodeLabelIndex t, TSNodeLabel fragment, BitSet coveredNodesSet) {		
		BitSet fragmentSet = new BitSet();
		if (t.sameLabelAndDaughersLabels(fragment)) {
			if (setCoveredNodesNonRecursive(t, fragment, fragmentSet)) {
				coveredNodesSet.or(fragmentSet);
				//if (top) coveredNodesSet.set(t.index); // 0
			}
		}		
		for(TSNodeLabel d : t.daughters) {
			TSNodeLabelIndex di = (TSNodeLabelIndex)d;
			if (di.isLexical) continue;
			setCoveredNodes(di, fragment, coveredNodesSet);
		}
	}
	
	private static boolean setCoveredNodesNonRecursive(TSNodeLabelIndex t, TSNodeLabel fragment, BitSet set) {
		set.set(t.index);
		int treeProle = t.prole();
		for(int i=0; i<treeProle; i++) {
			TSNodeLabelIndex treeDaughterIndex = (TSNodeLabelIndex)t.daughters[i];
			TSNodeLabel fragmentDaughter = fragment.daughters[i];			
			if (treeDaughterIndex.isLexical || fragmentDaughter.isTerminal()) {
				if (!treeDaughterIndex.sameLabel(fragmentDaughter)) return false;			
			}
			else  {
				if (!treeDaughterIndex.sameLabelAndDaughersLabels(fragmentDaughter)) return false;
				if (!setCoveredNodesNonRecursive(treeDaughterIndex, fragmentDaughter, set)) return false;
			}			
		}
		return true;					
	}
	
	public static TSNodeLabel getSubTreeAndInternalFrontiers(TSNodeLabelIndex thisNode,
			BitSet coveredNodesSet, ArrayList<TSNodeLabelIndex> frontiers) {
		TSNodeLabel result = thisNode.nonRecursiveCopy();
		if (thisNode.isLexical) return result;
		result.daughters = new TSNodeLabel[thisNode.daughters.length];
		for(int i=0; i<thisNode.daughters.length; i++) {
			TSNodeLabelIndex dNodeIndex = ((TSNodeLabelIndex)thisNode.daughters[i]);
			TSNodeLabel dSubTree = null;
			if (dNodeIndex.isLexical || !coveredNodesSet.get(dNodeIndex.index)) {
				dSubTree = getSubTreeAndInternalFrontiers(dNodeIndex, coveredNodesSet, frontiers);																
			}
			else {
				if (!dNodeIndex.isPreLexical()) frontiers.add(dNodeIndex);
				dSubTree = dNodeIndex.nonRecursiveCopy();				
			}
			dSubTree.parent = result;
			result.daughters[i] = dSubTree;
		}				
		return result;		
	}

	
	public static void main(String[] args) throws Exception {
		
		File treebankFile = new File(args[0]);
		File fragmentsFile = new File(args[1]);
		File outputFile = new File(args[2]);
		int threads = Integer.parseInt(args[3]);
		
		//File treebankFile = new File("tmp/OneStructureTreebank");
		//File fragmentsFile = new File("tmp/FewFragments");
		//File outputFile = new File("tmp/UnseenFragments");
		
		//File treebankFile = new File("tmp/wsj-02-21_uk1.mrg");
		//File fragmentsFile = new File("tmp/fragments_exactFreq_uk1.txt");
		//File outputFile = new File("tmp/unseenFragmentsUk1");
		
		Parameters.reportLineFlush("Collecting Uncovered Fragments in " + outputFile);
		new UncoveredFragmentsExtractor(treebankFile, fragmentsFile, outputFile, threads).run();
	}
	
}
