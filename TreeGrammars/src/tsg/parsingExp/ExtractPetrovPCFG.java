package tsg.parsingExp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.Vector;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.utils.CleanPetrov;
import util.Utility;

public class ExtractPetrovPCFG {

	ArrayList<TSNodeLabel> trainingTreebank;
	double petrovSmoothingFactor; 
	Hashtable<String, double[]> smoothedPCFG;
	String[] baseCats, refinedCats;
	int[] baseCatsFreq, refinedCatsFreq, splitsPerBaseCats;
	
	
	public ExtractPetrovPCFG(ArrayList<TSNodeLabel> trainingTreebank, double petrovSmoothingFactor) {
		Parameters.reportLineFlush("\nExtractPetrovPCFG");
		this.trainingTreebank = trainingTreebank;
		this.petrovSmoothingFactor = petrovSmoothingFactor;		
		getInternalBaseCats();
		buildSmoothedPCFG();
	}
	
	private void getInternalBaseCats() {
		TreeSet<String> baseCatsSet = new TreeSet<String>(); 
		TreeSet<String> refinedCatsSet = new TreeSet<String>();
		Hashtable<String, int[]> baseCatsFreqTable = new Hashtable<String, int[]>();
		Hashtable<String, int[]> refinedCatsFreqTable = new Hashtable<String, int[]>();
		for(TSNodeLabel t : trainingTreebank) {
			ArrayList<TSNodeLabel> intNodes = t.collectPhrasalNodes();
			for(TSNodeLabel n : intNodes) {
				String cat = n.label();				
				String baseNode = CleanPetrov.cleanPetrovLabel(cat);				
				baseCatsSet.add(baseNode);
				refinedCatsSet.add(cat);
				Utility.increaseStringIntArray(baseCatsFreqTable, baseNode);
				Utility.increaseStringIntArray(refinedCatsFreqTable, cat);
			}
		}
		baseCats = baseCatsSet.toArray(new String[]{});
		refinedCats = refinedCatsSet.toArray(new String[]{});
		baseCatsFreq = new int[baseCats.length];
		splitsPerBaseCats = new int[baseCats.length];
		refinedCatsFreq = new int[refinedCats.length];
		for(int i=0; i<baseCats.length; i++) {
			int freq = baseCatsFreqTable.get(baseCats[i])[0];
			baseCatsFreq[i] = freq;
		}
		for(int i=0; i<refinedCats.length; i++) {
			String refinedCat = refinedCats[i];			
			int freq = refinedCatsFreqTable.get(refinedCats[i])[0];
			refinedCatsFreq[i] = freq;
			String baseCat = CleanPetrov.cleanPetrovLabel(refinedCat);
			int baseCatIndex = Arrays.binarySearch(baseCats, baseCat);
			splitsPerBaseCats[baseCatIndex]++;
		}		
		Parameters.reportLine("Internal nodes base cats:");	
		reportCatFreq(baseCats, baseCatsFreq);
		Parameters.reportLine("Internal nodes refined cats:");
		reportCatFreq(refinedCats, refinedCatsFreq);
		Parameters.reportLine("Number of splits per base cats:");
		reportCatFreq(baseCats, splitsPerBaseCats);
		Parameters.reportLineFlush("");
	}
	
	
	private static void reportCatFreq(String[] cats, int[] freq) {
		for(int i=0; i<cats.length; i++) {
			Parameters.reportLine(cats[i] + "\t" + freq[i]);
		}		
	}

	private void buildSmoothedPCFG() {
				
		Parameters.reportLineFlush("Building smoothed PCFG");
		
		Vector<Hashtable<String, Double>> genericLhsRhsFreq = new Vector<Hashtable<String, Double>>();
		for(int i=0; i<baseCats.length; i++) {
			genericLhsRhsFreq.add(new Hashtable<String,Double>());
		}
		
		Hashtable<String, int[]> genericIntRulesFreq = new Hashtable<String, int[]>(); 
		for(TSNodeLabel t : trainingTreebank) {					
			ArrayList<TSNodeLabel> nodes = t.collectPhrasalNodes();
			for(TSNodeLabel n : nodes) {				
				String rule = n.cfgRule();
				String[] ruleSplit = rule.split("\\s");
				String lhsBase =  CleanPetrov.cleanPetrovLabel(ruleSplit[0]);				
				rule = lhsBase + " " + getRhs(ruleSplit);
				Utility.increaseStringIntArray(genericIntRulesFreq, rule);
			}
		}		
		
		for(Entry<String,int[]> e : genericIntRulesFreq.entrySet()) {
			String genericLhsRule = e.getKey();			
			String[] ruleSplit = genericLhsRule.split("\\s");
			String baseLhs = ruleSplit[0];
			int baseLhsIndex = Arrays.binarySearch(baseCats, baseLhs);
			String rhs = getRhs(ruleSplit);
			int freq = e.getValue()[0];
			double smoothedFreq = petrovSmoothingFactor * freq / splitsPerBaseCats[baseLhsIndex];
			genericLhsRhsFreq.get(baseLhsIndex).put(rhs, smoothedFreq);
		}
		
		smoothedPCFG = new Hashtable<String, double[]>();
		
		for(TSNodeLabel t : trainingTreebank) {					
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {				
				if (n.isLexical) continue;
				String rule = n.cfgRule();						
				Utility.increaseStringDoubleArray(smoothedPCFG, rule, 1.);				
			}
		}
		
		int refinedRules = smoothedPCFG.size();
		
		for(String cat : refinedCats) {					
			String baseLhs = CleanPetrov.cleanPetrovLabel(cat);
			int baseLhsIndex = Arrays.binarySearch(baseCats, baseLhs);
			Hashtable<String, Double> smoothedRulesFreq = genericLhsRhsFreq.get(baseLhsIndex);
			for(Entry<String,Double> e : smoothedRulesFreq.entrySet()) {
				String rhs = e.getKey();
				double smoothedFreq = e.getValue();
				String rule = cat + " " + rhs;
				Utility.increaseStringDoubleArray(smoothedPCFG, rule, smoothedFreq);
			}
		}		
		
		int cfgNumber = smoothedPCFG.size();
		Parameters.reportLine("Total CFG rules: " + cfgNumber);
		Parameters.reportLine("Total CFG present rules: " + refinedRules);
		Parameters.reportLine("Total CFG new smoothed rules: " + (cfgNumber-refinedRules));
				
	}
	
	
	private static String getRhs(String[] rule) {
		StringBuilder sb = new StringBuilder(rule[1]);
		for(int i=2; i<rule.length; i++) {
			sb.append(' ');
			sb.append(rule[i]);
		}
		return sb.toString();
	}

	public Hashtable<String, double[]> getCFGfreq() {
		return smoothedPCFG;
	}
}
