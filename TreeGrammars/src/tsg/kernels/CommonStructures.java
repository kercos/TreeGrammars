package tsg.kernels;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ListIterator;

import org.apache.tools.bzip2.CBZip2OutputStream;

import tsg.TSNodeLabelStructure;
import util.PrintProgress;
import util.Utility;

public abstract class CommonStructures extends Thread {
	
	public static int maxCombDaughters = 1000;
	//public static boolean extractIntermediateStructures = true;
	
	public static int maxDepth = 50;
	public static int flushToDiskEvery = -1;
	public static int flushToDiskFileCounter = 0;
	public static int diskFileCounterSize;	
	public static String flushToDiskPath;
	public static String prefixFragmentsFiles = "fragments_";
	public static String prefixTmpFragmentsFiles = "tmp_fragments_";
	public static boolean compressTmpFiles = true;
	
	int threads;
	int treebankSize;
	int currentIndex;
	PrintProgress progress;
	
	ArrayList<TSNodeLabelStructure> treebank;
	CommonStructuresThread[] threadsArray;
	
	public CommonStructures(ArrayList<TSNodeLabelStructure> treebank, 
			int threads, int startIndex) {
		this.treebank = treebank;
		this.threads = threads;		
		treebankSize = treebank.size();
		this.currentIndex = startIndex;
	}
	
	public void run() {
		if (currentIndex>=treebankSize) return;
		progress = new PrintProgress("Extracting fragment from treebank :", flushToDiskEvery, currentIndex);
		extractFromTreebank();
		progress.end();
	}
	
		
	protected void extractFromTreebank() {
		
		initiateThreadArray();
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
	
	protected abstract void initiateThreadArray();
	
	
	private synchronized TSNodeLabelStructure[] getNextTreeLoad(int[] startIndex, int[] fileCounter) {
		if (currentIndex>=treebankSize) return null;
		TSNodeLabelStructure[] result = new TSNodeLabelStructure[flushToDiskEvery];
		startIndex[0] = currentIndex;
		fileCounter[0] = ++flushToDiskFileCounter;
		ListIterator<TSNodeLabelStructure> iter = treebank.listIterator(currentIndex);
		int index = 0;
		while(iter.hasNext()) {
			result[index] = iter.next();
			progress.next();
			currentIndex++;
			if (++index==flushToDiskEvery) break;			
		}		
		return result;
	}
	
	protected abstract class CommonStructuresThread extends Thread {
		
		int[] startIndex, fileCounter;
		
		public CommonStructuresThread() {			
			startIndex = new int[]{0};
			fileCounter = new int[]{0};
		}
		
		public void run(){					
			TSNodeLabelStructure[] trees;
			while((trees = getNextTreeLoad(startIndex, fileCounter)) != null) {
				collectCommonFragments(trees);				
				flushToDisk();				
			}
		}
		
		protected abstract void collectCommonFragments(TSNodeLabelStructure[] trees);
		
		private void flushToDisk() {
			if (compressTmpFiles) flushToDiskCompressed();
			else flushToDiskStandard();
		}
		
		private void flushToDiskCompressed() {
			String nextFileNumber = Utility.padZero(diskFileCounterSize, fileCounter[0]);
			File outputFile = new File(flushToDiskPath + prefixTmpFragmentsFiles + nextFileNumber + ".bz2");			
			try {
				BufferedWriter gzipWriter = new BufferedWriter(new OutputStreamWriter(
						new CBZip2OutputStream(new FileOutputStream(outputFile)), "UTF-8"));
				printFragmentsToFile(gzipWriter);
				gzipWriter.close();
			} catch (Exception e) {
				System.err.println("Problems while writing bzip2 file");
				e.printStackTrace();
				return;
			}						
			outputFile.renameTo(new File(flushToDiskPath + prefixFragmentsFiles + nextFileNumber + ".bz2"));		
			clearFragmentBank();		
		}
		
		private void flushToDiskStandard() {
			String nextFileNumber = Utility.padZero(diskFileCounterSize, fileCounter[0]);
			File outputFile = new File(flushToDiskPath + prefixTmpFragmentsFiles + nextFileNumber + ".txt");			
			try {
				BufferedWriter writer = new BufferedWriter(new PrintWriter(outputFile,"UTF-8"));
				printFragmentsToFile(writer);
				writer.close();
			} catch (Exception e) {
				System.err.println("Problems while writing file");
				e.printStackTrace();
				return;
			}						
			outputFile.renameTo(new File(flushToDiskPath + prefixFragmentsFiles + nextFileNumber + ".txt"));		
			clearFragmentBank();		
		}
		
		protected abstract void printFragmentsToFile(BufferedWriter fileWriter) throws IOException;
		
		protected abstract void clearFragmentBank();
		
	}	
	
}
