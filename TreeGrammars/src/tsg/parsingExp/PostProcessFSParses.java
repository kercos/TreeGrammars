package tsg.parsingExp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import tsg.mb.TreeMarkoBinarization;
import tsg.mb.TreeMarkoBinarizationLeft_LC;
import tsg.mb.TreeMarkoBinarizationLeft_Petrov;
import tsg.metrics.MetricOptimizerArray;
import tsg.parseEval.EvalB;
import tsg.parseEval.EvalC;
import tsg.utils.CleanPetrov;
import util.ArgumentReader;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;
import wordModel.UkWordMapping;
import wordModel.UkWordMappingPetrov;
import wordModel.WordFeatures;

public class PostProcessFSParses {
	
	final static String lexPosSeparationString = "^";
	final static char lexPosSeparationChar = lexPosSeparationString.charAt(0);
	
	final static String internalFakeNodeLabel = "NODE@";
	final static int internalFakeNodeLabelLength = 5;
	final static String fakePrelexPrefix = "^";		
	
	static TreeMarkoBinarizationLeft_Petrov treeMarkovBinarizer;
	static Vector<TSNodeLabel> ambiguousCFGmapping;
	static Hashtable<String, TSNodeLabel> unambiguousCFGmapping;
	
	public static TSNodeLabel postProcessParseTree(TSNodeLabel tree) {		
		tree = replaceRulesWithFragments(tree);
		tree = treeMarkovBinarizer.undoMarkovBinarization(tree);
		return tree;
	}
	
	private static TSNodeLabel replaceRulesWithFragments(TSNodeLabel tree) {
		String firstDaughetLabel = tree.firstDaughter().label(); 
		boolean ambiguousFragment = firstDaughetLabel.startsWith(internalFakeNodeLabel);
		TSNodeLabel fragment = null;
		
		if (ambiguousFragment) {
			int index = 0;
			try {
				String digits = firstDaughetLabel.substring(internalFakeNodeLabelLength);
				index = Integer.parseInt(digits);
			}
			catch(StringIndexOutOfBoundsException e) {
				Parameters.reportLineFlush("Error302: " + firstDaughetLabel + " " + tree);				
				return null;
			}
			fragment = ambiguousCFGmapping.get(index);
			if (fragment==null) {
				Parameters.reportError("Couldn't find the fragment uniquely associated with unique index:" + index);
				return null;
			}
			tree.daughters = tree.firstDaughter().daughters;
		}
		else {
			fragment = unambiguousCFGmapping.get(tree.cfgRule());			
		}

		TSNodeLabel result = fragment.clone();
		ArrayList<TSNodeLabel> terminals = result.collectTerminalItems();
		Iterator<TSNodeLabel> termIter = terminals.iterator();
		for(TSNodeLabel d : tree.daughters) {
			TSNodeLabel term = termIter.next();
			if (term.isLexical) continue;			
			TSNodeLabel subFragment = replaceRulesWithFragments(d);			
			term.daughters = subFragment.daughters;
			for(TSNodeLabel d1 : subFragment.daughters) {
				d1.parent = subFragment;
			}			
		}
		return result;
	}	
	

	
	public static void main(String[] args) throws Exception {		
				
		TreeMarkoBinarization.markH = 1;
		TreeMarkoBinarization.markV = 2;
		treeMarkovBinarizer = new TreeMarkoBinarizationLeft_Petrov();
		
		File fragmentsAndCfg = ArgumentReader.readFileOptionNoSeparation(args[0]);
		File inputFile = ArgumentReader.readFileOptionNoSeparation(args[1]);
		File outputFile = ArgumentReader.readFileOptionNoSeparation(args[2]);		
		
		ConvertFragmentsToCFGRulesInt converter = 
			new ConvertFragmentsToCFGRulesInt(fragmentsAndCfg, new File("ambiguousFragmetnsCFG_bis.txt"), 
					new File("bitpar_grammar_bis.txt"), internalFakeNodeLabel, fakePrelexPrefix);		
				
		ambiguousCFGmapping = converter.getAmbiguousCFGmapping();
		unambiguousCFGmapping = converter.getunambiguousCFGmapping();
		
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();			
			TSNodeLabel tree = postProcessParseTree(new TSNodeLabel(line));
			pw.println(tree.toString());
		}
		
		pw.close();		
		
	}
	
	
}
