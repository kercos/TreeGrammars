package tsg;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

import util.PrintProgressStatic;
import util.file.FileUtil;

public class HeadLabeler {
	
	public static Label jollylabel = Label.getLabel("*");
	public static final Label[] punctuation = new Label[]{Label.getLabel("''"), Label.getLabel(","), 
		Label.getLabel("-LRB-"), Label.getLabel("-RRB-"), Label.getLabel("."), Label.getLabel(":"), Label.getLabel("``")};
	Hashtable<Label,ArrayList<PriorityList>> catPriorityListTable;
	
	public HeadLabeler(InputStream is) {
		catPriorityListTable = new Hashtable<Label,ArrayList<PriorityList>>();
		Scanner scan = new Scanner(is);
		scanHeads(scan);
		scan.close();
	}
	
	public HeadLabeler(File inputFile) {
		catPriorityListTable = new Hashtable<Label,ArrayList<PriorityList>>();
		Scanner scan = FileUtil.getScanner(inputFile);
		scanHeads(scan);
		scan.close();
	}
	
	private void scanHeads(Scanner scan) {
		Label currentCat = null;
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.length()==0) continue;
			if (line.startsWith("->") || line.startsWith("=>")) {
				PriorityList pl = new PriorityList(line);
				addInCatPriorityListTable(currentCat,pl);
			}
			else currentCat = Label.getLabel(line);
		}
	}
	
	private void addInCatPriorityListTable(Label cat, PriorityList pl) {
		ArrayList<PriorityList> plCat = catPriorityListTable.get(cat);
		if (plCat==null) {
			plCat = new ArrayList<PriorityList>();
			catPriorityListTable.put(cat, plCat);
		}
		plCat.add(pl);		
	}

	protected class PriorityList {		
		boolean absoluteFirst;
		boolean fromLeft;		
		Label[] priorityList;		
		
		protected PriorityList(String line) {
			String[] fields = line.split("\\s+");
			absoluteFirst = fields[0].charAt(0)=='='; 
			fromLeft = fields[1].toLowerCase().equals("left");
			priorityList = new Label[fields.length-2];
			for(int i=0; i<priorityList.length; i++) {
				priorityList[i] = Label.getLabel(fields[i+2]);
			}
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder(absoluteFirst ? "=>" : "->");
			sb.append(" " + (fromLeft ? "left" : "right"));
			for(Label l : priorityList) sb.append(" " + l);
			return sb.toString();
		}
				
	}
	
	public void markHead(TSNodeLabel t) {
		if (t.isPreLexical()) return;
		int prole = t.prole();
		if (prole==1) t.daughters[0].headMarked = true;
		else {
			Label catFun = t.label;
			Label cat = t.label.getLabelWithoutSemTags();
			ArrayList<PriorityList> plCat = catPriorityListTable.get(catFun);
			if (plCat==null) plCat = catPriorityListTable.get(cat);
			if (plCat==null) {
				System.err.println("Found unknown cat: '" + cat + "'");
				System.exit(-1);
			}
			Label[] daughterLabels = new Label[prole];
			for(int i=0; i<prole; i++) daughterLabels[i] = t.daughters[i].label.getLabelWithoutSemTags();			
			for(PriorityList pl : plCat) {
				if (annotateHead(pl, prole, daughterLabels, t.daughters)) break;
			}
		}
		for(TSNodeLabel d : t.daughters) markHead(d);
	}
	
	private static boolean equalsJollyButNotPunctuation(Label toMatch, Label daughter) {
		if (!toMatch.equals(jollylabel)) return false;
		for(Label p : punctuation) {
			if (p.equals(daughter)) return false;
		}
		return true;
	}
	
	private static boolean annotateHead(PriorityList pl, int prole, Label[] daughterLabels, TSNodeLabel[] daughters) {	
		if (pl.absoluteFirst) {
			if (pl.fromLeft) {
				for(int i=0;i<prole;i++) {
					for(Label toMatch : pl.priorityList) {																		
						Label d = daughterLabels[i];
						if (equalsJollyButNotPunctuation(toMatch, d) || d.equals(toMatch)) {
							daughters[i].headMarked = true;								
							return true;							
						}
					}
				}
			}
			else {
				for(int i=prole-1;i>=0;i--) {
					for(Label toMatch : pl.priorityList) {																		
						Label d = daughterLabels[i];
						if (equalsJollyButNotPunctuation(toMatch, d) || d.equals(toMatch)) {
							daughters[i].headMarked = true;								
							return true;							
						}
					}
				}
			}
		}
		else {
			if (pl.fromLeft) {
				for(Label toMatch : pl.priorityList) {						
					for(int i=0;i<prole;i++) {							
						Label d = daughterLabels[i];
						if (equalsJollyButNotPunctuation(toMatch, d) || d.equals(toMatch)) {
							daughters[i].headMarked = true;								
							return true;							
						}
					}
				}
			}
			else {
				for(Label toMatch : pl.priorityList) {						
					for(int i=prole-1;i>=0;i--) {							
						Label d = daughterLabels[i];
						if (equalsJollyButNotPunctuation(toMatch, d) || d.equals(toMatch)) {
							daughters[i].headMarked = true;								
							return true;							
						}
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File fedeRuleFile = new File("/Users/fsangati/Desktop/Viewers/fede.rules_12_11_09");
		File corpusFile = new File("/Users/fsangati/Desktop/Viewers/wsj-00_NP_fedeheads_31_10_09.mrg");
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(corpusFile);
		HeadLabeler fedeLabeler = new HeadLabeler(fedeRuleFile);
		PrintProgressStatic.start("");
		for(TSNodeLabel t : treebank) {
			PrintProgressStatic.next();
			TSNodeLabel tCopy = t.clone();
			tCopy.removeHeadLabels();
			fedeLabeler.markHead(tCopy);
			if (!t.checkHeadCorrespondance(tCopy)) break;
		}
		PrintProgressStatic.end();
	}

}
