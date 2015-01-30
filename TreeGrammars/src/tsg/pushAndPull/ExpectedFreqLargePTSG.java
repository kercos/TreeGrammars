package tsg.pushAndPull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import util.ArgumentReader;
import util.Duet;
import util.PrintProgress;
import util.PrintProgressStatic;
import util.Utility;
import util.file.FileUtil;
import Jama.Matrix;
import fig.basic.IdentityHashSet;

public class ExpectedFreqLargePTSG {

	static Label startSymbol = Label.getLabel("S");
	static final int trainOF_Index = 0;
	static final int weight_Index = 1;
	static final int expUF_Index = 2;
	static final int expOF_Index = 3;
	
	int treesInTreebank, totalFrags;
	HashMap<Label, HashMap<TSNodeLabel, double[]>> frag_trainOF_Weight_UF_OF_Table;
	TSNodeLabel[] allFragsArray;	
	Label[] nonTerms;
	
	HashMap<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>> focalFragSigmaInitTable, focalNodeSigmaContTable;
	String outputPath;
	
	FileOutputStream fileStreamInit, fileStreamCont;
	BufferedWriter bzipWriterInit, bzipWriterCont;
	boolean buildSigmaOnFile;
	
	// IdentityHashMap faster but takes lot of memory
	//IdentityHashMap<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>> focalNodeSigmaContTable;
	
	HashMap<Label, Double> nonTermUsageFreq;
	
	
	public ExpectedFreqLargePTSG(String outputPath, File treebankFile, 
			File fragFile, boolean buildSigmaOnFile) throws Exception {
		
		this.outputPath = outputPath;
		this.buildSigmaOnFile = buildSigmaOnFile;
		Parameters.openLogFile(new File(outputPath + "log.txt"));
		
		frag_trainOF_Weight_UF_OF_Table = new HashMap<Label, HashMap<TSNodeLabel, double[]>>();
		
		readTreebankFile(treebankFile);
		
		readFragFile(fragFile);
		
		getWeights();
		getAllFragsArray();
		nonTerms = frag_trainOF_Weight_UF_OF_Table.keySet().toArray(new Label[]{});
		
		buildSigmaTables(false); 
		//build focalFragSigmaInitTable and focalNodeSigmaContTable;
		
		//if (buildSigmaOnlyOnFile)
			
		//else
		//	readSigmaTables(false);
		
		
				
		//getAnalyticCounts();								
	}
	
	private void getAnalyticCounts() {
		Parameters.reportLineFlush("Analytical Counts");
		
		//build nonTermUsageFreq
		Parameters.reportLineFlush("\tNonTermUsageFreqViaInvertedMatrix");
		getNonTermUsageFreqViaInvertedMatrix(false);
		
		//calculate FragUF in frag_trainOF_Weight_UF_OF_Table
		Parameters.reportLineFlush("\tFragUsageFreqViaInvertedMatrix");
		getFragUsageFreqViaInvertedMatrix(false);		
		
		Parameters.reportLineFlush("\tFragOccuranceFreqAnalitically");
		getFragOccuranceFreqAnalitically(false);
	}
	
	private void readTreebankFile(File inputFile) throws Exception {
		Parameters.reportLine("Reading treebank: " + inputFile);
		treesInTreebank = 0;
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(inputFile);		
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.isEmpty())
				continue;
			treesInTreebank++;
		}
		Parameters.reportLineFlush("Total Trees: " + treesInTreebank);
	}
	
	private void readFragFile(File inputFile) throws Exception {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(inputFile);
		
		Parameters.reportLine("Reading fragments: " + inputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.isEmpty())
				continue;
			totalFrags++;
			String[] parts = line.split("\t");
			TSNodeLabel frag = new TSNodeLabel(parts[0], false);
			int freq = Integer.parseInt(parts[1]);
			double[] val = new double[4]; // freq, weight, uf, of
			val[trainOF_Index] = ((double)freq)/treesInTreebank;
			Utility.putInMapDouble(frag_trainOF_Weight_UF_OF_Table, frag.label, frag, val);			
		}	
		
		Parameters.reportLineFlush("Total fragments: " + totalFrags);
	}
	
	/**
	 * Compute relative frequencies
	 */
	private void getWeights() {
		//Parameters.reportLineFlush("Filling start weights");
		for(HashMap<TSNodeLabel, double[]> subTable : frag_trainOF_Weight_UF_OF_Table.values()) {
			//int size = subTable.size();
			double totFreq = Utility.getSumValue(subTable,trainOF_Index);
			for(double[] v : subTable.values()) {
				v[weight_Index] = v[trainOF_Index]/totFreq;
			}
		}
	}
	
	private void normalizeWeights() {
		for(HashMap<TSNodeLabel, double[]> subTable : frag_trainOF_Weight_UF_OF_Table.values()) {
			//int size = subTable.size();
			double totFreq = Utility.getSumValue(subTable,weight_Index);
			for(double[] v : subTable.values()) {
				v[weight_Index] = v[weight_Index]/totFreq;
			}
		}
	}
	
	private void checkNormalizedWeights() {
		for(Entry<Label, HashMap<TSNodeLabel, double[]>> e : frag_trainOF_Weight_UF_OF_Table.entrySet()) {
			//int size = subTable.size();
			double totFreq = Utility.getSumValue(e.getValue(),weight_Index);
			if (Math.abs(1-totFreq)>0.05) {
				System.err.println("Weights are not normalized for frags with root " + e.getKey());
				System.err.println("Total marginal prob: " + totFreq);
			}
		}
	}

	public void printFragsFreqWeights() {
		for(HashMap<TSNodeLabel, double[]> focalFragSubTable : frag_trainOF_Weight_UF_OF_Table.values()) {
			for(Entry<TSNodeLabel, double[]> e : focalFragSubTable.entrySet()) {
				TSNodeLabel focalFrag = e.getKey();
				double[] val = e.getValue();
				System.out.println(focalFrag + "\t" + val[trainOF_Index] + "\t" + val[weight_Index]);
			}
		}		
	}
	
	public void printFragsWeightsToFile(File ouptutFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(ouptutFile);
		for(HashMap<TSNodeLabel, double[]> focalFragSubTable : frag_trainOF_Weight_UF_OF_Table.values()) {
			for(Entry<TSNodeLabel, double[]> e : focalFragSubTable.entrySet()) {
				TSNodeLabel focalFrag = e.getKey();
				double[] val = e.getValue();
				pw.println(focalFrag.toString(false, true) + "\t" + val[weight_Index]);
			}
		}	
		pw.close();
	}

	private void getAllFragsArray() {
		HashSet<TSNodeLabel> result = new HashSet<TSNodeLabel>();
		for(HashMap<TSNodeLabel, double[]> subTable : frag_trainOF_Weight_UF_OF_Table.values()) {
			result.addAll(subTable.keySet());
		}	
		allFragsArray = result.toArray(new TSNodeLabel[]{});
	}
	
	public static void getAllFragsFreq(File f_trees, File f_frags) throws Exception {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(f_trees);
		HashMap<Label, HashMap<TSNodeLabel, int[]>> subTreesTable= new HashMap<Label, HashMap<TSNodeLabel, int[]>>(); 
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line, true);
			ArrayList<String> subtrees = t.allSubTrees(5, 5);
			for(String stString : subtrees) {
				TSNodeLabel st = new TSNodeLabel(stString, false);
				Utility.increaseInHashMap(subTreesTable, st.label, st, 1);
			}			
		}
		PrintWriter pw = new PrintWriter(f_frags);
		for(HashMap<TSNodeLabel, int[]> subTable : subTreesTable.values()) {
			//int totFreq = Utility.getSumValueInt(subTable);		
			for(Entry<TSNodeLabel, int[]> e : subTable.entrySet()) {
				TSNodeLabel frag = e.getKey();
				//double prob = ((double)e.getValue()[0])/totFreq;
				int freq = e.getValue()[0];
				pw.println(frag.toString(false, true) + "\t" + freq);
			}
		}
		pw.close();
				
	}

	public ArrayList<TSNodeLabel> generateRandomTreebank(int size) {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>(size);
		for(int i=0; i<size; i++) {
			result.add(generateRandomTree(new ArrayList<TSNodeLabel>()));
		}
		return result;
	}
	
	public HashMap<TSNodeLabel, double[]> getFragUsageOccFreqViaSimulation(int size, 
			boolean avoidRepetitions, boolean debug) {		
		
		System.out.println("Generating random trees: " + size);
		
		HashSet<TSNodeLabel> treebank = new HashSet<TSNodeLabel>(); 
		HashMap<Label, int[]> nonTermFreq = new HashMap<Label, int[]>(); 
		HashMap<TSNodeLabel, int[]> fragUsageFreq = new HashMap<TSNodeLabel, int[]>();
		HashMap<TSNodeLabel, int[]> fragOccFreq = new HashMap<TSNodeLabel, int[]>();
		HashMap<TSNodeLabel, double[]> fragUsageOccFreq = new HashMap<TSNodeLabel, double[]>();
		//HashMap<Label, int[]> nonTermPres = new HashMap<Label, int[]>(); 
		//HashMap<String, int[]> rulesPres = new HashMap<String, int[]>();
		
		for(int i=0; i<size; i++) {
			if (i%1000==0) {
				System.out.print("\r" + i);
			}
			ArrayList<TSNodeLabel> derFrags = new ArrayList<TSNodeLabel>();
			TSNodeLabel t = generateRandomTree(derFrags);			
			if (avoidRepetitions) {				
				derFrags = new ArrayList<TSNodeLabel>();
				while(!treebank.add(t)) {
					//System.out.println("--\t" + t);
					t = generateRandomTree(derFrags);					
				}
				//System.out.println(t);
				System.out.println(i);
			}
			//System.out.println(t.toString(false,true));			
			//HashSet<Label> presentLabels = new HashSet<Label>();
			//HashSet<String> presentRules = new HashSet<String>(); 
			for(TSNodeLabel frag : derFrags) {
				//System.out.println("\t" + frag.toString(false,true));
				Label l = frag.label;
				Utility.increaseInHashMap(nonTermFreq, l);
				Utility.increaseInHashMap(fragUsageFreq, frag);
				//presentLabels.add(l);
				//presentRules.add(r);
			}
			ArrayList<TSNodeLabel> nonTerm = t.collectNonLexicalNodes();			
			for(TSNodeLabel n : nonTerm) {
				HashMap<TSNodeLabel, double[]> root = frag_trainOF_Weight_UF_OF_Table.get(n.label);	
				for(TSNodeLabel frag : root.keySet()) {
					if (n.containsNonRecursiveFragment(frag)) {
						Utility.increaseInHashMap(fragOccFreq, frag);
					}
				}
			}
			/*
			for(Label l : presentLabels) {
				Utility.increaseInHashMap(nonTermPres, l);
			}
			for(String r : presentRules) {
				Utility.increaseInHashMap(rulesPres, r);
			}
			*/
		}
		System.out.print("\r");
		/*
		System.out.println("Normalized non-term freq (per tree):");
		for(Entry<Label, int[]> e : nonTermFreq.entrySet()) {
			double f = ((double)e.getValue()[0])/size;
			System.out.println(e.getKey() + "\t" + f);
		}
		System.out.println("Normalized frag usage freq (per tree):");
		for(Entry<TSNodeLabel, int[]> e : fragUsageFreq.entrySet()) {
			double f = ((double)e.getValue()[0])/size;
			System.out.println(e.getKey().toString(false, true) + "\t" + f);
		}
		System.out.println("Normalized frag occurrence freq (per tree):");
		for(Entry<TSNodeLabel, int[]> e : fragOccFreq.entrySet()) {
			double f = ((double)e.getValue()[0])/size;
			System.out.println(e.getKey().toString(false, true) + "\t" + f);
		}
		*/
		
		if (debug) {
			System.out.println("Frag\tusageFreq\toccFreq");
		}
		
		for(HashMap<TSNodeLabel, double[]> subTables : frag_trainOF_Weight_UF_OF_Table.values()) {
			for(TSNodeLabel frag : subTables.keySet()) {
				int[] usageFreqArr = fragUsageFreq.get(frag);
				int usageFreq = (usageFreqArr==null) ? 0 : usageFreqArr[0];
				//double usageAvgFreq = ((double)usageFreq/size);
				int[] occFreqArr = fragOccFreq.get(frag);
				int occFreq = (occFreqArr==null) ? 0 : occFreqArr[0];
				//double occAvgFreq = ((double)occFreq/size);
				//int diff = occFreq-usageFreq;
				//if (diff!=0)
				if (debug) {
					System.out.println(frag.toString() + " & " + usageFreq + " & " + occFreq + "\\");
				}				
					//System.out.println(frag.toString(false, true) + "\t" + diff);
				fragUsageOccFreq.put(frag, new double[]{usageFreq, occFreq});
			}
		}
		
		
		/*
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
		*/
		return fragUsageOccFreq;
	}
	
	public void compareAnalyticalWithSimulatedFreq(double threshold) {
		System.out.println("Comparing analytical with simulated freq");
		
		int numGenTrees = 10000000;
		HashMap<TSNodeLabel, double[]> simFreqUsagOccFreqTable = 
				getFragUsageOccFreqViaSimulation(numGenTrees, false, false);
		
		boolean checkOk = true;
		for(TSNodeLabel frag : allFragsArray) {
			double[] simFreq = simFreqUsagOccFreqTable.get(frag);
			double[] analyticFreq = frag_trainOF_Weight_UF_OF_Table.get(frag.label).get(frag);
			double simUsage = simFreq[0]/numGenTrees;			
			double simOcc = simFreq[1]/numGenTrees;
			double analyticUsage = analyticFreq[expUF_Index];
			double analyticOcc = analyticFreq[expOF_Index];
			
			System.out.println(frag.toString() + "\t" + simUsage + "\t" + analyticUsage + 
					"\t" + simOcc + "\t" + analyticOcc);
			
			double usageDelta = Math.abs(simUsage-analyticUsage);
			double occDelta = Math.abs(simOcc-analyticOcc);
			if (usageDelta>threshold) {
				System.err.println("Usage freq delta greater than " + threshold);
				checkOk = false;
			}
			if (occDelta>threshold) { 
				System.err.println("Occurance freq delta greater than " + threshold);
				checkOk = false;
			}
		}
		System.out.println("\nOverall Check OK: " + checkOk);
	}
	
	
	public TSNodeLabel getRandomFragClone(Label root, ArrayList<TSNodeLabel> derFrags) {
		HashMap<TSNodeLabel, double[]> subTable = frag_trainOF_Weight_UF_OF_Table.get(root);
		double random = Math.random();
		double totalFreq = 0;
		for(Entry<TSNodeLabel, double[]> e : subTable.entrySet()) {
			double f = e.getValue()[weight_Index];
			totalFreq += f;
			if (totalFreq>=random) {
				TSNodeLabel frag = e.getKey(); 
				derFrags.add(frag);
				return frag.clone();
			}
		}
		return null;
	}
	
	public void generateRandomTreeTest() {
		ArrayList<TSNodeLabel> derFrags = new ArrayList<TSNodeLabel>();
		TSNodeLabel result = generateRandomTree(derFrags);
		System.out.println(result.toString(false, true));
		for(TSNodeLabel frag : derFrags){
			System.out.println("\t" + frag.toString(false, true));
		}
		
	}

	public TSNodeLabel generateRandomTree(ArrayList<TSNodeLabel> derFrags) {
		TSNodeLabel result = getRandomFragClone(startSymbol, derFrags);
		generateRandomTreeRecursive(result, derFrags);
		return result;
	}
	
	private void generateRandomTreeRecursive(TSNodeLabel result, ArrayList<TSNodeLabel> derFrags) {
		ArrayList<TSNodeLabel> subsites = result.collectNonLexTerminals();		
		for(TSNodeLabel s : subsites) {						
			TSNodeLabel sp = s.parent;
			int i = sp.indexOfDaughter(s);
			TSNodeLabel dFrag = getRandomFragClone(s.label, derFrags);			
			sp.assignDaughter(dFrag, i);			
			generateRandomTreeRecursive(dFrag, derFrags);
		}
	}
	
	public void getNonTermCharge() {
		HashMap<Label, double[]> notTermTotalCharge = new HashMap<Label, double[]>();
		for(Label l : frag_trainOF_Weight_UF_OF_Table.keySet()) {
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
				HashMap<TSNodeLabel, double[]> ruleProb = frag_trainOF_Weight_UF_OF_Table.get(l);
				for(Entry<TSNodeLabel, double[]> f : ruleProb.entrySet()) {
					TSNodeLabel r = f.getKey();
					if (r.isPreLexical())
						continue;
					double newCharge = f.getValue()[0] * charge;
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

	private void getNonTermUsageFreqViaInvertedMatrix(boolean debug) {		
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
		//System.out.println("--------");
		if (debug) {
			for(int i=0; i<m; i++) {
				for(int j=0; j<m; j++) {
					if (resultArray[i][j]>0)
						System.out.println(nonTerms[i] + " -> " + nonTerms[j] + ": " + resultArray[i][j]);
				}
			}
		}
		nonTermUsageFreq = convertNonTermArrayToHashMap(resultArray).get(startSymbol);
		nonTermUsageFreq.put(startSymbol, 1d);
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
		HashMap<TSNodeLabel, double[]> fragRootX = frag_trainOF_Weight_UF_OF_Table.get(x);
		double totalProb = 0;
		for(Entry<TSNodeLabel, double[]> e : fragRootX.entrySet()) {
			TSNodeLabel f = e.getKey();
			int freq = Utility.countEquals(f.collectTerminalLabels(),y);
			if (freq>0) {
				double fragWeight = e.getValue()[weight_Index];
				totalProb += fragWeight * freq;
			}
		}
		return totalProb;
	}
	
	private void getFragUsageFreqViaInvertedMatrix(boolean debug) {		
		for(Entry<Label, HashMap<TSNodeLabel, double[]>> e : frag_trainOF_Weight_UF_OF_Table.entrySet()) {
			HashMap<TSNodeLabel, double[]> subTable = e.getValue();
			for(Entry<TSNodeLabel, double[]> f : subTable.entrySet()) {
				TSNodeLabel frag = f.getKey();
				Label root = frag.label;
				double[] val = f.getValue();
				val[expUF_Index] = val[weight_Index] * nonTermUsageFreq.get(root);
				
				if (debug) {
					System.out.println(frag + "\t" + val[expUF_Index]);
				}
			}
		}
	}
	
	private void getFragOccuranceFreqAnalitically(boolean debug) {		
		for(Entry<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>> e : focalFragSigmaInitTable.entrySet()) {
			TSNodeLabel focalFrag = e.getKey();
			IdentityHashMap<TSNodeLabel,double[]> focalFragContProb = new IdentityHashMap<TSNodeLabel,double[]>(); 
			double occFreq = 0;
			for(SigmaStructure s : e.getValue().keySet()) {				
				double sigmaOccFreq = s.getComplFragsMargProbUF(); //nonTermUsageFreq
				for(TSNodeLabel contNode : s.continuationSites) {
					sigmaOccFreq *= getMarginalContinuationProb(contNode, focalFragContProb);
				}
				occFreq += sigmaOccFreq;
			}
			if (debug) {
				System.out.println(focalFrag + "\t" + occFreq);
			}
			double[] val = frag_trainOF_Weight_UF_OF_Table.get(focalFrag.label).get(focalFrag);
			val[expOF_Index] = occFreq;
		}
	}

	private double getMarginalContinuationProb(TSNodeLabel contNode,
			IdentityHashMap<TSNodeLabel, double[]> focalFragContProb) {
		double[] result = focalFragContProb.get(contNode);
		if (result==null) {			
			double contProb = 0;
			for(SigmaStructure s : focalNodeSigmaContTable.get(contNode).keySet()) {
				double sigmaOccFreq = s.getComplFragsMargProbWeights();
				for(TSNodeLabel contNodeB : s.continuationSites) {
					sigmaOccFreq *=  getMarginalContinuationProb(contNodeB, focalFragContProb);
				}
				contProb += sigmaOccFreq;
			}
			result = new double[]{contProb};			
			focalFragContProb.put(contNode,result);
		}
		return result[0];
	}

	/**
	 * For every fragment x we compute all the structures sigma, such that
	 * sigma is an initial part of x (possibly including the root node 
	 * and whole fragment) and there is a fragment y that has sigma at the frontier.
	 * We indicate all the frontier nodes in sigma that are not a substitution site in x
	 * with a special mark (the head marks in TSNodeLabel).
	 * For each fragment x there may be multiple fragments y associated with the same sigma.
	 * For a given fragment x each specific sigma is also associated with a set of structures
	 * wich represent the remaining bits to complete x from sigma. The number of structures
	 * equals the number of marked nodes at the frontier of sigma. 
	 */
	private void buildSigmaTables(boolean debug) {
		
		Parameters.reportLineFlush("Building Sigma Tables");			
		
		focalFragSigmaInitTable = new HashMap<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>>();
		focalNodeSigmaContTable = new HashMap<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>>();
		//IdentityHashMap
				
		Parameters.reportLineFlush("\tTotal Frags: " + totalFrags);
		PrintProgress printProg = new PrintProgress("\tProcessing:");	
		
		if (buildSigmaOnFile) {
			openBzFileSigmaTables();
		}
		
		for(HashMap<TSNodeLabel, double[]> focalFragSubTable : frag_trainOF_Weight_UF_OF_Table.values()) {
			for(Entry<TSNodeLabel, double[]> e : focalFragSubTable.entrySet()) {
				printProg.next();
				TSNodeLabel focalFrag = e.getKey();
				double[] focalFragWeightUF = e.getValue();
				HashMap<SigmaStructure, SigmaStructure> focalFragSigmaTable = new HashMap<SigmaStructure, SigmaStructure>();
				IdentityHashSet<TSNodeLabel> focalNodeStopSet = new IdentityHashSet<TSNodeLabel>();
				//focalNodeStopSet stores the non-terminal nodes in focalFrags that cannot be continued
				IdentityHashSet<TSNodeLabel> focalNodeContPresentSet = new IdentityHashSet<TSNodeLabel>();
				//focalNodeContPresentSet stores the non-terminal nodes in focalFrags that already have a stored continuation
				for(HashMap<TSNodeLabel, double[]> superFragSubTable : frag_trainOF_Weight_UF_OF_Table.values()) {
					for(Entry<TSNodeLabel, double[]> f : superFragSubTable.entrySet()) {
						TSNodeLabel superFrag = f.getKey();
						double[] superFragWeightUF = f.getValue(); 
						fillSigmaStructureRecursive(focalFrag, focalFragWeightUF, superFrag, superFrag, 
								superFragWeightUF, focalFragSigmaTable, focalNodeContPresentSet, focalNodeStopSet, true);							
					}
				}
				if (!focalFragSigmaTable.isEmpty())
					focalFragSigmaInitTable.put(focalFrag, focalFragSigmaTable);
				
				if (buildSigmaOnFile) {
					appendBzFileSigmaTables();
					focalFragSigmaInitTable.clear();
					focalNodeSigmaContTable.clear();
				}
			}			
		}
		printProg.end();
				
		if (buildSigmaOnFile) {
			closeBzSigmaTableFile();
		}
		
		//polishSigmaInitTable();
		
		if (debug) {
			System.out.println("------------------------");
			System.out.println("Sigma FocalFrag Init Table");
			System.out.println(printSigmaTexTable(focalFragSigmaInitTable, false));
			System.out.println("------------------------");
			System.out.println("Sigma FocalFrag Cont Table");
			System.out.println(printSigmaTexTable(focalNodeSigmaContTable, true));
		}
	}
	
	private void readSigmaTables(boolean debug) throws Exception {
		
		Parameters.reportLineFlush("Reading Sigma Tables from file");
		
		focalFragSigmaInitTable = new HashMap<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>>();
		focalNodeSigmaContTable = new HashMap<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>>();
		
		File inputFileInit = new File(outputPath + "SigmaTableInit.txt.bz2");
		File inputFileCont = new File(outputPath + "SigmaTableCont.txt.bz2");
				
		FileInputStream inputFileStream = new FileInputStream(inputFileInit);
		/*
		if (readTwoBytes) {
			inputFileStream.read();
			inputFileStream.read();
		}
		*/
		BufferedReader gzipReader = new BufferedReader(
				new InputStreamReader(new CBZip2InputStream(inputFileStream),"UTF-8"));		
		
		String line = null;
		while((line = gzipReader.readLine()) != null) {
			TSNodeLabel frag = new TSNodeLabel(line, false);
			HashMap<SigmaStructure, SigmaStructure> fragTable = new HashMap<SigmaStructure, SigmaStructure>(); 
			while((line = gzipReader.readLine()) != null) {
				if (line.isEmpty()) 
					break;
				TSNodeLabel sigmaFrag = new TSNodeLabel(line,false);
				
			}
			//focalFragSigmaInitTable.put();
		}
		
	}
	
	
	private void openBzFileSigmaTables() {
		File outputFileInit = new File(outputPath + "SigmaTableInit.txt.bz2");
		File outputFileCont = new File(outputPath + "SigmaTableCont.txt.bz2");
		
		try {
			fileStreamInit = new FileOutputStream(outputFileInit);
			bzipWriterInit = new BufferedWriter(
					new OutputStreamWriter(new CBZip2OutputStream(fileStreamInit), "UTF-8"));
			fileStreamCont = new FileOutputStream(outputFileCont);
			bzipWriterCont = new BufferedWriter(
					new OutputStreamWriter(new CBZip2OutputStream(fileStreamCont), "UTF-8"));
			
		} catch (Exception e) {
			System.err.println("Problems while writing bzip2 file");
			e.printStackTrace();
			return;
		}						
	}
	
	private void appendBzFileSigmaTables() {
		try {
			appendBzFileSigmaTables(focalFragSigmaInitTable, bzipWriterInit);
			appendBzFileSigmaTables(focalNodeSigmaContTable, bzipWriterCont);
		} catch (Exception e) {
			System.err.println("Problems while writing bzip2 file");
			e.printStackTrace();
			return;
		}
		
	}
	
	private static void appendBzFileSigmaTables(HashMap<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>> table,
			BufferedWriter bzipWriter) throws IOException {
		for(Entry<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>> e : table.entrySet()) {
			TSNodeLabel frag = e.getKey();
			bzipWriter.write(frag.toString(true, true) + "\n");
			for(SigmaStructure s : e.getValue().keySet()) {
				bzipWriter.write("\t" + s.sigmaFrag.toString(true, true) + "\n");
			}
			bzipWriter.write("\n");
		}	
	}
	
	private void closeBzSigmaTableFile() {	
		try {
			bzipWriterInit.close();
			fileStreamInit.close();
			bzipWriterCont.close();
			fileStreamCont.close();
		} catch (Exception e) {
			System.err.println("Problems while writing bzip2 file");
			e.printStackTrace();
			return;
		}	
	}

	
	void fillSigmaStructureRecursive(TSNodeLabel focalFrag, double[] focalFragWeightUF, TSNodeLabel superFragSubNode, 
			TSNodeLabel superFrag, double[] superFragWeightUF,
			HashMap<SigmaStructure, SigmaStructure> focalFragSigmaTable, 
			IdentityHashSet<TSNodeLabel> focalNodeContPresentSet, IdentityHashSet<TSNodeLabel> focalNodeStopSet, 
			boolean first) {
		
		if (!first || focalFrag.label==startSymbol) {
			SigmaStructure focalSigma = getSigma(focalFrag, superFragSubNode);
			if (focalSigma!=null) {
				boolean hasAllContinuation = true;
				for(TSNodeLabel focalContNode : focalSigma.continuationSites) {
					if (!buildContinuationsRecursive(focalContNode, focalNodeContPresentSet, focalNodeStopSet)) {
						hasAllContinuation = false;
						break;
					}
					
				}
				if (hasAllContinuation) {
					SigmaStructure storedFocalSigma = focalFragSigmaTable.get(focalSigma);
					if (storedFocalSigma==null) {
						storedFocalSigma = focalSigma;
						focalFragSigmaTable.put(storedFocalSigma, storedFocalSigma);		
					}
					storedFocalSigma.addComplementFragUserFreq(superFrag, superFragWeightUF);
				}														
			}
		}
		
		if (!superFragSubNode.isTerminal()) {
			for(TSNodeLabel d : superFragSubNode.daughters) {
				fillSigmaStructureRecursive(focalFrag, focalFragWeightUF, d, superFrag, 
						superFragWeightUF, focalFragSigmaTable, focalNodeContPresentSet, focalNodeStopSet, false);
			}
		}
	}

	private boolean buildContinuationsRecursive(TSNodeLabel focalContNode,
			IdentityHashSet<TSNodeLabel> focalNodeContPresentSet, 
			IdentityHashSet<TSNodeLabel> focalNodeStopSet) {
		
		if (focalNodeContPresentSet.contains(focalContNode))
			return true;
		
		if (focalNodeStopSet.contains(focalContNode))
			return false;
		
		if (focalNodeSigmaContTable.containsKey(focalContNode)) {
			focalNodeContPresentSet.add(focalContNode);
			return true;
		}
		
		HashMap<TSNodeLabel, double[]> contFragSubTable = frag_trainOF_Weight_UF_OF_Table.get(focalContNode.label);		
		if (contFragSubTable==null) {
			focalNodeStopSet.add(focalContNode);
			return false;
		}
		
		HashMap<SigmaStructure, SigmaStructure> focalNodeContSigmaTable = 
				new HashMap<SigmaStructure, SigmaStructure>();
		
		for(Entry<TSNodeLabel, double[]> f : contFragSubTable.entrySet()) {
			TSNodeLabel contFrag = f.getKey();
			SigmaStructure contSigma = getSigma(focalContNode, contFrag);			
			if (contSigma!=null) {
				boolean hasAllContinuation = true;
				for(TSNodeLabel subFocalContNode : contSigma.continuationSites) {
					if (!buildContinuationsRecursive(subFocalContNode, focalNodeContPresentSet, focalNodeStopSet)) {
						hasAllContinuation = false;
						break;
					}
					
				}
				if (hasAllContinuation) {					
					SigmaStructure storedFocalSigma = focalNodeContSigmaTable.get(contSigma);
					if (storedFocalSigma==null) {
						storedFocalSigma = contSigma;
						focalNodeContSigmaTable.put(storedFocalSigma, storedFocalSigma);		
					}
					double[] superFragWeightUF = f.getValue();
					storedFocalSigma.addComplementFragUserFreq(contFrag, superFragWeightUF);
				}														
			}
		}
	
		if (focalNodeContSigmaTable.isEmpty()) {
			focalNodeStopSet.add(focalContNode);
			return false;
		}
				
		focalNodeContPresentSet.add(focalContNode);
		focalNodeSigmaContTable.put(focalContNode, focalNodeContSigmaTable);
		return true;
	}

	static SigmaStructure getSigma(TSNodeLabel focalFrag, TSNodeLabel superFragNode) {
		if (focalFrag.label!=superFragNode.label)
			return null;			
		TSNodeLabel sigma = new TSNodeLabel(focalFrag.label, false);
		ArrayList<TSNodeLabel> continuationSitesFocalFrag = new ArrayList<TSNodeLabel>(); 		
		if (completeSigmaRecursive(focalFrag, superFragNode, sigma, continuationSitesFocalFrag)) {
			return new SigmaStructure(sigma, continuationSitesFocalFrag);
		}
		return null;
	}
	
	/**
	 * Check whether fragA is present at the frontier of fragB
	 * @param focalFragSubNode
	 * @param superFragSubNode
	 * @param sigma
	 * @param continuationSitesFocalFrag
	 * @return
	 */
	static boolean completeSigmaRecursive(TSNodeLabel focalFragSubNode, TSNodeLabel superFragSubNode, 
			TSNodeLabel sigma, ArrayList<TSNodeLabel> continuationSitesFocalFrag) {
		
		if (focalFragSubNode.isTerminal())
			return true;
		
		if (superFragSubNode.isTerminal()) {
			if (!focalFragSubNode.isTerminal()) {
				sigma.headMarked = true;
				continuationSitesFocalFrag.add(focalFragSubNode);
			}
			return true;
		}
		
		if (focalFragSubNode.sameDaughtersLabel(superFragSubNode)) {
			sigma.assignDaughters(focalFragSubNode.cloneCfg().daughters);
			TSNodeLabel[] fragA_daughters = focalFragSubNode.daughters;
			TSNodeLabel[] fragB_daughters = superFragSubNode.daughters;
			TSNodeLabel[] sigma_daughters = sigma.daughters;
			for(int i=0; i<sigma.prole(); i++) {
				if (!completeSigmaRecursive(fragA_daughters[i], fragB_daughters[i], 
						sigma_daughters[i], continuationSitesFocalFrag))
					return false;
			}
			return true;
		}
		return false;
	}
	
	private String printSigmaTexTable(
			Map<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>> sigmaTable, boolean overlineFocalFrag) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\begin{tabular}{lll}\n");
		for(Entry<TSNodeLabel, HashMap<SigmaStructure, SigmaStructure>> e : sigmaTable.entrySet()) {
			TSNodeLabel frag = e.getKey();
			if (overlineFocalFrag) {
				TSNodeLabel rootFrag = frag.getRootNode();
				frag.headMarked = true;
				sb.append("\t" + rootFrag.toStringTex(true, false));
				frag.headMarked = false;
			}
			else {
				sb.append("\t" + frag.toStringTex(false, false));
			}
			boolean first = true;
			for(SigmaStructure s : e.getValue().keySet()) {
				if (first)
					first = false;
				else 
					sb.append("\t");
				sb.append(" & " + s.sigmaFrag.toStringTex(true, false) + " &");
				for(Duet<TSNodeLabel, double[]> sfd : s.complementFragsUserFreq) {
					TSNodeLabel superFrag = sfd.firstElement;
					sb.append(" " + superFrag.toStringTex(false, false));
				}
				sb.append(" \\\\\n");		
			}				
			sb.append("\t\\hline\n");
		}
		sb.append("\\end{tabular}\n");
		return sb.toString();
	}

	static class SigmaStructure {
		
		TSNodeLabel sigmaFrag;
		ArrayList<TSNodeLabel> continuationSites;
		ArrayList<Duet<TSNodeLabel,double[]>> complementFragsUserFreq;
		
		public SigmaStructure(TSNodeLabel sigma, ArrayList<TSNodeLabel> continuationSites) {
			this.sigmaFrag = sigma;
			this.continuationSites = continuationSites;
			complementFragsUserFreq = new ArrayList<Duet<TSNodeLabel,double[]>>();
		}
		
		public boolean isSingleNode() {			
			return sigmaFrag.isTerminal();
		}

		public double getComplFragsMargProbUF() { //HashMap<Label, Double> nonTermUsageFreq
			double result = 0;
			for(Duet<TSNodeLabel, double[]> e : complementFragsUserFreq) {
				//TSNodeLabel superFrag = e.getKey();
				double[] d = e.secondElement;
				//Label sigmaRootLabel = superFrag.label;
				//double sigmaRootUF = nonTermUsageFreq.get(sigmaRootLabel);
				//result += d[1] * sigmaRootUF;
				result += d[expUF_Index];
			}
			return result;
		}
		
		public double getComplFragsMargProbWeights() {
			double result = 0;
			for(Duet<TSNodeLabel, double[]> e : complementFragsUserFreq) {
				double[] d = e.secondElement;
				result += d[weight_Index];
			}
			return result;
		}
		
		public void addComplementFragUserFreq(TSNodeLabel complementFrag, double[] complementFragWeightUF) {
			Duet<TSNodeLabel, double[]> e = new Duet<TSNodeLabel, double[]>(complementFrag, complementFragWeightUF); 
			complementFragsUserFreq.add(e);
		}
		
		public boolean equals(Object o) {
			if (o instanceof SigmaStructure) {
				SigmaStructure otherStr = (SigmaStructure)o;
				return this.sigmaFrag.equals(otherStr.sigmaFrag);
			}
			return false;
		}
		
		public int hashCode() {
			return this.sigmaFrag.hashCode();
		}
	}

	
	public void pushAndPull(double changingRate, int maxCycle) throws FileNotFoundException {
		Parameters.reportLine("Push and Pull");
		Parameters.reportLine("\tChangingRate: " + changingRate);
		Parameters.reportLine("\tMaxCycle: " + maxCycle);
		Parameters.reportLineFlush("-----------------------------------");
		int printEvery = 1;	
		//double changingRate = 0.0001;
		double tolerance = 1E-10;
		
		int cycle  = 0;
		double delta = 0;
		double deltaDelta = 0;
		ArrayList<double[]> records = new ArrayList<double[]>(); 
		do {			
			Parameters.reportLineFlush("Cycle: "+ cycle);
			boolean debug = cycle%printEvery==0;
			double newDelta = getDeltaOccFreq(debug);
			deltaDelta = Math.abs(delta - newDelta);
			delta = newDelta;
			if (debug) {
				records.add(new double[]{cycle, delta});
				File outputFile = new File(outputPath + "PPCycle_" + Utility.padZero(4,cycle));				
				Parameters.reportLineFlush("Printing Frags to file: " + outputFile);
				printFragsWeightsToFile(outputFile);
			}
			Parameters.reportLineFlush("Cycle: "+ cycle + "\t" + "Total delta: "+ delta);
			decreaseDeltaOccFreq(changingRate, tolerance);
			normalizeWeights();			
			getAnalyticCounts();			
			cycle++;
			Parameters.reportLineFlush("-----------------------------------");
		} while(delta>0.001 && cycle<maxCycle && deltaDelta>1E-8);		
		
		getDeltaOccFreq(true);
		
		Parameters.reportLine("Cycle\tDelta");
		for(double[] cd : records)
			Parameters.reportLine((int)cd[0] + "\t" + cd[1]);
		Parameters.reportLineFlush("");
	}

	private double getDeltaOccFreq(boolean debug) {
		double absTotalDelta = 0;
		double[] mostOverUnderDelta = new double[]{-1,1};
		TSNodeLabel[] mostOverUnderFrag = new TSNodeLabel[2];
		for(HashMap<TSNodeLabel, double[]> subTable : frag_trainOF_Weight_UF_OF_Table.values()) {
			for(Entry<TSNodeLabel, double[]> f : subTable.entrySet()) {
				double[] val = f.getValue();
				double d = val[expOF_Index]-val[trainOF_Index];
				double dAbs = Math.abs(d);
				absTotalDelta += dAbs;
				if (debug) {
					System.out.println(f.getKey() + "\t" + Utility.arrayToString(val, '\t') + "\t" + d);
				}				
				if (debug) {
					if (d>0) { //over
						if (d>mostOverUnderDelta[0]) {
							mostOverUnderDelta[0] = d;
							mostOverUnderFrag[0] = f.getKey();
						}
					}
					else { //under
						if (d<mostOverUnderDelta[1]) {
							mostOverUnderDelta[1] = d;
							mostOverUnderFrag[1] = f.getKey();
						}
					}					
				}
			}
		}
		if (debug) {
			System.out.println();
			if (mostOverUnderFrag[0]!=null) {
				System.out.println("Most over-estimated frag:");
				System.out.println("\t" + mostOverUnderDelta[0] + "\t" + mostOverUnderFrag[0]);
			}
			if (mostOverUnderFrag[1]!=null) {
				System.out.println("Most under-estimated frag:");
				System.out.println("\t" + mostOverUnderDelta[1] + "\t" + mostOverUnderFrag[1]);
			}
		}
		return absTotalDelta;
	}
	
	private void decreaseDeltaOccFreq(double changingRate, double tolerance) {
		for(HashMap<TSNodeLabel, double[]> subTable : frag_trainOF_Weight_UF_OF_Table.values()) {
			for(Entry<TSNodeLabel, double[]> f : subTable.entrySet()) {
				double[] val = f.getValue();
				double d = val[expOF_Index]-val[trainOF_Index];
				double dAbs = Math.abs(d);
				if (dAbs>tolerance) {
					double newWeight = val[weight_Index] - Math.signum(d)*changingRate*dAbs;
					if (newWeight>=0)
						val[weight_Index] = newWeight;
				}
			}
		}
	}
	
	public void getDeltaCycleJellePP(String filePathPrefix, int cycles, File outputFile) throws Exception {
		//PP_outputFINAL | 1.stsg
		PrintWriter pw = new PrintWriter(outputFile);
		for(int c=1; c<=cycles; c++) {
			if (c%100==0)
				System.out.println("Cycle " + c);
			File inputFile = new File(filePathPrefix + c + ".stsg");
			@SuppressWarnings("resource")
			Scanner scan = new Scanner(inputFile);
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				//(0.028585946 0.010003157 1 0.0 (B (C x) (D y)))
				if (line.isEmpty())
					continue;
				line = line.substring(1, line.length()-1);
				double weight = Double.parseDouble(line.split("\\s+", 3)[1]);
				String treeString = line.substring(line.indexOf('('));
				treeString = treeString.replace("x", "\"x\"").replace("y", "\"y\"");
				TSNodeLabel tree = new TSNodeLabel(treeString, false);
				frag_trainOF_Weight_UF_OF_Table.get(tree.label).get(tree)[weight_Index] = weight;
			}
			checkNormalizedWeights();
			getAnalyticCounts();
			double delta = getDeltaOccFreq(false);
			pw.println((c-1) + "\t" + delta);
		}
		pw.close();
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String usage = "java [-Xmx1G] tsg.PushAndPull.ExpectedFreqLargePTSG " +
				"-startSymb:S -trebankFile:{filepath} -fragFile:{filepath}" +
				"-outputDir:{filepath}" +
				"-changingRate:0.001 -maxCycle:100" +
				"-sigmaTableOnFile:false";
		
		if (args.length!=8) {
			System.err.println("Wrong number of parameters. Should be 8. ");
			System.out.println(usage);
			return;
		}
		
		String startSymbString = ArgumentReader.readStringOption(args[0]);
		String trees_path = ArgumentReader.readStringOption(args[1]);
		String frag_path = ArgumentReader.readStringOption(args[2]);
		File outputDir = new File(ArgumentReader.readStringOption(args[3]));		
		double changingRate = ArgumentReader.readDoubleOption(args[4]);
		int maxCycle = ArgumentReader.readIntOption(args[5]);		
		boolean buildSigmaTableOnFile = ArgumentReader.readBooleanOption(args[6]);
		
		
		startSymbol = Label.getLabel(startSymbString);
		File f_trees = new File(trees_path);
		File f_frags = new File(frag_path);
		//File f_sigmaTable = new File(sigmaTable_path);
		
		if (!outputDir.exists()) {
			System.err.println("Output dir does not exists");
			return;
		}
		
		String outputDirPath = outputDir + "/" + FileUtil.dateTimeString() + "/";
		outputDir = new File(outputDirPath);
		outputDir.mkdir();
		 
				
		ExpectedFreqLargePTSG EF = new ExpectedFreqLargePTSG(outputDirPath, f_trees, f_frags, buildSigmaTableOnFile);
		
		EF.getAnalyticCounts();
		System.out.println("Push and Pulling");
		EF.pushAndPull(changingRate, maxCycle);
		//EF.compareAnalyticalWithSimulatedFreq(0.003); 
		//EF.getDeltaCycleJellePP(filePathPrefix, 10000, PPJelleDeltaCycleFile);
	}

	

}
