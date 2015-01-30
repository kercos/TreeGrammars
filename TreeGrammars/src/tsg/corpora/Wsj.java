package tsg.corpora;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;

import settings.Parameters;
import tesniere.Word;
import tsg.*;
import util.*;
import util.file.FileUtil;

public abstract class Wsj extends ConstCorpus {
	
	private static final long serialVersionUID = 0L;
	
	public static String testSet; // 00 | 01 | 22 | 23
	public static boolean skip120TrainingSentences;
	public static boolean transformNPbasal;
	public static boolean transformSG;
	public static int initialHeads; //0 1 2 3 4 5 6 7
	public static String WsjBase = Parameters.corpusPath + "WSJ/";
	public static String WsjConstBase = WsjBase + "CONSTITUENCY/";
	public static String WsjOriginal = WsjConstBase + "ORIGINAL/";
	public static String WsjOriginalReadable = WsjConstBase + "ORIGINAL_READABLE/";	
	public static String WsjOriginalReadableAuxify = WsjConstBase + "ORIGINAL_READABLE_AUXIFY/";
	public static String WsjOriginalCleaned = WsjConstBase + "ORIGINAL_READABLE_CLEANED/";
	public static String WsjOriginalCleanedTop= WsjConstBase + "ORIGINAL_READABLE_CLEANED_TOP/";
	public static String WsjOriginalCleanedSemTagsOff = WsjConstBase + "ORIGINAL_READABLE_CLEANED_SEMTAGSOFF/";
	public static String WsjOriginalCleanedTopSemTagsOff = WsjConstBase + "ORIGINAL_READABLE_CLEANED_TOP_SEMTAGSOFF/";
	public static String WsjOriginalCleanedTopSemTagsOffCharBased = WsjConstBase + "ORIGINAL_READABLE_CLEANED_TOP_SEMTAGSOFF_CHARBASED/";
	public static String WsjOriginalCleanedSemTagsOffAuxify = WsjConstBase + "ORIGINAL_READABLE_CLEANED_SEMTAGSOFF_AUXIFY/";	
	public static String WsjOriginalCleanedCollins97 = WsjConstBase + "ORIGINAL_READABLE_CLEANED_COLLINS_97/";
	public static String WsjOriginalCleanedCollins99 = WsjConstBase + "ORIGINAL_READABLE_CLEANED_COLLINS_99/";
	public static String WsjOriginalCleanedMagerman = WsjConstBase + "ORIGINAL_READABLE_CLEANED_MAGERMAN/";
	public static String WsjOriginalCleanedLeft = WsjConstBase + "ORIGINAL_READABLE_CLEANED_LEFT/";
	public static String WsjOriginalCleanedRight = WsjConstBase + "ORIGINAL_READABLE_CLEANED_RIGHT/";
	public static String WsjOriginalCleanedRandom = WsjConstBase + "ORIGINAL_READABLE_CLEANED_RANDOM/";
	public static String WsjOriginalCleanedYM = WsjConstBase + "ORIGINAL_READABLE_CLEANED_YM/";
	public static String WsjOriginalCollins97 = WsjConstBase + "ORIGINAL_READABLE_COLLINS_97/";
	public static String WsjOriginalCollins99 = WsjConstBase + "ORIGINAL_READABLE_COLLINS_99/";
	public static String WsjOriginalMagerman = WsjConstBase + "ORIGINAL_READABLE_MAGERMAN/";
	public static String WsjOriginalNoTraces = WsjConstBase + "ORIGINAL_READABLE_NOTRACES/";
	public static String WsjOriginalNPBraketing = WsjConstBase + "ORIGINAL_READABLE_NP_BRACKETING/";
	public static String WsjOriginalNPBraketingCleaned = WsjConstBase + "ORIGINAL_READABLE_NP_BRACKETING_CLEANED/";
	public static String WsjOriginalNPBraketingCleanedFixCC = WsjConstBase + "ORIGINAL_READABLE_NP_BRACKETING_CLEANED_FIXCC/";
	public static String WsjOriginalNPBraketingCleanedConll07 = WsjConstBase + "ORIGINAL_READABLE_NP_BRACKETING_CLEANED_CONLL07/";
	public static String Wsj10 = WsjConstBase + "WSJ10/";
	
	public static String WsjFlatBase = WsjBase + "FLAT/";
	public static String WsjFlatTraces = WsjFlatBase + "FLAT_TRACES/";
	public static String WsjFlatNoTraces = WsjFlatBase + "FLAT_NOTRACES/";
	public static String WsjFlatNoTracesCharniak = WsjFlatBase + "FLAT_NOTRACES_Charniak/";
	
	public static String WsjOriginalBikelPreprocessed = WsjConstBase + "BikelPreProcessed/";
	public static final String[] initialHeadLabels = new String[]{"No Heads", "Magerman", 
		"Collins 97", "Collins 99", "YM", "FirstLeft", "FirstRight", "Random"};
	public static String[] possibleInitialHeadPath = new String[]{WsjOriginalCleaned, WsjOriginalCleanedMagerman,
		WsjOriginalCleanedCollins97, WsjOriginalCleanedCollins99, WsjOriginalCleanedYM, 
		WsjOriginalCleanedLeft, WsjOriginalCleanedRight, WsjOriginalCleanedRandom};	
	
	
	public static String WsjBinary = WsjConstBase + "BINARY/";	
	public static String WsjTrainingSecFile = "wsj-02-21";
	
	public static String traceTag = "-NONE-";
	public static String NPbasalTag = "NPB";
	public static String SubjectLessTag = "SG";
	public static String[] nonCountCatInLength = new String[]{"#", "$", "''", ",", "-LCB-", "-LRB-", "-NONE-", "-RCB-", "-RRB-", ".", ":", "``"}; //new String[]{traceTag} 
	
	public static final String[] nonNecessaryLabels = {"." , "''" , "``"};
	public static final String[] semTagNonComplement = 
		{"ADV", "VOC", "BNF", "DIR", "EXT", "LOC", "MNR", "TMP", "CLR", "PRP"};
	public static final String[] complementParentCat = new String[]{"S","VP","SBAR"};
	public static final String[][] complementChildCat = new String[][]{
		{"NP", "SBAR", "S"},{"NP","SBAR","S","VP"},{"S"}
	};
	
	  /** The sentence numbers of sentences that Mike Collins' trainer skips,
    due to a strange historical reason of a pre-processing Perl script
    of his. 1-base index */
	public final static int[] collinsSkipArr =
	  {167, 557, 581, 687, 698, 863, 914, 1358, 1406, 1869, 1873, 1884, 1887,
	   2617, 2700, 2957, 3241, 3939, 3946, 3959, 4613, 4645, 4669, 5021, 5312,
	   5401, 6151, 6161, 6165, 6173, 6340, 6342, 6347, 6432, 6704, 6850, 7162,
	   7381, 7778, 7941, 8053, 8076, 8229, 10110, 10525, 10676, 11361, 11593,
	   11716, 11727, 11737, 12286, 12871, 12902, 13182, 13409, 13426, 13868,
	   13909, 13918, 14252, 14255, 16488, 16489, 16822, 17112, 17566, 17644,
	   18414, 19663, 20105, 20213, 20308, 20653, 22565, 23053, 23226, 23483,
	   24856, 24928, 24930, 25179, 25193, 25200, 26821, 26967, 27051, 27862,
	   28081, 28680, 28827, 29254, 29261, 29348, 30110, 30142, 31287, 31739,
	   31940, 32001, 32010, 32015, 32378, 34173, 34544, 34545, 34573, 35105,
	   35247, 35390, 35865, 35868, 36281, 37653, 38403, 38545, 39182, 39197,
	   39538, 39695};
	
	
	/**
	 * converts the original WSJ in single line sentences without extra parenthesis
	 */
	public static void makeReadable(File startDir, File outputDir) {
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;						
			File newFile = new File(outputDir + "/" + inputFile.getName());
			makeReadableFile(inputFile, newFile);
		}
	}
	
	public static void addTop(File startDir, File outputDir) {
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;						
			File newFile = new File(outputDir + "/" + inputFile.getName());
			ConstCorpus.addTop(inputFile, newFile);
		}
	}
	
	public static void makeReadableFile(File inputFile, File newFile) {
		Scanner reader = FileUtil.getScanner(inputFile);
		PrintWriter writer = FileUtil.getPrintWriter(newFile);
		int parenthesis = 0;
		String sentence = "";
		int lineNumber = 0;
		while(reader.hasNextLine()) {
			lineNumber++;
			String line = reader.nextLine();
			if (line.length()==0) continue; 
			/*if (line.charAt(0)=='*') {
				if (sentence.length()>0) {
					System.out.println(lineNumber);
				}
				continue;
			}*/
			parenthesis += Utility.countParenthesis(line);
			sentence += line;
			if (parenthesis==0) {
				if (line.length()==0) continue;
				sentence = sentence.trim();								
				sentence = sentence.replaceAll("\n", "");
				sentence = sentence.replaceAll("\\s+", " ");
				writer.println(ConstCorpus.adjustParenthesisation(sentence));
				//writer.println(sentence);
				sentence = "";
			}				
		}
		reader.close();
		writer.close();
	}
	
	public static ArrayList<String> makeReadableFileToArray(File inputFile) {
		ArrayList<String> result = new ArrayList<String>();
		//Scanner reader = FileUtil.getScanner(inputFile, "ISO-8859-1");
		Scanner reader = FileUtil.getScanner(inputFile);
		int parenthesis = 0;
		String sentence = "";
		int lineNumber = 0;
		while(reader.hasNextLine()) {
			lineNumber++;
			String line = reader.nextLine();
			if (line.length()==0) continue;
			if (line.indexOf('(')==-1 && line.indexOf(')')==-1) continue;
			int parethesisInLine = Utility.countParenthesis(line);			
			parenthesis += parethesisInLine;
			sentence += line;
			if (parenthesis==0) {
				if (line.length()==0) continue;
				sentence = sentence.trim();				
				sentence = sentence.replaceAll("\\\\", "");
				sentence = sentence.replaceAll("\n", "");
				sentence = sentence.replaceAll("\\s+", " ");
				sentence = ConstCorpus.adjustParenthesisation(sentence);
				result.add(sentence);
				sentence = "";
			}				
		}
		reader.close();
		return result;
	}
	
	private static void makeOriginalNoTraces() {
		File startDir = new File(WsjOriginalReadable);
		File outputDir = new File(WsjOriginalNoTraces);
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;
			File newFile = new File(outputDir + "/" + inputFile.getName());
			Scanner reader = FileUtil.getScanner(inputFile);
			PrintWriter writer = FileUtil.getPrintWriter(newFile);
			while(reader.hasNextLine()) {
				String line = reader.nextLine();
				if (line.length()==0) continue;
				TSNode treeLine = new TSNode(line);				
				treeLine.pruneSubTrees(traceTag);	
				treeLine.removeNumberInLabels();
				writer.println(treeLine);
			}
			reader.close();
			writer.close();
		}
	}
	
	private static void finalCleaning(ConstCorpus corpus, Hashtable<String,Integer> lexFreq, 
			boolean training, boolean traces) {		
		for(TSNode treeLine : corpus.treeBank) {
			if (Parameters.removeNonNecessaryLables) {		
				treeLine.pruneSubTrees(nonNecessaryLabels); // remove trees rooted on |``|''|.|
			}		
			if (!traces) {
				treeLine.pruneSubTrees(traceTag);
				treeLine.removeNumberInLabels();
			}		
			if (Parameters.removePunctStartEnd) {
				treeLine.prunePunctuationBeginning(); // remove punctuation at the beginning of the tree
				treeLine.prunePunctuationEnd(); // remove punctuation at the end of the tree			
			}		
			if (Parameters.raisePunctuation) treeLine.raisePunctuation(); // raise punctuation at the edge of constituencies;
			if (!Parameters.semanticTags) treeLine.removeSemanticTags();				
			if (training && transformNPbasal) treeLine.transformNodebasal("NP","NPB");			
			if (training && transformSG) treeLine.transformSubjectlessSentences(SubjectLessTag);	
			if (Parameters.removeRedundantRules) treeLine.removeRedundantRules(); // S --> S
			if (Parameters.replaceNumbers) treeLine.replaceNumbers(numberTag);		
			if (Parameters.ukLimit > 0 ) treeLine.updateUnknown(Parameters.ukLimit, lexFreq, unknownTag, numberTag);							
		}
	}
	
	private static void makeFlats() {
		File startDir = new File(WsjOriginalReadable);
		File outputDirTraces = new File(WsjFlatTraces);
		File outputDirNoTraces = new File(WsjFlatNoTraces);
		outputDirTraces.mkdirs();
		outputDirNoTraces.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;
			File newFileTraces = new File(outputDirTraces + "/" + inputFile.getName());
			File newFileNoTraces = new File(outputDirNoTraces + "/" + inputFile.getName());
			Scanner reader = FileUtil.getScanner(inputFile);
			PrintWriter writerTraces = FileUtil.getPrintWriter(newFileTraces);
			PrintWriter writerNoTraces = FileUtil.getPrintWriter(newFileNoTraces);
			while(reader.hasNextLine()) {
				String line = reader.nextLine();
				if (line.length()==0) continue;
				TSNode treeLine = new TSNode(line);
				writerTraces.println(treeLine.toFlat());
				treeLine.pruneSubTrees(traceTag);	
				writerNoTraces.println(treeLine.toFlat());
			}
			reader.close();
			writerTraces.close();
			writerNoTraces.close();
		}
	}
	
	private static void makeFlatFile(File inputFile, File outputFile) throws Exception {
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TSNodeLabel t : treebank) {
			String flat = t.toFlatSentence();
			pw.println(flat);			
		}
		pw.close();
	}
	
	private static void makeSingleLabelX(File inputFile, File outputFile) throws Exception {
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		Label labelX = Label.getLabel("X");
		for(TSNodeLabel t : treebank) {
			t.renameAllConstituentLabels(labelX);
			pw.println(t.toString());			
		}
		pw.close();
	}
	
	private static void countCC() throws Exception  {
		File startDir = new File(Wsj.WsjOriginalNPBraketingCleaned);
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);
		int sentenceCounter=0;
		int coordSentenceCouter=0;
		int coordCounter = 0;	
		for(File inputFile : fileList) {
			if (inputFile.isDirectory() || !inputFile.getName().startsWith("wsj")) continue;			
			Scanner reader = FileUtil.getScanner(inputFile);		
			while(reader.hasNextLine()) {
				String line = reader.nextLine();
				if (line.length()==0) continue;
				TSNodeLabel treeLine = new TSNodeLabel(line);
				int CCcount = treeLine.countDaughtersWithLabel(Word.conjunctionsSorted);
				sentenceCounter++;
				if (CCcount>0) {
					coordCounter += CCcount;
					coordSentenceCouter++;
				}
			}
			reader.close();		
		}
		System.out.println("Total sentences: " + sentenceCounter);
		System.out.println("Total sentences with coordinations: " + coordSentenceCouter);
		System.out.println("Total coordinations: " + coordCounter);
	}

	
	/*private static void makeStaticBinary() {
		File inputDir = new File(WsjOriginalReadable);
		File[] dirList = inputDir.listFiles();
		for(File inputFile : dirList) {
			if (inputFile.isDirectory()) continue;
			String name = inputFile.getName(); //wsj-00.mrg, wsj-00-21.mrg, ..., wsj-24.mrg			
			String section = name.substring(4, name.indexOf('.'));
			Corpus originalCorpus = new Corpus(inputFile, "Wsj");
			File binaryOut = new File(WsjBinary + section + ".binary");
			originalCorpus.toBinaryFile(binaryOut);
		}
	}*/

	static public void retriveTrainingAndTestCorpus() {
		
		Arrays.sort(nonNecessaryLabels);
		
		String dirParam = 	WsjBinary +
							"LLtr" + Parameters.lengthLimitTraining +
							"_LLts" + Parameters.lengthLimitTest +
							"_TS" + testSet +
							"_UK" + Parameters.ukLimit +
							"_H" + initialHeads +
							"_TR" + Utility.booleanToOnOff(Parameters.traces) +
							"_ST" + Utility.booleanToOnOff(Parameters.semanticTags) +
							"_RN" + Utility.booleanToOnOff(Parameters.replaceNumbers) +
							"_SK" + Utility.booleanToOnOff(skip120TrainingSentences) +
							"_RR" + Utility.booleanToOnOff(Parameters.removeRedundantRules) +
							"_NPB" + Utility.booleanToOnOff(transformNPbasal) +
							"_SG" + Utility.booleanToOnOff(transformSG) +
							"/";
		
		String WsjTestSecFile = "wsj-" + testSet;
				
		File trainingBinaryCorpus = new File(dirParam + WsjTrainingSecFile + ".binary");
		File testBinaryCorpus = new File(dirParam + WsjTestSecFile + ".binary");
		String trainingStatPath = dirParam + "TrainingStat/";
		File trainingStatDir = new File(trainingStatPath);
		File internalLabels = new File(trainingStatPath + "internalLabels");
		File posTagLabelsPath = new File(trainingStatPath + "posTagLabels");
		File lexiconTablePath = new File(trainingStatPath + "lexiconTable");
		File lexiconTable = new File(trainingStatPath + "labelReports.txt");
		File labelReport = new File(trainingStatPath + "labelReports.txt");
		File internalLablesTable = new File(trainingStatPath + "internalLabelsTable.txt");
		File posTagTable = new File(trainingStatPath + "posTagTable.txt");
		
		if (trainingBinaryCorpus.exists() && testBinaryCorpus.exists()) {
			FileUtil.appendReturn("Trainig and test corpora read from binary", Parameters.logFile);
			Parameters.trainingCorpus = ConstCorpus.fromBinaryFile(trainingBinaryCorpus);
			Parameters.testCorpus = ConstCorpus.fromBinaryFile(testBinaryCorpus);
			Parameters.internalLabels = (String[])FileUtil.fromBinaryFile(internalLabels);
			Parameters.posTagLabels = (String[])FileUtil.fromBinaryFile(posTagLabelsPath);
			Parameters.lexiconTable =  (Hashtable<String,Integer>)FileUtil.fromBinaryFile(lexiconTablePath);
		}
		else {		
			//not present --> build it!			
			String initialDir = possibleInitialHeadPath[initialHeads];
			File traininigFile =  new File(initialDir + WsjTrainingSecFile + ".mrg");
			File testFile = new File(initialDir + WsjTestSecFile + ".mrg");		
			File trainingCompleteCorpus = new File(dirParam + WsjTrainingSecFile + ".mrg");
			File testCompleteCorpus = new File(dirParam + WsjTestSecFile + ".mrg");
			
			Parameters.trainingCorpus = new ConstCorpus(traininigFile, WsjTrainingSecFile);
			Parameters.testCorpus = new ConstCorpus(testFile, WsjTestSecFile);	
			
			if (skip120TrainingSentences) {
				Utility.removeOneToIntArray(collinsSkipArr);
				Parameters.trainingCorpus.removeIndexes(collinsSkipArr);
			}
			Parameters.trainingCorpus.removeTreesLongerThan(Parameters.lengthLimitTraining, nonCountCatInLength);
			Parameters.testCorpus.removeTreesLongerThan(Parameters.lengthLimitTraining, nonCountCatInLength);
			
			Parameters.lexiconTable =  Parameters.trainingCorpus.buildLexFreq();			
			finalCleaning(Parameters.trainingCorpus, Parameters.lexiconTable, true, Parameters.traces);				
			finalCleaning(Parameters.testCorpus, Parameters.lexiconTable, false, Parameters.traces);	
			
			trainingStatDir.mkdirs();
			Utility.hashtableOrderedToFile(Parameters.lexiconTable, lexiconTable);
			Parameters.trainingCorpus.buildLabelsStatistics(labelReport, internalLablesTable, posTagTable);						
			FileUtil.toBinaryFile(internalLabels, Parameters.internalLabels);
			FileUtil.toBinaryFile(posTagLabelsPath, Parameters.posTagLabels);	
			FileUtil.toBinaryFile(lexiconTablePath, Parameters.lexiconTable);
			
			Parameters.trainingCorpus.toBinaryFile(trainingBinaryCorpus);
			Parameters.testCorpus.toBinaryFile(testBinaryCorpus);
			Parameters.trainingCorpus.toFile_Complete(trainingCompleteCorpus, true);
			Parameters.testCorpus.toFile_Complete(testCompleteCorpus, true);
			FileUtil.appendReturn("Built binary training and test corpus", Parameters.logFile);
		}

		FileUtil.appendReturn("The training corpus " + 
				"has # sentences: " + Parameters.trainingCorpus.size(), Parameters.logFile);
		FileUtil.appendReturn("The test corpus " + 
				"has # sentences: " + Parameters.testCorpus.size(), Parameters.logFile);				
	}
	
	/*static public void removeRedundantRulesPost() {
		if (!Parameters.semanticTags) {
			Parameters.trainingCorpus.removeRedundantRules();
			Parameters.testCorpus.removeRedundantRules();
		}
	}*/
	
    public static void removeQuotations(ConstCorpus corpus) {
        for(TSNode tree : corpus.treeBank) {
                tree.pruneSubTrees("''");
                tree.pruneSubTrees("``");
        }
    }

	
	public static String posProcessTestLine(String line) {
		// remove NP basal nodes
		// adjust subject-less sentences SG -> S
		return line;
	}
	
	public static void reportCollinsStatistics() {
		File binaryFie = new File("/home/fsangati/CORPUS/WSJ/BINARY/" +
				"22_LLtr40_LLts40_UK0_STon_RNoff_SKoff_KTFoff.binary");
		File statisticFile = new File("/home/fsangati/CORPUS/WSJ/BINARY/" +
				"22_LLtr40_LLts40_UK0_STon_RNoff_SKoff_KTFoff.Statistic");
		ConstCorpus collinsCorpus = ConstCorpus.fromBinaryFile(binaryFie);
		collinsCorpus.checkHeadAnnotationStatistics(statisticFile);
	}
	
	public static String writeParam() {
		String param = "\n\n" + "Corpus: " + "Wsj";
		param += "\n" + "Test Corpus Section: " + testSet; // 00 | 01 | 22 | 23
		param += "\n" + "Initial Heads: " + initialHeadLabels[initialHeads];		
		param += "\n" + "Skip 120 Collins sentences: " + skip120TrainingSentences;
		param += "\n" + "Transform NP basal (NP --> NPB): " + transformNPbasal;
		param += "\n" + "Transform subjectless sentence (S --> SG): " + transformSG;
		return param;
	}
	
	public static void makeClean(File startDir, File outputDirClean) {
		outputDirClean.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;			
			File outputCleanedFile = new File(outputDirClean + "/" + inputFile.getName());
			makeCleanFile(inputFile, outputCleanedFile);
		}		
	}
	
	public static void makeCleanFile(File startFile, File outputFile) {
		ConstCorpus corpus = new ConstCorpus(startFile, FileUtil.defaultEncoding);
		corpus.removeTraces(Wsj.traceTag);
		corpus.removeNumbersInLables();
		corpus.removeRedundantRules();
		corpus.toFile_Complete(outputFile, false, false);
	}
	
	public static void removeSemTagDir(File startDir, File outputDirClean) throws Exception {
		outputDirClean.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			if (inputFile.getName().startsWith(".") || inputFile.isDirectory()) continue;
			File outputCleanedFile = new File(outputDirClean + "/" + inputFile.getName());
			removeSemTagFile(inputFile, outputCleanedFile);
		}	
	}
	
	public static File[][] collectFiles(File startDir, File outputDir) {
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);
		ArrayList<File> arrayListResultInput = new ArrayList<File>(); 
		ArrayList<File> arrayListResultOutput = new ArrayList<File>();
		for(File inputFile : fileList) {
			if (inputFile.getName().startsWith(".") || inputFile.isDirectory()) continue;			
			arrayListResultInput.add(inputFile);			
			File outputFile = new File(outputDir + "/" + inputFile.getName());
			arrayListResultOutput.add(outputFile);
		}
		int size = arrayListResultInput.size(); 
		File[][] result = new File[2][size];
		arrayListResultInput.toArray(result[0]);
		arrayListResultInput.toArray(result[1]);
		return result;
	}
	
	public static void removeSemTagFile(File inputFile, File ouputFile) throws Exception {
		PrintWriter pw = FileUtil.getPrintWriter(ouputFile);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		for(TSNodeLabel t : treebank) {
			t.removeSemanticTags();
			t.removeRedundantRules();
			//t.replaceAllLabels("_C", "-ADV ");
			pw.println(t.toString());
		}
		pw.close();
	}
	
	public static void makeNaive() {
		File startDir = new File(Wsj.WsjOriginalCleaned);
		//File outputDirNaive = new File(Wsj.WsjOriginalCleanedLeft);
		//File outputDirNaive = new File(Wsj.WsjOriginalCleanedRight);
		File outputDirNaive = new File(Wsj.WsjOriginalCleanedRandom);
		outputDirNaive.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;
			ConstCorpus corpus = new ConstCorpus(inputFile, FileUtil.defaultEncoding, false);
			File outputCleanedFile = new File(outputDirNaive + "/" + inputFile.getName());
			//corpus.assignFirstLeftHeads();
			//corpus.assignFirstRightHeads();
			corpus.assignRandomHeads();
			corpus.toFile_Complete(outputCleanedFile, true, false);
		}		
	}
	
	public static void checkHeads(File startDir) {
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;
			ConstCorpus corpus = new ConstCorpus(inputFile, "noname");
			corpus.removeTop();
			corpus.checkHeadAnnotationStatistics(new File(startDir + "/" + inputFile.getName() + ".headCorrection"));
		}		
	}
	
	public static void removeArgumentInHeadsInCollins99() {
		File[] fileList = new File(WsjOriginalCleanedCollins99).listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {			
			if (inputFile.isDirectory()) continue;
			File outputFile = new File(inputFile + "_fixed");
			ConstCorpus corpus = new ConstCorpus(inputFile, "noname");
			corpus.removeTop();
			corpus.removeArgumentInHeads();
			corpus.toFile_Complete(outputFile, true);
		}	
	}
	
	public static void removeHeads() {
		File inFile = new File("/scratch/fsangati/RESULTS/TSG/LTSG/Collins/NoHeads/TrainingCorpus.processed");
		ConstCorpus corpus = new ConstCorpus(inFile,"");
		corpus.toFile_Complete(inFile, false, false);
	}
	
	public static void countAllFragments() throws Exception {
		File trainingCorpus = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingCorpus);
		BigInteger result = BigInteger.ZERO;
		for(TSNodeLabel t : treebank) {
			result = result.add(t.countTotalFragments()[1]);
		}
		System.out.println("Total fragments: " + result);
	}
	
	public static void countAllFragmentsDepths() throws Exception {
		File trainingCorpus = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingCorpus);
		int maxDepthTreebank = TSNodeLabel.maxDepthTreebank(treebank);
		BigInteger[] result = new BigInteger[maxDepthTreebank];		
		Arrays.fill(result, BigInteger.ZERO);
		for(TSNodeLabel t : treebank) {
			BigInteger[] resultTree = t.countTotalFragmentsDepth()[0];
			for(int i=0; i<resultTree.length; i++) {
				result[i] = result[i].add(resultTree[i]);
			}
		}
		BigInteger totalSum = BigInteger.ZERO;
		for(int i=0; i<maxDepthTreebank; i++) {
			BigInteger depthTotalFragments = result[i];
			System.out.println("Depth " + (i+1) + ": " + depthTotalFragments);
			totalSum = totalSum.add(depthTotalFragments);
		}
		System.out.println("Total fragments: " + totalSum);
	}
	
	public static void maxDepthMaxBranchingStatistics() throws Exception {
		File trainingCorpus = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingCorpus);
		int maxDepthTreebank = TSNodeLabel.maxDepthTreebank(treebank);
		int maxBranchingTreebank = TSNodeLabel.maxBranchingTreebank(treebank);
		System.out.println("Max depth: " + maxDepthTreebank);
		System.out.println("Max branching: " + maxBranchingTreebank);
		int[][] countDB = new int[maxDepthTreebank][maxBranchingTreebank];
		for(TSNodeLabel t : treebank) {
			int d = t.maxDepth();
			int b = t.maxBranching();
			if (b==51) System.out.println(t);
			countDB[d-1][b-1]++;
		}
		for(int d=0; d<maxDepthTreebank; d++) {			
			for(int b=0; b<maxBranchingTreebank; b++) {
				System.out.print(countDB[d][b] + "\t");
			}
			System.out.println();
		}
	}	
	
	public static BigInteger multiplyFactor = BigInteger.valueOf(10);
	public static void countAllFragmentsDepthsMaxBranching() throws Exception {
		File trainingCorpus = new File(Wsj.WsjOriginalCleaned + "wsj-02-21.mrg");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(trainingCorpus);
		//int maxDepthTreebank = TSNodeLabel.maxDepthTreebank(treebank);
		int maxAllowedDepth = 25;
		//int maxBranchingTreebank = TSNodeLabel.maxBranchingTreebank(treebank);
		int maxAllowedBranching = 7;
		System.out.println("Max depth: " + maxAllowedDepth);
		System.out.println("Max branching: " + maxAllowedBranching);						
		maxAllowedDepth++;
		maxAllowedBranching++;
		BigInteger[][] result = new BigInteger[maxAllowedDepth][maxAllowedBranching];		
		for(int d=0; d<maxAllowedDepth; d++) Arrays.fill(result[d], BigInteger.ZERO);				
		for(TSNodeLabel t : treebank) {		
			if (t.maxBranching()>maxAllowedBranching-1) continue;
			if (t.maxDepth()>maxAllowedDepth-1) continue;
			for(int b=0; b<maxAllowedBranching; b++) {
				BigInteger[] resultTree = t.countTotalFragmentsDepthMaxBranching(b)[0];
				for(int d=0; d<resultTree.length; d++) {
					result[d+1][b] = result[d+1][b].add(resultTree[d]);					
				}				
			}			
		}		
		boolean[] allEqualToPreviousBranching = new boolean[maxAllowedBranching];
		Arrays.fill(allEqualToPreviousBranching, true);
		allEqualToPreviousBranching[0] = false;
		for(int d=0; d<maxAllowedDepth; d++) {
			for(int b=1; b<maxAllowedBranching; b++) {				
				if (allEqualToPreviousBranching[b] && 
						result[d][b].compareTo(result[d][b-1].multiply(multiplyFactor))>0) {
					allEqualToPreviousBranching[b] = false;
				}
			}
		}
		for(int b=1; b<maxAllowedBranching; b++) {
			if (allEqualToPreviousBranching[b]) continue;
			System.out.print("\t" + b);
		}
		System.out.println();
		//System.out.println("0\t" + result[0][0]);		
		for(int d=1; d<maxAllowedDepth; d++) {
			System.out.print(d);
			for(int b=1; b<maxAllowedBranching; b++) {
				//if (d==0 && b==0) System.out.print("\t0");	
				if (allEqualToPreviousBranching[b]) continue;
				BigInteger depthBranchTotalFragments = result[d][b];
				System.out.print("\t" + depthBranchTotalFragments);				
			}
			System.out.println();
		}		
	}
	
	public static void corpusStatistics() throws Exception {
		TreeSet<String> nodeLabels = new TreeSet<String>();
		TreeSet<String> posLabels = new TreeSet<String>();		
		File baseDir = new File(Wsj.WsjOriginalCleanedSemTagsOff);
		File[] fileList = baseDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.getName().startsWith(".") || inputFile.isDirectory()) continue;
			ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);			
			for(TSNodeLabel t : treebank) {
				t.collectNodesPos(nodeLabels, posLabels);
			}
		}		
		File nodesOutputFile = new File (Wsj.WsjBase + "statistics/wsj-00-24_originalReadableCleaned_noSemTags_nodesStat.txt");
		PrintWriter pw = FileUtil.getPrintWriter(nodesOutputFile);
		for(String s : nodeLabels) pw.println(s);
		pw.close();
		File posOutputFile = new File (Wsj.WsjBase + "statistics/wsj-00-24_originalReadableCleaned_noSemTags_posStat.txt");
		pw = FileUtil.getPrintWriter(posOutputFile);
		for(String s : posLabels) pw.println(s);
		pw.close();
	}
	
	public static void markCircumstantialDir(File startDir, File outputDir) throws Exception {
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.getName().startsWith(".") || inputFile.isDirectory()) continue;						
			File newFile = new File(outputDir + "/" + inputFile.getName());
			markCircumstantial(inputFile, newFile);
		}
	}
	
	public static void markCircumstantial(File inputFile, File outputFile) throws Exception {
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TSNodeLabel t : treebank) {
			t.markCircumstantial(tesniere.Box.advSemTagSorted, "_C");
			pw.println(t.toStringExtraParenthesis());
		}
		
		pw.close();		
	}
	
	public static void makeCleanWSJ(File inputFile, File outputFile) throws Exception {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<String> treebankString = Wsj.makeReadableFileToArray(inputFile);
		for(String treeString : treebankString) {
			TSNodeLabel t = new TSNodeLabel(treeString);
			t.makeCleanWsj();			
			pw.println(t.toString());
		}
		pw.close();
	}
	
	public static void makeCharBased(File inputFile, File outputFile) throws Exception {
		if (inputFile.isDirectory()) {
			File[][] srcDstFiles = Wsj.getFilePairs(inputFile, outputFile);
			int size = srcDstFiles[0].length;
			for(int i=0; i<size; i++) {
				makeCharBased(srcDstFiles[0][i], srcDstFiles[1][i]);				
			}
			return;
		}
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		for(TSNodeLabel t : treebank) {
			t.makeTreeCharBased();			
			pw.println(t.toString());
		}
		pw.close();
	}
	
	private static void convertNMLBracketingToNormalDir(File startDir, File outputDir) throws Exception {
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.getName().startsWith(".") || inputFile.isDirectory()) continue;						
			File newFile = new File(outputDir + "/" + inputFile.getName());
			convertNMLBracketingToNormal(inputFile, newFile);
		}
		
	}
	
	private static void convertNMLBracketingToNormal(File inputFile, File outputFile) throws Exception {
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TSNodeLabel t : treebank) {
			t.replaceAllNonTerminalLabels("NML", "NP");
			t.replaceAllNonTerminalLabels("JJP", "ADJP");
			pw.println(t.toStringExtraParenthesis());
		}		
		pw.close();	
	}
	
	public static ArrayList<TSNodeLabel> getTreebankReadableAndClean(File inputFile) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		ArrayList<String> treebankString = Wsj.makeReadableFileToArray(inputFile);
		for(String treeString : treebankString) {
			TSNodeLabel t = new TSNodeLabel(treeString);
			t.makeCleanWsj();			
			result.add(t);
		}
		return result;
	}
	
	public static ArrayList<TSNodeLabel> getTreebank(File inputFile) throws Exception {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		ArrayList<String> treebankString = Wsj.makeReadableFileToArray(inputFile);
		int i=1;
		//try {
			for(String treeString : treebankString) {
				TSNodeLabel t = new TSNodeLabel(treeString);			
				result.add(t);
				i++;
			}
		/*} catch (Exception e) {			
			e.printStackTrace();
			System.out.println("Error in line: " + i);
			return null;
		}*/		
		return result;
	}
	
	public static ArrayList<TSNodeLabelStructure> getTreebankStructure(File inputFile) throws Exception {
		ArrayList<TSNodeLabelStructure> result = new ArrayList<TSNodeLabelStructure>();
		ArrayList<String> treebankString = Wsj.makeReadableFileToArray(inputFile);
		for(String treeString : treebankString) {
			TSNodeLabelStructure t = new TSNodeLabelStructure(treeString);		
			result.add(t);
		}
		return result;
	}
	
	public static ArrayList<File> getFileInDir(File startDir) throws Exception {
		ArrayList<File> result = new ArrayList<File>();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.getName().startsWith(".") || inputFile.isDirectory()) continue;
			result.add(inputFile);			
		}
		return result;
	}
	
	public static void auxifyWsj(File startDir, File outputDir) throws Exception {
		Auxify.orderArrays();
		ArrayList<File> fileList = getFileInDir(startDir);
		for(File inputFile : fileList) {
			ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
			File outputFile = new File(outputDir + "/" + inputFile.getName());
			for(TSNodeLabel t : treebank) {
				Auxify.auxify(t);				
			}
			TSNodeLabel.printTreebankToFile(outputFile, treebank, false, false);
		}
	}
	
	public static void filterSentenceUpToLength(int length, File startDir, File outputDir) throws Exception {
		ArrayList<File> fileList = getFileInDir(startDir);
		for(File inputFile : fileList) {
			ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
			File outputFile = new File(outputDir + "/" + inputFile.getName());
			PrintWriter pw = new PrintWriter(outputFile);
			for(TSNodeLabel t : treebank) {
				int sentenceLength = t.countLexicalNodesExcludingCatLabels(nonCountCatInLength); 
				if (sentenceLength>length) continue;
				pw.println(t.toString());
			}
			pw.close();
		}
	}
	
	private static void checkDiff() throws Exception {
		File file1 = new File(Wsj.WsjOriginalCleanedSemTagsOff + "wsj-02-21.mrg");
		File file2 = new File(Wsj.WsjOriginalCleanedTop + "wsj-02-21.mrg");
		ArrayList<TSNodeLabel> corpus1 = TSNodeLabel.getTreebank(file1);
		ArrayList<TSNodeLabel> corpus2 = TSNodeLabel.getTreebank(file2);
		Iterator<TSNodeLabel> iter1 = corpus1.iterator();
		Iterator<TSNodeLabel> iter2 = corpus2.iterator();
		int index = 0;
		while(iter1.hasNext()) {
			index++;
			TSNodeLabel tree1 = iter1.next().addTop();
			TSNodeLabel tree2 = iter2.next();
			tree2.removeSemanticTags();
			if (!tree1.equals(tree2)) {
				System.out.println(index + ":");
				System.out.println(tree1.toString());
				System.out.println(tree2.toString());
				System.out.println();
			}
		}
	}
	
	private static void makeTreesFlatForCharniack() {
		File inputFile = new File(WsjFlatNoTraces + "wsj-24.mrg");
		File outputFile = new File(WsjFlatNoTracesCharniak + "wsj-24.mrg");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			line = "<s> " + line + " </s>";			
			pw.println(line);
		}
		pw.close();
		scan.close();
	}
	
	private static void cleanCharniackOutput() {
		String workingDir = "/Users/fedja/Work/SOFTWARE/CharniakParser/reranking-parser/first-stage/Results/EN_noAux/";
		File inputFile = new File(workingDir + "wsj-22_1000best_noAux.mrg");
		File outputFile = new File(workingDir + "wsj-22_1000best_noAux_cleaned.mrg");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith("(")) {
				line = line.replaceAll("S1", "TOP");
				pw.println(line);	
			}
			else if (line.equals("")) pw.println();			
		}
		pw.close();
		scan.close();
	}
	
	private static void customTransform(File startDir, File outputDir) throws Exception {
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.getName().startsWith(".") || inputFile.isDirectory()) continue;						
			File newFile = new File(outputDir + "/" + inputFile.getName());
			makeCharBased(inputFile, newFile);
		}
	}
	
	private static void checkNodesOverlap() throws Exception {
		File corpusFile = new File(Wsj.WsjOriginalCleanedTopSemTagsOff + "wsj-02-21.mrg");
		ArrayList<TSNodeLabel> treebank = Wsj.getTreebank(corpusFile);
		HashSet<Label> internalNodesSet = new HashSet<Label>();
		HashSet<Label> lexicalNodesSet = new HashSet<Label>();
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> allNodes = t.collectAllNodes();
			for(TSNodeLabel n : allNodes) {
				if (n.isLexical) lexicalNodesSet.add(n.label);
				else internalNodesSet.add(n.label);
			}
		}
		System.out.println("Overlapping between lexical and non-lexical nodes:");
		internalNodesSet.retainAll(lexicalNodesSet);
		System.out.println(internalNodesSet.toString());
	}
	
	public static File[][] getFilePairs(File srcDir, File dstDir) {
		dstDir.mkdir();
		File[] fileList = srcDir.listFiles();
		Arrays.sort(fileList);
		ArrayList<File> srcFiles = new ArrayList<File>();
		ArrayList<File> dstFiles = new ArrayList<File>();
		int size = 0;
		for(File inputFile : fileList) {
			if (inputFile.getName().startsWith(".") || inputFile.isDirectory()) continue;
			srcFiles.add(inputFile);
			File newFile = new File(dstDir + "/" + inputFile.getName());
			dstFiles.add(newFile);
			size++;
		}
		File[][] result = new File[2][size];
		result[0] = srcFiles.toArray(result[0]);
		result[1] = dstFiles.toArray(result[1]);
		return result;
	}
	
	private static void makeAllCleaning(File inputFile, File outputFile) throws Exception {
		ArrayList<String> readablePenn = makeReadableFileToArray(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(String treePenn : readablePenn) {
			TSNodeLabel t = new TSNodeLabel(treePenn);			
			t.pruneSubTrees("-NONE-");
			t.removeNumberInLabels();			
			t.removeSemanticTags();
			//t.removeRedundantRules();
			pw.println(t);
		}
		pw.close();				
	}
	
	public static void makeWordStats() throws Exception {
		File outputFile = new File("/scratch/fsangati/CORPUS/WSJ/statistics/lexiconSec-02-21.txt");		
		File trainFile = new File("/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/ORIGINAL_READABLE_CLEANED/wsj-02-21.mrg");
		Scanner scan = FileUtil.getScanner(trainFile);
		HashMap<Label,int[]> lexFreq = new HashMap<Label,int[]>(); 
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line, true);
			ArrayList<Label> treeLex = tree.collectLexicalLabels();
			for(Label l : treeLex) {
				Utility.increaseInHashMap(lexFreq, l);
			}
		}
		TreeSet<StringInteger> lexOrder = new TreeSet<StringInteger>();
		for(Entry<Label,int[]> e : lexFreq.entrySet()) {
			lexOrder.add(new StringInteger(e.getKey().labelString, e.getValue()[0]));			
		}
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(StringInteger si : lexOrder) {
			pw.println(si.string() + "\t" + si.integer());
		}
		pw.close();
	}

	
	public static void main(String args[]) throws Exception {
		
		makeWordStats();
		
		//addTop( new File("/scratch/fsangati/CORPUS/FrenchTreebank/Cleaned/"), 
		//		new File("/scratch/fsangati/CORPUS/FrenchTreebank/CleanedTop/"));
		
		//makeAllCleaning(new File(WsjOriginal + "wsj-23.mrg"), 
		//		new File("/Users/fedja/Downloads/sec23/wsj-23_finalCleaning_withRedudantRules.mrg"));
		
		
		//removeSemTagDir(new File(Wsj.WsjOriginalCleanedTop), new File(WsjOriginalCleanedTopSemTagsOff));
		//makeReadable(new File(WsjOriginal), new File(WsjOriginalReadable)); 
		//makeReadable(new File(WsjConstBase + "ORIGINAL_NP_BRACKETING"), new File(WsjConstBase + "ORIGINAL_NP_BRACKETING_READABLE"));
		//makeOriginalNoTraces();
		//makeFlats();
		//makeStaticBinary();
		//makeClean(new File(WsjOriginalReadable), new File(WsjOriginalCleaned));		
		//makeClean(new File(WsjOriginalNPBraketing), new File(WsjOriginalNPBraketingCleaned));		
		//checkHeads(new File(WsjOriginalCleanedCollins99));
		//makeNaive();
		//removeHeads();
		//removeArgumentInHeadsInCollins99();
		//File inputFile = new File("/scratch/fsangati/CORPUS/Evalita09/Treebanks/Constituency/TUTinPENN-rel2_1newspaper10-9-09.penn");
		//File outputFile = new File("/scratch/fsangati/CORPUS/Evalita09/Treebanks/Constituency/TUTinPENN-rel2_1newspaper10-9-09.readable.penn");
		//makeReadableFile(inputFile, outputFile);
		//addTop(new File(WsjOriginalCleaned), new File(WsjOriginalCleanedTop));
		//fixCC();
		//countCC();
		//countAllFragments();
		//countAllFragmentsDepths();
		//countAllFragmentsDepthsMaxBranching();
		//maxDepthMaxBranchingStatistics();
		//removeSemTagDir(new File(WsjOriginalCleaned), new File(Wsj.WsjOriginalCleanedSemTagsOff));		
		//corpusStatistics();
		//convertNMLBracketingToNormalDir(new File(WsjConstBase + "ORIGINAL_NML_BRACKETING_READABLE_CIRCUMSTANTIAL"),
		//		new File(WsjConstBase + "ORIGINAL_NP_BRACKETING_READABLE_CIRCUMSTANTIAL"));
		//markCircumstantialDir(new File(WsjConstBase + "ORIGINAL_NP_BRACKETING_READABLE"), 
		//		new File(Wsj.WsjConstBase + "ORIGINAL_NP_BRACKETING_READABLE_CIRCUMSTANTIAL"));
		//makeCleanWSJ(
		//		new File(Wsj.WsjConstBase + "ORIGINAL_READABLE_CIRCUMSTANTIALS_SEMTAGSOFF/wsj-00.mrg"),
		//		new File("/Users/fedja/Desktop/Viewers/wsj-00_readble_cleaned_circum_semTagOff.mrg"));
		
		//auxifyWsj(new File(Wsj.WsjOriginalReadable), new File(Wsj.WsjOriginalReadableAuxify));
		//auxifyWsj(new File(Wsj.WsjOriginalCleanedSemTagsOff), new File(Wsj.WsjOriginalCleanedSemTagsOffAuxify));
		
		//checkDiff();
		
		//filterSentenceUpToLength(10, new File(Wsj.WsjOriginalReadable), new File(Wsj.Wsj10));
		//makeSingleLabelX(new File(Wsj.Wsj10 + "wsj-originalReadableCleaned.mrg"), new File(Wsj.Wsj10 + "wsj-singleLabelX.mrg"));
		
		//System.out.println(Arrays.toString(nonCountCatInLength));
		//Arrays.sort(nonCountCatInLength);
		//System.out.println(Arrays.toString(nonCountCatInLength));
		//makeTreesFlatForCharniack();
		//cleanCharniackOutput();

		
		//Wsj.addTop(new File(WsjOriginalCleanedSemTagsOff), 
		//		new File(WsjOriginalCleanedTopSemTagsOff));
		
		//customTransform(new File(WsjOriginalCleanedTopSemTagsOff), 
		//		new File(WsjOriginalCleanedTopSemTagsOffCharBased));
		
		//File inputFile = new File("/Users/fedja/Desktop/prova.mrg");
		//File outputFile = new File("/Users/fedja/Desktop/prova_char.mrg");
		
		//makeCharBased(new File(Wsj.WsjOriginalCleanedTopSemTagsOff), 
		//		new File(Wsj.WsjOriginalCleanedTopSemTagsOffCharBased));		
		
		//String s = "fdas\\/fdas";
		//System.out.println(s);
		//char[] c = s.toCharArray();
		//System.out.println(Arrays.toString(c));
		//checkNodesOverlap();
		
	}


	

	





	

}

