package tsg.kernels;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import util.PrintProgress;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;

public class SortAndMergeFragmentFiles {
	
	PrintProgress progress;
	
	public SortAndMergeFragmentFiles(File fileDir, File outputFile, 
			String unsortedFilePrefix, String sortedFilePrefix, String tmpFilePrefix) {
		
		String outputPath = fileDir.getAbsolutePath() + "/";
		File[] fileList = fileDir.listFiles();
		Arrays.sort(fileList);
		Vector<File> ouputSortedFile = new Vector<File>();
		File mergingDoneFile = new File(outputPath + ".mergingDone");
		if (mergingDoneFile.exists()) {
			System.out.println("Merged file already exists!");
			return;
		}
		progress = new PrintProgress("Sorting File ");				
		for(File inputFile : fileList) {
			progress.next();
			String fileName = inputFile.getName();						
			if (fileName.startsWith(sortedFilePrefix)) {
				ouputSortedFile.add(inputFile);
				continue;				
			}
			if (fileName.startsWith(unsortedFilePrefix)) {				
				File sortedFile = new File(outputPath + sortedFilePrefix + fileName);
				File sortedFileTmp = new File(outputPath + tmpFilePrefix + sortedFilePrefix + fileName);				
				sortFile(inputFile, sortedFileTmp, sortedFile);
				ouputSortedFile.add(sortedFile);
			}								
		}		
		progress.end();
		
		File[] ouputSortedFileArray = ouputSortedFile.toArray(new File[]{});
		System.out.println("Merging Files.");
		mergeSortedFiles(ouputSortedFileArray, outputFile);
				
		FileUtil.appendReturn("merging done!", mergingDoneFile);
	}

	private static void sortFile(File inputFile, File outputFileTmp, File outputFile) {
		FileUtil.countNonEmptyLines(inputFile);
		int linesNumber = FileUtil.countNonEmptyLines(inputFile);
		Scanner scan = FileUtil.getScanner(inputFile);
		String[] lines = new String[linesNumber];
		int i=0;
		while(scan.hasNextLine()) {
			lines[i] = scan.nextLine();
			i++;
		}
		Arrays.sort(lines);				
		PrintWriter pw = FileUtil.getPrintWriter(outputFileTmp);
		for(String l : lines) {
			pw.println(l);
		}
		pw.close();
		outputFileTmp.renameTo(outputFile);
	}
	
	private static void mergeSortedFiles(File[] sortedFiles, File outputFile) {		
		int filesNumber = sortedFiles.length;
		Scanner[] filesScanners = new Scanner[filesNumber];
		PrintWriter ouputWriter = FileUtil.getPrintWriter(outputFile);
		for(int i=0; i<filesNumber; i++) {
			filesScanners[i] = FileUtil.getScanner(sortedFiles[i]);
		}
		TreeSet<FragmentFreqFileIndex> queue = new TreeSet<FragmentFreqFileIndex>();
		for(int i=0; i<filesNumber; i++) {
			FragmentFreqFileIndex fileLine = new FragmentFreqFileIndex(filesScanners[i].nextLine(), i);
			queue.add(fileLine);
		}
		FragmentFreqFileIndex pendingFragment = queue.pollFirst();
		int fileIndex = pendingFragment.fileIndex;
		FragmentFreqFileIndex fileLine = new FragmentFreqFileIndex(filesScanners[fileIndex].nextLine(), fileIndex);
		queue.add(fileLine);
		do {
			FragmentFreqFileIndex topOfTheQueue = queue.pollFirst();
			fileIndex = topOfTheQueue.fileIndex;
			if (filesScanners[fileIndex].hasNextLine()) {
				fileLine = new FragmentFreqFileIndex(filesScanners[fileIndex].nextLine(), fileIndex);
				queue.add(fileLine);
			}
			if (topOfTheQueue.sameFragment(pendingFragment)) {
				Utility.arrayIntPlus(topOfTheQueue.freq, pendingFragment.freq);
				//pendingFragment.freq += topOfTheQueue.freq;
			}
			else {
				ouputWriter.println(pendingFragment.fragment + "\t" + pendingFragment.freqToString());
				pendingFragment = topOfTheQueue;
			}
		} while(!queue.isEmpty());
		ouputWriter.println(pendingFragment.fragment + "\t" + pendingFragment.freqToString());
		ouputWriter.close();
	}
	
	private static class FragmentFreqFileIndex implements Comparable<FragmentFreqFileIndex> {

		String fragment;
		int[] freq;
		int fileIndex;		
		
		public FragmentFreqFileIndex(String line, int fileIndex) {
			String[] treeFreq = line.split("\t");
			fragment = treeFreq[0];
			int length = treeFreq.length-1;
			freq = new int[length];
			for(int i=0; i<length; i++) {
				freq[i] = Integer.parseInt(treeFreq[i+1]); 
			}
			this.fileIndex = fileIndex;			
		}
		
		public String freqToString() {
			return Utility.arrayToString(freq,'\t');
		}
		
		public int compareTo(FragmentFreqFileIndex o) {
			int cmp = this.fragment.compareTo(o.fragment);
			if (cmp!=0) return cmp;
			return new Integer(fileIndex).compareTo(o.fileIndex);
			//return Utility.compare(this.freq, o.freq);
		}
		
		public boolean equals(Object o) {
			if (o instanceof FragmentFreqFileIndex) {
				FragmentFreqFileIndex otherLineFileIndex = (FragmentFreqFileIndex)o;
				return this.fragment.equals(otherLineFileIndex.fragment) &&
					this.fileIndex==otherLineFileIndex.fileIndex &&
					Arrays.equals(this.freq,otherLineFileIndex.freq); 
			}
			return false;
		}
		
		public boolean sameFragment(FragmentFreqFileIndex o) {
			return this.fragment.equals(o.fragment);
		}
		
		public String toString() {
			return fileIndex+ ": " + fragment + "\t" + freqToString();
		}
		
	}
	
	public static void main(String[] args) {
		//File file1 = new File(args[0]);
		//File file2 = new File(args[1]);
		//File outputFile = new File(args[2]);
		//mergeSortedFiles(new File[]{file1,file2}, outputFile);		
		File fileDir = new File("/disk/scratch/fsangati/PLTSG/Chelba_Right_H2_V1/FragmentSeeker_Sat_Jun_09_00_33_54_wulpx/tmp");
		File outputFile = new File("/disk/scratch/fsangati/PLTSG/Chelba_Right_H2_V1/FragmentSeeker_Sat_Jun_09_00_33_54_wulpx/fragments_approxFreq.txt");
		String unsortedFilePrefix = "fragments_";
		String tmpFilePrefix = "tmp_";
		String sortedFilePrefix = "sorted_";
		new SortAndMergeFragmentFiles(fileDir, outputFile, 
				unsortedFilePrefix, sortedFilePrefix, tmpFilePrefix);
	}

	
}
