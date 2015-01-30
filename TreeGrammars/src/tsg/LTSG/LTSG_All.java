package tsg.LTSG;

import java.util.*;

import settings.Parameters;
import tsg.TSNode;
import tsg.parser.*;
import util.*;

public class LTSG_All extends LTSG{
		
	public LTSG_All() {
		super();		
	}
	
	
	public Hashtable<String, LinkedList<String>> indexTreeByLexicon() {
		Hashtable<String,LinkedList<String>> indexedTrees = new Hashtable<String, LinkedList<String>>();
		for (Enumeration<String> e = template_freq.keys(); e.hasMoreElements() ;) {
			String template = e.nextElement();
			String lex = TSNode.get_unique_lexicon(template);
			lex = Utility.removeDoubleQuotes(lex);
			LinkedList<String> lexTable = (LinkedList<String>)(indexedTrees.get(lex));
			if (lexTable==null) {
				lexTable = new LinkedList<String>();
				indexedTrees.put(lex, lexTable);
			}
			lexTable.add(template);
		}
		return indexedTrees;
	}
	
	
	public static void main(String args[]) {
		Parameters.setDefaultParam();
		Parameters.removeTreesLimit = 1;
		
		Parameters.LTSGtype = "AllLexTrees";
		Parameters.outputPath = Parameters.resultsPath + "TSG/LTSG/" + Parameters.LTSGtype + "/";
		
		LTSG_All Grammar = new LTSG_All();
		Grammar.extractAllLexTrees();
		Grammar.treatTreeBank();			
		Grammar.toPCFG();
		
		Grammar.printTemplatesToFile();
		Grammar.printLexiconAndGrammarFiles();
		Grammar.checkNegativeFrequencies();		
		
		new Parser(Grammar);
	}
	
}
