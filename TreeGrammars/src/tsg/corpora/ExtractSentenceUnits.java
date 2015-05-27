package tsg.corpora;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.IdentityArrayList;
import util.IdentitySet;
import util.PrintProgress;
import util.Utility;

public class ExtractSentenceUnits {

	//"K_November_3_1960_60380.txt.xml	3	84"
	
	static String version = "0.21";
	
	static HashMap<String, TreeMap<Integer, TreeSet<Integer>>> indexTable =
			new HashMap<String, TreeMap<Integer, TreeSet<Integer>>>(); //File, sntNumb, wordIndexes
	
	static boolean printTree = false;
	static boolean debug = false;
	static boolean removeOverlappings = false;
	
	static PrintProgress pp;
	
	private static void extractTreeUnits(File indexFile, String parsingFileDir, File outputFile) throws Exception {
		if (!parsingFileDir.endsWith("/"))
			parsingFileDir += '/';
		readIndexes(indexFile);
		Iterator<Entry<String, TreeMap<Integer, TreeSet<Integer>>>> iter = indexTable.entrySet().iterator();
		PrintWriter pw = new PrintWriter(outputFile);
		pp = new PrintProgress("Processing tree", 1, 0);
		while(iter.hasNext()) {
			pp.next();
			Entry<String, TreeMap<Integer, TreeSet<Integer>>> n = iter.next();
			String fileName = n.getKey();
			File file = new File(parsingFileDir + fileName);
			TreeMap<Integer, TreeSet<Integer>> subTable = n.getValue();
			Scanner scan = new Scanner(file);
			printUnits(fileName, subTable, scan, pw);
		}
		pw.close();
		pp.end();
	}


	static void readIndexes(File inputFile) throws FileNotFoundException {
		System.out.println("Reading indexes from: " + inputFile);
		Scanner scan = new Scanner(inputFile);		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\\s++");
			String file = lineSplit[0];
			Integer snt = Integer.parseInt(lineSplit[1]);
			Integer index = Integer.parseInt(lineSplit[2]);
			Utility.putInTreeMapTreeSet(indexTable, file, snt, index);
		}
		scan.close();
		System.out.println("Total files: " + indexTable.size());
	}


	private static void printUnits(String fileName, TreeMap<Integer, TreeSet<Integer>> subTable,
			Scanner scan, PrintWriter pw) throws Exception {
		
		int sentenceNumb = 0;
		Iterator<Entry<Integer, TreeSet<Integer>>> iter = subTable.entrySet().iterator();
		Entry<Integer, TreeSet<Integer>> next = iter.next();
		int nextSnt = next.getKey();
		while(scan.hasNextLine()) {			
			String line = scan.nextLine().trim();
			if (line.startsWith("<parse>")) { //<parse>(Tree)</parse>
				line = line.substring(7, line.length()-8).trim();
				sentenceNumb++;
			}
			if (sentenceNumb==nextSnt) {
				TreeSet<Integer> indexes = next.getValue();
				TSNodeLabel tree = new TSNodeLabel(line); 
				printUnits(fileName, sentenceNumb, tree, indexes, pw);
				if (!iter.hasNext())
					return;
				next = iter.next();
				nextSnt = next.getKey();
			}
		}
		
	}


	private static void printUnits(String fileName, int sentenceNumb, TSNodeLabel tree, TreeSet<Integer> indexes, PrintWriter pw) {
		
		ArrayList<TreeSet<Integer>> contIndexes = getContigousZeroBasedIndexes(indexes);
		ArrayList<TSNodeLabel> units = new ArrayList<TSNodeLabel>();
		
		for(TreeSet<Integer> ci : contIndexes) {
			TSNodeLabel u = TSNodeLabel.getSentenceUnit(tree, ci);
			units.add(u);
		}
		
		if (removeOverlappings) {
			removeOverlappings(contIndexes, units);
		}
		
		Iterator<TreeSet<Integer>> iter = contIndexes.iterator();
		for(TSNodeLabel u : units) {
			if (debug) {
				TreeSet<Integer> ci = iter.next();
				ArrayList<TSNodeLabel> lex = tree.collectLexicalItems(); 
				int firstIndexSpan = getOneBasedIndex(u.getLeftmostLexicalNode(), lex);
				int lastIndexSpan = getOneBasedIndex(u.getRightmostLexicalNode(), lex);
				int[] indexSpan = new int[]{firstIndexSpan, lastIndexSpan};
				pw.print(fileName + "|" + sentenceNumb + "|" + oneBased(ci) + "|" + 
						Arrays.toString(indexSpan) + "\t");
			}
			pw.println(printTree ? u.toString() : u.toFlatSentence());
		}
		
	}


	private static void removeOverlappings(
			ArrayList<TreeSet<Integer>> contIndexes,
			ArrayList<TSNodeLabel> units) {
		
		int size = units.size();
		if (size==1)
			return;
		boolean[] remove = new boolean[size];
		ListIterator<TSNodeLabel> iter1 = units.listIterator();
		ListIterator<TSNodeLabel> iter2 = null;
		TSNodeLabel next1=null, next2=null;
		outer:
		while(iter1.hasNext()) {
			int i1 = iter1.nextIndex();
			next1 = iter1.next();
			if (remove[i1])
				continue;
			iter2 = units.listIterator(i1+1);
			while(iter2.hasNext()) {
				int i2 = iter2.nextIndex();
				next2 = iter2.next();
				if (remove[i2])
					continue;
				int overlap = overlap(next1, next2);
				if (overlap==1) {
					//remove next1
					remove[i1] = true;
					continue outer;
				}
				if (overlap==2) {
					//remove next2
					remove[i2] = true;
					continue;
				}
			}
		}
		ListIterator<TreeSet<Integer>> iterIndex = contIndexes.listIterator();
		ListIterator<TSNodeLabel> iterUnits = units.listIterator();
		for(boolean b : remove) {
			iterIndex.next();
			iterUnits.next();
			if (b) {
				iterIndex.remove();
				iterUnits.remove();
			}
		}
	}


	/*
	 * If s1 contains s2 returns 1, if s2 contains s1 returns s2, otherwise returns -1
	 */
	private static int overlap(TSNodeLabel next1, TSNodeLabel next2) {
		IdentitySet<TSNodeLabel> allNodes1 = new IdentitySet<TSNodeLabel>(next1.collectAllNodes());
		IdentitySet<TSNodeLabel> allNodes2 = new IdentitySet<TSNodeLabel>(next2.collectAllNodes());
		if (allNodes1.contains(next2))
			return 1;
		if (allNodes2.contains(next1))
			return 2;
		return -1;
	}


	private static int getOneBasedIndex(TSNodeLabel node, ArrayList<TSNodeLabel> lex) {
		int i =0;
		for(TSNodeLabel l : lex) {
			i++;
			if (l==node)
				return i;
		}
		return -1;
	}


	private static TreeSet<Integer> oneBased(TreeSet<Integer> ci) {
		TreeSet<Integer> result = new TreeSet<Integer>();
		for(int i : ci) {
			result.add(i+1);
		}
		return result;
	}


	private static ArrayList<TreeSet<Integer>> getContigousZeroBasedIndexes(TreeSet<Integer> indexes) {
		ArrayList<TreeSet<Integer>> result = new ArrayList<TreeSet<Integer>>();
		TreeSet<Integer> current = null;
		int previous = -2;
		for(int i : indexes) {
			if (i!=previous+1) {
				//non contiguous
				current = new TreeSet<Integer>();
				result.add(current);
			}
			current.add(i-1);
			previous = i;
		}
		return result;		
	}


	public static void main(String[] args) throws Exception {
		
		String usage = "ExtractTreeUnits v." + version + "\n" + 
				"usage: java -jar ExtractTreeUnits.jar "
				+ "-indexFile:path -parsedFile:path -outputFile:path "
				+ "-removeOverlapping:false -printTree:false -debug:false";
		
		
		//atomic_energy
		/*
		args = new String[]{
				"/Users/fedja/Downloads/kennedy_stanford/atomic_energy.txt",
				"/Users/fedja/Downloads/kennedy_stanford/",
				"/Users/fedja/Downloads/kennedy_stanford/atomic_energy.units_noOverlaps.txt", //_noOverlaps
				"true", //removeOverlappings
				"false", //printTree
				"true" //debug
		};
		*/
		
		//benson.txt
		/*
		args = new String[]{
				"/Users/fedja/Downloads/kennedy_stanford/benson.txt",
				"/Users/fedja/Downloads/kennedy_stanford/",
				"/Users/fedja/Downloads/kennedy_stanford/benson.txt.units.txt", //_noOverlaps
				"false", //removeOverlappings
				"false", //printTree
				"true" //debug
		};
		*/
						
		
		if (args.length!=6) {
			System.err.println("I need 6 arguments:");
			System.err.println(usage);
			return;
		}
						
		File indexFile = ArgumentReader.readFileOption(args[0]);
		String parsingFileDir = ArgumentReader.readStringOption(args[1]);
		File outputFile = ArgumentReader.readFileOption(args[2]);
		removeOverlappings = ArgumentReader.readBooleanOption(args[3]);
		printTree = ArgumentReader.readBooleanOption(args[4]);
		debug = ArgumentReader.readBooleanOption(args[5]);		
		
		extractTreeUnits(indexFile, parsingFileDir, outputFile);
		
	}


}
