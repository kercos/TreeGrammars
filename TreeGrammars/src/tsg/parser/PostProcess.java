package tsg.parser;

import java.io.File;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;

import settings.Parameters;
import tsg.TSNode;
import util.*;
import util.file.FileUtil;
import tsg.*;
import tsg.corpora.Wsj;

public class PostProcess {

	public static void processFile(File inputFile, File output, boolean markHeads) {
		int sentence = 0;
		String section = "";
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter out = FileUtil.getPrintWriter(output);
		PrintProgressStatic.start("PostProcessing sentence:");
		while ( scan.hasNextLine()) {									   				
	        String line = scan.nextLine();	        
	        if (line.indexOf("No parse for: ")==0) {
	        	sentence++;
	        	PrintProgressStatic.next();
	        	out.write(noParseDefault(line) + "\n");
	        	section = "";
	        }
	        else if (line.equals("")) {
	        	if (section.equals("")) continue;
	        	sentence++;
	        	PrintProgressStatic.next();
	        	out.write(processSentence(section, markHeads) + "\n");
				section = "";
	        }
	        else section += line + '\n';
		}
		PrintProgressStatic.end();
		scan.close();
		out.close();		
	}
	
	
	public static String processSentence(String section, boolean markHeads) {
		section = section.trim();
		if (section.indexOf("No parse for: ")==0) return noParseDefault(section);
		Hashtable<String, Double> parseSet = new Hashtable<String, Double>();
		String parses[] = section.split("\n");
		int i=0;
		if (parses[0].startsWith("InsideProb:")) i++;
		for(; i<parses.length; i++) {			
			parses[i] = parses[i].replaceFirst("vitprob=", "");
			double prob = Double.parseDouble(parses[i]);
			TSNode TN = new TSNode(parses[++i]);
			TN.removeUniqueInternalLabels(markHeads);
			TN.fromNormalForm();
			if (Wsj.transformNPbasal) TN.convertTag("NPB","NP");
			if (Wsj.transformSG) TN.convertTag("SG","S");
			TN.removeRedundantRules();
			String parse = (markHeads) ? TN.toString(true, false) : TN.toString(false, false);
			Double newProb = (Double) parseSet.get(parse);
			if (newProb==null) newProb = new Double(prob);
			else newProb = new Double(newProb.doubleValue() + prob);
			parseSet.put(parse, newProb);
		}
		String maxParse = "";
		double maxProb = -1;
		for (Enumeration<String> e = parseSet.keys(); e.hasMoreElements() ;) {
			String parse = (String)e.nextElement();
			Double prob = (Double)parseSet.get(parse);
			if (prob.doubleValue()>maxProb) {
				maxProb = prob.doubleValue();
				maxParse = parse;
			}
		}
		maxParse = Utility.replaceDoubleSlash(maxParse);
		maxParse = Utility.cleanSlash(maxParse);
		return maxParse;
	}
	
	public static int[] keepCovered(File testGoldComplete, File testGoldCovered, 
			File testResultComplete, File testResultCovered) {
		Scanner scanGold = FileUtil.getScanner(testGoldComplete);
		Scanner scanResult = FileUtil.getScanner(testResultComplete);
		PrintWriter pwGold = FileUtil.getPrintWriter(testGoldCovered);
		PrintWriter pwResult = FileUtil.getPrintWriter(testResultCovered);
		int[] countCoveredTotal = new int[]{0,0};
		while(scanGold.hasNextLine() && scanResult.hasNextLine()) {			
			String lineResult = scanResult.nextLine();
			String lineGold = scanGold.nextLine();
			countCoveredTotal[1]++;
			if (lineResult.startsWith("(TOP-X")) {
				continue;
			}			
			countCoveredTotal[0]++;
			pwGold.println(lineGold);
			pwResult.println(lineResult);						
		}
		scanGold.close();
		scanResult.close();
		pwGold.close();
		pwResult.close();
		return countCoveredTotal;
	}
	
	public static String noParseDefault(String sentence) {
		String defaultIntTag = "NNP";
		String defaultPostTag = "NP";
		sentence = sentence.replaceFirst("No parse for: ", "");
		sentence = Utility.removeDoubleQuotes(sentence).trim();		
		String words[] = sentence.split(" ");
		sentence = "(TOP-X (" + defaultPostTag;
		for(int i=0; i<words.length; i++) sentence+=" (" + defaultIntTag + " " + words[i] + ") ";
		sentence = sentence.trim();
		sentence += "))";
		return sentence;
	}
	
	public static void main(String args[]) {		
		Parameters.setDefaultParam();
		
		/*Parameters.setDefaultParam();
		String basePath = "/scratch/fsangati/RESULTS/TSG/LTSG/GreedyTop/Tue_Jan_27_03_05_15/";
		File inputFile = new File(basePath + "bitPar.parses");
		File outputFile = new File(basePath + "bitPar.best");	
		processFile(inputFile, outputFile);*/
		
		if (args.length==3) processFile(new File(args[0]), new File(args[1]), Boolean.parseBoolean(args[2]));
	}
	
}
