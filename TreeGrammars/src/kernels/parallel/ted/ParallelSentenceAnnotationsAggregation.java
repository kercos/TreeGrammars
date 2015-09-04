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
 * 62 fields (76 if MWE8 is not filled)
 * 		
 *  0: SNT#
 *  1: SOURCE (EN)
 *  2: MANUAL
 *  3: AUTO
 *  4: DONE?
 *  5-10: MWE1
 *  	5: SOURCE-TEXT
 *  	6: MANUAL-TEXT
 *  	7: MANUAL-CHECK
 *      8: CORRECT
 *      9: AUTO-TEXT
 *     10: AUTO-CHECK
 *  11-16: MWE2
 *  17-22: MWE3
 *  23-28: MWE4
 *  29-44: MWE5
 *  45-50: MWE6
 *  51-56: MWE7
 *  57-62: MWE8
 */ 

public class ParallelSentenceAnnotationsAggregation {
	
	//sentenceNumber -> ParallelSentence
	int sentenceNumber;
	String sourceSentence;
	String manualTranslation;
	String autoTranslation;
	Vector<AnnotatedMWE> annotations = new Vector<AnnotatedMWE>();
	// annotatorNumber -> annotations
	
	public class AnnotatedMWE {
		boolean manualCheck;
		String sourceSentenceMWE, manualTranslationMWE;
		String manualCorrection; //if manualCheck==false
		String autoTranslationMWE;
		boolean autoCheck;
		
		public int hashCode() {
			return manualCorrection.hashCode();
		}
		
		public String toString() {
			return sourceSentenceMWE + " -> " + manualTranslationMWE + "|" + autoCheck;
		}
		
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(sourceSentence);
		sb.append('\n');
		sb.append(manualTranslation);
		for(AnnotatedMWE a : annotations) {
			sb.append("\n\t");
			sb.append(a);
		}
		sb.append('\n');
		return sb.toString();
	}
	
	public ParallelSentenceAnnotationsAggregation(String[] fields) {
		sentenceNumber = Integer.parseInt(fields[0].trim());
		sourceSentence = fields[1].trim();
		manualTranslation = fields[2].trim();
		autoTranslation = fields[3].trim();
		addAnnotationLine(fields);
	}
	
	
	
	public boolean addAnnotationLine(String[] fields) {		
		
		boolean done = isYes(fields[4]);		
		if (!done) {
			return false;
		}
		
		annotations = new Vector<ParallelSentenceAnnotationsAggregation.AnnotatedMWE>();
		
		int fieldIndex = 5;		
		for(int i=0; i<8; i++) { //max 8 mwe per sentence
			String mwe_sourceSentence = fields[fieldIndex].trim();
			if (mwe_sourceSentence.isEmpty())
				break;			
			ParallelSentenceAnnotationsAggregation.AnnotatedMWE an = new AnnotatedMWE();
			an.sourceSentenceMWE = mwe_sourceSentence.toLowerCase(); //5
			fieldIndex++;
			an.manualTranslationMWE = fields[fieldIndex].trim().toLowerCase(); //6
			fieldIndex++;
			an.manualCheck = isYes(fields[fieldIndex]); //7
			fieldIndex++;
			if (!an.manualCheck) { //if manualCheck=="N"
				an.manualCorrection = fields[fieldIndex].trim().toLowerCase(); //8 
			}
			fieldIndex++;
			an.autoTranslationMWE = fields[fieldIndex].trim().toLowerCase(); //9
			fieldIndex++;
			an.autoCheck = isYes(fields[fieldIndex]); //10
			fieldIndex++;
			annotations.add(an);
		}
		
		return true;
	}
	
	private static boolean isYes(String s) {
		return s.trim().toUpperCase().startsWith("Y");
	}


	
	public void reportAllAnnotations(PrintWriter pw) {
	
		pw.println();
		pw.println("Sentence: " + this.sentenceNumber);
		pw.println("\t" + "Source: " + this.sourceSentence);		
		pw.println("\t" + " MWE(s)" + "\t->\t" + annotations.size() + "\t" + annotations);
	}
	
	public HashMap<ArrayList<Integer>, int[]> buildCheckPresenceSourceReport(
			PrintWriter pw, int[] counts, int[] sizes) {
		// counts
		// 0: present contiguous
		// 1: present gaps
		// 2: MT correct
		// 3: total
		HashMap<ArrayList<Integer>, int[]> mweFreq = new HashMap<ArrayList<Integer>, int[]>(); 
		StringBuilder sb = new StringBuilder();
		String[] snt = ParallelSubstrings.getInternedArrya(this.sourceSentence, "\\s+");
		sb.append(this.sentenceNumber + "\n");		
		sb.append("\t" + "Source: " + this.sourceSentence + "\n");
		boolean foundMistake = false;
		for(AnnotatedMWE mwe : annotations) {					
			counts[3]++;
			String expString = mwe.sourceSentenceMWE;
			expString = expString.replaceAll("\\.\\.\\.", " ");
			//expString = expString.replaceAll("'re", " 're");
			//expString = expString.replaceAll("'s", " 's");
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
				Utility.increaseInHashMap(mweFreq, indexes);
				sizes[indexes.size()]++;
			}
			else {
				foundMistake = true;
				sb.append("\t\t" + mwe.sourceSentenceMWE + "\n");
			}
			if (mwe.autoCheck) {
				// correctly translated by 
				counts[2]++;
			}
		}							
		if (foundMistake) {
			pw.println(sb);
			pw.println();
		}
		return mweFreq;
	}

	
}
