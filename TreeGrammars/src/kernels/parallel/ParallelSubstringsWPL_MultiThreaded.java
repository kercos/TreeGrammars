package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.TreeSet;

import kernels.algo.CommonSubstring;
import settings.Parameters;
import util.ArgumentReader;
import util.IdentityArrayList;
import util.PrintProgressPercentage;

public class ParallelSubstringsWPL_MultiThreaded extends ParallelSubstringsWPL {
	
	int threads;
	ListIterator<String[][]> sourceIter1, targetIter1, sourceIter2, targetIter2;
	String[][] source1, source2, target1, target2;
	int index1, index2;
	long reportCounter;

	public ParallelSubstringsWPL_MultiThreaded(File sourceFile, File sourceFileWPL, 
			File targetFile, File targetFileWPL, File outputFile, int minMatchSize, int threads)  {		
		
		super(sourceFile, sourceFileWPL, targetFile, targetFileWPL, outputFile, minMatchSize);
		
		this.threads = threads;
				
	}
	
	protected void init() throws FileNotFoundException {
		super.init();
		sourceIter1 = sentencesSouceWPL.listIterator();
		targetIter1 = sentencesTargetWPL.listIterator();
		source1 = sourceIter1.next();
		target1 = targetIter1.next();
		index1 = 0;
		index2 = 1;
		sourceIter2 = sentencesSouceWPL.listIterator(index2);
		targetIter2 = sentencesTargetWPL.listIterator(index2);		
	}
	
	protected void printParameters() {
		Parameters.logStdOutPrintln("Number of threads: " + threads);
		super.printParameters();			
	}
	
	protected void computePairwiseMatch()  {
		ThreadRunner[] threadArray = new ThreadRunner[threads];
		for (int i = 0; i < threads; i++) {
			threadArray[i] = new ThreadRunner();
		}
		
		for (int i = 0; i < threads - 1; i++) {
			ThreadRunner t = threadArray[i];
			t.start();
		}
		threadArray[threads - 1].run();

		for (int i = 0; i < threads - 1; i++) {
			try {
				threadArray[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
	}
	
	synchronized private PairInfo getNextPairs() {
		
		if (++reportCounter == reportEvery) {
			reportCounter = 0;
			progress.suspend();
			Parameters.logStdOutPrintln("Total size of table keys, subkey " + Arrays.toString(totalKeysAndPairs(finalTable)));
			if (CommonSubstring.minMatchLength==1)
				Parameters.logStdOutPrintln("Total size of table keys, subkey (of size 1) " + Arrays.toString(totalKeysLengthOneAndPairs()));
			progress.resume();
		}
				
		
		if (sourceIter2.hasNext()) {
			progress.next();
			source2 = sourceIter2.next();
			target2 = targetIter2.next();
			PairInfo pi = new PairInfo(source1, source2, target1, target2,index1, index2);
			index2++;
			return pi;
		}
		
		if (sourceIter1.hasNext()) {
			source1 = sourceIter1.next();
			target1 = targetIter1.next();
			index1++;
			index2 = index1 + 1;
			sourceIter2 = sentencesSouceWPL.listIterator(index2);
			targetIter2 = sentencesTargetWPL.listIterator(index2);
			if (sourceIter2.hasNext()) {
				progress.next();
				source2 = sourceIter2.next();
				target2 = targetIter2.next();
				PairInfo pi = new PairInfo(source1, source2, target1, target2, index1, index2);
				index2++;
				return pi;
			}
		}
		
		return null;
	}

	static protected class PairInfo {
	
		String[][] source1, source2, target1, target2;
		int index1, index2;

		public PairInfo(String[][] source1, String[][] source2, String[][] target1,
				String[][] target2, int index1, int index2) {
			this.source1 = source1;
			this.source2 = source2;
			this.target1 = target1;
			this.target2 = target2;
			this.index1 = index1;
			this.index2 = index2;
		}
	}
	
	protected class ThreadRunner extends Thread {

		public ThreadRunner() {
		}

		public void run() {
			PairInfo pi = null;
			while ((pi = getNextPairs()) != null) {
				computeMatch(pi.index1, pi.index2, pi.source1, pi.source2, pi.target1, pi.target2);
			}
		}

	}

	
	public static void main(String args[]) throws FileNotFoundException {
		
		/*
		String dir = "/Volumes/HardDisk/Scratch/CORPORA/TED_Parallel/en-it/giza_align/";
		args = new String[]{
			"-sourceFile:" + dir + "train.tags.en-it.tok.selection.en",
			"-sourceFileWPL:" + dir + "train.tags.en-it.tok.selection.onewpl.TTposlemmas.en",
			"-targetFile:" + dir + "train.tags.en-it.tok.selection.it",
			"-targetFileWPL:" + dir + "train.tags.en-it.tok.selection.onewpl.TTposlemmas.it",
			"-outputFile:" + dir + "outputTest.gz",
			"-minMatchSize:2",
			"-threads:2"
		};
		*/
		
		String usage = "ParallelSubstringsWPL_MultiThreaded v. " + getVersion() + "\n" + 
				"usage: java ParallelSubstringsMatch "
				+ "-sourceFile:file "
				+ "-sourceFileWPL:file "
				+ "-targetFile:file "
				+ "-targetFileWPL:file "
				+ "-outputFile:file "
				+ "-minMatchSize:n"		
				+ "-threads:n";
				
		if (args.length!=7) {
			System.err.println("Wrong number of arguments!");
			System.err.println(usage);
		}
		
		File sourceFile = ArgumentReader.readFileOption(args[0]);
		File sourceFileWPL = ArgumentReader.readFileOption(args[1]);
		File targetFile = ArgumentReader.readFileOption(args[2]);
		File targetFileWPL = ArgumentReader.readFileOption(args[3]);
		File outputFile = ArgumentReader.readFileOption(args[4]);
		int minMatchSize = ArgumentReader.readIntOption(args[5]);
		int threads = ArgumentReader.readIntOption(args[6]);
	
		ParallelSubstringsWPL_MultiThreaded PS = new ParallelSubstringsWPL_MultiThreaded(
				sourceFile, sourceFileWPL, targetFile, targetFileWPL, outputFile, minMatchSize, threads);
		PS.run();

	}
	
}
