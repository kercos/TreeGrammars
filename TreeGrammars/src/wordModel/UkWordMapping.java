package wordModel;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationLeft_Petrov;
import util.ArgumentReader;
import util.file.FileUtil;

public abstract class UkWordMapping {
	
	public static int ukThreashold;
	public static boolean compareTrainTest = false;	
	public static boolean printParamInfo = false;
	
	ArrayList<TSNodeLabel> trainingTreebank, testTreebank;
	Hashtable<String,int[]> lexFrequency;	
	Hashtable<String,Hashtable<String, ArrayList<String>>> trainingPosWordFeatureStats;
	Hashtable<String,Hashtable<String, ArrayList<String>>> developPosWordFeaturesStats;	
	int wordTokensTraining, wordTypesTraining, wordsBelowThresholdTraining, wordsDevelop;	
	
	int totalDevelopBins, awfullyBadMarkedBins, badMarkedBins, lessMarkedBins, waveMarkedBins, nonMarkedBins;  

	public void init(ArrayList<TSNodeLabel> trainingTreebank, ArrayList<TSNodeLabel> testTreebank) {
		this.trainingTreebank = trainingTreebank;
		this.testTreebank = testTreebank;
				
		buildLexFrequency();
		
		loadDefaultParameters();
		
		if (printParamInfo) printParametersInfo();
		if (compareTrainTest) compareTrainTest();				
	}
	
	public static UkWordMapping getModel(String readStringOption) {
		if (readStringOption.equals("Basic")) {
			return new UkWordMappingBasic();
		}
		else if (readStringOption.equals("English_Fede")) {
			return new UkWordMappingFede();
		}
		else if (readStringOption.equals("French_Arun")) {
			return new UkWordMappingFrenchArun();
		}
		else if (readStringOption.equals("French_Benoit")) {
			return new UkWordMappingFrenchBenoit();
		}
		else if (readStringOption.equals("English_Petrov")) {
			UkWordMappingPetrov.unknownLevel = 5;
			return new UkWordMappingPetrov();
		}
		else if (readStringOption.startsWith("Petrov_Level_")) {
			int level = Integer.parseInt(readStringOption.substring(readStringOption.length()-1));
			UkWordMappingPetrov.unknownLevel = level;
			return new UkWordMappingPetrov();
		}
		return null;
	}

	public abstract String getName(); 
	
	public int getUkThreashold() {
		return ukThreashold;
	}
	
	protected abstract void loadDefaultParameters();
	
	protected abstract void printParametersInfo();
	
	protected void compareTrainTest() {		
		
		trainingPosWordFeatureStats = buildPosWordFeaturesStats(true);
		
		developPosWordFeaturesStats = buildPosWordFeaturesStats(false);
						
		System.out.println("TRAINING STATISTICS");
		printPosWordFeatureStatsTraining();
		
		System.out.println("\n\n_______________________________________________\n\n");
		
		System.out.println("DEVELOP STATISTICS");
		printPosWordFeatureStatsDevelop();
		
		System.out.println("\n\n_______________________________________________\n\n");
		
		printModelStats();
		
		System.out.println("\n\n_______________________________________________\n\n");
		
		System.out.println("Words token in training: " + wordTokensTraining);
		System.out.println("Words type in training: " + wordTypesTraining);
		System.out.println("Words token below threshold training: " + wordsBelowThresholdTraining);
		System.out.println("Words token unknown in develop: " + wordsDevelop);
		
		System.out.println("\n\n_______________________________________________\n\n");
		
		System.out.println("Total Developed Bins: " + totalDevelopBins);
		System.out.println("Total Bins !! : " + awfullyBadMarkedBins);
		System.out.println("Total Bins ~  : " + waveMarkedBins);
		System.out.println("Total Bins !  : " + badMarkedBins);
		System.out.println("Total Bins <  : " + lessMarkedBins);
		System.out.println("Total Bins    : " + nonMarkedBins);
		
	}

	protected abstract void printModelStats();

	public void buildLexFrequency() {
		lexFrequency = new Hashtable<String,int[]>();
		for(TSNodeLabel t : trainingTreebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			wordTokensTraining += lex.size();
			for(TSNodeLabel l : lex) {
				String word = l.label();				
				int[] count = lexFrequency.get(word);
				if (count==null) {
					count = new int[]{1};
					lexFrequency.put(word, count);
					wordTypesTraining++;
				}
				else {
					count[0]++;
				}
			}
		}
	}
	
	public ArrayList<TSNodeLabel> transformTrainingTreebank() {
		return transformTreebank(trainingTreebank, ukThreashold);
	}
	
	public ArrayList<TSNodeLabel> transformTestTreebank() {
		return transformTreebank(testTreebank, ukThreashold);
	}
	
	public void ouputTrainigTreebankWithWordFeatures(File ouputFile) {
		ouputTreebankWithWordFeatures(trainingTreebank, ouputFile, ukThreashold);
	}
	
	public void ouputTestTreebankWithWordFeatures(File ouputFile) {
		ouputTreebankWithWordFeatures(testTreebank, ouputFile, ukThreashold);
	}
	
	public void ouputTreebankWithWordFeatures(ArrayList<TSNodeLabel> treebank, File outputFile, int threashold) {
		ArrayList<TSNodeLabel> treebankTransformed = transformTreebank(treebank, threashold);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TSNodeLabel t : treebankTransformed) {
			pw.println(t.toString());
		}
		pw.close();
	}
	
	public ArrayList<TSNodeLabel> transformTreebank(ArrayList<TSNodeLabel> treebank, int threashold) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(treebank.size()); 
		for(TSNodeLabel t : treebank) {
			TSNodeLabel tClone = t.clone();
			ArrayList<TSNodeLabel> lex = tClone.collectLexicalItems();
			boolean first = true;
			for(TSNodeLabel l : lex) {
				String word = l.label();				
				int[] freqArray = lexFrequency.get(word); 
				int freq = freqArray==null ? 0 : freqArray[0];				
				if (freq<=threashold) {
					String wordFeatures = getFeatureOfWord(word, first, -1);
					l.relabel(wordFeatures);
				}
				first = false;
			}
			result.add(tClone);
		}
		return result;
	}
	

	public Hashtable<String,Hashtable<String, ArrayList<String>>> buildPosWordFeaturesStats(boolean training) {
		Hashtable<String,Hashtable<String, ArrayList<String>>> result = 
			new Hashtable<String,Hashtable<String, ArrayList<String>>>();
		ArrayList<TSNodeLabel> treebank = training ? trainingTreebank : testTreebank;
		int trainingDevelop = training ? 0 : 1;
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			boolean first = true;
			for(TSNodeLabel l : lex) {
				String word = l.label();
				String pos = l.parent.label();
				int[] freqArray = lexFrequency.get(word); 
				int freq = freqArray==null ? 0 : freqArray[0];
				if (freq<=ukThreashold) {
					if (training) wordsBelowThresholdTraining++;
					else wordsDevelop++;
					String wordFeatures = getFeatureOfWord(word, first, trainingDevelop);
					addInPosWordFeaturesStats(result, pos, wordFeatures, word);
				}
				first = false;
			}
		}
		return result;
	}	
	
	
	public static void addInPosWordFeaturesStats(Hashtable<String,Hashtable<String, ArrayList<String>>> table,
			String pos, String wordFeatures, String word) {
		Hashtable<String, ArrayList<String>> posTable = table.get(pos);
		if (posTable==null) {
			posTable = new Hashtable<String, ArrayList<String>>();			
			ArrayList<String> wordList = new ArrayList<String>();
			wordList.add(word);
			posTable.put(wordFeatures, wordList);					
			table.put(pos, posTable);
		}
		else {
			ArrayList<String> wordList = posTable.get(wordFeatures);
			if (wordList==null) {
				wordList = new ArrayList<String>();
				posTable.put(wordFeatures, wordList);
			}
			wordList.add(word);
		}
	}
	
	public abstract String getFeatureOfWord(String word, boolean firstWord, int trainingDevelop);
	
	
	public void printPosWordFeatureStatsTraining() {
		for(Entry<String, Hashtable<String, ArrayList<String>>> e : trainingPosWordFeatureStats.entrySet()) {
			String pos = e.getKey();			
			Hashtable<String, ArrayList<String>> wordFeaturesStats = e.getValue();
			for(Entry<String, ArrayList<String>> f : wordFeaturesStats.entrySet()) {
				String wordFeatures = f.getKey();
				ArrayList<String> wordList = f.getValue();
				System.out.println(pos + "\t" + wordFeatures + "\t" + wordList);
			}
		}		
	}
	
	public void printPosWordFeatureStatsDevelop() {
		for(Entry<String, Hashtable<String, ArrayList<String>>> e : developPosWordFeaturesStats.entrySet()) {
			String pos = e.getKey();			
			Hashtable<String, ArrayList<String>> wordFeaturesStats = e.getValue();
			for(Entry<String, ArrayList<String>> f : wordFeaturesStats.entrySet()) {
				String wordFeatures = f.getKey();
				ArrayList<String> wordList = f.getValue();
				String freqTraining = getFreqTraining(wordFeatures, pos);
				System.out.println(pos + "\t" + wordFeatures + "\t" + wordList + "\t" + freqTraining);
			}
		}		
	}
		
	static int minFreqRealPos = 3;	
	
	private String getFreqTraining(String wordFeatures, String realPos) {
		StringBuilder result = new StringBuilder();
		int freqRealPos = -1;
		int maxPos = -1;
		int numPosWithMax = 0;
		for(Entry<String, Hashtable<String, ArrayList<String>>> e : trainingPosWordFeatureStats.entrySet()) {
			String pos = e.getKey();			
			Hashtable<String, ArrayList<String>> wordFeaturesStats = e.getValue();	
			ArrayList<String> wordList = wordFeaturesStats.get(wordFeatures);		
			if (wordList==null) continue;
			int freq = wordList.size();
			String wordListSelection = selectSample(wordList);
			result.append("\t" + pos + " " + freq + " " + wordListSelection);
			if (pos.equals(realPos)) freqRealPos = freq;
			if (freq>maxPos) {
				maxPos = freq;
				numPosWithMax = 0;
			}
			else if (freq==maxPos) {
				numPosWithMax++;
			}
		}
		
		totalDevelopBins++;
		
		if (result.length()==0) {
			awfullyBadMarkedBins++;
			return "!!";			
		}
		
		if (freqRealPos<=0) {
			badMarkedBins++;
			result.insert(0, "!");
		}
		else if (freqRealPos!=maxPos || numPosWithMax>1) {
			lessMarkedBins++;
			result.insert(0, "<");
		}
		else if (freqRealPos < minFreqRealPos) {
			waveMarkedBins++;
			result.insert(0, "~");			
		}
		else nonMarkedBins++;
		return result.toString();
	}

	static int selectSampleNumber = 10;
	static Random rand = new Random();
	private String selectSample(ArrayList<String> wordList) {
		HashSet<String> setWordList = new HashSet<String>(wordList);
		int size = setWordList.size();
		if (size<=selectSampleNumber) return setWordList.toString();
		HashSet<String> setResult = new HashSet<String>();
		Vector<String> wordVector = new Vector<String>(setWordList);
		int added = 0;
		do {
			int i = rand.nextInt(size);
			String w = wordVector.get(i);
			setResult.add(w);
			added++;
		} while(added < selectSampleNumber);
		wordVector = new Vector<String>(setResult);
		wordVector.add("...");
		return wordVector.toString();
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String outputPathOption = "-outputPath:";
		String ukModelOption = "-ukModel:";
		String uknownThresholdOption = "-ukThreshold:";
		
		String outputPath = null;
		
		String usage = "USAGE: java [-Xmx1G] tsg.mb.MarkoBinarizeUnknownWords " +
				"-outputPath:null " +
				"[-ukModel:English_Petrov] [-ukThreshold:-1] " +
				"treebankFile testFile";
		
		UkWordMapping.ukThreashold = -1;
		UkWordMapping ukModel = new UkWordMappingPetrov();
		
		//READING PARAMETERS
		if (args.length==0 || args.length>5) {
			System.err.println("Incorrect number of arguments: " + args.length);
			System.err.println(usage);
			return;
		}
		for(int i=0; i<args.length-2; i++) {
			String option = args[i];
			if (option.startsWith(outputPathOption))
				outputPath = ArgumentReader.readStringOption(option);
			else if (option.startsWith(ukModelOption)) {
				ukModel = UkWordMapping.getModel(ArgumentReader.readStringOption(option));
				if (ukModel==null) {
					System.err.println("Invalid ukModel");
					return;
				}
			}
			else if (option.startsWith(uknownThresholdOption))
				UkWordMapping.ukThreashold = ArgumentReader.readIntOption(option);			
			else {
				System.err.println("Not a valid option: " + option);
				System.err.println(usage);
				return;
			}		
		}
		
		if (outputPath==null) {
			System.err.println("OutputPath not specified.");
			return;
		}
		
		File dir = new File(outputPath);
		if (dir.exists()) {
			String dataFolder = "/UK_" + FileUtil.dateTimeStringUnique() + "/";
			outputPath = dir + dataFolder;
		}
		else
			outputPath = dir + "/";
		new File(outputPath).mkdirs();
		Parameters.openLogFile(new File(outputPath + "UK.log"));
		
		File trainingTB_File = new File(args[args.length-2]);
		if (!trainingTB_File.exists() || !trainingTB_File.canRead()) {
			System.err.println("Training file doesn't exist or not accessible: " + args[args.length-2]);
			return;
		}
		
		File testTB_File = new File(args[args.length-1]);
		if (!testTB_File.exists() || !testTB_File.canRead()) {
			System.err.println("Test file doesn't exist or not accessible: " + args[args.length-1]);
			return;
		}		
		
		ArrayList<TSNodeLabel> trainingTB = null;
		ArrayList<TSNodeLabel> testTB = null;
		
		try {						
			trainingTB = Wsj.getTreebank(trainingTB_File);
			testTB = Wsj.getTreebank(testTB_File);
		} catch (Exception e) {			
			e.printStackTrace();
			System.err.println("Something wrong happened while reading the treebank in file: " + trainingTB_File);
			System.exit(-1);	
		}
		
		Parameters.reportLineFlush("args:\t" + Arrays.toString(args));
		
		Parameters.reportLineFlush("Output path:\t" + outputPath);
		Parameters.reportLineFlush("Training TB file:\t" + trainingTB_File);
		Parameters.reportLineFlush("Training TB size:\t" + trainingTB.size());
		Parameters.reportLineFlush("Test TB file:\t" + testTB_File);
		Parameters.reportLineFlush("Test TB size:\t" + testTB.size());
		
		Parameters.reportLine("UkModel: " + ukModel.getName());
		Parameters.reportLineFlush("Uknown Word Threshold (less or equal):\t" + UkWordMapping.ukThreashold);
		
		ArrayList<TSNodeLabel> trainingTB_UK = null;			
		ArrayList<TSNodeLabel> testTB_UK = null;
		
		if (UkWordMapping.ukThreashold>0) {
			Parameters.reportLineFlush("Processing Unknown Words...");
			ukModel.init(trainingTB, testTB);
			trainingTB_UK = ukModel.transformTrainingTreebank();
			testTB_UK = ukModel.transformTestTreebank();
		}
		
		File transformedTrainingTB_File = new File(outputPath + trainingTB_File.getName());
		File transformedTestTB_File = new File(outputPath + testTB_File.getName());
		TSNodeLabel.printTreebankToFile(transformedTrainingTB_File, trainingTB_UK, false, false);			
		TSNodeLabel.printTreebankToFile(transformedTestTB_File, testTB_UK, false, false);
		Parameters.reportLineFlush("Written trainig TB with unknown words to: " + transformedTrainingTB_File);
		Parameters.reportLineFlush("Written test TB with unknown words to: " + transformedTestTB_File);
		
		
		
	}

	

}
