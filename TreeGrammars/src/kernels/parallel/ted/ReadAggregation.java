package kernels.parallel.ted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Map.Entry;

import util.IdentityArrayList;
import util.Utility;
import kernels.parallel.CheckGaps;
import kernels.parallel.ParallelSubstrings;
import kernels.parallel.ted.ParallelSentenceAnnotationsAggregation.AnnotatedMWE;

public class ReadAggregation {
	
	static TreeMap<Integer, ParallelSentenceAnnotationsAggregation> parallelSentenceData = 
			new TreeMap<Integer, ParallelSentenceAnnotationsAggregation>();

	
	private static void readAggreagationFile(File f) throws FileNotFoundException {
		Scanner scan = new Scanner(f);		
		scan.nextLine(); // skip FIRST header row		
		scan.nextLine(); // skip SECOND header row
		String firstLine = null;
		String lastLine = null;
		String[] fieldsFirst = null;
		String[] fieldsLast = null;
		String[] fields = null;
		while(true) {
			firstLine = scan.nextLine();
			fieldsFirst = firstLine.split("\t");
			int sntNum = Integer.parseInt(fieldsFirst[0]);
			//if (fieldsFirst.length<62) {
			//	System.out.println("PROBLEM IN ANNOTATION AT SENTENCE " + sntNum);
			//}
			
			while(scan.hasNextLine()) {
				lastLine = scan.nextLine();
				fieldsLast = lastLine.split("\t");
				String forth = fieldsLast[4];
				if(forth.equals("FINAL")) {
					break;
				}
			}
			
			fieldsFirst = Arrays.copyOfRange(fieldsFirst, 0, 4);
			fieldsLast = Arrays.copyOfRange(fieldsLast, 5, fieldsLast.length);			
			fields = new String[62];
			int i=0;
			for(String s : fieldsFirst) {
				fields[i++] = s;
			}
			for(String s : fieldsLast) {
				fields[i++] = s;
			}	
			while(i<62) {
				fields[i++] = "";
			}
			
			if (fields[4].trim().toUpperCase().startsWith("Y")) {
				ParallelSentenceAnnotationsAggregation ps = 
						new ParallelSentenceAnnotationsAggregation(fields);;
				parallelSentenceData.put(sntNum, ps);				
			}
									
			if (!scan.hasNext()) {
				break;
			}
		}		
		scan.close();
		System.out.println("Annotated FINAL sentences: " + parallelSentenceData.size());
		
	}
	
	private static void buildCheckPresenceSourceReport(File file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		int[] counts = new int[4]; // present_contiguous, present_gaps, MT_ok, total
		int[] sizes = new int[20];
		HashMap<ArrayList<Integer>, int[]> mweFreq = new HashMap<ArrayList<Integer>, int[]>();
		for(Entry<Integer, ParallelSentenceAnnotationsAggregation> e : parallelSentenceData.entrySet()) {
			ParallelSentenceAnnotationsAggregation ps = e.getValue();			
			HashMap<ArrayList<Integer>, int[]> psMweFreqps = 
					ps.buildCheckPresenceSourceReport(pw, counts, sizes);
			Utility.addAll(psMweFreqps, mweFreq);
			//HashMap<ArrayList<Integer>, int[]> spanTable = ps.buildCheckPresenceSourceReport(pw, counts);
			//System.out.println(e.getKey());	
		}
		pw.println();
		
		int totPres = counts[0]+counts[1];
		int mistkaes = counts[3]-totPres;
		pw.println("TOKENS:");
		pw.println("Present_in_source/Total MWEs " + totPres + "/" + counts[3] + " -> " + percentage(totPres,counts[3]));
		pw.println("Not_in_source/Total MWEs " + mistkaes + "/" + counts[3] + " -> " + percentage(mistkaes,counts[3]));
		pw.println("Present_in_source_contiguous/Total MWEs " + counts[0] + "/" + counts[3] + " -> " + percentage(counts[0],counts[3]));
		pw.println("Present_in_source_gaps/Total MWEs " + counts[1] + "/" + counts[3] + " -> " + percentage(counts[1],counts[3]));
		pw.println("Correctly_MTtranslated_/Total MWEs " + counts[2] + "/" + counts[3] + " -> " + percentage(counts[2],counts[3]));
		pw.println("Sizes");
		for(int i=0; i<sizes.length; i++) {
			if (sizes[i]>0)
				pw.println("\t" + i + "\t" + sizes[i]);
		}
		
		pw.println("\n" + "TYPES:" + Utility.totalSumValues(mweFreq));
		pw.println();
		pw.close();		
		
	}
	
	
	static DecimalFormat df = new DecimalFormat("0.0");
	public static String percentage(int num, int den) {
		return df.format(((double)num)*100/den) + "%";
	}
	
	private static void printContiguousList(File mweFile, File sentFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(mweFile);
		PrintWriter pwSent = new PrintWriter(sentFile);
		HashMap<String, int[]> mweContSourceFreq = new HashMap<String, int[]>();
		HashMap<String, int[]> mweDiscSourceFreq = new HashMap<String, int[]>();
		int tokens = 0;
		int[] contSizes = new int[20];
		for(ParallelSentenceAnnotationsAggregation e : parallelSentenceData.values()) {
			String sourceSentence = e.sourceSentence;
			String[] snt = ParallelSubstrings.getInternedArrya(sourceSentence, "\\s+");
			pwSent.println(sourceSentence);
			for(AnnotatedMWE mwe : e.annotations) {
				String mweSource = mwe.sourceSentenceMWE;
				IdentityArrayList<String> exp = ParallelSubstrings.getIdentityArrayList(mweSource, "\\s+");
				if (CheckGaps.hasGap(snt, exp)) {					
					Utility.increaseInHashMap(mweDiscSourceFreq, mweSource);					
				}
				else {
					Utility.increaseInHashMap(mweContSourceFreq, mweSource);
					contSizes[exp.size()]++;
				}
				tokens++;
			}	
		}
		for(Entry<String, int[]> e : mweContSourceFreq.entrySet()) {
			pw.println(e.getKey() + "\t" + e.getValue()[0]);
		}
		pw.close();
		pwSent.close();
		
		int tot = mweDiscSourceFreq.size() + mweContSourceFreq.size();
		System.out.println("Total source mwe (tokens): " + tokens);
		System.out.println("Total source mwe (types): " + tot);
		System.out.println("Dicontinuous source mwe (types): " + mweDiscSourceFreq.size());
		System.out.println("Continuous source mwe (types): " + mweContSourceFreq.size());
		
		System.out.println("Cont. Sizes");
		for(int i=0; i<contSizes.length; i++) {
			if (contSizes[i]>0)
				System.out.println("\t" + i + "\t" + contSizes[i]);
		}
	}
	
	private static void printPairsList(File mwePairFile) throws FileNotFoundException {
		HashMap<String, HashMap<String, int[]>> mwePairFreq = new HashMap<String, HashMap<String, int[]>>();
		int tokens = 0;
		int pairs = 0;
		PrintWriter pw = new PrintWriter(mwePairFile);
		for(ParallelSentenceAnnotationsAggregation e : parallelSentenceData.values()) {
			String sourceSentence = e.sourceSentence;
			String[] snt = ParallelSubstrings.getInternedArrya(sourceSentence, "\\s+");
			for(AnnotatedMWE mwe : e.annotations) {
				String mweSource = mwe.sourceSentenceMWE;
				String mweTarget = mwe.manualTranslationMWE;
				Utility.increaseInHashMap(mwePairFreq, mweSource, mweTarget);
				tokens++;
			}	
		}
		for(Entry<String, HashMap<String, int[]>> e : mwePairFreq.entrySet()) {
			for(Entry<String, int[]> f : e.getValue().entrySet()) {
				pw.println(e.getKey() + "\t" + f.getKey() + "\t" + f.getValue()[0]);
				pairs++;
			}			
		}
		pw.close();
		
		System.out.println("Total mwe pairs (tokens): " + tokens);
		System.out.println("Total mwe pairs (types): " + pairs);
		
	}


	
	public static void main(String[] args) throws FileNotFoundException {
		String workingDir = "/Users/fedja/Dropbox/ted_experiment/annotation/";
		File aggregation = new File(workingDir + "Aggregated_2015_05_30.tsv");
		File aggregationCheck = new File(workingDir + "Aggregated_2015_05_30.check.txt");
		File aggregationContList = new File(workingDir + "Aggregated_2015_05_30.cont.mwe.txt");
		File aggregationSourceSentences = new File(workingDir + "Aggregated_2015_05_30.cont.sent.txt");
		File aggregationMwePairs = new File(workingDir + "Aggregated_2015_05_30.mwe_pairs.txt");
		readAggreagationFile(aggregation);		
		buildCheckPresenceSourceReport(aggregationCheck);
		printContiguousList(aggregationContList, aggregationSourceSentences);
		printPairsList(aggregationMwePairs);
		System.out.println(parallelSentenceData.size());
		
	}






}
