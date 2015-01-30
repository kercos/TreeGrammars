package tsg.mwe;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

import tsg.TSNodeLabel;
import util.PrintProgress;
import util.Utility;

public class TSG_Frags_MWE_EntropyStats {

	File inputFile, outputFile;
	int threshold;
	HashMap<TSNodeLabel,  HashMap<TSNodeLabel, Integer>> unlexFragTypesFreq = 
		new HashMap<TSNodeLabel,  HashMap<TSNodeLabel, Integer>>();

	/*
	Vector<TSNodeLabel> fragments = new Vector<TSNodeLabel>();
	IdentityHashMap<String, BitSet> wordFreqPresence = new IdentityHashMap<String, BitSet>();
	int nbitRequired;
	*/
	
	
	public TSG_Frags_MWE_EntropyStats(File inputFile, File outputFile, int threshold) throws Exception {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.threshold = threshold;
		readFragmentBank();
		printStats();
		
	}
	
	private void readFragmentBank()  throws Exception {
		PrintProgress progress = new PrintProgress("Reading fragments", 100, 0);
		Scanner scan = new Scanner(inputFile);
		int index = 0;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			progress.next();
			String[] lineSplit = line.split("\t");
			TSNodeLabel frag = TSNodeLabel.newTSNodeLabelStd(lineSplit[0]);
			int freq = Integer.parseInt(lineSplit[1]);
			if (freq<threshold)
				continue;		
			addFrag(frag, freq, index);
			index++;
		}
		progress.end();
		//nbitRequired = 31 - Integer.numberOfLeadingZeros(fragments.size());
		System.out.println("Acquired frags: " + index);
		System.out.println("Acquired unlexicalized frags: " + unlexFragTypesFreq.size());
	}

	private void addFrag(TSNodeLabel frag, int freq, int index) {
		TSNodeLabel fragUnlex = frag.clone();
		ArrayList<TSNodeLabel> col = new ArrayList<TSNodeLabel>();
		fragUnlex.pruneAndCollectAllLex(col);
		Utility.putInHashMapIfNotPresent(unlexFragTypesFreq, fragUnlex, frag, freq);

		/*
		fragments.add(frag);
		for(TSNodeLabel lex : col) {
			String word = lex.label();
			word = word.intern();
			BitSet bs = wordFreqPresence.get(word);
			if (bs==null) {
				bs = new BitSet(nbitRequired);	
				wordFreqPresence.put(word, bs);
			}
			bs.set(index);			
		}
		*/		
	}
	
	

	private void printStats() throws Exception {		
		PrintProgress progress = new PrintProgress("Printing fragments with stats", 100, 0);
		System.out.println("Output file contains the following fields:");
		System.out.println("Frag\tFreq\tRel_Freq\tEntropy");
		Scanner scan = new Scanner(inputFile);
		PrintWriter pw = new PrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			progress.next();
			String[] lineSplit = line.split("\t");
			TSNodeLabel frag = TSNodeLabel.newTSNodeLabelStd(lineSplit[0]);
			int freq = Integer.parseInt(lineSplit[1]);
			if (freq<threshold)
				continue;		
			double[] relFreqEntropy = getRelFreqEntropy(frag, freq, unlexFragTypesFreq);
			pw.println(frag.toString(false, true) + "\t" + freq + "\t" + relFreqEntropy[0] + "\t" + relFreqEntropy[1]);
		}
		progress.end();	
		pw.close();
	}

	public static HashMap<TSNodeLabel, HashMap<TSNodeLabel, Integer>> buildUnlexFragTypesFreq(
			HashMap<TSNodeLabel, int[]> table) {
		
		HashMap<TSNodeLabel, HashMap<TSNodeLabel, Integer>> result =
				new HashMap<TSNodeLabel, HashMap<TSNodeLabel,Integer>>();
		for(Entry<TSNodeLabel, int[]> e : table.entrySet()) {
			TSNodeLabel frag = e.getKey();
			Integer freq = e.getValue()[0];
			TSNodeLabel fragUnlex = frag.clone();
			ArrayList<TSNodeLabel> col = new ArrayList<TSNodeLabel>();
			fragUnlex.pruneAndCollectAllLex(col);
			Utility.putInHashMapIfNotPresent(result, fragUnlex, frag, freq);
		}
		return result;
	}
	
	public static double[] getRelFreqEntropy(TSNodeLabel frag, int freq, 
			HashMap<TSNodeLabel, HashMap<TSNodeLabel, Integer>> table) {		
		TSNodeLabel fragUnlex = frag.clone();
		fragUnlex.pruneAllLex();
		HashMap<TSNodeLabel, Integer> subTable = table.get(fragUnlex);
		Collection<Integer> values  = subTable.values();
		double[] totalSumAndEntropy = Utility.totalSumAndEntropy(values);
		double relFreq = ((double) freq)/totalSumAndEntropy[0];
		return new double[]{relFreq, totalSumAndEntropy[1]};
	}

	public static void main(String[] args) throws Exception {
		/*
		String workingPath = "/home/sangati/Work/FBK/TSG_MWE/FTB/";
		File inputFile = new File(workingPath + "ftbStanford/candito.train");
		File outputFile = new File(workingPath + "mweStats/candito.train.mwe.stats");
		new TSG_Frags_MWE_Extractor(inputFile, outputFile);
		*/
		
		String usage = "java TSG_Frags_MWE_Extractor inputFile outputFile threshold";
		if (args.length!=3) {
			System.err.println(usage);
			return;
		}
		new TSG_Frags_MWE_EntropyStats(new File(args[0]), new File(args[1]), Integer.parseInt(args[2]));
		
		/*
		String a = "MWN";
		System.out.println(a.matches(MWE_Regex));
		*/
	}

	
	
}
