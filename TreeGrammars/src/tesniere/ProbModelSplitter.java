package tesniere;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;


public abstract class ProbModelSplitter extends ProbModel {	
	
	public ProbModelSplitter(ArrayList<Box> trainingTreebank) {
		super(trainingTreebank);
	}
	
	public void train() {
		int sentenceNumber = 1;
		for(Box b : trainingTreebank) {
			extractEvents(b);
			sentenceNumber++;
		}
	}
	
	private void extractEvents(Box b) {
		ArrayList<BoxStandard> allStdBoxInB = new ArrayList<BoxStandard>();		
		b.addStandardBoxesInCollection(allStdBoxInB);		 
		Collections.sort(allStdBoxInB);
		for(BoxStandard bs : allStdBoxInB) {
			Box gov = bs.governingParent();			
			if (gov.isJunctionBlock()) {
				BoxJunction govJunction = (BoxJunction)gov;
				ArrayList<BoxStandard> directIndirectStdBoxConjuncts = new ArrayList<BoxStandard>();
				govJunction.addAllDirectIndirectStdBoxConjuncts(directIndirectStdBoxConjuncts);
				Iterator<BoxStandard> iter = directIndirectStdBoxConjuncts.iterator();
				BoxStandard first = iter.next();
				addDependencyEvent(bs, first);
				while(iter.hasNext()) {
					BoxStandard next = iter.next();
					addExtraDepPresenceInCoord(bs, next, true);
				}
			}
			else {
				BoxStandard govStd = (BoxStandard)gov;
				addDependencyEvent(bs, govStd);
				if (govStd.isConjunct) {
					BoxJunction bigJuntionBox = govStd.getUpmostJunctionParent();
					ArrayList<BoxStandard> directIndirectStdBoxConjuncts = new ArrayList<BoxStandard>();
					bigJuntionBox.addAllDirectIndirectStdBoxConjuncts(directIndirectStdBoxConjuncts);
					for(BoxStandard conj : directIndirectStdBoxConjuncts) {
						if (conj==gov) continue;
						addExtraDepPresenceInCoord(bs, conj, false);
					}
				}
			}
		}	
		Iterator<BoxStandard> iter = allStdBoxInB.iterator();
		BoxStandard first = null;
		BoxStandard second = iter.next();
		while(iter.hasNext()) {
			first = second;
			second = iter.next();
			Box firstGov = first.governingParent(); 
			if (firstGov!=second.governingParent() || 
				sameSide(firstGov, first, second)) continue;
			boolean areConjuncts = !first.isConjunct || !second.isConjunct;
			boolean areConjuncted = areConjuncts && 
				first.getUpmostJunctionParent()==second.getUpmostJunctionParent();
			addCoordPresenceEvent(first, second, areConjuncted);	
		}
	}

	private boolean sameSide(Box gov, BoxStandard b1, BoxStandard b2) {
		return Entity.compare(b1, gov)==Entity.compare(b2, gov);
	}
	

	protected abstract void addDependencyEvent(BoxStandard bs, BoxStandard gov);
	
	protected abstract void addCoordPresenceEvent(BoxStandard bA, BoxStandard bB, boolean coordPresence);

	protected abstract void addExtraDepPresenceInCoord(BoxStandard bs, BoxStandard gov, boolean coordPresence);
	
	public double getProb(Box structure) {
		return 0.;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
