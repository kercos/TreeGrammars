package tsg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Scanner;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class Atis2 {

	public static String AtisBase = Parameters.corpusPath + "ATIS2/";
	public static File AtisOriginal = new File(AtisBase + "atis2");
	public static File AtisOriginalSentences = new File(AtisBase + "atis2_sentences");
	public static File AtisClean = new File(AtisBase + "atis2_clean.mrg");
	public static File AtisCleanNoTraces = new File(AtisBase + "atis2_clean_noTraces.mrg");
	public static File AtisCleanRightBranchingKlein = new File(AtisBase + "atis2_clean_RB_Klein.mrg");
	
	private static void makeAtisClean() throws Exception {
		Scanner reader = FileUtil.getScanner(AtisOriginal);
		PrintWriter writer = FileUtil.getPrintWriter(AtisClean);
		int lineNumber = 0;
		while(reader.hasNextLine()) {
			lineNumber++;
			String line = reader.nextLine().trim();
			if (line.length()==0) continue;		
			line = line.replaceAll("\\[tree", "");
			line = line.replaceAll("tree", "");
			//line = line.replaceAll("\\[\\]", "");
			line = line.replaceAll(",", " ");
			line = line.replaceAll("\\[", "");
			line = line.replaceAll("\\]", "");
			line = line.replaceAll("\\s+", " ");			
			line = adjustParenthesisation(line);
			TSNodeLabel tree = new TSNodeLabel(line);
			adjustLexiconAndPrelex(tree);
			writer.println(tree.toString());
			//writer.println(line);
		}
		reader.close();
		writer.close();		
	}
	
	private static String adjustParenthesisation(String line) {
		char[] charSequence = line.toCharArray();
		int length = charSequence.length;		
		BitSet charRemoveIndexes = new BitSet();
		BitSet fakeParIndexes = new BitSet();				
		boolean previousWasOpenPar = false;
		boolean firstCloseParSeries = false;
		int parIndex = 0;
		int lastOpenIndex = -1;
		for(int i=0; i<length; i++) {
			char c = charSequence[i];
			if (c=='(') {
				parIndex++;
				lastOpenIndex = i;
				firstCloseParSeries = true;
				if (previousWasOpenPar) {
					fakeParIndexes.set(parIndex);
					charRemoveIndexes.set(i);
				}
				previousWasOpenPar = true;
			}
			else {
				previousWasOpenPar = false;
				if (c==')') {
					if (fakeParIndexes.get(parIndex)) {
						fakeParIndexes.clear(parIndex);
						charRemoveIndexes.set(i);
					}
					if (firstCloseParSeries) {
						charRemoveIndexes.set(lastOpenIndex);
						charRemoveIndexes.set(i);						
					}
					firstCloseParSeries = false;
					parIndex--;
				}
			}
		}
		
		StringBuilder sb = new StringBuilder(length);
		for(int i=0; i<length; i++) {
			char c = charSequence[i];
			if (!charRemoveIndexes.get(i)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	private static void adjustLexiconAndPrelex(TSNodeLabel tree) {
		ArrayList<TSNodeLabel> lex = tree.collectLexicalItems();
		for(TSNodeLabel l : lex) {
			String label = l.label();
			String newLabel = label.substring(0, label.length()-1);
			l.relabel(newLabel);
			TSNodeLabel p = l.parent;
			String pLabel = p.label();
			p.relabel(pLabel.toUpperCase());
		}
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
		
	private static void makeAtisRightBranchingKlein() throws Exception {
		Label Xlabel = Label.getLabel("X");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(AtisClean);
		PrintWriter pw = new PrintWriter(AtisCleanRightBranchingKlein);
		for(TSNodeLabel tree : treebank) {
			ArrayList<TSNodeLabel> terminals = tree.collectPreLexicalItems();			
			TSNodeLabel rightBranching = TSNodeLabel.makeRightBranchingKlein(
					terminals, Xlabel, Xlabel);
			pw.println(rightBranching.toString());
		}
		pw.close();
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
	
	public static void main(String args[]) throws Exception {
		//makeAtisClean();
		removeTraces();
		//printPosTags();
		//checkStartingSymbols();

		//makeAtisRightBranchingKlein();
	}

	

	

	



	
}
