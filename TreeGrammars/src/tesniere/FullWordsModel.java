package tesniere;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Vector;


import symbols.SymbolFreq;
import symbols.SymbolString;
import tsg.HeadLabeler;
import tsg.corpora.Wsj;
import util.Utility;

public class FullWordsModel {
	
	public static HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_14_01_10"));
	public static int unknownWordFreqTreshold = 25;
	
	ArrayList<Box> trainingCorpus, developCorpus;
	
	Vector<Hashtable<SymbolString, int[]>> trainingCats;		
	
	Vector<TreeSet<SymbolFreq>> developCats; 
	
	public FullWordsModel(File trainingFile, File developFile) throws Exception {
		trainingCorpus = Conversion.getTesniereTreebank(
				trainingFile, HL);		
		trainingCats = new Vector<Hashtable<SymbolString, int[]>>(5);
		for(int i=0; i<5; i++) trainingCats.add(new Hashtable<SymbolString, int[]>());		
		int sentenceNumber = 0;
		for(Box b : trainingCorpus) {
			sentenceNumber++;
			ArrayList<BoxStandard> boxCollector = new ArrayList<BoxStandard>();
			b.addStandardBoxesInCollection(boxCollector);
			for(BoxStandard bs : boxCollector) {
				int originalCat = bs.originalCat+1;
				String fullWordAsString = bs.getFullWordsAsString();
				if (fullWordAsString==null) {
					System.err.println(sentenceNumber + 
							": found std. box witout full words in training: " + bs);
					continue;
				}
				SymbolString fullWord = new SymbolString(fullWordAsString);
				Utility.increaseInTableInt(trainingCats.get(originalCat), fullWord);
			}			
		}
		developCorpus = Conversion.getTesniereTreebank(
				developFile, HL);				
		developCats = new Vector<TreeSet<SymbolFreq>>(5);
		for(int i=0; i<5; i++) developCats.add(new TreeSet<SymbolFreq>());
		sentenceNumber = 0;
		for(Box b : developCorpus) {
			sentenceNumber++;
			ArrayList<BoxStandard> boxCollector = new ArrayList<BoxStandard>();
			b.addStandardBoxesInCollection(boxCollector);
			for(BoxStandard bs : boxCollector) {
				int originalCat = bs.originalCat+1;
				String fullWordAsString = bs.getFullWordsAsString();
				if (fullWordAsString==null) {
					System.err.println(sentenceNumber + 
							": found std. box witout full words in develop: " + bs);
					continue;
				}
				SymbolString fullWord = new SymbolString(fullWordAsString);
				int[] freq = trainingCats.get(originalCat).get(fullWord);
				if (freq==null) freq = new int[]{0};				
				developCats.get(originalCat).add(new SymbolFreq(fullWord, freq[0]));
			}
		}
		printDevelopCats();
	}
	
	

	private void printDevelopCats() {
		String[] catNames = new String[]{"NoCat","Verbs","Adverbs","Nouns","Adjectives"};
		int catNumber = 0;
		for(TreeSet<SymbolFreq> devCat : developCats) {
			System.out.println("Develop lexicon: " + catNames[catNumber]);
			for(SymbolFreq sf : devCat) {
				System.out.println(sf.symbol() + "\t" + sf.freq());
			}
			System.out.println("\n\n");
			catNumber++;
		}
		
	}


	public static void main(String[] args) throws Exception {		
		File trainingFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		File developFile = new File(Wsj.WsjOriginal + "wsj-00.mrg");
		new FullWordsModel(trainingFile, developFile);				
	}
	
}
