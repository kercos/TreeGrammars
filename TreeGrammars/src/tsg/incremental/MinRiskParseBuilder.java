package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import tsg.Label;
import tsg.TSNodeLabel;
import tsg.incremental.MinRiskParseBuilder.ViterbiCell;
import tsg.incremental.MinRiskParseBuilder.ViterbiCell.ViterbiRule;
import util.Utility;
import util.file.FileUtil;

public class MinRiskParseBuilder {

	int sentenceLength;
	double sentenceProb;
	RuleCell[][] ruleProbTable;
	ViterbiCell[][] prodViterbiChart; //sumViterbiChart
	ViterbiCell[][] sumViterbiChart;
	
	
	public MinRiskParseBuilder(int sentenceLength, double sentenceProb) {
		this.sentenceLength = sentenceLength;
		this.sentenceProb = sentenceProb;
		prodViterbiChart = new ViterbiCell[sentenceLength][sentenceLength];
		sumViterbiChart = new ViterbiCell[sentenceLength][sentenceLength];
		ruleProbTable = new RuleCell[sentenceLength][sentenceLength];
		for(int i=0; i<sentenceLength; i++) {
			for(int j=i; j<sentenceLength; j++) {
				//chart[i][j] = new ViterbiCell();
				ruleProbTable[i][j] = new RuleCell();				
			}
		}
	}
	
	public void addRuleSpanProb(TSNodeLabel cfgRule, int i, int j, double prob) {		
		if (cfgRule.isTerminal())
			return;
		HashMap<Label, HashMap<TSNodeLabel, double[]>> table =
			cfgRule.prole()==1 && !cfgRule.firstDaughter().isLexical ? 
					ruleProbTable[i][j].ruleTableUnaryInternals : ruleProbTable[i][j].ruleTableOthers;
		Utility.increaseInHashMapLogDouble(table, cfgRule.label, cfgRule, prob);
	}
	
	public class RuleCell {
		
		HashMap<Label, HashMap<TSNodeLabel, double[]>> ruleTableOthers = // binaries and unary leading to lexical rhs 
			new HashMap<Label, HashMap<TSNodeLabel, double[]>>();
		
		HashMap<Label, HashMap<TSNodeLabel, double[]>> ruleTableUnaryInternals = 
			new HashMap<Label, HashMap<TSNodeLabel, double[]>>();
		
		public void toStringTex(StringBuilder sb, boolean normalProb) {			
			if (ruleTableOthers.isEmpty())
				return;
			sb.append("\t\\begin{tabular}{ll}\n");
			HashSet<Label> lhsSet = new HashSet<Label>(ruleTableOthers.keySet());
			lhsSet.addAll(ruleTableUnaryInternals.keySet());
			for(Label lhs : lhsSet) {		
				HashMap<TSNodeLabel, double[]> subTableBinary = ruleTableOthers.get(lhs);
				if (subTableBinary!=null) {
					for(Entry<TSNodeLabel, double[]> f : subTableBinary.entrySet()) {
						TSNodeLabel cfgFrame = f.getKey();
						double prob = normalProb ? Math.exp(f.getValue()[0]) : f.getValue()[0];
						sb.append("\t\t" + cfgFrame.toStringTexRule() + " & " + prob + "\\\\\n");					
					}
				}
				HashMap<TSNodeLabel, double[]> subTableUnary = ruleTableUnaryInternals.get(lhs);
				if (subTableUnary!=null) {
					for(Entry<TSNodeLabel, double[]> f : subTableUnary.entrySet()) {
						TSNodeLabel cfgFrame = f.getKey();
						double prob = normalProb ? Math.exp(f.getValue()[0]) : f.getValue()[0];
						sb.append("\t\t" + cfgFrame.toStringTexRule() + " & " + prob + "\\\\\n");					
					}
				}								
			}
			sb.append("\t\\end{tabular}\n");
		}
	}
	
	public void toStringTex(File filename, boolean normalProb) {
		PrintWriter pw = FileUtil.getPrintWriter(filename);
		pw.println(toStringTex(normalProb));
		pw.close();
	}
	
	public String toStringTex(boolean normalProb) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\begin{tabular}{|" + Utility.repeat("l|", sentenceLength) + "}\n");
		sb.append("\\hline\n");
		for(int r=0; r<sentenceLength; r++) {
			for(int c=0; c<sentenceLength; c++) {
				RuleCell ruleRC = ruleProbTable[r][c];
				if (ruleRC!=null)					
					ruleRC.toStringTex(sb, normalProb);
				if (c!=sentenceLength-1)
					sb.append(" & ");
			}
			sb.append("\\\\\n");
			sb.append("\\hline\n");
		}
		sb.append("\\end{tabular}\n");
		return sb.toString();
	}
	
	public class ViterbiCell {
	
		HashMap<Label, ViterbiRule> viterbiRules = new HashMap<Label, ViterbiRule>();
		
		public class ViterbiRule {
			
			TSNodeLabel rule;
			double score;
			protected ViterbiRule[] cellsSplit;
			
			public ViterbiRule(TSNodeLabel r, double score) {
				this.rule = r;
				this.score = score;
			}
			
			public ViterbiRule(TSNodeLabel r, double score, ViterbiRule[] splits) {
				this(r,score);
				cellsSplit = splits;
			}	
		}
		
		public ViterbiRule get(Label l) {
			return viterbiRules.get(l);
		}
		
		public boolean addRule(Label l, ViterbiRule vr) {
			viterbiRules.put(l, vr);
			return true;
		}
		
		public boolean addRule(Label l, ViterbiRule vr, boolean checkIfPresentOrMaximize) {
			if (checkIfPresentOrMaximize) {
				ViterbiRule vrPresent = viterbiRules.get(l);
				if (vrPresent!=null && vrPresent.score>=vr.score)
					return false;
			}
			viterbiRules.put(l, vr);
			return true;
		}
		
		public Double getScore(Label l) {
			ViterbiRule vrPresent = viterbiRules.get(l);
			if (vrPresent==null)
				return null;
			return vrPresent.score;
		}
		
		public boolean isEmpty() {
			return viterbiRules.isEmpty();
		}
		
		public ViterbiRule getMaxViterbiRule() {
			if (isEmpty())
				return null;
			Iterator<ViterbiRule> iter = viterbiRules.values().iterator();		
			ViterbiRule maxVitRule = iter.next();
			double maxScore = maxVitRule.score;
			while(iter.hasNext()) {
				ViterbiRule next = iter.next();
				double nextScore = next.score;
				if (nextScore>maxScore) {
					maxScore = nextScore;
					maxVitRule = next;
				}
			}
			return maxVitRule;
		}
	}
	
	public TSNodeLabel[] getMinRiskParsesProdSum() {
		//this.toStringTex(new File("/tmp/chart.txt"), true);
		buildViterbiChart(true); // product
		//buildViterbiChart(false); // sum
		ViterbiCell topCellProd = prodViterbiChart[0][sentenceLength-1];
		//ViterbiCell topCellSum = sumViterbiChart[0][sentenceLength-1];
		TSNodeLabel maxRuleProd = getTree(topCellProd.getMaxViterbiRule());
		//TSNodeLabel maxRuleSum = getTree(topCellSum.getMaxViterbiRule());
		return new TSNodeLabel[]{maxRuleProd, null};			
	}
	
	private TSNodeLabel getTree(ViterbiRule vr) {
		TSNodeLabel result = vr.rule;
		if (vr.cellsSplit==null) return result;
		TSNodeLabel lowerUnaryNode = result;
		while(lowerUnaryNode.prole()==1)
			lowerUnaryNode = lowerUnaryNode.firstDaughter();
		//lowerUnaryNode had 2 dauthers
		TSNodeLabel[] daughtersArray = lowerUnaryNode.daughters;
		int i=0;
		for(ViterbiRule dRule : vr.cellsSplit) {
			TSNodeLabel d = getTree(dRule);
			d.parent = lowerUnaryNode;			
			daughtersArray[i] = d;
			i++;
		}		
		return result;
	}
	
	public void buildViterbiChart(boolean product) {	
		
		ViterbiCell[][] viterbiTable = product ? prodViterbiChart : sumViterbiChart;
		
		for(int span=0; span<sentenceLength; span++) {
			for(int s=0; s<sentenceLength-span; s++) {
				int e = s+span;
				RuleCell rc = ruleProbTable[s][e];				
				HashMap<Label, HashMap<TSNodeLabel, double[]>> ruleTableOthers = rc.ruleTableOthers;
				if (ruleTableOthers.isEmpty())
					continue;
				HashMap<Label, HashMap<TSNodeLabel, double[]>> ruleTableUnaryInternals = rc.ruleTableUnaryInternals;
				ViterbiCell vc = new ViterbiCell(); 				
				if (span==0) {
					for(Entry<Label, HashMap<TSNodeLabel, double[]>> rewriting : ruleTableOthers.entrySet()) {
						Entry<TSNodeLabel, double[]> maxLabelScore = Utility.getMaxEntry(rewriting.getValue());
						TSNodeLabel gRule = maxLabelScore.getKey();
						double totalScore = maxLabelScore.getValue()[0];
						ViterbiRule vrNew = vc.new ViterbiRule(gRule, totalScore);
						vc.addRule(gRule.label, vrNew); //always not present and unique (it's a rewriting rule)
					}	
				}
				else {														
					for(Entry<Label, HashMap<TSNodeLabel, double[]>> catBinaryRules : ruleTableOthers.entrySet()) {
						//Label lhs = catBinaryRules.getKey();
						//double maxScore = 0;
						//boolean first = true;
						for(Entry<TSNodeLabel, double[]> r : catBinaryRules.getValue().entrySet()) {
							TSNodeLabel rule = r.getKey().clone();					
							double ruleScore = r.getValue()[0];
							Label leftDaughter = rule.daughters[0].label;
							Label rightDaughter = rule.daughters[1].label;							
							for(int split=s; split<e; split++) {
								ViterbiCell cellL = prodViterbiChart[s][split];
								if (cellL==null) continue;								
								ViterbiCell cellR = prodViterbiChart[split+1][e];
								if (cellR==null) continue;
								ViterbiRule lVitRule = cellL.get(leftDaughter);
								if (lVitRule==null) continue;
								ViterbiRule rVitRule = cellR.get(rightDaughter);
								if (rVitRule==null) continue;
								double newScore = product ? ruleScore + lVitRule.score + rVitRule.score :
									Utility.logSum(new double[]{ruleScore, lVitRule.score, rVitRule.score});
								ViterbiRule vrNew = vc.new ViterbiRule(rule, newScore, 
										new ViterbiRule[]{lVitRule, rVitRule});
								vc.addRule(rule.label, vrNew, true);
							}
						}
					}
				}
				
				if (vc.isEmpty()) continue;
				viterbiTable[s][e] = vc;
				
				boolean newInserted;
				do {
					newInserted = false;
					for(Entry<Label, HashMap<TSNodeLabel, double[]>> internalTable : ruleTableUnaryInternals.entrySet()) {
						for(Entry<TSNodeLabel, double[]> internalUnary : internalTable.getValue().entrySet()) {
							TSNodeLabel rule = internalUnary.getKey().clone();
							TSNodeLabel rhs = rule.firstDaughter();
							Label rhsLabel = rhs.label;
							ViterbiRule subVitRule = vc.get(rhsLabel);
							if (subVitRule!=null) {
								double ruleScore = internalUnary.getValue()[0];
								rule.assignUniqueDaughter(subVitRule.rule.clone());
								double newScore = ruleScore + subVitRule.score; //always prod here otherwise infinite cycles
									//Utility.logSum(ruleScore, subVitRule.score); 
								ViterbiRule vrNew = vc.new ViterbiRule(rule, newScore, subVitRule.cellsSplit);
								if (vc.addRule(rule.label, vrNew, true)) {
									newInserted = true;
								}
							}
						}
					}
				} while(newInserted);
			}
		}
	
	}
	

	
	
	/*
	public class ChartCell {
				
		protected ArrayList<FrameProbSplit> frameProbSplitArray;
		public int startIndex, endIndex;
		//FrameProbSplit maxFrameProbSplit;
		
		public ChartCell(int s, int e) {
			this.startIndex = s;
			this.endIndex = e;
			frameProbSplitArray = new ArrayList<FrameProbSplit>();
		}		
		
		//public void computeMaxFrameProbSplit() {
		//	maxFrameProbSplit = getMaxFrameProbSplit();
		//}
		
		public void addFrameProbSplit(CfgFrame f, double s, FrameProbSplit ls, FrameProbSplit rs) {
			FrameProbSplit fps = new FrameProbSplit(f, s, new FrameProbSplit[]{ls, rs});
			frameProbSplitArray.add(fps);
		}

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
				return ChartCell.this.startIndex;
			}
			
			public int endIndex() {
				return ChartCell.this.endIndex;
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
	*/


	
}
