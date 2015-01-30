package depParsing;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.corpora.*;
import tdg.*;
import util.*;


public class Malt {
			
	
	public static void train(String dirName) {
		File trainingFile = new File(Parameters.corpusPath + "WSJ/DEPENDENCY/" + dirName + "/wsj-02-11.dep");
		ArrayList<TDNode> wsj_02_11 = DepCorpus.readTreebankFromFileYM(trainingFile, Parameters.lengthLimitTraining, true);
		File outTrainPath = new File(Parameters.outputPath + "malt.02-11.tab");
		DepCorpus.toFileMALTulab(outTrainPath, wsj_02_11, false);
		String[] argsTrainer = {
				"-w", Parameters.outputPath,
				"-c", "config",
				"-if", "malttab",
				"-of", "malttab",
				"-i", outTrainPath.toString(),
				"-lsx", "/home/fsangati/SOFTWARE/libsvm-2.86/svm-train", 
				"-a",  "nivrestandard", //	nivreeager Nivre arc-eager nivrestandard	Nivre arc-standardcovnonproj	Covington non-projectivecovproj
				"-m", "learn",
				"-d", "POSTAG", 
				"-s", "Input[0]", 
				"-T", "1000" 
		};		
		org.maltparser.Malt.main(argsTrainer);
	}
	
	public static void parse(String dirName) {
		File testFile = new File(Parameters.corpusPath + "WSJ/DEPENDENCY/" + dirName + "/wsj-22.dep");
		ArrayList<TDNode> wsj_22 = DepCorpus.readTreebankFromFileYM(testFile, Parameters.lengthLimitTest, true);
		File outGoldPath = new File(Parameters.outputPath + "malt.22.gold.tab");
		File outTestPath = new File(Parameters.outputPath + "malt.22.test");
		DepCorpus.toFileMALTulab(outGoldPath, wsj_22, false);
		DepCorpus.toFileMALTulab(outTestPath, wsj_22, true);			
		String[] argsParser = {			
				"-w", Parameters.outputPath,
				"-c", "config",
				"-of", "malttab",
				"-i", outTestPath.toString(),
				"-o", Parameters.outputPath + "malt.22.out.tab",
				"-m", "parse"
		};
		org.maltparser.Malt.main(argsParser);
	}
	
	public static void eval() {
		File testFile =  new File(Parameters.outputPath + "malt.22.out.tab");
		File goldFile = new File(Parameters.outputPath + "malt.22.gold.tab");
		File outputFile = new File(Parameters.outputPath + "UASEval.txt");
		depEval.MALTevalUAS(testFile, goldFile, outputFile);
	}
	
	public static void main(String args[]) {
		Parameters.lengthLimitTraining = 20;				
		Parameters.lengthLimitTest = 20;
		String dirName = "Entropy";	
		Parameters.outputPath = Parameters.resultsPath + "Malt/" + dirName + "/";		
		File outputPathFile = new File(Parameters.outputPath);
		outputPathFile.mkdirs();
		Parameters.logFile = new File(Parameters.outputPath + "Log");
		//args = new String[]{"train"};
		if (args[0].equals("train")) train(dirName);
		else if (args[0].equals("parse")) parse(dirName);			
		else if (args[0].equals("eval")) eval();
		//java -Xmx1024M -jar ~/Code/Malt/malt.jar -w ~/PROJECTS/Malt/ -c test_wsj00-21_collins -if malttab -i ~/CORPUS/WSJ/MALT2/wsj-02-21.tab -lsx ~/SOFTWARE/libsvm-2.86/svm-train -m learn -d POSTAG -s "Input[0]" -T 1000
		//java -jar malt.jar -c test -i examples/data/talbanken05_test.conll -o out.conll -m parse
	}
}

