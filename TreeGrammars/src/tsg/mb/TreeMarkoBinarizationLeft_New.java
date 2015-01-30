package tsg.mb;

import java.util.ArrayList;

import tsg.Label;
import tsg.TSNodeLabel;
import util.BoundedLinkedList;

public class TreeMarkoBinarizationLeft_New extends TreeMarkoBinarization {
	
	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = performParentAnnotation(t);
		return performMarkoBinarizationLeft(result);
	}
	
	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = undoMarkoBinarizationLeft(t);
		return undoParentAnnotation(result);
	}

	
	private TSNodeLabel performMarkoBinarizationLeft(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel binaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return binaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = performMarkoBinarizationLeft(t.firstDaughter());
			binaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};
			onlyDaughter.parent = binaryCopy;
			return binaryCopy;
		}
		
		BoundedLinkedList<String> lastSiblings = new BoundedLinkedList<String>(markH);
		String parentLabelString = t.label();
		
		TSNodeLabel previousDaughter = performMarkoBinarizationLeft(t.daughters[prole-1]);
		String previousDaughterLabelString = previousDaughter.label(); 
		lastSiblings.add(previousDaughterLabelString);
		Label previousLevelLabelString = buildArtificialLabel(parentLabelString, lastSiblings);
		TSNodeLabel previousLevel = new TSNodeLabel(previousLevelLabelString, false);
		binaryCopy.assignUniqueDaughter(previousLevel);
		
		for(int i=prole-2; i>0; i--) {
			
			TSNodeLabel currentDaughter = performMarkoBinarizationLeft(t.daughters[i]);			
			TSNodeLabel right = previousDaughter;
			lastSiblings.add(currentDaughter.label());
			Label newLevelLabel = buildArtificialLabel(parentLabelString, lastSiblings);
			TSNodeLabel newLevel = new TSNodeLabel(newLevelLabel, t.isLexical);			
			previousLevel.assignDaughters(new TSNodeLabel[]{newLevel,right});			
			previousLevel = newLevel;	
			previousDaughter = currentDaughter;
			
		}
		
		TSNodeLabel firstDaughter = performMarkoBinarizationLeft(t.daughters[0]);
		previousLevel.assignDaughters(new TSNodeLabel[]{firstDaughter, previousDaughter});
		
		return binaryCopy;
	}
	

	
	private TSNodeLabel undoMarkoBinarizationLeft(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoMarkoBinarizationLeft(t.firstDaughter());
			if (isArtificialNode(onlyDaughter)) {				
				unbinaryCopy.assignDaughters(onlyDaughter.daughters);
				return unbinaryCopy;
			}
			unbinaryCopy.assignUniqueDaughter(onlyDaughter);
			return unbinaryCopy;
		}
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			newDaughters.add(undoMarkoBinarizationLeft(currentLevel.lastDaughter()));
			currentLevel = currentLevel.daughters[0];
		} while(isArtificialNode(currentLevel));
		newDaughters.add(undoMarkoBinarizationLeft(currentLevel));
		int newProle = newDaughters.size();
		unbinaryCopy.daughters = new TSNodeLabel[newProle];
		int i=newProle-1;
		for(TSNodeLabel d : newDaughters) {
			unbinaryCopy.daughters[i--] = d;
			d.parent = unbinaryCopy;
		}
		return unbinaryCopy;
	}

	
	@Override
	public String getDescription() {
		return "TreeMarkoBinarizationLeft_New";
	}


	
	public static void main(String[] args) throws Exception {
		
		markH=0; 
		markV=1;	
		TSNodeLabel t = new TSNodeLabel("(A (B b) (C (F f) (G g) (H h)) (D (I i)) (E (J (K k) (L l))))");		
		//TSNodeLabel t = new TSNodeLabel("(A B C D E)");
		for(TSNodeLabel d : t.daughters) d.isLexical = false;
		System.out.println(t.toStringQtree());
		TreeMarkoBinarizationLeft_New tmb = new TreeMarkoBinarizationLeft_New();
		TSNodeLabel markoBinarizedTree = tmb.performMarkovBinarization(t);
		System.out.println(markoBinarizedTree.toStringQtree());
		System.out.println(markoBinarizedTree.checkParentDaughtersConsistency());
		System.out.println(markoBinarizedTree.checkOnlyAndAllTerminalsAreLexical());
		System.out.println(markoBinarizedTree.maxBranching());
		TSNodeLabel original = tmb.undoMarkovBinarization(markoBinarizedTree);
		System.out.println(original.toStringQtree());
		System.out.println(original.equals(t));
		System.out.println(original.checkParentDaughtersConsistency());
		System.out.println(original.checkOnlyAndAllTerminalsAreLexical());

	
	}

	
}
