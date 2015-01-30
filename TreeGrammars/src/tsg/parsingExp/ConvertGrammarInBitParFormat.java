package tsg.parsingExp;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Map.Entry;

import util.StringInteger;
import util.Utility;
import util.file.FileUtil;

public class ConvertGrammarInBitParFormat {
	
	static String artificialNodePrefix = "###AN_";
	
	public static void deterministicGrammar(File completeGrammarFile,
			File grammarOut, File lexiconOut) {
		int artificialNodeCounter = 0;		
		PrintWriter grammarPW = FileUtil.getPrintWriter(grammarOut);
		PrintWriter lexiconPW = FileUtil.getPrintWriter(lexiconOut);
		Hashtable<String, ArrayList<String>> lexiconPostags = new Hashtable<String, ArrayList<String>>(); 
		Scanner scan = FileUtil.getScanner(completeGrammarFile);
		while(scan.hasNextLine()) {
			String rule = scan.nextLine();
			if (rule.indexOf('"')==-1) grammarPW.println(rule);
			else {
				String[] ruleSymbols = rule.split("\\s+");				
				if (ruleSymbols.length==2) {
					String postag = ruleSymbols[0];
					String word = ruleSymbols[1].replaceAll("\"", "");
					add(word, postag, lexiconPostags);
				}
				else {
					String LHS = ruleSymbols[0];
					String[] nonTerminalRHS = new String[ruleSymbols.length-1];
					for(int i=1; i<ruleSymbols.length; i++) {
						String nodeRHS = ruleSymbols[i];
						if (nodeRHS.indexOf('"')==-1) nonTerminalRHS[i-1] = nodeRHS;
						else {														
							String artificialPostag = artificialNodePrefix + 
								(++artificialNodeCounter) + "_";
							nonTerminalRHS[i-1] = artificialPostag;
							String word = nodeRHS.replaceAll("\"", "");
							add(word, artificialPostag, lexiconPostags);
						}
					}
					String artificialRule = LHS + " " + 
						Utility.joinStringArrayToString(nonTerminalRHS, " ");
					grammarPW.println(artificialRule);
				}
			}
		}	
		for(Entry<String, ArrayList<String>> e : lexiconPostags.entrySet()) {
			lexiconPW.print(e.getKey());
			for(String postag : e.getValue()) {
				lexiconPW.print("\t" + postag);
			}
			lexiconPW.println();
		}
		grammarPW.close();
		lexiconPW.close();
	}
	
	public static void deterministicGrammarForOneSentence(File completeGrammarFile,
			String sentence, File grammarOut, File lexiconOut) {
		String[] sentenceWords = sentence.split("\\s+");
		HashSet<String> sentenceWordsSet = new HashSet<String>();
		for(String w : sentenceWords) sentenceWordsSet.add(w);
		int artificialNodeCounter = 0;
		PrintWriter grammarPW = FileUtil.getPrintWriter(grammarOut);
		PrintWriter lexiconPW = FileUtil.getPrintWriter(lexiconOut);
		Hashtable<String, ArrayList<String>> lexiconPostags = new Hashtable<String, ArrayList<String>>(); 
		Scanner scan = FileUtil.getScanner(completeGrammarFile);
		while(scan.hasNextLine()) {
			String rule = scan.nextLine();
			if (rule.indexOf('"')==-1) grammarPW.println(rule);
			else {
				String[] ruleSymbols = rule.split("\\s+");				
				if (ruleSymbols.length==2) {
					String postag = ruleSymbols[0];
					String word = ruleSymbols[1].replaceAll("\"", "");
					if (sentenceWordsSet.contains(word)) add(word, postag, lexiconPostags);
				}
				else {
					String LHS = ruleSymbols[0];
					String[] nonTerminalRHS = new String[ruleSymbols.length-1];
					boolean allLexicalWordsAreInTheSentence = true;
					ArrayList<String[]> lexicalRules = new ArrayList<String[]>();
					int artificialNodeCounterTmp = artificialNodeCounter;
					for(int i=1; i<ruleSymbols.length; i++) {
						String nodeRHS = ruleSymbols[i];
						if (nodeRHS.indexOf('"')==-1) nonTerminalRHS[i-1] = nodeRHS;						 
						else {										
							String word = nodeRHS.replaceAll("\"", "");
							if (!sentenceWordsSet.contains(word)) {
								allLexicalWordsAreInTheSentence = false;
								break;
							}
							String artificialPostag = artificialNodePrefix + 
								(++artificialNodeCounter) + "_";
							nonTerminalRHS[i-1] = artificialPostag;				
							lexicalRules.add(new String[]{word, artificialPostag});
						}
					}
					if (allLexicalWordsAreInTheSentence) {
						String artificialRule = LHS + " " + 
							Utility.joinStringArrayToString(nonTerminalRHS, " ");
						grammarPW.println(artificialRule);
						for(String[] wordPos : lexicalRules) {
							add(wordPos[0], wordPos[1], lexiconPostags);
						}
					}
					else artificialNodeCounter = artificialNodeCounterTmp;
				}
			}
		}	
		for(Entry<String, ArrayList<String>> e : lexiconPostags.entrySet()) {
			lexiconPW.print(e.getKey());
			for(String postag : e.getValue()) {
				lexiconPW.print("\t" + postag);
			}
			lexiconPW.println();
		}
		grammarPW.close();
		lexiconPW.close();
	}
	
	public static void probGrammarForOneSentence(File completeGrammarFile,String[] sentenceWords,
			File grammarOut, File lexiconOut) {
		HashSet<String> sentenceWordsSet = new HashSet<String>();
		for(String w : sentenceWords) sentenceWordsSet.add(w);
		int artificialNodeCounter = 0;
		PrintWriter grammarPW = FileUtil.getPrintWriter(grammarOut);
		PrintWriter lexiconPW = FileUtil.getPrintWriter(lexiconOut);
		Hashtable<String, ArrayList<StringInteger>> lexiconPostags = 
			new Hashtable<String, ArrayList<StringInteger>>(); 
		Scanner scan = FileUtil.getScanner(completeGrammarFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			if (line.indexOf('"')==-1) {
				grammarPW.println(line);
				continue;
			}
			String[] lineSplit = line.split("\t");
			int freq = Integer.parseInt(lineSplit[0]);
			String rule = lineSplit[1];
			
			String[] ruleSymbols = rule.split("\\s+");				
			if (ruleSymbols.length==2) {
				String postag = ruleSymbols[0];
				String word = ruleSymbols[1].replaceAll("\"", "");
				StringInteger postagFreq = new StringInteger(postag, freq);
				if (sentenceWordsSet.contains(word)) addLexPosFreq(word, postagFreq, lexiconPostags);
			}
			else {
				String LHS = ruleSymbols[0];
				String[] nonTerminalRHS = new String[ruleSymbols.length-1];
				boolean allLexicalWordsAreInTheSentence = true;
				ArrayList<String[]> lexicalRules = new ArrayList<String[]>();
				int artificialNodeCounterTmp = artificialNodeCounter;
				for(int i=1; i<ruleSymbols.length; i++) {
					String nodeRHS = ruleSymbols[i];
					if (nodeRHS.indexOf('"')==-1) nonTerminalRHS[i-1] = nodeRHS;						 
					else {										
						String word = nodeRHS.replaceAll("\"", "");
						if (!sentenceWordsSet.contains(word)) {
							allLexicalWordsAreInTheSentence = false;
							break;
						}
						String artificialPostag = artificialNodePrefix + 
							(++artificialNodeCounter) + "_";
						nonTerminalRHS[i-1] = artificialPostag;				
						lexicalRules.add(new String[]{word, artificialPostag});
					}
				}
				if (allLexicalWordsAreInTheSentence) {
					String artificialRule = LHS + " " + 
						Utility.joinStringArrayToString(nonTerminalRHS, " ");
					grammarPW.println(freq + "\t" + artificialRule);
					for(String[] wordPos : lexicalRules) {
						addLexPosFreq(wordPos[0], new StringInteger(wordPos[1],1), lexiconPostags);
					}
				}
				else artificialNodeCounter = artificialNodeCounterTmp;
			}
		}	
		for(Entry<String, ArrayList<StringInteger>> e : lexiconPostags.entrySet()) {
			lexiconPW.print(e.getKey());
			for(StringInteger postagFreq : e.getValue()) {
				lexiconPW.print("\t" + postagFreq.string() + " " + postagFreq.integer());
			}
			lexiconPW.println();
		}
		grammarPW.close();
		lexiconPW.close();
	}
	
	/**
	 * prepare completeGrammarFile to be used with BitPar
	 * @param completeGrammarFile grammar and lexical rules possibily lexicalized: NP NN "JJ^good" JJ NN
	 * @param sentencePosWords JJ_good
	 * @param grammarOut
	 * @param lexiconOut
	 */
	public static void probGrammarForOneSentenceNew(File completeGrammarFile,String[] sentencePosWords,
			File grammarOut, File lexiconOut, char lexPosSeparationChar) {		
		HashSet<String> sentencePosWordsSet = new HashSet<String>();
		for(String w : sentencePosWords) sentencePosWordsSet.add(w);
		PrintWriter grammarPW = FileUtil.getPrintWriter(grammarOut);
		PrintWriter lexiconPW = FileUtil.getPrintWriter(lexiconOut);
		Hashtable<String, ArrayList<StringInteger>> lexiconPostagsFreq = 
			new Hashtable<String, ArrayList<StringInteger>>(); 
		Scanner scan = FileUtil.getScanner(completeGrammarFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			if (line.indexOf('"')==-1) {  //doesn't have lex
				grammarPW.println(line);
				continue;
			}
			// has lexicon
			String[] lineSplit = line.split("\t");
			int freq = Integer.parseInt(lineSplit[0]);
			String rule = lineSplit[1];
			
			String[] ruleSymbols = rule.split("\\s+");				
			if (ruleSymbols.length==2) { // is terminal rule: NN "man"				
				String pos_word = ruleSymbols[1].replaceAll("\"", "");				
				if (sentencePosWordsSet.contains(pos_word)) {
					String pos = ruleSymbols[0];
					StringInteger posFreq = new StringInteger(pos, 1);
					//StringInteger posFreq = new StringInteger(pos, freq);
					addLexPosFreqIfNotPresent(pos_word, posFreq, lexiconPostagsFreq);
				}
			}
			else {				
				boolean allLexicalWordsAreInTheSentence = true;
				ArrayList<String> lexicalRules = new ArrayList<String>();				
				for(int i=1; i<ruleSymbols.length; i++) {
					String nodeRHS = ruleSymbols[i];
					if (nodeRHS.indexOf('"')!=-1) {										
						String pos_word = nodeRHS.replaceAll("\"", "");
						if (!sentencePosWordsSet.contains(pos_word)) {
							allLexicalWordsAreInTheSentence = false;
							break;
						}									
						lexicalRules.add(pos_word);
					}
				}
				if (allLexicalWordsAreInTheSentence) {					
					rule = rule.replaceAll("\"", "");					
					grammarPW.println(freq + "\t" + rule);
					for(String posWord : lexicalRules) {
						StringInteger posWordFreq = new StringInteger(posWord, 1);
						addLexPosFreqIfNotPresent(posWord, posWordFreq, lexiconPostagsFreq);
					}
				}
			}
		}	
		scan.close();
		// for unknown words
		for(String posWord : sentencePosWords) {
			String pos = posWord.substring(0, posWord.indexOf(lexPosSeparationChar));
			StringInteger posFreq = new StringInteger(pos, 1);
			addLexPosFreqIfNotPresent(posWord, posFreq, lexiconPostagsFreq);
		}
		//write lexicon
		for(Entry<String, ArrayList<StringInteger>> e : lexiconPostagsFreq.entrySet()) {
			lexiconPW.print(e.getKey());
			for(StringInteger postagFreq : e.getValue()) {
				lexiconPW.print("\t" + postagFreq.string() + " " + postagFreq.integer());
			}
			lexiconPW.println();
		}
		grammarPW.close();
		lexiconPW.close();
	}
	
	/**
	 * prepare completeGrammarFile to be used with BitPar (use only lex rules which are relevant)
	 * @param completeGrammarFile grammar and lexical rules possibily lexicalized: NP NN "JJ_good" JJ NN
	 * @param sentencePosWords JJ_good
	 * @param grammarOut
	 * @param lexiconOut
	 */
	public static void probGrammarForAllSentencesSelective(File completeGrammarFile, 
			HashSet<String> sentencePosWordsSet, File grammarOut, File lexiconOut, 
			char lexPosSeparationChar) {		
				
		PrintWriter grammarPW = FileUtil.getPrintWriter(grammarOut);
		PrintWriter lexiconPW = FileUtil.getPrintWriter(lexiconOut);
		Hashtable<String, ArrayList<StringInteger>> lexiconPostagsFreq = 
			new Hashtable<String, ArrayList<StringInteger>>(); 
		Scanner scan = FileUtil.getScanner(completeGrammarFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			if (line.indexOf('"')==-1) {  //doesn't have lex
				grammarPW.println(line);
				continue;
			}
			// has lexicon
			String[] lineSplit = line.split("\t");
			int freq = Integer.parseInt(lineSplit[0]);
			String rule = lineSplit[1];
			
			String[] ruleSymbols = rule.split("\\s+");				
			if (ruleSymbols.length==2) { // is terminal rule: NN "man"				
				String word = ruleSymbols[1].replaceAll("\"", "");				
				if (sentencePosWordsSet.contains(word)) {
					String pos = ruleSymbols[0];
					//StringInteger posFreq = new StringInteger(pos, 1);
					StringInteger posFreq = new StringInteger(pos, freq);
					addLexPosFreqIfNotPresent(word, posFreq, lexiconPostagsFreq);
				}
			}
			else {				
				boolean allLexicalWordsAreInTheSentence = true;
				ArrayList<String[]> lexicalRules = new ArrayList<String[]>();				
				for(int i=1; i<ruleSymbols.length; i++) {
					String nodeRHS = ruleSymbols[i];
					if (nodeRHS.indexOf('"')!=-1) {										
						String pos_word = nodeRHS.replaceAll("\"", "");
						if (!sentencePosWordsSet.contains(pos_word)) {
							allLexicalWordsAreInTheSentence = false;
							break;
						}									
						lexicalRules.add(new String[]{pos_word, pos_word});
					}
				}
				if (allLexicalWordsAreInTheSentence) {					
					rule = rule.replaceAll("\"", "");					
					grammarPW.println(freq + "\t" + rule);
					for(String[] posWord : lexicalRules) {
						StringInteger posWordFreq = new StringInteger(posWord[1], 1);
						addLexPosFreqIfNotPresent(posWord[0], posWordFreq, lexiconPostagsFreq);
					}
				}
			}
		}	
		// for unknown words
		for(String posWord : sentencePosWordsSet) {
			String pos = posWord.substring(0, posWord.indexOf(lexPosSeparationChar));
			StringInteger posFreq = new StringInteger(pos, 1);
			addLexPosFreqIfNotPresent(posWord, posFreq, lexiconPostagsFreq);
		}
		//write lexicon
		for(Entry<String, ArrayList<StringInteger>> e : lexiconPostagsFreq.entrySet()) {
			lexiconPW.print(e.getKey());
			for(StringInteger postagFreq : e.getValue()) {
				lexiconPW.print("\t" + postagFreq.string() + " " + postagFreq.integer());
			}
			lexiconPW.println();
		}
		grammarPW.close();
		lexiconPW.close();
	}
	
	/**
	 * prepare completeGrammarFile to be used with BitPar
	 * @param completeGrammarFile grammar and lexical rules possibily lexicalized: NP NN "JJ_good" JJ NN
	 * @param sentencePosWords JJ_good
	 * @param grammarOut
	 * @param lexiconOut
	 */
	public static void probGrammarForAllSentences(File completeGrammarFile, 
			HashSet<String> sentencePosWordsSet, File grammarOut, File lexiconOut, 
			char lexPosSeparationChar) {		
				
		PrintWriter grammarPW = FileUtil.getPrintWriter(grammarOut);
		PrintWriter lexiconPW = FileUtil.getPrintWriter(lexiconOut);
		Hashtable<String, ArrayList<StringInteger>> lexiconPostagsFreq = 
			new Hashtable<String, ArrayList<StringInteger>>(); 
		Scanner scan = FileUtil.getScanner(completeGrammarFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			if (line.indexOf('"')==-1) {  //doesn't have lex
				grammarPW.println(line);
				continue;
			}
			// has lexicon
			String[] lineSplit = line.split("\t");
			int freq = Integer.parseInt(lineSplit[0]);
			String rule = lineSplit[1];
			
			String[] ruleSymbols = rule.split("\\s+");				
			if (ruleSymbols.length==2) { // is terminal rule: NN "man"				
				String word = ruleSymbols[1].replaceAll("\"", "");				
				String pos = ruleSymbols[0];
				//StringInteger posFreq = new StringInteger(pos, 1);
				StringInteger posFreq = new StringInteger(pos, freq);
				addLexPosFreqIfNotPresent(word, posFreq, lexiconPostagsFreq);
			}
			else {																
				for(int i=1; i<ruleSymbols.length; i++) {
					String nodeRHS = ruleSymbols[i];
					if (nodeRHS.indexOf('"')!=-1) {										
						String pos_word = nodeRHS.replaceAll("\"", "");																					
						StringInteger posWordFreq = new StringInteger(pos_word, 1);
						addLexPosFreqIfNotPresent(pos_word, posWordFreq, lexiconPostagsFreq);
					}
				}
				rule = rule.replaceAll("\"", "");					
				grammarPW.println(freq + "\t" + rule);
			}
		}	
		// for unknown words
		for(String posWord : sentencePosWordsSet) {
			String pos = posWord.substring(0, posWord.indexOf(lexPosSeparationChar));
			StringInteger posFreq = new StringInteger(pos, 1);
			addLexPosFreqIfNotPresent(posWord, posFreq, lexiconPostagsFreq);
		}
		//write lexicon
		for(Entry<String, ArrayList<StringInteger>> e : lexiconPostagsFreq.entrySet()) {
			lexiconPW.print(e.getKey());
			for(StringInteger postagFreq : e.getValue()) {
				lexiconPW.print("\t" + postagFreq.string() + " " + postagFreq.integer());
			}
			lexiconPW.println();
		}
		grammarPW.close();
		lexiconPW.close();
	}
	
	
	public static void probGrammarForAllSentences(File completeGrammarFile, 
			File grammarOut, File lexiconOut, char lexPosSeparationChar) {		
				
		PrintWriter grammarPW = FileUtil.getPrintWriter(grammarOut);
		PrintWriter lexiconPW = FileUtil.getPrintWriter(lexiconOut);
		Hashtable<String, ArrayList<StringInteger>> lexiconPostagsFreq = 
			new Hashtable<String, ArrayList<StringInteger>>(); 
		Scanner scan = FileUtil.getScanner(completeGrammarFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			if (line.indexOf('"')==-1) {  //doesn't have lex
				grammarPW.println(line);
				continue;
			}
			// has lexicon
			String[] lineSplit = line.split("\t");
			int freq = Integer.parseInt(lineSplit[0]);
			String rule = lineSplit[1];
			
			String[] ruleSymbols = rule.split("\\s+");				
			if (ruleSymbols.length==2) { // is terminal rule: NN "man"				
				String word = ruleSymbols[1].replaceAll("\"", "");				
				String pos = ruleSymbols[0];
				//StringInteger posFreq = new StringInteger(pos, 1);
				StringInteger posFreq = new StringInteger(pos, freq);
				addLexPosFreqIfNotPresent(word, posFreq, lexiconPostagsFreq);
			}
			else {																
				for(int i=1; i<ruleSymbols.length; i++) {
					String nodeRHS = ruleSymbols[i];
					if (nodeRHS.indexOf('"')!=-1) {										
						String pos_word = nodeRHS.replaceAll("\"", "");																					
						StringInteger posWordFreq = new StringInteger(pos_word, 1);
						addLexPosFreqIfNotPresent(pos_word, posWordFreq, lexiconPostagsFreq);
					}
				}
				rule = rule.replaceAll("\"", "");					
				grammarPW.println(freq + "\t" + rule);
			}
		}	
		//write lexicon
		for(Entry<String, ArrayList<StringInteger>> e : lexiconPostagsFreq.entrySet()) {
			lexiconPW.print(e.getKey());
			for(StringInteger postagFreq : e.getValue()) {
				lexiconPW.print("\t" + postagFreq.string() + " " + postagFreq.integer());
			}
			lexiconPW.println();
		}
		grammarPW.close();
		lexiconPW.close();
	}
	
	public static void add(String key, String valueItem, 
			Hashtable<String, ArrayList<String>> table) {
		ArrayList<String> value = table.get(key);
		if (value==null) {
			value = new ArrayList<String>();
			table.put(key, value);
		}
		value.add(valueItem);
	}
	
	public static void addLexPosFreq(String key, StringInteger valueItem,
			Hashtable<String, ArrayList<StringInteger>> table) {
		ArrayList<StringInteger> value = table.get(key);
		if (value==null) {
			value = new ArrayList<StringInteger>();
			table.put(key, value);
		}
		value.add(valueItem);
	}
	
	public static void addLexPosFreqIfNotPresent(String key, StringInteger posFreq,
			Hashtable<String, ArrayList<StringInteger>> table) {
		ArrayList<StringInteger> value = table.get(key);
		if (value==null) {
			value = new ArrayList<StringInteger>();
			value.add(posFreq);
			table.put(key, value);			
			return;
		}
		String pos = posFreq.string();
		for(StringInteger vi : value) {
			if (vi.string().equals(pos)) {
				int freq = posFreq.integer();
				if (freq>vi.integer()) vi.setInteger(freq);
				return;
			}
		}
		value.add(posFreq);
	}
	
	

	public static void main(String[] args) {
		//deterministicGrammar(new File(args[0]));
		
	}
	
}
