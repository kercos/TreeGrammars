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
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class RetrieveNonEmbeddingCounts extends Thread {

	public static int numberOfTreesPerThreads = 10;	
	static boolean debug = false;
	
	File fragmentsFile, outputFile;	
	int threads=1;
		
	long fragmentReadCounter, fragmentWrittenCounter;
	long currentIndex;
	PrintProgress progress;
	ArrayList<TSNodeLabelIndex> treebank;
	ArrayList<TSNodeLabel> fragmentList;
	Hashtable<TSNodeLabel, int[]> finalFragmentsCount;
	Iterator<TSNodeLabelIndex> treeIterator;
	
	public RetrieveNonEmbeddingCounts(ArrayList<TSNodeLabelIndex> treebank, File fragmentsFile,
			File outputFile, int threads) {
		
		this.treebank = treebank;
		this.fragmentsFile = fragmentsFile;
		this.outputFile = outputFile;
		this.threads = threads;
		this.treeIterator = treebank.iterator();
	}
	
	public RetrieveNonEmbeddingCounts(ArrayList<TSNodeLabelIndex> treebank,
			ArrayList<TSNodeLabel> fragments) {
		this.treebank = treebank;
		this.fragmentList = fragments;
		this.treeIterator = treebank.iterator();
	}

	public void run() {		
		try {
			getFragmentList();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		retriveShortDerCounts();
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

	public void retriveShortDerCounts() {
		
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
		
	
	
	private void updateTableWithFragmCounts(
			Hashtable<TSNodeLabel, int[]> tableToUpdate,
			ArrayList<TSNodeLabelIndex> trees) {
		
		int treeindex = 0;
		
		for(TSNodeLabelIndex tree : trees) {			
			
			treeindex++;
			
			TSNodeLabelStructure treeStructure = new TSNodeLabelStructure(tree);			
			int length = treeStructure.length();
			
			// frontier indexes for the list of fragments rooted in the corresponding node index			
			Vector<ArrayList<TSNodeLabel>> nodeStartingFragments = 
				new Vector<ArrayList<TSNodeLabel>>(length);
			for(int i=0; i<length; i++) {
				nodeStartingFragments.add(new ArrayList<TSNodeLabel>());			
			}			 
			
			for(TSNodeLabel fragment : fragmentList) {				
				ArrayList<Integer> rootsIndexes = getRootIndexes(treeStructure, fragment);
				for(int root : rootsIndexes) {
					ArrayList<TSNodeLabel> rootFrag = nodeStartingFragments.get(root); 
					boolean addNew = true;
					ListIterator<TSNodeLabel> rootFragIter = rootFrag.listIterator();
					while(rootFragIter.hasNext()) {
						TSNodeLabel f = rootFragIter.next();
						if (f.containsNonRecursiveFragment(fragment)) {
							addNew = false;
							break;
						}
						if (fragment.containsNonRecursiveFragment(f)) {
							rootFragIter.remove();
						}
					}
					if (addNew) rootFrag.add(fragment);
				}				
			}
			
			for(ArrayList<TSNodeLabel> rootFrag : nodeStartingFragments) {
				for(TSNodeLabel frag : rootFrag) {
					Utility.increaseInTableInt(tableToUpdate, frag);
				}
			}
		}
	}
	
	
	private ArrayList<Integer> getRootIndexes(TSNodeLabelStructure treeStructure,
			TSNodeLabel fragment) {
		
		ArrayList<Integer> result = new ArrayList<Integer>();
		for(TSNodeLabelIndex treeIndex : treeStructure.structure) {
			if (treeIndex.containsNonRecursiveFragment(fragment))
				result.add(treeIndex.index);
		}
		return result;
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
			int count = e.getValue()[0]+1;			
			pw.println(fragment.toString(false, true) + "\t" + count);			
		}
		
		int added = 0;
		Set<TSNodeLabel> fragSet = finalFragmentsCount.keySet();
		for(TSNodeLabel f : fragmentList) {
			if (!fragSet.contains(f)) {
				pw.println(f.toString(false, true) + "\t" + 1);
				added++;
			}
		}
		pw.close();
		
		System.out.println("Count zero fragmnet (set to 1): " + added);		
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
		
		RetrieveNonEmbeddingCounts RCF = new RetrieveNonEmbeddingCounts(treebank, fragmentsList);
		RCF.retriveShortDerCounts();
		
		
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
		
		RetrieveNonEmbeddingCounts RCF = new RetrieveNonEmbeddingCounts(treebankIndex, inputFile, outputFile, threads);
		RCF.run();		
		
		System.out.println("Took: " + (System.currentTimeMillis() - time)/1000 + "seconds.");
	}
	
	
}
