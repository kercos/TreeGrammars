package tsg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class Atis3 {

	public static String AtisBase = Parameters.corpusPath + "ATIS3/";
	public static File AtisOriginal = new File(AtisBase + "atis3.mrg");
	public static File AtisOriginalPrd = new File(AtisBase + "atis3.prd");
	public static File AtisReadable = new File(AtisBase + "atis3_readable.mrg");
	public static File AtisReadablePrd = new File(AtisBase + "atis3_readable.prd");
	public static File AtisClean = new File(AtisBase + "atis3_clean.mrg");
	public static File AtisCleanNoTraces = new File(AtisBase + "atis3_clean_noTraces.mrg");
	public static File AtisTopClean = new File(AtisBase + "atis3_top_clean.mrg");
	public static File AtisTopCleanRightBranchingKlein = new File(AtisBase + "atis3_top_clean_RB_Klein.mrg");
	public static File AtisReadableRightBranchingKlein = new File(AtisBase + "atis3_readable_RB_Klein.mrg");
	
	private static void makeAtisReadable() throws Exception {
		Scanner reader = FileUtil.getScanner(AtisOriginalPrd);
		PrintWriter writer = FileUtil.getPrintWriter(AtisReadablePrd);
		int parenthesis = 0;
		String sentence = "";
		int lineNumber = 0;
		while(reader.hasNextLine()) {
			lineNumber++;
			String line = reader.nextLine();
			if (line.length()==0) continue;
			if (line.startsWith("(  @")) continue;
			if (line.equals("(  END_OF_TEXT_UNIT)")) continue;			
			parenthesis += Utility.countParenthesis(line);
			sentence += line;
			if (parenthesis==0) {
				if (line.length()==0) continue;
				sentence = sentence.trim();								
				sentence = sentence.replaceAll("\n", "");
				sentence = sentence.replaceAll("\\s+", " ");
				sentence = ConstCorpus.adjustParenthesisation(sentence);
				TSNodeLabel tree = new TSNodeLabel(sentence);
				//adjustLexicon(tree);
				writer.println(tree.toString());
				sentence = "";
			}				
		}
		reader.close();
		writer.close();		
	}
	
	private static void adjustLexicon(TSNodeLabel tree) {
		ArrayList<TSNodeLabel> lex = tree.collectLexicalItems();
		for(TSNodeLabel l : lex) {
			String currentLabel = l.label();
			int slashes = Utility.countCharInString(currentLabel, '/');
			if (slashes!=1) {
				System.err.println("Slash(es) problem: " + currentLabel);
			}
			String[] LexPos = currentLabel.split("/");
			l.relabel(LexPos[1]);
			l.isLexical = false;
			TSNodeLabel lexDaughter = new TSNodeLabel(Label.getLabel(LexPos[0]), true);
			l.daughters = new TSNodeLabel[]{lexDaughter};
		}
	}
	
	private static void makeAtisTopClean() throws Exception {		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(AtisReadable);
		PrintWriter pw = new PrintWriter(AtisTopClean);
		for(TSNodeLabel tree : treebank) {
			tree.pruneSubTrees("XXX");
			tree.removeNumberInLabels();
			tree.removeRedundantRules();	
			tree = tree.addTop();
			pw.println(tree.toString());
		}				
		pw.close();		
	}
	
	private static void makeAtisClean() throws Exception {		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(AtisReadable);
		PrintWriter pw = new PrintWriter(AtisClean);
		for(TSNodeLabel tree : treebank) {
			//tree.pruneSubTrees("XXX");
			tree.removeNumberInLabels();
			tree.removeRedundantRules();	
			//tree = tree.addTop();
			pw.println(tree.toString());
		}				
		pw.close();		
	}
	
	private static void makeAtisRightBranchingKlein() throws Exception {
		Label Xlabel = Label.getLabel("X");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(AtisTopClean);
		PrintWriter pw = new PrintWriter(AtisTopCleanRightBranchingKlein);
		for(TSNodeLabel tree : treebank) {
			ArrayList<TSNodeLabel> terminals = tree.collectPreLexicalItems();			
			TSNodeLabel rightBranching = TSNodeLabel.makeRightBranchingKlein(
					terminals, Xlabel, Xlabel);
			pw.println(rightBranching.toString());
		}
		pw.close();
	}
	
	private static void checkStartingSymbols() throws Exception {
		HashSet<String> startinSimbolsSet = new HashSet<String>(); 
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(AtisReadable);
		for(TSNodeLabel tree : treebank) {
			startinSimbolsSet.add(tree.label());
		}
		System.out.println(startinSimbolsSet);		
	}
	
	private static void printPosTags() throws Exception {
		HashSet<String> posSet = new HashSet<String>(); 
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(AtisClean);
		for(TSNodeLabel tree : treebank) {
			ArrayList<TSNodeLabel> pos = tree.collectPreLexicalItems();
			for(TSNodeLabel p : pos) {
				posSet.add(p.label());
			}
		}
		System.out.println(posSet);		
	}
	
	private static void removeTraces() throws Exception {		
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(AtisClean);
		PrintWriter pw = new PrintWriter(AtisCleanNoTraces);
		for(TSNodeLabel tree : treebank) {
			tree.pruneSubTrees("XXX");
			//tree.removeNumberInLabels();
			//tree.removeRedundantRules();	
			//tree = tree.addTop();
			pw.println(tree.toString());
		}				
		pw.close();		
	}
	
	public static void main(String args[]) throws Exception {
		//makeAtisReadable();
		//makeAtisTopClean();
		//makeAtisClean();
		//checkStartingSymbols();
		//makeAtisRightBranchingKlein();
		//printPosTags();
		removeTraces();
	}

	

	

	



	
}
