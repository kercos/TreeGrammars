package tesniere;

public class WordFeatures {
	
	boolean isNumber;
	boolean containsDigit, containsDot, containsHiphen;
	boolean multiWords, capitalized;
	
	public static String isNumberMatch = "[\\d\\.,\\\\/]+";
	public static String containsDigitMatch = ".*\\d.*";			
	
	public WordFeatures(String word) {
		if (isNumber(word)) {
			isNumber = true;
			return;
		}		
		containsDigit = containsDigit(word);		
		containsDot = containsDot(word);
		containsHiphen = containsHiphen(word);
		multiWords = isMultiWords(word);
		capitalized = isCapitalized(word);
	}
	
	public String toFeatureVector() {
		return
			(capitalized ? "1" : "0") +
			(multiWords ? "1" : "0") +
			(containsHiphen ? "1" : "0") +
			(containsDot ? "1" : "0") +
			(containsDigit ? "1" : "0") +
			(isNumber ? "1" : "0");			
	}
	
	public static boolean isNumber(String word) {
		return word.matches(isNumberMatch);
	}
	
	public static boolean containsDigit(String word) {
		return word.matches(containsDigitMatch);
	}
	
	public static boolean containsDot(String word) {
		return word.indexOf('.')>=0;
	}
	
	public static boolean containsHiphen(String word) {
		return word.indexOf('-')>=0;
	}
	
	public static boolean isMultiWords(String word) {
		return word.indexOf(' ')>=0;
	}
	
	public static boolean isCapitalized(String word) {
		return !Character.isLowerCase(word.charAt(0)); 
	}

	public static void main(String[] args) {
		
	}
	
}
