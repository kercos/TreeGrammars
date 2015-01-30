package tsg.metrics;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Map.Entry;

import tsg.ConstituencyWords;
import tsg.Label;
import tsg.TSNodeLabel;
import util.Duet;
import util.Utility;

public class MCP_l0 extends ParseMetricOptimizer{

	static Label noConstLabel = Label.getLabel("");
	static String unaryProductionSeparator = "==";
	static String dotBinarizationMarker = "<dot>";
		
	Hashtable<Label, double[]>[][] constsProbTable;
	ViterbiCell[][] cells;	
	
	public MCP_l0() {
		super();
		this.identifier = "MCP_l=" + MCP.lambdaFormat.format(0);
	}
		
	@SuppressWarnings("unchecked")
	protected void initSentence() {
		constsProbTable = new Hashtable[sentenceLength][sentenceLength];
		cells = new ViterbiCell[sentenceLength][sentenceLength];
		for(int i=0; i<sentenceLength; i++) {
			for(int j=i; j<sentenceLength; j++) {
				constsProbTable[i][j] = new Hashtable<Label, double[]>();
			}
		}
	}
	
	public void addNewDerivationChecked(TSNodeLabel tree, double prob) {
		tree.compressUnaryProductions(unaryProductionSeparator, false);
		tree.binarizeEarly(dotBinarizationMarker);
		ArrayList<ConstituencyWords> consts = ConstituencyWords.collectConsituencies(
				tree, true, new String[]{}, true);
		for(ConstituencyWords c : consts) {			
			Label label = c.getNode().label;
			double ruleProb = label.toString().contains(dotBinarizationMarker) ? 0 : prob;
			int startIndex = c.getInitialIndex();
			int endIndex = c.getFinalIndex();
			Hashtable<Label, double[]> cellTable = constsProbTable[startIndex][endIndex];
			Utility.increaseInTableDoubleArray(cellTable, label, ruleProb);
		}
	}	
		
	public TSNodeLabel getBestTree() {		
		for(int span=0; span<sentenceLength; span++) {
			for(int s=0; s<sentenceLength-span; s++) {
				int e = s+span;
				Hashtable<Label, double[]> cellTable = constsProbTable[s][e];
				Duet<Label, Double> maxLabelScore = getMaxLabelScore(cellTable);				
				double maxCombScore = 0;
				ViterbiCell leftSplit=null, rightSplit=null;
				if (span>0) {
					maxCombScore = -Double.MAX_VALUE;
					for(int split=s; split<e; split++) {
						ViterbiCell cellL = cells[s][split];
						if (cellL.label==noConstLabel) continue;
						ViterbiCell cellR = cells[split+1][e];	
						if (cellR.label==noConstLabel) continue;
						double sumScore = cellL.score + cellR.score;
						if (sumScore>maxCombScore) {
							maxCombScore = sumScore;
							leftSplit = cellL;
							rightSplit = cellR;
						}
					}
				}
				Label gLabel = maxLabelScore.getFirst();
				maxCombScore += maxLabelScore.getSecond();
				cells[s][e] = new ViterbiCell(gLabel, s, e, maxCombScore, leftSplit, rightSplit);
			}
		}
		
		ViterbiCell topCell = cells[0][sentenceLength-1];
		TSNodeLabel result = getTree(topCell);
		result.unbinarizeEarly(dotBinarizationMarker);
		result.uncompressUnaryProductions(unaryProductionSeparator);
		ArrayList<TSNodeLabel> terms = result.collectTerminalItems();
		int i=0;
		for(TSNodeLabel t : terms) {
			TSNodeLabel lexNode = new TSNodeLabel(lexicalItems[i], true); 
			t.daughters = new TSNodeLabel[]{lexNode};
			lexNode.parent = t;
			i++;
		}
		return result;		
	}
	
	public void printBestCellLabels() {
		for(int i=0; i<sentenceLength; i++) {			
			for(int j=0; j<sentenceLength; j++) {
				if (j<i) System.out.print("\t");
				else System.out.print("\"" + (cells[i][j]==null ? "null" : cells[i][j].label) + "\"" + "\t");
			}
			System.out.println();
		}
	}
	
	private void addDaughters(ViterbiCell cell, ArrayList<ViterbiCell> daughters) {
		for(ViterbiCell split : cell.cellsSplit) {
			if (split.label==noConstLabel) {
				addDaughters(split, daughters);
			}
			else daughters.add(split);
		}		
	}
	
	private TSNodeLabel getTree(ViterbiCell topCell) {
		TSNodeLabel result = new TSNodeLabel(topCell.label, false);
		if (topCell.cellsSplit[0]==null) return result;
		ArrayList<ViterbiCell> daughters = new ArrayList<ViterbiCell>();
		addDaughters(topCell, daughters);		
		TSNodeLabel[] daughtersArray = new TSNodeLabel[daughters.size()];
		result.daughters = daughtersArray;
		int i=0;
		for(ViterbiCell c : daughters) {
			TSNodeLabel d = getTree(c);
			d.parent = result;			
			daughtersArray[i] = d;
			i++;
		}		
		return result;
	}
	
	private static Duet<Label, Double> getMaxLabelScore(Hashtable<Label, double[]> table) {
		if (table.isEmpty()) return new Duet<Label, Double>(noConstLabel, 0d);
		double maxScore = -Double.MAX_VALUE;
		Label maxKey = null;
		for(Entry<Label, double[]> e : table.entrySet()) {
			double stat = e.getValue()[0];
			if (stat>maxScore) {
				maxScore = stat;
				maxKey = e.getKey();
			}
		}
		return new Duet<Label, Double>(maxKey, maxScore);
	}
		
	
	protected class ViterbiCell {
				
		protected double score;
		protected Label label;
		protected ViterbiCell[] cellsSplit;
		//int startIndex, endIndex;
		
		public ViterbiCell(Label l, int s, int e, double score, ViterbiCell leftSplit, ViterbiCell rightSplit) {
			this.label = l;
			this.score = score;
			//this.startIndex = s;
			//this.endIndex = e;
			cellsSplit = new ViterbiCell[]{leftSplit, rightSplit};
		}	
		
		public String toString() {
			//return "(" + label + "," + startIndex + "," + endIndex + "," + score + ")";			
			return "(" + label + ","  + score + ")";
		}
				
		
		
	}
	
	
	public static void main(String[] args) throws Exception {
		//MCP mcp = new MCP(0d);
		MCP_l0 mcp = new MCP_l0(); 
		File testFile = new File("tmp/wsj-24_214_215.mrg");		
		File nBestFile = new File("tmp/Parsing_Thu_Oct_21_01_29_54/BitParWorkingDir/outputBitPar_1000best_1.txt_cleaned");
		Scanner scan = new Scanner(testFile);		
		String firstTreeString = scan.nextLine();
		TSNodeLabel tree = new TSNodeLabel(firstTreeString);
		String[] words = tree.collectTerminalStrings().toArray(new String[]{});		
		mcp.prepareNextSentence(words);
		scan = new Scanner(nBestFile);
		double lastProb = 0d;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length()==0) continue;
			if (line.startsWith("vitprob=")) {
				lastProb = Double.parseDouble(line.substring(8));
				continue;
			}
			tree = new TSNodeLabel(line);
			mcp.addNewDerivation(tree, lastProb);			
		}
		TSNodeLabel bestTree = mcp.getBestTree(); 
		System.out.println(bestTree);
		int lexItemsNumber = bestTree.countLexicalNodes();
		System.out.println(words.length);
		System.out.println(lexItemsNumber);
		System.out.println(lexItemsNumber==words.length);		
	}
	
	
}
