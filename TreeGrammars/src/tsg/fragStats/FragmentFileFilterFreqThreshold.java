package tsg.fragStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import org.apache.tools.bzip2.CBZip2InputStream;

import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class FragmentFileFilterFreqThreshold {

	static boolean read2bytes = true;
	static int printProgressEvery = 100000;
	
	
	File fragmentFile;	
	int threshold;
	PrintProgress progress;
	PrintWriter pw;
	
	public FragmentFileFilterFreqThreshold(File fragmentFile,
		File outputFile, int threshold, boolean inputIsCompressed) throws Exception {
		
		this.fragmentFile = fragmentFile;
		this.threshold = threshold;
		this.pw = FileUtil.getPrintWriter(outputFile);
		this.progress = new PrintProgress("Reading fragments", printProgressEvery, 0);
		
		if (inputIsCompressed) filterFragemntFileCompressed();
		else filterFragemntFile();
		
		progress.end();
		pw.close();
	}
	
	private void analyzeNextLine(String line) throws Exception {
		progress.next();		
		String[] lineSplit = line.split("\t");
		int freq = Integer.parseInt(lineSplit[1]);
		if (freq>=threshold) {
			pw.println(line);
		}
	}

	private void filterFragemntFile() throws Exception {
						
		Scanner scan = FileUtil.getScanner(fragmentFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			analyzeNextLine(line);
		}		

		scan.close();		
				
	}
	
	private void filterFragemntFileCompressed() throws Exception {
		
		FileInputStream inputStream = new FileInputStream(fragmentFile);
		if (read2bytes) {
			inputStream.read();
			inputStream.read();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new CBZip2InputStream(inputStream),"UTF-8"));		
		
		while(true) {
			String line = reader.readLine();
			if (line==null) break;
			analyzeNextLine(line);
		}		

		reader.close();		
				
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// keep fragments whose freq. is >= threshold
		File fragmentFile = new File(args[0]);
		File outputFile = new File(args[1]);
		int threshold = ArgumentReader.readIntOption(args[2]);
		boolean inputIsCompressed = ArgumentReader.readBooleanOption(args[3]);
		
		new FragmentFileFilterFreqThreshold(fragmentFile, outputFile, threshold, inputIsCompressed);
	}	

}
