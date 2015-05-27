package kernels.parallel.ted;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Vector;

import kernels.algo.StringsAlignment;
import kernels.parallel.CheckGaps;
import kernels.parallel.ParallelSubstrings;
import util.IdentityArrayList;
import util.Utility;

/* 
 * 77 fields (76 if MWE8 is not filled)
 * 		
 *  0: SNT#
 *  1: SOURCE (EN)
 *  2: MANUAL
 *  3: AUTO
 *  4: DONE?
 *  5-13: MWE1
 *  	5: SOURCE-TEXT
 *  	6: /BAR/	
 *  	7: MANUAL-TEXT
 *  	8: /BAR/
 *  	9: MANUAL-CHECK
 *     10: CORRECT
 *     11: AUTO-TEXT
 *     12: /BAR/
 *     13: AUTO-CHECK
 *  14-22: MWE2
 *  23-32: MWE3
 *  33-41: MWE4
 *  42-50: MWE5
 *  51-59: MWE6
 *  60-68: MWE7
 *  69-77: MWE8
 */ 

public class ParallelSentenceAnnotations {
	
	//sentenceNumber -> ParallelSentence
	int sentenceNumber;
	String sourceSentence;
	String manualTranslation;
	String autoTranslation;
	TreeMap<Integer, Vector<AnnotatedMWE>> annotations = new TreeMap<Integer, Vector<AnnotatedMWE>>();
	// annotatorNumber -> annotations
	
	public class AnnotatedMWE {
		String sourceSentenceMWE;
		String manualTranslationMWE;		
		boolean manualCheck;
		String manualCorrection; //if manualCheck==false
		String autoTranslationMWE;
		boolean autoCheck;
		
		public IdentityArrayList<String> getSouceMweIdentityArray() {
			String[] split = sourceSentenceMWE.split("\\s+");
			for(int i=0; i<split.length; i++) {
				split[i] = split[i].trim().intern();
			}  
			return new IdentityArrayList<String>(split);
		}
		
		public boolean equals(Object o) {
			if (o instanceof AnnotatedMWE) {
				AnnotatedMWE a = (AnnotatedMWE)o;
				return this.sourceSentenceMWE.equals(a.sourceSentenceMWE);
			}
			return false;
		}
		
		public int hashCode() {
			return sourceSentenceMWE.hashCode();
		}
		
	}
	
	public ParallelSentenceAnnotations(String[] fields) {
		sentenceNumber = Integer.parseInt(fields[0].trim());
		sourceSentence = fields[1].trim();
		manualTranslation = fields[2].trim();
		autoTranslation = fields[3].trim();
	}

	public int numberOfAnnotators() {
		Iterator<Vector<AnnotatedMWE>> iter = annotations.values().iterator();
		int size = 0;
		while(iter.hasNext()) {
			if (iter.next()!=null)
				size++;
		}
		return size;
	}
	
	public int totalAnnotations() {
		Iterator<Vector<AnnotatedMWE>> iter = annotations.values().iterator();
		int total = 0;
		while(iter.hasNext()) {
			Vector<AnnotatedMWE> next = iter.next();
			if (next!=null) {
				total++;				
			}
		}
		return total;
	}
	
	public Collection<Integer> getMissingAnnotators() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for(Entry<Integer, Vector<AnnotatedMWE>> e : annotations.entrySet()) {
			if (e.getValue()==null)
				result.add(e.getKey());
		}
		return result;
	}
	
	public boolean hasMultipleAnnotators() {
		return numberOfAnnotators()>1;
	}
	
	public boolean hasAnnotator(int annotatorId) {
		return annotations.get(annotatorId)!=null;
	}

	public boolean atLeastTwoAnnotationsMathchInSize(int[] size) {		
		Iterator<Vector<AnnotatedMWE>> iter = annotations.values().iterator();
		HashSet<Integer> sizes = new HashSet<Integer>(); 
		while(iter.hasNext()) {
			Vector<AnnotatedMWE> next = iter.next();
			if (next!=null) {
				int s = next.size();
				if (!sizes.add(s)) { //already present
					size[0] = s;
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean atLeastTwoAnnotationsMatchInSourceMWE(int[] size) {
		if (!hasMultipleAnnotators())
			return false;	
		HashSet<ArrayList<String>> sourceMergedSet = new HashSet<ArrayList<String>>();
		Iterator<Vector<AnnotatedMWE>> iter = annotations.values().iterator();
		while(iter.hasNext()) {
			Vector<AnnotatedMWE> next = iter.next();
			if (next==null)
				continue;
			ArrayList<String> sourceMerged = getSouceMWEs(next); 
			if (!sourceMergedSet.add(sourceMerged)) { //already present
				size[0] = next.size();
				return true;
			}
		}
		return false;
	} 
	

	public static ArrayList<String> getSouceMWEs(Vector<AnnotatedMWE> v) {
		ArrayList<String> sourceMWEs = new ArrayList<String>(); 
		for(AnnotatedMWE a : v) {
			sourceMWEs.add(a.sourceSentenceMWE);
		}
		return sourceMWEs;
	}
	
	
	
	public boolean addAnnotationLine(String[] fields, int annNumb) {		
		
		boolean done = isYes(fields[4]);		
		if (!done) {
			this.annotations.put(annNumb, null);
			return false;
		}
		
		Vector<ParallelSentenceAnnotations.AnnotatedMWE> mweAnnotations = new Vector<ParallelSentenceAnnotations.AnnotatedMWE>();
		this.annotations.put(annNumb, mweAnnotations);
		
		int fieldIndex = 5;		
		for(int i=0; i<8; i++) { //max 8 mwe per sentence
			String mwe_sourceSentence = fields[fieldIndex].trim();
			if (mwe_sourceSentence.isEmpty())
				break;			
			ParallelSentenceAnnotations.AnnotatedMWE an = new AnnotatedMWE();
			an.sourceSentenceMWE = mwe_sourceSentence; //5
			fieldIndex+=2; //skip bar
			an.manualTranslationMWE = fields[fieldIndex].trim(); //7
			fieldIndex+=2; //skip bar
			an.manualCheck = isYes(fields[fieldIndex]); //9
			fieldIndex++;
			if (!an.manualCheck) { //if manualCheck=="N"
				an.manualCorrection = fields[fieldIndex].trim(); //10 
			}
			fieldIndex++;
			an.autoTranslationMWE = fields[fieldIndex].trim(); //11
			fieldIndex+=2; //skip bar
			an.autoCheck = isYes(fields[fieldIndex]); //13
			fieldIndex++;
			mweAnnotations.add(an);
		}
		
		return true;
	}
	
	private static boolean isYes(String s) {
		return s.trim().toUpperCase().startsWith("Y");
	}

	public void reportAnnotationDisagreementInNumber(int targetAnnotator, PrintWriter pw) {
		
		if (!hasMultipleAnnotators() || !hasAnnotator(targetAnnotator))
			return;
		
		Vector<AnnotatedMWE> targetAnnotation = annotations.get(targetAnnotator);
		int targetSize = targetAnnotation.size();
		for(Entry<Integer, Vector<AnnotatedMWE>> e : annotations.entrySet()) {
			Vector<AnnotatedMWE> next = e.getValue();			
			if (next==targetAnnotation || next==null)
				continue;
			if (next.size()!=targetSize) {
				int otherAnnId = e.getKey();
				pw.println();
				pw.println("Sentence: " + this.sentenceNumber);
				pw.println("\t" + "Source: " + this.sourceSentence);
				pw.println("\t" + "Your MWE(s) " + "\t->\t" + targetSize + "\t" + getSouceMWEs(targetAnnotation));
				pw.println("\t" + "Ann." + otherAnnId + " MWE(s)" + "\t->\t" + next.size() + "\t" + getSouceMWEs(next));
			}
		}
	}
	
	public void reportAnnotationDisagreementInText(int targetAnnotator, PrintWriter pw) {
		
		if (!hasMultipleAnnotators() || !hasAnnotator(targetAnnotator))
			return;
		
		Vector<AnnotatedMWE> targetAnnotation = annotations.get(targetAnnotator);
		int targetSize = targetAnnotation.size();
		for(Entry<Integer, Vector<AnnotatedMWE>> e : annotations.entrySet()) {
			Vector<AnnotatedMWE> next = e.getValue();			
			if (next==targetAnnotation || next==null)
				continue;
			if (!targetAnnotation.equals(next)) {
				int otherAnnId = e.getKey();
				pw.println();
				pw.println("Sentence: " + this.sentenceNumber);
				pw.println("\t" + "Source: " + this.sourceSentence);
				pw.println("\t" + "Your MWE(s) " + "\t->\t" + targetSize + "\t" + getSouceMWEs(targetAnnotation));
				pw.println("\t" + "Ann." + otherAnnId + " MWE(s)" + "\t->\t" + next.size() + "\t" + getSouceMWEs(next));
			}
		}
	}
	
	public void reportAllAnnotations(PrintWriter pw) {
	
		pw.println();
		pw.println("Sentence: " + this.sentenceNumber);
		pw.println("\t" + "Source: " + this.sourceSentence);
		
		for(Entry<Integer, Vector<AnnotatedMWE>> a : annotations.entrySet()) {
			int annNumber = a.getKey();
			Vector<AnnotatedMWE> ann = a.getValue();
			if (ann==null)
				pw.println("\t" + "Ann." + annNumber + " MWE(s)" + "\t->\t" + "NO ANNOTATIONS YET");
			else
				pw.println("\t" + "Ann." + annNumber + " MWE(s)" + "\t->\t" + ann.size() + "\t" + getSouceMWEs(ann));
		}
	}
	
	public void buildAggregatedTable(PrintWriter pw) {
		pw.print(this.sentenceNumber + "\t");
		pw.print(this.sourceSentence + "\t");
		pw.print(this.manualTranslation + "\t");
		pw.print(this.autoTranslation + "\t");
		pw.println();
		for(Entry<Integer, Vector<AnnotatedMWE>> a : annotations.entrySet()) {
			int annNumber = a.getKey();
			pw.print("\t\t\t\t");
			pw.print(annNumber + "\t\t");
			Vector<AnnotatedMWE> ann = a.getValue();
//			if (ann!=null) {
				for(AnnotatedMWE mwe : ann) {					
					pw.print(mwe.sourceSentenceMWE + "\t");
					pw.print(mwe.manualTranslationMWE + "\t");
					pw.print(booleanToYN(mwe.manualCheck) + "\t");
					pw.print(stringNulltoString(mwe.manualCorrection) + "\t");
					pw.print(mwe.autoTranslationMWE + "\t");
					pw.print(booleanToYN(mwe.autoCheck) + "\t");
				}				
//			}				
			pw.println();
		}
		pw.print("\t\t\t\t");
		pw.print("FINAL" + "\t" + "N"); // done? NO
		pw.println();
	}

	public HashMap<ArrayList<Integer>, int[]> buildCheckPresenceSourceReport(PrintWriter pw, int[] counts) {
		// counts
		// 0: present contiguous
		// 1: present gaps
		// 2: total
		HashMap<ArrayList<Integer>, int[]> spanTable = new HashMap<ArrayList<Integer>, int[]>(); 
		StringBuilder sb = new StringBuilder();
		String[] snt = ParallelSubstrings.getInternedArrya(this.sourceSentence, "\\s+");
		sb.append(this.sentenceNumber + "\n");		
		sb.append("\t" + "Source: " + this.sourceSentence + "\n");
		boolean foundMistake = false;
		for(Entry<Integer, Vector<AnnotatedMWE>> a : annotations.entrySet()) {
			int annNumber = a.getKey();
			Vector<AnnotatedMWE> ann = a.getValue();
			for(AnnotatedMWE mwe : ann) {					
				counts[2]++;
				String expString = mwe.sourceSentenceMWE;
				expString = expString.replaceAll("\\.\\.\\.", " ");
				expString = expString.replaceAll("'re", " 're");
				expString = expString.replaceAll("'s", " 's");
				expString = expString.toLowerCase();
				String[] expArray = expString.split("\\s+");
				IdentityArrayList<String> exp = ParallelSubstrings.getIdentityArrayList(expString, "\\s+");
				ArrayList<Integer> indexes = null;
				if (CheckGaps.checkPresence(snt, exp)) {
					if (CheckGaps.hasGap(snt, exp)) {
						counts[1]++;
						indexes = StringsAlignment.getBestIndexAlignemnt(expArray, snt);
					}
					else { 				
						//contiguous
						counts[0]++;
						indexes = CheckGaps.getContiguousIndexes(snt,exp);
					}
					Utility.increaseInHashMap(spanTable, indexes);
				}
				else {
					foundMistake = true;
					sb.append("\t\t" + "Ann." + annNumber + "\t" + mwe.sourceSentenceMWE + "\n");
				}
			}							
		}
		if (foundMistake) {
			pw.println(sb);
			pw.println();
		}
		return spanTable;
	}

	private String stringNulltoString(String manualCorrection) {		
		return manualCorrection==null ? "" : manualCorrection;
	}

	private String booleanToYN(boolean manualCheck) {		
		return manualCheck ? "Y" : "N";
	}

	public void addAllMWes(HashSet<IdentityArrayList<String>> manualMweSet) {		
		for(Vector<AnnotatedMWE> va : annotations.values()) {
			if (va!=null) {
				for(AnnotatedMWE a : va) {
					manualMweSet.add(a.getSouceMweIdentityArray());
				}
			}
		}
	}
	
	public static void main(String[] args) {

		String exp = "fdsa fd safdas dfsaf...safda sfas dfa";
		String[] expSplit = exp.replaceAll("\\.\\.\\.", " ").split("\\s+"); 
		System.out.println(Arrays.toString(expSplit)); 

	}
	
}
