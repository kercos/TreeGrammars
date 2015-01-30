package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;

import settings.Parameters;
import util.IdentityArrayList;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class PrepareManualValidation {

	ArrayList<String[]> sourceSentences, targetSentences, mtTargetSentences;	
	private HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> table;
	private IdentityHashMap<String, 
		HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>>> firstWordIndexedTable;
	File outputFile, logFile;
	int overlappingSourcePhrases, overlappingTestSentences;
	
	public PrepareManualValidation(File tableFile, File sourceFile,
			File targetFile, File mtTargetFile) throws FileNotFoundException {
	
		outputFile =  FileUtil.changeExtension(tableFile, mtTargetFile.getName());
		outputFile =  FileUtil.changeExtension(outputFile, "txt");
		logFile = FileUtil.changeExtension(outputFile, "log");
		
		Parameters.openLogFile(logFile);
		Parameters.logStdOutPrintln("Source Sentences File: " + sourceFile);
		Parameters.logStdOutPrintln("Target Sentences File: " + targetFile);
		Parameters.logStdOutPrintln("Target MT Sentences File: " + mtTargetFile);
		Parameters.logStdOutPrintln("Kernel Table File: " + tableFile);
		
		sourceSentences = ParallelSubstrings.getInternedSentences(sourceFile);
		targetSentences = ParallelSubstrings.getInternedSentences(targetFile);
		mtTargetSentences = ParallelSubstrings.getInternedSentences(mtTargetFile);		
		table = ParallelSubstrings.readTableFromFile(tableFile);
		
		int[] tableKeyPairs = ParallelSubstrings.totalKeysAndPairs(table);
		
		Parameters.logStdOutPrintln("Number of Test Sentences: " + sourceSentences.size());
		Parameters.logStdOutPrintln("Table keys pairs: " + Arrays.toString(tableKeyPairs));
		
		Parameters.logStdOutPrint("Building FirstWordIndexedTable...");
		buildFirstWordIndexedTable();
		Parameters.logStdOutPrintln("done");
		
		findOverlapAndPrintManualTest();
		
		Parameters.logStdOutPrintln("Overlapping Test Sentences: " + overlappingTestSentences);
		Parameters.logStdOutPrintln("Overlapping Source Phrases: " + overlappingSourcePhrases);
		
		Parameters.closeLogFile();
	}

	private void buildFirstWordIndexedTable() {
		
		firstWordIndexedTable = new IdentityHashMap<String, 
				HashMap<IdentityArrayList<String>,HashMap<IdentityArrayList<String>,TreeSet<Integer>>>>();
		
		for(Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> e : table.entrySet()) {
			IdentityArrayList<String> key = e.getKey();
			String wordIndex = key.get(0);
			HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> subTable = firstWordIndexedTable.get(wordIndex);
			if (subTable == null) {
				subTable = new HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>,TreeSet<Integer>>>();
				firstWordIndexedTable.put(wordIndex, subTable);
			}
			subTable.put(key, e.getValue());			
		}
	}
	
	public static final String[] fields = new String[] {
		"s", 		//0
		"G_t", 		//1
		"M_t", 		//2
		"S", 		//3
		"MWE_S",	//4:manual
		"G_T",		//5:manual
		"E:G_T",	//6:manual
		"MWE_G_T",	//7:manual
		"M_T",		//8:manual
		"E:M_T",	//9:manual
		"MWE_M_T",	//10:manual
		"K_T",		//11
		"E:K_T",	//12:manual
		"MWE_K_T"	//13:manual
	};

	public void findOverlapAndPrintManualTest() {
		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		pw.println(Utility.arrayToString(fields, '\t'));
		
		Iterator<String[]> sourceIter = sourceSentences.iterator();
		Iterator<String[]> targetIter = targetSentences.iterator();
		Iterator<String[]> mtTargetIter = mtTargetSentences.iterator();
		String[] sourceSentence, targetSentence, mtTargetSentence;
		HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> subTable = null;
		IdentityArrayList<String> sourcePhrase, targetPhrase;
		
		PrintProgress pp = new PrintProgress("Finding overlap Test Sentences", 1000, 0);
		
		while(sourceIter.hasNext()) {
			sourceSentence = sourceIter.next();			
			targetSentence = targetIter.next();
			mtTargetSentence = mtTargetIter.next();			
			boolean overlap = false;
			for(int i=0; i<sourceSentence.length-1; i++) {
				pp.next();
				String wordIndex = sourceSentence[i];
				subTable = firstWordIndexedTable.get(wordIndex);				
				if (subTable!=null) {					
					for(Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> e : subTable.entrySet()) {						
						sourcePhrase = e.getKey();						
						if (match(sourceSentence, sourcePhrase, i)) {
							overlappingSourcePhrases++;
							if (!overlap) {
								overlap = true;
								overlappingTestSentences++;
							}
							targetPhrase = e.getValue().keySet().iterator().next();
							pw.print(Utility.arrayToString(sourceSentence, ' ')); //0:"s"
							pw.print("\t");
							pw.print(Utility.arrayToString(targetSentence, ' ')); //1:"G_t"
							pw.print("\t");
							pw.print(Utility.arrayToString(mtTargetSentence, ' ')); //2:"M_t"
							pw.print("\t");
							pw.print(sourcePhrase.toString(' ')); //3:"S"
							pw.print("\t");
							pw.print("\t"); //4:"MWE_S":manual
							pw.print("\t"); //5:"G_T":manual
							pw.print("\t"); //6:"E:G_T":manual
							pw.print("\t"); //7:"MWE_G_T":manual
							pw.print("\t"); //8:"M_T":manual
							pw.print("\t"); //9:"E:M_T":manual
							pw.print("\t"); //10:"MWE_M_T":manual
							pw.print(targetPhrase.toString(' ')); //11:"K_T"
							pw.print("\t"); 
							pw.print("\t"); //12:"E:K_T":manual
							//pw.print("\t"); //13:"MWE_K_T":manual
							pw.println();
						}												
					}
				}
			}
		}
		
		pp.end();
		pw.close();
		
	}
	
	public static boolean match(String[] sentence, IdentityArrayList<String> phrase, int i) {
		//if (sentence[i] != phrase[0])
		//	return false;
		int ps = phrase.size();
		if (sentence.length < (i + ps))
			return false;
		Iterator<String> phraseIter = phrase.iterator();
		phraseIter.next(); //first already checked
		for(int p=i+1; p<i+ps; p++) {
			if (sentence[p]!=phraseIter.next())
				return false;
		}
		return true;
	}

	public static void main(String[] args) throws FileNotFoundException {
		File tableFile = new File(args[0]);
		File sourceFile = new File(args[1]);
		File targetFile = new File(args[2]);
		File mtTargetFile = new File(args[3]);
		new PrepareManualValidation(tableFile, sourceFile, targetFile, mtTargetFile);

	}

}
