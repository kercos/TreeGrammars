package tesniere;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeSet;

import settings.Parameters;
import tsg.HeadLabeler;
import tsg.Label;
import tsg.TSNode;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.Utility;
import util.file.FileUtil;

public class Conversion {
	
	private static int sentenceIndex = 0;
	
	public static ArrayList<Box> getTesniereTreebank(File inputFile, 
			HeadLabeler hl) throws Exception {
		ArrayList<Box> result = new ArrayList<Box>();
		ArrayList<TSNodeLabel> constTreebank = Wsj.getTreebank(inputFile);
		sentenceIndex = 1;
		for(TSNodeLabel constTree : constTreebank) {		
			Parameters.logPrintln("----" + sentenceIndex + "----");
			Box b = getTesniereStructure(constTree, hl);
			result.add(b);
			sentenceIndex++;
		}		
		return result;
	}
	
	public static Box getTesniereStructure(TSNodeLabel inputTree, 
			HeadLabeler hl) throws Exception {
		
		preprocess(inputTree);
		hl.markHead(inputTree);
		Box b = getBox(inputTree, new int[]{0});
		b = postProcess(b);
		checkCorrectness(inputTree, b);						
		return b;
	}
	
	private static void preprocess(TSNodeLabel inputTree) {
		inputTree.makeCleanWsj();
		//inputTree.pruneLex(Word.quotationSorted);
		//Auxify.auxify(inputTree);
		//inputTree.raisePunctuation(Word.punctuationSorted); // not a good idea ()
		//node.compressSequenceOfAdjacentNNPs();
		checkConjunctions(inputTree);
		fixPRNCoord(inputTree);
		ArrayList<TSNodeLabel> lex = inputTree.collectLexicalItems();
		//fixDTinPOSconstructions(lex);
		fixTO(lex);
		fixCommas(lex);
		fixNotCC(lex);
		fixCapitalLettersDotInNNP(lex);
		convertNNinRB(lex);				
		inputTree.fixCCPatterns(Word.comaColonAndConjunctionsSorted, Word.emptyPosSorted);
		inputTree.groupConsecutiveDaughters(Word.properNounsSorted, "NML");
		fix_JJorNN_thatShouldBeVB(inputTree);
		reduceSubClauses(inputTree);		
	}
	
	private static Box postProcess(Box b) {
		b.uniteVerbs();
		b = b.uniteCompoundCoordinatedVerbs();		
		b.fixRBJJ();						
		b.fixAppositionPP();
		b = b.fixStartCoordination();
		b.establishCategories();
		b = b.fixCoordVerbEllipsis();
		b.checkCoordinations();
		b.checkFullWords();		
		//b.spotComplexCoordinations(sentenceIndex);		
		return b;
	}
	
	private static void checkCorrectness(TSNodeLabel node, Box b) {
		int treeSize = node.countLexicalNodes();
		int bSize = b.countAllWords();		
		if (treeSize != bSize) {
			Parameters.logStdOutPrintln(sentenceIndex + ": Error, words not matching\n" + node + "\n" + b) ;
			TSNodeLabel[] lex = new TSNodeLabel[treeSize];
			lex = node.collectLexicalItems().toArray(lex);
			Word[] boxWords = new Word[500];
			b.fillWordArray(boxWords);
			int min = Math.min(treeSize, bSize);
			for(int i=0; i<min; i++) {
				Word w = boxWords[i];
				String boxW = w==null ? null : boxWords[i].word;
				String lexW = lex[i].label(); 
				if (!lexW.equals(boxW)) {
					System.out.println("Unmatch word (box/tree): " + boxW + "," + lexW + " ("+ (i+1) + ")");
					break;
				}
			}			
			System.exit(-1);
		}
	}
	

	
	static final Label JJLabel = Label.getLabel("JJ");	
	static final Label VPLabel = Label.getLabel("VP");
	static final Label VBLabel = Label.getLabel("VB"); //base
	static final Label VBPLabel = Label.getLabel("VBP"); //present non-3rd person
	static final Label VBZLabel = Label.getLabel("VBZ"); //base 3rd-person
	static final Label VBDLabel = Label.getLabel("VBD"); //past
	static final Label VBGLabel = Label.getLabel("VBG"); //ing
	static final Label VBNLabel = Label.getLabel("VBN"); //participle
	// VP !<< /^V.*$/ < JJ)
	private static void fix_JJorNN_thatShouldBeVB(TSNodeLabel node) {
		if (node.isLexical) return;
		if (node.label.equals(VPLabel)) {
			if (!node.hasDaughterStartingWith("V")) {
				TSNodeLabel JJnode = node.getDaughterWithLabelStartingWith("JJ");
				if (JJnode!=null) {
					Parameters.logPrintln("Fixed JJ to VBD: " + node.toStringOneLevel());
					JJnode.label = VBNLabel;
				}
				else {
					TSNodeLabel NNnode = node.getDaughterWithLabelStartingWith("NN");
					if (NNnode!=null) {
						String word = NNnode.getLeftmostTerminalNode().label();
						Label newLabel = word.endsWith("ing") ?  VBGLabel :
							(word.endsWith("ed") ?VBDLabel : 
							(word.endsWith("s") ? VBZLabel : VBLabel));
						Parameters.logPrintln("Fixed NN to" +  newLabel + ": " + node.toStringOneLevel());
						NNnode.label = newLabel;
					}
				}
			}
		}
		for(TSNodeLabel d : node.daughters) {
			fix_JJorNN_thatShouldBeVB(d);
		}
	}
	
	static final Label TOLabel = Label.getLabel("to");
	static final Label TO_posLabel = Label.getLabel("TO");
	private static void fixTO(ArrayList<TSNodeLabel> lex) {
		for(TSNodeLabel n : lex) {
			if (n.label.equals(TOLabel)) {
				TSNodeLabel p = n.parent;
				if (!p.label.equals(TO_posLabel)) {
					p.label = TO_posLabel;
					Parameters.logPrintln("Fixed 'to' POS to 'TO': " + n.toStringOneLevel());
				}
			}
		}
	}
	
	
	static final Label CDLabel = Label.getLabel("CD");
	static final Label NNPLabel = Label.getLabel("NNP");
	static final Label NNLabel = Label.getLabel("NN");
	private static void fixCommas(ArrayList<TSNodeLabel> lex) {
		for(TSNodeLabel n : lex) {
			TSNodeLabel p = n.parent;
			if (p.label.equals(commaLabel)) {
				if (n.label.equals(commaLabel)) continue;
				char firstChar = n.label().charAt(0);
				Label newLabel = Character.isDigit(firstChar) ? CDLabel :
					( Character.isUpperCase(firstChar) ? NNPLabel : NNLabel );
				p.label = newLabel;	
				Parameters.logPrintln("Fixed word to" +  newLabel + ": " + p.toStringOneLevel());	
			}
		}
	}
	
	static final Label RBLabel = Label.getLabel("RB");
	static final Label SNOMLabel = Label.getLabel("S-NOM");
	static final Label NMLlabel = Label.getLabel("NML");
	private static void fixNotCC(ArrayList<TSNodeLabel> lex) {
		for(TSNodeLabel n : lex) {
			if (n.label.equals(notLabel)) {
				TSNodeLabel p = n.parent;
				if (p.label.equals(RBLabel)) {
					TSNodeLabel g = p.parent;
					if (g!=null) {
						boolean change = true;
						int conjuncts = 0;
						for(TSNodeLabel d : g.daughters) {
							Label dLabel = d.label;
							if (dLabel.equals(RBLabel) || dLabel.equals(commaLabel)) continue;
							if (dLabel.equals(NMLlabel) || dLabel.equals(NPlabel) || dLabel.equals(SNOMLabel)) {
								conjuncts++;
								continue;
							}
							change = false;
						}
						if (change && conjuncts==2) {
							Parameters.logPrintln("Fixed not to CC: " + p.toStringOneLevel());
							p.label = CClabel;
						}
					}
				}
			}			
		}
	}
	
	private static void fixCapitalLettersDotInNNP(ArrayList<TSNodeLabel> lex) {
		for(TSNodeLabel n : lex) {
			TSNodeLabel p = n.parent;
			if (p.label.equals(NNLabel)) {
				String l = n.label();
				if (l.length()<2) continue;
				char first = l.charAt(0);
				if (Character.isUpperCase(first)) {
					char second = l.charAt(1);
					if (second=='.') {
						Parameters.logPrintln("Fixed capital word with dot to NNP " + p.toStringOneLevel());
						p.label = NNPLabel;
					}
				}
			}
		}
	}
	
	
	static final Label CClabel = Label.getLabel("CC");
	private static void checkConjunctions(TSNodeLabel node) {
		if (node.isLexical) {
			TSNodeLabel prelex = node.parent;
			if (!prelex.equals(CClabel) && node.label().toLowerCase().equals("and")) {
				prelex.label = CClabel;
				Parameters.logPrintln("Fixed 'and' not labeled as CC");
			}			
		}
		else {
			for(TSNodeLabel d : node.daughters) {
				checkConjunctions(d);
			}
		}		
	}

	
	
	public static String[] advNouns = {"TODAY","TOMORROW","YESTERDAY"};
	
	static final Label POSlabel = Label.getLabel("POS");
	static final Label DTlabel = Label.getLabel("DT");
	static final Label NPlabel = Label.getLabel("NP");
	
	/**
	 * @NP <, DT <- POS
	 * @param t
	 */
	private static void fixDTinPOSconstructions(ArrayList<TSNodeLabel> lex) {
		for(TSNodeLabel l : lex) {
			TSNodeLabel pos = l.parent; 
			if (pos.label.equals(POSlabel)) {
				TSNodeLabel posParent = pos.parent;
				TSNodeLabel dt = posParent.getLeftmostTerminalNode().parent;
				if (dt.label.equals(DTlabel)) {
					if (posParent.hasDaughterWithLabel(CClabel)) continue;
					TSNodeLabel posGrandParent = posParent.parent;
					TSNodeLabel dtParent = dt.parent;
					if (posGrandParent!=null) {
						int pIndex = posGrandParent.indexOfDaughter(posParent);
						dtParent.pruneDaughter(0);
						posGrandParent.insertNewDaughter(dt, pIndex);
						Parameters.logPrintln("Fixed DT in POS construction.");
					}				
				}
			}	
		}
	}
	

	
	/**
	 * VP < (/^NP-(TMP|ADV)$/ <: /^NN(S?)$/)
	 * find NP-TMP as daughters of VP, having a unique daughter NN or NNS.
	 * Change the pos into RB (today, yesterday, tomorrow)
	 * @param t
	 */		
	private static void convertNNinRB(ArrayList<TSNodeLabel> lex) {		
		for (TSNodeLabel l : lex) {
			if (Arrays.binarySearch(advNouns, l.label().toUpperCase())>=0) {
				TSNodeLabel pos = l.parent;
				TSNodeLabel grandParent = pos.parent;
				if (grandParent==null) continue;
				TSNodeLabel grandGrandParent = grandParent.parent;
				if (grandGrandParent!=null && grandParent.prole()==1 &&
						grandParent.label().startsWith("NP") &&						
						grandGrandParent.label().startsWith("VP")) {
					pos.relabel("RB");
					Parameters.logPrintln("Modified NN to JJ: " + l);
				}
			}
		}
	}
	
	/**
	 * PRN <, \: <- \: < CC
	 * @param t
	 */
	private static void fixPRNCoord(TSNodeLabel t) {
		if (t.isLexical) return;
		if (t.label().startsWith("PRN")) {
			//int prole = t.prole();
			//if (prole>2 && t.daughters[0].label().equals(":") && 
			//		t.daughters[prole-1].label().equals(":") && t.daughters[1].label().equals("CC")) {
			boolean hasCCdaughter = false;
			for(TSNodeLabel d : t.daughters) {
				if (Word.isConjunction(d)) {
					hasCCdaughter = true;
					break;
				}
			}
			if (hasCCdaughter) {
				t.removeCurrentNode();
				Parameters.logPrintln("Modified PRN with coordination.");				
			}
		}
		for(TSNodeLabel d : t.daughters) fixPRNCoord(d);
	}
	
	
	/**
	 * Reduce patterns of the form 
	 *  - VP <2 (S < (@NP $ @VP|NP)) into VP <2 (S < (@NP $ @VP|NP))
	 */
	private static void reduceSubClauses(TSNodeLabel t) {
		if (t.isTerminal()) return;
		if (t.label().startsWith("VP")) {			
			int tProle = t.prole();
			if (tProle>1) {
				TSNodeLabel d = t.daughters[1];				
				if (d.label().startsWith("S")) {
					int dProle = d.prole();
					if (dProle>=2 ) {
						int NPVPnumber = 0;
						for(TSNodeLabel n : d.daughters) {
							if (n.label().startsWith("NP") || n.label().startsWith("VP")) NPVPnumber++;
						}
						if (NPVPnumber>=2) {											
							int newProleSize = tProle - 1 + dProle;
							TSNodeLabel[] newDaughters = new TSNodeLabel[newProleSize];
							for(int j=0; j<1; j++) newDaughters[j] = t.daughters[j];
							for(int j=0; j<dProle; j++) {
								TSNodeLabel nephew = d.daughters[j];
								d.daughters[j].parent = t;
								newDaughters[1+j] = nephew;
							}					
							for(int j=2; j<tProle; j++)  newDaughters[j-1+dProle] = t.daughters[j];
							t.daughters = newDaughters;
							Parameters.logPrintln("Modified sub-clase pattern VP < (S < (@NP $ @NP *)) into VP < (@NP $ @NP *)");
						}
					}
				}
			}
		}
		for(TSNodeLabel d : t.daughters) reduceSubClauses(d);
	}
	
	private static int isNPCoordinationFinalEndingInHead(TSNodeLabel inputTree) {	
		String l = inputTree.label();
		if (!l.startsWith("NP") && !l.startsWith("NML")) return -1;
		int lastCCindex = -1;
		int di = 0;
		for(TSNodeLabel d : inputTree.daughters) {
			if (d.label().equals("CC")) lastCCindex = di;
			di++;
		}
		if (lastCCindex==-1) return -1;
		di--;
		if(inputTree.daughters[di].label().equals("POS")) di--;		 
		if (lastCCindex!=di-1 && inputTree.daughters[di].isHeadMarked()) return di; 
		return -1;		
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
	
	
	/**
	 * 
	 * @param node
	 * @return -1: no juntion, 0: apposition, 1: coordination, 2: complex coordination
	 */
	public static BitSet getConjunctsIndexes(TSNodeLabel node) {
		if (node.isTerminal()) return null;		
		BitSet indexDaughtersCC = conjunctionIndexes(node);		
		if (indexDaughtersCC==null) return getAppositionConjuncts(node);
		else return getConjunctDaughtersCoordination(node, indexDaughtersCC);
	}
	
	public static BitSet conjunctionIndexes(TSNodeLabel node) {
		BitSet result = new BitSet();
		int prole = node.prole();
		for(int di=0; di<prole; di++) {
			TSNodeLabel d = node.daughters[di];
			if (Arrays.binarySearch(Word.conjunctionsSorted, d.label())>=0) {
				if (d.firstDaughter().label.equals(commercialAndLabel)) continue;
				result.set(di);
			}
		}
		return result.isEmpty() ? null : result;
	}
	
	public static String[] NPeqClass = new String[]{"NML","NP"};
	public static String[] ADJPeqClass = new String[]{"ADJP","JJ","JJP","JJR","JJS"};
	
	private static boolean coordEqClass(String l1, String l2) {
		//l1 = TSNode.removeSemanticTag(l1);
		//l2 = TSNode.removeSemanticTag(l2);
		
		if (l1.equals(l2)) return true;
		if (l1.equals("S-NOM") && l2.equals("NP")) return true; 
				
		
		boolean l1SemTag = TSNode.hasSemanticTag(l1);
		boolean l2SemTag = TSNode.hasSemanticTag(l2);
		if (l1SemTag!=l2SemTag) return false;
		
		if ((l1.startsWith("NP") && l2.startsWith("FRAG")) || 
				(l1.startsWith("FRAG") && l2.startsWith("NP"))) return true;
		if ((l1.startsWith("NP") && l2.startsWith("NML")) || 
				(l1.startsWith("NML") && l2.startsWith("NP"))) return true;
		if ((l1.equals("NN") && l2.startsWith("NML")) || 
				(l1.startsWith("NML") && l2.equals("NN"))) return true;
		
		
		//if ((l1.startsWith("NML") && l2.startsWith("NNP")) || 
		//		(l1.startsWith("NNP") && l2.startsWith("NML"))) return true;
									
		//NP,NP-TTL
		//NP, RRC
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

	static Label soLabel = Label.getLabel("so");
	static Label notLabel = Label.getLabel("not");
	static String NNPprefix = "NNP";
	
	public static BitSet getAppositionConjuncts(TSNodeLabel node) {
		String firstLabel = null;
		TSNodeLabel firstLabelNode = null;
		boolean firstLabelNPorNML = false;
		boolean firstLabelYieldNNP = false;
		int sameLabelNumber = 0;
		BitSet result = new BitSet();
		int di = -1;
		int lastIndexNonPunct = node.prole()-1;
		while(Word.isPunctuation(node.daughters[lastIndexNonPunct].label())) {
			lastIndexNonPunct--;
		}
		int prole = node.prole();
		for(TSNodeLabel d : node.daughters) {
			di++;			
			String l =  d.label();
			if (d.isPreLexical()) {
				//if (Word.isEmpty(d.label())) continue;				
				if (Word.isPunctuation(d.label())) continue;
				Label lexLabel = d.getLeftmostTerminalNode().label; 
				//if (lexLabel.equals(soLabel) || lexLabel.equals(notLabel)) continue;
				if (lexLabel.equals(soLabel)) continue;
				if (firstLabelNPorNML && !firstLabelYieldNNP && di==lastIndexNonPunct && 
						sameLabelNumber==1 && l.startsWith("NNP") && 
						firstLabelNode.getLeftmostTerminalNode().parent.label.equals(DTlabel)) { //486, 494
					result.set(di);
					return result;
				}
				else if (di==0 && prole==2 && l.equals("NN") && prole==2 && 
						node.daughters[1].label.equals(NMLlabel)) { //723
					result.set(0);
					result.set(1);
					return result;
				}
				return null; // no apposition between full PoS
			}												  
			if (l.startsWith("ADVP") || l.startsWith("PRN") || l.endsWith("-LOC")) continue;			
			if (l.startsWith("NP")) {
				if (l.equals("NP-TTL")) l = "NP";
				else if (d.lastDaughter().label.equals(POSlabel)) { //getLeftmostLexicon().parent
					TSNodeLabel dParent = d.parent;					
					if (firstLabel==null && d.isHeadMarked())  { //&& dParent.prole()==2 &&  
							//&& followingDaughterLaebel.startsWith("NP")) {						
						d.headMarked = false;
						int lastChildIndex = dParent.prole()-1;
						dParent.daughters[lastChildIndex].headMarked = true;																		
					}
					if (di<prole-1) {
						Label followingDaughterLabel = dParent.daughters[di+1].label;					
						if (!followingDaughterLabel.equals(commaLabel) && 
							!followingDaughterLabel.toString().endsWith("-LOC")) return null;
					}
				}
			}			
			if (firstLabel==null) {
				firstLabel = l;
				firstLabelNode = d;
				firstLabelYieldNNP = (d.isPreLexical() && d.label().startsWith(NNPprefix)) || 
					(d.hasDaughterStartingWith(NNPprefix));
				firstLabelNPorNML = firstLabel.startsWith("NP") || firstLabel.startsWith("NML");
				//firstLabelYieldNNP = d.yieldPrelexicalStartingWith(NNPprefix);
				sameLabelNumber++;
				result.set(di);
				continue;
			}			
			if (firstLabelNPorNML) {
				if (l.startsWith("PP")) continue;
				if (l.startsWith("NP") && d.prole()==1 && d.daughters[0].label().equals("PRP")) return null; // the ads themselves
			}
			if (firstLabelYieldNNP) {
				if (d.prole()==1 && d.firstDaughter().label().equals("CD")) continue;
			}
			//if (!l.equals(firstLabel)) return null; // base case
			if (di==lastIndexNonPunct && result.cardinality()>1 && 
					(firstLabel.startsWith("NP") || firstLabel.startsWith("NML")) 
					&& (l.startsWith("SBAR") || l.startsWith("VP") || l.startsWith("ADJP")) ) {
				Parameters.logPrintln("Found apposition with ending modifier.");
				break;
			}
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
		//if (firstLabel.startsWith("PP")) deal with apposition for prepositions (i.e. sentence 15 from X to Y) --> posprocessing
		if (firstLabel.startsWith("NP")) Parameters.logPrintln("Nouns Apposition: " + node.toStringOneLevel());
		else Parameters.logPrintln("Non-Noun Apposition: " + node.toStringOneLevel());		
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
	
	//static Label butLabel = Label.getLabel("but");
	
	public static BitSet getConjunctDaughtersCoordination(TSNodeLabel node, BitSet indexDaughtersCC) {
		BitSet result = new BitSet();
		boolean singleConjunction = indexDaughtersCC.cardinality()==1;
		if (singleConjunction) {
			int conjunctionIndex = indexDaughtersCC.nextSetBit(0); 
			int prole = node.prole();
			/*if (node.daughters[conjunctionIndex].label.equals(commercialAndLabel)) {
				//no coordination because of &
				return null;
			}*/
			if (conjunctionIndex==0
					|| (conjunctionIndex==1 && Word.isPunctuation(node.daughters[0].label()))) {
				Parameters.logPrintln("NO Coordination: found only a conjunction at the beginning: " + node.toStringOneLevel());
				return null;
			}
			if (conjunctionIndex==prole-1
					|| (conjunctionIndex==prole-2 && Word.isPunctuation(node.daughters[prole-1].label()))) {
				Parameters.logPrintln("NO Coordination: found only a conjunction at the end: " + node.toStringOneLevel());
				return null;
			}
		}
		String firstLabel = null;
		int di = -1;
		for(TSNodeLabel d : node.daughters) {
			di++;
			if (Word.isConjunction(d)) continue;
			if (d.isPreLexical() && Word.isEmptyLookAtNextAndPrevious(d.daughters[0], true)>0) continue;
			String l = d.label();
			if (l.startsWith("PRN")) continue;
			if (firstLabel==null) {
				firstLabel = l;
				result.set(di);
				continue;
			}						
			//if (!l.equals(firstLabel)) {
			if (!coordEqClass(firstLabel, l)) {
				if (singleConjunction && node.prole()==3) {
					//Parameters.logPrintln("Simple Coordination, but the 2 conjuncts differ in labels: " + node.toStringOneLevel());
					Parameters.logPrintln("Simple Coordination, but the 2 conjuncts not in the same equivalence class: " + node.toStringOneLevel());
					result.set(di);
					return result;
					/*Label conjLabel = node.daughters[indexDaughtersCC.nextSetBit(0)].getLeftmostLexicon().label;
					if (conjLabel.equals(butLabel)) {
						Parameters.logPrintln("No coordination, labels not in equivance class and conjunction is 'but': " + node.toStringOneLevel());
						return null;
					}*/
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
	
	/*private static boolean indexIsBeforeCC(TSNodeLabel node, int index) {
		int prole = node.prole();		
		for(int indexNext = index+1; indexNext<prole; indexNext++) {
			TSNodeLabel d = node.daughters[indexNext];
			String label = d.label();
			if (Word.isConjunction(label)) return true;
			if (Word.isEmpty(label)) continue;
			return false;
		}
		return false;
	}*/
	
	private static BoxStandard getBoxUniqueWord(TSNodeLabel node, TSNodeLabel prelexNode, int[] position) {
		if (Word.isEmptyLookAtNextAndPrevious(prelexNode.getLeftmostTerminalNode(), true)>0) {			
			FunctionalWord ew = getEmptyWord(prelexNode, position[0]++, -1);
			BoxStandard box = new BoxStandard();
			box.addEmptyOrPunctWord(ew);
			box.setOriginalTSNode(node);
			return box;
		}
		ContentWord fw = getFullWord(prelexNode, position[0]++);		
		BoxStandard box = new BoxStandard(fw);
        box.setOriginalTSNode(node);        
        return box;
	}
	
	private static BoxStandard getBoxWithEmptyWords(TSNodeLabel node, ArrayList<TSNodeLabel> lex, int[] position) {
		BoxStandard box = new BoxStandard();
		box.setOriginalTSNode(node);
		for(TSNodeLabel l : lex) {
			FunctionalWord ew = getEmptyWord(l.parent, position[0]++, -1);
			box.addEmptyOrPunctWord(ew);
		}
		return box;
	}
	
	/*public static final String[] monthsNameSorted = new String[]{
		"April", "August", "December", "February", "January", "July", "June", 
			"March", "May", "November", "October", "September"};*/
	
	static final Label andLabel = Label.getLabel("and");
	static final Label orLabel = Label.getLabel("or");
	static final Label commaLabel = Label.getLabel(",");
	//static final Label NPTTL_Label = Label.getLabel("NP-TTL");
	static Label commercialAndLabel = Label.getLabel("&");
	static String[] applicableNodes = new String[]{"ADJP","NAC","NML","NP","NX"};
	//public static final String[] notAllowedPunctuationInBlock = new String[]{",", ":"};
	
	private static Box yieldOnlyProperNounsAndEmptyWords(TSNodeLabel node, int[] position) {
		Label label = node.label;
		/*if (label.equals(NPTTL_Label)) {
			ArrayList<TSNodeLabel> prelex = node.collectPreLexicalItems();
			BoxStandard box = new BoxStandard();
			int pos = position[0];
			for (TSNodeLabel p : prelex) {				
				String pLabel = p.label();				
				if (Word.isEmpty(pLabel)) {					
					FunctionalWord ew = getEmptyWord(p, pos++, -1);
					box.addEmptyOrPunctWord(ew); // no coordination
				}
				else {
					ContentWord fw = getFullWord(p, pos++);		
					box.addFullWord(fw);
				}
			}
			box.setOriginalTSNode(node);
			position[0] = pos;
			return box;
		}*/
		String l = label.toStringWithoutSemTags(); 		
		if (Arrays.binarySearch(applicableNodes, l)>=0) {
			if (node.yieldNonLexicalNodeStartingWith("PP")) return null;
			boolean hasAllDaughtersStartingWithNP = true;
			for(TSNodeLabel d : node.daughters) {
				if (d.label.equals(commaLabel)) continue;
				if (!d.label().startsWith("NP")) {
					hasAllDaughtersStartingWithNP = false;
					break;
				}
			}
			if (hasAllDaughtersStartingWithNP) return null;
			BoxStandard box = new BoxStandard();
			int pos = position[0];
			ArrayList<TSNodeLabel> prelex = node.collectPreLexicalItems();
			boolean hasCommercialAnd = false;
			for (TSNodeLabel p : prelex) {
				if ( p.label.equals(POSlabel) ) return null;
				if (p.label.equals(CClabel)) {
					Label lex = p.firstDaughter().label;
					if (lex.equals(andLabel) || lex.equals(orLabel)) return null;
					if (lex.equals(commercialAndLabel)) hasCommercialAnd = true;
				}
			}
			boolean hasNNP = false;
			for (TSNodeLabel p : prelex) {				
				String pLabel = p.label();				
				if (Word.isEmpty(pLabel)) {
					if (!hasCommercialAnd && p.label.equals(commaLabel)) return null;
					FunctionalWord ew = getEmptyWord(p, pos++, -1);
					box.addEmptyOrPunctWord(ew); // no coordination
				}
				else if (pLabel.startsWith("NNP")) {
					hasNNP = true;
					ContentWord fw = getFullWord(p, pos++);		
					box.addFullWord(fw);
				}
				else return null;
			}
			if (!hasNNP) return null;
			box.setOriginalTSNode(node);
			position[0] = pos;
			return box;
		}
		return null;
	}

	
	private static boolean areCloseConjuncts(TreeSet<Box> conjuncts) {
		Iterator<Box> iter = conjuncts.iterator();
		Box previous = null;
		Box conjunct = iter.next();
		while(iter.hasNext()) {				
			previous = conjunct;
			conjunct = iter.next();
			int previousLastPos = previous.endPosition();
			int conjunctFirstPos = conjunct.startPosition();
			if (previousLastPos<conjunctFirstPos-2) return false;			
		};
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
	
	
	/*private static boolean hasElementsInBetween(TreeSet<FunctionalWord> conjunctions,
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
	}*/
	
	
	private static Box getCloserConjunct(BoxJunction boxCoord, Box dependent) {
		Box[] previousNextConjunct = getPreviosNextConjunct(dependent, boxCoord.conjuncts);		
		Box previousConjunct = previousNextConjunct[0];
		Box nextConjunct = previousNextConjunct[1];		
		if  (nextConjunct==null) return previousConjunct;
		if  (previousConjunct==null) return nextConjunct;		
		
		if (previousConjunct.originalTSNodeLabel.toString().startsWith("NP") &&
				dependent.originalTSNodeLabel.toString().startsWith("PP")) return previousConjunct; 
		
		//TreeSet<FunctionalWord> conjPunt = new TreeSet<FunctionalWord>(boxCoord.conjunctions);
		//conjPunt.addAll(boxCoord.punctList);
		//if (!hasElementsInBetween(conjPunt, previousConjunct, dependent)) return previousConjunct;
		//if (!hasElementsInBetween(conjPunt, nextConjunct, dependent)) return nextConjunct;
		//if (hasElementsInBetween(boxCoord.punctList, nextConjunct, dependent))  return previousConjunct;
		//if (hasElementsInBetween(boxCoord.punctList, previousConjunct, dependent)) return nextConjunct;		
		int wordsFromPrevious = dependent.startPosition(true, true, true) - previousConjunct.endPosition(true,true,true);
		int wordsToNext = nextConjunct.startPosition(true,true,true) - dependent.endPosition(true, true, true);
		if (wordsFromPrevious<=wordsToNext) return previousConjunct;
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
	
	public static final String[] conjunctionsPunctSorted = new String[]{",", ":"};
	
	private static BitSet getPunctConjunctionsIndexes(TSNodeLabel node, BitSet conjunctsIndexes) {
		BitSet result = new BitSet();
		int prole = node.prole();
		int firstConjuctIndex = conjunctsIndexes.nextSetBit(0);
		int previousConjunctionIndex = -5;
		for(int i=0; i<prole; i++) {
			TSNodeLabel d = node.daughters[i];
			if (Word.isConjunction(d)) {
				result.set(i);
				previousConjunctionIndex = i;
			}
			else if (previousConjunctionIndex!=i-1 && 
					Arrays.binarySearch(conjunctionsPunctSorted, d.label())>=0 &&
					i > firstConjuctIndex && conjunctsIndexes.nextSetBit(i)!=-1) {
				
				TSNodeLabel nextD = i+1<prole ? node.daughters[i+1] : null;
				if (nextD==null || !Word.isConjunction(nextD)) {
					result.set(i);
				}
			}
		}
		return result;
	}

	private static Box getBox(TSNodeLabel node, int[] position) {
		
		BitSet conjunctsIndexes = getConjunctsIndexes(node);
		//junction block
		if (conjunctsIndexes!=null) {
			//int conjunctsNumber = conjunctsIndexes.cardinality();
			TreeSet<FunctionalWord> emptyWords = new TreeSet<FunctionalWord>();
			TreeSet<FunctionalWord> conjunctions = new TreeSet<FunctionalWord>();
			TreeSet<Box> conjuncts = new TreeSet<Box>();
			BitSet punctConjunctionsIndexes = getPunctConjunctionsIndexes(node, conjunctsIndexes);
			BoxJunction boxCoord = new BoxJunction();
			boxCoord.setOriginalTSNode(node);
			int dIndex = -1;
			LinkedList<Box> pendingDependents = new LinkedList<Box>();
			int coordWithHeadIndex = isNPCoordinationFinalEndingInHead(node);
			Box headBox = null;
			//int lastDaughterIndex = node.prole()-1;
			for(TSNodeLabel d : node.daughters) {
				dIndex++;
				boolean yieldsOneWord = d.yieldsOneWord();
				if (coordWithHeadIndex==dIndex) {
					Parameters.logPrintln("Found coordination with head marked: " + node.cfgRule());
					if (yieldsOneWord) headBox = getBoxUniqueWord(d, d.getLeftmostPreTerminal(), position);
					else headBox = getBox(d, position);
					continue;
				}				
				TSNodeLabel dPreLex = yieldsOneWord ? d.getLeftmostPreTerminal() : null;
				ArrayList<TSNodeLabel> lex = yieldsOneWord ? null : d.collectLexicalItems();
				if (conjunctsIndexes.get(dIndex)) {		
					Box box = yieldsOneWord ? getBoxUniqueWord(d, dPreLex, position) : 
						(areAllEmptyWords(lex) ? getBoxWithEmptyWords(d, lex, position) : getBox(d, position));
					conjuncts.add(box);
					continue;
				}
				if (punctConjunctionsIndexes.get(dIndex)) {
					for(TSNodeLabel c : d.daughters) {
						if (c.isLexical) c = c.parent;
						conjunctions.add(getEmptyWord(c, position[0]++, 2));						
					}
					continue;
				}								
				if (yieldsOneWord) {
					if (Word.isEmptyLookAtNextAndPrevious(dPreLex.daughters[0], true)>0) {
						FunctionalWord ew = getEmptyWord(dPreLex, position[0]++, -1);
						emptyWords.add(ew);
						continue;
					}
				}				
				else if (areAllEmptyWords(lex)) {
					for(TSNodeLabel l : lex) {
						emptyWords.add(getEmptyWord(l.parent, position[0]++, -1));
					}
					continue;
				}						
				Box box = yieldsOneWord ? getBoxUniqueWord(d, dPreLex, position) : getBox(d, position);
				pendingDependents.add(box);
			}
			if (headBox!=null) {
				headBox.addEmptyWordPunctuationList(emptyWords);
				boxCoord.setBoxesCoordEmptyWords(conjuncts, conjunctions, null);
				for(Box dep : pendingDependents) headBox.addDependent(dep);
				if (boxCoord.conjuncts!=null) headBox.addDependent(boxCoord); 
				else {// i.e. wsj-02-21[3568]
					headBox.addEmptyWordPunctuationList(boxCoord.punctList);
					headBox.addEmptyWordPunctuationList(boxCoord.conjunctions);
				}
				return headBox;
			}
			boxCoord.setBoxesCoordEmptyWords(conjuncts, conjunctions, emptyWords);
			if (!pendingDependents.isEmpty()) sortOutPendingDependent(boxCoord, pendingDependents);			
			return boxCoord;
		}
		
		Box b = yieldOnlyProperNounsAndEmptyWords(node, position);
		if (b!=null) return b;
		
		//standard block		
		Box headBox = null;
		TSNodeLabel headDaughter = node.getHeadDaughter();
		ArrayList<TSNodeLabel> headLex = headDaughter.collectLexicalItems();
		boolean headIsEmpty = areAllEmptyWords(headLex);
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
				if (Word.isEmptyLookAtNextAndPrevious(dPrelex.daughters[0], true)>0) {						
					emptyPunctWords.add(getEmptyWord(dPrelex, position[0]++, -1));
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
						emptyPunctWords.add(getEmptyWord(l.parent, position[0]++, -1));
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
		int size = lex.size();
		if (size>3) return false;
		boolean allEmpty = true;
		for(TSNodeLabel l : lex) {
			if (!Word.isEmpty(l, false)) {
				allEmpty = false;
				break;
			}
		}
		if (!allEmpty) {
			int lex1, lex2, lex3;
			switch (size) {
			case 1:
				if (Word.isEmptyLookAtNextAndPrevious(lex.get(0), false)==0) return false;
				break;
			case 2:
				lex1 = Word.isEmptyLookAtNextAndPrevious(lex.get(0), false);
				lex2 = Word.isEmptyLookAtNextAndPrevious(lex.get(1), false);
				if (lex1==2 || lex2==3 || (lex1==1 && lex2==2)) break;
				return false;
			case 3:
				lex1 = Word.isEmptyLookAtNextAndPrevious(lex.get(0), false);
				lex2 = Word.isEmptyLookAtNextAndPrevious(lex.get(1), false);
				lex3 = Word.isEmptyLookAtNextAndPrevious(lex.get(2), false);
				if (lex2==3 || (lex1>1 && lex3>0)) break;
				return false;
			}
		}		
		if (lex.size()>1) Parameters.logPrintln("Found multiple consecutive empty words: " + lex);
		return true;
	}

	public static FunctionalWord getEmptyWord(TSNodeLabel prelexNode, int position, int wordType) {
		String lex = prelexNode.daughters[0].label();
		return new FunctionalWord(lex, prelexNode, position, wordType); 
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
		HeadLabeler HL = new HeadLabeler(new File("resources/fede.rules_04_08_10"));
		
		/*
		TSNodeLabel t= new TSNodeLabel("(S (NP-SBJ (DT The) (NNP Lorillard) (NN spokeswoman) ) (VP (VBD said) (SBAR (-NONE- 0) (S (NP-SBJ-1 (NN asbestos) ) (VP (VBD was) (VP (VP (VBN used) (NP (-NONE- *-1) ) (PP-TMP (IN in) (NP (DT the) (JJ early) (NN 1950s) ))) (CC and) (VP (VBN replaced) (NP (-NONE- *-1) ) (PP-TMP (IN in) (NP (CD 1956) )))))))) (. .) )");
		Box b = Conversion.getTesniereStructure(t, HL);
		TSNodeLabel tTransformed = b.toPhraseStructure();
		System.out.println(tTransformed.toStringQtree());
		*/
		
						
		String[] fileNames = new String[]{"wsj-00","wsj-01","wsj-02-21","wsj-22","wsj-23","wsj-24"};
		for(String fn : fileNames) {
			System.out.println("Converting: " + fn);
			File inputFile = 
				new File("/scratch/fsangati/CORPUS/WSJ/CONSTITUENCY/ORIGINAL_READABLE_NP_BRACKETING/" + fn + ".mrg");
			File outputFile = new File("/Users/fedja/Work/JointProjects/Yoav/Conversion/" + fn + "_TDS_PS_v01.mrg");
			ArrayList<TSNodeLabel> treebankOriginal = TSNodeLabel.getTreebank(inputFile);
			ArrayList<Box> treebank = Conversion.getTesniereTreebank(inputFile, HL);
			PrintWriter pw = new PrintWriter(outputFile);
			int i = 0;
			for(Box b : treebank) {
				TSNodeLabel tTransformed = b.toPhraseStructure();
				boolean sameWords = b.countAllWords() == tTransformed.countLexicalNodes();
				if (!sameWords) {
					System.err.println("Error in conversion: " + b.countAllWords() + " " + tTransformed.countLexicalNodes());
					System.err.println(treebankOriginal);
					break;
				}
				pw.println(tTransformed);
				i++;
			}
			pw.close();
			System.out.println("Successfully converted " + i + " structures");
		}
		
	}
	
	public static void main1(String[] args) throws Exception {
		HeadLabeler HL = new HeadLabeler(new File("resources/fede.rules_04_08_10"));
		File wsj00 = new File(Wsj.WsjOriginalCleaned + "wsj-00.mrg");		
		//ArrayList<TSNodeLabel> sec00 = TSNodeLabel.getTreebank(wsj00);
		Conversion.getTesniereTreebank(wsj00, HL);
		//TSNodeLabel tree48 = sec00.get(47);
		//getTesniereStructure(tree48, defaultHL);
		
		//File inputFile = new File("/Users/fedja/Desktop/sentences.mrg");
		//getTesniereTreebank(inputFile, HL);
		
		
		//TSNodeLabel s0 = new TSNodeLabel("(SINV (`` ``) (S-TPC (ADVP (RB Sometimes)) (, ,) (PP (IN with) (NP (NP (DT the) (NN release)) (PP (IN of) (NP (NN stress))))) (, ,) (NP-SBJ (PRP you)) (VP (VBP hear) (S (NP-SBJ (`` `) (UH oohs) (: ') (CC and) (`` `) (UH ahs) (: ')) (VP (VBG coming) (PP-CLR (IN out) (PP (IN of) (NP (DT the) (NN room)))))))) (, ,) ('' '') (VP (VBZ explains)) (NP-SBJ (NP (NNP Morgan) (NNP Banks)) (, ,) (NP (NP (DT the) (NN agency) (POS 's)) (NN health) (NN specialist))) (. .))");
		//getTesniereStructure(s0, HL);
		
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
