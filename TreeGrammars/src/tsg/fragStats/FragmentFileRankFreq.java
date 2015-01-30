package tsg.fragStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.tools.bzip2.CBZip2InputStream;

import util.ArgumentReader;
import util.PrintProgress;
import util.file.FileUtil;

public class FragmentFileRankFreq {

	static boolean read2bytes = true;
	static int absoluteMax = 100;
	static int printProgressEvery = 100000;
	
	File fragmentFile, outputFile;	
	PrintProgress progress;
	ArrayList<Integer> freqArray = new ArrayList<Integer>();
		
	public FragmentFileRankFreq(File fragmentFile,
		File outputFile, boolean compressed) throws Exception {
		
		this.fragmentFile = fragmentFile;
		this.outputFile = outputFile;
		
		progress = new PrintProgress("Reading fragments", printProgressEvery, 0);
		if (compressed) readFragemntFileCompressed();
		else readFragemntFile();
		progress.end();		
		
		Collections.sort(freqArray);
		Collections.reverse(freqArray);
		
		writeStats();
	}
	
	private void readFragemntFile() throws Exception {
		
		Scanner scan = FileUtil.getScanner(fragmentFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			analyzeNextLine(line);
		}		

		scan.close();		
				
	}
	
	private void readFragemntFileCompressed() throws Exception {
		
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
	
	private void analyzeNextLine(String line) throws Exception {
		progress.next();		
		String[] lineSplit = line.split("\t");
		//TSNodeLabel fragment = new TSNodeLabel(lineSplit[0], false);		
		int freq = Integer.parseInt(lineSplit[1]);
		freqArray.add(freq);
	}	

	
	private void writeStats() {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Iterator<Integer> iter = freqArray.iterator();
		int i=1;
		while(iter.hasNext()) {
			pw.println(i++ + "\t" + iter.next());
		}
		pw.close();
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		File fragmentFile = new File(args[0]);
		File outputFile = new File(args[1]);
		boolean compressed = ArgumentReader.readBooleanOption(args[2]);
		
		new FragmentFileRankFreq(fragmentFile, outputFile, compressed);
	}	

}
