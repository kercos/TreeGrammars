package tsg.parsingExp;

import tsg.TSNode;
import tsg.corpora.Tiger2;
import util.*;
import util.file.FileUtil;
import tigerAPI.*;

import java.io.*;
import java.util.*;

public class ParseTiger {

	public static File corpusXmlShort = Tiger2.TigerTBxmlShort;
	public static File corpusXml = Tiger2.TigerTBxml;
	public static File corpusPennShort = Tiger2.TigerTBPennShort;
	public static File corpusPenn = Tiger2.TigerTBPenn;
	

	
	public static void tigerXmlToPenn(File inputFile, File outputFile){
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		Corpus corpus = new Corpus(inputFile.toString());
		
		// All-sentences-loop
		PrintProgressStatic.start("Converting sentence: ");
		for (int i = 0; i < corpus.getSentenceCount(); i++) {
			PrintProgressStatic.next();
			Sentence s = corpus.getSentence(i);
			String id = s.getId();
			//if (id.equals("s46234")) continue;
			TSNode TSNsentence = parserSentence(s);
			if (TSNsentence==null) System.out.println(s.toString());
			else out.println(TSNsentence.toString());			
		}
		PrintProgressStatic.end();
		out.close();
	}


	private static TSNode parserSentence(Sentence s) {
		
		Hashtable<String, TSNode> idNode = new Hashtable<String, TSNode>();
		
		int terminalCount = s.getTCount();		
		// All terminals
		for(int i = 0; i < terminalCount; i++) {
			T t = s.getT(i);
			String id = t.getId();
			String word = t.getWord();
			word = word.replaceAll("\\(", "-LRB-");
			word = word.replaceAll("\\)", "-RRB-");
			String pos = t.getPos();
			pos = pos.replaceAll("\\(", "[");
			pos = pos.replaceAll("\\)", "]");
			TSNode lexical = TSNode.TSNodeLexical(word);
			TSNode posNode = new TSNode(pos, new TSNode[]{lexical});
			idNode.put(id, posNode);
			if (terminalCount==1) return posNode;

		}
		
		// All-NTs-loop
		int nonTerminalCount = s.getNTCount();
	    for (int i = 0; i < nonTerminalCount; i++) {
	    	NT nt = s.getNT(i);
	    	String id = nt.getId();
	    	String cat =  nt.getDerivedCat();	    	
	    	ArrayList<GraphNode> GNd = nt.getDaughters();
	    	int prole = GNd.size();
	    	TSNode[] daughters = new TSNode[prole];
	    	for(int j=0; j < prole; j++) {
	    		GraphNode d = GNd.get(j);
	    		String idref = d.getId();
	    		daughters[j] = idNode.get(idref);
	    		String edgeLabel = d.getEdge2Mother();
	    		daughters[j].label += "-" + edgeLabel;
	    	}
	    	TSNode catNode = new TSNode(cat, daughters);
			if (i == nonTerminalCount-1) return catNode;
			idNode.put(id, catNode);
	    }
		return null;
	}
	

	
	public static void main(String[] args){
		tigerXmlToPenn(corpusXmlShort, corpusPennShort);
	}

}
