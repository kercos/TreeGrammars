package wordModel;

import java.util.Arrays;

public class UkWordMappingBasic extends UkWordMapping {
	
	public UkWordMappingBasic() {				
	}
	
	public String getName() {
		return "basic";
	}
	
	protected void loadDefaultParameters() {
		
		compareTrainTest = false;
				
	}
	
	
	protected void printParametersInfo() {		
		System.out.println("Unknown Word Threashold: " + ukThreashold);		
		System.out.println("\n");
	}
	
	protected void printModelStats() {
	}

	public String getFeatureOfWord(String word, boolean firstWord, int trainingDevelop) {
		return "UNKNOWN_WORD";		
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
