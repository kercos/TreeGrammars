package tsg.kernels;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.ArgumentReader;
import util.PrintProgress;
import util.StringLong;
import util.file.FileUtil;

public class RetrieveCorrectFreqQueue extends RetrieveCorrectFreq {

	public static int numberOfFragmentsPerThreads = 100;
	public static int printProgressEvery = 1000;	
	
	Scanner fragmentScanner;
	Writer fragmentWriter;
	long fragmentReadCounter, fragmentWrittenCounter;
	PriorityBlockingQueue<StringLong> fragmentsWaitingToBeWritten;
	long currentIndex;
	PrintProgress progress;
	
	public RetrieveCorrectFreqQueue(ArrayList<TSNodeLabel> treebank, File fragmentsFile,
			File outputFile, boolean partialFragments, int threads) {
		
		super(treebank, fragmentsFile,outputFile, partialFragments, threads);
		fragmentScanner = FileUtil.getScanner(fragmentsFile);		
	}
	
	public void retriveCorrectFreqResume() {
		Parameters.reportLineFlush("Retrieving Correct Frequencies Resuming from existing file");				
		Scanner scanOutput = FileUtil.getScanner(outputFile);
		File tempFile = new File(outputFile.getAbsolutePath() + ".tmp");
		
		//Parameters.reportLineFlush("Total Fragments: " + FileUtil.getNumberOfLines(fragmentsFile));		
		
		progress = new PrintProgress("Reading already counted fragments", printProgressEvery, 0);		
		String lineOutput = null;
		String lineInput = null;
		if (scanOutput.hasNextLine()) {
			lineInput = fragmentScanner.nextLine();
			lineOutput = scanOutput.nextLine();
			String[] lineInputSplit = lineInput.split("\t");
			String[]  lineOutputSplit = lineOutput.split("\t");			
			if (!lineInputSplit[0].equals(lineOutputSplit[0]) || lineOutputSplit.length<2) {
				Parameters.reportError("Cannot resume files, the two files don't match in the first line!");
				return;
			}
		}
		else {
			Parameters.reportLineFlush("Empty File, rewriting on it...");
			retriveCorrectFreq();
			return;
		}
		
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(tempFile));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		int countLine = 1;
		while(scanOutput.hasNextLine()) {
			countLine++;
			pw.println(lineOutput);
			lineInput = fragmentScanner.nextLine();			
			lineOutput = scanOutput.nextLine();			
		}
		progress.end();
		scanOutput.close();
		pw.close();
		
		if (!outputFile.delete()) {
	        Parameters.reportError("Could not delete file");
	        return;
	    } 
		if (!tempFile.renameTo(outputFile)) {
	        Parameters.reportError("Could not rename file");
			return;
		}
		
		try {
			fragmentWriter = new FileWriter(outputFile, true);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		String[] lineSplit = lineInput.split("\t");
		TSNodeLabel fragment = null;
		try {
			fragment = new TSNodeLabel(lineSplit[0], false);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		long count = partialFragments ? 
					TSNodeLabel.countPartialFragmentInTreebank(treebank, fragment) :
					TSNodeLabel.countFragmentInTreebank(treebank, fragment);	
		try {
			fragmentWriter.write(fragment.toString(false, true) + "\t" + count + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}		
		
		Parameters.reportLineFlush("Retrieving Correct Frequencies of remaining fragments");
		progress = new PrintProgress("Extracting from tree:", printProgressEvery, 0);
		try {
			if (threads==1) processInputOutputSingleThread();
			else processInputOutputMultipleThreads();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		progress.end();
	}
	
	public void retriveCorrectFreq() {
		fragmentWriter = FileUtil.getPrintWriter(outputFile);
		
		Parameters.reportLineFlush("Retrieving Correct Frequencies");
		//System.out.println("Total Fragments: " + FileUtil.getNumberOfLines(fragmentsFile));		
		progress = new PrintProgress("Extracting from tree:", printProgressEvery, 0);
		try {
			if (threads==1) processInputOutputSingleThread();
			else processInputOutputMultipleThreads();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		progress.end();
	}
		
	private void processInputOutputSingleThread() throws Exception {
		if (partialFragments) processInputOutputSingleThreadPartialFragments();
		else processInputOutputSingleThreadFragments();
	}
	
	private void processInputOutputSingleThreadFragments() throws Exception {
		while(fragmentScanner.hasNextLine()) {
			progress.next();
			String line = fragmentScanner.nextLine();
			if (line.equals("")) continue;			
			String[] lineSplit = line.split("\t");
			TSNodeLabel fragment = new TSNodeLabel(lineSplit[0], false);
			long count = TSNodeLabel.countFragmentInTreebank(treebank, fragment);	
			fragmentWriter.write(fragment.toString(false, true) + "\t" + count + "\n");
		}
	}
	
	private void processInputOutputSingleThreadPartialFragments() throws Exception {
		while(fragmentScanner.hasNextLine()) {
			progress.next();
			String line = fragmentScanner.nextLine();
			if (line.equals("")) continue;			
			String[] lineSplit = line.split("\t");
			TSNodeLabel fragment = new TSNodeLabel(lineSplit[0], false);
			long count = TSNodeLabel.countPartialFragmentInTreebank(treebank, fragment);							
			fragmentWriter.write(fragment.toString(false, true) + "\t" + count + "\n");
		}
	}
	
	private synchronized long getNextFragmentsLoad(TSNodeLabel[] fragmentsForThread) {
		Arrays.fill(fragmentsForThread, null);		
		boolean filled = fragmentScanner.hasNextLine();
		int i = 0;		
		while (fragmentScanner.hasNextLine()) {
			if (i==numberOfFragmentsPerThreads) break;						
			String line = fragmentScanner.nextLine();
			String[] lineSplit = line.split("\t");
			try {			
				fragmentsForThread[i] = new TSNodeLabel(lineSplit[0], false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			i++;
		}
		if (filled) {
			progress.next(i);
			long index = fragmentReadCounter;
			fragmentReadCounter += i;
			return index;
		}
		return -1;
				
	}
	
	private synchronized void addFragmentsToQueue(TSNodeLabel[] currentFragments, 
			long firstFragmentIndex, long[] fragmentsCounts) throws IOException {
				
		if (firstFragmentIndex==fragmentWrittenCounter) {
			int i=0;
			for(; i<numberOfFragmentsPerThreads; i++) {
				TSNodeLabel currentFragment = currentFragments[i];
				if (currentFragment==null) break;
				String newFragmentCount = currentFragment.toString(false, true) + "\t" + fragmentsCounts[i] + "\n";
				fragmentWriter.write(newFragmentCount);
			}
			fragmentWrittenCounter += i;
			while(!fragmentsWaitingToBeWritten.isEmpty()) {
				StringLong nextElement = fragmentsWaitingToBeWritten.peek();
				long position = nextElement.getLongInteger();
				if (position == fragmentWrittenCounter) {				
					fragmentWriter.write(nextElement.getString());
					fragmentsWaitingToBeWritten.poll();
					fragmentWrittenCounter++;
				}
				else break;
		    }
		}
		
		else {
			for(int i=0; i<numberOfFragmentsPerThreads; i++) {
				TSNodeLabel currentFragment = currentFragments[i];
				if (currentFragment==null) break;
				String newFragmentCount = currentFragment.toString(false, true) + "\t" + fragmentsCounts[i] + "\n";
				StringLong stringToQueue = new StringLong(newFragmentCount, firstFragmentIndex + i);
				fragmentsWaitingToBeWritten.add(stringToQueue);
			}
		}
		
	}
	
	private void processInputOutputMultipleThreads() throws Exception {
		fragmentsWaitingToBeWritten = new PriorityBlockingQueue<StringLong>();
		CountFragmentsThread[] threadsArray = new CountFragmentsThread[threads];
		
		int lastThreadIndex = threads-1;
		for(int t=0; t<threads; t++) {
			CountFragmentsThread newCounterThread = partialFragments ? 
					new CountPartialFragmentsThread() : new CountFragmentsThread();
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
				
		fragmentScanner.close();
		fragmentWriter.close();		
	}
	
	private class CountFragmentsThread extends Thread {
		
		TSNodeLabel[] currentFragments;
		long loadFirstIndex;
		long[] fragmentsCounts;
				
		public CountFragmentsThread() {
			currentFragments = new TSNodeLabel[numberOfFragmentsPerThreads];
			fragmentsCounts = new long[numberOfFragmentsPerThreads];
		}
		
		public void run(){
			while( (loadFirstIndex=getNextFragmentsLoad(currentFragments)) != -1 ) {

				for(int i=0; i<numberOfFragmentsPerThreads; i++) {
					TSNodeLabel currentFragment = currentFragments[i];
					if (currentFragment==null) break;
					fragmentsCounts[i] = getCount(currentFragment);			
				}
				try {
					addFragmentsToQueue(currentFragments, loadFirstIndex, fragmentsCounts);
				} catch (IOException e) {
					Parameters.reportError(e.getMessage());
					e.printStackTrace();
					return;
				}
			}
		}
		
		protected long getCount(TSNodeLabel currentFragment) {
			return TSNodeLabel.countFragmentInTreebank(treebank, currentFragment);
		}
		
	}
	
	private class CountPartialFragmentsThread extends CountFragmentsThread {
			
		protected long getCount(TSNodeLabel currentFragment) {
			return TSNodeLabel.countPartialFragmentInTreebank(treebank, currentFragment);
		}
	}	
		
	
	public static void main2(String[] args) throws Exception {
		//String fragmentString = "(ADV \"beaucoup\")";
		//String fragmentString = "(AP (ADV \"beaucoup\") (ADV \"plus\") ADJ)";
		//TSNodeLabel fragment = new TSNodeLabel(fragmentString, false);
		File treebankFile = new File(Parameters.corpusPath + "FrenchTreebank/ftbuc+lexeme-only/ftb_1_cleaned.mrg");
		ArrayList<TSNodeLabel> treebank = Wsj.getTreebank(treebankFile);		
		//long count = TSNodeLabel.countPartialFragmentInTreebank(treebank, fragment);
		//System.out.println(count);
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.label==null) {
					System.out.println(t);
				}
			}
		}
	}
	
	public static void main1(String[] args) throws Exception {
		main1(new String[]{
				"-threads:2",
				"/Users/fedja/Work/Code/TreeGrammars/tmp/wsj-02-21_uk1.mrg",
				"/Users/fedja/Work/Code/TreeGrammars/tmp/fragments_exactFreq_uk1_shuffled_first233.txt",				
				"/Users/fedja/Work/Code/TreeGrammars/tmp/fragments_exactFreq_uk1_shuffled_first233_new.txt"
		});
	}
	
	
	
	public static void main(String[] args) throws Exception {
		
		long time = System.currentTimeMillis();
		
		String usage = "USAGE: java RetrieveCorrectFreqQueue [-partialFragments:false] [-threads:1] " +
				"treebank inputFile outputFile";
		
		String partialFragmentsOption = "-partialFragments:";
		String threadsOption = "-threads:";
		int threads = 1;
		boolean partialFragments = false;
		int length = args.length; 
		
		if (length<3 || length>5) {
			System.err.println("Incorrect number of arguments");
			System.err.println(usage);
			return;
		}
		
		File treebankFile=null, inputFile=null, outputFile=null;
		for(String option : args) {
			if (option.equals(partialFragmentsOption)) 
				partialFragments = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(threadsOption))
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

		ArrayList<TSNodeLabel> treebank = Wsj.getTreebank(treebankFile);		
		RetrieveCorrectFreqQueue RCF = new RetrieveCorrectFreqQueue(treebank, inputFile, outputFile, partialFragments, threads);
		RCF.run();
		
		System.out.println("Took: " + (System.currentTimeMillis() - time)/1000 + "seconds.");
	}
	
	
	
}
