package tsg.metrics;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import tsg.ConstituencyWords;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.metrics.MRuleSum.ViterbiCell.FrameProbSplit;
import util.Utility;

/**
 * Max Constituency Parse
 * @author fedja
 *
 */
public class MRuleSum extends ParseMetricOptimizer{

	static Label noConstLabel = Label.getLabel("");
	//static CfgFrame noConstElement = new CfgFrame(noConstLabel, noConstLabel, noConstLabel);	
	
	//static Label negativeContrLabel = Label.getLabel("#-#");
	//static CfgFrame negativeContrElement = new CfgFrame(negativeContrLabel, noConstLabel, noConstLabel);
	
	static String unaryProductionSeparator = "==";
	static String dotBinarizationMarker = "<dot>";
		
	Hashtable<CfgFrame, double[]>[][] constsProbTable;
	double sentenceProb;
	ViterbiCell[][] cells;	
	
	//TSNodeLabel lastSeenTree;
	
	public MRuleSum() {
		super();
		this.identifier = "MRuleSum";
	}
		
	@SuppressWarnings("unchecked")
	protected void initSentence() {
		sentenceProb = 0;
		constsProbTable = new Hashtable[sentenceLength][sentenceLength];
		cells = new ViterbiCell[sentenceLength][sentenceLength];
		for(int i=0; i<sentenceLength; i++) {
			for(int j=i; j<sentenceLength; j++) {
				constsProbTable[i][j] = new Hashtable<CfgFrame, double[]>();
			}
		}
	}
	
	public void addNewDerivationChecked(TSNodeLabel tree, double prob) {
		//lastSeenTree = tree;
		sentenceProb += prob;
		tree.compressUnaryProductions(unaryProductionSeparator, false);
		tree.binarizeEarly(dotBinarizationMarker);
		ArrayList<ConstituencyWords> consts = ConstituencyWords.collectConsituencies(
				tree, true, new String[]{}, true);
		for(ConstituencyWords c : consts) {
			TSNodeLabel node = c.getNode();
			Label label = node.label;
			Label leftDaughterLabel = null;
			Label rightDaughterLabel = null;			
			if (!node.isPreLexical()) {
				leftDaughterLabel = node.daughters[0].label;
				rightDaughterLabel = node.daughters[1].label;
			}
			CfgFrame mce = new CfgFrame(label, leftDaughterLabel, rightDaughterLabel); 
			double ruleProb = label.toString().contains(dotBinarizationMarker) ? 0 : prob;
			int startIndex = c.getInitialIndex();
			int endIndex = c.getFinalIndex();
			Hashtable<CfgFrame, double[]> cellTable = constsProbTable[startIndex][endIndex];
			Utility.increaseInTableDoubleArray(cellTable, mce, ruleProb);			
		}
	}	
		
	public TSNodeLabel getBestTree() {		
		for(int span=0; span<sentenceLength; span++) {
			for(int s=0; s<sentenceLength-span; s++) {
				int e = s+span;
				
				cells[s][e] = new ViterbiCell(s,e);
				
				Hashtable<CfgFrame, double[]> cellTable = constsProbTable[s][e];
				for(Entry<CfgFrame, double[]> frameProb : cellTable.entrySet()) {
					CfgFrame gFrame = frameProb.getKey();
					double gScore = frameProb.getValue()[0];					
					double maxCombScore = 0;
					FrameProbSplit leftSplit=null, rightSplit=null;
					if (span>0) {					
						maxCombScore = -Double.MAX_VALUE;
						for(int split=s; split<e; split++) {
							ViterbiCell cellL = cells[s][split];
							if (cellL==null) continue;							
							FrameProbSplit fpsL = cellL.getMaxFrameProbSplit(gFrame.leftNodeLabel);
							if (fpsL==null) continue;
							ViterbiCell cellR = cells[split+1][e];
							if (cellR==null) continue;
							FrameProbSplit fpsR = cellR.getMaxFrameProbSplit(gFrame.rightNodeLabel);
							if (fpsR==null) continue;
							double sumScore = fpsL.prob + fpsR.prob;
							if (sumScore>maxCombScore) {
								maxCombScore = sumScore;
								leftSplit = fpsL;
								rightSplit = fpsR;
							}
						}
					}									
					maxCombScore += gScore;
					cells[s][e].addFrameProbSplit(gFrame, maxCombScore, leftSplit, rightSplit);
				}
			}
		}
		
		ViterbiCell topCell = cells[0][sentenceLength-1];
		FrameProbSplit topSplit = topCell.getMaxFrameProbSplit();
		TSNodeLabel result = getTree(topSplit);
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
				else System.out.print((cells[i][j]==null ? "null" : cells[i][j].toString()) + "\t");
			}
			System.out.println();
		}
	}
	
	private TSNodeLabel getTree(FrameProbSplit topSplit) {
		TSNodeLabel result = new TSNodeLabel(topSplit.frame.parentNodeLabel, false);
		if (topSplit.cellsSplit[0]==null) return result;
		TSNodeLabel[] daughtersArray = new TSNodeLabel[2];
		result.daughters = daughtersArray;
		int i=0;
		for(FrameProbSplit c : topSplit.cellsSplit) {
			TSNodeLabel d = getTree(c);
			d.parent = result;			
			daughtersArray[i] = d;
			i++;
		}		
		return result;
	}
	
	
	public static class CfgFrame {
		
		Label parentNodeLabel;
		Label leftNodeLabel, rightNodeLabel;
		int hashCode;
		
		public CfgFrame(Label p, Label l, Label r) {
			this.parentNodeLabel = p;
			this.leftNodeLabel = l;
			this.rightNodeLabel = r; 
		}
		
		public int hashCode() {
			return new String(parentNodeLabel + " " + 
					leftNodeLabel + " " + rightNodeLabel).hashCode();
		}
		
		public String toString() {
			return "[" + parentNodeLabel + " " + leftNodeLabel + " " + rightNodeLabel + "]";
		}
		
		public boolean equals(Object o) {
			if (o instanceof CfgFrame) {
				CfgFrame otherElement = (CfgFrame)o;
				return (otherElement.parentNodeLabel==this.parentNodeLabel &&
						otherElement.leftNodeLabel==this.leftNodeLabel &&
						otherElement.rightNodeLabel==this.rightNodeLabel);
			}
			return false;
		}
		
	}
		
	
	public class ViterbiCell {
				
		protected ArrayList<FrameProbSplit> frameProbSplitArray;
		public int startIndex, endIndex;
		//FrameProbSplit maxFrameProbSplit;
		
		public ViterbiCell(int s, int e) {
			this.startIndex = s;
			this.endIndex = e;
			frameProbSplitArray = new ArrayList<FrameProbSplit>();
		}		
		
		//public void computeMaxFrameProbSplit() {
		//	maxFrameProbSplit = getMaxFrameProbSplit();
		//}
		
		public FrameProbSplit getMaxFrameProbSplit() {
			if (frameProbSplitArray.isEmpty()) return null;
			double maxProb = -Double.MAX_VALUE;
			FrameProbSplit result = null;
			for(FrameProbSplit fps : frameProbSplitArray) {
				double prob = fps.prob;
				if (prob > maxProb) {
					maxProb = prob;
					result = fps;
				}
			}
			return result;
		}
		
		public FrameProbSplit getMaxFrameProbSplit(Label parentLabel) {
			if (frameProbSplitArray.isEmpty()) return null;
			double maxProb = -Double.MAX_VALUE;
			FrameProbSplit result = null;
			for(FrameProbSplit fps : frameProbSplitArray) {				
				if (fps.frame.parentNodeLabel!=parentLabel) continue;
				double prob = fps.prob;
				if (prob > maxProb) {
					maxProb = prob;
					result = fps;
				}
			}
			return result;
		}
		
		public void addFrameProbSplit(CfgFrame f, double s, FrameProbSplit ls, FrameProbSplit rs) {
			FrameProbSplit fps = new FrameProbSplit(f, s, new FrameProbSplit[]{ls, rs});
			frameProbSplitArray.add(fps);
		}	
		
		public String toString() {
			return "(" + startIndex + "," + endIndex + "," + frameProbSplitArray + ")";			
		}
		
		
		public class FrameProbSplit {
			CfgFrame frame;
			double prob;
			FrameProbSplit[] cellsSplit;
			
			public FrameProbSplit(CfgFrame frame, double prob, FrameProbSplit[] cellsSplit) {
				this.frame = frame;
				this.prob = prob;
				this.cellsSplit = cellsSplit;
			}
			
			public int startIndex() {
				return ViterbiCell.this.startIndex;
			}
			
			public int endIndex() {
				return ViterbiCell.this.endIndex;
			}
			

			public String toString() {				
				FrameProbSplit ls = cellsSplit[0];
				String leftId =  ls==null ? "null" : "(" + ls.startIndex() + ls.endIndex() + ")";
				FrameProbSplit rs = cellsSplit[1];
				String rightId =  rs==null ? "null" : "(" + rs.startIndex() + rs.endIndex() + ")";
				return "{" + frame + "," + prob + "," + leftId + "," + rightId + "}";
			}

		}
						
	}
	
	
	public static void main(String[] args) throws Exception {
	
		TSNodeLabel parse1 = new TSNodeLabel("(TOP (A (D a)) (B (E b) (F c)) (C (G d)))");
		TSNodeLabel parse2 = new TSNodeLabel("(TOP (A (D a)) (B (E b) (F c)) (C (H d)))");
		TSNodeLabel parse3 = new TSNodeLabel("(TOP (A (H a)) (B (E b) (F c)) (C (G d)))");
		String[] words = parse1.collectTerminalStrings().toArray(new String[]{});
		MRuleSum mrs = new MRuleSum();
		mrs.prepareNextSentence(words);
		mrs.addNewDerivation(parse1, .3);
		mrs.addNewDerivation(parse2, .3);
		mrs.addNewDerivation(parse3, .3);
		TSNodeLabel bestTree = mrs.getBestTree();
		System.out.println(bestTree);
		
	}
	
	
	
	
	
}
