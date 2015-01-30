package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class PrepareGrammarForMJcky {

	private static void printTable(
			HashMap<String, HashMap<TSNodeLabel, int[]>> table,
			File tableFile) {
		
		PrintWriter pw = FileUtil.getPrintWriter(tableFile);
			
		for(Entry<String, HashMap<TSNodeLabel, int[]>> e : table.entrySet()) {			
			pw.println(e.getKey() + "\t" + Utility.getMaxKeyInt(e.getValue()));
		}
		
		pw.close();		
	}
	
	private static HashMap<String, TSNodeLabel> readTable(File tableFile) throws Exception {
		HashMap<String, TSNodeLabel> result = new HashMap<String, TSNodeLabel>();
		Scanner scan = FileUtil.getScanner(tableFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			String rule = lineSplit[0];
			TSNodeLabel frag = new TSNodeLabel(lineSplit[1]);
			result.put(rule, frag);
		}
		return result;
	}
			
	
	private static void printGrammar(
			String startRule, HashMap<String, int[]> simpleRules, 
			HashMap<String, HashMap<TSNodeLabel, int[]>> table, File outputFile) {
		
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		pw.println(startRule);
		for(Entry<String, int[]> e : simpleRules.entrySet()) {
			pw.println(e.getValue()[0] + " " + e.getKey());
		}
		
		for(Entry<String, HashMap<TSNodeLabel, int[]>> e : table.entrySet()) {
			int maxFreq = Utility.getMaxValueInt(e.getValue());
			pw.println(maxFreq + " " + e.getKey());
		}
		
		pw.close();
	}
	
	public static void fragsToGrammar(File sortedGrammar, File mjCkyGrammar, File tableFile) throws Exception {
		String startSymbol = "Word";		
		
		Scanner scan = FileUtil.getScanner(sortedGrammar);
		HashMap<String, HashMap<TSNodeLabel, int[]>> table = 
			new HashMap<String, HashMap<TSNodeLabel, int[]>>();
		HashMap<String, int[]> simpleRules = new HashMap<String, int[]>();
		String startFreqRule = null;
		int lineNumber = 0;
		while(scan.hasNextLine()) {
			lineNumber++;
			String line = scan.nextLine();			
			String[] split = line.split("\\s", 2);
			int freq = Integer.parseInt(split[0]);
			String fragString = split[1];
			if (fragString.indexOf("-->")!=-1) {
				if (startFreqRule==null && fragString.startsWith(startSymbol))
					startFreqRule = freq + " " + fragString;
				else
					simpleRules.put(fragString, new int[]{freq});				
			}
			else {
				TSNodeLabel frag = new TSNodeLabel(fragString);
				ArrayList<TSNodeLabel> yield = frag.collectLexicalItems();
				StringBuilder rule = new StringBuilder();
				String root = frag.label();
				rule.append(root).append(" --> ");
				Iterator<TSNodeLabel> iter = yield.iterator();
				while(iter.hasNext()) {
					rule.append(iter.next().label());
					if (iter.hasNext())
						rule.append(" ");
				}						
				Utility.increaseInHashMap(table, rule.toString(), frag, freq);
			}			
		}
		
		
		printGrammar(startFreqRule, simpleRules, table, mjCkyGrammar);
		printTable(table, tableFile);	
	}
	
	
	private static TSNodeLabel replaceRulesWithFragments(TSNodeLabel node, 
			HashMap<String, TSNodeLabel> table) {
		
		if (node.isLexical)
			return node.clone();
		StringBuilder rule = new StringBuilder();
		rule.append(node.label()).append(" -->");
		for(TSNodeLabel d : node.daughters) {
			rule.append(" ").append(d.label());
		}
		TSNodeLabel frag = table.get(rule.toString());
		if (frag!=null)
			return frag.clone();
		TSNodeLabel result = node.cloneOneLevel(true);
		int prole = node.prole();
		TSNodeLabel[] daughters = new TSNodeLabel[prole];
		for(int i=0; i<prole; i++) {
			daughters[i] = replaceRulesWithFragments(node.daughters[i],table);
		}
		result.assignDaughters(daughters);
		return result;
	}	
	
	private static void postProcess(File tableFile, File parsedFile,
			File postProcessParsed) throws Exception {
		
		HashMap<String, TSNodeLabel> table = readTable(tableFile);
		Scanner scan = FileUtil.getScanner(parsedFile);
		PrintWriter pw = FileUtil.getPrintWriter(postProcessParsed);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			TSNodeLabel postProcessedTree = replaceRulesWithFragments(tree,table);
			pw.println(postProcessedTree.toString());
		}
		pw.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		String usage = "USAGE: java [-Xmx1G] -jar AGP.jar " +
				"PRE|POST inputFile tableFile outputFile";
		
		if (args.length!=4) {
			System.err.println("Wrong number of arguments.");
			System.err.println(usage);
			return;
		}
		
		String step = args[0];
		File inputFile = new File(args[1]);
		File tableFile = new File(args[2]);
		File outputFile = new File(args[3]);
		if (step.toUpperCase().equals("PRE")) {
			fragsToGrammar(inputFile, outputFile, tableFile);
		}
		else if (step.toUpperCase().equals("POST")){
			postProcess(tableFile, inputFile, outputFile);
		}
		else {
			System.err.println("First argument non recognized: " + step);
			System.err.println(usage);
		}
		
		/*
		String path = "/disk/scratch/fsangati/AdaptorGrammar/";
		File sortedGrammar = new File(path + "sorted_gram_utf8");
		File mjCkyGrammar = new File(path + "mj_cyk_grammar");
		File tableFile = new File(path + "mj_cyk_grammar_table");
		File parsedFile = new File(path + "cky_parse");
		File postProcessParsed = new File(path + "cky_parse_final");
		fragsToGrammar(sortedGrammar, mjCkyGrammar, tableFile);
		postProcess(tableFile, parsedFile, postProcessParsed);
		*/
		
	}






	
}
