package tsg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import tsg.corpora.Wsj;
import util.Utility;
import util.file.FileUtil;

public class PCFG_extractor {

	public static void extractPCFG(File treebankFile, File grammarOutputFile) throws Exception {
		//ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		ArrayList<TSNodeLabel> treebank = Wsj.getTreebank(treebankFile);
		Hashtable<String, int[]> cfgFreq = new Hashtable<String, int[]>();
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();		
				Utility.increaseStringIntArray(cfgFreq, rule);
			}
		}
		PrintWriter pw = FileUtil.getPrintWriter(grammarOutputFile);
		for(Entry<String, int[]> e : cfgFreq.entrySet()) {
			pw.println(e.getKey() + "\t" + e.getValue()[0]);			
		}
		pw.close();
	}
	
	public static void serachRecursiveRules(File treebankFile) throws Exception {
		//ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(treebankFile);
		ArrayList<TSNodeLabel> treebank = Wsj.getTreebank(treebankFile);		
		for(TSNodeLabel t : treebank) {
			ArrayList<TSNodeLabel> nodes = t.collectAllNodes();
			ArrayList<String> recursiveRules = new ArrayList<String>();
			boolean recursive = false;
			for(TSNodeLabel n : nodes) {
				if (n.isLexical) continue;
				String rule = n.cfgRule();		
				String[] ruleParts = rule.split("\\s");
				String lhs = ruleParts[0];				
				for(int i=1; i<ruleParts.length; i++) {
					if (ruleParts[i].equals(lhs)) {
						recursive = true;
						recursiveRules.add(rule);
						break;
					}
				}
			}
			if (recursive) {
				int lenght = t.countLexicalNodes();
				if (lenght>15) continue;
				System.out.println();
				System.out.println(t.toString());
				System.out.println(t.toFlatSentence());
				System.out.println(recursiveRules.toString());				
			}
		}				
	}

	public static void main(String[] args) throws Exception {
		String usage = "java -jar PCFG_extractor.jar treebankFile outputfile";
		if (args.length!=2) {
			System.err.println("Usage: " + usage);
			return;
		}
		File treebankFile = new File(args[0]);
		File grammarOutputFile = new File(args[1]);
		//File treebankFile = new File(Wsj.WsjOriginalCleaned + "wsj-00.mrg");
		//File grammarOutputFile = new File("built/prova.txt");
		extractPCFG(treebankFile, grammarOutputFile);
		//serachRecursiveRules(treebankFile);
		//FileUtil.sortFile(grammarOutputFile);		
	}
		
	
}
