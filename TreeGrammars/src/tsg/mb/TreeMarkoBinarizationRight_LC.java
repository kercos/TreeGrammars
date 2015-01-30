package tsg.mb;

import java.util.ArrayList;

import tsg.Label;
import tsg.TSNodeLabel;
import util.BoundedLinkedList;

public class TreeMarkoBinarizationRight_LC extends TreeMarkoBinarization {
	
	public TSNodeLabel performMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = performParentAnnotation(t);
		return performMarkoBinarizationRight(result);
	}
	
	public TSNodeLabel undoMarkovBinarization(TSNodeLabel t) {
		TSNodeLabel result = undoMarkoBinarizationRight(t);
		return undoParentAnnotation(result);
	}

	
	private TSNodeLabel performMarkoBinarizationRight(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel binaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return binaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = performMarkoBinarizationRight(t.firstDaughter());
			binaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};
			onlyDaughter.parent = binaryCopy;
			return binaryCopy;
		}
		
		BoundedLinkedList<String> lastSiblings = new BoundedLinkedList<String>(markH);
		String parentLabelString = t.label();
		TSNodeLabel previousLevel = binaryCopy;
		for(int i=0; i<prole-1; i++) {
			TSNodeLabel d = t.daughters[i];
			lastSiblings.add(d.label());			 			
			TSNodeLabel left = performMarkoBinarizationRight(d);
			Label rightLabel = TreeMarkoBinarization.buildArtificialLabel(parentLabelString, lastSiblings);
			TSNodeLabel right = new TSNodeLabel(rightLabel, t.isLexical);
			previousLevel.daughters = new TSNodeLabel[]{left,right};
			left.parent = previousLevel;
			right.parent = previousLevel;
			previousLevel = right;
		}
		
		TSNodeLabel onlyDaughter = performMarkoBinarizationRight(t.daughters[prole-1]);
		previousLevel.daughters = new TSNodeLabel[]{onlyDaughter};
		onlyDaughter.parent = previousLevel;
		
		return binaryCopy;
	}

	
	private TSNodeLabel undoMarkoBinarizationRight(TSNodeLabel t) {
		int prole = t.prole();
		TSNodeLabel unbinaryCopy = new TSNodeLabel(t.label, t.isLexical);
		if (prole==0) return unbinaryCopy;
		if (prole==1) {
			TSNodeLabel onlyDaughter = undoMarkoBinarizationRight(t.firstDaughter());
			unbinaryCopy.daughters = new TSNodeLabel[]{onlyDaughter};			
			onlyDaughter.parent = unbinaryCopy;
			return unbinaryCopy;
		}
		
		ArrayList<TSNodeLabel> newDaughters = new ArrayList<TSNodeLabel>();
		TSNodeLabel currentLevel = t;
		do {
			newDaughters.add(undoMarkoBinarizationRight(currentLevel.firstDaughter()));
			currentLevel = currentLevel.lastDaughter();
		} while(isArtificialNode(currentLevel));		
		int newProle = newDaughters.size();
		unbinaryCopy.daughters = new TSNodeLabel[newProle];
		int i=0;
		for(TSNodeLabel d : newDaughters) {
			unbinaryCopy.daughters[i++] = d;
			d.parent = unbinaryCopy;
		}
		return unbinaryCopy;
	}

	
	@Override
	public String getDescription() {
		return "TreeMarkoBinarizationRight_LC";
	}


	public static void main(String[] args) throws Exception {
		
		markH=1; 
		markV=2;
		TreeMarkoBinarizationRight_LC tmb = new TreeMarkoBinarizationRight_LC();
		//TSNodeLabel t = new TSNodeLabel("(A (B b) (C (F f) (G g) (H h)) (D (I i)) (E (J (K k) (L l))))");		
		//TSNodeLabel t = new TSNodeLabel("(A B C D E)");
		//TSNodeLabel t = new TSNodeLabel("(TOP (S (NP (NNS Terms)) (S@ (VP (VBD were) (VP (VBN disclosed))) (S@ (. .)))))");
		TSNodeLabel t = new TSNodeLabel("(TOP (NP (NNP UNK-SC-Dash-y) (NP@ (NNP Street))) (TOP@ (||STOP|| ||STOP||)))");
		//for(TSNodeLabel d : t.daughters) d.isLexical = false;
				
		TSNodeLabel p = tmb.undoMarkovBinarization(t);
		System.out.println(p.toString());
		
		/*
		System.out.println(t.toStringQtree());
		System.out.println(markoBinarizedTree.toStringQtree());
		System.out.println(markoBinarizedTree.checkParentDaughtersConsistency());
		System.out.println(markoBinarizedTree.checkOnlyAndAllTerminalsAreLexical());
		TSNodeLabel original = tmb.undoMarkovBinarization(markoBinarizedTree);
		System.out.println(original.toStringQtree());
		System.out.println(original.equals(t));
		System.out.println(original.checkParentDaughtersConsistency());
		System.out.println(original.checkOnlyAndAllTerminalsAreLexical());
		*/
		
		
	}
	
}
