package tsg.incremental.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.ArgumentReader;
import util.FixedSizeSortedSet;
import util.IntegerString;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class FragmentExtractor {
	
	static boolean debug = true;
	
	static boolean printFilteredFragmentsToFile;
	static boolean checkGoldLexPosCoverage;
	static boolean addMinimalFragments;
	static boolean smoothingFromFrags;
	static boolean smoothingFromMinSet;
	static int minFragFreqForSmoothingFromFrags = -1;
	static int minFragFreqForSmoothingFromMinSet = -1;
	static boolean printGenericFragsForSmoothing = false; 
	static int openClassPoSThreshold = 50; //Integer.MAX_VALUE;
	
	File logFile;	
	String outputPath;
	ArrayList<TSNodeLabel> trainTB, testTB;	
	public File fragmentsFile;
	HashMap<Label, HashSet<TSNodeLabel>> lexFragsTableAll, lexFragsTableFirstLex, lexFragsTableFirstSub;	
	HashMap<Label, HashSet<Label>> posLexTrain, lexPosTrain; 
	HashMap<Label, HashSet<Label>> posLexFinal, lexPosFinal;
	Set<Label> posSetTrain;
	HashSet<Label> internalNodesSetTrain;
	HashMap<Label, HashMap<TSNodeLabel,int[]>> posFragSmoothingFromFrags;
	HashMap<Label, HashMap<TSNodeLabel,int[]>> posFragSmoothingFromMinSet;
	HashMap<Label, HashSet<TSNodeLabel>> posFragSmoothingMerged;
	HashMap<Label,Integer> posFragSmoothingMergedBinSizes;	
	int novelWordPosPairsFromOpenClassPos, totalNumberGenericTemplates;
	
	public FragmentExtractor(File trainTBfile, File testTBfile, File fragmentsFile, 
			String outputPath) throws Exception {
		this(Wsj.getTreebank(trainTBfile),
				Wsj.getTreebank(testTBfile),
				fragmentsFile, outputPath);
	}
	
	public FragmentExtractor (ArrayList<TSNodeLabel> trainTB, ArrayList<TSNodeLabel> testTB, 
			File fragmentsFile, String outputPath) throws Exception {
		this.trainTB = trainTB;
		this.testTB = testTB;
		this.fragmentsFile = fragmentsFile;		
		this.outputPath = outputPath;		
	}
	
	public void extractFragments() throws Exception {
		Parameters.openLogFile(logFile);
			if (outputPath==null)
				outputPath = fragmentsFile.getParent() + "/";
			logFile = new File(outputPath + "log_FragExt.txt");
			extractFragments(logFile);
		Parameters.closeLogFile();
	}
	
	
	public void extractFragments(File logFile) throws Exception {			
		printParameters();
		initVariables();
		readLexiconFromTrainTB();			
		readFragmentsFile();
		addMinimalFragmentsFromTrainTB();
		buildFinalPosLex();
		filterGenericTableForSmoothing();
		mergingSmoothingTables();
		printFragmentsToFile();
		reportFragmentsStats();		
	}

	public HashSet<TSNodeLabel> getFreshWordFragsSetWithSmoothing(Label wordLabel) {
		HashSet<TSNodeLabel> result = new HashSet<TSNodeLabel>(		
				lexFragsTableAll.get(wordLabel));
		if (smoothingFromFrags || smoothingFromMinSet) {
			for(Label pos : lexPosFinal.get(wordLabel)) {
				HashSet<TSNodeLabel> posFrags = posFragSmoothingMerged.get(pos);
				if (posFrags==null)
					continue;
				for(TSNodeLabel frag : posFrags) {
					frag = frag.clone();
					TSNodeLabel lex = frag.getLeftmostLexicalNode();
					lex.label = wordLabel;
					result.add(frag);
				}
			}
		}
		return result;
	}

	private void initVariables() {
		lexFragsTableAll = new HashMap<Label, HashSet<TSNodeLabel>>();	 
		lexFragsTableFirstLex = new HashMap<Label, HashSet<TSNodeLabel>>();
		lexFragsTableFirstSub = new HashMap<Label, HashSet<TSNodeLabel>>();
		posFragSmoothingFromFrags = new HashMap<Label, HashMap<TSNodeLabel,int[]>>();
		posFragSmoothingFromMinSet = new HashMap<Label, HashMap<TSNodeLabel,int[]>>();
	}

	void printParameters() {
		
		Parameters.reportLineFlush("\n\n");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("FRAGMENT EXTRACTOR");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("");
		

		Parameters.reportLineFlush("Add Gold Pos Fragments: " + checkGoldLexPosCoverage);		
		Parameters.reportLineFlush("Add Minimal Fragments: " + addMinimalFragments);
		Parameters.reportLineFlush("Smoothing from frags: " + smoothingFromFrags);
		Parameters.reportLineFlush("Min Frag Freq for Smoothing from frags: " + minFragFreqForSmoothingFromFrags);
		Parameters.reportLineFlush("Smoothing from min set: " + smoothingFromMinSet);
		Parameters.reportLineFlush("Min Frag Freq for Smoothing from min set: " + minFragFreqForSmoothingFromMinSet);
		Parameters.reportLineFlush("Open-pos class threshold: " + openClassPoSThreshold);
		
	}
	
	private void readLexiconFromTrainTB() {
		posLexTrain = new HashMap<Label, HashSet<Label>>();
		lexPosTrain = new HashMap<Label, HashSet<Label>>();
		internalNodesSetTrain = new HashSet<Label>(); 
		PrintProgress pp = new PrintProgress("\nReading lexicon from training treebank (" + 
				trainTB.size() + ")");
		for(TSNodeLabel t : trainTB) {
			pp.next();
			ArrayList<TSNodeLabel> lexNodes = t.collectLexicalItems();
			for(TSNodeLabel l : lexNodes) {
				Label lexLabel = l.label;
				Label posLabel = l.parent.label;
				Utility.putInHashMap(posLexTrain, posLabel, lexLabel);
				Utility.putInHashMap(lexPosTrain, lexLabel, posLabel);
			}
			for(TSNodeLabel i : t.collectPhrasalNodes()) {
				Label intNodeLabel = i.label;
				internalNodesSetTrain.add(intNodeLabel);
			}			
		}
		pp.end();
		
		posSetTrain = posLexTrain.keySet(); 
		Parameters.reportLineFlush("\t Number of trees in training TB: " + trainTB.size());
		Parameters.reportLineFlush("\t Number of internal nodes: " + internalNodesSetTrain.size());
		Parameters.reportLineFlush("\t Number of pos: " + posLexTrain.size());
		Parameters.reportLineFlush("\t Number of lex: " + lexPosTrain.size());
		Parameters.reportLineFlush("\t Number of <pos,lex> pairs: " + Utility.countTotal(posLexTrain));
		Parameters.reportLineFlush("\t Number of <lex,pos> pairs: " + Utility.countTotal(lexPosTrain));
	}
	
	static Label emptyLabel = Label.getLabel("");

	private void readFragmentsFile() throws Exception {		
		if (fragmentsFile==null) {
			Parameters.reportLineFlush("\nEmpty Fragment File...");
			return;
		}
		Parameters.reportLineFlush("\nReading Fragments File...");		
		Scanner fragmentScan = FileUtil.getScanner(fragmentsFile);
		int totalFragments = 0;
		int totalLexicalizedFragments = 0;
		int[] lexFirstSecond = new int[2];
		PrintProgress progress = new PrintProgress("Reading Fragment", 10000, 0);
		while(fragmentScan.hasNextLine()) {
			totalFragments++;
			progress.next();
			String line = fragmentScan.nextLine();
			String[] treeFreq = line.split("\t");
			int freq = Integer.parseInt(treeFreq[1]);
			String fragmentString = treeFreq[0];
			TSNodeLabel fragment = new TSNodeLabel(fragmentString, false);
			List<TSNodeLabel> firstTwoTerminals = fragment.collectTerminalItems();
			firstTwoTerminals.add(null);
			firstTwoTerminals = firstTwoTerminals.subList(0, 2); 
			int firstLexIndex = -1;
			int firstPosIndex = -1;
			TSNodeLabel firstLexNode = null;
			TSNodeLabel firstPosNode = null;
			int i = 0;
			boolean doublePos = false;
			for(TSNodeLabel t : firstTwoTerminals) {
				if (t==null)
					break;
				if (firstLexIndex==-1 && t.isLexical) {
					firstLexNode = t;
					firstLexIndex = i;
					continue;
				}
				if (posSetTrain.contains(t.label)) {
					if (firstPosIndex==-1) {
						firstPosNode = t;
						firstPosIndex = i;
					}
					else 
						doublePos = true;
				}
				i++;
			}
			
			if (firstLexNode!=null) {
				boolean lexFirst = firstLexIndex==0;
				if (addLexFrag(firstLexNode, fragment, lexFirst)) {
					lexFirstSecond[firstLexIndex]++;
					totalLexicalizedFragments++;				
				}
				if (smoothingFromFrags) {
					TSNodeLabel fragClone = fragment.clone();
					firstLexNode = fragClone.getLeftmostLexicalNode();
					firstLexNode.relabel("");
					Utility.increaseInHashMap(posFragSmoothingFromFrags, 
							firstLexNode.parent.label, fragClone, freq);
					if (firstPosNode!=null) {
						fragClone = fragClone.clone();
						firstLexNode = fragClone.getLeftmostLexicalNode();
						firstLexNode.parent.daughters = null;
						firstPosNode = fragClone.getTerminalItemsAtPosition(1-firstLexIndex);
						firstPosNode.assignUniqueDaughter(new TSNodeLabel(emptyLabel, true));
						Utility.increaseInHashMap(posFragSmoothingFromFrags, 
								firstPosNode.label, fragClone, freq);
					}
				}
			}
			else if (smoothingFromFrags && firstPosNode!=null) {
				TSNodeLabel fragClone = fragment.clone();
				firstPosNode = fragClone.getLeftmostTerminalNode();
				firstPosNode.assignUniqueDaughter(new TSNodeLabel(emptyLabel, true));
				Utility.increaseInHashMap(posFragSmoothingFromFrags, 
						firstPosNode.label, fragClone, freq);
				if (doublePos) {
					fragClone = fragment.clone();
					TSNodeLabel secondPosNode = fragClone.getTerminalItemsAtPosition(1);
					secondPosNode.assignUniqueDaughter(new TSNodeLabel(emptyLabel, true));
					Utility.increaseInHashMap(posFragSmoothingFromFrags, 
							secondPosNode.label, fragClone, freq);
				}
			}
		}
		
		progress.end();
		Parameters.reportLineFlush("\tTotal Fragments: " + totalFragments);
		Parameters.reportLineFlush("\tTotal Accepted Fragments: " + totalLexicalizedFragments);
		Parameters.reportLineFlush("\t\tLex First: " + lexFirstSecond[0]);
		Parameters.reportLineFlush("\t\tLex Second: " + lexFirstSecond[1]);
		
	}

	private void addMinimalFragmentsFromTrainTB() {
		if (!addMinimalFragments)
			return;
		PrintProgress pp = new PrintProgress("\nAdding minimal fragments from training treebank (" + trainTB.size() + ")");		
		int newFrags = 0;
		int[] firstSecondFrags = new int[2];
		for(TSNodeLabel t : trainTB) {
			pp.next();
			ArrayList<TSNodeLabel> wordNodes = t.collectLexicalItems();			
			for(TSNodeLabel lexNode : wordNodes) {				
				TSNodeLabel p = lexNode.parent;
				TSNodeLabel d = lexNode;
				TSNodeLabel dFrag = d.clone();
				int lexIndex = 0;
				do {
					int dIndex = p.indexOfDaughter(d);
					lexIndex += dIndex;
					if (lexIndex>1)
						break;	
					TSNodeLabel frag = p.cloneCfg();
					frag.daughters[dIndex] = dFrag;
					dFrag.parent = frag;
					boolean firstLex = lexIndex==0;
					if (addLexFrag(lexNode, frag.clone(), firstLex)) {
						firstSecondFrags[lexIndex]++;
						newFrags++;						
					}
					if (smoothingFromMinSet) {
						TSNodeLabel fragClone = frag.clone();
						TSNodeLabel ln = fragClone.getLeftmostLexicalNode();
						ln.relabel("");
						Utility.increaseInHashMap(posFragSmoothingFromMinSet, 
								lexNode.parent.label, fragClone);
					}
					if (lexIndex==1)
						break;						
					d = p;
					p = p.parent;
					dFrag = frag;
				} while(p!=null);
			}
		}
		pp.end();
		Parameters.reportLineFlush("\t New lex fragments: " + newFrags);
		Parameters.reportLineFlush("\t\t lex first:" + firstSecondFrags[0]);
		Parameters.reportLineFlush("\t\t lex second:" + firstSecondFrags[1]);		
	}

	private void filterGenericTableForSmoothing() {
		if (smoothingFromFrags) {
			int[] genericFragsIndex = filterSmoothing(posFragSmoothingFromFrags,
					minFragFreqForSmoothingFromFrags);		
			Parameters.reportLineFlush("\nNumber of GENERIC frags for smoothing from frags: " + (Utility.sum(genericFragsIndex)));
			Parameters.reportLineFlush("\t lex first:  " + genericFragsIndex[0]);
			Parameters.reportLineFlush("\t lex second: " + genericFragsIndex[1]);
		}
		if (smoothingFromMinSet) {
			int[] genericFragsIndex = filterSmoothing(posFragSmoothingFromMinSet,
					minFragFreqForSmoothingFromMinSet);
			Parameters.reportLineFlush("\nNumber of GENERIC frags for smoothing from min set: " + (Utility.sum(genericFragsIndex)));
			Parameters.reportLineFlush("\t lex first:  " + genericFragsIndex[0]);
			Parameters.reportLineFlush("\t lex second: " + genericFragsIndex[1]);
		}
			
	}
	
	private static int[] filterSmoothing(
			HashMap<Label, HashMap<TSNodeLabel, int[]>>  posFragFreqTable, int minFreq) {
		
		int[] countFragsIndex = new int[2];
		Iterator<Label> posIter = posFragFreqTable.keySet().iterator();
		while(posIter.hasNext()) {
			Label pos = posIter.next();
			HashMap<TSNodeLabel, int[]> fragFreqTable = posFragFreqTable.get(pos);
			Iterator<TSNodeLabel> fragIter = fragFreqTable.keySet().iterator();
			while(fragIter.hasNext()) {
				TSNodeLabel frag = fragIter.next();
				int freq = fragFreqTable.get(frag)[0];
				if (freq<minFreq) {
					fragIter.remove();					
				}
				else {
					TSNodeLabel lex = frag.getLeftmostLexicalNode();
					TSNodeLabel sub = frag.getLeftmostTerminalNode();
					int index = lex==sub ? 0 : 1;
					countFragsIndex[index]++;
				}
			}
			if (fragFreqTable.isEmpty())
				posIter.remove();
		}
		return countFragsIndex;
	}
	
	private void mergingSmoothingTables() {
		posFragSmoothingMergedBinSizes = new HashMap<Label,Integer>();
		posFragSmoothingMerged = new HashMap<Label, HashSet<TSNodeLabel>>();		 
		totalNumberGenericTemplates = 0;		
		for(Label pos : posSetTrain) {
			HashSet<TSNodeLabel> mergedFrags = new HashSet<TSNodeLabel>(); 
			HashMap<TSNodeLabel, int[]> posFragsFromFrags = posFragSmoothingFromFrags.get(pos);
			if (posFragsFromFrags!=null) {
				mergedFrags.addAll(posFragsFromFrags.keySet());
			}
			HashMap<TSNodeLabel, int[]> posFragsFromMinSet = posFragSmoothingFromMinSet.get(pos);
			if (posFragsFromMinSet!=null) {
				mergedFrags.addAll(posFragsFromMinSet.keySet());
			}
			if (!mergedFrags.isEmpty()) {
				int size = mergedFrags.size();
				totalNumberGenericTemplates += size; 
				posFragSmoothingMerged.put(pos, mergedFrags);
				posFragSmoothingMergedBinSizes.put(pos, size);				
			}
			
		}
		Parameters.reportLineFlush("\nNumber of GENERIC frags for smoothing (MERGED): " + totalNumberGenericTemplates);
		//Parameters.reportLineFlush("\tPOS sizes: " + posFragSmoothingMergedBinSizes.toString());
	}


	
	private boolean addLexFrag(TSNodeLabel lexNode, TSNodeLabel fragment, boolean lexFirst) {
		
		Label lexNodeLabel = lexNode.label; 
		boolean result = Utility.putInHashMap(lexFragsTableAll, lexNodeLabel,fragment);
		if (result) {			
			if (lexFirst) {
				Utility.putInHashMap(lexFragsTableFirstLex, lexNodeLabel,fragment);
			}
			else
				Utility.putInHashMap(lexFragsTableFirstSub, lexNodeLabel,fragment);			
		}
		return result;
	}


	private void buildFinalPosLex() {
		
		Parameters.reportLineFlush("\nBuilding Final PosLex Set...");

		posLexFinal = Utility.deepHashMapClone(posLexTrain);
		lexPosFinal = Utility.deepHashMapClone(lexPosTrain);
		
		novelWordPosPairsFromOpenClassPos = 0;
		
		HashMap<Label, Integer> openPos = new HashMap<Label, Integer>();
		HashMap<Label, Integer> closedPos = new HashMap<Label, Integer>();
		for(Entry<Label, HashSet<Label>> e : posLexFinal.entrySet()) {
			Label pos = e.getKey();
			int wordCount = e.getValue().size();
			if (wordCount>openClassPoSThreshold)
				openPos.put(pos,wordCount);
			else
				closedPos.put(pos,wordCount);
		}				
		
		Parameters.reportLineFlush("\nOpen-class PoS (" + openPos.size() + "): " + openPos);
		Parameters.reportLineFlush("\nClosed-class PoS (" + closedPos.size() + "): " +  closedPos);
		
		for(Entry<Label, HashSet<Label>> e : lexPosFinal.entrySet()) {
			Label lex = e.getKey();
			HashSet<Label> lexPosSet = e.getValue();
			boolean hasOpen = false;
			HashSet<Label> newOpenClass = new HashSet<Label>(); 
			for(Label p : openPos.keySet()) {
				if (lexPosSet.contains(p))
					hasOpen = true;
				else
					newOpenClass.add(p);
			}
			if (hasOpen) {
				// word is open class			
				novelWordPosPairsFromOpenClassPos += newOpenClass.size();
				lexPosSet.addAll(newOpenClass);				
				for(Label p : newOpenClass) {
					posLexFinal.get(p).add(lex);
					TSNodeLabel fragment = new TSNodeLabel(p,false);
					TSNodeLabel lexNode = new TSNodeLabel(lex,true);
					fragment.assignUniqueDaughter(lexNode);
					addLexFrag(lexNode, fragment, true);
				}
			}			
		}
		
		Parameters.reportLineFlush("\nNovel word-pos pairs (open PoS smoothing): " + novelWordPosPairsFromOpenClassPos);
		Parameters.reportLineFlush("Total final word-pos pairs: " + Utility.countTotal(posLexFinal));
		Parameters.reportLineFlush("Total final pos-word pairs: " + Utility.countTotal(lexPosFinal));
		
		if (checkGoldLexPosCoverage) {
			Parameters.reportLineFlush("\nChecking Pos-Lex in test treebank...");
			boolean problems = false;
			for(TSNodeLabel t : testTB) {
				ArrayList<TSNodeLabel> lexNodes = t.collectLexicalItems();
				for(TSNodeLabel lex : lexNodes) {
					Label lexLabel = lex.label;
					if (!lexPosFinal.containsKey(lexLabel)) {
						problems = true;
						Parameters.reportError("\nLexicon missing word in test: " + lexLabel);
						Parameters.reportError("(adding it with open PoS but this should not happen)");
						lexPosFinal.put(lexLabel, new HashSet<Label>(openPos.keySet()));
						novelWordPosPairsFromOpenClassPos += openPos.size();
						for(Label p : openPos.keySet()) {
							posLexFinal.get(p).add(lexLabel);
							TSNodeLabel fragment = new TSNodeLabel(p,false);
							TSNodeLabel lexNode = new TSNodeLabel(lexLabel,true);
							fragment.assignUniqueDaughter(lexNode);
							addLexFrag(lexNode, fragment, true);
						}	
					}
					Label posLabel = lex.parent.label;
					if (!lexPosFinal.get(lexLabel).contains(posLabel)) {
						problems = true;
						Parameters.reportError("\nLexicon missing word-pos pair in test: " +
								"<" + posLabel + ", " + lexLabel + ">");
					}							
				}
			}				
			if (!problems)
				Parameters.reportLineFlush("All OK!");
		}		
	}

	private void printFragmentsToFile() {
		if (printFilteredFragmentsToFile) {
			File filteredFragmentsFile = new File(outputPath + "lexFrags.mrg");			
			Parameters.reportLineFlush("Printing frags to: " + filteredFragmentsFile);			
			PrintWriter pw = FileUtil.getPrintWriter(filteredFragmentsFile);
			for(HashSet<TSNodeLabel> fragList : lexFragsTableAll.values()) {
				for(TSNodeLabel f : fragList) {
					pw.println(f.toString(false, true));					
				}
			}
			pw.close();
		}		
		if (printGenericFragsForSmoothing) {
			File gericFragsForSmoothingFile = new File(outputPath + 
					"fragsForSmoothingFromFrags_" + minFragFreqForSmoothingFromFrags + ".mrg");
			Parameters.reportLineFlush("Printing generic frags for smoothing from frags to: " + gericFragsForSmoothingFile);
			PrintWriter pw = FileUtil.getPrintWriter(gericFragsForSmoothingFile);
			printGenericFragments(posFragSmoothingFromFrags, pw);
			pw.close();
			
			gericFragsForSmoothingFile = new File(outputPath + 
					"fragsForSmoothingFromMinSet_" + minFragFreqForSmoothingFromMinSet + ".mrg");
			Parameters.reportLineFlush("Printing generic frags for smoothing from min set to: " + gericFragsForSmoothingFile);
			pw = FileUtil.getPrintWriter(gericFragsForSmoothingFile);
			printGenericFragments(posFragSmoothingFromMinSet, pw);
			pw.close();
		}
	}
	
	private static void printGenericFragments(
			HashMap<Label, HashMap<TSNodeLabel, int[]>> table, PrintWriter pw) {
		for(Entry<Label,HashMap<TSNodeLabel,int[]>> e : table.entrySet()) {
			pw.println(e.getKey());
			for(Entry<TSNodeLabel,int[]> f : e.getValue().entrySet()) {
				pw.println("\t" + f.getKey().toString(false, true));					
			}
			pw.println();
		}
	}
	
	private int totalNumberOfFragFromSmoothing(Label lex) {
		int result = 0;
		for(Label pos : lexPosFinal.get(lex)) {
			Integer binSize = posFragSmoothingMergedBinSizes.get(pos);
			if (binSize!=null)
				result += binSize;
		}
		return result;
	}

	private void reportFragmentsStats() {
		
		TreeSet<IntegerString> posSizeOrdered = new TreeSet<IntegerString>();
		for(Entry<Label,Integer> e : posFragSmoothingMergedBinSizes.entrySet()) {
			posSizeOrdered.add(new IntegerString(e.getValue(),e.getKey().toString()));
		}
		
		Parameters.reportLine("\nSmoothing PoS fragments statistics:");
		Parameters.reportLine("SIZE\tPOS");
		for(IntegerString is : posSizeOrdered) {
			Parameters.reportLine(is.getInteger() + "\t" + is.getString());
		}
		Parameters.reportLineFlush("");
		
		FixedSizeSortedSet<IntegerString> tenMostProlificWords = 
			new FixedSizeSortedSet<IntegerString>(10);

		int totalLexFrags = 0;
		int totalSmoothedFrags = 0;

		for(Entry<Label,HashSet<TSNodeLabel>> lexFragList : lexFragsTableAll.entrySet()) {
			Label lex = lexFragList.getKey();
			int sizeFromLexFrags = lexFragList.getValue().size();
			int sizeFromSmoothing = totalNumberOfFragFromSmoothing(lex);
			int total = sizeFromLexFrags+sizeFromSmoothing;
			totalLexFrags += sizeFromLexFrags;
			totalSmoothedFrags += sizeFromSmoothing;			
			String description = lex.toString() + "\t" + sizeFromLexFrags + "\t" + sizeFromSmoothing;
			IntegerString lexSizeDescription = new IntegerString(total,description);
			tenMostProlificWords.add(lexSizeDescription);
		}
		
		Parameters.reportLine("\nTen most prolific words:");
		Parameters.reportLine("TOTAL\tLEX\tFRAGS\t_SMOOTH");
		for(IntegerString is : tenMostProlificWords) {
			Parameters.reportLine(is.getInteger() + "\t" + is.getString());
		}
		Parameters.reportLineFlush("");
		
		String[] words = new String[]{"is","make","can","saw","of","about","the","often",
				"well","good","car","person","year",","};
		
		Parameters.reportLine("\nOther selected words:");
		Parameters.reportLine("TOTAL\tLEX\tFRAGS\tSMOOTH");
		for(String w : words) {
			Label lex = Label.getLabel(w);
			HashSet<TSNodeLabel> wordSet = lexFragsTableAll.get(lex);
			if (wordSet==null)
				continue;
			int sizeFromLexFrags = wordSet.size();
			int sizeFromSmoothing = totalNumberOfFragFromSmoothing(lex);
			int total = sizeFromLexFrags+sizeFromSmoothing;
			String description = lex.toString() + "\t" + sizeFromLexFrags + "\t" + sizeFromSmoothing;
			Parameters.reportLine(total + "\t" + description);
		}
		Parameters.reportLineFlush("");
		
		int totalLexFragsWithoutOpenClassSmoothing = totalLexFrags - novelWordPosPairsFromOpenClassPos;
		int totalFirstLex = Utility.countTotal(lexFragsTableFirstLex);
		int totalFirstLexWithouthOpenClassSmoothing = totalFirstLex-novelWordPosPairsFromOpenClassPos;
		int totalFirstsub = Utility.countTotal(lexFragsTableFirstSub);
						
		Parameters.reportLineFlush("\nTOTAL LEXICALIZED FRAGS (from Frags and MinSet): " + totalLexFragsWithoutOpenClassSmoothing);
		Parameters.reportLineFlush("\tLex first: " + totalFirstLexWithouthOpenClassSmoothing); 
		Parameters.reportLineFlush("\tSub first: " + totalFirstsub);
		
		
		Parameters.reportLineFlush("\nTOTAL SMOOTHED OPEN POS-LEX PAIRS: " + novelWordPosPairsFromOpenClassPos);
		
		Parameters.reportLineFlush("\nTOTAL LEXICALIZED FRAGS (after open-pos smoothing) " + totalLexFrags);
		Parameters.reportLineFlush("\tLex first: " + totalFirstLex);
		Parameters.reportLineFlush("\tSub first: " + totalFirstsub);
		
		Parameters.reportLineFlush("\nTOTAL GENERIC TEMPLATES: " + totalNumberGenericTemplates);
		
		Parameters.reportLineFlush("\nTOTAL (VIRTUAL) GENERIC TEMPLATES: " + totalSmoothedFrags);
		
		Parameters.reportLineFlush("\nTOTAL (VIRTUAL) FRAGS IN LEXICON: " + (totalLexFrags+totalSmoothedFrags));
		
	}
	
	public static FragmentExtractor getDefaultFragmentExtractor(String workingPath, 
			String minSetSetting, String fragSetSetting,
			File trainTB, File testTB, File fragFile) throws Exception {
		
		debug = true;		
		checkGoldLexPosCoverage = true;
		printFilteredFragmentsToFile = false;
		printGenericFragsForSmoothing = false;
		
		addMinimalFragments = !minSetSetting.toUpperCase().equals("NO");
		smoothingFromMinSet = addMinimalFragments && !minSetSetting.toUpperCase().equals("NOSMOOTHING");
		minFragFreqForSmoothingFromMinSet = smoothingFromMinSet ? Integer.parseInt(minSetSetting) : -1; 			
		
		boolean addFragments = !fragSetSetting.toUpperCase().equals("NO");
		smoothingFromFrags = addFragments && !fragSetSetting.toUpperCase().equals("NOSMOOTHING");
		minFragFreqForSmoothingFromFrags = smoothingFromFrags ? Integer.parseInt(fragSetSetting) : -1;
			
		File fragmentsFile = addFragments ?
				fragFile : null;
		
		return new FragmentExtractor(trainTB, testTB, fragmentsFile, workingPath);
		
	}



	public static void main(String[] args) throws Exception {
		
		debug = true;		
		checkGoldLexPosCoverage = true;		
		addMinimalFragments = true;
		printFilteredFragmentsToFile = false;
		smoothingFromFrags = false;
		minFragFreqForSmoothingFromFrags = 100;
		smoothingFromMinSet = true;
		minFragFreqForSmoothingFromMinSet = 1;
		printGenericFragsForSmoothing = true;
		
		String homePath = System.getProperty("user.home");
		String outputPath = homePath + "/PLTSG/WSJ_RightBin_H0V1_UK4/";		
		File trainTB = new File(outputPath + "wsj-02-21.mrg");
		//File testTB = new File("./treeBaseball.mrg");
		File testTB = new File(outputPath + "wsj-22.mrg");
		//File testTB = new File(outputPath + "wsj-22_disclosed.mrg");
		//File testTB = new File(outputPath + "wsj-22_advertising.mrg");
		//File fragmentsFile = new File("./lexFrags.mrg");
		
		//File fragmentsFile = new File(outputPath + "lexFrags.mrg");
		//File fragmentsFile = new File(outputPath + "fragments_approxFreq.txt");
		File fragmentsFile = null;
		
		//for (int threshold : new int[]{-1, 2,3,5,10,20}) {
			//restricFragmentsWithFreqGreaterThan = threshold;
			new FragmentExtractor(trainTB, testTB, fragmentsFile, outputPath);
		//}
			
			
		/*
		//printFilteredFragmetnsToFile = true;
		File treebankFile = new File(args[0]);
		File fragmentsFile = new File(args[1]);
		String outputPath = args[2];
		threads = ArgumentReader.readIntOption(args[2]);		
		for(int j : new int[]{5,10,20,40,-1}) { //new int[]{5,10,20,40,-1} 
			treeLenghtLimit = j;
			new IncrementalTSGChecker(treebankFile, fragmentsFile, threads, outputPath).run();
			System.out.println();
		}
		*/
	}
	
}
