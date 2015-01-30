package util;

import java.io.*;
import java.util.*;

import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tsg.TSNode;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import util.file.FileUtil;

public class DepConstConverter {

	/**
	 * Assign the heads in the current TreeNode so that the head in input will dominate
	 * the dependent in input.
	 */
	public static void assignHeadsFromDependencyPair(TSNode TSN, int indexHead, int indexDependent) {
		List<TSNode> terminals = TSN.collectTerminals();
		TSNode head = terminals.get(indexHead);
		TSNode dependent = terminals.get(indexDependent);
		TSNode lowestCommonParent = TSN.lowestCommonParent(head, dependent);
		head = head.parent;
		while(head!=lowestCommonParent) {
			head.headMarked = true;
			head = head.parent;
		}		
	}
	
	/**
	 * Assign the heads in the current TreeNode given the dependency over
	 * the set of wordBoundary.
	 * If the sentence has 5 words the wordBoundary can look like
	 * {{0},{1},{2},{3},{4}}
	 * {{0},{1,2},{3},{4}}
	 * The dependency set always contains pair of indexes of the wordBoundary subSets {dependent, head}
	 * {{1,2},{1,3},{2,4},{4,3}}
	 */
	public static boolean assignHeadsFromDependencyTable(TSNode TSN, int[][] wordBoundary, 
			int[][] dependency, String[] output) {
		//this.removeQuotationMarks();
		if (dependency==null) {
			TSN.fixUnaryHeadConsistency();
			return (TSN.hasWrongHeadAssignment());
		}
		TSNode[] wordBoundaryNodes = new TSNode[wordBoundary.length];		
		TSNode[] terminals = TSN.collectTerminals().toArray(new TSNode[]{});
		int[] lastBlock = wordBoundary[wordBoundary.length-1];
		int lastIndex = lastBlock[lastBlock.length-1];
		if(lastIndex!=terminals.length-2  && !terminals[terminals.length-1].label.equals("''")) {
			int i = 0;
			i++;
		}
		for(int i=0; i<wordBoundary.length; i++) {
			if (wordBoundary[i].length==1) wordBoundaryNodes[i] = terminals[wordBoundary[i][0]].parent;
			else {
				TSNode[] terminalBlock = new TSNode[wordBoundary[i].length];
				for(int j=0; j<wordBoundary[i].length; j++) terminalBlock[j] = terminals[wordBoundary[i][j]];
				wordBoundaryNodes[i] = TSN.lowestCommonParent(terminalBlock);
			}
			output[0] += wordBoundaryNodes[i].collectTerminals().toString() + " ";
		}
		output[0] += "\n";
		for(int d=0; d<dependency.length; d++) {
			if (dependency[d][1] == dependency[d][0]) continue;
			TSNode head = wordBoundaryNodes[dependency[d][1]];
			TSNode dependent = wordBoundaryNodes[dependency[d][0]];
			output[0] += head.collectTerminals().toString() + " --> " + dependent.collectTerminals().toString() + "\n";
			TSNode lowestCommonParent = TSN.lowestCommonParent(head, dependent);
			TSNode markedDaughter = lowestCommonParent.markedDaughter();
			if (markedDaughter!=null) {
				TSNode competitor = markedDaughter;
				int wordBoundaryNodeIndexCompetitor = Utility.indexOf(competitor, wordBoundaryNodes);
				while(wordBoundaryNodeIndexCompetitor==-1) {
					competitor = competitor.markedDaughter();
					wordBoundaryNodeIndexCompetitor = Utility.indexOf(competitor, wordBoundaryNodes);
				} 
				if (wordBoundaryNodeIndexCompetitor == dependency[d][1]) continue;
				int[] dep = new int[]{dependency[d][1], wordBoundaryNodeIndexCompetitor};
				if (Utility.indexOf(dep, dependency)!=-1) continue;
				markedDaughter.headMarked = false;
			}
			while(head!=lowestCommonParent) {
					head.headMarked = true;
					head = head.parent;
			}
		}
		TSN.fixUnaryHeadConsistency();
		return (!TSN.hasWrongHeadAssignment());
	}
	
	/**
	 * Assign the heads in the current TreeNode given the dependency over
	 * the set of terminals.
	 * The dependency set always contains pair of indexes of the terminals {head, dependent}
	 * {{1,2},{1,3},{2,4},{4,3}}
	 */
	public static boolean assignHeadsFromDependencyTable(TSNode TSN, int[][] dependency, String[] output) {
		if (dependency==null) {
			TSN.fixUnaryHeadConsistency();
			return (!TSN.hasWrongHeadAssignment());
		}		
		TSNode[] terminals = TSN.collectTerminals().toArray(new TSNode[]{});
		for(int d=0; d<dependency.length; d++) {
			if (dependency[d][0] == dependency[d][1]) {
				continue;
			}
			if (dependency[d][0]>terminals.length-1 || dependency[d][1]>terminals.length-1) {
				continue;
			}
			if (dependency[d][0]<0 || dependency[d][1]<0) {
				continue;
			}
			TSNode head = terminals[dependency[d][0]];
			TSNode dependent = terminals[dependency[d][1]];
			output[0] += head.toString() + " (" + dependency[d][0] + ") --> " + dependent.toString() +  
							" (" + dependency[d][1] +")\n";
			TSNode lowestCommonParent = TSN.lowestCommonParent(head, dependent);
			head = head.parent;
			while(head!=lowestCommonParent) {
					head.headMarked = true;
					head = head.parent;
			}
		}
		TSN.fixUnaryHeadConsistency();
		return (!TSN.hasWrongHeadAssignment());
	}
	
	public static void dep2const(TSNode TSN, TDNode TDN, int index, PrintWriter log) {
		TDNode[] structure = TDN.getStructureArray();
		TSNode[] terminals = TSN.collectLexicalItems().toArray(new TSNode[]{});
		
		if (structure.length != terminals.length) {
			String errorMessage = "Error in dep2const: length unmatched at sentence" + index;
			if (log!=null) log.println(errorMessage);
			else System.err.println(errorMessage);
			return;
		}
		if (structure.length==1) {
			TSN.fixUnaryHeadConsistency();
			return;
		}				
		for(TDNode tdn : structure) {
			TDNode parent  = tdn.parent;			
			if (parent==null) {
				//terminals[tdn.index].markHeadPathToTop();
				continue;
			}				
			TSNode head = terminals[parent.index];
			TSNode dependent = terminals[tdn.index];			
			TSNode lowestCommonParent = TSN.lowestCommonParent(head, dependent);
			head = head.parent;
			while(head!=lowestCommonParent) {
					head.headMarked = true;
					head = head.parent;
			}
		}
		TSN.fixUnaryHeadConsistency();
		if (TSN.hasWrongHeadAssignment()) {
			String errorMessage = "Error in dep2const: wrong head assignment at sentence " + index;
			if (log!=null) log.println(errorMessage);
			else System.err.println(errorMessage);
			return;
		}
	}
	
	public static void convertConll07(){
		File DepConll07Dir = new File("/scratch/fsangati/CORPUS/WSJ/DEPENDENCY/CONLL07/");		
		File ConstConll07Dir = new File(Wsj.WsjOriginalNPBraketingCleanedConll07);
		File ConstCleanedDir = new File(Wsj.WsjOriginalNPBraketingCleaned);
		ConstConll07Dir.mkdirs();
		File[] fileDepList = DepConll07Dir.listFiles();
		File[] fileConstList = ConstCleanedDir.listFiles();
		Arrays.sort(fileDepList);
		Arrays.sort(fileConstList);
		for(int i=0; i<fileDepList.length; i++) {
			File inputDepFile = fileDepList[i];
			File inputConstFile = fileConstList[i];
			File outputConstFile = new File(ConstConll07Dir + "/" + inputConstFile.getName());
			File outputConstFileLog = new File(ConstConll07Dir + "/" + inputConstFile.getName() + ".log");
			PrintWriter output = FileUtil.getPrintWriter(outputConstFile);
			PrintWriter logOutput = FileUtil.getPrintWriter(outputConstFileLog);
			List<TDNode> depCorpus = DepCorpus.readTreebankFromFileConll07(inputDepFile, 1000);
			List<TSNode> constCorpusCleaned = new ConstCorpus(inputConstFile, "OrigNPBracketing", false).treeBank;
			System.out.println(inputConstFile.getName());
			for(int j=0; j<depCorpus.size(); j++) {
				System.out.println(j);
				TDNode tdn = depCorpus.get(j);
				TSNode tsn = constCorpusCleaned.get(j);
				dep2const(tsn, tdn, j, logOutput);
				output.println(tsn.toString(true, false));
			}
			output.close();
			logOutput.close();
		}			
	}
	
	public static void convertYM(){
		File DepYMDir = new File("/scratch/fsangati/CORPUS/WSJ/DEPENDENCY/YM/");
		File ConstYMDir = new File("/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/YM/");
		File ConstCleanedDir = new File(Wsj.WsjOriginalCleaned);
		ConstYMDir.mkdirs();
		File[] fileDepList = DepYMDir.listFiles();
		File[] fileConstList = ConstCleanedDir.listFiles();
		Arrays.sort(fileDepList);
		Arrays.sort(fileConstList);
		for(int i=0; i<fileDepList.length; i++) {
			File inputDepFile = fileDepList[i];
			File inputConstFile = fileConstList[i];
			File outputConstFile = new File(ConstYMDir + "/" + inputConstFile.getName());
			File outputConstFileLog = new File(ConstYMDir + "/" + inputConstFile.getName() + ".log");
			PrintWriter output = FileUtil.getPrintWriter(outputConstFile);
			PrintWriter logOutput = FileUtil.getPrintWriter(outputConstFileLog);
			List<TDNode> depCorpus = DepCorpus.readTreebankFromFileYM(inputDepFile, 1000);
			List<TSNode> constCorpusCleaned = new ConstCorpus(inputConstFile, "OrigNPBracketing", false).treeBank;
			System.out.println(inputConstFile.getName());
			for(int j=0; j<depCorpus.size(); j++) {
				System.out.println(j);
				TDNode tdn = depCorpus.get(j);
				TSNode tsn = constCorpusCleaned.get(j);
				dep2const(tsn, tdn, j, logOutput);
				output.println(tsn.toString(true, false));
			}
			output.close();
			logOutput.close();
		}			
	}
	
	public static void convertMalt(){
		File DepYMDir = new File("/scratch/fsangati/CORPUS/WSJ/DEPENDENCY/PENN2MALT");
		File ConstYMDir = new File("/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/PENN2MALT/");
		File ConstCleanedDir = new File(Wsj.WsjOriginalCleaned);
		ConstYMDir.mkdirs();
		File[] fileDepList = DepYMDir.listFiles();
		File[] fileConstList = ConstCleanedDir.listFiles();
		Arrays.sort(fileDepList);
		Arrays.sort(fileConstList);
		for(int i=0; i<fileDepList.length; i++) {
			File inputDepFile = fileDepList[i];
			File inputConstFile = fileConstList[i];
			File outputConstFile = new File(ConstYMDir + "/" + inputConstFile.getName());
			File outputConstFileLog = new File(ConstYMDir + "/" + inputConstFile.getName() + ".log");
			PrintWriter output = FileUtil.getPrintWriter(outputConstFile);
			PrintWriter logOutput = FileUtil.getPrintWriter(outputConstFileLog);
			List<TDNode> depCorpus = DepCorpus.readTreebankFromFileMALT(inputDepFile, 1000);
			List<TSNode> constCorpusCleaned = new ConstCorpus(inputConstFile, "OrigNPBracketing", false).treeBank;
			System.out.println(inputConstFile.getName());
			for(int j=0; j<depCorpus.size(); j++) {
				System.out.println(j);
				TDNode tdn = depCorpus.get(j);
				TSNode tsn = constCorpusCleaned.get(j);
				dep2const(tsn, tdn, j, logOutput);
				output.println(tsn.toString(true, false));
			}
			output.close();
			logOutput.close();
		}			
	}
	
	public static void main(String args[]) {
		//convertConll07();
		//convertYM();
		//convertMalt();
		File DepConll07_15 = new File("/scratch/fsangati/CORPUS/WSJ/DEPENDENCY/CONLL07/wsj-00_15.tab");
		List<TDNode> depCorpus = DepCorpus.readTreebankFromFileConll07(DepConll07_15, 1000);
		TSNode TSN = new TSNode("(S (PP-LOC (IN-H Among) (NP (NP-H (CD 33) (NNS-H men)) (SBAR (WHNP (WP-H who)) (S-H (VP-H (VBD-H worked) (ADVP-MNR (RB-H closely)) (PP-CLR (IN-H with) (NP (DT the) (NN-H substance)))))))) (, ,) (NP-SBJ-H (NP-H (CD-H 28))) (VP-H (VBP-H have) (VP (VBN-H died) (: --) (NP (QP (JJ more) (IN than) (CD three) (NNS-H times)) (DT the) (VBN expected) (NN-H number)))) (. .))");
		TDNode TDN = depCorpus.get(0);
		dep2const(TSN, TDN, 0, null);
	}
	
}

