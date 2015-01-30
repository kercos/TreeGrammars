package tsg.estimates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.ArgumentReader;
import util.PrintProgress;
import util.StringLong;
import util.Utility;
import util.file.FileUtil;

public class RetrieveGoodmanCounts extends Thread {

	public static int numberOfTreesPerThreads = 50;	
	
	File fragmentsFile, outputFile;	
	int threads;
		
	long fragmentReadCounter, fragmentWrittenCounter;
	long currentIndex;
	PrintProgress progress;
	ArrayList<TSNodeLabel> treebank, fragmentList;
	Hashtable<TSNodeLabel, double[]> finalFragmentsCount;
	Iterator<TSNodeLabel> treeIterator;
	
	public RetrieveGoodmanCounts(ArrayList<TSNodeLabel> treebank, File fragmentsFile,
			File outputFile, int threads) {
		
		this.treebank = treebank;
		this.fragmentsFile = fragmentsFile;
		this.outputFile = outputFile;
		this.threads = threads;
		this.treeIterator = treebank.iterator();;		 
	}
	
	public void run() {		
		try {
			getFragmentList();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		retriveGoodmanCounts();
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

	public void retriveGoodmanCounts() {
		
		finalFragmentsCount = new Hashtable<TSNodeLabel, double[]>();
		
		Parameters.reportLineFlush("Retrieving Goodman Counts");		
		progress = new PrintProgress("Extracting from tree:", numberOfTreesPerThreads, 0);
		
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
			ArrayList<TSNodeLabel> trees) {
		
		for(TSNodeLabel tree : trees) {
			Hashtable<TSNodeLabel, int[]> treeFragmentCount = new Hashtable<TSNodeLabel, int[]>(); 
			int totalCountsInTree = 0;
			for(TSNodeLabel fragment : fragmentList) { 
				int count = tree.countRecursiveFragment(fragment);
				if (count>0) {
					totalCountsInTree+=count;
					Utility.increaseInTableInt(treeFragmentCount, fragment, count);					
				}
			}
			for(Entry<TSNodeLabel,int[]> e : treeFragmentCount.entrySet()) {
				TSNodeLabel fragment = e.getKey();
				double ratio = (double)e.getValue()[0] / totalCountsInTree;
				Utility.increaseInTableDoubleArray(tableToUpdate, fragment, ratio);
			}
		}
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

	private synchronized ArrayList<TSNodeLabel> getNextTreeLoad() {
				
		if (!treeIterator.hasNext()) {
			return null;
		}
		ArrayList<TSNodeLabel> treesForThread = new ArrayList<TSNodeLabel>(numberOfTreesPerThreads);
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
						
			ArrayList<TSNodeLabel> treeLoad = null;
			while( (treeLoad=getNextTreeLoad()) != null ) {
				Hashtable<TSNodeLabel, double[]> threadFragmentCount = new Hashtable<TSNodeLabel, double[]>();
				updateTableWithFragmCounts(threadFragmentCount, treeLoad);
				addFragmentsToFinalTable(threadFragmentCount);
			}
		}		
				
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
		RetrieveGoodmanCounts RCF = new RetrieveGoodmanCounts(treebank, inputFile, outputFile, threads);
		RCF.run();
		
		System.out.println("Took: " + (System.currentTimeMillis() - time)/1000 + "seconds.");
	}
	
	
}
