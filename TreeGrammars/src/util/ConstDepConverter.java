package util;

import java.io.*;
import java.util.*;

import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import tsg.*;
import tsg.corpora.*;
import util.file.FileUtil;
import settings.*;

public class ConstDepConverter {

	public static void printMSTOutput(ConstCorpus inputPenn, File outputMaltFile, boolean blind, boolean labels) {
		PrintWriter writer = FileUtil.getPrintWriter(outputMaltFile);
		for(TSNode tree : inputPenn.treeBank) {
			writer.println(toMSTAnnotation(tree, blind, labels, false));
		}
		writer.close();
	}
	
	/**
	 * Return a string representation of the current TreeNode adherent to
	 * the MST format (dependencies).
	 * @return
	 */
	public static String toMSTAnnotation(TSNode TSN, boolean blind, boolean useLabels,
			boolean useArguments) {
		if (TSN.hasWrongHeadAssignment()) {
			System.err.println("Wrong head annotation for: " + TSN);
			return null;
		}		
		// word, postag, label, index
		String words = "";
		String postags = "";
		String labels = "";
		String indexes = "";
		List<TSNode> terminals = TSN.collectLexicalItems();
		for(TSNode leaf : terminals) {
			TSNode ancestor = leaf.parent;
			String dependentPOS = ancestor.label;
			words += leaf.label + "\t";
			while(ancestor.isHeadMarked() && !ancestor.isRoot()) ancestor = ancestor.parent;
			if (dependentPOS.endsWith("-A")) {
				dependentPOS = dependentPOS.substring(0, dependentPOS.length()-2);
			}
			if (useArguments && ancestor.isArgumentMarked()) {
				postags += dependentPOS + "-A\t";
			}
			else postags += dependentPOS + "\t";
			
			if (blind) {
				indexes += -1 + "\t";
				if (!useLabels) continue;
				labels += "LAB" + "\t";
				continue;
			}
			if (ancestor.isRoot()) {				
				indexes += 0 + "\t";
				if (!useLabels) continue;
				labels += "ROOT" + "\t";
			}
			else {
				String DLabel = ancestor.label;
				ancestor = ancestor.parent;
				String MLabel = ancestor.label;
				String HLabel = ancestor.getHeadDaughter().label;
				TSNode head = ancestor.getAnchorThroughPercolation();
				String headPOS = head.parent.label;
				int indexHead = terminals.indexOf(head) + 1;
				indexes += indexHead + "\t";
				if (!useLabels) continue;
				String depLabel = getMaltDependencyLabel(MLabel, DLabel, headPOS==HLabel);
				labels += depLabel + "\t";				
			}			 
		}
		String result = "";
		String[] resultArray = (useLabels) ? 
				new String[]{words, postags, labels, indexes} : 
				new String[]{words, postags, indexes};
		for(int i=0; i<resultArray.length; i++) {
			result += resultArray[i].trim() + "\n";
		}
		return result;		
	}
	
	/**
	 * Return a string representation of the current TreeNode adherent to
	 * the Malt-TAB format (dependencies).
	 * @return
	 */
	public static String toMaltTabAnnotation(TSNode TDN, int rootIndex, boolean blind, boolean labels) {
		if (!blind && TDN.hasWrongHeadAssignment()) {
			System.err.println("Wrong head annotation for: " + TDN);
			return null;
		}
		String result = "";
		List<TSNode> terminals = TDN.collectLexicalItems();
		for(TSNode leaf : terminals) {
			TSNode ancestor = leaf.parent;
			String dependentPOS = ancestor.label;
			if (blind) {
				result += leaf.label + "\t" + dependentPOS + "\n";
				continue;
			}
			while(ancestor.isHeadMarked() && !ancestor.isRoot()) ancestor = ancestor.parent;
			int indexHead = -1;
			String depLabel =  null;
			String LLabel = leaf.label;
			if (ancestor.isRoot()) {
				indexHead  = rootIndex;
				depLabel = "ROOT";
			}
			else {
				String DLabel = ancestor.label;
				ancestor = ancestor.parent;
				String MLabel = ancestor.label;
				String HLabel = ancestor.getHeadDaughter().label;				
				TSNode head = ancestor.getAnchorThroughPercolation();
				String headPOS = head.parent.label;
				indexHead = terminals.indexOf(head) + 1;
				depLabel = getMaltDependencyLabel(MLabel, DLabel, headPOS==HLabel);				
			}			 
			result += LLabel + "\t" + dependentPOS + "\t" + indexHead;
			if (labels) result += "\t" + depLabel;
			result += "\n";
		}
		return result;		
	}
	
	public static String getMaltDependencyLabel(String MLabel, String DLabel, boolean HLabelIsPOS) {		
		MLabel = TSNode.removeSemanticTag(MLabel);
		if (Utility.isPunctuation(DLabel)) return "P";		
		else if (DLabel.contains("-SBJ")) return "SUB";
		else if (DLabel.contains("-PRD")) return "PRD";
		else if (MLabel.equals("VP") && HLabelIsPOS && DLabel.equals("NP")) return "OBJ";
		else if (MLabel.equals("VP") && HLabelIsPOS && TSNode.removeSemanticTag(DLabel).equals("VP")) return "VC";
		else if (MLabel.equals("SBAR") && TSNode.removeSemanticTag(DLabel).equals("S")) return "SBAR";		
		else if (Utility.matchString(MLabel, new String[]{"VP","S","SQ","SINV","SBAR"})) return "VMOD";
		else if (Utility.matchString(MLabel, new String[]{"NP","NAC","NX","WHNP"})) return "NMOD";
		else if (Utility.matchString(MLabel, new String[]{"ADJP","ADVP","QP","WHADJP","WHADVP"})) return "AMOD";
		else if (Utility.matchString(MLabel, new String[]{"PP","WHPP"})) return "PMOD";
		else return "DEP";
	}
	
	public static void printMaltOutput(ConstCorpus inputPenn, File outputMaltFile, boolean blind) {
		inputPenn.correctHeadAnnotation();
		PrintWriter writer = FileUtil.getPrintWriter(outputMaltFile);
		for(TSNode tree : inputPenn.treeBank) {
			writer.println(toMaltTabAnnotation(tree, 0, blind, true));
		}
		writer.close();
	}
	
	public static void convertCollins97() {
		File ConstCollinsDir = new File(Wsj.WsjOriginalCleanedCollins97);
		File DepCollinsDir = new File(WsjD.WsjCOLLINS97);		
		DepCollinsDir.mkdirs();
		File[] fileList = ConstCollinsDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			File outputDepFile = new File(DepCollinsDir + "/" + inputFile.getName());
			PrintWriter output = FileUtil.getPrintWriter(outputDepFile);
			List<TSNode> constCorpus = new ConstCorpus(inputFile, "Collins", false).treeBank;
			for(TSNode tsn : constCorpus) {
				output.println(toMaltTabAnnotation(tsn, -1, false, false));												
			}
			output.close();
		}	
	}
	
	public static void convertCollins97_ulab() {
		File ConstCollinsDir = new File(Wsj.WsjOriginalCleanedCollins97);
		File DepCollinsDir = new File(WsjD.WsjCOLLINS97_ulab);		
		DepCollinsDir.mkdirs();
		File[] fileList = ConstCollinsDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			File outputDepFile = new File(DepCollinsDir + "/" + inputFile.getName());
			PrintWriter output = FileUtil.getPrintWriter(outputDepFile);
			List<TSNode> constCorpus = new ConstCorpus(inputFile, "Collins", false).treeBank;
			for(TSNode tsn : constCorpus) {
				output.println(toMSTAnnotation(tsn, false, false, false));											
			}
			output.close();
		}	
	}
	
	public static void convertCollins99_ulab() {
		File ConstCollinsDir = new File(Wsj.WsjOriginalCleanedCollins99);
		File DepCollinsDir = new File(WsjD.WsjCOLLINS99Arg_ulab);		
		DepCollinsDir.mkdirs();
		File[] fileList = ConstCollinsDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;
			File outputDepFile = new File(DepCollinsDir + "/" + inputFile.getName());
			PrintWriter output = FileUtil.getPrintWriter(outputDepFile);
			List<TSNode> constCorpus = new ConstCorpus(inputFile, "Collins", false).treeBank;
			for(TSNode tsn : constCorpus) {
				output.println(toMSTAnnotation(tsn, false, false, true));												
			}
			output.close();
		}	
	}
	
	public static void convertYM() {
		File constYMDir = new File(Wsj.WsjOriginalCleanedYM);
		File depYMDir = new File(WsjD.WsjDepBase + "YM_check");		
		depYMDir.mkdirs();
		File[] fileList = constYMDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			File outputDepFile = new File(depYMDir + "/" + inputFile.getName());
			PrintWriter output = FileUtil.getPrintWriter(outputDepFile);
			List<TSNode> constCorpus = new ConstCorpus(inputFile, "YM", false).treeBank;
			for(TSNode tsn : constCorpus) {
				output.println(toMaltTabAnnotation(tsn, -1, false, false));												
			}
			output.close();
		}	
	}
	
	public static void convertMSTulab() {
		File constYMDir = new File(Wsj.WsjOriginalCleanedYM);
		File depMSTDir = new File(WsjD.WsjDepBase + "MST_ulab");		
		depMSTDir.mkdirs();
		File[] fileList = constYMDir.listFiles();
		Arrays.sort(fileList);
		for(File inputFile : fileList) {
			File outputDepFile = new File(depMSTDir + "/" + inputFile.getName());
			PrintWriter output = FileUtil.getPrintWriter(outputDepFile);
			List<TSNode> constCorpus = new ConstCorpus(inputFile, "YM", false).treeBank;
			for(TSNode tsn : constCorpus) {
				output.println(toMSTAnnotation(tsn, false, false, false));												
			}
			output.close();
		}	
	}
	
	public static void convertMagerman() {
		File constYMDir = new File(Wsj.WsjOriginalCleanedMagerman);
		File depMSTDir = new File(WsjD.WsjDepBase + "Magerman");		
		depMSTDir.mkdirs();
		File[] fileList = constYMDir.listFiles();
		Arrays.sort(fileList);
		Parameters.logFile = new File (depMSTDir + "/conversionLog.txt");
		for(File inputFile : fileList) {
			File outputDepFile = new File(depMSTDir + "/" + inputFile.getName());
			PrintWriter output = FileUtil.getPrintWriter(outputDepFile);			
			ConstCorpus constCorpus = new ConstCorpus(inputFile, "YM", false);
			constCorpus.correctHeadAnnotation();
			for(TSNode tsn : constCorpus.treeBank) {
				output.println(toMaltTabAnnotation(tsn, -1, false, false));												
			}
			output.close();
		}	
	}
	
	public static void convertNaive() {
		File constYMDir = new File(Wsj.WsjOriginalCleanedRandom);
		File depMSTDir = new File(WsjD.WsjDepBase + "Random");		
		depMSTDir.mkdirs();
		File[] fileList = constYMDir.listFiles();
		Arrays.sort(fileList);
		Parameters.logFile = new File (depMSTDir + "/conversionLog.txt");
		for(File inputFile : fileList) {
			File outputDepFile = new File(depMSTDir + "/" + inputFile.getName());
			PrintWriter output = FileUtil.getPrintWriter(outputDepFile);			
			ConstCorpus constCorpus = new ConstCorpus(inputFile, "Naive", false);
			for(TSNode tsn : constCorpus.treeBank) {
				output.println(toMaltTabAnnotation(tsn, -1, false, false));												
			}
			output.close();
		}	
	}
	
	public static void tabToYMConverter() {
		String path = "/scratch/fsangati/CORPUS/WSJ/DEPENDENCY/Entropy/";
		
		File inFile =  new File(path + "wsj-02-11.tab");
		//File inFile =  new File(path + "wsj-22.tab");
		
		File outFile =  new File(path + "wsj-02-11.dep");
		//File outFile =  new File(path + "wsj-22.dep");
		
		List<TDNode> depCorpus = DepCorpus.readTreebankFromFileMALT(inFile, 1000);
		PrintWriter output = FileUtil.getPrintWriter(outFile);	
		for(TDNode tdn : depCorpus) {
			output.println(tdn.toStringMALTulab(-1, false, false));												
		}
		output.close();
		
		
	}
	
	public static void main_old(String args[]) {
		File inputPennCorpus = 
			new File("/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/ORIGINAL_COLLINS_97/wsj-22.mrg");
		File ouputNoTraceCorpus = 
			new File("/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/COLLINS_97_NOTRACES/wsj-22.mrg");
		ConstCorpus corpus = new ConstCorpus(inputPennCorpus, "noname");
		corpus.removeTraces(Wsj.traceTag);
		corpus.removeNumbersInLables();
		corpus.removeRedundantRules();
		corpus.toFile_Complete(ouputNoTraceCorpus, false, false);		
	}
		
	
	public static void main(String args[]) {
		//convertMSTulab();
		//convertMagerman();
		//convertNaive();
		//tabToYMConverter();
		convertCollins99_ulab();
		//convertCollins97_ulab();
	}
	
}
	