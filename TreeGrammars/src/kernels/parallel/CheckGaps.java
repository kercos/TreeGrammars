package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;

import kernels.algo.StringsAlignment;
import settings.Parameters;
import util.IdentityArrayList;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class CheckGaps {

	public static void separeContiguousFromGaps(File sentSourceFile, File sentTargetFile, 
			File tableFile, File outputFile) throws FileNotFoundException {

		File systemLogFile = FileUtil.changeExtension(outputFile, "log");
		Parameters.openLogFile(systemLogFile);
		Parameters.reportLine("Separe Contiguous From Gaps");
		Parameters.reportLine("Sentence file: " + sentSourceFile);
		Parameters.reportLine("Input table file: " + tableFile);
		Parameters.reportLine("Output table file: " + outputFile);
		Parameters.closeLogFile();

		Vector<String[]> sentSource = new Vector<String[]>(ParallelSubstrings.getInternedSentences(sentSourceFile));
		Vector<String[]> sentTarget = new Vector<String[]>(ParallelSubstrings.getInternedSentences(sentTargetFile));

		Parameters.reportLine("Input source sentences:" + sentSource.size());

		Parameters.reportLine("Printing gap table to:" + outputFile);

		Scanner scan = FileUtil.getGzipScanner(tableFile);
		PrintWriter pw = FileUtil.getGzipPrintWriter(outputFile);

		int totSources=0;
		int tatIndexes = 0;
		HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, int[]>> seqGapFreqTable = null;
		IdentityArrayList<String> source = null;		

		PrintProgress pp = new PrintProgress("Converting original table file", 10000, 0);
		while(scan.hasNextLine()) {
			pp.next();
			String line = scan.nextLine();
			String[] split = line.split("\t");
			if (split.length==1) {
				pw.println(line);
				continue; //2: 		
			}
			if (split.length==2) {
				//\t[will, red] 		
				print(source, seqGapFreqTable, pw);
				seqGapFreqTable = new HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, int[]>>();
				totSources++;
				source =  ParallelSubstrings.getIdentityArrayListFromBracket(split[1]);
				continue;
			}
			//split.length==4
			//\t\t[in, rosso]\t[106101, 133666]
			IdentityArrayList<String> target =  ParallelSubstrings.getIdentityArrayListFromBracket(split[2]);
			int[] indexes = ParallelSubstrings.getIndexeArrayFromParenthesis(split[3]);
			tatIndexes += indexes.length;

			for(int i: indexes) {
				String[] sentS = sentSource.get(i);
				String[] sentT = sentTarget.get(i);
				IdentityArrayList<String> sourceStringGap = getStringGap(sentS, source, i);
				IdentityArrayList<String> targetStringGap = getStringGap(sentT, target, i);
				if (sourceStringGap==null || targetStringGap==null) {
					return;
				}
				Utility.increaseInHashMap(seqGapFreqTable, sourceStringGap, targetStringGap);					
			}
						
		}
				
		Parameters.reportLine("Total sources:" + totSources);
		Parameters.reportLine("Total indexes:" + tatIndexes);

		pp.end();
		pw.close();

		Parameters.closeLogFile();
	}

	private static void print(
			IdentityArrayList<String> source, HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, int[]>> seqGapFreqTable,
			PrintWriter pw) {
		
		if (seqGapFreqTable==null)
			return;
		pw.println("\t" + source);
		for(Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, int[]>> e : seqGapFreqTable.entrySet()) {
			IdentityArrayList<String> sourceKey = e.getKey();
			HashMap<IdentityArrayList<String>, int[]> subTable = e.getValue();			
			pw.println("\t\t" + sourceKey);
			HashMap<IdentityArrayList<String>, Integer> subTableInt = Utility.convertHashMapIntArrayInteger(subTable);
			TreeMap<Integer, IdentityArrayList<String>> subTableIntInv = Utility.invertHashMapInTreeMap(subTableInt);
			for(Entry<Integer, IdentityArrayList<String>> f : subTableIntInv.descendingMap().entrySet()) {
				pw.println("\t\t\t" + f.getKey() + "\t" + f.getValue());
			}
		}
	}

	private static IdentityArrayList<String> getStringGap(String[] sent,
			IdentityArrayList<String> seq, int i) {
		
		if (!checkPresence(sent, seq)) {
			System.err.println("Sequence not present in sentence");
			System.err.println("Sequence: " + seq);
			System.err.println("Sentence # " + i);
			System.err.println("Setnence:  " + Arrays.toString(sent));
			return null;
		}
		if (hasGap(sent, seq)) {
			return StringsAlignment.getBestAlignedSubseqWithGaps(seq.toArray(new String[]{}),sent);
		}
		return seq;
	}

	public static boolean checkPresence(String[] sent, IdentityArrayList<String> exp) {
		Iterator<String> iter = exp.iterator();
		String next = iter.next();
		int i=0;
		while(true) {			
			if (sent[i]!=next) {//not matched
				if (++i==sent.length)
					return false; // not all consumed
			}
			else { //match
				if (iter.hasNext())
					next = iter.next();
				else
					return true; // all consumed
			}				
		} 			
	}

	public static boolean hasGap(String[] sent, IdentityArrayList<String> exp) {
		int lastIndex = sent.length-exp.size();
		for(int i=0; i<=lastIndex; i++) {
			if (hasContiguous(sent, exp, i))
				return false;
		}
		return true;
	}
	
	public static ArrayList<Integer> getContiguousIndexes(String[] sent, IdentityArrayList<String> exp) {
		ArrayList<Integer> result = new ArrayList<Integer>(exp.size());
		int lastIndex = sent.length-exp.size();
		for(int i=0; i<=lastIndex; i++) {
			if (hasContiguous(sent, exp, i)) {
				for(int j=0; j<exp.size(); j++) {
					result.add(i+j);
				}
				return result;
			}				
		}
		return result;
	}




	private static boolean hasContiguous(String[] sent,
			IdentityArrayList<String> exp, int i) {
		Iterator<String> iter = exp.iterator();
		while(iter.hasNext()) {
			if (sent[i++]!=iter.next())
				return false;
		}
		return true;
	}

	public static void main(String[] args) throws FileNotFoundException {

		/*
		args = new String[]{
			"/Users/fedja/Dropbox/ted_experiment/corpus_en_it/train.tags.en-it.clean.tok.lc.en",
			"/Users/fedja/Dropbox/ted_experiment/en_it/kernels/kernels.table.m2.gz",
			"/Users/fedja/Dropbox/ted_experiment/en_it/kernels/kernels.table.m2.gaps_cont.gz"
		};
		 */

		//args0: sentenceSourceFile
		//args1: sentenceTargetFile
		//args2: tableFile
		//args3: outputFile
		separeContiguousFromGaps(new File(args[0]),new File(args[1]),new File(args[2]),new File(args[3]));
	}

}
