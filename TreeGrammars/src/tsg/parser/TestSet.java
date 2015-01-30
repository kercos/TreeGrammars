package tsg.parser;
import tsg.corpora.*;
import util.*;
import util.file.FileUtil;

import java.util.*;
import java.io.*;

public class TestSet {
	
	Grammar grammar;
	ArrayList<ArrayList<Integer>> inputSentences;
	Hashtable<ArrayList<Integer>, Integer> subStrings; 
	static final int minSubStringLengthToCache = 1;
	
	public TestSet(List<String> inputSentences, Grammar structure) {
		this.grammar = structure;
		convertInputSentences(inputSentences);
		buildSubStringStructure();
		removeUniqueSubStrings();
		//printStructure();
	}
	
	public boolean containsSubString(ArrayList<Integer> subString) {
		return this.subStrings.containsKey(subString);
	}
	
	private void convertInputSentences(List<String> sentences) {
		subStrings = new Hashtable<ArrayList<Integer>, Integer>();
		this.inputSentences = new ArrayList<ArrayList<Integer>>(sentences.size());
		for(String sentence : sentences) {
			String[] sentenceWords = sentence.split("\\s+");
			ArrayList<Integer> sentenceIndexes = new ArrayList<Integer>(sentenceWords.length);
			this.inputSentences.add(sentenceIndexes);
			for(int i=0; i<sentenceWords.length; i++) {
				sentenceIndexes.add(grammar.getIndexOfWord(sentenceWords[i]));
			}
		}	
	}
	
	private void buildSubStringStructure() {
		for(ListIterator<ArrayList<Integer>> s = this.inputSentences.listIterator(); s.hasNext(); ) {
			ArrayList<Integer> sentenceIndexes = s.next();
			int sentenceLength = sentenceIndexes.size();
			for(int span=minSubStringLengthToCache; span<=sentenceLength; span++) {				
				for(int start=0; start<=sentenceLength-span; start++) {
					ArrayList<Integer> fragment = new ArrayList<Integer>(sentenceIndexes.subList(start, start+span));
					//fragment.trimToSize();
					Utility.increaseIntegerListInteger(subStrings, fragment, 1);
				}
			}
		}
	}
	
	/**
	 * remove all the keys with freq=1
	 */
	private void removeUniqueSubStrings() {
		for(Enumeration<ArrayList<Integer>> e = subStrings.keys(); e.hasMoreElements(); ) {
			ArrayList<Integer> key = e.nextElement();
			Integer value = subStrings.get(key);
			if (value.equals(1)) subStrings.remove(key);
		}
	}
	
	private void printStructure() {
		File completeOutput = new File("/home/fsangati/Desktop/completeStat");
		File compressOutput = new File("/home/fsangati/Desktop/compressStat");				
		TreeSet<Integer> orderedFreq = new TreeSet<Integer>(subStrings.values());
		for(Iterator<Integer> i = orderedFreq.iterator(); i.hasNext();) {			
			Integer freq = i.next();
			FileUtil.appendReturn(""+freq, completeOutput);
			FileUtil.appendReturn(""+freq, compressOutput);
			int[] lengthStat = new int[40];
			Arrays.fill(lengthStat, 0);
			for(Enumeration<ArrayList<Integer>> e = subStrings.keys(); e.hasMoreElements(); ) {
				ArrayList<Integer> key = e.nextElement();				
				Integer value = subStrings.get(key);
				if (value.equals(freq)) {
					lengthStat[key.size()]++;
					String fragment = "";
					for(ListIterator<Integer> l = key.listIterator(); l.hasNext(); ) {
						int index = l.next();
						if (index==-1) fragment += ConstCorpus.unknownTag + " ";
						else fragment += grammar.lexArray[index] + " ";						
					}
					FileUtil.appendReturn("\t" + fragment, completeOutput);
				}
			}
			for(int j=0; j<lengthStat.length; j++) {
				if (lengthStat[j]>0) {
					FileUtil.appendReturn("\t" + j + "\t" + lengthStat[j], compressOutput);
				}				
			}	
		}						
	}
	
	public static void main(String[] args) {
		int[] array1 = new int[]{1,2,3}; 
		int[] array2 = new int[]{1,2,3};
		System.out.println(array1.equals(array2));
	}
	
}
