package tsg.parser;

import settings.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import tsg.TSNode;
import tsg.parseEval.*;
import util.*;
import util.file.FileUtil;
import tsg.corpora.Wsj;

public class Oracle {
	
	public static void processFile(File inputFile, File goldFile, File output) {
		int sentence = 0;
		String section = "";
		Scanner scanInput = FileUtil.getScanner(inputFile);
		List<String> goldList = FileUtil.convertFileToStringList(goldFile);
		PrintWriter out = FileUtil.getPrintWriter(output);
		PrintProgressStatic.start("Oracle is processing sentence:");
		while ( scanInput.hasNextLine()) {									   				
	        String line = scanInput.nextLine();
	        if (line.indexOf("No parse for: ")==0) {	        		        	
	        	out.write(PostProcess.noParseDefault(line) + "\n");
	        	section = "";
	        	sentence++;
	        	PrintProgressStatic.next();
	        }
	        else if (line.equals("")) {
	        	if (section.equals("")) continue;	        		        		        	
	        	Set<String> kBest = processSentence(section);	        	
	        	String goldParse = goldList.get(sentence);
	        	String oracle = getOracle(kBest, goldParse);
	        	out.write(oracle  + "\n");
				section = "";
				sentence++;
	        }
	        else section += line + '\n';
		}
		PrintProgressStatic.end();
		scanInput.close();
		out.close();		
	}
	
	public static String getOracle(Set<String> kBest, String gold) {		
		float maxScore = -1;
		String oracle = "";
		TSNode TNgold = new TSNode(gold);
		for(String iBest : kBest) {
			TSNode TNi = new TSNode(iBest);
			float fscore = EvalC.getFScore(TNgold, TNi);
			if (fscore > maxScore) {
				maxScore = fscore;
				oracle = iBest;				
			}
		}
		return oracle;
	}
	
	
	public static Set<String> processSentence(String section) {
		Set<String> result = new HashSet<String>();
		section = section.trim();
		if (section.indexOf("No parse for: ")==0) {
			result.add(PostProcess.noParseDefault(section));
			return result;
		}
		String parses[] = section.split("\n");
		int i=0;
		if (parses[0].startsWith("InsideProb:")) i++;
		for(String parseTree : parses) {			
			if (parseTree.startsWith("vitprob=")) continue;			
			TSNode TN = new TSNode(parseTree);
			TN.removeUniqueInternalLabels(false);
			TN.fromNormalForm();
			if (Wsj.transformNPbasal) TN.convertTag("NPB","NP");
			if (Wsj.transformSG) TN.convertTag("SG","S");
			TN.removeRedundantRules();
			result.add(TN.toString(false, false));
		}
		return result;
	}
	
	public static void main(String args[]) {
		Parameters.setDefaultParam();
		
		/*String basePath = "/scratch/fsangati/RESULTS/TSG/LTSG/GreedyTop/Tue_Jan_27_03_05_15/";
		File inputFile = new File(basePath + "bitPar.parses");
		File goldFile = new File(basePath + "TestCorpus.gold");
		File oraceleFile = new File(basePath + "oracle.best");	
		processFile(inputFile, goldFile, oraceleFile);*/		
		
		if (args.length==3) processFile(new File(args[0]), new File(args[1]), new File(args[2]));
	}
	
}
