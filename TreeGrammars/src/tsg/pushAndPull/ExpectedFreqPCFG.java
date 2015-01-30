package tsg.pushAndPull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Scanner;

import Jama.Matrix;

import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;

public class ExpectedFreqPCFG {

	static Label startSymbol = Label.getLabel("S");
	
	HashMap<Label, HashMap<TSNodeLabel, Double>> ruleTable;
	Label[] nonTerms;
	
	public ExpectedFreqPCFG(File inputFile) throws Exception {
		ruleTable = new HashMap<Label, HashMap<TSNodeLabel, Double>>();
		readFile(inputFile);	
		nonTerms = ruleTable.keySet().toArray(new Label[]{});
	}
	
	private void readFile(File inputFile) throws Exception {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.isEmpty())
				continue;
			String[] parts = line.split("\t");
			TSNodeLabel rule = new TSNodeLabel(parts[0], false);
			Double freq = Double.parseDouble(parts[1]);
			Utility.putInMapDouble(ruleTable, rule.label, rule, freq);
			
		}
		
	}
	
	public ArrayList<TSNodeLabel> generateRandomTreebank(int size) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(size);
		for(int i=0; i<size; i++) {
			result.add(generateRandomTree());
		}
		return result;
	}
	
	public void analyzeRandomTreebankFreq(int size, boolean avoidRepetitions) {
		System.out.println("Generating random trees: " + size);
		HashSet<TSNodeLabel> treebank = new HashSet<TSNodeLabel>(); 
		HashMap<Label, int[]> nonTermFreq = new HashMap<Label, int[]>(); 
		HashMap<String, int[]> rulesFreq = new HashMap<String, int[]>();
		HashMap<Label, int[]> nonTermPres = new HashMap<Label, int[]>(); 
		HashMap<String, int[]> rulesPres = new HashMap<String, int[]>();
		for(int i=0; i<size; i++) {
			TSNodeLabel t = generateRandomTree();
			if (avoidRepetitions) {				
				while(!treebank.add(t)) {
					//System.out.println("--\t" + t);
					t = generateRandomTree();					
				}
				//System.out.println(t);
				System.out.println(i);
			}
			ArrayList<TSNodeLabel> nonTerm = t.collectNonLexicalNodes();
			HashSet<Label> presentLabels = new HashSet<Label>();
			HashSet<String> presentRules = new HashSet<String>(); 
			for(TSNodeLabel n : nonTerm) {
				Label l = n.label;
				String r = n.cfgRule();
				Utility.increaseInHashMap(nonTermFreq, l);
				Utility.increaseInHashMap(rulesFreq, r);
				presentLabels.add(l);
				presentRules.add(r);
			}
			for(Label l : presentLabels) {
				Utility.increaseInHashMap(nonTermPres, l);
			}
			for(String r : presentRules) {
				Utility.increaseInHashMap(rulesPres, r);
			}
		}
		System.out.println("Normalized non-term freq (per tree):");
		for(Entry<Label, int[]> e : nonTermFreq.entrySet()) {
			double f = ((double)e.getValue()[0])/size;
			System.out.println(e.getKey() + "\t" + f);
		}
		System.out.println("Normalized rule freq (per tree):");
		for(Entry<String, int[]> e : rulesFreq.entrySet()) {
			double f = ((double)e.getValue()[0])/size;
			System.out.println(e.getKey() + "\t" + f);
		}
		System.out.println("Normalized non-term prob. (per tree):");
		for(Entry<Label, int[]> e : nonTermPres.entrySet()) {
			double f = ((double)e.getValue()[0])/size;
			System.out.println(e.getKey() + "\t" + f);
		}
		System.out.println("Normalized rule prob (per tree):");
		for(Entry<String, int[]> e : rulesPres.entrySet()) {
			double f = ((double)e.getValue()[0])/size;
			System.out.println(e.getKey() + "\t" + f);
		}
	}
	
	public TSNodeLabel getRandomRuleClone(Label root) {
		HashMap<TSNodeLabel, Double> subTable = ruleTable.get(root);
		double random = Math.random();
		double totalFreq = 0;
		for(Entry<TSNodeLabel, Double> e : subTable.entrySet()) {
			double f = e.getValue();
			totalFreq += f;
			if (totalFreq>=random) {
				return e.getKey().clone();
			}
		}
		return null;
	}

	public TSNodeLabel generateRandomTree() {
		TSNodeLabel result = getRandomRuleClone(startSymbol);
		generateRandomTreeRecursive(result);
		return result;
	}
	
	private void generateRandomTreeRecursive(TSNodeLabel rule) {
		TSNodeLabel[] daughters = rule.daughters;		
		for(int i=0; i<daughters.length; i++) {
			TSNodeLabel d = daughters[i];
			if (d.isLexical)
				continue;
			TSNodeLabel dRule = getRandomRuleClone(d.label);
			rule.assignDaughter(dRule, i);			
			generateRandomTreeRecursive(dRule);
		}
	}
	
	public void getNonTermCharge() {
		HashMap<Label, double[]> notTermTotalCharge = new HashMap<Label, double[]>();
		for(Label l : ruleTable.keySet()) {
			notTermTotalCharge.put(l, new double[1]);
		}		
		HashMap<Label, double[]> nonTermCharge = new HashMap<Label, double[]>();
		nonTermCharge.put(startSymbol, new double[]{1});
		notTermTotalCharge.put(startSymbol, new double[]{1});
		double totalCharge = 1;
		int cycle = 0;
		while(totalCharge>1E-20) {
			cycle++;
			totalCharge = 0;
			HashMap<Label, double[]> newNonTermCharge = new HashMap<Label, double[]>(); 
			for(Entry<Label, double[]> e : nonTermCharge.entrySet()) {
				Label l = e.getKey();				
				double charge = e.getValue()[0];
				HashMap<TSNodeLabel, Double> ruleProb = ruleTable.get(l);
				for(Entry<TSNodeLabel, Double> f : ruleProb.entrySet()) {
					TSNodeLabel r = f.getKey();
					if (r.isPreLexical())
						continue;
					double newCharge = f.getValue() * charge;
					for(TSNodeLabel d : r.daughters) {
						Utility.increaseInHashMap(newNonTermCharge, d.label, newCharge);
					}
				}
			}
			Utility.increaseAllHashMap(newNonTermCharge, notTermTotalCharge);
			nonTermCharge = newNonTermCharge;
			totalCharge = Utility.getSumValue(nonTermCharge);
		}
		System.out.println("Total cycles: " + cycle);
		System.out.println("Non-term total charge:");
		for(Entry<Label, double[]> e : notTermTotalCharge.entrySet()) {
			double c = ((double)e.getValue()[0]);
			System.out.println(e.getKey() + "\t" + c);
		}
	}
	
	public HashMap<Label, Double> getNonTermUsageFreqViaInvertedMatrix(boolean debug) {		
		int m = nonTerms.length;				
		double[][] matrixRHS = new double[m][m];
		for(int i=0; i<m; i++) {
			for(int j=0; j<m; j++) {
				matrixRHS[i][j] = getRHSProb(nonTerms[i],nonTerms[j]);
				if (debug && matrixRHS[i][j]>0)
					System.out.println(nonTerms[i] + " -> " + nonTerms[j] + ": " + matrixRHS[i][j]);
			}
		}
		Matrix matrix = new Matrix(matrixRHS);
		Matrix identity = Matrix.identity(m,m);
		Matrix inverse = identity.minus(matrix).inverse();
		Matrix resultMatrix = matrix.times(inverse);
		double[][] resultArray = resultMatrix.getArray();
		System.out.println("--------");
		for(int i=0; i<m; i++) {
			for(int j=0; j<m; j++) {
				if (debug && resultArray[i][j]>0)
					System.out.println(nonTerms[i] + " -> " + nonTerms[j] + ": " + resultArray[i][j]);
			}
		}
		HashMap<Label, Double> result = convertNonTermArrayToHashMap(resultArray).get(startSymbol);
		result.put(startSymbol, 1d);
		return result;
	}

	private HashMap<Label, HashMap<Label, Double>> convertNonTermArrayToHashMap(
			double[][] resultArray) {
		HashMap<Label, HashMap<Label, Double>> result = new HashMap<Label, HashMap<Label, Double>>();
		int m = nonTerms.length;
		for(int i=0; i<m; i++) {
			Label x = nonTerms[i];
			HashMap<Label, Double> subTable = new HashMap<Label, Double>();
			result.put(x, subTable);
			for(int j=0; j<m; j++) {	
				Label y = nonTerms[j];
				subTable.put(y,resultArray[i][j]);
			}
		}
		return result;
	}

	/**
	 * Total prob of choosing a production x that has y has RHS
	 * @param x
	 * @param y
	 * @return
	 */
	private double getRHSProb(Label x, Label y) {
		HashMap<TSNodeLabel, Double> fragRootX = ruleTable.get(x);
		double totalProb = 0;
		for(Entry<TSNodeLabel, Double> e : fragRootX.entrySet()) {
			TSNodeLabel f = e.getKey();
			if (f.collectTerminalLabels().contains(y))
				totalProb += e.getValue();
		}
		return totalProb;
	}
	
	public HashMap<Label, HashMap<TSNodeLabel, Double>> getRuleUsageFreqViaInvertedMatrix(boolean debug) {
		HashMap<Label, HashMap<TSNodeLabel, Double>> ruleUsageFreq = new HashMap<Label, HashMap<TSNodeLabel, Double>>();
		HashMap<Label, Double> nonTermUsageFreq = getNonTermUsageFreqViaInvertedMatrix(false);
		for(Entry<Label, HashMap<TSNodeLabel, Double>> e : ruleTable.entrySet()) {
			Label l = e.getKey();			
			HashMap<TSNodeLabel, Double> subTableSrc = e.getValue();
			HashMap<TSNodeLabel, Double> subTableDst = new HashMap<TSNodeLabel, Double>();
			ruleUsageFreq.put(l, subTableDst);
			for(Entry<TSNodeLabel, Double> f : subTableSrc.entrySet()) {
				TSNodeLabel frag = f.getKey();
				Label root = frag.label;
				double fragUsageFreq = f.getValue() * nonTermUsageFreq.get(root);
				subTableDst.put(frag, fragUsageFreq);
				if (debug) {
					System.out.println(frag + ": " + fragUsageFreq);
				}
			}
		}
		return ruleUsageFreq;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File f = new File("/Users/fedja/Work/JointProjects/Jelle/PnP/ExpFreqExampleGrammar.txt");
		ExpectedFreqPCFG EF = new ExpectedFreqPCFG(f);
		//TSNodeLabel t = EF.generateRandomTree();
		//System.out.println(t.toStringTex(false, false));
		//EF.analyzeRandomTreebankFreq(10000000, false);
		//EF.getNonTermCharge();		
		//EF.getNonTermCharge();
		//EF.getNonTermUsageFreqViaInvertedMatrix(true);
		EF.getRuleUsageFreqViaInvertedMatrix(true);
	}

	

}
