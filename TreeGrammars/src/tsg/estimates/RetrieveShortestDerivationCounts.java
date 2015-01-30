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

public class RetrieveShortestDerivationCounts extends Thread {

	public static int numberOfTreesPerThreads = 100;	
	
	File fragmentsFile, outputFile;	
	int threads=1;
		
	PrintProgress progress;
	ArrayList<TSNodeLabelIndex> treebank;
	ArrayList<TSNodeLabel> fragmentList;
	Hashtable<TSNodeLabel, int[]> finalFragmentsCount;
	Iterator<TSNodeLabelIndex> treeIterator;
	
	public RetrieveShortestDerivationCounts(ArrayList<TSNodeLabelIndex> treebank, File fragmentsFile,
			File outputFile, int threads) throws Exception {
		
		this.treebank = treebank;
		this.fragmentsFile = fragmentsFile;
		this.outputFile = outputFile;
		this.threads = threads;
		this.treeIterator = treebank.iterator();
		getFragmentList();
		retriveShortestDerivationCounts();
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

	public void retriveShortestDerivationCounts() {
		
		finalFragmentsCount = new Hashtable<TSNodeLabel, int[]>();
		
		Parameters.reportLineFlush("Retrieving Shortest Derivation Counts");		
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
		
	class NodeShortestDerivation {
		
		int size;
		int index;
		TSNodeLabel startingTree;
		ArrayList<NodeShortestDerivation> subSitesShortestDerivation;
		
		public NodeShortestDerivation(int index) {
			this.index = index;
			size = Integer.MAX_VALUE;
		}
		
		public void checkIfBetterDerivation(TSNodeLabel startingTree, 
				ArrayList<NodeShortestDerivation> subSitesShortestDerivation) {
			int count = 1;
			for(NodeShortestDerivation s : subSitesShortestDerivation) {
				count += s.size;
			}
			if (count < size) {
				size = count;
				this.startingTree = startingTree;
				this.subSitesShortestDerivation = subSitesShortestDerivation;				
			}
		}

		public void extractTreeCount(Hashtable<TSNodeLabel, int[]> tableToUpdate) {
			Utility.increaseInTableInt(tableToUpdate, startingTree);
			for(NodeShortestDerivation s : subSitesShortestDerivation) {
				s.extractTreeCount(tableToUpdate);
			}
			
		}
				
	}
	
	private void updateTableWithFragmCounts(
			Hashtable<TSNodeLabel, int[]> tableToUpdate,
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
			
			NodeShortestDerivation[] finalNodeBestFragment = new NodeShortestDerivation[length];
			
			for(int index = length-1; index>=0; index--) {
				
				if (treeStructure.structure[index].isLexical) {
					continue;
				}
				
				NodeShortestDerivation NSD = new NodeShortestDerivation(index);
				finalNodeBestFragment[index]= NSD;
				
				for(Entry<TSNodeLabel, BitSet> fragSet : nodeStartingFragments.get(index).entrySet()) {
					TSNodeLabel startingTree = fragSet.getKey();
					BitSet subSites = fragSet.getValue();
					ArrayList<NodeShortestDerivation> subSitesDer = new ArrayList<NodeShortestDerivation>(); 
					int frontierIndex = subSites.nextSetBit(0);
					while(frontierIndex!=-1) {
						subSitesDer.add(finalNodeBestFragment[frontierIndex]);
						frontierIndex = subSites.nextSetBit(++frontierIndex);
					};
					NSD.checkIfBetterDerivation(startingTree, subSitesDer);
					
				}
				
			}
									
			NodeShortestDerivation rootDer = finalNodeBestFragment[0];
			rootDer.extractTreeCount(tableToUpdate);
			
		}
	}
	


		
	private ArrayList<BitSet> getRootFrontierIndexes(TSNodeLabelStructure treeStructure,
			TSNodeLabel fragment) {
		
		ArrayList<BitSet> result = new ArrayList<BitSet>();
		for(TSNodeLabelIndex treeIndex : treeStructure.structure) {
			if (fragment.sameLabelAndDaughersLabels(treeIndex)) {				
				BitSet rf = new BitSet();				
				rf.set(treeIndex.index);
				boolean isContained = true;
				for(int i=0; i<treeIndex.prole(); i++) {
					if (!addSubSitesIndexes( (TSNodeLabelIndex)treeIndex.daughters[i], 
							fragment.daughters[i], rf)) {
						isContained = false;
						break;
					}
				}
				if (isContained) result.add(rf);
			}
		}
		return result;
	}
	
	private boolean addSubSitesIndexes(TSNodeLabelIndex treeIndex,
			TSNodeLabel fragment, BitSet rf) {		
		
		boolean terminal = fragment.isTerminal();
		
		if (!terminal) { // and non lexical
			if (!fragment.sameLabelAndDaughersLabels(treeIndex))
				return false;
			for(int i=0; i<treeIndex.prole(); i++) {
				if (!addSubSitesIndexes( (TSNodeLabelIndex)treeIndex.daughters[i], 
						fragment.daughters[i], rf)) return false;
			}
			return true;
		}
		
		// terminal and possibly lexical
		if (fragment.sameLabel(treeIndex)) {
			boolean lexical = fragment.isLexical;
			if (!lexical) rf.set(treeIndex.index);
			return true;
		}
		return false;			
		
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
		for(Entry<TSNodeLabel,int[]> e : finalFragmentsCount.entrySet()) {
			TSNodeLabel fragment = e.getKey();
			int count = e.getValue()[0];
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
			Hashtable<TSNodeLabel, int[]> threadFragmentCount) {
				
		for(Entry<TSNodeLabel,int[]> e : threadFragmentCount.entrySet()) {
			TSNodeLabel fragment = e.getKey();
			Utility.increaseInTableInt(finalFragmentsCount, fragment, e.getValue()[0]);
		}
		
	}
	
	
	private class CountFragmentsThread extends Thread {
		
				
		public void run(){
						
			ArrayList<TSNodeLabelIndex> treeLoad = null;
			while( (treeLoad=getNextTreeLoad()) != null ) {
				Hashtable<TSNodeLabel, int[]> threadFragmentCount = new Hashtable<TSNodeLabel, int[]>();
				updateTableWithFragmCounts(threadFragmentCount, treeLoad);
				addFragmentsToFinalTable(threadFragmentCount);
			}
		}		
				
	}
	
	public static void main1(String[] args) throws Exception {
		String baseDir = "/scratch/fsangati/RESULTS/Debug/";
		main1(new String[]{
			"-threads:1",	
			baseDir + "trainingTreebank_UK_MB.mrg",
			baseDir + "fragments_ALL_exactFreq.txt",
			baseDir + "fragments_ALL_shortestDerEstimate.txt"
		});
	}
	
	
	public static void main(String[] args) throws Exception {
		
		long time = System.currentTimeMillis(); 

		String usage = "USAGE: java RetrieveShortestDerivationCounts [-threads:1] " +
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
		
		RetrieveShortestDerivationCounts RCF = new RetrieveShortestDerivationCounts(treebankIndex, inputFile, outputFile, threads);
		RCF.run();		
				
	}
	
	
}
