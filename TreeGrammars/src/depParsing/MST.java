package depParsing;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import tsg.CFSG;
import tsg.TSNode;
import tsg.corpora.*;
import util.*;
import util.file.FileUtil;


public class MST {
	
	public static void main1(String args[]) {
		Parameters.lengthLimitTest = 20;
		Parameters.lengthLimitTraining = 20;
		String dirName = "COLLINS97";
		Parameters.outputPath = Parameters.resultsPath + "MST/" + dirName + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		
		File trainingFile = new File(Parameters.corpusPath + "WSJ/DEPENDENCY/" + dirName + "/wsj-02-11.dep");
		File testFile = new File(Parameters.corpusPath + "WSJ/DEPENDENCY/" + dirName + "/wsj-22.dep");
		
		ArrayList<TDNode> wsj_02_11 = DepCorpus.readTreebankFromFileYM(trainingFile, Parameters.lengthLimitTraining, true);
		ArrayList<TDNode> wsj_22 = DepCorpus.readTreebankFromFileYM(testFile, Parameters.lengthLimitTest, true);
		
		File trainOut = new File(Parameters.outputPath + "MST.02-11.ulab");
		File goldOut = new File(Parameters.outputPath + "MST.22.gold.ulab");
		File testOut = new File(Parameters.outputPath + "MST.22.test.ulab");		
		File parsedFile = new File(Parameters.outputPath + "MST.22.parsed.ulab");
		
		DepCorpus.toFileMSTulab(trainOut, wsj_02_11, false);
		DepCorpus.toFileMSTulab(goldOut, wsj_22, false);
		DepCorpus.toFileMSTulab(testOut, wsj_22, true);		
		
		String[] argsTrainTestEval = {
			"train", 
			"train-file:" + trainOut,
			"model-name:" + Parameters.outputPath + "dep.model",
			"training-iterations:" + 10,
			"decode-type:" + "proj",
			"training-k:" + 1,
			"loss-type:" + "punc",
			"create-forest:" + "true",
			"order:" + 1,
			
			"test",
			"test-file:" + testOut,
			"model-name:" + Parameters.outputPath + "dep.model",
			"output-file:" + parsedFile ,
			"decode-type:" + "proj",
			"order:" + 1,
			
			"eval",
			"gold-file:" + goldOut,
			"output-file:" + parsedFile

		};		
						
		try{
		mstparser.DependencyParser.main(argsTrainTestEval);
		} catch (Exception e) {FileUtil.handleExceptions(e);}	
		
		ArrayList<TDNode> wsj_22_parseed = 
			DepCorpus.readTreebankFromFileMST(parsedFile, Parameters.lengthLimitTest, false, true);
		File scoreFile = new File(Parameters.outputPath + "evalDep_UAS.txt");
		depEval.UASeval(wsj_22_parseed, wsj_22, scoreFile);


	}
	
	public static void wsj() {
		Parameters.lengthLimitTest = 40;
		Parameters.lengthLimitTraining = 40;
		String dirName = "COLLINS99";
		Parameters.outputPath = Parameters.resultsPath + "MST/" + dirName + "/";
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		
		File trainingFile = new File(WsjD.WsjCOLLINS99_ulab + "wsj-02-21.ulab");
		File testFile = new File(WsjD.WsjCOLLINS99_ulab + "/wsj-22.ulab");
		
		ArrayList<TDNode> wsj_02_11 = DepCorpus.readTreebankFromFileMST(trainingFile, Parameters.lengthLimitTraining, false, true);
		ArrayList<TDNode> wsj_22 = DepCorpus.readTreebankFromFileMST(testFile, Parameters.lengthLimitTest, false, true);
		
		File trainOut = new File(Parameters.outputPath + "MST.02-21.ulab");
		File goldOut = new File(Parameters.outputPath + "MST.22.gold.ulab");
		File testOut = new File(Parameters.outputPath + "MST.22.test.ulab");		
		File parsedFile = new File(Parameters.outputPath + "MST.22.parsed.ulab");
		
		DepCorpus.toFileMSTulab(trainOut, wsj_02_11, false);
		DepCorpus.toFileMSTulab(goldOut, wsj_22, false);
		DepCorpus.toFileMSTulab(testOut, wsj_22, true);		
		
		String[] argsTrainTestEval = {
			"train", 
			"train-file:" + trainOut,
			"model-name:" + Parameters.outputPath + "dep.model",
			"training-iterations:" + 10,
			"decode-type:" + "proj",
			"training-k:" + 1,
			"loss-type:" + "punc",
			"create-forest:" + "true",
			"order:" + 2,
			"format:MST",
			
			"test",
			"test-file:" + testOut,
			"model-name:" + Parameters.outputPath + "dep.model",
			"output-file:" + parsedFile ,
			"decode-type:" + "proj",
			"order:" + 2,
			
			"eval",
			"gold-file:" + goldOut,
			"output-file:" + parsedFile

		};		
						
		try{
		mstparser.DependencyParser.main(argsTrainTestEval);
		} catch (Exception e) {FileUtil.handleExceptions(e);}	
	}
	
	public static void tanl() {
		//mstparser.DependencyParser 
		//train train-file:data/train.ulab model-name:dep.model 
		//test test-file:data/test.ulab output-file:out.txt 
		//eval gold-file:data/test.ulab
		
		int order = 2;
		String projectivity = "non-proj"; //proj, non-oroj
		
		File training = new File(Parameters.corpusPath + "TANL_DEP/isst_train.evalita");
		File goldOut = new File(Parameters.corpusPath + "TANL_DEP/isst_dev.evalita");
		String outputDir = Parameters.corpusPath + "TANL_DEP/MST_0.5_order" + order + "/";
		new File(outputDir).mkdirs();
		
		File parsedFile = new File(outputDir + "isst_dev_MST_order" + order + ".parses");
		File depModel = new File(outputDir + "dep.model_order" + order);
		
		String[] argsTrainTestEval = {
				"train", 
				"train-file:" + training,
				"model-name:" + depModel,
				"training-iterations:" + 10,
				"decode-type:" + "non-proj",
				"training-k:" + 1,
				"loss-type:" + "punc",
				"create-forest:" + "true",
				"order:" + order,
				
				"test",
				"test-file:" + goldOut,
				"model-name:" + depModel,
				"output-file:" + parsedFile ,
				"decode-type:" + projectivity,
				"order:" + order,
				
				"eval",
				"gold-file:" + goldOut,
				"output-file:" + parsedFile

			};			
						
		try{
		mstparser.DependencyParser.main(argsTrainTestEval);
		} catch (Exception e) {FileUtil.handleExceptions(e);}	
	}
	
	public static void utbIt() {
		//mstparser.DependencyParser 
		//train train-file:data/train.ulab model-name:dep.model 
		//test test-file:data/test.ulab output-file:out.txt 
		//eval gold-file:data/test.ulab
		
		int order = 2;
		String projectivity = "non-proj"; //proj, non-proj
		
		//String rootPath = "/Volumes/HardDisk/Scratch/CORPORA/UniversalTreebank/langs/it/";
		String rootPath = "/gardner0/data/Corpora/UniversalTreebank/langs/it/";
		
		File training = new File(rootPath + "it-ud-train.lab");
		File goldOut = new File(rootPath + "it-ud-test.lab");
		
		File parsedFile = new File(rootPath + "it-ud-test.MST.lab");
		File depModel = new File(rootPath + "mst_dep_model_nonProj_order" + order);
		
		String[] argsTrainTestEval = {
				
				/*
				"train", 
				"train-file:" + training,
				"model-name:" + depModel,
				"training-iterations:" + 10,
				"decode-type:" + projectivity,
				"training-k:" + 1,
				"loss-type:" + "punc",
				"create-forest:" + "true",
				"order:" + order,
				"format:MST"
				*/
				
				/*
				"test",
				"test-file:" + goldOut,
				"model-name:" + depModel,
				"output-file:" + parsedFile ,
				"decode-type:" + projectivity,
				"order:" + order,
				"format:MST"
				*/
					
				
				"eval",
				"gold-file:" + goldOut,
				"output-file:" + parsedFile,
				"format:MST"
				

			};			
						
		try{
		mstparser.DependencyParser.main(argsTrainTestEval);
		} catch (Exception e) {FileUtil.handleExceptions(e);}	
	}
	
	public static void main(String[] args) {
		utbIt();
	}

}
