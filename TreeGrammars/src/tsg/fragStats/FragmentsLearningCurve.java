package tsg.fragStats;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;

import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.ObjectInteger;
import util.PrintProgress;
import util.file.FileUtil;

public class FragmentsLearningCurve {
	
	HashSet<String> fragmentsSet;
	ArrayList<TSNodeLabel> treebank;
	ArrayList<TSNodeLabel> fragments;
	int[] grammarSizeArray;
	int currentGrammarSize;
	int currentIndex;
	PrintProgress progress;
	PriorityBlockingQueue<ObjectInteger<HashSet<String>>> fragmentsSetWaitingToBeChecked;
	
	public FragmentsLearningCurve(File treebankFile, File fragmentsFile, File outputFile, int threads) throws Exception {
		fragmentsSet = new HashSet<String>(); 
		treebank = Wsj.getTreebank(treebankFile);
		fragments = new ArrayList<TSNodeLabel>();
		
		Scanner fragmentScanner = FileUtil.getScanner(fragmentsFile);		 
		while(fragmentScanner.hasNextLine()) {
			String fragString = fragmentScanner.nextLine();
			TSNodeLabel frag = new TSNodeLabel(fragString, false);
			fragments.add(frag);
		}
		
		System.out.println("Number of fragments: " + fragments.size());
		
		grammarSizeArray = new int[treebank.size()];
						
		progress = new PrintProgress("Reading from treebank:");
		if (threads<=1) 
			extractFragmentsSingleThread(threads);
		else {
			fragmentsSetWaitingToBeChecked = new PriorityBlockingQueue<ObjectInteger<HashSet<String>>>();
			processInputOutputMultipleThreads(threads);
		}
		progress.end();
		
		printStats(outputFile);
		
		
	}
	
	private void extractFragmentsSingleThread(int threads) {			
		for(TSNodeLabel t : treebank) {			
			for(TSNodeLabel f : fragments) {
				if (t.containsRecursiveFragment(f)) {
					String fragString = f.toString(false, true);
					if (fragmentsSet.add(fragString)) {
						currentGrammarSize++;
					}
				}
			}	
			grammarSizeArray[currentIndex++] = currentGrammarSize;
			progress.next();
		}				
	}
	
	private void processInputOutputMultipleThreads(int threads) throws Exception {

		int fragmentsPerThread = treebank.size() / threads;
		int remainer = treebank.size() % threads;
		boolean hasRemainer = remainer>0;
		
		CountFragmentsThread[] threadsArray = new CountFragmentsThread[threads];
		
		int lastThreadIndex = threads-1;
		int startIndex = 0;
		
		for(int t=0; t<threads; t++) {
			boolean isLast = (t==lastThreadIndex);
			int numberOfTrees = (isLast || !hasRemainer) ? fragmentsPerThread : (fragmentsPerThread+1);
			CountFragmentsThread newCounterThread = new CountFragmentsThread(startIndex, numberOfTrees);
			threadsArray[t] = newCounterThread;
			if (isLast) newCounterThread.run();
			else newCounterThread.start();
			startIndex += numberOfTrees;
		}
		
		for(int i=0; i<lastThreadIndex; i++) {
			try {
				threadsArray[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
				
	}

	private void printStats(File outputFile) {
		System.out.println("Writing stats to: " + outputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(int i=0; i<treebank.size(); i++) {
			pw.println((i+1) + "\t" + grammarSizeArray[i]);
		}
		pw.close();		
	}

	
	private class CountFragmentsThread extends Thread {
		
		int firstIndex,numberOfTrees;		
		HashSet<String> fragmentsSetThread;
				
		public CountFragmentsThread(int firstIndex, int numberOfTrees) {			
			this.firstIndex = firstIndex;
			this.numberOfTrees = numberOfTrees;
			this.fragmentsSetThread = new HashSet<String>();
		}
		
		public void run(){
			//System.out.println("Starting thread starting from tree " + firstIndex + " with size " + numberOfTrees);
			int index = firstIndex;
			int counter = 0;
			ListIterator<TSNodeLabel> iter = treebank.listIterator(firstIndex);			
			while(iter.hasNext() && counter<numberOfTrees) {
				TSNodeLabel t = iter.next();
				counter++;
				HashSet<String> newFragments = new HashSet<String>();
				for(TSNodeLabel f : fragments) {
					if (t.containsRecursiveFragment(f)) {
						String fragString = f.toString(false, true);
						if (fragmentsSetThread.add(fragString)) {
							newFragments.add(fragString);
						}
					}
				}
				//System.out.println("UpdatingFragemntsCounts " + index);				
				updateFragmentsCounts(index, newFragments);				
				index++;
			}
		}
				
	}
	
	public synchronized void updateFragmentsCounts(int index, HashSet<String> newFragments) {
		if (checkIndexAndUpdate(index, newFragments)) {			
			while(!fragmentsSetWaitingToBeChecked.isEmpty()) {
				ObjectInteger<HashSet<String>> nextElement = fragmentsSetWaitingToBeChecked.peek();
				index = nextElement.getInteger();
				newFragments = nextElement.getObject();				
				if (!checkIndexAndUpdate(index, newFragments)) {				
					break;
				}
				fragmentsSetWaitingToBeChecked.poll();
		    }
		}		
		else {
			ObjectInteger<HashSet<String>> fragmentsSetPos = new ObjectInteger<HashSet<String>>(newFragments, index);
			fragmentsSetWaitingToBeChecked.add(fragmentsSetPos);
		}
		
	}
	
	private boolean checkIndexAndUpdate(int index, HashSet<String> newFragments) {
		if (index==currentIndex) {			
			for(String f : newFragments) {
				if (fragmentsSet.add(f))
					currentGrammarSize++;
			}
			grammarSizeArray[currentIndex++] = currentGrammarSize;
			progress.next();
			return true;
		}
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		File treebankFile = new File(args[0]);
		File fragmentsFile = new File(args[1]);
		File outputFile = new File(args[2]);
		int threads = Integer.parseInt(args[3]);
		new FragmentsLearningCurve(treebankFile, fragmentsFile, outputFile, threads);		
	}
	
}
