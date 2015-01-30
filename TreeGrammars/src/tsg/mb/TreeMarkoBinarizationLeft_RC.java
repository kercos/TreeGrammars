package tsg.mb;

import java.util.ArrayList;

import tsg.Label;
import tsg.TSNodeLabel;
import util.BoundedLinkedList;

public class TreeMarkoBinarizationLeft_RC extends TreeMarkoBinarization {
	
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
		TSNodeLabel previousLevel = binaryCopy;
		
		for(int i=prole-1; i>0; i--) {
			
			TSNodeLabel d = t.daughters[i];
			TSNodeLabel right = performMarkoBinarizationLeft(d);
			lastSiblings.add(d.label());			
			Label newLevelLabel = TreeMarkoBinarization.buildArtificialLabel(parentLabelString, lastSiblings);
			TSNodeLabel newLevel = new TSNodeLabel(newLevelLabel, t.isLexical);			
			previousLevel.assignDaughters(new TSNodeLabel[]{newLevel,right});
			
			previousLevel = newLevel;						
			
		}
		
		TSNodeLabel firstDaughter = performMarkoBinarizationLeft(t.firstDaughter());
		previousLevel.assignUniqueDaughter(firstDaughter);
		
		return binaryCopy;
	}
	

	
	private TSNodeLabel undoMarkoBinarizationLeft(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoMarkoBinarizationLeft(t.firstDaughter());
			unbinaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};			
			onlyDaughter.parent = unbinaryCopy;
			return unbinaryCopy;
		}
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			newDaughters.add(undoMarkoBinarizationLeft(currentLevel.lastDaughter()));
			currentLevel = currentLevel.daughters[0];
		} while(isArtificialNode(currentLevel));		
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
		return "TreeMarkoBinarizationLeft_RC";
	}



	public static void main(String[] args) throws Exception {
		
		markH=1; 
		markV=1;	
		//TSNodeLabel t = new TSNodeLabel("(A (B b) (C (F f) (G g) (H h)) (D (I i)) (E (J (K k) (L l))))");		
		TSNodeLabel t = new TSNodeLabel("(A B C D E)");
		for(TSNodeLabel d : t.daughters) d.isLexical = false;
		System.out.println(t.toStringQtree());
		TreeMarkoBinarizationLeft_RC tmb = new TreeMarkoBinarizationLeft_RC();
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
