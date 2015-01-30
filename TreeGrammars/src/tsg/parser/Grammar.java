package tsg.parser;
import util.*;
import util.file.FileUtil;
import settings.Parameters;
import tsg.*;
import tsg.corpora.*;

import java.util.*;
import java.io.*;

/**
 * This class contains the CFG grammar to be processed by the CKY parser.
 * Every non terminal and terminal has a unique index...
 * @author fsangati
 *
 */
public class Grammar {
	
	
	int lexCount, catCount;	
	String[] lexArray, catArray; //arrays of words and categories
	Hashtable<String, Integer> lexIndex, catIndex; //indexes of words and categories
	int[][] rulesOfWord; //rulesOfWord[w]: all the (ordered) index r such that r: y -> w
	int[][] binaryRulesOfLeftChild; //rulesOfLeftChild[x][]: all the (ordered) rules r such that r: y -> x *
	int[][] unaryRulesOfLeftChild; //rulesOfLeftChild[x][]: all the (ordered) rules r such that r: y -> x
	//int[][] rulesOfRightChild; //rulesOfRightChild[x][]: all the (ordered) rules r such that r: y -> * x
	int[][] intBinaryRules; //intRules[r] = [y,x,z] for all r: y -> x z
	int[][] intUnaryRules; //intUnaryRules[r'] = [y,x] for all r': y -> x	
	int[][] lexRules; //lexRules[r''] = [z,w] for all r'': z -> w
	int intBinaryRulesSize, intUnaryRulesSize, lexRulesSize;
	double[] intBinaryLogProb; //intLogProb[r]: LogProb(y -> x z) where rules[r] = [y,x,z]
	double[] intUnaryLogProb; //intLogProb[r']: LogProb(y -> x) where rules[r'] = [y,x]
	double[] lexLogProb; //lexLogProb[r'']: LogProb(z -> w) where rules[r''] = [y,w]	
	int maxCycleUnaryProduction;
	File logFile;
	
	/**
	 * Constructor accepting a CFG grammar
	 */
	public Grammar(CFSG<? extends Number> grammar) {
		this(grammar.lexRules, grammar.internalRules);
		logFile = Parameters.logFile;
	}
	
	/**
	 * Constructor accepting two hashtables with lexical rules and internal rules.
	 */
	public Grammar(Hashtable<String, ? extends Number> lexRulesTable, 
			Hashtable<String, ? extends Number> intRulesTable) {
		readGrammar(lexRulesTable, intRulesTable);
		printGrammarStatistics();
	}
	
	private void printGrammarStatistics() {
		FileUtil.appendReturn("\t...done", Parameters.logFile);
		FileUtil.appendReturn("\t# of words: " + lexArray.length, Parameters.logFile);
		FileUtil.appendReturn("\t# of categories: " + catArray.length, Parameters.logFile);
	}
	
	public int getIndexOfWord(String word) {
		Integer index = lexIndex.get(word);
		if (index==null) {			
			System.err.println(word + " unknown");
			System.exit(-1);
		}
		return index;
	}
	
	/**
	 * Builds lexCount, catCount, lexArray, catArray, lexIndex, catIndex, categoryTotFreq
	 * @param lexRules
	 * @param intBinaryRules
	 */
	private void readGrammar(Hashtable<String, ? extends Number> lexRulesTable, 
			Hashtable<String, ? extends Number> intRulesTable) {
		FileUtil.appendReturn("Starting fedePar ... reading grammar", Parameters.logFile);
		TreeSet<String> lexicon = new TreeSet<String>();
		TreeSet<String> categories = new TreeSet<String>();
		Hashtable<String, Double> intCatFreqTable = new Hashtable<String, Double>();
		Hashtable<String, Double> posCatFreqTable = new Hashtable<String, Double>();
		lexRulesSize = lexRulesTable.size();
		for(Enumeration<String> e = lexRulesTable.keys(); e.hasMoreElements(); ) {
			String rule = (String)e.nextElement();
			double count = lexRulesTable.get(rule).doubleValue();	
			String[] ruleSplit = rule.split(" ");
			Utility.increaseStringDouble(posCatFreqTable, ruleSplit[0], count);
			categories.add(ruleSplit[0]);
			lexicon.add(ruleSplit[1]);
		}
		for(Enumeration<String> e = intRulesTable.keys(); e.hasMoreElements(); ) {
			String rule = (String)e.nextElement();
			double count = intRulesTable.get(rule).doubleValue();	
			String[] ruleSplit = rule.split(" ");
			Utility.increaseStringDouble(intCatFreqTable, ruleSplit[0], count);
			categories.add(ruleSplit[0]);
			categories.add(ruleSplit[1]);
			if (ruleSplit.length>2) {
				categories.add(ruleSplit[2]);
				intBinaryRulesSize++;
			}
			else intUnaryRulesSize++;
		}
		//check overlapping between posTags and intTags
		HashSet<String> overlapping = new HashSet<String>(posCatFreqTable.keySet());
		overlapping.retainAll(intCatFreqTable.keySet());
		if (!overlapping.isEmpty()) {
			System.out.println("Overlapping between internal nodes and pos tags: " + overlapping.toString());
		}
		lexCount = lexicon.size();
		catCount = categories.size();
		lexArray = lexicon.toArray(new String[] {});
		catArray = categories.toArray(new String[] {});
		lexIndex = new Hashtable<String, Integer>();
		catIndex = new Hashtable<String, Integer>();		
		for(int i=0; i<lexArray.length; i++) lexIndex.put(lexArray[i], i);
		for(int i=0; i<catArray.length; i++) catIndex.put(catArray[i], i);
		readRules(lexRulesTable, intRulesTable, posCatFreqTable, intCatFreqTable);
	}
	
	/**
	 * Builds hasPos, posOfWord, hasParent_Left, hasParent_Right, parentsOfCat_Left, parentsOfCat_Right, logProb
	 */
	private void readRules(Hashtable<String, ? extends Number> lexRulesTable, Hashtable<String, ? extends Number> intRulesTable, 
			Hashtable<String, Double> posCatFreqTable, Hashtable<String, Double> intCatFreqTable) {		
		lexRules = new int[lexRulesSize][2];
		lexLogProb = new double[lexRulesSize];
		intBinaryRules = new int[intBinaryRulesSize][3];
		intBinaryLogProb =  new double[intBinaryRulesSize];
		intUnaryRules = new int[intUnaryRulesSize][2];
		intUnaryLogProb =  new double[intUnaryRulesSize];				
		rulesOfWord = new int[lexCount][];
		binaryRulesOfLeftChild = new int[catCount][];
		unaryRulesOfLeftChild = new int[catCount][];
		int lexRuleCount = 0;
		for(Enumeration<String> e = lexRulesTable.keys(); e.hasMoreElements(); ) {
			String rule = (String)e.nextElement();
			double count = lexRulesTable.get(rule).doubleValue();	
			String[] ruleSplit = rule.split(" ");
			int parentIndex = catIndex.get(ruleSplit[0]);
			int wordIndex = lexIndex.get(ruleSplit[1]);									
			double parentFreq = posCatFreqTable.get(ruleSplit[0]);			
			lexRules[lexRuleCount] = new int[]{parentIndex, wordIndex};
			lexLogProb[lexRuleCount] = Math.log(count / parentFreq);
			rulesOfWord[wordIndex] = Utility.appendIntArraySet(rulesOfWord[wordIndex], lexRuleCount);
			lexRuleCount++;
		}
		int intBinaryRuleCount=0, intUnaryRuleCount=0;
		for(Enumeration<String> e = intRulesTable.keys(); e.hasMoreElements(); ) {
			String rule = (String)e.nextElement();
			double count = intRulesTable.get(rule).doubleValue();	
			String[] ruleSplit = rule.split(" ");
			int parentIndex = catIndex.get(ruleSplit[0]);
			int leftDaughterIndex = catIndex.get(ruleSplit[1]);
			double parentFreq = intCatFreqTable.get(ruleSplit[0]);
			if (ruleSplit.length>2) {
				int rightDaughterIndex = catIndex.get(ruleSplit[2]);				
				intBinaryRules[intBinaryRuleCount] = new int[]{parentIndex, leftDaughterIndex, rightDaughterIndex};						
				intBinaryLogProb[intBinaryRuleCount] = Math.log(count / parentFreq);
				binaryRulesOfLeftChild[leftDaughterIndex] = Utility.appendIntArraySet(binaryRulesOfLeftChild[leftDaughterIndex], intBinaryRuleCount);
				intBinaryRuleCount++;
			}
			else {
				intUnaryRules[intUnaryRuleCount] = new int[]{parentIndex, leftDaughterIndex};
				intUnaryLogProb[intUnaryRuleCount] = Math.log(count / parentFreq);
				unaryRulesOfLeftChild[leftDaughterIndex] = Utility.appendIntArraySet(unaryRulesOfLeftChild[leftDaughterIndex], intUnaryRuleCount);
				intUnaryRuleCount++;
			}
		}
	}
	
	public void checkRecursiveUnaryProductions() {
		for(int i=0; i<this.intUnaryRules.length; i++) {
			if(this.intUnaryRules[i][0]==this.intUnaryRules[i][1]) {
				String logMessage = "Recursive unary production: " + this.catArray[this.intUnaryRules[i][0]] + " --> " + 
						this.catArray[this.intUnaryRules[i][1]];
				FileUtil.appendReturn(logMessage, this.logFile);
			}
			else {
				int[] rules = this.unaryRulesOfLeftChild[this.intUnaryRules[i][0]];
				if (rules==null) continue;
				for(int j=0; j<rules.length; j++) {
					int[] rule = this.intUnaryRules[rules[j]];
					if (rule[0]==this.intUnaryRules[i][1]) {
						String logMessage = "Recursive unary production: " + this.catArray[this.intUnaryRules[i][0]] + " --> " + 
								this.catArray[this.intUnaryRules[i][1]] + " | " + this.catArray[this.intUnaryRules[i][1]] + " --> " + 
								this.catArray[this.intUnaryRules[i][0]];
						FileUtil.appendReturn(logMessage, this.logFile);
					}
				}
			}
		}
	}
	
	public void checkCircularUnaryProductions(int limit) {
		for(int i=0; i<this.catCount; i++) {
			goUpUnaryProduction(i, 0, limit);		
		}
		
	}
	
	private void goUpUnaryProduction(int uniqueChild, int loop, int limit) {
		int[] possibleRules = this.unaryRulesOfLeftChild[uniqueChild];
		if (possibleRules==null) return;
		if(loop>limit) {
			this.maxCycleUnaryProduction = loop;
			System.err.println("Cyclic Unary Production: reached loop of level " + loop);
			System.exit(-1);
		}		
		if (loop>this.maxCycleUnaryProduction) this.maxCycleUnaryProduction = loop;
		for(int i=0; i<possibleRules.length; i++) {
			int ruleIndex = possibleRules[i];
			int[] rule = this.intUnaryRules[ruleIndex];
			goUpUnaryProduction(rule[0], loop+1, limit);			
		}
	}
	
	public double getLexLogProb(int rootIndex, int lexIndex) {
		int[] possibleRoots = this.rulesOfWord[lexIndex];
		for(int i=0; i<possibleRoots.length; i++) {
			int ruleIndex = possibleRoots[i];
			int[] rule = this.lexRules[ruleIndex];
			if (rule[0]==rootIndex) return this.lexLogProb[ruleIndex];
		}
		return -1;
	}
	
	public double getUnaryLogProb(int rootIndex, int leftIndex) {
		int[] possibleRoots = this.unaryRulesOfLeftChild[leftIndex];
		for(int i=0; i<possibleRoots.length; i++) {
			int ruleIndex = possibleRoots[i];
			int[] rule = this.intUnaryRules[ruleIndex];
			if (rule[0]==rootIndex) return this.intUnaryLogProb[ruleIndex];
		}
		return -1;
	}
	
	public double getBinaryLogProb(int rootIndex, int leftIndex, int rightIndex) {
		int[] possibleRoots = this.binaryRulesOfLeftChild[leftIndex];
		for(int i=0; i<possibleRoots.length; i++) {
			int ruleIndex = possibleRoots[i];
			int[] rule = this.intBinaryRules[ruleIndex];
			if (rule[0]==rootIndex && rule[2]==rightIndex) return this.intBinaryLogProb[ruleIndex];
		}
		return -1;
	}
	
	
}
