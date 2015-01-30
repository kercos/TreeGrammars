package tsg.estimates;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class RetrieveBonnemaCounts extends Thread {

	public static int numberOfTreesPerThreads = 10;	
	static boolean debug = false;
	
	File fragmentsFile, outputFile;	
	int threads=1;
		
	long fragmentReadCounter, fragmentWrittenCounter;
	long currentIndex;
	PrintProgress progress;
	ArrayList<TSNodeLabelIndex> treebank;
	ArrayList<TSNodeLabel> fragmentList;
	Hashtable<TSNodeLabel, double[]> finalFragmentsCount;
	Iterator<TSNodeLabelIndex> treeIterator;
	BigInteger minDerivations, maxDerivations;
	
	public RetrieveBonnemaCounts(ArrayList<TSNodeLabelIndex> treebank, File fragmentsFile,
			File outputFile, int threads) {
		
		this.treebank = treebank;
		this.fragmentsFile = fragmentsFile;
		this.outputFile = outputFile;
		this.threads = threads;
		this.treeIterator = treebank.iterator();
		minDerivations = new BigInteger("9999999999");		
		maxDerivations = new BigInteger("-1");
	}
	
	public RetrieveBonnemaCounts(ArrayList<TSNodeLabelIndex> treebank,
			ArrayList<TSNodeLabel> fragments) {
		this.treebank = treebank;
		this.fragmentList = fragments;
		this.treeIterator = treebank.iterator();
		minDerivations = new BigInteger("9999999999");		
		maxDerivations = new BigInteger("-1");
	}

	public void run() {		
		try {
			getFragmentList();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		retriveBonnemaCounts();
		writeFragmentsToFile();
		
	}
	
	
	private void getFragmentList() throws Exception {
		
		Parameters.reportLine("Extracting fragments from file: " + fragmentsFile);
		progress = new PrintProgress("Progress:", 10000, 0);		
		
		Scanner fragmentsScanner = FileUtil.getScanner(fragmentsFile);
		fragmentList = new ArrayList<TSNodeLabel>();
		while(fragmentsScanner.hasNextLine()) {
			progress.next();
			String line = fragmentsScanner.nextLine();
			if (line.equals("")) continue;			
			String[] lineSplit = line.split("\t");
			TSNodeLabel fragment = new TSNodeLabel(lineSplit[0], false);
			fragmentList.add(fragment);
		}
		progress.end();
		Parameters.reportLine("Extracted fragments: " + fragmentList.size());
	}

	public void retriveBonnemaCounts() {
		
		finalFragmentsCount = new Hashtable<TSNodeLabel, double[]>();
		
		Parameters.reportLineFlush("Retrieving Goodman Counts");		
		progress = new PrintProgress("Extracting from tree:", 100, 0);
		
		try {
			if (threads==1) {
				updateTableWithFragmCounts(finalFragmentsCount, treebank);
			}
			else {
				startMultiThreads();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		progress.end();
	}
		
	
	
	private void updateTableWithFragmCounts(
			Hashtable<TSNodeLabel, double[]> tableToUpdate,
			ArrayList<TSNodeLabelIndex> trees) {
		
		int treeindex = 0;
		
		for(TSNodeLabelIndex tree : trees) {			
			
			treeindex++;
			
			TSNodeLabelStructure treeStructure = new TSNodeLabelStructure(tree);			
			int length = treeStructure.length();
			
			// frontier indexes for the list of fragments rooted in the corresponding node index			
			Vector<Hashtable<TSNodeLabel, BitSet>> nodeStartingFragments = 
				new Vector<Hashtable<TSNodeLabel, BitSet>>(length);
			for(int i=0; i<length; i++) {
				nodeStartingFragments.add(new Hashtable<TSNodeLabel, BitSet>());			
			}			 
			
			for(TSNodeLabel fragment : fragmentList) {				
				ArrayList<BitSet> rootFrontiers = getRootFrontierIndexes(treeStructure, fragment);
				if (rootFrontiers==null)
					continue;
				for(BitSet rf : rootFrontiers) {
					int root = rf.nextSetBit(0);
					rf.clear(root); // remove the root, keep the frontiers
					nodeStartingFragments.get(root).put(fragment, rf);
				}				
			}
			
			// number of derivation starting from this node
			BigInteger[] nodeStartingDerivations = new BigInteger[length];
			// number of derivation arriving at this node
			BigInteger[] nodeArrivingDerivations = new BigInteger[length];
			for(int i=0; i<length; i++) {
				nodeStartingDerivations[i] = BigInteger.ZERO;
				nodeArrivingDerivations[i] = BigInteger.ZERO;
			}
			nodeArrivingDerivations[0] = BigInteger.ONE;
			
			BitSet lexIndex = new BitSet();
			ArrayList<TSNodeLabel> lexArray = tree.collectLexicalItems();
			for(TSNodeLabel l : lexArray) {
				lexIndex.set(((TSNodeLabelIndex)l).index);
			}
			
			int[] nodesFromLeavesToTop = Utility.countDownArray(length);
			int[] nodesFromTopToLeaves = Utility.countUpArray(length);
			
			
			updateDerivationsStarting(nodeStartingFragments, 
					nodeStartingDerivations, lexIndex, nodesFromLeavesToTop);
						
			updateDerivationsArriving(nodeStartingFragments, 
					nodeArrivingDerivations, lexIndex, nodesFromTopToLeaves);
			
						
			BigInteger totalDerivationsInTree = nodeStartingDerivations[0];
			
			
			if (totalDerivationsInTree.compareTo(BigInteger.ZERO) <= 0) {
				System.err.println("Tot der = " + totalDerivationsInTree + ", lex length = " + lexIndex.cardinality() + " tree index: " + treeindex);
			}
			
			
			
			if (totalDerivationsInTree.compareTo(minDerivations)<0)
				minDerivations = totalDerivationsInTree;
			if (totalDerivationsInTree.compareTo(maxDerivations)>0)
				maxDerivations = totalDerivationsInTree;
			
			//System.out.println("Total tree derivations: " + totalDerivationsInTree);
			
			/*
			System.out.println("Starting derivations from node:");
			for(int i=0; i<nodeStartingDerivations.length; i++) {
				System.out.println(i + "\t" + nodeStartingDerivations[i]);			
			}
			
			System.out.println("Arriving derivations from node:");
			for(int i=0; i<nodeStartingDerivations.length; i++) {
				System.out.println(i + "\t" + nodeArrivingDerivations[i]);			
			}
			*/
			
			int rootIndex = 0;
			for(Hashtable<TSNodeLabel, BitSet> startingFragments : nodeStartingFragments) {				
				for(Entry<TSNodeLabel,BitSet> e : startingFragments.entrySet()) {			
					BigInteger derivationsWithFragment = nodeArrivingDerivations[rootIndex];
					TSNodeLabel fragment = e.getKey();
					BitSet frontiers = e.getValue();
					int frontierIndex = frontiers.nextSetBit(0);
					do {
						if (!lexIndex.get(frontierIndex))
							derivationsWithFragment = derivationsWithFragment.multiply(
									nodeStartingDerivations[frontierIndex]);
						frontierIndex = frontiers.nextSetBit(++frontierIndex);
					} while(frontierIndex!=-1);
					double ratio = new BigDecimal(derivationsWithFragment).divide(
							new BigDecimal(totalDerivationsInTree),MathContext.DECIMAL128).doubleValue();					
					Utility.increaseInTableDoubleArray(tableToUpdate, fragment, ratio);
					//System.out.println(fragment + "\t" + derivationsWithFragment + "\t" + ratio);
				}
				rootIndex++;
			}				
		}
	}
	
	

	private static void updateDerivationsStarting(
			Vector<Hashtable<TSNodeLabel, BitSet>> nodeStartingFragments,			 
			BigInteger[] nodeStartingDerivations, BitSet lexIndex, int[] nodesFromLeavesToTop) {
							
		for(int nodeIndex : nodesFromLeavesToTop) {
			if (lexIndex.get(nodeIndex))
				continue;
			Hashtable<TSNodeLabel, BitSet> startingFragments = nodeStartingFragments.get(nodeIndex);
			for(Entry<TSNodeLabel, BitSet> e : startingFragments.entrySet()) {			
				BitSet frontiers = e.getValue();
				BigInteger startingDerivations = BigInteger.ONE;
				int frontierIndex = frontiers.nextSetBit(0);
				do {
					if (!lexIndex.get(frontierIndex))
						startingDerivations = startingDerivations.multiply(nodeStartingDerivations[frontierIndex]);
					frontierIndex = frontiers.nextSetBit(++frontierIndex);
				} while(frontierIndex!=-1);
				nodeStartingDerivations[nodeIndex] = nodeStartingDerivations[nodeIndex].add(startingDerivations);				
			}
		}			
		
	}


	private static void updateDerivationsArriving(
			Vector<Hashtable<TSNodeLabel, BitSet>> nodeStartingFragments,   
			BigInteger[] nodeArrivingDerivations, BitSet lexIndex, int[] nodesFromTopToLeaves) {
		
		for(int nodeIndex :  nodesFromTopToLeaves) {			
			if (lexIndex.get(nodeIndex)) 
				continue;
			BigInteger arrivingDerivationsRoot = nodeArrivingDerivations[nodeIndex];
			Hashtable<TSNodeLabel, BitSet> startingFragments = nodeStartingFragments.get(nodeIndex);
			for(Entry<TSNodeLabel, BitSet> e : startingFragments.entrySet()) {
				BitSet frontiers = e.getValue();
				int frontierIndex = frontiers.nextSetBit(0);
				do {
					if (!lexIndex.get(frontierIndex)) {
						nodeArrivingDerivations[frontierIndex] = 
							nodeArrivingDerivations[frontierIndex].add(arrivingDerivationsRoot);
					}					
					frontierIndex = frontiers.nextSetBit(++frontierIndex);
				} while(frontierIndex!=-1);								
			}
		}
		
	}

		
	private ArrayList<BitSet> getRootFrontierIndexes(TSNodeLabelStructure treeStructure,
			TSNodeLabel fragment) {
		
		ArrayList<BitSet> result = new ArrayList<BitSet>();
		for(TSNodeLabelIndex treeIndex : treeStructure.structure) {
			if (fragment.sameLabelAndDaughersLabels(treeIndex)) {				
				BitSet rf = new BitSet();				
				rf.set(treeIndex.index);
				boolean present = true;
				for(int i=0; i<treeIndex.prole(); i++) {
					if (!addFrontierIndexes( (TSNodeLabelIndex)treeIndex.daughters[i], 
							fragment.daughters[i], rf)) {
						present = false;
						break;
					}
				}
				if (present && rf.cardinality()>1) 
					result.add(rf);
			}
		}
		return result;
	}
	
	private boolean addFrontierIndexes(TSNodeLabelIndex treeIndex,
			TSNodeLabel fragment, BitSet rf) {		
		
		boolean terminal = fragment.isTerminal();
		if (!terminal) {
			if (!fragment.sameLabelAndDaughersLabels(treeIndex))
				return false;
		}
		else {
			if (fragment.sameLabel(treeIndex)) {
				rf.set(treeIndex.index);
				return true;
			}
			
		}
					
		for(int i=0; i<treeIndex.prole(); i++) {
			if (!addFrontierIndexes( (TSNodeLabelIndex)treeIndex.daughters[i], 
					fragment.daughters[i], rf)) return false;
		}
		return true;
		
	}
	
	
	
	private void startMultiThreads() throws Exception {
		CountFragmentsThread[] threadsArray = new CountFragmentsThread[threads];
		
		int lastThreadIndex = threads-1;
		for(int t=0; t<threads; t++) {
			CountFragmentsThread newCounterThread = new CountFragmentsThread();
			threadsArray[t] = newCounterThread;
			if (t==lastThreadIndex) newCounterThread.run();
			else newCounterThread.start();
		}
		
		for(int i=0; i<lastThreadIndex; i++) {
			try {
				threadsArray[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	
	
	private void writeFragmentsToFile() {
		Parameters.reportLine("Printing fragments with new counts to file: " + outputFile);		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<TSNodeLabel,double[]> e : finalFragmentsCount.entrySet()) {
			TSNodeLabel fragment = e.getKey();
			double count = e.getValue()[0];
			pw.println(fragment.toString(false, true) + "\t" + count);			
		}
		pw.close();
		
	}

	private synchronized ArrayList<TSNodeLabelIndex> getNextTreeLoad() {
				
		if (!treeIterator.hasNext()) {
			return null;
		}
		ArrayList<TSNodeLabelIndex> treesForThread = new ArrayList<TSNodeLabelIndex>(numberOfTreesPerThreads);
		int i = 0;		
		while (treeIterator.hasNext()) {
			if (i==numberOfTreesPerThreads) break;
			treesForThread.add(treeIterator.next());
			i++;
		}
		progress.next(i);			
		return treesForThread;				
	}
	
	
	private synchronized void addFragmentsToFinalTable(
			Hashtable<TSNodeLabel, double[]> threadFragmentCount) {
				
		for(Entry<TSNodeLabel,double[]> e : threadFragmentCount.entrySet()) {
			TSNodeLabel fragment = e.getKey();
			Utility.increaseInTableDoubleArray(finalFragmentsCount, fragment, e.getValue()[0]);
		}
		
	}
	
	
	private class CountFragmentsThread extends Thread {
		
				
		public void run(){
						
			ArrayList<TSNodeLabelIndex> treeLoad = null;
			while( (treeLoad=getNextTreeLoad()) != null ) {
				Hashtable<TSNodeLabel, double[]> threadFragmentCount = new Hashtable<TSNodeLabel, double[]>();
				updateTableWithFragmCounts(threadFragmentCount, treeLoad);
				addFragmentsToFinalTable(threadFragmentCount);
			}
		}		
				
	}
		
	public static void main2(String[] args) throws Exception {
		
		debug = true;
		TSNodeLabelIndex tree = new TSNodeLabelIndex("(A ( B (D h i) (E l m)) (C (F n o) (G p q)))");
		ArrayList<TSNodeLabelIndex> treebank = new ArrayList<TSNodeLabelIndex>();
		treebank.add(tree);
		
		ArrayList<TSNodeLabel> fragmentsList = new ArrayList<TSNodeLabel> ( 
			Arrays.asList(
					new TSNodeLabel[]{
							new TSNodeLabel("(A B C)",false),							
							new TSNodeLabel("(B D E)",false),
							new TSNodeLabel("(C F G)",false),
							new TSNodeLabel("(D \"h\" \"i\")",false),
							new TSNodeLabel("(E \"l\" \"m\")",false),
							new TSNodeLabel("(F \"n\" \"o\")",false),
							new TSNodeLabel("(G \"p\" \"q\")",false),
							
							new TSNodeLabel("(A B (C F G))",false),
							new TSNodeLabel("(B (D \"h\" \"i\") (E \"l\" \"m\"))",false),
					}));
		
		RetrieveBonnemaCounts RCF = new RetrieveBonnemaCounts(treebank, fragmentsList);
		RCF.retriveBonnemaCounts();
		
		
	}
	
	public static void main1(String[] args) throws Exception {
		main1(new String[]{
				"tmp/Bonnema/trainingTreebank_UK_first100.mrg",
				"tmp/Bonnema/fragmentsAndCfgRules.txt",
				"tmp/Bonnema/fragmentsAndCfgRules_bonnema.txt",
		});
	}
	
	public static void main(String[] args) throws Exception {
		
		long time = System.currentTimeMillis(); 

		String usage = "USAGE: java RetrieveGoodamnCounts [-threads:1] " +
				"treebankFile fragmentsFile outputFile";
		
		String threadsOption = "-threads:";
		int threads = 1;		
		int length = args.length; 
		
		if (length<3 || length>4) {
			System.err.println("Incorrect number of arguments");
			System.err.println(usage);
			return;
		}
		
		File treebankFile=null, inputFile=null, outputFile=null;
		for(String option : args) {
			if (option.startsWith(threadsOption))
				threads = ArgumentReader.readIntOption(option);
			else if (treebankFile==null) treebankFile = new File(option);
			else if (inputFile==null) inputFile = new File(option);
			else outputFile = new File(option);
		}
				
		if (!treebankFile.exists()) {
			System.err.println("Treebank File does not exist");
			System.err.println(usage);
			return;
		}
		if (!inputFile.exists()) {
			System.err.println("Input File does not exist");
			System.err.println(usage);
			return;
		}

		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);		
		ArrayList<TSNodeLabelIndex> treebankIndex = new ArrayList<TSNodeLabelIndex>();
		for(TSNodeLabel t : treebank) {
			treebankIndex.add(new TSNodeLabelIndex(t));
		}
		
		RetrieveBonnemaCounts RCF = new RetrieveBonnemaCounts(treebankIndex, inputFile, outputFile, threads);
		RCF.run();		
		
		System.out.println("Min derivations: " + RCF.minDerivations);
		System.out.println("Max derivations: " + RCF.maxDerivations);				
		System.out.println("Took: " + (System.currentTimeMillis() - time)/1000 + "seconds.");
	}
	
	
}
