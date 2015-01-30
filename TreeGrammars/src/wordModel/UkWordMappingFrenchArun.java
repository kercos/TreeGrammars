package wordModel;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.TreeSet;

import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.Utility;
import util.file.FileUtil;

public class UkWordMappingFrenchArun extends UkWordMapping {
	
	final static String isNumericalMatch = "[\\d,\\-,\\,,\\.,_]+";
	final static String containsDigitMatch = ".*\\d.*";
	final static String containsAlphaMatch = ".*[a-zA-Z].*";
		
	String[] derivations, inflections, prepositions;
	
	/*
	boolean useFirstWord, useFirstCap, useAllCap;
	boolean useDash, useForwardSlash, useDigit, useAlpha, useDollar;	
	boolean allowToCombineSuffixAffix;
	boolean useASFixWithDash, useASFixWithSlash, useASFixWithCapital;
	*/
	
	int[][] affixesStats, suffixesStats;

	public UkWordMappingFrenchArun() {				
	}
	
	public String getName() {
		return "French_Arun";
	}
	
	protected void loadDefaultParameters() {
		
		/*
		useFirstWord = true;
		useFirstCap = true;
		useAllCap = false;
		useDash = true; 
		useForwardSlash = false; 
		useDigit = true;
		useAlpha = false;
		useDollar = false;
		allowToCombineSuffixAffix = false;
		useASFixWithDash = false;
		useASFixWithSlash = false;
		useASFixWithCapital = false;
		*/
		
		compareTrainTest = false;
		
		String derivationalAll = "âtre aphe aphie ment aire if ien age al ale er ère ique tion able aux " +
				"enne ive eur ois oise eux";		
		
		String inflectionsAll = "issons issez issent isse isses issions issiez issant issais issait " +
				"issaient îmes îtes irent irai iras irons iront irez irais irait irions iriez " +
				"iraient erai eras erons erez eront erais erait erions eriez eraient ions iez " +
				"ant ais ait aient as âmes âtes èrent ons ez ent es ées";
		
		String prepositionsAll = "À à de du des d’ au aux Au Aux A De Des D’ Du";
		
		
		derivations = derivationalAll.split("\\s");
		inflections = inflectionsAll.split("\\s");
		prepositions = prepositionsAll.split("\\s");
		Arrays.sort(prepositions);
		
		if (derivations!=null) affixesStats = new int[2][derivations.length];
		if (inflections!=null) suffixesStats = new int[2][inflections.length];
				
	}
	
	public boolean isPreposition(String word) {
		return Arrays.binarySearch(prepositions, word)>=0;
	}
	
	public int endsInDerivation(String word) {
		int index = 0;
		for(String a : derivations) {
			index++;
			if (word.endsWith(a)) {				
				return index;
			}			
		}
		return -1;
	}
	
	public int endsInInflection(String word) {
		int index = 0;
		for(String a : inflections) {
			index++;
			if (word.endsWith(a)) {				
				return index;
			}			
		}
		return -1;
	}
	
	protected void printParametersInfo() {
		
		System.out.println("Unknown Word Threashold: " + ukThreashold);
				
		System.out.println("Total Affixes: " + (derivations==null ? 0 : derivations.length));
		
		System.out.println("Total Affixes: " + (derivations==null ? 0 : derivations.length));
		System.out.println(Arrays.toString(derivations));
		
		System.out.println("\n");
		
		System.out.println("Total Suffixes: " + (inflections==null ? 0 : inflections.length));
		System.out.println(Arrays.toString(inflections));
		
		System.out.println("\n");
	}
	
	protected void printModelStats() {
		if (derivations!=null) {
			System.out.println("AFFIXES:");
			for(int i=0; i<derivations.length; i++) {
				System.out.println(derivations[i] + "\t" + affixesStats[0][i] + "\t" + affixesStats[1][i]);
			}
		}
		if (inflections!=null) {
			System.out.println("-----------------------------");
			System.out.println("SUFFIXES:");
			for(int i=0; i<inflections.length; i++) {
				System.out.println(inflections[i] + "\t" + suffixesStats[0][i] + "\t" + suffixesStats[1][i]);
			}
		}		
	}
	
	public String getFeatureOfWord(String word, boolean firstWord, int trainingDevelop) {
		
		String feature1 = getFeature1(word);
		String feature2 = getFeature2(word, firstWord);
		String feature3 = getFeature3(word);
		String feature4 = getFeature4(word);
		String feature5 = getFeature5(word);
		String feature6 = getFeature6(word, feature5);
		
		return feature1 + "_" + feature2 + "_" + feature3 + 
			"_" + feature4 + "_" + feature5 + "_" + feature6; 		
	}
	
	// numerical
	private String getFeature1(String word) {
		if (word.matches(isNumericalMatch)) return "N1";
		return "N0";
	}
	
	// capitalization
	private String getFeature2(String word, boolean firstWord) {
		char firstChar = word.charAt(0);
		boolean firstCapital = Character.isUpperCase(firstChar);
		
		if (firstCapital) {
			if (firstWord) return "C1";
			if (allCapitals(word)) {
				if (word.matches(containsDigitMatch)) return "C2";
				else return "C3";
			}						
			return "C4";			
		}
		return "C0";	
	}
	
	// hypernations, symbols
	private String getFeature3(String word) {
		boolean hasDash = word.indexOf('-')!=-1;		
		if (hasDash) return "H1";
		boolean hasApostrophe = word.indexOf('\'')!=-1;
		if (hasApostrophe)	return "H2";
		boolean hasDollar = word.indexOf('$')!=-1;
		if (hasDollar) return "H3";
		return "H0";
	}
	
	// prepositions
	private String getFeature4(String word) {
		boolean hasUnderscore = word.indexOf('_')!=-1;
		if (hasUnderscore) {
			String[] parts = word.split("_");
			int lastPartIndex = parts.length-1;
			boolean firstIsPreposition = isPreposition(parts[0]);
			boolean lastIsPreposition = isPreposition(parts[lastPartIndex]);
			if (firstIsPreposition) {
				if (lastIsPreposition) return "P2";
				return "P1";
			}
			if (lastIsPreposition) return "P3";
			return "P4";
		}
		return "P0";
	}
	
	// derivational feature
	private String getFeature5(String word) {
		int derivationIndex = endsInDerivation(word);
		if (derivationIndex!=-1) {
			return "D" + derivationIndex;
		}
		return "D0";
	}
	
	// inflectional feature
	private String getFeature6(String word, String feature5) {
		if (!feature5.equals("D0") && word.length()>3) {
			int inflectionIndex = endsInInflection(word);
			if (inflectionIndex!=-1) {
				return "I" + inflectionIndex;
			}
		}
		return "I0";
	}
	
	public static boolean allCapitals(String w) {
		char[] charArray = w.toCharArray();
		for(char c : charArray) {
			if (Character.isLowerCase(c)) return false;
		}
		return true;
	}
	
	
	
	public static void getLexicon() throws Exception {
		UkWordMappingFrenchArun ukModel = new UkWordMappingFrenchArun();
		ukModel.loadDefaultParameters();
		File FTBtrain = new File("/scratch/fsangati/CORPUS/FrenchTreebank/ftbuc+lexeme-only/ftb_1_cleaned.mrg");
		ArrayList<TSNodeLabel> treebank = Wsj.getTreebank(FTBtrain);		
		Hashtable<String, int[]> lexicon = new Hashtable<String, int[]>(); 
		for(TSNodeLabel t : treebank) {			
			ArrayList<String> words = t.collectLexicalWords();
			for(String w : words) {
				Utility.increaseInTableInt(lexicon, w);
			}			
		}
		File lexFile = new File("FrenchTB/lex.txt");
		PrintWriter pw = FileUtil.getPrintWriter(lexFile);
		for(Entry<String,int[]> e : lexicon.entrySet()) {
			String word = e.getKey();
			pw.println(word + "\t" + e.getValue()[0] + "\t" + ukModel.getFeatureOfWord(word, false, 0));
		}
		pw.close();
	}

	/**
	 * @param args
	 * @throws Exception 
	 */

	public static void main(String[] args) throws Exception {
		
		//String a = "32_432_dfsa";
		//System.out.println(Arrays.toString(a.split("_")));
		
		
		getLexicon();
		
		/*
		threasholdTraining = 5;
		threasholdTesting = 5;
		
		loadStandardParameters();
		File trainingSet = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-02-21.mrg");
		
		File developSet1 = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-22.mrg");
		File developSet2 = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-23.mrg");
		File developSet3 = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-24.mrg");
		
		ArrayList<TSNodeLabel> trainingTreebank = TSNodeLabel.getTreebank(trainingSet);
		ArrayList<TSNodeLabel>  developTreebank = new ArrayList<TSNodeLabel>();
		//developTreebank.addAll(TSNodeLabel.getTreebank(developSet1));
		//developTreebank.addAll(TSNodeLabel.getTreebank(developSet2));
		developTreebank.addAll(TSNodeLabel.getTreebank(developSet3));
		
		compareTrainTest = true;
		threasholdTraining = 1;
		threasholdTesting = 1;
		
		new UkWordMappingStd(trainingTreebank, developTreebank);		
		*/
	}
	

}
