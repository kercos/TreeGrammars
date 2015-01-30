package tsg.metrics;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import tsg.ConstituencyWords;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.metrics.MCP_l0.ViterbiCell;
import util.Duet;
import util.Utility;

/**
 * Max Constituency Parse
 * @author fedja
 *
 */
public class MCP extends ParseMetricOptimizer{

	public static final DecimalFormat lambdaFormat = new DecimalFormat("0.00");
	
	static Label noConstLabel = Label.getLabel("");
	static Label negativeConstLabel = Label.getLabel("#-#");
	static String unaryProductionSeparator = "==";
	static String dotBinarizationMarker = "<dot>";
		
	Hashtable<Label, double[]>[][] constsProbTable;
	double sentenceProb;
	ViterbiCell[][] cells;	
	double lambda;
	
	public MCP(double lambda) {
		super();
		this.lambda = lambda;
		this.identifier = "MCP_l=" + lambdaFormat.format(lambda);
	}
		
	@SuppressWarnings("unchecked")
	protected void initSentence() {
		sentenceProb = 0;
		constsProbTable = new Hashtable[sentenceLength][sentenceLength];
		cells = new ViterbiCell[sentenceLength][sentenceLength];
		for(int i=0; i<sentenceLength; i++) {
			for(int j=i; j<sentenceLength; j++) {
				constsProbTable[i][j] = new Hashtable<Label, double[]>();
			}
		}
	}
	
	public void addNewDerivationChecked(TSNodeLabel tree, double prob) {
		sentenceProb += prob;
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
				double gScore = maxLabelScore.getSecond()/sentenceProb;
				double contribution = gScore - lambda * (1-gScore);
				boolean isNegativeContribution = contribution<0;
				if (isNegativeContribution) {
					Label l = (span==0 || span==sentenceLength-1) ? gLabel : negativeConstLabel;
					cells[s][e] = new ViterbiCell(l, s, e, maxCombScore, leftSplit, rightSplit);
				}
				else {
					maxCombScore += contribution;
					cells[s][e] = new ViterbiCell(gLabel, s, e, maxCombScore, leftSplit, rightSplit);
				}
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
			Label l = split.label;
			if (l==noConstLabel || l==negativeConstLabel) {
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
		int startIndex, endIndex;
		
		public ViterbiCell(Label l, int s, int e, double score, ViterbiCell leftSplit, ViterbiCell rightSplit) {
			this.label = l;
			this.score = score;
			this.startIndex = s;
			this.endIndex = e;
			cellsSplit = new ViterbiCell[]{leftSplit, rightSplit};
		}	
		
		public String toString() {
			return "(" + label + "," + startIndex + "," + endIndex + "," + score + ")";			
		}
				
		
		
	}
	
	
}
