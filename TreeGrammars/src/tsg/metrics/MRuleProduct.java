package tsg.metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;

import tsg.ConstituencyWords;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.metrics.MRuleProduct.ViterbiCell.FrameProbSplit;
import tsg.metrics.MRuleSum.CfgFrame;
import util.Duet;
import util.Pair;
import util.Utility;

/**
 * Max Constituency Parse
 * @author fedja
 *
 */
public class MRuleProduct extends ParseMetricOptimizer{

	static Label noConstLabel = Label.getLabel("");	
	//static CfgFrame noConstElement = new CfgFrame(noConstLabel, noConstLabel, noConstLabel);	
	
	//static Label negativeContrLabel = Label.getLabel("#-#");
	//static CfgFrame negativeContrElement = new CfgFrame(negativeContrLabel, noConstLabel, noConstLabel);
	
	//static String unaryProductionSeparator = "==";
	static String dotBinarizationMarker = "<dot>";
		
	Hashtable<CfgFrame, double[]>[][] constsProbTable;
	double sentenceProb;
	ViterbiCell[][] cells;
	
	//Hashtable<TSNodeLabel, Double> debugTable; 
	
	static double minusInf = -Double.MAX_VALUE;
	
	//TSNodeLabel lastSeenTree;
	
	public MRuleProduct() {
		super();
		this.identifier = "MRuleProduct";
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
		//debugTable = new Hashtable<TSNodeLabel, Double>();
	}
	
	public void addNewDerivationChecked(TSNodeLabel tree, double prob) {
		//debugTable.put(tree, prob);
		//lastSeenTree = tree;
		sentenceProb += prob;
		//tree.compressUnaryProductions(unaryProductionSeparator, false);
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
				if (node.prole()>1) {
					rightDaughterLabel = node.daughters[1].label;
				}
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
		
		convertCellTableToLog();
		
		for(int span=0; span<sentenceLength; span++) {
			for(int s=0; s<sentenceLength-span; s++) {
				int e = s+span;								
				ViterbiCell currentViterbiCell = new ViterbiCell(s,e);
				Hashtable<CfgFrame, double[]> cellTable = constsProbTable[s][e];								
				if (span>0) {
					for(Entry<CfgFrame, double[]> frameProb : cellTable.entrySet()) {
						CfgFrame gFrame = frameProb.getKey();
						if (gFrame.rightNodeLabel==null) {
							continue;
						}	
						double gScore = frameProb.getValue()[0];					
						double maxCombScore = minusInf;
						FrameProbSplit leftSplit=null, rightSplit=null;
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
						maxCombScore += gScore;
						currentViterbiCell.addFrameProbSplit(gFrame, maxCombScore, leftSplit, rightSplit);					
					}
				}
				
				ArrayList<Duet<CfgFrame, Double>> unaryRulesProb = getUnaryRulesProb(cellTable);
				boolean updated;
				do {
					updated = false;
					for(Duet<CfgFrame, Double> frameProb : unaryRulesProb) {
						CfgFrame gFrame = frameProb.getFirst();
						double inScore = frameProb.getSecond();
						FrameProbSplit fpsL = null;
						if (gFrame.leftNodeLabel!=null) {
							fpsL = currentViterbiCell.getMaxFrameProbSplit(gFrame.leftNodeLabel);
							if (fpsL==null) {
								inScore = minusInf;
							}
							else {
								inScore += fpsL.prob;
							}
						}
						if (currentViterbiCell.updateFrameProbSplit(gFrame, inScore, fpsL, null)) {
							updated = true;
						}
					}
				} while(updated);
				
				
				cells[s][e] = currentViterbiCell;
			}
		}
		
		ViterbiCell topCell = cells[0][sentenceLength-1];
		FrameProbSplit topSplit = topCell.getMaxFrameProbSplit(topLabel);
		TSNodeLabel result = getTree(topSplit);
		result.unbinarizeEarly(dotBinarizationMarker);
		//result.uncompressUnaryProductions(unaryProductionSeparator);
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
	
	/*
	private void convertBackCellTable() {
		for(int i=0; i<sentenceLength; i++) {
			for(int j=i; j<sentenceLength; j++) {
				for(Entry<CfgFrame,double[]> e : constsProbTable[i][j].entrySet()) {
					double[] val = e.getValue();
					double logProb = val[0];
					val[0] = logProb==minusInf ? 0 : Math.exp(logProb); 					
				}				
			}
		}		
	}
	*/
	
	private ArrayList<Duet<CfgFrame, Double>> getUnaryRulesProb(
			Hashtable<CfgFrame, double[]> cellTable) {
		
		ArrayList<Duet<CfgFrame, Double>> result = new ArrayList<Duet<CfgFrame, Double>>();
		
		for(Entry<CfgFrame, double[]> frameProb : cellTable.entrySet()) {
			CfgFrame gFrame = frameProb.getKey();
			if (gFrame.rightNodeLabel==null) {
				Double gScore = frameProb.getValue()[0];
				result.add(new Duet<CfgFrame, Double>(gFrame,gScore));
			}
		}
		return result;
	}
	
	/*
	private ArrayList<Duet<CfgFrame, Double>> getOrderedUnaryRulesProb(
			Hashtable<CfgFrame, double[]> cellTable) {
		
		ArrayList<Duet<CfgFrame, Double>> result = new ArrayList<Duet<CfgFrame, Double>>();
		HashSet<CfgFrame> addedRules = new HashSet<CfgFrame>(); 
		HashSet<Label> nextRoundLeftLabels = new HashSet<Label>(); 
		int totalToAdd = 0;
		
		for(Entry<CfgFrame, double[]> frameProb : cellTable.entrySet()) {
			CfgFrame gFrame = frameProb.getKey();
			if (gFrame.rightNodeLabel==null) {
				totalToAdd++;
				if (gFrame.leftNodeLabel==null) {
					// first round only terminals
					Double gScore = frameProb.getValue()[0];
					result.add(new Duet<CfgFrame, Double>(gFrame,gScore));
					addedRules.add(gFrame);
					nextRoundLeftLabels.add(gFrame.parentNodeLabel);					
				}	
			}
		}
		while(addedRules.size()<totalToAdd) { //!nextRoundLeftLabels.isEmpty()
			HashSet<Label> tmpNextRoundLeftLabels = new HashSet<Label>();
			for(Entry<CfgFrame, double[]> frameProb : cellTable.entrySet()) {
				CfgFrame gFrame = frameProb.getKey();				 
				if (gFrame.rightNodeLabel==null) {
					Label leftNodeLabel = gFrame.leftNodeLabel;
					if (leftNodeLabel!=null) {
						if (nextRoundLeftLabels.isEmpty() || nextRoundLeftLabels.contains(leftNodeLabel)) {
							Double gScore = frameProb.getValue()[0];
							result.add(new Duet<CfgFrame, Double>(gFrame,gScore));
							addedRules.add(gFrame);
							tmpNextRoundLeftLabels.add(gFrame.parentNodeLabel);
						}											
					}
				}
			}
			nextRoundLeftLabels = tmpNextRoundLeftLabels;
		}
		
		return result;
	}
 
	 */

	private void convertCellTableToLog() {
		for(int i=0; i<sentenceLength; i++) {
			for(int j=i; j<sentenceLength; j++) {
				for(Entry<CfgFrame,double[]> e : constsProbTable[i][j].entrySet()) {
					double[] val = e.getValue();
					double logProb = val[0];
					val[0] = logProb==0 ? 0 : Math.log(logProb); 					
				}				
			}
		}
		
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
		/*
		if (topSplit==null || topSplit.frame==null) {
			printDebugTable();
		}
		*/
		Label parentLabel = topSplit.frame.parentNodeLabel;		
		TSNodeLabel result = new TSNodeLabel(parentLabel, false);
		if (topSplit.cellsSplit[0]==null) return result;
		int prole = topSplit.cellsSplit[1]==null ? 1 : 2;
		TSNodeLabel[] daughtersArray = new TSNodeLabel[prole];
		result.daughters = daughtersArray;
		int i=0;
		for(FrameProbSplit c : topSplit.cellsSplit) {
			if (c==null) continue;
			TSNodeLabel d = getTree(c);
			d.parent = result;			
			daughtersArray[i] = d;
			i++;
		}		
		return result;
	}	
	
	/*
	private void printDebugTable() {
		for(Entry<TSNodeLabel, Double> candidate : debugTable.entrySet()) {
			System.out.println(candidate.getKey() + "\t" + candidate.getValue());
		}
		
	}
	*/


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
			double maxProb = minusInf;
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
			double maxProb = minusInf;
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
		
		public boolean updateFrameProbSplit(CfgFrame f, double s, FrameProbSplit ls, FrameProbSplit rs) {
			boolean found = false;
			for(FrameProbSplit fps : frameProbSplitArray) {				
				if (fps.frame.equals(f)) {
					found = true;
					if (fps.prob>=s) return false;
					fps.prob = s;
					fps.cellsSplit[0] = ls;
					fps.cellsSplit[1] = rs;
					return true;
				}
			}
			if (!found) {
				addFrameProbSplit(f, s, ls, rs);
			}			
			return true;
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
	
		//TSNodeLabel parse1 = new TSNodeLabel("(TOP (A (C c)) (B (D d)))");
		//TSNodeLabel parse1 = new TSNodeLabel("(TOP (A (D a)) (B (E b) (F c)) (C (G d)))");
		//TSNodeLabel parse2 = new TSNodeLabel("(TOP (A (D a)) (B (E b) (F c)) (C (H d)))");
		//TSNodeLabel parse3 = new TSNodeLabel("(TOP (A (H a)) (B (E b) (F c)) (C (G d)))");
		TSNodeLabel parse1 = new TSNodeLabel("(S (NP (DT The) (NN cat)) (VP (VBP saw) (NP (DT the) (JJ yellow) (NN dog))))");
		//TSNodeLabel parse2 = new TSNodeLabel("(TOP (S (ADVP (RB Meanwhile)) (, ,) (NP (NP (NP (NNP September)) (NP (NN housing) (NNS starts))) (, ,) (ADJP (JJ due) (NP (NNP Wednesday))) (, ,)) (VP (VBP are) (VP (VBN thought) (S (VP (TO to) (VP (VB have) (VP (VBD inched) (ADVP (RB upward)))))))) (. .)))");
		//TSNodeLabel parse3 = new TSNodeLabel("(TOP (S (ADVP (RB Meanwhile)) (, ,) (NP (NNP September) (NN housing) (NNS starts) (, ,) (NP (JJ due) (NNP Wednesday)) (, ,)) (VP (VBP are) (VP (VBN thought) (S (VP (TO to) (VP (VB have) (VP (VBD inched) (ADVP (RB upward)))))))) (. .)))");

		System.out.println(parse1);
		System.out.println(parse1.toStringQtree());
		
		//TSNodeLabel parse1 = new TSNodeLabel("(TOP (S (A a) (B b)))");
		//System.out.println(parse1.toStringQtree());
		String[] words = parse1.collectTerminalStrings().toArray(new String[]{});
		MRuleProduct mrp = new MRuleProduct();
		mrp.prepareNextSentence(words);
		mrp.addNewDerivation(parse1, 0.00154321);
		//mrp.addNewDerivation(parse2, 2.80546e-45);
		//mrp.addNewDerivation(parse3, 3.01535e-48);
		//mrp.addNewDerivation(parse2, .3);
		//mrp.addNewDerivation(parse3, .3);
		TSNodeLabel bestTree = mrp.getBestTree();
		System.out.println(bestTree);
		System.out.println(bestTree.toStringQtree());
		
	}
	
	
	
	
	
}
