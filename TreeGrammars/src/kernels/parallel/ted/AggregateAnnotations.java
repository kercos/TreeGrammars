package kernels.parallel.ted;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import kernels.parallel.ExtracSourceFromTable;
import util.IdentityArrayList;



public class AggregateAnnotations {
	
	static final int preannotatedSentneces = 23;
	static Vector<String> annotators = new Vector<String>();
	static TreeMap<Integer, ParallelSentenceAnnotations> parallelSentenceData = new TreeMap<Integer, ParallelSentenceAnnotations>();

	public static void aggregateAnnotations() throws FileNotFoundException {
		annotators.add("PREANNOTATOR");
		File[] annFiles = new File(TED_Corpus.studentAnnotationPath).listFiles();
		Arrays.sort(annFiles);
		for(File f : annFiles) {
			processFile(f);
		}
		
	}
	
	private static void processFile(File f) throws FileNotFoundException {
		String fileName = f.getName();
		String[] split = fileName.split(" - ");
		int annNumb = Integer.parseInt(split[0]);
		//String annName = split[1];
		//System.out.println(annNumb + " " + annName);
		//annotators.add(annName);
		annotators.add(fileName.substring(0, fileName.indexOf('.')));
		Scanner scan = new Scanner(f);
		String line = null;
		scan.nextLine(); // skip FIRST header row		
		scan.nextLine(); // skip SECOND header row
		for(int i=0; i<preannotatedSentneces; i++) {
			line = scan.nextLine();
			if (annNumb==1) { //read preannotated sentences only once
				addAnnotationLine(line, 0);
			}
		}
		while(scan.hasNextLine()) {
			line = scan.nextLine();
			addAnnotationLine(line, annNumb);
		}		
		scan.close();
	} 
	
	public static boolean addAnnotationLine(String line, int annNumb) {
		String[] fields = line.split("\t");
		if (fields.length<76) {
			System.out.println("PROBLEM IN ANNOTATION FILE " + annNumb + " AT SENTENCE " + fields[0]);
			return false;
		}
		
		int sntNum = Integer.parseInt(fields[0]);

		ParallelSentenceAnnotations ps = parallelSentenceData.get(sntNum);
		if (ps==null) {
			ps = new ParallelSentenceAnnotations(fields);			
			parallelSentenceData.put(sntNum, ps);
		}
		ps.addAnnotationLine(fields, annNumb);
		return true;
	}
	
	

	private static void reportMWEstats() {
		int multipleAnnotatedSentences = 0;
		int agreementInNumberOfMwe = 0;
		int exactMatchMweSource = 0;
		int totalMwe = 0; //tokens, types
		int agreementInNumberOfMweBins[] = new int[10];
		int exactMatchMweSourceBins[] = new int[10];
		for(Entry<Integer, ParallelSentenceAnnotations> e : parallelSentenceData.entrySet()) {
			ParallelSentenceAnnotations ps = e.getValue();
			totalMwe += ps.totalAnnotations();
			if (ps.hasMultipleAnnotators()) {
				multipleAnnotatedSentences++;
				int[] size = new int[]{0};
				if (ps.atLeastTwoAnnotationsMathchInSize(size)) {
					agreementInNumberOfMwe++;
					agreementInNumberOfMweBins[size[0]]++;
					if (ps.atLeastTwoAnnotationsMatchInSourceMWE(size)) {
						exactMatchMweSource++;
						exactMatchMweSourceBins[size[0]]++;
					}
				}	
			}
		}		
		
		System.out.println("Total MWEs: " + totalMwe);
		
		System.out.println("Agreement in number of MWEs (at least 2 annotators agree) " + agreementInNumberOfMwe + " (" + percentage(agreementInNumberOfMwe,multipleAnnotatedSentences) + ")");
		for(int i=0; i<agreementInNumberOfMweBins.length; i++) {
			if (agreementInNumberOfMweBins[i]>0)
				System.out.println("\t" + i  + " MWE(s): " + agreementInNumberOfMweBins[i]);
		}
		
		System.out.println("Agreement in all sources MWEs (at least 2 annotators agree) " + exactMatchMweSource + " (" + percentage(exactMatchMweSource,multipleAnnotatedSentences) + ")");
		for(int i=0; i<exactMatchMweSourceBins.length; i++) {
			if (exactMatchMweSourceBins[i]>0)
				System.out.println("\t" + i  + " MWE(s): " + exactMatchMweSourceBins[i]);
		}
	}

	private static void reportNumberOfSentences() {
		int sentencesInCorpus = parallelSentenceData.size();
		System.out.println("Numer of sentences in corpus: " + sentencesInCorpus);
		int annotatedSentences = 0;
		int[] annotatedSentencesBins = new int[10];
		int multipleAnnotatedSentences = 0;
		int[] multipleAnnotatedSentencesBin = new int[10];
		TreeMap<Integer,TreeSet<Integer>> remainingMultipleSentences = new TreeMap<Integer, TreeSet<Integer>>(); 
		for(Entry<Integer, ParallelSentenceAnnotations> e : parallelSentenceData.entrySet()) {
			int sentenceNumb = e.getKey();
			ParallelSentenceAnnotations ps = e.getValue();		
			annotatedSentences += ps.numberOfAnnotators();
			for(Vector<ParallelSentenceAnnotations.AnnotatedMWE> a : ps.annotations.values()) {
				if (a!=null)
					annotatedSentencesBins[a.size()]++;
			}
			if (ps.hasMultipleAnnotators()) {
				multipleAnnotatedSentences++;
				multipleAnnotatedSentencesBin[ps.numberOfAnnotators()]++;
			}
			else if (sentenceNumb>preannotatedSentneces) {
				TreeSet<Integer> missinAnnotators = remainingMultipleSentences.get(sentenceNumb);
				if (missinAnnotators==null) {
					missinAnnotators = new TreeSet<Integer>();
					remainingMultipleSentences.put(sentenceNumb, missinAnnotators);
				}
				missinAnnotators.addAll(ps.getMissingAnnotators());
			}
		}
		System.out.println("Numer of annotated sentences (with repetitions): " + annotatedSentences);
		for(int i=0; i<annotatedSentencesBins.length; i++) {
			if (annotatedSentencesBins[i]>0)
				System.out.println("\t" + i  + " MWE(s): " + annotatedSentencesBins[i]);
		}
		System.out.println("Preannotated sentences: " + preannotatedSentneces);
		System.out.println("Multiple annotated sentences: " + multipleAnnotatedSentences);
		for(int i=0; i<multipleAnnotatedSentencesBin.length; i++) {
			if (multipleAnnotatedSentencesBin[i]>0)
				System.out.println("\t" + i + " annotators: " + multipleAnnotatedSentencesBin[i]);
		}
		System.out.print("Remaining multiple sentences to annotate: ");		
		System.out.println(remainingMultipleSentences.size() + " " + remainingMultipleSentences);
	}
	
	private static void buildStudentFeedback() throws FileNotFoundException {		
		for(int i=1; i<annotators.size(); i++) {
			PrintWriter pw = new PrintWriter(new File(TED_Corpus.studentFeedbackPath + annotators.get(i)) + ".txt");
			pw.println("FRASI IN CUI LE TUE ANNOTAZIONI DELLE MWEs NELLE FRASI DI PARTENZA (SOURCE TEXT) \n"
					+ "DIFFERISCONO DA QUELLE DI UN ALTRO ANNOTATORE"); //*IN NUMERO* 
			for(Entry<Integer, ParallelSentenceAnnotations> e : parallelSentenceData.entrySet()) {
				ParallelSentenceAnnotations ps = e.getValue();			
				//ps.reportAnnotationDisagreementInNumber(i,pw); // only differences in numbers 
				ps.reportAnnotationDisagreementInText(i,pw); // differences in text
			}
			pw.close();
		}		
	}
	
	private static void buildGeneralFeedbackReport() throws FileNotFoundException {		
		PrintWriter pw = new PrintWriter(new File(TED_Corpus.studentFeedbackPath + "GeneralReport.txt"));
		for(Entry<Integer, ParallelSentenceAnnotations> e : parallelSentenceData.entrySet()) {
			ParallelSentenceAnnotations ps = e.getValue();			
			ps.reportAllAnnotations(pw);				
		}
		pw.close();		
	}
	
	private static void buildAggregatedTable() throws FileNotFoundException {		
		PrintWriter pw = new PrintWriter(new File(TED_Corpus.studentFeedbackPath + "AggregatedTable.txt"));
		for(Entry<Integer, ParallelSentenceAnnotations> e : parallelSentenceData.entrySet()) {
			ParallelSentenceAnnotations ps = e.getValue();			
			ps.buildAggregatedTable(pw);				
		}
		pw.close();		
	}
	
	
	
	private static void buildCheckPresenceSourceReport() throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(TED_Corpus.studentFeedbackPath + "CheckPresenceSourceReport.txt"));
		int[] counts = new int[3]; // present_contiguous, present_gaps, total
		int[] total_match_overlap = new int[3];
		for(Entry<Integer, ParallelSentenceAnnotations> e : parallelSentenceData.entrySet()) {
			ParallelSentenceAnnotations ps = e.getValue();			
			HashMap<ArrayList<Integer>, int[]> spanTable = ps.buildCheckPresenceSourceReport(pw, counts);
			//System.out.println(e.getKey());
			countTotalMatchOverlaps(spanTable, total_match_overlap);			
		}
		pw.println();
		
		int totPres = counts[0]+counts[1];
		int mistkaes = counts[2]-totPres;
		pw.println("Present_in_source/Total MWEs " + totPres + "/" + counts[2] + " -> " + percentage(totPres,counts[2]));
		pw.println("Not_in_source/Total MWEs " + mistkaes + "/" + counts[2] + " -> " + percentage(mistkaes,counts[2]));
		pw.println("Present_in_source_contiguous/Total MWEs " + counts[0] + "/" + counts[2] + " -> " + percentage(counts[0],counts[2]));
		pw.println("Present_in_source_gaps/Total MWEs " + counts[1] + "/" + counts[2] + " -> " + percentage(counts[1],counts[2]));
		
		pw.println("Matching MWEs for at least two annotators: " + total_match_overlap[1] + "/" + total_match_overlap[0] + " (" + percentage(total_match_overlap[1],total_match_overlap[0]) + ")");
		pw.println("Partial overlapping MWEs for at least two annotators: " + total_match_overlap[2] + "/" + total_match_overlap[0] + " (" + percentage(total_match_overlap[2],total_match_overlap[0]) + ")");
		pw.println();
		pw.close();		
		
	}



	private static void countTotalMatchOverlaps(
			HashMap<ArrayList<Integer>, int[]> spanTable,
			int[] total_match_overlap) {
				
		for(Entry<ArrayList<Integer>, int[]> f : spanTable.entrySet()) {
			int[] v = f.getValue();			
			int freq = v[0];
			total_match_overlap[0] += freq;
			if (freq>1) {
				total_match_overlap[1]+=freq;
				total_match_overlap[2]+=freq;
				//total_match_overlap[2];
			}
			else {
				ArrayList<Integer> k = f.getKey();
				for(Entry<ArrayList<Integer>, int[]> g : spanTable.entrySet()) {
					int[] v2 = g.getValue(); 
					if (v2==v || v2[0]>1)
						continue;
					HashSet<Integer> set = new HashSet<Integer>(k);
					set.retainAll(g.getKey());
					if (!set.isEmpty())
						total_match_overlap[2]++;
				}
			}			
			//System.out.println("\t" + f.getKey() + "\t" + f.getValue()[0]);
		}
		
	}



	static DecimalFormat df = new DecimalFormat("0.0");
	public static String percentage(int num, int den) {
		return df.format(((double)num)*100/den) + "%";
	}
	
	private static void checkSourceCoverageInKernels(File kernelFile) {		
		
		Set<IdentityArrayList<String>> kernelMWE = 
				ExtracSourceFromTable.extractSourceFreqFromTable(kernelFile).keySet();
		
		for(IdentityArrayList<String> mwe : kernelMWE) {
			CleanTED_Symbols.cleanIdentityArray(mwe);
		}
		
		HashSet<IdentityArrayList<String>> manualMWE = new HashSet<IdentityArrayList<String>>();
		for(ParallelSentenceAnnotations v : parallelSentenceData.values()) {
			v.addAllMWes(manualMWE);			
		}
		HashSet<IdentityArrayList<String>> intersectionMWE = new HashSet<IdentityArrayList<String>>(manualMWE);
		intersectionMWE.retainAll(kernelMWE);
		System.out.println("Size of auto (kernels) MWE set " + kernelMWE.size());
		System.out.println("Size of manual MWE set " + manualMWE.size());
		System.out.println("Size of intersection " + intersectionMWE.size());
		System.out.println("Intersection: " + intersectionMWE.toString());
	}
	
	public static void aggregateManualAndBuildReports() throws FileNotFoundException {
		aggregateAnnotations();
		reportNumberOfSentences();
		reportMWEstats();
		buildStudentFeedback();
		buildGeneralFeedbackReport();
		buildAggregatedTable();
		buildCheckPresenceSourceReport();
	}
	


	public static void main(String[] args) throws FileNotFoundException {		
		aggregateManualAndBuildReports();
		
		//aggregateAnnotations();
		//File kernelFile = TED_Corpus.kernelM2FileEnIt;
		//File kernelFile = new File("/Volumes/HardDisk/Scratch/RESULTS/TEDParallelKernels/ParallelSubstring_TED_Words_Min2/prunedTable.gz");
		//File kernelFile = new File("/Users/fedja/Dropbox/ted_experiment/en_it/kernels/kernels.table.m2.prune.threshold.0.8.gz");
		//checkSourceCoverageInKernels(kernelFile);
	}





}
