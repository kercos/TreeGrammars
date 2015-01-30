package tsg.incremental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import tsg.TSNodeLabel;
import util.Utility;

public class FragSpanChartProb {

	TSNodeLabel frag;
	int yieldLenght;
	int startIndex;
	double fragProb;
	ArrayList<TSNodeLabel> terms;
	IdentityHashMap<TSNodeLabel, HashMap<Integer, ArrayList<SpanProb>>> nodeStartIndexSpanProbTable;
	
	
	public FragSpanChartProb(TSNodeLabel frag, int startIndex, double fragProb) {		
		this.frag = frag;		
		this.terms = frag.collectTerminalItems();
		this.yieldLenght = terms.size();
		this.startIndex = startIndex;
		this.fragProb = fragProb;		
		nodeStartIndexSpanProbTable = 
			new IdentityHashMap<TSNodeLabel, HashMap<Integer, ArrayList<SpanProb>>>();		
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Frag: " + frag.toStringTex(false, true) + "\n");
		sb.append("Start index: " + startIndex + "\n");
		sb.append("Frag log prob: " + fragProb + "\n");
		ArrayList<TSNodeLabel> nodes = frag.collectAllNodesBreathFirst();
		for(TSNodeLabel n : nodes) {
			HashMap<Integer, ArrayList<SpanProb>> nodeSpans = nodeStartIndexSpanProbTable.get(n);
			sb.append(n.label() + "\n");
			if (nodeSpans==null)
				sb.append("\tNULL\n");
			else {
				for(ArrayList<SpanProb> spanArray : nodeSpans.values()) {
					for(SpanProb span : spanArray) {
						sb.append("\t" + span.toString(true) + "\n");
					}
				}
			}
		}				
		return sb.toString();
	}
	
	public void addTermSpanProb(int yieldElementIndex, SpanProb tsp) {
		TSNodeLabel termNode = terms.get(yieldElementIndex);
		SpanProb sp = new SpanProb(tsp);
		Utility.putInHashMapDoubleArrayList(nodeStartIndexSpanProbTable, termNode, sp.spanStartEnd[0], sp);
	}
	
	/**
	 * 
	 * @param startSpan position at which the first element in the yield start within the sentence 
	 * @param endSpan position of the dot within the sentence
	 * @param frag
	 * @param dotPos dotPosition within the yield. Should be always > 0
	 * @param fragLogProb
	 */
	public void finalizeNodesProb(MinRiskParseBuilder mRPBuilder) {

		findInternalNodeProbs();
		
		assert checkProbs();
		
		for(Entry<TSNodeLabel, HashMap<Integer, ArrayList<SpanProb>>> 
			e : nodeStartIndexSpanProbTable.entrySet()) {
			
			TSNodeLabel cfgRuleFrame = e.getKey().cloneCfg();
			HashMap<Integer, ArrayList<SpanProb>> nodeSpan = e.getValue();
			for(ArrayList<SpanProb> spanArray : nodeSpan.values()) {
				for(SpanProb sp : spanArray) {					
					int[] span = sp.spanStartEnd;
					double prob = sp.prob + fragProb;					
					mRPBuilder.addRuleSpanProb(cfgRuleFrame, span[0], span[1]-1, prob);
				}
			}
		}
			
	}
	
	private boolean checkProbs() {
		for(HashMap<Integer, ArrayList<SpanProb>> m : nodeStartIndexSpanProbTable.values()) {
			for(ArrayList<SpanProb> a : m.values()) {
				if (SpanProb.totalLogSum(a)>0.00001) {
					return false;
				}
			}			
		}	
		return true;
	}

	private void findInternalNodeProbs() {
		ArrayList<TSNodeLabel> fragNodes = frag.collectAllNodesBreathFirst();
		Collections.reverse(fragNodes); // root last
		for(TSNodeLabel node : fragNodes) {
			if (node.isTerminal()) //already present
				continue;
			int prole = node.prole();
			TSNodeLabel firstDaughter = node.firstDaughter();
			HashMap<Integer, ArrayList<SpanProb>> spanFirstDaughter = nodeStartIndexSpanProbTable.get(firstDaughter);
			if (prole==1) {								
				for(ArrayList<SpanProb> sp1Array : spanFirstDaughter.values()) {
					for(SpanProb sp1 : sp1Array) {
						SpanProb newSp = new SpanProb(sp1.spanStartEnd, sp1.prob, new SpanProb[]{sp1});
						Utility.putInHashMapDoubleArrayList(nodeStartIndexSpanProbTable, node, 
								newSp.startIndex(), newSp);
					}					
				}
			}
			else {
				assert prole==2;
				TSNodeLabel secondDaughter = node.secondDaughter();				
				HashMap<Integer, ArrayList<SpanProb>> spanSecondDaughter = nodeStartIndexSpanProbTable.get(secondDaughter);
				for(ArrayList<SpanProb> sp1Array : spanFirstDaughter.values()) {
					for(SpanProb sp1 : sp1Array) {
						int[] sp1Span = sp1.spanStartEnd;
						Integer stopSpanFirstDaughter = sp1Span[1];
						ArrayList<SpanProb> matchinSpan = spanSecondDaughter.get(stopSpanFirstDaughter);
						if (matchinSpan==null) {
							System.err.println("PROBLEM MATCHIN SPAN");							
						}
						for(SpanProb sp2 : matchinSpan) {
							SpanProb sp3 = SpanProb.combine(sp1, sp2);
							Utility.putInHashMapDoubleArrayList(nodeStartIndexSpanProbTable, node, 
									sp3.startIndex(), sp3);
						}
					}					
				}
			}
		}
	}
	
	/*
	static class TermSpanProb {
		int[] spanStartEnd;			
		double prob;
		
		public TermSpanProb(int[] spanStartEnd, double prob) {
			this.spanStartEnd = spanStartEnd;
			this.prob = prob;
		}

		public static double totalLogSum(ArrayList<TermSpanProb> tspArray) {
			int arraySize = tspArray.size() - 1;
			double[] array = new double[arraySize];
			Iterator<TermSpanProb> iter = tspArray.iterator();
			double max = iter.next().prob;
			int i=0;
			while(iter.hasNext()) {
				double prob = iter.next().prob;
				if (prob>max) {
					array[i++] = max;
					max = prob;
				}
				else
					array[i++] = prob;
			}	
			return Utility.logSum(array, max);
		}
	}
	*/
	
	
	static class SpanProb {
		
		int[] spanStartEnd;
		SpanProb[] subSpans;
		double prob;
		
		public SpanProb(int[] spanStartEnd, double prob) {
			this.spanStartEnd = spanStartEnd;
			this.prob = prob;
		}

		
		public SpanProb(int[] spanStartEnd, double prob, SpanProb[] subSpans) {
			this(spanStartEnd, prob);
			this.subSpans = subSpans;
		}
		
		public SpanProb(SpanProb tsp) {
			assert tsp.subSpans==null;
			this.spanStartEnd = tsp.spanStartEnd;			
			this.prob = tsp.prob;
		}


		public Integer startIndex() {
			return spanStartEnd[0];
		}
		
		public Integer endIndex() {
			return spanStartEnd[1];
		}

		public boolean isNextAdjacent(SpanProb preceeding) {
			return spanStartEnd[0]==preceeding.spanStartEnd[1];
		}
		
		static SpanProb combine(SpanProb a, SpanProb b) {
			if (!b.isNextAdjacent(a))
				return null;
			double newProb = a.prob + b.prob; // if fractions should be ok
			int[] nesSpan = new int[]{a.spanStartEnd[0],b.spanStartEnd[1]};
			return new SpanProb(nesSpan, newProb, new SpanProb[]{a,b});
		}
		
		public String toString() {
			return toString(false);
		}
		
		public String toString(boolean normalProb) {
			double probPrint = normalProb ? Math.exp(prob) : prob;
			return "[" + spanStartEnd[0] + ", " + spanStartEnd[1] + "]\t" + probPrint;
		}
		
		public static double totalLogSum(ArrayList<SpanProb> tspArray) {
			int arraySize = tspArray.size() - 1;
			double[] array = new double[arraySize];
			Iterator<SpanProb> iter = tspArray.iterator();
			double max = iter.next().prob;
			int i=0;
			while(iter.hasNext()) {
				double prob = iter.next().prob;
				if (prob>max) {
					array[i++] = max;
					max = prob;
				}
				else
					array[i++] = prob;
			}	
			return Utility.logSum(array, max);
		}

	}


	
}
