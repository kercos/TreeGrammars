package tesniere;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.TreeSet;

import tsg.HeadLabeler;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;
import util.IdentitySet;
import util.file.FileUtil;
import ccg.CCGInternalNode;
import ccg.CCGNode;
import ccg.CCGTerminalNode;
import ccg.CCGindexes;

public class CheckCoordWithCCG {
	
	public static ArrayList<String> getConjuncts(Box b) {
		ArrayList<String> result = new ArrayList<String>();
		TreeSet<BoxJunction> testBoxesArray = b.collectJunctionStructure();
		IdentitySet<BoxJunction> visited = new IdentitySet<BoxJunction>();
		for(BoxJunction bj : testBoxesArray) {
			if (visited.contains(bj) || bj.isConjunct) continue;
			String conj = getConjuncts(bj, visited);
			if (conj!=null) result.add(conj);
		}
		return result;
	}
	
	public static String getConjuncts(BoxJunction b, IdentitySet<BoxJunction> visited) {
		/*if (b.conjunctions!=null) {
			for(FunctionalWord f : b.conjunctions) {
				if (f.getLex().equals("&")) return null;
			}
		}*/
		String result = "(";
		boolean first = true;
		for(Box c : b.conjuncts) {			
			if (first) first = false;
			else result += ", ";
			if (c.isStandardBox()) {
				if (c.originalCat==0 && ((BoxStandard)c).ewList!=null && 
						((BoxStandard)c).ewList.first().isVerb()) {
					result += ((BoxStandard)c).ewList.first().word;
				}
				else {
					result += ((BoxStandard)c).fwList.last().word;
				}
			}
			else {
				BoxJunction cj = (BoxJunction)c;
				visited.add(cj);
				result += getConjuncts(cj, visited);			
			}
		}
		result += ")";
		return result;
	}
	
	public static ArrayList<String> getConjuncts(CCGNode n) {
		ArrayList<String> result = new ArrayList<String>();
		//ArrayList<CCGInternalNode> conjNodes = n.collectConjNodes();		
		ArrayList<CCGInternalNode> internalNodes = n.collectNonTerminalNodes();
		IdentitySet<CCGNode> visited = new IdentitySet<CCGNode>();
		for(CCGInternalNode c : internalNodes) {
			if (visited.contains(c)) continue;
			String conjuncts = getConjuncts(c, visited);
			if (conjuncts!=null) {
				result.add(conjuncts);
			}
		}
		return result;
	}
	
	
	private static String getConjuncts(CCGInternalNode c, IdentitySet<CCGNode> visited) {
		if (c.prole()!=2) return null;
		CCGNode rightD = c.daughters()[1];
		if (!rightD.isConjunct()) return null;		
		CCGNode leftD = c.daughters()[0];
		return extractConjuncts(leftD, rightD, visited);
	}

	private static String extractConjuncts(CCGNode leftD, CCGNode rightD,
			IdentitySet<CCGNode> visited) {
		String leftHead = getLexicalHead(leftD, visited);
		String rightHead = getLexicalHead(rightD, visited);
		String result = "(" + leftHead + ", " + rightHead + ")";
		return result;
	}

	private static String getLexicalHead(CCGNode n, IdentitySet<CCGNode> visited) {
		visited.add(n);
		if (n.isTerminal()) {
			CCGTerminalNode term = (CCGTerminalNode) n;
			return term.word();
		}
		else {
			CCGInternalNode intern = (CCGInternalNode) n;
			CCGNode leftD = intern.daughters()[0];
			if (intern.prole()==1) return getLexicalHead(leftD, visited);
			else {
				CCGNode rightD = intern.daughters()[1];
				if (rightD.isConjunct()) {
					return extractConjuncts(leftD, rightD, visited);
				}
				else {
					CCGNode head = leftD.isHead() ? leftD : rightD; 
					return getLexicalHead(head, visited);
				}
			}
		}
	}

	private static boolean sameCoordinationStructures(Box tdsStrcutre, CCGNode ccgStructure) {		
		return getConjuncts(tdsStrcutre).equals(getConjuncts(ccgStructure));		
	}
	
	public static void printCCGIndexes() throws Exception {
		int[] indexes = CCGindexes.getCcgIndexes();
		File wsjOriginalComplete = new File(Wsj.WsjOriginalNPBraketing + "wsj-00.mrg");		
		File outputFile = new File(Wsj.WsjOriginalNPBraketing + "wsj-00_CCGindexes.mrg");
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> treebank = Wsj.getTreebank(wsjOriginalComplete);
		ListIterator<TSNodeLabel> iter = treebank.listIterator();
		int treebankSize = treebank.size();
		TSNodeLabel next = null;
		for(int index : indexes) {
			if (index>=treebankSize) break;
			do {
				next = iter.next();				
			} while(iter.previousIndex()!=index);
			pw.println(next.toString());
		}
		pw.close();
	}
	
	public static void main1(String[] args) throws Exception {
		int[] indexes = CCGindexes.getCcgIndexes();
		//File wsjOriginalComplete = new File(Wsj.WsjOriginal + "wsj-complete.mrg");
		File wsjOriginalComplete = new File(Wsj.WsjOriginalNPBraketing + "wsj-00.mrg");
		//File ccgAutoComplete = new File("/scratch/fsangati/CORPUS/ccgbank_1_1/data/AUTO/wsj-complete.auto");
		File ccgAutoComplete = new File("/scratch/fsangati/CORPUS/ccgbank_1_1/data/AUTO/wsj-00.auto");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_04_08_10"));
		ArrayList<Box> tdsTreebank = Conversion.getTesniereTreebank(wsjOriginalComplete, HL);
		ArrayList<CCGNode> ccgTreebank = CCGNode.readCCGFile(ccgAutoComplete, FileUtil.defaultEncoding);
		ListIterator<Box> tdsTreebankIterator = tdsTreebank.listIterator();
		int index = -1;
		for(CCGNode ccgStructure : ccgTreebank) {
			int ccgIndex = indexes[++index];			
			Box tdsStrcutre = tdsTreebankIterator.next();
			while(tdsTreebankIterator.previousIndex()<ccgIndex) {
				tdsStrcutre = tdsTreebankIterator.next();
			}
			ArrayList<String> tdsConj = getConjuncts(tdsStrcutre);
			ArrayList<String> ccgConj = getConjuncts(ccgStructure);
			//if (!sameCoordinationStructures(tdsStrcutre, ccgStructure)) {
			if (!tdsConj.equals(ccgConj)) {
				System.out.println(index + ", " + ccgIndex + ": " + "Unmatched coordination: ");
				System.out.println("\tTDS: " + tdsConj);
				System.out.println("\tCCG: " + ccgConj);
				System.out.println();
			}
			else {
				//System.out.println(index + ", " + ccgIndex + ": " + "Matched coordination: " + tdsConj);
			}
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		printCCGIndexes();
	}

	
	
}
