package tsg.parsingExp;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import tsg.TSNodeLabel;
import tsg.TSNodeLabelFreqDouble;
import util.file.FileUtil;

public class ConvertFragmentsToCFGRulesDouble {
	
	public Hashtable<String, TreeSet<TSNodeLabelFreqDouble>> ruleFragmentsTable;
		
	//static NumberFormat formatter = new DecimalFormat("###.##########");;
	Vector<TSNodeLabel> ambiguousCFGmappingIndex;
	Hashtable<String, TSNodeLabel> unambiguousCFGmapping;
	File grammarFile, ambiguousFragmentsFile, outputFile;
	String internalFakeNodeLabel;	
	String fakePrelexPrefix;
		
	public ConvertFragmentsToCFGRulesDouble(File grammarFile, File ambiguousFragmentsFile, File outputFile,
			String internalFakeNodeLabel, String fakePrelexPrefix) {
		
		this.grammarFile = grammarFile;
		this.ambiguousFragmentsFile = ambiguousFragmentsFile;
		this.outputFile = outputFile;
		this.internalFakeNodeLabel = internalFakeNodeLabel;
		this.fakePrelexPrefix = fakePrelexPrefix;
		
		buildRuleFragmentsTable();		
		dismabiguateMappingAndPrintPCFG();
		
	}	
	
	public Hashtable<String, TreeSet<TSNodeLabelFreqDouble>> getRuleFragmentsTable() {
		return ruleFragmentsTable;
	}
	
	public int getCFGtypes() {
		return ruleFragmentsTable.size();
	}
	
	public Vector<TSNodeLabel> getAmbiguousCFGmapping() {
		return ambiguousCFGmappingIndex;
	}
	
	public Hashtable<String,TSNodeLabel> getunambiguousCFGmapping() {
		return unambiguousCFGmapping;		
	}	

	private void buildRuleFragmentsTable() {
		ruleFragmentsTable = new Hashtable<String, TreeSet<TSNodeLabelFreqDouble>>();
		Scanner scan = FileUtil.getScanner(grammarFile);
		while(scan.hasNextLine()) {
			String fragmentLine = scan.nextLine();
			String[] lineSplit = fragmentLine.split("\t");
			String fragmentString = lineSplit[0];
			double freq = Double.parseDouble(lineSplit[1]);			
			TSNodeLabel fragment = null;
			try {
				fragment = new TSNodeLabel(fragmentString, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			String treeCFGrule = compressToCFGRulePosLex(fragment);					
			add(treeCFGrule, new TSNodeLabelFreqDouble(fragment, freq));			
		}		
	}

	private String compressToCFGRulePosLex(TSNodeLabel thisNode) {
		StringBuilder sb = new StringBuilder(thisNode.label());
		fillStringBuilderWithTerminalsPosLex(thisNode, sb);
		return sb.toString();
	}
	
	private void fillStringBuilderWithTerminalsPosLex(TSNodeLabel thisNode, StringBuilder sb) {
		if (thisNode.isLexical) {
			sb.append(" " + fakePrelexPrefix + thisNode.label());
			return;
		}
		if (thisNode.isTerminal()) {
			sb.append(" " + thisNode.label());
			return;
		} 
		for(TSNodeLabel d  : thisNode.daughters) {
			fillStringBuilderWithTerminalsPosLex(d, sb);
		}
	}
	
	public void add(String treeCFGrule, TSNodeLabelFreqDouble treeFreq) {		
		TreeSet<TSNodeLabelFreqDouble> value = ruleFragmentsTable.get(treeCFGrule);
		if (value==null) {
			value = new TreeSet<TSNodeLabelFreqDouble>();			
			ruleFragmentsTable.put(treeCFGrule, value);			
		}
		value.add(treeFreq);
	}
	
	private void dismabiguateMappingAndPrintPCFG() {
		ambiguousCFGmappingIndex = new Vector<TSNodeLabel>();
		unambiguousCFGmapping = new Hashtable<String, TSNodeLabel>();
		PrintWriter pwAmbiguous = FileUtil.getPrintWriter(ambiguousFragmentsFile);
		PrintWriter pwPCFG = FileUtil.getPrintWriter(outputFile);
		pwAmbiguous.println("List of rules mapping to multiple fragments:\n");
		int uniqueAmbiguousFragmentIndex = 0;
		for(Entry<String, TreeSet<TSNodeLabelFreqDouble>> e :ruleFragmentsTable.entrySet()) {
			String cfgRule = e.getKey();
			TreeSet<TSNodeLabelFreqDouble> sortedFragmentsSet = e.getValue();
			if (sortedFragmentsSet.size()==1) {
				TSNodeLabelFreqDouble uniqueFragmentFreq = sortedFragmentsSet.first();
				TSNodeLabel uniqueFragment = uniqueFragmentFreq.tree();
				double freq = uniqueFragmentFreq.freq();
				unambiguousCFGmapping.put(cfgRule, uniqueFragment);
				pwPCFG.println(freq + "\t" + cfgRule);
				continue;
			}
			pwAmbiguous.println(cfgRule);
			Iterator<TSNodeLabelFreqDouble> discIter = sortedFragmentsSet.descendingIterator();
			int firstSpace = cfgRule.indexOf(' ');
			String lhs = cfgRule.substring(0,firstSpace);
			String rhs = cfgRule.substring(firstSpace+1);
			while(discIter.hasNext()) {
				TSNodeLabelFreqDouble fragmentFreq = discIter.next();
				TSNodeLabel fragment = fragmentFreq.tree();
				double freq = fragmentFreq.freq();
				String uniqueLabel = internalFakeNodeLabel + uniqueAmbiguousFragmentIndex++;
				pwAmbiguous.println(uniqueLabel + "\t" + fragmentFreq.toString(false, true));
				ambiguousCFGmappingIndex.add(fragment);	
				String firstRuleUnique = lhs + " " + uniqueLabel;
				String secondRuleUnique = uniqueLabel + " " + rhs;
				//pwPCFG.println(formatter.format(freq) + "\t" + firstRuleUnique);
				pwPCFG.println(freq + "\t" + firstRuleUnique);
				pwPCFG.println(1 + "\t" + secondRuleUnique);
			}
			
			pwAmbiguous.println();
		}
		pwAmbiguous.close();	
		pwPCFG.close();
	}
	
	
	

	public static void main(String[] args) throws Exception {
	}
	
}
