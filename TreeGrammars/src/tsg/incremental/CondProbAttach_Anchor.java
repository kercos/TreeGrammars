package tsg.incremental;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import settings.Parameters;
import tsg.TermLabel;
import tsg.incremental.CondProbAttach.CondProbAttachLex;
import util.Utility;
import util.file.FileUtil;


public abstract class CondProbAttach_Anchor extends CondProbAttach {

	static double multFactor = 5d;
	
	HashMap<TermLabel, //subSite
		HashMap<TermLabel, //anchor (first lex of previous frag, or null for base model)
			HashMap<FragFringeUnambigous, double[]>>> trainTable;
	
	HashMap<TermLabel, //fragLex 
		HashMap<TermLabel, //subSite
			HashMap<TermLabel, // anchor (first lex of previous frag, or null for base model)
				HashMap<FragFringeUnambigous, double[]>>>> parseTable;
	
	HashMap<TermLabel, //subSite
		HashMap<TermLabel, //anchor (first lex of previous frag, or null for base model)
			double[]>> logOneMinusLambdasTable; 
			
	
	public CondProbAttach_Anchor() {
		trainTable = new HashMap<TermLabel, HashMap<TermLabel,HashMap<FragFringeUnambigous,double[]>>>();
	}
	
	@Override
	public int[] totalEvents() {
		int totalEventsBasic = 0;
		int totalEventsAdvanced = 0;
		for(HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> subTable : trainTable.values()) {			
			for(Entry<TermLabel, HashMap<FragFringeUnambigous, double[]>> e : subTable.entrySet()) {
				TermLabel anchor = e.getKey();
				int size = e.getValue().size();
				if (anchor==null)
					totalEventsBasic += size;
				else
					totalEventsAdvanced +=size;
			}
		}
		return new int[]{totalEventsBasic,totalEventsAdvanced};
	}
	
	/**
	 * To run after training is finished
	 */
	public void estimateLogProbabilities() {
		computeMLE();
		//checkTable();
		convertToLog();		
		buildParseTable();
		trainTable = null; //no needed anymore
		TermLabel tl = TermLabel.getTermLabel("word", true);
		CondProbAttachLex cpl = getConProbLex(tl);
		if (cpl.hasEvents())
			Parameters.reportLine("Good entry for house");
		else 
			Parameters.reportLine("No entry for house");
		//Parameters.reportLine("Lex set in parse table: ");
		//Parameters.reportLine(parseTable.keySet().toString());
	}
	
	private void computeMLE() {
		logOneMinusLambdasTable = new HashMap<TermLabel, HashMap<TermLabel,double[]>>();
		for(Entry<TermLabel, HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>> i : trainTable.entrySet()) {
			TermLabel subSite = i.getKey();
			HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> subTable = i.getValue();
			HashMap<FragFringeUnambigous, double[]> baseTable = subTable.get(null);
			double totalSum = Utility.getSumValue(baseTable);					
			for(double[] d : baseTable.values()) {
				d[0] = d[0]/totalSum; // lambda = 1
			}
			for(Entry<TermLabel, HashMap<FragFringeUnambigous, double[]>> e : subTable.entrySet()) {
				TermLabel anchor = e.getKey();
				if (anchor==null)
					continue;
				HashMap<FragFringeUnambigous, double[]> subSubTable = e.getValue();
				double conditioningMass = Utility.getSumValue(subSubTable);
				int diversity = subSubTable.size();
				double lambda = conditioningMass / (conditioningMass + multFactor*diversity);				
				for(double[] d : subSubTable.values()) {
					d[0] = lambda * d[0]/conditioningMass;
				}
				double oneMinusLamda = 1d-lambda;
				double[] logoneMinusLamdaArray = new double[]{Math.log(oneMinusLamda)};
				Utility.putInMapDouble(logOneMinusLambdasTable, subSite, anchor, logoneMinusLamdaArray);
				/*
				for(Entry<FragFringeUnambigous, double[]> f : baseTable.entrySet()) {
					double newProb = oneMinusLamda * f.getValue()[0];
					Utility.increaseInHashMap(subSubTable, f.getKey(), newProb);
				}
				*/
			}
		}
	}
	
	private void checkTable() {
		for(HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> subTable : trainTable.values()) {
			for(HashMap<FragFringeUnambigous, double[]> subSubTable : subTable.values()) {
				double totalSum = Utility.getSumValue(subSubTable);	
				if (Math.abs(totalSum-1)>0.0001) {
					System.err.println("Error in table check - sum: " + totalSum);
				}
			}
		}
	}
	
	private void convertToLog() {
		for(HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> subTable : trainTable.values()) {
			for(HashMap<FragFringeUnambigous, double[]> subSubTable : subTable.values()) {
				for(double[] d : subSubTable.values()) {
					d[0] = Math.log(d[0]);
				}
			}
		}
	}

	private void buildParseTable() {
		
		parseTable = 
			new HashMap<TermLabel, //fragLex 
					HashMap<TermLabel, //subSite
						HashMap<TermLabel, // anchor (first lex of previous frag, or null for base model)
							HashMap<FragFringeUnambigous, double[]>>>>();
		
		for(Entry<TermLabel, HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>> e : trainTable.entrySet()) {
			TermLabel subSite = e.getKey();
			for(Entry<TermLabel, HashMap<FragFringeUnambigous, double[]>> f : e.getValue().entrySet()) {
				TermLabel anchor = f.getKey(); //possibly null
				for(Entry<FragFringeUnambigous, double[]> g : f.getValue().entrySet()) {
					FragFringeUnambigous ff = g.getKey();
					TermLabel fragLex = ff.firstLex();
					double[] d = g.getValue();
					Utility.putInMapQuadruple(parseTable, fragLex, subSite, anchor, ff, d);
				}
			}
		}		
	}

	public abstract TermLabel getCondHistory(SymbolicChartState a);
	
	public abstract TermLabel getCondHistoryBasic(FragFringeUnambigous a);
	
	public TermLabel getAnchor(SymbolicChartState a) {
		return a.fragFringe.firstLex();
	}
	


	@Override
	public void addTrainEvent(SymbolicChartState a, FragFringeUnambigous b, double d) {
		TermLabel subSite = getCondHistory(a);
		TermLabel anchor = getAnchor(a);
		Utility.increaseHashMap(trainTable, subSite, anchor, b, new double[]{d});		
		Utility.increaseHashMap(trainTable, subSite, null, b, new double[]{d});
	}
	
	@Override
	public void addTrainEventBasic(FragFringeUnambigous a) {
		TermLabel subSite = getCondHistoryBasic(a);
		Utility.putInHashMapIfNotPresent(trainTable, subSite, null, a, new double[]{1});
	}
	
	@Override
	public void addAllTrainingEvents(CondProbAttach attachModel) {
		CondProbAttach_Anchor otherModel = (CondProbAttach_Anchor)attachModel;
		Utility.increaseAllHashMapTriple(otherModel.trainTable, trainTable);		
	}

	@Override
	public CondProbAttachLex getConProbLex(TermLabel lex) {		
		return new CondProbAttachLex_Anchor(parseTable.get(lex));
	}

	public class CondProbAttachLex_Anchor extends CondProbAttachLex {

		HashMap<TermLabel, //subSite
			HashMap<TermLabel, //anchor (first lex of previous frag, or null for base model)
				HashMap<FragFringeUnambigous, double[]>>> lexTable;
		
		public CondProbAttachLex_Anchor(
				HashMap<TermLabel, HashMap<TermLabel, 
					HashMap<FragFringeUnambigous, double[]>>> lexTable) {			
			this.lexTable = lexTable;
		}
		
		@Override
		public HashMap<FragFringeUnambigous, double[]> getEventsLogProb(SymbolicChartState a) {
			TermLabel subSite = getCondHistory(a);			
			HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> subLexTable = lexTable.get(subSite);
			if (subLexTable==null)
				return null;
			TermLabel anchor = getAnchor(a);
			HashMap<FragFringeUnambigous, double[]> advTable = subLexTable.get(anchor);
			HashMap<FragFringeUnambigous, double[]> baseTable = subLexTable.get(null);
			if (advTable==null)
				return baseTable; //only base model
			double logOneMinusLambda = logOneMinusLambdasTable.get(subSite).get(anchor)[0];			
			return interpolate(advTable,baseTable,logOneMinusLambda);
		}

		private HashMap<FragFringeUnambigous, double[]> interpolate(
				HashMap<FragFringeUnambigous, double[]> advTable, 
				HashMap<FragFringeUnambigous, double[]> baseTable, 
				double logOneMinusLambda) {
			
			HashMap<FragFringeUnambigous, double[]> result = Utility.hashMapDoubleClone(advTable);
			for(Entry<FragFringeUnambigous, double[]> e : baseTable.entrySet()) {
				FragFringeUnambigous ff = e.getKey();
				double newLogProb = e.getValue()[0]+logOneMinusLambda;
				Utility.increaseInHashMapLogSum(result, ff, newLogProb);
			}
			return result;
		}

		@Override
		public boolean hasEvents() {
			return lexTable!=null;
		}
		
		@Override
		public void printToFile(File outputFile) {
			PrintWriter pw = FileUtil.getPrintWriter(outputFile);
			for(Entry<TermLabel, HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>>> i : lexTable.entrySet()) {
				TermLabel subSite = i.getKey();
				pw.println(subSite);
				HashMap<TermLabel, HashMap<FragFringeUnambigous, double[]>> subTable = i.getValue();			
				for(Entry<TermLabel, HashMap<FragFringeUnambigous, double[]>> e : subTable.entrySet()) {
					TermLabel anchor = e.getKey();
					pw.println("\t" + anchor);
					HashMap<FragFringeUnambigous, double[]> subSubTable = e.getValue();
					for(Entry<FragFringeUnambigous, double[]> f : subSubTable.entrySet()) {
						FragFringeUnambigous ff = f.getKey();
						double d = f.getValue()[0];
						pw.println("\t\t" + ff.toString() + "\t" + d);
					}
				}
			}
			pw.close();
		}
		
	}




	
}
