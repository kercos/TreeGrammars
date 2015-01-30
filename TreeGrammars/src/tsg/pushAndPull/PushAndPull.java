package tsg.pushAndPull;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Scanner;

import tsg.Label;
import tsg.ParenthesesBlockPenn;
import tsg.TSNode;
import tsg.TSNodeLabel;
import util.Utility;

public class PushAndPull {

	static Label startSymbol = Label.getLabel("S");
	
	HashMap<Label, HashMap<TSNodeLabel, double[]>> fragTable;
	
	public PushAndPull(File inputFile) throws Exception {
		fragTable = new HashMap<Label, HashMap<TSNodeLabel, double[]>>();
		readFile(inputFile);	
		//makeUniformDistribution();
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
			double freq = Double.parseDouble(parts[1]);
			Utility.putInMapDouble(fragTable, rule.label, rule, new double[]{freq});
			
		}		
	}
	
	private void makeUniformDistribution() {
		for(HashMap<TSNodeLabel, double[]> subTable : fragTable.values()) {
			int size = subTable.size();
			for(double[] v : subTable.values()) {
				v[0] = 1d/size;
			}
		}
	}
	
	
	private void getFragmentGeneratingSigma(TSNodeLabel sigma) {
		for(HashMap<TSNodeLabel, double[]> subTable : fragTable.values()) {
			for(TSNodeLabel frag : subTable.keySet()) {
				if (containsRecursiveSigma(frag, sigma)) {
					System.out.println(frag);
				}
			}
		}		
	}
	
	public static boolean containsRecursiveSigma(TSNodeLabel t, TSNodeLabel sigma) {
		if (containsNonRecursiveSigma(t,sigma)) return true;
		if (t.isTerminal()) 
			return sigma.headMarked;
		for(TSNodeLabel d : t.daughters) {
			if (containsNonRecursiveSigma(d, sigma)) return true;
		}
		return false;
	}
	
	public static boolean containsNonRecursiveSigma(TSNodeLabel t, TSNodeLabel sigma) {
		if (t.label.equals(sigma.label)) {
			if (sigma.isTerminal()) {
				return !sigma.headMarked || t.isTerminal(); 
			}
			if (t.isTerminal()) return false;
			int prole = t.prole();
			if (prole!=sigma.prole()) return false;			
			for(int i=0; i<prole; i++) {
				TSNodeLabel tDaughter = t.daughters[i];
				TSNodeLabel sDaughter = sigma.daughters[i];
				if (!containsNonRecursiveSigma(tDaughter, sDaughter)) 
					return false;
			}
			return true;			
		}
		return false;
	}
	
	static void acquireNonTerminalLabels(TSNodeLabel t) {
		String l = t.label();
		if (l.endsWith("-H")) {
			t.headMarked = true;
			t.label = Label.getLabel(l.substring(0, l.length()-2));
		}
		if (t.isTerminal())
			return;
		for(TSNodeLabel d : t.daughters) {
			acquireNonTerminalLabels(d);
		}
	}
	

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String workingDir = "/Users/fedja/Work/JointProjects/Jelle/PnP/";
		File f_trees = new File(workingDir + "ExpFreqExampleGrammarPTSG_trees.txt");		
		File f_frags = new File(workingDir + "ExpFreqExampleGrammarPTSG_frags.txt");
		//getAllFragsFreq(f_trees, f_frags);
		
		PushAndPull PP = new PushAndPull(f_frags);
		TSNodeLabel sigma = new TSNodeLabel("(B-H)", false);
		//TSNodeLabel sigma = new TSNodeLabel("(B C D-H)", false);
		//TSNodeLabel sigma = new TSNodeLabel("(B C (D x))", false);		
		acquireNonTerminalLabels(sigma);
		PP.getFragmentGeneratingSigma(sigma);
		//EF.generateRandomTreeTest();				
		//EF.getNonTermCharge();		
	}

	

	

}
