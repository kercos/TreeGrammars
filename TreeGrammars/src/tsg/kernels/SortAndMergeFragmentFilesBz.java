package tsg.kernels;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

import settings.Parameters;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class SortAndMergeFragmentFilesBz {
	
	PrintProgress progress;
	public static int numberOfFilesPerMergePass = 1000;
	
	public SortAndMergeFragmentFilesBz(File fileDir, File outputFile, 
			String unsortedFilePrefix, String sortedFilePrefix, String tmpFilePrefix) {
		
		String outputPath = fileDir.getAbsolutePath() + "/";
		File[] fileList = fileDir.listFiles();
		Arrays.sort(fileList);
		Vector<File> ouputSortedFile = new Vector<File>();		
		File mergingDoneFile = new File(outputPath + "/.mergingDone");
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
		Arrays.sort(ouputSortedFileArray);
		Parameters.reportLineFlush("Merging Files.");
		mergeSortedFiles(ouputSortedFileArray, outputFile, outputPath);
						
		FileUtil.appendReturn("merging done!", mergingDoneFile);
	}

	private static void sortFile(File inputFile, File outputFileTmp, File outputFile) {
		
		ArrayList<String> linesToSort = new ArrayList<String>();		 
		
		try {			 
			BufferedReader gzipReader = new BufferedReader(new InputStreamReader(
					new CBZip2InputStream(new FileInputStream(inputFile)),"UTF-8"));
			String line = null;			
			while((line = gzipReader.readLine()) != null) {
				linesToSort.add(line);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}		
		
		Collections.sort(linesToSort);
					
		try {
			FileOutputStream fileStream = new FileOutputStream(outputFileTmp);
			BufferedWriter gzipWriter = new BufferedWriter(
					new OutputStreamWriter(new CBZip2OutputStream(fileStream), "UTF-8"));
			
			for(String line : linesToSort) {
				gzipWriter.write(line + "\n");
			}
			
			gzipWriter.close();
			fileStream.close();
		} catch (Exception e) {
			System.err.println("Problems while writing bzip2 file");
			e.printStackTrace();
			return;
		}						
		outputFileTmp.renameTo(outputFile);	
		inputFile.delete();		
	}
	
	private static BufferedReader[] getFileReaders(File[] sortedFiles) {
		
		int filesNumber = sortedFiles.length;
		
		BufferedReader[] result = new BufferedReader[filesNumber];		
		
		try {
			for(int i=0; i<filesNumber; i++) {
				File f = sortedFiles[i];
				result[i] = new BufferedReader(new InputStreamReader(
						new CBZip2InputStream(new FileInputStream(f)),"UTF-8"));				
			}
		} catch(IOException e) {
			e.printStackTrace();
		}	
		
		return result;
	}
	
	private static String readNextLine(BufferedReader reader) {
		String result = null;
		try {
			result = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private static void mergeSortedFiles(File[] sortedFiles, File outputFile, String outputPath) {
		
		int numberOfFiles = sortedFiles.length; 
		if (numberOfFiles <= numberOfFilesPerMergePass) {
			mergeSortedFilesSinglePass(sortedFiles, outputFile);
		}
		else {
			mergeSortedFilesMultiPass(sortedFiles, outputFile, outputPath);
		}
	}
	
	private static void mergeSortedFilesSinglePass(File[] sortedFiles, File outputFile) {
		
		PrintWriter ouputWriter = FileUtil.getPrintWriter(outputFile);
		int filesNumber = sortedFiles.length;		
		BufferedReader[] filesReaders = getFileReaders(sortedFiles);		
		
		TreeSet<FragmentFreqFileIndex> queue = new TreeSet<FragmentFreqFileIndex>();
		for(int i=0; i<filesNumber; i++) {
			FragmentFreqFileIndex fileLine = new FragmentFreqFileIndex(readNextLine(filesReaders[i]), i);
			queue.add(fileLine);
		}
		FragmentFreqFileIndex pendingFragment = queue.pollFirst();
		int fileIndex = pendingFragment.fileIndex;
		FragmentFreqFileIndex fileLine = new FragmentFreqFileIndex(readNextLine(filesReaders[fileIndex]), fileIndex);
		queue.add(fileLine);
		do {
			FragmentFreqFileIndex topOfTheQueue = queue.pollFirst();
			fileIndex = topOfTheQueue.fileIndex;			
			String nextLine = readNextLine(filesReaders[fileIndex]);
			if (nextLine!=null) {
				fileLine = new FragmentFreqFileIndex(nextLine, fileIndex);
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
	
	private static void mergeSortedFilesSinglePassCompressed(
			File[] sortedFiles, File outputFile) throws UnsupportedEncodingException, IOException {
		
		FileOutputStream fileStream = new FileOutputStream(outputFile);
		BufferedWriter gzipWriter = new BufferedWriter(
				new OutputStreamWriter(new CBZip2OutputStream(fileStream), "UTF-8"));
		
		int filesNumber = sortedFiles.length;		
		BufferedReader[] filesReaders = getFileReaders(sortedFiles);		
		
		TreeSet<FragmentFreqFileIndex> queue = new TreeSet<FragmentFreqFileIndex>();
		for(int i=0; i<filesNumber; i++) {
			FragmentFreqFileIndex fileLine = new FragmentFreqFileIndex(readNextLine(filesReaders[i]), i);
			queue.add(fileLine);
		}
		FragmentFreqFileIndex pendingFragment = queue.pollFirst();
		int fileIndex = pendingFragment.fileIndex;
		FragmentFreqFileIndex fileLine = new FragmentFreqFileIndex(readNextLine(filesReaders[fileIndex]), fileIndex);
		queue.add(fileLine);
		do {
			FragmentFreqFileIndex topOfTheQueue = queue.pollFirst();
			fileIndex = topOfTheQueue.fileIndex;			
			String nextLine = readNextLine(filesReaders[fileIndex]);
			if (nextLine!=null) {
				fileLine = new FragmentFreqFileIndex(nextLine, fileIndex);
				queue.add(fileLine);
			}			
			if (topOfTheQueue.sameFragment(pendingFragment)) {
				Utility.arrayIntPlus(topOfTheQueue.freq, pendingFragment.freq);
				//pendingFragment.freq += topOfTheQueue.freq;
			}
			else {
				gzipWriter.write(pendingFragment.fragment + "\t" + pendingFragment.freq + "\n");
				pendingFragment = topOfTheQueue;
			}
		} while(!queue.isEmpty());
			
		gzipWriter.close();
		fileStream.close();
	}
	
	private static void mergeSortedFilesMultiPass(File[] sortedFiles, File outputFile, String outputPath) {
		
		int filesNumber = sortedFiles.length;
		int numberOfPasses =  filesNumber / numberOfFilesPerMergePass;
		int filesInLast = filesNumber % numberOfFilesPerMergePass;
		if (filesInLast>0) numberOfPasses++;
		else filesInLast = numberOfFilesPerMergePass;
		
		File[] outputFilesPasses = new File[numberOfPasses];
		
		for(int pass=1; pass<=numberOfPasses; pass++) {
						
			int filesInCurrentPass = pass==numberOfPasses ? filesInLast : numberOfFilesPerMergePass;
			File currentOutputFilePass = new File(outputPath + "tmp_pass_" + pass + ".bz2");
			outputFilesPasses[pass-1] = currentOutputFilePass;
			
			File[] sortedFilesPass = new File[filesInCurrentPass];
			int j = (pass-1) * numberOfFilesPerMergePass;			
			for(int i=0; i<filesInCurrentPass; i++) {
				sortedFilesPass[i] = sortedFiles[j];
				j++;
			}			
		
			try {
				mergeSortedFilesSinglePassCompressed(sortedFilesPass, currentOutputFilePass);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			Parameters.reportLineFlush("First pass done " + currentOutputFilePass);
			
		}
		
		mergeSortedFilesSinglePass(outputFilesPasses, outputFile);
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
			//return Utility.compare(this.freq, o.freq);
			return new Integer(fileIndex).compareTo(o.fileIndex);
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
		
	}
	
	public static void main1(String[] args) {
		File dir = new File(args[0]);
		File outputFile = new File(args[1]);
		File[] fileList = dir.listFiles();
		Vector<File> bzFileList = new Vector<File>();
		for(File f : fileList) {
			if (f.isDirectory()) continue;
			String name = f.getName();
			if (name.startsWith(".") || !name.endsWith("bz2")) continue;
			bzFileList.add(f);
		}
		fileList = bzFileList.toArray(new File[]{});
		int size = fileList.length;
		System.out.println("Uncompressing " + size + " .bz2 files to " + outputFile);
		mergeSortedFilesSinglePass(fileList, outputFile);		
	}
	
	private static void printAlphabet(BufferedWriter writer, int maxChar, int iter) throws IOException {
		char[] alpha = new char[maxChar];
		char[] beta = new char[maxChar];
		for(int i=0; i<maxChar; i++) {
			alpha[i] = (char)i;
		}
		for(int i=maxChar-1; i>=0; i--) {
			beta[i] = (char)i;
		}
		String a = String.copyValueOf(alpha);
		String b = String.copyValueOf(beta);
		
		for(int i=0; i<iter; i++) {
			writer.write(a+"\n");
			writer.write(b+"\n");
		}
	}
	
	
	
	public static void main(String[] args) throws IOException {
		
		File outputFile = new File("tmp/testCompression.txt");
		File outputFileCompressed = new File("tmp/testCompression_Java.txt.bz");
		File outputFileCompressedUTF8 = new File("tmp/testCompression_Java_utf8.txt.bz");
		
		FileOutputStream fileStreamCompressed = new FileOutputStream(outputFileCompressed);
		FileOutputStream fileStreamCompressedUTF8 = new FileOutputStream(outputFileCompressedUTF8);
		FileOutputStream fileStream = new FileOutputStream(outputFile);
		
		BufferedWriter gzipWriter = new BufferedWriter(
				new OutputStreamWriter(new CBZip2OutputStream(fileStreamCompressed)));
		
		BufferedWriter gzipWriterUTF8 = new BufferedWriter(
				new OutputStreamWriter(new CBZip2OutputStream(fileStreamCompressedUTF8), "UTF-8"));
		
		BufferedWriter normWriter = new BufferedWriter(new FileWriter(outputFile));
		
		char maxChar = 500; 
		int iter = 100;
		
		printAlphabet(gzipWriter, maxChar, iter);
		printAlphabet(gzipWriterUTF8, maxChar, iter);
		printAlphabet(normWriter, maxChar, iter);
			
		gzipWriter.close();
		gzipWriterUTF8.close();
		normWriter.close();
		fileStreamCompressed.close();
		fileStreamCompressedUTF8.close();
		fileStream.close();		
	}


	
}
