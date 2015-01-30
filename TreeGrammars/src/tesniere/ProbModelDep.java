package tesniere;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import tsg.HeadLabeler;
import tsg.corpora.Wsj;

public abstract class ProbModelDep extends ProbModel {	
	
	public ProbModelDep(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);
	}
	
	public ProbModelDep() {		
	}

	public void train() {
		int sentenceNumber = 1;
		for(Box b : trainingTreebank) {
			extractEvents(b);
			sentenceNumber++;
		}
	}
	
	private TreeSet<BoxStandard> getConjunctsStd(BoxJunction junction) {
		if (junction==null) return null;
		TreeSet<BoxStandard> result = new TreeSet<BoxStandard>();
		for(Box conj : junction.conjuncts) {
			if (conj.isJunctionBlock()) result.addAll(getConjunctsStd((BoxJunction)conj));
			else result.add((BoxStandard)conj);
		}
		return result;
	}
	
	private TreeSet<BoxStandard> getSameSideSistersStd(BoxStandard gov, BoxStandard daughter) {		
		Box previousBox = null;		
		for(Box d : gov.dependents) {
			boolean found = false;
			if (d==daughter) found = true;			 
			if (d.isJunctionBlock()) {
				BoxJunction dJun = (BoxJunction) d;
				if (containsInConjunctsRecursive(dJun,daughter)) found = true;
			}
			if (found) {
				if (previousBox==null) return null;				
				if (previousBox.isJunctionBlock()) {					
					TreeSet<BoxStandard> previousSisters = getConjunctsStd((BoxJunction)previousBox);
					if (!sameSide(gov, previousSisters.first(), daughter)) return null;
					return previousSisters;
				}
				BoxStandard previousSister = (BoxStandard)previousBox;
				if (!sameSide(gov, previousSister, daughter)) return null;
				TreeSet<BoxStandard> result = new TreeSet<BoxStandard>();
				result.add(previousSister);
				return result;
			}			
		}
		return null;
	}
	
	private boolean containsInConjunctsRecursive(BoxJunction dJun,BoxStandard toBeFound) {
		for(Box c : dJun.conjuncts) {
			if (c==toBeFound) return true;
			if (c.isJunctionBlock()) {
				BoxJunction cJun = (BoxJunction)c;
				if (containsInConjunctsRecursive(cJun, toBeFound)) return true;
			}
		}
		return false;
	}

	private TreeSet<BoxStandard> getGovParentsStd(Box gov) {
		if (gov==null) return null;
		TreeSet<BoxStandard> result = new TreeSet<BoxStandard>();
		if (gov.isJunctionBlock()) {
			BoxJunction govJun = (BoxJunction) gov;
			result.addAll(getConjunctsStd(govJun));
		}
		else result.add((BoxStandard)gov);
		return result;
	}
		
	private void extractEvents(Box b) {		
		ArrayList<BoxStandard> allStdBoxInB = new ArrayList<BoxStandard>();		
		b.addStandardBoxesInCollection(allStdBoxInB);
		for(BoxStandard bs : allStdBoxInB) {
			Box gov = bs.governingParent();
			TreeSet<BoxStandard> govParentsStd = getGovParentsStd(gov);
			for(BoxStandard govStd : govParentsStd) {
				TreeSet<BoxStandard> sameSideSistersStd = getSameSideSistersStd(govStd, bs);
			}			
		}
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
	
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
	}

	

}
