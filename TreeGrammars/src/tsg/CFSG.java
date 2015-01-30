package tsg;


import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.corpora.*;
import tsg.parser.*;
import util.*;
import util.file.FileUtil;

public class CFSG <T extends Number> {
	
	public Hashtable<String, T> lexRules, internalRules;
	
	public CFSG() {		
		Parameters.newRun(this);
		lexRules = new Hashtable<String, T>();
		internalRules  = new Hashtable<String, T>();
	}
	
	public CFSG(Hashtable<String, T> lex, Hashtable<String, T> gram) {
		this.lexRules = lex;
		this.internalRules = gram;
	}
	
	public void readCFGFromCorpus() {		
		for(TSNode TreeLine : Parameters.trainingCorpus.treeBank) {
			readCFGFromTreeLine(TreeLine);
		}
		String log = "Read rules from corups. \n # Internal Rules: " + internalRules.size() 
						+ "\n # Lex Rules: " + lexRules.size();
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	public void readCFGFromTreeLine(TSNode TreeLine) {
		if (Parameters.toNormalForm) TreeLine.toNormalForm();
		List<TSNode> nonLexicalNodes = TreeLine.collectNonLexicalNodes();
		for(TSNode nonTerminal : nonLexicalNodes) {
			Hashtable<String, T> table = (nonTerminal.isPrelexical()) ? lexRules : internalRules;
			String rule = nonTerminal.toCFG(false);
			Utility.increaseStringInteger(table, rule, 1);
		}
	}
	
	public Hashtable<String,Hashtable<String,T>> buildLexCatTable() {
		Hashtable<String,Hashtable<String,T>> lexCat = new Hashtable<String,Hashtable<String,T>>();
		for (Enumeration<String> e = lexRules.keys(); e.hasMoreElements() ;) {
			String rule = (String)e.nextElement();
			T count = lexRules.get(rule);
			String[] category_lexicon = rule.split(" ");
			String cat = category_lexicon[0];
			String lex = category_lexicon[1];			
			Hashtable<String,T> lexTable = lexCat.get(lex);
			if (lexTable == null) {
				lexTable = new Hashtable<String,T>();
				lexCat.put(lex, lexTable);								
			}
			if (lexTable.put(cat, count)!=null) System.out.println("Error!!");
		}
		return lexCat;
	}
	
	/**
	 * Print out two files lex_ambiguous and lex_ambiguous containing
	 * the words in the training corpus with ambiguous POS and unambiguous POS respectively.
	 */
	public void processLexicon() {
		File ambiguousFile = new File(Parameters.outputPath + "lex_ambiguous");
		File unambiguousFile = new File(Parameters.outputPath + "lex_unambiguous");
		PrintWriter amb = FileUtil.getPrintWriter(ambiguousFile);
		PrintWriter unamb = FileUtil.getPrintWriter(unambiguousFile);
		Hashtable<String,Hashtable<String,T>> lexCat = buildLexCatTable();
		
		for (Enumeration<String> e = lexCat.keys(); e.hasMoreElements() ;) {
			String lex = e.nextElement();
			Hashtable<String,T> lexTable = lexCat.get(lex);
			HashSet<String> categories = new HashSet<String>();
			for (Enumeration<String> f = lexTable.keys(); f.hasMoreElements() ;) {
				String cat = f.nextElement();
				cat = cat.replaceAll("@\\d+", "");
				categories.add(cat);
			}
			if (categories.size()==1) unamb.write(lex + "\n");
			else amb.write(lex + "\n");
		}
		amb.close();
		unamb.close();
	}
	
	/**
	 * Check if the internal and lexical frequency tables contains
	 * negative frequencies.
	 */
	public void checkNegativeFrequencies() {
		for(int i=0; i<2; i++) {
			Hashtable<String,T> table = (i==0) ? internalRules : lexRules;			
			for (Enumeration<String> e = table.keys(); e.hasMoreElements() ;) {
				String rule = (String)e.nextElement();			
				T count = table.get(rule);
				boolean negative;
				if (count.getClass().isInstance(new Long(0))) negative = (Long)count<0;
				else negative = (Integer)count<0;
				if (negative) System.err.println("Negative frequency in : " + rule);
			}	
		}
	}
	
	/**
	 * Print a unique file `FullGrammar` containing the full grammar
	 * The file contains all the internal rules, an empty line and 
	 * all the lexical production rules.
	 */
	public void printFullGrammar() {
		File fullGrammarFile = new File(Parameters.outputPath + "FullGrammar");
		PrintWriter grammar = FileUtil.getPrintWriter(fullGrammarFile);
		for(int i=0; i<2; i++) {
			Hashtable<String,T> table = (i==0) ? internalRules : lexRules;
			for (Enumeration<String> e = table.keys(); e.hasMoreElements() ;) {
				String rule = (String)e.nextElement();			
				T count = table.get(rule);										
				String line = count.toString() + " " + rule;
				grammar.write(line + "\n");
			}	
			if (i==0) grammar.write("\n");
		}			
		grammar.close();
		String log = "Printed full grammar";
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	/**
	 * Method to implement printLexiconAndGrammarFiles
	 * @return
	 */
	private Hashtable<String, String[]> buildCompactLexicon() {
		Hashtable<String, String[]> compactLexicon = new Hashtable<String, String[]>();
		for (Enumeration<String> e = this.lexRules.keys(); e.hasMoreElements() ;) {
			String rule = e.nextElement();
			Object count = this.lexRules.get(rule);
			String[] ruleSplit = rule.split(" ");
			String word = ruleSplit[1];
			String posTag = ruleSplit[0];			
			String[] posTags = compactLexicon.get(word);
			if (posTags==null) {
				posTags = new String[]{""};
				compactLexicon.put(word, posTags);
			}
			if (posTags[0].length()>0) posTags[0] += "\t";
			posTags[0] += posTag + " " + count;
		}	
		return compactLexicon;
	}
	
	/**
	 * Print two files `lexicon` and `grammar` containing the lexical production
	 * rules and the internal rules respectively.
	 */
	public void printLexiconAndGrammarFiles() {		
		File lexiconFile = new File(Parameters.outputPath + "lexicon");
		File grammarFile = new File(Parameters.outputPath + "grammar");
		PrintWriter grammar = FileUtil.getPrintWriter(grammarFile);
		TreeSet<String> orderedInternal = new TreeSet<String>(internalRules.keySet()); 
		for (String rule : orderedInternal) {			
			Object count = internalRules.get(rule);										
			String line = count.toString() + "\t" + rule;
			grammar.write(line + "\n");
		}	
		grammar.close();
		
		PrintWriter lexicon = FileUtil.getPrintWriter(lexiconFile);
		Hashtable<String, String[]> compactLexicon = this.buildCompactLexicon();
		TreeSet<String> orderedLexical = new TreeSet<String>(compactLexicon.keySet());		
		for (String word : orderedLexical) {
			String posTags = compactLexicon.get(word)[0];										
			String line = word + "\t" + posTags;
			lexicon.write(line + "\n");
		}	
		lexicon.close();
		
		String log = "Printed `lexicon` and `grammar` files";
		FileUtil.appendReturn(log, Parameters.logFile);
	}
	
	/**
	 * Print the training corpus in the output directory
	 */
	public void printTrainingCorpusToFile() {
		Parameters.printTrainingCorpusToFile();
	}
	
	/**
	 * Print the test corpus in the output directory
	 */
	public void printTestCorpusToFile() {
		Parameters.printTestCorpusToFile();
	}
	
	private Hashtable<String, Integer> buildDaughterParentTable() {
		boolean countUnary = false;
		Hashtable<String, Integer> daughterParentTable = new Hashtable<String, Integer>();
		for(TSNode TreeLine : Parameters.trainingCorpus.treeBank) {
			List<TSNode> allNodes = TreeLine.collectAllNodes();
			for(TSNode n : allNodes) {
				if (n.isTerminal()) continue;
				if (n.isUniqueDaughter() && !countUnary) continue;
				TSNode parent = n.parent;
				if (parent==null) continue;
				String daugherParent = n.label + " " + parent.label;
				Utility.increaseStringInteger(daughterParentTable, daugherParent, 1);
			}
		}
		return daughterParentTable;
	}
	
	public void assignHeadAnnotations(boolean allowPunctuation, boolean onlyExternalChoices) {
		Parameters.trainingCorpus.removeHeadAnnotations();
		Hashtable<String, Integer> daughterParentTable = buildDaughterParentTable();
		int totalAmbiguity=0, totalChoices=0;
		for(TSNode TreeLine : Parameters.trainingCorpus.treeBank) {
			List<TSNode> allNodes = TreeLine.collectAllNodes();
			for(TSNode n : allNodes) {
				if (n.isLexical || n.isPrelexical()) continue;
				if (n.prole()==1) {
					n.firstDaughter().headMarked = true;
					continue;
				}
				totalChoices++;
				int maxCount = -1;
				TSNode bestDaughter = null;
				boolean ambiguity = false;
				for(TSNode d : n.daughters) {
					if (!allowPunctuation && d.isPrelexical() && 
							Utility.isPunctuation(d.firstDaughter().label)) continue;
					if (onlyExternalChoices && !(d==n.firstDaughter() || d==n.lastDaughter())) continue;
					String daugherParent = d.label + " " + n.label;
					int count = daughterParentTable.get(daugherParent);					
					if (count>=maxCount) {
						if (count==maxCount) ambiguity = true;
						bestDaughter = d;
						maxCount = count;						
					}					
				}
				if (bestDaughter==null) {
					//all punctuation
					System.out.println("All punctuation daughter in " + n);
					n.daughters[n.prole()-1].headMarked=true;
				}
				else {
					bestDaughter.headMarked = true;
					if (ambiguity) totalAmbiguity++;					
				}
			}
		}
		float ratio = ((float) totalAmbiguity) / totalChoices;
		String report = "Ambiguity on head assignment: [" + 
						totalAmbiguity + " | " + totalChoices + "] -> " + ratio;
		System.out.println(report);
		FileUtil.appendReturn(report, Parameters.logFile);
	}
	
	public static void rulesStatistics() {
		//Parameters.trainingCorpus = new Corpus(new File(Wsj.WsjOriginalCollins99 + "wsj-02-21.mrg"),"noProcess");
		//Parameters.corpusName = "noProcess";
		Parameters.corpusName = "Wsj";
		Parameters.lengthLimitTraining = 1000;
		Parameters.lengthLimitTest = 1000;
		Parameters.semanticTags = false;
		Parameters.outputPath = Parameters.resultsPath + "TSG/CFG/RuleStatistics/";
		//int[] excluded = new int[]{0};
		CFSG<Integer> Grammar = new CFSG<Integer>();
		int threshold = 1;
		int sentenceBelowThreshold = 0;
		Hashtable<String, Integer> parentCategoryBelowThreshold = new Hashtable<String, Integer>();
		
		
		for(TSNode treeLine : Parameters.trainingCorpus.treeBank) {
			treeLine.removeNumberInLabels();
			List<TSNode> nonLexicalNodes = treeLine.collectNonLexicalNodes();
			for(TSNode nT : nonLexicalNodes) {
				if (!nT.isPrelexical()) {
					//Utility.increaseStringInteger(Grammar.internalRules, nT.toCFGCompl(false, excluded), 1);
					Utility.increaseStringInteger(Grammar.internalRules, nT.toCFG(false), 1);
				}					
			}
		}
		
		for(TSNode treeLine : Parameters.trainingCorpus.treeBank) {
			treeLine.removeNumberInLabels();
			List<TSNode> nonLexicalNodes = treeLine.collectNonLexicalNodes();
			boolean sentenceUncovered = false;
			for(TSNode nT : nonLexicalNodes) {
				if (!nT.isPrelexical()) {
					String rule = nT.toCFG(false);
					int count = Grammar.internalRules.get(rule);
					if (count<=threshold) {
						if (!sentenceUncovered) {
							sentenceUncovered=true;
							sentenceBelowThreshold++;
						}						
						Utility.increaseStringInteger(parentCategoryBelowThreshold, nT.label, 1);
					}
				}					
			}
		}
					
		//Set<String> internalRulesTrainingSet = Grammar.internalRules.keySet();
		Utility.hashtableOrderedToFile(Grammar.internalRules, new File(Parameters.outputPath + "CFG_freq"));
		Utility.hashtableRankedToFile(Grammar.internalRules, new File(Parameters.outputPath + "CFG_rank"));
		Utility.hashtableOrderedToFile(parentCategoryBelowThreshold, 
				new File(Parameters.outputPath + "catStat_below_" + threshold));
		
		int trainTotalTypes = Utility.countTotalTypesInTable(Grammar.internalRules);
		int trainTotalTokens = Utility.countTotalTokensInTable(Grammar.internalRules);
				
		String report = "\nRULES STATITSTICS:\n" +
				"Training Corpus initial total types|tokens:\t" + 
				trainTotalTypes + "\t" + trainTotalTokens + "\n" +
				"Sentence below threshold (" + threshold + ")|total:\t" + sentenceBelowThreshold + 
					"\t" + Parameters.trainingCorpus.size() + "\n";				
				//"Excluded children: " + excluded[0] + "\n";
		System.out.println(report);
		FileUtil.appendReturn(report, Parameters.logFile);
	}
	
	public static void checkCoverage() {
		Parameters.corpusName = "Wsj"; //Wsj, Negra, Parc, Tiger 
		Parameters.lengthLimitTraining = 1000;
		Parameters.lengthLimitTest = 1000;
		Parameters.semanticTags = false;		
		
		Wsj.testSet = "22"; //00 01 22 23 24
		Parameters.toNormalForm = false;
		
		Parameters.outputPath = Parameters.resultsPath + "TSG/CFG/Coverage/";
		
		CFSG<Integer> Grammar = new CFSG<Integer>();
		Grammar.readCFGFromCorpus();
		Hashtable<String, Integer> internalRulesTraining = Grammar.internalRules;				
		Set<String> internalRulesTrainingSet = internalRulesTraining.keySet();
		
		Hashtable<String, Integer> internalRulesTesting = new Hashtable<String, Integer>(); 
		int unmatchedSentences = 0;
		ArrayList<Integer> unmatchedSentencesLength = new ArrayList<Integer>(); 
		for(TSNode treeLine : Parameters.testCorpus.treeBank) {
			Grammar.internalRules = new Hashtable<String, Integer>();
			Grammar.readCFGFromTreeLine(treeLine);			
			Utility.addAll(Grammar.internalRules, internalRulesTesting);			
			for(String rule : Grammar.internalRules.keySet()) {
				if (!internalRulesTrainingSet.contains(rule)) {
					unmatchedSentences++;
					unmatchedSentencesLength.add(treeLine.countLexicalNodes());
					break;
				}
			}
		}		
		
		int trainTotalTypes = Utility.countTotalTypesInTable(internalRulesTraining);
		int trainTotalTokens = Utility.countTotalTokensInTable(internalRulesTraining);
		int testTotalTypes = Utility.countTotalTypesInTable(internalRulesTesting);
		int testTotalTokens = Utility.countTotalTokensInTable(internalRulesTesting);
		
		Utility.hashtableOrderedToFile(internalRulesTraining, new File(Parameters.outputPath + "CFG_train"));
		Utility.hashtableOrderedToFile(internalRulesTesting, new File(Parameters.outputPath + "CFG_test"));
		
		internalRulesTesting.keySet().removeAll(internalRulesTraining.keySet());		
		Utility.hashtableOrderedToFile(internalRulesTesting, new File(Parameters.outputPath + "CFG_test_unmatched"));		
		int unmatchedEvalTotalTypes = Utility.countTotalTypesInTable(internalRulesTesting);
		int unmatchedEvalTotalTokens = Utility.countTotalTokensInTable(internalRulesTesting);
		float unmatchedEvalPercentTypes = (float) unmatchedEvalTotalTypes / testTotalTypes;
		float unmatchedEvalPercentTokens = (float) unmatchedEvalTotalTokens / testTotalTokens;		
		String report = "\nCOVERAGE ANALYSIS:\n" +
				"Training Corpus initial total types|tokens: " + 
				trainTotalTypes + "|" + trainTotalTokens + "\n" + 
				"Test Corpus initial total types|tokens: " + 
				testTotalTypes + "|" + testTotalTokens + "\n" +				
				"Test Corpus unmatched total types|tokens: " + 
				unmatchedEvalTotalTypes + "|" + unmatchedEvalTotalTokens + "\n" +
				"Test Corpus unmatched % types|tokens: " + 
				unmatchedEvalPercentTypes + "|" + unmatchedEvalPercentTokens + "\n"+
				"Test Corpus unmatched sentences|total: " + 
				unmatchedSentences + "|" + Parameters.testCorpus.size() + "\n" +
				"Unmatched Sentences Length classes :\n" + 
				Utility.printIntegerListClasses(unmatchedSentencesLength, 10);
		System.out.println(report);
		FileUtil.appendReturn(report, Parameters.logFile);
	}
	
	
	public static void main(String args[]) {
		Parameters.setDefaultParam();	
		
		Parameters.lengthLimitTraining = 40;
		Parameters.lengthLimitTest = 40;
		
		Parameters.outputPath = Parameters.resultsPath + "TSG/CFSG/";
		CFSG<Integer> Grammar = new CFSG<Integer>();
		Grammar.readCFGFromCorpus();			

		Grammar.printLexiconAndGrammarFiles();
		Grammar.printTrainingCorpusToFile();
		Grammar.printTestCorpusToFile();
		
		//new Parser(Grammar);
		
		//checkCoverage();
		//rulesStatistics();
	}
	
}
