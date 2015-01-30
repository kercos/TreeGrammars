package tesniere;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeSet;

import settings.Parameters;
import tsg.HeadLabeler;
import tsg.TSNode;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.Utility;
import util.file.FileUtil;

public class Conversion_04_3 {
	
	private static int sentenceIndex = 0;
	
	public static ArrayList<Box> getTesniereTreebank(
			File inputFile, HeadLabeler hl) throws Exception {
		ArrayList<Box> result = new ArrayList<Box>();
		ArrayList<TSNodeLabel> constTreebank = Wsj.getTreebankReadableAndClean(inputFile);
		sentenceIndex = 1;
		for(TSNodeLabel constTree : constTreebank) {		
			Parameters.logPrintln("----" + sentenceIndex + "----");
			constTree.fixCCPatterns(Word.comaColonAndConjunctionsSorted, Word.emptyPosSorted);
			constTree.groupConsecutiveDaughters(Word.properNounsSorted, "NML");		
			hl.markHead(constTree);
			Box b = getTesniereStructure(constTree);
			result.add(b);
			sentenceIndex++;
		}		
		return result;
	}
	
	public static Box getTesniereStructure(
			TSNodeLabel inputTree, HeadLabeler hl) throws Exception {
		inputTree.makeCleanWsj();
		inputTree.fixCCPatterns(Word.comaColonAndConjunctionsSorted, Word.emptyPosSorted);
		inputTree.groupConsecutiveDaughters(Word.properNounsSorted, "NML");		
		hl.markHead(inputTree);
		return getTesniereStructure(inputTree);
	}
	
	private static boolean isNPCoordinationFinalEndingNN(TSNodeLabel inputTree) {		
		if (!inputTree.label().startsWith("NP")) return false;
		int lastCCindex = -1;
		int di=0;
		for(TSNodeLabel d : inputTree.daughters) {
			if (d.label().equals("CC")) lastCCindex = di;
			di++;
		}
		if (lastCCindex==-1) return false;
		di--;
		if (lastCCindex!=di-1 && inputTree.daughters[di].label().startsWith("NN")) return true;
		return false;		
	}

	public static ArrayList<Box> getTreebankFromXmlFile(File treebankFile) {
		Scanner scan = FileUtil.getScanner(treebankFile);
		ArrayList<Box> result = new ArrayList<Box>();
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length()==0) continue;
			if (line.startsWith("<TDS")) break;
		}
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.length()==0) continue;
			if (line.startsWith("<TDS")) continue;
			if (line.startsWith("</TDS")) continue;
			if (line.equals("</corpus>")) break;			 
			result.add(Box.getBoxFromXmlScan(scan, line));						
		}		
		scan.close();
		return result;
	}
	
	private static Box getTesniereStructure(TSNodeLabel node) {
		//node.compressSequenceOfAdjacentNNPs();
		Box b = getBox(node, new int[]{0});
		if (b.countAllWords() != node.countLexicalNodes()) {
			Parameters.logStdOutPrintln(sentenceIndex + ": Error, words not matching\n" + node + "\n" + b) ;
			System.exit(-1);
		}		
		b.uniteVerbs();
		b = b.uniteCompoundCoordinatedVerbs();
		b.fixRBJJ();		
		b.establishCategories();
		b.checkFullWords();
		b.checkCoordinations();
		//b.spotComplexCoordinations(sentenceIndex);
		return b; 
	}
	
	/**
	 * 
	 * @param node
	 * @return -1: no juntion, 0: apposition, 1: coordination, 2: complex coordination
	 */
	public static BitSet getConjunctsIndexes(TSNodeLabel node) {
		if (node.isTerminal()) return null;		
		BitSet indexDaughtersCC = node.indexDaughtersWithLabels(Word.conjunctionsSorted);		
		if (indexDaughtersCC==null) return getAppositionConjuncts(node);
		else return getConjunctDaughtersCoordination(node, indexDaughtersCC);
	}
	
	public static String[] NPeqClass = new String[]{"NML","NP"};
	public static String[] ADJPeqClass = new String[]{"ADJP","JJ","JJP","JJR","JJS"};
	
	private static boolean coordEqClass(String l1, String l2) {
		//l1 = TSNode.removeSemanticTag(l1);
		//l2 = TSNode.removeSemanticTag(l2);
		if (l1.equals(l2)) return true;
		boolean l1SemTag = TSNode.hasSemanticTag(l1);
		boolean l2SemTag = TSNode.hasSemanticTag(l2);
		if (l1SemTag!=l2SemTag) return false;								
		//if (l1.startsWith("JJ") && l2.startsWith("JJ")) return true; //JJ, JJR, JJS
		//if (l1.startsWith("NN") && l2.startsWith("NN")) return true; //NN, NNS, NNP, NNPS
		//if (l1.startsWith("RB") && l2.startsWith("RB")) return true; //RB, RBR, RBS		
		//if (l1.startsWith("VB") && l2.startsWith("VB")) return true; //VB, VBD, VBG, VBN, VBP, VBZ
		//if (Arrays.binarySearch(NPeqClass, l1)>=0 && Arrays.binarySearch(NPeqClass, l2)>=0) return true;
		//if (Arrays.binarySearch(ADJPeqClass, l1)>=0 && Arrays.binarySearch(ADJPeqClass, l2)>=0) return true;
		if (l1.startsWith("S") && l2.startsWith("S")) {						
			if (l1.equals("SYM") || l2.equals("SYM")) return false; //(no SYM!)
			if (l1.startsWith("SBAR") && l2.startsWith("SBAR")) return true; //SBAR, SBARQ
			if (l1.startsWith("SBAR") || l2.startsWith("SBAR")) return false;			 
			return true; //S, SINV, SQ
		}
		return false;
		
	}
	
	public static BitSet getAppositionConjuncts(TSNodeLabel node) {
		String firstLabel = null;
		int sameLabelNumber = 0;
		BitSet result = new BitSet();
		int di = -1;
		for(TSNodeLabel d : node.daughters) {
			di++;
			if (d.isPreLexical()) {
				if (Word.isEmpty(d.label())) continue;				
				return null; // no apposition between full PoS
			}
			String l = d.label();
			if (firstLabel==null) {
				firstLabel = l;
				sameLabelNumber++;
				result.set(di);
				continue;
			}			
			//if (!l.equals(firstLabel)) return null; // base case
			if (!coordEqClass(firstLabel, l)) return null; // base case
			/*if (!firstLabel.equals(l)) {
				System.out.println(sentenceIndex + ": Apposition\t" + firstLabel + "\t" + l);
			}*/
			result.set(di);
			sameLabelNumber++;
		}		
		if (sameLabelNumber<2) return null; //base case			
		if (Word.isProperNoun(firstLabel) || yieldsOnlyProperNounsAndPunct(node)) {
			Parameters.logPrintln("NO Apposition: only proper names: " + node.toStringOneLevel());
			return null;
		}
		Parameters.logPrintln("Apposition: " + node.toStringOneLevel());		
		return result;
	}
	
	public static boolean hasOnlyProperNounsAndPunct(TSNodeLabel node) {
		for(TSNodeLabel d : node.daughters) {
			String l = d.label();
			if (!Word.isProperNoun(l) && !Word.isPunctuation(l)) return false;
		}		
		return true;
	}
	
	public static boolean yieldsOnlyProperNounsAndPunct(TSNodeLabel node) {
		ArrayList<TSNodeLabel> lex = node.collectLexicalItems();
		for(TSNodeLabel d : lex) {
			String l = d.parent.label();
			if (!Word.isProperNoun(l) && !Word.isPunctuation(l)) return false;
		}		
		return true;
	}
	
	public static boolean yieldsOnlyProperNouns(TSNodeLabel node) {
		ArrayList<TSNodeLabel> lex = node.collectLexicalItems();
		for(TSNodeLabel d : lex) {
			String l = d.parent.label();
			if (!Word.isProperNoun(l)) return false;
		}		
		return true;
	}

	public static BitSet getConjunctDaughtersCoordination(TSNodeLabel node, BitSet indexDaughtersCC) {
		BitSet result = new BitSet();
		int indexDaughtersWithLabelsSize = indexDaughtersCC.cardinality();
		if (indexDaughtersWithLabelsSize==1) {
			int conjunctionIndex = indexDaughtersCC.nextSetBit(0); 
			int prole = node.prole();
			if (conjunctionIndex==0 || conjunctionIndex==prole-1
					|| (conjunctionIndex==1 && Word.isPunctuation(node.daughters[0].label()))
					|| (conjunctionIndex==prole-2 && Word.isPunctuation(node.daughters[prole-1].label()))) {
				Parameters.logPrintln("NO Coordination: found only a conjunction at the beginning or end: " + node.toStringOneLevel());
				return null;
			}
		}
		String firstLabel = null;
		int di = -1;
		for(TSNodeLabel d : node.daughters) {
			di++;
			if (Word.isConjunction(d)) continue;
			if (d.isPreLexical() && Word.isEmptyLookAtNextAndPrevious(d.daughters[0], true)) continue;
			String l = d.label();
			if (firstLabel==null) {
				firstLabel = l;
				result.set(di);
				continue;
			}						
			//if (!l.equals(firstLabel)) {
			if (!coordEqClass(firstLabel, l)) {
				if (indexDaughtersWithLabelsSize==1 && node.prole()==3) {
					//Parameters.logPrintln("Simple Coordination, but the 2 conjuncts differ in labels: " + node.toStringOneLevel());
					Parameters.logPrintln("Simple Coordination, but the 2 conjuncts not in the same equivalence class: " + node.toStringOneLevel());
					result.set(di);
					return result;
				}
				return getConjunctDaughtersComplexCoordination(node, indexDaughtersCC);
			}
			/*if (!firstLabel.equals(l)) {
				System.out.println(sentenceIndex + ": Coordination\t" + firstLabel + "\t" + l);
			}*/
			result.set(di);
		}		
		Parameters.logPrintln("Simple Coordination: " + node.toStringOneLevel());		
		return result;
	}	
	
	private static BitSet dealWithOddPositionCase(TSNodeLabel node, int prole, int numberConjuntions, 
			BitSet indexDaughtersPunctCC, BitSet indexDaughtersCC) {		
		if (prole%2==1 && numberConjuntions==((prole-1)/2) && allOdd(indexDaughtersPunctCC)) {
			BitSet result = new BitSet();
			if (indexDaughtersCC.cardinality()==1) {
				int indexCC = indexDaughtersCC.nextSetBit(0);
				if (node.daughters[indexCC-1].sameLabel(node.daughters[indexCC+1])) {
					result.set(indexCC+1);
					result.set(indexCC-1);
					Parameters.logPrintln("Complex Coordination, conjunctions or puncts. in odd positions " +
							"but choosing the 2 adjacent to CC: " + node.toStringOneLevel() + " --> " + result);
					return result;
				}
			}
			for(int i=0; i<prole; i+=2) {
				if (Word.isEmpty(node.daughters[i].label())) return null;
				result.set(i);
			}
			Parameters.logPrintln("Complex Coordination, conjunctions or puncts. in odd positions: " + node.toStringOneLevel() + " --> " + result);
			return result;
		}
		return null;
	}
	
	private static BitSet getConjunctDaughtersComplexCoordination(TSNodeLabel node, BitSet indexDaughtersCC) {
		
		int prole = node.prole();
		BitSet indexDaughtersPunctCC = node.indexDaughtersWithLabels(Word.comaColonAndConjunctionsSorted);
		int numberConjuntions = indexDaughtersPunctCC.cardinality(); 
		if (numberConjuntions==1 || //one conjunction				
				(numberConjuntions==2 && indexDaughtersPunctCC.nextSetBit(0)==0)) { //two conjunctions but first in initial position
			BitSet result = new BitSet();
			int coordIndex = numberConjuntions==1 ? indexDaughtersPunctCC.nextSetBit(0) : indexDaughtersPunctCC.nextSetBit(1);
			String firstLabel = node.daughters[coordIndex-1].label.toStringWithoutSemTags();
			String secondLabel = node.daughters[coordIndex+1].label.toStringWithoutSemTags();
			boolean sameLabel = node.daughters[coordIndex-1].sameLabel(node.daughters[coordIndex+1]);
			/*if (coordIndex!=1 && sameLabel && firstLabel.startsWith("NN")) { // NN NNS NNP NNPS
				String firstLabelSingular = firstLabel.endsWith("S") ? 
						firstLabel.substring(0,firstLabel.length()-1) : firstLabel;
				int lastNNindex = coordIndex+1;
				for(int i= lastNNindex+1; i<prole; i++) {
					if (node.daughters[i].label().startsWith(firstLabelSingular)) {
						lastNNindex = i;
					}					
				}
				result.set(coordIndex-1);
				result.set(lastNNindex);
				Parameters.logPrintln("Complex Coordination, case of NN/NNS/NNP/NNPS: " + node.toStringOneLevel() + " --> " + result);
				return result;
			}*/
			boolean singleAdverbialCat = Utility.xor(Word.isAdverbTypeCat(firstLabel),Word.isAdverbTypeCat(secondLabel));
			boolean emptyWordNextToCC = Word.isEmpty(firstLabel) || Word.isEmpty(secondLabel);
			if (!singleAdverbialCat && !emptyWordNextToCC) {
				result.set(coordIndex-1);
				result.set(coordIndex+1);				
				if (sameLabel) Parameters.logPrintln("Complex Coordination, one conjunction easy case same labels: " + node.toStringOneLevel() + " --> " + result);
				else Parameters.logPrintln("Complex Coordination, one conjunction easy case but different labels: " + node.toStringOneLevel() + " --> " + result);
				return result;
			}
			else {
				 if (singleAdverbialCat) Parameters.logPrintln("Complex Coordination, found case with adverbial cat.");
				 else Parameters.logPrintln("Complex Coordination, found empty word next to CC."); 
			}
		}	
		BitSet result = dealWithOddPositionCase(node, prole, numberConjuntions, indexDaughtersPunctCC, indexDaughtersCC);
		if (result!=null) return result;
		result = new BitSet();
		Hashtable<String, int[]> labelStats = new Hashtable<String, int[]>();
		int totalFullNodes = 0;
		int maxCount = 0;
		String maxLabel = null;
		boolean doubleMax = false;
		for(TSNodeLabel d : node.daughters) {
			String label = Word.getSingularPoS(d.label());
			if (Word.isEmpty(label)) continue;
			int[] count = labelStats.get(label);
			if (count==null) {
				count = new int[]{0};
				labelStats.put(label, count);
			}
			int c = ++count[0];
			if (c>maxCount) {
				maxCount = c;
				maxLabel = label;
				doubleMax = false;
			}
			else if (c==maxCount) doubleMax = true;
			totalFullNodes++;
		}
		if (totalFullNodes==2) {
			int di=-1;
			for(TSNodeLabel d : node.daughters) {
				di++;
				if (Word.isEmpty(d.label())) continue;
				result.set(di);
			}
			Parameters.logPrintln("Complex Coordination, but only two candidates conjuncts: " + node.toStringOneLevel() + " --> " + result);
			return result;
		}
		if (maxCount==1) {			
			for(int setIndex = indexDaughtersCC.nextSetBit(0); setIndex!=-1; setIndex = indexDaughtersCC.nextSetBit(setIndex+1)) {
				for(int setIndexPlus=setIndex+1; setIndexPlus<prole; setIndexPlus++) {
					TSNodeLabel d = node.daughters[setIndexPlus];
					if (Word.isEmpty(d.label())) continue;
					result.set(setIndexPlus);
					break;
				}
				for(int setIndexMinus=setIndex-1; setIndexMinus>=0; setIndexMinus--) {
					TSNodeLabel d = node.daughters[setIndexMinus];
					if (Word.isEmpty(d.label())) continue;
					result.set(setIndexMinus);
					break;
				}
			}
			Parameters.logPrintln("Complex Coordination, more than 2 candidates all occuring once, " +
					"selecting adjacent to CCs: " + node.toStringOneLevel() + " --> " + result);
			return result;
		}
		if (doubleMax) {
			int di=-1;
			for(TSNodeLabel d : node.daughters) {
				di++;
				String label = Word.getSingularPoS(d.label());
				if (Word.isEmpty(label)) continue;
				if (labelStats.get(label)[0]==maxCount) result.set(di);				
			}
			Parameters.logPrintln("Complex Coordination, no clues what are the conjuncts, selected all having maxCount: " 
					+ node.toStringOneLevel() + " --> " + result);
			return result;
		}
		int di=0;
		for(TSNodeLabel d : node.daughters) {
			String label = Word.getSingularPoS(d.label());
			if (label.equals(maxLabel)) result.set(di);
			di++;
		}
		Parameters.logPrintln("Complex Coordination, no clues what are the conjuncts, selected conjuncts with more freq. labels: " + node.toStringOneLevel() + " --> " + result);
		return result;
	}
	
	private static boolean allOdd(BitSet a) {
		int next = 1;		
		for(int setIndex = a.nextSetBit(0); setIndex!=-1; setIndex = a.nextSetBit(setIndex+1)) {
			if (setIndex!=next) return false;
			next+=2;
		}
		return true;
	}
	
	private static boolean indexIsBeforeCC(TSNodeLabel node, int index) {
		int prole = node.prole();		
		for(int indexNext = index+1; indexNext<prole; indexNext++) {
			TSNodeLabel d = node.daughters[indexNext];
			String label = d.label();
			if (Word.isConjunction(label)) return true;
			if (Word.isEmpty(label)) continue;
			return false;
		}
		return false;
	}
	
	private static BoxStandard getBoxUniqueWord(TSNodeLabel prelexNode, int[] position) {
		if (Word.isEmptyLookAtNextAndPrevious(prelexNode.getLeftmostTerminalNode(), true)) {			
			FunctionalWord ew = getEmptyWord(prelexNode, position[0]++);
			BoxStandard box = new BoxStandard();
			box.addEmptyOrPunctWord(ew);
			box.setOriginalTSNode(prelexNode);
			return box;
		}
		ContentWord fw = getFullWord(prelexNode, position[0]++);		
		BoxStandard box = new BoxStandard(fw);
        box.setOriginalTSNode(prelexNode);        
        return box;
	}
	
	private static BoxStandard getBoxWithEmptyWords(TSNodeLabel node, ArrayList<TSNodeLabel> lex, int[] position) {
		BoxStandard box = new BoxStandard();
		box.setOriginalTSNode(node);
		for(TSNodeLabel l : lex) {
			FunctionalWord ew = getEmptyWord(l.parent, position[0]++);
			box.addEmptyOrPunctWord(ew);
		}
		return box;
	}
	
	private static boolean areCloseConjuncts(TreeSet<Box> conjuncts) {
		Iterator<Box> iter = conjuncts.iterator();
		Box previous = null;
		Box conjunct = iter.next();
		do {				
			previous = conjunct;
			conjunct = iter.next();
			int previousLastPos = previous.endPosition();
			int conjunctFirstPos = conjunct.startPosition();
			if (previousLastPos<conjunctFirstPos-2) return false;			
		} while(iter.hasNext());
		return true;
	}
	
	private static Box[] getPreviosNextConjunct(Box d, TreeSet<Box> conjuncts) {
		Box[] result = new Box[]{null,null};
		for(Box c : conjuncts) {
			int pos = Entity.leftOverlapsRight(c, d);
			if (pos==-1) {
				result[0] = c;
				continue;
			}
			result[1] = c;
			break;
		}
		return result;
	}
	
	
	private static boolean hasElementsInBetween(TreeSet<FunctionalWord> conjunctions,
			Box box, Box dependent) {
		if (conjunctions==null) return false;
		int boxPos = box.startPosition();
		int depPos = dependent.startPosition();
		int minPos, maxPos;
		if (boxPos<depPos) {
			minPos = boxPos;
			maxPos = depPos;
		}
		else {
			minPos = depPos;
			maxPos = boxPos;
		}
		for(FunctionalWord ew : conjunctions) {
			int pos = ew.position;
			if (pos > minPos && pos < maxPos) return true;
		}		
		return false;
	}
	
	
	private static Box getCloserConjunct(BoxJunction boxCoord, Box dependent) {
		Box[] previousNextConjunct = getPreviosNextConjunct(dependent, boxCoord.conjuncts);
		Box previousConjunct = previousNextConjunct[0];
		Box nextConjunct = previousNextConjunct[1];
		if  (previousConjunct==null) return nextConjunct;
		if  (nextConjunct==null) return previousConjunct;
		if (hasElementsInBetween(boxCoord.conjunctions, previousConjunct, dependent)) return nextConjunct;
		if (hasElementsInBetween(boxCoord.conjunctions, nextConjunct, dependent)) return previousConjunct;
		if (hasElementsInBetween(boxCoord.punctList, previousConjunct, dependent)) return nextConjunct;
		if (hasElementsInBetween(boxCoord.punctList, nextConjunct, dependent))  return previousConjunct;
		int wordsFromPrevious = dependent.startPosition(true, true, false) - previousConjunct.endPosition(true,true,false);
		int wordsToNext = nextConjunct.startPosition(true,true,false) - dependent.endPosition(true, true, false);
		if (wordsFromPrevious<wordsToNext) return previousConjunct;
		return nextConjunct;
	}

	private static void sortOutPendingDependent(BoxJunction boxCoord, LinkedList<Box> pendingDependents) {
		
		TreeSet<Box> conjuncts = boxCoord.conjuncts;
		if (pendingDependents.size()==1) {
			Box uniqueDep = pendingDependents.getFirst();
			if (Entity.leftOverlapsRight(uniqueDep, boxCoord.conjuncts.first())==-1 ||
						Entity.leftOverlapsRight(uniqueDep, boxCoord.conjuncts.last())==1 ) {
					
				boxCoord.addDependent(uniqueDep);			
				Parameters.logPrintln("Sorting Out Pending Dependent. " +
						"Unique pending dependent before first conjunct or after last conjunct, adding it to the coordination block " + boxCoord);
				return;
			}
			else {
				Box closerConjunct = getCloserConjunct(boxCoord, uniqueDep);
				closerConjunct.addDependent(uniqueDep);
				Parameters.logPrintln("Sorting Out Pending Dependent. " +
						"Unique pending dependent in the middle of the conjuncts. Adding it to the closer conjunct " + closerConjunct);
				return;
			}
		}		
		if (areCloseConjuncts(conjuncts)) {
			for (Box d : pendingDependents) boxCoord.addDependent(d);
			Parameters.logPrintln("Sorting Out Pending Dependent. " +
					"Close Conjuncts. " +
					"Adding all pending dependent(s) to the coordination block: " + boxCoord);
			return;
		}
		if (allDependentsAreBeforeOrAfterConjuncts(pendingDependents, conjuncts)) {
			for(Box d : pendingDependents) boxCoord.addDependent(d);
			Parameters.logPrintln("Sorting Out Pending Dependent. " +
					"All pending dependents before first conjunct or after last conjunct, adding them to the coordination block " + boxCoord);
			return;
		}		
		Iterator<Box> iter = pendingDependents.descendingIterator();
		int i=0;
		while (iter.hasNext()) {
			i++;
			Box d = iter.next();
			Box closerConjunct = getCloserConjunct(boxCoord, d);
			int wordsInBetween = wordsInBetween(d, closerConjunct);
			if (wordsInBetween==0) {
				closerConjunct.addDependent(d);
				Parameters.logPrintln("Sorting Out Pending Dependent " + i +
						". Adding it to the closer conjunct: " + closerConjunct);
				continue;
			}
			else {
				boxCoord.addDependent(d);
				Parameters.logPrintln("Sorting Out Pending Dependent. " + i +
						". Closer conjunct not adjacent. " +
						"Adding pending dependent to the coordination block: " + boxCoord);
				continue;
			}
		}
		
	}


	private static boolean allDependentsAreBeforeOrAfterConjuncts(
			LinkedList<Box> pendingDependents, TreeSet<Box> conjuncts) {
		int firstConjunctPos = conjuncts.first().startPosition();
		int lastConjunctPos = conjuncts.last().startPosition();
		for(Box d : pendingDependents) {
			int dPos = d.startPosition();
			if (dPos>firstConjunctPos && dPos<lastConjunctPos) return false;
		}		
		return true;
	}

	private static int wordsInBetween(Box d, Box closerConjunct) {		
		int pos = Entity.leftOverlapsRight(d, closerConjunct);
		if (pos==0) return 0;
		if (pos==-1) {			
			return closerConjunct.startPosition(true, true, false) - d.endPosition(true, true, false) - 1;
		}
		return d.startPosition(true, true, false) - closerConjunct.endPosition(true, true, false) - 1;
	}

	private static Box getBox(TSNodeLabel node, int[] position) {
		TSNodeLabel headDaughter = node.getHeadDaughter();
		ArrayList<TSNodeLabel> headLex = headDaughter.collectLexicalItems();		
		boolean headIsEmpty = areAllEmptyWords(headLex);
		BitSet conjunctsIndexes = getConjunctsIndexes(node);	
		
		//junction block
		if (conjunctsIndexes!=null) {
			//int conjunctsNumber = conjunctsIndexes.cardinality();
			TreeSet<FunctionalWord> emptyWords = new TreeSet<FunctionalWord>();
			TreeSet<FunctionalWord> conjunctions = new TreeSet<FunctionalWord>();
			TreeSet<Box> conjuncts = new TreeSet<Box>();
			BoxJunction boxCoord = new BoxJunction();
			boxCoord.setOriginalTSNode(node);
			int dIndex = -1;
			LinkedList<Box> pendingDependents = new LinkedList<Box>();
			boolean isNPCoordinationFinalEndingNN = isNPCoordinationFinalEndingNN(node);
			int lastDaughterIndex = node.prole()-1;
			for(TSNodeLabel d : node.daughters) {
				dIndex++;
				if (isNPCoordinationFinalEndingNN && dIndex==lastDaughterIndex) {
					Parameters.logPrintln("Found NP Coordination with final ending NN: " + node.cfgRule());
					Box box = getBoxUniqueWord(d.getLeftmostPreTerminal(), position);
					box.setEmptyWords(emptyWords);
					boxCoord.setBoxesCoordEmptyWords(conjuncts, conjunctions, null);
					for(Box dep : pendingDependents) box.addDependent(dep);
					if (boxCoord.conjuncts!=null) box.addDependent(boxCoord); 
					else {// i.e. wsj-02-21[3568]
						box.addEmptyWordPunctuationList(boxCoord.punctList);
						box.addEmptyWordPunctuationList(boxCoord.conjunctions);
					}
					return box;					
				}
				boolean yieldsOneWord = d.yieldsOneWord();
				TSNodeLabel dPreLex = yieldsOneWord ? d.getLeftmostPreTerminal() : null;
				ArrayList<TSNodeLabel> lex = yieldsOneWord ? null : d.collectLexicalItems();
				if (conjunctsIndexes.get(dIndex)) {		
					Box box = yieldsOneWord ? getBoxUniqueWord(dPreLex, position) : 
						(areAllEmptyWords(lex) ? getBoxWithEmptyWords(d, lex, position) : getBox(d, position));
					conjuncts.add(box);
					continue;
				}
				if (d.label().equals("CONJP")) {
					for(TSNodeLabel n : d.daughters) {
						conjunctions.add(getEmptyWord(n, position[0]++));						
					}
					continue;
				}								
				if (yieldsOneWord) {
					if (Word.isEmptyLookAtNextAndPrevious(dPreLex.daughters[0], true)) {
						FunctionalWord ew = getEmptyWord(dPreLex, position[0]++);
						if (Word.isConjunction(dPreLex)) conjunctions.add(ew);
						else emptyWords.add(ew);
						continue;
					}					
				}				
				else if (areAllEmptyWords(lex)) {
					for(TSNodeLabel l : lex) {
						emptyWords.add(getEmptyWord(l.parent, position[0]++));
					}
					continue;
				}						
				Box box = yieldsOneWord ? getBoxUniqueWord(dPreLex, position) : getBox(d, position);
				pendingDependents.add(box);
			}
			boxCoord.setBoxesCoordEmptyWords(conjuncts, conjunctions, emptyWords);
			if (!pendingDependents.isEmpty()) sortOutPendingDependent(boxCoord, pendingDependents);			
			return boxCoord;
		}
		
		// proper noun group
		if (yieldsOnlyProperNouns(node)) {
			ArrayList<TSNodeLabel> lex = node.collectLexicalItems();			
			TreeSet<ContentWord> properNounDaughters = new TreeSet<ContentWord>();			
			ArrayList<String> properNamesReport = new ArrayList<String>(); 
			for(TSNodeLabel d : lex) {				
				ContentWord fw = getFullWord(d.parent, position[0]++);
				properNamesReport.add(fw.getLex());
				properNounDaughters.add(fw);				
			}	
			BoxStandard box = new BoxStandard(properNounDaughters);
			box.setOriginalTSNode(node);
			Parameters.logPrintln("Grouped proper nouns in single block: " + properNamesReport);
			return box;
		}		
		
		//standard block
		Box headBox = null;
		ArrayList<Box> dependents = new ArrayList<Box>();
		ArrayList<FunctionalWord> emptyPunctWords = new ArrayList<FunctionalWord>();				
		boolean foundHead = false;
		int prole = node.prole();
		for(int i=0; i<prole; i++) {
			TSNodeLabel d = node.daughters[i];
			boolean isHeadDaughter = d.headMarked;			
			if (d.yieldsOneWord()) {		
				TSNodeLabel dPrelex = d;				
				while (!dPrelex.isPreLexical()) dPrelex = dPrelex.daughters[0];			
				if (Word.isEmptyLookAtNextAndPrevious(dPrelex.daughters[0], true)) {						
					emptyPunctWords.add(getEmptyWord(dPrelex, position[0]++));
				}
				else {					
					BoxStandard box = new BoxStandard(getFullWord(dPrelex, position[0]++));
					box.setOriginalTSNode(d);					
					if (headIsEmpty || isHeadDaughter) {							
						if (foundHead) {
							Parameters.logPrintln("Warning: found two possible heads instead of empty word. " + node);
							dependents.add(box);
						}
						else {
							foundHead = true;
							headBox = box;
						}
					}
					else dependents.add(box);
				}
			}
			else {				
				ArrayList<TSNodeLabel> lex = d.collectLexicalItems();
				if (areAllEmptyWords(lex)) {
					for(TSNodeLabel l : lex) {
						emptyPunctWords.add(getEmptyWord(l.parent, position[0]++));
					}
				}
				else {
					Box box = getBox(d, position);					
					if (headIsEmpty || isHeadDaughter) {
						if (foundHead) {
							Parameters.logPrintln("Warning: found two possible heads instead of empty word. " + node);
							dependents.add(box);
						}
						else {
							foundHead = true;
							headBox = box;
						}
					}
					else dependents.add(box);
				}
			}
		}	
		if (headBox==null) {
			Parameters.logPrintln("Warning: headBox doesn't have full word: " + node);
			headBox = new BoxStandard();
		}
		headBox.setOriginalTSNode(node);
		headBox.addDependentList(dependents);
		headBox.addEmptyWordPunctuationList(emptyPunctWords);
		return headBox;
	}

	private static boolean areAllEmptyWords(ArrayList<TSNodeLabel> lex) {		
		for(TSNodeLabel l : lex) {
			if (!Word.isEmptyLookAtNextAndPrevious(l, false)) return false;
		}
		if (lex.size()>1) Parameters.logPrintln("Found multiple consecutive empty words: " + lex);
		return true;
	}

	public static FunctionalWord getEmptyWord(TSNodeLabel prelexNode, int position) {
		String lex = prelexNode.daughters[0].label();
		return new FunctionalWord(lex, prelexNode, position); 
	}
	
	public static ContentWord getFullWord(TSNodeLabel prelexNode, int position) {
		String lex = prelexNode.daughters[0].label();
		return new ContentWord(lex, prelexNode, position); 
	}
	
	public static void countCoordinations(ArrayList<Box> corpus) {
		int totalCoord = 0;
		int totalSenteceCoord = 0;
		int totalSentences = 0;
		for(Box b : corpus) {
			totalSentences++;
			int coords = b.countCoordinations();
			if (coords>0) {
				totalSenteceCoord++;
				totalCoord += coords;
			}
		}		
		System.out.println("Total sentences: " + totalSentences);
		System.out.println("Total sentences with coordination: " + totalSenteceCoord);
		System.out.println("Total coordination: " + totalCoord);
	}

	public static void main(String[] args) throws Exception {
		HeadLabeler HL = new HeadLabeler(new File("resources/fede.rules_22_01_10"));
		//File wsj00 = new File(Wsj.WsjOriginalCleaned + "wsj-00.mrg");		
		//ArrayList<TSNodeLabel> sec00 = TSNodeLabel.getTreebank(wsj00);
		//TSNodeLabel tree48 = sec00.get(47);
		//getTesniereStructure(tree48, defaultHL);
		
		TSNodeLabel s0 = new TSNodeLabel("(TOP (S (VP (VBG creating) (NP (NP (DT another) (JJ potential) (NN obstacle)) (PP" +
				"(TO to) (NP (NP (NP (DT the) (NN government) (POS 's)) (NN sale)) (PP (IN of)" +
				"(NP (JJ sick) (NNS thrifts)))))))))");
		getTesniereStructure(s0, HL);
		
		//int size = sec00.size();		
		//System.out.println(size);
		/*TSNodeLabel selectedStructure = sec00.get(sentenceIndex);		
		System.out.println(selectedStructure.toString());
		Box tesnierStructure = getTesniereStructure(selectedStructure);
		System.out.println(tesnierStructure.toString());*/
		/*int i=1;
		for(TSNodeLabel selectedStructure : sec00) {
			System.out.println(i++);
			//System.out.println(selectedStructure.toString());
			Box tesnierStructure = getTesniereStructure(selectedStructure);
			//System.out.println(tesnierStructure.toString());
			//System.out.println(i);
		}*/
	}
	
		
}
