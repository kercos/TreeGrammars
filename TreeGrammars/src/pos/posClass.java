package pos;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import tsg.TSNode;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import util.Utility;
import util.file.FileUtil;

public class posClass {
	
	Hashtable<String, Hashtable<String, Integer>> posLexiconTable;
	
	public posClass(ConstCorpus trainingCorpus, File outputFile) {
		posLexiconTable =  new Hashtable<String, Hashtable<String, Integer>>();
		HashSet<String> allPos = new HashSet<String>();
		
		for(TSNode t : trainingCorpus.treeBank) {
			List<TSNode> lexicalNodes = t.collectLexicalItems();
			for(TSNode lN : lexicalNodes) {
				String lex = lN.label().toLowerCase();
				String pos = lN.parent.label();
				allPos.add(pos);
				addPosLex(lex, pos);
			}
		}
		
		HashSet<String> openClass = new HashSet<String>();
		for (String pos : posLexiconTable.keySet()) {
			Hashtable<String, Integer> posTable = posLexiconTable.get(pos);
			if (posTable.size() > 50) openClass.add(pos);
		}
		
		HashSet<String> closeClass = new HashSet<String>(allPos);
		closeClass.removeAll(openClass);				
		System.out.println("Open Class of Pos: " + openClass);
		System.out.println("Close Class of Pos: " + closeClass);
		printStructureToFile(outputFile);
	}

	private void printStructureToFile(File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		for (String pos : posLexiconTable.keySet()) {
			pw.println(pos);
			Hashtable<String, Integer> posTable = posLexiconTable.get(pos);
			FileUtil.printHashtableToPwOrder(posTable, pw);
			pw.println("---------------------------------");
		}		
		pw.close();
	}

	private void addPosLex(String lex, String pos) {
		Hashtable<String, Integer> posTable = posLexiconTable.get(pos);		
		if (posTable==null) {
			posTable = new Hashtable<String, Integer>();
			posLexiconTable.put(pos, posTable);
		}
		Utility.increaseInTableInteger(posTable, lex, 1);				
	}
	
	public static void main(String[] args) {
		String baseDir = Wsj.WsjOriginalCleaned;
		File trainingFile = new File(baseDir + "wsj-02-21.mrg");
		ConstCorpus trainingCorpus = new ConstCorpus(trainingFile, "training");
		File output = new File("/home/fsangati/Desktop/posTagClass.txt");
		new posClass(trainingCorpus, output);
	}
	
}
