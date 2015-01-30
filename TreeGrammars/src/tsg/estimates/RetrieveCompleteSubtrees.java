package tsg.estimates;

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
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelIndex;
import tsg.TSNodeLabelStructure;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class RetrieveCompleteSubtrees extends Thread {
	
	
	public static void addSubTreeRecursive(TSNodeLabel fragment, int count,
			Hashtable<String, int[]> finalFragments) {
		
		String fragString = fragment.toString(false, true);
		Utility.increaseInTableInt(finalFragments, fragString, count);
		for(TSNodeLabel d : fragment.daughters) {
			if (d.isTerminal()) continue;
			addSubTreeRecursive(d, count, finalFragments);
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		
		long time = System.currentTimeMillis(); 

		String usage = "USAGE: java RetrieveCompleteSubtrees fragmentsFile outputFile";
		
		int length = args.length; 
		
		if (length!=2) {
			System.err.println("Incorrect number of arguments");
			System.err.println(usage);
			return;
		}
		
		File inputFile=null, outputFile=null;
		for(String option : args) {
			if (inputFile==null) inputFile = new File(option);
			else outputFile = new File(option);
		}				
		
		Hashtable<String, int[]> finalFragments = new Hashtable<String, int[]>();
		PrintProgress pp = new PrintProgress("Reading Fragments: ", 10000, 0);
		
		Scanner scan = FileUtil.getScanner(inputFile);				
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) continue;
			String[] lineSplit = line.split("\t");
			TSNodeLabel fragment = new TSNodeLabel(lineSplit[0], false);
			int count = Integer.parseInt(lineSplit[1]);						
			addSubTreeRecursive(fragment, count, finalFragments);
			pp.next();
		}
		pp.end();
		
		System.out.println("New fragments: " + finalFragments.size());
		pp = new PrintProgress("Writing Fragments: ", 1000);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(Entry<String,int[]> e : finalFragments.entrySet()) {
			pw.println(e.getKey() + "\t" + e.getValue()[0]);
			pp.next();
		}
		pp.end();
		pw.close();
		
		
		System.out.println("Took: " + (System.currentTimeMillis() - time)/1000 + "seconds.");
	}
	
	
}
