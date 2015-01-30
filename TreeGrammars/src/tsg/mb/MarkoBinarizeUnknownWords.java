package tsg.mb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.ArgumentReader;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;

public class MarkoBinarizeUnknownWords {

	
	
	public static void main(String[] args) {
		
		String outputPathOption = "-outputPath:";
		String ukModelOption = "-ukModel:";
		String uknownThresholdOption = "-ukThreshold:";
		String markoBinarizerTypeOption = "-markoBinarizerType:";
		String markovH_option = "-markovH:";
		String markovV_option = "-markovV:";		
		
		String outputPath = null;
		
		String usage = "USAGE: java [-Xmx1G] tsg.mb.MarkoBinarizeUnknownWords " +
				"-outputPath:null [-markoBinarizerType:Petrov_left] [-markovH:1] [-markovV:2] " +
				"[-ukModel:English_Petrov] [-ukThreshold:-1] " +
				"treebankFile testFile";
		
		UkWordMapping.ukThreashold = -1;
		UkWordMapping ukModel = new UkWordMappingPetrov();
		
		TreeMarkoBinarization treeMarkovBinarizer = new TreeMarkoBinarizationLeft_Petrov();
		TreeMarkoBinarization.markH = 1;		
		TreeMarkoBinarization.markV = 2;
		
		//READING PARAMETERS
		if (args.length==0 || args.length>8) {
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
			else if (option.startsWith(markoBinarizerTypeOption)) {
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
				return;
			}		
		}
		
		if (outputPath==null) {
			System.err.println("OutputPath not specified.");
			return;
		}
		
		File dir = new File(outputPath);
		if (dir.exists()) {
			String dataFolder = "/MB_UK_" + FileUtil.dateTimeStringUnique() + "/";
			outputPath = dir + dataFolder;
		}
		else
			outputPath = dir + "/";
		new File(outputPath).mkdirs();
		Parameters.openLogFile(new File(outputPath + "MB_UK.log"));
		
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
		
		Parameters.reportLine("MarkoBinarize Training Treebank");
		Parameters.reportLine("Type: " + treeMarkovBinarizer.getDescription());
		Parameters.reportLine("MarkH: " + TreeMarkoBinarization.markH);
		Parameters.reportLine("MarkV: " + TreeMarkoBinarization.markV);			
		
		Parameters.reportLine("UkModel: " + ukModel.getName());
		Parameters.reportLineFlush("Uknown Word Threshold (less or equal):\t" + UkWordMapping.ukThreashold);
		
		ArrayList<TSNodeLabel> trainingTB_MB_UK = treeMarkovBinarizer.markoBinarizeTreebank(trainingTB);			
		ArrayList<TSNodeLabel> testTB_MB_UK = treeMarkovBinarizer.markoBinarizeTreebank(testTB);
		
		if (UkWordMapping.ukThreashold>0) {
			Parameters.reportLineFlush("Processing Unknown Words...");
			ukModel.init(trainingTB_MB_UK, testTB_MB_UK);
			trainingTB_MB_UK = ukModel.transformTrainingTreebank();
			testTB_MB_UK = ukModel.transformTestTreebank();
		}
		
		File transformedTrainingTB_File = new File(outputPath + trainingTB_File.getName());
		File transformedTestTB_File = new File(outputPath + testTB_File.getName());
		TSNodeLabel.printTreebankToFile(transformedTrainingTB_File, trainingTB_MB_UK, false, false);			
		TSNodeLabel.printTreebankToFile(transformedTestTB_File, testTB_MB_UK, false, false);
		Parameters.reportLineFlush("Written trainig TB with binarization and unknown words to: " + transformedTrainingTB_File);
		Parameters.reportLineFlush("Written test TB with binarization and unknown words to: " + transformedTestTB_File);
		
	
	}	
	
}
