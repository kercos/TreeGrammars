package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class UnseenCfgExtractor {
		
	HashSet<TSNodeLabel> cfgRulesSetFragment = new HashSet<TSNodeLabel>();
	HashSet<TSNodeLabel> missingCfgRulesSetTB = new HashSet<TSNodeLabel>();
	File outputFile;

	public UnseenCfgExtractor(File trainTB, File fragmentFile, File outputFile) throws Exception {
		this.outputFile = outputFile;
		//printGrammars();
		readCfgFragments(fragmentFile);		
		checkCfgTB(trainTB);
		if (outputFile!=null)
			printMissedRules();
	}
	
	private void printMissedRules() {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for(TSNodeLabel r : missingCfgRulesSetTB) {
			pw.println(r.toString(false, true) + "\t" + 1);
		}
		pw.close();
	}

	private void readCfgFragments(File file) throws Exception {
		System.out.println("Reading fragment file " + file);
		Scanner scan = FileUtil.getScanner(file);
		while(scan.hasNextLine()) {
			String line = scan.nextLine().split("\t")[0];
			TSNodeLabel frag = new TSNodeLabel(line, false);
			if (frag.maxDepth()>1)
				continue;
			cfgRulesSetFragment.add(frag);
		}	
		System.out.println("Total extracted rules: " + cfgRulesSetFragment.size());
	}
	
	private void checkCfgTB(File file) throws Exception {
		System.out.println("Reading Treebank file " + file);
		HashSet<TSNodeLabel> cfgRulesSetTB = new HashSet<TSNodeLabel>();
		Scanner scan = FileUtil.getScanner(file);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			ArrayList<TSNodeLabel> nonLexNode = tree.collectNonLexicalNodes();
			for(TSNodeLabel n : nonLexNode) {
				TSNodeLabel cfgRule = n.cloneOneLevel();
				cfgRulesSetTB.add(cfgRule);
				if (!cfgRulesSetFragment.contains(cfgRule))
					missingCfgRulesSetTB.add(cfgRule);
			}
		}	
		System.out.println("Total extracted rules: " + cfgRulesSetTB.size());
		System.out.println("Total rules missing in fragments file: " + missingCfgRulesSetTB.size());
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File trainTB = new File(args[0]);
		File fragmentFile = new File(args[1]);
		File outputFile = args.length>2 ? new File(args[2]) : null;
		new UnseenCfgExtractor(trainTB, fragmentFile, outputFile);
	}

}
