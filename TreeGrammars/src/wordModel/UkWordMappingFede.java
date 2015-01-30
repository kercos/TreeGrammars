package wordModel;

import java.util.Arrays;

public class UkWordMappingFede extends UkWordMapping {
	
	final static String containsDigitMatch = ".*\\d.*";
	final static String containsAlphaMatch = ".*[a-zA-Z].*";
		
	boolean useFirstWord, useFirstCap, useAllCap;
	boolean useDash, useForwardSlash, useDigit, useAlpha, useDollar;
	String[] affixes, suffixes;
	boolean allowToCombineSuffixAffix;
	boolean useASFixWithDash, useASFixWithSlash, useASFixWithCapital;
	
	int[][] affixesStats, suffixesStats;

	public UkWordMappingFede() {				
	}
	
	public String getName() {
		return "English_Fede";
	}
	
	protected void loadDefaultParameters() {
		
		useFirstWord = true;
		useFirstCap = true;
		useAllCap = false;
		useDash = true; 
		useForwardSlash = true; 
		useDigit = true;
		useAlpha = true;
		useDollar = false;
		allowToCombineSuffixAffix = false;
		useASFixWithDash = false;
		useASFixWithSlash = false;
		useASFixWithCapital = false;
		
		compareTrainTest = false;
		
		//String affixesAll = "inter trans under over non com con dis pre pro co de in re un";
		String suffixesAll = "ments ance dent ence ists line ment ship time ans ant are " +
				"ate ble cal ess est ful ian ics ing ion ist ive man ons ory ous son tor " +
				"ure al ce ck cy de ds ed er es et ey fy gs gy ic is ks ld le ls ly ne rd " +
				"rs se sh sm th ts ty ze s y";
		
		affixes = null;
		//affixes = affixesAll.split("\\s");
		suffixes = suffixesAll.split("\\s");
		
		if (affixes!=null) affixesStats = new int[2][affixes.length];
		if (suffixes!=null) suffixesStats = new int[2][suffixes.length];
				
	}
	
	
	protected void printParametersInfo() {
		
		System.out.println("Unknown Word Threashold: " + ukThreashold);
				
		System.out.println("Total Affixes: " + (affixes==null ? 0 : affixes.length));
		
		System.out.println("Total Affixes: " + (affixes==null ? 0 : affixes.length));
		System.out.println(Arrays.toString(affixes));
		
		System.out.println("\n");
		
		System.out.println("Total Suffixes: " + (suffixes==null ? 0 : suffixes.length));
		System.out.println(Arrays.toString(suffixes));
		
		System.out.println("\n");
	}
	
	protected void printModelStats() {
		if (affixes!=null) {
			System.out.println("AFFIXES:");
			for(int i=0; i<affixes.length; i++) {
				System.out.println(affixes[i] + "\t" + affixesStats[0][i] + "\t" + affixesStats[1][i]);
			}
		}
		if (suffixes!=null) {
			System.out.println("-----------------------------");
			System.out.println("SUFFIXES:");
			for(int i=0; i<suffixes.length; i++) {
				System.out.println(suffixes[i] + "\t" + suffixesStats[0][i] + "\t" + suffixesStats[1][i]);
			}
		}		
	}

	public String getFeatureOfWord(String word, boolean firstWord, int trainingDevelop) {
		StringBuilder result = new StringBuilder();		
		if (useFirstWord) {
			result.append(firstWord ? "_1stY" : "_1stN");
		}
		boolean firstCapital = false;
		if (useFirstCap) {
			char firstChar = word.charAt(0);
			firstCapital = Character.isUpperCase(firstChar);
			result.append(firstCapital ? "_1capY" : "_1capN");
		}
		boolean allCapital = false;
		if (useAllCap) {			
			allCapital = allCapitals(word);
			result.append(allCapital ? "_AcapY" : "_AcapN");
		}
		boolean hasDash = false;
		if (useDash) {
			hasDash = word.indexOf('-')!=-1;
			result.append(hasDash ? "_dashY" : "_dashN");
		}	
		boolean hasForwardSlash = false;
		if (useForwardSlash) {
			hasForwardSlash = word.indexOf('/')!=-1;
			result.append(hasForwardSlash ? "_slshY" : "_slshN");
		}
		if (useDollar) {
			boolean hasDollar = word.indexOf('$')!=-1;
			result.append(hasDollar ? "_$Y" : "_$N");
		}
		if (useAlpha) {
			boolean hasDigit = word.matches(containsAlphaMatch);			
			result.append(hasDigit ? "_alfY" : "_alfN");
		}
		if (useDigit) {
			boolean hasDigit = word.matches(containsDigitMatch);			
			result.append(hasDigit ? "_digY" : "_digN");
		}
		
		if ( (useASFixWithDash || !hasDash) &&
			 (useASFixWithSlash || !hasForwardSlash) && 	
		     (useASFixWithCapital || (!firstCapital && !allCapital)) ){			
			String wordLower = word.toLowerCase();
			boolean foundSuffix = false;
			if (suffixes!=null) {
				String suff = getSuffix(wordLower, trainingDevelop);
				if (suff==null) result.append("_sfx:NONE");
				else {
					result.append("_sfx:" + suff);
					foundSuffix = true;
				}
			}
			if (affixes!=null) {
				if (!allowToCombineSuffixAffix && foundSuffix) {
					result.append("_afx:NONE");
				}
				else {
					String aff = getAffix(wordLower, trainingDevelop);
					result.append(aff==null ? "_afx:NONE" : ("_afx:" + aff));
				}
			}
		}
		else {
			result.append("_sfx:NONE");
			result.append("_afx:NONE");
		}
		return result.substring(1);		
	}
	
	public static boolean allCapitals(String w) {
		char[] charArray = w.toCharArray();
		for(char c : charArray) {
			if (Character.isLowerCase(c)) return false;
		}
		return true;
	}
	
	public String getAffix(String wordLower, int trainingDevelop) {
		int index = 0;
		for(String a : affixes) {
			if (wordLower.startsWith(a)) {
				if (trainingDevelop>=0) affixesStats[trainingDevelop][index]++;
				return a;
			}
			index++;
		}
		return null;
	}
	
	public String getSuffix(String wordLower, int trainingDevelop) {
		int index = 0;
		for(String a : suffixes) {
			if (wordLower.endsWith(a)) {
				if (trainingDevelop>=0) suffixesStats[trainingDevelop][index]++;
				return a;
			}
			index++;
		}
		return null;
	}
	
		

	/**
	 * @param args
	 * @throws Exception 
	 */
	/*
	public static void main(String[] args) throws Exception {
		
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
		
	}
	*/

}
