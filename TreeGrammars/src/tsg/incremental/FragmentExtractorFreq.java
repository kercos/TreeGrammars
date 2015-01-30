package tsg.incremental;

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
import util.FixedSizeSortedSet;
import util.IntegerString;
import util.PrintProgress;
import util.Utility;
import util.file.FileUtil;

public class FragmentExtractorFreq {
	
	static final Label nullaryLabel = Label.getLabel("_");
	
	static boolean debug = true;

	static boolean printAllLexFragmentsToFile;
	static boolean printTemplateFragsToFile = false;
	
	static boolean posSmoothing = true;
	static boolean checkGoldLexPosCoverage;
	static boolean addMinimalFragments;

	static boolean useAllLexFrags = false;
	
	static boolean smoothingFromFrags;
	static boolean smoothingFromMinSet;
	static int minFragFreqForSmoothingFromFrags = -1;
	static int minFragFreqForSmoothingFromMinSet = -1;	
	 
	static int openClassPoSThreshold = 50; //Integer.MAX_VALUE;
	
	File logFile;	
	String outputPath;
	ArrayList<TSNodeLabel> trainTB, testTB;	
	File fragmentsFile;
	
	HashMap<Label, HashMap<TSNodeLabel,int[]>> //indexed on first lex
		//lexFragsTableAll, 
		lexFragsTableFirstLex, 
		lexFragsTableFirstSub;
	
	HashMap<Label, HashMap<Label,int[]>> posLexTrain, lexPosTrain; 
	HashMap<Label, HashMap<Label,int[]>> posLexFinal, lexPosFinal;
	HashMap<Label, int[]> topFreq;
	Set<Label> posSetFinal;
	HashMap<Label,int[]> posSetFreqFinal;
	HashSet<Label> internalNodesSetTrain;
	HashMap<Label, HashMap<TSNodeLabel,int[]>> posFragSmoothingFromFrags;
	HashMap<Label, HashMap<TSNodeLabel,int[]>> posFragSmoothingFromMinSet;
	HashMap<Label, HashMap<TSNodeLabel,int[]>> posFragSmoothingMerged;
	HashMap<Label,Integer> posFragSmoothingMergedBinSizes;	
	int novelWordPosPairsFromOpenClassPos, totalNumberGenericTemplates;
	
	public FragmentExtractorFreq(File trainTBfile, File testTBfile, File fragmentsFile, 
			String outputPath) throws Exception {
		this(Wsj.getTreebank(trainTBfile),
				Wsj.getTreebank(testTBfile),
				fragmentsFile, outputPath);
	}
	
	public FragmentExtractorFreq (ArrayList<TSNodeLabel> trainTB, ArrayList<TSNodeLabel> testTB, 
			File fragmentsFile, String outputPath) throws Exception {
		this.trainTB = trainTB;
		this.testTB = testTB;
		this.fragmentsFile = fragmentsFile;		
		this.outputPath = outputPath;		
	}
	
	public void extractFragments() throws Exception {		
		if (outputPath==null)
		outputPath = fragmentsFile.getParent() + "/";
		logFile = new File(outputPath + "log_FragExtFreq.txt");
		Parameters.openLogFile(logFile);
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
		HashSet<TSNodeLabel> result = new HashSet<TSNodeLabel>();
		result.addAll(lexFragsTableFirstLex.get(wordLabel).keySet());		
		result.addAll(lexFragsTableFirstSub.get(wordLabel).keySet());
		if (smoothingFromFrags || smoothingFromMinSet) {
			for(Label pos : lexPosFinal.get(wordLabel).keySet()) {
				Set<TSNodeLabel> posFrags = posFragSmoothingMerged.get(pos).keySet();
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
		lexFragsTableFirstLex = new HashMap<Label, HashMap<TSNodeLabel,int[]>>();
		lexFragsTableFirstSub = new HashMap<Label, HashMap<TSNodeLabel,int[]>>();
		posFragSmoothingFromFrags = new HashMap<Label, HashMap<TSNodeLabel,int[]>>();
		posFragSmoothingFromMinSet = new HashMap<Label, HashMap<TSNodeLabel,int[]>>();
	}

	void printParameters() {
		
		Parameters.reportLineFlush("\n\n");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("FRAGMENT EXTRACTOR FREQ");
		Parameters.reportLineFlush("------------------------------------");
		Parameters.reportLineFlush("");
		
		Parameters.reportLineFlush("Check Gold Pos-Lex pairs: " + checkGoldLexPosCoverage);		
		Parameters.reportLineFlush("Add Minimal Fragments: " + addMinimalFragments);
		Parameters.reportLineFlush("Smoothing from frags: " + smoothingFromFrags);
		Parameters.reportLineFlush("Min Frag Freq for Smoothing from frags: " + minFragFreqForSmoothingFromFrags);
		Parameters.reportLineFlush("Smoothing from min set: " + smoothingFromMinSet);
		Parameters.reportLineFlush("Min Frag Freq for Smoothing from min set: " + minFragFreqForSmoothingFromMinSet);
		Parameters.reportLineFlush("Open-pos class threshold: " + openClassPoSThreshold);
		
	}
	
	private void readLexiconFromTrainTB() {
		posLexTrain = new HashMap<Label, HashMap<Label,int[]>>();
		lexPosTrain = new HashMap<Label, HashMap<Label,int[]>>();
		posSetFreqFinal = new HashMap<Label, int[]>();
		internalNodesSetTrain = new HashSet<Label>(); 
		topFreq = new HashMap<Label, int[]>();
		PrintProgress pp = new PrintProgress("\nReading lexicon from training treebank (" + 
				trainTB.size() + ")");
		for(TSNodeLabel t : trainTB) {
			pp.next();
			increaseTopNodes(t);
			ArrayList<TSNodeLabel> lexNodes = t.collectLexicalItems();
			for(TSNodeLabel l : lexNodes) {
				Label lexLabel = l.label;
				Label posLabel = l.parent.label;
				Utility.increaseInHashMap(posLexTrain, posLabel, lexLabel);
				Utility.increaseInHashMap(lexPosTrain, lexLabel, posLabel);
				Utility.increaseInHashMap(posSetFreqFinal, posLabel);
			}
			for(TSNodeLabel i : t.collectPhrasalNodes()) {
				Label intNodeLabel = i.label;
				internalNodesSetTrain.add(intNodeLabel);
			}			
		}
		pp.end();
		
		posSetFinal = posSetFreqFinal.keySet();
		
		Parameters.reportLineFlush("\t Number of trees in training TB: " + trainTB.size());
		Parameters.reportLineFlush("\t Number of internal nodes: " + internalNodesSetTrain.size());
		Parameters.reportLineFlush("\t Number of pos: " + posLexTrain.size());
		Parameters.reportLineFlush("\t Number of lex: " + lexPosTrain.size());
		Parameters.reportLineFlush("\t Number of <pos,lex> pairs: " + Utility.countTotalElements(posLexTrain));
		Parameters.reportLineFlush("\t Number of <lex,pos> pairs: " + Utility.countTotalElements(lexPosTrain));
	}
	
	private void increaseTopNodes(TSNodeLabel t) {
		do {
			Utility.increaseInHashMap(topFreq, t.label);
			if (t.prole()>1 || t.isPreLexical())
				break;
			t = t.firstDaughter();
		} while(true);	
		
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
			
			int[] freq = new int[3];
			freq[0] = Integer.parseInt(treeFreq[1]);
			freq[1] = Integer.parseInt(treeFreq[2]);
			freq[2] = Integer.parseInt(treeFreq[3]);
			
			String fragmentString = treeFreq[0];
			TSNodeLabel fragment = new TSNodeLabel(fragmentString, false);
			List<TSNodeLabel> terminals = fragment.collectTerminalItems();
			terminals.add(null); 
			int firstLexIndex = -1;
			TSNodeLabel firstLexNode = null;
			
			int i = 0;
			for(TSNodeLabel t : terminals) {
				if (t==null)
					break;
				if (firstLexIndex==-1 && t.isLexical) {
					firstLexNode = t;
					firstLexIndex = i;
					break;
				}
				i++;
			}
			
			if (firstLexNode!=null) {
				totalLexicalizedFragments++;	
				boolean recycleFrag = firstLexIndex>1;
				if (recycleFrag) {
					if (useAllLexFrags) {
						TSNodeLabel[] firstLexFirstSub = getFirstLexFirstSub(firstLexNode);
						boolean lexFirst = true;
						for(TSNodeLabel recycledFrag : firstLexFirstSub) {
							if (insertLexFrag(firstLexNode.label, recycledFrag, lexFirst, freq)) {
								lexFirstSecond[lexFirst ? 0 : 1]++;									
							}
							if (smoothingFromFrags) {
								TSNodeLabel fragClone = recycledFrag.clone();
								firstLexNode = fragClone.getLeftmostLexicalNode();
								firstLexNode.relabel("");
								Utility.increaseInHashMap(posFragSmoothingFromFrags, 
										firstLexNode.parent.label, fragClone, freq);
							}
							lexFirst = false;
						}
					}										
				}
				else {
					boolean lexFirst = firstLexIndex==0;				
					if (insertLexFrag(firstLexNode.label, fragment, lexFirst, freq)) {
						lexFirstSecond[firstLexIndex]++;	
					}
					if (smoothingFromFrags) {
						TSNodeLabel fragClone = fragment.clone();
						firstLexNode = fragClone.getLeftmostLexicalNode();
						firstLexNode.relabel("");
						Utility.increaseInHashMap(posFragSmoothingFromFrags, 
								firstLexNode.parent.label, fragClone, freq);
					}
				}
			}

		}
		
		progress.end();
		Parameters.reportLineFlush("\tTotal Fragments: " + totalFragments);
		Parameters.reportLineFlush("\t\tAccepted Fragments: " + totalLexicalizedFragments);
		Parameters.reportLineFlush("\t\tRefused Fragments: " + (totalFragments-totalLexicalizedFragments));
		Parameters.reportLineFlush("\tFragments in grammar: " + Utility.sum(lexFirstSecond));
		Parameters.reportLineFlush("\t\tLex First: " + lexFirstSecond[0]);
		Parameters.reportLineFlush("\t\tLex Second: " + lexFirstSecond[1]);
		
	}
	
	private TSNodeLabel[] getFirstLexFirstSub(TSNodeLabel lex) {		
		TSNodeLabel d = lex;
		TSNodeLabel p = d.parent;
		while(p!=null) {
			if (p.firstDaughter()!=d) {
				TSNodeLabel firstSubFrag = p.clone();
				firstSubFrag.firstDaughter().daughters=null;
				TSNodeLabel firstLexFrag = d.clone();				
				return new TSNodeLabel[]{firstLexFrag};//firstSubFrag
			}
			d = p;
			p = d.parent;
		}
		return null;
	}

	private void addMinimalFragmentsFromTrainTB() {
		if (!addMinimalFragments)
			return;
		PrintProgress pp = new PrintProgress("\nAdding min set fragments from training treebank ("
				+ trainTB.size() + ")");		
		int newFrags = 0;
		int[] firstSecondFrags = new int[2];
		for(TSNodeLabel t : trainTB) {
			pp.next();
			ArrayList<TSNodeLabel> wordNodes = t.collectLexicalItems();
			Iterator<TSNodeLabel> wordNodesIter = wordNodes.iterator();
			boolean firstWord = true;
			while(wordNodesIter.hasNext()) {	
				TSNodeLabel lexNode = wordNodesIter.next();
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
					boolean lastLex = !wordNodesIter.hasNext();
					int[] freq = new int[]{1, firstWord ? 1 : 0, lastLex ? 1 : 0};
					if (increaseLexFrag(lexNode.label, frag.clone(), firstLex, freq)) {
						firstSecondFrags[lexIndex]++;
						newFrags++;						
					}
					if (smoothingFromMinSet) {
						TSNodeLabel fragClone = frag.clone();
						TSNodeLabel ln = fragClone.getLeftmostLexicalNode();
						ln.relabel("");
						Utility.increaseInHashMap(posFragSmoothingFromMinSet, 
								lexNode.parent.label, fragClone, freq);
					}
					if (lexIndex==1)
						break;						
					d = p;
					p = p.parent;
					dFrag = frag;
				} while(p!=null);
				firstWord = false;
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
		posFragSmoothingMerged = new HashMap<Label, HashMap<TSNodeLabel,int[]>>();		 
		totalNumberGenericTemplates = 0;		
		for(Label pos : posSetFinal) {
			HashMap<TSNodeLabel,int[]> mergedFrags = new HashMap<TSNodeLabel,int[]>(); 
			HashMap<TSNodeLabel, int[]> posFragsFromFrags = posFragSmoothingFromFrags.get(pos);
			if (posFragsFromFrags!=null) {				
				mergedFrags.putAll(posFragsFromFrags);
			}
			HashMap<TSNodeLabel, int[]> posFragsFromMinSet = posFragSmoothingFromMinSet.get(pos);
			if (posFragsFromMinSet!=null) {
				for(Entry<TSNodeLabel,int[]> e : posFragsFromMinSet.entrySet()) {
					Utility.increaseInHashMap(mergedFrags, e.getKey(), e.getValue());
				}
				//mergedFrags.putAll(posFragsFromMinSet);
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


	
	private boolean insertLexFrag(
			Label lexNodeLabel, TSNodeLabel fragment, boolean lexFirst, int[] freq) {
		
		if (lexFirst)		
			return Utility.maximizeInHashMap(lexFragsTableFirstLex, lexNodeLabel,fragment, freq);

		// first sub	
		return Utility.maximizeInHashMap(lexFragsTableFirstSub, lexNodeLabel,fragment, freq);
	}

	private boolean increaseLexFrag(
			Label lexNodeLabel, TSNodeLabel fragment, boolean lexFirst, int[] toAdd) {
		
		if (lexFirst)
			return Utility.increaseInHashMap(lexFragsTableFirstLex, lexNodeLabel,fragment, toAdd);
		
		// first sub
		return Utility.increaseInHashMap(lexFragsTableFirstSub, lexNodeLabel,fragment, toAdd);			
	}
	
	static boolean getFirstWordInNullaryFrag(TSNodeLabel fragment, Label[] firstLex, boolean[] lexFirst) {
		ArrayList<TSNodeLabel> terms = fragment.collectTerminalItems();
		lexFirst[0] = true;
		for(TSNodeLabel t : terms) {
			if (t.isLexical) {
				if (t.label==nullaryLabel)
					continue;
				firstLex[0] = t.label;
				return true;
			}
			if (!lexFirst[0])
				return false;
			lexFirst[0] = false;
		}		
		// no real lex found
		return true; 
	}


	private void buildFinalPosLex() {
		
		Parameters.reportLineFlush("\nBuilding Final PosLex Set...");

		posLexFinal = Utility.deepHashMapCloneFreq(posLexTrain);
		lexPosFinal = Utility.deepHashMapCloneFreq(lexPosTrain);
		
		if (posSmoothing) {
		
			novelWordPosPairsFromOpenClassPos = 0;
			
			HashMap<Label, Integer> openPos = new HashMap<Label, Integer>();
			HashMap<Label, Integer> closedPos = new HashMap<Label, Integer>();
			for(Entry<Label, HashMap<Label,int[]>> e : posLexFinal.entrySet()) {
				Label pos = e.getKey();
				int wordCount = e.getValue().size();
				if (wordCount>openClassPoSThreshold)
					openPos.put(pos,wordCount);
				else
					closedPos.put(pos,wordCount);
			}				
			
			Parameters.reportLineFlush("\nOpen-class PoS (" + openPos.size() + "): " + openPos);
			Parameters.reportLineFlush("\nClosed-class PoS (" + closedPos.size() + "): " +  closedPos);
			
			for(Entry<Label, HashMap<Label,int[]>> e : lexPosFinal.entrySet()) {
				Label lex = e.getKey();
				HashMap<Label,int[]> value = e.getValue();
				boolean hasOpen = false;
				HashSet<Label> newOpenClass = new HashSet<Label>(); 
				for(Label p : openPos.keySet()) {
					if (value.containsKey(p))
						hasOpen = true;
					else
						newOpenClass.add(p);
				}
				if (hasOpen) {
					// word is open class			
					novelWordPosPairsFromOpenClassPos += newOpenClass.size();
					for(Label oc : newOpenClass) {
						value.put(oc, new int[]{0});
					}				
					for(Label p : newOpenClass) {
						posLexFinal.get(p).put(lex,new int[]{0});
						TSNodeLabel fragment = new TSNodeLabel(p,false);
						TSNodeLabel lexNode = new TSNodeLabel(lex,true);
						fragment.assignUniqueDaughter(lexNode);
						insertLexFrag(lexNode.label, fragment, true, new int[]{0,0});
					}
				}			
			}
			
			Parameters.reportLineFlush("\nNovel word-pos pairs (open PoS smoothing): " + novelWordPosPairsFromOpenClassPos);
			Parameters.reportLineFlush("Total final word-pos pairs: " + Utility.countTotalElements(posLexFinal));
			Parameters.reportLineFlush("Total final pos-word pairs: " + Utility.countTotalElements(lexPosFinal));
		
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
							lexPosFinal.put(lexLabel, 
									Utility.convertHashSetInHashMap(openPos.keySet(),0));
							novelWordPosPairsFromOpenClassPos += openPos.size();
							for(Label p : openPos.keySet()) {
								posLexFinal.get(p).put(lexLabel,new int[]{0});
								TSNodeLabel fragment = new TSNodeLabel(p,false);
								TSNodeLabel lexNode = new TSNodeLabel(lexLabel,true);
								fragment.assignUniqueDaughter(lexNode);
								increaseLexFrag(lexNode.label, fragment, true, new int[]{0,0});
							}	
						}
						Label posLabel = lex.parent.label;
						if (!lexPosFinal.get(lexLabel).containsKey(posLabel)) {
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
	}


	private void printFragmentsToFile() {
		if (printAllLexFragmentsToFile) {			
			File filteredFragmentsFile = new File(outputPath + "firstLexFrags.mrg");
			Parameters.reportLine("Printing first lex fragments to " + filteredFragmentsFile);			
			PrintWriter pw = FileUtil.getPrintWriter(filteredFragmentsFile);
			for(HashMap<TSNodeLabel, int[]> fragListFreq : lexFragsTableFirstLex.values()) {
				for(Entry<TSNodeLabel, int[]> e : fragListFreq.entrySet()) {
					pw.println(e.getKey().toString(false, true) + "\t" + 
							Utility.arrayToString(e.getValue(),'\t'));					
				}
			}
			pw.close();
			filteredFragmentsFile = new File(outputPath + "firstSubFrags.mrg");
			Parameters.reportLine("Printing first lex fragments to " + filteredFragmentsFile);			
			pw = FileUtil.getPrintWriter(filteredFragmentsFile);
			for(HashMap<TSNodeLabel, int[]> fragListFreq : lexFragsTableFirstSub.values()) {
				for(Entry<TSNodeLabel, int[]> e : fragListFreq.entrySet()) {
					pw.println(e.getKey().toString(false, true) + "\t" + 
							Utility.arrayToString(e.getValue(),'\t'));					
				}
			}
			pw.close();
		}		
		if (printTemplateFragsToFile) {
			File gericFragsForSmoothingFile = new File(outputPath + 
					"templatesFromFrags_" + minFragFreqForSmoothingFromFrags + ".mrg");
			Parameters.reportLine("Printing template fragments from recurring frags to " + gericFragsForSmoothingFile);
			PrintWriter pw = FileUtil.getPrintWriter(gericFragsForSmoothingFile);
			printGenericFragments(posFragSmoothingFromFrags, pw);
			pw.close();
			
			gericFragsForSmoothingFile = new File(outputPath + 
					"templatesFromMinSet_" + minFragFreqForSmoothingFromMinSet + ".mrg");
			Parameters.reportLine("Printing template fragments from minset to " + gericFragsForSmoothingFile);
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
				pw.println("\t" + f.getKey().toString(false, true) + 
						"\t" + Utility.arrayToString(f.getValue(),'\t'));					
			}
			pw.println();
		}
	}
	
	private int totalNumberOfFragFromSmoothing(Label lex) {
		HashMap<Label, int[]> t = lexPosFinal.get(lex);
		/*		
		if (t==null) {
			System.err.println(lex + " is not a key of this table!");
			return 0;
		}
		*/
		int result = 0;
		for(Label pos : t.keySet()) {
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

		for(Entry<Label,HashMap<TSNodeLabel,int[]>> lexFragList : lexFragsTableFirstLex.entrySet()) {
			Label lex = lexFragList.getKey();
			int sizeFromLexFrags = lexFragList.getValue().size();
			HashMap<TSNodeLabel,int[]> valueFirstSubTable = lexFragsTableFirstSub.get(lex);
			if (valueFirstSubTable!=null) {
				sizeFromLexFrags+= valueFirstSubTable.size();
			}
			int sizeFromSmoothing = totalNumberOfFragFromSmoothing(lex);
			int total = sizeFromLexFrags+sizeFromSmoothing;
			totalLexFrags += sizeFromLexFrags;
			totalSmoothedFrags += sizeFromSmoothing;			
			String description = lex.toString() + "\t" + sizeFromLexFrags + "\t" + sizeFromSmoothing;
			IntegerString lexSizeDescription = new IntegerString(total,description);
			tenMostProlificWords.add(lexSizeDescription);
		}
		
		for(Entry<Label,HashMap<TSNodeLabel,int[]>> lexFragList : lexFragsTableFirstSub.entrySet()) {
			Label lex = lexFragList.getKey();
			if (lexFragsTableFirstLex.get(lex)==null) {
				int sizeFromLexFrags = lexFragList.getValue().size();
				sizeFromLexFrags+= sizeFromLexFrags;
				String description = lex.toString() + "\t" + sizeFromLexFrags + "\t" + 0;
				IntegerString lexSizeDescription = new IntegerString(sizeFromLexFrags,description);
				tenMostProlificWords.add(lexSizeDescription);
			}
		}
		
		
		int totalFirstLexFragsNullary = 0;
		int totalFirstSubFragsNullary = 0;
		int totalFirstSubFragsNullaryWithoutLex = 0;
		
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
			HashMap<TSNodeLabel,int[]> wordSetFirstLex = lexFragsTableFirstLex.get(lex);
			HashMap<TSNodeLabel,int[]> wordSetFirstSub = lexFragsTableFirstSub.get(lex);
			if (wordSetFirstLex==null && wordSetFirstSub==null)
				continue;
			int sizeFromFirstLexFrags = wordSetFirstLex==null ? 0 : wordSetFirstLex.size();
			int sizeFromFirstSubFrags = wordSetFirstSub==null ? 0 : wordSetFirstSub.size();
			int sizeFromLexFrags = sizeFromFirstLexFrags+sizeFromFirstSubFrags;
			int sizeFromSmoothing = totalNumberOfFragFromSmoothing(lex);
			int total = sizeFromLexFrags+sizeFromSmoothing;
			String description = lex.toString() + "\t" + sizeFromLexFrags + "\t" + sizeFromSmoothing;
			Parameters.reportLine(total + "\t" + description);
		}
		Parameters.reportLineFlush("");
		
		int totalLexFragsWithoutOpenClassSmoothing = totalLexFrags - novelWordPosPairsFromOpenClassPos;
		int totalFirstLex = Utility.countTotalElements(lexFragsTableFirstLex);
		int totalFirstLexWithouthOpenClassSmoothing = totalFirstLex-novelWordPosPairsFromOpenClassPos;
		int totalFirstsub = Utility.countTotalElements(lexFragsTableFirstSub);
						
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
	
	
	public static FragmentExtractorFreq getDefaultFragmentExtractor(String workingPath, 
			String minSetSetting, String fragSetSetting,
			File trainTB, File testTB, File fragFile) throws Exception {
		
		debug = true;		
		checkGoldLexPosCoverage = true;

		printAllLexFragmentsToFile = false;
		printTemplateFragsToFile = false;

		addMinimalFragments = !minSetSetting.toUpperCase().equals("NO");
		smoothingFromMinSet = addMinimalFragments && !minSetSetting.toUpperCase().equals("NOSMOOTHING");
		minFragFreqForSmoothingFromMinSet = smoothingFromMinSet ? Integer.parseInt(minSetSetting) : -1; 			
		
		boolean addFragments = !fragSetSetting.toUpperCase().equals("NO");
		smoothingFromFrags = addFragments && !fragSetSetting.toUpperCase().equals("NOSMOOTHING");
		minFragFreqForSmoothingFromFrags = smoothingFromFrags ? Integer.parseInt(fragSetSetting) : -1;
			
		File fragmentsFile = addFragments ? fragFile : null;
		
		return new FragmentExtractorFreq(trainTB, testTB, fragmentsFile, workingPath);
		
	}



	public static void main(String[] args) throws Exception {
		
		String basePath = Parameters.scratchPath;
		//String workingPath = basePath + "/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4/";
		String workingPath = basePath + "/PLTSG/MB_ROARK_RightNull_H0_V1_UkM4_UkT4/";
		File trainTB = new File(workingPath + "wsj-02-21.mrg");
		File testTB = new File(workingPath + "wsj-24.mrg");					
		//File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		File fragFile = new File(workingPath + "fragments_approxFreq.txt");
		String fragExtrWorkingPath  = workingPath + "FragmentExtractor/";
				
		String[][] settings = new String[][]{
				//	minSet			frags	
				//only minset			
				//new String[]{	"1",			"No"},
				//new String[]{	"5",			"No"},					
				//new String[]{	"10",			"No"},
				//new String[]{	"100",			"No"},
				//new String[]{	"1000",			"No"},
				//new String[]{	"NoSmoothing",	"No"},
				
				//only frags
				//new String[]{	"No",			"100"},
				//new String[]{	"No",			"1000"},
				//new String[]{	"No",			"NoSmoothing"},
				
				//combination
				//new String[]{	"100",			"100"},
				//new String[]{	"1000",			"1000"},				
				//new String[]{	"10",			"NoSmoothing"},
				//new String[]{	"100",			"NoSmoothing"},
				//new String[]{	"1000",			"NoSmoothing"},
				new String[]{	"NoSmoothing",	"NoSmoothing"},
		};
		
		/*
		for(int t : new int[]{50}) {
			FragmentExtractorFreq.openClassPoSThreshold = t;
				for(String[] set : settings) {
					FragmentExtractorFreq FE = FragmentExtractorFreq.getDefaultFragmentExtractor(
							fragExtrWorkingPath, set[0], set[1], trainTB, testTB, fragFile);
					FragmentExtractorFreq.printAllLexFragmentsToFile = true;
					FE.extractFragments();		
				}
		}
		*/
			
			

	}
	
}
