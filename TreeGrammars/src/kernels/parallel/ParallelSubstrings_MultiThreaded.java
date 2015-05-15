package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.ListIterator;

import kernels.algo.AllOrderedStringExactSubsequences;
import kernels.algo.CommonSubstring;
import settings.Parameters;
import util.ArgumentReader;


public class ParallelSubstrings_MultiThreaded extends ParallelSubstrings{

	int threads;
	ListIterator<String[]> sourceIter1, targetIter1, sourceIter2, targetIter2;
	String[] source1, source2, target1, target2;
	int index1, index2;
	long reportCounter;
	
	public ParallelSubstrings_MultiThreaded(File sourceFile, File targetFile,
			File outputFile, int minMatchSize, int threads) {
		
		super(sourceFile, targetFile, outputFile, minMatchSize);
		this.threads = threads;
		
		
	}
	
	protected void init() throws FileNotFoundException {
		super.init();
		sourceIter1 = sentencesSource.listIterator();
		targetIter1 = sentencesTarget.listIterator();
		source1 = sourceIter1.next();
		target1 = targetIter1.next();
		index1 = 0;
		index2 = 1;
		sourceIter2 = sentencesSource.listIterator(index2);
		targetIter2 = sentencesTarget.listIterator(index2);
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
			sourceIter2 = sentencesSource.listIterator(index2);
			targetIter2 = sentencesTarget.listIterator(index2);
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
		String[] source1, source2, target1, target2;
		int index1, index2;

		public PairInfo(String[] source1, String[] source2, String[] target1,
				String[] target2, int index1, int index2) {
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
		String sourceFile = dir + "train.tags.en-it.tok.en.head";
		String targetFile = dir + "train.tags.en-it.tok.it.head";
		String logFile = dir + "matchingSubstrings.en-it.tok.log";
		args = new String[]{sourceFile, targetFile, logFile};
		*/
		
		String usage = "ParallelSubstrings_MultiThreaded v. " + getVersion() + "\n" + 
				"usage: java ParallelSubstringsMatch "
				+ "-sourceFile:file, -targetFile:file, -outputFile:file "
				+ "-minMatchSize:n -threads:n -onlyContiguous:true";		
		
		if (args.length!=6) {
			System.err.println("Wrong number of arguments!");
			System.err.println(usage);
		}
		
		File sourceFile = ArgumentReader.readFileOption(args[0]);
		File targetFile = ArgumentReader.readFileOption(args[1]);
		File outputFile = ArgumentReader.readFileOption(args[2]);
		int minMatchSize = ArgumentReader.readIntOption(args[3]);
		int threads = ArgumentReader.readIntOption(args[4]);
		onlyContiguous = ArgumentReader.readBooleanOption(args[5]);
		ParallelSubstrings_MultiThreaded PS = new ParallelSubstrings_MultiThreaded(
				sourceFile, targetFile, outputFile, minMatchSize, threads);
		PS.run();
		
		
		/*
		String s = "Basil Jones : But actually we 're going to start this evolution with a hyena .";
		String t = "< description > \" Puppets always have to try to be alive , \" says Adrian Kohler of the Handspring Puppet Company , a gloriously ambitious troupe of human and wooden actors . Beginning with the tale of a hyena 's subtle paw , puppeteers Kohler and Basil Jones build to the story of their latest astonishment : the wonderfully life-like Joey , the War Horse , who trots convincingly onto the stage . < / description >";
		test2Sentences(s,t);
		*/
	}
}
