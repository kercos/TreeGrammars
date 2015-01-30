package tesniere;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import tsg.HeadLabeler;
import tsg.corpora.Wsj;

public abstract class ProbModelExpander extends ProbModel {	
	
	public ProbModelExpander(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);
	}
	
	public ProbModelExpander() {		
	}

	public void train() {
		int sentenceNumber = 1;
		for(Box b : trainingTreebank) {
			extractEvents(b);
			sentenceNumber++;
		}
	}
	
	private TreeSet<BoxStandard> getStandardDep(Box gov) {
		if (gov.dependents==null) return null;
		TreeSet<BoxStandard> depSet = new TreeSet<BoxStandard>();
		for(Box d : gov.dependents) {					
			while(d.isJunctionBlock()) {
				d = ((BoxJunction)d).conjuncts.first();
			}
			depSet.add((BoxStandard)d);			
		}
		return depSet;
	}
	
	private TreeSet<BoxStandard> addBoxes(TreeSet<BoxStandard> setA, TreeSet<BoxStandard> setB, TreeSet<BoxStandard> setC) {		 
		if (setA==null && setB==null && setC==null) return null;
		TreeSet<BoxStandard> result = new TreeSet<BoxStandard>();
		if (setA!=null) result.addAll(setA);
		if (setB!=null) result.addAll(setB);
		if (setC!=null) result.addAll(setC);
		return result;
	}
	
	private void extractEvents(Box b) {
		addTopDependencyEvents(b);
		ArrayList<BoxStandard> allStdBoxInB = new ArrayList<BoxStandard>();		
		b.addStandardBoxesInCollection(allStdBoxInB);
		for(BoxStandard bs : allStdBoxInB) {			
			TreeSet<BoxStandard> standardDepBs = getStandardDep(bs);
			if (bs.isConjunct) {
				BoxJunction parent = (BoxJunction) bs.parent;				
				TreeSet<BoxStandard> standardDepJun = getStandardDep(parent);
				Box firstConjunct =  parent.conjuncts.first();
				while(firstConjunct.isJunctionBlock()) {
					firstConjunct = ((BoxJunction)firstConjunct).conjuncts.first();
				}
				BoxStandard firstConjunctStd = (BoxStandard)firstConjunct;
				if (firstConjunctStd==bs) {					 					
					TreeSet<BoxStandard> standardDepOuter =  collectAllStdDepOuterJunction(bs, parent);
					TreeSet<BoxStandard> standardDepTot = addBoxes(standardDepBs, standardDepJun, standardDepOuter);
					addDependencyEvents(bs, standardDepTot);
				}
				else {
					BoxStandard previousConjunct = getPreviousConjunct(parent, bs);
					addJunctionEvents(parent, previousConjunct, bs, standardDepJun);
				}
			}			
			else addDependencyEvents(bs, standardDepBs);
		}				
	}	

	private TreeSet<BoxStandard> collectAllStdDepOuterJunction(BoxStandard bs, BoxJunction parent) {
		BoxJunction oldParent = parent;
		TreeSet<BoxStandard> standardDepTot = new TreeSet<BoxStandard>(); 
		while(parent.parent.isJunctionBlock()) {						
			parent = (BoxJunction) parent.parent;
			Box firstConjunct = parent.conjuncts.first();
			if (firstConjunct!=oldParent) {
				TreeSet<BoxStandard> standardDepJun = getStandardDep(parent);
				BoxStandard previousConjunct = getPreviousConjunct(parent, oldParent);
				addJunctionEvents(parent, previousConjunct, bs, standardDepJun);
				break;
			}
			oldParent = parent;
			TreeSet<BoxStandard> standardDepJun = getStandardDep(parent);
			if (standardDepJun!=null) standardDepTot.addAll(standardDepJun);
		}
		if (standardDepTot.isEmpty()) return null;
		return standardDepTot;
	}
	
	private boolean sameSide(Box gov, BoxStandard b1, BoxStandard b2) {
		return Entity.leftOverlapsRight(b1, gov)==Entity.leftOverlapsRight(b2, gov);
	}
	
	public static String toStringShort(BoxStandard b) {
		StringBuilder result = new StringBuilder();	
		result.append("{");
		if (b.fwList!=null) result.append(" Full Words: " + b.fwList);
		//result.append(" Node " + originalTSNodeLabel.toString());
		if (b.ewList!=null) result.append(" Empty Words: " + b.ewList);
		//if (this.punctList!=null) result.append(" Punctuation Words: " + punctList);
		//if (this.dependents!=null) result.append(" Dependents: " + dependents);
		//result.append(" In/Out Cat: " + originalCat + "/" + derivedCat);
		result.append("}");
		return result.toString();
	}
	
	private void addTopDependencyEvents(Box b) {
		BoxStandard topStd = null;
		if (b.isJunctionBlock()) {
			while(b.isJunctionBlock()) {
				b = ((BoxJunction)b).conjuncts.first();
			}
			topStd = (BoxStandard)b;
		}
		topStd = (BoxStandard)b;
		TreeSet<FunctionalWord> totalEmptyWords = gatherEmptyWords(topStd);
		System.out.println("Dependency relation: " + 
				"\n\t" + "*TOP*" + " --> " + topStd.fwList.toString() +
				"\n\tEmpty words set of dependent: " + (totalEmptyWords==null ? "*NULL*" : totalEmptyWords.toString()));
	}
	
	protected void addDependencyEvents(BoxStandard gov, TreeSet<BoxStandard> deps) {
		if (deps==null) System.out.println("Dependency relation: " + gov.fwList + " --> *NULL*");		
		else {
			BoxStandard grandParent = getGoverningNodeStd(gov);
			BoxStandard leftSisterSameSide = null;						
			for(BoxStandard d : deps) {						
				int direction = Entity.leftOverlapsRight(d, gov);
				boolean sameSideOfPreviousDep = leftSisterSameSide!=null && sameSide(gov, leftSisterSameSide, d);
				TreeSet<FunctionalWord> totalEmptyWords = gatherEmptyWords(d);
				System.out.println("Dependency relation: " + 
					"\n\t" + toStringShort(gov) + " --> " + d.fwList.toString() + 
					"\n\tEmpty words set of dependent: " + (totalEmptyWords==null ? "*NULL*" : totalEmptyWords.toString()) +
					"\n\tDirection: " + direction +											
					"\n\tLeft Sister same side: " + ( (leftSisterSameSide==null || !sameSideOfPreviousDep)  ? "*NULL*" : toStringShort(leftSisterSameSide)) +					
					"\n\tGrand Parent: " + (grandParent==null ? "*NULL*" : toStringShort(grandParent))
				);
				leftSisterSameSide = d;
			}
		}		
	}
	
	private static String boxSetToString(TreeSet<BoxStandard> sharedDep) {
		if (sharedDep==null) return null;
		Iterator<BoxStandard> iter = sharedDep.iterator();
		String result = toStringShort(iter.next());
		while(iter.hasNext()) {
			result += toStringShort(iter.next());
		}
		return result;
	}
	
	public static BoxStandard getGoverningNodeStd(Box b) {
		Box gov = b.governingParent();
		if (gov==null) return null;
		while(gov.isJunctionBlock()) {
			gov = ((BoxJunction)gov).conjuncts.first();
		}
		return (BoxStandard) gov;
	}
	
	protected void addJunctionEvents(BoxJunction junctionBox, BoxStandard previousConjunct, 
			BoxStandard newConjunct, TreeSet<BoxStandard> sharedDep) {				
		BoxStandard governor = getGoverningNodeStd(junctionBox);		
		TreeSet<FunctionalWord> conjunctionsSet = getConjunctionSet(junctionBox, previousConjunct, newConjunct);
		TreeSet<FunctionalWord> sharedEmptyWords = gatherEmptyWords(newConjunct);
		System.out.println("Junction relation: " + 
				"\n\t" + toStringShort(previousConjunct) + " --> " + toStringShort(newConjunct) +
				"\n\tGovernor: " + (governor==null ? "*NULL*" : toStringShort(governor)) +
				"\n\tConjunctions in between: " + (conjunctionsSet==null ? "*NULL*" : conjunctionsSet.toString()) +
				"\n\tShared empty words: " + (sharedEmptyWords==null ? "*NULL*" : sharedEmptyWords.toString()) +
				"\n\tShared dependencies: " + boxSetToString(sharedDep)
		);		
	}
	
	private static TreeSet<FunctionalWord> gatherEmptyWords(Box b) {
		TreeSet<FunctionalWord> emptyWordsSet = new TreeSet<FunctionalWord>();
		if (b.ewList!=null) emptyWordsSet.addAll(b.ewList);		
		if (b.isConjunct) {
			Box parent = b.parent;
			do {			
				BoxJunction parentJun = (BoxJunction) parent;
				if (parentJun.ewList!=null)	emptyWordsSet.addAll(parentJun.ewList);	
				parent = parent.parent;
			} while(parent.isJunctionBlock());
		}
		if (emptyWordsSet.isEmpty()) return null;
		return emptyWordsSet;
	}
	
	
	private TreeSet<FunctionalWord> getConjunctionSet(BoxJunction junctionBox,
			BoxStandard previousConjunct, BoxStandard newConjunct) {
		TreeSet<FunctionalWord> result = new TreeSet<FunctionalWord>();
		ContentWord previousFW = previousConjunct.fwList.first();
		ContentWord currentFW = newConjunct.fwList.first();
		for(FunctionalWord conj : junctionBox.conjunctions) {
			if (conj.compareTo(currentFW)>0) break;
			if (conj.compareTo(previousFW)>0) result.add(conj);
		}
		if (result.isEmpty()) return null;
		return result;
	}

	private BoxStandard getPreviousConjunct(BoxJunction junctionBox, Box currentConjunct) {
		Iterator<Box> conjunctionsIter = junctionBox.conjuncts.iterator();
		Box conjunct = null, previous = null;
		while(conjunctionsIter.hasNext()) {
			previous = conjunct;
			conjunct = conjunctionsIter.next();			
			if (conjunct!=currentConjunct) continue;
			while(previous.isJunctionBlock()) {
				previous = ((BoxJunction)previous).conjuncts.first();
			}
			return (BoxStandard)previous;
		}
		return null;
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
	}

}
