package tsg.kernels;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelStructure;
import tsg.corpora.Wsj;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationLeft_Petrov;
import util.ArgumentReader;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class FragmentSeeker {
	
	static String exactFrequenciesOption = "-exactFrequencies:";
	static String partialFragmentsOption= "-partialFragments:";	
	static String maxCombinationOption = "-maxCombination:";
	static String maxMappingsOption = "-maxMappings:";
	static String flushToDiskEveryOption = "-flushToDiskEvery:";		
	static String resumeOption = "-resumeDir:";
	static String threadsOption = "-threads:";
	static String compressTmpFilesOption = "-compressTmpFiles:";
	static String removeTmpFilesOption = "-removeTmpFiles:";
	static String extractUncoveredFragmentsOption = "-extractUncoveredFragments:";
	static String ukModelOption = "-ukModel:";
	static String uknownThresholdOption = "-ukThreshold:";
	static String markoBinarizeOption = "-markoBinarize:";
	static String markoBinarizerTypeOption = "-markoBinarizerType:";
	static String markovH_option = "-markovH:";
	static String markovV_option = "-markovV:";
	static String outputPathOption = "-outputPath:";
	
	
	static String outputPath = null;
	static String resumePathDir = null;						
	static boolean exactFrequencies = true;
	static boolean partialFragments = false;
	static boolean compressTmpFiles = false;
	static boolean removeTmpFiles = true;
	static boolean extractCoverFragments = false;	
	static boolean markoBinarize = false;
	static int flushToDiskEvery = 100;
	static int threads = 1;
	
	static File treebankFile;
	static ArrayList<TSNodeLabelStructure> treebankStructure;
	static ArrayList<TSNodeLabel> treebank;
	static int treebankSize;
	
	static int startIndex = 0;		
	static int fileCounterStart = 0;
	static String prefixFragmentsFiles = "fragments_";
	static String prefixTmpFragmentsFiles = "tmp_";
	static String prefixSortedFragmentsFiles = "sorted_";	
	static File tmpDir = null;	
		
	public static void main(String[] args) throws Exception {		
		
		long time = System.currentTimeMillis();		
		

		String usage = "USAGE: java [-Xmx1G] -jar FragmentSeeker.jar [-partialFragments:false] " +
				"[-maxCombination:1000] [-maxMappings:1000] [-flushToDiskEvery:100] " +
				"[-resumeDir:previousDirPath] [-exactFrequencies:true] [-removeTmpFiles:true] " +
				"[-compressTmpFiles:false] [-extractUncoveredFragments:false] [-threads:1] [-outputPath:null] " +
				"[-markoBinarize:false] [-markoBinarizerType:Petrov_left] [-markovH:1] [-markovV:2] " +
				"[-ukModel:English_Petrov] [-ukThreshold:-1] treebankFile";

		
		//String usage = "USAGE: java [-Xmx1G] -jar FragmentExtractor.jar [-threads:1] [-markoBinarize:false] " +
		//		"[-ukThreshold:-1] treebankFile";
		
		/*args = new String[]{"-partialFragments", "-maxLength:1000", "-maxCombination:1000",
				"-maxMappings:1000", "/Users/fedja/Desktop/FSprob/wsj-02-21.mrg"};*/		
		
		CommonStructures.maxCombDaughters = 1000;
		//CommonStructures.extractIntermediateStructures = true;
		AllOrderedNodeSubSet.maxComb = 1000;
		AllOrderedNodeSubSet.maxComb = 1000;		
		
		UkWordMapping.ukThreashold = -1;
		UkWordMapping ukModel = new UkWordMappingPetrov(); //was new UkWordMappingStd();
		
		TreeMarkoBinarization treeMarkovBinarizer = new TreeMarkoBinarizationLeft_Petrov();
		//TreeMarkoBinarization treeMarkovBinarizer = new TreeMarkoBinarizationLeft_Klein();		
		//TreeMarkoBinarization treeMarkovBinarizer = new MarkoBinarizationBerkeley(Binarization.RIGHT); // LEFT, RIGHT, PARENT, HEAD		
		TreeMarkoBinarization.markH = 1;		
		TreeMarkoBinarization.markV = 2;		
		
		//READING PARAMETERS
		if (args.length==0 || args.length>18) {
			System.err.println("Incorrect number of arguments: " + args.length);
			System.err.println(usage);
			System.exit(-1);
		}
		for(int i=0; i<args.length-1; i++) {
			String option = args[i];
			if (option.startsWith(exactFrequenciesOption))
				exactFrequencies = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(partialFragmentsOption))
				partialFragments = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(maxCombinationOption))
				CommonSubBranch.maxCombDaughters = ArgumentReader.readIntOption(option);					
			else if (option.startsWith(maxMappingsOption))
				AllOrderedNodeSubSet.maxComb = ArgumentReader.readIntOption(option);
			else if (option.startsWith(flushToDiskEveryOption))
				flushToDiskEvery = ArgumentReader.readIntOption(option);
			else if (option.startsWith(resumeOption))
				resumePathDir = ArgumentReader.readStringOption(option);
			else if (option.startsWith(removeTmpFilesOption))
				removeTmpFiles = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(compressTmpFilesOption))
				compressTmpFiles = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(extractUncoveredFragmentsOption))
				extractCoverFragments = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(threadsOption))
				threads = ArgumentReader.readIntOption(option);
			else if (option.startsWith(ukModelOption)) {
				ukModel = UkWordMapping.getModel(ArgumentReader.readStringOption(option));
				if (ukModel==null) {
					System.err.println("Invalid ukModel");
					return;
				}
			}
			else if (option.startsWith(uknownThresholdOption))
				UkWordMapping.ukThreashold = ArgumentReader.readIntOption(option);
			else if (option.startsWith(outputPathOption))
				outputPath = ArgumentReader.readStringOption(option);
			else if (option.startsWith(markoBinarizeOption))
				markoBinarize = ArgumentReader.readBooleanOption(option);
			else if (option.startsWith(markoBinarizerTypeOption)) {
				markoBinarize = true;
				String type = ArgumentReader.readStringOption(option);
				treeMarkovBinarizer = TreeMarkoBinarization.getMarkoBinarizer(type);
				if (treeMarkovBinarizer==null) {
					System.err.println("Invalid Markobinarizer model");
					return;
				}
			}				
			else if (option.startsWith(markovH_option))
				TreeMarkoBinarization.markH = ArgumentReader.readIntOption(option);
			else if (option.startsWith(markovV_option))
				TreeMarkoBinarization.markV = ArgumentReader.readIntOption(option);
			else {
				System.err.println("Not a valid option: " + option);
				System.err.println(usage);
				System.exit(-1);
			}
		}			
		
		treebankFile = new File(args[args.length-1]);
		if (!treebankFile.exists() || !treebankFile.canRead()) {
			System.err.println("Input file doesn't exist or not accessible: " + args[args.length-1]);
			System.exit(-1);
		}
		
		String inputFileAbsolutePath = null;
		try {
			inputFileAbsolutePath = treebankFile.getCanonicalPath();
			if (resumePathDir!=null) outputPath = new File(resumePathDir).getCanonicalPath() + "/";
			else {
				String dataFolder = "/FragmentSeeker_" + FileUtil.dateTimeStringUnique() + "/";
				//String dataFolder = "/FragmentExtractor_" + FileUtil.dateTimeString() + "/";
				if (outputPath!=null) {
					File dir = new File(outputPath);
					if (dir.exists())
						outputPath = dir + dataFolder;						
					else
						outputPath = dir + "/";
				}				
				else {				
					outputPath = treebankFile.getCanonicalFile().getParentFile().getCanonicalPath() + dataFolder;
				}
				new File(outputPath).mkdirs();
			}			
		} catch (IOException e) {
			System.err.println("Inpur directory doesn't exist or not accessible: " + args[args.length-1]);
			System.exit(-1);
		}
		
		
		try {						
			treebank = Wsj.getTreebank(treebankFile);
		} catch (Exception e) {			
			e.printStackTrace();
			System.err.println("Something wrong happened while reading the treebank in file: " + inputFileAbsolutePath);
			System.exit(-1);	
		}
		
		tmpDir = new File(outputPath + "tmp");
		int treebankMaxDepth = TSNodeLabel.maxDepthTreebank(treebank);
		treebankSize = treebank.size(); 
		if (treebankSize<2) {
			System.err.println("The treebank should have at least 2 structures.");
			System.exit(-1);
		}			
		
		File approxFreqFile = partialFragments ?
			new File(outputPath + "partialfragments_approxFreq.txt") :
			new File(outputPath + "fragments_approxFreq.txt");
		
		int maxFileCounter = treebank.size() / flushToDiskEvery;
		if (treebank.size()%flushToDiskEvery>0) maxFileCounter++;
		
		CommonStructures.maxDepth = treebankMaxDepth;										
		CommonStructures.flushToDiskEvery = flushToDiskEvery;
		CommonStructures.flushToDiskFileCounter = fileCounterStart;
		CommonStructures.flushToDiskPath = tmpDir.getAbsolutePath() + "/";
		CommonStructures.diskFileCounterSize = new Integer(maxFileCounter).toString().length();
		CommonStructures.prefixFragmentsFiles = prefixFragmentsFiles;
		CommonStructures.prefixTmpFragmentsFiles = prefixTmpFragmentsFiles;
		CommonStructures.compressTmpFiles = compressTmpFiles;		
				
		
		//START
		
		if (resumePathDir==null) {
			new File(outputPath).mkdir();
			Parameters.openLogFile(new File(outputPath + "FS_1.log"));
		}		
		
		else { //resumePath			
			
			int countFS = 1;			
			File[] ouputFiles = new File(outputPath).listFiles();
			for(File f : ouputFiles) {
				String fName = f.getName();
				if (fName.startsWith("FS") && fName.endsWith(".log")) countFS++;
			}
			Parameters.openLogFile(new File(outputPath + "FS_" + countFS + ".log"));
			Parameters.reportLineFlush("\n\nResuming FS");
			
		}
		
		Parameters.reportLineFlush("Input Arguments:\t" + Utility.joinStringArrayToString(args, " "));		
		Parameters.reportLineFlush("TreeBank file:\t" + treebankFile);
		Parameters.reportLineFlush("Treebank size:\t" + treebankSize);
		Parameters.reportLineFlush("Max depth:\t" + treebankMaxDepth);
		Parameters.reportLineFlush("Using partial fragments:\t" + partialFragments);		
		Parameters.reportLineFlush("AllOrderedNodeSubSet.maxComb:\t" + AllOrderedNodeSubSet.maxComb);
		Parameters.reportLineFlush("maxCombDaughters:\t" + CommonStructures.maxCombDaughters);
		//Parameters.reportLineFlush("extractIntermediateStructures:\t" + CommonStructures.extractIntermediateStructures);
		Parameters.reportLineFlush("Number of threads:\t" + threads);
		Parameters.reportLineFlush("Flushing fragments every:\t" + flushToDiskEvery);
		Parameters.reportLineFlush("Output exact frequencies:\t" + exactFrequencies);
		Parameters.reportLineFlush("Extract Cover Fragments:\t" + extractCoverFragments);
		if (UkWordMapping.ukThreashold>0) {
			Parameters.reportLine("UkModel: " + ukModel.getName());
			Parameters.reportLineFlush("Uknown Word Threshold (less or equal):\t" + UkWordMapping.ukThreashold);
		}
		
		if (markoBinarize) {
			Parameters.reportLine("MarkoBinarize Training Treebank");
			Parameters.reportLine("Type: " + treeMarkovBinarizer.getDescription());
			Parameters.reportLine("MarkH: " + TreeMarkoBinarization.markH);
			Parameters.reportLine("MarkV: " + TreeMarkoBinarization.markV);			
			treebank = treeMarkovBinarizer.markoBinarizeTreebank(treebank);			
			File transformedTrainingTreeBankFile = new File(outputPath + "trainingTreebank_MB.mrg");
			treebankFile = transformedTrainingTreeBankFile;
			TSNodeLabel.printTreebankToFile(transformedTrainingTreeBankFile, treebank, false, false);
			Parameters.reportLineFlush("Printed training treebank after MarkoBinarization to: " + transformedTrainingTreeBankFile);
		}		
		
		if (UkWordMapping.ukThreashold>0) {
			Parameters.reportLineFlush("Processing Unknown Words...");
	
			ukModel.init(treebank, null);
			treebank = ukModel.transformTrainingTreebank();			
			File transformedTrainingTreeBankFile= markoBinarize ? 
					new File(outputPath + "trainingTreebank_MB_UK.mrg") :
					new File(outputPath + "trainingTreebank_UK.mrg");
			treebankFile = transformedTrainingTreeBankFile;
			TSNodeLabel.printTreebankToFile(transformedTrainingTreeBankFile, treebank, false, false);			
			Parameters.reportLineFlush("Written treebank with unknown words to: " + transformedTrainingTreeBankFile);			
		}
		treebankStructure = Wsj.getTreebankStructure(treebankFile);
		
		if (approxFreqFile.exists()) {
			Parameters.reportLineFlush("File with approx freq found. Not extracting Fragments again!");
		}		
		else {			
			if (!tmpDir.exists()) tmpDir.mkdir();						
			else resumeTmpDir();
			
			// partial fragments
			if (partialFragments) {							
				CommonStructures cs = (exactFrequencies) ? 
						new CommonSubBranchMUBThreads(treebankStructure, threads, startIndex) :
						new CommonSubBranchMUBFreqThreads(treebankStructure, threads, startIndex);	
				cs.run();			
				Parameters.reportLineFlush("Finished Extracting Partial Fragmetns.");
			}
		
			// normal fragments
			else { 
				CommonStructures cs = 
					//(exactFrequencies) ?
					//	new CommonSubtreesMUBThreads(treebankStructure, threads, startIndex) :
					//	new CommonSubtreesMUBFreqThreads(treebankStructure, threads, startIndex);
					new CommonSubtreesMUBFreqInitialFinalThreads(treebankStructure, threads, startIndex);
				cs.run();
				Parameters.reportLineFlush("Finished Extracting Fragmetns.");
			}	
			
			if (compressTmpFiles) {
				new SortAndMergeFragmentFilesBz(tmpDir, approxFreqFile, 
						prefixFragmentsFiles, prefixSortedFragmentsFiles, prefixTmpFragmentsFiles);
			}
			else {
				new SortAndMergeFragmentFiles(tmpDir, approxFreqFile,
						prefixFragmentsFiles, prefixSortedFragmentsFiles, prefixTmpFragmentsFiles);
			}
			Parameters.reportLineFlush("Fragments with their approximate frequencies written in:\n\t" + approxFreqFile);
			
		}	

		
		File fragmentExactFreqFile = partialFragments ?				
				new File(outputPath + "partialfragments_exactFreq.txt") :
				new File(outputPath + "fragments_exactFreq.txt");	
						
		if (exactFrequencies) {
			
			if (fragmentExactFreqFile.exists()) {
				Parameters.reportLineFlush("File with exact freq found.");
			}	
			else {
				RetrieveCorrectFreqQueueInitialFinal RCF = new RetrieveCorrectFreqQueueInitialFinal(treebank, approxFreqFile,					
						fragmentExactFreqFile, partialFragments, threads);
				RCF.run();
				if (RCF.isInterrupted()) {
					Parameters.reportLineFlush("Interruption!");
					return;
				}
				Parameters.reportLineFlush("Fragments with their exact frequencies written in:\n\t" + fragmentExactFreqFile);
			}

		}
		
		if (removeTmpFiles) {
			Parameters.reportLineFlush("Removing temporal files.");
			File[] outputFiles = tmpDir.listFiles();
			if (outputFiles!=null) {
				for(File f : outputFiles) {
					f.delete();
				}
			}
			tmpDir.delete();			
		}
		
		if (extractCoverFragments) {
			
			File coverFragmentsFile = null;
			File allFragmentsFile = null;
			File fragmentFile = exactFrequencies ? fragmentExactFreqFile : approxFreqFile;
			
			
			if (partialFragments) {

				if (exactFrequencies) {
					coverFragmentsFile = new File(outputPath + "partialfragments_COVER_exactFreq.txt");
					allFragmentsFile = new File(outputPath + "partialfragments_ALL_exactFreq.txt");					 
				}
				else {
					coverFragmentsFile = new File(outputPath + "partialfragments_COVER_approxFreq.txt");
					allFragmentsFile = new File(outputPath + "partialfragments_ALL_approxFreq.txt");
				}

				if (!allFragmentsFile.exists()) {
					Parameters.reportLineFlush("Cannot collect cover fragments for partial fragments yet!");
				}
				
				else Parameters.reportLineFlush("File with COVER fragments already found.");
					
			}
			else {
				
				if (exactFrequencies) {
					coverFragmentsFile = new File(outputPath + "fragments_COVER_exactFreq.txt");
					allFragmentsFile = new File(outputPath + "fragments_ALL_exactFreq.txt");					 
				}
				else {
					coverFragmentsFile = new File(outputPath + "fragments_COVER_approxFreq.txt");
					allFragmentsFile = new File(outputPath + "fragments_ALL_approxFreq.txt");
				}
				
				if (!allFragmentsFile.exists()) {					
					Parameters.reportLineFlush("Collecting COVER Fragments in " + coverFragmentsFile);					
					new UncoveredFragmentsExtractor(treebank, fragmentFile, coverFragmentsFile, threads).run();
				}			
				else Parameters.reportLineFlush("File with COVER fragments already found.");
			}
			
			PrintWriter allFragmentFilePW = FileUtil.getPrintWriter(allFragmentsFile);
			Parameters.reportLineFlush("Collecting ALL Fragments in " + allFragmentsFile);
			FileUtil.append(fragmentFile, allFragmentFilePW);
			if (!partialFragments)
				FileUtil.append(coverFragmentsFile, allFragmentFilePW);
			allFragmentFilePW.close();
			
		}		
		
		Parameters.reportLineFlush("Took: " + (System.currentTimeMillis() - time)/1000 + " seconds.");
		Parameters.closeLogFile();
	}
	
	private static void resumeTmpDir() {
		File[] tmpFiles = tmpDir.listFiles();
		Arrays.sort(tmpFiles);
		boolean foundSortedFile = false;
		boolean foundHole = false;
		int expectedIndex = 1;
		
		for(File f : tmpFiles) {
			String fileName = f.getName();
			if (fileName.startsWith(prefixSortedFragmentsFiles)) {
				foundSortedFile = true;
				break;
			}
		}
		
		for(File f : tmpFiles) {
			String fileName = f.getName();
			if (fileName.startsWith(prefixTmpFragmentsFiles)) {
				Parameters.reportLineFlush("Deleting " + fileName);
				f.delete();
			}
		}
		
		if (!foundSortedFile) {
			for(File f : tmpFiles) {
				String fileName = f.getName();
				if (fileName.startsWith(prefixFragmentsFiles)) {
					if (foundHole) {
						Parameters.reportLineFlush("Deleting " + fileName);
						f.delete();
						continue;
					}
					String fileIndexString = fileName.substring(fileName.lastIndexOf("_")+1, fileName.lastIndexOf("."));
					int fileIndex = Integer.parseInt(fileIndexString);
					if (fileIndex==expectedIndex) expectedIndex++;
					else {
						foundHole = true;
						Parameters.reportLineFlush("Found hole: didnt find file index " + expectedIndex);
						Parameters.reportLineFlush("Deleting " + fileName);
						f.delete();
					}
				}
			}
		}			
		
		if (foundSortedFile) {
			startIndex = treebankSize;
		}
		else {
			fileCounterStart = expectedIndex-1;
			startIndex = fileCounterStart * flushToDiskEvery;
		}			
		
		if (startIndex>=treebankSize) {
			Parameters.reportLineFlush("All trees have been compared");
		}
		else {
			Parameters.reportLineFlush("Starting to compare tree from index " + startIndex);
		}
		
		Parameters.closeLogFile();		
	}
	
	/*
	public static void main(String[] args) throws Exception {
		String argsString = 
			"-partialFragments:true " +
			"-flushToDiskEvery:10 " +
			"-exactFrequencies:true " +
			"-threads:1 " +
			"-resumeDir:tmp/FragmentSeeker_Wed_Sep_15_01_14_31 " +
			"tmp/ftb_1_cleaned_first100.mrg";
		args = argsString.split("\\s+");
		main1(args);		
	}
	*/
	

	
}
