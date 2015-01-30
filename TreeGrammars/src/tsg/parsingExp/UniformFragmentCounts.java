package tsg.parsingExp;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class UniformFragmentCounts {

	File fragmentsFile, outputFile;	
	Hashtable<TSNodeLabel, double[]> fragmentsCounts;
	Hashtable<String, double[]> rootsCounts;
		
	public UniformFragmentCounts(File fragmentsFile, File outputFile) throws Exception {
		
		this.fragmentsFile = fragmentsFile;
		this.outputFile = outputFile;		
		rootsCounts = new Hashtable<String, double[]>();
		run();
	}
	
	private void run() throws Exception {
		getRootCounts();		
		printFragments();
	}
	

	private void getRootCounts() throws Exception {
		
				
	}

	public void printFragments() throws Exception {
		
		Parameters.reportLine("Printing fragments to file: " + outputFile);
		PrintProgress progress = new PrintProgress("Progress:", 10000, 0);		
		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Scanner fragmentsScanner = FileUtil.getScanner(fragmentsFile);		
		while(fragmentsScanner.hasNextLine()) {
			progress.next();
			String line = fragmentsScanner.nextLine();
			if (line.equals("")) continue;			
			String[] lineSplit = line.split("\t");
			TSNodeLabel fragment = new TSNodeLabel(lineSplit[0], false);
			double count = Double.parseDouble(lineSplit[1]);
			String root = fragment.label();
			double rootCount = rootsCounts.get(root)[0];
			double relativeFreq = count/rootCount;
			pw.println(fragment.toString(false,true) + "\t" + relativeFreq);			
		}
		
		pw.close();
		progress.end();
		
	}
		
	public static void main(String[] args) throws Exception {
		
		File fragmentsFile = new File(args[0]);
		File outputFile = new File(args[1]);
		int count = Integer.parseInt(args[2]);
						
		Scanner fragmentsScanner = FileUtil.getScanner(fragmentsFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(fragmentsScanner.hasNextLine()) {			
			String line = fragmentsScanner.nextLine();
			if (line.equals("")) continue;			
			String[] lineSplit = line.split("\t");			
			pw.println(lineSplit[0] + "\t" + count);			
		}
		pw.close();
				
	}
	
	
}
